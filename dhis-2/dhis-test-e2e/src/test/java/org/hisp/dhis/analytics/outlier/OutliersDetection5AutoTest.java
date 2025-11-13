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
package org.hisp.dhis.analytics.outlier;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsOutlierDetectionActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/** Groups e2e tests for "/analytics/outlierDetection" endpoint. */
@EnabledIf(value = "hasOutliersSupport", disabledReason = "outliers are only supported in Postgres")
public class OutliersDetection5AutoTest extends AnalyticsApiTest {
  private final AnalyticsOutlierDetectionActions actions = new AnalyticsOutlierDetectionActions();

  @Test
  public void queryOutliertest17() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("endDate=2022-10-26")
            .add("outputIdScheme=uid")
            .add("sortOrder=asc")
            .add("orderBy=zscore")
            .add("threshold=3")
            .add("startDate=2022-07-26")
            .add("ds=BfMAe6Itzgt")
            .add("algorithm=Z_SCORE")
            .add("skipRounding=true")
            .add("relativePeriodDate=2022-07-01");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(18)))
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(18))
        .body("headerWidth", equalTo(18));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":3,\"orderBy\":\"Z_SCORE\",\"threshold\":\"3.0\",\"maxResults\":500,\"algorithm\":\"Z_SCORE\"}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 1, "dxname", "Data name", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 3, "pename", "Period name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 4, "ou", "Organisation unit", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 5, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        6,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(
        response, 7, "coc", "Category option combo", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        8,
        "cocname",
        "Category option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(
        response, 9, "aoc", "Attribute option combo", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        10,
        "aocname",
        "Attribute option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(response, 11, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 12, "mean", "Mean", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 13, "stddev", "Standard deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 14, "absdev", "Absolute deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 15, "zscore", "Z-score", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 16, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 17, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "dU0GquGkGQr",
            "Q_Early breastfeeding (within 1 hr after delivery) at BCG",
            "202209",
            "September 2022",
            "Mi4dWRtfIOC",
            "Sandaru CHC",
            "Sierra Leone / Kailahun / Penguia / Sandaru CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "105.0",
            "18.2635658915",
            "28.7055404478",
            "86.7364341085",
            "3.0215920953",
            "-67.8530554519",
            "104.3801872349"));
    validateRow(
        response,
        1,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202208",
            "August 2022",
            "CvBAqD6RzLZ",
            "Ngalu CHC",
            "Sierra Leone / Bo / Bargbe / Ngalu CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "220.0",
            "41.6440677966",
            "57.4495394538",
            "178.3559322034",
            "3.1045667885",
            "-130.7045505648",
            "213.992686158"));
    validateRow(
        response,
        2,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202209",
            "September 2022",
            "RhJbg8UD75Q",
            "Yemoh Town CHC",
            "Sierra Leone / Bo / Kakua / Yemoh Town CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "466.0",
            "48.1860465116",
            "114.2796629138",
            "417.8139534884",
            "3.6560656799",
            "-294.6529422297",
            "391.025035253"));
  }

  @Test
  public void queryOutliertest18() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "headers=dxname,cocname,pename,ouname,value,mean,zscore,stddev,lowerbound,upperbound")
            .add("dx=Jtf34kNZhzP,fbfJHSPpUQD,fbfJHSPpUQD.pq2XI5kz2BY")
            .add("pe=2022Nov")
            .add("displayProperty=NAME")
            .add("maxResults=5")
            .add("ou=USER_ORGUNIT")
            .add("sortOrder=desc")
            .add("orderBy=upperbound")
            .add("threshold=4")
            .add("algorithm=Z_SCORE");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(10)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(10))
        .body("headerWidth", equalTo(10));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":5,\"orderBy\":\"UPPER_BOUND\",\"threshold\":\"4.0\",\"maxResults\":5,\"algorithm\":\"Z_SCORE\"}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dxname", "Data name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        1,
        "cocname",
        "Category option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(response, 2, "pename", "Period name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 3, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 5, "mean", "Mean", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "zscore", "Z-score", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 7, "stddev", "Standard deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 8, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 9, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "ANC 1st visit",
            "Outreach",
            "202111",
            "Ngelehun CHC",
            "700.0",
            "40.37",
            "4.24",
            "155.5",
            "-581.61",
            "662.35"));
    validateRow(
        response,
        1,
        List.of(
            "ANC 1st visit",
            "Fixed",
            "202202",
            "Bumban MCHP",
            "34.0",
            "4.1",
            "4.24",
            "7.05",
            "-24.1",
            "32.3"));
    validateRow(
        response,
        2,
        List.of(
            "ANC 3rd visit",
            "Outreach",
            "202202",
            "Bumban MCHP",
            "23.0",
            "2.9",
            "4.25",
            "4.73",
            "-16.03",
            "21.83"));
    validateRow(
        response,
        3,
        List.of(
            "ANC 1st visit",
            "Fixed",
            "202111",
            "Njandama MCHP",
            "23.0",
            "5.88",
            "4.59",
            "3.73",
            "-9.06",
            "20.81"));
    validateRow(
        response,
        4,
        List.of(
            "ANC 3rd visit",
            "Fixed",
            "202201",
            "Bumban MCHP",
            "20.0",
            "3.55",
            "4.03",
            "4.08",
            "-12.77",
            "19.87"));
  }

  @Test
  public void queryOutliertest19() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "headers=dxname,cocname,pename,ouname,value,mean,zscore,stddev,lowerbound,upperbound")
            .add("dx=Jtf34kNZhzP,fbfJHSPpUQD,fbfJHSPpUQD.pq2XI5kz2BY")
            .add("pe=2022Q1")
            .add("displayProperty=NAME")
            .add("maxResults=5")
            .add("ou=USER_ORGUNIT")
            .add("sortOrder=desc")
            .add("orderBy=value")
            .add("threshold=3")
            .add("algorithm=Z_SCORE");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(10)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(10))
        .body("headerWidth", equalTo(10));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":5,\"orderBy\":\"VALUE\",\"threshold\":\"3.0\",\"maxResults\":5,\"algorithm\":\"Z_SCORE\"}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dxname", "Data name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        1,
        "cocname",
        "Category option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(response, 2, "pename", "Period name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 3, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 5, "mean", "Mean", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "zscore", "Z-score", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 7, "stddev", "Standard deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 8, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 9, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "ANC 3rd visit",
            "Fixed",
            "202201",
            "St Anthony clinic",
            "145.0",
            "52.33",
            "3.05",
            "30.43",
            "-38.96",
            "143.63"));
    validateRow(
        response,
        1,
        List.of(
            "ANC 1st visit",
            "Fixed",
            "202203",
            "Maforay (B. Sebora) MCHP",
            "92.0",
            "17.82",
            "3.14",
            "23.6",
            "-52.98",
            "88.61"));
    validateRow(
        response,
        2,
        List.of(
            "ANC 3rd visit",
            "Fixed",
            "202203",
            "Maforay (B. Sebora) MCHP",
            "64.0",
            "11.27",
            "3.16",
            "16.71",
            "-38.85",
            "61.39"));
    validateRow(
        response,
        3,
        List.of(
            "ANC 1st visit",
            "Outreach",
            "202201",
            "Tikonko (gaura) MCHP",
            "58.0",
            "12.75",
            "3.23",
            "14.03",
            "-29.34",
            "54.84"));
    validateRow(
        response,
        4,
        List.of(
            "ANC 1st visit",
            "Outreach",
            "202202",
            "Bumban MCHP",
            "45.0",
            "10.8",
            "3.6",
            "9.51",
            "-17.73",
            "39.33"));
  }

  @Test
  public void queryOutliertest20() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "headers=dxname,cocname,pename,ouname,value,mean,zscore,stddev,lowerbound,upperbound")
            .add("dx=Jtf34kNZhzP,fbfJHSPpUQD,fbfJHSPpUQD.pq2XI5kz2BY")
            .add("pe=2022AprilS1")
            .add("displayProperty=NAME")
            .add("maxResults=5")
            .add("ou=USER_ORGUNIT")
            .add("sortOrder=desc")
            .add("orderBy=value")
            .add("threshold=3")
            .add("algorithm=Z_SCORE");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(10)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(10))
        .body("headerWidth", equalTo(10));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":5,\"orderBy\":\"VALUE\",\"threshold\":\"3.0\",\"maxResults\":5,\"algorithm\":\"Z_SCORE\"}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dxname", "Data name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        1,
        "cocname",
        "Category option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(response, 2, "pename", "Period name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 3, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 5, "mean", "Mean", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "zscore", "Z-score", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 7, "stddev", "Standard deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 8, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 9, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "ANC 1st visit",
            "Fixed",
            "202205",
            "Ross Road Health Centre",
            "188.0",
            "72.67",
            "3.1",
            "37.26",
            "-39.1",
            "184.44"));
    validateRow(
        response,
        1,
        List.of(
            "ANC 1st visit",
            "Fixed",
            "202204",
            "Lyn Maternity MCHP",
            "121.0",
            "29.33",
            "3.14",
            "29.21",
            "-58.29",
            "116.95"));
    validateRow(
        response,
        2,
        List.of(
            "ANC 3rd visit",
            "Outreach",
            "202206",
            "Mondema CHC",
            "90.0",
            "14.82",
            "3.13",
            "24.0",
            "-57.19",
            "86.83"));
    validateRow(
        response,
        3,
        List.of(
            "ANC 1st visit",
            "Fixed",
            "202204",
            "Fogbo (WAR) MCHP",
            "84.0",
            "25.58",
            "3.25",
            "17.98",
            "-28.35",
            "79.52"));
    validateRow(
        response,
        4,
        List.of(
            "ANC 1st visit",
            "Fixed",
            "202205",
            "Gbanja Town MCHP",
            "35.0",
            "14.0",
            "3.15",
            "6.67",
            "-6.01",
            "34.01"));
  }
}
