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
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/analytics" aggregate endpoint. */
public class AnalyticsQueryDv16AutoTest extends AnalyticsApiTest {
  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void tableLayoutKeepOrderOfColumnsHeaders() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:USER_ORGUNIT")
            .add("tableLayout=true")
            .add("columns=dx")
            .add("completedOnly=false")
            .add("rows=pe")
            .add("dimension=dx:Uvn6LCg7dVU;OdiHJayrsKo;sB79w2hiLp8;cYeuwXTCPkU,pe:LAST_12_MONTHS")
            .add("skipRounding=false")
            .add("relativePeriodDate=2023-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(12)))
        .body("height", equalTo(12))
        .body("width", equalTo(8))
        .body("headerWidth", equalTo(8));

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"sB79w2hiLp8\":{\"name\":\"ANC 3 Coverage\"},\"ou\":{\"name\":\"Organisation unit\"},\"OdiHJayrsKo\":{\"name\":\"ANC 2 Coverage\"},\"202208\":{\"name\":\"August 2022\"},\"202209\":{\"name\":\"September 2022\"},\"202206\":{\"name\":\"June 2022\"},\"202207\":{\"name\":\"July 2022\"},\"202204\":{\"name\":\"April 2022\"},\"202205\":{\"name\":\"May 2022\"},\"202202\":{\"name\":\"February 2022\"},\"202203\":{\"name\":\"March 2022\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202211\":{\"name\":\"November 2022\"},\"202201\":{\"name\":\"January 2022\"},\"202212\":{\"name\":\"December 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202210\":{\"name\":\"October 2022\"},\"dx\":{\"name\":\"Data\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"pe\":{\"name\":\"Period\"},\"cYeuwXTCPkU\":{\"name\":\"ANC 2nd visit\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\",\"OdiHJayrsKo\",\"sB79w2hiLp8\",\"cYeuwXTCPkU\"],\"pe\":[\"202201\",\"202202\",\"202203\",\"202204\",\"202205\",\"202206\",\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeader(response, 0, "Period ID", "periodid", "TEXT", "java.lang.String", true, true);
    validateHeader(response, 1, "Period", "periodname", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 2, "Period code", "periodcode", "TEXT", "java.lang.String", true, true);
    validateHeader(
        response,
        3,
        "Period description",
        "perioddescription",
        "TEXT",
        "java.lang.String",
        true,
        true);
    validateHeader(
        response,
        4,
        "anc 1 coverage",
        "ANC 1 Coverage",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response,
        5,
        "anc 2 coverage",
        "ANC 2 Coverage",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response,
        6,
        "anc 3 coverage",
        "ANC 3 Coverage",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeader(
        response, 7, "anc 2nd visit", "ANC 2nd visit", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values
    validateRow(
        response,
        List.of("202208", "August 2022", "202208", "", "104.11", "96.68", "69.53", "20433"));

    validateRow(
        response,
        List.of("202207", "July 2022", "202207", "", "107.05", "100.34", "70.47", "21208"));

    validateRow(
        response,
        List.of("202212", "December 2022", "202212", "", "75.58", "70.2", "50.13", "14837"));

    validateRow(
        response,
        List.of("202203", "March 2022", "202203", "", "87.89", "87.14", "59.17", "18418"));

    validateRow(
        response,
        List.of("202205", "May 2022", "202205", "", "139.39", "112.26", "74.28", "23726"));

    validateRow(
        response,
        List.of("202210", "October 2022", "202210", "", "89.75", "88.93", "69.28", "18795"));

    validateRow(
        response,
        List.of("202201", "January 2022", "202201", "", "94.72", "82.08", "56.13", "17347"));

    validateRow(
        response,
        List.of("202204", "April 2022", "202204", "", "106.96", "95.7", "68.12", "19574"));

    validateRow(
        response,
        List.of("202206", "June 2022", "202206", "", "116.42", "116.84", "76.63", "23898"));

    validateRow(
        response,
        List.of("202211", "November 2022", "202211", "", "87.64", "78.78", "57.81", "16113"));

    validateRow(
        response,
        List.of("202202", "February 2022", "202202", "", "98.8", "97.32", "63.72", "18578"));

    validateRow(
        response,
        List.of("202209", "September 2022", "202209", "", "109.52", "99.9", "74.92", "20433"));
  }
}
