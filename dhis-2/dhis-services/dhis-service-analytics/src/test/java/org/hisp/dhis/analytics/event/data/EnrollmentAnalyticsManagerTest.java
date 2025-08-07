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
import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;
import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.common.QueryOperator.NEQ;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_DATABASE;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramIndicator;
import static org.hisp.dhis.test.TestBase.getDate;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.programindicator.DefaultProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagInfoInitializer;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.external.conf.DefaultDhisConfigurationProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
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

  @Spy
  private EnrollmentTimeFieldSqlRenderer enrollmentTimeFieldSqlRenderer =
      new EnrollmentTimeFieldSqlRenderer(sqlBuilder);

  @Spy private SystemSettings systemSettings;

  @Mock private DefaultDhisConfigurationProvider config;

  @Captor private ArgumentCaptor<String> sql;

  private static final String DEFAULT_COLUMNS =
      """
      enrollment,trackedentity,enrollmentdate,occurreddate,storedby,createdbydisplayname,\
      lastupdatedbydisplayname,lastupdated,ST_AsGeoJSON(enrollmentgeometry),\
      longitude,latitude,ouname,ounamehierarchy,oucode,enrollmentstatus""";

  private final BeanRandomizer rnd = BeanRandomizer.create();

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
    when(rowSet.getMetaData()).thenReturn(rowSetMetaData);
    when(systemSettings.getOrgUnitCentroidsInEventsAnalytics()).thenReturn(false);
    ColumnMapper columnMapper = new ColumnMapper(sqlBuilder, systemSettingsService);

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
            columnMapper);
  }

  @Test
  void verifyWithProgramAndStartEndDate() {
    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .withStartDate(getDate(2017, 1, 1))
            .withEndDate(getDate(2017, 12, 31))
            .build();

    Grid grid = new ListGrid();
    int unlimited = 0;

    subject.getEnrollments(params, grid, unlimited);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\",ax.\"ou\"  from "
            + getTable(programA.getUid())
            + " as ax where (((enrollmentdate >= '2017-01-01' and enrollmentdate < '2018-01-01'))) and (ax.\"uidlevel1\" = 'ouabcdefghA' ) ";

    assertSql(sql.getValue(), expected);
    assertTrue(grid.hasLastDataRow());
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

    String expected =
        "ax.\"quarterly\",ax.\"ou\"  from "
            + getTable(programA.getUid())
            + " as ax where (((lastupdated >= '2017-01-01' and lastupdated < '2018-01-01'))) and (ax.\"uidlevel1\" = 'ouabcdefghA' ) limit 10001";

    assertSql(sql.getValue(), expected);
  }

  @Test
  void verifyWithRepeatableProgramStageAndNumericDataElement() {
    verifyWithRepeatableProgramStageAndDataElement(ValueType.NUMBER);
  }

  @Test
  void verifyWithRepeatableProgramStageAndTextDataElement() {
    verifyWithRepeatableProgramStageAndDataElement(ValueType.TEXT);
  }

  @Test
  void verifyWithProgramStageAndTextDataElement() {
    verifyWithProgramStageAndDataElement(ValueType.TEXT);
  }

  @Test
  void verifyWithProgramStageAndNumericDataElement() {
    verifyWithProgramStageAndDataElement(ValueType.NUMBER);
  }

  private void verifyWithProgramStageAndDataElement(ValueType valueType) {
    EventQueryParams params = createRequestParams(this.programStage, valueType);

    subject.getEnrollments(params, new ListGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String subSelect =
        "(select \"fWIAEtYVEGk\" from analytics_event_"
            + programA.getUid()
            + " where analytics_event_"
            + programA.getUid()
            + ".eventstatus != 'SCHEDULE' and analytics_event_"
            + programA.getUid()
            + ".enrollment = ax.enrollment and \"fWIAEtYVEGk\" is not null and ps = '"
            + programStage.getUid()
            + "' order by occurreddate desc, created desc  limit 1 )";

    if (valueType == ValueType.NUMBER) {
      subSelect = subSelect + " as \"fWIAEtYVEGk\"";
    }
    String expected =
        "ax.\"quarterly\",ax.\"ou\","
            + subSelect
            + "  from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and (ax.\"uidlevel1\" = 'ouabcdefghA' ) "
            + "and ps = '"
            + programStage.getUid()
            + "' limit 101";

    assertSql(sql.getValue(), expected);
  }

  private void verifyWithRepeatableProgramStageAndDataElement(ValueType valueType) {
    EventQueryParams params = createRequestParams(repeatableProgramStage, valueType);

    subject.getEnrollments(params, new ListGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String programUid = repeatableProgramStage.getProgram().getUid();

    String programStageUid = repeatableProgramStage.getUid();

    String dataElementUid = dataElementA.getUid();

    String expected =
        "select enrollment,trackedentity,enrollmentdate,occurreddate,storedby,createdbydisplayname,lastupdatedbydisplayname,lastupdated,ST_AsGeoJSON(enrollmentgeometry),longitude,latitude,"
            + "ouname,ounamehierarchy,oucode,enrollmentstatus,ax.\"quarterly\",ax.\"ou\","
            + "(select \""
            + dataElementUid
            + "\" from analytics_event_"
            + programUid
            + " where analytics_event_"
            + programUid
            + ".eventstatus != 'SCHEDULE' and analytics_event_"
            + programUid
            + ".enrollment = ax.enrollment and ps = '"
            + repeatableProgramStage.getUid()
            + "' order by occurreddate desc, created desc offset 1 limit 1 ) "
            + "as \""
            + programStageUid
            + "[-1]."
            + dataElementUid
            + "\", exists ((select \""
            + dataElementUid
            + "\" "
            + "from analytics_event_"
            + programUid
            + " where analytics_event_"
            + programUid
            + ".eventstatus != 'SCHEDULE' and analytics_event_"
            + programUid
            + ".enrollment = ax.enrollment and ps = '"
            + programStageUid
            + "' order by occurreddate desc, created desc offset 1 limit 1 )) "
            + "as \""
            + programStageUid
            + "[-1]."
            + dataElementUid
            + ".exists\""
            + ",(select eventstatus "
            + "from analytics_event_"
            + programUid
            + " where analytics_event_"
            + programUid
            + ".eventstatus != 'SCHEDULE' and analytics_event_"
            + programUid
            + ".enrollment = ax.enrollment and ps = '"
            + programStageUid
            + "' order by occurreddate desc, created desc offset 1 limit 1 ) "
            + "as \""
            + programStageUid
            + "[-1]."
            + dataElementUid
            + ".status\"  "
            + "from analytics_enrollment_"
            + programUid
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and (ax.\"uidlevel1\" = 'ouabcdefghA' ) "
            + "and ps = '"
            + programStageUid
            + "' limit 101";

    assertEquals(expected, sql.getValue());
  }

  @Test
  void verifyWithProgramStageAndTextualDataElementAndFilter() {
    EventQueryParams params = createRequestParamsWithFilter(programStage, ValueType.TEXT);

    subject.getEnrollments(params, new ListGrid(), 10000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String subSelect =
        "(select \"fWIAEtYVEGk\" from analytics_event_"
            + programA.getUid()
            + " where analytics_event_"
            + programA.getUid()
            + ".eventstatus != 'SCHEDULE' and analytics_event_"
            + programA.getUid()
            + ".enrollment = ax.enrollment and \"fWIAEtYVEGk\" is not null and ps = '"
            + programStage.getUid()
            + "' order by occurreddate desc, created desc  limit 1 )";

    String expected =
        "ax.\"quarterly\",ax.\"ou\","
            + subSelect
            + "  from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and (ax.\"uidlevel1\" = 'ouabcdefghA' ) "
            + "and ps = '"
            + programStage.getUid()
            + "' and "
            + subSelect
            + " > '10' limit 10001";

    assertSql(sql.getValue(), expected);
  }

  @Test
  void verifyGetEventsWithProgramStatusParam() {
    mockEmptyRowSet();

    EventQueryParams params = createRequestParamsWithStatuses();

    subject.getEnrollments(params, new ListGrid(), 10000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\",ax.\"ou\"  from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and (ax.\"uidlevel1\" = 'ouabcdefghA' )"
            + " and enrollmentstatus in ('ACTIVE','COMPLETED') limit 10001";

    assertSql(sql.getValue(), expected);
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

  @Test
  void verifyWithProgramStageAndNumericDataElementAndFilter2() {
    EventQueryParams params = createRequestParamsWithFilter(programStage, ValueType.NUMBER);

    subject.getEnrollments(params, new ListGrid(), 10000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String subSelect =
        "(select \"fWIAEtYVEGk\" from analytics_event_"
            + programA.getUid()
            + " where analytics_event_"
            + programA.getUid()
            + ".eventstatus != 'SCHEDULE' and analytics_event_"
            + programA.getUid()
            + ".enrollment = ax.enrollment and \"fWIAEtYVEGk\" is not null and ps = '"
            + programStage.getUid()
            + "' order by occurreddate desc, created desc  limit 1 )";

    String expected =
        "ax.\"quarterly\",ax.\"ou\","
            + subSelect
            + " as \"fWIAEtYVEGk\""
            + "  from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and (ax.\"uidlevel1\" = 'ouabcdefghA' ) "
            + "and ps = '"
            + programStage.getUid()
            + "' and "
            + subSelect
            + " > '10' limit 10001";

    assertSql(sql.getValue(), expected);
  }

  @Test
  void verifyGetEnrollmentsWithMissingValueEqFilter() {
    String subSelect =
        "(select \"fWIAEtYVEGk\" from analytics_event_"
            + programA.getUid()
            + " where analytics_event_"
            + programA.getUid()
            + ".eventstatus != 'SCHEDULE' and analytics_event_"
            + programA.getUid()
            + ".enrollment = ax.enrollment and \"fWIAEtYVEGk\" is not null and ps = '"
            + programStage.getUid()
            + "' order by occurreddate desc, created desc  limit 1 )";

    String expected = subSelect + " is null";

    testIt(
        EQ,
        NV,
        Collections.singleton((capturedSql) -> assertThat(capturedSql, containsString(expected))));
  }

  @Test
  void verifyGetEnrollmentsWithMissingValueNeqFilter() {
    String subSelect =
        "(select \"fWIAEtYVEGk\" from analytics_event_"
            + programA.getUid()
            + " where analytics_event_"
            + programA.getUid()
            + ".eventstatus != 'SCHEDULE' and analytics_event_"
            + programA.getUid()
            + ".enrollment = ax.enrollment and \"fWIAEtYVEGk\" is not null and ps = '"
            + programStage.getUid()
            + "' order by occurreddate desc, created desc  limit 1 )";

    String expected = subSelect + " is not null";
    testIt(
        NEQ,
        NV,
        Collections.singleton((capturedSql) -> assertThat(capturedSql, containsString(expected))));
  }

  @Test
  void verifyGetEnrollmentsWithMissingValueAndNumericValuesInFilter() {
    String subSelect =
        "(select \"fWIAEtYVEGk\" from analytics_event_"
            + programA.getUid()
            + " where analytics_event_"
            + programA.getUid()
            + ".eventstatus != 'SCHEDULE' and analytics_event_"
            + programA.getUid()
            + ".enrollment = ax.enrollment and \"fWIAEtYVEGk\" is not null and ps = '"
            + programStage.getUid()
            + "' order by occurreddate desc, created desc  limit 1 )";

    String numericValues = String.join(OPTION_SEP, "10", "11", "12");
    String expected =
        "("
            + subSelect
            + " in ("
            + String.join(",", numericValues.split(OPTION_SEP))
            + ") or ("
            + subSelect
            + " is null and exists("
            + subSelect
            + ")))";
    testIt(
        IN,
        numericValues + OPTION_SEP + NV,
        Collections.singleton((capturedSql) -> assertThat(capturedSql, containsString(expected))));
  }

  @Test
  void verifyGetEnrollmentsWithoutMissingValueAndNumericValuesInFilter() {
    String subSelect =
        "(select \"fWIAEtYVEGk\" from analytics_event_"
            + programA.getUid()
            + " where analytics_event_"
            + programA.getUid()
            + ".eventstatus != 'SCHEDULE' and analytics_event_"
            + programA.getUid()
            + ".enrollment = ax.enrollment and \"fWIAEtYVEGk\" is not null and ps = '"
            + programStage.getUid()
            + "' order by occurreddate desc, created desc  limit 1 )";

    String numericValues = String.join(OPTION_SEP, "10", "11", "12");
    String expected = subSelect + " in (" + String.join(",", numericValues.split(OPTION_SEP)) + ")";
    testIt(
        IN,
        numericValues,
        Collections.singleton((capturedSql) -> assertThat(capturedSql, containsString(expected))));
  }

  @Test
  void verifyGetEnrollmentsWithOnlyMissingValueInFilter() {
    String subSelect =
        "(select \"fWIAEtYVEGk\" from analytics_event_"
            + programA.getUid()
            + " where analytics_event_"
            + programA.getUid()
            + ".eventstatus != 'SCHEDULE' and analytics_event_"
            + programA.getUid()
            + ".enrollment = ax.enrollment and \"fWIAEtYVEGk\" is not null and ps = '"
            + programStage.getUid()
            + "' order by occurreddate desc, created desc  limit 1 )";

    String expected = subSelect + " is null";
    String unexpected = "(" + subSelect + " in (";
    testIt(
        IN,
        NV,
        List.of(
            (capturedSql) -> assertThat(capturedSql, containsString(expected)),
            (capturedSql) -> assertThat(capturedSql, not(containsString(unexpected)))));
  }

  private void testIt(
      QueryOperator operator, String filter, Collection<Consumer<String>> assertions) {
    subject.getEnrollments(
        createRequestParamsWithFilter(programStage, ValueType.INTEGER, operator, filter),
        new ListGrid(),
        10000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertions.forEach(consumer -> consumer.accept(sql.getValue()));
  }

  @Test
  void verifyWithProgramIndicatorAndRelationshipTypeBothSidesTrackedEntity() {
    Date startDate = getDate(2015, 1, 1);
    Date endDate = getDate(2017, 4, 8);

    String piSubquery = "distinct event";

    ProgramIndicator programIndicatorA = createProgramIndicator('A', programA, "", "");

    RelationshipType relationshipTypeA = createRelationshipType();

    EventQueryParams.Builder params =
        new EventQueryParams.Builder(createRequestParams(programIndicatorA, relationshipTypeA))
            .withStartDate(startDate)
            .withEndDate(endDate);

    when(programIndicatorService.getAnalyticsSql(
            "", NUMERIC, programIndicatorA, getDate(2000, 1, 1), getDate(2017, 4, 8), "subax"))
        .thenReturn(piSubquery);

    subject.getEnrollments(params.build(), new ListGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\",ax.\"ou\",(SELECT avg ("
            + piSubquery
            + ") FROM analytics_event_"
            + programA.getUid().toLowerCase()
            + " as subax WHERE  "
            + "subax.trackedentity in (select te.uid from trackedentity te "
            + "left join relationshipitem ri on te.trackedentityid = ri.trackedentityid  "
            + "left join relationship r on r.from_relationshipitemid = ri.relationshipitemid "
            + "left join relationshipitem ri2 on r.to_relationshipitemid = ri2.relationshipitemid "
            + "left join relationshiptype rty on rty.relationshiptypeid = r.relationshiptypeid "
            + "left join trackedentity te2 on te2.trackedentityid = ri2.trackedentityid "
            + "WHERE rty.relationshiptypeid = "
            + relationshipTypeA.getId()
            + " and te2.uid = ax.trackedentity )) as \""
            + programIndicatorA.getUid()
            + "\"  "
            + "from analytics_enrollment_"
            + programA.getUid()
            + " as ax where (((enrollmentdate >= '2015-01-01' and enrollmentdate < '2017-04-09'))) and (ax.\"uidlevel1\" = 'ouabcdefghA' ) limit 101";

    assertSql(sql.getValue(), expected);
  }

  @Test
  void verifyWithProgramIndicatorAndRelationshipTypeDifferentConstraint() {
    Date startDate = getDate(2015, 1, 1);
    Date endDate = getDate(2017, 4, 8);

    String piSubquery = "distinct event";

    ProgramIndicator programIndicatorA = createProgramIndicator('A', programA, "", "");

    RelationshipType relationshipTypeA =
        createRelationshipType(RelationshipEntity.PROGRAM_INSTANCE);

    EventQueryParams.Builder params =
        new EventQueryParams.Builder(createRequestParams(programIndicatorA, relationshipTypeA))
            .withStartDate(startDate)
            .withEndDate(endDate);

    when(programIndicatorService.getAnalyticsSql(
            "", NUMERIC, programIndicatorA, getDate(2000, 1, 1), getDate(2017, 4, 8), "subax"))
        .thenReturn(piSubquery);

    subject.getEnrollments(params.build(), new ListGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\",ax.\"ou\",(SELECT avg ("
            + piSubquery
            + ") FROM analytics_event_"
            + programA.getUid().toLowerCase()
            + " as subax WHERE "
            + " subax.trackedentity in (select te.uid from trackedentity te "
            + "left join relationshipitem ri on te.trackedentityid = ri.trackedentityid  "
            + "left join relationship r on r.from_relationshipitemid = ri.relationshipitemid "
            + "left join relationshipitem ri2 on r.to_relationshipitemid = ri2.relationshipitemid "
            + "left join relationshiptype rty on rty.relationshiptypeid = r.relationshiptypeid "
            + "left join enrollment en2 on en2.enrollmentid = ri2.enrollmentid WHERE rty.relationshiptypeid "
            + "= "
            + relationshipTypeA.getId()
            + " and en2.uid = ax.enrollment ))"
            + " as \""
            + programIndicatorA.getUid()
            + "\"  "
            + "from analytics_enrollment_"
            + programA.getUid()
            + " as ax where (((enrollmentdate >= '2015-01-01' and enrollmentdate < '2017-04-09'))) and (ax.\"uidlevel1\" = 'ouabcdefghA' ) limit 101";

    assertSql(sql.getValue(), expected);
  }

  @Override
  String getTableName() {
    return "analytics_enrollment";
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

  private void assertSql(String actual, String expected) {
    assertThat(actual, is("select " + DEFAULT_COLUMNS + "," + expected));
  }

  @Test
  void verifyWithProgramIndicatorAndRelationshipTypeBothSidesTrackedEntity2() {
    Date startDate = getDate(2015, 1, 1);
    Date endDate = getDate(2017, 4, 8);
    Program programB = createProgram('B');
    String piSubquery = "distinct event";

    ProgramIndicator programIndicatorA = createProgramIndicator('A', programB, "", "");

    RelationshipType relationshipTypeA = createRelationshipType();

    EventQueryParams.Builder params =
        new EventQueryParams.Builder(createRequestParams(programIndicatorA, relationshipTypeA))
            .withStartDate(startDate)
            .withEndDate(endDate);

    when(programIndicatorService.getAnalyticsSql(
            "", NUMERIC, programIndicatorA, getDate(2000, 1, 1), getDate(2017, 4, 8), "subax"))
        .thenReturn(piSubquery);

    subject.getEnrollments(params.build(), new ListGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\",ax.\"ou\",(SELECT avg ("
            + piSubquery
            + ") FROM analytics_event_"
            + programB.getUid().toLowerCase()
            + " as subax WHERE  "
            + "subax.trackedentity in (select te.uid from trackedentity te "
            + "left join relationshipitem ri on te.trackedentityid = ri.trackedentityid  "
            + "left join relationship r on r.from_relationshipitemid = ri.relationshipitemid "
            + "left join relationshipitem ri2 on r.to_relationshipitemid = ri2.relationshipitemid "
            + "left join relationshiptype rty on rty.relationshiptypeid = r.relationshiptypeid "
            + "left join trackedentity te2 on te2.trackedentityid = ri2.trackedentityid "
            + "WHERE rty.relationshiptypeid = "
            + relationshipTypeA.getId()
            + " and te2.uid = ax.trackedentity )) as \""
            + programIndicatorA.getUid()
            + "\"  "
            + "from analytics_enrollment_"
            + programA.getUid()
            + " as ax where (((enrollmentdate >= '2015-01-01' and enrollmentdate < '2017-04-09'))) and (ax.\"uidlevel1\" = 'ouabcdefghA' ) limit 101";

    assertSql(sql.getValue(), expected);
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

    assertThat(
        columnSql,
        is(
            "(select \""
                + dataElementA.getUid()
                + "\" from analytics_event_"
                + programA.getUid()
                + " where analytics_event_"
                + programA.getUid()
                + ".eventstatus != 'SCHEDULE' and analytics_event_"
                + programA.getUid()
                + ".enrollment = ax.enrollment and \""
                + dataElementA.getUid()
                + "\" is not null and ps = '"
                + programStage.getUid()
                + "' order by occurreddate desc, created desc  limit 1 )"));
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

    assertThat(
        columnSql,
        is(
            "(select \""
                + dataElementA.getUid()
                + "\" from analytics_event_"
                + programB.getUid()
                + " where analytics_event_"
                + programB.getUid()
                + ".eventstatus != 'SCHEDULE' and analytics_event_"
                + programB.getUid()
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
    String eventTableName = "analytics_event_" + item.getProgram().getUid();

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
    String eventTableName = "analytics_event_" + item.getProgram().getUid();

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
