/*
 * Copyright (c) 2004-2023, University of Oslo
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
public class EventsQuery2AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void queryVisitOverviewThisYearBombali() throws JSONException {

    boolean expectPostgis = isPostgres();
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=pe:THIS_YEAR,ou:fdc6uOvgoji,Zj7UnCAulEk.tUdBD1JDxpn,Zj7UnCAulEk.gWxh7DiRmG7,Zj7UnCAulEk.x7PaHGvgWY2,Zj7UnCAulEk.hlPt8H4bUOQ,Zj7UnCAulEk.Y7hKDSuqEtH,Zj7UnCAulEk.oZg33kd9taw,Zj7UnCAulEk.GieVkTxp4HH-TBxGTceyzwy,Zj7UnCAulEk.HS8QXAJtuKV,Zj7UnCAulEk.vV9UWAZohSf-OrkEzxZEH4X")
            .add("desc=eventdate,lastupdated")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(expectPostgis ? 29 : 25)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(expectPostgis ? 29 : 25))
        .body("headerWidth", equalTo(expectPostgis ? 29 : 25));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":false},\"items\":{\"lnccUWrmqL0\":{\"name\":\"80 - 90\"},\"eySqrYxteI7\":{\"name\":\"200+\"},\"hlPt8H4bUOQ\":{\"name\":\"BMI female under 5 y\"},\"BHlWGFLIU20\":{\"name\":\"120 - 140\"},\"GWuQsWJDGvN\":{\"name\":\"140 - 160\"},\"GDFw7T4aFGz\":{\"name\":\"60 - 70\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"Zj7UnCAulEk.hlPt8H4bUOQ\":{\"name\":\"BMI female under 5 y\"},\"NxQrJ3icPkE\":{\"name\":\"0 - 20\"},\"b9UzeWaSs2u\":{\"name\":\"20 - 40\"},\"xVezsaEXU3k\":{\"name\":\"70 - 80\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"CivTksSoCt0\":{\"name\":\"100 - 120\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"tUdBD1JDxpn\":{\"name\":\"Average age of deaths\"},\"x7PaHGvgWY2\":{\"name\":\"BMI\"},\"AD5jueZTZSK\":{\"name\":\"40 - 50\"},\"Zj7UnCAulEk.tUdBD1JDxpn\":{\"name\":\"Average age of deaths\"},\"Zj7UnCAulEk.x7PaHGvgWY2\":{\"name\":\"BMI\"},\"f3prvzpfniC\":{\"name\":\"100+\"},\"sxFVvKLpE0y\":{\"name\":\"0 - 100\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"Zj7UnCAulEk.gWxh7DiRmG7\":{\"name\":\"Average height of girls at 5 years old\"},\"Zj7UnCAulEk.oZg33kd9taw\":{\"name\":\"Gender\"},\"B1X4JyH4Mdw\":{\"name\":\"180 - 200\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"Zj7UnCAulEk.vV9UWAZohSf\":{\"name\":\"Weight in kg\"},\"ou\":{\"name\":\"Organisation unit\"},\"Zj7UnCAulEk.Y7hKDSuqEtH\":{\"name\":\"BMI male under 5 y\"},\"Zj7UnCAulEk.HS8QXAJtuKV\":{\"name\":\"Inpatient bed days average\"},\"vV9UWAZohSf\":{\"name\":\"Weight in kg\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"Sjp6IB3gthI\":{\"name\":\"50 - 60\"},\"Y7hKDSuqEtH\":{\"name\":\"BMI male under 5 y\"},\"gWxh7DiRmG7\":{\"name\":\"Average height of girls at 5 years old\"},\"GieVkTxp4HH\":{\"name\":\"Height in cm\"},\"wgbW2ZQnlIc\":{\"name\":\"160 - 180\"},\"XKEvGfAkh3R\":{\"name\":\"90 - 100\"},\"oZg33kd9taw\":{\"name\":\"Gender\"},\"HS8QXAJtuKV\":{\"name\":\"Inpatient bed days average\"},\"Zj7UnCAulEk.GieVkTxp4HH\":{\"name\":\"Height in cm\"}},\"dimensions\":{\"Zj7UnCAulEk.gWxh7DiRmG7\":[],\"Zj7UnCAulEk.oZg33kd9taw\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"pe\":[],\"Zj7UnCAulEk.vV9UWAZohSf\":[\"NxQrJ3icPkE\",\"b9UzeWaSs2u\",\"AD5jueZTZSK\",\"Sjp6IB3gthI\",\"GDFw7T4aFGz\",\"xVezsaEXU3k\",\"lnccUWrmqL0\",\"XKEvGfAkh3R\",\"f3prvzpfniC\"],\"ou\":[\"fdc6uOvgoji\"],\"Zj7UnCAulEk.Y7hKDSuqEtH\":[],\"Zj7UnCAulEk.HS8QXAJtuKV\":[],\"Zj7UnCAulEk.tUdBD1JDxpn\":[],\"Zj7UnCAulEk.x7PaHGvgWY2\":[],\"Zj7UnCAulEk.hlPt8H4bUOQ\":[],\"Zj7UnCAulEk.GieVkTxp4HH\":[\"sxFVvKLpE0y\",\"CivTksSoCt0\",\"BHlWGFLIU20\",\"GWuQsWJDGvN\",\"wgbW2ZQnlIc\",\"B1X4JyH4Mdw\",\"eySqrYxteI7\"]}}";
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
        9,
        "scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    if (expectPostgis) {
      validateHeader(response, 10, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
      validateHeader(
          response,
          11,
          "enrollmentgeometry",
          "Enrollment geometry",
          "TEXT",
          "java.lang.String",
          false,
          true);
      validateHeader(
          response, 12, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
      validateHeader(
          response, 13, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
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
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.gWxh7DiRmG7",
        "Average height of girls at 5 years old",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.oZg33kd9taw",
        "Gender",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.Y7hKDSuqEtH",
        "BMI male under 5 y",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.HS8QXAJtuKV",
        "Inpatient bed days average",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.tUdBD1JDxpn",
        "Average age of deaths",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.x7PaHGvgWY2",
        "BMI",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.vV9UWAZohSf",
        "Weight in kg",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.hlPt8H4bUOQ",
        "BMI female under 5 y",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Zj7UnCAulEk.GieVkTxp4HH",
        "Height in cm",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        setRowData(
            List.of(
                "fM9apOQyeMf",
                "Zj7UnCAulEk",
                "2022-12-29 00:00:00.0",
                "",
                ",  ()",
                ",  ()",
                "2018-04-21 14:06:04.136",
                "2017-04-21 14:06:04.136",
                "2022-04-21 00:00:00.0",
                "",
                "",
                "",
                "0.0",
                "0.0",
                "Gbendembu Wesleyan CHC",
                "Sierra Leone / Bombali / Gbendembu Ngowahun / Gbendembu Wesleyan CHC",
                "OU_193281",
                "",
                "COMPLETED",
                "YAuJ3fyoEuI",
                "",
                "Male",
                "",
                "14",
                "",
                "27.04",
                "AD5jueZTZSK",
                "",
                "BHlWGFLIU20"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));

    validateRow(
        response,
        10,
        setRowData(
            List.of(
                "KwVfZotd3H9",
                "Zj7UnCAulEk",
                "2022-12-29 00:00:00.0",
                "",
                ",  ()",
                ",  ()",
                "2018-04-21 13:56:58.057",
                "2017-04-21 13:56:58.056",
                "2022-04-21 00:00:00.0",
                "",
                "",
                "",
                "0.0",
                "0.0",
                "Mateboi CHC",
                "Sierra Leone / Bombali / Sanda Tendaren / Mateboi CHC",
                "OU_193197",
                "",
                "COMPLETED",
                "EXbPGmEUdnc",
                "",
                "Female",
                "",
                "14",
                "",
                "168.03",
                "lnccUWrmqL0",
                "",
                "sxFVvKLpE0y"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));

    validateRow(
        response,
        20,
        setRowData(
            List.of(
                "l15QNizydyL",
                "Zj7UnCAulEk",
                "2022-12-28 00:00:00.0",
                "",
                ",  ()",
                ",  ()",
                "2018-04-21 14:00:00.57",
                "2017-04-21 14:00:00.569",
                "2022-04-21 00:00:00.0",
                "",
                "",
                "",
                "0.0",
                "0.0",
                "Kamabaio MCHP",
                "Sierra Leone / Bombali / Sella Limba / Kamabaio MCHP",
                "OU_193294",
                "",
                "COMPLETED",
                "OwHjzJEVEUN",
                "",
                "Male",
                "",
                "14",
                "",
                "16.66",
                "b9UzeWaSs2u",
                "",
                "GWuQsWJDGvN"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));

    validateRow(
        response,
        30,
        setRowData(
            List.of(
                "XQg2lf2RpjO",
                "Zj7UnCAulEk",
                "2022-12-28 00:00:00.0",
                "",
                ",  ()",
                ",  ()",
                "2018-04-21 13:29:09.533",
                "2017-04-21 13:29:09.532",
                "2022-04-21 00:00:00.0",
                "",
                "",
                "",
                "0.0",
                "0.0",
                "Govt. Hosp. Makeni",
                "Sierra Leone / Bombali / Bombali Sebora / Govt. Hosp. Makeni",
                "OU_193206",
                "",
                "COMPLETED",
                "GQcsUZf81vP",
                "",
                "Female",
                "",
                "14",
                "",
                "21.87",
                "GDFw7T4aFGz",
                "",
                "wgbW2ZQnlIc"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));
    validateRow(
        response,
        40,
        setRowData(
            List.of(
                "eNWP8IgdRTn",
                "Zj7UnCAulEk",
                "2022-12-27 00:00:00.0",
                "",
                ",  ()",
                ",  ()",
                "2018-04-21 13:55:38.768",
                "2017-04-21 13:55:38.767",
                "2022-04-21 00:00:00.0",
                "",
                "",
                "",
                "0.0",
                "0.0",
                "Kayongoro MCHP",
                "Sierra Leone / Bombali / Biriwa / Kayongoro MCHP",
                "OU_193232",
                "",
                "COMPLETED",
                "tEgxbwwrwUd",
                "",
                "Female",
                "",
                "14",
                "81",
                "254.19",
                "AD5jueZTZSK",
                "",
                "sxFVvKLpE0y"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));

    validateRow(
        response,
        50,
        setRowData(
            List.of(
                "KgJwq6FjLq4",
                "Zj7UnCAulEk",
                "2022-12-26 00:00:00.0",
                "",
                ",  ()",
                ",  ()",
                "2018-04-21 14:02:01.592",
                "2017-04-21 14:02:01.591",
                "2022-04-21 00:00:00.0",
                "",
                "",
                "",
                "0.0",
                "0.0",
                "Punthun MCHP",
                "Sierra Leone / Bombali / Makari Gbanti / Punthun MCHP",
                "OU_193260",
                "",
                "COMPLETED",
                "rNaQEFRINbd",
                "",
                "Female",
                "",
                "14",
                "73",
                "77.78",
                "b9UzeWaSs2u",
                "",
                "sxFVvKLpE0y"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));
    validateRow(
        response,
        60,
        setRowData(
            List.of(
                "zx7CSw8OJSP",
                "Zj7UnCAulEk",
                "2022-12-26 00:00:00.0",
                "",
                ",  ()",
                ",  ()",
                "2018-04-21 13:29:02.774",
                "2017-04-21 13:29:02.773",
                "2022-04-21 00:00:00.0",
                "",
                "",
                "",
                "0.0",
                "0.0",
                "Makaiba MCHP",
                "Sierra Leone / Bombali / Gbanti Kamaranka / Makaiba MCHP",
                "OU_193219",
                "",
                "COMPLETED",
                "ewh5SKxcCAl",
                "",
                "Female",
                "",
                "14",
                "",
                "52.94",
                "Sjp6IB3gthI",
                "",
                "CivTksSoCt0"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));
    validateRow(
        response,
        70,
        setRowData(
            List.of(
                "Q2lx6tUnHlV",
                "Zj7UnCAulEk",
                "2022-12-25 00:00:00.0",
                "",
                ",  ()",
                ",  ()",
                "2018-04-21 13:59:07.604",
                "2017-04-21 13:59:07.603",
                "2022-04-21 00:00:00.0",
                "",
                "",
                "",
                "0.0",
                "0.0",
                "Makump Bana MCHP",
                "Sierra Leone / Bombali / Bombali Sebora / Makump Bana MCHP",
                "OU_193211",
                "",
                "COMPLETED",
                "E7IDb3nNiW7",
                "",
                "Male",
                "",
                "14",
                "",
                "20.31",
                "GDFw7T4aFGz",
                "",
                "B1X4JyH4Mdw"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));
    validateRow(
        response,
        90,
        setRowData(
            List.of(
                "sud2AhBwxSc",
                "Zj7UnCAulEk",
                "2022-12-24 00:00:00.0",
                "",
                ",  ()",
                ",  ()",
                "2018-04-21 13:53:40.11",
                "2017-04-21 13:53:40.109",
                "2022-04-21 00:00:00.0",
                "",
                "",
                "",
                "0.0",
                "0.0",
                "Kagbere CHC",
                "Sierra Leone / Bombali / Magbaimba Ndowahun / Kagbere CHC",
                "OU_193225",
                "",
                "COMPLETED",
                "TjZwphhxCuV",
                "",
                "Female",
                "",
                "14",
                "",
                "86.53",
                "NxQrJ3icPkE",
                "86.53",
                "sxFVvKLpE0y"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));
    validateRow(
        response,
        99,
        setRowData(
            List.of(
                "rGUBpq8eDo2",
                "Zj7UnCAulEk",
                "2022-12-23 00:00:00.0",
                "",
                ",  ()",
                ",  ()",
                "2018-04-21 13:57:41.308",
                "2017-04-21 13:57:41.307",
                "2022-04-21 00:00:00.0",
                "",
                "",
                "",
                "0.0",
                "0.0",
                "Fullah Town (B.Sebora) MCHP",
                "Sierra Leone / Bombali / Bombali Sebora / Fullah Town (B.Sebora) MCHP",
                "OU_424885",
                "",
                "COMPLETED",
                "aQoqXL4cZaF",
                "",
                "Female",
                "",
                "14",
                "",
                "26",
                "b9UzeWaSs2u",
                "",
                "CivTksSoCt0"),
            (expectPostgis ? new HashSet<>() : Set.of(8, 9, 10, 11))));
  }
}
