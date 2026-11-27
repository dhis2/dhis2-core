/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hamcrest.Matchers.equalTo;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
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
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/aggregate" endpoint. */
public class EventsAggregate15AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void legendValueAndDimensionAverageFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=A03MvHHogjR.ou:ImspTQPwCqd")
            .add("aggregationType=AVERAGE")
            .add("dimension=A03MvHHogjR.EVENT_DATE:LAST_5_YEARS,A03MvHHogjR.a3kGcGDCuk6:LT:6")
            .add("value=A03MvHHogjR.a3kGcGDCuk6")
            .add("relativePeriodDate=2026-11-20");

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
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"2023\":{\"name\":\"2023\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"2025\":{\"name\":\"2025\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2024\":{\"name\":\"2024\"}},\"dimensions\":{\"A03MvHHogjR.ou\":[\"in ImspTQPwCqd\"],\"A03MvHHogjR.a3kGcGDCuk6\":[],\"A03MvHHogjR.eventdate\":[\"2021\",\"2022\",\"2023\",\"2024\",\"2025\"],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
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
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2021", "A03MvHHogjR.a3kGcGDCuk6", "0.0", "value", "0.0"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2021", "A03MvHHogjR.a3kGcGDCuk6", "2.0", "value", "2.0"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2021", "A03MvHHogjR.a3kGcGDCuk6", "5.0", "value", "5.0"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2022", "A03MvHHogjR.a3kGcGDCuk6", "1.0", "value", "1.0"));

    // Validate row exists with values from original row index 8
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2022", "A03MvHHogjR.a3kGcGDCuk6", "5.0", "value", "5.0"));
  }

  @Test
  public void legendValueAndDimensionCountFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=A03MvHHogjR.ou:ImspTQPwCqd")
            .add("aggregationType=COUNT")
            .add("dimension=A03MvHHogjR.EVENT_DATE:LAST_5_YEARS,A03MvHHogjR.a3kGcGDCuk6:LT:6")
            .add("value=A03MvHHogjR.a3kGcGDCuk6")
            .add("relativePeriodDate=2026-11-20");

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
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"2023\":{\"name\":\"2023\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"2025\":{\"name\":\"2025\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2024\":{\"name\":\"2024\"}},\"dimensions\":{\"A03MvHHogjR.ou\":[\"in ImspTQPwCqd\"],\"A03MvHHogjR.a3kGcGDCuk6\":[],\"A03MvHHogjR.eventdate\":[\"2021\",\"2022\",\"2023\",\"2024\",\"2025\"],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
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
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "A03MvHHogjR.eventdate", "2021", "A03MvHHogjR.a3kGcGDCuk6", "0.0", "value", "3666.0"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "A03MvHHogjR.eventdate", "2021", "A03MvHHogjR.a3kGcGDCuk6", "2.0", "value", "3650.0"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2021", "A03MvHHogjR.a3kGcGDCuk6", "5.0", "value", "3.0"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "A03MvHHogjR.eventdate", "2022", "A03MvHHogjR.a3kGcGDCuk6", "1.0", "value", "2638.0"));

    // Validate row exists with values from original row index 8
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2022", "A03MvHHogjR.a3kGcGDCuk6", "5.0", "value", "2.0"));
  }

  @Test
  public void legendValueAndDimensionSumFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=A03MvHHogjR.ou:ImspTQPwCqd")
            .add("aggregationType=SUM")
            .add("dimension=A03MvHHogjR.EVENT_DATE:LAST_5_YEARS,A03MvHHogjR.a3kGcGDCuk6:LT:6")
            .add("value=A03MvHHogjR.a3kGcGDCuk6")
            .add("relativePeriodDate=2026-11-20");

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
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"2023\":{\"name\":\"2023\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"2025\":{\"name\":\"2025\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2024\":{\"name\":\"2024\"}},\"dimensions\":{\"A03MvHHogjR.ou\":[\"in ImspTQPwCqd\"],\"A03MvHHogjR.a3kGcGDCuk6\":[],\"A03MvHHogjR.eventdate\":[\"2021\",\"2022\",\"2023\",\"2024\",\"2025\"],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
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
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2021", "A03MvHHogjR.a3kGcGDCuk6", "0.0", "value", "0.0"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "A03MvHHogjR.eventdate", "2021", "A03MvHHogjR.a3kGcGDCuk6", "2.0", "value", "7300.0"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2021", "A03MvHHogjR.a3kGcGDCuk6", "5.0", "value", "15.0"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "A03MvHHogjR.eventdate", "2022", "A03MvHHogjR.a3kGcGDCuk6", "1.0", "value", "2638.0"));

    // Validate row exists with values from original row index 8
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2022", "A03MvHHogjR.a3kGcGDCuk6", "5.0", "value", "10.0"));
  }

  @Test
  public void legendValueAndDimensionAverageDimension() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=A03MvHHogjR.ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6:LT:6")
            .add("aggregationType=AVERAGE")
            .add("dimension=A03MvHHogjR.EVENT_DATE:LAST_5_YEARS")
            .add("value=A03MvHHogjR.a3kGcGDCuk6")
            .add("relativePeriodDate=2026-11-20");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        2,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"2023\":{\"name\":\"2023\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"2025\":{\"name\":\"2025\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2024\":{\"name\":\"2024\"}},\"dimensions\":{\"A03MvHHogjR.ou\":[\"in ImspTQPwCqd\"],\"A03MvHHogjR.a3kGcGDCuk6\":[\"< 6\"],\"A03MvHHogjR.eventdate\":[\"2021\",\"2022\",\"2023\",\"2024\",\"2025\"],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.eventdate", "2021", "value", "1.0"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.eventdate", "2022", "value", "0.99"));
  }

  @Test
  public void legendValueAndDimensionCountDimension() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=A03MvHHogjR.ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6:LT:6")
            .add("aggregationType=COUNT")
            .add("dimension=A03MvHHogjR.EVENT_DATE:LAST_5_YEARS")
            .add("value=A03MvHHogjR.a3kGcGDCuk6")
            .add("relativePeriodDate=2026-11-20");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        2,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"2023\":{\"name\":\"2023\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"2025\":{\"name\":\"2025\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2024\":{\"name\":\"2024\"}},\"dimensions\":{\"A03MvHHogjR.ou\":[\"in ImspTQPwCqd\"],\"A03MvHHogjR.a3kGcGDCuk6\":[\"< 6\"],\"A03MvHHogjR.eventdate\":[\"2021\",\"2022\",\"2023\",\"2024\",\"2025\"],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.eventdate", "2021", "value", "11005.0"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.eventdate", "2022", "value", "8001.0"));
  }

  @Test
  public void legendValueAndDimensionSumDimension() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=A03MvHHogjR.ou:ImspTQPwCqd,A03MvHHogjR.a3kGcGDCuk6:LT:6")
            .add("aggregationType=SUM")
            .add("dimension=A03MvHHogjR.EVENT_DATE:LAST_5_YEARS")
            .add("value=A03MvHHogjR.a3kGcGDCuk6")
            .add("relativePeriodDate=2026-11-20");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        2,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"2023\":{\"name\":\"2023\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"2025\":{\"name\":\"2025\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2024\":{\"name\":\"2024\"}},\"dimensions\":{\"A03MvHHogjR.ou\":[\"in ImspTQPwCqd\"],\"A03MvHHogjR.a3kGcGDCuk6\":[\"< 6\"],\"A03MvHHogjR.eventdate\":[\"2021\",\"2022\",\"2023\",\"2024\",\"2025\"],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.eventdate", "2021", "value", "11007.0"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.eventdate", "2022", "value", "7912.0"));
  }

  @Test
  public void stageEventDateWithPeriodsOfDifferentTypes() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder().add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.EVENT_DATE:2021;2022Q1");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        2,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"2022Q1\":{\"name\":\"January - March 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2021\":{\"name\":\"2021\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.eventdate\":[\"2021\",\"2022Q1\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2021", "ou", "ImspTQPwCqd", "value", "11017"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2022Q1", "ou", "ImspTQPwCqd", "value", "2017"));
  }

  @Test
  public void stageEventDateWithOverlappingPeriodsOfDifferentTypes() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder().add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.EVENT_DATE:2022;2022Q1");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        2,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"2022Q1\":{\"name\":\"January - March 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2022\":{\"name\":\"2022\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.eventdate\":[\"2022\",\"2022Q1\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2022", "ou", "ImspTQPwCqd", "value", "8005"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2022Q1", "ou", "ImspTQPwCqd", "value", "2017"));
  }

  @Test
  public void stageEventDateWithRelativePeriodsOfDifferentTypes() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.EVENT_DATE:LAST_12_MONTHS;THIS_YEAR")
            .add("relativePeriodDate=2022-06-15");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        13,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"202109\":{\"name\":\"September 2021\"},\"2022\":{\"name\":\"2022\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"202204\":{\"name\":\"April 2022\"},\"202106\":{\"name\":\"June 2021\"},\"202205\":{\"name\":\"May 2022\"},\"202202\":{\"name\":\"February 2022\"},\"202203\":{\"name\":\"March 2022\"},\"202112\":{\"name\":\"December 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202201\":{\"name\":\"January 2022\"},\"202110\":{\"name\":\"October 2021\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"202111\":{\"name\":\"November 2021\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.eventdate\":[\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\",\"202201\",\"202202\",\"202203\",\"202204\",\"202205\",\"2022\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "202106", "ou", "ImspTQPwCqd", "value", "897"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "202110", "ou", "ImspTQPwCqd", "value", "973"));

    // Validate row exists with values from original row index 8
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "202202", "ou", "ImspTQPwCqd", "value", "629"));

    // Validate row exists with values from original row index 12
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2022", "ou", "ImspTQPwCqd", "value", "8005"));
  }

  @Test
  public void stageScheduledDateWithPeriodsOfDifferentTypes() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.SCHEDULED_DATE:2021;2022Q1");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        2,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"2022Q1\":{\"name\":\"January - March 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR.scheduleddate\":{\"name\":\"Scheduled date\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2021\":{\"name\":\"2021\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.scheduleddate\":[\"2021\",\"2022Q1\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.scheduleddate",
        "Scheduled date",
        "DATE",
        "java.time.LocalDate",
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.scheduleddate", "2021", "ou", "ImspTQPwCqd", "value", "11009"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.scheduleddate", "2022Q1", "ou", "ImspTQPwCqd", "value", "1995"));
  }

  @Test
  void stageEventDateWithPeriodsAndDateRangeIsRejected() {
    assertStageDateRejected("A03MvHHogjR.EVENT_DATE:2021;2022Q1;2022-05-01_2022-05-31");
  }

  @Test
  void stageEventDateWithTwoDateRangesIsRejected() {
    assertStageDateRejected("A03MvHHogjR.EVENT_DATE:2022-01-01_2022-01-31;2022-03-01_2022-03-31");
  }

  @Test
  void stageScheduledDateWithPeriodAndDateRangeIsRejected() {
    assertStageDateRejected("A03MvHHogjR.SCHEDULED_DATE:2022;2022-05-01_2022-05-31");
  }

  private void assertStageDateRejected(String dimension) {
    QueryParamsBuilder params =
        new QueryParamsBuilder().add("dimension=ou:ImspTQPwCqd," + dimension);

    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7271"));
  }

  @Test
  public void stageEventDateWithNonconsecutiveQuartersAndYear() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.EVENT_DATE:2021;2022Q1;2022Q3");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        3,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"2022Q1\":{\"name\":\"January - March 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2021\":{\"name\":\"2021\"},\"2022Q3\":{\"name\":\"July - September 2022\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.eventdate\":[\"2021\",\"2022Q1\",\"2022Q3\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2021", "ou", "ImspTQPwCqd", "value", "11017"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.eventdate", "2022Q3", "ou", "ImspTQPwCqd", "value", "2013"));
  }

  @Test
  public void orgUnitFilterAtDifferentLevelsCountsEveryLevel() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:O6uvpzGd5pu;taKiTcaf05H")
            .add("stage=A03MvHHogjR")
            .add("outputType=EVENT")
            .add("dimension=pe:2022");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"pe\":{\"name\":\"Period\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"taKiTcaf05H\":{\"name\":\"Mabenteh Community Hospital\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"O6uvpzGd5pu\",\"taKiTcaf05H\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("pe", "2022", "value", "870"));
  }
}
