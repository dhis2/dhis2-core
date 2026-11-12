/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for enrollments that are not deleted but belong to a deleted tracked entity {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/tracker_data/enrollments_not_deleted_of_deleted_tracked_entities.yaml}
 */
class DataIntegrityEnrollmentsNotDeletedOfDeletedTrackedEntitiesControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private static final String CHECK = "enrollments_not_deleted_of_deleted_tracked_entities";

  private static final String DETAILS_ID_TYPE = "programs";

  private OrganisationUnit orgUnit;

  private TrackedEntityType trackedEntityType;

  private Program program;

  @BeforeEach
  void setUp() {
    orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit, false);
    // tracker imports require the user to have capture scope access to the org unit
    User admin = userService.getUser(getSuperUser().getUid());
    admin.addOrganisationUnit(orgUnit);
    userService.updateUser(admin);
    switchContextToUser(admin);

    trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType, false);

    program = createProgram('A');
    program.setTrackedEntityType(trackedEntityType);
    manager.save(program, false);
  }

  @Test
  void shouldReportNoIssuesWhenThereIsNoTrackerData() {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void shouldReportNoIssuesWhenEnrollmentsMatchTheirTrackedEntity() {
    enrollment(trackedEntity(false), false);
    enrollment(trackedEntity(true), true);

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void shouldReportProgramWhenEnrollmentIsNotDeletedButItsTrackedEntityIs() {
    enrollment(trackedEntity(false), false);
    Enrollment orphan = enrollment(trackedEntity(true), false);

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, CHECK, 50, program.getUid(), program.getName(), "1 enrollment(s)", true);
    assertEquals(
        List.of(orphan.getUid()),
        getDetails(CHECK).getIssues().get(0).getRefs().toList(JsonString::string));
  }

  @Test
  void shouldReportNoIssuesWhenListedEnrollmentsAreDeletedThroughTheTrackerApi() {
    enrollment(trackedEntity(false), false);
    Enrollment orphan = enrollment(trackedEntity(true), false);

    POST(
            "/tracker?async=false&importStrategy=DELETE",
            """
            {"enrollments": [{"enrollment": "%s"}]}
            """
                .formatted(orphan.getUid()))
        .content(HttpStatus.OK);

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  private TrackedEntity trackedEntity(boolean deleted) {
    TrackedEntity trackedEntity = createTrackedEntity(orgUnit);
    trackedEntity.setTrackedEntityType(trackedEntityType);
    trackedEntity.setDeleted(deleted);
    manager.save(trackedEntity);
    return trackedEntity;
  }

  private Enrollment enrollment(TrackedEntity trackedEntity, boolean deleted) {
    Enrollment enrollment = createEnrollment(program, trackedEntity, orgUnit);
    enrollment.setDeleted(deleted);
    manager.save(enrollment);
    manager.flush();
    return enrollment;
  }
}
