/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.aggregate;

import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowExists;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsTrackedEntityActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/trackedEntities/aggregate" endpoint. */
public class TrackedEntityAggregate7AutoTest extends AnalyticsApiTest {
  private final AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  @Test
  public void aggregateCountByOrgUnitAndCreatedAliasDateRange() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=ou,CREATED:2019-01-01_2019-12-31");

    // When
    ApiResponse response = actions.aggregate().get("Zy2SEgA61ys", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        37,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"g8upMTyEZGZ\":{\"name\":\"Njandama MCHP\"},\"qDkgAbB5Jlk\":{\"name\":\"Malaria case diagnosis, treatment and investigation\"},\"ou\":{\"name\":\"Organisation unit\"},\"created\":{\"name\":\"Created\"},\"eHvTba5ijAh\":{\"name\":\"Case outcome\"},\"wYTF0YCHMWr\":{\"name\":\"Case investigation & classification\"},\"hYyB7FUS5eR\":{\"name\":\"Diagnosis & treatment\"},\"C0aLZo75dgJ\":{\"name\":\"Household investigation\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"DiszpKrYNg8\",\"g8upMTyEZGZ\"],\"created\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
        response,
        actualHeaders,
        "created",
        "Created",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("ou", "DiszpKrYNg8", "created", "2019-08-21 13:23:37.63", "value", "1"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response,
        actualHeaders,
        Map.of("ou", "DiszpKrYNg8", "created", "2019-08-21 13:25:07.634", "value", "1"));

    // Validate row exists with values from original row index 12
    validateRowExists(
        response,
        actualHeaders,
        Map.of("ou", "DiszpKrYNg8", "created", "2019-08-21 13:24:21.658", "value", "1"));

    // Validate row exists with values from original row index 18
    validateRowExists(
        response,
        actualHeaders,
        Map.of("ou", "DiszpKrYNg8", "created", "2019-08-21 13:25:38.022", "value", "1"));

    // Validate row exists with values from original row index 24
    validateRowExists(
        response,
        actualHeaders,
        Map.of("ou", "DiszpKrYNg8", "created", "2019-08-21 13:25:47.06", "value", "1"));

    // Validate row exists with values from original row index 30
    validateRowExists(
        response,
        actualHeaders,
        Map.of("ou", "DiszpKrYNg8", "created", "2019-08-21 13:24:47.119", "value", "1"));

    // Validate row exists with values from original row index 36
    validateRowExists(
        response,
        actualHeaders,
        Map.of("ou", "DiszpKrYNg8", "created", "2019-08-21 13:24:04.74", "value", "1"));
  }
}
