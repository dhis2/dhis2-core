/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.common;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.With;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;

/**
 * @author Lars Helge Overland
 */
@AllArgsConstructor
public class GridHeader implements Serializable {
  private static final Set<String> NUMERIC_TYPES =
      Set.of(
          Float.class.getName(),
          Double.class.getName(),
          Long.class.getName(),
          Integer.class.getName());

  /** Format header key name. */
  private String name;

  /** Readable pretty header title. */
  private String column;

  private ValueType valueType;

  private String type;

  private boolean hidden;

  private boolean meta;

  private OptionSet optionSet;

  private LegendSet legendSet;

  private String programStage;

  private String displayColumn;

  @With private transient RepeatableStageParams repeatableStageParams;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public GridHeader() {}

  /**
   * @param name formal header name.
   */
  public GridHeader(String name) {
    this.name = name;
    this.column = name;
    this.type = String.class.getName();
    this.hidden = false;
    this.meta = false;
  }

  /**
   * @param name formal header name.
   * @param column readable header title.
   */
  public GridHeader(String name, String column) {
    this(name);
    this.column = column;
  }

  /**
   * @param name formal header name.
   * @param valueType the header value type.
   */
  public GridHeader(String name, ValueType valueType) {
    this(name);
    this.valueType = valueType;
    this.type = valueType.getJavaClass().getName();
  }

  /**
   * Sets the column property to the name value. Sets the type property to String.
   *
   * @param name formal header name.
   * @param hidden indicates whether header is hidden.
   * @param meta indicates whether header is metadata.
   */
  public GridHeader(String name, boolean hidden, boolean meta) {
    this(name);
    this.column = name;
    this.hidden = hidden;
    this.meta = meta;
  }

  /**
   * @param name formal header name.
   * @param column readable header title.
   * @param valueType header value type.
   * @param hidden indicates whether header is hidden.
   * @param meta indicates whether header is metadata.
   */
  public GridHeader(String name, String column, ValueType valueType, boolean hidden, boolean meta) {
    this(name, column);
    this.valueType = valueType;
    this.type = valueType.getJavaClass().getName();
    this.hidden = hidden;
    this.meta = meta;
  }

  /**
   * @param name formal header name.
   * @param column readable header title.
   * @param valueType header value type.
   * @param hidden indicates whether header is hidden.
   * @param meta indicates whether header is metadata.
   * @param optionSet option set.
   * @param legendSet legend set.
   */
  public GridHeader(
      String name,
      String column,
      ValueType valueType,
      boolean hidden,
      boolean meta,
      OptionSet optionSet,
      LegendSet legendSet) {
    this(name, column, valueType, hidden, meta);
    this.optionSet = optionSet;
    this.legendSet = legendSet;
  }

  /**
   * @param name formal header name.
   * @param column readable header title.
   * @param valueType header value type.
   * @param hidden indicates whether header is hidden.
   * @param meta indicates whether header is metadata.
   * @param optionSet option set.
   * @param legendSet legend set.
   * @param programStage program stage.
   * @param repeatableStageParams params for repeatable program stage.
   */
  public GridHeader(
      String name,
      String column,
      ValueType valueType,
      boolean hidden,
      boolean meta,
      OptionSet optionSet,
      LegendSet legendSet,
      String programStage,
      RepeatableStageParams repeatableStageParams) {
    this(name, column, valueType, hidden, meta, optionSet, legendSet);
    this.programStage = programStage;
    this.repeatableStageParams = repeatableStageParams;
  }

  /**
   * @param name formal header name.
   * @param column readable header title. displayColumn the custom display column.
   * @param displayColumn the custom display column.
   * @param valueType header value type.
   * @param hidden indicates whether header is hidden.
   * @param meta indicates whether header is metadata.
   * @param optionSet option set.
   * @param legendSet legend set.
   */
  public GridHeader(
      String name,
      String column,
      String displayColumn,
      ValueType valueType,
      boolean hidden,
      boolean meta,
      OptionSet optionSet,
      LegendSet legendSet) {
    this(name, column, valueType, hidden, meta);
    this.optionSet = optionSet;
    this.legendSet = legendSet;
    this.displayColumn = displayColumn;
  }

  /**
   * @param name formal header name.
   * @param column readable header title.
   * @param displayColumn the custom display column.
   * @param valueType header value type.
   * @param hidden indicates whether header is hidden.
   * @param meta indicates whether header is metadata.
   * @param optionSet option set.
   * @param legendSet legend set.
   * @param programStage program stage.
   * @param repeatableStageParams params for repeatable program stage.
   */
  public GridHeader(
      String name,
      String column,
      String displayColumn,
      ValueType valueType,
      boolean hidden,
      boolean meta,
      OptionSet optionSet,
      LegendSet legendSet,
      String programStage,
      RepeatableStageParams repeatableStageParams) {
    this(name, column, displayColumn, valueType, hidden, meta, optionSet, legendSet);
    this.programStage = programStage;
    this.repeatableStageParams = repeatableStageParams;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean isNumeric() {
    return type != null && NUMERIC_TYPES.contains(type);
  }

  public boolean hasLegendSet() {
    return legendSet != null;
  }

  public boolean hasOptionSet() {
    return optionSet != null;
  }

  public boolean hasValueType(ValueType valueType) {
    return getValueType() == valueType;
  }

  public boolean isDoubleWithoutLegendSet() {
    return Double.class.getName().equals(getType()) && !hasLegendSet();
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty
  public String getColumn() {
    return column;
  }

  public String getDisplayColumn() {
    return defaultIfEmpty(displayColumn, column);
  }

  @JsonProperty
  public ValueType getValueType() {
    return valueType;
  }

  @JsonProperty
  public String getType() {
    return type;
  }

  @JsonProperty
  public boolean isHidden() {
    return hidden;
  }

  @JsonProperty
  public boolean isMeta() {
    return meta;
  }

  @JsonProperty
  public String getOptionSet() {
    return optionSet != null ? optionSet.getUid() : null;
  }

  @JsonIgnore
  public OptionSet getOptionSetObject() {
    return optionSet;
  }

  @JsonProperty
  public String getLegendSet() {
    return legendSet != null ? legendSet.getUid() : null;
  }

  @JsonIgnore
  public LegendSet getLegendSetObject() {
    return legendSet;
  }

  @JsonProperty
  public String getProgramStage() {
    return StringUtils.trimToNull(programStage);
  }

  @JsonProperty
  public String getRepeatableStageParams() {
    return repeatableStageParams != null ? repeatableStageParams.toString() : null;
  }

  @JsonProperty
  public Integer getStageOffset() {
    if (repeatableStageParams == null) {
      return null;
    }

    return repeatableStageParams.getIndex();
  }

  // -------------------------------------------------------------------------
  // hashCode, equals, toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null) {
      return false;
    }

    if (getClass() != object.getClass()) {
      return false;
    }

    final GridHeader other = (GridHeader) object;

    return name.equals(other.name);
  }

  @Override
  public String toString() {
    return "[Name: "
        + name
        + ", column: "
        + column
        + ", value type: "
        + valueType
        + ", type: "
        + type
        + "]";
  }
}
