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
package org.hisp.dhis.analytics.tei;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsTeiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/trackedEntities/query" endpoint. */
public class TeiQuery4AutoTest extends AnalyticsApiTest {
  private AnalyticsTeiActions actions = new AnalyticsTeiActions();

  @Test
  public void sortByGlobalOuAsc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ouname")
            .add(
                "headers=gHGyrwKPzej,ciq2USN94oJ,IpHINAT79UW.ouname,IpHINAT79UW.enrollmentdate,IpHINAT79UW.programstatus,IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO,ouname")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=gHGyrwKPzej,ciq2USN94oJ,IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO,ou:USER_ORGUNIT")
            .add("programStatus=IpHINAT79UW.ACTIVE")
            .add("relativePeriodDate=2017-01-27");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(7)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(7))
        .body("headerWidth", equalTo(7));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"GQY2lXrypjO\":{\"uid\":\"GQY2lXrypjO\",\"code\":\"DE_2006099\",\"name\":\"MCH Infant Weight  (g)\",\"description\":\"Infant weight in grams\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"IpHINAT79UW.enrollmentdate\":{\"name\":\"Date of enrollment\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO\":{\"uid\":\"GQY2lXrypjO\",\"code\":\"DE_2006099\",\"name\":\"MCH Infant Weight  (g)\",\"description\":\"Infant weight in grams\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"GQY2lXrypjO\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "gHGyrwKPzej", "Birth date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 1, "ciq2USN94oJ", "Civil status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        2,
        "IpHINAT79UW.ouname",
        "Organisation Unit Name, Child Programme",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        3,
        "IpHINAT79UW.enrollmentdate",
        "Date of enrollment, Child Programme",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        4,
        "IpHINAT79UW.programstatus",
        "Program Status, Child Programme",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        5,
        "IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO",
        "MCH Infant Weight  (g), Child Programme, Baby Postnatal",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response, 6, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "", "", " Panderu MCHP", "2022-04-09 12:05:00.0", "ACTIVE", "3225.0", " Panderu MCHP"));
    validateRow(
        response,
        1,
        List.of(
            "", "", " Panderu MCHP", "2022-09-20 12:05:00.0", "ACTIVE", "3317.0", " Panderu MCHP"));
    validateRow(
        response,
        2,
        List.of(
            "", "", " Panderu MCHP", "2022-01-10 12:05:00.0", "ACTIVE", "3670.0", " Panderu MCHP"));
    validateRow(
        response,
        3,
        List.of(
            "", "", " Panderu MCHP", "2022-05-02 12:05:00.0", "ACTIVE", "3447.0", " Panderu MCHP"));
    validateRow(
        response,
        4,
        List.of(
            "", "", " Panderu MCHP", "2023-09-06 12:05:00.0", "ACTIVE", "3784.0", " Panderu MCHP"));
    validateRow(
        response,
        5,
        List.of(
            "", "", " Panderu MCHP", "2023-04-19 12:05:00.0", "ACTIVE", "3686.0", " Panderu MCHP"));
    validateRow(
        response,
        6,
        List.of(
            "", "", " Panderu MCHP", "2022-10-18 12:05:00.0", "ACTIVE", "3490.0", " Panderu MCHP"));
    validateRow(
        response,
        7,
        List.of(
            "", "", " Panderu MCHP", "2022-02-28 12:05:00.0", "ACTIVE", "2559.0", " Panderu MCHP"));
    validateRow(
        response,
        8,
        List.of(
            "", "", " Panderu MCHP", "2022-08-18 12:05:00.0", "ACTIVE", "2546.0", " Panderu MCHP"));
    validateRow(
        response,
        9,
        List.of("", "", " Panderu MCHP", "2023-12-21 12:05:00.0", "ACTIVE", "", " Panderu MCHP"));
    validateRow(
        response,
        10,
        List.of(
            "", "", " Panderu MCHP", "2022-02-25 12:05:00.0", "ACTIVE", "3548.0", " Panderu MCHP"));
    validateRow(
        response,
        11,
        List.of(
            "", "", " Panderu MCHP", "2022-10-15 12:05:00.0", "ACTIVE", "3346.0", " Panderu MCHP"));
    validateRow(
        response,
        12,
        List.of(
            "", "", " Panderu MCHP", "2023-07-15 12:05:00.0", "ACTIVE", "3845.0", " Panderu MCHP"));
    validateRow(
        response,
        13,
        List.of(
            "", "", " Panderu MCHP", "2022-05-27 12:05:00.0", "ACTIVE", "3090.0", " Panderu MCHP"));
    validateRow(
        response,
        14,
        List.of("", "", "Adonkia CHP", "2023-03-22 12:05:00.0", "ACTIVE", "3538.0", "Adonkia CHP"));
    validateRow(
        response,
        15,
        List.of("", "", "Adonkia CHP", "2022-11-08 12:05:00.0", "ACTIVE", "2984.0", "Adonkia CHP"));
    validateRow(
        response,
        16,
        List.of("", "", "Adonkia CHP", "2023-05-14 12:05:00.0", "ACTIVE", "3626.0", "Adonkia CHP"));
    validateRow(
        response,
        17,
        List.of("", "", "Adonkia CHP", "2022-04-30 12:05:00.0", "ACTIVE", "3229.0", "Adonkia CHP"));
    validateRow(
        response,
        18,
        List.of("", "", "Adonkia CHP", "2022-09-21 12:05:00.0", "ACTIVE", "3859.0", "Adonkia CHP"));
    validateRow(
        response,
        19,
        List.of("", "", "Adonkia CHP", "2022-08-22 12:05:00.0", "ACTIVE", "3244.0", "Adonkia CHP"));
    validateRow(
        response,
        20,
        List.of("", "", "Adonkia CHP", "2022-08-25 12:05:00.0", "ACTIVE", "3599.0", "Adonkia CHP"));
    validateRow(
        response,
        21,
        List.of("", "", "Adonkia CHP", "2023-06-03 12:05:00.0", "ACTIVE", "3548.0", "Adonkia CHP"));
    validateRow(
        response,
        22,
        List.of("", "", "Adonkia CHP", "2022-12-04 12:05:00.0", "ACTIVE", "2995.0", "Adonkia CHP"));
    validateRow(
        response,
        23,
        List.of("", "", "Adonkia CHP", "2023-12-06 12:05:00.0", "ACTIVE", "2854.0", "Adonkia CHP"));
    validateRow(
        response,
        24,
        List.of("", "", "Adonkia CHP", "2022-08-24 12:05:00.0", "ACTIVE", "3963.0", "Adonkia CHP"));
    validateRow(
        response,
        25,
        List.of("", "", "Adonkia CHP", "2023-09-04 12:05:00.0", "ACTIVE", "2629.0", "Adonkia CHP"));
    validateRow(
        response,
        26,
        List.of("", "", "Adonkia CHP", "2022-03-23 12:05:00.0", "ACTIVE", "3371.0", "Adonkia CHP"));
    validateRow(
        response,
        27,
        List.of("", "", "Adonkia CHP", "2023-01-07 12:05:00.0", "ACTIVE", "3243.0", "Adonkia CHP"));
    validateRow(
        response,
        28,
        List.of("", "", "Adonkia CHP", "2023-12-06 12:05:00.0", "ACTIVE", "3521.0", "Adonkia CHP"));
    validateRow(
        response,
        29,
        List.of("", "", "Adonkia CHP", "2023-12-20 12:05:00.0", "ACTIVE", "", "Adonkia CHP"));
    validateRow(
        response,
        30,
        List.of("", "", "Adonkia CHP", "2023-03-17 12:05:00.0", "ACTIVE", "2607.0", "Adonkia CHP"));
    validateRow(
        response,
        31,
        List.of("", "", "Adonkia CHP", "2022-05-08 12:05:00.0", "ACTIVE", "3116.0", "Adonkia CHP"));
    validateRow(
        response,
        32,
        List.of("", "", "Adonkia CHP", "2022-05-12 12:05:00.0", "ACTIVE", "3165.0", "Adonkia CHP"));
    validateRow(
        response,
        33,
        List.of("", "", "Adonkia CHP", "2022-02-09 12:05:00.0", "ACTIVE", "3974.0", "Adonkia CHP"));
    validateRow(
        response,
        34,
        List.of("", "", "Adonkia CHP", "2022-01-26 12:05:00.0", "ACTIVE", "3652.0", "Adonkia CHP"));
    validateRow(
        response,
        35,
        List.of("", "", "Adonkia CHP", "2023-11-15 12:05:00.0", "ACTIVE", "3371.0", "Adonkia CHP"));
    validateRow(
        response,
        36,
        List.of("", "", "Adonkia CHP", "2022-07-10 12:05:00.0", "ACTIVE", "3813.0", "Adonkia CHP"));
    validateRow(
        response,
        37,
        List.of("", "", "Adonkia CHP", "2022-09-28 12:05:00.0", "ACTIVE", "3700.0", "Adonkia CHP"));
    validateRow(
        response,
        38,
        List.of(
            "",
            "",
            "Afro Arab Clinic",
            "2022-09-03 12:05:00.0",
            "ACTIVE",
            "3069.0",
            "Afro Arab Clinic"));
    validateRow(
        response,
        39,
        List.of(
            "",
            "",
            "Afro Arab Clinic",
            "2022-01-19 12:05:00.0",
            "ACTIVE",
            "3781.0",
            "Afro Arab Clinic"));
    validateRow(
        response,
        40,
        List.of(
            "", "", "Afro Arab Clinic", "2023-12-24 12:05:00.0", "ACTIVE", "", "Afro Arab Clinic"));
    validateRow(
        response,
        41,
        List.of(
            "",
            "",
            "Afro Arab Clinic",
            "2022-11-09 12:05:00.0",
            "ACTIVE",
            "2927.0",
            "Afro Arab Clinic"));
    validateRow(
        response,
        42,
        List.of(
            "",
            "",
            "Afro Arab Clinic",
            "2023-12-11 12:05:00.0",
            "ACTIVE",
            "3574.0",
            "Afro Arab Clinic"));
    validateRow(
        response,
        43,
        List.of(
            "",
            "",
            "Afro Arab Clinic",
            "2022-07-23 12:05:00.0",
            "ACTIVE",
            "2767.0",
            "Afro Arab Clinic"));
    validateRow(
        response,
        44,
        List.of(
            "",
            "",
            "Afro Arab Clinic",
            "2023-01-02 12:05:00.0",
            "ACTIVE",
            "2747.0",
            "Afro Arab Clinic"));
    validateRow(
        response,
        45,
        List.of(
            "",
            "",
            "Afro Arab Clinic",
            "2022-01-03 12:05:00.0",
            "ACTIVE",
            "2888.0",
            "Afro Arab Clinic"));
    validateRow(
        response,
        46,
        List.of(
            "",
            "",
            "Afro Arab Clinic",
            "2023-09-14 12:05:00.0",
            "ACTIVE",
            "3463.0",
            "Afro Arab Clinic"));
    validateRow(
        response,
        47,
        List.of(
            "",
            "",
            "Afro Arab Clinic",
            "2023-09-02 12:05:00.0",
            "ACTIVE",
            "3291.0",
            "Afro Arab Clinic"));
    validateRow(
        response,
        48,
        List.of(
            "",
            "",
            "Afro Arab Clinic",
            "2023-05-20 12:05:00.0",
            "ACTIVE",
            "3213.0",
            "Afro Arab Clinic"));
    validateRow(
        response,
        49,
        List.of(
            "",
            "",
            "Afro Arab Clinic",
            "2023-01-03 12:05:00.0",
            "ACTIVE",
            "3637.0",
            "Afro Arab Clinic"));
    validateRow(
        response,
        50,
        List.of("", "", "Agape CHP", "2023-11-27 12:05:00.0", "ACTIVE", "2527.0", "Agape CHP"));
    validateRow(
        response,
        51,
        List.of("", "", "Agape CHP", "2023-10-23 12:05:00.0", "ACTIVE", "2569.0", "Agape CHP"));
    validateRow(
        response,
        52,
        List.of("", "", "Agape CHP", "2022-07-29 12:05:00.0", "ACTIVE", "2749.0", "Agape CHP"));
    validateRow(
        response,
        53,
        List.of("", "", "Agape CHP", "2023-01-02 12:05:00.0", "ACTIVE", "3175.0", "Agape CHP"));
    validateRow(
        response,
        54,
        List.of("", "", "Agape CHP", "2022-01-03 12:05:00.0", "ACTIVE", "3609.0", "Agape CHP"));
    validateRow(
        response,
        55,
        List.of("", "", "Agape CHP", "2022-09-26 12:05:00.0", "ACTIVE", "2600.0", "Agape CHP"));
    validateRow(
        response,
        56,
        List.of("", "", "Agape CHP", "2023-05-02 12:05:00.0", "ACTIVE", "3437.0", "Agape CHP"));
    validateRow(
        response,
        57,
        List.of("", "", "Agape CHP", "2022-10-05 12:05:00.0", "ACTIVE", "3592.0", "Agape CHP"));
    validateRow(
        response,
        58,
        List.of("", "", "Agape CHP", "2022-01-22 12:05:00.0", "ACTIVE", "3696.0", "Agape CHP"));
    validateRow(
        response,
        59,
        List.of("", "", "Agape CHP", "2022-01-13 12:05:00.0", "ACTIVE", "2973.0", "Agape CHP"));
    validateRow(
        response,
        60,
        List.of("", "", "Agape CHP", "2022-12-29 12:05:00.0", "ACTIVE", "3581.0", "Agape CHP"));
    validateRow(
        response,
        61,
        List.of("", "", "Agape CHP", "2023-06-27 12:05:00.0", "ACTIVE", "3338.0", "Agape CHP"));
    validateRow(
        response,
        62,
        List.of("", "", "Agape CHP", "2023-07-24 12:05:00.0", "ACTIVE", "3812.0", "Agape CHP"));
    validateRow(
        response,
        63,
        List.of("", "", "Agape CHP", "2023-03-01 12:05:00.0", "ACTIVE", "2870.0", "Agape CHP"));
    validateRow(
        response,
        64,
        List.of("", "", "Agape CHP", "2022-09-25 12:05:00.0", "ACTIVE", "3525.0", "Agape CHP"));
    validateRow(
        response,
        65,
        List.of("", "", "Agape CHP", "2022-03-24 12:05:00.0", "ACTIVE", "2771.0", "Agape CHP"));
    validateRow(
        response,
        66,
        List.of("", "", "Agape CHP", "2022-05-21 12:05:00.0", "ACTIVE", "3994.0", "Agape CHP"));
    validateRow(
        response,
        67,
        List.of("", "", "Agape CHP", "2023-05-04 12:05:00.0", "ACTIVE", "3882.0", "Agape CHP"));
    validateRow(
        response,
        68,
        List.of("", "", "Agape CHP", "2023-09-09 12:05:00.0", "ACTIVE", "3864.0", "Agape CHP"));
    validateRow(
        response,
        69,
        List.of("", "", "Agape CHP", "2022-05-04 12:05:00.0", "ACTIVE", "2631.0", "Agape CHP"));
    validateRow(
        response,
        70,
        List.of("", "", "Agape CHP", "2022-02-02 12:05:00.0", "ACTIVE", "2804.0", "Agape CHP"));
    validateRow(
        response,
        71,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2023-02-09 12:05:00.0",
            "ACTIVE",
            "3937.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        72,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2022-11-08 12:05:00.0",
            "ACTIVE",
            "2970.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        73,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2023-04-20 12:05:00.0",
            "ACTIVE",
            "3244.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        74,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2022-11-19 12:05:00.0",
            "ACTIVE",
            "3921.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        75,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2023-08-05 12:05:00.0",
            "ACTIVE",
            "2941.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        76,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2023-10-10 12:05:00.0",
            "ACTIVE",
            "2572.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        77,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2022-01-06 12:05:00.0",
            "ACTIVE",
            "3271.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        78,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2022-05-19 12:05:00.0",
            "ACTIVE",
            "3284.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        79,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2022-01-09 12:05:00.0",
            "ACTIVE",
            "3910.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        80,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2022-10-14 12:05:00.0",
            "ACTIVE",
            "2632.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        81,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2022-08-20 12:05:00.0",
            "ACTIVE",
            "3703.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        82,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2023-07-01 12:05:00.0",
            "ACTIVE",
            "2731.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        83,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2023-11-07 12:05:00.0",
            "ACTIVE",
            "3908.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        84,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2022-11-18 12:05:00.0",
            "ACTIVE",
            "3853.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        85,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2022-04-16 12:05:00.0",
            "ACTIVE",
            "2934.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        86,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2022-12-24 12:05:00.0",
            "ACTIVE",
            "2972.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        87,
        List.of(
            "",
            "",
            "Ahamadyya Mission Cl",
            "2022-11-24 12:05:00.0",
            "ACTIVE",
            "2536.0",
            "Ahamadyya Mission Cl"));
    validateRow(
        response,
        88,
        List.of(
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2022-03-09 12:05:00.0",
            "ACTIVE",
            "3817.0",
            "Ahmadiyya Muslim Hospital"));
    validateRow(
        response,
        89,
        List.of(
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2022-10-26 12:05:00.0",
            "ACTIVE",
            "3598.0",
            "Ahmadiyya Muslim Hospital"));
    validateRow(
        response,
        90,
        List.of(
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2022-09-16 12:05:00.0",
            "ACTIVE",
            "3889.0",
            "Ahmadiyya Muslim Hospital"));
    validateRow(
        response,
        91,
        List.of(
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2023-05-06 12:05:00.0",
            "ACTIVE",
            "3437.0",
            "Ahmadiyya Muslim Hospital"));
    validateRow(
        response,
        92,
        List.of(
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2022-04-21 12:05:00.0",
            "ACTIVE",
            "3943.0",
            "Ahmadiyya Muslim Hospital"));
    validateRow(
        response,
        93,
        List.of(
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2022-01-19 12:05:00.0",
            "ACTIVE",
            "2563.0",
            "Ahmadiyya Muslim Hospital"));
    validateRow(
        response,
        94,
        List.of(
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2022-07-06 12:05:00.0",
            "ACTIVE",
            "2625.0",
            "Ahmadiyya Muslim Hospital"));
    validateRow(
        response,
        95,
        List.of(
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2022-07-07 12:05:00.0",
            "ACTIVE",
            "3221.0",
            "Ahmadiyya Muslim Hospital"));
    validateRow(
        response,
        96,
        List.of(
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2022-01-03 12:05:00.0",
            "ACTIVE",
            "3545.0",
            "Ahmadiyya Muslim Hospital"));
    validateRow(
        response,
        97,
        List.of(
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2023-08-12 12:05:00.0",
            "ACTIVE",
            "3894.0",
            "Ahmadiyya Muslim Hospital"));
    validateRow(
        response,
        98,
        List.of(
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2022-02-13 12:05:00.0",
            "ACTIVE",
            "3618.0",
            "Ahmadiyya Muslim Hospital"));
    validateRow(
        response,
        99,
        List.of(
            "",
            "",
            "Ahmadiyya Muslim Hospital",
            "2022-03-26 12:05:00.0",
            "ACTIVE",
            "2506.0",
            "Ahmadiyya Muslim Hospital"));
  }

  @Test
  public void sortByEventDateDesc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add(
                "headers=ouname,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,IpHINAT79UW.A03MvHHogjR.occurreddate")
            .add("IpHINAT79UW.A03MvHHogjR.occurreddate=LAST_YEAR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("desc=IpHINAT79UW.A03MvHHogjR.occurreddate")
            .add("relativePeriodDate=2018-01-28");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(7)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(7))
        .body("headerWidth", equalTo(7));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"rBvjJYbMCVx\":{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\",\"name\":\"Male\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.occurreddate\":{\"name\":\"Report date\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\"},{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "gHGyrwKPzej", "Birth date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 2, "ciq2USN94oJ", "Civil status", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        4,
        "IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x",
        "MCH BCG dose, Child Programme, Birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        5,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        6,
        "IpHINAT79UW.A03MvHHogjR.occurreddate",
        "Report date, Child Programme, Birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of("Makeni-Lol MCHP", "", "", "Male", "0", "1.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        1,
        List.of("Helegombu MCHP", "", "", "Female", "0", "0.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        2,
        List.of("Mamanso Sanka CHP", "", "", "Male", "0", "0.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response, 3, List.of("Bumpeh CHP", "", "", "Male", "0", "0.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response, 4, List.of("Bayama MCHP", "", "", "Male", "0", "0.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response, 5, List.of("Yalieboya CHP", "", "", "Male", "1", "0.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        6,
        List.of("Serabu (Small Bo) CHP", "", "", "Female", "0", "2.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        7,
        List.of("Musaia (Koinadugu) CHC", "", "", "Female", "1", "2.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        8,
        List.of("Ngelehun CHC", "", "", "Female", "1", "2.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        9,
        List.of("Barmoi Luma MCHP", "", "", "Female", "0", "0.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response, 10, List.of("Kasse MCHP", "", "", "Female", "1", "1.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        11,
        List.of("Govt. Hospital", "", "", "Female", "0", "1.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response, 12, List.of("Dogoloya CHP", "", "", "Male", "0", "0.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        13,
        List.of(
            "Lungi Govt. Hospital, Port Loko",
            "",
            "",
            "Male",
            "1",
            "0.0",
            "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        14,
        List.of("Bumbanday MCHP", "", "", "Male", "0", "1.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response, 15, List.of("Deima MCHP", "", "", "Female", "1", "0.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response, 16, List.of("Telu CHP", "", "", "Female", "1", "1.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        17,
        List.of("Junctionla MCHP", "", "", "Female", "0", "0.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        18,
        List.of("Senehun CHC", "", "", "Female", "0", "2.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        19,
        List.of("St. Luke's Wellington", "", "", "Male", "1", "0.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        20,
        List.of("Wilberforce CHC", "", "", "Female", "1", "0.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        21,
        List.of("Waterloo CHC", "", "", "Female", "0", "0.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response, 22, List.of("Baiwala CHP", "", "", "Male", "0", "1.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        23,
        List.of("New Police Barracks CHC", "", "", "Female", "1", "2.0", "2022-12-29 00:00:00.0"));
    validateRow(
        response,
        24,
        List.of("Masongbo Limba MCHP", "", "", "Female", "1", "0.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        25,
        List.of("Wilberforce MCHP", "", "", "Male", "1", "2.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        26,
        List.of("Mokaiyegbeh MCHP", "", "", "Male", "0", "0.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response, 27, List.of("Gbanti CHC", "", "", "Female", "0", "1.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        28,
        List.of("Gerehun CHC", "", "", "Female", "0", "0.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        29,
        List.of(
            "Nyandehun (Mano Sakrim) MCHP", "", "", "Male", "1", "2.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        30,
        List.of("Kamagbewu MCHP", "", "", "Male", "1", "1.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        31,
        List.of("Njandama MCHP", "", "", "Female", "1", "0.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response, 32, List.of("Waterloo CHC", "", "", "Male", "0", "1.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response, 33, List.of("Yoni CHC", "", "", "Male", "1", "2.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        34,
        List.of("Gondama (Tikonko) CHC", "", "", "Female", "0", "2.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        35,
        List.of("Maronko MCHP", "", "", "Female", "0", "0.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        36,
        List.of("Gbonkobana CHP", "", "", "Female", "0", "1.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        37,
        List.of("Ross Road Health Centre", "", "", "Female", "0", "1.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        38,
        List.of("Fogbo (WAR) MCHP", "", "", "Male", "0", "2.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response, 39, List.of("Dodo CHC", "", "", "Male", "1", "1.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response, 40, List.of("Mbokie CHP", "", "", "Female", "1", "0.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        41,
        List.of("Rorocks CHP", "", "", "Female", "1", "0.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response, 42, List.of("Newton CHC", "", "", "Male", "0", "0.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response, 43, List.of("Rokai CHP", "", "", "Female", "0", "1.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        44,
        List.of("Calaba town CHC", "", "", "Male", "1", "0.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response, 45, List.of("Falaba MCHP", "", "", "Male", "0", "2.0", "2022-12-28 00:00:00.0"));
    validateRow(
        response,
        46,
        List.of("Makolor CHP", "", "", "Female", "0", "0.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        47,
        List.of("Koakoyima CHC", "", "", "Female", "0", "1.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response, 48, List.of("Dawa MCHP", "", "", "Male", "1", "1.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        49,
        List.of("Govt. Hosp. Makeni", "", "", "Female", "0", "0.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        50,
        List.of(
            "Nyandehun (Mano Sakrim) MCHP", "", "", "Female", "0", "0.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response, 51, List.of("Jormu MCHP", "", "", "Female", "1", "0.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response, 52, List.of("Kumala CHP", "", "", "Male", "1", "0.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        53,
        List.of("Menika MCHP", "", "", "Female", "1", "2.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        54,
        List.of("Magbethy MCHP", "", "", "Male", "1", "0.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response, 55, List.of("Arab Clinic", "", "", "Male", "1", "1.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        56,
        List.of(
            "Mabenteh Community Hospital", "", "", "Female", "0", "2.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response, 57, List.of("Gbongeh CHP", "", "", "Male", "0", "1.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response, 58, List.of("Iscon CHP", "", "", "Male", "1", "0.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        59,
        List.of(
            "Kingtom Police Hospital (MI Room)",
            "",
            "",
            "Male",
            "0",
            "1.0",
            "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        60,
        List.of("Yarawadu MCHP", "", "", "Male", "1", "2.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        61,
        List.of("Mattru UBC Hospital", "", "", "Male", "1", "0.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        62,
        List.of("Kayongoro MCHP", "", "", "Female", "1", "2.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        63,
        List.of("Mamankie MCHP", "", "", "Male", "1", "2.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        64,
        List.of("Kamadu Sokuralla MCHP", "", "", "Female", "0", "2.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response,
        65,
        List.of("Kaniya MCHP", "", "", "Female", "0", "2.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response, 66, List.of("Venima CHP", "", "", "Male", "0", "1.0", "2022-12-27 00:00:00.0"));
    validateRow(
        response, 67, List.of("Senthai MCHP", "", "", "Male", "0", "1.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response,
        68,
        List.of("Baoma Station CHP", "", "", "Male", "0", "0.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response,
        69,
        List.of("Koinadugu II CHP", "", "", "Male", "0", "0.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response, 70, List.of("Mongere CHC", "", "", "Male", "1", "0.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response,
        71,
        List.of("Konta-Line MCHP", "", "", "Male", "1", "2.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response, 72, List.of("Tokeh MCHP", "", "", "Male", "1", "2.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response,
        73,
        List.of("Bunumbu CHP", "", "", "Female", "1", "0.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response, 74, List.of("Yamandu CHC", "", "", "Male", "1", "0.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response,
        75,
        List.of("Tissana CHC", "", "", "Female", "0", "2.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response,
        76,
        List.of("Gbangba MCHP", "", "", "Female", "0", "0.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response, 77, List.of("York CHC", "", "", "Male", "1", "2.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response,
        78,
        List.of("Njagbahun MCHP", "", "", "Female", "1", "2.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response,
        79,
        List.of("Madina Wesleyan Mission", "", "", "Female", "1", "1.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response,
        80,
        List.of("Gbogbodo MCHP", "", "", "Female", "1", "1.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response, 81, List.of("Burma 2 MCHP", "", "", "Male", "0", "0.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response, 82, List.of("Kanikay MCHP", "", "", "Male", "0", "2.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response,
        83,
        List.of("Blessed Mokaba East", "", "", "Male", "1", "1.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response,
        84,
        List.of("Kemedugu MCHP", "", "", "Male", "0", "2.0", "2022-12-26 00:00:00.0"));
    validateRow(
        response, 85, List.of("Topan CHP", "", "", "Male", "0", "0.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response,
        86,
        List.of("Ola During Clinic", "", "", "Female", "1", "0.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response,
        87,
        List.of("Kambama CHP", "", "", "Female", "0", "0.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response,
        88,
        List.of("Mano Njeigbla CHP", "", "", "Male", "0", "0.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response,
        89,
        List.of("Bongor MCHP", "", "", "Female", "1", "0.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response,
        90,
        List.of("Ngogbebu MCHP", "", "", "Female", "1", "1.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response, 91, List.of("Yele CHC", "", "", "Male", "1", "2.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response,
        92,
        List.of(
            "Mabenteh Community Hospital", "", "", "Male", "1", "0.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response, 93, List.of("Firawa CHC", "", "", "Male", "0", "1.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response,
        94,
        List.of("Serabu (Bumpe Ngao) UFC", "", "", "Male", "1", "1.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response, 95, List.of("Baama CHC", "", "", "Female", "1", "2.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response, 96, List.of("Alkalia CHP", "", "", "Male", "0", "0.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response,
        97,
        List.of("Petifu Fulamasa MCHP", "", "", "Male", "1", "1.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response, 98, List.of("Gao MCHP", "", "", "Male", "1", "0.0", "2022-12-25 00:00:00.0"));
    validateRow(
        response,
        99,
        List.of("George Brook Health Centre", "", "", "Male", "0", "1.0", "2022-12-25 00:00:00.0"));
  }

  @Test
  public void sortByEventDateWithOffsetsDesc() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add(
                "headers=ouname,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,IpHINAT79UW.A03MvHHogjR.occurreddate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("IpHINAT79UW[-1].A03MvHHogjR[-1].occurreddate=LAST_YEAR")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("desc=IpHINAT79UW[-1].A03MvHHogjR[-1].occurreddate")
            .add("relativePeriodDate=2018-01-28");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(7)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(7))
        .body("headerWidth", equalTo(7));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"rBvjJYbMCVx\":{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\",\"name\":\"Male\"},\"bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ciq2USN94oJ\":{\"uid\":\"ciq2USN94oJ\",\"code\":\"MMD_PER_STA\",\"name\":\"Civil status\",\"description\":\"Civil status\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"gHGyrwKPzej\":{\"uid\":\"gHGyrwKPzej\",\"code\":\"MMD_PER_DOB\",\"name\":\"Birth date\",\"description\":\"Birth date\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.occurreddate\":{\"name\":\"Report date\",\"dimensionType\":\"PERIOD\"},\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"uid\":\"bx6fsa0t90x\",\"code\":\"DE_2006101\",\"name\":\"MCH BCG dose\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\"},{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "gHGyrwKPzej", "Birth date", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 2, "ciq2USN94oJ", "Civil status", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        4,
        "IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x",
        "MCH BCG dose, Child Programme, Birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        5,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        6,
        "IpHINAT79UW.A03MvHHogjR.occurreddate",
        "Report date, Child Programme, Birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Konabu MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 1, List.of("Conakry Dee CHC", "", "", "Male", "", "", ""));
    validateRow(response, 2, List.of("Maforay (B. Sebora) MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 3, List.of("Hill Station MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 4, List.of("Dankawalie MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 5, List.of("Robat MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 6, List.of("Kpolies Clinic", "", "", "Male", "", "", ""));
    validateRow(response, 7, List.of("Maboni MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 8, List.of("Masongbo Limba MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 9, List.of("Kamagbewu MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 10, List.of("Mapailleh MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 11, List.of("KingHarman Rd. Hospital", "", "", "Male", "", "", ""));
    validateRow(response, 12, List.of("Bradford CHC", "", "", "Female", "", "", ""));
    validateRow(response, 13, List.of("UMC Clinic Taiama", "", "", "Male", "", "", ""));
    validateRow(response, 14, List.of("Ngelehun CHC", "", "", "Female", "", "", ""));
    validateRow(response, 15, List.of("Mamboma MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 16, List.of("Melekuray CHC", "", "", "Male", "", "", ""));
    validateRow(
        response, 17, List.of("St. John of God Catholic Clinic", "", "", "Male", "", "", ""));
    validateRow(response, 18, List.of("Kondeya (Sandor) MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 19, List.of("UFC Port Loko", "", "", "Male", "", "", ""));
    validateRow(response, 20, List.of("Bumban MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 21, List.of("Mogbwemo CHP", "", "", "Female", "", "", ""));
    validateRow(response, 22, List.of("Koidu Under Five Clinic", "", "", "Female", "", "", ""));
    validateRow(response, 23, List.of("Teko Barracks Clinic", "", "", "Male", "", "", ""));
    validateRow(response, 24, List.of("Njagbwema Fiama CHC", "", "", "Female", "", "", ""));
    validateRow(response, 25, List.of("Kissy Health Centre", "", "", "Female", "", "", ""));
    validateRow(response, 26, List.of("Guala MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 27, List.of("Maronko MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 28, List.of("Manjoro MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 29, List.of("Govt. Hosp. Pujehun", "", "", "Male", "", "", ""));
    validateRow(response, 30, List.of("Ginger Hall Health Centre", "", "", "Male", "", "", ""));
    validateRow(response, 31, List.of("Pendembu CHC", "", "", "Female", "", "", ""));
    validateRow(response, 32, List.of("Karlu CHC", "", "", "Male", "", "", ""));
    validateRow(response, 33, List.of("Yamandu CHC", "", "", "Male", "", "", ""));
    validateRow(response, 34, List.of("Mokelleh CHC", "", "", "Female", "", "", ""));
    validateRow(response, 35, List.of("Marie Stopes (Kakua) Clinic", "", "", "Female", "", "", ""));
    validateRow(response, 36, List.of("Ngolahun CHC", "", "", "Male", "", "", ""));
    validateRow(response, 37, List.of("Talia CHC", "", "", "Male", "", "", ""));
    validateRow(response, 38, List.of("Agape CHP", "", "", "Female", "", "", ""));
    validateRow(response, 39, List.of("Mawoma MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 40, List.of("Futa CHC", "", "", "Male", "", "", ""));
    validateRow(response, 41, List.of("Robarie MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 42, List.of("Masuba MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 43, List.of("Gbondapi CHC", "", "", "Male", "", "", ""));
    validateRow(response, 44, List.of("Fanima CHP", "", "", "Female", "", "", ""));
    validateRow(response, 45, List.of("Koribondo CHC", "", "", "Male", "", "", ""));
    validateRow(response, 46, List.of("Sembehun (Gaura) MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 47, List.of("Mokoba MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 48, List.of("Kissy Health Centre", "", "", "Female", "", "", ""));
    validateRow(response, 49, List.of("Makonthanday MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 50, List.of("Tugbebu CHP", "", "", "Female", "", "", ""));
    validateRow(response, 51, List.of("Benkeh MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 52, List.of("Grafton MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 53, List.of("Kpandebu CHP", "", "", "Female", "", "", ""));
    validateRow(response, 54, List.of("Yankasa MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 55, List.of("Kalainkay MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 56, List.of("Royeiben MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 57, List.of("Kpetema (Lower Bambara) CHP", "", "", "Male", "", "", ""));
    validateRow(response, 58, List.of("Gondama (Kamaje) CHP", "", "", "Female", "", "", ""));
    validateRow(response, 59, List.of("Kamasaypana MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 60, List.of("Foredugu MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 61, List.of("Mangay Loko MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 62, List.of("Yiraia CHP", "", "", "Male", "", "", ""));
    validateRow(response, 63, List.of("Katongha MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 64, List.of("Royeama CHP", "", "", "Female", "", "", ""));
    validateRow(response, 65, List.of("Nduvuibu MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 66, List.of("Agape CHP", "", "", "Male", "", "", ""));
    validateRow(response, 67, List.of("Fotaneh Junction MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 68, List.of("Bendu Mameima CHC", "", "", "Male", "", "", ""));
    validateRow(response, 69, List.of("Madina (Malema) MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 70, List.of("Njala CHP", "", "", "Female", "", "", ""));
    validateRow(response, 71, List.of("Kania (Masungbala) MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 72, List.of("Liya MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 73, List.of("Kuranko MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 74, List.of("Lango Town MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 75, List.of("Gbaa (Makpele) CHP", "", "", "Male", "", "", ""));
    validateRow(response, 76, List.of("Baptist Centre Kassirie", "", "", "Female", "", "", ""));
    validateRow(response, 77, List.of("Dia CHP", "", "", "Male", "", "", ""));
    validateRow(response, 78, List.of("Dogoloya CHP", "", "", "Male", "", "", ""));
    validateRow(response, 79, List.of("Masiaka CHC", "", "", "Female", "", "", ""));
    validateRow(response, 80, List.of("Koindu CHC", "", "", "Male", "", "", ""));
    validateRow(response, 81, List.of("Tombo CHC", "", "", "Male", "", "", ""));
    validateRow(response, 82, List.of("Pejewa MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 83, List.of("Malal MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 84, List.of("Njagbahun (Fakunya) MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 85, List.of("Lowoma MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 86, List.of("Manjeihun MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 87, List.of("MacDonald MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 88, List.of("Macoth MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 89, List.of("Madina Fullah CHP", "", "", "Male", "", "", ""));
    validateRow(response, 90, List.of("Yonibana MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 91, List.of("Dulukoro MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 92, List.of("Maami CHP", "", "", "Male", "", "", ""));
    validateRow(response, 93, List.of("Koribondo CHC", "", "", "Female", "", "", ""));
    validateRow(response, 94, List.of("Batkanu CHC", "", "", "Male", "", "", ""));
    validateRow(response, 95, List.of("Durukoro MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 96, List.of("Vaahun MCHP", "", "", "Female", "", "", ""));
    validateRow(response, 97, List.of("Wallehun MCHP", "", "", "Male", "", "", ""));
    validateRow(response, 98, List.of("Masorie CHP", "", "", "Female", "", "", ""));
    validateRow(response, 99, List.of("Waiima (Kori) MCHP", "", "", "Male", "", "", ""));
  }

  @Test
  public void sortByEnrollmentOuAscMultiProgram() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=ur1Edk5Oe2n.enrollmentdate")
            .add(
                "headers=ouname,cejWyOfXge6,w75KJ2mc4zz,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,IpHINAT79UW.ouname,ur1Edk5Oe2n.EPEcjy3FWmI.fTZFU8cWvb3,ur1Edk5Oe2n.enrollmentdate")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=ur1Edk5Oe2n.LAST_12_MONTHS,ur1Edk5Oe2n.LAST_5_YEARS")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,cejWyOfXge6,w75KJ2mc4zz,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,ur1Edk5Oe2n.EPEcjy3FWmI.fTZFU8cWvb3")
            .add("relativePeriodDate=2022-01-27");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(7)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(7))
        .body("headerWidth", equalTo(7));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"fTZFU8cWvb3\":{\"uid\":\"fTZFU8cWvb3\",\"code\":\"DE_860002\",\"name\":\"TB lab Hemoglobin\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"202109\":{\"uid\":\"202109\",\"code\":\"202109\",\"name\":\"September 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-09-01T00:00:00.000\",\"endDate\":\"2021-09-30T00:00:00.000\"},\"202107\":{\"uid\":\"202107\",\"code\":\"202107\",\"name\":\"July 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-07-01T00:00:00.000\",\"endDate\":\"2021-07-31T00:00:00.000\"},\"202108\":{\"uid\":\"202108\",\"code\":\"202108\",\"name\":\"August 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-08-01T00:00:00.000\",\"endDate\":\"2021-08-31T00:00:00.000\"},\"202105\":{\"uid\":\"202105\",\"code\":\"202105\",\"name\":\"May 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-05-01T00:00:00.000\",\"endDate\":\"2021-05-31T00:00:00.000\"},\"202106\":{\"uid\":\"202106\",\"code\":\"202106\",\"name\":\"June 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-06-01T00:00:00.000\",\"endDate\":\"2021-06-30T00:00:00.000\"},\"202103\":{\"uid\":\"202103\",\"code\":\"202103\",\"name\":\"March 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-03-01T00:00:00.000\",\"endDate\":\"2021-03-31T00:00:00.000\"},\"202104\":{\"uid\":\"202104\",\"code\":\"202104\",\"name\":\"April 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-04-01T00:00:00.000\",\"endDate\":\"2021-04-30T00:00:00.000\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"IpHINAT79UW.ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"202112\":{\"uid\":\"202112\",\"code\":\"202112\",\"name\":\"December 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-12-01T00:00:00.000\",\"endDate\":\"2021-12-31T00:00:00.000\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"202110\":{\"uid\":\"202110\",\"code\":\"202110\",\"name\":\"October 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-10-01T00:00:00.000\",\"endDate\":\"2021-10-31T00:00:00.000\"},\"202111\":{\"uid\":\"202111\",\"code\":\"202111\",\"name\":\"November 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-11-01T00:00:00.000\",\"endDate\":\"2021-11-30T00:00:00.000\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\"},{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"w75KJ2mc4zz\":{\"uid\":\"w75KJ2mc4zz\",\"code\":\"MMD_PER_NAM\",\"name\":\"First name\",\"description\":\"First name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ur1Edk5Oe2n.pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"2021\":{\"uid\":\"2021\",\"code\":\"2021\",\"name\":\"2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-01-01T00:00:00.000\",\"endDate\":\"2021-12-31T00:00:00.000\"},\"2020\":{\"uid\":\"2020\",\"code\":\"2020\",\"name\":\"2020\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2020-01-01T00:00:00.000\",\"endDate\":\"2020-12-31T00:00:00.000\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"202101\":{\"uid\":\"202101\",\"code\":\"202101\",\"name\":\"January 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-01-01T00:00:00.000\",\"endDate\":\"2021-01-31T00:00:00.000\"},\"202102\":{\"uid\":\"202102\",\"code\":\"202102\",\"name\":\"February 2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-02-01T00:00:00.000\",\"endDate\":\"2021-02-28T00:00:00.000\"},\"2019\":{\"uid\":\"2019\",\"code\":\"2019\",\"name\":\"2019\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2019-01-01T00:00:00.000\",\"endDate\":\"2019-12-31T00:00:00.000\"},\"ur1Edk5Oe2n.enrollmentdate\":{\"name\":\"Start of treatment date\",\"dimensionType\":\"PERIOD\"},\"2018\":{\"uid\":\"2018\",\"code\":\"2018\",\"name\":\"2018\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2018-01-01T00:00:00.000\",\"endDate\":\"2018-12-31T00:00:00.000\"},\"2017\":{\"uid\":\"2017\",\"code\":\"2017\",\"name\":\"2017\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2017-01-01T00:00:00.000\",\"endDate\":\"2017-12-31T00:00:00.000\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"ur1Edk5Oe2n.EPEcjy3FWmI.fTZFU8cWvb3\":{\"uid\":\"fTZFU8cWvb3\",\"code\":\"DE_860002\",\"name\":\"TB lab Hemoglobin\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"fTZFU8cWvb3\":[],\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"a3kGcGDCuk6\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[\"2017\",\"2018\",\"2019\",\"2020\",\"2021\"],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        3,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        4,
        "IpHINAT79UW.ouname",
        "Organisation Unit Name, Child Programme",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        5,
        "ur1Edk5Oe2n.EPEcjy3FWmI.fTZFU8cWvb3",
        "TB lab Hemoglobin, TB program, Lab monitoring",
        "TRUE_ONLY",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        6,
        "ur1Edk5Oe2n.enrollmentdate",
        "Start of treatment date, TB program",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of("Bai Largo MCHP", "Female", "Furuta", "", "", "", "2021-02-22 12:28:59.5"));
    validateRow(
        response,
        1,
        List.of("Konjo CHP", "Female", "Sesuna", "", "", "", "2021-02-22 12:29:10.226"));
    validateRow(
        response,
        2,
        List.of("Mokpende MCHP", "Female", "Simret", "", "", "", "2021-02-22 12:29:17.878"));
    validateRow(
        response,
        3,
        List.of("Konta Wallah MCHP", "Female", "Sarama", "", "", "", "2021-02-22 12:30:18.762"));
    validateRow(
        response,
        4,
        List.of("Magbaft MCHP", "Female", "Makda", "", "", "", "2021-02-22 12:36:38.143"));
    validateRow(
        response,
        5,
        List.of("Masoko MCHP", "Female", "Miniya", "", "", "", "2021-02-22 12:37:38.153"));
    validateRow(
        response,
        6,
        List.of("Mamalikie MCHP", "Male", "Iggi", "", "", "", "2021-02-22 12:37:43.562"));
    validateRow(
        response,
        7,
        List.of("Ninkikoro MCHP", "Male", "Medhanie", "", "", "", "2021-02-22 12:37:59.06"));
    validateRow(
        response,
        8,
        List.of("Seidu MCHP", "Male", "Daniel", "", "", "", "2021-02-22 12:38:16.207"));
    validateRow(
        response,
        9,
        List.of("Bangoma MCHP", "Female", "Sarama", "", "", "", "2021-02-22 12:42:36.654"));
    validateRow(
        response,
        10,
        List.of("Blamawo MCHP", "Male", "Abdullah", "", "", "", "2021-02-23 12:28:45.299"));
    validateRow(
        response,
        11,
        List.of(
            "Benguima Grassfield MCHP", "Female", "Almaz", "", "", "", "2021-02-23 12:32:07.441"));
    validateRow(
        response,
        12,
        List.of("Degbuama MCHP", "Male", "Amanuel", "", "", "", "2021-02-23 12:34:11.197"));
    validateRow(
        response,
        13,
        List.of("Suen CHP", "Female", "Mezan", "", "", "", "2021-02-23 12:40:28.099"));
    validateRow(
        response,
        14,
        List.of("Petifu CHC", "Male", "Medhane", "", "", "", "2021-02-23 12:42:51.379"));
    validateRow(
        response,
        15,
        List.of("Kpowubu MCHP", "Female", "Furuta", "", "", "", "2021-02-24 12:28:13.16"));
    validateRow(
        response,
        16,
        List.of("Massahun MCHP", "Female", "Luwam", "", "", "", "2021-02-24 12:29:44.902"));
    validateRow(
        response,
        17,
        List.of("Kawula CHP", "Male", "Kifle", "", "", "", "2021-02-24 12:30:06.041"));
    validateRow(
        response,
        18,
        List.of("Kamiendor MCHP", "Male", "Demsas", "", "", "", "2021-02-24 12:33:12.117"));
    validateRow(
        response,
        19,
        List.of("Mapaki CHC", "Female", "Demet", "", "", "", "2021-02-24 12:33:15.016"));
    validateRow(
        response,
        20,
        List.of("Makali CHC", "Male", "Yemane", "", "", "", "2021-02-24 12:39:47.712"));
    validateRow(
        response,
        21,
        List.of("Koindukura MCHP", "Male", "Brhane", "", "", "", "2021-02-24 12:40:06.942"));
    validateRow(
        response,
        22,
        List.of("Kamabai CHC", "Female", "Selamawit", "", "", "", "2021-02-25 12:28:25.771"));
    validateRow(
        response,
        23,
        List.of("Mafufuneh MCHP", "Male", "Massawa", "", "", "", "2021-02-25 12:29:02.87"));
    validateRow(
        response,
        24,
        List.of("St. Mary's Clinic", "Female", "Asmeret", "", "", "", "2021-02-25 12:29:39.291"));
    validateRow(
        response,
        25,
        List.of("Bomu Saamba CHP", "Female", "Yorda", "", "", "", "2021-02-25 12:29:52.461"));
    validateRow(
        response,
        26,
        List.of("Geoma Jagor CHC", "Female", "Bisirat", "", "", "", "2021-02-25 12:30:46.251"));
    validateRow(
        response,
        27,
        List.of("Makona MCHP", "Male", "Sebhat", "", "", "", "2021-02-25 12:31:45.792"));
    validateRow(
        response,
        28,
        List.of("Bo Govt. Hosp.", "Male", "Tewolde", "", "", "", "2021-02-25 12:33:20.889"));
    validateRow(
        response,
        29,
        List.of("Fairo CHC", "Male", "Petros", "", "", "", "2021-02-25 12:37:04.983"));
    validateRow(
        response,
        30,
        List.of("Leprosy & TB Hospital", "Female", "Lete", "", "", "", "2021-02-25 12:38:01.026"));
    validateRow(
        response,
        31,
        List.of("Bangambaya MCHP", "Female", "Hiewan", "", "", "", "2021-02-25 12:38:09.338"));
    validateRow(
        response,
        32,
        List.of("Bath Bana MCHP", "Female", "Hiwet", "", "", "", "2021-02-25 12:38:17.317"));
    validateRow(
        response,
        33,
        List.of("Kpayama 1 MCHP", "Female", "Hiwet", "", "", "", "2021-02-25 12:39:22.294"));
    validateRow(
        response,
        34,
        List.of("Lumley Hospital", "Female", "Martha", "", "", "", "2021-02-25 12:39:46.595"));
    validateRow(
        response,
        35,
        List.of("Komrabai Station MCHP", "Female", "Abeba", "", "", "", "2021-02-25 12:39:51.606"));
    validateRow(
        response,
        36,
        List.of("Blessed Mokaba East", "Male", "Welde", "", "", "", "2021-02-25 12:42:07.582"));
    validateRow(
        response,
        37,
        List.of("Dia CHP", "Female", "Mebrat", "", "", "", "2021-02-26 12:28:37.765"));
    validateRow(
        response,
        38,
        List.of("Kono Bendu CHP", "Male", "Yusef", "", "", "", "2021-02-26 12:29:03.809"));
    validateRow(
        response,
        39,
        List.of("Manjama UMC CHC", "Female", "Jamila", "", "", "", "2021-02-26 12:29:11.379"));
    validateRow(
        response, 40, List.of("Gao MCHP", "Male", "Sayid", "", "", "", "2021-02-26 12:30:45.641"));
    validateRow(
        response,
        41,
        List.of("Motorbong MCHP", "Female", "Milen", "", "", "", "2021-02-26 12:30:58.237"));
    validateRow(
        response,
        42,
        List.of("Masankoro MCHP", "Male", "Ermias", "", "", "", "2021-02-26 12:31:21.125"));
    validateRow(
        response,
        43,
        List.of("Madina Fullah CHP", "Male", "Luwam", "", "", "", "2021-02-26 12:32:37.3"));
    validateRow(
        response,
        44,
        List.of("Juba M I Room", "Female", "Senait", "", "", "", "2021-02-26 12:32:46.604"));
    validateRow(
        response,
        45,
        List.of("Mabonkanie MCHP", "Female", "Liya", "", "", "", "2021-02-26 12:32:53.865"));
    validateRow(
        response,
        46,
        List.of(
            "Mosenessie Junction MCHP", "Male", "Abaalom", "", "", "", "2021-02-26 12:34:53.977"));
    validateRow(
        response,
        47,
        List.of("Kurubonla CHC", "Female", "Hagosa", "", "", "", "2021-02-26 12:34:58.611"));
    validateRow(
        response,
        48,
        List.of("Mabang MCHP", "Male", "Abraham", "", "", "", "2021-02-26 12:36:01.248"));
    validateRow(
        response,
        49,
        List.of("Junctionla MCHP", "Female", "Saba", "", "", "", "2021-02-26 12:36:57.393"));
    validateRow(
        response,
        50,
        List.of("Nonkoba CHP", "Female", "Aida", "", "", "", "2021-02-26 12:37:07.182"));
    validateRow(
        response,
        51,
        List.of(
            "Sembehun (Gaura) MCHP", "Female", "Milena", "", "", "", "2021-02-26 12:37:19.027"));
    validateRow(
        response,
        52,
        List.of("Degbuama MCHP", "Male", "Mehari", "", "", "", "2021-02-26 12:37:27.814"));
    validateRow(
        response,
        53,
        List.of("Gbonkobana CHP", "Male", "Brhane", "", "", "", "2021-02-26 12:37:31.099"));
    validateRow(
        response,
        54,
        List.of("Moyeamoh CHP", "Male", "Tesfalem", "", "", "", "2021-02-26 12:38:51.547"));
    validateRow(
        response,
        55,
        List.of("Sussex MCHP", "Female", "Luam", "", "", "", "2021-02-26 12:39:36.076"));
    validateRow(
        response,
        56,
        List.of("Makarie MCHP", "Male", "Haylom", "", "", "", "2021-02-26 12:40:16.731"));
    validateRow(
        response,
        57,
        List.of("Mid Land MCHP", "Female", "Elisa", "", "", "", "2021-02-26 12:43:28.39"));
    validateRow(
        response,
        58,
        List.of("Benduma MCHP", "Female", "Elisa", "", "", "", "2021-02-27 12:27:59.555"));
    validateRow(
        response,
        59,
        List.of("Gbonkonka CHP", "Female", "Nebyat", "", "", "", "2021-02-27 12:29:50.898"));
    validateRow(
        response,
        60,
        List.of("SLC. RHC Port Loko", "Male", "Brhane", "", "", "", "2021-02-27 12:31:01.122"));
    validateRow(
        response,
        61,
        List.of("Makoba Bana MCHP", "Female", "Dehab", "", "", "", "2021-02-27 12:33:53.97"));
    validateRow(
        response,
        62,
        List.of("Kareneh MCHP", "Male", "Nuguse", "", "", "", "2021-02-27 12:34:21.34"));
    validateRow(
        response,
        63,
        List.of("Mansadu MCHP", "Female", "Bisrat", "", "", "", "2021-02-27 12:34:53.741"));
    validateRow(
        response,
        64,
        List.of(
            "Macauley Satellite Hospital", "Male", "Abel", "", "", "", "2021-02-27 12:35:12.582"));
    validateRow(
        response,
        65,
        List.of("Mabom CHP", "Female", "Luam", "", "", "", "2021-02-27 12:35:32.277"));
    validateRow(
        response,
        66,
        List.of("Mandema CHP", "Male", "Efrem", "", "", "", "2021-02-27 12:35:43.526"));
    validateRow(
        response,
        67,
        List.of("Pendembu Njeigbla MCHP", "Male", "Zula", "", "", "", "2021-02-27 12:36:18.534"));
    validateRow(
        response,
        68,
        List.of("Yakaji MCHP", "Female", "Zewdi", "", "", "", "2021-02-27 12:36:58.222"));
    validateRow(
        response,
        69,
        List.of("Kayima CHC", "Female", "Dehab", "", "", "", "2021-02-27 12:37:07.225"));
    validateRow(
        response,
        70,
        List.of("Romeni MCHP", "Male", "Fethawi", "", "", "", "2021-02-27 12:37:20.321"));
    validateRow(
        response,
        71,
        List.of("Gbentu CHP", "Male", "Filmon", "", "", "", "2021-02-27 12:38:04.695"));
    validateRow(
        response,
        72,
        List.of("Belebu CHP", "Female", "Fethawit", "", "", "", "2021-02-27 12:38:13.871"));
    validateRow(
        response,
        73,
        List.of("Maraka MCHP", "Female", "Semira", "", "", "", "2021-02-27 12:39:17.9"));
    validateRow(
        response,
        74,
        List.of("Gbongongor CHP", "Male", "Kidane", "", "", "", "2021-02-27 12:40:09.653"));
    validateRow(
        response,
        75,
        List.of("Mano Menima CHP", "Male", "Iggi", "", "", "", "2021-02-27 12:40:12.846"));
    validateRow(
        response, 76, List.of("Fairo CHC", "Male", "Senay", "", "", "", "2021-02-27 12:41:11.511"));
    validateRow(
        response,
        77,
        List.of(
            "Musaia (Koinadugu) CHC", "Female", "Zemzem", "", "", "", "2021-02-27 12:41:13.21"));
    validateRow(
        response,
        78,
        List.of("Gbanti CHC", "Male", "Mebrahtu", "", "", "", "2021-02-27 12:41:14.905"));
    validateRow(
        response,
        79,
        List.of("Samamaia MCHP", "Male", "Hagos", "", "", "", "2021-02-27 12:41:26.228"));
    validateRow(
        response,
        80,
        List.of("Kornia Kpindema CHP", "Female", "Aida", "", "", "", "2021-02-27 12:42:18.53"));
    validateRow(
        response,
        81,
        List.of("Gbeika MCHP", "Female", "Mebrat", "", "", "", "2021-02-28 12:28:00.06"));
    validateRow(
        response,
        82,
        List.of("Badala MCHP", "Female", "Milen", "", "", "", "2021-02-28 12:28:09.606"));
    validateRow(
        response,
        83,
        List.of("Kagbasia MCHP", "Female", "Fatimah", "", "", "", "2021-02-28 12:29:12.929"));
    validateRow(
        response,
        84,
        List.of("Bonkababay MCHP", "Female", "Mezan", "", "", "", "2021-02-28 12:29:42.004"));
    validateRow(
        response,
        85,
        List.of("Serabu (Koya) CHP", "Male", "Tewelde", "", "", "", "2021-02-28 12:29:52.696"));
    validateRow(
        response,
        86,
        List.of("Bandawor MCHP", "Male", "Abdullah", "", "", "", "2021-02-28 12:32:14.175"));
    validateRow(
        response,
        87,
        List.of("Sanya CHP", "Female", "Hiwet", "", "", "", "2021-02-28 12:32:25.701"));
    validateRow(
        response,
        88,
        List.of("Benduma MCHP", "Female", "Selamawit", "", "", "", "2021-02-28 12:34:08.109"));
    validateRow(
        response,
        89,
        List.of("Makaiba MCHP", "Female", "Feorie", "", "", "", "2021-02-28 12:36:01.399"));
    validateRow(
        response,
        90,
        List.of("Yonibana MCHP", "Male", "Tesfalem", "", "", "", "2021-02-28 12:36:18.048"));
    validateRow(
        response,
        91,
        List.of("Tugbebu CHP", "Male", "Berhane", "", "", "", "2021-02-28 12:39:13.877"));
    validateRow(
        response,
        92,
        List.of("Foya CHP", "Female", "Meaza", "", "", "", "2021-02-28 12:39:42.834"));
    validateRow(
        response,
        93,
        List.of("Ngegbwema CHC", "Female", "Kisanet", "", "", "", "2021-02-28 12:40:26.277"));
    validateRow(
        response,
        94,
        List.of("Juma MCHP", "Female", "Asmeret", "", "", "", "2021-02-28 12:40:30.62"));
    validateRow(
        response,
        95,
        List.of("Magbele MCHP", "Female", "Natsnet", "", "", "", "2021-02-28 12:40:38.91"));
    validateRow(
        response, 96, List.of("Fogbo CHP", "Male", "Fikru", "", "", "", "2021-02-28 12:41:08.019"));
    validateRow(
        response,
        97,
        List.of("Fadugu CHC", "Male", "Kinfe", "", "", "", "2021-02-28 12:41:40.995"));
    validateRow(
        response,
        98,
        List.of("Sukudu Soa MCHP", "Female", "Saba", "", "", "", "2021-02-28 12:41:52.21"));
    validateRow(
        response,
        99,
        List.of("Sandayeima MCHP", "Female", "Adiam", "", "", "", "2021-02-28 12:42:18.148"));
  }
}
