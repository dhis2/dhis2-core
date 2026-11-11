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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.function.Consumer;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.utils.RelationshipUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for relationships that are not deleted but link to deleted tracker data {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/tracker_data/relationships_not_deleted_of_deleted_tracker_data.yaml}
 */
class DataIntegrityRelationshipsNotDeletedOfDeletedTrackerDataControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private static final String CHECK = "relationships_not_deleted_of_deleted_tracker_data";

  private static final String DETAILS_ID_TYPE = "relationshipTypes";

  private OrganisationUnit orgUnit;

  private TrackedEntityType trackedEntityType;

  private Program program;

  private ProgramStage programStage;

  private Program singleEventProgram;

  private ProgramStage singleEventProgramStage;

  private RelationshipType relationshipType;

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

    singleEventProgram = createProgramWithoutRegistration('B');
    manager.save(singleEventProgram, false);
    singleEventProgramStage = createProgramStage('B', singleEventProgram);
    manager.save(singleEventProgramStage, false);

    relationshipType = createRelationshipType('A');
    manager.save(relationshipType, false);
  }

  @Test
  void shouldReportNoIssuesWhenThereIsNoTrackerData() {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void shouldReportNoIssuesWhenRelationshipsMatchTheTrackerDataTheyLinkTo() {
    TrackedEntity trackedEntity = trackedEntity(false);
    relationship(false, trackedEntity, i -> i.setTrackedEntity(trackedEntity(false)));
    relationship(true, trackedEntity, i -> i.setTrackedEntity(trackedEntity(true)));
    relationship(true, trackedEntity, i -> i.setEnrollment(enrollment(trackedEntity(false), true)));

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void shouldReportRelationshipTypeWhenRelationshipLinksToDeletedTrackerData() {
    TrackedEntity trackedEntity = trackedEntity(false);
    relationship(false, trackedEntity, i -> i.setTrackedEntity(trackedEntity(false)));
    Set<String> orphans =
        Set.of(
            relationship(false, trackedEntity, i -> i.setTrackedEntity(trackedEntity(true)))
                .getUid(),
            relationship(
                    false,
                    trackedEntity,
                    i -> i.setEnrollment(enrollment(trackedEntity(false), true)))
                .getUid(),
            relationship(
                    false,
                    trackedEntity,
                    i -> i.setEvent(event(enrollment(trackedEntity(false), false), true)))
                .getUid(),
            relationship(false, trackedEntity, i -> i.setEvent(singleEvent(true))).getUid());

    assertOrphans(orphans, 80);
  }

  @Test
  void shouldReportRelationshipTypeWhenRelationshipLinksToDataUnderADeletedParent() {
    TrackedEntity trackedEntity = trackedEntity(false);
    relationship(false, trackedEntity, i -> i.setTrackedEntity(trackedEntity(false)));
    Set<String> orphans =
        Set.of(
            relationship(
                    false,
                    trackedEntity,
                    i -> i.setEnrollment(enrollment(trackedEntity(true), false)))
                .getUid(),
            relationship(
                    false,
                    trackedEntity,
                    i -> i.setEvent(event(enrollment(trackedEntity(false), true), false)))
                .getUid(),
            relationship(
                    false,
                    trackedEntity,
                    i -> i.setEvent(event(enrollment(trackedEntity(true), false), false)))
                .getUid());

    assertOrphans(orphans, 75);
  }

  @Test
  void shouldReportNoIssuesWhenListedRelationshipsAreDeletedThroughTheTrackerApi() {
    TrackedEntity trackedEntity = trackedEntity(false);
    relationship(false, trackedEntity, i -> i.setTrackedEntity(trackedEntity(false)));
    Relationship toDeletedTrackedEntity =
        relationship(false, trackedEntity, i -> i.setTrackedEntity(trackedEntity(true)));
    Relationship toEnrollmentOfDeletedTrackedEntity =
        relationship(
            false, trackedEntity, i -> i.setEnrollment(enrollment(trackedEntity(true), false)));
    Relationship toEventInDeletedEnrollment =
        relationship(
            false,
            trackedEntity,
            i -> i.setEvent(event(enrollment(trackedEntity(false), true), false)));
    Relationship toDeletedSingleEvent =
        relationship(false, trackedEntity, i -> i.setEvent(singleEvent(true)));

    POST(
            "/tracker?async=false&importStrategy=DELETE",
            """
            {"relationships": [
              {"relationship": "%s"},
              {"relationship": "%s"},
              {"relationship": "%s"},
              {"relationship": "%s"}
            ]}
            """
                .formatted(
                    toDeletedTrackedEntity.getUid(),
                    toEnrollmentOfDeletedTrackedEntity.getUid(),
                    toEventInDeletedEnrollment.getUid(),
                    toDeletedSingleEvent.getUid()))
        .content(HttpStatus.OK);

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  private void assertOrphans(Set<String> orphans, int percentage) {
    // the summary counts relationships while the details group them by relationship type
    checkDataIntegritySummary(CHECK, orphans.size(), percentage, true);
    checkDataIntegrityDetailsIssues(
        CHECK,
        relationshipType.getUid(),
        relationshipType.getName(),
        orphans.size() + " relationship(s)",
        DETAILS_ID_TYPE);
    assertEquals(
        orphans,
        Set.copyOf(getDetails(CHECK).getIssues().get(0).getRefs().toList(JsonString::string)));
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

  private Event event(Enrollment enrollment, boolean deleted) {
    Event event = createEvent(programStage, enrollment, orgUnit);
    event.setDeleted(deleted);
    manager.save(event);
    return event;
  }

  private Event singleEvent(boolean deleted) {
    // events in programs without registration belong to an enrollment without tracked entity
    Enrollment enrollment = createEnrollment(singleEventProgram, null, orgUnit);
    manager.save(enrollment);
    Event event = createEvent(singleEventProgramStage, enrollment, orgUnit);
    event.setDeleted(deleted);
    manager.save(event);
    return event;
  }

  private Relationship relationship(
      boolean deleted, TrackedEntity from, Consumer<RelationshipItem> toSetter) {
    Relationship relationship = new Relationship();
    RelationshipItem fromItem = new RelationshipItem();
    fromItem.setTrackedEntity(from);
    fromItem.setRelationship(relationship);
    RelationshipItem toItem = new RelationshipItem();
    toSetter.accept(toItem);
    toItem.setRelationship(relationship);

    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(fromItem);
    relationship.setTo(toItem);
    relationship.setKey(RelationshipUtils.generateRelationshipKey(relationship));
    relationship.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationship));
    relationship.setAutoFields();
    relationship.setDeleted(deleted);
    manager.save(relationship);
    entityManager.flush();
    return relationship;
  }
}
