/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.tracker.test.TrackerTestBase.createEnrollment;
import static org.hisp.dhis.tracker.test.TrackerTestBase.createEvent;
import static org.hisp.dhis.tracker.test.TrackerTestBase.createTrackedEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for tracker events that are not deleted but belong to a deleted enrollment or tracked entity
 * {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/tracker_data/tracker_events_not_deleted_of_deleted_enrollments_or_tracked_entities.yaml}
 */
class DataIntegrityTrackerEventsNotDeletedOfDeletedEnrollmentsControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private static final String CHECK =
      "tracker_events_not_deleted_of_deleted_enrollments_or_tracked_entities";

  private static final String DETAILS_ID_TYPE = "programs";

  private OrganisationUnit orgUnit;

  private TrackedEntityType trackedEntityType;

  private Program program;

  private ProgramStage programStage;

  @BeforeEach
  void setUp() {
    orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit, false);

    trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType, false);

    program = createProgram('A');
    program.setTrackedEntityType(trackedEntityType);
    manager.save(program, false);

    programStage = createProgramStage('A', program);
    manager.save(programStage, false);
  }

  @Test
  void shouldReportNoIssuesWhenThereIsNoTrackerData() {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void shouldReportNoIssuesWhenEventsMatchTheirEnrollmentAndTrackedEntity() {
    event(enrollment(trackedEntity(false), false), false);
    event(enrollment(trackedEntity(false), true), true);
    event(enrollment(trackedEntity(true), true), true);

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void shouldReportProgramWhenEventIsNotDeletedButItsEnrollmentOrTrackedEntityIs() {
    event(enrollment(trackedEntity(false), false), false);
    event(enrollment(trackedEntity(false), false), false);
    TrackerEvent inDeletedEnrollment = event(enrollment(trackedEntity(false), true), false);
    TrackerEvent inDeletedTrackedEntity = event(enrollment(trackedEntity(true), false), false);

    // the summary counts events while the details group them by program
    checkDataIntegritySummary(CHECK, 2, 50, true);
    checkDataIntegrityDetailsIssues(
        CHECK, program.getUid(), program.getName(), "2 event(s)", DETAILS_ID_TYPE);
    assertEquals(
        Set.of(inDeletedEnrollment.getUid(), inDeletedTrackedEntity.getUid()),
        Set.copyOf(getDetails(CHECK).getIssues().get(0).getRefs().toList(JsonString::string)));
  }

  @Test
  void shouldReportNoIssuesWhenListedEventsAreDeletedThroughTheTrackerApi() {
    event(enrollment(trackedEntity(false), false), false);
    TrackerEvent inDeletedEnrollment = event(enrollment(trackedEntity(false), true), false);
    TrackerEvent inDeletedTrackedEntity = event(enrollment(trackedEntity(true), false), false);

    POST(
            "/tracker?async=false&importStrategy=DELETE",
            """
            {"events": [{"event": "%s"}, {"event": "%s"}]}
            """
                .formatted(inDeletedEnrollment.getUid(), inDeletedTrackedEntity.getUid()))
        .content(HttpStatus.OK);

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  private TrackedEntity trackedEntity(boolean deleted) {
    TrackedEntity trackedEntity = createTrackedEntity(orgUnit, trackedEntityType);
    trackedEntity.setDeleted(deleted);
    manager.save(trackedEntity);
    return trackedEntity;
  }

  private Enrollment enrollment(TrackedEntity trackedEntity, boolean deleted) {
    Enrollment enrollment = createEnrollment(program, trackedEntity, orgUnit);
    enrollment.setDeleted(deleted);
    manager.save(enrollment);
    return enrollment;
  }

  private TrackerEvent event(Enrollment enrollment, boolean deleted) {
    TrackerEvent event = createEvent(programStage, enrollment, orgUnit);
    event.setDeleted(deleted);
    manager.save(event);
    entityManager.flush();
    return event;
  }
}
