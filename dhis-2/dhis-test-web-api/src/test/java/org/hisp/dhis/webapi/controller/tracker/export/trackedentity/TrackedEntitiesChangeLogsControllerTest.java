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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
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

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  private TrackedEntity trackedEntity;

  private TrackedEntityAttribute trackedEntityAttribute;

  @BeforeEach
  void setUp() throws IOException {
    testSetup.importMetadata();

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData("tracker/single_te.json");

    trackedEntity = manager.get(TrackedEntity.class, "IOR1AXXl24H");

    JsonWebMessage importResponse =
        POST("/tracker?async=false&importStrategy=UPDATE", createJsonPayload("numericAttr", 2))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());

    importResponse =
        POST("/tracker?async=false&importStrategy=UPDATE", createJsonPayload("numericAttr", 3))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());

    importResponse =
        POST("/tracker?async=false&importStrategy=UPDATE", createJsonPayload("numericAttr", 4))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());

    trackedEntityAttribute = trackedEntityAttributeService.getTrackedEntityAttribute("numericAttr");
  }

  @Test
  void shouldGetTrackedEntityChangeLogInDescOrderByDefault() {
    JsonList<JsonTrackedEntityChangeLog> changeLogs =
        GET("/tracker/trackedEntities/{id}/changeLogs", trackedEntity.getUid())
            .content(HttpStatus.OK)
            .getList("changeLogs", JsonTrackedEntityChangeLog.class);

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertUpdate(trackedEntityAttribute.getUid(), "3", "4", changeLogs.get(0)),
        () -> assertUpdate(trackedEntityAttribute.getUid(), "2", "3", changeLogs.get(1)),
        () -> assertCreate(trackedEntityAttribute.getUid(), "2", changeLogs.get(2)));
  }

  @Test
  void shouldGetTrackedEntityChangeLogInAscOrder() {
    JsonList<JsonTrackedEntityChangeLog> changeLogs =
        GET("/tracker/trackedEntities/{id}/changeLogs?order=createdAt:asc", trackedEntity.getUid())
            .content(HttpStatus.OK)
            .getList("changeLogs", JsonTrackedEntityChangeLog.class);

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertCreate(trackedEntityAttribute.getUid(), "2", changeLogs.get(0)),
        () -> assertUpdate(trackedEntityAttribute.getUid(), "2", "3", changeLogs.get(1)),
        () -> assertUpdate(trackedEntityAttribute.getUid(), "3", "4", changeLogs.get(2)));
  }

  @Test
  void shouldGetEventChangeLogsWhenFilteringByAttribute() {
    JsonWebMessage importResponse =
        POST("/tracker?async=false&importStrategy=UPDATE", createJsonPayload("toUpdate000", 10))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());

    JsonList<JsonTrackedEntityChangeLog> changeLogs =
        GET(
                "/tracker/trackedEntities/{id}/changeLogs?filter=attribute:eq:toUpdate000",
                trackedEntity.getUid())
            .content(HttpStatus.OK)
            .getList("changeLogs", JsonTrackedEntityChangeLog.class);

    assertNumberOfChanges(1, changeLogs);
    assertAll(() -> assertCreate("toUpdate000", "10", changeLogs.get(0)));
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
                "3")
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonPager pagerObject = changeLogs.getPager();
    assertAll(
        () -> assertEquals(1, pagerObject.getPage()),
        () -> assertEquals(3, pagerObject.getPageSize()),
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

  private String createJsonPayload(String attribute, int value) {
    return """
           {
             "trackedEntities": [
               {
                 "attributes": [
                   {
                     "attribute": "%s",
                     "value": %d
                   }
                 ],
                 "trackedEntity": "IOR1AXXl24H",
                 "trackedEntityType": "ja8NY4PW7Xm",
                 "orgUnit": "h4w96yEMlzO"
               }
             ]
           }
           """
        .formatted(attribute, value);
  }

  private static void assertPagerLink(String actual, int page, int pageSize, String start) {
    assertNotNull(actual);
    assertAll(
        () -> assertStartsWith(start, actual),
        () -> assertContains("page=" + page, actual),
        () -> assertContains("pageSize=" + pageSize, actual));
  }

  private static void assertNumberOfChanges(
      int expected, JsonList<JsonTrackedEntityChangeLog> changeLogs) {
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
