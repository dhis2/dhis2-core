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
import static org.hisp.dhis.analytics.ValidationHelper.validateRowContext;
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
public class TeiQuery1AutoTest extends AnalyticsApiTest {
  private AnalyticsTeiActions actions = new AnalyticsTeiActions();

  @Test
  public void queryTrackedentityquerywithrowcontext1() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("pageSize=10")
            .add("program=IpHINAT79UW")
            .add(
                "dimension=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add(
                "desc=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,lastupdated");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(18)))
        .body("rows", hasSize(equalTo(10)))
        .body("height", equalTo(10))
        .body("width", equalTo(18))
        .body("headerWidth", equalTo(18));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"lZGmxYbs97q\":{\"name\":\"Unique ID\"},\"zDhUuAYrxNC\":{\"name\":\"Last name\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"w75KJ2mc4zz\":{\"name\":\"First name\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"cejWyOfXge6\":{\"name\":\"Gender\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\"},{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"}},\"dimensions\":{\"lZGmxYbs97q\":[],\"zDhUuAYrxNC\":[],\"pe\":[],\"w75KJ2mc4zz\":[],\"a3kGcGDCuk6\":[],\"UXz7xuGCEhU\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "trackedentityinstanceuid",
        "Tracked entity instance",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        1,
        "lastupdated",
        "Last updated",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        2,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 3, "created", "Created", "DATETIME", "java.time.LocalDateTime", false, true);
    validateHeader(
        response, 4, "createdbydisplayname", "Created by", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 5, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 6, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 7, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(response, 8, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 9, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 10, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        11,
        "ounamehierarchy",
        "Organisation unit hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 12, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 13, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 14, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 15, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        16,
        "IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU",
        "MCH Weight (g)",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        17,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rowContext
    validateRowContext(response, 1, 17, "ND");

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "ebp9zX3uy7m",
            "2015-08-06 21:20:41.753",
            "",
            "2015-08-06 21:20:41.753",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Sharon",
            "Johnson",
            "Female",
            "",
            "36282",
            "3"));
    validateRow(
        response,
        1,
        List.of(
            "vOxUH373fy5",
            "2017-05-26 11:46:22.372",
            "",
            "2017-01-20 10:44:02.77",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Filona",
            "Ryder",
            "Female",
            "",
            "4322",
            ""));
    validateRow(
        response,
        2,
        List.of(
            "x2UnW32bNDR",
            "2014-11-15 21:22:30.301",
            "",
            "2014-11-15 21:22:30.204",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Jack",
            "Dean",
            "Male",
            "",
            "4210",
            "10"));
    validateRow(
        response,
        List.of(
            "UtDZmrX5lSd",
            "2014-11-15 21:20:06.375",
            "",
            "2014-11-15 21:20:06.274",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Tim",
            "Johnson",
            "Male",
            "",
            "4201",
            "8"));
    validateRow(
        response,
        List.of(
            "PgkxEogQBnX",
            "2014-11-15 19:12:23.235",
            "",
            "2014-11-15 19:12:23.127",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "Lily",
            "Matthews",
            "Female",
            "",
            "4201",
            "5"));
    validateRow(
        response,
        List.of(
            "D4SVdlwKuXe",
            "2015-08-07 15:47:20.218",
            "",
            "2015-08-07 15:47:20.218",
            "",
            "",
            "",
            "",
            "",
            "Konjo MCHP",
            "OU_222670",
            "Sierra Leone / Kenema / Lower Bambara / Konjo MCHP",
            "Bobby",
            "Foster",
            "Male",
            "",
            "3999",
            "2"));
  }

  @Test
  public void queryTrackedentityquerywithrowcontext2() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("pageSize=10")
            .add("program=IpHINAT79UW")
            .add(
                "dimension=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("desc=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"lZGmxYbs97q\":{\"name\":\"Unique ID\"},\"zDhUuAYrxNC\":{\"name\":\"Last name\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"w75KJ2mc4zz\":{\"name\":\"First name\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"cejWyOfXge6\":{\"name\":\"Gender\"}},\"dimensions\":{\"lZGmxYbs97q\":[],\"zDhUuAYrxNC\":[],\"pe\":[],\"w75KJ2mc4zz\":[],\"a3kGcGDCuk6\":[],\"UXz7xuGCEhU\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU",
        "MCH Weight (g)",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        1,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rowContext
    validateRowContext(response, 1, 1, "ND");

    // Assert rows.
    validateRow(response, 0, List.of("36282", "3"));
    validateRow(response, 1, List.of("4322", ""));
    validateRow(response, 2, List.of("4210", "10"));
    validateRow(response, 3, List.of("4201", "8"));
    validateRow(response, 4, List.of("4201", "5"));
    validateRow(response, 5, List.of("3999", "2"));
    validateRow(response, 6, List.of("3999", "2"));
    validateRow(response, 7, List.of("3999", "2"));
    validateRow(response, 8, List.of("3999", "2"));
    validateRow(response, 9, List.of("3999", "1"));
  }

  @Test
  public void queryTrackedentityquerywithrowcontext3() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("pageSize=10")
            .add("program=IpHINAT79UW")
            .add(
                "dimension=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(18)))
        .body("rows", hasSize(equalTo(10)))
        .body("height", equalTo(10))
        .body("width", equalTo(18))
        .body("headerWidth", equalTo(18));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"lZGmxYbs97q\":{\"name\":\"Unique ID\"},\"zDhUuAYrxNC\":{\"name\":\"Last name\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"w75KJ2mc4zz\":{\"name\":\"First name\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"cejWyOfXge6\":{\"name\":\"Gender\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\"},{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"}},\"dimensions\":{\"lZGmxYbs97q\":[],\"zDhUuAYrxNC\":[],\"pe\":[],\"w75KJ2mc4zz\":[],\"a3kGcGDCuk6\":[],\"UXz7xuGCEhU\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "trackedentityinstanceuid",
        "Tracked entity instance",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        1,
        "lastupdated",
        "Last updated",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        2,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 3, "created", "Created", "DATETIME", "java.time.LocalDateTime", false, true);
    validateHeader(
        response, 4, "createdbydisplayname", "Created by", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 5, "storedby", "Stored by", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 6, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 7, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(response, 8, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 9, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 10, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        11,
        "ounamehierarchy",
        "Organisation unit hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 12, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 13, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 14, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 15, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        16,
        "IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU",
        "MCH Weight (g)",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        17,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rowContext
    validateRowContext(response, 0, 0, null);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "A0EeVFFEUV6",
            "2015-08-06 21:20:48.576",
            "",
            "2015-08-06 21:20:48.576",
            "",
            "",
            "",
            "",
            "",
            "Pejewa CHC",
            "OU_204927",
            "Sierra Leone / Kailahun / Peje West / Pejewa CHC",
            "Nancy",
            "Jones",
            "Female",
            "",
            "2796",
            "0"));
    validateRow(
        response,
        1,
        List.of(
            "A0Hae4WYtQl",
            "2015-08-06 21:15:40.015",
            "",
            "2015-08-06 21:15:40.013",
            "",
            "",
            "",
            "",
            "",
            "Gbangba MCHP",
            "OU_8399",
            "Sierra Leone / Bo / Selenga / Gbangba MCHP",
            "James",
            "Jordan",
            "Male",
            "",
            "3418",
            "2"));
    validateRow(
        response,
        2,
        List.of(
            "A0TpTrpIoOK",
            "2015-08-06 21:20:51.879",
            "",
            "2015-08-06 21:20:51.879",
            "",
            "",
            "",
            "",
            "",
            "Massabendu CHP",
            "OU_233335",
            "Sierra Leone / Kono / Nimiyama / Massabendu CHP",
            "Jane",
            "Cox",
            "Female",
            "",
            "3740",
            "0"));
    validateRow(
        response,
        3,
        List.of(
            "A0UwTZLUnHY",
            "2015-08-06 21:15:41.923",
            "",
            "2015-08-06 21:15:41.922",
            "",
            "",
            "",
            "",
            "",
            "Mansundu MCHP",
            "OU_233396",
            "Sierra Leone / Kono / Nimikoro / Mansundu MCHP",
            "Martha",
            "Harvey",
            "Female",
            "",
            "3150",
            "0"));
    validateRow(
        response,
        4,
        List.of(
            "A0wAPfNmMkD",
            "2015-08-07 15:47:21.213",
            "",
            "2015-08-07 15:47:21.212",
            "",
            "",
            "",
            "",
            "",
            "Liya MCHP",
            "OU_260442",
            "Sierra Leone / Pujehun / Kpaka / Liya MCHP",
            "Joshua",
            "Wood",
            "Male",
            "",
            "3416",
            "0"));
    validateRow(
        response,
        5,
        List.of(
            "A0zUZPe4bk0",
            "2015-08-06 21:20:51.006",
            "",
            "2015-08-06 21:20:51.006",
            "",
            "",
            "",
            "",
            "",
            "SLRCS (Nongowa) clinic",
            "OU_222695",
            "Sierra Leone / Kenema / Nongowa / SLRCS (Nongowa) clinic",
            "James",
            "Jones",
            "Male",
            "",
            "3580",
            "1"));
    validateRow(
        response,
        6,
        List.of(
            "A12rcS9b7nG",
            "2015-08-07 15:47:24.148",
            "",
            "2015-08-07 15:47:24.147",
            "",
            "",
            "",
            "",
            "",
            "Mabonkanie MCHP",
            "OU_193270",
            "Sierra Leone / Bombali / Safroko Limba / Mabonkanie MCHP",
            "John",
            "Lynch",
            "Male",
            "",
            "3510",
            "0"));
    validateRow(
        response,
        7,
        List.of(
            "A16VGL32xEg",
            "2015-08-07 15:47:19.802",
            "",
            "2015-08-07 15:47:19.801",
            "",
            "",
            "",
            "",
            "",
            "Mabineh MCHP",
            "OU_268179",
            "Sierra Leone / Tonkolili / Kunike / Mabineh MCHP",
            "Arthur",
            "Alexander",
            "Male",
            "",
            "2502",
            "1"));
    validateRow(
        response,
        8,
        List.of(
            "A17SeLLte4D",
            "2015-08-07 15:47:21.607",
            "",
            "2015-08-07 15:47:21.606",
            "",
            "",
            "",
            "",
            "",
            "Modia MCHP",
            "OU_211233",
            "Sierra Leone / Kambia / Magbema / Modia MCHP",
            "Sandra",
            "Richards",
            "Female",
            "",
            "2510",
            "1"));
    validateRow(
        response,
        9,
        List.of(
            "A17oc8VDBjC",
            "2015-08-06 21:20:52.062",
            "",
            "2015-08-06 21:20:52.061",
            "",
            "",
            "",
            "",
            "",
            "Yoyema MCHP",
            "OU_247067",
            "Sierra Leone / Moyamba / Kaiyamba / Yoyema MCHP",
            "Dennis",
            "Washington",
            "Male",
            "",
            "2678",
            "0"));
  }

  @Test
  public void queryTrackedentityquerywithrowcontext4() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("pageSize=10")
            .add("program=IpHINAT79UW")
            .add(
                "dimension=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6")
            .add("desc=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(1)))
        .body("rows", hasSize(equalTo(10)))
        .body("height", equalTo(10))
        .body("width", equalTo(1))
        .body("headerWidth", equalTo(1));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"lZGmxYbs97q\":{\"name\":\"Unique ID\"},\"zDhUuAYrxNC\":{\"name\":\"Last name\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"w75KJ2mc4zz\":{\"name\":\"First name\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"cejWyOfXge6\":{\"name\":\"Gender\"}},\"dimensions\":{\"lZGmxYbs97q\":[],\"zDhUuAYrxNC\":[],\"pe\":[],\"w75KJ2mc4zz\":[],\"a3kGcGDCuk6\":[],\"UXz7xuGCEhU\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rowContext
    validateRowContext(response, 1, 0, "ND");

    // Assert rows.
    validateRow(response, 0, List.of("3"));
    validateRow(response, 1, List.of(""));
    validateRow(response, 2, List.of("10"));
    validateRow(response, 3, List.of("8"));
    validateRow(response, 4, List.of("5"));
    validateRow(response, 5, List.of("2"));
    validateRow(response, 6, List.of("2"));
    validateRow(response, 7, List.of("2"));
    validateRow(response, 8, List.of("2"));
    validateRow(response, 9, List.of("1"));
  }
}
