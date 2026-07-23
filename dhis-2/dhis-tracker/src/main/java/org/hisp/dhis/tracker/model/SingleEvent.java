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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseTrackerObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.SoftDeletableEntity;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.locationtech.jts.geom.Geometry;

@Entity
@Table(name = "singleevent")
@Auditable(scope = AuditScope.TRACKER)
@Setter
@Getter
@NoArgsConstructor
public class SingleEvent extends BaseTrackerObject
    implements IdentifiableObject, SoftDeletableEntity {

  @Id
  @Column(name = "eventid")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "singleevent_sequence")
  @SequenceGenerator(
      name = "singleevent_sequence",
      sequenceName = "singleevent_sequence",
      allocationSize = 1)
  private long id;

  @Column(name = "createdatclient")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdAtClient;

  @Column(name = "lastupdatedatclient")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdatedAtClient;

  @Type(type = "jbUserInfoSnapshot")
  @Column(name = "createdbyuserinfo")
  private UserInfoSnapshot createdByUserInfo;

  @Type(type = "jbUserInfoSnapshot")
  @Column(name = "lastupdatedbyuserinfo")
  private UserInfoSnapshot lastUpdatedByUserInfo;

  @AuditAttribute
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "programstageid",
      foreignKey = @ForeignKey(name = "fk_singleevent_programstageid"),
      nullable = false)
  private ProgramStage programStage;

  @Column(name = "occurreddate")
  @Temporal(TemporalType.TIMESTAMP)
  private Date occurredDate;

  @AuditAttribute
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "organisationunitid",
      foreignKey = @ForeignKey(name = "fk_singleevent_organisationunitid"),
      nullable = false)
  private OrganisationUnit organisationUnit;

  @AuditAttribute
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "attributeoptioncomboid",
      foreignKey = @ForeignKey(name = "fk_singleevent_attributeoptioncomboid"),
      nullable = false)
  private CategoryOptionCombo attributeOptionCombo;

  @ListIndexBase(1)
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
  @JoinTable(
      name = "singleevent_notes",
      joinColumns = @JoinColumn(name = "eventid"),
      inverseJoinColumns = @JoinColumn(name = "noteid"))
  @OrderColumn(name = "sort_order")
  private List<Note> notes = new ArrayList<>();

  @AuditAttribute
  @Type(type = "jsbEventDataValues")
  @Column(name = "eventdatavalues")
  private Set<EventDataValue> eventDataValues = new HashSet<>();

  @OneToMany(mappedBy = "singleEvent", fetch = FetchType.LAZY)
  private Set<RelationshipItem> relationshipItems = new HashSet<>();

  @AuditAttribute
  @Column(name = "status", length = 25, nullable = false)
  @Enumerated(EnumType.STRING)
  private EventStatus status = EventStatus.ACTIVE;

  @Column(name = "completedby")
  private String completedBy;

  @Column(name = "completeddate")
  @Temporal(TemporalType.TIMESTAMP)
  private Date completedDate;

  @Column(name = "lastsynchronized")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastSynchronized = new Date(0);

  @Column(name = "geometry", columnDefinition = "geometry")
  private Geometry geometry;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "assigneduserid",
      foreignKey = @ForeignKey(name = "fk_singleevent_assigneduserid"))
  private User assignedUser;

  @Column(name = "deleted")
  private boolean deleted = false;

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

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

    if (createdAtClient == null) {
      createdAtClient = created;
    }

    lastUpdatedAtClient = lastUpdated;
  }

  public boolean hasAttributeOptionCombo() {
    return attributeOptionCombo != null;
  }

  // -------------------------------------------------------------------------
  // Equals and hashCode
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    return Objects.hash(result, deleted);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getRealClass(this) != getRealClass(obj)) return false;

    SingleEvent other = (SingleEvent) obj;
    return Objects.equals(getUid(), other.getUid()) && isDeleted() == other.isDeleted();
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @Override
  public long getId() {
    return id;
  }

  @Override
  public boolean isDeleted() {
    return deleted;
  }

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

  // -------------------------------------------------------------------------
  // Not supported methods
  // -------------------------------------------------------------------------

  @Override
  public User getLastUpdatedBy() {
    return getCreatedBy();
  }

  @Override
  public void setLastUpdatedBy(User user) {
    // not supported
  }

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

  /** SingleEvent does not support sharing */
  @Override
  public boolean hasSharing() {
    return false;
  }

  /** SingleEvent does not support sharing */
  @Override
  public void setOwner(String owner) {
    // not supported
  }

  /** SingleEvent does not support AttributeValues */
  @Override
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** SingleEvent does not support AttributeValues */
  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    // not supported
  }

  /** SingleEvent does not support AttributeValues */
  @Override
  public void addAttributeValue(String attributeUid, String value) {
    // not supported
  }

  /** SingleEvent does not support AttributeValues */
  @Override
  public void removeAttributeValue(String attributeId) {
    // not supported
  }

  /**
   * @deprecated SingleEvent does not support createdBy, use storeBy instead.
   */
  @Override
  public User getUser() {
    return getCreatedBy();
  }

  /**
   * @deprecated SingleEvent does not support createdBy, use storeBy instead.
   */
  @Override
  public void setCreatedBy(User createdBy) {
    // not supported
  }

  /**
   * @deprecated SingleEvent does not support createdBy, use storeBy instead.
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
