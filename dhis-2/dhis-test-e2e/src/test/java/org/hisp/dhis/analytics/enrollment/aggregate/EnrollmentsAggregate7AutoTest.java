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

/** Groups e2e tests for "/enrollments/aggregate" endpoint. */
public class EnrollmentsAggregate7AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void stageAndEventDateSpecificMonth() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,A03MvHHogjR.EVENT_DATE:202205");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        31,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR.occurreddate\":{\"uid\":\"occurreddate\",\"code\":\"occurreddate\",\"name\":\"occurreddate\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"A03MvHHogjR.occurreddate\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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
    validateRowValueByName(response, actualHeaders, 0, "value", "24");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.eventdate", "2022-05-01 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "value", "16");
    validateRowValueByName(
        response, actualHeaders, 6, "A03MvHHogjR.eventdate", "2022-05-07 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 6, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 12
    validateRowValueByName(response, actualHeaders, 12, "value", "21");
    validateRowValueByName(
        response, actualHeaders, 12, "A03MvHHogjR.eventdate", "2022-05-13 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 12, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 18
    validateRowValueByName(response, actualHeaders, 18, "value", "19");
    validateRowValueByName(
        response, actualHeaders, 18, "A03MvHHogjR.eventdate", "2022-05-19 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 18, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 24
    validateRowValueByName(response, actualHeaders, 24, "value", "32");
    validateRowValueByName(
        response, actualHeaders, 24, "A03MvHHogjR.eventdate", "2022-05-25 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 24, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 30
    validateRowValueByName(response, actualHeaders, 30, "value", "27");
    validateRowValueByName(
        response, actualHeaders, 30, "A03MvHHogjR.eventdate", "2022-05-31 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 30, "ou", "ImspTQPwCqd");
  }

  @Test
  public void stageAndEventDateWithRange() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,A03MvHHogjR.EVENT_DATE:2022-05-01_2022-05-10");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        10,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR.occurreddate\":{\"uid\":\"occurreddate\",\"code\":\"occurreddate\",\"name\":\"occurreddate\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"A03MvHHogjR.occurreddate\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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
    validateRowValueByName(response, actualHeaders, 0, "value", "24");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.eventdate", "2022-05-01 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "value", "30");
    validateRowValueByName(
        response, actualHeaders, 3, "A03MvHHogjR.eventdate", "2022-05-04 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 3, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "value", "16");
    validateRowValueByName(
        response, actualHeaders, 6, "A03MvHHogjR.eventdate", "2022-05-07 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 6, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 9
    validateRowValueByName(response, actualHeaders, 9, "value", "24");
    validateRowValueByName(
        response, actualHeaders, 9, "A03MvHHogjR.eventdate", "2022-05-10 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 9, "ou", "ImspTQPwCqd");
  }

  @Test
  public void stageAndSimpleOu() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=pe:2022,A03MvHHogjR.ou:Qr41Mw2MSjo");

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
        5,
        5); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ou\":{\"uid\":\"ou\",\"code\":\"ou\",\"name\":\"ou\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"2022\":{\"uid\":\"2022\",\"code\":\"2022\",\"name\":\"2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-01-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"}},\"dimensions\":{\"pe\":[\"2022\"],\"A03MvHHogjR.ou\":[\"Qr41Mw2MSjo\"]}}";
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
        "A03MvHHogjR.ou",
        "ou",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.oucode",
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
    validateRowValueByName(response, actualHeaders, 0, "value", "17");
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.oucode", "OU_211240");
  }

  @Test
  public void stageAndOuUserOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=pe:2022,A03MvHHogjR.ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1166,
        5,
        5); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ou\":{\"uid\":\"ou\",\"code\":\"ou\",\"name\":\"ou\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"2022\":{\"uid\":\"2022\",\"code\":\"2022\",\"name\":\"2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-01-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"}},\"dimensions\":{\"pe\":[\"2022\"],\"A03MvHHogjR.ou\":[\"ImspTQPwCqd\"]}}";
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
        "A03MvHHogjR.ou",
        "ou",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.oucode",
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

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "10",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Panderu MCHP",
            "A03MvHHogjR.oucode", "OU_222702"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "5",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Afro Arab Clinic",
            "A03MvHHogjR.oucode", "OU_278371"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "11",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Air Port Centre, Lungi",
            "A03MvHHogjR.oucode", "OU_255017"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "9",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Allen Town Health Post",
            "A03MvHHogjR.oucode", "OU_278337"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "8",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Baama CHC",
            "A03MvHHogjR.oucode", "OU_222681"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "5",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Bai Bureh Memorial Hospital",
            "A03MvHHogjR.oucode", "OU_255019"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "10",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Bambara MCHP",
            "A03MvHHogjR.oucode", "OU_222679"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "16",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Bandajuma Yawei CHC",
            "A03MvHHogjR.oucode", "OU_204938"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "8",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Bangoma MCHP",
            "A03MvHHogjR.oucode", "OU_543045"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "11",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Barakuya MCHP",
            "A03MvHHogjR.oucode", "OU_211225"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "12",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Batkanu CHC",
            "A03MvHHogjR.oucode", "OU_193303"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "14",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Benduma MCHP",
            "A03MvHHogjR.oucode", "OU_602"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "10",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Blessed Mokaba clinic",
            "A03MvHHogjR.oucode", "OU_278342"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "12",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Bo Govt. Hosp.",
            "A03MvHHogjR.oucode", "OU_15"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "9",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Bumpeh River CHP",
            "A03MvHHogjR.oucode", "OU_247027"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "12",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Catholic Clinic",
            "A03MvHHogjR.oucode", "OU_233405"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "10",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Connaught Hospital",
            "A03MvHHogjR.oucode", "OU_278320"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "7",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Dalakuru CHP",
            "A03MvHHogjR.oucode", "OU_758907"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "7",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Elshadai Clinic",
            "A03MvHHogjR.oucode", "OU_1138230"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "8",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Faabu CHP",
            "A03MvHHogjR.oucode", "OU_574"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "14",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Ginger Hall Health Centre",
            "A03MvHHogjR.oucode", "OU_278361"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "14",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Handicap Clinic",
            "A03MvHHogjR.oucode", "OU_233385"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "10",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Jormu CHP",
            "A03MvHHogjR.oucode", "OU_222708"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "10",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Koidu Govt. Hospital",
            "A03MvHHogjR.oucode", "OU_233387"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "15",
            "pe", "2022",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Lungi Govt. Hospital, Port Loko",
            "A03MvHHogjR.oucode", "OU_255010"));
  }

  @Test
  public void stageAndOuLevel() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=pe:202212,A03MvHHogjR.ou:LEVEL-H1KlN4QIauv");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);
    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        610,
        5,
        5); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"202212\":{\"uid\":\"202212\",\"code\":\"202212\",\"name\":\"December 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-12-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ou\":{\"uid\":\"ou\",\"code\":\"ou\",\"name\":\"ou\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[\"202212\"],\"A03MvHHogjR.ou\":[\"ImspTQPwCqd\"]}}";
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
        "A03MvHHogjR.ou",
        "ou",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.oucode",
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

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Adonkia CHP",
            "A03MvHHogjR.oucode", "OU_651071"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Barakuya MCHP",
            "A03MvHHogjR.oucode", "OU_211225"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Bendugu CHC",
            "A03MvHHogjR.oucode", "OU_268170"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "3",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Delken MCHP",
            "A03MvHHogjR.oucode", "OU_197399"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Fulamansa MCHP",
            "A03MvHHogjR.oucode", "OU_226217"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Gbalamuya MCHP",
            "A03MvHHogjR.oucode", "OU_211219"));

    //    validateRowExists(
    //            response,
    //            actualHeaders,
    //            Map.of(
    //                    "value", "1",
    //                    "pe", "202212",
    //                    "A03MvHHogjR.ou", "ImspTQPwCqd",
    //                    "A03MvHHogjR.ouname", "Gbonkomaria CHP",
    //                    "A03MvHHogjR.oucode", "OU_211235"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Grafton MCHP",
            "A03MvHHogjR.oucode", "OU_278398"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Jokibu MCHP",
            "A03MvHHogjR.oucode", "OU_204926"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "2",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Kagbo MCHP",
            "A03MvHHogjR.oucode", "OU_193268"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "2",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Kpewama MCHP",
            "A03MvHHogjR.oucode", "OU_1156"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Kroo Bay CHC",
            "A03MvHHogjR.oucode", "OU_278359"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Levuma Kai MCHP",
            "A03MvHHogjR.oucode", "OU_247065"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Mabom CHP",
            "A03MvHHogjR.oucode", "OU_268162"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Magbethy MCHP",
            "A03MvHHogjR.oucode", "OU_211231"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "3",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Makona MCHP",
            "A03MvHHogjR.oucode", "OU_651066"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "2",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Mamanso Sanka CHP",
            "A03MvHHogjR.oucode", "OU_268181"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "2",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Mapotolon CHC",
            "A03MvHHogjR.oucode", "OU_211248"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Massayeima MCHP",
            "A03MvHHogjR.oucode", "OU_204934"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "1",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Mattru UBC Hospital",
            "A03MvHHogjR.oucode", "OU_197392"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "2",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Njala CHP",
            "A03MvHHogjR.oucode", "OU_233401"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "2",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Pejewa CHC",
            "A03MvHHogjR.oucode", "OU_204927"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "2",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Royeiben MCHP",
            "A03MvHHogjR.oucode", "OU_255038"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "2",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Sienga CHP",
            "A03MvHHogjR.oucode", "OU_204921"));

    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "3",
            "pe", "202212",
            "A03MvHHogjR.ou", "ImspTQPwCqd",
            "A03MvHHogjR.ouname", "Zimmi CHC",
            "A03MvHHogjR.oucode", "OU_260382"));
  }

  @Test
  public void stageAndMultipleOus() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=pe:202212,A03MvHHogjR.ou:O6uvpzGd5pu;DiszpKrYNg8");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        64,
        5,
        5); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"202212\":{\"uid\":\"202212\",\"code\":\"202212\",\"name\":\"December 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-12-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ou\":{\"uid\":\"ou\",\"code\":\"ou\",\"name\":\"ou\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[\"202212\"],\"A03MvHHogjR.ou\":[\"O6uvpzGd5pu\",\"DiszpKrYNg8\"]}}";
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
        "A03MvHHogjR.ou",
        "ou",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.oucode",
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
    validateRowValueByName(response, actualHeaders, 0, "value", "1");
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.oucode", "OU_678892");

    // Validate selected values for row index 7
    validateRowValueByName(response, actualHeaders, 7, "value", "1");
    validateRowValueByName(response, actualHeaders, 7, "A03MvHHogjR.oucode", "OU_619");

    // Validate selected values for row index 14
    validateRowValueByName(response, actualHeaders, 14, "value", "2");
    validateRowValueByName(response, actualHeaders, 14, "A03MvHHogjR.oucode", "OU_623");

    // Validate selected values for row index 21
    validateRowValueByName(response, actualHeaders, 21, "value", "1");
    validateRowValueByName(response, actualHeaders, 21, "A03MvHHogjR.oucode", "OU_1023");

    // Validate selected values for row index 28
    validateRowValueByName(response, actualHeaders, 28, "value", "1");
    validateRowValueByName(response, actualHeaders, 28, "A03MvHHogjR.oucode", "OU_172176");

    // Validate selected values for row index 35
    validateRowValueByName(response, actualHeaders, 35, "value", "1");
    validateRowValueByName(response, actualHeaders, 35, "A03MvHHogjR.oucode", "OU_73733");

    // Validate selected values for row index 42
    validateRowValueByName(response, actualHeaders, 42, "value", "1");
    validateRowValueByName(response, actualHeaders, 42, "A03MvHHogjR.oucode", "OU_73747");

    // Validate selected values for row index 49
    validateRowValueByName(response, actualHeaders, 49, "value", "1");
    validateRowValueByName(response, actualHeaders, 49, "A03MvHHogjR.oucode", "OU_678887");

    // Validate selected values for row index 56
    validateRowValueByName(response, actualHeaders, 56, "value", "2");
    validateRowValueByName(response, actualHeaders, 56, "A03MvHHogjR.oucode", "OU_824");

    // Validate selected values for row index 63
    validateRowValueByName(response, actualHeaders, 63, "value", "1");
    validateRowValueByName(response, actualHeaders, 63, "A03MvHHogjR.oucode", "OU_636");
  }

  @Test
  public void stageAndScheduledDate() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,A03MvHHogjR.SCHEDULED_DATE:202107");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        30,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR.scheduleddate\":{\"uid\":\"scheduleddate\",\"code\":\"scheduleddate\",\"name\":\"scheduleddate\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.scheduleddate\":[]}}";
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

    // 7. Assert row values by name (sample validation: evenly spaced rows, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "value", "38");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.scheduleddate", "2021-07-01 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 5
    validateRowValueByName(response, actualHeaders, 5, "value", "30");
    validateRowValueByName(
        response, actualHeaders, 5, "A03MvHHogjR.scheduleddate", "2021-07-06 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 5, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 10
    validateRowValueByName(response, actualHeaders, 10, "value", "31");
    validateRowValueByName(
        response, actualHeaders, 10, "A03MvHHogjR.scheduleddate", "2021-07-11 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 10, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 15
    validateRowValueByName(response, actualHeaders, 15, "value", "40");
    validateRowValueByName(
        response, actualHeaders, 15, "A03MvHHogjR.scheduleddate", "2021-07-16 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 15, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 20
    validateRowValueByName(response, actualHeaders, 20, "value", "24");
    validateRowValueByName(
        response, actualHeaders, 20, "A03MvHHogjR.scheduleddate", "2021-07-21 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 20, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 25
    validateRowValueByName(response, actualHeaders, 25, "value", "42");
    validateRowValueByName(
        response, actualHeaders, 25, "A03MvHHogjR.scheduleddate", "2021-07-26 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 25, "ou", "ImspTQPwCqd");

    // Validate selected values for row index 29
    validateRowValueByName(response, actualHeaders, 29, "value", "28");
    validateRowValueByName(
        response, actualHeaders, 29, "A03MvHHogjR.scheduleddate", "2021-07-30 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 29, "ou", "ImspTQPwCqd");
  }

  @Test
  public void stageAndEventStatus() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=pe:202212,A03MvHHogjR.EVENT_STATUS:ACTIVE");

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
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"202212\":{\"uid\":\"202212\",\"code\":\"202212\",\"name\":\"December 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-12-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR.eventstatus\":{\"uid\":\"eventstatus\",\"code\":\"eventstatus\",\"name\":\"eventstatus\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"A03MvHHogjR.eventstatus\":[],\"pe\":[\"202212\"],\"ou\":[\"ImspTQPwCqd\"]}}";
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
        "A03MvHHogjR.eventstatus",
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

    // 7. Assert row values by name (sample validation: evenly spaced rows, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "value", "858");
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.eventstatus", "ACTIVE");
  }
}
