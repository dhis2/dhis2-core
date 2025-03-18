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
public class AnalyticsQueryDv13AutoTest extends AnalyticsApiTest {
  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void weeklyShortNameLabel() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:USER_ORGUNIT")
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=SHORTNAME")
            .add("skipMeta=false")
            .add("dimension=dx:fbfJHSPpUQD,pe:2021SunW52");

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
    String expectedMetaData =
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"dx\":{\"name\":\"Data\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"pe\":{\"name\":\"Period\"},\"ou\":{\"name\":\"Organisation unit\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"},\"2021SunW52\":{\"name\":\"W52 2021-12-26 - 2022-01-01\"},\"fbfJHSPpUQD\":{\"name\":\"ANC 1st visit\"}},\"dimensions\":{\"dx\":[\"fbfJHSPpUQD\"],\"pe\":[\"2021SunW52\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
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
    validateRow(response, List.of("fbfJHSPpUQD", "2021SunW52", "462", "", "", "", "", ""));
  }

  @Test
  public void weeklyNameLabel() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:USER_ORGUNIT")
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:fbfJHSPpUQD,pe:2021SunW52");

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
    String expectedMetaData =
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"dx\":{\"name\":\"Data\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"pe\":{\"name\":\"Period\"},\"ou\":{\"name\":\"Organisation unit\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"},\"2021SunW52\":{\"name\":\"Week 52 2021-12-26 - 2022-01-01\"},\"fbfJHSPpUQD\":{\"name\":\"ANC 1st visit\"}},\"dimensions\":{\"dx\":[\"fbfJHSPpUQD\"],\"pe\":[\"2021SunW52\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
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
    validateRow(response, List.of("fbfJHSPpUQD", "2021SunW52", "462", "", "", "", "", ""));
  }

  @Test
  public void dataElementOperandsAsDimension() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:ImspTQPwCqd")
            .add("displayProperty=NAME")
            .add("dimension=pe:MONTHS_THIS_YEAR,dx:cYeuwXTCPkU.pq2XI5kz2BY")
            .add("relativePeriodDate=2021-10-04");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(12)))
        .body("height", equalTo(12))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ou\":{\"name\":\"Organisation unit\"},\"202109\":{\"name\":\"September 2021\"},\"MONTHS_THIS_YEAR\":{\"name\":\"Months this year\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"202101\":{\"name\":\"January 2021\"},\"202112\":{\"name\":\"December 2021\"},\"202102\":{\"name\":\"February 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"cYeuwXTCPkU.pq2XI5kz2BY\":{\"name\":\"ANC 2nd visit Fixed\"}},\"dimensions\":{\"dx\":[\"cYeuwXTCPkU.pq2XI5kz2BY\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202104", "13984"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202103", "15409"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202106", "19307"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202105", "19287"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202108", "16366"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202107", "16618"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202109", "16071"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202111", "15278"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202110", "12797"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202102", "13646"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202112", "12393"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202101", "13065"));
  }

  @Test
  public void dataElementOperandsAsFilter() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=dx:cYeuwXTCPkU.pq2XI5kz2BY,ou:ImspTQPwCqd")
            .add("skipData=false")
            .add("displayProperty=NAME")
            .add("skipMeta=true")
            .add("dimension=pe:MONTHS_THIS_YEAR")
            .add("relativePeriodDate=2021-10-04");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(12)))
        .body("height", equalTo(12))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData = "{}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("202101", "13065"));
    validateRow(response, List.of("202112", "12393"));
    validateRow(response, List.of("202102", "13646"));
    validateRow(response, List.of("202110", "12797"));
    validateRow(response, List.of("202111", "15278"));
    validateRow(response, List.of("202109", "16071"));
    validateRow(response, List.of("202107", "16618"));
    validateRow(response, List.of("202108", "16366"));
    validateRow(response, List.of("202105", "19287"));
    validateRow(response, List.of("202106", "19307"));
    validateRow(response, List.of("202103", "15409"));
    validateRow(response, List.of("202104", "13984"));
  }

  @Test
  public void dataElementOperandsAndDataElementsAsDimension() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:ImspTQPwCqd")
            .add("displayProperty=NAME")
            .add("dimension=pe:MONTHS_THIS_YEAR,dx:cYeuwXTCPkU.pq2XI5kz2BY;cYeuwXTCPkU")
            .add("relativePeriodDate=2021-10-04");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(24)))
        .body("height", equalTo(24))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ou\":{\"name\":\"Organisation unit\"},\"202109\":{\"name\":\"September 2021\"},\"MONTHS_THIS_YEAR\":{\"name\":\"Months this year\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"202101\":{\"name\":\"January 2021\"},\"202112\":{\"name\":\"December 2021\"},\"202102\":{\"name\":\"February 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"pe\":{\"name\":\"Period\"},\"cYeuwXTCPkU\":{\"name\":\"ANC 2nd visit\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"},\"cYeuwXTCPkU.pq2XI5kz2BY\":{\"name\":\"ANC 2nd visit Fixed\"}},\"dimensions\":{\"dx\":[\"cYeuwXTCPkU.pq2XI5kz2BY\",\"cYeuwXTCPkU\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("cYeuwXTCPkU", "202106", "23904"));
    validateRow(response, List.of("cYeuwXTCPkU", "202107", "21130"));
    validateRow(response, List.of("cYeuwXTCPkU", "202108", "20413"));
    validateRow(response, List.of("cYeuwXTCPkU", "202109", "20433"));
    validateRow(response, List.of("cYeuwXTCPkU", "202102", "18488"));
    validateRow(response, List.of("cYeuwXTCPkU", "202103", "19574"));
    validateRow(response, List.of("cYeuwXTCPkU", "202104", "18403"));
    validateRow(response, List.of("cYeuwXTCPkU", "202105", "23726"));
    validateRow(response, List.of("cYeuwXTCPkU", "202110", "16113"));
    validateRow(response, List.of("cYeuwXTCPkU", "202111", "19453"));
    validateRow(response, List.of("cYeuwXTCPkU", "202112", "15183"));
    validateRow(response, List.of("cYeuwXTCPkU", "202101", "17269"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202104", "13984"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202103", "15409"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202106", "19307"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202105", "19287"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202108", "16366"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202107", "16618"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202109", "16071"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202111", "15278"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202110", "12797"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202102", "13646"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202112", "12393"));
    validateRow(response, List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202101", "13065"));
  }

  @Test
  public void dataElementOperandsAndDataElementsAsFilter() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=dx:cYeuwXTCPkU.pq2XI5kz2BY;cYeuwXTCPkU,ou:ImspTQPwCqd")
            .add("skipData=false")
            .add("displayProperty=NAME")
            .add("skipMeta=true")
            .add("dimension=pe:MONTHS_THIS_YEAR")
            .add("relativePeriodDate=2021-10-04");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(24)))
        .body("height", equalTo(24))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData = "{}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("202109", "20433"));
    validateRow(response, List.of("202107", "21130"));
    validateRow(response, List.of("202108", "20413"));
    validateRow(response, List.of("202105", "23726"));
    validateRow(response, List.of("202106", "23904"));
    validateRow(response, List.of("202103", "19574"));
    validateRow(response, List.of("202104", "18403"));
    validateRow(response, List.of("202101", "17269"));
    validateRow(response, List.of("202112", "15183"));
    validateRow(response, List.of("202102", "18488"));
    validateRow(response, List.of("202110", "16113"));
    validateRow(response, List.of("202111", "19453"));
    validateRow(response, List.of("202101", "13065"));
    validateRow(response, List.of("202112", "12393"));
    validateRow(response, List.of("202102", "13646"));
    validateRow(response, List.of("202110", "12797"));
    validateRow(response, List.of("202111", "15278"));
    validateRow(response, List.of("202109", "16071"));
    validateRow(response, List.of("202107", "16618"));
    validateRow(response, List.of("202108", "16366"));
    validateRow(response, List.of("202105", "19287"));
    validateRow(response, List.of("202106", "19307"));
    validateRow(response, List.of("202103", "15409"));
    validateRow(response, List.of("202104", "13984"));
  }
}
