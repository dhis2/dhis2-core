/*
 * Copyright (c) 2004-2023, University of Oslo
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
public class EnrollmentsQuery1AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void queryRandom1() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add(
                "headers=ouname,spFvx9FndA4,H9IlTX2X6SL,cejWyOfXge6,ruQQnf6rswq,OvY4VVhSDeJ,enrollmentdate,uIuxlbV1vRT,Bpx0589u8y0")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2021")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:ImspTQPwCqd,spFvx9FndA4,H9IlTX2X6SL,cejWyOfXge6:IN:Female,ruQQnf6rswq,OvY4VVhSDeJ,uIuxlbV1vRT:J40PpdN4Wkk,Bpx0589u8y0")
            .add("relativePeriodDate=2023-07-14");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"uIuxlbV1vRT\":{\"uid\":\"uIuxlbV1vRT\",\"name\":\"Area\",\"dimensionType\":\"ORGANISATION_UNIT_GROUP_SET\"},\"2021\":{\"name\":\"2021\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"MAs88nJc9nL\":{\"uid\":\"MAs88nJc9nL\",\"code\":\"Private Clinic\",\"name\":\"Private Clinic\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"ruQQnf6rswq\":{\"uid\":\"ruQQnf6rswq\",\"name\":\"TB number\",\"description\":\"TB number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"J40PpdN4Wkk\":{\"uid\":\"J40PpdN4Wkk\",\"name\":\"Northern Area\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"PVLOW4bCshG\":{\"uid\":\"PVLOW4bCshG\",\"code\":\"NGO\",\"name\":\"NGO\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"oRVt7g429ZO\":{\"uid\":\"oRVt7g429ZO\",\"code\":\"Public facilities\",\"name\":\"Public facilities\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"Bpx0589u8y0\":{\"uid\":\"Bpx0589u8y0\",\"name\":\"Facility Ownership\",\"dimensionType\":\"ORGANISATION_UNIT_GROUP_SET\"},\"w0gFTTmsUcF\":{\"uid\":\"w0gFTTmsUcF\",\"code\":\"Mission\",\"name\":\"Mission\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"spFvx9FndA4\":{\"uid\":\"spFvx9FndA4\",\"name\":\"Age\",\"description\":\"Age\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"H9IlTX2X6SL\":{\"uid\":\"H9IlTX2X6SL\",\"code\":\"Blood type\",\"name\":\"Blood type\",\"description\":\"Blood type\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"OvY4VVhSDeJ\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"uIuxlbV1vRT\":[\"J40PpdN4Wkk\"],\"Bpx0589u8y0\":[\"MAs88nJc9nL\",\"PVLOW4bCshG\",\"w0gFTTmsUcF\",\"oRVt7g429ZO\"],\"spFvx9FndA4\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"Mnp3oXrpAbK\"],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "spFvx9FndA4", "Age", "AGE", "java.util.Date", false, true);
    validateHeader(
        response, 2, "H9IlTX2X6SL", "Blood type", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 4, "ruQQnf6rswq", "TB number", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 5, "OvY4VVhSDeJ", "Weight in kg", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response,
        6,
        "enrollmentdate",
        "Start of treatment date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeader(response, 7, "uIuxlbV1vRT", "Area", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 8, "Bpx0589u8y0", "Facility Ownership", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Makelleh MCHP",
            "",
            "B+",
            "Female",
            "1Z 877 174 06 8974 168 7",
            "94.8",
            "2021-09-05 12:43:46.134",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        1,
        List.of(
            "Maborie MCHP",
            "",
            "B+",
            "Female",
            "1Z 881 9E6 08 6878 110 4",
            "64.7",
            "2021-09-02 12:43:46.15",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        2,
        List.of(
            "Kemedugu MCHP",
            "",
            "O+",
            "Female",
            "1Z 581 532 75 9366 399 0",
            "86.5",
            "2021-04-12 12:27:49.459",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        3,
        List.of(
            "Mayossoh MCHP",
            "",
            "A+",
            "Female",
            "1Z 501 391 59 1103 658 3",
            "96.9",
            "2021-09-09 12:27:50.438",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        4,
        List.of(
            "Maharie MCHP",
            "",
            "A+",
            "Female",
            "1Z 868 032 85 2314 849 4",
            "56.1",
            "2021-08-28 12:27:50.519",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        5,
        List.of(
            "Heremakono MCHP",
            "",
            "A+",
            "Female",
            "1Z 702 455 54 8490 727 4",
            "83.4",
            "2021-05-17 12:27:50.576",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        6,
        List.of(
            "Kondeya (Sandor) MCHP",
            "",
            "A+",
            "Female",
            "1Z 355 W25 56 2039 227 9",
            "72.6",
            "2021-07-03 12:27:50.585",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        7,
        List.of(
            "Teko Barracks Clinic",
            "",
            "B+",
            "Female",
            "1Z 25A 9V5 87 1415 883 4",
            "51.6",
            "2021-05-25 12:27:51.771",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        8,
        List.of(
            "Hinistas CHC",
            "",
            "O+",
            "Female",
            "1Z 2W3 317 85 7012 770 5",
            "54.6",
            "2021-12-28 12:27:52.267",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        9,
        List.of(
            "Manjoro MCHP",
            "",
            "O+",
            "Female",
            "1Z 591 4A7 64 7727 803 6",
            "55.2",
            "2021-03-27 12:27:52.806",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        10,
        List.of(
            "Njala CHP",
            "",
            "A+",
            "Female",
            "1Z 306 51Y 42 6807 872 8",
            "95.3",
            "2021-10-22 12:27:52.517",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        11,
        List.of(
            "Makona MCHP",
            "",
            "O+",
            "Female",
            "1Z 153 376 70 9804 805 7",
            "75.8",
            "2021-12-17 12:27:52.527",
            "J40PpdN4Wkk",
            ""));
    validateRow(
        response,
        12,
        List.of(
            "Mansadu MCHP",
            "",
            "O+",
            "Female",
            "1Z 7W7 536 63 8327 711 0",
            "52.1",
            "2021-10-16 12:27:52.891",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        13,
        List.of(
            "Bombordu MCHP",
            "",
            "A+",
            "Female",
            "1Z 886 762 16 4804 491 1",
            "66.3",
            "2021-06-27 12:27:53.705",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        14,
        List.of(
            "Gbanti CHC",
            "",
            "B+",
            "Female",
            "1Z 675 9E9 97 2311 479 6",
            "91.9",
            "2021-09-28 12:27:55.071",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        15,
        List.of(
            "Ganya MCHP",
            "",
            "B+",
            "Female",
            "1Z 3Y7 425 01 1982 204 4",
            "90.4",
            "2021-05-21 12:27:55.045",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        16,
        List.of(
            "Kamasaypana MCHP",
            "",
            "O+",
            "Female",
            "1Z E85 463 28 3572 171 7",
            "54.4",
            "2021-06-14 12:27:55.438",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        17,
        List.of(
            "Masuba MCHP",
            "",
            "A+",
            "Female",
            "1Z 200 119 45 0449 691 0",
            "63.9",
            "2021-08-26 12:27:55.702",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        18,
        List.of(
            "Kasanikoro MCHP",
            "",
            "B+",
            "Female",
            "1Z 758 303 70 7842 509 2",
            "53.1",
            "2021-04-09 12:27:56.229",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        19,
        List.of(
            "Makarie MCHP",
            "",
            "A+",
            "Female",
            "1Z 552 088 87 6135 760 4",
            "98.3",
            "2021-08-07 12:27:56.569",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        20,
        List.of(
            "Mayogbor MCHP",
            "",
            "O+",
            "Female",
            "1Z 382 57V 36 6449 525 2",
            "70.0",
            "2021-12-23 12:27:56.648",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        21,
        List.of(
            "Manna MCHP",
            "",
            "O+",
            "Female",
            "1Z 6V7 562 64 0806 520 7",
            "56.7",
            "2021-05-02 12:27:56.769",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        22,
        List.of(
            "Mathufulie MCHP",
            "",
            "O-",
            "Female",
            "1Z A24 174 55 4029 787 4",
            "67.6",
            "2021-06-29 12:27:56.703",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        23,
        List.of(
            "Fothaneh Bana MCHP",
            "",
            "O+",
            "Female",
            "1Z 524 1A9 59 7730 963 6",
            "53.3",
            "2021-05-10 12:27:56.886",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        24,
        List.of(
            "Samandu MCHP",
            "",
            "B+",
            "Female",
            "1Z 272 E98 87 6867 535 1",
            "82.7",
            "2021-10-18 12:27:57.269",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        25,
        List.of(
            "Mabolleh MCHP",
            "",
            "O+",
            "Female",
            "1Z 920 150 72 4552 282 5",
            "50.8",
            "2021-09-15 12:27:57.199",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        26,
        List.of(
            "Mangay Loko MCHP",
            "",
            "A+",
            "Female",
            "1Z 707 166 53 6576 603 6",
            "91.5",
            "2021-12-10 12:27:57.284",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        27,
        List.of(
            "Nasarah Clinic",
            "",
            "A+",
            "Female",
            "1Z F27 662 48 3616 393 4",
            "52.4",
            "2021-11-03 12:27:57.34",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        28,
        List.of(
            "Royeama CHP",
            "",
            "AB+",
            "Female",
            "1Z A61 6A2 23 3626 224 4",
            "76.7",
            "2021-06-30 12:27:58.172",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        29,
        List.of(
            "Kaimunday CHP",
            "",
            "O+",
            "Female",
            "1Z 980 284 82 5130 191 4",
            "80.5",
            "2021-10-05 12:27:58.204",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        30,
        List.of(
            "Fotaneh Junction MCHP",
            "",
            "B+",
            "Female",
            "1Z W14 680 97 0747 571 6",
            "66.8",
            "2021-08-01 12:27:58.293",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        31,
        List.of(
            "Njala CHP",
            "",
            "O+",
            "Female",
            "1Z 464 4A6 08 8137 093 1",
            "51.4",
            "2021-03-30 12:27:58.968",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        32,
        List.of(
            "Rokonta CHC",
            "",
            "A+",
            "Female",
            "1Z 790 6W0 46 8927 598 3",
            "53.2",
            "2021-05-07 12:27:59.385",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        33,
        List.of(
            "Kochero MCHP",
            "",
            "B+",
            "Female",
            "1Z 27E 299 93 6419 357 4",
            "102.3",
            "2021-03-20 12:27:59.422",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        34,
        List.of(
            "Kainkordu CHC",
            "",
            "A+",
            "Female",
            "1Z 582 436 54 2310 033 8",
            "100.4",
            "2021-10-14 12:28:00.284",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        35,
        List.of(
            "Maselleh MCHP",
            "",
            "B+",
            "Female",
            "1Z 577 012 14 1155 612 6",
            "101.2",
            "2021-11-25 12:28:00.51",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        36,
        List.of(
            "Bafodia CHC",
            "",
            "O+",
            "Female",
            "1Z 4W8 535 62 9062 857 7",
            "72.6",
            "2021-09-13 12:28:01.15",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        37,
        List.of(
            "Kaliyereh MCHP",
            "",
            "O+",
            "Female",
            "1Z 263 100 38 2754 804 9",
            "64.4",
            "2021-05-07 12:28:01.198",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        38,
        List.of(
            "Yormandu CHC",
            "",
            "A+",
            "Female",
            "1Z 55V 700 59 2663 179 1",
            "79.4",
            "2021-09-28 12:28:01.652",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        39,
        List.of(
            "Kamiendor MCHP",
            "",
            "A+",
            "Female",
            "1Z W38 201 19 3591 297 7",
            "92.8",
            "2021-03-30 12:28:01.676",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        40,
        List.of(
            "Matholey MCHP",
            "",
            "B+",
            "Female",
            "1Z 0E3 81E 48 2849 435 6",
            "67.8",
            "2021-04-04 12:28:02.28",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        41,
        List.of(
            "Foria CHP",
            "",
            "A+",
            "Female",
            "1Z 617 760 32 0725 562 8",
            "85.5",
            "2021-12-13 12:28:01.942",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        42,
        List.of(
            "Dulukoro MCHP",
            "",
            "B+",
            "Female",
            "1Z 828 F67 55 1777 651 4",
            "100.7",
            "2021-10-14 12:28:02.116",
            "J40PpdN4Wkk",
            ""));
    validateRow(
        response,
        43,
        List.of(
            "Kunya MCHP",
            "",
            "O+",
            "Female",
            "1Z V54 124 19 4469 025 3",
            "62.8",
            "2021-11-11 12:28:02.657",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        44,
        List.of(
            "Durukoro MCHP",
            "",
            "O+",
            "Female",
            "1Z 652 625 20 3218 941 2",
            "82.6",
            "2021-09-17 12:28:02.84",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        45,
        List.of(
            "Tongoro MCHP",
            "",
            "AB+",
            "Female",
            "1Z F15 222 46 9808 547 4",
            "64.8",
            "2021-11-30 12:28:02.872",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        46,
        List.of(
            "Yarawadu MCHP",
            "",
            "O+",
            "Female",
            "1Z 32V 47V 84 7042 746 0",
            "53.7",
            "2021-06-14 12:28:03.111",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        47,
        List.of(
            "Koidu Under Five Clinic",
            "",
            "O+",
            "Female",
            "1Z 195 204 10 9978 253 9",
            "95.9",
            "2021-07-17 12:28:03.037",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        48,
        List.of(
            "Mathonkara MCHP",
            "",
            "B+",
            "Female",
            "1Z 504 400 08 1714 976 2",
            "57.2",
            "2021-05-17 12:28:03.685",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        49,
        List.of(
            "Kamasikie MCHP",
            "",
            "B+",
            "Female",
            "1Z E21 F38 20 1349 344 8",
            "55.7",
            "2021-05-14 12:28:04.065",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        50,
        List.of(
            "Kamakwie MCHP",
            "",
            "O+",
            "Female",
            "1Z 231 466 81 0303 882 5",
            "94.1",
            "2021-04-05 12:28:04.086",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        51,
        List.of(
            "Alkalia CHP",
            "",
            "A+",
            "Female",
            "1Z 994 439 51 8108 057 9",
            "58.0",
            "2021-05-31 12:28:04.331",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        52,
        List.of(
            "Kamadu Sokuralla MCHP",
            "",
            "B+",
            "Female",
            "1Z A50 Y14 25 3071 689 1",
            "50.2",
            "2021-12-06 12:28:04.911",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        53,
        List.of(
            "Masory MCHP",
            "",
            "AB+",
            "Female",
            "1Z 205 876 09 4174 302 0",
            "102.1",
            "2021-07-06 12:28:04.845",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        54,
        List.of(
            "Makoni Line MCHP",
            "",
            "O+",
            "Female",
            "1Z 1V1 625 67 4921 877 4",
            "60.3",
            "2021-12-26 12:28:06.0",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        55,
        List.of(
            "Masaika MCHP",
            "",
            "A+",
            "Female",
            "1Z 9F4 764 68 4852 759 4",
            "77.6",
            "2021-10-03 12:28:05.688",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        56,
        List.of(
            "Feuror MCHP",
            "",
            "O+",
            "Female",
            "1Z 781 364 08 4249 103 7",
            "71.8",
            "2021-05-15 12:28:05.776",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        57,
        List.of(
            "Mamanso Kafla MCHP",
            "",
            "A-",
            "Female",
            "1Z 020 136 47 4822 855 0",
            "90.0",
            "2021-12-18 12:28:06.249",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        58,
        List.of(
            "Gbainkfay MCHP",
            "",
            "B+",
            "Female",
            "1Z 14F 402 32 5479 921 5",
            "63.3",
            "2021-09-07 12:28:06.517",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        59,
        List.of(
            "Karina MCHP",
            "",
            "A-",
            "Female",
            "1Z W35 641 73 7249 422 5",
            "67.2",
            "2021-08-17 12:28:06.622",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        60,
        List.of(
            "Peya MCHP",
            "",
            "B+",
            "Female",
            "1Z 223 1A2 35 3136 403 0",
            "56.5",
            "2021-07-14 12:28:06.774",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        61,
        List.of(
            "Maharibo MCHP",
            "",
            "O+",
            "Female",
            "1Z 629 629 03 5025 338 2",
            "70.9",
            "2021-09-14 12:28:07.88",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        62,
        List.of(
            "Mathoir CHC",
            "",
            "O+",
            "Female",
            "1Z 534 005 48 4325 400 3",
            "54.4",
            "2021-10-01 12:28:07.773",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        63,
        List.of(
            "Alkalia CHP",
            "",
            "A+",
            "Female",
            "1Z 387 12Y 48 5023 605 1",
            "100.2",
            "2021-04-24 12:28:07.795",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        64,
        List.of(
            "Kamawornie CHP",
            "",
            "AB+",
            "Female",
            "1Z 190 167 57 3702 688 8",
            "63.4",
            "2021-12-04 12:28:08.348",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        65,
        List.of(
            "Kamabai CHC",
            "",
            "O+",
            "Female",
            "1Z 035 013 76 6624 472 9",
            "98.2",
            "2021-04-29 12:28:08.459",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        66,
        List.of(
            "Dankawalia MCHP",
            "",
            "O+",
            "Female",
            "1Z 309 7E6 00 3933 356 8",
            "76.0",
            "2021-08-28 12:28:09.708",
            "J40PpdN4Wkk",
            ""));
    validateRow(
        response,
        67,
        List.of(
            "Sukudu MCHP",
            "",
            "A+",
            "Female",
            "1Z V76 173 03 7081 845 8",
            "99.3",
            "2021-06-03 12:28:09.725",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        68,
        List.of(
            "Sandia CHP",
            "",
            "B+",
            "Female",
            "1Z W91 A32 37 6352 746 1",
            "97.0",
            "2021-07-28 12:28:10.044",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        69,
        List.of(
            "Quidadu MCHP",
            "",
            "O+",
            "Female",
            "1Z A65 318 92 5527 993 1",
            "85.2",
            "2021-07-10 12:28:10.644",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        70,
        List.of(
            "Foakor MCHP",
            "",
            "A+",
            "Female",
            "1Z A16 E64 82 8083 208 9",
            "80.5",
            "2021-03-07 12:28:10.519",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        71,
        List.of(
            "Gbendembu Wesleyan CHC",
            "",
            "B+",
            "Female",
            "1Z 193 2E1 45 5692 687 3",
            "54.4",
            "2021-11-11 12:28:10.911",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        72,
        List.of(
            "Njala CHP",
            "",
            "O+",
            "Female",
            "1Z 60Y 678 32 5462 421 8",
            "88.1",
            "2021-05-29 12:28:11.325",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        73,
        List.of(
            "Kabombeh MCHP",
            "",
            "A+",
            "Female",
            "1Z 550 605 24 4615 784 3",
            "75.0",
            "2021-03-10 12:28:11.664",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        74,
        List.of(
            "Kabonka MCHP",
            "",
            "O+",
            "Female",
            "1Z 291 Y74 08 9544 415 4",
            "69.8",
            "2021-09-20 12:28:11.36",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        75,
        List.of(
            "Rothatha MCHP",
            "",
            "B+",
            "Female",
            "1Z F25 750 76 5339 315 3",
            "92.8",
            "2021-10-12 12:28:11.367",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        76,
        List.of(
            "Durukoro MCHP",
            "",
            "A+",
            "Female",
            "1Z 728 795 74 9789 970 9",
            "57.0",
            "2021-07-12 12:28:12.046",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        77,
        List.of(
            "Kamabaio MCHP",
            "",
            "O+",
            "Female",
            "1Z 199 903 74 6025 292 3",
            "96.1",
            "2021-07-04 12:28:12.919",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        78,
        List.of(
            "Masofinia MCHP",
            "",
            "A+",
            "Female",
            "1Z 602 1Y7 73 8619 935 1",
            "55.6",
            "2021-03-08 12:28:12.967",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        79,
        List.of(
            "Baiama CHP",
            "",
            "A-",
            "Female",
            "1Z 5W1 148 19 2160 132 8",
            "50.6",
            "2021-04-05 12:28:13.365",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        80,
        List.of(
            "Mathinkalol MCHP",
            "",
            "A+",
            "Female",
            "1Z 907 520 76 2140 522 3",
            "64.2",
            "2021-07-20 12:28:14.642",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        81,
        List.of(
            "Koidu Under Five Clinic",
            "",
            "A+",
            "Female",
            "1Z 170 011 86 9311 066 0",
            "98.8",
            "2021-09-03 12:28:14.446",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        82,
        List.of(
            "Kagbere CHC",
            "",
            "A+",
            "Female",
            "1Z 280 01F 01 5863 288 8",
            "60.7",
            "2021-04-16 12:28:14.584",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        83,
        List.of(
            "Mabontor CHP",
            "",
            "B+",
            "Female",
            "1Z Y24 443 59 1665 867 1",
            "52.4",
            "2021-08-31 12:28:14.96",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        84,
        List.of(
            "Kania MCHP",
            "",
            "A+",
            "Female",
            "1Z 146 Y34 82 9220 027 3",
            "79.3",
            "2021-06-11 12:28:14.967",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        85,
        List.of(
            "Kombilie MCHP",
            "",
            "O+",
            "Female",
            "1Z 426 432 89 6505 702 3",
            "48.8",
            "2021-03-17 12:28:15.174",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        86,
        List.of(
            "Teko Barracks Clinic",
            "",
            "O-",
            "Female",
            "1Z 167 663 12 1162 685 0",
            "64.9",
            "2021-05-01 12:28:15.281",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        87,
        List.of(
            "Dulukoro MCHP",
            "",
            "A+",
            "Female",
            "1Z 11Y 726 88 6777 759 8",
            "62.6",
            "2021-12-02 12:28:15.288",
            "J40PpdN4Wkk",
            ""));
    validateRow(
        response,
        88,
        List.of(
            "Kambia CHP",
            "",
            "B+",
            "Female",
            "1Z 442 194 98 1727 928 0",
            "57.9",
            "2021-08-25 12:28:15.204",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        89,
        List.of(
            "Massingbi CHC",
            "",
            "O+",
            "Female",
            "1Z 915 3F5 22 6716 940 0",
            "60.9",
            "2021-06-10 12:28:15.445",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        90,
        List.of(
            "Kunya MCHP",
            "",
            "A+",
            "Female",
            "1Z 008 474 93 5536 353 4",
            "88.9",
            "2021-09-01 12:28:15.961",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        91,
        List.of(
            "Kunsho CHP",
            "",
            "O+",
            "Female",
            "1Z 295 911 14 8893 135 4",
            "61.4",
            "2021-08-12 12:28:16.538",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        92,
        List.of(
            "Kathanta Yimbor CHC",
            "",
            "O-",
            "Female",
            "1Z 88F 60Y 99 9732 249 8",
            "87.7",
            "2021-10-12 12:28:17.104",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        93,
        List.of(
            "Warrima MCHP",
            "",
            "A+",
            "Female",
            "1Z 059 273 24 8536 158 9",
            "99.1",
            "2021-09-26 12:28:18.062",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        94,
        List.of(
            "Kumrabai Yoni MCHP",
            "",
            "O+",
            "Female",
            "1Z 0E2 553 41 9542 779 4",
            "91.6",
            "2021-05-14 12:28:17.652",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        95,
        List.of(
            "Mansundu MCHP",
            "",
            "O-",
            "Female",
            "1Z V66 W95 61 2194 822 1",
            "79.3",
            "2021-06-06 12:28:17.961",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        96,
        List.of(
            "EM&BEE Maternity Home Clinic",
            "",
            "O+",
            "Female",
            "1Z 976 218 61 8695 879 1",
            "59.9",
            "2021-04-21 12:28:18.02",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        97,
        List.of(
            "Torkpumbu MCHP",
            "",
            "B+",
            "Female",
            "1Z 08V 333 84 5946 454 6",
            "63.1",
            "2021-08-30 12:28:18.04",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        98,
        List.of(
            "Baoma MCHP",
            "",
            "B+",
            "Female",
            "1Z 197 606 45 4217 207 1",
            "69.8",
            "2021-09-17 12:28:19.002",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        99,
        List.of(
            "Fankoya MCHP",
            "",
            "O-",
            "Female",
            "1Z 87W 383 85 5753 939 2",
            "86.8",
            "2021-06-23 12:28:18.734",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
  }

  @Test
  public void queryRandom2() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=uIuxlbV1vRT:J40PpdN4Wkk")
            .add("includeMetadataDetails=true")
            .add("asc=lastupdated")
            .add(
                "headers=ouname,spFvx9FndA4,H9IlTX2X6SL,cejWyOfXge6,ruQQnf6rswq,OvY4VVhSDeJ,Bpx0589u8y0,lastupdated")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2021")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:ImspTQPwCqd,spFvx9FndA4,H9IlTX2X6SL,cejWyOfXge6:IN:Female,ruQQnf6rswq,OvY4VVhSDeJ,Bpx0589u8y0")
            .add("relativePeriodDate=2023-07-14");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(8))
        .body("headerWidth", equalTo(8));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"uIuxlbV1vRT\":{\"uid\":\"uIuxlbV1vRT\",\"name\":\"Area\",\"dimensionType\":\"ORGANISATION_UNIT_GROUP_SET\"},\"2021\":{\"name\":\"2021\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"MAs88nJc9nL\":{\"uid\":\"MAs88nJc9nL\",\"code\":\"Private Clinic\",\"name\":\"Private Clinic\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"ruQQnf6rswq\":{\"uid\":\"ruQQnf6rswq\",\"name\":\"TB number\",\"description\":\"TB number\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"J40PpdN4Wkk\":{\"uid\":\"J40PpdN4Wkk\",\"name\":\"Northern Area\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"PVLOW4bCshG\":{\"uid\":\"PVLOW4bCshG\",\"code\":\"NGO\",\"name\":\"NGO\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"oRVt7g429ZO\":{\"uid\":\"oRVt7g429ZO\",\"code\":\"Public facilities\",\"name\":\"Public facilities\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"Bpx0589u8y0\":{\"uid\":\"Bpx0589u8y0\",\"name\":\"Facility Ownership\",\"dimensionType\":\"ORGANISATION_UNIT_GROUP_SET\"},\"w0gFTTmsUcF\":{\"uid\":\"w0gFTTmsUcF\",\"code\":\"Mission\",\"name\":\"Mission\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"spFvx9FndA4\":{\"uid\":\"spFvx9FndA4\",\"name\":\"Age\",\"description\":\"Age\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"H9IlTX2X6SL\":{\"uid\":\"H9IlTX2X6SL\",\"code\":\"Blood type\",\"name\":\"Blood type\",\"description\":\"Blood type\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"OvY4VVhSDeJ\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"uIuxlbV1vRT\":[\"J40PpdN4Wkk\"],\"Bpx0589u8y0\":[\"MAs88nJc9nL\",\"PVLOW4bCshG\",\"w0gFTTmsUcF\",\"oRVt7g429ZO\"],\"spFvx9FndA4\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"Mnp3oXrpAbK\"],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "spFvx9FndA4", "Age", "AGE", "java.util.Date", false, true);
    validateHeader(
        response, 2, "H9IlTX2X6SL", "Blood type", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 4, "ruQQnf6rswq", "TB number", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 5, "OvY4VVhSDeJ", "Weight in kg", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 6, "Bpx0589u8y0", "Facility Ownership", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 7, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Kamaranka CHC",
            "",
            "A+",
            "Female",
            "1Z 403 A74 58 7426 320 0",
            "88.1",
            "oRVt7g429ZO",
            "2017-03-28 12:27:48.659"));
    validateRow(
        response,
        1,
        List.of(
            "Makoba Bana MCHP",
            "",
            "O+",
            "Female",
            "1Z 642 921 45 4137 673 7",
            "74.2",
            "oRVt7g429ZO",
            "2017-03-28 12:27:48.707"));
    validateRow(
        response,
        2,
        List.of(
            "Kayongoro MCHP",
            "",
            "A+",
            "Female",
            "1Z 429 49F 80 2259 546 1",
            "99.7",
            "oRVt7g429ZO",
            "2017-03-28 12:27:49.331"));
    validateRow(
        response,
        3,
        List.of(
            "Kemedugu MCHP",
            "",
            "O+",
            "Female",
            "1Z 581 532 75 9366 399 0",
            "86.5",
            "oRVt7g429ZO",
            "2017-03-28 12:27:49.465"));
    validateRow(
        response,
        4,
        List.of(
            "Rothatha MCHP",
            "",
            "O+",
            "Female",
            "1Z A93 775 07 0458 684 4",
            "89.7",
            "oRVt7g429ZO",
            "2017-03-28 12:27:49.833"));
    validateRow(
        response,
        5,
        List.of(
            "Tonko Maternity Clinic",
            "",
            "A+",
            "Female",
            "1Z 512 807 87 2912 636 2",
            "66.7",
            "oRVt7g429ZO",
            "2017-03-28 12:27:50.13"));
    validateRow(
        response,
        6,
        List.of(
            "Dulukoro MCHP",
            "",
            "A+",
            "Female",
            "1Z V93 757 18 8966 694 0",
            "69.0",
            "",
            "2017-03-28 12:27:50.415"));
    validateRow(
        response,
        7,
        List.of(
            "Mayossoh MCHP",
            "",
            "A+",
            "Female",
            "1Z 501 391 59 1103 658 3",
            "96.9",
            "oRVt7g429ZO",
            "2017-03-28 12:27:50.444"));
    validateRow(
        response,
        8,
        List.of(
            "Maharie MCHP",
            "",
            "A+",
            "Female",
            "1Z 868 032 85 2314 849 4",
            "56.1",
            "oRVt7g429ZO",
            "2017-03-28 12:27:50.524"));
    validateRow(
        response,
        9,
        List.of(
            "Makeni-Rokfullah MCHP",
            "",
            "AB+",
            "Female",
            "1Z 526 96E 65 7742 917 0",
            "55.7",
            "oRVt7g429ZO",
            "2017-03-28 12:27:50.542"));
    validateRow(
        response,
        10,
        List.of(
            "Heremakono MCHP",
            "",
            "A+",
            "Female",
            "1Z 702 455 54 8490 727 4",
            "83.4",
            "oRVt7g429ZO",
            "2017-03-28 12:27:50.581"));
    validateRow(
        response,
        11,
        List.of(
            "Kondeya (Sandor) MCHP",
            "",
            "A+",
            "Female",
            "1Z 355 W25 56 2039 227 9",
            "72.6",
            "oRVt7g429ZO",
            "2017-03-28 12:27:50.591"));
    validateRow(
        response,
        12,
        List.of(
            "Kania MCHP",
            "",
            "A+",
            "Female",
            "1Z 667 96V 14 4714 325 7",
            "83.9",
            "oRVt7g429ZO",
            "2017-03-28 12:27:50.655"));
    validateRow(
        response,
        13,
        List.of(
            "Kiampkakolo MCHP",
            "",
            "B-",
            "Female",
            "1Z 9Y1 17V 01 7396 072 7",
            "60.5",
            "oRVt7g429ZO",
            "2017-03-28 12:27:50.873"));
    validateRow(
        response,
        14,
        List.of(
            "Gbangadu MCHP",
            "",
            "O+",
            "Female",
            "1Z V39 276 59 1180 995 6",
            "75.7",
            "oRVt7g429ZO",
            "2017-03-28 12:27:50.966"));
    validateRow(
        response,
        15,
        List.of(
            "Tefeya CHP",
            "",
            "A+",
            "Female",
            "1Z 751 969 47 0091 497 9",
            "89.4",
            "oRVt7g429ZO",
            "2017-03-28 12:27:51.295"));
    validateRow(
        response,
        16,
        List.of(
            "Kayongoro MCHP",
            "",
            "B+",
            "Female",
            "1Z 985 03V 68 0988 752 0",
            "83.4",
            "oRVt7g429ZO",
            "2017-03-28 12:27:51.463"));
    validateRow(
        response,
        17,
        List.of(
            "Marie Stopes (Gbense) Clinic",
            "",
            "AB+",
            "Female",
            "1Z 263 8Y8 72 6499 982 6",
            "71.8",
            "",
            "2017-03-28 12:27:51.562"));
    validateRow(
        response,
        18,
        List.of(
            "Kondeya (Sandor) MCHP",
            "",
            "B+",
            "Female",
            "1Z 000 504 58 2791 823 2",
            "78.8",
            "oRVt7g429ZO",
            "2017-03-28 12:27:51.588"));
    validateRow(
        response,
        19,
        List.of(
            "Wordu CHP",
            "",
            "O-",
            "Female",
            "1Z 025 V35 17 0946 674 7",
            "49.0",
            "oRVt7g429ZO",
            "2017-03-28 12:27:51.731"));
    validateRow(
        response,
        20,
        List.of(
            "Teko Barracks Clinic",
            "",
            "B+",
            "Female",
            "1Z 25A 9V5 87 1415 883 4",
            "51.6",
            "oRVt7g429ZO",
            "2017-03-28 12:27:51.789"));
    validateRow(
        response,
        21,
        List.of(
            "Hinistas CHC",
            "",
            "O+",
            "Female",
            "1Z 2W3 317 85 7012 770 5",
            "54.6",
            "oRVt7g429ZO",
            "2017-03-28 12:27:52.273"));
    validateRow(
        response,
        22,
        List.of(
            "Makaiba MCHP",
            "",
            "B-",
            "Female",
            "1Z 118 A79 29 1039 970 1",
            "59.9",
            "oRVt7g429ZO",
            "2017-03-28 12:27:52.512"));
    validateRow(
        response,
        23,
        List.of(
            "Njala CHP",
            "",
            "A+",
            "Female",
            "1Z 306 51Y 42 6807 872 8",
            "95.3",
            "oRVt7g429ZO",
            "2017-03-28 12:27:52.523"));
    validateRow(
        response,
        24,
        List.of(
            "Makona MCHP",
            "",
            "O+",
            "Female",
            "1Z 153 376 70 9804 805 7",
            "75.8",
            "",
            "2017-03-28 12:27:52.532"));
    validateRow(
        response,
        25,
        List.of(
            "Njagbwema Fiama CHC",
            "",
            "O+",
            "Female",
            "1Z 805 F83 56 7490 844 4",
            "98.7",
            "oRVt7g429ZO",
            "2017-03-28 12:27:52.552"));
    validateRow(
        response,
        26,
        List.of(
            "Manjoro MCHP",
            "",
            "O+",
            "Female",
            "1Z 591 4A7 64 7727 803 6",
            "55.2",
            "oRVt7g429ZO",
            "2017-03-28 12:27:52.814"));
    validateRow(
        response,
        27,
        List.of(
            "Mansadu MCHP",
            "",
            "O+",
            "Female",
            "1Z 7W7 536 63 8327 711 0",
            "52.1",
            "oRVt7g429ZO",
            "2017-03-28 12:27:52.905"));
    validateRow(
        response,
        28,
        List.of(
            "Makarie MCHP",
            "",
            "A+",
            "Female",
            "1Z 234 229 75 9938 524 2",
            "89.3",
            "oRVt7g429ZO",
            "2017-03-28 12:27:53.131"));
    validateRow(
        response,
        29,
        List.of(
            "Kondewakoro CHP",
            "",
            "B+",
            "Female",
            "1Z 741 083 83 5827 323 6",
            "107.2",
            "oRVt7g429ZO",
            "2017-03-28 12:27:53.339"));
    validateRow(
        response,
        30,
        List.of(
            "Bombordu MCHP",
            "",
            "A+",
            "Female",
            "1Z 886 762 16 4804 491 1",
            "66.3",
            "oRVt7g429ZO",
            "2017-03-28 12:27:53.711"));
    validateRow(
        response,
        31,
        List.of(
            "Fadugu CHC",
            "",
            "O+",
            "Female",
            "1Z 407 Y87 85 5694 750 2",
            "66.1",
            "oRVt7g429ZO",
            "2017-03-28 12:27:54.993"));
    validateRow(
        response,
        32,
        List.of(
            "Ganya MCHP",
            "",
            "B+",
            "Female",
            "1Z 3Y7 425 01 1982 204 4",
            "90.4",
            "oRVt7g429ZO",
            "2017-03-28 12:27:55.05"));
    validateRow(
        response,
        33,
        List.of(
            "Gbanti CHC",
            "",
            "B+",
            "Female",
            "1Z 675 9E9 97 2311 479 6",
            "91.9",
            "oRVt7g429ZO",
            "2017-03-28 12:27:55.076"));
    validateRow(
        response,
        34,
        List.of(
            "Fullah Town (M.Gbanti) MCHP",
            "",
            "A+",
            "Female",
            "1Z F98 841 01 5372 202 9",
            "97.5",
            "oRVt7g429ZO",
            "2017-03-28 12:27:55.314"));
    validateRow(
        response,
        35,
        List.of(
            "Kamasaypana MCHP",
            "",
            "O+",
            "Female",
            "1Z E85 463 28 3572 171 7",
            "54.4",
            "oRVt7g429ZO",
            "2017-03-28 12:27:55.444"));
    validateRow(
        response,
        36,
        List.of(
            "Peya MCHP",
            "",
            "O+",
            "Female",
            "1Z Y68 W50 37 7234 403 1",
            "77.6",
            "oRVt7g429ZO",
            "2017-03-28 12:27:55.54"));
    validateRow(
        response,
        37,
        List.of(
            "Kombilie MCHP",
            "",
            "O+",
            "Female",
            "1Z 941 564 85 0250 662 5",
            "93.1",
            "oRVt7g429ZO",
            "2017-03-28 12:27:55.565"));
    validateRow(
        response,
        38,
        List.of(
            "Makaiba MCHP",
            "",
            "A+",
            "Female",
            "1Z 498 6A5 30 3542 125 8",
            "56.3",
            "oRVt7g429ZO",
            "2017-03-28 12:27:55.581"));
    validateRow(
        response,
        39,
        List.of(
            "Masuba MCHP",
            "",
            "A+",
            "Female",
            "1Z 200 119 45 0449 691 0",
            "63.9",
            "oRVt7g429ZO",
            "2017-03-28 12:27:55.708"));
    validateRow(
        response,
        40,
        List.of(
            "Mabonkanie MCHP",
            "",
            "O+",
            "Female",
            "1Z 314 0F4 71 9417 098 3",
            "97.3",
            "oRVt7g429ZO",
            "2017-03-28 12:27:55.793"));
    validateRow(
        response,
        41,
        List.of(
            "Kaponkie MCHP",
            "",
            "O+",
            "Female",
            "1Z 568 833 94 7813 703 5",
            "55.1",
            "oRVt7g429ZO",
            "2017-03-28 12:27:55.985"));
    validateRow(
        response,
        42,
        List.of(
            "Kasanikoro MCHP",
            "",
            "B+",
            "Female",
            "1Z 758 303 70 7842 509 2",
            "53.1",
            "oRVt7g429ZO",
            "2017-03-28 12:27:56.236"));
    validateRow(
        response,
        43,
        List.of(
            "Makarie MCHP",
            "",
            "A+",
            "Female",
            "1Z 552 088 87 6135 760 4",
            "98.3",
            "oRVt7g429ZO",
            "2017-03-28 12:27:56.574"));
    validateRow(
        response,
        44,
        List.of(
            "Mayogbor MCHP",
            "",
            "O+",
            "Female",
            "1Z 382 57V 36 6449 525 2",
            "70.0",
            "oRVt7g429ZO",
            "2017-03-28 12:27:56.652"));
    validateRow(
        response,
        45,
        List.of(
            "Maborie MCHP",
            "",
            "O+",
            "Female",
            "1Z 242 612 10 2671 334 1",
            "82.3",
            "oRVt7g429ZO",
            "2017-03-28 12:27:56.667"));
    validateRow(
        response,
        46,
        List.of(
            "Mathufulie MCHP",
            "",
            "O-",
            "Female",
            "1Z A24 174 55 4029 787 4",
            "67.6",
            "oRVt7g429ZO",
            "2017-03-28 12:27:56.707"));
    validateRow(
        response,
        47,
        List.of(
            "Manna MCHP",
            "",
            "O+",
            "Female",
            "1Z 6V7 562 64 0806 520 7",
            "56.7",
            "oRVt7g429ZO",
            "2017-03-28 12:27:56.774"));
    validateRow(
        response,
        48,
        List.of(
            "Fothaneh Bana MCHP",
            "",
            "O+",
            "Female",
            "1Z 524 1A9 59 7730 963 6",
            "53.3",
            "oRVt7g429ZO",
            "2017-03-28 12:27:56.894"));
    validateRow(
        response,
        49,
        List.of(
            "Kamasaypana MCHP",
            "",
            "B+",
            "Female",
            "1Z 283 614 40 2788 333 8",
            "76.4",
            "oRVt7g429ZO",
            "2017-03-28 12:27:57.019"));
    validateRow(
        response,
        50,
        List.of(
            "Mabolleh MCHP",
            "",
            "O+",
            "Female",
            "1Z 920 150 72 4552 282 5",
            "50.8",
            "oRVt7g429ZO",
            "2017-03-28 12:27:57.213"));
    validateRow(
        response,
        51,
        List.of(
            "Samandu MCHP",
            "",
            "B+",
            "Female",
            "1Z 272 E98 87 6867 535 1",
            "82.7",
            "oRVt7g429ZO",
            "2017-03-28 12:27:57.276"));
    validateRow(
        response,
        52,
        List.of(
            "Mangay Loko MCHP",
            "",
            "A+",
            "Female",
            "1Z 707 166 53 6576 603 6",
            "91.5",
            "oRVt7g429ZO",
            "2017-03-28 12:27:57.293"));
    validateRow(
        response,
        53,
        List.of(
            "Nasarah Clinic",
            "",
            "A+",
            "Female",
            "1Z F27 662 48 3616 393 4",
            "52.4",
            "oRVt7g429ZO",
            "2017-03-28 12:27:57.353"));
    validateRow(
        response,
        54,
        List.of(
            "Gbonkobana CHP",
            "",
            "A+",
            "Female",
            "1Z 89F 937 95 6735 286 9",
            "81.0",
            "oRVt7g429ZO",
            "2017-03-28 12:27:57.378"));
    validateRow(
        response,
        55,
        List.of(
            "Masankorie CHP",
            "",
            "A+",
            "Female",
            "1Z 690 173 24 8049 133 0",
            "97.8",
            "oRVt7g429ZO",
            "2017-03-28 12:27:57.497"));
    validateRow(
        response,
        56,
        List.of(
            "Tefeya CHP",
            "",
            "O+",
            "Female",
            "1Z 304 968 70 5853 168 1",
            "98.4",
            "oRVt7g429ZO",
            "2017-03-28 12:27:57.792"));
    validateRow(
        response,
        57,
        List.of(
            "Royeama CHP",
            "",
            "AB+",
            "Female",
            "1Z A61 6A2 23 3626 224 4",
            "76.7",
            "oRVt7g429ZO",
            "2017-03-28 12:27:58.176"));
    validateRow(
        response,
        58,
        List.of(
            "Kaimunday CHP",
            "",
            "O+",
            "Female",
            "1Z 980 284 82 5130 191 4",
            "80.5",
            "oRVt7g429ZO",
            "2017-03-28 12:27:58.209"));
    validateRow(
        response,
        59,
        List.of(
            "Fotaneh Junction MCHP",
            "",
            "B+",
            "Female",
            "1Z W14 680 97 0747 571 6",
            "66.8",
            "oRVt7g429ZO",
            "2017-03-28 12:27:58.298"));
    validateRow(
        response,
        60,
        List.of(
            "Kambia CHP",
            "",
            "B-",
            "Female",
            "1Z 3E5 946 92 6991 366 3",
            "70.2",
            "oRVt7g429ZO",
            "2017-03-28 12:27:58.371"));
    validateRow(
        response,
        61,
        List.of(
            "Njala CHP",
            "",
            "O+",
            "Female",
            "1Z 464 4A6 08 8137 093 1",
            "51.4",
            "oRVt7g429ZO",
            "2017-03-28 12:27:58.973"));
    validateRow(
        response,
        62,
        List.of(
            "Manjoro MCHP",
            "",
            "O+",
            "Female",
            "1Z 4A5 18W 75 7410 905 8",
            "91.2",
            "oRVt7g429ZO",
            "2017-03-28 12:27:59.103"));
    validateRow(
        response,
        63,
        List.of(
            "Nasarah Clinic",
            "",
            "O+",
            "Female",
            "1Z E93 069 55 5523 404 4",
            "86.5",
            "oRVt7g429ZO",
            "2017-03-28 12:27:59.181"));
    validateRow(
        response,
        64,
        List.of(
            "Mabontor CHP",
            "",
            "AB+",
            "Female",
            "1Z F88 822 64 7661 513 0",
            "100.3",
            "oRVt7g429ZO",
            "2017-03-28 12:27:59.347"));
    validateRow(
        response,
        65,
        List.of(
            "Rokonta CHC",
            "",
            "A+",
            "Female",
            "1Z 790 6W0 46 8927 598 3",
            "53.2",
            "oRVt7g429ZO",
            "2017-03-28 12:27:59.391"));
    validateRow(
        response,
        66,
        List.of(
            "Kochero MCHP",
            "",
            "B+",
            "Female",
            "1Z 27E 299 93 6419 357 4",
            "102.3",
            "oRVt7g429ZO",
            "2017-03-28 12:27:59.432"));
    validateRow(
        response,
        67,
        List.of(
            "Massaba MCHP",
            "",
            "A+",
            "Female",
            "1Z 278 10Y 17 8218 652 6",
            "64.8",
            "",
            "2017-03-28 12:27:59.517"));
    validateRow(
        response,
        68,
        List.of(
            "Masongbo Limba MCHP",
            "",
            "O+",
            "Female",
            "1Z 837 046 04 6711 464 8",
            "50.6",
            "oRVt7g429ZO",
            "2017-03-28 12:27:59.546"));
    validateRow(
        response,
        69,
        List.of(
            "Kondewakoro CHP",
            "",
            "A+",
            "Female",
            "1Z 27Y 204 66 0229 394 5",
            "82.3",
            "oRVt7g429ZO",
            "2017-03-28 12:27:59.553"));
    validateRow(
        response,
        70,
        List.of(
            "Matoto MCHP",
            "",
            "A+",
            "Female",
            "1Z 571 869 08 2432 829 3",
            "70.7",
            "oRVt7g429ZO",
            "2017-03-28 12:27:59.675"));
    validateRow(
        response,
        71,
        List.of(
            "The White House Clinic",
            "",
            "AB+",
            "Female",
            "1Z 9Y0 719 68 0537 730 4",
            "101.4",
            "",
            "2017-03-28 12:28:00.112"));
    validateRow(
        response,
        72,
        List.of(
            "Kainkordu CHC",
            "",
            "A+",
            "Female",
            "1Z 582 436 54 2310 033 8",
            "100.4",
            "oRVt7g429ZO",
            "2017-03-28 12:28:00.298"));
    validateRow(
        response,
        73,
        List.of(
            "Maselleh MCHP",
            "",
            "B+",
            "Female",
            "1Z 577 012 14 1155 612 6",
            "101.2",
            "oRVt7g429ZO",
            "2017-03-28 12:28:00.524"));
    validateRow(
        response,
        74,
        List.of(
            "Woama MCHP",
            "",
            "O+",
            "Female",
            "1Z 891 651 29 5708 781 0",
            "69.8",
            "oRVt7g429ZO",
            "2017-03-28 12:28:01.126"));
    validateRow(
        response,
        75,
        List.of(
            "Bafodia CHC",
            "",
            "O+",
            "Female",
            "1Z 4W8 535 62 9062 857 7",
            "72.6",
            "oRVt7g429ZO",
            "2017-03-28 12:28:01.154"));
    validateRow(
        response,
        76,
        List.of(
            "Kaliyereh MCHP",
            "",
            "O+",
            "Female",
            "1Z 263 100 38 2754 804 9",
            "64.4",
            "oRVt7g429ZO",
            "2017-03-28 12:28:01.204"));
    validateRow(
        response,
        77,
        List.of(
            "Koakoyima CHC",
            "",
            "A+",
            "Female",
            "1Z 966 112 87 5758 002 5",
            "54.8",
            "oRVt7g429ZO",
            "2017-03-28 12:28:01.327"));
    validateRow(
        response,
        78,
        List.of(
            "Yormandu CHC",
            "",
            "A+",
            "Female",
            "1Z 55V 700 59 2663 179 1",
            "79.4",
            "oRVt7g429ZO",
            "2017-03-28 12:28:01.666"));
    validateRow(
        response,
        79,
        List.of(
            "Kamiendor MCHP",
            "",
            "A+",
            "Female",
            "1Z W38 201 19 3591 297 7",
            "92.8",
            "oRVt7g429ZO",
            "2017-03-28 12:28:01.685"));
    validateRow(
        response,
        80,
        List.of(
            "Yonibana MCHP",
            "",
            "O-",
            "Female",
            "1Z 12A 531 63 8264 455 2",
            "64.6",
            "oRVt7g429ZO",
            "2017-03-28 12:28:01.899"));
    validateRow(
        response,
        81,
        List.of(
            "Foria CHP",
            "",
            "A+",
            "Female",
            "1Z 617 760 32 0725 562 8",
            "85.5",
            "oRVt7g429ZO",
            "2017-03-28 12:28:01.953"));
    validateRow(
        response,
        82,
        List.of(
            "Dulukoro MCHP",
            "",
            "B+",
            "Female",
            "1Z 828 F67 55 1777 651 4",
            "100.7",
            "",
            "2017-03-28 12:28:02.129"));
    validateRow(
        response,
        83,
        List.of(
            "Matholey MCHP",
            "",
            "B+",
            "Female",
            "1Z 0E3 81E 48 2849 435 6",
            "67.8",
            "oRVt7g429ZO",
            "2017-03-28 12:28:02.284"));
    validateRow(
        response,
        84,
        List.of(
            "Njagbwema CHP",
            "",
            "O-",
            "Female",
            "1Z 50Y 807 45 6007 309 8",
            "56.1",
            "oRVt7g429ZO",
            "2017-03-28 12:28:02.553"));
    validateRow(
        response,
        85,
        List.of(
            "Foindu MCHP",
            "",
            "A+",
            "Female",
            "1Z 165 399 26 8656 025 3",
            "85.9",
            "oRVt7g429ZO",
            "2017-03-28 12:28:02.577"));
    validateRow(
        response,
        86,
        List.of(
            "Kunya MCHP",
            "",
            "O+",
            "Female",
            "1Z V54 124 19 4469 025 3",
            "62.8",
            "oRVt7g429ZO",
            "2017-03-28 12:28:02.67"));
    validateRow(
        response,
        87,
        List.of(
            "Durukoro MCHP",
            "",
            "O+",
            "Female",
            "1Z 652 625 20 3218 941 2",
            "82.6",
            "oRVt7g429ZO",
            "2017-03-28 12:28:02.845"));
    validateRow(
        response,
        88,
        List.of(
            "Tongoro MCHP",
            "",
            "AB+",
            "Female",
            "1Z F15 222 46 9808 547 4",
            "64.8",
            "oRVt7g429ZO",
            "2017-03-28 12:28:02.876"));
    validateRow(
        response,
        89,
        List.of(
            "Tefeya CHP",
            "",
            "B+",
            "Female",
            "1Z 97V E71 87 0776 844 6",
            "89.8",
            "oRVt7g429ZO",
            "2017-03-28 12:28:02.972"));
    validateRow(
        response,
        90,
        List.of(
            "Koidu Under Five Clinic",
            "",
            "O+",
            "Female",
            "1Z 195 204 10 9978 253 9",
            "95.9",
            "oRVt7g429ZO",
            "2017-03-28 12:28:03.041"));
    validateRow(
        response,
        91,
        List.of(
            "Falaba CHC",
            "",
            "B+",
            "Female",
            "1Z 849 08V 32 1340 078 9",
            "84.3",
            "oRVt7g429ZO",
            "2017-03-28 12:28:03.061"));
    validateRow(
        response,
        92,
        List.of(
            "Yarawadu MCHP",
            "",
            "O+",
            "Female",
            "1Z 32V 47V 84 7042 746 0",
            "53.7",
            "oRVt7g429ZO",
            "2017-03-28 12:28:03.115"));
    validateRow(
        response,
        93,
        List.of(
            "Mathonkara MCHP",
            "",
            "B+",
            "Female",
            "1Z 504 400 08 1714 976 2",
            "57.2",
            "oRVt7g429ZO",
            "2017-03-28 12:28:03.699"));
    validateRow(
        response,
        94,
        List.of(
            "Kamasikie MCHP",
            "",
            "B+",
            "Female",
            "1Z E21 F38 20 1349 344 8",
            "55.7",
            "oRVt7g429ZO",
            "2017-03-28 12:28:04.077"));
    validateRow(
        response,
        95,
        List.of(
            "Kamakwie MCHP",
            "",
            "O+",
            "Female",
            "1Z 231 466 81 0303 882 5",
            "94.1",
            "oRVt7g429ZO",
            "2017-03-28 12:28:04.099"));
    validateRow(
        response,
        96,
        List.of(
            "Alkalia CHP",
            "",
            "A+",
            "Female",
            "1Z 994 439 51 8108 057 9",
            "58.0",
            "oRVt7g429ZO",
            "2017-03-28 12:28:04.344"));
    validateRow(
        response,
        97,
        List.of(
            "Kpetema CHP (Toli)",
            "",
            "O-",
            "Female",
            "1Z 266 8A1 93 7291 901 2",
            "62.1",
            "oRVt7g429ZO",
            "2017-03-28 12:28:04.41"));
    validateRow(
        response,
        98,
        List.of(
            "Masanga Leprosy Hospital",
            "",
            "O+",
            "Female",
            "1Z 798 072 16 6627 435 9",
            "70.3",
            "oRVt7g429ZO",
            "2017-03-28 12:28:04.6"));
    validateRow(
        response,
        99,
        List.of(
            "Kumrabai Yoni MCHP",
            "",
            "O+",
            "Female",
            "1Z 674 009 41 8990 655 3",
            "102.1",
            "oRVt7g429ZO",
            "2017-03-28 12:28:04.683"));
  }

  @Test
  public void queryRandom3() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=lastupdated")
            .add("headers=ouname,Bpx0589u8y0,lastupdated,Kv4fmHVAzwX")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2021")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ou:ImspTQPwCqd,Bpx0589u8y0,Kv4fmHVAzwX")
            .add("relativePeriodDate=2023-07-14");

    // When
    ApiResponse response = actions.query().get("M3xtLkYBlKI", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(8)))
        .body("height", equalTo(8))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"Kv4fmHVAzwX\":{\"uid\":\"Kv4fmHVAzwX\",\"name\":\"Focus Name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"CWaAcQYKVpq\":{\"uid\":\"CWaAcQYKVpq\",\"name\":\"Foci investigation & classification\",\"description\":\"Includes the details on the foci investigation (including information on households, population, geography, breeding sites, species types, vector behaviour) as well as its final classification at the time of the investigation. This is a repeatable stage as foci can be investigated more than once and may change their classification as time goes on. \"},\"2021\":{\"name\":\"2021\"},\"PVLOW4bCshG\":{\"uid\":\"PVLOW4bCshG\",\"code\":\"NGO\",\"name\":\"NGO\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"oRVt7g429ZO\":{\"uid\":\"oRVt7g429ZO\",\"code\":\"Public facilities\",\"name\":\"Public facilities\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"Bpx0589u8y0\":{\"uid\":\"Bpx0589u8y0\",\"name\":\"Facility Ownership\",\"dimensionType\":\"ORGANISATION_UNIT_GROUP_SET\"},\"uvMKOn1oWvd\":{\"uid\":\"uvMKOn1oWvd\",\"name\":\"Foci response\",\"description\":\"Details the public health response conducted within the foci  (including diagnosis and treatment activities, vector control actions and the effectiveness/results of the response). This is a repeatable stage as multiple public health responses for the same foci can occur depending on its classification at the time of investigation.\"},\"w0gFTTmsUcF\":{\"uid\":\"w0gFTTmsUcF\",\"code\":\"Mission\",\"name\":\"Mission\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"M3xtLkYBlKI\":{\"uid\":\"M3xtLkYBlKI\",\"name\":\"Malaria focus investigation\",\"description\":\"It allows to register new focus areas in the system. Each focus area needs to be investigated and classified. Includes the relevant identifiers for the foci including the name and geographical details including the locality and its area. \"},\"MAs88nJc9nL\":{\"uid\":\"MAs88nJc9nL\",\"code\":\"Private Clinic\",\"name\":\"Private Clinic\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"Kv4fmHVAzwX\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"Bpx0589u8y0\":[\"MAs88nJc9nL\",\"PVLOW4bCshG\",\"w0gFTTmsUcF\",\"oRVt7g429ZO\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "Bpx0589u8y0", "Facility Ownership", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 3, "Kv4fmHVAzwX", "Focus Name", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Njandama MCHP",
            "oRVt7g429ZO",
            "2019-08-21 13:29:14.578",
            "Village 32 Sarah Baartman"));
    validateRow(
        response,
        1,
        List.of("Ngelehun CHC", "oRVt7g429ZO", "2019-08-21 13:29:21.574", "Focus A focus"));
    validateRow(
        response,
        2,
        List.of(
            "Njandama MCHP",
            "oRVt7g429ZO",
            "2019-08-21 13:29:24.678",
            "Village 28 Oliver Tambo District"));
    validateRow(
        response,
        3,
        List.of(
            "Ngelehun CHC",
            "oRVt7g429ZO",
            "2019-08-21 13:29:28.064",
            "Village 5 Alfred Nzo Municipality"));
    validateRow(
        response,
        4,
        List.of("Njandama MCHP", "oRVt7g429ZO", "2019-08-21 13:29:31.708", "Village C"));
    validateRow(
        response,
        5,
        List.of("Njandama MCHP", "oRVt7g429ZO", "2019-08-21 13:29:37.117", "Village B"));
    validateRow(
        response,
        6,
        List.of(
            "Ngelehun CHC",
            "oRVt7g429ZO",
            "2019-08-21 13:29:39.311",
            "Village 17 Alfred Nzo Municipality"));
    validateRow(
        response,
        7,
        List.of("Ngelehun CHC", "oRVt7g429ZO", "2019-08-21 13:29:44.942", "Test focus"));
  }

  @Test
  public void queryRandom4() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=zyTL3AMIkf2")
            .add("includeMetadataDetails=true")
            .add("asc=lastupdated")
            .add("headers=ouname,Bpx0589u8y0,lastupdated,Kv4fmHVAzwX,CH6wamtY9kK")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=2021")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("page=1")
            .add("dimension=ou:ImspTQPwCqd,Bpx0589u8y0,Kv4fmHVAzwX,CH6wamtY9kK")
            .add("relativePeriodDate=2023-07-14");

    // When
    ApiResponse response = actions.query().get("M3xtLkYBlKI", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(8)))
        .body("height", equalTo(8))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"Kv4fmHVAzwX\":{\"uid\":\"Kv4fmHVAzwX\",\"name\":\"Focus Name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"CH6wamtY9kK\":{\"uid\":\"CH6wamtY9kK\",\"name\":\"Foci investigations performed & classified\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"2021\":{\"name\":\"2021\"},\"MAs88nJc9nL\":{\"uid\":\"MAs88nJc9nL\",\"code\":\"Private Clinic\",\"name\":\"Private Clinic\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"CWaAcQYKVpq\":{\"uid\":\"CWaAcQYKVpq\",\"name\":\"Foci investigation & classification\",\"description\":\"Includes the details on the foci investigation (including information on households, population, geography, breeding sites, species types, vector behaviour) as well as its final classification at the time of the investigation. This is a repeatable stage as foci can be investigated more than once and may change their classification as time goes on. \"},\"PVLOW4bCshG\":{\"uid\":\"PVLOW4bCshG\",\"code\":\"NGO\",\"name\":\"NGO\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"oRVt7g429ZO\":{\"uid\":\"oRVt7g429ZO\",\"code\":\"Public facilities\",\"name\":\"Public facilities\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"Bpx0589u8y0\":{\"uid\":\"Bpx0589u8y0\",\"name\":\"Facility Ownership\",\"dimensionType\":\"ORGANISATION_UNIT_GROUP_SET\"},\"uvMKOn1oWvd\":{\"uid\":\"uvMKOn1oWvd\",\"name\":\"Foci response\",\"description\":\"Details the public health response conducted within the foci  (including diagnosis and treatment activities, vector control actions and the effectiveness/results of the response). This is a repeatable stage as multiple public health responses for the same foci can occur depending on its classification at the time of investigation.\"},\"zyTL3AMIkf2\":{\"uid\":\"zyTL3AMIkf2\",\"name\":\"Foci classified as active\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"w0gFTTmsUcF\":{\"uid\":\"w0gFTTmsUcF\",\"code\":\"Mission\",\"name\":\"Mission\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"M3xtLkYBlKI\":{\"uid\":\"M3xtLkYBlKI\",\"name\":\"Malaria focus investigation\",\"description\":\"It allows to register new focus areas in the system. Each focus area needs to be investigated and classified. Includes the relevant identifiers for the foci including the name and geographical details including the locality and its area. \"}},\"dimensions\":{\"Kv4fmHVAzwX\":[],\"CH6wamtY9kK\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"Bpx0589u8y0\":[\"MAs88nJc9nL\",\"PVLOW4bCshG\",\"w0gFTTmsUcF\",\"oRVt7g429ZO\"],\"zyTL3AMIkf2\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "Bpx0589u8y0", "Facility Ownership", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true);
    validateHeader(
        response, 3, "Kv4fmHVAzwX", "Focus Name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        4,
        "CH6wamtY9kK",
        "Foci investigations performed & classified",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Njandama MCHP",
            "oRVt7g429ZO",
            "2019-08-21 13:29:14.578",
            "Village 32 Sarah Baartman",
            "1"));
    validateRow(
        response,
        1,
        List.of("Ngelehun CHC", "oRVt7g429ZO", "2019-08-21 13:29:21.574", "Focus A focus", "1"));
    validateRow(
        response,
        2,
        List.of(
            "Njandama MCHP",
            "oRVt7g429ZO",
            "2019-08-21 13:29:24.678",
            "Village 28 Oliver Tambo District",
            "1"));
    validateRow(
        response,
        3,
        List.of(
            "Ngelehun CHC",
            "oRVt7g429ZO",
            "2019-08-21 13:29:28.064",
            "Village 5 Alfred Nzo Municipality",
            "0"));
    validateRow(
        response,
        4,
        List.of("Njandama MCHP", "oRVt7g429ZO", "2019-08-21 13:29:31.708", "Village C", "1"));
    validateRow(
        response,
        5,
        List.of("Njandama MCHP", "oRVt7g429ZO", "2019-08-21 13:29:37.117", "Village B", "1"));
    validateRow(
        response,
        6,
        List.of(
            "Ngelehun CHC",
            "oRVt7g429ZO",
            "2019-08-21 13:29:39.311",
            "Village 17 Alfred Nzo Municipality",
            "0"));
    validateRow(
        response,
        7,
        List.of("Ngelehun CHC", "oRVt7g429ZO", "2019-08-21 13:29:44.942", "Test focus", "1"));
  }
}
