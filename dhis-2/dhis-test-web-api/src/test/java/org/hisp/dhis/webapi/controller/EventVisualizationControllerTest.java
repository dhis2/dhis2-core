/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.dataelement.DataElementDomain.TRACKER;
import static org.hisp.dhis.eventvisualization.Attribute.COLUMN;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.LINE;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpStatus.BAD_REQUEST;
import static org.hisp.dhis.http.HttpStatus.CONFLICT;
import static org.hisp.dhis.http.HttpStatus.CREATED;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventvisualization.EventRepetition;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonError;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controller tests for {@link org.hisp.dhis.webapi.controller.event.EventVisualizationController}.
 *
 * @author maikel arabori
 */
@Transactional
class EventVisualizationControllerTest extends H2ControllerIntegrationTestBase {
  @Autowired private ObjectMapper jsonMapper;

  @Autowired private IdentifiableObjectManager manager;

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
  void testPostForOuDimensionAsFilter() throws JsonProcessingException {
    // Given
    DataElement ouDe = createDataElement('D', ValueType.TEXT, AggregationType.SUM, TRACKER);
    ouDe.setValueType(ValueType.ORGANISATION_UNIT);
    POST("/dataElements", jsonMapper.writeValueAsString(ouDe)).content(CREATED);

    String createPayload =
        """
            {
              "type": "LINE_LIST",
              "outputType": "EVENT",
              "program": {
            	"id": "%s"
              },
              "programStage": {
            	"id": "%s"
              },
              "columns": [
            	{
            	  "dimension": "ou",
            	  "items": [
            		{
            		  "id": "USER_ORGUNIT"
            		}
            	  ]
            	},
            	{
            	  "dimension": "lastUpdated",
            	  "items": [
            		{
            		  "id": "THIS_MONTH"
            		}
            	  ]
            	},
            	{
            	  "dimension": "%s",
            	  "filter": "EQ:%s",
            	  "programStage": {
            		"id": "%s"
            	  }
            	}
              ],
              "rows": [],
              "filters": [],
              "displayDensity": "NORMAL",
              "fontSize": "NORMAL",
              "digitGroupSeparator": "SPACE",
              "showHierarchy": false,
              "skipRounding": false,
              "legend": {},
              "name": "OU Filter"
            }
            """
            .formatted(
                mockProgram.getUid(),
                mockProgramStage.getUid(),
                ouDe.getUid(),
                mockOrganisationUnit.getUid(),
                mockProgramStage.getUid());

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", createPayload));

    // Then
    JsonObject response = GET("/eventVisualizations/" + uid).content();
    System.out.println(response.toString());

    assertThat(
        response.get("parentGraphMap").toString(), containsString(mockOrganisationUnit.getUid()));
  }

  @Test
  void testDelete() {
    // Given
    String eventDateDimension = "eventDate";
    String eventDate = "2021-07-21_2021-08-01";
    String dimensionBody =
        "{'dimension': '" + eventDateDimension + "', 'items': [{'id': '" + eventDate + "'}]}";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN','eventRepetitions':null, 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': ["
            + dimensionBody
            + "]}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    DELETE("/eventVisualizations/" + uid).content(OK);
  }

  @Test
  void testPostForMultiEventDates() {
    // Given
    String eventDateDimension = "eventDate";
    String eventDate = "2021-07-21_2021-08-01";
    String incidentDateDimension = "incidentDate";
    String incidentDate = "2021-07-21_2021-08-01";
    String createdDateDimension = "createdDate";
    String createdDate = "2021-07-21_2021-08-01";
    String completedDateDimension = "completedDate";
    String completedDate = "2021-07-21_2021-08-01";
    String eventDateBody =
        "{'dimension': '" + eventDateDimension + "', 'items': [{'id': '" + eventDate + "'}]}";
    String incidentDateBody =
        "{'dimension': '" + incidentDateDimension + "', 'items': [{'id': '" + incidentDate + "'}]}";
    String createdDateBody =
        "{'dimension': '" + createdDateDimension + "', 'items': [{'id': '" + createdDate + "'}]}";
    String completedDateBody =
        "{'dimension': '"
            + completedDateDimension
            + "', 'items': [{'id': '"
            + completedDate
            + "'}]}";
    String body =
        "{'name': 'Name Test', 'type': 'STACKED_COLUMN', 'program': {'id':'"
            + mockProgram.getUid()
            + "'}, 'rows': ["
            + eventDateBody
            + ","
            + incidentDateBody
            + ","
            + createdDateBody
            + ","
            + completedDateBody
            + "]}";

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    JsonObject response = GET("/eventVisualizations/" + uid).content();

    assertThat(response.get("simpleDimensions").toString(), containsString("ROW"));
    assertThat(response.get("simpleDimensions").toString(), containsString(eventDate));
    assertThat(response.get("simpleDimensions").toString(), containsString(incidentDate));
    assertThat(response.get("simpleDimensions").toString(), containsString(createdDate));
    assertThat(response.get("simpleDimensions").toString(), containsString(completedDate));
    assertThat(response.get("rows").toString(), containsString(eventDateDimension));
    assertThat(response.get("rows").toString(), containsString(incidentDateDimension));
    assertThat(response.get("rows").toString(), containsString(createdDateDimension));
    assertThat(response.get("rows").toString(), containsString(completedDateDimension));
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
  @Disabled("Only runs locally")
  void testLoadColumnWithRepetitionWithoutProgram() {
    // Given
    EventRepetition repetitionNoProgram = new EventRepetition();
    repetitionNoProgram.setDimension("pe");
    repetitionNoProgram.setParent(COLUMN);
    repetitionNoProgram.setIndexes(List.of(-1, 0, 2));

    BaseDimensionalObject dim = new BaseDimensionalObject();
    dim.setEventRepetition(repetitionNoProgram);

    String evUid = "XSnivU7HgpA";
    EventVisualization eventVisualization = new EventVisualization("Test");
    eventVisualization.setProgram(mockProgram);
    eventVisualization.setType(LINE);
    eventVisualization.setUid(evUid);
    eventVisualization.setColumns(List.of(dim));
    eventVisualization.setEventRepetitions(List.of(repetitionNoProgram));

    manager.save(eventVisualization);

    // When
    String getParams = "?fields=:all,columns[:all,items,repetitions]";
    JsonObject response = GET("/eventVisualizations/" + evUid + getParams).content();

    // Then
    String repetitionIndexes = repetitionNoProgram.getIndexes().toString().replace(" ", "");

    assertThat(response.get("repetitions").toString(), containsString("COLUMN"));
    assertThat(response.get("repetitions").toString(), containsString(repetitionIndexes));
    assertThat(response.get("repetitions").toString(), containsString("pe"));
    assertThat(response.get("columns").toString(), containsString(repetitionIndexes));
    assertThat(response.get("rows").toString(), not(containsString(repetitionIndexes)));
    assertThat(response.get("filters").toString(), not(containsString(repetitionIndexes)));
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
  void testPostMultiPrograms() {
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
              "dimension": "deabcdefghP.deabcdefghB",
              "direction": "DESC"
          }
      ],
      "columns": [
          {
              "dimension": "deabcdefghB",
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
          },
          {
            "dimension": "created",
            "items": [
                {
                    "id": "2021-01-21_2021-02-01"
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
                + "?fields=*,sorting,filters[dimension,programStage,repetition[:all],items[code,name,dimensionItemType,dimensionItem,id],repetitions],columns[dimension,program,programStage,items[id],repetitions]")
            .content();

    assertThat(response.get("name").node().value(), is(equalTo("Test multi-programs post")));
    assertThat(response.get("type").node().value(), is(equalTo("LINE_LIST")));
    assertThat(response.get("skipRounding").node().value(), is(equalTo(false)));
    assertThat(response.get("legacy").node().value(), is(equalTo(false)));
    assertThat(
        response.get("trackedEntityType").node().value().toString(),
        is(
            equalTo(
"""
{"id":"nEenWmSyUEp"}""")));

    JsonNode simpleDimensionNode0 = response.get("simpleDimensions").node().element(0);
    assertThat(simpleDimensionNode0.get("parent").value().toString(), is(equalTo("COLUMN")));
    assertThat(simpleDimensionNode0.get("dimension").value().toString(), is(equalTo("eventDate")));
    assertThat(simpleDimensionNode0.get("program").value().toString(), is(equalTo("deabcdefghP")));
    assertThat(
        simpleDimensionNode0.get("values").value().toString(),
        is(
            equalTo(
"""
["2023-07-21_2023-08-01","2023-01-21_2023-02-01"]""")));
    assertThat(simpleDimensionNode0.get("parent").value().toString(), is(equalTo("COLUMN")));

    JsonNode sortingNode0 = response.get("sorting").node().element(0);
    assertThat(
        sortingNode0.get("dimension").value().toString(),
        is(equalTo("deabcdefghP[-1].deabcdefghS[0].deabcdefghB")));
    assertThat(sortingNode0.get("direction").value().toString(), is(equalTo("ASC")));

    JsonNode sortingNode1 = response.get("sorting").node().element(1);
    assertThat(
        sortingNode1.get("dimension").value().toString(), is(equalTo("deabcdefghP.deabcdefghB")));
    assertThat(sortingNode1.get("direction").value().toString(), is(equalTo("DESC")));

    assertThat(response.get("rows").node().value().toString(), is(equalTo("[]")));
    assertThat(response.get("rowDimensions").node().value().toString(), is(equalTo("[]")));

    assertThat(
        response.get("columnDimensions").node().value().toString(),
        is(
            equalTo(
"""
["deabcdefghP.deabcdefghB","deabcdefghC","deabcdefghP.eventDate","created"]""")));

    assertThat(
        response.get("filterDimensions").node().value().toString(),
        is(
            equalTo(
"""
["deabcdefghP.deabcdefghS.ou","deabcdefghE"]""")));

    JsonNode dataElementDimensionsNode0 = response.get("dataElementDimensions").node().element(0);
    assertThat(
        dataElementDimensionsNode0.get("dataElement").value().toString(),
        is(
            equalTo(
"""
{"id":"deabcdefghC"}""")));
    assertThat(
        dataElementDimensionsNode0.get("filter").value().toString(), is(equalTo("IN:Female")));

    JsonNode dataElementDimensionsNode1 = response.get("dataElementDimensions").node().element(1);
    assertThat(
        dataElementDimensionsNode1.get("dataElement").value().toString(),
        is(
            equalTo(
"""
{"id":"deabcdefghE"}""")));
    assertFalse(dataElementDimensionsNode1.isMember("filter"));

    assertThat(
        response.get("programIndicatorDimensions").node().value().toString(),
        is(
            equalTo(
"""
[{"programIndicator":{"id":"deabcdefghB"}}]""")));

    assertThat(
        response.get("organisationUnits").node().value().toString(),
        is(
            equalTo(
"""
[{"id":"ImspTQPwCqd"}]""")));

    JsonNode repetitionsNode0 = response.get("repetitions").node().element(0);
    assertThat(repetitionsNode0.get("parent").value().toString(), is(equalTo("FILTER")));
    assertThat(repetitionsNode0.get("dimension").value().toString(), is(equalTo("ou")));
    assertThat(repetitionsNode0.get("program").value().toString(), is(equalTo("deabcdefghP")));
    assertThat(repetitionsNode0.get("programStage").value().toString(), is(equalTo("deabcdefghS")));
    assertThat(repetitionsNode0.get("indexes").value().toString(), is(equalTo("[1,2,3,-2,-1,0]")));

    JsonNode repetitionsNode1 = response.get("repetitions").node().element(1);
    assertThat(repetitionsNode1.get("parent").value().toString(), is(equalTo("FILTER")));
    assertThat(repetitionsNode1.get("dimension").value().toString(), is(equalTo("deabcdefghE")));
    assertFalse(repetitionsNode1.isMember("program"));
    assertFalse(repetitionsNode1.isMember("programStage"));
    assertThat(repetitionsNode1.get("indexes").value().toString(), is(equalTo("[1,2,0]")));

    JsonNode columnsNode0 = response.get("columns").node().element(0);
    assertThat(columnsNode0.get("items").value().toString(), is(equalTo("[]")));
    assertThat(
        columnsNode0.get("program").value().toString(),
        is(
            equalTo(
"""
{"id":"deabcdefghP"}""")));
    assertThat(columnsNode0.get("dimension").value().toString(), is(equalTo("deabcdefghB")));

    JsonNode columnsNode1 = response.get("columns").node().element(1);
    assertThat(columnsNode1.get("items").value().toString(), is(equalTo("[]")));
    assertFalse(columnsNode1.isMember("program"));
    assertThat(columnsNode1.get("dimension").value().toString(), is(equalTo("deabcdefghC")));

    JsonNode columnsNode2 = response.get("columns").node().element(2);
    assertThat(
        columnsNode2.get("items").value().toString(),
        is(
            equalTo(
"""
[{"id":"2023-07-21_2023-08-01"},{"id":"2023-01-21_2023-02-01"}]""")));
    assertThat(
        columnsNode2.get("program").value().toString(),
        is(
            equalTo(
"""
{"id":"deabcdefghP"}""")));
    assertThat(columnsNode2.get("dimension").value().toString(), is(equalTo("eventDate")));

    JsonNode columnsNode3 = response.get("columns").node().element(3);
    assertThat(
        columnsNode3.get("items").value().toString(),
        is(
            equalTo(
"""
[{"id":"2021-01-21_2021-02-01"}]""")));
    assertThat(columnsNode3.get("dimension").value().toString(), is(equalTo("created")));

    JsonNode filtersNode0 = response.get("filters").node().element(0);
    assertThat(
        filtersNode0.get("items").element(0).get("dimensionItem").value().toString(),
        is(equalTo("ImspTQPwCqd")));
    assertThat(
        filtersNode0.get("items").element(0).get("id").value().toString(),
        is(equalTo("ImspTQPwCqd")));
    assertThat(
        filtersNode0.get("programStage").value().toString(),
        is(
            equalTo(
"""
{"id":"deabcdefghS"}""")));
    assertThat(filtersNode0.get("dimension").value().toString(), is(equalTo("ou")));
    assertThat(
        filtersNode0.get("repetition").get("parent").value().toString(), is(equalTo("FILTER")));
    assertThat(
        filtersNode0.get("repetition").get("dimension").value().toString(), is(equalTo("ou")));
    assertThat(
        filtersNode0.get("repetition").get("program").value().toString(),
        is(equalTo("deabcdefghP")));
    assertThat(
        filtersNode0.get("repetition").get("programStage").value().toString(),
        is(equalTo("deabcdefghS")));
    assertThat(
        filtersNode0.get("repetition").get("indexes").value().toString(),
        is(equalTo("[1,2,3,-2,-1,0]")));

    JsonNode filtersNode1 = response.get("filters").node().element(1);
    assertThat(filtersNode1.get("items").value().toString(), is(equalTo("[]")));
    assertThat(filtersNode1.get("dimension").value().toString(), is(equalTo("deabcdefghE")));
    assertThat(
        filtersNode1.get("repetition").get("parent").value().toString(), is(equalTo("FILTER")));
    assertThat(
        filtersNode1.get("repetition").get("dimension").value().toString(),
        is(equalTo("deabcdefghE")));
    assertFalse(filtersNode1.get("repetition").isMember("program"));
    assertFalse(filtersNode1.get("repetition").isMember("programStage"));
    assertThat(
        filtersNode1.get("repetition").get("indexes").value().toString(), is(equalTo("[1,2,0]")));
    assertEquals("[{\"id\":\"deabcdefghP\"}]", response.get("programDimensions").toString());
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

  @Test
  void testGetMetaDataObject() {
    // Given
    OrganisationUnitGroup organisationUnitGroup = createOrganisationUnitGroup('A');
    organisationUnitGroup.setUid("CXw2yu5fodb");
    manager.save(organisationUnitGroup);

    DataElement ouDe = createDataElement('D', ValueType.TEXT, AggregationType.SUM, TRACKER);
    ouDe.setUid("Zj7UnCAulEk");
    ouDe.setValueType(ValueType.ORGANISATION_UNIT);
    manager.save(ouDe);

    String body =
        """
              {"name": "Test metadata post", "type": "STACKED_COLUMN",
              "program": {"id": "deabcdefghP"},
              "trackedEntityType": {"id": "nEenWmSyUEp"},
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
                  },
                  {
                      "dimension": "Zj7UnCAulEk",
                      "filter": "IN:OU_GROUP-CXw2yu5fodb"
                   }
               ]
              }
              """;

    // When
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    JsonObject response = GET("/eventVisualizations/" + uid).content();
    String metaData = response.get("metaData").node().value().toString();

    assertThat(response.get("name").node().value(), is(equalTo("Test metadata post")));
    assertThat(
        metaData,
        containsString(
            "\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OrganisationUnitCodeA\",\"name\":\"OrganisationUnitA\"}"));
    assertThat(
        metaData,
        containsString(
            "{\"uid\":\"CXw2yu5fodb\",\"code\":\"OrganisationUnitGroupCodeA\",\"name\":\"OrganisationUnitGroupA\"}"));
    assertThat(
        metaData,
        containsString(
            "\"deabcdefghE\":{\"uid\":\"deabcdefghE\",\"code\":\"DataElementCodeB\",\"name\":\"DataElementB\"}"));
    assertThat(
        metaData,
        containsString(
            "{\"uid\":\"Zj7UnCAulEk\",\"code\":\"DataElementCodeD\",\"name\":\"DataElementD\"}"));
    assertThat(response.get("type").node().value(), is(equalTo("STACKED_COLUMN")));
    assertThat(response.get("program").node().get("id").value(), is(equalTo(mockProgram.getUid())));
  }
}
