/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.user;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.scheduling.JobProgress;

/**
 * Tracks per-day user activity across all authenticated clients (web, Android, API) and exposes
 * daily-active-user statistics. The write path is guarded by a short-TTL per-user cache so the cost
 * is roughly one small upsert per user per cache window.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface UserActivityService {

  /**
   * Marks the given user as active now. Cache-guarded and failure-safe: never throws to the caller,
   * so it is safe on the request hot path.
   *
   * @param username the active user
   */
  void recordActivity(String username);

  /**
   * Counts distinct users active on or after each of the given timestamps, in one query.
   *
   * @param sinceDates the cutoff timestamps
   * @return counts in the same order as the given timestamps
   */
  List<Integer> getActiveUsersCounts(List<Date> sinceDates);

  /**
   * Returns distinct active users per calendar day for the given inclusive date range.
   *
   * @param startDate inclusive start day
   * @param endDate inclusive end day
   * @return one entry per day that has data, ordered by date ascending
   */
  List<DailyActiveUsers> getDailyStatistics(LocalDate startDate, LocalDate endDate);

  /**
   * Rolls raw activity rows older than the retention window into the daily aggregate table and then
   * deletes them.
   *
   * @param retentionDays number of days of raw rows to keep
   * @param progress job progress tracker
   */
  void cleanup(int retentionDays, JobProgress progress);
}
