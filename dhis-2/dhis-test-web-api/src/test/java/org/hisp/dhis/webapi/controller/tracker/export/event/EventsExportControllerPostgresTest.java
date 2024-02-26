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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static org.hisp.dhis.security.Authorities.ALL;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.Sets;
import java.util.Date;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.controller.tracker.JsonEventChangeLog;
import org.hisp.dhis.webapi.controller.tracker.JsonEventChangeLog.JsonDataValueChange;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.hisp.dhis.webapi.controller.tracker.JsonPage.JsonPager;
import org.hisp.dhis.webapi.controller.tracker.JsonUser;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventsExportControllerPostgresTest extends DhisControllerIntegrationTest {
  private static final String DATA_ELEMENT_VALUE = "value 1";

  @Autowired private IdentifiableObjectManager manager;
  private User user;
  private Program program;
  private ProgramStage programStage;
  private OrganisationUnit orgUnit;
  private User owner;
  private TrackedEntityType trackedEntityType;
  private Event event;
  private DataElement dataElement;

  @BeforeEach
  void setUp() {
    owner = makeUser("owner");
    orgUnit = createOrganisationUnit("a");
    orgUnit.setUid("ZiMBqH865GV");
    manager.save(orgUnit);

    user = createAndAddUser("username", orgUnit, ALL.name());

    trackedEntityType = trackedEntityTypeAccessible();

    program = createProgram('A');
    program.getOrganisationUnits().add(orgUnit);
    program.setUid("q04UBOqq3rp");
    program.setTrackedEntityType(trackedEntityType);
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

    EventDataValue dataValue = new EventDataValue();
    dataValue.setDataElement(dataElement.getUid());
    dataValue.setStoredBy("user");
    dataValue.setValue(DATA_ELEMENT_VALUE);

    event = event(enrollment(trackedEntity()));
    event.getEventDataValues().add(dataValue);
    manager.update(event);

    JsonWebMessage importResponse =
        POST("/tracker?async=false&importStrategy=UPDATE", createJson(event, "value 2"))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());

    importResponse =
        POST("/tracker?async=false&importStrategy=UPDATE", createJson(event, "value 3"))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());

    importResponse =
        POST("/tracker?async=false&importStrategy=UPDATE", createJson(event, ""))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());
  }

  @Test
  void shouldGetEventChangeLogWhenDataValueUpdatedAndThenDeleted() {
    JsonList<JsonEventChangeLog> changeLogs =
        GET("/tracker/events/{id}/changeLogs", event.getUid())
            .content(HttpStatus.OK)
            .getList("changeLogs", JsonEventChangeLog.class);

    JsonEventChangeLog changeLog = changeLogs.get(0);
    JsonUser createdBy = changeLog.getCreatedBy();
    JsonDataValueChange dataValueChange = changeLog.getChange().getDataValueChange();
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();

    assertAll(
        () -> assertEquals(currentUser.getUid(), createdBy.getUid()),
        () -> assertEquals(currentUser.getUsername(), createdBy.getUsername()),
        () -> assertEquals(currentUser.getFirstName(), createdBy.getFirstName()),
        () -> assertEquals(currentUser.getSurname(), createdBy.getSurname()),
        () -> assertEquals("DELETE", changeLog.getType()),
        () -> assertEquals(dataElement.getUid(), dataValueChange.getDataElement()),
        () -> assertEquals("value 3", dataValueChange.getPreviousValue()),
        () -> assertHasNoMember(dataValueChange, "currentValue"));
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
        () -> assertHasNoMember(pager, "prevPage"),
        () ->
            assertPagerLink(
                pager.getNextPage(),
                2,
                1,
                String.format("http://localhost/tracker/events/%s/changeLogs", event.getUid())));
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
        () ->
            assertPagerLink(
                pager.getPrevPage(),
                1,
                1,
                String.format("http://localhost/tracker/events/%s/changeLogs", event.getUid())),
        () ->
            assertPagerLink(
                pager.getNextPage(),
                3,
                1,
                String.format("http://localhost/tracker/events/%s/changeLogs", event.getUid())));
  }

  @Test
  void
      shouldGetChangeLogPagerWithPreviousElementWhenMultipleElementsImportedAndLastPageRequested() {
    JsonPage changeLogs =
        GET(
                "/tracker/events/{id}/changeLogs?page={page}&pageSize={pageSize}",
                event.getUid(),
                "3",
                "1")
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonPager pager = changeLogs.getPager();
    assertAll(
        () -> assertEquals(3, pager.getPage()),
        () -> assertEquals(1, pager.getPageSize()),
        () ->
            assertPagerLink(
                pager.getPrevPage(),
                2,
                1,
                String.format("http://localhost/tracker/events/%s/changeLogs", event.getUid())),
        () -> assertHasNoMember(pager, "nextPage"));
  }

  @Test
  void
      shouldGetChangeLogPagerWithoutPreviousNorNextElementWhenMultipleElementsImportedAndAllElementsFitInOnePage() {
    JsonPage changeLogs =
        GET(
                "/tracker/events/{id}/changeLogs?page={page}&pageSize={pageSize}",
                event.getUid(),
                "1",
                "3")
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonPager pagerObject = changeLogs.getPager();
    assertAll(
        () -> assertEquals(1, pagerObject.getPage()),
        () -> assertEquals(3, pagerObject.getPageSize()),
        () -> assertHasNoMember(pagerObject, "prevPage"),
        () -> assertHasNoMember(pagerObject, "nextPage"));
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
    TrackedEntity te = createTrackedEntity(orgUnit);
    te.setTrackedEntityType(trackedEntityType);
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
    enrollment.setStatus(ProgramStatus.COMPLETED);
    manager.save(enrollment);
    return enrollment;
  }

  private Event event(Enrollment enrollment) {
    Event event = new Event(enrollment, programStage, enrollment.getOrganisationUnit());
    event.setAutoFields();
    manager.save(event);
    return event;
  }

  private String createJson(Event event, String value) {
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
            "deleted": false,
            "createdAt": "2018-01-20T10:44:03.222",
            "createdAtClient": "2017-01-20T10:44:03.222",
            "updatedAt": "2018-01-20T10:44:33.777",
            "completedBy": "tracker",
            "completedAt": "2023-01-20",
            "notes": [],
            "followup": false,
            "geometry": null,
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

  private static void assertPagerLink(String actual, int page, int pageSize, String start) {
    assertNotNull(actual);
    assertAll(
        () -> assertStartsWith(start, actual),
        () -> assertContains("page=" + page, actual),
        () -> assertContains("pageSize=" + pageSize, actual));
  }
}
