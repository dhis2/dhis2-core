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
import static org.hisp.dhis.analytics.ValidationHelper.validateRowValueByName;
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
public class TrackedEntityAggregate1AutoTest extends AnalyticsApiTest {
  private final AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  @Test
  public void aggregateCountByOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=ou")
            .add("totalPages=false")
            .add("pageSize=5")
            .add("dimension=ou:a04CZxe0PSe;a1dP5m3Clw4;a1E6QWBTEwX;a5glgtnXJRG;aBfyTU5Wgds");

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
        "{\"pager\":{\"page\":1,\"pageSize\":5,\"isLastPage\":true},\"items\":{\"a04CZxe0PSe\":{\"name\":\"Murray Town CHC\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"aBfyTU5Wgds\":{\"name\":\"Nduvuibu MCHP\"},\"a1dP5m3Clw4\":{\"name\":\"Baoma Kpenge CHP\"},\"a1E6QWBTEwX\":{\"name\":\"Sienga CHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"a5glgtnXJRG\":{\"name\":\"Magbanabom MCHP\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"a04CZxe0PSe\",\"a1dP5m3Clw4\",\"a1E6QWBTEwX\",\"a5glgtnXJRG\",\"aBfyTU5Wgds\"]}}";
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

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ou", "a04CZxe0PSe");
    validateRowValueByName(response, actualHeaders, 0, "value", "54");

    // Validate selected values for row index 3 (collation-stable; index 2 swaps a1d/a1E under C
    // collation)
    validateRowValueByName(response, actualHeaders, 3, "ou", "a5glgtnXJRG");
    validateRowValueByName(response, actualHeaders, 3, "value", "62");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "ou", "aBfyTU5Wgds");
    validateRowValueByName(response, actualHeaders, 4, "value", "53");
  }

  @Test
  public void aggregateAverageValueByOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=ou")
            .add("aggregationType=AVERAGE")
            .add("totalPages=false")
            .add("pageSize=5")
            .add("dimension=ou:a04CZxe0PSe;a1dP5m3Clw4;a1E6QWBTEwX;a5glgtnXJRG;aBfyTU5Wgds")
            .add("value=lw1SqmMlnfh");

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
        "{\"pager\":{\"page\":1,\"pageSize\":5,\"isLastPage\":true},\"items\":{\"a04CZxe0PSe\":{\"name\":\"Murray Town CHC\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"aBfyTU5Wgds\":{\"name\":\"Nduvuibu MCHP\"},\"a1dP5m3Clw4\":{\"name\":\"Baoma Kpenge CHP\"},\"a1E6QWBTEwX\":{\"name\":\"Sienga CHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"a5glgtnXJRG\":{\"name\":\"Magbanabom MCHP\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"a04CZxe0PSe\",\"a1dP5m3Clw4\",\"a1E6QWBTEwX\",\"a5glgtnXJRG\",\"aBfyTU5Wgds\"]}}";
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

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ou", "a04CZxe0PSe");
    validateRowValueByName(response, actualHeaders, 0, "value", "169.47");

    // Validate selected values for row index 3 (collation-stable; index 2 swaps a1d/a1E under C
    // collation)
    validateRowValueByName(response, actualHeaders, 3, "ou", "a5glgtnXJRG");
    validateRowValueByName(response, actualHeaders, 3, "value", "172.36");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "ou", "aBfyTU5Wgds");
    validateRowValueByName(response, actualHeaders, 4, "value", "170.32");
  }

  @Test
  public void aggregateSumValueByOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=ou")
            .add("aggregationType=SUM")
            .add("totalPages=false")
            .add("pageSize=5")
            .add("dimension=ou:a04CZxe0PSe;a1dP5m3Clw4;a1E6QWBTEwX;a5glgtnXJRG;aBfyTU5Wgds")
            .add("value=lw1SqmMlnfh");

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
        "{\"pager\":{\"page\":1,\"pageSize\":5,\"isLastPage\":true},\"items\":{\"a04CZxe0PSe\":{\"name\":\"Murray Town CHC\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"aBfyTU5Wgds\":{\"name\":\"Nduvuibu MCHP\"},\"a1dP5m3Clw4\":{\"name\":\"Baoma Kpenge CHP\"},\"a1E6QWBTEwX\":{\"name\":\"Sienga CHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"a5glgtnXJRG\":{\"name\":\"Magbanabom MCHP\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"a04CZxe0PSe\",\"a1dP5m3Clw4\",\"a1E6QWBTEwX\",\"a5glgtnXJRG\",\"aBfyTU5Wgds\"]}}";
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

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ou", "a04CZxe0PSe");
    validateRowValueByName(response, actualHeaders, 0, "value", "6101");

    // Validate selected values for row index 3 (collation-stable; index 2 swaps a1d/a1E under C
    // collation)
    validateRowValueByName(response, actualHeaders, 3, "ou", "a5glgtnXJRG");
    validateRowValueByName(response, actualHeaders, 3, "value", "7756");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "ou", "aBfyTU5Wgds");
    validateRowValueByName(response, actualHeaders, 4, "value", "6302");
  }

  @Test
  public void aggregateCountOfNonNullValueByOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=ou")
            .add("aggregationType=COUNT")
            .add("totalPages=false")
            .add("pageSize=5")
            .add("dimension=ou:a04CZxe0PSe;a1dP5m3Clw4;a1E6QWBTEwX;a5glgtnXJRG;aBfyTU5Wgds")
            .add("value=lw1SqmMlnfh");

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
        "{\"pager\":{\"page\":1,\"pageSize\":5,\"isLastPage\":true},\"items\":{\"a04CZxe0PSe\":{\"name\":\"Murray Town CHC\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"aBfyTU5Wgds\":{\"name\":\"Nduvuibu MCHP\"},\"a1dP5m3Clw4\":{\"name\":\"Baoma Kpenge CHP\"},\"a1E6QWBTEwX\":{\"name\":\"Sienga CHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"a5glgtnXJRG\":{\"name\":\"Magbanabom MCHP\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"a04CZxe0PSe\",\"a1dP5m3Clw4\",\"a1E6QWBTEwX\",\"a5glgtnXJRG\",\"aBfyTU5Wgds\"]}}";
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

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ou", "a04CZxe0PSe");
    validateRowValueByName(response, actualHeaders, 0, "value", "36");

    // Validate selected values for row index 3 (collation-stable; index 2 swaps a1d/a1E under C
    // collation)
    validateRowValueByName(response, actualHeaders, 3, "ou", "a5glgtnXJRG");
    validateRowValueByName(response, actualHeaders, 3, "value", "45");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "ou", "aBfyTU5Wgds");
    validateRowValueByName(response, actualHeaders, 4, "value", "37");
  }

  @Test
  public void aggregateAverageValueByOrgUnitAndGender() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("asc=ou,cejWyOfXge6")
            .add("aggregationType=AVERAGE")
            .add("totalPages=false")
            .add("pageSize=5")
            .add("dimension=ou:a04CZxe0PSe;a1dP5m3Clw4,cejWyOfXge6")
            .add("value=lw1SqmMlnfh");

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
        "{\"pager\":{\"page\":1,\"pageSize\":5,\"isLastPage\":false},\"items\":{\"a04CZxe0PSe\":{\"name\":\"Murray Town CHC\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"a1dP5m3Clw4\":{\"name\":\"Baoma Kpenge CHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\"},{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"cejWyOfXge6\":{\"name\":\"Gender\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"a04CZxe0PSe\",\"a1dP5m3Clw4\"],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
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
        response, actualHeaders, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ou", "a04CZxe0PSe");
    validateRowValueByName(response, actualHeaders, 0, "value", "162.52");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "ou", "a04CZxe0PSe");
    validateRowValueByName(response, actualHeaders, 2, "value", "");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "ou", "a1dP5m3Clw4");
    validateRowValueByName(response, actualHeaders, 4, "value", "177.53");
  }
}
