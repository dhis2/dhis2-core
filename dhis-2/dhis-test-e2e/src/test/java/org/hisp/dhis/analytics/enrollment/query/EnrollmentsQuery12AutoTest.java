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

import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderExistence;
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

public class EnrollmentsQuery12AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void enrollmentsWithTotalPagesAndAttributeFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=ouname,enrollmentdate")
            .add("totalPages=true")
            .add("outputType=ENROLLMENT")
            .add("pageSize=6")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf,cejWyOfXge6:EQ:Female")
            .add("desc=enrollmentdate");

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
        "{\"pager\":{\"page\":1,\"total\":107,\"pageSize\":6,\"pageCount\":18},\"items\":{\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"cejWyOfXge6\":{\"name\":\"Gender\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"lY93YpCxJqf\"],\"cejWyOfXge6\":[\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

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
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mabenteh Community Hospital");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2023-12-27 12:05:00.0");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Kerefay Loko MCHP");
    validateRowValueByName(response, actualHeaders, 2, "enrollmentdate", "2023-12-08 12:05:00.0");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "ouname", "Kerefay Loko MCHP");
    validateRowValueByName(response, actualHeaders, 4, "enrollmentdate", "2023-10-26 12:05:00.0");

    // Validate selected values for row index 5
    validateRowValueByName(response, actualHeaders, 5, "ouname", "Kunsho CHP");
    validateRowValueByName(response, actualHeaders, 5, "enrollmentdate", "2023-10-13 12:05:00.0");
  }

  @Test
  public void enrollmentsWithTotalPagesAndRepeatableStageDataElementFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=ouname,enrollmentdate")
            .add("totalPages=true")
            .add("outputType=ENROLLMENT")
            .add("pageSize=10")
            .add("page=1")
            .add("dimension=ou:ImspTQPwCqd,jdRD35YwbRH.zocHNQIQBIN:EQ:POS")
            .add("desc=enrollmentdate");

    // When
    ApiResponse response = actions.query().get("ur1Edk5Oe2n", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        4,
        2,
        2); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":4,\"pageSize\":10,\"pageCount\":1},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"Im9PtRGpdDW\":{\"uid\":\"Im9PtRGpdDW\",\"name\":\"Test result (positive\\/negative)\",\"options\":[{\"uid\":\"PLnNqhtyzAu\",\"code\":\"POS\"}]},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"ou\":{\"name\":\"Organisation unit\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"PLnNqhtyzAu\":{\"code\":\"POS\",\"name\":\"Positive\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"zocHNQIQBIN\":{\"name\":\"TB smear microscopy test outcome\"},\"jdRD35YwbRH.zocHNQIQBIN\":{\"name\":\"TB smear microscopy test outcome\"}},\"dimensions\":{\"jdRD35YwbRH.zocHNQIQBIN\":[\"PLnNqhtyzAu\"],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

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

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Motorbong MCHP");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2022-02-18 12:27:49.129");

    // Validate selected values for row index 1
    validateRowValueByName(response, actualHeaders, 1, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 1, "enrollmentdate", "2022-02-09 12:27:48.637");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 2, "enrollmentdate", "2021-10-15 12:34:17.849");

    // Validate selected values for row index 3
    validateRowValueByName(response, actualHeaders, 3, "ouname", "Ngelehun CHC");
    validateRowValueByName(response, actualHeaders, 3, "enrollmentdate", "2021-06-17 12:27:48.595");
  }

  @Test
  public void enrollmentsWithTotalPagesAndEnrollmentDateFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("totalPages=true")
            .add("outputType=ENROLLMENT")
            .add("pageSize=6")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf,ENROLLMENT_DATE:GT:2023-01-01")
            .add("desc=enrollmentdate");

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
        19,
        16); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"total\":107,\"pageSize\":6,\"pageCount\":18},\"items\":{\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"enrollmentdate\":{\"name\":\"enrollmentdate\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"lY93YpCxJqf\"],\"enrollmentdate\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "pi", "Enrollment", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "tei", "Tracked entity", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "enrollmentdate",
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
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
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
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
        "created",
        "Created on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "completed",
        "Completed on",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response, actualHeaders, "geometry", "Geometry", "TEXT", "java.lang.String", false, true);
    }
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "longitude",
          "Longitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
    if (expectPostgis) {
      validateHeaderPropertiesByName(
          response,
          actualHeaders,
          "latitude",
          "Latitude",
          "NUMBER",
          "java.lang.Double",
          false,
          true);
    }
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
        "ounamehierarchy",
        "Organisation unit name hierarchy",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "oucode",
        "Organisation unit code",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "programstatus",
        "Program status",
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

    // Assert PostGIS-specific headers existence based on 'expectPostgis' flag
    if (expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", true);
      validateHeaderExistence(actualHeaders, "longitude", true);
      validateHeaderExistence(actualHeaders, "latitude", true);
    } else {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "pi", "JJyzRNkM11L");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2023-12-27 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 0, "tei", "aF3AZDKWP6C");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2023-12-27 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 0, "incidentdate", "2023-12-27 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mabenteh Community Hospital");
    validateRowValueByName(response, actualHeaders, 0, "programstatus", "ACTIVE");
    validateRowValueByName(response, actualHeaders, 0, "ou", "taKiTcaf05H");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "pi", "QkpN2N91b7O");
    validateRowValueByName(response, actualHeaders, 2, "enrollmentdate", "2023-12-23 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 2, "tei", "n9Nmtwk7eCI");
    validateRowValueByName(response, actualHeaders, 2, "enrollmentdate", "2023-12-23 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 2, "incidentdate", "2023-12-23 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Tonkomba MCHP");
    validateRowValueByName(response, actualHeaders, 2, "programstatus", "ACTIVE");
    validateRowValueByName(response, actualHeaders, 2, "ou", "xIMxph4NMP1");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "pi", "KfWbe2RVaJO");
    validateRowValueByName(response, actualHeaders, 4, "enrollmentdate", "2023-12-21 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 4, "tei", "Eyt9B8NqA70");
    validateRowValueByName(response, actualHeaders, 4, "enrollmentdate", "2023-12-21 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 4, "incidentdate", "2023-12-21 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 4, "ouname", "Stocco CHP");
    validateRowValueByName(response, actualHeaders, 4, "programstatus", "ACTIVE");
    validateRowValueByName(response, actualHeaders, 4, "ou", "Zwnfm4rnzbZ");

    // Validate selected values for row index 5
    validateRowValueByName(response, actualHeaders, 5, "pi", "X5ZoWNIK9Xj");
    validateRowValueByName(response, actualHeaders, 5, "enrollmentdate", "2023-12-20 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 5, "tei", "K1qieDyIage");
    validateRowValueByName(response, actualHeaders, 5, "enrollmentdate", "2023-12-20 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 5, "incidentdate", "2023-12-20 12:05:00.0");
    validateRowValueByName(response, actualHeaders, 5, "ouname", "Tonkomba MCHP");
    validateRowValueByName(response, actualHeaders, 5, "programstatus", "ACTIVE");
    validateRowValueByName(response, actualHeaders, 5, "ou", "xIMxph4NMP1");
  }

  @Test
  public void enrollmentsWithTotalPagesAndStageDataElementFilter() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=ouname,enrollmentdate")
            .add("stage=A03MvHHogjR")
            .add("totalPages=true")
            .add("outputType=ENROLLMENT")
            .add("pageSize=6")
            .add("page=1")
            .add("dimension=ou:lY93YpCxJqf,A03MvHHogjR.UXz7xuGCEhU:GT:3000")
            .add("desc=enrollmentdate");

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
        "{\"pager\":{\"page\":1,\"total\":167,\"pageSize\":6,\"pageCount\":28},\"items\":{\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"A03MvHHogjR.UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"},\"UXz7xuGCEhU\":{\"name\":\"MCH Weight (g)\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"lY93YpCxJqf\"],\"A03MvHHogjR.UXz7xuGCEhU\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Dimension values must retain their requested order.
    assertEquals(
        new JSONObject(expectedMetaData).getJSONObject("dimensions").toString(),
        new JSONObject(actualMetaData).getJSONObject("dimensions").toString(),
        true);

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
        "Date of enrollment",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values by name at specific indices (sorted results).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "ouname", "Mabenteh Community Hospital");
    validateRowValueByName(response, actualHeaders, 0, "enrollmentdate", "2023-12-27 12:05:00.0");

    // Validate selected values for row index 2
    validateRowValueByName(response, actualHeaders, 2, "ouname", "Stocco CHP");
    validateRowValueByName(response, actualHeaders, 2, "enrollmentdate", "2023-12-22 12:05:00.0");

    // Validate selected values for row index 4
    validateRowValueByName(response, actualHeaders, 4, "ouname", "Tonkomba MCHP");
    validateRowValueByName(response, actualHeaders, 4, "enrollmentdate", "2023-12-20 12:05:00.0");

    // Validate selected values for row index 5
    validateRowValueByName(response, actualHeaders, 5, "ouname", "Tonkomba MCHP");
    validateRowValueByName(response, actualHeaders, 5, "enrollmentdate", "2023-12-14 12:05:00.0");
  }
}
