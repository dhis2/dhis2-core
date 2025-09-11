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
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

@JacksonXmlRootElement(localName = "section", namespace = DxfNamespaces.DXF_2_0)
@Setter
@Entity
@Table(name = "section")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Section extends BaseMetadataObject implements IdentifiableObject, MetadataObject {
  
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "sectionid")
  private long id;

  @Column(name = "code", unique = true, nullable = true, length = 50)
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Embedded 
  private TranslationProperty translations = new TranslationProperty();

  @ManyToOne
  @JoinColumn(name = "datasetid", nullable = false)
  private DataSet dataSet;

  @ManyToMany
  @JoinTable(name = "sectiondataelements", 
    joinColumns = @JoinColumn(name = "sectionid"),
    inverseJoinColumns = @JoinColumn(name = "dataelementid"))
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<DataElement> dataElements = new ArrayList<>();

  @ManyToMany
  @JoinTable(name = "sectionindicators",
    joinColumns = @JoinColumn(name = "sectionid"),
    inverseJoinColumns = @JoinColumn(name = "indicatorid"))
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<Indicator> indicators = new ArrayList<>();

  @ManyToMany
  @JoinTable(name = "sectiongreyedfields",
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

  private transient Sharing sharing;

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

  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataSet getDataSet() {
    return dataSet;
  }

  public void setDataSet(DataSet dataSet) {
    this.dataSet = dataSet;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "dataElements", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataElement", namespace = DxfNamespaces.DXF_2_0)
  public List<DataElement> getDataElements() {
    return dataElements;
  }

  public void setDataElements(List<DataElement> dataElements) {
    this.dataElements = dataElements;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "indicators", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "indicator", namespace = DxfNamespaces.DXF_2_0)
  public List<Indicator> getIndicators() {
    return indicators;
  }

  public void setIndicators(List<Indicator> indicators) {
    this.indicators = indicators;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "greyedFields", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "greyedField", namespace = DxfNamespaces.DXF_2_0)
  public Set<DataElementOperand> getGreyedFields() {
    return greyedFields;
  }

  public void setGreyedFields(Set<DataElementOperand> greyedFields) {
    this.greyedFields = greyedFields;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isShowRowTotals() {
    return showRowTotals;
  }

  public void setShowRowTotals(boolean showRowTotals) {
    this.showRowTotals = showRowTotals;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isShowColumnTotals() {
    return showColumnTotals;
  }

  public void setShowColumnTotals(boolean showColumnTotals) {
    this.showColumnTotals = showColumnTotals;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @OpenApi.Property(ObjectNode.class)
  public String getDisplayOptions() {
    return displayOptions;
  }

  public void setDisplayOptions(String displayOptions) {
    this.displayOptions = displayOptions;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isDisableDataElementAutoGroup() {
    return disableDataElementAutoGroup;
  }

  public void setDisableDataElementAutoGroup(boolean disableDataElementAutoGroup) {
    this.disableDataElementAutoGroup = disableDataElementAutoGroup;
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

  public void setName(String name) {
    this.name = name;
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

  public void setTranslations(Set<Translation> translations) {
    this.translations.setTranslations(translations);
  }

  public void setUser(User user) {
    setCreatedBy(getCreatedBy() == null ? user : getCreatedBy());
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Sharing getSharing() {
    if (sharing == null) {
      sharing = new Sharing();
    }
    return sharing;
  }

  @Override
  public void setSharing(Sharing sharing) {
    this.sharing = sharing;
  }

  @Override
  public void setOwner(String ownerId) {
    getSharing().setOwner(ownerId);
  }

}
