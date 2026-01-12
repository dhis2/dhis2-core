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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class EnrollmentsQuery7AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  @Disabled("Temporarily disabled")
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
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.EVENT_DATE\":{\"name\":\"Report date, Birth\"}},\"dimensions\":{\"A03MvHHogjR.occurreddate\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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

    // 7. Assert row values by name (sample validation: evenly spaced rows, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Youndu CHP");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.eventdate", "2021-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2022-12-29 12:05:00.0");

    // Validate selected values for row index 9
    validateRowValueByName(response, actualHeaders, 9, "ouname", "Mokobo MCHP");
    validateRowValueByName(
        response, actualHeaders, 9, "A03MvHHogjR.eventdate", "2021-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 9, "enrollmentdate", "2022-12-29 12:05:00.0");

    // Validate selected values for row index 18
    validateRowValueByName(response, actualHeaders, 18, "ouname", "Kalainkay MCHP");
    validateRowValueByName(
        response, actualHeaders, 18, "A03MvHHogjR.eventdate", "2021-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 18, "enrollmentdate", "2022-12-29 12:05:00.0");

    // Validate selected values for row index 27
    validateRowValueByName(response, actualHeaders, 27, "ouname", "Moyeamoh CHP");
    validateRowValueByName(
        response, actualHeaders, 27, "A03MvHHogjR.eventdate", "2021-12-28 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 27, "enrollmentdate", "2022-12-28 12:05:00.0");

    // Validate selected values for row index 36
    validateRowValueByName(response, actualHeaders, 36, "ouname", "Madina Loko CHP");
    validateRowValueByName(
        response, actualHeaders, 36, "A03MvHHogjR.eventdate", "2021-12-28 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 36, "enrollmentdate", "2022-12-28 12:05:00.0");

    // Validate selected values for row index 45
    validateRowValueByName(response, actualHeaders, 45, "ouname", "Kamiendor MCHP");
    validateRowValueByName(
        response, actualHeaders, 45, "A03MvHHogjR.eventdate", "2021-12-28 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 45, "enrollmentdate", "2022-12-28 12:05:00.0");

    // Validate selected values for row index 54
    validateRowValueByName(response, actualHeaders, 54, "ouname", "Bandajuma Sinneh MCHP");
    validateRowValueByName(
        response, actualHeaders, 54, "A03MvHHogjR.eventdate", "2021-12-28 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 54, "enrollmentdate", "2022-12-28 12:05:00.0");

    // Validate selected values for row index 63
    validateRowValueByName(response, actualHeaders, 63, "ouname", "Needy CHC");
    validateRowValueByName(
        response, actualHeaders, 63, "A03MvHHogjR.eventdate", "2021-12-27 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 63, "enrollmentdate", "2022-12-27 12:05:00.0");

    // Validate selected values for row index 72
    validateRowValueByName(response, actualHeaders, 72, "ouname", "Konda CHP");
    validateRowValueByName(
        response, actualHeaders, 72, "A03MvHHogjR.eventdate", "2021-12-27 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 72, "enrollmentdate", "2022-12-27 12:05:00.0");

    // Validate selected values for row index 81
    validateRowValueByName(response, actualHeaders, 81, "ouname", "Foredugu MCHP");
    validateRowValueByName(
        response, actualHeaders, 81, "A03MvHHogjR.eventdate", "2021-12-27 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 81, "enrollmentdate", "2022-12-27 12:05:00.0");

    // Validate selected values for row index 90
    validateRowValueByName(response, actualHeaders, 90, "ouname", "Sindadu MCHP");
    validateRowValueByName(
        response, actualHeaders, 90, "A03MvHHogjR.eventdate", "2021-12-26 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 90, "enrollmentdate", "2022-12-26 12:05:00.0");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "ouname", "MCH Static/U5");
    validateRowValueByName(
        response, actualHeaders, 99, "A03MvHHogjR.eventdate", "2021-12-26 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 99, "enrollmentdate", "2022-12-26 12:05:00.0");
  }

  @Test
  @Disabled("Temporarily disabled")
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
  public void stageAndOuGroup() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,enrollmentdate,ZzYYXq4fJie.ouname,ZzYYXq4fJie.oucode")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZzYYXq4fJie.ou:OU_GROUP-CXw2yu5fodb,pe:202301")
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
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"FNnj3jKGS7i\":{\"uid\":\"FNnj3jKGS7i\",\"code\":\"OU_260387\",\"name\":\"Bandajuma Clinic CHC\"},\"Q2USZSJmcNK\":{\"uid\":\"Q2USZSJmcNK\",\"code\":\"OU_268195\",\"name\":\"Bumbuna CHC\"},\"sLKHXoBIqSs\":{\"uid\":\"sLKHXoBIqSs\",\"code\":\"OU_233362\",\"name\":\"Njagbwema Fiama CHC\"},\"mwN7QuEfT8m\":{\"uid\":\"mwN7QuEfT8m\",\"code\":\"OU_820\",\"name\":\"Koribondo CHC\"},\"jGYT5U5qJP6\":{\"uid\":\"jGYT5U5qJP6\",\"code\":\"OU_653\",\"name\":\"Gbaiima CHC\"},\"agM0BKQlTh3\":{\"uid\":\"agM0BKQlTh3\",\"code\":\"OU_193303\",\"name\":\"Batkanu CHC\"},\"MuZJ8lprGqK\":{\"uid\":\"MuZJ8lprGqK\",\"code\":\"OU_247089\",\"name\":\"Moyamba Junction CHC\"},\"e2WgqiasKnD\":{\"uid\":\"e2WgqiasKnD\",\"code\":\"OU_246997\",\"name\":\"Taiama (Kori) CHC\"},\"KcCbIDzRcui\":{\"uid\":\"KcCbIDzRcui\",\"code\":\"OU_268221\",\"name\":\"Matotoka CHC\"},\"Jyv7sjpl9bA\":{\"uid\":\"Jyv7sjpl9bA\",\"code\":\"OU_222650\",\"name\":\"Sendumei CHC\"},\"mshIal30ffW\":{\"uid\":\"mshIal30ffW\",\"code\":\"OU_193301\",\"name\":\"Mapaki CHC\"},\"zEsMdeJOty4\":{\"uid\":\"zEsMdeJOty4\",\"code\":\"OU_278331\",\"name\":\"Moyiba CHC\"},\"uFp0ztDOFbI\":{\"uid\":\"uFp0ztDOFbI\",\"code\":\"OU_197430\",\"name\":\"Bendu CHC\"},\"scc4QyxenJd\":{\"uid\":\"scc4QyxenJd\",\"code\":\"OU_268215\",\"name\":\"Makali CHC\"},\"aHs9PLxIdbr\":{\"uid\":\"aHs9PLxIdbr\",\"code\":\"OU_268203\",\"name\":\"Mayepoh CHC\"},\"PA1spYiNZfv\":{\"uid\":\"PA1spYiNZfv\",\"code\":\"OU_233398\",\"name\":\"Yengema CHC\"},\"LnToY3ExKxL\":{\"uid\":\"LnToY3ExKxL\",\"code\":\"OU_255018\",\"name\":\"Mahera CHC\"},\"IlMQTFvcq9r\":{\"uid\":\"IlMQTFvcq9r\",\"code\":\"OU_222656\",\"name\":\"Lowoma CHC\"},\"jhtj3eQa1pM\":{\"uid\":\"jhtj3eQa1pM\",\"code\":\"OU_1100\",\"name\":\"Gondama (Tikonko) CHC\"},\"QsAwd531Cpd\":{\"uid\":\"QsAwd531Cpd\",\"code\":\"OU_1038\",\"name\":\"Njala CHC\"},\"pNPmNeqyrim\":{\"uid\":\"pNPmNeqyrim\",\"code\":\"OU_222666\",\"name\":\"Foindu (Lower Bamabara) CHC\"},\"K00jR5dmoFZ\":{\"uid\":\"K00jR5dmoFZ\",\"code\":\"OU_260399\",\"name\":\"Karlu CHC\"},\"xa4F6gesVJm\":{\"uid\":\"xa4F6gesVJm\",\"code\":\"OU_278400\",\"name\":\"York CHC\"},\"zQpYVEyAM2t\":{\"uid\":\"zQpYVEyAM2t\",\"code\":\"OU_278377\",\"name\":\"Hastings Health Centre\"},\"hzf90qz08AW\":{\"uid\":\"hzf90qz08AW\",\"code\":\"OU_247034\",\"name\":\"Njama CHC\"},\"CFPrsD3dNeb\":{\"uid\":\"CFPrsD3dNeb\",\"code\":\"OU_197423\",\"name\":\"Tissana CHC\"},\"MpcMjLmbATv\":{\"uid\":\"MpcMjLmbATv\",\"code\":\"OU_204938\",\"name\":\"Bandajuma Yawei CHC\"},\"KYXbIQBQgP1\":{\"uid\":\"KYXbIQBQgP1\",\"code\":\"OU_1103\",\"name\":\"Tikonko CHC\"},\"lxxASQqPUqd\":{\"uid\":\"lxxASQqPUqd\",\"code\":\"OU_233341\",\"name\":\"Tombodu CHC\"},\"oRncQGhLYNE\":{\"uid\":\"oRncQGhLYNE\",\"code\":\"OU_278376\",\"name\":\"Regent (RWA) CHC\"},\"RhJbg8UD75Q\":{\"uid\":\"RhJbg8UD75Q\",\"code\":\"OU_1027\",\"name\":\"Yemoh Town CHC\"},\"EUUkKEDoNsf\":{\"uid\":\"EUUkKEDoNsf\",\"code\":\"OU_278343\",\"name\":\"Wilberforce CHC\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"FbD5Z8z22Yb\":{\"uid\":\"FbD5Z8z22Yb\",\"code\":\"OU_260385\",\"name\":\"Geoma Jagor CHC\"},\"ua5GXy2uhBR\":{\"uid\":\"ua5GXy2uhBR\",\"code\":\"OU_197412\",\"name\":\"Tihun CHC\"},\"QpRIPul20Sb\":{\"uid\":\"QpRIPul20Sb\",\"code\":\"OU_222637\",\"name\":\"Gorahun CHC\"},\"mt47bcb0Rcj\":{\"uid\":\"mt47bcb0Rcj\",\"code\":\"OU_193231\",\"name\":\"Kamabai CHC\"},\"uDzWmUDHKeR\":{\"uid\":\"uDzWmUDHKeR\",\"code\":\"OU_260389\",\"name\":\"Futa CHC\"},\"ZzYYXq4fJie.ou\":{\"name\":\"Organisation unit, Baby Postnatal\"},\"OjXNuYyLaCJ\":{\"uid\":\"OjXNuYyLaCJ\",\"code\":\"OU_255004\",\"name\":\"Sendugu CHC\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZdPkczYqeIY\":{\"uid\":\"ZdPkczYqeIY\",\"code\":\"OU_247092\",\"name\":\"Gandorhun CHC\"},\"p310xqwAJge\":{\"uid\":\"p310xqwAJge\",\"code\":\"OU_226268\",\"name\":\"Kondembaia CHC\"},\"HcB2W6Fgp7i\":{\"uid\":\"HcB2W6Fgp7i\",\"code\":\"OU_255065\",\"name\":\"Melekuray CHC\"},\"lOv6IFgr6Fs\":{\"uid\":\"lOv6IFgr6Fs\",\"code\":\"OU_832\",\"name\":\"Manjama Shellmingo CHC\"},\"amgb83zVxp5\":{\"uid\":\"amgb83zVxp5\",\"code\":\"OU_222674\",\"name\":\"Bendu Mameima CHC\"},\"K6oyIMh7Lee\":{\"uid\":\"K6oyIMh7Lee\",\"code\":\"OU_226224\",\"name\":\"Fadugu CHC\"},\"m0XorV4WWg0\":{\"uid\":\"m0XorV4WWg0\",\"code\":\"OU_278361\",\"name\":\"Ginger Hall Health Centre\"},\"bSj2UnYhTFb\":{\"uid\":\"bSj2UnYhTFb\",\"code\":\"OU_193218\",\"name\":\"Kamaranka CHC\"},\"CvBAqD6RzLZ\":{\"uid\":\"CvBAqD6RzLZ\",\"code\":\"OU_595\",\"name\":\"Ngalu CHC\"},\"k6lOze3vTzP\":{\"uid\":\"k6lOze3vTzP\",\"code\":\"OU_260435\",\"name\":\"Potoru CHC\"},\"p9KfD6eaRvu\":{\"uid\":\"p9KfD6eaRvu\",\"code\":\"OU_247069\",\"name\":\"Shenge CHC\"},\"K3jhn3TXF3a\":{\"uid\":\"K3jhn3TXF3a\",\"code\":\"OU_222665\",\"name\":\"Tongo Field CHC\"},\"HNv1aLPdMYb\":{\"uid\":\"HNv1aLPdMYb\",\"code\":\"OU_193242\",\"name\":\"Kamalo CHC\"},\"yMCshbaVExv\":{\"uid\":\"yMCshbaVExv\",\"code\":\"OU_254991\",\"name\":\"Babara CHC\"},\"pJv8NJlJNhU\":{\"uid\":\"pJv8NJlJNhU\",\"code\":\"OU_204916\",\"name\":\"Pendembu CHC\"},\"YAuJ3fyoEuI\":{\"uid\":\"YAuJ3fyoEuI\",\"code\":\"OU_193281\",\"name\":\"Gbendembu Wesleyan CHC\"},\"g5A3hiJlwmI\":{\"uid\":\"g5A3hiJlwmI\",\"code\":\"OU_233397\",\"name\":\"UMC Mitchener Memorial Maternity & Health Centre\"},\"jKZ0U8Og5aV\":{\"uid\":\"jKZ0U8Og5aV\",\"code\":\"OU_222683\",\"name\":\"Dodo CHC\"},\"EXbPGmEUdnc\":{\"uid\":\"EXbPGmEUdnc\",\"code\":\"OU_193197\",\"name\":\"Mateboi CHC\"},\"nX05QLraDhO\":{\"uid\":\"nX05QLraDhO\",\"code\":\"OU_585\",\"name\":\"Yamandu CHC\"},\"Umh4HKqqFp6\":{\"uid\":\"Umh4HKqqFp6\",\"code\":\"OU_578\",\"name\":\"Jembe CHC\"},\"ubsjwFFBaJM\":{\"uid\":\"ubsjwFFBaJM\",\"code\":\"OU_247016\",\"name\":\"Gbangbatoke CHC\"},\"rm60vuHyQXj\":{\"uid\":\"rm60vuHyQXj\",\"code\":\"OU_1082\",\"name\":\"Nengbema CHC\"},\"iOA3z6Y3cq5\":{\"uid\":\"iOA3z6Y3cq5\",\"code\":\"OU_222699\",\"name\":\"Largo CHC\"},\"sIVFEyNfOg4\":{\"uid\":\"sIVFEyNfOg4\",\"code\":\"OU_247073\",\"name\":\"Mokandor CHP\"},\"k8ZPul89UDm\":{\"uid\":\"k8ZPul89UDm\",\"code\":\"OU_233370\",\"name\":\"Kayima CHC\"},\"iMZihUMzH92\":{\"uid\":\"iMZihUMzH92\",\"code\":\"OU_247083\",\"name\":\"Bauya (Kongbora) CHC\"},\"MXdbul7bBqV\":{\"uid\":\"MXdbul7bBqV\",\"code\":\"OU_204912\",\"name\":\"Mobai CHC\"},\"dkmpOuVhBba\":{\"uid\":\"dkmpOuVhBba\",\"code\":\"OU_268226\",\"name\":\"Mathoir CHC\"},\"q56204kKXgZ\":{\"uid\":\"q56204kKXgZ\",\"code\":\"OU_255057\",\"name\":\"Lunsar CHC\"},\"kuqKh33SPgg\":{\"uid\":\"kuqKh33SPgg\",\"code\":\"OU_226230\",\"name\":\"Falaba CHC\"},\"NjyJYiIuKIG\":{\"uid\":\"NjyJYiIuKIG\",\"code\":\"OU_193291\",\"name\":\"Kathanta Yimbor CHC\"},\"fAsj6a4nudH\":{\"uid\":\"fAsj6a4nudH\",\"code\":\"OU_197398\",\"name\":\"Yoni CHC\"},\"a04CZxe0PSe\":{\"uid\":\"a04CZxe0PSe\",\"code\":\"OU_278333\",\"name\":\"Murray Town CHC\"},\"qxbsDd9QYv6\":{\"uid\":\"qxbsDd9QYv6\",\"code\":\"OU_226274\",\"name\":\"Yiffin CHC\"},\"Mi4dWRtfIOC\":{\"uid\":\"Mi4dWRtfIOC\",\"code\":\"OU_204860\",\"name\":\"Sandaru CHC\"},\"QzPf0qKBU4n\":{\"uid\":\"QzPf0qKBU4n\",\"code\":\"OU_260411\",\"name\":\"Jendema CHC\"},\"mzsOsz0NwNY\":{\"uid\":\"mzsOsz0NwNY\",\"code\":\"OU_836\",\"name\":\"New Police Barracks CHC\"},\"vELbGdEphPd\":{\"uid\":\"vELbGdEphPd\",\"code\":\"OU_614\",\"name\":\"Jimmi CHC\"},\"qVvitxEF2ck\":{\"uid\":\"qVvitxEF2ck\",\"code\":\"OU_254949\",\"name\":\"Rogbere CHC\"},\"RAsstekPRco\":{\"uid\":\"RAsstekPRco\",\"code\":\"OU_211269\",\"name\":\"Mambolo CHC\"},\"va2lE4FiVVb\":{\"uid\":\"va2lE4FiVVb\",\"code\":\"OU_247019\",\"name\":\"Mano CHC\"},\"cNAp6CJeLxk\":{\"uid\":\"cNAp6CJeLxk\",\"code\":\"OU_247015\",\"name\":\"Mokanji CHC\"},\"wByqtWCCuDJ\":{\"uid\":\"wByqtWCCuDJ\",\"code\":\"OU_1095\",\"name\":\"Damballa CHC\"},\"PcADvhvcaI2\":{\"uid\":\"PcADvhvcaI2\",\"code\":\"OU_211253\",\"name\":\"Kychom CHC\"},\"n7wN9gMFfZ5\":{\"uid\":\"n7wN9gMFfZ5\",\"code\":\"OU_197435\",\"name\":\"Benduma CHC\"},\"kUzpbgPCwVA\":{\"uid\":\"kUzpbgPCwVA\",\"code\":\"OU_222624\",\"name\":\"Blama CHC\"},\"rCKWdLr4B8K\":{\"uid\":\"rCKWdLr4B8K\",\"code\":\"OU_197427\",\"name\":\"Motuo CHC\"},\"DMxw0SASFih\":{\"uid\":\"DMxw0SASFih\",\"code\":\"OU_204941\",\"name\":\"Koindu CHC\"},\"PQEpIeuSTCN\":{\"uid\":\"PQEpIeuSTCN\",\"code\":\"OU_222623\",\"name\":\"Tobanda CHC\"},\"inpc5QsFRTm\":{\"uid\":\"inpc5QsFRTm\",\"code\":\"OU_211274\",\"name\":\"Kamassasa CHC\"},\"aSxNNRxPuBP\":{\"uid\":\"aSxNNRxPuBP\",\"code\":\"OU_193278\",\"name\":\"Kalangba CHC\"},\"O1KFJmM6HUx\":{\"uid\":\"O1KFJmM6HUx\",\"code\":\"OU_260438\",\"name\":\"Mano Gbonjeima CHC\"},\"s5aXfzOL456\":{\"uid\":\"s5aXfzOL456\",\"code\":\"OU_197437\",\"name\":\"Talia CHC\"},\"tSBcgrTDdB8\":{\"uid\":\"tSBcgrTDdB8\",\"code\":\"OU_834\",\"name\":\"Paramedical CHC\"},\"KiheEgvUZ0i\":{\"uid\":\"KiheEgvUZ0i\",\"code\":\"OU_278357\",\"name\":\"Calaba town CHC\"},\"d9zRBAoM8OC\":{\"uid\":\"d9zRBAoM8OC\",\"code\":\"OU_260423\",\"name\":\"Bumpeh Perri CHC\"},\"sesv0eXljBq\":{\"uid\":\"sesv0eXljBq\",\"code\":\"OU_268207\",\"name\":\"Yele CHC\"},\"L4Tw4NlaMjn\":{\"uid\":\"L4Tw4NlaMjn\",\"code\":\"OU_222705\",\"name\":\"Nekabo CHC\"},\"sznCEDMABa2\":{\"uid\":\"sznCEDMABa2\",\"code\":\"OU_204903\",\"name\":\"Ngiehun CHC\"},\"E497Rk80ivZ\":{\"uid\":\"E497Rk80ivZ\",\"code\":\"OU_651\",\"name\":\"Bumpe CHC\"},\"CXw2yu5fodb\":{\"uid\":\"CXw2yu5fodb\",\"code\":\"CHC\",\"name\":\"CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"W7ekX3gi0ut\":{\"uid\":\"W7ekX3gi0ut\",\"code\":\"OU_233333\",\"name\":\"Jaiama Sewafe CHC\"},\"roGdTjEqLZQ\":{\"uid\":\"roGdTjEqLZQ\",\"code\":\"OU_233371\",\"name\":\"Yormandu CHC\"},\"Jiymtq0A01x\":{\"uid\":\"Jiymtq0A01x\",\"code\":\"OU_226243\",\"name\":\"Bafodia CHC\"},\"aIsnJuZbmVA\":{\"uid\":\"aIsnJuZbmVA\",\"code\":\"OU_226256\",\"name\":\"Dogoloya CHP\"},\"IlnqGuxfQAw\":{\"uid\":\"IlnqGuxfQAw\",\"code\":\"OU_226216\",\"name\":\"Sinkunia CHC\"},\"lpQvlm9czYE\":{\"uid\":\"lpQvlm9czYE\",\"code\":\"OU_222631\",\"name\":\"Tungie CHC\"},\"uedNhvYPMNu\":{\"uid\":\"uedNhvYPMNu\",\"code\":\"OU_193216\",\"name\":\"Gbanti CHC\"},\"KKoPh1lDd9j\":{\"uid\":\"KKoPh1lDd9j\",\"code\":\"OU_233321\",\"name\":\"Kainkordu CHC\"},\"xKaB8tfbTzm\":{\"uid\":\"xKaB8tfbTzm\",\"code\":\"OU_193247\",\"name\":\"Fintonia CHC\"},\"TEVtOFKcLAP\":{\"uid\":\"TEVtOFKcLAP\",\"code\":\"OU_197447\",\"name\":\"Gbap CHC\"},\"pXDcgDRz8Od\":{\"uid\":\"pXDcgDRz8Od\",\"code\":\"OU_278402\",\"name\":\"Songo CHC\"},\"NaVzm59XKGf\":{\"uid\":\"NaVzm59XKGf\",\"code\":\"OU_254980\",\"name\":\"Gbinti CHC\"},\"Ahh47q8AkId\":{\"uid\":\"Ahh47q8AkId\",\"code\":\"OU_268167\",\"name\":\"Mabang CHC\"},\"QZtMuEEV9Vv\":{\"uid\":\"QZtMuEEV9Vv\",\"code\":\"OU_211237\",\"name\":\"Rokupr CHC\"},\"U4FzUXMvbI8\":{\"uid\":\"U4FzUXMvbI8\",\"code\":\"OU_255009\",\"name\":\"Conakry Dee CHC\"},\"o0BgK1dLhF8\":{\"uid\":\"o0BgK1dLhF8\",\"code\":\"OU_268170\",\"name\":\"Bendugu CHC\"},\"agEKP19IUKI\":{\"uid\":\"agEKP19IUKI\",\"code\":\"OU_193280\",\"name\":\"Tambiama CHC\"},\"qIpBLa1SCZt\":{\"uid\":\"qIpBLa1SCZt\",\"code\":\"OU_222714\",\"name\":\"Talia (Nongowa) CHC\"},\"H97XE5Ea089\":{\"uid\":\"H97XE5Ea089\",\"code\":\"OU_247045\",\"name\":\"Bomotoke CHC\"},\"nv41sOz8IVM\":{\"uid\":\"nv41sOz8IVM\",\"code\":\"OU_204927\",\"name\":\"Pejewa CHC\"},\"202301\":{\"name\":\"January 2023\"},\"NMcx2jmra3c\":{\"uid\":\"NMcx2jmra3c\",\"code\":\"OU_226273\",\"name\":\"Firawa CHC\"},\"m3VnSQbE8CD\":{\"uid\":\"m3VnSQbE8CD\",\"code\":\"OU_278382\",\"name\":\"Newton CHC\"},\"OY7mYDATra3\":{\"uid\":\"OY7mYDATra3\",\"code\":\"OU_268176\",\"name\":\"Massingbi CHC\"},\"RQgXBKxgvHf\":{\"uid\":\"RQgXBKxgvHf\",\"code\":\"OU_211248\",\"name\":\"Mapotolon CHC\"},\"YvwYw7GilkP\":{\"uid\":\"YvwYw7GilkP\",\"code\":\"OU_222726\",\"name\":\"Levuma (Kandu Lep) CHC\"},\"PduUQmdt0pB\":{\"uid\":\"PduUQmdt0pB\",\"code\":\"OU_211273\",\"name\":\"Numea CHC\"},\"U514Dz4v9pv\":{\"uid\":\"U514Dz4v9pv\",\"code\":\"OU_278330\",\"name\":\"George Brook Health Centre\"},\"PuZOFApTSeo\":{\"uid\":\"PuZOFApTSeo\",\"code\":\"OU_952\",\"name\":\"Sahn CHC\"},\"wUmVUKhnPuy\":{\"uid\":\"wUmVUKhnPuy\",\"code\":\"OU_247057\",\"name\":\"Kangahun CHC\"},\"RaQGHRti7JM\":{\"uid\":\"RaQGHRti7JM\",\"code\":\"OU_278335\",\"name\":\"Gods Favour health Center\"},\"xMn4Wki9doK\":{\"uid\":\"xMn4Wki9doK\",\"code\":\"OU_197417\",\"name\":\"Moriba Town CHC\"},\"JQJjsXvHE5M\":{\"uid\":\"JQJjsXvHE5M\",\"code\":\"OU_247007\",\"name\":\"Mokelleh CHC\"},\"m5BX6CvJ6Ex\":{\"uid\":\"m5BX6CvJ6Ex\",\"code\":\"OU_204865\",\"name\":\"Daru CHC\"},\"D2rB1GRuh8C\":{\"uid\":\"D2rB1GRuh8C\",\"code\":\"OU_197418\",\"name\":\"Gbamgbama CHC\"},\"Gm7YUjhVi9Q\":{\"uid\":\"Gm7YUjhVi9Q\",\"code\":\"OU_260415\",\"name\":\"Fairo CHC\"},\"gE3gEGZbQMi\":{\"uid\":\"gE3gEGZbQMi\",\"code\":\"OU_197407\",\"name\":\"Madina (BUM) CHC\"},\"zuXW98AEbE7\":{\"uid\":\"zuXW98AEbE7\",\"code\":\"OU_255023\",\"name\":\"Kamasondo CHC\"},\"TjZwphhxCuV\":{\"uid\":\"TjZwphhxCuV\",\"code\":\"OU_193225\",\"name\":\"Kagbere CHC\"},\"g5lonXJ9ndA\":{\"uid\":\"g5lonXJ9ndA\",\"code\":\"OU_268237\",\"name\":\"Hinistas CHC\"},\"r5WWF9WDzoa\":{\"uid\":\"r5WWF9WDzoa\",\"code\":\"OU_222681\",\"name\":\"Baama CHC\"},\"VhRX5JDVo7R\":{\"uid\":\"VhRX5JDVo7R\",\"code\":\"OU_278397\",\"name\":\"Waterloo CHC\"},\"PMsF64R6OJX\":{\"uid\":\"PMsF64R6OJX\",\"code\":\"OU_226248\",\"name\":\"Bendugu (Mongo) CHC\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"azRICFoILuh\":{\"uid\":\"azRICFoILuh\",\"code\":\"OU_577\",\"name\":\"Golu MCHP\"},\"HQoxFu4lYPS\":{\"uid\":\"HQoxFu4lYPS\",\"code\":\"OU_204868\",\"name\":\"Pellie CHC\"},\"JrSIoCOdTH2\":{\"uid\":\"JrSIoCOdTH2\",\"code\":\"OU_278403\",\"name\":\"Tombo CHC\"},\"ke2gwHKHP3z\":{\"uid\":\"ke2gwHKHP3z\",\"code\":\"OU_254983\",\"name\":\"Petifu CHC\"},\"TSyzvBiovKh\":{\"uid\":\"TSyzvBiovKh\",\"code\":\"OU_576\",\"name\":\"Gerehun CHC\"},\"g10jm7jPdzf\":{\"uid\":\"g10jm7jPdzf\",\"code\":\"OU_222707\",\"name\":\"Hangha CHC\"},\"DiszpKrYNg8\":{\"uid\":\"DiszpKrYNg8\",\"code\":\"OU_559\",\"name\":\"Ngelehun CHC\"},\"bPqP6eRfkyn\":{\"uid\":\"bPqP6eRfkyn\",\"code\":\"OU_278318\",\"name\":\"Ross Road Health Centre\"},\"EURoFVjowXs\":{\"uid\":\"EURoFVjowXs\",\"code\":\"OU_254964\",\"name\":\"Masiaka CHC\"},\"ii2KMnWMx2L\":{\"uid\":\"ii2KMnWMx2L\",\"code\":\"OU_233392\",\"name\":\"Gandorhun (Gbane) CHC\"},\"J42QfNe0GJZ\":{\"uid\":\"J42QfNe0GJZ\",\"code\":\"OU_268185\",\"name\":\"Mara CHC\"},\"gUPhNWkSXvD\":{\"uid\":\"gUPhNWkSXvD\",\"code\":\"OU_247032\",\"name\":\"Rotifunk CHC\"},\"FLjwMPWLrL2\":{\"uid\":\"FLjwMPWLrL2\",\"code\":\"OU_1126\",\"name\":\"Baomahun CHC\"},\"FclfbEFMcf3\":{\"uid\":\"FclfbEFMcf3\",\"code\":\"OU_278340\",\"name\":\"Kissy Health Centre\"},\"lL2LBkhlsmV\":{\"uid\":\"lL2LBkhlsmV\",\"code\":\"OU_278336\",\"name\":\"Grassfield CHC\"},\"NRPCjDljVtu\":{\"uid\":\"NRPCjDljVtu\",\"code\":\"OU_278404\",\"name\":\"Lakka\\/Ogoo Farm CHC\"},\"egjrZ1PHNtT\":{\"uid\":\"egjrZ1PHNtT\",\"code\":\"OU_247053\",\"name\":\"Sembehun CHC\"},\"PC3Ag91n82e\":{\"uid\":\"PC3Ag91n82e\",\"code\":\"OU_1122\",\"name\":\"Mongere CHC\"},\"X79FDd4EAgo\":{\"uid\":\"X79FDd4EAgo\",\"code\":\"OU_193196\",\"name\":\"Rokulan CHC\"},\"sM0Us0NkSez\":{\"uid\":\"sM0Us0NkSez\",\"code\":\"OU_278359\",\"name\":\"Kroo Bay CHC\"},\"bHcw141PTsE\":{\"uid\":\"bHcw141PTsE\",\"code\":\"OU_260407\",\"name\":\"Gbondapi CHC\"},\"O63vIA5MVn6\":{\"uid\":\"O63vIA5MVn6\",\"code\":\"OU_255014\",\"name\":\"Tagrin CHC\"},\"HWXk4EBHUyk\":{\"uid\":\"HWXk4EBHUyk\",\"code\":\"OU_260393\",\"name\":\"Sahn (Malen) CHC\"},\"W2KnxOMvmgE\":{\"uid\":\"W2KnxOMvmgE\",\"code\":\"OU_1050\",\"name\":\"Sumbuya CHC\"},\"tO01bqIipeD\":{\"uid\":\"tO01bqIipeD\",\"code\":\"OU_204892\",\"name\":\"Buedu CHC\"},\"w3mBVfrWhXl\":{\"uid\":\"w3mBVfrWhXl\",\"code\":\"OU_255043\",\"name\":\"Mange CHC\"},\"TljiT6C5D0J\":{\"uid\":\"TljiT6C5D0J\",\"code\":\"OU_222740\",\"name\":\"Kpandebu CHC\"},\"TmCsvdJLHoX\":{\"uid\":\"TmCsvdJLHoX\",\"code\":\"OU_193195\",\"name\":\"Mabunduka CHC\"},\"BNFrspDBKel\":{\"uid\":\"BNFrspDBKel\",\"code\":\"OU_260382\",\"name\":\"Zimmi CHC\"},\"N3tpEjZcPm9\":{\"uid\":\"N3tpEjZcPm9\",\"code\":\"OU_204879\",\"name\":\"Laleihun Kovoma CHC\"},\"Qc9lf4VM9bD\":{\"uid\":\"Qc9lf4VM9bD\",\"code\":\"OU_278317\",\"name\":\"Wellington Health Centre\"},\"cMFi8lYbXHY\":{\"uid\":\"cMFi8lYbXHY\",\"code\":\"OU_233402\",\"name\":\"Bumpeh (Nimikoro) CHC\"},\"P4upLKrpkHP\":{\"uid\":\"P4upLKrpkHP\",\"code\":\"OU_222641\",\"name\":\"Ngegbwema CHC\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"Yj2ni275yPJ\":{\"uid\":\"Yj2ni275yPJ\",\"code\":\"OU_222647\",\"name\":\"Baoma (Koya) CHC\"},\"SnCrOCRrxGX\":{\"uid\":\"SnCrOCRrxGX\",\"code\":\"OU_233327\",\"name\":\"Koakoyima CHC\"},\"Z9ny6QeqsgX\":{\"uid\":\"Z9ny6QeqsgX\",\"code\":\"OU_969\",\"name\":\"Manjama UMC CHC\"},\"wtdBuXDwZYQ\":{\"uid\":\"wtdBuXDwZYQ\",\"code\":\"OU_1006\",\"name\":\"Praise Foundation CHC\"},\"oLuhRyYPxRO\":{\"uid\":\"oLuhRyYPxRO\",\"code\":\"OU_247011\",\"name\":\"Senehun CHC\"},\"PaNv9VyD06n\":{\"uid\":\"PaNv9VyD06n\",\"code\":\"OU_204930\",\"name\":\"Manowa CHC\"},\"uRQj8WRK0Py\":{\"uid\":\"uRQj8WRK0Py\",\"code\":\"OU_193255\",\"name\":\"Masongbo CHC\"},\"Y8foq27WLti\":{\"uid\":\"Y8foq27WLti\",\"code\":\"OU_222728\",\"name\":\"Baoma Oil Mill CHC\"},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"mepHuAA9l51\":{\"uid\":\"mepHuAA9l51\",\"code\":\"OU_193205\",\"name\":\"Rokonta CHC\"},\"k1Y0oNqPlmy\":{\"uid\":\"k1Y0oNqPlmy\",\"code\":\"OU_1161\",\"name\":\"Gboyama CHC\"},\"UOJlcpPnBat\":{\"uid\":\"UOJlcpPnBat\",\"code\":\"OU_172174\",\"name\":\"Needy CHC\"},\"CKkE4GBJekz\":{\"uid\":\"CKkE4GBJekz\",\"code\":\"OU_222671\",\"name\":\"Weima CHC\"},\"GHHvGp7tgtZ\":{\"uid\":\"GHHvGp7tgtZ\",\"code\":\"OU_193275\",\"name\":\"Binkolo CHC\"},\"dQggcljEImF\":{\"uid\":\"dQggcljEImF\",\"code\":\"OU_278392\",\"name\":\"Goderich Health Centre\"},\"DvzKyuC0G4w\":{\"uid\":\"DvzKyuC0G4w\",\"code\":\"OU_204872\",\"name\":\"Jojoima CHC\"},\"Luv2kmWWgoG\":{\"uid\":\"Luv2kmWWgoG\",\"code\":\"OU_222632\",\"name\":\"Mondema CHC\"},\"cw0Wm1QTHRq\":{\"uid\":\"cw0Wm1QTHRq\",\"code\":\"OU_222750\",\"name\":\"Joru CHC\"},\"Ep5iWL1UKvF\":{\"uid\":\"Ep5iWL1UKvF\",\"code\":\"OU_226276\",\"name\":\"Kurubonla CHC\"},\"EH0dXLB4nZg\":{\"uid\":\"EH0dXLB4nZg\",\"code\":\"OU_255032\",\"name\":\"Masimera CHC\"},\"L5gENbBNNup\":{\"uid\":\"L5gENbBNNup\",\"code\":\"OU_222689\",\"name\":\"Boajibu CHC\"}},\"dimensions\":{\"ZzYYXq4fJie.ou\":[\"r5WWF9WDzoa\",\"yMCshbaVExv\",\"Jiymtq0A01x\",\"FNnj3jKGS7i\",\"MpcMjLmbATv\",\"Yj2ni275yPJ\",\"Y8foq27WLti\",\"FLjwMPWLrL2\",\"agM0BKQlTh3\",\"iMZihUMzH92\",\"uFp0ztDOFbI\",\"amgb83zVxp5\",\"PMsF64R6OJX\",\"o0BgK1dLhF8\",\"n7wN9gMFfZ5\",\"GHHvGp7tgtZ\",\"kUzpbgPCwVA\",\"L5gENbBNNup\",\"H97XE5Ea089\",\"tO01bqIipeD\",\"Q2USZSJmcNK\",\"E497Rk80ivZ\",\"cMFi8lYbXHY\",\"d9zRBAoM8OC\",\"KiheEgvUZ0i\",\"U4FzUXMvbI8\",\"wByqtWCCuDJ\",\"m5BX6CvJ6Ex\",\"jKZ0U8Og5aV\",\"aIsnJuZbmVA\",\"K6oyIMh7Lee\",\"Gm7YUjhVi9Q\",\"kuqKh33SPgg\",\"xKaB8tfbTzm\",\"NMcx2jmra3c\",\"pNPmNeqyrim\",\"uDzWmUDHKeR\",\"ii2KMnWMx2L\",\"ZdPkczYqeIY\",\"jGYT5U5qJP6\",\"D2rB1GRuh8C\",\"ubsjwFFBaJM\",\"uedNhvYPMNu\",\"TEVtOFKcLAP\",\"YAuJ3fyoEuI\",\"NaVzm59XKGf\",\"bHcw141PTsE\",\"k1Y0oNqPlmy\",\"FbD5Z8z22Yb\",\"U514Dz4v9pv\",\"TSyzvBiovKh\",\"m0XorV4WWg0\",\"dQggcljEImF\",\"RaQGHRti7JM\",\"azRICFoILuh\",\"jhtj3eQa1pM\",\"QpRIPul20Sb\",\"lL2LBkhlsmV\",\"g10jm7jPdzf\",\"zQpYVEyAM2t\",\"g5lonXJ9ndA\",\"W7ekX3gi0ut\",\"Umh4HKqqFp6\",\"QzPf0qKBU4n\",\"vELbGdEphPd\",\"DvzKyuC0G4w\",\"cw0Wm1QTHRq\",\"TjZwphhxCuV\",\"KKoPh1lDd9j\",\"aSxNNRxPuBP\",\"mt47bcb0Rcj\",\"HNv1aLPdMYb\",\"bSj2UnYhTFb\",\"zuXW98AEbE7\",\"inpc5QsFRTm\",\"wUmVUKhnPuy\",\"K00jR5dmoFZ\",\"NjyJYiIuKIG\",\"k8ZPul89UDm\",\"FclfbEFMcf3\",\"SnCrOCRrxGX\",\"DMxw0SASFih\",\"p310xqwAJge\",\"mwN7QuEfT8m\",\"TljiT6C5D0J\",\"sM0Us0NkSez\",\"Ep5iWL1UKvF\",\"PcADvhvcaI2\",\"NRPCjDljVtu\",\"N3tpEjZcPm9\",\"iOA3z6Y3cq5\",\"YvwYw7GilkP\",\"IlMQTFvcq9r\",\"q56204kKXgZ\",\"Ahh47q8AkId\",\"TmCsvdJLHoX\",\"gE3gEGZbQMi\",\"LnToY3ExKxL\",\"scc4QyxenJd\",\"RAsstekPRco\",\"w3mBVfrWhXl\",\"lOv6IFgr6Fs\",\"Z9ny6QeqsgX\",\"va2lE4FiVVb\",\"O1KFJmM6HUx\",\"PaNv9VyD06n\",\"mshIal30ffW\",\"RQgXBKxgvHf\",\"J42QfNe0GJZ\",\"EURoFVjowXs\",\"EH0dXLB4nZg\",\"uRQj8WRK0Py\",\"OY7mYDATra3\",\"EXbPGmEUdnc\",\"dkmpOuVhBba\",\"KcCbIDzRcui\",\"aHs9PLxIdbr\",\"HcB2W6Fgp7i\",\"MXdbul7bBqV\",\"sIVFEyNfOg4\",\"cNAp6CJeLxk\",\"JQJjsXvHE5M\",\"Luv2kmWWgoG\",\"PC3Ag91n82e\",\"xMn4Wki9doK\",\"rCKWdLr4B8K\",\"MuZJ8lprGqK\",\"zEsMdeJOty4\",\"a04CZxe0PSe\",\"UOJlcpPnBat\",\"L4Tw4NlaMjn\",\"rm60vuHyQXj\",\"mzsOsz0NwNY\",\"m3VnSQbE8CD\",\"CvBAqD6RzLZ\",\"P4upLKrpkHP\",\"DiszpKrYNg8\",\"sznCEDMABa2\",\"sLKHXoBIqSs\",\"QsAwd531Cpd\",\"hzf90qz08AW\",\"PduUQmdt0pB\",\"tSBcgrTDdB8\",\"nv41sOz8IVM\",\"HQoxFu4lYPS\",\"pJv8NJlJNhU\",\"ke2gwHKHP3z\",\"k6lOze3vTzP\",\"wtdBuXDwZYQ\",\"oRncQGhLYNE\",\"qVvitxEF2ck\",\"mepHuAA9l51\",\"X79FDd4EAgo\",\"QZtMuEEV9Vv\",\"bPqP6eRfkyn\",\"gUPhNWkSXvD\",\"HWXk4EBHUyk\",\"PuZOFApTSeo\",\"Mi4dWRtfIOC\",\"egjrZ1PHNtT\",\"OjXNuYyLaCJ\",\"Jyv7sjpl9bA\",\"oLuhRyYPxRO\",\"p9KfD6eaRvu\",\"IlnqGuxfQAw\",\"pXDcgDRz8Od\",\"W2KnxOMvmgE\",\"O63vIA5MVn6\",\"e2WgqiasKnD\",\"qIpBLa1SCZt\",\"s5aXfzOL456\",\"agEKP19IUKI\",\"ua5GXy2uhBR\",\"KYXbIQBQgP1\",\"CFPrsD3dNeb\",\"PQEpIeuSTCN\",\"JrSIoCOdTH2\",\"lxxASQqPUqd\",\"K3jhn3TXF3a\",\"lpQvlm9czYE\",\"g5A3hiJlwmI\",\"VhRX5JDVo7R\",\"CKkE4GBJekz\",\"Qc9lf4VM9bD\",\"EUUkKEDoNsf\",\"nX05QLraDhO\",\"sesv0eXljBq\",\"RhJbg8UD75Q\",\"PA1spYiNZfv\",\"qxbsDd9QYv6\",\"fAsj6a4nudH\",\"xa4F6gesVJm\",\"roGdTjEqLZQ\",\"BNFrspDBKel\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ZzYYXq4fJie.oucode",
        "Organisation unit code",
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

    // 7. Assert row values by name (sample validation: evenly spaced rows, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Rogbere CHC");
    validateRowValueByName(response, actualHeaders, 0, "ZzYYXq4fJie.oucode", "OU_254949");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2023-01-31 12:05:00.0");

    // Validate selected values for row index 9
    validateRowValueByName(response, actualHeaders, 9, "ouname", "Kurubonla CHC");
    validateRowValueByName(response, actualHeaders, 9, "ZzYYXq4fJie.oucode", "OU_226276");
    validateRowValueByName(response, actualHeaders, 9, "enrollmentdate", "2023-01-29 12:05:00.0");

    // Validate selected values for row index 18
    validateRowValueByName(response, actualHeaders, 18, "ouname", "Tongo Field CHC");
    validateRowValueByName(response, actualHeaders, 18, "ZzYYXq4fJie.oucode", "OU_222665");
    validateRowValueByName(response, actualHeaders, 18, "enrollmentdate", "2023-01-26 12:05:00.0");

    // Validate selected values for row index 27
    validateRowValueByName(response, actualHeaders, 27, "ouname", "Rokupr CHC");
    validateRowValueByName(response, actualHeaders, 27, "ZzYYXq4fJie.oucode", "OU_211237");
    validateRowValueByName(response, actualHeaders, 27, "enrollmentdate", "2023-01-24 12:05:00.0");

    // Validate selected values for row index 36
    validateRowValueByName(response, actualHeaders, 36, "ouname", "Ginger Hall Health Centre");
    validateRowValueByName(response, actualHeaders, 36, "ZzYYXq4fJie.oucode", "OU_278361");
    validateRowValueByName(response, actualHeaders, 36, "enrollmentdate", "2023-01-21 12:05:00.0");

    // Validate selected values for row index 45
    validateRowValueByName(response, actualHeaders, 45, "ouname", "Karlu CHC");
    validateRowValueByName(response, actualHeaders, 45, "ZzYYXq4fJie.oucode", "OU_260399");
    validateRowValueByName(response, actualHeaders, 45, "enrollmentdate", "2023-01-18 12:05:00.0");

    // Validate selected values for row index 54
    validateRowValueByName(response, actualHeaders, 54, "ouname", "Bumpeh Perri CHC");
    validateRowValueByName(response, actualHeaders, 54, "ZzYYXq4fJie.oucode", "OU_260423");
    validateRowValueByName(response, actualHeaders, 54, "enrollmentdate", "2023-01-16 12:05:00.0");

    // Validate selected values for row index 63
    validateRowValueByName(response, actualHeaders, 63, "ouname", "Praise Foundation CHC");
    validateRowValueByName(response, actualHeaders, 63, "ZzYYXq4fJie.oucode", "OU_1006");
    validateRowValueByName(response, actualHeaders, 63, "enrollmentdate", "2023-01-12 12:05:00.0");

    // Validate selected values for row index 72
    validateRowValueByName(response, actualHeaders, 72, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 72, "ZzYYXq4fJie.oucode", "OU_559");
    validateRowValueByName(response, actualHeaders, 72, "enrollmentdate", "2023-01-09 12:05:00.0");

    // Validate selected values for row index 81
    validateRowValueByName(response, actualHeaders, 81, "ouname", "Rokupr CHC");
    validateRowValueByName(response, actualHeaders, 81, "ZzYYXq4fJie.oucode", "OU_211237");
    validateRowValueByName(response, actualHeaders, 81, "enrollmentdate", "2023-01-06 12:05:00.0");

    // Validate selected values for row index 90
    validateRowValueByName(response, actualHeaders, 90, "ouname", "Bendugu CHC");
    validateRowValueByName(response, actualHeaders, 90, "ZzYYXq4fJie.oucode", "OU_268170");
    validateRowValueByName(response, actualHeaders, 90, "enrollmentdate", "2023-01-04 12:05:00.0");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "ouname", "Gerehun CHC");
    validateRowValueByName(response, actualHeaders, 99, "ZzYYXq4fJie.oucode", "OU_576");
    validateRowValueByName(response, actualHeaders, 99, "enrollmentdate", "2023-01-02 12:05:00.0");
  }

  @Test
  @Disabled("Temporarly disabled")
  public void stageAndEventStatusActiveAndEventDate2021() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,enrollmentdate,ZzYYXq4fJie.eventstatus,A03MvHHogjR.eventdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ZzYYXq4fJie.EVENT_STATUS:ACTIVE,A03MvHHogjR.EVENT_DATE:2021")
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
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"ZzYYXq4fJie.EVENT_STATUS\":{\"name\":\"Event status, Baby Postnatal\"},\"A03MvHHogjR.EVENT_DATE\":{\"name\":\"Report date, Birth\"}},\"dimensions\":{\"ZzYYXq4fJie.eventstatus\":[],\"A03MvHHogjR.occurreddate\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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
        "ZzYYXq4fJie.eventstatus",
        "eventstatus",
        "TEXT",
        "java.lang.String",
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

    // 7. Assert row values by name (sample validation: evenly spaced rows, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Youndu CHP");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.eventdate", "2021-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2022-12-29 12:05:00.0");

    // Validate selected values for row index 9
    validateRowValueByName(response, actualHeaders, 9, "ouname", "Mokobo MCHP");
    validateRowValueByName(
        response, actualHeaders, 9, "A03MvHHogjR.eventdate", "2021-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 9, "enrollmentdate", "2022-12-29 12:05:00.0");

    // Validate selected values for row index 18
    validateRowValueByName(response, actualHeaders, 18, "ouname", "Kalainkay MCHP");
    validateRowValueByName(
        response, actualHeaders, 18, "A03MvHHogjR.eventdate", "2021-12-29 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 18, "enrollmentdate", "2022-12-29 12:05:00.0");

    // Validate selected values for row index 27
    validateRowValueByName(response, actualHeaders, 27, "ouname", "Moyeamoh CHP");
    validateRowValueByName(
        response, actualHeaders, 27, "A03MvHHogjR.eventdate", "2021-12-28 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 27, "enrollmentdate", "2022-12-28 12:05:00.0");

    // Validate selected values for row index 36
    validateRowValueByName(response, actualHeaders, 36, "ouname", "Madina Loko CHP");
    validateRowValueByName(
        response, actualHeaders, 36, "A03MvHHogjR.eventdate", "2021-12-28 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 36, "enrollmentdate", "2022-12-28 12:05:00.0");

    // Validate selected values for row index 45
    validateRowValueByName(response, actualHeaders, 45, "ouname", "Kamiendor MCHP");
    validateRowValueByName(
        response, actualHeaders, 45, "A03MvHHogjR.eventdate", "2021-12-28 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 45, "enrollmentdate", "2022-12-28 12:05:00.0");

    // Validate selected values for row index 54
    validateRowValueByName(response, actualHeaders, 54, "ouname", "Bandajuma Sinneh MCHP");
    validateRowValueByName(
        response, actualHeaders, 54, "A03MvHHogjR.eventdate", "2021-12-28 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 54, "enrollmentdate", "2022-12-28 12:05:00.0");

    // Validate selected values for row index 63
    validateRowValueByName(response, actualHeaders, 63, "ouname", "Needy CHC");
    validateRowValueByName(
        response, actualHeaders, 63, "A03MvHHogjR.eventdate", "2021-12-27 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 63, "enrollmentdate", "2022-12-27 12:05:00.0");

    // Validate selected values for row index 72
    validateRowValueByName(response, actualHeaders, 72, "ouname", "Konda CHP");
    validateRowValueByName(
        response, actualHeaders, 72, "A03MvHHogjR.eventdate", "2021-12-27 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 72, "enrollmentdate", "2022-12-27 12:05:00.0");

    // Validate selected values for row index 81
    validateRowValueByName(response, actualHeaders, 81, "ouname", "Foredugu MCHP");
    validateRowValueByName(
        response, actualHeaders, 81, "A03MvHHogjR.eventdate", "2021-12-27 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 81, "enrollmentdate", "2022-12-27 12:05:00.0");

    // Validate selected values for row index 90
    validateRowValueByName(response, actualHeaders, 90, "ouname", "Sindadu MCHP");
    validateRowValueByName(
        response, actualHeaders, 90, "A03MvHHogjR.eventdate", "2021-12-26 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 90, "enrollmentdate", "2022-12-26 12:05:00.0");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "ouname", "MCH Static/U5");
    validateRowValueByName(
        response, actualHeaders, 99, "A03MvHHogjR.eventdate", "2021-12-26 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 99, "enrollmentdate", "2022-12-26 12:05:00.0");
  }
}
