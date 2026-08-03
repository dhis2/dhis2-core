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

/**
 * Persistence for the {@code useractivity} table (one row per user per calendar day) and its daily
 * rollup.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface UserActivityStore {

  /**
   * Inserts or refreshes the activity row for the given user and day, setting {@code lastactive} to
   * the given timestamp.
   *
   * @param username the active user
   * @param date the calendar day
   * @param lastActive the activity timestamp
   */
  void upsertActivity(String username, LocalDate date, Date lastActive);

  /**
   * Counts distinct users whose last recorded activity is on or after each of the given timestamps.
   * All counts are computed in a single query.
   *
   * @param sinceDates the cutoff timestamps
   * @return counts in the same order as the given timestamps
   */
  List<Integer> getActiveUserCounts(List<Date> sinceDates);

  /**
   * Returns distinct active users per calendar day for the given window, combining the daily rollup
   * (for pruned days) with live counts over raw rows.
   *
   * @param startDate inclusive start of the window
   * @param endDate exclusive end of the window
   * @return one entry per day that has data, ordered by date ascending
   */
  List<DailyActiveUsers> getDailyStatistics(Date startDate, Date endDate);

  /**
   * Upserts daily rollup rows for every calendar day with raw rows strictly older than {@code
   * olderThan}.
   *
   * @param olderThan exclusive upper bound on {@code activitydate}
   * @return number of calendar days rolled up
   */
  int rollupOlderThan(Date olderThan);

  /**
   * Deletes raw activity rows with {@code activitydate} strictly older than {@code olderThan}.
   *
   * @param olderThan exclusive upper bound
   * @return number of rows deleted
   */
  int deleteOlderThan(Date olderThan);
}
