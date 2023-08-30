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
package org.hisp.dhis.category;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.SystemDefaultMetadataObject;

/**
 * A Category is a dimension of a data element. DataElements can have sets of dimensions (known as
 * CategoryCombos). An Example of a Category might be "Sex". The Category could have two (or more)
 * CategoryOptions such as "Male" and "Female".
 *
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement(localName = "category", namespace = DxfNamespaces.DXF_2_0)
public class Category extends BaseDimensionalObject implements SystemDefaultMetadataObject {
  public static final String DEFAULT_NAME = "default";

  private List<CategoryOption> categoryOptions = new ArrayList<>();

  private List<CategoryCombo> categoryCombos = new ArrayList<>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Category() {}

  public Category(String name, DataDimensionType dataDimensionType) {
    this.dataDimensionType = dataDimensionType;
    this.name = name;
  }

  public Category(
      String name, DataDimensionType dataDimensionType, List<CategoryOption> categoryOptions) {
    this(name, dataDimensionType);
    this.categoryOptions = categoryOptions;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void addCategoryOption(CategoryOption categoryOption) {
    categoryOptions.add(categoryOption);
    categoryOption.getCategories().add(this);
  }

  public void removeCategoryOption(CategoryOption categoryOption) {
    categoryOptions.remove(categoryOption);
    categoryOption.getCategories().remove(this);
  }

  public void removeAllCategoryOptions() {
    for (CategoryOption categoryOption : categoryOptions) {
      categoryOption.getCategories().remove(this);
    }

    categoryOptions.clear();
  }

  public void addCategoryCombo(CategoryCombo categoryCombo) {
    categoryCombos.add(categoryCombo);
    categoryCombo.getCategories().add(this);
  }

  public void removeCategoryCombo(CategoryCombo categoryCombo) {
    categoryCombos.remove(categoryCombo);
    categoryCombo.getCategories().remove(this);
  }

  public void removeAllCategoryCombos() {
    for (CategoryCombo categoryCombo : categoryCombos) {
      categoryCombo.getCategories().remove(this);
    }

    categoryCombos.clear();
  }

  public CategoryOption getCategoryOption(CategoryOptionCombo categoryOptionCombo) {
    for (CategoryOption categoryOption : categoryOptions) {
      if (categoryOption.getCategoryOptionCombos().contains(categoryOptionCombo)) {
        return categoryOption;
      }
    }

    return null;
  }

  @Override
  public boolean isDefault() {
    return DEFAULT_NAME.equals(name);
  }

  // -------------------------------------------------------------------------
  // Dimensional object
  // -------------------------------------------------------------------------

  @Override
  @JsonProperty
  @JsonSerialize(contentAs = BaseDimensionalItemObject.class)
  @JacksonXmlElementWrapper(localName = "items", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "item", namespace = DxfNamespaces.DXF_2_0)
  public List<DimensionalItemObject> getItems() {
    return Lists.newArrayList(categoryOptions);
  }

  @Override
  public DimensionType getDimensionType() {
    return DimensionType.CATEGORY;
  }

  // ------------------------------------------------------------------------
  // Getters and setters
  // ------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "categoryOptions", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryOption", namespace = DxfNamespaces.DXF_2_0)
  public List<CategoryOption> getCategoryOptions() {
    return categoryOptions;
  }

  public void setCategoryOptions(List<CategoryOption> categoryOptions) {
    this.categoryOptions = categoryOptions;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "categoryCombos", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryCombo", namespace = DxfNamespaces.DXF_2_0)
  public List<CategoryCombo> getCategoryCombos() {
    return categoryCombos;
  }

  public void setCategoryCombos(List<CategoryCombo> categoryCombos) {
    this.categoryCombos = categoryCombos;
  }
}
