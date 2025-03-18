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
package org.hisp.dhis.analytics.tracker;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static lombok.AccessLevel.PRIVATE;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.getItemUid;
import static org.hisp.dhis.common.ValueType.COORDINATE;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.ValueType.TEXT;

import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RepeatableStageParams;

@NoArgsConstructor(access = PRIVATE)
public class HeaderHelper {
  public static void addCommonHeaders(
      Grid grid, EventQueryParams params, List<DimensionalObject> periods) {

    for (DimensionalObject dimension : params.getDimensions()) {
      grid.addHeader(
          new GridHeader(
              dimension.getDimension(), dimension.getDimensionDisplayName(), TEXT, false, true));
    }

    for (DimensionalObject dimension : periods) {
      grid.addHeader(
          new GridHeader(
              dimension.getDimension(), dimension.getDimensionDisplayName(), TEXT, false, true));
    }

    DisplayProperty displayProperty = params.getDisplayProperty();
    Map<String, Long> repeatedNames =
        params.getItems().stream()
            .collect(groupingBy(s -> s.getItem().getDisplayProperty(displayProperty), counting()));

    for (QueryItem item : params.getItems()) {
      /**
       * If the request contains an item of value type ORGANISATION_UNIT and the item UID is linked
       * to coordinates (coordinateField), then create header of value type COORDINATE and type
       * Point.
       */
      if (item.getValueType() == ORGANISATION_UNIT
          && params.getCoordinateFields().stream()
              .anyMatch(f -> f.equals(item.getItem().getUid()))) {
        grid.addHeader(
            new GridHeader(
                item.getItem().getUid(),
                item.getItem().getDisplayProperty(displayProperty),
                COORDINATE,
                false,
                true,
                item.getOptionSet(),
                item.getLegendSet()));
      } else if (item.hasNonDefaultRepeatableProgramStageOffset()) {
        String column = item.getItem().getDisplayProperty(displayProperty);
        String displayColumn = item.getColumnName(displayProperty, repeatedNames.get(column) > 1);

        RepeatableStageParams repeatableStageParams = item.getRepeatableStageParams();

        String name = repeatableStageParams.getDimension();

        grid.addHeader(
            new GridHeader(
                name,
                column,
                displayColumn,
                item.getValueType(),
                false,
                true,
                item.getOptionSet(),
                item.getLegendSet(),
                item.getProgramStage().getUid(),
                item.getRepeatableStageParams()));
      } else {
        String uid = getItemUid(item);
        String column = item.getItem().getDisplayProperty(displayProperty);
        String displayColumn = item.getColumnName(displayProperty, repeatedNames.get(column) > 1);

        grid.addHeader(
            new GridHeader(
                uid,
                column,
                displayColumn,
                item.getValueType(),
                false,
                true,
                item.getOptionSet(),
                item.getLegendSet()));
      }
    }
  }
}
