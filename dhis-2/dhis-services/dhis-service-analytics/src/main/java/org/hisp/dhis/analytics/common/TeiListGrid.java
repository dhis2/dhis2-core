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
package org.hisp.dhis.analytics.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueStatus;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public class TeiListGrid extends ListGrid {

  private static final List<ValueType> ROUNDABLE_TYPES =
      List.of(ValueType.PERCENTAGE, ValueType.NUMBER);

  @JsonIgnore private final transient TeiQueryParams teiQueryParams;

  public TeiListGrid(TeiQueryParams teiQueryParams) {
    super();
    this.teiQueryParams = teiQueryParams;
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

          Object value = getValueAndRoundIfNecessary(rs, columnLabel);
          addValue(value);
          headersSet.add(columnLabel);

          rowContextItems.putAll(getRowContextItem(rs, cols[i], i));
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

  private Object getValueAndRoundIfNecessary(SqlRowSet rs, String columnLabel) {
    ValueType valueType = getValueType(columnLabel);
    if (isRoundableType(valueType)) {
      Object value = rs.getObject(columnLabel);
      if (Objects.nonNull(value)) {
        double doubleValue = rs.getDouble(columnLabel);
        if (!teiQueryParams.getCommonParams().isSkipRounding()) {
          return MathUtils.getRounded(doubleValue);
        }
        return doubleValue;
      }
      // value is null here
      return null;
    }
    return rs.getObject(columnLabel);
  }

  private boolean isRoundableType(ValueType valueType) {
    return ROUNDABLE_TYPES.contains(valueType);
  }

  private ValueType getValueType(String col) {
    return teiQueryParams
        .getCommonParams()
        .streamDimensions()
        .filter(d -> d.toString().equals(col))
        .findFirst()
        .map(DimensionIdentifier::getDimension)
        .map(DimensionParam::getValueType)
        .orElse(ValueType.TEXT);
  }

  /**
   * The method retrieves row context content that describes the origin of the data value,
   * indicating whether it is set, not set, or undefined. The column index is used as the map key,
   * and the corresponding value contains information about the origin, also known as the value
   * status.
   *
   * @param rs the {@link ResultSet},
   * @param columnName the {@link String}, grid row column name
   * @return Map of column index and value status
   */
  private Map<String, Object> getRowContextItem(SqlRowSet rs, String columnName, int rowIndex) {
    Map<String, Object> rowContextItem = new HashMap<>();
    String existIndicatorColumnLabel = columnName + EXISTS;
    String statusIndicatorColumnLabel = columnName + STATUS;
    String hasValueIndicatorColumnLabel = columnName + HAS_VALUE;

    if (Arrays.stream(rs.getMetaData().getColumnNames())
        .anyMatch(n -> n.equalsIgnoreCase(existIndicatorColumnLabel))) {

      boolean isDefined = rs.getBoolean(existIndicatorColumnLabel);
      boolean isSet = rs.getBoolean(hasValueIndicatorColumnLabel);
      boolean isScheduled =
          StringUtils.equalsIgnoreCase(
              rs.getString(statusIndicatorColumnLabel), EventStatus.SCHEDULE.toString());

      ValueStatus valueStatus = ValueStatus.SET;

      if (!isDefined) {
        valueStatus = ValueStatus.NOT_DEFINED;
      } else if (isScheduled) {
        valueStatus = ValueStatus.SCHEDULED;
      } else if (!isSet) {
        valueStatus = ValueStatus.NOT_SET;
      }

      if (valueStatus != ValueStatus.SET) {
        Map<String, String> valueStatusMap = new HashMap<>();
        valueStatusMap.put("valueStatus", valueStatus.getValue());
        rowContextItem.put(Integer.toString(rowIndex), valueStatusMap);
      }
    }

    return rowContextItem;
  }
}
