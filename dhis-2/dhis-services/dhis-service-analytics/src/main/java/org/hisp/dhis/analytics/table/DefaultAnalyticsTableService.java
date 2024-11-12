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
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;
import static org.hisp.dhis.util.DateUtils.toLongDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.dataelement.DataElementService;
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

  private AnalyticsTableStrategy analyticsTableStrategy;

  private int parallelJobs;

  @PostConstruct
  void postConstruct() {
    // we can use a standard constructor as well (@Maikel preferences?)
    this.parallelJobs = getParallelJobs();
    this.analyticsTableStrategy = getTableStrategy(parallelJobs);
  }

  @Override
  public AnalyticsTableType getAnalyticsTableType() {
    return tableManager.getAnalyticsTableType();
  }

  @Override
  public void create(AnalyticsTableUpdateParams params, JobProgress progress) {

    int tableUpdates = 0;

    log.info("Analytics table update parameters: {}", params);

    AnalyticsTableType tableType = getAnalyticsTableType();

    Clock clock = startClock(tableType, parallelJobs);

    progress.startingStage("Validating analytics table: {}", tableType);
    boolean validState = analyticsTableStrategy.validateState(progress, tableType);
    progress.completedStage("Validated analytics tables with outcome: {}", validState);

    if (!validState || progress.isCancelled()) {
      return;
    }

    List<AnalyticsTable> tables = tableManager.getAnalyticsTables(params);

    if (tables.isEmpty()) {
      clock.logTime("Table update aborted, nothing to update: '{}'", tableType.getTableName());
      progress.startingStage("Table updates " + tableType);
      progress.completedStage("Table updated aborted, no table or partitions to be updated");
      return;
    }

    clock.logTime(
        "Table update start: {}, earliest: {}, parameters: {}",
        tableType.getTableName(),
        toLongDate(params.getFromDate()),
        params);
    progress.startingStage("Performing pre-create table work");

    //
    // Perform pre-create tables
    //
    withClock(
        clock,
        "Performed pre-create table work " + tableType,
        () -> analyticsTableStrategy.preCreateTables(params, progress));

    //
    // Drop staging tables
    //
    progress.startingStage("Dropping staging tables (if any) " + tableType, tables.size());
    withClock(
        clock,
        "Dropped staging tables " + tableType,
        () -> analyticsTableStrategy.dropStagingTables(tables, progress));

    //
    // Create analytics tables
    //
    progress.startingStage("Creating analytics tables " + tableType, tables.size());
    withClock(
        clock,
        "Created analytics tables " + tableType,
        () -> analyticsTableStrategy.createTables(tables, progress));

    List<AnalyticsTablePartition> partitions = getTablePartitions(tables);
    int partitionSize = partitions.size();

    //
    // Populate analytics tables
    //
    progress.startingStage(
        "Populating " + partitionSize + " analytics tables " + tableType, partitionSize);
    withClock(
        clock,
        "Populated analytics tables",
        () -> analyticsTableStrategy.populateTables(params, partitions, progress));

    progress.startingStage("Invoking analytics table hooks " + tableType);
    tableUpdates += progress.runStage(0, tableManager::invokeAnalyticsTableSqlHooks);

    clock.logTime("Invoked analytics table hooks");

    //
    // Apply aggregation levels
    //
    tableUpdates += analyticsTableStrategy.applyAggregationLevels(tableType, partitions, progress);
    clock.logTime("Applied aggregation levels");

    //
    // Create indexes
    //
    withClock(
        clock,
        "Created indexes",
        () -> analyticsTableStrategy.createIndexes(getIndexes(partitions), progress, tableType));

    if (tableUpdates > 0) {
      progress.startingStage("Vacuuming tables " + tableType, partitions.size());
      withClock(
          clock, "Optimized tables", () -> analyticsTableStrategy.optimizeTables(tables, progress));
    }

    //
    // Analyze tables
    //
    progress.startingStage("Analyzing analytics tables " + tableType, partitions.size());
    withClock(
        clock, "Analyzed tables", () -> analyticsTableStrategy.analyzeTables(tables, progress));

    if (params.isLatestUpdate()) {
      progress.startingStage("Removing updated and deleted data " + tableType, SKIP_STAGE);
      progress.runStage(() -> tableManager.removeUpdatedData(tables));
      clock.logTime("Removed updated and deleted data");
    }

    //
    // Swap tables
    //
    analyticsTableStrategy.swapTables(params, tables, progress, tableType);

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

  private static Clock startClock(AnalyticsTableType tableType, int parallelJobs) {
    return new Clock(log)
        .startClock()
        .logTime(
            "Starting update of type: {}, table name: '{}', parallel jobs: {}",
            tableType,
            tableType.getTableName(),
            parallelJobs);
  }

  // Generic wrapper method
  private static void withClock(
      Clock clock, String message, Supplier<TableStrategyOpResult> operation) {
    TableStrategyOpResult result = operation.get();
    if (result.equals(TableStrategyOpResult.EXECUTED)) {
      clock.logTime(message);
    }
  }

  private AnalyticsTableStrategy getTableStrategy(int parallelJobs) {

    return switch (this.sqlBuilder.getDatabase()) {
      case DORIS ->
          new DorisAnalyticsTableStrategy(
              tableManager,
              organisationUnitService,
              dataElementService,
              resourceTableService,
              parallelJobs);
      case POSTGRESQL ->
          new PostgresAnalyticsTableStrategy(
              tableManager,
              organisationUnitService,
              dataElementService,
              resourceTableService,
              parallelJobs);
    };
  }
}
