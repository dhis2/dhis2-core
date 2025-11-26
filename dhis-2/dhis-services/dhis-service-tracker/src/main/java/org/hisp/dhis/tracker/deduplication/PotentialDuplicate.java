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
package org.hisp.dhis.tracker.deduplication;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

@Entity
@Table(name = "potentialduplicate")
@Getter
@Setter
public class PotentialDuplicate implements IdentifiableObject {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "potentialduplicatesequence")
  @Column(name = "potentialduplicateid")
  private long id;

  @Column(name = "uid", unique = true, nullable = false, length = 11)
  private String uid;

  @JsonProperty
  @Column(name = "created", nullable = false, updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @JsonProperty
  @Column(name = "lastUpdated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdated;

  /**
   * original represents the UID of a TrackedEntity. original is required. original is a potential
   * duplicate of duplicate.
   */
  @JsonProperty
  @Type(type = "org.hisp.dhis.hibernate.UIDUserType")
  @Column(name = "original", nullable = false)
  private UID original;

  /**
   * duplicate represents the UID of a TrackedEntity. duplicate is required. duplicate is a
   * potential duplicate of original.
   */
  @JsonProperty
  @Type(type = "org.hisp.dhis.hibernate.UIDUserType")
  @Column(name = "duplicate", nullable = false)
  private UID duplicate;

  @JsonProperty
  @Column(name = "lastupdatebyusername", nullable = false)
  protected String lastUpdatedByUserName;

  @JsonProperty
  @Column(name = "createdbyusername", nullable = false)
  protected String createdByUserName;

  /**
   * status represents the state of the PotentialDuplicate. all new Potential duplicates are OPEN by
   * default.
   */
  @JsonProperty
  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private DeduplicationStatus status = DeduplicationStatus.OPEN;

  public PotentialDuplicate() {}

  public PotentialDuplicate(UID original, UID duplicate) {
    this.original = original;
    this.duplicate = duplicate;
  }

  // -------------------------------------------------------------------------
  // Not supported properties
  // -------------------------------------------------------------------------
  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
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

  @Override
  public String getDisplayName() {
    return getName();
  }

  @Override
  public User getLastUpdatedBy() {
    return null;
  }

  @Override
  public Sharing getSharing() {
    return Sharing.empty();
  }

  @Override
  public void setSharing(Sharing sharing) {
    // not supported
  }

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
  public void setLastUpdatedBy(User user) {
    // not supported
  }

  @Override
  public String getCode() {
    return null;
  }

  @Override
  public void setCode(String code) {
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

  /** PotentialDuplicate does not support sharing */
  @Override
  public boolean hasSharing() {
    return false;
  }

  /** PotentialDuplicate does not support sharing */
  @Override
  public void setOwner(String owner) {
    // not supported
  }

  /** PotentialDuplicate does not support AttributeValues */
  @Override
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** PotentialDuplicate does not support AttributeValues */
  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    // not supported
  }

  /** PotentialDuplicate does not support AttributeValues */
  @Override
  public void addAttributeValue(String attributeUid, String value) {
    // not supported
  }

  /** PotentialDuplicate does not support AttributeValues */
  @Override
  public void removeAttributeValue(String attributeId) {
    // not supported
  }

  /**
   * @deprecated PotentialDuplicate does not support createdBy, use storeBy instead.
   */
  @Override
  public User getUser() {
    return getCreatedBy();
  }

  /**
   * @deprecated PotentialDuplicate does not support createdBy, use storeBy instead.
   */
  @Override
  public void setCreatedBy(User createdBy) {
    // not supported
  }

  /**
   * @deprecated PotentialDuplicate does not support createdBy, use storeBy instead.
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
