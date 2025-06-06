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

import static org.hisp.dhis.common.DataDimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DataDimensionItemType.DATA_ELEMENT_OPERAND;
import static org.hisp.dhis.common.DataDimensionItemType.EXPRESSION_DIMENSION_ITEM;
import static org.hisp.dhis.common.DataDimensionItemType.INDICATOR;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_ATTRIBUTE_OPTION;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_DATA_ELEMENT;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_DATA_ELEMENT_OPTION;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.DataDimensionItemType.REPORTING_RATE;
import static org.hisp.dhis.common.DataDimensionItemType.SUBEXPRESSION_DIMENSION_ITEM;
import static org.hisp.dhis.common.DataDimensionItemType.VALIDATION_RULE;
import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramDataElementOptionDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeOptionDimensionItem;
import org.hisp.dhis.subexpression.SubexpressionDimensionItem;
import org.hisp.dhis.util.MapBuilder;
import org.hisp.dhis.validation.ValidationRule;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "dataDimensionItem", namespace = DXF_2_0)
public class DataDimensionItem {
  private static final Map<DataDimensionItemType, Class<? extends DimensionalItemObject>>
      DATA_DIM_TYPE_CLASS_MAP =
          new MapBuilder<DataDimensionItemType, Class<? extends DimensionalItemObject>>()
              .put(INDICATOR, Indicator.class)
              .put(DATA_ELEMENT, DataElement.class)
              .put(DATA_ELEMENT_OPERAND, DataElementOperand.class)
              .put(REPORTING_RATE, ReportingRate.class)
              .put(PROGRAM_INDICATOR, ProgramIndicator.class)
              .put(PROGRAM_DATA_ELEMENT, ProgramDataElementDimensionItem.class)
              .put(PROGRAM_ATTRIBUTE, ProgramTrackedEntityAttributeDimensionItem.class)
              .put(EXPRESSION_DIMENSION_ITEM, ExpressionDimensionItem.class)
              .put(SUBEXPRESSION_DIMENSION_ITEM, SubexpressionDimensionItem.class)
              .put(VALIDATION_RULE, ValidationRule.class)
              .put(PROGRAM_DATA_ELEMENT_OPTION, ProgramDataElementOptionDimensionItem.class)
              .put(PROGRAM_ATTRIBUTE_OPTION, ProgramTrackedEntityAttributeOptionDimensionItem.class)
              .build();

  public static final Set<Class<? extends DimensionalItemObject>> DATA_DIM_CLASSES =
      Set.copyOf(DATA_DIM_TYPE_CLASS_MAP.values());

  private int id;

  // -------------------------------------------------------------------------
  // Data dimension objects
  // -------------------------------------------------------------------------

  private Indicator indicator;

  private DataElement dataElement;

  private DataElementOperand dataElementOperand;

  private ReportingRate reportingRate;

  private ProgramIndicator programIndicator;

  private ProgramDataElementDimensionItem programDataElement;

  private ProgramTrackedEntityAttributeDimensionItem programAttribute;

  private ExpressionDimensionItem expressionDimensionItem;

  private SubexpressionDimensionItem subexpressionDimensionItem;

  private ProgramDataElementOptionDimensionItem programDataElementOption;

  private ProgramTrackedEntityAttributeOptionDimensionItem programAttributeOption;

  // -------------------------------------------------------------------------
  // Constructor
  // -------------------------------------------------------------------------

  public DataDimensionItem() {}

  public static List<DataDimensionItem> createWithDependencies(
      DimensionalItemObject object, List<DataDimensionItem> items) {
    if (DataElement.class.isAssignableFrom(object.getClass())) {
      DataDimensionItem dimension = new DataDimensionItem();
      DataElement dataElement = (DataElement) object;
      dimension.setDataElement(dataElement);

      List<DataDimensionItem> dataElementOperands =
          items.stream()
              .filter(ddi -> ddi.getDataElementOperand() != null)
              .filter(
                  ddi ->
                      ddi.getDataElementOperand()
                          .getDataElement()
                          .getUid()
                          .equals(dataElement.getUid()))
              .collect(Collectors.toList());

      List<DataDimensionItem> programDataElements =
          items.stream()
              .filter(ddi -> ddi.getProgramDataElement() != null)
              .filter(
                  ddi ->
                      ddi.getProgramDataElement()
                          .getDataElement()
                          .getUid()
                          .equals(dataElement.getUid()))
              .collect(Collectors.toList());

      List<DataDimensionItem> dimensions = Lists.newArrayList(dimension);
      dimensions.addAll(dataElementOperands);
      dimensions.addAll(programDataElements);
      return dimensions;
    }

    return Lists.newArrayList(create(object));
  }

  public static DataDimensionItem create(DimensionalItemObject object) {
    DataDimensionItem dimension = new DataDimensionItem();

    if (Indicator.class.isAssignableFrom(object.getClass())) {
      dimension.setIndicator((Indicator) object);
    } else if (DataElement.class.isAssignableFrom(object.getClass())) {
      dimension.setDataElement((DataElement) object);
    } else if (DataElementOperand.class.isAssignableFrom(object.getClass())) {
      dimension.setDataElementOperand((DataElementOperand) object);
    } else if (ReportingRate.class.isAssignableFrom(object.getClass())) {
      dimension.setReportingRate((ReportingRate) object);
    } else if (ProgramIndicator.class.isAssignableFrom(object.getClass())) {
      dimension.setProgramIndicator((ProgramIndicator) object);
    } else if (ProgramDataElementDimensionItem.class.isAssignableFrom(object.getClass())) {
      dimension.setProgramDataElement((ProgramDataElementDimensionItem) object);
    } else if (ProgramTrackedEntityAttributeDimensionItem.class.isAssignableFrom(
        object.getClass())) {
      dimension.setProgramAttribute((ProgramTrackedEntityAttributeDimensionItem) object);
    } else if (ExpressionDimensionItem.class.isAssignableFrom(object.getClass())) {
      dimension.setExpressionDimensionItem((ExpressionDimensionItem) object);
    } else if (SubexpressionDimensionItem.class.isAssignableFrom(object.getClass())) {
      dimension.setSubexpressionDimensionItem((SubexpressionDimensionItem) object);
    } else if (ProgramDataElementOptionDimensionItem.class.isAssignableFrom(object.getClass())) {
      dimension.setProgramDataElementOption((ProgramDataElementOptionDimensionItem) object);
    } else if (ProgramTrackedEntityAttributeOptionDimensionItem.class.isAssignableFrom(
        object.getClass())) {
      dimension.setProgramAttributeOption(
          (ProgramTrackedEntityAttributeOptionDimensionItem) object);
    } else {
      throw new IllegalArgumentException(
          "Not a valid data dimension: " + object.getClass().getSimpleName() + ", " + object);
    }

    return dimension;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public DimensionalItemObject getDimensionalItemObject() {
    if (indicator != null) {
      return indicator;
    } else if (dataElement != null) {
      return dataElement;
    } else if (dataElementOperand != null) {
      return dataElementOperand;
    } else if (reportingRate != null) {
      return reportingRate;
    } else if (programIndicator != null) {
      return programIndicator;
    } else if (programDataElement != null) {
      return programDataElement;
    } else if (programAttribute != null) {
      return programAttribute;
    } else if (expressionDimensionItem != null) {
      return expressionDimensionItem;
    } else if (subexpressionDimensionItem != null) {
      return subexpressionDimensionItem;
    } else if (programDataElementOption != null) {
      return programDataElementOption;
    } else if (programAttributeOption != null) {
      return programAttributeOption;
    }

    return null;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public DataDimensionItemType getDataDimensionItemType() {
    if (indicator != null) {
      return INDICATOR;
    } else if (dataElement != null) {
      return DATA_ELEMENT;
    } else if (dataElementOperand != null) {
      return DATA_ELEMENT_OPERAND;
    } else if (reportingRate != null) {
      return REPORTING_RATE;
    } else if (programIndicator != null) {
      return PROGRAM_INDICATOR;
    } else if (programDataElement != null) {
      return PROGRAM_DATA_ELEMENT;
    } else if (programAttribute != null) {
      return PROGRAM_ATTRIBUTE;
    } else if (expressionDimensionItem != null) {
      return EXPRESSION_DIMENSION_ITEM;
    } else if (programDataElementOption != null) {
      return PROGRAM_DATA_ELEMENT_OPTION;
    } else if (programAttributeOption != null) {
      return PROGRAM_ATTRIBUTE_OPTION;
    }

    return null;
  }

  /**
   * Returns the class type for the given data dimension item type.
   *
   * @param itemType the {@link DataDimensionItemType}.
   * @return the class type.
   */
  public static Class<? extends DimensionalItemObject> getType(DataDimensionItemType itemType) {
    return DATA_DIM_TYPE_CLASS_MAP.get(itemType);
  }

  // -------------------------------------------------------------------------
  // Equals and hashCode
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result
            + ((getDimensionalItemObject() == null) ? 0 : getDimensionalItemObject().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    DataDimensionItem other = (DataDimensionItem) obj;

    DimensionalItemObject object = getDimensionalItemObject();

    if (object == null) {
      if (other.getDimensionalItemObject() != null) {
        return false;
      }
    } else if (!object.equals(other.getDimensionalItemObject())) {
      return false;
    }

    return true;
  }

  // -------------------------------------------------------------------------
  // Get and set methods
  // -------------------------------------------------------------------------

  @JsonIgnore
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @JsonProperty
  @JsonSerialize(as = BaseNameableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Indicator getIndicator() {
    return indicator;
  }

  public void setIndicator(Indicator indicator) {
    this.indicator = indicator;
  }

  public DataElement getDataElement() {
    return dataElement;
  }

  public void setDataElement(DataElement dataElement) {
    this.dataElement = dataElement;
  }

  @JsonProperty
  @JsonSerialize(as = BaseNameableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public DataElementOperand getDataElementOperand() {
    return dataElementOperand;
  }

  public void setDataElementOperand(DataElementOperand dataElementOperand) {
    this.dataElementOperand = dataElementOperand;
  }

  @JsonProperty
  @JsonSerialize(as = BaseNameableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public ReportingRate getReportingRate() {
    return reportingRate;
  }

  public void setReportingRate(ReportingRate reportingRate) {
    this.reportingRate = reportingRate;
  }

  @JsonProperty
  @JsonSerialize(as = BaseNameableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public ProgramIndicator getProgramIndicator() {
    return programIndicator;
  }

  public void setProgramIndicator(ProgramIndicator programIndicator) {
    this.programIndicator = programIndicator;
  }

  @JsonProperty
  @JsonSerialize(as = BaseNameableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public ProgramDataElementDimensionItem getProgramDataElement() {
    return programDataElement;
  }

  public void setProgramDataElement(ProgramDataElementDimensionItem programDataElement) {
    this.programDataElement = programDataElement;
  }

  @JsonProperty
  @JsonSerialize(as = BaseNameableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public ProgramTrackedEntityAttributeDimensionItem getProgramAttribute() {
    return programAttribute;
  }

  public void setProgramAttribute(ProgramTrackedEntityAttributeDimensionItem programAttribute) {
    this.programAttribute = programAttribute;
  }

  @JsonProperty
  @JsonSerialize(as = BaseNameableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public ExpressionDimensionItem getExpressionDimensionItem() {
    return expressionDimensionItem;
  }

  public void setExpressionDimensionItem(ExpressionDimensionItem expressionDimensionItem) {
    this.expressionDimensionItem = expressionDimensionItem;
  }

  @JsonProperty
  @JsonSerialize(as = BaseNameableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public SubexpressionDimensionItem getSubexpressionDimensionItem() {
    return subexpressionDimensionItem;
  }

  public void setProgramDataElementOption(
      ProgramDataElementOptionDimensionItem programDataElementOption) {
    this.programDataElementOption = programDataElementOption;
  }

  @JsonProperty
  @JsonSerialize(as = BaseNameableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public ProgramDataElementOptionDimensionItem getProgramDataElementOption() {
    return programDataElementOption;
  }

  public void setProgramAttributeOption(
      ProgramTrackedEntityAttributeOptionDimensionItem programAttributeOption) {
    this.programAttributeOption = programAttributeOption;
  }

  @JsonProperty
  @JsonSerialize(as = BaseNameableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public ProgramTrackedEntityAttributeOptionDimensionItem getProgramAttributeOption() {
    return programAttributeOption;
  }

  public void setSubexpressionDimensionItem(SubexpressionDimensionItem subexpressionDimensionItem) {
    this.subexpressionDimensionItem = subexpressionDimensionItem;
  }

  /**
   * Indicates whether this item has an indicator.
   *
   * @return
   */
  public boolean hasIndicator() {
    return indicator != null;
  }

  /**
   * Indicates whether this item has a data element.
   *
   * @return
   */
  public boolean hasDataElement() {
    return dataElement != null;
  }

  /**
   * Indicates whether this item has a data element operand.
   *
   * @return
   */
  public boolean hasDataElementOperand() {
    return dataElementOperand != null;
  }

  /**
   * Indicates whether this item has a reporting rate.
   *
   * @return
   */
  public boolean hasReportingRate() {
    return reportingRate != null;
  }

  /**
   * Indicates whether this item has a program indicator.
   *
   * @return
   */
  public boolean hasProgramIndicator() {
    return programIndicator != null;
  }

  /**
   * Indicates whether this item has a program tracked entity attribute dimension item.
   *
   * @return
   */
  public boolean hasProgramTrackedEntityAttributeDimensionItem() {
    return programAttribute != null;
  }
}
