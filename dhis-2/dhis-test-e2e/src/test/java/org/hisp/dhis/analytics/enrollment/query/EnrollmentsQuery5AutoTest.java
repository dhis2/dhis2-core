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
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/enrollments/query" endpoint. */
public class EnrollmentsQuery5AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void financialYear2022Sep() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,GxdhnY5wmHq,lastupdated")
            .add("lastUpdated=2022Sep")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,GxdhnY5wmHq");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"2022Sep\":{\"name\":\"September 2022 - August 2023\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"GxdhnY5wmHq\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "GxdhnY5wmHq",
        "Average weight (g)",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        2,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
  }

  @Test
  public void programIndicatorWeightGainBetweenBirthAndLastPostnatal() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,lastupdated,qhTkqwAJLMv")
            .add("lastUpdated=LAST_6_MONTHS")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=10")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,qhTkqwAJLMv:EQ:NV")
            .add("desc=lastupdated")
            .add("relativePeriodDate=2019-02-01");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"LAST_6_MONTHS\":{\"name\":\"Last 6 months\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"qhTkqwAJLMv\":{\"uid\":\"qhTkqwAJLMv\",\"name\":\"Weight gain(in g) between birth and last postnatal\",\"description\":\"The average number of grams the baby has gained through the postnatal period. Only counted among the babies that has completed the postnatal period.\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"qhTkqwAJLMv\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        2,
        "qhTkqwAJLMv",
        "Weight gain(in g) between birth and last postnatal",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Elshadai Clinic", "2018-10-11 17:04:24.953", ""));
    validateRow(response, 1, List.of("Tonko Maternity Clinic", "2018-08-07 15:47:29.301", ""));
    validateRow(response, 2, List.of("Banka Makuloh MCHP", "2018-08-07 15:47:29.3", ""));
    validateRow(response, 3, List.of("Yonibana MCHP", "2018-08-07 15:47:29.299", ""));
    validateRow(response, 4, List.of("Kindoyal Hospital", "2018-08-07 15:47:29.298", ""));
    validateRow(response, 5, List.of("Baoma-Peje CHP", "2018-08-07 15:47:29.297", ""));
    validateRow(response, 6, List.of("Bontiwo MCHP", "2018-08-07 15:47:29.296", ""));
    validateRow(response, 7, List.of("Konia MCHP", "2018-08-07 15:47:29.295", ""));
    validateRow(response, 8, List.of("Kania MCHP", "2018-08-07 15:47:29.294", ""));
    validateRow(response, 9, List.of("Taninahun (Malen) CHP", "2018-08-07 15:47:29.293", ""));
  }

  @Test
  public void programIndicatorWomenWithLowHbVisitWithinOneYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,lastupdated,EDp0b79Rbx9,programstatus,enrollmentdate")
            .add("lastUpdated=LAST_12_MONTHS")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=EDp0b79Rbx9,ou:USER_ORGUNIT")
            .add("relativePeriodDate=2019-02-01")
            .add("desc=lastupdated");

    // When
    ApiResponse response = actions.query().get("WSGAb5XwJ3Y", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(2)))
        .body("height", equalTo(2))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"EDp0b79Rbx9\":{\"uid\":\"EDp0b79Rbx9\",\"name\":\"Number of women with at least one low hemoglobin visit within one year of the reporting period end\",\"description\":\"Counts the number of women enrolled in the reporting period, that has had at least one low-hemoglobin visit within the end of the reporting period.\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"PFDfvmGpsR3\":{\"uid\":\"PFDfvmGpsR3\",\"name\":\"Care at birth\",\"description\":\"Intrapartum care / Childbirth / Labour and delivery\"},\"bbKtnxRZKEP\":{\"uid\":\"bbKtnxRZKEP\",\"name\":\"Postpartum care visit\",\"description\":\"Provision of care for the mother for some weeks after delivery\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"PUZaKR0Jh2k\":{\"uid\":\"PUZaKR0Jh2k\",\"name\":\"Previous deliveries\",\"description\":\"Table for recording earlier deliveries\"},\"edqlbukwRfQ\":{\"uid\":\"edqlbukwRfQ\",\"name\":\"Second antenatal care visit\",\"description\":\"Antenatal care visit\"},\"WZbXY0S00lP\":{\"uid\":\"WZbXY0S00lP\",\"name\":\"First antenatal care visit\",\"description\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"}},\"dimensions\":{\"EDp0b79Rbx9\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        2,
        "EDp0b79Rbx9",
        "Number of women with at least one low hemoglobin visit within one year of the reporting period end",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response, 3, "programstatus", "Program status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        4,
        "enrollmentdate",
        "Date of first visit",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of("Ngelehun CHC", "2018-05-22 22:34:09.668", "0", "ACTIVE", "2023-10-14 02:00:00.0"));
    validateRow(
        response,
        1,
        List.of("Ngelehun CHC", "2018-02-20 18:47:35.335", "0", "ACTIVE", "2023-10-15 02:00:00.0"));
  }

  @Test
  public void programIndicatorMalariaCaseComplex() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=Ym2kI6k1I5e")
            .add("headers=ouname,lastupdated,Ym2kI6k1I5e")
            .add("lastUpdated=LAST_10_YEARS")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=Ym2kI6k1I5e:NE:NV:GE:76:LE:80,ou:USER_ORGUNIT")
            .add("relativePeriodDate=2025-01-20");

    // When
    ApiResponse response = actions.query().get("qDkgAbB5Jlk", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(2)))
        .body("height", equalTo(2))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"Ym2kI6k1I5e\":{\"uid\":\"Ym2kI6k1I5e\",\"name\":\"Malaria Case PI Complex\",\"description\":\"http://localhost:8080/dhis/api/42/analytics/enrollments/query/qDkgAbB5Jlk?dimension=Ym2kI6k1I5e:NE:NV:GE:76:LE:80,ou:USER_ORGUNIT&headers=ouname,lastupdated,Ym2kI6k1I5e&totalPages=false&rowContext=true&lastUpdated=LAST_10_YEARS&displayProperty=NAME&pageSize=100&page=1&includeMetadataDetails=true&outputType=ENROLLMENT&asc=Ym2kI6k1I5e\",\"legendSet\":\"TiOkbpGEud4\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"LAST_10_YEARS\":{\"name\":\"Last 10 years\"},\"qDkgAbB5Jlk\":{\"uid\":\"qDkgAbB5Jlk\",\"name\":\"Malaria case diagnosis, treatment and investigation\",\"description\":\"All cases in an elimination setting should be registered in this program. Includes relevant case identifiers/details including the ID, Name, Index, Age, Gender, Location,etc..\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"eHvTba5ijAh\":{\"uid\":\"eHvTba5ijAh\",\"name\":\"Case outcome\",\"description\":\"This stage details the final outcome of the case\"},\"wYTF0YCHMWr\":{\"uid\":\"wYTF0YCHMWr\",\"name\":\"Case investigation & classification\",\"description\":\"This includes the investigation of the index case (including the confirmation of symptoms, previous malaria history, LLIN usage details, IRS details), and the summary of the results for the case investigation including the final case classification (both the species type and the case classification). \"},\"hYyB7FUS5eR\":{\"uid\":\"hYyB7FUS5eR\",\"name\":\"Diagnosis & treatment\",\"description\":\"This stage is used to identify initial diagnosis and treatment. This includes the method of case detection, information about the case include travel history, method of diagnosis, malaria species type and treatment details. \"},\"C0aLZo75dgJ\":{\"uid\":\"C0aLZo75dgJ\",\"name\":\"Household investigation\",\"description\":\"Nearby household investigations occur when an index case is identified within a specific geographical area.\"}},\"dimensions\":{\"Ym2kI6k1I5e\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        2,
        "Ym2kI6k1I5e",
        "Malaria Case PI Complex",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Ngelehun CHC", "2019-08-21 13:29:58.317", "77.0"));
    validateRow(response, 1, List.of("Ngelehun CHC", "2019-08-21 13:30:13.73", "78.0"));
  }

  @Test
  public void programIndicatorTBProgramComplex() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=tzjB2D6SvQZ")
            .add("headers=ouname,tzjB2D6SvQZ,lastupdated")
            .add("lastUpdated=LAST_10_YEARS")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,tzjB2D6SvQZ:GT:404:NE:NV")
            .add("relativePeriodDate=2025-01-20");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"LAST_10_YEARS\":{\"name\":\"Last 10 years\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"tzjB2D6SvQZ\":{\"uid\":\"tzjB2D6SvQZ\",\"name\":\"TB PI Complex\",\"description\":\"http://localhost:8080/dhis/api/42/analytics/enrollments/query/ur1Edk5Oe2n?dimension=ou:USER_ORGUNIT,tzjB2D6SvQZ:GT:404:NE:NV&headers=ouname,tzjB2D6SvQZ,lastupdated&totalPages=false&rowContext=true&lastUpdated=LAST_10_YEARS&displayProperty=NAME&pageSize=100&page=1&includeMetadataDetails=true&outputType=ENROLLMENT&asc=tzjB2D6SvQZ\",\"legendSet\":\"fqs276KXCXi\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"}},\"dimensions\":{\"tzjB2D6SvQZ\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "tzjB2D6SvQZ", "TB PI Complex", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response,
        2,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Motorbong MCHP", "405.34", "2017-03-28 12:27:49.148"));
    validateRow(response, 1, List.of("Ngelehun CHC", "426.34", "2017-03-28 12:35:24.047"));
    validateRow(response, 2, List.of("Ngelehun CHC", "462.24", "2017-03-28 12:34:17.868"));
  }

  @Test
  public void programIndicatorChildProgramComplex() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=ouname,lastupdated,qZOBw051LSf,programstatus")
            .add("displayProperty=NAME")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("relativePeriodDate=2025-01-20")
            .add("includeMetadataDetails=true")
            .add("lastUpdated=LAST_10_YEARS")
            .add("totalPages=false")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,qZOBw051LSf")
            .add("programStatus=COMPLETED")
            .add("desc=lastupdated");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"LAST_10_YEARS\":{\"name\":\"Last 10 years\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"qZOBw051LSf\":{\"uid\":\"qZOBw051LSf\",\"name\":\"Child PI Complex\",\"legendSet\":\"Yf6UHoPkdS6\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"qZOBw051LSf\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response, 2, "qZOBw051LSf", "Child PI Complex", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 3, "programstatus", "Program status", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(
        response, 0, List.of("Ngelehun CHC", "2018-01-20 10:44:33.776", "0.98", "COMPLETED"));
    validateRow(
        response, 1, List.of("Ngelehun CHC", "2018-01-20 10:42:54.423", "0.98", "COMPLETED"));
    validateRow(
        response, 2, List.of("Ngelehun CHC", "2018-01-20 10:41:01.537", "0.98", "COMPLETED"));
    validateRow(
        response, 3, List.of("Ngelehun CHC", "2017-11-16 12:54:26.958", "0.98", "COMPLETED"));
    validateRow(
        response, 4, List.of("Ngelehun CHC", "2017-11-16 12:26:42.851", "0.98", "COMPLETED"));
  }
}
