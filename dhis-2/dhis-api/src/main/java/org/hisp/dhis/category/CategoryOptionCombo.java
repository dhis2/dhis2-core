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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.common.TotalAggregationType;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
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
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Abyot Aselefew
 */
@JacksonXmlRootElement(localName = "categoryOptionCombo", namespace = DxfNamespaces.DXF_2_0)
@Entity
@Setter
@Table(name = "categoryoptioncombo")
@SecondaryTable(
    name = "categorycombos_optioncombos",
    pkJoinColumns =
        @PrimaryKeyJoinColumn(
            name = "categoryoptioncomboid",
            foreignKey =
                @ForeignKey(name = "fk_categorycombos_optioncombos_categoryoptioncomboid")))
public class CategoryOptionCombo
    implements SystemDefaultMetadataObject, DimensionalItemObject, Serializable {
  public static final String DEFAULT_NAME = "default";

  public static final String DEFAULT_TOSTRING = "(default)";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "categoryoptioncomboid")
  private long id;

  @Column(name = "uid", unique = true, nullable = false, length = 11)
  @AuditAttribute
  private String uid;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  /**
   * Persisted via property access ({@link #getName()}) — matching the original HBM {@code
   * access="property"}. The name is derived from the category options when not set explicitly (e.g.
   * the system default combo persists the derived value "default"), so it must be written through
   * the getter, not the raw field.
   */
  @Transient private String name;

  @Column(name = "created", nullable = false, updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Column(name = "lastUpdated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdated;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lastupdatedby")
  private User lastUpdatedBy;

  /**
   * A category option combo is generated by the system, not created by a user, so no creator is
   * persisted (the {@code categoryoptioncombo} table has no {@code userid} column). Kept as a
   * transient field to satisfy the {@link IdentifiableObject} contract.
   */
  @Transient private User createdBy;

  /** As part of serializing, a link to this object for the REST API. */
  @Transient private String href;

  /** Access information for this object for the current user. */
  @Transient private transient Access access;

  /** The category combo. Mapped through the {@code categorycombos_optioncombos} join table. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "categorycomboid",
      table = "categorycombos_optioncombos",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_categorycombos_optioncombos_categorycomboid"))
  private CategoryCombo categoryCombo;

  /** The category options. */
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "categoryoptioncombos_categoryoptions",
      joinColumns =
          @JoinColumn(
              name = "categoryoptioncomboid",
              foreignKey =
                  @ForeignKey(
                      name = "fk_categoryoptioncombos_categoryoptions_categoryoptioncomboid")),
      inverseJoinColumns =
          @JoinColumn(
              name = "categoryoptionid",
              foreignKey = @ForeignKey(name = "fk_categoryoptioncombo_categoryoptionid")))
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<CategoryOption> categoryOptions = new HashSet<>();

  /** Indicates whether to ignore data approval. */
  @Column(name = "ignoreapproval")
  private boolean ignoreApproval;

  @Type(type = "jsbAttributeValues")
  @Column(name = "attributevalues")
  private AttributeValues attributeValues = AttributeValues.empty();

  @Embedded private TranslationProperty translations = new TranslationProperty();

  /**
   * Sharing is not persisted for this entity (no sharing column in the {@code categoryoptioncombo}
   * table). Kept transient to satisfy the {@link IdentifiableObject} contract.
   */
  @Transient private Sharing sharing = new Sharing();

  // -------------------------------------------------------------------------
  // DimensionalItemObject / NameableObject state (not persisted for this entity)
  // -------------------------------------------------------------------------

  @Transient private DimensionItemType dimensionItemType;

  @Transient private List<LegendSet> legendSets = new ArrayList<>();

  @Transient private AggregationType aggregationType;

  @Transient private transient QueryModifiers queryMods;

  @Transient private String description;

  @Transient private String formName;

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

  public void removeCategoryOptions(Collection<CategoryOption> categoryOptions) {
    categoryOptions.forEach(this::removeCategoryOption);
  }

  public void removeAllCategoryOptions() {
    categoryOptions.clear();
  }

  @Override
  public boolean isDefault() {
    return DEFAULT_NAME.equals(name);
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

  @JsonIgnore
  @Override
  public long getId() {
    return id;
  }

  @JsonProperty(value = "id")
  @JacksonXmlProperty(localName = "id", isAttribute = true)
  @PropertyRange(min = 11, max = 11)
  @Property(value = PropertyType.IDENTIFIER, required = Value.FALSE)
  @Override
  public String getUid() {
    return uid;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was created.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  @Override
  public Date getCreated() {
    return created;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was last updated.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  @Override
  public Date getLastUpdated() {
    return lastUpdated;
  }

  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Override
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @Gist(included = Include.FALSE)
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Override
  public User getCreatedBy() {
    return createdBy;
  }

  @OpenApi.Ignore
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Override
  public User getUser() {
    return createdBy;
  }

  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(localName = "access", namespace = DxfNamespaces.DXF_2_0)
  @Override
  public Access getAccess() {
    return access;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Override
  public String getHref() {
    return href;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getCode() {
    return code;
  }

  @Override
  @jakarta.persistence.Access(AccessType.PROPERTY)
  @Column(name = "name", columnDefinition = "text")
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
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

      for (CategoryOption option : categoryOptions) {
        if (options.contains(option)) {
          builder.append(option.getDisplayName()).append(", ");
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
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return translations.getTranslation("NAME", getName());
  }

  @JsonIgnore
  public String getShortName() {
    return getName();
  }

  public void setShortName(String shortName) {
    // Not supported
  }

  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public CategoryCombo getCategoryCombo() {
    return categoryCombo;
  }

  public void setCategoryCombo(CategoryCombo categoryCombo) {
    this.categoryCombo = categoryCombo;
  }

  @JsonProperty
  @JsonSerialize(contentAs = IdentifiableObject.class)
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

  @Override
  @JsonProperty("attributeValues")
  public AttributeValues getAttributeValues() {
    return attributeValues;
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    this.attributeValues = attributeValues == null ? AttributeValues.empty() : attributeValues;
  }

  @Override
  public void addAttributeValue(String attributeUid, String value) {
    this.attributeValues = this.attributeValues.added(attributeUid, value);
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    this.attributeValues = this.attributeValues.removed(attributeId);
  }

  @Gist(included = Include.FALSE)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translations.getTranslations();
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translations.setTranslations(translations);
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
  public void setSharing(Sharing sharing) {
    this.sharing = sharing;
  }

  @Override
  public void setUser(User user) {
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
  }

  @Override
  public void setOwner(String ownerId) {
    getSharing().setOwner(ownerId);
  }

  // --------------------------------------------------
  // IdScheme property value accessors
  // --------------------------------------------------

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      return code;
    } else if (idScheme.is(IdentifiableProperty.NAME)) {
      return getName();
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    }
    return null;
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    } else {
      return getPropertyValue(idScheme);
    }
  }

  // --------------------------------------------------
  // NameableObject implementation
  // --------------------------------------------------

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "shortName", key = "SHORT_NAME")
  public String getDisplayShortName() {
    return translations.getTranslation("SHORT_NAME", getShortName());
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  public String getDisplayDescription() {
    return translations.getTranslation("DESCRIPTION", getDescription());
  }

  @Override
  @JsonIgnore
  public String getDisplayProperty(DisplayProperty displayProperty) {
    if (DisplayProperty.SHORTNAME == displayProperty && getDisplayShortName() != null) {
      return getDisplayShortName();
    } else {
      return getDisplayName();
    }
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFormName() {
    return formName;
  }

  public void setFormName(String formName) {
    this.formName = formName;
  }

  /** Returns the form name, or the display name if it does not exist. */
  public String getFormNameFallback() {
    return formName != null && !formName.isEmpty() ? getFormName() : getDisplayName();
  }

  @JsonProperty
  @Sortable(whenPersisted = false)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "formName", key = "FORM_NAME")
  public String getDisplayFormName() {
    return translations.getTranslation("FORM_NAME", getFormNameFallback());
  }

  // --------------------------------------------------
  // DimensionalItemObject implementation
  // --------------------------------------------------

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDimensionItem() {
    return getUid();
  }

  @Override
  public String getDimensionItem(IdScheme idScheme) {
    return getPropertyValue(idScheme);
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DimensionItemType getDimensionItemType() {
    return DimensionItemType.DATA_ELEMENT_OPERAND;
  }

  public void setDimensionItemType(DimensionItemType dimensionItemType) {
    this.dimensionItemType = dimensionItemType;
  }

  @Override
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "legendSets", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "legendSets", namespace = DxfNamespaces.DXF_2_0)
  public List<LegendSet> getLegendSets() {
    return legendSets;
  }

  public void setLegendSets(List<LegendSet> legendSets) {
    this.legendSets = legendSets;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public LegendSet getLegendSet() {
    return legendSets.isEmpty() ? null : legendSets.get(0);
  }

  @Override
  public boolean hasLegendSet() {
    return legendSets != null && !legendSets.isEmpty();
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AggregationType getAggregationType() {
    return (queryMods != null && queryMods.getAggregationType() != null)
        ? queryMods.getAggregationType()
        : aggregationType;
  }

  public void setAggregationType(AggregationType aggregationType) {
    this.aggregationType = aggregationType;
  }

  @Override
  public boolean hasAggregationType() {
    return getAggregationType() != null;
  }

  @Override
  public TotalAggregationType getTotalAggregationType() {
    return getAggregationType() == AggregationType.NONE
        ? TotalAggregationType.NONE
        : TotalAggregationType.SUM;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public QueryModifiers getQueryMods() {
    return queryMods;
  }

  @Override
  public void setQueryMods(QueryModifiers queryMods) {
    this.queryMods = queryMods;
  }
}
