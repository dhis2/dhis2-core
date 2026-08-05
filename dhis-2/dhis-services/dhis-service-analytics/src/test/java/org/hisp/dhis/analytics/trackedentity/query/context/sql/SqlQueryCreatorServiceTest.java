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
package org.hisp.dhis.analytics.trackedentity.query.context.sql;

import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.AggregateQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.DataElementQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.EnrolledInProgramQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.LimitOffsetQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.MainTableQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.OrgUnitQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.PeriodQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.ProgramIndicatorQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.TrackedEntityQueryBuilder;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SqlQueryCreatorService}.
 *
 * @author Dusan Bernat
 */
class SqlQueryCreatorServiceTest extends TestBase {
  private SqlQueryCreatorService sqlQueryCreatorService;

  private List<SqlQueryBuilder> queryBuilders;

  @BeforeEach
  void setUp() {
    ProgramIndicatorService programIndicatorService = mock(ProgramIndicatorService.class);
    queryBuilders =
        List.of(
            new DataElementQueryBuilder(),
            new LimitOffsetQueryBuilder(),
            new MainTableQueryBuilder(),
            new OrgUnitQueryBuilder(),
            new PeriodQueryBuilder(),
            new EnrolledInProgramQueryBuilder(),
            new TrackedEntityQueryBuilder(mock(IdentifiableObjectManager.class)),
            new ProgramIndicatorQueryBuilder(programIndicatorService));
    sqlQueryCreatorService = new SqlQueryCreatorService(queryBuilders);
  }

  @Test
  void testSqlQueryRenderingWithOrgUnitNameObject() {
    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder().trackedEntityType(createTrackedEntityType('A')).build();

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(trackedEntityQueryParams)
            .commonRaw(new CommonRequestParams())
            .commonParsed(stubSortingCommonParams(null, 1, "ouname"))
            .build();

    String sql =
        sqlQueryCreatorService.getSqlQueryCreator(contextParams).createForSelect().getStatement();

    assertTrue(sql.contains("ouname"));
    assertContains("order by t_1.\"ouname\" desc", sql);
  }

  @Test
  void testSqlQueryRenderingWithCommonDimensionalObject() {
    DimensionalObject dimensionalObject = new BaseDimensionalObject("abc");

    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    Program program = createProgram('A');
    program.setTrackedEntityType(trackedEntityType);

    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder().trackedEntityType(trackedEntityType).build();

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(trackedEntityQueryParams)
            .commonRaw(new CommonRequestParams())
            .commonParsed(stubSortingCommonParams(program, 1, dimensionalObject))
            .build();

    String sql =
        sqlQueryCreatorService.getSqlQueryCreator(contextParams).createForSelect().getStatement();

    assertTrue(sql.contains(" order by (select (\"eventdatavalues\" -> 'abc' ->> 'value')::TEXT"));
  }

  @Test
  void testEnrolledInProgramWhenSpecifiedInRequest() {
    CommonParsedParams commonParsed =
        CommonParsedParams.builder()
            .programs(List.of(mockProgram("program1"), mockProgram("program2")))
            .build();

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setProgram(Set.of("program3"));
    requestParams.getInternal().setRequestPrograms(true);

    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder().trackedEntityType(createTrackedEntityType('A')).build();

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(trackedEntityQueryParams)
            .commonRaw(requestParams)
            .commonParsed(commonParsed)
            .build();

    String sql =
        sqlQueryCreatorService.getSqlQueryCreator(contextParams).createForSelect().getStatement();

    assertTrue(sql.contains("ouname"));
    assertContains("t_1.\"program1\" and t_1.\"program2\"", sql);
  }

  @Test
  void testEnrolledInProgramWhenNotSpecifiedInRequest() {
    CommonParsedParams commonParsed =
        CommonParsedParams.builder()
            .programs(List.of(mockProgram("program1"), mockProgram("program2")))
            .build();

    CommonRequestParams requestParams = new CommonRequestParams();

    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder().trackedEntityType(createTrackedEntityType('A')).build();

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(trackedEntityQueryParams)
            .commonParsed(commonParsed)
            .commonRaw(requestParams)
            .build();

    String sql =
        sqlQueryCreatorService.getSqlQueryCreator(contextParams).createForSelect().getStatement();

    assertTrue(sql.contains("ouname"));
    assertContains("(t_1.\"program1\" or t_1.\"program2\")", sql);
  }

  @Test
  void testGroupByFieldsArePropagatedToFinalQuery() {
    List<SqlQueryBuilder> buildersWithGroupBy = new ArrayList<>(queryBuilders);
    buildersWithGroupBy.add(new GroupByStubBuilder());
    SqlQueryCreatorService service = new SqlQueryCreatorService(buildersWithGroupBy);

    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder().trackedEntityType(createTrackedEntityType('A')).build();

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(trackedEntityQueryParams)
            .commonRaw(new CommonRequestParams())
            .commonParsed(stubSortingCommonParams(null, 1, "ouname"))
            .build();

    String sql = service.getSqlQueryCreator(contextParams).createForSelect().getStatement();

    assertContains("group by t_1.\"ou\"", sql);
  }

  @Test
  void testAggregateCountGroupedByOrgUnit() {
    List<SqlQueryBuilder> aggregateBuilders = new ArrayList<>();
    aggregateBuilders.add(new AggregateQueryBuilder());
    aggregateBuilders.addAll(queryBuilders);
    SqlQueryCreatorService service = new SqlQueryCreatorService(aggregateBuilders);

    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder()
            .trackedEntityType(createTrackedEntityType('A'))
            .aggregate(true)
            .build();

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setDimension(Set.of("ou"));

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(trackedEntityQueryParams)
            .commonRaw(requestParams)
            .commonParsed(
                CommonParsedParams.builder()
                    .dimensionIdentifiers(List.of(stubOuDimension("ou1")))
                    .build())
            .build();

    String sql = service.getSqlQueryCreator(contextParams).createForSelect().getStatement();

    assertContains("count(1) as \"value\"", sql);
    assertContains("group by t_1.\"ou\"", sql);
    assertFalse(
        sql.contains("enrollments"), "per-TEI columns must be suppressed in aggregate mode");
  }

  @Test
  void testAggregateCountGroupedByAttribute() {
    List<SqlQueryBuilder> aggregateBuilders = new ArrayList<>();
    aggregateBuilders.add(new AggregateQueryBuilder());
    aggregateBuilders.addAll(queryBuilders);
    SqlQueryCreatorService service = new SqlQueryCreatorService(aggregateBuilders);

    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder()
            .trackedEntityType(createTrackedEntityType('A'))
            .aggregate(true)
            .build();

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setDimension(Set.of("attr1"));

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(trackedEntityQueryParams)
            .commonRaw(requestParams)
            .commonParsed(
                CommonParsedParams.builder()
                    .dimensionIdentifiers(List.of(stubAttributeDimension("attr1")))
                    .build())
            .build();

    String sql = service.getSqlQueryCreator(contextParams).createForSelect().getStatement();

    assertContains("count(1) as \"value\"", sql);
    assertContains("group by t_1.\"attr1\"", sql);
  }

  @Test
  void testAggregateCountGroupedByOrgUnitAndAttribute() {
    List<SqlQueryBuilder> aggregateBuilders = new ArrayList<>();
    aggregateBuilders.add(new AggregateQueryBuilder());
    aggregateBuilders.addAll(queryBuilders);
    SqlQueryCreatorService service = new SqlQueryCreatorService(aggregateBuilders);

    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder()
            .trackedEntityType(createTrackedEntityType('A'))
            .aggregate(true)
            .build();

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setDimension(Set.of("ou", "attr1"));

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(trackedEntityQueryParams)
            .commonRaw(requestParams)
            .commonParsed(
                CommonParsedParams.builder()
                    .dimensionIdentifiers(
                        List.of(stubOuDimension("ou1"), stubAttributeDimension("attr1")))
                    .build())
            .build();

    String sql = service.getSqlQueryCreator(contextParams).createForSelect().getStatement();

    // every explicitly requested dimension becomes a group-by key; the value column is last
    // and not grouped.
    assertContains("select t_1.\"ou\", t_1.\"attr1\", count(1) as \"value\"", sql);
    assertContains("group by t_1.\"ou\", t_1.\"attr1\"", sql);
  }

  @Test
  void testAggregateAverageOverValueAttribute() {
    List<SqlQueryBuilder> aggregateBuilders = new ArrayList<>();
    aggregateBuilders.add(new AggregateQueryBuilder());
    aggregateBuilders.addAll(queryBuilders);
    SqlQueryCreatorService service = new SqlQueryCreatorService(aggregateBuilders);

    TrackedEntityAttribute valueAttribute = createTrackedEntityAttribute('V');
    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder()
            .trackedEntityType(createTrackedEntityType('A'))
            .aggregate(true)
            .value(valueAttribute)
            .aggregationType(AggregationType.AVERAGE)
            .build();

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setDimension(Set.of("ou"));

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(trackedEntityQueryParams)
            .commonRaw(requestParams)
            .commonParsed(
                CommonParsedParams.builder()
                    .dimensionIdentifiers(List.of(stubOuDimension("ou1")))
                    .build())
            .build();

    String sql = service.getSqlQueryCreator(contextParams).createForSelect().getStatement();

    assertContains("avg(t_1.\"" + valueAttribute.getUid() + "\") as \"value\"", sql);
    assertContains("group by t_1.\"ou\"", sql);
    assertFalse(sql.contains("count(1)"), "value aggregation must replace the count(1) column");
  }

  @Test
  void testAggregateCountOverValueAttributeCountsNonNullValues() {
    List<SqlQueryBuilder> aggregateBuilders = new ArrayList<>();
    aggregateBuilders.add(new AggregateQueryBuilder());
    aggregateBuilders.addAll(queryBuilders);
    SqlQueryCreatorService service = new SqlQueryCreatorService(aggregateBuilders);

    TrackedEntityAttribute valueAttribute = createTrackedEntityAttribute('V');
    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder()
            .trackedEntityType(createTrackedEntityType('A'))
            .aggregate(true)
            .value(valueAttribute)
            .aggregationType(AggregationType.COUNT)
            .build();

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setDimension(Set.of("ou"));

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(trackedEntityQueryParams)
            .commonRaw(requestParams)
            .commonParsed(
                CommonParsedParams.builder()
                    .dimensionIdentifiers(List.of(stubOuDimension("ou1")))
                    .build())
            .build();

    String sql = service.getSqlQueryCreator(contextParams).createForSelect().getStatement();

    assertContains("count(t_1.\"" + valueAttribute.getUid() + "\") as \"value\"", sql);
    assertFalse(sql.contains("count(1)"), "explicit COUNT over a value counts non-null values");
  }

  @Test
  void testAggregateGroupsByExplicitlyRequestedDimensionsOnly() {
    List<SqlQueryBuilder> aggregateBuilders = new ArrayList<>();
    aggregateBuilders.add(new AggregateQueryBuilder());
    aggregateBuilders.addAll(queryBuilders);
    SqlQueryCreatorService service = new SqlQueryCreatorService(aggregateBuilders);

    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder()
            .trackedEntityType(createTrackedEntityType('A'))
            .aggregate(true)
            .build();

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setDimension(Set.of("ou"));

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(trackedEntityQueryParams)
            .commonRaw(requestParams)
            .commonParsed(
                CommonParsedParams.builder()
                    .dimensionIdentifiers(
                        List.of(stubOuDimension("ou1"), stubAttributeDimension("attr1")))
                    .build())
            .build();

    String sql = service.getSqlQueryCreator(contextParams).createForSelect().getStatement();

    assertContains("count(1) as \"value\"", sql);
    assertContains("group by t_1.\"ou\"", sql);
    assertFalse(sql.contains("attr1"), "auto-injected attribute must not leak into the SQL");
  }

  private DimensionIdentifier<DimensionParam> stubAttributeDimension(String attribute) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new BaseDimensionalObject(attribute, DimensionType.PROGRAM_ATTRIBUTE, List.of()),
            DimensionParamType.DIMENSIONS,
            UID,
            List.of());
    return DimensionIdentifier.of(
            ElementWithOffset.emptyElementWithOffset(),
            ElementWithOffset.emptyElementWithOffset(),
            dimensionParam)
        .withDefaultGroupId();
  }

  private DimensionIdentifier<DimensionParam> stubOuDimension(String ou) {
    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid(ou);
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(orgUnit)),
            DimensionParamType.DIMENSIONS,
            UID,
            List.of(ou));
    return DimensionIdentifier.of(
            ElementWithOffset.emptyElementWithOffset(),
            ElementWithOffset.emptyElementWithOffset(),
            dimensionParam)
        .withDefaultGroupId();
  }

  /** Stub builder that contributes a group-by field, used to verify group-by propagation. */
  private static class GroupByStubBuilder implements SqlQueryBuilder {
    @Override
    public RenderableSqlQuery buildSqlQuery(
        QueryContext queryContext,
        List<DimensionIdentifier<DimensionParam>> acceptedHeaders,
        List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
        List<AnalyticsSortingParams> acceptedSortingParams) {
      return RenderableSqlQuery.builder().groupByField(Field.of("t_1", () -> "ou", "")).build();
    }

    @Override
    public boolean alwaysRun() {
      return true;
    }
  }

  private Program mockProgram(String uid) {
    Program program = mock(Program.class);
    when(program.getUid()).thenReturn(uid);
    return program;
  }

  private CommonParsedParams stubSortingCommonParams(
      Program program, int offset, Object dimensionalObject) {
    ElementWithOffset<Program> prg =
        program == null
            ? ElementWithOffset.emptyElementWithOffset()
            : ElementWithOffset.of(program, offset);

    ElementWithOffset<ProgramStage> programStage =
        program == null
            ? ElementWithOffset.emptyElementWithOffset()
            : ElementWithOffset.of(createProgramStage('S', program), offset);

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        DimensionIdentifier.of(
            prg,
            programStage,
            DimensionParam.ofObject(
                dimensionalObject, DimensionParamType.SORTING, UID, List.of(StringUtils.EMPTY)));

    AnalyticsSortingParams analyticsSortingParams =
        AnalyticsSortingParams.builder()
            .sortDirection(SortDirection.DESC)
            .orderBy(dimensionIdentifier)
            .build();

    AnalyticsPagingParams analyticsPagingParams =
        AnalyticsPagingParams.builder().pageSize(10).page(1).build();

    return CommonParsedParams.builder()
        .orderParams(List.of(analyticsSortingParams))
        .pagingParams(analyticsPagingParams)
        .build();
  }
}
