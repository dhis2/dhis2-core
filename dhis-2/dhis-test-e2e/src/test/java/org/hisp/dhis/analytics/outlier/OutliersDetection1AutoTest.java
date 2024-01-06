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
public class OutliersDetection1AutoTest extends AnalyticsApiTest {
  private AnalyticsOutlierDetectionActions actions = new AnalyticsOutlierDetectionActions();

  @Test
  public void queryOutlierTest1() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("endDate=2024-01-02")
            .add("ou=O6uvpzGd5pu,fdc6uOvgoji")
            .add("maxResults=500")
            .add("sortOrder=ASC")
            .add("orderBy=value")
            .add("threshold=3")
            .add("startDate=2022-06-01")
            .add("ds=BfMAe6Itzgt")
            .add("algorithm=Z_SCORE");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":5,\"orderBy\":\"value\",\"threshold\":\"3.0\",\"maxResults\":500,\"algorithm\":\"Z_SCORE\"}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 1, "dxname", "Data name", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 3, "ou", "Organisation unit", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 4, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        5,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(
        response, 6, "coc", "Category option combo", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        7,
        "cocname",
        "Category option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(
        response, 8, "aoc", "Attribute option combo", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        9,
        "aocname",
        "Attribute option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(response, 10, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 11, "mean", "Mean", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 12, "stddev", "Standard deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 13, "absdev", "Absolute deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 14, "zscore", "zScore", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 15, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 16, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202207",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "V6L425pT3A0",
            "Outreach, <1y",
            "HllvX50cXC0",
            "default",
            "23.0",
            "7.548387096774194",
            "5.148370927292904",
            "15.451612903225806",
            "3.0012625588635498",
            "-7.896725685104517",
            "22.993499878652905"));
    validateRow(
        response,
        1,
        List.of(
            "pnL2VG8Bn7N",
            "Weight for height 70-79 percent",
            "202207",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "120.0",
            "28.96124031007752",
            "28.868158768956732",
            "91.03875968992247",
            "3.153604648586756",
            "-57.643235996792676",
            "115.56571661694773"));
    validateRow(
        response,
        2,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202208",
            "CvBAqD6RzLZ",
            "Ngalu CHC",
            "/Sierra Leone/Bo/Bargbe/Ngalu CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "220.0",
            "41.644067796610166",
            "57.44953945379397",
            "178.35593220338984",
            "3.104566788509063",
            "-130.70455056477175",
            "213.99268615799207"));
    validateRow(
        response,
        3,
        List.of(
            "x3Do5e7g4Qo",
            "OPV0 doses given",
            "202207",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "245.0",
            "43.018181818181816",
            "64.62014628415139",
            "201.9818181818182",
            "3.1256787518501157",
            "-150.84225703427234",
            "236.878620670636"));
    validateRow(
        response,
        4,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202209",
            "RhJbg8UD75Q",
            "Yemoh Town CHC",
            "/Sierra Leone/Bo/Kakua/Yemoh Town CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "466.0",
            "48.18604651162791",
            "114.2796629137825",
            "417.8139534883721",
            "3.656065679889071",
            "-294.6529422297196",
            "391.02503525297544"));
  }

  @Test
  public void queryOutlierTest2() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("endDate=2023-01-02")
            .add("ou=O6uvpzGd5pu,fdc6uOvgoji")
            .add("maxResults=35")
            .add("orderBy=mean")
            .add("threshold=5")
            .add("startDate=2020-10-01")
            .add("ds=BfMAe6Itzgt")
            .add("algorithm=Z_SCORE");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(17));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":0,\"orderBy\":\"middle_value\",\"threshold\":\"5.0\",\"maxResults\":35,\"algorithm\":\"Z_SCORE\"}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 1, "dxname", "Data name", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 3, "ou", "Organisation unit", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 4, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        5,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(
        response, 6, "coc", "Category option combo", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        7,
        "cocname",
        "Category option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(
        response, 8, "aoc", "Attribute option combo", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        9,
        "aocname",
        "Attribute option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(response, 10, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 11, "mean", "Mean", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 12, "stddev", "Standard deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 13, "absdev", "Absolute deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 14, "zscore", "zScore", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 15, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 16, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
  }

  @Test
  public void queryOutlierTest3() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("endDate=2023-01-02")
            .add("ou=O6uvpzGd5pu,fdc6uOvgoji")
            .add("maxResults=100")
            .add("orderBy=median")
            .add("threshold=10")
            .add("startDate=2020-10-01")
            .add("ds=BfMAe6Itzgt")
            .add("algorithm=MOD_Z_SCORE");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":100,\"orderBy\":\"middle_value\",\"threshold\":\"10.0\",\"maxResults\":100,\"algorithm\":\"MOD_Z_SCORE\"}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 1, "dxname", "Data name", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 3, "ou", "Organisation unit", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 4, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        5,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(
        response, 6, "coc", "Category option combo", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        7,
        "cocname",
        "Category option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(
        response, 8, "aoc", "Attribute option combo", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        9,
        "aocname",
        "Attribute option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(response, 10, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 11, "median", "Median", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response,
        12,
        "medianabsdeviation",
        "Median absolute deviation",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response, 13, "absdev", "Absolute deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response,
        14,
        "modifiedzscore",
        "Modified zScore",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response, 15, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 16, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "z7duEFcpApd",
            "LLITN given at Penta3",
            "202010",
            "vELbGdEphPd",
            "Jimmi CHC",
            "/Sierra Leone/Bo/Bargbo/Jimmi CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "243.0",
            "39.0",
            "11.0",
            "204.0",
            "12.50890909090909",
            "-1155.3024226065986",
            "1233.3024226065986"));
    validateRow(
        response,
        1,
        List.of(
            "z7duEFcpApd",
            "LLITN given at Penta3",
            "202111",
            "vELbGdEphPd",
            "Jimmi CHC",
            "/Sierra Leone/Bo/Bargbo/Jimmi CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "238.0",
            "39.0",
            "11.0",
            "199.0",
            "12.202318181818184",
            "-1155.3024226065988",
            "1233.3024226065988"));
    validateRow(
        response,
        2,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202202",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "323.0",
            "31.5",
            "8.0",
            "291.5",
            "24.57709375",
            "-1030.0984279482623",
            "1093.0984279482623"));
    validateRow(
        response,
        3,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202201",
            "NjyJYiIuKIG",
            "Kathanta Yimbor CHC",
            "/Sierra Leone/Bombali/Sella Limba/Kathanta Yimbor CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "130.0",
            "31.0",
            "2.0",
            "99.0",
            "33.38775",
            "-300.6221960322691",
            "362.6221960322691"));
    validateRow(
        response,
        4,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202101",
            "NjyJYiIuKIG",
            "Kathanta Yimbor CHC",
            "/Sierra Leone/Bombali/Sella Limba/Kathanta Yimbor CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "130.0",
            "31.0",
            "2.0",
            "99.0",
            "33.38775",
            "-300.6221960322691",
            "362.6221960322691"));
    validateRow(
        response,
        5,
        List.of(
            "pikOziyCXbM",
            "OPV1 doses given",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "702.0",
            "30.0",
            "7.0",
            "672.0",
            "64.752",
            "-1652.7770420953104",
            "1712.7770420953104"));
    validateRow(
        response,
        6,
        List.of(
            "pikOziyCXbM",
            "OPV1 doses given",
            "202010",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "646.0",
            "30.0",
            "7.0",
            "616.0",
            "59.356",
            "-1652.7770420953104",
            "1712.7770420953104"));
    validateRow(
        response,
        7,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202010",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "714.0",
            "29.0",
            "7.0",
            "685.0",
            "66.00464285714285",
            "-1718.5828112998884",
            "1776.5828112998884"));
    validateRow(
        response,
        8,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "680.0",
            "29.0",
            "7.0",
            "651.0",
            "62.7285",
            "-1718.5828112998877",
            "1776.5828112998877"));
    validateRow(
        response,
        9,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202102",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "154.0",
            "26.0",
            "8.0",
            "128.0",
            "10.792",
            "-430.49440140688677",
            "482.49440140688677"));
    validateRow(
        response,
        10,
        List.of(
            "x3Do5e7g4Qo",
            "OPV0 doses given",
            "202207",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "245.0",
            "26.0",
            "5.0",
            "219.0",
            "29.5431",
            "-620.2014628415138",
            "672.2014628415138"));
    validateRow(
        response,
        11,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202010",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "118.0",
            "25.0",
            "3.0",
            "93.0",
            "20.909499999999998",
            "-297.5768694973244",
            "347.5768694973244"));
    validateRow(
        response,
        12,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202101",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "V6L425pT3A0",
            "Outreach, <1y",
            "HllvX50cXC0",
            "default",
            "154.0",
            "24.0",
            "8.5",
            "130.0",
            "10.315882352941177",
            "-473.22179977911566",
            "521.2217997791156"));
    validateRow(
        response,
        13,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202108",
            "CvBAqD6RzLZ",
            "Ngalu CHC",
            "/Sierra Leone/Bo/Bargbe/Ngalu CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "220.0",
            "23.0",
            "3.0",
            "197.0",
            "44.29216666666667",
            "-551.4953945379397",
            "597.4953945379397"));
    validateRow(
        response,
        14,
        List.of(
            "L2kxa2IA2cs",
            "PCV3 doses given",
            "202205",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "88.0",
            "23.0",
            "2.0",
            "65.0",
            "21.92125",
            "-211.8169455850329",
            "257.8169455850329"));
    validateRow(
        response,
        15,
        List.of(
            "L2kxa2IA2cs",
            "PCV3 doses given",
            "202205",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "88.0",
            "23.0",
            "2.5",
            "65.0",
            "17.537",
            "-269.70853625617383",
            "315.70853625617383"));
    validateRow(
        response,
        16,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202208",
            "CvBAqD6RzLZ",
            "Ngalu CHC",
            "/Sierra Leone/Bo/Bargbe/Ngalu CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "220.0",
            "23.0",
            "3.0",
            "197.0",
            "44.29216666666667",
            "-551.4953945379397",
            "597.4953945379397"));
    validateRow(
        response,
        17,
        List.of(
            "pikOziyCXbM",
            "OPV1 doses given",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "489.0",
            "22.0",
            "8.0",
            "467.0",
            "39.3739375",
            "-2179.8865039850953",
            "2223.8865039850953"));
    validateRow(
        response,
        18,
        List.of(
            "pikOziyCXbM",
            "OPV1 doses given",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "614.0",
            "22.0",
            "8.0",
            "592.0",
            "49.913",
            "-2179.8865039850953",
            "2223.8865039850953"));
    validateRow(
        response,
        19,
        List.of(
            "pikOziyCXbM",
            "OPV1 doses given",
            "202108",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "489.0",
            "22.0",
            "8.0",
            "467.0",
            "39.3739375",
            "-2179.8865039850953",
            "2223.8865039850953"));
    validateRow(
        response,
        20,
        List.of(
            "pikOziyCXbM",
            "OPV1 doses given",
            "202011",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "682.0",
            "22.0",
            "8.0",
            "660.0",
            "55.64625",
            "-2179.8865039850953",
            "2223.8865039850953"));
    validateRow(
        response,
        21,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202010",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "615.0",
            "21.0",
            "6.0",
            "594.0",
            "66.77550000000001",
            "-1450.2590107585331",
            "1492.2590107585331"));
    validateRow(
        response,
        22,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "580.0",
            "21.0",
            "6.0",
            "559.0",
            "62.840916666666665",
            "-1450.2590107585331",
            "1492.2590107585331"));
    validateRow(
        response,
        23,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202104",
            "cgqkFdShPzg",
            "Loreto Clinic",
            "/Sierra Leone/Bombali/Bombali Sebora/Loreto Clinic",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "69.0",
            "20.0",
            "3.0",
            "49.0",
            "11.016833333333333",
            "-154.89117138253303",
            "194.89117138253303"));
    validateRow(
        response,
        24,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "606.0",
            "20.0",
            "6.0",
            "586.0",
            "65.87616666666666",
            "-1792.9340407051104",
            "1832.9340407051104"));
    validateRow(
        response,
        25,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202108",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "489.0",
            "20.0",
            "6.0",
            "469.0",
            "52.72341666666667",
            "-1792.9340407051104",
            "1832.9340407051104"));
    validateRow(
        response,
        26,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202104",
            "cgqkFdShPzg",
            "Loreto Clinic",
            "/Sierra Leone/Bombali/Bombali Sebora/Loreto Clinic",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "69.0",
            "20.0",
            "3.0",
            "49.0",
            "11.016833333333333",
            "-151.40653178475702",
            "191.40653178475702"));
    validateRow(
        response,
        27,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202104",
            "cgqkFdShPzg",
            "Loreto Clinic",
            "/Sierra Leone/Bombali/Bombali Sebora/Loreto Clinic",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "69.0",
            "20.0",
            "3.0",
            "49.0",
            "11.016833333333333",
            "-149.41785036421734",
            "189.41785036421734"));
    validateRow(
        response,
        28,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "489.0",
            "20.0",
            "6.0",
            "469.0",
            "52.72341666666667",
            "-1792.9340407051104",
            "1832.9340407051104"));
    validateRow(
        response,
        29,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202203",
            "cgqkFdShPzg",
            "Loreto Clinic",
            "/Sierra Leone/Bombali/Bombali Sebora/Loreto Clinic",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "69.0",
            "20.0",
            "3.0",
            "49.0",
            "11.016833333333333",
            "-154.89117138253303",
            "194.89117138253303"));
    validateRow(
        response,
        30,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202203",
            "cgqkFdShPzg",
            "Loreto Clinic",
            "/Sierra Leone/Bombali/Bombali Sebora/Loreto Clinic",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "69.0",
            "20.0",
            "3.0",
            "49.0",
            "11.016833333333333",
            "-151.40653178475705",
            "191.40653178475705"));
    validateRow(
        response,
        31,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202203",
            "cgqkFdShPzg",
            "Loreto Clinic",
            "/Sierra Leone/Bombali/Bombali Sebora/Loreto Clinic",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "69.0",
            "20.0",
            "3.0",
            "49.0",
            "11.016833333333333",
            "-149.41785036421734",
            "189.41785036421734"));
    validateRow(
        response,
        32,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202011",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "642.0",
            "20.0",
            "6.0",
            "622.0",
            "69.92316666666666",
            "-1792.934040705111",
            "1832.934040705111"));
    validateRow(
        response,
        33,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202010",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "638.0",
            "19.0",
            "4.0",
            "619.0",
            "104.378875",
            "-1921.2057688748855",
            "1959.2057688748855"));
    validateRow(
        response,
        34,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "580.0",
            "19.0",
            "4.0",
            "561.0",
            "94.598625",
            "-1921.2057688748855",
            "1959.2057688748855"));
    validateRow(
        response,
        35,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202102",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "154.0",
            "18.0",
            "7.0",
            "136.0",
            "13.104571428571429",
            "-447.5139901695406",
            "483.5139901695406"));
    validateRow(
        response,
        36,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202103",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "154.0",
            "18.0",
            "7.0",
            "136.0",
            "13.104571428571429",
            "-447.5139901695406",
            "483.5139901695406"));
    validateRow(
        response,
        37,
        List.of(
            "qPVDd87kS9Z",
            "Weight for height 80 percent and above",
            "202104",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "97.0",
            "16.0",
            "4.5",
            "81.0",
            "12.140999999999998",
            "-291.0567360655639",
            "323.0567360655639"));
    validateRow(
        response,
        38,
        List.of(
            "qPVDd87kS9Z",
            "Weight for height 80 percent and above",
            "202203",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "97.0",
            "16.0",
            "4.5",
            "81.0",
            "12.140999999999998",
            "-291.05673606556377",
            "323.05673606556377"));
    validateRow(
        response,
        39,
        List.of(
            "NLnXLV5YpZF",
            "Weight for age on or above middle line (green)",
            "202209",
            "PC3Ag91n82e",
            "Mongere CHC",
            "/Sierra Leone/Bo/Valunia/Mongere CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "128.0",
            "15.5",
            "7.5",
            "112.5",
            "10.1175",
            "-358.1525538091673",
            "389.1525538091673"));
    validateRow(
        response,
        40,
        List.of(
            "NLnXLV5YpZF",
            "Weight for age on or above middle line (green)",
            "202109",
            "PC3Ag91n82e",
            "Mongere CHC",
            "/Sierra Leone/Bo/Valunia/Mongere CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "128.0",
            "15.5",
            "7.5",
            "112.5",
            "10.1175",
            "-358.15255380916716",
            "389.15255380916716"));
    validateRow(
        response,
        41,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202101",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "103.0",
            "14.0",
            "6.0",
            "89.0",
            "10.005083333333333",
            "-553.5267230142175",
            "581.5267230142175"));
    validateRow(
        response,
        42,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202103",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "154.0",
            "14.0",
            "6.0",
            "140.0",
            "15.738333333333332",
            "-553.5267230142175",
            "581.5267230142175"));
    validateRow(
        response,
        43,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202102",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badija/Ngelehun CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "154.0",
            "14.0",
            "6.0",
            "140.0",
            "15.738333333333332",
            "-553.5267230142175",
            "581.5267230142175"));
    validateRow(
        response,
        44,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "598.0",
            "14.0",
            "5.0",
            "584.0",
            "78.7816",
            "-2012.82074184476",
            "2040.82074184476"));
    validateRow(
        response,
        45,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202108",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "451.0",
            "14.0",
            "5.0",
            "437.0",
            "58.9513",
            "-2012.82074184476",
            "2040.82074184476"));
    validateRow(
        response,
        46,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202102",
            "EXbPGmEUdnc",
            "Mateboi CHC",
            "/Sierra Leone/Bombali/Sanda Tendaren/Mateboi CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "40.0",
            "14.0",
            "1.0",
            "26.0",
            "17.537",
            "-69.77101828192033",
            "97.77101828192033"));
    validateRow(
        response,
        47,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202109",
            "RhJbg8UD75Q",
            "Yemoh Town CHC",
            "/Sierra Leone/Bo/Kakua/Yemoh Town CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "466.0",
            "14.0",
            "6.0",
            "452.0",
            "50.81233333333333",
            "-1128.7966291378252",
            "1156.7966291378252"));
    validateRow(
        response,
        48,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202109",
            "RhJbg8UD75Q",
            "Yemoh Town CHC",
            "/Sierra Leone/Bo/Kakua/Yemoh Town CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "466.0",
            "14.0",
            "6.0",
            "452.0",
            "50.81233333333333",
            "-1424.6649928739664",
            "1452.6649928739664"));
    validateRow(
        response,
        49,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202101",
            "agEKP19IUKI",
            "Tambiama CHC",
            "/Sierra Leone/Bombali/Gbendembu Ngowahun/Tambiama CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "113.0",
            "14.0",
            "1.0",
            "99.0",
            "66.7755",
            "-406.97769055521405",
            "434.97769055521405"));
    validateRow(
        response,
        50,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202102",
            "agEKP19IUKI",
            "Tambiama CHC",
            "/Sierra Leone/Bombali/Gbendembu Ngowahun/Tambiama CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "113.0",
            "14.0",
            "1.0",
            "99.0",
            "66.7755",
            "-406.97769055521405",
            "434.97769055521405"));
    validateRow(
        response,
        51,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "600.0",
            "14.0",
            "4.0",
            "586.0",
            "98.81425",
            "-1894.867827700701",
            "1922.867827700701"));
    validateRow(
        response,
        52,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202108",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "430.0",
            "14.0",
            "4.0",
            "416.0",
            "70.148",
            "-1894.867827700701",
            "1922.867827700701"));
    validateRow(
        response,
        53,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202011",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "652.0",
            "14.0",
            "5.0",
            "638.0",
            "86.06620000000001",
            "-2012.8207418447596",
            "2040.8207418447596"));
    validateRow(
        response,
        54,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202011",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "552.0",
            "14.0",
            "4.0",
            "538.0",
            "90.72025",
            "-1894.8678277007007",
            "1922.8678277007007"));
    validateRow(
        response,
        55,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "451.0",
            "14.0",
            "5.0",
            "437.0",
            "58.9513",
            "-2012.8207418447594",
            "2040.8207418447594"));
    validateRow(
        response,
        56,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202202",
            "EXbPGmEUdnc",
            "Mateboi CHC",
            "/Sierra Leone/Bombali/Sanda Tendaren/Mateboi CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "40.0",
            "14.0",
            "1.0",
            "26.0",
            "17.537",
            "-69.77101828192036",
            "97.77101828192036"));
    validateRow(
        response,
        57,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202209",
            "RhJbg8UD75Q",
            "Yemoh Town CHC",
            "/Sierra Leone/Bo/Kakua/Yemoh Town CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "466.0",
            "14.0",
            "6.0",
            "452.0",
            "50.81233333333333",
            "-1128.796629137825",
            "1156.796629137825"));
    validateRow(
        response,
        58,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202209",
            "RhJbg8UD75Q",
            "Yemoh Town CHC",
            "/Sierra Leone/Bo/Kakua/Yemoh Town CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "466.0",
            "14.0",
            "6.0",
            "452.0",
            "50.81233333333333",
            "-1424.6649928739662",
            "1452.6649928739662"));
    validateRow(
        response,
        59,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "430.0",
            "14.0",
            "4.0",
            "416.0",
            "70.148",
            "-1894.8678277007007",
            "1922.8678277007007"));
    validateRow(
        response,
        60,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202201",
            "agEKP19IUKI",
            "Tambiama CHC",
            "/Sierra Leone/Bombali/Gbendembu Ngowahun/Tambiama CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "113.0",
            "14.0",
            "1.0",
            "99.0",
            "66.7755",
            "-406.9776905552141",
            "434.9776905552141"));
    validateRow(
        response,
        61,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202202",
            "agEKP19IUKI",
            "Tambiama CHC",
            "/Sierra Leone/Bombali/Gbendembu Ngowahun/Tambiama CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "113.0",
            "14.0",
            "1.0",
            "99.0",
            "66.7755",
            "-406.9776905552141",
            "434.9776905552141"));
    validateRow(
        response,
        62,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "451.0",
            "13.0",
            "3.0",
            "438.0",
            "98.47699999999999",
            "-1937.0352555137376",
            "1963.0352555137376"));
    validateRow(
        response,
        63,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "430.0",
            "13.0",
            "4.0",
            "417.0",
            "70.316625",
            "-2060.335991122377",
            "2086.335991122377"));
    validateRow(
        response,
        64,
        List.of(
            "NLnXLV5YpZF",
            "Weight for age on or above middle line (green)",
            "202208",
            "PC3Ag91n82e",
            "Mongere CHC",
            "/Sierra Leone/Bo/Valunia/Mongere CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "88.0",
            "13.0",
            "4.0",
            "75.0",
            "12.646875",
            "-253.71993383032765",
            "279.71993383032765"));
    validateRow(
        response,
        65,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "530.0",
            "13.0",
            "4.0",
            "517.0",
            "87.179125",
            "-1445.0903683875267",
            "1471.0903683875267"));
    validateRow(
        response,
        66,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "616.0",
            "13.0",
            "4.0",
            "603.0",
            "101.680875",
            "-1771.8403381676567",
            "1797.8403381676567"));
    validateRow(
        response,
        67,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "598.0",
            "13.0",
            "3.0",
            "585.0",
            "131.5275",
            "-1937.0352555137379",
            "1963.0352555137379"));
    validateRow(
        response,
        68,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "600.0",
            "13.0",
            "4.0",
            "587.0",
            "98.98287499999999",
            "-2060.335991122377",
            "2086.335991122377"));
    validateRow(
        response,
        69,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202108",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "451.0",
            "13.0",
            "3.0",
            "438.0",
            "98.47699999999999",
            "-1937.0352555137379",
            "1963.0352555137379"));
    validateRow(
        response,
        70,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202108",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "430.0",
            "13.0",
            "4.0",
            "417.0",
            "70.316625",
            "-2060.335991122377",
            "2086.335991122377"));
    validateRow(
        response,
        71,
        List.of(
            "NLnXLV5YpZF",
            "Weight for age on or above middle line (green)",
            "202108",
            "PC3Ag91n82e",
            "Mongere CHC",
            "/Sierra Leone/Bo/Valunia/Mongere CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "88.0",
            "13.0",
            "4.0",
            "75.0",
            "12.646875",
            "-253.71993383032765",
            "279.71993383032765"));
    validateRow(
        response,
        72,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202010",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "557.0",
            "13.0",
            "4.0",
            "544.0",
            "91.732",
            "-1445.090368387526",
            "1471.090368387526"));
    validateRow(
        response,
        73,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202011",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "604.0",
            "13.0",
            "3.0",
            "591.0",
            "132.8765",
            "-1937.0352555137385",
            "1963.0352555137385"));
    validateRow(
        response,
        74,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202011",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "690.0",
            "13.0",
            "4.0",
            "677.0",
            "114.159125",
            "-2060.3359911223765",
            "2086.3359911223765"));
    validateRow(
        response,
        75,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202010",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "684.0",
            "13.0",
            "4.0",
            "671.0",
            "113.147375",
            "-1771.8403381676567",
            "1797.8403381676567"));
    validateRow(
        response,
        76,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202011",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "650.0",
            "12.5",
            "6.0",
            "637.5",
            "71.66562499999999",
            "-2334.9843043884807",
            "2359.9843043884807"));
    validateRow(
        response,
        77,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "457.0",
            "12.5",
            "6.0",
            "444.5",
            "49.969208333333334",
            "-2334.984304388482",
            "2359.984304388482"));
    validateRow(
        response,
        78,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "596.0",
            "12.5",
            "6.0",
            "583.5",
            "65.595125",
            "-2334.9843043884807",
            "2359.9843043884807"));
    validateRow(
        response,
        79,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202108",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "457.0",
            "12.5",
            "6.0",
            "444.5",
            "49.969208333333334",
            "-2334.9843043884807",
            "2359.9843043884807"));
    validateRow(
        response,
        80,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "594.0",
            "12.0",
            "5.0",
            "582.0",
            "78.5118",
            "-2083.4501021925676",
            "2107.4501021925676"));
    validateRow(
        response,
        81,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202108",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "455.0",
            "12.0",
            "5.0",
            "443.0",
            "59.7607",
            "-2083.4501021925676",
            "2107.4501021925676"));
    validateRow(
        response,
        82,
        List.of(
            "NLnXLV5YpZF",
            "Weight for age on or above middle line (green)",
            "202101",
            "EXbPGmEUdnc",
            "Mateboi CHC",
            "/Sierra Leone/Bombali/Sanda Tendaren/Mateboi CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "50.0",
            "12.0",
            "2.0",
            "38.0",
            "12.8155",
            "-111.9631099295363",
            "135.9631099295363"));
    validateRow(
        response,
        83,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "616.0",
            "12.0",
            "3.0",
            "604.0",
            "135.79933333333332",
            "-1879.8073855928467",
            "1903.8073855928467"));
    validateRow(
        response,
        84,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "530.0",
            "12.0",
            "4.0",
            "518.0",
            "87.34775",
            "-1401.5277221748743",
            "1425.5277221748743"));
    validateRow(
        response,
        85,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202111",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "547.0",
            "12.0",
            "4.0",
            "535.0",
            "90.214375",
            "-1563.276768521412",
            "1587.276768521412"));
    validateRow(
        response,
        86,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202011",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "600.0",
            "12.0",
            "5.0",
            "588.0",
            "79.3212",
            "-2083.4501021925676",
            "2107.4501021925676"));
    validateRow(
        response,
        87,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202010",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "598.0",
            "12.0",
            "3.0",
            "586.0",
            "131.75233333333333",
            "-1879.807385592847",
            "1903.807385592847"));
    validateRow(
        response,
        88,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202010",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "588.0",
            "12.0",
            "4.0",
            "576.0",
            "97.128",
            "-1401.5277221748752",
            "1425.5277221748752"));
    validateRow(
        response,
        89,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202010",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "514.0",
            "12.0",
            "4.0",
            "502.0",
            "84.64975",
            "-1563.2767685214121",
            "1587.2767685214121"));
    validateRow(
        response,
        90,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "455.0",
            "12.0",
            "5.0",
            "443.0",
            "59.7607",
            "-2083.450102192568",
            "2107.450102192568"));
    validateRow(
        response,
        91,
        List.of(
            "NLnXLV5YpZF",
            "Weight for age on or above middle line (green)",
            "202201",
            "EXbPGmEUdnc",
            "Mateboi CHC",
            "/Sierra Leone/Bombali/Sanda Tendaren/Mateboi CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "50.0",
            "12.0",
            "2.0",
            "38.0",
            "12.8155",
            "-111.96310992953633",
            "135.96310992953633"));
    validateRow(
        response,
        92,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202208",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "457.0",
            "11.5",
            "5.5",
            "445.5",
            "54.6345",
            "-2228.7107560607287",
            "2251.7107560607287"));
    validateRow(
        response,
        93,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202110",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "596.0",
            "11.5",
            "5.5",
            "584.5",
            "71.68095454545454",
            "-2228.710756060728",
            "2251.710756060728"));
    validateRow(
        response,
        94,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202108",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "457.0",
            "11.5",
            "5.5",
            "445.5",
            "54.6345",
            "-2228.710756060728",
            "2251.710756060728"));
    validateRow(
        response,
        95,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202011",
            "tSBcgrTDdB8",
            "Paramedical CHC",
            "/Sierra Leone/Bo/Kakua/Paramedical CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "536.0",
            "11.5",
            "5.5",
            "524.5",
            "64.32277272727272",
            "-2228.710756060728",
            "2251.710756060728"));
    validateRow(
        response,
        96,
        List.of(
            "tU7GixyHhsv",
            "Vitamin A given to < 5y",
            "202203",
            "FLjwMPWLrL2",
            "Baomahun CHC",
            "/Sierra Leone/Bo/Valunia/Baomahun CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "30.0",
            "11.0",
            "1.0",
            "19.0",
            "12.8155",
            "-110.51779638926187",
            "132.5177963892619"));
    validateRow(
        response,
        97,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202202",
            "EXbPGmEUdnc",
            "Mateboi CHC",
            "/Sierra Leone/Bombali/Sanda Tendaren/Mateboi CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "70.0",
            "11.0",
            "1.0",
            "59.0",
            "39.7955",
            "-268.5802476631745",
            "290.5802476631745"));
    validateRow(
        response,
        98,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202202",
            "X79FDd4EAgo",
            "Rokulan CHC",
            "/Sierra Leone/Bombali/Sanda Tendaren/Rokulan CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "30.0",
            "11.0",
            "1.0",
            "19.0",
            "12.8155",
            "-81.21801186453393",
            "103.21801186453393"));
    validateRow(
        response,
        99,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202201",
            "EXbPGmEUdnc",
            "Mateboi CHC",
            "/Sierra Leone/Bombali/Sanda Tendaren/Mateboi CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "80.0",
            "11.0",
            "1.0",
            "69.0",
            "46.5405",
            "-268.5802476631745",
            "290.5802476631745"));
  }

  @Test
  public void queryOutlierTest4() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dx=Y7Oq71I3ASg")
            .add("endDate=2024-01-02")
            .add("ou=O6uvpzGd5pu,fdc6uOvgoji")
            .add("maxResults=100")
            .add("startDate=2020-10-01")
            .add("algorithm=MOD_Z_SCORE");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(6)))
        .body("height", equalTo(6))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":6,\"orderBy\":\"middle_value_abs_dev\",\"threshold\":\"3.0\",\"maxResults\":100,\"algorithm\":\"MOD_Z_SCORE\"}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 1, "dxname", "Data name", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 3, "ou", "Organisation unit", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 4, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        5,
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(
        response, 6, "coc", "Category option combo", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        7,
        "cocname",
        "Category option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(
        response, 8, "aoc", "Attribute option combo", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        9,
        "aocname",
        "Attribute option combo name",
        "TEXT",
        "java.lang.String",
        false,
        false);
    validateHeader(response, 10, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 11, "median", "Median", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response,
        12,
        "medianabsdeviation",
        "Median absolute deviation",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response, 13, "absdev", "Absolute deviation", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response,
        14,
        "modifiedzscore",
        "Modified zScore",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response, 15, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 16, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Y7Oq71I3ASg",
            "Schistosomiasis new",
            "202104",
            "jNb63DIHuwU",
            "Baoma Station CHP",
            "/Sierra Leone/Bo/Baoma/Baoma Station CHP",
            "SdOUI2yT46H",
            "5-14y",
            "HllvX50cXC0",
            "default",
            "55.0",
            "4.0",
            "1.0",
            "51.0",
            "34.399499999999996",
            "-68.84229540589726",
            "76.84229540589726"));
    validateRow(
        response,
        1,
        List.of(
            "Y7Oq71I3ASg",
            "Schistosomiasis new",
            "202203",
            "jNb63DIHuwU",
            "Baoma Station CHP",
            "/Sierra Leone/Bo/Baoma/Baoma Station CHP",
            "SdOUI2yT46H",
            "5-14y",
            "HllvX50cXC0",
            "default",
            "55.0",
            "4.0",
            "1.0",
            "51.0",
            "34.399499999999996",
            "-68.84229540589726",
            "76.84229540589726"));
    validateRow(
        response,
        2,
        List.of(
            "Y7Oq71I3ASg",
            "Schistosomiasis new",
            "202102",
            "TSyzvBiovKh",
            "Gerehun CHC",
            "/Sierra Leone/Bo/Baoma/Gerehun CHC",
            "jOkIbJVhECg",
            "15y+",
            "HllvX50cXC0",
            "default",
            "8.0",
            "2.0",
            "0.5",
            "6.0",
            "8.094",
            "-6.317902379807062",
            "10.317902379807062"));
    validateRow(
        response,
        3,
        List.of(
            "Y7Oq71I3ASg",
            "Schistosomiasis new",
            "202202",
            "TSyzvBiovKh",
            "Gerehun CHC",
            "/Sierra Leone/Bo/Baoma/Gerehun CHC",
            "jOkIbJVhECg",
            "15y+",
            "HllvX50cXC0",
            "default",
            "8.0",
            "2.0",
            "0.5",
            "6.0",
            "8.094",
            "-6.317902379807062",
            "10.317902379807062"));
    validateRow(
        response,
        4,
        List.of(
            "Y7Oq71I3ASg",
            "Schistosomiasis new",
            "202108",
            "jhtj3eQa1pM",
            "Gondama (Tikonko) CHC",
            "/Sierra Leone/Bo/Tikonko/Gondama (Tikonko) CHC",
            "SdOUI2yT46H",
            "5-14y",
            "HllvX50cXC0",
            "default",
            "7.0",
            "2.0",
            "1.0",
            "5.0",
            "3.3725",
            "-3.2493385826745405",
            "7.2493385826745405"));
    validateRow(
        response,
        5,
        List.of(
            "Y7Oq71I3ASg",
            "Schistosomiasis new",
            "202208",
            "jhtj3eQa1pM",
            "Gondama (Tikonko) CHC",
            "/Sierra Leone/Bo/Tikonko/Gondama (Tikonko) CHC",
            "SdOUI2yT46H",
            "5-14y",
            "HllvX50cXC0",
            "default",
            "7.0",
            "2.0",
            "1.0",
            "5.0",
            "3.3725",
            "-3.2493385826745405",
            "7.2493385826745405"));
  }
}
