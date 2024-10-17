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
package org.hisp.dhis.tracker.bundle;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.stream.Stream;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.ImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class EventImportTest extends TrackerTest {
  @Autowired private TrackerImportService trackerImportService;

  private User importUser;

  @Autowired protected UserService _userService;

  public void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/simple_metadata.json");

    importUser = userService.getUser("M5zQapPyTZI");
    injectSecurityContext(importUser);
  }

  @Test
  void shouldPopulateCompletedDataWhenCreatingAnEventWithStatusCompleted() throws IOException {
    TrackerImportParams params = fromJson("tracker/te_enrollment_event.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.getEvents().get(0).setStatus(EventStatus.COMPLETED);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertNoErrors(importReport);

    ProgramStageInstance event =
        manager.get(ProgramStageInstance.class, params.getEvents().get(0).getUid());

    assertEquals(importUser.getUsername(), event.getCompletedBy());
    assertNotNull(event.getCompletedDate());
  }

  @ParameterizedTest
  @MethodSource("notCompletedStatus")
  void shouldNotPopulateCompletedDataWhenCreatingAnEventWithNotCompletedStatus(EventStatus status)
      throws IOException {
    TrackerImportParams params = fromJson("tracker/te_enrollment_event.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.getEvents().get(0).setStatus(status);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertNoErrors(importReport);

    ProgramStageInstance event =
        manager.get(ProgramStageInstance.class, params.getEvents().get(0).getUid());

    assertNull(event.getCompletedBy());
    assertNull(event.getCompletedDate());
  }

  @Test
  void shouldDeleteCompletedDataWhenUpdatingAnEventWithStatusActive() throws IOException {
    TrackerImportParams params = fromJson("tracker/te_enrollment_event.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.getEvents().get(0).setStatus(EventStatus.COMPLETED);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertNoErrors(importReport);

    params.getEvents().get(0).setStatus(EventStatus.ACTIVE);

    importReport = trackerImportService.importTracker(params);

    assertNoErrors(importReport);

    ProgramStageInstance event =
        manager.get(ProgramStageInstance.class, params.getEvents().get(0).getUid());

    assertNull(event.getCompletedBy());
    assertNull(event.getCompletedDate());
  }

  @ParameterizedTest
  @MethodSource("notCompletedStatus")
  void shouldPopulateCompletedDataWhenUpdatingAnEventWithStatusCompleted(EventStatus status)
      throws IOException {
    TrackerImportParams params = fromJson("tracker/te_enrollment_event.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.getEvents().get(0).setStatus(status);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertNoErrors(importReport);

    params.getEvents().get(0).setStatus(EventStatus.COMPLETED);

    importReport = trackerImportService.importTracker(params);

    assertNoErrors(importReport);

    ProgramStageInstance event =
        manager.get(ProgramStageInstance.class, params.getEvents().get(0).getUid());

    assertEquals(importUser.getUsername(), event.getCompletedBy());
    assertNotNull(event.getCompletedDate());
  }

  public Stream<Arguments> notCompletedStatus() {
    return Stream.of(
        Arguments.of(EventStatus.ACTIVE),
        Arguments.of(EventStatus.SCHEDULE),
        Arguments.of(EventStatus.OVERDUE),
        Arguments.of(EventStatus.SKIPPED));
  }
}
