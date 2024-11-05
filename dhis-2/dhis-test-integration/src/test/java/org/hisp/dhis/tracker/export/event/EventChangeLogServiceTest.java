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

import static org.hisp.dhis.changelog.ChangeLogType.UPDATE;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.feedback.Assertions;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.export.event.EventChangeLog.DataValueChange;
import org.hisp.dhis.tracker.export.event.EventChangeLog.PropertyChange;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

class EventChangeLogServiceTest extends PostgresIntegrationTestBase {

  @Autowired private EventChangeLogService eventChangeLogService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private TrackerObjectDeletionService trackerObjectDeletionService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private RenderService renderService;

  private User importUser;

  private TrackerImportParams importParams;

  private final EventChangeLogOperationParams defaultOperationParams =
      EventChangeLogOperationParams.builder().build();
  private final PageParams defaultPageParams = new PageParams(null, null, false);

  private final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");

  @BeforeEach
  void setUp() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    importParams = TrackerImportParams.builder().build();
    assertNoErrors(
        trackerImportService.importTracker(
            importParams, fromJson("tracker/event_and_enrollment.json")));
  }

  @Test
  void shouldFailWhenEventDoesNotExist() {
    assertThrows(
        NotFoundException.class,
        () -> eventChangeLogService.getEventChangeLog(UID.generate(), null, null));
  }

  @Test
  void shouldFailWhenEventIsSoftDeleted() throws NotFoundException {
    trackerObjectDeletionService.deleteEvents(List.of(UID.of("D9PbzJY8bJM")));

    assertThrows(
        NotFoundException.class,
        () -> eventChangeLogService.getEventChangeLog(UID.generate(), null, null));
  }

  @Test
  void shouldFailWhenEventOrgUnitIsNotAccessible() {
    testAsUser("o1HMTIzBGo7");

    assertThrows(
        NotFoundException.class,
        () -> eventChangeLogService.getEventChangeLog(UID.of("D9PbzJY8bJM"), null, null));
  }

  @Test
  void shouldFailWhenEventProgramIsNotAccessible() {
    testAsUser("o1HMTIzBGo7");

    assertThrows(
        NotFoundException.class,
        () -> eventChangeLogService.getEventChangeLog(UID.of("G9PbzJY8bJG"), null, null));
  }

  @Test
  void shouldFailWhenProgramTrackedEntityTypeIsNotAccessible() {
    testAsUser("FIgVWzUCkpw");

    assertThrows(
        NotFoundException.class,
        () -> eventChangeLogService.getEventChangeLog(UID.of("H0PbzJY8bJG"), null, null));
  }

  @Test
  void shouldFailWhenProgramWithoutRegistrationAndNoAccessToEventOrgUnit() {
    testAsUser("o1HMTIzBGo7");

    assertThrows(
        NotFoundException.class,
        () -> eventChangeLogService.getEventChangeLog(UID.of("G9PbzJY8bJG"), null, null));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsCreated() throws NotFoundException, ForbiddenException {
    String event = "QRYjLTiJTrA";
    String dataElement = getDataElement(event);

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(1, changeLogs);
    assertDataElementCreate(dataElement, "15", changeLogs.get(0));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsDeleted()
      throws NotFoundException, IOException, ForbiddenException {
    String event = "QRYjLTiJTrA";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(2, changeLogs);
    assertAll(
        () -> assertDataElementDelete(dataElement, "15", changeLogs.get(0)),
        () -> assertDataElementCreate(dataElement, "15", changeLogs.get(1)));
  }

  @Test
  void shouldNotUpdateChangeLogsWhenDataValueIsDeletedTwiceInARow()
      throws NotFoundException, IOException, ForbiddenException {
    String event = "QRYjLTiJTrA";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "");
    updateDataValue(event, dataElement, "");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(2, changeLogs);
    assertAll(
        () -> assertDataElementDelete(dataElement, "15", changeLogs.get(0)),
        () -> assertDataElementCreate(dataElement, "15", changeLogs.get(1)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsUpdated()
      throws NotFoundException, IOException, ForbiddenException {
    String event = "QRYjLTiJTrA";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "20");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(2, changeLogs);
    assertAll(
        () -> assertDataElementUpdate(dataElement, "15", "20", changeLogs.get(0)),
        () -> assertDataElementCreate(dataElement, "15", changeLogs.get(1)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsUpdatedTwiceInARow()
      throws NotFoundException, IOException, ForbiddenException {
    String event = "QRYjLTiJTrA";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "20");
    updateDataValue(event, dataElement, "25");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertDataElementUpdate(dataElement, "20", "25", changeLogs.get(0)),
        () -> assertDataElementUpdate(dataElement, "15", "20", changeLogs.get(1)),
        () -> assertDataElementCreate(dataElement, "15", changeLogs.get(2)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsCreatedUpdatedAndDeleted()
      throws IOException, NotFoundException, ForbiddenException {
    String event = "QRYjLTiJTrA";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "20");
    updateDataValue(event, dataElement, "");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of(event), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertDataElementDelete(dataElement, "20", changeLogs.get(0)),
        () -> assertDataElementUpdate(dataElement, "15", "20", changeLogs.get(1)),
        () -> assertDataElementCreate(dataElement, "15", changeLogs.get(2)));
  }

  @Test
  void shouldReturnOnlyUserNameWhenUserDoesNotExistInDatabase()
      throws ForbiddenException, NotFoundException {
    Event event = getEvent("QRYjLTiJTrA");
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();
    DataElement dataElement = manager.get(DataElement.class, dataElementUid);
    User fakeUser = new User();
    fakeUser.setUsername("fakeUserName");
    eventChangeLogService.addEventChangeLog(
        event, dataElement, "", "current", "previous", UPDATE, fakeUser.getUsername());

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of("QRYjLTiJTrA"), defaultOperationParams, defaultPageParams));

    assertNumberOfChanges(2, changeLogs);
    assertAll(
        () ->
            assertUpdate(dataElementUid, null, "previous", "current", changeLogs.get(0), fakeUser),
        () -> assertDataElementCreate(dataElementUid, "15", changeLogs.get(1)));
  }

  @Test
  void shouldReturnEventPropertiesChangeLogWhenNewDatePropertyValueAdded()
      throws ForbiddenException, NotFoundException {
    String event = "QRYjLTiJTrA";

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(
            UID.of(event), defaultOperationParams, defaultPageParams);
    List<EventChangeLog> scheduledDateLogs = getChangeLogsByProperty(changeLogs, "scheduledDate");
    List<EventChangeLog> occurredDateLogs = getChangeLogsByProperty(changeLogs, "occurredDate");

    assertNumberOfChanges(1, scheduledDateLogs);
    assertNumberOfChanges(1, occurredDateLogs);
    assertAll(
        () ->
            assertPropertyCreate(
                "scheduledDate", "2022-04-22 06:00:38.343", scheduledDateLogs.get(0)),
        () ->
            assertPropertyCreate(
                "occurredDate", "2022-04-20 06:00:38.343", occurredDateLogs.get(0)));
  }

  @Test
  void shouldReturnEventPropertiesChangeLogWhenExistingDatePropertyUpdated()
      throws IOException, ForbiddenException, NotFoundException {
    UID event = UID.of("QRYjLTiJTrA");
    LocalDateTime currentTime = LocalDateTime.now();

    updateEventDates(event, currentTime.toDate().toInstant());

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(event, defaultOperationParams, defaultPageParams);
    List<EventChangeLog> scheduledDateLogs = getChangeLogsByProperty(changeLogs, "scheduledDate");
    List<EventChangeLog> occurredDateLogs = getChangeLogsByProperty(changeLogs, "occurredDate");

    assertNumberOfChanges(2, scheduledDateLogs);
    assertNumberOfChanges(2, occurredDateLogs);
    assertAll(
        () ->
            assertPropertyUpdate(
                "scheduledDate",
                "2022-04-22 06:00:38.343",
                currentTime.toString(formatter),
                scheduledDateLogs.get(0)),
        () ->
            assertPropertyCreate(
                "scheduledDate", "2022-04-22 06:00:38.343", scheduledDateLogs.get(1)),
        () ->
            assertPropertyUpdate(
                "occurredDate",
                "2022-04-20 06:00:38.343",
                currentTime.toString(formatter),
                occurredDateLogs.get(0)),
        () ->
            assertPropertyCreate(
                "occurredDate", "2022-04-20 06:00:38.343", occurredDateLogs.get(1)));
  }

  @Test
  void shouldReturnEventPropertiesChangeLogWhenExistingDatePropertyDeleted()
      throws IOException, ForbiddenException, NotFoundException {
    UID event = UID.of("QRYjLTiJTrA");

    deleteScheduledAtDate(event);

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(event, defaultOperationParams, defaultPageParams);
    List<EventChangeLog> scheduledDateLogs = getChangeLogsByProperty(changeLogs, "scheduledDate");
    List<EventChangeLog> occurredDateLogs = getChangeLogsByProperty(changeLogs, "occurredDate");

    assertNumberOfChanges(2, scheduledDateLogs);
    assertNumberOfChanges(1, occurredDateLogs);
    assertAll(
        () ->
            assertPropertyDelete(
                "scheduledDate", "2022-04-22 06:00:38.343", scheduledDateLogs.get(0)),
        () ->
            assertPropertyCreate(
                "scheduledDate", "2022-04-22 06:00:38.343", scheduledDateLogs.get(1)),
        () ->
            assertPropertyCreate(
                "occurredDate", "2022-04-20 06:00:38.343", occurredDateLogs.get(0)));
  }

  @Test
  void shouldReturnEventPropertiesChangeLogWhenNewGeometryPropertyValueAdded()
      throws ForbiddenException, NotFoundException {
    String event = "QRYjLTiJTrA";

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(
            UID.of(event), defaultOperationParams, defaultPageParams);
    List<EventChangeLog> geometryChangeLogs = getChangeLogsByProperty(changeLogs, "geometry");

    assertNumberOfChanges(1, geometryChangeLogs);
    assertAll(
        () ->
            assertPropertyCreate("geometry", "POINT (-11.4197 8.1039)", geometryChangeLogs.get(0)));
  }

  @Test
  void shouldReturnEventPropertiesChangeLogWhenExistingGeometryPropertyUpdated()
      throws ForbiddenException, NotFoundException, IOException {
    UID event = UID.of("QRYjLTiJTrA");

    Geometry geometry = createGeometryPoint(16.435547, 49.26422);
    updateEventGeometry(event, geometry);

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(event, defaultOperationParams, defaultPageParams);
    List<EventChangeLog> geometryChangeLogs = getChangeLogsByProperty(changeLogs, "geometry");

    assertNumberOfChanges(2, geometryChangeLogs);
    assertAll(
        () ->
            assertPropertyUpdate(
                "geometry",
                "POINT (-11.4197 8.1039)",
                geometry.toString(),
                geometryChangeLogs.get(0)),
        () ->
            assertPropertyCreate("geometry", "POINT (-11.4197 8.1039)", geometryChangeLogs.get(1)));
  }

  @Test
  void shouldReturnEventPropertiesChangeLogWhenExistingGeometryPropertyDeleted()
      throws IOException, ForbiddenException, NotFoundException {
    UID event = UID.of("QRYjLTiJTrA");

    deleteEventGeometry(event);

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(event, defaultOperationParams, defaultPageParams);
    List<EventChangeLog> geometryChangeLogs = getChangeLogsByProperty(changeLogs, "geometry");

    assertNumberOfChanges(2, geometryChangeLogs);
    assertAll(
        () ->
            assertPropertyDelete("geometry", "POINT (-11.4197 8.1039)", geometryChangeLogs.get(0)),
        () ->
            assertPropertyCreate("geometry", "POINT (-11.4197 8.1039)", geometryChangeLogs.get(1)));
  }

  private void updateDataValue(String event, String dataElementUid, String newValue)
      throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().getValue().equalsIgnoreCase(event))
        .findFirst()
        .flatMap(
            e ->
                e.getDataValues().stream()
                    .filter(
                        dv -> dv.getDataElement().getIdentifier().equalsIgnoreCase(dataElementUid))
                    .findFirst())
        .ifPresent(dv -> dv.setValue(newValue));
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
  }

  private void updateEventDates(UID event, Instant newDate) throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().equals(event))
        .findFirst()
        .ifPresent(
            e -> {
              e.setOccurredAt(newDate);
              e.setScheduledAt(newDate);
            });
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
  }

  private void deleteScheduledAtDate(UID event) throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().equals(event))
        .findFirst()
        .ifPresent(
            e -> {
              e.setScheduledAt(null);
            });
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
  }

  private void updateEventGeometry(UID event, Geometry newGeometry) throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().equals(event))
        .findFirst()
        .ifPresent(e -> e.setGeometry(newGeometry));
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
  }

  private void deleteEventGeometry(UID event) throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().equals(event))
        .findFirst()
        .ifPresent(e -> e.setGeometry(null));
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
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
        () -> assertEquals("CREATE", changeLog.type()),
        () -> assertDataElementChange(dataElement, null, currentValue, changeLog));
  }

  private void assertPropertyCreate(
      String property, String currentValue, EventChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("CREATE", changeLog.type()),
        () -> assertPropertyChange(property, null, currentValue, changeLog));
  }

  private void assertDataElementUpdate(
      String dataElement, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertUpdate(dataElement, null, previousValue, currentValue, changeLog, importUser);
  }

  private void assertPropertyUpdate(
      String property, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertUpdate(null, property, previousValue, currentValue, changeLog, importUser);
  }

  private void assertUpdate(
      String dataElement,
      String property,
      String previousValue,
      String currentValue,
      EventChangeLog changeLog,
      User user) {
    assertAll(
        () -> assertUser(user, changeLog),
        () -> assertEquals("UPDATE", changeLog.type()),
        () -> {
          if (dataElement != null) {
            assertDataElementChange(dataElement, previousValue, currentValue, changeLog);
          } else {
            assertPropertyChange(property, previousValue, currentValue, changeLog);
          }
        });
  }

  private void assertDataElementDelete(
      String dataElement, String previousValue, EventChangeLog changeLog) {
    assertDelete(dataElement, null, previousValue, changeLog);
  }

  private void assertPropertyDelete(
      String property, String previousValue, EventChangeLog changeLog) {
    assertDelete(null, property, previousValue, changeLog);
  }

  private void assertDelete(
      String dataElement, String property, String previousValue, EventChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("DELETE", changeLog.type()),
        () -> {
          if (dataElement != null) {
            assertDataElementChange(dataElement, previousValue, null, changeLog);
          } else {
            assertPropertyChange(property, previousValue, null, changeLog);
          }
        });
  }

  private static void assertDataElementChange(
      String dataElement, String previousValue, String currentValue, EventChangeLog changeLog) {
    DataValueChange expected = new DataValueChange(dataElement, previousValue, currentValue);
    assertEquals(expected, changeLog.change().dataValue());
  }

  private static void assertPropertyChange(
      String property, String previousValue, String currentValue, EventChangeLog changeLog) {
    PropertyChange expected = new PropertyChange(property, previousValue, currentValue);
    assertEquals(expected, changeLog.change().eventProperty());
  }

  private static void assertUser(User user, EventChangeLog changeLog) {
    assertAll(
        () -> assertEquals(user.getUsername(), changeLog.createdBy().getUsername()),
        () -> assertEquals(user.getFirstName(), changeLog.createdBy().getFirstName()),
        () -> assertEquals(user.getSurname(), changeLog.createdBy().getSurname()),
        () -> assertEquals(user.getUid(), changeLog.createdBy().getUid()));
  }

  private ObjectBundle setUpMetadata(String path) throws IOException {
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(new ClassPathResource(path).getInputStream(), RenderFormat.JSON);
    ObjectBundleParams params = new ObjectBundleParams();
    params.setObjectBundleMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.setObjects(metadata);
    ObjectBundle bundle = objectBundleService.create(params);
    Assertions.assertNoErrors(objectBundleValidationService.validate(bundle));
    objectBundleService.commit(bundle);
    return bundle;
  }

  private TrackerObjects fromJson(String path) throws IOException {
    return renderService.fromJson(
        new ClassPathResource(path).getInputStream(), TrackerObjects.class);
  }

  private List<EventChangeLog> getDataElementChangeLogs(Page<EventChangeLog> changeLogs) {
    return changeLogs.getItems().stream().filter(cl -> cl.change().dataValue() != null).toList();
  }

  private List<EventChangeLog> getChangeLogsByProperty(
      Page<EventChangeLog> changeLogs, String propertyName) {
    return changeLogs.getItems().stream()
        .filter(
            cl ->
                cl.change().eventProperty() != null
                    && cl.change().eventProperty().property().equals(propertyName))
        .toList();
  }

  private Geometry createGeometryPoint(double x, double y) {
    GeometryFactory geometryFactory = new GeometryFactory();
    Coordinate coordinate = new Coordinate(x, y);

    return geometryFactory.createPoint(coordinate);
  }
}
