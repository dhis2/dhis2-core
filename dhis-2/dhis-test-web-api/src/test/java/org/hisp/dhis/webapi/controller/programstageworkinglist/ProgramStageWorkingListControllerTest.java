/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.programstageworkinglist;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProgramStageWorkingListControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private RenderService renderService;

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private IdentifiableObjectManager manager;

  private final String programId = "BFcipDERJnf";

  private final String programStageId = "NpsdDv6kKSO";

  protected void setUpMetadata(String path) throws IOException {
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(new ClassPathResource(path).getInputStream(), RenderFormat.JSON);
    ObjectBundleParams params = new ObjectBundleParams();
    params.setObjectBundleMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.setObjects(metadata);
    ObjectBundle bundle = objectBundleService.create(params);
    assertNoErrors(objectBundleValidationService.validate(bundle));
    objectBundleService.commit(bundle);
  }

  @BeforeAll
  void setUp() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    manager.flush();
    manager.clear();
  }

  @Test
  void shouldReturnAllWorkingListsWhenWorkingListsRequested() {
    String workingListId1 = createWorkingList("Test working list 1");
    String workingListId2 = createWorkingList("Test working list 2");

    String response = GET("/programStageWorkingLists?fields=id").content().toString();

    assertTrue(
        response.contains(workingListId1),
        "The working list id: " + workingListId1 + " is not present in the response");
    assertTrue(
        response.contains(workingListId2),
        "The working list id: " + workingListId2 + " is not present in the response");
  }

  static Stream<Arguments> shouldCreateWorkingListWithProgramStageQueryCriteria() {
    Consumer<JsonProgramStageQueryCriteria> followUpIsNull =
        json ->
            assertFalse(
                json.has("followUp"),
                "FollowUp check has not been requested and should not be in the response");
    Consumer<JsonProgramStageQueryCriteria> followUpIsTrue =
        json -> assertTrue(json.getFollowUp(), "Expected followUp true but got false");
    Consumer<JsonProgramStageQueryCriteria> followUpIsFalse =
        json -> assertFalse(json.getFollowUp(), "Expected followUp false but got true");

    return Stream.of(
        arguments("", followUpIsNull),
        arguments("'followUp': true,\n", followUpIsTrue),
        arguments("'followUp': false,\n", followUpIsFalse));
  }

  @MethodSource
  @ParameterizedTest
  void shouldCreateWorkingListWithProgramStageQueryCriteria(
      String followUp, Consumer<JsonProgramStageQueryCriteria> followUpAssertion) {
    String dataElementId = "DATAEL00001";
    String workingListJson =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programStageWorkingLists",
                """
               {
                 'program': {'id': '%s'},
                 'programStage': {'id': '%s'},
                 'name':'workingListName',
                 'programStageQueryCriteria': {
                   "displayColumnOrder": [
                     "w75KJ2mc4zz",
                     "zDhUuAYrxNC",
                     "APtutTb0nOY"
                   ],
                   'order': 'createdAt:desc',
                   'dataFilters':[{'null':'','dataItem':'%s'}],
                   'enrollmentStatus': 'ACTIVE',
                    %s
                   'eventOccurredAt': {
                     'type': 'RELATIVE',
                     'period': 'TODAY'
                    }
                  }
                }
              """
                    .formatted(programId, programStageId, dataElementId, followUp)));

    JsonWorkingList workingList =
        GET("/programStageWorkingLists/{id}", workingListJson).content().as(JsonWorkingList.class);

    assertFalse(workingList.isEmpty());

    assertEquals(programId, workingList.getProgram());
    assertEquals(programStageId, workingList.getProgramStage());

    JsonProgramStageQueryCriteria programStageQueryCriteria =
        workingList.getProgramStageQueryCriteria();

    assertFalse(programStageQueryCriteria.isEmpty());

    assertEquals("ACTIVE", programStageQueryCriteria.getEnrollmentStatus());
    assertEquals("createdAt:desc", programStageQueryCriteria.getOrder());

    JsonDatePeriod eventOccurredAt = programStageQueryCriteria.getEventOccurredAt();
    assertEquals("RELATIVE", eventOccurredAt.getType());
    assertEquals("TODAY", eventOccurredAt.getPeriod());

    JsonArray displayColumnOrder = programStageQueryCriteria.getDisplayColumnOrder();
    assertEquals("w75KJ2mc4zz", displayColumnOrder.getString(0).string());
    assertEquals("zDhUuAYrxNC", displayColumnOrder.getString(1).string());
    assertEquals("APtutTb0nOY", displayColumnOrder.getString(2).string());

    JsonArray dataFilters = workingList.getProgramStageQueryCriteria().getDataFilters();
    assertFalse(dataFilters.isEmpty(), "Data filters should not be empty");
    JsonObject dataFilter =
        workingList.getProgramStageQueryCriteria().getDataFilters().get(0).asObject();
    assertEquals("", dataFilter.getString("null").string());
    assertEquals(dataElementId, dataFilter.getString("dataItem").string());

    followUpAssertion.accept(programStageQueryCriteria);
  }

  @Test
  void shouldUpdateWorkingListWhenUpdateRequested() {
    String workingListId = createWorkingList("Test working list to update");

    String updatedName = "Updated working list";
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/programStageWorkingLists/" + workingListId,
            "{'program': {'id': '"
                + programId
                + "'}, 'programStage': {'id': '"
                + programStageId
                + "'}, 'name':'"
                + updatedName
                + "'}"));

    String response = GET("/programStageWorkingLists/{id}", workingListId).content().toString();
    assertTrue(
        response.contains(updatedName),
        "Could not find the working list name: " + updatedName + " in the response");
  }

  @Test
  void shouldDeleteWorkingListWhenDeleteRequested() {
    String workingListId = createWorkingList("Test working to delete");

    HttpResponse response = DELETE("/programStageWorkingLists/" + workingListId);
    assertEquals(HttpStatus.OK, response.status());
  }

  @Test
  void shouldFailWhenCreatingWorkingListWithoutName() {
    HttpResponse response =
        POST(
            "/programStageWorkingLists",
            "{'program': {'id': '"
                + programId
                + "'}, 'programStage': {'id': '"
                + programStageId
                + "'}}");

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertEquals(
        "Missing required property `name`",
        response.error().getTypeReport().getErrorReports().get(0).getMessage());
  }

  @Test
  void shouldFailWhenCreatingWorkingListWithoutProgramId() {
    HttpResponse response =
        POST(
            "/programStageWorkingLists",
            "{'programStage': {'id': '" + programStageId + "'}, 'name':'Test'}");

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertEquals(
        "Missing required property `program`",
        response.error().getTypeReport().getErrorReports().get(0).getMessage());
  }

  @Test
  void shouldFailWhenCreatingWorkingListWithNonExistentProgramId() {
    HttpResponse response =
        POST(
            "/programStageWorkingLists",
            "{'program': {'id': 'madeUpProgramId'}, 'programStage': {'id': '"
                + programStageId
                + "'}, 'name':'Test'}");

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertContains(
        "Invalid reference [madeUpProgramId]",
        response.error().getTypeReport().getErrorReports().get(0).getMessage());
  }

  @Test
  void shouldFailWhenCreatingWorkingListWithoutProgramStageId() {
    HttpResponse response =
        POST(
            "/programStageWorkingLists", "{'program': {'id': '" + programId + "'}, 'name':'Test'}");

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertEquals(
        "Missing required property `programStage`",
        response.error().getTypeReport().getErrorReports().get(0).getMessage());
  }

  @Test
  void shouldFailWhenCreatingWorkingListWithNonExistentProgramStageId() {
    HttpResponse response =
        POST(
            "/programStageWorkingLists",
            "{'program': {'id': '"
                + programId
                + "'}, 'programStage': {'id': 'madeUpProgramStageId'}, 'name':'Test'}");

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertContains(
        "Invalid reference [madeUpProgramStageId]",
        response.error().getTypeReport().getErrorReports().get(0).getMessage());
  }

  @Test
  void shouldFailWhenUpdatingWorkingListWithoutProgramId() {
    String workingListId = createWorkingList("Test working list to update");

    String updatedName = "Updated working list";
    HttpResponse response =
        PUT(
            "/programStageWorkingLists/" + workingListId,
            "{'programStage': {'id': '" + programStageId + "'}, 'name':'" + updatedName + "'}");

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertEquals(
        "Missing required property `program`",
        response.error().getTypeReport().getErrorReports().get(0).getMessage());
  }

  @Test
  void shouldFailWhenUpdatingWorkingListWithNonExistentProgramId() {
    String workingListId = createWorkingList("Test working list to update");

    String updatedName = "Updated working list";
    HttpResponse response =
        PUT(
            "/programStageWorkingLists/" + workingListId,
            "{'program': {'id': 'madeUpProgramId'}, 'programStage': {'id': '"
                + programStageId
                + "'}, 'name':'"
                + updatedName
                + "'}");

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertContains(
        "Invalid reference [madeUpProgramId]",
        response.error().getTypeReport().getErrorReports().get(0).getMessage());
  }

  @Test
  void shouldFailWhenUpdatingWorkingListWithoutProgramStageId() {
    String workingListId = createWorkingList("Test working list to update");

    String updatedName = "Updated working list";
    HttpResponse response =
        PUT(
            "/programStageWorkingLists/" + workingListId,
            "{'program': {'id': '" + programId + "'}, 'name':'" + updatedName + "'}");

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertEquals(
        "Missing required property `programStage`",
        response.error().getTypeReport().getErrorReports().get(0).getMessage());
  }

  @Test
  void shouldFailWhenUpdatingWorkingListWithNonExistentProgramStageId() {
    String workingListId = createWorkingList("Test working list to update");

    String updatedName = "Updated working list";
    HttpResponse response =
        PUT(
            "/programStageWorkingLists/" + workingListId,
            "{'program': {'id': '"
                + programId
                + "'}, 'programStage': {'id': 'madeUpProgramStageId'}, 'name':'"
                + updatedName
                + "'}");

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertContains(
        "Invalid reference [madeUpProgramStageId]",
        response.error().getTypeReport().getErrorReports().get(0).getMessage());
  }

  private String createWorkingList(String workingListName) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/programStageWorkingLists",
            """
                {
                  'program': {'id': '%s'},
                  'programStage': {'id': '%s'},
                  'name':'%s'
                }
              """
                .formatted(programId, programStageId, workingListName)));
  }

  protected TrackerObjects fromJson(String path) throws IOException {
    return renderService.fromJson(
        new ClassPathResource(path).getInputStream(), TrackerObjects.class);
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
