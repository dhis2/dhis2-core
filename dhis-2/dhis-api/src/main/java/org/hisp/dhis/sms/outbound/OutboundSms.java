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
package org.hisp.dhis.sms.outbound;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
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
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * An SMS sent, or to be sent, through an SMS gateway.
 *
 * <p>The outbound_sms table has no code, name, created, lastupdated, user, sharing, translations or
 * attributevalues column, so this entity implements {@link IdentifiableObject} directly. Created
 * and last updated are kept in memory only (set on construction, as before); the other unsupported
 * properties are no-op stubs at the end of the class.
 */
@Entity
@Proxy(lazy = false)
@Table(
    name = "outbound_sms",
    indexes = @Index(name = "outbound_sms_status_index", columnList = "status"))
@JacksonXmlRootElement(localName = "outboundsms")
public class OutboundSms implements IdentifiableObject {
  public static final String DHIS_SYSTEM_SENDER = "DHIS-System";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "id")
  private long id;

  @Column(name = "uid", unique = true, nullable = false, length = 11)
  private String uid;

  @Column(name = "sender")
  private String sender;

  @ElementCollection
  @CollectionTable(
      name = "outbound_sms_recipients",
      joinColumns = @JoinColumn(name = "outbound_sms_id"))
  @Column(name = "elt", columnDefinition = "text")
  @Type(type = "text")
  @Fetch(FetchMode.SELECT)
  private Set<String> recipients;

  @Column(name = "date", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date date;

  /** Passed to the gateway when sending, not persisted. */
  @Transient private String subject;

  @Column(name = "message", length = 500)
  private String message;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.ORDINAL)
  private OutboundSmsStatus status = OutboundSmsStatus.OUTBOUND;

  /** Not persisted, there is no created column. */
  @Transient private Date created;

  /** Not persisted, there is no lastupdated column. */
  @Transient private Date lastUpdated;

  /** Link to this object in the API, set when it is rendered by the web layer. */
  @Transient private transient String href;

  /** Access information for this object. Applies to current user. */
  @Transient private transient Access access;

  /**
   * Not persisted, there is no translations column. Kept in memory so that a translations check
   * still sees translations set on this non-translatable type instead of dropping them.
   */
  @Transient private Set<Translation> translations = new HashSet<>();

  public OutboundSms() {
    setAutoFields();
  }

  public OutboundSms(String subject, String message, Set<String> recipients) {
    this.subject = subject;
    this.message = message;
    this.recipients = recipients;
  }

  @JsonProperty(value = "recipients")
  @JacksonXmlProperty(localName = "recipients")
  public Set<String> getRecipients() {
    return recipients;
  }

  public void setRecipients(Set<String> recipients) {
    this.recipients = recipients;
  }

  @JsonProperty(value = "date")
  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  @JsonProperty(value = "message")
  @JacksonXmlProperty(localName = "message")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @JsonProperty(value = "sender")
  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  @JsonProperty(value = "status")
  public OutboundSmsStatus getStatus() {
    return status;
  }

  public void setStatus(OutboundSmsStatus status) {
    this.status = status;
  }

  @JsonProperty(value = "subject")
  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
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

  /** Class check uses the real class and get-methods to handle proxied objects. */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof OutboundSms other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName());
  }

  @Override
  public String toString() {
    return "OutboundSMS [recipients=" + getNumbers() + ", message=" + message + "]";
  }

  private String getNumbers() {
    if (this.recipients == null) {
      return null;
    }

    String numbers = "";

    for (String recipient : this.recipients) {
      numbers += recipient + ", ";
    }

    return numbers.substring(0, numbers.length() - 2);
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
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
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
  // Unsupported IdentifiableObject methods
  // -------------------------------------------------------------------------

  /** OutboundSms does not support code. */
  @Override
  @Deprecated(since = "2.44")
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The unique code for this Object.")
  @Property(PropertyType.IDENTIFIER)
  public String getCode() {
    return null;
  }

  /** OutboundSms does not support code. */
  @Override
  @Deprecated(since = "2.44")
  public void setCode(String code) {
    // Not supported
  }

  /** OutboundSms does not support name. */
  @Override
  @Deprecated(since = "2.44")
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The name of this Object. Required and unique.")
  @PropertyRange(min = 1)
  public String getName() {
    return null;
  }

  /** OutboundSms does not support name. */
  @Override
  @Deprecated(since = "2.44")
  public void setName(String name) {
    // Not supported
  }

  /** OutboundSms does not support name, so it has no display name. */
  @Override
  @Deprecated(since = "2.44")
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return null;
  }

  /** OutboundSms does not support created by. */
  @Override
  @Deprecated(since = "2.44")
  @Gist(included = Include.FALSE)
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getCreatedBy() {
    return null;
  }

  /** OutboundSms does not support created by. */
  @Override
  @Deprecated(since = "2.44")
  public void setCreatedBy(User createdBy) {
    // Not supported
  }

  /** OutboundSms does not support user. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Ignore
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getUser() {
    return null;
  }

  /** OutboundSms does not support user. */
  @Override
  @Deprecated(since = "2.44")
  public void setUser(User user) {
    // Not supported
  }

  /** OutboundSms does not support last updated by. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getLastUpdatedBy() {
    return null;
  }

  /** OutboundSms does not support last updated by. */
  @Override
  @Deprecated(since = "2.44")
  public void setLastUpdatedBy(User lastUpdatedBy) {
    // Not supported
  }

  /** OutboundSms does not support sharing. */
  @Override
  @Deprecated(since = "2.44")
  public void setOwner(String owner) {
    // Not supported
  }

  /**
   * OutboundSms does not support sharing: there is no sharing column, so writes through the
   * returned object are discarded.
   */
  @Override
  @Deprecated(since = "2.44")
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Sharing getSharing() {
    return Sharing.empty();
  }

  /** OutboundSms does not support sharing. */
  @Override
  @Deprecated(since = "2.44")
  public void setSharing(Sharing sharing) {
    // Not supported
  }

  /** OutboundSms does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** OutboundSms does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** OutboundSms does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** OutboundSms does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }
}
