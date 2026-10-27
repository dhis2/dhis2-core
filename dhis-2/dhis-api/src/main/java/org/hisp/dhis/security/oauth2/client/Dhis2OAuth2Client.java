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
package org.hisp.dhis.security.oauth2.client;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.AccessType;
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
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.OpenApi;
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
 * Persisted OAuth2 registered-client entity. Mirrors Spring Authorization Server's {@link
 * org.springframework.security.oauth2.server.authorization.client.RegisteredClient}, mapped to the
 * {@code oauth2_client} table, and is the DB row that authorizes an app to request tokens from
 * DHIS2 acting as an Authorization Server.
 *
 * <p>Exposed via the admin CRUD endpoints under {@code /api/oAuth2Clients}, gated by the {@code
 * F_OAUTH2_CLIENT_MANAGE} authority. Clients can also be created dynamically via the Dynamic Client
 * Registration (RFC 7591) endpoint {@code /connect/register}.
 *
 * <p>Several fields are stored as comma-separated strings (for example {@link
 * #authorizationGrantTypes}, {@link #clientAuthenticationMethods}, {@link #redirectUris}, {@link
 * #postLogoutRedirectUris}, {@link #scopes}) and are parsed into Spring AS's typed values (e.g.
 * {@link org.springframework.security.oauth2.core.AuthorizationGrantType}, {@link
 * org.springframework.security.oauth2.core.ClientAuthenticationMethod}) by {@code
 * Dhis2OAuth2ClientServiceImpl.toObject} when building a {@link
 * org.springframework.security.oauth2.server.authorization.client.RegisteredClient}.
 *
 * <p>The table maps the creator to {@code createdby} and has no sharing, translations or attribute
 * values columns, so this entity implements {@link IdentifiableObject} directly instead of
 * extending {@code BaseMetadataObject}. Sharing and translations are kept in memory only: the store
 * sets owner and public access on save, and the metadata importer rejects translations for this
 * non-translatable type. Attribute values are no-op stubs at the end of the class.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Entity
@Table(name = "oauth2_client")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "oauth2Client", namespace = DxfNamespaces.DXF_2_0)
public class Dhis2OAuth2Client implements IdentifiableObject, MetadataObject {

  /** Length of the {@code name} column, which the client id fallback is truncated to. */
  private static final int NAME_LENGTH = 230;

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

  /** Mapped through {@link #getName()}, which falls back to the client id. */
  @Transient private String name;

  /**
   * Public OAuth2 {@code client_id} presented by the client at the token and authorize endpoints.
   */
  @Getter
  @Setter
  @JsonProperty
  @Column(name = "client_id", nullable = false, unique = true, length = 255)
  private String clientId;

  /**
   * Client secret used for the {@code client_secret_basic} / {@code client_secret_post}
   * authentication methods. Stored hashed; null for public clients and for clients that
   * authenticate via {@code private_key_jwt}.
   */
  @Getter
  @Setter
  @JsonProperty
  @Column(name = "client_secret", length = 255)
  private String clientSecret;

  /** Timestamp at which {@link #clientId} was issued. */
  @Getter
  @Setter
  @JsonProperty
  @Column(name = "client_id_issued_at")
  @Temporal(TemporalType.TIMESTAMP)
  private Date clientIdIssuedAt;

  /** Optional expiry for {@link #clientSecret}; null means the secret does not expire. */
  @Getter
  @Setter
  @JsonProperty
  @Column(name = "client_secret_expires_at")
  @Temporal(TemporalType.TIMESTAMP)
  private Date clientSecretExpiresAt;

  /**
   * Comma-separated list of OAuth2 client authentication methods the client may use at the token
   * endpoint (e.g. {@code client_secret_basic}, {@code client_secret_post}, {@code
   * private_key_jwt}, {@code none}). Parsed into Spring AS {@link
   * org.springframework.security.oauth2.core.ClientAuthenticationMethod} values at load time.
   */
  @Getter
  @Setter
  @JsonProperty
  @Column(name = "client_authentication_methods", length = 1000)
  private String clientAuthenticationMethods;

  /**
   * Comma-separated list of OAuth2 authorization grant types the client is permitted to use (e.g.
   * {@code authorization_code}, {@code client_credentials}, {@code refresh_token}, {@code
   * urn:ietf:params:oauth:grant-type:device_code}). Parsed into Spring AS {@link
   * org.springframework.security.oauth2.core.AuthorizationGrantType} values via {@link
   * org.hisp.dhis.security.oauth2.OAuth2GrantTypes#resolve(String)}.
   */
  @Getter
  @Setter
  @JsonProperty
  @Column(name = "authorization_grant_types", length = 1000)
  private String authorizationGrantTypes;

  /**
   * Comma-separated list of registered redirect URIs used by the authorization-code and device-code
   * flows; an incoming {@code redirect_uri} must match one of these exactly.
   */
  @Getter
  @Setter
  @JsonProperty
  @Column(name = "redirect_uris", length = 1000)
  private String redirectUris;

  /** Comma-separated list of post-logout redirect URIs allowed after OIDC RP-initiated logout. */
  @Getter
  @Setter
  @JsonProperty
  @Column(name = "post_logout_redirect_uris", length = 1000)
  private String postLogoutRedirectUris;

  /**
   * Comma-separated list of OAuth2 / OpenID Connect scopes (e.g. {@code openid}, {@code profile},
   * {@code email}) the client is permitted to request.
   */
  @Getter
  @Setter
  @JsonProperty
  @Column(name = "scopes", length = 1000)
  private String scopes;

  /**
   * JSON-encoded Spring AS {@code ClientSettings}; controls client-level options such as whether
   * user consent is required and PKCE requirements.
   */
  @Getter
  @Setter
  @JsonProperty
  @Column(name = "client_settings")
  @Type(type = "text")
  private String clientSettings;

  /**
   * JSON-encoded Spring AS {@code TokenSettings}; controls token lifetimes, access-token format
   * (opaque vs JWT), refresh-token behavior and ID-token signature algorithm.
   */
  @Getter
  @Setter
  @JsonProperty
  @Column(name = "token_settings")
  @Type(type = "text")
  private String tokenSettings;

  /** Sharing of this client. Not persisted, set by the store on save. */
  @Transient private transient Sharing sharing = new Sharing();

  /**
   * Translations of this client. Not persisted; kept so the metadata importer can reject them for
   * this non-translatable type.
   */
  @Transient private Set<Translation> translations = new HashSet<>();

  /** Link to this object in the API, set when it is rendered by the web layer. */
  @Transient private transient String href;

  /** Access information for this object. Applies to current user. */
  @Transient private transient Access access;

  public Dhis2OAuth2Client() {}

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

  /**
   * Mapped with property access so that the persisted {@code name} column is always populated even
   * if the caller (the settings UI, which has no name field) doesn't supply one: the value returned
   * here is what gets written to the DB and what the schema validator at {@code POST
   * /api/schemas/oAuth2Client} reads via reflection, letting us keep the column not-null without
   * breaking UI pre-validation. Truncated to the column length (230) so the schema-validator's
   * {@code @PropertyRange} check on a long {@code clientId} (max 255) doesn't reject the request.
   */
  @Override
  @jakarta.persistence.Access(AccessType.PROPERTY)
  @Column(name = "name", nullable = false, length = NAME_LENGTH)
  @JsonProperty
  public String getName() {
    if (name != null && !name.isEmpty()) {
      return name;
    }
    if (clientId == null) {
      return null;
    }
    return clientId.length() > NAME_LENGTH ? clientId.substring(0, NAME_LENGTH) : clientId;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the raw {@code name} field value without the {@link #getName()} fallback. Use this when
   * defaulting / preservation logic needs to distinguish "caller didn't supply a name" from "caller
   * supplied the same value as the fallback".
   */
  public String getRawName() {
    return name;
  }

  /** The display name is the name, as a client has no persisted translations. */
  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return getName();
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
        || obj instanceof Dhis2OAuth2Client other
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

  /** Dhis2OAuth2Client does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** Dhis2OAuth2Client does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** Dhis2OAuth2Client does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** Dhis2OAuth2Client does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }
}
