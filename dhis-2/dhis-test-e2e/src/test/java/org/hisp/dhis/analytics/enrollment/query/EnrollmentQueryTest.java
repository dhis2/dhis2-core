/*
 * Copyright (c) 2004-2022, University of Oslo
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
import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for Enrollments "/query" endpoint.
 *
 * @author maikel arabori
 */
public class EnrollmentQueryTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions enrollmentsActions = new AnalyticsEnrollmentsActions();

  @Test
  public void queryWithProgramAndProgramStageWhenTotalPagesIsFalse() throws JSONException {
    // --- Test Configuration ---
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = BooleanUtils.toBoolean(System.getProperty("expect.postgis", "true"));

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=A03MvHHogjR.UXz7xuGCEhU,lastupdated")
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd,A03MvHHogjR.UXz7xuGCEhU")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = enrollmentsActions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        100,
        17,
        14); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"A03MvHHogjR.UXz7xuGCEhU\":[]}}";
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
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "incidentdate",
        "Date of birth",
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
        "A03MvHHogjR.UXz7xuGCEhU",
        "MCH Weight (g)",
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
    validateRowValueByName(response, actualHeaders, 0, "pi", "KxXkjF6buFN");
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.UXz7xuGCEhU", "2313");
    validateRowValueByName(response, actualHeaders, 0, "tei", "uhubxsfLanV");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2022-04-02 02:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2022-04-02 02:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "COMPLETED");
    validateRowValueByName(response, actualHeaders, 0, "ou", "DiszpKrYNg8");

    // Validate selected values for row index 99
    validateRowValueByName(response, actualHeaders, 99, "pi", "Ff9wbfm05au");
    validateRowValueByName(response, actualHeaders, 99, "A03MvHHogjR.UXz7xuGCEhU", "2521");
    validateRowValueByName(response, actualHeaders, 99, "tei", "HJJMsNWg0SN");
    validateRowValueByName(response, actualHeaders, 99, "enrollmentdate", "2022-02-03 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 99, "incidentdate", "2022-02-03 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 99, "ouname", "Grey Bush CHC");
    validateRowValueByName(response, actualHeaders, 99, "programstatus", "ACTIVE");
    validateRowValueByName(response, actualHeaders, 99, "ou", "JZraNIfZ5JM");
  }

  @Test
  public void queryWithProgramAndRepeatableProgramStage() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "dimension=edqlbukwRfQ[-2].vANAXwtLwcT,edqlbukwRfQ[10].vANAXwtLwcT,pe:LAST_12_MONTHS,ou:ImspTQPwCqd")
            .add(
                "headers=ou,ounamehierarchy,edqlbukwRfQ[-2].vANAXwtLwcT,edqlbukwRfQ[10].vANAXwtLwcT")
            .add("stage=edqlbukwRfQ")
            .add("displayProperty=NAME")
            .add("outputType=ENROLLMENT")
            .add("desc=edqlbukwRfQ[-2].vANAXwtLwcT,ounamehierarchy,enrollmentdate")
            .add("totalPages=false")
            .add("pageSize=2")
            .add("page=4")
            .add("rowContext=true")
            .add("relativePeriodDate=2023-06-27");

    // When
    ApiResponse response = enrollmentsActions.query().get("WSGAb5XwJ3Y", JSON, JSON, params);
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(2)));

    validateHeader(response, 0, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        2,
        "edqlbukwRfQ[-2].vANAXwtLwcT",
        "WHOMCH Hemoglobin value",
        "NUMBER",
        "java.lang.Double",
        false,
        true,
        "edqlbukwRfQ",
        "index:-2 startDate:null endDate: null dimension: edqlbukwRfQ[-2].vANAXwtLwcT",
        -2);
    validateHeader(
        response,
        3,
        "edqlbukwRfQ[10].vANAXwtLwcT",
        "WHOMCH Hemoglobin value",
        "NUMBER",
        "java.lang.Double",
        false,
        true,
        "edqlbukwRfQ",
        "index:10 startDate:null endDate: null dimension: edqlbukwRfQ[10].vANAXwtLwcT",
        10);

    validateRowContext(response, 0, 3, "ND");
    validateRowContext(response, 1, 3, "ND");

    validateRow(
        response,
        0,
        List.of("fmkqsEx6MRo", "Sierra Leone / Port Loko / Koya / Mabora MCHP", "25", ""));
    validateRow(
        response,
        1,
        List.of(
            "GCbYmPqcOOP",
            "Sierra Leone / Port Loko / Bureh Kasseh Maconteh / Romeni MCHP",
            "25",
            ""));
  }

  @Test
  public void queryWithoutPeriodDimension() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:USER_ORGUNIT,edqlbukwRfQ.vANAXwtLwcT")
            .add("headers=ouname,edqlbukwRfQ.vANAXwtLwcT,enrollmentdate")
            .add("pageSize=5")
            .add("page=1");

    // When
    ApiResponse response = enrollmentsActions.query().get("WSGAb5XwJ3Y", JSON, JSON, params);
    response.validate().statusCode(200).body("headers", hasSize(equalTo(3)));

    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "edqlbukwRfQ.vANAXwtLwcT",
        "WHOMCH Hemoglobin value",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        2,
        "enrollmentdate",
        "Date of first visit",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
  }
}
