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
public class TrackedEntityAggregate8AutoTest extends AnalyticsApiTest {
  private final AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  @Test
  public void aggregateCountByStageOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add(
                "dimension=A03MvHHogjR.ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        5,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit, Child Programme, Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"IpHINAT79UW.A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"QII5GqfDfO3\",\"DiszpKrYNg8\",\"QZzRkqdGjlm\",\"Qr41Mw2MSjo\",\"VpYAl8dXs6m\"],\"A03MvHHogjR.ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ou",
        "Organisation unit, Child Programme, Birth",
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
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "DiszpKrYNg8", "value", "32"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "Qr41Mw2MSjo", "value", "30"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "VpYAl8dXs6m", "value", "30"));
  }

  @Test
  public void aggregateCountByFullyQualifiedStageOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=IpHINAT79UW.A03MvHHogjR.ou:QII5GqfDfO3;DiszpKrYNg8");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"IpHINAT79UW.A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{\"name\":\"Organisation unit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit, Child Programme, Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"QII5GqfDfO3\",\"DiszpKrYNg8\"],\"A03MvHHogjR.ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ou",
        "Organisation unit, Child Programme, Birth",
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
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "DiszpKrYNg8", "value", "32"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "QII5GqfDfO3", "value", "33"));
  }

  @Test
  public void aggregateCountByStageOrgUnitWithStageOffset() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add(
                "dimension=A03MvHHogjR[1].ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        5,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit, Child Programme (1), Birth (1)\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"IpHINAT79UW.A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"QII5GqfDfO3\",\"DiszpKrYNg8\",\"QZzRkqdGjlm\",\"Qr41Mw2MSjo\",\"VpYAl8dXs6m\"],\"A03MvHHogjR.ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ou",
        "Organisation unit, Child Programme (1), Birth (1)",
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
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "DiszpKrYNg8", "value", "32"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "Qr41Mw2MSjo", "value", "30"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "VpYAl8dXs6m", "value", "30"));
  }

  @Test
  public void aggregateCountByStageEventDateIsoYear() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=A03MvHHogjR.EVENT_DATE:2021");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"A03MvHHogjR.eventdate\":{\"name\":\"Event Date, Child Programme, Birth\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"A03MvHHogjR.eventdate\":[],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Event Date, Child Programme, Birth",
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
        response, actualHeaders, Map.of("A03MvHHogjR.eventdate", "2021", "value", "11015"));
  }

  @Test
  public void aggregateCountByStageEventDateIsoMonths() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=A03MvHHogjR.EVENT_DATE:202107;202108");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"A03MvHHogjR.eventdate\":{\"name\":\"Event Date, Child Programme, Birth\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"A03MvHHogjR.eventdate\":[],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Event Date, Child Programme, Birth",
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
        response, actualHeaders, Map.of("A03MvHHogjR.eventdate", "202107", "value", "950"));
  }

  @Test
  public void aggregateCountByStageEventDateRelativePeriod() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=A03MvHHogjR.EVENT_DATE:LAST_5_YEARS")
            .add("relativePeriodDate=2022-07-01");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"A03MvHHogjR.eventdate\":{\"name\":\"Event Date, Child Programme, Birth\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"A03MvHHogjR.eventdate\":[],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Event Date, Child Programme, Birth",
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
        response, actualHeaders, Map.of("A03MvHHogjR.eventdate", "2021", "value", "11015"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.eventdate", "2022", "value", "8004"));
  }

  @Test
  public void aggregateCountByStageScheduledDateIsoYear() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=A03MvHHogjR.SCHEDULED_DATE:2021");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR.scheduleddate\":{\"name\":\"Scheduled Date, Child Programme, Birth\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[],\"A03MvHHogjR.scheduleddate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.scheduleddate",
        "Scheduled Date, Child Programme, Birth",
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
        response, actualHeaders, Map.of("A03MvHHogjR.scheduleddate", "2021", "value", "11007"));
  }

  @Test
  public void aggregateCountByStageEventStatus() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=A03MvHHogjR.EVENT_STATUS");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        3,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"A03MvHHogjR.eventstatus\":{\"name\":\"Event Status, Child Programme, Birth\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"A03MvHHogjR.eventstatus\":[],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventstatus",
        "Event Status, Child Programme, Birth",
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
        response, actualHeaders, Map.of("A03MvHHogjR.eventstatus", "ACTIVE", "value", "19002"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.eventstatus", "", "value", "54053"));
  }

  @Test
  public void aggregateCountByStageEventStatusActiveAndCompleted() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=A03MvHHogjR.EVENT_STATUS:ACTIVE;COMPLETED");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"A03MvHHogjR.eventstatus\":{\"name\":\"Event Status, Child Programme, Birth\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"A03MvHHogjR.eventstatus\":[],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventstatus",
        "Event Status, Child Programme, Birth",
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
        response, actualHeaders, Map.of("A03MvHHogjR.eventstatus", "ACTIVE", "value", "19002"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.eventstatus", "COMPLETED", "value", "17"));
  }

  @Test
  public void aggregateCountByStageDataElementValue() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=A03MvHHogjR.ou:QII5GqfDfO3")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("dimension=A03MvHHogjR.UXz7xuGCEhU");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        33,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"}},\"dimensions\":{\"pe\":[],\"A03MvHHogjR.UXz7xuGCEhU\":[],\"UXz7xuGCEhU\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.UXz7xuGCEhU",
        "MCH Weight (g), Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "2538", "value", "1"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "2843", "value", "1"));

    // Validate row exists with values from original row index 12
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "3085", "value", "1"));

    // Validate row exists with values from original row index 18
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "3312", "value", "1"));

    // Validate row exists with values from original row index 24
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "3638", "value", "1"));

    // Validate row exists with values from original row index 30
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "3822", "value", "1"));

    // Validate row exists with values from original row index 32
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "3901", "value", "1"));
  }

  @Test
  public void aggregateCountByStageOrgUnitAndEventStatus() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add(
                "dimension=A03MvHHogjR.ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m,A03MvHHogjR.EVENT_STATUS");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit, Child Programme, Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"IpHINAT79UW.A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR.eventstatus\":{\"name\":\"Event Status, Child Programme, Birth\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"A03MvHHogjR.eventstatus\":[],\"pe\":[],\"ou\":[\"QII5GqfDfO3\",\"DiszpKrYNg8\",\"QZzRkqdGjlm\",\"Qr41Mw2MSjo\",\"VpYAl8dXs6m\"],\"A03MvHHogjR.ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ou",
        "Organisation unit, Child Programme, Birth",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventstatus",
        "Event Status, Child Programme, Birth",
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
        Map.of(
            "A03MvHHogjR.ou", "DiszpKrYNg8", "A03MvHHogjR.eventstatus", "ACTIVE", "value", "15"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "A03MvHHogjR.ou", "QII5GqfDfO3", "A03MvHHogjR.eventstatus", "ACTIVE", "value", "33"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "A03MvHHogjR.ou", "QZzRkqdGjlm", "A03MvHHogjR.eventstatus", "ACTIVE", "value", "32"));

    // Validate row exists with values from original row index 5
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "A03MvHHogjR.ou", "VpYAl8dXs6m", "A03MvHHogjR.eventstatus", "ACTIVE", "value", "30"));
  }

  @Test
  public void aggregateCountByRegistrationAndStageOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add(
                "dimension=ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m,A03MvHHogjR.ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        5,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit, Child Programme, Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"IpHINAT79UW.A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"],\"A03MvHHogjR.ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"]}}";
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
        "A03MvHHogjR.ou",
        "Organisation unit, Child Programme, Birth",
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
        Map.of("ou", "DiszpKrYNg8", "A03MvHHogjR.ou", "DiszpKrYNg8", "value", "32"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response,
        actualHeaders,
        Map.of("ou", "Qr41Mw2MSjo", "A03MvHHogjR.ou", "Qr41Mw2MSjo", "value", "30"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response,
        actualHeaders,
        Map.of("ou", "VpYAl8dXs6m", "A03MvHHogjR.ou", "VpYAl8dXs6m", "value", "30"));
  }

  @Test
  public void aggregateAverageBirthWeightByStageOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("aggregationType=AVERAGE")
            .add("totalPages=false")
            .add("pageSize=50")
            .add(
                "dimension=A03MvHHogjR.ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m")
            .add("value=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        5,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit, Child Programme, Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"IpHINAT79UW.A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"QII5GqfDfO3\",\"DiszpKrYNg8\",\"QZzRkqdGjlm\",\"Qr41Mw2MSjo\",\"VpYAl8dXs6m\"],\"A03MvHHogjR.ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ou",
        "Organisation unit, Child Programme, Birth",
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
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "DiszpKrYNg8", "value", "4385.19"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "Qr41Mw2MSjo", "value", "3281.07"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "VpYAl8dXs6m", "value", "3220.1"));
  }

  @Test
  public void aggregateCountByStageOrgUnitAndTrackedEntityAttribute() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add(
                "dimension=A03MvHHogjR.ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m,cejWyOfXge6");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        10,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit, Child Programme, Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"cejWyOfXge6\":{\"name\":\"Gender\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"IpHINAT79UW.A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\"},{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"QII5GqfDfO3\",\"DiszpKrYNg8\",\"QZzRkqdGjlm\",\"Qr41Mw2MSjo\",\"VpYAl8dXs6m\"],\"A03MvHHogjR.ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ou",
        "Organisation unit, Child Programme, Birth",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "DiszpKrYNg8", "cejWyOfXge6", "Female", "value", "17"));

    // Validate row exists with values from original row index 3
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "QZzRkqdGjlm", "cejWyOfXge6", "Female", "value", "19"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "QII5GqfDfO3", "cejWyOfXge6", "Male", "value", "15"));

    // Validate row exists with values from original row index 9
    validateRowExists(
        response,
        actualHeaders,
        Map.of("A03MvHHogjR.ou", "VpYAl8dXs6m", "cejWyOfXge6", "Male", "value", "14"));
  }

  @Test
  public void aggregateCountByEnrollmentOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add(
                "dimension=IpHINAT79UW.ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        5,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"IpHINAT79UW.ou\":{\"name\":\"Organisation unit\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"IpHINAT79UW.ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"],\"pe\":[],\"ou\":[\"QII5GqfDfO3\",\"DiszpKrYNg8\",\"QZzRkqdGjlm\",\"Qr41Mw2MSjo\",\"VpYAl8dXs6m\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "IpHINAT79UW.ou",
        "Organisation unit, Child Programme",
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
        response, actualHeaders, Map.of("IpHINAT79UW.ou", "DiszpKrYNg8", "value", "36"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response, actualHeaders, Map.of("IpHINAT79UW.ou", "Qr41Mw2MSjo", "value", "30"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response, actualHeaders, Map.of("IpHINAT79UW.ou", "VpYAl8dXs6m", "value", "30"));
  }

  @Test
  public void aggregateCountByEnrollmentOuKeywordScopedToProgram() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add(
                "dimension=IpHINAT79UW.ENROLLMENT_OU:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        5,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"IpHINAT79UW.ou\":{\"name\":\"Organisation unit\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"IpHINAT79UW.ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"],\"pe\":[],\"ou\":[\"QII5GqfDfO3\",\"DiszpKrYNg8\",\"QZzRkqdGjlm\",\"Qr41Mw2MSjo\",\"VpYAl8dXs6m\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "IpHINAT79UW.ou",
        "Organisation unit, Child Programme",
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
        response, actualHeaders, Map.of("IpHINAT79UW.ou", "DiszpKrYNg8", "value", "36"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response, actualHeaders, Map.of("IpHINAT79UW.ou", "Qr41Mw2MSjo", "value", "30"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response, actualHeaders, Map.of("IpHINAT79UW.ou", "VpYAl8dXs6m", "value", "30"));
  }

  @Test
  public void aggregateCountByEnrollmentOrgUnitName() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "filter=IpHINAT79UW.ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m")
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=IpHINAT79UW.ouname");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        5,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"IpHINAT79UW.ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"}},\"dimensions\":{\"pe\":[],\"IpHINAT79UW.ouname\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "IpHINAT79UW.ouname",
        "Organisation Unit Name, Child Programme",
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
        Map.of("IpHINAT79UW.ouname", "Bendoma (Malegohun) MCHP", "value", "30"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response, actualHeaders, Map.of("IpHINAT79UW.ouname", "Ngelehun CHC", "value", "36"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response, actualHeaders, Map.of("IpHINAT79UW.ouname", "Senthai MCHP", "value", "30"));
  }

  @Test
  public void aggregateCountByEnrollmentDateIsoYear() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=IpHINAT79UW.ENROLLMENT_DATE:2022");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"IpHINAT79UW.enrollmentdate\":{\"name\":\"Date of enrollment\",\"dimensionType\":\"PERIOD\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"}},\"dimensions\":{\"pe\":[],\"IpHINAT79UW.enrollmentdate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "IpHINAT79UW.enrollmentdate",
        "Date of enrollment, Child Programme",
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
        response, actualHeaders, Map.of("IpHINAT79UW.enrollmentdate", "2022", "value", "11024"));
  }

  @Test
  public void aggregateCountByEnrollmentIncidentDateIsoYear() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=IpHINAT79UW.INCIDENT_DATE:2022");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"IpHINAT79UW.incidentdate\":{\"name\":\"Date of birth\",\"dimensionType\":\"PERIOD\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"}},\"dimensions\":{\"pe\":[],\"IpHINAT79UW.incidentdate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "IpHINAT79UW.incidentdate",
        "Date of birth, Child Programme",
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
        response, actualHeaders, Map.of("IpHINAT79UW.incidentdate", "2022", "value", "11023"));
  }

  @Test
  public void aggregateCountByProgramStatus() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=IpHINAT79UW.PROGRAM_STATUS");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        3,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"IpHINAT79UW.programstatus\":{\"name\":\"Program Status, Child Programme\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"}},\"dimensions\":{\"pe\":[],\"IpHINAT79UW.programstatus\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "IpHINAT79UW.programstatus",
        "Program Status, Child Programme",
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
        response, actualHeaders, Map.of("IpHINAT79UW.programstatus", "ACTIVE", "value", "19025"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response, actualHeaders, Map.of("IpHINAT79UW.programstatus", "", "value", "54043"));
  }

  @Test
  public void aggregateCountByEnrollmentStatusActive() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=IpHINAT79UW.ENROLLMENT_STATUS:ACTIVE");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"IpHINAT79UW.enrollmentstatus\":{\"name\":\"Enrollment Status, Child Programme\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"}},\"dimensions\":{\"IpHINAT79UW.enrollmentstatus\":[],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "IpHINAT79UW.enrollmentstatus",
        "Enrollment Status, Child Programme",
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
        Map.of("IpHINAT79UW.enrollmentstatus", "ACTIVE", "value", "19025"));
  }

  @Test
  public void aggregateCountByEnrollmentOrgUnitAndProgramStatus() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add(
                "dimension=IpHINAT79UW.ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m,IpHINAT79UW.PROGRAM_STATUS");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"IpHINAT79UW.ou\":{\"name\":\"Organisation unit\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"IpHINAT79UW.programstatus\":{\"name\":\"Program Status, Child Programme\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"IpHINAT79UW.ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"],\"pe\":[],\"ou\":[\"QII5GqfDfO3\",\"DiszpKrYNg8\",\"QZzRkqdGjlm\",\"Qr41Mw2MSjo\",\"VpYAl8dXs6m\"],\"IpHINAT79UW.programstatus\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "IpHINAT79UW.ou",
        "Organisation unit, Child Programme",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "IpHINAT79UW.programstatus",
        "Program Status, Child Programme",
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
        Map.of(
            "IpHINAT79UW.ou", "DiszpKrYNg8", "IpHINAT79UW.programstatus", "ACTIVE", "value", "32"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "IpHINAT79UW.ou", "QII5GqfDfO3", "IpHINAT79UW.programstatus", "ACTIVE", "value", "33"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "IpHINAT79UW.ou", "QZzRkqdGjlm", "IpHINAT79UW.programstatus", "ACTIVE", "value", "32"));

    // Validate row exists with values from original row index 5
    validateRowExists(
        response,
        actualHeaders,
        Map.of(
            "IpHINAT79UW.ou", "VpYAl8dXs6m", "IpHINAT79UW.programstatus", "ACTIVE", "value", "30"));
  }

  @Test
  public void aggregateCountByEnrollmentOrgUnitSortedAscending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=IpHINAT79UW.ou")
            .add("totalPages=false")
            .add("pageSize=50")
            .add(
                "dimension=IpHINAT79UW.ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        5,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"IpHINAT79UW.ou\":{\"name\":\"Organisation unit\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"IpHINAT79UW.ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"],\"pe\":[],\"ou\":[\"QII5GqfDfO3\",\"DiszpKrYNg8\",\"QZzRkqdGjlm\",\"Qr41Mw2MSjo\",\"VpYAl8dXs6m\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "IpHINAT79UW.ou",
        "Organisation unit, Child Programme",
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
        response, actualHeaders, Map.of("IpHINAT79UW.ou", "DiszpKrYNg8", "value", "36"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response, actualHeaders, Map.of("IpHINAT79UW.ou", "Qr41Mw2MSjo", "value", "30"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response, actualHeaders, Map.of("IpHINAT79UW.ou", "VpYAl8dXs6m", "value", "30"));
  }

  @Test
  public void aggregateCountByStageOrgUnitSortedAscending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=A03MvHHogjR.ou")
            .add("totalPages=false")
            .add("pageSize=50")
            .add(
                "dimension=A03MvHHogjR.ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        5,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR.ou\":{\"name\":\"Organisation unit, Child Programme, Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"IpHINAT79UW.A03MvHHogjR.ou\":{\"name\":\"Organisation unit\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"QII5GqfDfO3\",\"DiszpKrYNg8\",\"QZzRkqdGjlm\",\"Qr41Mw2MSjo\",\"VpYAl8dXs6m\"],\"A03MvHHogjR.ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.ou",
        "Organisation unit, Child Programme, Birth",
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
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "DiszpKrYNg8", "value", "32"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "Qr41Mw2MSjo", "value", "30"));

    // Validate row exists with values from original row index 4
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.ou", "VpYAl8dXs6m", "value", "30"));
  }

  @Test
  public void aggregateCountByStageEventDateSortedDescending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=A03MvHHogjR.EVENT_DATE:202107;202108")
            .add("desc=A03MvHHogjR.EVENT_DATE");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"A03MvHHogjR.eventdate\":{\"name\":\"Event Date, Child Programme, Birth\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"A03MvHHogjR.eventdate\":[],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventdate",
        "Event Date, Child Programme, Birth",
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
        response, actualHeaders, Map.of("A03MvHHogjR.eventdate", "202107", "value", "950"));
  }

  @Test
  public void aggregateCountByStageEventStatusSortedAscending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=A03MvHHogjR.EVENT_STATUS")
            .add("totalPages=false")
            .add("pageSize=50")
            .add("dimension=A03MvHHogjR.EVENT_STATUS");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        3,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"A03MvHHogjR.eventstatus\":{\"name\":\"Event Status, Child Programme, Birth\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"A03MvHHogjR.eventstatus\":[],\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.eventstatus",
        "Event Status, Child Programme, Birth",
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
        response, actualHeaders, Map.of("A03MvHHogjR.eventstatus", "ACTIVE", "value", "19002"));

    // Validate row exists with values from original row index 2
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.eventstatus", "", "value", "54053"));
  }

  @Test
  public void aggregateCountByStageDataElementSortedDescending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=A03MvHHogjR.ou:QII5GqfDfO3")
            .add("totalPages=false")
            .add("pageSize=100")
            .add("dimension=A03MvHHogjR.UXz7xuGCEhU")
            .add("desc=A03MvHHogjR.UXz7xuGCEhU");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        33,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"}},\"dimensions\":{\"pe\":[],\"A03MvHHogjR.UXz7xuGCEhU\":[],\"UXz7xuGCEhU\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "A03MvHHogjR.UXz7xuGCEhU",
        "MCH Weight (g), Child Programme, Birth",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "3901", "value", "1"));

    // Validate row exists with values from original row index 6
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "3716", "value", "1"));

    // Validate row exists with values from original row index 12
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "3336", "value", "1"));

    // Validate row exists with values from original row index 18
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "3138", "value", "1"));

    // Validate row exists with values from original row index 24
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "3023", "value", "1"));

    // Validate row exists with values from original row index 30
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "2560", "value", "1"));

    // Validate row exists with values from original row index 32
    validateRowExists(
        response, actualHeaders, Map.of("A03MvHHogjR.UXz7xuGCEhU", "2538", "value", "1"));
  }

  @Test
  public void aggregateScopedLatestMissingValueAverage() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("aggregationType=AVERAGE")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.ou")
            .add("value=r21967Prg01.r21967Stg01.r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[\"DiszpKrYNg8\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression, Grouped visits",
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
        response, actualHeaders, Map.of("r21967Stg01.ou", "DiszpKrYNg8", "value", ""));
  }

  @Test
  public void aggregateScopedLatestMissingValueCount() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("aggregationType=COUNT")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.ou")
            .add("value=r21967Prg01.r21967Stg01.r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[\"DiszpKrYNg8\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression, Grouped visits",
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
        response, actualHeaders, Map.of("r21967Stg01.ou", "DiszpKrYNg8", "value", "0"));
  }

  @Test
  public void aggregateScopedMissingGroupedDataElement() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Prg01.r21967Stg01.r21967De001\":{\"name\":\"TE aggregate regression weight\"},\"r21967De001\":{\"name\":\"TE aggregate regression weight\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"},\"r21967Stg01.r21967De001\":{\"name\":\"TE aggregate regression weight\"}},\"dimensions\":{\"r21967De001\":[],\"pe\":[],\"r21967Stg01.r21967De001\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.r21967De001",
        "TE aggregate regression weight, TE aggregate scoped regression, Grouped visits",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("r21967Stg01.r21967De001", "", "value", "1"));
  }

  @Test
  public void aggregateScopedYearWithIndependentDateFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=r21967Stg01.EVENT_DATE:GE:2021-07-01")
            .add("asc=r21967Stg01.EVENT_DATE")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.EVENT_DATE:2021");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Stg01.eventdate\":{\"name\":\"Event Date, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.eventdate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.eventdate",
        "Event Date, TE aggregate scoped regression, Grouped visits",
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
        response, actualHeaders, Map.of("r21967Stg01.eventdate", "2021", "value", "3"));
  }

  @Test
  public void aggregateScopedComparisonKeepsRawTimestamps() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.EVENT_DATE:GE:2021-07-01;LT:2021-07-02");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

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
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Stg01.eventdate\":{\"name\":\"Event Date, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.eventdate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.eventdate",
        "Event Date, TE aggregate scoped regression, Grouped visits",
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
        Map.of("r21967Stg01.eventdate", "2021-07-01 10:00:00.0", "value", "1"));

    // Validate row exists with values from original row index 1
    validateRowExists(
        response,
        actualHeaders,
        Map.of("r21967Stg01.eventdate", "2021-07-01 18:00:00.0", "value", "1"));
  }

  @Test
  public void aggregateScopedLatestEventAcrossEnrollments() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01[0].ou");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[\"DiszpKrYNg8\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression, Grouped visits",
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
        response, actualHeaders, Map.of("r21967Stg01.ou", "DiszpKrYNg8", "value", "1"));
  }

  @Test
  public void aggregateScopedPreviousEventAcrossEnrollments() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01[-1].ou");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression (-1), Grouped visits (-1)\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[\"QII5GqfDfO3\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression (-1), Grouped visits (-1)",
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
        response, actualHeaders, Map.of("r21967Stg01.ou", "QII5GqfDfO3", "value", "1"));
  }

  @Test
  public void aggregateScopedFirstEventAcrossEnrollments() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01[1].ou");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression (1), Grouped visits (1)\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[\"QII5GqfDfO3\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression (1), Grouped visits (1)",
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
        response, actualHeaders, Map.of("r21967Stg01.ou", "QII5GqfDfO3", "value", "1"));
  }

  @Test
  public void aggregateScopedPreviousEventWithMatchingValue() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("aggregationType=AVERAGE")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01[-1].ou")
            .add("value=r21967Prg01.r21967Stg01[-1].r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression (-1), Grouped visits (-1)\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[\"QII5GqfDfO3\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression (-1), Grouped visits (-1)",
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
        response, actualHeaders, Map.of("r21967Stg01.ou", "QII5GqfDfO3", "value", "10"));
  }

  @Test
  public void aggregateScopedFirstEventWithMatchingValue() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("aggregationType=AVERAGE")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01[1].ou")
            .add("value=r21967Prg01.r21967Stg01[1].r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression (1), Grouped visits (1)\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[\"QII5GqfDfO3\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression (1), Grouped visits (1)",
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
        response, actualHeaders, Map.of("r21967Stg01.ou", "QII5GqfDfO3", "value", "10"));
  }

  @Test
  public void aggregateScopedMatchingOrgUnitFilterAndValue() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001,r21967Stg01.ou:DiszpKrYNg8")
            .add("aggregationType=AVERAGE")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.ou")
            .add("value=r21967Prg01.r21967Stg01.r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[\"DiszpKrYNg8\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression, Grouped visits",
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
        response, actualHeaders, Map.of("r21967Stg01.ou", "DiszpKrYNg8", "value", ""));
  }

  @Test
  public void aggregateScopedOrgUnitFilterExcludesOtherSelectedEvent() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=r21967Stg01.ou:QII5GqfDfO3")
            .add("aggregationType=AVERAGE")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.ou,r21967Stg01.EVENT_DATE:2021")
            .add("value=r21967Prg01.r21967Stg01.r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression, Grouped visits\"},\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"r21967Stg01.eventdate\":{\"name\":\"Event Date, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[\"QII5GqfDfO3\"],\"r21967Stg01.eventdate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression, Grouped visits",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.eventdate",
        "Event Date, TE aggregate scoped regression, Grouped visits",
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
        Map.of("r21967Stg01.ou", "QII5GqfDfO3", "r21967Stg01.eventdate", "2021", "value", "30"));
  }

  @Test
  public void aggregateScopedDifferentValueStage() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("aggregationType=AVERAGE")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.ou")
            .add("value=r21967Prg01.r21967Stg02.r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[\"DiszpKrYNg8\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression, Grouped visits",
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
        response, actualHeaders, Map.of("r21967Stg01.ou", "DiszpKrYNg8", "value", "99"));
  }

  @Test
  public void aggregateScopedDifferentValueOffset() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("aggregationType=AVERAGE")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.ou")
            .add("value=r21967Prg01.r21967Stg01[1].r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[\"DiszpKrYNg8\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression, Grouped visits",
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
        response, actualHeaders, Map.of("r21967Stg01.ou", "DiszpKrYNg8", "value", "10"));
  }

  @Test
  public void aggregateScopedDateBucketAndFullOrgUnitAliasWithValue() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("aggregationType=AVERAGE")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.EVENT_DATE:2021,r21967Prg01.r21967Stg01.ou")
            .add("value=r21967Prg01.r21967Stg01.r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        3,
        3); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression, Grouped visits\"},\"r21967Stg01.eventdate\":{\"name\":\"Event Date, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[\"DiszpKrYNg8\"],\"r21967Stg01.eventdate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.eventdate",
        "Event Date, TE aggregate scoped regression, Grouped visits",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression, Grouped visits",
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
        Map.of("r21967Stg01.eventdate", "2021", "r21967Stg01.ou", "DiszpKrYNg8", "value", ""));
  }

  @Test
  public void aggregateScopedValueOnlySelectionCompatibility() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("aggregationType=AVERAGE")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("value=r21967Prg01.r21967Stg01.r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        1,
        1); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Stg02\":{\"name\":\"Independent visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("value", "10"));
  }

  @Test
  public void aggregateScopedMissingEventNullGroup() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te006")
            .add("aggregationType=COUNT")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.ou")
            .add("value=r21967Prg01.r21967Stg01.r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression, Grouped visits\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression, Grouped visits",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("r21967Stg01.ou", "", "value", "0"));
  }

  @Test
  public void aggregateScopedBeyondAvailableEventOffset() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("aggregationType=COUNT")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01[-5].ou")
            .add("value=r21967Prg01.r21967Stg01[-5].r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression (-5), Grouped visits (-5)\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.ou\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression (-5), Grouped visits (-5)",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("r21967Stg01.ou", "", "value", "0"));
  }

  @Test
  public void aggregateScopedSelectedEventCoordinatesAndMissingValue() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te001")
            .add("aggregationType=AVERAGE")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.ou,r21967Stg01.EVENT_STATUS,r21967Stg01.r21967De001")
            .add("value=r21967Prg01.r21967Stg01.r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        4,
        4); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"r21967Stg01.eventstatus\":{\"name\":\"Event Status, TE aggregate scoped regression, Grouped visits\"},\"r21967De001\":{\"name\":\"TE aggregate regression weight\"},\"r21967Prg01.r21967Stg01.r21967De001\":{\"name\":\"TE aggregate regression weight\"},\"r21967Stg01.ou\":{\"name\":\"Organisation unit, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"},\"r21967Stg01.r21967De001\":{\"name\":\"TE aggregate regression weight\"}},\"dimensions\":{\"r21967Stg01.eventstatus\":[],\"r21967De001\":[],\"pe\":[],\"r21967Stg01.ou\":[\"DiszpKrYNg8\"],\"r21967Stg01.r21967De001\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.ou",
        "Organisation unit, TE aggregate scoped regression, Grouped visits",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.eventstatus",
        "Event Status, TE aggregate scoped regression, Grouped visits",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.r21967De001",
        "TE aggregate regression weight, TE aggregate scoped regression, Grouped visits",
        "NUMBER",
        "java.lang.Double",
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
        Map.of(
            "r21967Stg01.ou",
            "DiszpKrYNg8",
            "r21967Stg01.eventstatus",
            "ACTIVE",
            "r21967Stg01.r21967De001",
            "",
            "value",
            ""));
  }

  @Test
  public void aggregateScopedTiedEventsCountOnce() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=trackedentity:EQ:r21967Te007")
            .add("aggregationType=COUNT")
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.EVENT_DATE")
            .add("value=r21967Prg01.r21967Stg01.r21967De001");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        1,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Stg01.eventdate\":{\"name\":\"Event Date, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.eventdate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.eventdate",
        "Event Date, TE aggregate scoped regression, Grouped visits",
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
        Map.of("r21967Stg01.eventdate", "2020-01-01 00:00:00.0", "value", "1"));
  }

  @Test
  public void aggregateScopedGroupedDatesDoNotMultiplyTrackedEntities() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("paging=false")
            .add("program=r21967Prg01")
            .add("dimension=r21967Stg01.EVENT_DATE");

    // When
    ApiResponse response = actions.aggregate().get("r21967Tet01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        7,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"r21967Stg01\":{\"name\":\"Grouped visits\"},\"r21967Stg01.eventdate\":{\"name\":\"Event Date, TE aggregate scoped regression, Grouped visits\"},\"r21967Prg01\":{\"name\":\"TE aggregate scoped regression\"}},\"dimensions\":{\"pe\":[],\"r21967Stg01.eventdate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "r21967Stg01.eventdate",
        "Event Date, TE aggregate scoped regression, Grouped visits",
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
        Map.of("r21967Stg01.eventdate", "2020-01-01 00:00:00.0", "value", "1"));

    // Validate row exists with values from original row index 3
    validateRowExists(
        response,
        actualHeaders,
        Map.of("r21967Stg01.eventdate", "2021-07-01 18:00:00.0", "value", "1"));

    // Validate row exists with values from original row index 6
    validateRowExists(response, actualHeaders, Map.of("r21967Stg01.eventdate", "", "value", "1"));
  }
}
