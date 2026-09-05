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

import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.ClickHouseSqlBuilder;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Covers the {@code swapTable()} branch for databases with declarative partitioning that do NOT
 * support continuous analytics (e.g. ClickHouse): staging data must not be inserted into the main
 * table, the staging table is simply dropped, same as before the Doris continuous-analytics
 * pipeline was wired up.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JdbcAnalyticsTableManagerClickHouseTest {
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

  @Mock private DataElementService dataElementService;

  @Spy private SqlBuilder sqlBuilder = new ClickHouseSqlBuilder("dhis2");

  @InjectMocks private JdbcAnalyticsTableManager subject;

  @BeforeEach
  void setUp() {
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    when(settings.getLastSuccessfulResourceTablesUpdate()).thenReturn(new Date(0L));
  }

  @Test
  void testSwapTableDoesNotInsertStagingDataWhenContinuousAnalyticsNotSupported() {
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
    // skipMasterTable branch. ClickHouse supports declarative partitioning but not continuous
    // analytics, so neither the reparenting branch nor the new insert-into-main branch should
    // fire here.
    when(jdbcTemplate.queryForList(sqlBuilder.tableExists(table.getMainName())))
        .thenReturn(List.of(Map.of("table_name", "analytics")));

    subject.swapTable(params, table);

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    Mockito.verify(jdbcTemplate, Mockito.times(1)).execute(sql.capture());

    String statement = sql.getValue();
    assertFalse(
        statement.startsWith("insert into"), () -> "Unexpected insert statement: " + statement);
    assertTrue(
        statement.contains("drop table"),
        () -> "Expected a drop-table statement, got: " + statement);
  }
}
