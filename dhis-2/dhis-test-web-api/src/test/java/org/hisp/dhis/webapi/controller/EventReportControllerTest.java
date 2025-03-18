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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpStatus.BAD_REQUEST;
import static org.hisp.dhis.http.HttpStatus.CREATED;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controller tests for {@link org.hisp.dhis.webapi.controller.event.EventReportController}.
 *
 * @author maikel arabori
 */
@Transactional
class EventReportControllerTest extends H2ControllerIntegrationTestBase {

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
    assertStatus(OK, POST("/metadata/", Path.of("metadata/map_new.json")));
    final JsonObject response = GET("/mapViews/zyFOjTfzLws").content();
    assertTrue(response.getObject("relativePeriods").getBoolean("last12Months").booleanValue());
  }

  @Test
  void testReportPdf() {
    String body =
        """
            {"name": "Name Test", "relativePeriods": {"last12Months": true},
            "designContent": "<?xml version=\\"1.0\\" encoding=\\"UTF-8\\"?> <jasperReport xmlns=\\"http://jasperreports.sourceforge.net/jasperreports\\" xmlns:xsi=\\"http://www.w3.org/2001/XMLSchema-instance\\" xsi:schemaLocation=\\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\\" name=\\"dpt\\" pageWidth=\\"595\\" pageHeight=\\"842\\" columnWidth=\\"555\\" leftMargin=\\"20\\" rightMargin=\\"20\\" topMargin=\\"20\\" bottomMargin=\\"20\\" uuid=\\"17839606-ae6d-42a7-8501-a4137adad2c1\\"> <property name=\\"ireport.zoom\\" value=\\"1.5\\"/> <property name=\\"ireport.x\\" value=\\"14\\"/> <property name=\\"ireport.y\\" value=\\"0\\"/> <field name=\\"organisationunitid\\" class=\\"java.lang.Integer\\"/> <field name=\\"organisationunitname\\" class=\\"java.lang.String\\"/> <field name=\\"reporting_month_name\\" class=\\"java.lang.String\\"/> <field name=\\"param_organisationunit_name\\" class=\\"java.lang.String\\"/> <field name=\\"organisation_unit_is_parent\\" class=\\"java.lang.String\\"/> <field name=\\"anc 1st visit_year\\" class=\\"java.lang.Double\\"/> <field name=\\"anc 2nd visit_year\\" class=\\"java.lang.Double\\"/> <field name=\\"anc 3rd visit_year\\" class=\\"java.lang.Double\\"/> <field name=\\"anc 4th or more_year\\" class=\\"java.lang.Double\\"/> <background> <band splitType=\\"Stretch\\"/> </background> <title> <band height=\\"326\\" splitType=\\"Stretch\\"> <staticText> <reportElement uuid=\\"1876010e-28c0-499a-bf70-aea9f6040d7b\\" x=\\"12\\" y=\\"15\\" width=\\"532\\" height=\\"41\\" forecolor=\\"#184F73\\"/> <textElement textAlignment=\\"Center\\"> <font size=\\"24\\"/> </textElement> <text><![CDATA[ANC Visits]]></text> </staticText> <bar3DChart> <chart evaluationTime=\\"Report\\"> <reportElement uuid=\\"fe511602-c6ba-4e97-a653-8acd9b61d305\\" x=\\"12\\" y=\\"114\\" width=\\"532\\" height=\\"210\\"/> <chartTitle/> <chartSubtitle/> <chartLegend/> </chart> <categoryDataset> <dataset> <incrementWhenExpression><![CDATA[$F{organisation_unit_is_parent}.equals( \\"Yes\\" )]]></incrementWhenExpression> </dataset> <categorySeries> <seriesExpression><![CDATA[\\"ANC 1st visit\\"]]></seriesExpression> <categoryExpression><![CDATA[\\"\\"]]></categoryExpression> <valueExpression><![CDATA[$F{anc 1st visit_year}]]></valueExpression> </categorySeries> <categorySeries> <seriesExpression><![CDATA[\\"ANC 2nd visit\\"]]></seriesExpression> <categoryExpression><![CDATA[\\"\\"]]></categoryExpression> <valueExpression><![CDATA[$F{anc 2nd visit_year}]]></valueExpression> </categorySeries> <categorySeries> <seriesExpression><![CDATA[\\"ANC 3rd visit\\"]]></seriesExpression> <categoryExpression><![CDATA[\\"\\"]]></categoryExpression> <valueExpression><![CDATA[$F{anc 3rd visit_year}]]></valueExpression> </categorySeries> <categorySeries> <seriesExpression><![CDATA[\\"ANC >=4 visits\\"]]></seriesExpression> <categoryExpression><![CDATA[\\"\\"]]></categoryExpression> <valueExpression><![CDATA[$F{anc 4th or more_year}]]></valueExpression> </categorySeries> </categoryDataset> <bar3DPlot isShowLabels=\\"true\\"> <plot/> <itemLabel color=\\"#000000\\" backgroundColor=\\"#FFFFFF\\"/> </bar3DPlot> </bar3DChart> <textField> <reportElement uuid=\\"0aeb7c9b-04ad-43d7-82e9-ca53dc04b3fc\\" x=\\"10\\" y=\\"68\\" width=\\"253\\" height=\\"20\\"/> <textElement textAlignment=\\"Right\\"> <font size=\\"13\\"/> </textElement> <textFieldExpression><![CDATA[$F{reporting_month_name}]]></textFieldExpression> </textField> <textField> <reportElement uuid=\\"a25c78a8-61a7-4003-a10f-1dbfbbdc43ac\\" x=\\"287\\" y=\\"68\\" width=\\"255\\" height=\\"20\\"/> <textElement> <font size=\\"13\\"/> </textElement> <textFieldExpression><![CDATA[$F{param_organisationunit_name}]]></textFieldExpression> </textField> <staticText> <reportElement uuid=\\"fb9533bc-db90-43bb-aad2-722abae6b51d\\" x=\\"263\\" y=\\"68\\" width=\\"24\\" height=\\"20\\"/> <textElement textAlignment=\\"Center\\"> <font size=\\"13\\"/> </textElement> <text><![CDATA[-]]></text> </staticText> </band> </title> <pageHeader> <band height=\\"15\\" splitType=\\"Stretch\\"/> </pageHeader> <columnHeader> <band height=\\"51\\" splitType=\\"Stretch\\"> <staticText> <reportElement uuid=\\"6e085c2a-f2f0-4b51-951c-3d0f0a117a92\\" x=\\"89\\" y=\\"26\\" width=\\"129\\" height=\\"20\\"/> <textElement> <font size=\\"11\\" isBold=\\"true\\"/> </textElement> <text><![CDATA[Organisation unit]]></text> </staticText> <staticText> <reportElement uuid=\\"a575c888-3f49-4840-9aa3-0a84e6d35a78\\" x=\\"218\\" y=\\"26\\" width=\\"55\\" height=\\"20\\"/> <textElement textAlignment=\\"Center\\"> <font size=\\"11\\" isBold=\\"true\\"/> </textElement> <text><![CDATA[ANC 1st visit]]></text> </staticText> <staticText> <reportElement uuid=\\"6f1d357d-5f36-4b1f-9a3a-0c8a51b318ec\\" x=\\"273\\" y=\\"26\\" width=\\"55\\" height=\\"20\\"/> <textElement textAlignment=\\"Center\\"> <font size=\\"11\\" isBold=\\"true\\"/> </textElement> <text><![CDATA[ANC 2nd visit]]></text> </staticText> <staticText> <reportElement uuid=\\"a21c2500-87f5-4509-a766-dbdc9dcec98f\\" x=\\"328\\" y=\\"26\\" width=\\"55\\" height=\\"20\\"/> <textElement textAlignment=\\"Center\\"> <font size=\\"11\\" isBold=\\"true\\"/> </textElement> <text><![CDATA[ANC 3rd visit]]></text> </staticText> <staticText> <reportElement uuid=\\"50047dbb-126f-4099-b49a-960e1cd09e33\\" x=\\"383\\" y=\\"26\\" width=\\"55\\" height=\\"20\\"/> <textElement textAlignment=\\"Center\\"> <font size=\\"11\\" isBold=\\"true\\"/> </textElement> <text><![CDATA[ANC >=4 visits]]></text> </staticText> <line> <reportElement uuid=\\"43f67873-03e8-4b94-bca3-8882afbbad7e\\" x=\\"73\\" y=\\"45\\" width=\\"381\\" height=\\"1\\"/> </line> </band> </columnHeader> <detail> <band height=\\"21\\" splitType=\\"Stretch\\"> <textField isBlankWhenNull=\\"true\\"> <reportElement uuid=\\"8088fed7-0677-4ffd-8f04-0dbf5d39ad95\\" x=\\"89\\" y=\\"0\\" width=\\"129\\" height=\\"20\\"/> <textElement> <font size=\\"9\\"/> </textElement> <textFieldExpression><![CDATA[$F{organisationunitname}]]></textFieldExpression> </textField> <textField pattern=\\"###0\\" isBlankWhenNull=\\"true\\"> <reportElement uuid=\\"ee6ce247-f961-4b3f-ba8e-c98c9e6e33ab\\" x=\\"218\\" y=\\"0\\" width=\\"55\\" height=\\"20\\"/> <textElement textAlignment=\\"Center\\"> <font size=\\"9\\"/> </textElement> <textFieldExpression><![CDATA[$F{anc 1st visit_year}]]></textFieldExpression> </textField> <textField pattern=\\"###0\\" isBlankWhenNull=\\"true\\"> <reportElement uuid=\\"ab1f1ec1-e403-48bf-a2fd-70b5fd73b3b3\\" x=\\"273\\" y=\\"0\\" width=\\"55\\" height=\\"20\\"/> <textElement textAlignment=\\"Center\\"> <font size=\\"9\\"/> </textElement> <textFieldExpression><![CDATA[$F{anc 2nd visit_year}]]></textFieldExpression> </textField> <textField pattern=\\"###0\\" isBlankWhenNull=\\"true\\"> <reportElement uuid=\\"5ec61f4a-c211-4d92-b585-ed7310d33acc\\" x=\\"328\\" y=\\"0\\" width=\\"55\\" height=\\"20\\"/> <textElement textAlignment=\\"Center\\"> <font size=\\"9\\"/> </textElement> <textFieldExpression><![CDATA[$F{anc 3rd visit_year}]]></textFieldExpression> </textField> <textField pattern=\\"###0\\" isBlankWhenNull=\\"true\\"> <reportElement uuid=\\"3ba05985-9b10-4ba0-9ef5-ab3aa00910cc\\" x=\\"383\\" y=\\"0\\" width=\\"55\\" height=\\"20\\"/> <textElement textAlignment=\\"Center\\"> <font size=\\"9\\"/> </textElement> <textFieldExpression><![CDATA[$F{anc 4th or more_year}]]></textFieldExpression> </textField> </band> </detail> <columnFooter> <band height=\\"22\\" splitType=\\"Stretch\\"/> </columnFooter> <pageFooter> <band height=\\"20\\" splitType=\\"Stretch\\"/> </pageFooter> <summary> <band height=\\"28\\" splitType=\\"Stretch\\"/> </summary> </jasperReport>"
            }""";
    String uid = assertStatus(CREATED, POST("/reports/", body));
    assertFalse(
        GET("/reports/" + uid + "/data.pdf?t=1715330660314&ou=ImspTQPwCqd&pe=2023&date=2023-01-01")
            .content("application/pdf")
            .isEmpty());
  }
}
