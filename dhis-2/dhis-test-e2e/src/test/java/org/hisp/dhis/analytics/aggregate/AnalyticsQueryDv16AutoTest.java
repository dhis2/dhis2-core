/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderExistence;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/analytics" aggregate endpoint. */
public class AnalyticsQueryDv16AutoTest extends AnalyticsApiTest {
  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  @DisplayName("Assert that metadata only contains the selected options")
  public void programDataElementOptionDimensionItemWithOptions() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:USER_ORGUNIT")
            .add("skipData=true")
            .add("includeMetadataDetails=true")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("relativePeriodDate=2025-03-01")
            .add(
                "dimension=dx:eBAyeGv0exc.K6uUAvq500H.IwbX7Ubzu57;eBAyeGv0exc.K6uUAvq500H.Fg4hq7La1MN;eBAyeGv0exc.K6uUAvq500H.UYhscH2W06r;eBAyeGv0exc.K6uUAvq500H.rmYsyizWAJx,pe:LAST_12_MONTHS");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(0)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(0));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"eBAyeGv0exc.K6uUAvq500H.rmYsyizWAJx\":{\"name\":\"A00 Cholera (Diagnosis (ICD-10), Inpatient morbidity and mortality)\",\"dimensionItemType\":\"PROGRAM_DATA_ELEMENT_OPTION\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"202408\":{\"uid\":\"202408\",\"code\":\"202408\",\"name\":\"August 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-08-01T00:00:00.000\",\"endDate\":\"2024-08-31T00:00:00.000\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"202409\":{\"uid\":\"202409\",\"code\":\"202409\",\"name\":\"September 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-09-01T00:00:00.000\",\"endDate\":\"2024-09-30T00:00:00.000\"},\"202406\":{\"uid\":\"202406\",\"code\":\"202406\",\"name\":\"June 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-06-01T00:00:00.000\",\"endDate\":\"2024-06-30T00:00:00.000\"},\"K6uUAvq500H.eUZ79clX7y1\":{\"uid\":\"eUZ79clX7y1\",\"name\":\"Diagnosis ICD10\",\"options\":[{\"uid\":\"rmYsyizWAJx\",\"code\":\"A00\"},{\"uid\":\"IwbX7Ubzu57\",\"code\":\"A000\"},{\"uid\":\"UYhscH2W06r\",\"code\":\"A009\"},{\"uid\":\"Fg4hq7La1MN\",\"code\":\"A001\"}]},\"202407\":{\"uid\":\"202407\",\"code\":\"202407\",\"name\":\"July 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-07-01T00:00:00.000\",\"endDate\":\"2024-07-31T00:00:00.000\"},\"202404\":{\"uid\":\"202404\",\"code\":\"202404\",\"name\":\"April 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-04-01T00:00:00.000\",\"endDate\":\"2024-04-30T00:00:00.000\"},\"202405\":{\"uid\":\"202405\",\"code\":\"202405\",\"name\":\"May 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-05-01T00:00:00.000\",\"endDate\":\"2024-05-31T00:00:00.000\"},\"202501\":{\"uid\":\"202501\",\"code\":\"202501\",\"name\":\"January 2025\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2025-01-01T00:00:00.000\",\"endDate\":\"2025-01-31T00:00:00.000\"},\"202403\":{\"uid\":\"202403\",\"code\":\"202403\",\"name\":\"March 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-03-01T00:00:00.000\",\"endDate\":\"2024-03-31T00:00:00.000\"},\"202502\":{\"uid\":\"202502\",\"code\":\"202502\",\"name\":\"February 2025\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2025-02-01T00:00:00.000\",\"endDate\":\"2025-02-28T00:00:00.000\"},\"202411\":{\"uid\":\"202411\",\"code\":\"202411\",\"name\":\"November 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-11-01T00:00:00.000\",\"endDate\":\"2024-11-30T00:00:00.000\"},\"202412\":{\"uid\":\"202412\",\"code\":\"202412\",\"name\":\"December 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-12-01T00:00:00.000\",\"endDate\":\"2024-12-31T00:00:00.000\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202410\":{\"uid\":\"202410\",\"code\":\"202410\",\"name\":\"October 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-10-01T00:00:00.000\",\"endDate\":\"2024-10-31T00:00:00.000\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"dx\":{\"uid\":\"dx\",\"name\":\"Data\",\"dimensionType\":\"DATA_X\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"eBAyeGv0exc.K6uUAvq500H.IwbX7Ubzu57\":{\"name\":\"A000 Cholera due to Vibrio cholerae 01, biovar cholerae (Diagnosis (ICD-10), Inpatient morbidity and mortality)\",\"dimensionItemType\":\"PROGRAM_DATA_ELEMENT_OPTION\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"eBAyeGv0exc.K6uUAvq500H.UYhscH2W06r\":{\"name\":\"A009 Cholera, unspecified (Diagnosis (ICD-10), Inpatient morbidity and mortality)\",\"dimensionItemType\":\"PROGRAM_DATA_ELEMENT_OPTION\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"eBAyeGv0exc.K6uUAvq500H.Fg4hq7La1MN\":{\"name\":\"A001 Cholera due to Vibrio cholerae 01, biovar eltor (Diagnosis (ICD-10), Inpatient morbidity and mortality)\",\"dimensionItemType\":\"PROGRAM_DATA_ELEMENT_OPTION\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"dx\":[\"eBAyeGv0exc.K6uUAvq500H.IwbX7Ubzu57\",\"eBAyeGv0exc.K6uUAvq500H.Fg4hq7La1MN\",\"eBAyeGv0exc.K6uUAvq500H.UYhscH2W06r\",\"eBAyeGv0exc.K6uUAvq500H.rmYsyizWAJx\"],\"pe\":[\"202403\",\"202404\",\"202405\",\"202406\",\"202407\",\"202408\",\"202409\",\"202410\",\"202411\",\"202412\",\"202501\",\"202502\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);
  }

  @Test
  public void programDataElementOptionShortNameInMetaData() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:USER_ORGUNIT")
            .add("skipData=true")
            .add("includeMetadataDetails=true")
            .add("includeNumDen=true")
            .add("displayProperty=SHORTNAME")
            .add("skipMeta=false")
            .add("relativePeriodDate=2025-03-01")
            .add(
                "dimension=dx:qDkgAbB5Jlk.XCMLePzaZiL.vak9GKjzzAP;qDkgAbB5Jlk.XCMLePzaZiL.zPVS0EAEwia,pe:LAST_12_MONTHS");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(0)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(0));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"202408\":{\"uid\":\"202408\",\"code\":\"202408\",\"name\":\"August 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-08-01T00:00:00.000\",\"endDate\":\"2024-08-31T00:00:00.000\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"202409\":{\"uid\":\"202409\",\"code\":\"202409\",\"name\":\"September 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-09-01T00:00:00.000\",\"endDate\":\"2024-09-30T00:00:00.000\"},\"202406\":{\"uid\":\"202406\",\"code\":\"202406\",\"name\":\"June 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-06-01T00:00:00.000\",\"endDate\":\"2024-06-30T00:00:00.000\"},\"202407\":{\"uid\":\"202407\",\"code\":\"202407\",\"name\":\"July 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-07-01T00:00:00.000\",\"endDate\":\"2024-07-31T00:00:00.000\"},\"202404\":{\"uid\":\"202404\",\"code\":\"202404\",\"name\":\"April 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-04-01T00:00:00.000\",\"endDate\":\"2024-04-30T00:00:00.000\"},\"202405\":{\"uid\":\"202405\",\"code\":\"202405\",\"name\":\"May 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-05-01T00:00:00.000\",\"endDate\":\"2024-05-31T00:00:00.000\"},\"qDkgAbB5Jlk.XCMLePzaZiL.vak9GKjzzAP\":{\"name\":\"No (Symptoms, Case)\",\"dimensionItemType\":\"PROGRAM_DATA_ELEMENT_OPTION\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"202501\":{\"uid\":\"202501\",\"code\":\"202501\",\"name\":\"January 2025\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2025-01-01T00:00:00.000\",\"endDate\":\"2025-01-31T00:00:00.000\"},\"202403\":{\"uid\":\"202403\",\"code\":\"202403\",\"name\":\"March 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-03-01T00:00:00.000\",\"endDate\":\"2024-03-31T00:00:00.000\"},\"202502\":{\"uid\":\"202502\",\"code\":\"202502\",\"name\":\"February 2025\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2025-02-01T00:00:00.000\",\"endDate\":\"2025-02-28T00:00:00.000\"},\"202411\":{\"uid\":\"202411\",\"code\":\"202411\",\"name\":\"November 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-11-01T00:00:00.000\",\"endDate\":\"2024-11-30T00:00:00.000\"},\"202412\":{\"uid\":\"202412\",\"code\":\"202412\",\"name\":\"December 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-12-01T00:00:00.000\",\"endDate\":\"2024-12-31T00:00:00.000\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202410\":{\"uid\":\"202410\",\"code\":\"202410\",\"name\":\"October 2024\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2024-10-01T00:00:00.000\",\"endDate\":\"2024-10-31T00:00:00.000\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"XCMLePzaZiL.qtswn9lMSXN\":{\"uid\":\"qtswn9lMSXN\",\"name\":\"Yes/No\",\"options\":[{\"code\":\"YES\",\"uid\":\"zPVS0EAEwia\"},{\"code\":\"NO\",\"uid\":\"vak9GKjzzAP\"}]},\"dx\":{\"uid\":\"dx\",\"name\":\"Data\",\"dimensionType\":\"DATA_X\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"qDkgAbB5Jlk.XCMLePzaZiL.zPVS0EAEwia\":{\"name\":\"Yes (Symptoms, Case)\",\"dimensionItemType\":\"PROGRAM_DATA_ELEMENT_OPTION\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"}},\"dimensions\":{\"dx\":[\"qDkgAbB5Jlk.XCMLePzaZiL.vak9GKjzzAP\",\"qDkgAbB5Jlk.XCMLePzaZiL.zPVS0EAEwia\"],\"pe\":[\"202403\",\"202404\",\"202405\",\"202406\",\"202407\",\"202408\",\"202409\",\"202410\",\"202411\",\"202412\",\"202501\",\"202502\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);
  }

  @Test
  public void tableLayoutKeepOrderOfColumnsHeaders() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = BooleanUtils.toBoolean(System.getProperty("expect.postgis", "false"));

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:USER_ORGUNIT")
            .add("tableLayout=true")
            .add("columns=dx")
            .add("completedOnly=false")
            .add("rows=pe")
            .add("dimension=dx:Uvn6LCg7dVU;OdiHJayrsKo;sB79w2hiLp8;cYeuwXTCPkU,pe:LAST_12_MONTHS")
            .add("skipRounding=false")
            .add("relativePeriodDate=2023-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        12,
        11,
        8); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"sB79w2hiLp8\":{\"name\":\"ANC 3 Coverage\"},\"ou\":{\"name\":\"Organisation unit\"},\"OdiHJayrsKo\":{\"name\":\"ANC 2 Coverage\"},\"202208\":{\"name\":\"August 2022\"},\"202209\":{\"name\":\"September 2022\"},\"202206\":{\"name\":\"June 2022\"},\"202207\":{\"name\":\"July 2022\"},\"202204\":{\"name\":\"April 2022\"},\"202205\":{\"name\":\"May 2022\"},\"202202\":{\"name\":\"February 2022\"},\"202203\":{\"name\":\"March 2022\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202211\":{\"name\":\"November 2022\"},\"202201\":{\"name\":\"January 2022\"},\"202212\":{\"name\":\"December 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202210\":{\"name\":\"October 2022\"},\"dx\":{\"name\":\"Data\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"pe\":{\"name\":\"Period\"},\"cYeuwXTCPkU\":{\"name\":\"ANC 2nd visit\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\",\"OdiHJayrsKo\",\"sB79w2hiLp8\",\"cYeuwXTCPkU\"],\"pe\":[\"202201\",\"202202\",\"202203\",\"202204\",\"202205\",\"202206\",\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "Period ID", "periodid", "TEXT", "java.lang.String", true, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "Period", "periodname", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Period code",
        "periodcode",
        "TEXT",
        "java.lang.String",
        true,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Period description",
        "perioddescription",
        "TEXT",
        "java.lang.String",
        true,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "anc 1 coverage",
        "ANC 1 Coverage",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "anc 2 coverage",
        "ANC 2 Coverage",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "anc 3 coverage",
        "ANC 3 Coverage",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "anc 2nd visit",
        "ANC 2nd visit",
        "NUMBER",
        "java.lang.Double",
        false,
        false);

    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false
    if (!expectPostgis) {
      validateHeaderExistence(actualHeaders, "geometry", false);
      validateHeaderExistence(actualHeaders, "longitude", false);
      validateHeaderExistence(actualHeaders, "latitude", false);
    }

    // rowContext not found or empty in the response, skipping assertions.

    // 7. Assert row values
    validateRow(
        response,
        List.of("202208", "August 2022", "202208", "", "104.11", "96.68", "69.53", "20433"));

    validateRow(
        response,
        List.of("202207", "July 2022", "202207", "", "107.05", "100.34", "70.47", "21208"));

    validateRow(
        response,
        List.of("202212", "December 2022", "202212", "", "75.58", "70.2", "50.13", "14837"));

    validateRow(
        response,
        List.of("202203", "March 2022", "202203", "", "87.89", "87.14", "59.17", "18418"));

    validateRow(
        response,
        List.of("202205", "May 2022", "202205", "", "139.39", "112.26", "74.28", "23726"));

    validateRow(
        response,
        List.of("202210", "October 2022", "202210", "", "89.75", "88.93", "69.28", "18795"));

    validateRow(
        response,
        List.of("202201", "January 2022", "202201", "", "94.72", "82.08", "56.13", "17347"));

    validateRow(
        response,
        List.of("202204", "April 2022", "202204", "", "106.96", "95.7", "68.12", "19574"));

    validateRow(
        response,
        List.of("202206", "June 2022", "202206", "", "116.42", "116.84", "76.63", "23898"));

    validateRow(
        response,
        List.of("202211", "November 2022", "202211", "", "87.64", "78.78", "57.81", "16113"));

    validateRow(
        response,
        List.of("202202", "February 2022", "202202", "", "98.8", "97.32", "63.72", "18578"));

    validateRow(
        response,
        List.of("202209", "September 2022", "202209", "", "109.52", "99.9", "74.92", "20433"));
  }
}
