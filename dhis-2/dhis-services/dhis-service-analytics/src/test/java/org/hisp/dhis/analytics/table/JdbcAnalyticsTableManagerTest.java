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

import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.Logged.LOGGED;
import static org.hisp.dhis.db.model.Logged.UNLOGGED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Lars Helge Overland
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class JdbcAnalyticsTableManagerTest {
  @Mock private SystemSettingManager systemSettingManager;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private AnalyticsTableSettings analyticsTableSettings;

  @Mock private PeriodDataProvider periodDataProvider;

  @Mock private DataApprovalLevelService dataApprovalLevelService;

  @Spy private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  private JdbcAnalyticsTableManager subject;

  @BeforeEach
  public void setUp() {
    subject =
        new JdbcAnalyticsTableManager(
            mock(IdentifiableObjectManager.class),
            mock(OrganisationUnitService.class),
            mock(CategoryService.class),
            systemSettingManager,
            dataApprovalLevelService,
            mock(ResourceTableService.class),
            mock(AnalyticsTableHookService.class),
            mock(PartitionManager.class),
            mock(DatabaseInfoProvider.class),
            jdbcTemplate,
            analyticsTableSettings,
            periodDataProvider,
            sqlBuilder);
  }

  @Test
  void testGetRegularAnalyticsTable() {
    Date startTime = new DateTime(2019, 3, 1, 10, 0).toDate();
    List<Integer> dataYears = List.of(2018, 2019);

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().withStartTime(startTime).build();

    when(analyticsTableSettings.getTableLogged()).thenReturn(UNLOGGED);
    when(jdbcTemplate.queryForList(Mockito.anyString(), ArgumentMatchers.<Class<Integer>>any()))
        .thenReturn(dataYears);
    when(analyticsTableSettings.getTableLogged()).thenReturn(UNLOGGED);

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertEquals(1, tables.size());

    AnalyticsTable table = tables.get(0);

    assertNotNull(table);
    assertNotNull(table.getTablePartitions());
    assertEquals(2, table.getTablePartitions().size());

    AnalyticsTablePartition partitionA = table.getTablePartitions().get(0);
    AnalyticsTablePartition partitionB = table.getTablePartitions().get(1);

    assertNotNull(partitionA);
    assertNotNull(partitionA.getStartDate());
    assertNotNull(partitionA.getEndDate());
    assertTrue(partitionA.isUnlogged());
    assertEquals(
        partitionA.getYear().intValue(), new DateTime(partitionA.getStartDate()).getYear());

    assertNotNull(partitionB);
    assertNotNull(partitionB.getStartDate());
    assertNotNull(partitionB.getEndDate());
    assertTrue(partitionB.isUnlogged());
    assertEquals(
        partitionB.getYear().intValue(), new DateTime(partitionB.getStartDate()).getYear());
  }

  @Test
  void testGetRegularAnalyticsTableLogged() {
    Date startTime = new DateTime(2019, 3, 1, 10, 0).toDate();
    List<Integer> dataYears = List.of(2018, 2019);

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().withStartTime(startTime).build();

    when(analyticsTableSettings.getTableLogged()).thenReturn(LOGGED);
    when(jdbcTemplate.queryForList(Mockito.anyString(), ArgumentMatchers.<Class<Integer>>any()))
        .thenReturn(dataYears);

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertEquals(1, tables.size());

    AnalyticsTable table = tables.get(0);

    assertNotNull(table);
    assertNotNull(table.getTablePartitions());
    assertEquals(2, table.getTablePartitions().size());

    AnalyticsTablePartition partitionA = table.getTablePartitions().get(0);
    AnalyticsTablePartition partitionB = table.getTablePartitions().get(1);

    assertNotNull(partitionA);
    assertNotNull(partitionA.getStartDate());
    assertNotNull(partitionA.getEndDate());
    assertFalse(partitionA.isUnlogged());
    assertEquals(
        partitionA.getYear().intValue(), new DateTime(partitionA.getStartDate()).getYear());

    assertNotNull(partitionB);
    assertNotNull(partitionB.getStartDate());
    assertNotNull(partitionB.getEndDate());
    assertFalse(partitionB.isUnlogged());
    assertEquals(
        partitionB.getYear().intValue(), new DateTime(partitionB.getStartDate()).getYear());
  }

  @Test
  void testGetLatestAnalyticsTable() {
    Date lastFullTableUpdate = new DateTime(2019, 3, 1, 2, 0).toDate();
    Date lastLatestPartitionUpdate = new DateTime(2019, 3, 1, 9, 0).toDate();
    Date startTime = new DateTime(2019, 3, 1, 10, 0).toDate();

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withStartTime(startTime)
            .withLatestPartition()
            .build();

    List<Map<String, Object>> queryResp = new ArrayList<>();
    queryResp.add(Map.of("dataelementid", 1));

    when(systemSettingManager.getDateSetting(SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE))
        .thenReturn(lastFullTableUpdate);
    when(systemSettingManager.getDateSetting(
            SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE))
        .thenReturn(lastLatestPartitionUpdate);
    when(analyticsTableSettings.getTableLogged()).thenReturn(UNLOGGED);
    when(jdbcTemplate.queryForList(Mockito.anyString())).thenReturn(queryResp);

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertEquals(1, tables.size());

    AnalyticsTable table = tables.get(0);

    assertNotNull(table);
    assertNotNull(table.getTablePartitions());
    assertEquals(1, table.getTablePartitions().size());

    AnalyticsTablePartition partition = table.getLatestTablePartition();

    assertNotNull(partition);
    assertTrue(partition.isLatestPartition());
    assertEquals(lastFullTableUpdate, partition.getStartDate());
    assertEquals(startTime, partition.getEndDate());
    assertTrue(partition.isUnlogged());
  }

  @Test
  void testGetLatestAnalyticsTableNoFullTableUpdate() {
    Date lastLatestPartitionUpdate = new DateTime(2019, 3, 1, 9, 0).toDate();
    Date startTime = new DateTime(2019, 3, 1, 10, 0).toDate();

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withStartTime(startTime)
            .withLatestPartition()
            .build();

    when(systemSettingManager.getDateSetting(SettingKey.LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE))
        .thenReturn(null);
    when(systemSettingManager.getDateSetting(SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE))
        .thenReturn(null);
    when(systemSettingManager.getDateSetting(
            SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE))
        .thenReturn(lastLatestPartitionUpdate);
    assertThrows(IllegalArgumentException.class, () -> subject.getAnalyticsTables(params));
  }

  @Test
  @DisplayName(
      "Verify if the method swapParentTable is called with the swapped table name not the staging table name")
  void testSwapTable() {
    Date startTime = new DateTime(2019, 3, 1, 10, 0).toDate();
    AnalyticsTable table =
        new AnalyticsTable(
            AnalyticsTableType.DATA_VALUE,
            List.of(
                AnalyticsTableColumn.builder()
                    .name("year")
                    .dataType(INTEGER)
                    .selectExpression("")
                    .build()),
            LOGGED);
    table.addTablePartition(List.of(), 2023, new DateTime(2023, 1, 1, 0, 0).toDate(), null);
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withStartTime(startTime)
            .withLatestPartition()
            .build();
    when(jdbcTemplate.queryForList(any())).thenReturn(List.of(Map.of("table_name", "analytic")));

    Table swappedPartition = table.getTablePartitions().get(0).swapFromStaging();
    subject.swapTable(params, table);
    assertEquals("analytics_2023_temp", table.getTablePartitions().get(0).getName());
    assertEquals("analytics_2023", swappedPartition.getName());

    verify(sqlBuilder).swapParentTable(swappedPartition, "analytics_temp", "analytics");
  }

  @Test
  void testGetApprovalSelectExpression() {
    String expected = "coalesce(des.datasetapprovallevel, aon.approvallevel, da.minlevel, 999)";

    assertEquals(expected, subject.getApprovalSelectExpression());
  }

  @Test
  void testGetApprovalJoinClause() {
    when(dataApprovalLevelService.getOrganisationUnitApprovalLevels())
        .thenReturn(
            Set.of(
                new OrganisationUnitLevel(1, "National"),
                new OrganisationUnitLevel(3, "District")));

    String expected =
        """
        left join analytics_rs_dataapprovalminlevel da on des.workflowid=da.workflowid \
        and da.periodid=dv.periodid and da.attributeoptioncomboid=dv.attributeoptioncomboid \
        and (ous.idlevel1 = da.organisationunitid or ous.idlevel3 = da.organisationunitid) \
        """;

    assertEquals(expected, subject.getApprovalJoinClause());
  }
}
