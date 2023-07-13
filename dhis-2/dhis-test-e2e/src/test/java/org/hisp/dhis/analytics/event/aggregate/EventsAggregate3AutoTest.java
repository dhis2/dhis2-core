/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.analytics.event.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/aggregate" endpoint. */
public class EventsAggregate3AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void queryGenderAndModeOfDischargeByAdmissionDate() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("timeField=eMyVanycQSC")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:jUb8gELQApl;TEQlaapDQoK;eIQbndfxQMb;Vth0fbpFcsO;PMa2VCrupOd;O6uvpzGd5pu;bL4ooGhyHRQ;kJq2mPyFEHo;fdc6uOvgoji;at6UHUQatSo;lc3eMKXaEfw;qhqAxPSTUXp;jmIPBj66vD6,Zj7UnCAulEk.oZg33kd9taw,Zj7UnCAulEk.fWIAEtYVEGk,pe:LAST_4_QUARTERS")
            .add("relativePeriodDate=2020-01-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"gj2fKKyp8OH\":{\"code\":\"MODDIED\",\"name\":\"Died\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"LAST_4_QUARTERS\":{\"name\":\"Last 4 quarters\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"2019Q4\":{\"name\":\"October - December 2019\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"2019Q3\":{\"name\":\"July - September 2019\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"Fhbf4aKpZmZ\":{\"code\":\"MODABSC\",\"name\":\"Absconded\"},\"fWIAEtYVEGk\":{\"name\":\"Mode of Discharge\"},\"2019Q2\":{\"name\":\"April - June 2019\"},\"2019Q1\":{\"name\":\"January - March 2019\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"pe\":{\"name\":\"Period\"},\"oZg33kd9taw\":{\"name\":\"Gender\"},\"fShHdgT7XGb\":{\"code\":\"MODTRANS\",\"name\":\"Transferred\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"yeod5tOXpkP\":{\"code\":\"MODDISCH\",\"name\":\"Discharged\"}},\"dimensions\":{\"pe\":[\"2019Q1\",\"2019Q2\",\"2019Q3\",\"2019Q4\"],\"ou\":[\"jUb8gELQApl\",\"TEQlaapDQoK\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"PMa2VCrupOd\",\"O6uvpzGd5pu\",\"bL4ooGhyHRQ\",\"kJq2mPyFEHo\",\"fdc6uOvgoji\",\"at6UHUQatSo\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"jmIPBj66vD6\"],\"oZg33kd9taw\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"fWIAEtYVEGk\":[\"yeod5tOXpkP\",\"gj2fKKyp8OH\",\"fShHdgT7XGb\",\"Fhbf4aKpZmZ\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "oZg33kd9taw", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "fWIAEtYVEGk", "Mode of Discharge", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "2019Q3", "9"));
    validateRow(response, 1, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "2019Q3", "4"));
    validateRow(response, 2, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "2019Q1", "1"));
    validateRow(response, 3, List.of("Male", "MODABSC", "O6uvpzGd5pu", "2019Q3", "1"));
    validateRow(response, 4, List.of("", "MODTRANS", "O6uvpzGd5pu", "2019Q3", "1"));
  }

  @Test
  public void queryGenderAndModeOfDischargeByDischargeDate() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("timeField=msodh3rEMJa")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:O6uvpzGd5pu;fdc6uOvgoji,pe:LAST_4_QUARTERS,Zj7UnCAulEk.oZg33kd9taw,Zj7UnCAulEk.fWIAEtYVEGk")
            .add("relativePeriodDate=2020-06-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(4)))
        .body("height", equalTo(4))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"2019Q4\":{\"name\":\"October - December 2019\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"2019Q3\":{\"name\":\"July - September 2019\"},\"ou\":{\"name\":\"Organisation unit\"},\"Fhbf4aKpZmZ\":{\"code\":\"MODABSC\",\"name\":\"Absconded\"},\"fWIAEtYVEGk\":{\"name\":\"Mode of Discharge\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"2019Q2\":{\"name\":\"April - June 2019\"},\"gj2fKKyp8OH\":{\"code\":\"MODDIED\",\"name\":\"Died\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"2020Q1\":{\"name\":\"January - March 2020\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"pe\":{\"name\":\"Period\"},\"LAST_4_QUARTERS\":{\"name\":\"Last 4 quarters\"},\"oZg33kd9taw\":{\"name\":\"Gender\"},\"fShHdgT7XGb\":{\"code\":\"MODTRANS\",\"name\":\"Transferred\"},\"yeod5tOXpkP\":{\"code\":\"MODDISCH\",\"name\":\"Discharged\"}},\"dimensions\":{\"pe\":[\"2019Q2\",\"2019Q3\",\"2019Q4\",\"2020Q1\"],\"ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\"],\"oZg33kd9taw\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"fWIAEtYVEGk\":[\"yeod5tOXpkP\",\"gj2fKKyp8OH\",\"fShHdgT7XGb\",\"Fhbf4aKpZmZ\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "oZg33kd9taw", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "fWIAEtYVEGk", "Mode of Discharge", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "2019Q3", "9"));
    validateRow(response, 1, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "2019Q3", "4"));
    validateRow(response, 2, List.of("Male", "MODABSC", "O6uvpzGd5pu", "2019Q3", "1"));
    validateRow(response, 3, List.of("", "MODTRANS", "O6uvpzGd5pu", "2019Q3", "1"));
  }

  @Test
  public void queryGenderAndModeOfDischargeByDistrictsLast12Monthsagg() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=pe:LAST_12_MONTHS,ou:TEQlaapDQoK;jUb8gELQApl;eIQbndfxQMb;Vth0fbpFcsO;PMa2VCrupOd;bL4ooGhyHRQ;O6uvpzGd5pu;kJq2mPyFEHo;fdc6uOvgoji;at6UHUQatSo;lc3eMKXaEfw;jmIPBj66vD6;qhqAxPSTUXp,Zj7UnCAulEk.oZg33kd9taw,Zj7UnCAulEk.fWIAEtYVEGk")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(625)))
        .body("height", equalTo(625))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"202208\":{\"name\":\"August 2022\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"202209\":{\"name\":\"September 2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202305\":{\"name\":\"May 2023\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"gj2fKKyp8OH\":{\"code\":\"MODDIED\",\"name\":\"Died\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"202210\":{\"name\":\"October 2022\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"Fhbf4aKpZmZ\":{\"code\":\"MODABSC\",\"name\":\"Absconded\"},\"fWIAEtYVEGk\":{\"name\":\"Mode of Discharge\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"pe\":{\"name\":\"Period\"},\"oZg33kd9taw\":{\"name\":\"Gender\"},\"fShHdgT7XGb\":{\"code\":\"MODTRANS\",\"name\":\"Transferred\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"yeod5tOXpkP\":{\"code\":\"MODDISCH\",\"name\":\"Discharged\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"ou\":[\"TEQlaapDQoK\",\"jUb8gELQApl\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"PMa2VCrupOd\",\"bL4ooGhyHRQ\",\"O6uvpzGd5pu\",\"kJq2mPyFEHo\",\"fdc6uOvgoji\",\"at6UHUQatSo\",\"lc3eMKXaEfw\",\"jmIPBj66vD6\",\"qhqAxPSTUXp\"],\"oZg33kd9taw\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"fWIAEtYVEGk\":[\"yeod5tOXpkP\",\"gj2fKKyp8OH\",\"fShHdgT7XGb\",\"Fhbf4aKpZmZ\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "oZg33kd9taw", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "fWIAEtYVEGk", "Mode of Discharge", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("Male", "MODDIED", "kJq2mPyFEHo", "202212", "75"));
    validateRow(response, 1, List.of("Male", "MODABSC", "O6uvpzGd5pu", "202209", "74"));
    validateRow(response, 2, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "202208", "73"));
    validateRow(response, 3, List.of("Male", "MODABSC", "O6uvpzGd5pu", "202211", "72"));
    validateRow(response, 4, List.of("Male", "MODDIED", "kJq2mPyFEHo", "202210", "71"));
    validateRow(response, 5, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "202210", "71"));
    validateRow(response, 6, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "202209", "70"));
    validateRow(response, 7, List.of("Male", "MODDIED", "kJq2mPyFEHo", "202208", "69"));
    validateRow(response, 8, List.of("Male", "MODABSC", "fdc6uOvgoji", "202208", "68"));
    validateRow(response, 9, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "202210", "68"));
    validateRow(response, 10, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "202208", "68"));
    validateRow(response, 11, List.of("Male", "MODABSC", "kJq2mPyFEHo", "202210", "68"));
    validateRow(response, 12, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "202207", "68"));
    validateRow(response, 13, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "202209", "67"));
    validateRow(response, 14, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "202209", "67"));
    validateRow(response, 15, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "202211", "65"));
    validateRow(response, 16, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "202211", "65"));
    validateRow(response, 17, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "202211", "65"));
    validateRow(response, 18, List.of("Male", "MODTRANS", "TEQlaapDQoK", "202211", "65"));
    validateRow(response, 19, List.of("Male", "MODDIED", "O6uvpzGd5pu", "202207", "65"));
    validateRow(response, 20, List.of("Male", "MODDIED", "TEQlaapDQoK", "202209", "65"));
    validateRow(response, 21, List.of("Male", "MODDISCH", "jmIPBj66vD6", "202207", "65"));
    validateRow(response, 22, List.of("Male", "MODDIED", "fdc6uOvgoji", "202211", "65"));
    validateRow(response, 23, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "202207", "64"));
    validateRow(response, 24, List.of("Male", "MODTRANS", "fdc6uOvgoji", "202207", "64"));
    validateRow(response, 25, List.of("Male", "MODABSC", "TEQlaapDQoK", "202208", "64"));
    validateRow(response, 26, List.of("Male", "MODABSC", "TEQlaapDQoK", "202212", "64"));
    validateRow(response, 27, List.of("Male", "MODABSC", "O6uvpzGd5pu", "202208", "64"));
    validateRow(response, 28, List.of("Male", "MODABSC", "fdc6uOvgoji", "202210", "63"));
    validateRow(response, 29, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "202212", "63"));
    validateRow(response, 30, List.of("Male", "MODABSC", "TEQlaapDQoK", "202209", "63"));
    validateRow(response, 31, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "202207", "63"));
    validateRow(response, 32, List.of("Male", "MODDISCH", "eIQbndfxQMb", "202211", "63"));
    validateRow(response, 33, List.of("Male", "MODDIED", "TEQlaapDQoK", "202212", "63"));
    validateRow(response, 34, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "202211", "63"));
    validateRow(response, 35, List.of("Male", "MODDISCH", "fdc6uOvgoji", "202210", "63"));
    validateRow(response, 36, List.of("Male", "MODABSC", "O6uvpzGd5pu", "202210", "62"));
    validateRow(response, 37, List.of("Male", "MODDISCH", "jmIPBj66vD6", "202211", "62"));
    validateRow(response, 38, List.of("Male", "MODABSC", "at6UHUQatSo", "202209", "62"));
    validateRow(response, 39, List.of("Male", "MODABSC", "kJq2mPyFEHo", "202212", "62"));
    validateRow(response, 40, List.of("Male", "MODTRANS", "at6UHUQatSo", "202209", "62"));
    validateRow(response, 41, List.of("Male", "MODABSC", "kJq2mPyFEHo", "202208", "62"));
    validateRow(response, 42, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "202210", "62"));
    validateRow(response, 43, List.of("Male", "MODDIED", "O6uvpzGd5pu", "202210", "61"));
    validateRow(response, 44, List.of("Male", "MODABSC", "O6uvpzGd5pu", "202207", "61"));
    validateRow(response, 45, List.of("Male", "MODDIED", "TEQlaapDQoK", "202207", "61"));
    validateRow(response, 46, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "202210", "60"));
    validateRow(response, 47, List.of("Male", "MODDIED", "kJq2mPyFEHo", "202209", "60"));
    validateRow(response, 48, List.of("Male", "MODDIED", "O6uvpzGd5pu", "202209", "60"));
    validateRow(response, 49, List.of("Male", "MODTRANS", "jmIPBj66vD6", "202209", "60"));
    validateRow(response, 50, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "202208", "60"));
    validateRow(response, 51, List.of("Male", "MODDIED", "O6uvpzGd5pu", "202211", "60"));
    validateRow(response, 52, List.of("Male", "MODDIED", "O6uvpzGd5pu", "202212", "59"));
    validateRow(response, 53, List.of("Male", "MODTRANS", "TEQlaapDQoK", "202209", "59"));
    validateRow(response, 54, List.of("Male", "MODABSC", "kJq2mPyFEHo", "202211", "59"));
    validateRow(response, 55, List.of("Male", "MODABSC", "at6UHUQatSo", "202211", "59"));
    validateRow(response, 56, List.of("Male", "MODABSC", "at6UHUQatSo", "202207", "59"));
    validateRow(response, 57, List.of("Male", "MODDIED", "kJq2mPyFEHo", "202207", "59"));
    validateRow(response, 58, List.of("Male", "MODABSC", "O6uvpzGd5pu", "202212", "59"));
    validateRow(response, 59, List.of("Male", "MODABSC", "TEQlaapDQoK", "202210", "58"));
    validateRow(response, 60, List.of("Male", "MODABSC", "TEQlaapDQoK", "202207", "58"));
    validateRow(response, 61, List.of("Male", "MODDIED", "TEQlaapDQoK", "202210", "58"));
    validateRow(response, 62, List.of("Male", "MODTRANS", "TEQlaapDQoK", "202210", "57"));
    validateRow(response, 63, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "202208", "57"));
    validateRow(response, 64, List.of("Male", "MODDISCH", "TEQlaapDQoK", "202209", "57"));
    validateRow(response, 65, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "202212", "57"));
    validateRow(response, 66, List.of("Male", "MODTRANS", "TEQlaapDQoK", "202208", "56"));
    validateRow(response, 67, List.of("Male", "MODTRANS", "TEQlaapDQoK", "202212", "56"));
    validateRow(response, 68, List.of("Male", "MODDISCH", "jmIPBj66vD6", "202212", "56"));
    validateRow(response, 69, List.of("Male", "MODDIED", "TEQlaapDQoK", "202208", "56"));
    validateRow(response, 70, List.of("Male", "MODABSC", "eIQbndfxQMb", "202209", "56"));
    validateRow(response, 71, List.of("Male", "MODABSC", "kJq2mPyFEHo", "202207", "56"));
    validateRow(response, 72, List.of("Male", "MODDISCH", "TEQlaapDQoK", "202207", "56"));
    validateRow(response, 73, List.of("Male", "MODTRANS", "jmIPBj66vD6", "202210", "56"));
    validateRow(response, 74, List.of("Male", "MODDISCH", "at6UHUQatSo", "202207", "56"));
    validateRow(response, 75, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "202208", "55"));
    validateRow(response, 76, List.of("Male", "MODDISCH", "at6UHUQatSo", "202208", "55"));
    validateRow(response, 77, List.of("Male", "MODDISCH", "fdc6uOvgoji", "202212", "55"));
    validateRow(response, 78, List.of("Male", "MODDIED", "at6UHUQatSo", "202208", "54"));
    validateRow(response, 79, List.of("Male", "MODABSC", "Vth0fbpFcsO", "202208", "54"));
    validateRow(response, 80, List.of("Male", "MODDIED", "at6UHUQatSo", "202209", "54"));
    validateRow(response, 81, List.of("Male", "MODDISCH", "eIQbndfxQMb", "202207", "54"));
    validateRow(response, 82, List.of("Male", "MODTRANS", "TEQlaapDQoK", "202207", "54"));
    validateRow(response, 83, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "202209", "54"));
    validateRow(response, 84, List.of("Male", "MODTRANS", "eIQbndfxQMb", "202207", "54"));
    validateRow(response, 85, List.of("Male", "MODDIED", "fdc6uOvgoji", "202210", "54"));
    validateRow(response, 86, List.of("Male", "MODDIED", "kJq2mPyFEHo", "202211", "53"));
    validateRow(response, 87, List.of("Male", "MODTRANS", "jmIPBj66vD6", "202207", "53"));
    validateRow(response, 88, List.of("Male", "MODABSC", "TEQlaapDQoK", "202211", "53"));
    validateRow(response, 89, List.of("Male", "MODABSC", "at6UHUQatSo", "202208", "53"));
    validateRow(response, 90, List.of("Male", "MODDIED", "Vth0fbpFcsO", "202210", "53"));
    validateRow(response, 91, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "202212", "53"));
    validateRow(response, 92, List.of("Male", "MODDISCH", "at6UHUQatSo", "202211", "52"));
    validateRow(response, 93, List.of("Male", "MODDIED", "at6UHUQatSo", "202210", "52"));
    validateRow(response, 94, List.of("Male", "MODDISCH", "TEQlaapDQoK", "202211", "52"));
    validateRow(response, 95, List.of("Male", "MODDIED", "jmIPBj66vD6", "202208", "52"));
    validateRow(response, 96, List.of("Male", "MODTRANS", "eIQbndfxQMb", "202210", "52"));
    validateRow(response, 97, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "202212", "52"));
    validateRow(response, 98, List.of("Male", "MODTRANS", "at6UHUQatSo", "202208", "52"));
    validateRow(response, 99, List.of("Male", "MODTRANS", "jmIPBj66vD6", "202211", "52"));
    validateRow(response, 100, List.of("Male", "MODDISCH", "TEQlaapDQoK", "202210", "51"));
    validateRow(response, 101, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "202208", "51"));
    validateRow(response, 102, List.of("Male", "MODABSC", "jmIPBj66vD6", "202208", "51"));
    validateRow(response, 103, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "202210", "51"));
    validateRow(response, 104, List.of("Male", "MODDISCH", "TEQlaapDQoK", "202212", "50"));
    validateRow(response, 105, List.of("Male", "MODDISCH", "eIQbndfxQMb", "202209", "50"));
    validateRow(response, 106, List.of("Male", "MODTRANS", "jmIPBj66vD6", "202208", "50"));
    validateRow(response, 107, List.of("Male", "MODDISCH", "at6UHUQatSo", "202212", "50"));
    validateRow(response, 108, List.of("Male", "MODDIED", "TEQlaapDQoK", "202211", "50"));
    validateRow(response, 109, List.of("Male", "MODDIED", "O6uvpzGd5pu", "202208", "50"));
    validateRow(response, 110, List.of("Male", "MODDIED", "at6UHUQatSo", "202211", "50"));
    validateRow(response, 111, List.of("Male", "MODABSC", "fdc6uOvgoji", "202212", "49"));
    validateRow(response, 112, List.of("Male", "MODABSC", "eIQbndfxQMb", "202212", "49"));
    validateRow(response, 113, List.of("Male", "MODTRANS", "fdc6uOvgoji", "202209", "49"));
    validateRow(response, 114, List.of("Male", "MODABSC", "Vth0fbpFcsO", "202210", "49"));
    validateRow(response, 115, List.of("Male", "MODDIED", "fdc6uOvgoji", "202208", "49"));
    validateRow(response, 116, List.of("Male", "MODTRANS", "at6UHUQatSo", "202210", "49"));
    validateRow(response, 117, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "202207", "49"));
    validateRow(response, 118, List.of("Male", "MODDISCH", "eIQbndfxQMb", "202208", "48"));
    validateRow(response, 119, List.of("Male", "MODDISCH", "fdc6uOvgoji", "202207", "48"));
    validateRow(response, 120, List.of("Male", "MODABSC", "Vth0fbpFcsO", "202207", "48"));
    validateRow(response, 121, List.of("Male", "MODDIED", "eIQbndfxQMb", "202211", "48"));
    validateRow(response, 122, List.of("Male", "MODTRANS", "fdc6uOvgoji", "202210", "48"));
    validateRow(response, 123, List.of("Male", "MODDIED", "jmIPBj66vD6", "202210", "47"));
    validateRow(response, 124, List.of("Male", "MODDIED", "eIQbndfxQMb", "202208", "47"));
    validateRow(response, 125, List.of("Male", "MODABSC", "at6UHUQatSo", "202212", "47"));
    validateRow(response, 126, List.of("Male", "MODDISCH", "TEQlaapDQoK", "202208", "47"));
    validateRow(response, 127, List.of("Male", "MODTRANS", "at6UHUQatSo", "202211", "47"));
    validateRow(response, 128, List.of("Male", "MODABSC", "PMa2VCrupOd", "202208", "47"));
    validateRow(response, 129, List.of("Male", "MODABSC", "at6UHUQatSo", "202210", "47"));
    validateRow(response, 130, List.of("Male", "MODABSC", "Vth0fbpFcsO", "202209", "47"));
    validateRow(response, 131, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "202212", "47"));
    validateRow(response, 132, List.of("Male", "MODABSC", "jmIPBj66vD6", "202207", "46"));
    validateRow(response, 133, List.of("Male", "MODDISCH", "fdc6uOvgoji", "202211", "46"));
    validateRow(response, 134, List.of("Male", "MODDIED", "Vth0fbpFcsO", "202209", "46"));
    validateRow(response, 135, List.of("Male", "MODDIED", "jmIPBj66vD6", "202211", "46"));
    validateRow(response, 136, List.of("Male", "MODABSC", "fdc6uOvgoji", "202207", "46"));
    validateRow(response, 137, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "202211", "46"));
    validateRow(response, 138, List.of("Male", "MODDIED", "fdc6uOvgoji", "202212", "46"));
    validateRow(response, 139, List.of("Male", "MODDISCH", "jUb8gELQApl", "202210", "46"));
    validateRow(response, 140, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "202209", "46"));
    validateRow(response, 141, List.of("Male", "MODDISCH", "at6UHUQatSo", "202209", "46"));
    validateRow(response, 142, List.of("Male", "MODDIED", "at6UHUQatSo", "202207", "46"));
    validateRow(response, 143, List.of("Male", "MODTRANS", "at6UHUQatSo", "202207", "46"));
    validateRow(response, 144, List.of("Male", "MODABSC", "kJq2mPyFEHo", "202209", "45"));
    validateRow(response, 145, List.of("Male", "MODABSC", "jmIPBj66vD6", "202211", "45"));
    validateRow(response, 146, List.of("Male", "MODTRANS", "eIQbndfxQMb", "202209", "45"));
    validateRow(response, 147, List.of("Male", "MODDIED", "fdc6uOvgoji", "202209", "45"));
    validateRow(response, 148, List.of("Male", "MODABSC", "eIQbndfxQMb", "202210", "45"));
    validateRow(response, 149, List.of("Male", "MODABSC", "qhqAxPSTUXp", "202209", "45"));
    validateRow(response, 150, List.of("Male", "MODDIED", "eIQbndfxQMb", "202212", "45"));
    validateRow(response, 151, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "202212", "44"));
    validateRow(response, 152, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "202209", "44"));
    validateRow(response, 153, List.of("Male", "MODABSC", "fdc6uOvgoji", "202211", "44"));
    validateRow(response, 154, List.of("Male", "MODABSC", "jmIPBj66vD6", "202210", "44"));
    validateRow(response, 155, List.of("Male", "MODDIED", "jmIPBj66vD6", "202209", "44"));
    validateRow(response, 156, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "202207", "44"));
    validateRow(response, 157, List.of("Male", "MODDIED", "jmIPBj66vD6", "202207", "43"));
    validateRow(response, 158, List.of("Male", "MODDIED", "jUb8gELQApl", "202212", "43"));
    validateRow(response, 159, List.of("Male", "MODDIED", "qhqAxPSTUXp", "202207", "43"));
    validateRow(response, 160, List.of("Male", "MODDIED", "qhqAxPSTUXp", "202208", "43"));
    validateRow(response, 161, List.of("Male", "MODDISCH", "at6UHUQatSo", "202210", "43"));
    validateRow(response, 162, List.of("Male", "MODABSC", "eIQbndfxQMb", "202211", "43"));
    validateRow(response, 163, List.of("Male", "MODDIED", "PMa2VCrupOd", "202207", "43"));
    validateRow(response, 164, List.of("Male", "MODDISCH", "eIQbndfxQMb", "202210", "42"));
    validateRow(response, 165, List.of("Male", "MODABSC", "PMa2VCrupOd", "202212", "42"));
    validateRow(response, 166, List.of("Male", "MODABSC", "jUb8gELQApl", "202212", "42"));
    validateRow(response, 167, List.of("Male", "MODDISCH", "PMa2VCrupOd", "202207", "42"));
    validateRow(response, 168, List.of("Male", "MODDIED", "Vth0fbpFcsO", "202212", "42"));
    validateRow(response, 169, List.of("Male", "MODABSC", "eIQbndfxQMb", "202207", "42"));
    validateRow(response, 170, List.of("Male", "MODDISCH", "jmIPBj66vD6", "202208", "41"));
    validateRow(response, 171, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "202211", "41"));
    validateRow(response, 172, List.of("Male", "MODDISCH", "PMa2VCrupOd", "202210", "41"));
    validateRow(response, 173, List.of("Male", "MODABSC", "Vth0fbpFcsO", "202212", "41"));
    validateRow(response, 174, List.of("Male", "MODDIED", "eIQbndfxQMb", "202209", "41"));
    validateRow(response, 175, List.of("Male", "MODDISCH", "fdc6uOvgoji", "202209", "41"));
    validateRow(response, 176, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "202210", "41"));
    validateRow(response, 177, List.of("Male", "MODTRANS", "at6UHUQatSo", "202212", "41"));
    validateRow(response, 178, List.of("Male", "MODTRANS", "fdc6uOvgoji", "202212", "41"));
    validateRow(response, 179, List.of("Male", "MODTRANS", "eIQbndfxQMb", "202208", "41"));
    validateRow(response, 180, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "202207", "41"));
    validateRow(response, 181, List.of("Male", "MODTRANS", "fdc6uOvgoji", "202208", "40"));
    validateRow(response, 182, List.of("Male", "MODDIED", "jUb8gELQApl", "202207", "40"));
    validateRow(response, 183, List.of("Male", "MODABSC", "jUb8gELQApl", "202209", "40"));
    validateRow(response, 184, List.of("Male", "MODDIED", "eIQbndfxQMb", "202207", "40"));
    validateRow(response, 185, List.of("Male", "MODDIED", "jUb8gELQApl", "202210", "40"));
    validateRow(response, 186, List.of("Male", "MODDIED", "PMa2VCrupOd", "202211", "40"));
    validateRow(response, 187, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "202210", "40"));
    validateRow(response, 188, List.of("Male", "MODTRANS", "eIQbndfxQMb", "202211", "40"));
    validateRow(response, 189, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "202207", "40"));
    validateRow(response, 190, List.of("Male", "MODDIED", "fdc6uOvgoji", "202207", "39"));
    validateRow(response, 191, List.of("Male", "MODABSC", "fdc6uOvgoji", "202209", "39"));
    validateRow(response, 192, List.of("Male", "MODDIED", "Vth0fbpFcsO", "202208", "39"));
    validateRow(response, 193, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "202211", "39"));
    validateRow(response, 194, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "202211", "39"));
    validateRow(response, 195, List.of("Male", "MODDIED", "at6UHUQatSo", "202212", "39"));
    validateRow(response, 196, List.of("Male", "MODTRANS", "PMa2VCrupOd", "202209", "38"));
    validateRow(response, 197, List.of("Male", "MODABSC", "Vth0fbpFcsO", "202211", "38"));
    validateRow(response, 198, List.of("Male", "MODTRANS", "jUb8gELQApl", "202211", "38"));
    validateRow(response, 199, List.of("Male", "MODDIED", "PMa2VCrupOd", "202209", "38"));
    validateRow(response, 200, List.of("Male", "MODABSC", "qhqAxPSTUXp", "202212", "38"));
    validateRow(response, 201, List.of("Male", "MODDIED", "jmIPBj66vD6", "202212", "38"));
    validateRow(response, 202, List.of("Male", "MODDIED", "PMa2VCrupOd", "202208", "38"));
    validateRow(response, 203, List.of("Male", "MODDIED", "jUb8gELQApl", "202211", "38"));
    validateRow(response, 204, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "202207", "38"));
    validateRow(response, 205, List.of("Male", "MODDIED", "eIQbndfxQMb", "202210", "38"));
    validateRow(response, 206, List.of("Male", "MODTRANS", "jmIPBj66vD6", "202212", "38"));
    validateRow(response, 207, List.of("Male", "MODABSC", "jmIPBj66vD6", "202209", "38"));
    validateRow(response, 208, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "202207", "37"));
    validateRow(response, 209, List.of("Male", "MODTRANS", "jUb8gELQApl", "202207", "37"));
    validateRow(response, 210, List.of("Male", "MODABSC", "qhqAxPSTUXp", "202211", "37"));
    validateRow(response, 211, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "202212", "37"));
    validateRow(response, 212, List.of("Male", "MODDISCH", "jUb8gELQApl", "202211", "37"));
    validateRow(response, 213, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "202209", "37"));
    validateRow(response, 214, List.of("Male", "MODABSC", "eIQbndfxQMb", "202208", "36"));
    validateRow(response, 215, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "202207", "36"));
    validateRow(response, 216, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "202209", "36"));
    validateRow(response, 217, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "202208", "36"));
    validateRow(response, 218, List.of("Male", "MODDISCH", "jUb8gELQApl", "202207", "36"));
    validateRow(response, 219, List.of("Male", "MODABSC", "qhqAxPSTUXp", "202210", "35"));
    validateRow(response, 220, List.of("Male", "MODTRANS", "jUb8gELQApl", "202208", "35"));
    validateRow(response, 221, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "202208", "35"));
    validateRow(response, 222, List.of("Male", "MODABSC", "jmIPBj66vD6", "202212", "35"));
    validateRow(response, 223, List.of("Male", "MODABSC", "qhqAxPSTUXp", "202207", "35"));
    validateRow(response, 224, List.of("Male", "MODTRANS", "jUb8gELQApl", "202209", "35"));
    validateRow(response, 225, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "202212", "35"));
    validateRow(response, 226, List.of("Male", "MODABSC", "lc3eMKXaEfw", "202209", "34"));
    validateRow(response, 227, List.of("Male", "MODDIED", "Vth0fbpFcsO", "202207", "34"));
    validateRow(response, 228, List.of("Male", "MODDISCH", "jmIPBj66vD6", "202209", "34"));
    validateRow(response, 229, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "202210", "34"));
    validateRow(response, 230, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "202211", "34"));
    validateRow(response, 231, List.of("Male", "MODABSC", "PMa2VCrupOd", "202207", "34"));
    validateRow(response, 232, List.of("Male", "MODDISCH", "jmIPBj66vD6", "202210", "34"));
    validateRow(response, 233, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "202208", "33"));
    validateRow(response, 234, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "202209", "33"));
    validateRow(response, 235, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "202207", "33"));
    validateRow(response, 236, List.of("Male", "MODABSC", "jUb8gELQApl", "202211", "33"));
    validateRow(response, 237, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "202210", "33"));
    validateRow(response, 238, List.of("Male", "MODDIED", "qhqAxPSTUXp", "202209", "33"));
    validateRow(response, 239, List.of("Male", "MODDISCH", "fdc6uOvgoji", "202208", "33"));
    validateRow(response, 240, List.of("Male", "MODDIED", "jUb8gELQApl", "202209", "33"));
    validateRow(response, 241, List.of("Male", "MODTRANS", "jUb8gELQApl", "202210", "33"));
    validateRow(response, 242, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "202210", "33"));
    validateRow(response, 243, List.of("Male", "MODABSC", "PMa2VCrupOd", "202209", "32"));
    validateRow(response, 244, List.of("Male", "MODDISCH", "eIQbndfxQMb", "202212", "32"));
    validateRow(response, 245, List.of("Male", "MODDISCH", "PMa2VCrupOd", "202212", "32"));
    validateRow(response, 246, List.of("Male", "MODTRANS", "fdc6uOvgoji", "202211", "32"));
    validateRow(response, 247, List.of("Male", "MODABSC", "lc3eMKXaEfw", "202211", "32"));
    validateRow(response, 248, List.of("Male", "MODABSC", "qhqAxPSTUXp", "202208", "32"));
    validateRow(response, 249, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "202211", "31"));
    validateRow(response, 250, List.of("Male", "MODABSC", "lc3eMKXaEfw", "202207", "31"));
    validateRow(response, 251, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "202209", "31"));
    validateRow(response, 252, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "202210", "31"));
    validateRow(response, 253, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "202212", "31"));
    validateRow(response, 254, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "202208", "31"));
    validateRow(response, 255, List.of("Male", "MODDIED", "lc3eMKXaEfw", "202208", "31"));
    validateRow(response, 256, List.of("Male", "MODDISCH", "jUb8gELQApl", "202209", "31"));
    validateRow(response, 257, List.of("Male", "MODTRANS", "eIQbndfxQMb", "202212", "31"));
    validateRow(response, 258, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "202212", "31"));
    validateRow(response, 259, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "202209", "30"));
    validateRow(response, 260, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "202207", "30"));
    validateRow(response, 261, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "202209", "30"));
    validateRow(response, 262, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "202212", "30"));
    validateRow(response, 263, List.of("Male", "MODABSC", "PMa2VCrupOd", "202211", "30"));
    validateRow(response, 264, List.of("Male", "MODDISCH", "jUb8gELQApl", "202208", "30"));
    validateRow(response, 265, List.of("Male", "MODTRANS", "PMa2VCrupOd", "202210", "30"));
    validateRow(response, 266, List.of("Male", "MODTRANS", "PMa2VCrupOd", "202207", "30"));
    validateRow(response, 267, List.of("Male", "MODTRANS", "jUb8gELQApl", "202212", "30"));
    validateRow(response, 268, List.of("Male", "MODDIED", "PMa2VCrupOd", "202210", "29"));
    validateRow(response, 269, List.of("Male", "MODDIED", "jUb8gELQApl", "202208", "29"));
    validateRow(response, 270, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "202208", "29"));
    validateRow(response, 271, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "202212", "29"));
    validateRow(response, 272, List.of("Male", "MODDISCH", "PMa2VCrupOd", "202208", "28"));
    validateRow(response, 273, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "202210", "28"));
    validateRow(response, 274, List.of("Male", "MODDISCH", "PMa2VCrupOd", "202211", "28"));
    validateRow(response, 275, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "202211", "28"));
    validateRow(response, 276, List.of("Male", "MODABSC", "jUb8gELQApl", "202210", "28"));
    validateRow(response, 277, List.of("Male", "MODDISCH", "PMa2VCrupOd", "202209", "28"));
    validateRow(response, 278, List.of("Male", "MODDIED", "qhqAxPSTUXp", "202212", "28"));
    validateRow(response, 279, List.of("Male", "MODDIED", "qhqAxPSTUXp", "202210", "28"));
    validateRow(response, 280, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "202208", "28"));
    validateRow(response, 281, List.of("Male", "MODTRANS", "PMa2VCrupOd", "202208", "28"));
    validateRow(response, 282, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "202208", "27"));
    validateRow(response, 283, List.of("Male", "MODDIED", "Vth0fbpFcsO", "202211", "27"));
    validateRow(response, 284, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "202207", "27"));
    validateRow(response, 285, List.of("Male", "MODDIED", "lc3eMKXaEfw", "202210", "27"));
    validateRow(response, 286, List.of("Male", "MODDISCH", "jUb8gELQApl", "202212", "27"));
    validateRow(response, 287, List.of("Male", "MODTRANS", "PMa2VCrupOd", "202211", "27"));
    validateRow(response, 288, List.of("Male", "MODDIED", "lc3eMKXaEfw", "202207", "27"));
    validateRow(response, 289, List.of("Male", "MODDIED", "lc3eMKXaEfw", "202211", "26"));
    validateRow(response, 290, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "202210", "26"));
    validateRow(response, 291, List.of("Male", "MODABSC", "jUb8gELQApl", "202208", "26"));
    validateRow(response, 292, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "202211", "26"));
    validateRow(response, 293, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "202207", "26"));
    validateRow(response, 294, List.of("Male", "MODDIED", "qhqAxPSTUXp", "202211", "26"));
    validateRow(response, 295, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "202211", "26"));
    validateRow(response, 296, List.of("Male", "MODABSC", "PMa2VCrupOd", "202210", "25"));
    validateRow(response, 297, List.of("Male", "MODABSC", "jUb8gELQApl", "202207", "25"));
    validateRow(response, 298, List.of("Male", "MODABSC", "lc3eMKXaEfw", "202212", "25"));
    validateRow(response, 299, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "202212", "25"));
    validateRow(response, 300, List.of("Male", "MODDIED", "PMa2VCrupOd", "202212", "24"));
    validateRow(response, 301, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "202211", "24"));
    validateRow(response, 302, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "202208", "24"));
    validateRow(response, 303, List.of("Male", "MODDIED", "lc3eMKXaEfw", "202212", "24"));
    validateRow(response, 304, List.of("Male", "MODABSC", "lc3eMKXaEfw", "202208", "24"));
    validateRow(response, 305, List.of("Male", "MODTRANS", "PMa2VCrupOd", "202212", "24"));
    validateRow(response, 306, List.of("Male", "MODABSC", "lc3eMKXaEfw", "202210", "24"));
    validateRow(response, 307, List.of("Male", "MODDIED", "lc3eMKXaEfw", "202209", "23"));
    validateRow(response, 308, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "202210", "23"));
    validateRow(response, 309, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "202209", "23"));
    validateRow(response, 310, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "202209", "21"));
    validateRow(response, 311, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "202212", "19"));
    validateRow(response, 312, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "202207", "84"));
    validateRow(response, 313, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "202209", "73"));
    validateRow(response, 314, List.of("Female", "MODABSC", "kJq2mPyFEHo", "202212", "71"));
    validateRow(response, 315, List.of("Female", "MODDIED", "TEQlaapDQoK", "202207", "69"));
    validateRow(response, 316, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "202208", "69"));
    validateRow(response, 317, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "202208", "68"));
    validateRow(response, 318, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "202207", "68"));
    validateRow(response, 319, List.of("Female", "MODDIED", "kJq2mPyFEHo", "202211", "67"));
    validateRow(response, 320, List.of("Female", "MODDIED", "kJq2mPyFEHo", "202210", "67"));
    validateRow(response, 321, List.of("Female", "MODDIED", "kJq2mPyFEHo", "202208", "67"));
    validateRow(response, 322, List.of("Female", "MODABSC", "O6uvpzGd5pu", "202212", "67"));
    validateRow(response, 323, List.of("Female", "MODDIED", "kJq2mPyFEHo", "202207", "67"));
    validateRow(response, 324, List.of("Female", "MODABSC", "kJq2mPyFEHo", "202211", "67"));
    validateRow(response, 325, List.of("Female", "MODABSC", "kJq2mPyFEHo", "202209", "66"));
    validateRow(response, 326, List.of("Female", "MODDIED", "O6uvpzGd5pu", "202212", "66"));
    validateRow(response, 327, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "202210", "66"));
    validateRow(response, 328, List.of("Female", "MODTRANS", "fdc6uOvgoji", "202210", "65"));
    validateRow(response, 329, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "202211", "64"));
    validateRow(response, 330, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "202208", "64"));
    validateRow(response, 331, List.of("Female", "MODDISCH", "eIQbndfxQMb", "202210", "63"));
    validateRow(response, 332, List.of("Female", "MODDISCH", "at6UHUQatSo", "202211", "63"));
    validateRow(response, 333, List.of("Female", "MODTRANS", "eIQbndfxQMb", "202210", "63"));
    validateRow(response, 334, List.of("Female", "MODDISCH", "at6UHUQatSo", "202208", "62"));
    validateRow(response, 335, List.of("Female", "MODDIED", "TEQlaapDQoK", "202211", "62"));
    validateRow(response, 336, List.of("Female", "MODTRANS", "at6UHUQatSo", "202211", "62"));
    validateRow(response, 337, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "202209", "62"));
    validateRow(response, 338, List.of("Female", "MODABSC", "O6uvpzGd5pu", "202208", "62"));
    validateRow(response, 339, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "202210", "62"));
    validateRow(response, 340, List.of("Female", "MODABSC", "O6uvpzGd5pu", "202207", "62"));
    validateRow(response, 341, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "202212", "60"));
    validateRow(response, 342, List.of("Female", "MODDIED", "O6uvpzGd5pu", "202209", "60"));
    validateRow(response, 343, List.of("Female", "MODDISCH", "TEQlaapDQoK", "202208", "60"));
    validateRow(response, 344, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "202210", "60"));
    validateRow(response, 345, List.of("Female", "MODDIED", "O6uvpzGd5pu", "202207", "60"));
    validateRow(response, 346, List.of("Female", "MODDIED", "O6uvpzGd5pu", "202210", "60"));
    validateRow(response, 347, List.of("Female", "MODDIED", "kJq2mPyFEHo", "202212", "60"));
    validateRow(response, 348, List.of("Female", "MODTRANS", "TEQlaapDQoK", "202208", "60"));
    validateRow(response, 349, List.of("Female", "MODDIED", "O6uvpzGd5pu", "202208", "59"));
    validateRow(response, 350, List.of("Female", "MODABSC", "kJq2mPyFEHo", "202207", "59"));
    validateRow(response, 351, List.of("Female", "MODABSC", "fdc6uOvgoji", "202210", "59"));
    validateRow(response, 352, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "202208", "59"));
    validateRow(response, 353, List.of("Female", "MODDISCH", "jmIPBj66vD6", "202211", "59"));
    validateRow(response, 354, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "202207", "59"));
    validateRow(response, 355, List.of("Female", "MODDIED", "at6UHUQatSo", "202211", "58"));
    validateRow(response, 356, List.of("Female", "MODABSC", "at6UHUQatSo", "202212", "58"));
    validateRow(response, 357, List.of("Female", "MODDISCH", "fdc6uOvgoji", "202210", "58"));
    validateRow(response, 358, List.of("Female", "MODABSC", "kJq2mPyFEHo", "202208", "58"));
    validateRow(response, 359, List.of("Female", "MODDIED", "at6UHUQatSo", "202207", "57"));
    validateRow(response, 360, List.of("Female", "MODABSC", "TEQlaapDQoK", "202208", "57"));
    validateRow(response, 361, List.of("Female", "MODDIED", "kJq2mPyFEHo", "202209", "57"));
    validateRow(response, 362, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "202210", "56"));
    validateRow(response, 363, List.of("Female", "MODTRANS", "fdc6uOvgoji", "202208", "56"));
    validateRow(response, 364, List.of("Female", "MODDIED", "eIQbndfxQMb", "202207", "56"));
    validateRow(response, 365, List.of("Female", "MODABSC", "O6uvpzGd5pu", "202209", "56"));
    validateRow(response, 366, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "202211", "56"));
    validateRow(response, 367, List.of("Female", "MODTRANS", "fdc6uOvgoji", "202207", "56"));
    validateRow(response, 368, List.of("Female", "MODDIED", "jmIPBj66vD6", "202207", "56"));
    validateRow(response, 369, List.of("Female", "MODTRANS", "TEQlaapDQoK", "202207", "56"));
    validateRow(response, 370, List.of("Female", "MODTRANS", "at6UHUQatSo", "202210", "56"));
    validateRow(response, 371, List.of("Female", "MODDISCH", "jmIPBj66vD6", "202209", "55"));
    validateRow(response, 372, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "202212", "55"));
    validateRow(response, 373, List.of("Female", "MODDISCH", "TEQlaapDQoK", "202207", "55"));
    validateRow(response, 374, List.of("Female", "MODTRANS", "jmIPBj66vD6", "202207", "55"));
    validateRow(response, 375, List.of("Female", "MODDISCH", "eIQbndfxQMb", "202207", "55"));
    validateRow(response, 376, List.of("Female", "MODABSC", "TEQlaapDQoK", "202209", "55"));
    validateRow(response, 377, List.of("Female", "MODTRANS", "at6UHUQatSo", "202207", "55"));
    validateRow(response, 378, List.of("Female", "MODTRANS", "TEQlaapDQoK", "202209", "55"));
    validateRow(response, 379, List.of("Female", "MODABSC", "kJq2mPyFEHo", "202210", "54"));
    validateRow(response, 380, List.of("Female", "MODDIED", "eIQbndfxQMb", "202209", "54"));
    validateRow(response, 381, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "202209", "54"));
    validateRow(response, 382, List.of("Female", "MODTRANS", "at6UHUQatSo", "202208", "54"));
    validateRow(response, 383, List.of("Female", "MODABSC", "at6UHUQatSo", "202207", "53"));
    validateRow(response, 384, List.of("Female", "MODDISCH", "TEQlaapDQoK", "202211", "53"));
    validateRow(response, 385, List.of("Female", "MODABSC", "O6uvpzGd5pu", "202210", "53"));
    validateRow(response, 386, List.of("Female", "MODTRANS", "TEQlaapDQoK", "202210", "53"));
    validateRow(response, 387, List.of("Female", "MODDISCH", "TEQlaapDQoK", "202209", "53"));
    validateRow(response, 388, List.of("Female", "MODDISCH", "at6UHUQatSo", "202209", "53"));
    validateRow(response, 389, List.of("Female", "MODTRANS", "at6UHUQatSo", "202209", "53"));
    validateRow(response, 390, List.of("Female", "MODDISCH", "eIQbndfxQMb", "202211", "53"));
    validateRow(response, 391, List.of("Female", "MODABSC", "fdc6uOvgoji", "202211", "52"));
    validateRow(response, 392, List.of("Female", "MODABSC", "O6uvpzGd5pu", "202211", "52"));
    validateRow(response, 393, List.of("Female", "MODDIED", "jmIPBj66vD6", "202212", "52"));
    validateRow(response, 394, List.of("Female", "MODABSC", "TEQlaapDQoK", "202210", "52"));
    validateRow(response, 395, List.of("Female", "MODDISCH", "fdc6uOvgoji", "202207", "51"));
    validateRow(response, 396, List.of("Female", "MODTRANS", "jmIPBj66vD6", "202212", "51"));
    validateRow(response, 397, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "202207", "51"));
    validateRow(response, 398, List.of("Female", "MODDISCH", "eIQbndfxQMb", "202212", "51"));
    validateRow(response, 399, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "202208", "51"));
    validateRow(response, 400, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "202212", "51"));
    validateRow(response, 401, List.of("Female", "MODDIED", "TEQlaapDQoK", "202209", "51"));
    validateRow(response, 402, List.of("Female", "MODDISCH", "at6UHUQatSo", "202210", "51"));
    validateRow(response, 403, List.of("Female", "MODDIED", "fdc6uOvgoji", "202211", "51"));
    validateRow(response, 404, List.of("Female", "MODABSC", "eIQbndfxQMb", "202210", "51"));
    validateRow(response, 405, List.of("Female", "MODDISCH", "jmIPBj66vD6", "202210", "50"));
    validateRow(response, 406, List.of("Female", "MODABSC", "at6UHUQatSo", "202208", "50"));
    validateRow(response, 407, List.of("Female", "MODABSC", "at6UHUQatSo", "202209", "50"));
    validateRow(response, 408, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "202212", "50"));
    validateRow(response, 409, List.of("Female", "MODABSC", "fdc6uOvgoji", "202209", "50"));
    validateRow(response, 410, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "202211", "50"));
    validateRow(response, 411, List.of("Female", "MODDISCH", "fdc6uOvgoji", "202212", "50"));
    validateRow(response, 412, List.of("Female", "MODABSC", "jUb8gELQApl", "202208", "50"));
    validateRow(response, 413, List.of("Female", "MODTRANS", "jmIPBj66vD6", "202209", "49"));
    validateRow(response, 414, List.of("Female", "MODABSC", "jmIPBj66vD6", "202211", "49"));
    validateRow(response, 415, List.of("Female", "MODDIED", "O6uvpzGd5pu", "202211", "49"));
    validateRow(response, 416, List.of("Female", "MODABSC", "fdc6uOvgoji", "202207", "49"));
    validateRow(response, 417, List.of("Female", "MODDIED", "jUb8gELQApl", "202208", "49"));
    validateRow(response, 418, List.of("Female", "MODDIED", "jmIPBj66vD6", "202210", "49"));
    validateRow(response, 419, List.of("Female", "MODTRANS", "TEQlaapDQoK", "202211", "49"));
    validateRow(response, 420, List.of("Female", "MODABSC", "jmIPBj66vD6", "202209", "48"));
    validateRow(response, 421, List.of("Female", "MODDISCH", "jUb8gELQApl", "202208", "48"));
    validateRow(response, 422, List.of("Female", "MODDIED", "at6UHUQatSo", "202209", "48"));
    validateRow(response, 423, List.of("Female", "MODTRANS", "eIQbndfxQMb", "202209", "48"));
    validateRow(response, 424, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "202207", "47"));
    validateRow(response, 425, List.of("Female", "MODABSC", "eIQbndfxQMb", "202209", "47"));
    validateRow(response, 426, List.of("Female", "MODABSC", "jmIPBj66vD6", "202210", "47"));
    validateRow(response, 427, List.of("Female", "MODDIED", "Vth0fbpFcsO", "202211", "47"));
    validateRow(response, 428, List.of("Female", "MODDIED", "eIQbndfxQMb", "202212", "47"));
    validateRow(response, 429, List.of("Female", "MODDISCH", "fdc6uOvgoji", "202211", "47"));
    validateRow(response, 430, List.of("Female", "MODTRANS", "jUb8gELQApl", "202207", "47"));
    validateRow(response, 431, List.of("Female", "MODDIED", "fdc6uOvgoji", "202207", "47"));
    validateRow(response, 432, List.of("Female", "MODABSC", "eIQbndfxQMb", "202211", "47"));
    validateRow(response, 433, List.of("Female", "MODABSC", "TEQlaapDQoK", "202211", "47"));
    validateRow(response, 434, List.of("Female", "MODDISCH", "jmIPBj66vD6", "202207", "47"));
    validateRow(response, 435, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "202211", "47"));
    validateRow(response, 436, List.of("Female", "MODTRANS", "fdc6uOvgoji", "202209", "46"));
    validateRow(response, 437, List.of("Female", "MODTRANS", "jmIPBj66vD6", "202210", "46"));
    validateRow(response, 438, List.of("Female", "MODTRANS", "TEQlaapDQoK", "202212", "46"));
    validateRow(response, 439, List.of("Female", "MODDIED", "eIQbndfxQMb", "202211", "46"));
    validateRow(response, 440, List.of("Female", "MODTRANS", "eIQbndfxQMb", "202207", "46"));
    validateRow(response, 441, List.of("Female", "MODDISCH", "jmIPBj66vD6", "202212", "46"));
    validateRow(response, 442, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "202209", "46"));
    validateRow(response, 443, List.of("Female", "MODABSC", "at6UHUQatSo", "202210", "46"));
    validateRow(response, 444, List.of("Female", "MODABSC", "TEQlaapDQoK", "202207", "46"));
    validateRow(response, 445, List.of("Female", "MODDIED", "jmIPBj66vD6", "202209", "46"));
    validateRow(response, 446, List.of("Female", "MODTRANS", "fdc6uOvgoji", "202211", "45"));
    validateRow(response, 447, List.of("Female", "MODDISCH", "at6UHUQatSo", "202212", "45"));
    validateRow(response, 448, List.of("Female", "MODTRANS", "eIQbndfxQMb", "202208", "45"));
    validateRow(response, 449, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "202208", "45"));
    validateRow(response, 450, List.of("Female", "MODDISCH", "jmIPBj66vD6", "202208", "45"));
    validateRow(response, 451, List.of("Female", "MODABSC", "fdc6uOvgoji", "202212", "45"));
    validateRow(response, 452, List.of("Female", "MODABSC", "Vth0fbpFcsO", "202207", "45"));
    validateRow(response, 453, List.of("Female", "MODDIED", "fdc6uOvgoji", "202210", "44"));
    validateRow(response, 454, List.of("Female", "MODABSC", "Vth0fbpFcsO", "202211", "44"));
    validateRow(response, 455, List.of("Female", "MODDIED", "TEQlaapDQoK", "202212", "44"));
    validateRow(response, 456, List.of("Female", "MODABSC", "at6UHUQatSo", "202211", "44"));
    validateRow(response, 457, List.of("Female", "MODDIED", "jmIPBj66vD6", "202211", "44"));
    validateRow(response, 458, List.of("Female", "MODDIED", "fdc6uOvgoji", "202209", "44"));
    validateRow(response, 459, List.of("Female", "MODABSC", "eIQbndfxQMb", "202212", "44"));
    validateRow(response, 460, List.of("Female", "MODDISCH", "eIQbndfxQMb", "202208", "44"));
    validateRow(response, 461, List.of("Female", "MODDIED", "TEQlaapDQoK", "202210", "43"));
    validateRow(response, 462, List.of("Female", "MODDISCH", "jUb8gELQApl", "202212", "43"));
    validateRow(response, 463, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "202210", "43"));
    validateRow(response, 464, List.of("Female", "MODDISCH", "fdc6uOvgoji", "202208", "43"));
    validateRow(response, 465, List.of("Female", "MODDIED", "at6UHUQatSo", "202210", "43"));
    validateRow(response, 466, List.of("Female", "MODDIED", "eIQbndfxQMb", "202210", "43"));
    validateRow(response, 467, List.of("Female", "MODABSC", "qhqAxPSTUXp", "202211", "43"));
    validateRow(response, 468, List.of("Female", "MODABSC", "fdc6uOvgoji", "202208", "43"));
    validateRow(response, 469, List.of("Female", "MODDIED", "at6UHUQatSo", "202208", "43"));
    validateRow(response, 470, List.of("Female", "MODTRANS", "jmIPBj66vD6", "202211", "43"));
    validateRow(response, 471, List.of("Female", "MODTRANS", "at6UHUQatSo", "202212", "43"));
    validateRow(response, 472, List.of("Female", "MODDISCH", "fdc6uOvgoji", "202209", "42"));
    validateRow(response, 473, List.of("Female", "MODDIED", "Vth0fbpFcsO", "202212", "42"));
    validateRow(response, 474, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "202211", "42"));
    validateRow(response, 475, List.of("Female", "MODDIED", "TEQlaapDQoK", "202208", "42"));
    validateRow(response, 476, List.of("Female", "MODABSC", "qhqAxPSTUXp", "202212", "42"));
    validateRow(response, 477, List.of("Female", "MODDISCH", "at6UHUQatSo", "202207", "41"));
    validateRow(response, 478, List.of("Female", "MODABSC", "jUb8gELQApl", "202210", "41"));
    validateRow(response, 479, List.of("Female", "MODDIED", "fdc6uOvgoji", "202208", "41"));
    validateRow(response, 480, List.of("Female", "MODABSC", "jmIPBj66vD6", "202212", "41"));
    validateRow(response, 481, List.of("Female", "MODTRANS", "jUb8gELQApl", "202208", "41"));
    validateRow(response, 482, List.of("Female", "MODDIED", "at6UHUQatSo", "202212", "41"));
    validateRow(response, 483, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "202209", "41"));
    validateRow(response, 484, List.of("Female", "MODDISCH", "TEQlaapDQoK", "202210", "41"));
    validateRow(response, 485, List.of("Female", "MODABSC", "Vth0fbpFcsO", "202212", "41"));
    validateRow(response, 486, List.of("Female", "MODTRANS", "fdc6uOvgoji", "202212", "41"));
    validateRow(response, 487, List.of("Female", "MODDIED", "qhqAxPSTUXp", "202209", "40"));
    validateRow(response, 488, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "202207", "40"));
    validateRow(response, 489, List.of("Female", "MODABSC", "jmIPBj66vD6", "202208", "40"));
    validateRow(response, 490, List.of("Female", "MODABSC", "jmIPBj66vD6", "202207", "40"));
    validateRow(response, 491, List.of("Female", "MODDIED", "fdc6uOvgoji", "202212", "40"));
    validateRow(response, 492, List.of("Female", "MODABSC", "eIQbndfxQMb", "202208", "40"));
    validateRow(response, 493, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "202211", "40"));
    validateRow(response, 494, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "202208", "40"));
    validateRow(response, 495, List.of("Female", "MODABSC", "eIQbndfxQMb", "202207", "40"));
    validateRow(response, 496, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "202212", "39"));
    validateRow(response, 497, List.of("Female", "MODTRANS", "eIQbndfxQMb", "202211", "39"));
    validateRow(response, 498, List.of("Female", "MODTRANS", "jUb8gELQApl", "202209", "39"));
    validateRow(response, 499, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "202207", "39"));
    validateRow(response, 500, List.of("Female", "MODDIED", "qhqAxPSTUXp", "202208", "39"));
    validateRow(response, 501, List.of("Female", "MODDIED", "Vth0fbpFcsO", "202209", "39"));
    validateRow(response, 502, List.of("Female", "MODABSC", "Vth0fbpFcsO", "202208", "39"));
    validateRow(response, 503, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "202210", "39"));
    validateRow(response, 504, List.of("Female", "MODABSC", "TEQlaapDQoK", "202212", "39"));
    validateRow(response, 505, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "202212", "39"));
    validateRow(response, 506, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "202212", "38"));
    validateRow(response, 507, List.of("Female", "MODDIED", "qhqAxPSTUXp", "202211", "38"));
    validateRow(response, 508, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "202210", "38"));
    validateRow(response, 509, List.of("Female", "MODABSC", "Vth0fbpFcsO", "202210", "38"));
    validateRow(response, 510, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "202211", "38"));
    validateRow(response, 511, List.of("Female", "MODABSC", "qhqAxPSTUXp", "202210", "38"));
    validateRow(response, 512, List.of("Female", "MODABSC", "jUb8gELQApl", "202211", "38"));
    validateRow(response, 513, List.of("Female", "MODTRANS", "PMa2VCrupOd", "202207", "38"));
    validateRow(response, 514, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "202208", "38"));
    validateRow(response, 515, List.of("Female", "MODDIED", "Vth0fbpFcsO", "202207", "38"));
    validateRow(response, 516, List.of("Female", "MODDIED", "jUb8gELQApl", "202210", "37"));
    validateRow(response, 517, List.of("Female", "MODDIED", "qhqAxPSTUXp", "202207", "37"));
    validateRow(response, 518, List.of("Female", "MODDIED", "Vth0fbpFcsO", "202210", "37"));
    validateRow(response, 519, List.of("Female", "MODDISCH", "TEQlaapDQoK", "202212", "37"));
    validateRow(response, 520, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "202208", "37"));
    validateRow(response, 521, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "202212", "37"));
    validateRow(response, 522, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "202210", "37"));
    validateRow(response, 523, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "202211", "37"));
    validateRow(response, 524, List.of("Female", "MODDIED", "jUb8gELQApl", "202211", "37"));
    validateRow(response, 525, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "202208", "36"));
    validateRow(response, 526, List.of("Female", "MODDIED", "jmIPBj66vD6", "202208", "36"));
    validateRow(response, 527, List.of("Female", "MODDISCH", "jUb8gELQApl", "202209", "36"));
    validateRow(response, 528, List.of("Female", "MODDIED", "jUb8gELQApl", "202212", "36"));
    validateRow(response, 529, List.of("Female", "MODABSC", "qhqAxPSTUXp", "202207", "35"));
    validateRow(response, 530, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "202208", "35"));
    validateRow(response, 531, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "202210", "35"));
    validateRow(response, 532, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "202209", "35"));
    validateRow(response, 533, List.of("Female", "MODTRANS", "eIQbndfxQMb", "202212", "35"));
    validateRow(response, 534, List.of("Female", "MODTRANS", "jUb8gELQApl", "202211", "35"));
    validateRow(response, 535, List.of("Female", "MODDISCH", "eIQbndfxQMb", "202209", "34"));
    validateRow(response, 536, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "202210", "34"));
    validateRow(response, 537, List.of("Female", "MODABSC", "Vth0fbpFcsO", "202209", "34"));
    validateRow(response, 538, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "202212", "34"));
    validateRow(response, 539, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "202210", "34"));
    validateRow(response, 540, List.of("Female", "MODDISCH", "PMa2VCrupOd", "202210", "34"));
    validateRow(response, 541, List.of("Female", "MODDIED", "Vth0fbpFcsO", "202208", "34"));
    validateRow(response, 542, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "202211", "33"));
    validateRow(response, 543, List.of("Female", "MODDIED", "PMa2VCrupOd", "202209", "33"));
    validateRow(response, 544, List.of("Female", "MODABSC", "PMa2VCrupOd", "202210", "33"));
    validateRow(response, 545, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "202211", "33"));
    validateRow(response, 546, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "202210", "33"));
    validateRow(response, 547, List.of("Female", "MODDIED", "jUb8gELQApl", "202207", "33"));
    validateRow(response, 548, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "202208", "33"));
    validateRow(response, 549, List.of("Female", "MODDIED", "PMa2VCrupOd", "202210", "33"));
    validateRow(response, 550, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "202209", "33"));
    validateRow(response, 551, List.of("Female", "MODTRANS", "jmIPBj66vD6", "202208", "33"));
    validateRow(response, 552, List.of("Female", "MODDISCH", "jUb8gELQApl", "202207", "33"));
    validateRow(response, 553, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "202209", "32"));
    validateRow(response, 554, List.of("Female", "MODABSC", "PMa2VCrupOd", "202212", "32"));
    validateRow(response, 555, List.of("Female", "MODABSC", "PMa2VCrupOd", "202208", "32"));
    validateRow(response, 556, List.of("Female", "MODDIED", "jUb8gELQApl", "202209", "32"));
    validateRow(response, 557, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "202207", "32"));
    validateRow(response, 558, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "202209", "32"));
    validateRow(response, 559, List.of("Female", "MODABSC", "lc3eMKXaEfw", "202208", "32"));
    validateRow(response, 560, List.of("Female", "MODDIED", "lc3eMKXaEfw", "202211", "32"));
    validateRow(response, 561, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "202210", "31"));
    validateRow(response, 562, List.of("Female", "MODTRANS", "jUb8gELQApl", "202212", "31"));
    validateRow(response, 563, List.of("Female", "MODDIED", "PMa2VCrupOd", "202208", "31"));
    validateRow(response, 564, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "202207", "31"));
    validateRow(response, 565, List.of("Female", "MODDISCH", "jUb8gELQApl", "202210", "31"));
    validateRow(response, 566, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "202209", "31"));
    validateRow(response, 567, List.of("Female", "MODABSC", "jUb8gELQApl", "202207", "31"));
    validateRow(response, 568, List.of("Female", "MODDISCH", "PMa2VCrupOd", "202207", "31"));
    validateRow(response, 569, List.of("Female", "MODDIED", "PMa2VCrupOd", "202211", "30"));
    validateRow(response, 570, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "202209", "30"));
    validateRow(response, 571, List.of("Female", "MODABSC", "jUb8gELQApl", "202209", "30"));
    validateRow(response, 572, List.of("Female", "MODABSC", "jUb8gELQApl", "202212", "30"));
    validateRow(response, 573, List.of("Female", "MODABSC", "PMa2VCrupOd", "202211", "30"));
    validateRow(response, 574, List.of("Female", "MODDIED", "eIQbndfxQMb", "202208", "30"));
    validateRow(response, 575, List.of("Female", "MODABSC", "PMa2VCrupOd", "202207", "29"));
    validateRow(response, 576, List.of("Female", "MODDISCH", "PMa2VCrupOd", "202212", "29"));
    validateRow(response, 577, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "202209", "29"));
    validateRow(response, 578, List.of("Female", "MODTRANS", "PMa2VCrupOd", "202209", "29"));
    validateRow(response, 579, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "202211", "29"));
    validateRow(response, 580, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "202207", "29"));
    validateRow(response, 581, List.of("Female", "MODDIED", "qhqAxPSTUXp", "202212", "29"));
    validateRow(response, 582, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "202207", "29"));
    validateRow(response, 583, List.of("Female", "MODTRANS", "jUb8gELQApl", "202210", "28"));
    validateRow(response, 584, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "202212", "28"));
    validateRow(response, 585, List.of("Female", "MODDISCH", "PMa2VCrupOd", "202208", "28"));
    validateRow(response, 586, List.of("Female", "MODABSC", "qhqAxPSTUXp", "202209", "27"));
    validateRow(response, 587, List.of("Female", "MODABSC", "lc3eMKXaEfw", "202210", "27"));
    validateRow(response, 588, List.of("Female", "MODDIED", "qhqAxPSTUXp", "202210", "27"));
    validateRow(response, 589, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "202212", "27"));
    validateRow(response, 590, List.of("Female", "MODDIED", "PMa2VCrupOd", "202207", "27"));
    validateRow(response, 591, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "202209", "27"));
    validateRow(response, 592, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "202209", "26"));
    validateRow(response, 593, List.of("Female", "MODDISCH", "PMa2VCrupOd", "202211", "26"));
    validateRow(response, 594, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "202208", "26"));
    validateRow(response, 595, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "202207", "26"));
    validateRow(response, 596, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "202211", "26"));
    validateRow(response, 597, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "202212", "26"));
    validateRow(response, 598, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "202212", "26"));
    validateRow(response, 599, List.of("Female", "MODDIED", "PMa2VCrupOd", "202212", "26"));
    validateRow(response, 600, List.of("Female", "MODABSC", "lc3eMKXaEfw", "202209", "26"));
    validateRow(response, 601, List.of("Female", "MODTRANS", "PMa2VCrupOd", "202212", "26"));
    validateRow(response, 602, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "202207", "25"));
    validateRow(response, 603, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "202210", "25"));
    validateRow(response, 604, List.of("Female", "MODDIED", "lc3eMKXaEfw", "202208", "25"));
    validateRow(response, 605, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "202211", "25"));
    validateRow(response, 606, List.of("Female", "MODDIED", "lc3eMKXaEfw", "202207", "24"));
    validateRow(response, 607, List.of("Female", "MODDISCH", "jUb8gELQApl", "202211", "24"));
    validateRow(response, 608, List.of("Female", "MODTRANS", "PMa2VCrupOd", "202211", "23"));
    validateRow(response, 609, List.of("Female", "MODTRANS", "PMa2VCrupOd", "202210", "23"));
    validateRow(response, 610, List.of("Female", "MODDIED", "lc3eMKXaEfw", "202212", "23"));
    validateRow(response, 611, List.of("Female", "MODABSC", "qhqAxPSTUXp", "202208", "23"));
    validateRow(response, 612, List.of("Female", "MODABSC", "PMa2VCrupOd", "202209", "23"));
    validateRow(response, 613, List.of("Female", "MODTRANS", "PMa2VCrupOd", "202208", "23"));
    validateRow(response, 614, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "202211", "22"));
    validateRow(response, 615, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "202212", "22"));
    validateRow(response, 616, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "202207", "21"));
    validateRow(response, 617, List.of("Female", "MODABSC", "lc3eMKXaEfw", "202207", "21"));
    validateRow(response, 618, List.of("Female", "MODDIED", "lc3eMKXaEfw", "202210", "20"));
    validateRow(response, 619, List.of("Female", "MODABSC", "lc3eMKXaEfw", "202211", "20"));
    validateRow(response, 620, List.of("Female", "MODDIED", "lc3eMKXaEfw", "202209", "20"));
    validateRow(response, 621, List.of("Female", "MODDISCH", "PMa2VCrupOd", "202209", "19"));
    validateRow(response, 622, List.of("Female", "MODABSC", "lc3eMKXaEfw", "202212", "19"));
    validateRow(response, 623, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "202208", "19"));
    validateRow(response, 624, List.of("", "MODTRANS", "O6uvpzGd5pu", "202208", "1"));
  }

  @Test
  public void queryGenderAndModeOfDischargeByDistrictsThisYearagg() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=pe:THIS_YEAR,ou:jUb8gELQApl;TEQlaapDQoK;eIQbndfxQMb;Vth0fbpFcsO;PMa2VCrupOd;O6uvpzGd5pu;bL4ooGhyHRQ;kJq2mPyFEHo;fdc6uOvgoji;at6UHUQatSo;lc3eMKXaEfw;qhqAxPSTUXp;jmIPBj66vD6,Zj7UnCAulEk.oZg33kd9taw,Zj7UnCAulEk.fWIAEtYVEGk")
            .add("relativePeriodDate=2021-02-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(108)))
        .body("height", equalTo(108))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"gj2fKKyp8OH\":{\"code\":\"MODDIED\",\"name\":\"Died\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"Fhbf4aKpZmZ\":{\"code\":\"MODABSC\",\"name\":\"Absconded\"},\"fWIAEtYVEGk\":{\"name\":\"Mode of Discharge\"},\"2021\":{\"name\":\"2021\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"pe\":{\"name\":\"Period\"},\"oZg33kd9taw\":{\"name\":\"Gender\"},\"fShHdgT7XGb\":{\"code\":\"MODTRANS\",\"name\":\"Transferred\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"yeod5tOXpkP\":{\"code\":\"MODDISCH\",\"name\":\"Discharged\"}},\"dimensions\":{\"pe\":[\"2021\"],\"ou\":[\"jUb8gELQApl\",\"TEQlaapDQoK\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"PMa2VCrupOd\",\"O6uvpzGd5pu\",\"bL4ooGhyHRQ\",\"kJq2mPyFEHo\",\"fdc6uOvgoji\",\"at6UHUQatSo\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"jmIPBj66vD6\"],\"oZg33kd9taw\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"fWIAEtYVEGk\":[\"yeod5tOXpkP\",\"gj2fKKyp8OH\",\"fShHdgT7XGb\",\"Fhbf4aKpZmZ\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "oZg33kd9taw", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "fWIAEtYVEGk", "Mode of Discharge", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "2021", "739"));
    validateRow(response, 1, List.of("Male", "MODDIED", "O6uvpzGd5pu", "2021", "734"));
    validateRow(response, 2, List.of("Male", "MODABSC", "O6uvpzGd5pu", "2021", "724"));
    validateRow(response, 3, List.of("Male", "MODDIED", "TEQlaapDQoK", "2021", "710"));
    validateRow(response, 4, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "2021", "705"));
    validateRow(response, 5, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "2021", "701"));
    validateRow(response, 6, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "2021", "693"));
    validateRow(response, 7, List.of("Male", "MODABSC", "kJq2mPyFEHo", "2021", "687"));
    validateRow(response, 8, List.of("Male", "MODABSC", "TEQlaapDQoK", "2021", "677"));
    validateRow(response, 9, List.of("Male", "MODDIED", "kJq2mPyFEHo", "2021", "675"));
    validateRow(response, 10, List.of("Male", "MODDISCH", "TEQlaapDQoK", "2021", "666"));
    validateRow(response, 11, List.of("Male", "MODTRANS", "TEQlaapDQoK", "2021", "659"));
    validateRow(response, 12, List.of("Male", "MODDISCH", "fdc6uOvgoji", "2021", "646"));
    validateRow(response, 13, List.of("Male", "MODABSC", "at6UHUQatSo", "2021", "639"));
    validateRow(response, 14, List.of("Male", "MODTRANS", "at6UHUQatSo", "2021", "628"));
    validateRow(response, 15, List.of("Male", "MODDIED", "at6UHUQatSo", "2021", "585"));
    validateRow(response, 16, List.of("Male", "MODTRANS", "eIQbndfxQMb", "2021", "571"));
    validateRow(response, 17, List.of("Male", "MODTRANS", "fdc6uOvgoji", "2021", "570"));
    validateRow(response, 18, List.of("Male", "MODABSC", "fdc6uOvgoji", "2021", "559"));
    validateRow(response, 19, List.of("Male", "MODDIED", "fdc6uOvgoji", "2021", "559"));
    validateRow(response, 20, List.of("Male", "MODABSC", "jmIPBj66vD6", "2021", "552"));
    validateRow(response, 21, List.of("Male", "MODDISCH", "eIQbndfxQMb", "2021", "547"));
    validateRow(response, 22, List.of("Male", "MODDISCH", "jmIPBj66vD6", "2021", "542"));
    validateRow(response, 23, List.of("Male", "MODDISCH", "at6UHUQatSo", "2021", "542"));
    validateRow(response, 24, List.of("Male", "MODDIED", "eIQbndfxQMb", "2021", "526"));
    validateRow(response, 25, List.of("Male", "MODDIED", "jmIPBj66vD6", "2021", "521"));
    validateRow(response, 26, List.of("Male", "MODDIED", "Vth0fbpFcsO", "2021", "509"));
    validateRow(response, 27, List.of("Male", "MODTRANS", "jmIPBj66vD6", "2021", "503"));
    validateRow(response, 28, List.of("Male", "MODABSC", "Vth0fbpFcsO", "2021", "500"));
    validateRow(response, 29, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "2021", "491"));
    validateRow(response, 30, List.of("Male", "MODABSC", "eIQbndfxQMb", "2021", "480"));
    validateRow(response, 31, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "2021", "477"));
    validateRow(response, 32, List.of("Male", "MODDISCH", "jUb8gELQApl", "2021", "462"));
    validateRow(response, 33, List.of("Male", "MODABSC", "jUb8gELQApl", "2021", "459"));
    validateRow(response, 34, List.of("Male", "MODTRANS", "jUb8gELQApl", "2021", "442"));
    validateRow(response, 35, List.of("Male", "MODDIED", "jUb8gELQApl", "2021", "424"));
    validateRow(response, 36, List.of("Male", "MODDIED", "qhqAxPSTUXp", "2021", "423"));
    validateRow(response, 37, List.of("Male", "MODABSC", "qhqAxPSTUXp", "2021", "403"));
    validateRow(response, 38, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "2021", "390"));
    validateRow(response, 39, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "2021", "386"));
    validateRow(response, 40, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "2021", "382"));
    validateRow(response, 41, List.of("Male", "MODABSC", "PMa2VCrupOd", "2021", "378"));
    validateRow(response, 42, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "2021", "378"));
    validateRow(response, 43, List.of("Male", "MODDIED", "PMa2VCrupOd", "2021", "377"));
    validateRow(response, 44, List.of("Male", "MODABSC", "lc3eMKXaEfw", "2021", "366"));
    validateRow(response, 45, List.of("Male", "MODTRANS", "PMa2VCrupOd", "2021", "361"));
    validateRow(response, 46, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "2021", "358"));
    validateRow(response, 47, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "2021", "345"));
    validateRow(response, 48, List.of("Male", "MODDIED", "lc3eMKXaEfw", "2021", "331"));
    validateRow(response, 49, List.of("Male", "MODDISCH", "PMa2VCrupOd", "2021", "322"));
    validateRow(response, 50, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "2021", "320"));
    validateRow(response, 51, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "2021", "317"));
    validateRow(response, 52, List.of("Male", "", "O6uvpzGd5pu", "2021", "11"));
    validateRow(response, 53, List.of("Female", "MODABSC", "kJq2mPyFEHo", "2021", "741"));
    validateRow(response, 54, List.of("Female", "MODDIED", "O6uvpzGd5pu", "2021", "733"));
    validateRow(response, 55, List.of("Female", "MODABSC", "O6uvpzGd5pu", "2021", "720"));
    validateRow(response, 56, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "2021", "715"));
    validateRow(response, 57, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "2021", "705"));
    validateRow(response, 58, List.of("Female", "MODDIED", "kJq2mPyFEHo", "2021", "693"));
    validateRow(response, 59, List.of("Female", "MODDIED", "TEQlaapDQoK", "2021", "680"));
    validateRow(response, 60, List.of("Female", "MODTRANS", "TEQlaapDQoK", "2021", "673"));
    validateRow(response, 61, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "2021", "668"));
    validateRow(response, 62, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "2021", "663"));
    validateRow(response, 63, List.of("Female", "MODDISCH", "TEQlaapDQoK", "2021", "652"));
    validateRow(response, 64, List.of("Female", "MODABSC", "TEQlaapDQoK", "2021", "652"));
    validateRow(response, 65, List.of("Female", "MODABSC", "at6UHUQatSo", "2021", "624"));
    validateRow(response, 66, List.of("Female", "MODTRANS", "fdc6uOvgoji", "2021", "601"));
    validateRow(response, 67, List.of("Female", "MODDISCH", "at6UHUQatSo", "2021", "600"));
    validateRow(response, 68, List.of("Female", "MODDIED", "at6UHUQatSo", "2021", "593"));
    validateRow(response, 69, List.of("Female", "MODDIED", "fdc6uOvgoji", "2021", "582"));
    validateRow(response, 70, List.of("Female", "MODABSC", "fdc6uOvgoji", "2021", "580"));
    validateRow(response, 71, List.of("Female", "MODTRANS", "at6UHUQatSo", "2021", "579"));
    validateRow(response, 72, List.of("Female", "MODDISCH", "eIQbndfxQMb", "2021", "567"));
    validateRow(response, 73, List.of("Female", "MODABSC", "jmIPBj66vD6", "2021", "559"));
    validateRow(response, 74, List.of("Female", "MODDISCH", "fdc6uOvgoji", "2021", "553"));
    validateRow(response, 75, List.of("Female", "MODTRANS", "jmIPBj66vD6", "2021", "538"));
    validateRow(response, 76, List.of("Female", "MODDISCH", "jmIPBj66vD6", "2021", "525"));
    validateRow(response, 77, List.of("Female", "MODDIED", "eIQbndfxQMb", "2021", "519"));
    validateRow(response, 78, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "2021", "516"));
    validateRow(response, 79, List.of("Female", "MODTRANS", "eIQbndfxQMb", "2021", "511"));
    validateRow(response, 80, List.of("Female", "MODABSC", "eIQbndfxQMb", "2021", "510"));
    validateRow(response, 81, List.of("Female", "MODDIED", "jmIPBj66vD6", "2021", "498"));
    validateRow(response, 82, List.of("Female", "MODABSC", "Vth0fbpFcsO", "2021", "484"));
    validateRow(response, 83, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "2021", "469"));
    validateRow(response, 84, List.of("Female", "MODDIED", "jUb8gELQApl", "2021", "465"));
    validateRow(response, 85, List.of("Female", "MODDISCH", "jUb8gELQApl", "2021", "464"));
    validateRow(response, 86, List.of("Female", "MODDIED", "Vth0fbpFcsO", "2021", "463"));
    validateRow(response, 87, List.of("Female", "MODABSC", "jUb8gELQApl", "2021", "462"));
    validateRow(response, 88, List.of("Female", "MODTRANS", "jUb8gELQApl", "2021", "429"));
    validateRow(response, 89, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "2021", "404"));
    validateRow(response, 90, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "2021", "404"));
    validateRow(response, 91, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "2021", "391"));
    validateRow(response, 92, List.of("Female", "MODABSC", "qhqAxPSTUXp", "2021", "388"));
    validateRow(response, 93, List.of("Female", "MODDIED", "qhqAxPSTUXp", "2021", "385"));
    validateRow(response, 94, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "2021", "384"));
    validateRow(response, 95, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "2021", "379"));
    validateRow(response, 96, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "2021", "375"));
    validateRow(response, 97, List.of("Female", "MODABSC", "PMa2VCrupOd", "2021", "355"));
    validateRow(response, 98, List.of("Female", "MODABSC", "lc3eMKXaEfw", "2021", "354"));
    validateRow(response, 99, List.of("Female", "MODTRANS", "PMa2VCrupOd", "2021", "352"));
    validateRow(response, 100, List.of("Female", "MODDIED", "lc3eMKXaEfw", "2021", "335"));
    validateRow(response, 101, List.of("Female", "MODDISCH", "PMa2VCrupOd", "2021", "332"));
    validateRow(response, 102, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "2021", "331"));
    validateRow(response, 103, List.of("Female", "MODDIED", "PMa2VCrupOd", "2021", "330"));
    validateRow(response, 104, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "2021", "327"));
    validateRow(response, 105, List.of("Female", "", "O6uvpzGd5pu", "2021", "2"));
    validateRow(response, 106, List.of("", "MODTRANS", "O6uvpzGd5pu", "2021", "1"));
    validateRow(response, 107, List.of("", "MODDISCH", "O6uvpzGd5pu", "2021", "1"));
  }
}
