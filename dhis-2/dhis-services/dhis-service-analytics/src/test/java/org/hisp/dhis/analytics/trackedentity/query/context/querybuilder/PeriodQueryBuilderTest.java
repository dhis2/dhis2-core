/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.query.context.querybuilder;

import static org.hisp.dhis.common.IdScheme.UID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlParameterManager;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests how {@link PeriodQueryBuilder} treats a date dimension the aggregate query already groups
 * by. The grouped expression buckets the date and carries the restriction, so repeating either the
 * restriction or the ordering here would filter or sort the rows a second time.
 */
class PeriodQueryBuilderTest {

  private static final String PROGRAM_UID = "IpHINAT79UW";

  private static final String TET_UID = "nEenWmSyUEp";

  private PeriodQueryBuilder builder;

  private TrackedEntityType trackedEntityType;

  @BeforeEach
  void setUp() {
    builder = new PeriodQueryBuilder();
    trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid(TET_UID);
  }

  @Test
  void restrictedEnrollmentDateOutsideAnAggregateProducesACondition() {
    DimensionIdentifier<DimensionParam> enrollmentDate = enrollmentDate("2022");

    RenderableSqlQuery query =
        builder.buildSqlQuery(rowLevelContext(), List.of(), List.of(enrollmentDate), List.of());

    assertEquals(1, query.getGroupableConditions().size());
    String rendered = query.getGroupableConditions().get(0).getRenderable().render();
    assertTrue(
        rendered.contains("enrollmentdate"),
        "expected the enrollment date column to be restricted, got " + rendered);
  }

  @Test
  void groupedEnrollmentDateProducesNoCondition() {
    DimensionIdentifier<DimensionParam> enrollmentDate = enrollmentDate("2022");

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            aggregateContextGroupingBy(enrollmentDate),
            List.of(),
            List.of(enrollmentDate),
            List.of());

    assertEquals(
        List.of(),
        query.getGroupableConditions(),
        "a grouped date is already restricted by the group by expression");
  }

  @Test
  void sortingOnAnEnrollmentDateOutsideAnAggregateProducesAnOrderClause() {
    DimensionIdentifier<DimensionParam> enrollmentDate = enrollmentDate();

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            rowLevelContext(), List.of(), List.of(), List.of(ascendingSortOn(enrollmentDate)));

    assertEquals(1, query.getOrderClauses().size());
    String rendered = query.getOrderClauses().get(0).getRenderable().render();
    assertTrue(
        rendered.contains("enrollmentdate"),
        "expected the enrollment date column to be ordered on, got " + rendered);
  }

  @Test
  void sortingOnAGroupedEnrollmentDateProducesNoOrderClause() {
    DimensionIdentifier<DimensionParam> enrollmentDate = enrollmentDate();

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            aggregateContextGroupingBy(enrollmentDate),
            List.of(),
            List.of(),
            List.of(ascendingSortOn(enrollmentDate)));

    assertEquals(
        List.of(),
        query.getOrderClauses(),
        "a grouped date is already ordered by the group by expression");
  }

  private AnalyticsSortingParams ascendingSortOn(DimensionIdentifier<DimensionParam> dimension) {
    return AnalyticsSortingParams.builder()
        .index(0)
        .orderBy(dimension)
        .sortDirection(SortDirection.ASC)
        .build();
  }

  /** An enrollment scoped enrollment date, which is what this builder accepts. */
  private DimensionIdentifier<DimensionParam> enrollmentDate(String... restrictions) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            StaticDimension.ENROLLMENTDATE.name(),
            DimensionParamType.DIMENSIONS,
            UID,
            List.of(restrictions));

    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setTrackedEntityType(trackedEntityType);

    return DimensionIdentifier.of(
        ElementWithOffset.of(program), ElementWithOffset.emptyElementWithOffset(), dimensionParam);
  }

  private QueryContext rowLevelContext() {
    return QueryContext.of(
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(
                TrackedEntityQueryParams.builder().trackedEntityType(trackedEntityType).build())
            .commonRaw(new CommonRequestParams())
            .commonParsed(CommonParsedParams.builder().build())
            .build(),
        new SqlParameterManager());
  }

  private QueryContext aggregateContextGroupingBy(DimensionIdentifier<DimensionParam> dimension) {
    return QueryContext.of(
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(
                TrackedEntityQueryParams.builder()
                    .aggregate(true)
                    .trackedEntityType(trackedEntityType)
                    .build())
            .commonRaw(new CommonRequestParams().withDimension(Set.of(dimension.getKey())))
            .commonParsed(
                CommonParsedParams.builder().dimensionIdentifiers(List.of(dimension)).build())
            .build(),
        new SqlParameterManager());
  }
}
