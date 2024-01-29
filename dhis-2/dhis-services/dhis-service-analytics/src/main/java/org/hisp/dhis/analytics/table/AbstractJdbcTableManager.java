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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.hisp.dhis.analytics.table.PartitionUtils.getEndDate;
import static org.hisp.dhis.analytics.table.PartitionUtils.getStartDate;
import static org.hisp.dhis.analytics.table.model.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.table.model.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.createIndexStatement;
import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.getIndexName;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.getCollation;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.Collections;
import java.util.Date;
import java.util.List;
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
import org.hisp.dhis.analytics.table.model.AnalyticsIndex;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableExportSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.Collation;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
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
      "^\\d{4}-\\d{2}-\\d{2}(\\s|T)?((\\d{2}:)(\\d{2}:)?(\\d{2}))?(|.(\\d{3})|.(\\d{3})Z)?$";

  protected static final Set<ValueType> NO_INDEX_VAL_TYPES =
      Set.of(ValueType.TEXT, ValueType.LONG_TEXT);

  protected static final String PREFIX_ORGUNITLEVEL = "uidlevel";

  protected static final String PREFIX_ORGUNITNAMELEVEL = "namelevel";

  protected final IdentifiableObjectManager idObjectManager;

  protected final OrganisationUnitService organisationUnitService;

  protected final CategoryService categoryService;

  protected final SystemSettingManager systemSettingManager;

  protected final DataApprovalLevelService dataApprovalLevelService;

  protected final ResourceTableService resourceTableService;

  protected final AnalyticsTableHookService tableHookService;

  protected final PartitionManager partitionManager;

  private final DatabaseInfoProvider databaseInfoProvider;

  protected Boolean spatialSupport;

  protected final JdbcTemplate jdbcTemplate;

  protected final AnalyticsTableExportSettings analyticsExportSettings;

  protected final PeriodDataProvider periodDataProvider;

  private static final String WITH_AUTOVACUUM_ENABLED_FALSE = "with(autovacuum_enabled = false)";

  protected boolean isSpatialSupport() {
    if (spatialSupport == null)
      spatialSupport = databaseInfoProvider.getDatabaseInfo().isSpatialSupport();
    return spatialSupport;
  }

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
    createTempTable(table);
    createTempTablePartitions(table);
  }

  @Override
  public void createIndex(AnalyticsIndex index) {
    String indexName = getIndexName(index, getAnalyticsTableType());
    String sql = createIndexStatement(index, getAnalyticsTableType());

    log.debug("Create index: '{}' with SQL: '{}'", indexName, sql);

    jdbcTemplate.execute(sql);

    log.debug("Created index: '{}'", indexName);
  }

  @Override
  public void swapTable(AnalyticsTableUpdateParams params, AnalyticsTable table) {
    boolean tableExists = partitionManager.tableExists(table.getTableName());
    boolean skipMasterTable =
        params.isPartialUpdate() && tableExists && table.getTableType().hasLatestPartition();

    log.info(
        "Swapping table, master table exists: '{}', skip master table: '{}'",
        tableExists,
        skipMasterTable);

    table.getTablePartitions().stream()
        .forEach(p -> swapTable(p.getTempTableName(), p.getTableName()));

    if (!skipMasterTable) {
      swapTable(table.getTempTableName(), table.getTableName());
    } else {
      table.getTablePartitions().stream()
          .forEach(
              p ->
                  swapInheritance(
                      p.getTableName(), table.getTempTableName(), table.getTableName()));
      dropTempTable(table);
    }
  }

  @Override
  public void dropTempTable(AnalyticsTable table) {
    dropTableCascade(table.getTempTableName());
  }

  @Override
  public void dropTempTablePartition(AnalyticsTablePartition tablePartition) {
    dropTableCascade(tablePartition.getTempTableName());
  }

  @Override
  public void dropTable(String tableName) {
    executeSafely("drop table if exists " + tableName);
  }

  @Override
  public void dropTableCascade(String tableName) {
    executeSafely("drop table if exists " + tableName + " cascade");
  }

  @Override
  public void analyzeTable(String tableName) {
    executeSafely("analyze " + tableName);
  }

  @Override
  public void populateTablePartition(
      AnalyticsTableUpdateParams params, AnalyticsTablePartition partition) {
    populateTable(params, partition);
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

  // -------------------------------------------------------------------------
  // Abstract methods
  // -------------------------------------------------------------------------

  /**
   * Returns a list of table checks (constraints) for the given analytics table partition.
   *
   * @param partition the {@link AnalyticsTablePartition}.
   */
  protected abstract List<String> getPartitionChecks(AnalyticsTablePartition partition);

  /**
   * Populates the given analytics table.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param partition the {@link AnalyticsTablePartition} to populate.
   */
  protected abstract void populateTable(
      AnalyticsTableUpdateParams params, AnalyticsTablePartition partition);

  /**
   * Indicates whether data was created or updated for the given time range since last successful
   * "latest" table partition update.
   *
   * @param startDate the start date.
   * @param endDate the end date.
   * @return true if updated data exists.
   */
  protected abstract boolean hasUpdatedLatestData(Date startDate, Date endDate);

  // -------------------------------------------------------------------------
  // Protected supportive methods
  // -------------------------------------------------------------------------

  /** Returns the analytics table name. */
  protected String getTableName() {
    return getAnalyticsTableType().getTableName();
  }

  /**
   * Executes a SQL statement "safely" (without throwing any exception). Instead, exceptions are
   * simply logged.
   *
   * @param sql the SQL statement.
   */
  private void executeSafely(String sql) {
    try {
      jdbcTemplate.execute(sql);
    } catch (DataAccessException ex) {
      log.error(ex.getMessage());
    }
  }

  /**
   * Drops and creates the given analytics table.
   *
   * @param table the {@link AnalyticsTable}.
   */
  private void createTempTable(AnalyticsTable table) {
    StringBuilder sql = new StringBuilder();

    String tableName = table.getTempTableName();
    String unlogged = table.isUnlogged() ? "unlogged" : "";

    sql.append("create ").append(unlogged).append(" table ").append(tableName).append(" (");

    for (AnalyticsTableColumn col : table.getColumns()) {
      String dataType = col.getDataType().getValue();
      String nullable = col.isNotNull() ? " not null" : " null";
      String collation = col.hasCollation() ? getCollation(col.getCollation().name()) : EMPTY;

      sql.append(col.getName())
          .append(SPACE)
          .append(dataType)
          .append(collation)
          .append(nullable)
          .append(",");
    }

    TextUtils.removeLastComma(sql).append(") ").append(getTableOptions());

    log.info("Creating table: '{}', columns: '{}'", tableName, table.getColumnCount());
    log.debug("Create table SQL: '{}'", sql);

    jdbcTemplate.execute(sql.toString());
  }

  /**
   * Creates the table partitions for the given analytics table.
   *
   * @param table the {@link AnalyticsTable}.
   */
  private void createTempTablePartitions(AnalyticsTable table) {
    for (AnalyticsTablePartition partition : table.getTablePartitions()) {
      createTempTablePartition(table, partition);
    }
  }

  /**
   * Creates the given table partition.
   *
   * @param table the {@link AnalyticsTable}.
   * @param partition the {@link AnalyticsTablePartition}.
   */
  private void createTempTablePartition(AnalyticsTable table, AnalyticsTablePartition partition) {
    String tableName = partition.getTempTableName();
    String unlogged = table.isUnlogged() ? "unlogged" : "";
    List<String> checks = getPartitionChecks(partition);

    StringBuilder sql = new StringBuilder();

    sql.append("create ").append(unlogged).append(" table ").append(tableName).append("(");

    if (!checks.isEmpty()) {
      StringBuilder sqlCheck = new StringBuilder();
      checks.stream().forEach(check -> sqlCheck.append("check (" + check + "), "));
      sql.append(TextUtils.removeLastComma(sqlCheck.toString()));
    }

    sql.append(") inherits (")
        .append(table.getTempTableName())
        .append(") ")
        .append(getTableOptions());

    log.info("Creating partition table: '{}'", tableName);
    log.debug("Create table SQL: '{}'", sql);

    jdbcTemplate.execute(sql.toString());
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
      List<AnalyticsTableColumn> columns) {

    List<Integer> years = ListUtils.mutableCopy(dataYears);
    Logged logged = analyticsExportSettings.getTableLogged();

    Collections.sort(years);

    AnalyticsTable table = new AnalyticsTable(getAnalyticsTableType(), columns, logged);

    for (Integer year : years) {
      table.addPartitionTable(year, getStartDate(year), getEndDate(year));
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
    Date lastFullTableUpdate =
        systemSettingManager.getDateSetting(SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE);
    Date lastLatestPartitionUpdate =
        systemSettingManager.getDateSetting(
            SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE);
    Date lastAnyTableUpdate = DateUtils.getLatest(lastLatestPartitionUpdate, lastFullTableUpdate);

    Assert.notNull(
        lastFullTableUpdate,
        "A full analytics table update must be run prior to a latest partition update");

    Logged logged = analyticsExportSettings.getTableLogged();
    Date endDate = params.getStartTime();
    boolean hasUpdatedData = hasUpdatedLatestData(lastAnyTableUpdate, endDate);

    AnalyticsTable table = new AnalyticsTable(getAnalyticsTableType(), columns, logged);

    if (hasUpdatedData) {
      table.addPartitionTable(
          AnalyticsTablePartition.LATEST_PARTITION, lastFullTableUpdate, endDate);
      log.info(
          "Added latest analytics partition with start: '{}' and end: '{}'",
          getLongDateString(lastFullTableUpdate),
          getLongDateString(endDate));
    } else {
      log.info(
          "No updated latest data found with start: '{}' and end: '{}'",
          getLongDateString(lastAnyTableUpdate),
          getLongDateString(endDate));
    }

    return table;
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
        systemSettingManager.getDateSetting(SettingKey.LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE);

    if (lastResourceTableUpdate == null) {
      return columns;
    }

    return columns.stream()
        .filter(c -> c.getCreated() == null || c.getCreated().before(lastResourceTableUpdate))
        .collect(Collectors.toList());
  }

  /**
   * Executes the given SQL statement. Logs and times the operation.
   *
   * @param sql the SQL statement.
   * @param logMessage the custom log message to include in the log statement.
   */
  protected void invokeTimeAndLog(String sql, String logMessage) {
    Timer timer = new SystemTimer().start();

    jdbcTemplate.execute(sql);

    log.info("{} in: {}", logMessage, timer.stop().toString());
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
              String column = quote(pt.getName().toLowerCase());
              return new AnalyticsTableColumn(column, TEXT, prefix + "." + column);
            })
        .collect(Collectors.toList());
  }

  /**
   * Collects all the {@link OrganisationUnitLevel} as a list of {@link AnalyticsTableColumn}.
   *
   * @return a List of {@link AnalyticsTableColumn}
   */
  protected List<AnalyticsTableColumn> getOrganisationUnitLevelColumns() {
    return organisationUnitService.getFilledOrganisationUnitLevels().stream()
        .map(
            lv -> {
              String column = quote(PREFIX_ORGUNITLEVEL + lv.getLevel());
              return new AnalyticsTableColumn(column, CHARACTER_11, "ous." + column)
                  .withCreated(lv.getCreated());
            })
        .collect(Collectors.toList());
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
    return new AnalyticsTableColumn("ounamehierarchy", TEXT, Collation.C, columnExpression);
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
              String column = quote(ougs.getUid());
              return new AnalyticsTableColumn(column, CHARACTER_11, "ougs." + column)
                  .withCreated(ougs.getCreated());
            })
        .collect(Collectors.toList());
  }

  /**
   * Returns the select clause, potentially with a cast statement, based on the given value type.
   *
   * @param valueType the value type to represent as database column type.
   */
  protected String getSelectClause(ValueType valueType, String columnName) {
    if (valueType.isDecimal()) {
      return "cast(" + columnName + " as double precision)";
    } else if (valueType.isInteger()) {
      return "cast(" + columnName + " as bigint)";
    } else if (valueType.isBoolean()) {
      return "case when "
          + columnName
          + " = 'true' then 1 when "
          + columnName
          + " = 'false' then 0 else null end";
    } else if (valueType.isDate()) {
      return "cast(" + columnName + " as timestamp)";
    } else if (valueType.isGeo() && isSpatialSupport()) {
      return "ST_GeomFromGeoJSON('{\"type\":\"Point\", \"coordinates\":' || ("
          + columnName
          + ") || ', \"crs\":{\"type\":\"name\", \"properties\":{\"name\":\"EPSG:4326\"}}}')";
    } else if (valueType.isOrganisationUnit()) {
      return "ou.uid from organisationunit ou where ou.uid = (select " + columnName;
    } else {
      return columnName;
    }
  }

  // -------------------------------------------------------------------------
  // Private supportive methods
  // -------------------------------------------------------------------------

  /** Returns a table options SQL statement. */
  private String getTableOptions() {
    return WITH_AUTOVACUUM_ENABLED_FALSE;
  }

  /**
   * Swaps a database table, meaning drops the real table and renames the temporary table to become
   * the real table.
   *
   * @param tempTableName the temporary table name.
   * @param realTableName the real table name.
   */
  private void swapTable(String tempTableName, String realTableName) {
    String[] sqlSteps = {
      "drop table if exists " + realTableName + " cascade",
      "alter table " + tempTableName + " rename to " + realTableName
    };

    executeSafely(sqlSteps, true);
  }

  /**
   * Updates table inheritance of a table partition from the temp master table to the real master
   * table.
   *
   * @param partitionTableName the partition table name.
   * @param tempMasterTableName the temporary master table name.
   * @param realMasterTableName the real master table name.
   */
  private void swapInheritance(
      String partitionTableName, String tempMasterTableName, String realMasterTableName) {
    String[] sqlSteps = {
      "alter table " + partitionTableName + " inherit " + realMasterTableName,
      "alter table " + partitionTableName + " no inherit " + tempMasterTableName
    };

    executeSafely(sqlSteps, true);
  }

  /**
   * Executes a set of SQL statements "safely" (without throwing any exception). Instead, exceptions
   * are simply logged.
   *
   * @param sqlStatements the SQL statements to be executed
   * @param atomically if true, the statements are executed all together in a single JDBC call
   */
  private void executeSafely(String[] sqlStatements, boolean atomically) {
    if (atomically) {
      String sql = String.join(";", sqlStatements) + ";";
      log.debug(sql);
      executeSafely(sql);
    } else {
      for (int i = 0; i < sqlStatements.length; i++) {
        log.debug(sqlStatements[i]);
        executeSafely(sqlStatements[i]);
      }
    }
  }
}
