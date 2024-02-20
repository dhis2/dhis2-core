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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.export.event.EventChangeLog.DataValueChange;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventChangeLogServiceTest extends TrackerTest {

  @Autowired private EventChangeLogService eventChangeLogService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired protected UserService _userService;

  private User importUser;

  private TrackerImportParams importParams;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/simple_metadata.json");
    importUser = userService.getUser("M5zQapPyTZI");
    importParams = TrackerImportParams.builder().userId(importUser.getUid()).build();
    assertNoErrors(
        trackerImportService.importTracker(
            importParams, fromJson("tracker/event_and_enrollment.json")));
  }

  @Test
  void shouldFailWhenEventDoesNotExist() {
    assertThrows(
        NotFoundException.class,
        () -> eventChangeLogService.getEventChangeLog(UID.of(CodeGenerator.generateUid())));
  }

  @Test
  void shouldFailWhenEventOrgUnitIsNotAccessible() {
    testAsUser("o1HMTIzBGo7");

    assertThrows(
        NotFoundException.class,
        () -> eventChangeLogService.getEventChangeLog(UID.of("D9PbzJY8bJM")));
  }

  @Test
  void shouldFailWhenEventProgramIsNotAccessible() {
    testAsUser("o1HMTIzBGo7");

    assertThrows(
        NotFoundException.class,
        () -> eventChangeLogService.getEventChangeLog(UID.of("G9PbzJY8bJG")));
  }

  @Test
  void shouldFailWhenProgramTrackedEntityTypeIsNotAccessible() {
    testAsUser("FIgVWzUCkpw");

    assertThrows(
        NotFoundException.class,
        () -> eventChangeLogService.getEventChangeLog(UID.of("H0PbzJY8bJG")));
  }

  @Test
  void shouldFailWhenProgramWithoutRegistrationAndNoAccessToEventOrgUnit() {
    testAsUser("o1HMTIzBGo7");

    assertThrows(
        NotFoundException.class,
        () -> eventChangeLogService.getEventChangeLog(UID.of("G9PbzJY8bJG")));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsCreated() throws NotFoundException {
    testAsUser("M5zQapPyTZI");
    Event event = getEvent("QRYjLTiJTrA");
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();

    List<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"));

    assertNumberOfChanges(1, changeLogs);
    assertAll(
        () -> assertChange(dataElementUid, null, "15", changeLogs.get(0)),
        () -> assertUser(importUser, changeLogs.get(0)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsDeleted() throws NotFoundException, IOException {
    testAsUser("M5zQapPyTZI");

    Event event = getEvent("QRYjLTiJTrA");
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();

    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    List<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"));
    assertNumberOfChanges(2, changeLogs);
    assertAll(
        () -> assertChange(dataElementUid, "15", null, changeLogs.get(0)),
        () -> assertUser(importUser, changeLogs.get(0)));
    assertAll(
        () -> assertChange(dataElementUid, null, "15", changeLogs.get(1)),
        () -> assertUser(importUser, changeLogs.get(1)));
  }

  @Test
  void shouldNotUpdateChangeLogsWhenDataValueIsDeletedTwiceInARow()
      throws NotFoundException, IOException {
    testAsUser("M5zQapPyTZI");

    Event event = getEvent("QRYjLTiJTrA");
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();

    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    List<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"));
    assertNumberOfChanges(2, changeLogs);
    assertAll(
        () -> assertChange(dataElementUid, "15", null, changeLogs.get(0)),
        () -> assertUser(importUser, changeLogs.get(0)));
    assertAll(
        () -> assertChange(dataElementUid, null, "15", changeLogs.get(1)),
        () -> assertUser(importUser, changeLogs.get(1)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsUpdated() throws NotFoundException, IOException {
    testAsUser("M5zQapPyTZI");

    Event event = getEvent("QRYjLTiJTrA");
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();

    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "20");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    List<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"));
    assertNumberOfChanges(2, changeLogs);
    assertAll(
        () -> assertChange(dataElementUid, "15", "20", changeLogs.get(0)),
        () -> assertUser(importUser, changeLogs.get(0)));
    assertAll(
        () -> assertChange(dataElementUid, null, "15", changeLogs.get(1)),
        () -> assertUser(importUser, changeLogs.get(1)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsUpdatedTwiceInARow()
      throws NotFoundException, IOException {
    testAsUser("M5zQapPyTZI");

    Event event = getEvent("QRYjLTiJTrA");
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();

    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "20");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "25");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    List<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"));
    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertChange(dataElementUid, "20", "25", changeLogs.get(0)),
        () -> assertUser(importUser, changeLogs.get(0)));
    assertAll(
        () -> assertChange(dataElementUid, "15", "20", changeLogs.get(1)),
        () -> assertUser(importUser, changeLogs.get(1)));
    assertAll(
        () -> assertChange(dataElementUid, null, "15", changeLogs.get(2)),
        () -> assertUser(importUser, changeLogs.get(2)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsCreatedUpdatedAndDeleted()
      throws IOException, NotFoundException {
    testAsUser("M5zQapPyTZI");

    Event event = getEvent("QRYjLTiJTrA");
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();

    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "20");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    List<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"));
    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertChange(dataElementUid, "20", null, changeLogs.get(0)),
        () -> assertUser(importUser, changeLogs.get(0)));
    assertAll(
        () -> assertChange(dataElementUid, "15", "20", changeLogs.get(1)),
        () -> assertUser(importUser, changeLogs.get(1)));
    assertAll(
        () -> assertChange(dataElementUid, null, "15", changeLogs.get(2)),
        () -> assertUser(importUser, changeLogs.get(2)));
  }

  private void updateDataValue(
      TrackerObjects trackerObjects, String eventUid, String dataElementUid, String newValue) {
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().equalsIgnoreCase(eventUid))
        .findFirst()
        .flatMap(
            e ->
                e.getDataValues().stream()
                    .filter(
                        dv -> dv.getDataElement().getIdentifier().equalsIgnoreCase(dataElementUid))
                    .findFirst())
        .ifPresent(dv -> dv.setValue(newValue));
  }

  private Event getEvent(String uid) {
    Event event = manager.get(Event.class, uid);
    assertNotNull(event);

    return event;
  }

  private void testAsUser(String user) {
    injectSecurityContextUser(manager.get(User.class, user));
  }

  private static void assertNumberOfChanges(int expected, List<EventChangeLog> changeLogs) {
    assertNotNull(changeLogs);
    assertEquals(
        expected,
        changeLogs.size(),
        String.format(
            "Expected to find %s elements in the change log list, found %s instead: %s",
            expected, changeLogs.size(), changeLogs));
  }

  private static void assertChange(
      String dataElement, String previousValue, String currentValue, EventChangeLog changeLog) {
    DataValueChange expected = new DataValueChange(dataElement, previousValue, currentValue);
    assertEquals(expected, changeLog.change().dataValue());
  }

  private static void assertUser(User user, EventChangeLog changeLog) {
    assertAll(
        () -> assertEquals(user.getUsername(), changeLog.createdBy().getUsername()),
        () -> assertEquals(user.getFirstName(), changeLog.createdBy().getFirstName()),
        () -> assertEquals(user.getSurname(), changeLog.createdBy().getSurname()),
        () -> assertEquals(user.getUid(), changeLog.createdBy().getUid()));
  }
}
