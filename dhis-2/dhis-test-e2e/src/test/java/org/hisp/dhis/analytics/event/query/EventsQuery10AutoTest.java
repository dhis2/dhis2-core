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
package org.hisp.dhis.analytics.event.query;

import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowValueByName;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/query" endpoint. */
public class EventsQuery10AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void enrollmentOccurredDateParam() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=eventdate,lastupdated")
            .add("headers=psi,incidentdate,ouname")
            .add("stage=A03MvHHogjR")
            .add("enrollmentOccurredDate=2022-01-01_2022-01-01")
            .add("totalPages=true")
            .add("pageSize=100")
            .add("dimension=ou:ImspTQPwCqd");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        32,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":32,\"pageSize\":100,\"pageCount\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"incidentdate\":{\"name\":\"Date of birth\"},\"2022-01-01_2022-01-01\":{\"name\":\"2022-01-01 - 2022-01-01\"},\"20220101\":{\"name\":\"2022-01-01\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"incidentdate\":[\"20220101\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "incidentdate",
        "Date of birth",
        "DATETIME",
        "java.time.LocalDateTime",
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "l7AKlV3Xa0b");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Feiba CHP");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2022-01-01 12:05:00.0");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "psi", "zfXl09nzPf9");
    validateRowValueByName(response, actualHeaders, 6, "ouname", "Maselleh MCHP");
    validateRowValueByName(response, actualHeaders, 6, "incidentdate", "2022-01-01 12:05:00.0");

    // Validate selected values for row index 12
    validateRowValueByName(response, actualHeaders, 12, "psi", "KDuMaq22jYK");
    validateRowValueByName(response, actualHeaders, 12, "ouname", "Niagorehun CHP");
    validateRowValueByName(response, actualHeaders, 12, "incidentdate", "2022-01-01 12:05:00.0");

    // Validate selected values for row index 18
    validateRowValueByName(response, actualHeaders, 18, "psi", "gh94wOXOIGX");
    validateRowValueByName(response, actualHeaders, 18, "ouname", "Yorgbofore MCHP");
    validateRowValueByName(response, actualHeaders, 18, "incidentdate", "2022-01-01 12:05:00.0");

    // Validate selected values for row index 24
    validateRowValueByName(response, actualHeaders, 24, "psi", "Rg527IKf27G");
    validateRowValueByName(response, actualHeaders, 24, "ouname", "Mano Menima CHP");
    validateRowValueByName(response, actualHeaders, 24, "incidentdate", "2022-01-01 12:05:00.0");

    // Validate selected values for row index 30
    validateRowValueByName(response, actualHeaders, 30, "psi", "OkBOZkeYVfZ");
    validateRowValueByName(response, actualHeaders, 30, "ouname", "Jormu MCHP");
    validateRowValueByName(response, actualHeaders, 30, "incidentdate", "2022-01-01 12:05:00.0");

    // Validate selected values for row index 31
    validateRowValueByName(response, actualHeaders, 31, "psi", "H61O1Y578zg");
    validateRowValueByName(response, actualHeaders, 31, "ouname", "Looking Town MCHP");
    validateRowValueByName(response, actualHeaders, 31, "incidentdate", "2022-01-01 12:05:00.0");
  }

  @Test
  public void enrollmentProgramIndicatorConditionCountsPages() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=eventdate,lastupdated")
            .add("headers=psi,eventdate,lastupdated,qZOBw051LSf")
            .add("totalPages=true")
            .add("pageSize=20")
            .add("dimension=ou:DiszpKrYNg8,pe:202201,qZOBw051LSf:GT:0");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        11,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":11,\"pageSize\":20,\"pageCount\":1},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"202201\":{\"name\":\"January 2022\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"qZOBw051LSf\":{\"name\":\"Child PI Complex\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"DiszpKrYNg8\"],\"qZOBw051LSf\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "qZOBw051LSf",
        "Child PI Complex",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "Ni0yhZ7XhAP");
    validateRowValueByName(response, actualHeaders, 0, "qZOBw051LSf", "13458.8");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "psi", "FsDo0TAPHPI");
    validateRowValueByName(response, actualHeaders, 3, "qZOBw051LSf", "13458.8");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "psi", "ercyn8UqwA4");
    validateRowValueByName(response, actualHeaders, 6, "qZOBw051LSf", "13458.8");

    // Validate selected values for row index 9
    validateRowValueByName(response, actualHeaders, 9, "psi", "L1J3DUtK2hX");
    validateRowValueByName(response, actualHeaders, 9, "qZOBw051LSf", "13458.8");

    // Validate selected values for row index 10
    validateRowValueByName(response, actualHeaders, 10, "psi", "bI6CMm9YWCv");
    validateRowValueByName(response, actualHeaders, 10, "qZOBw051LSf", "13458.8");
  }

  @Test
  public void enrollmentProgramIndicatorConditionExcludesEventsFromPageCount()
      throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=eventdate,lastupdated")
            .add("headers=psi,eventdate,lastupdated,qZOBw051LSf")
            .add("totalPages=true")
            .add("pageSize=20")
            .add("dimension=ou:DiszpKrYNg8,pe:202201,qZOBw051LSf:GT:20000");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        0,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":0,\"pageSize\":20,\"pageCount\":0},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"202201\":{\"name\":\"January 2022\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"qZOBw051LSf\":{\"name\":\"Child PI Complex\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"DiszpKrYNg8\"],\"qZOBw051LSf\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "eventdate",
        "Event date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdated",
        "Last updated on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "qZOBw051LSf",
        "Child PI Complex",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // No rows found in response, skipping row assertions.
  }

  @Test
  public void orgUnitsAtDifferentLevelsDimensionKeepStageAndPeriod() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=eventdate,lastupdated")
            .add("headers=psi,ps,ouname,eventdate")
            .add("stage=A03MvHHogjR")
            .add("totalPages=true")
            .add("pageSize=20")
            .add("page=1")
            .add("dimension=pe:2022,ou:O6uvpzGd5pu;taKiTcaf05H");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        20,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":870,\"pageSize\":20,\"pageCount\":44},\"items\":{\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"2022\":{\"name\":\"2022\"},\"taKiTcaf05H\":{\"name\":\"Mabenteh Community Hospital\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"O6uvpzGd5pu\",\"taKiTcaf05H\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
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
        "eventdate",
        "Report date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "Ni0yhZ7XhAP");
    validateRowValueByName(response, actualHeaders, 0, "eventdate", "2022-01-01 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "psi", "AXWMFgN8C9v");
    validateRowValueByName(response, actualHeaders, 4, "eventdate", "2022-01-02 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 4, "ouname", "Gerehun CHC");

    // Validate selected values for row index 8
    validateRowValueByName(response, actualHeaders, 8, "psi", "owiel45GcVa");
    validateRowValueByName(response, actualHeaders, 8, "eventdate", "2022-01-03 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 8, "ouname", "EDC Unit CHP");

    // Validate selected values for row index 12
    validateRowValueByName(response, actualHeaders, 12, "psi", "CGk6I1zpkXB");
    validateRowValueByName(response, actualHeaders, 12, "eventdate", "2022-01-04 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 12, "ouname", "Tikonko CHC");

    // Validate selected values for row index 16
    validateRowValueByName(response, actualHeaders, 16, "psi", "Flh3WAWRjjg");
    validateRowValueByName(response, actualHeaders, 16, "eventdate", "2022-01-05 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 16, "ouname", "Yemoh MCHP");

    // Validate selected values for row index 19
    validateRowValueByName(response, actualHeaders, 19, "psi", "PLZagZg6dfU");
    validateRowValueByName(response, actualHeaders, 19, "eventdate", "2022-01-06 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 19, "ouname", "Kpetema CHP");
  }

  @Test
  public void orgUnitsAtDifferentLevelsFilterKeepStageAndPeriod() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:O6uvpzGd5pu;taKiTcaf05H")
            .add("asc=eventdate,lastupdated")
            .add("headers=psi,ps,ouname,eventdate")
            .add("stage=A03MvHHogjR")
            .add("totalPages=true")
            .add("pageSize=20")
            .add("page=1")
            .add("dimension=pe:2022");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        20,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":870,\"pageSize\":20,\"pageCount\":44},\"items\":{\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"taKiTcaf05H\":{\"name\":\"Mabenteh Community Hospital\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"O6uvpzGd5pu\",\"taKiTcaf05H\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "psi", "Event", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "ps", "Program stage", "TEXT", "java.lang.String", false, true);
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
        "eventdate",
        "Report date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "psi", "Ni0yhZ7XhAP");
    validateRowValueByName(response, actualHeaders, 0, "eventdate", "2022-01-01 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "psi", "AXWMFgN8C9v");
    validateRowValueByName(response, actualHeaders, 4, "eventdate", "2022-01-02 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 4, "ouname", "Gerehun CHC");

    // Validate selected values for row index 8
    validateRowValueByName(response, actualHeaders, 8, "psi", "owiel45GcVa");
    validateRowValueByName(response, actualHeaders, 8, "eventdate", "2022-01-03 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 8, "ouname", "EDC Unit CHP");

    // Validate selected values for row index 12
    validateRowValueByName(response, actualHeaders, 12, "psi", "CGk6I1zpkXB");
    validateRowValueByName(response, actualHeaders, 12, "eventdate", "2022-01-04 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 12, "ouname", "Tikonko CHC");

    // Validate selected values for row index 16
    validateRowValueByName(response, actualHeaders, 16, "psi", "Flh3WAWRjjg");
    validateRowValueByName(response, actualHeaders, 16, "eventdate", "2022-01-05 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 16, "ouname", "Yemoh MCHP");

    // Validate selected values for row index 19
    validateRowValueByName(response, actualHeaders, 19, "psi", "PLZagZg6dfU");
    validateRowValueByName(response, actualHeaders, 19, "eventdate", "2022-01-06 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 19, "ouname", "Kpetema CHP");
  }
}
