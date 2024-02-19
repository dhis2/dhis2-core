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
public class OutliersDetection3AutoTest extends AnalyticsApiTest {
  private AnalyticsOutlierDetectionActions actions = new AnalyticsOutlierDetectionActions();

  @Test
  public void queryQueryoutliertest9() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("pe=THIS_YEAR")
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
            "158.3",
            "-559.9",
            "579.9"));
    validateRow(
        response,
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
            "145.8",
            "-1075.7",
            "1129.7"));
    validateRow(
        response,
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
            "131.2",
            "-399.5",
            "421.5"));
    validateRow(
        response,
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
            "98.5",
            "-474.5",
            "500.5"));
    validateRow(
        response,
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
            "87.5",
            "-416.7",
            "438.7"));
    validateRow(
        response,
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
            "80.3",
            "-84.3",
            "106.3"));
    validateRow(
        response,
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
            "70.3",
            "-505.3",
            "531.3"));
    validateRow(
        response,
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
            "70.1",
            "-463.2",
            "491.2"));
    validateRow(
        response,
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
            "68.1",
            "-63.4",
            "83.4"));
    validateRow(
        response,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202201",
            "202201",
            "agEKP19IUKI",
            "Tambiama CHC",
            "Sierra Leone / Bombali / Gbendembu Ngowahun / Tambiama CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "113.0",
            "14.0",
            "1.0",
            "99.0",
            "66.8",
            "-91.2",
            "119.2"));
    validateRow(
        response,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202202",
            "202202",
            "agEKP19IUKI",
            "Tambiama CHC",
            "Sierra Leone / Bombali / Gbendembu Ngowahun / Tambiama CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "113.0",
            "14.0",
            "1.0",
            "99.0",
            "66.8",
            "-91.2",
            "119.2"));
    validateRow(
        response,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202208",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "455.0",
            "12.0",
            "5.0",
            "443.0",
            "59.8",
            "-511.9",
            "535.9"));
    validateRow(
        response,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
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
            "14.0",
            "5.0",
            "437.0",
            "59.0",
            "-492.7",
            "520.7"));
    validateRow(
        response,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202208",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "457.0",
            "11.5",
            "5.5",
            "445.5",
            "54.6",
            "-548.6",
            "571.6"));
    validateRow(
        response,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202208",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "489.0",
            "20.0",
            "6.0",
            "469.0",
            "52.7",
            "-433.2",
            "473.2"));
    validateRow(
        response,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202209",
            "202209",
            "RhJbg8UD75Q",
            "Yemoh Town CHC",
            "Sierra Leone / Bo / Kakua / Yemoh Town CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "466.0",
            "14.0",
            "6.0",
            "452.0",
            "50.8",
            "-271.7",
            "299.7"));
    validateRow(
        response,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202209",
            "202209",
            "RhJbg8UD75Q",
            "Yemoh Town CHC",
            "Sierra Leone / Bo / Kakua / Yemoh Town CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "466.0",
            "14.0",
            "6.0",
            "452.0",
            "50.8",
            "-345.7",
            "373.7"));
    validateRow(
        response,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202208",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "Sierra Leone / Bo / Kakua / Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "457.0",
            "12.5",
            "6.0",
            "444.5",
            "50.0",
            "-574.4",
            "599.4"));
    validateRow(
        response,
        List.of(
            "Rmixc9wJl0G",
            "Q_LLITN given at time of 2nd Vit A dose",
            "202202",
            "202202",
            "EUUkKEDoNsf",
            "Wilberforce CHC",
            "Sierra Leone / Western Area / Freetown / Wilberforce CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "40.0",
            "5.5",
            "0.5",
            "34.5",
            "46.5",
            "-40.0",
            "51.0"));
    validateRow(
        response,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202201",
            "202201",
            "EXbPGmEUdnc",
            "Mateboi CHC",
            "Sierra Leone / Bombali / Sanda Tendaren / Mateboi CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "80.0",
            "11.0",
            "1.0",
            "69.0",
            "46.5",
            "-58.9",
            "80.9"));
  }

  @Test
  public void queryQueryoutliertest10() throws JSONException {
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
            "158.3",
            "-559.9",
            "579.9"));
    validateRow(
        response,
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
            "145.8",
            "-1075.7",
            "1129.7"));
    validateRow(
        response,
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
            "-461.0",
            "485.0"));
    validateRow(
        response,
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
            "131.5",
            "-474.5",
            "500.5"));
    validateRow(
        response,
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
            "131.2",
            "-399.5",
            "421.5"));
    validateRow(
        response,
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
            "101.7",
            "-433.2",
            "459.2"));
    validateRow(
        response,
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
            "99.0",
            "-505.3",
            "531.3"));
    validateRow(
        response,
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
            "98.8",
            "-463.2",
            "491.2"));
    validateRow(
        response,
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
            "98.5",
            "-474.5",
            "500.5"));
    validateRow(
        response,
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
            "-466.1",
            "504.1"));
    validateRow(
        response,
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
            "90.2",
            "-381.8",
            "405.8"));
    validateRow(
        response,
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
            "87.5",
            "-416.7",
            "438.7"));
    validateRow(
        response,
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
            "87.3",
            "-341.4",
            "365.4"));
    validateRow(
        response,
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
            "87.2",
            "-351.5",
            "377.5"));
    validateRow(
        response,
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
            "82.6",
            "-318.8",
            "358.8"));
    validateRow(
        response,
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
            "80.3",
            "-84.3",
            "106.3"));
    validateRow(
        response,
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
            "78.8",
            "-492.7",
            "520.7"));
    validateRow(
        response,
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
            "78.5",
            "-511.9",
            "535.9"));
    validateRow(
        response,
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
            "71.7",
            "-548.6",
            "571.6"));
    validateRow(
        response,
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
            "70.3",
            "-505.3",
            "531.3"));
  }

  @Test
  public void queryQueryoutliertest11() throws JSONException {
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
            "18.3",
            "28.7",
            "86.7",
            "3.0",
            "-67.9",
            "104.4"));
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
            "41.6",
            "57.4",
            "178.4",
            "3.1",
            "-130.7",
            "214.0"));
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
            "48.2",
            "114.3",
            "417.8",
            "3.7",
            "-294.7",
            "391.0"));
  }

  @Test
  public void queryQueryoutliertest12() throws JSONException {
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
            "18.3",
            "28.7",
            "86.7",
            "3.0",
            "-67.9",
            "104.4"));
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
            "41.6",
            "57.4",
            "178.4",
            "3.1",
            "-130.7",
            "214.0"));
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
            "48.2",
            "114.3",
            "417.8",
            "3.7",
            "-294.7",
            "391.0"));
  }
}
