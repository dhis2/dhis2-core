/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.route;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.common.auth.AuthScheme;
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
 * @author Morten Olav Hansen
 */
@Entity
@Table(name = "route")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "identifiableObject", namespace = DxfNamespaces.DXF_2_0)
@Accessors(chain = true)
public class Route extends BaseMetadataObject implements IdentifiableObject, MetadataObject {
  public static final int DEFAULT_RESPONSE_TIMEOUT_SECONDS = 5;
  public static final String PATH_WILDCARD_SUFFIX = "/**";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "routeid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  @AuditAttribute
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @Type(type = "text")
  @Column(name = "description")
  @Getter
  @Setter
  @JsonProperty
  private String description;

  @Column(name = "disabled")
  @Getter
  @Setter
  @JsonProperty(required = true)
  private boolean disabled;

  @Type(type = "text")
  @Column(name = "url")
  @Getter
  @Setter
  @JsonProperty(required = true)
  private String url;

  @Type(type = "jbMap")
  @Column(name = "headers", nullable = false)
  @Getter
  @Setter
  @JsonProperty(required = true)
  private Map<String, String> headers = new HashMap<>();

  /** Optional. Authentication to be passed as part of the route request. */
  @Type(type = "jbAuth")
  @Column(name = "auth")
  @Getter
  @Setter
  @JsonProperty
  private AuthScheme auth;

  /** Optional. Required authorities for invoking the route. */
  @Type(type = "jbList")
  @Column(name = "authorities", nullable = false)
  @Getter
  @Setter
  @JsonProperty
  private List<String> authorities = new ArrayList<>();

  @Column(name = "responsetimeoutseconds")
  @Getter
  @Setter
  @JsonProperty(defaultValue = "" + DEFAULT_RESPONSE_TIMEOUT_SECONDS)
  private int responseTimeoutSeconds = DEFAULT_RESPONSE_TIMEOUT_SECONDS;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  @Type(type = "jsbAttributeValues")
  @Column(name = "attributevalues")
  @AuditAttribute
  private AttributeValues attributeValues = AttributeValues.empty();

  /**
   * If the route url ends with /** return true. Otherwise return false.
   *
   * @return true if the route is configured to allow subpaths
   */
  public boolean allowsSubpaths() {
    return url.endsWith(PATH_WILDCARD_SUFFIX);
  }

  /**
   * If this route supports sub-paths, return the base URL without the /** path wildcard suffix but
   * including the trailing slash. For example, if the url is configured as https://google.com/**,
   * return https://google.com/. If the route does not support sub-paths, return the full configured
   * url.
   *
   * @return the base url String
   */
  public String getBaseUrl() {
    if (allowsSubpaths()) {
      return url.substring(0, url.length() - PATH_WILDCARD_SUFFIX.length()) + "/";
    }
    return url;
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    return Objects.hash(
        getUid(),
        getCode(),
        getName(),
        getDescription(),
        isDisabled(),
        getUrl(),
        getHeaders(),
        getAuth(),
        getAuthorities(),
        getResponseTimeoutSeconds());
  }

  /**
   * Compares uid, code and name like {@link BaseIdentifiableObject}, plus description, disabled,
   * url, headers, auth, authorities and responseTimeoutSeconds. Uses get-methods and the real class
   * to handle proxied objects.
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof Route other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName())
            && Objects.equals(getDescription(), other.getDescription())
            && isDisabled() == other.isDisabled()
            && Objects.equals(getUrl(), other.getUrl())
            && Objects.equals(getHeaders(), other.getHeaders())
            && Objects.equals(getAuth(), other.getAuth())
            && Objects.equals(getAuthorities(), other.getAuthorities())
            && getResponseTimeoutSeconds() == other.getResponseTimeoutSeconds();
  }

  @Override
  public String toString() {
    return ("{\"class\":\"%s\", \"id\":\"%s\", \"uid\":\"%s\", \"code\":\"%s\", \"name\":\"%s\","
            + " \"created\":\"%s\", \"lastUpdated\":\"%s\" }")
        .formatted(
            getClass(), getId(), getUid(), getCode(), getName(), getCreated(), getLastUpdated());
  }

  // -------------------------------------------------------------------------
  // Identifiable object getters and setters
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
  public void setCode(String code) {
    this.code = code;
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
  public void setName(String name) {
    this.name = name;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return translationProperty().getTranslation("NAME", getName());
  }

  @Override
  @Gist(included = Include.FALSE)
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translationProperty().getTranslations();
  }

  /** Clears out the translation cache. A null set is stored as an empty set. */
  @Override
  public void setTranslations(Set<Translation> translations) {
    translationProperty().setTranslations(translations != null ? translations : new HashSet<>());
  }

  /**
   * Returns the embedded translations, which Hibernate loads as null when the translations column
   * is null.
   */
  private TranslationProperty translationProperty() {
    if (translations == null) {
      translations = new TranslationProperty();
    }

    return translations;
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
  public void addAttributeValue(String attributeUid, String value) {
    this.attributeValues = getAttributeValues().added(attributeUid, value);
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    this.attributeValues = getAttributeValues().removed(attributeId);
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
  public void setSharing(Sharing sharing) {
    this.sharing = sharing;
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
    }
    return getPropertyValue(idScheme);
  }
}
