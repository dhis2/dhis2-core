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
import static org.hisp.dhis.analytics.ValidationHelper.validateRowValueByName;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dependsOn.DependsOn;
import org.hisp.dhis.test.e2e.dependsOn.Resource;
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
    boolean expectPostgis = isPostgres();

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
        8,
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

  @Test
  public void tableLayoutDownloadPivotTable() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();

    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("tableLayout=true")
            .add("columns=pe")
            .add("completedOnly=false")
            .add("rows=dx;ou")
            .add(
                "dimension=pe:202101;202102;202103;202104;202105;202106;202107;202108;202109;202110;202111;202112;202201;202202;202203;202204;202205;202206;202207;202208;202209;202210;202211;202212;202001;202002;202003;202004;202005;202006;202007;202008;202009;202010;202011;202012,dx:FQ2o8UBlcrS;GSae40Fyppf;dSBYyCUjCXd;RUv0hqER0zV;IpHINAT79UW.cejWyOfXge6;IpHINAT79UW.wQLfBvPrXqq,ou:ImspTQPwCqd;USER_ORGUNIT_CHILDREN;USER_ORGUNIT_GRANDCHILDREN")
            .add("skipRounding=false");

    // When
    ApiResponse response = actions.get(params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response,
        expectPostgis,
        996,
        44,
        44); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"yu4N82FFeLm\":{\"name\":\"Mandu\"},\"KXSqt7jv6DU\":{\"name\":\"Gorama Mende\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"vULnao2hV5v\":{\"name\":\"Fakunya\"},\"202008\":{\"name\":\"August 2020\"},\"U6Kr7Gtpidn\":{\"name\":\"Kakua\"},\"hjpHnHZIniP\":{\"name\":\"Kissi Tongi\"},\"202009\":{\"name\":\"September 2020\"},\"202006\":{\"name\":\"June 2020\"},\"iUauWFeH8Qp\":{\"name\":\"Bum\"},\"EYt6ThQDagn\":{\"name\":\"Koya (kenema)\"},\"202007\":{\"name\":\"July 2020\"},\"202004\":{\"name\":\"April 2020\"},\"202005\":{\"name\":\"May 2020\"},\"202011\":{\"name\":\"November 2020\"},\"202012\":{\"name\":\"December 2020\"},\"BmYyh9bZ0sr\":{\"name\":\"Kafe Simira\"},\"202010\":{\"name\":\"October 2020\"},\"pk7bUK5c1Uf\":{\"name\":\"Ya Kpukumu Krim\"},\"ERmBhYkhV6Y\":{\"name\":\"Njaluahun\"},\"daJPPxtIrQn\":{\"name\":\"Jaiama Bongor\"},\"r1RUyfVBkLp\":{\"name\":\"Sambaia Bendugu\"},\"NNE0YMCDZkO\":{\"name\":\"Yoni\"},\"jWSIbtKfURj\":{\"name\":\"Langrama\"},\"CF243RPvNY7\":{\"name\":\"Fiama\"},\"I4jWcnFmgEC\":{\"name\":\"Niawa Lenga\"},\"cM2BKSrj9F9\":{\"name\":\"Luawa\"},\"jPidqyo7cpF\":{\"name\":\"Bagruwa\"},\"g8DdBm7EmUt\":{\"name\":\"Sittia\"},\"ZiOVcrSjSYe\":{\"name\":\"Dibia\"},\"j43EZb15rjI\":{\"name\":\"Sella Limba\"},\"GSae40Fyppf\":{\"name\":\"Age at visit\"},\"SdOUI2yT46H\":{\"name\":\"5-14y\"},\"USER_ORGUNIT_GRANDCHILDREN\":{\"organisationUnits\":[\"nV3OkyzF4US\",\"r06ohri9wA9\",\"Z9QaI6sxTwW\",\"A3Fh37HWBWE\",\"DBs6e2Oxaj1\",\"sxRd2XOzFbz\",\"CG4QD1HC3h4\",\"j0Mtr3xTMjM\",\"YuQRtpLP10I\",\"QwMiPiME3bA\",\"iEkBZnMDarP\",\"KSdZwrU7Hh6\",\"g5ptsn0SFX8\",\"y5X4mP5XylL\",\"USQdmvrHh1Q\",\"KXSqt7jv6DU\",\"xGMGhjA3y6J\",\"yu4N82FFeLm\",\"vn9KJsLyP5f\",\"LsYpCyYxSLY\",\"EYt6ThQDagn\",\"npWGUj37qDe\",\"HWjrSuoNPte\",\"nlt6j60tCHF\",\"VCtF1DbspR5\",\"l7pFejMtUoF\",\"XEyIRFd9pct\",\"xhyjU2SVewz\",\"lYIM1MXbSYS\",\"pRHGAROvuyI\",\"NqWaKXcg01b\",\"BD9gU0GKlr2\",\"RzKeCma9qb1\",\"iUauWFeH8Qp\",\"ENHOJz3UH5L\",\"PrJQHI6q7w2\",\"HV8RTzgcFH3\",\"LfTkc0S4b5k\",\"NNE0YMCDZkO\",\"ARZ4y5i4reU\",\"iGHlidSFdpu\",\"DmaLM8WYmWv\",\"RWvG1aFrr0r\",\"QlCIp2S9NHs\",\"P69SId31eDp\",\"GWTIxJO9pRo\",\"M2qEv692lS6\",\"rXLor9Knq6l\",\"AovmOHadayb\",\"ajILkI0cfxn\",\"hjpHnHZIniP\",\"Qhmi8IZyPyD\",\"W5fN3G6y1VI\",\"GFk45MOxzJJ\",\"J4GiUImJZoE\",\"U09TSwIjG0s\",\"EjnIQNVAXGp\",\"JsxnA2IywRo\",\"Zoy23SSHCPs\",\"nOYt1LtFSyU\",\"vULnao2hV5v\",\"smoyi1iYNK6\",\"x4HaBHHwBML\",\"EVkm2xYcf6Z\",\"PaqugoqjRIj\",\"fwH9ipvXde9\",\"Lt8U7GVWvSR\",\"K1r3uF6eZ8n\",\"eV4cuxniZgP\",\"KIUCimTXf8Q\",\"hdEuw2ugkVF\",\"dGheVylzol6\",\"lY93YpCxJqf\",\"eROJsBwxQHt\",\"FRxcUEwktoV\",\"kvkDWg42lHR\",\"byp7w6Xd9Df\",\"vzup1f6ynON\",\"cM2BKSrj9F9\",\"l0ccv2yzfF3\",\"EfWCa0Cc8WW\",\"zSNUViKdkk3\",\"TQkG0sX9nca\",\"pmxZm7klXBy\",\"KctpIIucige\",\"C9uduqDZr9d\",\"XG8HGAbrbbL\",\"EB1zRKdYjdY\",\"gy8rmvYT4cj\",\"qgQ49DH9a0v\",\"hRZOIgQ0O1m\",\"daJPPxtIrQn\",\"pk7bUK5c1Uf\",\"qIRCo0MfuGb\",\"xIKjidMrico\",\"uKC54fzxRzO\",\"j43EZb15rjI\",\"TA7NvKjsn4A\",\"YpVol7asWvd\",\"BXJdOLvUrZB\",\"KKkLOTpMXGV\",\"YmmeuGbqOwR\",\"I4jWcnFmgEC\",\"fwxkctgmffZ\",\"jPidqyo7cpF\",\"r1RUyfVBkLp\",\"Mr4au3jR9bt\",\"U6Kr7Gtpidn\",\"EZPwuUTeIIG\",\"DfUfwjM9am5\",\"VGAFxBXz16y\",\"DxAPPqXvwLy\",\"QywkxFudXrC\",\"zFDYIgyGmXG\",\"qtr8GGlm4gg\",\"ERmBhYkhV6Y\",\"g8DdBm7EmUt\",\"CF243RPvNY7\",\"LhaAPLxdSFH\",\"N233eZJZ1bh\",\"JdhagCUEMbj\",\"WXnNDWTiE9r\",\"vWbkYPRmKyS\",\"XrF5AvaGcuw\",\"UhHipWG7J8b\",\"kbPmt60yi0L\",\"eNtRuQrrZeo\",\"Jiyc4ekaMMh\",\"L8iA6eLwKNb\",\"fRLX08WHWpL\",\"BmYyh9bZ0sr\",\"BGGmAwx33dj\",\"e1eIKM1GIF3\",\"bQiBfA2j5cw\",\"OTFepb1k9Db\",\"cgOy0hRMGu9\",\"FlBemv1NfEC\",\"RndxKqQGzUl\",\"vEvs2ckGNQj\",\"DNRAeXT9IwS\",\"aWQTfvgPA5v\",\"JdqfYTIFZXN\",\"myQ4q1W6B4y\",\"X7dWcGerQIm\",\"VP397wRvePm\",\"ZiOVcrSjSYe\",\"PQZJPIpTepd\",\"kU8vhUkAGaT\",\"Pc3JTyqnsmL\",\"GE25DpSrqpB\",\"d9iMR1MpuIO\",\"jWSIbtKfURj\"]},\"fwxkctgmffZ\":{\"name\":\"Kholifa Mabang\"},\"QwMiPiME3bA\":{\"name\":\"Kpanga Kabonde\"},\"eV4cuxniZgP\":{\"name\":\"Magbaimba Ndowahun\"},\"xhyjU2SVewz\":{\"name\":\"Tane\"},\"TA7NvKjsn4A\":{\"name\":\"Bureh Kasseh Maconteh\"},\"myQ4q1W6B4y\":{\"name\":\"Dama\"},\"wHBMVthqIX4\":{\"name\":\"12-59m\"},\"hdEuw2ugkVF\":{\"name\":\"Lower Bambara\"},\"Pc3JTyqnsmL\":{\"name\":\"Buya Romende\"},\"GE25DpSrqpB\":{\"name\":\"Malema\"},\"ARZ4y5i4reU\":{\"name\":\"Wonde\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"uKC54fzxRzO\":{\"name\":\"Niawa\"},\"PrJQHI6q7w2\":{\"name\":\"Tainkatopa Makama Safrokoh\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"xGMGhjA3y6J\":{\"name\":\"Mambolo\"},\"nOYt1LtFSyU\":{\"name\":\"Bumpeh\"},\"e1eIKM1GIF3\":{\"name\":\"Gbanti Kamaranka\"},\"lYIM1MXbSYS\":{\"name\":\"Dea\"},\"d9iMR1MpuIO\":{\"name\":\"Soro-Gbeima\"},\"U09TSwIjG0s\":{\"name\":\"Nomo\"},\"zFDYIgyGmXG\":{\"name\":\"Bargbo\"},\"dx\":{\"name\":\"Data\"},\"xIKjidMrico\":{\"name\":\"Kowa\"},\"pRHGAROvuyI\":{\"name\":\"Koya\"},\"GWTIxJO9pRo\":{\"name\":\"Gorama Kono\"},\"HWjrSuoNPte\":{\"name\":\"Sanda Magbolonthor\"},\"UhHipWG7J8b\":{\"name\":\"Sanda Tendaren\"},\"kU8vhUkAGaT\":{\"name\":\"Lugbu\"},\"rXLor9Knq6l\":{\"name\":\"Kunike Barina\"},\"iEkBZnMDarP\":{\"name\":\"Folosaba Dembelia\"},\"JsxnA2IywRo\":{\"name\":\"Kissi Kama\"},\"g5ptsn0SFX8\":{\"name\":\"Sandor\"},\"pmxZm7klXBy\":{\"name\":\"Peje West\"},\"YuQRtpLP10I\":{\"name\":\"Badjia\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"KSdZwrU7Hh6\":{\"name\":\"Jawi\"},\"ajILkI0cfxn\":{\"name\":\"Gbane\"},\"y5X4mP5XylL\":{\"name\":\"Tonko Limba\"},\"fwH9ipvXde9\":{\"name\":\"Biriwa\"},\"EjnIQNVAXGp\":{\"name\":\"Mafindor\"},\"x4HaBHHwBML\":{\"name\":\"Malegohun\"},\"EZPwuUTeIIG\":{\"name\":\"Wara Wara Yagala\"},\"Zoy23SSHCPs\":{\"name\":\"Gbane Kandor\"},\"fRLX08WHWpL\":{\"name\":\"Lokomasama\"},\"LsYpCyYxSLY\":{\"name\":\"Kamaje\"},\"JdqfYTIFZXN\":{\"name\":\"Maforki\"},\"EB1zRKdYjdY\":{\"name\":\"Bendu Cha\"},\"RUv0hqER0zV\":{\"name\":\"All other follow-ups\"},\"CG4QD1HC3h4\":{\"name\":\"Yawbeko\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"RndxKqQGzUl\":{\"name\":\"Dasse\"},\"YpVol7asWvd\":{\"name\":\"Kpanga Krim\"},\"Mr4au3jR9bt\":{\"name\":\"Dembelia Sinkunia\"},\"DmaLM8WYmWv\":{\"name\":\"Nimikoro\"},\"IpHINAT79UW.cejWyOfXge6\":{\"name\":\"Child Programme Gender\"},\"BD9gU0GKlr2\":{\"name\":\"Makpele\"},\"GFk45MOxzJJ\":{\"name\":\"Neya\"},\"A3Fh37HWBWE\":{\"name\":\"Simbaru\"},\"Lt8U7GVWvSR\":{\"name\":\"Diang\"},\"JdhagCUEMbj\":{\"name\":\"Komboya\"},\"PQZJPIpTepd\":{\"name\":\"Kholifa Rowalla\"},\"EVkm2xYcf6Z\":{\"name\":\"Malal Mara\"},\"WXnNDWTiE9r\":{\"name\":\"Sanda Loko\"},\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"DxAPPqXvwLy\":{\"name\":\"Peje Bongre\"},\"eROJsBwxQHt\":{\"name\":\"Gaura\"},\"PaqugoqjRIj\":{\"name\":\"Sulima (Koinadugu)\"},\"gy8rmvYT4cj\":{\"name\":\"Ribbi\"},\"RzKeCma9qb1\":{\"name\":\"Barri\"},\"EfWCa0Cc8WW\":{\"name\":\"Masimera\"},\"AovmOHadayb\":{\"name\":\"Timidale\"},\"Jiyc4ekaMMh\":{\"name\":\"Kongbora\"},\"FlBemv1NfEC\":{\"name\":\"Masungbala\"},\"XrF5AvaGcuw\":{\"name\":\"Wara Wara Bafodia\"},\"LhaAPLxdSFH\":{\"name\":\"Lei\"},\"zSNUViKdkk3\":{\"name\":\"Kpaka\"},\"r06ohri9wA9\":{\"name\":\"Samu\"},\"Z9QaI6sxTwW\":{\"name\":\"Kargboro\"},\"W5fN3G6y1VI\":{\"name\":\"Lower Banta\"},\"ENHOJz3UH5L\":{\"name\":\"BMC\"},\"QywkxFudXrC\":{\"name\":\"Magbema\"},\"J4GiUImJZoE\":{\"name\":\"Nieni\"},\"BGGmAwx33dj\":{\"name\":\"Bumpe Ngao\"},\"kvkDWg42lHR\":{\"name\":\"Kamara\"},\"iGHlidSFdpu\":{\"name\":\"Soa\"},\"vn9KJsLyP5f\":{\"name\":\"Kaffu Bullom\"},\"QlCIp2S9NHs\":{\"name\":\"Dodo\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"jOkIbJVhECg\":{\"name\":\"15y+\"},\"bQiBfA2j5cw\":{\"name\":\"Penguia\"},\"NqWaKXcg01b\":{\"name\":\"Sowa\"},\"VP397wRvePm\":{\"name\":\"Nongoba Bullum\"},\"dSBYyCUjCXd\":{\"name\":\"Age at visit - calc from days\"},\"Qhmi8IZyPyD\":{\"name\":\"Tambaka\"},\"OTFepb1k9Db\":{\"name\":\"Mongo\"},\"DBs6e2Oxaj1\":{\"name\":\"Upper Banta\"},\"vWbkYPRmKyS\":{\"name\":\"Baoma\"},\"dGheVylzol6\":{\"name\":\"Bargbe\"},\"npWGUj37qDe\":{\"name\":\"Valunia\"},\"nV3OkyzF4US\":{\"name\":\"Kori\"},\"X7dWcGerQIm\":{\"name\":\"Wandor\"},\"kbPmt60yi0L\":{\"name\":\"Bramaia\"},\"qIRCo0MfuGb\":{\"name\":\"Gbinleh Dixion\"},\"eNtRuQrrZeo\":{\"name\":\"Galliness Perri\"},\"HV8RTzgcFH3\":{\"name\":\"Kwamabai Krim\"},\"KKkLOTpMXGV\":{\"name\":\"Bombali Sebora\"},\"l7pFejMtUoF\":{\"name\":\"Tunkia\"},\"202201\":{\"name\":\"January 2022\"},\"K1r3uF6eZ8n\":{\"name\":\"Kandu Lepiema\"},\"VGAFxBXz16y\":{\"name\":\"Sengbeh\"},\"byp7w6Xd9Df\":{\"name\":\"Yawei\"},\"BXJdOLvUrZB\":{\"name\":\"Gbendembu Ngowahun\"},\"hRZOIgQ0O1m\":{\"name\":\"Libeisaygahun\"},\"TQkG0sX9nca\":{\"name\":\"Gbense\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"USQdmvrHh1Q\":{\"name\":\"Kaiyamba\"},\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"202208\":{\"name\":\"August 2022\"},\"sxRd2XOzFbz\":{\"name\":\"Tikonko\"},\"202209\":{\"name\":\"September 2022\"},\"202206\":{\"name\":\"June 2022\"},\"202207\":{\"name\":\"July 2022\"},\"202204\":{\"name\":\"April 2022\"},\"202205\":{\"name\":\"May 2022\"},\"202202\":{\"name\":\"February 2022\"},\"202203\":{\"name\":\"March 2022\"},\"202211\":{\"name\":\"November 2022\"},\"qgQ49DH9a0v\":{\"name\":\"Nimiyama\"},\"202212\":{\"name\":\"December 2022\"},\"202210\":{\"name\":\"October 2022\"},\"M2qEv692lS6\":{\"name\":\"Tankoro\"},\"FQ2o8UBlcrS\":{\"name\":\"Acute Flaccid Paralysis (AFP) new\"},\"qtr8GGlm4gg\":{\"name\":\"Rural Western Area\"},\"C9uduqDZr9d\":{\"name\":\"Freetown\"},\"YmmeuGbqOwR\":{\"name\":\"Gbo\"},\"nlt6j60tCHF\":{\"name\":\"Mano Sakrim\"},\"XG8HGAbrbbL\":{\"name\":\"Safroko Limba\"},\"l0ccv2yzfF3\":{\"name\":\"Kunike\"},\"USER_ORGUNIT_CHILDREN\":{\"organisationUnits\":[\"at6UHUQatSo\",\"TEQlaapDQoK\",\"PMa2VCrupOd\",\"qhqAxPSTUXp\",\"kJq2mPyFEHo\",\"jmIPBj66vD6\",\"Vth0fbpFcsO\",\"jUb8gELQApl\",\"fdc6uOvgoji\",\"eIQbndfxQMb\",\"O6uvpzGd5pu\",\"lc3eMKXaEfw\",\"bL4ooGhyHRQ\"]},\"LfTkc0S4b5k\":{\"name\":\"Upper Bambara\"},\"202101\":{\"name\":\"January 2021\"},\"202102\":{\"name\":\"February 2021\"},\"KIUCimTXf8Q\":{\"name\":\"Nongowa\"},\"vEvs2ckGNQj\":{\"name\":\"Kasonko\"},\"XEyIRFd9pct\":{\"name\":\"Imperi\"},\"cgOy0hRMGu9\":{\"name\":\"Sogbini\"},\"N233eZJZ1bh\":{\"name\":\"Pejeh\"},\"smoyi1iYNK6\":{\"name\":\"Kalansogoia\"},\"DNRAeXT9IwS\":{\"name\":\"Dema\"},\"202109\":{\"name\":\"September 2021\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"202112\":{\"name\":\"December 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"P69SId31eDp\":{\"name\":\"Gbonkonlenken\"},\"aWQTfvgPA5v\":{\"name\":\"Kpanda Kemoh\"},\"KctpIIucige\":{\"name\":\"Selenga\"},\"S34ULMcHMca\":{\"name\":\"0-11m\"},\"L8iA6eLwKNb\":{\"name\":\"Paki Masabong\"},\"j0Mtr3xTMjM\":{\"name\":\"Kissi Teng\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"DfUfwjM9am5\":{\"name\":\"Malen\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"FRxcUEwktoV\":{\"name\":\"Toli\"},\"ou\":{\"name\":\"Organisation unit\"},\"VCtF1DbspR5\":{\"name\":\"Jong\"},\"IpHINAT79UW.wQLfBvPrXqq\":{\"name\":\"Child Programme MCH ARV at birth\"},\"202002\":{\"name\":\"February 2020\"},\"202003\":{\"name\":\"March 2020\"},\"202001\":{\"name\":\"January 2020\"},\"vzup1f6ynON\":{\"name\":\"Small Bo\"},\"pe\":{\"name\":\"Period\"},\"RWvG1aFrr0r\":{\"name\":\"Marampa\"}},\"dimensions\":{\"dx\":[\"FQ2o8UBlcrS\",\"GSae40Fyppf\",\"dSBYyCUjCXd\",\"RUv0hqER0zV\",\"IpHINAT79UW.cejWyOfXge6\",\"IpHINAT79UW.wQLfBvPrXqq\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\",\"202201\",\"202202\",\"202203\",\"202204\",\"202205\",\"202206\",\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202001\",\"202002\",\"202003\",\"202004\",\"202005\",\"202006\",\"202007\",\"202008\",\"202009\",\"202010\",\"202011\",\"202012\"],\"ou\":[\"ImspTQPwCqd\",\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\",\"YuQRtpLP10I\",\"vWbkYPRmKyS\",\"dGheVylzol6\",\"zFDYIgyGmXG\",\"BGGmAwx33dj\",\"YmmeuGbqOwR\",\"daJPPxtIrQn\",\"U6Kr7Gtpidn\",\"JdhagCUEMbj\",\"kU8vhUkAGaT\",\"I4jWcnFmgEC\",\"KctpIIucige\",\"sxRd2XOzFbz\",\"npWGUj37qDe\",\"ARZ4y5i4reU\",\"fwH9ipvXde9\",\"KKkLOTpMXGV\",\"e1eIKM1GIF3\",\"BXJdOLvUrZB\",\"hRZOIgQ0O1m\",\"eV4cuxniZgP\",\"lY93YpCxJqf\",\"L8iA6eLwKNb\",\"XG8HGAbrbbL\",\"WXnNDWTiE9r\",\"UhHipWG7J8b\",\"j43EZb15rjI\",\"Qhmi8IZyPyD\",\"ENHOJz3UH5L\",\"EB1zRKdYjdY\",\"iUauWFeH8Qp\",\"DNRAeXT9IwS\",\"XEyIRFd9pct\",\"VCtF1DbspR5\",\"aWQTfvgPA5v\",\"HV8RTzgcFH3\",\"VP397wRvePm\",\"g8DdBm7EmUt\",\"cgOy0hRMGu9\",\"CG4QD1HC3h4\",\"lYIM1MXbSYS\",\"KSdZwrU7Hh6\",\"JsxnA2IywRo\",\"j0Mtr3xTMjM\",\"hjpHnHZIniP\",\"cM2BKSrj9F9\",\"GE25DpSrqpB\",\"yu4N82FFeLm\",\"ERmBhYkhV6Y\",\"DxAPPqXvwLy\",\"pmxZm7klXBy\",\"bQiBfA2j5cw\",\"LfTkc0S4b5k\",\"byp7w6Xd9Df\",\"kbPmt60yi0L\",\"qIRCo0MfuGb\",\"QywkxFudXrC\",\"xGMGhjA3y6J\",\"FlBemv1NfEC\",\"r06ohri9wA9\",\"y5X4mP5XylL\",\"myQ4q1W6B4y\",\"QlCIp2S9NHs\",\"eROJsBwxQHt\",\"KXSqt7jv6DU\",\"K1r3uF6eZ8n\",\"EYt6ThQDagn\",\"jWSIbtKfURj\",\"hdEuw2ugkVF\",\"x4HaBHHwBML\",\"uKC54fzxRzO\",\"U09TSwIjG0s\",\"KIUCimTXf8Q\",\"A3Fh37HWBWE\",\"vzup1f6ynON\",\"l7pFejMtUoF\",\"X7dWcGerQIm\",\"Mr4au3jR9bt\",\"Lt8U7GVWvSR\",\"iEkBZnMDarP\",\"vEvs2ckGNQj\",\"OTFepb1k9Db\",\"GFk45MOxzJJ\",\"J4GiUImJZoE\",\"VGAFxBXz16y\",\"PaqugoqjRIj\",\"XrF5AvaGcuw\",\"EZPwuUTeIIG\",\"CF243RPvNY7\",\"ajILkI0cfxn\",\"Zoy23SSHCPs\",\"TQkG0sX9nca\",\"GWTIxJO9pRo\",\"kvkDWg42lHR\",\"LhaAPLxdSFH\",\"EjnIQNVAXGp\",\"DmaLM8WYmWv\",\"qgQ49DH9a0v\",\"g5ptsn0SFX8\",\"iGHlidSFdpu\",\"M2qEv692lS6\",\"FRxcUEwktoV\",\"jPidqyo7cpF\",\"nOYt1LtFSyU\",\"RndxKqQGzUl\",\"vULnao2hV5v\",\"USQdmvrHh1Q\",\"LsYpCyYxSLY\",\"Z9QaI6sxTwW\",\"Jiyc4ekaMMh\",\"nV3OkyzF4US\",\"xIKjidMrico\",\"W5fN3G6y1VI\",\"gy8rmvYT4cj\",\"AovmOHadayb\",\"DBs6e2Oxaj1\",\"TA7NvKjsn4A\",\"Pc3JTyqnsmL\",\"ZiOVcrSjSYe\",\"vn9KJsLyP5f\",\"pRHGAROvuyI\",\"fRLX08WHWpL\",\"JdqfYTIFZXN\",\"RWvG1aFrr0r\",\"EfWCa0Cc8WW\",\"HWjrSuoNPte\",\"PrJQHI6q7w2\",\"RzKeCma9qb1\",\"eNtRuQrrZeo\",\"zSNUViKdkk3\",\"QwMiPiME3bA\",\"YpVol7asWvd\",\"BD9gU0GKlr2\",\"DfUfwjM9am5\",\"nlt6j60tCHF\",\"N233eZJZ1bh\",\"d9iMR1MpuIO\",\"NqWaKXcg01b\",\"pk7bUK5c1Uf\",\"P69SId31eDp\",\"BmYyh9bZ0sr\",\"smoyi1iYNK6\",\"fwxkctgmffZ\",\"PQZJPIpTepd\",\"l0ccv2yzfF3\",\"rXLor9Knq6l\",\"EVkm2xYcf6Z\",\"r1RUyfVBkLp\",\"xhyjU2SVewz\",\"NNE0YMCDZkO\",\"C9uduqDZr9d\",\"qtr8GGlm4gg\"],\"co\":[\"S34ULMcHMca\",\"SdOUI2yT46H\",\"wHBMVthqIX4\",\"jOkIbJVhECg\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "Data ID", "dataid", "TEXT", "java.lang.String", true, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "Data", "dataname", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "Data code", "datacode", "TEXT", "java.lang.String", true, true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Data description",
        "datadescription",
        "TEXT",
        "java.lang.String",
        true,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Organisation unit ID",
        "organisationunitid",
        "TEXT",
        "java.lang.String",
        true,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Organisation unit",
        "organisationunitname",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Organisation unit code",
        "organisationunitcode",
        "TEXT",
        "java.lang.String",
        true,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "Organisation unit description",
        "organisationunitdescription",
        "TEXT",
        "java.lang.String",
        true,
        true);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "january 2021",
        "January 2021",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "february 2021",
        "February 2021",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "march 2021",
        "March 2021",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "april 2021",
        "April 2021",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "may 2021",
        "May 2021",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "june 2021",
        "June 2021",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "july 2021",
        "July 2021",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "august 2021",
        "August 2021",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "september 2021",
        "September 2021",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "october 2021",
        "October 2021",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "november 2021",
        "November 2021",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "december 2021",
        "December 2021",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "january 2022",
        "January 2022",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "february 2022",
        "February 2022",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "march 2022",
        "March 2022",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "april 2022",
        "April 2022",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "may 2022",
        "May 2022",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "june 2022",
        "June 2022",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "july 2022",
        "July 2022",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "august 2022",
        "August 2022",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "september 2022",
        "September 2022",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "october 2022",
        "October 2022",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "november 2022",
        "November 2022",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "december 2022",
        "December 2022",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "january 2020",
        "January 2020",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "february 2020",
        "February 2020",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "march 2020",
        "March 2020",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "april 2020",
        "April 2020",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "may 2020",
        "May 2020",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "june 2020",
        "June 2020",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "july 2020",
        "July 2020",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "august 2020",
        "August 2020",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "september 2020",
        "September 2020",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "october 2020",
        "October 2020",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "november 2020",
        "November 2020",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "december 2020",
        "December 2020",
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

    // 7. Assert row values by name (sample validation: first/last row, key columns).
    // Validate selected values for row index 0
    validateRowValueByName(response, actualHeaders, 0, "Data ID", "FQ2o8UBlcrS");
    validateRowValueByName(response, actualHeaders, 0, "december 2020", "");

    // Validate selected values for row index 995
    validateRowValueByName(response, actualHeaders, 995, "Data ID", "IpHINAT79UW.wQLfBvPrXqq");
    validateRowValueByName(response, actualHeaders, 995, "december 2020", "");
  }

  @DependsOn(
      files = {"ind-period-offset.json"},
      delete = true)
  @Test
  public void periodOffsetIndicatorAsFilter(List<Resource> resource) throws JSONException {
    // Test for ISSUE DHIS2-18502
    String indicatorId = resource.get(0).uid();
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "filter=ou:USER_ORGUNIT,pe:202101;202102;202103;202104;202105;202106;202107;202108;202109;202110;202111;202112")
            .add("skipData=false")
            .add("includeMetadataDetails=true")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:%s".formatted(indicatorId));

    // When
    ApiResponse response = actions.get(params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime
    // 'expectPostgis' flag.
    validateResponseStructure(
        response, false, 1, 7, 7); // Pass runtime flag, row count, and expected header counts

    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders =
        response.extractList("headers", Map.class).stream()
            .map(obj -> (Map<String, Object>) obj) // Ensure correct type
            .collect(Collectors.toList());

    // 3. Assert metaData.
    String expectedMetaData =
        """
                {
                   "dimensions": {
                      "co": [],
                      "dx": [
                         "%s"
                      ],
                      "ou": [
                         "ImspTQPwCqd"
                      ],
                      "pe": [
                         "202101",
                         "202102",
                         "202103",
                         "202104",
                         "202105",
                         "202106",
                         "202107",
                         "202108",
                         "202109",
                         "202110",
                         "202111",
                         "202112"
                      ]
                   },
                   "items": {
                      "202101": {
                         "code": "202101",
                         "dimensionItemType": "PERIOD",
                         "endDate": "2021-01-31T00:00:00.000",
                         "name": "January 2021",
                         "startDate": "2021-01-01T00:00:00.000",
                         "totalAggregationType": "SUM",
                         "uid": "202101",
                         "valueType": "TEXT"
                      },
                      "202102": {
                         "code": "202102",
                         "dimensionItemType": "PERIOD",
                         "endDate": "2021-02-28T00:00:00.000",
                         "name": "February 2021",
                         "startDate": "2021-02-01T00:00:00.000",
                         "totalAggregationType": "SUM",
                         "uid": "202102",
                         "valueType": "TEXT"
                      },
                      "202103": {
                         "code": "202103",
                         "dimensionItemType": "PERIOD",
                         "endDate": "2021-03-31T00:00:00.000",
                         "name": "March 2021",
                         "startDate": "2021-03-01T00:00:00.000",
                         "totalAggregationType": "SUM",
                         "uid": "202103",
                         "valueType": "TEXT"
                      },
                      "202104": {
                         "code": "202104",
                         "dimensionItemType": "PERIOD",
                         "endDate": "2021-04-30T00:00:00.000",
                         "name": "April 2021",
                         "startDate": "2021-04-01T00:00:00.000",
                         "totalAggregationType": "SUM",
                         "uid": "202104",
                         "valueType": "TEXT"
                      },
                      "202105": {
                         "code": "202105",
                         "dimensionItemType": "PERIOD",
                         "endDate": "2021-05-31T00:00:00.000",
                         "name": "May 2021",
                         "startDate": "2021-05-01T00:00:00.000",
                         "totalAggregationType": "SUM",
                         "uid": "202105",
                         "valueType": "TEXT"
                      },
                      "202106": {
                         "code": "202106",
                         "dimensionItemType": "PERIOD",
                         "endDate": "2021-06-30T00:00:00.000",
                         "name": "June 2021",
                         "startDate": "2021-06-01T00:00:00.000",
                         "totalAggregationType": "SUM",
                         "uid": "202106",
                         "valueType": "TEXT"
                      },
                      "202107": {
                         "code": "202107",
                         "dimensionItemType": "PERIOD",
                         "endDate": "2021-07-31T00:00:00.000",
                         "name": "July 2021",
                         "startDate": "2021-07-01T00:00:00.000",
                         "totalAggregationType": "SUM",
                         "uid": "202107",
                         "valueType": "TEXT"
                      },
                      "202108": {
                         "code": "202108",
                         "dimensionItemType": "PERIOD",
                         "endDate": "2021-08-31T00:00:00.000",
                         "name": "August 2021",
                         "startDate": "2021-08-01T00:00:00.000",
                         "totalAggregationType": "SUM",
                         "uid": "202108",
                         "valueType": "TEXT"
                      },
                      "202109": {
                         "code": "202109",
                         "dimensionItemType": "PERIOD",
                         "endDate": "2021-09-30T00:00:00.000",
                         "name": "September 2021",
                         "startDate": "2021-09-01T00:00:00.000",
                         "totalAggregationType": "SUM",
                         "uid": "202109",
                         "valueType": "TEXT"
                      },
                      "202110": {
                         "code": "202110",
                         "dimensionItemType": "PERIOD",
                         "endDate": "2021-10-31T00:00:00.000",
                         "name": "October 2021",
                         "startDate": "2021-10-01T00:00:00.000",
                         "totalAggregationType": "SUM",
                         "uid": "202110",
                         "valueType": "TEXT"
                      },
                      "202111": {
                         "code": "202111",
                         "dimensionItemType": "PERIOD",
                         "endDate": "2021-11-30T00:00:00.000",
                         "name": "November 2021",
                         "startDate": "2021-11-01T00:00:00.000",
                         "totalAggregationType": "SUM",
                         "uid": "202111",
                         "valueType": "TEXT"
                      },
                      "202112": {
                         "code": "202112",
                         "dimensionItemType": "PERIOD",
                         "endDate": "2021-12-31T00:00:00.000",
                         "name": "December 2021",
                         "startDate": "2021-12-01T00:00:00.000",
                         "totalAggregationType": "SUM",
                         "uid": "202112",
                         "valueType": "TEXT"
                      },
                      "%s": {
                         "code": "DHIS2-18502",
                         "dimensionItemType": "INDICATOR",
                         "indicatorType": {
                            "displayName": "Number (Factor 1)",
                            "factor": 1,
                            "name": "Number (Factor 1)",
                            "number": true
                         },
                         "name": "PERIOD_OFFSET_TEST",
                         "totalAggregationType": "SUM",
                         "uid": "%s",
                         "valueType": "NUMBER"
                      },
                      "dx": {
                         "dimensionType": "DATA_X",
                         "name": "Data",
                         "uid": "dx"
                      },
                      "ImspTQPwCqd": {
                         "code": "OU_525",
                         "dimensionItemType": "ORGANISATION_UNIT",
                         "name": "Sierra Leone",
                         "totalAggregationType": "SUM",
                         "uid": "ImspTQPwCqd",
                         "valueType": "TEXT"
                      },
                      "ou": {
                         "dimensionType": "ORGANISATION_UNIT",
                         "name": "Organisation unit",
                         "uid": "ou"
                      },
                      "pe": {
                         "dimensionType": "PERIOD",
                         "name": "Period",
                         "uid": "pe"
                      }
                   }
                }
                """
            .formatted(indicatorId, indicatorId, indicatorId);
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
    validateHeaderPropertiesByName(
        response, actualHeaders, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeaderPropertiesByName(
        response, actualHeaders, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "numerator",
        "Numerator",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "denominator",
        "Denominator",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response, actualHeaders, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeaderPropertiesByName(
        response,
        actualHeaders,
        "multiplier",
        "Multiplier",
        "NUMBER",
        "java.lang.Double",
        false,
        false);
    validateHeaderPropertiesByName(
        response, actualHeaders, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    validateRowValueByName(response, actualHeaders, 0, "dx", indicatorId);
    validateRowValueByName(response, actualHeaders, 0, "divisor", "1");
  }
}
