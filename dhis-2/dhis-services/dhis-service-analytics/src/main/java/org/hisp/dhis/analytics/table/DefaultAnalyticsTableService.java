/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.getIndexes;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.util.PartitionUtils;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.Clock;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultAnalyticsTableService implements AnalyticsTableService {
  private final AnalyticsTableManager tableManager;

  private final OrganisationUnitService organisationUnitService;

  private final DataElementService dataElementService;

  private final ResourceTableService resourceTableService;

  private final SystemSettingManager systemSettingManager;

  @Override
  public AnalyticsTableType getAnalyticsTableType() {
    return tableManager.getAnalyticsTableType();
  }

  @Override
  public void create(AnalyticsTableUpdateParams params, JobProgress progress) {
    int parallelJobs = getParallelJobs();
    int tableUpdates = 0;

    log.info("Analytics table update parameters: {}", params);

    AnalyticsTableType tableType = getAnalyticsTableType();

    Clock clock =
        new Clock(log)
            .startClock()
            .logTime(
                "Starting update of type: {}, table name: '{}', parallel jobs: {}",
                tableType,
                tableType.getTableName(),
                parallelJobs);

    progress.startingStage("Validating analytics table: {}", tableType);
    String validState = tableManager.validState();
    progress.completedStage(validState);

    if (validState != null || progress.isCancelled()) {
      return;
    }

    List<AnalyticsTable> tables = tableManager.getAnalyticsTables(params);

    if (tables.isEmpty()) {
      clock.logTime(
          "Table update aborted, no table or partitions to be updated: '{}'",
          tableType.getTableName());
      progress.startingStage("Table updates " + tableType);
      progress.completedStage("Table updated aborted, no table or partitions to be updated");
      return;
    }

    clock.logTime(
        "Table update start: {}, earliest: {}, parameters: {}",
        tableType.getTableName(),
        getLongDateString(params.getFromDate()),
        params);
    progress.startingStage("Performing pre-create table work");
    progress.runStage(() -> tableManager.preCreateTables(params));
    clock.logTime("Performed pre-create table work " + tableType);

    progress.startingStage("Dropping staging tables (if any) " + tableType, tables.size());
    dropTables(tables, progress);
    clock.logTime("Dropped staging tables");

    progress.startingStage("Creating analytics tables " + tableType, tables.size());
    createTables(tables, progress);
    clock.logTime("Created analytics tables");

    List<AnalyticsTablePartition> partitions = PartitionUtils.getTablePartitions(tables);

    progress.startingStage("Populating analytics tables " + tableType, partitions.size());
    populateTables(params, partitions, progress);
    clock.logTime("Populated analytics tables");

    progress.startingStage("Invoking analytics table hooks " + tableType);
    tableUpdates += progress.runStage(0, tableManager::invokeAnalyticsTableSqlHooks);
    clock.logTime("Invoked analytics table hooks");

    tableUpdates += applyAggregationLevels(tableType, partitions, progress);
    clock.logTime("Applied aggregation levels");

    List<Index> indexes = getIndexes(partitions);
    progress.startingStage("Creating indexes " + tableType, indexes.size(), SKIP_ITEM_OUTLIER);
    createIndexes(indexes, progress);
    clock.logTime("Created indexes");

    if (tableUpdates > 0) {
      progress.startingStage("Vacuuming tables " + tableType, partitions.size());
      vacuumTables(partitions, progress);
      clock.logTime("Tables vacuumed");
    }

    progress.startingStage("Analyzing analytics tables " + tableType, partitions.size());
    analyzeTables(partitions, progress);
    clock.logTime("Analyzed tables");

    if (params.isLatestUpdate()) {
      progress.startingStage("Removing updated and deleted data " + tableType, SKIP_STAGE);
      progress.runStage(() -> tableManager.removeUpdatedData(tables));
      clock.logTime("Removed updated and deleted data");
    }

    swapTables(params, tables, progress);

    clock.logTime("Table update done: '{}'", tableType.getTableName());
  }

  @Override
  public void dropTables() {
    Set<String> tables = tableManager.getExistingDatabaseTables();

    tables.stream().forEach(tableManager::dropTable);

    log.info("Analytics tables dropped");
  }

  @Override
  public void analyzeAnalyticsTables() {
    Set<String> tables = tableManager.getExistingDatabaseTables();

    tables.forEach(tableManager::analyzeTable);

    log.info("Analytics tables analyzed");
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Drops the given analytics tables. */
  private void dropTables(List<AnalyticsTable> tables, JobProgress progress) {

    progress.runStage(tables, AnalyticsTable::getName, tableManager::dropTable);
  }

  /** Creates the given analytics tables. */
  private void createTables(List<AnalyticsTable> tables, JobProgress progress) {
    progress.runStage(tables, AnalyticsTable::getName, tableManager::createTable);
  }

  /** Populates the given analytics tables. */
  private void populateTables(
      AnalyticsTableUpdateParams params,
      List<AnalyticsTablePartition> partitions,
      JobProgress progress) {
    int parallelism = Math.min(getParallelJobs(), partitions.size());
    log.info("Populate table task number: " + parallelism);

    progress.runStageInParallel(
        parallelism,
        partitions,
        AnalyticsTablePartition::getName,
        partition -> tableManager.populateTablePartition(params, partition));
  }

  /**
   * Applies aggregation levels to the given analytics tables.
   *
   * @return the number of aggregation levels applied for data elements.
   */
  private int applyAggregationLevels(
      AnalyticsTableType tableType,
      List<AnalyticsTablePartition> partitions,
      JobProgress progress) {
    int maxLevels = organisationUnitService.getNumberOfOrganisationalLevels();

    int aggLevels = 0;

    for (int i = 0; i < maxLevels; i++) {
      int level = maxLevels - i;

      Collection<String> dataElements =
          IdentifiableObjectUtils.getUids(
              dataElementService.getDataElementsByAggregationLevel(level));

      if (!dataElements.isEmpty()) {
        progress.startingStage(
            "Applying aggregation level " + level + " " + tableType, partitions.size());
        progress.runStageInParallel(
            getParallelJobs(),
            partitions,
            AnalyticsTablePartition::getName,
            partition -> tableManager.applyAggregationLevels(partition, dataElements, level));

        aggLevels += dataElements.size();
      }
    }

    return aggLevels;
  }

  /** Creates indexes on the given analytics tables. */
  private void createIndexes(List<Index> indexes, JobProgress progress) {
    progress.runStageInParallel(
        getParallelJobs(), indexes, index -> index.getName(), tableManager::createIndex);
  }

  /** Vacuums the given analytics tables. */
  private void vacuumTables(List<AnalyticsTablePartition> partitions, JobProgress progress) {
    progress.runStageInParallel(
        getParallelJobs(), partitions, AnalyticsTablePartition::getName, tableManager::vacuumTable);
  }

  /** Analyzes the given analytics tables. */
  private void analyzeTables(List<AnalyticsTablePartition> partitions, JobProgress progress) {
    progress.runStageInParallel(
        getParallelJobs(),
        partitions,
        AnalyticsTablePartition::getName,
        tableManager::analyzeTable);
  }

  /**
   * Swaps the given analytics tables.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param tables the list of {@link AnalyticsTable}.
   */
  private void swapTables(
      AnalyticsTableUpdateParams params, List<AnalyticsTable> tables, JobProgress progress) {
    resourceTableService.dropAllSqlViews(progress);

    progress.startingStage("Swapping analytics tables " + getAnalyticsTableType(), tables.size());
    progress.runStage(
        tables, AnalyticsTable::getName, table -> tableManager.swapTable(params, table));

    resourceTableService.createAllSqlViews(progress);
  }

  /**
   * Returns the number of parallel jobs to use for processing analytics tables. The order of
   * determination is:
   *
   * <ul>
   *   <li>The system setting for parallel jobs in analytics table export, if set.
   *   <li>The system setting for number of available processors of the database server, if set.
   *   <li>The number of available processors of the application server, minus 1 if > 2.
   * </ul>
   *
   * @return the number of parallel jobs to use for processing analytics tables.
   */
  int getParallelJobs() {
    Integer parallelJobs =
        systemSettingManager.getIntegerSetting(SettingKey.PARALLEL_JOBS_IN_ANALYTICS_TABLE_EXPORT);
    Integer databaseCpus = systemSettingManager.getIntegerSetting(SettingKey.DATABASE_SERVER_CPUS);
    int serverCpus = SystemUtils.getCpuCores();

    if (parallelJobs != null && parallelJobs > 0) {
      return parallelJobs;
    }

    if (databaseCpus != null && databaseCpus > 0) {
      return databaseCpus;
    }

    if (serverCpus > 2) {
      return serverCpus - 1;
    }

    return serverCpus;
  }
}
