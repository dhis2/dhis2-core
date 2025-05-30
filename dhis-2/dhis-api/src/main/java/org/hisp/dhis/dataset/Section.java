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
package org.hisp.dhis.dataset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.schema.annotation.PropertyRange;

@JacksonXmlRootElement(localName = "section", namespace = DxfNamespaces.DXF_2_0)
public class Section extends BaseIdentifiableObject implements MetadataObject {
  private String description;

  private DataSet dataSet;

  private List<DataElement> dataElements = new ArrayList<>();

  private List<Indicator> indicators = new ArrayList<>();

  private Set<DataElementOperand> greyedFields = new HashSet<>();

  private int sortOrder;

  private boolean showRowTotals;

  private boolean showColumnTotals;

  private boolean disableDataElementAutoGroup;

  private String displayOptions;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Section() {}

  public Section(
      String name,
      DataSet dataSet,
      List<DataElement> dataElements,
      Set<DataElementOperand> greyedFields) {
    this.name = name;
    this.dataSet = dataSet;
    this.dataElements = dataElements;
    this.greyedFields = greyedFields;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean hasCategoryCombo() {
    return !getCategoryCombos().isEmpty();
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Set<CategoryCombo> getCategoryCombos() {
    Set<CategoryCombo> categoryCombos = new HashSet<>();

    for (DataElement dataElement : dataElements) {
      CategoryCombo categoryCombo = dataElement.getDataElementCategoryCombo(dataSet);

      if (categoryCombo != null) {
        categoryCombos.add(categoryCombo);
      }
    }

    return categoryCombos;
  }

  public boolean hasDataElements() {
    return dataElements != null && !dataElements.isEmpty();
  }

  public List<DataElement> getDataElementsByCategoryCombo(CategoryCombo categoryCombo) {
    List<DataElement> dataElements = new ArrayList<>();

    for (DataElement dataElement : this.dataElements) {
      if (dataElement.getDataElementCategoryCombo(this.dataSet).equals(categoryCombo)) {
        dataElements.add(dataElement);
      }
    }

    return dataElements;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataSet getDataSet() {
    return dataSet;
  }

  public void setDataSet(DataSet dataSet) {
    this.dataSet = dataSet;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "dataElements", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataElement", namespace = DxfNamespaces.DXF_2_0)
  public List<DataElement> getDataElements() {
    return dataElements;
  }

  public void setDataElements(List<DataElement> dataElements) {
    this.dataElements = dataElements;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "indicators", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "indicator", namespace = DxfNamespaces.DXF_2_0)
  public List<Indicator> getIndicators() {
    return indicators;
  }

  public void setIndicators(List<Indicator> indicators) {
    this.indicators = indicators;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "greyedFields", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "greyedField", namespace = DxfNamespaces.DXF_2_0)
  public Set<DataElementOperand> getGreyedFields() {
    return greyedFields;
  }

  public void setGreyedFields(Set<DataElementOperand> greyedFields) {
    this.greyedFields = greyedFields;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isShowRowTotals() {
    return showRowTotals;
  }

  public void setShowRowTotals(boolean showRowTotals) {
    this.showRowTotals = showRowTotals;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isShowColumnTotals() {
    return showColumnTotals;
  }

  public void setShowColumnTotals(boolean showColumnTotals) {
    this.showColumnTotals = showColumnTotals;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @OpenApi.Property(ObjectNode.class)
  public String getDisplayOptions() {
    return displayOptions;
  }

  public void setDisplayOptions(String displayOptions) {
    this.displayOptions = displayOptions;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isDisableDataElementAutoGroup() {
    return disableDataElementAutoGroup;
  }

  public void setDisableDataElementAutoGroup(boolean disableDataElementAutoGroup) {
    this.disableDataElementAutoGroup = disableDataElementAutoGroup;
  }

  public void removeIndicator(Indicator i) {
    this.indicators.remove(i);
  }

  /**
   * Add an Indicator if it is not already present. This helps prevent duplicates in the list.
   *
   * @param i Indicator
   * @return whether the Indicator was added or not
   */
  public boolean addIndicator(@Nonnull Indicator i) {
    if (!this.indicators.contains(i)) {
      return this.indicators.add(i);
    } else return false;
  }

  public void removeIndicators(List<Indicator> sources) {
    for (Indicator i : sources) removeIndicator(i);
  }
}
