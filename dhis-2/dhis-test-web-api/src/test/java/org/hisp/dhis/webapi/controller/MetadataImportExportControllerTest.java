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

import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpClientAdapter.ContentType;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.geojson.GeoJsonObject;
import org.geojson.Polygon;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.test.api.TestCategoryMetadata;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonAttributeValue;
import org.hisp.dhis.test.webapi.json.domain.JsonDataElement;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.test.webapi.json.domain.JsonImportSummary;
import org.hisp.dhis.test.webapi.json.domain.JsonProgram;
import org.hisp.dhis.test.webapi.json.domain.JsonTypeReport;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.metadata.MetadataImportExportController} using
 * (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class MetadataImportExportControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private DataElementService dataElementService;

  @Test
  void testPostJsonMetadata() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        null,
        POST(
                "/38/metadata",
                "{'organisationUnits':[{'name':'My Unit', 'shortName':'OU1', 'openingDate':"
                    + " '2020-01-01'}]}")
            .content(HttpStatus.OK));
  }

  @Test
  void testPostJsonMetadata_Empty() {
    assertWebMessage("OK", 200, "OK", null, POST("/38/metadata", "{}").content(HttpStatus.OK));
  }

  @Test
  void testPostCsvMetadata() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        null,
        POST("/38/metadata?classKey=ORGANISATION_UNIT", Body(","), ContentType("application/csv"))
            .content(HttpStatus.OK));
  }

  @Test
  void testPostGmlMetadata() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        null,
        POST("/38/metadata/gml", Body("<metadata></metadata>"), ContentType("application/xml"))
            .content(HttpStatus.OK));
  }

  @Test
  void testPostProgramStageWithoutProgram() {
    JsonWebMessage message =
        POST("/metadata/", "{'programStages':[{'name':'test programStage'}]}")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);
    JsonImportSummary response = message.get("response", JsonImportSummary.class);
    assertEquals(
        1, response.getTypeReports().get(0).getObjectReports().get(0).getErrorReports().size());
    assertEquals(
        ErrorCode.E4053,
        response
            .getTypeReports()
            .get(0)
            .getObjectReports()
            .get(0)
            .getErrorReports()
            .get(0)
            .getErrorCode());
  }

  @Test
  void testPostProgramStageWithProgram() {
    POST(
            "/metadata/",
            "{'programs':[{'name':'test program', 'id':'VoZMWi7rBgj', 'shortName':'test"
                + " program','programType':'WITH_REGISTRATION','programStages':[{'id':'VoZMWi7rBgf'}]"
                + " }],'programStages':[{'id':'VoZMWi7rBgf','name':'test programStage'}]}")
        .content(HttpStatus.OK);
    assertEquals(
        "VoZMWi7rBgj",
        GET("/programStages/{id}", "VoZMWi7rBgf").content().getString("program.id").string());
    assertEquals(
        "VoZMWi7rBgf",
        GET("/programs/{id}", "VoZMWi7rBgj").content().getString("programStages[0].id").string());
  }

  @Test
  void testGetWithIeqFilter() {
    POST(
            "/metadata/",
            "{'programs':[{'name':'Test Program', 'id':'VoZMWi7rBgj', 'shortName':'test"
                + " program','programType':'WITH_REGISTRATION', 'version':'5'}]}")
        .content(HttpStatus.OK);

    assertEquals(
        "Test Program",
        GET("/metadata?programs=true&filter=name:ieq:test program")
            .content()
            .getList("programs", JsonProgram.class)
            .get(0)
            .getName());
  }

  @Test
  void testGetWithIeqFilterNonString() {
    POST(
            "/metadata/",
            "{'programs':[{'name':'Test Program', 'id':'VoZMWi7rBgj', 'shortName':'test"
                + " program','programType':'WITH_REGISTRATION', 'version':'5'}]}")
        .content(HttpStatus.OK);

    JsonWebMessage response =
        GET("/metadata?programs=true&filter=version:ieq:5")
            .content(HttpStatus.Series.CLIENT_ERROR)
            .as(JsonWebMessage.class);

    assertEquals(
        "Value `5` of type `Integer` is not supported by this operator.", response.getMessage());
    assertEquals(409, response.getHttpStatusCode());
    assertEquals("Conflict", response.getHttpStatus());
  }

  @Test
  void testPostValidGeoJsonAttribute() throws IOException {
    POST(
            "/metadata",
            "{\"organisationUnits\": [ {\"id\":\"rXnqqH2Pu6N\",\"name\": \"My Unit"
                + " 2\",\"shortName\": \"OU2\",\"openingDate\": \"2020-01-01\",\"attributeValues\":"
                + " [{\"value\":  \"{\\\"type\\\": \\\"Polygon\\\",\\\"coordinates\\\": "
                + " [[[100,0],[101,0],[101,1],[100,1],[100,0]]] }\",\"attribute\": {\"id\":"
                + " \"RRH9IFiZZYN\"}}]}],"
                + "\"attributes\":[{\"id\":\"RRH9IFiZZYN\",\"valueType\":\"GEOJSON\",\"organisationUnitAttribute\":true,\"name\":\"testgeojson\"}]}")
        .content(HttpStatus.OK);

    JsonIdentifiableObject organisationUnit =
        GET("/organisationUnits/{id}", "rXnqqH2Pu6N").content().asA(JsonIdentifiableObject.class);

    assertEquals(1, organisationUnit.getAttributeValues().size());
    JsonAttributeValue attributeValue = organisationUnit.getAttributeValues().get(0);
    GeoJsonObject geoJSON =
        new ObjectMapper().readValue(attributeValue.getValue(), GeoJsonObject.class);
    assertTrue(geoJSON instanceof Polygon);
    Polygon polygon = (Polygon) geoJSON;
    assertEquals(100, polygon.getCoordinates().get(0).get(0).getLongitude());
  }

  @Test
  void testPostInValidGeoJsonAttribute() {
    JsonWebMessage message =
        POST(
                "/metadata",
                "{\"organisationUnits\": [ {\"id\":\"rXnqqH2Pu6N\",\"name\": \"My Unit"
                    + " 2\",\"shortName\": \"OU2\",\"openingDate\":"
                    + " \"2020-01-01\",\"attributeValues\": [{\"value\":  \"{\\\"type\\\":"
                    + " \\\"Polygon\\\"}\",\"attribute\": {\"id\": \"RRH9IFiZZYN\"}}]}],"
                    + "\"attributes\":[{\"id\":\"RRH9IFiZZYN\",\"valueType\":\"GEOJSON\",\"organisationUnitAttribute\":true,\"name\":\"testgeojson\"}]}")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);
    assertNotNull(
        message.find(JsonErrorReport.class, report -> report.getErrorCode() == ErrorCode.E6004));
  }

  /** Import OptionSet with two Options, sort orders are 2 and 3. */
  @Test
  void testImportOptionSetWithOptions() {
    POST(
            "/metadata",
            "{\"optionSets\":\n"
                + "    [{\"name\": \"Device category\",\"id\": \"RHqFlB1Wm4d\",\"version\":"
                + " 2,\"valueType\": \"TEXT\",\"options\":[{\"id\": \"Uh4HvjK6zg3\"},{\"id\":"
                + " \"BQMei56UBl6\"}]}],\n"
                + "\"options\":\n"
                + "    [{\"code\": \"Vaccine freezer\",\"name\": \"Vaccine freezer\",\"id\":"
                + " \"BQMei56UBl6\",\"sortOrder\": 5,\"optionSet\":{\"id\": \"RHqFlB1Wm4d\"}},\n"
                + "    {\"code\": \"Icelined refrigerator\",\"name\": \"Icelined"
                + " refrigerator\",\"id\": \"Uh4HvjK6zg3\",\"optionSet\":{\"id\":"
                + " \"RHqFlB1Wm4d\"}}]}")
        .content(HttpStatus.OK);

    JsonObject response =
        GET("/optionSets/{uid}?fields=options[id,sortOrder]", "RHqFlB1Wm4d").content();

    assertEquals(2, response.getObject("options").size());
    assertEquals(0, response.getNumber("options[0].sortOrder").intValue());
    assertEquals(1, response.getNumber("options[1].sortOrder").intValue());
    assertEquals("Uh4HvjK6zg3", response.getString("options[0].id").string());
    assertEquals("BQMei56UBl6", response.getString("options[1].id").string());
  }

  /** Import OptionSet with two Options, one has sortOrder and the other doesn't */
  @Test
  void testImportOptionSetWithOptionsOneSortOrder() {
    POST(
            "/metadata",
            "{\"optionSets\":\n"
                + "    [{\"name\": \"Device category\",\"id\": \"RHqFlB1Wm4d\",\"version\":"
                + " 2,\"valueType\": \"TEXT\",\"options\":[{\"id\": \"Uh4HvjK6zg3\"},{\"id\":"
                + " \"BQMei56UBl6\"}]}],\n"
                + "\"options\":\n"
                + "    [{\"code\": \"Vaccine freezer\",\"name\": \"Vaccine freezer\",\"id\":"
                + " \"BQMei56UBl6\",\"optionSet\":{\"id\": \"RHqFlB1Wm4d\"}},\n"
                + "    {\"code\": \"Icelined refrigerator\",\"name\": \"Icelined"
                + " refrigerator\",\"id\": \"Uh4HvjK6zg3\",\"sortOrder\": 3,\"optionSet\":{\"id\":"
                + " \"RHqFlB1Wm4d\"}}]}")
        .content(HttpStatus.OK);

    JsonObject response =
        GET("/optionSets/{uid}?fields=options[id,sortOrder]", "RHqFlB1Wm4d").content();

    assertEquals(2, response.getObject("options").size());
    assertNotNull(response.get("options[0].sortOrder"));
    assertNotNull(response.get("options[1].sortOrder"));
  }

  /** Import OptionSet with two Options, both doesn't have sortOrder */
  @Test
  void testImportOptionSetWithOptionsNoSortOrder() {
    POST(
            "/metadata",
            "{\"optionSets\":\n"
                + "    [{\"name\": \"Device category\",\"id\": \"RHqFlB1Wm4d\",\"version\":"
                + " 2,\"valueType\": \"TEXT\",\"options\":[{\"id\": \"BQMei56UBl6\"},{\"id\":"
                + " \"Uh4HvjK6zg3\"}]}],\n"
                + "\"options\":\n"
                + "    [{\"code\": \"Vaccine freezer\",\"name\": \"Vaccine freezer\",\"id\":"
                + " \"BQMei56UBl6\",\"optionSet\":{\"id\": \"RHqFlB1Wm4d\"}},\n"
                + "    {\"code\": \"Icelined refrigerator\",\"name\": \"Icelined"
                + " refrigerator\",\"id\": \"Uh4HvjK6zg3\",\"optionSet\":{\"id\":"
                + " \"RHqFlB1Wm4d\"}}]}")
        .content(HttpStatus.OK);

    JsonObject response =
        GET("/optionSets/{uid}?fields=options[id,sortOrder]", "RHqFlB1Wm4d").content();

    assertEquals(2, response.getObject("options").size());
    assertEquals(0, response.getNumber("options[0].sortOrder").intValue());
    assertEquals("BQMei56UBl6", response.getString("options[0].id").string());
    assertNotNull(response.get("options[1].sortOrder"));
    assertEquals("Uh4HvjK6zg3", response.getString("options[1].id").string());
    assertEquals(1, response.getNumber("options[1].sortOrder").intValue());

    POST(
            "/metadata",
            "{\"optionSets\":\n"
                + "    [{\"name\": \"Device category\",\"id\": \"RHqFlB1Wm4d\",\"version\":"
                + " 2,\"valueType\": \"TEXT\",\"options\":[{\"id\": \"Uh4HvjK6zg3\"},{\"id\":"
                + " \"BQMei56UBl6\"}]}],\n"
                + "\"options\":\n"
                + "    [{\"code\": \"Icelined refrigerator\",\"name\": \"Icelined"
                + " refrigerator\",\"id\": \"Uh4HvjK6zg3\",\"optionSet\":{\"id\":"
                + " \"RHqFlB1Wm4d\"}},\n"
                + "    {\"code\": \"Vaccine freezer\",\"name\": \"Vaccine freezer\",\"id\":"
                + " \"BQMei56UBl6\",\"optionSet\":{\"id\": \"RHqFlB1Wm4d\"}}]}")
        .content(HttpStatus.OK);

    response = GET("/optionSets/{uid}?fields=options[id,sortOrder]", "RHqFlB1Wm4d").content();
    assertEquals("Uh4HvjK6zg3", response.getString("options[0].id").string());
    assertEquals("BQMei56UBl6", response.getString("options[1].id").string());
  }

  /** Import OptionSet with two Options, both have same sortOrder */
  @Test
  void testImportOptionSetWithOptionsDuplicateSortOrder() {
    POST(
            "/metadata",
            "{\"optionSets\":\n"
                + "    [{\"name\": \"Device category\",\"id\": \"RHqFlB1Wm4d\",\"version\":"
                + " 2,\"valueType\": \"TEXT\",\"options\":[{\"id\": \"Uh4HvjK6zg3\"},{\"id\":"
                + " \"BQMei56UBl6\"}]}],\n"
                + "\"options\":\n"
                + "    [{\"code\": \"Vaccine freezer\",\"name\": \"Vaccine freezer\",\"sortOrder\":"
                + " 2,\"id\": \"BQMei56UBl6\",\"optionSet\":{\"id\": \"RHqFlB1Wm4d\"}},\n"
                + "    {\"code\": \"Icelined refrigerator\",\"name\": \"Icelined"
                + " refrigerator\",\"sortOrder\": 2,\"id\": \"Uh4HvjK6zg3\",\"optionSet\":{\"id\":"
                + " \"RHqFlB1Wm4d\"}}]}")
        .content(HttpStatus.OK);

    JsonObject response =
        GET("/optionSets/{uid}?fields=options[id,sortOrder]", "RHqFlB1Wm4d").content();

    assertEquals(2, response.getObject("options").size());
    assertNotNull(response.get("options[0].sortOrder"));
    assertNotNull(response.get("options[1].sortOrder"));
  }

  @Test
  void testImportOptionSetWithNoLinkOptions() {
    POST(
            "/metadata",
            "{\"optionSets\":\n"
                + "    [{\"name\": \"Device category\",\"id\": \"RHqFlB1Wm4d\",\"version\":"
                + " 2,\"valueType\": \"TEXT\"}],\n"
                + "\"options\":\n"
                + "    [{\"code\": \"Vaccine freezer\",\"name\": \"Vaccine freezer\",\"sortOrder\":"
                + " 2,\"id\": \"BQMei56UBl6\",\"optionSet\":{\"id\": \"RHqFlB1Wm4d\"}},\n"
                + "    {\"code\": \"Icelined refrigerator\",\"name\": \"Icelined"
                + " refrigerator\",\"sortOrder\": 3,\"id\": \"Uh4HvjK6zg3\",\"optionSet\":{\"id\":"
                + " \"RHqFlB1Wm4d\"}}]}")
        .content(HttpStatus.OK);

    JsonObject response =
        GET("/optionSets/{uid}?fields=options[id,sortOrder]", "RHqFlB1Wm4d").content();

    assertEquals(2, response.getObject("options").size());
    assertNotNull(response.get("options[0].sortOrder"));
    assertNotNull(response.get("options[1].sortOrder"));
  }

  @Test
  @DisplayName(
      "Should not include null objects in collection Category.categorycombos or"
          + " CategoryCombo.categories after importing")
  void testImportCategoryComboAndCategory() {
    POST("/metadata", Path.of("metadata/category_and_categorycombo.json")).content(HttpStatus.OK);
    JsonMixed response = GET("/categories/{uid}?fields=id,categoryCombos", "IjOK1aXkjVO").content();
    JsonList<JsonObject> catCombos = response.getList("categoryCombos", JsonObject.class);
    assertNotNull(catCombos);
    assertFalse(catCombos.stream().anyMatch(JsonValue::isNull));

    response = GET("/categoryCombos/{uid}?fields=id,categoryCombos", "TIAbMD7ETV6").content();
    JsonList<JsonObject> categories = response.getList("categories", JsonObject.class);
    assertNotNull(categories);
    assertFalse(categories.stream().anyMatch(JsonValue::isNull));
  }

  @Test
  void testImportDashboardWithInvalidLayout_UpdateFlow() {
    JsonImportSummary createReport =
        POST("/metadata", Path.of("dashboard/import_dashboard_with_valid_layout.json"))
            .content(HttpStatus.OK)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals("OK", createReport.getStatus());
    assertEquals(1, createReport.getStats().getCreated());
    assertEquals(0, createReport.getStats().getIgnored());
    assertEquals(0, createReport.getStats().getUpdated());
    assertEquals(1, createReport.getStats().getTotal());

    JsonImportSummary updateReport =
        POST("/metadata", Path.of("dashboard/import_dashboard_with_invalid_layout.json"))
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals("ERROR", updateReport.getStatus());
    assertEquals(0, updateReport.getStats().getCreated());
    assertEquals(1, updateReport.getStats().getIgnored());
    assertEquals(0, updateReport.getStats().getUpdated());
    assertEquals(1, updateReport.getStats().getTotal());
  }

  @Test
  void testImportDashboardWithValidLayout_CreateFlow() {
    JsonImportSummary report =
        POST("/metadata", Path.of("dashboard/import_dashboard_with_valid_layout.json"))
            .content(HttpStatus.OK)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals("OK", report.getStatus());
    assertEquals(1, report.getStats().getCreated());
    assertEquals(0, report.getStats().getIgnored());
    assertEquals(0, report.getStats().getUpdated());
    assertEquals(1, report.getStats().getTotal());
  }

  @Test
  void testImportDashboardWithInvalidLayout_CreateFlow() {
    JsonImportSummary report =
        POST("/metadata", Path.of("dashboard/import_dashboard_with_invalid_layout.json"))
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals("ERROR", report.getStatus());
    assertEquals(0, report.getStats().getCreated());
    assertEquals(1, report.getStats().getIgnored());
    assertEquals(0, report.getStats().getUpdated());
    assertEquals(1, report.getStats().getTotal());
  }

  @Test
  @DisplayName("Export user metadata with skipSharing option returns expected fields")
  void exportUserWithSkipSharing() {
    // when users are exported including the skipSharing option
    JsonObject user =
        GET("/metadata.json?skipSharing=true&download=true&users=true")
            .content(HttpStatus.OK)
            .getArray("users")
            .getObject(0);

    // then the returned users should have the following fields present
    assertTrue(user.exists());
    assertTrue(user.getString("username").exists());
    assertTrue(user.getString("userRoles").exists());
  }

  @Test
  void testImportWithInvalidCreatedBy() {
    JsonMixed report =
        POST(
                "/metadata",
                "{\"optionSets\":\n"
                    + "    [{\"name\": \"Device category\",\"id\": \"RHqFlB1Wm4d\",\"version\":"
                    + " 2,\"valueType\": \"TEXT\",\"createdBy\": \"invalid\"}]}")
            .content(HttpStatus.OK);

    assertNotNull(report.get("response"));

    JsonMixed optionSet = GET("/optionSets/{uid}", "RHqFlB1Wm4d").content(HttpStatus.OK);
    assertTrue(optionSet.get("createdBy").exists());
  }

  @Test
  void testImportWithInvalidCreatedByAndSkipSharing() {
    JsonMixed report =
        POST(
                "/metadata?skipSharing=true",
                "{\"optionSets\":\n"
                    + "    [{\"name\": \"Device category\",\"id\": \"RHqFlB1Wm4d\",\"version\":"
                    + " 2,\"valueType\": \"TEXT\",\"createdBy\": \"invalid\"}]}")
            .content(HttpStatus.OK);

    assertNotNull(report.get("response"));

    JsonMixed optionSet = GET("/optionSets/{uid}", "RHqFlB1Wm4d").content(HttpStatus.OK);
    assertTrue(optionSet.get("createdBy").exists());
  }

  /**
   * After upgrading Spring 6.1 this test failed. The reason is the DataElementDeletionHandler use a
   * JDBC template to execute the exists query and this query doesn't see the changes made in the
   * main test transaction. In result, deletion event is not vetoed.
   */
  @Disabled
  @DisplayName(
      "Should return error in import report if deleting object is referenced by other object")
  void testDeleteWithException() {
    POST(
            "/metadata",
"""
{'optionSets':
    [{'name': 'Device category','id': 'RHqFlB1Wm4d','version': 2,'valueType': 'TEXT'}]
,'dataElements':
[{'name':'test DataElement with OptionSet', 'shortName':'test DataElement', 'aggregationType':'SUM','domainType':'AGGREGATE','categoryCombo':{'id':'bjDvmb4bfuf'},'valueType':'NUMBER','optionSet':{'id':'RHqFlB1Wm4d'}
}]}""")
        .content(HttpStatus.OK);
    JsonMixed response =
        POST(
                "/metadata?importStrategy=DELETE",
                """
                {'optionSets':
                [{'name': 'Device category','id': 'RHqFlB1Wm4d','version': 2,'valueType': 'TEXT'}]}""")
            .content(HttpStatus.CONFLICT);
    JsonImportSummary report = response.get("response").as(JsonImportSummary.class);
    assertEquals(0, report.getStats().getDeleted());
    assertEquals(1, report.getStats().getIgnored());
    assertEquals(
        "Object could not be deleted because it is associated with another object: DataElement",
        report
            .find(
                JsonErrorReport.class, errorReport -> errorReport.getErrorCode() == ErrorCode.E4030)
            .getMessage());
  }

  @Test()
  @DisplayName("Should not return error E6305 when PATCH any property of an AggregateDataExchange")
  void testPatchAggregateDataExchange() {
    POST("/metadata/", Path.of("metadata/aggregate_data_exchange.json")).content(HttpStatus.OK);
    PATCH(
            "/aggregateDataExchanges/PnWccbwCJLQ",
            Body(
                "[{'op': 'replace', 'path': '/name', 'value': 'External basic auth data exchange"
                    + " updated'}]"))
        .content(HttpStatus.OK);

    JsonObject object =
        GET("/aggregateDataExchanges/PnWccbwCJLQ").content(HttpStatus.OK).as(JsonObject.class);
    assertEquals("External basic auth data exchange updated", object.getString("name").string());
  }

  @Test
  @DisplayName(
      "Should return error E6305 if create a new AggregateDataExchange without authentication"
          + " details")
  void testCreateAggregateDataExchangeWithoutAuthentication() {
    JsonImportSummary report =
        POST("/metadata/", Path.of("metadata/aggregate_data_exchange_no_auth.json"))
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals(
        "Aggregate data exchange target API must specify either access token or username and"
            + " password",
        report
            .find(
                JsonErrorReport.class, errorReport -> errorReport.getErrorCode() == ErrorCode.E6305)
            .getMessage());
  }

  @Test
  @DisplayName(
      "Should return error if user doesn't have Data Write permission for given"
          + " AggregateDataExchange")
  void testAggregateDataExchangeFail() {
    POST("/metadata/", Path.of("metadata/aggregate_data_exchange.json")).content(HttpStatus.OK);
    User userA = createAndAddUser("UserA");
    PATCH(
            "/aggregateDataExchanges/iFOyIpQciyk",
            """
            [{"op":"add", "path":"/sharing",
            "value":{"owner": "GOLswS44mh8",
              "public": "rw------",
              "external": false,
              "users": {"%s": {"id": "%s", "access": "rw------"}}}}]"""
                .formatted(userA.getUid(), userA.getUid()))
        .content(HttpStatus.OK);
    injectSecurityContext(UserDetails.fromUser(userA));
    JsonTypeReport typeReport =
        POST("/aggregateDataExchanges/iFOyIpQciyk/exchange")
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonTypeReport.class);
    JsonImportSummary report = typeReport.getImportSummaries().get(0).as(JsonImportSummary.class);
    assertEquals("ERROR", report.getStatus());
    assertEquals(
        "User has no data write access for AggregateDataExchange: Internal data exchange",
        report.getString("description").string());
  }

  @Test
  void testAggregateDataExchangeSuccess() {
    POST("/metadata/", Path.of("metadata/aggregate_data_exchange.json")).content(HttpStatus.OK);
    JsonTypeReport typeReport =
        POST("/aggregateDataExchanges/iFOyIpQciyk/exchange")
            .content(HttpStatus.OK)
            .get("response")
            .as(JsonTypeReport.class);
    JsonImportSummary report = typeReport.getImportSummaries().get(0).as(JsonImportSummary.class);
    assertEquals("SUCCESS", report.getStatus());
  }

  @Test
  void deleteStatsAreCorrectWhenDeleteNotAllowedTest() {
    // given import of 2 categories and 2 category combos
    POST(
            "/metadata?importReportMode=FULL&importStrategy=CREATE_AND_UPDATE&async=false",
            Path.of("metadata/categories_with_category_combos.json"))
        .content(HttpStatus.OK);

    // when trying to delete 2 categories
    JsonImportSummary importSummary =
        POST(
                "/metadata?importReportMode=FULL&importStrategy=DELETE&async=false",
                Path.of("metadata/delete_categories.json"))
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    // then report shows items as ignored as delete is not allowed
    assertEquals("WARNING", importSummary.getStatus());
    JsonTypeReport typeReport = importSummary.getTypeReports().get(0).as(JsonTypeReport.class);

    assertTrue(
        typeReport.getObjectReports().stream()
            .flatMap(or -> or.getErrorReports().stream())
            .allMatch(
                er ->
                    er.getMessage()
                        .contains(
                            "Object could not be deleted because it is associated with another object")));
    assertEquals(2, importSummary.getStats().getTotal());
    assertEquals(2, importSummary.getStats().getIgnored());
    assertEquals(0, importSummary.getStats().getDeleted());
    assertEquals(0, importSummary.getStats().getCreated());
    assertEquals(0, importSummary.getStats().getUpdated());

    assertEquals(2, typeReport.getStats().getTotal());
    assertEquals(2, typeReport.getStats().getIgnored());
    assertEquals(0, typeReport.getStats().getDeleted());
    assertEquals(0, typeReport.getStats().getCreated());
    assertEquals(0, typeReport.getStats().getUpdated());
  }

  @Test
  @DisplayName("Should not insert duplicate translation records when updating object")
  void testUpdateObjectWithTranslation() {
    POST(
            "/metadata",
            "{\"optionSets\":\n"
                + "    [{\"name\": \"Device category\",\"id\": \"RHqFlB1Wm4d\",\"version\":"
                + " 2,\"valueType\": \"TEXT\", \"translations\":[{\n"
                + "      \"locale\": \"en_GB\",\n"
                + "      \"property\": \"NAME\",\n"
                + "      \"value\": \"Device category 1\"\n"
                + "    }]}]}")
        .content(HttpStatus.OK);

    POST(
            "/metadata",
            "{\"optionSets\":\n"
                + "    [{\"name\": \"Device category\",\"id\": \"RHqFlB1Wm4d\",\"version\":"
                + " 2,\"valueType\": \"TEXT\", \"translations\":[{\n"
                + "      \"locale\": \"en_GB\",\n"
                + "      \"property\": \"NAME\",\n"
                + "      \"value\": \"Device category 2\"\n"
                + "    }]}]}")
        .content(HttpStatus.OK);

    JsonObject response = GET("/optionSets/{uid}", "RHqFlB1Wm4d").content();

    assertEquals(1, response.getArray("translations").size());
    assertEquals(
        "Device category 2",
        response.getArray("translations").getObject(0).getString("value").string());
  }

  @Test
  @DisplayName(
      "DataElements with default categoryCombo should be present in payload when defaults are"
          + " INCLUDE by default")
  void metadataWithCatComboFieldsIncludingDefaultsTest() {
    CategoryCombo catComboA = createCategoryCombo('A');
    CategoryCombo catComboB = createCategoryCombo('B');
    CategoryCombo catComboC = createCategoryCombo('C');
    categoryService.addCategoryCombo(catComboA);
    categoryService.addCategoryCombo(catComboB);
    categoryService.addCategoryCombo(catComboC);

    setupDataElementsWithCatCombos(catComboA, catComboB, catComboC);

    JsonArray dataElements =
        GET("/metadata?fields=id,name,categoryCombo[id,name]&dataElements=true")
            .content(HttpStatus.OK)
            .getArray("dataElements");

    assertEquals(
        Set.of(catComboA.getUid(), catComboB.getUid(), catComboC.getUid(), "bjDvmb4bfuf"),
        dataElements.stream()
            .map(jde -> jde.as(JsonDataElement.class))
            .map(JsonDataElement::getCategoryCombo)
            .map(JsonIdentifiableObject::getId)
            .collect(Collectors.toSet()),
        "Returned cat combo IDs equal custom cat combos and default cat combo Ids");
  }

  @Test
  @DisplayName(
      "DataElements in payload should not include the default categoryCombo when EXCLUDE used")
  void metadataExcludingDefaultCatComboTest() {
    CategoryCombo catComboA = createCategoryCombo('A');
    CategoryCombo catComboB = createCategoryCombo('B');
    CategoryCombo catComboC = createCategoryCombo('C');
    categoryService.addCategoryCombo(catComboA);
    categoryService.addCategoryCombo(catComboB);
    categoryService.addCategoryCombo(catComboC);

    setupDataElementsWithCatCombos(catComboA, catComboB, catComboC);

    JsonArray dataElements =
        GET("/metadata?fields=id,name,categoryCombo[id,name]&defaults=EXCLUDE&dataElements=true")
            .content(HttpStatus.OK)
            .getArray("dataElements");

    // get map of data elements with/without cat combo
    Map<Boolean, List<JsonValue>> deWithCatCombo =
        dataElements.stream()
            .collect(
                Collectors.partitioningBy(
                    jv -> {
                      JsonDataElement jsonDataElement = jv.as(JsonDataElement.class);
                      return jsonDataElement.getCategoryCombo() != null;
                    }));

    assertEquals(
        3,
        deWithCatCombo.get(true).size(),
        "There should be 3 dataElements with a cat combo field");

    assertEquals(
        1,
        deWithCatCombo.get(false).size(),
        "There should be 1 dataElement without a cat combo field");

    assertEquals(
        Set.of(catComboA.getUid(), catComboB.getUid(), catComboC.getUid()),
        deWithCatCombo.get(true).stream()
            .map(jde -> jde.as(JsonDataElement.class))
            .map(JsonDataElement::getCategoryCombo)
            .map(JsonIdentifiableObject::getId)
            .collect(Collectors.toSet()),
        "Returned cat combo IDs equal custom cat combos Ids only");
  }

  @Test
  @DisplayName("Importing Map with MapView with pre-existing OrgUnitGroupSetDimensions succeeds")
  void importingMapWithMapViewAndOrgUnitGroupSetDimensionsExistingTest() {
    // Given org unit metadata exists
    JsonImportSummary report1 =
        POST("/metadata", Path.of("metadata/metadata_org_unit_group.json"))
            .content()
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals("OK", report1.getStatus());

    // When importing a Map with MapView that references an existing OrgUnitGroupSetDimension
    JsonImportSummary report2 =
        POST("/metadata", Path.of("metadata/metadata_map_mapview.json"))
            .content()
            .get("response")
            .as(JsonImportSummary.class);

    // Then the import is successful and the OrgUnitGroupSetDimension is present when retrieved
    assertEquals("OK", report2.getStatus());

    JsonMixed content = GET("/maps/d7x2WOLhCA8").content(HttpStatus.OK);

    assertEquals(
        "J5jldMd8OHv",
        content
            .getArray("mapViews")
            .getObject(0)
            .getArray("organisationUnitGroupSetDimensions")
            .getObject(0)
            .getObject("organisationUnitGroupSet")
            .getString("id")
            .string());

    assertEquals(
        "uYxK4wmcPqA",
        content
            .getArray("mapViews")
            .getObject(0)
            .getArray("organisationUnitGroupSetDimensions")
            .getObject(0)
            .getArray("organisationUnitGroups")
            .getObject(0)
            .getString("id")
            .string());
  }

  @Test
  @DisplayName("Importing Map with MapView with OrgUnitGroupSetDimensions in same import succeeds")
  void importingMapWithMapViewAndOrgUnitGroupSetDimensionsPayloadTest() {
    // When importing a Map with MapView that references OrgUnitGroupSet data in the same import
    JsonImportSummary report =
        POST(
                "/metadata",
                Path.of("metadata/metadata_map_mapview_with_org_unit_group_dimension.json"))
            .content()
            .get("response")
            .as(JsonImportSummary.class);

    // Then the import is successful and the OrgUnitGroupSetDimension is present when retrieved
    assertEquals("OK", report.getStatus());

    JsonMixed content = GET("/maps/A7x2WOLhCA8").content(HttpStatus.OK);

    assertEquals(
        "A5jldMd8OHv",
        content
            .getArray("mapViews")
            .getObject(0)
            .getArray("organisationUnitGroupSetDimensions")
            .getObject(0)
            .getObject("organisationUnitGroupSet")
            .getString("id")
            .string());

    assertEquals(
        "AYxK4wmcPqA",
        content
            .getArray("mapViews")
            .getObject(0)
            .getArray("organisationUnitGroupSetDimensions")
            .getObject(0)
            .getArray("organisationUnitGroups")
            .getObject(0)
            .getString("id")
            .string());
  }

  @Test
  @DisplayName("Importing (update) expected CategoryOptionCombos should succeed")
  void importExpectedCocsTest() {
    // Given category metadata exists
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("a");

    // When importing COCs that match the generated COC state
    JsonImportSummary report =
        POST("/metadata", Body(cocsMatchExpectedState(categoryMetadata)))
            .contentUnchecked()
            .get("response")
            .as(JsonImportSummary.class);

    // Then the import is successful and the COCs show as updated
    assertEquals("OK", report.getStatus());
    assertEquals(4, report.getStats().getUpdated());
  }

  @Test
  @DisplayName(
      "Importing (update) expected number, but different CategoryOptionCombos, should fail")
  void importExpectedCocsDifferentTest() {
    // Given category metadata exists
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("a");

    // When importing COCs that match expected number, but differ from the generated COC state
    JsonImportSummary report =
        POST("/metadata", Body(cocsDifferExpectedNumState(categoryMetadata)))
            .contentUnchecked()
            .get("response")
            .as(JsonImportSummary.class);

    // Then the import fails
    assertEquals("ERROR", report.getStatus());
    assertEquals(0, report.getStats().getUpdated());
    assertEquals(4, report.getStats().getIgnored());

    JsonTypeReport typeReport = report.getTypeReport(CategoryOptionCombo.class);
    JsonErrorReport errorReport = typeReport.getFirstErrorReport();
    assertNotNull(errorReport, "Expecting an error report in the import report");
    String errorMessage = errorReport.getMessage();
    assertNotNull(errorMessage, "Expecting an error message in the import report");
    String unexpectedPart = errorMessage.substring(0, errorMessage.indexOf('.'));
    String expectedPart = errorMessage.substring(errorMessage.indexOf('.'));

    assertTrue(
        unexpectedPart.contains("Unexpected CategoryOptionCombo provided with CategoryOptions"));
    assertTrue(unexpectedPart.contains(categoryMetadata.co1().getUid()));
    assertTrue(unexpectedPart.contains(categoryMetadata.co2().getUid()));
    assertTrue(unexpectedPart.contains(categoryMetadata.cc1().getUid()));

    assertTrue(
        expectedPart.contains("Missing expected CategoryOptionCombos with CategoryOption sets"));
    assertTrue(expectedPart.contains(categoryMetadata.co1().getUid()));
    assertTrue(expectedPart.contains(categoryMetadata.co4().getUid()));
  }

  @Test
  @DisplayName("Importing (update) fewer CategoryOptionCombos than expected should fail")
  void importFewerCocsTest() {
    // Given category metadata exists
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("b");

    // When importing COCs that do not match the generated COC state (3 supplied, 4 expected)
    JsonImportSummary report =
        POST("/metadata", Body(fewerCocs(categoryMetadata)))
            .contentUnchecked()
            .get("response")
            .as(JsonImportSummary.class);

    // Then the import fails and the COCs show as ignored
    assertEquals("ERROR", report.getStatus());
    assertEquals(3, report.getStats().getIgnored());
    JsonTypeReport typeReport = report.getTypeReport(CategoryOptionCombo.class);
    JsonErrorReport errorReport = typeReport.getFirstErrorReport();
    assertEquals(
        "Importing CategoryOptionCombos size 3 does not match expected size 4",
        errorReport.getMessage());
  }

  @Test
  @DisplayName("Importing (update) more CategoryOptionCombos than expected should fail")
  void importMoreCocsTest() {
    // Given category metadata exists
    TestCategoryMetadata categoryMetadata = setupCategoryMetadata("c");

    // When importing COCs that do not match the generated COC state (more supplied)
    JsonImportSummary report =
        POST("/metadata", Body(moreCocs(categoryMetadata)))
            .contentUnchecked()
            .get("response")
            .as(JsonImportSummary.class);

    // Then the import fails and the COCs show as ignored
    assertEquals("ERROR", report.getStatus());
    assertEquals(5, report.getStats().getIgnored());
    JsonTypeReport typeReport = report.getTypeReport(CategoryOptionCombo.class);
    JsonErrorReport errorReport = typeReport.getFirstErrorReport();
    assertEquals(
        "Importing CategoryOptionCombos size 5 does not match expected size 4",
        errorReport.getMessage());
  }

  @Test
  @DisplayName("Importing (create) expected CategoryOptionCombos should succeed")
  void importCreateExpectedCocsTest() {
    // When importing COCs that match the generated COC state
    JsonImportSummary report =
        POST("/metadata", Path.of("metadata/category/cat_model_expected_cocs.json"))
            .contentUnchecked()
            .get("response")
            .as(JsonImportSummary.class);

    // Then the import is successful and the COCs show as created (4 cocs + 7 other metadata)
    assertEquals("OK", report.getStatus());
    assertEquals(11, report.getStats().getCreated());
  }

  @Test
  @DisplayName(
      "Importing (create) expected number, but with duplicate CategoryOptionCombo, should fail")
  void importExpectedCocsDifferentCreateTest() {
    // When importing COCs that match expected number, but differ from the generated COC state
    JsonImportSummary report =
        POST(
                "/metadata",
                Path.of("metadata/category/cat_model_expected_num_but_different_cocs.json"))
            .contentUnchecked()
            .get("response")
            .as(JsonImportSummary.class);

    // Then the import fails
    assertEquals("ERROR", report.getStatus());
    assertEquals(0, report.getStats().getUpdated());
    assertEquals(11, report.getStats().getIgnored());

    JsonTypeReport typeReport = report.getTypeReport(CategoryOptionCombo.class);
    JsonErrorReport errorReport = typeReport.getFirstErrorReport();
    assertNotNull(errorReport, "Expecting an error report in the import report");
    String errorMessage = errorReport.getMessage();
    assertNotNull(errorMessage, "Expecting an error message in the import report");
    String unexpectedPart = errorMessage.substring(0, errorMessage.indexOf('.'));
    String expectedPart = errorMessage.substring(errorMessage.indexOf('.'));

    assertTrue(
        unexpectedPart.contains("Unexpected CategoryOptionCombo provided with CategoryOptions"));
    assertTrue(unexpectedPart.contains("CatOptUida1"));
    assertTrue(unexpectedPart.contains("CatOptUida3"));
    assertTrue(unexpectedPart.contains("CatComUida1"));

    assertTrue(
        expectedPart.contains("Missing expected CategoryOptionCombos with CategoryOption sets"));
    assertTrue(expectedPart.contains("CatOptUida2"));
    assertTrue(expectedPart.contains("CatOptUida3"));
  }

  @Test
  @DisplayName("Importing (create) fewer CategoryOptionCombos than expected should fail")
  void importCreateFewerCocsTest() {
    // When importing COCs that do not match the generated COC state (3 supplied, 4 expected)
    JsonImportSummary report =
        POST("/metadata", Path.of("metadata/category/cat_model_fewer_cocs.json"))
            .contentUnchecked()
            .get("response")
            .as(JsonImportSummary.class);

    // Then the import fails and the COCs show as ignored
    assertEquals("ERROR", report.getStatus());
    assertEquals(10, report.getStats().getIgnored());
    assertEquals(0, report.getStats().getCreated());
    JsonTypeReport typeReport = report.getTypeReport(CategoryOptionCombo.class);
    JsonErrorReport errorReport = typeReport.getFirstErrorReport();
    assertEquals(
        "Importing CategoryOptionCombos size 3 does not match expected size 4",
        errorReport.getMessage());
  }

  @Test
  @DisplayName("Importing (create) more CategoryOptionCombos than expected should fail")
  void importCreateMoreCocsTest() {
    // When importing COCs that do not match the generated COC state (more supplied)
    JsonImportSummary report =
        POST("/metadata", Path.of("metadata/category/cat_model_more_cocs.json"))
            .contentUnchecked()
            .get("response")
            .as(JsonImportSummary.class);

    // Then the import fails and the COCs show as ignored
    assertEquals("ERROR", report.getStatus());
    assertEquals(12, report.getStats().getIgnored());
    assertEquals(0, report.getStats().getCreated());
    JsonTypeReport typeReport = report.getTypeReport(CategoryOptionCombo.class);
    JsonErrorReport errorReport = typeReport.getFirstErrorReport();
    assertEquals(
        "Importing CategoryOptionCombos size 5 does not match expected size 4",
        errorReport.getMessage());
  }

  private String cocsMatchExpectedState(TestCategoryMetadata metadata) {
    return """
      {
        "categoryOptionCombos": [
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          },
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          },
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          },
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          }
        ]
      }
      """
        .formatted(
            metadata.coc1().getUid(),
            metadata.cc1().getUid(),
            metadata.co1().getUid(),
            metadata.co3().getUid(),
            metadata.coc2().getUid(),
            metadata.cc1().getUid(),
            metadata.co1().getUid(),
            metadata.co4().getUid(),
            metadata.coc3().getUid(),
            metadata.cc1().getUid(),
            metadata.co2().getUid(),
            metadata.co3().getUid(),
            metadata.coc4().getUid(),
            metadata.cc1().getUid(),
            metadata.co2().getUid(),
            metadata.co4().getUid());
  }

  private String cocsDifferExpectedNumState(TestCategoryMetadata metadata) {
    return """
      {
        "categoryOptionCombos": [
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          },
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          },
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          },
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          }
        ]
      }
      """
        .formatted(
            metadata.coc1().getUid(),
            metadata.cc1().getUid(),
            metadata.co1().getUid(),
            metadata.co3().getUid(),
            metadata.coc2().getUid(),
            metadata.cc1().getUid(),
            metadata.co1().getUid(),
            metadata.co2().getUid(),
            metadata.coc3().getUid(),
            metadata.cc1().getUid(),
            metadata.co2().getUid(),
            metadata.co3().getUid(),
            metadata.coc4().getUid(),
            metadata.cc1().getUid(),
            metadata.co2().getUid(),
            metadata.co4().getUid());
  }

  private String moreCocs(TestCategoryMetadata metadata) {
    return """
      {
        "categoryOptionCombos": [
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          },
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          },
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          },
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          },
          {
            "id": "NewCocUid01",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          }
        ]
      }
      """
        .formatted(
            metadata.coc1().getUid(),
            metadata.cc1().getUid(),
            metadata.co1().getUid(),
            metadata.co3().getUid(),
            metadata.coc2().getUid(),
            metadata.cc1().getUid(),
            metadata.co1().getUid(),
            metadata.co4().getUid(),
            metadata.coc3().getUid(),
            metadata.cc1().getUid(),
            metadata.co2().getUid(),
            metadata.co3().getUid(),
            metadata.coc4().getUid(),
            metadata.cc1().getUid(),
            metadata.co2().getUid(),
            metadata.co4().getUid(),
            metadata.cc1().getUid(),
            metadata.co2().getUid(),
            metadata.co4().getUid());
  }

  private String fewerCocs(TestCategoryMetadata metadata) {
    return """
      {
        "categoryOptionCombos": [
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          },
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          },
          {
            "id": "%s",
            "categoryCombo": {
              "id": "%s"
            },
            "categoryOptions": [
              {
                "id": "%s"
              },
              {
                "id": "%s"
              }
            ]
          }
        ]
      }
      """
        .formatted(
            metadata.coc1().getUid(),
            metadata.cc1().getUid(),
            metadata.co1().getUid(),
            metadata.co3().getUid(),
            metadata.coc2().getUid(),
            metadata.cc1().getUid(),
            metadata.co1().getUid(),
            metadata.co4().getUid(),
            metadata.coc3().getUid(),
            metadata.cc1().getUid(),
            metadata.co2().getUid(),
            metadata.co3().getUid());
  }

  private void setupDataElementsWithCatCombos(CategoryCombo... categoryCombos) {
    DataElement deA = createDataElement('A', categoryCombos[0]);
    DataElement deB = createDataElement('B', categoryCombos[1]);
    DataElement deC = createDataElement('C', categoryCombos[2]);
    DataElement deZ = createDataElement('Z');
    dataElementService.addDataElement(deA);
    dataElementService.addDataElement(deB);
    dataElementService.addDataElement(deC);
    dataElementService.addDataElement(deZ);
  }
}
