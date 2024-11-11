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
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(
            UID.of(event), defaultOperationParams, defaultPageParams);

    assertNumberOfChanges(1, changeLogs.getItems());
    assertCreate(dataElement, "15", changeLogs.getItems().get(0));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsDeleted()
      throws NotFoundException, IOException, ForbiddenException {
    String event = "QRYjLTiJTrA";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "");

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(
            UID.of(event), defaultOperationParams, defaultPageParams);

    assertNumberOfChanges(2, changeLogs.getItems());
    assertAll(
        () -> assertDelete(dataElement, "15", changeLogs.getItems().get(0)),
        () -> assertCreate(dataElement, "15", changeLogs.getItems().get(1)));
  }

  @Test
  void shouldNotUpdateChangeLogsWhenDataValueIsDeletedTwiceInARow()
      throws NotFoundException, IOException, ForbiddenException {
    String event = "QRYjLTiJTrA";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "");
    updateDataValue(event, dataElement, "");

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(
            UID.of(event), defaultOperationParams, defaultPageParams);

    assertNumberOfChanges(2, changeLogs.getItems());
    assertAll(
        () -> assertDelete(dataElement, "15", changeLogs.getItems().get(0)),
        () -> assertCreate(dataElement, "15", changeLogs.getItems().get(1)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsUpdated()
      throws NotFoundException, IOException, ForbiddenException {
    String event = "QRYjLTiJTrA";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "20");

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(
            UID.of(event), defaultOperationParams, defaultPageParams);

    assertNumberOfChanges(2, changeLogs.getItems());
    assertAll(
        () -> assertUpdate(dataElement, "15", "20", changeLogs.getItems().get(0)),
        () -> assertCreate(dataElement, "15", changeLogs.getItems().get(1)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsUpdatedTwiceInARow()
      throws NotFoundException, IOException, ForbiddenException {
    String event = "QRYjLTiJTrA";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "20");
    updateDataValue(event, dataElement, "25");

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(
            UID.of(event), defaultOperationParams, defaultPageParams);

    assertNumberOfChanges(3, changeLogs.getItems());
    assertAll(
        () -> assertUpdate(dataElement, "20", "25", changeLogs.getItems().get(0)),
        () -> assertUpdate(dataElement, "15", "20", changeLogs.getItems().get(1)),
        () -> assertCreate(dataElement, "15", changeLogs.getItems().get(2)));
  }

  @Test
  void shouldReturnChangeLogsWhenDataValueIsCreatedUpdatedAndDeleted()
      throws IOException, NotFoundException, ForbiddenException {
    String event = "QRYjLTiJTrA";
    String dataElement = getDataElement(event);

    updateDataValue(event, dataElement, "20");
    updateDataValue(event, dataElement, "");

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(
            UID.of(event), defaultOperationParams, defaultPageParams);

    assertNumberOfChanges(3, changeLogs.getItems());
    assertAll(
        () -> assertDelete(dataElement, "20", changeLogs.getItems().get(0)),
        () -> assertUpdate(dataElement, "15", "20", changeLogs.getItems().get(1)),
        () -> assertCreate(dataElement, "15", changeLogs.getItems().get(2)));
  }

  @Test
  void shouldReturnOnlyUserNameWhenUserDoesNotExistInDatabase()
      throws ForbiddenException, NotFoundException {
    Event event = getEvent("QRYjLTiJTrA");
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();
    DataElement dataElement = manager.get(DataElement.class, dataElementUid);
    User deletedUser = new User();
    deletedUser.setUsername("deletedUsername");
    eventChangeLogService.addDataValueChangeLog(
        event, dataElement, "current", "previous", UPDATE, deletedUser.getUsername());

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(
            UID.of("QRYjLTiJTrA"), defaultOperationParams, defaultPageParams);

    assertNumberOfChanges(2, changeLogs.getItems());
    assertAll(
        () ->
            assertUpdate(
                dataElementUid, "previous", "current", changeLogs.getItems().get(0), deletedUser),
        () -> assertCreate(dataElementUid, "15", changeLogs.getItems().get(1)));
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

  private void assertCreate(String dataElement, String currentValue, EventChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("CREATE", changeLog.getChangeLogType().name()),
        () -> assertChange(dataElement, null, currentValue, changeLog));
  }

  private void assertUpdate(
      String dataElement, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertUpdate(dataElement, previousValue, currentValue, changeLog, importUser);
  }

  private void assertUpdate(
      String dataElement,
      String previousValue,
      String currentValue,
      EventChangeLog changeLog,
      User user) {
    assertAll(
        () -> assertUser(user, changeLog),
        () -> assertEquals("UPDATE", changeLog.getChangeLogType().name()),
        () -> assertChange(dataElement, previousValue, currentValue, changeLog));
  }

  private void assertDelete(String dataElement, String previousValue, EventChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("DELETE", changeLog.getChangeLogType().name()),
        () -> assertChange(dataElement, previousValue, null, changeLog));
  }

  private static void assertChange(
      String dataElement, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertEquals(dataElement, changeLog.getDataElement().getUid());
    assertEquals(currentValue, changeLog.getCurrentValue());
    assertEquals(previousValue, changeLog.getPreviousValue());
  }

  private static void assertUser(User user, EventChangeLog changeLog) {
    assertAll(
        () -> assertEquals(user.getUsername(), changeLog.getCreatedBy().getUsername()),
        () ->
            assertEquals(
                user.getFirstName(),
                changeLog.getCreatedBy() == null ? null : changeLog.getCreatedBy().getFirstName()),
        () ->
            assertEquals(
                user.getSurname(),
                changeLog.getCreatedBy() == null ? null : changeLog.getCreatedBy().getSurname()),
        () ->
            assertEquals(
                user.getUid(),
                changeLog.getCreatedBy() == null ? null : changeLog.getCreatedBy().getUid()));
  }

  private void setUpMetadata(String path) throws IOException {
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(new ClassPathResource(path).getInputStream(), RenderFormat.JSON);
    ObjectBundleParams params = new ObjectBundleParams();
    params.setObjectBundleMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.setObjects(metadata);
    ObjectBundle bundle = objectBundleService.create(params);
    Assertions.assertNoErrors(objectBundleValidationService.validate(bundle));
    objectBundleService.commit(bundle);
  }

  private TrackerObjects fromJson(String path) throws IOException {
    return renderService.fromJson(
        new ClassPathResource(path).getInputStream(), TrackerObjects.class);
  }
}
