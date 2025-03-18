/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.hisp.dhis.analytics.common.params.dimension.AnalyticsQueryOperator;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamItem;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlParameterManager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class DataElementConditionTest {

  private DimensionIdentifier<DimensionParam> dimensionIdentifier;

  @BeforeEach
  public void setUp() {
    dimensionIdentifier = mock(DimensionIdentifier.class);
  }

  @Test
  void testWhenNoLegendSet() {
    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);

    DimensionParam dimensionParam = mock(DimensionParam.class);
    QueryItem queryItem = mock(QueryItem.class);
    DimensionParamItem dimensionParamItem = mock(DimensionParamItem.class);
    when(dimensionIdentifier.getDimension()).thenReturn(dimensionParam);

    when(dimensionParam.getQueryItem()).thenReturn(queryItem);
    when(dimensionParam.getUid()).thenReturn("uid");
    when(queryItem.hasLegendSet()).thenReturn(false);

    when(dimensionParam.getValueType()).thenReturn(ValueType.TEXT);

    when(dimensionParam.getItems()).thenReturn(List.of(dimensionParamItem));
    when(dimensionParamItem.getOperator()).thenReturn(AnalyticsQueryOperator.of(QueryOperator.EQ));
    when(dimensionParamItem.getValues()).thenReturn(List.of("value"));

    DataElementCondition dataElementCondition =
        DataElementCondition.of(queryContext, dimensionIdentifier);

    String rendered = dataElementCondition.render();

    assertEquals("(\"eventdatavalues\" -> 'uid' ->> 'value')::TEXT = :1", rendered);
    assertEquals("value", queryContext.getParametersPlaceHolder().get("1"));
  }

  @Test
  void testWhenLegendSet() {
    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);

    DimensionParam dimensionParam = mock(DimensionParam.class);
    QueryItem queryItem = mock(QueryItem.class);
    DimensionParamItem dimensionParamItem = mock(DimensionParamItem.class);
    when(dimensionIdentifier.hasLegendSet()).thenReturn(true);
    when(dimensionIdentifier.getDimension()).thenReturn(dimensionParam);

    when(dimensionParam.getQueryItem()).thenReturn(queryItem);
    when(dimensionParam.getUid()).thenReturn("uid");
    when(queryItem.hasLegendSet()).thenReturn(true);

    when(dimensionParam.getValueType()).thenReturn(ValueType.TEXT);

    when(dimensionParam.getItems()).thenReturn(List.of(dimensionParamItem));
    when(dimensionParamItem.getOperator()).thenReturn(AnalyticsQueryOperator.of(QueryOperator.IN));
    when(dimensionParamItem.getValues()).thenReturn(List.of("value"));

    LegendSet legendSet = mock(LegendSet.class);
    when(queryItem.getLegendSet()).thenReturn(legendSet);

    Legend legend = mock(Legend.class);
    when(legendSet.getLegendByUid("value")).thenReturn(legend);

    when(legend.getStartValue()).thenReturn(1.0);
    when(legend.getEndValue()).thenReturn(2.0);

    DataElementCondition dataElementCondition =
        DataElementCondition.of(queryContext, dimensionIdentifier);

    String rendered = dataElementCondition.render();

    assertEquals(
        "(\"eventdatavalues\" -> 'uid' ->> 'value')::DECIMAL >= :1 "
            + "and (\"eventdatavalues\" -> 'uid' ->> 'value')::DECIMAL < :2",
        rendered);
    assertEquals(new BigDecimal("1.0"), queryContext.getParametersPlaceHolder().get("1"));
    assertEquals(new BigDecimal("2.0"), queryContext.getParametersPlaceHolder().get("2"));
  }
}
