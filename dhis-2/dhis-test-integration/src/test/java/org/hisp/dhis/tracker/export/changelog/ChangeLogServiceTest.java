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
package org.hisp.dhis.tracker.export.changelog;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ChangeLogServiceTest extends TrackerTest {

  @Autowired private ChangeLogService changeLogService;

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
        () -> changeLogService.getEventChangeLog(UID.of(CodeGenerator.generateUid())));
  }

  @Test
  void shouldFailWhenEventOrgUnitIsNotAccessible() {
    injectSecurityContextUser(manager.get(User.class, "o1HMTIzBGo7"));

    assertThrows(
        NotFoundException.class, () -> changeLogService.getEventChangeLog(UID.of("D9PbzJY8bJM")));
  }

  @Test
  void shouldFailWhenEventProgramIsNotAccessible() {
    injectSecurityContextUser(manager.get(User.class, "o1HMTIzBGo7"));

    assertThrows(
        NotFoundException.class, () -> changeLogService.getEventChangeLog(UID.of("G9PbzJY8bJG")));
  }

  @Test
  void shouldFailWhenProgramTrackedEntityTypeIsNotAccessible() {
    injectSecurityContextUser(manager.get(User.class, "FIgVWzUCkpw"));

    assertThrows(
        NotFoundException.class, () -> changeLogService.getEventChangeLog(UID.of("H0PbzJY8bJG")));
  }

  @Test
  void shouldFailWhenProgramWithoutRegistrationAndNoAccessToEventOrgUnit() {
    injectSecurityContextUser(manager.get(User.class, "o1HMTIzBGo7"));

    assertThrows(
        NotFoundException.class, () -> changeLogService.getEventChangeLog(UID.of("G9PbzJY8bJG")));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsCreated() throws NotFoundException {
    injectSecurityContextUser(manager.get(User.class, "M5zQapPyTZI"));

    Event event = manager.get(Event.class, "QRYjLTiJTrA");
    assertNotNull(event);
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();
    DataElement dataElement = manager.get(DataElement.class, dataElementUid);
    assertNotNull(dataElement);

    List<EventChangeLog> changeLogs = changeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"));

    assertAll(
        () -> {
          validateNonValueElements(1, changeLogs, importUser, dataElementUid);
          assertEquals("15", changeLogs.get(0).change().dataValue().currentValue());
          assertNull(changeLogs.get(0).change().dataValue().previousValue());
        });
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsDeleted() throws NotFoundException, IOException {
    injectSecurityContextUser(manager.get(User.class, "M5zQapPyTZI"));

    Event event = manager.get(Event.class, "QRYjLTiJTrA");
    assertNotNull(event);
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();
    DataElement dataElement = manager.get(DataElement.class, dataElementUid);
    assertNotNull(dataElement);

    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    List<EventChangeLog> changeLogs = changeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"));

    assertAll(
        () -> {
          validateNonValueElements(2, changeLogs, importUser, dataElementUid);
          assertNull(changeLogs.get(0).change().dataValue().currentValue());
          assertEquals("15", changeLogs.get(0).change().dataValue().previousValue());
          assertEquals("15", changeLogs.get(1).change().dataValue().currentValue());
          assertNull(changeLogs.get(1).change().dataValue().previousValue());
        });
  }

  @Test
  void shouldNotUpdateChangeLogsWhenDataValueIsDeletedTwiceInARow() throws NotFoundException, IOException {
    injectSecurityContextUser(manager.get(User.class, "M5zQapPyTZI"));

    Event event = manager.get(Event.class, "QRYjLTiJTrA");
    assertNotNull(event);
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();
    DataElement dataElement = manager.get(DataElement.class, dataElementUid);
    assertNotNull(dataElement);

    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    List<EventChangeLog> changeLogs = changeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"));

    assertAll(
        () -> {
          validateNonValueElements(2, changeLogs, importUser, dataElementUid);
          assertNull(changeLogs.get(0).change().dataValue().currentValue());
          assertEquals("15", changeLogs.get(0).change().dataValue().previousValue());
          assertEquals("15", changeLogs.get(1).change().dataValue().currentValue());
          assertNull(changeLogs.get(1).change().dataValue().previousValue());
        });
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsUpdated() throws NotFoundException, IOException {
    injectSecurityContextUser(manager.get(User.class, "M5zQapPyTZI"));

    Event event = manager.get(Event.class, "QRYjLTiJTrA");
    assertNotNull(event);
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();
    DataElement dataElement = manager.get(DataElement.class, dataElementUid);
    assertNotNull(dataElement);

    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "20");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    List<EventChangeLog> changeLogs = changeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"));

    assertAll(
        () -> {
          validateNonValueElements(2, changeLogs, importUser, dataElementUid);
          assertEquals("20", changeLogs.get(0).change().dataValue().currentValue());
          assertEquals("15", changeLogs.get(0).change().dataValue().previousValue());
          assertEquals("15", changeLogs.get(1).change().dataValue().currentValue());
          assertNull(changeLogs.get(1).change().dataValue().previousValue());
        });
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsUpdatedTwiceInARow() throws NotFoundException, IOException {
    injectSecurityContextUser(manager.get(User.class, "M5zQapPyTZI"));

    Event event = manager.get(Event.class, "QRYjLTiJTrA");
    assertNotNull(event);
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();
    DataElement dataElement = manager.get(DataElement.class, dataElementUid);
    assertNotNull(dataElement);

    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "20");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "25");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    List<EventChangeLog> changeLogs = changeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"));

    assertAll(
        () -> {
          validateNonValueElements(3, changeLogs, importUser, dataElementUid);
          assertEquals("25", changeLogs.get(0).change().dataValue().currentValue());
          assertEquals("20", changeLogs.get(0).change().dataValue().previousValue());
          assertEquals("20", changeLogs.get(1).change().dataValue().currentValue());
          assertEquals("15", changeLogs.get(1).change().dataValue().previousValue());
          assertEquals("15", changeLogs.get(2).change().dataValue().currentValue());
          assertNull(changeLogs.get(2).change().dataValue().previousValue());
        });
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsCreatedUpdatedAndDeleted()
      throws IOException, NotFoundException {
    injectSecurityContextUser(manager.get(User.class, "M5zQapPyTZI"));

    Event event = manager.get(Event.class, "QRYjLTiJTrA");
    assertNotNull(event);
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();
    DataElement dataElement = manager.get(DataElement.class, dataElementUid);
    assertNotNull(dataElement);

    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "20");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    updateDataValue(trackerObjects, event.getUid(), dataElementUid, "");
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    List<EventChangeLog> changeLogs = changeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"));

    assertAll(
        () -> {
          validateNonValueElements(3, changeLogs, importUser, dataElementUid);
          assertNull(changeLogs.get(0).change().dataValue().currentValue());
          assertEquals("20", changeLogs.get(0).change().dataValue().previousValue());
          assertEquals("20", changeLogs.get(1).change().dataValue().currentValue());
          assertEquals("15", changeLogs.get(1).change().dataValue().previousValue());
          assertEquals("15", changeLogs.get(2).change().dataValue().currentValue());
          assertNull(changeLogs.get(2).change().dataValue().previousValue());
        });
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

  private void validateNonValueElements(
      int expectedListSize,
      List<EventChangeLog> changeLogs,
      User importUser,
      String dataElementUid) {
    assertEquals(expectedListSize, changeLogs.size());

    for (EventChangeLog eventChangeLog : changeLogs) {
      assertEquals(importUser.getUsername(), eventChangeLog.updatedBy().getUsername());
      assertEquals(importUser.getFirstName(), eventChangeLog.updatedBy().getFirstName());
      assertEquals(importUser.getSurname(), eventChangeLog.updatedBy().getSurname());
      assertEquals(importUser.getUid(), eventChangeLog.updatedBy().getUid());
      assertEquals(dataElementUid, eventChangeLog.change().dataValue().dataElement());
    }
  }
}
