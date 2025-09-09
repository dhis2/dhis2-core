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

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseIdentifiableObject.AttributeValue;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.common.TotalAggregationType;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.DailyPeriodType;
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
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;

/**
 * @author Abyot Asalefew
 */
@Entity
@Table(name = "categoryoption")
@Setter
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "categoryOption", namespace = DXF_2_0)
public class CategoryOption extends BaseMetadataObject
    implements DimensionalItemObject, SystemDefaultMetadataObject, Serializable {

  public static final String DEFAULT_NAME = "default";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "categoryoptionid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @Column(name = "shortname", nullable = false, unique = true, length = 50)
  private String shortName;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "formname", length = 230)
  private String formName;

  @Temporal(TemporalType.DATE)
  private Date startDate;

  @Temporal(TemporalType.DATE)
  private Date endDate;

  @Type(type = "jbObjectStyle")
  private ObjectStyle style;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @AuditAttribute
  @Type(type = "jsbAttributeValues")
  private AttributeValues attributeValues = AttributeValues.empty();

  @ManyToMany
  @JoinTable(
      name = "categoryoption_organisationunits",
      joinColumns = @JoinColumn(name = "categoryoptionid"),
      inverseJoinColumns = @JoinColumn(name = "organisationunitid"))
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  @BatchSize(size = 100)
  private Set<OrganisationUnit> organisationUnits = new HashSet<>();

  @ManyToMany(mappedBy = "categoryOptions")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  @BatchSize(size = 100)
  private Set<Category> categories = new HashSet<>();

  @ManyToMany(mappedBy = "categoryOptions")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<CategoryOptionCombo> categoryOptionCombos = new HashSet<>();

  @ManyToMany(mappedBy = "members")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<CategoryOptionGroup> groups = new HashSet<>();

  @Type(type = "jsbObjectSharing")
  private Sharing sharing = new Sharing();

  // -----------------------------------------------------------------------------
  // Transient fields
  // -----------------------------------------------------------------------------

  @Transient private transient QueryModifiers queryMods;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public CategoryOption() {}

  public CategoryOption(String name) {
    this.name = name;
    this.shortName = name;
  }

  // -------------------------------------------------------------------------
  // hashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof CategoryOption other
            && HibernateProxyUtils.getRealClass(this) == HibernateProxyUtils.getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName())
            && Objects.equals(getShortName(), other.getShortName())
            && Objects.equals(queryMods, other.queryMods);
  }

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (getShortName() != null ? getShortName().hashCode() : 0);
    result = 31 * result + (queryMods != null ? queryMods.hashCode() : 0);
    return result;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  @JsonProperty("isDefault")
  @Override
  public boolean isDefault() {
    return DEFAULT_NAME.equals(name);
  }

  /**
   * Returns a set of category option group sets which are associated with the category option
   * groups of this category option.
   */
  public Set<CategoryOptionGroupSet> getGroupSets() {
    Set<CategoryOptionGroupSet> groupSets = new HashSet<>();

    if (groups != null) {
      for (CategoryOptionGroup group : groups) {
        groupSets.addAll(group.getGroupSets());
      }
    }

    return groupSets;
  }

  public void addCategoryOptionCombo(CategoryOptionCombo dataElementCategoryOptionCombo) {
    categoryOptionCombos.add(dataElementCategoryOptionCombo);
    dataElementCategoryOptionCombo.getCategoryOptions().add(this);
  }

  public void addCategoryOptionCombos(Collection<CategoryOptionCombo> categoryOptionCombos) {
    categoryOptionCombos.forEach(this::addCategoryOptionCombo);
  }

  public void removeCategoryOptionCombo(CategoryOptionCombo dataElementCategoryOptionCombo) {
    categoryOptionCombos.remove(dataElementCategoryOptionCombo);
    dataElementCategoryOptionCombo.getCategoryOptions().remove(this);
  }

  public void removeCategoryOptionCombos(Collection<CategoryOptionCombo> categoryOptionCombos) {
    categoryOptionCombos.forEach(this::removeCategoryOptionCombo);
  }

  public void addOrganisationUnit(OrganisationUnit organisationUnit) {
    organisationUnits.add(organisationUnit);
    organisationUnit.getCategoryOptions().add(this);
  }

  public void addOrganisationUnits(Set<OrganisationUnit> organisationUnits) {
    organisationUnits.forEach(this::addOrganisationUnit);
  }

  public void removeOrganisationUnit(OrganisationUnit organisationUnit) {
    organisationUnits.remove(organisationUnit);
    organisationUnit.getCategoryOptions().remove(this);
  }

  public void removeOrganisationUnits(Set<OrganisationUnit> organisationUnits) {
    organisationUnits.forEach(this::removeOrganisationUnit);
  }

  /**
   * Gets an adjusted end date, adjusted if this data set has open periods after the end date.
   *
   * @param dataSet the data set to adjust for
   * @return the adjusted end date
   */
  public Date getAdjustedEndDate(DataSet dataSet) {
    if (endDate == null || dataSet.getOpenPeriodsAfterCoEndDate() == 0) {
      return endDate;
    }

    return dataSet
        .getPeriodType()
        .getRewindedDate(endDate, -dataSet.getOpenPeriodsAfterCoEndDate());
  }

  /**
   * Gets an adjusted end date, adjusted if a data element belongs to any data sets that have open
   * periods after the end date. If so, it chooses the latest end date.
   *
   * @param dataElement the data element to adjust for
   * @return the adjusted end date
   */
  public Date getAdjustedEndDate(DataElement dataElement) {
    if (endDate == null) {
      return null;
    }

    Date latestAdjustedDate = endDate;

    for (DataSetElement element : dataElement.getDataSetElements()) {
      Date adjustedDate = getAdjustedEndDate(element.getDataSet());

      if (adjustedDate.after(latestAdjustedDate)) {
        latestAdjustedDate = adjustedDate;
      }
    }

    return latestAdjustedDate;
  }

  /**
   * Gets an adjusted end date, adjusted if this program has open days after the end date.
   *
   * @param program the program to adjust for
   * @return the adjusted end date
   */
  public Date getAdjustedEndDate(Program program) {
    if (endDate == null || program.getOpenDaysAfterCoEndDate() == 0) {
      return endDate;
    }

    return (new DailyPeriodType()).getRewindedDate(endDate, -program.getOpenDaysAfterCoEndDate());
  }

  // -------------------------------------------------------------------------
  // DimensionalItemObject implementation
  // -------------------------------------------------------------------------

  @Override
  public String getDimensionItem() {
    return getUid();
  }

  @Override
  public String getDimensionItem(IdScheme idScheme) {
    return getPropertyValue(idScheme);
  }

  @Override
  public DimensionItemType getDimensionItemType() {
    return DimensionItemType.CATEGORY_OPTION;
  }

  @Override
  public AggregationType getAggregationType() {
    return (queryMods != null && queryMods.getAggregationType() != null)
        ? queryMods.getAggregationType()
        : null;
  }

  @Override
  public boolean hasAggregationType() {
    return false;
  }

  @Override
  public TotalAggregationType getTotalAggregationType() {
    return null;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public QueryModifiers getQueryMods() {
    return queryMods;
  }

  // -------------------------------------------------------------------------
  // IdentifiableObject implementation
  // -------------------------------------------------------------------------

  @Override
  @JsonIgnore
  public long getId() {
    return id;
  }

  @Override
  @JsonProperty(value = "id")
  @JacksonXmlProperty(localName = "id", isAttribute = true)
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
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was last updated.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getLastUpdated() {
    return lastUpdated;
  }

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
  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }

  @Override
  public void setUser(User user) {
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
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
  public void setAccess(Access access) {
    this.access = access;
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
  @JsonIgnore
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      return code;
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    } else if (idScheme.is(IdentifiableProperty.NAME)) {
      return name;
    } else if (idScheme.is(IdentifiableProperty.ATTRIBUTE)) {
      return attributeValues.get(idScheme.getAttribute());
    }
    return null;
  }

  @Override
  @JsonIgnore
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    } else {
      return getPropertyValue(idScheme);
    }
  }

  // -------------------------------------------------------------------------
  // AttributeValues
  // -------------------------------------------------------------------------

  @Override
  @OpenApi.Property(AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return attributeValues;
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    this.attributeValues = attributeValues == null ? AttributeValues.empty() : attributeValues;
  }

  @Override
  public void addAttributeValue(String attributeId, String value) {
    this.attributeValues = attributeValues.added(attributeId, value);
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    this.attributeValues = attributeValues.removed(attributeId);
  }

  @JsonIgnore
  public String getAttributeValue(String attributeUid) {
    return attributeValues.get(attributeUid);
  }

  // -------------------------------------------------------------------------
  // Getters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getStartDate() {
    return startDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getEndDate() {
    return endDate;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0)
  public Set<OrganisationUnit> getOrganisationUnits() {
    return organisationUnits;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "categories", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "category", namespace = DxfNamespaces.DXF_2_0)
  public Set<Category> getCategories() {
    return categories;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "categoryOptionCombos", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryOptionCombo", namespace = DxfNamespaces.DXF_2_0)
  public Set<CategoryOptionCombo> getCategoryOptionCombos() {
    return categoryOptionCombos;
  }

  @JsonProperty("categoryOptionGroups")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "categoryOptionGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryOptionGroup", namespace = DxfNamespaces.DXF_2_0)
  public Set<CategoryOptionGroup> getGroups() {
    return groups;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ObjectStyle getStyle() {
    return style;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getFormName() {
    return formName;
  }

  // -------------------------------------------------------------------------
  // NameableObject implementation
  // -------------------------------------------------------------------------

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getShortName() {
    return shortName;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "shortName", key = "SHORTNAME")
  public String getDisplayShortName() {
    return translations.getTranslation("SHORTNAME", shortName);
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDescription() {
    return description;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  public String getDisplayDescription() {
    return translations.getTranslation("DESCRIPTION", description);
  }

  @Override
  @JsonIgnore
  public String getDisplayProperty(DisplayProperty property) {
    if (DisplayProperty.SHORTNAME == property && getDisplayShortName() != null) {
      return getDisplayShortName();
    } else {
      return getDisplayName();
    }
  }

  @Override
  public void setOwner(String owner) {
    getSharing().setOwner(owner);
  }

  // -------------------------------------------------------------------------
  // Sharing helpers
  // -------------------------------------------------------------------------

  public void setExternalAccess(boolean externalAccess) {
    getSharing().setExternal(externalAccess);
  }

  public void setPublicAccess(String access) {
    getSharing().setPublicAccess(access);
  }

  public String getPublicAccess() {
    return getSharing().getPublicAccess();
  }

  public Collection<UserAccess> getUserAccesses() {
    return getSharing().getUsers().values();
  }

  public Collection<UserGroupAccess> getUserGroupAccesses() {
    return getSharing().getUserGroups().values();
  }

  // -------------------------------------------------------------------------
  // Deprecated methods for non-mapped properties
  // -------------------------------------------------------------------------

  /** Category options do not have legendSets, so this method is deprecated. */
  @Override
  @Deprecated(since = "43", forRemoval = true)
  public List<LegendSet> getLegendSets() {
    return List.of();
  }

  /** Category options do not have a legend set, so this method is deprecated. */
  @Override
  @Deprecated(since = "43", forRemoval = true)
  public LegendSet getLegendSet() {
    return null;
  }

  @Override
  public boolean hasLegendSet() {
    return false;
  }
}
