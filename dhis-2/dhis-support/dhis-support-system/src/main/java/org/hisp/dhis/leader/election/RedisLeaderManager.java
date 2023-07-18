/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.leader.election;

import static java.lang.String.format;

import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Takes care of the leader election implementation backed by redis.
 *
 * @author Ameen Mohamed
 */
@Slf4j
public class RedisLeaderManager implements LeaderManager {
  private static final String KEY = "dhis2:leader";

  private static final String NODE_ID_KEY = "dhis2:leaderNodeId";

  private static final String CLUSTER_LEADER_RENEWAL = "Cluster leader renewal";

  private final String nodeUuid;

  private final String nodeId;

  private final Long timeToLiveSeconds;

  private SchedulingManager schedulingManager;

  private final StringRedisTemplate redisTemplate;

  public RedisLeaderManager(
      Long timeToLiveMinutes,
      StringRedisTemplate redisTemplate,
      DhisConfigurationProvider dhisConfigurationProvider) {
    this.nodeId = dhisConfigurationProvider.getProperty(ConfigurationKey.NODE_ID);
    this.nodeUuid = UUID.randomUUID().toString();
    log.info(
        "Setting up redis based leader manager with NodeUuid:{} and NodeID:{}",
        this.nodeUuid,
        this.nodeId);
    this.timeToLiveSeconds = timeToLiveMinutes * 60;
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void renewLeader(JobProgress progress) {
    if (isLeader()) {
      progress.startingStage("Renewing leader with nodeId:" + nodeUuid);
      progress.runStage(
          () -> {
            redisTemplate.expire(KEY, timeToLiveSeconds, TimeUnit.SECONDS);
            redisTemplate.expire(NODE_ID_KEY, timeToLiveSeconds, TimeUnit.SECONDS);
          });
    }
  }

  @Override
  public void electLeader(JobProgress progress) {
    progress.startingStage("Election attempt by nodeId:" + nodeUuid);
    progress.runStage(
        () -> {
          redisTemplate
              .opsForValue()
              .setIfAbsent(KEY, nodeUuid, timeToLiveSeconds, TimeUnit.SECONDS);
          redisTemplate
              .opsForValue()
              .setIfAbsent(NODE_ID_KEY, nodeId, timeToLiveSeconds, TimeUnit.SECONDS);
        });
    if (isLeader()) {
      renewLeader(progress);

      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.SECOND, (int) (this.timeToLiveSeconds / 2));
      progress.startingStage(
          format("Schedule leader renewal for nodeId:%s at: %s", nodeUuid, calendar.getTime()));
      JobConfiguration leaderRenewalJobConfiguration =
          new JobConfiguration(CLUSTER_LEADER_RENEWAL, JobType.LEADER_RENEWAL, null, true);
      progress.runStage(
          () ->
              schedulingManager.scheduleWithStartTime(
                  leaderRenewalJobConfiguration, calendar.getTime()));
    }
  }

  @Override
  public boolean isLeader() {
    String leaderId = getLeaderNodeUuidFromRedis();
    return nodeUuid.equals(leaderId);
  }

  private String getLeaderNodeUuidFromRedis() {
    return redisTemplate.boundValueOps(KEY).get();
  }

  private String getLeaderNodeIdFromRedis() {
    return redisTemplate.boundValueOps(NODE_ID_KEY).get();
  }

  @Override
  public void setSchedulingManager(SchedulingManager schedulingManager) {
    this.schedulingManager = schedulingManager;
  }

  @Override
  public String getCurrentNodeUuid() {
    return this.nodeUuid;
  }

  @Override
  public String getLeaderNodeUuid() {
    return getLeaderNodeUuidFromRedis();
  }

  @Override
  public String getLeaderNodeId() {
    return getLeaderNodeIdFromRedis();
  }
}
