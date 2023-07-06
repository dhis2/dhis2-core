/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.util;

import static org.hisp.dhis.common.DimensionalObjectUtils.getKey;
import static org.hisp.dhis.common.DimensionalObjectUtils.getName;
import static org.hisp.dhis.common.DimensionalObjectUtils.getSortedKeysMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CombinationGenerator;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.system.grid.ListGrid;

/**
 * @author Lars Helge Overland
 */
public class GridRenderUtils {
  /**
   * Generates a grid according to the provided columns, rows and values.
   *
   * @param columns the columns.
   * @param rows the rows.
   * @param valueMap the values as a mapping between metadata key and value.
   * @return a grid.
   */
  public static Grid asGrid(
      List<? extends DimensionalObject> columns,
      List<? extends DimensionalObject> rows,
      Map<String, Object> valueMap) {
    List<List<DimensionalItemObject>> columnItems =
        columns.stream().map(DimensionalObject::getItems).collect(Collectors.toList());
    List<List<DimensionalItemObject>> rowItems =
        rows.stream().map(DimensionalObject::getItems).collect(Collectors.toList());

    List<List<DimensionalItemObject>> gridColumns =
        CombinationGenerator.newInstance(columnItems).getCombinations();
    List<List<DimensionalItemObject>> gridRows =
        CombinationGenerator.newInstance(rowItems).getCombinations();

    Map<String, Object> internalValueMap = getSortedKeysMap(valueMap);

    Grid grid = new ListGrid();

    // ---------------------------------------------------------------------
    // Headers
    // ---------------------------------------------------------------------

    for (DimensionalObject object : rows) {
      grid.addHeader(new GridHeader(object.getDimension(), object.getDimensionDisplayName()));
    }

    for (List<DimensionalItemObject> column : gridColumns) {
      grid.addHeader(new GridHeader(getKey(column), getName(column)));
    }

    // ---------------------------------------------------------------------
    // Rows
    // ---------------------------------------------------------------------

    for (List<DimensionalItemObject> row : gridRows) {
      grid.addRow();

      for (DimensionalItemObject object : row) {
        grid.addValue(object.getDisplayName());
      }

      for (List<DimensionalItemObject> column : gridColumns) {
        String key = getKey(column, row);
        Object value = internalValueMap.get(key);
        grid.addValue(value);
      }
    }

    return grid;
  }
}
