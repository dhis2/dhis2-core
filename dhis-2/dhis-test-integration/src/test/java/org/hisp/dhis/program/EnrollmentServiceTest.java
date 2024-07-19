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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.note.NoteService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@Transactional
class EnrollmentServiceTest extends PostgresIntegrationTestBase {
  private static final UID ENROLLMENT_A_UID = UID.of(CodeGenerator.generateUid());
  private static final UID ENROLLMENT_B_UID = UID.of(CodeGenerator.generateUid());
  private static final UID ENROLLMENT_C_UID = UID.of(CodeGenerator.generateUid());
  private static final UID ENROLLMENT_D_UID = UID.of(CodeGenerator.generateUid());
  private static final UID EVENT_UID = UID.of(CodeGenerator.generateUid());

  @Autowired private EnrollmentService apiEnrollmentService;

  @Autowired private TrackerObjectDeletionService trackerObjectDeletionService;

  @Autowired private org.hisp.dhis.tracker.export.enrollment.EnrollmentService enrollmentService;

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private NoteService noteService;

  @Autowired private CategoryService categoryService;

  @Autowired private IdentifiableObjectManager manager;

  private Date incidentDate;

  private Date enrollmentDate;

  private CategoryOptionCombo coA;

  private Program programA;

  private Program programB;

  private Program programC;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private Event eventA;

  private Enrollment enrollmentA;

  private Enrollment enrollmentB;

  private Enrollment enrollmentC;

  private Enrollment enrollmentD;

  private TrackedEntity trackedEntityA;

  private User user;

  @BeforeEach
  void setUp() {
    coA = categoryService.getDefaultCategoryOptionCombo();

    organisationUnitA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnitA);
    organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);

    user =
        createAndAddUser(
            false, "user", Set.of(organisationUnitA), Set.of(organisationUnitA), "F_EXPORT_DATA");
    user.setTeiSearchOrganisationUnits(Set.of(organisationUnitA, organisationUnitB));
    user.setOrganisationUnits(Set.of(organisationUnitA));

    programA = createProgram('A', new HashSet<>(), organisationUnitA);
    programService.addProgram(programA);
    programA.setSharing(Sharing.builder().publicAccess("rwrw----").build());
    ProgramStage stageA = createProgramStage('A', programA);
    stageA.setSortOrder(1);
    programStageService.saveProgramStage(stageA);
    ProgramStage stageB = createProgramStage('B', programA);
    stageB.setSortOrder(2);
    programStageService.saveProgramStage(stageB);
    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(stageA);
    programStages.add(stageB);
    programA.setProgramStages(programStages);
    programService.updateProgram(programA);
    programB = createProgram('B', new HashSet<>(), organisationUnitA);
    programService.addProgram(programB);
    programC = createProgram('C', new HashSet<>(), organisationUnitA);
    programService.addProgram(programC);
    trackedEntityA = createTrackedEntity(organisationUnitA);
    trackedEntityService.addTrackedEntity(trackedEntityA);
    TrackedEntity trackedEntityB = createTrackedEntity(organisationUnitB);
    trackedEntityService.addTrackedEntity(trackedEntityB);
    DateTime testDate1 = DateTime.now();
    testDate1.withTimeAtStartOfDay();
    testDate1 = testDate1.minusDays(70);
    incidentDate = testDate1.toDate();
    DateTime testDate2 = DateTime.now();
    testDate2.withTimeAtStartOfDay();
    enrollmentDate = testDate2.toDate();
    enrollmentA = new Enrollment(enrollmentDate, incidentDate, trackedEntityA, programA);
    enrollmentA.setUid(ENROLLMENT_A_UID.getValue());
    enrollmentA.setOrganisationUnit(organisationUnitA);
    enrollmentB = new Enrollment(enrollmentDate, incidentDate, trackedEntityA, programB);
    enrollmentB.setUid(ENROLLMENT_B_UID.getValue());
    enrollmentB.setStatus(EnrollmentStatus.CANCELLED);
    enrollmentB.setOrganisationUnit(organisationUnitB);
    enrollmentC = new Enrollment(enrollmentDate, incidentDate, trackedEntityA, programC);
    enrollmentC.setUid(ENROLLMENT_C_UID.getValue());
    enrollmentC.setStatus(EnrollmentStatus.COMPLETED);
    enrollmentC.setOrganisationUnit(organisationUnitA);
    enrollmentD = new Enrollment(enrollmentDate, incidentDate, trackedEntityB, programA);
    enrollmentD.setUid(ENROLLMENT_D_UID.getValue());
    enrollmentD.setOrganisationUnit(organisationUnitB);
    eventA = new Event(enrollmentA, stageA);
    eventA.setUid(EVENT_UID.getValue());
    eventA.setOrganisationUnit(organisationUnitA);
    eventA.setAttributeOptionCombo(coA);

    injectSecurityContextUser(user);
  }

  @Test
  void testAddEnrollment() {
    manager.save(enrollmentA);
    manager.save(enrollmentB);
    assertNotNull(manager.get(Enrollment.class, enrollmentA.getUid()));
    assertNotNull(manager.get(Enrollment.class, enrollmentB.getUid()));
  }

  @Test
  void testDeleteEnrollment() {
    manager.save(enrollmentA);
    manager.save(enrollmentB);
    assertNotNull(manager.get(Enrollment.class, enrollmentA.getUid()));
    assertNotNull(manager.get(Enrollment.class, enrollmentB.getUid()));
    apiEnrollmentService.deleteEnrollment(enrollmentA);
    assertNull(manager.get(Enrollment.class, enrollmentA.getUid()));
    assertNotNull(manager.get(Enrollment.class, enrollmentB.getUid()));
    apiEnrollmentService.deleteEnrollment(enrollmentB);
    assertNull(manager.get(Enrollment.class, enrollmentA.getUid()));
    assertNull(manager.get(Enrollment.class, enrollmentB.getUid()));
  }

  @Test
  void testSoftDeleteEnrollmentAndLinkedEvent() throws NotFoundException {
    manager.save(enrollmentA);
    manager.save(eventA);
    long eventIdA = eventA.getId();
    enrollmentA.setEvents(Sets.newHashSet(eventA));
    manager.update(enrollmentA);
    assertNotNull(manager.get(Enrollment.class, enrollmentA.getUid()));
    assertNotNull(manager.get(Event.class, eventIdA));

    trackerObjectDeletionService.deleteEnrollments(List.of(enrollmentA.getUid()));

    assertNull(manager.get(Enrollment.class, enrollmentA.getUid()));
    assertNull(manager.get(Event.class, eventIdA));
  }

  @Test
  void testUpdateEnrollment() {
    manager.save(enrollmentA);
    assertNotNull(manager.get(Enrollment.class, enrollmentA.getUid()));
    enrollmentA.setOccurredDate(enrollmentDate);
    manager.update(enrollmentA);
    assertEquals(
        enrollmentDate, manager.get(Enrollment.class, enrollmentA.getUid()).getOccurredDate());
  }

  @Test
  void testGetEnrollmentById() {
    manager.save(enrollmentA);
    manager.save(enrollmentB);
    assertEquals(enrollmentA, manager.get(Enrollment.class, enrollmentA.getUid()));
    assertEquals(enrollmentB, manager.get(Enrollment.class, enrollmentB.getUid()));
  }

  @Test
  void testGetEnrollmentByUid() {
    manager.save(enrollmentA);
    manager.save(enrollmentB);
    assertEquals(
        ENROLLMENT_A_UID.getValue(),
        manager.get(Enrollment.class, ENROLLMENT_A_UID.getValue()).getUid());
    assertEquals(
        ENROLLMENT_B_UID.getValue(),
        manager.get(Enrollment.class, ENROLLMENT_B_UID.getValue()).getUid());
  }

  @Test
  void testGetEnrollmentsByProgram() {
    manager.save(enrollmentA);
    manager.save(enrollmentB);
    manager.save(enrollmentD);
    List<Enrollment> enrollments = apiEnrollmentService.getEnrollments(programA);
    assertEquals(2, enrollments.size());
    assertTrue(enrollments.contains(enrollmentA));
    assertTrue(enrollments.contains(enrollmentD));
    enrollments = apiEnrollmentService.getEnrollments(programB);
    assertEquals(1, enrollments.size());
    assertTrue(enrollments.contains(enrollmentB));
  }

  @Test
  void testGetEnrollmentsByTrackedEntityProgramAndEnrollmentStatus() {
    manager.save(enrollmentA);
    Enrollment enrollment1 =
        apiEnrollmentService.enrollTrackedEntity(
            trackedEntityA, programA, enrollmentDate, incidentDate, organisationUnitA);
    enrollment1.setStatus(EnrollmentStatus.COMPLETED);
    manager.update(enrollment1);
    Enrollment enrollment2 =
        apiEnrollmentService.enrollTrackedEntity(
            trackedEntityA, programA, enrollmentDate, incidentDate, organisationUnitA);
    enrollment2.setStatus(EnrollmentStatus.COMPLETED);
    manager.update(enrollment2);
    List<Enrollment> enrollments =
        apiEnrollmentService.getEnrollments(trackedEntityA, programA, EnrollmentStatus.COMPLETED);
    assertEquals(2, enrollments.size());
    assertTrue(enrollments.contains(enrollment1));
    assertTrue(enrollments.contains(enrollment2));
    enrollments =
        apiEnrollmentService.getEnrollments(trackedEntityA, programA, EnrollmentStatus.ACTIVE);
    assertEquals(1, enrollments.size());
    assertTrue(enrollments.contains(enrollmentA));
  }

  @Test
  void testEnrollTrackedEntity() {
    Enrollment enrollment =
        apiEnrollmentService.enrollTrackedEntity(
            trackedEntityA, programB, enrollmentDate, incidentDate, organisationUnitA);
    assertNotNull(manager.get(Enrollment.class, enrollment.getId()));
  }

  @Test
  void shouldNotDeleteNoteWhenDeletingEnrollment() throws ForbiddenException, NotFoundException {

    Note note = new Note();
    note.setCreator(CodeGenerator.generateUid());
    note.setNoteText("text");
    noteService.addNote(note);

    enrollmentA.setNotes(List.of(note));

    manager.save(enrollmentA);

    assertNotNull(enrollmentService.getEnrollment(enrollmentA.getUid()));

    apiEnrollmentService.deleteEnrollment(enrollmentA);

    Assertions.assertThrows(
        NotFoundException.class, () -> enrollmentService.getEnrollment(enrollmentA.getUid()));
    assertTrue(noteService.noteExists(note.getUid()));
  }
}
