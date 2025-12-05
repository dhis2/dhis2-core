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
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/query" endpoint. */
public class EventsQuery6AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  @DisplayName("Validate period dimension with stage-specific date dimension is rejected")
  public void validatePeriodAndStageRejected() {

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=pe:THIS_YEAR")
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
            equalTo(
                "Period dimension cannot be used with stage-specific date dimensions (EVENT_DATE, SCHEDULED_DATE)"))
        .body("errorCode", equalTo("E7242"));
  }

  @Test
  @DisplayName("Validate period dimension with stage-specific ou dimension is not rejected")
  public void validatePeriodAndStageWithOuNotRejected() throws JSONException {
    {
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
          18,
          15); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"pager\":{\"page\":1,\"total\":54022,\"pageSize\":100,\"pageCount\":541},\"items\":{\"Zj7UnCAulEk.ou\":{\"name\":\"ou\"},\"ImspTQPwCqd\":{\"code\":\"OU_525\",\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{\"name\":\"ou\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"Zj7UnCAulEk.ou\":[\"ImspTQPwCqd\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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
          "storedby",
          "Stored by",
          "TEXT",
          "java.lang.String",
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
            response,
            actualHeaders,
            "geometry",
            "Geometry",
            "TEXT",
            "java.lang.String",
            false,
            true);
      }
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
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
          "Zj7UnCAulEk.ou",
          "ou",
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

      // 7. Assert row values by name (sample validation: first/last row, key columns).
      // Validate selected values for row index 0
      validateRowValueByName(response, actualHeaders, 0, "psi", "lfjiNgsMNCR");
      validateRowValueByName(response, actualHeaders, 0, "Zj7UnCAulEk.ou", "Mokorbu MCHP");
      validateRowValueByName(response, actualHeaders, 0, "ouname", "Mokorbu MCHP");
      validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

      // Validate selected values for row index 99
      validateRowValueByName(response, actualHeaders, 99, "psi", "RPNgwvx5j65");
      validateRowValueByName(response, actualHeaders, 99, "Zj7UnCAulEk.ou", "Gbenikoro MCHP");
      validateRowValueByName(response, actualHeaders, 99, "ouname", "Gbenikoro MCHP");
      validateRowValueByName(response, actualHeaders, 99, "programstatus", "");
    }
  }

  @Test
  @DisplayName("Validate period dimension with stage-specific date dimension is rejected")
  public void validateStageAndStageSpecificDimenionRejected() {

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
  public void validateStageAndStageSpecificDimenionRejected2() {

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
        18,
        15); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":54022,\"pageSize\":100,\"pageCount\":541},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{},\"Zj7UnCAulEk.EVENT_DATE\":{\"name\":\"Report date, Inpatient morbidity and mortality\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.occurreddate\":[]}}";
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
        response, actualHeaders, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
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
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentgeometry",
        "Enrollment geometry",
        "TEXT",
        "java.lang.String",
        false,
        true);
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
        "Zj7UnCAulEk.occurreddate",
        "occurreddate",
        "DATE",
        "java.time.LocalDate",
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
        response, actualHeaders, 0, "Zj7UnCAulEk.occurreddate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mokorbu MCHP");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "psi", "RPNgwvx5j65");
    validateRowValueByName(
        response, actualHeaders, 99, "Zj7UnCAulEk.occurreddate", "2022-12-29 00:00:00.0");
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
        18,
        15); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":53768,\"pageSize\":100,\"pageCount\":538},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{},\"Zj7UnCAulEk.EVENT_DATE\":{\"name\":\"Report date, Inpatient morbidity and mortality\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.occurreddate\":[]}}";
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
        response, actualHeaders, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
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
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentgeometry",
        "Enrollment geometry",
        "TEXT",
        "java.lang.String",
        false,
        true);
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
        "Zj7UnCAulEk.occurreddate",
        "occurreddate",
        "DATE",
        "java.time.LocalDate",
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
        response, actualHeaders, 0, "Zj7UnCAulEk.occurreddate", "2021-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Foindu (Lower Bamabara) CHC");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "psi", "Z3rMvGhgfw3");
    validateRowValueByName(
        response, actualHeaders, 99, "Zj7UnCAulEk.occurreddate", "2021-12-29 00:00:00.0");
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
        18,
        15); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":13744,\"pageSize\":100,\"pageCount\":138},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{},\"Zj7UnCAulEk.EVENT_DATE\":{\"name\":\"Report date, Inpatient morbidity and mortality\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.occurreddate\":[]}}";
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
        response, actualHeaders, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
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
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentgeometry",
        "Enrollment geometry",
        "TEXT",
        "java.lang.String",
        false,
        true);
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
        "Zj7UnCAulEk.occurreddate",
        "occurreddate",
        "DATE",
        "java.time.LocalDate",
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
        response, actualHeaders, 0, "Zj7UnCAulEk.occurreddate", "2021-05-31 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Fogbo CHP");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "psi", "e2wE3axQQ5Z");
    validateRowValueByName(
        response, actualHeaders, 99, "Zj7UnCAulEk.occurreddate", "2021-05-31 00:00:00.0");
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
            // .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
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
        18,
        15); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":89750,\"pageSize\":100,\"pageCount\":898},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{},\"Zj7UnCAulEk.EVENT_DATE\":{\"name\":\"Report date, Inpatient morbidity and mortality\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.occurreddate\":[]}}";
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
        response, actualHeaders, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
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
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentgeometry",
        "Enrollment geometry",
        "TEXT",
        "java.lang.String",
        false,
        true);
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
        "Zj7UnCAulEk.occurreddate",
        "occurreddate",
        "DATE",
        "java.time.LocalDate",
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
        response, actualHeaders, 0, "Zj7UnCAulEk.occurreddate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mokorbu MCHP");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "psi", "RPNgwvx5j65");
    validateRowValueByName(
        response, actualHeaders, 99, "Zj7UnCAulEk.occurreddate", "2022-12-29 00:00:00.0");
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
            // .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
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
        18,
        15); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":107790,\"pageSize\":100,\"pageCount\":1078},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{},\"Zj7UnCAulEk.EVENT_DATE\":{\"name\":\"Report date, Inpatient morbidity and mortality\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.occurreddate\":[]}}";
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
        response, actualHeaders, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
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
    }
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentgeometry",
        "Enrollment geometry",
        "TEXT",
        "java.lang.String",
        false,
        true);
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
        "Zj7UnCAulEk.occurreddate",
        "occurreddate",
        "DATE",
        "java.time.LocalDate",
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
        response, actualHeaders, 0, "Zj7UnCAulEk.occurreddate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mokorbu MCHP");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "psi", "RPNgwvx5j65");
    validateRowValueByName(
        response, actualHeaders, 99, "Zj7UnCAulEk.occurreddate", "2022-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 99, "ouname", "Gbenikoro MCHP");
    validateRowValueByName(response, actualHeaders, 99, "programstatus", "");

    // where:
    // (ax."occurreddate" <= '2023-05-01' and ax."ps" = 'Zj7UnCAulEk')
  }

  @Test
  public void stageAndEventDateMultipleStages() throws JSONException {

    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

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
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        4,
        23,
        20); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":4,\"pageSize\":100,\"pageCount\":1},\"items\":{\"ZkbAXlQUYJG.EVENT_DATE\":{\"name\":\"TB visit date, TB visit\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"jdRD35YwbRH.EVENT_DATE\":{\"name\":\"Report date, Sputum smear microscopy test\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"jdRD35YwbRH.occurreddate\":[],\"ZkbAXlQUYJG.occurreddate\":[]}}";
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
        response, actualHeaders, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentgeometry",
        "Enrollment geometry",
        "TEXT",
        "java.lang.String",
        false,
        true);
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
        "jdRD35YwbRH.occurreddate",
        "occurreddate",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZkbAXlQUYJG.occurreddate",
        "occurreddate",
        "DATE",
        "java.time.LocalDate",
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
    validateRowValueByName(response, actualHeaders, 0, "psi", "IQCiAZs7PrK");
    validateRowValueByName(
        response, actualHeaders, 0, "ZkbAXlQUYJG.occurreddate", "2022-07-03 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2021-05-14 12:35:24.03");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2021-04-25 12:35:24.03");
    validateRowValueByName(response, actualHeaders, 0, "tei", "LxMVYhJm3Jp");
    validateRowValueByName(response, actualHeaders, 0, "pi", "awZ5RHoJin5");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "ACTIVE");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "psi", "BijwU5PwIMh");
    validateRowValueByName(
        response, actualHeaders, 3, "ZkbAXlQUYJG.occurreddate", "2022-01-01 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 3, "enrollmentdate", "2023-01-15 01:00:00.0");
    validateRowValueByName(response, actualHeaders, 3, "incidentdate", "2023-01-15 01:00:00.0");
    validateRowValueByName(response, actualHeaders, 3, "tei", "fSofnQR6lAU");
    validateRowValueByName(response, actualHeaders, 3, "pi", "czKU08gniYG");
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 3, "programstatus", "ACTIVE");
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
        22,
        19); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":4,\"pageSize\":100,\"pageCount\":1},\"items\":{\"ImspTQPwCqd\":{\"code\":\"OU_525\",\"name\":\"Sierra Leone\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{\"name\":\"ou\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"ZkbAXlQUYJG.ou\":{\"name\":\"ou\"},\"2022\":{\"name\":\"2022\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"ZkbAXlQUYJG.ou\":[\"ImspTQPwCqd\"]}}";
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
        response, actualHeaders, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentgeometry",
        "Enrollment geometry",
        "TEXT",
        "java.lang.String",
        false,
        true);
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
        "ZkbAXlQUYJG.ou",
        "ou",
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

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "IQCiAZs7PrK");
    validateRowValueByName(response, actualHeaders, 0, "ZkbAXlQUYJG.ou", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2021-05-14 12:35:24.03");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2021-04-25 12:35:24.03");
    validateRowValueByName(response, actualHeaders, 0, "tei", "LxMVYhJm3Jp");
    validateRowValueByName(response, actualHeaders, 0, "pi", "awZ5RHoJin5");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "ACTIVE");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "psi", "BijwU5PwIMh");
    validateRowValueByName(response, actualHeaders, 3, "ZkbAXlQUYJG.ou", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 3, "enrollmentdate", "2023-01-15 01:00:00.0");
    validateRowValueByName(response, actualHeaders, 3, "incidentdate", "2023-01-15 01:00:00.0");
    validateRowValueByName(response, actualHeaders, 3, "tei", "fSofnQR6lAU");
    validateRowValueByName(response, actualHeaders, 3, "pi", "czKU08gniYG");
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 3, "programstatus", "ACTIVE");
    // where:
    // (ax."uidlevel1" in ('ImspTQPwCqd') and ax."ps" = 'ZkbAXlQUYJG')
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
        22,
        19); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":1,\"pageSize\":100,\"pageCount\":1},\"items\":{\"ImspTQPwCqd\":{\"code\":\"OU_525\",\"name\":\"Sierra Leone\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"name\":\"ou\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"ZkbAXlQUYJG.ou\":{\"name\":\"ou\"},\"202206\":{\"name\":\"June 2022\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"ZkbAXlQUYJG.ou\":[\"ImspTQPwCqd\"]}}";
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
        response, actualHeaders, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentgeometry",
        "Enrollment geometry",
        "TEXT",
        "java.lang.String",
        false,
        true);
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
        "ZkbAXlQUYJG.ou",
        "ou",
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

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "La2PAKKx3it");
    validateRowValueByName(response, actualHeaders, 0, "ZkbAXlQUYJG.ou", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2022-08-11 12:32:30.524");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2022-08-05 02:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "tei", "pUK3xmXayQ5");
    validateRowValueByName(response, actualHeaders, 0, "pi", "hXECENVui3x");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "ACTIVE");
    // where:
    // (ax."uidlevel1" in ('ImspTQPwCqd') and ax."ps" = 'ZkbAXlQUYJG')
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
        22,
        19); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":2,\"pageSize\":100,\"pageCount\":1},\"items\":{\"yu4N82FFeLm\":{\"code\":\"OU_204910\",\"name\":\"Mandu\"},\"KXSqt7jv6DU\":{\"code\":\"OU_222627\",\"name\":\"Gorama Mende\"},\"lY93YpCxJqf\":{\"code\":\"OU_193249\",\"name\":\"Makari Gbanti\"},\"eROJsBwxQHt\":{\"code\":\"OU_222743\",\"name\":\"Gaura\"},\"DxAPPqXvwLy\":{\"code\":\"OU_204929\",\"name\":\"Peje Bongre\"},\"PaqugoqjRIj\":{\"code\":\"OU_226225\",\"name\":\"Sulima (Koinadugu)\"},\"gy8rmvYT4cj\":{\"code\":\"OU_247037\",\"name\":\"Ribbi\"},\"RzKeCma9qb1\":{\"code\":\"OU_260428\",\"name\":\"Barri\"},\"vULnao2hV5v\":{\"code\":\"OU_247086\",\"name\":\"Fakunya\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"EfWCa0Cc8WW\":{\"code\":\"OU_255030\",\"name\":\"Masimera\"},\"AovmOHadayb\":{\"code\":\"OU_247044\",\"name\":\"Timidale\"},\"hjpHnHZIniP\":{\"code\":\"OU_204887\",\"name\":\"Kissi Tongi\"},\"U6Kr7Gtpidn\":{\"code\":\"OU_546\",\"name\":\"Kakua\"},\"EYt6ThQDagn\":{\"code\":\"OU_222642\",\"name\":\"Koya (kenema)\"},\"iUauWFeH8Qp\":{\"code\":\"OU_197402\",\"name\":\"Bum\"},\"Jiyc4ekaMMh\":{\"code\":\"OU_247080\",\"name\":\"Kongbora\"},\"FlBemv1NfEC\":{\"code\":\"OU_211256\",\"name\":\"Masungbala\"},\"XrF5AvaGcuw\":{\"code\":\"OU_226240\",\"name\":\"Wara Wara Bafodia\"},\"LhaAPLxdSFH\":{\"code\":\"OU_233348\",\"name\":\"Lei\"},\"BmYyh9bZ0sr\":{\"code\":\"OU_268197\",\"name\":\"Kafe Simira\"},\"pk7bUK5c1Uf\":{\"code\":\"OU_260397\",\"name\":\"Ya Kpukumu Krim\"},\"zSNUViKdkk3\":{\"code\":\"OU_260440\",\"name\":\"Kpaka\"},\"r06ohri9wA9\":{\"code\":\"OU_211243\",\"name\":\"Samu\"},\"ERmBhYkhV6Y\":{\"code\":\"OU_204877\",\"name\":\"Njaluahun\"},\"Z9QaI6sxTwW\":{\"code\":\"OU_247068\",\"name\":\"Kargboro\"},\"daJPPxtIrQn\":{\"code\":\"OU_545\",\"name\":\"Jaiama Bongor\"},\"W5fN3G6y1VI\":{\"code\":\"OU_247012\",\"name\":\"Lower Banta\"},\"r1RUyfVBkLp\":{\"code\":\"OU_268169\",\"name\":\"Sambaia Bendugu\"},\"NNE0YMCDZkO\":{\"code\":\"OU_268225\",\"name\":\"Yoni\"},\"ENHOJz3UH5L\":{\"code\":\"OU_197440\",\"name\":\"BMC\"},\"QywkxFudXrC\":{\"code\":\"OU_211227\",\"name\":\"Magbema\"},\"jWSIbtKfURj\":{\"code\":\"OU_222751\",\"name\":\"Langrama\"},\"J4GiUImJZoE\":{\"code\":\"OU_226269\",\"name\":\"Nieni\"},\"CF243RPvNY7\":{\"code\":\"OU_233359\",\"name\":\"Fiama\"},\"I4jWcnFmgEC\":{\"code\":\"OU_549\",\"name\":\"Niawa Lenga\"},\"cM2BKSrj9F9\":{\"code\":\"OU_204894\",\"name\":\"Luawa\"},\"kvkDWg42lHR\":{\"code\":\"OU_233339\",\"name\":\"Kamara\"},\"jPidqyo7cpF\":{\"code\":\"OU_247049\",\"name\":\"Bagruwa\"},\"BGGmAwx33dj\":{\"code\":\"OU_543\",\"name\":\"Bumpe Ngao\"},\"iGHlidSFdpu\":{\"code\":\"OU_233317\",\"name\":\"Soa\"},\"g8DdBm7EmUt\":{\"code\":\"OU_197397\",\"name\":\"Sittia\"},\"ZiOVcrSjSYe\":{\"code\":\"OU_254976\",\"name\":\"Dibia\"},\"vn9KJsLyP5f\":{\"code\":\"OU_255005\",\"name\":\"Kaffu Bullom\"},\"QlCIp2S9NHs\":{\"code\":\"OU_222682\",\"name\":\"Dodo\"},\"j43EZb15rjI\":{\"code\":\"OU_193285\",\"name\":\"Sella Limba\"},\"bQiBfA2j5cw\":{\"code\":\"OU_204857\",\"name\":\"Penguia\"},\"NqWaKXcg01b\":{\"code\":\"OU_260384\",\"name\":\"Sowa\"},\"VP397wRvePm\":{\"code\":\"OU_197445\",\"name\":\"Nongoba Bullum\"},\"fwxkctgmffZ\":{\"code\":\"OU_268163\",\"name\":\"Kholifa Mabang\"},\"QwMiPiME3bA\":{\"code\":\"OU_260400\",\"name\":\"Kpanga Kabonde\"},\"Qhmi8IZyPyD\":{\"code\":\"OU_193245\",\"name\":\"Tambaka\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"OTFepb1k9Db\":{\"code\":\"OU_226244\",\"name\":\"Mongo\"},\"DBs6e2Oxaj1\":{\"code\":\"OU_247002\",\"name\":\"Upper Banta\"},\"ZkbAXlQUYJG.ou\":{\"name\":\"ou\"},\"eV4cuxniZgP\":{\"code\":\"OU_193224\",\"name\":\"Magbaimba Ndowahun\"},\"xhyjU2SVewz\":{\"code\":\"OU_268217\",\"name\":\"Tane\"},\"dGheVylzol6\":{\"code\":\"OU_541\",\"name\":\"Bargbe\"},\"vWbkYPRmKyS\":{\"code\":\"OU_540\",\"name\":\"Baoma\"},\"npWGUj37qDe\":{\"code\":\"OU_552\",\"name\":\"Valunia\"},\"TA7NvKjsn4A\":{\"code\":\"OU_255041\",\"name\":\"Bureh Kasseh Maconteh\"},\"myQ4q1W6B4y\":{\"code\":\"OU_222731\",\"name\":\"Dama\"},\"nV3OkyzF4US\":{\"code\":\"OU_246991\",\"name\":\"Kori\"},\"X7dWcGerQIm\":{\"code\":\"OU_222677\",\"name\":\"Wandor\"},\"qIRCo0MfuGb\":{\"code\":\"OU_211213\",\"name\":\"Gbinleh Dixion\"},\"kbPmt60yi0L\":{\"code\":\"OU_211220\",\"name\":\"Bramaia\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"eNtRuQrrZeo\":{\"code\":\"OU_260420\",\"name\":\"Galliness Perri\"},\"HV8RTzgcFH3\":{\"code\":\"OU_197432\",\"name\":\"Kwamabai Krim\"},\"KKkLOTpMXGV\":{\"code\":\"OU_193198\",\"name\":\"Bombali Sebora\"},\"Pc3JTyqnsmL\":{\"code\":\"OU_255020\",\"name\":\"Buya Romende\"},\"hdEuw2ugkVF\":{\"code\":\"OU_222652\",\"name\":\"Lower Bambara\"},\"l7pFejMtUoF\":{\"code\":\"OU_222634\",\"name\":\"Tunkia\"},\"K1r3uF6eZ8n\":{\"code\":\"OU_222725\",\"name\":\"Kandu Lepiema\"},\"VGAFxBXz16y\":{\"code\":\"OU_226231\",\"name\":\"Sengbeh\"},\"GE25DpSrqpB\":{\"code\":\"OU_204869\",\"name\":\"Malema\"},\"ARZ4y5i4reU\":{\"code\":\"OU_553\",\"name\":\"Wonde\"},\"byp7w6Xd9Df\":{\"code\":\"OU_204933\",\"name\":\"Yawei\"},\"BXJdOLvUrZB\":{\"code\":\"OU_193277\",\"name\":\"Gbendembu Ngowahun\"},\"uKC54fzxRzO\":{\"code\":\"OU_222648\",\"name\":\"Niawa\"},\"TQkG0sX9nca\":{\"code\":\"OU_233375\",\"name\":\"Gbense\"},\"hRZOIgQ0O1m\":{\"code\":\"OU_193302\",\"name\":\"Libeisaygahun\"},\"PrJQHI6q7w2\":{\"code\":\"OU_255061\",\"name\":\"Tainkatopa Makama Safrokoh\"},\"USQdmvrHh1Q\":{\"code\":\"OU_247055\",\"name\":\"Kaiyamba\"},\"xGMGhjA3y6J\":{\"code\":\"OU_211262\",\"name\":\"Mambolo\"},\"nOYt1LtFSyU\":{\"code\":\"OU_247025\",\"name\":\"Bumpeh\"},\"e1eIKM1GIF3\":{\"code\":\"OU_193215\",\"name\":\"Gbanti Kamaranka\"},\"lYIM1MXbSYS\":{\"code\":\"OU_204920\",\"name\":\"Dea\"},\"sxRd2XOzFbz\":{\"code\":\"OU_551\",\"name\":\"Tikonko\"},\"d9iMR1MpuIO\":{\"code\":\"OU_260410\",\"name\":\"Soro-Gbeima\"},\"qgQ49DH9a0v\":{\"code\":\"OU_233332\",\"name\":\"Nimiyama\"},\"U09TSwIjG0s\":{\"code\":\"OU_222617\",\"name\":\"Nomo\"},\"zFDYIgyGmXG\":{\"code\":\"OU_542\",\"name\":\"Bargbo\"},\"M2qEv692lS6\":{\"code\":\"OU_233324\",\"name\":\"Tankoro\"},\"qtr8GGlm4gg\":{\"code\":\"OU_278366\",\"name\":\"Rural Western Area\"},\"pRHGAROvuyI\":{\"code\":\"OU_254960\",\"name\":\"Koya\"},\"xIKjidMrico\":{\"code\":\"OU_247033\",\"name\":\"Kowa\"},\"GWTIxJO9pRo\":{\"code\":\"OU_233355\",\"name\":\"Gorama Kono\"},\"HWjrSuoNPte\":{\"code\":\"OU_254999\",\"name\":\"Sanda Magbolonthor\"},\"UhHipWG7J8b\":{\"code\":\"OU_193191\",\"name\":\"Sanda Tendaren\"},\"rXLor9Knq6l\":{\"code\":\"OU_268212\",\"name\":\"Kunike Barina\"},\"kU8vhUkAGaT\":{\"code\":\"OU_548\",\"name\":\"Lugbu\"},\"C9uduqDZr9d\":{\"code\":\"OU_278311\",\"name\":\"Freetown\"},\"YmmeuGbqOwR\":{\"code\":\"OU_544\",\"name\":\"Gbo\"},\"iEkBZnMDarP\":{\"code\":\"OU_226253\",\"name\":\"Folosaba Dembelia\"},\"JsxnA2IywRo\":{\"code\":\"OU_204875\",\"name\":\"Kissi Kama\"},\"nlt6j60tCHF\":{\"code\":\"OU_260437\",\"name\":\"Mano Sakrim\"},\"g5ptsn0SFX8\":{\"code\":\"OU_233365\",\"name\":\"Sandor\"},\"XG8HGAbrbbL\":{\"code\":\"OU_193267\",\"name\":\"Safroko Limba\"},\"pmxZm7klXBy\":{\"code\":\"OU_204924\",\"name\":\"Peje West\"},\"l0ccv2yzfF3\":{\"code\":\"OU_268174\",\"name\":\"Kunike\"},\"YuQRtpLP10I\":{\"code\":\"OU_539\",\"name\":\"Badjia\"},\"LfTkc0S4b5k\":{\"code\":\"OU_204915\",\"name\":\"Upper Bambara\"},\"KSdZwrU7Hh6\":{\"code\":\"OU_204861\",\"name\":\"Jawi\"},\"ajILkI0cfxn\":{\"code\":\"OU_233390\",\"name\":\"Gbane\"},\"y5X4mP5XylL\":{\"code\":\"OU_211270\",\"name\":\"Tonko Limba\"},\"fwH9ipvXde9\":{\"code\":\"OU_193228\",\"name\":\"Biriwa\"},\"KIUCimTXf8Q\":{\"code\":\"OU_222690\",\"name\":\"Nongowa\"},\"vEvs2ckGNQj\":{\"code\":\"OU_226219\",\"name\":\"Kasonko\"},\"XEyIRFd9pct\":{\"code\":\"OU_197413\",\"name\":\"Imperi\"},\"cgOy0hRMGu9\":{\"code\":\"OU_197408\",\"name\":\"Sogbini\"},\"EjnIQNVAXGp\":{\"code\":\"OU_233344\",\"name\":\"Mafindor\"},\"x4HaBHHwBML\":{\"code\":\"OU_222672\",\"name\":\"Malegohun\"},\"EZPwuUTeIIG\":{\"code\":\"OU_226258\",\"name\":\"Wara Wara Yagala\"},\"N233eZJZ1bh\":{\"code\":\"OU_260388\",\"name\":\"Pejeh\"},\"smoyi1iYNK6\":{\"code\":\"OU_268191\",\"name\":\"Kalansogoia\"},\"DNRAeXT9IwS\":{\"code\":\"OU_197421\",\"name\":\"Dema\"},\"fRLX08WHWpL\":{\"code\":\"OU_254982\",\"name\":\"Lokomasama\"},\"Zoy23SSHCPs\":{\"code\":\"OU_233311\",\"name\":\"Gbane Kandor\"},\"LsYpCyYxSLY\":{\"code\":\"OU_247008\",\"name\":\"Kamaje\"},\"JdqfYTIFZXN\":{\"code\":\"OU_254946\",\"name\":\"Maforki\"},\"EB1zRKdYjdY\":{\"code\":\"OU_197429\",\"name\":\"Bendu Cha\"},\"CG4QD1HC3h4\":{\"code\":\"OU_197436\",\"name\":\"Yawbeko\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202111\":{\"name\":\"November 2021\"},\"YpVol7asWvd\":{\"code\":\"OU_260417\",\"name\":\"Kpanga Krim\"},\"RndxKqQGzUl\":{\"code\":\"OU_247018\",\"name\":\"Dasse\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"P69SId31eDp\":{\"code\":\"OU_268202\",\"name\":\"Gbonkonlenken\"},\"aWQTfvgPA5v\":{\"code\":\"OU_197424\",\"name\":\"Kpanda Kemoh\"},\"KctpIIucige\":{\"code\":\"OU_550\",\"name\":\"Selenga\"},\"Mr4au3jR9bt\":{\"code\":\"OU_226214\",\"name\":\"Dembelia Sinkunia\"},\"L8iA6eLwKNb\":{\"code\":\"OU_193295\",\"name\":\"Paki Masabong\"},\"j0Mtr3xTMjM\":{\"code\":\"OU_204939\",\"name\":\"Kissi Teng\"},\"DmaLM8WYmWv\":{\"code\":\"OU_233394\",\"name\":\"Nimikoro\"},\"DfUfwjM9am5\":{\"code\":\"OU_260392\",\"name\":\"Malen\"},\"ou\":{\"name\":\"ou\"},\"FRxcUEwktoV\":{\"code\":\"OU_233314\",\"name\":\"Toli\"},\"VCtF1DbspR5\":{\"code\":\"OU_197386\",\"name\":\"Jong\"},\"BD9gU0GKlr2\":{\"code\":\"OU_260378\",\"name\":\"Makpele\"},\"GFk45MOxzJJ\":{\"code\":\"OU_226275\",\"name\":\"Neya\"},\"A3Fh37HWBWE\":{\"code\":\"OU_222687\",\"name\":\"Simbaru\"},\"vzup1f6ynON\":{\"code\":\"OU_222619\",\"name\":\"Small Bo\"},\"Lt8U7GVWvSR\":{\"code\":\"OU_226263\",\"name\":\"Diang\"},\"JdhagCUEMbj\":{\"code\":\"OU_547\",\"name\":\"Komboya\"},\"EVkm2xYcf6Z\":{\"code\":\"OU_268184\",\"name\":\"Malal Mara\"},\"PQZJPIpTepd\":{\"code\":\"OU_268150\",\"name\":\"Kholifa Rowalla\"},\"WXnNDWTiE9r\":{\"code\":\"OU_193239\",\"name\":\"Sanda Loko\"},\"RWvG1aFrr0r\":{\"code\":\"OU_255053\",\"name\":\"Marampa\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"ZkbAXlQUYJG.ou\":[\"ENHOJz3UH5L\",\"YuQRtpLP10I\",\"jPidqyo7cpF\",\"vWbkYPRmKyS\",\"dGheVylzol6\",\"zFDYIgyGmXG\",\"RzKeCma9qb1\",\"EB1zRKdYjdY\",\"fwH9ipvXde9\",\"KKkLOTpMXGV\",\"kbPmt60yi0L\",\"iUauWFeH8Qp\",\"BGGmAwx33dj\",\"nOYt1LtFSyU\",\"TA7NvKjsn4A\",\"Pc3JTyqnsmL\",\"myQ4q1W6B4y\",\"RndxKqQGzUl\",\"lYIM1MXbSYS\",\"DNRAeXT9IwS\",\"Mr4au3jR9bt\",\"Lt8U7GVWvSR\",\"ZiOVcrSjSYe\",\"QlCIp2S9NHs\",\"vULnao2hV5v\",\"CF243RPvNY7\",\"iEkBZnMDarP\",\"C9uduqDZr9d\",\"eNtRuQrrZeo\",\"eROJsBwxQHt\",\"ajILkI0cfxn\",\"Zoy23SSHCPs\",\"e1eIKM1GIF3\",\"BXJdOLvUrZB\",\"TQkG0sX9nca\",\"qIRCo0MfuGb\",\"YmmeuGbqOwR\",\"P69SId31eDp\",\"GWTIxJO9pRo\",\"KXSqt7jv6DU\",\"XEyIRFd9pct\",\"daJPPxtIrQn\",\"KSdZwrU7Hh6\",\"VCtF1DbspR5\",\"BmYyh9bZ0sr\",\"vn9KJsLyP5f\",\"USQdmvrHh1Q\",\"U6Kr7Gtpidn\",\"smoyi1iYNK6\",\"LsYpCyYxSLY\",\"kvkDWg42lHR\",\"K1r3uF6eZ8n\",\"Z9QaI6sxTwW\",\"vEvs2ckGNQj\",\"fwxkctgmffZ\",\"PQZJPIpTepd\",\"JsxnA2IywRo\",\"j0Mtr3xTMjM\",\"hjpHnHZIniP\",\"JdhagCUEMbj\",\"Jiyc4ekaMMh\",\"nV3OkyzF4US\",\"xIKjidMrico\",\"pRHGAROvuyI\",\"EYt6ThQDagn\",\"zSNUViKdkk3\",\"aWQTfvgPA5v\",\"QwMiPiME3bA\",\"YpVol7asWvd\",\"l0ccv2yzfF3\",\"rXLor9Knq6l\",\"HV8RTzgcFH3\",\"jWSIbtKfURj\",\"LhaAPLxdSFH\",\"hRZOIgQ0O1m\",\"fRLX08WHWpL\",\"hdEuw2ugkVF\",\"W5fN3G6y1VI\",\"cM2BKSrj9F9\",\"kU8vhUkAGaT\",\"EjnIQNVAXGp\",\"JdqfYTIFZXN\",\"eV4cuxniZgP\",\"QywkxFudXrC\",\"lY93YpCxJqf\",\"BD9gU0GKlr2\",\"EVkm2xYcf6Z\",\"x4HaBHHwBML\",\"GE25DpSrqpB\",\"DfUfwjM9am5\",\"xGMGhjA3y6J\",\"yu4N82FFeLm\",\"nlt6j60tCHF\",\"RWvG1aFrr0r\",\"EfWCa0Cc8WW\",\"FlBemv1NfEC\",\"OTFepb1k9Db\",\"GFk45MOxzJJ\",\"uKC54fzxRzO\",\"I4jWcnFmgEC\",\"J4GiUImJZoE\",\"DmaLM8WYmWv\",\"qgQ49DH9a0v\",\"ERmBhYkhV6Y\",\"U09TSwIjG0s\",\"VP397wRvePm\",\"KIUCimTXf8Q\",\"L8iA6eLwKNb\",\"DxAPPqXvwLy\",\"pmxZm7klXBy\",\"N233eZJZ1bh\",\"bQiBfA2j5cw\",\"gy8rmvYT4cj\",\"qtr8GGlm4gg\",\"XG8HGAbrbbL\",\"r1RUyfVBkLp\",\"r06ohri9wA9\",\"WXnNDWTiE9r\",\"HWjrSuoNPte\",\"UhHipWG7J8b\",\"g5ptsn0SFX8\",\"KctpIIucige\",\"j43EZb15rjI\",\"VGAFxBXz16y\",\"A3Fh37HWBWE\",\"g8DdBm7EmUt\",\"vzup1f6ynON\",\"iGHlidSFdpu\",\"cgOy0hRMGu9\",\"d9iMR1MpuIO\",\"NqWaKXcg01b\",\"PaqugoqjRIj\",\"PrJQHI6q7w2\",\"Qhmi8IZyPyD\",\"xhyjU2SVewz\",\"M2qEv692lS6\",\"sxRd2XOzFbz\",\"AovmOHadayb\",\"FRxcUEwktoV\",\"y5X4mP5XylL\",\"l7pFejMtUoF\",\"LfTkc0S4b5k\",\"DBs6e2Oxaj1\",\"npWGUj37qDe\",\"X7dWcGerQIm\",\"XrF5AvaGcuw\",\"EZPwuUTeIIG\",\"ARZ4y5i4reU\",\"pk7bUK5c1Uf\",\"CG4QD1HC3h4\",\"byp7w6Xd9Df\",\"NNE0YMCDZkO\"]}}";
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
        response, actualHeaders, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentgeometry",
        "Enrollment geometry",
        "TEXT",
        "java.lang.String",
        false,
        true);
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
        "ZkbAXlQUYJG.ou",
        "ou",
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

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "o6G3PSfXK8L");
    validateRowValueByName(response, actualHeaders, 0, "ZkbAXlQUYJG.ou", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2021-11-20 12:27:48.415");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2021-10-29 12:27:48.415");
    validateRowValueByName(response, actualHeaders, 0, "tei", "fSofnQR6lAU");
    validateRowValueByName(response, actualHeaders, 0, "pi", "Cl7ZhgxaYQO");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "CANCELLED");

    // Validate selected values for row index 1
    validateRowValueByName(response, actualHeaders, 1, "psi", "TAZ4L5XN1oD");
    validateRowValueByName(response, actualHeaders, 1, "ZkbAXlQUYJG.ou", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 1, "enrollmentdate", "2021-09-11 12:27:48.552");
    validateRowValueByName(response, actualHeaders, 1, "incidentdate", "2021-09-10 12:27:48.552");
    validateRowValueByName(response, actualHeaders, 1, "tei", "uh47DXf1St9");
    validateRowValueByName(response, actualHeaders, 1, "pi", "iSNBeFcHO0X");
    validateRowValueByName(response, actualHeaders, 1, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 1, "programstatus", "ACTIVE");

    // where:
    // and (ax."uidlevel3" in ('EYt6ThQDagn', 'DNRAeXT9IwS', ...)
    //		and ax."ps" = 'ZkbAXlQUYJG')
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
        22,
        19); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":1,\"pageSize\":100,\"pageCount\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"ZkbAXlQUYJG.SCHEDULED_DATE\":{\"name\":\"scheduleddate, TB visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"ZkbAXlQUYJG.scheduleddate\":[]}}";
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
        response, actualHeaders, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentgeometry",
        "Enrollment geometry",
        "TEXT",
        "java.lang.String",
        false,
        true);
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
        "ZkbAXlQUYJG.scheduleddate",
        "scheduleddate",
        "DATE",
        "java.time.LocalDate",
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
        22,
        19); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":1,\"pageSize\":100,\"pageCount\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"ZkbAXlQUYJG.EVENT_STATUS\":{\"name\":\"Event status, TB visit\"},\"2021\":{\"name\":\"2021\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"ZkbAXlQUYJG.eventstatus\":[]}}";
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
        response, actualHeaders, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentgeometry",
        "Enrollment geometry",
        "TEXT",
        "java.lang.String",
        false,
        true);
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
        "ZkbAXlQUYJG.eventstatus",
        "eventstatus",
        "TEXT",
        "java.lang.String",
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
}
