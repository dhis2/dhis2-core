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
package org.hisp.dhis.datastore;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Access;
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
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * An entry of the shared datastore, identified by namespace and key.
 *
 * <p>The value is written either to the jsonb column or, when {@link #getEncrypted()} is set, to
 * the AES-128 encrypted column. Both columns are mapped through their getters, which return a
 * pending {@link #setValue(String) value} over the loaded column value, so that a new value is
 * persisted in the right column on flush.
 *
 * <p>A datastore entry is not metadata, so this entity implements {@link IdentifiableObject}
 * directly instead of extending {@code BaseMetadataObject}: a {@code MetadataObject} would get a
 * {@code deletedobject} row on every delete and its schema would become metadata. The table has no
 * name, translations or attribute values columns; those properties and {@code href} are unsupported
 * stubs at the end of the class.
 *
 * <p>The XML root element name {@code identifiableObject} is the one the class used to inherit; it
 * is also the name of the {@code dataStore} schema.
 *
 * @author Stian Sandvold
 */
@Entity
@Table(name = "keyjsonvalue")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "identifiableObject", namespace = DxfNamespaces.DXF_2_0)
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class DatastoreEntry implements IdentifiableObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "keyjsonvalueid")
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
  @JoinColumn(name = "userid")
  private User createdBy;

  /** A namespace represents a collection of keys */
  @JsonProperty
  @ToString.Include
  @Column(name = "namespace", nullable = false)
  private String namespace;

  /** A key belongs to a namespace, and represent a value */
  @JsonProperty
  @ToString.Include
  @Column(name = "namespacekey", nullable = false)
  private String key;

  /**
   * A value referenced by a key and namespace, JSON-formatted data stored as a string but in a
   * jsonb column. Mapped through {@link #getJbPlainValue()}.
   */
  @Transient private String jbPlainValue;

  /** Whether this KeyJsonValue is encrypted or not. Default is false. */
  @Column(name = "encrypted")
  private Boolean encrypted = false;

  /** Encrypted value if encrypted is set to true. Mapped through {@link #getEncryptedValue()}. */
  @Transient private String encryptedValue;

  /** Object sharing (JSONB). */
  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  /**
   * Transient variable to hold any new values set during session. Will be made into the correct
   * type when being persisted by the persistence layer (encrypted or plain).
   */
  @Transient private transient String value;

  /** Access information for this object. Applies to current user. */
  @Transient private transient org.hisp.dhis.security.acl.Access access;

  public DatastoreEntry(String namespace, String key) {
    this(namespace, key, null, false);
  }

  public DatastoreEntry(String namespace, String key, String value) {
    this(namespace, key, value, false);
  }

  public DatastoreEntry(String namespace, String key, String value, boolean encrypted) {
    this.namespace = namespace;
    this.key = key;
    this.value = value;
    this.encrypted = encrypted;
  }

  @JsonProperty
  @ToString.Include
  public String getValue() {
    return isEncryptedInternal() ? getEncryptedValue() : getJbPlainValue();
  }

  @Access(AccessType.PROPERTY)
  @Type(type = "jbPlainString")
  @Column(name = "jbvalue")
  public String getJbPlainValue() {
    return !isEncryptedInternal() && value != null ? value : jbPlainValue;
  }

  @Access(AccessType.PROPERTY)
  @Type(type = "Aes128EncryptedString")
  @Column(name = "encrypted_value")
  public String getEncryptedValue() {
    return isEncryptedInternal() && value != null ? value : encryptedValue;
  }

  /** Note: this must use a name that is not also a hibernate mapped property */
  private boolean isEncryptedInternal() {
    return encrypted != null && encrypted;
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
  @JsonProperty(value = "id")
  @JacksonXmlProperty(localName = "id", isAttribute = true)
  @Description("The Unique Identifier for this Object.")
  @Property(value = PropertyType.IDENTIFIER, required = Value.FALSE)
  @PropertyRange(min = 11, max = 11)
  public String getUid() {
    return uid;
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
  @Description("The date this object was created.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getCreated() {
    return created;
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
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(localName = "access", namespace = DxfNamespaces.DXF_2_0)
  public org.hisp.dhis.security.acl.Access getAccess() {
    return access;
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      return code;
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    }
    return null;
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    return getPropertyValue(idScheme);
  }

  // -------------------------------------------------------------------------
  // hashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    return Objects.hash(getUid(), getCode(), getNamespace(), getKey(), getValue());
  }

  /** Class check uses the real class and get-methods to handle proxied objects. */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof DatastoreEntry other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getNamespace(), other.getNamespace())
            && Objects.equals(getKey(), other.getKey())
            && Objects.equals(getValue(), other.getValue());
  }

  // -------------------------------------------------------------------------
  // Unsupported IdentifiableObject methods
  // -------------------------------------------------------------------------

  /** DatastoreEntry does not support name. */
  @Override
  @Deprecated(since = "2.44")
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The name of this Object. Required and unique.")
  @PropertyRange(min = 1)
  public String getName() {
    return null;
  }

  /** DatastoreEntry does not support name. */
  @Override
  @Deprecated(since = "2.44")
  public void setName(String name) {
    // Not supported
  }

  /** DatastoreEntry does not support name. */
  @Override
  @Deprecated(since = "2.44")
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return null;
  }

  /** DatastoreEntry does not support translations. */
  @Override
  @Deprecated(since = "2.44")
  @Gist(included = Include.FALSE)
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return new HashSet<>();
  }

  /** DatastoreEntry does not support translations. */
  @Override
  @Deprecated(since = "2.44")
  public void setTranslations(Set<Translation> translations) {
    // Not supported
  }

  /** DatastoreEntry does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** DatastoreEntry does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** DatastoreEntry does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** DatastoreEntry does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void removeAttributeValue(String attributeUid) {
    // Not supported
  }

  /** DatastoreEntry is not given a link by the API. */
  @Override
  @Deprecated(since = "2.44")
  @Sortable(value = false)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(isAttribute = true)
  @Property(PropertyType.URL)
  public String getHref() {
    return null;
  }

  /** DatastoreEntry is not given a link by the API. */
  @Override
  @Deprecated(since = "2.44")
  public void setHref(String href) {
    // Not supported
  }
}
