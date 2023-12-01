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

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Takes care of the leader election implementation backed by redis.
 *
 * @author Ameen Mohamed
 */
@Slf4j
public class RedisLeaderManager implements LeaderManager {

  private static final String KEY = "dhis2:leader";
  private static final String NODE_ID_KEY_PREFIX = "dhis2:leader:";

  private final String nodeUuid;
  private final StringRedisTemplate redisTemplate;

  public RedisLeaderManager(
      StringRedisTemplate redisTemplate, DhisConfigurationProvider dhisConfigurationProvider) {
    this.redisTemplate = redisTemplate;
    this.nodeUuid = UUID.randomUUID().toString();
    String nodeId = dhisConfigurationProvider.getProperty(ConfigurationKey.NODE_ID);
    log.info(
        "Setting up redis based leader manager with NodeUuid:{} and NodeID:{}", nodeUuid, nodeId);
    redisTemplate.opsForValue().set(NODE_ID_KEY_PREFIX + nodeUuid, nodeId);
  }

  @Override
  public void renewLeader(int ttlSeconds) {
    if (isLeader()) {
      redisTemplate.expire(KEY, ttlSeconds, TimeUnit.SECONDS);
    }
  }

  @Override
  public void electLeader(int ttlSeconds) {
    redisTemplate.opsForValue().setIfAbsent(KEY, nodeUuid, ttlSeconds, TimeUnit.SECONDS);
  }

  @Override
  public boolean isLeader() {
    return nodeUuid.equals(getLeaderNodeUuidFromRedis());
  }

  private String getLeaderNodeUuidFromRedis() {
    return redisTemplate.boundValueOps(KEY).get();
  }

  private String getLeaderNodeIdFromRedis() {
    String uuid = getLeaderNodeUuidFromRedis();
    return uuid == null ? null : redisTemplate.boundValueOps(NODE_ID_KEY_PREFIX + uuid).get();
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
