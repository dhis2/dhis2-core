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
package org.hisp.dhis.tracker.converter;

import static org.hisp.dhis.tracker.domain.EnrollmentStatus.ACTIVE;
import static org.hisp.dhis.tracker.domain.EnrollmentStatus.CANCELLED;
import static org.hisp.dhis.tracker.domain.EnrollmentStatus.COMPLETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;
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

  public org.hisp.dhis.tracker.preheat.TrackerPreheat preheat;

  private final Program program = createProgram('A');

  private OrganisationUnit organisationUnit;

  private TrackedEntityType trackerEntityType;

  private User userC;

  private User userU;

  private TrackerConverterService<org.hisp.dhis.tracker.domain.Enrollment, ProgramInstance>
      converter = new EnrollmentTrackerConverterService(new NotesConverterService());

  @BeforeEach
  void setUpTest() {
    userC = makeUser("C");
    userU = makeUser("U");

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
  void shouldMapProgramInstanceWhenItIsACreation() {
    preheat.putTrackedEntities(List.of(trackedEntity()));
    org.hisp.dhis.tracker.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_UID)
            .trackedEntity(TE_UID)
            .orgUnit(MetadataIdentifier.ofUid(organisationUnit))
            .program(MetadataIdentifier.ofUid(program))
            .createdAtClient(NOW.toInstant())
            .updatedAtClient(NOW.toInstant())
            .enrolledAt(NOW.toInstant())
            .occurredAt(YESTERDAY.toInstant())
            .status(ACTIVE)
            .storedBy(userC.getUsername())
            .notes(notes(userC))
            .build();
    preheat.setUser(userC);
    preheat.setUserInfo(UserInfoSnapshot.from(userC));

    ProgramInstance actual = converter.from(preheat, enrollment);

    assertMappedProgramInstance(enrollment, actual, userC, userC);
    assertEquals(YESTERDAY, actual.getIncidentDate());
    assertNull(actual.getCompletedBy());
    assertNull(actual.getEndDate());
  }

  @Test
  void shouldMapProgramInstanceWhenItIsACreationAndOccurredDateIsNull() {
    preheat.putTrackedEntities(List.of(trackedEntity()));
    org.hisp.dhis.tracker.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_UID)
            .trackedEntity(TE_UID)
            .orgUnit(MetadataIdentifier.ofUid(organisationUnit))
            .program(MetadataIdentifier.ofUid(program))
            .createdAtClient(NOW.toInstant())
            .updatedAtClient(NOW.toInstant())
            .enrolledAt(NOW.toInstant())
            .status(ACTIVE)
            .storedBy(userC.getUsername())
            .notes(notes(userC))
            .build();
    preheat.setUser(userC);
    preheat.setUserInfo(UserInfoSnapshot.from(userC));

    ProgramInstance actual = converter.from(preheat, enrollment);

    assertMappedProgramInstance(enrollment, actual, userC, userC);
    assertEquals(actual.getEnrollmentDate(), actual.getIncidentDate());
    assertNull(actual.getCompletedBy());
    assertNull(actual.getEndDate());
  }

  @Test
  void shouldMapProgramInstanceWhenItIsAnUpdateAndProgramInstanceGetCompleted() {
    ProgramInstance savedProgramInstance = enrollment(ACTIVE);
    preheat.putTrackedEntities(List.of(savedProgramInstance.getEntityInstance()));
    preheat.putEnrollments(List.of(savedProgramInstance));
    org.hisp.dhis.tracker.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_UID)
            .trackedEntity(TE_UID)
            .orgUnit(MetadataIdentifier.ofUid(organisationUnit))
            .program(MetadataIdentifier.ofUid(program))
            .createdAtClient(NOW.toInstant())
            .updatedAtClient(NOW.toInstant())
            .enrolledAt(NOW.toInstant())
            .status(COMPLETED)
            .storedBy(userC.getUsername())
            .notes(notes(userC))
            .build();
    preheat.setUser(userU);
    preheat.setUserInfo(UserInfoSnapshot.from(userU));

    ProgramInstance actual = converter.from(preheat, enrollment);

    assertMappedProgramInstance(enrollment, actual, userC, userU);
    assertEquals(actual.getEnrollmentDate(), actual.getIncidentDate());
    assertEquals(userU.getUsername(), actual.getCompletedBy());
    assertNotNull(actual.getEndDate());
  }

  @Test
  void shouldMapProgramInstanceWhenItIsAnUpdateAndProgramInstanceGetCancelled() {
    ProgramInstance savedProgramInstance = enrollment(ACTIVE);
    preheat.putTrackedEntities(List.of(savedProgramInstance.getEntityInstance()));
    preheat.putEnrollments(List.of(savedProgramInstance));
    org.hisp.dhis.tracker.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_UID)
            .trackedEntity(TE_UID)
            .orgUnit(MetadataIdentifier.ofUid(organisationUnit))
            .program(MetadataIdentifier.ofUid(program))
            .createdAtClient(NOW.toInstant())
            .updatedAtClient(NOW.toInstant())
            .enrolledAt(NOW.toInstant())
            .status(CANCELLED)
            .storedBy(userC.getUsername())
            .notes(notes(userC))
            .build();
    preheat.setUser(userU);
    preheat.setUserInfo(UserInfoSnapshot.from(userU));

    ProgramInstance actual = converter.from(preheat, enrollment);

    assertMappedProgramInstance(enrollment, actual, userC, userU);
    assertEquals(actual.getEnrollmentDate(), actual.getIncidentDate());
    assertNull(actual.getCompletedBy());
    assertNotNull(actual.getEndDate());
  }

  @Test
  void shouldMapProgramInstanceWhenItIsAnUpdateAndProgramInstanceGetUncompleted() {
    ProgramInstance savedProgramInstance = enrollment(COMPLETED);
    preheat.putTrackedEntities(List.of(savedProgramInstance.getEntityInstance()));
    preheat.putEnrollments(List.of(savedProgramInstance));
    org.hisp.dhis.tracker.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_UID)
            .trackedEntity(TE_UID)
            .orgUnit(MetadataIdentifier.ofUid(organisationUnit))
            .program(MetadataIdentifier.ofUid(program))
            .createdAtClient(NOW.toInstant())
            .updatedAtClient(NOW.toInstant())
            .enrolledAt(NOW.toInstant())
            .status(ACTIVE)
            .storedBy(userC.getUsername())
            .notes(notes(userC))
            .build();
    preheat.setUser(userU);
    preheat.setUserInfo(UserInfoSnapshot.from(userU));

    ProgramInstance actual = converter.from(preheat, enrollment);

    assertMappedProgramInstance(enrollment, actual, userC, userU);
    assertEquals(actual.getEnrollmentDate(), actual.getIncidentDate());
    assertNull(actual.getCompletedBy());
    assertNull(actual.getEndDate());
  }

  private void assertMappedProgramInstance(
      org.hisp.dhis.tracker.domain.Enrollment enrollment,
      ProgramInstance actual,
      User createdBy,
      User updatedBy) {
    assertEquals(enrollment.getUid(), actual.getUid());
    assertEquals(enrollment.getTrackedEntity(), actual.getEntityInstance().getUid());
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
    assertNotes(enrollment.getNotes(), actual.getComments(), updatedBy);
  }

  private void assertNotes(
      List<org.hisp.dhis.tracker.domain.Note> notes,
      List<TrackedEntityComment> dbNotes,
      User updatedBy) {
    for (org.hisp.dhis.tracker.domain.Note note : notes) {
      TrackedEntityComment dbNote =
          dbNotes.stream().filter(n -> n.getUid().equals(note.getNote())).findFirst().orElse(null);
      assertNotNull(dbNote);
      assertEquals(note.getValue(), dbNote.getCommentText());
      assertEquals(note.getStoredBy(), dbNote.getCreator());
      assertEquals(updatedBy.getUid(), dbNote.getLastUpdatedBy().getUid());
    }
  }

  private TrackedEntityInstance trackedEntity() {
    TrackedEntityInstance dbTrackedEntity = new TrackedEntityInstance();
    dbTrackedEntity.setUid(TE_UID);
    dbTrackedEntity.setCreated(NOW);
    dbTrackedEntity.setCreatedByUserInfo(UserInfoSnapshot.from(userC));

    dbTrackedEntity.setLastUpdatedByUserInfo(UserInfoSnapshot.from(userU));
    dbTrackedEntity.setStoredBy(userC.getUsername());
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

  private ProgramInstance enrollment(EnrollmentStatus status) {
    ProgramInstance dbProgramInstance = new ProgramInstance();
    dbProgramInstance.setUid(ENROLLMENT_UID);
    dbProgramInstance.setCreated(NOW);
    dbProgramInstance.setCreatedByUserInfo(UserInfoSnapshot.from(userC));
    dbProgramInstance.setLastUpdatedByUserInfo(UserInfoSnapshot.from(userU));
    dbProgramInstance.setEntityInstance(trackedEntity());
    dbProgramInstance.setOrganisationUnit(organisationUnit);
    dbProgramInstance.setProgram(program);
    dbProgramInstance.setCreatedAtClient(NOW);
    dbProgramInstance.setLastUpdatedAtClient(NOW);
    dbProgramInstance.setEnrollmentDate(NOW);
    dbProgramInstance.setIncidentDate(YESTERDAY);
    dbProgramInstance.setStatus(status.getProgramStatus());
    dbProgramInstance.setStoredBy(userC.getUsername());
    return dbProgramInstance;
  }

  private List<org.hisp.dhis.tracker.domain.Note> notes(User user) {
    return List.of(
        org.hisp.dhis.tracker.domain.Note.builder()
            .note(NOTE_UID)
            .value("This is a note")
            .storedBy(user.getUsername())
            .build());
  }
}
