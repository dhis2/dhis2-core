/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.event.query;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderExistence;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowValueByName;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/query" endpoint. */
public class EventsQuery6AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  @DisplayName("Validate period dimension with stage-specific date dimension is rejected")
  public void validatePeriodAndStageWithOuNotRejected() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=pe:THIS_YEAR,Zj7UnCAulEk.ou:ImspTQPwCqd")
            .add("desc=eventdate,lastupdated")
            .add("relativePeriodDate=2022-12-31");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        100,
        19,
        15); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":54022,\"pageCount\":541,\"pageSize\":100,\"page\":1},\"items\":{\"Zj7UnCAulEk.ou\":{\"name\":\"Organisation unit\"},\"ImspTQPwCqd\":{\"code\":\"OU_525\",\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"Zj7UnCAulEk.ou\":[\"ImspTQPwCqd\"],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.ou",
        "Organisation unit",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: evenly spaced rows, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "lfjiNgsMNCR");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

    // Validate selected values for row index 9
    validateRowValueByName(response, actualHeaders, 9, "psi", "nLpj83AM1Mh");
    validateRowValueByName(response, actualHeaders, 9, "programstatus", "");

    // Validate selected values for row index 18
    validateRowValueByName(response, actualHeaders, 18, "psi", "fGRi36ns4kX");
    validateRowValueByName(response, actualHeaders, 18, "programstatus", "");

    // Validate selected values for row index 27
    validateRowValueByName(response, actualHeaders, 27, "psi", "tA6FviZCjM3");
    validateRowValueByName(response, actualHeaders, 27, "programstatus", "");

    // Validate selected values for row index 36
    validateRowValueByName(response, actualHeaders, 36, "psi", "sB86NztPpJT");
    validateRowValueByName(response, actualHeaders, 36, "programstatus", "");

    // Validate selected values for row index 45
    validateRowValueByName(response, actualHeaders, 45, "psi", "OPxpkETTNRL");
    validateRowValueByName(response, actualHeaders, 45, "programstatus", "");

    // Validate selected values for row index 54
    validateRowValueByName(response, actualHeaders, 54, "psi", "FJT2xnBFs8D");
    validateRowValueByName(response, actualHeaders, 54, "programstatus", "");

    // Validate selected values for row index 63
    validateRowValueByName(response, actualHeaders, 63, "psi", "HdRDfv7w67a");
    validateRowValueByName(response, actualHeaders, 63, "programstatus", "");

    // Validate selected values for row index 72
    validateRowValueByName(response, actualHeaders, 72, "psi", "eVSbPM94Nm0");
    validateRowValueByName(response, actualHeaders, 72, "programstatus", "");

    // Validate selected values for row index 81
    validateRowValueByName(response, actualHeaders, 81, "psi", "K5r81bJsc0i");
    validateRowValueByName(response, actualHeaders, 81, "programstatus", "");

    // Validate selected values for row index 90
    validateRowValueByName(response, actualHeaders, 90, "psi", "u7Phq0A0y20");
    validateRowValueByName(response, actualHeaders, 90, "programstatus", "");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "psi", "RPNgwvx5j65");
    validateRowValueByName(response, actualHeaders, 99, "programstatus", "");
  }

  @Test
  @DisplayName("Validate period dimension with stage-specific date dimension is rejected")
  public void validateStageAndStageSpecificDimensionRejected() {

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("stage=Zj7UnCAulEk")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:THIS_YEAR")
            .add("desc=eventdate,lastupdated")
            .add("relativePeriodDate=2022-12-31");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("httpStatusCode", equalTo(409))
        .body("status", equalTo("ERROR"))
        .body(
            "message",
            equalTo("Stage parameter cannot be used with stage-specific dimension identifiers"))
        .body("errorCode", equalTo("E7241"));
  }

  @Test
  @DisplayName("Validate period dimension with stage-specific date dimension is rejected")
  public void validateStageAndStageSpecificDimensionRejected2() {

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("stage=Zj7UnCAulEk")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:THIS_YEAR")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:LAST_YEAR")
            .add("desc=eventdate,lastupdated")
            .add("relativePeriodDate=2022-12-31");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("httpStatusCode", equalTo(409))
        .body("status", equalTo("ERROR"))
        .body("message", containsString("Duplicate stage dimension identifier"))
        .body("errorCode", equalTo("E7243"));
  }

  @Test
  public void stageAndEventDateThisYear() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:THIS_YEAR")
            .add("desc=eventdate,lastupdated")
            .add("relativePeriodDate=2022-12-31");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        100,
        19,
        15); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":54022,\"pageCount\":541,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{},\"2022\":{\"name\":\"2022\"},\"Zj7UnCAulEk.eventdate\":{\"name\":\"Report date\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.eventdate\":[\"2022\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "lfjiNgsMNCR");
    validateRowValueByName(
        response, actualHeaders, 0, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mokorbu MCHP");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "psi", "RPNgwvx5j65");
    validateRowValueByName(
        response, actualHeaders, 99, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 99, "ouname", "Gbenikoro MCHP");
    validateRowValueByName(response, actualHeaders, 99, "programstatus", "");

    // where:
    // (ax."occurreddate" >= '2022-01-01' and ax."occurreddate" <= '2022-12-31' and ax."ps" =
    // 'Zj7UnCAulEk')

    // metadata
    //      "items": {
    //          "Zj7UnCAulEk.dimension=Zj7UnCAulEk.EVENT_DATE:THIS_YEAREVENT_DATE": {
    //              "name": "Report date, name of the stage"
    //          },

  }

  @Test
  public void stageAndEventDateThisYearAndLastYear() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:THIS_YEAR;LAST_YEAR")
            .add("desc=eventdate,lastupdated")
            .add("relativePeriodDate=2022-12-31");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        100,
        19,
        15); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":107790,\"pageSize\":100,\"pageCount\":1078},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"Zj7UnCAulEk.eventdate\":{\"name\":\"Report date\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"Zj7UnCAulEk.eventdate\":[\"2022\",\"2021\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);

      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert PostGIS-specific headers existence based on 'expectPostgis' flag
    if (expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", true);
      validateHeaderExistence(actualHeaders, "longitude", true);
      validateHeaderExistence(actualHeaders, "latitude", true);
    } else {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "lfjiNgsMNCR");
    validateRowValueByName(
        response, actualHeaders, 0, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mokorbu MCHP");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

    // Validate selected values for row index 9
    validateRowValueByName(response, actualHeaders, 9, "psi", "nLpj83AM1Mh");
    validateRowValueByName(
        response, actualHeaders, 9, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 9, "ouname", "Yemoh Town CHC");
    validateRowValueByName(response, actualHeaders, 9, "programstatus", "");

    // Validate selected values for row index 18
    validateRowValueByName(response, actualHeaders, 18, "psi", "fGRi36ns4kX");
    validateRowValueByName(
        response, actualHeaders, 18, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 18, "ouname", "Sukudu MCHP");
    validateRowValueByName(response, actualHeaders, 18, "programstatus", "");

    // Validate selected values for row index 27
    validateRowValueByName(response, actualHeaders, 27, "psi", "tA6FviZCjM3");
    validateRowValueByName(
        response, actualHeaders, 27, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 27, "ouname", "Bumpeh (Nimikoro) CHC");
    validateRowValueByName(response, actualHeaders, 27, "programstatus", "");

    // Validate selected values for row index 36
    validateRowValueByName(response, actualHeaders, 36, "psi", "sB86NztPpJT");
    validateRowValueByName(
        response, actualHeaders, 36, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 36, "ouname", "Yemoh MCHP");
    validateRowValueByName(response, actualHeaders, 36, "programstatus", "");

    // Validate selected values for row index 45
    validateRowValueByName(response, actualHeaders, 45, "psi", "OPxpkETTNRL");
    validateRowValueByName(
        response, actualHeaders, 45, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 45, "ouname", "Gbaa (Makpele) CHP");
    validateRowValueByName(response, actualHeaders, 45, "programstatus", "");

    // Validate selected values for row index 54
    validateRowValueByName(response, actualHeaders, 54, "psi", "FJT2xnBFs8D");
    validateRowValueByName(
        response, actualHeaders, 54, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 54, "ouname", "Mosanda CHP");
    validateRowValueByName(response, actualHeaders, 54, "programstatus", "");

    // Validate selected values for row index 63
    validateRowValueByName(response, actualHeaders, 63, "psi", "HdRDfv7w67a");
    validateRowValueByName(
        response, actualHeaders, 63, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 63, "ouname", "Nyandehun (Mano Sakrim) MCHP");
    validateRowValueByName(response, actualHeaders, 63, "programstatus", "");

    // Validate selected values for row index 72
    validateRowValueByName(response, actualHeaders, 72, "psi", "eVSbPM94Nm0");
    validateRowValueByName(
        response, actualHeaders, 72, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 72, "ouname", "Binkolo CHC");
    validateRowValueByName(response, actualHeaders, 72, "programstatus", "");

    // Validate selected values for row index 81
    validateRowValueByName(response, actualHeaders, 81, "psi", "K5r81bJsc0i");
    validateRowValueByName(
        response, actualHeaders, 81, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 81, "ouname", "Serabu Hospital Mission");
    validateRowValueByName(response, actualHeaders, 81, "programstatus", "");

    // Validate selected values for row index 90
    validateRowValueByName(response, actualHeaders, 90, "psi", "u7Phq0A0y20");
    validateRowValueByName(
        response, actualHeaders, 90, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 90, "ouname", "Stocco CHP");
    validateRowValueByName(response, actualHeaders, 90, "programstatus", "");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "psi", "RPNgwvx5j65");
    validateRowValueByName(
        response, actualHeaders, 99, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 99, "ouname", "Gbenikoro MCHP");
    validateRowValueByName(response, actualHeaders, 99, "programstatus", "");
  }

  @Test
  public void stageAndEventDateSpecificYear() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:2021")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        100,
        19,
        15); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":53768,\"pageCount\":538,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{},\"2021\":{\"name\":\"2021\"},\"Zj7UnCAulEk.eventdate\":{\"name\":\"Report date\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.eventdate\":[\"2021\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "gInPXXRIWy5");
    validateRowValueByName(
        response, actualHeaders, 0, "Zj7UnCAulEk.eventdate", "2021-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Foindu (Lower Bamabara) CHC");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "psi", "Z3rMvGhgfw3");
    validateRowValueByName(
        response, actualHeaders, 99, "Zj7UnCAulEk.eventdate", "2021-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 99, "ouname", "Praise Foundation CHC");
    validateRowValueByName(response, actualHeaders, 99, "programstatus", "");

    // where:
    // and (ax."occurreddate" >= '2021-01-01' and ax."occurreddate" <= '2021-12-31' and ax."ps" =
    // 'Zj7UnCAulEk')

  }

  @Test
  public void stageAndEventDateRange() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:2021-03-01_2021-05-31")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        100,
        19,
        15); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":13744,\"pageCount\":138,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{},\"Zj7UnCAulEk.eventdate\":{\"name\":\"Report date\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.eventdate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);

      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);

      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "WJeBlIRE5Lb");
    validateRowValueByName(
        response, actualHeaders, 0, "Zj7UnCAulEk.eventdate", "2021-05-31 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Fogbo CHP");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "psi", "e2wE3axQQ5Z");
    validateRowValueByName(
        response, actualHeaders, 99, "Zj7UnCAulEk.eventdate", "2021-05-31 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 99, "ouname", "Makeni-Rokfullah MCHP");
    validateRowValueByName(response, actualHeaders, 99, "programstatus", "");

    // where:
    // (ax."occurreddate" >= '2021-03-01' and ax."occurreddate" <= '2021-05-31' and ax."ps" =
    // 'Zj7UnCAulEk')
  }

  @Test
  public void stageAndEventGreaterThan() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:GT:2021-05-01")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        100,
        19,
        15); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":89750,\"pageCount\":898,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{},\"Zj7UnCAulEk.eventdate\":{\"name\":\"Report date\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.eventdate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "lfjiNgsMNCR");
    validateRowValueByName(
        response, actualHeaders, 0, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mokorbu MCHP");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "psi", "RPNgwvx5j65");
    validateRowValueByName(
        response, actualHeaders, 99, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 99, "ouname", "Gbenikoro MCHP");
    validateRowValueByName(response, actualHeaders, 99, "programstatus", "");

    // where:
    // (ax."occurreddate" > '2023-05-01' and ax."ps" = 'Zj7UnCAulEk')
  }

  @Test
  public void stageAndEventLowerThan() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=Zj7UnCAulEk.EVENT_DATE:LE:2023-05-01")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        100,
        19,
        15); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":107790,\"pageCount\":1078,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{},\"Zj7UnCAulEk.eventdate\":{\"name\":\"Report date\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.eventdate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);

      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
    }
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "lfjiNgsMNCR");
    validateRowValueByName(
        response, actualHeaders, 0, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mokorbu MCHP");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "psi", "RPNgwvx5j65");
    validateRowValueByName(
        response, actualHeaders, 99, "Zj7UnCAulEk.eventdate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 99, "ouname", "Gbenikoro MCHP");
    validateRowValueByName(response, actualHeaders, 99, "programstatus", "");

    // where:
    // (ax."occurreddate" <= '2023-05-01' and ax."ps" = 'Zj7UnCAulEk')
  }

  @Test
  public void stageAndEventDateMultipleStagesRejected() {

    // The test verifies that querying with multiple stages in stage-specific dimensions
    // results in a conflict error. At the moment, only one stage per query is supported.

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.EVENT_DATE:2022,jdRD35YwbRH.EVENT_DATE:2023")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("httpStatusCode", equalTo(409))
        .body("status", equalTo("ERROR"))
        .body(
            "message",
            equalTo(
                "Multiple stages in stage-specific dimensions are not allowed: `ZkbAXlQUYJG, jdRD35YwbRH`"))
        .body("errorCode", equalTo("E7244"));
  }

  @Test
  public void stageAndInvalidOu() {

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.ou:THIS_YEAR")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("httpStatusCode", equalTo(409))
        .body("status", equalTo("ERROR"))
        .body("message", equalTo("Organisation unit or organisation unit level is not valid"))
        .body("errorCode", equalTo("E7143"));
  }

  @Test
  public void stageAndSimpleOu() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.ou:ImspTQPwCqd,pe:2022")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        4,
        23,
        19); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":4,\"pageCount\":1,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"code\":\"OU_525\",\"name\":\"Sierra Leone\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"ZkbAXlQUYJG.ou\":{\"name\":\"Organisation unit\"},\"2022\":{\"name\":\"2022\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"}},\"dimensions\":{\"pe\":[],\"ZkbAXlQUYJG.ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "incidentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "tei", "Tracked entity", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pi", "Program instance", "TEXT", "java.lang.String", false, true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.ou",
        "Organisation unit",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: evenly spaced rows, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "IQCiAZs7PrK");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2021-05-14 12:35:24.03");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2021-04-25 12:35:24.03");
    validateRowValueByName(response, actualHeaders, 0, "tei", "LxMVYhJm3Jp");
    validateRowValueByName(response, actualHeaders, 0, "pi", "awZ5RHoJin5");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "ACTIVE");

    // Validate selected values for row index 1
    validateRowValueByName(response, actualHeaders, 1, "psi", "La2PAKKx3it");
    validateRowValueByName(response, actualHeaders, 1, "enrollmentdate", "2022-08-11 12:32:30.524");
    validateRowValueByName(response, actualHeaders, 1, "incidentdate", "2022-08-05 02:00:00.0");
    validateRowValueByName(response, actualHeaders, 1, "tei", "pUK3xmXayQ5");
    validateRowValueByName(response, actualHeaders, 1, "pi", "hXECENVui3x");
    validateRowValueByName(response, actualHeaders, 1, "programstatus", "ACTIVE");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "psi", "qgaHGxGEI56");
    validateRowValueByName(response, actualHeaders, 2, "enrollmentdate", "2023-01-01 01:00:00.0");
    validateRowValueByName(response, actualHeaders, 2, "incidentdate", "2023-01-01 01:00:00.0");
    validateRowValueByName(response, actualHeaders, 2, "tei", "fSofnQR6lAU");
    validateRowValueByName(response, actualHeaders, 2, "pi", "fMCNMupsPrg");
    validateRowValueByName(response, actualHeaders, 2, "programstatus", "CANCELLED");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "psi", "BijwU5PwIMh");
    validateRowValueByName(response, actualHeaders, 3, "enrollmentdate", "2023-01-15 01:00:00.0");
    validateRowValueByName(response, actualHeaders, 3, "incidentdate", "2023-01-15 01:00:00.0");
    validateRowValueByName(response, actualHeaders, 3, "tei", "fSofnQR6lAU");
    validateRowValueByName(response, actualHeaders, 3, "pi", "czKU08gniYG");
    validateRowValueByName(response, actualHeaders, 3, "programstatus", "ACTIVE");
  }

  @Test
  public void stageAndOuUserOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.ou:USER_ORGUNIT,pe:202206")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        23,
        19); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":1,\"pageCount\":1,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"code\":\"OU_525\",\"name\":\"Sierra Leone\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"ZkbAXlQUYJG.ou\":{\"name\":\"Organisation unit\"},\"202206\":{\"name\":\"June 2022\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"}},\"dimensions\":{\"pe\":[],\"ZkbAXlQUYJG.ou\":[\"ImspTQPwCqd\"]}}\n";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "incidentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "tei", "Tracked entity", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pi", "Program instance", "TEXT", "java.lang.String", false, true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
    }
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);

      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.ou",
        "Organisation unit",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: evenly spaced rows, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "La2PAKKx3it");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2022-08-11 12:32:30.524");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2022-08-05 02:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "tei", "pUK3xmXayQ5");
    validateRowValueByName(response, actualHeaders, 0, "pi", "hXECENVui3x");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "ACTIVE");
  }

  @Test
  public void stageAndOuUserLevel() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.ou:LEVEL-3,pe:202111")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        2,
        23,
        19); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":2,\"pageCount\":1,\"pageSize\":100,\"page\":1},\"items\":{\"yu4N82FFeLm\":{\"code\":\"OU_204910\",\"name\":\"Mandu\"},\"KXSqt7jv6DU\":{\"code\":\"OU_222627\",\"name\":\"Gorama Mende\"},\"lY93YpCxJqf\":{\"code\":\"OU_193249\",\"name\":\"Makari Gbanti\"},\"eROJsBwxQHt\":{\"code\":\"OU_222743\",\"name\":\"Gaura\"},\"DxAPPqXvwLy\":{\"code\":\"OU_204929\",\"name\":\"Peje Bongre\"},\"PaqugoqjRIj\":{\"code\":\"OU_226225\",\"name\":\"Sulima (Koinadugu)\"},\"gy8rmvYT4cj\":{\"code\":\"OU_247037\",\"name\":\"Ribbi\"},\"RzKeCma9qb1\":{\"code\":\"OU_260428\",\"name\":\"Barri\"},\"vULnao2hV5v\":{\"code\":\"OU_247086\",\"name\":\"Fakunya\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"EfWCa0Cc8WW\":{\"code\":\"OU_255030\",\"name\":\"Masimera\"},\"AovmOHadayb\":{\"code\":\"OU_247044\",\"name\":\"Timidale\"},\"hjpHnHZIniP\":{\"code\":\"OU_204887\",\"name\":\"Kissi Tongi\"},\"U6Kr7Gtpidn\":{\"code\":\"OU_546\",\"name\":\"Kakua\"},\"EYt6ThQDagn\":{\"code\":\"OU_222642\",\"name\":\"Koya (kenema)\"},\"iUauWFeH8Qp\":{\"code\":\"OU_197402\",\"name\":\"Bum\"},\"Jiyc4ekaMMh\":{\"code\":\"OU_247080\",\"name\":\"Kongbora\"},\"FlBemv1NfEC\":{\"code\":\"OU_211256\",\"name\":\"Masungbala\"},\"XrF5AvaGcuw\":{\"code\":\"OU_226240\",\"name\":\"Wara Wara Bafodia\"},\"LhaAPLxdSFH\":{\"code\":\"OU_233348\",\"name\":\"Lei\"},\"BmYyh9bZ0sr\":{\"code\":\"OU_268197\",\"name\":\"Kafe Simira\"},\"pk7bUK5c1Uf\":{\"code\":\"OU_260397\",\"name\":\"Ya Kpukumu Krim\"},\"zSNUViKdkk3\":{\"code\":\"OU_260440\",\"name\":\"Kpaka\"},\"r06ohri9wA9\":{\"code\":\"OU_211243\",\"name\":\"Samu\"},\"ERmBhYkhV6Y\":{\"code\":\"OU_204877\",\"name\":\"Njaluahun\"},\"Z9QaI6sxTwW\":{\"code\":\"OU_247068\",\"name\":\"Kargboro\"},\"daJPPxtIrQn\":{\"code\":\"OU_545\",\"name\":\"Jaiama Bongor\"},\"W5fN3G6y1VI\":{\"code\":\"OU_247012\",\"name\":\"Lower Banta\"},\"r1RUyfVBkLp\":{\"code\":\"OU_268169\",\"name\":\"Sambaia Bendugu\"},\"NNE0YMCDZkO\":{\"code\":\"OU_268225\",\"name\":\"Yoni\"},\"ENHOJz3UH5L\":{\"code\":\"OU_197440\",\"name\":\"BMC\"},\"QywkxFudXrC\":{\"code\":\"OU_211227\",\"name\":\"Magbema\"},\"jWSIbtKfURj\":{\"code\":\"OU_222751\",\"name\":\"Langrama\"},\"J4GiUImJZoE\":{\"code\":\"OU_226269\",\"name\":\"Nieni\"},\"CF243RPvNY7\":{\"code\":\"OU_233359\",\"name\":\"Fiama\"},\"I4jWcnFmgEC\":{\"code\":\"OU_549\",\"name\":\"Niawa Lenga\"},\"cM2BKSrj9F9\":{\"code\":\"OU_204894\",\"name\":\"Luawa\"},\"kvkDWg42lHR\":{\"code\":\"OU_233339\",\"name\":\"Kamara\"},\"jPidqyo7cpF\":{\"code\":\"OU_247049\",\"name\":\"Bagruwa\"},\"BGGmAwx33dj\":{\"code\":\"OU_543\",\"name\":\"Bumpe Ngao\"},\"iGHlidSFdpu\":{\"code\":\"OU_233317\",\"name\":\"Soa\"},\"g8DdBm7EmUt\":{\"code\":\"OU_197397\",\"name\":\"Sittia\"},\"ZiOVcrSjSYe\":{\"code\":\"OU_254976\",\"name\":\"Dibia\"},\"vn9KJsLyP5f\":{\"code\":\"OU_255005\",\"name\":\"Kaffu Bullom\"},\"QlCIp2S9NHs\":{\"code\":\"OU_222682\",\"name\":\"Dodo\"},\"j43EZb15rjI\":{\"code\":\"OU_193285\",\"name\":\"Sella Limba\"},\"bQiBfA2j5cw\":{\"code\":\"OU_204857\",\"name\":\"Penguia\"},\"NqWaKXcg01b\":{\"code\":\"OU_260384\",\"name\":\"Sowa\"},\"VP397wRvePm\":{\"code\":\"OU_197445\",\"name\":\"Nongoba Bullum\"},\"fwxkctgmffZ\":{\"code\":\"OU_268163\",\"name\":\"Kholifa Mabang\"},\"QwMiPiME3bA\":{\"code\":\"OU_260400\",\"name\":\"Kpanga Kabonde\"},\"Qhmi8IZyPyD\":{\"code\":\"OU_193245\",\"name\":\"Tambaka\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"OTFepb1k9Db\":{\"code\":\"OU_226244\",\"name\":\"Mongo\"},\"DBs6e2Oxaj1\":{\"code\":\"OU_247002\",\"name\":\"Upper Banta\"},\"ZkbAXlQUYJG.ou\":{\"name\":\"Organisation unit\"},\"eV4cuxniZgP\":{\"code\":\"OU_193224\",\"name\":\"Magbaimba Ndowahun\"},\"xhyjU2SVewz\":{\"code\":\"OU_268217\",\"name\":\"Tane\"},\"dGheVylzol6\":{\"code\":\"OU_541\",\"name\":\"Bargbe\"},\"vWbkYPRmKyS\":{\"code\":\"OU_540\",\"name\":\"Baoma\"},\"npWGUj37qDe\":{\"code\":\"OU_552\",\"name\":\"Valunia\"},\"TA7NvKjsn4A\":{\"code\":\"OU_255041\",\"name\":\"Bureh Kasseh Maconteh\"},\"myQ4q1W6B4y\":{\"code\":\"OU_222731\",\"name\":\"Dama\"},\"nV3OkyzF4US\":{\"code\":\"OU_246991\",\"name\":\"Kori\"},\"X7dWcGerQIm\":{\"code\":\"OU_222677\",\"name\":\"Wandor\"},\"qIRCo0MfuGb\":{\"code\":\"OU_211213\",\"name\":\"Gbinleh Dixion\"},\"kbPmt60yi0L\":{\"code\":\"OU_211220\",\"name\":\"Bramaia\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"eNtRuQrrZeo\":{\"code\":\"OU_260420\",\"name\":\"Galliness Perri\"},\"HV8RTzgcFH3\":{\"code\":\"OU_197432\",\"name\":\"Kwamabai Krim\"},\"KKkLOTpMXGV\":{\"code\":\"OU_193198\",\"name\":\"Bombali Sebora\"},\"Pc3JTyqnsmL\":{\"code\":\"OU_255020\",\"name\":\"Buya Romende\"},\"hdEuw2ugkVF\":{\"code\":\"OU_222652\",\"name\":\"Lower Bambara\"},\"l7pFejMtUoF\":{\"code\":\"OU_222634\",\"name\":\"Tunkia\"},\"K1r3uF6eZ8n\":{\"code\":\"OU_222725\",\"name\":\"Kandu Lepiema\"},\"VGAFxBXz16y\":{\"code\":\"OU_226231\",\"name\":\"Sengbeh\"},\"GE25DpSrqpB\":{\"code\":\"OU_204869\",\"name\":\"Malema\"},\"ARZ4y5i4reU\":{\"code\":\"OU_553\",\"name\":\"Wonde\"},\"byp7w6Xd9Df\":{\"code\":\"OU_204933\",\"name\":\"Yawei\"},\"BXJdOLvUrZB\":{\"code\":\"OU_193277\",\"name\":\"Gbendembu Ngowahun\"},\"uKC54fzxRzO\":{\"code\":\"OU_222648\",\"name\":\"Niawa\"},\"TQkG0sX9nca\":{\"code\":\"OU_233375\",\"name\":\"Gbense\"},\"hRZOIgQ0O1m\":{\"code\":\"OU_193302\",\"name\":\"Libeisaygahun\"},\"PrJQHI6q7w2\":{\"code\":\"OU_255061\",\"name\":\"Tainkatopa Makama Safrokoh\"},\"USQdmvrHh1Q\":{\"code\":\"OU_247055\",\"name\":\"Kaiyamba\"},\"xGMGhjA3y6J\":{\"code\":\"OU_211262\",\"name\":\"Mambolo\"},\"nOYt1LtFSyU\":{\"code\":\"OU_247025\",\"name\":\"Bumpeh\"},\"e1eIKM1GIF3\":{\"code\":\"OU_193215\",\"name\":\"Gbanti Kamaranka\"},\"lYIM1MXbSYS\":{\"code\":\"OU_204920\",\"name\":\"Dea\"},\"sxRd2XOzFbz\":{\"code\":\"OU_551\",\"name\":\"Tikonko\"},\"d9iMR1MpuIO\":{\"code\":\"OU_260410\",\"name\":\"Soro-Gbeima\"},\"qgQ49DH9a0v\":{\"code\":\"OU_233332\",\"name\":\"Nimiyama\"},\"U09TSwIjG0s\":{\"code\":\"OU_222617\",\"name\":\"Nomo\"},\"zFDYIgyGmXG\":{\"code\":\"OU_542\",\"name\":\"Bargbo\"},\"M2qEv692lS6\":{\"code\":\"OU_233324\",\"name\":\"Tankoro\"},\"qtr8GGlm4gg\":{\"code\":\"OU_278366\",\"name\":\"Rural Western Area\"},\"pRHGAROvuyI\":{\"code\":\"OU_254960\",\"name\":\"Koya\"},\"xIKjidMrico\":{\"code\":\"OU_247033\",\"name\":\"Kowa\"},\"GWTIxJO9pRo\":{\"code\":\"OU_233355\",\"name\":\"Gorama Kono\"},\"HWjrSuoNPte\":{\"code\":\"OU_254999\",\"name\":\"Sanda Magbolonthor\"},\"UhHipWG7J8b\":{\"code\":\"OU_193191\",\"name\":\"Sanda Tendaren\"},\"rXLor9Knq6l\":{\"code\":\"OU_268212\",\"name\":\"Kunike Barina\"},\"kU8vhUkAGaT\":{\"code\":\"OU_548\",\"name\":\"Lugbu\"},\"C9uduqDZr9d\":{\"code\":\"OU_278311\",\"name\":\"Freetown\"},\"YmmeuGbqOwR\":{\"code\":\"OU_544\",\"name\":\"Gbo\"},\"iEkBZnMDarP\":{\"code\":\"OU_226253\",\"name\":\"Folosaba Dembelia\"},\"JsxnA2IywRo\":{\"code\":\"OU_204875\",\"name\":\"Kissi Kama\"},\"nlt6j60tCHF\":{\"code\":\"OU_260437\",\"name\":\"Mano Sakrim\"},\"g5ptsn0SFX8\":{\"code\":\"OU_233365\",\"name\":\"Sandor\"},\"XG8HGAbrbbL\":{\"code\":\"OU_193267\",\"name\":\"Safroko Limba\"},\"pmxZm7klXBy\":{\"code\":\"OU_204924\",\"name\":\"Peje West\"},\"l0ccv2yzfF3\":{\"code\":\"OU_268174\",\"name\":\"Kunike\"},\"YuQRtpLP10I\":{\"code\":\"OU_539\",\"name\":\"Badjia\"},\"LfTkc0S4b5k\":{\"code\":\"OU_204915\",\"name\":\"Upper Bambara\"},\"KSdZwrU7Hh6\":{\"code\":\"OU_204861\",\"name\":\"Jawi\"},\"ajILkI0cfxn\":{\"code\":\"OU_233390\",\"name\":\"Gbane\"},\"y5X4mP5XylL\":{\"code\":\"OU_211270\",\"name\":\"Tonko Limba\"},\"fwH9ipvXde9\":{\"code\":\"OU_193228\",\"name\":\"Biriwa\"},\"KIUCimTXf8Q\":{\"code\":\"OU_222690\",\"name\":\"Nongowa\"},\"vEvs2ckGNQj\":{\"code\":\"OU_226219\",\"name\":\"Kasonko\"},\"XEyIRFd9pct\":{\"code\":\"OU_197413\",\"name\":\"Imperi\"},\"cgOy0hRMGu9\":{\"code\":\"OU_197408\",\"name\":\"Sogbini\"},\"EjnIQNVAXGp\":{\"code\":\"OU_233344\",\"name\":\"Mafindor\"},\"x4HaBHHwBML\":{\"code\":\"OU_222672\",\"name\":\"Malegohun\"},\"EZPwuUTeIIG\":{\"code\":\"OU_226258\",\"name\":\"Wara Wara Yagala\"},\"N233eZJZ1bh\":{\"code\":\"OU_260388\",\"name\":\"Pejeh\"},\"smoyi1iYNK6\":{\"code\":\"OU_268191\",\"name\":\"Kalansogoia\"},\"DNRAeXT9IwS\":{\"code\":\"OU_197421\",\"name\":\"Dema\"},\"fRLX08WHWpL\":{\"code\":\"OU_254982\",\"name\":\"Lokomasama\"},\"Zoy23SSHCPs\":{\"code\":\"OU_233311\",\"name\":\"Gbane Kandor\"},\"LsYpCyYxSLY\":{\"code\":\"OU_247008\",\"name\":\"Kamaje\"},\"JdqfYTIFZXN\":{\"code\":\"OU_254946\",\"name\":\"Maforki\"},\"EB1zRKdYjdY\":{\"code\":\"OU_197429\",\"name\":\"Bendu Cha\"},\"CG4QD1HC3h4\":{\"code\":\"OU_197436\",\"name\":\"Yawbeko\"},\"202111\":{\"name\":\"November 2021\"},\"YpVol7asWvd\":{\"code\":\"OU_260417\",\"name\":\"Kpanga Krim\"},\"RndxKqQGzUl\":{\"code\":\"OU_247018\",\"name\":\"Dasse\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"P69SId31eDp\":{\"code\":\"OU_268202\",\"name\":\"Gbonkonlenken\"},\"aWQTfvgPA5v\":{\"code\":\"OU_197424\",\"name\":\"Kpanda Kemoh\"},\"KctpIIucige\":{\"code\":\"OU_550\",\"name\":\"Selenga\"},\"Mr4au3jR9bt\":{\"code\":\"OU_226214\",\"name\":\"Dembelia Sinkunia\"},\"L8iA6eLwKNb\":{\"code\":\"OU_193295\",\"name\":\"Paki Masabong\"},\"j0Mtr3xTMjM\":{\"code\":\"OU_204939\",\"name\":\"Kissi Teng\"},\"DmaLM8WYmWv\":{\"code\":\"OU_233394\",\"name\":\"Nimikoro\"},\"DfUfwjM9am5\":{\"code\":\"OU_260392\",\"name\":\"Malen\"},\"FRxcUEwktoV\":{\"code\":\"OU_233314\",\"name\":\"Toli\"},\"VCtF1DbspR5\":{\"code\":\"OU_197386\",\"name\":\"Jong\"},\"BD9gU0GKlr2\":{\"code\":\"OU_260378\",\"name\":\"Makpele\"},\"GFk45MOxzJJ\":{\"code\":\"OU_226275\",\"name\":\"Neya\"},\"A3Fh37HWBWE\":{\"code\":\"OU_222687\",\"name\":\"Simbaru\"},\"vzup1f6ynON\":{\"code\":\"OU_222619\",\"name\":\"Small Bo\"},\"Lt8U7GVWvSR\":{\"code\":\"OU_226263\",\"name\":\"Diang\"},\"JdhagCUEMbj\":{\"code\":\"OU_547\",\"name\":\"Komboya\"},\"EVkm2xYcf6Z\":{\"code\":\"OU_268184\",\"name\":\"Malal Mara\"},\"PQZJPIpTepd\":{\"code\":\"OU_268150\",\"name\":\"Kholifa Rowalla\"},\"WXnNDWTiE9r\":{\"code\":\"OU_193239\",\"name\":\"Sanda Loko\"},\"RWvG1aFrr0r\":{\"code\":\"OU_255053\",\"name\":\"Marampa\"}},\"dimensions\":{\"pe\":[],\"ZkbAXlQUYJG.ou\":[\"YuQRtpLP10I\",\"jPidqyo7cpF\",\"vWbkYPRmKyS\",\"dGheVylzol6\",\"zFDYIgyGmXG\",\"RzKeCma9qb1\",\"EB1zRKdYjdY\",\"fwH9ipvXde9\",\"ENHOJz3UH5L\",\"KKkLOTpMXGV\",\"kbPmt60yi0L\",\"iUauWFeH8Qp\",\"BGGmAwx33dj\",\"nOYt1LtFSyU\",\"TA7NvKjsn4A\",\"Pc3JTyqnsmL\",\"myQ4q1W6B4y\",\"RndxKqQGzUl\",\"lYIM1MXbSYS\",\"DNRAeXT9IwS\",\"Mr4au3jR9bt\",\"Lt8U7GVWvSR\",\"ZiOVcrSjSYe\",\"QlCIp2S9NHs\",\"vULnao2hV5v\",\"CF243RPvNY7\",\"iEkBZnMDarP\",\"C9uduqDZr9d\",\"eNtRuQrrZeo\",\"eROJsBwxQHt\",\"ajILkI0cfxn\",\"Zoy23SSHCPs\",\"e1eIKM1GIF3\",\"BXJdOLvUrZB\",\"TQkG0sX9nca\",\"qIRCo0MfuGb\",\"YmmeuGbqOwR\",\"P69SId31eDp\",\"GWTIxJO9pRo\",\"KXSqt7jv6DU\",\"XEyIRFd9pct\",\"daJPPxtIrQn\",\"KSdZwrU7Hh6\",\"VCtF1DbspR5\",\"BmYyh9bZ0sr\",\"vn9KJsLyP5f\",\"USQdmvrHh1Q\",\"U6Kr7Gtpidn\",\"smoyi1iYNK6\",\"LsYpCyYxSLY\",\"kvkDWg42lHR\",\"K1r3uF6eZ8n\",\"Z9QaI6sxTwW\",\"vEvs2ckGNQj\",\"fwxkctgmffZ\",\"PQZJPIpTepd\",\"JsxnA2IywRo\",\"j0Mtr3xTMjM\",\"hjpHnHZIniP\",\"JdhagCUEMbj\",\"Jiyc4ekaMMh\",\"nV3OkyzF4US\",\"xIKjidMrico\",\"pRHGAROvuyI\",\"EYt6ThQDagn\",\"zSNUViKdkk3\",\"aWQTfvgPA5v\",\"QwMiPiME3bA\",\"YpVol7asWvd\",\"l0ccv2yzfF3\",\"rXLor9Knq6l\",\"HV8RTzgcFH3\",\"jWSIbtKfURj\",\"LhaAPLxdSFH\",\"hRZOIgQ0O1m\",\"fRLX08WHWpL\",\"hdEuw2ugkVF\",\"W5fN3G6y1VI\",\"cM2BKSrj9F9\",\"kU8vhUkAGaT\",\"EjnIQNVAXGp\",\"JdqfYTIFZXN\",\"eV4cuxniZgP\",\"QywkxFudXrC\",\"lY93YpCxJqf\",\"BD9gU0GKlr2\",\"EVkm2xYcf6Z\",\"x4HaBHHwBML\",\"GE25DpSrqpB\",\"DfUfwjM9am5\",\"xGMGhjA3y6J\",\"yu4N82FFeLm\",\"nlt6j60tCHF\",\"RWvG1aFrr0r\",\"EfWCa0Cc8WW\",\"FlBemv1NfEC\",\"OTFepb1k9Db\",\"GFk45MOxzJJ\",\"uKC54fzxRzO\",\"I4jWcnFmgEC\",\"J4GiUImJZoE\",\"DmaLM8WYmWv\",\"qgQ49DH9a0v\",\"ERmBhYkhV6Y\",\"U09TSwIjG0s\",\"VP397wRvePm\",\"KIUCimTXf8Q\",\"L8iA6eLwKNb\",\"DxAPPqXvwLy\",\"pmxZm7klXBy\",\"N233eZJZ1bh\",\"bQiBfA2j5cw\",\"gy8rmvYT4cj\",\"qtr8GGlm4gg\",\"XG8HGAbrbbL\",\"r1RUyfVBkLp\",\"r06ohri9wA9\",\"WXnNDWTiE9r\",\"HWjrSuoNPte\",\"UhHipWG7J8b\",\"g5ptsn0SFX8\",\"KctpIIucige\",\"j43EZb15rjI\",\"VGAFxBXz16y\",\"A3Fh37HWBWE\",\"g8DdBm7EmUt\",\"vzup1f6ynON\",\"iGHlidSFdpu\",\"cgOy0hRMGu9\",\"d9iMR1MpuIO\",\"NqWaKXcg01b\",\"PaqugoqjRIj\",\"PrJQHI6q7w2\",\"Qhmi8IZyPyD\",\"xhyjU2SVewz\",\"M2qEv692lS6\",\"sxRd2XOzFbz\",\"AovmOHadayb\",\"FRxcUEwktoV\",\"y5X4mP5XylL\",\"l7pFejMtUoF\",\"LfTkc0S4b5k\",\"DBs6e2Oxaj1\",\"npWGUj37qDe\",\"X7dWcGerQIm\",\"XrF5AvaGcuw\",\"EZPwuUTeIIG\",\"ARZ4y5i4reU\",\"pk7bUK5c1Uf\",\"CG4QD1HC3h4\",\"byp7w6Xd9Df\",\"NNE0YMCDZkO\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "incidentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "tei", "Tracked entity", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pi", "Program instance", "TEXT", "java.lang.String", false, true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);

      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.ou",
        "Organisation unit",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: evenly spaced rows, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "o6G3PSfXK8L");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2021-11-20 12:27:48.415");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2021-10-29 12:27:48.415");
    validateRowValueByName(response, actualHeaders, 0, "tei", "fSofnQR6lAU");
    validateRowValueByName(response, actualHeaders, 0, "pi", "Cl7ZhgxaYQO");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "CANCELLED");

    // Validate selected values for row index 1
    validateRowValueByName(response, actualHeaders, 1, "psi", "TAZ4L5XN1oD");
    validateRowValueByName(response, actualHeaders, 1, "enrollmentdate", "2021-09-11 12:27:48.552");
    validateRowValueByName(response, actualHeaders, 1, "incidentdate", "2021-09-10 12:27:48.552");
    validateRowValueByName(response, actualHeaders, 1, "tei", "uh47DXf1St9");
    validateRowValueByName(response, actualHeaders, 1, "pi", "iSNBeFcHO0X");
    validateRowValueByName(response, actualHeaders, 1, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 1, "programstatus", "ACTIVE");
  }

  @Test
  @Disabled("Disabled since ou logic not clear")
  public void stageAndOuMultipleOus() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            // .add("dimension=A03MvHHogjR.ou:at6UHUQatSo;GE25DpSrqpB")
            .add("dimension=ou:at6UHUQatSo;WMj6mBDw76A")
            .add("dimension=pe:THIS_YEAR")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // where:ur1Edk5Oe2n
    // and ax."uidlevel1" in ('ImspTQPwCqd') and (ax."uidlevel2" in ('eIQbndfxQMb') and
    // ax."uidlevel4" in ('WjO2puYKysP') and ax."ps" = 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndScheduledDate() throws JSONException {

    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.SCHEDULED_DATE:202208")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        23,
        19); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":1,\"pageCount\":1,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"202208\":{\"name\":\"August 2022\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"ZkbAXlQUYJG.scheduleddate\":{\"name\":\"Scheduled date\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"ZkbAXlQUYJG.scheduleddate\":[\"202208\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "incidentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "tei", "Tracked entity", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pi", "Program instance", "TEXT", "java.lang.String", false, true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);

      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);

      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.scheduleddate",
        "Scheduled date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "blZxytttTqq");
    validateRowValueByName(
        response, actualHeaders, 0, "ZkbAXlQUYJG.scheduleddate", "2022-08-05 19:25:49.996");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2022-02-09 12:27:48.637");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2022-01-29 12:27:48.637");
    validateRowValueByName(response, actualHeaders, 0, "tei", "PQfMcpmXeFE");
    validateRowValueByName(response, actualHeaders, 0, "pi", "Yf47yST5FF2");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "ACTIVE");

    // where:
    // and (ax."scheduleddate" >= '2019-10-01' and ax."scheduleddate" <= '2019-10-31' and ax."ps" =
    // 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndStatus() throws JSONException {

    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.EVENT_STATUS:ACTIVE,pe:2021")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        23,
        19); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":1,\"pageCount\":1,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"2021\":{\"name\":\"2021\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"ZkbAXlQUYJG.eventstatus\":{\"name\":\"Event status\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"ZkbAXlQUYJG.eventstatus\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "incidentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "tei", "Tracked entity", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pi", "Program instance", "TEXT", "java.lang.String", false, true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "QsAhMiZtnl2");
    validateRowValueByName(response, actualHeaders, 0, "ZkbAXlQUYJG.eventstatus", "ACTIVE");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2021-06-17 12:27:48.595");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2021-06-05 12:27:48.595");
    validateRowValueByName(response, actualHeaders, 0, "tei", "foc5zag6gbE");
    validateRowValueByName(response, actualHeaders, 0, "pi", "SolDyMgW3oc");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "ACTIVE");

    // where:
    // (ax."eventstatus" in ('ACTIVE') and ax."ps" = 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndCategory() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=eventdate")
            .add("headers=kO3z4Dhc038.LFsZ8v5v7rq,oucode")
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=pe:2021,kO3z4Dhc038.LFsZ8v5v7rq:CW81uF03hvV;B3nxOazOO2G")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("bMcwwoVnbSR", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        10,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":658,\"pageSize\":10,\"pageCount\":66},\"items\":{\"RkbOhHwiOgW\":{\"name\":\"CARE International\"},\"kO3z4Dhc038.LFsZ8v5v7rq\":{\"name\":\"Implementing Partner\"},\"ou\":{},\"uilaJSyXt7d\":{\"name\":\"World Vision\"},\"2021\":{\"name\":\"2021\"},\"VLFVaH1MwnF\":{\"name\":\"Pathfinder International\"},\"CW81uF03hvV\":{\"name\":\"AIDSRelief Consortium\"},\"hERJraxV8D9\":{\"name\":\"Hope Worldwide\"},\"g3bcPGD5Q5i\":{\"name\":\"International Rescue Committee\"},\"yrwgRxRhBoU\":{\"name\":\"Path\"},\"TY5rBQzlBRa\":{\"name\":\"Family Health International\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"yfWXlxYNbhy\":{\"name\":\"IntraHealth International\"},\"e5YBV5F5iUd\":{\"name\":\"Plan International\"},\"LEWNFo4Qrrs\":{\"name\":\"World Concern\"},\"XK6u6cJCR0t\":{\"name\":\"Population Services International\"},\"C6nZpLKjEJr\":{\"name\":\"African Medical and Research Foundation\"},\"bMcwwoVnbSR\":{\"name\":\"Malaria testing and surveillance\"},\"B3nxOazOO2G\":{\"name\":\"APHIAplus\"},\"kO3z4Dhc038\":{\"name\":\"Malaria testing and surveillance\"},\"xwZ2u3WyQR0\":{\"name\":\"Unicef\"},\"xEunk8LPzkb\":{\"name\":\"World Relief\"}},\"dimensions\":{\"kO3z4Dhc038.LFsZ8v5v7rq\":[\"CW81uF03hvV\",\"B3nxOazOO2G\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "kO3z4Dhc038.LFsZ8v5v7rq",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "kO3z4Dhc038.LFsZ8v5v7rq", "B3nxOazOO2G");
    validateRowValueByName(response, actualHeaders, 0, "oucode", "OU_226216");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "kO3z4Dhc038.LFsZ8v5v7rq", "B3nxOazOO2G");
    validateRowValueByName(response, actualHeaders, 3, "oucode", "OU_222657");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "kO3z4Dhc038.LFsZ8v5v7rq", "CW81uF03hvV");
    validateRowValueByName(response, actualHeaders, 6, "oucode", "OU_254980");

    // Validate selected values for row index 9
    validateRowValueByName(response, actualHeaders, 9, "kO3z4Dhc038.LFsZ8v5v7rq", "B3nxOazOO2G");
    validateRowValueByName(response, actualHeaders, 9, "oucode", "OU_255018");
  }

  @Test
  public void stageAndCategoryOptionGroupSet() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=eventdate")
            .add("headers=kO3z4Dhc038.C31vHZqu0qU,oucode")
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=pe:2021,kO3z4Dhc038.C31vHZqu0qU:j3C417uW6J7;ddAo6zmIHOk")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("bMcwwoVnbSR", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        10,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":1582,\"pageSize\":10,\"pageCount\":159},\"items\":{\"uilaJSyXt7d\":{\"name\":\"World Vision\"},\"VLFVaH1MwnF\":{\"name\":\"Pathfinder International\"},\"CW81uF03hvV\":{\"name\":\"AIDSRelief Consortium\"},\"hERJraxV8D9\":{\"name\":\"Hope Worldwide\"},\"TY5rBQzlBRa\":{\"name\":\"Family Health International\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"XK6u6cJCR0t\":{\"name\":\"Population Services International\"},\"B3nxOazOO2G\":{\"name\":\"APHIAplus\"},\"ddAo6zmIHOk\":{\"name\":\"DFID\"},\"j3C417uW6J7\":{\"name\":\"DANIDA\"},\"RkbOhHwiOgW\":{\"name\":\"CARE International\"},\"ou\":{},\"2021\":{\"name\":\"2021\"},\"g3bcPGD5Q5i\":{\"name\":\"International Rescue Committee\"},\"yrwgRxRhBoU\":{\"name\":\"Path\"},\"kO3z4Dhc038.C31vHZqu0qU\":{\"name\":\"Donor\"},\"yfWXlxYNbhy\":{\"name\":\"IntraHealth International\"},\"e5YBV5F5iUd\":{\"name\":\"Plan International\"},\"LEWNFo4Qrrs\":{\"name\":\"World Concern\"},\"C6nZpLKjEJr\":{\"name\":\"African Medical and Research Foundation\"},\"bMcwwoVnbSR\":{\"name\":\"Malaria testing and surveillance\"},\"kO3z4Dhc038\":{\"name\":\"Malaria testing and surveillance\"},\"xwZ2u3WyQR0\":{\"name\":\"Unicef\"},\"xEunk8LPzkb\":{\"name\":\"World Relief\"}},\"dimensions\":{\"kO3z4Dhc038.C31vHZqu0qU\":[\"j3C417uW6J7\",\"ddAo6zmIHOk\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "kO3z4Dhc038.C31vHZqu0qU",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "kO3z4Dhc038.C31vHZqu0qU", "ddAo6zmIHOk");
    validateRowValueByName(response, actualHeaders, 0, "oucode", "OU_226242");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "kO3z4Dhc038.C31vHZqu0qU", "ddAo6zmIHOk");
    validateRowValueByName(response, actualHeaders, 3, "oucode", "OU_197405");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "kO3z4Dhc038.C31vHZqu0qU", "ddAo6zmIHOk");
    validateRowValueByName(response, actualHeaders, 6, "oucode", "OU_204931");

    // Validate selected values for row index 9
    validateRowValueByName(response, actualHeaders, 9, "kO3z4Dhc038.C31vHZqu0qU", "ddAo6zmIHOk");
    validateRowValueByName(response, actualHeaders, 9, "oucode", "OU_260419");
  }

  @Test
  public void enrollmentOuWithLevel() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=eventdate")
            .add("headers=oucode,enrollmentou,enrollmentouname")
            .add("displayProperty=NAME")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=ENROLLMENT_OU:LEVEL-m9lBJogzE95,pe:2021")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);
    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        10,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":10,\"pageCount\":1,\"pageSize\":10,\"page\":1},\"items\":{\"vRC0stJ5y9Q\":{\"name\":\"Bucksal Clinic\"},\"simyC07XwnS\":{\"name\":\"Maforay MCHP\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"E9oBVjyEaCe\":{\"name\":\"Gbanja Town MCHP\"},\"yTMrs5kClCv\":{\"name\":\"Condama MCHP\"},\"ZpE2POxvl9P\":{\"name\":\"Faabu CHP\"},\"FO1Tq8vUa62\":{\"name\":\"EPI Headquarter\"},\"jGYT5U5qJP6\":{\"name\":\"Gbaiima CHC\"},\"LaxJ6CD2DHq\":{\"name\":\"EM&BEE Maternity Home Clinic\"},\"WerHl8SDtRU\":{\"name\":\"Mandema CHP\"},\"RHJram03Rlm\":{\"name\":\"Makonkondey MCHP\"},\"CTnuuI55SOj\":{\"name\":\"Manewa MCHP\"},\"MHAWZr2Caxw\":{\"name\":\"Bakeloko CHP\"},\"IXJg79fclDm\":{\"name\":\"Under five (Luawa) Clinic\"},\"gfWvbbgdjoS\":{\"name\":\"Tengbewabu MCHP\"},\"GIRLSZ1tB00\":{\"name\":\"Kpetema CHP (Toli)\"},\"m0PiiU5BteW\":{\"name\":\"Walia MCHP\"},\"gfk1TNPI4wN\":{\"name\":\"Mayombo MCHP\"},\"irVdYBmHBxs\":{\"name\":\"Mbowohun CHP\"},\"MQHszd6K6V5\":{\"name\":\"Pelewahun MCHP\"},\"BedE3DKQDFf\":{\"name\":\"Mokongbetty MCHP\"},\"fA43H8Ds0Ja\":{\"name\":\"Momajo MCHP\"},\"QMnoFLTLpkY\":{\"name\":\"Kemedugu MCHP\"},\"oolcy5HBlMy\":{\"name\":\"Hamilton MCHP\"},\"scc4QyxenJd\":{\"name\":\"Makali CHC\"},\"mshIal30ffW\":{\"name\":\"Mapaki CHC\"},\"AiGBODidxPw\":{\"name\":\"Nyangbe-Bo MCHP\"},\"QoROdPmIdY1\":{\"name\":\"Njagbahun (Fakunya) MCHP\"},\"iP4fRh8EHmF\":{\"name\":\"Foakor MCHP\"},\"KuGO75X47Gk\":{\"name\":\"Ngiewahun CHP\"},\"PeyblWrhOwL\":{\"name\":\"Tei CHP\"},\"UAtEKSd5QTf\":{\"name\":\"Konta (Gorama M) CHP\"},\"RG6MGu5nUlI\":{\"name\":\"Mapailleh MCHP\"},\"QsAwd531Cpd\":{\"name\":\"Njala CHC\"},\"QDoO5r6Sae7\":{\"name\":\"Yambama MCHP\"},\"HVQ6gJE8R24\":{\"name\":\"Bundulai MCHP\"},\"rNaQEFRINbd\":{\"name\":\"Punthun MCHP\"},\"yP2nhllbQPh\":{\"name\":\"New Harvest Clinic\"},\"PqlNXedmh7u\":{\"name\":\"Lumley Hospital\"},\"jCnyQOKQBFX\":{\"name\":\"St Monica's Clinic\"},\"Eyqyhztf8G1\":{\"name\":\"Ferry CHP\"},\"kzmwOrwmzbW\":{\"name\":\"Seria MCHP\"},\"WxMmxNU6Gla\":{\"name\":\"Mokaiyegbeh MCHP\"},\"M3dL6ZAIZ3I\":{\"name\":\"Gbonkomaria CHP\"},\"K3k64jslIlL\":{\"name\":\"EDC Unit CHP\"},\"cDw53Ej8rju\":{\"name\":\"Afro Arab Clinic\"},\"JKdMirJ02nv\":{\"name\":\"Makona MCHP\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"STv4PP4Hiyl\":{\"name\":\"Govt. Hosp. Pujehun\"},\"C1zlHePEQe6\":{\"name\":\"Maforay (B. Sebora) MCHP\"},\"msH78gZ7Fe6\":{\"name\":\"Royeama CHP\"},\"Xytauldn2QJ\":{\"name\":\"Dalakuru CHP\"},\"QpRIPul20Sb\":{\"name\":\"Gorahun CHC\"},\"mt47bcb0Rcj\":{\"name\":\"Kamabai CHC\"},\"hoJ0Do9loZl\":{\"name\":\"Ngogbebu MCHP\"},\"zpEPGogIr6q\":{\"name\":\"Nyandehun Nguvoihun CHP\"},\"DSBXsRQSXUW\":{\"name\":\"Handicap Clinic\"},\"NJolnlvYgLr\":{\"name\":\"Kakoya MCHP\"},\"ywNG86IY4Ve\":{\"name\":\"Gibena MCHP\"},\"XjpmsLNjyrz\":{\"name\":\"Magbaft MCHP\"},\"EO6ghLtWv4W\":{\"name\":\"Porpon MCHP\"},\"AlG0apJE5cm\":{\"name\":\"Karleh MCHP\"},\"HcB2W6Fgp7i\":{\"name\":\"Melekuray CHC\"},\"lOv6IFgr6Fs\":{\"name\":\"Manjama Shellmingo CHC\"},\"wxMmC45UyNw\":{\"name\":\"Yarawadu MCHP\"},\"koa3hwZZ2i7\":{\"name\":\"Magbethy MCHP\"},\"DcmSvQd5N8c\":{\"name\":\"Kpewama MCHP\"},\"aXsLBCzwYWW\":{\"name\":\"Konjo (Dama) CHP\"},\"F2TAF765q1b\":{\"name\":\"Kokoru CHP\"},\"XvqLmn4kZXy\":{\"name\":\"Mano Sewallu CHP\"},\"ZALwM386w0T\":{\"name\":\"Manumtheneh MCHP\"},\"UWhv0MQOqoB\":{\"name\":\"Gambia CHP\"},\"mMvt6zhCclb\":{\"name\":\"Manjama MCHP\"},\"nGb94wPdcqx\":{\"name\":\"Nagbena CHP\"},\"jVDUkOBCjDy\":{\"name\":\"Mabontor CHP\"},\"xt08cuqf1ys\":{\"name\":\"Mokoba MCHP\"},\"LOpWauwwghf\":{\"name\":\"Bambara Kaima CHP\"},\"vPz4Irz7sxR\":{\"name\":\"Njala CHP\"},\"TGRCfJEnXJr\":{\"name\":\"Yorgbofore MCHP\"},\"YhBJbiD5N1z\":{\"name\":\"Gbahama (P. Bongre) CHP\"},\"xQIU41mR69s\":{\"name\":\"Bangambaya MCHP\"},\"TWH05Rjz6oT\":{\"name\":\"Gbahama (Makpele) MCHP\"},\"EXbPGmEUdnc\":{\"name\":\"Mateboi CHC\"},\"nX05QLraDhO\":{\"name\":\"Yamandu CHC\"},\"WAjjFMDJKcx\":{\"name\":\"Blamawo MCHP\"},\"jIkxZKctVhB\":{\"name\":\"Mogbwemo CHP\"},\"cXOR7vSMBKO\":{\"name\":\"Kortuma MCHP\"},\"OUPkxfQld8y\":{\"name\":\"Mansundu (Sandor) MCHP\"},\"FGV6TAbL0eN\":{\"name\":\"Peyima CHP\"},\"D6B4jrCpCwu\":{\"name\":\"Masoko MCHP\"},\"jkPHBqdn9SA\":{\"name\":\"Konda CHP\"},\"cHqboEGRUiY\":{\"name\":\"Mokorbu MCHP\"},\"DA2BEQMhv9B\":{\"name\":\"Gbuihun MCHP\"},\"q56204kKXgZ\":{\"name\":\"Lunsar CHC\"},\"wB4R3E1X6pC\":{\"name\":\"Masanga Leprosy Hospital\"},\"qcYG2Id7GS8\":{\"name\":\"Mokpanabom MCHP\"},\"cC03EwJLBiO\":{\"name\":\"Niahun Buima MCHP\"},\"uGa5JtIMfRx\":{\"name\":\"Matoto MCHP\"},\"m3QGt8fY3L0\":{\"name\":\"Jormu CHP\"},\"bVZTNrnfn9G\":{\"name\":\"St Anthony clinic\"},\"Cc9kMNFpGmC\":{\"name\":\"Taninihun Mboka MCHP\"},\"a04CZxe0PSe\":{\"name\":\"Murray Town CHC\"},\"vcY0lzBz6fU\":{\"name\":\"Sandia CHP\"},\"qxbsDd9QYv6\":{\"name\":\"Yiffin CHC\"},\"wO4z5Aqo0hf\":{\"name\":\"Kamawornie CHP\"},\"cUNdCErxl9g\":{\"name\":\"Bayama (K. Teng) MCHP\"},\"AIM09vwxjoN\":{\"name\":\"Mosagbe MCHP\"},\"UUgajyaViT7\":{\"name\":\"Kambia CHP\"},\"TrmusBXxLm3\":{\"name\":\"Malenkie MCHP\"},\"qVvitxEF2ck\":{\"name\":\"Rogbere CHC\"},\"kpDoH80fwdX\":{\"name\":\"MCH Static\"},\"U2QkKSeyL5r\":{\"name\":\"Crossing MCHP\"},\"Fbq6Vxa4MIx\":{\"name\":\"Mano Njeigbla CHP\"},\"Gtnbmf4LkOz\":{\"name\":\"Motorbong MCHP\"},\"IFXdzAk7hKi\":{\"name\":\"Tokpombu MCHP\"},\"oUR5HPmim7E\":{\"name\":\"Sandayeima MCHP\"},\"wjFsUXI1MlO\":{\"name\":\"Iscon CHP\"},\"BXJnMD2eJAx\":{\"name\":\"Gbonkobana CHP\"},\"S2NaydvPENH\":{\"name\":\"Kundorma CHP\"},\"TAN6Q7vjvuk\":{\"name\":\"Gbo-Kakajama 1 MCHP\"},\"RTixJpRqS4C\":{\"name\":\"Kpetema CHP\"},\"SVEfwJ0BGeD\":{\"name\":\"Kono Bendu CHP\"},\"kBP1UvZpsNj\":{\"name\":\"Blessed Mokaba clinic\"},\"dqHvtpUqLwB\":{\"name\":\"Masory MCHP\"},\"PQEpIeuSTCN\":{\"name\":\"Tobanda CHC\"},\"qELjt3LRkSD\":{\"name\":\"Gberifeh MCHP\"},\"K5wBtEzE2qJ\":{\"name\":\"Gbamgbaia CHP\"},\"XGUOQaRUPjO\":{\"name\":\"Ngo Town CHP\"},\"hCm2Nh7C8BW\":{\"name\":\"Korgbotuma MCHP\"},\"cJkZLwhL8RP\":{\"name\":\"Kasse MCHP\"},\"zCSWBz2pyMd\":{\"name\":\"Wai MCHP\"},\"lCEeiuv4NaB\":{\"name\":\"Kholifaga MCHP\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"UCwtaCrNUls\":{\"name\":\"Heremakono MCHP\"},\"d9zRBAoM8OC\":{\"name\":\"Bumpeh Perri CHC\"},\"duGLGssecoD\":{\"name\":\"Kagbaneh CHP\"},\"rwfuVQHnZJ5\":{\"name\":\"Dodo Kortuma CHP\"},\"tWjUy6MCx8q\":{\"name\":\"Patama MCHP\"},\"qO2JLjYrg91\":{\"name\":\"Bandakarifaia MCHP\"},\"L4Tw4NlaMjn\":{\"name\":\"Nekabo CHC\"},\"GCbYmPqcOOP\":{\"name\":\"Romeni MCHP\"},\"X9zzzyPZViR\":{\"name\":\"Rochen Malal MCHP\"},\"MwfWgjMRgId\":{\"name\":\"Bumbukoro MCHP\"},\"g7BLyiBb0ET\":{\"name\":\"PMO Clinetown\"},\"GMOl74xzmAE\":{\"name\":\"Rotaimbana MCHP\"},\"wkYbuEwNWyf\":{\"name\":\"Sengema (Luawa) CHP\"},\"W7ekX3gi0ut\":{\"name\":\"Jaiama Sewafe CHC\"},\"tcEjL7gmFJL\":{\"name\":\"Senjekoro MCHP\"},\"fdsRQbuuAuh\":{\"name\":\"Nonkoba CHP\"},\"uedNhvYPMNu\":{\"name\":\"Gbanti CHC\"},\"r0TCGeLkQKI\":{\"name\":\"Maboni MCHP\"},\"wYLjA4vN6Y9\":{\"name\":\"Bambukoro MCHP\"},\"MrME31scKA1\":{\"name\":\"Kornia Kpindema CHP\"},\"CKJ9YS2AbWy\":{\"name\":\"Maharibo MCHP\"},\"JCXEtUDYyp9\":{\"name\":\"UFC Nixon Hospital\"},\"xKaB8tfbTzm\":{\"name\":\"Fintonia CHC\"},\"t52CJEyLhch\":{\"name\":\"Baoma MCHP\"},\"ltF8BmYAXpQ\":{\"name\":\"Maguama CHP\"},\"SFQigiC2ISS\":{\"name\":\"Madina (Malema) MCHP\"},\"cZtKKa9eJZ3\":{\"name\":\"Jenner Wright Clinic\"},\"qMbxFg9McOF\":{\"name\":\"Kawaya MCHP\"},\"YPSCWmJ3TyN\":{\"name\":\"Waiima MCHP\"},\"tXL6C7P0ObJ\":{\"name\":\"Kanekor MCHP\"},\"GhDwjKv07iC\":{\"name\":\"Kapethe MCHP\"},\"fCFdj2T0Bq1\":{\"name\":\"Mabang MCHP\"},\"jYPY8mT8gn6\":{\"name\":\"Kundundu MCHP\"},\"KbO0JnhiMwl\":{\"name\":\"Dibia MCHP\"},\"gowgzHWc8FT\":{\"name\":\"Manna MCHP\"},\"cXMQtUId06K\":{\"name\":\"Modonkor CHP\"},\"Ea3j0kUvyWg\":{\"name\":\"Stella Maries Clinic\"},\"DQHGtTGOP6b\":{\"name\":\"Bendu (Yawei) CHP\"},\"qusWt6sESRU\":{\"name\":\"Belentin MCHP\"},\"rJ25bHbIujw\":{\"name\":\"Susan's Bay MCHP\"},\"XtuhRhmbrJM\":{\"name\":\"Baiama CHP\"},\"mkIugjeYSjE\":{\"name\":\"Bumpeh River CHP\"},\"HlDMbDWUmTy\":{\"name\":\"Mapawn MCHP\"},\"Xk2fvz4aTBU\":{\"name\":\"Kabati CHP\"},\"B9RxRfRUi2R\":{\"name\":\"Ronietta MCHP\"},\"PB8FMGbn19r\":{\"name\":\"Bombohun MCHP\"},\"gaOSAjPM07w\":{\"name\":\"Mangay Loko MCHP\"},\"FGbXmz7gTTl\":{\"name\":\"SLC. RHC Port Loko\"},\"NMcx2jmra3c\":{\"name\":\"Firawa CHC\"},\"yDFM5J6WeKU\":{\"name\":\"Bengani MCHP\"},\"UGVLYrO63mR\":{\"name\":\"Bathurst MCHP\"},\"RQgXBKxgvHf\":{\"name\":\"Mapotolon CHC\"},\"BH7rDkWjUqc\":{\"name\":\"Bai Bureh Memorial Hospital\"},\"ndan8zClk4E\":{\"name\":\"Jenneh MCHP\"},\"fzBpuujglTY\":{\"name\":\"Swarray Town MCHP\"},\"g3O1pGAfgK1\":{\"name\":\"Motema CHP\"},\"I2UW55qvn82\":{\"name\":\"Rogballan MCHP\"},\"gsypzntLahf\":{\"name\":\"Lungi Govt. Hospital, Port Loko\"},\"PuZOFApTSeo\":{\"name\":\"Sahn CHC\"},\"ZZmMpGIE7pD\":{\"name\":\"Kagbanthama CHP\"},\"I48Qu6R0sGm\":{\"name\":\"Madina Loko CHP\"},\"vPKxHJ1og0r\":{\"name\":\"Giema (Luawa) MCHP\"},\"IN2dOk0gY1G\":{\"name\":\"Wullah Thenkle MCHP\"},\"ABM75Q1UfoP\":{\"name\":\"Bandajuma Kpolihun CHP\"},\"rs87nYgwbKv\":{\"name\":\"Lengekoro MCHP\"},\"wNYYRm2c9EK\":{\"name\":\"Red Cross Clinic\"},\"m5BX6CvJ6Ex\":{\"name\":\"Daru CHC\"},\"Bq5nb7UAEGd\":{\"name\":\"Sierra Rutile Clinic\"},\"zO5hgxxfU4T\":{\"name\":\"kamba mamudia\"},\"C1tAqIpKB9k\":{\"name\":\"Degbuama MCHP\"},\"YXdC9hjYPqQ\":{\"name\":\"Ninkikoro MCHP\"},\"HOgWkpYH3KB\":{\"name\":\"Gbongongor CHP\"},\"ZvX8lXd1tYs\":{\"name\":\"Govt. Hospital\"},\"oph70zH8JB2\":{\"name\":\"Robarie MCHP\"},\"f7yRhIeFn1k\":{\"name\":\"Jokibu MCHP\"},\"pYr0Kcy93M2\":{\"name\":\"Kangama (Kangama) CHP\"},\"l3jnkNNpoD8\":{\"name\":\"Masankoro MCHP\"},\"PybxeRWVSrI\":{\"name\":\"Mabolleh MCHP\"},\"Zp2Yi4j2AAH\":{\"name\":\"Katick MCHP\"},\"tlMeFk8C4CG\":{\"name\":\"Badala MCHP\"},\"uYG1rUdsJJi\":{\"name\":\"Borma (YKK) MCHP\"},\"k92yudERPlv\":{\"name\":\"Bayama MCHP\"},\"DxPNV7VHauJ\":{\"name\":\"Petifu Mayepoh MCHP\"},\"nE01sGNCY5P\":{\"name\":\"Kpandebu CHP\"},\"cWIiusmHULW\":{\"name\":\"Siama (U. Bamabara) MCHP\"},\"NqLYdlnK8sc\":{\"name\":\"Warrima MCHP\"},\"VXrJKs8hic4\":{\"name\":\"Bomie MCHP\"},\"uYTq3TEO2a9\":{\"name\":\"Charlotte CHP\"},\"eCfxBe1lnxb\":{\"name\":\"kamaron MCHP\"},\"U0KpeSx4UIB\":{\"name\":\"Macoth MCHP\"},\"M4hyYfnb21I\":{\"name\":\"Diamei MCHP\"},\"VF7LfO19vxS\":{\"name\":\"Kochero MCHP\"},\"nurO6U9bOLi\":{\"name\":\"Sindadu MCHP\"},\"ctMepV9p92I\":{\"name\":\"Gbangbalia MCHP\"},\"TSyzvBiovKh\":{\"name\":\"Gerehun CHC\"},\"Zbp8TbiMKVc\":{\"name\":\"Tabe MCHP\"},\"eoYV2p74eVz\":{\"name\":\"Approved School CHP\"},\"bPqP6eRfkyn\":{\"name\":\"Ross Road Health Centre\"},\"tlvNeDXXrS7\":{\"name\":\"Bassia MCHP\"},\"BTXwf2gl7av\":{\"name\":\"Pejewa MCHP\"},\"OI0BQUurVFS\":{\"name\":\"Bumban MCHP\"},\"OynYyQiFu82\":{\"name\":\"Kondeya MCHP\"},\"WZ8PTx8qQlE\":{\"name\":\"Saama MCHP\"},\"yJ1xkKha5oE\":{\"name\":\"Konia MCHP\"},\"duINhdt3Yay\":{\"name\":\"Gbengama MCHP\"},\"ADeZNq1pKsu\":{\"name\":\"Durukoro MCHP\"},\"G5FuODAbH6X\":{\"name\":\"Rofutha MCHP\"},\"hKD6hpZUh9v\":{\"name\":\"Faala CHP\"},\"rebbn0ooFSO\":{\"name\":\"Gbongeima MCHP\"},\"PC3Ag91n82e\":{\"name\":\"Mongere CHC\"},\"l0WRLZlEgB1\":{\"name\":\"Gbo-Kakajama 2 MCHP\"},\"aRXfvyonenP\":{\"name\":\"Sengema CHP\"},\"bHcw141PTsE\":{\"name\":\"Gbondapi CHC\"},\"F7u30K5OIpi\":{\"name\":\"Mamboma (Peje Bongre) CHP\"},\"BLVKubgVxkF\":{\"name\":\"SL Red Cross (Gbense) Clinic\"},\"v0HMlSxlH7l\":{\"name\":\"Maraka MCHP\"},\"IWM4eKPJJSc\":{\"name\":\"Sembehun MCHP\"},\"kFur7xPhpH9\":{\"name\":\"Teibor MCHP\"},\"VFF7f43dJv4\":{\"name\":\"Tombo Wallah CHP\"},\"ObJjzhhBkfy\":{\"name\":\"Gondama (Nimikoro) MCHP\"},\"TljiT6C5D0J\":{\"name\":\"Kpandebu CHC\"},\"w3mBVfrWhXl\":{\"name\":\"Mange CHC\"},\"w7a4l3XHIgi\":{\"name\":\"Gbaneh Bana MCHP\"},\"MErVkzdbsP5\":{\"name\":\"Yataya CHP\"},\"UugO8xDeLQD\":{\"name\":\"UNIMUS MCHP\"},\"w0QDch3dyPH\":{\"name\":\"Komneh CHP\"},\"Q23tMsKOoO6\":{\"name\":\"Fanima (Wonde) MCHP\"},\"cMFi8lYbXHY\":{\"name\":\"Bumpeh (Nimikoro) CHC\"},\"Rp268JB6Ne4\":{\"name\":\"Adonkia CHP\"},\"vAdMjyOspGL\":{\"name\":\"Grafton MCHP\"},\"c41XRVOYNJm\":{\"name\":\"Baiima CHP\"},\"Bift1B4gjru\":{\"name\":\"Marie Stopes (Gbense) Clinic\"},\"dyn5pihalrJ\":{\"name\":\"Victoria MCHP\"},\"Tc3zugEWdTm\":{\"name\":\"Sumbuya Bessima CHP\"},\"rspjJHg4WY1\":{\"name\":\"Bunabu MCHP\"},\"q5kAX5MyPB6\":{\"name\":\"Mabai (Kholifa Rowalla) MCHP\"},\"oLuhRyYPxRO\":{\"name\":\"Senehun CHC\"},\"bqtZrXoryDF\":{\"name\":\"Mercy Ship Hospital\"},\"aVlSMMvgVzf\":{\"name\":\"Bomu Saamba CHP\"},\"b1F5bfb7WUR\":{\"name\":\"MadaKa MCHP\"},\"Dbn6fyCgMBV\":{\"name\":\"Makama MCHP\"},\"Qu0QOykPdcD\":{\"name\":\"Mattru on the Rail MCHP\"},\"XXlzHWzhf5d\":{\"name\":\"Pepel CHP\"},\"is3w3HROKVc\":{\"name\":\"Rapha Clinic\"},\"ui12Hyvn6jR\":{\"name\":\"Wilberforce Military Hospital\"},\"VeXU3mndzri\":{\"name\":\"Magbengbeh MCHP\"},\"UOJlcpPnBat\":{\"name\":\"Needy CHC\"},\"dQggcljEImF\":{\"name\":\"Goderich Health Centre\"},\"RwkdG4Pku2x\":{\"name\":\"Tongorma MCHP\"},\"qqF8jshIs66\":{\"name\":\"Foya CHP\"},\"mc3jvzpzSi4\":{\"name\":\"Mabineh MCHP\"},\"EQnfnY03sRp\":{\"name\":\"Mandu CHP\"},\"ALZ2qr5u0X0\":{\"name\":\"Mamalikie MCHP\"},\"u1eQDDtKqm7\":{\"name\":\"Scan Drive MCHP\"},\"hDW65lFySeF\":{\"name\":\"Youndu CHP\"},\"DErmFP7bri7\":{\"name\":\"Dankawalie MCHP\"},\"rYIkxCJFtTX\":{\"name\":\"Feuror MCHP\"},\"BqRElDluXGa\":{\"name\":\"Levuma Nyomeh CHP\"},\"Q2USZSJmcNK\":{\"name\":\"Bumbuna CHC\"},\"sLKHXoBIqSs\":{\"name\":\"Njagbwema Fiama CHC\"},\"Bf9R1R91mw4\":{\"name\":\"Levuma CHP\"},\"UjusePB4jmP\":{\"name\":\"Kensay MCHP\"},\"qwmh84DV65K\":{\"name\":\"Kongoifeh MCHP\"},\"HPg74Rr7UWp\":{\"name\":\"School Health Clinic\"},\"dtuiqEXYa7z\":{\"name\":\"Rokel MCHP\"},\"mEUUK7MHLSF\":{\"name\":\"Magburaka Govt. Hospital\"},\"sTOXJA2KcY2\":{\"name\":\"Musaia CHP\"},\"E7IDb3nNiW7\":{\"name\":\"Makump Bana MCHP\"},\"jr5hIZcJBXB\":{\"name\":\"Kowama MCHP\"},\"uFp0ztDOFbI\":{\"name\":\"Bendu CHC\"},\"MUnd4KWox8m\":{\"name\":\"Njagbwema CHP\"},\"aHs9PLxIdbr\":{\"name\":\"Mayepoh CHC\"},\"yEU926iVAJJ\":{\"name\":\"Kathombo MCHP\"},\"ShdRyzuLKA2\":{\"name\":\"Majihun MCHP\"},\"WdgS1JcBL2g\":{\"name\":\"Mongerewa MCHP\"},\"LnToY3ExKxL\":{\"name\":\"Mahera CHC\"},\"VdXuxcNkiad\":{\"name\":\"Yoyema MCHP\"},\"QZ5rmKrVleg\":{\"name\":\"Yenkissa MCHP\"},\"jhtj3eQa1pM\":{\"name\":\"Gondama (Tikonko) CHC\"},\"ZsjXrmZS59z\":{\"name\":\"Ndegbome MCHP\"},\"XctPvvWIIcF\":{\"name\":\"Yargoi MCHP\"},\"eP4F9eB76B0\":{\"name\":\"Mamankie MCHP\"},\"SC0nM3cbGHy\":{\"name\":\"Mokobo MCHP\"},\"e0RGds86ow6\":{\"name\":\"Fobu MCHP\"},\"GRc9WXp9gSy\":{\"name\":\"Bradford CHC\"},\"TQ5DSmdliN7\":{\"name\":\"Baoma (Luawa) MCHP\"},\"DwEfz1MN7Z5\":{\"name\":\"Ngiegboiya MCHP\"},\"Pw9SihGDbZ5\":{\"name\":\"Gbogbodo MCHP\"},\"zQpYVEyAM2t\":{\"name\":\"Hastings Health Centre\"},\"wicmjKI3xiP\":{\"name\":\"SLRCS MCH Clinic\"},\"tEgxbwwrwUd\":{\"name\":\"Kayongoro MCHP\"},\"sgcHQEaB40Y\":{\"name\":\"Yalieboya CHP\"},\"lxxASQqPUqd\":{\"name\":\"Tombodu CHC\"},\"oRncQGhLYNE\":{\"name\":\"Regent (RWA) CHC\"},\"EUUkKEDoNsf\":{\"name\":\"Wilberforce CHC\"},\"TkhwySsXC5V\":{\"name\":\"Konta Wallah MCHP\"},\"T3iVyvrCpZ0\":{\"name\":\"Kagbasia MCHP\"},\"g031LbUPMmh\":{\"name\":\"Yeben MCHP\"},\"uDzWmUDHKeR\":{\"name\":\"Futa CHC\"},\"a5glgtnXJRG\":{\"name\":\"Magbanabom MCHP\"},\"wGsBlwh6Zzt\":{\"name\":\"Mogbuama MCHP\"},\"flQBQV8eyHc\":{\"name\":\"Dankawalia MCHP\"},\"ZdPkczYqeIY\":{\"name\":\"Gandorhun CHC\"},\"RpRJUDOPtt7\":{\"name\":\"Dandabu CHP\"},\"CTOMXJg41hz\":{\"name\":\"Kaniya MCHP\"},\"s7SLtx8wmRA\":{\"name\":\"Kenema Gbandoma MCHP\"},\"p310xqwAJge\":{\"name\":\"Kondembaia CHC\"},\"PwoQgMJNWbR\":{\"name\":\"Koardu MCHP\"},\"WoqN1oUBX2R\":{\"name\":\"Mile 38 CHP\"},\"XJ6DqDkMlPv\":{\"name\":\"Wesleyan Health Clinic\"},\"Rll4VmTDRiE\":{\"name\":\"Bai Largo MCHP\"},\"GjWQK6UA4FO\":{\"name\":\"Kambawama MCHP\"},\"bJ0VSATHwO2\":{\"name\":\"Kumrabai Yoni MCHP\"},\"u6ZGNI8yUmt\":{\"name\":\"Rina Clinic\"},\"tZxqVn3xNrA\":{\"name\":\"Wallehun MCHP\"},\"x3ti3t9eOuX\":{\"name\":\"Rosengbeh MCHP\"},\"er9S4CQ9QOn\":{\"name\":\"Bendu (Kowa) MCHP\"},\"nq7F0t1Pz6t\":{\"name\":\"Arab Clinic\"},\"XmfqaErvQ2T\":{\"name\":\"Mosenessie Junction MCHP\"},\"ZxuSbAmsLCn\":{\"name\":\"Kamasikie MCHP\"},\"NqwvaQC1ni4\":{\"name\":\"Komendeh (Nongowa) MCHP\"},\"xuk02oLk12O\":{\"name\":\"Lungi UFC\"},\"k6lOze3vTzP\":{\"name\":\"Potoru CHC\"},\"p9KfD6eaRvu\":{\"name\":\"Shenge CHC\"},\"K3jhn3TXF3a\":{\"name\":\"Tongo Field CHC\"},\"HNv1aLPdMYb\":{\"name\":\"Kamalo CHC\"},\"pJv8NJlJNhU\":{\"name\":\"Pendembu CHC\"},\"sK498nBOLfQ\":{\"name\":\"Elshadai MCHP\"},\"KvE0PYQzXMM\":{\"name\":\"Mano Yorgbo MCHP\"},\"rpAgG9XCWhO\":{\"name\":\"Bureh MCHP\"},\"YTQRSW91PxO\":{\"name\":\"Falaba MCHP\"},\"xX4lIVqF4yb\":{\"name\":\"Senekedugu MCHP\"},\"YAuJ3fyoEuI\":{\"name\":\"Gbendembu Wesleyan CHC\"},\"jKZ0U8Og5aV\":{\"name\":\"Dodo CHC\"},\"kIbcKauMdlW\":{\"name\":\"Kunya MCHP\"},\"n9HIySyR00g\":{\"name\":\"Magbil MCHP\"},\"Umh4HKqqFp6\":{\"name\":\"Jembe CHC\"},\"IpA5FViU8tk\":{\"name\":\"Kanga MCHP\"},\"ubsjwFFBaJM\":{\"name\":\"Gbangbatoke CHC\"},\"lBob31rp6l4\":{\"name\":\"Mabom CHP\"},\"sIVFEyNfOg4\":{\"name\":\"Mokandor CHP\"},\"ARAZtL7Bdpy\":{\"name\":\"Guala MCHP\"},\"iMZihUMzH92\":{\"name\":\"Bauya (Kongbora) CHC\"},\"dGZbEZroAWr\":{\"name\":\"Funyehun MCHP\"},\"N7mHLD3ljYc\":{\"name\":\"Kambia GH\"},\"fmkqsEx6MRo\":{\"name\":\"Mabora MCHP\"},\"FFU3PJ3pY7s\":{\"name\":\"Malal MCHP\"},\"oIgBLlEo6eH\":{\"name\":\"Deima MCHP\"},\"CY8cV5khn7e\":{\"name\":\"Maselleh MCHP\"},\"GM9ddjXIO5b\":{\"name\":\"Koindu-kuntey MCHP\"},\"kuqKh33SPgg\":{\"name\":\"Falaba CHC\"},\"M721NHGtdZV\":{\"name\":\"St. Mary's Clinic\"},\"LzvoPaeLPsb\":{\"name\":\"Maron MCHP\"},\"EQc3n1juPFn\":{\"name\":\"Koakor MCHP\"},\"fAsj6a4nudH\":{\"name\":\"Yoni CHC\"},\"Mi4dWRtfIOC\":{\"name\":\"Sandaru CHC\"},\"Srnpwq8jKbp\":{\"name\":\"Mawoma MCHP\"},\"ei21lW7hFPX\":{\"name\":\"Gbaama MCHP\"},\"DXegteybeb5\":{\"name\":\"Warima MCHP\"},\"mzsOsz0NwNY\":{\"name\":\"New Police Barracks CHC\"},\"TYq1YW7qs7k\":{\"name\":\"Gbo-Lambayama 2 MCHP\"},\"BXd3TqaAxkK\":{\"name\":\"Sahun (Bumpeh) MCHP\"},\"pdF4XIHIGPx\":{\"name\":\"Bangoma MCHP\"},\"wByqtWCCuDJ\":{\"name\":\"Damballa CHC\"},\"cNAp6CJeLxk\":{\"name\":\"Mokanji CHC\"},\"GAvxcmr5jB1\":{\"name\":\"Gao MCHP\"},\"n7wN9gMFfZ5\":{\"name\":\"Benduma CHC\"},\"n3MRjKtwr3O\":{\"name\":\"Kagbulor CHP\"},\"iIQENGb7za6\":{\"name\":\"Sandia (Kissi Tongi) CHP\"},\"wbtk73Zwhj9\":{\"name\":\"Bumpeh CHP\"},\"mW20aiZHqwE\":{\"name\":\"Tassoh MCHP\"},\"LUGqPutql0P\":{\"name\":\"Fonikor CHP\"},\"Q8oWscr9rlQ\":{\"name\":\"Kiampkakolo MCHP\"},\"TJA0eGRoRpc\":{\"name\":\"Upper Komende MCHP\"},\"nDwbwJZQUYU\":{\"name\":\"Kanikay MCHP\"},\"Zf2v0kbI7ah\":{\"name\":\"Makonkorie MCHP\"},\"D7UVRRE9iUC\":{\"name\":\"Gbentu CHP\"},\"fGp4OcovQpa\":{\"name\":\"Fogbo CHP\"},\"OTn9VMNEkdo\":{\"name\":\"Mathufulie MCHP\"},\"BpWJ3cRsO6g\":{\"name\":\"Motonkoh MCHP\"},\"inpc5QsFRTm\":{\"name\":\"Kamassasa CHC\"},\"t66taqSF1mW\":{\"name\":\"Follah MCHP\"},\"djMCTPYvltl\":{\"name\":\"Govt. Hosp. Kenema\"},\"L3GgannGGKl\":{\"name\":\"Mafufuneh MCHP\"},\"YnuwSqXPx9H\":{\"name\":\"Ngaiya MCHP\"},\"GGDHb8xd8jc\":{\"name\":\"Tokeh MCHP\"},\"aSxNNRxPuBP\":{\"name\":\"Kalangba CHC\"},\"Q0HywoaWOcM\":{\"name\":\"Sukudu MCHP\"},\"O1KFJmM6HUx\":{\"name\":\"Mano Gbonjeima CHC\"},\"s5aXfzOL456\":{\"name\":\"Talia CHC\"},\"kSo9KSpHUPL\":{\"name\":\"Makaba MCHP\"},\"DZaJmtlaBMl\":{\"name\":\"Mogbasske CHP\"},\"vyIl6s0lhKc\":{\"name\":\"Barmoi Luma MCHP\"},\"F9zWBqG5Pmi\":{\"name\":\"kamaron CHP\"},\"vQYIk5G9NxP\":{\"name\":\"Mayossoh MCHP\"},\"ZW3XCXXiLcO\":{\"name\":\"Minah MCHP\"},\"LWlh25dfvEA\":{\"name\":\"Makundu MCHP\"},\"tSBcgrTDdB8\":{\"name\":\"Paramedical CHC\"},\"KiheEgvUZ0i\":{\"name\":\"Calaba town CHC\"},\"cTU2WmWcJKx\":{\"name\":\"Magbaikoli MCHP\"},\"sesv0eXljBq\":{\"name\":\"Yele CHC\"},\"u0SlCNJnK3K\":{\"name\":\"Mamaka (Yoni) MCHP\"},\"fNL2oehab2Q\":{\"name\":\"Samaia MCHP\"},\"kDxbU1uSBFh\":{\"name\":\"Kpetema MCHP\"},\"NLN0MvWv9tl\":{\"name\":\"Grima CHP\"},\"RXeDDKU26rB\":{\"name\":\"Gbaa (Makpele) CHP\"},\"bKiJzk8ZZbS\":{\"name\":\"Kuntorloh CHP\"},\"SlNw6FxElY9\":{\"name\":\"Mendekelema (Upper Banbara) CHP\"},\"IlnqGuxfQAw\":{\"name\":\"Sinkunia CHC\"},\"lpQvlm9czYE\":{\"name\":\"Tungie CHC\"},\"aSnKB1sWaz4\":{\"name\":\"Ngolahun Jabaty MCHP\"},\"zQ2pFkzGtIg\":{\"name\":\"Peya MCHP\"},\"HAqUY00X9N5\":{\"name\":\"Goderich MI Room\"},\"KKoPh1lDd9j\":{\"name\":\"Kainkordu CHC\"},\"fRV3Fhz1IP8\":{\"name\":\"Gondama MCHP\"},\"AFi1GjbeejL\":{\"name\":\"Kanga (LB) MCHP\"},\"esMAQ4vs4kM\":{\"name\":\"Yiraia CHP\"},\"TEVtOFKcLAP\":{\"name\":\"Gbap CHC\"},\"pXDcgDRz8Od\":{\"name\":\"Songo CHC\"},\"M9JyYBZTqR7\":{\"name\":\"Kukuna CHP\"},\"waNtxFbPjrI\":{\"name\":\"Blessed Mokaka East Clinic\"},\"dCvUVvKnhMe\":{\"name\":\"Malema 1 MCHP\"},\"OqBiNJjKQAu\":{\"name\":\"Kasongha MCHP\"},\"F0uVXCVvOPO\":{\"name\":\"Malone MCHP\"},\"NaVzm59XKGf\":{\"name\":\"Gbinti CHC\"},\"Ahh47q8AkId\":{\"name\":\"Mabang CHC\"},\"SHLY5rkOFTQ\":{\"name\":\"UFC Port Loko\"},\"oDAoqMWcsJQ\":{\"name\":\"Kaliyereh MCHP\"},\"DF76ZjQtFSg\":{\"name\":\"Kamba Mamudia MCHP\"},\"cDRQOxX1wHO\":{\"name\":\"Mosanda CHP\"},\"vSbt6cezomG\":{\"name\":\"UMC (Urban Centre) Hospital\"},\"dWOAzMcK2Wt\":{\"name\":\"Alkalia CHP\"},\"qIpBLa1SCZt\":{\"name\":\"Talia (Nongowa) CHC\"},\"xWjiTeok0Sr\":{\"name\":\"Masorie CHP\"},\"z4silfLpw2G\":{\"name\":\"Mafoimara MCHP\"},\"H97XE5Ea089\":{\"name\":\"Bomotoke CHC\"},\"bkMlhoccaVw\":{\"name\":\"Mabureh Mende MCHP\"},\"kEkU53NrFmy\":{\"name\":\"Taninahun (BN) CHP\"},\"YvwYw7GilkP\":{\"name\":\"Levuma (Kandu Lep) CHC\"},\"RaQGHRti7JM\":{\"name\":\"Gods Favour health Center\"},\"xMn4Wki9doK\":{\"name\":\"Moriba Town CHC\"},\"iMDr2FG7i8Q\":{\"name\":\"Makobeh MCHP\"},\"dmdYffw2I0F\":{\"name\":\"Makeni-Lol MCHP\"},\"JQJjsXvHE5M\":{\"name\":\"Mokelleh CHC\"},\"RJpiHpefEUw\":{\"name\":\"Torkpumbu MCHP\"},\"h9q3qixffZT\":{\"name\":\"Campbell Town  CHP\"},\"kvzdkXBxHoN\":{\"name\":\"Njaluahun CHP\"},\"zY9ds4oNZxw\":{\"name\":\"Potehun MCHP\"},\"FRX63UWciyO\":{\"name\":\"Mamusa MCHP\"},\"kd2Aqw5S07V\":{\"name\":\"Mokassie MCHP\"},\"pMEnu7BjqMz\":{\"name\":\"Kpumbu MCHP\"},\"kbGqmM6ZWWV\":{\"name\":\"Allen Town Health Post\"},\"MMrdfNDfBIi\":{\"name\":\"Gbongeh CHP\"},\"nbMpoRiVRWd\":{\"name\":\"Pewama CHP\"},\"Pr2stbkaSX3\":{\"name\":\"Fayeima CHP\"},\"hpXXBtRXXSd\":{\"name\":\"Kaimunday CHP\"},\"bf6PXrSNMKK\":{\"name\":\"Magbassabana MCHP\"},\"FQ5CCuUKNLf\":{\"name\":\"Romando MCHP\"},\"fUxVOkpX3yi\":{\"name\":\"Manack MCHP\"},\"PMsF64R6OJX\":{\"name\":\"Bendugu (Mongo) CHC\"},\"v0dXACseLuB\":{\"name\":\"Woroma CHP\"},\"ih77LC7LE1p\":{\"name\":\"Morfindor CHP\"},\"bPHn9IgjKLC\":{\"name\":\"Nasarah Clinic\"},\"HHc5HDPFlXy\":{\"name\":\"Menika MCHP\"},\"hLGkoHmvBgI\":{\"name\":\"Mano-Jaiama CHP\"},\"roQ2l7TX0eZ\":{\"name\":\"SLRCS (Bo) Clinic\"},\"TNbHYOuQi8s\":{\"name\":\"Bambawolo CHP\"},\"JrSIoCOdTH2\":{\"name\":\"Tombo CHC\"},\"a1E6QWBTEwX\":{\"name\":\"Sienga CHP\"},\"nCh5dBoJVNw\":{\"name\":\"SL Red Cross (BMC) Clinic\"},\"gmen7SXL9CU\":{\"name\":\"SLIMS Clinic\"},\"cJ7omISg7gG\":{\"name\":\"Kamba MCHP\"},\"rwgK8TkRwHl\":{\"name\":\"Kawengha MCHP\"},\"dNT8lAL4zGo\":{\"name\":\"Nyeama CHP\"},\"gGv9ATEs68L\":{\"name\":\"UFC Bonthe\"},\"hZpaU5uFSDm\":{\"name\":\"Magbele MCHP\"},\"KFowGOhmuSL\":{\"name\":\"Suga MCHP\"},\"MPUiud3BYRq\":{\"name\":\"Katherie MCHP\"},\"c9wCIfbcyVo\":{\"name\":\"M I Room (Military)\"},\"ua3kNk4uraZ\":{\"name\":\"Modia MCHP\"},\"NnQpISrLYWZ\":{\"name\":\"Govt. Hosp. Bonthe\"},\"lf7FRlrchg3\":{\"name\":\"Gofor CHP\"},\"yh1PrRTboyg\":{\"name\":\"Kassama MCHP\"},\"QN4te5Z5svQ\":{\"name\":\"Mbaoma CHP\"},\"FLjwMPWLrL2\":{\"name\":\"Baomahun CHC\"},\"suFG8zx4bU3\":{\"name\":\"Masabong Pil MCHP\"},\"Gba5bTc8NIg\":{\"name\":\"Serabu (Small Bo) CHP\"},\"uAk40nFigUK\":{\"name\":\"Magbenka CHP\"},\"lL2LBkhlsmV\":{\"name\":\"Grassfield CHC\"},\"nYiOoF2nXIr\":{\"name\":\"Kalangba BKM MCHP\"},\"erqWTArTsyJ\":{\"name\":\"Telu CHP\"},\"NRPCjDljVtu\":{\"name\":\"Lakka\\/Ogoo Farm CHC\"},\"pJj2r2HElLE\":{\"name\":\"Madina Fullah CHP\"},\"egjrZ1PHNtT\":{\"name\":\"Sembehun CHC\"},\"up9gjdODKXE\":{\"name\":\"Vaahun MCHP\"},\"lzz1UhTzO4E\":{\"name\":\"New Maforkie CHP\"},\"X79FDd4EAgo\":{\"name\":\"Rokulan CHC\"},\"E4jn4059Y1x\":{\"name\":\"Gondama (Kamaje) CHP\"},\"NqTZjfTIsxC\":{\"name\":\"Semewebu MCHP\"},\"wfGRNqXqf92\":{\"name\":\"Kabonka MCHP\"},\"PHo0IV7Vk50\":{\"name\":\"Rochem Kamandao CHP\"},\"O63vIA5MVn6\":{\"name\":\"Tagrin CHC\"},\"MnfykVk3zin\":{\"name\":\"Senjehun MCHP\"},\"KwSj4DlRWAm\":{\"name\":\"Makoba Bana MCHP\"},\"zsqxu7ZZRpO\":{\"name\":\"Koeyor MCHP\"},\"VZ6Cocesljy\":{\"name\":\"Quidadu MCHP\"},\"N3tpEjZcPm9\":{\"name\":\"Laleihun Kovoma CHC\"},\"Qc9lf4VM9bD\":{\"name\":\"Wellington Health Centre\"},\"KnU2XHRvyiX\":{\"name\":\"Kamakwie MCHP\"},\"P4upLKrpkHP\":{\"name\":\"Ngegbwema CHC\"},\"rozv5QUSE7a\":{\"name\":\"Lowoma MCHP\"},\"AtZJOoQiGHd\":{\"name\":\"Woama MCHP\"},\"xXYv82KlBUh\":{\"name\":\"Quarry MCHP\"},\"VTtyiYcc6TE\":{\"name\":\"Roktolon MCHP\"},\"wjP03y8OY5k\":{\"name\":\"Serabu (Koya) CHP\"},\"GQpxsB7tekR\":{\"name\":\"Mogomgbay MCHP\"},\"VfZnZ6UKyn8\":{\"name\":\"Bontiwo MCHP\"},\"MBtmOhLs7y1\":{\"name\":\"Sengama MCHP\"},\"XzmWizbR343\":{\"name\":\"Masuba MCHP\"},\"UxpUYgdb4oU\":{\"name\":\"Rokai CHP\"},\"GHHvGp7tgtZ\":{\"name\":\"Binkolo CHC\"},\"wqbyzbQ78oI\":{\"name\":\"Suen CHP\"},\"Qwzs1iinAI7\":{\"name\":\"Wonkibor MCHP\"},\"wwM3YPvBKu2\":{\"name\":\"Ngolahun CHC\"},\"wzvDhS0TkAF\":{\"name\":\"Pate Bana CHP\"},\"lsqa3EEGHxv\":{\"name\":\"Bunumbu CHP\"},\"nImgPWDVQIa\":{\"name\":\"Mbokie CHP\"},\"jj1MhWhHqta\":{\"name\":\"Kasanikoro MCHP\"},\"AvGz949akv4\":{\"name\":\"Saiama MCHP\"},\"x8SUTSsJoeO\":{\"name\":\"Baoma-Peje CHP\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"},\"xRsoZIRmnt4\":{\"name\":\"Mabai MCHP\"},\"pUZIL5xBsve\":{\"name\":\"Sumbuya MCHP\"},\"OzVuFaZgm5U\":{\"name\":\"Gbo-Lambayama 1 MCHP\"},\"k6DIO9LIEk9\":{\"name\":\"Lyn Maternity MCHP\"},\"XbyObqerCya\":{\"name\":\"Yabaima CHP\"},\"jbfISeV6Wdu\":{\"name\":\"Makeni-Rokfullah MCHP\"},\"mwN7QuEfT8m\":{\"name\":\"Koribondo CHC\"},\"Hu31NCRjZlj\":{\"name\":\"Masamboi MCHP\"},\"nDoybVJLD74\":{\"name\":\"Gbalahun CHP\"},\"aV9VVijeVB2\":{\"name\":\"Njagbwema MCHP\"},\"MuZJ8lprGqK\":{\"name\":\"Moyamba Junction CHC\"},\"YWXXO0XMkQe\":{\"name\":\"Mendewa MCHP\"},\"Jyv7sjpl9bA\":{\"name\":\"Sendumei CHC\"},\"BnVjTzwis3o\":{\"name\":\"Samaya CHP\"},\"qzm5ww3U0vz\":{\"name\":\"Jangalor MCHP\"},\"PSjKMcPGUvA\":{\"name\":\"Kangama CHP\"},\"R0CmUlFULXg\":{\"name\":\"Mayakie MCHP\"},\"EDxXfB4iVpY\":{\"name\":\"Wilberforce MCHP\"},\"zEsMdeJOty4\":{\"name\":\"Moyiba CHC\"},\"PA1spYiNZfv\":{\"name\":\"Yengema CHC\"},\"hMBotMwWnU1\":{\"name\":\"Koinadugu II CHP\"},\"mVvEwzoFutG\":{\"name\":\"Nyandehun MCHP\"},\"WjO2puYKysP\":{\"name\":\"Sonkoya MCHP\"},\"zm9breCeT1m\":{\"name\":\"Gbonkoh Kareneh MCHP\"},\"KFhJrkqnrnb\":{\"name\":\"Mathamp MCHP\"},\"sSgOnY1Xqd9\":{\"name\":\"Delken MCHP\"},\"jfV49JGnYKF\":{\"name\":\"Fatibra CHP\"},\"z9KGMrElTYS\":{\"name\":\"Fullah Town (M.Gbanti) MCHP\"},\"rLaGvUnv2BF\":{\"name\":\"Mathonkara MCHP\"},\"xIMxph4NMP1\":{\"name\":\"Tonkomba MCHP\"},\"tNs4E0JcMKe\":{\"name\":\"Sawula MCHP\"},\"WOk7efLlLSj\":{\"name\":\"Niagorehun MCHP\"},\"Eyj2kiEJ7M3\":{\"name\":\"Bailor CHP\"},\"SIxGTeya5lN\":{\"name\":\"Mapillah MCHP\"},\"NnGUNkc5Zq8\":{\"name\":\"Mende Buima MCHP\"},\"Uwcj0mz78BV\":{\"name\":\"Manjoro MCHP\"},\"JQr6TJx5KE3\":{\"name\":\"Nyandehun CHP\"},\"UqHuR4IYvTY\":{\"name\":\"Sanya CHP\"},\"kO9xe2HCovK\":{\"name\":\"Kambia Makama CHP\"},\"RhJbg8UD75Q\":{\"name\":\"Yemoh Town CHC\"},\"dU3vTbLRLHy\":{\"name\":\"Tambeyama MCHP\"},\"Pae8DR7VmcL\":{\"name\":\"MCH (Kakua) Static\"},\"FbD5Z8z22Yb\":{\"name\":\"Geoma Jagor CHC\"},\"ym42ZOlfZ1P\":{\"name\":\"Robaka MCHP\"},\"ua5GXy2uhBR\":{\"name\":\"Tihun CHC\"},\"WT6JLfyR9lL\":{\"name\":\"Fanima CHP\"},\"MiYhwDprCCA\":{\"name\":\"Mabella MCHP\"},\"aF6iPGbrcRk\":{\"name\":\"Bandasuma Fiama MCHP\"},\"sY1WN6LjmAx\":{\"name\":\"Moyowa MCHP\"},\"brnL0W3Fbsj\":{\"name\":\"Koya MCHP\"},\"oV9P0VvL9Jh\":{\"name\":\"Plantain Island MCHP\"},\"WxMIZC6Cxqs\":{\"name\":\"Magbengberah MCHP\"},\"amgb83zVxp5\":{\"name\":\"Bendu Mameima CHC\"},\"KaevAHPgkA8\":{\"name\":\"Rothatha MCHP\"},\"U02o1QAm6cC\":{\"name\":\"Kpetema (Lower Bambara) CHP\"},\"FwKJ7gYEv8U\":{\"name\":\"Ngelehun MCHP\"},\"nornKUJmQqn\":{\"name\":\"Konta-Line MCHP\"},\"PhR1PdMTzhW\":{\"name\":\"Masongbo Limba MCHP\"},\"kedYKTsv95j\":{\"name\":\"Gbado MCHP\"},\"lELJZCBxz7H\":{\"name\":\"Kent CHP\"},\"cd3U2Tp0qR2\":{\"name\":\"Makonthanday MCHP\"},\"ZKL5hlVG6F6\":{\"name\":\"Benguima Grassfield MCHP\"},\"cUltUneFSan\":{\"name\":\"Komrabai Station MCHP\"},\"EQUwHqZOb5L\":{\"name\":\"Kabaima MCHP\"},\"xXhKbgwL39t\":{\"name\":\"Blama Massaquoi CHP\"},\"bG0PlyD0iP3\":{\"name\":\"Tugbebu CHP\"},\"m0XorV4WWg0\":{\"name\":\"Ginger Hall Health Centre\"},\"lyONqUkY1Bq\":{\"name\":\"Matholey MCHP\"},\"CvBAqD6RzLZ\":{\"name\":\"Ngalu CHC\"},\"yg7uxUol97F\":{\"name\":\"Laiya CHP\"},\"HFyjUvMjQ8H\":{\"name\":\"Baiwala CHP\"},\"SmhR2aaKLjw\":{\"name\":\"sonkoya MCHP\"},\"RpjUEvgWSNO\":{\"name\":\"Dulukoro MCHP\"},\"BJMWTGwuGiw\":{\"name\":\"Niahun Gboyama MCHP\"},\"m8qnxndRDR6\":{\"name\":\"Lumpa CHP\"},\"tHUYjt9cU6h\":{\"name\":\"Ola During Clinic\"},\"yets9NmUcRS\":{\"name\":\"Deep Eye water MCHP\"},\"uNEhNuBUr0i\":{\"name\":\"Tonko Maternity Clinic\"},\"ETRqfu74kge\":{\"name\":\"Masaika MCHP\"},\"AhnK8hb3JWm\":{\"name\":\"The White House Clinic\"},\"d9uZeZ5fMUo\":{\"name\":\"Mamaka MCHP\"},\"jIrb5XckcU6\":{\"name\":\"Masseseh MCHP\"},\"wy6tbexg2nu\":{\"name\":\"Tawuya CHP\"},\"cBi3y4lGhDd\":{\"name\":\"Gbomsamba MCHP\"},\"Kmu7ox2MiiU\":{\"name\":\"Koyagbema MCHP\"},\"mGmu0GJ5neg\":{\"name\":\"Kpetewoma CHP\"},\"rx9ubw0UCqj\":{\"name\":\"Bandajuma MCHP\"},\"k8ZPul89UDm\":{\"name\":\"Kayima CHC\"},\"lQIe6vtSe1P\":{\"name\":\"Gbangadu MCHP\"},\"CSDGDOa7wHd\":{\"name\":\"Kornia MCHP\"},\"e4P2zTzM7gQ\":{\"name\":\"Kamasaypana MCHP\"},\"OzjRQLn3G24\":{\"name\":\"Koidu Govt. Hospital\"},\"HHz1kAG1LKn\":{\"name\":\"Mosenegor MCHP\"},\"x5ZxMDvEQUb\":{\"name\":\"Yonibana MCHP\"},\"EihevoTWn2i\":{\"name\":\"Gbamani CHP\"},\"gP6hn503KUX\":{\"name\":\"Robat MCHP\"},\"bW5BaqrBM4K\":{\"name\":\"Wordu CHP\"},\"GkHpMSo5K60\":{\"name\":\"Fothaneh Bana MCHP\"},\"PD1fqyvJssC\":{\"name\":\"Catholic Clinic\"},\"NjyJYiIuKIG\":{\"name\":\"Kathanta Yimbor CHC\"},\"KGN2jvZ0GJy\":{\"name\":\"Kantia CHP\"},\"taKiTcaf05H\":{\"name\":\"Mabenteh Community Hospital\"},\"hHKKi9WNoBG\":{\"name\":\"Kamiendor MCHP\"},\"b09gf2vvZDb\":{\"name\":\"Mabureh CHP\"},\"VSwnkMSAdp7\":{\"name\":\"Worreh MCHP\"},\"caif2tNAS0n\":{\"name\":\"Mathinkalol MCHP\"},\"el8sgzyHuEe\":{\"name\":\"Rosint Buya MCHP\"},\"xATvj8pdYoT\":{\"name\":\"Grima Jou MCHP\"},\"Vw4Uv6UPIPC\":{\"name\":\"Ngueh MCHP\"},\"g6y7PS0UQR4\":{\"name\":\"Sandialu MCHP\"},\"bM4Ky73uMao\":{\"name\":\"Kpolies Clinic\"},\"t1aAdpBbDB3\":{\"name\":\"Bandusuma MCHP\"},\"hIpcmjLrDDW\":{\"name\":\"London (Blama) MCHP\"},\"QzPf0qKBU4n\":{\"name\":\"Jendema CHC\"},\"QBRQnWPRO3V\":{\"name\":\"Madopolahun MCHP\"},\"as1dnmlXLzG\":{\"name\":\"Gbetema MCHP (Fiama)\"},\"vELbGdEphPd\":{\"name\":\"Jimmi CHC\"},\"n2qFnUIhbq3\":{\"name\":\"Rosinor CHP\"},\"NDqR2cWlVy3\":{\"name\":\"Sahn Bumpe MCHP\"},\"va2lE4FiVVb\":{\"name\":\"Mano CHC\"},\"JNJIPX9DfaW\":{\"name\":\"S.L.R.C.S Clinic\"},\"mhJQYk2Jwym\":{\"name\":\"Konabu MCHP\"},\"UqXSUMp19FB\":{\"name\":\"Kalangba MCHP\"},\"K0d08d3sUOv\":{\"name\":\"Lakka Hospital\"},\"BJ3DJFBKwBR\":{\"name\":\"Saama (Lower Bamabara) CHP\"},\"pmzk0ho80aA\":{\"name\":\"Kathanta Bana MCHP\"},\"cag6vQQ9SQk\":{\"name\":\"Masselleh MCHP\"},\"RNGpZqutw3Y\":{\"name\":\"Kania (Masungbala) MCHP\"},\"I2DzylqJa2i\":{\"name\":\"Samai Town MCHP\"},\"2021\":{\"name\":\"2021\"},\"LZclRdyVk1t\":{\"name\":\"Bumbanday MCHP\"},\"SKJoPDgjELa\":{\"name\":\"Samamaia MCHP\"},\"kUzpbgPCwVA\":{\"name\":\"Blama CHC\"},\"PFZbQjwty2n\":{\"name\":\"Kpandebu MCHP\"},\"FupvWBUFXr7\":{\"name\":\"MacDonald MCHP\"},\"Mi4Ax9suQmB\":{\"name\":\"Sembehun (Gaura) MCHP\"},\"PysJIi3VIol\":{\"name\":\"Juba M I Room\"},\"rCKWdLr4B8K\":{\"name\":\"Motuo CHC\"},\"lwHs72tP6Kh\":{\"name\":\"Kordebotehun MCHP\"},\"DINXUs8QZWg\":{\"name\":\"Nyandehun (Koya) MCHP\"},\"Sglj9VCoQmc\":{\"name\":\"Kagboray MCHP\"},\"m73lWmo5BDG\":{\"name\":\"Korbu MCHP\"},\"qAFXoNjlZCB\":{\"name\":\"Vaama  (kpanga krim) MCHP\"},\"fYmE4ymzZSe\":{\"name\":\"Pendembu Njeigbla MCHP\"},\"ctfiYW0ePJ8\":{\"name\":\"Philip Street Clinic\"},\"svCLFkT99Yx\":{\"name\":\"Timbo CHP\"},\"cZI3AWM7bIa\":{\"name\":\"Minthomor CHP\"},\"EoIjKXqXxi2\":{\"name\":\"Sukudu Soa MCHP\"},\"ewh5SKxcCAl\":{\"name\":\"Makaiba MCHP\"},\"plnHVbJR6p4\":{\"name\":\"Ahamadyya Mission Cl\"},\"GA7eQkgK5mX\":{\"name\":\"Massahun MCHP\"},\"QaeQJJCmnTS\":{\"name\":\"Sembehunwo MCHP\"},\"S6KDC0jVhmD\":{\"name\":\"Massaba MCHP\"},\"u3rHGQGLLP7\":{\"name\":\"Kanku Bramaia MCHP\"},\"l2kZRcJjomr\":{\"name\":\"Hima MCHP\"},\"BV4IomHvri4\":{\"name\":\"Ahmadiyya Muslim Hospital\"},\"t7bcrWLjL1m\":{\"name\":\"Jao MCHP\"},\"GvstqlRRnpV\":{\"name\":\"Sumbaria MCHP\"},\"KfUCAQoOIae\":{\"name\":\"Pelewahun (Baoma) MCHP\"},\"W3t0pSZLtrC\":{\"name\":\"Gendema MCHP\"},\"etrIik4vsBQ\":{\"name\":\"Kawula CHP\"},\"PyLBGdbzdEo\":{\"name\":\"Kamboma MCHP\"},\"tBRDdxfKbMx\":{\"name\":\"Liya MCHP\"},\"WhCQNekdIwM\":{\"name\":\"Moyeamoh CHP\"},\"z6v73gowbuM\":{\"name\":\"Kortohun CHP\"},\"Zwnfm4rnzbZ\":{\"name\":\"Stocco CHP\"},\"xmZNDeO0qCR\":{\"name\":\"Govt. Medical Hospital\"},\"vgOQ7fWmMyZ\":{\"name\":\"Sambaya MCHP\"},\"Uo4cyJwAhTW\":{\"name\":\"Mutual Faith Clinic\"},\"DJr17K6RWzO\":{\"name\":\"Mogbongisseh MCHP\"},\"Jiymtq0A01x\":{\"name\":\"Bafodia CHC\"},\"hyLU8ivDJDi\":{\"name\":\"Mid Land MCHP\"},\"GtJoxCaM2zg\":{\"name\":\"Mabain MCHP\"},\"lPeZdUm9fD7\":{\"name\":\"Blessed Mokaba East\"},\"GcwGqLqyi1M\":{\"name\":\"Rotawa CHP\"},\"al4GkB6X2X3\":{\"name\":\"Ngieyehun MCHP\"},\"CEoD9uQVIZB\":{\"name\":\"Mabonkanie MCHP\"},\"geVF87N7qTw\":{\"name\":\"Kpayama 2 MCHP\"},\"fXT1scbEObM\":{\"name\":\"Family Clinic\"},\"EuoA3Crpqts\":{\"name\":\"Mbundorbu MCHP\"},\"ueuQlqb8ccl\":{\"name\":\"Panderu MCHP\"},\"tdhB1JXYBx2\":{\"name\":\"Kunsho CHP\"},\"YBZcWphXQ99\":{\"name\":\"Kareneh MCHP\"},\"o0BgK1dLhF8\":{\"name\":\"Bendugu CHC\"},\"agEKP19IUKI\":{\"name\":\"Tambiama CHC\"},\"f90eISKFm7P\":{\"name\":\"Doujou CHP\"},\"JU4dWUv0Pmd\":{\"name\":\"Royeiben MCHP\"},\"DlLBIHdpaTy\":{\"name\":\"Waiima (Kori) MCHP\"},\"S9QckzKX6Lg\":{\"name\":\"Kormende MCHP\"},\"b7YDjQ6DBzt\":{\"name\":\"Kamagbewu MCHP\"},\"Fhko00f3hXT\":{\"name\":\"Taninahun MCHP\"},\"RVAkLOVWSWc\":{\"name\":\"Mansumana CHP\"},\"iqd7BiRHor0\":{\"name\":\"Kamadu Sokuralla MCHP\"},\"OY7mYDATra3\":{\"name\":\"Massingbi CHC\"},\"PLoeN9CaL7z\":{\"name\":\"SLRCS (Koinadugu) Clinic\"},\"IWb1hstfROc\":{\"name\":\"Gandorhun CHP\"},\"m21WB5iqHAb\":{\"name\":\"Ngiehun (Lower Bambara) MCHP\"},\"PduUQmdt0pB\":{\"name\":\"Numea CHC\"},\"SZrG4yHGV4x\":{\"name\":\"Madina Gbonkobor MCHP\"},\"Ioxjc2KBjWd\":{\"name\":\"Fengehun MCHP\"},\"UgUcwzbEv2C\":{\"name\":\"Moyollo MCHP\"},\"y5hLlID8ihI\":{\"name\":\"Barlie MCHP\"},\"jk1TtiBM5hz\":{\"name\":\"Holy Mary Hospital\"},\"D2rB1GRuh8C\":{\"name\":\"Gbamgbama CHC\"},\"NpHsnQ2L1oY\":{\"name\":\"Bumpetoke CHP\"},\"Gm7YUjhVi9Q\":{\"name\":\"Fairo CHC\"},\"HTDuY3uxj6u\":{\"name\":\"Gloucester CHP\"},\"D3oZZXtXjNk\":{\"name\":\"Foindu MCHP\"},\"Vw6CNyFUeh9\":{\"name\":\"Gbandiwulo CHP\"},\"KR0jLuFOB3d\":{\"name\":\"Griema MCHP\"},\"m7fBMpmVpSM\":{\"name\":\"Kolisokor MCHP\"},\"rZkUcho9Z65\":{\"name\":\"Torma Bum CHP\"},\"r5WWF9WDzoa\":{\"name\":\"Baama CHC\"},\"g5lonXJ9ndA\":{\"name\":\"Hinistas CHC\"},\"GjJjES51GvK\":{\"name\":\"Vaama MCHP\"},\"cKXicCOquXe\":{\"name\":\"Mange Bissan MCHP\"},\"Bpvug2zxHEZ\":{\"name\":\"Njala University Hospital\"},\"AKvgfYx5WZq\":{\"name\":\"Hill Station MCHP\"},\"FsunWIQLXoF\":{\"name\":\"Gbalan Thallan MCHP\"},\"a1dP5m3Clw4\":{\"name\":\"Baoma Kpenge CHP\"},\"PwgoRuWEDvJ\":{\"name\":\"Belebu CHP\"},\"T2Cn45nBY0u\":{\"name\":\"SLRC (Mattru) Clinic\"},\"XLiqwElsFHO\":{\"name\":\"Kissy Koya MCHP\"},\"kRWIof0qPJj\":{\"name\":\"Kondiama MCHP\"},\"HQoxFu4lYPS\":{\"name\":\"Pellie CHC\"},\"X3D19LoA2Ij\":{\"name\":\"Gbombana MCHP\"},\"D6yiaX1K5sO\":{\"name\":\"Bomaru CHP\"},\"RUCp6OaTSAD\":{\"name\":\"St. John of God Catholic Clinic\"},\"yvDKjcRRQsR\":{\"name\":\"Rogbin MCHP\"},\"iIpPPnnzDo6\":{\"name\":\"Tongoro MCHP\"},\"voQXVNftP4W\":{\"name\":\"Maami CHP\"},\"Qw7c6Ckb0XC\":{\"name\":\"UMC Clinic Taiama\"},\"ZzdTFqWrlDa\":{\"name\":\"Geima CHP\"},\"yZPsWcZC9WA\":{\"name\":\"Sellah Kafta MCHP\"},\"EURoFVjowXs\":{\"name\":\"Masiaka CHC\"},\"ii2KMnWMx2L\":{\"name\":\"Gandorhun (Gbane) CHC\"},\"J42QfNe0GJZ\":{\"name\":\"Mara CHC\"},\"VrDA0Hn4Xc6\":{\"name\":\"Harvest Time MCHP\"},\"AnXoUM1tfNT\":{\"name\":\"Yakaji MCHP\"},\"ntQSuMb7J21\":{\"name\":\"Lungi Town MCHP\"},\"gUPhNWkSXvD\":{\"name\":\"Rotifunk CHC\"},\"FclfbEFMcf3\":{\"name\":\"Kissy Health Centre\"},\"JLKGG67z7oj\":{\"name\":\"Fatkom Muchendeh Maternity Clinic\"},\"ZOZ4s2gTPj7\":{\"name\":\"Serekolia MCHP\"},\"tGf942oWszb\":{\"name\":\"Gbongboma MCHP\"},\"NwX8noGxLoz\":{\"name\":\"Makelleh MCHP\"},\"mkFoaAdosuY\":{\"name\":\"Soriebolomia MCHP\"},\"fPe1l06MurL\":{\"name\":\"Woyama MCHP\"},\"tt9XZYR5avl\":{\"name\":\"Mathuraneh MCHP\"},\"tR6e8k99ODA\":{\"name\":\"Mansundu MCHP\"},\"hBPtNXkQ3mP\":{\"name\":\"Ngiehun MCHP\"},\"QFcMulIoEii\":{\"name\":\"Gbainkfay MCHP\"},\"wcHRDp21Lw1\":{\"name\":\"Sussex MCHP\"},\"XL745P4ETSL\":{\"name\":\"Mobefa MCHP\"},\"V6QWyB0KqvP\":{\"name\":\"Juma MCHP\"},\"zAyK28LLaez\":{\"name\":\"Bongor MCHP\"},\"OjTS752GbZE\":{\"name\":\"Kagbankona MCHP\"},\"am6EFqHGKeU\":{\"name\":\"Mokpende MCHP\"},\"FgYDmGwmpEU\":{\"name\":\"Sorbeh Grima MCHP\"},\"mYMJHVqdBKt\":{\"name\":\"Kambama CHP\"},\"HWXk4EBHUyk\":{\"name\":\"Sahn (Malen) CHC\"},\"wB4tSXlryyO\":{\"name\":\"Voahun MCHP\"},\"p9ZtyC3LQ9f\":{\"name\":\"Niagorehun CHP\"},\"W2KnxOMvmgE\":{\"name\":\"Sumbuya CHC\"},\"tO01bqIipeD\":{\"name\":\"Buedu CHC\"},\"xEip3dtU8bp\":{\"name\":\"Lango Town MCHP\"},\"ZoHdXy2ueVn\":{\"name\":\"Malambay CHP\"},\"weLTzWrLXCO\":{\"name\":\"Bapuya MCHP\"},\"sYJCxNdKHxR\":{\"name\":\"Punduru CHP\"},\"CvYsZipdHMN\":{\"name\":\"Kumala CHP\"},\"pVuRAzSstbn\":{\"name\":\"Rokimbi MCHP\"},\"HDOnfLXKkYs\":{\"name\":\"Hamdalai MCHP\"},\"aBfyTU5Wgds\":{\"name\":\"Nduvuibu MCHP\"},\"VH7hLUaypel\":{\"name\":\"Gbangeima MCHP\"},\"X7ZVgRPt31q\":{\"name\":\"Lawana MCHP\"},\"nAH0uNc3b5f\":{\"name\":\"Tefeya CHP\"},\"eKoXODABUJe\":{\"name\":\"Masofinia MCHP\"},\"SCc0TNTDJED\":{\"name\":\"Gberia Timbakor MCHP\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"qHBTf9A89xW\":{\"name\":\"Dia CHP\"},\"Z9ny6QeqsgX\":{\"name\":\"Manjama UMC CHC\"},\"flJbtXOQ4ha\":{\"name\":\"Masumbrie MCHP\"},\"RxmgoSlw9YF\":{\"name\":\"Mokellay MCHP\"},\"en0j7qFnySQ\":{\"name\":\"Makabo MCHP\"},\"Y8foq27WLti\":{\"name\":\"Baoma Oil Mill CHC\"},\"mepHuAA9l51\":{\"name\":\"Rokonta CHC\"},\"uROAmk9ymNE\":{\"name\":\"Kindoyal Hospital\"},\"sHbLRZLmS4w\":{\"name\":\"Mapamurie MCHP\"},\"Uv15pOAstzX\":{\"name\":\"Komrabai Ngolla MCHP\"},\"F7oVR22kQ5J\":{\"name\":\"Elshadai Clinic\"},\"DvzKyuC0G4w\":{\"name\":\"Jojoima CHC\"},\"Luv2kmWWgoG\":{\"name\":\"Mondema CHC\"},\"YDDOlgRBEAA\":{\"name\":\"Yongoro CHP\"},\"qvHMAxtWWK6\":{\"name\":\"Burma 2 MCHP\"},\"PEZNsGbZaVJ\":{\"name\":\"Panguma Mission Hosp.\"},\"kqyeoWyfDmQ\":{\"name\":\"Mofombo MCHP\"},\"cw0Wm1QTHRq\":{\"name\":\"Joru CHC\"},\"LmRTf03IFkA\":{\"name\":\"Mambiama CHP\"},\"pe\":{},\"Ep5iWL1UKvF\":{\"name\":\"Kurubonla CHC\"},\"qEQFWnKh4gs\":{\"name\":\"Maharie MCHP\"},\"EH0dXLB4nZg\":{\"name\":\"Masimera CHC\"},\"XiORvSsxn6s\":{\"name\":\"Mayogbor MCHP\"},\"egv5Es0QlQP\":{\"name\":\"Kigbai MCHP\"},\"YQ3csPLAlrn\":{\"name\":\"St. Joseph CHC\"},\"aQoqXL4cZaF\":{\"name\":\"Fullah Town (B.Sebora) MCHP\"},\"FZxJ0KST9jn\":{\"name\":\"Gelehun MCHP\"},\"YFlZA0y0Vi6\":{\"name\":\"Mamanso Sanka CHP\"},\"FNnj3jKGS7i\":{\"name\":\"Bandajuma Clinic CHC\"},\"y77LiPqLMoq\":{\"name\":\"Gbenikoro MCHP\"},\"vv1QJFONsT6\":{\"name\":\"St. Joseph's Clinic\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"SFQblJrFblm\":{\"name\":\"Laleihun CHP\"},\"kFScvrF3wPo\":{\"name\":\"Madina MCHP\"},\"ObV5AR1NECl\":{\"name\":\"Karina MCHP\"},\"T62lSjsZe9n\":{\"name\":\"Komba Yendeh CHP\"},\"BDBXHeASwHl\":{\"name\":\"Katongha MCHP\"},\"PfZXxl6Wp3F\":{\"name\":\"Sulima CHP\"},\"IcVHzEm0b6Z\":{\"name\":\"Bonkababay MCHP\"},\"agM0BKQlTh3\":{\"name\":\"Batkanu CHC\"},\"IPvrsWbm0EM\":{\"name\":\"Gbalamuya MCHP\"},\"ApLCxUmnT6q\":{\"name\":\"Maborie MCHP\"},\"Tht0fnjagHi\":{\"name\":\"Serabu Hospital Mission\"},\"jSPLEMDwXN4\":{\"name\":\"Rofoindu CHP\"},\"DUDHgE5DECu\":{\"name\":\"Shekaia MCHP\"},\"KcCbIDzRcui\":{\"name\":\"Matotoka CHC\"},\"e2WgqiasKnD\":{\"name\":\"Taiama (Kori) CHC\"},\"Zr7pgiajIo9\":{\"name\":\"Komende (Kaiyamba) MCHP\"},\"GQcsUZf81vP\":{\"name\":\"Govt. Hosp. Makeni\"},\"Ykx8Ovui7g0\":{\"name\":\"Upper Saama MCHP\"},\"kMTHqMgenme\":{\"name\":\"Morning Star Clinic\"},\"wP1zsnNxbSE\":{\"name\":\"Kasoria MCHP\"},\"JemZqD90S44\":{\"name\":\"Dawa MCHP\"},\"cgqkFdShPzg\":{\"name\":\"Loreto Clinic\"},\"BzEwqabuW19\":{\"name\":\"Thompson Bay MCHP\"},\"BgOhMcH9bxq\":{\"name\":\"Levuma Kai MCHP\"},\"LqH7ZGU9KAx\":{\"name\":\"PCM Hospital\"},\"QCnJDmNjQy0\":{\"name\":\"Junctionla MCHP\"},\"Z0q0Y3GRugt\":{\"name\":\"Massabendu CHP\"},\"f6xGA6BZBLO\":{\"name\":\"Perrie MCHP\"},\"vwvDblM3MNX\":{\"name\":\"Gbolon MCHP\"},\"OwhDCucf4Ue\":{\"name\":\"Fotaneh Junction MCHP\"},\"rIgJX4N0DGZ\":{\"name\":\"Macauley Satellite Hospital\"},\"IlMQTFvcq9r\":{\"name\":\"Lowoma CHC\"},\"sDTodaygv5u\":{\"name\":\"Bath Bana MCHP\"},\"WUQrS4Yqmoy\":{\"name\":\"Kuranko MCHP\"},\"CgunjDKbM45\":{\"name\":\"Makalie MCHP\"},\"pNPmNeqyrim\":{\"name\":\"Foindu (Lower Bamabara) CHC\"},\"iH79WhpsByj\":{\"name\":\"Massayeima MCHP\"},\"Zq9ATbrmKIa\":{\"name\":\"Komende Luyaima MCHP\"},\"JttXgTlQAGE\":{\"name\":\"Ganya MCHP\"},\"K00jR5dmoFZ\":{\"name\":\"Karlu CHC\"},\"fmLRqcL9sWF\":{\"name\":\"Fankoya MCHP\"},\"xa4F6gesVJm\":{\"name\":\"York CHC\"},\"Vh1fsWOYcv1\":{\"name\":\"Mamuntha MCHP\"},\"JKhjdiwoQZu\":{\"name\":\"Foria CHP\"},\"DwpbWkiqjMy\":{\"name\":\"Bumbeh MCHP\"},\"OcRCVRy2M7X\":{\"name\":\"Benkia MCHP\"},\"hzf90qz08AW\":{\"name\":\"Njama CHC\"},\"CFPrsD3dNeb\":{\"name\":\"Tissana CHC\"},\"MpcMjLmbATv\":{\"name\":\"Bandajuma Yawei CHC\"},\"KYXbIQBQgP1\":{\"name\":\"Tikonko CHC\"},\"DKZnUSfwjKx\":{\"name\":\"Tikonko (gaura) MCHP\"},\"BG2fC2mRFOL\":{\"name\":\"Saahun (kpaka) MCHP\"},\"Efmr3Xo36DR\":{\"name\":\"Senehun Gbloh MCHP\"},\"lmNWdmeOYmV\":{\"name\":\"Kissy Town CHP\"},\"OjRCvy71kAL\":{\"name\":\"Mafaray CHP\"},\"OjXNuYyLaCJ\":{\"name\":\"Sendugu CHC\"},\"vlNXjc2lk9y\":{\"name\":\"Mano Menima CHP\"},\"t6S2MopeRaM\":{\"name\":\"Nyandehun (Mano Sakrim) MCHP\"},\"KxtLZtVmpur\":{\"name\":\"Leicester (RWA) CHP\"},\"GhXvo3BpCvo\":{\"name\":\"Kerefay Loko MCHP\"},\"AekX8HBymng\":{\"name\":\"Gissiwolo MCHP\"},\"mokUyyg3olJ\":{\"name\":\"Konjo CHP\"},\"wSHfjjFqUay\":{\"name\":\"Makarie MCHP\"},\"enrollmentou\":{\"name\":\"Enrollment org. unit\"},\"QIp6DHlMGfb\":{\"name\":\"Baptist Centre Kassirie\"},\"yXBtSoD0IRS\":{\"name\":\"Samiquidu MCHP\"},\"K6oyIMh7Lee\":{\"name\":\"Fadugu CHC\"},\"t0DLywkw6O1\":{\"name\":\"Masingbi-Lol MCHP\"},\"byOPfWkK6M6\":{\"name\":\"Petifu Line MCHP\"},\"bSj2UnYhTFb\":{\"name\":\"Kamaranka CHC\"},\"ZSBnWFBpPPJ\":{\"name\":\"Kondewakoro CHP\"},\"vxExu6yOYLg\":{\"name\":\"Maborognor MCHP\"},\"yMCshbaVExv\":{\"name\":\"Babara CHC\"},\"DwlFKzDSuQU\":{\"name\":\"Nyandeyaima MCHP\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"rxc497GUdDt\":{\"name\":\"Banka Makuloh MCHP\"},\"kBrq7i12aan\":{\"name\":\"Malama MCHP\"},\"rZxk3S0qN63\":{\"name\":\"Bo Govt. Hosp.\"},\"oNqqmKD0zXj\":{\"name\":\"Tambaliabalia MCHP\"},\"g5A3hiJlwmI\":{\"name\":\"UMC Mitchener Memorial Maternity & Health Centre\"},\"qjboFI0irVu\":{\"name\":\"Air Port Centre, Lungi\"},\"G5NCnFJ3bbV\":{\"name\":\"Makrugbeh MCHP\"},\"lekPjgUm0o2\":{\"name\":\"Kingtom Police Hospital (MI Room)\"},\"rm60vuHyQXj\":{\"name\":\"Nengbema CHC\"},\"iOA3z6Y3cq5\":{\"name\":\"Largo CHC\"},\"Mod8hYpQ3Ma\":{\"name\":\"Malema (Yawei) CHP\"},\"TWMVxJANJeU\":{\"name\":\"Kabba Ferry MCHP\"},\"U8uqyDAu5bH\":{\"name\":\"Govt. Hospital Moyamba\"},\"MXdbul7bBqV\":{\"name\":\"Mobai CHC\"},\"foPGXhwhlqp\":{\"name\":\"MCH Static Pujehun\"},\"EJoI3HArJ2W\":{\"name\":\"Bum Kaku MCHP\"},\"w3vRmEz3J7t\":{\"name\":\"Mamboma MCHP\"},\"dkmpOuVhBba\":{\"name\":\"Mathoir CHC\"},\"xWIyicUgscN\":{\"name\":\"St. John of God Catholic Hospital\"},\"mTNOoGXuC39\":{\"name\":\"Under Fives Clinic\"},\"ctN0WgIvfke\":{\"name\":\"Mattru UBC Hospital\"},\"fvytjjnlQlK\":{\"name\":\"Motoni MCHP\"},\"xO9WbCvFq5k\":{\"name\":\"Mercy Ship ACFC\"},\"jjtzkzrmG7s\":{\"name\":\"Banana Island MCHP\"},\"L05Bfpu7AcZ\":{\"name\":\"Gbeworbu-Gao CHP\"},\"vpNGJvZ0ljF\":{\"name\":\"Massam MCHP\"},\"UgYg0YW7ZIh\":{\"name\":\"Taninahun (Malen) CHP\"},\"lvxIJAb2QJo\":{\"name\":\"Sembehun Mamagewor MCHP\"},\"sFgNRYS5pBo\":{\"name\":\"Magbass MCHP\"},\"J1x66stNjk2\":{\"name\":\"Hunduwa CHP\"},\"QkczRcSeNck\":{\"name\":\"Kpowubu MCHP\"},\"lpAPY3QOY2D\":{\"name\":\"Bandawor MCHP\"},\"NfE9gvFwLIF\":{\"name\":\"Rokupa Govt. Hospital\"},\"SoXpnYO84eZ\":{\"name\":\"Venima CHP\"},\"pRg7dkjqNPc\":{\"name\":\"Falaba CHP\"},\"OuwX8H2CcRO\":{\"name\":\"Teko Barracks Clinic\"},\"XuGfiry96Bg\":{\"name\":\"Wellbody MCHP\"},\"gei3Sqw8do7\":{\"name\":\"KingHarman Rd. Hospital\"},\"cZxP4NE5O9z\":{\"name\":\"Lion for Lion Clinic\"},\"ncGs9vXS36w\":{\"name\":\"Small Sefadu MCHP\"},\"XePkcmza9e8\":{\"name\":\"Makarankay MCHP\"},\"Yc8Cmr5XS4B\":{\"name\":\"Petifu Fulamasa MCHP\"},\"RAsstekPRco\":{\"name\":\"Mambolo CHC\"},\"XkA2vbJAWHG\":{\"name\":\"Barmoi CHP\"},\"SptGAcmbgPz\":{\"name\":\"Tissana MCHP\"},\"DIQl5jJ17IE\":{\"name\":\"Magbaingba MCHP\"},\"bqSIIRuZ1qj\":{\"name\":\"Koindukura MCHP\"},\"YQYgz8exK9S\":{\"name\":\"Bombordu MCHP\"},\"PdGktj8bAML\":{\"name\":\"UBC Under 5\"},\"U8tyWV7WmIB\":{\"name\":\"Gbeika MCHP\"},\"dczh6Jfd4no\":{\"name\":\"Kayasie MCHP\"},\"Xnif5imKLlT\":{\"name\":\"Macrogba MCHP\"},\"AQQCxQqDxLe\":{\"name\":\"Konta CHP\"},\"sAO5hEWo4z5\":{\"name\":\"Mokorewa MCHP\"},\"uoPC2z9r7Cc\":{\"name\":\"Seidu MCHP\"},\"PcADvhvcaI2\":{\"name\":\"Kychom CHC\"},\"HMltAwIjIIe\":{\"name\":\"Moribaya MCHP\"},\"hTGeTrwzrPi\":{\"name\":\"Sandaru (Gaura) MCHP\"},\"iHQVo7h7KOQ\":{\"name\":\"Taninihun Kapuima MCHP\"},\"OGaAWQD6SYs\":{\"name\":\"Kalainkay MCHP\"},\"XJI24bY3AN7\":{\"name\":\"Salina CHP\"},\"OZ1olxsTyNa\":{\"name\":\"Bandajuma Sinneh MCHP\"},\"OwHjzJEVEUN\":{\"name\":\"Kamabaio MCHP\"},\"DMxw0SASFih\":{\"name\":\"Koindu CHC\"},\"WMj6mBDw76A\":{\"name\":\"Njama MCHP\"},\"eLLMnNjuluX\":{\"name\":\"Barakuya MCHP\"},\"JZraNIfZ5JM\":{\"name\":\"Grey Bush CHC\"},\"vj0HUVazItT\":{\"name\":\"Koije MCHP\"},\"ifw5aLygJEi\":{\"name\":\"Gbainty Wallah CHP\"},\"TrIXhUR4sDQ\":{\"name\":\"Mathen MCHP\"},\"JiEz2VDLwHY\":{\"name\":\"Komboya Gbauja MCHP\"},\"z1ielwdLtPl\":{\"name\":\"Foredugu MCHP\"},\"v2vi8UaIYlo\":{\"name\":\"Gbonkonka CHP\"},\"fHqBRE3LTiQ\":{\"name\":\"Robina MCHP\"},\"Ls2ESQONh9S\":{\"name\":\"Koidu Under Five Clinic\"},\"D0iakqyTknH\":{\"name\":\"Rorocks CHP\"},\"rFelzKE3SEp\":{\"name\":\"Salima MCHP\"},\"Vnc2qIRLbyw\":{\"name\":\"SLRCS (Nongowa) clinic\"},\"j57JudVQJtn\":{\"name\":\"Magbaesa MCHP\"},\"IHa6fsNWsOZ\":{\"name\":\"Niayahun CHP\"},\"kLNQT4KQ9hT\":{\"name\":\"Marie Stopes (Kakua) Clinic\"},\"PWqwcBdRGIH\":{\"name\":\"Magboki Rd. Mile 91 MCHP\"},\"JBhJiwqBCUa\":{\"name\":\"Mayolla MCHP\"},\"hjqgB6hEdl3\":{\"name\":\"Topan CHP\"},\"E497Rk80ivZ\":{\"name\":\"Bumpe CHC\"},\"sznCEDMABa2\":{\"name\":\"Ngiehun CHC\"},\"bne6tOoPaWn\":{\"name\":\"Nomo Faama CHP\"},\"XfVYz6l2rzg\":{\"name\":\"Magbolonthor MCHP\"},\"si34vmovtgR\":{\"name\":\"Makolor CHP\"},\"Brre03pQkKB\":{\"name\":\"Ngessehun MCHP\"},\"RzgSFJ9E46G\":{\"name\":\"Jormu MCHP\"},\"i7qaYfmGVDr\":{\"name\":\"Gbotima MCHP\"},\"roGdTjEqLZQ\":{\"name\":\"Yormandu CHC\"},\"aIsnJuZbmVA\":{\"name\":\"Dogoloya CHP\"},\"r93q83kZoR9\":{\"name\":\"Gbangba MCHP\"},\"DqfiI6NVnB1\":{\"name\":\"Sembehun 17 CHP\"},\"SzEmaH63Qe8\":{\"name\":\"Kwellu Ngieya CHP\"},\"g9xUM1x1f1i\":{\"name\":\"Samandu MCHP\"},\"M9q1wOOsrXp\":{\"name\":\"Yara MCHP\"},\"UJ80rknbJtm\":{\"name\":\"Magbeni MCHP\"},\"TbiRD4Bsz4Z\":{\"name\":\"Fulamansa MCHP\"},\"oxAoPoePpqy\":{\"name\":\"Gbaneh Lol MCHP\"},\"IW3guWF3uvF\":{\"name\":\"Loppa CHP\"},\"Crgx572DnXR\":{\"name\":\"Kaponkie MCHP\"},\"CqARw68kXbB\":{\"name\":\"Pehala MCHP\"},\"QZtMuEEV9Vv\":{\"name\":\"Rokupr CHC\"},\"szbAJSWOXjT\":{\"name\":\"Boroma MCHP\"},\"U4FzUXMvbI8\":{\"name\":\"Conakry Dee CHC\"},\"AXZq6q7Dr6E\":{\"name\":\"Buma MCHP\"},\"PnMPARoMhWW\":{\"name\":\"Mattru Jong MCHP\"},\"nv41sOz8IVM\":{\"name\":\"Pejewa CHC\"},\"So2b8zJfcMa\":{\"name\":\"Kpayama 1 MCHP\"},\"aVycEyoSBJx\":{\"name\":\"Fogbo (WAR) MCHP\"},\"nZblzPvJ5UW\":{\"name\":\"Rolembray MCHP\"},\"EmTN0L4EAVi\":{\"name\":\"SLRCS (Freetown) Clinic\"},\"S7KwVLbFlss\":{\"name\":\"Kpuabu MCHP\"},\"e5sGsWLEn3k\":{\"name\":\"Kondeya (Sandor) MCHP\"},\"U9klfqqGlRa\":{\"name\":\"Mana II CHP\"},\"m3VnSQbE8CD\":{\"name\":\"Newton CHC\"},\"DxguTiXvIJu\":{\"name\":\"Helegombu MCHP\"},\"AGrsLyKWrVX\":{\"name\":\"Kania MCHP\"},\"UlgEReuUPM4\":{\"name\":\"Masumana MCHP\"},\"DVjewuIdgMN\":{\"name\":\"Woreh Bana MCHP\"},\"U514Dz4v9pv\":{\"name\":\"George Brook Health Centre\"},\"bLYNonGzr0Y\":{\"name\":\"Mokainsumana CHP\"},\"vxa2YQRGV7I\":{\"name\":\"St. Luke's Wellington\"},\"r4W2vzlmPhm\":{\"name\":\"Feiba CHP\"},\"wUmVUKhnPuy\":{\"name\":\"Kangahun CHC\"},\"vELaJEPLOPF\":{\"name\":\"Barmoi Munu CHP\"},\"REtQE1gstTf\":{\"name\":\"Sembeima MCHP\"},\"wQ71REGAMet\":{\"name\":\"Benkeh MCHP\"},\"uczMdDZXdtl\":{\"name\":\"New London MCHP\"},\"w9FJ9oAdFys\":{\"name\":\"UFC Magburaka\"},\"R9gZAoI9aQM\":{\"name\":\"Mokotawa CHP\"},\"InQWjSe6k2f\":{\"name\":\"Saahun (barri) MCHP\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"VjygCFzqcYu\":{\"name\":\"Njagbahun MCHP\"},\"mUuCjQWMaOc\":{\"name\":\"Bambara MCHP\"},\"Z8Cm76B2726\":{\"name\":\"Mayassoh MCHP\"},\"gE3gEGZbQMi\":{\"name\":\"Madina (BUM) CHC\"},\"zuXW98AEbE7\":{\"name\":\"Kamasondo CHC\"},\"eRg3KZyWUSJ\":{\"name\":\"Fullawahun MCHP\"},\"TjZwphhxCuV\":{\"name\":\"Kagbere CHC\"},\"KQFAul3T9xz\":{\"name\":\"Nafaya MCHP\"},\"LV2b3vaLRl1\":{\"name\":\"Holy Mary Clinic\"},\"GyH8bjdOTsD\":{\"name\":\"Mansadu MCHP\"},\"VhRX5JDVo7R\":{\"name\":\"Waterloo CHC\"},\"VjVYaKZ9t4K\":{\"name\":\"Menicurve MCHP\"},\"uPshwz3B3Uu\":{\"name\":\"Bandasuma CHP\"},\"pvTYrkG1d6f\":{\"name\":\"Rogbaneh MCHP\"},\"Z7UAnjpK74g\":{\"name\":\"Looking Town MCHP\"},\"dx4NOnoGtE7\":{\"name\":\"Yemoh MCHP\"},\"ldXIdLNUNEn\":{\"name\":\"Connaught Hospital\"},\"Dluer5aKZmd\":{\"name\":\"Semabu MCHP\"},\"HC2NlwpoXfb\":{\"name\":\"Kombilie MCHP\"},\"azRICFoILuh\":{\"name\":\"Golu MCHP\"},\"ptc0SQi05E4\":{\"name\":\"Massah Memorial Maternity MCHP\"},\"XQudzejlhJZ\":{\"name\":\"UFC Nongowa\"},\"aSfF9kuNINJ\":{\"name\":\"Bambuibu Tommy MCHP\"},\"ke2gwHKHP3z\":{\"name\":\"Petifu CHC\"},\"VH8vOjm0l8w\":{\"name\":\"Jui CHP\"},\"T1lTKu6zkHN\":{\"name\":\"Mamanso Kafla MCHP\"},\"i7Oh2tlkToJ\":{\"name\":\"Fodaya MCHP\"},\"g10jm7jPdzf\":{\"name\":\"Hangha CHC\"},\"CbIWQQoWcLc\":{\"name\":\"Kabombeh MCHP\"},\"prNiMdHuaaU\":{\"name\":\"Serabu (Bumpe Ngao) UFC\"},\"SQz3xtx1Sgr\":{\"name\":\"Yankasa MCHP\"},\"sYjp3h6amhA\":{\"name\":\"Mendekelema CHP\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"Urk55T8KgpT\":{\"name\":\"Yoyah CHP\"},\"OTlKtnhvEm1\":{\"name\":\"Kagbo MCHP\"},\"GKrklllwmbU\":{\"name\":\"Sogballeh MCHP\"},\"bPJABq7F5Iy\":{\"name\":\"Rogbangba MCHP\"},\"J3wTSn87RP2\":{\"name\":\"Manjeihun MCHP\"},\"sM0Us0NkSez\":{\"name\":\"Kroo Bay CHC\"},\"cdmkMyYv04T\":{\"name\":\"Leprosy & TB Hospital\"},\"mRNfATVxa3m\":{\"name\":\"Mangaybana CHP\"},\"g8upMTyEZGZ\":{\"name\":\"Njandama MCHP\"},\"d7hw1ababST\":{\"name\":\"Konjo MCHP\"},\"G6LbealddgU\":{\"name\":\"Sawuria CHP\"},\"jNb63DIHuwU\":{\"name\":\"Baoma Station CHP\"},\"LFpl1falVZi\":{\"name\":\"Gbindi CHP\"},\"YldSFPxB6WH\":{\"name\":\"Makiteh MCHP\"},\"u3B5RqJuDAP\":{\"name\":\"Njagbahun (L.Banta) MCHP\"},\"aXnGiQGhOAj\":{\"name\":\"Lawana (Kongbora) MCHP\"},\"EFTcruJcNmZ\":{\"name\":\"Yengema CHP\"},\"Jd7G0NYBTx1\":{\"name\":\"Sebengu MCHP\"},\"TmCsvdJLHoX\":{\"name\":\"Mabunduka CHC\"},\"BNFrspDBKel\":{\"name\":\"Zimmi CHC\"},\"Wr8kmywwseZ\":{\"name\":\"Benduma MCHP\"},\"ALnjmvcRSxU\":{\"name\":\"Madina Wesleyan Mission\"},\"DplgrYeRIZ1\":{\"name\":\"John Thorpe MCHP\"},\"dBD9OHJFN8u\":{\"name\":\"Yekior MCHP\"},\"iPcreOldeV9\":{\"name\":\"Benguema MI Room\"},\"Yj2ni275yPJ\":{\"name\":\"Baoma (Koya) CHC\"},\"UUZoBCSn245\":{\"name\":\"Rokel (Masimera) MCHP\"},\"eqPIdr5yD1Q\":{\"name\":\"Rokolon MCHP\"},\"SnCrOCRrxGX\":{\"name\":\"Koakoyima CHC\"},\"zLiMZ1WrxdG\":{\"name\":\"Panlap MCHP\"},\"wtdBuXDwZYQ\":{\"name\":\"Praise Foundation CHC\"},\"GvFqTavdpGE\":{\"name\":\"Agape CHP\"},\"PaNv9VyD06n\":{\"name\":\"Manowa CHC\"},\"GHPuYdLcVN5\":{\"name\":\"Tawahun MCHP\"},\"w9XjBMJYL9R\":{\"name\":\"MCH Static\\/U5\"},\"AlLmKZIIIT4\":{\"name\":\"Gbamandu MCHP\"},\"UoLtRvXxNaB\":{\"name\":\"Maronko MCHP\"},\"lBMmM0HBp4s\":{\"name\":\"Musaia (Koinadugu) CHC\"},\"uRQj8WRK0Py\":{\"name\":\"Masongbo CHC\"},\"KuR0y0h0mOM\":{\"name\":\"Marie Stopes Clinic (Abedeen R)\"},\"H0OkaM4ReRK\":{\"name\":\"Sam Lean's MCHP\"},\"k1Y0oNqPlmy\":{\"name\":\"Gboyama CHC\"},\"cZZG5BMDLps\":{\"name\":\"Borongoh Makarankay CHP\"},\"CKkE4GBJekz\":{\"name\":\"Weima CHC\"},\"Xzxy8NuVsLp\":{\"name\":\"Mabayo MCHP\"},\"XsB16iHtwLL\":{\"name\":\"Mania MCHP\"},\"CebtBqqp1fp\":{\"name\":\"Makoni Line MCHP\"},\"EZIMUaUD8AJ\":{\"name\":\"Rogballan CHP\"},\"U7yKrx2QVet\":{\"name\":\"Bandaperie CHP\"},\"L5gENbBNNup\":{\"name\":\"Boajibu CHC\"},\"eyfrdOUUkXO\":{\"name\":\"Masankorie CHP\"},\"zw5ppT2dwZy\":{\"name\":\"Tokpombu (Dama) CHP\"},\"oiSllOTiHNx\":{\"name\":\"Thellia CHP\"}},\"dimensions\":{\"enrollmentou\":[\"Rp268JB6Ne4\",\"cDw53Ej8rju\",\"GvFqTavdpGE\",\"plnHVbJR6p4\",\"BV4IomHvri4\",\"qjboFI0irVu\",\"dWOAzMcK2Wt\",\"kbGqmM6ZWWV\",\"eoYV2p74eVz\",\"nq7F0t1Pz6t\",\"r5WWF9WDzoa\",\"yMCshbaVExv\",\"tlMeFk8C4CG\",\"Jiymtq0A01x\",\"BH7rDkWjUqc\",\"Rll4VmTDRiE\",\"XtuhRhmbrJM\",\"c41XRVOYNJm\",\"Eyj2kiEJ7M3\",\"HFyjUvMjQ8H\",\"MHAWZr2Caxw\",\"LOpWauwwghf\",\"mUuCjQWMaOc\",\"TNbHYOuQi8s\",\"aSfF9kuNINJ\",\"wYLjA4vN6Y9\",\"jjtzkzrmG7s\",\"FNnj3jKGS7i\",\"ABM75Q1UfoP\",\"rx9ubw0UCqj\",\"OZ1olxsTyNa\",\"MpcMjLmbATv\",\"qO2JLjYrg91\",\"U7yKrx2QVet\",\"uPshwz3B3Uu\",\"aF6iPGbrcRk\",\"lpAPY3QOY2D\",\"t1aAdpBbDB3\",\"xQIU41mR69s\",\"pdF4XIHIGPx\",\"rxc497GUdDt\",\"Yj2ni275yPJ\",\"TQ5DSmdliN7\",\"a1dP5m3Clw4\",\"t52CJEyLhch\",\"Y8foq27WLti\",\"jNb63DIHuwU\",\"x8SUTSsJoeO\",\"FLjwMPWLrL2\",\"QIp6DHlMGfb\",\"weLTzWrLXCO\",\"eLLMnNjuluX\",\"y5hLlID8ihI\",\"XkA2vbJAWHG\",\"vyIl6s0lhKc\",\"vELaJEPLOPF\",\"tlvNeDXXrS7\",\"sDTodaygv5u\",\"UGVLYrO63mR\",\"agM0BKQlTh3\",\"iMZihUMzH92\",\"cUNdCErxl9g\",\"k92yudERPlv\",\"PwgoRuWEDvJ\",\"qusWt6sESRU\",\"VpYAl8dXs6m\",\"er9S4CQ9QOn\",\"DQHGtTGOP6b\",\"uFp0ztDOFbI\",\"amgb83zVxp5\",\"PMsF64R6OJX\",\"o0BgK1dLhF8\",\"n7wN9gMFfZ5\",\"Wr8kmywwseZ\",\"yDFM5J6WeKU\",\"iPcreOldeV9\",\"ZKL5hlVG6F6\",\"wQ71REGAMet\",\"OcRCVRy2M7X\",\"GHHvGp7tgtZ\",\"kUzpbgPCwVA\",\"xXhKbgwL39t\",\"WAjjFMDJKcx\",\"kBP1UvZpsNj\",\"lPeZdUm9fD7\",\"waNtxFbPjrI\",\"rZxk3S0qN63\",\"L5gENbBNNup\",\"D6yiaX1K5sO\",\"PB8FMGbn19r\",\"YQYgz8exK9S\",\"VXrJKs8hic4\",\"H97XE5Ea089\",\"aVlSMMvgVzf\",\"zAyK28LLaez\",\"IcVHzEm0b6Z\",\"VfZnZ6UKyn8\",\"uYG1rUdsJJi\",\"szbAJSWOXjT\",\"cZZG5BMDLps\",\"GRc9WXp9gSy\",\"vRC0stJ5y9Q\",\"tO01bqIipeD\",\"EJoI3HArJ2W\",\"AXZq6q7Dr6E\",\"OI0BQUurVFS\",\"LZclRdyVk1t\",\"DwpbWkiqjMy\",\"MwfWgjMRgId\",\"Q2USZSJmcNK\",\"E497Rk80ivZ\",\"cMFi8lYbXHY\",\"wbtk73Zwhj9\",\"d9zRBAoM8OC\",\"mkIugjeYSjE\",\"NpHsnQ2L1oY\",\"rspjJHg4WY1\",\"HVQ6gJE8R24\",\"lsqa3EEGHxv\",\"rpAgG9XCWhO\",\"qvHMAxtWWK6\",\"KiheEgvUZ0i\",\"h9q3qixffZT\",\"PD1fqyvJssC\",\"uYTq3TEO2a9\",\"U4FzUXMvbI8\",\"yTMrs5kClCv\",\"ldXIdLNUNEn\",\"U2QkKSeyL5r\",\"Xytauldn2QJ\",\"wByqtWCCuDJ\",\"RpRJUDOPtt7\",\"flQBQV8eyHc\",\"DErmFP7bri7\",\"m5BX6CvJ6Ex\",\"JemZqD90S44\",\"yets9NmUcRS\",\"C1tAqIpKB9k\",\"oIgBLlEo6eH\",\"sSgOnY1Xqd9\",\"qHBTf9A89xW\",\"M4hyYfnb21I\",\"KbO0JnhiMwl\",\"jKZ0U8Og5aV\",\"rwfuVQHnZJ5\",\"aIsnJuZbmVA\",\"f90eISKFm7P\",\"RpjUEvgWSNO\",\"ADeZNq1pKsu\",\"K3k64jslIlL\",\"F7oVR22kQ5J\",\"sK498nBOLfQ\",\"LaxJ6CD2DHq\",\"FO1Tq8vUa62\",\"ZpE2POxvl9P\",\"hKD6hpZUh9v\",\"K6oyIMh7Lee\",\"Gm7YUjhVi9Q\",\"kuqKh33SPgg\",\"pRg7dkjqNPc\",\"YTQRSW91PxO\",\"fXT1scbEObM\",\"Q23tMsKOoO6\",\"WT6JLfyR9lL\",\"fmLRqcL9sWF\",\"jfV49JGnYKF\",\"JLKGG67z7oj\",\"Pr2stbkaSX3\",\"r4W2vzlmPhm\",\"Ioxjc2KBjWd\",\"Eyqyhztf8G1\",\"rYIkxCJFtTX\",\"xKaB8tfbTzm\",\"NMcx2jmra3c\",\"iP4fRh8EHmF\",\"e0RGds86ow6\",\"i7Oh2tlkToJ\",\"aVycEyoSBJx\",\"fGp4OcovQpa\",\"pNPmNeqyrim\",\"D3oZZXtXjNk\",\"t66taqSF1mW\",\"LUGqPutql0P\",\"z1ielwdLtPl\",\"JKhjdiwoQZu\",\"OwhDCucf4Ue\",\"GkHpMSo5K60\",\"qqF8jshIs66\",\"TbiRD4Bsz4Z\",\"aQoqXL4cZaF\",\"z9KGMrElTYS\",\"eRg3KZyWUSJ\",\"dGZbEZroAWr\",\"uDzWmUDHKeR\",\"UWhv0MQOqoB\",\"ii2KMnWMx2L\",\"ZdPkczYqeIY\",\"IWb1hstfROc\",\"JttXgTlQAGE\",\"GAvxcmr5jB1\",\"RXeDDKU26rB\",\"ei21lW7hFPX\",\"kedYKTsv95j\",\"TWH05Rjz6oT\",\"YhBJbiD5N1z\",\"jGYT5U5qJP6\",\"QFcMulIoEii\",\"ifw5aLygJEi\",\"nDoybVJLD74\",\"IPvrsWbm0EM\",\"FsunWIQLXoF\",\"AlLmKZIIIT4\",\"EihevoTWn2i\",\"K5wBtEzE2qJ\",\"D2rB1GRuh8C\",\"Vw6CNyFUeh9\",\"w7a4l3XHIgi\",\"oxAoPoePpqy\",\"lQIe6vtSe1P\",\"r93q83kZoR9\",\"ctMepV9p92I\",\"ubsjwFFBaJM\",\"VH7hLUaypel\",\"E9oBVjyEaCe\",\"uedNhvYPMNu\",\"TEVtOFKcLAP\",\"U8tyWV7WmIB\",\"YAuJ3fyoEuI\",\"duINhdt3Yay\",\"y77LiPqLMoq\",\"D7UVRRE9iUC\",\"SCc0TNTDJED\",\"qELjt3LRkSD\",\"as1dnmlXLzG\",\"L05Bfpu7AcZ\",\"LFpl1falVZi\",\"NaVzm59XKGf\",\"TAN6Q7vjvuk\",\"l0WRLZlEgB1\",\"OzVuFaZgm5U\",\"TYq1YW7qs7k\",\"Pw9SihGDbZ5\",\"vwvDblM3MNX\",\"X3D19LoA2Ij\",\"cBi3y4lGhDd\",\"bHcw141PTsE\",\"tGf942oWszb\",\"MMrdfNDfBIi\",\"rebbn0ooFSO\",\"HOgWkpYH3KB\",\"BXJnMD2eJAx\",\"zm9breCeT1m\",\"M3dL6ZAIZ3I\",\"v2vi8UaIYlo\",\"i7qaYfmGVDr\",\"k1Y0oNqPlmy\",\"DA2BEQMhv9B\",\"ZzdTFqWrlDa\",\"FZxJ0KST9jn\",\"W3t0pSZLtrC\",\"FbD5Z8z22Yb\",\"U514Dz4v9pv\",\"TSyzvBiovKh\",\"ywNG86IY4Ve\",\"vPKxHJ1og0r\",\"m0XorV4WWg0\",\"AekX8HBymng\",\"HTDuY3uxj6u\",\"dQggcljEImF\",\"HAqUY00X9N5\",\"RaQGHRti7JM\",\"lf7FRlrchg3\",\"azRICFoILuh\",\"E4jn4059Y1x\",\"ObJjzhhBkfy\",\"jhtj3eQa1pM\",\"fRV3Fhz1IP8\",\"QpRIPul20Sb\",\"NnQpISrLYWZ\",\"djMCTPYvltl\",\"GQcsUZf81vP\",\"STv4PP4Hiyl\",\"ZvX8lXd1tYs\",\"U8uqyDAu5bH\",\"xmZNDeO0qCR\",\"vAdMjyOspGL\",\"lL2LBkhlsmV\",\"JZraNIfZ5JM\",\"KR0jLuFOB3d\",\"NLN0MvWv9tl\",\"xATvj8pdYoT\",\"ARAZtL7Bdpy\",\"HDOnfLXKkYs\",\"oolcy5HBlMy\",\"DSBXsRQSXUW\",\"g10jm7jPdzf\",\"VrDA0Hn4Xc6\",\"zQpYVEyAM2t\",\"DxguTiXvIJu\",\"UCwtaCrNUls\",\"AKvgfYx5WZq\",\"l2kZRcJjomr\",\"g5lonXJ9ndA\",\"LV2b3vaLRl1\",\"jk1TtiBM5hz\",\"J1x66stNjk2\",\"wjFsUXI1MlO\",\"W7ekX3gi0ut\",\"qzm5ww3U0vz\",\"t7bcrWLjL1m\",\"Umh4HKqqFp6\",\"QzPf0qKBU4n\",\"ndan8zClk4E\",\"cZtKKa9eJZ3\",\"vELbGdEphPd\",\"DplgrYeRIZ1\",\"DvzKyuC0G4w\",\"f7yRhIeFn1k\",\"m3QGt8fY3L0\",\"RzgSFJ9E46G\",\"cw0Wm1QTHRq\",\"PysJIi3VIol\",\"VH8vOjm0l8w\",\"V6QWyB0KqvP\",\"QCnJDmNjQy0\",\"EQUwHqZOb5L\",\"Xk2fvz4aTBU\",\"TWMVxJANJeU\",\"CbIWQQoWcLc\",\"wfGRNqXqf92\",\"duGLGssecoD\",\"OjTS752GbZE\",\"ZZmMpGIE7pD\",\"T3iVyvrCpZ0\",\"TjZwphhxCuV\",\"OTlKtnhvEm1\",\"Sglj9VCoQmc\",\"n3MRjKtwr3O\",\"hpXXBtRXXSd\",\"KKoPh1lDd9j\",\"NJolnlvYgLr\",\"OGaAWQD6SYs\",\"nYiOoF2nXIr\",\"aSxNNRxPuBP\",\"UqXSUMp19FB\",\"oDAoqMWcsJQ\",\"mt47bcb0Rcj\",\"OwHjzJEVEUN\",\"iqd7BiRHor0\",\"b7YDjQ6DBzt\",\"KnU2XHRvyiX\",\"HNv1aLPdMYb\",\"bSj2UnYhTFb\",\"F9zWBqG5Pmi\",\"eCfxBe1lnxb\",\"e4P2zTzM7gQ\",\"ZxuSbAmsLCn\",\"zuXW98AEbE7\",\"inpc5QsFRTm\",\"wO4z5Aqo0hf\",\"zO5hgxxfU4T\",\"DF76ZjQtFSg\",\"cJ7omISg7gG\",\"mYMJHVqdBKt\",\"GjWQK6UA4FO\",\"UUgajyaViT7\",\"N7mHLD3ljYc\",\"kO9xe2HCovK\",\"PyLBGdbzdEo\",\"hHKKi9WNoBG\",\"tXL6C7P0ObJ\",\"AFi1GjbeejL\",\"IpA5FViU8tk\",\"wUmVUKhnPuy\",\"pYr0Kcy93M2\",\"PSjKMcPGUvA\",\"RNGpZqutw3Y\",\"AGrsLyKWrVX\",\"nDwbwJZQUYU\",\"CTOMXJg41hz\",\"u3rHGQGLLP7\",\"KGN2jvZ0GJy\",\"GhDwjKv07iC\",\"Crgx572DnXR\",\"YBZcWphXQ99\",\"ObV5AR1NECl\",\"AlG0apJE5cm\",\"K00jR5dmoFZ\",\"jj1MhWhHqta\",\"OqBiNJjKQAu\",\"wP1zsnNxbSE\",\"yh1PrRTboyg\",\"cJkZLwhL8RP\",\"pmzk0ho80aA\",\"NjyJYiIuKIG\",\"MPUiud3BYRq\",\"yEU926iVAJJ\",\"Zp2Yi4j2AAH\",\"BDBXHeASwHl\",\"qMbxFg9McOF\",\"rwgK8TkRwHl\",\"etrIik4vsBQ\",\"dczh6Jfd4no\",\"k8ZPul89UDm\",\"tEgxbwwrwUd\",\"QMnoFLTLpkY\",\"s7SLtx8wmRA\",\"UjusePB4jmP\",\"lELJZCBxz7H\",\"GhXvo3BpCvo\",\"lCEeiuv4NaB\",\"Q8oWscr9rlQ\",\"egv5Es0QlQP\",\"uROAmk9ymNE\",\"gei3Sqw8do7\",\"lekPjgUm0o2\",\"FclfbEFMcf3\",\"XLiqwElsFHO\",\"lmNWdmeOYmV\",\"EQc3n1juPFn\",\"SnCrOCRrxGX\",\"PwoQgMJNWbR\",\"VF7LfO19vxS\",\"zsqxu7ZZRpO\",\"OzjRQLn3G24\",\"Ls2ESQONh9S\",\"vj0HUVazItT\",\"hMBotMwWnU1\",\"DMxw0SASFih\",\"GM9ddjXIO5b\",\"bqSIIRuZ1qj\",\"F2TAF765q1b\",\"m7fBMpmVpSM\",\"T62lSjsZe9n\",\"HC2NlwpoXfb\",\"JiEz2VDLwHY\",\"Zr7pgiajIo9\",\"Zq9ATbrmKIa\",\"NqwvaQC1ni4\",\"w0QDch3dyPH\",\"Uv15pOAstzX\",\"cUltUneFSan\",\"mhJQYk2Jwym\",\"jkPHBqdn9SA\",\"p310xqwAJge\",\"ZSBnWFBpPPJ\",\"e5sGsWLEn3k\",\"OynYyQiFu82\",\"kRWIof0qPJj\",\"qwmh84DV65K\",\"yJ1xkKha5oE\",\"aXsLBCzwYWW\",\"mokUyyg3olJ\",\"d7hw1ababST\",\"SVEfwJ0BGeD\",\"UAtEKSd5QTf\",\"AQQCxQqDxLe\",\"TkhwySsXC5V\",\"nornKUJmQqn\",\"m73lWmo5BDG\",\"lwHs72tP6Kh\",\"hCm2Nh7C8BW\",\"mwN7QuEfT8m\",\"S9QckzKX6Lg\",\"MrME31scKA1\",\"CSDGDOa7wHd\",\"z6v73gowbuM\",\"cXOR7vSMBKO\",\"jr5hIZcJBXB\",\"brnL0W3Fbsj\",\"Kmu7ox2MiiU\",\"TljiT6C5D0J\",\"nE01sGNCY5P\",\"PFZbQjwty2n\",\"So2b8zJfcMa\",\"geVF87N7qTw\",\"U02o1QAm6cC\",\"RTixJpRqS4C\",\"GIRLSZ1tB00\",\"kDxbU1uSBFh\",\"mGmu0GJ5neg\",\"DcmSvQd5N8c\",\"bM4Ky73uMao\",\"QkczRcSeNck\",\"S7KwVLbFlss\",\"pMEnu7BjqMz\",\"sM0Us0NkSez\",\"M9JyYBZTqR7\",\"CvYsZipdHMN\",\"bJ0VSATHwO2\",\"S2NaydvPENH\",\"jYPY8mT8gn6\",\"tdhB1JXYBx2\",\"bKiJzk8ZZbS\",\"kIbcKauMdlW\",\"WUQrS4Yqmoy\",\"Ep5iWL1UKvF\",\"SzEmaH63Qe8\",\"PcADvhvcaI2\",\"yg7uxUol97F\",\"K0d08d3sUOv\",\"NRPCjDljVtu\",\"SFQblJrFblm\",\"N3tpEjZcPm9\",\"xEip3dtU8bp\",\"iOA3z6Y3cq5\",\"aXnGiQGhOAj\",\"X7ZVgRPt31q\",\"KxtLZtVmpur\",\"rs87nYgwbKv\",\"cdmkMyYv04T\",\"YvwYw7GilkP\",\"Bf9R1R91mw4\",\"BgOhMcH9bxq\",\"BqRElDluXGa\",\"cZxP4NE5O9z\",\"tBRDdxfKbMx\",\"hIpcmjLrDDW\",\"Z7UAnjpK74g\",\"IW3guWF3uvF\",\"cgqkFdShPzg\",\"IlMQTFvcq9r\",\"rozv5QUSE7a\",\"PqlNXedmh7u\",\"m8qnxndRDR6\",\"gsypzntLahf\",\"ntQSuMb7J21\",\"xuk02oLk12O\",\"q56204kKXgZ\",\"k6DIO9LIEk9\",\"c9wCIfbcyVo\",\"voQXVNftP4W\",\"q5kAX5MyPB6\",\"xRsoZIRmnt4\",\"GtJoxCaM2zg\",\"Ahh47q8AkId\",\"fCFdj2T0Bq1\",\"Xzxy8NuVsLp\",\"MiYhwDprCCA\",\"taKiTcaf05H\",\"mc3jvzpzSi4\",\"PybxeRWVSrI\",\"lBob31rp6l4\",\"r0TCGeLkQKI\",\"CEoD9uQVIZB\",\"jVDUkOBCjDy\",\"fmkqsEx6MRo\",\"ApLCxUmnT6q\",\"vxExu6yOYLg\",\"TmCsvdJLHoX\",\"b09gf2vvZDb\",\"bkMlhoccaVw\",\"rIgJX4N0DGZ\",\"FupvWBUFXr7\",\"U0KpeSx4UIB\",\"Xnif5imKLlT\",\"b1F5bfb7WUR\",\"gE3gEGZbQMi\",\"SFQigiC2ISS\",\"pJj2r2HElLE\",\"SZrG4yHGV4x\",\"I48Qu6R0sGm\",\"kFScvrF3wPo\",\"ALnjmvcRSxU\",\"QBRQnWPRO3V\",\"OjRCvy71kAL\",\"z4silfLpw2G\",\"C1zlHePEQe6\",\"simyC07XwnS\",\"L3GgannGGKl\",\"j57JudVQJtn\",\"XjpmsLNjyrz\",\"cTU2WmWcJKx\",\"DIQl5jJ17IE\",\"a5glgtnXJRG\",\"sFgNRYS5pBo\",\"bf6PXrSNMKK\",\"hZpaU5uFSDm\",\"VeXU3mndzri\",\"WxMIZC6Cxqs\",\"UJ80rknbJtm\",\"uAk40nFigUK\",\"koa3hwZZ2i7\",\"n9HIySyR00g\",\"PWqwcBdRGIH\",\"XfVYz6l2rzg\",\"mEUUK7MHLSF\",\"ltF8BmYAXpQ\",\"CKJ9YS2AbWy\",\"qEQFWnKh4gs\",\"LnToY3ExKxL\",\"ShdRyzuLKA2\",\"kSo9KSpHUPL\",\"en0j7qFnySQ\",\"ewh5SKxcCAl\",\"scc4QyxenJd\",\"CgunjDKbM45\",\"Dbn6fyCgMBV\",\"XePkcmza9e8\",\"wSHfjjFqUay\",\"NwX8noGxLoz\",\"dmdYffw2I0F\",\"jbfISeV6Wdu\",\"YldSFPxB6WH\",\"KwSj4DlRWAm\",\"iMDr2FG7i8Q\",\"si34vmovtgR\",\"JKdMirJ02nv\",\"CebtBqqp1fp\",\"RHJram03Rlm\",\"Zf2v0kbI7ah\",\"cd3U2Tp0qR2\",\"G5NCnFJ3bbV\",\"E7IDb3nNiW7\",\"LWlh25dfvEA\",\"FFU3PJ3pY7s\",\"kBrq7i12aan\",\"ZoHdXy2ueVn\",\"Mod8hYpQ3Ma\",\"dCvUVvKnhMe\",\"TrmusBXxLm3\",\"F0uVXCVvOPO\",\"u0SlCNJnK3K\",\"d9uZeZ5fMUo\",\"ALZ2qr5u0X0\",\"eP4F9eB76B0\",\"T1lTKu6zkHN\",\"YFlZA0y0Vi6\",\"LmRTf03IFkA\",\"RAsstekPRco\",\"F7u30K5OIpi\",\"w3vRmEz3J7t\",\"Vh1fsWOYcv1\",\"FRX63UWciyO\",\"U9klfqqGlRa\",\"fUxVOkpX3yi\",\"WerHl8SDtRU\",\"EQnfnY03sRp\",\"CTnuuI55SOj\",\"gaOSAjPM07w\",\"mRNfATVxa3m\",\"cKXicCOquXe\",\"w3mBVfrWhXl\",\"XsB16iHtwLL\",\"mMvt6zhCclb\",\"lOv6IFgr6Fs\",\"Z9ny6QeqsgX\",\"J3wTSn87RP2\",\"Uwcj0mz78BV\",\"gowgzHWc8FT\",\"va2lE4FiVVb\",\"O1KFJmM6HUx\",\"vlNXjc2lk9y\",\"Fbq6Vxa4MIx\",\"XvqLmn4kZXy\",\"KvE0PYQzXMM\",\"hLGkoHmvBgI\",\"PaNv9VyD06n\",\"GyH8bjdOTsD\",\"RVAkLOVWSWc\",\"OUPkxfQld8y\",\"tR6e8k99ODA\",\"ZALwM386w0T\",\"RG6MGu5nUlI\",\"mshIal30ffW\",\"sHbLRZLmS4w\",\"HlDMbDWUmTy\",\"SIxGTeya5lN\",\"RQgXBKxgvHf\",\"J42QfNe0GJZ\",\"v0HMlSxlH7l\",\"Bift1B4gjru\",\"kLNQT4KQ9hT\",\"KuR0y0h0mOM\",\"LzvoPaeLPsb\",\"UoLtRvXxNaB\",\"suFG8zx4bU3\",\"ETRqfu74kge\",\"Hu31NCRjZlj\",\"wB4R3E1X6pC\",\"eyfrdOUUkXO\",\"l3jnkNNpoD8\",\"CY8cV5khn7e\",\"EURoFVjowXs\",\"EH0dXLB4nZg\",\"t0DLywkw6O1\",\"eKoXODABUJe\",\"D6B4jrCpCwu\",\"uRQj8WRK0Py\",\"PhR1PdMTzhW\",\"xWjiTeok0Sr\",\"dqHvtpUqLwB\",\"S6KDC0jVhmD\",\"Z0q0Y3GRugt\",\"ptc0SQi05E4\",\"GA7eQkgK5mX\",\"vpNGJvZ0ljF\",\"iH79WhpsByj\",\"cag6vQQ9SQk\",\"jIrb5XckcU6\",\"OY7mYDATra3\",\"XzmWizbR343\",\"UlgEReuUPM4\",\"flJbtXOQ4ha\",\"EXbPGmEUdnc\",\"KFhJrkqnrnb\",\"TrIXhUR4sDQ\",\"caif2tNAS0n\",\"dkmpOuVhBba\",\"lyONqUkY1Bq\",\"rLaGvUnv2BF\",\"OTn9VMNEkdo\",\"tt9XZYR5avl\",\"uGa5JtIMfRx\",\"KcCbIDzRcui\",\"PnMPARoMhWW\",\"Qu0QOykPdcD\",\"ctN0WgIvfke\",\"Srnpwq8jKbp\",\"R0CmUlFULXg\",\"Z8Cm76B2726\",\"aHs9PLxIdbr\",\"XiORvSsxn6s\",\"JBhJiwqBCUa\",\"gfk1TNPI4wN\",\"vQYIk5G9NxP\",\"QN4te5Z5svQ\",\"nImgPWDVQIa\",\"irVdYBmHBxs\",\"EuoA3Crpqts\",\"Pae8DR7VmcL\",\"kpDoH80fwdX\",\"foPGXhwhlqp\",\"w9XjBMJYL9R\",\"HcB2W6Fgp7i\",\"NnGUNkc5Zq8\",\"SlNw6FxElY9\",\"sYjp3h6amhA\",\"YWXXO0XMkQe\",\"VjVYaKZ9t4K\",\"HHc5HDPFlXy\",\"xO9WbCvFq5k\",\"bqtZrXoryDF\",\"hyLU8ivDJDi\",\"WoqN1oUBX2R\",\"ZW3XCXXiLcO\",\"QZzRkqdGjlm\",\"cZI3AWM7bIa\",\"MXdbul7bBqV\",\"XL745P4ETSL\",\"ua3kNk4uraZ\",\"cXMQtUId06K\",\"kqyeoWyfDmQ\",\"DZaJmtlaBMl\",\"DJr17K6RWzO\",\"wGsBlwh6Zzt\",\"jIkxZKctVhB\",\"GQpxsB7tekR\",\"bLYNonGzr0Y\",\"WxMmxNU6Gla\",\"sIVFEyNfOg4\",\"cNAp6CJeLxk\",\"kd2Aqw5S07V\",\"RxmgoSlw9YF\",\"JQJjsXvHE5M\",\"xt08cuqf1ys\",\"SC0nM3cbGHy\",\"BedE3DKQDFf\",\"cHqboEGRUiY\",\"sAO5hEWo4z5\",\"R9gZAoI9aQM\",\"qcYG2Id7GS8\",\"am6EFqHGKeU\",\"fA43H8Ds0Ja\",\"Luv2kmWWgoG\",\"PC3Ag91n82e\",\"WdgS1JcBL2g\",\"ih77LC7LE1p\",\"xMn4Wki9doK\",\"HMltAwIjIIe\",\"kMTHqMgenme\",\"AIM09vwxjoN\",\"cDRQOxX1wHO\",\"HHz1kAG1LKn\",\"XmfqaErvQ2T\",\"g3O1pGAfgK1\",\"fvytjjnlQlK\",\"BpWJ3cRsO6g\",\"Gtnbmf4LkOz\",\"rCKWdLr4B8K\",\"MuZJ8lprGqK\",\"WhCQNekdIwM\",\"zEsMdeJOty4\",\"UgUcwzbEv2C\",\"sY1WN6LjmAx\",\"a04CZxe0PSe\",\"lBMmM0HBp4s\",\"sTOXJA2KcY2\",\"Uo4cyJwAhTW\",\"KQFAul3T9xz\",\"nGb94wPdcqx\",\"bPHn9IgjKLC\",\"ZsjXrmZS59z\",\"aBfyTU5Wgds\",\"UOJlcpPnBat\",\"L4Tw4NlaMjn\",\"rm60vuHyQXj\",\"yP2nhllbQPh\",\"uczMdDZXdtl\",\"lzz1UhTzO4E\",\"mzsOsz0NwNY\",\"m3VnSQbE8CD\",\"YnuwSqXPx9H\",\"CvBAqD6RzLZ\",\"P4upLKrpkHP\",\"DiszpKrYNg8\",\"FwKJ7gYEv8U\",\"Brre03pQkKB\",\"DwEfz1MN7Z5\",\"m21WB5iqHAb\",\"sznCEDMABa2\",\"QII5GqfDfO3\",\"hBPtNXkQ3mP\",\"KuGO75X47Gk\",\"al4GkB6X2X3\",\"XGUOQaRUPjO\",\"hoJ0Do9loZl\",\"wwM3YPvBKu2\",\"aSnKB1sWaz4\",\"Vw4Uv6UPIPC\",\"p9ZtyC3LQ9f\",\"WOk7efLlLSj\",\"cC03EwJLBiO\",\"BJMWTGwuGiw\",\"IHa6fsNWsOZ\",\"YXdC9hjYPqQ\",\"QoROdPmIdY1\",\"u3B5RqJuDAP\",\"VjygCFzqcYu\",\"MUnd4KWox8m\",\"sLKHXoBIqSs\",\"aV9VVijeVB2\",\"QsAwd531Cpd\",\"vPz4Irz7sxR\",\"Bpvug2zxHEZ\",\"kvzdkXBxHoN\",\"hzf90qz08AW\",\"WMj6mBDw76A\",\"g8upMTyEZGZ\",\"bne6tOoPaWn\",\"fdsRQbuuAuh\",\"PduUQmdt0pB\",\"DINXUs8QZWg\",\"t6S2MopeRaM\",\"JQr6TJx5KE3\",\"mVvEwzoFutG\",\"zpEPGogIr6q\",\"DwlFKzDSuQU\",\"AiGBODidxPw\",\"dNT8lAL4zGo\",\"tHUYjt9cU6h\",\"ueuQlqb8ccl\",\"PEZNsGbZaVJ\",\"zLiMZ1WrxdG\",\"tSBcgrTDdB8\",\"tWjUy6MCx8q\",\"wzvDhS0TkAF\",\"LqH7ZGU9KAx\",\"CqARw68kXbB\",\"nv41sOz8IVM\",\"BTXwf2gl7av\",\"KfUCAQoOIae\",\"MQHszd6K6V5\",\"HQoxFu4lYPS\",\"pJv8NJlJNhU\",\"fYmE4ymzZSe\",\"XXlzHWzhf5d\",\"f6xGA6BZBLO\",\"ke2gwHKHP3z\",\"Yc8Cmr5XS4B\",\"byOPfWkK6M6\",\"DxPNV7VHauJ\",\"nbMpoRiVRWd\",\"zQ2pFkzGtIg\",\"FGV6TAbL0eN\",\"ctfiYW0ePJ8\",\"oV9P0VvL9Jh\",\"g7BLyiBb0ET\",\"EO6ghLtWv4W\",\"zY9ds4oNZxw\",\"k6lOze3vTzP\",\"wtdBuXDwZYQ\",\"sYJCxNdKHxR\",\"rNaQEFRINbd\",\"xXYv82KlBUh\",\"VZ6Cocesljy\",\"is3w3HROKVc\",\"wNYYRm2c9EK\",\"oRncQGhLYNE\",\"u6ZGNI8yUmt\",\"ym42ZOlfZ1P\",\"oph70zH8JB2\",\"gP6hn503KUX\",\"fHqBRE3LTiQ\",\"PHo0IV7Vk50\",\"X9zzzyPZViR\",\"jSPLEMDwXN4\",\"G5FuODAbH6X\",\"EZIMUaUD8AJ\",\"I2UW55qvn82\",\"pvTYrkG1d6f\",\"bPJABq7F5Iy\",\"qVvitxEF2ck\",\"yvDKjcRRQsR\",\"UxpUYgdb4oU\",\"UUZoBCSn245\",\"dtuiqEXYa7z\",\"pVuRAzSstbn\",\"eqPIdr5yD1Q\",\"mepHuAA9l51\",\"VTtyiYcc6TE\",\"X79FDd4EAgo\",\"NfE9gvFwLIF\",\"QZtMuEEV9Vv\",\"nZblzPvJ5UW\",\"FQ5CCuUKNLf\",\"GCbYmPqcOOP\",\"B9RxRfRUi2R\",\"D0iakqyTknH\",\"x3ti3t9eOuX\",\"n2qFnUIhbq3\",\"el8sgzyHuEe\",\"bPqP6eRfkyn\",\"GMOl74xzmAE\",\"GcwGqLqyi1M\",\"KaevAHPgkA8\",\"gUPhNWkSXvD\",\"msH78gZ7Fe6\",\"JU4dWUv0Pmd\",\"JNJIPX9DfaW\",\"InQWjSe6k2f\",\"BG2fC2mRFOL\",\"BJ3DJFBKwBR\",\"WZ8PTx8qQlE\",\"HWXk4EBHUyk\",\"NDqR2cWlVy3\",\"PuZOFApTSeo\",\"BXd3TqaAxkK\",\"AvGz949akv4\",\"rFelzKE3SEp\",\"XJI24bY3AN7\",\"H0OkaM4ReRK\",\"I2DzylqJa2i\",\"fNL2oehab2Q\",\"SKJoPDgjELa\",\"g9xUM1x1f1i\",\"BnVjTzwis3o\",\"vgOQ7fWmMyZ\",\"yXBtSoD0IRS\",\"hTGeTrwzrPi\",\"Mi4dWRtfIOC\",\"oUR5HPmim7E\",\"iIQENGb7za6\",\"vcY0lzBz6fU\",\"g6y7PS0UQR4\",\"UqHuR4IYvTY\",\"tNs4E0JcMKe\",\"G6LbealddgU\",\"u1eQDDtKqm7\",\"HPg74Rr7UWp\",\"Jd7G0NYBTx1\",\"uoPC2z9r7Cc\",\"yZPsWcZC9WA\",\"Dluer5aKZmd\",\"Mi4Ax9suQmB\",\"DqfiI6NVnB1\",\"egjrZ1PHNtT\",\"lvxIJAb2QJo\",\"IWM4eKPJJSc\",\"QaeQJJCmnTS\",\"REtQE1gstTf\",\"NqTZjfTIsxC\",\"OjXNuYyLaCJ\",\"Jyv7sjpl9bA\",\"oLuhRyYPxRO\",\"Efmr3Xo36DR\",\"xX4lIVqF4yb\",\"MBtmOhLs7y1\",\"wkYbuEwNWyf\",\"aRXfvyonenP\",\"MnfykVk3zin\",\"tcEjL7gmFJL\",\"Qr41Mw2MSjo\",\"prNiMdHuaaU\",\"wjP03y8OY5k\",\"Gba5bTc8NIg\",\"Tht0fnjagHi\",\"ZOZ4s2gTPj7\",\"kzmwOrwmzbW\",\"DUDHgE5DECu\",\"p9KfD6eaRvu\",\"cWIiusmHULW\",\"a1E6QWBTEwX\",\"Bq5nb7UAEGd\",\"nurO6U9bOLi\",\"IlnqGuxfQAw\",\"nCh5dBoJVNw\",\"BLVKubgVxkF\",\"FGbXmz7gTTl\",\"gmen7SXL9CU\",\"T2Cn45nBY0u\",\"roQ2l7TX0eZ\",\"EmTN0L4EAVi\",\"PLoeN9CaL7z\",\"Vnc2qIRLbyw\",\"wicmjKI3xiP\",\"ncGs9vXS36w\",\"GKrklllwmbU\",\"pXDcgDRz8Od\",\"SmhR2aaKLjw\",\"WjO2puYKysP\",\"FgYDmGwmpEU\",\"mkFoaAdosuY\",\"bVZTNrnfn9G\",\"jCnyQOKQBFX\",\"RUCp6OaTSAD\",\"xWIyicUgscN\",\"YQ3csPLAlrn\",\"vv1QJFONsT6\",\"vxa2YQRGV7I\",\"M721NHGtdZV\",\"Ea3j0kUvyWg\",\"Zwnfm4rnzbZ\",\"wqbyzbQ78oI\",\"KFowGOhmuSL\",\"Q0HywoaWOcM\",\"EoIjKXqXxi2\",\"PfZXxl6Wp3F\",\"GvstqlRRnpV\",\"Tc3zugEWdTm\",\"W2KnxOMvmgE\",\"pUZIL5xBsve\",\"rJ25bHbIujw\",\"wcHRDp21Lw1\",\"fzBpuujglTY\",\"Zbp8TbiMKVc\",\"O63vIA5MVn6\",\"e2WgqiasKnD\",\"qIpBLa1SCZt\",\"s5aXfzOL456\",\"oNqqmKD0zXj\",\"dU3vTbLRLHy\",\"agEKP19IUKI\",\"kEkU53NrFmy\",\"UgYg0YW7ZIh\",\"Fhko00f3hXT\",\"iHQVo7h7KOQ\",\"Cc9kMNFpGmC\",\"mW20aiZHqwE\",\"GHPuYdLcVN5\",\"wy6tbexg2nu\",\"nAH0uNc3b5f\",\"PeyblWrhOwL\",\"kFur7xPhpH9\",\"OuwX8H2CcRO\",\"erqWTArTsyJ\",\"gfWvbbgdjoS\",\"AhnK8hb3JWm\",\"oiSllOTiHNx\",\"BzEwqabuW19\",\"ua5GXy2uhBR\",\"DKZnUSfwjKx\",\"KYXbIQBQgP1\",\"svCLFkT99Yx\",\"CFPrsD3dNeb\",\"SptGAcmbgPz\",\"PQEpIeuSTCN\",\"GGDHb8xd8jc\",\"zw5ppT2dwZy\",\"IFXdzAk7hKi\",\"JrSIoCOdTH2\",\"VFF7f43dJv4\",\"lxxASQqPUqd\",\"K3jhn3TXF3a\",\"RwkdG4Pku2x\",\"iIpPPnnzDo6\",\"uNEhNuBUr0i\",\"xIMxph4NMP1\",\"hjqgB6hEdl3\",\"RJpiHpefEUw\",\"rZkUcho9Z65\",\"bG0PlyD0iP3\",\"lpQvlm9czYE\",\"PdGktj8bAML\",\"gGv9ATEs68L\",\"w9FJ9oAdFys\",\"JCXEtUDYyp9\",\"XQudzejlhJZ\",\"SHLY5rkOFTQ\",\"vSbt6cezomG\",\"Qw7c6Ckb0XC\",\"g5A3hiJlwmI\",\"IXJg79fclDm\",\"mTNOoGXuC39\",\"UugO8xDeLQD\",\"TJA0eGRoRpc\",\"Ykx8Ovui7g0\",\"up9gjdODKXE\",\"qAFXoNjlZCB\",\"GjJjES51GvK\",\"SoXpnYO84eZ\",\"dyn5pihalrJ\",\"wB4tSXlryyO\",\"zCSWBz2pyMd\",\"DlLBIHdpaTy\",\"YPSCWmJ3TyN\",\"m0PiiU5BteW\",\"tZxqVn3xNrA\",\"DXegteybeb5\",\"NqLYdlnK8sc\",\"VhRX5JDVo7R\",\"CKkE4GBJekz\",\"XuGfiry96Bg\",\"Qc9lf4VM9bD\",\"XJ6DqDkMlPv\",\"EUUkKEDoNsf\",\"EDxXfB4iVpY\",\"ui12Hyvn6jR\",\"AtZJOoQiGHd\",\"Qwzs1iinAI7\",\"bW5BaqrBM4K\",\"DVjewuIdgMN\",\"v0dXACseLuB\",\"VSwnkMSAdp7\",\"fPe1l06MurL\",\"IN2dOk0gY1G\",\"XbyObqerCya\",\"AnXoUM1tfNT\",\"sgcHQEaB40Y\",\"nX05QLraDhO\",\"QDoO5r6Sae7\",\"SQz3xtx1Sgr\",\"M9q1wOOsrXp\",\"wxMmC45UyNw\",\"XctPvvWIIcF\",\"MErVkzdbsP5\",\"g031LbUPMmh\",\"dBD9OHJFN8u\",\"sesv0eXljBq\",\"dx4NOnoGtE7\",\"RhJbg8UD75Q\",\"PA1spYiNZfv\",\"EFTcruJcNmZ\",\"QZ5rmKrVleg\",\"qxbsDd9QYv6\",\"esMAQ4vs4kM\",\"YDDOlgRBEAA\",\"fAsj6a4nudH\",\"x5ZxMDvEQUb\",\"TGRCfJEnXJr\",\"xa4F6gesVJm\",\"roGdTjEqLZQ\",\"hDW65lFySeF\",\"Urk55T8KgpT\",\"VdXuxcNkiad\",\"BNFrspDBKel\"],\"pe\":[]}}\n";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentou",
        "Enrollment org unit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentouname",
        "Enrollment org unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "oucode", "OU_559");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentouname", "Ngelehun CHC");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "oucode", "OU_559");
    validateRowValueByName(response, actualHeaders, 3, "enrollmentouname", "Ngelehun CHC");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "oucode", "OU_559");
    validateRowValueByName(response, actualHeaders, 6, "enrollmentouname", "Ngelehun CHC");

    // Validate selected values for row index 9
    validateRowValueByName(response, actualHeaders, 9, "oucode", "OU_559");
    validateRowValueByName(response, actualHeaders, 9, "enrollmentouname", "Ngelehun CHC");
  }

  @Test
  public void enrollmentOuWithUserOrg() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=eventdate")
            .add("headers=oucode,enrollmentou,enrollmentouname")
            .add("displayProperty=NAME")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=ENROLLMENT_OU:USER_ORGUNIT,pe:2021")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);
  }

  @Test
  public void enrollmentOuWithMultipleOus() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=eventdate")
            .add("headers=oucode,enrollmentou,enrollmentouname")
            .add("displayProperty=NAME")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=ENROLLMENT_OU:BXd3TqaAxkK;VpYAl8dXs6m;uFp0ztDOFbI,pe:2021")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        10,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"total\":107,\"pageCount\":11,\"pageSize\":10,\"page\":1},\"items\":{\"enrollmentou\":{\"name\":\"Enrollment org. unit\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"uFp0ztDOFbI\":{\"name\":\"Bendu CHC\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"BXd3TqaAxkK\":{\"name\":\"Sahun (Bumpeh) MCHP\"},\"2021\":{\"name\":\"2021\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"}},\"dimensions\":{\"enrollmentou\":[\"BXd3TqaAxkK\",\"VpYAl8dXs6m\",\"uFp0ztDOFbI\"],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentou",
        "Enrollment org unit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentouname",
        "Enrollment org unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "oucode", "OU_222676");
    validateRowValueByName(
        response, actualHeaders, 0, "enrollmentouname", "Bendoma (Malegohun) MCHP");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "oucode", "OU_222676");
    validateRowValueByName(
        response, actualHeaders, 3, "enrollmentouname", "Bendoma (Malegohun) MCHP");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "oucode", "OU_247031");
    validateRowValueByName(response, actualHeaders, 6, "enrollmentouname", "Sahun (Bumpeh) MCHP");

    // Validate selected values for row index 9
    validateRowValueByName(response, actualHeaders, 9, "oucode", "OU_197430");
    validateRowValueByName(response, actualHeaders, 9, "enrollmentouname", "Bendu CHC");
  }

  @Nested
  class ScheduledDate {
    @Test
    public void scheduledDateAsDimension() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("asc=eventdate")
              .add("headers=oucode,scheduleddate")
              .add("displayProperty=NAME")
              .add("pageSize=10")
              .add("page=1")
              .add("dimension=SCHEDULED_DATE:2021")
              .add("desc=eventdate,lastupdated");

      // When
      ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          10,
          2,
          2); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"total\":12,\"pageSize\":10,\"pageCount\":2},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"pe\":{},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"2021\":{\"name\":\"2021\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"scheduleddate\":{\"name\":\"Scheduled date\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"scheduleddate\":[\"2021\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "oucode",
          "Organisation unit code",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "scheduleddate",
          "Scheduled date",
          "DATETIME",
          "java.time.LocalDateTime",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row values by name at specific indices (sorted results).
      // Validate selected values for row index 0
      validateRowValueByName(response, actualHeaders, 0, "oucode", "OU_559");
      validateRowValueByName(
          response, actualHeaders, 0, "scheduleddate", "2021-07-25 10:55:36.038");

      // Validate selected values for row index 3
      validateRowValueByName(response, actualHeaders, 3, "oucode", "OU_559");
      validateRowValueByName(
          response, actualHeaders, 3, "scheduleddate", "2021-07-23 12:30:39.555");

      // Validate selected values for row index 6
      validateRowValueByName(response, actualHeaders, 6, "oucode", "OU_559");
      validateRowValueByName(
          response, actualHeaders, 6, "scheduleddate", "2021-11-20 16:48:06.834");

      // Validate selected values for row index 9
      validateRowValueByName(response, actualHeaders, 9, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 9, "scheduleddate", "2021-08-05 00:00:00.0");
    }
  }

  @Nested
  class LastUpdated {
    @Test
    public void lastUpdatedAsDimension() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("asc=eventdate")
              .add("headers=oucode,lastupdated")
              .add("displayProperty=NAME")
              .add("pageSize=10")
              .add("page=1")
              .add("dimension=LAST_UPDATED:2017")
              .add("desc=eventdate,lastupdated");

      // When
      ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          10,
          2,
          2); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"total\":10,\"pageSize\":10,\"pageCount\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"2017\":{\"name\":\"2017\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"pe\":{},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"lastupdated\":{\"name\":\"Last updated\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"}},\"dimensions\":{\"pe\":[],\"lastupdated\":[\"2017\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "oucode",
          "Organisation unit code",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "lastupdated",
          "Last updated on",
          "DATETIME",
          "java.time.LocalDateTime",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row values by name at specific indices (sorted results).
      // Validate selected values for row index 0
      validateRowValueByName(response, actualHeaders, 0, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 0, "lastupdated", "2017-07-25 10:55:36.038");

      // Validate selected values for row index 3
      validateRowValueByName(response, actualHeaders, 3, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 3, "lastupdated", "2017-07-23 12:34:46.757");

      // Validate selected values for row index 6
      validateRowValueByName(response, actualHeaders, 6, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 6, "lastupdated", "2017-08-11 00:01:22.963");

      // Validate selected values for row index 9
      validateRowValueByName(response, actualHeaders, 9, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 9, "lastupdated", "2017-11-02 22:57:15.166");
    }
  }

  @Nested
  class Created {
    @Test
    public void createdDateAsDimension() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("asc=eventdate")
              .add("headers=oucode,created")
              .add("displayProperty=NAME")
              .add("pageSize=10")
              .add("page=1")
              .add("dimension=CREATED:2017")
              .add("desc=eventdate,lastupdated");

      // When
      ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          10,
          2,
          2); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"total\":34,\"pageSize\":10,\"pageCount\":4},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"2017\":{\"name\":\"2017\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"pe\":{},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"created\":{\"name\":\"Created\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"}},\"dimensions\":{\"pe\":[],\"created\":[\"2017\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "oucode",
          "Organisation unit code",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "created",
          "Created on",
          "DATETIME",
          "java.time.LocalDateTime",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row values by name at specific indices (sorted results).
      // Validate selected values for row index 0
      validateRowValueByName(response, actualHeaders, 0, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 0, "created", "2017-01-28 08:27:04.865");

      // Validate selected values for row index 3
      validateRowValueByName(response, actualHeaders, 3, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 3, "created", "2017-06-28 16:45:30.069");

      // Validate selected values for row index 6
      validateRowValueByName(response, actualHeaders, 6, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 6, "created", "2017-06-28 15:27:25.429");

      // Validate selected values for row index 9
      validateRowValueByName(response, actualHeaders, 9, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 9, "created", "2017-07-04 20:29:27.906");
    }
  }

  @Nested
  class Completed {
    @Test
    public void completedDateAsDimension() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("asc=eventdate")
              .add("headers=oucode,completed")
              .add("displayProperty=NAME")
              .add("pageSize=10")
              .add("page=1")
              .add("dimension=COMPLETED:2022")
              .add("desc=eventdate,lastupdated");

      // When
      ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          6,
          2,
          2); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"total\":6,\"pageSize\":10,\"pageCount\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"pe\":{},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"2022\":{\"name\":\"2022\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"completed\":{\"name\":\"Completed\"},\"completeddate\":{\"name\":\"Completed date\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"completed\":[\"2022\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "oucode",
          "Organisation unit code",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "completed",
          "Completed on",
          "DATETIME",
          "java.time.LocalDateTime",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row values by name at specific indices (sorted results).
      // Validate selected values for row index 0
      validateRowValueByName(response, actualHeaders, 0, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 0, "completed", "2022-01-01 00:00:00.0");

      // Validate selected values for row index 2
      validateRowValueByName(response, actualHeaders, 2, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 2, "completed", "2022-07-04 00:00:00.0");

      // Validate selected values for row index 4
      validateRowValueByName(response, actualHeaders, 4, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 4, "completed", "2022-07-04 00:00:00.0");

      // Validate selected values for row index 5
      validateRowValueByName(response, actualHeaders, 5, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 5, "completed", "2022-07-04 00:00:00.0");
    }
  }

  @Nested
  class EnrollmentDate {
    @Test
    public void enrollmentDate() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("asc=eventdate")
              .add("headers=oucode,enrollmentdate")
              .add("displayProperty=NAME")
              .add("pageSize=10")
              .add("page=1")
              .add("dimension=ENROLLMENT_DATE:2021")
              .add("desc=eventdate,lastupdated");

      // When
      ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          10,
          2,
          2); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"total\":27,\"pageSize\":10,\"pageCount\":3},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"pe\":{},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"2021\":{\"name\":\"2021\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"enrollmentdate\":{\"name\":\"Start of treatment date\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"enrollmentdate\":[\"2021\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "oucode",
          "Organisation unit code",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentdate",
          "Start of treatment date",
          "DATETIME",
          "java.time.LocalDateTime",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row values by name at specific indices (sorted results).
      // Validate selected values for row index 0
      validateRowValueByName(response, actualHeaders, 0, "oucode", "OU_559");
      validateRowValueByName(
          response, actualHeaders, 0, "enrollmentdate", "2021-11-11 12:27:48.386");

      // Validate selected values for row index 3
      validateRowValueByName(response, actualHeaders, 3, "oucode", "OU_559");
      validateRowValueByName(
          response, actualHeaders, 3, "enrollmentdate", "2021-05-19 12:27:48.317");

      // Validate selected values for row index 6
      validateRowValueByName(response, actualHeaders, 6, "oucode", "OU_559");
      validateRowValueByName(
          response, actualHeaders, 6, "enrollmentdate", "2021-09-11 12:27:48.552");

      // Validate selected values for row index 9
      validateRowValueByName(response, actualHeaders, 9, "oucode", "OU_559");
      validateRowValueByName(
          response, actualHeaders, 9, "enrollmentdate", "2021-05-14 12:35:24.03");
    }

    @Test
    public void enrollmentDateRelativePeriod() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("asc=eventdate")
              .add("headers=oucode,enrollmentdate")
              .add("displayProperty=NAME")
              .add("pageSize=10")
              .add("page=1")
              .add("dimension=ENROLLMENT_DATE:LAST_6_MONTHS")
              .add("relativePeriodDate=2021-11-11")
              .add("desc=eventdate,lastupdated");

      // When
      ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          10,
          2,
          2); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"total\":21,\"pageSize\":10,\"pageCount\":3},\"items\":{\"ou\":{},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"202109\":{\"name\":\"September 2021\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202110\":{\"name\":\"October 2021\"},\"LAST_6_MONTHS\":{\"name\":\"Last 6 months\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"pe\":{},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"enrollmentdate\":{\"name\":\"Start of treatment date\"}},\"dimensions\":{\"ou\":[\"ImspTQPwCqd\"],\"enrollmentdate\":[\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "oucode",
          "Organisation unit code",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentdate",
          "Start of treatment date",
          "DATETIME",
          "java.time.LocalDateTime",
          false,
          true);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row values by name at specific indices (sorted results).
      // Validate selected values for row index 0
      validateRowValueByName(response, actualHeaders, 0, "oucode", "OU_559");
      validateRowValueByName(
          response, actualHeaders, 0, "enrollmentdate", "2021-05-19 12:27:48.317");

      // Validate selected values for row index 3
      validateRowValueByName(response, actualHeaders, 3, "oucode", "OU_559");
      validateRowValueByName(
          response, actualHeaders, 3, "enrollmentdate", "2021-05-14 12:35:24.03");

      // Validate selected values for row index 6
      validateRowValueByName(response, actualHeaders, 6, "oucode", "OU_559");
      validateRowValueByName(
          response, actualHeaders, 6, "enrollmentdate", "2021-10-15 12:34:17.849");

      // Validate selected values for row index 9
      validateRowValueByName(response, actualHeaders, 9, "oucode", "OU_559");
      validateRowValueByName(
          response, actualHeaders, 9, "enrollmentdate", "2021-10-15 12:34:17.849");
    }
  }

  @Nested
  class IncidentDate {
    @Test
    public void incidentDateAsDimension() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("asc=eventdate")
              .add("headers=oucode,incidentdate")
              .add("displayProperty=NAME")
              .add("pageSize=10")
              .add("page=1")
              .add("dimension=INCIDENT_DATE:2021")
              .add("desc=eventdate,lastupdated");

      // When
      ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          10,
          2,
          2); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"total\":12,\"pageSize\":10,\"pageCount\":2},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"pe\":{},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"2021\":{\"name\":\"2021\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"incidentdate\":{\"name\":\"Start of treatment date\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"incidentdate\":[\"2021\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "oucode",
          "Organisation unit code",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "incidentdate",
          "Start of treatment date",
          "DATETIME",
          "java.time.LocalDateTime",
          false,
          true);

      // 7. Assert row values by name at specific indices (sorted results).
      // Validate selected values for row index 0
      validateRowValueByName(response, actualHeaders, 0, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2021-10-22 12:27:48.386");

      // Validate selected values for row index 3
      validateRowValueByName(response, actualHeaders, 3, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 3, "incidentdate", "2022-03-08 12:27:48.401");

      // Validate selected values for row index 6
      validateRowValueByName(response, actualHeaders, 6, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 6, "incidentdate", "2021-10-22 12:27:48.61");

      // Validate selected values for row index 9
      validateRowValueByName(response, actualHeaders, 9, "oucode", "OU_559");
      validateRowValueByName(response, actualHeaders, 9, "incidentdate", "2021-09-10 12:27:48.552");
    }
  }

  @Test
  public void validateStagePrefixedDataElementHeaderWithoutDimension() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=enrollmentouname,A03MvHHogjR.a3kGcGDCuk6")
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ENROLLMENT_OU:jNb63DIHuwU")
            .add("dimension=A03MvHHogjR.EVENT_DATE:THIS_YEAR")
            // .add("dimension=ou:O6uvpzGd5pu")

            .add("relativePeriodDate=2022-12-31")
            .add("totalPages=false");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);
    // Then
    response.validate().statusCode(200).body("headers", hasSize(2));

    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj)
            .collect(Collectors.toList());

    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentouname",
        "Enrollment org unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Row cells must align with the requested headers. A prior regression swapped SQL and grid
    // column ordering so the item value landed in the enrollmentouname cell and vice versa.
    validateRowValueByName(response, actualHeaders, 0, "enrollmentouname", "Baoma Station CHP");
  }

  @Test
  public void verifyDimensionAcceptsOuAndEnrollmentOu() {

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.ou:USER_ORGUNIT")
            .add("dimension=ENROLLMENT_OU:USER_ORGUNIT")
            .add("headers=enrollmentouname,ZkbAXlQUYJG.ouname")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);
    response.validate().statusCode(200).body("headers", hasSize(2));

    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj)
            .collect(Collectors.toList());

    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentouname",
        "Enrollment org unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
  }

  @Test
  public void verifyDimensionAcceptsTeaAttributeAsHeader() {

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=A03MvHHogjR.ou:USER_ORGUNIT")
            .add("headers=cejWyOfXge6,A03MvHHogjR.ouname,A03MvHHogjR.eventdate")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj)
            .collect(Collectors.toList());

    validateHeaderPropertiesByName(
        response, actualHeaders, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    response.validate().statusCode(200).body("headers", hasSize(3));
  }

  @Test
  public void stageAndOuLevelWithBoundary() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.ou:LEVEL-wjP19dkFeIk;ImspTQPwCqd,pe:2022")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        4,
        23,
        19); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":4,\"pageSize\":100,\"pageCount\":1},\"items\":{\"TEQlaapDQoK\":{\"code\":\"OU_254945\",\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"code\":\"OU_268149\",\"name\":\"Tonkolili\"},\"jUb8gELQApl\":{\"code\":\"OU_204856\",\"name\":\"Kailahun\"},\"Vth0fbpFcsO\":{\"code\":\"OU_233310\",\"name\":\"Kono\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"PMa2VCrupOd\":{\"code\":\"OU_211212\",\"name\":\"Kambia\"},\"2022\":{\"name\":\"2022\"},\"bL4ooGhyHRQ\":{\"code\":\"OU_260377\",\"name\":\"Pujehun\"},\"O6uvpzGd5pu\":{\"code\":\"OU_264\",\"name\":\"Bo\"},\"kJq2mPyFEHo\":{\"code\":\"OU_222616\",\"name\":\"Kenema\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"wjP19dkFeIk\":{\"name\":\"District\"},\"fdc6uOvgoji\":{\"code\":\"OU_193190\",\"name\":\"Bombali\"},\"at6UHUQatSo\":{\"code\":\"OU_278310\",\"name\":\"Western Area\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"pe\":{},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ZkbAXlQUYJG.ou\":{\"name\":\"Organisation unit\"},\"lc3eMKXaEfw\":{\"code\":\"OU_197385\",\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"code\":\"OU_226213\",\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"code\":\"OU_246990\",\"name\":\"Moyamba\"}},\"dimensions\":{\"pe\":[],\"ZkbAXlQUYJG.ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "incidentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "tei", "Tracked entity", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pi", "Program instance", "TEXT", "java.lang.String", false, true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);

      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.ou",
        "Organisation unit",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);

    // Assert PostGIS-specific headers existence based on 'expectPostgis' flag
    if (expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", true);
      validateHeaderExistence(actualHeaders, "longitude", true);
      validateHeaderExistence(actualHeaders, "latitude", true);
    } else {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "IQCiAZs7PrK");
    validateRowValueByName(response, actualHeaders, 0, "ZkbAXlQUYJG.ou", "O6uvpzGd5pu");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2021-05-14 12:35:24.03");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2021-04-25 12:35:24.03");
    validateRowValueByName(response, actualHeaders, 0, "tei", "LxMVYhJm3Jp");
    validateRowValueByName(response, actualHeaders, 0, "pi", "awZ5RHoJin5");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "ACTIVE");

    // Validate selected values for row index 1
    validateRowValueByName(response, actualHeaders, 1, "psi", "La2PAKKx3it");
    validateRowValueByName(response, actualHeaders, 1, "ZkbAXlQUYJG.ou", "O6uvpzGd5pu");
    validateRowValueByName(response, actualHeaders, 1, "enrollmentdate", "2022-08-11 12:32:30.524");
    validateRowValueByName(response, actualHeaders, 1, "incidentdate", "2022-08-05 02:00:00.0");
    validateRowValueByName(response, actualHeaders, 1, "tei", "pUK3xmXayQ5");
    validateRowValueByName(response, actualHeaders, 1, "pi", "hXECENVui3x");
    validateRowValueByName(response, actualHeaders, 1, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 1, "programstatus", "ACTIVE");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "psi", "qgaHGxGEI56");
    validateRowValueByName(response, actualHeaders, 2, "ZkbAXlQUYJG.ou", "O6uvpzGd5pu");
    validateRowValueByName(response, actualHeaders, 2, "enrollmentdate", "2023-01-01 01:00:00.0");
    validateRowValueByName(response, actualHeaders, 2, "incidentdate", "2023-01-01 01:00:00.0");
    validateRowValueByName(response, actualHeaders, 2, "tei", "fSofnQR6lAU");
    validateRowValueByName(response, actualHeaders, 2, "pi", "fMCNMupsPrg");
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 2, "programstatus", "CANCELLED");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "psi", "BijwU5PwIMh");
    validateRowValueByName(response, actualHeaders, 3, "ZkbAXlQUYJG.ou", "O6uvpzGd5pu");
    validateRowValueByName(response, actualHeaders, 3, "enrollmentdate", "2023-01-15 01:00:00.0");
    validateRowValueByName(response, actualHeaders, 3, "incidentdate", "2023-01-15 01:00:00.0");
    validateRowValueByName(response, actualHeaders, 3, "tei", "fSofnQR6lAU");
    validateRowValueByName(response, actualHeaders, 3, "pi", "czKU08gniYG");
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 3, "programstatus", "ACTIVE");
  }
}
