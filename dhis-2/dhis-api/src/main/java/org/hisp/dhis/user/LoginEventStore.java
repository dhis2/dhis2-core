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
import org.hisp.dhis.common.GenericStore;

/**
 * Persistence for {@link LoginEvent} rows and their daily rollup.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface LoginEventStore extends GenericStore<LoginEvent> {

  /**
   * Aggregates login events between the given dates into one row per calendar day.
   *
   * @param startDate inclusive start of the window
   * @param endDate exclusive end of the window
   * @return daily statistics ordered by date ascending
   */
  List<DailyLoginStatistics> getDailyStatistics(Date startDate, Date endDate);

  /**
   * Upserts daily rollup rows covering every calendar day that has raw login events strictly older
   * than {@code olderThan}. Existing rollup rows are overwritten with the recomputed counts.
   *
   * @param olderThan exclusive upper bound on the raw event timestamp
   * @return number of calendar days rolled up
   */
  int rollupOlderThan(Date olderThan);

  /**
   * Deletes raw login event rows with timestamp strictly older than {@code olderThan}.
   *
   * @param olderThan exclusive upper bound
   * @return number of rows deleted
   */
  int deleteOlderThan(Date olderThan);

  /**
   * Returns the earliest date covered by the daily rollup table, or null if empty.
   *
   * @return the earliest rollup date, or null
   */
  LocalDate getEarliestRollupDate();
}
