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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.util.ObjectUtils;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Abyot Asalefew
 */
@Auditable(scope = AuditScope.TRACKER)
@JacksonXmlRootElement(localName = "enrollment", namespace = DxfNamespaces.DXF_2_0)
public class Enrollment extends SoftDeletableObject {
  private Date createdAtClient;

  private Date lastUpdatedAtClient;

  private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

  @AuditAttribute private OrganisationUnit organisationUnit;

  private Date occurredDate;

  private Date enrollmentDate;

  private Date completedDate;

  private UserInfoSnapshot createdByUserInfo;

  private UserInfoSnapshot lastUpdatedByUserInfo;

  @AuditAttribute private TrackedEntity trackedEntity;

  @AuditAttribute private Program program;

  private Set<TrackerEvent> events = new HashSet<>();

  private Set<RelationshipItem> relationshipItems = new HashSet<>();

  private List<MessageConversation> messageConversations = new ArrayList<>();

  private Boolean followup = false;

  private List<Note> notes = new ArrayList<>();

  private String completedBy;

  private Geometry geometry;

  private String storedBy;

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
    super.setAutoFields();

    if (createdAtClient == null) {
      createdAtClient = created;
    }

    lastUpdatedAtClient = lastUpdated;
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
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public OrganisationUnit getOrganisationUnit() {
    return organisationUnit;
  }

  public Enrollment setOrganisationUnit(OrganisationUnit organisationUnit) {
    this.organisationUnit = organisationUnit;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getOccurredDate() {
    return occurredDate;
  }

  public void setOccurredDate(Date occurredDate) {
    this.occurredDate = occurredDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getEnrollmentDate() {
    return enrollmentDate;
  }

  public void setEnrollmentDate(Date enrollmentDate) {
    this.enrollmentDate = enrollmentDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getCompletedDate() {
    return completedDate;
  }

  public void setCompletedDate(Date completedDate) {
    this.completedDate = completedDate;
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
  public EnrollmentStatus getStatus() {
    return status;
  }

  public void setStatus(EnrollmentStatus status) {
    this.status = status;
  }

  @JsonProperty("trackedEntityInstance")
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(localName = "trackedEntityInstance", namespace = DxfNamespaces.DXF_2_0)
  public TrackedEntity getTrackedEntity() {
    return trackedEntity;
  }

  public void setTrackedEntity(TrackedEntity trackedEntity) {
    this.trackedEntity = trackedEntity;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "events", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "event", namespace = DxfNamespaces.DXF_2_0)
  public Set<TrackerEvent> getEvents() {
    return events;
  }

  public void setEvents(Set<TrackerEvent> events) {
    this.events = events;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getFollowup() {
    return followup;
  }

  public void setFollowup(Boolean followup) {
    this.followup = followup;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "messageConversations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "messageConversation", namespace = DxfNamespaces.DXF_2_0)
  public List<MessageConversation> getMessageConversations() {
    return messageConversations;
  }

  public void setMessageConversations(List<MessageConversation> messageConversations) {
    this.messageConversations = messageConversations;
  }

  @JsonProperty("trackedEntityComments")
  @JacksonXmlElementWrapper(localName = "trackedEntityComments", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "trackedEntityComment", namespace = DxfNamespaces.DXF_2_0)
  public List<Note> getNotes() {
    return notes;
  }

  public void setNotes(List<Note> notes) {
    this.notes = notes;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getCompletedBy() {
    return completedBy;
  }

  public void setCompletedBy(String completedBy) {
    this.completedBy = completedBy;
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
  @JacksonXmlElementWrapper(localName = "relationshipItems", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "relationshipItem", namespace = DxfNamespaces.DXF_2_0)
  public Set<RelationshipItem> getRelationshipItems() {
    return relationshipItems;
  }

  public void setRelationshipItems(Set<RelationshipItem> relationshipItems) {
    this.relationshipItems = relationshipItems;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("uid", uid)
        .add("code", code)
        .add("name", name)
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

  public static final BiFunction<Enrollment, Program, Enrollment> copyOf =
      (original, prog) -> {
        Enrollment copy = new Enrollment();
        copy.setAutoFields();
        setShallowCopyValues(copy, original, prog);
        return copy;
      };

  private static void setShallowCopyValues(
      Enrollment copy, Enrollment original, Program programCopy) {
    copy.setNotes(ObjectUtils.copyOf(original.getNotes()));
    copy.setCompletedBy(original.getCompletedBy());
    copy.setCreatedAtClient(original.getCreatedAtClient());
    copy.setCreatedByUserInfo(original.getCreatedByUserInfo());
    copy.setCompletedDate(original.getCompletedDate());
    copy.setEnrollmentDate(original.getEnrollmentDate());
    copy.setEvents(new HashSet<>());
    copy.setFollowup(original.getFollowup());
    copy.setGeometry(original.getGeometry());
    copy.setOccurredDate(original.getOccurredDate());
    copy.setLastUpdatedAtClient(original.getLastUpdatedAtClient());
    copy.setLastUpdatedByUserInfo(original.getLastUpdatedByUserInfo());
    copy.setMessageConversations(ObjectUtils.copyOf(original.getMessageConversations()));
    copy.setName(original.getName());
    copy.setOrganisationUnit(original.getOrganisationUnit());
    copy.setProgram(programCopy);
    copy.setPublicAccess(original.getPublicAccess());
    copy.setRelationshipItems(ObjectUtils.copyOf(original.getRelationshipItems()));
    copy.setStatus(original.getStatus());
    copy.setStoredBy(original.getStoredBy());
    copy.setTrackedEntity(original.getTrackedEntity());
  }
}
