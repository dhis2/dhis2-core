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

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventImportTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private TrackerEventService trackerEventService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
  }

  @Test
  void shouldPopulateCompletedDataWhenCreatingAnEventWithStatusCompleted()
      throws IOException, ForbiddenException, NotFoundException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/te_enrollment_event.json");
    trackerObjects.getEvents().get(0).setStatus(EventStatus.COMPLETED);

    importTracker(params, trackerObjects);

    TrackerEvent event = trackerEventService.getEvent(trackerObjects.getEvents().get(0).getUid());

    assertEquals(importUser.getUsername(), event.getCompletedBy());
    assertNotNull(event.getCompletedDate());
  }

  @ParameterizedTest
  @MethodSource("notCompletedStatus")
  void shouldNotPopulateCompletedDataWhenCreatingAnEventWithNotCompletedStatus(EventStatus status)
      throws IOException, ForbiddenException, NotFoundException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/te_enrollment_event.json");
    trackerObjects.getEvents().get(0).setStatus(status);

    importTracker(params, trackerObjects);

    TrackerEvent event = trackerEventService.getEvent(trackerObjects.getEvents().get(0).getUid());

    assertNull(event.getCompletedBy());
    assertNull(event.getCompletedDate());
  }

  @Test
  void shouldDeleteCompletedDataWhenUpdatingAnEventWithStatusActive()
      throws IOException, NotFoundException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/te_enrollment_event.json");
    trackerObjects.getEvents().get(0).setStatus(EventStatus.COMPLETED);

    importTracker(params, trackerObjects);

    trackerObjects.getEvents().get(0).setStatus(EventStatus.ACTIVE);

    importTracker(params, trackerObjects);

    TrackerEvent event = trackerEventService.getEvent(trackerObjects.getEvents().get(0).getUid());

    assertNull(event.getCompletedBy());
    assertNull(event.getCompletedDate());
  }

  @ParameterizedTest
  @MethodSource("notCompletedStatus")
  void shouldPopulateCompletedDataWhenUpdatingAnEventWithStatusCompleted(EventStatus status)
      throws IOException, ForbiddenException, NotFoundException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/te_enrollment_event.json");
    trackerObjects.getEvents().get(0).setStatus(status);

    importTracker(params, trackerObjects);

    trackerObjects.getEvents().get(0).setStatus(EventStatus.COMPLETED);

    importTracker(params, trackerObjects);

    TrackerEvent event = trackerEventService.getEvent(trackerObjects.getEvents().get(0).getUid());

    assertEquals(importUser.getUsername(), event.getCompletedBy());
    assertNotNull(event.getCompletedDate());
  }

  private void importTracker(TrackerImportParams params, TrackerObjects trackerObjects) {
    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));
    manager.flush();
  }

  public Stream<Arguments> notCompletedStatus() {
    return Stream.of(
        Arguments.of(EventStatus.ACTIVE),
        Arguments.of(EventStatus.SCHEDULE),
        Arguments.of(EventStatus.OVERDUE),
        Arguments.of(EventStatus.SKIPPED));
  }
}
