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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.OU_COLUMN_NAME;
import static org.hisp.dhis.common.DimensionConstants.OPTION_SEP;
import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.common.QueryOperator.NEQ;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.AGGREGATE;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_DATABASE;
import static org.hisp.dhis.program.EnrollmentStatus.ACTIVE;
import static org.hisp.dhis.program.EnrollmentStatus.COMPLETED;
import static org.hisp.dhis.test.TestBase.createPeriodDimensions;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramIndicator;
import static org.hisp.dhis.test.TestBase.getDate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.programindicator.DefaultProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagInfoInitializer;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageDatePeriodBucketSqlRenderer;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageOrgUnitSqlService;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageQueryItemClassifier;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageQuerySqlFacade;
import org.hisp.dhis.analytics.event.data.stage.StageQuerySqlFacade;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.common.AnalyticsCustomHeader;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.ClickHouseAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.DorisAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.external.conf.DefaultDhisConfigurationProvider;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.test.random.BeanRandomizer;
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

  private final BeanRandomizer rnd = BeanRandomizer.create();

  @Spy private SystemSettings systemSettings;

  @Mock private DefaultDhisConfigurationProvider config;

  @Captor private ArgumentCaptor<String> sql;

  private String programAUid;

  @BeforeEach
  public void setUp() {
    when(jdbcTemplate.queryForRowSet(anyString())).thenReturn(this.rowSet);
    when(systemSettingsService.getCurrentSettings()).thenReturn(systemSettings);
    when(systemSettings.getOrgUnitCentroidsInEventsAnalytics()).thenReturn(false);
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn("postgresql");
    when(rowSet.getMetaData()).thenReturn(rowSetMetaData);
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    // Mock stage.ou CTE context for stage OU dimension tests
    when(organisationUnitResolver.buildStageOuCteContext(any(), any()))
        .thenReturn(
            new OrganisationUnitResolver.StageOuCteContext(
                "\"ou\"", "", "\"ouname\" as ev_ouname, \"oucode\" as ev_oucode,"));
    subject = createEnrollmentAnalyticsManager(sqlBuilder, "postgresql");
    programAUid = programA.getUid().toLowerCase();
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
            .formatted(programAUid, programStage.getUid(), inClause);

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
            .formatted(programAUid, programStage.getUid());

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
            .formatted(programAUid, programStage.getUid(), nonNvInClause);

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
            .formatted(programAUid, programStage.getUid(), inClause);

    Collection<Consumer<String>> assertions =
        Arrays.asList(
            sql -> assertThat(sql, containsString(cte)),
            sql -> assertThat(sql, containsString(inClause)));

    testIt(IN, textValues, ValueType.TEXT, assertions);
  }

  @Test
  void verifyGetEnrollmentsWithStageOuDimensionIncludesOuNameAndCodeWhenRequested() {
    // Stage.ou dimensions should include ev_ouname and ev_oucode columns
    // only when explicitly requested in headers
    String stageUid = programStage.getUid();
    EventQueryParams params =
        createStageOuRequestParamsWithHeaders(Set.of(stageUid + ".ouname", stageUid + ".oucode"));

    subject.getEnrollments(params, new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();

    // Verify CTE includes ev_ouname and ev_oucode columns
    assertThat(generatedSql, containsString("ev_ouname"));
    assertThat(generatedSql, containsString("ev_oucode"));
    // Verify output aliases use stage UID prefix
    assertThat(generatedSql, containsString(stageUid + ".ouname"));
    assertThat(generatedSql, containsString(stageUid + ".oucode"));
  }

  @Test
  void verifyGetEnrollmentsWithStageOuDimensionExcludesOuNameAndCodeWithoutHeaders() {
    // Stage.ou dimensions should NOT include ouname/oucode columns
    // when not explicitly requested in headers
    EventQueryParams params = createStageOuRequestParams();

    subject.getEnrollments(params, new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();

    // CTE still includes ev_ouname and ev_oucode (they are always in the CTE)
    assertThat(generatedSql, containsString("ev_ouname"));
    assertThat(generatedSql, containsString("ev_oucode"));
    // But outer SELECT should NOT alias them with stage UID prefix
    assertThat(generatedSql, not(containsString(programStage.getUid() + ".ouname")));
    assertThat(generatedSql, not(containsString(programStage.getUid() + ".oucode")));
    // Stage-specific filtering must stay on the event-side CTE, not leak to the enrollment alias
    assertThat(generatedSql, not(containsString("ax.\"ps\"")));
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

    String generatedSql = noEof(sql.getValue());
    String baseCteSql =
        generatedSql.substring(
            generatedSql.indexOf("enrollment_aggr_base as ("),
            generatedSql.indexOf("select count(eb.enrollment) as value"));

    // The SQL should contain a per-stage filter CTE
    assertThat(generatedSql, containsString("latest_events_" + programStage.getUid()));

    // The SQL should NOT contain a separate CTE like 'Zj7UnCAulEk_occurreddate_0'
    // (avoiding redundant CTE generation)
    assertThat(
        generatedSql, not(containsString(programStage.getUid() + "_" + OCCURRED_DATE_COLUMN_NAME)));

    // The filter CTE should use the ev_occurreddate alias for the date column
    assertThat(generatedSql, containsString("ev_occurreddate"));
    // Stage event-date periods must not leak into the base enrollment-date filter
    assertThat(baseCteSql, not(containsString("enrollmentdate >=")));
  }

  @Test
  void verifyAggregateEnrollmentStageOrgUnitFilterStaysInFilterCte() {
    DataElement orgUnitDataElement =
        org.hisp.dhis.test.TestBase.createDataElement(
            'O', ValueType.ORGANISATION_UNIT, AggregationType.NONE);
    orgUnitDataElement.setUid("n1rtSHYf6O6");

    QueryItem queryItem =
        new QueryItem(
            orgUnitDataElement,
            programA,
            null,
            ValueType.ORGANISATION_UNIT,
            orgUnitDataElement.getAggregationType(),
            null);
    queryItem.setProgram(programA);
    queryItem.setProgramStage(programStage);
    queryItem.addFilter(new QueryFilter(IN, "ImspTQPwCqd"));

    when(organisationUnitResolver.resolveOrgUnits(
            any(QueryFilter.class), anyList(), any(QueryItem.class)))
        .thenReturn("ImspTQPwCqd");

    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.withEndpointAction(AGGREGATE);
    params.addItemFilter(queryItem);

    ListGrid grid = new ListGrid();
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, false));

    subject.getEnrollments(params.build(), grid, 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());
    String baseCteSql =
        generatedSql.substring(
            generatedSql.indexOf("enrollment_aggr_base as ("),
            generatedSql.indexOf("select count(eb.enrollment) as value"));

    assertThat(
        generatedSql,
        containsString(
            noEof(
                """
                latest_events_%s as (
                select enrollment, ev_n1rtSHYf6O6
                from
                """
                    .formatted(programStage.getUid()))));
    assertThat(generatedSql, containsString("\"n1rtSHYf6O6\" in ('ImspTQPwCqd')"));
    assertThat(baseCteSql, containsString("inner join latest_events_" + programStage.getUid()));
    assertThat(baseCteSql, not(containsString("and \"n1rtSHYf6O6\" in ('ImspTQPwCqd')")));
  }

  @Test
  void verifyAggregateEnrollmentUnfilteredEventStatusUsesLatestEventsCte() {
    // When a stage-specific dimension without a filter (e.g. stage.EVENT_STATUS) is requested
    // alongside a filtered stage dimension (stage.EVENT_DATE), the unfiltered dimension must
    // also be projected by the latest_events_<stage> CTE so the outer SELECT can read it from
    // the same "latest event" row. Without this, the resolver emits a reference to
    // uiogg.ev_eventstatus while the CTE doesn't project the column, producing a SQL error.
    String stageUid = programStage.getUid();

    QueryItem dateItem =
        new QueryItem(
            new BaseDimensionalItemObject(OCCURRED_DATE_COLUMN_NAME),
            programA,
            null,
            ValueType.DATE,
            null,
            null);
    dateItem.setProgram(programA);
    dateItem.setProgramStage(programStage);
    dateItem.addFilter(new QueryFilter(QueryOperator.GE, "2026-01-01"));
    dateItem.addFilter(new QueryFilter(QueryOperator.LE, "2026-12-31"));

    QueryItem statusItem =
        new QueryItem(
            new BaseDimensionalItemObject(EVENT_STATUS_COLUMN_NAME),
            programA,
            null,
            ValueType.TEXT,
            null,
            null);
    statusItem.setProgram(programA);
    statusItem.setProgramStage(programStage);

    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.addItem(dateItem);
    params.addItem(statusItem);
    params.withEndpointAction(AGGREGATE);

    ListGrid grid = new ListGrid();
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, false));
    grid.addHeader(
        new GridHeader(stageUid + ".eventdate", "Event date", ValueType.DATE, false, false));
    grid.addHeader(
        new GridHeader(stageUid + ".eventstatus", "Event status", ValueType.TEXT, false, false));

    subject.getEnrollments(params.build(), grid, 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());
    String latestEventsCteSql =
        generatedSql.substring(
            generatedSql.indexOf("latest_events_" + stageUid + " as ("),
            generatedSql.indexOf("enrollment_aggr_base as ("));

    // The latest_events CTE must project both the filtered (eventdate) and unfiltered
    // (eventstatus) stage dimensions so the outer SELECT can read them from the same row.
    assertThat(latestEventsCteSql, containsString("ev_occurreddate"));
    assertThat(latestEventsCteSql, containsString("ev_eventstatus"));

    // The redundant per-item CTE must not be created when a latest_events_<stage> already exists.
    assertThat(generatedSql, not(containsString(stageUid + "_" + EVENT_STATUS_COLUMN_NAME + "_0")));
  }

  @Test
  void verifyAggregateEnrollmentRepeatableOffsetItemKeepsPerItemCteWhenLatestEventsCteExists() {
    String stageUid = repeatableProgramStage.getUid();
    String deUid = dataElementA.getUid();

    QueryItem dateItem = createFilteredStageDateItem(repeatableProgramStage);
    QueryItem offsetItem = createRepeatableOffsetDataElementItem(stageUid, -1);

    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.addItem(dateItem);
    params.addItem(offsetItem);
    params.withEndpointAction(AGGREGATE);

    ListGrid grid = new ListGrid();
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, false));
    grid.addHeader(
        new GridHeader(stageUid + ".eventdate", "Event date", ValueType.DATE, false, false));
    grid.addHeader(
        new GridHeader(stageUid + "[-1]." + deUid, "Offset value", ValueType.NUMBER, false, false));

    subject.getEnrollments(params.build(), grid, 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());
    String latestEventsCteSql =
        generatedSql.substring(
            generatedSql.indexOf("latest_events_" + stageUid + " as ("),
            generatedSql.indexOf("enrollment_aggr_base as ("));

    assertThat(latestEventsCteSql, containsString("ev_occurreddate"));
    assertThat(latestEventsCteSql, not(containsString("ev_" + deUid)));
    assertThat(generatedSql, containsString(stageUid + "_" + deUid + "_0 as ("));
    assertThat(generatedSql, containsString("as \"" + stageUid + "[-1]." + deUid + "\""));
  }

  @Test
  void verifyAggregateEnrollmentRepeatableOffsetsAreNotDuplicatedInLatestEventsCte() {
    String stageUid = repeatableProgramStage.getUid();
    String deUid = dataElementA.getUid();

    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.addItem(createFilteredStageDateItem(repeatableProgramStage));
    params.addItem(createRepeatableOffsetDataElementItem(stageUid, -1));
    params.addItem(createRepeatableOffsetDataElementItem(stageUid, 1));
    params.withEndpointAction(AGGREGATE);

    ListGrid grid = new ListGrid();
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, false));
    grid.addHeader(
        new GridHeader(stageUid + ".eventdate", "Event date", ValueType.DATE, false, false));

    subject.getEnrollments(params.build(), grid, 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());
    String latestEventsCteSql =
        generatedSql.substring(
            generatedSql.indexOf("latest_events_" + stageUid + " as ("),
            generatedSql.indexOf("enrollment_aggr_base as ("));

    assertThat(countOccurrences(latestEventsCteSql, "ev_" + deUid), is(0));
    assertThat(generatedSql, containsString(stageUid + "_" + deUid + "_0 as ("));
    assertThat(generatedSql, containsString(stageUid + "_" + deUid + "_1 as ("));
  }

  @Test
  void verifyAggregateEnrollmentIncludesProgramStatusFilterInSql() {
    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.withEndpointAction(AGGREGATE);
    params.withEnrollmentStatuses(
        new java.util.LinkedHashSet<>(java.util.List.of(ACTIVE, COMPLETED)));

    subject.getEnrollments(params.build(), new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();

    assertThat(generatedSql, containsString("enrollmentstatus in ("));
    assertThat(generatedSql, containsString("'ACTIVE'"));
    assertThat(generatedSql, containsString("'COMPLETED'"));
  }

  @Test
  void verifyAggregateEnrollmentEventDateFilterUsesEventJoinInBaseCte() {
    EventQueryParams params = createAggregateEnrollmentWithEventDateParams();

    ListGrid grid = new ListGrid();
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, false));

    subject.getEnrollments(params, grid, 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());

    assertThat(
        generatedSql,
        containsString(
            noEof(
                """
                inner join (
                    select
                        ev.enrollment as enrollment,
                        max(ev."occurreddate") as event_occurreddate
                    from analytics_event_%s ev
                    where ev.eventstatus != 'SCHEDULE'
                      and ev."occurreddate" >= '2022-09-01' and ev."occurreddate" < '2023-09-01'
                    group by ev.enrollment
                ) evf on evf.enrollment = ax.enrollment
                """
                    .formatted(programAUid))));
    assertThat(generatedSql, not(containsString("ax.\"occurreddate\" >=")));
  }

  @Test
  void verifyAggregateEnrollmentRequestEventDateUsesEventJoinInBaseCte() {
    EventQueryParams params = createAggregateEnrollmentWithRequestEventDateParams();

    ListGrid grid = new ListGrid();
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, false));

    subject.getEnrollments(params, grid, 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());

    assertThat(
        generatedSql,
        containsString(
            noEof(
                """
                inner join (
                    select
                        ev.enrollment as enrollment,
                        max(ev."occurreddate") as event_occurreddate
                    from analytics_event_%s ev
                    where ev.eventstatus != 'SCHEDULE'
                      and ev."occurreddate" >= '2022-09-01' and ev."occurreddate" < '2023-09-01'
                    group by ev.enrollment
                ) evf on evf.enrollment = ax.enrollment
                """
                    .formatted(programAUid))));
    assertThat(generatedSql, not(containsString("where (((occurreddate >=")));
    assertThat(generatedSql, not(containsString("enrollmentdate >=")));
  }

  @Test
  void verifyAggregateEnrollmentRequestEventDateHeaderUsesLatestEventAlias() {
    EventQueryParams params = createAggregateEnrollmentWithRequestEventDateParams();

    ListGrid grid = new ListGrid();
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, false));
    grid.addHeader(new GridHeader("eventdate", "Event date", ValueType.DATE, false, false));

    subject.getEnrollments(params, grid, 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());

    assertThat(
        generatedSql,
        containsString(
            noEof(
                """
                select
                    ev.enrollment as enrollment,
                    max(ev."occurreddate") as event_occurreddate
                from analytics_event_%s ev
                """
                    .formatted(programAUid))));
    assertThat(generatedSql, containsString(eventDateJoinProjection()));
    assertThat(generatedSql, not(containsString(", \"eventdate\" from analytics_enrollment")));
  }

  @Test
  void verifyAggregateEnrollmentIncidentDateHeaderMapsToOccurredDateAlias() {
    EventQueryParams params = createAggregateEnrollmentWithIncidentDateHeaderParams();

    ListGrid grid = new ListGrid();
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, false));
    grid.addHeader(new GridHeader("incidentdate", "Incident date", ValueType.DATE, false, false));

    subject.getEnrollments(params, grid, 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());

    assertThat(generatedSql, containsString("occurreddate as \"incidentdate\""));
  }

  @Test
  void verifyAggregateEnrollmentUsesEnrollmentDateBucketForNonDefaultPeriodDimension() {
    EventQueryParams.Builder params = createRequestParamsBuilder();
    List<org.hisp.dhis.period.PeriodDimension> periods = createPeriodDimensions("202001");
    periods.forEach(period -> period.setDateField(TimeField.ENROLLMENT_DATE.name()));

    params.withPeriods(periods, "monthly");
    params.withEndpointAction(AGGREGATE);

    subject.getEnrollments(params.build(), new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();

    assertThat(generatedSql, containsString("enrollment_aggr_base as ("));
    assertThat(generatedSql, containsString("ax.enrollment"));
    assertThat(generatedSql, containsString("enrollmentdate"));
    assertThat(
        generatedSql,
        containsString(
            "(select \"monthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('month', eb.\"enrollmentdate\")::date) as \"monthly\""));
    assertThat(
        generatedSql,
        containsString(
            ", (select \"monthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('month', eb.\"enrollmentdate\")::date)"));
  }

  @Test
  void verifyAggregateEnrollmentUsesJoinBasedPeriodLookupForDoris() {
    DorisAnalyticsSqlBuilder dorisBuilder =
        new DorisAnalyticsSqlBuilder("internal", "doris-jdbc.jar");
    JdbcEnrollmentAnalyticsManager dorisSubject =
        createEnrollmentAnalyticsManager(dorisBuilder, "doris");

    EventQueryParams.Builder params = createRequestParamsBuilder();
    List<org.hisp.dhis.period.PeriodDimension> periods = createPeriodDimensions("202001");
    periods.forEach(period -> period.setDateField(TimeField.ENROLLMENT_DATE.name()));

    params.withPeriods(periods, "monthly");
    params.withEndpointAction(AGGREGATE);

    dorisSubject.getEnrollments(params.build(), new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();

    assertThat(
        generatedSql,
        containsString(
            "left join analytics_rs_dateperiodstructure dps_period_eb_enrollmentdate on dps_period_eb_enrollmentdate.`dateperiod` = cast(date_trunc(cast(eb.`enrollmentdate` as date), 'month') as date)"));
    assertThat(generatedSql, containsString("dps_period_eb_enrollmentdate.`monthly` as `monthly`"));
    assertThat(generatedSql, containsString(", dps_period_eb_enrollmentdate.`monthly`"));
  }

  @Test
  void verifyAggregateEnrollmentUsesJoinBasedPeriodLookupForClickHouse() {
    ClickHouseAnalyticsSqlBuilder clickHouseBuilder = new ClickHouseAnalyticsSqlBuilder("dhis2");
    JdbcEnrollmentAnalyticsManager clickHouseSubject =
        createEnrollmentAnalyticsManager(clickHouseBuilder, "clickhouse");

    EventQueryParams.Builder params = createRequestParamsBuilder();
    List<org.hisp.dhis.period.PeriodDimension> periods = createPeriodDimensions("202001");
    periods.forEach(period -> period.setDateField(TimeField.ENROLLMENT_DATE.name()));

    params.withPeriods(periods, "monthly");
    params.withEndpointAction(AGGREGATE);

    clickHouseSubject.getEnrollments(params.build(), new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();

    // ClickHouse cannot resolve correlated scalar subqueries that reference non-constant outer
    // columns; the period-bucket lookup is therefore emitted as a LEFT JOIN, mirroring Doris but
    // using ClickHouse identifier quoting and date functions.
    assertThat(
        generatedSql,
        containsString(
            "left join analytics_rs_dateperiodstructure dps_period_eb_enrollmentdate "
                + "on dps_period_eb_enrollmentdate.\"dateperiod\" = "
                + "toDate(date_trunc('month', toDate(eb.\"enrollmentdate\")))"));
    assertThat(
        generatedSql, containsString("dps_period_eb_enrollmentdate.\"monthly\" as \"monthly\""));
    assertThat(generatedSql, containsString(", dps_period_eb_enrollmentdate.\"monthly\""));

    // Postgres-only constructs must not leak into the ClickHouse SQL.
    assertThat(generatedSql, not(containsString("::date")));
    assertThat(generatedSql, not(containsString(" interval ")));
    assertThat(generatedSql, not(containsString("make_date")));
  }

  @Test
  void verifyAggregateEnrollmentDorisLastUpdatedFinancialPeriodDoesNotProjectLegacyPeriodColumn() {
    DorisAnalyticsSqlBuilder dorisBuilder =
        new DorisAnalyticsSqlBuilder("internal", "doris-jdbc.jar");
    JdbcEnrollmentAnalyticsManager dorisSubject =
        createEnrollmentAnalyticsManager(dorisBuilder, "doris");

    EventQueryParams.Builder params = createRequestParamsBuilder();
    List<org.hisp.dhis.period.PeriodDimension> periods = createPeriodDimensions("2018Sep");
    periods.forEach(period -> period.setDateField(TimeField.LAST_UPDATED.name()));

    params.withPeriods(periods, "financialsep");
    params.withEndpointAction(AGGREGATE);

    dorisSubject.getEnrollments(params.build(), new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());

    assertThat(
        generatedSql,
        containsString(
            "left join analytics_rs_dateperiodstructure as dps_period_ax_lastupdated on dps_period_ax_lastupdated.`dateperiod` = date_add(makedate(case when month(cast(ax.`lastupdated` as date)) >= 9 then year(cast(ax.`lastupdated` as date)) else year(cast(ax.`lastupdated` as date)) - 1 end, 1), interval (9 - 1) month)"));
    assertThat(generatedSql, containsString("ax.enrollment"));
    assertThat(generatedSql, containsString("lastupdated from analytics_enrollment"));
    assertThat(generatedSql, not(containsString(", FinancialSep,")));
    assertThat(
        generatedSql, containsString("dps_period_eb_lastupdated.`financialsep` as `financialsep`"));
  }

  @Test
  void verifyAggregateEnrollmentEventDatePeriodDoesNotProjectEventDateAsStandaloneColumn() {
    // When EVENT_DATE is the period date field (EVENT_DATE:2022Sep),
    // the eventdate header should NOT appear as a standalone column in the final SELECT/GROUP BY.
    // Instead, the period bucket should handle it.
    EventQueryParams.Builder params = createRequestParamsBuilder();
    List<org.hisp.dhis.period.PeriodDimension> periods = createPeriodDimensions("2022Sep");
    periods.forEach(period -> period.setDateField(TimeField.EVENT_DATE.name()));
    params.withPeriods(periods, "financialsep");
    params.withEndpointAction(AGGREGATE);

    ListGrid grid = new ListGrid();
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, false));
    grid.addHeader(new GridHeader("ou", "Organisation unit", ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader("eventdate", "Event date", ValueType.TEXT, false, true));

    subject.getEnrollments(params.build(), grid, 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());

    // The period bucket for financialsep should be present
    assertThat(generatedSql, containsString("\"financialsep\""));

    // eventdate should NOT appear as a standalone column in the outer query
    // when EVENT_DATE is the period date field — the period bucket already covers it.
    // The eventdate column in the base CTE is fine, but the outer query must not
    // project it as a separate dimension.
    String outerQuery = generatedSql.substring(generatedSql.indexOf("from enrollment_aggr_base"));
    assertThat(outerQuery, not(containsString("\"eventdate\"")));
  }

  @Test
  void verifyAggregateEnrollmentProjectsMultipleDateFieldPeriodDimensions() {
    EventQueryParams.Builder params = createRequestParamsBuilder();
    List<PeriodDimension> periods = createPeriodDimensions("202001");
    periods.get(0).setDateField(TimeField.SCHEDULED_DATE.name());
    PeriodDimension lastUpdatedPeriod = createPeriodDimensions("202001").get(0);
    lastUpdatedPeriod.setDateField(TimeField.LAST_UPDATED.name());

    params.withPeriods(List.of(periods.get(0), lastUpdatedPeriod), "monthly");
    params.withEndpointAction(AGGREGATE);

    ListGrid grid = new ListGrid();
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, false));
    grid.addHeader(new GridHeader("scheduleddate", "Scheduled date", ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader("lastupdated", "Last updated", ValueType.TEXT, false, true));

    subject.getEnrollments(params.build(), grid, 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());

    // Both bucket expressions in SELECT with correct aliases
    assertThat(generatedSql, containsString("as \"scheduleddate\""));
    assertThat(generatedSql, containsString("as \"lastupdated\""));
    // Both in GROUP BY
    assertThat(generatedSql, containsString("group by"));
  }

  private JdbcEnrollmentAnalyticsManager createEnrollmentAnalyticsManager(
      AnalyticsSqlBuilder builder, String analyticsDatabase) {
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn(analyticsDatabase);

    DefaultProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder =
        new DefaultProgramIndicatorSubqueryBuilder(
            programIndicatorService, systemSettingsService, builder, dataElementService);
    programIndicatorSubqueryBuilder.init();
    ColumnMapper columnMapper = new ColumnMapper(builder, systemSettingsService);
    QueryItemFilterBuilder filterBuilder =
        new QueryItemFilterBuilder(organisationUnitResolver, builder);
    EnrollmentTimeFieldSqlRenderer timeFieldRenderer = new EnrollmentTimeFieldSqlRenderer(builder);
    StageQuerySqlFacade stageQuerySqlFacade =
        new DefaultStageQuerySqlFacade(
            new DefaultStageQueryItemClassifier(),
            new DefaultStageDatePeriodBucketSqlRenderer(builder),
            new DefaultStageOrgUnitSqlService(organisationUnitResolver, builder),
            builder);

    DateFieldPeriodBucketColumnResolver bucketResolver =
        new DateFieldPeriodBucketColumnResolver(builder);
    return new JdbcEnrollmentAnalyticsManager(
        jdbcTemplate,
        programIndicatorService,
        programIndicatorSubqueryBuilder,
        piDisagInfoInitializer,
        piDisagQueryGenerator,
        timeFieldRenderer,
        executionPlanStore,
        systemSettingsService,
        config,
        builder,
        organisationUnitResolver,
        columnMapper,
        filterBuilder,
        stageQuerySqlFacade,
        bucketResolver,
        new EnrollmentEventSubqueryBuilder(builder, new ProgramStageOffsetSqlBuilder(builder)),
        new AggregatedEnrollmentQueryAssembler(
            builder, bucketResolver, new DefaultStageDatePeriodBucketSqlRenderer(builder)));
  }

  @Test
  void verifyAggregateEnrollmentRequestCreatedDateUsesCreatedColumn() {
    EventQueryParams params = createAggregateEnrollmentWithRequestCreatedDateParams();

    subject.getEnrollments(params, new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());

    assertThat(
        generatedSql, containsString("(created >= '2022-09-01' and created < '2023-09-01')"));
    assertThat(generatedSql, not(containsString("enrollmentdate >= '2022-09-01'")));
    assertThat(generatedSql, not(containsString("occurreddate >= '2022-09-01'")));
  }

  @Test
  void verifyAggregateEnrollmentRequestCompletedDateUsesCompletedDateColumn() {
    EventQueryParams params = createAggregateEnrollmentWithRequestCompletedDateParams();

    subject.getEnrollments(params, new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());

    assertThat(
        generatedSql,
        containsString("(completeddate >= '2022-09-01' and completeddate < '2023-09-01')"));
    assertThat(generatedSql, not(containsString("enrollmentdate >= '2022-09-01'")));
    assertThat(generatedSql, not(containsString("occurreddate >= '2022-09-01'")));
  }

  @Test
  void verifyWithProgramAndStartEndDate() {
    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .withStartDate(getDate(2017, 1, 1))
            .withEndDate(getDate(2017, 12, 31))
            .build();

    subject.getEnrollments(params, new ListGrid(), 0);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();
    assertThat(generatedSql, containsString("enrollmentdate >= '2017-01-01'"));
    assertThat(generatedSql, containsString("enrollmentdate < '2018-01-01'"));
    assertThat(generatedSql, containsString("ax.\"uidlevel1\" in ('ouabcdefghA')"));
  }

  @Test
  void verifyWithLastUpdatedTimeField() {
    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .withStartDate(getDate(2017, 1, 1))
            .withEndDate(getDate(2017, 12, 31))
            .withTimeField(TimeField.LAST_UPDATED.name())
            .build();

    subject.getEnrollments(params, new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();
    assertThat(generatedSql, containsString("lastupdated >= '2017-01-01'"));
    assertThat(generatedSql, containsString("lastupdated < '2018-01-01'"));
  }

  @Test
  void verifyWithProgramStageAndNumericDataElement() {
    verifyWithProgramStageAndDataElement(ValueType.NUMBER);
  }

  @Test
  void verifyWithProgramStageAndTextDataElement() {
    verifyWithProgramStageAndDataElement(ValueType.TEXT);
  }

  private void verifyWithProgramStageAndDataElement(ValueType valueType) {
    EventQueryParams params = createRequestParams(this.programStage, valueType);

    subject.getEnrollments(params, new ListGrid(), 100);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());
    String stageUid = programStage.getUid();
    String deUid = dataElementA.getUid();

    // CTE for the stage data element
    assertThat(
        generatedSql,
        containsString(
            noEof(
                """
                %s_%s_0 as ( select enrollment, "%s" as value,
                row_number() over ( partition by enrollment order by occurreddate desc, created desc ) as rn
                from analytics_event_%s where eventstatus != 'SCHEDULE' and ps = '%s' )
                """
                    .formatted(stageUid, deUid, deUid, programAUid, stageUid))));

    // Value projected as stage.de alias
    assertThat(generatedSql, containsString("as \"" + stageUid + "." + deUid + "\""));

    // Stage filter in WHERE
    assertThat(generatedSql, containsString("and ps = '" + stageUid + "'"));
  }

  @Test
  void verifyWithRepeatableProgramStageAndNumericDataElement() {
    verifyWithRepeatableProgramStageAndDataElement(ValueType.NUMBER);
  }

  @Test
  void verifyWithRepeatableProgramStageAndTextDataElement() {
    verifyWithRepeatableProgramStageAndDataElement(ValueType.TEXT);
  }

  private void verifyWithRepeatableProgramStageAndDataElement(ValueType valueType) {
    EventQueryParams params = createRequestParams(repeatableProgramStage, valueType);

    subject.getEnrollments(params, new ListGrid(), 100);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());
    String stageUid = repeatableProgramStage.getUid();
    String deUid = dataElementA.getUid();

    // CTE includes eventstatus for repeatable stage
    assertThat(
        generatedSql,
        containsString(
            noEof(
                """
                %s_%s_0 as ( select enrollment, "%s" as value, eventstatus,
                row_number() over ( partition by enrollment order by occurreddate desc, created desc ) as rn
                from analytics_event_%s where eventstatus != 'SCHEDULE' and ps = '%s' )
                """
                    .formatted(stageUid, deUid, deUid, programAUid, stageUid))));

    // Existence CTE
    assertThat(
        generatedSql,
        containsString(
            noEof(
                """
                select distinct enrollment from analytics_event_%s
                where eventstatus != 'SCHEDULE' and ps = '%s'
                """
                    .formatted(programAUid, stageUid))));

    // SELECT projections for repeatable stage
    assertThat(generatedSql, containsString("\"" + stageUid + "[-1]." + deUid + "\""));
    assertThat(generatedSql, containsString("\"" + stageUid + "[-1]." + deUid + ".exists\""));
    assertThat(generatedSql, containsString("\"" + stageUid + "[-1]." + deUid + ".status\""));
  }

  @Test
  void verifyWithProgramStageAndTextualDataElementAndFilter() {
    verifyWithProgramStageAndDataElementAndFilter(ValueType.TEXT);
  }

  @Test
  void verifyWithProgramStageAndNumericDataElementAndFilter() {
    verifyWithProgramStageAndDataElementAndFilter(ValueType.NUMBER);
  }

  private void verifyWithProgramStageAndDataElementAndFilter(ValueType valueType) {
    EventQueryParams params = createRequestParamsWithFilter(programStage, valueType);

    subject.getEnrollments(params, new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());
    String stageUid = programStage.getUid();
    String deUid = dataElementA.getUid();

    // Filter pushed into CTE
    assertThat(
        generatedSql,
        containsString(
            noEof(
                """
                %s_%s_0 as ( select enrollment, "%s" as value,
                row_number() over ( partition by enrollment order by occurreddate desc, created desc ) as rn
                from analytics_event_%s where eventstatus != 'SCHEDULE' and ps = '%s' and "%s" > '10' )
                """
                    .formatted(stageUid, deUid, deUid, programAUid, stageUid, deUid))));

    // Inner join (not left join) because filter exists
    assertThat(generatedSql, containsString("inner join " + stageUid + "_" + deUid + "_0"));
  }

  @Test
  void verifyWithProgramStageOptionSetDataElementAndFilterProjectsValueFromCte() {
    String optionCode = "OI0BQUurVFS";
    OptionSet optionSet = new OptionSet("Option set A", ValueType.TEXT);
    DataElement optionDataElement =
        org.hisp.dhis.test.TestBase.createDataElement('N', ValueType.TEXT, AggregationType.NONE);
    optionDataElement.setUid("n1rtSHYf6O6");
    optionDataElement.setOptionSet(optionSet);

    QueryItem queryItem =
        new QueryItem(
            optionDataElement,
            programA,
            null,
            ValueType.TEXT,
            optionDataElement.getAggregationType(),
            optionSet);
    queryItem.setProgram(programA);
    queryItem.setProgramStage(programStage);
    queryItem.addFilter(new QueryFilter(EQ, optionCode));

    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.addItem(queryItem);

    subject.getEnrollments(params.build(), new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());
    String stageUid = programStage.getUid();
    String deUid = optionDataElement.getUid();

    assertThat(
        generatedSql,
        containsString(
            noEof(
                """
                %s_%s_0 as ( select enrollment, "%s" as value,
                row_number() over ( partition by enrollment order by occurreddate desc, created desc ) as rn
                from analytics_event_%s where eventstatus != 'SCHEDULE' and ps = '%s' and "%s" = '%s' )
                """
                    .formatted(stageUid, deUid, deUid, programAUid, stageUid, deUid, optionCode))));
    assertThat(generatedSql, containsString(".value as \"" + stageUid + "." + deUid + "\""));
    assertThat(generatedSql, not(containsString("\"" + deUid + "_name\" as value_name")));
    assertThat(
        generatedSql, not(containsString(".value_name as \"" + stageUid + "." + deUid + "\"")));
    assertThat(generatedSql, not(containsString("select \"" + deUid + "_name\"")));
    assertThat(
        generatedSql,
        not(containsString(".enrollment = ax.enrollment and \"" + deUid + "_name\"")));
  }

  @Test
  void verifyWithProgramStageOrgUnitDataElementAndFilterProjectsNameFromCte() {
    String orgUnitUid = "OI0BQUurVFS";
    DataElement orgUnitDataElement =
        org.hisp.dhis.test.TestBase.createDataElement(
            'O', ValueType.ORGANISATION_UNIT, AggregationType.SUM);
    orgUnitDataElement.setUid("n1rtSHYf6O6");

    QueryItem queryItem =
        new QueryItem(
            orgUnitDataElement,
            programA,
            null,
            ValueType.ORGANISATION_UNIT,
            orgUnitDataElement.getAggregationType(),
            null);
    queryItem.setProgram(programA);
    queryItem.setProgramStage(programStage);
    queryItem.addFilter(new QueryFilter(EQ, orgUnitUid));

    when(organisationUnitResolver.resolveOrgUnits(
            any(QueryFilter.class), anyList(), any(QueryItem.class)))
        .thenReturn(orgUnitUid);

    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.addItem(queryItem);

    subject.getEnrollments(params.build(), new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = noEof(sql.getValue());
    String stageUid = programStage.getUid();
    String deUid = orgUnitDataElement.getUid();

    assertThat(
        generatedSql,
        containsString(
            noEof(
                """
                %s_%s_0 as ( select enrollment, "%s" as value, "%s_name" as value_name,
                row_number() over ( partition by enrollment order by occurreddate desc, created desc ) as rn
                from analytics_event_%s where eventstatus != 'SCHEDULE' and ps = '%s' and "%s" = '%s' )
                """
                    .formatted(
                        stageUid, deUid, deUid, deUid, programAUid, stageUid, deUid, orgUnitUid))));
    assertThat(generatedSql, containsString(".value_name as \"" + stageUid + "." + deUid + "\""));
    assertThat(generatedSql, not(containsString("select \"" + deUid + "_name\"")));
    assertThat(
        generatedSql,
        not(containsString(".enrollment = ax.enrollment and \"" + deUid + "_name\"")));
  }

  @Test
  void verifyGetEventsWithProgramStatusParam() {
    mockEmptyRowSet();
    EventQueryParams params = createRequestParamsWithStatusesForEnrollmentQuery();

    subject.getEnrollments(params, new ListGrid(), 10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertThat(sql.getValue(), containsString("enrollmentstatus in ('ACTIVE','COMPLETED')"));
  }

  @Test
  void verifyGetEnrollmentsWithMissingValueEqFilter() {
    String stageUid = programStage.getUid();
    String deUid = dataElementA.getUid();

    // CTE without filter (NV stays in WHERE)
    String cte =
        noEof(
            """
            %s_%s_0 as ( select enrollment, "%s" as value,
            row_number() over ( partition by enrollment order by occurreddate desc, created desc ) as rn
            from analytics_event_%s where eventstatus != 'SCHEDULE' and ps = '%s' )
            """
                .formatted(stageUid, deUid, deUid, programAUid, stageUid));

    testIt(
        EQ,
        NV,
        List.of(
            capturedSql -> assertThat(capturedSql, containsString(cte)),
            capturedSql -> assertThat(capturedSql, containsString("value is NULL"))));
  }

  @Test
  void verifyGetEnrollmentsWithMissingValueNeqFilter() {
    String stageUid = programStage.getUid();
    String deUid = dataElementA.getUid();

    String cte =
        noEof(
            """
            %s_%s_0 as ( select enrollment, "%s" as value,
            row_number() over ( partition by enrollment order by occurreddate desc, created desc ) as rn
            from analytics_event_%s where eventstatus != 'SCHEDULE' and ps = '%s' )
            """
                .formatted(stageUid, deUid, deUid, programAUid, stageUid));

    testIt(
        NEQ,
        NV,
        List.of(
            capturedSql -> assertThat(capturedSql, containsString(cte)),
            capturedSql -> assertThat(capturedSql, containsString("value is not NULL"))));
  }

  @Test
  void verifyWithProgramIndicatorAndRelationshipTypeBothSidesTrackedEntity() {
    ProgramIndicator programIndicatorA = createProgramIndicator('A', programA, "", "");

    RelationshipType relationshipTypeA = createRelationshipType();

    EventQueryParams.Builder params =
        new EventQueryParams.Builder(createRequestParams(programIndicatorA, relationshipTypeA))
            .withStartDate(getDate(2015, 1, 1))
            .withEndDate(getDate(2017, 4, 8));

    when(programIndicatorService.getAnalyticsSql(
            "", NUMERIC, programIndicatorA, getDate(2000, 1, 1), getDate(2017, 4, 8), "subax"))
        .thenReturn("distinct event");

    subject.getEnrollments(params.build(), new ListGrid(), 100);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();
    assertThat(generatedSql, containsString("value as " + programIndicatorA.getUid()));
    assertThat(generatedSql, containsString("enrollmentdate >= '2015-01-01'"));
    assertThat(generatedSql, containsString("enrollmentdate < '2017-04-09'"));
  }

  @Test
  void verifyWithProgramIndicatorAndRelationshipTypeDifferentConstraint() {
    ProgramIndicator programIndicatorA = createProgramIndicator('A', programA, "", "");

    RelationshipType relationshipTypeA =
        createRelationshipType(RelationshipEntity.PROGRAM_INSTANCE);

    EventQueryParams.Builder params =
        new EventQueryParams.Builder(createRequestParams(programIndicatorA, relationshipTypeA))
            .withStartDate(getDate(2015, 1, 1))
            .withEndDate(getDate(2017, 4, 8));

    when(programIndicatorService.getAnalyticsSql(
            "", NUMERIC, programIndicatorA, getDate(2000, 1, 1), getDate(2017, 4, 8), "subax"))
        .thenReturn("distinct event");

    subject.getEnrollments(params.build(), new ListGrid(), 100);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();
    assertThat(generatedSql, containsString("value as " + programIndicatorA.getUid()));
    assertThat(generatedSql, containsString("enrollmentdate >= '2015-01-01'"));
  }

  @Test
  void verifyWithProgramIndicatorAndRelationshipTypeBothSidesTrackedEntity2() {
    Program programB = createProgram('B');
    ProgramIndicator programIndicatorA = createProgramIndicator('A', programB, "", "");

    RelationshipType relationshipTypeA = createRelationshipType();

    EventQueryParams.Builder params =
        new EventQueryParams.Builder(createRequestParams(programIndicatorA, relationshipTypeA))
            .withStartDate(getDate(2015, 1, 1))
            .withEndDate(getDate(2017, 4, 8));

    when(programIndicatorService.getAnalyticsSql(
            "", NUMERIC, programIndicatorA, getDate(2000, 1, 1), getDate(2017, 4, 8), "subax"))
        .thenReturn("distinct event");

    subject.getEnrollments(params.build(), new ListGrid(), 100);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();
    assertThat(generatedSql, containsString("value as " + programIndicatorA.getUid()));
    assertThat(generatedSql, containsString("enrollmentdate >= '2015-01-01'"));
  }

  private RelationshipType createRelationshipType(RelationshipEntity toConstraint) {
    RelationshipType relationshipTypeA = rnd.nextObject(RelationshipType.class);

    RelationshipConstraint from = new RelationshipConstraint();
    from.setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);

    RelationshipConstraint to = new RelationshipConstraint();
    to.setRelationshipEntity(toConstraint);

    relationshipTypeA.setFromConstraint(from);
    relationshipTypeA.setToConstraint(to);
    return relationshipTypeA;
  }

  private RelationshipType createRelationshipType() {
    return createRelationshipType(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
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

  private QueryItem createFilteredStageDateItem(ProgramStage stage) {
    QueryItem dateItem =
        new QueryItem(
            new BaseDimensionalItemObject(OCCURRED_DATE_COLUMN_NAME),
            programA,
            null,
            ValueType.DATE,
            null,
            null);
    dateItem.setProgram(programA);
    dateItem.setProgramStage(stage);
    dateItem.addFilter(new QueryFilter(QueryOperator.GE, "2026-01-01"));
    dateItem.addFilter(new QueryFilter(QueryOperator.LE, "2026-12-31"));
    return dateItem;
  }

  private QueryItem createRepeatableOffsetDataElementItem(String stageUid, int offset) {
    QueryItem item = new QueryItem(new BaseDimensionalItemObject(dataElementA.getUid()));
    item.setProgram(programA);
    item.setProgramStage(repeatableProgramStage);
    item.setValueType(ValueType.NUMBER);
    item.setRepeatableStageParams(
        RepeatableStageParams.of(offset, stageUid + "[" + offset + "]." + dataElementA.getUid()));
    return item;
  }

  private EventQueryParams createAggregateEnrollmentWithEventDateParams() {
    BaseDimensionalItemObject dateItem = new BaseDimensionalItemObject(OCCURRED_DATE_COLUMN_NAME);
    QueryItem queryItem = new QueryItem(dateItem, programA, null, ValueType.DATE, null, null);
    queryItem.setProgram(programA);
    queryItem.addFilter(new QueryFilter(QueryOperator.GE, "2022-09-01"));
    queryItem.addFilter(new QueryFilter(QueryOperator.LT, "2023-09-01"));

    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.addItem(queryItem);
    params.withEndpointAction(AGGREGATE);
    return params.build();
  }

  private EventQueryParams createAggregateEnrollmentWithRequestEventDateParams() {
    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.withPeriods(
        java.util.List.of(PeriodDimension.of("2022Sep").setDateField("EVENT_DATE")), "yearly");
    params.withEndpointAction(AGGREGATE);
    return new EventQueryParams.Builder(params.build()).withStartEndDatesForPeriods().build();
  }

  private EventQueryParams createAggregateEnrollmentWithIncidentDateHeaderParams() {
    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.withEndpointAction(AGGREGATE);
    params.withTimeField(TimeField.INCIDENT_DATE.name());
    return params.build();
  }

  private EventQueryParams createAggregateEnrollmentWithRequestCreatedDateParams() {
    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.withPeriods(
        java.util.List.of(PeriodDimension.of("2022Sep").setDateField("CREATED")), "yearly");
    params.withEndpointAction(AGGREGATE);
    return new EventQueryParams.Builder(params.build()).withStartEndDatesForPeriods().build();
  }

  private EventQueryParams createAggregateEnrollmentWithRequestCompletedDateParams() {
    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.withPeriods(
        java.util.List.of(PeriodDimension.of("2022Sep").setDateField("COMPLETED")), "yearly");
    params.withEndpointAction(AGGREGATE);
    return new EventQueryParams.Builder(params.build()).withStartEndDatesForPeriods().build();
  }

  private String eventDateJoinProjection() {
    return noEof("evf.event_occurreddate as \"eventdate\"");
  }

  private EventQueryParams createStageOuRequestParams() {
    return createStageOuRequestParamsWithHeaders(Set.of());
  }

  private EventQueryParams createStageOuRequestParamsWithHeaders(Set<String> headers) {
    // Create a stage.ou dimension query item
    BaseDimensionalItemObject ouItem = new BaseDimensionalItemObject(OU_COLUMN_NAME);
    QueryItem queryItem = new QueryItem(ouItem);
    queryItem.setItem(ouItem);
    queryItem.setProgramStage(programStage);
    queryItem.setProgram(programA);
    queryItem.setValueType(ValueType.ORGANISATION_UNIT);

    EventQueryParams.Builder params = createRequestParamsBuilder();
    params.addItem(queryItem);
    params.withHeaders(headers);
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

  private int countOccurrences(String value, String search) {
    int count = 0;
    int index = 0;
    while ((index = value.indexOf(search, index)) >= 0) {
      count++;
      index += search.length();
    }
    return count;
  }

  // -------------------------------------------------------------------------
  // Program indicator placeholder resolution in aggregate queries
  // (classic /api/analytics path: getAggregatedEventData / getEnrollmentCount)
  // -------------------------------------------------------------------------

  @Test
  void verifyAggregatedEventDataResolvesPsdePlaceholderInPiFilter() {
    ProgramIndicator pi =
        createEnrollmentProgramIndicator(
            "V{enrollment_count}",
            "A{w75KJ2mc4zz} == 'PIPPO' && #{%s.%s} == 'hello'"
                .formatted(programStage.getUid(), dataElementA.getUid()));
    stubPiExpressionSql(pi, "enrollment");
    stubPiFilterSql(pi, "\"w75KJ2mc4zz\" = 'PIPPO' and " + psdePlaceholder(pi) + " = 'hello'");

    subject.getAggregatedEventData(
        createAggregatePiParams(pi, AggregationType.COUNT), new ListGrid(), 200000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();
    assertNoCtePlaceholders(generatedSql);
    assertThat(generatedSql, startsWith("with "));
    assertThat(generatedSql, containsString(pi.getUid() + " as ("));
    assertThat(generatedSql, containsString("inner join " + pi.getUid()));
    assertThat(generatedSql, containsString(".enrollment = ax.enrollment"));
  }

  @Test
  void verifyEnrollmentCountResolvesPsdePlaceholderInPiFilter() {
    ProgramIndicator pi =
        createEnrollmentProgramIndicator(
            "V{enrollment_count}",
            "#{%s.%s} == 'hello'".formatted(programStage.getUid(), dataElementA.getUid()));
    stubPiExpressionSql(pi, "enrollment");
    stubPiFilterSql(pi, psdePlaceholder(pi) + " = 'hello'");

    subject.getEnrollmentCount(createAggregatePiParams(pi, AggregationType.COUNT));
    verify(jdbcTemplate).queryForObject(sql.capture(), eq(Long.class));

    String generatedSql = sql.getValue();
    assertNoCtePlaceholders(generatedSql);
    assertThat(generatedSql, startsWith("with "));
    assertThat(generatedSql, containsString("inner join " + pi.getUid()));
    assertThat(generatedSql, containsString(".enrollment = ax.enrollment"));
  }

  @Test
  void verifyAggregatedEventDataResolvesPsdePlaceholderInPiExpression() {
    ProgramIndicator pi =
        createEnrollmentProgramIndicator(
            "#{%s.%s}".formatted(programStage.getUid(), dataElementA.getUid()), null);
    stubPiExpressionSql(pi, psdePlaceholder(pi));

    subject.getAggregatedEventData(
        createAggregatePiParams(pi, AggregationType.SUM), new ListGrid(), 200000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();
    assertNoCtePlaceholders(generatedSql);
    assertThat(generatedSql, startsWith("with "));
    assertThat(generatedSql, containsString("inner join " + pi.getUid()));
    assertThat(generatedSql, containsString("sum("));
  }

  @Test
  void verifyAggregatedEventDataResolvesVariablePlaceholderInPiFilter() {
    // d2:daysBetween keeps the V{...} reference out of the simple-filter analyzer, so the
    // variable placeholder must be resolved by the complex-filter processing chain
    ProgramIndicator pi =
        createEnrollmentProgramIndicator(
            "V{enrollment_count}", "d2:daysBetween(V{enrollment_date}, V{event_date}) > 7");
    stubPiExpressionSql(pi, "enrollment");
    stubPiFilterSql(
        pi, "(cast(" + variablePlaceholder(pi) + " as date) - cast(enrollmentdate as date)) > 7");

    subject.getAggregatedEventData(
        createAggregatePiParams(pi, AggregationType.COUNT), new ListGrid(), 200000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();
    assertNoCtePlaceholders(generatedSql);
    assertThat(generatedSql, startsWith("with "));
    assertThat(generatedSql, containsString("inner join " + pi.getUid()));
  }

  @Test
  void verifyAggregatedEventDataResolvesD2FunctionPlaceholderInPiFilter() {
    ProgramIndicator pi =
        createEnrollmentProgramIndicator(
            "V{enrollment_count}",
            "d2:countIfValue(#{%s.%s}, 10) > 0"
                .formatted(programStage.getUid(), dataElementA.getUid()));
    stubPiExpressionSql(pi, "enrollment");
    stubPiFilterSql(pi, d2FunctionPlaceholder(pi) + " > 0");

    subject.getAggregatedEventData(
        createAggregatePiParams(pi, AggregationType.COUNT), new ListGrid(), 200000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();
    assertNoCtePlaceholders(generatedSql);
    assertThat(generatedSql, startsWith("with "));
    assertThat(generatedSql, containsString("inner join " + pi.getUid()));
  }

  @Test
  void verifyAggregatedEventDataResolvesMixedPlaceholdersInPiFilter() {
    ProgramIndicator pi =
        createEnrollmentProgramIndicator(
            "V{enrollment_count}",
            "A{w75KJ2mc4zz} == 'PIPPO' && #{%s.%s} == 'hello' && (V{event_date} > '2021-01-01' || A{w75KJ2mc4zz} == 'x')"
                .formatted(programStage.getUid(), dataElementA.getUid()));
    stubPiExpressionSql(pi, "enrollment");
    stubPiFilterSql(
        pi,
        "\"w75KJ2mc4zz\" = 'PIPPO' and "
            + psdePlaceholder(pi)
            + " = 'hello' and ("
            + variablePlaceholder(pi)
            + " > '2021-01-01' or \"w75KJ2mc4zz\" = 'x')");

    subject.getAggregatedEventData(
        createAggregatePiParams(pi, AggregationType.COUNT), new ListGrid(), 200000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();
    assertNoCtePlaceholders(generatedSql);
    assertThat(generatedSql, startsWith("with "));
    assertThat(generatedSql, containsString("inner join " + pi.getUid()));
  }

  @Test
  void verifyAggregatedEventDataWithoutProgramIndicatorHasNoCtes() {
    subject.getAggregatedEventData(
        createRequestParams(programStage, ValueType.INTEGER), new ListGrid(), 200000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();
    assertThat(generatedSql, not(startsWith("with ")));
    assertThat(generatedSql, not(containsString("inner join")));
  }

  private ProgramIndicator createEnrollmentProgramIndicator(String expression, String filter) {
    return createProgramIndicator('X', AnalyticsType.ENROLLMENT, programA, expression, filter);
  }

  private EventQueryParams createAggregatePiParams(
      ProgramIndicator pi, AggregationType aggregationType) {
    return new EventQueryParams.Builder(createRequestParams())
        .withProgramIndicator(pi)
        .withAggregationType(AnalyticsAggregationType.fromAggregationType(aggregationType))
        .build();
  }

  private String psdePlaceholder(ProgramIndicator pi) {
    return "__PSDE_CTE_PLACEHOLDER__(psUid='%s', deUid='%s', offset='0', boundaryHash='noboundaries', piUid='%s')"
        .formatted(programStage.getUid(), dataElementA.getUid(), pi.getUid());
  }

  private String variablePlaceholder(ProgramIndicator pi) {
    return "FUNC_CTE_VAR( type='vEventDate', column='occurreddate', piUid='%s', psUid='null', offset='0')"
        .formatted(pi.getUid());
  }

  private String d2FunctionPlaceholder(ProgramIndicator pi) {
    return "__D2FUNC__(func='countIfValue', ps='%s', de='%s', argType='val64', arg64='MTA=', hash='noboundaries', pi='%s')__"
        .formatted(programStage.getUid(), dataElementA.getUid(), pi.getUid());
  }

  private void stubPiExpressionSql(ProgramIndicator pi, String rawExpressionSql) {
    when(programIndicatorService.getAnalyticsSql(
            eq(pi.getExpression()), eq(NUMERIC), eq(pi), any(), any()))
        .thenReturn(rawExpressionSql);
    when(programIndicatorService.getAnalyticsSql(
            eq(pi.getExpression()), eq(NUMERIC), eq(pi), any(), any(), anyString()))
        .thenReturn(rawExpressionSql);
  }

  private void stubPiFilterSql(ProgramIndicator pi, String rawFilterSql) {
    // The filter analyzer may strip simple V{...} comparisons from the filter text before
    // asking the service for SQL, so match any expression string for this PI.
    when(programIndicatorService.getAnalyticsSqlAllowingNulls(
            anyString(), eq(BOOLEAN), eq(pi), any(), any()))
        .thenReturn(rawFilterSql);
    when(programIndicatorService.getAnalyticsSqlAllowingNulls(
            anyString(), eq(BOOLEAN), eq(pi), any(), any(), anyString()))
        .thenReturn(rawFilterSql);
  }

  private void assertNoCtePlaceholders(String generatedSql) {
    assertThat(generatedSql, not(containsString("__PSDE_CTE_PLACEHOLDER__")));
    assertThat(generatedSql, not(containsString("FUNC_CTE_VAR(")));
    assertThat(generatedSql, not(containsString("__D2FUNC__")));
    assertJoinTargetsAreDefinedCtes(generatedSql);
  }

  /**
   * Every inner/left join target that is not an analytics table must be defined as a CTE in the
   * with clause, otherwise the query fails at runtime with "relation does not exist".
   */
  private void assertJoinTargetsAreDefinedCtes(String generatedSql) {
    java.util.Set<String> definedCtes =
        java.util.regex.Pattern.compile("([A-Za-z0-9_]+)\\s+as\\s*\\(")
            .matcher(generatedSql)
            .results()
            .map(r -> r.group(1))
            .collect(java.util.stream.Collectors.toSet());
    java.util.regex.Matcher joins =
        java.util.regex.Pattern.compile("(?:inner|left)\\s+join\\s+([A-Za-z0-9_]+)")
            .matcher(generatedSql);
    while (joins.find()) {
      String target = joins.group(1);
      if (target.startsWith("analytics_")) {
        continue;
      }
      assertThat(
          "join target '" + target + "' must be defined as a CTE in: " + generatedSql,
          definedCtes.contains(target),
          is(true));
    }
  }
}
