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

import java.util.Deque;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobKey;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SystemSettings;

/**
 * @author Lars Helge Overland
 * @author Jan Bernitt (pulled up default methods)
 */
public interface Notifier {

  default Notifier notify(JobConfiguration id, String message) {
    return notify(id, NotificationLevel.INFO, message, false);
  }

  default Notifier notify(JobKey key, String message) {
    return notify(key, NotificationLevel.INFO, message, false);
  }

  default Notifier notify(JobConfiguration id, @Nonnull NotificationLevel level, String message) {
    return notify(id, level, message, false);
  }

  default Notifier notify(JobKey key, @Nonnull NotificationLevel level, String message) {
    return notify(key, level, message, false);
  }

  default Notifier notify(JobConfiguration id, String message, boolean completed) {
    return notify(id, NotificationLevel.INFO, message, completed);
  }

  default Notifier notify(JobKey key, String message, boolean completed) {
    return notify(key, NotificationLevel.INFO, message, completed);
  }

  default Notifier notify(
      JobConfiguration id, @Nonnull NotificationLevel level, String message, boolean completed) {
    return notify(id.toKey(), level, message, completed, null, null);
  }

  default Notifier notify(
      JobKey key, @Nonnull NotificationLevel level, String message, boolean completed) {
    return notify(key, level, message, completed, null, null);
  }

  Notifier notify(
      JobKey key,
      @Nonnull NotificationLevel level,
      String message,
      boolean completed,
      NotificationDataType dataType,
      JsonValue data);

  /**
   * @param gist when true, only the first and last message are included for each job. When {@code
   *     null} the {@link SystemSettings#getNotifierGistOverview()} is used.
   * @return a map with notifications for all job types and jobs
   */
  Map<JobType, Map<String, Deque<Notification>>> getNotifications(@CheckForNull Boolean gist);

  Deque<Notification> getNotificationsByJobId(JobType jobType, String jobId);

  /**
   * @param jobType include jobs of this type in the result
   * @param gist when true, only the first and last message are included for each job. When {@code
   *     null} the {@link SystemSettings#getNotifierGistOverview()} is used.
   * @return a map with notifications for all jobs of the provided type
   */
  Map<String, Deque<Notification>> getNotificationsByJobType(
      JobType jobType, @CheckForNull Boolean gist);

  default <T> Notifier addJobSummary(JobConfiguration job, T summary, Class<T> type) {
    return addJobSummary(job, NotificationLevel.INFO, summary, type);
  }

  default <T> Notifier addJobSummary(JobKey job, T summary, Class<T> type) {
    return addJobSummary(job, NotificationLevel.INFO, summary, type);
  }

  default <T> Notifier addJobSummary(
      JobConfiguration job, NotificationLevel level, T summary, Class<T> type) {
    return addJobSummary(job == null ? null : job.toKey(), level, summary, type);
  }

  <T> Notifier addJobSummary(JobKey job, NotificationLevel level, T summary, Class<T> type);

  Map<String, JsonValue> getJobSummariesForJobType(JobType jobType);

  JsonValue getJobSummaryByJobId(JobType jobType, String jobId);

  /**
   * @since 2.42
   * @return true, when currently no messages are being processed and no automatic cleanup is
   *     running in the background.
   */
  boolean isIdle();

  /*
  Cleanup API
   */

  /**
   * Removes all data for the specified job (both notifications and summary)
   *
   * @param type of the job to clear
   * @param job ID of the job to clear
   * @since 2.42
   */
  void clear(@Nonnull JobType type, @Nonnull UID job);

  /**
   * Removes all data for all jobs of the specified type.
   *
   * @param type of the jobs to clear
   * @since 2.42
   */
  void clear(@Nonnull JobType type);

  /**
   * Removes all data (of all jobs and job types).
   *
   * @since 2.42
   */
  void clear();

  /**
   * Removes all data for jobs of the given type unless they are younger than the given max age in
   * days
   *
   * @param maxAge keep the data for jobs from the most recent days
   * @param type of jobs to check
   * @since 2.42
   */
  void capMaxAge(int maxAge, @Nonnull JobType type);

  /**
   * Removes all data unless the job is younger than the given max age in days
   *
   * @param maxAge keep the data for jobs from the most recent days
   * @since 2.42
   */
  void capMaxAge(int maxAge);

  /**
   * Removes all data for jobs of the given type except for the most recent n ones.
   *
   * @param maxCount number to keep (most recent first)
   * @param type of jobs to check
   * @since 2.42
   */
  void capMaxCount(int maxCount, @Nonnull JobType type);

  /**
   * Removes all data for jobs except for the most recent n ones for each type.
   *
   * @param maxCount number to keep (most recent first)
   * @since 2.42
   */
  void capMaxCount(int maxCount);

  /**
   * For backwards compatibility (not having to update all callers)
   *
   * @param key the job to clear all data for
   * @return itself for chaining
   */
  default Notifier clear(@Nonnull JobKey key) {
    clear(key.type(), key.id());
    return this;
  }
}
