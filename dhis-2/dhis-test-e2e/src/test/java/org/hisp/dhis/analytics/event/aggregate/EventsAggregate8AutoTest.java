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
package org.hisp.dhis.analytics.event.aggregate;

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

/** Groups e2e tests for "/events/aggregate" endpoint. */
public class EventsAggregate8AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void queryEnrollmentoutput4() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "filter=uvMKOn1oWvd.zgnTlAH4ZOk,uvMKOn1oWvd.Y14cBKFUsg4,uvMKOn1oWvd.ffbaoqebOT3,uvMKOn1oWvd.QRg7SZ6VOAV:LIKE:a")
            .add("stage=uvMKOn1oWvd")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("dimension=ou:ImspTQPwCqd,pe:2019;2020;2021;LAST_5_YEARS")
            .add("relativePeriodDate=2023-07-14");

    // When
    ApiResponse response = actions.aggregate().get("M3xtLkYBlKI", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ou\":{\"name\":\"Organisation unit\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"uvMKOn1oWvd.QRg7SZ6VOAV\":{\"name\":\"Local Focus ID\"},\"2020\":{\"name\":\"2020\"},\"uvMKOn1oWvd.zgnTlAH4ZOk\":{\"name\":\"Follow-up vector control action details\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"uvMKOn1oWvd.Y14cBKFUsg4\":{\"name\":\"Follow-up vector control action details 2\"},\"2019\":{\"name\":\"2019\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"2018\":{\"name\":\"2018\"},\"pe\":{\"name\":\"Period\"},\"uvMKOn1oWvd.ffbaoqebOT3\":{\"name\":\"Name of health facility catchment area\"},\"uvMKOn1oWvd\":{\"name\":\"Foci response\"},\"M3xtLkYBlKI\":{\"name\":\"Malaria focus investigation\"}},\"dimensions\":{\"Y14cBKFUsg4\":[],\"pe\":[\"2019\",\"2020\",\"2021\",\"2018\",\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"QRg7SZ6VOAV\":[\"like a\"],\"ffbaoqebOT3\":[],\"zgnTlAH4ZOk\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("2021", "ImspTQPwCqd", "1"));
  }

  @Test
  public void queryEnrollmentoutput5() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=WZbXY0S00lP.ZcBPrXKahq2:LIKE:1,WZbXY0S00lP.KmEUg2hHEtx")
            .add("stage=WZbXY0S00lP")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add(
                "dimension=ou:ImspTQPwCqd,pe:2019;2020;2021;LAST_5_YEARS,WZbXY0S00lP.sdchiIXIcCf,WZbXY0S00lP.xPTngRLQTnu,WZbXY0S00lP.zzGNbeMnTd6,WZbXY0S00lP.roKuXYfw1BW")
            .add("relativePeriodDate=2023-07-14");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(7)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(7))
        .body("headerWidth", equalTo(7));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"WZbXY0S00lP.sdchiIXIcCf\":{\"name\":\"WHOMCH Renal disease\"},\"ou\":{\"name\":\"Organisation unit\"},\"WZbXY0S00lP.roKuXYfw1BW\":{\"name\":\"WHOMCH Gestational age at visit\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"2020\":{\"name\":\"2020\"},\"WZbXY0S00lP.KmEUg2hHEtx\":{\"name\":\"Email address\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"WZbXY0S00lP.zzGNbeMnTd6\":{\"name\":\"WHOMCH Autoimmune disease\"},\"2019\":{\"name\":\"2019\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"2018\":{\"name\":\"2018\"},\"pe\":{\"name\":\"Period\"},\"WZbXY0S00lP.xPTngRLQTnu\":{\"name\":\"WHOMCH Other chronic condition\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"WZbXY0S00lP.ZcBPrXKahq2\":{\"name\":\"Postal code\"}},\"dimensions\":{\"WZbXY0S00lP.zzGNbeMnTd6\":[],\"WZbXY0S00lP.sdchiIXIcCf\":[],\"pe\":[\"2019\",\"2020\",\"2021\",\"2018\",\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"KmEUg2hHEtx\":[],\"WZbXY0S00lP.xPTngRLQTnu\":[],\"WZbXY0S00lP.roKuXYfw1BW\":[],\"ZcBPrXKahq2\":[\"like 1\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "WZbXY0S00lP.zzGNbeMnTd6",
        "WHOMCH Autoimmune disease",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        1,
        "WZbXY0S00lP.sdchiIXIcCf",
        "WHOMCH Renal disease",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        2,
        "WZbXY0S00lP.xPTngRLQTnu",
        "WHOMCH Other chronic condition",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        3,
        "WZbXY0S00lP.roKuXYfw1BW",
        "WHOMCH Gestational age at visit",
        "INTEGER",
        "java.lang.Integer",
        false,
        true);
    validateHeader(response, 4, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 5, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 6, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("", "", "", "156", "2022", "ImspTQPwCqd", "1"));
  }

  @Test
  public void queryEnrollmentoutput6() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=bbKtnxRZKEP")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("dimension=ou:ImspTQPwCqd,pe:LAST_5_YEARS")
            .add("relativePeriodDate=2023-07-14");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

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
        "{\"items\":{\"2019\":{\"name\":\"2019\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"2018\":{\"name\":\"2018\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"pe\":{\"name\":\"Period\"},\"ou\":{\"name\":\"Organisation unit\"},\"2022\":{\"name\":\"2022\"},\"2021\":{\"name\":\"2021\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"2020\":{\"name\":\"2020\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"}},\"dimensions\":{\"pe\":[\"2018\",\"2019\",\"2020\",\"2021\",\"2022\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("2021", "ImspTQPwCqd", "697"));
    validateRow(response, List.of("2022", "ImspTQPwCqd", "988"));
  }
}
