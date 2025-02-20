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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertPagerLink;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonEnrollment;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.hisp.dhis.webapi.controller.tracker.JsonPage.JsonPager;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests how {@link org.hisp.dhis.webapi.controller.tracker.export} controllers serialize {@link
 * Page} to JSON.
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExportControllerPaginationTest extends PostgresControllerIntegrationTestBase {
  @Autowired private RenderService renderService;

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  private TrackerObjects trackerObjects;

  private Enrollment enrollment1;
  private Enrollment enrollment2;
  private Event event;

  protected ObjectBundle setUpMetadata(String path) throws IOException {
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(new ClassPathResource(path).getInputStream(), RenderFormat.JSON);
    ObjectBundleParams params = new ObjectBundleParams();
    params.setObjectBundleMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.setObjects(metadata);
    ObjectBundle bundle = objectBundleService.create(params);
    assertNoErrors(objectBundleValidationService.validate(bundle));
    objectBundleService.commit(bundle);
    return bundle;
  }

  protected TrackerObjects fromJson(String path) throws IOException {
    return renderService.fromJson(
        new ClassPathResource(path).getInputStream(), TrackerObjects.class);
  }

  @BeforeAll
  void setUp() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    TrackerImportParams params = TrackerImportParams.builder().build();
    trackerObjects = fromJson("tracker/event_and_enrollment.json");
    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));

    manager.flush();
    manager.clear();

    enrollment1 = getEnrollment(UID.of("nxP7UnKhomJ"));
    enrollment2 = getEnrollment(UID.of("nxP8UnKhomJ"));
    event = getEvent(UID.of("pTzf9KYMk72"));
  }

  @BeforeEach
  void setUpUser() {
    switchContextToUser(importUser);
  }

  @Test
  void shouldGetEmptyEnrollmentsPage() {
    JsonPage page =
        GET("/tracker/enrollments?enrollments={uid}", UID.generate())
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
    JsonPage page =
        GET(
                "/tracker/enrollments?enrollments={uid},{uid}",
                enrollment1.getUid(),
                enrollment2.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of(enrollment1.getUid().getValue(), enrollment2.getUid().getValue()),
        page.getList("enrollments", JsonEnrollment.class).toList(JsonEnrollment::getEnrollment));

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertHasNoMember(pager, "total");
    assertHasNoMember(pager, "pageCount");
  }

  // TODO(ivo) add a test for the first page
  // TODO(ivo) add a test for the middle page
  // TODO(ivo) add a test for the last page
  // TODO(ivo) add a test for being past the last page
  @Test
  void shouldGetPaginatedEnrollmentsWithNonDefaultsAndTotals() {
    JsonPage page =
        GET(
                "/tracker/enrollments?enrollments={uid},{uid}&page=2&pageSize=1&totalPages=true",
                enrollment1.getUid(),
                enrollment2.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertHasSize(
        1, page.getList("enrollments", JsonEnrollment.class).toList(JsonEnrollment::getEnrollment));

    JsonPager pager = page.getPager();
    assertEquals(2, pager.getPage());
    assertEquals(1, pager.getPageSize());
    assertEquals(2, pager.getTotal());
    assertEquals(2, pager.getPageCount());
    // TODO(ivo) what do I expect the URL to look like again?
    //    ### get page of attributes
    //    GET {{PROTOCOL}}://{{AUTH}}@{{HOST}}/api/attributes?fields=id,name&page=3&pageSize=2
    //    "pager": {
    //          "page": 3,
    //          "total": 11,
    //          "pageSize": 2,
    //          "nextPage":
    // "https://play.im.dhis2.org/dev/api/attributes?page=4&pageSize=2&fields=id%2Cname",
    //          "prevPage":
    // "https://play.im.dhis2.org/dev/api/attributes?page=2&pageSize=2&fields=id%2Cname",
    //          "pageCount": 6
    //    },

    assertPagerLink(
        pager.getPrevPage(),
        1,
        1,
        String.format(
            "http://localhost/api/tracker/enrollments?enrollments=%s,%s",
            enrollment1.getUid(), enrollment2.getUid()));
    assertHasNoMember(pager, "nextPage");
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
        GET("/tracker/relationships?event={uid}", event.getUid())
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
  void shouldGetPaginatedRelationshipsWithPagingSetToTrue() {
    JsonPage page =
        GET("/tracker/relationships?event={uid}&paging=true", event.getUid())
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
  void shouldGetPaginatedRelationshipsWithDefaultsAndTotals() {
    JsonPage page =
        GET("/tracker/relationships?event={uid}&paging=true&totalPages=true", event.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of("oLT07jKRu9e", "yZxjxJli9mO"),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertEquals(2, pager.getTotal());
    assertEquals(1, pager.getPageCount());
  }

  @Test
  void shouldGetPaginatedRelationshipsWithNonDefaults() {
    JsonPage page =
        GET("/tracker/relationships?event={uid}&paging=true&page=2&pageSize=1", event.getUid())
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
    assertHasNoMember(pager, "total");
    assertHasNoMember(pager, "pageCount");
  }

  @Test
  void shouldGetPaginatedRelationshipsWithNonDefaultsAndTotals() {
    JsonPage page =
        GET("/tracker/relationships?event={uid}&page=2&pageSize=1&totalPages=true", event.getUid())
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
        GET("/tracker/relationships?event={uid}&paging=false", event.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of("oLT07jKRu9e", "yZxjxJli9mO"),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));
    assertHasNoMember(page, "pager");
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

  public static void assertNoErrors(ImportReport report) {
    assertNotNull(report);
    assertEquals(
        Status.OK,
        report.getStatus(),
        errorMessage(
            "Expected import with status OK, instead got:%n", report.getValidationReport()));
  }

  private static Supplier<String> errorMessage(String errorTitle, ValidationReport report) {
    return () -> {
      StringBuilder msg = new StringBuilder(errorTitle);
      report
          .getErrors()
          .forEach(
              e -> {
                msg.append(e.getErrorCode());
                msg.append(": ");
                msg.append(e.getMessage());
                msg.append('\n');
              });
      return msg.toString();
    };
  }

  public static void assertNoErrors(ObjectBundleValidationReport report) {
    assertNotNull(report);
    List<String> errors = new ArrayList<>();
    report.forEachErrorReport(err -> errors.add(err.toString()));
    assertFalse(
        report.hasErrorReports(), String.format("Expected no errors, instead got: %s%n", errors));
  }
}
