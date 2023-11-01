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
import static org.hisp.dhis.dataelement.DataElementDomain.TRACKER;
import static org.hisp.dhis.web.HttpStatus.BAD_REQUEST;
import static org.hisp.dhis.web.HttpStatus.CONFLICT;
import static org.hisp.dhis.web.HttpStatus.CREATED;
import static org.hisp.dhis.web.HttpStatus.OK;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonError;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
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

  private DataElement mockDataElement;

  private OrganisationUnit mockOrganisationUnit;

  private TrackedEntityType mockTrackerEntityType;

  @BeforeEach
  public void beforeEach() throws JsonProcessingException {
    mockProgram = createProgram('A');
    mockProgram.setUid("deabcdefghP");
    POST("/programs", jsonMapper.writeValueAsString(mockProgram)).content(CREATED);

    mockProgramIndicator = createProgramIndicator('A', mockProgram, "exp", "filter");
    mockProgramIndicator.setUid("deabcdefghB");
    POST("/programIndicators", jsonMapper.writeValueAsString(mockProgramIndicator))
        .content(CREATED);

    mockDataElement = createDataElement('A');
    mockDataElement.setUid("deabcdefghC");
    mockDataElement.setDomainType(TRACKER);
    POST("/dataElements", jsonMapper.writeValueAsString(mockDataElement)).content(CREATED);

    mockDataElement = createDataElement('B');
    mockDataElement.setUid("deabcdefghE");
    mockDataElement.setDomainType(TRACKER);
    POST("/dataElements", jsonMapper.writeValueAsString(mockDataElement)).content(CREATED);

    mockProgramStage = createProgramStage('A', mockProgram);
    mockProgramStage.setUid("deabcdefghS");
    POST("/programStages", jsonMapper.writeValueAsString(mockProgramStage)).content(CREATED);

    mockOrganisationUnit = createOrganisationUnit('A');
    mockOrganisationUnit.setUid("ImspTQPwCqd");
    POST("/organisationUnits", jsonMapper.writeValueAsString(mockOrganisationUnit))
        .content(CREATED);

    mockTrackerEntityType = createTrackedEntityType('A');
    mockTrackerEntityType.setUid("nEenWmSyUEp");
    POST("/trackedEntityTypes", jsonMapper.writeValueAsString(mockTrackerEntityType))
        .content(CREATED);
  }

  @Test
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
    JsonObject response = GET("/eventVisualizations/" + uid).content();

    assertThat(response.get("simpleDimensions").toString(), containsString("COLUMN"));
    assertThat(response.get("simpleDimensions").toString(), containsString(eventDateDimension));
    assertThat(response.get("simpleDimensions").toString(), containsString(eventDate));
    assertThat(response.get("columns").toString(), containsString(eventDateDimension));
    assertThat(response.get("rows").toString(), not(containsString(eventDateDimension)));
    assertThat(response.get("filters").toString(), not(containsString(eventDateDimension)));
  }

  @Test
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
    JsonObject response = GET("/eventVisualizations/" + uid).content();

    assertThat(response.get("simpleDimensions").toString(), containsString("ROW"));
    assertThat(response.get("simpleDimensions").toString(), containsString(eventDate));
    assertThat(response.get("simpleDimensions").toString(), containsString(incidentDate));
    assertThat(response.get("rows").toString(), containsString(eventDateDimension));
    assertThat(response.get("rows").toString(), containsString(incidentDateDimension));
    assertThat(response.get("columns").toString(), not(containsString(eventDateDimension)));
    assertThat(response.get("columns").toString(), not(containsString(incidentDateDimension)));
    assertThat(response.get("filters").toString(), not(containsString(eventDateDimension)));
    assertThat(response.get("filters").toString(), not(containsString(incidentDateDimension)));
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
    JsonObject response = GET("/eventVisualizations/" + uid + getParams).content();

    assertThat(response.get("repetitions").toString(), containsString("FILTER"));
    assertThat(response.get("repetitions").toString(), containsString(indexes));
    assertThat(response.get("repetitions").toString(), containsString(dimension));
    assertThat(response.get("filters").toString(), containsString(indexes));
    assertThat(response.get("columns").toString(), not(containsString(indexes)));
    assertThat(response.get("rows").toString(), not(containsString(indexes)));
  }

  @Test
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
    JsonObject response = GET("/eventVisualizations/" + uid + getParams).content();

    assertThat(response.get("repetitions").toString(), containsString("ROW"));
    assertThat(response.get("repetitions").toString(), containsString(indexes));
    assertThat(response.get("repetitions").toString(), containsString(dimension));
    assertThat(response.get("rows").toString(), containsString(indexes));
    assertThat(response.get("columns").toString(), not(containsString(indexes)));
    assertThat(response.get("filters").toString(), not(containsString(indexes)));
  }

  @Test
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
    JsonObject response = GET("/eventVisualizations/" + uid + getParams).content();

    assertThat(response.get("repetitions").toString(), containsString("COLUMN"));
    assertThat(response.get("repetitions").toString(), containsString(indexes));
    assertThat(response.get("repetitions").toString(), containsString(dimension));
    assertThat(response.get("columns").toString(), containsString(indexes));
    assertThat(response.get("rows").toString(), not(containsString(indexes)));
    assertThat(response.get("filters").toString(), not(containsString(indexes)));
  }

  @Test
  void testPostSortingObject() {
    // Given
    String dimension = "pe";
    String sorting = "'sorting': [{'dimension': '" + dimension + "', 'direction':'ASC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': '"
            + dimension
            + "'}],"
            + sorting
            + "}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    String getParams = "?fields=:all,columns[:all,items,sorting]";
    JsonObject response = GET("/eventVisualizations/" + uid + getParams).content();

    assertThat(response.get("sorting").toString(), containsString("pe"));
    assertThat(response.get("sorting").toString(), containsString("ASC"));
  }

  @Test
  void testPostSortingObjectWithPrefixedColumn() {
    // Given
    String programStageUid = mockProgramStage.getUid();
    String dimensionUid = mockProgramIndicator.getUid();
    String sorting =
        "'sorting': [{'dimension': '"
            + programStageUid
            + "."
            + dimensionUid
            + "', 'direction':'ASC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': '"
            + dimensionUid
            + "',"
            + "'programStage': {'id':'"
            + programStageUid
            + "'}"
            + "}],"
            + sorting
            + "}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    String getParams = "?fields=:all,columns[:all,items,sorting]";
    JsonObject response = GET("/eventVisualizations/" + uid + getParams).content();

    assertThat(
        response.get("sorting").toString(), containsString(programStageUid + "." + dimensionUid));
    assertThat(response.get("sorting").toString(), containsString("ASC"));
  }

  @Test
  void testPostSortingObjectWithPrefixedColumnWithIndex() {
    // Given
    String programStageUid = mockProgramStage.getUid() + "[-2]";
    String dimensionUid = mockProgramIndicator.getUid();
    String sorting =
        "'sorting': [{'dimension': '"
            + programStageUid
            + "."
            + dimensionUid
            + "', 'direction':'ASC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': '"
            + dimensionUid
            + "',"
            + "'programStage': {'id':'"
            + programStageUid
            + "'}"
            + "}],"
            + sorting
            + "}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    String getParams = "?fields=:all,columns[:all,items,sorting]";
    JsonObject response = GET("/eventVisualizations/" + uid + getParams).content();

    assertThat(
        response.get("sorting").toString(), containsString(programStageUid + "." + dimensionUid));
    assertThat(response.get("sorting").toString(), containsString("ASC"));
  }

  @Test
  void testPostMultipleSortingObject() {
    // Given
    String dimension1 = "pe";
    String dimension2 = "ou";
    String sorting =
        "'sorting': [{'dimension': '"
            + dimension1
            + "', 'direction':'ASC'},"
            + "{'dimension': '"
            + dimension2
            + "', 'direction':'DESC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': '"
            + dimension1
            + "'}, {'dimension': '"
            + dimension2
            + "'}],"
            + sorting
            + "}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    String getParams = "?fields=:all,columns[:all,items,sorting]";
    JsonObject response = GET("/eventVisualizations/" + uid + getParams).content();

    assertThat(response.get("sorting").toString(), containsString("pe"));
    assertThat(response.get("sorting").toString(), containsString("ASC"));
    assertThat(response.get("sorting").toString(), containsString("ou"));
    assertThat(response.get("sorting").toString(), containsString("DESC"));
  }

  @Test
  void testPostSortingObjectWithDuplication() {
    // Given
    String dimension = "pe";
    String sorting =
        "'sorting': [{'dimension': '"
            + dimension
            + "', 'direction':'ASC'},"
            + "{'dimension': '"
            + dimension
            + "', 'direction':'DESC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': '"
            + dimension
            + "'}],"
            + sorting
            + "}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    String getParams = "?fields=:all,columns[:all,items,sorting]";
    JsonObject response = GET("/eventVisualizations/" + uid + getParams).content();

    assertThat(response.get("sorting").toString(), containsString("pe"));
    assertThat(response.get("sorting").toString(), containsString("ASC"));
    assertThat(response.get("sorting").toString(), not(containsString("DESC")));
  }

  @Test
  void testPostInvalidSortingObject() {
    // Given
    String invalidDimension = "invalidOne";
    String sorting = "'sorting': [{'dimension': '" + invalidDimension + "', 'direction':'ASC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': 'pe'}],"
            + sorting
            + "}";

    // When
    HttpResponse response = POST("/eventVisualizations/", body);

    // Then
    assertEquals(
        "Sorting dimension ‘" + invalidDimension + "’ is not a column",
        response.error(CONFLICT).getMessage());
  }

  @Test
  void testPostBlankSortingObject() {
    // Given
    String blankDimension = " ";
    String sorting = "'sorting': [{'dimension': '" + blankDimension + "', 'direction':'ASC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': 'pe'}],"
            + sorting
            + "}";

    // When
    HttpResponse response = POST("/eventVisualizations/", body);

    // Then
    assertEquals(
        "Sorting must have a valid dimension and a direction",
        response.error(CONFLICT).getMessage());
  }

  @Test
  void testPostNullSortingObject() {
    // Given
    String blankDimension = " ";
    String sorting = "'sorting': [{'dimension': '" + blankDimension + "', 'direction':'ASC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': 'pe'}],"
            + sorting
            + "}";

    // When
    HttpResponse response = POST("/eventVisualizations/", body);

    // Then
    assertEquals(
        "Sorting must have a valid dimension and a direction",
        response.error(CONFLICT).getMessage());
  }

  @Test
  void testPutSortingObject() {
    // Given
    String dimension = "pe";
    String sorting = "'sorting': [{'dimension': '" + dimension + "', 'direction':'ASC'}]";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': [{'dimension': '"
            + dimension
            + "'}],"
            + sorting
            + "}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    String getParams = "?fields=:all,columns[:all,items,sorting]";
    JsonObject response = GET("/eventVisualizations/" + uid + getParams).content();

    assertThat(response.get("sorting").toString(), containsString("pe"));
    assertThat(response.get("sorting").toString(), containsString("ASC"));

    assertStatus(OK, PUT("/eventVisualizations/" + uid, body));

    // Ensures the sorting remains set.
    response = GET("/eventVisualizations/" + uid + getParams).content();
    assertThat(response.get("sorting").toString(), containsString("pe"));
    assertThat(response.get("sorting").toString(), containsString("ASC"));
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

  @Test
  void testPostMultiPrograms() throws JSONException {
    // Given
    String body =
        """
      {"name": "Test multi-programs post", "type": "LINE_LIST",
      "program": {"id": "deabcdefghP"},
      "trackedEntityType": {"id": "nEenWmSyUEp"},
      "sorting": [
          {
              "dimension": "deabcdefghP[-1].deabcdefghS[0].deabcdefghB",
              "direction": "ASC"
          },
          {
              "dimension": "deabcdefghP.deabcdefghS.deabcdefghB",
              "direction": "DESC"
          }
      ],
      "columns": [
          {
              "dimension": "deabcdefghB",
              "programStage": {
                  "id": "deabcdefghS"
              },
              "program": {
                  "id": "deabcdefghP"
              }
          },
          {
              "dimension": "deabcdefghC",
              "filter": "IN:Female"
          },
          {
              "dimension": "eventDate",
              "program": {
                  "id": "deabcdefghP"
              },
              "items": [
                  {
                      "id": "2023-07-21_2023-08-01"
                  },
                  {
                      "id": "2023-01-21_2023-02-01"
                  }
              ]
          }
       ],
      "filters": [
          {
              "dimension": "ou",
              "programStage": {
                  "id": "deabcdefghS"
              },
              "repetition": {
                  "indexes": [
                      1,
                      2,
                      3,
                      -2,
                      -1,
                      0
                  ]
              },
              "items": [
                  {
                      "id": "ImspTQPwCqd"
                  }
              ]
          },
          {
              "dimension": "deabcdefghE",
              "repetition": {
                  "indexes": [
                      1,
                      2,
                      0
                  ]
              },
              "items": []
          }
       ]
     }""";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    JsonObject response =
        GET("/eventVisualizations/"
                + uid
                + "?fields=*,sorting,filters[:all,items[code, name, sharing, shortName, dimensionItemType, dimensionItem, displayShortName, displayName, displayFormName, id],repetitions],columns[:all,items[:all],repetitions]")
            .content();

    assertThat(response.get("name").node().value(), is(equalTo("Test multi-programs post")));
    assertThat(response.get("type").node().value(), is(equalTo("LINE_LIST")));
    assertThat(response.get("skipRounding").node().value(), is(equalTo(false)));
    assertThat(response.get("legacy").node().value(), is(equalTo(false)));
    assertThat(
        response.get("trackedEntityType").node().value().toString(),
        is(equalTo("""
{"id":"nEenWmSyUEp"}""")));

    JSONAssert.assertEquals(
        """
[{"parent":"COLUMN","dimension":"eventDate","program":"deabcdefghP","values":["2023-07-21_2023-08-01","2023-01-21_2023-02-01"]}]""",
        response.get("simpleDimensions").node().value().toString(),
        false);

    JSONAssert.assertEquals(
        """
[{"dimension":"deabcdefghP[-1].deabcdefghS[0].deabcdefghB","direction":"ASC"},{"dimension":"deabcdefghP.deabcdefghS.deabcdefghB","direction":"DESC"}]""",
        response.get("sorting").node().value().toString(),
        false);

    assertThat(response.get("rows").node().value().toString(), is(equalTo("[]")));
    assertThat(response.get("rowDimensions").node().value().toString(), is(equalTo("[]")));

    assertThat(
        response.get("columnDimensions").node().value().toString(),
        is(
            equalTo(
                """
["deabcdefghP.deabcdefghS.deabcdefghB","deabcdefghC","deabcdefghP.eventDate"]""")));

    assertThat(
        response.get("filterDimensions").node().value().toString(),
        is(equalTo("""
["deabcdefghP.deabcdefghS.ou","deabcdefghE"]""")));

    JSONAssert.assertEquals(
        """
[{"dataElement":{"id":"deabcdefghC"},"filter":"IN:Female"},{"dataElement":{"id":"deabcdefghE"}}]""",
        response.get("dataElementDimensions").node().value().toString(),
        false);

    assertThat(
        response.get("programIndicatorDimensions").node().value().toString(),
        is(equalTo("""
[{"programIndicator":{"id":"deabcdefghB"}}]""")));

    assertThat(
        response.get("organisationUnits").node().value().toString(),
        is(equalTo("""
[{"id":"ImspTQPwCqd"}]""")));

    JSONAssert.assertEquals(
        """
[{"parent":"FILTER","dimension":"ou","program":"deabcdefghP","programStage":"deabcdefghS","indexes":[1,2,3,-2,-1,0]},{"parent":"FILTER","dimension":"deabcdefghE","indexes":[1,2,0]}]""",
        response.get("repetitions").node().value().toString(),
        false);

    JSONAssert.assertEquals(
        """
[{"translations":[],"favorites":[],"sharing":{"external":false,"users":{},"userGroups":{}},"dimensionType":"PROGRAM_INDICATOR","dataDimension":true,"items":[],"allItems":false,"program":{"id":"deabcdefghP"},"dimension":"deabcdefghB","access":{"manage":true,"externalize":true,"write":true,"read":true,"update":true,"delete":true},"favorite":false,"id":"deabcdefghB","attributeValues":[]},{"translations":[],"favorites":[],"sharing":{"external":false,"users":{},"userGroups":{}},"dimensionType":"PROGRAM_DATA_ELEMENT","dataDimension":true,"items":[],"allItems":false,"filter":"IN:Female","dimension":"deabcdefghC","valueType":"INTEGER","access":{"manage":true,"externalize":true,"write":true,"read":true,"update":true,"delete":true},"favorite":false,"id":"deabcdefghC","attributeValues":[]},{"translations":[],"favorites":[],"sharing":{"external":false,"users":{},"userGroups":{}},"dimensionType":"PERIOD","dataDimension":true,"items":[{"code":"2023-07-21_2023-08-01","name":"2023-07-21_2023-08-01","translations":[],"favorites":[],"sharing":{"external":false,"users":{},"userGroups":{}},"legendSets":[],"dimensionItem":"2023-07-21_2023-08-01","access":{"manage":true,"externalize":true,"write":true,"read":true,"update":true,"delete":true},"displayName":"2023-07-21_2023-08-01","favorite":false,"displayFormName":"2023-07-21_2023-08-01","id":"2023-07-21_2023-08-01","attributeValues":[]},{"code":"2023-01-21_2023-02-01","name":"2023-01-21_2023-02-01","translations":[],"favorites":[],"sharing":{"external":false,"users":{},"userGroups":{}},"legendSets":[],"dimensionItem":"2023-01-21_2023-02-01","access":{"manage":true,"externalize":true,"write":true,"read":true,"update":true,"delete":true},"displayName":"2023-01-21_2023-02-01","favorite":false,"displayFormName":"2023-01-21_2023-02-01","id":"2023-01-21_2023-02-01","attributeValues":[]}],"allItems":false,"program":{"id":"deabcdefghP"},"dimension":"eventDate","access":{"manage":true,"externalize":true,"write":true,"read":true,"update":true,"delete":true},"favorite":false,"id":"eventDate","attributeValues":[]}]""",
        response.get("columns").node().value().toString(),
        false);

    JSONAssert.assertEquals(
        """
[{"translations":[],"favorites":[],"sharing":{"external":false,"users":{},"userGroups":{}},"dimensionType":"ORGANISATION_UNIT","dataDimension":true,"items":[{"code":"OrganisationUnitCodeA","name":"OrganisationUnitA","sharing":{"external":false,"users":{},"userGroups":{}},"shortName":"OrganisationUnitShortA","dimensionItemType":"ORGANISATION_UNIT","dimensionItem":"ImspTQPwCqd","displayShortName":"OrganisationUnitShortA","displayName":"OrganisationUnitA","displayFormName":"OrganisationUnitA","id":"ImspTQPwCqd"}],"allItems":false,"programStage":{"id":"deabcdefghS"},"program":{"id":"deabcdefghP"},"dimension":"ou","access":{"manage":true,"externalize":true,"write":true,"read":true,"update":true,"delete":true},"favorite":false,"id":"ou","attributeValues":[],"repetition":{"parent":"FILTER","dimension":"ou","program":"deabcdefghP","programStage":"deabcdefghS","indexes":[1,2,3,-2,-1,0]}},{"translations":[],"favorites":[],"sharing":{"external":false,"users":{},"userGroups":{}},"dimensionType":"PROGRAM_DATA_ELEMENT","dataDimension":true,"items":[],"allItems":false,"dimension":"deabcdefghE","valueType":"INTEGER","access":{"manage":true,"externalize":true,"write":true,"read":true,"update":true,"delete":true},"favorite":false,"id":"deabcdefghE","attributeValues":[],"repetition":{"parent":"FILTER","dimension":"deabcdefghE","indexes":[1,2,0]}}]""",
        response.get("filters").node().value().toString(),
        false);
  }

  @Test
  void testGetDataForNonAllowedType() {
    // Given
    String body =
        """
          {"name": "Test multi-programs post", "type": "LINE_LIST",
          "program": {"id": "deabcdefghP"}
          }""";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    JsonError error = GET("/eventVisualizations/" + uid + "/data").error(CONFLICT);
    assertEquals("ERROR", error.getStatus());
    assertEquals("Cannot generate chart for LINE_LIST", error.getMessage());
  }

  @Test
  void testGetDataForMultiProgram() {
    // Given
    String body =
        """
              {"name": "Test multi-programs post", "type": "STACKED_COLUMN",
              "program": {"id": "deabcdefghP"},
              "trackedEntityType": {"id": "nEenWmSyUEp"}
              }""";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    JsonError error = GET("/eventVisualizations/" + uid + "/data").error(CONFLICT);
    assertEquals("ERROR", error.getStatus());
    assertEquals(
        "Cannot generate chart for multi-program visualization " + uid, error.getMessage());
  }
}
