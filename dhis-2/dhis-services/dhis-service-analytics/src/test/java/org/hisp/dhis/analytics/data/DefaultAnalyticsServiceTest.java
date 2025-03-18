/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.Test;

class DefaultAnalyticsServiceTest {

  @Test
  void testGetGridItems() {
    Grid grid = new ListGrid();

    List.of(
            new GridHeader("R-A", false, false),
            new GridHeader("R-B", false, false),
            new GridHeader("R-C", false, false),
            new GridHeader("C-A", false, false),
            new GridHeader("C-B", false, false))
        .forEach(grid::addHeader);

    List.of(
            List.of("R-A1", "R-B1", "R-C2", "C-A1", "C-B1", "v1"),
            List.of("R-A1", "R-B2", "R-C1", "C-A1", "C-B3", "v2"),
            List.of("R-A2", "R-B1", "R-C1", "C-A2", "C-B1", "v3"))
        .forEach(
            row -> {
              grid.addRow();
              row.forEach(grid::addValue);
            });

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

    Set<List<String>> rowsFromGrid = getRowsFromGrid(grid);
    Set<List<String>> rowsFromGridItems = getRowsFromGridItems(gridItems);

    assertEquals(rowsFromGrid, rowsFromGridItems);
  }

  private Set<List<String>> getRowsFromGrid(Grid grid) {
    Set<List<String>> rowsFromGrid = new HashSet<>();
    for (List<Object> gridRow : grid.getRows()) {
      List<String> gridRowAsString = new ArrayList<>();
      for (int j = 0; j < gridRow.size() - 1; j++) {
        gridRowAsString.add(gridRow.get(j).toString());
      }
      rowsFromGrid.add(gridRowAsString);
    }
    return rowsFromGrid;
  }

  private Set<List<String>> getRowsFromGridItems(List<List<DimensionalItemObject>> gridItems) {
    Set<List<String>> rowsFromGridItems = new HashSet<>();
    for (List<DimensionalItemObject> gridItem : gridItems) {
      List<String> gridItemAsString = new ArrayList<>();
      for (DimensionalItemObject dimensionalItemObject : gridItem) {
        gridItemAsString.add(dimensionalItemObject.getDimensionItem());
      }
      rowsFromGridItems.add(gridItemAsString);
    }
    return rowsFromGridItems;
  }
}
