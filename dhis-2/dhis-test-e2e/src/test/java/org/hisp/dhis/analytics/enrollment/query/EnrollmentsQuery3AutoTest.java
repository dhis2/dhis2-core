/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowContext;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
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
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"2022\":{\"name\":\"2022\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"EPEcjy3FWmI.lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"EPEcjy3FWmI.lJTx9EZ1dk1\":[]}}";
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
        "DATE",
        "java.time.LocalDate",
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
            .add("desc=ouname,enrollmentdate");

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
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"2022\":{\"name\":\"2022\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"EPEcjy3FWmI.lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"EPEcjy3FWmI.lJTx9EZ1dk1\":[]}}";
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
        "DATE",
        "java.time.LocalDate",
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
    validateRow(response, 0, List.of("sonkoya MCHP", "", "2022-03-07 12:38:08.598"));
    validateRow(response, 1, List.of("sonkoya MCHP", "", "2022-03-05 12:28:46.886"));
    validateRow(response, 2, List.of("sonkoya MCHP", "", "2022-02-11 12:43:10.757"));
    validateRow(response, 3, List.of("sonkoya MCHP", "", "2022-01-26 12:40:08.658"));
    validateRow(response, 4, List.of("sonkoya MCHP", "", "2022-01-23 12:41:30.493"));
    validateRow(response, 5, List.of("kamba mamudia", "", "2022-04-17 12:42:44.887"));
    validateRow(response, 6, List.of("kamba mamudia", "", "2022-02-26 12:31:45.327"));
    validateRow(response, 7, List.of("kamba mamudia", "", "2022-02-16 12:33:59.273"));
    validateRow(response, 8, List.of("kamba mamudia", "", "2022-02-09 12:43:21.288"));
    validateRow(response, 9, List.of("kamba mamudia", "", "2022-02-09 12:40:38.934"));
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
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"2022\":{\"name\":\"2022\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"EPEcjy3FWmI.lJTx9EZ1dk1\":{\"uid\":\"lJTx9EZ1dk1\",\"code\":\"DE_860003\",\"name\":\"Tb lab Glucose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"EPEcjy3FWmI.lJTx9EZ1dk1\":[]}}";
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
        "DATE",
        "java.time.LocalDate",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Motorbong MCHP", "1", "", "2022-02-18 12:27:49.129"));
  }

  @Test
  public void queryRandomquery12() throws JSONException {
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
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(8)))
        .body("height", equalTo(8))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"CWaAcQYKVpq\":{\"uid\":\"CWaAcQYKVpq\",\"name\":\"Foci investigation & classification\",\"description\":\"Includes the details on the foci investigation (including information on households, population, geography, breeding sites, species types, vector behaviour) as well as its final classification at the time of the investigation. This is a repeatable stage as foci can be investigated more than once and may change their classification as time goes on. \"},\"CWaAcQYKVpq.fyjPqlHE7Dn\":{\"uid\":\"fyjPqlHE7Dn\",\"name\":\"Proven insecticide resistance\",\"description\":\"Is there proven insecticide resistance\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"LAST_YEAR\":{\"name\":\"Last year\"},\"uvMKOn1oWvd\":{\"uid\":\"uvMKOn1oWvd\",\"name\":\"Foci response\",\"description\":\"Details the public health response conducted within the foci  (including diagnosis and treatment activities, vector control actions and the effectiveness/results of the response). This is a repeatable stage as multiple public health responses for the same foci can occur depending on its classification at the time of investigation.\"},\"fyjPqlHE7Dn\":{\"uid\":\"fyjPqlHE7Dn\",\"name\":\"Proven insecticide resistance\",\"description\":\"Is there proven insecticide resistance\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"M3xtLkYBlKI\":{\"uid\":\"M3xtLkYBlKI\",\"name\":\"Malaria focus investigation\",\"description\":\"It allows to register new focus areas in the system. Each focus area needs to be investigated and classified. Includes the relevant identifiers for the foci including the name and geographical details including the locality and its area. \"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"CWaAcQYKVpq.fyjPqlHE7Dn\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "pi", "Enrollment", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "tei", "Tracked entity instance", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        2,
        "enrollmentdate",
        "Date of Focus Registration",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeader(
        response, 3, "incidentdate", "Incident date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(response, 4, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 5, "createdbydisplayname", "Created by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        6,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 7, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true);
    validateHeader(response, 8, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 9, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(response, 10, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 11, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        12,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 13, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 14, "programstatus", "Program status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 15, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        16,
        "CWaAcQYKVpq[-1].fyjPqlHE7Dn",
        "Proven insecticide resistance",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert rowContext
    validateRowContext(response, 0, 16, "NS");
    validateRowContext(response, 1, 16, "ND");
    validateRowContext(response, 2, 16, "ND");
    validateRowContext(response, 3, 16, "ND");
    validateRowContext(response, 4, 16, "ND");
    validateRowContext(response, 5, 16, "ND");
    validateRowContext(response, 6, 16, "ND");
    validateRowContext(response, 7, 16, "ND");

    // Assert rows.
    validateRow(
        response,
        List.of(
            "TRE0GT7eh7Q",
            "s4NfKOuayqG",
            "2021-11-13 00:00:00.0",
            "2021-11-13 00:00:00.0",
            "healthworker1",
            "",
            "",
            "2019-08-21 13:29:44.942",
            "",
            "",
            "",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "COMPLETED",
            "DiszpKrYNg8",
            ""));
    validateRow(
        response,
        List.of(
            "o6xZZq7MPFH",
            "nVtfCS1gUZH",
            "2021-11-12 00:00:00.0",
            "2021-11-12 07:24:56.327",
            "healthworker3",
            "",
            "",
            "2019-08-21 13:29:31.708",
            "",
            "",
            "",
            "Njandama MCHP",
            "Sierra Leone / Bo / Badjia / Njandama MCHP",
            "OU_167609",
            "ACTIVE",
            "g8upMTyEZGZ",
            ""));
    validateRow(
        response,
        List.of(
            "aOc1W0Xb7Yj",
            "neR4cmMY22o",
            "2021-11-07 00:00:00.0",
            "2021-11-12 04:20:50.918",
            "healthworker2",
            "",
            "",
            "2019-08-21 13:29:37.117",
            "",
            "",
            "",
            "Njandama MCHP",
            "Sierra Leone / Bo / Badjia / Njandama MCHP",
            "OU_167609",
            "ACTIVE",
            "g8upMTyEZGZ",
            ""));
    validateRow(
        response,
        List.of(
            "zRfAPUpjoG3",
            "S3JjTA4QMNe",
            "2021-11-10 00:00:00.0",
            "2021-11-10 03:53:09.193",
            "karoline",
            "",
            "",
            "2019-08-21 13:29:28.064",
            "",
            "",
            "",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "DiszpKrYNg8",
            ""));
    validateRow(
        response,
        List.of(
            "HbLOTSi7jvg",
            "yKO4L1jaUbg",
            "2021-11-07 00:00:00.0",
            "2021-11-10 03:53:09.027",
            "karoline",
            "",
            "",
            "2019-08-21 13:29:24.678",
            "",
            "",
            "",
            "Njandama MCHP",
            "Sierra Leone / Bo / Badjia / Njandama MCHP",
            "OU_167609",
            "ACTIVE",
            "g8upMTyEZGZ",
            ""));
    validateRow(
        response,
        List.of(
            "V8uPJuhvlL7",
            "dNpxRu1mWG5",
            "2021-10-16 00:00:00.0",
            "2021-10-16 10:13:57.545",
            "testmalaria",
            "",
            "",
            "2019-08-21 13:29:39.311",
            "",
            "",
            "",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "DiszpKrYNg8",
            ""));
    validateRow(
        response,
        List.of(
            "FonAHm0CsIR",
            "lebQyoZNcp7",
            "2021-10-16 00:00:00.0",
            "2021-10-16 10:13:57.42",
            "testmalaria",
            "",
            "",
            "2019-08-21 13:29:14.578",
            "",
            "",
            "",
            "Njandama MCHP",
            "Sierra Leone / Bo / Badjia / Njandama MCHP",
            "OU_167609",
            "ACTIVE",
            "g8upMTyEZGZ",
            ""));
    validateRow(
        response,
        List.of(
            "ZjixUoY4jE8",
            "Imv2o18b9wX",
            "2021-07-26 00:00:00.0",
            "2021-07-26 00:00:00.0",
            "braimbault",
            "",
            "",
            "2019-08-21 13:29:21.574",
            "",
            "",
            "",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "DiszpKrYNg8",
            ""));
  }
}
