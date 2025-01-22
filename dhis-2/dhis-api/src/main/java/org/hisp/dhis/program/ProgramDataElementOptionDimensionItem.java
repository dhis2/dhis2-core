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
package org.hisp.dhis.program;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.joinWith;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_DATA_ELEMENT_OPTION;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;
import static org.hisp.dhis.schema.PropertyType.CONSTANT;
import static org.hisp.dhis.schema.PropertyType.REFERENCE;
import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;
import static org.hisp.dhis.schema.annotation.Property.Value.TRUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.Objects;
import java.util.List;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.Aggregation;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.annotation.Property;

/**
 * This class represents the junction of a {@link Program}, {@link DataElement}, {@link Option} and
 * the respective {@link Aggregation}. It's mainly used as a dimensional item ({@link
 * DimensionalItemObject}), in visualization objects. ie: {@link
 * org.hisp.dhis.visualization.Visualization}
 */
@NoArgsConstructor
@JacksonXmlRootElement(localName = "programDataElementOptionDimension", namespace = DXF_2_0)
public class ProgramDataElementOptionDimensionItem extends BaseDimensionalItemObject
    implements EmbeddedObject, ValueTypedDimensionalItemObject {
  private Program program;
  private DataElement dataElement;
  private Option option;
  private Aggregation aggregation;

  public ProgramDataElementOptionDimensionItem(
      Program program, DataElement dataElement, Option option, Aggregation aggregation) {
    this.program = program;
    this.dataElement = dataElement;
    this.option = option;
    this.aggregation = aggregation;
  }

  @Override
  public String getName() {
    return joinWith(
        SPACE, program.getDisplayName(), dataElement.getDisplayName(), option.getDisplayName());
  }

  @Override
  public String getShortName() {
    return joinWith(
        SPACE,
        program.getDisplayShortName(),
        dataElement.getDisplayShortName(),
        option.getDisplayShortName());
  }

  @Override
  public boolean hasOptionSet() {
    return dataElement.hasOptionSet();
  }

  @Override
  public OptionSet getOptionSet() {
    return dataElement.getOptionSet();
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public ValueType getValueType() {
    return dataElement.getValueType();
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .add("program", program)
        .add("dataElement", dataElement)
        .add("option", option)
        .add("aggregation", aggregation)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(program, dataElement, option, aggregation);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof ProgramDataElementOptionDimensionItem item && objectEquals(item);
  }

  private boolean objectEquals(ProgramDataElementOptionDimensionItem other) {
    return Objects.equal(program, other.program)
        && Objects.equal(dataElement, other.dataElement)
        && Objects.equal(option, other.option)
        && Objects.equal(aggregation, other.aggregation);
  }

  @Override
  public String getDimensionItem() {
    return joinWith(
        COMPOSITE_DIM_OBJECT_PLAIN_SEP,
        program.getUid(),
        dataElement.getUid(),
        option.getUid(),
        aggregation);
  }

  @Override
  public String getDimensionItem(IdScheme idScheme) {
    return program.getPropertyValue(idScheme)
        + COMPOSITE_DIM_OBJECT_PLAIN_SEP
        + dataElement.getPropertyValue(idScheme);
  }

  @Override
  public DimensionItemType getDimensionItemType() {
    return PROGRAM_DATA_ELEMENT_OPTION;
  }

  @Override
  public List<LegendSet> getLegendSets() {
    return dataElement.getLegendSets();
  }

  @Override
  public AggregationType getAggregationType() {
    return dataElement.getAggregationType();
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  @Property(value = REFERENCE, required = TRUE)
  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  @Property(value = REFERENCE, required = TRUE)
  public DataElement getDataElement() {
    return dataElement;
  }

  public void setDataElement(DataElement dataElement) {
    this.dataElement = dataElement;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  @Property(value = REFERENCE, required = FALSE)
  public Option getOption() {
    return option;
  }

  public void setOption(Option option) {
    this.option = option;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  @Property(value = CONSTANT, required = TRUE)
  public Aggregation getAggregation() {
    return aggregation;
  }

  public void setAggregation(Aggregation aggregation) {
    this.aggregation = aggregation;
  }
}
