/*
 * Copyright (c) 2004-2025, University of Oslo
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
import static java.util.function.Predicate.not;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.scheduling.JobType;

/**
 * A {@link NotifierStore} that stores values in memory.
 *
 * @implNote The implementation does need to use concurrent collections because there can be
 *     concurrent reads (in particular iteration) while writes are running.
 * @since 2.42
 * @author Jan Bernitt
 */
public final class InMemoryNotifierStore implements NotifierStore {

  private final Map<JobType, Map<UID, SummaryStore>> summaryStores = new ConcurrentHashMap<>();
  private final Map<JobType, Map<UID, NotificationStore>> notificationStores =
      new ConcurrentHashMap<>();

  @Nonnull
  @Override
  public NotificationStore notifications(@Nonnull JobType type, @Nonnull UID job) {
    return notificationStores
        .computeIfAbsent(type, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(
            job, k -> new InMemoryNotificationStore(type, job, new ConcurrentLinkedDeque<>()));
  }

  @Nonnull
  @Override
  public SummaryStore summary(@Nonnull JobType type, @Nonnull UID job) {
    return summaryStores
        .computeIfAbsent(type, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(job, k -> new InMemorySummaryStore(type, job, new AtomicReference<>()));
  }

  @Nonnull
  @Override
  public List<NotificationStore> notifications(@Nonnull JobType type) {
    Map<UID, NotificationStore> byId = notificationStores.get(type);
    return byId == null
        ? List.of()
        : byId.values().stream().filter(not(NotificationStore::isEmpty)).toList();
  }

  @Nonnull
  @Override
  public List<SummaryStore> summaries(@Nonnull JobType type) {
    Map<UID, SummaryStore> byId = summaryStores.get(type);
    return byId == null
        ? List.of()
        : byId.values().stream().filter(SummaryStore::isPresent).toList();
  }

  @Override
  public void clear() {
    notificationStores.clear();
    summaryStores.clear();
  }

  @Override
  public void clear(@Nonnull JobType type) {
    notificationStores.remove(type);
    summaryStores.remove(type);
  }

  @Override
  public void clear(@Nonnull JobType type, @Nonnull UID job) {
    Map<UID, ?> map = notificationStores.get(type);
    if (map != null) map.remove(job);
    map = summaryStores.get(type);
    if (map != null) map.remove(job);
  }

  private record InMemoryNotificationStore(JobType type, UID job, Deque<Notification> collection)
      implements NotificationStore {

    @Override
    public boolean isEmpty() {
      return collection.isEmpty();
    }

    @Override
    public int size() {
      return collection.size();
    }

    @Override
    public void removeNewest() {
      collection.pollFirst();
    }

    @Override
    public void removeOldest(int n) {
      Notification n0 = collection.pollLast();
      for (int i = 0; i < n; i++) collection.pollLast();
      if (n0 != null) collection.addLast(n0);
    }

    @Override
    public void add(@Nonnull Notification n) {
      collection.addFirst(n);
    }

    @CheckForNull
    @Override
    public Notification getNewest() {
      return collection.peekFirst();
    }

    @CheckForNull
    @Override
    public Notification getOldest() {
      return collection.peekLast();
    }

    @Nonnull
    @Override
    public Stream<Notification> listNewestFirst() {
      return collection.stream();
    }
  }

  private record Summary(long ageTimestamp, JsonValue value) {}

  private record InMemorySummaryStore(JobType type, UID job, AtomicReference<Summary> summary)
      implements SummaryStore {

    @Override
    public long ageTimestamp() {
      Summary s = summary.get();
      return s == null ? 0L : s.ageTimestamp();
    }

    @CheckForNull
    @Override
    public JsonValue get() {
      Summary s = summary.get();
      return s == null ? null : s.value();
    }

    @Override
    public void set(@Nonnull JsonValue summary) {
      this.summary.set(new Summary(currentTimeMillis(), summary));
    }
  }
}
