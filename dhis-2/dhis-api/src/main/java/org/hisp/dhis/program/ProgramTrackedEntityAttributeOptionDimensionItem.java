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

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.joinWith;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_ATTRIBUTE_OPTION;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;
import static org.hisp.dhis.schema.PropertyType.REFERENCE;
import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;
import static org.hisp.dhis.schema.annotation.Property.Value.TRUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.Objects;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * This class represents the junction of a {@link Program}, {@link TrackedEntityAttribute} and
 * {@link Option}. It's mainly used as a dimensional item ({@link DimensionalItemObject}), in
 * visualization objects. ie: {@link org.hisp.dhis.visualization.Visualization}
 */
@NoArgsConstructor
@JacksonXmlRootElement(localName = "programAttributeOptionDimension", namespace = DXF_2_0)
public class ProgramTrackedEntityAttributeOptionDimensionItem extends BaseDimensionalItemObject
    implements EmbeddedObject {
  private Program program;
  private TrackedEntityAttribute attribute;
  private Option option;

  public ProgramTrackedEntityAttributeOptionDimensionItem(
      @Nonnull Program program, @Nonnull TrackedEntityAttribute attribute, @Nonnull Option option) {
    this.program = program;
    this.attribute = attribute;
    this.option = option;
  }

  @Override
  public String getName() {
    return format(
        "%s (%s, %s)",
        option.getDisplayName(), attribute.getDisplayName(), program.getDisplayName());
  }

  @Override
  public String getShortName() {
    return format(
        "%s (%s, %s)",
        option.getDisplayShortName(),
        attribute.getDisplayShortName(),
        program.getDisplayShortName());
  }

  @Override
  public String getDisplayProperty(DisplayProperty displayProperty) {
    return format(
        "%s (%s, %s)",
        option.getDisplayProperty(displayProperty),
        attribute.getDisplayProperty(displayProperty),
        program.getDisplayProperty(displayProperty));
  }

  @Override
  public String getDimensionItem() {
    return joinWith(
        COMPOSITE_DIM_OBJECT_PLAIN_SEP, program.getUid(), attribute.getUid(), option.getUid());
  }

  @Override
  public String getDimensionItem(IdScheme idScheme) {
    return program.getPropertyValue(idScheme)
        + COMPOSITE_DIM_OBJECT_PLAIN_SEP
        + attribute.getPropertyValue(idScheme);
  }

  @Override
  public DimensionItemType getDimensionItemType() {
    return PROGRAM_ATTRIBUTE_OPTION;
  }

  @Override
  public List<LegendSet> getLegendSets() {
    return attribute.getLegendSets();
  }

  @Override
  public AggregationType getAggregationType() {
    return attribute.getAggregationType();
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .add("program", program)
        .add("attribute", attribute)
        .add("option", option)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(program, attribute, option);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof ProgramTrackedEntityAttributeOptionDimensionItem item
            && objectEquals(item);
  }

  private boolean objectEquals(ProgramTrackedEntityAttributeOptionDimensionItem other) {
    return Objects.equal(attribute, other.attribute)
        && Objects.equal(program, other.program)
        && Objects.equal(option, other.option);
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
  public TrackedEntityAttribute getAttribute() {
    return attribute;
  }

  public void setAttribute(TrackedEntityAttribute attribute) {
    this.attribute = attribute;
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
}
