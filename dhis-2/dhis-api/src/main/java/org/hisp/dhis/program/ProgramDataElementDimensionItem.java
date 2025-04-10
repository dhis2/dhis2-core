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
package org.hisp.dhis.program;

import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "programDataElement", namespace = DxfNamespaces.DXF_2_0)
public class ProgramDataElementDimensionItem extends BaseDimensionalItemObject
    implements EmbeddedObject, ValueTypedDimensionalItemObject {
  private Program program;

  private DataElement dataElement;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public ProgramDataElementDimensionItem() {}

  public ProgramDataElementDimensionItem(Program program, DataElement dataElement) {
    this.program = program;
    this.dataElement = dataElement;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  @Override
  public String getName() {
    return program.getDisplayName() + " " + dataElement.getDisplayName();
  }

  @Override
  public String getShortName() {
    return program.getDisplayShortName() + " " + dataElement.getDisplayShortName();
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
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ValueType getValueType() {
    return dataElement.getValueType();
  }

  @Override
  public String toString() {
    return "{"
        + "\"class\":\""
        + getClass()
        + "\", "
        + "\"id\":\""
        + id
        + "\", "
        + "\"uid\":\""
        + uid
        + "\", "
        + "\"program\":"
        + program
        + ", "
        + "\"dataElement\":"
        + dataElement
        + ", "
        + "\"created\":\""
        + created
        + "\", "
        + "\"lastUpdated\":\""
        + lastUpdated
        + "\" "
        + "}";
  }

  // -------------------------------------------------------------------------
  // DimensionalItemObject
  // -------------------------------------------------------------------------

  @Override
  public String getDimensionItem() {
    return program.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + dataElement.getUid();
  }

  @Override
  public String getDimensionItem(IdScheme idScheme) {
    return program.getPropertyValue(idScheme)
        + COMPOSITE_DIM_OBJECT_PLAIN_SEP
        + dataElement.getPropertyValue(idScheme);
  }

  @Override
  public DimensionItemType getDimensionItemType() {
    return DimensionItemType.PROGRAM_DATA_ELEMENT;
  }

  @Override
  public List<LegendSet> getLegendSets() {
    return dataElement.getLegendSets();
  }

  @Override
  public AggregationType getAggregationType() {
    return dataElement.getAggregationType();
  }

  // -------------------------------------------------------------------------
  // Get and set methods
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(value = PropertyType.REFERENCE, required = Property.Value.TRUE)
  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(value = PropertyType.REFERENCE, required = Property.Value.TRUE)
  public DataElement getDataElement() {
    return dataElement;
  }

  public void setDataElement(DataElement dataElement) {
    this.dataElement = dataElement;
  }

  /**
   * Indicates whether this item has a data element.
   *
   * @return
   */
  public boolean hasDataElement() {
    return dataElement != null;
  }
}
