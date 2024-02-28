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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueChangeLog;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueChangeLogService;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLog.TrackedEntityAttributeChange;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrackedEntityChangeLogServiceTest extends IntegrationTestBase {

  @Autowired private TrackedEntityChangeLogService trackedEntityChangeLogService;

  @Autowired private TrackedEntityAttributeValueChangeLogService oldChangeLogService;

  @Autowired private TrackedEntityProgramOwnerService programOwnerService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private ProgramService programService;

  @Autowired private IdentifiableObjectManager manager;

  private static final String MADE_UP_UID = "madeUpUid11";

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  private TrackedEntityType trackedEntityType;

  private TrackedEntity trackedEntity;

  private TrackedEntityAttribute trackedEntityAttribute;

  private TrackedEntityAttributeValue trackedEntityAttributeValue;

  private User user;

  private final PageParams defaultPageParams = new PageParams(null, null, false);

  @Override
  protected void setUpTest() {
    orgUnitA = createOrganisationUnit('A');
    manager.save(orgUnitA, false);

    orgUnitB = createOrganisationUnit('B');
    manager.save(orgUnitB, false);

    trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType, false);

    trackedEntity = createTrackedEntity(orgUnitA);
    trackedEntity.setTrackedEntityType(trackedEntityType);
    manager.save(trackedEntity, false);

    trackedEntityAttribute = createTrackedEntityAttribute('A');
    trackedEntityAttributeValue =
        new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntity, "value");
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttribute);

    trackedEntity.setTrackedEntityAttributeValues(Set.of(trackedEntityAttributeValue));
    manager.update(trackedEntity);

    user = createAndAddUser(false, "user", Set.of(orgUnitA), Set.of(orgUnitA), "F_EXPORT_DATA");
  }

  @Test
  void shouldFailWhenTrackedEntityDoesNotExist() {
    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityChangeLogService.getTrackedEntityChangeLog(
                    UID.of(MADE_UP_UID), UID.of(MADE_UP_UID), null));
    assertEquals(
        String.format("TrackedEntity with id %s could not be found.", MADE_UP_UID),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenProgramDoesNotExist() {
    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityChangeLogService.getTrackedEntityChangeLog(
                    UID.of(trackedEntity.getUid()), UID.of(MADE_UP_UID), null));
    assertEquals(
        String.format("Program with id %s could not be found.", MADE_UP_UID),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenUserHasNoAccessToAnyTETA() {
    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityChangeLogService.getTrackedEntityChangeLog(
                    UID.of(trackedEntity.getUid()), null, null));

    assertEquals(
        String.format("TrackedEntity with id %s could not be found.", trackedEntity.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenUserHasNoAccessToOrgUnitScope() {
    OrganisationUnit outOfScopeOrgUnit = createOrganisationUnit("out of scope org unit");
    manager.save(outOfScopeOrgUnit, false);
    TrackedEntity trackedEntity = createTrackedEntity(outOfScopeOrgUnit);
    trackedEntity.setTrackedEntityType(trackedEntityType);
    manager.save(trackedEntity, false);

    injectSecurityContextUser(user);

    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityChangeLogService.getTrackedEntityChangeLog(
                    UID.of(trackedEntity.getUid()), null, null));

    assertEquals(
        String.format("TrackedEntity with id %s could not be found.", trackedEntity.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenTEEnrolledToProgramAndUserHasNoOwnership() {
    Program program = createAndAddProgram('B');
    injectSecurityContextUser(user);

    programOwnerService.createOrUpdateTrackedEntityProgramOwner(
        trackedEntity.getUid(), program.getUid(), orgUnitB.getUid());

    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityChangeLogService.getTrackedEntityChangeLog(
                    UID.of(trackedEntity.getUid()), UID.of(program.getUid()), null));
    assertEquals(
        String.format("TrackedEntity with id %s could not be found.", trackedEntity.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldReturnChangeLogsWhenTrackedEntityAttributeValueIsCreated() throws NotFoundException {
    updateAttributeValue(trackedEntityAttributeValue, ChangeLogType.CREATE, "new value");
    Program program = createAndAddProgram('C');

    programOwnerService.createOrUpdateTrackedEntityProgramOwner(
        trackedEntity.getUid(), program.getUid(), orgUnitA.getUid());

    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of(trackedEntity.getUid()), UID.of(program.getUid()), defaultPageParams);

    assertNumberOfChanges(1, changeLogs.getItems());
    assertAll(
        () ->
            assertChange(
                trackedEntityAttribute.getUid(), null, "new value", changeLogs.getItems().get(0)),
        () -> assertUser(user, changeLogs.getItems().get(0)));
  }

  @Test
  void shouldReturnChangeLogsWhenTrackedEntityAttributeValueIsDeleted() throws NotFoundException {
    updateAttributeValue(trackedEntityAttributeValue, ChangeLogType.DELETE, "value");
    Program program = createAndAddProgram('D');

    programOwnerService.createOrUpdateTrackedEntityProgramOwner(
        trackedEntity.getUid(), program.getUid(), orgUnitA.getUid());

    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of(trackedEntity.getUid()), UID.of(program.getUid()), defaultPageParams);

    assertNumberOfChanges(1, changeLogs.getItems());
    assertAll(
        () ->
            assertChange(
                trackedEntityAttribute.getUid(), "value", null, changeLogs.getItems().get(0)),
        () -> assertUser(user, changeLogs.getItems().get(0)));
  }

  @Test
  void shouldReturnChangeLogsWhenTrackedEntityAttributeValueIsUpdated() throws NotFoundException {
    updateAttributeValue(trackedEntityAttributeValue, ChangeLogType.UPDATE, "updated value");
    Program program = createAndAddProgram('E');

    programOwnerService.createOrUpdateTrackedEntityProgramOwner(
        trackedEntity.getUid(), program.getUid(), orgUnitA.getUid());

    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of(trackedEntity.getUid()), UID.of(program.getUid()), defaultPageParams);

    assertNumberOfChanges(1, changeLogs.getItems());
    assertAll(
        () ->
            assertChange(
                trackedEntityAttribute.getUid(),
                null,
                "updated value",
                changeLogs.getItems().get(0)),
        () -> assertUser(user, changeLogs.getItems().get(0)));
  }

  @Test
  void shouldReturnChangeLogsWhenTrackedEntityAttributeValueIsUpdatedTwiceInARow()
      throws NotFoundException {
    updateAttributeValue(trackedEntityAttributeValue, ChangeLogType.UPDATE, "updated value");
    updateAttributeValue(trackedEntityAttributeValue, ChangeLogType.UPDATE, "latest updated value");
    Program program = createAndAddProgram('F');
    programOwnerService.createOrUpdateTrackedEntityProgramOwner(
        trackedEntity.getUid(), program.getUid(), orgUnitA.getUid());

    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of(trackedEntity.getUid()), UID.of(program.getUid()), defaultPageParams);

    assertNumberOfChanges(2, changeLogs.getItems());
    assertAll(
        () ->
            assertUpdate(
                trackedEntityAttribute.getUid(),
                "updated value",
                "latest updated value",
                changeLogs.getItems().get(0)),
        () -> assertUser(user, changeLogs.getItems().get(0)),
        () ->
            assertUpdate(
                trackedEntityAttribute.getUid(),
                null,
                "updated value",
                changeLogs.getItems().get(1)),
        () -> assertUser(user, changeLogs.getItems().get(1)));
  }

  @Test
  void shouldReturnChangeLogsWhenTrackedEntityAttributeValueIsCreatedUpdatedAndDeleted()
      throws NotFoundException {
    updateAttributeValue(trackedEntityAttributeValue, ChangeLogType.CREATE, "new value");
    updateAttributeValue(trackedEntityAttributeValue, ChangeLogType.UPDATE, "updated value");
    updateAttributeValue(trackedEntityAttributeValue, ChangeLogType.DELETE, "updated value");
    Program program = createAndAddProgram('G');
    programOwnerService.createOrUpdateTrackedEntityProgramOwner(
        trackedEntity.getUid(), program.getUid(), orgUnitA.getUid());

    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of(trackedEntity.getUid()), UID.of(program.getUid()), defaultPageParams);

    assertNumberOfChanges(3, changeLogs.getItems());
    assertAll(
        () ->
            assertDelete(
                trackedEntityAttribute.getUid(), "updated value", changeLogs.getItems().get(0)),
        () -> assertUser(user, changeLogs.getItems().get(0)),
        () ->
            assertUpdate(
                trackedEntityAttribute.getUid(),
                "new value",
                "updated value",
                changeLogs.getItems().get(1)),
        () -> assertUser(user, changeLogs.getItems().get(1)),
        () ->
            assertCreate(
                trackedEntityAttribute.getUid(), "new value", changeLogs.getItems().get(2)),
        () -> assertUser(user, changeLogs.getItems().get(2)));
  }

  @Test
  void shouldReturnChangeLogsFromAccessibleTEAWhenProgramNotSpecified() throws NotFoundException {
    updateAttributeValue(trackedEntityAttributeValue, ChangeLogType.CREATE, "new value");
    TrackedEntityTypeAttribute trackedEntityTypeAttribute =
        new TrackedEntityTypeAttribute(trackedEntityType, trackedEntityAttribute);
    trackedEntityTypeAttribute.setPublicAccess(AccessStringHelper.CATEGORY_OPTION_DEFAULT);
    manager.save(trackedEntityTypeAttribute, false);

    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of(trackedEntity.getUid()), null, defaultPageParams);

    assertNumberOfChanges(1, changeLogs.getItems());
    assertAll(
        () ->
            assertCreate(
                trackedEntityAttribute.getUid(), "new value", changeLogs.getItems().get(0)),
        () -> assertUser(user, changeLogs.getItems().get(0)));
  }

  private void updateAttributeValue(
      TrackedEntityAttributeValue trackedEntityAttributeValue,
      ChangeLogType changeLogType,
      String value) {
    oldChangeLogService.addTrackedEntityAttributeValueChangLog(
        new TrackedEntityAttributeValueChangeLog(
            trackedEntityAttributeValue, value, user.getUsername(), changeLogType));

    // wait the specified time to make sure attributes are not updated at the same time, so when
    // ordering by date, it works as expected
    Awaitility.await().pollDelay(2, TimeUnit.MILLISECONDS).untilAsserted(() -> assertTrue(true));
  }

  private static void assertNumberOfChanges(int expected, List<TrackedEntityChangeLog> changeLogs) {
    assertNotNull(changeLogs);
    assertEquals(
        expected,
        changeLogs.size(),
        String.format(
            "Expected to find %s elements in the tracked entity change log list, found %s instead: %s",
            expected, changeLogs.size(), changeLogs));
  }

  private static void assertCreate(
      String trackedEntityAttribute, String currentValue, TrackedEntityChangeLog changeLog) {
    assertAll(
        () -> assertEquals("CREATE", changeLog.type()),
        () -> assertChange(trackedEntityAttribute, null, currentValue, changeLog));
  }

  private static void assertUpdate(
      String trackedEntityAttribute,
      String previousValue,
      String currentValue,
      TrackedEntityChangeLog changeLog) {
    assertAll(
        () -> assertEquals("UPDATE", changeLog.type()),
        () -> assertChange(trackedEntityAttribute, previousValue, currentValue, changeLog));
  }

  private static void assertDelete(
      String trackedEntityAttribute, String previousValue, TrackedEntityChangeLog changeLog) {
    assertAll(
        () -> assertEquals("DELETE", changeLog.type()),
        () -> assertChange(trackedEntityAttribute, previousValue, null, changeLog));
  }

  private static void assertChange(
      String trackedEntityAttribute,
      String previousValue,
      String currentValue,
      TrackedEntityChangeLog changeLog) {
    TrackedEntityAttributeChange expected =
        new TrackedEntityAttributeChange(trackedEntityAttribute, previousValue, currentValue);
    assertEquals(expected, changeLog.change().attributeValue());
  }

  private static void assertUser(User user, TrackedEntityChangeLog changeLog) {
    assertAll(
        () -> assertEquals(user.getUsername(), changeLog.createdBy().getUsername()),
        () -> assertEquals(user.getFirstName(), changeLog.createdBy().getFirstName()),
        () -> assertEquals(user.getSurname(), changeLog.createdBy().getSurname()),
        () -> assertEquals(user.getUid(), changeLog.createdBy().getUid()));
  }

  private Program createAndAddProgram(char uniqueCharacter) {
    Program program = createProgram(uniqueCharacter);
    programService.addProgram(program);

    return program;
  }
}
