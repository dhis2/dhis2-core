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
package org.hisp.dhis.tracker;

import static org.hisp.dhis.test.e2e.actions.tracker.TrackerImportExportActions.EVENTS;
import static org.hisp.dhis.test.e2e.actions.tracker.TrackerImportExportActions.TRACKED_ENTITIES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.util.Map;
import org.hisp.dhis.helpers.TrackerCleanUp;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.TestRunStorage;
import org.hisp.dhis.test.e2e.dto.TrackerApiResponse;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

/** Tests the clean-up of tracker data created by e2e tests, see {@link TrackerCleanUp}. */
class TrackerCleanUpTest extends TrackerApiTest {
  private static final String NON_EXISTENT_UID = "Zz0Zz0Zz0Zz";

  @Test
  void shouldDeleteDependentTrackerDataWhenOnlyTrackedEntityIsPassed() throws Exception {
    TrackerApiResponse response =
        importTrackedEntitiesWithEnrollmentAndEvent(
            Constants.ORG_UNIT_IDS[0], TRACKER_PROGRAM_ID, TRACKER_PROGRAM_STAGE_ID);
    String trackedEntity = response.extractImportedTrackedEntities().get(0);
    String enrollment = response.extractImportedEnrollments().get(0);
    String event = response.extractImportedEvents().get(0);
    String otherTrackedEntity = importTrackedEntity();
    String trackedEntityRelationship =
        importRelationshipBetweenTrackedEntities(trackedEntity, otherTrackedEntity)
            .extractImportedRelationships()
            .get(0);
    String eventRelationship =
        importRelationshipEventToTrackedEntity(event, otherTrackedEntity)
            .extractImportedRelationships()
            .get(0);
    String singleEvent = importEvents().get(0);

    new TrackerCleanUp()
        .deleteTrackerData(Map.of(trackedEntity, TRACKED_ENTITIES, singleEvent, EVENTS));

    trackerImportExportActions.getTrackedEntity(trackedEntity).validateStatus(404);
    trackerImportExportActions.getEnrollment(enrollment).validateStatus(404);
    trackerImportExportActions.getEvent(event).validateStatus(404);
    trackerImportExportActions.getRelationship(trackedEntityRelationship).validateStatus(404);
    trackerImportExportActions.getRelationship(eventRelationship).validateStatus(404);
    trackerImportExportActions.getEvent(singleEvent).validateStatus(404);
    trackerImportExportActions.getTrackedEntity(otherTrackedEntity).validateStatus(200);
  }

  @Test
  void shouldRemoveDeletedTrackerDataFromStorageWhenDeleted() throws Exception {
    String trackedEntity = importTrackedEntity();

    new TrackerCleanUp().deleteTrackerData(Map.of(trackedEntity, TRACKED_ENTITIES));

    assertFalse(TestRunStorage.getCreatedEntities().containsKey(trackedEntity));
  }

  @Test
  void shouldNotFailWhenTrackerDataIsAlreadyDeletedOrDoesNotExist() throws Exception {
    String trackedEntity = importTrackedEntity();
    trackerImportExportActions
        .postAndGetJobReport(
            new JsonObjectBuilder()
                .addProperty("trackedEntity", trackedEntity)
                .wrapIntoArray("trackedEntities"),
            new QueryParamsBuilder().add("importStrategy", "DELETE"))
        .validateSuccessfulImport();

    assertDoesNotThrow(
        () ->
            new TrackerCleanUp()
                .deleteTrackerData(
                    Map.of(trackedEntity, TRACKED_ENTITIES, NON_EXISTENT_UID, EVENTS)));
  }

  @Test
  void shouldRegisterTrackerDataWhenImportedSynchronously() throws Exception {
    String trackedEntity =
        trackerImportExportActions
            .postAndGetJobReport(
                new FileReaderUtils()
                    .readJsonAndGenerateData(
                        new File(
                            "src/test/resources/tracker/importer/trackedEntities/trackedEntity.json")),
                new QueryParamsBuilder().add("async", "false"))
            .validateSuccessfulImport()
            .extractImportedTrackedEntities()
            .get(0);

    assertEquals(TRACKED_ENTITIES, TestRunStorage.getCreatedEntities().get(trackedEntity));
  }

  @Test
  void shouldNotRegisterTrackerDataWhenDeletedByImport() throws Exception {
    String trackedEntity = importTrackedEntity();
    TestRunStorage.removeEntity(TRACKED_ENTITIES, trackedEntity);

    trackerImportExportActions
        .postAndGetJobReport(
            new JsonObjectBuilder()
                .addProperty("trackedEntity", trackedEntity)
                .wrapIntoArray("trackedEntities"),
            new QueryParamsBuilder().add("importStrategy", "DELETE"))
        .validateSuccessfulImport();

    assertFalse(TestRunStorage.getCreatedEntities().containsKey(trackedEntity));
  }
}
