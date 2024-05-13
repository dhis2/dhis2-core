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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.web.HttpStatus.BAD_REQUEST;
import static org.hisp.dhis.web.HttpStatus.CREATED;
import static org.hisp.dhis.web.HttpStatus.OK;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.web.WebClient;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller tests for {@link org.hisp.dhis.webapi.controller.event.EventReportController}.
 *
 * @author maikel arabori
 */
class EventReportControllerTest extends DhisControllerConvenienceTest {

  @Autowired private IdentifiableObjectManager manager;

  private Program mockProgram;

  @BeforeEach
  public void beforeEach() {
    mockProgram = createProgram('A');
    manager.save(mockProgram);
  }

  @Test
  void testPostForSingleEventDate() {
    // Given
    final String eventDateDimension = "eventDate";
    final String eventDate = "2021-07-21_2021-08-01";
    final String dimensionBody =
        "{'dimension': '" + eventDateDimension + "', 'items': [{'id': '" + eventDate + "'}]}";
    final String body =
        "{'name': 'Name Test', 'type':'LINE_LIST', 'program':{'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': ["
            + dimensionBody
            + "]}";

    // When
    final String uid = assertStatus(CREATED, POST("/eventReports/", body));

    // Then
    final JsonObject response = GET("/eventVisualizations/" + uid).content();

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
    final String eventDateDimension = "eventDate";
    final String incidentDateDimension = "incidentDate";
    final String eventDate = "2021-07-21_2021-08-01";
    final String incidentDate = "2021-07-21_2021-08-01";
    final String eventDateBody =
        "{'dimension': '" + eventDateDimension + "', 'items': [{'id': '" + eventDate + "'}]}";
    final String incidentDateBody =
        "{'dimension': '" + incidentDateDimension + "', 'items': [{'id': '" + incidentDate + "'}]}";
    final String body =
        "{'name': 'Name Test', 'type':'LINE_LIST', 'program':{'id':'"
            + mockProgram.getUid()
            + "'}, 'rows': ["
            + eventDateBody
            + ","
            + incidentDateBody
            + "]}";

    // When
    final String uid = assertStatus(CREATED, POST("/eventReports/", body));

    // Then
    final JsonObject response = GET("/eventReports/" + uid).content();

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
    final String invalidDimension = "invalidDimension";
    final String eventDate = "2021-07-21_2021-08-01";
    final String dimensionBody =
        "{'dimension': '" + invalidDimension + "', 'items': [{'id': '" + eventDate + "'}]}";
    final String body =
        "{'name': 'Name Test', 'type':'LINE_LIST', 'program':{'id':'"
            + mockProgram.getUid()
            + "'}, 'columns': ["
            + dimensionBody
            + "]}";

    // When
    final String uid = assertStatus(CREATED, POST("/eventReports/", body));

    // Then
    assertEquals(
        "Not a valid dimension: " + invalidDimension,
        GET("/eventReports/" + uid).error(BAD_REQUEST).getMessage());
  }

  @Test
  void testThatGetEventVisualizationsContainsLegacyEventReports() {
    // Given
    final String body =
        "{'name': 'Name Test', 'type':'LINE_LIST', 'program':{'id':'"
            + mockProgram.getUid()
            + "'}}";

    // When
    final String uid = assertStatus(CREATED, POST("/eventReports/", body));

    // Then
    final JsonObject response = GET("/eventVisualizations/" + uid).content();

    assertThat(response.get("name").toString(), containsString("Name Test"));
    assertThat(response.get("type").toString(), containsString("LINE_LIST"));
  }

  @Test
  void testThatGetEventReportsDoesNotContainNewEventVisualizations() {
    // Given
    final String body =
        "{'name': 'Name Test', 'type':'LINE_LIST', 'program':{'id':'"
            + mockProgram.getUid()
            + "'}}";

    // When
    final String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    assertTrue(GET("/eventReports/" + uid).content().isEmpty());
  }

  @Test
  void testEventReportRelativePeriods() {
    String body =
        String.format(
            """
        {"name": "Name Test",
        "type":"LINE_LIST",
        "program":{"id":"%s"},
        "columns": [{
          "dimension": "%s",
          "items": [{"id": "%s"}]}],
          "filters":[{
                "items":[{
                    "name": "THIS_YEAR",
                    "dimensionItemType": "PERIOD",
                    "displayShortName": "LAST_12_MONTHS",
                    "displayName": "LAST 12 MONTHS",
                    "id": "LAST_12_MONTHS"}],
                "dimension": "pe"}]}""",
            mockProgram.getUid(), "eventDate", "2021-07-21_2021-08-01");
    String uid = assertStatus(CREATED, POST("/eventReports/", body));
    final JsonObject response = GET("/eventVisualizations/" + uid).content();
    assertTrue(response.getObject("relativePeriods").getBoolean("last12Months").booleanValue());
  }

  @Test
  void testReportRelativePeriods() {
    String body =
        """
            {"name": "Name Test", "relativePeriods": {"last12Months": true}}""";
    String uid = assertStatus(CREATED, POST("/reports/", body));
    final JsonObject response = GET("/reports/" + uid).content();
    assertTrue(response.getObject("relativePeriods").getBoolean("last12Months").booleanValue());
  }

  @Test
  void testEventChartRelativePeriods() {
    String body =
        String.format(
            """
        {"name": "Name Test",
        "type":"BAR",
        "program":{"id":"%s"},
        "rows":[{
              "dimension": "pe",
              "items":[{
                  "id": "LAST_12_MONTHS"}]}]}""",
            mockProgram.getUid());
    String uid = assertStatus(CREATED, POST("/eventCharts/", body));
    final JsonObject response = GET("/eventVisualizations/" + uid).content();
    assertTrue(response.getObject("relativePeriods").getBoolean("last12Months").booleanValue());
  }

  @Test
  void testEventVisualizationRelativePeriods() {
    String body =
        String.format(
            """
        {"name": "Name Test",
        "type":"BAR",
        "program":{"id":"%s"},
        "rows":[{
              "dimension": "pe",
              "items":[{
                  "id": "LAST_12_MONTHS"}]}]}""",
            mockProgram.getUid());
    String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));
    final JsonObject response = GET("/eventVisualizations/" + uid).content();
    assertTrue(response.getObject("relativePeriods").getBoolean("last12Months").booleanValue());
  }

  @Test
  void testMapViewRelativePeriods() {
    assertStatus(OK, POST("/metadata/", WebClient.Body("metadata/map_new.json")));
    final JsonObject response = GET("/mapViews/zyFOjTfzLws").content();
    assertTrue(response.getObject("relativePeriods").getBoolean("last12Months").booleanValue());
  }

  @Test
  void testReportPdf() {
    String body =
        """
            {"name": "Name Test", "relativePeriods": {"last12Months": true},
            "designContent": "<?xml version=\\"1.0\\" encoding=\\"UTF-8\\"?>\\n<jasperReport xmlns=\\"http://jasperreports.sourceforge.net/jasperreports\\" xmlns:xsi=\\"http://www.w3.org/2001/XMLSchema-instance\\" xsi:schemaLocation=\\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\\" name=\\"dpt\\" pageWidth=\\"595\\" pageHeight=\\"842\\" columnWidth=\\"555\\" leftMargin=\\"20\\" rightMargin=\\"20\\" topMargin=\\"20\\" bottomMargin=\\"20\\"></jasperReport>"
            }""";
    String uid = assertStatus(CREATED, POST("/reports/", body));
    assertFalse(
        GET("/reports/" + uid + "/data.pdf?t=1715330660314&ou=ImspTQPwCqd&pe=2023&date=2023-01-01")
            .content("application/pdf")
            .isEmpty());
  }
}
