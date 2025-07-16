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
package org.hisp.dhis.analytics.table;

import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.getIndexes;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;
import static org.hisp.dhis.util.DateUtils.toLongDate;
import static org.hisp.dhis.util.TextUtils.format;

import java.util.ArrayList;
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
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
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

  private final SystemSettingsProvider settingsProvider;

  private final SqlBuilder sqlBuilder;

  @Override
  public AnalyticsTableType getAnalyticsTableType() {
    return tableManager.getAnalyticsTableType();
  }

  @Override
  public void create(AnalyticsTableUpdateParams params, JobProgress progress) {
    final int parallelJobs = getParallelJobs();
    int tableUpdates = 0;

    log.info("Analytics table update parameters: {}", params);

    AnalyticsTableType tableType = getAnalyticsTableType();

    Clock clock = new Clock(log).startClock();
    clock.logTime(
        "Starting update of type: {}, table name: '{}', parallel jobs: {}",
        tableType,
        tableType.getTableName(),
        parallelJobs);

    progress.startingStage("Validating analytics table: {}", tableType);
    boolean validState = tableManager.validState();
    progress.completedStage("Validated analytics tables with outcome: {}", validState);

    if (!validState || progress.isCancelled()) {
      return;
    }

    List<AnalyticsTable> tables = tableManager.getAnalyticsTables(params);

    if (tables.isEmpty()) {
      clock.logTime("Table update aborted, nothing to update: '{}'", tableType.getTableName());
      progress.startingStage("Table update of type: '{}'", tableType);
      progress.completedStage("Table updated aborted, no table or partitions to be updated");
      return;
    }

    clock.logTime(
        "Table update start: {}, earliest: {}, parameters: {}",
        tableType.getTableName(),
        toLongDate(params.getFromDate()),
        params);
    progress.startingStage("Performing pre-create table work");
    progress.runStage(() -> tableManager.preCreateTables(params));
    clock.logTime("Performed pre-create table work: '{}'", tableType);

    progress.startingStage(format("Dropping staging tables: '{}'", tableType), tables.size());
    dropTables(tables, progress);
    clock.logTime("Dropped staging tables");

    progress.startingStage(format("Creating analytics tables: '{}'", tableType), tables.size());
    createTables(tables, progress);
    clock.logTime("Created analytics tables");

    List<AnalyticsTablePartition> partitions = getTablePartitions(tables);
    int partitionSize = partitions.size();

    progress.startingStage(
        format("Populating {} analytics tables: '{}'", partitionSize, tableType), partitionSize);
    populateTables(params, partitions, progress);
    clock.logTime("Populated analytics tables");

    progress.startingStage("Invoking analytics table hooks: '{}'", tableType);
    tableUpdates += progress.runStage(0, tableManager::invokeAnalyticsTableSqlHooks);
    clock.logTime("Invoked analytics table hooks");

    tableUpdates += applyAggregationLevels(tableType, partitions, progress);
    clock.logTime("Applied aggregation levels");

    if (sqlBuilder.requiresIndexesForAnalytics()) {
      List<Index> indexes = getIndexes(partitions);
      int indexSize = indexes.size();
      progress.startingStage(
          format("Creating {} indexes: '{}'", indexSize, tableType), indexSize, SKIP_ITEM_OUTLIER);
      createIndexes(indexes, progress);
      clock.logTime("Created indexes");
    }

    if (tableUpdates > 0 && sqlBuilder.supportsVacuum()) {
      progress.startingStage(format("Vacuuming tables: '{}'", tableType), partitions.size());
      vacuumTables(partitions, progress);
      clock.logTime("Tables vacuumed");
    }

    if (sqlBuilder.supportsAnalyze()) {
      progress.startingStage(
          format("Analyzing analytics tables: '{}'", tableType), partitions.size());
      analyzeTables(partitions, progress);
      clock.logTime("Analyzed tables");
    }

    if (params.isLatestUpdate()) {
      progress.startingStage(
          format("Removing updated and deleted data: '{}'", tableType), SKIP_STAGE);
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
   * Creates the given analytics tables.
   *
   * @param tables the list of {@link AnalyticsTable}.
   * @param progress the {@link JobProgress}.
   */
  private void createTables(List<AnalyticsTable> tables, JobProgress progress) {
    progress.runStage(tables, AnalyticsTable::getName, tableManager::createTable);
  }

  /**
   * Populates the given analytics tables.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param partitions the {@link AnalyticsTablePartition}.
   * @param progress the {@link JobProgress}.
   */
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
        partition -> tableManager.populateTable(params, partition));
  }

  /**
   * Applies aggregation levels to the given analytics tables.
   *
   * @param tableType the {@link AnalyticsTableType}.
   * @param tables the list of {@link Table}.
   * @param progress the {@link JobProgress}.
   * @return the number of aggregation levels applied for data elements.
   */
  private int applyAggregationLevels(
      AnalyticsTableType tableType, List<? extends Table> tables, JobProgress progress) {
    int maxLevels = organisationUnitService.getNumberOfOrganisationalLevels();

    int aggLevels = 0;

    for (int i = 0; i < maxLevels; i++) {
      int level = maxLevels - i;

      List<String> dataElements =
          IdentifiableObjectUtils.getUids(
              dataElementService.getDataElementsByAggregationLevel(level));

      if (!dataElements.isEmpty()) {
        progress.startingStage(
            format("Applying aggregation level {}: '{}'", level, tableType), tables.size());
        progress.runStageInParallel(
            getParallelJobs(),
            tables,
            Table::getName,
            partition -> tableManager.applyAggregationLevels(partition, dataElements, level));

        aggLevels += dataElements.size();
      }
    }

    return aggLevels;
  }

  /**
   * Creates indexes on the given tables.
   *
   * @param indexes the list of {@link Index}.
   * @param progress the {@link JobProgress}.
   */
  private void createIndexes(List<Index> indexes, JobProgress progress) {
    progress.runStageInParallel(
        getParallelJobs(), indexes, index -> index.getName(), tableManager::createIndex);
  }

  /**
   * Vacuums the given tables.
   *
   * @param tables the list of {@link Table}.
   * @param progress the {@link JobProgress}.
   */
  private void vacuumTables(List<? extends Table> tables, JobProgress progress) {
    progress.runStageInParallel(
        getParallelJobs(), tables, Table::getName, tableManager::vacuumTable);
  }

  /**
   * Analyzes the given tables.
   *
   * @param tables the list of {@link Table}.
   * @param progress the {@link JobProgress}.
   */
  private void analyzeTables(List<? extends Table> tables, JobProgress progress) {
    progress.runStageInParallel(
        getParallelJobs(), tables, Table::getName, tableManager::analyzeTable);
  }

  /**
   * Swaps the given analytics tables.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param tables the list of {@link AnalyticsTable}.
   * @param progress the {@link JobProgress}.
   */
  private void swapTables(
      AnalyticsTableUpdateParams params, List<AnalyticsTable> tables, JobProgress progress) {
    resourceTableService.dropAllSqlViews(progress);

    progress.startingStage(
        format("Swapping analytics tables: '{}'", getAnalyticsTableType()), tables.size());
    progress.runStage(
        tables, AnalyticsTable::getName, table -> tableManager.swapTable(params, table));

    resourceTableService.createAllSqlViews(progress);
  }

  /**
   * Returns a list of table partitions based on the given analytics tables. For master tables with
   * no partitions, a fake partition representing the master table is used.
   *
   * @param tables the list of {@link AnalyticsTable}.
   * @return a list of {@link AnalyticsTablePartition}.
   */
  List<AnalyticsTablePartition> getTablePartitions(List<AnalyticsTable> tables) {
    List<AnalyticsTablePartition> partitions = new ArrayList<>();

    for (AnalyticsTable table : tables) {
      if (table.hasTablePartitions() && !sqlBuilder.supportsDeclarativePartitioning()) {
        partitions.addAll(table.getTablePartitions());
      } else {
        // Fake partition representing the master table
        partitions.add(new AnalyticsTablePartition(table));
      }
    }

    return partitions;
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
    SystemSettings settings = settingsProvider.getCurrentSettings();
    int parallelJobs = settings.getParallelJobsInAnalyticsTableExport();
    if (parallelJobs > 0) {
      return parallelJobs;
    }
    int databaseCpus = settings.getDatabaseServerCpus();
    if (databaseCpus > 0) {
      return databaseCpus;
    }
    int serverCpus = SystemUtils.getCpuCores();
    if (serverCpus > 2) {
      return serverCpus - 1;
    }
    return serverCpus;
  }
}
