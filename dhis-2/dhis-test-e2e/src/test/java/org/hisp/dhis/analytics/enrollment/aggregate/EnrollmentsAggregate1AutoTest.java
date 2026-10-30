/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.enrollment.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowExists;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/enrollments/aggregate" endpoint. */
public class EnrollmentsAggregate1AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void queryAggregatedenrollmentsbirthgenderthisyearlevel4orgtest() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=SHORTNAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("dimension=ou:DiszpKrYNg8,pe:THIS_YEAR,A03MvHHogjR.cejWyOfXge6")
            .add("relativePeriodDate=2023-08-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":50,\"page\":1},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"A03MvHHogjR.cejWyOfXge6\":{\"name\":\"Gender\"},\"2023\":{\"name\":\"2023\"}},\"dimensions\":{\"pe\":[\"2023\"],\"ou\":[\"DiszpKrYNg8\"],\"A03MvHHogjR.cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}\n";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 3, "A03MvHHogjR.cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(response, List.of("5", "DiszpKrYNg8", "2023", "Female"));
    validateRow(response, List.of("5", "DiszpKrYNg8", "2023", "Male"));
  }

  @Test
  public void queryAggregatedenrollmentsbirthbcgdosethisyearlevel4orgtest() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=SHORTNAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("dimension=ou:DiszpKrYNg8,pe:THIS_YEAR,A03MvHHogjR.bx6fsa0t90x")
            .add("relativePeriodDate=2023-08-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":50,\"page\":1},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2023\":{\"name\":\"2023\"},\"A03MvHHogjR.bx6fsa0t90x\":{\"name\":\"MCH BCG dose\"}},\"dimensions\":{\"pe\":[\"2023\"],\"ou\":[\"DiszpKrYNg8\"],\"A03MvHHogjR.bx6fsa0t90x\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        3,
        "A03MvHHogjR.bx6fsa0t90x",
        "BCG dose",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);

    // Assert rows.
    validateRow(response, List.of("3", "DiszpKrYNg8", "2023", "0"));
    validateRow(response, List.of("5", "DiszpKrYNg8", "2023", "1"));
    validateRow(response, List.of("3", "DiszpKrYNg8", "2023", ""));
  }

  @Test
  public void queryAggregatedenrollmentsbirthbcgdosegenderthisyearlevel4org() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=SHORTNAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add(
                "dimension=ou:DiszpKrYNg8,pe:THIS_YEAR,A03MvHHogjR.bx6fsa0t90x,A03MvHHogjR.cejWyOfXge6")
            .add("relativePeriodDate=2023-08-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(7)))
        .body("height", equalTo(7))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":50,\"page\":1},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"A03MvHHogjR.cejWyOfXge6\":{\"name\":\"Gender\"},\"2023\":{\"name\":\"2023\"},\"A03MvHHogjR.bx6fsa0t90x\":{\"name\":\"MCH BCG dose\"}},\"dimensions\":{\"pe\":[\"2023\"],\"ou\":[\"DiszpKrYNg8\"],\"A03MvHHogjR.bx6fsa0t90x\":[],\"A03MvHHogjR.cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 3, "A03MvHHogjR.cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        4,
        "A03MvHHogjR.bx6fsa0t90x",
        "BCG dose",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);

    // Assert rows.
    validateRow(response, List.of("1", "DiszpKrYNg8", "2023", "Female", "0"));
    validateRow(response, List.of("3", "DiszpKrYNg8", "2023", "Female", "1"));
    validateRow(response, List.of("1", "DiszpKrYNg8", "2023", "Female", ""));
    validateRow(response, List.of("2", "DiszpKrYNg8", "2023", "Male", "0"));
    validateRow(response, List.of("2", "DiszpKrYNg8", "2023", "Male", "1"));
    validateRow(response, List.of("1", "DiszpKrYNg8", "2023", "Male", ""));
  }

  @Test
  public void queryAggregatedenrollmentsbirthgenderlast12monthslevel4org() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=SHORTNAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("dimension=ou:DiszpKrYNg8,pe:LAST_12_MONTHS,A03MvHHogjR.cejWyOfXge6")
            .add("relativePeriodDate=2023-08-01");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(11)))
        .body("height", equalTo(11))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"isLastPage\":true,\"pageSize\":50,\"page\":1},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"202208\":{\"name\":\"August 2022\"},\"202307\":{\"name\":\"July 2023\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"202210\":{\"name\":\"October 2022\"},\"pe\":{},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"A03MvHHogjR.cejWyOfXge6\":{\"name\":\"Gender\"}},\"dimensions\":{\"pe\":[\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\",\"202307\"],\"ou\":[\"DiszpKrYNg8\"],\"A03MvHHogjR.cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}\n";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 3, "A03MvHHogjR.cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(response, List.of("2", "DiszpKrYNg8", "202208", "Female"));
    validateRow(response, List.of("1", "DiszpKrYNg8", "202209", "Female"));
    validateRow(response, List.of("1", "DiszpKrYNg8", "202209", "Male"));
    validateRow(response, List.of("1", "DiszpKrYNg8", "202209", ""));
    validateRow(response, List.of("4", "DiszpKrYNg8", "202211", "Female"));
    validateRow(response, List.of("5", "DiszpKrYNg8", "202211", "Male"));
    validateRow(response, List.of("3", "DiszpKrYNg8", "202301", "Female"));
    validateRow(response, List.of("4", "DiszpKrYNg8", "202301", "Male"));
    validateRow(response, List.of("1", "DiszpKrYNg8", "202302", "Male"));
    validateRow(response, List.of("1", "DiszpKrYNg8", "202305", "Female"));
  }

  @Test
  public void orgUnitGroupSetDimension() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("dimension=uIuxlbV1vRT,ou:ImspTQPwCqd,pe:2022;2023");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        10,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"nlX2VoouN63\":{\"name\":\"Eastern Area\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"jqBqIXoXpfy\":{\"name\":\"Southern Area\"},\"2023\":{\"name\":\"2023\"},\"2022\":{\"name\":\"2022\"},\"uIuxlbV1vRT\":{\"name\":\"Area\"},\"J40PpdN4Wkk\":{\"name\":\"Northern Area\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"b0EsAxm8Nge\":{\"name\":\"Western Area\"},\"pe\":{},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"pe\":[\"2022\",\"2023\"],\"ou\":[\"ImspTQPwCqd\"],\"uIuxlbV1vRT\":[\"J40PpdN4Wkk\",\"jqBqIXoXpfy\",\"b0EsAxm8Nge\",\"nlX2VoouN63\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ou",
        "Organisation unit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "uIuxlbV1vRT", "Area", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "1942", "ou", "ImspTQPwCqd", "uIuxlbV1vRT", "b0EsAxm8Nge", "pe", "2023"));

    // Validate row exists with values from original row index 3
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "2335", "ou", "ImspTQPwCqd", "uIuxlbV1vRT", "J40PpdN4Wkk", "pe", "2023"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "1163", "ou", "ImspTQPwCqd", "uIuxlbV1vRT", "", "pe", "2022"));

    // Validate row exists with values from original row index 9
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "962", "ou", "ImspTQPwCqd", "uIuxlbV1vRT", "nlX2VoouN63", "pe", "2023"));
  }

  @Test
  public void orgUnitGroupSetDimensionWithItems() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("dimension=uIuxlbV1vRT:jqBqIXoXpfy;nlX2VoouN63,ou:ImspTQPwCqd,pe:2022");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        2,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"nlX2VoouN63\":{\"name\":\"Eastern Area\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"jqBqIXoXpfy\":{\"name\":\"Southern Area\"},\"2022\":{\"name\":\"2022\"},\"uIuxlbV1vRT\":{\"name\":\"Area\"}},\"dimensions\":{\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\"],\"uIuxlbV1vRT\":[\"jqBqIXoXpfy\",\"nlX2VoouN63\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ou",
        "Organisation unit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "uIuxlbV1vRT", "Area", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "pe", "Period", "TEXT", "java.lang.String", false, true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "1333", "ou", "ImspTQPwCqd", "uIuxlbV1vRT", "nlX2VoouN63", "pe", "2022"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response,
        actualHeaders,
        Map.of("value", "2722", "ou", "ImspTQPwCqd", "uIuxlbV1vRT", "jqBqIXoXpfy", "pe", "2022"));
  }
}
