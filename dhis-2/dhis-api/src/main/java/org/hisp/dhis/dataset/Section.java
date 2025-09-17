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
package org.hisp.dhis.dataset;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseLinkableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

@JacksonXmlRootElement(localName = "section", namespace = DxfNamespaces.DXF_2_0)
@Setter
@Entity
@Table(name = "section")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Section extends BaseLinkableObject implements IdentifiableObject, MetadataObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "sectionid")
  private long id;

  @Column(unique = true, nullable = false, length = 11)
  private String uid;

  @Column(unique = true, length = 50)
  private String code;

  @Column(nullable = false, unique = true, length = 230)
  private String name;

  @Column(updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Column(name = "lastUpdated")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdated;

  @ManyToOne
  @JoinColumn(name = "lastupdatedby")
  private User lastUpdatedBy;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @ManyToOne
  @JoinColumn(name = "datasetid", nullable = false)
  private DataSet dataSet;

  @ManyToMany
  @JoinTable(
      name = "sectiondataelements",
      joinColumns = @JoinColumn(name = "sectionid"),
      inverseJoinColumns = @JoinColumn(name = "dataelementid"))
  @OrderColumn(name = "sort_order")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<DataElement> dataElements = new ArrayList<>();

  @ManyToMany
  @JoinTable(
      name = "sectionindicators",
      joinColumns = @JoinColumn(name = "sectionid"),
      inverseJoinColumns = @JoinColumn(name = "indicatorid"))
  @OrderColumn(name = "sort_order")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<Indicator> indicators = new ArrayList<>();

  @ManyToMany
  @Cascade(
      value = {
        org.hibernate.annotations.CascadeType.ALL,
        org.hibernate.annotations.CascadeType.DELETE_ORPHAN
      })
  @JoinTable(
      name = "sectiongreyedfields",
      joinColumns = @JoinColumn(name = "sectionid"),
      inverseJoinColumns = @JoinColumn(name = "dataelementoperandid"))
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<DataElementOperand> greyedFields = new HashSet<>();

  @Column(name = "sortorder", nullable = false)
  private int sortOrder;

  private boolean showRowTotals;

  private boolean showColumnTotals;

  @Column(name = "disabledataelementautogroup")
  private boolean disableDataElementAutoGroup;

  @Type(type = "jbPlainString")
  @Column(name = "displayoptions", length = 50000)
  private String displayOptions;

  @Type(type = "jsbAttributeValues")
  @AuditAttribute
  private AttributeValues attributeValues = AttributeValues.empty();

  // -------------------------------------------------------------------------
  // Transient properties
  // -------------------------------------------------------------------------

  @Transient private transient Access access;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Section() {}

  public Section(
      String name,
      DataSet dataSet,
      List<DataElement> dataElements,
      Set<DataElementOperand> greyedFields) {
    this.name = name;
    this.dataSet = dataSet;
    this.dataElements = dataElements;
    this.greyedFields = greyedFields;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean hasCategoryCombo() {
    return !getCategoryCombos().isEmpty();
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Set<CategoryCombo> getCategoryCombos() {
    Set<CategoryCombo> categoryCombos = new HashSet<>();

    for (DataElement dataElement : dataElements) {
      CategoryCombo categoryCombo = dataElement.getDataElementCategoryCombo(dataSet);

      if (categoryCombo != null) {
        categoryCombos.add(categoryCombo);
      }
    }

    return categoryCombos;
  }

  public boolean hasDataElements() {
    return dataElements != null && !dataElements.isEmpty();
  }

  public List<DataElement> getDataElementsByCategoryCombo(CategoryCombo categoryCombo) {
    List<DataElement> result = new ArrayList<>();

    for (DataElement dataElement : this.dataElements) {
      if (dataElement.getDataElementCategoryCombo(this.dataSet).equals(categoryCombo)) {
        result.add(dataElement);
      }
    }

    return result;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getDescription() {
    return description;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataSet getDataSet() {
    return dataSet;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "dataElements", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataElement", namespace = DxfNamespaces.DXF_2_0)
  public List<DataElement> getDataElements() {
    return dataElements;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "indicators", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "indicator", namespace = DxfNamespaces.DXF_2_0)
  public List<Indicator> getIndicators() {
    return indicators;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getSortOrder() {
    return sortOrder;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "greyedFields", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "greyedField", namespace = DxfNamespaces.DXF_2_0)
  public Set<DataElementOperand> getGreyedFields() {
    return greyedFields;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isShowRowTotals() {
    return showRowTotals;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isShowColumnTotals() {
    return showColumnTotals;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @OpenApi.Property(ObjectNode.class)
  public String getDisplayOptions() {
    return displayOptions;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isDisableDataElementAutoGroup() {
    return disableDataElementAutoGroup;
  }

  public void removeIndicator(Indicator i) {
    this.indicators.remove(i);
  }

  /**
   * Add an Indicator if it is not already present. This helps prevent duplicates in the list.
   *
   * @param i Indicator
   * @return whether the Indicator was added or not
   */
  public boolean addIndicator(@Nonnull Indicator i) {
    if (!this.indicators.contains(i)) {
      return this.indicators.add(i);
    } else return false;
  }

  public void removeIndicators(List<Indicator> sources) {
    for (Indicator i : sources) removeIndicator(i);
  }

  // -------------------------------------------------------------------------
  // IdentifiableObject interface implementation
  // -------------------------------------------------------------------------

  @Override
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

  public void setUid(String uid) {
    this.uid = uid;
  }

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
  public Date getLastUpdated() {
    return lastUpdated;
  }

  @Override
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @PropertyRange(min = 1)
  public String getCode() {
    return code;
  }

  @Override
  public void setCode(String code) {
    this.code = code;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @PropertyRange(min = 1)
  public String getName() {
    return name;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return translations.getTranslation("NAME", name);
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
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return getUid();
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
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof IdentifiableObject identifiableObject)) return false;
    return getRealClass(this) == getRealClass(obj) && typedEquals(identifiableObject);
  }

  public final boolean typedEquals(IdentifiableObject other) {
    if (other == null) {
      return false;
    }
    return Objects.equals(getUid(), other.getUid())
        && Objects.equals(getCode(), other.getCode())
        && Objects.equals(getName(), other.getName());
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translations.getTranslations();
  }

  @Override
  public void setAccess(Access access) {
    this.access = access;
  }

  public void setTranslations(Set<Translation> translations) {
    this.translations.setTranslations(translations);
  }

  /** Section does not have userid column. */
  public void setUser(User user) {}

  @Override
  public Access getAccess() {
    return access;
  }

  /** Section does not have userid column. */
  @Override
  @Transient
  @JsonIgnore
  public User getCreatedBy() {
    return null;
  }

  /** Section does not have userid column. */
  @Override
  public User getUser() {
    return null;
  }

  /** Section does not have userid column. */
  @Override
  public void setCreatedBy(User createdBy) {}

  /** Section does not have sharing. */
  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Sharing getSharing() {
    return Sharing.empty();
  }

  /** Section does not have sharing. */
  @Override
  public void setSharing(Sharing sharing) {}

  /** Section does not have userid column. */
  @Override
  public void setOwner(String ownerId) {}
}
