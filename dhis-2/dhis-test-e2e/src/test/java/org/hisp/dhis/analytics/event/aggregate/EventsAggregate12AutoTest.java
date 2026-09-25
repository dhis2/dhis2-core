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
public class EventsAggregate12AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

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
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"ou\":{},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"pe\":{\"name\":\"Period\"},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("pe", "2022", "registrationou", "O6uvpzGd5pu", "value", "1"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response,
        actualHeaders,
        Map.of("pe", "2022", "registrationou", "jUb8gELQApl", "value", "3"));
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
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{\"name\":\"Period\"},\"ou\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("pe", "2022", "registrationou", "O6uvpzGd5pu", "value", "1"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response,
        actualHeaders,
        Map.of("pe", "2022", "registrationou", "fdc6uOvgoji", "value", "2"));
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
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"pe\":{\"name\":\"Period\"},\"ou\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"2022\":{\"name\":\"2022\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("pe", "2022", "value", "3"));
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
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{\"name\":\"Period\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"2022\":{\"name\":\"2022\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("pe", "2022", "registrationou", "ImspTQPwCqd", "value", "6"));
  }

  @Test
  public void registrationOuCombinedWithEventOu() throws JSONException {
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
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"2022\":{\"name\":\"2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"wjP19dkFeIk\":{\"uid\":\"wjP19dkFeIk\",\"name\":\"District\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{\"name\":\"Period\"},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"registrationou\":[\"O6uvpzGd5pu\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("ou", "jUb8gELQApl", "pe", "2022", "registrationou", "O6uvpzGd5pu", "value", "1"));
  }

  @Test
  public void registrationOuTableLayoutSingleDistrict() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("tableLayout=true")
            .add("columns=pe")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rows=registrationou")
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
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // metaData not found or is empty in response, skipping assertion.

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Registration org unit",
        "registrationou",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "2022", "2022", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("Registration org unit", "Bo", "2022", "1"));
  }

  @Test
  public void registrationOuTableLayoutRows() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("tableLayout=true")
            .add("columns=pe")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rows=registrationou")
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
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // metaData not found or is empty in response, skipping assertion.

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Registration org unit",
        "registrationou",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "2022", "2022", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("Registration org unit", "Bo", "2022", "1"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("Registration org unit", "Bombali", "2022", "2"));
  }

  @Test
  public void registrationOuTableLayoutColumns() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("tableLayout=true")
            .add("columns=registrationou")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rows=pe")
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
        1,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // metaData not found or is empty in response, skipping assertion.

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "Period", "period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationou Bo",
        "registrationou Bo",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationou Bombali",
        "registrationou Bombali",
        "NUMBER",
        "java.lang.Double",
        false,
        false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Period", "2022", "registrationou Bo", "1", "registrationou Bombali", "2"));
  }

  @Test
  public void registrationOuTableLayoutWithEventOu() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("tableLayout=true")
            .add("columns=pe")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rows=ou;registrationou")
            .add("dimension=pe:2022,ou:jUb8gELQApl,REGISTRATION_OU:O6uvpzGd5pu");

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

    // metaData not found or is empty in response, skipping assertion.

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Organisation unit",
        "organisationunit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Registration org unit",
        "registrationou",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "2022", "2022", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Organisation unit", "Kailahun", "Registration org unit", "Bo", "2022", "1"));
  }

  @Test
  public void registrationOuTableLayoutFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=REGISTRATION_OU:O6uvpzGd5pu")
            .add("tableLayout=true")
            .add("columns=pe")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rows=ou")
            .add("dimension=pe:2022,ou:jUb8gELQApl");

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

    // metaData not found or is empty in response, skipping assertion.

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Organisation unit",
        "organisationunit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "2022", "2022", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("Organisation unit", "Kailahun", "2022", "1"));
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
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{\"name\":\"Period\"},\"ou\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"O6uvpzGd5pu\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("pe", "2022", "registrationou", "OU_264", "value", "1"));
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
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{\"name\":\"Period\"},\"ou\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"O6uvpzGd5pu\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("pe", "2022", "registrationou", "Bo", "value", "1"));
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
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"2022\":{\"name\":\"2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"wjP19dkFeIk\":{\"uid\":\"wjP19dkFeIk\",\"name\":\"District\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{\"name\":\"Period\"},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"registrationou\":[\"O6uvpzGd5pu\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("ou", "OU_204856", "pe", "2022", "registrationou", "OU_264", "value", "1"));
  }

  @Test
  public void registrationOuTableLayoutOutputCode() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("tableLayout=true")
            .add("outputIdScheme=CODE")
            .add("columns=pe")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rows=ou;registrationou")
            .add("dimension=pe:2022,REGISTRATION_OU:O6uvpzGd5pu,ou:jUb8gELQApl");

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

    // metaData not found or is empty in response, skipping assertion.

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Organisation unit",
        "organisationunit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Registration org unit",
        "registrationou",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "2022", "2022", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Organisation unit", "Kailahun", "Registration org unit", "Bo", "2022", "1"));
  }

  @Test
  public void stageOuLevel2NameHierarchy() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("showHierarchy=true")
            .add("dimension=A03MvHHogjR.ou:LEVEL-2,pe:LAST_12_MONTHS")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        156,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"202109\":{\"name\":\"September 2021\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"202107\":{\"name\":\"July 2021\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"wjP19dkFeIk\":{\"name\":\"District\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202112\":{\"name\":\"December 2021\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"202101\":{\"name\":\"January 2021\"},\"202102\":{\"name\":\"February 2021\"},\"pe\":{\"name\":\"Period\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"ouNameHierarchy\":{\"jUb8gELQApl\":\"\\/Sierra Leone\\/Kailahun\",\"TEQlaapDQoK\":\"\\/Sierra Leone\\/Port Loko\",\"eIQbndfxQMb\":\"\\/Sierra Leone\\/Tonkolili\",\"Vth0fbpFcsO\":\"\\/Sierra Leone\\/Kono\",\"PMa2VCrupOd\":\"\\/Sierra Leone\\/Kambia\",\"O6uvpzGd5pu\":\"\\/Sierra Leone\\/Bo\",\"bL4ooGhyHRQ\":\"\\/Sierra Leone\\/Pujehun\",\"kJq2mPyFEHo\":\"\\/Sierra Leone\\/Kenema\",\"fdc6uOvgoji\":\"\\/Sierra Leone\\/Bombali\",\"at6UHUQatSo\":\"\\/Sierra Leone\\/Western Area\",\"lc3eMKXaEfw\":\"\\/Sierra Leone\\/Bonthe\",\"qhqAxPSTUXp\":\"\\/Sierra Leone\\/Koinadugu\",\"jmIPBj66vD6\":\"\\/Sierra Leone\\/Moyamba\"},\"dimensions\":{\"A03MvHHogjR.ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"]}}";
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
        "A03MvHHogjR.ou",
        "Organisation unit",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "lc3eMKXaEfw", "pe", "202103", "value", "56"));

    // Validate row exists with values from original row index 12
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "jUb8gELQApl", "pe", "202112", "value", "58"));

    // Validate row exists with values from original row index 24
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "jUb8gELQApl", "pe", "202107", "value", "60"));

    // Validate row exists with values from original row index 36
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "jUb8gELQApl", "pe", "202105", "value", "54"));

    // Validate row exists with values from original row index 48
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "lc3eMKXaEfw", "pe", "202112", "value", "43"));

    // Validate row exists with values from original row index 60
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "TEQlaapDQoK", "pe", "202106", "value", "80"));

    // Validate row exists with values from original row index 72
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "lc3eMKXaEfw", "pe", "202104", "value", "46"));

    // Validate row exists with values from original row index 84
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "jmIPBj66vD6", "pe", "202103", "value", "91"));

    // Validate row exists with values from original row index 96
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "jmIPBj66vD6", "pe", "202108", "value", "75"));

    // Validate row exists with values from original row index 108
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "Vth0fbpFcsO", "pe", "202106", "value", "71"));

    // Validate row exists with values from original row index 120
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "qhqAxPSTUXp", "pe", "202107", "value", "47"));

    // Validate row exists with values from original row index 132
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "bL4ooGhyHRQ", "pe", "202108", "value", "42"));

    // Validate row exists with values from original row index 144
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "bL4ooGhyHRQ", "pe", "202104", "value", "57"));

    // Validate row exists with values from original row index 155
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "qhqAxPSTUXp", "pe", "202102", "value", "47"));
  }

  @Test
  public void stageOuLevel2Hierarchy() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("hierarchyMeta=true")
            .add("dimension=A03MvHHogjR.ou:LEVEL-2,pe:LAST_12_MONTHS")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        156,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"ouHierarchy\":{\"jUb8gELQApl\":\"ImspTQPwCqd\",\"TEQlaapDQoK\":\"ImspTQPwCqd\",\"eIQbndfxQMb\":\"ImspTQPwCqd\",\"Vth0fbpFcsO\":\"ImspTQPwCqd\",\"PMa2VCrupOd\":\"ImspTQPwCqd\",\"O6uvpzGd5pu\":\"ImspTQPwCqd\",\"bL4ooGhyHRQ\":\"ImspTQPwCqd\",\"kJq2mPyFEHo\":\"ImspTQPwCqd\",\"fdc6uOvgoji\":\"ImspTQPwCqd\",\"at6UHUQatSo\":\"ImspTQPwCqd\",\"lc3eMKXaEfw\":\"ImspTQPwCqd\",\"qhqAxPSTUXp\":\"ImspTQPwCqd\",\"jmIPBj66vD6\":\"ImspTQPwCqd\"},\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"202109\":{\"name\":\"September 2021\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"202107\":{\"name\":\"July 2021\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"wjP19dkFeIk\":{\"name\":\"District\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202112\":{\"name\":\"December 2021\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"202101\":{\"name\":\"January 2021\"},\"202102\":{\"name\":\"February 2021\"},\"pe\":{\"name\":\"Period\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"A03MvHHogjR.ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"]}}";
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
        "A03MvHHogjR.ou",
        "Organisation unit",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "lc3eMKXaEfw", "pe", "202103", "value", "56"));

    // Validate row exists with values from original row index 12
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "jUb8gELQApl", "pe", "202112", "value", "58"));

    // Validate row exists with values from original row index 24
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "jUb8gELQApl", "pe", "202107", "value", "60"));

    // Validate row exists with values from original row index 36
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "jUb8gELQApl", "pe", "202105", "value", "54"));

    // Validate row exists with values from original row index 48
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "lc3eMKXaEfw", "pe", "202112", "value", "43"));

    // Validate row exists with values from original row index 60
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "TEQlaapDQoK", "pe", "202106", "value", "80"));

    // Validate row exists with values from original row index 72
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "lc3eMKXaEfw", "pe", "202104", "value", "46"));

    // Validate row exists with values from original row index 84
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "jmIPBj66vD6", "pe", "202103", "value", "91"));

    // Validate row exists with values from original row index 96
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "jmIPBj66vD6", "pe", "202108", "value", "75"));

    // Validate row exists with values from original row index 108
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "Vth0fbpFcsO", "pe", "202106", "value", "71"));

    // Validate row exists with values from original row index 120
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "qhqAxPSTUXp", "pe", "202107", "value", "47"));

    // Validate row exists with values from original row index 132
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "bL4ooGhyHRQ", "pe", "202108", "value", "42"));

    // Validate row exists with values from original row index 144
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "bL4ooGhyHRQ", "pe", "202104", "value", "57"));

    // Validate row exists with values from original row index 155
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "qhqAxPSTUXp", "pe", "202102", "value", "47"));
  }

  @Test
  public void stageOuFilterLevel2BothHierarchies() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=A03MvHHogjR.ou:LEVEL-2")
            .add("showHierarchy=true")
            .add("hierarchyMeta=true")
            .add("dimension=pe:LAST_12_MONTHS")
            .add("relativePeriodDate=2022-01-01");

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
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"ouHierarchy\":{\"jUb8gELQApl\":\"ImspTQPwCqd\",\"TEQlaapDQoK\":\"ImspTQPwCqd\",\"eIQbndfxQMb\":\"ImspTQPwCqd\",\"Vth0fbpFcsO\":\"ImspTQPwCqd\",\"PMa2VCrupOd\":\"ImspTQPwCqd\",\"O6uvpzGd5pu\":\"ImspTQPwCqd\",\"bL4ooGhyHRQ\":\"ImspTQPwCqd\",\"kJq2mPyFEHo\":\"ImspTQPwCqd\",\"fdc6uOvgoji\":\"ImspTQPwCqd\",\"at6UHUQatSo\":\"ImspTQPwCqd\",\"lc3eMKXaEfw\":\"ImspTQPwCqd\",\"qhqAxPSTUXp\":\"ImspTQPwCqd\",\"jmIPBj66vD6\":\"ImspTQPwCqd\"},\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"202109\":{\"name\":\"September 2021\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"202107\":{\"name\":\"July 2021\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"wjP19dkFeIk\":{\"name\":\"District\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202112\":{\"name\":\"December 2021\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"202101\":{\"name\":\"January 2021\"},\"202102\":{\"name\":\"February 2021\"},\"pe\":{\"name\":\"Period\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"ouNameHierarchy\":{\"jUb8gELQApl\":\"\\/Sierra Leone\\/Kailahun\",\"TEQlaapDQoK\":\"\\/Sierra Leone\\/Port Loko\",\"eIQbndfxQMb\":\"\\/Sierra Leone\\/Tonkolili\",\"Vth0fbpFcsO\":\"\\/Sierra Leone\\/Kono\",\"PMa2VCrupOd\":\"\\/Sierra Leone\\/Kambia\",\"O6uvpzGd5pu\":\"\\/Sierra Leone\\/Bo\",\"bL4ooGhyHRQ\":\"\\/Sierra Leone\\/Pujehun\",\"kJq2mPyFEHo\":\"\\/Sierra Leone\\/Kenema\",\"fdc6uOvgoji\":\"\\/Sierra Leone\\/Bombali\",\"at6UHUQatSo\":\"\\/Sierra Leone\\/Western Area\",\"lc3eMKXaEfw\":\"\\/Sierra Leone\\/Bonthe\",\"qhqAxPSTUXp\":\"\\/Sierra Leone\\/Koinadugu\",\"jmIPBj66vD6\":\"\\/Sierra Leone\\/Moyamba\"},\"dimensions\":{\"A03MvHHogjR.ou\":[\"in LEVEL-2\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"]}}";
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
    validateRowExists(response, actualHeaders, Map.of("pe", "202107", "value", "951"));

    // Validate row exists with values from original row index 3
    validateRowExists(response, actualHeaders, Map.of("pe", "202104", "value", "933"));

    // Validate row exists with values from original row index 6
    validateRowExists(response, actualHeaders, Map.of("pe", "202102", "value", "841"));

    // Validate row exists with values from original row index 9
    validateRowExists(response, actualHeaders, Map.of("pe", "202109", "value", "838"));

    // Validate row exists with values from original row index 11
    validateRowExists(response, actualHeaders, Map.of("pe", "202101", "value", "949"));
  }

  @Test
  public void valueAlsoLegendSetDimension() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:ImspTQPwCqd")
            .add("aggregationType=AVERAGE")
            .add("stage=Zj7UnCAulEk")
            .add("dimension=pe:LAST_12_MONTHS,Zj7UnCAulEk.qrur9Dvnyt5-Yf6UHoPkdS6")
            .add("value=Zj7UnCAulEk.qrur9Dvnyt5")
            .add("relativePeriodDate=2023-01-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        109,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"202208\":{\"name\":\"August 2022\"},\"OyVUzWsX8UF\":{\"name\":\"10 - 20\"},\"202209\":{\"name\":\"September 2022\"},\"202206\":{\"name\":\"June 2022\"},\"qrur9Dvnyt5\":{\"name\":\"Age in years\"},\"202207\":{\"name\":\"July 2022\"},\"pZzk1L4Blf1\":{\"name\":\"0 - 10\"},\"202204\":{\"name\":\"April 2022\"},\"202205\":{\"name\":\"May 2022\"},\"202202\":{\"name\":\"February 2022\"},\"202203\":{\"name\":\"March 2022\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"b7MCpzqJaR2\":{\"name\":\"70 - 80\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202210\":{\"name\":\"October 2022\"},\"Tq4NYCn9eNH\":{\"name\":\"60 - 70\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{\"name\":\"Organisation unit\"},\"CpP5yzbgfHo\":{\"name\":\"40 - 50\"},\"scvmgP9F9rn\":{\"name\":\"90 - 100\"},\"202201\":{\"name\":\"January 2022\"},\"cbPqyIAFw9u\":{\"name\":\"50 - 60\"},\"TvM2MQgD7Jd\":{\"name\":\"20 - 30\"},\"pe\":{\"name\":\"Period\"},\"Zj7UnCAulEk.qrur9Dvnyt5\":{\"name\":\"Age in years\"},\"puI3YpLJ3fC\":{\"name\":\"80 - 90\"},\"ZUUGJnvX40X\":{\"name\":\"30 - 40\"}},\"dimensions\":{\"pe\":[\"202201\",\"202202\",\"202203\",\"202204\",\"202205\",\"202206\",\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\"],\"Zj7UnCAulEk.qrur9Dvnyt5\":[\"pZzk1L4Blf1\",\"OyVUzWsX8UF\",\"TvM2MQgD7Jd\",\"ZUUGJnvX40X\",\"CpP5yzbgfHo\",\"cbPqyIAFw9u\",\"Tq4NYCn9eNH\",\"b7MCpzqJaR2\",\"puI3YpLJ3fC\",\"scvmgP9F9rn\"],\"ou\":[\"ImspTQPwCqd\"]}}";
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
        "Zj7UnCAulEk.qrur9Dvnyt5",
        "Age in years",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Zj7UnCAulEk.qrur9Dvnyt5", "pZzk1L4Blf1", "pe", "202208", "value", "4.46"));

    // Validate row exists with values from original row index 10
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Zj7UnCAulEk.qrur9Dvnyt5", "ZUUGJnvX40X", "pe", "202211", "value", "34.65"));

    // Validate row exists with values from original row index 20
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Zj7UnCAulEk.qrur9Dvnyt5", "Tq4NYCn9eNH", "pe", "202208", "value", "64.48"));

    // Validate row exists with values from original row index 30
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Zj7UnCAulEk.qrur9Dvnyt5", "b7MCpzqJaR2", "pe", "202201", "value", "74.6"));

    // Validate row exists with values from original row index 40
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Zj7UnCAulEk.qrur9Dvnyt5", "CpP5yzbgfHo", "pe", "202201", "value", "44.42"));

    // Validate row exists with values from original row index 50
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Zj7UnCAulEk.qrur9Dvnyt5", "puI3YpLJ3fC", "pe", "202205", "value", "84.22"));

    // Validate row exists with values from original row index 60
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Zj7UnCAulEk.qrur9Dvnyt5", "Tq4NYCn9eNH", "pe", "202206", "value", "64.61"));

    // Validate row exists with values from original row index 70
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Zj7UnCAulEk.qrur9Dvnyt5", "puI3YpLJ3fC", "pe", "202203", "value", "83.88"));

    // Validate row exists with values from original row index 80
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Zj7UnCAulEk.qrur9Dvnyt5", "Tq4NYCn9eNH", "pe", "202212", "value", "64.66"));

    // Validate row exists with values from original row index 90
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Zj7UnCAulEk.qrur9Dvnyt5", "pZzk1L4Blf1", "pe", "202204", "value", "4.5"));

    // Validate row exists with values from original row index 100
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Zj7UnCAulEk.qrur9Dvnyt5", "puI3YpLJ3fC", "pe", "202202", "value", "84.25"));

    // Validate row exists with values from original row index 108
    validateRowExists(
        response,
        actualHeaders,
        Map.of("Zj7UnCAulEk.qrur9Dvnyt5", "puI3YpLJ3fC", "pe", "202210", "value", "84.0"));
  }
}
