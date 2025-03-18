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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlParameterManager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.legend.LegendSet;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class DataElementQueryBuilderTest {

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
}
