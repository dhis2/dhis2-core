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
package org.hisp.dhis.legend;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Jan Henrik Overland
 */
@Entity
@Table(
    name = "maplegend",
    indexes = {
      @Index(name = "maplegend_startvalue", columnList = "startvalue"),
      @Index(name = "maplegend_endvalue", columnList = "endvalue")
    })
@JacksonXmlRootElement(localName = "legend", namespace = DxfNamespaces.DXF_2_0)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Setter
public class Legend implements IdentifiableObject, EmbeddedObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "maplegendid")
  private long id;

  // Fields from BaseMetadataObject (except createdBy)
  @Column(name = "uid", unique = true, nullable = false, length = 11)
  protected String uid;

  @Column(name = "created", nullable = false, updatable = false)
  @jakarta.persistence.Temporal(jakarta.persistence.TemporalType.TIMESTAMP)
  protected Date created;

  @Column(name = "lastupdated", nullable = false)
  @jakarta.persistence.Temporal(jakarta.persistence.TemporalType.TIMESTAMP)
  protected Date lastUpdated;

  @ManyToOne
  @JoinColumn(name = "lastupdatedby")
  protected User lastUpdatedBy;

  @jakarta.persistence.Transient protected String href;

  @jakarta.persistence.Transient protected org.hisp.dhis.security.acl.Access access;

  // Legend-specific fields
  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "name", length = 255)
  private String name;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Column(name = "startvalue")
  private Double startValue;

  @Column(name = "endvalue")
  private Double endValue;

  @Column(name = "color")
  private String color;

  @Column(name = "image")
  private String image;

  @ManyToOne
  @JoinColumn(name = "maplegendsetid")
  private LegendSet legendSet;

  public Legend() {}

  public Legend(String name, Double startValue, Double endValue, String color, String image) {
    this.name = name;
    this.startValue = startValue;
    this.endValue = endValue;
    this.color = color;
    this.image = image;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = Integer.MIN_VALUE)
  public Double getStartValue() {
    return startValue;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = Integer.MIN_VALUE)
  public Double getEndValue() {
    return endValue;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getColor() {
    return color;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getImage() {
    return image;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public LegendSet getLegendSet() {
    return legendSet;
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
  public void setId(long id) {
    this.id = id;
  }

  @JsonProperty(value = "id")
  @JacksonXmlProperty(localName = "id", isAttribute = true)
  @Override
  public String getUid() {
    return uid;
  }

  @Override
  public void setUid(String uid) {
    this.uid = uid;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Override
  public Date getCreated() {
    return created;
  }

  @Override
  public void setCreated(Date created) {
    this.created = created;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Override
  public Date getLastUpdated() {
    return lastUpdated;
  }

  @Override
  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  @JsonProperty
  @Override
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @Override
  public void setLastUpdatedBy(User lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getHref() {
    return href;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(localName = "access", namespace = DxfNamespaces.DXF_2_0)
  public org.hisp.dhis.security.acl.Access getAccess() {
    return access;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getCode() {
    return code;
  }

  @Override
  public void setCode(String code) {
    this.code = code;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getName() {
    return name;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDisplayName() {
    return translations.getTranslation("name", name);
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setAutoFields() {
    IdentifiableObject.super.setAutoFields();
  }

  @Override
  public User getCreatedBy() {
    return null; // Not supported - maplegend table doesn't have userid column
  }

  @Override
  public void setCreatedBy(User createdBy) {
    // Not supported - maplegend table doesn't have userid column
  }

  @Override
  public Set<Translation> getTranslations() {
    return translations != null ? translations.getTranslations() : Set.of();
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    if (this.translations == null) {
      this.translations = new TranslationProperty();
    }
    this.translations.setTranslations(translations);
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return getUid();
    }
    if (idScheme.is(IdentifiableProperty.CODE)) {
      return getCode();
    }
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getName();
    }
    return null;
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return getDisplayName();
    }
    if (idScheme.is(IdentifiableProperty.CODE)) {
      return getCode();
    }
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    }
    return null;
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
        && Objects.equals(getCode(), other.getCode())
        && Objects.equals(getName(), other.getName());
  }

  // -------------------------------------------------------------------------
  // Unsupported IdentifiableObject methods
  // -------------------------------------------------------------------------
  @Deprecated
  @Override
  public Sharing getSharing() {
    return null;
  }

  @Deprecated
  @Override
  public void setSharing(Sharing sharing) {
    // Not supported
  }

  /** This entity does not support attribute values. */
  @Override
  @Deprecated
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

  /**
   * @deprecated This method is replaced by {@link #getCreatedBy()}
   */
  @Override
  @Deprecated
  public User getUser() {
    return getCreatedBy();
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
  public void setOwner(String owner) {
    // Not supported
  }

  @Override
  public boolean hasSharing() {
    return false;
  }
}
