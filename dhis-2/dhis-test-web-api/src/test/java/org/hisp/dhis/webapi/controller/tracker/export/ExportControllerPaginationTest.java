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
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
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
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests how {@link org.hisp.dhis.webapi.controller.tracker.export} controllers serialize {@link
 * Page} to JSON. The tests use the {@link
 * org.hisp.dhis.webapi.controller.tracker.export.relationship} controller but hold true for any of
 * the export controllers.
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

  private org.hisp.dhis.tracker.imports.domain.Event event;

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

    event = getEvent(UID.of("pTzf9KYMk72"));
  }

  @Test
  void shouldGetPaginatedItemsWithDefaults() {
    JsonPage page =
        GET("/tracker/relationships?event={uid}", event.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of("oLT07jKRu9e", "yZxjxJli9mO"),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));

    // TODO(jan) I would like to assert that the JsonPage has this expected pager. The assertion
    // should ideally fail with a diff so I don't need to debug to see the actual JSON. I could also
    // imagine asserting on the result of page.getPager() if that is easier like
    // assertJSONEquals(expected, page.getPager()). Whitespace and order of keys do not matter here.
    // I also see a case where both actual and expected are String
    // representations of some JSON (but that's maybe for later).
    String expected =
        """
"pager" : {
  "page" : 1,
  "pageSize" : 50
}
""";
    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertHasNoMember(page.getPager(), "total");
    assertHasNoMember(page.getPager(), "pageCount");
  }

  @Test
  void shouldGetPaginatedItemsWithPagingSetToTrue() {
    JsonPage page =
        GET("/tracker/relationships?event={uid}&paging=true", event.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of("oLT07jKRu9e", "yZxjxJli9mO"),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));
    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertHasNoMember(page.getPager(), "total");
    assertHasNoMember(page.getPager(), "pageCount");
  }

  @Test
  void shouldGetPaginatedItemsWithDefaultsAndTotals() {
    JsonPage page =
        GET("/tracker/relationships?event={uid}&paging=true&totalPages=true", event.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of("oLT07jKRu9e", "yZxjxJli9mO"),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));
    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertEquals(2, page.getPager().getTotal());
    assertEquals(1, page.getPager().getPageCount());
  }

  @Test
  void shouldGetPaginatedItemsWithNonDefaults() {
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
    assertEquals(2, page.getPager().getPage());
    assertEquals(1, page.getPager().getPageSize());
    assertHasNoMember(page.getPager(), "total");
    assertHasNoMember(page.getPager(), "pageCount");
  }

  @Test
  void shouldGetPaginatedItemsWithNonDefaultsAndTotals() {
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
    assertEquals(2, page.getPager().getPage());
    assertEquals(1, page.getPager().getPageSize());
    assertEquals(2, page.getPager().getTotal());
    assertEquals(2, page.getPager().getPageCount());
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

  private org.hisp.dhis.tracker.imports.domain.Event getEvent(UID event) {
    return trackerObjects.getEvents().stream()
        .filter(ev -> ev.getEvent().equals(event))
        .findFirst()
        .get();
  }

  private org.hisp.dhis.tracker.imports.domain.Relationship getRelationship(UID relationship) {
    return trackerObjects.getRelationships().stream()
        .filter(r -> r.getRelationship().equals(relationship))
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
