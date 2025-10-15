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
public class OutliersDetection2AutoTest extends AnalyticsApiTest {
  private final AnalyticsOutlierDetectionActions actions = new AnalyticsOutlierDetectionActions();

  @Test
  public void queryOutliertest5() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=aoc,dx,ouname,pe")
            .add("dx=Y7Oq71I3ASg")
            .add("endDate=2023-01-02")
            .add("ou=O6uvpzGd5pu,fdc6uOvgoji")
            .add("maxResults=30")
            .add("orderBy=modifiedzscore")
            .add("startDate=2022-10-01")
            .add("algorithm=MODIFIED_Z_SCORE");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":0,\"orderBy\":\"MODIFIED_Z_SCORE\",\"threshold\":\"3.0\",\"maxResults\":30,\"algorithm\":\"MODIFIED_Z_SCORE\"}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "aoc", "Attribute option combo", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 1, "dx", "Data", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response, 2, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, false);

    // Assert rows.
  }

  @Test
  public void queryOutliertest6() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("endDate=2022-10-26")
            .add("ou=LEVEL-m9lBJogzE95")
            .add("maxResults=30")
            .add("orderBy=zscore")
            .add("threshold=3")
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
        "{\"count\":3,\"orderBy\":\"Z_SCORE\",\"threshold\":\"3.0\",\"maxResults\":30,\"algorithm\":\"Z_SCORE\"}";
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
  }

  @Test
  public void queryOutliertest7() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=dx,dxname,modifiedzscore")
            .add("endDate=2023-10-26")
            .add("ou=ImspTQPwCqd")
            .add("maxResults=5")
            .add("orderBy=modifiedzscore")
            .add("threshold=3.0")
            .add("startDate=2022-07-26")
            .add("ds=BfMAe6Itzgt")
            .add("algorithm=MODIFIED_Z_SCORE");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":5,\"orderBy\":\"MODIFIED_Z_SCORE\",\"threshold\":\"3.0\",\"maxResults\":5,\"algorithm\":\"MODIFIED_Z_SCORE\"}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 1, "dxname", "Data name", "TEXT", "java.lang.String", false, false);
    validateHeader(
        response,
        2,
        "modifiedzscore",
        "Modified Z-score",
        "NUMBER",
        "java.lang.Double",
        false,
        false);

    // Assert rows.
    validateRow(response, 0, List.of("O05mAByOgAv", "OPV2 doses given", "98.48"));
    validateRow(response, 1, List.of("vI2csg55S9C", "OPV3 doses given", "70.32"));
    validateRow(response, 2, List.of("n6aMJNLdvep", "Penta3 doses given", "70.15"));
    validateRow(response, 3, List.of("UOlfIjgN8X6", "Fully Immunized child", "59.76"));
    validateRow(response, 4, List.of("I78gJm4KBo7", "Penta2 doses given", "58.95"));
  }

  @Test
  public void queryOutliertest8() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=dx,dxname,zscore")
            .add("endDate=2022-10-26")
            .add("ou=ImspTQPwCqd")
            .add("maxResults=30")
            .add("orderBy=zscore")
            .add("threshold=3.0")
            .add("startDate=2022-07-26")
            .add("ds=BfMAe6Itzgt")
            .add("algorithm=Z_SCORE");

    // When
    ApiResponse response = actions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"count\":3,\"orderBy\":\"Z_SCORE\",\"threshold\":\"3.0\",\"maxResults\":30,\"algorithm\":\"Z_SCORE\"}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 1, "dxname", "Data name", "TEXT", "java.lang.String", false, false);
    validateHeader(response, 2, "zscore", "Z-score", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("l6byfWFUGaP", "Yellow Fever doses given", "3.66"));
    validateRow(response, 1, List.of("s46m5MS0hxu", "BCG doses given", "3.1"));
    validateRow(
        response,
        2,
        List.of(
            "dU0GquGkGQr", "Q_Early breastfeeding (within 1 hr after delivery) at BCG", "3.02"));
  }
}
