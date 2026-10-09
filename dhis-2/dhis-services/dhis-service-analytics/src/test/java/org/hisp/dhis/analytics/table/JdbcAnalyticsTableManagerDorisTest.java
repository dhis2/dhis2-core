/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.period.PeriodType.PERIOD_TYPES;
import static org.hisp.dhis.test.TestBase.createTrackedEntityType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.DorisSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class JdbcAnalyticsTableManagerDorisTest {
  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private CategoryService categoryService;

  @Mock private SystemSettingsProvider settingsProvider;

  @Mock private SystemSettings settings;

  @Mock private DataApprovalLevelService dataApprovalLevelService;

  @Mock private ResourceTableService resourceTableService;

  @Mock private AnalyticsTableHookService analyticsTableHookService;

  @Mock private PartitionManager partitionManager;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private AnalyticsTableSettings analyticsTableSettings;

  @Mock private PeriodDataProvider periodDataProvider;

  @Mock private ConfigurationService configurationService;

  @Mock private Configuration configuration;

  @Mock private DataElementService dataElementService;

  @Spy private SqlBuilder sqlBuilder = new DorisSqlBuilder("dhis2", "driver");

  @InjectMocks private JdbcAnalyticsTableManager subject;

  @BeforeEach
  void setUp() {
    // shared by the tests that build tables, not needed by every test
    lenient().when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    lenient().when(settings.getLastSuccessfulResourceTablesUpdate()).thenReturn(new Date(0L));
  }

  @Test
  void testGetRegularAnalyticsTableHasUniqueKeyOnId() {
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .startTime(new DateTime(2020, 1, 1, 0, 0).toDate())
            .build();

    when(jdbcTemplate.queryForList(
            org.mockito.Mockito.anyString(), org.mockito.Mockito.eq(Integer.class)))
        .thenReturn(List.of(2020));
    when(configurationService.getConfiguration()).thenReturn(configuration);
    when(configuration.getDataOutputPeriodTypes())
        .thenReturn(PERIOD_TYPES.stream().collect(toUnmodifiableSet()));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertEquals(1, tables.size());
    AnalyticsTable table = tables.get(0);
    assertTrue(table.hasPrimaryKey());
    assertEquals(List.of("id", "year"), table.getPrimaryKey());
    String createTableSql = sqlBuilder.createTable(table);
    assertTrue(createTableSql.contains("unique key (`id`,`year`)"));
    // Doris rejects unbounded string/text types as key columns on any key model (errCode = 2,
    // "String Type should not be used in key column"). The id column (a concat(...) of several
    // dimension UIDs) must be given a bounded type on Doris, unlike Postgres/ClickHouse where it
    // stays TEXT.
    assertTrue(
        createTableSql.contains("`id` varchar(1020)"),
        () -> "Expected id column to use a bounded varchar type, got: " + createTableSql);
    assertFalse(
        createTableSql.contains("`id` string"),
        () ->
            "id column must not use the unbounded string type as a key column, got: "
                + createTableSql);
  }

  @Test
  @DisplayName(
      "Doris latest-partition window starts at the last continuous update, not the last full"
          + " rebuild, since Doris merges into the persistent main table rather than replacing a"
          + " partition wholesale")
  void testGetLatestAnalyticsTableUsesLastAnyTableUpdate() {
    Date lastFullTableUpdate = new DateTime(2019, 3, 1, 2, 0).toDate();
    Date lastLatestPartitionUpdate = new DateTime(2019, 3, 1, 9, 0).toDate();
    Date startTime = new DateTime(2019, 3, 1, 10, 0).toDate();

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().startTime(startTime).build().withLatestPartition();

    List<Map<String, Object>> queryResp = new ArrayList<>();
    queryResp.add(Map.of("dataelementid", 1));

    when(settings.getLastSuccessfulAnalyticsTablesUpdate()).thenReturn(lastFullTableUpdate);
    when(settings.getLastSuccessfulAnalyticsTablesUpdate(AnalyticsTableType.DATA_VALUE))
        .thenReturn(lastFullTableUpdate);
    when(settings.getLastSuccessfulLatestAnalyticsPartitionUpdate(AnalyticsTableType.DATA_VALUE))
        .thenReturn(lastLatestPartitionUpdate);
    when(analyticsTableSettings.getTableLogged()).thenReturn(Logged.UNLOGGED);
    when(jdbcTemplate.queryForList(org.mockito.Mockito.anyString())).thenReturn(queryResp);
    when(configurationService.getConfiguration()).thenReturn(configuration);
    when(configuration.getDataOutputPeriodTypes())
        .thenReturn(PERIOD_TYPES.stream().collect(toUnmodifiableSet()));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertEquals(1, tables.size());

    AnalyticsTablePartition partition = tables.get(0).getLatestTablePartition();

    assertTrue(partition.isLatestPartition());
    assertEquals(lastLatestPartitionUpdate, partition.getStartDate());
    assertEquals(startTime, partition.getEndDate());
  }

  @Test
  @DisplayName(
      "Doris latest-partition window starts at this table type's own last update, not the shared"
          + " one, which also advances when this type was skipped or its update was aborted")
  void testGetLatestAnalyticsTableUsesPerTypeLastUpdateNotShared() {
    Date lastFullTableUpdate = new DateTime(2019, 3, 1, 2, 0).toDate();
    Date lastDataValueUpdate = new DateTime(2019, 3, 1, 5, 0).toDate();
    Date lastSharedUpdate = new DateTime(2019, 3, 1, 9, 0).toDate();
    Date startTime = new DateTime(2019, 3, 1, 10, 0).toDate();

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().startTime(startTime).build().withLatestPartition();

    List<Map<String, Object>> queryResp = new ArrayList<>();
    queryResp.add(Map.of("dataelementid", 1));

    when(settings.getLastSuccessfulAnalyticsTablesUpdate()).thenReturn(lastFullTableUpdate);
    lenient()
        .when(settings.getLastSuccessfulLatestAnalyticsPartitionUpdate())
        .thenReturn(lastSharedUpdate);
    when(settings.getLastSuccessfulAnalyticsTablesUpdate(AnalyticsTableType.DATA_VALUE))
        .thenReturn(lastFullTableUpdate);
    when(settings.getLastSuccessfulLatestAnalyticsPartitionUpdate(AnalyticsTableType.DATA_VALUE))
        .thenReturn(lastDataValueUpdate);
    when(analyticsTableSettings.getTableLogged()).thenReturn(Logged.UNLOGGED);
    when(jdbcTemplate.queryForList(org.mockito.Mockito.anyString())).thenReturn(queryResp);
    when(configurationService.getConfiguration()).thenReturn(configuration);
    when(configuration.getDataOutputPeriodTypes())
        .thenReturn(PERIOD_TYPES.stream().collect(toUnmodifiableSet()));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertEquals(1, tables.size());
    assertEquals(lastDataValueUpdate, tables.get(0).getLatestTablePartition().getStartDate());
  }

  @Test
  void testRemoveUpdatedDataMaterializesKeysNatively() {
    Date lastFullTableUpdate = new DateTime(2019, 3, 1, 2, 0).toDate();
    Date lastLatestPartitionUpdate = new DateTime(2019, 3, 1, 9, 0).toDate();
    Date startTime = new DateTime(2019, 3, 1, 10, 0).toDate();

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().startTime(startTime).build().withLatestPartition();

    List<Map<String, Object>> queryResp = new ArrayList<>();
    queryResp.add(Map.of("dataelementid", 1));

    when(settings.getLastSuccessfulAnalyticsTablesUpdate()).thenReturn(lastFullTableUpdate);
    when(settings.getLastSuccessfulAnalyticsTablesUpdate(AnalyticsTableType.DATA_VALUE))
        .thenReturn(lastFullTableUpdate);
    when(settings.getLastSuccessfulLatestAnalyticsPartitionUpdate(AnalyticsTableType.DATA_VALUE))
        .thenReturn(lastLatestPartitionUpdate);
    when(jdbcTemplate.queryForList(org.mockito.Mockito.anyString())).thenReturn(queryResp);
    when(configurationService.getConfiguration()).thenReturn(configuration);
    when(configuration.getDataOutputPeriodTypes())
        .thenReturn(PERIOD_TYPES.stream().collect(toUnmodifiableSet()));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);
    assertEquals(1, tables.size());

    subject.removeUpdatedData(tables);

    org.mockito.ArgumentCaptor<String> sql = org.mockito.ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(jdbcTemplate, org.mockito.Mockito.times(5)).execute(sql.capture());
    List<String> statements = sql.getAllValues();

    String drop1 = statements.get(0);
    assertTrue(drop1.toLowerCase().contains("drop table if exists"));
    assertTrue(drop1.contains("analytics_delete_keys"));

    String create = statements.get(1);
    assertTrue(create.toLowerCase().contains("create table"));
    assertTrue(create.contains("analytics_delete_keys"));
    assertTrue(create.contains("`dx`"));
    assertTrue(create.contains("`co`"));
    assertTrue(create.contains("`ao`"));
    assertTrue(create.contains("`pe`"));
    assertTrue(create.contains("`ou`"));
    assertTrue(create.contains("`year`"));
    assertFalse(create.contains("varchar(1020)"));

    String insert = statements.get(2);
    assertTrue(insert.toLowerCase().contains("insert into"));
    assertTrue(insert.contains("analytics_delete_keys"));
    assertTrue(insert.contains("datavalue"));
    assertTrue(insert.contains("analytics_rs_dataelementstructure"));
    assertFalse(insert.contains("concat("));
    assertTrue(insert.contains("dv.lastupdated >="));
    assertTrue(insert.contains("dv.lastupdated <"));
    assertTrue(
        insert.contains(
            "(dx,co,ao,pe,ou,year) select des.dataelementuid, dcs.categoryoptioncombouid, "
                + "acs.categoryoptioncombouid, ps.iso, ous.organisationunituid, ps.year"),
        () -> "Expected column-list-to-select-list pairing to match exactly, got: " + insert);

    String delete = statements.get(3);
    assertTrue(delete.toLowerCase().contains("delete from"));
    assertTrue(delete.toLowerCase().contains("using"));
    assertTrue(delete.contains("analytics_delete_keys"));
    assertTrue(delete.contains("ax.dx=k.dx"));
    assertTrue(delete.contains("ax.co=k.co"));
    assertTrue(delete.contains("ax.ao=k.ao"));
    assertTrue(delete.contains("ax.pe=k.pe"));
    assertTrue(delete.contains("ax.ou=k.ou"));
    assertTrue(delete.contains("ax.year=k.year"));
    assertFalse(delete.contains("datavalue"));
    assertFalse(delete.contains("analytics_rs_"));

    String drop2 = statements.get(4);
    assertTrue(drop2.toLowerCase().contains("drop table"));
    assertTrue(drop2.contains("analytics_delete_keys"));
  }

  @Test
  void testSwapTableInsertsStagingDataIntoMainTableWhenSkippingMasterTable() {
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .startTime(new DateTime(2020, 3, 1, 10, 0).toDate())
            .build()
            .withLatestPartition();

    List<AnalyticsTableColumn> columns =
        List.of(
            AnalyticsTableColumn.builder()
                .name("dx")
                .dataType(TEXT)
                .selectExpression("dx")
                .build());
    AnalyticsTable table =
        new AnalyticsTable(
            AnalyticsTableType.DATA_VALUE, columns, List.of(), List.of("dx"), Logged.UNLOGGED);

    // Main table already exists, and params.isPartialUpdate() (via withLatestPartition()) plus
    // AnalyticsTableType.DATA_VALUE.isLatestPartition()==true together push swapTable() into the
    // skipMasterTable branch.
    when(jdbcTemplate.queryForList(sqlBuilder.tableExists(table.getMainName())))
        .thenReturn(List.of(Map.of("table_name", "analytics")));

    subject.swapTable(params, table);

    org.mockito.ArgumentCaptor<String> sql = org.mockito.ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(jdbcTemplate, org.mockito.Mockito.times(2)).execute(sql.capture());

    List<String> statements = sql.getAllValues();
    assertTrue(
        statements.stream()
            .anyMatch(
                s ->
                    s.startsWith("insert into `analytics`") && s.contains("from `analytics_temp`")),
        () -> "Expected an insert-into-main-from-staging statement, got: " + statements);
    // The staging table is a native Doris table, not a federated one, so the FROM reference must
    // NOT be qualified with the federated catalog prefix that qualifyTable() would add.
    assertTrue(
        statements.stream().noneMatch(s -> s.contains("dhis2.public.")),
        () ->
            "Staging table reference must not carry the federated catalog prefix, got: "
                + statements);
  }

  @Test
  void testSwapTableKeepsMainTableForContinuousUpdateOfTableWithoutPrimaryKey() {
    // The tracked entity event tables have no unique key on Doris, so no updated and deleted rows
    // are removed from them in a continuous update, and merging the staged rows would duplicate
    // rows. The main table is kept as it is, as before continuous updates were supported.
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .startTime(new DateTime(2020, 3, 1, 10, 0).toDate())
            .build()
            .withLatestPartition();

    List<AnalyticsTableColumn> columns =
        List.of(
            AnalyticsTableColumn.builder()
                .name("event")
                .dataType(TEXT)
                .selectExpression("event")
                .build());
    AnalyticsTable table =
        new AnalyticsTable(
            AnalyticsTableType.TRACKED_ENTITY_INSTANCE_EVENTS,
            columns,
            Logged.UNLOGGED,
            createTrackedEntityType('A'));

    when(jdbcTemplate.queryForList(sqlBuilder.tableExists(table.getMainName())))
        .thenReturn(List.of(Map.of("table_name", table.getMainName())));

    subject.swapTable(params, table);

    org.mockito.ArgumentCaptor<String> sql = org.mockito.ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(jdbcTemplate, org.mockito.Mockito.atLeastOnce())
        .execute(sql.capture());

    List<String> statements = sql.getAllValues();
    assertTrue(
        statements.stream().noneMatch(s -> s.startsWith("insert into")),
        () -> "A table without a unique key must not be merged into, got: " + statements);
    assertTrue(
        statements.stream().noneMatch(s -> s.contains(" rename ")),
        () -> "The main table must be kept, got: " + statements);
    assertTrue(
        statements.stream().anyMatch(s -> s.contains("drop table") && s.contains(table.getName())),
        () -> "Expected the staging table to be dropped, got: " + statements);
  }

  @Test
  void testSwapTableReplacesMainTableForBoundedYearsUpdateOnceMainTableExists() {
    // lastYears(1) (a normal bounded-years rebuild) makes isPartialUpdate()==true but
    // isLatestUpdate()==false -- distinct from the "latest partition" (lastYears=0) case the
    // insert-into-main branch is scoped to. removeUpdatedData() never runs for this case (it's
    // gated on isLatestUpdate()), so merging staging data into main here would leave deleted rows
    // behind forever, and dropping the staging data would leave stale values. The staging table
    // holds every year on Doris (no year filter on declarative partitioning), so the whole main
    // table is replaced instead.
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(1)
            .startTime(new DateTime(2020, 3, 1, 10, 0).toDate())
            .build();

    List<AnalyticsTableColumn> columns =
        List.of(
            AnalyticsTableColumn.builder()
                .name("dx")
                .dataType(TEXT)
                .selectExpression("dx")
                .build());
    AnalyticsTable table =
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columns, List.of(), Logged.UNLOGGED);

    // Same preconditions as the merge test: main table already exists,
    // params.isPartialUpdate()==true (via lastYears(1)), and DATA_VALUE.isLatestPartition()==true.
    when(jdbcTemplate.queryForList(sqlBuilder.tableExists(table.getMainName())))
        .thenReturn(List.of(Map.of("table_name", "analytics")));

    subject.swapTable(params, table);

    org.mockito.ArgumentCaptor<String> sql = org.mockito.ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(jdbcTemplate, org.mockito.Mockito.atLeastOnce())
        .execute(sql.capture());

    List<String> statements = sql.getAllValues();
    assertTrue(
        statements.stream()
            .anyMatch(s -> s.contains("alter table `analytics_temp` rename `analytics`;")),
        () -> "Expected the staging table to replace the main table, got: " + statements);
    assertTrue(
        statements.stream().noneMatch(s -> s.startsWith("insert into")),
        () -> "Bounded update must not merge into the main table, got: " + statements);
  }

  @Test
  void testIsReadyForContinuousUpdateWhenMainTableDoesNotExistYet() {
    AnalyticsTable table = dataValueTableFixture();

    when(jdbcTemplate.queryForList(sqlBuilder.tableExists(table.getMainName())))
        .thenReturn(List.of());

    assertTrue(subject.isReadyForContinuousUpdate(List.of(table)));

    org.mockito.Mockito.verify(jdbcTemplate, org.mockito.Mockito.never())
        .queryForList(sqlBuilder.showCreateTable(table.getMainName()));
  }

  @Test
  void testIsReadyForContinuousUpdateWhenMainTableHasUniqueKey() {
    AnalyticsTable table = dataValueTableFixture();

    when(jdbcTemplate.queryForList(sqlBuilder.tableExists(table.getMainName())))
        .thenReturn(List.of(Map.of("table_name", "analytics")));
    when(jdbcTemplate.queryForList(sqlBuilder.showCreateTable(table.getMainName())))
        .thenReturn(
            List.of(
                Map.of(
                    "Table", "analytics",
                    "Create Table",
                        "CREATE TABLE `analytics` (...) UNIQUE KEY (`id`,`year`) ...")));

    assertTrue(subject.isReadyForContinuousUpdate(List.of(table)));
  }

  @Test
  void testIsReadyForContinuousUpdateWhenMainTablePredatesUniqueKeySupport() {
    // A main table built before unique-key analytics tables were introduced remains on the
    // duplicate key model until a full rebuild recreates it. The delete step a continuous update
    // depends on only works on unique-key tables, so it must not be attempted.
    AnalyticsTable table = dataValueTableFixture();

    when(jdbcTemplate.queryForList(sqlBuilder.tableExists(table.getMainName())))
        .thenReturn(List.of(Map.of("table_name", "analytics")));
    when(jdbcTemplate.queryForList(sqlBuilder.showCreateTable(table.getMainName())))
        .thenReturn(
            List.of(
                Map.of(
                    "Table", "analytics",
                    "Create Table", "CREATE TABLE `analytics` (...) DUPLICATE KEY (`id`) ...")));

    assertFalse(subject.isReadyForContinuousUpdate(List.of(table)));
  }

  private AnalyticsTable dataValueTableFixture() {
    List<AnalyticsTableColumn> columns =
        List.of(
            AnalyticsTableColumn.builder()
                .name("dx")
                .dataType(TEXT)
                .selectExpression("dx")
                .build());
    return new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columns, List.of(), Logged.UNLOGGED);
  }
}
