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
package org.hisp.dhis.message;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import lombok.Setter;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Lars Helge Overland
 */
@Entity
@Table(name = "message")
@JacksonXmlRootElement(localName = "message", namespace = DxfNamespaces.DXF_2_0)
@Setter
public class Message implements IdentifiableObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_sequence")
  @SequenceGenerator(
      name = "message_sequence",
      sequenceName = "message_sequence",
      allocationSize = 1)
  @Column(name = "messageid")
  private long id;

  @Column(name = "uid", unique = true, nullable = false, length = 11)
  private String uid;

  @Column(name = "created", nullable = false, updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  protected Date created;

  /** The message text. */
  @Column(name = "messagetext", columnDefinition = "text")
  private String text;

  /** The message meta data, like user agent and OS of sender. */
  @Column(name = "metadata")
  private String metaData;

  /** The message sender. */
  @ManyToOne
  @JoinColumn(name = "userid", foreignKey = @ForeignKey(name = "fk_message_userid"))
  private User sender;

  /** Internal message flag. Can only be seen by users in "FeedbackRecipients" group. */
  @Column(name = "internal")
  private Boolean internal;

  @Column(name = "lastUpdated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  protected Date lastUpdated;

  /** Attached files */
  @ManyToMany(cascade = CascadeType.ALL)
  @JoinTable(
      name = "messageattachments",
      joinColumns = @JoinColumn(name = "messageid"),
      inverseJoinColumns = @JoinColumn(name = "fileresourceid", unique = true))
  private Set<FileResource> attachments;

  // -------------------------------------------------------------------------------------------
  // Transient fields
  // -------------------------------------------------------------------------------------------

  /**
   * As part of the serializing process, this field can be set to indicate a link to this
   * identifiable object (will be used on the web layer for navigating the REST API)
   */
  @Transient
  protected String href;

  /** Access information for this object. Applies to current user. */
  @Transient 
  protected org.hisp.dhis.security.acl.Access access;

  public Message() {
    this.uid = CodeGenerator.generateUid();
    this.lastUpdated = new Date();
    this.internal = false;
  }

  public Message(String text, String metaData, User sender) {
    this.uid = CodeGenerator.generateUid();
    this.lastUpdated = new Date();
    this.text = text;
    this.metaData = metaData;
    this.sender = sender;
    this.internal = false;
  }

  public Message(String text, String metaData, User sender, boolean internal) {
    this.uid = CodeGenerator.generateUid();
    this.lastUpdated = new Date();
    this.text = text;
    this.metaData = metaData;
    this.sender = sender;
    this.internal = internal;
  }

  @Override
  public int hashCode() {
    return uid != null ? uid.hashCode() : 0;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof Message other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid());
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getHref() {
    return href;
  }

  @Override
  @JsonIgnore
  public long getId() {
    return id;
  }

  @Override
  public String getUid() {
    return uid;
  }

  @Column(name = "created", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @Access(AccessType.PROPERTY)
  @Override
  public Date getCreated() {
    return created;
  }

  @Override
  @JsonProperty
  public Date getLastUpdated() {
    return lastUpdated;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @Override
  @Deprecated
  public String getName() {
    return null;
  }

  @JsonProperty
  @JacksonXmlProperty
  public String getText() {
    return text;
  }

  @JsonProperty
  @JacksonXmlProperty
  public String getMetaData() {
    return metaData;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty
  public User getSender() {
    return sender;
  }

  @Override
  public String toString() {
    return "[" + text + "]";
  }

  @JsonProperty
  @JacksonXmlProperty
  public boolean isInternal() {
    return internal;
  }

  public void setInternal(boolean internal) {
    this.internal = internal;
  }

  @JsonProperty
  @JacksonXmlProperty
  public Set<FileResource> getAttachments() {
    return attachments;
  }

  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(localName = "access", namespace = DxfNamespaces.DXF_2_0)
  public org.hisp.dhis.security.acl.Access getAccess() {
    return access;
  }

  @Deprecated
  @Override
  public String getCode() {
    return "";
  }

  @Deprecated
  @Override
  public String getDisplayName() {
    return "";
  }

  @Deprecated
  @Override
  public User getLastUpdatedBy() {
    return null;
  }

  @Deprecated
  @Override
  public AttributeValues getAttributeValues() {
    return null;
  }

  @Deprecated
  @Override
  public void setAttributeValues(AttributeValues attributeValues) {}

  @Deprecated
  @Override
  public void addAttributeValue(String attributeUid, String value) {}

  @Deprecated
  @Override
  public void removeAttributeValue(String attributeId) {}

  @Deprecated
  @Override
  public Set<Translation> getTranslations() {
    return null;
  }

  @Deprecated
  @Override
  public void setAccess(org.hisp.dhis.security.acl.Access access) {}

  @Deprecated
  @Override
  public User getCreatedBy() {
    return null;
  }

  @Deprecated
  @Override
  public User getUser() {
    return null;
  }

  @Deprecated
  @Override
  public void setCreatedBy(User createdBy) {}

  @Deprecated
  @Override
  public void setUser(User user) {}

  @Deprecated
  @Override
  public Sharing getSharing() {
    return null;
  }

  @Override
  public void setSharing(Sharing sharing) {}

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    return "";
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    return "";
  }

  @Deprecated
  @Override
  public void setName(String name) {}

  @Deprecated
  @Override
  public void setCode(String code) {}

  @Deprecated
  @Override
  public void setOwner(String owner) {}

  @Deprecated
  @Override
  public void setTranslations(Set<Translation> translations) {}

  @Deprecated
  @Override
  public void setLastUpdatedBy(User user) {}

  @Override
  public void setHref(String link) {
    this.href = link;
  }
}
