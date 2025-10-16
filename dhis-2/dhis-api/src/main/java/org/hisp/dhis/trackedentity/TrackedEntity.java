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

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseTrackerObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.SoftDeletableEntity;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Abyot Asalefew Gizaw
 */
@Entity
@Setter
@Table(name = "trackedentity")
@JacksonXmlRootElement(localName = "trackedEntityInstance", namespace = DxfNamespaces.DXF_2_0)
@Auditable(scope = AuditScope.TRACKER)
public class TrackedEntity extends BaseTrackerObject implements IdentifiableObject,
    SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(sequenceName = "trackedentityinstance_sequence")
  @Column(name = "trackedentityid")
  private long id;
  
  private boolean deleted = false;

  @Column(name = "createdatclient")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdAtClient;

  @Column(name = "lastupdatedatclient")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdatedAtClient;

  @OneToMany(mappedBy = "trackedEntity", fetch = FetchType.LAZY)
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

    TrackedEntity other = (TrackedEntity) obj;
    return Objects.equals(getUid(), other.getUid())
           && Objects.equals(getCode(), other.getCode())
           && Objects.equals(getName(), other.getName())
           && isDeleted() == other.isDeleted();
  }
  
  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @Override
  @JsonIgnore
  public long getId() {
    return id;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(localName = "deleted", namespace = DxfNamespaces.DXF_2_0)
  public boolean isDeleted() {
    return deleted;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isPotentialDuplicate() {
    return potentialDuplicate;
  }


  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getCreatedAtClient() {
    return createdAtClient;
  }


  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getLastUpdatedAtClient() {
    return lastUpdatedAtClient;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public OrganisationUnit getOrganisationUnit() {
    return organisationUnit;
  }


  @JsonProperty("trackedEntityAttributeValues")
  @JacksonXmlElementWrapper(
      localName = "trackedEntityAttributeValues",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "trackedEntityAttributeValue", namespace = DxfNamespaces.DXF_2_0)
  public Set<TrackedEntityAttributeValue> getTrackedEntityAttributeValues() {
    return trackedEntityAttributeValues;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "enrollments", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "enrollment", namespace = DxfNamespaces.DXF_2_0)
  public Set<Enrollment> getEnrollments() {
    return enrollments;
  }
  
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "programOwners", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programOwners", namespace = DxfNamespaces.DXF_2_0)
  public Set<TrackedEntityProgramOwner> getProgramOwners() {
    return programOwners;
  }


  @JsonProperty
  @JacksonXmlElementWrapper(localName = "trackedEntityType", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "trackedEntityType", namespace = DxfNamespaces.DXF_2_0)
  public TrackedEntityType getTrackedEntityType() {
    return trackedEntityType;
  }


  @JsonProperty
  @JacksonXmlProperty(localName = "inactive", namespace = DxfNamespaces.DXF_2_0)
  public boolean isInactive() {
    return inactive;
  }


  @JsonIgnore
  public Date getLastSynchronized() {
    return lastSynchronized;
  }


  @JsonProperty
  @JacksonXmlElementWrapper(localName = "relationshipItems", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "relationshipItem", namespace = DxfNamespaces.DXF_2_0)
  public Set<RelationshipItem> getRelationshipItems() {
    return relationshipItems;
  }


  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Geometry getGeometry() {
    return geometry;
  }


  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public UserInfoSnapshot getCreatedByUserInfo() {
    return createdByUserInfo;
  }


  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public UserInfoSnapshot getLastUpdatedByUserInfo() {
    return lastUpdatedByUserInfo;
  }

  @Override
  public String toString() {
    return "TrackedEntity{"
        + "id="
        + id
        + ", uid='"
        + uid
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
  public String getDisplayName() {
    return getName();
  }
  
  @Override
  public Sharing getSharing() {
    return Sharing.empty();
  }

  @Override
  public void setSharing(Sharing sharing) {
  }

  // -------------------------------------------------------------------------
  // Not supported properties
  // -------------------------------------------------------------------------
  
  @Override
  public Set<Translation> getTranslations() {
    return Set.of();
  }

  /**
   * @param user
   * @deprecated This method is replaced by {@link #setCreatedBy(User)} ()} Currently it is only
   * used for web api backward compatibility
   */
  @Override
  public void setUser(User user) {
    
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
  
  /**
   * TrackedEntity does not support sharing
   */
  @Override
  public boolean hasSharing() {
    return false;
  }

  /**
   * TrackedEntity does not support sharing
   */
  @Override
  public void setOwner(String owner) {
  }

  /**
   * TrackedEntity does not support AttributeValues
   */
  @Override
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /**
   * TrackedEntity does not support AttributeValues
   */
  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    // not supported
  }

  /**
   * TrackedEntity does not support AttributeValues
   */
  @Override
  public void addAttributeValue(String attributeUid, String value) {
    // not supported
  }

  /**
   * TrackedEntity does not support AttributeValues
   */
  @Override
  public void removeAttributeValue(String attributeId) {
    // not supported
  }

  /**
   * @deprecated TrackedEntity does not support createdBy, use storeBy instead.
   */
  @Override
  public User getUser() {
    return getCreatedBy();
  }

  /**
   * @deprecated TrackedEntity does not support createdBy, use storeBy instead.
   */
  @Override
  public void setCreatedBy(User createdBy) {
    // not supported
  }

  /**
   * @deprecated TrackedEntity does not support createdBy, use storeBy instead.
   */  @Override
  public User getCreatedBy() {
    return null;
  }
}
