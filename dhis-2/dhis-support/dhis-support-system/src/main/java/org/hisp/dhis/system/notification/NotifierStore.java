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

import java.util.List;
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
 */
public interface NotifierStore {

  @Nonnull
  NotificationStore getNotificationStore(@Nonnull JobType type, @Nonnull UID job);

  /**
   * @param type for which to list existing {@link NotificationStore}s
   * @return a list containing all existing {@link NotificationStore}s for a specific job.
   */
  @Nonnull
  List<? extends NotificationStore> getNotificationStores(@Nonnull JobType type);

  @Nonnull
  SummaryStore getSummaryStore(@Nonnull JobType type, @Nonnull UID job);

  @Nonnull
  List<? extends SummaryStore> getSummaryStores(@Nonnull JobType type);

  void capStoresByCount(int n, @Nonnull JobType type);

  void capStoresByAge(int days, @Nonnull JobType type);

  void clearStore(@Nonnull JobType type, @Nonnull UID job);

  default void clearStore(@Nonnull JobType type) {
    capStoresByCount(0, type);
  }

  default void clearStores() {
    capStoresByCount(0);
  }

  /**
   * Retains the n stores for each {@link JobType} which have the most recent notifications. Must
   * only be called when not calling {@link NotificationStore#add(Notification)} concurrently.
   *
   * @param n number of stores to retain
   */
  default void capStoresByCount(int n) {
    Stream.of(JobType.values()).forEach(type -> capStoresByCount(n, type));
  }

  default void capStoresByAge(int days) {
    Stream.of(JobType.values()).forEach(type -> capStoresByAge(days, type));
  }

  sealed interface PerJobStore {

    JobType type();

    UID job();

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

    int size();

    void removeNewest();

    /**
     * This does however retain the very first message.
     *
     * @param n number of messages to remove from the start (oldest) (but not the 1st)
     */
    void removeOldest(int n);

    void add(@Nonnull Notification n);

    @CheckForNull
    Notification getNewest();

    @CheckForNull
    Notification getOldest();

    @Nonnull
    Stream<Notification> listNewestFirst();

    default long ageTimestamp() {
      Notification newest = getNewest();
      // removing empty collections is fine
      // because this never happens while adding
      return newest == null ? 0L : newest.getTime().getTime();
    }
  }

  non-sealed interface SummaryStore extends PerJobStore {

    /**
     * @return the stored summary value, or {@code null} when no such summary was set, or it did
     *     expire
     */
    @CheckForNull
    JsonValue get();

    void set(@Nonnull JsonValue value);
  }
}
