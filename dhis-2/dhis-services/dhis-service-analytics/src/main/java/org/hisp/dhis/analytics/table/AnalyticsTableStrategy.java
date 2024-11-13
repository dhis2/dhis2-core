/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.table;

import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.scheduling.JobProgress;

/**
 * Interface for analytics table generation.
 *
 * <p>The interface provides a set of methods that can be implemented to generate analytics tables,
 * indexes, and perform other operations related to analytics tables.
 *
 * <p>
 */
public interface AnalyticsTableStrategy {

  /**
   * Execute some sanity checks to ensure that the database is in a valid state for analytics table
   * generation. The checks are dependent on the table type.
   *
   * @param tableType the table type
   * @return true if the state is valid, false otherwise
   */
  default boolean validateState(AnalyticsTableType tableType) {
    return true;
  }

  default TableStrategyOpResult preCreateTables(
      AnalyticsTableUpdateParams params, JobProgress progress) {
    return TableStrategyOpResult.SKIPPED;
  }

  default TableStrategyOpResult dropStagingTables(
      List<AnalyticsTable> tables, JobProgress progress) {
    return TableStrategyOpResult.SKIPPED;
  }

  default TableStrategyOpResult createTables(List<AnalyticsTable> tables, JobProgress progress) {
    return TableStrategyOpResult.SKIPPED;
  }

  default TableStrategyOpResult populateTables(
      AnalyticsTableUpdateParams params,
      List<AnalyticsTablePartition> tables,
      JobProgress progress) {
    return TableStrategyOpResult.SKIPPED;
  }

  default int applyAggregationLevels(
      AnalyticsTableType tableType, List<? extends Table> tables, JobProgress progress) {
    return 0;
  }

  default TableStrategyOpResult createIndexes(
      List<Index> tables, JobProgress progress, AnalyticsTableType tableType) {
    return TableStrategyOpResult.SKIPPED;
  }

  default TableStrategyOpResult optimizeTables(List<AnalyticsTable> tables, JobProgress progress) {
    return TableStrategyOpResult.SKIPPED;
  }

  default TableStrategyOpResult analyzeTables(List<AnalyticsTable> tables, JobProgress progress) {
    return TableStrategyOpResult.SKIPPED;
  }

  default TableStrategyOpResult swapTables(
      AnalyticsTableUpdateParams params,
      List<AnalyticsTable> tables,
      JobProgress progress,
      AnalyticsTableType tableType) {
    return TableStrategyOpResult.SKIPPED;
  }
}
