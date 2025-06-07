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

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.AnalyticsStringUtils.replaceQualify;
import static org.hisp.dhis.analytics.AnalyticsStringUtils.toCommaSeparated;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DATE;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.analytics.table.util.ColumnUtils;
import org.hisp.dhis.analytics.table.writer.JdbcOwnershipWriter;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.jdbc.batchhandler.MappingBatchHandler;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.quick.JdbcConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populates the analytics_ownership_[programuid] table which is joined for tracker analytics
 * queries to find the owning organisation unit at the start or end of the query date range.
 *
 * @author Jim Grace
 */
@Slf4j
@Service("org.hisp.dhis.analytics.OwnershipAnalyticsTableManager")
public class JdbcOwnershipAnalyticsTableManager extends AbstractEventJdbcTableManager {
  private final JdbcConfiguration jdbcConfiguration;

  private static final String HISTORY_TABLE_ID = "1001-01-01";

  // Must be later than the dummy HISTORY_TABLE_ID for SQL query order.
  private static final String TRACKED_ENTITY_OWN_TABLE_ID = "2002-02-02";

  protected static final List<AnalyticsTableColumn> FIXED_COLS =
      List.of(
          AnalyticsTableColumn.builder()
              .name("teuid")
              .dataType(CHARACTER_11)
              .nullable(NOT_NULL)
              .selectExpression("te.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("startdate")
              .dataType(DATE)
              .selectExpression("a.startdate")
              .build(),
          AnalyticsTableColumn.builder()
              .name("enddate")
              .dataType(DATE)
              .selectExpression("a.enddate")
              .build(),
          AnalyticsTableColumn.builder()
              .name("ou")
              .dataType(CHARACTER_11)
              .nullable(NOT_NULL)
              .selectExpression("ou.uid")
              .build());

  public JdbcOwnershipAnalyticsTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingsProvider settingsProvider,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      PartitionManager partitionManager,
      @Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate,
      JdbcConfiguration jdbcConfiguration,
      AnalyticsTableSettings analyticsTableSettings,
      PeriodDataProvider periodDataProvider,
      ColumnUtils columnUtils,
      SqlBuilder sqlBuilder) {
    super(
        idObjectManager,
        organisationUnitService,
        categoryService,
        settingsProvider,
        dataApprovalLevelService,
        resourceTableService,
        tableHookService,
        partitionManager,
        jdbcTemplate,
        analyticsTableSettings,
        periodDataProvider,
        columnUtils,
        sqlBuilder);
    this.jdbcConfiguration = jdbcConfiguration;
  }

  @Override
  public AnalyticsTableType getAnalyticsTableType() {
    return AnalyticsTableType.OWNERSHIP;
  }

  @Override
  @Transactional
  public List<AnalyticsTable> getAnalyticsTables(AnalyticsTableUpdateParams params) {
    return params.isLatestUpdate() ? List.of() : getRegularAnalyticsTables();
  }

  /**
   * Creates a list of {@link AnalyticsTable} for each program.
   *
   * @return a list of {@link AnalyticsTableUpdateParams}.
   */
  private List<AnalyticsTable> getRegularAnalyticsTables() {
    Logged logged = analyticsTableSettings.getTableLogged();
    return idObjectManager.getAllNoAcl(Program.class).stream()
        .map(pr -> new AnalyticsTable(getAnalyticsTableType(), getColumns(), logged, pr))
        .collect(toList());
  }

  @Override
  protected List<String> getPartitionChecks(Integer year, Date endDate) {
    return List.of();
  }

  @Override
  public void populateTable(AnalyticsTableUpdateParams params, AnalyticsTablePartition partition) {
    String tableName = partition.getName();

    Program program = partition.getMasterTable().getProgram();

    if (program.getProgramType() == WITHOUT_REGISTRATION) {
      return; // Builds an empty table which may be joined in queries
    }

    String sql = getInputSql(program);

    log.debug("Populate table '{}' with SQL: '{}'", tableName, sql);

    Timer timer = new SystemTimer().start();

    populateOwnershipTableInternal(partition, sql);

    log.info("Populate table '{}' in: '{}'", tableName, timer.stop().toString());
  }

  private void populateOwnershipTableInternal(AnalyticsTablePartition partition, String sql) {
    String tableName = partition.getName();

    List<String> columnNames =
        getColumns().stream().map(AnalyticsTableColumn::getName).collect(toList());

    try (MappingBatchHandler batchHandler =
        MappingBatchHandler.builder()
            .jdbcConfiguration(jdbcConfiguration)
            .tableName(tableName)
            .columns(columnNames)
            .build()) {
      batchHandler.init();

      JdbcOwnershipWriter writer = JdbcOwnershipWriter.getInstance(batchHandler);
      AtomicInteger queryRowCount = new AtomicInteger();

      jdbcTemplate.query(
          sql,
          resultSet -> {
            writer.write(getRowMap(columnNames, resultSet));
            queryRowCount.getAndIncrement();
          });

      log.info(
          "OwnershipAnalytics query row count was {} for table '{}'", queryRowCount, tableName);
      batchHandler.flush();
    } catch (Exception ex) {
      log.error("Failed to alter table ownership: ", ex);
    }
  }

  /**
   * Returns a SQL select query. For the from clause, for tracked entities in this program in
   * programownershiphistory, get one row for each programownershiphistory row and then get a final
   * row from the trackedentityprogramowner table to show the final owner.
   *
   * <p>The start date values are dummy so that all the history table rows will be ordered first and
   * the tracked entity owner table row will come last.
   *
   * <p>The start date in the analytics table will be a far past date for the first row for each
   * tracked entity, or the previous row's end date plus one day in subsequent rows for that tracked
   * entity.
   *
   * <p>Rows in programownershiphistory that don't have organisationunitid will be filtered out.
   *
   * @param program the {@link Program}.
   * @return a SQL select query.
   */
  private String getInputSql(Program program) {
    List<AnalyticsTableColumn> columns = getColumns();

    StringBuilder sql = new StringBuilder("select ");
    sql.append(toCommaSeparated(columns, AnalyticsTableColumn::getSelectExpression));

    sql.append(
        replaceQualify(
            sqlBuilder,
            """
            \sfrom (\
            select h.trackedentityid, '${historyTableId}' as startdate, h.enddate as enddate, h.organisationunitid \
            from ${programownershiphistory} h \
            where h.programid = ${programId} \
            and h.organisationunitid is not null \
            union distinct \
            select o.trackedentityid, '${trackedEntityOwnTableId}' as startdate, null as enddate, o.organisationunitid \
            from ${trackedentityprogramowner} o \
            where o.programid = ${programId} \
            and o.trackedentityid in (\
            select distinct p.trackedentityid \
            from ${programownershiphistory} p \
            where p.programid = ${programId} \
            and p.organisationunitid is not null)) a \
            inner join ${trackedentity} te on a.trackedentityid = te.trackedentityid \
            inner join ${organisationunit} ou on a.organisationunitid = ou.organisationunitid \
            left join analytics_rs_orgunitstructure ous on a.organisationunitid = ous.organisationunitid \
            left join analytics_rs_organisationunitgroupsetstructure ougs on a.organisationunitid = ougs.organisationunitid \
            order by te.uid, a.startdate, a.enddate""",
            Map.of(
                "historyTableId", HISTORY_TABLE_ID,
                "trackedEntityOwnTableId", TRACKED_ENTITY_OWN_TABLE_ID,
                "programId", String.valueOf(program.getId()))));
    return sql.toString();
  }

  private Map<String, Object> getRowMap(List<String> columnNames, ResultSet resultSet)
      throws SQLException {
    Map<String, Object> rowMap = new HashMap<>();

    for (int i = 0; i < columnNames.size(); i++) {
      rowMap.put(columnNames.get(i), resultSet.getObject(i + 1));
    }

    return rowMap;
  }

  /**
   * Returns dimensional analytics table columns.
   *
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumns() {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(FIXED_COLS);
    columns.addAll(getOrganisationUnitLevelColumns());
    columns.addAll(getOrganisationUnitGroupSetColumns());

    return filterDimensionColumns(columns);
  }
}
