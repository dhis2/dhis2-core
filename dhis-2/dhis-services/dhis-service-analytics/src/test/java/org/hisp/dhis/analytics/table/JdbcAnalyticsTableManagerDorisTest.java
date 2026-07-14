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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.period.PeriodType.PERIOD_TYPES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    when(settings.getLastSuccessfulResourceTablesUpdate()).thenReturn(new Date(0L));
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
    assertTrue(sqlBuilder.createTable(table).contains("unique key (`id`,`year`)"));
  }

  @Test
  void testRemoveUpdatedDataUsesUsingJoinNotSubquery() {
    Date lastFullTableUpdate = new DateTime(2019, 3, 1, 2, 0).toDate();
    Date lastLatestPartitionUpdate = new DateTime(2019, 3, 1, 9, 0).toDate();
    Date startTime = new DateTime(2019, 3, 1, 10, 0).toDate();

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().startTime(startTime).build().withLatestPartition();

    List<Map<String, Object>> queryResp = new ArrayList<>();
    queryResp.add(Map.of("dataelementid", 1));

    when(settings.getLastSuccessfulAnalyticsTablesUpdate()).thenReturn(lastFullTableUpdate);
    when(settings.getLastSuccessfulLatestAnalyticsPartitionUpdate())
        .thenReturn(lastLatestPartitionUpdate);
    when(jdbcTemplate.queryForList(org.mockito.Mockito.anyString())).thenReturn(queryResp);
    when(configurationService.getConfiguration()).thenReturn(configuration);
    when(configuration.getDataOutputPeriodTypes())
        .thenReturn(PERIOD_TYPES.stream().collect(toUnmodifiableSet()));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);
    assertEquals(1, tables.size());

    subject.removeUpdatedData(tables);

    org.mockito.ArgumentCaptor<String> sql = org.mockito.ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(jdbcTemplate).execute(sql.capture());

    assertTrue(sql.getValue().contains("using"));
    assertTrue(sql.getValue().contains("ax.id ="));
    assertTrue(!sql.getValue().contains("in ("));
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
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columns, List.of(), Logged.UNLOGGED);

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
}
