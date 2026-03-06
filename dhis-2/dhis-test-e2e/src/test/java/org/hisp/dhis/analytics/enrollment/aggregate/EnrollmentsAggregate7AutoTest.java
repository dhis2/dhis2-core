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
            // .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            // .add("outputType=ENROLLMENT")
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\",\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"202205\":{\"uid\":\"202205\",\"code\":\"202205\",\"endDate\":\"2022-05-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"202205\",\"startDate\":\"2022-05-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"A03MvHHogjR.eventdate\":[\"202205\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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
        "Report date",
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\",\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"A03MvHHogjR.eventdate\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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
        "Report date",
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
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"2022\":{\"uid\":\"2022\",\"code\":\"2022\",\"endDate\":\"2022-12-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"2022\",\"startDate\":\"2022-01-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[\"2022\"],\"A03MvHHogjR.ou\":[\"Qr41Mw2MSjo\"]}}";
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
        "Organisation unit",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "17", "pe", "2022", "A03MvHHogjR.ou", "Qr41Mw2MSjo"));
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"2022\":{\"uid\":\"2022\",\"code\":\"2022\",\"endDate\":\"2022-12-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"2022\",\"startDate\":\"2022-01-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[\"2022\"],\"A03MvHHogjR.ou\":[\"ImspTQPwCqd\"]}}";
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
        "Organisation unit",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "11015", "pe", "2022", "A03MvHHogjR.ou", "ImspTQPwCqd"));
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"202212\":{\"uid\":\"202212\",\"code\":\"202212\",\"endDate\":\"2022-12-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"December 2022\",\"startDate\":\"2022-12-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"}},\"dimensions\":{\"pe\":[\"202212\"],\"A03MvHHogjR.ou\":[\"ImspTQPwCqd\"]}}";
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
        "Organisation unit",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "858", "pe", "202212", "A03MvHHogjR.ou", "ImspTQPwCqd"));
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"202212\":{\"uid\":\"202212\",\"code\":\"202212\",\"endDate\":\"2022-12-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"December 2022\",\"startDate\":\"2022-12-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"}},\"dimensions\":{\"pe\":[\"202212\"],\"A03MvHHogjR.ou\":[\"O6uvpzGd5pu\",\"DiszpKrYNg8\"]}}";
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
        "Organisation unit",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "84", "pe", "202212", "A03MvHHogjR.ou", "O6uvpzGd5pu"));
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\",\"name\":\"Organisation unit\"},\"A03MvHHogjR.scheduleddate\":{\"name\":\"Scheduled date\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"202107\":{\"uid\":\"202107\",\"code\":\"202107\",\"endDate\":\"2021-07-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"202107\",\"startDate\":\"2021-07-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.scheduleddate\":[\"202107\"]}}";
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
        "Scheduled date",
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
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"202212\":{\"uid\":\"202212\",\"code\":\"202212\",\"endDate\":\"2022-12-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"December 2022\",\"startDate\":\"2022-12-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR.eventstatus\":{\"name\":\"Event status\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"}},\"dimensions\":{\"A03MvHHogjR.eventstatus\":[],\"pe\":[\"202212\"],\"ou\":[\"ImspTQPwCqd\"]}}";
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
        "Event status",
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

  @Test
  public void stageAndMultipleStages() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,PUZaKR0Jh2k.EVENT_DATE:202205,edqlbukwRfQ.EVENT_STATUS:COMPLETED;ACTIVE");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        30,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"PUZaKR0Jh2k.eventdate\":{\"name\":\"Date of birth\"},\"PFDfvmGpsR3\":{\"uid\":\"PFDfvmGpsR3\",\"name\":\"Care at birth\",\"description\":\"Intrapartum care \\/ Childbirth \\/ Labour and delivery\"},\"bbKtnxRZKEP\":{\"uid\":\"bbKtnxRZKEP\",\"name\":\"Postpartum care visit\",\"description\":\"Provision of care for the mother for some weeks after delivery\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\",\"name\":\"Organisation unit\"},\"PUZaKR0Jh2k\":{\"uid\":\"PUZaKR0Jh2k\",\"name\":\"Previous deliveries\",\"description\":\"Table for recording earlier deliveries\"},\"edqlbukwRfQ\":{\"uid\":\"edqlbukwRfQ\",\"name\":\"Second antenatal care visit\",\"description\":\"Antenatal care visit\"},\"WZbXY0S00lP\":{\"uid\":\"WZbXY0S00lP\",\"name\":\"First antenatal care visit\",\"description\":\"First antenatal care visit\"},\"edqlbukwRfQ.eventstatus\":{\"name\":\"Event status\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"202205\":{\"uid\":\"202205\",\"code\":\"202205\",\"endDate\":\"2022-05-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"202205\",\"startDate\":\"2022-05-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"PUZaKR0Jh2k.eventdate\":[\"202205\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"edqlbukwRfQ.eventstatus\":[]}}";
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
        "edqlbukwRfQ.eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "PUZaKR0Jh2k.eventdate",
        "Date of birth",
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
            "1",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE",
            "PUZaKR0Jh2k.eventdate",
            "2022-05-01 12:05:00.0"));

    // Validate row exists with values from original row index 5
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "3",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE",
            "PUZaKR0Jh2k.eventdate",
            "2022-05-06 12:05:00.0"));

    // Validate row exists with values from original row index 10
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "3",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE",
            "PUZaKR0Jh2k.eventdate",
            "2022-05-11 12:05:00.0"));

    // Validate row exists with values from original row index 15
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "3",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE",
            "PUZaKR0Jh2k.eventdate",
            "2022-05-16 12:05:00.0"));

    // Validate row exists with values from original row index 20
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "3",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE",
            "PUZaKR0Jh2k.eventdate",
            "2022-05-21 12:05:00.0"));

    // Validate row exists with values from original row index 25
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "6",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE",
            "PUZaKR0Jh2k.eventdate",
            "2022-05-26 12:05:00.0"));

    // Validate row exists with values from original row index 29
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "3",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE",
            "PUZaKR0Jh2k.eventdate",
            "2022-05-30 12:05:00.0"));
  }

  @Test
  public void stageAndMultipleStagesSameStage() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,edqlbukwRfQ.EVENT_DATE:202205,edqlbukwRfQ.EVENT_STATUS:COMPLETED;ACTIVE");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        30,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":100,\"page\":1},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"PFDfvmGpsR3\":{\"uid\":\"PFDfvmGpsR3\",\"name\":\"Care at birth\",\"description\":\"Intrapartum care \\/ Childbirth \\/ Labour and delivery\"},\"bbKtnxRZKEP\":{\"uid\":\"bbKtnxRZKEP\",\"name\":\"Postpartum care visit\",\"description\":\"Provision of care for the mother for some weeks after delivery\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\",\"name\":\"Organisation unit\"},\"PUZaKR0Jh2k\":{\"uid\":\"PUZaKR0Jh2k\",\"name\":\"Previous deliveries\",\"description\":\"Table for recording earlier deliveries\"},\"edqlbukwRfQ\":{\"uid\":\"edqlbukwRfQ\",\"name\":\"Second antenatal care visit\",\"description\":\"Antenatal care visit\"},\"WZbXY0S00lP\":{\"uid\":\"WZbXY0S00lP\",\"name\":\"First antenatal care visit\",\"description\":\"First antenatal care visit\"},\"edqlbukwRfQ.eventdate\":{\"name\":\"Date of visit\"},\"edqlbukwRfQ.eventstatus\":{\"name\":\"Event status\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"202205\":{\"uid\":\"202205\",\"code\":\"202205\",\"endDate\":\"2022-05-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"202205\",\"startDate\":\"2022-05-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"edqlbukwRfQ.eventdate\":[\"202205\"],\"edqlbukwRfQ.eventstatus\":[]}}";
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
        "edqlbukwRfQ.eventdate",
        "Date of visit",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "edqlbukwRfQ.eventstatus",
        "Event status",
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
            "6",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventdate",
            "2022-05-01 12:05:00.0",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE"));

    // Validate row exists with values from original row index 5
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "10",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventdate",
            "2022-05-06 12:05:00.0",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE"));

    // Validate row exists with values from original row index 10
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "11",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventdate",
            "2022-05-11 12:05:00.0",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE"));

    // Validate row exists with values from original row index 15
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "18",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventdate",
            "2022-05-16 12:05:00.0",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE"));

    // Validate row exists with values from original row index 20
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "16",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventdate",
            "2022-05-21 12:05:00.0",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE"));

    // Validate row exists with values from original row index 25
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "15",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventdate",
            "2022-05-26 12:05:00.0",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE"));

    // Validate row exists with values from original row index 29
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "value",
            "12",
            "ou",
            "ImspTQPwCqd",
            "edqlbukwRfQ.eventdate",
            "2022-05-30 12:05:00.0",
            "edqlbukwRfQ.eventstatus",
            "ACTIVE"));
  }
}
