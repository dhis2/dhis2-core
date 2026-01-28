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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR.eventdate\":{\"uid\":\"occurreddate\",\"code\":\"occurreddate\",\"valueType\":\"NUMBER\",\"name\":\"Report date\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\",\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"A03MvHHogjR.eventdate\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "24", "ou", "ImspTQPwCqd", "A03MvHHogjR.eventdate", "2022-05-01 00:00:00.0"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "16", "ou", "ImspTQPwCqd", "A03MvHHogjR.eventdate", "2022-05-07 00:00:00.0"));

    // Validate row exists with values from original row index 12
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "21", "ou", "ImspTQPwCqd", "A03MvHHogjR.eventdate", "2022-05-13 00:00:00.0"));

    // Validate row exists with values from original row index 18
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "19", "ou", "ImspTQPwCqd", "A03MvHHogjR.eventdate", "2022-05-19 00:00:00.0"));

    // Validate row exists with values from original row index 24
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "32", "ou", "ImspTQPwCqd", "A03MvHHogjR.eventdate", "2022-05-25 00:00:00.0"));

    // Validate row exists with values from original row index 30
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "27", "ou", "ImspTQPwCqd", "A03MvHHogjR.eventdate", "2022-05-31 00:00:00.0"));
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR.eventdate\":{\"uid\":\"occurreddate\",\"code\":\"occurreddate\",\"valueType\":\"NUMBER\",\"name\":\"Report date\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\",\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"A03MvHHogjR.eventdate\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "24", "ou", "ImspTQPwCqd", "A03MvHHogjR.eventdate", "2022-05-01 00:00:00.0"));

    // Validate row exists with values from original row index 3
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "30", "ou", "ImspTQPwCqd", "A03MvHHogjR.eventdate", "2022-05-04 00:00:00.0"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "16", "ou", "ImspTQPwCqd", "A03MvHHogjR.eventdate", "2022-05-07 00:00:00.0"));

    // Validate row exists with values from original row index 9
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value", "24", "ou", "ImspTQPwCqd", "A03MvHHogjR.eventdate", "2022-05-10 00:00:00.0"));
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ou\":{\"uid\":\"ou\",\"code\":\"ou\",\"valueType\":\"NUMBER\",\"name\":\"Organisation unit\",\"totalAggregationType\":\"SUM\"},\"2022\":{\"uid\":\"2022\",\"code\":\"2022\",\"endDate\":\"2022-12-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"2022\",\"startDate\":\"2022-01-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[\"2022\"],\"A03MvHHogjR.ou\":[\"Qr41Mw2MSjo\"]}}";
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
    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "17",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "Qr41Mw2MSjo",
            "A03MvHHogjR.ouname",
            "Senthai MCHP",
            "A03MvHHogjR.oucode",
            "OU_211240"));
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ou\":{\"uid\":\"ou\",\"code\":\"ou\",\"valueType\":\"NUMBER\",\"name\":\"Organisation unit\",\"totalAggregationType\":\"SUM\"},\"2022\":{\"uid\":\"2022\",\"code\":\"2022\",\"endDate\":\"2022-12-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"2022\",\"startDate\":\"2022-01-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[\"2022\"],\"A03MvHHogjR.ou\":[\"ImspTQPwCqd\"]}}";
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "10",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Panderu MCHP",
            "A03MvHHogjR.oucode",
            "OU_222702"));

    // Validate row exists with values from original row index 40
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "8",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Bangoma MCHP",
            "A03MvHHogjR.oucode",
            "OU_543045"));

    // Validate row exists with values from original row index 80
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "10",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Binkolo CHC",
            "A03MvHHogjR.oucode",
            "OU_193275"));

    // Validate row exists with values from original row index 120
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "10",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Bureh MCHP",
            "A03MvHHogjR.oucode",
            "OU_255052"));

    // Validate row exists with values from original row index 160
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "8",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Falaba CHP",
            "A03MvHHogjR.oucode",
            "OU_260425"));

    // Validate row exists with values from original row index 200
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "16",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Gao MCHP",
            "A03MvHHogjR.oucode",
            "OU_222738"));

    // Validate row exists with values from original row index 240
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "11",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Gbo-Lambayama 1 MCHP",
            "A03MvHHogjR.oucode",
            "OU_222697"));

    // Validate row exists with values from original row index 280
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "8",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Govt. Hosp. Kenema",
            "A03MvHHogjR.oucode",
            "OU_222716"));

    // Validate row exists with values from original row index 320
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "8",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Jormu MCHP",
            "A03MvHHogjR.oucode",
            "OU_579"));

    // Validate row exists with values from original row index 360
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "14",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Kamawornie CHP",
            "A03MvHHogjR.oucode",
            "OU_193290"));

    // Validate row exists with values from original row index 400
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "6",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Kawaya MCHP",
            "A03MvHHogjR.oucode",
            "OU_247050"));

    // Validate row exists with values from original row index 440
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "10",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Komneh CHP",
            "A03MvHHogjR.oucode",
            "OU_255002"));

    // Validate row exists with values from original row index 480
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "8",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Kpetema MCHP",
            "A03MvHHogjR.oucode",
            "OU_222698"));

    // Validate row exists with values from original row index 520
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "7",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Loppa CHP",
            "A03MvHHogjR.oucode",
            "OU_222732"));

    // Validate row exists with values from original row index 560
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "8",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Madina Fullah CHP",
            "A03MvHHogjR.oucode",
            "OU_193244"));

    // Validate row exists with values from original row index 600
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "14",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Makarie MCHP",
            "A03MvHHogjR.oucode",
            "OU_193253"));

    // Validate row exists with values from original row index 640
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "7",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Mangay Loko MCHP",
            "A03MvHHogjR.oucode",
            "OU_193265"));

    // Validate row exists with values from original row index 680
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "10",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Masanga Leprosy Hospital",
            "A03MvHHogjR.oucode",
            "OU_268159"));

    // Validate row exists with values from original row index 720
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "11",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Mayakie MCHP",
            "A03MvHHogjR.oucode",
            "OU_211267"));

    // Validate row exists with values from original row index 760
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "8",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Mokaiyegbeh MCHP",
            "A03MvHHogjR.oucode",
            "OU_247026"));

    // Validate row exists with values from original row index 800
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "14",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Nafaya MCHP",
            "A03MvHHogjR.oucode",
            "OU_849"));

    // Validate row exists with values from original row index 840
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "6",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Njagbwema CHP",
            "A03MvHHogjR.oucode",
            "OU_233403"));

    // Validate row exists with values from original row index 880
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "12",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Petifu Line MCHP",
            "A03MvHHogjR.oucode",
            "OU_268175"));

    // Validate row exists with values from original row index 920
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "15",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Roktolon MCHP",
            "A03MvHHogjR.oucode",
            "OU_803066"));

    // Validate row exists with values from original row index 960
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "6",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Sandaru CHC",
            "A03MvHHogjR.oucode",
            "OU_204860"));

    // Validate row exists with values from original row index 1000
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "6",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Shenge CHC",
            "A03MvHHogjR.oucode",
            "OU_247069"));

    // Validate row exists with values from original row index 1040
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "9",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Sumbuya CHC",
            "A03MvHHogjR.oucode",
            "OU_1050"));

    // Validate row exists with values from original row index 1080
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "7",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Tombo CHC",
            "A03MvHHogjR.oucode",
            "OU_278403"));

    // Validate row exists with values from original row index 1120
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "15",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Waterloo CHC",
            "A03MvHHogjR.oucode",
            "OU_278397"));

    // Validate row exists with values from original row index 1160
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "9",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "York CHC",
            "A03MvHHogjR.oucode",
            "OU_278400"));

    // Validate row exists with values from original row index 1165
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "9",
            "pe",
            "2022",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Zimmi CHC",
            "A03MvHHogjR.oucode",
            "OU_260382"));
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"202212\":{\"uid\":\"202212\",\"code\":\"202212\",\"endDate\":\"2022-12-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"December 2022\",\"startDate\":\"2022-12-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ou\":{\"uid\":\"ou\",\"code\":\"ou\",\"valueType\":\"NUMBER\",\"name\":\"Organisation unit\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[\"202212\"],\"A03MvHHogjR.ou\":[\"ImspTQPwCqd\"]}}";
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Adonkia CHP",
            "A03MvHHogjR.oucode",
            "OU_651071"));

    // Validate row exists with values from original row index 25
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Barakuya MCHP",
            "A03MvHHogjR.oucode",
            "OU_211225"));

    // Validate row exists with values from original row index 50
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Bombohun MCHP",
            "A03MvHHogjR.oucode",
            "OU_204864"));

    // Validate row exists with values from original row index 75
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Damballa CHC",
            "A03MvHHogjR.oucode",
            "OU_1095"));

    // Validate row exists with values from original row index 100
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "3",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Fodaya MCHP",
            "A03MvHHogjR.oucode",
            "OU_211217"));

    // Validate row exists with values from original row index 125
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Gbangbatoke CHC",
            "A03MvHHogjR.oucode",
            "OU_247016"));

    // Validate row exists with values from original row index 150
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Goderich MI Room",
            "A03MvHHogjR.oucode",
            "OU_278370"));

    // Validate row exists with values from original row index 175
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Jenner Wright Clinic",
            "A03MvHHogjR.oucode",
            "OU_278353"));

    // Validate row exists with values from original row index 200
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Kamasondo CHC",
            "A03MvHHogjR.oucode",
            "OU_255023"));

    // Validate row exists with values from original row index 225
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Kensay MCHP",
            "A03MvHHogjR.oucode",
            "OU_233330"));

    // Validate row exists with values from original row index 250
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Kono Bendu CHP",
            "A03MvHHogjR.oucode",
            "OU_204858"));

    // Validate row exists with values from original row index 275
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "4",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Levuma (Kandu Lep) CHC",
            "A03MvHHogjR.oucode",
            "OU_222726"));

    // Validate row exists with values from original row index 300
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "2",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Madopolahun MCHP",
            "A03MvHHogjR.oucode",
            "OU_204888"));

    // Validate row exists with values from original row index 325
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "2",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Makonkondey MCHP",
            "A03MvHHogjR.oucode",
            "OU_278367"));

    // Validate row exists with values from original row index 350
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Mansundu (Sandor) MCHP",
            "A03MvHHogjR.oucode",
            "OU_233372"));

    // Validate row exists with values from original row index 375
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "3",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Mathuraneh MCHP",
            "A03MvHHogjR.oucode",
            "OU_211242"));

    // Validate row exists with values from original row index 400
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Mogbongisseh MCHP",
            "A03MvHHogjR.oucode",
            "OU_247038"));

    // Validate row exists with values from original row index 425
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "3",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Nekabo CHC",
            "A03MvHHogjR.oucode",
            "OU_222705"));

    // Validate row exists with values from original row index 450
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Nonkoba CHP",
            "A03MvHHogjR.oucode",
            "OU_255040"));

    // Validate row exists with values from original row index 475
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "2",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Rofoindu CHP",
            "A03MvHHogjR.oucode",
            "OU_254970"));

    // Validate row exists with values from original row index 500
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "6",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Samamaia MCHP",
            "A03MvHHogjR.oucode",
            "OU_758927"));

    // Validate row exists with values from original row index 525
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "SLRCS (Koinadugu) Clinic",
            "A03MvHHogjR.oucode",
            "OU_226238"));

    // Validate row exists with values from original row index 550
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Tawahun MCHP",
            "A03MvHHogjR.oucode",
            "OU_222742"));

    // Validate row exists with values from original row index 575
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "2",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Vaama  (kpanga krim) MCHP",
            "A03MvHHogjR.oucode",
            "OU_260419"));

    // Validate row exists with values from original row index 600
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Yele CHC",
            "A03MvHHogjR.oucode",
            "OU_268207"));

    // Validate row exists with values from original row index 609
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "3",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.ouname",
            "Zimmi CHC",
            "A03MvHHogjR.oucode",
            "OU_260382"));
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"202212\":{\"uid\":\"202212\",\"code\":\"202212\",\"endDate\":\"2022-12-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"December 2022\",\"startDate\":\"2022-12-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ou\":{\"uid\":\"ou\",\"code\":\"ou\",\"valueType\":\"NUMBER\",\"name\":\"Organisation unit\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[\"202212\"],\"A03MvHHogjR.ou\":[\"O6uvpzGd5pu\",\"DiszpKrYNg8\"]}}";
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "O6uvpzGd5pu",
            "A03MvHHogjR.ouname",
            "Agape CHP",
            "A03MvHHogjR.oucode",
            "OU_678892"));

    // Validate row exists with values from original row index 7
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "O6uvpzGd5pu",
            "A03MvHHogjR.ouname",
            "Bum Kaku MCHP",
            "A03MvHHogjR.oucode",
            "OU_619"));

    // Validate row exists with values from original row index 14
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "2",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "O6uvpzGd5pu",
            "A03MvHHogjR.ouname",
            "Gbangbalia MCHP",
            "A03MvHHogjR.oucode",
            "OU_623"));

    // Validate row exists with values from original row index 21
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "O6uvpzGd5pu",
            "A03MvHHogjR.ouname",
            "Harvest Time MCHP",
            "A03MvHHogjR.oucode",
            "OU_1023"));

    // Validate row exists with values from original row index 28
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "O6uvpzGd5pu",
            "A03MvHHogjR.ouname",
            "Kindoyal Hospital",
            "A03MvHHogjR.oucode",
            "OU_172176"));

    // Validate row exists with values from original row index 35
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "O6uvpzGd5pu",
            "A03MvHHogjR.ouname",
            "Lyn Maternity MCHP",
            "A03MvHHogjR.oucode",
            "OU_73733"));

    // Validate row exists with values from original row index 42
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "O6uvpzGd5pu",
            "A03MvHHogjR.ouname",
            "Mercy Ship Hospital",
            "A03MvHHogjR.oucode",
            "OU_73747"));

    // Validate row exists with values from original row index 49
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "O6uvpzGd5pu",
            "A03MvHHogjR.ouname",
            "Ngolahun CHC",
            "A03MvHHogjR.oucode",
            "OU_678887"));

    // Validate row exists with values from original row index 56
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "2",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "O6uvpzGd5pu",
            "A03MvHHogjR.ouname",
            "Telu CHP",
            "A03MvHHogjR.oucode",
            "OU_824"));

    // Validate row exists with values from original row index 63
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "1",
            "pe",
            "202212",
            "A03MvHHogjR.ou",
            "O6uvpzGd5pu",
            "A03MvHHogjR.ouname",
            "Yengema CHP",
            "A03MvHHogjR.oucode",
            "OU_636"));
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\",\"name\":\"Organisation unit\"},\"A03MvHHogjR.scheduleddate\":{\"uid\":\"scheduleddate\",\"code\":\"scheduleddate\",\"valueType\":\"NUMBER\",\"name\":\"Scheduled date\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.scheduleddate\":[]}}";
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "38",
            "ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.scheduleddate",
            "2021-07-01 12:05:00.0"));

    // Validate row exists with values from original row index 5
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "30",
            "ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.scheduleddate",
            "2021-07-06 12:05:00.0"));

    // Validate row exists with values from original row index 10
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "31",
            "ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.scheduleddate",
            "2021-07-11 12:05:00.0"));

    // Validate row exists with values from original row index 15
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "40",
            "ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.scheduleddate",
            "2021-07-16 12:05:00.0"));

    // Validate row exists with values from original row index 20
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "24",
            "ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.scheduleddate",
            "2021-07-21 12:05:00.0"));

    // Validate row exists with values from original row index 25
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "42",
            "ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.scheduleddate",
            "2021-07-26 12:05:00.0"));

    // Validate row exists with values from original row index 29
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "28",
            "ou",
            "ImspTQPwCqd",
            "A03MvHHogjR.scheduleddate",
            "2021-07-30 12:05:00.0"));
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"202212\":{\"uid\":\"202212\",\"code\":\"202212\",\"endDate\":\"2022-12-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"December 2022\",\"startDate\":\"2022-12-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR.eventstatus\":{\"uid\":\"eventstatus\",\"code\":\"eventstatus\",\"valueType\":\"NUMBER\",\"name\":\"Event status\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"A03MvHHogjR.eventstatus\":[],\"pe\":[\"202212\"],\"ou\":[\"ImspTQPwCqd\"]}}";
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "858", "pe", "202212", "A03MvHHogjR.eventstatus", "ACTIVE"));
  }
}
