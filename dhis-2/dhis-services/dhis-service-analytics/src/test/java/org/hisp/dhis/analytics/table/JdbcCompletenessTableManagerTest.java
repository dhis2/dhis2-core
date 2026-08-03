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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.period.PeriodType.PERIOD_TYPES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.util.DateUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JdbcCompletenessTableManagerTest {
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

  @Spy private SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  @InjectMocks private JdbcCompletenessTableManager manager;

  @BeforeEach
  void setUp() {
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    when(settings.getLastSuccessfulResourceTablesUpdate()).thenReturn(new Date(0L));
    when(configurationService.getConfiguration()).thenReturn(configuration);
    when(configuration.getDataOutputPeriodTypes())
        .thenReturn(PERIOD_TYPES.stream().collect(toUnmodifiableSet()));
  }

  @Test
  void testGetDataYearsQueryWithLastYears() {
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .startTime(DateUtils.getDate(2025, 5, 1, 10, 45))
            .today(DateUtils.getDate(2025, 5, 1))
            .lastYears(2)
            .build();

    String expected =
        """
        select distinct(extract(year from ps.startdate)) \
        from "completedatasetregistration" cdr \
        inner join analytics_rs_periodstructure ps on cdr.periodid=ps.periodid \
        where ps.startdate is not null \
        and cdr.date < '2025-05-01T10:45:00' \
        and ps.startdate >= '2024-01-01'\
        """;

    assertEquals(expected, manager.getDataYearsQuery(params));
  }

  @Test
  void testRemoveUpdatedDataUsesSubqueryDeleteForPostgres() {
    Date lastFullTableUpdate = new DateTime(2019, 3, 1, 2, 0).toDate();
    Date lastLatestPartitionUpdate = new DateTime(2019, 3, 1, 9, 0).toDate();
    Date startTime = new DateTime(2019, 3, 1, 10, 0).toDate();

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().startTime(startTime).build().withLatestPartition();

    List<Map<String, Object>> queryResp = new ArrayList<>();
    queryResp.add(Map.of("datasetid", 1));

    when(settings.getLastSuccessfulAnalyticsTablesUpdate()).thenReturn(lastFullTableUpdate);
    when(settings.getLastSuccessfulLatestAnalyticsPartitionUpdate())
        .thenReturn(lastLatestPartitionUpdate);
    when(jdbcTemplate.queryForList(anyString())).thenReturn(queryResp);

    List<AnalyticsTable> tables = manager.getAnalyticsTables(params);
    assertEquals(1, tables.size());

    manager.removeUpdatedData(tables);

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate, times(1)).execute(sql.capture());

    String delete = sql.getValue();
    assertTrue(delete.toLowerCase().contains("delete from"));
    assertTrue(delete.contains("where ax.id in ("));
    assertTrue(
        delete.contains(
            "select concat(ds.uid,'-',ps.iso,'-',ous.organisationunituid,'-',acs.categoryoptioncombouid) as id"));
    assertTrue(delete.contains("completedatasetregistration"));
    assertTrue(delete.contains("cdr.lastupdated >="));
    assertTrue(delete.contains("cdr.lastupdated <"));
    assertFalse(
        delete.toLowerCase().contains("using"),
        () ->
            "Postgres path must use the subquery delete, not the Doris staging-keys using-join"
                + " delete, got: "
                + delete);
  }
}
