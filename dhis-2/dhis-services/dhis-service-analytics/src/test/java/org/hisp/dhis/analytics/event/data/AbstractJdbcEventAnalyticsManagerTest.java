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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.fromAggregationType;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.common.QueryOperator.NE;
import static org.hisp.dhis.common.QueryOperator.NEQ;
import static org.hisp.dhis.common.QueryOperator.NIEQ;
import static org.hisp.dhis.common.QueryOperator.NILIKE;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.AGGREGATE;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.QUERY;
import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.ENROLLMENT;
import static org.hisp.dhis.common.ValueType.BOOLEAN;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.period.RelativePeriodEnum.THIS_YEAR;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createPeriod;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramIndicator;
import static org.hisp.dhis.test.TestBase.getDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.sql.rowset.RowSetMetaDataImpl;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryParams.Builder;
import org.hisp.dhis.analytics.event.data.programindicator.DefaultProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.FallbackCoordinateFieldType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class AbstractJdbcEventAnalyticsManagerTest extends EventAnalyticsTest {
  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private ProgramIndicatorService programIndicatorService;

  @Mock private ExecutionPlanStore executionPlanStore;

  @Mock private OrganisationUnitService organisationUnitService;
  @Mock private DataElementService dataElementService;

  @Mock private SystemSettingsService systemSettingsService;

  @Mock private OrganisationUnitResolver organisationUnitResolver;

  @Mock private PiDisagQueryGenerator piDisagQueryGenerator;

  @Spy
  private ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder =
      new DefaultProgramIndicatorSubqueryBuilder(
          programIndicatorService,
          systemSettingsService,
          new PostgreSqlBuilder(),
          dataElementService);

  @Spy private AnalyticsSqlBuilder sqlBuilder = new PostgreSqlAnalyticsSqlBuilder();

  @Spy
  private EventTimeFieldSqlRenderer eventTimeFieldSqlRenderer =
      new EventTimeFieldSqlRenderer(sqlBuilder);

  @Spy
  private EnrollmentTimeFieldSqlRenderer enrollmentTimeFieldSqlRenderer =
      new EnrollmentTimeFieldSqlRenderer(sqlBuilder);

  @InjectMocks private JdbcEventAnalyticsManager eventSubject;

  @InjectMocks private JdbcEnrollmentAnalyticsManager enrollmentSubject;

  private Program programA;

  private DataElement dataElementA;

  private final Date from = getDate(2017, 10, 10);

  private final Date to = getDate(2018, 10, 10);

  private final UUID uuidA = UUID.fromString("c0d3661b-c7f6-48a0-8cf3-b1380cd005dd");

  private final UUID uuidB = UUID.fromString("1786142e-6d51-48e3-8bbe-9cc2a2836120");

  @BeforeEach
  public void setUp() {
    programA = createProgram('A');
    dataElementA = createDataElement('A', ValueType.INTEGER, AggregationType.SUM);
    dataElementA.setUid("fWIAEtYVEGk");
  }

  @Test
  void verifyGetSelectSqlWithProgramIndicator() {
    ProgramIndicator programIndicator = createProgramIndicator('A', programA, "9.0", null);
    QueryItem item = new QueryItem(programIndicator);
    eventSubject.getSelectSql(new QueryFilter(), item, from, to);

    verify(programIndicatorService)
        .getAnalyticsSql(programIndicator.getExpression(), NUMERIC, programIndicator, from, to);
  }

  @Test
  void verifyGetSelectSqlWithTextDataElementIgnoringCase() {
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());

    QueryItem item = new QueryItem(dio);
    item.setValueType(ValueType.TEXT);

    QueryFilter queryFilter = new QueryFilter(QueryOperator.IEQ, "IEQ");

    String column = eventSubject.getSelectSql(queryFilter, item, from, to);

    assertThat(column, is("lower(ax.\"" + dataElementA.getUid() + "\")"));
  }

  @Test
  void verifyGetSelectSqlWithTextDataElement() {
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());

    QueryItem item = new QueryItem(dio);
    item.setValueType(ValueType.TEXT);

    QueryFilter queryFilter = new QueryFilter(EQ, "EQ");

    String column = eventSubject.getSelectSql(queryFilter, item, from, to);

    assertThat(column, is("ax.\"" + dataElementA.getUid() + "\""));
  }

  @Test
  void verifyGetSelectSqlWithNonTextDataElement() {
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());

    QueryItem item = new QueryItem(dio);
    item.setValueType(NUMBER);

    String column = eventSubject.getSelectSql(new QueryFilter(), item, from, to);

    assertThat(column, is("ax.\"" + dataElementA.getUid() + "\""));
  }

  @Test
  void verifyGetCoordinateColumn() {
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());
    QueryItem item = new QueryItem(dio);

    String column = eventSubject.getCoordinateColumn(item).asSql();

    String colName = quote(item.getItemName());

    assertThat(
        column,
        is(
            "'[' || round(ST_X("
                + colName
                + ")::numeric, 6) || ',' || round(ST_Y("
                + colName
                + ")::numeric, 6) || ']' as "
                + colName));
  }

  @Test
  void verifyGetColumn() {
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());

    QueryItem item = new QueryItem(dio);

    String column = eventSubject.getColumn(item);

    assertThat(column, is("ax.\"" + dataElementA.getUid() + "\""));
  }

  @Override
  String getTableName() {
    return "";
  }

  @Test
  void verifyGetAggregateClauseWithBooleanValue() {
    DataElement booleanElement = createDataElement('A');
    booleanElement.setValueType(BOOLEAN);

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams()).withValue(booleanElement).build();

    String clause = eventSubject.getAggregateClause(params);

    assertThat(clause, is("sum(ax.\"" + booleanElement.getUid() + "\")"));
  }

  @Test
  void verifyGetAggregateClauseWithValue() {
    DataElement de = new DataElement();

    de.setUid(dataElementA.getUid());
    de.setAggregationType(AggregationType.SUM);
    de.setValueType(NUMBER);

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .withValue(de)
            .withAggregationType(AnalyticsAggregationType.SUM)
            .build();

    String clause = eventSubject.getAggregateClause(params);

    assertThat(clause, is("sum(ax.\"fWIAEtYVEGk\")"));
  }

  @Test
  void verifyGetAggregateClauseWithValueFails() {
    DataElement de = new DataElement();

    de.setAggregationType(AggregationType.CUSTOM);
    de.setValueType(NUMBER);

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .withValue(de)
            .withAggregationType(fromAggregationType(AggregationType.CUSTOM))
            .build();

    assertThrows(NullPointerException.class, () -> eventSubject.getAggregateClause(params));
  }

  @ParameterizedTest
  @MethodSource("noAggregationTestCases")
  void verifyGetAggregateClauseWithNoAggregation(
      AggregationType valueAggregationType, AggregationType overrideAggregationType) {
    DataElement de = new DataElement();

    de.setUid(dataElementA.getUid());
    de.setAggregationType(valueAggregationType);
    de.setValueType(NUMBER);

    EventQueryParams.Builder paramsBuilder =
        new EventQueryParams.Builder(createRequestParams()).withValue(de);

    // Apply override aggregation if specified
    if (overrideAggregationType != null) {
      paramsBuilder.withAggregationType(
          AnalyticsAggregationType.fromAggregationType(overrideAggregationType));
    }

    EventQueryParams params = paramsBuilder.build();

    String clause = eventSubject.getAggregateClause(params);

    assertThat(clause, is("null"));
  }

  private static Stream<Arguments> noAggregationTestCases() {
    return Stream.of(
        // Test case 1: Both value and override are NONE
        Arguments.of(AggregationType.NONE, AggregationType.NONE),

        // Test case 2: Value is NONE, no override
        Arguments.of(AggregationType.NONE, null),

        // Test case 3: Value is SUM, override is NONE
        Arguments.of(AggregationType.SUM, AggregationType.NONE));
  }

  @Test
  void verifyGetAggregateClauseWithEventFallback() {
    DataElement de = new DataElement();

    de.setAggregationType(AggregationType.NONE);
    de.setValueType(TEXT);

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .withValue(de)
            .withAggregationType(fromAggregationType(AggregationType.CUSTOM))
            .withOutputType(EventOutputType.EVENT)
            .build();

    String aggregateClause = eventSubject.getAggregateClause(params);

    assertEquals("count(ax.\"event\")", aggregateClause);
  }

  @Test
  void verifyGetAggregateClauseWithEnrollmentFallback() {
    DataElement de = new DataElement();

    de.setAggregationType(AggregationType.SUM);
    de.setValueType(TEXT);

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .withValue(de)
            .withAggregationType(fromAggregationType(AggregationType.CUSTOM))
            .withOutputType(EventOutputType.ENROLLMENT)
            .build();

    String aggregateClause = eventSubject.getAggregateClause(params);

    assertEquals("count(distinct ax.\"enrollment\")", aggregateClause);
  }

  @Test
  void verifyGetAggregateClauseWithProgramIndicator() {
    ProgramIndicator programIndicator = createProgramIndicator('A', programA, "9.0", null);
    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .withProgramIndicator(programIndicator)
            .build();

    when(programIndicatorService.getAnalyticsSql(
            programIndicator.getExpression(),
            NUMERIC,
            programIndicator,
            params.getEarliestStartDate(),
            params.getLatestEndDate()))
        .thenReturn("select * from table");

    String clause = eventSubject.getAggregateClause(params);

    assertThat(clause, is("avg(select * from table)"));
  }

  @Test
  void verifyGetAggregateClauseWithProgramIndicatorAndCustomAggregationType() {
    ProgramIndicator programIndicator = createProgramIndicator('A', programA, "9.0", null);
    programIndicator.setAggregationType(AggregationType.CUSTOM);

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .withProgramIndicator(programIndicator)
            .withAggregationType(fromAggregationType(programIndicator.getAggregationTypeFallback()))
            .build();

    when(programIndicatorService.getAnalyticsSql(
            programIndicator.getExpression(),
            NUMERIC,
            programIndicator,
            params.getEarliestStartDate(),
            params.getLatestEndDate()))
        .thenReturn("select * from table");

    String clause = eventSubject.getAggregateClause(params);

    assertThat(clause, is("(select * from table)"));
  }

  @Test
  void verifyGetAggregateClauseWithEnrollmentDimension() {
    ProgramIndicator programIndicator = createProgramIndicator('A', programA, "9.0", null);
    programIndicator.setAnalyticsType(AnalyticsType.ENROLLMENT);
    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .withProgramIndicator(programIndicator)
            .build();

    when(programIndicatorService.getAnalyticsSql(
            programIndicator.getExpression(),
            NUMERIC,
            programIndicator,
            params.getEarliestStartDate(),
            params.getLatestEndDate()))
        .thenReturn("select * from table");

    String clause = eventSubject.getAggregateClause(params);

    assertThat(clause, is("avg(select * from table)"));
  }

  @Test
  void
      verifyGetColumnsWithAttributeOrgUnitTypeAndCoordinatesReturnsFetchesCoordinatesFromOrgUnite() {
    DataElement deA = createDataElement('A', ValueType.ORGANISATION_UNIT, AggregationType.NONE);
    DimensionalObject periods =
        new BaseDimensionalObject(
            DimensionalObject.PERIOD_DIM_ID,
            DimensionType.PERIOD,
            List.of(MonthlyPeriodType.getPeriodFromIsoString("201701")));

    DimensionalObject orgUnits =
        new BaseDimensionalObject(
            DimensionalObject.ORGUNIT_DIM_ID,
            DimensionType.ORGANISATION_UNIT,
            "ouA",
            List.of(createOrganisationUnit('A')));

    QueryItem qiA = new QueryItem(deA, null, deA.getValueType(), deA.getAggregationType(), null);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(periods)
            .addDimension(orgUnits)
            .addItem(qiA)
            .withCoordinateFields(List.of(deA.getUid()))
            .withSkipData(true)
            .withSkipMeta(false)
            .build();

    List<String> columns = this.eventSubject.getSelectColumns(params, false);

    assertThat(columns, hasSize(3));
    assertThat(
        columns,
        containsInAnyOrder(
            "ax.\"pe\"",
            "ax.\"ou\"",
            "'[' || round(ST_X(ST_Centroid(\""
                + deA.getUid()
                + "_geom"
                + "\"))::numeric, 6) || ',' || round(ST_Y(ST_Centroid(\""
                + deA.getUid()
                + "_geom"
                + "\"))::numeric, 6) || ']' as \""
                + deA.getUid()
                + "_geom"
                + "\""));
  }

  @Test
  void
      verifyGetWhereClauseWithAttributeOrgUnitTypeAndCoordinatesReturnsFetchesCoordinatesFromOrgUnite() {
    DataElement deA = createDataElement('A', ValueType.ORGANISATION_UNIT, AggregationType.NONE);
    DimensionalObject periods =
        new BaseDimensionalObject(
            DimensionalObject.PERIOD_DIM_ID,
            DimensionType.PERIOD,
            List.of(MonthlyPeriodType.getPeriodFromIsoString("201701")));

    DimensionalObject orgUnits =
        new BaseDimensionalObject(
            DimensionalObject.ORGUNIT_DIM_ID,
            DimensionType.ORGANISATION_UNIT,
            "ouA",
            List.of(createOrganisationUnit('A')));

    QueryItem qiA = new QueryItem(deA, null, deA.getValueType(), deA.getAggregationType(), null);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(periods)
            .addDimension(orgUnits)
            .addItem(qiA)
            .withCoordinateFields(List.of(deA.getUid()))
            .withSkipData(true)
            .withSkipMeta(false)
            .withStartDate(new Date())
            .withEndDate(new Date())
            // the not null condition is only triggered by this flag (or
            // withGeometry) being true
            .withCoordinatesOnly(true)
            .build();

    String whereClause = this.eventSubject.getWhereClause(params);

    assertThat(
        whereClause,
        containsString("and coalesce(ax.\"" + deA.getUid() + "_geom" + "\") is not null"));
  }

  @Test
  void testGetWhereClauseWithMultipleOrgUnitDescendantsAtSameLevel() {
    DimensionalObject periods =
        new BaseDimensionalObject(
            DimensionalObject.PERIOD_DIM_ID,
            DimensionType.PERIOD,
            List.of(MonthlyPeriodType.getPeriodFromIsoString("201801")));

    DimensionalObject multipleOrgUnitsSameLevel =
        new BaseDimensionalObject(
            DimensionalObject.ORGUNIT_DIM_ID,
            DimensionType.ORGANISATION_UNIT,
            "uidlevel1",
            "Level 1",
            List.of(
                createOrganisationUnit('A'),
                createOrganisationUnit('B'),
                createOrganisationUnit('C')));

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(periods)
            .addDimension(multipleOrgUnitsSameLevel)
            .withSkipData(true)
            .withSkipMeta(false)
            .withStartDate(new Date())
            .withEndDate(new Date())
            .build();

    String whereClause = this.eventSubject.getWhereClause(params);

    assertThat(
        whereClause,
        containsString("and ax.\"uidlevel1\" in ('ouabcdefghA','ouabcdefghB','ouabcdefghC')"));
  }

  @Test
  void testValidCoordinatesFieldInSqlWhereClauseForEvent() {
    EventQueryParams params =
        getEventQueryParamsForCoordinateFieldsTest(
            List.of("enrollmentgeometry", "eventgeometry", "tegeometry", "ougeometry"));

    String whereClause = this.eventSubject.getWhereClause(params);

    assertThat(
        whereClause,
        containsString(
            "coalesce(ax.\"enrollmentgeometry\",ax.\"eventgeometry\",ax.\"tegeometry\",ax.\"ougeometry\") is not null"));
  }

  @Test
  void testMissingPsiGeometryInDefaultCoordinatesFieldInSqlSelectClause() {
    EventQueryParams params =
        getEventQueryParamsForCoordinateFieldsTest(
            List.of("enrollmentgeometry", "tegeometry", "ougeometry"));

    String whereClause = this.eventSubject.getSelectClause(params);

    assertThat(
        whereClause,
        containsString("coalesce(ax.\"enrollmentgeometry\",ax.\"tegeometry\",ax.\"ougeometry\")"));
  }

  @Test
  void testValidExplicitCoordinatesFieldInSqlSelectClause() {
    EventQueryParams params =
        getEventQueryParamsForCoordinateFieldsTest(List.of("ougeometry", "eventgeometry"));

    String whereClause = this.eventSubject.getSelectClause(params);

    assertThat(whereClause, containsString("coalesce(ax.\"ougeometry\",ax.\"eventgeometry\")"));
  }

  @Test
  void testGetCoalesceReturnsDefaultColumnNameWhenCoordinateFieldIsEmpty() {
    String sql =
        this.eventSubject.getCoalesce(
            List.of(), FallbackCoordinateFieldType.EVENT_GEOMETRY.getValue());

    assertEquals(FallbackCoordinateFieldType.EVENT_GEOMETRY.getValue(), sql);
  }

  @Test
  void testGetCoalesceReturnsDefaultColumnNameWhenCoordinateFieldCollectionIsNull() {
    String sqlSnippet =
        this.eventSubject.getCoalesce(null, FallbackCoordinateFieldType.EVENT_GEOMETRY.getValue());

    assertEquals(FallbackCoordinateFieldType.EVENT_GEOMETRY.getValue(), sqlSnippet);
  }

  @Test
  void testGetCoalesceReturnsCoalesceWhenCoordinateFieldCollectionIsNotEmpty() {
    String sqlSnippet =
        this.eventSubject.getCoalesce(
            List.of("coorA", "coorB", "coorC"),
            FallbackCoordinateFieldType.EVENT_GEOMETRY.getValue());

    assertEquals("coalesce(ax.\"coorA\",ax.\"coorB\",ax.\"coorC\")", sqlSnippet);
  }

  @Test
  void testGeItemNoFiltersSql() {
    EventQueryParams queryParams =
        new EventQueryParams.Builder()
            .addItem(buildQueryItemWithGroupAndFilters("item", uuidA, List.of()))
            .build();
    assertEquals("", eventSubject.getQueryItemsAndFiltersWhereClause(queryParams, new SqlHelper()));
  }

  @Test
  void testGetItemSimpleFilterSql() {
    EventQueryParams queryParams =
        defaultEventQueryParamsBuilder()
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item", uuidA, List.of(buildQueryFilter(EQ, "A"))))
            .build();
    String result = eventSubject.getQueryItemsAndFiltersWhereClause(queryParams, new SqlHelper());
    assertEquals("where ax.\"item\" = 'A' ", result);
  }

  @Test
  void testGetItemNotLikeFilterSql() {
    EventQueryParams queryParams =
        defaultEventQueryParamsBuilder()
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item", uuidA, List.of(buildQueryFilter(NEQ, "12")), NUMBER))
            .build();
    String result = eventSubject.getQueryItemsAndFiltersWhereClause(queryParams, new SqlHelper());
    assertEquals("where (ax.\"item\" is null or ax.\"item\" != '12') ", result);
  }

  @Test
  void testGetItemNotILikeFilterSqlNullValueType() {
    EventQueryParams queryParams =
        defaultEventQueryParamsBuilder()
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item", uuidA, List.of(buildQueryFilter(NILIKE, "A"))))
            .build();
    String result = eventSubject.getQueryItemsAndFiltersWhereClause(queryParams, new SqlHelper());
    assertEquals("where (ax.\"item\" is null or ax.\"item\" not ilike '%A%') ", result);
  }

  private Builder defaultEventQueryParamsBuilder() {
    return new EventQueryParams.Builder()
        .withPeriods(List.of(createPeriod("201801")), PeriodTypeEnum.MONTHLY.getName());
  }

  @Test
  void testGetItemNotEqualsFilterSql() {
    EventQueryParams queryParams =
        defaultEventQueryParamsBuilder()
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item", uuidA, List.of(buildQueryFilter(NEQ, "A")), TEXT))
            .build();
    String result = eventSubject.getQueryItemsAndFiltersWhereClause(queryParams, new SqlHelper());
    assertEquals("where (coalesce(ax.\"item\", '') = '' or ax.\"item\" != 'A') ", result);

    queryParams =
        defaultEventQueryParamsBuilder()
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item", uuidA, List.of(buildQueryFilter(NE, "A")), TEXT))
            .build();
    result = eventSubject.getQueryItemsAndFiltersWhereClause(queryParams, new SqlHelper());
    assertEquals("where (coalesce(ax.\"item\", '') = '' or ax.\"item\" != 'A') ", result);
  }

  @Test
  void testGetItemNotIEqualsFilterSql() {
    EventQueryParams queryParams =
        defaultEventQueryParamsBuilder()
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item", uuidA, List.of(buildQueryFilter(NIEQ, "A")), TEXT))
            .build();
    String result = eventSubject.getQueryItemsAndFiltersWhereClause(queryParams, new SqlHelper());
    assertEquals(
        "where (coalesce(lower(ax.\"item\"), '') = '' or lower(ax.\"item\") != 'a') ", result);
  }

  @Test
  void testGetItemTwoConditionsSameGroupSql() {
    EventQueryParams queryParams =
        defaultEventQueryParamsBuilder()
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item", uuidA, List.of(buildQueryFilter(EQ, "A"), buildQueryFilter(EQ, "B"))))
            .build();
    String result = eventSubject.getQueryItemsAndFiltersWhereClause(queryParams, new SqlHelper());
    assertEquals("where ax.\"item\" = 'A'  and ax.\"item\" = 'B' ", result);
  }

  @Test
  void testGetItemSameItemTwoConditionsSqlEnhancedConditions() {
    EventQueryParams queryParams =
        defaultEventQueryParamsBuilder()
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item", uuidA, List.of(buildQueryFilter(EQ, "A"), buildQueryFilter(EQ, "B"))))
            .withEnhancedConditions(true)
            .build();

    String result = eventSubject.getQueryItemsAndFiltersWhereClause(queryParams, new SqlHelper());
    assertEquals("where (ax.\"item\" = 'A'  and ax.\"item\" = 'B' )", result);
  }

  @Test
  void testGetItemTwoConditionsSameGroupSqlEnhancedConditions() {
    EventQueryParams queryParams =
        defaultEventQueryParamsBuilder()
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item1", uuidA, List.of(buildQueryFilter(EQ, "A"))))
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item2", uuidA, List.of(buildQueryFilter(EQ, "B"))))
            .withEnhancedConditions(true)
            .build();

    String result = eventSubject.getQueryItemsAndFiltersWhereClause(queryParams, new SqlHelper());
    assertEquals("where (ax.\"item1\" = 'A'  or ax.\"item2\" = 'B' )", result);
  }

  @Test
  void testGetItemTwoConditionsDifferentGroupsSqlEnhancedConditions() {
    EventQueryParams queryParams =
        defaultEventQueryParamsBuilder()
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item1", uuidA, List.of(buildQueryFilter(EQ, "A"))))
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item2", uuidA, List.of(buildQueryFilter(EQ, "B"))))
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item3", uuidB, List.of(buildQueryFilter(EQ, "C"))))
            .addItem(
                buildQueryItemWithGroupAndFilters(
                    "item4", uuidB, List.of(buildQueryFilter(EQ, "D"))))
            .withEnhancedConditions(true)
            .build();

    String result = eventSubject.getQueryItemsAndFiltersWhereClause(queryParams, new SqlHelper());
    assertTrue(result.contains("(ax.\"item1\" = 'A'  or ax.\"item2\" = 'B' )"));
    assertTrue(result.contains("(ax.\"item3\" = 'C'  or ax.\"item4\" = 'D' )"));
  }

  @Test
  void testAddGridValueForDoubleObject() throws SQLException {
    Double doubleNumber = 35.5d;
    String doubleValue = "35.5";
    int index = 1;

    RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
    metaData.setColumnCount(2);
    metaData.setColumnName(1, "col-1");
    metaData.setColumnName(2, "col-2");

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getObject(index)).thenReturn(doubleNumber);
    when(resultSet.getMetaData()).thenReturn(metaData);

    EventQueryParams queryParams = new EventQueryParams.Builder().withSkipRounding(false).build();

    GridHeader header = new GridHeader("header-1", NUMBER);
    Grid grid = new ListGrid();
    grid.addHeader(header);
    grid.addRow();

    SqlRowSet sqlRowSet = new ResultSetWrappingSqlRowSet(resultSet);

    eventSubject.addGridValue(grid, header, index, sqlRowSet, queryParams);

    assertTrue(grid.getColumn(0).contains(doubleValue), "Should contain value " + doubleValue);
  }

  @Test
  void testAddGridValueForBigDecimalObject() throws SQLException {
    // Given
    BigDecimal bigDecimalObject = new BigDecimal("123.40000000");
    int index = 1;

    RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
    metaData.setColumnCount(2);
    metaData.setColumnName(1, "col-1");
    metaData.setColumnName(2, "col-2");

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getObject(index)).thenReturn(bigDecimalObject);
    when(resultSet.getMetaData()).thenReturn(metaData);

    EventQueryParams queryParams = new EventQueryParams.Builder().build();

    GridHeader header = new GridHeader("header-1", NUMBER);
    Grid grid = new ListGrid();
    grid.addHeader(header);
    grid.addRow();

    SqlRowSet sqlRowSet = new ResultSetWrappingSqlRowSet(resultSet);

    // When
    eventSubject.addGridValue(grid, header, index, sqlRowSet, queryParams);

    // Then
    String expected =
        bigDecimalObject.setScale(2, RoundingMode.CEILING).stripTrailingZeros().toPlainString();
    assertEquals(expected, grid.getColumn(0).get(0), "Should contain value " + expected);
  }

  @Test
  void testAddGridValueForNull() throws SQLException {
    Double nullObject = null;
    int index = 1;

    RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
    metaData.setColumnCount(2);
    metaData.setColumnName(1, "col-1");
    metaData.setColumnName(2, "col-2");

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getObject(index)).thenReturn(nullObject);
    when(resultSet.getMetaData()).thenReturn(metaData);
    EventQueryParams queryParams = new EventQueryParams.Builder().withSkipRounding(false).build();

    GridHeader header = new GridHeader("header-1", NUMBER);
    Grid grid = new ListGrid();
    grid.addHeader(header);
    grid.addRow();

    SqlRowSet sqlRowSet = new ResultSetWrappingSqlRowSet(resultSet);

    eventSubject.addGridValue(grid, header, index, sqlRowSet, queryParams);

    assertTrue(grid.getColumn(0).contains(EMPTY), "Should contain empty value");
  }

  @Test
  void testGetSelectClauseForAggregatedEnrollments() {
    // Given
    Period period = new Period(THIS_YEAR);
    period.setPeriodType(new YearlyPeriodType());
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(createProgram('A'))
            .withEndpointAction(AGGREGATE)
            .withEndpointItem(ENROLLMENT)
            .withPeriods(List.of(period), PeriodTypeEnum.YEARLY.getName())
            .build();
    // When
    String select = enrollmentSubject.getSelectClause(params);
    // Then
    assertEquals("select enrollment,Yearly ", select);
  }

  @Test
  void testItemsInFilterAreQuotedForOrganisationUnit() {
    // Given
    QueryItem queryItem = mock(QueryItem.class);
    QueryFilter queryFilter = new QueryFilter(IN, "A;B;C");
    EventQueryParams params =
        new EventQueryParams.Builder().withStartDate(new Date()).withEndDate(new Date()).build();
    when(queryItem.getItemName()).thenReturn("anyItem");
    when(queryItem.getValueType()).thenReturn(ValueType.ORGANISATION_UNIT);
    when(organisationUnitResolver.resolveOrgUnits(any(), any())).thenReturn("A;B;C");

    // When
    String sql = eventSubject.toSql(queryItem, queryFilter, params).trim();

    // Then
    assertEquals("ax.\"anyItem\" in ('A','B','C')", sql);
  }

  @Test
  void testInFilterForEnrollmentsAggregate() {
    // Given
    QueryItem queryItem = mock(QueryItem.class);
    QueryFilter queryFilter = new QueryFilter(IN, "A;B;C");
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withStartDate(new Date())
            .withEndDate(new Date())
            .withEndpointAction(AGGREGATE)
            .withEndpointItem(ENROLLMENT)
            .build();
    when(queryItem.getItemName()).thenReturn("anyItem");
    when(queryItem.getValueType()).thenReturn(ValueType.ORGANISATION_UNIT);
    when(organisationUnitResolver.resolveOrgUnits(any(), any())).thenReturn("A;B;C");

    // When
    String sql = eventSubject.toSql(queryItem, queryFilter, params).trim();

    // Then
    assertEquals("ax.\"anyItem\" in ('A','B','C')", sql);
  }

  @Test
  void testGetSelectClauseForQueryEnrollments() {
    // Given
    Period period = new Period(THIS_YEAR);
    period.setPeriodType(new YearlyPeriodType());
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(createProgram('A'))
            .withEndpointAction(QUERY)
            .withEndpointItem(ENROLLMENT)
            .withPeriods(List.of(period), PeriodTypeEnum.YEARLY.getName())
            .build();
    // When
    String select = enrollmentSubject.getSelectClause(params);
    // Then
    assertEquals(
        "select enrollment,trackedentity,enrollmentdate,occurreddate,storedby,createdbydisplayname,lastupdatedbydisplayname,lastupdated,ST_AsGeoJSON(enrollmentgeometry),longitude,latitude,ouname,ounamehierarchy,oucode,enrollmentstatus,ax.\"yearly\" ",
        select);
  }

  private QueryFilter buildQueryFilter(QueryOperator operator, String filter) {
    return new QueryFilter(operator, filter);
  }

  private QueryItem buildQueryItem(String item) {
    return new QueryItem(new BaseDimensionalItemObject(item));
  }

  private QueryItem buildQueryItemWithGroupAndFilters(
      String item, UUID groupUUID, Collection<QueryFilter> filters, ValueType valueType) {
    QueryItem queryItem = buildQueryItemWithGroupAndFilters(item, groupUUID, filters);
    queryItem.setValueType(valueType);

    return queryItem;
  }

  private QueryItem buildQueryItemWithGroupAndFilters(
      String item, UUID groupUUID, Collection<QueryFilter> filters) {
    QueryItem queryItem = buildQueryItem(item);
    queryItem.setGroupUUID(groupUUID);
    queryItem.setFilters(new ArrayList<>(filters));
    return queryItem;
  }

  private EventQueryParams getEventQueryParamsForCoordinateFieldsTest(
      List<String> coordinateFields) {
    DataElement deA = createDataElement('A', TEXT, AggregationType.NONE);
    Period peA = createPeriod("202201");
    QueryItem qiA = new QueryItem(deA, null, deA.getValueType(), deA.getAggregationType(), null);
    Program program = createProgram('A');

    return new EventQueryParams.Builder()
        .withPeriods(List.of(peA), PeriodTypeEnum.MONTHLY.getName())
        .withOrganisationUnits(List.of(createOrganisationUnit('A')))
        .addItem(qiA)
        .withProgram(program)
        .withStartDate(new Date())
        .withEndDate(new Date())
        .withCoordinatesOnly(true)
        .withGeometryOnly(true)
        .withCoordinateFields(coordinateFields)
        .build();
  }
}
