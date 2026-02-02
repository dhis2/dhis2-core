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
package org.hisp.dhis.dataapproval;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * Records the approval of DataSet values for a given OrganisationUnit and Period.
 *
 * @author Jim Grace
 */
@JacksonXmlRootElement(localName = "dataApprovalLevel", namespace = DxfNamespaces.DXF_2_0)
@Entity
@Table(
    name = "dataapprovallevel",
    uniqueConstraints =
        @UniqueConstraint(
            name = "dataapprovallevel_orgunitlevel_categoryoptiongroupset_unique_key",
            columnNames = {"orgunitlevel", "categoryoptiongroupsetid"}))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DataApprovalLevel extends BaseMetadataObject implements IdentifiableObject {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "dataapprovallevelid")
  private long id;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  /** The data approval level, 1=highest level, max=lowest level. */
  @Column(name = "level", nullable = false)
  private int level;

  /** The organisation unit level for this data approval level. */
  @Column(name = "orgunitlevel", nullable = false)
  private int orgUnitLevel;

  /** The category option group set (optional) for this data approval level. */
  @ManyToOne
  @JoinColumn(
      name = "categoryoptiongroupsetid",
      foreignKey = @ForeignKey(name = "fK_dataapprovallevel_categoryoptiongroupsetid"))
  private CategoryOptionGroupSet categoryOptionGroupSet;

  /** The name of the organisation unit level (derived through the service.) */
  private transient String orgUnitLevelName;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DataApprovalLevel() {}

  public DataApprovalLevel(String name, int orgUnitLevel) {
    this.name = name;
    this.orgUnitLevel = orgUnitLevel;
  }

  public DataApprovalLevel(
      String name, int orgUnitLevel, CategoryOptionGroupSet categoryOptionGroupSet) {
    this(name, orgUnitLevel);
    this.categoryOptionGroupSet = categoryOptionGroupSet;
  }

  public DataApprovalLevel(DataApprovalLevel level) {
    this.name = level.name;
    this.level = level.level;
    this.orgUnitLevel = level.orgUnitLevel;
    this.categoryOptionGroupSet = level.categoryOptionGroupSet;
    this.created = level.created;
    this.lastUpdated = level.lastUpdated;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /**
   * Returns the name of the category option group set for this data approval level, or an empty
   * string if there is no category option group set.
   *
   * @return name of this approval level's category option group set.
   */
  public String getCategoryOptionGroupSetName() {
    return categoryOptionGroupSet == null ? "" : categoryOptionGroupSet.getName();
  }

  /** Indicates whether this approval level specified a category option group set. */
  public boolean hasCategoryOptionGroupSet() {
    return categoryOptionGroupSet != null;
  }

  /** Indicates whether the given approval level represents the same level as this. */
  public boolean levelEquals(DataApprovalLevel other) {
    if (other == null) {
      return false;
    }

    if (level != other.getLevel()) {
      return false;
    }

    if (categoryOptionGroupSet != null
        ? !categoryOptionGroupSet.equals(other.getCategoryOptionGroupSet())
        : other.getCategoryOptionGroupSet() != null) {
      return false;
    }

    return true;
  }

  // -------------------------------------------------------------------------
  // hashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);

    return result;
  }

  /** Class check uses isAssignableFrom and get-methods to handle proxied objects. */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof IdentifiableObject other
            && getRealClass(this) == getRealClass(obj)
            && typedEquals(other);
  }

  /**
   * Equality check against typed identifiable object. This method is not vulnerable to proxy
   * issues, where an uninitialized object class type fails comparison to a real class.
   *
   * @param other the identifiable object to compare this object against.
   * @return true if equal.
   */
  public final boolean typedEquals(IdentifiableObject other) {
    if (other == null) {
      return false;
    }
    return Objects.equals(getUid(), other.getUid())
        && Objects.equals(getName(), other.getName());
  }

  // -------------------------------------------------------------------------
  // toString
  // -------------------------------------------------------------------------

  @Override
  public String toString() {
    return "DataApprovalLevel{"
        + "name="
        + name
        + ", level="
        + level
        + ", orgUnitLevel="
        + orgUnitLevel
        + ", categoryOptionGroupSet='"
        + (categoryOptionGroupSet == null ? "(null)" : categoryOptionGroupSet.getName())
        + "'"
        + ", created="
        + created
        + ", lastUpdated="
        + lastUpdated
        + '}';
  }

  // -------------------------------------------------------------------------
  // Getters and Setters
  // -------------------------------------------------------------------------

  @Override
  @JsonIgnore
  public long getId() {
    return id;
  }

  @Override
  public void setId(long id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Set<org.hisp.dhis.translation.Translation> getTranslations() {
    if (translations == null) {
      return new HashSet<>();
    }
    return translations.getTranslations();
  }

  @Override
  public void setTranslations(Set<org.hisp.dhis.translation.Translation> translations) {
    if (this.translations == null) {
      this.translations = new TranslationProperty();
    }
    this.translations.setTranslations(translations);
  }

  @Override
  public String getDisplayName() {
    return getName();
  }

  @Override
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

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(
      value = PropertyType.INTEGER,
      required = Property.Value.TRUE,
      persisted = Property.Value.TRUE,
      owner = Property.Value.TRUE)
  public int getOrgUnitLevel() {
    return orgUnitLevel;
  }

  public void setOrgUnitLevel(int orgUnitLevel) {
    this.orgUnitLevel = orgUnitLevel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(
      value = PropertyType.REFERENCE,
      required = Property.Value.DEFAULT,
      persisted = Property.Value.TRUE,
      owner = Property.Value.TRUE)
  public CategoryOptionGroupSet getCategoryOptionGroupSet() {
    return categoryOptionGroupSet;
  }

  public void setCategoryOptionGroupSet(CategoryOptionGroupSet categoryOptionGroupSet) {
    this.categoryOptionGroupSet = categoryOptionGroupSet;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getOrgUnitLevelName() {
    return orgUnitLevelName;
  }

  public void setOrgUnitLevelName(String orgUnitLevelName) {
    this.orgUnitLevelName = orgUnitLevelName;
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.UID)) {
      return uid;
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
  // IdentifiableObject interface methods - Not supported by this entity
  // -------------------------------------------------------------------------

  /**
   * This entity does not support attribute values.
   *
   * @return Empty AttributeValues
   * @deprecated This method is not supported by DataApprovalLevel
   */
  @Override
  @Deprecated
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /**
   * This entity does not support setting attribute values.
   *
   * @deprecated This method is not supported by DataApprovalLevel
   */
  @Override
  @Deprecated
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported - no-op
  }

  /**
   * This entity does not support adding attribute values.
   *
   * @deprecated This method is not supported by DataApprovalLevel
   */
  @Override
  @Deprecated
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported - no-op
  }

  /**
   * This entity does not support removing attribute values.
   *
   * @deprecated This method is not supported by DataApprovalLevel
   */
  @Override
  @Deprecated
  public void removeAttributeValue(String attributeId) {
    // Not supported - no-op
  }

  @Override
  public void setUser(User user) {
    setCreatedBy(user);
  }

  /**
   * This entity does not support setting owner directly.
   *
   * @deprecated This method is not supported by DataApprovalLevel
   */
  @Override
  @Deprecated
  public void setOwner(String owner) {
    // Not supported - no-op
  }

  @Deprecated
  @Override
  public String getCode() {
    return "";
  }

  @Deprecated
  @Override
  public void setCode(String code) {
    // Not supported - no-op
  }
}
