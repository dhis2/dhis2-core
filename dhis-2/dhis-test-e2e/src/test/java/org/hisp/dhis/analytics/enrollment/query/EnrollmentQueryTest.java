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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowContext;

import java.util.List;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for Enrollments "/query" endpoint.
 *
 * @author maikel arabori
 */
public class EnrollmentQueryTest extends AnalyticsApiTest {
  private AnalyticsEnrollmentsActions enrollmentsActions = new AnalyticsEnrollmentsActions();

  @Test
  public void queryWithProgramAndProgramStageWhenTotalPagesIsFalse() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd,A03MvHHogjR.UXz7xuGCEhU")
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("outputType=ENROLLMENT")
            .add("asc=A03MvHHogjR.UXz7xuGCEhU,lastupdated")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("page=1")
            .add("relativePeriodDate=2022-09-27");

    // When
    ApiResponse response = enrollmentsActions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(100)))
        .body("metaData.pager.page", equalTo(1))
        .body("metaData.pager.pageSize", equalTo(100))
        .body("metaData.pager.isLastPage", is(false))
        .body("metaData.pager", not(hasKey("total")))
        .body("metaData.pager", not(hasKey("pageCount")))
        .body("metaData.items.ImspTQPwCqd.name", equalTo("Sierra Leone"))
        .body("metaData.items.IpHINAT79UW.name", equalTo("Child Programme"))
        .body("metaData.items.ZzYYXq4fJie.name", equalTo("Baby Postnatal"))
        .body("metaData.items.A03MvHHogjR.name", equalTo("Birth"))
        .body("metaData.items.ou.name", equalTo("Organisation unit"))
        .body("metaData.items.LAST_12_MONTHS.name", equalTo("Last 12 months"))
        .body("metaData.dimensions.pe", hasSize(equalTo(0)))
        .body("metaData.dimensions.ou", hasSize(equalTo(1)))
        .body("metaData.dimensions.ou", hasItem("ImspTQPwCqd"))
        .body("metaData.dimensions.\"A03MvHHogjR.UXz7xuGCEhU\"", hasSize(equalTo(0)))
        .body("height", equalTo(100))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17));

    // Validate headers
    validateHeader(response, 0, "pi", "Enrollment", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "tei", "Tracked entity", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        2,
        "enrollmentdate",
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        3,
        "incidentdate",
        "Date of birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
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
        response,
        7,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
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
        "A03MvHHogjR.UXz7xuGCEhU",
        "MCH Weight (g)",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Validate the first three rows, as samples.
    validateRow(
        response,
        0,
        List.of(
            "KxXkjF6buFN",
            "uhubxsfLanV",
            "2022-04-02 02:00:00.0",
            "2022-04-02 02:00:00.0",
            "",
            ",  ()",
            ",  ()",
            "2017-11-16 12:26:42.851",
            "",
            "",
            "",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "COMPLETED",
            "DiszpKrYNg8",
            "2313.0"));

    validateRow(
        response,
        1,
        List.of(
            "xieBMWGVUhY",
            "Ss42PAKPV4p",
            "2022-01-09 12:05:00.0",
            "2022-01-09 12:05:00.0",
            "",
            ",  ()",
            ",  ()",
            "2018-08-06 21:15:40.207",
            "",
            "",
            "",
            "Sienga CHP",
            "Sierra Leone / Kailahun / Dea / Sienga CHP",
            "OU_204921",
            "ACTIVE",
            "a1E6QWBTEwX",
            "2500.0"));

    validateRow(
        response,
        2,
        List.of(
            "B5X5DWgtrSu",
            "bB8bbcI5hQO",
            "2022-07-18 12:05:00.0",
            "2022-07-18 12:05:00.0",
            "",
            ",  ()",
            ",  ()",
            "2018-08-06 21:15:41.333",
            "",
            "",
            "",
            "Masumana MCHP",
            "Sierra Leone / Port Loko / Koya / Masumana MCHP",
            "OU_254965",
            "ACTIVE",
            "UlgEReuUPM4",
            "2500.0"));
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
        List.of("fmkqsEx6MRo", "Sierra Leone / Port Loko / Koya / Mabora MCHP", "25.0", ""));
    validateRow(
        response,
        1,
        List.of(
            "GCbYmPqcOOP",
            "Sierra Leone / Port Loko / Bureh Kasseh Maconteh / Romeni MCHP",
            "25.0",
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
