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

import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.getIndexName;
import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.getIndexes;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsIndex;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.dataelement.DataElementService;
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
@AllArgsConstructor
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
  public void update(AnalyticsTableUpdateParams params, JobProgress progress) {
    int processNo = getProcessNo();

    int tableUpdates = 0;

    log.info(String.format("Analytics table update parameters: %s", params));

    AnalyticsTableType tableType = getAnalyticsTableType();

    Clock clock =
        new Clock(log)
            .startClock()
            .logTime(
                String.format(
                    "Starting update of type: %s, table name: '%s', processes: %d",
                    tableType, tableType.getTableName(), processNo));

    progress.startingStage("Validating Analytics Table " + tableType);
    String validState = tableManager.validState();
    progress.completedStage(validState);
    if (validState != null || progress.isCancellationRequested()) {
      return;
    }

    List<AnalyticsTable> tables = tableManager.getAnalyticsTables(params);

    if (tables.isEmpty()) {
      clock.logTime(
          String.format(
              "Table update aborted, no table or partitions to be updated: '%s'",
              tableType.getTableName()));
      progress.startingStage("Table updates " + tableType);
      progress.completedStage("Table updated aborted, no table or partitions to be updated");
      return;
    }

    clock.logTime(
        String.format(
            "Table update start: %s, earliest: %s, parameters: %s",
            tableType.getTableName(), getLongDateString(params.getFromDate()), params));
    progress.startingStage("Performing pre-create table work");
    progress.runStage(() -> tableManager.preCreateTables(params));
    clock.logTime("Performed pre-create table work " + tableType);

    progress.startingStage("Dropping temp tables (if any) " + tableType, tables.size());
    dropAllTempTables(progress, tables);
    clock.logTime("Dropped temp tables");

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

    if (tableUpdates > 0) {
      progress.startingStage("Vacuuming tables " + tableType, partitions.size());
      vacuumTables(partitions, progress);
      clock.logTime("Tables vacuumed");
    }

    List<AnalyticsIndex> indexes = getIndexes(partitions);
    progress.startingStage("Creating indexes " + tableType, indexes.size());
    createIndexes(indexes, progress);
    clock.logTime("Created indexes");

    progress.startingStage("Analyzing analytics tables " + tableType, partitions.size());
    analyzeTables(partitions, progress);
    clock.logTime("Analyzed tables");

    if (params.isLatestUpdate()) {
      progress.startingStage("Removing updated and deleted data " + tableType);
      progress.runStage(() -> tableManager.removeUpdatedData(tables));
      clock.logTime("Removed updated and deleted data");
    }

    swapTables(params, tables, progress);

    clock.logTime("Table update done: " + tableType.getTableName());
  }

  @Override
  public void dropTables() {
    Set<String> tables = tableManager.getExistingDatabaseTables();

    tables.forEach(tableManager::dropTableCascade);

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
   * Drops all temporary tables, including the ones used as partitions.
   *
   * @param progress
   * @param tables
   */
  private void dropAllTempTables(final JobProgress progress, final List<AnalyticsTable> tables) {
    dropTempTables(tables, progress);
  }

  /** Drops the given temporary analytics tables. */
  private void dropTempTables(final List<AnalyticsTable> tables, final JobProgress progress) {
    progress.runStage(tables, AnalyticsTable::getTableName, tableManager::dropTempTable);
  }

  /** Creates the given analytics tables. */
  private void createTables(List<AnalyticsTable> tables, JobProgress progress) {
    progress.runStage(tables, AnalyticsTable::getTableName, tableManager::createTable);
  }

  /** Populates the given analytics tables. */
  private void populateTables(
      AnalyticsTableUpdateParams params,
      List<AnalyticsTablePartition> partitions,
      JobProgress progress) {
    int parallelism = Math.min(getProcessNo(), partitions.size());
    log.info("Populate table task number: " + parallelism);

    progress.runStageInParallel(
        parallelism,
        partitions,
        AnalyticsTablePartition::getTableName,
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
            getProcessNo(),
            partitions,
            AnalyticsTablePartition::getTableName,
            partition -> tableManager.applyAggregationLevels(partition, dataElements, level));

        aggLevels += dataElements.size();
      }
    }

    return aggLevels;
  }

  /** Vacuums the given analytics tables. */
  private void vacuumTables(List<AnalyticsTablePartition> partitions, JobProgress progress) {
    progress.runStageInParallel(
        getProcessNo(),
        partitions,
        AnalyticsTablePartition::getTableName,
        tableManager::vacuumTables);
  }

  /** Creates indexes on the given analytics tables. */
  private void createIndexes(List<AnalyticsIndex> indexes, JobProgress progress) {
    AnalyticsTableType type = getAnalyticsTableType();
    log.info("No of analytics table indexes: " + indexes.size());
    progress.runStageInParallel(
        getProcessNo(),
        indexes,
        index -> getIndexName(index, type).replace("\"", ""),
        tableManager::createIndex);
  }

  /** Analyzes the given analytics tables. */
  private void analyzeTables(List<AnalyticsTablePartition> partitions, JobProgress progress) {
    progress.runStage(
        partitions,
        AnalyticsTablePartition::getTableName,
        table -> tableManager.analyzeTable(table.getTempTableName()));
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
        tables, AnalyticsTable::getTableName, table -> tableManager.swapTable(params, table));

    resourceTableService.createAllSqlViews(progress);
  }

  /**
   * Gets the number of available cores. Uses explicit number from system setting if available.
   * Detects number of cores from current server runtime if not. Subtracts one to the number of
   * cores if greater than two to allow one core for general system operations.
   */
  private int getProcessNo() {
    Integer cores = systemSettingManager.getIntegerSetting(SettingKey.DATABASE_SERVER_CPUS);

    cores = (cores == null || cores == 0) ? SystemUtils.getCpuCores() : cores;

    return cores > 2 ? (cores - 1) : cores;
  }
}
