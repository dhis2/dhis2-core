/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.programstageworkinglist;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProgramStageWorkingListControllerTest extends PostgresControllerIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private IdentifiableObjectManager manager;

  private final String programId = "BFcipDERJnf";

  private final String programStageId = "NpsdDv6kKSO";

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

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
    return Stream.of(
        arguments(
            "",
            assertFollowUp(null),
            "'dataFilters':[{'dataItem':'DATAEL00001'}],\n",
            assertUnaryOperator(null)),
        arguments(
            "'followUp': true,\n",
            assertFollowUp(true),
            "'dataFilters':[{'null':true,'dataItem':'DATAEL00001'}],\n",
            assertUnaryOperator(true)),
        arguments(
            "'followUp': false,\n",
            assertFollowUp(false),
            "'dataFilters':[{'null':false,'dataItem':'DATAEL00001'}],\n",
            assertUnaryOperator(false)));
  }

  private static Consumer<JsonProgramStageQueryCriteria> assertFollowUp(Boolean expected) {
    return json -> {
      if (expected == null) {
        assertFalse(
            json.has("followUp"),
            "FollowUp check has not been requested and should not be in the response");
      } else {
        assertEquals(expected, json.getFollowUp(), "Unexpected followUp value");
      }
    };
  }

  private static Consumer<JsonProgramStageQueryCriteria> assertUnaryOperator(Boolean expected) {
    return json -> {
      if (expected == null) {
        assertTrue(
            json.getDataFilters().stream().noneMatch(filter -> filter.asObject().has("null")),
            "Null unary operator not expected in the response.");
      } else {
        boolean actual =
            Boolean.TRUE.equals(
                json.getDataFilters().get(0).asObject().getBoolean("null").booleanValue());
        assertEquals(
            expected,
            actual,
            String.format(
                "Expected null unary operator to be %s, but is %s instead.", expected, actual));
      }
    };
  }

  @MethodSource
  @ParameterizedTest
  void shouldCreateWorkingListWithProgramStageQueryCriteria(
      String followUp,
      Consumer<JsonProgramStageQueryCriteria> followUpAssertion,
      String dataFilter,
      Consumer<JsonProgramStageQueryCriteria> dataFilterAssertion) {
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
                   %s
                   'enrollmentStatus': 'ACTIVE',
                    %s
                   'eventOccurredAt': {
                     'type': 'RELATIVE',
                     'period': 'TODAY'
                    }
                  }
                }
              """
                    .formatted(programId, programStageId, dataFilter, followUp)));

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

    dataFilterAssertion.accept(programStageQueryCriteria);
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
}
