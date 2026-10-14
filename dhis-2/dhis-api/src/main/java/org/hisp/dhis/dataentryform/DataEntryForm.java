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
package org.hisp.dhis.dataentryform;

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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DisplayDensity;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
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
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * A custom HTML form used for data entry in data sets, programs and program stages.
 *
 * <p>The dataentryform table has no userid, sharing or attributevalues column, so this entity
 * implements {@link IdentifiableObject} directly instead of extending {@code BaseMetadataObject}
 * (which maps {@code createdBy} to userid). The unsupported properties are no-op stubs at the end
 * of the class.
 *
 * @author Bharath Kumar
 */
@Entity
@Table(name = "dataentryform")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "dataEntryForm", namespace = DxfNamespaces.DXF_2_0)
public class DataEntryForm implements IdentifiableObject, MetadataObject {
  public static final int CURRENT_FORMAT = 2;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "dataentryformid")
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

  /** Name of DataEntryForm. Required and unique. */
  @Column(name = "name", nullable = false, unique = true, length = 160)
  private String name;

  /** The display style to use to render the form. */
  @Enumerated(EnumType.STRING)
  @Column(name = "style", length = 40)
  private DisplayDensity style;

  /** HTML Code of DataEntryForm */
  @Type(type = "text")
  @Column(name = "htmlcode", columnDefinition = "text")
  private String htmlCode;

  /** The format of the DataEntryForm. */
  @Column(name = "format")
  private int format;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  /** Link to this object in the API, set when it is rendered by the web layer. */
  @Transient private transient String href;

  /** Access information for this object. Applies to current user. */
  @Transient private transient Access access;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DataEntryForm() {}

  public DataEntryForm(String name) {
    this();
    this.name = name;
  }

  public DataEntryForm(String name, String htmlCode) {
    this(name);
    this.htmlCode = htmlCode;
  }

  public DataEntryForm(String name, DisplayDensity style, String htmlCode) {
    this(name, htmlCode);
    this.style = style;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /** Indicates whether this data entry form has custom form HTML code. */
  public boolean hasForm() {
    return htmlCode != null && !htmlCode.trim().isEmpty();
  }

  // -------------------------------------------------------------------------
  // hashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);

    return 31 * result + Objects.hash(getName(), getStyle(), getHtmlCode(), getFormat());
  }

  /** Class check uses the real class and get-methods to handle proxied objects. */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof DataEntryForm other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName())
            && Objects.equals(getStyle(), other.getStyle())
            && Objects.equals(getHtmlCode(), other.getHtmlCode())
            && getFormat() == other.getFormat();
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DisplayDensity getStyle() {
    return style;
  }

  public void setStyle(DisplayDensity style) {
    this.style = style;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getHtmlCode() {
    return htmlCode;
  }

  public void setHtmlCode(String htmlCode) {
    this.htmlCode = htmlCode;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getFormat() {
    return format;
  }

  public void setFormat(int format) {
    this.format = format;
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
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
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
    return translations.getTranslation("NAME", name);
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
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translations.getTranslations();
  }

  /** Clears out the translation cache. A null set is stored as an empty set. */
  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translations.setTranslations(translations != null ? translations : new HashSet<>());
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
  // Unsupported IdentifiableObject methods
  // -------------------------------------------------------------------------

  /** DataEntryForm does not support created by, the dataentryform table has no userid column. */
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

  /** DataEntryForm does not support created by, the dataentryform table has no userid column. */
  @Override
  @Deprecated(since = "2.44")
  public void setCreatedBy(User createdBy) {
    // Not supported
  }

  /** DataEntryForm does not support created by, the dataentryform table has no userid column. */
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

  /** DataEntryForm does not support created by, the dataentryform table has no userid column. */
  @Override
  @Deprecated(since = "2.44")
  public void setUser(User user) {
    // Not supported
  }

  /** DataEntryForm does not support sharing. */
  @Override
  @Deprecated(since = "2.44")
  public void setOwner(String owner) {
    // Not supported
  }

  /**
   * DataEntryForm does not support sharing: there is no sharing column, so writes through the
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

  /** DataEntryForm does not support sharing. */
  @Override
  @Deprecated(since = "2.44")
  public void setSharing(Sharing sharing) {
    // Not supported
  }

  /** DataEntryForm does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** DataEntryForm does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** DataEntryForm does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** DataEntryForm does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }
}
