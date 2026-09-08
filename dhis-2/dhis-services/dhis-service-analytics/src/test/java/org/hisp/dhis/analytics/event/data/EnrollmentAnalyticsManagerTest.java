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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_DATABASE;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import org.hisp.dhis.analytics.AggregationType;
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
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.external.conf.DefaultDhisConfigurationProvider;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
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
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EnrollmentAnalyticsManagerTest extends EventAnalyticsTest {
  private JdbcEnrollmentAnalyticsManager subject;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private ExecutionPlanStore executionPlanStore;

  @Mock private SqlRowSet rowSet;

  @Mock private SqlRowSetMetaData rowSetMetaData;

  @Mock private ProgramIndicatorService programIndicatorService;

  @Spy private PostgreSqlAnalyticsSqlBuilder sqlBuilder = new PostgreSqlAnalyticsSqlBuilder();

  @Mock private SystemSettingsService systemSettingsService;

  @Mock private DataElementService dataElementService;

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
  void setUp() {
    when(jdbcTemplate.queryForRowSet(anyString())).thenReturn(this.rowSet);
    when(systemSettingsService.getCurrentSettings()).thenReturn(systemSettings);
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn("postgresql");
    DefaultProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder =
        new DefaultProgramIndicatorSubqueryBuilder(
            programIndicatorService,
            systemSettingsService,
            new PostgreSqlBuilder(),
            dataElementService);
    programIndicatorSubqueryBuilder.init();
    when(rowSet.getMetaData()).thenReturn(rowSetMetaData);
    when(systemSettings.getOrgUnitCentroidsInEventsAnalytics()).thenReturn(false);
    ColumnMapper columnMapper = new ColumnMapper(sqlBuilder, systemSettingsService);
    filterBuilder = new QueryItemFilterBuilder(organisationUnitResolver, sqlBuilder);
    StageQuerySqlFacade stageQuerySqlFacade =
        new DefaultStageQuerySqlFacade(
            new DefaultStageQueryItemClassifier(),
            new DefaultStageDatePeriodBucketSqlRenderer(sqlBuilder),
            new DefaultStageOrgUnitSqlService(organisationUnitResolver, sqlBuilder),
            sqlBuilder);

    DateFieldPeriodBucketColumnResolver bucketResolver =
        new DateFieldPeriodBucketColumnResolver(new PostgreSqlAnalyticsSqlBuilder());

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
            filterBuilder,
            stageQuerySqlFacade,
            bucketResolver,
            new EnrollmentEventSubqueryBuilder(
                sqlBuilder, new ProgramStageOffsetSqlBuilder(sqlBuilder)),
            new AggregatedEnrollmentQueryAssembler(
                sqlBuilder,
                bucketResolver,
                new DefaultStageDatePeriodBucketSqlRenderer(sqlBuilder)));
  }

  @Test
  void verifySortsByCreatedDescending() {
    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .addDescSortItem(new QueryItem(new BaseDimensionalItemObject("created")))
            .build();

    subject.getEnrollments(params, new ListGrid(), 10000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertThat(sql.getValue(), containsString("order by \"created\" desc nulls last"));
  }

  @Test
  void testBadGrammarExceptionNonMultipleQueries() {
    // Given
    mockEmptyRowSet();
    EventQueryParams params = createRequestParamsWithStatuses();
    when(jdbcTemplate.queryForRowSet(anyString())).thenThrow(BadSqlGrammarException.class);

    // Then
    assertThrows(
        BadSqlGrammarException.class, () -> subject.getEnrollments(params, new ListGrid(), 10000));
  }

  @Test
  void testBadGrammarExceptionWithMultipleQueries() {
    // Given
    mockEmptyRowSet();
    EventQueryParams params = createRequestParamsWithMultipleQueries();
    SQLException sqlException = new SQLException("Some exception", "HY000");
    BadSqlGrammarException badSqlGrammarException =
        new BadSqlGrammarException("task", "select * from nothing", sqlException);
    when(jdbcTemplate.queryForRowSet(anyString())).thenThrow(badSqlGrammarException);

    // Then
    assertDoesNotThrow(() -> subject.getEnrollments(params, new ListGrid(), 10000));
  }

  @Override
  String getTableName() {
    return "analytics_enrollment";
  }

  @Test
  void verifyGetColumnOfTypeCoordinateAndNoProgramStages() {
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());

    QueryItem item = new QueryItem(dio);
    item.setValueType(ValueType.COORDINATE);

    String columnSql = subject.getColumn(item);

    assertThat(columnSql, is("ax.\"" + dataElementA.getUid() + "\""));
  }

  @Test
  void verifyGetColumnOfTypeCoordinateAndWithProgramStages() {
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());

    QueryItem item = new QueryItem(dio);
    item.setValueType(ValueType.COORDINATE);
    item.setProgramStage(programStage);
    item.setProgram(programA);

    String columnSql = subject.getColumn(item);
    String programAUid = programA.getUid().toLowerCase();
    assertThat(
        columnSql,
        is(
            "(select \""
                + dataElementA.getUid()
                + "\" from analytics_event_"
                + programAUid
                + " where analytics_event_"
                + programAUid
                + ".eventstatus != 'SCHEDULE' and analytics_event_"
                + programAUid
                + ".enrollment = ax.enrollment and \""
                + dataElementA.getUid()
                + "\" is not null and ps = '"
                + programStage.getUid()
                + "' order by occurreddate desc, created desc  limit 1 )"));
  }

  @Test
  void verifyEventProgramIndicatorOnEnrollmentQueryUsesEnrollmentKeyedCteJoin() {
    mockEmptyRowSet();
    ProgramIndicator programIndicator = new ProgramIndicator();
    programIndicator.setUid("CH6wamtY9kK");
    programIndicator.setProgram(programA);
    programIndicator.setAnalyticsType(AnalyticsType.EVENT);
    programIndicator.setAggregationType(AggregationType.COUNT);
    programIndicator.setExpression("case when eventstatus in ('ACTIVE', 'COMPLETED') then 1 end");

    when(programIndicatorService.getAnalyticsSqlDeferRelationshipCount(
            eq(programIndicator.getExpression()),
            eq(NUMERIC),
            eq(programIndicator),
            any(),
            any(),
            eq("subax")))
        .thenReturn("case when eventstatus in ('ACTIVE', 'COMPLETED') then 1 end");

    subject.getEnrollments(createRequestParams(programIndicator, null), new ListGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    assertThat(generatedSql, containsString("with ch6wamty9kk as"));
    assertThat(
        generatedSql,
        containsString(
            "select subax.enrollment as enrollment, count(case when eventstatus in ('active', 'completed') then 1 end) as value"));
    assertThat(generatedSql, containsString("group by subax.enrollment"));
    assertThat(generatedSql, containsString(" left join ch6wamty9kk "));
    assertThat(generatedSql, containsString(".enrollment = ax.enrollment"));
    assertThat(generatedSql, not(containsString(".event = ax.event")));
  }

  @Test
  void verifyGetColumnOfTypeCoordinateAndWithProgramStagesAndParamsWithNumberTypeValue() {
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());

    QueryItem item = new QueryItem(dio);
    item.setValueType(ValueType.COORDINATE);
    item.setProgramStage(repeatableProgramStage);
    item.setProgram(programB);
    RepeatableStageParams params = new RepeatableStageParams();

    params.setIndex(0);
    item.setRepeatableStageParams(params);

    String columnSql = subject.getColumn(item);
    String programBUid = programB.getUid().toLowerCase();
    assertThat(
        columnSql,
        is(
            "(select \""
                + dataElementA.getUid()
                + "\" from analytics_event_"
                + programBUid
                + " where analytics_event_"
                + programBUid
                + ".eventstatus != 'SCHEDULE' and analytics_event_"
                + programBUid
                + ".enrollment = ax.enrollment and ps = '"
                + repeatableProgramStage.getUid()
                + "' order by occurreddate desc, created desc  limit 1 )"));
  }

  @Test
  void verifyGetCoordinateColumnAndNoProgramStage() {
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());

    QueryItem item = new QueryItem(dio);
    item.setValueType(ValueType.COORDINATE);
    item.setProgram(programA);

    String columnSql = subject.getCoordinateColumn(item).asSql();

    String colName = quote(item.getItemName());
    String eventTableName = "analytics_event_" + item.getProgram().getUid().toLowerCase();

    assertThat(
        columnSql,
        is(
            "(select "
                + "'[' || round(ST_X(("
                + colName
                + "))::numeric, 6) || ',' || round(ST_Y(("
                + colName
                + "))::numeric, 6) || ']' as "
                + colName
                + " from "
                + eventTableName
                + " where "
                + eventTableName
                + ".enrollment = "
                + ANALYTICS_TBL_ALIAS
                + ".enrollment "
                + "and "
                + colName
                + " is not null  "
                + "order by occurreddate desc, created "
                + "desc  limit 1 )"));
  }

  @Test
  void verifyGetCoordinateColumnWithProgramStage() {
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());

    QueryItem item = new QueryItem(dio);
    item.setValueType(ValueType.COORDINATE);
    item.setProgramStage(programStage);
    item.setProgram(programA);

    String columnSql = subject.getCoordinateColumn(item).asSql();

    String colName = quote(item.getItemName());
    String eventTableName = "analytics_event_" + item.getProgram().getUid().toLowerCase();

    assertThat(
        columnSql,
        is(
            "(select "
                + "'[' || round(ST_X(("
                + colName
                + "))::numeric, 6) || ',' || round(ST_Y(("
                + colName
                + "))::numeric, 6) || ']' as "
                + colName
                + " from "
                + eventTableName
                + " where "
                + eventTableName
                + ".enrollment = "
                + ANALYTICS_TBL_ALIAS
                + ".enrollment "
                + "and "
                + colName
                + " is not null "
                + "and ps = '"
                + item.getProgramStage().getUid()
                + "'  order by occurreddate desc, created "
                + "desc  limit 1 )"));
  }

  @Test
  void verifyGetCoordinateColumnWithNoProgram() {
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());

    QueryItem item = new QueryItem(dio);
    item.setValueType(ValueType.COORDINATE);
    item.setProgramStage(programStage);

    String columnSql = subject.getCoordinateColumn(item).asSql();

    assertThat(columnSql, is(EMPTY));
  }
}
