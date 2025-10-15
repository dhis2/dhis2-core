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
public class OutliersDetection6AutoTest extends OutliersApiTest {
  private final AnalyticsOutlierDetectionActions actions = new AnalyticsOutlierDetectionActions();

  @Test
  public void queryOutliertest21() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("pe=THIS_YEAR")
            .add("ou=eIQbndfxQMb")
            .add("maxResults=5")
            .add("orderBy=modifiedZScore")
            .add("threshold=3")
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
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(18))
        .body("headerWidth", equalTo(18));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":5,\"orderBy\":\"MODIFIED_Z_SCORE\",\"threshold\":\"3.0\",\"maxResults\":5,\"algorithm\":\"MODIFIED_Z_SCORE\"}";
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
            "-3.34",
            "23.34"));
    validateRow(
        response,
        1,
        List.of(
            "qw2sIef52Fu",
            "Children getting therapeutic feeding",
            "202205",
            "202205",
            "sesv0eXljBq",
            "Yele CHC",
            "Sierra Leone / Tonkolili / Gbonkonlenken / Yele CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "55.0",
            "8.0",
            "1.0",
            "47.0",
            "31.7",
            "3.55",
            "12.45"));
    validateRow(
        response,
        2,
        List.of(
            "pnL2VG8Bn7N",
            "Weight for height 70-79 percent",
            "202201",
            "202201",
            "Ahh47q8AkId",
            "Mabang CHC",
            "Sierra Leone / Tonkolili / Kholifa Mabang / Mabang CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "25.0",
            "3.0",
            "1.0",
            "22.0",
            "14.84",
            "-1.45",
            "7.45"));
    validateRow(
        response,
        3,
        List.of(
            "bTcRDVjC66S",
            "Weight for age below lower line (red)",
            "202201",
            "202201",
            "Ahh47q8AkId",
            "Mabang CHC",
            "Sierra Leone / Tonkolili / Kholifa Mabang / Mabang CHC",
            "Prlt0C1RF0s",
            "Fixed, <1y",
            "HllvX50cXC0",
            "default",
            "24.0",
            "4.0",
            "1.0",
            "20.0",
            "13.49",
            "-0.45",
            "8.45"));
    validateRow(
        response,
        4,
        List.of(
            "tU7GixyHhsv",
            "Vitamin A given to < 5y",
            "202202",
            "202202",
            "wB4R3E1X6pC",
            "Masanga Leprosy Hospital",
            "Sierra Leone / Tonkolili / Kholifa Rowalla / Masanga Leprosy Hospital",
            "psbwp3CQEhs",
            "Fixed, >1y",
            "HllvX50cXC0",
            "default",
            "26.0",
            "10.0",
            "1.0",
            "16.0",
            "10.79",
            "5.55",
            "14.45"));
  }
}
