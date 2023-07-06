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
package org.hisp.dhis.trackedentity;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.textpattern.TextPattern;

/**
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement(localName = "trackedEntityAttribute", namespace = DxfNamespaces.DXF_2_0)
public class TrackedEntityAttribute extends BaseDimensionalItemObject
    implements MetadataObject, ValueTypedDimensionalItemObject {
  private String description;

  private ValueType valueType;

  private Boolean inherit = false;

  private OptionSet optionSet;

  private String expression;

  private Boolean displayOnVisitSchedule = false;

  private Integer sortOrderInVisitSchedule;

  private Boolean displayInListNoProgram = false;

  private Integer sortOrderInListNoProgram;

  private Boolean confidential = false;

  private Boolean unique = false;

  // For TextPattern:

  private Boolean generated = false;

  private String pattern;

  private TextPattern textPattern;

  /**
   * Field mask represent how the value should be formatted during input. This string will be
   * validated as a TextPatternSegment of type TEXT.
   */
  private String fieldMask;

  /** The style representing how TrackedEntityAttributes should be presented on the client */
  private ObjectStyle style;

  // For Local ID type

  private Boolean orgunitScope = false;

  private Boolean skipSynchronization = false;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public TrackedEntityAttribute() {}

  public TrackedEntityAttribute(
      String name,
      String description,
      ValueType valueType,
      Boolean inherit,
      Boolean displayOnVisitSchedule) {
    this.name = name;
    this.description = description;
    this.valueType = valueType;
    this.inherit = inherit;
    this.displayOnVisitSchedule = displayOnVisitSchedule;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /** Indicates whether the value type of this attribute is numeric. */
  public boolean isNumericType() {
    return valueType.isNumeric();
  }

  /** Indicates whether the value type of this attribute is date. */
  public boolean isDateType() {
    return valueType.isDate();
  }

  /** Indicates whether this attribute has confidential information. */
  @JsonIgnore
  public boolean isConfidentialBool() {
    return confidential != null && confidential;
  }

  /** Indicates whether this attribute has an option set. */
  @Override
  public boolean hasOptionSet() {
    return optionSet != null;
  }

  @Override
  public boolean hasLegendSet() {
    return isNotEmpty(legendSets);
  }

  @JsonIgnore
  public boolean getOrgUnitScopeNullSafe() {
    return orgunitScope != null && orgunitScope;
  }

  public Boolean isSystemWideUnique() {
    return isUnique() && !getOrgUnitScopeNullSafe();
  }

  // -------------------------------------------------------------------------
  // DimensionalItemObject
  // -------------------------------------------------------------------------

  // TODO dimension, not item

  @Override
  public DimensionItemType getDimensionItemType() {
    return DimensionItemType.PROGRAM_ATTRIBUTE;
  }

  // -------------------------------------------------------------------------
  // Helper getters
  // -------------------------------------------------------------------------

  @JsonProperty
  public boolean isOptionSetValue() {
    return optionSet != null;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getInherit() {
    return inherit;
  }

  public void setInherit(Boolean inherit) {
    this.inherit = inherit;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ValueType getValueType() {
    return valueType;
  }

  public void setValueType(ValueType valueType) {
    this.valueType = valueType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getExpression() {
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getDisplayOnVisitSchedule() {
    return displayOnVisitSchedule;
  }

  public void setDisplayOnVisitSchedule(Boolean displayOnVisitSchedule) {
    this.displayOnVisitSchedule = displayOnVisitSchedule;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Integer getSortOrderInVisitSchedule() {
    return sortOrderInVisitSchedule;
  }

  public void setSortOrderInVisitSchedule(Integer sortOrderInVisitSchedule) {
    this.sortOrderInVisitSchedule = sortOrderInVisitSchedule;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getDisplayInListNoProgram() {
    return displayInListNoProgram;
  }

  public void setDisplayInListNoProgram(Boolean displayInListNoProgram) {
    this.displayInListNoProgram = displayInListNoProgram;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Integer getSortOrderInListNoProgram() {
    return sortOrderInListNoProgram;
  }

  public void setSortOrderInListNoProgram(Integer sortOrderInListNoProgram) {
    this.sortOrderInListNoProgram = sortOrderInListNoProgram;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean isUnique() {
    return unique != null ? unique : false;
  }

  public void setUnique(Boolean unique) {
    this.unique = unique;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean isGenerated() {
    return generated != null ? generated : false;
  }

  public void setGenerated(Boolean generated) {
    this.generated = generated;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getPattern() {
    return pattern != null ? pattern : "";
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getOrgunitScope() {
    return orgunitScope;
  }

  public void setOrgunitScope(Boolean orgunitScope) {
    this.orgunitScope = orgunitScope;
  }

  @Override
  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public OptionSet getOptionSet() {
    return optionSet;
  }

  public void setOptionSet(OptionSet optionSet) {
    this.optionSet = optionSet;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getConfidential() {
    return confidential;
  }

  public void setConfidential(Boolean confidential) {
    this.confidential = confidential;
  }

  public TextPattern getTextPattern() {
    return textPattern;
  }

  public void setTextPattern(TextPattern textPattern) {
    this.textPattern = textPattern;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ObjectStyle getStyle() {
    return style;
  }

  public void setStyle(ObjectStyle style) {
    this.style = style;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFormName() {
    return formName;
  }

  @Override
  public void setFormName(String formName) {
    this.formName = formName;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getSkipSynchronization() {
    return skipSynchronization;
  }

  public void setSkipSynchronization(Boolean skipSynchronization) {
    this.skipSynchronization = skipSynchronization;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFieldMask() {
    return fieldMask;
  }

  public void setFieldMask(String fieldMask) {
    this.fieldMask = fieldMask;
  }

  @Override
  public String toString() {
    return "TrackedEntityAttribute{"
        + "description='"
        + description
        + '\''
        + ", formName='"
        + formName
        + '\''
        + ", valueType="
        + valueType
        + ", inherit="
        + inherit
        + ", optionSet="
        + optionSet
        + ", expression='"
        + expression
        + '\''
        + ", displayOnVisitSchedule="
        + displayOnVisitSchedule
        + ", sortOrderInVisitSchedule="
        + sortOrderInVisitSchedule
        + ", displayInListNoProgram="
        + displayInListNoProgram
        + ", sortOrderInListNoProgram="
        + sortOrderInListNoProgram
        + ", confidential="
        + confidential
        + ", unique="
        + unique
        + ", generated="
        + generated
        + ", pattern='"
        + pattern
        + '\''
        + ", textPattern="
        + textPattern
        + ", style="
        + style
        + ", orgunitScope="
        + orgunitScope
        + ", skipSynchronization="
        + skipSynchronization
        + '}';
  }
}
