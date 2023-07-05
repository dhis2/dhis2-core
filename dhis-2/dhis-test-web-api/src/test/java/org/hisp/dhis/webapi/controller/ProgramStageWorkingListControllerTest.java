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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProgramStageWorkingListControllerTest extends DhisControllerConvenienceTest {

  private String programId;

  private String programStageId;

  @BeforeEach
  void setUp() {
    programId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name': 'ProgramTest', 'shortName': 'ProgramTest', 'programType': 'WITHOUT_REGISTRATION'}"));

    programStageId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programStages/",
                "{'name': 'ProgramStageTest', 'program':" + "{'id': '" + programId + "'}}"));
  }

  @Test
  void shouldReturnWorkingListIdWhenWorkingListCreated() {
    String workingListId = createWorkingList("Test working list");

    assertFalse(workingListId.isEmpty(), "Expected working list id, but got nothing instead");
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

  @Test
  void shouldDeleteWorkingListWhenDeleteRequested() {
    String workingListId = createWorkingList("Test working to delete");

    HttpResponse response = DELETE("/programStageWorkingLists/" + workingListId);
    assertEquals(HttpStatus.OK, response.status());
  }

  private String createWorkingList(String workingListName) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/programStageWorkingLists",
            "{'program': {'id': '"
                + programId
                + "'}, 'programStage': {'id': '"
                + programStageId
                + "'}, 'name':'"
                + workingListName
                + "'}"));
  }
}
