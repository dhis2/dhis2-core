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
package org.hisp.dhis.analytics.event.query;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/query" endpoint. */
public class EventsQuery4AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void financialYear2023Sep() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,GxdhnY5wmHq,lastupdated")
            .add("lastUpdated=2023Sep")
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("outputType=EVENT")
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
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"2023Sep\":{\"name\":\"September 2023 - August 2024\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"GxdhnY5wmHq\":{\"uid\":\"GxdhnY5wmHq\",\"name\":\"Average weight (g)\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"GxdhnY5wmHq\":[]}}";
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
  public void programIndicatorMalariaCaseMicroscopyPv514YearsFemale() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,TEmaOXaSPe7")
            .add("stage=hYyB7FUS5eR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=MONTHS_THIS_YEAR")
            .add("pageSize=10")
            .add("outputType=EVENT")
            .add("page=1")
            .add("dimension=TEmaOXaSPe7,ou:USER_ORGUNIT")
            .add("relativePeriodDate=2022-07-01")
            .add("desc=lastupdated");

    // When
    ApiResponse response = actions.query().get("qDkgAbB5Jlk", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(10)))
        .body("height", equalTo(10))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"TEmaOXaSPe7\":{\"uid\":\"TEmaOXaSPe7\",\"name\":\"Malaria case - microscopy Pv. 5-14 years female\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk\":{\"uid\":\"qDkgAbB5Jlk\",\"name\":\"Malaria case diagnosis, treatment and investigation\",\"description\":\"All cases in an elimination setting should be registered in this program. Includes relevant case identifiers/details including the ID, Name, Index, Age, Gender, Location,etc..\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"MONTHS_THIS_YEAR\":{\"name\":\"Months this year\"},\"hYyB7FUS5eR\":{\"uid\":\"hYyB7FUS5eR\",\"name\":\"Diagnosis & treatment\",\"description\":\"This stage is used to identify initial diagnosis and treatment. This includes the method of case detection, information about the case include travel history, method of diagnosis, malaria species type and treatment details. \"}},\"dimensions\":{\"TEmaOXaSPe7\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "TEmaOXaSPe7",
        "Malaria case - microscopy Pv. 5-14 years female",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Ngelehun CHC", "1"));
    validateRow(response, 1, List.of("Ngelehun CHC", "0"));
    validateRow(response, 2, List.of("Ngelehun CHC", "0"));
    validateRow(response, 3, List.of("Ngelehun CHC", "1"));
    validateRow(response, 4, List.of("Ngelehun CHC", "0"));
    validateRow(response, 5, List.of("Ngelehun CHC", "0"));
    validateRow(response, 6, List.of("Ngelehun CHC", "0"));
    validateRow(response, 7, List.of("Ngelehun CHC", "0"));
    validateRow(response, 8, List.of("Ngelehun CHC", "0"));
    validateRow(response, 9, List.of("Ngelehun CHC", "0"));
  }

  @Test
  public void programIndicatorNumberOfOrgUnitsWithOngoingArv() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,rxNjqzJ7dkK,lastupdated")
            .add("lastUpdated=LAST_YEAR")
            .add("stage=PUZaKR0Jh2k")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,rxNjqzJ7dkK:GE:0")
            .add("relativePeriodDate=2019-07-01")
            .add("desc=lastupdated");

    // When
    ApiResponse response = actions.query().get("WSGAb5XwJ3Y", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"PUZaKR0Jh2k\":{\"uid\":\"PUZaKR0Jh2k\",\"name\":\"Previous deliveries\",\"description\":\"Table for recording earlier deliveries\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"rxNjqzJ7dkK\":{\"uid\":\"rxNjqzJ7dkK\",\"name\":\"Number of organisation units with ongoing arv treatment\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"LAST_YEAR\":{\"name\":\"Last year\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"rxNjqzJ7dkK\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "rxNjqzJ7dkK",
        "Number of organisation units with ongoing arv treatment",
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
    validateRow(response, 0, List.of("Ngelehun CHC", "0", "2018-10-14 14:14:36.922"));
    validateRow(response, 1, List.of("Kondewakoro CHP", "0", "2018-01-26 13:48:13.368"));
    validateRow(response, 2, List.of("Mathen MCHP", "0", "2018-01-26 13:48:13.366"));
    validateRow(response, 3, List.of("Mbokie CHP", "0", "2018-01-26 13:48:13.363"));
    validateRow(response, 4, List.of("Grey Bush CHC", "0", "2018-01-26 13:48:13.359"));
    validateRow(response, 5, List.of("Bangoma MCHP", "0", "2018-01-26 13:48:13.353"));
    validateRow(response, 6, List.of("Blessed Mokaka East Clinic", "0", "2018-01-26 13:48:13.351"));
    validateRow(response, 7, List.of("MCH Static/U5", "0", "2018-01-26 13:48:13.349"));
    validateRow(response, 8, List.of("Konjo MCHP", "0", "2018-01-26 13:48:13.345"));
    validateRow(response, 9, List.of("Lakka/Ogoo Farm CHC", "0", "2018-01-26 13:48:13.341"));
  }

  @Test
  public void programIndicatorBMIFemaleUnder5y() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,lastupdated,hlPt8H4bUOQ")
            .add("lastUpdated=LAST_12_MONTHS")
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=5")
            .add("outputType=EVENT")
            .add("page=1")
            .add("dimension=hlPt8H4bUOQ,ou:USER_ORGUNIT")
            .add("relativePeriodDate=2019-02-01")
            .add("desc=lastupdated");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":5,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"hlPt8H4bUOQ\":{\"uid\":\"hlPt8H4bUOQ\",\"name\":\"BMI female under 5 y\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"eBAyeGv0exc\":{\"uid\":\"eBAyeGv0exc\",\"name\":\"Inpatient morbidity and mortality\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"Zj7UnCAulEk\":{\"uid\":\"Zj7UnCAulEk\",\"name\":\"Inpatient morbidity and mortality\",\"description\":\"Anonymous and ICD-10 coded inpatient data\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"}},\"dimensions\":{\"hlPt8H4bUOQ\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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
        "hlPt8H4bUOQ",
        "BMI female under 5 y",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Ngelehun CHC", "2018-08-04 15:19:36.473", ""));
    validateRow(response, 1, List.of("Ngelehun CHC", "2018-08-04 15:18:06.085", ""));
    validateRow(response, 2, List.of("Ngelehun CHC", "2018-08-04 15:17:39.87", ""));
    validateRow(response, 3, List.of("Ngelehun CHC", "2018-08-04 15:17:10.722", ""));
    validateRow(response, 4, List.of("Ngelehun CHC", "2018-08-04 15:15:50.672", ""));
  }

  @Test
  public void programIndicatorChildProgramComplex() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=ouname,qZOBw051LSf,lastupdated,eventstatus")
            .add("displayProperty=NAME")
            .add("pageSize=100")
            .add("outputType=EVENT")
            .add("relativePeriodDate=2025-01-20")
            .add("includeMetadataDetails=true")
            .add("asc=lastupdated")
            .add("lastUpdated=LAST_10_YEARS")
            .add("stage=A03MvHHogjR")
            .add("eventStatus=SCHEDULE")
            .add("totalPages=false")
            .add("page=1")
            .add("dimension=ou:USER_ORGUNIT,qZOBw051LSf");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"LAST_10_YEARS\":{\"name\":\"Last 10 years\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"qZOBw051LSf\":{\"uid\":\"qZOBw051LSf\",\"name\":\"Child PI Complex\",\"legendSet\":\"Yf6UHoPkdS6\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"qZOBw051LSf\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "qZOBw051LSf", "Child PI Complex", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response,
        2,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response, 3, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(
        response, 0, List.of("Ngelehun CHC", "18527.66", "2017-07-23 12:46:11.472", "SCHEDULE"));
    validateRow(
        response, 1, List.of("Ngelehun CHC", "18527.66", "2017-11-15 17:55:06.66", "SCHEDULE"));
    validateRow(
        response, 2, List.of("Ngelehun CHC", "18527.66", "2018-09-14 20:05:43.971", "SCHEDULE"));
  }

  @Test
  public void eventsWithDataElementTypeCoordinates() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:2022")
            .add("coordinatesOnly=true")
            .add("stage=Zj7UnCAulEk")
            .add("coordinateField=F3ogKBuviRA")
            .add("dimension=ou:ImspTQPwCqd");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(4)))
        .body("height", equalTo(4))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":4,\"pageSize\":50,\"pageCount\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{\"name\":\"Organisation unit\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        2,
        "eventdate",
        "Report date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(response, 3, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 4, "createdbydisplayname", "Created by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        5,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        6,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        7,
        "scheduleddate",
        "Scheduled date",
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
        response, 15, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 16, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "vexvMPiL7fL",
            "Zj7UnCAulEk",
            "2022-01-05 00:00:00.0",
            "",
            ",  ()",
            ",  ()",
            "2018-01-15 18:15:59.488",
            "",
            "{\"type\":\"Point\",\"coordinates\":[-12.92643,8.431033]}",
            "-12.91769",
            "8.42347",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "ACTIVE",
            "DiszpKrYNg8"));
    validateRow(
        response,
        1,
        List.of(
            "ldLR9NLHI5w",
            "Zj7UnCAulEk",
            "2022-08-01 00:00:00.0",
            "system",
            ",  ()",
            ",  ()",
            "2018-08-04 15:15:50.672",
            "2022-08-04 15:15:50.671",
            "{\"type\":\"Point\",\"coordinates\":[-12.182307,8.244543]}",
            "-12.164612",
            "8.24411",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "ACTIVE",
            "DiszpKrYNg8"));
    validateRow(
        response,
        2,
        List.of(
            "aTL7SjJycjA",
            "Zj7UnCAulEk",
            "2022-08-02 00:00:00.0",
            "system",
            ",  ()",
            ",  ()",
            "2018-08-04 15:17:10.722",
            "2022-08-04 15:17:10.722",
            "{\"type\":\"Point\",\"coordinates\":[-12.38522,8.214034]}",
            "-12.384338",
            "8.200616",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "ACTIVE",
            "DiszpKrYNg8"));
    validateRow(
        response,
        3,
        List.of(
            "n1V0AKAOgj2",
            "Zj7UnCAulEk",
            "2022-08-01 00:00:00.0",
            "",
            ",  ()",
            ",  ()",
            "2018-08-04 15:17:39.87",
            "2022-08-04 15:17:39.821",
            "{\"type\":\"Point\",\"coordinates\":[-12.267675,8.200319]}",
            "-12.262115",
            "8.195179",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "OU_559",
            "ACTIVE",
            "ACTIVE",
            "DiszpKrYNg8"));
  }
}
