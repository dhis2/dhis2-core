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
            .add("relativePeriodDate=2023-07-14")
            .add("desc=enrollmentdate");

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
            "Kaliyereh MCHP",
            "",
            "B+",
            "Female",
            "1Z 691 559 12 4328 671 5",
            "64.0",
            "2021-12-31 12:43:24.911",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        1,
        List.of(
            "Gbongongor CHP",
            "",
            "O+",
            "Female",
            "1Z 720 908 25 4308 140 9",
            "71.3",
            "2021-12-31 12:43:01.209",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        2,
        List.of(
            "kamaron CHP",
            "",
            "B+",
            "Female",
            "1Z 617 684 42 3139 945 8",
            "77.2",
            "2021-12-31 12:42:30.307",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        3,
        List.of(
            "The White House Clinic",
            "",
            "O-",
            "Female",
            "1Z 432 343 69 4629 181 2",
            "83.9",
            "2021-12-31 12:41:25.538",
            "J40PpdN4Wkk",
            ""));
    validateRow(
        response,
        4,
        List.of(
            "Suga MCHP",
            "",
            "B+",
            "Female",
            "1Z 4Y5 146 64 0804 232 1",
            "53.5",
            "2021-12-31 12:40:01.122",
            "J40PpdN4Wkk",
            ""));
    validateRow(
        response,
        5,
        List.of(
            "Rokimbi MCHP",
            "",
            "O+",
            "Female",
            "1Z 028 228 09 9075 349 3",
            "88.0",
            "2021-12-31 12:37:56.563",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        6,
        List.of(
            "Maselleh MCHP",
            "",
            "A+",
            "Female",
            "1Z 110 E97 08 4023 800 6",
            "72.5",
            "2021-12-31 12:37:54.169",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        7,
        List.of(
            "Gandorhun (Gbane) CHC",
            "",
            "O-",
            "Female",
            "1Z 464 593 73 6723 159 7",
            "59.8",
            "2021-12-31 12:37:53.613",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        8,
        List.of(
            "Gondama (Nimikoro) MCHP",
            "",
            "O+",
            "Female",
            "1Z 018 083 81 3354 738 4",
            "69.7",
            "2021-12-31 12:37:10.725",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        9,
        List.of(
            "Massabendu CHP",
            "",
            "AB+",
            "Female",
            "1Z 84E 60V 20 8672 192 7",
            "51.1",
            "2021-12-31 12:36:07.694",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        10,
        List.of(
            "Kortohun CHP",
            "",
            "A+",
            "Female",
            "1Z 206 7F0 75 4085 314 4",
            "96.8",
            "2021-12-31 12:36:05.22",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        11,
        List.of(
            "Kakoya MCHP",
            "",
            "A+",
            "Female",
            "1Z 687 32A 60 0622 128 0",
            "72.3",
            "2021-12-31 12:36:03.962",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        12,
        List.of(
            "Macrogba MCHP",
            "",
            "A+",
            "Female",
            "1Z 545 4Y0 31 0102 358 4",
            "99.4",
            "2021-12-31 12:32:48.595",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        13,
        List.of(
            "Catholic Clinic",
            "",
            "A+",
            "Female",
            "1Z 02Y 611 05 9088 649 3",
            "69.9",
            "2021-12-31 12:32:22.33",
            "J40PpdN4Wkk",
            ""));
    validateRow(
        response,
        14,
        List.of(
            "Robat MCHP",
            "",
            "O+",
            "Female",
            "1Z 22F V08 27 5089 544 4",
            "85.8",
            "2021-12-31 12:32:11.939",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        15,
        List.of(
            "Bangambaya MCHP",
            "",
            "O+",
            "Female",
            "1Z 97Y 499 60 1677 285 3",
            "100.0",
            "2021-12-31 12:31:57.558",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        16,
        List.of(
            "Tombodu CHC",
            "",
            "A+",
            "Female",
            "1Z 215 127 10 7992 978 2",
            "93.3",
            "2021-12-31 12:31:00.706",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        17,
        List.of(
            "Sumbaria MCHP",
            "",
            "O+",
            "Female",
            "1Z 1Y3 947 45 7856 880 1",
            "57.4",
            "2021-12-31 12:31:00.302",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        18,
        List.of(
            "Peyima CHP",
            "",
            "A+",
            "Female",
            "1Z 787 0F2 41 7585 810 5",
            "92.0",
            "2021-12-30 12:42:36.052",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        19,
        List.of(
            "Yele CHC",
            "",
            "O+",
            "Female",
            "1Z 176 429 51 6227 894 5",
            "83.3",
            "2021-12-30 12:40:31.735",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        20,
        List.of(
            "Ninkikoro MCHP",
            "",
            "A+",
            "Female",
            "1Z F28 W61 33 9488 119 1",
            "48.4",
            "2021-12-30 12:40:13.252",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        21,
        List.of(
            "kamaron MCHP",
            "",
            "B+",
            "Female",
            "1Z 366 8V8 44 3157 342 4",
            "76.4",
            "2021-12-30 12:39:46.442",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        22,
        List.of(
            "Bumbukoro MCHP",
            "",
            "A+",
            "Female",
            "1Z E10 378 35 4334 485 2",
            "50.5",
            "2021-12-30 12:39:09.642",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        23,
        List.of(
            "Bandaperie CHP",
            "",
            "B+",
            "Female",
            "1Z 219 Y55 04 5490 846 0",
            "97.8",
            "2021-12-30 12:38:26.021",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        24,
        List.of(
            "Alkalia CHP",
            "",
            "A+",
            "Female",
            "1Z 965 325 66 1362 459 9",
            "83.1",
            "2021-12-30 12:37:23.332",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        25,
        List.of(
            "Gberifeh MCHP",
            "",
            "O+",
            "Female",
            "1Z 545 463 05 1194 170 3",
            "61.9",
            "2021-12-30 12:35:57.915",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        26,
        List.of(
            "Falaba CHC",
            "",
            "AB+",
            "Female",
            "1Z 784 616 83 3425 162 4",
            "75.8",
            "2021-12-30 12:35:20.653",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        27,
        List.of(
            "Gandorhun (Gbane) CHC",
            "",
            "A-",
            "Female",
            "1Z W13 376 71 8167 042 0",
            "93.3",
            "2021-12-30 12:35:07.846",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        28,
        List.of(
            "Handicap Clinic",
            "",
            "O+",
            "Female",
            "1Z 446 3F8 00 9146 024 2",
            "79.3",
            "2021-12-30 12:35:05.261",
            "J40PpdN4Wkk",
            ""));
    validateRow(
        response,
        29,
        List.of(
            "Sanya CHP",
            "",
            "O+",
            "Female",
            "1Z A10 931 85 8029 336 6",
            "49.5",
            "2021-12-30 12:34:38.297",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        30,
        List.of(
            "Mansadu MCHP",
            "",
            "O+",
            "Female",
            "1Z 3Y9 0A7 00 8391 602 5",
            "74.2",
            "2021-12-30 12:32:33.173",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        31,
        List.of(
            "Gbonkonka CHP",
            "",
            "O-",
            "Female",
            "1Z 279 576 90 8563 052 8",
            "65.5",
            "2021-12-30 12:31:19.004",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        32,
        List.of(
            "Magbaikoli MCHP",
            "",
            "A+",
            "Female",
            "1Z Y04 093 07 6881 820 6",
            "74.2",
            "2021-12-30 12:31:00.257",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        33,
        List.of(
            "Tonko Maternity Clinic",
            "",
            "O+",
            "Female",
            "1Z 509 30Y 48 1031 255 8",
            "93.3",
            "2021-12-30 12:29:47.023",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        34,
        List.of(
            "Mansadu MCHP",
            "",
            "O+",
            "Female",
            "1Z 8E3 0A6 48 6790 974 9",
            "72.5",
            "2021-12-30 12:29:30.946",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        35,
        List.of(
            "Mangay Loko MCHP",
            "",
            "O+",
            "Female",
            "1Z 1Y7 05E 07 8487 596 4",
            "52.9",
            "2021-12-29 12:42:52.753",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        36,
        List.of(
            "Firawa CHC",
            "",
            "B+",
            "Female",
            "1Z 349 304 76 1559 019 6",
            "76.4",
            "2021-12-29 12:42:24.555",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        37,
        List.of(
            "Mansadu MCHP",
            "",
            "A+",
            "Female",
            "1Z F88 589 40 9498 717 0",
            "54.5",
            "2021-12-29 12:39:47.347",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        38,
        List.of(
            "Bandusuma MCHP",
            "",
            "O-",
            "Female",
            "1Z 468 5Y3 10 1076 986 3",
            "62.1",
            "2021-12-29 12:38:53.132",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        39,
        List.of(
            "Bendugu CHC",
            "",
            "O+",
            "Female",
            "1Z 297 477 81 0905 285 6",
            "71.8",
            "2021-12-29 12:37:42.81",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        40,
        List.of(
            "Komrabai Station MCHP",
            "",
            "A+",
            "Female",
            "1Z 543 712 58 2846 627 5",
            "85.4",
            "2021-12-29 12:37:38.48",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        41,
        List.of(
            "Ngaiya MCHP",
            "",
            "B+",
            "Female",
            "1Z 801 893 22 8074 630 7",
            "67.8",
            "2021-12-29 12:37:03.203",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        42,
        List.of(
            "Tombodu CHC",
            "",
            "O+",
            "Female",
            "1Z 3F8 57Y 21 0322 266 6",
            "60.8",
            "2021-12-29 12:36:48.965",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        43,
        List.of(
            "Kerefay Loko MCHP",
            "",
            "A+",
            "Female",
            "1Z W89 589 47 3823 879 8",
            "72.4",
            "2021-12-29 12:36:44.067",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        44,
        List.of(
            "Kathanta Bana MCHP",
            "",
            "A+",
            "Female",
            "1Z 4A6 931 00 1516 640 3",
            "83.8",
            "2021-12-29 12:34:58.388",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        45,
        List.of(
            "Mangay Loko MCHP",
            "",
            "B+",
            "Female",
            "1Z 79W 7A6 33 1282 995 2",
            "103.2",
            "2021-12-29 12:34:13.124",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        46,
        List.of(
            "Kurubonla CHC",
            "",
            "O+",
            "Female",
            "1Z 533 511 37 3145 056 3",
            "87.0",
            "2021-12-29 12:32:16.352",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        47,
        List.of(
            "Sandia CHP",
            "",
            "A-",
            "Female",
            "1Z 8A8 63W 23 8362 796 7",
            "54.1",
            "2021-12-29 12:32:14.946",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        48,
        List.of(
            "Gondama (Nimikoro) MCHP",
            "",
            "A+",
            "Female",
            "1Z 723 7E3 38 0273 001 9",
            "50.1",
            "2021-12-29 12:31:38.681",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        49,
        List.of(
            "Sukudu MCHP",
            "",
            "B+",
            "Female",
            "1Z 882 083 60 5391 019 3",
            "85.3",
            "2021-12-29 12:29:59.194",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        50,
        List.of(
            "Kakoya MCHP",
            "",
            "A-",
            "Female",
            "1Z 353 230 07 1587 081 8",
            "79.5",
            "2021-12-28 12:42:24.365",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        51,
        List.of(
            "Serekolia MCHP",
            "",
            "AB+",
            "Female",
            "1Z 202 728 72 9272 091 0",
            "75.4",
            "2021-12-28 12:42:18.545",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        52,
        List.of(
            "Rorocks CHP",
            "",
            "O+",
            "Female",
            "1Z 804 A57 20 0458 217 0",
            "77.9",
            "2021-12-28 12:42:18.516",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        53,
        List.of(
            "Kagbere CHC",
            "",
            "A+",
            "Female",
            "1Z 9A2 3A2 58 6386 718 5",
            "53.7",
            "2021-12-28 12:41:32.242",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        54,
        List.of(
            "sonkoya MCHP",
            "",
            "A+",
            "Female",
            "1Z 687 589 72 8693 793 8",
            "102.5",
            "2021-12-28 12:41:06.216",
            "J40PpdN4Wkk",
            ""));
    validateRow(
        response,
        55,
        List.of(
            "Masoko MCHP",
            "",
            "O+",
            "Female",
            "1Z 944 442 20 3138 011 6",
            "101.8",
            "2021-12-28 12:40:19.241",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        56,
        List.of(
            "Masongbo CHC",
            "",
            "O-",
            "Female",
            "1Z 988 4Y2 12 8255 933 0",
            "56.8",
            "2021-12-28 12:39:10.962",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        57,
        List.of(
            "Gbamandu MCHP",
            "",
            "O+",
            "Female",
            "1Z 381 47W 56 0076 846 8",
            "68.9",
            "2021-12-28 12:29:53.434",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        58,
        List.of(
            "Ngo Town CHP",
            "",
            "O-",
            "Female",
            "1Z A50 986 15 8182 952 3",
            "78.0",
            "2021-12-28 12:29:04.361",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        59,
        List.of(
            "Karina MCHP",
            "",
            "A+",
            "Female",
            "1Z 32Y 339 45 7531 442 0",
            "92.0",
            "2021-12-28 12:28:39.647",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        60,
        List.of(
            "EM&BEE Maternity Home Clinic",
            "",
            "A+",
            "Female",
            "1Z 330 717 76 1308 906 5",
            "62.4",
            "2021-12-28 12:28:38.774",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        61,
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
        62,
        List.of(
            "Kabombeh MCHP",
            "",
            "O+",
            "Female",
            "1Z 65E 170 58 8065 919 4",
            "73.4",
            "2021-12-27 12:43:45.08",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        63,
        List.of(
            "Mansundu (Sandor) MCHP",
            "",
            "A+",
            "Female",
            "1Z 157 A39 50 7388 550 5",
            "81.5",
            "2021-12-27 12:42:33.024",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        64,
        List.of(
            "Red Cross Clinic",
            "",
            "O+",
            "Female",
            "1Z 579 476 14 1849 889 3",
            "55.2",
            "2021-12-27 12:39:22.495",
            "J40PpdN4Wkk",
            ""));
    validateRow(
        response,
        65,
        List.of(
            "Bumbuna CHC",
            "",
            "O+",
            "Female",
            "1Z 309 92Y 24 6273 033 7",
            "56.4",
            "2021-12-27 12:39:15.027",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        66,
        List.of(
            "Makaiba MCHP",
            "",
            "B+",
            "Female",
            "1Z 975 8Y2 93 5002 083 1",
            "59.1",
            "2021-12-27 12:39:11.096",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        67,
        List.of(
            "Makump Bana MCHP",
            "",
            "A-",
            "Female",
            "1Z 2Y2 F59 75 8103 803 6",
            "80.9",
            "2021-12-27 12:37:13.508",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        68,
        List.of(
            "Manewa MCHP",
            "",
            "A+",
            "Female",
            "1Z 975 W83 45 8903 937 6",
            "51.2",
            "2021-12-27 12:36:48.139",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        69,
        List.of(
            "Dankawalia MCHP",
            "",
            "O+",
            "Female",
            "1Z 011 462 34 1965 523 3",
            "56.9",
            "2021-12-27 12:35:24.811",
            "J40PpdN4Wkk",
            ""));
    validateRow(
        response,
        70,
        List.of(
            "Makali CHC",
            "",
            "B+",
            "Female",
            "1Z 2F8 409 71 6184 093 4",
            "62.4",
            "2021-12-27 12:35:15.182",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        71,
        List.of(
            "Kerefay Loko MCHP",
            "",
            "A+",
            "Female",
            "1Z 492 738 62 4224 378 6",
            "83.1",
            "2021-12-27 12:35:09.917",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        72,
        List.of(
            "Kolisokor MCHP",
            "",
            "A+",
            "Female",
            "1Z 5Y1 678 08 9396 347 2",
            "97.8",
            "2021-12-27 12:34:36.548",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        73,
        List.of(
            "Massaba MCHP",
            "",
            "O+",
            "Female",
            "1Z 3V4 108 92 4611 376 4",
            "76.2",
            "2021-12-27 12:34:32.672",
            "J40PpdN4Wkk",
            ""));
    validateRow(
        response,
        74,
        List.of(
            "Sumbaria MCHP",
            "",
            "O+",
            "Female",
            "1Z 533 48E 89 7731 460 7",
            "60.3",
            "2021-12-27 12:34:14.812",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        75,
        List.of(
            "Tongorma MCHP",
            "",
            "O+",
            "Female",
            "1Z 165 43V 52 1963 634 5",
            "56.1",
            "2021-12-27 12:33:37.26",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        76,
        List.of(
            "Yataya CHP",
            "",
            "O+",
            "Female",
            "1Z 671 544 20 6046 774 1",
            "98.3",
            "2021-12-27 12:32:48.166",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        77,
        List.of(
            "Masofinia MCHP",
            "",
            "A+",
            "Female",
            "1Z F37 694 55 4840 549 6",
            "85.9",
            "2021-12-27 12:32:30.04",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        78,
        List.of(
            "Kumala CHP",
            "",
            "B+",
            "Female",
            "1Z 514 683 85 2108 708 0",
            "95.0",
            "2021-12-27 12:32:25.424",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        79,
        List.of(
            "Senekedugu MCHP",
            "",
            "O+",
            "Female",
            "1Z A19 21A 45 3811 811 4",
            "85.0",
            "2021-12-27 12:30:35.116",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        80,
        List.of(
            "Gbetema MCHP (Fiama)",
            "",
            "B-",
            "Female",
            "1Z Y20 069 90 2965 251 9",
            "76.9",
            "2021-12-27 12:28:45.019",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        81,
        List.of(
            "Boroma MCHP",
            "",
            "O-",
            "Female",
            "1Z 729 E78 97 8135 225 3",
            "98.7",
            "2021-12-26 12:40:56.749",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        82,
        List.of(
            "Makarie MCHP",
            "",
            "O+",
            "Female",
            "1Z 3A2 469 48 1463 579 5",
            "82.3",
            "2021-12-26 12:40:24.838",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        83,
        List.of(
            "Gberia Timbakor MCHP",
            "",
            "A+",
            "Female",
            "1Z 179 828 28 7298 366 1",
            "56.2",
            "2021-12-26 12:39:43.386",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        84,
        List.of(
            "Kaimunday CHP",
            "",
            "O+",
            "Female",
            "1Z 9Y5 793 02 6146 748 9",
            "90.9",
            "2021-12-26 12:39:28.251",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        85,
        List.of(
            "Rogbin MCHP",
            "",
            "A+",
            "Female",
            "1Z 30V 008 16 3919 470 4",
            "76.8",
            "2021-12-26 12:39:07.618",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        86,
        List.of(
            "Quidadu MCHP",
            "",
            "O+",
            "Female",
            "1Z Y50 1V8 41 1771 622 8",
            "90.4",
            "2021-12-26 12:38:29.204",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        87,
        List.of(
            "Walia MCHP",
            "",
            "AB+",
            "Female",
            "1Z 80E 703 30 8208 433 4",
            "81.2",
            "2021-12-26 12:37:33.111",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        88,
        List.of(
            "Manjama MCHP",
            "",
            "A+",
            "Female",
            "1Z 022 527 99 8058 632 0",
            "55.9",
            "2021-12-26 12:37:16.837",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        89,
        List.of(
            "Makaiba MCHP",
            "",
            "A+",
            "Female",
            "1Z 981 170 41 6884 722 5",
            "80.6",
            "2021-12-26 12:35:20.732",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        90,
        List.of(
            "Kondembaia CHC",
            "",
            "O+",
            "Female",
            "1Z 090 774 48 3482 093 8",
            "96.1",
            "2021-12-26 12:35:15.688",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        91,
        List.of(
            "Kangama (Kangama) CHP",
            "",
            "O+",
            "Female",
            "1Z 7E1 5Y7 57 2604 131 0",
            "69.4",
            "2021-12-26 12:33:48.326",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        92,
        List.of(
            "Gbetema MCHP (Fiama)",
            "",
            "A+",
            "Female",
            "1Z 840 190 36 9524 102 8",
            "105.5",
            "2021-12-26 12:33:07.063",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        93,
        List.of(
            "Gbangadu MCHP",
            "",
            "A+",
            "Female",
            "1Z 056 5F0 32 3585 958 0",
            "53.6",
            "2021-12-26 12:32:47.273",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        94,
        List.of(
            "Kondeya MCHP",
            "",
            "B+",
            "Female",
            "1Z 885 F36 71 8653 773 4",
            "91.0",
            "2021-12-26 12:31:28.067",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        95,
        List.of(
            "Fullah Town (M.Gbanti) MCHP",
            "",
            "B+",
            "Female",
            "1Z 135 54Y 63 3420 354 3",
            "101.7",
            "2021-12-26 12:31:05.154",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        96,
        List.of(
            "Kolisokor MCHP",
            "",
            "O+",
            "Female",
            "1Z W02 738 65 1045 239 0",
            "74.6",
            "2021-12-26 12:29:03.317",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        97,
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
        98,
        List.of(
            "Rochem Kamandao CHP",
            "",
            "A+",
            "Female",
            "1Z 262 077 34 7074 780 5",
            "87.8",
            "2021-12-25 12:42:53.5",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
    validateRow(
        response,
        99,
        List.of(
            "Nasarah Clinic",
            "",
            "O+",
            "Female",
            "1Z 190 488 14 1653 627 4",
            "71.9",
            "2021-12-25 12:42:49.145",
            "J40PpdN4Wkk",
            "oRVt7g429ZO"));
  }

  @Test
  public void queryRandom2() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "headers=ouname,spFvx9FndA4,H9IlTX2X6SL,cejWyOfXge6,ruQQnf6rswq,OvY4VVhSDeJ,Bpx0589u8y0,lastupdated")
            .add("displayProperty=NAME")
            .add("enrollmentDate=2021")
            .add("outputType=ENROLLMENT")
            .add("pageSize=100")
            .add("relativePeriodDate=2023-07-14")
            .add("filter=uIuxlbV1vRT:J40PpdN4Wkk")
            .add("includeMetadataDetails=true")
            .add("asc=lastupdated")
            .add("totalPages=false")
            .add("page=1")
            .add(
                "dimension=ou:ImspTQPwCqd,spFvx9FndA4,H9IlTX2X6SL,cejWyOfXge6:IN:Female,ruQQnf6rswq,OvY4VVhSDeJ,Bpx0589u8y0")
            .add("desc=enrollmentdate");

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
