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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static org.hisp.dhis.security.Authorities.ALL;
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertPagerLink;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.webapi.controller.tracker.JsonAssertions;
import org.hisp.dhis.webapi.controller.tracker.JsonEventChangeLog;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.hisp.dhis.webapi.controller.tracker.JsonPage.JsonPager;
import org.hisp.dhis.webapi.controller.tracker.JsonUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class EventsExportChangeLogsControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String DATA_ELEMENT_VALUE = "value 1";

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private CategoryService categoryService;

  @Autowired private DhisConfigurationProvider config;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  private CategoryOptionCombo coc;

  private User user;

  private Program program;

  private ProgramStage programStage;

  private OrganisationUnit orgUnit;

  private User owner;

  private TrackedEntityType trackedEntityType;

  private TrackerEvent event;

  private DataElement dataElement;

  private EventDataValue dataValue;

  @BeforeEach
  void setUp() {
    owner = makeUser("owner");

    coc = categoryService.getDefaultCategoryOptionCombo();

    orgUnit = createOrganisationUnit("a");
    orgUnit.setUid("ZiMBqH865GV");
    manager.save(orgUnit);

    user = createAndAddUser("username", orgUnit, ALL.name());
    injectSecurityContextUser(user);

    trackedEntityType = trackedEntityTypeAccessible();

    program = createProgram('A');
    program.getOrganisationUnits().add(orgUnit);
    program.setUid("q04UBOqq3rp");
    program.setTrackedEntityType(trackedEntityType);
    program.setAllowChangeLog(true);
    manager.save(program);

    dataElement = createDataElement('A', ValueType.TEXT, AggregationType.NONE);
    dataElement.getSharing().setOwner(owner);
    manager.save(dataElement, false);

    programStage = createProgramStage('A', program);
    programStage.setUid("pSllsjpfLH2");
    program.getProgramStages().add(programStage);
    ProgramStageDataElement programStageDataElement =
        createProgramStageDataElement(programStage, dataElement, 1, false);
    manager.save(programStageDataElement);
    programStage.setProgramStageDataElements(Sets.newHashSet(programStageDataElement));
    manager.save(programStage);

    dataValue = new EventDataValue();
    dataValue.setDataElement(dataElement.getUid());
    dataValue.setStoredBy("user");
    dataValue.setValue(DATA_ELEMENT_VALUE);

    event = event(enrollment(trackedEntity()));
    event.getEventDataValues().add(dataValue);
    manager.update(event);

    updateDataValue("value 2");
    updateDataValue("value 3");
    updateDataValue("");
  }

  @Test
  void shouldGetEventChangeLogInDescOrderByDefault() {
    JsonList<JsonEventChangeLog> changeLogs =
        GET("/tracker/events/{id}/changeLogs", event.getUid())
            .content(HttpStatus.OK)
            .getList("changeLogs", JsonEventChangeLog.class);
    List<JsonEventChangeLog> dataValueChangeLogs =
        changeLogs.stream()
            .filter(log -> log.getChange().getDataValue().getDataElement() != null)
            .toList();
    List<JsonEventChangeLog> eventFieldChangeLogs =
        changeLogs.stream()
            .filter(log -> log.getChange().getEventField().getField() != null)
            .toList();

    assertHasSize(3, dataValueChangeLogs);
    assertHasSize(2, eventFieldChangeLogs);

    assertAll(
        () -> assertDelete(dataElement, "value 3", dataValueChangeLogs.get(0)),
        () -> assertUpdate(dataElement, "value 2", "value 3", dataValueChangeLogs.get(1)),
        () -> assertUpdate(dataElement, "value 1", "value 2", dataValueChangeLogs.get(2)),
        () ->
            assertFieldCreateExists("occurredAt", "2023-01-10 00:00:00.000", eventFieldChangeLogs),
        () ->
            assertFieldCreateExists(
                "scheduledAt", "2023-01-10 00:00:00.000", eventFieldChangeLogs));
  }

  @Test
  void shouldGetEventChangeLogInAscOrder() {
    JsonList<JsonEventChangeLog> changeLogs =
        GET("/tracker/events/{id}/changeLogs?order=createdAt:asc", event.getUid())
            .content(HttpStatus.OK)
            .getList("changeLogs", JsonEventChangeLog.class);
    List<JsonEventChangeLog> dataValueChangeLogs =
        changeLogs.stream()
            .filter(log -> log.getChange().getDataValue().getDataElement() != null)
            .toList();
    List<JsonEventChangeLog> eventFieldChangeLogs =
        changeLogs.stream()
            .filter(log -> log.getChange().getEventField().getField() != null)
            .toList();

    assertHasSize(3, dataValueChangeLogs);
    assertHasSize(2, eventFieldChangeLogs);
    assertAll(
        () -> assertUpdate(dataElement, "value 1", "value 2", dataValueChangeLogs.get(0)),
        () -> assertUpdate(dataElement, "value 2", "value 3", dataValueChangeLogs.get(1)),
        () -> assertDelete(dataElement, "value 3", dataValueChangeLogs.get(2)),
        () ->
            assertFieldCreateExists("occurredAt", "2023-01-10 00:00:00.000", eventFieldChangeLogs),
        () ->
            assertFieldCreateExists(
                "scheduledAt", "2023-01-10 00:00:00.000", eventFieldChangeLogs));
  }

  @Test
  void shouldGetEventChangeLogsWhenFilteringByField() {
    JsonList<JsonEventChangeLog> changeLogs =
        GET("/tracker/events/{id}/changeLogs?filter=field:eq:occurredAt", event.getUid())
            .content(HttpStatus.OK)
            .getList("changeLogs", JsonEventChangeLog.class);
    List<JsonEventChangeLog> eventFieldChangeLogs =
        changeLogs.stream()
            .filter(log -> log.getChange().getEventField().getField() != null)
            .toList();

    assertAll(
        () -> assertHasSize(1, eventFieldChangeLogs),
        () ->
            assertFieldCreateExists("occurredAt", "2023-01-10 00:00:00.000", eventFieldChangeLogs));
  }

  @Test
  void shouldGetChangeLogPagerWithNextElementWhenMultipleElementsImportedAndFirstPageRequested() {
    JsonPage changeLogs =
        GET(
                "/tracker/events/{id}/changeLogs?page={page}&pageSize={pageSize}",
                event.getUid(),
                "1",
                "1")
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonPager pager = changeLogs.getPager();
    assertAll(
        () -> assertEquals(1, pager.getPage()),
        () -> assertEquals(1, pager.getPageSize()),
        () -> assertHasNoMember(pager, "prevPage", "total", "pageCount"),
        () ->
            assertPagerLink(
                pager.getNextPage(),
                2,
                1,
                String.format(
                    "http://localhost/api/tracker/events/%s/changeLogs", event.getUid())));
  }

  @Test
  void
      shouldGetChangeLogPagerWithNextAndPreviousElementsWhenMultipleElementsImportedAndSecondPageRequested() {
    JsonPage changeLogs =
        GET(
                "/tracker/events/{id}/changeLogs?page={page}&pageSize={pageSize}",
                event.getUid(),
                "2",
                "1")
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonPager pager = changeLogs.getPager();
    assertAll(
        () -> assertEquals(2, pager.getPage()),
        () -> assertEquals(1, pager.getPageSize()),
        () -> assertHasNoMember(pager, "total", "pageCount"),
        () ->
            assertPagerLink(
                pager.getPrevPage(),
                1,
                1,
                String.format("http://localhost/api/tracker/events/%s/changeLogs", event.getUid())),
        () ->
            assertPagerLink(
                pager.getNextPage(),
                3,
                1,
                String.format(
                    "http://localhost/api/tracker/events/%s/changeLogs", event.getUid())));
  }

  @Test
  void
      shouldGetChangeLogPagerWithPreviousElementWhenMultipleElementsImportedAndLastPageRequested() {
    JsonPage changeLogs =
        GET(
                "/tracker/events/{id}/changeLogs?page={page}&pageSize={pageSize}",
                event.getUid(),
                "5",
                "1")
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonPager pager = changeLogs.getPager();
    assertAll(
        () -> assertEquals(5, pager.getPage()),
        () -> assertEquals(1, pager.getPageSize()),
        () ->
            assertPagerLink(
                pager.getPrevPage(),
                4,
                1,
                String.format("http://localhost/api/tracker/events/%s/changeLogs", event.getUid())),
        () -> assertHasNoMember(pager, "nextPage", "total", "pageCount"));
  }

  @Test
  void
      shouldGetChangeLogPagerWithoutPreviousNorNextElementWhenMultipleElementsImportedAndAllElementsFitInOnePage() {
    JsonPage changeLogs =
        GET(
                "/tracker/events/{id}/changeLogs?page={page}&pageSize={pageSize}",
                event.getUid(),
                "1",
                "5")
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonPager pagerObject = changeLogs.getPager();
    assertAll(
        () -> assertEquals(1, pagerObject.getPage()),
        () -> assertEquals(5, pagerObject.getPageSize()),
        () -> assertHasNoMember(pagerObject, "prevPage", "nextPage", "total", "pageCount"));
  }

  @Test
  void shouldNotLogChangesWhenChangeLogConfigDisabled() {
    event = event(enrollment(trackedEntity()));
    event.getEventDataValues().add(dataValue);
    manager.update(event);

    Program program = manager.get(Program.class, event.getProgramStage().getProgram().getUid());
    program.setAllowChangeLog(false);
    manager.update(program);

    updateDataValue("new value");
    updateDataValue("updated value");
    updateDataValue("");
    updateScheduledAtEventField("2025-01-10");

    JsonList<JsonEventChangeLog> changeLogs =
        GET("/tracker/events/{id}/changeLogs?order=createdAt:asc", event.getUid())
            .content(HttpStatus.OK)
            .getList("changeLogs", JsonEventChangeLog.class);

    List<JsonEventChangeLog> dataValueChangeLogs =
        changeLogs.stream()
            .filter(log -> log.getChange().getDataValue().getDataElement() != null)
            .toList();
    List<JsonEventChangeLog> eventFieldChangeLogs =
        changeLogs.stream()
            .filter(log -> log.getChange().getEventField().getField() != null)
            .toList();

    assertIsEmpty(dataValueChangeLogs);
    assertIsEmpty(eventFieldChangeLogs);
  }

  @Test
  void shouldGetEventChangeLogsWithSimpleFieldsFilter() {
    JsonList<JsonEventChangeLog> changeLogs =
        GET("/tracker/events/{id}/changeLogs?fields=:simple", event.getUid())
            .content(HttpStatus.OK)
            .getList("changeLogs", JsonEventChangeLog.class);

    assertFalse(changeLogs.isEmpty(), "should have some change logs");
    assertHasOnlyMembers(changeLogs.get(0), "createdAt", "type");
  }

  private void updateDataValue(String value) {
    JsonWebMessage importResponse =
        POST("/tracker?async=false&importStrategy=UPDATE", createDataValueJson(event, value))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());
  }

  private void updateScheduledAtEventField(String value) {
    JsonWebMessage importResponse =
        POST(
                "/tracker?async=false&importStrategy=UPDATE",
                createScheduledAtEventFieldJson(event, value))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());
  }

  private TrackedEntity trackedEntity() {
    TrackedEntity te = trackedEntity(orgUnit);
    manager.save(te, false);
    return te;
  }

  private TrackedEntity trackedEntity(OrganisationUnit orgUnit) {
    return trackedEntity(orgUnit, trackedEntityType);
  }

  private TrackedEntity trackedEntity(
      OrganisationUnit orgUnit, TrackedEntityType trackedEntityType) {
    TrackedEntity te = createTrackedEntity(orgUnit, trackedEntityType);
    te.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    te.getSharing().setOwner(owner);
    return te;
  }

  private TrackedEntityType trackedEntityTypeAccessible() {
    TrackedEntityType type = trackedEntityType();
    type.getSharing().addUserAccess(userAccess());
    manager.save(type, false);
    return type;
  }

  private TrackedEntityType trackedEntityType() {
    TrackedEntityType type = createTrackedEntityType('A');
    type.getSharing().setOwner(owner);
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    return type;
  }

  private UserAccess userAccess() {
    UserAccess a = new UserAccess();
    a.setUser(user);
    a.setAccess(AccessStringHelper.FULL);
    return a;
  }

  private Enrollment enrollment(TrackedEntity te) {
    Enrollment enrollment = new Enrollment(program, te, te.getOrganisationUnit());
    enrollment.setAutoFields();
    enrollment.setEnrollmentDate(new Date());
    enrollment.setOccurredDate(new Date());
    enrollment.setStatus(EnrollmentStatus.COMPLETED);
    manager.save(enrollment);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        te, program, te.getOrganisationUnit());
    return enrollment;
  }

  private TrackerEvent event(Enrollment enrollment) {
    TrackerEvent eventA = new TrackerEvent();
    eventA.setEnrollment(enrollment);
    eventA.setProgramStage(programStage);
    eventA.setOrganisationUnit(enrollment.getOrganisationUnit());
    eventA.setAttributeOptionCombo(coc);
    eventA.setAutoFields();
    manager.save(eventA);
    return eventA;
  }

  private String createDataValueJson(TrackerEvent event, String value) {
    return """
           {
             "events": [
               {
                 "event": "%s",
                 "status": "COMPLETED",
                 "program": "%s",
                 "programStage": "%s",
                 "enrollment": "%s",
                 "trackedEntity": "%s",
                 "orgUnit": "%s",
                 "occurredAt": "2023-01-10",
                 "scheduledAt": "2023-01-10",
                 "storedBy": "tracker",
                 "followUp": false,
                 "createdAtClient": "2017-01-20T10:44:03.222",
                 "completedBy": "tracker",
                 "completedAt": "2023-01-20",
                 "notes": [],
                 "dataValues": [
                   {
                     "dataElement": "%s",
                     "value": "%s"
                   }
                 ]
               }
             ]
           }}
           """
        .formatted(
            event.getUid(),
            program.getUid(),
            programStage.getUid(),
            event.getEnrollment().getUid(),
            event.getEnrollment().getTrackedEntity().getUid(),
            event.getOrganisationUnit().getUid(),
            event.getEventDataValues().iterator().next().getDataElement(),
            value);
  }

  private String createScheduledAtEventFieldJson(TrackerEvent event, String scheduledAt) {
    return """
           {
             "events": [
               {
                 "event": "%s",
                 "status": "COMPLETED",
                 "program": "%s",
                 "programStage": "%s",
                 "enrollment": "%s",
                 "trackedEntity": "%s",
                 "orgUnit": "%s",
                 "occurredAt": "2023-01-10",
                 "scheduledAt": "%s",
                 "storedBy": "tracker",
                 "followUp": false,
                 "createdAtClient": "2017-01-20T10:44:03.222",
                 "completedBy": "tracker",
                 "completedAt": "2023-01-20",
                 "notes": []
               }
             ]
           }}
           """
        .formatted(
            event.getUid(),
            program.getUid(),
            programStage.getUid(),
            event.getEnrollment().getUid(),
            event.getEnrollment().getTrackedEntity().getUid(),
            event.getOrganisationUnit().getUid(),
            scheduledAt);
  }

  private static void assertUser(JsonEventChangeLog changeLog) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    JsonUser createdBy = changeLog.getCreatedBy();
    assertAll(
        () -> assertEquals(currentUser.getUid(), createdBy.getUid()),
        () -> assertEquals(currentUser.getUsername(), createdBy.getUsername()),
        () -> assertEquals(currentUser.getFirstName(), createdBy.getFirstName()),
        () -> assertEquals(currentUser.getSurname(), createdBy.getSurname()));
  }

  private static void assertUpdate(
      DataElement dataElement,
      String previousValue,
      String currentValue,
      JsonEventChangeLog actual) {
    assertAll(
        () -> assertUser(actual),
        () -> assertEquals("UPDATE", actual.getType()),
        () -> assertChange(dataElement, previousValue, currentValue, actual));
  }

  private static void assertDelete(
      DataElement dataElement, String previousValue, JsonEventChangeLog actual) {
    assertAll(
        () -> assertUser(actual),
        () -> assertEquals("DELETE", actual.getType()),
        () -> assertChange(dataElement, previousValue, null, actual));
  }

  private static void assertChange(
      DataElement dataElement,
      String previousValue,
      String currentValue,
      JsonEventChangeLog actual) {
    assertAll(
        () ->
            assertEquals(dataElement.getUid(), actual.getChange().getDataValue().getDataElement()),
        () -> assertEquals(previousValue, actual.getChange().getDataValue().getPreviousValue()),
        () -> assertEquals(currentValue, actual.getChange().getDataValue().getCurrentValue()),
        () -> JsonAssertions.assertHasNoMember(actual.getChange(), "eventField"));
  }

  private static void assertFieldCreateExists(
      String field, String currentValue, List<JsonEventChangeLog> changeLogs) {
    assertTrue(
        changeLogs.stream().anyMatch(cl -> isEventFieldCreate(cl, field, currentValue)),
        "Expected a "
            + field
            + " change with value "
            + currentValue
            + " among the change log entries.");
    assertTrue(
        changeLogs.stream().noneMatch(cl -> cl.getChange().has("dataValue")),
        "Data value change not expected to be present, but it was");
  }

  private static boolean isEventFieldCreate(
      JsonEventChangeLog actual, String field, String currentValue) {
    return actual.getType().equals(ChangeLogType.CREATE.name())
        && actual.getChange().getEventField().getField().equals(field)
        && actual.getChange().getEventField().getCurrentValue().equals(currentValue)
        && actual.getChange().getEventField().getPreviousValue() == null;
  }
}
