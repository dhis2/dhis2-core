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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.tracker.Assertions.assertHasError;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.model.Relationship;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RelationshipImportTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private User userA;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    userA = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(userA);

    testSetup.importTrackerData("tracker/one_te.json");
    testSetup.importTrackerData("tracker/one_enrollment.json");
    testSetup.importTrackerData("tracker/one_tracker_event.json");
    manager.flush();
  }

  @BeforeEach
  void setUpUser() {
    injectSecurityContextUser(userA);
  }

  @Test
  void successImportingRelationships() throws IOException {
    injectSecurityContextUser(userService.getUser("M5zQapPyTZI"));
    TrackerImportParams params = TrackerImportParams.builder().build();

    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/relationships.json"));

    assertThat(importReport.getStatus(), is(Status.OK));
    assertThat(importReport.getStats().getCreated(), is(2));
  }

  @Test
  void shouldPersistRelationshipFieldsAndItemWiringWhenRelationshipsAreCreated()
      throws IOException {
    injectSecurityContextUser(userService.getUser("M5zQapPyTZI"));

    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().build(),
            testSetup.fromJson("tracker/relationships.json")));
    manager.flush();
    manager.clear();

    // Tracked entity -> enrollment relationship
    Relationship teToEnrollment = manager.get(Relationship.class, "Nva3Xj2j75W");
    assertAll(
        "tracked entity to enrollment relationship not persisted correctly",
        () -> assertNotNull(teToEnrollment, "relationship was not persisted"),
        () -> assertFalse(teToEnrollment.isDeleted()),
        () -> assertNotNull(teToEnrollment.getKey(), "relationship key was not persisted"),
        () -> assertEquals("xLmPUYJX8Ks", teToEnrollment.getRelationshipType().getUid()),
        () -> assertNotNull(teToEnrollment.getFrom(), "from relationship item was not wired"),
        () ->
            assertEquals(
                "IOR1AXXl24H",
                teToEnrollment.getFrom().getTrackedEntity().getUid(),
                "from item should point to the tracked entity"),
        () -> assertNotNull(teToEnrollment.getTo(), "to relationship item was not wired"),
        () ->
            assertEquals(
                "TvctPPhpD8u",
                teToEnrollment.getTo().getEnrollment().getUid(),
                "to item should point to the enrollment"));

    // Tracked entity -> event relationship
    Relationship teToEvent = manager.get(Relationship.class, "HiXiipNGsxT");
    assertAll(
        "tracked entity to event relationship not persisted correctly",
        () -> assertNotNull(teToEvent, "relationship was not persisted"),
        () -> assertFalse(teToEvent.isDeleted()),
        () -> assertEquals("TV9oB9LT3sh", teToEvent.getRelationshipType().getUid()),
        () ->
            assertEquals(
                "IOR1AXXl24H",
                teToEvent.getFrom().getTrackedEntity().getUid(),
                "from item should point to the tracked entity"),
        () ->
            assertEquals(
                "D9PbzJY8bJO",
                teToEvent.getTo().getTrackerEvent().getUid(),
                "to item should point to the tracker event"));
  }

  @Test
  void shouldFailWhenUserNotAuthorizedToCreateRelationship() throws IOException {
    injectSecurityContextUser(userService.getUser("o1HMTIzBGo7"));
    TrackerImportParams params = TrackerImportParams.builder().build();

    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/relationships.json"));

    assertHasError(importReport, ValidationCode.E4020);
  }

  @Test
  void successUpdateRelationships() throws IOException {
    testSetup.importTrackerData("tracker/relationships.json");

    TrackerObjects trackerObjects = testSetup.fromJson("tracker/relationshipToUpdate.json");
    TrackerImportParams trackerImportParams =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
            .build();
    ImportReport importReport =
        trackerImportService.importTracker(trackerImportParams, trackerObjects);

    assertThat(importReport.getStatus(), is(Status.OK));
    assertThat(importReport.getStats().getCreated(), is(0));
    assertThat(importReport.getStats().getIgnored(), is(1));
  }

  @Test
  void shouldFailWhenTryingToUpdateADeletedRelationship() throws IOException {
    TrackerObjects trackerObjects = testSetup.importTrackerData("tracker/relationships.json");

    manager.delete(manager.get(Relationship.class, "Nva3Xj2j75W"));

    TrackerImportParams trackerImportParams =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
            .build();
    ImportReport importReport =
        trackerImportService.importTracker(trackerImportParams, trackerObjects);

    assertHasError(importReport, ValidationCode.E4017);
    assertThat(importReport.getStats().getIgnored(), is(2));
  }

  @Test
  void shouldRejectDuplicateRelationships() throws IOException {
    injectSecurityContextUser(userService.getUser("M5zQapPyTZI"));
    TrackerImportParams params = TrackerImportParams.builder().build();

    ImportReport first =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/relationships.json"));
    assertThat(first.getStatus(), is(Status.OK));
    assertThat(first.getStats().getCreated(), is(2));

    ImportReport second =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/relationships_duplicate.json"));

    assertHasError(second, ValidationCode.E4018);
    assertThat(second.getStats().getIgnored(), is(2));
    assertThat(second.getStats().getCreated(), is(0));
  }
}
