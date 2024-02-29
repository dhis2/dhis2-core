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

import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.hisp.dhis.webapi.controller.tracker.JsonPage.JsonPager;
import org.hisp.dhis.webapi.controller.tracker.JsonTrackedEntityChangeLog;
import org.hisp.dhis.webapi.controller.tracker.JsonTrackedEntityChangeLog.JsonAttributeValue;
import org.hisp.dhis.webapi.controller.tracker.JsonUser;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

class TrackedEntitiesExportControllerPostgresTest extends DhisControllerIntegrationTest {

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private RenderService _renderService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private ProgramService programService;

  private Program program;

  private TrackedEntity trackedEntity;

  @BeforeEach
  void setUp() throws IOException {
    this.renderService = _renderService;
    setUpMetadata("tracker/simple_metadata.json");

    TrackerImportParams params = TrackerImportParams.builder().userId(superUser.getUid()).build();
    assertNoDataErrors(
        trackerImportService.importTracker(params, fromJson("tracker/single_tei.json")));

    trackedEntity = trackedEntityService.getTrackedEntity("IOR1AXXl24H");
    program = programService.getProgram("BFcipDERJnf");

    JsonWebMessage importResponse =
        POST(
                "/tracker?async=false&importStrategy=UPDATE",
              createJsonPayload(2))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());

    importResponse =
        POST(
            "/tracker?async=false&importStrategy=UPDATE",
            createJsonPayload(3))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());

    importResponse =
        POST(
            "/tracker?async=false&importStrategy=UPDATE",
            createJsonPayload(4))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());
  }

  @Test
  void shouldGetTrackedEntityAttributeChangeLogWhenValueUpdatedAndThenDeleted() {
    JsonList<JsonTrackedEntityChangeLog> changeLogs =
        GET(
                "/tracker/trackedEntities/{id}/changeLogs?program={programUid}",
                trackedEntity.getUid(),
                program.getUid())
            .content(HttpStatus.OK)
            .getList("changeLogs", JsonTrackedEntityChangeLog.class);

    JsonTrackedEntityChangeLog changeLog = changeLogs.get(2);
    JsonUser createdBy = changeLog.getCreatedBy();
    JsonAttributeValue attributeChange =
        changeLog.getChange().getAttributeValue();
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();

    assertAll(
        () -> assertEquals(currentUser.getUid(), createdBy.getUid()),
        () -> assertEquals(currentUser.getUsername(), createdBy.getUsername()),
        () -> assertEquals(currentUser.getFirstName(), createdBy.getFirstName()),
        () -> assertEquals(currentUser.getSurname(), createdBy.getSurname()),
        () -> assertEquals("CREATE", changeLog.getType()),
        () -> assertEquals("numericAttr", attributeChange.getAttribute()),
        () -> assertNull(attributeChange.getPreviousValue()),
        () -> assertEquals("2", attributeChange.getCurrentValue()));
  }

  @Test
  void
      shouldGetChangeLogPagerWithNextAttributeWhenMultipleAttributesImportedAndFirstPageRequested() {
    JsonPage changeLogs =
        GET(
            "/tracker/trackedEntities/{id}/changeLogs?program={programUid}&page={page}&pageSize={pageSize}",
            trackedEntity.getUid(), program.getUid(),
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
                String.format("http://localhost/tracker/trackedEntities/%s/changeLogs", trackedEntity.getUid())));
  }

  @Test
  void
      shouldGetChangeLogPagerWithNextAndPreviousAttributesWhenMultipleAttributesImportedAndSecondPageRequested() {
    JsonPage changeLogs =
        GET(
            "/tracker/trackedEntities/{id}/changeLogs?program={programUid}&page={page}&pageSize={pageSize}",
            trackedEntity.getUid(), program.getUid(),
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
                String.format("http://localhost/tracker/trackedEntities/%s/changeLogs", trackedEntity.getUid())),
        () ->
            assertPagerLink(
                pager.getNextPage(),
                3,
                1,
                String.format("http://localhost/tracker/trackedEntities/%s/changeLogs", trackedEntity.getUid())));
  }

  @Test
  void
      shouldGetChangeLogPagerWithPreviousAttributeWhenMultipleAttributesImportedAndLastPageRequested() {
    JsonPage changeLogs =
        GET(
            "/tracker/trackedEntities/{id}/changeLogs?program={programUid}&page={page}&pageSize={pageSize}",
            trackedEntity.getUid(), program.getUid(),
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
                String.format("http://localhost/tracker/trackedEntities/%s/changeLogs", trackedEntity.getUid())),
        () -> assertHasNoMember(pager, "nextPage"));
  }

  @Test
  void
      shouldGetChangeLogPagerWithoutPreviousNorNextAttributeWhenMultipleAttributesImportedAndAllAttributesFitInOnePage() {
    JsonPage changeLogs =
        GET(
            "/tracker/trackedEntities/{id}/changeLogs?program={programUid}&page={page}&pageSize={pageSize}",
            trackedEntity.getUid(), program.getUid(),
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

  private TrackerObjects fromJson(String path) throws IOException {
    return renderService.fromJson(
        new ClassPathResource(path).getInputStream(), TrackerObjects.class);
  }

  private ObjectBundle setUpMetadata(String path) throws IOException {
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(new ClassPathResource(path).getInputStream(), RenderFormat.JSON);
    ObjectBundleParams params = new ObjectBundleParams();
    params.setObjectBundleMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE_AND_UPDATE);
    params.setObjects(metadata);
    ObjectBundle bundle = objectBundleService.create(params);
    assertNoMetadataErrors(objectBundleValidationService.validate(bundle));
    objectBundleService.commit(bundle);
    return bundle;
  }

  private void assertNoMetadataErrors(ObjectBundleValidationReport report) {
    assertNotNull(report);
    List<String> errors = new ArrayList<>();
    report.forEachErrorReport(
        err -> {
          errors.add(err.toString());
        });
    assertFalse(
        report.hasErrorReports(), String.format("Expected no errors, instead got: %s\n", errors));
  }

  private void assertNoDataErrors(ImportReport report) {
    assertNotNull(report);
    assertEquals(
        Status.OK,
        report.getStatus(),
        errorMessage(
            "Expected import with status OK, instead got:\n", report.getValidationReport()));
  }

  private Supplier<String> errorMessage(String errorTitle, ValidationReport report) {
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

  private String createJsonPayload(int value) {
    return """
        {
          "trackedEntities": [
            {
              "attributes": [
                {
                  "attribute": "numericAttr",
                  "value": %d
                }
              ],
              "trackedEntity": "IOR1AXXl24H",
              "trackedEntityType": "ja8NY4PW7Xm",
              "orgUnit": "h4w96yEMlzO"
            }
          ]
        }
        """.formatted(value);
  }

  private static void assertPagerLink(String actual, int page, int pageSize, String start) {
    assertNotNull(actual);
    assertAll(
        () -> assertStartsWith(start, actual),
        () -> assertContains("page=" + page, actual),
        () -> assertContains("pageSize=" + pageSize, actual));
  }
}
