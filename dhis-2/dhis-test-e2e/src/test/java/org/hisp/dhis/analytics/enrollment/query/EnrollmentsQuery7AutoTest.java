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
package org.hisp.dhis.analytics.enrollment.query;

import static org.hisp.dhis.analytics.ValidationHelper.*;
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

/** Groups e2e tests for "/enrollments/query" endpoint. */
public class EnrollmentsQuery7AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void stageAndEventDate2021() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,enrollmentdate,A03MvHHogjR.eventdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.EVENT_DATE:2021")
            .add("desc=enrollmentdate");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(response, expectPostgis, 100, 3, 3);

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"isLastPage\":false,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\",\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.EVENT_DATE\":{\"name\":\"Report date, Birth\"}},\"dimensions\":{\"A03MvHHogjR.occurreddate\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    System.out.println(actualMetaData);
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
        "enrollmentdate",
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
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
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mambiama CHP");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.eventdate", "2021-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2022-12-29 12:05:00.0");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "ouname", "Geima CHP");
    validateRowValueByName(
        response, actualHeaders, 99, "A03MvHHogjR.eventdate", "2021-12-26 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 99, "enrollmentdate", "2022-12-26 12:05:00.0");
  }

  @Test
  public void stageAndScheduledDate() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,enrollmentdate,A03MvHHogjR.scheduleddate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ou:ImspTQPwCqd,A03MvHHogjR.SCHEDULED_DATE:2021")
            .add("desc=enrollmentdate,ouname");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        100,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR.SCHEDULED_DATE\":{\"name\":\"Scheduled date, Birth\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.scheduleddate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
        "enrollmentdate",
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.scheduleddate",
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
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Youndu CHP");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.scheduleddate", "2021-12-29 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2022-12-29 12:05:00.0");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "ouname", "MCH Static/U5");
    validateRowValueByName(
        response, actualHeaders, 99, "A03MvHHogjR.scheduleddate", "2021-12-26 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 99, "enrollmentdate", "2022-12-26 12:05:00.0");
  }

  @Test
  public void stageAndSimpleOu() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,enrollmentdate,ZzYYXq4fJie.ouname")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZzYYXq4fJie.ou:DiszpKrYNg8,pe:202301")
            .add("desc=enrollmentdate,ouname");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        7,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"uid\":\"DiszpKrYNg8\",\"code\":\"OU_559\",\"name\":\"Ngelehun CHC\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ZzYYXq4fJie.ou\":{\"name\":\"Organisation unit, Baby Postnatal\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"202301\":{\"name\":\"January 2023\"}},\"dimensions\":{\"ZzYYXq4fJie.ou\":[\"DiszpKrYNg8\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
        "enrollmentdate",
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZzYYXq4fJie.ouname",
        "Organisation unit name",
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
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "ZzYYXq4fJie.ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2023-01-19 12:05:00.0");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 6, "ZzYYXq4fJie.ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 6, "enrollmentdate", "2023-01-01 00:00:00.0");
  }

  @Test
  public void stageAndMultipleOu() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,enrollmentdate,ZzYYXq4fJie.ou")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZzYYXq4fJie.ou:DiszpKrYNg8;PMa2VCrupOd,pe:202301")
            .add("desc=enrollmentdate,ouname");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        45,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"uid\":\"DiszpKrYNg8\",\"code\":\"OU_559\",\"name\":\"Ngelehun CHC\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ZzYYXq4fJie.ou\":{\"name\":\"Organisation unit, Baby Postnatal\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"PMa2VCrupOd\":{\"uid\":\"PMa2VCrupOd\",\"code\":\"OU_211212\",\"name\":\"Kambia\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"202301\":{\"name\":\"January 2023\"}},\"dimensions\":{\"ZzYYXq4fJie.ou\":[\"DiszpKrYNg8\",\"PMa2VCrupOd\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
        "enrollmentdate",
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZzYYXq4fJie.ou",
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
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Kamassasa CHC");
    validateRowValueByName(response, actualHeaders, 0, "ZzYYXq4fJie.ou", "PMa2VCrupOd");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2023-01-31 12:05:00.0");

    // Validate selected values for row index 44
    validateRowValueByName(response, actualHeaders, 44, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 44, "ZzYYXq4fJie.ou", "DiszpKrYNg8");
    validateRowValueByName(response, actualHeaders, 44, "enrollmentdate", "2023-01-01 00:00:00.0");
  }

  @Test
  public void stageAndLevelAndUserOrg() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,enrollmentdate,ZzYYXq4fJie.oucode,ZzYYXq4fJie.ouname")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZzYYXq4fJie.ou:LEVEL-tTUf91fCytl;USER_ORGUNIT;cgqkFdShPzg,pe:202301")
            .add("desc=enrollmentdate,ouname");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        100,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"yu4N82FFeLm\":{\"uid\":\"yu4N82FFeLm\",\"code\":\"OU_204910\",\"name\":\"Mandu\"},\"KXSqt7jv6DU\":{\"uid\":\"KXSqt7jv6DU\",\"code\":\"OU_222627\",\"name\":\"Gorama Mende\"},\"lY93YpCxJqf\":{\"uid\":\"lY93YpCxJqf\",\"code\":\"OU_193249\",\"name\":\"Makari Gbanti\"},\"eROJsBwxQHt\":{\"uid\":\"eROJsBwxQHt\",\"code\":\"OU_222743\",\"name\":\"Gaura\"},\"DxAPPqXvwLy\":{\"uid\":\"DxAPPqXvwLy\",\"code\":\"OU_204929\",\"name\":\"Peje Bongre\"},\"PaqugoqjRIj\":{\"uid\":\"PaqugoqjRIj\",\"code\":\"OU_226225\",\"name\":\"Sulima (Koinadugu)\"},\"gy8rmvYT4cj\":{\"uid\":\"gy8rmvYT4cj\",\"code\":\"OU_247037\",\"name\":\"Ribbi\"},\"RzKeCma9qb1\":{\"uid\":\"RzKeCma9qb1\",\"code\":\"OU_260428\",\"name\":\"Barri\"},\"vULnao2hV5v\":{\"uid\":\"vULnao2hV5v\",\"code\":\"OU_247086\",\"name\":\"Fakunya\"},\"EfWCa0Cc8WW\":{\"uid\":\"EfWCa0Cc8WW\",\"code\":\"OU_255030\",\"name\":\"Masimera\"},\"AovmOHadayb\":{\"uid\":\"AovmOHadayb\",\"code\":\"OU_247044\",\"name\":\"Timidale\"},\"hjpHnHZIniP\":{\"uid\":\"hjpHnHZIniP\",\"code\":\"OU_204887\",\"name\":\"Kissi Tongi\"},\"U6Kr7Gtpidn\":{\"uid\":\"U6Kr7Gtpidn\",\"code\":\"OU_546\",\"name\":\"Kakua\"},\"EYt6ThQDagn\":{\"uid\":\"EYt6ThQDagn\",\"code\":\"OU_222642\",\"name\":\"Koya (kenema)\"},\"iUauWFeH8Qp\":{\"uid\":\"iUauWFeH8Qp\",\"code\":\"OU_197402\",\"name\":\"Bum\"},\"Jiyc4ekaMMh\":{\"uid\":\"Jiyc4ekaMMh\",\"code\":\"OU_247080\",\"name\":\"Kongbora\"},\"FlBemv1NfEC\":{\"uid\":\"FlBemv1NfEC\",\"code\":\"OU_211256\",\"name\":\"Masungbala\"},\"XrF5AvaGcuw\":{\"uid\":\"XrF5AvaGcuw\",\"code\":\"OU_226240\",\"name\":\"Wara Wara Bafodia\"},\"LhaAPLxdSFH\":{\"uid\":\"LhaAPLxdSFH\",\"code\":\"OU_233348\",\"name\":\"Lei\"},\"BmYyh9bZ0sr\":{\"uid\":\"BmYyh9bZ0sr\",\"code\":\"OU_268197\",\"name\":\"Kafe Simira\"},\"pk7bUK5c1Uf\":{\"uid\":\"pk7bUK5c1Uf\",\"code\":\"OU_260397\",\"name\":\"Ya Kpukumu Krim\"},\"zSNUViKdkk3\":{\"uid\":\"zSNUViKdkk3\",\"code\":\"OU_260440\",\"name\":\"Kpaka\"},\"r06ohri9wA9\":{\"uid\":\"r06ohri9wA9\",\"code\":\"OU_211243\",\"name\":\"Samu\"},\"ERmBhYkhV6Y\":{\"uid\":\"ERmBhYkhV6Y\",\"code\":\"OU_204877\",\"name\":\"Njaluahun\"},\"Z9QaI6sxTwW\":{\"uid\":\"Z9QaI6sxTwW\",\"code\":\"OU_247068\",\"name\":\"Kargboro\"},\"daJPPxtIrQn\":{\"uid\":\"daJPPxtIrQn\",\"code\":\"OU_545\",\"name\":\"Jaiama Bongor\"},\"W5fN3G6y1VI\":{\"uid\":\"W5fN3G6y1VI\",\"code\":\"OU_247012\",\"name\":\"Lower Banta\"},\"r1RUyfVBkLp\":{\"uid\":\"r1RUyfVBkLp\",\"code\":\"OU_268169\",\"name\":\"Sambaia Bendugu\"},\"cgqkFdShPzg\":{\"uid\":\"cgqkFdShPzg\",\"code\":\"OU_193213\",\"name\":\"Loreto Clinic\"},\"NNE0YMCDZkO\":{\"uid\":\"NNE0YMCDZkO\",\"code\":\"OU_268225\",\"name\":\"Yoni\"},\"ENHOJz3UH5L\":{\"uid\":\"ENHOJz3UH5L\",\"code\":\"OU_197440\",\"name\":\"BMC\"},\"QywkxFudXrC\":{\"uid\":\"QywkxFudXrC\",\"code\":\"OU_211227\",\"name\":\"Magbema\"},\"jWSIbtKfURj\":{\"uid\":\"jWSIbtKfURj\",\"code\":\"OU_222751\",\"name\":\"Langrama\"},\"J4GiUImJZoE\":{\"uid\":\"J4GiUImJZoE\",\"code\":\"OU_226269\",\"name\":\"Nieni\"},\"CF243RPvNY7\":{\"uid\":\"CF243RPvNY7\",\"code\":\"OU_233359\",\"name\":\"Fiama\"},\"I4jWcnFmgEC\":{\"uid\":\"I4jWcnFmgEC\",\"code\":\"OU_549\",\"name\":\"Niawa Lenga\"},\"cM2BKSrj9F9\":{\"uid\":\"cM2BKSrj9F9\",\"code\":\"OU_204894\",\"name\":\"Luawa\"},\"kvkDWg42lHR\":{\"uid\":\"kvkDWg42lHR\",\"code\":\"OU_233339\",\"name\":\"Kamara\"},\"jPidqyo7cpF\":{\"uid\":\"jPidqyo7cpF\",\"code\":\"OU_247049\",\"name\":\"Bagruwa\"},\"BGGmAwx33dj\":{\"uid\":\"BGGmAwx33dj\",\"code\":\"OU_543\",\"name\":\"Bumpe Ngao\"},\"iGHlidSFdpu\":{\"uid\":\"iGHlidSFdpu\",\"code\":\"OU_233317\",\"name\":\"Soa\"},\"g8DdBm7EmUt\":{\"uid\":\"g8DdBm7EmUt\",\"code\":\"OU_197397\",\"name\":\"Sittia\"},\"ZiOVcrSjSYe\":{\"uid\":\"ZiOVcrSjSYe\",\"code\":\"OU_254976\",\"name\":\"Dibia\"},\"vn9KJsLyP5f\":{\"uid\":\"vn9KJsLyP5f\",\"code\":\"OU_255005\",\"name\":\"Kaffu Bullom\"},\"QlCIp2S9NHs\":{\"uid\":\"QlCIp2S9NHs\",\"code\":\"OU_222682\",\"name\":\"Dodo\"},\"j43EZb15rjI\":{\"uid\":\"j43EZb15rjI\",\"code\":\"OU_193285\",\"name\":\"Sella Limba\"},\"202301\":{\"name\":\"January 2023\"},\"bQiBfA2j5cw\":{\"uid\":\"bQiBfA2j5cw\",\"code\":\"OU_204857\",\"name\":\"Penguia\"},\"NqWaKXcg01b\":{\"uid\":\"NqWaKXcg01b\",\"code\":\"OU_260384\",\"name\":\"Sowa\"},\"VP397wRvePm\":{\"uid\":\"VP397wRvePm\",\"code\":\"OU_197445\",\"name\":\"Nongoba Bullum\"},\"fwxkctgmffZ\":{\"uid\":\"fwxkctgmffZ\",\"code\":\"OU_268163\",\"name\":\"Kholifa Mabang\"},\"QwMiPiME3bA\":{\"uid\":\"QwMiPiME3bA\",\"code\":\"OU_260400\",\"name\":\"Kpanga Kabonde\"},\"Qhmi8IZyPyD\":{\"uid\":\"Qhmi8IZyPyD\",\"code\":\"OU_193245\",\"name\":\"Tambaka\"},\"OTFepb1k9Db\":{\"uid\":\"OTFepb1k9Db\",\"code\":\"OU_226244\",\"name\":\"Mongo\"},\"DBs6e2Oxaj1\":{\"uid\":\"DBs6e2Oxaj1\",\"code\":\"OU_247002\",\"name\":\"Upper Banta\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"eV4cuxniZgP\":{\"uid\":\"eV4cuxniZgP\",\"code\":\"OU_193224\",\"name\":\"Magbaimba Ndowahun\"},\"xhyjU2SVewz\":{\"uid\":\"xhyjU2SVewz\",\"code\":\"OU_268217\",\"name\":\"Tane\"},\"dGheVylzol6\":{\"uid\":\"dGheVylzol6\",\"code\":\"OU_541\",\"name\":\"Bargbe\"},\"vWbkYPRmKyS\":{\"uid\":\"vWbkYPRmKyS\",\"code\":\"OU_540\",\"name\":\"Baoma\"},\"npWGUj37qDe\":{\"uid\":\"npWGUj37qDe\",\"code\":\"OU_552\",\"name\":\"Valunia\"},\"TA7NvKjsn4A\":{\"uid\":\"TA7NvKjsn4A\",\"code\":\"OU_255041\",\"name\":\"Bureh Kasseh Maconteh\"},\"ZzYYXq4fJie.ou\":{\"name\":\"Organisation unit, Baby Postnatal\"},\"tTUf91fCytl\":{\"uid\":\"tTUf91fCytl\",\"name\":\"Chiefdom\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"myQ4q1W6B4y\":{\"uid\":\"myQ4q1W6B4y\",\"code\":\"OU_222731\",\"name\":\"Dama\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"nV3OkyzF4US\":{\"uid\":\"nV3OkyzF4US\",\"code\":\"OU_246991\",\"name\":\"Kori\"},\"X7dWcGerQIm\":{\"uid\":\"X7dWcGerQIm\",\"code\":\"OU_222677\",\"name\":\"Wandor\"},\"qIRCo0MfuGb\":{\"uid\":\"qIRCo0MfuGb\",\"code\":\"OU_211213\",\"name\":\"Gbinleh Dixion\"},\"kbPmt60yi0L\":{\"uid\":\"kbPmt60yi0L\",\"code\":\"OU_211220\",\"name\":\"Bramaia\"},\"eNtRuQrrZeo\":{\"uid\":\"eNtRuQrrZeo\",\"code\":\"OU_260420\",\"name\":\"Galliness Perri\"},\"HV8RTzgcFH3\":{\"uid\":\"HV8RTzgcFH3\",\"code\":\"OU_197432\",\"name\":\"Kwamabai Krim\"},\"KKkLOTpMXGV\":{\"uid\":\"KKkLOTpMXGV\",\"code\":\"OU_193198\",\"name\":\"Bombali Sebora\"},\"Pc3JTyqnsmL\":{\"uid\":\"Pc3JTyqnsmL\",\"code\":\"OU_255020\",\"name\":\"Buya Romende\"},\"hdEuw2ugkVF\":{\"uid\":\"hdEuw2ugkVF\",\"code\":\"OU_222652\",\"name\":\"Lower Bambara\"},\"l7pFejMtUoF\":{\"uid\":\"l7pFejMtUoF\",\"code\":\"OU_222634\",\"name\":\"Tunkia\"},\"K1r3uF6eZ8n\":{\"uid\":\"K1r3uF6eZ8n\",\"code\":\"OU_222725\",\"name\":\"Kandu Lepiema\"},\"VGAFxBXz16y\":{\"uid\":\"VGAFxBXz16y\",\"code\":\"OU_226231\",\"name\":\"Sengbeh\"},\"GE25DpSrqpB\":{\"uid\":\"GE25DpSrqpB\",\"code\":\"OU_204869\",\"name\":\"Malema\"},\"ARZ4y5i4reU\":{\"uid\":\"ARZ4y5i4reU\",\"code\":\"OU_553\",\"name\":\"Wonde\"},\"byp7w6Xd9Df\":{\"uid\":\"byp7w6Xd9Df\",\"code\":\"OU_204933\",\"name\":\"Yawei\"},\"BXJdOLvUrZB\":{\"uid\":\"BXJdOLvUrZB\",\"code\":\"OU_193277\",\"name\":\"Gbendembu Ngowahun\"},\"uKC54fzxRzO\":{\"uid\":\"uKC54fzxRzO\",\"code\":\"OU_222648\",\"name\":\"Niawa\"},\"TQkG0sX9nca\":{\"uid\":\"TQkG0sX9nca\",\"code\":\"OU_233375\",\"name\":\"Gbense\"},\"hRZOIgQ0O1m\":{\"uid\":\"hRZOIgQ0O1m\",\"code\":\"OU_193302\",\"name\":\"Libeisaygahun\"},\"PrJQHI6q7w2\":{\"uid\":\"PrJQHI6q7w2\",\"code\":\"OU_255061\",\"name\":\"Tainkatopa Makama Safrokoh\"},\"USQdmvrHh1Q\":{\"uid\":\"USQdmvrHh1Q\",\"code\":\"OU_247055\",\"name\":\"Kaiyamba\"},\"xGMGhjA3y6J\":{\"uid\":\"xGMGhjA3y6J\",\"code\":\"OU_211262\",\"name\":\"Mambolo\"},\"nOYt1LtFSyU\":{\"uid\":\"nOYt1LtFSyU\",\"code\":\"OU_247025\",\"name\":\"Bumpeh\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"e1eIKM1GIF3\":{\"uid\":\"e1eIKM1GIF3\",\"code\":\"OU_193215\",\"name\":\"Gbanti Kamaranka\"},\"lYIM1MXbSYS\":{\"uid\":\"lYIM1MXbSYS\",\"code\":\"OU_204920\",\"name\":\"Dea\"},\"sxRd2XOzFbz\":{\"uid\":\"sxRd2XOzFbz\",\"code\":\"OU_551\",\"name\":\"Tikonko\"},\"d9iMR1MpuIO\":{\"uid\":\"d9iMR1MpuIO\",\"code\":\"OU_260410\",\"name\":\"Soro-Gbeima\"},\"qgQ49DH9a0v\":{\"uid\":\"qgQ49DH9a0v\",\"code\":\"OU_233332\",\"name\":\"Nimiyama\"},\"U09TSwIjG0s\":{\"uid\":\"U09TSwIjG0s\",\"code\":\"OU_222617\",\"name\":\"Nomo\"},\"zFDYIgyGmXG\":{\"uid\":\"zFDYIgyGmXG\",\"code\":\"OU_542\",\"name\":\"Bargbo\"},\"M2qEv692lS6\":{\"uid\":\"M2qEv692lS6\",\"code\":\"OU_233324\",\"name\":\"Tankoro\"},\"qtr8GGlm4gg\":{\"uid\":\"qtr8GGlm4gg\",\"code\":\"OU_278366\",\"name\":\"Rural Western Area\"},\"pRHGAROvuyI\":{\"uid\":\"pRHGAROvuyI\",\"code\":\"OU_254960\",\"name\":\"Koya\"},\"xIKjidMrico\":{\"uid\":\"xIKjidMrico\",\"code\":\"OU_247033\",\"name\":\"Kowa\"},\"GWTIxJO9pRo\":{\"uid\":\"GWTIxJO9pRo\",\"code\":\"OU_233355\",\"name\":\"Gorama Kono\"},\"HWjrSuoNPte\":{\"uid\":\"HWjrSuoNPte\",\"code\":\"OU_254999\",\"name\":\"Sanda Magbolonthor\"},\"UhHipWG7J8b\":{\"uid\":\"UhHipWG7J8b\",\"code\":\"OU_193191\",\"name\":\"Sanda Tendaren\"},\"rXLor9Knq6l\":{\"uid\":\"rXLor9Knq6l\",\"code\":\"OU_268212\",\"name\":\"Kunike Barina\"},\"kU8vhUkAGaT\":{\"uid\":\"kU8vhUkAGaT\",\"code\":\"OU_548\",\"name\":\"Lugbu\"},\"C9uduqDZr9d\":{\"uid\":\"C9uduqDZr9d\",\"code\":\"OU_278311\",\"name\":\"Freetown\"},\"YmmeuGbqOwR\":{\"uid\":\"YmmeuGbqOwR\",\"code\":\"OU_544\",\"name\":\"Gbo\"},\"iEkBZnMDarP\":{\"uid\":\"iEkBZnMDarP\",\"code\":\"OU_226253\",\"name\":\"Folosaba Dembelia\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"JsxnA2IywRo\":{\"uid\":\"JsxnA2IywRo\",\"code\":\"OU_204875\",\"name\":\"Kissi Kama\"},\"nlt6j60tCHF\":{\"uid\":\"nlt6j60tCHF\",\"code\":\"OU_260437\",\"name\":\"Mano Sakrim\"},\"g5ptsn0SFX8\":{\"uid\":\"g5ptsn0SFX8\",\"code\":\"OU_233365\",\"name\":\"Sandor\"},\"XG8HGAbrbbL\":{\"uid\":\"XG8HGAbrbbL\",\"code\":\"OU_193267\",\"name\":\"Safroko Limba\"},\"pmxZm7klXBy\":{\"uid\":\"pmxZm7klXBy\",\"code\":\"OU_204924\",\"name\":\"Peje West\"},\"l0ccv2yzfF3\":{\"uid\":\"l0ccv2yzfF3\",\"code\":\"OU_268174\",\"name\":\"Kunike\"},\"YuQRtpLP10I\":{\"uid\":\"YuQRtpLP10I\",\"code\":\"OU_539\",\"name\":\"Badjia\"},\"LfTkc0S4b5k\":{\"uid\":\"LfTkc0S4b5k\",\"code\":\"OU_204915\",\"name\":\"Upper Bambara\"},\"KSdZwrU7Hh6\":{\"uid\":\"KSdZwrU7Hh6\",\"code\":\"OU_204861\",\"name\":\"Jawi\"},\"ajILkI0cfxn\":{\"uid\":\"ajILkI0cfxn\",\"code\":\"OU_233390\",\"name\":\"Gbane\"},\"y5X4mP5XylL\":{\"uid\":\"y5X4mP5XylL\",\"code\":\"OU_211270\",\"name\":\"Tonko Limba\"},\"fwH9ipvXde9\":{\"uid\":\"fwH9ipvXde9\",\"code\":\"OU_193228\",\"name\":\"Biriwa\"},\"KIUCimTXf8Q\":{\"uid\":\"KIUCimTXf8Q\",\"code\":\"OU_222690\",\"name\":\"Nongowa\"},\"vEvs2ckGNQj\":{\"uid\":\"vEvs2ckGNQj\",\"code\":\"OU_226219\",\"name\":\"Kasonko\"},\"XEyIRFd9pct\":{\"uid\":\"XEyIRFd9pct\",\"code\":\"OU_197413\",\"name\":\"Imperi\"},\"cgOy0hRMGu9\":{\"uid\":\"cgOy0hRMGu9\",\"code\":\"OU_197408\",\"name\":\"Sogbini\"},\"EjnIQNVAXGp\":{\"uid\":\"EjnIQNVAXGp\",\"code\":\"OU_233344\",\"name\":\"Mafindor\"},\"x4HaBHHwBML\":{\"uid\":\"x4HaBHHwBML\",\"code\":\"OU_222672\",\"name\":\"Malegohun\"},\"EZPwuUTeIIG\":{\"uid\":\"EZPwuUTeIIG\",\"code\":\"OU_226258\",\"name\":\"Wara Wara Yagala\"},\"N233eZJZ1bh\":{\"uid\":\"N233eZJZ1bh\",\"code\":\"OU_260388\",\"name\":\"Pejeh\"},\"smoyi1iYNK6\":{\"uid\":\"smoyi1iYNK6\",\"code\":\"OU_268191\",\"name\":\"Kalansogoia\"},\"DNRAeXT9IwS\":{\"uid\":\"DNRAeXT9IwS\",\"code\":\"OU_197421\",\"name\":\"Dema\"},\"fRLX08WHWpL\":{\"uid\":\"fRLX08WHWpL\",\"code\":\"OU_254982\",\"name\":\"Lokomasama\"},\"Zoy23SSHCPs\":{\"uid\":\"Zoy23SSHCPs\",\"code\":\"OU_233311\",\"name\":\"Gbane Kandor\"},\"LsYpCyYxSLY\":{\"uid\":\"LsYpCyYxSLY\",\"code\":\"OU_247008\",\"name\":\"Kamaje\"},\"JdqfYTIFZXN\":{\"uid\":\"JdqfYTIFZXN\",\"code\":\"OU_254946\",\"name\":\"Maforki\"},\"EB1zRKdYjdY\":{\"uid\":\"EB1zRKdYjdY\",\"code\":\"OU_197429\",\"name\":\"Bendu Cha\"},\"CG4QD1HC3h4\":{\"uid\":\"CG4QD1HC3h4\",\"code\":\"OU_197436\",\"name\":\"Yawbeko\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\"},\"YpVol7asWvd\":{\"uid\":\"YpVol7asWvd\",\"code\":\"OU_260417\",\"name\":\"Kpanga Krim\"},\"RndxKqQGzUl\":{\"uid\":\"RndxKqQGzUl\",\"code\":\"OU_247018\",\"name\":\"Dasse\"},\"P69SId31eDp\":{\"uid\":\"P69SId31eDp\",\"code\":\"OU_268202\",\"name\":\"Gbonkonlenken\"},\"aWQTfvgPA5v\":{\"uid\":\"aWQTfvgPA5v\",\"code\":\"OU_197424\",\"name\":\"Kpanda Kemoh\"},\"KctpIIucige\":{\"uid\":\"KctpIIucige\",\"code\":\"OU_550\",\"name\":\"Selenga\"},\"Mr4au3jR9bt\":{\"uid\":\"Mr4au3jR9bt\",\"code\":\"OU_226214\",\"name\":\"Dembelia Sinkunia\"},\"L8iA6eLwKNb\":{\"uid\":\"L8iA6eLwKNb\",\"code\":\"OU_193295\",\"name\":\"Paki Masabong\"},\"j0Mtr3xTMjM\":{\"uid\":\"j0Mtr3xTMjM\",\"code\":\"OU_204939\",\"name\":\"Kissi Teng\"},\"DmaLM8WYmWv\":{\"uid\":\"DmaLM8WYmWv\",\"code\":\"OU_233394\",\"name\":\"Nimikoro\"},\"DfUfwjM9am5\":{\"uid\":\"DfUfwjM9am5\",\"code\":\"OU_260392\",\"name\":\"Malen\"},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"FRxcUEwktoV\":{\"uid\":\"FRxcUEwktoV\",\"code\":\"OU_233314\",\"name\":\"Toli\"},\"VCtF1DbspR5\":{\"uid\":\"VCtF1DbspR5\",\"code\":\"OU_197386\",\"name\":\"Jong\"},\"BD9gU0GKlr2\":{\"uid\":\"BD9gU0GKlr2\",\"code\":\"OU_260378\",\"name\":\"Makpele\"},\"GFk45MOxzJJ\":{\"uid\":\"GFk45MOxzJJ\",\"code\":\"OU_226275\",\"name\":\"Neya\"},\"A3Fh37HWBWE\":{\"uid\":\"A3Fh37HWBWE\",\"code\":\"OU_222687\",\"name\":\"Simbaru\"},\"vzup1f6ynON\":{\"uid\":\"vzup1f6ynON\",\"code\":\"OU_222619\",\"name\":\"Small Bo\"},\"Lt8U7GVWvSR\":{\"uid\":\"Lt8U7GVWvSR\",\"code\":\"OU_226263\",\"name\":\"Diang\"},\"JdhagCUEMbj\":{\"uid\":\"JdhagCUEMbj\",\"code\":\"OU_547\",\"name\":\"Komboya\"},\"EVkm2xYcf6Z\":{\"uid\":\"EVkm2xYcf6Z\",\"code\":\"OU_268184\",\"name\":\"Malal Mara\"},\"PQZJPIpTepd\":{\"uid\":\"PQZJPIpTepd\",\"code\":\"OU_268150\",\"name\":\"Kholifa Rowalla\"},\"WXnNDWTiE9r\":{\"uid\":\"WXnNDWTiE9r\",\"code\":\"OU_193239\",\"name\":\"Sanda Loko\"},\"RWvG1aFrr0r\":{\"uid\":\"RWvG1aFrr0r\",\"code\":\"OU_255053\",\"name\":\"Marampa\"}},\"dimensions\":{\"ZzYYXq4fJie.ou\":[\"ImspTQPwCqd\",\"cgqkFdShPzg\",\"YuQRtpLP10I\",\"jPidqyo7cpF\",\"vWbkYPRmKyS\",\"dGheVylzol6\",\"zFDYIgyGmXG\",\"RzKeCma9qb1\",\"EB1zRKdYjdY\",\"fwH9ipvXde9\",\"ENHOJz3UH5L\",\"KKkLOTpMXGV\",\"kbPmt60yi0L\",\"iUauWFeH8Qp\",\"BGGmAwx33dj\",\"nOYt1LtFSyU\",\"TA7NvKjsn4A\",\"Pc3JTyqnsmL\",\"myQ4q1W6B4y\",\"RndxKqQGzUl\",\"lYIM1MXbSYS\",\"DNRAeXT9IwS\",\"Mr4au3jR9bt\",\"Lt8U7GVWvSR\",\"ZiOVcrSjSYe\",\"QlCIp2S9NHs\",\"vULnao2hV5v\",\"CF243RPvNY7\",\"iEkBZnMDarP\",\"C9uduqDZr9d\",\"eNtRuQrrZeo\",\"eROJsBwxQHt\",\"ajILkI0cfxn\",\"Zoy23SSHCPs\",\"e1eIKM1GIF3\",\"BXJdOLvUrZB\",\"TQkG0sX9nca\",\"qIRCo0MfuGb\",\"YmmeuGbqOwR\",\"P69SId31eDp\",\"GWTIxJO9pRo\",\"KXSqt7jv6DU\",\"XEyIRFd9pct\",\"daJPPxtIrQn\",\"KSdZwrU7Hh6\",\"VCtF1DbspR5\",\"BmYyh9bZ0sr\",\"vn9KJsLyP5f\",\"USQdmvrHh1Q\",\"U6Kr7Gtpidn\",\"smoyi1iYNK6\",\"LsYpCyYxSLY\",\"kvkDWg42lHR\",\"K1r3uF6eZ8n\",\"Z9QaI6sxTwW\",\"vEvs2ckGNQj\",\"fwxkctgmffZ\",\"PQZJPIpTepd\",\"JsxnA2IywRo\",\"j0Mtr3xTMjM\",\"hjpHnHZIniP\",\"JdhagCUEMbj\",\"Jiyc4ekaMMh\",\"nV3OkyzF4US\",\"xIKjidMrico\",\"pRHGAROvuyI\",\"EYt6ThQDagn\",\"zSNUViKdkk3\",\"aWQTfvgPA5v\",\"QwMiPiME3bA\",\"YpVol7asWvd\",\"l0ccv2yzfF3\",\"rXLor9Knq6l\",\"HV8RTzgcFH3\",\"jWSIbtKfURj\",\"LhaAPLxdSFH\",\"hRZOIgQ0O1m\",\"fRLX08WHWpL\",\"hdEuw2ugkVF\",\"W5fN3G6y1VI\",\"cM2BKSrj9F9\",\"kU8vhUkAGaT\",\"EjnIQNVAXGp\",\"JdqfYTIFZXN\",\"eV4cuxniZgP\",\"QywkxFudXrC\",\"lY93YpCxJqf\",\"BD9gU0GKlr2\",\"EVkm2xYcf6Z\",\"x4HaBHHwBML\",\"GE25DpSrqpB\",\"DfUfwjM9am5\",\"xGMGhjA3y6J\",\"yu4N82FFeLm\",\"nlt6j60tCHF\",\"RWvG1aFrr0r\",\"EfWCa0Cc8WW\",\"FlBemv1NfEC\",\"OTFepb1k9Db\",\"GFk45MOxzJJ\",\"uKC54fzxRzO\",\"I4jWcnFmgEC\",\"J4GiUImJZoE\",\"DmaLM8WYmWv\",\"qgQ49DH9a0v\",\"ERmBhYkhV6Y\",\"U09TSwIjG0s\",\"VP397wRvePm\",\"KIUCimTXf8Q\",\"L8iA6eLwKNb\",\"DxAPPqXvwLy\",\"pmxZm7klXBy\",\"N233eZJZ1bh\",\"bQiBfA2j5cw\",\"gy8rmvYT4cj\",\"qtr8GGlm4gg\",\"XG8HGAbrbbL\",\"r1RUyfVBkLp\",\"r06ohri9wA9\",\"WXnNDWTiE9r\",\"HWjrSuoNPte\",\"UhHipWG7J8b\",\"g5ptsn0SFX8\",\"KctpIIucige\",\"j43EZb15rjI\",\"VGAFxBXz16y\",\"A3Fh37HWBWE\",\"g8DdBm7EmUt\",\"vzup1f6ynON\",\"iGHlidSFdpu\",\"cgOy0hRMGu9\",\"d9iMR1MpuIO\",\"NqWaKXcg01b\",\"PaqugoqjRIj\",\"PrJQHI6q7w2\",\"Qhmi8IZyPyD\",\"xhyjU2SVewz\",\"M2qEv692lS6\",\"sxRd2XOzFbz\",\"AovmOHadayb\",\"FRxcUEwktoV\",\"y5X4mP5XylL\",\"l7pFejMtUoF\",\"LfTkc0S4b5k\",\"DBs6e2Oxaj1\",\"npWGUj37qDe\",\"X7dWcGerQIm\",\"XrF5AvaGcuw\",\"EZPwuUTeIIG\",\"ARZ4y5i4reU\",\"pk7bUK5c1Uf\",\"CG4QD1HC3h4\",\"byp7w6Xd9Df\",\"NNE0YMCDZkO\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
        "enrollmentdate",
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZzYYXq4fJie.oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZzYYXq4fJie.ouname",
        "Organisation unit name",
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
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Under five (Luawa) Clinic");
    validateRowValueByName(
        response, actualHeaders, 0, "ZzYYXq4fJie.ouname", "Under five (Luawa) Clinic");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2023-01-31 12:05:00.0");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "ouname", "Yoni CHC");
    validateRowValueByName(response, actualHeaders, 99, "ZzYYXq4fJie.ouname", "Yoni CHC");
    validateRowValueByName(response, actualHeaders, 99, "enrollmentdate", "2023-01-27 12:05:00.0");
  }

  @Test
  public void queryRandom4() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,enrollmentdate,uIuxlbV1vRT,Bpx0589u8y0")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2021")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            // .add("dimension=ou:ImspTQPwCqd")
            .add("dimension=ZkbAXlQUYJG.EVENT_STATUS:ACTIVE,pe:2022")
            // .add("relativePeriodDate=2023-07-14")
            .add("desc=enrollmentdate");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);
  }

  @Test
  public void stageAndOuLevel() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,enrollmentdate,ZzYYXq4fJie.ou")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZzYYXq4fJie.ou:LEVEL-4,pe:202301")
            .add("desc=enrollmentdate,ouname");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);
  }
}
