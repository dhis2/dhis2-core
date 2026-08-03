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
import java.util.List;
import org.hisp.dhis.scheduling.JobProgress;

/**
 * Records successful authentications and exposes login statistics.
 *
 * <p>Write path is short-window deduped per (username, authType) so per-request schemes like PAT
 * and basic auth do not flood the raw table. The cleanup job rolls raw rows older than the
 * retention window into a daily aggregate and then prunes them.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface LoginEventService {

  /**
   * Records a successful authentication if the short-window dedup cache does not already hold an
   * entry for the same (username, authType). Safe to call from hot paths; never throws to the
   * caller (failures are logged).
   *
   * @param username the authenticated username
   * @param authType the authentication method
   */
  void recordLogin(String username, LoginAuthType authType);

  /**
   * Returns login statistics per calendar day for the given inclusive date range. Combines daily
   * rollup rows (for dates already cleaned up) with live aggregates over remaining raw events.
   *
   * @param startDate inclusive start day
   * @param endDate inclusive end day
   * @return one entry per day that has data, ordered by date ascending
   */
  List<DailyLoginStatistics> getDailyStatistics(LocalDate startDate, LocalDate endDate);

  /**
   * Rolls raw login events older than the retention window into the daily aggregate table and then
   * deletes those raw rows.
   *
   * @param retentionDays number of days of raw events to keep
   * @param progress job progress tracker
   */
  void cleanup(int retentionDays, JobProgress progress);
}
