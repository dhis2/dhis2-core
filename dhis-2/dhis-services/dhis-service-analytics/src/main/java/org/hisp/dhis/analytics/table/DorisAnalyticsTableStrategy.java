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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.scheduling.JobProgress;

@RequiredArgsConstructor
@Slf4j
public class DorisAnalyticsTableStrategy implements AnalyticsTableStrategy {

  private final AnalyticsTableManager tableManager;
  private final OrganisationUnitService organisationUnitService;
  private final DataElementService dataElementService;
  private final ResourceTableService resourceTableService;
  private final int parallelJobs;

  @Override
  public boolean validateState(AnalyticsTableType tableType) {
    return tableManager.validState();
  }

  @Override
  public TableStrategyOpResult preCreateTables(
      AnalyticsTableUpdateParams params, JobProgress progress) {
    progress.runStage(() -> tableManager.preCreateTables(params));
    return TableStrategyOpResult.EXECUTED;
  }

  @Override
  public TableStrategyOpResult dropStagingTables(
      List<AnalyticsTable> tables, JobProgress progress) {
    dropTables(tables, progress);
    return TableStrategyOpResult.EXECUTED;
  }

  @Override
  public TableStrategyOpResult createTables(List<AnalyticsTable> tables, JobProgress progress) {
    progress.runStage(tables, AnalyticsTable::getName, tableManager::createTable);
    return TableStrategyOpResult.EXECUTED;
  }

  @Override
  public TableStrategyOpResult populateTables(
      AnalyticsTableUpdateParams params,
      List<AnalyticsTablePartition> tables,
      JobProgress progress) {
    doPopulateTables(params, tables, progress);
    return TableStrategyOpResult.EXECUTED;
  }

  @Override
  public int applyAggregationLevels(
      AnalyticsTableType tableType, List<? extends Table> tables, JobProgress progress) {
    return doApplyAggregationLevels(tableType, tables, progress);
  }

  @Override
  public TableStrategyOpResult swapTables(
      AnalyticsTableUpdateParams params,
      List<AnalyticsTable> tables,
      JobProgress progress,
      AnalyticsTableType tableType) {

    doSwapTables(params, tables, progress, tableType);
    return TableStrategyOpResult.EXECUTED;
  }

  /**
   * Drops the given analytics tables.
   *
   * @param tables the list of {@link AnalyticsTable}.
   * @param progress the {@link JobProgress}.
   */
  private void dropTables(List<AnalyticsTable> tables, JobProgress progress) {
    progress.runStage(tables, AnalyticsTable::getName, tableManager::dropTable);
  }

  /**
   * Populates the given analytics tables.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param partitions the {@link AnalyticsTablePartition}.
   * @param progress the {@link JobProgress}.
   */
  private void doPopulateTables(
      AnalyticsTableUpdateParams params,
      List<AnalyticsTablePartition> partitions,
      JobProgress progress) {
    int parallelism = Math.min(this.parallelJobs, partitions.size());
    log.info("Populate table task number: " + parallelism);

    progress.runStageInParallel(
        parallelism,
        partitions,
        AnalyticsTablePartition::getName,
        partition -> tableManager.populateTable(params, partition));
  }

  private int doApplyAggregationLevels(
      AnalyticsTableType tableType, List<? extends Table> tables, JobProgress progress) {
    // TODO eliminate dep on organisationUnitService
    int maxLevels = organisationUnitService.getNumberOfOrganisationalLevels();

    int aggLevels = 0;
    // TODO can this be optimized by using a single query that takes all the levels?
    // e.g SELECT * FROM dataelement WHERE aggregationLevel IN (1, 2, 3, 4, 5)
    for (int i = 0; i < maxLevels; i++) {
      int level = maxLevels - i;

      List<String> dataElements =
          IdentifiableObjectUtils.getUids(
              // TODO eliminate dep on dataElementService
              dataElementService.getDataElementsByAggregationLevel(level));

      if (!dataElements.isEmpty()) {
        progress.startingStage(
            "Applying aggregation level " + level + " " + tableType, tables.size());
        progress.runStageInParallel(
            this.parallelJobs,
            tables,
            Table::getName,
            partition -> tableManager.applyAggregationLevels(partition, dataElements, level));

        aggLevels += dataElements.size();
      }
    }

    return aggLevels;
  }

  /**
   * Swaps the given analytics tables.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param tables the list of {@link AnalyticsTable}.
   * @param progress the {@link JobProgress}.
   * @param tableType
   */
  private void doSwapTables(
      AnalyticsTableUpdateParams params,
      List<AnalyticsTable> tables,
      JobProgress progress,
      AnalyticsTableType tableType) {

    progress.startingStage("Swapping analytics tables " + tableType, tables.size());
    progress.runStage(
        tables, AnalyticsTable::getName, table -> tableManager.swapTable(params, table));
  }
}
