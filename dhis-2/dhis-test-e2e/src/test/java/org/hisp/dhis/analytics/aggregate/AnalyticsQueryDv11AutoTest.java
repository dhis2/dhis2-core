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
package org.hisp.dhis.analytics.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/analytics" aggregate endpoint. */
public class AnalyticsQueryDv11AutoTest extends AnalyticsApiTest {
  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void queryAnalyticsAggregateProgramOuDim() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=true")
            .add("dimension=dx:FQ2o8UBlcrS,pe:LAST_5_YEARS,ou:PR-q04UBOqq3rp")
            .add("relativePeriodDate=2022-10-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(26)))
        .body("height", equalTo(26))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    String expectedMetaData = "{}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 5, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 7, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 8, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "a04CZxe0PSe", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "VSwnkMSAdp7", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "o0BgK1dLhF8", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "PaNv9VyD06n", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "pJv8NJlJNhU", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "nAH0uNc3b5f", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "HlDMbDWUmTy", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "IPvrsWbm0EM", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "BJMWTGwuGiw", "3", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "k8ZPul89UDm", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "wjFsUXI1MlO", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "VhRX5JDVo7R", "4", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "VFF7f43dJv4", "3", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "DiszpKrYNg8", "23", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "lmNWdmeOYmV", "3", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "aVycEyoSBJx", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "BzEwqabuW19", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "OY7mYDATra3", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "xXYv82KlBUh", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "PfZXxl6Wp3F", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "z9KGMrElTYS", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "wB4R3E1X6pC", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "zQpYVEyAM2t", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "inpc5QsFRTm", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "flQBQV8eyHc", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "ALZ2qr5u0X0", "1", "", "", "", "", ""));
  }

  @Test
  public void queryAnalyticsAggregateDataSetOuDim() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=true")
            .add("dimension=dx:FQ2o8UBlcrS,pe:LAST_5_YEARS,ou:DS-BfMAe6Itzgt")
            .add("relativePeriodDate=2022-10-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(29)))
        .body("height", equalTo(29))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    String expectedMetaData = "{}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 5, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 7, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 8, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "a04CZxe0PSe", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "VSwnkMSAdp7", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "o0BgK1dLhF8", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "O6uvpzGd5pu", "23", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "PaNv9VyD06n", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "pJv8NJlJNhU", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "nAH0uNc3b5f", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "ImspTQPwCqd", "57", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "HlDMbDWUmTy", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "IPvrsWbm0EM", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "BJMWTGwuGiw", "3", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "k8ZPul89UDm", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "wjFsUXI1MlO", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "VhRX5JDVo7R", "4", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "VFF7f43dJv4", "3", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "DiszpKrYNg8", "23", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "lmNWdmeOYmV", "3", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "aVycEyoSBJx", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "BzEwqabuW19", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "OY7mYDATra3", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "PfZXxl6Wp3F", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "xXYv82KlBUh", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "z9KGMrElTYS", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "wB4R3E1X6pC", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "zQpYVEyAM2t", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "YuQRtpLP10I", "23", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "inpc5QsFRTm", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "flQBQV8eyHc", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "ALZ2qr5u0X0", "1", "", "", "", "", ""));
  }

  @Test
  public void queryAnalyticsAggregateProgramDataSetOuDim() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=true")
            .add("dimension=dx:FQ2o8UBlcrS,pe:LAST_5_YEARS,ou:DS-BfMAe6Itzgt;PR-q04UBOqq3rp")
            .add("relativePeriodDate=2022-10-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(29)))
        .body("height", equalTo(29))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    String expectedMetaData = "{}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 5, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 7, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 8, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "a04CZxe0PSe", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "VSwnkMSAdp7", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "o0BgK1dLhF8", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "O6uvpzGd5pu", "23", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "PaNv9VyD06n", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "pJv8NJlJNhU", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "nAH0uNc3b5f", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "ImspTQPwCqd", "57", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "HlDMbDWUmTy", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "IPvrsWbm0EM", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "BJMWTGwuGiw", "3", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "k8ZPul89UDm", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "wjFsUXI1MlO", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "VhRX5JDVo7R", "4", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "VFF7f43dJv4", "3", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "DiszpKrYNg8", "23", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "lmNWdmeOYmV", "3", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "aVycEyoSBJx", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "BzEwqabuW19", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "OY7mYDATra3", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "PfZXxl6Wp3F", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "xXYv82KlBUh", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "z9KGMrElTYS", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "wB4R3E1X6pC", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "zQpYVEyAM2t", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "YuQRtpLP10I", "23", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "inpc5QsFRTm", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "flQBQV8eyHc", "1", "", "", "", "", ""));
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "ALZ2qr5u0X0", "1", "", "", "", "", ""));
  }

  @Test
  public void queryAnalyticsAggregateProgramOuFilter() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:PR-q04UBOqq3rp")
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=true")
            .add("dimension=dx:FQ2o8UBlcrS,pe:LAST_5_YEARS")
            .add("relativePeriodDate=2022-10-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(8))
        .body("headerWidth", equalTo(8));

    // Assert metaData.
    String expectedMetaData = "{}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 3, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 5, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 6, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 7, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "57", "", "", "", "", ""));
  }

  @Test
  public void queryAnalyticsAggregateDataSetOuFilter() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:DS-BfMAe6Itzgt")
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=true")
            .add("dimension=dx:FQ2o8UBlcrS,pe:LAST_5_YEARS")
            .add("relativePeriodDate=2022-10-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(8))
        .body("headerWidth", equalTo(8));

    // Assert metaData.
    String expectedMetaData = "{}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 3, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 5, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 6, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 7, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "57", "", "", "", "", ""));
  }

  @Test
  public void queryAnalyticsAggregateProgramDataSetOuFilter() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:DS-BfMAe6Itzgt;PR-q04UBOqq3rp")
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=true")
            .add("dimension=dx:FQ2o8UBlcrS,pe:LAST_5_YEARS")
            .add("relativePeriodDate=2022-10-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(8))
        .body("headerWidth", equalTo(8));

    // Assert metaData.
    String expectedMetaData = "{}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 3, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 5, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 6, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 7, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("FQ2o8UBlcrS", "2021", "57", "", "", "", "", ""));
  }
}
