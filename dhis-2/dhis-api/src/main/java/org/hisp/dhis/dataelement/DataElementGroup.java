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

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

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
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TotalAggregationType;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Kristian Nordal
 */
@Entity
@Table(name = "dataelementgroup")
@Setter
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@JacksonXmlRootElement(localName = "dataElementGroup", namespace = DxfNamespaces.DXF_2_0)
public class DataElementGroup extends BaseMetadataObject
    implements DimensionalItemObject, MetadataObject {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "dataelementgroupid")
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

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "dataelementgroupmembers",
      joinColumns =
          @JoinColumn(
              name = "dataelementgroupid",
              foreignKey = @ForeignKey(name = "fk_dataelementgroupmembers_dataelementgroupid")),
      inverseJoinColumns =
          @JoinColumn(
              name = "dataelementid",
              foreignKey = @ForeignKey(name = "fk_dataelementgroup_dataelementid")))
  @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
  private Set<DataElement> members = new HashSet<>();

  /**
   * Inverse side of {@code dataelementgroupsetmembers}. The owning side is {@link
   * DataElementGroupSet#members}, which is still Hibernate HBM-mapped as an ordered {@code <list>}.
   */
  @ManyToMany(fetch = FetchType.LAZY, mappedBy = "members")
  @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
  private Set<DataElementGroupSet> groupSets = new HashSet<>();

  @Type(type = "jsbAttributeValues")
  @Column(name = "attributevalues")
  private AttributeValues attributeValues = AttributeValues.empty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  // -------------------------------------------------------------------------
  // DimensionalItemObject / NameableObject state (not persisted for this entity)
  // -------------------------------------------------------------------------

  @Transient private List<LegendSet> legendSets = new ArrayList<>();

  @Transient private transient QueryModifiers queryMods;

  @Transient private String formName;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DataElementGroup() {}

  public DataElementGroup(String name) {
    this.name = name;
    this.shortName = name;
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

  public void addDataElement(DataElement dataElement) {
    members.add(dataElement);
    dataElement.getGroups().add(this);
  }

  public void removeDataElement(DataElement dataElement) {
    members.remove(dataElement);
    dataElement.getGroups().remove(this);
  }

  public void removeAllDataElements() {
    for (DataElement dataElement : members) {
      dataElement.getGroups().remove(this);
    }

    members.clear();
  }

  public void updateDataElements(Set<DataElement> updates) {
    for (DataElement dataElement : new HashSet<>(members)) {
      if (!updates.contains(dataElement)) {
        removeDataElement(dataElement);
      }
    }

    for (DataElement dataElement : updates) {
      addDataElement(dataElement);
    }
  }

  /**
   * Returns the value type of the data elements in this group. Uses an arbitrary member to
   * determine the value type.
   */
  public ValueType getValueType() {
    return hasMembers() ? members.iterator().next().getValueType() : null;
  }

  /**
   * Returns the period type of the data elements in this group. Uses an arbitrary member to
   * determine the period type.
   */
  public PeriodType getPeriodType() {
    return hasMembers() ? members.iterator().next().getPeriodType() : null;
  }

  /** Indicates whether this group has a period type. */
  public boolean hasPeriodType() {
    return getPeriodType() != null;
  }

  /** Indicates whether this group has any members. */
  public boolean hasMembers() {
    return members != null && !members.isEmpty();
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonIgnore
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
  public String getName() {
    return name;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return translations.getTranslation("NAME", getName());
  }

  @Override
  @Sortable
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getShortName() {
    return shortName;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "shortName", key = "SHORT_NAME")
  public String getDisplayShortName() {
    return translations.getTranslation("SHORT_NAME", getShortName());
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

  @JsonProperty("dataElements")
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "dataElements", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataElement", namespace = DxfNamespaces.DXF_2_0)
  public Set<DataElement> getMembers() {
    return members;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "groupSets", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "groupSet", namespace = DxfNamespaces.DXF_2_0)
  public Set<DataElementGroupSet> getGroupSets() {
    return groupSets;
  }

  @Override
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

  // -------------------------------------------------------------------------
  // DimensionalItemObject
  // -------------------------------------------------------------------------

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
    return DimensionItemType.DATA_ELEMENT_GROUP;
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

  /**
   * Returns the aggregation type of the data elements in this group. Uses an arbitrary member to
   * determine the aggregation operator.
   */
  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AggregationType getAggregationType() {
    return hasMembers() ? members.iterator().next().getAggregationType() : null;
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
