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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CombinationGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.SystemDefaultMetadataObject;

/**
 * @author Abyot Aselefew
 */
@JacksonXmlRootElement(localName = "categoryCombo", namespace = DxfNamespaces.DXF_2_0)
public class CategoryCombo extends BaseIdentifiableObject implements SystemDefaultMetadataObject {
  public static final String DEFAULT_CATEGORY_COMBO_NAME = "default";

  /** A set with categories. */
  private List<Category> categories = new ArrayList<>();

  /**
   * A set of category option combinations. Use getSortedOptionCombos() to get a sorted list of
   * category option combinations.
   */
  private Set<CategoryOptionCombo> optionCombos = new HashSet<>();

  /**
   * Type of data dimension. Category combinations of type DISAGGREGATION can be linked to data
   * elements, whereas type ATTRIBUTE can be linked to data sets.
   */
  private DataDimensionType dataDimensionType;

  /** Indicates whether to skip total values for the categories in reports. */
  private boolean skipTotal;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public CategoryCombo() {}

  public CategoryCombo(String name, DataDimensionType dataDimensionType) {
    this.name = name;
    this.dataDimensionType = dataDimensionType;
  }

  public CategoryCombo(
      String name, DataDimensionType dataDimensionType, List<Category> categories) {
    this(name, dataDimensionType);
    this.categories = categories;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  @JsonProperty("isDefault")
  @Override
  public boolean isDefault() {
    return DEFAULT_CATEGORY_COMBO_NAME.equals(name);
  }

  /**
   * Indicates whether this category combo has at least one category, has at least one category
   * option combo and that all categories have at least one category option.
   */
  public boolean isValid() {
    if (categories == null || categories.isEmpty()) {
      return false;
    }

    for (Category category : categories) {
      if (category == null
          || category.getCategoryOptions() == null
          || category.getCategoryOptions().isEmpty()) {
        return false;
      }
    }

    return true;
  }

  public List<CategoryOption> getCategoryOptions() {
    final List<CategoryOption> categoryOptions = new ArrayList<>();

    for (Category category : categories) {
      categoryOptions.addAll(category.getCategoryOptions());
    }

    return categoryOptions;
  }

  public boolean doTotal() {
    return optionCombos != null && optionCombos.size() > 1 && !skipTotal;
  }

  public boolean doSubTotals() {
    return categories != null && categories.size() > 1;
  }

  public List<List<CategoryOption>> getCategoryOptionsAsLists() {
    return categories.stream()
        .filter(ca -> !ca.getCategoryOptions().isEmpty())
        .map(ca -> ca.getCategoryOptions())
        .collect(Collectors.toList());
  }

  public List<CategoryOptionCombo> generateOptionCombosList() {
    List<CategoryOptionCombo> list = new ArrayList<>();

    CombinationGenerator<CategoryOption> generator =
        CombinationGenerator.newInstance(getCategoryOptionsAsLists());

    while (generator.hasNext()) {
      CategoryOptionCombo optionCombo = new CategoryOptionCombo();
      optionCombo.setCategoryOptions(new HashSet<>(generator.getNext()));
      optionCombo.setCategoryCombo(this);
      list.add(optionCombo);
    }

    return list;
  }

  public List<CategoryOptionCombo> getSortedOptionCombos() {
    List<CategoryOptionCombo> list = new ArrayList<>();

    CombinationGenerator<CategoryOption> generator =
        CombinationGenerator.newInstance(getCategoryOptionsAsLists());

    while (generator.hasNext()) {
      List<CategoryOption> categoryOptions = generator.getNext();

      Set<CategoryOption> categoryOptionSet = new HashSet<>(categoryOptions);

      for (CategoryOptionCombo optionCombo : optionCombos) {
        Set<CategoryOption> persistedCategoryOptions =
            new HashSet<>(optionCombo.getCategoryOptions());

        if (categoryOptionSet.equals(persistedCategoryOptions)) {
          list.add(optionCombo);
          continue;
        }
      }
    }

    return list;
  }

  public void generateOptionCombos() {
    this.optionCombos = new HashSet<>(generateOptionCombosList());

    for (CategoryOptionCombo optionCombo : optionCombos) {
      for (CategoryOption categoryOption : optionCombo.getCategoryOptions()) {
        categoryOption.addCategoryOptionCombo(optionCombo);
      }
    }
  }

  public boolean hasOptionCombos() {
    return optionCombos != null && !optionCombos.isEmpty();
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void addCategory(Category category) {
    categories.add(category);
    category.getCategoryCombos().add(this);
  }

  public void removeCategory(Category category) {
    categories.remove(category);
    category.getCategoryCombos().remove(this);
  }

  public void removeAllCategories() {
    for (Category category : categories) {
      category.getCategoryCombos().remove(this);
    }

    categories.clear();
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "categories", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "category", namespace = DxfNamespaces.DXF_2_0)
  public List<Category> getCategories() {
    return categories;
  }

  public void setCategories(List<Category> categories) {
    this.categories = categories;
  }

  @JsonProperty("categoryOptionCombos")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "categoryOptionCombos", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryOptionCombo", namespace = DxfNamespaces.DXF_2_0)
  public Set<CategoryOptionCombo> getOptionCombos() {
    return optionCombos;
  }

  public void setOptionCombos(Set<CategoryOptionCombo> optionCombos) {
    this.optionCombos = optionCombos;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataDimensionType getDataDimensionType() {
    return dataDimensionType;
  }

  public void setDataDimensionType(DataDimensionType dataDimensionType) {
    this.dataDimensionType = dataDimensionType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isSkipTotal() {
    return skipTotal;
  }

  public void setSkipTotal(boolean skipTotal) {
    this.skipTotal = skipTotal;
  }
}
