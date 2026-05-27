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

import static org.hisp.dhis.common.DimensionConstants.OPTION_SEP;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Lists;
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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.QueryKey;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.AnalyticsType;
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
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.eventvisualization.EventRepetition;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.hibernate.HibernateProxyUtils;

/**
 * A Category is a dimension of a data element. DataElements can have sets of dimensions (known as
 * CategoryCombos). An Example of a Category might be "Sex". The Category could have two (or more)
 * CategoryOptions such as "Male" and "Female".
 *
 * @author Abyot Asalefew
 */
@Entity
@Table(name = "category")
@Setter
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "category", namespace = DxfNamespaces.DXF_2_0)
public class Category extends BaseMetadataObject
    implements DimensionalObject, SystemDefaultMetadataObject {
  public static final String DEFAULT_NAME = "default";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "categoryid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @Column(name = "shortname", nullable = false, unique = true, length = 50)
  private String shortName;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "datadimensiontype", nullable = false)
  private DataDimensionType dataDimensionType;

  @Column(name = "datadimension", nullable = false)
  private boolean dataDimension = true;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "categories_categoryoptions",
      joinColumns =
          @JoinColumn(
              name = "categoryid",
              foreignKey = @ForeignKey(name = "fk_categories_categoryoptions_categoryid")),
      inverseJoinColumns =
          @JoinColumn(
              name = "categoryoptionid",
              foreignKey = @ForeignKey(name = "fk_category_categoryoptionid")))
  @OrderColumn(name = "sort_order")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<CategoryOption> categoryOptions = new ArrayList<>();

  @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<CategoryCombo> categoryCombos = new HashSet<>();

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Type(type = "jsbAttributeValues")
  @Column(name = "attributevalues")
  private AttributeValues attributeValues = AttributeValues.empty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  @Transient private transient String dimensionName;
  @Transient private String dimensionDisplayName;
  @Transient private ValueType valueType;
  @Transient private OptionSet optionSet;
  @Transient private boolean allItems;
  @Transient private LegendSet legendSet;
  @Transient private ProgramStage programStage;
  @Transient private Program program;
  @Transient private AggregationType aggregationType;
  @Transient private String filter;
  @Transient private EventRepetition eventRepetition;
  @Transient private DimensionItemKeywords dimensionItemKeywords;
  @Transient private boolean fixed;
  @Transient private UUID groupUUID;

  public Category() {}

  public Category(String name, DataDimensionType dataDimensionType) {
    this.name = name;
    this.shortName = name;
    this.dataDimensionType = dataDimensionType;
  }

  public Category(
      String name, DataDimensionType dataDimensionType, List<CategoryOption> categoryOptions) {
    this(name, dataDimensionType);
    this.categoryOptions = categoryOptions;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof Category other
            && HibernateProxyUtils.getRealClass(this) == HibernateProxyUtils.getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName())
            && Objects.equals(getShortName(), other.getShortName())
            && Objects.equals(getDescription(), other.getDescription());
  }

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (getShortName() != null ? getShortName().hashCode() : 0);
    result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
    return result;
  }

  public void addCategoryOption(CategoryOption categoryOption) {
    if (!hasCategoryOption(categoryOption)) {
      categoryOptions.add(categoryOption);
      categoryOption.getCategories().add(this);
    }
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
  @JsonProperty("isDefault")
  public boolean isDefault() {
    return DEFAULT_NAME.equals(name);
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDimension() {
    return getUid();
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
  public String getDimensionName() {
    return dimensionName != null ? dimensionName : getUid();
  }

  @Override
  public String getDimensionDisplayName() {
    return dimensionDisplayName;
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

  @Override
  @JsonProperty
  @JsonSerialize(contentAs = DimensionalItemObject.class)
  @JacksonXmlElementWrapper(localName = "items", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "item", namespace = DxfNamespaces.DXF_2_0)
  public List<DimensionalItemObject> getItems() {
    return Lists.newArrayList(categoryOptions);
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isAllItems() {
    return allItems;
  }

  @Override
  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public LegendSet getLegendSet() {
    return legendSet;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramStage getProgramStage() {
    return programStage;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Program getProgram() {
    return program;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AggregationType getAggregationType() {
    return aggregationType;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFilter() {
    return filter;
  }

  @Override
  @JsonProperty("repetition")
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public EventRepetition getEventRepetition() {
    return eventRepetition;
  }

  @Override
  @JsonIgnore
  public AnalyticsType getAnalyticsType() {
    return AnalyticsType.AGGREGATE;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isDataDimension() {
    return dataDimension;
  }

  @Override
  @JsonIgnore
  public boolean isFixed() {
    return fixed;
  }

  @Override
  @JsonIgnore
  public List<String> getFilterItemsAsList() {
    final String inOp = QueryOperator.IN.getValue().toLowerCase();
    final int opLen = inOp.length() + 1;

    if (filter == null || !filter.toLowerCase().startsWith(inOp) || filter.length() < opLen) {
      return null;
    }

    String filterItems = filter.substring(opLen);
    return new ArrayList<>(Arrays.asList(filterItems.split(OPTION_SEP)));
  }

  @Override
  @JsonIgnore
  public String getKey() {
    QueryKey key = new QueryKey();
    key.add("dimension", getDimension());
    getItems().forEach(item -> key.add("item", item.getDimensionItem()));

    return key.add("allItems", allItems)
        .addIgnoreNull("legendSet", legendSet)
        .addIgnoreNull("aggregationType", aggregationType)
        .addIgnoreNull("filter", filter)
        .asPlainKey();
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DimensionItemKeywords getDimensionItemKeywords() {
    return dimensionItemKeywords;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Property(PropertyType.IDENTIFIER)
  public String getCode() {
    return code;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
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
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getShortName() {
    return shortName;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "shortName", key = "SHORT_NAME")
  public String getDisplayShortName() {
    return translations.getTranslation("SHORT_NAME", shortName);
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDescription() {
    return description;
  }

  @Override
  @Sortable(value = false)
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
    }
    return getDisplayName();
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

  @Override
  @JsonIgnore
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
  @JsonIgnore
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    }
    return getPropertyValue(idScheme);
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
  public void setOwner(String owner) {
    getSharing().setOwner(owner);
  }

  @Override
  public void setUser(User user) {
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
  }

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

  private boolean hasCategoryOption(CategoryOption categoryOption) {
    return this.categoryOptions.stream()
        .anyMatch(co -> co.getUid().equals(categoryOption.getUid()));
  }
}
