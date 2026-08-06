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
import com.google.common.base.MoreObjects;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ListIndexBase;
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
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.eventvisualization.EventRepetition;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Lars Helge Overland
 */
@Entity
@Table(name = "categoryoptiongroupset")
@Setter
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "categoryOptionGroupSet", namespace = DxfNamespaces.DXF_2_0)
public class CategoryOptionGroupSet extends BaseMetadataObject implements DimensionalObject {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "categoryoptiongroupsetid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @Column(name = "shortname", nullable = false, unique = true, length = 50)
  private String shortName;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "datadimension", nullable = false)
  private boolean dataDimension = true;

  @Enumerated(EnumType.STRING)
  @Column(name = "datadimensiontype", nullable = false)
  private DataDimensionType dataDimensionType;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "categoryoptiongroupsetmembers",
      joinColumns =
          @JoinColumn(
              name = "categoryoptiongroupsetid",
              foreignKey =
                  @ForeignKey(name = "fk_categoryoptiongroupsetmembers_categoryoptiongroupsetid")),
      inverseJoinColumns =
          @JoinColumn(
              name = "categoryoptiongroupid",
              foreignKey =
                  @ForeignKey(name = "fk_categoryoptiongroupsetmembers_categoryoptiongroupid")))
  @OrderColumn(name = "sort_order")
  @ListIndexBase(1)
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<CategoryOptionGroup> members = new ArrayList<>();

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Type(type = "jsbAttributeValues")
  @Column(name = "attributevalues")
  private AttributeValues attributeValues = AttributeValues.empty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  // -------------------------------------------------------------------------
  // DimensionalObject state (not persisted for this entity)
  // -------------------------------------------------------------------------

  @Transient private transient String dimensionName;
  @Transient private transient String dimensionDisplayName;
  @Transient private transient ValueType valueType;
  @Transient private transient OptionSet optionSet;
  @Transient private transient boolean allItems;
  @Transient private transient LegendSet legendSet;
  @Transient private transient ProgramStage programStage;
  @Transient private transient Program program;
  @Transient private transient AggregationType aggregationType;
  @Transient private transient String filter;
  @Transient private transient EventRepetition eventRepetition;
  @Transient private transient DimensionItemKeywords dimensionItemKeywords;
  @Transient private transient boolean fixed;
  @Transient private transient UUID groupUUID;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public CategoryOptionGroupSet() {}

  public CategoryOptionGroupSet(String name) {
    this.name = name;
    this.shortName = name;
  }

  public CategoryOptionGroupSet(String name, DataDimensionType dataDimensionType) {
    this(name);
    this.dataDimensionType = dataDimensionType;
  }

  // -------------------------------------------------------------------------
  // hashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (getShortName() != null ? getShortName().hashCode() : 0);
    result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof CategoryOptionGroupSet other
            && HibernateProxyUtils.getRealClass(this) == HibernateProxyUtils.getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName())
            && Objects.equals(getShortName(), other.getShortName())
            && Objects.equals(getDescription(), other.getDescription());
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  // TODO link group set to category to avoid conflicting grouping of category
  // option combos

  public CategoryOptionGroup getGroup(CategoryOptionCombo optionCombo) {
    Set<CategoryOption> categoryOptions = optionCombo.getCategoryOptions();

    for (CategoryOptionGroup group : members) {
      if (!CollectionUtils.intersection(group.getMembers(), categoryOptions).isEmpty()) {
        return group;
      }
    }

    return null;
  }

  public void addCategoryOptionGroup(CategoryOptionGroup categoryOptionGroup) {
    members.add(categoryOptionGroup);
    categoryOptionGroup.getGroupSets().add(this);
  }

  public void removeCategoryOptionGroup(CategoryOptionGroup categoryOptionGroup) {
    members.remove(categoryOptionGroup);
    categoryOptionGroup.getGroupSets().remove(this);
  }

  // -------------------------------------------------------------------------
  // Dimensional object
  // -------------------------------------------------------------------------

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDimension() {
    return getUid();
  }

  @Override
  public DimensionType getDimensionType() {
    return DimensionType.CATEGORY_OPTION_GROUP_SET;
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
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JsonSerialize(contentAs = DimensionalItemObject.class)
  @JacksonXmlElementWrapper(localName = "items", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "item", namespace = DxfNamespaces.DXF_2_0)
  public List<DimensionalItemObject> getItems() {
    return new ArrayList<>(members);
  }

  @Override
  public void setItems(List<DimensionalItemObject> items) {
    this.members =
        items.stream().map(CategoryOptionGroup.class::cast).collect(Collectors.toList());
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
      return List.of();
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

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty("categoryOptionGroups")
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "categoryOptionGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryOptionGroup", namespace = DxfNamespaces.DXF_2_0)
  public List<CategoryOptionGroup> getMembers() {
    return members;
  }

  public void setMembers(List<CategoryOptionGroup> members) {
    this.members = members;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
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
  @JacksonXmlProperty(isAttribute = true)
  @PropertyRange(min = 1, max = 50)
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

  @Override
  public String toString() {
    List<String> itemStr =
        members.stream()
            .map(
                item ->
                    MoreObjects.toStringHelper(DimensionalItemObject.class)
                        .add("uid", item.getUid())
                        .add("name", item.getName())
                        .toString())
            .collect(Collectors.toList());

    return MoreObjects.toStringHelper(this)
        .add("dimension", uid)
        .add("type", DimensionType.CATEGORY_OPTION_GROUP_SET)
        .add("dimension display name", dimensionDisplayName)
        .add("items", itemStr)
        .toString();
  }
}
