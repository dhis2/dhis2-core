/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.export.singleevent;

import static org.hisp.dhis.changelog.ChangeLogType.UPDATE;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.export.event.EventChangeLog;
import org.hisp.dhis.tracker.export.event.EventChangeLogOperationParams;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.tracker.imports.domain.SingleEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SingleEventChangeLogServiceTest extends PostgresIntegrationTestBase {

  @Autowired private SingleEventChangeLogService singleEventChangeLogService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private TrackerObjectDeletionService trackerObjectDeletionService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  private final EventChangeLogOperationParams defaultOperationParams =
      EventChangeLogOperationParams.builder().build();
  private final PageParams defaultPageParams;

  private final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private TrackerObjects trackerObjects;
  @Autowired private TestSetup testSetup;

  SingleEventChangeLogServiceTest() throws BadRequestException {
    defaultPageParams = PageParams.of(1, 10, false);
  }

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    trackerObjects = testSetup.importTrackerData();
  }

  @BeforeEach
  void resetSecurityContext() {
    injectSecurityContextUser(importUser);
  }

  @Test
  void shouldFailWhenEventDoesNotExist() {
    assertThrows(
        NotFoundException.class,
        () -> singleEventChangeLogService.getEventChangeLog(UID.generate(), null, null));
  }

  @Test
  void shouldFailWhenEventIsSoftDeleted() throws NotFoundException {
    trackerObjectDeletionService.deleteSingleEvents(List.of(UID.of("D9PbzJY8bJM")));

    assertThrows(
        NotFoundException.class,
        () -> singleEventChangeLogService.getEventChangeLog(UID.generate(), null, null));
  }

  @Test
  void shouldFailWhenEventOrgUnitIsNotAccessible() {
    testAsUser("o1HMTIzBGo7");

    assertThrows(
        NotFoundException.class,
        () -> singleEventChangeLogService.getEventChangeLog(UID.of("D9PbzJY8bJM"), null, null));
  }

  @Test
  void shouldFailWhenEventProgramIsNotAccessible() {
    testAsUser("o1HMTIzBGo7");

    assertThrows(
        NotFoundException.class,
        () -> singleEventChangeLogService.getEventChangeLog(UID.of("G9PbzJY8bJG"), null, null));
  }

  @Test
  void shouldFailWithNotFoundWhenTryingToGetChangeLogsForTrackerEvent() {
    assertThrows(
        NotFoundException.class,
        () -> singleEventChangeLogService.getEventChangeLog(UID.of("H0PbzJY8bJG"), null, null));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsCreated() throws NotFoundException {
    String event = "OTmjvJDn0Fu";
    String dataElement = getDataElement(event);

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            singleEventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(1, changeLogs);
    assertDataElementCreate(dataElement, "13", changeLogs.get(0));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsDeleted() throws NotFoundException {
    String event = "OTmjvJDn0Fu";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            singleEventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(2, changeLogs);
    assertAll(
        () -> assertDataElementDelete(dataElement, "13", changeLogs.get(0)),
        () -> assertDataElementCreate(dataElement, "13", changeLogs.get(1)));
  }

  @Test
  void shouldNotUpdateChangeLogsWhenDataValueIsDeletedTwiceInARow() throws NotFoundException {
    String event = "OTmjvJDn0Fu";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "");
    updateDataValue(event, dataElement, "");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            singleEventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(2, changeLogs);
    assertAll(
        () -> assertDataElementDelete(dataElement, "13", changeLogs.get(0)),
        () -> assertDataElementCreate(dataElement, "13", changeLogs.get(1)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsUpdated() throws NotFoundException {
    String event = "OTmjvJDn0Fu";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "20");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            singleEventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(2, changeLogs);
    assertAll(
        () -> assertDataElementUpdate(dataElement, "13", "20", changeLogs.get(0)),
        () -> assertDataElementCreate(dataElement, "13", changeLogs.get(1)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsUpdatedTwiceInARow() throws NotFoundException {
    String event = "OTmjvJDn0Fu";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "20");
    updateDataValue(event, dataElement, "25");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            singleEventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertDataElementUpdate(dataElement, "20", "25", changeLogs.get(0)),
        () -> assertDataElementUpdate(dataElement, "13", "20", changeLogs.get(1)),
        () -> assertDataElementCreate(dataElement, "13", changeLogs.get(2)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsCreatedUpdatedAndDeleted() throws NotFoundException {
    String event = "OTmjvJDn0Fu";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "20");
    updateDataValue(event, dataElement, "");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            singleEventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertDataElementDelete(dataElement, "20", changeLogs.get(0)),
        () -> assertDataElementUpdate(dataElement, "13", "20", changeLogs.get(1)),
        () -> assertDataElementCreate(dataElement, "13", changeLogs.get(2)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsCreatedDeletedAndCreatedAgain()
      throws NotFoundException {
    String event = "OTmjvJDn0Fu";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "");
    updateDataValue(event, dataElement, "20");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            singleEventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertDataElementCreate(dataElement, "20", changeLogs.get(0)),
        () -> assertDataElementDelete(dataElement, "13", changeLogs.get(1)),
        () -> assertDataElementCreate(dataElement, "13", changeLogs.get(2)));
  }

  @Test
  void shouldReturnOnlyUserNameWhenUserDoesNotExistInDatabase() throws NotFoundException {
    Event event = getEvent("OTmjvJDn0Fu");
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();
    DataElement dataElement = manager.get(DataElement.class, dataElementUid);
    User deletedUser = new User();
    deletedUser.setUsername("deletedUserName");
    singleEventChangeLogService.addEventChangeLog(
        event, dataElement, "previous", "current", UPDATE, deletedUser.getUsername());

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            singleEventChangeLogService.getEventChangeLog(
                UID.of("OTmjvJDn0Fu"), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(2, changeLogs);
    assertAll(
        () ->
            assertUpdate(
                dataElementUid, null, "previous", "current", changeLogs.get(0), deletedUser),
        () -> assertDataElementCreate(dataElementUid, "13", changeLogs.get(1)));
  }

  @Test
  void shouldReturnEventFieldChangeLogWhenNewDateFieldValueAdded() throws NotFoundException {
    String event = "OTmjvJDn0Fu";

    Page<EventChangeLog> changeLogs =
        singleEventChangeLogService.getEventChangeLog(
            UID.of(event), defaultOperationParams, defaultPageParams);
    List<EventChangeLog> occurredAtLogs = getChangeLogsByField(changeLogs, "occurredAt");

    assertNumberOfChanges(1, occurredAtLogs);
    assertAll(
        () -> assertFieldCreate("occurredAt", "2022-04-23 06:00:38.343", occurredAtLogs.get(0)));
  }

  @Test
  void shouldReturnEventFieldChangeLogWhenExistingDateFieldUpdated()
      throws IOException, NotFoundException {
    UID event = UID.of("OTmjvJDn0Fu");
    LocalDateTime currentTime = LocalDateTime.now();

    updateEventDates(event, currentTime.toDate().toInstant());

    Page<EventChangeLog> changeLogs =
        singleEventChangeLogService.getEventChangeLog(
            event, defaultOperationParams, defaultPageParams);
    List<EventChangeLog> occurredAtLogs = getChangeLogsByField(changeLogs, "occurredAt");

    assertNumberOfChanges(2, occurredAtLogs);
    assertAll(
        () ->
            assertFieldUpdate(
                "occurredAt",
                "2022-04-23 06:00:38.343",
                currentTime.toString(formatter),
                occurredAtLogs.get(0)),
        () -> assertFieldCreate("occurredAt", "2022-04-23 06:00:38.343", occurredAtLogs.get(1)));
  }

  @Test
  void shouldReturnEventFieldChangeLogWhenNewGeometryPointFieldValueAdded()
      throws NotFoundException {
    String event = "OTmjvJDn0Fu";

    Page<EventChangeLog> changeLogs =
        singleEventChangeLogService.getEventChangeLog(
            UID.of(event), defaultOperationParams, defaultPageParams);
    List<EventChangeLog> geometryChangeLogs = getChangeLogsByField(changeLogs, "geometry");

    assertNumberOfChanges(1, geometryChangeLogs);
    assertAll(
        () -> assertFieldCreate("geometry", "(-11.419700, 8.103900)", geometryChangeLogs.get(0)));
  }

  @Test
  void shouldReturnEventFieldChangeLogWhenExistingGeometryPointFieldUpdated()
      throws NotFoundException {
    UID event = UID.of("OTmjvJDn0Fu");

    Geometry geometry = createGeometryPoint(16.435547, 49.26422);
    updateEventGeometry(event, geometry);

    Page<EventChangeLog> changeLogs =
        singleEventChangeLogService.getEventChangeLog(
            event, defaultOperationParams, defaultPageParams);
    List<EventChangeLog> geometryChangeLogs = getChangeLogsByField(changeLogs, "geometry");

    assertNumberOfChanges(2, geometryChangeLogs);
    assertAll(
        () ->
            assertFieldUpdate(
                "geometry",
                "(-11.419700, 8.103900)",
                "(16.435547, 49.264220)",
                geometryChangeLogs.get(0)),
        () -> assertFieldCreate("geometry", "(-11.419700, 8.103900)", geometryChangeLogs.get(1)));
  }

  @Test
  void shouldReturnEventFieldChangeLogWhenExistingGeometryPointFieldDeleted()
      throws NotFoundException {
    UID event = UID.of("OTmjvJDn0Fu");

    deleteEventGeometry(event);

    Page<EventChangeLog> changeLogs =
        singleEventChangeLogService.getEventChangeLog(
            event, defaultOperationParams, defaultPageParams);
    List<EventChangeLog> geometryChangeLogs = getChangeLogsByField(changeLogs, "geometry");

    assertNumberOfChanges(2, geometryChangeLogs);
    assertAll(
        () -> assertFieldDelete("geometry", "(-11.419700, 8.103900)", geometryChangeLogs.get(0)),
        () -> assertFieldCreate("geometry", "(-11.419700, 8.103900)", geometryChangeLogs.get(1)));
  }

  private void updateDataValue(String event, String dataElementUid, String newValue) {
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().getValue().equalsIgnoreCase(event))
        .findFirst()
        .ifPresent(
            e -> {
              e.getDataValues().stream()
                  .filter(
                      dv -> dv.getDataElement().getIdentifier().equalsIgnoreCase(dataElementUid))
                  .findFirst()
                  .ifPresent(dv -> dv.setValue(newValue));

              assertNoErrors(
                  trackerImportService.importTracker(
                      TrackerImportParams.builder().build(),
                      TrackerObjects.builder().events(List.of(e)).build()));
            });
  }

  private void updateEventDates(UID event, Instant newDate) throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/base_data.json");

    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().equals(event))
        .findFirst()
        .ifPresent(
            e -> {
              org.hisp.dhis.tracker.imports.domain.Event ev =
                  SingleEvent.builderFromEvent(e).occurredAt(newDate).build();

              assertNoErrors(
                  trackerImportService.importTracker(
                      TrackerImportParams.builder().build(),
                      TrackerObjects.builder().events(List.of(ev)).build()));
            });
  }

  private void updateEventGeometry(UID event, Geometry newGeometry) {
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().equals(event))
        .findFirst()
        .ifPresent(
            e -> {
              org.hisp.dhis.tracker.imports.domain.Event ev =
                  SingleEvent.builderFromEvent(e).geometry(newGeometry).build();

              assertNoErrors(
                  trackerImportService.importTracker(
                      TrackerImportParams.builder().build(),
                      TrackerObjects.builder().events(List.of(ev)).build()));
            });
  }

  private void deleteEventGeometry(UID event) {
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().equals(event))
        .findFirst()
        .ifPresent(
            e -> {
              org.hisp.dhis.tracker.imports.domain.Event ev =
                  SingleEvent.builderFromEvent(e).geometry(null).build();

              assertNoErrors(
                  trackerImportService.importTracker(
                      TrackerImportParams.builder().build(),
                      TrackerObjects.builder().events(List.of(ev)).build()));
            });
  }

  private String getDataElement(String uid) {
    Event event = getEvent(uid);
    String dataElement = event.getEventDataValues().iterator().next().getDataElement();
    assertNotNull(dataElement);
    return dataElement;
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
            "Expected to find %s elements in the event change log list, found %s instead: %s",
            expected, changeLogs.size(), changeLogs));
  }

  private void assertDataElementCreate(
      String dataElement, String currentValue, EventChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("CREATE", changeLog.changeLogType().name()),
        () -> assertDataElementChange(dataElement, null, currentValue, changeLog));
  }

  private void assertFieldCreate(String field, String currentValue, EventChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("CREATE", changeLog.changeLogType().name()),
        () -> assertFieldChange(field, null, currentValue, changeLog));
  }

  private void assertDataElementUpdate(
      String dataElement, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertUpdate(dataElement, null, previousValue, currentValue, changeLog, importUser);
  }

  private void assertFieldUpdate(
      String field, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertUpdate(null, field, previousValue, currentValue, changeLog, importUser);
  }

  private void assertUpdate(
      String dataElement,
      String field,
      String previousValue,
      String currentValue,
      EventChangeLog changeLog,
      User user) {
    assertAll(
        () -> assertUser(user, changeLog),
        () -> assertEquals("UPDATE", changeLog.changeLogType().name()),
        () -> {
          if (dataElement != null) {
            assertDataElementChange(dataElement, previousValue, currentValue, changeLog);
          } else {
            assertFieldChange(field, previousValue, currentValue, changeLog);
          }
        });
  }

  private void assertDataElementDelete(
      String dataElement, String previousValue, EventChangeLog changeLog) {
    assertDelete(dataElement, null, previousValue, changeLog);
  }

  private void assertFieldDelete(String field, String previousValue, EventChangeLog changeLog) {
    assertDelete(null, field, previousValue, changeLog);
  }

  private void assertDelete(
      String dataElement, String field, String previousValue, EventChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("DELETE", changeLog.changeLogType().name()),
        () -> {
          if (dataElement != null) {
            assertDataElementChange(dataElement, previousValue, null, changeLog);
          } else {
            assertFieldChange(field, previousValue, null, changeLog);
          }
        });
  }

  private static void assertDataElementChange(
      String dataElement, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertEquals(
        dataElement, changeLog.dataElement() != null ? changeLog.dataElement().getUid() : null);
    assertEquals(previousValue, changeLog.previousValue());
    assertEquals(currentValue, changeLog.currentValue());
  }

  private static void assertFieldChange(
      String field, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertEquals(field, changeLog.eventField());
    assertEquals(previousValue, changeLog.previousValue());
    assertEquals(currentValue, changeLog.currentValue());
  }

  private static void assertUser(User user, EventChangeLog changeLog) {
    assertAll(
        () -> assertEquals(user.getUsername(), changeLog.createdBy().getUsername()),
        () ->
            assertEquals(
                user.getFirstName(),
                changeLog.createdBy() == null ? null : changeLog.createdBy().getFirstName()),
        () ->
            assertEquals(
                user.getSurname(),
                changeLog.createdBy() == null ? null : changeLog.createdBy().getSurname()),
        () ->
            assertEquals(
                user.getUid(),
                changeLog.createdBy() == null ? null : changeLog.createdBy().getUid()));
  }

  private List<EventChangeLog> getDataElementChangeLogs(Page<EventChangeLog> changeLogs) {
    return changeLogs.getItems().stream().filter(cl -> cl.dataElement() != null).toList();
  }

  private List<EventChangeLog> getChangeLogsByField(
      Page<EventChangeLog> changeLogs, String fieldName) {
    return changeLogs.getItems().stream()
        .filter(cl -> cl.eventField() != null && cl.eventField().equals(fieldName))
        .toList();
  }

  private Geometry createGeometryPoint(double x, double y) {
    GeometryFactory geometryFactory = new GeometryFactory();
    Coordinate coordinate = new Coordinate(x, y);

    return geometryFactory.createPoint(coordinate);
  }
}
