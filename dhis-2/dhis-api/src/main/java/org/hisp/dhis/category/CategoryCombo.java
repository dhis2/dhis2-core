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
package org.hisp.dhis.category;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CombinationGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;
import org.jetbrains.annotations.NotNull;

/**
 * @author Abyot Aselefew
 */
@Setter
@JacksonXmlRootElement(localName = "categoryCombo", namespace = DxfNamespaces.DXF_2_0)
public class CategoryCombo  implements SystemDefaultMetadataObject, IdentifiableObject {
  public static final String DEFAULT_CATEGORY_COMBO_NAME = "default";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "categorycomboid")
  private long id;

  @Column(name = "uid", unique = true, nullable = false, length = 11)
  private String uid;

  @Column(name = "code", unique = true, nullable = true, length = 50)
  private String code;

  @Column(name = "created", nullable = false, updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Column(name = "lastUpdated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdated;

  @ManyToOne
  @JoinColumn(name = "lastupdatedby", foreignKey = @ForeignKey(name = "fk_lastupdateby_userid"))
  private User lastUpdatedBy;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @Column(name = "translations")
  @Type(
      type = "jblTranslations",
      parameters = {@Parameter(name = "clazz", value = "org.hisp.dhis.translation.Translation")})
  private String translations;

  @OneToMany
  @JoinTable(
      name = "categorycombos_categories",
      joinColumns =
      @JoinColumn(
          name = "categorycomboid",
          foreignKey = @ForeignKey(name = "fk_categorycombos_categories_categorycomboid")),
      inverseJoinColumns =
      @JoinColumn(
          name = "categoryid",
          foreignKey = @ForeignKey(name = "fk_categorycombo_categoryid")))
  @OrderColumn(name = "sort_order")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<Category> categories;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinTable(
      name = "categorycombos_optioncombos",
      joinColumns =
      @JoinColumn(
          name = "categorycomboid",
          foreignKey = @ForeignKey(name = "fk_categorycombos_optioncombos_categorycomboid")),
      inverseJoinColumns =
      @JoinColumn(
          name = "categoryoptioncomboid",
          foreignKey = @ForeignKey(name = "fk_categorycombo_categoryoptioncomboid")))
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<CategoryOptionCombo> optionCombos;

  @Column(name = "datadimensiontype", nullable = false)
  @Enumerated(EnumType.STRING)
  private DataDimensionType dataDimensionType;

  @Column(name = "skiptotal", nullable = false)
  private boolean skipTotal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "userid", foreignKey = @ForeignKey(name = "fk_categorycombo_userid"))
  private User createdBy;

  @Column(name = "sharing")
  @Type(type = "jsbObjectSharing")
  private String sharing;

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
    // return default option combos if default
    if (this.isDefault()) return this.optionCombos.stream().toList();

    List<CategoryOptionCombo> list = new ArrayList<>();
    CombinationGenerator<CategoryOption> generator =
        CombinationGenerator.newInstance(getCategoryOptionsAsLists());

    while (generator.hasNext()) {
      CategoryOptionCombo optionCombo = new CategoryOptionCombo();
      optionCombo.setCategoryOptions(new HashSet<>(generator.getNext()));
      optionCombo.setCategoryCombo(this);
      list.add(optionCombo);

      for (CategoryOption categoryOption : optionCombo.getCategoryOptions()) {
        categoryOption.addCategoryOptionCombo(optionCombo);
      }
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
        }
      }
    }

    return list;
  }

  @JsonIgnore
  public List<Category> getDataDimensionCategories() {
    return categories.stream().filter(Category::isDataDimension).toList();
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

  public void addCategoryOptionCombo(@Nonnull CategoryOptionCombo coc) {
    this.getOptionCombos().add(coc);
  }

  public void removeCategoryOptionCombo(@Nonnull CategoryOptionCombo coc) {
    this.getOptionCombos().remove(coc);
  }

  public void removeCategoryOptionCombos(@Nonnull Collection<CategoryOptionCombo> cocs) {
    cocs.forEach(this::removeCategoryOptionCombo);
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

  // -------------------------------------------------------------------------
  // Implementation methods from IdentifiableObject
  // -------------------------------------------------------------------------
  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getDisplayName() {
    return "";
  }

  @Override
  public Date getCreated() {
    return created;
  }

  @Override
  public Date getLastUpdated() {
    return lastUpdated;
  }

  @Override
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @Override
  public AttributeValues getAttributeValues() {
    return null;
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {}

  @Override
  public void addAttributeValue(String attributeUid, String value) {}

  @Override
  public void removeAttributeValue(String attributeId) {}

  @Override
  public void setAccess(Access access) {}

  @Override
  public Set<String> getFavorites() {
    return Set.of();
  }

  @Override
  public boolean isFavorite() {
    return false;
  }

  @Override
  public boolean setAsFavorite(UserDetails user) {
    return false;
  }

  @Override
  public boolean removeAsFavorite(UserDetails user) {
    return false;
  }

  @Override
  public User getUser() {
    return null;
  }

  @Override
  public void setUser(User user) {}

  @Override
  public Access getAccess() {
    return null;
  }

  @Override
  public void setSharing(Sharing sharing) {}

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    return "";
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    return "";
  }

  @Override
  public int compareTo(@NotNull IdentifiableObject o) {
    return 0;
  }

  @Override
  public String getHref() {
    return "";
  }

  @Override
  public void setHref(String link) {}

  @Override
  public String getUid() {
    return "";
  }

  @Override
  public Sharing getSharing() {
    return null;
  }

  public Set<Translation> getTranslations() {
    return translations == null ? Collections.emptySet() : new HashSet<>();
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public User getCreatedBy() {
    return createdBy;
  }
}
