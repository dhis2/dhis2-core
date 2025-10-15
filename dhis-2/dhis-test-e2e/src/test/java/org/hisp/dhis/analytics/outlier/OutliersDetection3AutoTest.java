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
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsOutlierDetectionActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/** Groups e2e tests for "/analytics/outlierDetection" endpoint. */
@EnabledIf(value = "hasOutliersSupport", disabledReason = "outliers are only supported in Postgres")
public class OutliersDetection3AutoTest extends OutliersApiTest {
  private final AnalyticsOutlierDetectionActions actions = new AnalyticsOutlierDetectionActions();

  @Test
  public void queryOutliertest9() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("pe=THIS_YEAR")
            .add("ou=ImspTQPwCqd")
            .add("maxResults=10")
            .add("orderBy=MODIFIEDZSCORE")
            .add("threshold=2.5")
            .add("ds=BfMAe6Itzgt")
            .add("algorithm=MODIFIED_Z_SCORE")
            .add("relativePeriodDate=2022-07-26");

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
        "{\"count\":10,\"orderBy\":\"MODIFIED_Z_SCORE\",\"threshold\":\"2.5\",\"maxResults\":10,\"algorithm\":\"MODIFIED_Z_SCORE\"}";
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
        0,
        List.of(
            "tU7GixyHhsv",
            "Vitamin A given to < 5y",
            "202206",
            "202206",
            "scc4QyxenJd",
            "Makali CHC",
            "Sierra Leone / Tonkolili / Kunike Barina / Makali CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "714.0",
            "10.0",
            "3.0",
            "704.0",
            "158.28",
            "-1.12",
            "21.12"));
    validateRow(
        response,
        1,
        List.of(
            "lVsbKXoF0zX",
            "Weight for height below 70 percent",
            "202206",
            "202206",
            "Qc9lf4VM9bD",
            "Wellington Health Centre",
            "Sierra Leone / Western Area / Freetown / Wellington Health Centre",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "1540.0",
            "27.0",
            "7.0",
            "1513.0",
            "145.79",
            "1.05",
            "52.95"));
    validateRow(
        response,
        2,
        List.of(
            "bTcRDVjC66S",
            "Weight for age below lower line (red)",
            "202205",
            "202205",
            "OzjRQLn3G24",
            "Koidu Govt. Hospital",
            "Sierra Leone / Kono / Gbense / Koidu Govt. Hospital",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "400.0",
            "11.0",
            "2.0",
            "389.0",
            "131.19",
            "3.59",
            "18.41"));
    validateRow(
        response,
        3,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202208",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "451.0",
            "13.0",
            "3.0",
            "438.0",
            "98.48",
            "1.88",
            "24.12"));
    validateRow(
        response,
        4,
        List.of(
            "lVsbKXoF0zX",
            "Weight for height below 70 percent",
            "202205",
            "202205",
            "OzjRQLn3G24",
            "Koidu Govt. Hospital",
            "Sierra Leone / Kono / Gbense / Koidu Govt. Hospital",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "400.0",
            "11.0",
            "3.0",
            "389.0",
            "87.46",
            "-0.12",
            "22.12"));
    validateRow(
        response,
        5,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202202",
            "202202",
            "oLuhRyYPxRO",
            "Senehun CHC",
            "Sierra Leone / Moyamba / Kamaje / Senehun CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "130.0",
            "11.0",
            "1.0",
            "119.0",
            "80.27",
            "7.29",
            "14.71"));
    validateRow(
        response,
        6,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202208",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "430.0",
            "13.0",
            "4.0",
            "417.0",
            "70.32",
            "-1.83",
            "27.83"));
    validateRow(
        response,
        7,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202208",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "430.0",
            "14.0",
            "4.0",
            "416.0",
            "70.15",
            "-0.83",
            "28.83"));
    validateRow(
        response,
        8,
        List.of(
            "DUSpd8Jq3M7",
            "Newborn protected at birth against tetanus (TT2+)",
            "202207",
            "202207",
            "PQEpIeuSTCN",
            "Tobanda CHC",
            "Sierra Leone / Kenema / Small Bo / Tobanda CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "111.0",
            "10.0",
            "1.0",
            "101.0",
            "68.12",
            "6.29",
            "13.71"));
  }

  @Test
  public void queryOutliertest10() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("pe=LAST_YEAR")
            .add("ou=ImspTQPwCqd")
            .add("maxResults=20")
            .add("orderBy=MODIFIEDZSCORE")
            .add("threshold=2.5")
            .add("ds=BfMAe6Itzgt")
            .add("algorithm=MODIFIED_Z_SCORE")
            .add("relativePeriodDate=2022-07-26");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(18)))
        .body("rows", hasSize(equalTo(20)))
        .body("height", equalTo(20))
        .body("width", equalTo(18))
        .body("headerWidth", equalTo(18));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":20,\"orderBy\":\"MODIFIED_Z_SCORE\",\"threshold\":\"2.5\",\"maxResults\":20,\"algorithm\":\"MODIFIED_Z_SCORE\"}";
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
        0,
        List.of(
            "tU7GixyHhsv",
            "Vitamin A given to < 5y",
            "202106",
            "202106",
            "scc4QyxenJd",
            "Makali CHC",
            "Sierra Leone / Tonkolili / Kunike Barina / Makali CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "714.0",
            "10.0",
            "3.0",
            "704.0",
            "158.28",
            "-1.12",
            "21.12"));
    validateRow(
        response,
        1,
        List.of(
            "lVsbKXoF0zX",
            "Weight for height below 70 percent",
            "202106",
            "202106",
            "Qc9lf4VM9bD",
            "Wellington Health Centre",
            "Sierra Leone / Western Area / Freetown / Wellington Health Centre",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "1540.0",
            "27.0",
            "7.0",
            "1513.0",
            "145.79",
            "1.05",
            "52.95"));
    validateRow(
        response,
        2,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202111",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "Sierra Leone / Bo / Kakua / New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "616.0",
            "12.0",
            "3.0",
            "604.0",
            "135.8",
            "0.88",
            "23.12"));
    validateRow(
        response,
        3,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202110",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "598.0",
            "13.0",
            "3.0",
            "585.0",
            "131.53",
            "1.88",
            "24.12"));
    validateRow(
        response,
        4,
        List.of(
            "bTcRDVjC66S",
            "Weight for age below lower line (red)",
            "202105",
            "202105",
            "OzjRQLn3G24",
            "Koidu Govt. Hospital",
            "Sierra Leone / Kono / Gbense / Koidu Govt. Hospital",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "400.0",
            "11.0",
            "2.0",
            "389.0",
            "131.19",
            "3.59",
            "18.41"));
    validateRow(
        response,
        5,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202111",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "Sierra Leone / Bo / Kakua / New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "616.0",
            "13.0",
            "4.0",
            "603.0",
            "101.68",
            "-1.83",
            "27.83"));
    validateRow(
        response,
        6,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202110",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "600.0",
            "13.0",
            "4.0",
            "587.0",
            "98.98",
            "-1.83",
            "27.83"));
    validateRow(
        response,
        7,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202110",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "600.0",
            "14.0",
            "4.0",
            "586.0",
            "98.81",
            "-0.83",
            "28.83"));
    validateRow(
        response,
        8,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202108",
            "202108",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "451.0",
            "13.0",
            "3.0",
            "438.0",
            "98.48",
            "1.88",
            "24.12"));
    validateRow(
        response,
        9,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202111",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "Sierra Leone / Bo / Kakua / New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "580.0",
            "19.0",
            "4.0",
            "561.0",
            "94.6",
            "4.17",
            "33.83"));
    validateRow(
        response,
        10,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202111",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "Sierra Leone / Bo / Kakua / New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "547.0",
            "12.0",
            "4.0",
            "535.0",
            "90.21",
            "-2.83",
            "26.83"));
    validateRow(
        response,
        11,
        List.of(
            "lVsbKXoF0zX",
            "Weight for height below 70 percent",
            "202105",
            "202105",
            "OzjRQLn3G24",
            "Koidu Govt. Hospital",
            "Sierra Leone / Kono / Gbense / Koidu Govt. Hospital",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "400.0",
            "11.0",
            "3.0",
            "389.0",
            "87.46",
            "-0.12",
            "22.12"));
    validateRow(
        response,
        12,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202111",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "Sierra Leone / Bo / Kakua / New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "530.0",
            "12.0",
            "4.0",
            "518.0",
            "87.35",
            "-2.83",
            "26.83"));
    validateRow(
        response,
        13,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202111",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "Sierra Leone / Bo / Kakua / New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "530.0",
            "13.0",
            "4.0",
            "517.0",
            "87.18",
            "-1.83",
            "27.83"));
    validateRow(
        response,
        14,
        List.of(
            "x3Do5e7g4Qo",
            "OPV0 doses given",
            "202112",
            "202112",
            "ui12Hyvn6jR",
            "Wilberforce Military Hospital",
            "Sierra Leone / Western Area / Freetown / Wilberforce Military Hospital",
            "V6L425pT3A0",
            "Outreach, <1y",
            "HllvX50cXC0",
            "default",
            "510.0",
            "20.0",
            "4.0",
            "490.0",
            "82.63",
            "5.17",
            "34.83"));
    validateRow(
        response,
        15,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202102",
            "202102",
            "oLuhRyYPxRO",
            "Senehun CHC",
            "Sierra Leone / Moyamba / Kamaje / Senehun CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "130.0",
            "11.0",
            "1.0",
            "119.0",
            "80.27",
            "7.29",
            "14.71"));
    validateRow(
        response,
        16,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202110",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "598.0",
            "14.0",
            "5.0",
            "584.0",
            "78.78",
            "-4.53",
            "32.53"));
    validateRow(
        response,
        17,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202110",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "594.0",
            "12.0",
            "5.0",
            "582.0",
            "78.51",
            "-6.53",
            "30.53"));
    validateRow(
        response,
        18,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202110",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "596.0",
            "11.5",
            "5.5",
            "584.5",
            "71.68",
            "-8.89",
            "31.89"));
    validateRow(
        response,
        19,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202108",
            "202108",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "430.0",
            "13.0",
            "4.0",
            "417.0",
            "70.32",
            "-1.83",
            "27.83"));
  }

  @Test
  public void queryOutliertest11() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("endDate=2022-10-26")
            .add("ou=ImspTQPwCqd")
            .add("maxResults=5")
            .add("outputIdScheme=uid")
            .add("sortOrder=asc")
            .add("orderBy=mean")
            .add("startDate=2022-07-26")
            .add("ds=BfMAe6Itzgt")
            .add("algorithm=Z_SCORE");

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
        "{\"count\":3,\"orderBy\":\"MEAN\",\"threshold\":\"3.0\",\"maxResults\":5,\"algorithm\":\"Z_SCORE\"}";
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
            "18.26",
            "28.71",
            "86.74",
            "3.02",
            "-67.85",
            "104.38"));
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
            "41.64",
            "57.45",
            "178.36",
            "3.1",
            "-130.7",
            "213.99"));
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
            "48.19",
            "114.28",
            "417.81",
            "3.66",
            "-294.65",
            "391.03"));
  }

  @Test
  public void queryOutliertest12() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("endDate=2022-10-26")
            .add("ou=ImspTQPwCqd")
            .add("maxResults=5")
            .add("outputIdScheme=uid")
            .add("sortOrder=asc")
            .add("orderBy=stddev")
            .add("startDate=2022-07-26")
            .add("ds=BfMAe6Itzgt")
            .add("algorithm=Z_SCORE");

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
        "{\"count\":3,\"orderBy\":\"STD_DEV\",\"threshold\":\"3.0\",\"maxResults\":5,\"algorithm\":\"Z_SCORE\"}";
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
            "18.26",
            "28.71",
            "86.74",
            "3.02",
            "-67.85",
            "104.38"));
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
            "41.64",
            "57.45",
            "178.36",
            "3.1",
            "-130.7",
            "213.99"));
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
            "48.19",
            "114.28",
            "417.81",
            "3.66",
            "-294.65",
            "391.03"));
  }
}
