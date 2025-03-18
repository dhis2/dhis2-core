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
import static java.util.Comparator.comparingLong;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.scheduling.JobType;

/**
 * Persistence API for {@link Notification}s.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
public interface NotifierStore {

  @Nonnull
  NotificationStore notifications(@Nonnull JobType type, @Nonnull UID job);

  @Nonnull
  List<NotificationStore> notifications(@Nonnull JobType type);

  @Nonnull
  SummaryStore summary(@Nonnull JobType type, @Nonnull UID job);

  @Nonnull
  List<SummaryStore> summaries(@Nonnull JobType type);

  /**
   * Removes all data for the specified job (both notifications and summary)
   *
   * @param type of the job to clear
   * @param job ID of the job to clear
   */
  void clear(@Nonnull JobType type, @Nonnull UID job);

  /**
   * Removes all data for all jobs of the specified type.
   *
   * @param type of the jobs to clear
   */
  void clear(@Nonnull JobType type);

  /** Removes all data (of all jobs and job types). */
  void clear();

  /**
   * Removes all data for jobs except for the most recent n ones for each type.
   *
   * @param maxCount number to keep (most recent first)
   */
  default void capMaxCount(int maxCount) {
    if (maxCount <= 0) {
      clear();
      return;
    }
    Stream.of(JobType.values()).forEach(type -> capMaxCount(maxCount, type));
  }

  /**
   * Removes all data unless the job is younger than the given max age in days
   *
   * @param maxAge keep the data for jobs from the most recent days
   */
  default void capMaxAge(int maxAge) {
    Stream.of(JobType.values()).forEach(type -> capMaxAge(maxAge, type));
  }

  /**
   * Removes all data for jobs of the given type except for the most recent n ones.
   *
   * @param type of jobs to check
   * @param maxCount number to keep (most recent first)
   */
  default void capMaxCount(int maxCount, @Nonnull JobType type) {
    if (maxCount <= 0) {
      clear(type);
      return;
    }
    List<? extends NotificationStore> stores = notifications(type);
    if (stores.size() <= maxCount) return;
    int remove = stores.size() - maxCount;
    stores.stream()
        .sorted(comparingLong(NotificationStore::ageTimestamp))
        .limit(remove)
        .forEach(s -> clear(s.type(), s.job()));
  }

  /**
   * Removes all data for jobs of the given type unless they are younger than the given max age in
   * days
   *
   * @param type of jobs to check
   * @param maxAge keep the data for jobs from the most recent days
   */
  default void capMaxAge(int maxAge, @Nonnull JobType type) {
    List<? extends NotificationStore> stores = notifications(type);
    long removeBefore = currentTimeMillis() - TimeUnit.DAYS.toMillis(maxAge);
    stores.stream()
        .filter(s -> s.ageTimestamp() < removeBefore)
        .forEach(s -> clear(s.type(), s.job()));
  }

  /** The common API for {@link NotificationStore} and {@link SummaryStore}. */
  sealed interface PerJobStore {

    /**
     * @return the {@link JobType} the store deals with
     */
    JobType type();

    /**
     * @return the ID of the {@link org.hisp.dhis.scheduling.JobConfiguration} the store contains
     *     data for
     */
    UID job();

    /**
     * @return the most recent timestamp the data in this store was touched
     */
    long ageTimestamp();
  }

  /**
   * API to interact with the collection for a single list of {@link Notification}s for a specific
   * {@link JobType} and job by its {@link UID}. The collection is sorted by the {@link
   * Notification#getTime()} value.
   *
   * @implSpec Implementations must handle reading and writing methods being used concurrently, but
   *     writes ({@link #add(Notification)}) will never be called concurrently
   */
  non-sealed interface NotificationStore extends PerJobStore {

    default boolean isEmpty() {
      return size() == 0;
    }

    /**
     * @return number of {@link Notification}s in the collection of this store
     */
    int size();

    /** Removes the {@link Notification} most recently added */
    void removeNewest();

    /**
     * This does however retain the very first message.
     *
     * @param n number of messages to remove from the start (oldest) (but not the 1st)
     */
    void removeOldest(int n);

    /**
     * Adds a new {@link Notification} which is expected to be younger in terms of {@link
     * Notification#getTime()} as any entry already added before.
     *
     * @param n the entry to add
     */
    void add(@Nonnull Notification n);

    /**
     * @return the entry most recently added, or null if the store is empty
     */
    @CheckForNull
    Notification getNewest();

    /**
     * @return the entry added first (when the store was empty) or null if it still is empty
     */
    @CheckForNull
    Notification getOldest();

    /**
     * @return a stream of the store entries starting with the one most recently added and ending
     *     with the one added first, when the store was empty
     */
    @Nonnull
    Stream<Notification> listNewestFirst();

    /**
     * @return the timestamp to use when comparing the age of this store to the age of other stores
     */
    @Override
    default long ageTimestamp() {
      Notification newest = getNewest();
      return newest == null ? 0L : newest.getTime().getTime();
    }
  }

  /**
   * API for a store containing the summary value associated with a specific {@link
   * org.hisp.dhis.scheduling.JobConfiguration}.
   */
  non-sealed interface SummaryStore extends PerJobStore {

    default boolean isPresent() {
      return get() != null;
    }

    /**
     * @return the stored summary value, or {@code null} when no such summary was set, or when it
     *     did expire (got cleaned)
     */
    @CheckForNull
    JsonValue get();

    /**
     * Set a new summary for a {@link org.hisp.dhis.scheduling.JobConfiguration} run.
     *
     * @param summary the value to store
     */
    void set(@Nonnull JsonValue summary);
  }
}
