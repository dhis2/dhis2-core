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

import static java.util.function.Predicate.not;
import static org.hisp.dhis.analytics.table.util.PartitionUtils.getEndDate;
import static org.hisp.dhis.analytics.table.util.PartitionUtils.getStartDate;
import static org.hisp.dhis.commons.util.TextUtils.format;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.util.DateUtils.toLongDate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.AnalyticsTablePhase;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsDimensionType;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.db.model.Collation;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.util.DateUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractJdbcTableManager implements AnalyticsTableManager {
  /**
   * Matches the following patterns:
   *
   * <ul>
   *   <li>1999-12-12
   *   <li>1999-12-12T
   *   <li>1999-12-12T10:10:10
   *   <li>1999-10-10 10:10:10
   *   <li>1999-10-10 10:10
   *   <li>2021-12-14T11:45:00.000Z
   *   <li>2021-12-14T11:45:00.000
   * </ul>
   */
  protected static final String DATE_REGEXP =
      "'^[0-9]{4}-[0-9]{2}-[0-9]{2}(\\s|T)?(([0-9]{2}:)([0-9]{2}:)?([0-9]{2}))?(|.([0-9]{3})|.([0-9]{3})Z)?$'";

  protected static final String PREFIX_ORGUNITLEVEL = "uidlevel";

  protected static final String PREFIX_ORGUNITNAMELEVEL = "namelevel";

  public static final String OU_NAME_HIERARCHY_COLUMN_NAME = "ounamehierarchy";

  protected final IdentifiableObjectManager idObjectManager;

  protected final OrganisationUnitService organisationUnitService;

  protected final CategoryService categoryService;

  protected final SystemSettingsProvider settingsProvider;

  protected final DataApprovalLevelService dataApprovalLevelService;

  protected final ResourceTableService resourceTableService;

  protected final AnalyticsTableHookService tableHookService;

  protected final PartitionManager partitionManager;

  protected final JdbcTemplate jdbcTemplate;

  protected final AnalyticsTableSettings analyticsTableSettings;

  protected final PeriodDataProvider periodDataProvider;

  protected final SqlBuilder sqlBuilder;

  /**
   * Encapsulates the SQL logic to get the correct date column based on the event status. If new
   * statuses need to be loaded into the analytics events tables, they have to be supported/added
   * into this logic.
   */
  protected final String eventDateExpression =
      "CASE WHEN 'SCHEDULE' = ev.status THEN ev.scheduleddate ELSE ev.occurreddate END";

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  public Set<String> getExistingDatabaseTables() {
    return partitionManager.getAnalyticsPartitions(getAnalyticsTableType());
  }

  /** Override in order to perform work before tables are being generated. */
  @Override
  public void preCreateTables(AnalyticsTableUpdateParams params) {}

  /**
   * Removes data which was updated or deleted between the last successful analytics table update
   * and the start of this analytics table update process, excluding data which was created during
   * that time span.
   *
   * <p>Override in order to remove updated and deleted data for "latest" partition update.
   */
  @Override
  public void removeUpdatedData(List<AnalyticsTable> tables) {}

  @Override
  public void createTable(AnalyticsTable table) {
    createAnalyticsTable(table);

    if (!sqlBuilder.supportsDeclarativePartitioning()) {
      createAnalyticsTablePartitions(table);
    }
  }

  /**
   * Drops and creates the given analytics table or table partition.
   *
   * @param table the {@link Table}.
   */
  private void createAnalyticsTable(Table table) {
    log.info("Creating table: '{}', columns: '{}'", table.getName(), table.getColumns().size());

    String sql = sqlBuilder.createTable(table);

    log.debug("Create table SQL: '{}'", sql);

    jdbcTemplate.execute(sql);
  }

  /**
   * Creates the table partitions for the given analytics table.
   *
   * @param table the {@link AnalyticsTable}.
   */
  private void createAnalyticsTablePartitions(AnalyticsTable table) {
    for (AnalyticsTablePartition partition : table.getTablePartitions()) {
      createAnalyticsTable(partition);
    }
  }

  @Override
  public void createIndex(Index index) {
    log.debug("Creating index: '{}'", index.getName());

    String sql = sqlBuilder.createIndex(index);

    log.debug("Create index SQL: '{}'", sql);

    jdbcTemplate.execute(sql);
  }

  @Override
  public void swapTable(AnalyticsTableUpdateParams params, AnalyticsTable table) {
    boolean tableExists = tableExists(table.getMainName());
    boolean skipMasterTable =
        params.isPartialUpdate() && tableExists && table.getTableType().isLatestPartition();

    log.info("Swapping table: '{}'", table.getMainName());
    log.info("Master table exists: '{}', skip master table: '{}'", tableExists, skipMasterTable);

    List<Table> swappedPartitions = new UniqueArrayList<>();

    if (!sqlBuilder.supportsDeclarativePartitioning()) {
      table.getTablePartitions().forEach(part -> swapTable(part, part.getMainName()));
      table.getTablePartitions().forEach(part -> swappedPartitions.add(part.fromStaging()));
    }

    if (!skipMasterTable) {
      // Full replace update and main table exist, swap main table
      swapTable(table, table.getMainName());
    } else {
      // Incremental append update, update parent of partitions to existing main table
      if (!sqlBuilder.supportsDeclarativePartitioning()) {
        swappedPartitions.forEach(
            partition -> swapParentTable(partition, table.getName(), table.getMainName()));
      }
      dropTable(table);
    }
  }

  @Override
  public void dropTable(Table table) {
    dropTable(table.getName());
  }

  @Override
  public void dropTable(String name) {
    executeSilently(sqlBuilder.dropTableIfExistsCascade(name));
  }

  @Override
  public void analyzeTable(String name) {
    executeSilently(sqlBuilder.analyzeTable(name));
  }

  @Override
  public void vacuumTable(Table table) {
    executeSilently(sqlBuilder.vacuumTable(table));
  }

  @Override
  public void analyzeTable(Table table) {
    executeSilently(sqlBuilder.analyzeTable(table));
  }

  @Override
  public int invokeAnalyticsTableSqlHooks() {
    AnalyticsTableType type = getAnalyticsTableType();
    List<AnalyticsTableHook> hooks =
        tableHookService.getByPhaseAndAnalyticsTableType(
            AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED, type);
    tableHookService.executeAnalyticsTableSqlHooks(hooks);
    return hooks.size();
  }

  /**
   * Swaps a database table, meaning drops the main table and renames the staging table to become
   * the main table.
   *
   * @param stagingTable the staging table.
   * @param mainTableName the main table name.
   */
  private void swapTable(Table stagingTable, String mainTableName) {
    if (sqlBuilder.supportsMultiStatements()) {
      executeSilently(sqlBuilder.swapTable(stagingTable, mainTableName));
    } else {
      executeSilently(sqlBuilder.dropTableIfExistsCascade(mainTableName));
      executeSilently(sqlBuilder.renameTable(stagingTable, mainTableName));
    }
  }

  /**
   * Updates table inheritance of a table partition from the staging master table to the main master
   * table.
   *
   * @param stagingMasterName the staging master table name.
   * @param mainMasterName the main master table name.
   */
  private void swapParentTable(Table partition, String stagingMasterName, String mainMasterName) {
    if (sqlBuilder.supportsMultiStatements()) {
      executeSilently(sqlBuilder.swapParentTable(partition, stagingMasterName, mainMasterName));
    } else {
      executeSilently(sqlBuilder.removeParentTable(partition, stagingMasterName));
      executeSilently(sqlBuilder.setParentTable(partition, mainMasterName));
    }
  }

  /**
   * Indicates if a table with the given name exists.
   *
   * @param name the table name.
   * @return true if a table with the given name exists.
   */
  private boolean tableExists(String name) {
    return !jdbcTemplate.queryForList(sqlBuilder.tableExists(name)).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Abstract methods
  // -------------------------------------------------------------------------

  /**
   * Returns a list of table partition checks (constraints) for the given year and end date.
   *
   * @param year the year.
   * @param endDate the end date.
   * @return the list of table partition checks.
   */
  protected abstract List<String> getPartitionChecks(Integer year, Date endDate);

  // -------------------------------------------------------------------------
  // Protected supportive methods
  // -------------------------------------------------------------------------

  /**
   * Indicates whether the DBMS supports geospatial data types and functions.
   *
   * @return true if the DBMS supports geospatial data types and functions.
   */
  protected boolean isGeospatialSupport() {
    return sqlBuilder.supportsGeospatialData();
  }

  /**
   * Returns the analytics table name.
   *
   * @return the analytics table name.
   */
  protected String getTableName() {
    return getAnalyticsTableType().getTableName();
  }

  /**
   * Creates a {@link AnalyticsTable} with partitions based on a list of years with data.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param dataYears the list of years with data.
   * @param columns the list of {@link AnalyticsTableColumn}.
   */
  protected AnalyticsTable getRegularAnalyticsTable(
      AnalyticsTableUpdateParams params,
      List<Integer> dataYears,
      List<AnalyticsTableColumn> columns,
      List<String> sortKey) {
    Calendar calendar = PeriodType.getCalendar();
    List<Integer> years = ListUtils.mutableCopy(dataYears);
    Logged logged = analyticsTableSettings.getTableLogged();

    Collections.sort(years);

    AnalyticsTable table = new AnalyticsTable(getAnalyticsTableType(), columns, sortKey, logged);

    for (Integer year : years) {
      List<String> checks = getPartitionChecks(year, getEndDate(calendar, year));

      table.addTablePartition(
          checks, year, getStartDate(calendar, year), getEndDate(calendar, year));
    }

    return table;
  }

  /**
   * Creates a {@link AnalyticsTable} with a partition for the "latest" data. The start date of the
   * partition is the time of the last successful full analytics table update. The end date of the
   * partition is the start time of this analytics table update process.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param columns the list of {@link AnalyticsTableColumn}.
   */
  protected AnalyticsTable getLatestAnalyticsTable(
      AnalyticsTableUpdateParams params, List<AnalyticsTableColumn> columns) {
    SystemSettings settings = settingsProvider.getCurrentSettings();
    Date lastFullTableUpdate = settings.getLastSuccessfulAnalyticsTablesUpdate();
    Date lastLatestPartitionUpdate = settings.getLastSuccessfulLatestAnalyticsPartitionUpdate();
    Date lastAnyTableUpdate = DateUtils.getLatest(lastLatestPartitionUpdate, lastFullTableUpdate);

    Assert.isTrue(
        lastFullTableUpdate.getTime() > 0L,
        "A full analytics table update must be run prior to a latest partition update");

    Logged logged = analyticsTableSettings.getTableLogged();
    Date endDate = params.getStartTime();
    boolean hasUpdatedData = hasUpdatedLatestData(lastAnyTableUpdate, endDate);

    AnalyticsTable table = new AnalyticsTable(getAnalyticsTableType(), columns, List.of(), logged);

    if (hasUpdatedData) {
      table.addTablePartition(
          List.of(), AnalyticsTablePartition.LATEST_PARTITION, lastFullTableUpdate, endDate);
      log.info(
          "Added latest analytics partition with start: '{}' and end: '{}'",
          toLongDate(lastFullTableUpdate),
          toLongDate(endDate));
    } else {
      log.info(
          "No updated latest data found with start: '{}' and end: '{}'",
          toLongDate(lastAnyTableUpdate),
          toLongDate(endDate));
    }

    return table;
  }

  /**
   * Executes the given SQL statement. Logs and times the operation.
   *
   * @param sql the SQL statement.
   * @param logPattern the log message pattern.
   * @param args the log message arguments.
   */
  protected void invokeTimeAndLog(String sql, String logPattern, Object... args) {
    Timer timer = new SystemTimer().start();

    log.debug("Populate table SQL: '{}'", sql);

    jdbcTemplate.execute(sql);

    String logMessage = format(logPattern, args);

    log.info("{} in: {}", logMessage, timer.stop().toString());
  }

  /**
   * Returns a map of identifiable properties and values.
   *
   * @param object the {@link IdentifiableObject}.
   * @return a {@link Map}.
   */
  protected Map<String, String> toVariableMap(IdentifiableObject object) {
    return Map.of(
        "id", String.valueOf(object.getId()),
        "uid", quote(object.getUid()));
  }

  /**
   * Filters out analytics table columns which were created after the time of the last successful
   * resource table update. This so that the create table query does not refer to columns not
   * present in resource tables.
   *
   * @param columns the analytics table columns.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  protected List<AnalyticsTableColumn> filterDimensionColumns(List<AnalyticsTableColumn> columns) {
    Date lastResourceTableUpdate =
        settingsProvider.getCurrentSettings().getLastSuccessfulResourceTablesUpdate();

    if (lastResourceTableUpdate.getTime() == 0L) {
      return columns;
    }

    List<AnalyticsTableColumn> filteredColumns = new ArrayList<>();

    for (AnalyticsTableColumn c : columns) {
      if (c.getCreated() == null || c.getCreated().before(lastResourceTableUpdate)) {
        filteredColumns.add(c);
      }
    }

    return filteredColumns;
  }

  /**
   * Collects all the {@link PeriodType} as a list of {@link AnalyticsTableColumn}.
   *
   * @param prefix the prefix to use for the column name
   * @return a List of {@link AnalyticsTableColumn}
   */
  protected List<AnalyticsTableColumn> getPeriodTypeColumns(String prefix) {
    return PeriodType.getAvailablePeriodTypes().stream()
        .map(
            pt -> {
              String name = pt.getName().toLowerCase();
              return AnalyticsTableColumn.builder()
                  .name(name)
                  .dataType(TEXT)
                  .selectExpression(prefix + "." + quote(name))
                  .skipIndex(skipIndex(name))
                  .build();
            })
        .filter(not(this::skipColumn))
        .toList();
  }

  /**
   * Collects all the {@link OrganisationUnitLevel} as a list of {@link AnalyticsTableColumn}.
   *
   * @return a List of {@link AnalyticsTableColumn}
   */
  protected List<AnalyticsTableColumn> getOrganisationUnitLevelColumns() {
    return organisationUnitService.getFilledOrganisationUnitLevels().stream()
        .map(
            level -> {
              String name = PREFIX_ORGUNITLEVEL + level.getLevel();
              return AnalyticsTableColumn.builder()
                  .name(name)
                  .dataType(CHARACTER_11)
                  .selectExpression("ous." + quote(name))
                  .created(level.getCreated())
                  .build();
            })
        .toList();
  }

  /**
   * Organisation unit name hierarchy delivery.
   *
   * @return a table column {@link AnalyticsTableColumn}
   */
  protected AnalyticsTableColumn getOrganisationUnitNameHierarchyColumn() {
    String columnExpression =
        "concat_ws(' / ',"
            + organisationUnitService.getFilledOrganisationUnitLevels().stream()
                .map(lv -> "ous." + PREFIX_ORGUNITNAMELEVEL + lv.getLevel())
                .collect(Collectors.joining(","))
            + ") as ounamehierarchy";
    return AnalyticsTableColumn.builder()
        .name(OU_NAME_HIERARCHY_COLUMN_NAME)
        .dataType(TEXT)
        .collation(Collation.C)
        .selectExpression(columnExpression)
        .build();
  }

  /**
   * Collects all the {@link OrganisationUnitGroupSet} as a list of {@link AnalyticsTableColumn}.
   *
   * @return a List of {@link AnalyticsTableColumn}
   */
  protected List<AnalyticsTableColumn> getOrganisationUnitGroupSetColumns() {
    return idObjectManager.getDataDimensionsNoAcl(OrganisationUnitGroupSet.class).stream()
        .map(
            ougs -> {
              String name = ougs.getUid();
              return AnalyticsTableColumn.builder()
                  .name(name)
                  .dimensionType(AnalyticsDimensionType.DYNAMIC)
                  .dataType(CHARACTER_11)
                  .selectExpression("ougs." + quote(name))
                  .skipIndex(skipIndex(ougs))
                  .created(ougs.getCreated())
                  .build();
            })
        .toList();
  }

  protected List<AnalyticsTableColumn> getDataElementGroupSetColumns() {
    return idObjectManager.getDataDimensionsNoAcl(DataElementGroupSet.class).stream()
        .map(
            degs -> {
              String name = degs.getUid();
              return AnalyticsTableColumn.builder()
                  .name(name)
                  .dimensionType(AnalyticsDimensionType.DYNAMIC)
                  .dataType(CHARACTER_11)
                  .selectExpression("degs." + quote(name))
                  .skipIndex(skipIndex(degs))
                  .created(degs.getCreated())
                  .build();
            })
        .toList();
  }

  protected List<AnalyticsTableColumn> getDisaggregationCategoryOptionGroupSetColumns() {
    return categoryService.getDisaggregationCategoryOptionGroupSetsNoAcl().stream()
        .map(
            cogs -> {
              String name = cogs.getUid();
              return AnalyticsTableColumn.builder()
                  .name(name)
                  .dimensionType(AnalyticsDimensionType.DYNAMIC)
                  .dataType(CHARACTER_11)
                  .selectExpression("dcs." + quote(name))
                  .skipIndex(skipIndex(cogs))
                  .created(cogs.getCreated())
                  .build();
            })
        .toList();
  }

  protected List<AnalyticsTableColumn> getAttributeCategoryOptionGroupSetColumns() {
    return categoryService.getAttributeCategoryOptionGroupSetsNoAcl().stream()
        .map(
            cogs -> {
              String name = cogs.getUid();
              return AnalyticsTableColumn.builder()
                  .name(name)
                  .dimensionType(AnalyticsDimensionType.DYNAMIC)
                  .dataType(CHARACTER_11)
                  .selectExpression("acs." + quote(name))
                  .skipIndex(skipIndex(cogs))
                  .created(cogs.getCreated())
                  .build();
            })
        .toList();
  }

  protected List<AnalyticsTableColumn> getDisaggregationCategoryColumns() {
    return categoryService.getDisaggregationDataDimensionCategoriesNoAcl().stream()
        .map(
            category -> {
              String name = category.getUid();
              return AnalyticsTableColumn.builder()
                  .name(name)
                  .dimensionType(AnalyticsDimensionType.DYNAMIC)
                  .dataType(CHARACTER_11)
                  .selectExpression("dcs." + quote(name))
                  .skipIndex(skipIndex(category))
                  .created(category.getCreated())
                  .build();
            })
        .toList();
  }

  protected List<AnalyticsTableColumn> getAttributeCategoryColumns() {
    return categoryService.getAttributeDataDimensionCategoriesNoAcl().stream()
        .map(
            category -> {
              String name = category.getUid();
              return AnalyticsTableColumn.builder()
                  .name(name)
                  .dimensionType(AnalyticsDimensionType.DYNAMIC)
                  .dataType(CHARACTER_11)
                  .selectExpression("acs." + quote(name))
                  .skipIndex(skipIndex(category))
                  .created(category.getCreated())
                  .build();
            })
        .toList();
  }

  /**
   * Indicates whether indexing should be skipped for the given dimensional object based on the
   * system configuration.
   *
   * @param dimension the {@link DimensionalObject}.
   * @return {@link Skip#SKIP} if index should be skipped, {@link Skip#INCLUDE} otherwise.
   */
  protected Skip skipIndex(DimensionalObject dimension) {
    return skipIndex(dimension.getUid());
  }

  /**
   * Indicates whether indexing should be skipped for the given dimensional identifier based on the
   * system configuration.
   *
   * @param dimension the dimension identifier.
   * @return {@link Skip#SKIP} if index should be skipped, {@link Skip#INCLUDE} otherwise.
   */
  protected Skip skipIndex(String dimension) {
    Set<String> dimensions = analyticsTableSettings.getSkipIndexDimensions();
    return dimensions.contains(dimension) ? Skip.SKIP : Skip.INCLUDE;
  }

  /**
   * Indicates whether the column should be skipped for the given {@link AnalyticsTableColumn} based
   * on the system configuration. The matching is performed using the {@code name} property of the
   * given column.
   *
   * @param column the {@link AnalyticsTableColumn}.
   * @return true if the column should be skipped, false otherwise.
   */
  protected boolean skipColumn(AnalyticsTableColumn column) {
    return analyticsTableSettings.getSkipColumnDimensions().contains(column.getName());
  }

  /**
   * Indicates whether the table with the given name is not empty, i.e. has at least one row.
   *
   * @param name the table name.
   * @return true if the table is not empty.
   */
  protected boolean tableIsNotEmpty(String name) {
    String sql = format("select 1 from {} limit 1;", sqlBuilder.qualifyTable(name));
    return jdbcTemplate.queryForRowSet(sql).next();
  }

  /**
   * Quotes the given relation.
   *
   * @param relation the relation to quote, e.g. a table or column name.
   * @return a double quoted relation.
   */
  protected String quote(String relation) {
    return sqlBuilder.quote(relation);
  }

  /**
   * Returns a quoted and comma delimited string.
   *
   * @param items the items to join.
   * @return a string representing the comma delimited and quoted item values.
   */
  protected String quotedCommaDelimitedString(Collection<String> items) {
    return sqlBuilder.singleQuotedCommaDelimited(items);
  }

  // -------------------------------------------------------------------------
  // Private supportive methods
  // -------------------------------------------------------------------------

  /**
   * Executes a SQL statement silently without throwing any exceptions. Instead exceptions are
   * logged.
   *
   * @param sql the SQL statement to execute.
   */
  private void executeSilently(String sql) {
    try {
      jdbcTemplate.execute(sql);
    } catch (DataAccessException ex) {
      log.error(ex.getMessage());
    }
  }
}
