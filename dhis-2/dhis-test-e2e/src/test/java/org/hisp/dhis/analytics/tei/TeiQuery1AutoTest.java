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
        "{\"pager\":{\"isLastPage\":false,\"pageSize\":10,\"page\":1},\"items\":{\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\"},{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"}},\"dimensions\":{\"lZGmxYbs97q\":[],\"zDhUuAYrxNC\":[],\"pe\":[],\"w75KJ2mc4zz\":[],\"a3kGcGDCuk6\":[],\"UXz7xuGCEhU\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
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
        "MCH Weight (g), Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        17,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rowContext
    validateRowContext(response, 1, 17, "NS");

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
        "{\"pager\":{\"isLastPage\":false,\"pageSize\":10,\"page\":1},\"items\":{\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"}},\"dimensions\":{\"lZGmxYbs97q\":[],\"zDhUuAYrxNC\":[],\"pe\":[],\"w75KJ2mc4zz\":[],\"a3kGcGDCuk6\":[],\"UXz7xuGCEhU\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU",
        "MCH Weight (g), Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        1,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rowContext
    validateRowContext(response, 1, 1, "NS");

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
            .add("desc=lastupdated")
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
        "{\"pager\":{\"isLastPage\":false,\"pageSize\":10,\"page\":1},\"items\":{\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\"},{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"}},\"dimensions\":{\"lZGmxYbs97q\":[],\"zDhUuAYrxNC\":[],\"pe\":[],\"w75KJ2mc4zz\":[],\"a3kGcGDCuk6\":[],\"UXz7xuGCEhU\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
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
        "MCH Weight (g), Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(
        response,
        17,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
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
            "Sierra Leone / Bo / Badija / Ngelehun CHC",
            "Filona",
            "Ryder",
            "Female",
            "",
            "4322",
            ""));
    validateRow(
        response,
        1,
        List.of(
            "lkuI9OgwfOc",
            "2017-01-20 10:41:45.624",
            "",
            "2017-01-20 10:37:57.638",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badija / Ngelehun CHC",
            "Frank",
            "Fjordsen",
            "Male",
            "",
            "3444",
            "5"));
    validateRow(
        response,
        2,
        List.of(
            "pybd813kIWx",
            "2017-01-20 10:40:31.913",
            "",
            "2017-01-20 10:40:31.623",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badija / Ngelehun CHC",
            "Gertrude",
            "Fjordsen",
            "Female",
            "",
            "3320",
            "5"));
    validateRow(
        response,
        3,
        List.of(
            "eHFFZnew6KJ",
            "2016-10-11 11:09:02.811",
            "",
            "2015-08-07 15:47:19.378",
            "",
            "",
            "",
            "",
            "",
            "Mapailleh MCHP",
            "OU_247072",
            "Sierra Leone / Moyamba / Kargboro / Mapailleh MCHP",
            "Paula",
            "Walker",
            "Female",
            "",
            "3444",
            "1"));
    validateRow(
        response,
        4,
        List.of(
            "PQfMcpmXeFE",
            "2016-08-03 23:49:43.309",
            "",
            "2014-03-06 05:49:28.256",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badija / Ngelehun CHC",
            "John",
            "Kelly",
            "Male",
            "",
            "",
            ""));
    validateRow(
        response,
        5,
        List.of(
            "EaOyKGOIGRp",
            "2016-08-03 23:47:14.517",
            "",
            "2014-11-16 11:38:23.592",
            "",
            "",
            "",
            "",
            "",
            "Ngelehun CHC",
            "OU_559",
            "Sierra Leone / Bo / Badija / Ngelehun CHC",
            "Anna",
            "Jones",
            "Female",
            "",
            "3243",
            "10"));
    validateRow(
        response,
        6,
        List.of(
            "zDFe02SbmSD",
            "2016-04-21 18:13:40.579",
            "",
            "2015-08-07 15:47:25.385",
            "",
            "",
            "",
            "",
            "",
            "Gelehun MCHP",
            "OU_222626",
            "Sierra Leone / Kenema / Small Bo / Gelehun MCHP",
            "Eric",
            "Robertson",
            "Male",
            "",
            "3706",
            "2"));
    validateRow(
        response,
        7,
        List.of(
            "lSxhGlVaTvy",
            "2016-04-21 16:01:20.435",
            "",
            "2015-08-06 21:20:49.454",
            "",
            "",
            "",
            "",
            "",
            "Masoko MCHP",
            "OU_268158",
            "Sierra Leone / Tonkolili / Kholifa Rowalla / Masoko MCHP",
            "Diane",
            "Bryant",
            "Female",
            "",
            "3779",
            "0"));
    validateRow(
        response,
        8,
        List.of(
            "gGDBG5aGlIk",
            "2015-08-07 15:47:29.301",
            "",
            "2015-08-07 15:47:29.301",
            "",
            "",
            "",
            "",
            "",
            "Tonko Maternity Clinic",
            "OU_193214",
            "Sierra Leone / Bombali / Bombali Sebora / Tonko Maternity Clinic",
            "Mark",
            "Coleman",
            "Male",
            "",
            "3297",
            "0"));
    validateRow(
        response,
        9,
        List.of(
            "IjhtI0sk0O6",
            "2015-08-07 15:47:29.3",
            "",
            "2015-08-07 15:47:29.3",
            "",
            "",
            "",
            "",
            "",
            "Banka Makuloh MCHP",
            "OU_211259",
            "Sierra Leone / Kambia / Masungbala / Banka Makuloh MCHP",
            "Deborah",
            "James",
            "Female",
            "",
            "2579",
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
        "{\"pager\":{\"isLastPage\":false,\"pageSize\":10,\"page\":1},\"items\":{\"IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"}},\"dimensions\":{\"lZGmxYbs97q\":[],\"zDhUuAYrxNC\":[],\"pe\":[],\"w75KJ2mc4zz\":[],\"a3kGcGDCuk6\":[],\"UXz7xuGCEhU\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score, Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rowContext
    validateRowContext(response, 1, 0, "NS");

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
