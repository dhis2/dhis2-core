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
package org.hisp.dhis.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.common.BaseTrackerObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.SoftDeletableEntity;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Abyot Asalefew
 */
@Entity
@Table(name = "enrollment")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Auditable(scope = AuditScope.TRACKER)
@Data
public class Enrollment extends BaseTrackerObject
    implements IdentifiableObject, SoftDeletableEntity {

  @Id
  @Column(name = "enrollmentid")
  @GeneratedValue(generator = "programinstance_sequence")
  @SequenceGenerator(sequenceName = "programinstance_sequence")
  private long id;

  @JsonProperty
  @Column(name = "createdatclient")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdAtClient;

  @JsonProperty
  @Column(name = "lastupdatedatclient")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdatedAtClient;

  @JsonProperty
  @Type(type = "jbUserInfoSnapshot")
  @Column(name = "createdbyuserinfo")
  private UserInfoSnapshot createdByUserInfo;

  @JsonProperty
  @Type(type = "jbUserInfoSnapshot")
  @Column(name = "lastupdatedbyuserinfo")
  private UserInfoSnapshot lastUpdatedByUserInfo;

  @JsonProperty
  @Column(name = "occurreddate")
  @Temporal(TemporalType.TIMESTAMP)
  private Date occurredDate;

  @JsonProperty
  @Column(name = "enrollmentdate", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date enrollmentDate;

  @JsonProperty
  @Column(name = "completeddate")
  @Temporal(TemporalType.TIMESTAMP)
  private Date completedDate;

  @JsonProperty
  @Column(name = "followup")
  private Boolean followup = false;

  @JsonProperty
  @Column(name = "completedby")
  private String completedBy;

  @JsonProperty
  @Column(name = "geometry", columnDefinition = "geometry")
  private Geometry geometry;

  @JsonProperty
  @Column(name = "deleted")
  private boolean deleted = false;

  @JsonProperty
  @Column(name = "status", length = 50)
  @Enumerated(EnumType.STRING)
  private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

  @JsonProperty
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trackedentityid", referencedColumnName = "trackedentityid")
  @AuditAttribute
  private TrackedEntity trackedEntity;

  @JsonProperty
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "programid", referencedColumnName = "programid", nullable = false)
  @AuditAttribute
  private Program program;

  @JsonProperty
  @OneToMany(mappedBy = "enrollment", fetch = FetchType.LAZY)
  @OrderBy("occurreddate, scheduleddate")
  private Set<TrackerEvent> events = new HashSet<>();

  @JsonProperty
  @OneToMany(mappedBy = "enrollment", fetch = FetchType.LAZY)
  private Set<RelationshipItem> relationshipItems = new HashSet<>();

  @JsonProperty
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "organisationunitid",
      referencedColumnName = "organisationunitid",
      nullable = false)
  @AuditAttribute
  private OrganisationUnit organisationUnit;

  @JsonProperty
  @ListIndexBase(1)
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
  @JoinTable(
      name = "enrollment_notes",
      joinColumns = @JoinColumn(name = "enrollmentid"),
      inverseJoinColumns = @JoinColumn(name = "noteid"))
  @OrderColumn(name = "sort_order")
  private List<Note> notes = new ArrayList<>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Enrollment() {}

  public Enrollment(
      Date enrollmentDate, Date occurredDate, TrackedEntity trackedEntity, Program program) {
    this.enrollmentDate = enrollmentDate;
    this.occurredDate = occurredDate;
    this.trackedEntity = trackedEntity;
    this.program = program;
  }

  public Enrollment(
      Program program, TrackedEntity trackedEntity, OrganisationUnit organisationUnit) {
    this.program = program;
    this.trackedEntity = trackedEntity;
    this.organisationUnit = organisationUnit;
  }

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

    if (lastUpdatedAtClient == null) {
      lastUpdatedAtClient = lastUpdated;
    }
  }

  // -------------------------------------------------------------------------
  // equals and hashCode
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();

    result = prime * result + ((occurredDate == null) ? 0 : occurredDate.hashCode());
    result = prime * result + ((enrollmentDate == null) ? 0 : enrollmentDate.hashCode());
    result = prime * result + ((trackedEntity == null) ? 0 : trackedEntity.hashCode());
    result = prime * result + ((program == null) ? 0 : program.hashCode());

    return result;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof Enrollment && objectEquals((Enrollment) obj);
  }

  private boolean objectEquals(Enrollment other) {
    return Objects.equals(occurredDate, other.occurredDate)
        && Objects.equals(enrollmentDate, other.enrollmentDate)
        && Objects.equals(trackedEntity, other.trackedEntity)
        && Objects.equals(program, other.program);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @Override
  public long getId() {
    return id;
  }

  @Override
  public void setId(long id) {
    this.id = id;
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
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("uid", uid)
        .add("created", created)
        .add("lastUpdated", lastUpdated)
        .add("organisationUnit", organisationUnit)
        .add("occurredDate", occurredDate)
        .add("enrollmentDate", enrollmentDate)
        .add("trackedEntity", trackedEntity)
        .add("program", program)
        .add("deleted", deleted)
        .add("storedBy", storedBy)
        .toString();
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

  /** Enrollment does not support sharing */
  @Override
  public boolean hasSharing() {
    return false;
  }

  /** Enrollment does not support sharing */
  @Override
  public void setOwner(String owner) {
    // not supported
  }

  /** Enrollment does not support AttributeValues */
  @Override
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** Enrollment does not support AttributeValues */
  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    // not supported
  }

  /** Enrollment does not support AttributeValues */
  @Override
  public void addAttributeValue(String attributeUid, String value) {
    // not supported
  }

  /** Enrollment does not support AttributeValues */
  @Override
  public void removeAttributeValue(String attributeId) {
    // not supported
  }

  /**
   * @deprecated Enrollment does not support createdBy, use storeBy instead.
   */
  @Override
  public User getUser() {
    return getCreatedBy();
  }

  /**
   * @deprecated Enrollment does not support createdBy, use storeBy instead.
   */
  @Override
  public void setCreatedBy(User createdBy) {
    // not supported
  }

  /**
   * @deprecated Enrollment does not support createdBy, use storeBy instead.
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
