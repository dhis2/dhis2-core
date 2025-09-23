/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.imports.bundle;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.util.RelationshipKeySupport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;

/**
 * TrackerObjectsMapper maps tracker domain objects to Hibernate objects so they can be persisted in
 * the DB. This class provides static methods to convert imported domain objects such as {@link
 * TrackedEntity}, {@link Enrollment}, {@link TrackerEvent}, and {@link Relationship} into their
 * corresponding database entities. It gets existing records from the database through the preheat
 * and maps the incoming data accordingly, ensuring that all necessary fields are populated
 * correctly. All the values that should be set by the system are set here (eg. createdAt,
 * updatedBy...)
 */
public class TrackerObjectsMapper {
  private TrackerObjectsMapper() {
    throw new IllegalStateException("Utility class");
  }

  public static @Nonnull TrackedEntity map(
      @Nonnull TrackerPreheat preheat,
      @Nonnull org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity,
      @Nonnull UserDetails user) {
    TrackedEntity dbTrackedEntity = preheat.getTrackedEntity(trackedEntity.getTrackedEntity());

    Date now = new Date();

    if (dbTrackedEntity == null) {
      dbTrackedEntity = new TrackedEntity();
      dbTrackedEntity.setUid(trackedEntity.getTrackedEntity().getValue());
      dbTrackedEntity.setCreated(now);
      dbTrackedEntity.setCreatedByUserInfo(UserInfoSnapshot.from(user));
      dbTrackedEntity.setStoredBy(trackedEntity.getStoredBy());
    }

    dbTrackedEntity.setLastUpdated(now);
    dbTrackedEntity.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));
    dbTrackedEntity.setCreatedAtClient(DateUtils.fromInstant(trackedEntity.getCreatedAtClient()));
    dbTrackedEntity.setLastUpdatedAtClient(
        DateUtils.fromInstant(trackedEntity.getUpdatedAtClient()));

    OrganisationUnit organisationUnit = preheat.getOrganisationUnit(trackedEntity.getOrgUnit());
    dbTrackedEntity.setOrganisationUnit(organisationUnit);
    TrackedEntityType trackedEntityType =
        preheat.getTrackedEntityType(trackedEntity.getTrackedEntityType());
    dbTrackedEntity.setTrackedEntityType(trackedEntityType);

    dbTrackedEntity.setPotentialDuplicate(trackedEntity.isPotentialDuplicate());
    dbTrackedEntity.setInactive(trackedEntity.isInactive());
    dbTrackedEntity.setGeometry(trackedEntity.getGeometry());

    return dbTrackedEntity;
  }

  public static @Nonnull Enrollment map(
      @Nonnull TrackerPreheat preheat,
      @Nonnull org.hisp.dhis.tracker.imports.domain.Enrollment enrollment,
      @Nonnull UserDetails user) {
    Enrollment dbEnrollment = preheat.getEnrollment(enrollment.getEnrollment());

    Date now = new Date();

    if (dbEnrollment == null) {
      dbEnrollment = new Enrollment();
      dbEnrollment.setUid(enrollment.getEnrollment().getValue());
      dbEnrollment.setCreated(now);
      dbEnrollment.setStoredBy(enrollment.getStoredBy());
      dbEnrollment.setCreatedByUserInfo(UserInfoSnapshot.from(user));
    }

    dbEnrollment.setLastUpdated(now);
    dbEnrollment.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));
    dbEnrollment.setCreatedAtClient(DateUtils.fromInstant(enrollment.getCreatedAtClient()));
    dbEnrollment.setLastUpdatedAtClient(DateUtils.fromInstant(enrollment.getUpdatedAtClient()));

    OrganisationUnit organisationUnit = preheat.getOrganisationUnit(enrollment.getOrgUnit());
    dbEnrollment.setOrganisationUnit(organisationUnit);
    Program program = preheat.getProgram(enrollment.getProgram());
    dbEnrollment.setProgram(program);
    TrackedEntity trackedEntity = preheat.getTrackedEntity(enrollment.getTrackedEntity());
    dbEnrollment.setTrackedEntity(trackedEntity);

    Date enrollmentDate = DateUtils.fromInstant(enrollment.getEnrolledAt());
    Date occurredDate = DateUtils.fromInstant(enrollment.getOccurredAt());
    dbEnrollment.setEnrollmentDate(enrollmentDate);
    dbEnrollment.setOccurredDate(occurredDate != null ? occurredDate : enrollmentDate);

    dbEnrollment.setFollowup(enrollment.isFollowUp());
    dbEnrollment.setGeometry(enrollment.getGeometry());

    if (enrollment.getStatus() != dbEnrollment.getStatus()) {
      dbEnrollment.setStatus(enrollment.getStatus());
      Date completedDate =
          enrollment.getCompletedAt() == null
              ? now
              : DateUtils.fromInstant(enrollment.getCompletedAt());
      switch (enrollment.getStatus()) {
        case ACTIVE -> {
          dbEnrollment.setCompletedDate(null);
          dbEnrollment.setCompletedBy(null);
        }
        case COMPLETED -> {
          dbEnrollment.setCompletedDate(completedDate);
          dbEnrollment.setCompletedBy(user.getUsername());
        }
        case CANCELLED -> {
          dbEnrollment.setCompletedDate(completedDate);
          dbEnrollment.setCompletedBy(null);
        }
      }
    }

    if (isNotEmpty(enrollment.getNotes())) {
      dbEnrollment
          .getNotes()
          .addAll(
              enrollment.getNotes().stream()
                  .map(note -> map(preheat, note, user))
                  .collect(Collectors.toSet()));
    }
    return dbEnrollment;
  }

  public static @Nonnull TrackerEvent map(
      @Nonnull TrackerPreheat preheat,
      @Nonnull org.hisp.dhis.tracker.imports.domain.TrackerEvent event,
      @Nonnull UserDetails user) {
    TrackerEvent dbEvent = preheat.getTrackerEvent(event.getEvent());

    Date now = new Date();

    if (dbEvent == null) {
      dbEvent = new TrackerEvent();
      dbEvent.setUid(event.getEvent().getValue());
      dbEvent.setCreated(now);
      dbEvent.setStoredBy(event.getStoredBy());
      dbEvent.setCreatedByUserInfo(UserInfoSnapshot.from(user));
    }
    dbEvent.setLastUpdated(now);
    dbEvent.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));
    dbEvent.setCreatedAtClient(DateUtils.fromInstant(event.getCreatedAtClient()));
    dbEvent.setLastUpdatedAtClient(DateUtils.fromInstant(event.getUpdatedAtClient()));

    OrganisationUnit organisationUnit = preheat.getOrganisationUnit(event.getOrgUnit());
    dbEvent.setOrganisationUnit(organisationUnit);
    dbEvent.setEnrollment(preheat.getEnrollment(event.getEnrollment()));
    ProgramStage programStage = preheat.getProgramStage(event.getProgramStage());
    dbEvent.setProgramStage(programStage);

    dbEvent.setOccurredDate(DateUtils.fromInstant(event.getOccurredAt()));
    dbEvent.setScheduledDate(DateUtils.fromInstant(event.getScheduledAt()));

    dbEvent.setGeometry(event.getGeometry());

    EventStatus currentStatus = event.getStatus();
    EventStatus previousStatus = dbEvent.getStatus();
    if (currentStatus != previousStatus && currentStatus == EventStatus.COMPLETED) {
      dbEvent.setCompletedDate(
          event.getCompletedAt() == null ? now : DateUtils.fromInstant(event.getCompletedAt()));
      dbEvent.setCompletedBy(user.getUsername());
    }

    if (currentStatus != EventStatus.COMPLETED) {
      dbEvent.setCompletedDate(null);
      dbEvent.setCompletedBy(null);
    }
    dbEvent.setStatus(currentStatus);

    if (event.getAttributeOptionCombo().isNotBlank()) {
      dbEvent.setAttributeOptionCombo(
          preheat.getCategoryOptionCombo(event.getAttributeOptionCombo()));
    } else {
      dbEvent.setAttributeOptionCombo(preheat.getDefault(CategoryOptionCombo.class));
    }

    if (Boolean.TRUE.equals(programStage.isEnableUserAssignment())
        && event.getAssignedUser() != null
        && !event.getAssignedUser().isEmpty()) {
      Optional<User> assignedUser =
          preheat.getUserByUsername(event.getAssignedUser().getUsername());
      assignedUser.ifPresent(dbEvent::setAssignedUser);
    }

    if (isNotEmpty(event.getNotes())) {
      dbEvent
          .getNotes()
          .addAll(
              event.getNotes().stream()
                  .map(note -> map(preheat, note, user))
                  .collect(Collectors.toSet()));
    }

    return dbEvent;
  }

  public static @Nonnull SingleEvent mapSingleEvent(
      @Nonnull TrackerPreheat preheat,
      @Nonnull org.hisp.dhis.tracker.imports.domain.SingleEvent event,
      @Nonnull UserDetails user) {
    SingleEvent dbEvent = preheat.getSingleEvent(event.getEvent());

    Date now = new Date();

    if (dbEvent == null) {
      dbEvent = new SingleEvent();
      dbEvent.setUid(event.getEvent().getValue());
      dbEvent.setCreated(now);
      dbEvent.setStoredBy(event.getStoredBy());
      dbEvent.setCreatedByUserInfo(UserInfoSnapshot.from(user));
    }
    dbEvent.setLastUpdated(now);
    dbEvent.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));
    dbEvent.setCreatedAtClient(DateUtils.fromInstant(event.getCreatedAtClient()));
    dbEvent.setLastUpdatedAtClient(DateUtils.fromInstant(event.getUpdatedAtClient()));

    OrganisationUnit organisationUnit = preheat.getOrganisationUnit(event.getOrgUnit());
    dbEvent.setOrganisationUnit(organisationUnit);
    ProgramStage programStage = preheat.getProgramStage(event.getProgramStage());
    dbEvent.setProgramStage(programStage);

    dbEvent.setOccurredDate(DateUtils.fromInstant(event.getOccurredAt()));

    dbEvent.setGeometry(event.getGeometry());

    EventStatus currentStatus = event.getStatus();
    EventStatus previousStatus = dbEvent.getStatus();
    if (currentStatus != previousStatus && currentStatus == EventStatus.COMPLETED) {
      dbEvent.setCompletedDate(
          event.getCompletedAt() == null ? now : DateUtils.fromInstant(event.getCompletedAt()));
      dbEvent.setCompletedBy(user.getUsername());
    }

    if (currentStatus != EventStatus.COMPLETED) {
      dbEvent.setCompletedDate(null);
      dbEvent.setCompletedBy(null);
    }
    dbEvent.setStatus(currentStatus);

    if (event.getAttributeOptionCombo().isNotBlank()) {
      dbEvent.setAttributeOptionCombo(
          preheat.getCategoryOptionCombo(event.getAttributeOptionCombo()));
    } else {
      dbEvent.setAttributeOptionCombo(preheat.getDefault(CategoryOptionCombo.class));
    }

    if (Boolean.TRUE.equals(programStage.isEnableUserAssignment())
        && event.getAssignedUser() != null
        && !event.getAssignedUser().isEmpty()) {
      Optional<User> assignedUser =
          preheat.getUserByUsername(event.getAssignedUser().getUsername());
      assignedUser.ifPresent(dbEvent::setAssignedUser);
    }

    if (isNotEmpty(event.getNotes())) {
      dbEvent
          .getNotes()
          .addAll(
              event.getNotes().stream()
                  .map(note -> map(preheat, note, user))
                  .collect(Collectors.toSet()));
    }

    return dbEvent;
  }

  public static @Nonnull Relationship map(
      @Nonnull TrackerPreheat preheat,
      @Nonnull org.hisp.dhis.tracker.imports.domain.Relationship relationship,
      @Nonnull UserDetails user) {
    Date now = new Date();
    Relationship dbRelationship = new org.hisp.dhis.relationship.Relationship();
    dbRelationship.setUid(relationship.getRelationship().getValue());
    dbRelationship.setCreated(now);
    dbRelationship.setLastUpdated(now);
    dbRelationship.setLastUpdatedBy(preheat.getUserByUid(user.getUid()).orElse(null));
    dbRelationship.setCreatedAtClient(DateUtils.fromInstant(relationship.getCreatedAtClient()));

    RelationshipType relationshipType =
        preheat.getRelationshipType(relationship.getRelationshipType());
    dbRelationship.setRelationshipType(relationshipType);

    // FROM
    RelationshipItem fromItem = new org.hisp.dhis.relationship.RelationshipItem();
    fromItem.setRelationship(dbRelationship);
    switch (relationshipType.getFromConstraint().getRelationshipEntity()) {
      case TRACKED_ENTITY_INSTANCE ->
          fromItem.setTrackedEntity(
              preheat.getTrackedEntity(relationship.getFrom().getTrackedEntity()));
      case PROGRAM_INSTANCE ->
          fromItem.setEnrollment(preheat.getEnrollment(relationship.getFrom().getEnrollment()));
      case PROGRAM_STAGE_INSTANCE -> {
        fromItem.setTrackerEvent(preheat.getTrackerEvent(relationship.getFrom().getEvent()));
        fromItem.setSingleEvent(preheat.getSingleEvent(relationship.getFrom().getEvent()));
      }
    }
    dbRelationship.setFrom(fromItem);

    // TO
    RelationshipItem toItem = new org.hisp.dhis.relationship.RelationshipItem();
    toItem.setRelationship(dbRelationship);
    switch (relationshipType.getToConstraint().getRelationshipEntity()) {
      case TRACKED_ENTITY_INSTANCE ->
          toItem.setTrackedEntity(
              preheat.getTrackedEntity(relationship.getTo().getTrackedEntity()));
      case PROGRAM_INSTANCE ->
          toItem.setEnrollment(preheat.getEnrollment(relationship.getTo().getEnrollment()));
      case PROGRAM_STAGE_INSTANCE -> {
        toItem.setTrackerEvent(preheat.getTrackerEvent(relationship.getTo().getEvent()));
        toItem.setSingleEvent(preheat.getSingleEvent(relationship.getTo().getEvent()));
      }
    }
    dbRelationship.setTo(toItem);

    RelationshipKey relationshipKey =
        RelationshipKeySupport.getRelationshipKey(relationship, relationshipType);
    dbRelationship.setKey(relationshipKey.asString());
    dbRelationship.setInvertedKey(relationshipKey.inverseKey().asString());

    return dbRelationship;
  }

  private static @Nonnull Note map(
      @Nonnull TrackerPreheat preheat,
      @Nonnull org.hisp.dhis.tracker.imports.domain.Note note,
      @Nonnull UserDetails user) {

    return map(note, preheat.getUserByUid(user.getUid()).orElse(null));
  }

  public static @Nonnull Note map(
      @Nonnull org.hisp.dhis.tracker.imports.domain.Note note, @CheckForNull User user) {
    Date now = new Date();

    Note dbNote = new Note();
    dbNote.setUid(note.getNote().getValue());
    dbNote.setCreated(now);
    dbNote.setLastUpdatedBy(user);
    dbNote.setCreator(note.getStoredBy());
    dbNote.setNoteText(note.getValue());

    return dbNote;
  }

  // TODO(tracker): To remove when refactoring ProgramNotificationInstanceController
  @Deprecated
  public static TrackerEvent map(SingleEvent singleEvent) {
    if (singleEvent == null) {
      return null;
    }

    TrackerEvent event = new TrackerEvent();
    event.setId(singleEvent.getId());
    event.setUid(singleEvent.getUid());
    event.setCreated(singleEvent.getCreated());
    event.setCreatedBy(singleEvent.getCreatedBy());
    event.setCreatedAtClient(singleEvent.getCreatedAtClient());
    event.setCreatedByUserInfo(singleEvent.getCreatedByUserInfo());
    event.setStoredBy(singleEvent.getStoredBy());
    event.setLastUpdated(singleEvent.getLastUpdated());
    event.setLastUpdatedBy(singleEvent.getLastUpdatedBy());
    event.setLastUpdatedAtClient(singleEvent.getLastUpdatedAtClient());
    event.setLastUpdatedByUserInfo(singleEvent.getLastUpdatedByUserInfo());
    event.setOccurredDate(singleEvent.getOccurredDate());
    event.setCompletedBy(singleEvent.getCompletedBy());
    event.setCompletedDate(singleEvent.getCompletedDate());

    event.setProgramStage(singleEvent.getProgramStage());
    event.setOrganisationUnit(singleEvent.getOrganisationUnit());
    event.setRelationshipItems(singleEvent.getRelationshipItems());

    event.setAssignedUser(singleEvent.getAssignedUser());
    event.setAttributeOptionCombo(singleEvent.getAttributeOptionCombo());
    event.setEventDataValues(singleEvent.getEventDataValues());
    event.setGeometry(singleEvent.getGeometry());
    event.setStatus(singleEvent.getStatus());
    event.setNotes(singleEvent.getNotes());

    return event;
  }
}
