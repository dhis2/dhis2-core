/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.analytics.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.junit.jupiter.api.Test;

class DefaultAnalyticsServiceTest {

  @Test
  void testGetGridItems() {

    Grid grid = mock(Grid.class);

    when(grid.getWidth()).thenReturn(6);
    when(grid.getRows())
        .thenReturn(
            List.of(
                List.of("R-A1", "R-B1", "R-C2", "C-A1", "C-B1", "v1"),
                List.of("R-A1", "R-B2", "R-C1", "C-A1", "C-B3", "v2"),
                List.of("R-A2", "R-B1", "R-C1", "C-A2", "C-B1", "v3")));
    when(grid.getHeaders())
        .thenReturn(
            List.of(
                new GridHeader("R-A", false, false),
                new GridHeader("R-B", false, false),
                new GridHeader("R-C", false, false),
                new GridHeader("C-A", false, false),
                new GridHeader("C-B", false, false)));

    Map<String, List<DimensionalItemObject>> dimensionItemsByDimension =
        Map.of(
            "R-A",
                List.of(
                    new BaseDimensionalItemObject("R-A1"),
                    new BaseDimensionalItemObject("R-A2"),
                    new BaseDimensionalItemObject("R-A3")),
            "R-B",
                List.of(
                    new BaseDimensionalItemObject("R-B1"),
                    new BaseDimensionalItemObject("R-B2"),
                    new BaseDimensionalItemObject("R-B3")),
            "R-C",
                List.of(
                    new BaseDimensionalItemObject("R-C1"),
                    new BaseDimensionalItemObject("R-C2"),
                    new BaseDimensionalItemObject("R-C3")),
            "C-A",
                List.of(
                    new BaseDimensionalItemObject("C-A1"),
                    new BaseDimensionalItemObject("C-A2"),
                    new BaseDimensionalItemObject("C-A3")),
            "C-B",
                List.of(
                    new BaseDimensionalItemObject("C-B1"),
                    new BaseDimensionalItemObject("C-B2"),
                    new BaseDimensionalItemObject("C-B3")));

    List<String> dimensionIds = List.of("R-A", "R-B", "R-C", "C-A", "C-B");

    List<List<DimensionalItemObject>> gridItems =
        DefaultAnalyticsService.getGridItems(grid, dimensionItemsByDimension, dimensionIds);

    assertEquals(grid.getRows().size(), gridItems.size());

    for (int i = 0; i < grid.getRows().size(); i++) {
      assertEquals(grid.getWidth() - 1, gridItems.get(i).size());
    }

    Set<String> fromGrid =
        grid.getRows().stream()
            .map(
                row ->
                    row.stream()
                        .limit(row.size() - 1)
                        .map(Object::toString)
                        .collect(Collectors.joining()))
            .collect(Collectors.toSet());

    Set<String> fromGridItems =
        gridItems.stream()
            .map(
                row ->
                    row.stream()
                        .map(DimensionalItemObject::getDimensionItem)
                        .collect(Collectors.joining()))
            .collect(Collectors.toSet());

    assertEquals(fromGridItems, fromGrid);
  }
}
