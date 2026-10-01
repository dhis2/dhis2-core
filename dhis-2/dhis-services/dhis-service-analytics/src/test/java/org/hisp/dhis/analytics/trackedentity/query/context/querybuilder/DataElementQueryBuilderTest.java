/*
 * Copyright (c) 2004-2023, University of Oslo
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlParameterManager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class DataElementQueryBuilderTest {

  private static final String PROGRAM_UID = "IpHINAT79UW";

  private static final String PROGRAM_STAGE_UID = "A03MvHHogjR";

  private static final String TET_UID = "nEenWmSyUEp";

  private static final String DATA_ELEMENT_UID = "UXz7xuGCEhU";

  private final DataElementQueryBuilder dataElementQueryBuilder = new DataElementQueryBuilder();

  @Test
  void testBuildSqlQuery() {
    DimensionIdentifier<DimensionParam> dWithLegendSet = mock(DimensionIdentifier.class);
    DimensionParam dimensionForLegendSet = mock(DimensionParam.class);
    when(dWithLegendSet.getDimension()).thenReturn(dimensionForLegendSet);
    when(dWithLegendSet.hasLegendSet()).thenReturn(true);
    QueryItem queryItem = mock(QueryItem.class);
    when(dimensionForLegendSet.getQueryItem()).thenReturn(queryItem);
    LegendSet legendSet = mock(LegendSet.class);
    when(queryItem.getLegendSet()).thenReturn(legendSet);
    when(dWithLegendSet.getKey()).thenReturn("key");

    DimensionIdentifier<DimensionParam> dWithNoLegendSet = mock(DimensionIdentifier.class);
    DimensionParam dimensionForNoLegendSet = mock(DimensionParam.class);
    when(dWithNoLegendSet.getDimension()).thenReturn(dimensionForNoLegendSet);
    when(dWithNoLegendSet.hasLegendSet()).thenReturn(false);
    when(dWithNoLegendSet.getKey()).thenReturn("key");

    QueryContext queryContext =
        QueryContext.of(mock(ContextParams.class), mock(SqlParameterManager.class));
    List<DimensionIdentifier<DimensionParam>> acceptedHeaders = List.of(dWithNoLegendSet);
    List<DimensionIdentifier<DimensionParam>> acceptedDimensions = List.of(dWithLegendSet);
    List<AnalyticsSortingParams> acceptedSortingParams = List.of();
    RenderableSqlQuery renderableSqlQuery =
        dataElementQueryBuilder.buildSqlQuery(
            queryContext, acceptedHeaders, acceptedDimensions, acceptedSortingParams);

    // when 2 DE with the same key are referenced, one of them with legendSet and one without,
    // then 5 select fields should be added to the query:
    // 1. The DE with legendSet
    // 2. The DE without legendSet (the "raw" value)
    // 3. The "exists" field
    // 4. the "status" field
    // 5. the "hasValue" field
    assertEquals(2, renderableSqlQuery.getSelectFields().size());
  }

  /**
   * A data element the aggregate query groups by is already restricted and ordered by its group by
   * expression, so this builder must not emit a second restriction or a second ordering for it.
   */
  @Test
  void restrictedDataElementOutsideAnAggregateProducesACondition() {
    DimensionIdentifier<DimensionParam> dataElement = stageDataElement("value1");

    RenderableSqlQuery query =
        dataElementQueryBuilder.buildSqlQuery(
            rowLevelContext(), List.of(), List.of(dataElement), List.of());

    assertEquals(1, query.getGroupableConditions().size());
    String rendered = query.getGroupableConditions().get(0).getRenderable().render();
    assertTrue(
        rendered.contains(DATA_ELEMENT_UID),
        "expected the data element to be restricted, got " + rendered);
  }

  @Test
  void groupedDataElementProducesNoCondition() {
    DimensionIdentifier<DimensionParam> dataElement = stageDataElement("value1");

    RenderableSqlQuery query =
        dataElementQueryBuilder.buildSqlQuery(
            aggregateContextGroupingBy(dataElement), List.of(), List.of(dataElement), List.of());

    assertEquals(
        List.of(),
        query.getGroupableConditions(),
        "a grouped data element is already restricted by the group by expression");
  }

  @Test
  void sortingOnADataElementOutsideAnAggregateProducesAnOrderClause() {
    DimensionIdentifier<DimensionParam> dataElement = stageDataElement();

    RenderableSqlQuery query =
        dataElementQueryBuilder.buildSqlQuery(
            rowLevelContext(), List.of(), List.of(), List.of(ascendingSortOn(dataElement)));

    assertEquals(1, query.getOrderClauses().size());
    String rendered = query.getOrderClauses().get(0).getRenderable().render();
    assertTrue(
        rendered.contains(DATA_ELEMENT_UID),
        "expected the data element to be ordered on, got " + rendered);
  }

  @Test
  void sortingOnAGroupedDataElementProducesNoOrderClause() {
    DimensionIdentifier<DimensionParam> dataElement = stageDataElement();

    RenderableSqlQuery query =
        dataElementQueryBuilder.buildSqlQuery(
            aggregateContextGroupingBy(dataElement),
            List.of(),
            List.of(),
            List.of(ascendingSortOn(dataElement)));

    assertEquals(
        List.of(),
        query.getOrderClauses(),
        "a grouped data element is already ordered by the group by expression");
  }

  private AnalyticsSortingParams ascendingSortOn(DimensionIdentifier<DimensionParam> dimension) {
    return AnalyticsSortingParams.builder()
        .index(0)
        .orderBy(dimension)
        .sortDirection(SortDirection.ASC)
        .build();
  }

  private DimensionIdentifier<DimensionParam> stageDataElement(String... restrictions) {
    DataElement dataElement = new DataElement();
    dataElement.setUid(DATA_ELEMENT_UID);
    dataElement.setValueType(ValueType.TEXT);

    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new QueryItem(dataElement), DimensionParamType.DIMENSIONS, UID, List.of(restrictions));

    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid(TET_UID);

    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setTrackedEntityType(trackedEntityType);

    ProgramStage programStage = new ProgramStage();
    programStage.setUid(PROGRAM_STAGE_UID);
    programStage.setProgram(program);

    return DimensionIdentifier.of(
        ElementWithOffset.of(program), ElementWithOffset.of(programStage), dimensionParam);
  }

  private QueryContext rowLevelContext() {
    return QueryContext.of(
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(TrackedEntityQueryParams.builder().build())
            .commonRaw(new CommonRequestParams())
            .commonParsed(CommonParsedParams.builder().build())
            .build(),
        new SqlParameterManager());
  }

  private QueryContext aggregateContextGroupingBy(DimensionIdentifier<DimensionParam> dimension) {
    return QueryContext.of(
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(TrackedEntityQueryParams.builder().aggregate(true).build())
            .commonRaw(new CommonRequestParams().withDimension(Set.of(dimension.getKey())))
            .commonParsed(
                CommonParsedParams.builder().dimensionIdentifiers(List.of(dimension)).build())
            .build(),
        new SqlParameterManager());
  }
}
