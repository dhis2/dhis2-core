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
package org.hisp.dhis.analytics.outlier;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsOutlierDetectionActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/analytics/outlierDetection" endpoint. */
public class OutliersDetection4AutoTest extends AnalyticsApiTest {
  private AnalyticsOutlierDetectionActions actions = new AnalyticsOutlierDetectionActions();

  @Test
  public void queryOutliertest13() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "dx=FHD3wiSM7Sn.wHBMVthqIX4,FHD3wiSM7Sn.SdOUI2yT46H,FHD3wiSM7Sn.jOkIbJVhECg,fbfJHSPpUQD,cYeuwXTCPkU,Jtf34kNZhzP")
            .add("pe=LAST_12_MONTHS")
            .add("displayProperty=NAME")
            .add("maxResults=10")
            .add("ou=USER_ORGUNIT")
            .add("sortOrder=desc")
            .add("orderBy=value")
            .add("threshold=3")
            .add("algorithm=MODIFIED_Z_SCORE")
            .add("relativePeriodDate=2022-07-01");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

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
        "{\"count\":10,\"orderBy\":\"VALUE\",\"threshold\":\"3.0\",\"maxResults\":10,\"algorithm\":\"MODIFIED_Z_SCORE\"}";
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
    validateHeader(response, 12, "median", "Median", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response,
        13,
        "medianabsdeviation",
        "Median absolute deviation",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response, 14, "absdev", "Absolute deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response,
        15,
        "modifiedzscore",
        "Modified Z-score",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response, 16, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 17, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        List.of(
            "Jtf34kNZhzP",
            "ANC 3rd visit",
            "202111",
            "November 2021",
            "cJkZLwhL8RP",
            "Kasse MCHP",
            "Sierra Leone / Bo / Bargbo / Kasse MCHP",
            "PT59n8BQbqM",
            "Outreach",
            "HllvX50cXC0",
            "default",
            "1926.0",
            "16.0",
            "6.0",
            "1910.0",
            "214.7",
            "-1568.6",
            "1600.6"));
    validateRow(
        response,
        List.of(
            "cYeuwXTCPkU",
            "ANC 2nd visit",
            "202111",
            "November 2021",
            "vSbt6cezomG",
            "UMC (Urban Centre) Hospital",
            "Sierra Leone / Western Area / Freetown / UMC (Urban Centre) Hospital",
            "pq2XI5kz2BY",
            "Fixed",
            "HllvX50cXC0",
            "default",
            "1669.0",
            "749.0",
            "159.0",
            "920.0",
            "3.9",
            "-290.9",
            "1788.9"));
    validateRow(
        response,
        List.of(
            "cYeuwXTCPkU",
            "ANC 2nd visit",
            "202206",
            "June 2022",
            "aBfyTU5Wgds",
            "Nduvuibu MCHP",
            "Sierra Leone / Bo / Kakua / Nduvuibu MCHP",
            "pq2XI5kz2BY",
            "Fixed",
            "HllvX50cXC0",
            "default",
            "914.0",
            "60.5",
            "21.5",
            "853.5",
            "26.8",
            "-651.3",
            "772.3"));
    validateRow(
        response,
        List.of(
            "fbfJHSPpUQD",
            "ANC 1st visit",
            "202111",
            "November 2021",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "PT59n8BQbqM",
            "Outreach",
            "HllvX50cXC0",
            "default",
            "700.0",
            "3.0",
            "1.0",
            "697.0",
            "470.1",
            "-463.5",
            "469.5"));
    validateRow(
        response,
        List.of(
            "cYeuwXTCPkU",
            "ANC 2nd visit",
            "202111",
            "November 2021",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "PT59n8BQbqM",
            "Outreach",
            "HllvX50cXC0",
            "default",
            "657.0",
            "4.0",
            "2.0",
            "653.0",
            "220.2",
            "-432.3",
            "440.3"));
    validateRow(
        response,
        List.of(
            "FHD3wiSM7Sn",
            "ARI treated with antibiotics (pneumonia) follow-up",
            "202110",
            "October 2021",
            "Qc9lf4VM9bD",
            "Wellington Health Centre",
            "Sierra Leone / Western Area / Freetown / Wellington Health Centre",
            "wHBMVthqIX4",
            "12-59m",
            "HllvX50cXC0",
            "default",
            "500.0",
            "75.0",
            "47.0",
            "425.0",
            "6.1",
            "-377.8",
            "527.8"));
    validateRow(
        response,
        List.of(
            "fbfJHSPpUQD",
            "ANC 1st visit",
            "202205",
            "May 2022",
            "XQudzejlhJZ",
            "UFC Nongowa",
            "Sierra Leone / Kenema / Nongowa / UFC Nongowa",
            "pq2XI5kz2BY",
            "Fixed",
            "HllvX50cXC0",
            "default",
            "444.0",
            "163.5",
            "29.0",
            "280.5",
            "6.5",
            "-120.8",
            "447.8"));
    validateRow(
        response,
        List.of(
            "cYeuwXTCPkU",
            "ANC 2nd visit",
            "202205",
            "May 2022",
            "ui12Hyvn6jR",
            "Wilberforce Military Hospital",
            "Sierra Leone / Western Area / Freetown / Wilberforce Military Hospital",
            "pq2XI5kz2BY",
            "Fixed",
            "HllvX50cXC0",
            "default",
            "351.0",
            "90.5",
            "41.0",
            "260.5",
            "4.3",
            "-163.1",
            "344.1"));
    validateRow(
        response,
        List.of(
            "fbfJHSPpUQD",
            "ANC 1st visit",
            "202205",
            "May 2022",
            "Qc9lf4VM9bD",
            "Wellington Health Centre",
            "Sierra Leone / Western Area / Freetown / Wellington Health Centre",
            "pq2XI5kz2BY",
            "Fixed",
            "HllvX50cXC0",
            "default",
            "339.0",
            "122.5",
            "29.5",
            "216.5",
            "5.0",
            "-81.6",
            "326.6"));
    validateRow(
        response,
        List.of(
            "FHD3wiSM7Sn",
            "ARI treated with antibiotics (pneumonia) follow-up",
            "202111",
            "November 2021",
            "aHs9PLxIdbr",
            "Mayepoh CHC",
            "Sierra Leone / Tonkolili / Gbonkonlenken / Mayepoh CHC",
            "wHBMVthqIX4",
            "12-59m",
            "HllvX50cXC0",
            "default",
            "323.0",
            "62.5",
            "13.0",
            "260.5",
            "13.5",
            "-285.0",
            "410.0"));
  }

  @Test
  public void queryOutliertest14() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dx=FHD3wiSM7Sn.wHBMVthqIX4,FHD3wiSM7Sn.SdOUI2yT46H,FHD3wiSM7Sn.jOkIbJVhECg")
            .add("pe=LAST_12_MONTHS")
            .add("displayProperty=NAME")
            .add("maxResults=5")
            .add("ou=USER_ORGUNIT")
            .add("sortOrder=desc")
            .add("orderBy=value")
            .add("threshold=3")
            .add("algorithm=MODIFIED_Z_SCORE")
            .add("relativePeriodDate=2022-07-01");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(18)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(18))
        .body("headerWidth", equalTo(18));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":5,\"orderBy\":\"VALUE\",\"threshold\":\"3.0\",\"maxResults\":5,\"algorithm\":\"MODIFIED_Z_SCORE\"}";
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
    validateHeader(response, 12, "median", "Median", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response,
        13,
        "medianabsdeviation",
        "Median absolute deviation",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response, 14, "absdev", "Absolute deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response,
        15,
        "modifiedzscore",
        "Modified Z-score",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response, 16, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 17, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        List.of(
            "FHD3wiSM7Sn",
            "ARI treated with antibiotics (pneumonia) follow-up",
            "202110",
            "October 2021",
            "Qc9lf4VM9bD",
            "Wellington Health Centre",
            "Sierra Leone / Western Area / Freetown / Wellington Health Centre",
            "wHBMVthqIX4",
            "12-59m",
            "HllvX50cXC0",
            "default",
            "500.0",
            "75.0",
            "47.0",
            "425.0",
            "6.1",
            "-377.8",
            "527.8"));
    validateRow(
        response,
        List.of(
            "FHD3wiSM7Sn",
            "ARI treated with antibiotics (pneumonia) follow-up",
            "202111",
            "November 2021",
            "aHs9PLxIdbr",
            "Mayepoh CHC",
            "Sierra Leone / Tonkolili / Gbonkonlenken / Mayepoh CHC",
            "wHBMVthqIX4",
            "12-59m",
            "HllvX50cXC0",
            "default",
            "323.0",
            "62.5",
            "13.0",
            "260.5",
            "13.5",
            "-285.0",
            "410.0"));
    validateRow(
        response,
        List.of(
            "FHD3wiSM7Sn",
            "ARI treated with antibiotics (pneumonia) follow-up",
            "202112",
            "December 2021",
            "Qc9lf4VM9bD",
            "Wellington Health Centre",
            "Sierra Leone / Western Area / Freetown / Wellington Health Centre",
            "wHBMVthqIX4",
            "12-59m",
            "HllvX50cXC0",
            "default",
            "296.0",
            "75.0",
            "47.0",
            "221.0",
            "3.2",
            "-377.8",
            "527.8"));
    validateRow(
        response,
        List.of(
            "FHD3wiSM7Sn",
            "ARI treated with antibiotics (pneumonia) follow-up",
            "202107",
            "July 2021",
            "FclfbEFMcf3",
            "Kissy Health Centre",
            "Sierra Leone / Western Area / Freetown / Kissy Health Centre",
            "wHBMVthqIX4",
            "12-59m",
            "HllvX50cXC0",
            "default",
            "195.0",
            "12.5",
            "11.0",
            "182.5",
            "11.2",
            "-149.6",
            "174.6"));
    validateRow(
        response,
        List.of(
            "FHD3wiSM7Sn",
            "ARI treated with antibiotics (pneumonia) follow-up",
            "202205",
            "May 2022",
            "VeXU3mndzri",
            "Magbengbeh MCHP",
            "Sierra Leone / Kambia / Gbinleh Dixion / Magbengbeh MCHP",
            "wHBMVthqIX4",
            "12-59m",
            "HllvX50cXC0",
            "default",
            "130.0",
            "6.0",
            "4.0",
            "124.0",
            "20.9",
            "-123.7",
            "135.7"));
  }

  @Test
  public void queryOutliertest15() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dx=fbfJHSPpUQD,cYeuwXTCPkU,Jtf34kNZhzP")
            .add("pe=LAST_12_MONTHS")
            .add("displayProperty=NAME")
            .add("maxResults=10")
            .add("ou=USER_ORGUNIT")
            .add("sortOrder=desc")
            .add("orderBy=value")
            .add("threshold=3")
            .add("algorithm=MODIFIED_Z_SCORE")
            .add("relativePeriodDate=2022-07-01");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

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
        "{\"count\":10,\"orderBy\":\"VALUE\",\"threshold\":\"3.0\",\"maxResults\":10,\"algorithm\":\"MODIFIED_Z_SCORE\"}";
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
    validateHeader(response, 12, "median", "Median", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response,
        13,
        "medianabsdeviation",
        "Median absolute deviation",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response, 14, "absdev", "Absolute deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response,
        15,
        "modifiedzscore",
        "Modified Z-score",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response, 16, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 17, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        List.of(
            "Jtf34kNZhzP",
            "ANC 3rd visit",
            "202111",
            "November 2021",
            "cJkZLwhL8RP",
            "Kasse MCHP",
            "Sierra Leone / Bo / Bargbo / Kasse MCHP",
            "PT59n8BQbqM",
            "Outreach",
            "HllvX50cXC0",
            "default",
            "1926.0",
            "16.0",
            "6.0",
            "1910.0",
            "214.7",
            "-1568.6",
            "1600.6"));
    validateRow(
        response,
        List.of(
            "cYeuwXTCPkU",
            "ANC 2nd visit",
            "202111",
            "November 2021",
            "vSbt6cezomG",
            "UMC (Urban Centre) Hospital",
            "Sierra Leone / Western Area / Freetown / UMC (Urban Centre) Hospital",
            "pq2XI5kz2BY",
            "Fixed",
            "HllvX50cXC0",
            "default",
            "1669.0",
            "749.0",
            "159.0",
            "920.0",
            "3.9",
            "-290.9",
            "1788.9"));
    validateRow(
        response,
        List.of(
            "cYeuwXTCPkU",
            "ANC 2nd visit",
            "202206",
            "June 2022",
            "aBfyTU5Wgds",
            "Nduvuibu MCHP",
            "Sierra Leone / Bo / Kakua / Nduvuibu MCHP",
            "pq2XI5kz2BY",
            "Fixed",
            "HllvX50cXC0",
            "default",
            "914.0",
            "60.5",
            "21.5",
            "853.5",
            "26.8",
            "-651.3",
            "772.3"));
    validateRow(
        response,
        List.of(
            "fbfJHSPpUQD",
            "ANC 1st visit",
            "202111",
            "November 2021",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "PT59n8BQbqM",
            "Outreach",
            "HllvX50cXC0",
            "default",
            "700.0",
            "3.0",
            "1.0",
            "697.0",
            "470.1",
            "-463.5",
            "469.5"));
    validateRow(
        response,
        List.of(
            "cYeuwXTCPkU",
            "ANC 2nd visit",
            "202111",
            "November 2021",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "Sierra Leone / Bo / Badjia / Ngelehun CHC",
            "PT59n8BQbqM",
            "Outreach",
            "HllvX50cXC0",
            "default",
            "657.0",
            "4.0",
            "2.0",
            "653.0",
            "220.2",
            "-432.3",
            "440.3"));
    validateRow(
        response,
        List.of(
            "fbfJHSPpUQD",
            "ANC 1st visit",
            "202205",
            "May 2022",
            "XQudzejlhJZ",
            "UFC Nongowa",
            "Sierra Leone / Kenema / Nongowa / UFC Nongowa",
            "pq2XI5kz2BY",
            "Fixed",
            "HllvX50cXC0",
            "default",
            "444.0",
            "163.5",
            "29.0",
            "280.5",
            "6.5",
            "-120.8",
            "447.8"));
    validateRow(
        response,
        List.of(
            "cYeuwXTCPkU",
            "ANC 2nd visit",
            "202205",
            "May 2022",
            "ui12Hyvn6jR",
            "Wilberforce Military Hospital",
            "Sierra Leone / Western Area / Freetown / Wilberforce Military Hospital",
            "pq2XI5kz2BY",
            "Fixed",
            "HllvX50cXC0",
            "default",
            "351.0",
            "90.5",
            "41.0",
            "260.5",
            "4.3",
            "-163.1",
            "344.1"));
    validateRow(
        response,
        List.of(
            "fbfJHSPpUQD",
            "ANC 1st visit",
            "202205",
            "May 2022",
            "Qc9lf4VM9bD",
            "Wellington Health Centre",
            "Sierra Leone / Western Area / Freetown / Wellington Health Centre",
            "pq2XI5kz2BY",
            "Fixed",
            "HllvX50cXC0",
            "default",
            "339.0",
            "122.5",
            "29.5",
            "216.5",
            "5.0",
            "-81.6",
            "326.6"));
    validateRow(
        response,
        List.of(
            "fbfJHSPpUQD",
            "ANC 1st visit",
            "202205",
            "May 2022",
            "VhRX5JDVo7R",
            "Waterloo CHC",
            "Sierra Leone / Western Area / Rural Western Area / Waterloo CHC",
            "pq2XI5kz2BY",
            "Fixed",
            "HllvX50cXC0",
            "default",
            "318.0",
            "170.0",
            "24.5",
            "148.0",
            "4.1",
            "16.6",
            "323.4"));
    validateRow(
        response,
        List.of(
            "fbfJHSPpUQD",
            "ANC 1st visit",
            "202205",
            "May 2022",
            "FclfbEFMcf3",
            "Kissy Health Centre",
            "Sierra Leone / Western Area / Freetown / Kissy Health Centre",
            "pq2XI5kz2BY",
            "Fixed",
            "HllvX50cXC0",
            "default",
            "269.0",
            "145.0",
            "20.0",
            "124.0",
            "4.2",
            "21.4",
            "268.6"));
  }

  @Test
  public void queryOutliertest16() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=dx,dxname,pename,pe,zscore,lowerbound")
            .add("endDate=2022-10-26")
            .add("ou=ImspTQPwCqd")
            .add("maxResults=5")
            .add("outputIdScheme=uid")
            .add("sortOrder=asc")
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
        .body("headers", hasSize(equalTo(6)))
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(6))
        .body("headerWidth", equalTo(6));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":3,\"orderBy\":\"ABS_DEV\",\"threshold\":\"3.0\",\"maxResults\":5,\"algorithm\":\"Z_SCORE\"}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 1, "dxname", "Data name", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 2, "pename", "Period name", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 4, "zscore", "Z-score", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 5, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        List.of(
            "dU0GquGkGQr",
            "Q_Early breastfeeding (within 1 hr after delivery) at BCG",
            "September 2022",
            "202209",
            "3.0215920953",
            "-67.8530554519"));
    validateRow(
        response,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "August 2022",
            "202208",
            "3.1045667885",
            "-130.7045505648"));
    validateRow(
        response,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "September 2022",
            "202209",
            "3.6560656799",
            "-294.6529422297"));
  }

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
}
