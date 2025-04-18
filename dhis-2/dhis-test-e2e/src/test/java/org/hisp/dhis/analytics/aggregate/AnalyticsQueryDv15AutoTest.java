/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dependsOn.CreatedResource;
import org.hisp.dhis.dependsOn.DependsOn;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
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
  @DependsOn(
      files = {"ind-expected-pregnancies.json"},
      delete = true)
  public void subExpressionIndicator(List<CreatedResource> deps) throws JSONException {
    // Given
    String uid = deps.get(0).uid();
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:Rp268JB6Ne4;cDw53Ej8rju;iPcreOldeV9;jjtzkzrmG7s")
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:%s,pe:2021".formatted(uid));

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
        "{\"items\":{\"cDw53Ej8rju\":{\"name\":\"Afro Arab Clinic\"},\"%s\":{\"name\":\"Expected Pregnancies\"},\"iPcreOldeV9\":{\"name\":\"Benguema MI Room\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"ou\":{\"name\":\"Organisation unit\"},\"2021\":{\"name\":\"2021\"},\"Rp268JB6Ne4\":{\"name\":\"Adonkia CHP\"},\"jjtzkzrmG7s\":{\"name\":\"Banana Island MCHP\"}},\"dimensions\":{\"dx\":[\"%s\"],\"pe\":[\"2021\"],\"ou\":[\"Rp268JB6Ne4\",\"cDw53Ej8rju\",\"iPcreOldeV9\",\"jjtzkzrmG7s\"],\"co\":[]}}"
            .formatted(uid, uid);
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
    validateRow(response, List.of(uid, "2021", "2.0", "2.0", "1.0", "1.0", "1", "1"));
  }

  @Test
  @DependsOn(
      files = {"ind-expected-pregnancies.json"},
      delete = true)
  public void subExpressionIndicatorSingleValue(List<CreatedResource> deps) throws JSONException {
    // Given
    String uid = deps.get(0).uid();
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:Rp268JB6Ne4;cDw53Ej8rju;jjtzkzrmG7s;iPcreOldeV9,pe:2021")
            .add("skipData=true")
            .add("includeMetadataDetails=true")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:%s".formatted(uid));

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(0)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(0));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"cDw53Ej8rju\":{\"uid\":\"cDw53Ej8rju\",\"code\":\"OU_278371\",\"name\":\"Afro Arab Clinic\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"%s\":{\"uid\":\"%s\",\"name\":\"Expected Pregnancies\",\"dimensionItemType\":\"INDICATOR\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"indicatorType\":{\"name\":\"Number (Factor 1)\",\"displayName\":\"Number (Factor 1)\",\"factor\":1,\"number\":true}},\"iPcreOldeV9\":{\"uid\":\"iPcreOldeV9\",\"code\":\"OU_278379\",\"name\":\"Benguema MI Room\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"dx\":{\"uid\":\"dx\",\"name\":\"Data\",\"dimensionType\":\"DATA_X\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"Rp268JB6Ne4\":{\"uid\":\"Rp268JB6Ne4\",\"code\":\"OU_651071\",\"name\":\"Adonkia CHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"2021\":{\"uid\":\"2021\",\"code\":\"2021\",\"name\":\"2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-01-01T00:00:00.000\",\"endDate\":\"2021-12-31T00:00:00.000\"},\"jjtzkzrmG7s\":{\"uid\":\"jjtzkzrmG7s\",\"code\":\"OU_278384\",\"name\":\"Banana Island MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"dx\":[\"%s\"],\"pe\":[\"2021\"],\"ou\":[\"Rp268JB6Ne4\",\"cDw53Ej8rju\",\"jjtzkzrmG7s\",\"iPcreOldeV9\"],\"co\":[]}}"
            .formatted(uid, uid, uid);
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);
  }
}
