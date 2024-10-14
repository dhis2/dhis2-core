/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.imports.converter;

import static org.hisp.dhis.tracker.imports.domain.EnrollmentStatus.ACTIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnrollmentTrackerConverterServiceTest extends DhisConvenienceTest {
  private static final Date NOW = new Date();

  private static final Date YESTERDAY = DateUtils.addDays(NOW, -1);

  private static final String TE_UID = CodeGenerator.generateUid();

  private static final String ENROLLMENT_UID = CodeGenerator.generateUid();

  private static final String NOTE_UID = CodeGenerator.generateUid();

  private static final String ORGANISATION_UNIT_UID = CodeGenerator.generateUid();

  private static final String PROGRAM_UID = CodeGenerator.generateUid();

  public TrackerPreheat preheat;

  private final Program program = createProgram('A');

  private OrganisationUnit organisationUnit;

  private TrackedEntityType trackerEntityType;

  private UserDetails updatingUser;

  private UserDetails creatingUser;

  private User userC;

  private User userU;

  private TrackerConverterService<org.hisp.dhis.tracker.imports.domain.Enrollment, Enrollment>
      converter = new EnrollmentTrackerConverterService(new NotesConverterService());

  @BeforeEach
  void setUpTest() {
    userC = makeUser("C");
    creatingUser = UserDetails.fromUser(userC);
    userU = makeUser("U");
    updatingUser = UserDetails.fromUser(userU);

    trackerEntityType = createTrackedEntityType('A');
    organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(ORGANISATION_UNIT_UID);
    program.setUid(PROGRAM_UID);

    preheat = new TrackerPreheat();
    preheat.put(TrackerIdSchemeParam.UID, program);
    preheat.put(TrackerIdSchemeParam.UID, organisationUnit);
    preheat.put(TrackerIdSchemeParam.UID, trackerEntityType);
    preheat.setUser(userC);
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
    preheat.setUser(userC);
    preheat.setUserInfo(UserInfoSnapshot.from(creatingUser));

    Enrollment actual = converter.from(preheat, enrollment);

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
    preheat.setUser(userC);
    preheat.setUserInfo(UserInfoSnapshot.from(creatingUser));

    Enrollment actual = converter.from(preheat, enrollment);

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
    preheat.setUser(userU);
    preheat.setUserInfo(UserInfoSnapshot.from(updatingUser));

    Enrollment actual = converter.from(preheat, enrollment);

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
    preheat.setUser(userU);
    preheat.setUserInfo(UserInfoSnapshot.from(updatingUser));

    Enrollment actual = converter.from(preheat, enrollment);

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
    preheat.setUser(userU);
    preheat.setUserInfo(UserInfoSnapshot.from(updatingUser));

    Enrollment actual = converter.from(preheat, enrollment);

    assertMappedEnrollment(enrollment, actual, creatingUser, updatingUser);
    assertEquals(actual.getEnrollmentDate(), actual.getOccurredDate());
    assertNull(actual.getCompletedBy());
    assertNull(actual.getCompletedDate());
  }

  private void assertMappedEnrollment(
      org.hisp.dhis.tracker.imports.domain.Enrollment enrollment,
      Enrollment actual,
      UserDetails createdBy,
      UserDetails updatedBy) {
    assertEquals(enrollment.getUid(), actual.getUid());
    assertEquals(enrollment.getTrackedEntity(), actual.getTrackedEntity().getUid());
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
    assertEquals(enrollment.getStatus().getProgramStatus(), actual.getStatus());
    assertEquals(enrollment.getGeometry(), actual.getGeometry());
    assertEquals(enrollment.isFollowUp(), actual.getFollowup());
    assertNotes(enrollment.getNotes(), actual.getNotes(), updatedBy);
  }

  private void assertNotes(
      List<org.hisp.dhis.tracker.imports.domain.Note> notes,
      List<Note> dbNotes,
      UserDetails updatedBy) {
    for (org.hisp.dhis.tracker.imports.domain.Note note : notes) {
      Note dbNote =
          dbNotes.stream().filter(n -> n.getUid().equals(note.getNote())).findFirst().orElse(null);
      assertNotNull(dbNote);
      assertEquals(note.getValue(), dbNote.getNoteText());
      assertEquals(note.getStoredBy(), dbNote.getCreator());
      assertEquals(updatedBy.getUid(), dbNote.getLastUpdatedBy().getUid());
    }
  }

  private TrackedEntity trackedEntity() {
    TrackedEntity dbTrackedEntity = new TrackedEntity();
    dbTrackedEntity.setUid(TE_UID);
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
    dbEnrollment.setUid(ENROLLMENT_UID);
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
    dbEnrollment.setStatus(status.getProgramStatus());
    dbEnrollment.setStoredBy(creatingUser.getUsername());
    return dbEnrollment;
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
