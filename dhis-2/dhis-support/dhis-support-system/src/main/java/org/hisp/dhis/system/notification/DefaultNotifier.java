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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SystemSettingsProvider;

/**
 * Implements the {@link Notifier} API on to of a {@link NotifierStore}.
 *
 * @since 2.42
 * @author Jan Bernitt
 */
@Slf4j
public class DefaultNotifier implements Notifier {

  private final NotifierStore store;
  private final ObjectMapper jsonMapper;
  private final SystemSettingsProvider settingsProvider;
  private final BlockingQueue<Entry<UID, Notification>> pushToStore;
  private int messageLimit;
  private long settingsSince;

  public DefaultNotifier(
      NotifierStore store, ObjectMapper jsonMapper, SystemSettingsProvider settingsProvider) {
    this.store = store;
    this.jsonMapper = jsonMapper;
    this.settingsProvider = settingsProvider;
    this.messageLimit = settingsProvider.getCurrentSettings().getNotifierMaxMessages();
    this.pushToStore = new ArrayBlockingQueue<>(2048);
    this.settingsSince = currentTimeMillis();
    Executors.newSingleThreadExecutor().execute(this::pushToStore);
  }

  private void pushToStore() {
    while (true) {
      try {
        Entry<UID, Notification> n = pushToStore.take();
        while (n.getValue().getLevel() == NotificationLevel.LOOP && !pushToStore.isEmpty()) {
          n = pushToStore.take(); // skip pushing LOOP which are already outdated
        }
        pushToStore(n.getKey(), n.getValue());
      } catch (Exception ex) {
        log.warn("Notification lost due to: " + ex.getMessage());
      }
    }
  }

  /** This is potentially called so often that it is cached here refreshing it every 10 seconds. */
  private int getMessageLimit() {
    long now = currentTimeMillis();
    if (now - settingsSince > 10_000) {
      messageLimit = settingsProvider.getCurrentSettings().getNotifierMaxMessages();
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
      JsonValue data) {
    if (id == null || level.isOff()) return this;

    Notification n =
        new Notification(level, id.getJobType(), new Date(), message, completed, dataType, data);

    try {
      if (!pushToStore.offer(Map.entry(UID.of(id.getUid()), n), 100, TimeUnit.MILLISECONDS))
        log.warn("Notification lost due to timeout: " + n);
    } catch (InterruptedException e) {
      log.warn("Notification lost due to interruption: " + n);
    }
    return this;
  }

  private void pushToStore(UID job, Notification n) {
    NotifierStore.NotificationStore list = store.getNotificationStore(n.getCategory(), job);

    boolean limit = true;
    Notification newest = list.getNewest();
    if (newest != null && newest.getLevel() == NotificationLevel.LOOP) {
      list.removeNewest();
      limit = false; // we deleted one so there is room for one
    }
    if (limit) {
      int size = list.size();
      int maxSize = getMessageLimit();
      if (size >= maxSize) {
        list.removeEarliest(size - maxSize);
      }
    }
    list.add(n);
    NotificationLoggerUtil.log(log, n.getLevel(), n.getMessage());
  }

  @Override
  public Map<JobType, Map<String, Deque<Notification>>> getNotifications(Boolean gist) {
    Map<JobType, Map<String, Deque<Notification>>> res = new EnumMap<>(JobType.class);
    for (JobType jobType : JobType.values()) {
      Map<String, Deque<Notification>> byJobId = getNotificationsByJobType(jobType, gist);
      if (!byJobId.isEmpty()) res.put(jobType, byJobId);
    }
    return res;
  }

  @Override
  public Deque<Notification> getNotificationsByJobId(JobType jobType, String jobId) {
    return getAllNotificationsByJobId(jobType, UID.of(jobId));
  }

  @Nonnull
  private Deque<Notification> getAllNotificationsByJobId(JobType jobType, UID job) {
    return store
        .getNotificationStore(jobType, job)
        .listNewestFirst()
        .collect(toCollection(LinkedList::new));
  }

  private Deque<Notification> getGistNotificationsByJobId(JobType jobType, UID job) {
    Deque<Notification> res = new LinkedList<>();
    NotifierStore.NotificationStore notifications = store.getNotificationStore(jobType, job);
    Notification newest = notifications.getNewest();
    if (newest == null) return res;
    // newest goes first
    res.addFirst(newest);
    Notification earliest = notifications.getEarliest();
    if (earliest != null && !newest.getId().equals(earliest.getId())) res.addLast(earliest);
    return res;
  }

  @Override
  public Map<String, Deque<Notification>> getNotificationsByJobType(JobType jobType, Boolean gist) {
    if (gist == null) gist = settingsProvider.getCurrentSettings().isNotifierGistOverview();
    BiFunction<JobType, UID, Deque<Notification>> read =
        gist ? this::getGistNotificationsByJobId : this::getAllNotificationsByJobId;
    Map<String, Deque<Notification>> res = new LinkedHashMap<>();
    store.getNotificationStores(jobType).stream()
        .sorted(comparing(NotifierStore.NotificationStore::ageTimestamp))
        .forEach(s -> res.put(s.job().getValue(), read.apply(s.type(), s.job())));
    return res;
  }

  @Override
  public Notifier clear(JobConfiguration id) {
    if (id != null) store.clearStore(id.getJobType(), UID.of(id.getUid()));
    return this;
  }

  @Override
  public <T> Notifier addJobSummary(
      JobConfiguration id, NotificationLevel level, T summary, Class<T> type) {
    if (id == null || level == null || level.isOff() || !type.equals(summary.getClass()))
      return this;

    try {
      store
          .getSummaryStore(id.getJobType(), UID.of(id.getUid()))
          .set(JsonValue.of(jsonMapper.writeValueAsString(summary)));
    } catch (Exception ex) {
      log.warn("Summary lost due to: " + ex.getMessage());
    }
    return this;
  }

  @Override
  public Map<String, JsonValue> getJobSummariesForJobType(JobType jobType) {
    Map<String, JsonValue> res = new LinkedHashMap<>();
    store.getSummaryStores(jobType).stream()
        .sorted(comparing(NotifierStore.SummaryStore::ageTimestamp))
        .forEach(s -> res.put(s.job().getValue(), s.get()));
    return res;
  }

  @Override
  public JsonValue getJobSummaryByJobId(JobType jobType, String jobId) {
    return store.getSummaryStore(jobType, UID.of(jobId)).get();
  }
}
