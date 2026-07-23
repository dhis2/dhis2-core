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
package org.hisp.dhis.tracker.model;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseTrackerObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.SoftDeletableEntity;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Abyot Asalefew
 * @author Stian Sandvold
 */
@Entity
@Table(name = "relationship")
@Auditable(scope = AuditScope.TRACKER)
public class Relationship extends BaseTrackerObject
    implements IdentifiableObject, SoftDeletableEntity, Serializable {
  /** Determines if a de-serialized file is compatible with this class. */
  private static final long serialVersionUID = 3818815755138507997L;

  @Id
  @Column(name = "relationshipid")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "relationship_sequence")
  @SequenceGenerator(
      name = "relationship_sequence",
      sequenceName = "relationship_sequence",
      allocationSize = 1)
  private long id;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lastupdatedby", foreignKey = @ForeignKey(name = "fk_lastupdateby_userid"))
  private User lastUpdatedBy;

  private boolean deleted = false;

  @Column(name = "createdatclient")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdAtClient;

  @AuditAttribute
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "relationshiptypeid",
      foreignKey = @ForeignKey(name = "fk_relationship_relationshiptypeid"),
      nullable = false)
  private RelationshipType relationshipType;

  @AuditAttribute
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "from_relationshipitemid",
      foreignKey = @ForeignKey(name = "fk_relationship_from_relationshipitemid"),
      unique = true)
  @Cascade(
      value = {
        org.hibernate.annotations.CascadeType.ALL,
        org.hibernate.annotations.CascadeType.DELETE_ORPHAN
      })
  private RelationshipItem from;

  @AuditAttribute
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "to_relationshipitemid",
      foreignKey = @ForeignKey(name = "fk_relationship_to_relationshipitemid"),
      unique = true)
  @Cascade(
      value = {
        org.hibernate.annotations.CascadeType.ALL,
        org.hibernate.annotations.CascadeType.DELETE_ORPHAN
      })
  private RelationshipItem to;

  @Type(type = "jbObjectStyle")
  @Column(name = "style")
  private ObjectStyle style;

  @Transient private String formName;

  @Transient private String description;

  /**
   * The key is an aggregated representation of the relationship and its sides based on uids. The
   * format is type_from_to
   */
  @Column(name = "key", length = 255, nullable = false)
  private String key;

  /**
   * The inverted key is a key, but with the sides switched. This will make it possible to match a
   * key when it is bidirectional. the format is type_to_from
   */
  @Column(name = "inverted_key", length = 255, nullable = false)
  private String invertedKey;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Relationship() {}

  @Override
  public void setAutoFields() {
    if (getUid() == null || getUid().isEmpty()) {
      setUid(CodeGenerator.generateUid());
    }

    Date date = new Date();

    if (getCreated() == null) {
      setCreated(date);
    }

    setLastUpdated(date);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonIgnore
  public Set<UID> getTrackedEntityOrigins() {
    Set<UID> uids = new HashSet<>();

    Optional.ofNullable(this.getFrom().getTrackedEntity()).map(UID::of).ifPresent(uids::add);

    if (this.getRelationshipType().isBidirectional()) {
      Optional.ofNullable(this.getTo().getTrackedEntity()).map(UID::of).ifPresent(uids::add);
    }

    return uids;
  }

  @JsonProperty
  public Date getCreatedAtClient() {
    return createdAtClient;
  }

  public void setCreatedAtClient(Date createdAtClient) {
    this.createdAtClient = createdAtClient;
  }

  /**
   * @return the relationshipType
   */
  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  public RelationshipType getRelationshipType() {
    return relationshipType;
  }

  /**
   * @param relationshipType the relationshipType to set
   */
  public void setRelationshipType(RelationshipType relationshipType) {
    this.relationshipType = relationshipType;
  }

  @JsonProperty
  public ObjectStyle getStyle() {
    return style;
  }

  public void setStyle(ObjectStyle style) {
    this.style = style;
  }

  @JsonProperty
  public String getFormName() {
    return formName;
  }

  public void setFormName(String formName) {
    this.formName = formName;
  }

  @JsonProperty
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty
  public RelationshipItem getFrom() {
    return from;
  }

  public void setFrom(RelationshipItem from) {
    this.from = from;
  }

  @JsonProperty
  public RelationshipItem getTo() {
    return to;
  }

  public void setTo(RelationshipItem to) {
    this.to = to;
  }

  @JsonIgnore
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @JsonIgnore
  public String getInvertedKey() {
    return invertedKey;
  }

  public void setInvertedKey(String invertedKey) {
    this.invertedKey = invertedKey;
  }

  @Override
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public void setCode(String code) {
    this.code = code;
  }

  @Override
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @Override
  public void setLastUpdatedBy(User lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  @Override
  public boolean isDeleted() {
    return deleted;
  }

  @Override
  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
    }
    if (idScheme.is(IdentifiableProperty.CODE)) {
      return code;
    }
    if (idScheme.is(IdentifiableProperty.ID)) {
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
  // Equals and hashCode
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    return Objects.hash(result, deleted);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getRealClass(this) != getRealClass(obj)) return false;

    Relationship other = (Relationship) obj;
    return Objects.equals(getUid(), other.getUid())
        && Objects.equals(getCode(), other.getCode())
        && Objects.equals(getName(), other.getName())
        && isDeleted() == other.isDeleted();
  }

  @Override
  public String toString() {
    return "Relationship{"
        + "id="
        + id
        + ", relationshipType="
        + relationshipType
        + ", from="
        + from
        + ", to="
        + to
        + ", style="
        + style
        + ", formName='"
        + formName
        + '\''
        + ", description='"
        + description
        + '\''
        + '}';
  }

  // -------------------------------------------------------------------------
  // Not supported methods
  // -------------------------------------------------------------------------

  @Override
  public String getDisplayName() {
    return getName();
  }

  @Override
  public Sharing getSharing() {
    return Sharing.empty();
  }

  @Override
  public void setSharing(Sharing sharing) {
    // not supported
  }

  // -------------------------------------------------------------------------
  // Not supported properties
  // -------------------------------------------------------------------------

  @Override
  public Set<Translation> getTranslations() {
    return Set.of();
  }

  @Override
  public void setAccess(Access access) {
    // not supported
  }

  /**
   * @param user
   * @deprecated This method is replaced by {@link #setCreatedBy(User)} ()} Currently it is only
   *     used for web api backward compatibility
   */
  @Override
  public void setUser(User user) {
    // not supported
  }

  @Override
  public Access getAccess() {
    return null;
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    // not supported
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public void setName(String name) {
    // not supported
  }

  /** Relationship does not support sharing */
  @Override
  public boolean hasSharing() {
    return false;
  }

  /** Relationship does not support sharing */
  @Override
  public void setOwner(String owner) {
    // not supported
  }

  /** Relationship does not support AttributeValues */
  @Override
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** Relationship does not support AttributeValues */
  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    // not supported
  }

  /** Relationship does not support AttributeValues */
  @Override
  public void addAttributeValue(String attributeUid, String value) {
    // not supported
  }

  /** Relationship does not support AttributeValues */
  @Override
  public void removeAttributeValue(String attributeId) {
    // not supported
  }

  /**
   * @deprecated Relationship does not support createdBy.
   */
  @Override
  public User getUser() {
    return getCreatedBy();
  }

  /**
   * @deprecated Relationship does not support createdBy.
   */
  @Override
  public void setCreatedBy(User createdBy) {
    // not supported
  }

  /**
   * @deprecated Relationship does not support createdBy.
   */
  @Override
  public User getCreatedBy() {
    return null;
  }

  @Override
  public String getHref() {
    return "";
  }

  @Override
  public void setHref(String link) {
    // not supported
  }
}
