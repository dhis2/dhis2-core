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
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;
import static org.hisp.dhis.analytics.event.data.OrganisationUnitResolver.isStageOuDimension;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.getItemUid;
import static org.hisp.dhis.common.ValueType.COORDINATE;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.ValueType.TEXT;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;

@NoArgsConstructor(access = PRIVATE)
public class HeaderHelper {
  public static void addCommonHeaders(
      Grid grid, EventQueryParams params, List<DimensionalObject> periods) {

    addDimensionHeaders(grid, params.getDimensions());
    addDimensionHeaders(grid, periods);

    DisplayProperty displayProperty = params.getDisplayProperty();
    HeaderBuildContext context = HeaderBuildContext.of(params, displayProperty);

    for (QueryItem item : params.getItems()) {
      grid.addHeader(buildGridHeader(item, context));

      if (isStageOuDimension(item) && params.hasHeaders()) {
        String stageUid = item.getProgramStage().getUid();

        if (params.getHeaders().contains(stageUid + ".ouname")) {
          grid.addHeader(
              new GridHeader(
                  stageUid + ".ouname",
                  "Organisation unit name",
                  "Organisation unit name",
                  TEXT,
                  false,
                  true,
                  null,
                  null));
        }

        if (params.getHeaders().contains(stageUid + ".oucode")) {
          grid.addHeader(
              new GridHeader(
                  stageUid + ".oucode",
                  "Organisation unit code",
                  "Organisation unit code",
                  TEXT,
                  false,
                  true,
                  null,
                  null));
        }
      }
    }
  }

  private static void addDimensionHeaders(Grid grid, List<DimensionalObject> dimensions) {
    for (DimensionalObject dimension : dimensions) {
      grid.addHeader(
          new GridHeader(
              dimension.getDimension(), dimension.getDimensionDisplayName(), TEXT, false, true));
    }
  }

  private static GridHeader buildGridHeader(QueryItem item, HeaderBuildContext context) {
    if (isCoordinateHeader(item, context.coordinateFields())) {
      return toGridHeader(buildCoordinateHeaderSpec(item, context.displayProperty()));
    }

    if (item.hasNonDefaultRepeatableProgramStageOffset()) {
      return toGridHeader(buildRepeatableStageHeaderSpec(item, context));
    }

    return toGridHeader(buildDefaultHeaderSpec(item, context));
  }

  private static boolean isCoordinateHeader(QueryItem item, Set<String> coordinateFields) {
    return item.getValueType() == ORGANISATION_UNIT
        && coordinateFields.contains(item.getItem().getUid());
  }

  private static HeaderSpec buildCoordinateHeaderSpec(
      QueryItem item, DisplayProperty displayProperty) {
    return new HeaderSpec(
        item.getItem().getUid(),
        item.getItem().getDisplayProperty(displayProperty),
        null,
        COORDINATE,
        item.getOptionSet(),
        item.getLegendSet(),
        null,
        null);
  }

  private static HeaderSpec buildRepeatableStageHeaderSpec(
      QueryItem item, HeaderBuildContext context) {
    String column = item.getItem().getDisplayProperty(context.displayProperty());
    long repeatedCount = context.repeatedNames().getOrDefault(column, 0L);
    String displayColumn = item.getColumnName(context.displayProperty(), repeatedCount > 1);

    RepeatableStageParams repeatableStageParams = item.getRepeatableStageParams();

    return new HeaderSpec(
        repeatableStageParams.getDimension(),
        column,
        displayColumn,
        item.getValueType(),
        item.getOptionSet(),
        item.getLegendSet(),
        item.getProgramStage().getUid(),
        repeatableStageParams);
  }

  private static HeaderSpec buildDefaultHeaderSpec(QueryItem item, HeaderBuildContext context) {
    String uid =
        item.hasCustomHeader()
            ? item.getCustomHeader().headerKey(item.getCustomHeader().key())
            : getItemUid(item);
    String column =
        item.hasCustomHeader()
            ? item.getCustomHeader().label()
            : item.getItem().getDisplayProperty(context.displayProperty());
    String repeatedNamesKey =
        item.hasCustomHeader()
            ? item.getCustomHeader().label()
            : item.getItem().getDisplayProperty(context.displayProperty());
    long repeatedCount = context.repeatedNames().getOrDefault(repeatedNamesKey, 0L);
    String displayColumn = item.getColumnName(context.displayProperty(), repeatedCount > 1);

    return new HeaderSpec(
        uid,
        column,
        displayColumn,
        item.getValueType(),
        item.getOptionSet(),
        item.getLegendSet(),
        null,
        null);
  }

  private static GridHeader toGridHeader(HeaderSpec spec) {
    if (spec.programStage() != null && spec.repeatableStageParams() != null) {
      if (spec.displayColumn() != null) {
        return new GridHeader(
            spec.name(),
            spec.column(),
            spec.displayColumn(),
            spec.valueType(),
            false,
            true,
            spec.optionSet(),
            spec.legendSet(),
            spec.programStage(),
            spec.repeatableStageParams());
      }

      return new GridHeader(
          spec.name(),
          spec.column(),
          spec.valueType(),
          false,
          true,
          spec.optionSet(),
          spec.legendSet(),
          spec.programStage(),
          spec.repeatableStageParams());
    }

    if (spec.displayColumn() != null) {
      return new GridHeader(
          spec.name(),
          spec.column(),
          spec.displayColumn(),
          spec.valueType(),
          false,
          true,
          spec.optionSet(),
          spec.legendSet());
    }

    return new GridHeader(
        spec.name(),
        spec.column(),
        spec.valueType(),
        false,
        true,
        spec.optionSet(),
        spec.legendSet());
  }

  private record HeaderBuildContext(
      DisplayProperty displayProperty,
      Map<String, Long> repeatedNames,
      Set<String> coordinateFields) {
    private static HeaderBuildContext of(EventQueryParams params, DisplayProperty displayProperty) {
      Map<String, Long> repeatedNames =
          params.getItems().stream()
              .collect(
                  groupingBy(s -> s.getItem().getDisplayProperty(displayProperty), counting()));

      Set<String> coordinateFields =
          params.getCoordinateFields() == null
              ? Collections.emptySet()
              : params.getCoordinateFields().stream().collect(toSet());

      return new HeaderBuildContext(displayProperty, repeatedNames, coordinateFields);
    }
  }

  private record HeaderSpec(
      String name,
      String column,
      String displayColumn,
      ValueType valueType,
      OptionSet optionSet,
      LegendSet legendSet,
      String programStage,
      RepeatableStageParams repeatableStageParams) {}
}
