/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.deduplication;

import static org.hisp.dhis.test.utils.Assertions.assertContains;
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
import java.util.Set;
import java.util.function.Supplier;
import org.hisp.dhis.common.IdentifiableObject;
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
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.deduplication.DeduplicationStatus;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicate;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.hisp.dhis.webapi.controller.tracker.JsonPage.JsonPager;
import org.hisp.dhis.webapi.controller.tracker.JsonTrackedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author luca@dhis2.org
 */
@Transactional
class DeduplicationControllerTest extends PostgresControllerIntegrationTestBase {
  @Autowired private RenderService renderService;

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private TrackerImportService trackerImportService;

  private TrackerObjects trackerObjects;

  private org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntityAOriginal;
  private org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntityADuplicate;
  private JsonPotentialDuplicate potentialDuplicateA;
  private org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntityBDuplicate;
  private JsonPotentialDuplicate potentialDuplicateB;
  private JsonPotentialDuplicate potentialDuplicateC;

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

  @BeforeEach
  void setUp() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    trackerObjects = fromJson("tracker/event_and_enrollment.json");
    assertNoErrors(
        trackerImportService.importTracker(TrackerImportParams.builder().build(), trackerObjects));

    TrackerObjects duplicateTrackedEntities =
        fromJson("tracker/deduplication/potential_duplicates.json");
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().build(), duplicateTrackedEntities));

    manager.flush();
    manager.clear();

    // potential duplicate A
    trackedEntityAOriginal = trackerObjects.findTrackedEntity(UID.of("QS6w44flWAf")).get();
    trackedEntityADuplicate =
        duplicateTrackedEntities.findTrackedEntity(UID.of("DS6w44flWAf")).get();
    potentialDuplicateA =
        POST(
                "/potentialDuplicates",
                """
        {
        "original": "%s",
        "duplicate": "%s"
        }
        """
                    .formatted(trackedEntityAOriginal.getUid(), trackedEntityADuplicate.getUid()))
            .content(HttpStatus.OK)
            .as(JsonPotentialDuplicate.class);
    // potential duplicate B
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntityBOriginal =
        trackerObjects.findTrackedEntity(UID.of("dUE514NMOlo")).get();
    trackedEntityBDuplicate =
        duplicateTrackedEntities.findTrackedEntity(UID.of("DUE514NMOlo")).get();
    potentialDuplicateB =
        POST(
                "/potentialDuplicates",
                """
        {
        "original": "%s",
        "duplicate": "%s"
        }
        """
                    .formatted(trackedEntityBOriginal.getUid(), trackedEntityBDuplicate.getUid()))
            .content(HttpStatus.OK)
            .as(JsonPotentialDuplicate.class);
    // potential duplicate C
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntityCOriginal =
        trackerObjects.findTrackedEntity(UID.of("mHWCacsGYYn")).get();
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntityCDuplicate =
        duplicateTrackedEntities.findTrackedEntity(UID.of("DHWCacsGYYn")).get();
    potentialDuplicateC =
        POST(
                "/potentialDuplicates",
                """
        {
        "original": "%s",
        "duplicate": "%s"
        }
        """
                    .formatted(trackedEntityCOriginal.getUid(), trackedEntityCDuplicate.getUid()))
            .content(HttpStatus.OK)
            .as(JsonPotentialDuplicate.class);
  }

  @Test
  void shouldThrowBadRequestExceptionWhenMissingDuplicate() {
    assertContains(
        "required input property 'duplicate'",
        POST(
                "/potentialDuplicates",
                """
{
  "original": "%s"
}
"""
                    .formatted(trackedEntityAOriginal.getUid()))
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldThrowBadRequestExceptionWhenMissingOrigin() {
    assertContains(
        "required input property 'original'",
        POST(
                "/potentialDuplicates",
                """
{
"duplicate": "%s"
}
"""
                    .formatted(trackedEntityAOriginal.getUid()))
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldThrowNotFoundExceptionWhenTeDoesNotExist() {
    POST(
            "/potentialDuplicates",
            """
{
  "original": "%s",
  "duplicate": "%s"
}
"""
                .formatted(trackedEntityAOriginal.getUid(), UID.generate()))
        .content(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldThrowForbiddenExceptionWhenPostAndUserHasNoTeAccess() {
    User user = userService.getUser("Z7870757a75");
    TrackedEntityType trackedEntityType = manager.get(TrackedEntityType.class, "ja8NY4PW7Xm");
    trackedEntityType.getSharing().setUserAccesses(Set.of());
    trackedEntityType.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(trackedEntityType, false);
    switchContextToUser(user);

    POST(
            "/potentialDuplicates",
            """
{
  "original": "%s",
  "duplicate": "%s"
}
"""
                .formatted(
                    trackedEntityAOriginal.getUid(),
                    trackerObjects.findTrackedEntity(UID.of("dUE514NMOlo")).get().getUid()))
        .content(HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldAutoMergePotentialDuplicateWhenUserHasAccessAndMergeIsOk() {
    POST("/potentialDuplicates/" + potentialDuplicateA.getUid() + "/merge", "{}")
        .content(HttpStatus.OK);
  }

  @Test
  void shouldManualMergePotentialDuplicateWhenUserHasAccessAndMergeIsOk() {
    POST(
            "/potentialDuplicates/" + potentialDuplicateA.getUid() + "/merge?mergeStrategy=MANUAL",
            "{}")
        .content(HttpStatus.OK);
  }

  @Test
  void shouldThrowForbiddenExceptionWhenAutoMergingAndUserHasNoAccessToTrackedEntityType() {
    User user = userService.getUser("Z7870757a75");
    TrackedEntityType trackedEntityType = manager.get(TrackedEntityType.class, "ja8NY4PW7Xm");
    trackedEntityType.getSharing().setUserAccesses(Set.of());
    trackedEntityType.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(trackedEntityType, false);
    switchContextToUser(user);

    POST("/potentialDuplicates/" + potentialDuplicateA.getUid() + "/merge", "{}")
        .content(HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldUpdatePotentialDuplicateWhenPotentialDuplicateExistsAndCorrectStatus() {
    PUT("/potentialDuplicates/" + potentialDuplicateA.getUid() + "?status=INVALID")
        .content(HttpStatus.OK);
  }

  @Test
  void shouldThrowBadRequestExceptionWhenPutPotentialDuplicateAlreadyMerged() {
    manager
        .get(PotentialDuplicate.class, potentialDuplicateA.getUid())
        .setStatus(DeduplicationStatus.MERGED);

    assertContains(
        "already MERGED",
        PUT("/potentialDuplicates/" + potentialDuplicateA.getUid() + "?status=INVALID")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldThrowBadRequestExceptionWhenPutPotentialDuplicateToMergedStatus() {
    assertContains(
        "update a potential duplicate to MERGED",
        PUT("/potentialDuplicates/" + potentialDuplicateA.getUid() + "?status=MERGED")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldGetPotentialDuplicateByIdWhenPotentialDuplicateExists() {
    JsonPotentialDuplicate json =
        GET("/potentialDuplicates/{uid}", potentialDuplicateA.getUid())
            .content(HttpStatus.OK)
            .as(JsonPotentialDuplicate.class);

    assertEquals(json.getUid(), potentialDuplicateA.getUid());
    assertEquals(json.getStatus(), potentialDuplicateA.getStatus());
    assertEquals(json.getOriginal(), potentialDuplicateA.getOriginal());
    assertEquals(json.getDuplicate(), potentialDuplicateA.getDuplicate());
    assertNotNull(json.getCreated());
    assertNotNull(json.getLastUpdated());
  }

  @Test
  void shouldThrowNotFoundExceptionWhenPotentialDuplicateDoesNotExist() {
    GET("/potentialDuplicates/{uid}", UID.generate()).content(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldGetEmptyPage() {
    JsonPage page =
        GET("/potentialDuplicates?trackedEntities={uid}", UID.generate())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertIsEmpty(
        page.getList("potentialDuplicates", JsonPotentialDuplicate.class).stream().toList());

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertHasNoMember(pager, "total", "pageCount", "prevPage", "nextPage");
  }

  @Test
  void shouldGetPageWithDefaults() {
    JsonPage page =
        GET(
                "/potentialDuplicates?trackedEntities={uid},{uid}",
                trackedEntityADuplicate.getUid(),
                trackedEntityBDuplicate.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of(potentialDuplicateA.getUid(), potentialDuplicateB.getUid()),
        page.getList("potentialDuplicates", JsonPotentialDuplicate.class)
            .toList(JsonPotentialDuplicate::getUid));

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertHasNoMember(pager, "total", "pageCount");
  }

  @Test
  void shouldGetLastPage() {
    JsonPage page =
        GET(
                "/potentialDuplicates?trackedEntities={uid},{uid}&page=2&pageSize=1",
                trackedEntityADuplicate.getUid(),
                trackedEntityBDuplicate.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertHasSize(
        1,
        page.getList("potentialDuplicates", JsonTrackedEntity.class)
            .toList(JsonTrackedEntity::getTrackedEntity));

    JsonPager pager = page.getPager();
    assertEquals(2, pager.getPage());
    assertEquals(1, pager.getPageSize());
    assertPagerLink(
        pager.getPrevPage(),
        1,
        1,
        String.format(
            "http://localhost/api/potentialDuplicates?trackedEntities=%s,%s",
            trackedEntityADuplicate.getUid(), trackedEntityBDuplicate.getUid()));
    assertHasNoMember(pager, "total", "pageCount", "nextPage");
  }

  @Test
  void shouldGetNonPaginatedItemsWithPagingSetToFalse() {
    JsonPage page =
        GET("/potentialDuplicates?paging=false").content(HttpStatus.OK).asA(JsonPage.class);

    JsonList<JsonPotentialDuplicate> list =
        page.getList("potentialDuplicates", JsonPotentialDuplicate.class);
    assertContainsOnly(
        List.of(
            potentialDuplicateA.getUid(),
            potentialDuplicateB.getUid(),
            potentialDuplicateC.getUid()),
        list.toList(JsonPotentialDuplicate::getUid));
    assertHasNoMember(page, "pager");
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
