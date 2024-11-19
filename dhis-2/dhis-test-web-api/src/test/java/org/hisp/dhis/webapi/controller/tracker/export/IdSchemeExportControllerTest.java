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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

/** Tests tracker exporter idScheme support. */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdSchemeExportControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String METADATA_ATTRIBUTE = "j45AR9cBQKc";

  @Autowired private RenderService renderService;

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

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
    assertNoErrors(
        trackerImportService.importTracker(params, fromJson("tracker/event_and_enrollment.json")));
    get(Attribute.class, METADATA_ATTRIBUTE); // ensure this is created in setup

    manager.flush();
    manager.clear();
  }

  @ParameterizedTest
  @MethodSource(value = "shouldExportMetadataUsingGivenIdSchemeProvider")
  void shouldExportMetadataUsingGivenIdScheme(TrackerIdSchemeParam idSchemeParam) {
    switchContextToUser(importUser);
    Event event = get(Event.class, "QRYjLTiJTrA");

    // maps JSON fields to idScheme request parameters
    Map<String, String> idSchemeRequestParams =
        Map.of(
            "orgUnit",
            "orgUnit",
            "program",
            "program",
            "programStage",
            "programStage",
            "attributeOptionCombo",
            "categoryOptionCombo",
            "attributeCategoryOptions",
            "categoryOption",
            "dataValues",
            "dataElement");
    // maps JSON fields to expected metadata identifier in the requested idScheme and form. many
    // category options are mapped to a single string value in the event.attributeCategoryOptions
    Map<String, Function<JsonObject, Executable>> metadata =
        Map.of(
            "orgUnit",
            actual ->
                (() ->
                    assertIdScheme(
                        idSchemeParam.getIdentifier(event.getOrganisationUnit()),
                        actual,
                        idSchemeParam,
                        "orgUnit")),
            "program",
            actual ->
                (() ->
                    assertIdScheme(
                        idSchemeParam.getIdentifier(event.getProgramStage().getProgram()),
                        actual,
                        idSchemeParam,
                        "program")),
            "programStage",
            actual ->
                (() ->
                    assertIdScheme(
                        idSchemeParam.getIdentifier(event.getProgramStage()),
                        actual,
                        idSchemeParam,
                        "programStage")),
            "attributeOptionCombo",
            actual ->
                (() ->
                    assertIdScheme(
                        idSchemeParam.getIdentifier(event.getAttributeOptionCombo()),
                        actual,
                        idSchemeParam,
                        "attributeOptionCombo")),
            "attributeCategoryOptions",
            json ->
                (() -> {
                  String field = "attributeCategoryOptions";
                  List<String> expected =
                      event.getAttributeOptionCombo().getCategoryOptions().stream()
                          .map(co -> idSchemeParam.getIdentifier(co))
                          .toList();
                  assertNotEmpty(
                      expected,
                      String.format(
                          "metadata corresponding to field \"%s\" has no value in test data for"
                              + " idScheme '%s'",
                          field, idSchemeParam));
                  assertTrue(
                      json.has(field),
                      () ->
                          String.format(
                              "field \"%s\" is not in response %s for idScheme '%s'",
                              field, json, idSchemeParam));
                  assertContainsOnly(
                      expected, Arrays.asList(json.getString(field).string().split(",")));
                }),
            "dataValues",
            json ->
                (() -> {
                  String field = "dataValues";
                  List<String> expected =
                      event.getEventDataValues().stream()
                          .map(
                              dv ->
                                  idSchemeParam.getIdentifier(
                                      get(DataElement.class, dv.getDataElement())))
                          .toList();
                  assertNotEmpty(
                      expected,
                      String.format(
                          "metadata corresponding to field \"%s\" has no value in test data for"
                              + " idScheme '%s'",
                          field, idSchemeParam));
                  assertTrue(
                      json.has(field),
                      () ->
                          String.format(
                              "field \"%s\" is not in response %s for idScheme '%s'",
                              field, json, idSchemeParam));
                  List<String> actual =
                      json.getList(field, JsonObject.class)
                          .toList(el -> el.getString("dataElement").string(""));
                  assertContainsOnly(expected, actual);
                }));
    String fields = metadata.keySet().stream().collect(Collectors.joining(","));
    String idSchemes =
        metadata.keySet().stream()
            .map(m -> idSchemeRequestParams.get(m) + "IdScheme=" + idSchemeParam)
            .collect(Collectors.joining("&"));

    JsonEvent actual =
        GET("/tracker/events/{id}?fields={fields}&{idSchemes}", event.getUid(), fields, idSchemes)
            .content(HttpStatus.OK)
            .as(JsonEvent.class);

    assertMetadataIdScheme(metadata, actual, idSchemeParam, "event");
  }

  public static Stream<TrackerIdSchemeParam> shouldExportMetadataUsingGivenIdSchemeProvider() {
    return Stream.of(
        TrackerIdSchemeParam.UID,
        TrackerIdSchemeParam.CODE,
        TrackerIdSchemeParam.NAME,
        TrackerIdSchemeParam.ofAttribute(METADATA_ATTRIBUTE));
  }

  /**
   * Asserts that every metadata key from {@code expected} is a field in the {@code actual} JSON and
   * that its string value matches the requested {@code idSchemeParam}.
   */
  private void assertMetadataIdScheme(
      Map<String, Function<JsonObject, Executable>> expected,
      JsonObject actual,
      TrackerIdSchemeParam idSchemeParam,
      String objectName) {
    List<Executable> assertions =
        expected.entrySet().stream().map(e -> e.getValue().apply(actual)).toList();
    assertAll(objectName + " metadata assertions for idScheme=" + idSchemeParam, assertions);
  }

  private static void assertIdScheme(
      String expected, JsonObject actual, TrackerIdSchemeParam idSchemeParam, String field) {
    assertNotEmpty(
        expected,
        String.format(
            "metadata corresponding to field \"%s\" has no value in test data for idScheme '%s'",
            field, idSchemeParam));
    assertTrue(
        actual.has(field),
        () ->
            String.format(
                "field \"%s\" is not in response %s for idScheme '%s'",
                field, actual, idSchemeParam));
    assertEquals(
        expected,
        actual.getString(field).string(),
        () ->
            String.format(
                "field \"%s\" does not have required idScheme '%s' in response",
                field, idSchemeParam));
  }

  private <T extends IdentifiableObject> T get(Class<T> type, String uid) {
    T t = manager.get(type, uid);
    assertNotNull(
        t,
        () ->
            String.format(
                "'%s' with uid '%s' should have been created", type.getSimpleName(), uid));
    return t;
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
    report.forEachErrorReport(
        err -> {
          errors.add(err.toString());
        });
    assertFalse(
        report.hasErrorReports(), String.format("Expected no errors, instead got: %s%n", errors));
  }
}
