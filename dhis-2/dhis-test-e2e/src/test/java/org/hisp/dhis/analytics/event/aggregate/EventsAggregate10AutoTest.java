/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.event.aggregate;

import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderExistence;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowExists;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/aggregate" endpoint. */
public class EventsAggregate10AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Nested
  @DisplayName("Scheduled date")
  class ScheduledDate {
    @Test
    @DisplayName("Events Aggregate - Relative period - Time field: scheduledDate")
    public void eventAggregateWithRelativePeriodTimeField() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("stage=A03MvHHogjR")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("scheduledDate=LAST_5_FINANCIAL_YEARS")
              .add("outputType=EVENT")
              .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6")
              .add("relativePeriodDate=2025-09-29");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          19,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"items\":{\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"2020Oct\":{\"name\":\"October 2020 - September 2021\"},\"pe\":{},\"2019Oct\":{\"name\":\"October 2019 - September 2020\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2023Oct\":{\"name\":\"October 2023 - September 2024\"},\"2022Oct\":{\"name\":\"October 2022 - September 2023\"},\"2021Oct\":{\"name\":\"October 2021 - September 2022\"},\"scheduleddate\":{\"name\":\"Scheduled date\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"scheduleddate\":[\"2019Oct\",\"2020Oct\",\"2021Oct\",\"2022Oct\",\"2023Oct\"],\"pe\":[\"2019Oct\",\"2020Oct\",\"2021Oct\",\"2022Oct\",\"2023Oct\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "A03MvHHogjR.a3kGcGDCuk6",
          "MCH Apgar Score",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "scheduleddate",
          "Scheduled date",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "",
              "ou",
              "ImspTQPwCqd",
              "scheduleddate",
              "2021Oct",
              "value",
              "2"));

      // Validate row exists with values from original row index 4
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "11.0",
              "ou",
              "ImspTQPwCqd",
              "scheduleddate",
              "2021Oct",
              "value",
              "2"));

      // Validate row exists with values from original row index 8
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "10.0",
              "ou",
              "ImspTQPwCqd",
              "scheduleddate",
              "2021Oct",
              "value",
              "1"));

      // Validate row exists with values from original row index 12
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "2.0",
              "ou",
              "ImspTQPwCqd",
              "scheduleddate",
              "2020Oct",
              "value",
              "2762"));

      // Validate row exists with values from original row index 16
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "0.0",
              "ou",
              "ImspTQPwCqd",
              "scheduleddate",
              "2020Oct",
              "value",
              "2715"));

      // Validate row exists with values from original row index 18
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "8.0",
              "ou",
              "ImspTQPwCqd",
              "scheduleddate",
              "2021Oct",
              "value",
              "5"));
    }

    @Test
    @DisplayName("Events Aggregate - Relative period - SCHEDULED_DATE as dimension")
    public void eventAggregateWithRelativePeriodTimeFieldDim() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("stage=A03MvHHogjR")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add(
                  "dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6,SCHEDULED_DATE:LAST_5_FINANCIAL_YEARS")
              .add("relativePeriodDate=2025-09-29");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          19,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"items\":{\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"2020Oct\":{\"name\":\"October 2020 - September 2021\"},\"pe\":{},\"2019Oct\":{\"name\":\"October 2019 - September 2020\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2023Oct\":{\"name\":\"October 2023 - September 2024\"},\"2022Oct\":{\"name\":\"October 2022 - September 2023\"},\"2021Oct\":{\"name\":\"October 2021 - September 2022\"},\"scheduleddate\":{\"name\":\"Scheduled date\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"scheduleddate\":[\"2019Oct\",\"2020Oct\",\"2021Oct\",\"2022Oct\",\"2023Oct\"],\"pe\":[\"2019Oct\",\"2020Oct\",\"2021Oct\",\"2022Oct\",\"2023Oct\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "A03MvHHogjR.a3kGcGDCuk6",
          "MCH Apgar Score",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "scheduleddate",
          "Scheduled date",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "",
              "ou",
              "ImspTQPwCqd",
              "scheduleddate",
              "2021Oct",
              "value",
              "2"));

      // Validate row exists with values from original row index 4
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "11.0",
              "ou",
              "ImspTQPwCqd",
              "scheduleddate",
              "2021Oct",
              "value",
              "2"));

      // Validate row exists with values from original row index 8
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "10.0",
              "ou",
              "ImspTQPwCqd",
              "scheduleddate",
              "2021Oct",
              "value",
              "1"));

      // Validate row exists with values from original row index 12
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "2.0",
              "ou",
              "ImspTQPwCqd",
              "scheduleddate",
              "2020Oct",
              "value",
              "2762"));

      // Validate row exists with values from original row index 16
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "0.0",
              "ou",
              "ImspTQPwCqd",
              "scheduleddate",
              "2020Oct",
              "value",
              "2715"));

      // Validate row exists with values from original row index 18
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "8.0",
              "ou",
              "ImspTQPwCqd",
              "scheduleddate",
              "2021Oct",
              "value",
              "5"));
    }
  }

  @Nested
  @DisplayName("Enrollment date")
  class EnrollmentDate {
    @Test
    @DisplayName("Events Aggregate - Fixed periods with gaps - Time field: enrollmentDate")
    public void eventAggregateWithFixedPeriodTimeField() throws JSONException {
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("stage=A03MvHHogjR")
              .add("displayProperty=NAME")
              .add("enrollmentDate=202301,202402")
              .add("totalPages=false")
              .add("outputType=EVENT")
              .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6")
              .add("relativePeriodDate=2025-09-29");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          6,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"202402\":{\"name\":\"February 2024\"},\"202301\":{\"name\":\"January 2023\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[\"202301\",\"202402\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "A03MvHHogjR.a3kGcGDCuk6",
          "MCH Apgar Score",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      // validateHeaderPropertiesByName(response, actualHeaders,"pe", "", "TEXT",
      // "java.lang.String",
      // false, true);
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

      // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
      if (!expectPostgis) {
        validateHeaderExistence(actualHeaders, "geometry", false);
        validateHeaderExistence(actualHeaders, "longitude", false);
        validateHeaderExistence(actualHeaders, "latitude", false);
      }

      // rowContext not found or empty in the response, skipping assertions.
      // 7. Assert row values in any order.
      validateRow(response, List.of("", "ImspTQPwCqd", "202301", "1"));
      validateRow(response, List.of("2.0", "ImspTQPwCqd", "202301", "236"));
    }

    @Test
    @DisplayName(
        "Events Aggregate - Fixed periods with gaps - Time field: ENROLLMENT_DATE as dimension")
    public void eventAggregateWithFixedPeriodTimeFieldasDimension() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("stage=A03MvHHogjR")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("outputType=EVENT")
              .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6,ENROLLMENT_DATE:202301;202402")
              .add("relativePeriodDate=2025-09-29");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          6,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"enrollmentdate\":{\"name\":\"Date of enrollment\"},\"202402\":{\"name\":\"February 2024\"},\"202301\":{\"name\":\"January 2023\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"enrollmentdate\":[\"202301\",\"202402\"],\"pe\":[\"202301\",\"202402\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "A03MvHHogjR.a3kGcGDCuk6",
          "MCH Apgar Score",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "enrollmentdate",
          "Date of enrollment",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "202301",
              "value",
              "1"));

      // Validate row exists with values from original row index 2
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "1.0",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "202301",
              "value",
              "207"));

      // Validate row exists with values from original row index 4
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "2.0",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "202301",
              "value",
              "236"));

      // Validate row exists with values from original row index 5
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "7.0",
              "ou",
              "ImspTQPwCqd",
              "enrollmentdate",
              "202301",
              "value",
              "1"));
    }
  }

  @Nested
  @DisplayName("Incident date")
  class IncidentDateDate {
    @Test
    @DisplayName("Events Aggregate - Date range - Time field: incidentDate")
    public void incidentDateWithTimeRange() throws JSONException {

      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("stage=A03MvHHogjR")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("incidentDate=2021-03-01_2023-04-30")
              .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6")
              .add("relativePeriodDate=2025-09-29");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          2023,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"items\":{\"20210301\":{\"name\":\"2021-03-01\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[\"20210301\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "A03MvHHogjR.a3kGcGDCuk6",
          "MCH Apgar Score",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      // validateHeaderPropertiesByName(response, actualHeaders,"pe", "", "TEXT",
      // "java.lang.String",
      // false, true);
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row values in any order.
      validateRow(response, List.of("1.0", "ImspTQPwCqd", "20211008", "9"));
      validateRow(response, List.of("0.0", "ImspTQPwCqd", "20211028", "5"));
    }

    @Test
    @DisplayName("Events Aggregate - Date range - Time field: INCIDENT_DATE as dimension")
    public void incidentDateAsDimensionWithTimeRange() throws JSONException {

      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("stage=A03MvHHogjR")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add(
                  "dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6,INCIDENT_DATE:2021-03-01_2023-04-30")
              .add("relativePeriodDate=2025-09-29");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          2023,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"items\":{\"20210301\":{\"name\":\"2021-03-01\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[\"20210301\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "A03MvHHogjR.a3kGcGDCuk6",
          "MCH Apgar Score",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      // validateHeaderPropertiesByName(response, actualHeaders,"pe", "", "TEXT",
      // "java.lang.String",
      // false, true);
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row values in any order.
      validateRow(response, List.of("1.0", "ImspTQPwCqd", "20211008", "9"));
      validateRow(response, List.of("0.0", "ImspTQPwCqd", "20211028", "5"));
    }
  }

  @Nested
  @DisplayName("Last updated")
  class LastUpdated {

    @Test
    @DisplayName("Events Aggregate - Date range - Time field: lastUpdated")
    public void lastUpdatedWithTimeRange() throws JSONException {

      // generated sql condition:
      // where (((ax."lastupdated" >= '2018-07-01' and ax."lastupdated" < '2019-01-31'))))
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("lastUpdated=2018-07-30_2018-08-07")
              .add("stage=A03MvHHogjR")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6")
              .add("relativePeriodDate=2025-09-29");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          7,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"20180730\":{\"name\":\"2018-07-30\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[\"20180730\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "A03MvHHogjR.a3kGcGDCuk6",
          "MCH Apgar Score",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      // validateHeaderPropertiesByName(response, actualHeaders,"pe", "", "TEXT",
      // "java.lang.String",
      // false, true);
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

      // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
      if (!expectPostgis) {
        validateHeaderExistence(actualHeaders, "geometry", false);
        validateHeaderExistence(actualHeaders, "longitude", false);
        validateHeaderExistence(actualHeaders, "latitude", false);
      }

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row values in any order.
      validateRow(response, List.of("0.0", "ImspTQPwCqd", "20180806", "3666"));
      validateRow(response, List.of("1.0", "ImspTQPwCqd", "20180806", "3683"));
      validateRow(response, List.of("2.0", "ImspTQPwCqd", "20180806", "3649"));
      validateRow(response, List.of("0.0", "ImspTQPwCqd", "20180807", "2728"));
      validateRow(response, List.of("1.0", "ImspTQPwCqd", "20180807", "2638"));
      validateRow(response, List.of("2.0", "ImspTQPwCqd", "20180807", "2629"));
      validateRow(response, List.of("3.0", "ImspTQPwCqd", "20180807", "1"));
    }

    @Test
    @DisplayName("Events Aggregate - Date range - Time field: LAST_UPDATED as dimension")
    public void lastUpdatedAsDimensionWithTimeRange() throws JSONException {

      // generated sql condition:
      // where (((ax."lastupdated" >= '2018-07-01' and ax."lastupdated" < '2019-01-31'))))
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("stage=A03MvHHogjR")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add(
                  "dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6,LAST_UPDATED:2018-07-30_2018-08-07")
              .add("relativePeriodDate=2025-09-29");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          7,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"20180730\":{\"name\":\"2018-07-30\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[\"20180730\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "A03MvHHogjR.a3kGcGDCuk6",
          "MCH Apgar Score",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      // validateHeaderPropertiesByName(response, actualHeaders,"pe", "", "TEXT",
      // "java.lang.String",
      // false, true);
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

      // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
      if (!expectPostgis) {
        validateHeaderExistence(actualHeaders, "geometry", false);
        validateHeaderExistence(actualHeaders, "longitude", false);
        validateHeaderExistence(actualHeaders, "latitude", false);
      }

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row values in any order.
      validateRow(response, List.of("0.0", "ImspTQPwCqd", "20180806", "3666"));
      validateRow(response, List.of("1.0", "ImspTQPwCqd", "20180806", "3683"));
      validateRow(response, List.of("2.0", "ImspTQPwCqd", "20180806", "3649"));
      validateRow(response, List.of("0.0", "ImspTQPwCqd", "20180807", "2728"));
      validateRow(response, List.of("1.0", "ImspTQPwCqd", "20180807", "2638"));
      validateRow(response, List.of("2.0", "ImspTQPwCqd", "20180807", "2629"));
      validateRow(response, List.of("3.0", "ImspTQPwCqd", "20180807", "1"));
    }
  }

  @Nested
  @DisplayName("Created date")
  class CreatedDate {

    @Test
    @DisplayName("Events Aggregate - Date range - Time field: CREATED as dimension")
    public void createdDateAsDimensionWithTimeRange() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("stage=A03MvHHogjR")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6,CREATED:2016-07-30_2017-08-07")
              .add("relativePeriodDate=2025-09-29");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          20,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"20160730\":{\"name\":\"2016-07-30\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"created\":{\"name\":\"Created\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[\"20160730\"],\"created\":[\"20160730\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "A03MvHHogjR.a3kGcGDCuk6",
          "MCH Apgar Score",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response, actualHeaders, "created", "Created", "TEXT", "java.lang.String", false, true);
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "5.0",
              "ou",
              "ImspTQPwCqd",
              "created",
              "20160923",
              "value",
              "1"));

      // Validate row exists with values from original row index 4
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "123.0",
              "ou",
              "ImspTQPwCqd",
              "created",
              "20161115",
              "value",
              "1"));

      // Validate row exists with values from original row index 8
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "7.0",
              "ou",
              "ImspTQPwCqd",
              "created",
              "20170120",
              "value",
              "1"));

      // Validate row exists with values from original row index 12
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "1.0",
              "ou",
              "ImspTQPwCqd",
              "created",
              "20170806",
              "value",
              "3683"));

      // Validate row exists with values from original row index 16
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "0.0",
              "ou",
              "ImspTQPwCqd",
              "created",
              "20170807",
              "value",
              "2729"));

      // Validate row exists with values from original row index 19
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "11.0",
              "ou",
              "ImspTQPwCqd",
              "created",
              "20170807",
              "value",
              "1"));
    }
  }

  @Nested
  @DisplayName("Completed date")
  class CompletedDate {

    @Test
    @DisplayName("Events Aggregate - Date range - Time field: COMPLETED as dimension")
    public void completedDateAsDimensionWithTimeRange() throws JSONException {
      // Read the 'expect.postgis' system property at runtime to adapt assertions.
      boolean expectPostgis = isPostgres();

      // Given
      QueryParamsBuilder params =
          new QueryParamsBuilder()
              .add("stage=A03MvHHogjR")
              .add("displayProperty=NAME")
              .add("totalPages=false")
              .add(
                  "dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6,COMPLETED:2021-07-30_2022-08-07")
              .add("relativePeriodDate=2025-09-29");

      // When
      ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

      // Then
      // 1. Validate Response Structure (Counts, Headers, Height/Width)
      //    This helper checks basic counts and dimensions, adapting based on the runtime
      // 'expectPostgis' flag.
      validateResponseStructure(
          response,
          expectPostgis,
          9,
          4,
          4); // Pass runtime flag, row count, and expected header counts

      // 2. Extract Headers into a List of Maps for easy access by name
      List<Map<String, Object>> actualHeaders =
          response.extractList("headers", Map.class).stream()
              .map(obj -> (Map<String, Object>) obj) // Ensure correct type
              .collect(Collectors.toList());

      // 3. Assert metaData.
      String expectedMetaData =
          "{\"items\":{\"20210730\":{\"name\":\"2021-07-30\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"completed\":{\"name\":\"Completed\"},\"completeddate\":{\"name\":\"Completed date\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"completed\":[\"20210730\"],\"pe\":[\"20210730\"],\"ou\":[\"ImspTQPwCqd\"]}}";
      String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
      assertEquals(expectedMetaData, actualMetaData, false);

      // 4. Validate Headers By Name (conditionally checking PostGIS headers).
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "A03MvHHogjR.a3kGcGDCuk6",
          "MCH Apgar Score",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "ou",
          "Organisation unit",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "completed",
          "Completed",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeaderPropertiesByName(
          response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

      // rowContext not found or empty in the response, skipping assertions.

      // 7. Assert row existence by value (unsorted results - validates all columns).
      // Validate row exists with values from original row index 0
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "5.0",
              "ou",
              "ImspTQPwCqd",
              "completed",
              "20211115",
              "value",
              "3"));

      // Validate row exists with values from original row index 2
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "10.0",
              "ou",
              "ImspTQPwCqd",
              "completed",
              "20211115",
              "value",
              "1"));

      // Validate row exists with values from original row index 4
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "10.0",
              "ou",
              "ImspTQPwCqd",
              "completed",
              "20211116",
              "value",
              "1"));

      // Validate row exists with values from original row index 6
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "5.0",
              "ou",
              "ImspTQPwCqd",
              "completed",
              "20220120",
              "value",
              "2"));

      // Validate row exists with values from original row index 8
      validateRowExists(
          response,
          actualHeaders,
          Map.of(
              "A03MvHHogjR.a3kGcGDCuk6",
              "11.0",
              "ou",
              "ImspTQPwCqd",
              "completed",
              "20220227",
              "value",
              "1"));
    }
  }

  @Test
  @DisplayName("Events Aggregate - Six months - Time field: eventDate")
  public void eventAggregateLastSixMonths() throws JSONException {

    // generated sql condition:
    // (((ax."occurreddate" >= '2021-01-01' and ax."occurreddate" < '2021-07-01')))

    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6")
            .add("eventDate=2021S1")
            .add("relativePeriodDate=2025-09-29");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        8,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"2021S1\":{\"name\":\"January - June 2021\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[\"2021S1\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ou",
        "Organisation unit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    // validateHeaderPropertiesByName(response, actualHeaders,"pe", "", "TEXT", "java.lang.String",
    // false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values in any order.
    validateRow(response, List.of("1.0", "ImspTQPwCqd", "2021S1", "1870"));
    validateRow(response, List.of("2.0", "ImspTQPwCqd", "2021S1", "1847"));
  }
}
