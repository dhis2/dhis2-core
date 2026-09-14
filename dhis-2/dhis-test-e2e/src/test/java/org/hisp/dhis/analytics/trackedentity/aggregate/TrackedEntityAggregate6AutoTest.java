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
public class TrackedEntityAggregate6AutoTest extends AnalyticsApiTest {
  private final AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  @Test
  public void aggregateCountByBareOrgUnitDimension() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder().add("totalPages=false").add("pageSize=50").add("dimension=ou");

    // When
    ApiResponse response = actions.aggregate().get("Zy2SEgA61ys", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        2,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"g8upMTyEZGZ\":{\"name\":\"Njandama MCHP\"},\"qDkgAbB5Jlk\":{\"name\":\"Malaria case diagnosis, treatment and investigation\"},\"ou\":{\"name\":\"Organisation unit\"},\"eHvTba5ijAh\":{\"name\":\"Case outcome\"},\"wYTF0YCHMWr\":{\"name\":\"Case investigation & classification\"},\"hYyB7FUS5eR\":{\"name\":\"Diagnosis & treatment\"},\"C0aLZo75dgJ\":{\"name\":\"Household investigation\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"DiszpKrYNg8\",\"g8upMTyEZGZ\"]}}";
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
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("ou", "DiszpKrYNg8", "value", "32"));

    // Validate row exists with values from original row index 1
    validateRowExists(response, actualHeaders, Map.of("ou", "g8upMTyEZGZ", "value", "5"));
  }

  @Test
  public void aggregateCountByBareOrgUnitAndOrgUnitNameDimensions() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=ou,ouname");

    // When
    ApiResponse response = actions.aggregate().get("Zy2SEgA61ys", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        2,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"g8upMTyEZGZ\":{\"name\":\"Njandama MCHP\"},\"qDkgAbB5Jlk\":{\"name\":\"Malaria case diagnosis, treatment and investigation\"},\"ou\":{\"name\":\"Organisation unit\"},\"eHvTba5ijAh\":{\"name\":\"Case outcome\"},\"wYTF0YCHMWr\":{\"name\":\"Case investigation & classification\"},\"hYyB7FUS5eR\":{\"name\":\"Diagnosis & treatment\"},\"C0aLZo75dgJ\":{\"name\":\"Household investigation\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"g8upMTyEZGZ\",\"DiszpKrYNg8\"],\"ouname\":[]}}";
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
        "ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
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
        Map.of("ou", "g8upMTyEZGZ", "ouname", "Njandama MCHP", "value", "5"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response,
        actualHeaders,
        Map.of("ou", "DiszpKrYNg8", "ouname", "Ngelehun CHC", "value", "32"));
  }

  @Test
  public void aggregateCountByLastUpdatedAliasDimension() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=LAST_UPDATED");

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
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"eHvTba5ijAh\":{\"name\":\"Case outcome\"},\"lastupdated\":{\"name\":\"Last updated\"},\"wYTF0YCHMWr\":{\"name\":\"Case investigation & classification\"},\"qDkgAbB5Jlk\":{\"name\":\"Malaria case diagnosis, treatment and investigation\"},\"hYyB7FUS5eR\":{\"name\":\"Diagnosis & treatment\"},\"C0aLZo75dgJ\":{\"name\":\"Household investigation\"}},\"dimensions\":{\"pe\":[],\"lastupdated\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated",
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
        response, actualHeaders, Map.of("lastupdated", "2019-08-21 13:31:04.596", "value", "1"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response, actualHeaders, Map.of("lastupdated", "2019-08-21 13:31:33.41", "value", "1"));

    // Validate row exists with values from original row index 12
    validateRowExists(
        response, actualHeaders, Map.of("lastupdated", "2019-08-21 13:30:25.656", "value", "1"));

    // Validate row exists with values from original row index 18
    validateRowExists(
        response, actualHeaders, Map.of("lastupdated", "2019-08-21 13:31:12.627", "value", "1"));

    // Validate row exists with values from original row index 24
    validateRowExists(
        response, actualHeaders, Map.of("lastupdated", "2019-08-21 13:31:09.399", "value", "1"));

    // Validate row exists with values from original row index 30
    validateRowExists(
        response, actualHeaders, Map.of("lastupdated", "2019-08-21 13:30:05.037", "value", "1"));

    // Validate row exists with values from original row index 36
    validateRowExists(
        response, actualHeaders, Map.of("lastupdated", "2019-08-21 13:31:02.279", "value", "1"));
  }

  @Test
  public void aggregateCountByCreatedAliasAndIsoPeriod() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=CREATED:2019");

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
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"eHvTba5ijAh\":{\"name\":\"Case outcome\"},\"wYTF0YCHMWr\":{\"name\":\"Case investigation & classification\"},\"qDkgAbB5Jlk\":{\"name\":\"Malaria case diagnosis, treatment and investigation\"},\"hYyB7FUS5eR\":{\"name\":\"Diagnosis & treatment\"},\"C0aLZo75dgJ\":{\"name\":\"Household investigation\"},\"created\":{\"name\":\"Created\"}},\"dimensions\":{\"pe\":[],\"created\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
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
        response, actualHeaders, Map.of("created", "2019-08-21 13:24:04.74", "value", "1"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response, actualHeaders, Map.of("created", "2019-08-21 13:23:30.144", "value", "1"));

    // Validate row exists with values from original row index 12
    validateRowExists(
        response, actualHeaders, Map.of("created", "2019-08-21 13:25:29.756", "value", "1"));

    // Validate row exists with values from original row index 18
    validateRowExists(
        response, actualHeaders, Map.of("created", "2019-08-21 13:24:38.952", "value", "1"));

    // Validate row exists with values from original row index 24
    validateRowExists(
        response, actualHeaders, Map.of("created", "2019-08-21 13:24:21.658", "value", "1"));

    // Validate row exists with values from original row index 30
    validateRowExists(
        response, actualHeaders, Map.of("created", "2019-08-21 13:23:45.456", "value", "1"));

    // Validate row exists with values from original row index 36
    validateRowExists(
        response, actualHeaders, Map.of("created", "2019-08-21 13:24:59.811", "value", "1"));
  }
}
