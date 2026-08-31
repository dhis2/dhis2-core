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
public class EventsQuery8AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void optionSetDimensionFilteredByNoValueKeyword() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,fWIAEtYVEGk")
            .add("stage=Zj7UnCAulEk")
            .add("paging=false")
            .add("dimension=ou:ImspTQPwCqd,fWIAEtYVEGk:IN:D2__NOVALUE")
            .add("eventDate=2021");

    // When
    ApiResponse response = actions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        13,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"eBAyeGv0exc\":{\"uid\":\"eBAyeGv0exc\",\"name\":\"Inpatient morbidity and mortality\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"fWIAEtYVEGk\":{\"uid\":\"fWIAEtYVEGk\",\"code\":\"DE_3000009\",\"name\":\"Mode of Discharge\",\"description\":\"How the patient was discharged.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"2021\":{\"name\":\"2021\"},\"eventdate\":{\"name\":\"Event date\"},\"Zj7UnCAulEk\":{\"uid\":\"Zj7UnCAulEk\",\"name\":\"Inpatient morbidity and mortality\",\"description\":\"Anonymous and ICD-10 coded inpatient data\"}},\"dimensions\":{\"pe\":[],\"eventdate\":[\"2021\"],\"ou\":[\"ImspTQPwCqd\"],\"fWIAEtYVEGk\":[\"D2__NOVALUE\"]}}";
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
        "fWIAEtYVEGk",
        "Mode of Discharge",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 0, "fWIAEtYVEGk", "");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 4, "fWIAEtYVEGk", "");

    // Validate selected values for row index 8
    validateRowValueByName(response, actualHeaders, 8, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 8, "fWIAEtYVEGk", "");

    // Validate selected values for row index 12
    validateRowValueByName(response, actualHeaders, 12, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 12, "fWIAEtYVEGk", "");
  }

  @Test
  public void registrationOuWithBothHeadersAndEventOuName() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=registrationou,registrationouname,ouname")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=pe:2022,REGISTRATION_OU:O6uvpzGd5pu");

    // When
    ApiResponse response = actions.query().get("regOuProg01", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"pe\":{},\"ou\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"O6uvpzGd5pu\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationou",
        "Registration org unit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationouname",
        "Registration org unit name",
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "registrationou", "aBfyTU5Wgds");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Sienga CHP");
  }

  @Test
  public void registrationOuWithLevel2SortedAscending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=registrationouname")
            .add("headers=registrationou,registrationouname")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=pe:2022,REGISTRATION_OU:LEVEL-2");

    // When
    ApiResponse response = actions.query().get("regOuProg01", JSON, JSON, params);

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
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"ou\":{},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"pe\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationou",
        "Registration org unit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationouname",
        "Registration org unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "registrationou", "aBfyTU5Wgds");
    validateRowValueByName(response, actualHeaders, 0, "registrationouname", "Nduvuibu MCHP");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "registrationou", "a1E6QWBTEwX");
    validateRowValueByName(response, actualHeaders, 2, "registrationouname", "Sienga CHP");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "registrationou", "agEKP19IUKI");
    validateRowValueByName(response, actualHeaders, 4, "registrationouname", "Tambiama CHC");

    // Validate selected values for row index 5
    validateRowValueByName(response, actualHeaders, 5, "registrationou", "agEKP19IUKI");
    validateRowValueByName(response, actualHeaders, 5, "registrationouname", "Tambiama CHC");
  }

  @Test
  public void registrationOuAsFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=REGISTRATION_OU:jUb8gELQApl")
            .add("headers=ouname")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=pe:2022");

    // When
    ApiResponse response = actions.query().get("regOuProg01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        3,
        1,
        1); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"pe\":{},\"ou\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"2022\":{\"name\":\"2022\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Tambiama CHC");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Tambiama CHC");
  }

  @Test
  public void registrationOuSortedDescending() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=registrationouname")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=pe:2022,REGISTRATION_OU:LEVEL-2")
            .add("desc=registrationouname");

    // When
    ApiResponse response = actions.query().get("regOuProg01", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        6,
        1,
        1); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"ou\":{},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"2022\":{\"name\":\"2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"registrationou\":{\"name\":\"Registration org unit\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"pe\":{},\"regOuProg01\":{\"name\":\"Registration OU test program\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"regOuStge01\":{\"name\":\"Registration OU test stage\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"registrationou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "registrationouname",
        "Registration org unit name",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "registrationouname", "Tambiama CHC");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "registrationouname", "Sienga CHP");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "registrationouname", "Sienga CHP");

    // Validate selected values for row index 5
    validateRowValueByName(response, actualHeaders, 5, "registrationouname", "Nduvuibu MCHP");
  }
}
