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
package org.hisp.dhis.system.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Notifier implementation backed by redis. It holds 2 types of data. Notifications and Summaries.
 * Since order of the Notifications and Summaries are important, (to limit the maximum number of
 * objects held), we use a combination of "Sorted Sets" , "HashMaps" and "Values" (data structures
 * in redis) to have a similar behaviour as InMemoryNotifier.
 *
 * @author Ameen Mohamed
 */
@Slf4j
public class RedisNotifier implements Notifier {
  private static final String NOTIFIER_ERROR = "Redis Notifier error:%s";

  private final RedisTemplate<String, String> redisTemplate;

  private static final String NOTIFICATIONS_KEY_PREFIX = "notifications:";

  private static final String NOTIFICATION_ORDER_KEY_PREFIX = "notification:order:";

  private static final String SUMMARIES_KEY_PREFIX = "summaries:";

  private static final String SUMMARIES_KEY_ORDER_PREFIX = "summary:order:";

  private static final String SUMMARY_TYPE_PREFIX = "summary:type:";

  private static final String COLON = ":";

  private static final int MAX_POOL_TYPE_SIZE = 500;

  private final ObjectMapper jsonMapper;

  public RedisNotifier(RedisTemplate<String, String> redisTemplate, ObjectMapper jsonMapper) {
    this.redisTemplate = redisTemplate;
    this.jsonMapper = jsonMapper;
  }

  // -------------------------------------------------------------------------
  // Notifier implementation backed by Redis
  // -------------------------------------------------------------------------

  @Override
  public Notifier notify(
      JobConfiguration id,
      @Nonnull NotificationLevel level,
      String message,
      boolean completed,
      NotificationDataType dataType,
      JsonNode data) {
    if (id != null && !level.isOff()) {
      Notification notification =
          new Notification(level, id.getJobType(), new Date(), message, completed, dataType, data);

      if (id.isInMemoryJob() && StringUtils.isEmpty(id.getUid())) {
        notification.setUid(id.getUid());
      }

      String notificationKey = generateNotificationKey(id.getJobType(), id.getUid());
      String notificationOrderKey = generateNotificationOrderKey(id.getJobType());

      Date now = new Date();

      try {
        Long zCard = redisTemplate.boundZSetOps(notificationOrderKey).zCard();
        if (zCard != null && zCard >= MAX_POOL_TYPE_SIZE) {
          Set<String> deleteKeys = redisTemplate.boundZSetOps(notificationOrderKey).range(0, 0);
          if (deleteKeys != null) {
            redisTemplate.delete(deleteKeys);
          }
          redisTemplate.boundZSetOps(notificationOrderKey).removeRange(0, 0);
        }

        redisTemplate
            .boundZSetOps(notificationKey)
            .add(jsonMapper.writeValueAsString(notification), now.getTime());
        redisTemplate.boundZSetOps(notificationOrderKey).add(id.getUid(), now.getTime());
      } catch (JsonProcessingException ex) {
        log.warn(String.format(NOTIFIER_ERROR, ex.getMessage()));
      }

      NotificationLoggerUtil.log(log, level, message);
    }
    return this;
  }

  @Override
  public Map<JobType, Map<String, Deque<Notification>>> getNotifications() {
    Map<JobType, Map<String, Deque<Notification>>> notifications = new EnumMap<>(JobType.class);
    for (JobType jobType : JobType.values()) {
      notifications.put(jobType, getNotificationsByJobType(jobType));
    }
    return notifications;
  }

    @Override
    public Deque<Notification> getNotificationsByJobId( JobType jobType, String jobId )
    {
        Set<String> notifications = redisTemplate.boundZSetOps( generateNotificationKey( jobType, jobId ) ).range( 0,
            -1 );
        if ( notifications == null )
            return new LinkedList<>();
        List<Notification> res = new ArrayList<>();
        notifications.forEach( notification -> executeLogErrors(
            () -> res.add( jsonMapper.readValue( notification, Notification.class ) ) ) );
        Collections.sort( res );
        return new LinkedList<>( res );
    }

  @Override
  public Map<String, Deque<Notification>> getNotificationsByJobType(JobType jobType) {
    Set<String> keys =
        redisTemplate.boundZSetOps(generateNotificationOrderKey(jobType)).range(0, -1);
    if (keys == null || keys.isEmpty()) return Map.of();
    LinkedHashMap<String, Deque<Notification>> res = new LinkedHashMap<>();
    keys.forEach(jobId -> res.put(jobId, getNotificationsByJobId(jobType, jobId)));
    return res;
  }

  @Override
  public Notifier clear(JobConfiguration id) {
    if (id != null) {
      redisTemplate.delete(generateNotificationKey(id.getJobType(), id.getUid()));
      redisTemplate.boundHashOps(generateSummaryKey(id.getJobType())).delete(id.getUid());
      redisTemplate.boundZSetOps(generateNotificationOrderKey(id.getJobType())).remove(id.getUid());
      redisTemplate.boundZSetOps(generateSummaryOrderKey(id.getJobType())).remove(id.getUid());
    }

    return this;
  }

  @Override
  public <T> Notifier addJobSummary(
      JobConfiguration id, NotificationLevel level, T jobSummary, Class<T> jobSummaryType) {
    if (id != null
        && !(level != null && level.isOff())
        && jobSummary.getClass().equals(jobSummaryType)) {
      String summaryKey = generateSummaryKey(id.getJobType());
      try {
        String existingSummaryTypeStr =
            redisTemplate.boundValueOps(generateSummaryTypeKey(id.getJobType())).get();
        if (existingSummaryTypeStr == null) {
          redisTemplate
              .boundValueOps(generateSummaryTypeKey(id.getJobType()))
              .set(jobSummaryType.getName());
        } else {
          Class<?> existingSummaryType = Class.forName(existingSummaryTypeStr);
          if (!existingSummaryType.equals(jobSummaryType)) {
            return this;
          }
        }

        String summaryOrderKey = generateSummaryOrderKey(id.getJobType());
        Long zCard = redisTemplate.boundZSetOps(summaryOrderKey).zCard();
        if (zCard != null && zCard >= MAX_POOL_TYPE_SIZE) {
          Set<String> summaryKeysToBeDeleted =
              redisTemplate.boundZSetOps(summaryOrderKey).range(0, 0);
          redisTemplate.boundZSetOps(summaryOrderKey).removeRange(0, 0);
          if (summaryKeysToBeDeleted != null) {
            summaryKeysToBeDeleted.forEach(d -> redisTemplate.boundHashOps(summaryKey).delete(d));
          }
        }
        redisTemplate
            .boundHashOps(summaryKey)
            .put(id.getUid(), jsonMapper.writeValueAsString(jobSummary));
        Date now = new Date();

        redisTemplate.boundZSetOps(summaryOrderKey).add(id.getUid(), now.getTime());

      } catch (JsonProcessingException | ClassNotFoundException ex) {
        log.warn(String.format(NOTIFIER_ERROR, ex.getMessage()));
      }
    }
    return this;
  }

  @Override
  public Map<String, Object> getJobSummariesForJobType(JobType jobType) {
    Map<String, Object> jobSummariesForType = new LinkedHashMap<>();
    try {
      String existingSummaryTypeStr =
          redisTemplate.boundValueOps(generateSummaryTypeKey(jobType)).get();
      if (existingSummaryTypeStr == null) {
        return jobSummariesForType;
      }

      Class<?> existingSummaryType = Class.forName(existingSummaryTypeStr);
      Map<Object, Object> serializedSummaryMap =
          redisTemplate.boundHashOps(generateSummaryKey(jobType)).entries();
      if (serializedSummaryMap != null) {
        serializedSummaryMap.forEach(
            (k, v) ->
                executeLogErrors(
                    () ->
                        jobSummariesForType.put(
                            (String) k, jsonMapper.readValue((String) v, existingSummaryType))));
      }
    } catch (ClassNotFoundException ex) {
      log.warn(String.format(NOTIFIER_ERROR, ex.getMessage()));
    }

    return jobSummariesForType;
  }

  @Override
  public Object getJobSummaryByJobId(JobType jobType, String jobId) {
    String existingSummaryTypeStr =
        redisTemplate.boundValueOps(generateSummaryTypeKey(jobType)).get();
    if (existingSummaryTypeStr == null) {
      return null;
    }
    try {
      Class<?> existingSummaryType = Class.forName(existingSummaryTypeStr);
      Object serializedSummary = redisTemplate.boundHashOps(generateSummaryKey(jobType)).get(jobId);

      return serializedSummary != null
          ? jsonMapper.readValue((String) serializedSummary, existingSummaryType)
          : null;
    } catch (IOException | ClassNotFoundException ex) {
      log.warn(String.format(NOTIFIER_ERROR, ex.getMessage()));
    }
    return null;
  }

  private static String generateNotificationKey(JobType jobType, String jobUid) {
    return NOTIFICATIONS_KEY_PREFIX + jobType.toString() + COLON + jobUid;
  }

  private static String generateNotificationOrderKey(JobType jobType) {
    return NOTIFICATION_ORDER_KEY_PREFIX + jobType.toString();
  }

  private static String generateSummaryKey(JobType jobType) {
    return SUMMARIES_KEY_PREFIX + jobType.toString();
  }

  private static String generateSummaryOrderKey(JobType jobType) {
    return SUMMARIES_KEY_ORDER_PREFIX + jobType.toString();
  }

  private static String generateSummaryTypeKey(JobType jobType) {
    return SUMMARY_TYPE_PREFIX + jobType.toString();
  }

  private interface Operation {

    void run() throws Exception;
  }

  private static void executeLogErrors(Operation operation) {
    try {
      operation.run();
    } catch (Exception ex) {
      log.warn(String.format(NOTIFIER_ERROR, ex.getMessage()));
    }
  }
}
