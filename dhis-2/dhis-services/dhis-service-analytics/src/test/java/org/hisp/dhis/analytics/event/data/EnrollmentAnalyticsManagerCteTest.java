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
package org.hisp.dhis.analytics.event.data;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.OU_COLUMN_NAME;
import static org.hisp.dhis.common.DimensionConstants.OPTION_SEP;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.AGGREGATE;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_DATABASE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.programindicator.DefaultProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagInfoInitializer;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.common.AnalyticsCustomHeader;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.external.conf.DefaultDhisConfigurationProvider;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EnrollmentAnalyticsManagerCteTest extends EventAnalyticsTest {
  private JdbcEnrollmentAnalyticsManager subject;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private ExecutionPlanStore executionPlanStore;

  @Mock private SqlRowSet rowSet;

  @Mock private SqlRowSetMetaData rowSetMetaData;

  @Mock private ProgramIndicatorService programIndicatorService;

  @Mock private DataElementService dataElementService;

  @Spy private PostgreSqlAnalyticsSqlBuilder sqlBuilder = new PostgreSqlAnalyticsSqlBuilder();

  @Mock private SystemSettingsService systemSettingsService;

  @Mock private OrganisationUnitResolver organisationUnitResolver;

  @Mock private PiDisagInfoInitializer piDisagInfoInitializer;

  @Mock private PiDisagQueryGenerator piDisagQueryGenerator;

  private QueryItemFilterBuilder filterBuilder;

  @Spy
  private EnrollmentTimeFieldSqlRenderer enrollmentTimeFieldSqlRenderer =
      new EnrollmentTimeFieldSqlRenderer(sqlBuilder);

  @Spy private SystemSettings systemSettings;

  @Mock private DefaultDhisConfigurationProvider config;

  @Captor private ArgumentCaptor<String> sql;

  @BeforeEach
  public void setUp() {
    when(jdbcTemplate.queryForRowSet(anyString())).thenReturn(this.rowSet);
    when(systemSettingsService.getCurrentSettings()).thenReturn(systemSettings);
    when(systemSettings.getUseExperimentalAnalyticsQueryEngine()).thenReturn(true);
    when(systemSettings.getOrgUnitCentroidsInEventsAnalytics()).thenReturn(false);
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn("postgresql");
    when(rowSet.getMetaData()).thenReturn(rowSetMetaData);
    // Mock stage.ou CTE context for stage OU dimension tests
    when(organisationUnitResolver.buildStageOuCteContext(any(), any()))
        .thenReturn(
            new OrganisationUnitResolver.StageOuCteContext(
                "\"ou\"", "", "\"ouname\" as ev_ouname, \"oucode\" as ev_oucode,"));
    DefaultProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder =
        new DefaultProgramIndicatorSubqueryBuilder(
            programIndicatorService,
            systemSettingsService,
            new PostgreSqlBuilder(),
            dataElementService);
    ColumnMapper columnMapper = new ColumnMapper(sqlBuilder, systemSettingsService);
    filterBuilder = new QueryItemFilterBuilder(organisationUnitResolver, sqlBuilder);

    subject =
        new JdbcEnrollmentAnalyticsManager(
            jdbcTemplate,
            programIndicatorService,
            programIndicatorSubqueryBuilder,
            piDisagInfoInitializer,
            piDisagQueryGenerator,
            enrollmentTimeFieldSqlRenderer,
            executionPlanStore,
            systemSettingsService,
            config,
            sqlBuilder,
            organisationUnitResolver,
            columnMapper,
            filterBuilder);
  }

  @Test
  void verifyGetEnrollmentsWithoutMissingValueAndNumericValuesInFilter() {
    String numericValues = String.join(OPTION_SEP, "10", "11", "12");
    String inClause = " in (" + String.join(",", numericValues.split(OPTION_SEP)) + ")";

    // Non-NV filters are now pushed into the CTE for better query performance
    String cte =
        noEof(
                """
                ( select enrollment,
                         "fWIAEtYVEGk" as value,
                         row_number() over (
                            partition by enrollment order by occurreddate desc, created desc ) as rn
                  from analytics_event_%s
                  where eventstatus != 'SCHEDULE' and ps = '%s' and "fWIAEtYVEGk"%s )
                """)
            .formatted(programA.getUid(), programStage.getUid(), inClause);

    Collection<Consumer<String>> assertions =
        Arrays.asList(
            sql -> assertThat(sql, containsString(cte)),
            sql -> assertThat(sql, containsString(inClause)));

    testIt(IN, numericValues, assertions);
  }

  @Test
  void verifyGetEnrollmentsWithNvOnlyFilterKeepsNvInWhereClause() {
    // NV-only filters should NOT be pushed into CTE - they stay in WHERE clause
    // because NV semantics require checking if the most recent event's value is null
    String cteWithoutNvFilter =
        noEof(
                """
                ( select enrollment,
                         "fWIAEtYVEGk" as value,
                         row_number() over (
                            partition by enrollment order by occurreddate desc, created desc ) as rn
                  from analytics_event_%s
                  where eventstatus != 'SCHEDULE' and ps = '%s' )
                """)
            .formatted(programA.getUid(), programStage.getUid());

    Collection<Consumer<String>> assertions =
        Arrays.asList(
            // CTE should NOT contain NV filter condition
            sql -> assertThat(sql, containsString(cteWithoutNvFilter)),
            // NV filter should be in WHERE clause as IS NULL check
            sql -> assertThat(sql, containsString("is null")));

    testIt(IN, NV, assertions);
  }

  @Test
  void verifyGetEnrollmentsWithMixedNvAndNumericValuesInFilter() {
    // Mixed filters: non-NV values go to CTE, NV is stripped from CTE filter
    String numericValues = String.join(OPTION_SEP, "10", "11", NV);
    String nonNvInClause = " in (10,11)";

    // CTE should only contain non-NV values (NV is stripped)
    String cteWithNonNvFilter =
        noEof(
                """
                ( select enrollment,
                         "fWIAEtYVEGk" as value,
                         row_number() over (
                            partition by enrollment order by occurreddate desc, created desc ) as rn
                  from analytics_event_%s
                  where eventstatus != 'SCHEDULE' and ps = '%s' and "fWIAEtYVEGk"%s )
                """)
            .formatted(programA.getUid(), programStage.getUid(), nonNvInClause);

    Collection<Consumer<String>> assertions =
        Arrays.asList(
            sql -> assertThat(sql, containsString(cteWithNonNvFilter)),
            // Non-NV values should be in CTE
            sql -> assertThat(sql, containsString(nonNvInClause)),
            // NV should NOT appear in CTE filter - it's stripped
            sql -> assertThat(sql, not(containsString("in (10,11," + NV + ")"))));

    testIt(IN, numericValues, assertions);
  }

  @Test
  void verifyGetEnrollmentsWithTextValuesInFilter() {
    String textValues = String.join(OPTION_SEP, "ValueA", "ValueB", "ValueC");
    String inClause = " in ('ValueA','ValueB','ValueC')";

    // Text filters are also pushed into CTE
    String cte =
        noEof(
                """
                ( select enrollment,
                         "fWIAEtYVEGk" as value,
                         row_number() over (
                            partition by enrollment order by occurreddate desc, created desc ) as rn
                  from analytics_event_%s
                  where eventstatus != 'SCHEDULE' and ps = '%s' and "fWIAEtYVEGk"%s )
                """)
            .formatted(programA.getUid(), programStage.getUid(), inClause);

    Collection<Consumer<String>> assertions =
        Arrays.asList(
            sql -> assertThat(sql, containsString(cte)),
            sql -> assertThat(sql, containsString(inClause)));

    testIt(IN, textValues, ValueType.TEXT, assertions);
  }

  @Test
  void verifyGetEnrollmentsWithStageOuDimensionIncludesOuNameAndCode() {
    // Stage.ou dimensions should include ev_ouname and ev_oucode columns
    // This tests the STAGE_OU_NAME_COLUMN and STAGE_OU_CODE_COLUMN constants
    EventQueryParams params = createStageOuRequestParams();

    subject.getEnrollments(params, new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();

    // Verify CTE includes ev_ouname and ev_oucode columns
    assertThat(generatedSql, containsString("ev_ouname"));
    assertThat(generatedSql, containsString("ev_oucode"));
    // Verify output aliases use stage UID prefix
    assertThat(generatedSql, containsString(programStage.getUid() + ".ouname"));
    assertThat(generatedSql, containsString(programStage.getUid() + ".oucode"));
  }

  @Test
  void verifyAggregateEnrollmentWithStageDateDimensionGeneratesValidSql() {
    // Test that aggregate enrollment queries with stage-specific EVENT_DATE:
    // 1. Use a per-stage filter CTE (latest_events_<stageUid>)
    // 2. Do NOT create a redundant program stage CTE
    // 3. Map the header column correctly (eventdate -> ev_occurreddate)
    EventQueryParams params = createAggregateEnrollmentWithStageDateParams();

    ListGrid grid = new ListGrid();
    // Add headers that match what would be returned from the endpoint
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, false));
    grid.addHeader(
        new GridHeader(
            programStage.getUid() + ".eventdate", "Event date", ValueType.DATE, false, false));

    subject.getEnrollments(params, grid, 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();

    // The SQL should contain a per-stage filter CTE
    assertThat(generatedSql, containsString("latest_events_" + programStage.getUid()));

    // The SQL should NOT contain a separate CTE like 'Zj7UnCAulEk_occurreddate_0'
    // (avoiding redundant CTE generation)
    assertThat(
        generatedSql, not(containsString(programStage.getUid() + "_" + OCCURRED_DATE_COLUMN_NAME)));

    // The filter CTE should use the ev_occurreddate alias for the date column
    assertThat(generatedSql, containsString("ev_occurreddate"));
  }

  private EventQueryParams createAggregateEnrollmentWithStageDateParams() {
    // Create a stage-specific EVENT_DATE query item (like Zj7UnCAulEk.EVENT_DATE:THIS_YEAR)
    BaseDimensionalItemObject dateItem = new BaseDimensionalItemObject(OCCURRED_DATE_COLUMN_NAME);
    QueryItem queryItem =
        new QueryItem(dateItem, programA, null, ValueType.DATE, null, null)
            .withCustomHeader(AnalyticsCustomHeader.forEventDate(programStage));
    queryItem.setProgramStage(programStage);
    queryItem.setProgram(programA);

    // Add a date filter (like :THIS_YEAR which gets translated to date bounds)
    queryItem.addFilter(new QueryFilter(QueryOperator.GE, "2026-01-01"));
    queryItem.addFilter(new QueryFilter(QueryOperator.LE, "2026-12-31"));

    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.addItem(queryItem);
    // Set aggregate enrollment mode
    params.withEndpointAction(AGGREGATE);
    return params.build();
  }

  private EventQueryParams createStageOuRequestParams() {
    // Create a stage.ou dimension query item
    BaseDimensionalItemObject ouItem = new BaseDimensionalItemObject(OU_COLUMN_NAME);
    QueryItem queryItem = new QueryItem(ouItem);
    queryItem.setItem(ouItem);
    queryItem.setProgramStage(programStage);
    queryItem.setProgram(programA);
    queryItem.setValueType(ValueType.ORGANISATION_UNIT);

    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.addItem(queryItem);
    return params.build();
  }

  @Override
  String getTableName() {
    return "analytics_enrollment";
  }

  private void testIt(
      QueryOperator operator, String filter, Collection<Consumer<String>> assertions) {
    testIt(operator, filter, ValueType.INTEGER, assertions);
  }

  private void testIt(
      QueryOperator operator,
      String filter,
      ValueType valueType,
      Collection<Consumer<String>> assertions) {
    subject.getEnrollments(
        createRequestParamsWithFilter(programStage, valueType, operator, filter),
        new ListGrid(),
        10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertions.forEach(consumer -> consumer.accept(sql.getValue()));
  }

  private String noEof(String sql) {
    return sql.replaceAll("\\s+", " ").trim();
  }
}
