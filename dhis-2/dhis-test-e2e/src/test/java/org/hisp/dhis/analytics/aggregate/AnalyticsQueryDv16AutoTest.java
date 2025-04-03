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
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.Map;
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
}
