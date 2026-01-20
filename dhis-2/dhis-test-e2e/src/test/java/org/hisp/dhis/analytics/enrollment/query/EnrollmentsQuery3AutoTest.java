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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderExistence;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowContext;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowValueByName;
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
public class EnrollmentsQuery3AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void queryRandom9() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,EPEcjy3FWmI.lJTx9EZ1dk1,enrollmentdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2022")
            .add("rowContext=true")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,EPEcjy3FWmI.lJTx9EZ1dk1:IN:NV");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"2022\":{\"name\":\"2022\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"EPEcjy3FWmI.lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"EPEcjy3FWmI.lJTx9EZ1dk1\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "EPEcjy3FWmI.lJTx9EZ1dk1",
        "Tb lab Glucose",
        "TRUE_ONLY",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        2,
        "enrollmentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
  }

  @Test
  public void queryRandom10() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,EPEcjy3FWmI[-1].lJTx9EZ1dk1,enrollmentdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2022")
            .add("rowContext=true")
            .add("outputType=ENROLLMENT")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,EPEcjy3FWmI[-1].lJTx9EZ1dk1")
            .add("desc=enrollmentdate");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(10)))
        .body("height", equalTo(10))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"2022\":{\"name\":\"2022\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"EPEcjy3FWmI.lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"EPEcjy3FWmI.lJTx9EZ1dk1\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "EPEcjy3FWmI[-1].lJTx9EZ1dk1",
        "Tb lab Glucose",
        "TRUE_ONLY",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        2,
        "enrollmentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rowContext
    validateRowContext(response, 0, 1, "ND");
    validateRowContext(response, 1, 1, "ND");
    validateRowContext(response, 2, 1, "ND");
    validateRowContext(response, 3, 1, "ND");
    validateRowContext(response, 4, 1, "ND");
    validateRowContext(response, 5, 1, "ND");
    validateRowContext(response, 6, 1, "ND");
    validateRowContext(response, 7, 1, "ND");
    validateRowContext(response, 8, 1, "ND");
    validateRowContext(response, 9, 1, "ND");

    // Assert rows.
    validateRow(response, 0, List.of("Ngelehun CHC", "", "2022-08-11 12:32:30.524"));
    validateRow(response, 1, List.of("Panderu MCHP", "", "2022-04-26 12:43:23.827"));
    validateRow(response, 2, List.of("Bumbuna CHC", "", "2022-04-26 12:34:21.521"));
    validateRow(response, 3, List.of("Gissiwolo MCHP", "", "2022-04-26 12:33:26.455"));
    validateRow(response, 4, List.of("Tengbewabu MCHP", "", "2022-04-26 12:32:04.965"));
    validateRow(response, 5, List.of("Bombordu MCHP", "", "2022-04-25 12:37:33.687"));
    validateRow(response, 6, List.of("Macrogba MCHP", "", "2022-04-25 12:34:07.072"));
    validateRow(response, 7, List.of("Bandawor MCHP", "", "2022-04-25 12:33:45.644"));
    validateRow(response, 8, List.of("Rofutha MCHP", "", "2022-04-25 12:29:47.209"));
    validateRow(response, 9, List.of("Nyandehun MCHP", "", "2022-04-25 12:29:32.927"));
  }

  @Test
  public void queryRandom11() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add(
                "headers=ouname,EPEcjy3FWmI[0].lJTx9EZ1dk1,EPEcjy3FWmI[-1].lJTx9EZ1dk1,enrollmentdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2022")
            .add("rowContext=true")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,EPEcjy3FWmI[0].lJTx9EZ1dk1,EPEcjy3FWmI[-1].lJTx9EZ1dk1:IN:NV")
            .add("desc=ouname,enrollmentdate");
    ;

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"2022\":{\"name\":\"2022\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"EPEcjy3FWmI.lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"EPEcjy3FWmI.lJTx9EZ1dk1\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "EPEcjy3FWmI[0].lJTx9EZ1dk1",
        "Tb lab Glucose",
        "TRUE_ONLY",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        2,
        "EPEcjy3FWmI[-1].lJTx9EZ1dk1",
        "Tb lab Glucose",
        "TRUE_ONLY",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        3,
        "enrollmentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Motorbong MCHP", "1", "", "2022-02-18 12:27:49.129"));
  }

  @Test
  public void queryRandomquery12() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,pe:LAST_YEAR,CWaAcQYKVpq[-1].fyjPqlHE7Dn")
            .add("skipRounding=true")
            .add("relativePeriodDate=2022-01-01")
            .add("desc=incidentdate");

    // When
    ApiResponse response = actions.query().get("M3xtLkYBlKI", JSON, JSON, params);

    // Then
    validateResponseStructure(
        response,
        expectPostgis,
        8,
        17,
        14); // Pass runtime flag, row count, and expected header counts

    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"CWaAcQYKVpq\":{\"uid\":\"CWaAcQYKVpq\",\"name\":\"Foci investigation & classification\",\"description\":\"Includes the details on the foci investigation (including information on households, population, geography, breeding sites, species types, vector behaviour) as well as its final classification at the time of the investigation. This is a repeatable stage as foci can be investigated more than once and may change their classification as time goes on. \"},\"CWaAcQYKVpq.fyjPqlHE7Dn\":{\"uid\":\"fyjPqlHE7Dn\",\"name\":\"Proven insecticide resistance\",\"description\":\"Is there proven insecticide resistance\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"LAST_YEAR\":{\"name\":\"Last year\"},\"uvMKOn1oWvd\":{\"uid\":\"uvMKOn1oWvd\",\"name\":\"Foci response\",\"description\":\"Details the public health response conducted within the foci  (including diagnosis and treatment activities, vector control actions and the effectiveness\\/results of the response). This is a repeatable stage as multiple public health responses for the same foci can occur depending on its classification at the time of investigation.\"},\"fyjPqlHE7Dn\":{\"uid\":\"fyjPqlHE7Dn\",\"name\":\"Proven insecticide resistance\",\"description\":\"Is there proven insecticide resistance\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"M3xtLkYBlKI\":{\"uid\":\"M3xtLkYBlKI\",\"name\":\"Malaria focus investigation\",\"description\":\"It allows to register new focus areas in the system. Each focus area needs to be investigated and classified. Includes the relevant identifiers for the foci including the name and geographical details including the locality and its area. \"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"CWaAcQYKVpq.fyjPqlHE7Dn\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "pi", "Enrollment", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "tei", "Tracked entity", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentdate",
        "Date of Focus Registration",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "incidentdate",
        "Incident date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
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
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
        "TEXT",
        "java.lang.String",
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
        response,
        actualHeaders,
        "CWaAcQYKVpq[-1].fyjPqlHE7Dn",
        "Proven insecticide resistance",
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
    validateRowValueByName(response, actualHeaders, 0, "pi", "TRE0GT7eh7Q");
    validateRowValueByName(response, actualHeaders, 0, "CWaAcQYKVpq[-1].fyjPqlHE7Dn", "");
    validateRowValueByName(response, actualHeaders, 0, "tei", "s4NfKOuayqG");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2021-11-13 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2021-11-13 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "COMPLETED");
    validateRowValueByName(response, actualHeaders, 0, "ou", "DiszpKrYNg8");

    // Validate selected values for row index 7
    validateRowValueByName(response, actualHeaders, 7, "pi", "ZjixUoY4jE8");
    validateRowValueByName(response, actualHeaders, 7, "CWaAcQYKVpq[-1].fyjPqlHE7Dn", "");
    validateRowValueByName(response, actualHeaders, 7, "tei", "Imv2o18b9wX");
    validateRowValueByName(response, actualHeaders, 7, "enrollmentdate", "2021-07-26 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 7, "incidentdate", "2021-07-26 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 7, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 7, "programstatus", "ACTIVE");
    validateRowValueByName(response, actualHeaders, 7, "ou", "DiszpKrYNg8");
  }

  @Test
  public void queryRandomQuery13() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=incidentdate,ouname")
            .add("headers=ouname,enrollmentdate,incidentdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=202301,202201")
            .add("rowContext=true")
            .add("pageSize=5")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("incidentDate=202201,202301")
            .add("dimension=ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":5,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"202201\":{\"name\":\"January 2022\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"202301\":{\"name\":\"January 2023\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "enrollmentdate",
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        2,
        "incidentdate",
        "Date of birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(
        response, 0, List.of("Baiwala CHP", "2022-01-01 12:05:00.0", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        1,
        List.of("Bandajuma Sinneh MCHP", "2022-01-01 12:05:00.0", "2022-01-01 12:05:00.0"));
    validateRow(
        response,
        2,
        List.of("Banka Makuloh MCHP", "2022-01-01 12:05:00.0", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 3, List.of("Conakry Dee CHC", "2022-01-01 12:05:00.0", "2022-01-01 12:05:00.0"));
    validateRow(
        response, 4, List.of("Dankawalie MCHP", "2022-01-01 12:05:00.0", "2022-01-01 12:05:00.0"));
  }

  @Test
  public void queryRandomQuery14() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=incidentdate,ouname")
            .add("headers=ouname,enrollmentdate,incidentdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=202308")
            .add("rowContext=true")
            .add("pageSize=5")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("incidentDate=202308,202308")
            .add("dimension=ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":5,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"202308\":{\"name\":\"August 2023\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "enrollmentdate",
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        2,
        "incidentdate",
        "Date of birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of("Banka Makuloh MCHP", "2023-08-01 12:05:00.0", "2023-08-01 12:05:00.0"));
    validateRow(
        response,
        1,
        List.of("Baoma Station CHP", "2023-08-01 12:05:00.0", "2023-08-01 12:05:00.0"));
    validateRow(
        response, 2, List.of("Bomie MCHP", "2023-08-01 12:05:00.0", "2023-08-01 12:05:00.0"));
    validateRow(
        response, 3, List.of("Bumban MCHP", "2023-08-01 12:05:00.0", "2023-08-01 12:05:00.0"));
    validateRow(
        response,
        4,
        List.of("Gbonkoh Kareneh MCHP", "2023-08-01 12:05:00.0", "2023-08-01 12:05:00.0"));
  }

  @Test
  public void queryRandomQuery15() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=incidentdate,ouname")
            .add("headers=ouname,enrollmentdate,incidentdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=5")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("incidentDate=202301,202401")
            .add("dimension=ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":5,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"202301\":{\"name\":\"January 2023\"},\"202401\":{\"name\":\"January 2024\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "enrollmentdate",
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        2,
        "incidentdate",
        "Date of birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(
        response, 0, List.of("Ngelehun CHC", "2023-01-01 00:00:00.0", "2023-01-01 00:00:00.0"));
    validateRow(
        response, 1, List.of("Fulamansa MCHP", "2023-01-01 12:05:00.0", "2023-01-01 12:05:00.0"));
    validateRow(
        response,
        2,
        List.of("Kordebotehun MCHP", "2023-01-01 12:05:00.0", "2023-01-01 12:05:00.0"));
    validateRow(
        response, 3, List.of("Largo CHC", "2023-01-01 12:05:00.0", "2023-01-01 12:05:00.0"));
    validateRow(
        response, 4, List.of("Manjeihun MCHP", "2023-01-01 12:05:00.0", "2023-01-01 12:05:00.0"));
  }
}
