/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.security.oauth2.authorization;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.SecondaryMetadataObject;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
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
 * DHIS2 persistence representation of a Spring Authorization Server {@code OAuth2Authorization}.
 *
 * <p>An {@code OAuth2Authorization} is a single authorization grant and its associated tokens for
 * one principal authenticating against one registered client. It holds the authorization code,
 * access token, refresh token, OIDC id-token, device code and user code, each with its own
 * issued/expires timestamps and opaque metadata blob. Spring Authorization Server operates
 * exclusively on the framework type {@code
 * org.springframework.security.oauth2.server.authorization.OAuth2Authorization}, which is an
 * immutable, builder-constructed value object not wired for JPA or DHIS2 ACL.
 *
 * <p>This class is the mutable DHIS2-side mirror: an {@link IdentifiableObject} with a JPA mapping
 * and a JSON/XML view, so the authorization can live in the DHIS2 database alongside all other
 * identifiable objects and be inspected through the admin REST surface. Conversion between the two
 * representations happens in {@code Dhis2OAuth2AuthorizationServiceImpl}: {@code
 * toEntity(OAuth2Authorization)} builds a {@link Dhis2OAuth2Authorization} for persistence, and
 * {@code toObject(Dhis2OAuth2Authorization)} reconstructs the Spring-side {@code
 * OAuth2Authorization} for the authorization-server runtime.
 *
 * <p>Marked {@link SecondaryMetadataObject} so the type is excluded from the default {@code
 * /api/metadata} export; token-bearing fields are additionally {@link JsonIgnore}'d so no REST
 * surface can leak them even on explicit requests.
 *
 * <p>The table maps the creator to {@code createdby} and has no sharing, translations or attribute
 * values columns, so this entity implements {@link IdentifiableObject} directly instead of
 * extending {@code BaseMetadataObject}. Sharing and translations are kept in memory only: the store
 * sets owner and public access on save, and the metadata importer rejects translations for this
 * non-translatable type. Attribute values are no-op stubs at the end of the class.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Getter
@Setter
@Entity
@Table(name = "oauth2_authorization")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "oauth2Authorization", namespace = DxfNamespaces.DXF_2_0)
public class Dhis2OAuth2Authorization implements IdentifiableObject, SecondaryMetadataObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "id")
  private long id;

  @AuditAttribute
  @Column(name = "uid", unique = true, nullable = false, length = 11)
  private String uid;

  @AuditAttribute
  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "created", nullable = false, updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Column(name = "lastupdated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdated;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lastupdatedby")
  private User lastUpdatedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "createdby")
  private User createdBy;

  @Column(name = "name", nullable = false, length = 230)
  private String name;

  /**
   * Reference to the {@link org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client} this grant was
   * issued to. Holds the internal id of the registered client, not its public {@code clientId}.
   */
  @Column(name = "registered_client_id", nullable = false, length = 255)
  @JsonProperty
  private String registeredClientId;

  /**
   * Name of the resource owner the grant is tied to. For user-delegated flows this is the DHIS2
   * username; for {@code client_credentials} it is the client itself.
   */
  @Column(name = "principal_name", nullable = false, length = 255)
  @JsonProperty
  private String principalName;

  /**
   * The grant type that produced this authorization (e.g. {@code authorization_code}, {@code
   * client_credentials}, {@code refresh_token}, {@code
   * urn:ietf:params:oauth:grant-type:device_code}).
   */
  @Column(name = "authorization_grant_type", nullable = false, length = 255)
  @JsonProperty
  private String authorizationGrantType;

  /** Comma-separated list of scopes that were actually granted for this authorization. */
  @Column(name = "authorized_scopes", length = 1000)
  @JsonProperty
  private String authorizedScopes;

  /** JSON-encoded Spring AS attributes map (authenticated principal, request metadata, etc.). */
  @Type(type = "text")
  @Column(name = "attributes", columnDefinition = "text")
  @JsonIgnore
  private String attributes;

  /** Opaque {@code state} value used by Spring AS for OAuth2 CSRF protection during the flow. */
  @Column(name = "state", length = 500)
  @JsonIgnore
  private String state;

  @Column(name = "authorization_code_value", length = 4000)
  @JsonIgnore
  private String authorizationCodeValue;

  @Column(name = "authorization_code_issued_at")
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty
  private Date authorizationCodeIssuedAt;

  @Column(name = "authorization_code_expires_at")
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty
  private Date authorizationCodeExpiresAt;

  @Type(type = "text")
  @Column(name = "authorization_code_metadata", columnDefinition = "text")
  @JsonIgnore
  private String authorizationCodeMetadata;

  @Column(name = "access_token_value", length = 4000)
  @JsonIgnore
  private String accessTokenValue;

  @Column(name = "access_token_issued_at")
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty
  private Date accessTokenIssuedAt;

  @Column(name = "access_token_expires_at")
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty
  private Date accessTokenExpiresAt;

  @Type(type = "text")
  @Column(name = "access_token_metadata", columnDefinition = "text")
  @JsonIgnore
  private String accessTokenMetadata;

  @Column(name = "access_token_type", length = 255)
  @JsonProperty
  private String accessTokenType;

  @Column(name = "access_token_scopes", length = 1000)
  @JsonProperty
  private String accessTokenScopes;

  @Column(name = "refresh_token_value", length = 4000)
  @JsonIgnore
  private String refreshTokenValue;

  @Column(name = "refresh_token_issued_at")
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty
  private Date refreshTokenIssuedAt;

  @Column(name = "refresh_token_expires_at")
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty
  private Date refreshTokenExpiresAt;

  @Type(type = "text")
  @Column(name = "refresh_token_metadata", columnDefinition = "text")
  @JsonIgnore
  private String refreshTokenMetadata;

  @Column(name = "oidc_id_token_value", length = 4000)
  @JsonIgnore
  private String oidcIdTokenValue;

  @Column(name = "oidc_id_token_issued_at")
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty
  private Date oidcIdTokenIssuedAt;

  @Column(name = "oidc_id_token_expires_at")
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty
  private Date oidcIdTokenExpiresAt;

  @Type(type = "text")
  @Column(name = "oidc_id_token_metadata", columnDefinition = "text")
  @JsonIgnore
  private String oidcIdTokenMetadata;

  @Type(type = "text")
  @Column(name = "oidc_id_token_claims", columnDefinition = "text")
  @JsonIgnore
  private String oidcIdTokenClaims;

  @Column(name = "user_code_value", length = 4000)
  @JsonIgnore
  private String userCodeValue;

  @Column(name = "user_code_issued_at")
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty
  private Date userCodeIssuedAt;

  @Column(name = "user_code_expires_at")
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty
  private Date userCodeExpiresAt;

  @Type(type = "text")
  @Column(name = "user_code_metadata", columnDefinition = "text")
  @JsonIgnore
  private String userCodeMetadata;

  @Column(name = "device_code_value", length = 4000)
  @JsonIgnore
  private String deviceCodeValue;

  @Column(name = "device_code_issued_at")
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty
  private Date deviceCodeIssuedAt;

  @Column(name = "device_code_expires_at")
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty
  private Date deviceCodeExpiresAt;

  @Type(type = "text")
  @Column(name = "device_code_metadata", columnDefinition = "text")
  @JsonIgnore
  private String deviceCodeMetadata;

  @Transient private transient Sharing sharing = new Sharing();

  @Transient private Set<Translation> translations = new HashSet<>();

  @Transient private transient String href;

  @Transient private transient Access access;

  /** Required by Hibernate + Jackson for reflective instantiation. */
  public Dhis2OAuth2Authorization() {}

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
  public void setUid(String uid) {
    this.uid = uid;
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

  /** The display name is the name, as an authorization has no persisted translations. */
  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return name;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was created.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getCreated() {
    return created;
  }

  @Override
  public void setCreated(Date created) {
    this.created = created;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was last updated.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getLastUpdated() {
    return lastUpdated;
  }

  @Override
  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  @Override
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @Override
  public void setLastUpdatedBy(User lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  @Override
  @Gist(included = Include.FALSE)
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getCreatedBy() {
    return createdBy;
  }

  @Override
  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }

  @Override
  @OpenApi.Ignore
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getUser() {
    return createdBy;
  }

  @Override
  public void setUser(User user) {
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
  }

  @Override
  public void setOwner(String owner) {
    getSharing().setOwner(owner);
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
  @Gist(included = Include.FALSE)
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    if (translations == null) {
      translations = new HashSet<>();
    }

    return translations;
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translations = translations;
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
  public void setHref(String href) {
    this.href = href;
  }

  @Override
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(localName = "access", namespace = DxfNamespaces.DXF_2_0)
  public Access getAccess() {
    return access;
  }

  @Override
  public void setAccess(Access access) {
    this.access = access;
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
    }
    return getPropertyValue(idScheme);
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);

    return result;
  }

  /**
   * Same real class (Hibernate proxies resolved) and the same {@code uid}, {@code code} and {@code
   * name}, read through the getters.
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof Dhis2OAuth2Authorization other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName());
  }

  @Override
  public String toString() {
    return "{"
        + "\"class\":\""
        + getClass()
        + "\", "
        + "\"id\":\""
        + getId()
        + "\", "
        + "\"uid\":\""
        + getUid()
        + "\", "
        + "\"code\":\""
        + getCode()
        + "\", "
        + "\"name\":\""
        + getName()
        + "\", "
        + "\"created\":\""
        + getCreated()
        + "\", "
        + "\"lastUpdated\":\""
        + getLastUpdated()
        + "\" "
        + "}";
  }

  // -------------------------------------------------------------------------
  // Unsupported IdentifiableObject methods
  // -------------------------------------------------------------------------

  /** Dhis2OAuth2Authorization does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** Dhis2OAuth2Authorization does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** Dhis2OAuth2Authorization does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** Dhis2OAuth2Authorization does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }
}
