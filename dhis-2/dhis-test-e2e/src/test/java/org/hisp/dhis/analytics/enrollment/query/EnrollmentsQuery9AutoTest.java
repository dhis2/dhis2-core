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
package org.hisp.dhis.analytics.enrollment.query;

import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowValueByName;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.dependsOn.DependsOn;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class EnrollmentsQuery9AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  @DependsOn(
      files = {"pi-true-only-data-element.json"},
      delete = true)
  public void queryProgramIndicatorWithTrueOnlyDataElement() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("asc=enrollmentdate")
            .add("headers=ouname,enrollmentdate,PItrueOnly1")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=LAST_5_YEARS")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("outputType=ENROLLMENT")
            .add("page=1")
            .add("dimension=PItrueOnly1:EQ:1,ou:ImspTQPwCqd")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        6,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"jdRD35YwbRH\":{\"uid\":\"jdRD35YwbRH\",\"name\":\"Sputum smear microscopy test\",\"description\":\"Sputum smear microscopy test\"},\"2022\":{\"uid\":\"2022\",\"code\":\"2022\",\"name\":\"2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-01-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"},\"2021\":{\"uid\":\"2021\",\"code\":\"2021\",\"name\":\"2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-01-01T00:00:00.000\",\"endDate\":\"2021-12-31T00:00:00.000\"},\"2020\":{\"uid\":\"2020\",\"code\":\"2020\",\"name\":\"2020\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2020-01-01T00:00:00.000\",\"endDate\":\"2020-12-31T00:00:00.000\"},\"ZkbAXlQUYJG\":{\"uid\":\"ZkbAXlQUYJG\",\"name\":\"TB visit\",\"description\":\"Routine TB visit\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"PItrueOnly1\":{\"uid\":\"PItrueOnly1\",\"code\":\"ENROLLMENT_TRUE_ONLY_DATA_ELEMENT\",\"name\":\"ENROLLMENT_TRUE_ONLY_DATA_ELEMENT\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"2019\":{\"uid\":\"2019\",\"code\":\"2019\",\"name\":\"2019\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2019-01-01T00:00:00.000\",\"endDate\":\"2019-12-31T00:00:00.000\"},\"2018\":{\"uid\":\"2018\",\"code\":\"2018\",\"name\":\"2018\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2018-01-01T00:00:00.000\",\"endDate\":\"2018-12-31T00:00:00.000\"},\"EPEcjy3FWmI\":{\"uid\":\"EPEcjy3FWmI\",\"name\":\"Lab monitoring\",\"description\":\"Laboratory monitoring\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"ur1Edk5Oe2n\":{\"uid\":\"ur1Edk5Oe2n\",\"name\":\"TB program\"},\"enrollmentdate\":{\"name\":\"Start of treatment date\"}},\"dimensions\":{\"enrollmentdate\":[\"2018\",\"2019\",\"2020\",\"2021\",\"2022\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"PItrueOnly1\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentdate",
        "Start of treatment date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "PItrueOnly1",
        "ENROLLMENT_TRUE_ONLY_DATA_ELEMENT",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "PItrueOnly1", "1");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2021-05-14 12:35:24.03");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 2, "PItrueOnly1", "1");
    validateRowValueByName(response, actualHeaders, 2, "enrollmentdate", "2021-10-15 12:34:17.849");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "ouname", "Motorbong MCHP");
    validateRowValueByName(response, actualHeaders, 4, "PItrueOnly1", "1");
    validateRowValueByName(response, actualHeaders, 4, "enrollmentdate", "2022-02-18 12:27:49.129");

    // Validate selected values for row index 5
    validateRowValueByName(response, actualHeaders, 5, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 5, "PItrueOnly1", "1");
    validateRowValueByName(response, actualHeaders, 5, "enrollmentdate", "2022-08-11 12:32:30.524");
  }
}
