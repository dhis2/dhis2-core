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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.indicator.IndicatorType;

/**
 * Represents a collection of options grouped by an OptionSet. It extends MetadataItem to be
 * storable in the same map. This class is designed to serialize only the "options" property.
 */
public class MetadataOptionSet extends MetadataItem {

  /**
   * The list of options to be serialized. The field is named 'optionItems' to avoid conflict with
   * the parent's 'options' field.
   */
  private final List<MetadataItem> optionItems;

  public MetadataOptionSet(List<MetadataItem> options) {
    super(null);
    this.optionItems = options;
  }

  /** Getter that is serialized to JSON. */
  @JsonProperty("options")
  public List<MetadataItem> getOptionItems() {
    return optionItems;
  }

  @Override
  @JsonIgnore
  public String getUid() {
    return super.getUid();
  }

  @Override
  @JsonIgnore
  public String getCode() {
    return super.getCode();
  }

  @Override
  @JsonIgnore
  public String getName() {
    return super.getName();
  }

  @Override
  @JsonIgnore
  public String getDescription() {
    return super.getDescription();
  }

  @Override
  @JsonIgnore
  public String getLegendSet() {
    return super.getLegendSet();
  }

  @Override
  @JsonIgnore
  public DimensionType getDimensionType() {
    return super.getDimensionType();
  }

  @Override
  @JsonIgnore
  public DimensionItemType getDimensionItemType() {
    return super.getDimensionItemType();
  }

  @Override
  @JsonIgnore
  public ValueType getValueType() {
    return super.getValueType();
  }

  @Override
  @JsonIgnore
  public AggregationType getAggregationType() {
    return super.getAggregationType();
  }

  @Override
  @JsonIgnore
  public TotalAggregationType getTotalAggregationType() {
    return super.getTotalAggregationType();
  }

  @Override
  @JsonIgnore
  public IndicatorType getIndicatorType() {
    return super.getIndicatorType();
  }

  @Override
  @JsonIgnore
  public Date getStartDate() {
    return super.getStartDate();
  }

  @Override
  @JsonIgnore
  public Date getEndDate() {
    return super.getEndDate();
  }

  @Override
  @JsonIgnore
  public String getExpression() {
    return super.getExpression();
  }

  @Override
  @JsonIgnore
  public ObjectStyle getStyle() {
    return super.getStyle();
  }

  @Override
  @JsonIgnore
  public List<Map<String, String>> getOptions() {
    return super.getOptions();
  }
}
