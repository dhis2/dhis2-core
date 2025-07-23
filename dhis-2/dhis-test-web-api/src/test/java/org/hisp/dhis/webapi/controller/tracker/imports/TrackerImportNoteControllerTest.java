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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Set;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.webapi.controller.tracker.JsonNote;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackerImportNoteControllerTest extends PostgresControllerIntegrationTestBase {
  @Autowired TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;
  private User importUser;

  private TrackerEvent event;

  private Enrollment enrollment;

  @BeforeAll
  void setUp() {
    importUser = makeUser("o");
    manager.save(importUser, false);

    CategoryOptionCombo coc = categoryService.getDefaultCategoryOptionCombo();

    OrganisationUnit orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit);

    importUser.addOrganisationUnit(orgUnit);
    manager.update(importUser);

    Program program = createProgram('A');
    program.getSharing().addUserAccess(new UserAccess(importUser, AccessStringHelper.DATA_READ));
    manager.save(program, false);

    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType);
    trackedEntityType
        .getSharing()
        .addUserAccess(new UserAccess(importUser, AccessStringHelper.DATA_READ));
    manager.update(trackedEntityType);

    ProgramStage programStage = createProgramStage('A', program);
    programStage
        .getSharing()
        .addUserAccess(new UserAccess(importUser, AccessStringHelper.DATA_READ));
    manager.save(programStage, false);

    TrackedEntity te = createTrackedEntity(orgUnit, trackedEntityType);
    te.setTrackedEntityType(trackedEntityType);
    manager.save(te);

    enrollment = enrollment(te, program, orgUnit);
    event = event(enrollment, programStage, coc);
    enrollment.setEvents(Set.of(event));
    manager.update(enrollment);

    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(te, program, orgUnit);
  }

  @BeforeEach
  void injectUser() {
    injectSecurityContextUser(importUser);
  }

  @Test
  void shouldReturnBadRequestWhenValueIsNullForEventNote() {
    JsonWebMessage webMessage =
        POST(
                "/tracker/events/" + event.getUid() + "/note",
                """

                    {
                       "creator": "I am the creator"
                    }
                    """)
            .content(HttpStatus.BAD_REQUEST)
            .as(JsonWebMessage.class);

    assertEquals("Value cannot be empty", webMessage.getMessage());
  }

  @Test
  void shouldCreateEventNote() {
    JsonNote note =
        POST(
                "/tracker/events/" + event.getUid() + "/note",
                """

                        {
                           "value": "This is a note"
                        }
                        """)
            .content(HttpStatus.OK)
            .as(JsonNote.class);

    assertEquals("This is a note", note.getValue());
    assertTrue(CodeGenerator.isValidUid(note.getNote()));
  }

  @Test
  void shouldCreateEventNoteWhenNoteUidIsProvided() {
    UID noteUid = UID.generate();
    JsonNote note =
        POST(
                "/tracker/events/" + event.getUid() + "/note",
                """

                        {
                           "note": "%s",
                           "value": "This is a note"
                        }
                        """
                    .formatted(noteUid.getValue()))
            .content(HttpStatus.OK)
            .as(JsonNote.class);

    assertEquals("This is a note", note.getValue());
    assertEquals(noteUid.getValue(), note.getNote());
  }

  @Test
  void shouldReturnBadRequestWhenValueIsNullForEnrollmentNote() {
    JsonWebMessage webMessage =
        POST(
                "/tracker/enrollments/" + enrollment.getUid() + "/note",
                """
                        {
                           "creator": "I am the creator"
                        }
                        """)
            .content(HttpStatus.BAD_REQUEST)
            .as(JsonWebMessage.class);

    assertEquals("Value cannot be empty", webMessage.getMessage());
  }

  @Test
  void shouldCreateEnrollmentNote() {
    JsonNote note =
        POST(
                "/tracker/enrollments/" + enrollment.getUid() + "/note",
                """
                        {
                           "value": "This is a note"
                        }
                        """)
            .content(HttpStatus.OK)
            .as(JsonNote.class);

    assertEquals("This is a note", note.getValue());
    assertTrue(CodeGenerator.isValidUid(note.getNote()));
  }

  @Test
  void shouldCreateEnrollmentNoteWhenNoteUidIsProvided() {
    UID noteUid = UID.generate();
    JsonNote note =
        POST(
                "/tracker/enrollments/" + enrollment.getUid() + "/note",
                """
                        {
                           "note": "%s",
                           "value": "This is a note"
                        }
                        """
                    .formatted(noteUid.getValue()))
            .content(HttpStatus.OK)
            .as(JsonNote.class);

    assertEquals("This is a note", note.getValue());
    assertEquals(noteUid.getValue(), note.getNote());
  }

  private TrackerEvent event(
      Enrollment enrollment, ProgramStage programStage, CategoryOptionCombo coc) {
    TrackerEvent eventA = new TrackerEvent();
    eventA.setEnrollment(enrollment);
    eventA.setProgramStage(programStage);
    eventA.setOrganisationUnit(enrollment.getOrganisationUnit());
    eventA.setAttributeOptionCombo(coc);
    eventA.setAutoFields();
    manager.save(eventA);
    return eventA;
  }

  private Enrollment enrollment(TrackedEntity te, Program program, OrganisationUnit orgUnit) {
    Enrollment enrollmentA = new Enrollment(program, te, orgUnit);
    enrollmentA.setAutoFields();
    enrollmentA.setEnrollmentDate(new Date());
    enrollmentA.setOccurredDate(new Date());
    enrollmentA.setStatus(EnrollmentStatus.COMPLETED);
    enrollmentA.setFollowup(true);
    manager.save(enrollmentA, false);
    te.setEnrollments(Set.of(enrollmentA));
    manager.save(te, false);
    return enrollmentA;
  }
}
