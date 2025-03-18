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
package org.hisp.dhis.analytics.common;

import static java.lang.String.format;
import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.jsonextractor.SqlRowSetJsonExtractorDelegator;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.jdbc.support.rowset.SqlRowSet;

@Slf4j
public class TrackedEntityListGrid extends ListGrid {

  private static final List<ValueType> ROUNDABLE_TYPES =
      List.of(ValueType.PERCENTAGE, ValueType.NUMBER);

  @JsonIgnore
  private final transient ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>
      contextParams;

  @JsonIgnore private final transient CommonRequestParams commonRequestParams;
  @JsonIgnore private final transient CommonParsedParams commonParsedParams;

  public TrackedEntityListGrid(
      @Nonnull ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    super();
    this.contextParams = contextParams;
    this.commonRequestParams = contextParams.getCommonRaw();
    this.commonParsedParams = contextParams.getCommonParsed();
  }

  /**
   * Adds named rows to the grid based on the given result set. The column names are used to
   * identify the grid headers and the row values are added to the grid. The method also adds row
   * context content that describes the origin of the data value, indicating whether it is set, not
   * set, undefined or scheduled.
   *
   * @param rs the {@link ResultSet}
   * @return the {@link Grid} object
   */
  public Grid addNamedRows(SqlRowSet rs) {
    String[] cols = getHeaders().stream().map(GridHeader::getName).toArray(String[]::new);
    Set<String> headersSet = new LinkedHashSet<>();
    Map<Integer, Map<String, Object>> rowContext = new HashMap<>();

    while (rs.next()) {
      addRow();
      Map<String, Object> rowContextItems = new HashMap<>();

      for (int i = 0; i < cols.length; i++) {
        if (headerExists(cols[i])) {
          String columnLabel = cols[i];

          boolean columnHasLegendSet =
              commonParsedParams
                  .streamDimensions()
                  .filter(DimensionIdentifier::hasLegendSet)
                  .map(DimensionIdentifier::getKey)
                  .anyMatch(columnLabel::equals);

          boolean columnHasOptionSet =
              commonParsedParams
                  .streamDimensions()
                  .filter(DimensionIdentifier::hasOptionSet)
                  .map(DimensionIdentifier::getKey)
                  .anyMatch(columnLabel::equals);

          boolean skipRounding =
              commonRequestParams.isSkipRounding() || columnHasLegendSet || columnHasOptionSet;

          Object value =
              getValueAndRoundIfNecessary(
                  rs, columnHasLegendSet ? columnLabel + LEGEND : columnLabel, skipRounding);
          addValue(value);
          headersSet.add(columnLabel);

          rowContextItems.putAll(
              ((SqlRowSetJsonExtractorDelegator) rs).getRowContextItem(cols[i], i));
        }
      }
      if (!rowContextItems.isEmpty()) {
        rowContext.put(getCurrentRowWriteIndex(), rowContextItems);
      }
    }

    // Needs to ensure the ordering of columns based on grid headers.
    repositionColumns(repositionHeaders(new ArrayList<>(headersSet)));

    setRowContext(rowContext);
    return this;
  }

  private Object getValueAndRoundIfNecessary(
      SqlRowSet rs, String columnLabel, boolean skipRounding) {
    ValueType valueType = getValueType(columnLabel);
    Object value = rs.getObject(columnLabel);
    if (skipRounding || isNotRoundableType(valueType)) {
      return value;
    }
    // if roundable type we try to parse from string into double and round it
    try {
      return roundIfNecessary(value);
    } catch (Exception e) {
      log.warn(
          format("Failed to parse value as double: %s for column: %s ", value, columnLabel), e);
      // as a fallback we return the value as is
      return value;
    }
  }

  private Double roundIfNecessary(Object value) {
    if (isNull(value)) {
      return null;
    }
    double doubleValue = Double.parseDouble(value.toString());
    if (commonRequestParams.isSkipRounding()) {
      return doubleValue;
    }
    return MathUtils.getRounded(doubleValue);
  }

  private boolean isNotRoundableType(ValueType valueType) {
    return !ROUNDABLE_TYPES.contains(valueType);
  }

  private ValueType getValueType(String col) {
    return commonParsedParams
        .streamDimensions()
        .filter(d -> d.toString().equals(col))
        .findFirst()
        .map(DimensionIdentifier::getDimension)
        .map(DimensionParam::getValueType)
        .orElse(ValueType.TEXT);
  }
}
