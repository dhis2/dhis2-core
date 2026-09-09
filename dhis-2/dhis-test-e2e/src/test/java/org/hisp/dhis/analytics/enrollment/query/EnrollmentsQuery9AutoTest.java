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
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class EnrollmentsQuery9AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void enrollmentsSortedByStageOrgUnitNameAscending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=A03MvHHogjR.ouname")
            .add("headers=ouname,A03MvHHogjR.ouname,A03MvHHogjR.eventdate")
            .add("enrollmentDate=202212")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("pageSize=50")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf");

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
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"202212\":{\"name\":\"December 2022\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"enrollmentdate\":{\"name\":\"Date of enrollment\"}},\"dimensions\":{\"A03MvHHogjR.ou\":[],\"enrollmentdate\":[\"202212\"],\"A03MvHHogjR.eventdate\":[],\"pe\":[],\"ou\":[\"lY93YpCxJqf\"]}}";
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
        "DATE",
        "java.time.LocalDate",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Fullah Town (M.Gbanti) MCHP");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.eventdate", "2021-12-23 00:00:00.0");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Mabenteh Community Hospital");
    validateRowValueByName(
        response, actualHeaders, 3, "A03MvHHogjR.eventdate", "2021-12-17 00:00:00.0");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "ouname", "Tonkomba MCHP");
    validateRowValueByName(
        response, actualHeaders, 6, "A03MvHHogjR.eventdate", "2021-12-03 00:00:00.0");

    // Validate selected values for row index 7
    validateRowValueByName(response, actualHeaders, 7, "ouname", "Yankasa MCHP");
    validateRowValueByName(
        response, actualHeaders, 7, "A03MvHHogjR.eventdate", "2021-12-05 00:00:00.0");
  }

  @Test
  public void enrollmentsSortedByStageOrgUnitCodeDescendingWithoutMatchingHeader()
      throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=ouname,A03MvHHogjR.ouname")
            .add("enrollmentDate=202212")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("pageSize=50")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf")
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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"202212\":{\"name\":\"December 2022\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"enrollmentdate\":{\"name\":\"Date of enrollment\"}},\"dimensions\":{\"A03MvHHogjR.ou\":[],\"enrollmentdate\":[\"202212\"],\"pe\":[],\"ou\":[\"lY93YpCxJqf\"]}}";
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
        "A03MvHHogjR.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mangay Loko MCHP");
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.ouname", "Mangay Loko MCHP");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Panlap MCHP");
    validateRowValueByName(response, actualHeaders, 3, "A03MvHHogjR.ouname", "Panlap MCHP");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "ouname", "Fullah Town (M.Gbanti) MCHP");
    validateRowValueByName(
        response, actualHeaders, 6, "A03MvHHogjR.ouname", "Fullah Town (M.Gbanti) MCHP");

    // Validate selected values for row index 7
    validateRowValueByName(response, actualHeaders, 7, "ouname", "Mabenteh Community Hospital");
    validateRowValueByName(
        response, actualHeaders, 7, "A03MvHHogjR.ouname", "Mabenteh Community Hospital");
  }

  @Test
  public void enrollmentsSortedByStageEventDateDescending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=ouname,incidentdate,A03MvHHogjR.eventdate")
            .add("enrollmentDate=202212")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("pageSize=50")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf")
            .add("desc=A03MvHHogjR.eventdate");

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
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"202212\":{\"name\":\"December 2022\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"enrollmentdate\":{\"name\":\"Date of enrollment\"}},\"dimensions\":{\"enrollmentdate\":[\"202212\"],\"A03MvHHogjR.eventdate\":[],\"pe\":[],\"ou\":[\"lY93YpCxJqf\"]}}";
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
        "incidentdate",
        "Date of birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mangay Loko MCHP");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.eventdate", "2021-12-28 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2022-12-28 12:05:00.0");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Mabayo MCHP");
    validateRowValueByName(
        response, actualHeaders, 3, "A03MvHHogjR.eventdate", "2021-12-13 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 3, "incidentdate", "2022-12-13 12:05:00.0");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "ouname", "Yankasa MCHP");
    validateRowValueByName(
        response, actualHeaders, 6, "A03MvHHogjR.eventdate", "2021-12-05 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 6, "incidentdate", "2022-12-05 12:05:00.0");

    // Validate selected values for row index 7
    validateRowValueByName(response, actualHeaders, 7, "ouname", "Tonkomba MCHP");
    validateRowValueByName(
        response, actualHeaders, 7, "A03MvHHogjR.eventdate", "2021-12-03 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 7, "incidentdate", "2022-12-03 12:05:00.0");
  }

  @Test
  public void enrollmentsSortedByStageScheduledDateAscendingWithoutMatchingHeader()
      throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=A03MvHHogjR.scheduleddate")
            .add("headers=ouname,incidentdate")
            .add("enrollmentDate=202212")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("pageSize=50")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf");

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"202212\":{\"name\":\"December 2022\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"enrollmentdate\":{\"name\":\"Date of enrollment\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"lY93YpCxJqf\"],\"enrollmentdate\":[\"202212\"]}}";
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
        "incidentdate",
        "Date of birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Tonkomba MCHP");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2022-12-03 12:05:00.0");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Kunsho CHP");
    validateRowValueByName(response, actualHeaders, 3, "incidentdate", "2022-12-12 12:05:00.0");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "ouname", "Fullah Town (M.Gbanti) MCHP");
    validateRowValueByName(response, actualHeaders, 6, "incidentdate", "2022-12-23 12:05:00.0");

    // Validate selected values for row index 7
    validateRowValueByName(response, actualHeaders, 7, "ouname", "Mangay Loko MCHP");
    validateRowValueByName(response, actualHeaders, 7, "incidentdate", "2022-12-28 12:05:00.0");
  }

  @Test
  public void enrollmentsSortedByStageEventDateTokenAscending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=A03MvHHogjR.EVENT_DATE")
            .add("headers=ouname,incidentdate,A03MvHHogjR.eventdate")
            .add("enrollmentDate=202212")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("pageSize=50")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf");

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
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"202212\":{\"name\":\"December 2022\"},\"A03MvHHogjR.eventdate\":{\"name\":\"Report date\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"enrollmentdate\":{\"name\":\"Date of enrollment\"}},\"dimensions\":{\"enrollmentdate\":[\"202212\"],\"A03MvHHogjR.eventdate\":[],\"pe\":[],\"ou\":[\"lY93YpCxJqf\"]}}";
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
        "incidentdate",
        "Date of birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Report date",
        "DATE",
        "java.time.LocalDate",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Tonkomba MCHP");
    validateRowValueByName(
        response, actualHeaders, 0, "A03MvHHogjR.eventdate", "2021-12-03 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2022-12-03 12:05:00.0");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Kunsho CHP");
    validateRowValueByName(
        response, actualHeaders, 3, "A03MvHHogjR.eventdate", "2021-12-12 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 3, "incidentdate", "2022-12-12 12:05:00.0");

    // Validate selected values for row index 6
    validateRowValueByName(response, actualHeaders, 6, "ouname", "Fullah Town (M.Gbanti) MCHP");
    validateRowValueByName(
        response, actualHeaders, 6, "A03MvHHogjR.eventdate", "2021-12-23 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 6, "incidentdate", "2022-12-23 12:05:00.0");

    // Validate selected values for row index 7
    validateRowValueByName(response, actualHeaders, 7, "ouname", "Mangay Loko MCHP");
    validateRowValueByName(
        response, actualHeaders, 7, "A03MvHHogjR.eventdate", "2021-12-28 00:00:00.0");
    validateRowValueByName(response, actualHeaders, 7, "incidentdate", "2022-12-28 12:05:00.0");
  }

  @Test
  public void enrollmentsSortedByStageOrgUnitTokenDescending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=ouname,A03MvHHogjR.ouname")
            .add("enrollmentDate=202205")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("pageSize=50")
            .add("page=1")
            .add("dimension=ou:BGGmAwx33dj")
            .add("desc=A03MvHHogjR.ou");

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"BGGmAwx33dj\":{\"name\":\"Bumpe Ngao\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"enrollmentdate\":{\"name\":\"Date of enrollment\"},\"202205\":{\"name\":\"May 2022\"}},\"dimensions\":{\"A03MvHHogjR.ou\":[],\"enrollmentdate\":[\"202205\"],\"pe\":[],\"ou\":[\"BGGmAwx33dj\"]}}";
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
        "A03MvHHogjR.ouname",
        "Organisation unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mokoba MCHP");
    validateRowValueByName(response, actualHeaders, 0, "A03MvHHogjR.ouname", "Mokoba MCHP");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Taninahun (BN) CHP");
    validateRowValueByName(response, actualHeaders, 2, "A03MvHHogjR.ouname", "Taninahun (BN) CHP");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "ouname", "Kaniya MCHP");
    validateRowValueByName(response, actualHeaders, 4, "A03MvHHogjR.ouname", "Kaniya MCHP");

    // Validate selected values for row index 5
    validateRowValueByName(response, actualHeaders, 5, "ouname", "Buma MCHP");
    validateRowValueByName(response, actualHeaders, 5, "A03MvHHogjR.ouname", "Buma MCHP");
  }
}
