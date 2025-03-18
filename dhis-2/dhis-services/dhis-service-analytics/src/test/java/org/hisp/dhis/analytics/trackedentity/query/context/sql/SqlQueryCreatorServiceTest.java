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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.DataElementQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.EnrolledInProgramQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.LimitOffsetQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.MainTableQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.OrgUnitQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.PeriodQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.ProgramIndicatorQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.TrackedEntityQueryBuilder;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.TestBase;
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

  @BeforeEach
  void setUp() {
    ProgramIndicatorService programIndicatorService = mock(ProgramIndicatorService.class);
    List<SqlQueryBuilder> queryBuilders =
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
