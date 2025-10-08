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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.hisp.dhis.webapi.controller.tracker.JsonPage.JsonPager;
import org.hisp.dhis.webapi.controller.tracker.JsonTrackedEntityChangeLog;
import org.hisp.dhis.webapi.controller.tracker.JsonUser;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrackedEntitiesChangeLogsControllerTest extends PostgresControllerIntegrationTestBase {
  @Autowired private TestSetup testSetup;
  @Autowired private IdentifiableObjectManager manager;

  private final String trackedEntityAttribute = "integerAttr";
  private TrackedEntity trackedEntity;

  @BeforeEach
  void setUp() throws IOException {
    testSetup.importMetadata();

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData();

    trackedEntity = manager.get(TrackedEntity.class, "QS6w44flWAf");

    updateAttribute(trackedEntityAttribute, "2");
    updateAttribute(trackedEntityAttribute, "3");
    updateAttribute(trackedEntityAttribute, "4");
  }

  @Test
  void shouldGetTrackedEntityChangeLogInDescOrderByDefault() {
    List<JsonTrackedEntityChangeLog> changeLogs = getChangeLogs(trackedEntityAttribute);

    assertNumberOfChanges(4, changeLogs);
    assertAll(
        () -> assertUpdate(trackedEntityAttribute, "3", "4", changeLogs.get(0)),
        () -> assertUpdate(trackedEntityAttribute, "2", "3", changeLogs.get(1)),
        () -> assertUpdate(trackedEntityAttribute, "88", "2", changeLogs.get(2)),
        () -> assertCreate(trackedEntityAttribute, "88", changeLogs.get(3)));
  }

  @Test
  void shouldGetTrackedEntityChangeLogInAscOrder() {
    List<JsonTrackedEntityChangeLog> changeLogs =
        getChangeLogs("order=createdAt:asc", trackedEntityAttribute, trackedEntity);

    assertNumberOfChanges(4, changeLogs);
    assertAll(
        () -> assertCreate(trackedEntityAttribute, "88", changeLogs.get(0)),
        () -> assertUpdate(trackedEntityAttribute, "88", "2", changeLogs.get(1)),
        () -> assertUpdate(trackedEntityAttribute, "2", "3", changeLogs.get(2)),
        () -> assertUpdate(trackedEntityAttribute, "3", "4", changeLogs.get(3)));
  }

  @Test
  void shouldGetTrackedEntityChangeLogsWhenFilteringByAttribute() {
    String trackedEntityAttribute = "toUpdate000";
    updateAttribute(trackedEntityAttribute, "10");

    List<JsonTrackedEntityChangeLog> changeLogs =
        getChangeLogs("filter=attribute:eq:toUpdate000", trackedEntityAttribute, trackedEntity);

    assertNumberOfChanges(2, changeLogs);
    assertAll(
        () -> assertUpdate(trackedEntityAttribute, "summer day", "10", changeLogs.get(0)),
        () -> assertCreate(trackedEntityAttribute, "summer day", changeLogs.get(1)));
  }

  @Test
  void
      shouldGetChangeLogPagerWithNextAttributeWhenMultipleAttributesImportedAndFirstPageRequested() {
    JsonPage changeLogs =
        GET(
                "40/tracker/trackedEntities/{id}/changeLogs?page={page}&pageSize={pageSize}",
                trackedEntity.getUid(),
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
                    "http://localhost/api/40/tracker/trackedEntities/%s/changeLogs",
                    trackedEntity.getUid())));
  }

  @Test
  void
      shouldGetChangeLogPagerWithNextAndPreviousAttributesWhenMultipleAttributesImportedAndSecondPageRequested() {
    JsonPage changeLogs =
        GET(
                "/tracker/trackedEntities/{id}/changeLogs?page={page}&pageSize={pageSize}",
                trackedEntity.getUid(),
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
                String.format(
                    "http://localhost/api/tracker/trackedEntities/%s/changeLogs",
                    trackedEntity.getUid())),
        () ->
            assertPagerLink(
                pager.getNextPage(),
                3,
                1,
                String.format(
                    "http://localhost/api/tracker/trackedEntities/%s/changeLogs",
                    trackedEntity.getUid())));
  }

  @Test
  void
      shouldGetChangeLogPagerWithPreviousAttributeWhenMultipleAttributesImportedAndLastPageRequested() {
    JsonPage changeLogs =
        GET(
                "/tracker/trackedEntities/{id}/changeLogs?page={page}&pageSize={pageSize}",
                trackedEntity.getUid(),
                "6",
                "1")
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonPager pager = changeLogs.getPager();
    assertAll(
        () -> assertEquals(6, pager.getPage()),
        () -> assertEquals(1, pager.getPageSize()),
        () ->
            assertPagerLink(
                pager.getPrevPage(),
                5,
                1,
                String.format(
                    "http://localhost/api/tracker/trackedEntities/%s/changeLogs",
                    trackedEntity.getUid())),
        () -> assertHasNoMember(pager, "nextPage", "total", "pageCount"));
  }

  @Test
  void
      shouldGetChangeLogPagerWithoutPreviousNorNextAttributeWhenMultipleAttributesImportedAndAllAttributesFitInOnePage() {
    JsonPage changeLogs =
        GET(
                "/tracker/trackedEntities/{id}/changeLogs?page={page}&pageSize={pageSize}",
                trackedEntity.getUid(),
                "1",
                "6")
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonPager pagerObject = changeLogs.getPager();
    assertAll(
        () -> assertEquals(1, pagerObject.getPage()),
        () -> assertEquals(6, pagerObject.getPageSize()),
        () -> assertHasNoMember(pagerObject, "prevPage", "nextPage", "total", "pageCount"));
  }

  @Test
  void shouldIgnoreTotalPages() {
    JsonPage page =
        GET("/tracker/trackedEntities/{id}/changeLogs?totalPages=true", trackedEntity.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonPager pager = page.getPager();
    assertHasNoMember(pager, "total", "pageCount");
  }

  @Test
  void shouldAlwaysReturnPages() {
    JsonPage page =
        GET("/tracker/trackedEntities/{id}/changeLogs?paging=false", trackedEntity.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
  }

  @Test
  void shouldNotLogChangesWhenChangeLogConfigDisabled() {
    TrackedEntityType trackedEntityType = manager.get(TrackedEntityType.class, "ja8NY4PW7Xm");
    trackedEntityType.setAllowChangeLog(false);
    manager.update(trackedEntityType);

    String trackedEntityAttribute = "dIVt4l5vIOa";
    updateAttribute(trackedEntityAttribute, "10");
    updateAttribute(trackedEntityAttribute, "5");
    deleteAttribute(trackedEntityAttribute);

    List<JsonTrackedEntityChangeLog> changeLogs = getChangeLogs(trackedEntityAttribute);

    assertNumberOfChanges(0, changeLogs);
  }

  @Test
  void shouldGetTrackedEntityChangeLogsWithSimpleFieldsFilter() {
    JsonList<JsonTrackedEntityChangeLog> changeLogs =
        GET("/tracker/trackedEntities/{id}/changeLogs?fields=:simple", trackedEntity.getUid())
            .content(HttpStatus.OK)
            .getList("changeLogs", JsonTrackedEntityChangeLog.class);

    assertFalse(changeLogs.isEmpty(), "should have some change logs");
    assertHasOnlyMembers(changeLogs.get(0), "createdAt", "type");
  }

  @Test
  void shouldLogChangesWhenTrackedEntityTypeAuditConfigDisabled() {
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, "XUitxQbWYNz");
    updateAttribute(trackedEntityAttribute, "10", trackedEntity);
    updateAttribute(trackedEntityAttribute, "5", trackedEntity);
    deleteAttribute(trackedEntityAttribute, trackedEntity);

    List<JsonTrackedEntityChangeLog> changeLogs =
        getChangeLogs(trackedEntityAttribute, trackedEntity);

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertDelete(trackedEntityAttribute, "5", changeLogs.get(0)),
        () -> assertUpdate(trackedEntityAttribute, "10", "5", changeLogs.get(1)),
        () -> assertCreate(trackedEntityAttribute, "10", changeLogs.get(2)));
  }

  private void updateAttribute(String attribute, String value) {
    updateAttribute(attribute, value, trackedEntity);
  }

  private void updateAttribute(String attribute, String value, TrackedEntity trackedEntity) {
    JsonWebMessage importResponse =
        POST("/tracker?async=false", createUpdateJsonPayload(attribute, value, trackedEntity))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());
  }

  private void deleteAttribute(String attribute) {
    JsonWebMessage importResponse =
        POST("/tracker?async=false", createDeleteJsonPayload(attribute, trackedEntity))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());
  }

  private void deleteAttribute(String attribute, TrackedEntity trackedEntity) {
    JsonWebMessage importResponse =
        POST("/tracker?async=false", createDeleteJsonPayload(attribute, trackedEntity))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());
  }

  private String createUpdateJsonPayload(
      String attribute, String value, TrackedEntity trackedEntity) {
    return """
           {
             "trackedEntities": [
               {
                 "attributes": [
                   {
                     "attribute": "%s",
                     "value": "%s"
                   }
                 ],
                 "trackedEntity": "%s",
                 "trackedEntityType": "%s",
                 "orgUnit": "%s"
               }
             ]
           }
           """
        .formatted(
            attribute,
            value,
            trackedEntity.getUid(),
            trackedEntity.getTrackedEntityType().getUid(),
            trackedEntity.getOrganisationUnit().getUid());
  }

  private String createDeleteJsonPayload(String attribute, TrackedEntity trackedEntity) {
    return """
           {
             "trackedEntities": [
               {
                 "attributes": [
                   {
                     "attribute": "%s",
                     "value": null
                   }
                 ],
                 "trackedEntity": "%s",
                 "trackedEntityType": "%s",
                 "orgUnit": "%s"
               }
             ]
           }
           """
        .formatted(
            attribute,
            trackedEntity.getUid(),
            trackedEntity.getTrackedEntityType().getUid(),
            trackedEntity.getOrganisationUnit().getUid());
  }

  private List<JsonTrackedEntityChangeLog> getChangeLogs(String attribute) {
    return getChangeLogs("", attribute, trackedEntity);
  }

  private List<JsonTrackedEntityChangeLog> getChangeLogs(
      String attribute, TrackedEntity trackedEntity) {
    return getChangeLogs("", attribute, trackedEntity);
  }

  private List<JsonTrackedEntityChangeLog> getChangeLogs(
      String requestParams, String attribute, TrackedEntity trackedEntity) {
    return GET("/tracker/trackedEntities/{id}/changeLogs?" + requestParams, trackedEntity.getUid())
        .content(HttpStatus.OK)
        .getList("changeLogs", JsonTrackedEntityChangeLog.class)
        .stream()
        .filter(cl -> cl.getChange().getAttributeValue().getAttribute().equalsIgnoreCase(attribute))
        .toList();
  }

  private static void assertPagerLink(String actual, int page, int pageSize, String start) {
    assertNotNull(actual);
    assertAll(
        () -> assertStartsWith(start, actual),
        () -> assertContains("page=" + page, actual),
        () -> assertContains("pageSize=" + pageSize, actual));
  }

  private static void assertNumberOfChanges(
      int expected, List<JsonTrackedEntityChangeLog> changeLogs) {
    assertNotNull(changeLogs);
    assertEquals(
        expected,
        changeLogs.size(),
        String.format(
            "Expected to find %s elements in the change log list, found %s instead: %s",
            expected, changeLogs.size(), changeLogs));
  }

  private static void assertUser(JsonTrackedEntityChangeLog changeLog) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    JsonUser createdBy = changeLog.getCreatedBy();
    assertAll(
        () -> assertEquals(currentUser.getUid(), createdBy.getUid()),
        () -> assertEquals(currentUser.getUsername(), createdBy.getUsername()),
        () -> assertEquals(currentUser.getFirstName(), createdBy.getFirstName()),
        () -> assertEquals(currentUser.getSurname(), createdBy.getSurname()));
  }

  private static void assertCreate(
      String attribute, String currentValue, JsonTrackedEntityChangeLog actual) {
    assertAll(
        () -> assertUser(actual),
        () -> assertEquals("CREATE", actual.getType()),
        () -> assertChange(attribute, null, currentValue, actual));
  }

  private static void assertUpdate(
      String attribute,
      String previousValue,
      String currentValue,
      JsonTrackedEntityChangeLog actual) {
    assertAll(
        () -> assertUser(actual),
        () -> assertEquals("UPDATE", actual.getType()),
        () -> assertChange(attribute, previousValue, currentValue, actual));
  }

  private static void assertDelete(
      String attribute, String previousValue, JsonTrackedEntityChangeLog actual) {
    assertAll(
        () -> assertUser(actual),
        () -> assertEquals("DELETE", actual.getType()),
        () -> assertChange(attribute, previousValue, null, actual));
  }

  private static void assertChange(
      String attribute,
      String previousValue,
      String currentValue,
      JsonTrackedEntityChangeLog actual) {
    assertAll(
        () -> assertEquals(attribute, actual.getChange().getAttributeValue().getAttribute()),
        () ->
            assertEquals(previousValue, actual.getChange().getAttributeValue().getPreviousValue()),
        () -> assertEquals(currentValue, actual.getChange().getAttributeValue().getCurrentValue()));
  }
}
