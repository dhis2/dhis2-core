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

import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;
import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TotalAggregationType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.PropertyType;
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
 * This object can act both as a hydrated persisted object and as a wrapper object (but not both at
 * the same time).
 *
 * <p>This object implements IdentifiableObject but does not have any UID. Instead the UID is
 * generated based on the data element and category option combo which this object is based on.
 *
 * @author Abyot Asalefew
 */
@Entity
@Table(name = "dataelementoperand")
@Setter
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@JacksonXmlRootElement(localName = "dataElementOperand", namespace = DxfNamespaces.DXF_2_0)
public class DataElementOperand implements EmbeddedObject, ValueTypedDimensionalItemObject {
  public static final String SEPARATOR = COMPOSITE_DIM_OBJECT_PLAIN_SEP;

  private static final String SPACE = " ";

  // -------------------------------------------------------------------------
  // Properties
  // -------------------------------------------------------------------------

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "dataelementoperandid")
  private long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "dataelementid",
      foreignKey = @ForeignKey(name = "fk_dataelementoperand_dataelement"))
  private DataElement dataElement;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "categoryoptioncomboid",
      foreignKey = @ForeignKey(name = "fk_dataelementoperand_dataelementcategoryoptioncombo"))
  private CategoryOptionCombo categoryOptionCombo;

  /** Not mapped in the database - implemented this way intentionally. */
  @Transient private CategoryOptionCombo attributeOptionCombo;

  /**
   * {@code queryMods} is the only IdentifiableObject/DimensionalItemObject property below that
   * carries real, live state for this entity: expression parsing (e.g. {@code periodOffset()}/
   * {@code yearToDate()} in indicator expressions) sets it, and analytics query planning and
   * period-shifting read it back (see {@code PeriodOffsetUtils}, {@code
   * QueryPlannerUtils#getQueryModsElementMap}). Every other IdentifiableObject property is a no-op
   * below - the {@code dataelementoperand} table has no uid/code/name/created/lastupdated/userid/
   * sharing/translations/legendsets columns, and nothing reads a value back from them.
   */
  @Transient private transient QueryModifiers queryMods;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DataElementOperand() {
    setAutoFields();
  }

  public DataElementOperand(DataElement dataElement) {
    this.dataElement = dataElement;
  }

  public DataElementOperand(DataElement dataElement, CategoryOptionCombo categoryOptionCombo) {
    this.dataElement = dataElement;
    this.categoryOptionCombo = categoryOptionCombo;
  }

  public DataElementOperand(
      DataElement dataElement,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo) {
    this.dataElement = dataElement;
    this.categoryOptionCombo = categoryOptionCombo;
    this.attributeOptionCombo = attributeOptionCombo;
  }

  // -------------------------------------------------------------------------
  // ValueTypedDimensionalItemObject
  // -------------------------------------------------------------------------

  @Override
  public boolean hasOptionSet() {
    return dataElement.hasOptionSet();
  }

  @Override
  public OptionSet getOptionSet() {
    return dataElement.getOptionSet();
  }

  @Override
  public ValueType getValueType() {
    return dataElement.getValueType();
  }

  // -------------------------------------------------------------------------
  // DimensionalItemObject
  // -------------------------------------------------------------------------

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDimensionItem() {
    return getDimensionItem(IdScheme.UID);
  }

  @Override
  public String getDimensionItem(IdScheme idScheme) {
    String item = null;

    if (dataElement != null) {
      item = dataElement.getPropertyValue(idScheme);

      if (categoryOptionCombo != null) {
        item += SEPARATOR + categoryOptionCombo.getPropertyValue(idScheme);
      } else if (attributeOptionCombo != null) {
        item += SEPARATOR + SYMBOL_WILDCARD;
      }

      if (attributeOptionCombo != null) {
        item += SEPARATOR + attributeOptionCombo.getPropertyValue(idScheme);
      }
    }

    return item;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DimensionItemType getDimensionItemType() {
    return DimensionItemType.DATA_ELEMENT_OPERAND;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AggregationType getAggregationType() {
    return dataElement.getAggregationType();
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

  /** This entity does not support legend sets. */
  @Override
  @Deprecated
  @JsonIgnore
  public List<LegendSet> getLegendSets() {
    // A fresh mutable list, not List.of(): generic reflection-based tooling (e.g.
    // DefaultMetadataMergeService.merge()) calls clear()/addAll() on whatever a collection
    // getter returns for any collection property that still has a setter.
    return new ArrayList<>();
  }

  /** This entity does not support legend sets. */
  @Override
  @Deprecated
  @JsonIgnore
  public LegendSet getLegendSet() {
    return null;
  }

  /** This entity does not support legend sets. */
  @Override
  @Deprecated
  public boolean hasLegendSet() {
    return false;
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

  // -------------------------------------------------------------------------
  // IdentifiableObject
  // -------------------------------------------------------------------------

  @JsonIgnore
  @Override
  public long getId() {
    return id;
  }

  @Override
  @JsonProperty(value = "id")
  @JacksonXmlProperty(localName = "id", isAttribute = true)
  @Description("The Unique Identifier for this Object.")
  @Property(value = PropertyType.IDENTIFIER, required = Value.FALSE)
  @PropertyRange(min = 11, max = 11)
  public String getUid() {
    String uid = null;

    if (dataElement != null) {
      uid = dataElement.getUid();
    }

    if (categoryOptionCombo != null && !categoryOptionCombo.isDefault()) {
      uid += SEPARATOR + categoryOptionCombo.getUid();
    }

    return uid;
  }

  @Override
  public void setUid(String uid) {
    // Not supported - uid is always computed from the data element and category option combo
  }

  @Override
  @JsonIgnore
  public String getCode() {
    return null;
  }

  @Override
  public void setCode(String code) {
    // Not supported
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The name of this Object. Required and unique.")
  @PropertyRange(min = 1)
  public String getName() {
    String name = null;

    if (dataElement != null) {
      name = dataElement.getName();
    }

    if (hasNonDefaultCategoryOptionCombo()) {
      name += SPACE + categoryOptionCombo.getName();
    } else if (hasNonDefaultAttributeOptionCombo()) {
      name += SPACE + SYMBOL_WILDCARD;
    }

    if (hasNonDefaultAttributeOptionCombo()) {
      name += SPACE + attributeOptionCombo.getName();
    }

    return name;
  }

  @Override
  public void setName(String name) {
    // Not supported - name is always computed from the data element and category option combo
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    String displayName = null;

    if (dataElement != null) {
      displayName = dataElement.getDisplayName();
    }

    if (hasNonDefaultCategoryOptionCombo()) {
      displayName += SPACE + categoryOptionCombo.getDisplayName();
    } else if (hasNonDefaultAttributeOptionCombo()) {
      displayName += SPACE + SYMBOL_WILDCARD;
    }

    if (hasNonDefaultAttributeOptionCombo()) {
      displayName += SPACE + attributeOptionCombo.getDisplayName();
    }

    return displayName;
  }

  @Override
  @JsonIgnore
  public Date getCreated() {
    return null;
  }

  @Override
  public void setCreated(Date created) {
    // Not supported
  }

  @Override
  @JsonIgnore
  public User getLastUpdatedBy() {
    return null;
  }

  @Override
  public void setLastUpdatedBy(User lastUpdatedBy) {
    // Not supported
  }

  @Override
  @JsonIgnore
  public Date getLastUpdated() {
    return null;
  }

  @Override
  public void setLastUpdated(Date lastUpdated) {
    // Not supported
  }

  /** This entity does not support attribute values. */
  @Override
  @Deprecated
  @JsonIgnore
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** This entity does not support attribute values. */
  @Override
  @Deprecated
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** This entity does not support attribute values. */
  @Override
  @Deprecated
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** This entity does not support attribute values. */
  @Override
  @Deprecated
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }

  /** This entity does not support translations. */
  @Override
  @Deprecated
  @JsonIgnore
  public Set<Translation> getTranslations() {
    // A fresh mutable set, not Set.of(): generic reflection-based tooling (e.g.
    // DefaultMetadataMergeService.merge()) calls clear()/addAll() on whatever a collection
    // getter returns for any collection property that still has a setter - confirmed live,
    // this threw UnsupportedOperationException during a DataSet PUT with
    // compulsoryDataElementOperands.
    return new HashSet<>();
  }

  /** This entity does not support translations. */
  @Override
  @Deprecated
  public void setTranslations(Set<Translation> translations) {
    // Not supported
  }

  /**
   * @deprecated This method is replaced by {@link #getCreatedBy()}
   */
  @Override
  @Deprecated
  @OpenApi.Ignore
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getUser() {
    return getCreatedBy();
  }

  @Override
  @JsonIgnore
  public User getCreatedBy() {
    return null; // Not supported - dataelementoperand table has no userid column
  }

  @Override
  public void setCreatedBy(User createdBy) {
    // Not supported - dataelementoperand table has no userid column
  }

  /**
   * @deprecated This method is replaced by {@link #setCreatedBy(User)}
   */
  @Override
  @Deprecated
  public void setUser(User user) {
    setCreatedBy(user);
  }

  @Override
  @Deprecated
  public void setOwner(String ownerId) {
    // Not supported
  }

  @Override
  @JsonIgnore
  public String getHref() {
    return null;
  }

  @Override
  public void setHref(String href) {
    // Not supported
  }

  @Override
  @JsonIgnore
  public Access getAccess() {
    return null;
  }

  @Override
  public void setAccess(Access access) {
    // Not supported
  }

  @Override
  @Deprecated
  @JsonIgnore
  public Sharing getSharing() {
    return Sharing.empty();
  }

  @Override
  @Deprecated
  public void setSharing(Sharing sharing) {
    // Not supported
  }

  @Override
  public boolean hasSharing() {
    return false;
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return getUid();
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      return getCode();
    } else if (idScheme.is(IdentifiableProperty.NAME)) {
      return getName();
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    } else if (idScheme.is(IdentifiableProperty.ATTRIBUTE)) {
      return getAttributeValues().get(idScheme.getAttribute());
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
  // NameableObject
  // -------------------------------------------------------------------------

  @Override
  @Sortable
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @PropertyRange(min = 1, max = 50)
  public String getShortName() {
    String shortName = null;

    if (dataElement != null) {
      shortName = dataElement.getShortName();
    }

    if (hasNonDefaultCategoryOptionCombo()) {
      shortName += SPACE + categoryOptionCombo.getShortName();
    } else if (hasNonDefaultAttributeOptionCombo()) {
      shortName += SPACE + SYMBOL_WILDCARD;
    }

    if (hasNonDefaultAttributeOptionCombo()) {
      shortName += SPACE + attributeOptionCombo.getName();
    }

    return shortName;
  }

  public void setShortName(String shortName) {
    // Not supported - shortName is always computed from the data element and category option
    // combo
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "shortName", key = "SHORT_NAME")
  public String getDisplayShortName() {
    String displayShortName = null;

    if (dataElement != null) {
      displayShortName = dataElement.getDisplayShortName();
    }

    if (hasNonDefaultCategoryOptionCombo()) {
      displayShortName += SPACE + categoryOptionCombo.getDisplayShortName();
    } else if (hasNonDefaultAttributeOptionCombo()) {
      displayShortName += SPACE + SYMBOL_WILDCARD;
    }

    if (hasNonDefaultAttributeOptionCombo()) {
      displayShortName += SPACE + attributeOptionCombo.getDisplayShortName();
    }

    return displayShortName;
  }

  @Override
  @JsonIgnore
  public String getDescription() {
    return null;
  }

  public void setDescription(String description) {
    // Not supported
  }

  @Override
  @JsonIgnore
  public String getDisplayDescription() {
    return getDescription();
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

  /** Returns the form name, or the name if it does not exist. */
  public String getFormNameFallback() {
    return getDisplayName();
  }

  @JsonIgnore
  public String getFormName() {
    return null;
  }

  public void setFormName(String formName) {
    // Not supported
  }

  @JsonProperty
  @Sortable(whenPersisted = false)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "formName", key = "FORM_NAME")
  public String getDisplayFormName() {
    return getFormNameFallback();
  }

  /**
   * Creates a {@link DataElementOperand} instance from the given identifiers.
   *
   * @param dataElementUid the data element identifier.
   * @param categoryOptionComboUid the category option combo identifier.
   * @return a data element operand instance.
   */
  public static DataElementOperand instance(String dataElementUid, String categoryOptionComboUid) {
    DataElement de = new DataElement();
    de.setUid(dataElementUid);

    CategoryOptionCombo coc = null;

    if (categoryOptionComboUid != null) {
      coc = new CategoryOptionCombo();
      coc.setUid(categoryOptionComboUid);
    }

    return new DataElementOperand(de, coc);
  }

  /** Indicates whether a category option combination exists which is different from default. */
  public boolean hasNonDefaultCategoryOptionCombo() {
    return categoryOptionCombo != null && !categoryOptionCombo.isDefault();
  }

  /** Indicates whether an attribute option combination exists which is different from default. */
  public boolean hasNonDefaultAttributeOptionCombo() {
    return attributeOptionCombo != null && !attributeOptionCombo.isDefault();
  }

  // -------------------------------------------------------------------------
  // Getters & setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataElement getDataElement() {
    return dataElement;
  }

  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public CategoryOptionCombo getCategoryOptionCombo() {
    return categoryOptionCombo;
  }

  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public CategoryOptionCombo getAttributeOptionCombo() {
    return attributeOptionCombo;
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof DataElementOperand other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getName(), other.getName())
            && Objects.equals(queryMods, other.queryMods)
            && objectEquals(other);
  }

  private boolean objectEquals(DataElementOperand other) {
    return Objects.equals(dataElement, other.dataElement)
        && Objects.equals(categoryOptionCombo, other.categoryOptionCombo)
        && Objects.equals(attributeOptionCombo, other.attributeOptionCombo);
  }

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (queryMods != null ? queryMods.hashCode() : 0);

    return Objects.hash(result, dataElement, categoryOptionCombo, attributeOptionCombo);
  }

  @Override
  public String toString() {
    return "{"
        + "\"class\":\""
        + getClass()
        + "\", "
        + "\"id\":\""
        + id
        + "\", "
        + "\"uid\":\""
        + getUid()
        + "\", "
        + "\"dataElement\":"
        + dataElement
        + ", "
        + "\"categoryOptionCombo\":"
        + categoryOptionCombo
        + "\"attributeOptionCombo\":"
        + attributeOptionCombo
        + '}';
  }

  // -------------------------------------------------------------------------
  // Option combination type
  // -------------------------------------------------------------------------

  public enum TotalType {
    COC_ONLY(true, false, 1),
    AOC_ONLY(false, true, 1),
    COC_AND_AOC(true, true, 2),
    NONE(false, false, 0);

    private boolean coc;

    private boolean aoc;

    private int propertyCount;

    TotalType() {}

    TotalType(boolean coc, boolean aoc, int propertyCount) {
      this.coc = coc;
      this.aoc = aoc;
      this.propertyCount = propertyCount;
    }

    public boolean isCategoryOptionCombo() {
      return coc;
    }

    public boolean isAttributeOptionCombo() {
      return aoc;
    }

    public int getPropertyCount() {
      return propertyCount;
    }
  }

  public TotalType getTotalType() {
    if (categoryOptionCombo != null && attributeOptionCombo != null) {
      return TotalType.COC_AND_AOC;
    } else if (categoryOptionCombo != null) {
      return TotalType.COC_ONLY;
    } else if (attributeOptionCombo != null) {
      return TotalType.AOC_ONLY;
    } else {
      return TotalType.NONE;
    }
  }
}
