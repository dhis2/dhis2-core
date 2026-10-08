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
package org.hisp.dhis.analytics.enrollment.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowExists;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Rejected {@code value} and {@code aggregationType} combinations on /enrollments/aggregate. The
 * accepted ones are covered by the generated {@code customValue*} scenarios.
 */
public class EnrollmentsAggregateCustomValueTest extends AnalyticsApiTest {
  private static final String PROGRAM = "WSGAb5XwJ3Y";
  private static final String ANC_VISIT = "edqlbukwRfQ";
  private static final String HEMOGLOBIN = "vANAXwtLwcT";
  private static final String TB_PROGRAM = "ur1Edk5Oe2n";
  private static final String HEIGHT_IN_CM = "lw1SqmMlnfh";
  private static final String SIERRA_LEONE = "ImspTQPwCqd";

  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  private QueryParamsBuilder ancParams() {
    return new QueryParamsBuilder()
        .add("dimension=ou:" + SIERRA_LEONE + ",pe:2022;2023")
        .add("totalPages=false");
  }

  @Test
  void attributeOfTypeNoneWithoutAggregationTypeIsRejected() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:" + SIERRA_LEONE + ",pe:2022")
            .add("value=" + HEIGHT_IN_CM);

    assertConflict(actions.aggregate().get(TB_PROGRAM, JSON, JSON, params), "E7265");
  }

  @Test
  void dataElementWithoutStageIsRejected() {
    QueryParamsBuilder params =
        ancParams().add("value=" + HEMOGLOBIN).add("aggregationType=AVERAGE");

    assertConflict(actions.aggregate().get(PROGRAM, JSON, JSON, params), "E7266");
  }

  @Test
  void unsupportedAggregationTypeIsRejected() {
    QueryParamsBuilder params =
        ancParams().add("value=" + ANC_VISIT + "." + HEMOGLOBIN).add("aggregationType=LAST");

    assertConflict(actions.aggregate().get(PROGRAM, JSON, JSON, params), "E7267");
  }

  @Test
  void offsetInValueIsRejected() {
    QueryParamsBuilder params =
        ancParams().add("value=" + ANC_VISIT + "[1]." + HEMOGLOBIN).add("aggregationType=AVERAGE");

    assertConflict(actions.aggregate().get(PROGRAM, JSON, JSON, params), "E7264");
  }

  @Test
  void aggregationTypeWithoutValueIsRejected() {
    QueryParamsBuilder params = ancParams().add("aggregationType=AVERAGE");

    assertConflict(actions.aggregate().get(PROGRAM, JSON, JSON, params), "E7204");
  }

  @Test
  public void customValueStageDataElementAverage() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("aggregationType=AVERAGE")
            .add("totalPages=false")
            .add("dimension=ou:ImspTQPwCqd,pe:2022;2023")
            .add("value=edqlbukwRfQ.vANAXwtLwcT");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"2023\":{\"name\":\"2023\"},\"2022\":{\"name\":\"2022\"},\"edqlbukwRfQ.vANAXwtLwcT\":{\"code\":\"LAB_HB\",\"name\":\"WHOMCH Hemoglobin value\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"pe\":{},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[\"2022\",\"2023\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
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
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("value", "15.43", "ou", "ImspTQPwCqd", "pe", "2022"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("value", "15.6", "ou", "ImspTQPwCqd", "pe", "2023"));
  }

  @Test
  public void customValueStageDataElementOwnAggregationType() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("dimension=ou:ImspTQPwCqd,pe:2022;2023")
            .add("value=edqlbukwRfQ.vANAXwtLwcT");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"2023\":{\"name\":\"2023\"},\"2022\":{\"name\":\"2022\"},\"edqlbukwRfQ.vANAXwtLwcT\":{\"code\":\"LAB_HB\",\"name\":\"WHOMCH Hemoglobin value\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"pe\":{},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[\"2022\",\"2023\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
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
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("value", "28007", "ou", "ImspTQPwCqd", "pe", "2022"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("value", "27884", "ou", "ImspTQPwCqd", "pe", "2023"));
  }

  @Test
  public void customValueBlankLatestEventDropsEnrollmentCount() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("aggregationType=COUNT")
            .add("totalPages=false")
            .add("dimension=ou:ImspTQPwCqd,pe:2022;2023")
            .add("value=edqlbukwRfQ.EyfTU3ibMmJ");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"edqlbukwRfQ.EyfTU3ibMmJ\":{\"code\":\"CLI_PAL\",\"name\":\"WHOMCH Extreme pallor\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"2023\":{\"name\":\"2023\"},\"2022\":{\"name\":\"2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"pe\":{},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[\"2022\",\"2023\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
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
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("value", "1812", "ou", "ImspTQPwCqd", "pe", "2022"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("value", "1786", "ou", "ImspTQPwCqd", "pe", "2023"));
  }

  @Test
  public void customValueBooleanAverage() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("aggregationType=AVERAGE")
            .add("totalPages=false")
            .add("dimension=ou:ImspTQPwCqd,pe:2022;2023")
            .add("value=edqlbukwRfQ.EyfTU3ibMmJ");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"edqlbukwRfQ.EyfTU3ibMmJ\":{\"code\":\"CLI_PAL\",\"name\":\"WHOMCH Extreme pallor\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"2023\":{\"name\":\"2023\"},\"2022\":{\"name\":\"2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"pe\":{},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[\"2022\",\"2023\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
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
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("value", "0.51", "ou", "ImspTQPwCqd", "pe", "2022"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("value", "0.52", "ou", "ImspTQPwCqd", "pe", "2023"));
  }

  @Test
  public void customValueAttributeAverageSkipsEmptyCells() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("aggregationType=AVERAGE")
            .add("totalPages=false")
            .add("dimension=ou:ImspTQPwCqd,pe:2021;2022;2023")
            .add("value=lw1SqmMlnfh");

    // When
    ApiResponse response = actions.aggregate().get("ur1Edk5Oe2n", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"lw1SqmMlnfh\":{\"code\":\"Height in cm\",\"name\":\"Height in cm\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"pe\":{},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{\"name\":\"Organisation unit\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"2023\":{\"name\":\"2023\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"}},\"dimensions\":{\"pe\":[\"2021\",\"2022\",\"2023\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
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
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("value", "169.61", "ou", "ImspTQPwCqd", "pe", "2021"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("value", "169.59", "ou", "ImspTQPwCqd", "pe", "2022"));
  }

  @Test
  public void customValueAbsentCountsEnrollments() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("dimension=ou:ImspTQPwCqd,pe:2022;2023");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"pe\":{},\"ou\":{\"name\":\"Organisation unit\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"2023\":{\"name\":\"2023\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"2022\":{\"name\":\"2022\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[\"2022\",\"2023\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
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
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("value", "2008", "ou", "ImspTQPwCqd", "pe", "2023"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("value", "2002", "ou", "ImspTQPwCqd", "pe", "2022"));
  }

  private static void assertConflict(ApiResponse response, String errorCode) {
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo(errorCode));
  }
}
