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
package org.hisp.dhis.dataelement;

import static org.hisp.dhis.common.DimensionConstants.OPTION_SEP;
import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.Type;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.QueryKey;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.AnalyticsType;
import org.hisp.dhis.common.BaseDimensionalItemObject;
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
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.Sortable;
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

/**
 * DataElementGroupSet is a set of DataElementGroups. It is by default exclusive, in the sense that
 * a DataElement can only be a member of one or zero of the DataElementGroups in a
 * DataElementGroupSet.
 *
 * @author Lars Helge Overland
 */
@Entity
@Table(name = "dataelementgroupset")
@Setter
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@JacksonXmlRootElement(localName = "dataElementGroupSet", namespace = DxfNamespaces.DXF_2_0)
public class DataElementGroupSet extends BaseMetadataObject
    implements DimensionalObject, MetadataObject {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "dataelementgroupsetid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @Column(name = "shortname", nullable = false, unique = true, length = 50)
  private String shortName;

  @Type(type = "text")
  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "compulsory")
  private Boolean compulsory = false;

  @Column(name = "datadimension", nullable = false)
  private boolean dataDimension = true;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "dataelementgroupsetmembers",
      joinColumns =
          @JoinColumn(
              name = "dataelementgroupsetid",
              foreignKey =
                  @ForeignKey(name = "fk_dataelementgroupsetmembers_dataelementgroupsetid")),
      inverseJoinColumns =
          @JoinColumn(
              name = "dataelementgroupid",
              foreignKey = @ForeignKey(name = "fk_dataelementgroupset_dataelementgroupid")))
  @OrderColumn(name = "sort_order")
  @ListIndexBase(1)
  @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
  private List<DataElementGroup> members = new ArrayList<>();

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
  @Transient private transient DataDimensionType dataDimensionType;
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
  @Getter @Transient private transient UUID groupUUID;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DataElementGroupSet() {}

  public DataElementGroupSet(String name) {
    this.name = name;
    this.compulsory = false;
  }

  public DataElementGroupSet(String name, Boolean compulsory) {
    this(name);
    this.compulsory = compulsory;
  }

  public DataElementGroupSet(String name, String description, Boolean compulsory) {
    this(name, compulsory);
    this.description = description;
  }

  public DataElementGroupSet(
      String name, String description, boolean compulsory, boolean dataDimension) {
    this(name, description, compulsory);
    this.dataDimension = dataDimension;
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
        || obj instanceof DataElementGroupSet other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName())
            && Objects.equals(getShortName(), other.getShortName())
            && Objects.equals(getDescription(), other.getDescription());
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void addDataElementGroup(DataElementGroup dataElementGroup) {
    members.add(dataElementGroup);
    dataElementGroup.getGroupSets().add(this);
  }

  public void removeDataElementGroup(DataElementGroup dataElementGroup) {
    members.remove(dataElementGroup);
    dataElementGroup.getGroupSets().remove(this);
  }

  public void removeAllDataElementGroups() {
    for (DataElementGroup dataElementGroup : members) {
      dataElementGroup.getGroupSets().remove(this);
    }

    members.clear();
  }

  public Collection<DataElement> getDataElements() {
    List<DataElement> dataElements = new ArrayList<>();

    for (DataElementGroup group : members) {
      dataElements.addAll(group.getMembers());
    }

    return dataElements;
  }

  public DataElementGroup getGroup(DataElement dataElement) {
    for (DataElementGroup group : members) {
      if (group.getMembers().contains(dataElement)) {
        return group;
      }
    }

    return null;
  }

  public Boolean isMemberOfDataElementGroups(DataElement dataElement) {
    for (DataElementGroup group : members) {
      if (group.getMembers().contains(dataElement)) {
        return true;
      }
    }

    return false;
  }

  public Boolean hasDataElementGroups() {
    return members != null && members.size() > 0;
  }

  public List<DataElementGroup> getSortedGroups() {
    List<DataElementGroup> sortedGroups = new ArrayList<>(members);

    Collections.sort(sortedGroups);

    return sortedGroups;
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
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DimensionType getDimensionType() {
    return DimensionType.DATA_ELEMENT_GROUP_SET;
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
  @JsonDeserialize(contentAs = BaseDimensionalItemObject.class)
  @JacksonXmlElementWrapper(localName = "items", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "item", namespace = DxfNamespaces.DXF_2_0)
  public List<DimensionalItemObject> getItems() {
    return Lists.newArrayList(members);
  }

  /** Items are derived from {@link #members}; setting them directly has no effect. */
  @Override
  public void setItems(List<DimensionalItemObject> items) {
    // Not supported
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
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramStage getProgramStage() {
    return programStage;
  }

  @Override
  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
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

  /**
   * Returns the items in the filter as a list. Order of items are preserved. Requires that the
   * filter has the IN operator and that at least one item is specified in the filter, returns null
   * if not.
   */
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

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean isCompulsory() {
    if (compulsory == null) {
      return false;
    }

    return compulsory;
  }

  public void setCompulsory(Boolean compulsory) {
    this.compulsory = compulsory;
  }

  @JsonProperty("dataElementGroups")
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "dataElementGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataElementGroup", namespace = DxfNamespaces.DXF_2_0)
  public List<DataElementGroup> getMembers() {
    return members;
  }

  public void setMembers(List<DataElementGroup> members) {
    this.members = members;
  }

  @Override
  @JsonIgnore
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
  @Sortable
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
  @Sortable
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
  @Sortable
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
  public String getDisplayProperty(DisplayProperty displayProperty) {
    if (DisplayProperty.SHORTNAME == displayProperty && getDisplayShortName() != null) {
      return getDisplayShortName();
    }

    String displayName = getDimensionDisplayName();
    return displayName == null || displayName.isBlank() ? getDisplayName() : displayName;
  }

  @Override
  @Sortable(value = false)
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
        .add("type", DimensionType.DATA_ELEMENT_GROUP_SET)
        .add("dimension display name", dimensionDisplayName)
        .add("items", itemStr)
        .toString();
  }
}
