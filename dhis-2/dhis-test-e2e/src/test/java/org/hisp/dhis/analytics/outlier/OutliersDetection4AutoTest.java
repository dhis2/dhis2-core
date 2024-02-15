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
            "214.71583",
            "-1568.56161",
            "1600.56161"));
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
            "3.90277",
            "-290.93014",
            "1788.93014"));
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
            "26.77608",
            "-651.2975",
            "772.2975"));
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
            "470.1265",
            "-463.48562",
            "469.48562"));
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
            "220.22425",
            "-432.28176",
            "440.28176"));
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
            "6.0992",
            "-377.7511",
            "527.7511"));
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
            "6.52404",
            "-120.83431",
            "447.83431"));
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
            "4.28554",
            "-163.09749",
            "344.09749"));
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
            "4.95014",
            "-81.56433",
            "326.56433"));
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
            "13.51594",
            "-285.01475",
            "410.01475"));
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
            "6.0992",
            "-377.7511",
            "527.7511"));
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
            "13.51594",
            "-285.01475",
            "410.01475"));
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
            "3.17159",
            "-377.7511",
            "527.7511"));
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
            "11.19057",
            "-149.64558",
            "174.64558"));
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
            "20.9095",
            "-123.73049",
            "135.73049"));
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
            "214.71583",
            "-1568.56161",
            "1600.56161"));
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
            "3.90277",
            "-290.93014",
            "1788.93014"));
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
            "26.77608",
            "-651.2975",
            "772.2975"));
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
            "470.1265",
            "-463.48562",
            "469.48562"));
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
            "220.22425",
            "-432.28176",
            "440.28176"));
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
            "6.52404",
            "-120.83431",
            "447.83431"));
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
            "4.28554",
            "-163.09749",
            "344.09749"));
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
            "4.95014",
            "-81.56433",
            "326.56433"));
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
            "4.07453",
            "16.61588",
            "323.38412"));
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
            "4.1819",
            "21.40313",
            "268.59687"));
  }
}
