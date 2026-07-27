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
package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Date;
import org.junit.jupiter.api.Test;

class DataExportParamsTest {

  @Test
  void testHasPeriodFilters_NoFiltersFalse() {
    DataExportParams params = DataExportParams.builder().build();

    assertFalse(params.hasPeriodFilters());
  }

  @Test
  void testHasPeriodFilters_LastUpdatedAloneIsNotAPeriodFilter() {
    // lastUpdated/lastUpdatedDuration do not filter by the period table, so they must not
    // trigger the pe_ids CTE/JOIN the query builder erases based on hasPeriodFilters().
    DataExportParams params =
        DataExportParams.builder()
            .lastUpdated(new Date())
            .lastUpdatedDuration(Duration.ofDays(10000))
            .build();

    assertFalse(params.hasPeriodFilters());
  }

  @Test
  void testHasLastUpdatedFilters_NoFiltersFalse() {
    DataExportParams params = DataExportParams.builder().build();

    assertFalse(params.hasLastUpdatedFilters());
  }

  @Test
  void testHasLastUpdatedFilters_LastUpdatedDurationTrue() {
    DataExportParams params =
        DataExportParams.builder().lastUpdatedDuration(Duration.ofDays(10000)).build();

    assertTrue(params.hasLastUpdatedFilters());
  }

  @Test
  void testHasLastUpdatedFilters_LastUpdatedTrue() {
    DataExportParams params = DataExportParams.builder().lastUpdated(new Date()).build();

    assertTrue(params.hasLastUpdatedFilters());
  }
}
