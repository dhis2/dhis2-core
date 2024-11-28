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
package org.hisp.dhis.tracker.export.trackedentity;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrackedEntityChangeLogServiceTest extends TrackerTest {
  @Autowired private TrackedEntityChangeLogService trackedEntityChangeLogService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  private TrackerImportParams importParams;

  private final TrackedEntityChangeLogOperationParams defaultOperationParams =
      TrackedEntityChangeLogOperationParams.builder().build();
  private final PageParams defaultPageParams = new PageParams(null, null, false);

  private TrackerObjects trackerObjects;

  @BeforeAll
  void setUp() throws IOException {
    injectSecurityContextUser(getAdminUser());
    setUpMetadata("tracker/simple_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    importParams = TrackerImportParams.builder().build();
    trackerObjects = fromJson("tracker/event_and_enrollment.json");

    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
  }

  @BeforeEach
  void resetSecurityContext() {
    injectSecurityContextUser(importUser);
  }

  @Test
  void shouldFailWhenTrackedEntityDoesNotExist() {
    String deletedTrackedEntity = "deletedTE00";
    String program = "BFcipDERJnf";

    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityChangeLogService.getTrackedEntityChangeLog(
                    UID.of(deletedTrackedEntity),
                    UID.of(program),
                    defaultOperationParams,
                    defaultPageParams));
    assertEquals(
        String.format("TrackedEntity with id %s could not be found.", deletedTrackedEntity),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenProgramDoesNotExist() {
    String trackedEntity = "QS6w44flWAf";
    String deletedProgram = "deletedP000";

    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityChangeLogService.getTrackedEntityChangeLog(
                    UID.of(trackedEntity),
                    UID.of(deletedProgram),
                    defaultOperationParams,
                    defaultPageParams));
    assertEquals(
        String.format("Program with id %s could not be found.", deletedProgram),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenUserHasNoAccessToTET() {
    String trackedEntity = "XUitxQbWYNq";

    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityChangeLogService.getTrackedEntityChangeLog(
                    UID.of(trackedEntity), null, defaultOperationParams, defaultPageParams));

    assertEquals(
        String.format("TrackedEntity with id %s could not be found.", trackedEntity),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenUserHasNoAccessToOrgUnitScope() {
    injectSecurityContextUser(manager.get(User.class, "o1HMTIzBGo7"));
    String trackedEntity = "XUitxQbWYNq";

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityChangeLogService.getTrackedEntityChangeLog(
                    UID.of(trackedEntity), null, defaultOperationParams, defaultPageParams));

    assertEquals(
        String.format("User has no access to TrackedEntity:%s", trackedEntity),
        exception.getMessage());
  }

  @Test
  void shouldReturnChangeLogsWhenTrackedEntityAttributeValueIsCreated()
      throws NotFoundException, ForbiddenException, BadRequestException {
    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of("QS6w44flWAf"), null, defaultOperationParams, defaultPageParams);

    assertNumberOfChanges(1, changeLogs.getItems());
    assertAll(() -> assertCreate("numericAttr", "88", changeLogs.getItems().get(0)));
  }

  @Test
  void shouldReturnChangeLogsWhenTrackedEntityAttributeValueIsDeleted()
      throws NotFoundException, ForbiddenException, BadRequestException {
    String trackedEntity = "QS6w44flWAf";
    String trackedEntityAttribute = "numericAttr";
    updateAttributeValue(trackedEntity, trackedEntityAttribute, "");

    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of(trackedEntity), null, defaultOperationParams, defaultPageParams);

    assertNumberOfChanges(2, changeLogs.getItems());
    assertAll(
        () -> assertDelete(trackedEntityAttribute, "88", changeLogs.getItems().get(0)),
        () -> assertCreate(trackedEntityAttribute, "88", changeLogs.getItems().get(1)));
  }

  @Test
  void shouldReturnChangeLogsWhenTrackedEntityAttributeValueIsUpdated()
      throws NotFoundException, ForbiddenException, BadRequestException {
    String trackedEntity = "QS6w44flWAf";
    String trackedEntityAttribute = "numericAttr";
    String updatedValue = "100";
    updateAttributeValue(trackedEntity, trackedEntityAttribute, updatedValue);

    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of(trackedEntity), null, defaultOperationParams, defaultPageParams);

    assertNumberOfChanges(2, changeLogs.getItems());
    assertAll(
        () ->
            assertUpdate(trackedEntityAttribute, "88", updatedValue, changeLogs.getItems().get(0)),
        () -> assertCreate(trackedEntityAttribute, "88", changeLogs.getItems().get(1)));
  }

  @Test
  void shouldReturnChangeLogsWhenTrackedEntityAttributeValueIsUpdatedTwiceInARow()
      throws NotFoundException, ForbiddenException, BadRequestException {
    String trackedEntity = "QS6w44flWAf";
    String trackedEntityAttribute = "numericAttr";
    String updatedValue = "100";
    String secondUpdatedValue = "200";
    updateAttributeValue(trackedEntity, trackedEntityAttribute, updatedValue);
    updateAttributeValue(trackedEntity, trackedEntityAttribute, secondUpdatedValue);

    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of(trackedEntity), null, defaultOperationParams, defaultPageParams);

    assertNumberOfChanges(3, changeLogs.getItems());
    assertAll(
        () ->
            assertUpdate(
                trackedEntityAttribute,
                updatedValue,
                secondUpdatedValue,
                changeLogs.getItems().get(0)),
        () ->
            assertUpdate(trackedEntityAttribute, "88", updatedValue, changeLogs.getItems().get(1)),
        () -> assertCreate(trackedEntityAttribute, "88", changeLogs.getItems().get(2)));
  }

  @Test
  void shouldReturnChangeLogsWhenTrackedEntityAttributeValueIsCreatedUpdatedAndDeleted()
      throws NotFoundException, ForbiddenException, BadRequestException {
    String trackedEntity = "QS6w44flWAf";
    String trackedEntityAttribute = "numericAttr";
    String updatedValue = "100";
    updateAttributeValue(trackedEntity, trackedEntityAttribute, updatedValue);
    updateAttributeValue(trackedEntity, trackedEntityAttribute, "");

    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of(trackedEntity), null, defaultOperationParams, defaultPageParams);

    assertNumberOfChanges(3, changeLogs.getItems());
    assertAll(
        () -> assertDelete(trackedEntityAttribute, updatedValue, changeLogs.getItems().get(0)),
        () ->
            assertUpdate(trackedEntityAttribute, "88", updatedValue, changeLogs.getItems().get(1)),
        () -> assertCreate(trackedEntityAttribute, "88", changeLogs.getItems().get(2)));
  }

  @Test
  void shouldReturnChangeLogsFromSpecifiedProgramOnlyWhenMultipleLogsExist()
      throws NotFoundException, ForbiddenException, BadRequestException {
    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of("QS6w44flWAf"),
            UID.of("BFcipDERJnf"),
            defaultOperationParams,
            defaultPageParams);

    assertNumberOfChanges(1, changeLogs.getItems());
    assertAll(() -> assertCreate("dIVt4l5vIOa", "Value", changeLogs.getItems().get(0)));
  }

  private static void assertNumberOfChanges(int expected, List<TrackedEntityChangeLog> changeLogs) {
    assertNotNull(changeLogs);
    assertEquals(
        expected,
        changeLogs.size(),
        String.format(
            "Expected to find %s attributes in the tracked entity change log list, found %s instead: %s",
            expected, changeLogs.size(), changeLogs));
  }

  private void assertCreate(
      String trackedEntityAttribute, String currentValue, TrackedEntityChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("CREATE", changeLog.getChangeLogType().name()),
        () -> assertChange(trackedEntityAttribute, null, currentValue, changeLog));
  }

  private void assertUpdate(
      String trackedEntityAttribute,
      String previousValue,
      String currentValue,
      TrackedEntityChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("UPDATE", changeLog.getChangeLogType().name()),
        () -> assertChange(trackedEntityAttribute, previousValue, currentValue, changeLog));
  }

  private void assertDelete(
      String trackedEntityAttribute, String previousValue, TrackedEntityChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("DELETE", changeLog.getChangeLogType().name()),
        () -> assertChange(trackedEntityAttribute, previousValue, null, changeLog));
  }

  private void assertChange(
      String trackedEntityAttribute,
      String previousValue,
      String currentValue,
      TrackedEntityChangeLog changeLog) {
    assertEquals(trackedEntityAttribute, changeLog.getTrackedEntityAttribute().getUid());
    assertEquals(previousValue, changeLog.getPreviousValue());
    assertEquals(currentValue, changeLog.getCurrentValue());
  }

  private void assertUser(User user, TrackedEntityChangeLog changeLog) {
    assertAll(
        () -> assertEquals(user.getUsername(), changeLog.getCreatedBy().getUsername()),
        () -> assertEquals(user.getFirstName(), changeLog.getCreatedBy().getFirstName()),
        () -> assertEquals(user.getSurname(), changeLog.getCreatedBy().getSurname()),
        () -> assertEquals(user.getUid(), changeLog.getCreatedBy().getUid()));
  }

  private void updateAttributeValue(
      String trackedEntity, String trackedEntityAttribute, String newValue) {
    trackerObjects.getTrackedEntities().stream()
        .filter(t -> t.getTrackedEntity().getValue().equalsIgnoreCase(trackedEntity))
        .findFirst()
        .ifPresent(
            t -> {
              t.getAttributes().stream()
                  .filter(
                      tea ->
                          tea.getAttribute()
                              .getIdentifier()
                              .equalsIgnoreCase(trackedEntityAttribute))
                  .findFirst()
                  .ifPresent(attribute -> attribute.setValue(newValue));

              assertNoErrors(
                  trackerImportService.importTracker(
                      importParams, TrackerObjects.builder().trackedEntities(List.of(t)).build()));
            });
  }
}
