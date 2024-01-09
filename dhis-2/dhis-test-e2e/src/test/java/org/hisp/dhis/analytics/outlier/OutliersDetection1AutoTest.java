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
  public void queryOutliertest1() throws JSONException {
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
        .body("headers", hasSize(equalTo(18)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(18))
        .body("headerWidth", equalTo(18));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":5,\"orderBy\":\"value\",\"threshold\":\"3.0\",\"maxResults\":500,\"algorithm\":\"Z_SCORE\"}";
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
    validateHeader(response, 15, "zscore", "zScore", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 16, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 17, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202207",
            "July 2022",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "V6L425pT3A0",
            "Outreach, <1y",
            "HllvX50cXC0",
            "default",
            "23.0",
            "7.54839",
            "5.14837",
            "15.45161",
            "3.00126",
            "-7.89673",
            "22.9935"));
    validateRow(
        response,
        1,
        List.of(
            "pnL2VG8Bn7N",
            "Weight for height 70-79 percent",
            "202207",
            "July 2022",
            "mzsOsz0NwNY",
            "New Police Barracks CHC",
            "/Sierra Leone/Bo/Kakua/New Police Barracks CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "120.0",
            "28.96124",
            "28.86816",
            "91.03876",
            "3.1536",
            "-57.64324",
            "115.56572"));
    validateRow(
        response,
        2,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202208",
            "August 2022",
            "CvBAqD6RzLZ",
            "Ngalu CHC",
            "/Sierra Leone/Bo/Bargbe/Ngalu CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "220.0",
            "41.64407",
            "57.44954",
            "178.35593",
            "3.10457",
            "-130.70455",
            "213.99269"));
    validateRow(
        response,
        3,
        List.of(
            "x3Do5e7g4Qo",
            "OPV0 doses given",
            "202207",
            "July 2022",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "245.0",
            "43.01818",
            "64.62015",
            "201.98182",
            "3.12568",
            "-150.84226",
            "236.87862"));
    validateRow(
        response,
        4,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202209",
            "September 2022",
            "RhJbg8UD75Q",
            "Yemoh Town CHC",
            "/Sierra Leone/Bo/Kakua/Yemoh Town CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "466.0",
            "48.18605",
            "114.27966",
            "417.81395",
            "3.65607",
            "-294.65294",
            "391.02504"));
  }

  @Test
  public void queryOutliertest2() throws JSONException {
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
        .body("headers", hasSize(equalTo(18)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(18));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":0,\"orderBy\":\"middle_value\",\"threshold\":\"5.0\",\"maxResults\":35,\"algorithm\":\"Z_SCORE\"}";
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
    validateHeader(response, 15, "zscore", "zScore", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 16, "lowerbound", "Lower boundary", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 17, "upperbound", "Upper boundary", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
  }

  @Test
  public void queryOutliertest3() throws JSONException {
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
        .body("headers", hasSize(equalTo(18)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(18))
        .body("headerWidth", equalTo(18));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":100,\"orderBy\":\"middle_value\",\"threshold\":\"10.0\",\"maxResults\":100,\"algorithm\":\"MOD_Z_SCORE\"}";
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
        "Modified zScore",
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
            "z7duEFcpApd",
            "LLITN given at Penta3",
            "202010",
            "October 2020",
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
            "12.50891",
            "-1155.30242",
            "1233.30242"));
    validateRow(
        response,
        1,
        List.of(
            "z7duEFcpApd",
            "LLITN given at Penta3",
            "202111",
            "November 2021",
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
            "12.20232",
            "-1155.30242",
            "1233.30242"));
    validateRow(
        response,
        2,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202202",
            "February 2022",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "323.0",
            "31.5",
            "8.0",
            "291.5",
            "24.57709",
            "-1030.09843",
            "1093.09843"));
    validateRow(
        response,
        3,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202201",
            "January 2022",
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
            "-300.6222",
            "362.6222"));
    validateRow(
        response,
        4,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202101",
            "January 2021",
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
            "-300.6222",
            "362.6222"));
    validateRow(
        response,
        5,
        List.of(
            "pikOziyCXbM",
            "OPV1 doses given",
            "202111",
            "November 2021",
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
            "-1652.77704",
            "1712.77704"));
    validateRow(
        response,
        6,
        List.of(
            "pikOziyCXbM",
            "OPV1 doses given",
            "202010",
            "October 2020",
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
            "-1652.77704",
            "1712.77704"));
    validateRow(
        response,
        7,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202010",
            "October 2020",
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
            "66.00464",
            "-1718.58281",
            "1776.58281"));
    validateRow(
        response,
        8,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202111",
            "November 2021",
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
            "-1718.58281",
            "1776.58281"));
    validateRow(
        response,
        9,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202102",
            "February 2021",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "154.0",
            "26.0",
            "8.0",
            "128.0",
            "10.792",
            "-430.4944",
            "482.4944"));
    validateRow(
        response,
        10,
        List.of(
            "x3Do5e7g4Qo",
            "OPV0 doses given",
            "202207",
            "July 2022",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "245.0",
            "26.0",
            "5.0",
            "219.0",
            "29.5431",
            "-620.20146",
            "672.20146"));
    validateRow(
        response,
        11,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202010",
            "October 2020",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "hEFKSsPV5et",
            "Outreach, >1y",
            "HllvX50cXC0",
            "default",
            "118.0",
            "25.0",
            "3.0",
            "93.0",
            "20.9095",
            "-297.57687",
            "347.57687"));
    validateRow(
        response,
        12,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202101",
            "January 2021",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "V6L425pT3A0",
            "Outreach, <1y",
            "HllvX50cXC0",
            "default",
            "154.0",
            "24.0",
            "8.5",
            "130.0",
            "10.31588",
            "-473.2218",
            "521.2218"));
    validateRow(
        response,
        13,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202108",
            "August 2021",
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
            "44.29217",
            "-551.49539",
            "597.49539"));
    validateRow(
        response,
        14,
        List.of(
            "L2kxa2IA2cs",
            "PCV3 doses given",
            "202205",
            "May 2022",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "88.0",
            "23.0",
            "2.0",
            "65.0",
            "21.92125",
            "-211.81695",
            "257.81695"));
    validateRow(
        response,
        15,
        List.of(
            "L2kxa2IA2cs",
            "PCV3 doses given",
            "202205",
            "May 2022",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "88.0",
            "23.0",
            "2.5",
            "65.0",
            "17.537",
            "-269.70854",
            "315.70854"));
    validateRow(
        response,
        16,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202208",
            "August 2022",
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
            "44.29217",
            "-551.49539",
            "597.49539"));
    validateRow(
        response,
        17,
        List.of(
            "pikOziyCXbM",
            "OPV1 doses given",
            "202208",
            "August 2022",
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
            "39.37394",
            "-2179.8865",
            "2223.8865"));
    validateRow(
        response,
        18,
        List.of(
            "pikOziyCXbM",
            "OPV1 doses given",
            "202110",
            "October 2021",
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
            "-2179.8865",
            "2223.8865"));
    validateRow(
        response,
        19,
        List.of(
            "pikOziyCXbM",
            "OPV1 doses given",
            "202108",
            "August 2021",
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
            "39.37394",
            "-2179.8865",
            "2223.8865"));
    validateRow(
        response,
        20,
        List.of(
            "pikOziyCXbM",
            "OPV1 doses given",
            "202011",
            "November 2020",
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
            "-2179.8865",
            "2223.8865"));
    validateRow(
        response,
        21,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202010",
            "October 2020",
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
            "66.7755",
            "-1450.25901",
            "1492.25901"));
    validateRow(
        response,
        22,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202111",
            "November 2021",
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
            "62.84092",
            "-1450.25901",
            "1492.25901"));
    validateRow(
        response,
        23,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202104",
            "April 2021",
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
            "11.01683",
            "-154.89117",
            "194.89117"));
    validateRow(
        response,
        24,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202110",
            "October 2021",
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
            "65.87617",
            "-1792.93404",
            "1832.93404"));
    validateRow(
        response,
        25,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202108",
            "August 2021",
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
            "52.72342",
            "-1792.93404",
            "1832.93404"));
    validateRow(
        response,
        26,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202104",
            "April 2021",
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
            "11.01683",
            "-151.40653",
            "191.40653"));
    validateRow(
        response,
        27,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202104",
            "April 2021",
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
            "11.01683",
            "-149.41785",
            "189.41785"));
    validateRow(
        response,
        28,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202203",
            "March 2022",
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
            "11.01683",
            "-154.89117",
            "194.89117"));
    validateRow(
        response,
        29,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202208",
            "August 2022",
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
            "52.72342",
            "-1792.93404",
            "1832.93404"));
    validateRow(
        response,
        30,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202203",
            "March 2022",
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
            "11.01683",
            "-151.40653",
            "191.40653"));
    validateRow(
        response,
        31,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202203",
            "March 2022",
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
            "11.01683",
            "-149.41785",
            "189.41785"));
    validateRow(
        response,
        32,
        List.of(
            "fClA2Erf6IO",
            "Penta1 doses given",
            "202011",
            "November 2020",
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
            "69.92317",
            "-1792.93404",
            "1832.93404"));
    validateRow(
        response,
        33,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202010",
            "October 2020",
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
            "104.37888",
            "-1921.20577",
            "1959.20577"));
    validateRow(
        response,
        34,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202111",
            "November 2021",
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
            "94.59863",
            "-1921.20577",
            "1959.20577"));
    validateRow(
        response,
        35,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202102",
            "February 2021",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "154.0",
            "18.0",
            "7.0",
            "136.0",
            "13.10457",
            "-447.51399",
            "483.51399"));
    validateRow(
        response,
        36,
        List.of(
            "s46m5MS0hxu",
            "BCG doses given",
            "202103",
            "March 2021",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "154.0",
            "18.0",
            "7.0",
            "136.0",
            "13.10457",
            "-447.51399",
            "483.51399"));
    validateRow(
        response,
        37,
        List.of(
            "qPVDd87kS9Z",
            "Weight for height 80 percent and above",
            "202104",
            "April 2021",
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
            "12.141",
            "-291.05674",
            "323.05674"));
    validateRow(
        response,
        38,
        List.of(
            "qPVDd87kS9Z",
            "Weight for height 80 percent and above",
            "202203",
            "March 2022",
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
            "12.141",
            "-291.05674",
            "323.05674"));
    validateRow(
        response,
        39,
        List.of(
            "NLnXLV5YpZF",
            "Weight for age on or above middle line (green)",
            "202209",
            "September 2022",
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
            "-358.15255",
            "389.15255"));
    validateRow(
        response,
        40,
        List.of(
            "NLnXLV5YpZF",
            "Weight for age on or above middle line (green)",
            "202109",
            "September 2021",
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
            "-358.15255",
            "389.15255"));
    validateRow(
        response,
        41,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202110",
            "October 2021",
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
            "-2012.82074",
            "2040.82074"));
    validateRow(
        response,
        42,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202108",
            "August 2021",
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
            "-2012.82074",
            "2040.82074"));
    validateRow(
        response,
        43,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202101",
            "January 2021",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "103.0",
            "14.0",
            "6.0",
            "89.0",
            "10.00508",
            "-553.52672",
            "581.52672"));
    validateRow(
        response,
        44,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202103",
            "March 2021",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "154.0",
            "14.0",
            "6.0",
            "140.0",
            "15.73833",
            "-553.52672",
            "581.52672"));
    validateRow(
        response,
        45,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202102",
            "February 2021",
            "DiszpKrYNg8",
            "Ngelehun CHC",
            "/Sierra Leone/Bo/Badjia/Ngelehun CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "154.0",
            "14.0",
            "6.0",
            "140.0",
            "15.73833",
            "-553.52672",
            "581.52672"));
    validateRow(
        response,
        46,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202102",
            "February 2021",
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
            "-69.77102",
            "97.77102"));
    validateRow(
        response,
        47,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202101",
            "January 2021",
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
            "-406.97769",
            "434.97769"));
    validateRow(
        response,
        48,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202102",
            "February 2021",
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
            "-406.97769",
            "434.97769"));
    validateRow(
        response,
        49,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202110",
            "October 2021",
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
            "-1894.86783",
            "1922.86783"));
    validateRow(
        response,
        50,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202108",
            "August 2021",
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
            "-1894.86783",
            "1922.86783"));
    validateRow(
        response,
        51,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202109",
            "September 2021",
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
            "50.81233",
            "-1128.79663",
            "1156.79663"));
    validateRow(
        response,
        52,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202109",
            "September 2021",
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
            "50.81233",
            "-1424.66499",
            "1452.66499"));
    validateRow(
        response,
        53,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202011",
            "November 2020",
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
            "86.0662",
            "-2012.82074",
            "2040.82074"));
    validateRow(
        response,
        54,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202011",
            "November 2020",
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
            "-1894.86783",
            "1922.86783"));
    validateRow(
        response,
        55,
        List.of(
            "I78gJm4KBo7",
            "Penta2 doses given",
            "202208",
            "August 2022",
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
            "-2012.82074",
            "2040.82074"));
    validateRow(
        response,
        56,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202202",
            "February 2022",
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
            "-69.77102",
            "97.77102"));
    validateRow(
        response,
        57,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202208",
            "August 2022",
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
            "-1894.86783",
            "1922.86783"));
    validateRow(
        response,
        58,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202201",
            "January 2022",
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
            "-406.97769",
            "434.97769"));
    validateRow(
        response,
        59,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202202",
            "February 2022",
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
            "-406.97769",
            "434.97769"));
    validateRow(
        response,
        60,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202209",
            "September 2022",
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
            "50.81233",
            "-1128.79663",
            "1156.79663"));
    validateRow(
        response,
        61,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202209",
            "September 2022",
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
            "50.81233",
            "-1424.66499",
            "1452.66499"));
    validateRow(
        response,
        62,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202208",
            "August 2022",
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
            "70.31663",
            "-2060.33599",
            "2086.33599"));
    validateRow(
        response,
        63,
        List.of(
            "NLnXLV5YpZF",
            "Weight for age on or above middle line (green)",
            "202208",
            "August 2022",
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
            "12.64688",
            "-253.71993",
            "279.71993"));
    validateRow(
        response,
        64,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202208",
            "August 2022",
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
            "98.477",
            "-1937.03526",
            "1963.03526"));
    validateRow(
        response,
        65,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202111",
            "November 2021",
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
            "101.68088",
            "-1771.84034",
            "1797.84034"));
    validateRow(
        response,
        66,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202110",
            "October 2021",
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
            "98.98287",
            "-2060.33599",
            "2086.33599"));
    validateRow(
        response,
        67,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202108",
            "August 2021",
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
            "70.31663",
            "-2060.33599",
            "2086.33599"));
    validateRow(
        response,
        68,
        List.of(
            "NLnXLV5YpZF",
            "Weight for age on or above middle line (green)",
            "202108",
            "August 2021",
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
            "12.64688",
            "-253.71993",
            "279.71993"));
    validateRow(
        response,
        69,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202111",
            "November 2021",
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
            "87.17913",
            "-1445.09037",
            "1471.09037"));
    validateRow(
        response,
        70,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202110",
            "October 2021",
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
            "-1937.03526",
            "1963.03526"));
    validateRow(
        response,
        71,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202108",
            "August 2021",
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
            "98.477",
            "-1937.03526",
            "1963.03526"));
    validateRow(
        response,
        72,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202011",
            "November 2020",
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
            "114.15913",
            "-2060.33599",
            "2086.33599"));
    validateRow(
        response,
        73,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202010",
            "October 2020",
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
            "113.14738",
            "-1771.84034",
            "1797.84034"));
    validateRow(
        response,
        74,
        List.of(
            "n6aMJNLdvep",
            "Penta3 doses given",
            "202010",
            "October 2020",
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
            "-1445.09037",
            "1471.09037"));
    validateRow(
        response,
        75,
        List.of(
            "O05mAByOgAv",
            "OPV2 doses given",
            "202011",
            "November 2020",
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
            "-1937.03526",
            "1963.03526"));
    validateRow(
        response,
        76,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202011",
            "November 2020",
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
            "71.66562",
            "-2334.9843",
            "2359.9843"));
    validateRow(
        response,
        77,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202208",
            "August 2022",
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
            "49.96921",
            "-2334.9843",
            "2359.9843"));
    validateRow(
        response,
        78,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202110",
            "October 2021",
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
            "65.59513",
            "-2334.9843",
            "2359.9843"));
    validateRow(
        response,
        79,
        List.of(
            "YtbsuPPo010",
            "Measles doses given",
            "202108",
            "August 2021",
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
            "49.96921",
            "-2334.9843",
            "2359.9843"));
    validateRow(
        response,
        80,
        List.of(
            "NLnXLV5YpZF",
            "Weight for age on or above middle line (green)",
            "202101",
            "January 2021",
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
            "-111.96311",
            "135.96311"));
    validateRow(
        response,
        81,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202111",
            "November 2021",
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
            "135.79933",
            "-1879.80739",
            "1903.80739"));
    validateRow(
        response,
        82,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202111",
            "November 2021",
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
            "90.21438",
            "-1563.27677",
            "1587.27677"));
    validateRow(
        response,
        83,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202110",
            "October 2021",
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
            "-2083.4501",
            "2107.4501"));
    validateRow(
        response,
        84,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202108",
            "August 2021",
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
            "-2083.4501",
            "2107.4501"));
    validateRow(
        response,
        85,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202111",
            "November 2021",
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
            "-1401.52772",
            "1425.52772"));
    validateRow(
        response,
        86,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202010",
            "October 2020",
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
            "131.75233",
            "-1879.80739",
            "1903.80739"));
    validateRow(
        response,
        87,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202010",
            "October 2020",
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
            "-1563.27677",
            "1587.27677"));
    validateRow(
        response,
        88,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202011",
            "November 2020",
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
            "-2083.4501",
            "2107.4501"));
    validateRow(
        response,
        89,
        List.of(
            "vI2csg55S9C",
            "OPV3 doses given",
            "202010",
            "October 2020",
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
            "-1401.52772",
            "1425.52772"));
    validateRow(
        response,
        90,
        List.of(
            "NLnXLV5YpZF",
            "Weight for age on or above middle line (green)",
            "202201",
            "January 2022",
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
            "-111.96311",
            "135.96311"));
    validateRow(
        response,
        91,
        List.of(
            "UOlfIjgN8X6",
            "Fully Immunized child",
            "202208",
            "August 2022",
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
            "-2083.4501",
            "2107.4501"));
    validateRow(
        response,
        92,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202208",
            "August 2022",
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
            "-2228.71076",
            "2251.71076"));
    validateRow(
        response,
        93,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202110",
            "October 2021",
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
            "71.68095",
            "-2228.71076",
            "2251.71076"));
    validateRow(
        response,
        94,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202108",
            "August 2021",
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
            "-2228.71076",
            "2251.71076"));
    validateRow(
        response,
        95,
        List.of(
            "l6byfWFUGaP",
            "Yellow Fever doses given",
            "202011",
            "November 2020",
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
            "64.32277",
            "-2228.71076",
            "2251.71076"));
    validateRow(
        response,
        96,
        List.of(
            "tU7GixyHhsv",
            "Vitamin A given to < 5y",
            "202203",
            "March 2022",
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
            "-110.5178",
            "132.5178"));
    validateRow(
        response,
        97,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202202",
            "February 2022",
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
            "-268.58025",
            "290.58025"));
    validateRow(
        response,
        98,
        List.of(
            "Y53Jcc9LBYh",
            "Children supplied with food supplemements",
            "202201",
            "January 2022",
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
            "-268.58025",
            "290.58025"));
    validateRow(
        response,
        99,
        List.of(
            "qPVDd87kS9Z",
            "Weight for height 80 percent and above",
            "202203",
            "March 2022",
            "uRQj8WRK0Py",
            "Masongbo CHC",
            "/Sierra Leone/Bombali/Makari Gbanti/Masongbo CHC",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "140.0",
            "11.0",
            "6.0",
            "129.0",
            "14.50175",
            "-594.30787",
            "616.30787"));
  }

  @Test
  public void queryOutliertest4() throws JSONException {
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
        .body("headers", hasSize(equalTo(18)))
        .body("rows", hasSize(equalTo(6)))
        .body("height", equalTo(6))
        .body("width", equalTo(18))
        .body("headerWidth", equalTo(18));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":6,\"orderBy\":\"middle_value_abs_dev\",\"threshold\":\"3.0\",\"maxResults\":100,\"algorithm\":\"MOD_Z_SCORE\"}";
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
        "Modified zScore",
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
            "Y7Oq71I3ASg",
            "Schistosomiasis new",
            "202104",
            "April 2021",
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
            "34.3995",
            "-68.8423",
            "76.8423"));
    validateRow(
        response,
        1,
        List.of(
            "Y7Oq71I3ASg",
            "Schistosomiasis new",
            "202203",
            "March 2022",
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
            "34.3995",
            "-68.8423",
            "76.8423"));
    validateRow(
        response,
        2,
        List.of(
            "Y7Oq71I3ASg",
            "Schistosomiasis new",
            "202102",
            "February 2021",
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
            "-6.3179",
            "10.3179"));
    validateRow(
        response,
        3,
        List.of(
            "Y7Oq71I3ASg",
            "Schistosomiasis new",
            "202202",
            "February 2022",
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
            "-6.3179",
            "10.3179"));
    validateRow(
        response,
        4,
        List.of(
            "Y7Oq71I3ASg",
            "Schistosomiasis new",
            "202108",
            "August 2021",
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
            "-3.24934",
            "7.24934"));
    validateRow(
        response,
        5,
        List.of(
            "Y7Oq71I3ASg",
            "Schistosomiasis new",
            "202208",
            "August 2022",
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
            "-3.24934",
            "7.24934"));
  }
}
