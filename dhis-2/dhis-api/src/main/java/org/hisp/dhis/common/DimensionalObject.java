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

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.QueryKey;
import org.hisp.dhis.dimensional.DimensionalProperties;
import org.hisp.dhis.eventvisualization.EventRepetition;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

/**
 * @author Lars Helge Overland
 */
public interface DimensionalObject extends NameableObject, GroupableItem {
  /** Gets the dimension identifier. */
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  default String getDimension() {
    return getUid();
  }

  /** Gets the dimension type. */
  DimensionType getDimensionType();

  /**
   * Gets the data dimension type. Can be null. Only applicable for {@link DimensionType#CATEGORY}.
   */
  DataDimensionType getDataDimensionType();

  // -------------------------------------------------------------------------
  // Dimensional properties
  // -------------------------------------------------------------------------

  default DimensionalProperties getDimensionalProperties() {
    return new DimensionalProperties();
  }

  /**
   * Gets the dimension name, which corresponds to a column in the analytics tables, with fall back
   * to dimension.
   */
  default String getDimensionName() {
    return getDimensionalProperties().getDimensionName();
  }

  /** Gets the dimension display name. */
  default String getDimensionDisplayName() {
    return getDimensionalProperties().getDimensionDisplayName();
  }

  /**
   * Returns the value type of the dimension.
   *
   * <p>NOTE: not all dimensional objects have a ValueType, hence this method will return null in
   * such cases.
   */
  default ValueType getValueType() {
    return getDimensionalProperties().getValueType();
  }

  /** Returns the option set of the dimension, if any. */
  default OptionSet getOptionSet() {
    return getDimensionalProperties().getOptionSet();
  }

  // -------------------------------------------------------------------------

  /** Dimension items. */
  List<DimensionalItemObject> getItems();

  default void setItems(List<DimensionalItemObject> items) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + " does not have items");
  }

  /** Indicates whether all available items in this dimension are included. */
  boolean isAllItems();

  /** Indicates whether this dimension has any dimension items. */
  default boolean hasItems() {
    return isNotEmpty(getItems());
  }

  /** Gets the legend set. */
  LegendSet getLegendSet();

  /** Indicates whether this dimension has a legend set. */
  default boolean hasLegendSet() {
    return getLegendSet() != null;
  }

  /** Gets the program stage (not persisted). */
  ProgramStage getProgramStage();

  /** Gets the program (not persisted). */
  Program getProgram();

  /** Indicates whether this dimension has a program (not persisted). */
  default boolean hasProgram() {
    return getProgram() != null;
  }

  /** Indicates whether this dimension has a program stage (not persisted). */
  default boolean hasProgramStage() {
    return getProgramStage() != null;
  }

  /** Gets the aggregation type. */
  AggregationType getAggregationType();

  /** Gets the filter. Contains operator and filter. Applicable for events. */
  String getFilter();

  /** Gets the events repetition. Only applicable for events. */
  EventRepetition getEventRepetition();

  /** Indicates the analytics type of this dimensional object. */
  AnalyticsType getAnalyticsType();

  void setDimensionName(String dimensionName);

  /** Indicates whether this object should be handled as a data dimension. Persistent property. */
  boolean isDataDimension();

  void setEventRepetition(EventRepetition eventRepetition);

  /**
   * Indicates whether this dimension is fixed, meaning that the name of the dimension will be
   * returned as is for all dimension items in the response.
   */
  boolean isFixed();

  List<String> getFilterItemsAsList();
  
  void setFixed(boolean fixed);

  /** Returns dimension item keywords for this dimension. */
  DimensionItemKeywords getDimensionItemKeywords();

  /** Returns a unique key representing this dimension. */
  default String getKey() {
    QueryKey key = new QueryKey();

    key.add("dimension", getDimension());
    getItems().forEach(e -> key.add("item", e.getDimensionItem()));

    return key.add("allItems", getItems())
        .addIgnoreNull("legendSet", getLegendSet())
        .addIgnoreNull("aggregationType", getAggregationType())
        .addIgnoreNull("filter", getFilter())
        .asPlainKey();
  }
}
