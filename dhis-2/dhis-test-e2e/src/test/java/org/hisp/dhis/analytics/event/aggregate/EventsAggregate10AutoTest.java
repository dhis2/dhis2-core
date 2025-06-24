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
import static org.hisp.dhis.analytics.ValidationHelper.validateRowValueByName;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/aggregate" endpoint. */
public class EventsAggregate10AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  @DisplayName("Events Aggregate - Date range - Time field: lastUpdated")
  public void eventAggregateWithRangeTimeField() throws JSONException {

    // generated sql condition:
    // where (((ax."lastupdated" >= '2018-07-01' and ax."lastupdated" < '2019-01-31')))

    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = BooleanUtils.toBoolean(System.getProperty("expect.postgis", "false"));

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("lastUpdated=2018-07-01_2019-01-30")
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6");

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
        6,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "a3kGcGDCuk6",
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
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "a3kGcGDCuk6", "11.0");
    validateRowValueByName(response, actualHeaders, 0, "value", "2");
    validateRowValueByName(response, actualHeaders, 0, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 5
    validateRowValueByName(response, actualHeaders, 5, "a3kGcGDCuk6", "1.0");
    validateRowValueByName(response, actualHeaders, 5, "value", "6321");
    validateRowValueByName(response, actualHeaders, 5, "ou", "ImspTQPwCqd");
  }

  @Test
  @DisplayName("Events Aggregate - Six months - Time field: eventDate")
  public void eventAggregateLastSixMonths() throws JSONException {

    // generated sql condition:
    // (((ax."occurreddate" >= '2021-01-01' and ax."occurreddate" < '2021-07-01')))

    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = BooleanUtils.toBoolean(System.getProperty("expect.postgis", "false"));

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6")
            .add("eventDate=2021S1");

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
        6,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "a3kGcGDCuk6",
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
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "a3kGcGDCuk6", "3.0");
    validateRowValueByName(response, actualHeaders, 0, "value", "1");
    validateRowValueByName(response, actualHeaders, 0, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 7
    validateRowValueByName(response, actualHeaders, 7, "a3kGcGDCuk6", "2.0");
    validateRowValueByName(response, actualHeaders, 7, "value", "1847");
    validateRowValueByName(response, actualHeaders, 7, "ou", "ImspTQPwCqd");
  }

  @Test
  @DisplayName("Events Aggregate - Fixed periods with gaps - Time field: enrollmentDate")
  public void eventAggregateWithFixedPeriodTimeField() throws JSONException {
    // generated sql condition:
    // (((ax."enrollmentdate" >= '2023-01-01' and ax."enrollmentdate" < '2023-02-01')
    // or
    // (ax."enrollmentdate" >= '2024-02-01' and ax."enrollmentdate" < '2024-03-01')))

    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = BooleanUtils.toBoolean(System.getProperty("expect.postgis", "false"));

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("enrollmentDate=202301,202402")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6");

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
        6,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "a3kGcGDCuk6",
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
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "a3kGcGDCuk6", "2.0");
    validateRowValueByName(response, actualHeaders, 0, "value", "236");
    validateRowValueByName(response, actualHeaders, 0, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 5
    validateRowValueByName(response, actualHeaders, 5, "a3kGcGDCuk6", "1.0");
    validateRowValueByName(response, actualHeaders, 5, "value", "207");
    validateRowValueByName(response, actualHeaders, 5, "ou", "ImspTQPwCqd");
  }

  @Test
  @DisplayName("Events Aggregate - Relative period - Time field: scheduledDate")
  public void eventAggregateWithRelativePeriodTimeField() throws JSONException {

    // generated sql condition:
    // (((ax."scheduleddate" >= '2019-10-01' and ax."scheduleddate" < '2024-10-01')))

    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = BooleanUtils.toBoolean(System.getProperty("expect.postgis", "false"));

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("scheduledDate=LAST_5_FINANCIAL_YEARS")
            .add("outputType=EVENT")
            .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        12,
        6,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "a3kGcGDCuk6",
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
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "a3kGcGDCuk6", "8.0");
    validateRowValueByName(response, actualHeaders, 0, "value", "6");
    validateRowValueByName(response, actualHeaders, 0, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 11
    validateRowValueByName(response, actualHeaders, 11, "a3kGcGDCuk6", "1.0");
    validateRowValueByName(response, actualHeaders, 11, "value", "6321");
    validateRowValueByName(response, actualHeaders, 11, "ou", "ImspTQPwCqd");
  }

  @Test
  @DisplayName("Events Aggregate - Date range - Time field: incidentDate")
  public void financialYear2023Sep6() throws JSONException {

    // generated sql condition:
    // (((ax."enrollmentoccurreddate" >= '2021-03-01' and ax."enrollmentoccurreddate" <
    // '2023-05-01')))

    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = BooleanUtils.toBoolean(System.getProperty("expect.postgis", "false"));

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("incidentDate=2021-03-01_2023-04-30")
            .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        12,
        6,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "a3kGcGDCuk6",
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
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "a3kGcGDCuk6", "3.0");
    validateRowValueByName(response, actualHeaders, 0, "value", "3");
    validateRowValueByName(response, actualHeaders, 0, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 11
    validateRowValueByName(response, actualHeaders, 11, "a3kGcGDCuk6", "7.0");
    validateRowValueByName(response, actualHeaders, 11, "value", "1");
    validateRowValueByName(response, actualHeaders, 11, "ou", "ImspTQPwCqd");
  }
}
