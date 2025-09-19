/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class JdbcValidationResultTableManagerTest {

  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private CategoryService categoryService;

  @Mock private SystemSettingsProvider settingsProvider;

  @Mock private DataApprovalLevelService dataApprovalLevelService;

  @Mock private ResourceTableService resourceTableService;

  @Mock private AnalyticsTableHookService analyticsTableHookService;

  @Mock private PartitionManager partitionManager;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private AnalyticsTableSettings analyticsTableSettings;

  @Mock private PeriodDataProvider periodDataProvider;

  @Spy private SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  private JdbcValidationResultTableManager subject;

  private static final Date START_TIME = new DateTime(2021, 5, 10, 0, 0).toDate();

  @BeforeEach
  void setUp() {
    lenient().when(settingsProvider.getCurrentSettings()).thenReturn(SystemSettings.of(Map.of()));
    // Avoid NPEs when building dynamic columns
    lenient().when(organisationUnitService.getFilledOrganisationUnitLevels()).thenReturn(List.of());
    lenient()
        .when(idObjectManager.getDataDimensionsNoAcl(OrganisationUnitGroupSet.class))
        .thenReturn(List.of());
    lenient()
        .when(categoryService.getAttributeDataDimensionCategoriesNoAcl())
        .thenReturn(List.of());

    subject =
        new JdbcValidationResultTableManager(
            idObjectManager,
            organisationUnitService,
            categoryService,
            settingsProvider,
            dataApprovalLevelService,
            resourceTableService,
            analyticsTableHookService,
            partitionManager,
            jdbcTemplate,
            analyticsTableSettings,
            periodDataProvider,
            sqlBuilder);
  }

  @Test
  @DisplayName("Returns VALIDATION_RESULT as the analytics table type for this manager")
  void testGetAnalyticsTableType() {
    assertEquals(AnalyticsTableType.VALIDATION_RESULT, subject.getAnalyticsTableType());
  }

  @Test
  @DisplayName("Reports existing DB table name as analytics_validationresult")
  void testGetExistingDatabaseTables() {
    Set<String> tables = subject.getExistingDatabaseTables();
    assertEquals(Set.of("analytics_validationresult"), tables);
  }

  @Test
  @DisplayName("Builds partition check clause using provided year (e.g. year = 2022)")
  void testGetPartitionChecks() {
    assertEquals(List.of("year = 2022"), subject.getPartitionChecks(2022, new Date()));
  }

  @Test
  @DisplayName(
      "Regular update path builds partitions for each data year returned by getDataYears()")
  void testGetAnalyticsTablesWithYearsCreatesPartitions() {
    when(jdbcTemplate.queryForList(anyString(), eq(Integer.class))).thenReturn(List.of(2020, 2021));

    AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertEquals(1, tables.size());
    AnalyticsTable t = tables.get(0);
    assertEquals(2, t.getTablePartitions().size());
  }

  @Test
  @DisplayName("Latest-update path returns no tables but still constructs columns via getColumns()")
  void testGetAnalyticsTablesLatestUpdateReturnsEmpty() {
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(org.hisp.dhis.analytics.table.model.AnalyticsTablePartition.LATEST_PARTITION)
            .build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);
    assertEquals(0, tables.size());
  }

  @Test
  @DisplayName(
      "Latest-update path invokes getColumns() collaborators even though result set is empty")
  void testGetAnalyticsTablesLatestUpdateUsesGetColumns() {
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(org.hisp.dhis.analytics.table.model.AnalyticsTablePartition.LATEST_PARTITION)
            .build();

    // Call method; even if latest returns empty list, getColumns() should run
    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);
    assertEquals(0, tables.size());
    // Verify underlying collaborators invoked by getColumns()
    verify(organisationUnitService).getFilledOrganisationUnitLevels();
    verify(idObjectManager).getDataDimensionsNoAcl(OrganisationUnitGroupSet.class);
    verify(categoryService).getAttributeDataDimensionCategoriesNoAcl();
  }

  @Test
  @DisplayName(
      "Regular update includes expected fixed/dynamic columns and value column uses vrs.created")
  void testGetAnalyticsTablesRegularIncludesExpectedColumns() {
    // One data year so we build one table with columns from getColumns()
    when(jdbcTemplate.queryForList(anyString(), eq(Integer.class))).thenReturn(List.of(2022));

    AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);
    assertFalse(tables.isEmpty());

    AnalyticsTable table = tables.get(0);
    List<org.hisp.dhis.analytics.table.model.AnalyticsTableColumn> cols =
        table.getAnalyticsTableColumns();

    // Verify key fixed/dynamic columns exist
    assertThat(
        cols.stream()
            .map(org.hisp.dhis.analytics.table.model.AnalyticsTableColumn::getName)
            .toList(),
        org.hamcrest.Matchers.hasItems(
            "dx", "pestartdate", "peenddate", "year", "monthly", "value"));

    // Verify the 'value' column comes from vrs.created
    org.hisp.dhis.analytics.table.model.AnalyticsTableColumn valueCol =
        cols.stream().filter(c -> c.getName().equals("value")).findFirst().orElseThrow();
    assertEquals("vrs.created as value", valueCol.getSelectExpression());
  }

  @Test
  @DisplayName(
      "populateTable generates expected SELECT and JOINs with time filters and OU alignment")
  void testPopulateTableBuildsExpectedSql() {
    when(jdbcTemplate.queryForList(anyString(), eq(Integer.class))).thenReturn(List.of(2021));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().startTime(START_TIME).build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);
    assertFalse(tables.isEmpty());

    // Use a simple partition based on the built table
    AnalyticsTablePartition partition = tables.get(0).getTablePartitions().get(0);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

    subject.populateTable(params, partition);

    verify(jdbcTemplate).execute(sqlCaptor.capture());

    String sql = sqlCaptor.getValue();

    // Select includes the fact column
    assertThat(sql, containsString("vrs.created as value"));
    // Time filter and non-null guard
    assertThat(sql, containsString("vrs.created <"));
    assertThat(sql, containsString("vrs.created is not null"));
    // Join clauses and OU groupset alignment
    assertThat(sql, containsString("analytics_rs_periodstructure ps"));
    assertThat(sql, containsString("analytics_rs_organisationunitgroupsetstructure ougs"));
    assertThat(sql, containsString("ougs.startdate is null or ps.monthstartdate=ougs.startdate"));
    // No legacy replacement expected in FROM/JOIN clauses
  }

  @Test
  @DisplayName("getDataYears SQL includes from-date filter when lastYears is specified")
  void testGetAnalyticsTablesAddsFromDateClauseWhenLastYearsSpecified() {
    // Make getDataYears run and capture the SQL
    when(jdbcTemplate.queryForList(anyString(), eq(Integer.class))).thenReturn(List.of());

    Date today = new DateTime(2021, 6, 15, 0, 0).toDate();
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(2)
            .today(today)
            .startTime(START_TIME)
            .build();

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    subject.getAnalyticsTables(params);

    verify(jdbcTemplate).queryForList(sqlCaptor.capture(), eq(Integer.class));
    String sql = sqlCaptor.getValue();
    // From date should be first day of the previous year: 2020-01-01
    assertThat(sql, containsString("and ps.startdate >= '2020-01-01'"));
  }

  @Test
  @DisplayName("getPartitionChecks throws when year is null to enforce non-null input")
  void testGetPartitionChecksNullYearThrows() {
    org.junit.jupiter.api.Assertions.assertThrows(
        NullPointerException.class, () -> subject.getPartitionChecks(null, new Date()));
  }

  @Test
  @DisplayName(
      "When declarative partitioning is supported, partition clause (ps.year = ...) is omitted")
  void testPopulateTableOmitsPartitionClauseWhenDeclarativePartitioningSupported() {
    // Force declarative partitioning and verify no 'ps.year = ...' in SQL
    when(jdbcTemplate.queryForList(anyString(), eq(Integer.class))).thenReturn(List.of(2021));
    when(sqlBuilder.supportsDeclarativePartitioning()).thenReturn(true);

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().startTime(START_TIME).build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);
    assertFalse(tables.isEmpty());
    AnalyticsTablePartition partition = tables.get(0).getTablePartitions().get(0);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    subject.populateTable(params, partition);
    verify(jdbcTemplate).execute(sqlCaptor.capture());

    String sql = sqlCaptor.getValue();
    assertThat(sql, org.hamcrest.Matchers.not(containsString("ps.year = 2021")));
  }
}
