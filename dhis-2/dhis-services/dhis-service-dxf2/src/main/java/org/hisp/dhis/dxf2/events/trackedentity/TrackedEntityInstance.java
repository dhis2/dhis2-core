/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.dxf2.events.trackedentity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement(localName = "trackedEntityInstance", namespace = DxfNamespaces.DXF_2_0)
public class TrackedEntityInstance {
  private String trackedEntityType;

  private String trackedEntityInstance;

  private String orgUnit;

  private String created;

  private String lastUpdated;

  private String createdAtClient;

  private String lastUpdatedAtClient;

  private List<Relationship> relationships = new ArrayList<>();

  private List<Attribute> attributes = new ArrayList<>();

  private List<Enrollment> enrollments = new ArrayList<>();

  private List<ProgramOwner> programOwners = new ArrayList<>();

  private Boolean inactive;

  private Boolean deleted = false;

  private Boolean potentialDuplicate = false;

  private FeatureType featureType = FeatureType.NONE;

  private String coordinates;

  private Geometry geometry;

  private String storedBy;

  private UserInfoSnapshot createdByUserInfo;

  private UserInfoSnapshot lastUpdatedByUserInfo;

  public TrackedEntityInstance() {}

  /** Trims the value property of attribute values to null. */
  public void trimValuesToNull() {
    if (attributes != null) {
      for (Attribute attribute : attributes) {
        attribute.setValue(StringUtils.trimToNull(attribute.getValue()));
      }
    }
  }

  public void clear() {
    this.setDeleted(null);
    this.setFeatureType(null);
    this.setEnrollments(null);
    this.setRelationships(null);
    this.setAttributes(null);
  }

  @JsonProperty(required = true)
  @JacksonXmlProperty(isAttribute = true)
  public String getTrackedEntityType() {
    return trackedEntityType;
  }

  public void setTrackedEntityType(String trackedEntityType) {
    this.trackedEntityType = trackedEntityType;
  }

  @JsonProperty(required = true)
  @JacksonXmlProperty(isAttribute = true)
  public String getTrackedEntityInstance() {
    return trackedEntityInstance;
  }

  public void setTrackedEntityInstance(String trackedEntityInstance) {
    this.trackedEntityInstance = trackedEntityInstance;
  }

  @JsonProperty(required = true)
  @JacksonXmlProperty(isAttribute = true)
  public String getOrgUnit() {
    return orgUnit;
  }

  public void setOrgUnit(String orgUnit) {
    this.orgUnit = orgUnit;
  }

  @JsonProperty(required = true)
  @JacksonXmlProperty(isAttribute = true)
  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  @JsonProperty(required = true)
  @JacksonXmlProperty(isAttribute = true)
  public String getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(String lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  @JsonProperty(required = true)
  @JacksonXmlProperty(isAttribute = true)
  public String getCreatedAtClient() {
    return createdAtClient;
  }

  public void setCreatedAtClient(String createdAtClient) {
    this.createdAtClient = createdAtClient;
  }

  @JsonProperty(required = true)
  @JacksonXmlProperty(isAttribute = true)
  public String getLastUpdatedAtClient() {
    return lastUpdatedAtClient;
  }

  public void setLastUpdatedAtClient(String lastUpdatedAtClient) {
    this.lastUpdatedAtClient = lastUpdatedAtClient;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public List<Relationship> getRelationships() {
    return relationships;
  }

  public void setRelationships(List<Relationship> relationships) {
    this.relationships = relationships;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "attributes", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "attribute", namespace = DxfNamespaces.DXF_2_0)
  public List<Attribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<Attribute> attributes) {
    this.attributes = attributes;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "enrollments", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "enrollment", namespace = DxfNamespaces.DXF_2_0)
  public List<Enrollment> getEnrollments() {
    return enrollments;
  }

  public void setEnrollments(List<Enrollment> enrollments) {
    this.enrollments = enrollments;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "programOwners", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programOwner", namespace = DxfNamespaces.DXF_2_0)
  public List<ProgramOwner> getProgramOwners() {
    return programOwners;
  }

  public void setProgramOwners(List<ProgramOwner> programOwners) {
    this.programOwners = programOwners;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "inactive", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "inactive", namespace = DxfNamespaces.DXF_2_0)
  public Boolean isInactive() {
    return inactive;
  }

  public void setInactive(Boolean inactive) {
    this.inactive = inactive;
  }

  @JsonProperty
  @JacksonXmlProperty(localName = "deleted", namespace = DxfNamespaces.DXF_2_0)
  public Boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public FeatureType getFeatureType() {
    return featureType;
  }

  public void setFeatureType(FeatureType featureType) {
    this.featureType = featureType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(PropertyType.GEOLOCATION)
  public String getCoordinates() {
    return coordinates;
  }

  public void setCoordinates(String coordinates) {
    this.coordinates = coordinates;
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
  public String getStoredBy() {
    return storedBy;
  }

  public void setStoredBy(String storedBy) {
    this.storedBy = storedBy;
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

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean isPotentialDuplicate() {
    return potentialDuplicate;
  }

  public void setPotentialDuplicate(Boolean potentialDuplicate) {
    this.potentialDuplicate = potentialDuplicate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TrackedEntityInstance that = (TrackedEntityInstance) o;
    return Objects.equals(trackedEntityType, that.trackedEntityType)
        && Objects.equals(trackedEntityInstance, that.trackedEntityInstance)
        && Objects.equals(orgUnit, that.orgUnit)
        && Objects.equals(created, that.created)
        && Objects.equals(createdAtClient, that.createdAtClient)
        && Objects.equals(lastUpdated, that.lastUpdated)
        && Objects.equals(lastUpdatedAtClient, that.lastUpdatedAtClient)
        && Objects.equals(relationships, that.relationships)
        && Objects.equals(attributes, that.attributes)
        && Objects.equals(enrollments, that.enrollments)
        && Objects.equals(inactive, that.inactive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        trackedEntityType,
        trackedEntityInstance,
        orgUnit,
        created,
        createdAtClient,
        lastUpdated,
        lastUpdatedAtClient,
        relationships,
        attributes,
        enrollments,
        inactive);
  }

  @Override
  public String toString() {
    return "TrackedEntityInstance{"
        + "trackedEntityType='"
        + trackedEntityType
        + '\''
        + ", trackedEntityInstance='"
        + trackedEntityInstance
        + '\''
        + ", orgUnit='"
        + orgUnit
        + '\''
        + ", created='"
        + created
        + '\''
        + ", lastUpdated='"
        + lastUpdated
        + '\''
        + ", createdAtClient='"
        + createdAtClient
        + '\''
        + ", lastUpdatedAtClient='"
        + lastUpdatedAtClient
        + '\''
        + ", relationships="
        + relationships
        + ", attributes="
        + attributes
        + ", enrollments="
        + enrollments
        + ", programOwners="
        + programOwners
        + ", inactive="
        + inactive
        + ", deleted="
        + deleted
        + ", featureType="
        + featureType
        + ", coordinates='"
        + coordinates
        + '\''
        + '}';
  }
}
