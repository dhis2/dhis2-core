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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.webapi.Assertions.assertNoDiff;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertPagerLink;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonEnrollment;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.hisp.dhis.webapi.controller.tracker.JsonPage.JsonPager;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.hisp.dhis.webapi.controller.tracker.JsonTrackedEntity;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests how {@link org.hisp.dhis.webapi.controller.tracker.export} controllers serialize {@link
 * Page} to JSON. The logic and actual items returned in the pages is tested in the {@code
 * OrderAndPaginationExporterTest}. Each controller has at least one or two tests. A couple more
 * combinations are tested via the relationships controller which are also testing logic used by
 * other controllers.
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExportControllerPaginationTest extends PostgresControllerIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  private ObjectBundle objectBundle;
  private TrackerObjects trackerObjects;

  private TrackedEntity trackedEntity1;
  private TrackedEntity trackedEntity2;
  private Enrollment enrollment1;
  private Enrollment enrollment2;
  private Event event1;
  private Event event2;
  private final String program = "BFcipDERJnf";

  @BeforeAll
  void setUp() throws IOException {
    objectBundle = testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    trackerObjects = testSetup.importTrackerData();

    manager.flush();
    manager.clear();

    trackedEntity1 = getTrackedEntity(UID.of("QS6w44flWAf"));
    trackedEntity2 = getTrackedEntity(UID.of("dUE514NMOlo"));
    enrollment1 = getEnrollment(UID.of("nxP8UnKhomJ"));
    enrollment2 = getEnrollment(UID.of("JuioKiICQqI"));
    event1 = getEvent(UID.of("pTzf9KYMk72"));
    event2 = getEvent(UID.of("D9PbzJY8bJM"));
  }

  @BeforeEach
  void setUpUser() {
    switchContextToUser(importUser);
  }

  @Test
  void shouldGetEmptyTrackedEntitiesPage() {
    JsonPage page =
        GET("/tracker/trackedEntities?trackedEntities={uid}", UID.generate())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertIsEmpty(page.getList("trackedEntities", JsonTrackedEntity.class).stream().toList());

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertHasNoMember(pager, "total", "pageCount", "prevPage", "nextPage");
  }

  @Test
  void shouldGetPaginatedTrackedEntitiesWithDefaults() {
    JsonPage page =
        GET(
                "/tracker/trackedEntities?trackedEntities={uid},{uid}",
                trackedEntity1.getUid(),
                trackedEntity2.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of(trackedEntity1.getUid().getValue(), trackedEntity2.getUid().getValue()),
        page.getList("trackedEntities", JsonTrackedEntity.class)
            .toList(JsonTrackedEntity::getTrackedEntity));

    assertNoDiff(
        """
        { "page": 1, "pageSize": 50 }""",
        page.getPager());
  }

  @Test
  void shouldGetPaginatedTrackedEntitiesLastPage() {
    JsonPage page =
        GET(
                "/tracker/trackedEntities?trackedEntities={uid},{uid}&page=2&pageSize=1&totalPages=true",
                trackedEntity1.getUid(),
                trackedEntity2.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertHasSize(
        1,
        page.getList("trackedEntities", JsonTrackedEntity.class)
            .toList(JsonTrackedEntity::getTrackedEntity));

    JsonPager pager = page.getPager();
    assertEquals(2, pager.getPage());
    assertEquals(1, pager.getPageSize());
    assertEquals(2, pager.getTotal());
    assertEquals(2, pager.getPageCount());
    assertPagerLink(
        pager.getPrevPage(),
        1,
        1,
        String.format(
            "http://localhost/api/tracker/trackedEntities?trackedEntities=%s,%s",
            trackedEntity1.getUid(), trackedEntity2.getUid()));
    assertHasNoMember(pager, "nextPage");
  }

  @Test
  void shouldGetEmptyEnrollmentsPage() {
    Program programWithoutEnrollments = getProgram(UID.of("TsngICFQjvP"));
    JsonPage page =
        GET("/tracker/enrollments?program={uid}", programWithoutEnrollments.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertIsEmpty(page.getList("enrollments", JsonEnrollment.class).stream().toList());

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertHasNoMember(pager, "total", "pageCount", "prevPage", "nextPage");
  }

  @Test
  void shouldGetPaginatedEnrollmentsWithDefaults() {
    Program program = getProgram(UID.of("shPjYNifvMK"));
    JsonPage page =
        GET("/tracker/enrollments?program={uid}", program.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of(enrollment1.getUid().getValue(), enrollment2.getUid().getValue()),
        page.getList("enrollments", JsonEnrollment.class).toList(JsonEnrollment::getEnrollment));

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertHasNoMember(pager, "total", "pageCount");
  }

  @Test
  void shouldGetPaginatedEnrollmentsMiddlePage() {
    JsonPage page =
        GET("/tracker/enrollments?program={uid}&page=2&pageSize=1&totalPages=true", program)
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertHasSize(
        1, page.getList("enrollments", JsonEnrollment.class).toList(JsonEnrollment::getEnrollment));

    JsonPager pager = page.getPager();
    assertEquals(2, pager.getPage());
    assertEquals(1, pager.getPageSize());
    assertEquals(3, pager.getTotal());
    assertEquals(3, pager.getPageCount());
    assertPagerLink(
        pager.getPrevPage(),
        1,
        1,
        String.format("http://localhost/api/tracker/enrollments?program=%s", program));
    assertPagerLink(
        pager.getNextPage(),
        3,
        1,
        String.format("http://localhost/api/tracker/enrollments?program=%s", program));
  }

  @Test
  void shouldGetEmptyEventsPage() {
    JsonPage page =
        GET("/tracker/events?events={uid}&program={programUid}", UID.generate(), program)
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertIsEmpty(page.getList("events", JsonEvent.class).stream().toList());

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertHasNoMember(pager, "total", "pageCount", "prevPage", "nextPage");
  }

  @Test
  void shouldGetPaginatedEventsWithDefaults() {
    JsonPage page =
        GET(
                "/tracker/events?program={programUid}&events={uid},{uid}",
                program,
                event1.getUid(),
                event2.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of(event1.getUid().getValue(), event2.getUid().getValue()),
        page.getList("events", JsonEvent.class).toList(JsonEvent::getEvent));

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertHasNoMember(pager, "total", "pageCount");
  }

  @Test
  void shouldGetPaginatedEventsFirstPage() {
    JsonPage page =
        GET(
                "/tracker/events?events={uid},{uid}&program={programUid}&page=1&pageSize=1&totalPages=true",
                event1.getUid(),
                event2.getUid(),
                program)
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertHasSize(1, page.getList("events", JsonEvent.class).toList(JsonEvent::getEvent));

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(1, pager.getPageSize());
    assertEquals(2, pager.getTotal());
    assertEquals(2, pager.getPageCount());
    assertHasNoMember(pager, "prevPage");
    assertPagerLink(
        pager.getNextPage(),
        2,
        1,
        String.format(
            "http://localhost/api/tracker/events?events=%s,%s", event1.getUid(), event2.getUid()));
  }

  @Test
  void shouldGetEmptyRelationshipsPage() {
    JsonPage page =
        GET("/tracker/relationships?event={uid}", "H0PbzJY8bJG")
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertIsEmpty(page.getList("relationships", JsonEnrollment.class).stream().toList());

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertHasNoMember(pager, "total", "pageCount", "prevPage", "nextPage");
  }

  @Test
  void shouldGetPaginatedRelationshipsWithDefaults() {
    JsonPage page =
        GET("/tracker/relationships?event={uid}", event1.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of("oLT07jKRu9e", "yZxjxJli9mO"),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertHasNoMember(pager, "total", "pageCount");
  }

  @Test
  void shouldGetPaginatedRelationshipsWithPagingSetToTrue() {
    JsonPage page =
        GET("/tracker/relationships?event={uid}&paging=true", event1.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of("oLT07jKRu9e", "yZxjxJli9mO"),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertHasNoMember(pager, "total");
    assertHasNoMember(pager, "pageCount");
  }

  @Test
  void shouldGetPaginatedRelationshipsLastPage() {
    JsonPage page =
        GET(
                "/tracker/relationships?event={uid}&paging=true&page=2&pageSize=1&totalPages=true",
                event1.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertHasSize(
        1,
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));

    JsonPager pager = page.getPager();
    assertEquals(2, pager.getPage());
    assertEquals(1, pager.getPageSize());
    assertEquals(2, pager.getTotal());
    assertEquals(2, pager.getPageCount());
    assertPagerLink(
        pager.getPrevPage(),
        1,
        1,
        String.format("http://localhost/api/tracker/relationships?event=%s", event1.getUid()));
    assertHasNoMember(pager, "nextPage");
  }

  @Test
  void shouldGetPaginatedRelationshipsWithNonDefaultsAndTotals() {
    JsonPage page =
        GET("/tracker/relationships?event={uid}&page=2&pageSize=1&totalPages=true", event1.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonRelationship> relationships =
        page.getList("relationships", JsonRelationship.class);
    assertEquals(
        1,
        relationships.size(),
        () ->
            String.format("mismatch in number of expected relationship(s), got %s", relationships));

    JsonPager pager = page.getPager();
    assertEquals(2, pager.getPage());
    assertEquals(1, pager.getPageSize());
    assertEquals(2, pager.getTotal());
    assertEquals(2, pager.getPageCount());
  }

  @Test
  void shouldGetNonPaginatedItemsWithPagingSetToFalse() {
    JsonPage page =
        GET("/tracker/relationships?event={uid}&paging=false", event1.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of("oLT07jKRu9e", "yZxjxJli9mO"),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));
    assertHasNoMember(page, "pager");
  }

  private org.hisp.dhis.tracker.imports.domain.TrackedEntity getTrackedEntity(UID trackedEntity) {
    return trackerObjects.getTrackedEntities().stream()
        .filter(ev -> ev.getTrackedEntity().equals(trackedEntity))
        .findFirst()
        .get();
  }

  private org.hisp.dhis.tracker.imports.domain.Enrollment getEnrollment(UID enrollment) {
    return trackerObjects.getEnrollments().stream()
        .filter(ev -> ev.getEnrollment().equals(enrollment))
        .findFirst()
        .get();
  }

  private Event getEvent(UID event) {
    return trackerObjects.getEvents().stream()
        .filter(ev -> ev.getEvent().equals(event))
        .findFirst()
        .get();
  }

  private Program getProgram(UID program) {
    return objectBundle.getPreheat().get(PreheatIdentifier.UID, Program.class, program.getValue());
  }
}
