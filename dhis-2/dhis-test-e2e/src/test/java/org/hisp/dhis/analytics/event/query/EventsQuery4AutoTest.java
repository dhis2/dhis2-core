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
package org.hisp.dhis.analytics.event.query;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.*;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:2022")
            .add("coordinatesOnly=true")
            .add("stage=Zj7UnCAulEk")
            .add("coordinateField=F3ogKBuviRA")
            .add("dimension=ou:ImspTQPwCqd")
            .add("desc=lastupdated");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(expectPostgis ? 18 : 14)))
        .body("rows", hasSize(equalTo(4)))
        .body("height", equalTo(4))
        .body("width", equalTo(expectPostgis ? 18 : 14))
        .body("headerWidth", equalTo(expectPostgis ? 18 : 14));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":4,\"pageSize\":50,\"pageCount\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"ou\":{\"name\":\"Organisation unit\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    List<Map<String, Object>> actualHeaders = getHeadersFromResponse(response);

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
    if (expectPostgis) {
      validateHeader(response, 8, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
      validateHeader(
          response,
          9,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeader(
          response, 10, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
      validateHeader(
          response, 11, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
    }
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
        "eventstatus",
        "Event status",
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

    // Assert rows.
    validateRow(
        response,
        0,
        setRowData(
            List.of(
                "n1V0AKAOgj2",
                "Zj7UnCAulEk",
                "2022-08-01 00:00:00.0",
                "",
                ",  ()",
                ",  ()",
                "2018-08-04 15:17:39.87",
                "",
                "{\"type\":\"Point\",\"coordinates\":[-12.267675,8.200319]}",
                "",
                "-12.262115",
                "8.195179",
                "Ngelehun CHC",
                "Sierra Leone / Bo / Badjia / Ngelehun CHC",
                "OU_559",
                "",
                "ACTIVE",
                "DiszpKrYNg8"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));

    validateRow(
        response,
        1,
        setRowData(
            List.of(
                "aTL7SjJycjA",
                "Zj7UnCAulEk",
                "2022-08-02 00:00:00.0",
                "system",
                ",  ()",
                ",  ()",
                "2018-08-04 15:17:10.722",
                "",
                "{\"type\":\"Point\",\"coordinates\":[-12.38522,8.214034]}",
                "",
                "-12.384338",
                "8.200616",
                "Ngelehun CHC",
                "Sierra Leone / Bo / Badjia / Ngelehun CHC",
                "OU_559",
                "",
                "ACTIVE",
                "DiszpKrYNg8"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));
    validateRow(
        response,
        2,
        setRowData(
            List.of(
                "ldLR9NLHI5w",
                "Zj7UnCAulEk",
                "2022-08-01 00:00:00.0",
                "system",
                ",  ()",
                ",  ()",
                "2018-08-04 15:15:50.672",
                "",
                "{\"type\":\"Point\",\"coordinates\":[-12.182307,8.244543]}",
                "",
                "-12.164612",
                "8.24411",
                "Ngelehun CHC",
                "Sierra Leone / Bo / Badjia / Ngelehun CHC",
                "OU_559",
                "",
                "ACTIVE",
                "DiszpKrYNg8"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));
    validateRow(
        response,
        3,
        setRowData(
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
                "",
                "-12.91769",
                "8.42347",
                "Ngelehun CHC",
                "Sierra Leone / Bo / Badjia / Ngelehun CHC",
                "OU_559",
                "",
                "ACTIVE",
                "DiszpKrYNg8"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));
  }

  @Test
  public void metadataForDataElementOfTypeOrgUnitFilterEq() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("outputType=EVENT")
            .add("page=1")
            .add("dimension=Zj7UnCAulEk.S33cRBsnXPo:EQ:Vth0fbpFcsO");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(expectPostgis ? 18 : 14)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(expectPostgis ? 18 : 14));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"eBAyeGv0exc\":{\"uid\":\"eBAyeGv0exc\",\"name\":\"Inpatient morbidity and mortality\"},\"Vth0fbpFcsO\":{\"uid\":\"Vth0fbpFcsO\",\"code\":\"OU_233310\",\"name\":\"Kono\"},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"S33cRBsnXPo\":{\"uid\":\"S33cRBsnXPo\",\"code\":\"DE_862347\",\"name\":\"Inpatient Place of Infection\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Zj7UnCAulEk.S33cRBsnXPo\":{\"uid\":\"S33cRBsnXPo\",\"code\":\"DE_862347\",\"name\":\"Inpatient Place of Infection\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"Zj7UnCAulEk\":{\"uid\":\"Zj7UnCAulEk\",\"name\":\"Inpatient morbidity and mortality\",\"description\":\"Anonymous and ICD-10 coded inpatient data\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.S33cRBsnXPo\":[\"Vth0fbpFcsO\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    List<Map<String, Object>> actualHeaders = getHeadersFromResponse(response);

    // Assert headers.
    validateHeader(response, 0, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "eventdate", "Event date", "DATETIME", "java.time.LocalDateTime", false, true);
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
    if (expectPostgis) {
      validateHeader(response, 8, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
      validateHeader(
          response,
          9,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeader(
          response, 10, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
      validateHeader(
          response, 11, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
    }
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
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.S33cRBsnXPo",
        "Inpatient Place of Infection",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);

    // Assert rows.
  }

  @Test
  public void metadataForDataElementOfTypeOrgUnitFilterIn() throws JSONException {

    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:USER_ORGUNIT")
            .add("includeMetadataDetails=true")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("outputType=EVENT")
            .add("page=1")
            .add(
                "dimension=Zj7UnCAulEk.S33cRBsnXPo:IN:USER_ORGUNIT;USER_ORGUNIT_GRANDCHILDREN;lc3eMKXaEfw;bL4ooGhyHRQ;LEVEL-H1KlN4QIauv;USER_ORGUNIT_CHILDREN;Vth0fbpFcsO;OU_GROUP-CXw2yu5fodb");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(expectPostgis ? 18 : 14)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(expectPostgis ? 18 : 14));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"yu4N82FFeLm\":{\"uid\":\"yu4N82FFeLm\",\"code\":\"OU_204910\",\"name\":\"Mandu\"},\"KXSqt7jv6DU\":{\"uid\":\"KXSqt7jv6DU\",\"code\":\"OU_222627\",\"name\":\"Gorama Mende\"},\"lY93YpCxJqf\":{\"uid\":\"lY93YpCxJqf\",\"code\":\"OU_193249\",\"name\":\"Makari Gbanti\"},\"eROJsBwxQHt\":{\"uid\":\"eROJsBwxQHt\",\"code\":\"OU_222743\",\"name\":\"Gaura\"},\"DxAPPqXvwLy\":{\"uid\":\"DxAPPqXvwLy\",\"code\":\"OU_204929\",\"name\":\"Peje Bongre\"},\"PaqugoqjRIj\":{\"uid\":\"PaqugoqjRIj\",\"code\":\"OU_226225\",\"name\":\"Sulima (Koinadugu)\"},\"gy8rmvYT4cj\":{\"uid\":\"gy8rmvYT4cj\",\"code\":\"OU_247037\",\"name\":\"Ribbi\"},\"eIQbndfxQMb\":{\"uid\":\"eIQbndfxQMb\",\"code\":\"OU_268149\",\"name\":\"Tonkolili\"},\"RzKeCma9qb1\":{\"uid\":\"RzKeCma9qb1\",\"code\":\"OU_260428\",\"name\":\"Barri\"},\"vULnao2hV5v\":{\"uid\":\"vULnao2hV5v\",\"code\":\"OU_247086\",\"name\":\"Fakunya\"},\"EfWCa0Cc8WW\":{\"uid\":\"EfWCa0Cc8WW\",\"code\":\"OU_255030\",\"name\":\"Masimera\"},\"AovmOHadayb\":{\"uid\":\"AovmOHadayb\",\"code\":\"OU_247044\",\"name\":\"Timidale\"},\"hjpHnHZIniP\":{\"uid\":\"hjpHnHZIniP\",\"code\":\"OU_204887\",\"name\":\"Kissi Tongi\"},\"U6Kr7Gtpidn\":{\"uid\":\"U6Kr7Gtpidn\",\"code\":\"OU_546\",\"name\":\"Kakua\"},\"EYt6ThQDagn\":{\"uid\":\"EYt6ThQDagn\",\"code\":\"OU_222642\",\"name\":\"Koya (kenema)\"},\"iUauWFeH8Qp\":{\"uid\":\"iUauWFeH8Qp\",\"code\":\"OU_197402\",\"name\":\"Bum\"},\"Jiyc4ekaMMh\":{\"uid\":\"Jiyc4ekaMMh\",\"code\":\"OU_247080\",\"name\":\"Kongbora\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"FlBemv1NfEC\":{\"uid\":\"FlBemv1NfEC\",\"code\":\"OU_211256\",\"name\":\"Masungbala\"},\"XrF5AvaGcuw\":{\"uid\":\"XrF5AvaGcuw\",\"code\":\"OU_226240\",\"name\":\"Wara Wara Bafodia\"},\"LhaAPLxdSFH\":{\"uid\":\"LhaAPLxdSFH\",\"code\":\"OU_233348\",\"name\":\"Lei\"},\"BmYyh9bZ0sr\":{\"uid\":\"BmYyh9bZ0sr\",\"code\":\"OU_268197\",\"name\":\"Kafe Simira\"},\"pk7bUK5c1Uf\":{\"uid\":\"pk7bUK5c1Uf\",\"code\":\"OU_260397\",\"name\":\"Ya Kpukumu Krim\"},\"zSNUViKdkk3\":{\"uid\":\"zSNUViKdkk3\",\"code\":\"OU_260440\",\"name\":\"Kpaka\"},\"r06ohri9wA9\":{\"uid\":\"r06ohri9wA9\",\"code\":\"OU_211243\",\"name\":\"Samu\"},\"ERmBhYkhV6Y\":{\"uid\":\"ERmBhYkhV6Y\",\"code\":\"OU_204877\",\"name\":\"Njaluahun\"},\"Z9QaI6sxTwW\":{\"uid\":\"Z9QaI6sxTwW\",\"code\":\"OU_247068\",\"name\":\"Kargboro\"},\"daJPPxtIrQn\":{\"uid\":\"daJPPxtIrQn\",\"code\":\"OU_545\",\"name\":\"Jaiama Bongor\"},\"W5fN3G6y1VI\":{\"uid\":\"W5fN3G6y1VI\",\"code\":\"OU_247012\",\"name\":\"Lower Banta\"},\"r1RUyfVBkLp\":{\"uid\":\"r1RUyfVBkLp\",\"code\":\"OU_268169\",\"name\":\"Sambaia Bendugu\"},\"CXw2yu5fodb\":{\"uid\":\"CXw2yu5fodb\",\"code\":\"CHC\",\"name\":\"CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT_GROUP\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"NNE0YMCDZkO\":{\"uid\":\"NNE0YMCDZkO\",\"code\":\"OU_268225\",\"name\":\"Yoni\"},\"ENHOJz3UH5L\":{\"uid\":\"ENHOJz3UH5L\",\"code\":\"OU_197440\",\"name\":\"BMC\"},\"QywkxFudXrC\":{\"uid\":\"QywkxFudXrC\",\"code\":\"OU_211227\",\"name\":\"Magbema\"},\"jWSIbtKfURj\":{\"uid\":\"jWSIbtKfURj\",\"code\":\"OU_222751\",\"name\":\"Langrama\"},\"J4GiUImJZoE\":{\"uid\":\"J4GiUImJZoE\",\"code\":\"OU_226269\",\"name\":\"Nieni\"},\"CF243RPvNY7\":{\"uid\":\"CF243RPvNY7\",\"code\":\"OU_233359\",\"name\":\"Fiama\"},\"I4jWcnFmgEC\":{\"uid\":\"I4jWcnFmgEC\",\"code\":\"OU_549\",\"name\":\"Niawa Lenga\"},\"cM2BKSrj9F9\":{\"uid\":\"cM2BKSrj9F9\",\"code\":\"OU_204894\",\"name\":\"Luawa\"},\"kvkDWg42lHR\":{\"uid\":\"kvkDWg42lHR\",\"code\":\"OU_233339\",\"name\":\"Kamara\"},\"jPidqyo7cpF\":{\"uid\":\"jPidqyo7cpF\",\"code\":\"OU_247049\",\"name\":\"Bagruwa\"},\"BGGmAwx33dj\":{\"uid\":\"BGGmAwx33dj\",\"code\":\"OU_543\",\"name\":\"Bumpe Ngao\"},\"iGHlidSFdpu\":{\"uid\":\"iGHlidSFdpu\",\"code\":\"OU_233317\",\"name\":\"Soa\"},\"g8DdBm7EmUt\":{\"uid\":\"g8DdBm7EmUt\",\"code\":\"OU_197397\",\"name\":\"Sittia\"},\"ZiOVcrSjSYe\":{\"uid\":\"ZiOVcrSjSYe\",\"code\":\"OU_254976\",\"name\":\"Dibia\"},\"vn9KJsLyP5f\":{\"uid\":\"vn9KJsLyP5f\",\"code\":\"OU_255005\",\"name\":\"Kaffu Bullom\"},\"QlCIp2S9NHs\":{\"uid\":\"QlCIp2S9NHs\",\"code\":\"OU_222682\",\"name\":\"Dodo\"},\"j43EZb15rjI\":{\"uid\":\"j43EZb15rjI\",\"code\":\"OU_193285\",\"name\":\"Sella Limba\"},\"Vth0fbpFcsO\":{\"uid\":\"Vth0fbpFcsO\",\"code\":\"OU_233310\",\"name\":\"Kono\"},\"O6uvpzGd5pu\":{\"uid\":\"O6uvpzGd5pu\",\"code\":\"OU_264\",\"name\":\"Bo\"},\"kJq2mPyFEHo\":{\"uid\":\"kJq2mPyFEHo\",\"code\":\"OU_222616\",\"name\":\"Kenema\"},\"bQiBfA2j5cw\":{\"uid\":\"bQiBfA2j5cw\",\"code\":\"OU_204857\",\"name\":\"Penguia\"},\"NqWaKXcg01b\":{\"uid\":\"NqWaKXcg01b\",\"code\":\"OU_260384\",\"name\":\"Sowa\"},\"USER_ORGUNIT_GRANDCHILDREN\":{\"organisationUnits\":[\"nV3OkyzF4US\",\"r06ohri9wA9\",\"Z9QaI6sxTwW\",\"A3Fh37HWBWE\",\"DBs6e2Oxaj1\",\"sxRd2XOzFbz\",\"CG4QD1HC3h4\",\"j0Mtr3xTMjM\",\"YuQRtpLP10I\",\"QwMiPiME3bA\",\"iEkBZnMDarP\",\"KSdZwrU7Hh6\",\"g5ptsn0SFX8\",\"y5X4mP5XylL\",\"USQdmvrHh1Q\",\"KXSqt7jv6DU\",\"xGMGhjA3y6J\",\"yu4N82FFeLm\",\"vn9KJsLyP5f\",\"LsYpCyYxSLY\",\"EYt6ThQDagn\",\"npWGUj37qDe\",\"HWjrSuoNPte\",\"nlt6j60tCHF\",\"VCtF1DbspR5\",\"l7pFejMtUoF\",\"XEyIRFd9pct\",\"xhyjU2SVewz\",\"lYIM1MXbSYS\",\"pRHGAROvuyI\",\"NqWaKXcg01b\",\"BD9gU0GKlr2\",\"RzKeCma9qb1\",\"iUauWFeH8Qp\",\"ENHOJz3UH5L\",\"PrJQHI6q7w2\",\"HV8RTzgcFH3\",\"LfTkc0S4b5k\",\"NNE0YMCDZkO\",\"ARZ4y5i4reU\",\"iGHlidSFdpu\",\"DmaLM8WYmWv\",\"RWvG1aFrr0r\",\"QlCIp2S9NHs\",\"P69SId31eDp\",\"GWTIxJO9pRo\",\"M2qEv692lS6\",\"rXLor9Knq6l\",\"AovmOHadayb\",\"ajILkI0cfxn\",\"hjpHnHZIniP\",\"Qhmi8IZyPyD\",\"W5fN3G6y1VI\",\"GFk45MOxzJJ\",\"J4GiUImJZoE\",\"U09TSwIjG0s\",\"EjnIQNVAXGp\",\"JsxnA2IywRo\",\"Zoy23SSHCPs\",\"nOYt1LtFSyU\",\"vULnao2hV5v\",\"smoyi1iYNK6\",\"x4HaBHHwBML\",\"EVkm2xYcf6Z\",\"PaqugoqjRIj\",\"fwH9ipvXde9\",\"Lt8U7GVWvSR\",\"K1r3uF6eZ8n\",\"eV4cuxniZgP\",\"KIUCimTXf8Q\",\"hdEuw2ugkVF\",\"dGheVylzol6\",\"lY93YpCxJqf\",\"eROJsBwxQHt\",\"FRxcUEwktoV\",\"kvkDWg42lHR\",\"byp7w6Xd9Df\",\"vzup1f6ynON\",\"cM2BKSrj9F9\",\"l0ccv2yzfF3\",\"EfWCa0Cc8WW\",\"zSNUViKdkk3\",\"TQkG0sX9nca\",\"pmxZm7klXBy\",\"KctpIIucige\",\"C9uduqDZr9d\",\"XG8HGAbrbbL\",\"EB1zRKdYjdY\",\"gy8rmvYT4cj\",\"qgQ49DH9a0v\",\"hRZOIgQ0O1m\",\"daJPPxtIrQn\",\"pk7bUK5c1Uf\",\"qIRCo0MfuGb\",\"xIKjidMrico\",\"uKC54fzxRzO\",\"j43EZb15rjI\",\"TA7NvKjsn4A\",\"YpVol7asWvd\",\"BXJdOLvUrZB\",\"KKkLOTpMXGV\",\"YmmeuGbqOwR\",\"I4jWcnFmgEC\",\"fwxkctgmffZ\",\"jPidqyo7cpF\",\"r1RUyfVBkLp\",\"Mr4au3jR9bt\",\"U6Kr7Gtpidn\",\"EZPwuUTeIIG\",\"DfUfwjM9am5\",\"VGAFxBXz16y\",\"DxAPPqXvwLy\",\"QywkxFudXrC\",\"zFDYIgyGmXG\",\"qtr8GGlm4gg\",\"ERmBhYkhV6Y\",\"g8DdBm7EmUt\",\"CF243RPvNY7\",\"LhaAPLxdSFH\",\"N233eZJZ1bh\",\"JdhagCUEMbj\",\"WXnNDWTiE9r\",\"vWbkYPRmKyS\",\"XrF5AvaGcuw\",\"UhHipWG7J8b\",\"kbPmt60yi0L\",\"eNtRuQrrZeo\",\"Jiyc4ekaMMh\",\"L8iA6eLwKNb\",\"fRLX08WHWpL\",\"BmYyh9bZ0sr\",\"BGGmAwx33dj\",\"e1eIKM1GIF3\",\"bQiBfA2j5cw\",\"OTFepb1k9Db\",\"cgOy0hRMGu9\",\"FlBemv1NfEC\",\"RndxKqQGzUl\",\"vEvs2ckGNQj\",\"DNRAeXT9IwS\",\"aWQTfvgPA5v\",\"JdqfYTIFZXN\",\"myQ4q1W6B4y\",\"X7dWcGerQIm\",\"VP397wRvePm\",\"ZiOVcrSjSYe\",\"PQZJPIpTepd\",\"kU8vhUkAGaT\",\"Pc3JTyqnsmL\",\"GE25DpSrqpB\",\"d9iMR1MpuIO\",\"jWSIbtKfURj\"]},\"VP397wRvePm\":{\"uid\":\"VP397wRvePm\",\"code\":\"OU_197445\",\"name\":\"Nongoba Bullum\"},\"fwxkctgmffZ\":{\"uid\":\"fwxkctgmffZ\",\"code\":\"OU_268163\",\"name\":\"Kholifa Mabang\"},\"QwMiPiME3bA\":{\"uid\":\"QwMiPiME3bA\",\"code\":\"OU_260400\",\"name\":\"Kpanga Kabonde\"},\"eBAyeGv0exc\":{\"uid\":\"eBAyeGv0exc\",\"name\":\"Inpatient morbidity and mortality\"},\"Qhmi8IZyPyD\":{\"uid\":\"Qhmi8IZyPyD\",\"code\":\"OU_193245\",\"name\":\"Tambaka\"},\"OTFepb1k9Db\":{\"uid\":\"OTFepb1k9Db\",\"code\":\"OU_226244\",\"name\":\"Mongo\"},\"DBs6e2Oxaj1\":{\"uid\":\"DBs6e2Oxaj1\",\"code\":\"OU_247002\",\"name\":\"Upper Banta\"},\"S33cRBsnXPo\":{\"uid\":\"S33cRBsnXPo\",\"code\":\"DE_862347\",\"name\":\"Inpatient Place of Infection\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"eV4cuxniZgP\":{\"uid\":\"eV4cuxniZgP\",\"code\":\"OU_193224\",\"name\":\"Magbaimba Ndowahun\"},\"xhyjU2SVewz\":{\"uid\":\"xhyjU2SVewz\",\"code\":\"OU_268217\",\"name\":\"Tane\"},\"dGheVylzol6\":{\"uid\":\"dGheVylzol6\",\"code\":\"OU_541\",\"name\":\"Bargbe\"},\"vWbkYPRmKyS\":{\"uid\":\"vWbkYPRmKyS\",\"code\":\"OU_540\",\"name\":\"Baoma\"},\"npWGUj37qDe\":{\"uid\":\"npWGUj37qDe\",\"code\":\"OU_552\",\"name\":\"Valunia\"},\"TA7NvKjsn4A\":{\"uid\":\"TA7NvKjsn4A\",\"code\":\"OU_255041\",\"name\":\"Bureh Kasseh Maconteh\"},\"myQ4q1W6B4y\":{\"uid\":\"myQ4q1W6B4y\",\"code\":\"OU_222731\",\"name\":\"Dama\"},\"nV3OkyzF4US\":{\"uid\":\"nV3OkyzF4US\",\"code\":\"OU_246991\",\"name\":\"Kori\"},\"X7dWcGerQIm\":{\"uid\":\"X7dWcGerQIm\",\"code\":\"OU_222677\",\"name\":\"Wandor\"},\"qIRCo0MfuGb\":{\"uid\":\"qIRCo0MfuGb\",\"code\":\"OU_211213\",\"name\":\"Gbinleh Dixion\"},\"kbPmt60yi0L\":{\"uid\":\"kbPmt60yi0L\",\"code\":\"OU_211220\",\"name\":\"Bramaia\"},\"eNtRuQrrZeo\":{\"uid\":\"eNtRuQrrZeo\",\"code\":\"OU_260420\",\"name\":\"Galliness Perri\"},\"HV8RTzgcFH3\":{\"uid\":\"HV8RTzgcFH3\",\"code\":\"OU_197432\",\"name\":\"Kwamabai Krim\"},\"KKkLOTpMXGV\":{\"uid\":\"KKkLOTpMXGV\",\"code\":\"OU_193198\",\"name\":\"Bombali Sebora\"},\"Pc3JTyqnsmL\":{\"uid\":\"Pc3JTyqnsmL\",\"code\":\"OU_255020\",\"name\":\"Buya Romende\"},\"hdEuw2ugkVF\":{\"uid\":\"hdEuw2ugkVF\",\"code\":\"OU_222652\",\"name\":\"Lower Bambara\"},\"l7pFejMtUoF\":{\"uid\":\"l7pFejMtUoF\",\"code\":\"OU_222634\",\"name\":\"Tunkia\"},\"K1r3uF6eZ8n\":{\"uid\":\"K1r3uF6eZ8n\",\"code\":\"OU_222725\",\"name\":\"Kandu Lepiema\"},\"VGAFxBXz16y\":{\"uid\":\"VGAFxBXz16y\",\"code\":\"OU_226231\",\"name\":\"Sengbeh\"},\"GE25DpSrqpB\":{\"uid\":\"GE25DpSrqpB\",\"code\":\"OU_204869\",\"name\":\"Malema\"},\"ARZ4y5i4reU\":{\"uid\":\"ARZ4y5i4reU\",\"code\":\"OU_553\",\"name\":\"Wonde\"},\"byp7w6Xd9Df\":{\"uid\":\"byp7w6Xd9Df\",\"code\":\"OU_204933\",\"name\":\"Yawei\"},\"BXJdOLvUrZB\":{\"uid\":\"BXJdOLvUrZB\",\"code\":\"OU_193277\",\"name\":\"Gbendembu Ngowahun\"},\"lc3eMKXaEfw\":{\"uid\":\"lc3eMKXaEfw\",\"code\":\"OU_197385\",\"name\":\"Bonthe\"},\"uKC54fzxRzO\":{\"uid\":\"uKC54fzxRzO\",\"code\":\"OU_222648\",\"name\":\"Niawa\"},\"TQkG0sX9nca\":{\"uid\":\"TQkG0sX9nca\",\"code\":\"OU_233375\",\"name\":\"Gbense\"},\"hRZOIgQ0O1m\":{\"uid\":\"hRZOIgQ0O1m\",\"code\":\"OU_193302\",\"name\":\"Libeisaygahun\"},\"PrJQHI6q7w2\":{\"uid\":\"PrJQHI6q7w2\",\"code\":\"OU_255061\",\"name\":\"Tainkatopa Makama Safrokoh\"},\"jmIPBj66vD6\":{\"uid\":\"jmIPBj66vD6\",\"code\":\"OU_246990\",\"name\":\"Moyamba\"},\"qhqAxPSTUXp\":{\"uid\":\"qhqAxPSTUXp\",\"code\":\"OU_226213\",\"name\":\"Koinadugu\"},\"USQdmvrHh1Q\":{\"uid\":\"USQdmvrHh1Q\",\"code\":\"OU_247055\",\"name\":\"Kaiyamba\"},\"xGMGhjA3y6J\":{\"uid\":\"xGMGhjA3y6J\",\"code\":\"OU_211262\",\"name\":\"Mambolo\"},\"nOYt1LtFSyU\":{\"uid\":\"nOYt1LtFSyU\",\"code\":\"OU_247025\",\"name\":\"Bumpeh\"},\"jUb8gELQApl\":{\"uid\":\"jUb8gELQApl\",\"code\":\"OU_204856\",\"name\":\"Kailahun\"},\"e1eIKM1GIF3\":{\"uid\":\"e1eIKM1GIF3\",\"code\":\"OU_193215\",\"name\":\"Gbanti Kamaranka\"},\"lYIM1MXbSYS\":{\"uid\":\"lYIM1MXbSYS\",\"code\":\"OU_204920\",\"name\":\"Dea\"},\"sxRd2XOzFbz\":{\"uid\":\"sxRd2XOzFbz\",\"code\":\"OU_551\",\"name\":\"Tikonko\"},\"d9iMR1MpuIO\":{\"uid\":\"d9iMR1MpuIO\",\"code\":\"OU_260410\",\"name\":\"Soro-Gbeima\"},\"Zj7UnCAulEk\":{\"uid\":\"Zj7UnCAulEk\",\"name\":\"Inpatient morbidity and mortality\",\"description\":\"Anonymous and ICD-10 coded inpatient data\"},\"qgQ49DH9a0v\":{\"uid\":\"qgQ49DH9a0v\",\"code\":\"OU_233332\",\"name\":\"Nimiyama\"},\"U09TSwIjG0s\":{\"uid\":\"U09TSwIjG0s\",\"code\":\"OU_222617\",\"name\":\"Nomo\"},\"zFDYIgyGmXG\":{\"uid\":\"zFDYIgyGmXG\",\"code\":\"OU_542\",\"name\":\"Bargbo\"},\"M2qEv692lS6\":{\"uid\":\"M2qEv692lS6\",\"code\":\"OU_233324\",\"name\":\"Tankoro\"},\"qtr8GGlm4gg\":{\"uid\":\"qtr8GGlm4gg\",\"code\":\"OU_278366\",\"name\":\"Rural Western Area\"},\"pRHGAROvuyI\":{\"uid\":\"pRHGAROvuyI\",\"code\":\"OU_254960\",\"name\":\"Koya\"},\"xIKjidMrico\":{\"uid\":\"xIKjidMrico\",\"code\":\"OU_247033\",\"name\":\"Kowa\"},\"GWTIxJO9pRo\":{\"uid\":\"GWTIxJO9pRo\",\"code\":\"OU_233355\",\"name\":\"Gorama Kono\"},\"HWjrSuoNPte\":{\"uid\":\"HWjrSuoNPte\",\"code\":\"OU_254999\",\"name\":\"Sanda Magbolonthor\"},\"UhHipWG7J8b\":{\"uid\":\"UhHipWG7J8b\",\"code\":\"OU_193191\",\"name\":\"Sanda Tendaren\"},\"rXLor9Knq6l\":{\"uid\":\"rXLor9Knq6l\",\"code\":\"OU_268212\",\"name\":\"Kunike Barina\"},\"kU8vhUkAGaT\":{\"uid\":\"kU8vhUkAGaT\",\"code\":\"OU_548\",\"name\":\"Lugbu\"},\"C9uduqDZr9d\":{\"uid\":\"C9uduqDZr9d\",\"code\":\"OU_278311\",\"name\":\"Freetown\"},\"YmmeuGbqOwR\":{\"uid\":\"YmmeuGbqOwR\",\"code\":\"OU_544\",\"name\":\"Gbo\"},\"iEkBZnMDarP\":{\"uid\":\"iEkBZnMDarP\",\"code\":\"OU_226253\",\"name\":\"Folosaba Dembelia\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"JsxnA2IywRo\":{\"uid\":\"JsxnA2IywRo\",\"code\":\"OU_204875\",\"name\":\"Kissi Kama\"},\"nlt6j60tCHF\":{\"uid\":\"nlt6j60tCHF\",\"code\":\"OU_260437\",\"name\":\"Mano Sakrim\"},\"g5ptsn0SFX8\":{\"uid\":\"g5ptsn0SFX8\",\"code\":\"OU_233365\",\"name\":\"Sandor\"},\"XG8HGAbrbbL\":{\"uid\":\"XG8HGAbrbbL\",\"code\":\"OU_193267\",\"name\":\"Safroko Limba\"},\"pmxZm7klXBy\":{\"uid\":\"pmxZm7klXBy\",\"code\":\"OU_204924\",\"name\":\"Peje West\"},\"USER_ORGUNIT_CHILDREN\":{\"organisationUnits\":[\"at6UHUQatSo\",\"TEQlaapDQoK\",\"PMa2VCrupOd\",\"qhqAxPSTUXp\",\"kJq2mPyFEHo\",\"jmIPBj66vD6\",\"Vth0fbpFcsO\",\"jUb8gELQApl\",\"fdc6uOvgoji\",\"eIQbndfxQMb\",\"O6uvpzGd5pu\",\"lc3eMKXaEfw\",\"bL4ooGhyHRQ\"]},\"Zj7UnCAulEk.S33cRBsnXPo\":{\"uid\":\"S33cRBsnXPo\",\"code\":\"DE_862347\",\"name\":\"Inpatient Place of Infection\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"l0ccv2yzfF3\":{\"uid\":\"l0ccv2yzfF3\",\"code\":\"OU_268174\",\"name\":\"Kunike\"},\"YuQRtpLP10I\":{\"uid\":\"YuQRtpLP10I\",\"code\":\"OU_539\",\"name\":\"Badjia\"},\"fdc6uOvgoji\":{\"uid\":\"fdc6uOvgoji\",\"code\":\"OU_193190\",\"name\":\"Bombali\"},\"LfTkc0S4b5k\":{\"uid\":\"LfTkc0S4b5k\",\"code\":\"OU_204915\",\"name\":\"Upper Bambara\"},\"KSdZwrU7Hh6\":{\"uid\":\"KSdZwrU7Hh6\",\"code\":\"OU_204861\",\"name\":\"Jawi\"},\"ajILkI0cfxn\":{\"uid\":\"ajILkI0cfxn\",\"code\":\"OU_233390\",\"name\":\"Gbane\"},\"y5X4mP5XylL\":{\"uid\":\"y5X4mP5XylL\",\"code\":\"OU_211270\",\"name\":\"Tonko Limba\"},\"fwH9ipvXde9\":{\"uid\":\"fwH9ipvXde9\",\"code\":\"OU_193228\",\"name\":\"Biriwa\"},\"KIUCimTXf8Q\":{\"uid\":\"KIUCimTXf8Q\",\"code\":\"OU_222690\",\"name\":\"Nongowa\"},\"vEvs2ckGNQj\":{\"uid\":\"vEvs2ckGNQj\",\"code\":\"OU_226219\",\"name\":\"Kasonko\"},\"H1KlN4QIauv\":{\"uid\":\"H1KlN4QIauv\",\"name\":\"National\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"},\"XEyIRFd9pct\":{\"uid\":\"XEyIRFd9pct\",\"code\":\"OU_197413\",\"name\":\"Imperi\"},\"cgOy0hRMGu9\":{\"uid\":\"cgOy0hRMGu9\",\"code\":\"OU_197408\",\"name\":\"Sogbini\"},\"EjnIQNVAXGp\":{\"uid\":\"EjnIQNVAXGp\",\"code\":\"OU_233344\",\"name\":\"Mafindor\"},\"x4HaBHHwBML\":{\"uid\":\"x4HaBHHwBML\",\"code\":\"OU_222672\",\"name\":\"Malegohun\"},\"EZPwuUTeIIG\":{\"uid\":\"EZPwuUTeIIG\",\"code\":\"OU_226258\",\"name\":\"Wara Wara Yagala\"},\"N233eZJZ1bh\":{\"uid\":\"N233eZJZ1bh\",\"code\":\"OU_260388\",\"name\":\"Pejeh\"},\"smoyi1iYNK6\":{\"uid\":\"smoyi1iYNK6\",\"code\":\"OU_268191\",\"name\":\"Kalansogoia\"},\"DNRAeXT9IwS\":{\"uid\":\"DNRAeXT9IwS\",\"code\":\"OU_197421\",\"name\":\"Dema\"},\"fRLX08WHWpL\":{\"uid\":\"fRLX08WHWpL\",\"code\":\"OU_254982\",\"name\":\"Lokomasama\"},\"Zoy23SSHCPs\":{\"uid\":\"Zoy23SSHCPs\",\"code\":\"OU_233311\",\"name\":\"Gbane Kandor\"},\"LsYpCyYxSLY\":{\"uid\":\"LsYpCyYxSLY\",\"code\":\"OU_247008\",\"name\":\"Kamaje\"},\"bL4ooGhyHRQ\":{\"uid\":\"bL4ooGhyHRQ\",\"code\":\"OU_260377\",\"name\":\"Pujehun\"},\"JdqfYTIFZXN\":{\"uid\":\"JdqfYTIFZXN\",\"code\":\"OU_254946\",\"name\":\"Maforki\"},\"EB1zRKdYjdY\":{\"uid\":\"EB1zRKdYjdY\",\"code\":\"OU_197429\",\"name\":\"Bendu Cha\"},\"CG4QD1HC3h4\":{\"uid\":\"CG4QD1HC3h4\",\"code\":\"OU_197436\",\"name\":\"Yawbeko\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\"},\"at6UHUQatSo\":{\"uid\":\"at6UHUQatSo\",\"code\":\"OU_278310\",\"name\":\"Western Area\"},\"YpVol7asWvd\":{\"uid\":\"YpVol7asWvd\",\"code\":\"OU_260417\",\"name\":\"Kpanga Krim\"},\"RndxKqQGzUl\":{\"uid\":\"RndxKqQGzUl\",\"code\":\"OU_247018\",\"name\":\"Dasse\"},\"P69SId31eDp\":{\"uid\":\"P69SId31eDp\",\"code\":\"OU_268202\",\"name\":\"Gbonkonlenken\"},\"aWQTfvgPA5v\":{\"uid\":\"aWQTfvgPA5v\",\"code\":\"OU_197424\",\"name\":\"Kpanda Kemoh\"},\"KctpIIucige\":{\"uid\":\"KctpIIucige\",\"code\":\"OU_550\",\"name\":\"Selenga\"},\"Mr4au3jR9bt\":{\"uid\":\"Mr4au3jR9bt\",\"code\":\"OU_226214\",\"name\":\"Dembelia Sinkunia\"},\"L8iA6eLwKNb\":{\"uid\":\"L8iA6eLwKNb\",\"code\":\"OU_193295\",\"name\":\"Paki Masabong\"},\"j0Mtr3xTMjM\":{\"uid\":\"j0Mtr3xTMjM\",\"code\":\"OU_204939\",\"name\":\"Kissi Teng\"},\"TEQlaapDQoK\":{\"uid\":\"TEQlaapDQoK\",\"code\":\"OU_254945\",\"name\":\"Port Loko\"},\"DmaLM8WYmWv\":{\"uid\":\"DmaLM8WYmWv\",\"code\":\"OU_233394\",\"name\":\"Nimikoro\"},\"DfUfwjM9am5\":{\"uid\":\"DfUfwjM9am5\",\"code\":\"OU_260392\",\"name\":\"Malen\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"PMa2VCrupOd\":{\"uid\":\"PMa2VCrupOd\",\"code\":\"OU_211212\",\"name\":\"Kambia\"},\"FRxcUEwktoV\":{\"uid\":\"FRxcUEwktoV\",\"code\":\"OU_233314\",\"name\":\"Toli\"},\"VCtF1DbspR5\":{\"uid\":\"VCtF1DbspR5\",\"code\":\"OU_197386\",\"name\":\"Jong\"},\"BD9gU0GKlr2\":{\"uid\":\"BD9gU0GKlr2\",\"code\":\"OU_260378\",\"name\":\"Makpele\"},\"GFk45MOxzJJ\":{\"uid\":\"GFk45MOxzJJ\",\"code\":\"OU_226275\",\"name\":\"Neya\"},\"A3Fh37HWBWE\":{\"uid\":\"A3Fh37HWBWE\",\"code\":\"OU_222687\",\"name\":\"Simbaru\"},\"vzup1f6ynON\":{\"uid\":\"vzup1f6ynON\",\"code\":\"OU_222619\",\"name\":\"Small Bo\"},\"Lt8U7GVWvSR\":{\"uid\":\"Lt8U7GVWvSR\",\"code\":\"OU_226263\",\"name\":\"Diang\"},\"JdhagCUEMbj\":{\"uid\":\"JdhagCUEMbj\",\"code\":\"OU_547\",\"name\":\"Komboya\"},\"EVkm2xYcf6Z\":{\"uid\":\"EVkm2xYcf6Z\",\"code\":\"OU_268184\",\"name\":\"Malal Mara\"},\"PQZJPIpTepd\":{\"uid\":\"PQZJPIpTepd\",\"code\":\"OU_268150\",\"name\":\"Kholifa Rowalla\"},\"WXnNDWTiE9r\":{\"uid\":\"WXnNDWTiE9r\",\"code\":\"OU_193239\",\"name\":\"Sanda Loko\"},\"RWvG1aFrr0r\":{\"uid\":\"RWvG1aFrr0r\",\"code\":\"OU_255053\",\"name\":\"Marampa\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.S33cRBsnXPo\":[\"ImspTQPwCqd\",\"YuQRtpLP10I\",\"vWbkYPRmKyS\",\"dGheVylzol6\",\"zFDYIgyGmXG\",\"BGGmAwx33dj\",\"YmmeuGbqOwR\",\"daJPPxtIrQn\",\"U6Kr7Gtpidn\",\"JdhagCUEMbj\",\"kU8vhUkAGaT\",\"I4jWcnFmgEC\",\"KctpIIucige\",\"sxRd2XOzFbz\",\"npWGUj37qDe\",\"ARZ4y5i4reU\",\"fwH9ipvXde9\",\"KKkLOTpMXGV\",\"e1eIKM1GIF3\",\"BXJdOLvUrZB\",\"hRZOIgQ0O1m\",\"eV4cuxniZgP\",\"lY93YpCxJqf\",\"L8iA6eLwKNb\",\"XG8HGAbrbbL\",\"WXnNDWTiE9r\",\"UhHipWG7J8b\",\"j43EZb15rjI\",\"Qhmi8IZyPyD\",\"ENHOJz3UH5L\",\"EB1zRKdYjdY\",\"iUauWFeH8Qp\",\"DNRAeXT9IwS\",\"XEyIRFd9pct\",\"VCtF1DbspR5\",\"aWQTfvgPA5v\",\"HV8RTzgcFH3\",\"VP397wRvePm\",\"g8DdBm7EmUt\",\"cgOy0hRMGu9\",\"CG4QD1HC3h4\",\"lYIM1MXbSYS\",\"KSdZwrU7Hh6\",\"JsxnA2IywRo\",\"j0Mtr3xTMjM\",\"hjpHnHZIniP\",\"cM2BKSrj9F9\",\"GE25DpSrqpB\",\"yu4N82FFeLm\",\"ERmBhYkhV6Y\",\"DxAPPqXvwLy\",\"pmxZm7klXBy\",\"bQiBfA2j5cw\",\"LfTkc0S4b5k\",\"byp7w6Xd9Df\",\"kbPmt60yi0L\",\"qIRCo0MfuGb\",\"QywkxFudXrC\",\"xGMGhjA3y6J\",\"FlBemv1NfEC\",\"r06ohri9wA9\",\"y5X4mP5XylL\",\"myQ4q1W6B4y\",\"QlCIp2S9NHs\",\"eROJsBwxQHt\",\"KXSqt7jv6DU\",\"K1r3uF6eZ8n\",\"EYt6ThQDagn\",\"jWSIbtKfURj\",\"hdEuw2ugkVF\",\"x4HaBHHwBML\",\"uKC54fzxRzO\",\"U09TSwIjG0s\",\"KIUCimTXf8Q\",\"A3Fh37HWBWE\",\"vzup1f6ynON\",\"l7pFejMtUoF\",\"X7dWcGerQIm\",\"Mr4au3jR9bt\",\"Lt8U7GVWvSR\",\"iEkBZnMDarP\",\"vEvs2ckGNQj\",\"OTFepb1k9Db\",\"GFk45MOxzJJ\",\"J4GiUImJZoE\",\"VGAFxBXz16y\",\"PaqugoqjRIj\",\"XrF5AvaGcuw\",\"EZPwuUTeIIG\",\"CF243RPvNY7\",\"ajILkI0cfxn\",\"Zoy23SSHCPs\",\"TQkG0sX9nca\",\"GWTIxJO9pRo\",\"kvkDWg42lHR\",\"LhaAPLxdSFH\",\"EjnIQNVAXGp\",\"DmaLM8WYmWv\",\"qgQ49DH9a0v\",\"g5ptsn0SFX8\",\"iGHlidSFdpu\",\"M2qEv692lS6\",\"FRxcUEwktoV\",\"jPidqyo7cpF\",\"nOYt1LtFSyU\",\"RndxKqQGzUl\",\"vULnao2hV5v\",\"USQdmvrHh1Q\",\"LsYpCyYxSLY\",\"Z9QaI6sxTwW\",\"Jiyc4ekaMMh\",\"nV3OkyzF4US\",\"xIKjidMrico\",\"W5fN3G6y1VI\",\"gy8rmvYT4cj\",\"AovmOHadayb\",\"DBs6e2Oxaj1\",\"TA7NvKjsn4A\",\"Pc3JTyqnsmL\",\"ZiOVcrSjSYe\",\"vn9KJsLyP5f\",\"pRHGAROvuyI\",\"fRLX08WHWpL\",\"JdqfYTIFZXN\",\"RWvG1aFrr0r\",\"EfWCa0Cc8WW\",\"HWjrSuoNPte\",\"PrJQHI6q7w2\",\"RzKeCma9qb1\",\"eNtRuQrrZeo\",\"zSNUViKdkk3\",\"QwMiPiME3bA\",\"YpVol7asWvd\",\"BD9gU0GKlr2\",\"DfUfwjM9am5\",\"nlt6j60tCHF\",\"N233eZJZ1bh\",\"d9iMR1MpuIO\",\"NqWaKXcg01b\",\"pk7bUK5c1Uf\",\"P69SId31eDp\",\"BmYyh9bZ0sr\",\"smoyi1iYNK6\",\"fwxkctgmffZ\",\"PQZJPIpTepd\",\"l0ccv2yzfF3\",\"rXLor9Knq6l\",\"EVkm2xYcf6Z\",\"r1RUyfVBkLp\",\"xhyjU2SVewz\",\"NNE0YMCDZkO\",\"C9uduqDZr9d\",\"qtr8GGlm4gg\",\"lc3eMKXaEfw\",\"bL4ooGhyHRQ\",\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"eIQbndfxQMb\",\"at6UHUQatSo\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    List<Map<String, Object>> actualHeaders = getHeadersFromResponse(response);

    // Assert headers.
    validateHeader(response, 0, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "eventdate", "Event date", "DATETIME", "java.time.LocalDateTime", false, true);
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
    if (expectPostgis) {
      validateHeader(response, 8, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
      validateHeader(
          response,
          9,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeader(
          response, 10, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
      validateHeader(
          response, 11, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
    }
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
        "eventstatus",
        "Event status",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.S33cRBsnXPo",
        "Inpatient Place of Infection",
        "ORGANISATION_UNIT",
        "org.hisp.dhis.organisationunit.OrganisationUnit",
        false,
        true);
  }
}
