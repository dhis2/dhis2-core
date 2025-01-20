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

import static java.lang.System.currentTimeMillis;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static org.springframework.data.redis.core.ScanOptions.scanOptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Date;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNumber;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.util.DateUtils;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.Cursor;
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
  private static final String NOTIFICATIONS_KEY_PREFIX = "notifications:";
  private static final String SUMMARIES_KEY_PREFIX = "summaries:";
  private static final String SUMMARIES_KEY_ORDER_PREFIX = "summary:order:";
  private static final String SUMMARY_TYPE_PREFIX = "summary:type:";
  private static final String COLON = ":";

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper jsonMapper;
  private final SystemSettingsProvider settingsProvider;
  private int messageLimit;
  private long settingsSince;

  public RedisNotifier(
      RedisTemplate<String, String> redisTemplate,
      ObjectMapper jsonMapper,
      SystemSettingsProvider settingsProvider) {
    this.redisTemplate = redisTemplate;
    this.jsonMapper = jsonMapper;
    this.settingsProvider = settingsProvider;
    this.messageLimit = settingsProvider.getCurrentSettings().getNotifierMessageLimit();
    this.settingsSince = currentTimeMillis();
  }

  /** This is potentially called so often that it is cached here refreshing it every 10 seconds. */
  private int getMessageLimit() {
    long now = currentTimeMillis();
    if (now - settingsSince > 10_000) {
      messageLimit = settingsProvider.getCurrentSettings().getNotifierMessageLimit();
      settingsSince = now;
    }
    return messageLimit;
  }

  @Override
  public Notifier notify(
      JobConfiguration id,
      @Nonnull NotificationLevel level,
      String message,
      boolean completed,
      NotificationDataType dataType,
      JsonNode data) {
    if (id == null || level.isOff()) return this;

    JobType jobType = id.getJobType();
    String zSetKey = generateNotificationsZSetKey(jobType, id.getUid());
    Date now = new Date();
    boolean limit = true;

    BoundZSetOperations<String, String> notifications = redisTemplate.boundZSetOps(zSetKey);
    // -1, -1 is the last (most recent) entry
    Set<String> values = notifications.range(-1, -1);
    if (values != null && !values.isEmpty()) {
      Notification last = readNotificationJson(jobType, values.stream().findFirst().orElse(null));
      if (last != null && last.getLevel() == NotificationLevel.LOOP) {
        notifications.removeRange(-1, -1);
        limit = false; // we deleted one so there is room for one
      }
    }
    if (limit) {
      Long size = notifications.zCard();
      int maxSize = getMessageLimit();
      if (size != null && size >= maxSize) {
        notifications.removeRange(0, size - maxSize);
      }
    }

    Notification notification =
        new Notification(level, jobType, now, message, completed, dataType, data);
    try {
      notifications.add(toJson(notification), now.getTime());
    } catch (RuntimeException ex) {
      log.warn(String.format(NOTIFIER_ERROR, ex.getMessage()));
    }

    NotificationLoggerUtil.log(log, level, message);
    return this;
  }

  @Override
  public Map<JobType, Map<String, Deque<Notification>>> getNotifications(Boolean gist) {
    Map<JobType, Map<String, Deque<Notification>>> res = new EnumMap<>(JobType.class);
    for (JobType jobType : JobType.values()) {
      Map<String, Deque<Notification>> notifications = getNotificationsByJobType(jobType, gist);
      if (!notifications.isEmpty()) res.put(jobType, notifications);
    }
    return res;
  }

  @Override
  public Deque<Notification> getNotificationsByJobId(JobType jobType, String jobId) {
    return getAllNotificationsByRedisKey(jobType, generateNotificationsZSetKey(jobType, jobId));
  }

  @Nonnull
  private Deque<Notification> getAllNotificationsByRedisKey(JobType jobType, String key) {
    // the zset contains messages in timestamp order oldest first, so we use reverse to get newest
    // first
    Set<String> newestFirst = redisTemplate.boundZSetOps(key).reverseRange(0, -1);
    if (newestFirst == null) return new LinkedList<>();
    return newestFirst.stream()
        .map(n -> readNotificationJson(jobType, n))
        .collect(toCollection(LinkedList::new));
  }

  private Deque<Notification> getGistNotificationsByRedisKey(JobType jobType, String key) {
    Deque<Notification> res = new LinkedList<>();
    Set<String> last = redisTemplate.boundZSetOps(key).range(-1, -1);
    if (last == null || last.isEmpty()) return res;
    // newest goes first
    last.forEach(n -> res.add(readNotificationJson(jobType, n)));
    Set<String> first = redisTemplate.boundZSetOps(key).range(0, 0);
    if (first != null && !first.isEmpty() && !last.equals(first))
      first.forEach(n -> res.add(readNotificationJson(jobType, n)));
    return res;
  }

  private Notification readNotificationJson(JobType jobType, String notification) {
    if (notification == null) return null;
    return executeLogErrors(() -> fromJson(notification, jobType));
  }

  @Override
  public Map<String, Deque<Notification>> getNotificationsByJobType(JobType jobType, Boolean gist) {
    if (gist == null) gist = settingsProvider.getCurrentSettings().isNotifierGistOverview();
    BiFunction<JobType, String, Deque<Notification>> read =
        gist ? this::getGistNotificationsByRedisKey : this::getAllNotificationsByRedisKey;
    try (Cursor<String> keys =
        redisTemplate.scan(
            scanOptions().match(generateNotificationsZSetKey(jobType, "*")).build())) {
      return keys.stream()
          .map(key -> Map.entry(key, read.apply(jobType, key)))
          .sorted(comparing(e -> e.getValue().getLast()))
          .collect(
              toMap(
                  e -> redisKeyToJobId(e.getKey()),
                  Entry::getValue,
                  (x, y) -> y,
                  LinkedHashMap::new));
    }
  }

  @Nonnull
  private static String redisKeyToJobId(String key) {
    return key.substring(key.lastIndexOf(':') + 1);
  }

  @Override
  public Notifier clear(JobConfiguration id) {
    if (id != null) {
      redisTemplate.delete(generateNotificationsZSetKey(id.getJobType(), id.getUid()));
      redisTemplate.boundHashOps(generateSummaryKey(id.getJobType())).delete(id.getUid());
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
        if (zCard != null && zCard >= getMessageLimit()) {
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
    Map<String, Object> res = new LinkedHashMap<>();
    try {
      String existingSummaryTypeStr =
          redisTemplate.boundValueOps(generateSummaryTypeKey(jobType)).get();
      if (existingSummaryTypeStr == null) {
        return res;
      }

      Class<?> existingSummaryType = Class.forName(existingSummaryTypeStr);
      Map<Object, Object> serializedSummaryMap =
          redisTemplate.boundHashOps(generateSummaryKey(jobType)).entries();
      if (serializedSummaryMap != null) {
        serializedSummaryMap.forEach(
            (k, v) -> res.put((String) k, readSummaryJson((String) v, existingSummaryType)));
      }
    } catch (ClassNotFoundException ex) {
      log.warn(String.format(NOTIFIER_ERROR, ex.getMessage()));
    }

    return res;
  }

  @CheckForNull
  private Object readSummaryJson(String v, Class<?> summaryType) {
    return executeLogErrors(() -> jsonMapper.readValue(v, summaryType));
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

  private static String generateNotificationsZSetKey(JobType jobType, String jobUid) {
    return NOTIFICATIONS_KEY_PREFIX + jobType.toString() + COLON + jobUid;
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

  private static <T> T executeLogErrors(Callable<T> operation) {
    try {
      return operation.call();
    } catch (Exception ex) {
      log.warn(String.format(NOTIFIER_ERROR, ex.getMessage()));
      return null;
    }
  }

  /*
  Why roll our own (de)-serialisation?

  In large instances there are lots of notification messages.
  The goal is to use less disk/memory.
  Therefor all fields of a message that contain duplicate information
  are skipped and later recovered.
   */

  private String toJson(Notification n) {
    return JsonBuilder.createObject(
            obj -> {
              obj.addString("id", n.getId())
                  .addNumber("time", n.getTime().getTime())
                  .addString("message", n.getMessage());
              if (n.getLevel() != NotificationLevel.INFO)
                obj.addString("level", n.getLevel().name());
              if (n.isCompleted()) obj.addBoolean("completed", true);
              if (n.getDataType() != null) {
                obj.addString("dataType", n.getDataType().name());
                String data = executeLogErrors(() -> jsonMapper.writeValueAsString(n.getData()));
                if (data != null) obj.addMember("data", org.hisp.dhis.jsontree.JsonNode.of(data));
              }
            })
        .toString();
  }

  private Notification fromJson(String json, JobType jobType) {
    JsonObject src = JsonMixed.of(json);
    Notification dest = new Notification();
    dest.setCategory(jobType);
    JsonString level = src.getString("level");
    dest.setLevel(
        level.isUndefined() ? NotificationLevel.INFO : level.parsed(NotificationLevel::valueOf));
    String id = src.getString("id").string();
    dest.setUid(id);
    dest.setCompleted(src.getBoolean("completed").booleanValue(false));
    JsonValue time = src.get("time");
    dest.setTime(
        time.isNumber()
            ? new Date(time.as(JsonNumber.class).longValue())
            : DateUtils.parseDate(time.as(JsonString.class).string()));
    dest.setMessage(src.getString("message").string());
    dest.setDataType(src.getString("dataType").parsed(NotificationDataType::valueOf));
    JsonValue data = src.get("data");
    if (!data.isUndefined())
      dest.setData(executeLogErrors(() -> jsonMapper.readTree(src.toJson())));
    return dest;
  }
}
