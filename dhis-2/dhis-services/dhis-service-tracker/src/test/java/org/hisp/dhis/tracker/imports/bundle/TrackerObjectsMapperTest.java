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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hisp.dhis.program.EnrollmentStatus.ACTIVE;
import static org.hisp.dhis.test.utils.Assertions.assertEqualUids;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.util.RelationshipKeySupport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrackerObjectsMapperTest extends TestBase {
  private static final Date NOW = new Date();

  private static final Date YESTERDAY = DateUtils.addDays(NOW, -1);

  private static final UID TE_UID = UID.generate();

  private static final UID ENROLLMENT_UID = UID.generate();

  private static final UID EVENT_UID = UID.generate();

  private static final UID RELATIONSHIP_UID = UID.generate();

  private static final UID NOTE_UID = UID.generate();

  private static final String PROGRAM_STAGE_UID = CodeGenerator.generateUid();

  private static final String ORGANISATION_UNIT_UID = CodeGenerator.generateUid();

  private static final String PROGRAM_UID = CodeGenerator.generateUid();

  private static final String COC_UID = CodeGenerator.generateUid();

  private static final String TE_TO_ENROLLMENT_RELATIONSHIP_TYPE = CodeGenerator.generateUid();

  private static final String TE_TO_EVENT_RELATIONSHIP_TYPE = CodeGenerator.generateUid();

  private static final String TE_TO_TE_RELATIONSHIP_TYPE = CodeGenerator.generateUid();

  private static final Date today = new Date();

  public TrackerPreheat preheat;

  private final Program program = createProgram('A');

  private ProgramStage programStage;

  private OrganisationUnit organisationUnit;

  private TrackedEntityType trackerEntityType;

  private CategoryOptionCombo defaultCategoryOptionCombo;

  private UserDetails updatingUser;

  private UserDetails creatingUser;

  @BeforeEach
  void setUpTest() {
    User user = makeUser("C");
    creatingUser = UserDetails.fromUser(user);
    User userB = makeUser("U");
    updatingUser = UserDetails.fromUser(userB);

    trackerEntityType = createTrackedEntityType('A');
    programStage = createProgramStage('A', 1);
    programStage.setUid(PROGRAM_STAGE_UID);
    programStage.setProgram(program);
    programStage.setEnableUserAssignment(true);
    organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(ORGANISATION_UNIT_UID);
    program.setUid(PROGRAM_UID);

    defaultCategoryOptionCombo = createCategoryOptionCombo('C');
    CategoryOptionCombo categoryOptionCombo = createCategoryOptionCombo('C');
    categoryOptionCombo.setUid(COC_UID);

    RelationshipType teToEnrollmentRelationshipType =
        createTeToEnrollmentRelationshipType('A', program, trackerEntityType, true);
    teToEnrollmentRelationshipType.setUid(TE_TO_ENROLLMENT_RELATIONSHIP_TYPE);
    RelationshipType teToEventRelationshipType =
        createTeToEventRelationshipType('B', program, trackerEntityType, false);
    teToEventRelationshipType.setUid(TE_TO_EVENT_RELATIONSHIP_TYPE);
    RelationshipType teToTeRelationshipType =
        createPersonToPersonRelationshipType('C', program, trackerEntityType, true);
    teToTeRelationshipType.setUid(TE_TO_TE_RELATIONSHIP_TYPE);

    preheat = new TrackerPreheat();
    preheat.put(TrackerIdSchemeParam.UID, programStage);
    preheat.put(TrackerIdSchemeParam.UID, program);
    preheat.put(TrackerIdSchemeParam.UID, organisationUnit);
    preheat.put(TrackerIdSchemeParam.UID, trackerEntityType);
    preheat.addUsers(Set.of(user, userB));
    preheat.putDefault(CategoryOptionCombo.class, defaultCategoryOptionCombo);
    preheat.put(TrackerIdSchemeParam.UID, categoryOptionCombo);
    preheat.put(TrackerIdSchemeParam.UID, teToEnrollmentRelationshipType);
    preheat.put(TrackerIdSchemeParam.UID, teToEventRelationshipType);
    preheat.put(TrackerIdSchemeParam.UID, teToTeRelationshipType);
  }

  @Test
  void shouldMapTrackedEntityWhenItIsACreation() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(TE_UID)
            .orgUnit(MetadataIdentifier.ofUid(organisationUnit.getUid()))
            .trackedEntityType(MetadataIdentifier.ofUid(trackerEntityType))
            .createdAtClient(NOW.toInstant())
            .updatedAtClient(NOW.toInstant())
            .storedBy(creatingUser.getUsername())
            .inactive(true)
            .build();

    TrackedEntity actual = TrackerObjectsMapper.map(preheat, trackedEntity, creatingUser);

    assertMappedTrackedEntity(trackedEntity, actual, creatingUser, creatingUser);
  }

  @Test
  void shouldMapTrackedEntityWhenItIsAnUpdate() {
    preheat.putTrackedEntities(List.of(trackedEntity()));
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(TE_UID)
            .orgUnit(MetadataIdentifier.ofUid(organisationUnit.getUid()))
            .trackedEntityType(MetadataIdentifier.ofUid(trackerEntityType))
            .createdAtClient(NOW.toInstant())
            .updatedAtClient(NOW.toInstant())
            .storedBy(creatingUser.getUsername())
            .inactive(true)
            .build();

    TrackedEntity actual = TrackerObjectsMapper.map(preheat, trackedEntity, updatingUser);

    assertMappedTrackedEntity(trackedEntity, actual, creatingUser, updatingUser);
  }

  @Test
  void shouldMapEnrollmentWhenItIsACreation() {
    preheat.putTrackedEntities(List.of(trackedEntity()));
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_UID)
            .trackedEntity(TE_UID)
            .orgUnit(MetadataIdentifier.ofUid(organisationUnit))
            .program(MetadataIdentifier.ofUid(program))
            .createdAtClient(NOW.toInstant())
            .updatedAtClient(NOW.toInstant())
            .enrolledAt(NOW.toInstant())
            .occurredAt(YESTERDAY.toInstant())
            .status(ACTIVE)
            .storedBy(creatingUser.getUsername())
            .notes(notes(creatingUser))
            .build();

    Enrollment actual = TrackerObjectsMapper.map(preheat, enrollment, creatingUser);

    assertMappedEnrollment(enrollment, actual, creatingUser, creatingUser);
    assertEquals(YESTERDAY, actual.getOccurredDate());
    assertNull(actual.getCompletedBy());
    assertNull(actual.getCompletedDate());
  }

  @Test
  void shouldMapEnrollmentWhenItIsACreationAndOccurredDateIsNull() {
    preheat.putTrackedEntities(List.of(trackedEntity()));
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_UID)
            .trackedEntity(TE_UID)
            .orgUnit(MetadataIdentifier.ofUid(organisationUnit))
            .program(MetadataIdentifier.ofUid(program))
            .createdAtClient(NOW.toInstant())
            .updatedAtClient(NOW.toInstant())
            .enrolledAt(NOW.toInstant())
            .status(ACTIVE)
            .storedBy(creatingUser.getUsername())
            .notes(notes(creatingUser))
            .build();

    Enrollment actual = TrackerObjectsMapper.map(preheat, enrollment, creatingUser);

    assertMappedEnrollment(enrollment, actual, creatingUser, creatingUser);
    assertEquals(actual.getEnrollmentDate(), actual.getOccurredDate());
    assertNull(actual.getCompletedBy());
    assertNull(actual.getCompletedDate());
  }

  @Test
  void shouldMapEnrollmentWhenItIsAnUpdateAndEnrollmentGetCompleted() {
    Enrollment savedEnrollment = enrollment(ACTIVE);
    preheat.putTrackedEntities(List.of(savedEnrollment.getTrackedEntity()));
    preheat.putEnrollments(List.of(savedEnrollment));
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_UID)
            .trackedEntity(TE_UID)
            .orgUnit(MetadataIdentifier.ofUid(organisationUnit))
            .program(MetadataIdentifier.ofUid(program))
            .createdAtClient(NOW.toInstant())
            .updatedAtClient(NOW.toInstant())
            .enrolledAt(NOW.toInstant())
            .status(EnrollmentStatus.COMPLETED)
            .storedBy(creatingUser.getUsername())
            .notes(notes(creatingUser))
            .build();

    Enrollment actual = TrackerObjectsMapper.map(preheat, enrollment, updatingUser);

    assertMappedEnrollment(enrollment, actual, creatingUser, updatingUser);
    assertEquals(actual.getEnrollmentDate(), actual.getOccurredDate());
    assertEquals(updatingUser.getUsername(), actual.getCompletedBy());
    assertNotNull(actual.getCompletedDate());
  }

  @Test
  void shouldMapEnrollmentWhenItIsAnUpdateAndEnrollmentGetCancelled() {
    Enrollment savedEnrollment = enrollment(ACTIVE);
    preheat.putTrackedEntities(List.of(savedEnrollment.getTrackedEntity()));
    preheat.putEnrollments(List.of(savedEnrollment));
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_UID)
            .trackedEntity(TE_UID)
            .orgUnit(MetadataIdentifier.ofUid(organisationUnit))
            .program(MetadataIdentifier.ofUid(program))
            .createdAtClient(NOW.toInstant())
            .updatedAtClient(NOW.toInstant())
            .enrolledAt(NOW.toInstant())
            .status(EnrollmentStatus.CANCELLED)
            .storedBy(creatingUser.getUsername())
            .notes(notes(creatingUser))
            .build();

    Enrollment actual = TrackerObjectsMapper.map(preheat, enrollment, updatingUser);

    assertMappedEnrollment(enrollment, actual, creatingUser, updatingUser);
    assertEquals(actual.getEnrollmentDate(), actual.getOccurredDate());
    assertNull(actual.getCompletedBy());
    assertNotNull(actual.getCompletedDate());
  }

  @Test
  void shouldMapEnrollmentWhenItIsAnUpdateAndEnrollmentGetUncompleted() {
    Enrollment savedEnrollment = enrollment(EnrollmentStatus.COMPLETED);
    preheat.putTrackedEntities(List.of(savedEnrollment.getTrackedEntity()));
    preheat.putEnrollments(List.of(savedEnrollment));
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_UID)
            .trackedEntity(TE_UID)
            .orgUnit(MetadataIdentifier.ofUid(organisationUnit))
            .program(MetadataIdentifier.ofUid(program))
            .createdAtClient(NOW.toInstant())
            .updatedAtClient(NOW.toInstant())
            .enrolledAt(NOW.toInstant())
            .status(ACTIVE)
            .storedBy(creatingUser.getUsername())
            .notes(notes(creatingUser))
            .build();

    Enrollment actual = TrackerObjectsMapper.map(preheat, enrollment, updatingUser);

    assertMappedEnrollment(enrollment, actual, creatingUser, updatingUser);
    assertEquals(actual.getEnrollmentDate(), actual.getOccurredDate());
    assertNull(actual.getCompletedBy());
    assertNull(actual.getCompletedDate());
  }

  @Test
  void shouldMapEventWhenItIsACreation() {
    preheat.putEnrollments(List.of(event(EventStatus.ACTIVE).getEnrollment()));

    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(EVENT_UID)
            .enrollment(ENROLLMENT_UID)
            .status(EventStatus.COMPLETED)
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID))
            .program(MetadataIdentifier.ofUid(PROGRAM_UID))
            .orgUnit(MetadataIdentifier.ofUid(ORGANISATION_UNIT_UID))
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .storedBy(creatingUser.getUsername())
            .build();

    TrackerEvent result = TrackerObjectsMapper.map(preheat, event, creatingUser);

    assertMappedEvent(event, result, creatingUser, creatingUser);
    assertEquals(defaultCategoryOptionCombo.getUid(), result.getAttributeOptionCombo().getUid());
  }

  @Test
  void shouldMapEventWithNullCompletedDataWhenStatusIsActive() {
    TrackerEvent dbEvent = event(EventStatus.COMPLETED);
    preheat.putEnrollments(List.of(dbEvent.getEnrollment()));
    preheat.putTrackerEvents(List.of(dbEvent));

    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(EVENT_UID)
            .enrollment(ENROLLMENT_UID)
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID))
            .program(MetadataIdentifier.ofUid(PROGRAM_UID))
            .orgUnit(MetadataIdentifier.ofUid(ORGANISATION_UNIT_UID))
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .notes(notes(creatingUser))
            .build();

    TrackerEvent result = TrackerObjectsMapper.map(preheat, event, updatingUser);

    assertMappedEvent(event, result, creatingUser, updatingUser);
    assertEquals(defaultCategoryOptionCombo.getUid(), result.getAttributeOptionCombo().getUid());
    assertNull(result.getCompletedBy());
    assertNull(result.getCompletedDate());
  }

  @Test
  void shouldMapEventWhenStatusIsCompleted() {
    TrackerEvent dbEvent = event(EventStatus.ACTIVE);
    preheat.putEnrollments(List.of(dbEvent.getEnrollment()));
    preheat.putTrackerEvents(List.of(dbEvent));

    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(EVENT_UID)
            .enrollment(ENROLLMENT_UID)
            .status(EventStatus.COMPLETED)
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID))
            .program(MetadataIdentifier.ofUid(PROGRAM_UID))
            .orgUnit(MetadataIdentifier.ofUid(ORGANISATION_UNIT_UID))
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .notes(notes(creatingUser))
            .build();

    TrackerEvent result = TrackerObjectsMapper.map(preheat, event, updatingUser);

    assertMappedEvent(event, result, creatingUser, updatingUser);
    assertEquals(defaultCategoryOptionCombo.getUid(), result.getAttributeOptionCombo().getUid());
    assertEquals(updatingUser.getUsername(), result.getCompletedBy());
    assertNotNull(result.getCompletedDate());
  }

  @Test
  void shouldMapEventWhenAssignedUserIsPresent() {
    TrackerEvent dbEvent = event(EventStatus.ACTIVE);
    preheat.putEnrollments(List.of(dbEvent.getEnrollment()));
    preheat.putTrackerEvents(List.of(dbEvent));

    org.hisp.dhis.tracker.imports.domain.User user =
        org.hisp.dhis.tracker.imports.domain.User.builder()
            .uid(creatingUser.getUid())
            .username(creatingUser.getUsername())
            .build();

    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(EVENT_UID)
            .enrollment(ENROLLMENT_UID)
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID))
            .program(MetadataIdentifier.ofUid(PROGRAM_UID))
            .orgUnit(MetadataIdentifier.ofUid(ORGANISATION_UNIT_UID))
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .notes(notes(creatingUser))
            .assignedUser(user)
            .build();

    TrackerEvent result = TrackerObjectsMapper.map(preheat, event, updatingUser);

    assertMappedEvent(event, result, creatingUser, updatingUser);
    assertEquals(defaultCategoryOptionCombo.getUid(), result.getAttributeOptionCombo().getUid());
    assertEquals(event.getAssignedUser().getUid(), result.getAssignedUser().getUid());
  }

  @Test
  void shouldMapEventWhenCategoryOptionComboIsPresent() {
    TrackerEvent dbEvent = event(EventStatus.ACTIVE);
    preheat.putEnrollments(List.of(dbEvent.getEnrollment()));
    preheat.putTrackerEvents(List.of(dbEvent));

    org.hisp.dhis.tracker.imports.domain.User user =
        org.hisp.dhis.tracker.imports.domain.User.builder()
            .uid(creatingUser.getUid())
            .username(creatingUser.getUsername())
            .build();

    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(EVENT_UID)
            .enrollment(ENROLLMENT_UID)
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID))
            .program(MetadataIdentifier.ofUid(PROGRAM_UID))
            .orgUnit(MetadataIdentifier.ofUid(ORGANISATION_UNIT_UID))
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .notes(notes(creatingUser))
            .assignedUser(user)
            .attributeOptionCombo(MetadataIdentifier.ofUid(COC_UID))
            .build();

    TrackerEvent result = TrackerObjectsMapper.map(preheat, event, updatingUser);

    assertMappedEvent(event, result, creatingUser, updatingUser);
    assertEquals(
        event.getAttributeOptionCombo().getIdentifier(), result.getAttributeOptionCombo().getUid());
  }

  @Test
  void testMapRelationshipFromTEToEnrollment() {
    preheat.putTrackedEntities(List.of(trackedEntity()));
    preheat.putEnrollments(List.of(enrollment(ACTIVE)));
    org.hisp.dhis.tracker.imports.domain.Relationship relationship =
        org.hisp.dhis.tracker.imports.domain.Relationship.builder()
            .relationship(RELATIONSHIP_UID)
            .relationshipType(MetadataIdentifier.ofUid(TE_TO_ENROLLMENT_RELATIONSHIP_TYPE))
            .from(RelationshipItem.builder().trackedEntity(TE_UID).build())
            .to(RelationshipItem.builder().enrollment(ENROLLMENT_UID).build())
            .build();

    org.hisp.dhis.relationship.Relationship dbRelationship =
        TrackerObjectsMapper.map(preheat, relationship, creatingUser);

    assertMappedRelationship(relationship, dbRelationship, creatingUser);
  }

  @Test
  void testMapRelationshipFromTEToEvent() {
    preheat.putTrackedEntities(List.of(trackedEntity()));
    preheat.putTrackerEvents(List.of(event(EventStatus.ACTIVE)));
    org.hisp.dhis.tracker.imports.domain.Relationship relationship =
        org.hisp.dhis.tracker.imports.domain.Relationship.builder()
            .relationship(RELATIONSHIP_UID)
            .relationshipType(MetadataIdentifier.ofUid(TE_TO_EVENT_RELATIONSHIP_TYPE))
            .from(RelationshipItem.builder().trackedEntity(TE_UID).build())
            .to(RelationshipItem.builder().event(EVENT_UID).build())
            .build();

    org.hisp.dhis.relationship.Relationship dbRelationship =
        TrackerObjectsMapper.map(preheat, relationship, creatingUser);

    assertMappedRelationship(relationship, dbRelationship, creatingUser);
  }

  @Test
  void testMapRelationshipFromTEToTE() {
    TrackedEntity toTrackedEntity = trackedEntity();
    UID toTrackedEntityUid = UID.generate();
    toTrackedEntity.setUid(toTrackedEntityUid.getValue());
    preheat.putTrackedEntities(List.of(toTrackedEntity, trackedEntity()));
    org.hisp.dhis.tracker.imports.domain.Relationship relationship =
        org.hisp.dhis.tracker.imports.domain.Relationship.builder()
            .relationship(RELATIONSHIP_UID)
            .relationshipType(MetadataIdentifier.ofUid(TE_TO_TE_RELATIONSHIP_TYPE))
            .from(RelationshipItem.builder().trackedEntity(TE_UID).build())
            .to(RelationshipItem.builder().trackedEntity(toTrackedEntityUid).build())
            .build();

    org.hisp.dhis.relationship.Relationship dbRelationship =
        TrackerObjectsMapper.map(preheat, relationship, creatingUser);

    assertMappedRelationship(relationship, dbRelationship, creatingUser);
  }

  private void assertMappedTrackedEntity(
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity,
      TrackedEntity actual,
      UserDetails createdBy,
      UserDetails updatedBy) {
    assertEqualUids(trackedEntity.getTrackedEntity(), actual);
    assertEquals(trackedEntity.getOrgUnit().getIdentifier(), actual.getOrganisationUnit().getUid());
    assertEquals(
        trackedEntity.getTrackedEntityType().getIdentifier(),
        actual.getTrackedEntityType().getUid());
    assertEquals(UserInfoSnapshot.from(createdBy), actual.getCreatedByUserInfo());
    assertEquals(UserInfoSnapshot.from(updatedBy), actual.getLastUpdatedByUserInfo());
    assertEquals(
        DateUtils.fromInstant(trackedEntity.getCreatedAtClient()), actual.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(trackedEntity.getUpdatedAtClient()), actual.getLastUpdatedAtClient());
    assertEquals(createdBy.getUsername(), actual.getStoredBy());
    assertEquals(trackedEntity.isPotentialDuplicate(), actual.isPotentialDuplicate());
    assertEquals(trackedEntity.isInactive(), actual.isInactive());
    assertEquals(trackedEntity.getGeometry(), actual.getGeometry());
  }

  private void assertMappedEnrollment(
      org.hisp.dhis.tracker.imports.domain.Enrollment enrollment,
      Enrollment actual,
      UserDetails createdBy,
      UserDetails updatedBy) {
    assertEqualUids(enrollment.getEnrollment(), actual);
    assertEqualUids(enrollment.getTrackedEntity(), actual.getTrackedEntity());
    assertEquals(enrollment.getOrgUnit().getIdentifier(), actual.getOrganisationUnit().getUid());
    assertEquals(enrollment.getProgram().getIdentifier(), actual.getProgram().getUid());
    assertEquals(UserInfoSnapshot.from(createdBy), actual.getCreatedByUserInfo());
    assertEquals(UserInfoSnapshot.from(updatedBy), actual.getLastUpdatedByUserInfo());
    assertEquals(
        DateUtils.fromInstant(enrollment.getCreatedAtClient()), actual.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(enrollment.getUpdatedAtClient()), actual.getLastUpdatedAtClient());
    assertEquals(DateUtils.fromInstant(enrollment.getEnrolledAt()), actual.getEnrollmentDate());
    assertEquals(createdBy.getUsername(), actual.getStoredBy());
    assertEquals(enrollment.getStatus(), actual.getStatus());
    assertEquals(enrollment.getGeometry(), actual.getGeometry());
    assertEquals(enrollment.isFollowUp(), actual.getFollowup());
    assertNotes(enrollment.getNotes(), actual.getNotes(), updatedBy);
  }

  private void assertMappedEvent(
      org.hisp.dhis.tracker.imports.domain.TrackerEvent event,
      TrackerEvent actual,
      UserDetails createdBy,
      UserDetails updatedBy) {
    assertEqualUids(event.getUid(), actual);
    assertEqualUids(event.getEnrollment(), actual.getEnrollment());
    assertEquals(event.getOrgUnit().getIdentifier(), actual.getOrganisationUnit().getUid());
    assertEquals(event.getProgramStage().getIdentifier(), actual.getProgramStage().getUid());
    assertEquals(UserInfoSnapshot.from(createdBy), actual.getCreatedByUserInfo());
    assertEquals(UserInfoSnapshot.from(updatedBy), actual.getLastUpdatedByUserInfo());
    assertEquals(DateUtils.fromInstant(event.getCreatedAtClient()), actual.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(event.getUpdatedAtClient()), actual.getLastUpdatedAtClient());
    assertEquals(DateUtils.fromInstant(event.getScheduledAt()), actual.getScheduledDate());
    assertEquals(DateUtils.fromInstant(event.getOccurredAt()), actual.getOccurredDate());
    assertEquals(createdBy.getUsername(), actual.getStoredBy());
    assertEquals(event.getStatus(), actual.getStatus());
    assertEquals(event.getGeometry(), actual.getGeometry());
    assertNotes(event.getNotes(), actual.getNotes(), updatedBy);
  }

  private void assertMappedRelationship(
      org.hisp.dhis.tracker.imports.domain.Relationship relationship,
      Relationship actual,
      UserDetails createdBy) {
    assertEqualUids(relationship.getRelationship(), actual);
    assertEquals(createdBy.getUid(), actual.getLastUpdatedBy().getUid());
    assertEquals(
        DateUtils.fromInstant(relationship.getCreatedAtClient()), actual.getCreatedAtClient());
    assertEquals(
        relationship.getRelationshipType().getIdentifier(), actual.getRelationshipType().getUid());
    switch (actual.getRelationshipType().getFromConstraint().getRelationshipEntity()) {
      case TRACKED_ENTITY_INSTANCE ->
          assertEqualUids(
              relationship.getFrom().getTrackedEntity(), actual.getFrom().getTrackedEntity());
      case PROGRAM_INSTANCE ->
          assertEqualUids(relationship.getFrom().getEnrollment(), actual.getFrom().getEnrollment());
      case PROGRAM_STAGE_INSTANCE ->
          assertEqualUids(relationship.getFrom().getEvent(), actual.getFrom().getEvent());
    }

    switch (actual.getRelationshipType().getToConstraint().getRelationshipEntity()) {
      case TRACKED_ENTITY_INSTANCE ->
          assertEqualUids(
              relationship.getTo().getTrackedEntity(), actual.getTo().getTrackedEntity());
      case PROGRAM_INSTANCE ->
          assertEqualUids(relationship.getTo().getEnrollment(), actual.getTo().getEnrollment());
      case PROGRAM_STAGE_INSTANCE ->
          assertEqualUids(relationship.getTo().getEvent(), actual.getTo().getEvent());
    }

    RelationshipKey relationshipKey =
        RelationshipKeySupport.getRelationshipKey(relationship, actual.getRelationshipType());
    assertEquals(relationshipKey.asString(), actual.getKey());
    assertEquals(relationshipKey.inverseKey().asString(), actual.getInvertedKey());
  }

  private void assertNotes(
      List<org.hisp.dhis.tracker.imports.domain.Note> notes,
      List<Note> dbNotes,
      UserDetails updatedBy) {
    for (org.hisp.dhis.tracker.imports.domain.Note note : notes) {
      Note dbNote =
          dbNotes.stream()
              .filter(n -> n.getUid().equals(note.getNote().getValue()))
              .findFirst()
              .orElse(null);
      assertNotNull(dbNote);
      assertEquals(note.getValue(), dbNote.getNoteText());
      assertEquals(note.getStoredBy(), dbNote.getCreator());
      assertEquals(updatedBy.getUid(), dbNote.getLastUpdatedBy().getUid());
    }
  }

  private TrackedEntity trackedEntity() {
    TrackedEntity dbTrackedEntity = new TrackedEntity();
    dbTrackedEntity.setUid(TE_UID.getValue());
    dbTrackedEntity.setCreated(NOW);
    dbTrackedEntity.setCreatedByUserInfo(UserInfoSnapshot.from(creatingUser));

    dbTrackedEntity.setLastUpdatedByUserInfo(UserInfoSnapshot.from(updatingUser));
    dbTrackedEntity.setStoredBy(creatingUser.getUsername());
    dbTrackedEntity.setLastUpdated(NOW);
    dbTrackedEntity.setDeleted(false);
    dbTrackedEntity.setPotentialDuplicate(false);
    dbTrackedEntity.setCreatedAtClient(NOW);
    dbTrackedEntity.setLastUpdatedAtClient(NOW);
    dbTrackedEntity.setOrganisationUnit(organisationUnit);
    dbTrackedEntity.setTrackedEntityType(trackerEntityType);
    dbTrackedEntity.setInactive(false);
    return dbTrackedEntity;
  }

  private Enrollment enrollment(EnrollmentStatus status) {
    Enrollment dbEnrollment = new Enrollment();
    dbEnrollment.setUid(ENROLLMENT_UID.getValue());
    dbEnrollment.setCreated(NOW);
    dbEnrollment.setCreatedByUserInfo(UserInfoSnapshot.from(creatingUser));
    dbEnrollment.setLastUpdatedByUserInfo(UserInfoSnapshot.from(updatingUser));
    dbEnrollment.setTrackedEntity(trackedEntity());
    dbEnrollment.setOrganisationUnit(organisationUnit);
    dbEnrollment.setProgram(program);
    dbEnrollment.setCreatedAtClient(NOW);
    dbEnrollment.setLastUpdatedAtClient(NOW);
    dbEnrollment.setEnrollmentDate(NOW);
    dbEnrollment.setOccurredDate(YESTERDAY);
    dbEnrollment.setStatus(status);
    dbEnrollment.setStoredBy(creatingUser.getUsername());
    return dbEnrollment;
  }

  private TrackerEvent event(EventStatus status) {
    TrackerEvent dbEvent = new TrackerEvent();
    dbEvent.setUid(EVENT_UID.getValue());
    dbEvent.setCreated(NOW);
    dbEvent.setCreatedByUserInfo(UserInfoSnapshot.from(creatingUser));
    dbEvent.setLastUpdatedByUserInfo(UserInfoSnapshot.from(updatingUser));
    dbEvent.setEnrollment(enrollment(ACTIVE));
    dbEvent.setOrganisationUnit(organisationUnit);
    dbEvent.setProgramStage(programStage);
    dbEvent.setCreatedAtClient(NOW);
    dbEvent.setLastUpdatedAtClient(NOW);
    dbEvent.setScheduledDate(NOW);
    dbEvent.setOccurredDate(YESTERDAY);
    dbEvent.setStatus(status);
    dbEvent.setStoredBy(creatingUser.getUsername());
    dbEvent.setAttributeOptionCombo(defaultCategoryOptionCombo);
    dbEvent.setOccurredDate(today);
    dbEvent.setStoredBy(creatingUser.getUsername());
    return dbEvent;
  }

  private List<org.hisp.dhis.tracker.imports.domain.Note> notes(UserDetails user) {
    return List.of(
        org.hisp.dhis.tracker.imports.domain.Note.builder()
            .note(NOTE_UID)
            .value("This is a note")
            .storedBy(user.getUsername())
            .build());
  }
}
