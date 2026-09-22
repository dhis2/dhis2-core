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
public class EventsQuery9AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void eventsSortedByStageOrgUnitNameAscending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=A03MvHHogjR.ouname")
            .add("headers=A03MvHHogjR.ouname,A03MvHHogjR.eventdate")
            .add("stage=A03MvHHogjR")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("pageSize=50")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf")
            .add("eventDate=202112");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        8,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"202112\":{\"name\":\"December 2021\"},\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"eventdate\":{\"name\":\"Event date\"}},\"dimensions\":{\"pe\":[],\"eventdate\":[\"202112\"],\"ou\":[\"lY93YpCxJqf\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.ouname", "Fullah Town (M.Gbanti) MCHP");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.eventdate", "2021-12-23 00:00:00.0");

    // Validate selected values for row index 3
    validateRowValueByName(
        response, actualHeaders, 3, "A03MvHHogjR.ouname", "Mabenteh Community Hospital");
    validateRowValueByName(
        response, actualHeaders, 3, "A03MvHHogjR.eventdate", "2021-12-17 00:00:00.0");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "A03MvHHogjR.ouname", "Tonkomba MCHP");
    validateRowValueByName(
        response, actualHeaders, 6, "A03MvHHogjR.eventdate", "2021-12-03 00:00:00.0");

    // Validate selected values for row index 7
    validateRowValueByName(response, actualHeaders, 7, "A03MvHHogjR.ouname", "Yankasa MCHP");
    validateRowValueByName(
        response, actualHeaders, 7, "A03MvHHogjR.eventdate", "2021-12-05 00:00:00.0");
  }

  @Test
  public void eventsSortedByStageOrgUnitCodeDescendingWithoutMatchingHeader() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=A03MvHHogjR.ouname,A03MvHHogjR.eventdate")
            .add("stage=A03MvHHogjR")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("pageSize=50")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf")
            .add("eventDate=202112")
            .add("desc=A03MvHHogjR.oucode");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        8,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"202112\":{\"name\":\"December 2021\"},\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"eventdate\":{\"name\":\"Event date\"}},\"dimensions\":{\"pe\":[],\"eventdate\":[\"202112\"],\"ou\":[\"lY93YpCxJqf\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.ouname", "Mangay Loko MCHP");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.eventdate", "2021-12-28 00:00:00.0");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "A03MvHHogjR.ouname", "Panlap MCHP");
    validateRowValueByName(
        response, actualHeaders, 3, "A03MvHHogjR.eventdate", "2021-12-10 00:00:00.0");

    // Validate selected values for row index 6
    validateRowValueByName(
        response, actualHeaders, 6, "A03MvHHogjR.ouname", "Fullah Town (M.Gbanti) MCHP");
    validateRowValueByName(
        response, actualHeaders, 6, "A03MvHHogjR.eventdate", "2021-12-23 00:00:00.0");

    // Validate selected values for row index 7
    validateRowValueByName(
        response, actualHeaders, 7, "A03MvHHogjR.ouname", "Mabenteh Community Hospital");
    validateRowValueByName(
        response, actualHeaders, 7, "A03MvHHogjR.eventdate", "2021-12-17 00:00:00.0");
  }

  @Test
  public void eventsSortedByStageEventDateAscending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=A03MvHHogjR.eventdate")
            .add("headers=A03MvHHogjR.ouname,A03MvHHogjR.eventdate")
            .add("stage=A03MvHHogjR")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("pageSize=50")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf")
            .add("eventDate=202112");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        8,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"202112\":{\"name\":\"December 2021\"},\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"eventdate\":{\"name\":\"Event date\"}},\"dimensions\":{\"pe\":[],\"eventdate\":[\"202112\"],\"ou\":[\"lY93YpCxJqf\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.ouname", "Tonkomba MCHP");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.eventdate", "2021-12-03 00:00:00.0");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "A03MvHHogjR.ouname", "Kunsho CHP");
    validateRowValueByName(
        response, actualHeaders, 3, "A03MvHHogjR.eventdate", "2021-12-12 00:00:00.0");

    // Validate selected values for row index 6
    validateRowValueByName(
        response, actualHeaders, 6, "A03MvHHogjR.ouname", "Fullah Town (M.Gbanti) MCHP");
    validateRowValueByName(
        response, actualHeaders, 6, "A03MvHHogjR.eventdate", "2021-12-23 00:00:00.0");

    // Validate selected values for row index 7
    validateRowValueByName(response, actualHeaders, 7, "A03MvHHogjR.ouname", "Mangay Loko MCHP");
    validateRowValueByName(
        response, actualHeaders, 7, "A03MvHHogjR.eventdate", "2021-12-28 00:00:00.0");
  }

  @Test
  public void eventsSortedByStageScheduledDateDescending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=A03MvHHogjR.ouname,A03MvHHogjR.scheduleddate")
            .add("stage=A03MvHHogjR")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("pageSize=50")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf")
            .add("eventDate=202112")
            .add("desc=A03MvHHogjR.scheduleddate");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        8,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"202112\":{\"name\":\"December 2021\"},\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"eventdate\":{\"name\":\"Event date\"}},\"dimensions\":{\"pe\":[],\"eventdate\":[\"202112\"],\"ou\":[\"lY93YpCxJqf\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.scheduleddate",
        "Scheduled date",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.ouname", "Mangay Loko MCHP");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.scheduleddate", "2021-12-28 12:05:00.0");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "A03MvHHogjR.ouname", "Mabayo MCHP");
    validateRowValueByName(
        response, actualHeaders, 3, "A03MvHHogjR.scheduleddate", "2021-12-13 12:05:00.0");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "A03MvHHogjR.ouname", "Yankasa MCHP");
    validateRowValueByName(
        response, actualHeaders, 6, "A03MvHHogjR.scheduleddate", "2021-12-05 12:05:00.0");

    // Validate selected values for row index 7
    validateRowValueByName(response, actualHeaders, 7, "A03MvHHogjR.ouname", "Tonkomba MCHP");
    validateRowValueByName(
        response, actualHeaders, 7, "A03MvHHogjR.scheduleddate", "2021-12-03 12:05:00.0");
  }

  @Test
  public void eventsSortedByStageEventDateTokenDescendingWithoutMatchingHeader()
      throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=A03MvHHogjR.ouname")
            .add("stage=A03MvHHogjR")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("pageSize=50")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf")
            .add("eventDate=202112")
            .add("desc=A03MvHHogjR.EVENT_DATE");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        8,
        1,
        1); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"202112\":{\"name\":\"December 2021\"},\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"eventdate\":{\"name\":\"Event date\"}},\"dimensions\":{\"pe\":[],\"eventdate\":[\"202112\"],\"ou\":[\"lY93YpCxJqf\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.ouname", "Mangay Loko MCHP");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "A03MvHHogjR.ouname", "Mabayo MCHP");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "A03MvHHogjR.ouname", "Yankasa MCHP");

    // Validate selected values for row index 7
    validateRowValueByName(response, actualHeaders, 7, "A03MvHHogjR.ouname", "Tonkomba MCHP");
  }

  @Test
  public void eventsSortedByStageOrgUnitTokenAscending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=A03MvHHogjR.ou")
            .add("headers=A03MvHHogjR.ouname,ou")
            .add("stage=A03MvHHogjR")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("pageSize=50")
            .add("page=1")
            .add("dimension=ou:BGGmAwx33dj")
            .add("eventDate=202105");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        6,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"BGGmAwx33dj\":{\"name\":\"Bumpe Ngao\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"202105\":{\"name\":\"May 2021\"},\"eventdate\":{\"name\":\"Event date\"}},\"dimensions\":{\"pe\":[],\"eventdate\":[\"202105\"],\"ou\":[\"BGGmAwx33dj\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "ou",
        "Organisation unit",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.ouname", "Buma MCHP");
    validateRowValueByName(response, actualHeaders, 0, "ou", "AXZq6q7Dr6E");

    // Validate selected values for row index 1
    validateRowValueByName(response, actualHeaders, 1, "A03MvHHogjR.ouname", "Kaniya MCHP");
    validateRowValueByName(response, actualHeaders, 1, "ou", "CTOMXJg41hz");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "A03MvHHogjR.ouname", "Yengema CHP");
    validateRowValueByName(response, actualHeaders, 2, "ou", "EFTcruJcNmZ");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "A03MvHHogjR.ouname", "Taninahun (BN) CHP");
    validateRowValueByName(response, actualHeaders, 3, "ou", "kEkU53NrFmy");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "A03MvHHogjR.ouname", "Wallehun MCHP");
    validateRowValueByName(response, actualHeaders, 4, "ou", "tZxqVn3xNrA");

    // Validate selected values for row index 5
    validateRowValueByName(response, actualHeaders, 5, "A03MvHHogjR.ouname", "Mokoba MCHP");
    validateRowValueByName(response, actualHeaders, 5, "ou", "xt08cuqf1ys");
  }
}
