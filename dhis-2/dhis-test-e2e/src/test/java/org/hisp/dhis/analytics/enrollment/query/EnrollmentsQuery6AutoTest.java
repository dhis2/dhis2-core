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

import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderExistence;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowValueByName;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.dependsOn.DependsOn;
import org.hisp.dhis.test.e2e.dependsOn.Resource;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/enrollments/query" endpoint. */
public class EnrollmentsQuery6AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  @DependsOn(
      files = {"pi-creation-date.json"},
      delete = true)
  public void queryProgramIndicatorCreationDate(List<Resource> resource) throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();
    String piUid = resource.get(0).uid();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=enrollmentdate,ouname")
            .add("headers=ouname,enrollmentdate,%s".formatted(piUid))
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=%s,ou:DiszpKrYNg8;g8upMTyEZGZ".formatted(piUid));

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        48,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"uid\":\"DiszpKrYNg8\",\"code\":\"OU_559\",\"name\":\"Ngelehun CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"g8upMTyEZGZ\":{\"uid\":\"g8upMTyEZGZ\",\"code\":\"OU_167609\",\"name\":\"Njandama MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"%s\":{\"uid\":\"%s\",\"code\":\"PI_CREATION_DATE\",\"name\":\"ENROLLMENT_CREATION_DATE\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"DiszpKrYNg8\",\"g8upMTyEZGZ\"],\"%s\":[]}}"
            .formatted(piUid, piUid, piUid);
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
        "%s".formatted(piUid),
        "ENROLLMENT_CREATION_DATE",
        "NUMBER",
        "java.lang.Double",
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
    validateRowValueByName(response, actualHeaders, 0, "%s".formatted(piUid), "36.42");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2022-02-10 12:05:00.0");

    // Validate selected values for row index 47
    validateRowValueByName(response, actualHeaders, 47, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 47, "%s".formatted(piUid), "32.28");
    validateRowValueByName(response, actualHeaders, 47, "enrollmentdate", "2023-12-29 12:05:00.0");
  }

  @Test
  @DependsOn(
      files = {"pi-event-status.json"},
      delete = true)
  public void queryProgramIndicatorEventStatusFilter(List<Resource> resource) throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();
    String piUid = resource.get(0).uid();

    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=enrollmentdate,ouname")
            .add("headers=ouname,%s,enrollmentdate".formatted(piUid))
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=LAST_10_YEARS")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=ou:DiszpKrYNg8;g8upMTyEZGZ;jNb63DIHuwU,%s".formatted(piUid));

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        62,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"uid\":\"DiszpKrYNg8\",\"code\":\"OU_559\",\"name\":\"Ngelehun CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"g8upMTyEZGZ\":{\"uid\":\"g8upMTyEZGZ\",\"code\":\"OU_167609\",\"name\":\"Njandama MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"%s\":{\"uid\":\"%s\",\"code\":\"PI_EVENT_STATUS\",\"name\":\"ENROLLMENT_EVENT_STATUS_FILTER\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"LAST_10_YEARS\":{\"name\":\"Last 10 years\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"jNb63DIHuwU\":{\"uid\":\"jNb63DIHuwU\",\"code\":\"OU_573\",\"name\":\"Baoma Station CHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"%s\":[],\"pe\":[],\"ou\":[\"DiszpKrYNg8\",\"g8upMTyEZGZ\",\"jNb63DIHuwU\"]}}"
            .formatted(piUid, piUid, piUid);
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
        "%s".formatted(piUid),
        "ENROLLMENT_EVENT_STATUS_FILTER",
        "NUMBER",
        "java.lang.Double",
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

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Baoma Station CHP");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2022-01-06 12:05:00.0");

    // Validate selected values for row index 61
    validateRowValueByName(response, actualHeaders, 61, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 61, "enrollmentdate", "2023-12-29 12:05:00.0");
  }

  @Test
  @DependsOn(
      files = {"pi-count-if-value.json"},
      delete = true)
  public void queryCountIfValue(List<Resource> resource) throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();
    String piUid = resource.get(0).uid();
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=enrollmentdate,ouname")
            .add("headers=ouname,enrollmentdate,%s".formatted(piUid))
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=LAST_5_YEARS")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add(
                "dimension=%s:GE:1,ou:DiszpKrYNg8;g8upMTyEZGZ;jNb63DIHuwU;ImspTQPwCqd"
                    .formatted(piUid));

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"DiszpKrYNg8\":{\"uid\":\"DiszpKrYNg8\",\"code\":\"OU_559\",\"name\":\"Ngelehun CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"g8upMTyEZGZ\":{\"uid\":\"g8upMTyEZGZ\",\"code\":\"OU_167609\",\"name\":\"Njandama MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"%s\":{\"uid\":\"%s\",\"code\":\"ENROLLMENT_COUNT_IF_VALUE\",\"name\":\"ENROLLMENT_COUNT_IF_VALUE\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"jNb63DIHuwU\":{\"uid\":\"jNb63DIHuwU\",\"code\":\"OU_573\",\"name\":\"Baoma Station CHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"}},\"dimensions\":{\"%s\":[],\"pe\":[],\"ou\":[\"DiszpKrYNg8\",\"g8upMTyEZGZ\",\"jNb63DIHuwU\",\"ImspTQPwCqd\"]}}"
            .formatted(piUid, piUid, piUid);
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
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "%s".formatted(piUid),
        "ENROLLMENT_COUNT_IF_VALUE",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Bai Largo MCHP");
    validateRowValueByName(response, actualHeaders, 0, "%s".formatted(piUid), "1");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2021-02-22 12:28:59.5");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "ouname", "Sandayeima MCHP");
    validateRowValueByName(response, actualHeaders, 99, "%s".formatted(piUid), "1");
    validateRowValueByName(
        response, actualHeaders, 99, "enrollmentdate", "2021-02-28 12:42:18.148");
  }

  @DisplayName("Enrollment query using createdDate as time field")
  @Test
  public void financialYear2022SepCreatedDate() throws JSONException {
    // sql: where (((created >= '2017-01-01' and created < '2017-07-01')))

    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,GxdhnY5wmHq,lastupdated")
            .add("createdDate=2017S1")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("asc=lastupdated")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,GxdhnY5wmHq");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        5,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"2017S1\":{\"name\":\"January - June 2017\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"GxdhnY5wmHq\":[]}}";
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
        "GxdhnY5wmHq",
        "Average weight (g)",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
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
    validateRowValueByName(response, actualHeaders, 0, "lastupdated", "2018-01-20 10:41:01.537");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 4, "lastupdated", "2018-05-08 20:54:31.858");
  }

  @Test
  @DisplayName("Enrollment query using completedDate as time field")
  public void financialYear2022SepCompletedDate() throws JSONException {
    // sql: where (((completeddate >= '2022-09-01' and completeddate < '2023-09-01')))

    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,GxdhnY5wmHq,lastupdated")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("asc=lastupdated")
            .add("dimension=ou:USER_ORGUNIT,GxdhnY5wmHq")
            .add("completedDate=2022Sep");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        5,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"2022Sep\":{\"name\":\"September 2022 - August 2023\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"GxdhnY5wmHq\":[]}}";
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
        "GxdhnY5wmHq",
        "Average weight (g)",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "GxdhnY5wmHq", "2327");
    validateRowValueByName(response, actualHeaders, 0, "lastupdated", "2017-11-16 12:26:42.851");

    validateRowValueByName(response, actualHeaders, 1, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 1, "GxdhnY5wmHq", "3766.5");
    validateRowValueByName(response, actualHeaders, 1, "lastupdated", "2017-11-16 12:54:26.958");

    validateRowValueByName(response, actualHeaders, 2, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 2, "GxdhnY5wmHq", "3387.5");
    validateRowValueByName(response, actualHeaders, 2, "lastupdated", "2018-01-20 10:41:01.537");

    validateRowValueByName(response, actualHeaders, 3, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 3, "GxdhnY5wmHq", "3338.5");
    validateRowValueByName(response, actualHeaders, 3, "lastupdated", "2018-01-20 10:42:54.423");

    validateRowValueByName(response, actualHeaders, 4, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 4, "GxdhnY5wmHq", "4817");
    validateRowValueByName(response, actualHeaders, 4, "lastupdated", "2018-01-20 10:44:33.776");
  }
}
