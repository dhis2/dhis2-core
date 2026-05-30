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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createLegend;
import static org.hisp.dhis.test.TestBase.createLegendSet;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.analytics.EventAnalyticsDimensionalItem;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.Test;

class EventAggregateServiceTest {
  private final EventAggregateService service =
      new EventAggregateService(null, null, null, null, null, null, null, null, null, null, null);

  @Test
  void shouldFallBackToAllLegendsWhenDimensionsMetadataOmitsLegendSetDataElement()
      throws Exception {
    Legend legendA = createLegend('A', 0.0, 2.0);
    Legend legendB = createLegend('B', 2.0, 4.0);
    LegendSet legendSet = createLegendSet('A', legendA, legendB);

    DataElement dataElement = createDataElement('A');
    dataElement.setValueType(ValueType.NUMBER);
    dataElement.setLegendSets(List.of(legendSet));

    Grid grid = new ListGrid();
    grid.getMetaData().put(AnalyticsMetaDataKey.DIMENSIONS.getKey(), new HashMap<String, Object>());

    List<EventAnalyticsDimensionalItem> dimensionalItems = new ArrayList<>();

    invokeAddEventReportDimensionalItems(dataElement, dimensionalItems, grid, dataElement.getUid());

    assertFalse(dimensionalItems.isEmpty());
  }

  @Test
  void shouldNotNpeWhenItemsMetadataOmitsRowDimension() throws Exception {
    String missingDimensionUid = "deUidAAAAA";

    Grid grid = new ListGrid();
    grid.getMetaData().put(AnalyticsMetaDataKey.ITEMS.getKey(), new HashMap<String, Object>());

    EventQueryParams params = new EventQueryParams.Builder().build();

    invokeGenerateOutputGrid(grid, params, List.of(), List.of(), List.of(missingDimensionUid));
  }

  private void invokeAddEventReportDimensionalItems(
      ValueTypedDimensionalItemObject item,
      List<EventAnalyticsDimensionalItem> dimensionalItems,
      Grid grid,
      String dimension)
      throws Exception {
    Method method =
        EventAggregateService.class.getDeclaredMethod(
            "addEventReportDimensionalItems",
            ValueTypedDimensionalItemObject.class,
            List.class,
            Grid.class,
            String.class);
    method.setAccessible(true);
    method.invoke(service, item, dimensionalItems, grid, dimension);
  }

  private Grid invokeGenerateOutputGrid(
      Grid grid,
      EventQueryParams params,
      List<Map<String, EventAnalyticsDimensionalItem>> rowPermutations,
      List<Map<String, EventAnalyticsDimensionalItem>> columnPermutations,
      List<String> rowDimensions)
      throws Exception {
    Method method =
        EventAggregateService.class.getDeclaredMethod(
            "generateOutputGrid",
            Grid.class,
            EventQueryParams.class,
            List.class,
            List.class,
            List.class);
    method.setAccessible(true);
    return (Grid)
        method.invoke(service, grid, params, rowPermutations, columnPermutations, rowDimensions);
  }
}
