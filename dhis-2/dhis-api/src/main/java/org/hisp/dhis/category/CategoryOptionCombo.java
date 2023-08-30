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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;

/**
 * @author Abyot Aselefew
 */
@JacksonXmlRootElement(localName = "categoryOptionCombo", namespace = DxfNamespaces.DXF_2_0)
public class CategoryOptionCombo extends BaseDimensionalItemObject
    implements SystemDefaultMetadataObject {
  public static final String DEFAULT_NAME = "default";

  public static final String DEFAULT_TOSTRING = "(default)";

  /** The category combo. */
  private CategoryCombo categoryCombo;

  /** The category options. */
  private Set<CategoryOption> categoryOptions = new HashSet<>();

  /** Indicates whether to ignore data approval. */
  private boolean ignoreApproval;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public CategoryOptionCombo() {}

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    final int prime = 31;

    int result = 1;

    result = prime * result + ((categoryCombo == null) ? 0 : categoryCombo.hashCode());
    result = prime * result + ((categoryOptions == null) ? 0 : categoryOptions.hashCode());

    return result;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof CategoryOptionCombo && objectEquals((CategoryOptionCombo) obj);
  }

  private boolean objectEquals(CategoryOptionCombo other) {
    return Objects.equals(categoryCombo, other.categoryCombo)
        && Objects.equals(categoryOptions, other.categoryOptions);
  }

  @Override
  public String toString() {
    return "{"
        + "\"class\":\""
        + getClass()
        + "\", "
        + "\"id\":\""
        + getId()
        + "\", "
        + "\"uid\":\""
        + getUid()
        + "\", "
        + "\"code\":\""
        + getCode()
        + "\", "
        + "\"categoryCombo\":"
        + categoryCombo
        + ", "
        + "\"categoryOptions\":"
        + categoryOptions
        + "}";
  }

  // -------------------------------------------------------------------------
  // hashCode and equals based on identifiable object
  // -------------------------------------------------------------------------

  public int hashCodeIdentifiableObject() {
    return super.hashCode();
  }

  public boolean equalsIdentifiableObject(Object object) {
    return super.equals(object);
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void addCategoryOption(CategoryOption dataElementCategoryOption) {
    categoryOptions.add(dataElementCategoryOption);
    dataElementCategoryOption.getCategoryOptionCombos().add(this);
  }

  public void removeCategoryOption(CategoryOption dataElementCategoryOption) {
    categoryOptions.remove(dataElementCategoryOption);
    dataElementCategoryOption.getCategoryOptionCombos().remove(this);
  }

  public void removeAllCategoryOptions() {
    categoryOptions.clear();
  }

  @Override
  public boolean isDefault() {
    return categoryCombo != null && DEFAULT_NAME.equals(categoryCombo.getName());
  }

  /**
   * Gets a range of valid dates for this (attribute) category option combo for a data set.
   *
   * <p>The earliest valid date is the latest start date (if any) from all the category options
   * associated with this option combo.
   *
   * <p>The latest valid date is the earliest end date (if any) from all the category options
   * associated with this option combo.
   *
   * @param dataSet the data set for which to check dates.
   * @return valid date range for this (attribute) category option combo.
   */
  public DateRange getDateRange(DataSet dataSet) {
    Date earliestEndDate =
        getCategoryOptions().stream()
            .map(co -> co.getAdjustedEndDate(dataSet))
            .filter(Objects::nonNull)
            .min(Date::compareTo)
            .orElse(null);

    return new DateRange(getLatestStartDate(), earliestEndDate);
  }

  /**
   * Gets a range of valid dates for this (attribute) category option combo for a data element (for
   * all data sets to which the data element belongs).
   *
   * <p>The earliest valid date is the latest start date (if any) from all the category options
   * associated with this option combo.
   *
   * <p>The latest valid date is the earliest end date (if any) from all the category options
   * associated with this option combo.
   *
   * @param dataElement the data element for which to check dates.
   * @return valid date range for this (attribute) category option combo.
   */
  public DateRange getDateRange(DataElement dataElement) {
    Date earliestEndDate =
        getCategoryOptions().stream()
            .map(co -> co.getAdjustedEndDate(dataElement))
            .filter(Objects::nonNull)
            .min(Date::compareTo)
            .orElse(null);

    return new DateRange(getLatestStartDate(), earliestEndDate);
  }

  /**
   * Gets a range of valid dates for this (attribute) category option combo for a program.
   *
   * <p>The earliest valid date is the latest start date (if any) from all the category options
   * associated with this option combo.
   *
   * <p>The latest valid date is the earliest end date (if any) from all the category options
   * associated with this option combo.
   *
   * @param program the data set for which to check dates.
   * @return valid date range for this (attribute) category option combo.
   */
  public DateRange getDateRange(Program program) {
    Date earliestEndDate =
        getCategoryOptions().stream()
            .map(co -> co.getAdjustedEndDate(program))
            .filter(Objects::nonNull)
            .min(Date::compareTo)
            .orElse(null);

    return new DateRange(getLatestStartDate(), earliestEndDate);
  }

  /**
   * Gets a set of valid organisation units (subtrees) for this (attribute) category option combo,
   * if any.
   *
   * <p>The set of valid organisation units (if any) is the intersection of the sets of valid
   * organisation untis for all the category options associated with this option combo.
   *
   * <p>Note: returns null if there are no organisation unit restrictions (no associated option
   * combos have any organisation unit restrictions), but returns an empty set if associated option
   * combos have organisation unit restrictions and their intersection is empty.
   *
   * @return valid organisation units for this (attribute) category option combo.
   */
  public Set<OrganisationUnit> getOrganisationUnits() {
    Set<OrganisationUnit> orgUnits = null;

    for (CategoryOption option : getCategoryOptions()) {
      if (!CollectionUtils.isEmpty(option.getOrganisationUnits())) {
        if (orgUnits == null) {
          orgUnits = option.getOrganisationUnits();
        } else {
          orgUnits = new HashSet<>(orgUnits);
          orgUnits.retainAll(option.getOrganisationUnits());
        }
      }
    }

    return orgUnits;
  }

  /**
   * Gets the latest category option start date for this category option combo. The combo is only
   * valid between the latest start date of any options and the earliest end date of any options.
   *
   * @return the latest option start date for this combo.
   */
  public Date getLatestStartDate() {
    return getCategoryOptions().stream()
        .map(CategoryOption::getStartDate)
        .filter(Objects::nonNull)
        .max(Date::compareTo)
        .orElse(null);
  }

  /**
   * Gets the earliest category option end date for this category option combo. The combo is only
   * valid between the latest start date of any options and the earliest end date of any options.
   *
   * <p>Note that this end date does not take into account any possible extensions to the category
   * end dates for aggregate data entry in data sets with openPeriodsAfterCoEndDate.
   *
   * @return the earliest option end date for this combo.
   */
  public Date getEarliestEndDate() {
    return getCategoryOptions().stream()
        .map(CategoryOption::getEndDate)
        .filter(Objects::nonNull)
        .min(Date::compareTo)
        .orElse(null);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @Override
  public String getName() {
    if (name != null) {
      return name;
    }

    StringBuilder builder = new StringBuilder();

    if (categoryCombo == null || categoryCombo.getCategories().isEmpty()) {
      return uid;
    }

    List<Category> categories = categoryCombo.getCategories();

    for (Category category : categories) {
      List<CategoryOption> options = category.getCategoryOptions();

      optionLoop:
      for (CategoryOption option : categoryOptions) {
        if (options.contains(option)) {
          builder.append(option.getDisplayName()).append(", ");

          continue optionLoop;
        }
      }
    }

    builder.delete(Math.max(builder.length() - 2, 0), builder.length());

    return StringUtils.substring(builder.toString(), 0, 255);
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  @JsonIgnore
  public String getShortName() {
    return getName();
  }

  @Override
  public void setShortName(String shortName) {
    // Not supported
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public CategoryCombo getCategoryCombo() {
    return categoryCombo;
  }

  public void setCategoryCombo(CategoryCombo categoryCombo) {
    this.categoryCombo = categoryCombo;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "categoryOptions", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryOption", namespace = DxfNamespaces.DXF_2_0)
  public Set<CategoryOption> getCategoryOptions() {
    return categoryOptions;
  }

  public void setCategoryOptions(Set<CategoryOption> categoryOptions) {
    this.categoryOptions = categoryOptions;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isIgnoreApproval() {
    return ignoreApproval;
  }

  public void setIgnoreApproval(boolean ignoreApproval) {
    this.ignoreApproval = ignoreApproval;
  }
}
