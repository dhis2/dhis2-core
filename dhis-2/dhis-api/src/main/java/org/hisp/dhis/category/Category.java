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
import com.google.common.collect.Lists;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.Type;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.AnalyticsType;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseIdentifiableObject.AttributeValue;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dimensional.DimensionalProperties;
import org.hisp.dhis.eventvisualization.EventRepetition;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * A Category is a dimension of a data element. DataElements can have sets of dimensions (known as
 * CategoryCombos). An Example of a Category might be "Sex". The Category could have two (or more)
 * CategoryOptions such as "Male" and "Female".
 *
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement(localName = "category", namespace = DxfNamespaces.DXF_2_0)
@Entity
@Setter
@Table(name = "category")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Category extends BaseMetadataObject
    implements DimensionalObject, SystemDefaultMetadataObject {
  public static final String DEFAULT_NAME = "default";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "categoryid")
  private long id;

  @Column(nullable = false, unique = true, length = 230)
  private String name;

  @Column(nullable = false, unique = true, length = 50)
  private String shortName;

  @Column(columnDefinition = "text")
  private String description;

  @Column(length = 50, unique = true, nullable = false)
  private String code;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  /**
   * The data dimension type of this dimension. Can be null. Only applicable for {@link
   * DimensionType#CATEGORY}.
   */
  @Column(name = "datadimensiontype", nullable = false)
  @Type(type = "org.hisp.dhis.common.DataDimensionTypeUserType")
  private DataDimensionType dataDimensionType;

  @ManyToMany
  @JoinTable(
      name = "categories_categoryoptions",
      joinColumns = @JoinColumn(name = "categoryid"),
      inverseJoinColumns = @JoinColumn(name = "categoryoptionid"))
  @OrderColumn(name = "sort_order")
  @ListIndexBase(value = 1)
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<CategoryOption> categoryOptions = new ArrayList<>();

  @ManyToMany(mappedBy = "categories")
  private Set<CategoryCombo> categoryCombos = new HashSet<>();

  /** Indicates whether this object should be handled as a data dimension. */
  @Column(name = "datadimension", nullable = false)
  private boolean dataDimension = true;

  @AuditAttribute
  @Type(type = "jsbAttributeValues")
  private AttributeValues attributeValues = AttributeValues.empty();

  @Type(type = "jsbObjectSharing")
  private Sharing sharing = new Sharing();

  // -------------------------------------------------------------------------
  // Transient properties
  // -------------------------------------------------------------------------

  @Transient
  private transient DimensionalProperties dimensionalProperties = new DimensionalProperties();

  /** The name of this dimension. */
  @Transient private transient String dimensionName;

  /** The display name to use for this dimension. */
  @Transient private transient String dimensionDisplayName;

  /** Holds the value type of the parent dimension. */
  @Transient private transient ValueType valueType;

  /** The option set associated with the dimension, if any. */
  @Transient private transient OptionSet optionSet;

  /** Indicates whether all available items in this dimension are included. */
  @Transient private transient boolean allItems;

  /** The legend set for this dimension. */
  @Transient private transient LegendSet legendSet;

  /** The program stage for this dimension. */
  @Transient private transient ProgramStage programStage;

  /** The program for this dimension. */
  @Transient private transient Program program;

  /** The aggregation type for this dimension. */
  @Transient private transient AggregationType aggregationType;

  /** Filter. Applicable for events. Contains operator and filter. */
  @Transient private transient String filter;

  /** Applicable only for events. Holds the indexes relate to the repetition object. */
  @Transient private transient EventRepetition eventRepetition;

  /** Defines a pre-defined group of items. */
  @Transient private transient DimensionItemKeywords dimensionItemKeywords;

  /** Indicates whether this dimension is fixed. */
  @Transient private transient boolean fixed;

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
  // hashCode and equals
  // -------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof Category other
            && HibernateProxyUtils.getRealClass(this) == HibernateProxyUtils.getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName());
  }

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    return result;
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

  public void removeCategoryOptions(Collection<CategoryOption> categoryOptions) {
    categoryOptions.forEach(this::removeCategoryOption);
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
  @JsonSerialize(contentAs = DimensionalItemObject.class)
  @JacksonXmlElementWrapper(localName = "items", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "item", namespace = DxfNamespaces.DXF_2_0)
  public List<DimensionalItemObject> getItems() {
    return Lists.newArrayList(categoryOptions);
  }

  @Override
  public DimensionType getDimensionType() {
    return DimensionType.CATEGORY;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataDimensionType getDataDimensionType() {
    return dataDimensionType;
  }

  @Override
  public DimensionalProperties getDimensionalProperties() {
    return dimensionalProperties;
  }

  @Override
  public String getDimensionName() {
    return dimensionName != null ? dimensionName : uid;
  }

  @Override
  public String getDimensionDisplayName() {
    return dimensionDisplayName;
  }

  // ------------------------------------------------------------------------
  // Getters and setters
  // ------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "categoryOptions", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryOption", namespace = DxfNamespaces.DXF_2_0)
  public List<CategoryOption> getCategoryOptions() {
    return categoryOptions;
  }

  @JsonProperty
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "categoryCombos", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryCombo", namespace = DxfNamespaces.DXF_2_0)
  public Set<CategoryCombo> getCategoryCombos() {
    return categoryCombos;
  }

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

  @Override
  public void setDimensionName(String dimensionName) {
    this.dimensionalProperties.setDimensionName(dimensionName);
  }

  /** Indicates whether this object should be handled as a data dimension. Persistent property. */
  @Override
  public boolean isDataDimension() {
    return dataDimension;
  }

  @Override
  public String getShortName() {
    return shortName;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Sharing getSharing() {
    return sharing;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ValueType getValueType() {
    return valueType;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public OptionSet getOptionSet() {
    return optionSet;
  }

  // -------------------------------------------------------------------------
  // Not supported dimensional object methods
  // -------------------------------------------------------------------------

  /** Indicates whether all available items in this dimension are included. */
  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isAllItems() {
    return allItems;
  }

  /** Gets the legend set. */
  @Override
  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public LegendSet getLegendSet() {
    return legendSet;
  }

  /** Gets the program stage (not persisted). */
  @Override
  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramStage getProgramStage() {
    return programStage;
  }

  /** Gets the program (not persisted). */
  @Override
  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Program getProgram() {
    return program;
  }

  /** Gets the aggregation type. */
  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AggregationType getAggregationType() {
    return aggregationType;
  }

  /** Gets the filter. Contains operator and filter. Applicable for events. */
  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFilter() {
    return filter;
  }

  /** Gets the events repetition. Only applicable for events. */
  @Override
  @JsonProperty("repetition")
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public EventRepetition getEventRepetition() {
    return eventRepetition;
  }

  /** Indicates the analytics type of this dimensional object. */
  @Override
  public AnalyticsType getAnalyticsType() {
    return null;
  }

  @Override
  public void setEventRepetition(EventRepetition eventRepetition) {
    this.eventRepetition = eventRepetition;
  }

  /**
   * Indicates whether this dimension is fixed, meaning that the name of the dimension will be
   * returned as is for all dimension items in the response.
   */
  @Override
  @JsonIgnore
  public boolean isFixed() {
    return fixed;
  }

  @Override
  public List<String> getFilterItemsAsList() {
    return List.of();
  }

  /** Returns a unique key representing this dimension. */
  @Override
  public String getKey() {
    return "";
  }

  @Override
  public void setFixed(boolean fixed) {
    this.fixed = fixed;
  }

  /** Returns dimension item keywords for this dimension. */
  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DimensionItemKeywords getDimensionItemKeywords() {
    return dimensionItemKeywords;
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
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  public String getDisplayDescription() {
    return translations.getTranslation("DESCRIPTION", description);
  }

  @Override
  public String getDisplayProperty(DisplayProperty property) {
    if (DisplayProperty.SHORTNAME == property && getDisplayShortName() != null) {
      return getDisplayShortName();
    } else {
      return getDisplayName();
    }
  }

  @Override
  public String getCode() {
    return this.code;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return translations.getTranslation("NAME", name);
  }

  @Gist(included = Include.FALSE)
  @Override
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translations.getTranslations();
  }

  /**
   * @param user
   * @deprecated This method is replaced by {@link #setCreatedBy(User)} ()} Currently it is only
   *     used for web api backward compatibility
   */
  @Override
  public void setUser(User user) {
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
  }

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
      return attributeValues.get(idScheme.getAttribute());
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

  @Override
  public void setId(long id) {
    this.id = id;
  }

  @Override
  public void setOwner(String owner) {
    getSharing().setOwner(owner);
  }

  /** Clears out cache when setting translations. */
  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translations.setTranslations(translations);
  }

  @Override
  @JsonIgnore
  public long getId() {
    return id;
  }
}
