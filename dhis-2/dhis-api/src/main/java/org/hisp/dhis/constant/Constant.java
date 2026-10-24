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
package org.hisp.dhis.constant;

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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Dang Duy Hieu
 */
@Entity
@Table(name = "constant")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "constant", namespace = DxfNamespaces.DXF_2_0)
public class Constant extends BaseMetadataObject implements NameableObject {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "constantid")
  @Setter
  private long id;

  @Column(name = "code", unique = true, length = 50)
  @Setter
  @AuditAttribute
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  @Setter
  private String name;

  @Column(name = "shortname", nullable = false, unique = true, length = 50)
  @Setter
  private String shortName;

  @Type(type = "text")
  @Column(name = "description", columnDefinition = "text")
  @Setter
  private String description;

  @Column(name = "value", nullable = false)
  @Setter
  private double value;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  @Setter
  private Sharing sharing = new Sharing();

  @Type(type = "jsbAttributeValues")
  @Column(name = "attributevalues")
  @AuditAttribute
  private AttributeValues attributeValues = AttributeValues.empty();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Constant() {}

  public Constant(String name) {
    this.name = name;
    this.shortName = name;
  }

  public Constant(String name, double value) {
    this.name = name;
    this.shortName = name;
    this.value = value;
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
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

  /** Class check uses get-methods and the real class to handle proxied objects. */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof Constant other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName())
            && Objects.equals(getShortName(), other.getShortName())
            && Objects.equals(getDescription(), other.getDescription());
  }

  @Override
  public String toString() {
    return ("{\"class\":\"%s\", \"hashCode\":\"%s\", \"id\":\"%s\", \"uid\":\"%s\","
            + " \"code\":\"%s\", \"name\":\"%s\", \"shortName\":\"%s\","
            + " \"description\":\"%s\", \"created\":\"%s\", \"lastUpdated\":\"%s\" }")
        .formatted(
            getClass(),
            hashCode(),
            getId(),
            getUid(),
            getCode(),
            getName(),
            getShortName(),
            getDescription(),
            getCreated(),
            getLastUpdated());
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @Override
  @JsonIgnore
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
    return uid;
  }

  @Override
  @Sortable(value = false)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(isAttribute = true)
  @Property(PropertyType.URL)
  public String getHref() {
    return href;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The unique code for this Object.")
  @Property(PropertyType.IDENTIFIER)
  public String getCode() {
    return code;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The name of this Object. Required and unique.")
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
    return translations.getTranslation("NAME", getName());
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
    return translations.getTranslation("SHORT_NAME", getShortName());
  }

  @Override
  @Sortable
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 1)
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

  /**
   * Returns the display property indicated by the given display property. Falls back to display
   * name if display short name is null.
   */
  @Override
  @JsonIgnore
  public String getDisplayProperty(DisplayProperty displayProperty) {
    if (DisplayProperty.SHORTNAME == displayProperty && getDisplayShortName() != null) {
      return getDisplayShortName();
    } else {
      return getDisplayName();
    }
  }

  /** The form name falls back to the display name; it can be translated with the FORM_NAME key. */
  @JsonProperty
  @Sortable(whenPersisted = false)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "formName", key = "FORM_NAME")
  public String getDisplayFormName() {
    return translations.getTranslation("FORM_NAME", getDisplayName());
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public double getValue() {
    return value;
  }

  @Override
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    if (attributeValues == null) {
      attributeValues = AttributeValues.empty();
    }

    return attributeValues;
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    this.attributeValues = attributeValues == null ? AttributeValues.empty() : attributeValues;
  }

  @Override
  public void addAttributeValue(String attributeId, String value) {
    this.attributeValues = getAttributeValues().added(attributeId, value);
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    this.attributeValues = getAttributeValues().removed(attributeId);
  }

  @Override
  @Gist(included = Include.FALSE)
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translations.getTranslations();
  }

  /** A null value is stored as an empty set; the {@code translations} column is not nullable. */
  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translations.setTranslations(translations == null ? new HashSet<>() : translations);
  }

  @Override
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Sharing getSharing() {
    if (sharing == null) {
      sharing = new Sharing();
    }

    return sharing;
  }

  @Override
  public void setUser(User user) {
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
  }

  @Override
  public void setOwner(String userId) {
    getSharing().setOwner(userId);
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
  // Unsupported NameableObject methods
  // -------------------------------------------------------------------------

  /** Constant does not support form name. */
  @Deprecated(since = "2.44")
  @Sortable
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFormName() {
    return null;
  }

  /** Constant does not support form name. */
  @Deprecated(since = "2.44")
  public void setFormName(String formName) {
    // Not supported
  }
}
