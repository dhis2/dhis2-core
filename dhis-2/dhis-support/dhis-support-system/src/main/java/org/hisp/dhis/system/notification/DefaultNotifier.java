/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;

/**
 * Implements the {@link Notifier} API on top of a {@link NotifierStore}.
 *
 * <p>Incoming {@link Notification} messages are decoupled from the caller thread by pushing them
 * into a {@link BlockingQueue}. If the queue is full the message is dropped after a short timeout
 * to not block the source thread. A worker thread constantly takes massages from the queue and
 * pushes them into the {@link NotifierStore.NotificationStore}. This has the big advantage that
 * only one thread ever writes to the store preventing any issues caused by concurrent writes. Also,
 * this removes the burden from the source thread to be slowed down by the cost of storing the
 * messages. Last but not least it also makes sure that any error from persisting messages does not
 * affect the source thread.
 *
 * <p>Please note that this implementation does not filter on {@link NotificationLevel} other than
 * {@link NotificationLevel#OFF}. The {@link SystemSettings#getNotifierLogLevel()} is applied in the
 * {@link org.hisp.dhis.scheduling.JobProgress} that forwards to the {@link Notifier} API. This is
 * for backwards compatibility of usages of the {@link Notifier} API outside of job execution and to
 * keep the {@link NotificationLevel} consistent over a job run.
 *
 * @since 2.42
 * @author Jan Bernitt
 */
@Slf4j
public class DefaultNotifier implements Notifier {

  private final NotifierStore store;
  private final ObjectMapper jsonMapper;
  private final SystemSettingsService settingsService;
  private final LongSupplier clock;
  private final BlockingQueue<Entry<UID, Notification>> pushToStore;
  private final AtomicBoolean cleaning = new AtomicBoolean();

  private int maxMessagesPerJob;
  private long cleanAfterIdleTime;
  private long settingsSince;

  public DefaultNotifier(
      NotifierStore store,
      ObjectMapper jsonMapper,
      SystemSettingsService settingsService,
      LongSupplier clock) {
    this.store = store;
    this.jsonMapper = jsonMapper;
    this.settingsService = settingsService;
    this.clock = clock;
    SystemSettings settings = settingsService.getCurrentSettings();
    this.maxMessagesPerJob = settings.getNotifierMaxMessagesPerJob();
    this.cleanAfterIdleTime = settings.getNotifierCleanAfterIdleTime();
    this.settingsSince = currentTimeMillis();
    this.pushToStore = new ArrayBlockingQueue<>(2048);
    Executors.newSingleThreadExecutor().execute(this::asyncPushToStore);
  }

  @Override
  public boolean isIdle() {
    return pushToStore.isEmpty() && !cleaning.get();
  }

  private void asyncPushToStore() {
    long lastTime = 0;
    while (true) {
      try {
        Entry<UID, Notification> e = pushToStore.poll(cleanAfterIdleTime, TimeUnit.MILLISECONDS);
        while (e != null
            && e.getValue().getLevel() == NotificationLevel.LOOP
            && !pushToStore.isEmpty()) {
          e = pushToStore.take(); // skip pushing LOOP entries which are already outdated
        }
        if (e != null) {
          Notification n = e.getValue();
          // make sure notifications are at least 1ms apart
          // also, make sure the time is actually reflecting the insert order
          if (n.getTimestamp() <= lastTime) n.setTimestamp(lastTime + 1);
          asyncPushToStore(e.getKey(), n);
          lastTime = n.getTimestamp();
        } else {
          // when there hasn't been any notifications lately
          // the poll times out; it is a good time to run some cleanup
          asyncAutomaticCleanup();
        }
      } catch (InterruptedException ex) {
        log.warn("Notification lost due interruption.");
        Thread.currentThread().interrupt();
      } catch (Exception ex) {
        log.warn("Notification lost due to: " + ex.getMessage());
      }
    }
  }

  private void asyncPushToStore(UID job, Notification n) {
    NotifierStore.NotificationStore list = store.notifications(n.getCategory(), job);

    boolean limit = true;
    Notification newest = list.getNewest();
    if (newest != null && newest.getLevel() == NotificationLevel.LOOP) {
      list.removeNewest();
      limit = false; // we deleted one so there is room for one
    }
    if (limit) {
      int size = list.size();
      int maxSize = getMaxMessagesPerJob();
      if (size + 1 > maxSize) {
        list.removeOldest(size + 1 - maxSize);
      }
    }
    list.add(n);
    logNotificationAdded(n);
  }

  private static void logNotificationAdded(Notification n) {
    String message = n.getMessage();
    if (message == null || message.isEmpty()) return;
    switch (n.getLevel()) {
      case LOOP, DEBUG -> log.debug(message);
      case INFO -> log.info(message);
      case WARN -> log.warn(message);
      case ERROR -> log.error(message);
    }
  }

  private void asyncAutomaticCleanup() {
    cleaning.set(true);
    try {
      SystemSettings settings = settingsService.getCurrentSettings();
      store.capMaxAge(settings.getNotifierMaxAgeDays());
      store.capMaxCount(settings.getNotifierMaxJobsPerType());
    } finally {
      cleaning.set(false);
    }
  }

  /** This is potentially called so often that it is cached here refreshing it every 10 seconds. */
  private int getMaxMessagesPerJob() {
    long now = currentTimeMillis();
    if (now - settingsSince > 10_000) {
      settingsService
          .clearCurrentSettings(); // since this is in its own worder we have to clear it to refresh
      SystemSettings settings = settingsService.getCurrentSettings();
      maxMessagesPerJob = settings.getNotifierMaxMessagesPerJob();
      cleanAfterIdleTime = settings.getNotifierCleanAfterIdleTime();
      settingsSince = now;
    }
    return maxMessagesPerJob;
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
        new Notification(
            level, id.getJobType(), clock.getAsLong(), message, completed, dataType, data);

    try {
      if (!pushToStore.offer(Map.entry(UID.of(id.getUid()), n), 50, TimeUnit.MILLISECONDS))
        log.warn("Notification lost due to timeout: " + n);
    } catch (InterruptedException e) {
      log.warn("Notification lost due to interruption: " + n);
      Thread.currentThread().interrupt();
    }
    return this;
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
        .notifications(jobType, job)
        .listNewestFirst()
        .collect(toCollection(LinkedList::new));
  }

  private Deque<Notification> getGistNotificationsByJobId(JobType jobType, UID job) {
    Deque<Notification> res = new LinkedList<>();
    NotifierStore.NotificationStore notifications = store.notifications(jobType, job);
    Notification newest = notifications.getNewest();
    if (newest == null) return res;
    // newest goes first
    res.addFirst(newest);
    Notification oldest = notifications.getOldest();
    if (oldest != null && !newest.getId().equals(oldest.getId())) res.addLast(oldest);
    return res;
  }

  @Override
  public Map<String, Deque<Notification>> getNotificationsByJobType(JobType jobType, Boolean gist) {
    if (gist == null) gist = settingsService.getCurrentSettings().getNotifierGistOverview();
    BiFunction<JobType, UID, Deque<Notification>> read =
        gist ? this::getGistNotificationsByJobId : this::getAllNotificationsByJobId;
    Map<String, Deque<Notification>> res = new LinkedHashMap<>();
    store.notifications(jobType).stream()
        .sorted(comparing(NotifierStore.NotificationStore::ageTimestamp).reversed())
        .forEach(s -> res.put(s.job().getValue(), read.apply(s.type(), s.job())));
    return res;
  }

  @Override
  public void clear() {
    store.clear();
  }

  @Override
  public void clear(@Nonnull JobType type) {
    store.clear(type);
  }

  @Override
  public void clear(@Nonnull JobType type, @Nonnull UID job) {
    store.clear(type, job);
  }

  @Override
  public void capMaxAge(int maxAge) {
    store.capMaxAge(maxAge);
  }

  @Override
  public void capMaxCount(int maxCount) {
    store.capMaxCount(maxCount);
  }

  @Override
  public void capMaxAge(int maxAge, @Nonnull JobType type) {
    store.capMaxAge(maxAge, type);
  }

  @Override
  public void capMaxCount(int maxCount, @Nonnull JobType type) {
    store.capMaxCount(maxCount, type);
  }

  @Override
  public <T> Notifier addJobSummary(
      JobConfiguration id, NotificationLevel level, T summary, Class<T> type) {
    if (id == null || level == null || level.isOff() || !type.equals(summary.getClass()))
      return this;

    try {
      store
          .summary(id.getJobType(), UID.of(id.getUid()))
          .set(JsonValue.of(jsonMapper.writeValueAsString(summary)));
    } catch (Exception ex) {
      log.warn("Summary lost due to: " + ex.getMessage());
    }
    return this;
  }

  @Override
  public Map<String, JsonValue> getJobSummariesForJobType(JobType jobType) {
    Map<String, JsonValue> res = new LinkedHashMap<>();
    store.summaries(jobType).stream()
        .sorted(comparing(NotifierStore.SummaryStore::ageTimestamp).reversed())
        .forEach(s -> res.put(s.job().getValue(), s.get()));
    return res;
  }

  @Override
  public JsonValue getJobSummaryByJobId(JobType jobType, String jobId) {
    return store.summary(jobType, UID.of(jobId)).get();
  }
}
