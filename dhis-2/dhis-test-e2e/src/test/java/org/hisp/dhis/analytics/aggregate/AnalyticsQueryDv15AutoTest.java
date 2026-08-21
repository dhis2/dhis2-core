/*
 * Copyright (c) 2004-2025, University of Oslo
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
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowExists;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dependsOn.DependsOn;
import org.hisp.dhis.test.e2e.dependsOn.Resource;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/analytics" aggregate endpoint. */
public class AnalyticsQueryDv15AutoTest extends AnalyticsApiTest {
  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void subExpressionIndicator() throws JSONException {

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:Rp268JB6Ne4;cDw53Ej8rju;iPcreOldeV9;jjtzkzrmG7s")
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:KjElpUc6jeX,pe:2021");

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
        "{\"items\":{\"cDw53Ej8rju\":{\"name\":\"Afro Arab Clinic\"},\"KjElpUc6jeX\":{\"name\":\"Expected Pregnancies\"},\"iPcreOldeV9\":{\"name\":\"Benguema MI Room\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"ou\":{\"name\":\"Organisation unit\"},\"2021\":{\"name\":\"2021\"},\"Rp268JB6Ne4\":{\"name\":\"Adonkia CHP\"},\"jjtzkzrmG7s\":{\"name\":\"Banana Island MCHP\"}},\"dimensions\":{\"dx\":[\"KjElpUc6jeX\"],\"pe\":[\"2021\"],\"ou\":[\"Rp268JB6Ne4\",\"cDw53Ej8rju\",\"iPcreOldeV9\",\"jjtzkzrmG7s\"],\"co\":[]}}";
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
    validateRow(response, List.of("KjElpUc6jeX", "2021", "2.0", "2.0", "1.0", "1.0", "1", "1"));
  }

  @Test
  public void subExpressionIndicatorSingleValue() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:Rp268JB6Ne4;cDw53Ej8rju;jjtzkzrmG7s;iPcreOldeV9,pe:2021")
            .add("skipData=true")
            .add("includeMetadataDetails=true")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:KjElpUc6jeX");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(409)
        .body(
            "message",
            equalTo("Periods as filter not supported with Indicator with period offset"));
  }

  @Test
  @DependsOn(
      files = {"ind-subexpression-no-offset.json"},
      delete = true)
  public void subExpressionIndicatorWithPeriodAndOrgUnitAsFilter(List<Resource> dependencies) {
    String indicatorUid = dependencies.get(0).uid();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:Rp268JB6Ne4;cDw53Ej8rju;iPcreOldeV9;jjtzkzrmG7s,pe:2021")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=true")
            .add("dimension=dx:" + indicatorUid);

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(2));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows. Three of the four facilities reported ANC 1st visit in 2021.
    validateRow(response, List.of(indicatorUid, "3.0"));
  }

  @Test
  public void weeklyStartingOnMonday() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:USER_ORGUNIT")
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:HS9zqaBdOQ4,pe:THIS_WEEK")
            .add("relativePeriodDate=2022-02-01");

    // When
    ApiResponse response = actions.get(params);

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"HS9zqaBdOQ4\":{\"name\":\"IDSR Plague\"},\"THIS_WEEK\":{\"name\":\"This week\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"2022W5\":{\"name\":\"Week 5 - 2022-01-31 - 2022-02-06\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"ou\":{\"name\":\"Organisation unit\"},\"HllvX50cXC0\":{\"name\":\"default\"}},\"dimensions\":{\"dx\":[\"HS9zqaBdOQ4\"],\"pe\":[\"2022W5\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"HllvX50cXC0\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "numerator",
        "Numerator",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "denominator",
        "Denominator",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response, actualHeaders, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "multiplier",
        "Multiplier",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response, actualHeaders, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.ofEntries(
            Map.entry("dx", "HS9zqaBdOQ4"),
            Map.entry("pe", "2022W5"),
            Map.entry("value", "466"),
            Map.entry("numerator", ""),
            Map.entry("denominator", ""),
            Map.entry("factor", ""),
            Map.entry("multiplier", ""),
            Map.entry("divisor", "")));
  }

  @Test
  @DependsOn(
      files = {"pi-max-sum-org-unit.json"},
      delete = true)
  public void programIndicatorMaxSumOrgUnitWithChildOu(List<Resource> dependencies) {
    String indicatorUid = dependencies.get(0).uid();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:AXZq6q7Dr6E;BGGmAwx33dj")
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=true")
            .add("dimension=dx:" + indicatorUid + ",pe:2021;2022;2020");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(2)))
        .body("height", equalTo(2))
        .body("width", equalTo(8));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows. Three of the four facilities reported ANC 1st visit in 2021.
    validateRow(response, List.of(indicatorUid, "2022", "64657.0", "", "", "", "", ""));
    validateRow(response, List.of(indicatorUid, "2021", "67138.0", "", "", "", "", ""));
  }

  @Test
  @DependsOn(
      files = {"pi-max-sum-org-unit.json"},
      delete = true)
  public void programIndicatorMaxSumOrgUnitWithoutChildOu(List<Resource> dependencies) {
    String indicatorUid = dependencies.get(0).uid();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:AXZq6q7Dr6E")
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=true")
            .add("dimension=dx:" + indicatorUid + ",pe:2021;2022;2020");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(2)))
        .body("height", equalTo(2))
        .body("width", equalTo(8));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows. Three of the four facilities reported ANC 1st visit in 2021.
    validateRow(response, List.of(indicatorUid, "2022", "187.0", "", "", "", "", ""));
    validateRow(response, List.of(indicatorUid, "2021", "188.0", "", "", "", "", ""));
  }
}
