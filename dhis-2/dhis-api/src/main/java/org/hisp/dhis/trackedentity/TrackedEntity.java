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
package org.hisp.dhis.trackedentity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.hibernate.annotations.Type;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.SoftDeletable;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Abyot Asalefew Gizaw
 */
@JacksonXmlRootElement(localName = "trackedEntityInstance", namespace = DxfNamespaces.DXF_2_0)
@Auditable(scope = AuditScope.TRACKER)
@Entity
@Table(name = "trackedentity")
public class TrackedEntity extends BaseIdentifiableObject implements SoftDeletable {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(sequenceName = "trackedentityinstance_sequence")
  @Column(name = "trackedentityid")
  private long id;

  /** Indicates whether the object is soft deleted. */
  @AuditAttribute
  @Column(name = "deleted")
  private boolean deleted = false;
  
  @Column(name = "createdatclient")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdAtClient;

  @Column(name = "lastupdatedatclient")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdatedAtClient;

  @OneToMany(mappedBy = "trackedEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<TrackedEntityAttributeValue> trackedEntityAttributeValues = new LinkedHashSet<>();

  @OneToMany(mappedBy = "trackedEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<RelationshipItem> relationshipItems = new HashSet<>();

  @OneToMany(mappedBy = "trackedEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<Enrollment> enrollments = new HashSet<>();

  @OneToMany(mappedBy = "trackedEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<TrackedEntityProgramOwner> programOwners = new HashSet<>();

  @Column(name = "potentialDuplicate")
  private boolean potentialDuplicate;

  @AuditAttribute
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organisationunitid", foreignKey = @ForeignKey(name = "fk_trackedentityinstance_organisationunitid"))
  private OrganisationUnit organisationUnit;

  @AuditAttribute
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trackedentitytypeid", foreignKey = @ForeignKey(name = "fk_trackedentityinstance_trackedentitytypeid"))
  private TrackedEntityType trackedEntityType;

  @AuditAttribute
  @Column(name = "inactive")
  private boolean inactive;

  @Column(name = "geometry")
  private Geometry geometry;

  @Column(name = "lastsynchronized")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastSynchronized = new Date(0);

  @Column(name = "storedby", length = 255)
  private String storedBy;

  @Type(type = "jbUserInfoSnapshot")
  @Column(name = "createdbyuserinfo")
  private UserInfoSnapshot createdByUserInfo;

  @Type(type = "jbUserInfoSnapshot")
  @Column(name = "lastupdatedbyuserinfo")
  private UserInfoSnapshot lastUpdatedByUserInfo;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public TrackedEntity() {}

  @Override
  public boolean isDeleted() {
    return deleted;
  }

  @Override
  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  @Override
  public void setAutoFields() {
    super.setAutoFields();

    if (createdAtClient == null) {
      createdAtClient = created;
    }

    if (lastUpdatedAtClient == null) {
      lastUpdatedAtClient = lastUpdated;
    }
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void addAttributeValue(TrackedEntityAttributeValue attributeValue) {
    trackedEntityAttributeValues.add(attributeValue);
    attributeValue.setTrackedEntity(this);
  }

  public void removeAttributeValue(TrackedEntityAttributeValue attributeValue) {
    trackedEntityAttributeValues.remove(attributeValue);
    attributeValue.setTrackedEntity(null);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------


  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isPotentialDuplicate() {
    return potentialDuplicate;
  }

  public void setPotentialDuplicate(boolean potentialDuplicate) {
    this.potentialDuplicate = potentialDuplicate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getCreatedAtClient() {
    return createdAtClient;
  }

  public void setCreatedAtClient(Date createdAtClient) {
    this.createdAtClient = createdAtClient;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getLastUpdatedAtClient() {
    return lastUpdatedAtClient;
  }

  public void setLastUpdatedAtClient(Date lastUpdatedAtClient) {
    this.lastUpdatedAtClient = lastUpdatedAtClient;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getStoredBy() {
    return storedBy;
  }

  public void setStoredBy(String storedBy) {
    this.storedBy = storedBy;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public OrganisationUnit getOrganisationUnit() {
    return organisationUnit;
  }

  public void setOrganisationUnit(OrganisationUnit organisationUnit) {
    this.organisationUnit = organisationUnit;
  }

  @JsonProperty("trackedEntityAttributeValues")
  @JacksonXmlElementWrapper(
      localName = "trackedEntityAttributeValues",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "trackedEntityAttributeValue", namespace = DxfNamespaces.DXF_2_0)
  public Set<TrackedEntityAttributeValue> getTrackedEntityAttributeValues() {
    return trackedEntityAttributeValues;
  }

  public void setTrackedEntityAttributeValues(
      Set<TrackedEntityAttributeValue> trackedEntityAttributeValues) {
    this.trackedEntityAttributeValues = trackedEntityAttributeValues;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "enrollments", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "enrollment", namespace = DxfNamespaces.DXF_2_0)
  public Set<Enrollment> getEnrollments() {
    return enrollments;
  }

  public void setEnrollments(Set<Enrollment> enrollments) {
    this.enrollments = enrollments;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "programOwners", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programOwners", namespace = DxfNamespaces.DXF_2_0)
  public Set<TrackedEntityProgramOwner> getProgramOwners() {
    return programOwners;
  }

  public void setProgramOwners(Set<TrackedEntityProgramOwner> programOwners) {
    this.programOwners = programOwners;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "trackedEntityType", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "trackedEntityType", namespace = DxfNamespaces.DXF_2_0)
  public TrackedEntityType getTrackedEntityType() {
    return trackedEntityType;
  }

  public void setTrackedEntityType(TrackedEntityType trackedEntityType) {
    this.trackedEntityType = trackedEntityType;
  }

  @JsonProperty
  @JacksonXmlProperty(localName = "inactive", namespace = DxfNamespaces.DXF_2_0)
  public boolean isInactive() {
    return inactive;
  }

  public void setInactive(boolean inactive) {
    this.inactive = inactive;
  }

  @JsonIgnore
  public Date getLastSynchronized() {
    return lastSynchronized;
  }

  public void setLastSynchronized(Date lastSynchronized) {
    this.lastSynchronized = lastSynchronized;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "relationshipItems", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "relationshipItem", namespace = DxfNamespaces.DXF_2_0)
  public Set<RelationshipItem> getRelationshipItems() {
    return relationshipItems;
  }

  public void setRelationshipItems(Set<RelationshipItem> relationshipItems) {
    this.relationshipItems = relationshipItems;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Geometry getGeometry() {
    return geometry;
  }

  public void setGeometry(Geometry geometry) {
    this.geometry = geometry;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public UserInfoSnapshot getCreatedByUserInfo() {
    return createdByUserInfo;
  }

  public void setCreatedByUserInfo(UserInfoSnapshot createdByUserInfo) {
    this.createdByUserInfo = createdByUserInfo;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public UserInfoSnapshot getLastUpdatedByUserInfo() {
    return lastUpdatedByUserInfo;
  }

  public void setLastUpdatedByUserInfo(UserInfoSnapshot lastUpdatedByUserInfo) {
    this.lastUpdatedByUserInfo = lastUpdatedByUserInfo;
  }

  @Override
  public String toString() {
    return "TrackedEntity{"
        + "id="
        + id
        + ", uid='"
        + uid
        + '\''
        + ", name='"
        + name
        + '\''
        + ", organisationUnit="
        + organisationUnit
        + ", trackedEntityType="
        + trackedEntityType
        + ", inactive="
        + inactive
        + ", deleted="
        + isDeleted()
        + ", lastSynchronized="
        + lastSynchronized
        + '}';
  }
}
