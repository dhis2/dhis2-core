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

/** Groups e2e tests for "/enrollments/aggregate" endpoint. */
public class EnrollmentsAggregate8AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void registrationOuWithLevel2() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=asc")
            .add("totalPages=false")
            .add("dimension=pe:2022,REGISTRATION_OU:LEVEL-2");

    // When
    ApiResponse response = actions.aggregate().get("regOuProg01", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"ou\":{},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"pe\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationou",
        "Registration org unit",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "1", "pe", "2022", "registrationou", "O6uvpzGd5pu"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "3", "pe", "2022", "registrationou", "jUb8gELQApl"));
  }

  @Test
  public void registrationOuWithMultipleOus() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("sortOrder=asc")
            .add("totalPages=false")
            .add("dimension=pe:2022,REGISTRATION_OU:O6uvpzGd5pu;fdc6uOvgoji");

    // When
    ApiResponse response = actions.aggregate().get("regOuProg01", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{},\"ou\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationou",
        "Registration org unit",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "1", "pe", "2022", "registrationou", "O6uvpzGd5pu"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "2", "pe", "2022", "registrationou", "fdc6uOvgoji"));
  }

  @Test
  public void registrationOuAsFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=REGISTRATION_OU:jUb8gELQApl")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("dimension=pe:2022");

    // When
    ApiResponse response = actions.aggregate().get("regOuProg01", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"pe\":{},\"ou\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"2022\":{\"name\":\"2022\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("value", "3", "pe", "2022"));
  }

  @Test
  public void registrationOuWithUserOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("dimension=pe:2022,REGISTRATION_OU:USER_ORGUNIT");

    // When
    ApiResponse response = actions.aggregate().get("regOuProg01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"2022\":{\"name\":\"2022\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationou",
        "Registration org unit",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "6", "pe", "2022", "registrationou", "ImspTQPwCqd"));
  }

  @Test
  public void registrationOuCombinedWithEnrollmentOu() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("dimension=pe:2022,ou:LEVEL-2,REGISTRATION_OU:O6uvpzGd5pu");

    // When
    ApiResponse response = actions.aggregate().get("regOuProg01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"2022\":{\"name\":\"2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"wjP19dkFeIk\":{\"uid\":\"wjP19dkFeIk\",\"name\":\"District\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"registrationou\":[\"O6uvpzGd5pu\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationou",
        "Registration org unit",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "1", "ou", "fdc6uOvgoji", "pe", "2022", "registrationou", "O6uvpzGd5pu"));
  }

  @Test
  public void registrationOuOutputCode() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("outputIdScheme=CODE")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("dimension=pe:2022,REGISTRATION_OU:O6uvpzGd5pu");

    // When
    ApiResponse response = actions.aggregate().get("regOuProg01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{},\"ou\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"O6uvpzGd5pu\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationou",
        "Registration org unit",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("value", "1", "pe", "2022", "registrationou", "OU_264"));
  }

  @Test
  public void registrationOuOutputName() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("outputIdScheme=NAME")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("dimension=pe:2022,REGISTRATION_OU:O6uvpzGd5pu");

    // When
    ApiResponse response = actions.aggregate().get("regOuProg01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{},\"ou\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"O6uvpzGd5pu\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationou",
        "Registration org unit",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("value", "1", "pe", "2022", "registrationou", "Bo"));
  }

  @Test
  public void registrationOuOutputCodeWithOrdinaryOu() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("outputIdScheme=CODE")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("dimension=pe:2022,REGISTRATION_OU:O6uvpzGd5pu,ou:LEVEL-2");

    // When
    ApiResponse response = actions.aggregate().get("regOuProg01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"2022\":{\"name\":\"2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"wjP19dkFeIk\":{\"uid\":\"wjP19dkFeIk\",\"name\":\"District\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"registrationou\":[\"O6uvpzGd5pu\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationou",
        "Registration org unit",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "1", "ou", "OU_193190", "pe", "2022", "registrationou", "OU_264"));
  }

  @Test
  public void incidentDateDimensionForEnrollments() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder().add("dimension=ou:ImspTQPwCqd,INCIDENT_DATE:GT:2022-12-15");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        32,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"occurreddate\":{\"name\":\"occurreddate\"}},\"dimensions\":{\"occurreddate\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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
        response,
        actualHeaders,
        "occurreddate",
        "occurreddate",
        "DATE",
        "java.time.LocalDate",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "11", "ou", "ImspTQPwCqd", "occurreddate", "2023-11-25 12:05:00.0"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "27", "ou", "ImspTQPwCqd", "occurreddate", "2023-12-01 12:05:00.0"));

    // Validate row exists with values from original row index 12
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "19", "ou", "ImspTQPwCqd", "occurreddate", "2023-12-07 12:05:00.0"));

    // Validate row exists with values from original row index 18
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "1", "ou", "ImspTQPwCqd", "occurreddate", "2023-12-17 00:00:00.0"));

    // Validate row exists with values from original row index 24
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "25", "ou", "ImspTQPwCqd", "occurreddate", "2023-12-22 12:05:00.0"));

    // Validate row exists with values from original row index 30
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "22", "ou", "ImspTQPwCqd", "occurreddate", "2023-12-28 12:05:00.0"));

    // Validate row exists with values from original row index 31
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "24", "ou", "ImspTQPwCqd", "occurreddate", "2023-12-29 12:05:00.0"));
  }

  @Test
  public void enrollmentStatusCompletedOnEnrollmentsAggregate() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("enrollmentStatus=COMPLETED")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("dimension=ou:ImspTQPwCqd,pe:LAST_10_YEARS")
            .add("relativePeriodDate=2025-01-01");

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
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"2023\":{\"name\":\"2023\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"2020\":{\"name\":\"2020\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"2019\":{\"name\":\"2019\"},\"2018\":{\"name\":\"2018\"},\"programstatus\":{\"name\":\"Program status\"},\"COMPLETED\":{\"name\":\"Completed\"},\"2017\":{\"name\":\"2017\"},\"2016\":{\"name\":\"2016\"},\"2015\":{\"name\":\"2015\"},\"pe\":{},\"2024\":{\"name\":\"2024\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"pe\":[\"2015\",\"2016\",\"2017\",\"2018\",\"2019\",\"2020\",\"2021\",\"2022\",\"2023\",\"2024\"],\"ou\":[\"ImspTQPwCqd\"],\"programstatus\":[\"COMPLETED\"]}}";
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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "2", "ou", "ImspTQPwCqd", "pe", "2022", "programstatus", "COMPLETED"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "3", "ou", "ImspTQPwCqd", "pe", "2023", "programstatus", "COMPLETED"));
  }

  private static final String PROGRAM = "IpHINAT79UW";

  @Test
  void enrollmentDateRangeDimensionIsRejected() {
    assertRejected("ENROLLMENT_DATE:2022-01-01_2022-12-31");
  }

  @Test
  void singleDayEnrollmentDateRangeDimensionIsRejected() {
    assertRejected("ENROLLMENT_DATE:2022-01-01_2022-01-01");
  }

  @Test
  void periodRangeDimensionIsRejected() {
    assertRejected("pe:2022-01-01_2022-12-31");
  }

  private void assertRejected(String dimension) {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd," + dimension)
            .add("totalPages=false");

    ApiResponse response = actions.aggregate().get(PROGRAM, JSON, JSON, params);

    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7270"));
  }

  @Test
  public void programIndicatorDimensionCountsEachEnrollmentOnce() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("dimension=ou:ImspTQPwCqd,pe:2021;2022;2023;2024,qZOBw051LSf");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        4,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"2024\":{\"name\":\"2024\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2023\":{\"name\":\"2023\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"qZOBw051LSf\":{\"name\":\"Child PI Complex\"}},\"dimensions\":{\"pe\":[\"2021\",\"2022\",\"2023\",\"2024\"],\"ou\":[\"ImspTQPwCqd\"],\"qZOBw051LSf\":[]}}";
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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "qZOBw051LSf",
        "Child PI Complex",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "8", "ou", "ImspTQPwCqd", "pe", "2022", "qZOBw051LSf", "0.69"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "7867", "ou", "ImspTQPwCqd", "pe", "2023", "qZOBw051LSf", "0.98"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "11017", "ou", "ImspTQPwCqd", "pe", "2022", "qZOBw051LSf", "0.98"));

    // Validate row exists with values from original row index 3
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "138", "ou", "ImspTQPwCqd", "pe", "2023", "qZOBw051LSf", "0.69"));
  }

  @Test
  public void programIndicatorReadingRepeatableStageCountsEachEnrollmentOnce()
      throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("dimension=ou:ImspTQPwCqd,pe:2022,TcTRasl29wo");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        39,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"pe\":{},\"TcTRasl29wo\":{\"name\":\"WHO RMNCH ANC Hemoglobin improvement\"},\"ou\":{\"name\":\"Organisation unit\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"2022\":{\"name\":\"2022\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[\"2022\"],\"TcTRasl29wo\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "TcTRasl29wo",
        "WHO RMNCH ANC Hemoglobin improvement",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "1076", "ou", "ImspTQPwCqd", "pe", "2022", "TcTRasl29wo", ""));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "13", "ou", "ImspTQPwCqd", "pe", "2022", "TcTRasl29wo", "-15"));

    // Validate row exists with values from original row index 12
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "6", "ou", "ImspTQPwCqd", "pe", "2022", "TcTRasl29wo", "18"));

    // Validate row exists with values from original row index 18
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "28", "ou", "ImspTQPwCqd", "pe", "2022", "TcTRasl29wo", "-7"));

    // Validate row exists with values from original row index 24
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "22", "ou", "ImspTQPwCqd", "pe", "2022", "TcTRasl29wo", "11"));

    // Validate row exists with values from original row index 30
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "13", "ou", "ImspTQPwCqd", "pe", "2022", "TcTRasl29wo", "-14"));

    // Validate row exists with values from original row index 36
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "7", "ou", "ImspTQPwCqd", "pe", "2022", "TcTRasl29wo", "13"));

    // Validate row exists with values from original row index 38
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "8", "ou", "ImspTQPwCqd", "pe", "2022", "TcTRasl29wo", "-13"));
  }
}
