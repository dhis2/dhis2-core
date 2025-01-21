/*
 * Copyright (c) 2004-2025, University of Oslo
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
import static java.util.Comparator.comparingLong;
import static org.springframework.data.redis.core.ScanOptions.scanOptions;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.Json;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNumber;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.util.DateUtils;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Provides Redis backed implementation of the {@link NotifierStore}.
 *
 * @since 2.42
 * @author Jan Bernitt
 */
@RequiredArgsConstructor
public class RedisNotifierStore implements NotifierStore {

  private final RedisTemplate<String, String> redis;

  @Nonnull
  @Override
  public NotificationStore getNotificationStore(@Nonnull JobType type, @Nonnull UID job) {
    return getNotificationStore(type, getNotificationsKey(type.name(), job.getValue()));
  }

  @Nonnull
  @Override
  public List<? extends NotificationStore> getNotificationStores(@Nonnull JobType type) {
    try (Cursor<String> keys =
        redis.scan(scanOptions().match(getNotificationsKey(type.name(), "*")).build())) {
      return keys.stream().map(key -> getNotificationStore(type, key)).toList();
    }
  }

  @Nonnull
  private RedisNotificationStore getNotificationStore(@Nonnull JobType type, String key) {
    UID job = UID.of(key.substring(key.lastIndexOf(':') + 1));
    return new RedisNotificationStore(type, job, redis.boundZSetOps(key));
  }

  @Nonnull
  @Override
  public SummaryStore getSummaryStore(@Nonnull JobType type, @Nonnull UID job) {
    return new RedisSummaryStore(type, job, redis.boundHashOps(getSummariesKey(type.name())));
  }

  @Nonnull
  @Override
  public List<? extends SummaryStore> getSummaryStores(@Nonnull JobType type) {
    BoundHashOperations<String, String, String> table =
        redis.boundHashOps(getSummariesKey(type.name()));
    Set<String> keys = table.keys();
    if (keys == null || keys.isEmpty()) return List.of();
    return keys.stream().map(job -> new RedisSummaryStore(type, UID.of(job), table)).toList();
  }

  @Override
  public void clearStores() {
    clear(getNotificationsKey("*", null));
    clear(getSummariesKey("*"));
  }

  @Override
  public void clearStore(@Nonnull JobType type) {
    clear(getNotificationsKey(type.name(), "*"));
    clear(getSummariesKey(type.name()));
  }

  @Override
  public void clearStore(@Nonnull JobType type, @Nonnull UID job) {
    redis.delete(getNotificationsKey(type.name(), job.getValue()));
    redis.boundHashOps(getSummariesKey(type.name())).delete(job.getValue());
  }

  private void clear(String pattern) {
    Set<String> keys = redis.keys(pattern);
    if (keys == null || keys.isEmpty()) return;
    redis.delete(keys);
  }

  @Override
  public void capStoresByCount(int n, @Nonnull JobType type) {
    if (n <= 0) {
      clearStore(type);
      return;
    }
    List<? extends NotificationStore> stores = getNotificationStores(type);
    if (stores.size() <= n) return;
    int remove = stores.size() - n;
    stores.stream()
        .sorted(comparingLong(NotificationStore::ageTimestamp))
        .limit(remove)
        .forEach(s -> clearStore(s.type(), s.job()));
  }

  @Override
  public void capStoresByAge(int days, @Nonnull JobType type) {
    List<? extends NotificationStore> stores = getNotificationStores(type);
    long removeBefore = currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
    stores.stream()
        .filter(s -> s.ageTimestamp() < removeBefore)
        .forEach(s -> clearStore(s.type(), s.job()));
  }

  private record RedisNotificationStore(
      JobType type, UID job, BoundZSetOperations<String, String> collection)
      implements NotificationStore {

    @Override
    public long ageTimestamp() {
      Notification newest = getNewest();
      // when it is empty it is fine to remove it,
      // should it be active it will be added to anyhow
      return newest == null ? 0L : newest.getTime().getTime();
    }

    @Override
    public int size() {
      Long size = collection.zCard();
      return size == null ? 0 : size.intValue();
    }

    @Override
    public void removeNewest() {
      collection.removeRange(-1, -1);
    }

    @Override
    public void removeOldest(int n) {
      collection.removeRange(1, n); // keep the first
    }

    @Override
    public void add(@Nonnull Notification n) {
      collection.add(toJson(n), n.getTime().getTime());
    }

    @CheckForNull
    @Override
    public Notification getNewest() {
      return getEntry(-1); // last in zset
    }

    @CheckForNull
    @Override
    public Notification getOldest() {
      return getEntry(0); // first in zset
    }

    @CheckForNull
    private Notification getEntry(int index) {
      Set<String> values = collection.range(index, index);
      if (values == null || values.isEmpty()) return null;
      return fromJson(type, values.stream().findFirst().orElse(null));
    }

    @Nonnull
    @Override
    public Stream<Notification> listNewestFirst() {
      Set<String> newestFirst = collection.reverseRange(0, -1);
      if (newestFirst == null) return Stream.empty();
      return newestFirst.stream().map(n -> fromJson(type, n));
    }

    private Notification fromJson(JobType jobType, String json) {
      if (json == null || json.isEmpty()) return null;
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
      if (!data.isUndefined()) dest.setData(data);
      return dest;
    }

    /**
     * The idea is to make the JSON as small as possible. Therefore, uid is excluded (duplicate of
     * id), time is added as timestamp number, level is only added then not INFO, completed is only
     * added when true, data and dataType are only added when set. The category is skipped entirely
     * as it can be recovered from the redis key/query.
     *
     * @param n a notification to serialize
     * @return the notification as minimal JSON
     */
    private String toJson(Notification n) {
      return Json.object(
              obj -> {
                obj.addString("id", n.getId())
                    .addNumber("time", n.getTime().getTime())
                    .addString("message", n.getMessage());
                if (n.getLevel() != NotificationLevel.INFO)
                  obj.addString("level", n.getLevel().name());
                if (n.isCompleted()) obj.addBoolean("completed", true);
                if (n.getDataType() != null) {
                  obj.addString("dataType", n.getDataType().name());
                  if (n.getData() != null && !n.getData().isUndefined())
                    obj.addMember("data", n.getData().node());
                }
              })
          .toJson();
    }
  }

  private record RedisSummaryStore(
      JobType type, UID job, BoundHashOperations<String, String, String> table)
      implements SummaryStore {

    @Override
    public long ageTimestamp() {
      String json = table.get(job.getValue());
      if (json == null) return 0L;
      JsonNumber ageTimestamp = JsonMixed.of(json).getNumber("ageTimestamp");
      return ageTimestamp.isUndefined() ? 0L : ageTimestamp.longValue();
    }

    @CheckForNull
    @Override
    public JsonValue get() {
      String json = table.get(job.getValue());
      if (json == null) return null;
      JsonMixed root = JsonMixed.of(json);
      if (root.get("ageTimestamp").isUndefined()) return root;
      return root.get("value");
    }

    @Override
    public void set(@Nonnull JsonValue value) {
      table.put(
          job().getValue(),
          Json.object(
                  obj ->
                      obj.addMember("value", value.node())
                          .addNumber("ageTimestamp", currentTimeMillis()))
              .toJson());
    }
  }

  private static String getNotificationsKey(@Nonnull String jobType, @CheckForNull String jobUid) {
    return jobUid == null
        ? "notifications:%s".formatted(jobType)
        : "notifications:%s:%s".formatted(jobType, jobUid);
  }

  private static String getSummariesKey(@Nonnull String jobType) {
    return "summaries:%s".formatted(jobType);
  }
}
