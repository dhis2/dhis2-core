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
  public void stageAndInvalidOu() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.ou:THIS_YEAR") // TODO this should fail
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);
  }

  @Test
  public void stageAndSimpleOu() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.ou:ImspTQPwCqd")
            .add("dimension=pe:THIS_YEAR")
            // .add("dimension=ou:ImspTQPwCqd")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // where:
    // (ax."uidlevel1" in ('ImspTQPwCqd') and ax."ps" = 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndOuUserOrgUnit() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            // .add("dimension=ZkbAXlQUYJG.ou:USER_ORGUNIT")
            .add("dimension=pe:THIS_YEAR")
            .add("dimension=ou:USER_ORGUNIT")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // where:
    // (ax."uidlevel1" in ('ImspTQPwCqd') and ax."ps" = 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndOuUserLevel() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.ou:LEVEL-3")
            .add("dimension=pe:THIS_YEAR")
            // .add("dimension=ou:LEVEL-3")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // where:
    // and (ax."uidlevel3" in ('EYt6ThQDagn', 'DNRAeXT9IwS', ...)
    //		and ax."ps" = 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndOuMultipleOus() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.ou:WjO2puYKysP;eIQbndfxQMb")
            .add("dimension=pe:THIS_YEAR")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // where:
    // and ax."uidlevel1" in ('ImspTQPwCqd') and (ax."uidlevel2" in ('eIQbndfxQMb') and
    // ax."uidlevel4" in ('WjO2puYKysP') and ax."ps" = 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndScheduledDate() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZkbAXlQUYJG.SCHEDULED_DATE:201910")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // where:
    // and (ax."scheduleddate" >= '2019-10-01' and ax."scheduleddate" <= '2019-10-31' and ax."ps" =
    // 'ZkbAXlQUYJG')
  }

  @Test
  public void stageAndStatus() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=pe:THIS_YEAR")
            .add("desc=eventdate,lastupdated");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // where:
    // (ax."eventstatus" in ('ACTIVE') and ax."ps" = 'ZkbAXlQUYJG')
  }
}
