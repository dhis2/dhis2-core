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

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.CombinationGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Abyot Aselefew
 */
@Entity
@Table(name = "categorycombo")
@Setter
@JacksonXmlRootElement(localName = "categoryCombo", namespace = DxfNamespaces.DXF_2_0)
public class CategoryCombo extends BaseMetadataObject
    implements SystemDefaultMetadataObject, IdentifiableObject {
  public static final String DEFAULT_CATEGORY_COMBO_NAME = "default";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "categorycomboid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @ManyToMany(fetch = FetchType.LAZY)
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
  @ListIndexBase(1)
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<Category> categories = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "categoryCombo")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<CategoryOptionCombo> optionCombos = new HashSet<>();

  @Column(name = "datadimensiontype", nullable = false)
  @Enumerated(EnumType.STRING)
  private DataDimensionType dataDimensionType;

  @Column(name = "skiptotal", nullable = false)
  private boolean skipTotal;

  @Column(name = "sharing")
  @Type(type = "jsbObjectSharing")
  private Sharing sharing = new Sharing();

  @Embedded private TranslationProperty translations = new TranslationProperty();

  // -------------------------------------------------------------------------
  // Transient fields
  // -------------------------------------------------------------------------
  /** Access information for this object. Applies to current user. */
  @Transient private transient Access access;

  /**
   * As part of the serializing process, this field can be set to indicate a link to this
   * identifiable object (will be used on the web layer for navigating the REST API)
   */
  @Transient private transient String href;

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
  // hashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);

    return result;
  }

  /** Class check uses isAssignableFrom and get-methods to handle proxied objects. */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof IdentifiableObject
            && getRealClass(this) == getRealClass(obj)
            && typedEquals((IdentifiableObject) obj);
  }

  /**
   * Equality check against typed identifiable object. This method is not vulnerable to proxy
   * issues, where an uninitialized object class type fails comparison to a real class.
   *
   * @param other the identifiable object to compare this object against.
   * @return true if equal.
   */
  public final boolean typedEquals(IdentifiableObject other) {
    if (other == null) {
      return false;
    }
    return Objects.equals(getUid(), other.getUid())
        && Objects.equals(getCode(), other.getCode())
        && Objects.equals(getName(), other.getName());
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

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

  /**
   * Generates a set of all possible combinations of category option combos for this category combo.
   * This is done by generating all possible combinations of the category options in the categories
   * of this category combo. This used to return a list but that had the potential to return
   * duplicates if categories shared similar category options.
   *
   * @return a set of all possible combinations of category option combos for this category combo.
   */
  public Set<CategoryOptionCombo> generateOptionCombosSet() {
    // return default option combos if default
    if (this.isDefault()) return new HashSet<>(this.optionCombos);

    Set<CategoryOptionCombo> generatedOptionCombos = new HashSet<>();
    CombinationGenerator<CategoryOption> generator =
        CombinationGenerator.newInstance(getCategoryOptionsAsLists());

    while (generator.hasNext()) {
      CategoryOptionCombo optionCombo = new CategoryOptionCombo();
      optionCombo.setCategoryOptions(new HashSet<>(generator.getNext()));
      optionCombo.setCategoryCombo(this);
      generatedOptionCombos.add(optionCombo);

      for (CategoryOption categoryOption : optionCombo.getCategoryOptions()) {
        categoryOption.addCategoryOptionCombo(optionCombo);
      }
    }

    return generatedOptionCombos;
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

  @JsonProperty("isDefault")
  @Override
  public boolean isDefault() {
    return DEFAULT_CATEGORY_COMBO_NAME.equals(name);
  }

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
  @JsonSerialize(contentAs = IdentifiableObject.class)
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

  @Override
  @JsonProperty(value = "id")
  @JacksonXmlProperty(localName = "id", isAttribute = true)
  @Description("The Unique Identifier for this Object.")
  @Property(value = PropertyType.IDENTIFIER, required = Value.FALSE)
  @PropertyRange(min = 11, max = 11)
  public String getUid() {
    return uid;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The unique code for this Object.")
  @Property(PropertyType.IDENTIFIER)
  public String getCode() {
    return code;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The name of this Object. Required and unique.")
  @PropertyRange(min = 1)
  public String getName() {
    return name;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return translations.getTranslation("NAME", name);
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was created.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getCreated() {
    return created;
  }

  @Override
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was last updated.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getLastUpdated() {
    return lastUpdated;
  }

  @Override
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  @Override
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Sharing getSharing() {
    return sharing;
  }

  @Override
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(localName = "access", namespace = DxfNamespaces.DXF_2_0)
  public Access getAccess() {
    return access;
  }

  @Override
  @OpenApi.Ignore
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getUser() {
    return createdBy;
  }

  @Override
  public void setUser(User user) {
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
  }

  @Override
  @Sortable(value = false)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(isAttribute = true)
  @Property(PropertyType.URL)
  public String getHref() {
    return href;
  }

  @Override
  public void setHref(String href) {
    this.href = href;
  }

  @Override
  public void setOwner(String ownerId) {
    getSharing().setOwner(ownerId);
  }

  public void setTranslations(Set<Translation> translations) {
    this.translations.setTranslations(translations);
  }

  @Gist(included = Gist.Include.FALSE)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translations.getTranslations();
  }

  @JsonIgnore
  @Override
  public long getId() {
    return id;
  }

  @Override
  @Gist(included = Include.FALSE)
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getCreatedBy() {
    return createdBy;
  }

  // --------------------------------------------------
  // Copy methods from BaseIdentifiableObject
  // --------------------------------------------------

  /**
   * Returns the value of the property referred to by the given {@link IdScheme}.
   *
   * @param idScheme the {@link IdScheme}.
   * @return the value of the property referred to by the {@link IdScheme}.
   */
  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      return code;
    } else if (idScheme.is(IdentifiableProperty.NAME)) {
      return name;
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    } else if (idScheme.is(IdentifiableProperty.ATTRIBUTE)) {
      return null;
    }
    return null;
  }

  /**
   * Returns the value of the property referred to by the given {@link IdScheme}. If this happens to
   * refer to NAME, it returns the translatable/display version.
   *
   * @param idScheme the {@link IdScheme}.
   * @return the value of the property referred to by the {@link IdScheme}.
   */
  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    } else {
      return getPropertyValue(idScheme);
    }
  }

  // -------------------------------------------------
  // Not Supported methods overridden from IdentifiableObject
  // -------------------------------------------------

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    // DO NOTHING as this class does not have attributes
  }

  @Override
  public void addAttributeValue(String attributeUid, String value) {
    // DO NOTHING as this class does not have attributes
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    // DO NOTHING as this class does not have attributes
  }

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
}
