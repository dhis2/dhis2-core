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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.web.HttpStatus.BAD_REQUEST;
import static org.hisp.dhis.web.HttpStatus.CREATED;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller tests for {@link org.hisp.dhis.webapi.controller.event.EventVisualizationController}.
 *
 * @author maikel arabori
 */
class EventVisualizationControllerTest extends DhisControllerConvenienceTest {
  @Autowired private ObjectMapper jsonMapper;

  private Program mockProgram;

  private ProgramStage mockProgramStage;

  private ProgramIndicator mockProgramIndicator;

  @BeforeEach
  public void beforeEach() throws JsonProcessingException {
    mockProgram = createProgram('A');
    POST("/programs", jsonMapper.writeValueAsString(mockProgram)).content(CREATED);

    mockProgramIndicator = createProgramIndicator('A', mockProgram, "exp", "filter");
    mockProgramIndicator.setUid("deabcdefghB");
    POST("/programIndicators", jsonMapper.writeValueAsString(mockProgramIndicator))
        .content(CREATED);

    mockProgramStage = createProgramStage('A', mockProgram);
    mockProgramStage.setUid("deabcdefghA");
    POST("/programStages", jsonMapper.writeValueAsString(mockProgramStage)).content(CREATED);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testPostForSingleEventDate() {
    // Given
    String eventDateDimension = "eventDate";
    String eventDate = "2021-07-21_2021-08-01";
    String dimensionBody =
        "{'dimension': '" + eventDateDimension + "', 'items': [{'id': '" + eventDate + "'}]}";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': ["
            + dimensionBody
            + "]}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    JsonResponse response = GET("/eventVisualizations/" + uid).content();
    Map<String, JsonNode> nodeMap = (Map<String, JsonNode>) response.node().value();

    assertThat(nodeMap.get("simpleDimensions").toString(), containsString("COLUMN"));
    assertThat(nodeMap.get("simpleDimensions").toString(), containsString(eventDateDimension));
    assertThat(nodeMap.get("simpleDimensions").toString(), containsString(eventDate));
    assertThat(nodeMap.get("columns").toString(), containsString(eventDateDimension));
    assertThat(nodeMap.get("rows").toString(), not(containsString(eventDateDimension)));
    assertThat(nodeMap.get("filters").toString(), not(containsString(eventDateDimension)));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testPostForMultiEventDates() {
    // Given
    String eventDateDimension = "eventDate";
    String incidentDateDimension = "incidentDate";
    String eventDate = "2021-07-21_2021-08-01";
    String incidentDate = "2021-07-21_2021-08-01";
    String eventDateBody =
        "{'dimension': '" + eventDateDimension + "', 'items': [{'id': '" + eventDate + "'}]}";
    String incidentDateBody =
        "{'dimension': '" + incidentDateDimension + "', 'items': [{'id': '" + incidentDate + "'}]}";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'rows': ["
            + eventDateBody
            + ","
            + incidentDateBody
            + "]}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    JsonResponse response = GET("/eventVisualizations/" + uid).content();
    Map<String, JsonNode> nodeMap = (Map<String, JsonNode>) response.node().value();

    assertThat(nodeMap.get("simpleDimensions").toString(), containsString("ROW"));
    assertThat(nodeMap.get("simpleDimensions").toString(), containsString(eventDate));
    assertThat(nodeMap.get("simpleDimensions").toString(), containsString(incidentDate));
    assertThat(nodeMap.get("rows").toString(), containsString(eventDateDimension));
    assertThat(nodeMap.get("rows").toString(), containsString(incidentDateDimension));
    assertThat(nodeMap.get("columns").toString(), not(containsString(eventDateDimension)));
    assertThat(nodeMap.get("columns").toString(), not(containsString(incidentDateDimension)));
    assertThat(nodeMap.get("filters").toString(), not(containsString(eventDateDimension)));
    assertThat(nodeMap.get("filters").toString(), not(containsString(incidentDateDimension)));
  }

  @Test
  void testPostForInvalidEventDimension() {
    // Given
    String invalidDimension = "invalidDimension";
    String eventDate = "2021-07-21_2021-08-01";
    String dimensionBody =
        "{'dimension': '" + invalidDimension + "', 'items': [{'id': '" + eventDate + "'}]}";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': ["
            + dimensionBody
            + "]}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    assertEquals(
        "Not a valid dimension: " + invalidDimension,
        GET("/eventVisualizations/" + uid).error(BAD_REQUEST).getMessage());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testPostRepetitionForFilter() {
    // Given
    String dimension = "ou";
    String indexes = "[1,2,3,-2,-1,0]";
    String repetition = "'repetition': {'indexes': " + indexes + "}";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'filters': [{'dimension': '"
            + dimension
            + "', "
            + repetition
            + "}]}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    String getParams = "?fields=:all,filters[:all,items,repetitions]";
    JsonResponse response = GET("/eventVisualizations/" + uid + getParams).content();
    Map<String, JsonNode> nodeMap = (Map<String, JsonNode>) response.node().value();

    assertThat(nodeMap.get("repetitions").toString(), containsString("FILTER"));
    assertThat(nodeMap.get("repetitions").toString(), containsString(indexes));
    assertThat(nodeMap.get("repetitions").toString(), containsString(dimension));
    assertThat(nodeMap.get("filters").toString(), containsString(indexes));
    assertThat(nodeMap.get("columns").toString(), not(containsString(indexes)));
    assertThat(nodeMap.get("rows").toString(), not(containsString(indexes)));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testPostRepetitionForRow() {
    // Given
    String dimension = "pe";
    String indexes = "[1,2,0]";
    String repetition = "'repetition': {'indexes': " + indexes + "}";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'rows': [{'dimension': '"
            + dimension
            + "', "
            + repetition
            + "}]}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    String getParams = "?fields=:all,rows[:all,items,repetitions]";
    JsonResponse response = GET("/eventVisualizations/" + uid + getParams).content();
    Map<String, JsonNode> nodeMap = (Map<String, JsonNode>) response.node().value();

    assertThat(nodeMap.get("repetitions").toString(), containsString("ROW"));
    assertThat(nodeMap.get("repetitions").toString(), containsString(indexes));
    assertThat(nodeMap.get("repetitions").toString(), containsString(dimension));
    assertThat(nodeMap.get("rows").toString(), containsString(indexes));
    assertThat(nodeMap.get("columns").toString(), not(containsString(indexes)));
    assertThat(nodeMap.get("filters").toString(), not(containsString(indexes)));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testPostRepetitionForColumn() {
    // Given
    String dimension = "pe";
    String indexes = "[1,0,-1]";
    String repetition = "'repetition': {'indexes': " + indexes + "}";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': '"
            + dimension
            + "', "
            + repetition
            + "}]}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    String getParams = "?fields=:all,columns[:all,items,repetitions]";
    JsonResponse response = GET("/eventVisualizations/" + uid + getParams).content();
    Map<String, JsonNode> nodeMap = (Map<String, JsonNode>) response.node().value();

    assertThat(nodeMap.get("repetitions").toString(), containsString("COLUMN"));
    assertThat(nodeMap.get("repetitions").toString(), containsString(indexes));
    assertThat(nodeMap.get("repetitions").toString(), containsString(dimension));
    assertThat(nodeMap.get("columns").toString(), containsString(indexes));
    assertThat(nodeMap.get("rows").toString(), not(containsString(indexes)));
    assertThat(nodeMap.get("filters").toString(), not(containsString(indexes)));
  }

  @Test
  void testPost() {
    // Given
    String body =
        "{'name': 'Test post', 'type': 'LINE_LIST', 'program': {'id': '"
            + mockProgram.getUid()
            + "'}, 'skipRounding': true}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    JsonObject response = GET("/eventVisualizations/" + uid).content();

    assertThat(response.get("name").node().value(), is(equalTo("Test post")));
    assertThat(response.get("type").node().value(), is(equalTo("LINE_LIST")));
    assertThat(response.get("program").node().get("id").value(), is(equalTo(mockProgram.getUid())));
    assertThat(response.get("skipRounding").node().value(), is(equalTo(true)));
  }
}
