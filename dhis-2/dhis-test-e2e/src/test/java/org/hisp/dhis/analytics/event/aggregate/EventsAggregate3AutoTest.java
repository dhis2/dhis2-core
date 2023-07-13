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
    validateRow(response, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "2019Q3", "9"));
    validateRow(response, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "2019Q3", "4"));
    validateRow(response, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "2019Q1", "1"));
    validateRow(response, List.of("Male", "MODABSC", "O6uvpzGd5pu", "2019Q3", "1"));
    validateRow(response, List.of("", "MODTRANS", "O6uvpzGd5pu", "2019Q3", "1"));
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
    validateRow(response, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "2019Q3", "9"));
    validateRow(response, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "2019Q3", "4"));
    validateRow(response, List.of("Male", "MODABSC", "O6uvpzGd5pu", "2019Q3", "1"));
    validateRow(response, List.of("", "MODTRANS", "O6uvpzGd5pu", "2019Q3", "1"));
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
    validateRow(response, List.of("Male", "MODDIED", "kJq2mPyFEHo", "202212", "75"));
    validateRow(response, List.of("Male", "MODABSC", "O6uvpzGd5pu", "202209", "74"));
    validateRow(response, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "202208", "73"));
    validateRow(response, List.of("Male", "MODABSC", "O6uvpzGd5pu", "202211", "72"));
    validateRow(response, List.of("Male", "MODDIED", "kJq2mPyFEHo", "202210", "71"));
    validateRow(response, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "202210", "71"));
    validateRow(response, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "202209", "70"));
    validateRow(response, List.of("Male", "MODDIED", "kJq2mPyFEHo", "202208", "69"));
    validateRow(response, List.of("Male", "MODABSC", "fdc6uOvgoji", "202208", "68"));
    validateRow(response, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "202210", "68"));
    validateRow(response, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "202208", "68"));
    validateRow(response, List.of("Male", "MODABSC", "kJq2mPyFEHo", "202210", "68"));
    validateRow(response, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "202207", "68"));
    validateRow(response, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "202209", "67"));
    validateRow(response, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "202209", "67"));
    validateRow(response, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "202211", "65"));
    validateRow(response, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "202211", "65"));
    validateRow(response, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "202211", "65"));
    validateRow(response, List.of("Male", "MODTRANS", "TEQlaapDQoK", "202211", "65"));
    validateRow(response, List.of("Male", "MODDIED", "O6uvpzGd5pu", "202207", "65"));
    validateRow(response, List.of("Male", "MODDIED", "TEQlaapDQoK", "202209", "65"));
    validateRow(response, List.of("Male", "MODDISCH", "jmIPBj66vD6", "202207", "65"));
    validateRow(response, List.of("Male", "MODDIED", "fdc6uOvgoji", "202211", "65"));
    validateRow(response, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "202207", "64"));
    validateRow(response, List.of("Male", "MODTRANS", "fdc6uOvgoji", "202207", "64"));
    validateRow(response, List.of("Male", "MODABSC", "TEQlaapDQoK", "202208", "64"));
    validateRow(response, List.of("Male", "MODABSC", "TEQlaapDQoK", "202212", "64"));
    validateRow(response, List.of("Male", "MODABSC", "O6uvpzGd5pu", "202208", "64"));
    validateRow(response, List.of("Male", "MODABSC", "fdc6uOvgoji", "202210", "63"));
    validateRow(response, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "202212", "63"));
    validateRow(response, List.of("Male", "MODABSC", "TEQlaapDQoK", "202209", "63"));
    validateRow(response, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "202207", "63"));
    validateRow(response, List.of("Male", "MODDISCH", "eIQbndfxQMb", "202211", "63"));
    validateRow(response, List.of("Male", "MODDIED", "TEQlaapDQoK", "202212", "63"));
    validateRow(response, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "202211", "63"));
    validateRow(response, List.of("Male", "MODDISCH", "fdc6uOvgoji", "202210", "63"));
    validateRow(response, List.of("Male", "MODABSC", "O6uvpzGd5pu", "202210", "62"));
    validateRow(response, List.of("Male", "MODDISCH", "jmIPBj66vD6", "202211", "62"));
    validateRow(response, List.of("Male", "MODABSC", "at6UHUQatSo", "202209", "62"));
    validateRow(response, List.of("Male", "MODABSC", "kJq2mPyFEHo", "202212", "62"));
    validateRow(response, List.of("Male", "MODTRANS", "at6UHUQatSo", "202209", "62"));
    validateRow(response, List.of("Male", "MODABSC", "kJq2mPyFEHo", "202208", "62"));
    validateRow(response, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "202210", "62"));
    validateRow(response, List.of("Male", "MODDIED", "O6uvpzGd5pu", "202210", "61"));
    validateRow(response, List.of("Male", "MODABSC", "O6uvpzGd5pu", "202207", "61"));
    validateRow(response, List.of("Male", "MODDIED", "TEQlaapDQoK", "202207", "61"));
    validateRow(response, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "202210", "60"));
    validateRow(response, List.of("Male", "MODDIED", "kJq2mPyFEHo", "202209", "60"));
    validateRow(response, List.of("Male", "MODDIED", "O6uvpzGd5pu", "202209", "60"));
    validateRow(response, List.of("Male", "MODTRANS", "jmIPBj66vD6", "202209", "60"));
    validateRow(response, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "202208", "60"));
    validateRow(response, List.of("Male", "MODDIED", "O6uvpzGd5pu", "202211", "60"));
    validateRow(response, List.of("Male", "MODDIED", "O6uvpzGd5pu", "202212", "59"));
    validateRow(response, List.of("Male", "MODTRANS", "TEQlaapDQoK", "202209", "59"));
    validateRow(response, List.of("Male", "MODABSC", "kJq2mPyFEHo", "202211", "59"));
    validateRow(response, List.of("Male", "MODABSC", "at6UHUQatSo", "202211", "59"));
    validateRow(response, List.of("Male", "MODABSC", "at6UHUQatSo", "202207", "59"));
    validateRow(response, List.of("Male", "MODDIED", "kJq2mPyFEHo", "202207", "59"));
    validateRow(response, List.of("Male", "MODABSC", "O6uvpzGd5pu", "202212", "59"));
    validateRow(response, List.of("Male", "MODABSC", "TEQlaapDQoK", "202210", "58"));
    validateRow(response, List.of("Male", "MODABSC", "TEQlaapDQoK", "202207", "58"));
    validateRow(response, List.of("Male", "MODDIED", "TEQlaapDQoK", "202210", "58"));
    validateRow(response, List.of("Male", "MODTRANS", "TEQlaapDQoK", "202210", "57"));
    validateRow(response, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "202208", "57"));
    validateRow(response, List.of("Male", "MODDISCH", "TEQlaapDQoK", "202209", "57"));
    validateRow(response, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "202212", "57"));
    validateRow(response, List.of("Male", "MODTRANS", "TEQlaapDQoK", "202208", "56"));
    validateRow(response, List.of("Male", "MODTRANS", "TEQlaapDQoK", "202212", "56"));
    validateRow(response, List.of("Male", "MODDISCH", "jmIPBj66vD6", "202212", "56"));
    validateRow(response, List.of("Male", "MODDIED", "TEQlaapDQoK", "202208", "56"));
    validateRow(response, List.of("Male", "MODABSC", "eIQbndfxQMb", "202209", "56"));
    validateRow(response, List.of("Male", "MODABSC", "kJq2mPyFEHo", "202207", "56"));
    validateRow(response, List.of("Male", "MODDISCH", "TEQlaapDQoK", "202207", "56"));
    validateRow(response, List.of("Male", "MODTRANS", "jmIPBj66vD6", "202210", "56"));
    validateRow(response, List.of("Male", "MODDISCH", "at6UHUQatSo", "202207", "56"));
    validateRow(response, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "202208", "55"));
    validateRow(response, List.of("Male", "MODDISCH", "at6UHUQatSo", "202208", "55"));
    validateRow(response, List.of("Male", "MODDISCH", "fdc6uOvgoji", "202212", "55"));
    validateRow(response, List.of("Male", "MODDIED", "at6UHUQatSo", "202208", "54"));
    validateRow(response, List.of("Male", "MODABSC", "Vth0fbpFcsO", "202208", "54"));
    validateRow(response, List.of("Male", "MODDIED", "at6UHUQatSo", "202209", "54"));
    validateRow(response, List.of("Male", "MODDISCH", "eIQbndfxQMb", "202207", "54"));
    validateRow(response, List.of("Male", "MODTRANS", "TEQlaapDQoK", "202207", "54"));
    validateRow(response, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "202209", "54"));
    validateRow(response, List.of("Male", "MODTRANS", "eIQbndfxQMb", "202207", "54"));
    validateRow(response, List.of("Male", "MODDIED", "fdc6uOvgoji", "202210", "54"));
    validateRow(response, List.of("Male", "MODDIED", "kJq2mPyFEHo", "202211", "53"));
    validateRow(response, List.of("Male", "MODTRANS", "jmIPBj66vD6", "202207", "53"));
    validateRow(response, List.of("Male", "MODABSC", "TEQlaapDQoK", "202211", "53"));
    validateRow(response, List.of("Male", "MODABSC", "at6UHUQatSo", "202208", "53"));
    validateRow(response, List.of("Male", "MODDIED", "Vth0fbpFcsO", "202210", "53"));
    validateRow(response, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "202212", "53"));
    validateRow(response, List.of("Male", "MODDISCH", "at6UHUQatSo", "202211", "52"));
    validateRow(response, List.of("Male", "MODDIED", "at6UHUQatSo", "202210", "52"));
    validateRow(response, List.of("Male", "MODDISCH", "TEQlaapDQoK", "202211", "52"));
    validateRow(response, List.of("Male", "MODDIED", "jmIPBj66vD6", "202208", "52"));
    validateRow(response, List.of("Male", "MODTRANS", "eIQbndfxQMb", "202210", "52"));
    validateRow(response, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "202212", "52"));
    validateRow(response, List.of("Male", "MODTRANS", "at6UHUQatSo", "202208", "52"));
    validateRow(response, List.of("Male", "MODTRANS", "jmIPBj66vD6", "202211", "52"));
    validateRow(response, List.of("Male", "MODDISCH", "TEQlaapDQoK", "202210", "51"));
    validateRow(response, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "202208", "51"));
    validateRow(response, List.of("Male", "MODABSC", "jmIPBj66vD6", "202208", "51"));
    validateRow(response, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "202210", "51"));
    validateRow(response, List.of("Male", "MODDISCH", "TEQlaapDQoK", "202212", "50"));
    validateRow(response, List.of("Male", "MODDISCH", "eIQbndfxQMb", "202209", "50"));
    validateRow(response, List.of("Male", "MODTRANS", "jmIPBj66vD6", "202208", "50"));
    validateRow(response, List.of("Male", "MODDISCH", "at6UHUQatSo", "202212", "50"));
    validateRow(response, List.of("Male", "MODDIED", "TEQlaapDQoK", "202211", "50"));
    validateRow(response, List.of("Male", "MODDIED", "O6uvpzGd5pu", "202208", "50"));
    validateRow(response, List.of("Male", "MODDIED", "at6UHUQatSo", "202211", "50"));
    validateRow(response, List.of("Male", "MODABSC", "fdc6uOvgoji", "202212", "49"));
    validateRow(response, List.of("Male", "MODABSC", "eIQbndfxQMb", "202212", "49"));
    validateRow(response, List.of("Male", "MODTRANS", "fdc6uOvgoji", "202209", "49"));
    validateRow(response, List.of("Male", "MODABSC", "Vth0fbpFcsO", "202210", "49"));
    validateRow(response, List.of("Male", "MODDIED", "fdc6uOvgoji", "202208", "49"));
    validateRow(response, List.of("Male", "MODTRANS", "at6UHUQatSo", "202210", "49"));
    validateRow(response, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "202207", "49"));
    validateRow(response, List.of("Male", "MODDISCH", "eIQbndfxQMb", "202208", "48"));
    validateRow(response, List.of("Male", "MODDISCH", "fdc6uOvgoji", "202207", "48"));
    validateRow(response, List.of("Male", "MODABSC", "Vth0fbpFcsO", "202207", "48"));
    validateRow(response, List.of("Male", "MODDIED", "eIQbndfxQMb", "202211", "48"));
    validateRow(response, List.of("Male", "MODTRANS", "fdc6uOvgoji", "202210", "48"));
    validateRow(response, List.of("Male", "MODDIED", "jmIPBj66vD6", "202210", "47"));
    validateRow(response, List.of("Male", "MODDIED", "eIQbndfxQMb", "202208", "47"));
    validateRow(response, List.of("Male", "MODABSC", "at6UHUQatSo", "202212", "47"));
    validateRow(response, List.of("Male", "MODDISCH", "TEQlaapDQoK", "202208", "47"));
    validateRow(response, List.of("Male", "MODTRANS", "at6UHUQatSo", "202211", "47"));
    validateRow(response, List.of("Male", "MODABSC", "PMa2VCrupOd", "202208", "47"));
    validateRow(response, List.of("Male", "MODABSC", "at6UHUQatSo", "202210", "47"));
    validateRow(response, List.of("Male", "MODABSC", "Vth0fbpFcsO", "202209", "47"));
    validateRow(response, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "202212", "47"));
    validateRow(response, List.of("Male", "MODABSC", "jmIPBj66vD6", "202207", "46"));
    validateRow(response, List.of("Male", "MODDISCH", "fdc6uOvgoji", "202211", "46"));
    validateRow(response, List.of("Male", "MODDIED", "Vth0fbpFcsO", "202209", "46"));
    validateRow(response, List.of("Male", "MODDIED", "jmIPBj66vD6", "202211", "46"));
    validateRow(response, List.of("Male", "MODABSC", "fdc6uOvgoji", "202207", "46"));
    validateRow(response, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "202211", "46"));
    validateRow(response, List.of("Male", "MODDIED", "fdc6uOvgoji", "202212", "46"));
    validateRow(response, List.of("Male", "MODDISCH", "jUb8gELQApl", "202210", "46"));
    validateRow(response, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "202209", "46"));
    validateRow(response, List.of("Male", "MODDISCH", "at6UHUQatSo", "202209", "46"));
    validateRow(response, List.of("Male", "MODDIED", "at6UHUQatSo", "202207", "46"));
    validateRow(response, List.of("Male", "MODTRANS", "at6UHUQatSo", "202207", "46"));
    validateRow(response, List.of("Male", "MODABSC", "kJq2mPyFEHo", "202209", "45"));
    validateRow(response, List.of("Male", "MODABSC", "jmIPBj66vD6", "202211", "45"));
    validateRow(response, List.of("Male", "MODTRANS", "eIQbndfxQMb", "202209", "45"));
    validateRow(response, List.of("Male", "MODDIED", "fdc6uOvgoji", "202209", "45"));
    validateRow(response, List.of("Male", "MODABSC", "eIQbndfxQMb", "202210", "45"));
    validateRow(response, List.of("Male", "MODABSC", "qhqAxPSTUXp", "202209", "45"));
    validateRow(response, List.of("Male", "MODDIED", "eIQbndfxQMb", "202212", "45"));
    validateRow(response, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "202212", "44"));
    validateRow(response, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "202209", "44"));
    validateRow(response, List.of("Male", "MODABSC", "fdc6uOvgoji", "202211", "44"));
    validateRow(response, List.of("Male", "MODABSC", "jmIPBj66vD6", "202210", "44"));
    validateRow(response, List.of("Male", "MODDIED", "jmIPBj66vD6", "202209", "44"));
    validateRow(response, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "202207", "44"));
    validateRow(response, List.of("Male", "MODDIED", "jmIPBj66vD6", "202207", "43"));
    validateRow(response, List.of("Male", "MODDIED", "jUb8gELQApl", "202212", "43"));
    validateRow(response, List.of("Male", "MODDIED", "qhqAxPSTUXp", "202207", "43"));
    validateRow(response, List.of("Male", "MODDIED", "qhqAxPSTUXp", "202208", "43"));
    validateRow(response, List.of("Male", "MODDISCH", "at6UHUQatSo", "202210", "43"));
    validateRow(response, List.of("Male", "MODABSC", "eIQbndfxQMb", "202211", "43"));
    validateRow(response, List.of("Male", "MODDIED", "PMa2VCrupOd", "202207", "43"));
    validateRow(response, List.of("Male", "MODDISCH", "eIQbndfxQMb", "202210", "42"));
    validateRow(response, List.of("Male", "MODABSC", "PMa2VCrupOd", "202212", "42"));
    validateRow(response, List.of("Male", "MODABSC", "jUb8gELQApl", "202212", "42"));
    validateRow(response, List.of("Male", "MODDISCH", "PMa2VCrupOd", "202207", "42"));
    validateRow(response, List.of("Male", "MODDIED", "Vth0fbpFcsO", "202212", "42"));
    validateRow(response, List.of("Male", "MODABSC", "eIQbndfxQMb", "202207", "42"));
    validateRow(response, List.of("Male", "MODDISCH", "jmIPBj66vD6", "202208", "41"));
    validateRow(response, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "202211", "41"));
    validateRow(response, List.of("Male", "MODDISCH", "PMa2VCrupOd", "202210", "41"));
    validateRow(response, List.of("Male", "MODABSC", "Vth0fbpFcsO", "202212", "41"));
    validateRow(response, List.of("Male", "MODDIED", "eIQbndfxQMb", "202209", "41"));
    validateRow(response, List.of("Male", "MODDISCH", "fdc6uOvgoji", "202209", "41"));
    validateRow(response, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "202210", "41"));
    validateRow(response, List.of("Male", "MODTRANS", "at6UHUQatSo", "202212", "41"));
    validateRow(response, List.of("Male", "MODTRANS", "fdc6uOvgoji", "202212", "41"));
    validateRow(response, List.of("Male", "MODTRANS", "eIQbndfxQMb", "202208", "41"));
    validateRow(response, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "202207", "41"));
    validateRow(response, List.of("Male", "MODTRANS", "fdc6uOvgoji", "202208", "40"));
    validateRow(response, List.of("Male", "MODDIED", "jUb8gELQApl", "202207", "40"));
    validateRow(response, List.of("Male", "MODABSC", "jUb8gELQApl", "202209", "40"));
    validateRow(response, List.of("Male", "MODDIED", "eIQbndfxQMb", "202207", "40"));
    validateRow(response, List.of("Male", "MODDIED", "jUb8gELQApl", "202210", "40"));
    validateRow(response, List.of("Male", "MODDIED", "PMa2VCrupOd", "202211", "40"));
    validateRow(response, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "202210", "40"));
    validateRow(response, List.of("Male", "MODTRANS", "eIQbndfxQMb", "202211", "40"));
    validateRow(response, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "202207", "40"));
    validateRow(response, List.of("Male", "MODDIED", "fdc6uOvgoji", "202207", "39"));
    validateRow(response, List.of("Male", "MODABSC", "fdc6uOvgoji", "202209", "39"));
    validateRow(response, List.of("Male", "MODDIED", "Vth0fbpFcsO", "202208", "39"));
    validateRow(response, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "202211", "39"));
    validateRow(response, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "202211", "39"));
    validateRow(response, List.of("Male", "MODDIED", "at6UHUQatSo", "202212", "39"));
    validateRow(response, List.of("Male", "MODTRANS", "PMa2VCrupOd", "202209", "38"));
    validateRow(response, List.of("Male", "MODABSC", "Vth0fbpFcsO", "202211", "38"));
    validateRow(response, List.of("Male", "MODTRANS", "jUb8gELQApl", "202211", "38"));
    validateRow(response, List.of("Male", "MODDIED", "PMa2VCrupOd", "202209", "38"));
    validateRow(response, List.of("Male", "MODABSC", "qhqAxPSTUXp", "202212", "38"));
    validateRow(response, List.of("Male", "MODDIED", "jmIPBj66vD6", "202212", "38"));
    validateRow(response, List.of("Male", "MODDIED", "PMa2VCrupOd", "202208", "38"));
    validateRow(response, List.of("Male", "MODDIED", "jUb8gELQApl", "202211", "38"));
    validateRow(response, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "202207", "38"));
    validateRow(response, List.of("Male", "MODDIED", "eIQbndfxQMb", "202210", "38"));
    validateRow(response, List.of("Male", "MODTRANS", "jmIPBj66vD6", "202212", "38"));
    validateRow(response, List.of("Male", "MODABSC", "jmIPBj66vD6", "202209", "38"));
    validateRow(response, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "202207", "37"));
    validateRow(response, List.of("Male", "MODTRANS", "jUb8gELQApl", "202207", "37"));
    validateRow(response, List.of("Male", "MODABSC", "qhqAxPSTUXp", "202211", "37"));
    validateRow(response, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "202212", "37"));
    validateRow(response, List.of("Male", "MODDISCH", "jUb8gELQApl", "202211", "37"));
    validateRow(response, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "202209", "37"));
    validateRow(response, List.of("Male", "MODABSC", "eIQbndfxQMb", "202208", "36"));
    validateRow(response, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "202207", "36"));
    validateRow(response, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "202209", "36"));
    validateRow(response, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "202208", "36"));
    validateRow(response, List.of("Male", "MODDISCH", "jUb8gELQApl", "202207", "36"));
    validateRow(response, List.of("Male", "MODABSC", "qhqAxPSTUXp", "202210", "35"));
    validateRow(response, List.of("Male", "MODTRANS", "jUb8gELQApl", "202208", "35"));
    validateRow(response, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "202208", "35"));
    validateRow(response, List.of("Male", "MODABSC", "jmIPBj66vD6", "202212", "35"));
    validateRow(response, List.of("Male", "MODABSC", "qhqAxPSTUXp", "202207", "35"));
    validateRow(response, List.of("Male", "MODTRANS", "jUb8gELQApl", "202209", "35"));
    validateRow(response, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "202212", "35"));
    validateRow(response, List.of("Male", "MODABSC", "lc3eMKXaEfw", "202209", "34"));
    validateRow(response, List.of("Male", "MODDIED", "Vth0fbpFcsO", "202207", "34"));
    validateRow(response, List.of("Male", "MODDISCH", "jmIPBj66vD6", "202209", "34"));
    validateRow(response, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "202210", "34"));
    validateRow(response, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "202211", "34"));
    validateRow(response, List.of("Male", "MODABSC", "PMa2VCrupOd", "202207", "34"));
    validateRow(response, List.of("Male", "MODDISCH", "jmIPBj66vD6", "202210", "34"));
    validateRow(response, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "202208", "33"));
    validateRow(response, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "202209", "33"));
    validateRow(response, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "202207", "33"));
    validateRow(response, List.of("Male", "MODABSC", "jUb8gELQApl", "202211", "33"));
    validateRow(response, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "202210", "33"));
    validateRow(response, List.of("Male", "MODDIED", "qhqAxPSTUXp", "202209", "33"));
    validateRow(response, List.of("Male", "MODDISCH", "fdc6uOvgoji", "202208", "33"));
    validateRow(response, List.of("Male", "MODDIED", "jUb8gELQApl", "202209", "33"));
    validateRow(response, List.of("Male", "MODTRANS", "jUb8gELQApl", "202210", "33"));
    validateRow(response, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "202210", "33"));
    validateRow(response, List.of("Male", "MODABSC", "PMa2VCrupOd", "202209", "32"));
    validateRow(response, List.of("Male", "MODDISCH", "eIQbndfxQMb", "202212", "32"));
    validateRow(response, List.of("Male", "MODDISCH", "PMa2VCrupOd", "202212", "32"));
    validateRow(response, List.of("Male", "MODTRANS", "fdc6uOvgoji", "202211", "32"));
    validateRow(response, List.of("Male", "MODABSC", "lc3eMKXaEfw", "202211", "32"));
    validateRow(response, List.of("Male", "MODABSC", "qhqAxPSTUXp", "202208", "32"));
    validateRow(response, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "202211", "31"));
    validateRow(response, List.of("Male", "MODABSC", "lc3eMKXaEfw", "202207", "31"));
    validateRow(response, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "202209", "31"));
    validateRow(response, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "202210", "31"));
    validateRow(response, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "202212", "31"));
    validateRow(response, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "202208", "31"));
    validateRow(response, List.of("Male", "MODDIED", "lc3eMKXaEfw", "202208", "31"));
    validateRow(response, List.of("Male", "MODDISCH", "jUb8gELQApl", "202209", "31"));
    validateRow(response, List.of("Male", "MODTRANS", "eIQbndfxQMb", "202212", "31"));
    validateRow(response, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "202212", "31"));
    validateRow(response, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "202209", "30"));
    validateRow(response, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "202207", "30"));
    validateRow(response, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "202209", "30"));
    validateRow(response, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "202212", "30"));
    validateRow(response, List.of("Male", "MODABSC", "PMa2VCrupOd", "202211", "30"));
    validateRow(response, List.of("Male", "MODDISCH", "jUb8gELQApl", "202208", "30"));
    validateRow(response, List.of("Male", "MODTRANS", "PMa2VCrupOd", "202210", "30"));
    validateRow(response, List.of("Male", "MODTRANS", "PMa2VCrupOd", "202207", "30"));
    validateRow(response, List.of("Male", "MODTRANS", "jUb8gELQApl", "202212", "30"));
    validateRow(response, List.of("Male", "MODDIED", "PMa2VCrupOd", "202210", "29"));
    validateRow(response, List.of("Male", "MODDIED", "jUb8gELQApl", "202208", "29"));
    validateRow(response, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "202208", "29"));
    validateRow(response, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "202212", "29"));
    validateRow(response, List.of("Male", "MODDISCH", "PMa2VCrupOd", "202208", "28"));
    validateRow(response, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "202210", "28"));
    validateRow(response, List.of("Male", "MODDISCH", "PMa2VCrupOd", "202211", "28"));
    validateRow(response, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "202211", "28"));
    validateRow(response, List.of("Male", "MODABSC", "jUb8gELQApl", "202210", "28"));
    validateRow(response, List.of("Male", "MODDISCH", "PMa2VCrupOd", "202209", "28"));
    validateRow(response, List.of("Male", "MODDIED", "qhqAxPSTUXp", "202212", "28"));
    validateRow(response, List.of("Male", "MODDIED", "qhqAxPSTUXp", "202210", "28"));
    validateRow(response, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "202208", "28"));
    validateRow(response, List.of("Male", "MODTRANS", "PMa2VCrupOd", "202208", "28"));
    validateRow(response, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "202208", "27"));
    validateRow(response, List.of("Male", "MODDIED", "Vth0fbpFcsO", "202211", "27"));
    validateRow(response, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "202207", "27"));
    validateRow(response, List.of("Male", "MODDIED", "lc3eMKXaEfw", "202210", "27"));
    validateRow(response, List.of("Male", "MODDISCH", "jUb8gELQApl", "202212", "27"));
    validateRow(response, List.of("Male", "MODTRANS", "PMa2VCrupOd", "202211", "27"));
    validateRow(response, List.of("Male", "MODDIED", "lc3eMKXaEfw", "202207", "27"));
    validateRow(response, List.of("Male", "MODDIED", "lc3eMKXaEfw", "202211", "26"));
    validateRow(response, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "202210", "26"));
    validateRow(response, List.of("Male", "MODABSC", "jUb8gELQApl", "202208", "26"));
    validateRow(response, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "202211", "26"));
    validateRow(response, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "202207", "26"));
    validateRow(response, List.of("Male", "MODDIED", "qhqAxPSTUXp", "202211", "26"));
    validateRow(response, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "202211", "26"));
    validateRow(response, List.of("Male", "MODABSC", "PMa2VCrupOd", "202210", "25"));
    validateRow(response, List.of("Male", "MODABSC", "jUb8gELQApl", "202207", "25"));
    validateRow(response, List.of("Male", "MODABSC", "lc3eMKXaEfw", "202212", "25"));
    validateRow(response, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "202212", "25"));
    validateRow(response, List.of("Male", "MODDIED", "PMa2VCrupOd", "202212", "24"));
    validateRow(response, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "202211", "24"));
    validateRow(response, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "202208", "24"));
    validateRow(response, List.of("Male", "MODDIED", "lc3eMKXaEfw", "202212", "24"));
    validateRow(response, List.of("Male", "MODABSC", "lc3eMKXaEfw", "202208", "24"));
    validateRow(response, List.of("Male", "MODTRANS", "PMa2VCrupOd", "202212", "24"));
    validateRow(response, List.of("Male", "MODABSC", "lc3eMKXaEfw", "202210", "24"));
    validateRow(response, List.of("Male", "MODDIED", "lc3eMKXaEfw", "202209", "23"));
    validateRow(response, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "202210", "23"));
    validateRow(response, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "202209", "23"));
    validateRow(response, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "202209", "21"));
    validateRow(response, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "202212", "19"));
    validateRow(response, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "202207", "84"));
    validateRow(response, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "202209", "73"));
    validateRow(response, List.of("Female", "MODABSC", "kJq2mPyFEHo", "202212", "71"));
    validateRow(response, List.of("Female", "MODDIED", "TEQlaapDQoK", "202207", "69"));
    validateRow(response, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "202208", "69"));
    validateRow(response, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "202208", "68"));
    validateRow(response, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "202207", "68"));
    validateRow(response, List.of("Female", "MODDIED", "kJq2mPyFEHo", "202211", "67"));
    validateRow(response, List.of("Female", "MODDIED", "kJq2mPyFEHo", "202210", "67"));
    validateRow(response, List.of("Female", "MODDIED", "kJq2mPyFEHo", "202208", "67"));
    validateRow(response, List.of("Female", "MODABSC", "O6uvpzGd5pu", "202212", "67"));
    validateRow(response, List.of("Female", "MODDIED", "kJq2mPyFEHo", "202207", "67"));
    validateRow(response, List.of("Female", "MODABSC", "kJq2mPyFEHo", "202211", "67"));
    validateRow(response, List.of("Female", "MODABSC", "kJq2mPyFEHo", "202209", "66"));
    validateRow(response, List.of("Female", "MODDIED", "O6uvpzGd5pu", "202212", "66"));
    validateRow(response, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "202210", "66"));
    validateRow(response, List.of("Female", "MODTRANS", "fdc6uOvgoji", "202210", "65"));
    validateRow(response, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "202211", "64"));
    validateRow(response, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "202208", "64"));
    validateRow(response, List.of("Female", "MODDISCH", "eIQbndfxQMb", "202210", "63"));
    validateRow(response, List.of("Female", "MODDISCH", "at6UHUQatSo", "202211", "63"));
    validateRow(response, List.of("Female", "MODTRANS", "eIQbndfxQMb", "202210", "63"));
    validateRow(response, List.of("Female", "MODDISCH", "at6UHUQatSo", "202208", "62"));
    validateRow(response, List.of("Female", "MODDIED", "TEQlaapDQoK", "202211", "62"));
    validateRow(response, List.of("Female", "MODTRANS", "at6UHUQatSo", "202211", "62"));
    validateRow(response, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "202209", "62"));
    validateRow(response, List.of("Female", "MODABSC", "O6uvpzGd5pu", "202208", "62"));
    validateRow(response, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "202210", "62"));
    validateRow(response, List.of("Female", "MODABSC", "O6uvpzGd5pu", "202207", "62"));
    validateRow(response, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "202212", "60"));
    validateRow(response, List.of("Female", "MODDIED", "O6uvpzGd5pu", "202209", "60"));
    validateRow(response, List.of("Female", "MODDISCH", "TEQlaapDQoK", "202208", "60"));
    validateRow(response, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "202210", "60"));
    validateRow(response, List.of("Female", "MODDIED", "O6uvpzGd5pu", "202207", "60"));
    validateRow(response, List.of("Female", "MODDIED", "O6uvpzGd5pu", "202210", "60"));
    validateRow(response, List.of("Female", "MODDIED", "kJq2mPyFEHo", "202212", "60"));
    validateRow(response, List.of("Female", "MODTRANS", "TEQlaapDQoK", "202208", "60"));
    validateRow(response, List.of("Female", "MODDIED", "O6uvpzGd5pu", "202208", "59"));
    validateRow(response, List.of("Female", "MODABSC", "kJq2mPyFEHo", "202207", "59"));
    validateRow(response, List.of("Female", "MODABSC", "fdc6uOvgoji", "202210", "59"));
    validateRow(response, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "202208", "59"));
    validateRow(response, List.of("Female", "MODDISCH", "jmIPBj66vD6", "202211", "59"));
    validateRow(response, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "202207", "59"));
    validateRow(response, List.of("Female", "MODDIED", "at6UHUQatSo", "202211", "58"));
    validateRow(response, List.of("Female", "MODABSC", "at6UHUQatSo", "202212", "58"));
    validateRow(response, List.of("Female", "MODDISCH", "fdc6uOvgoji", "202210", "58"));
    validateRow(response, List.of("Female", "MODABSC", "kJq2mPyFEHo", "202208", "58"));
    validateRow(response, List.of("Female", "MODDIED", "at6UHUQatSo", "202207", "57"));
    validateRow(response, List.of("Female", "MODABSC", "TEQlaapDQoK", "202208", "57"));
    validateRow(response, List.of("Female", "MODDIED", "kJq2mPyFEHo", "202209", "57"));
    validateRow(response, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "202210", "56"));
    validateRow(response, List.of("Female", "MODTRANS", "fdc6uOvgoji", "202208", "56"));
    validateRow(response, List.of("Female", "MODDIED", "eIQbndfxQMb", "202207", "56"));
    validateRow(response, List.of("Female", "MODABSC", "O6uvpzGd5pu", "202209", "56"));
    validateRow(response, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "202211", "56"));
    validateRow(response, List.of("Female", "MODTRANS", "fdc6uOvgoji", "202207", "56"));
    validateRow(response, List.of("Female", "MODDIED", "jmIPBj66vD6", "202207", "56"));
    validateRow(response, List.of("Female", "MODTRANS", "TEQlaapDQoK", "202207", "56"));
    validateRow(response, List.of("Female", "MODTRANS", "at6UHUQatSo", "202210", "56"));
    validateRow(response, List.of("Female", "MODDISCH", "jmIPBj66vD6", "202209", "55"));
    validateRow(response, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "202212", "55"));
    validateRow(response, List.of("Female", "MODDISCH", "TEQlaapDQoK", "202207", "55"));
    validateRow(response, List.of("Female", "MODTRANS", "jmIPBj66vD6", "202207", "55"));
    validateRow(response, List.of("Female", "MODDISCH", "eIQbndfxQMb", "202207", "55"));
    validateRow(response, List.of("Female", "MODABSC", "TEQlaapDQoK", "202209", "55"));
    validateRow(response, List.of("Female", "MODTRANS", "at6UHUQatSo", "202207", "55"));
    validateRow(response, List.of("Female", "MODTRANS", "TEQlaapDQoK", "202209", "55"));
    validateRow(response, List.of("Female", "MODABSC", "kJq2mPyFEHo", "202210", "54"));
    validateRow(response, List.of("Female", "MODDIED", "eIQbndfxQMb", "202209", "54"));
    validateRow(response, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "202209", "54"));
    validateRow(response, List.of("Female", "MODTRANS", "at6UHUQatSo", "202208", "54"));
    validateRow(response, List.of("Female", "MODABSC", "at6UHUQatSo", "202207", "53"));
    validateRow(response, List.of("Female", "MODDISCH", "TEQlaapDQoK", "202211", "53"));
    validateRow(response, List.of("Female", "MODABSC", "O6uvpzGd5pu", "202210", "53"));
    validateRow(response, List.of("Female", "MODTRANS", "TEQlaapDQoK", "202210", "53"));
    validateRow(response, List.of("Female", "MODDISCH", "TEQlaapDQoK", "202209", "53"));
    validateRow(response, List.of("Female", "MODDISCH", "at6UHUQatSo", "202209", "53"));
    validateRow(response, List.of("Female", "MODTRANS", "at6UHUQatSo", "202209", "53"));
    validateRow(response, List.of("Female", "MODDISCH", "eIQbndfxQMb", "202211", "53"));
    validateRow(response, List.of("Female", "MODABSC", "fdc6uOvgoji", "202211", "52"));
    validateRow(response, List.of("Female", "MODABSC", "O6uvpzGd5pu", "202211", "52"));
    validateRow(response, List.of("Female", "MODDIED", "jmIPBj66vD6", "202212", "52"));
    validateRow(response, List.of("Female", "MODABSC", "TEQlaapDQoK", "202210", "52"));
    validateRow(response, List.of("Female", "MODDISCH", "fdc6uOvgoji", "202207", "51"));
    validateRow(response, List.of("Female", "MODTRANS", "jmIPBj66vD6", "202212", "51"));
    validateRow(response, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "202207", "51"));
    validateRow(response, List.of("Female", "MODDISCH", "eIQbndfxQMb", "202212", "51"));
    validateRow(response, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "202208", "51"));
    validateRow(response, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "202212", "51"));
    validateRow(response, List.of("Female", "MODDIED", "TEQlaapDQoK", "202209", "51"));
    validateRow(response, List.of("Female", "MODDISCH", "at6UHUQatSo", "202210", "51"));
    validateRow(response, List.of("Female", "MODDIED", "fdc6uOvgoji", "202211", "51"));
    validateRow(response, List.of("Female", "MODABSC", "eIQbndfxQMb", "202210", "51"));
    validateRow(response, List.of("Female", "MODDISCH", "jmIPBj66vD6", "202210", "50"));
    validateRow(response, List.of("Female", "MODABSC", "at6UHUQatSo", "202208", "50"));
    validateRow(response, List.of("Female", "MODABSC", "at6UHUQatSo", "202209", "50"));
    validateRow(response, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "202212", "50"));
    validateRow(response, List.of("Female", "MODABSC", "fdc6uOvgoji", "202209", "50"));
    validateRow(response, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "202211", "50"));
    validateRow(response, List.of("Female", "MODDISCH", "fdc6uOvgoji", "202212", "50"));
    validateRow(response, List.of("Female", "MODABSC", "jUb8gELQApl", "202208", "50"));
    validateRow(response, List.of("Female", "MODTRANS", "jmIPBj66vD6", "202209", "49"));
    validateRow(response, List.of("Female", "MODABSC", "jmIPBj66vD6", "202211", "49"));
    validateRow(response, List.of("Female", "MODDIED", "O6uvpzGd5pu", "202211", "49"));
    validateRow(response, List.of("Female", "MODABSC", "fdc6uOvgoji", "202207", "49"));
    validateRow(response, List.of("Female", "MODDIED", "jUb8gELQApl", "202208", "49"));
    validateRow(response, List.of("Female", "MODDIED", "jmIPBj66vD6", "202210", "49"));
    validateRow(response, List.of("Female", "MODTRANS", "TEQlaapDQoK", "202211", "49"));
    validateRow(response, List.of("Female", "MODABSC", "jmIPBj66vD6", "202209", "48"));
    validateRow(response, List.of("Female", "MODDISCH", "jUb8gELQApl", "202208", "48"));
    validateRow(response, List.of("Female", "MODDIED", "at6UHUQatSo", "202209", "48"));
    validateRow(response, List.of("Female", "MODTRANS", "eIQbndfxQMb", "202209", "48"));
    validateRow(response, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "202207", "47"));
    validateRow(response, List.of("Female", "MODABSC", "eIQbndfxQMb", "202209", "47"));
    validateRow(response, List.of("Female", "MODABSC", "jmIPBj66vD6", "202210", "47"));
    validateRow(response, List.of("Female", "MODDIED", "Vth0fbpFcsO", "202211", "47"));
    validateRow(response, List.of("Female", "MODDIED", "eIQbndfxQMb", "202212", "47"));
    validateRow(response, List.of("Female", "MODDISCH", "fdc6uOvgoji", "202211", "47"));
    validateRow(response, List.of("Female", "MODTRANS", "jUb8gELQApl", "202207", "47"));
    validateRow(response, List.of("Female", "MODDIED", "fdc6uOvgoji", "202207", "47"));
    validateRow(response, List.of("Female", "MODABSC", "eIQbndfxQMb", "202211", "47"));
    validateRow(response, List.of("Female", "MODABSC", "TEQlaapDQoK", "202211", "47"));
    validateRow(response, List.of("Female", "MODDISCH", "jmIPBj66vD6", "202207", "47"));
    validateRow(response, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "202211", "47"));
    validateRow(response, List.of("Female", "MODTRANS", "fdc6uOvgoji", "202209", "46"));
    validateRow(response, List.of("Female", "MODTRANS", "jmIPBj66vD6", "202210", "46"));
    validateRow(response, List.of("Female", "MODTRANS", "TEQlaapDQoK", "202212", "46"));
    validateRow(response, List.of("Female", "MODDIED", "eIQbndfxQMb", "202211", "46"));
    validateRow(response, List.of("Female", "MODTRANS", "eIQbndfxQMb", "202207", "46"));
    validateRow(response, List.of("Female", "MODDISCH", "jmIPBj66vD6", "202212", "46"));
    validateRow(response, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "202209", "46"));
    validateRow(response, List.of("Female", "MODABSC", "at6UHUQatSo", "202210", "46"));
    validateRow(response, List.of("Female", "MODABSC", "TEQlaapDQoK", "202207", "46"));
    validateRow(response, List.of("Female", "MODDIED", "jmIPBj66vD6", "202209", "46"));
    validateRow(response, List.of("Female", "MODTRANS", "fdc6uOvgoji", "202211", "45"));
    validateRow(response, List.of("Female", "MODDISCH", "at6UHUQatSo", "202212", "45"));
    validateRow(response, List.of("Female", "MODTRANS", "eIQbndfxQMb", "202208", "45"));
    validateRow(response, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "202208", "45"));
    validateRow(response, List.of("Female", "MODDISCH", "jmIPBj66vD6", "202208", "45"));
    validateRow(response, List.of("Female", "MODABSC", "fdc6uOvgoji", "202212", "45"));
    validateRow(response, List.of("Female", "MODABSC", "Vth0fbpFcsO", "202207", "45"));
    validateRow(response, List.of("Female", "MODDIED", "fdc6uOvgoji", "202210", "44"));
    validateRow(response, List.of("Female", "MODABSC", "Vth0fbpFcsO", "202211", "44"));
    validateRow(response, List.of("Female", "MODDIED", "TEQlaapDQoK", "202212", "44"));
    validateRow(response, List.of("Female", "MODABSC", "at6UHUQatSo", "202211", "44"));
    validateRow(response, List.of("Female", "MODDIED", "jmIPBj66vD6", "202211", "44"));
    validateRow(response, List.of("Female", "MODDIED", "fdc6uOvgoji", "202209", "44"));
    validateRow(response, List.of("Female", "MODABSC", "eIQbndfxQMb", "202212", "44"));
    validateRow(response, List.of("Female", "MODDISCH", "eIQbndfxQMb", "202208", "44"));
    validateRow(response, List.of("Female", "MODDIED", "TEQlaapDQoK", "202210", "43"));
    validateRow(response, List.of("Female", "MODDISCH", "jUb8gELQApl", "202212", "43"));
    validateRow(response, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "202210", "43"));
    validateRow(response, List.of("Female", "MODDISCH", "fdc6uOvgoji", "202208", "43"));
    validateRow(response, List.of("Female", "MODDIED", "at6UHUQatSo", "202210", "43"));
    validateRow(response, List.of("Female", "MODDIED", "eIQbndfxQMb", "202210", "43"));
    validateRow(response, List.of("Female", "MODABSC", "qhqAxPSTUXp", "202211", "43"));
    validateRow(response, List.of("Female", "MODABSC", "fdc6uOvgoji", "202208", "43"));
    validateRow(response, List.of("Female", "MODDIED", "at6UHUQatSo", "202208", "43"));
    validateRow(response, List.of("Female", "MODTRANS", "jmIPBj66vD6", "202211", "43"));
    validateRow(response, List.of("Female", "MODTRANS", "at6UHUQatSo", "202212", "43"));
    validateRow(response, List.of("Female", "MODDISCH", "fdc6uOvgoji", "202209", "42"));
    validateRow(response, List.of("Female", "MODDIED", "Vth0fbpFcsO", "202212", "42"));
    validateRow(response, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "202211", "42"));
    validateRow(response, List.of("Female", "MODDIED", "TEQlaapDQoK", "202208", "42"));
    validateRow(response, List.of("Female", "MODABSC", "qhqAxPSTUXp", "202212", "42"));
    validateRow(response, List.of("Female", "MODDISCH", "at6UHUQatSo", "202207", "41"));
    validateRow(response, List.of("Female", "MODABSC", "jUb8gELQApl", "202210", "41"));
    validateRow(response, List.of("Female", "MODDIED", "fdc6uOvgoji", "202208", "41"));
    validateRow(response, List.of("Female", "MODABSC", "jmIPBj66vD6", "202212", "41"));
    validateRow(response, List.of("Female", "MODTRANS", "jUb8gELQApl", "202208", "41"));
    validateRow(response, List.of("Female", "MODDIED", "at6UHUQatSo", "202212", "41"));
    validateRow(response, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "202209", "41"));
    validateRow(response, List.of("Female", "MODDISCH", "TEQlaapDQoK", "202210", "41"));
    validateRow(response, List.of("Female", "MODABSC", "Vth0fbpFcsO", "202212", "41"));
    validateRow(response, List.of("Female", "MODTRANS", "fdc6uOvgoji", "202212", "41"));
    validateRow(response, List.of("Female", "MODDIED", "qhqAxPSTUXp", "202209", "40"));
    validateRow(response, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "202207", "40"));
    validateRow(response, List.of("Female", "MODABSC", "jmIPBj66vD6", "202208", "40"));
    validateRow(response, List.of("Female", "MODABSC", "jmIPBj66vD6", "202207", "40"));
    validateRow(response, List.of("Female", "MODDIED", "fdc6uOvgoji", "202212", "40"));
    validateRow(response, List.of("Female", "MODABSC", "eIQbndfxQMb", "202208", "40"));
    validateRow(response, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "202211", "40"));
    validateRow(response, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "202208", "40"));
    validateRow(response, List.of("Female", "MODABSC", "eIQbndfxQMb", "202207", "40"));
    validateRow(response, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "202212", "39"));
    validateRow(response, List.of("Female", "MODTRANS", "eIQbndfxQMb", "202211", "39"));
    validateRow(response, List.of("Female", "MODTRANS", "jUb8gELQApl", "202209", "39"));
    validateRow(response, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "202207", "39"));
    validateRow(response, List.of("Female", "MODDIED", "qhqAxPSTUXp", "202208", "39"));
    validateRow(response, List.of("Female", "MODDIED", "Vth0fbpFcsO", "202209", "39"));
    validateRow(response, List.of("Female", "MODABSC", "Vth0fbpFcsO", "202208", "39"));
    validateRow(response, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "202210", "39"));
    validateRow(response, List.of("Female", "MODABSC", "TEQlaapDQoK", "202212", "39"));
    validateRow(response, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "202212", "39"));
    validateRow(response, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "202212", "38"));
    validateRow(response, List.of("Female", "MODDIED", "qhqAxPSTUXp", "202211", "38"));
    validateRow(response, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "202210", "38"));
    validateRow(response, List.of("Female", "MODABSC", "Vth0fbpFcsO", "202210", "38"));
    validateRow(response, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "202211", "38"));
    validateRow(response, List.of("Female", "MODABSC", "qhqAxPSTUXp", "202210", "38"));
    validateRow(response, List.of("Female", "MODABSC", "jUb8gELQApl", "202211", "38"));
    validateRow(response, List.of("Female", "MODTRANS", "PMa2VCrupOd", "202207", "38"));
    validateRow(response, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "202208", "38"));
    validateRow(response, List.of("Female", "MODDIED", "Vth0fbpFcsO", "202207", "38"));
    validateRow(response, List.of("Female", "MODDIED", "jUb8gELQApl", "202210", "37"));
    validateRow(response, List.of("Female", "MODDIED", "qhqAxPSTUXp", "202207", "37"));
    validateRow(response, List.of("Female", "MODDIED", "Vth0fbpFcsO", "202210", "37"));
    validateRow(response, List.of("Female", "MODDISCH", "TEQlaapDQoK", "202212", "37"));
    validateRow(response, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "202208", "37"));
    validateRow(response, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "202212", "37"));
    validateRow(response, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "202210", "37"));
    validateRow(response, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "202211", "37"));
    validateRow(response, List.of("Female", "MODDIED", "jUb8gELQApl", "202211", "37"));
    validateRow(response, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "202208", "36"));
    validateRow(response, List.of("Female", "MODDIED", "jmIPBj66vD6", "202208", "36"));
    validateRow(response, List.of("Female", "MODDISCH", "jUb8gELQApl", "202209", "36"));
    validateRow(response, List.of("Female", "MODDIED", "jUb8gELQApl", "202212", "36"));
    validateRow(response, List.of("Female", "MODABSC", "qhqAxPSTUXp", "202207", "35"));
    validateRow(response, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "202208", "35"));
    validateRow(response, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "202210", "35"));
    validateRow(response, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "202209", "35"));
    validateRow(response, List.of("Female", "MODTRANS", "eIQbndfxQMb", "202212", "35"));
    validateRow(response, List.of("Female", "MODTRANS", "jUb8gELQApl", "202211", "35"));
    validateRow(response, List.of("Female", "MODDISCH", "eIQbndfxQMb", "202209", "34"));
    validateRow(response, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "202210", "34"));
    validateRow(response, List.of("Female", "MODABSC", "Vth0fbpFcsO", "202209", "34"));
    validateRow(response, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "202212", "34"));
    validateRow(response, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "202210", "34"));
    validateRow(response, List.of("Female", "MODDISCH", "PMa2VCrupOd", "202210", "34"));
    validateRow(response, List.of("Female", "MODDIED", "Vth0fbpFcsO", "202208", "34"));
    validateRow(response, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "202211", "33"));
    validateRow(response, List.of("Female", "MODDIED", "PMa2VCrupOd", "202209", "33"));
    validateRow(response, List.of("Female", "MODABSC", "PMa2VCrupOd", "202210", "33"));
    validateRow(response, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "202211", "33"));
    validateRow(response, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "202210", "33"));
    validateRow(response, List.of("Female", "MODDIED", "jUb8gELQApl", "202207", "33"));
    validateRow(response, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "202208", "33"));
    validateRow(response, List.of("Female", "MODDIED", "PMa2VCrupOd", "202210", "33"));
    validateRow(response, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "202209", "33"));
    validateRow(response, List.of("Female", "MODTRANS", "jmIPBj66vD6", "202208", "33"));
    validateRow(response, List.of("Female", "MODDISCH", "jUb8gELQApl", "202207", "33"));
    validateRow(response, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "202209", "32"));
    validateRow(response, List.of("Female", "MODABSC", "PMa2VCrupOd", "202212", "32"));
    validateRow(response, List.of("Female", "MODABSC", "PMa2VCrupOd", "202208", "32"));
    validateRow(response, List.of("Female", "MODDIED", "jUb8gELQApl", "202209", "32"));
    validateRow(response, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "202207", "32"));
    validateRow(response, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "202209", "32"));
    validateRow(response, List.of("Female", "MODABSC", "lc3eMKXaEfw", "202208", "32"));
    validateRow(response, List.of("Female", "MODDIED", "lc3eMKXaEfw", "202211", "32"));
    validateRow(response, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "202210", "31"));
    validateRow(response, List.of("Female", "MODTRANS", "jUb8gELQApl", "202212", "31"));
    validateRow(response, List.of("Female", "MODDIED", "PMa2VCrupOd", "202208", "31"));
    validateRow(response, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "202207", "31"));
    validateRow(response, List.of("Female", "MODDISCH", "jUb8gELQApl", "202210", "31"));
    validateRow(response, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "202209", "31"));
    validateRow(response, List.of("Female", "MODABSC", "jUb8gELQApl", "202207", "31"));
    validateRow(response, List.of("Female", "MODDISCH", "PMa2VCrupOd", "202207", "31"));
    validateRow(response, List.of("Female", "MODDIED", "PMa2VCrupOd", "202211", "30"));
    validateRow(response, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "202209", "30"));
    validateRow(response, List.of("Female", "MODABSC", "jUb8gELQApl", "202209", "30"));
    validateRow(response, List.of("Female", "MODABSC", "jUb8gELQApl", "202212", "30"));
    validateRow(response, List.of("Female", "MODABSC", "PMa2VCrupOd", "202211", "30"));
    validateRow(response, List.of("Female", "MODDIED", "eIQbndfxQMb", "202208", "30"));
    validateRow(response, List.of("Female", "MODABSC", "PMa2VCrupOd", "202207", "29"));
    validateRow(response, List.of("Female", "MODDISCH", "PMa2VCrupOd", "202212", "29"));
    validateRow(response, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "202209", "29"));
    validateRow(response, List.of("Female", "MODTRANS", "PMa2VCrupOd", "202209", "29"));
    validateRow(response, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "202211", "29"));
    validateRow(response, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "202207", "29"));
    validateRow(response, List.of("Female", "MODDIED", "qhqAxPSTUXp", "202212", "29"));
    validateRow(response, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "202207", "29"));
    validateRow(response, List.of("Female", "MODTRANS", "jUb8gELQApl", "202210", "28"));
    validateRow(response, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "202212", "28"));
    validateRow(response, List.of("Female", "MODDISCH", "PMa2VCrupOd", "202208", "28"));
    validateRow(response, List.of("Female", "MODABSC", "qhqAxPSTUXp", "202209", "27"));
    validateRow(response, List.of("Female", "MODABSC", "lc3eMKXaEfw", "202210", "27"));
    validateRow(response, List.of("Female", "MODDIED", "qhqAxPSTUXp", "202210", "27"));
    validateRow(response, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "202212", "27"));
    validateRow(response, List.of("Female", "MODDIED", "PMa2VCrupOd", "202207", "27"));
    validateRow(response, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "202209", "27"));
    validateRow(response, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "202209", "26"));
    validateRow(response, List.of("Female", "MODDISCH", "PMa2VCrupOd", "202211", "26"));
    validateRow(response, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "202208", "26"));
    validateRow(response, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "202207", "26"));
    validateRow(response, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "202211", "26"));
    validateRow(response, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "202212", "26"));
    validateRow(response, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "202212", "26"));
    validateRow(response, List.of("Female", "MODDIED", "PMa2VCrupOd", "202212", "26"));
    validateRow(response, List.of("Female", "MODABSC", "lc3eMKXaEfw", "202209", "26"));
    validateRow(response, List.of("Female", "MODTRANS", "PMa2VCrupOd", "202212", "26"));
    validateRow(response, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "202207", "25"));
    validateRow(response, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "202210", "25"));
    validateRow(response, List.of("Female", "MODDIED", "lc3eMKXaEfw", "202208", "25"));
    validateRow(response, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "202211", "25"));
    validateRow(response, List.of("Female", "MODDIED", "lc3eMKXaEfw", "202207", "24"));
    validateRow(response, List.of("Female", "MODDISCH", "jUb8gELQApl", "202211", "24"));
    validateRow(response, List.of("Female", "MODTRANS", "PMa2VCrupOd", "202211", "23"));
    validateRow(response, List.of("Female", "MODTRANS", "PMa2VCrupOd", "202210", "23"));
    validateRow(response, List.of("Female", "MODDIED", "lc3eMKXaEfw", "202212", "23"));
    validateRow(response, List.of("Female", "MODABSC", "qhqAxPSTUXp", "202208", "23"));
    validateRow(response, List.of("Female", "MODABSC", "PMa2VCrupOd", "202209", "23"));
    validateRow(response, List.of("Female", "MODTRANS", "PMa2VCrupOd", "202208", "23"));
    validateRow(response, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "202211", "22"));
    validateRow(response, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "202212", "22"));
    validateRow(response, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "202207", "21"));
    validateRow(response, List.of("Female", "MODABSC", "lc3eMKXaEfw", "202207", "21"));
    validateRow(response, List.of("Female", "MODDIED", "lc3eMKXaEfw", "202210", "20"));
    validateRow(response, List.of("Female", "MODABSC", "lc3eMKXaEfw", "202211", "20"));
    validateRow(response, List.of("Female", "MODDIED", "lc3eMKXaEfw", "202209", "20"));
    validateRow(response, List.of("Female", "MODDISCH", "PMa2VCrupOd", "202209", "19"));
    validateRow(response, List.of("Female", "MODABSC", "lc3eMKXaEfw", "202212", "19"));
    validateRow(response, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "202208", "19"));
    validateRow(response, List.of("", "MODTRANS", "O6uvpzGd5pu", "202208", "1"));
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
    validateRow(response, List.of("Male", "MODDISCH", "O6uvpzGd5pu", "2021", "739"));
    validateRow(response, List.of("Male", "MODDIED", "O6uvpzGd5pu", "2021", "734"));
    validateRow(response, List.of("Male", "MODABSC", "O6uvpzGd5pu", "2021", "724"));
    validateRow(response, List.of("Male", "MODDIED", "TEQlaapDQoK", "2021", "710"));
    validateRow(response, List.of("Male", "MODDISCH", "kJq2mPyFEHo", "2021", "705"));
    validateRow(response, List.of("Male", "MODTRANS", "O6uvpzGd5pu", "2021", "701"));
    validateRow(response, List.of("Male", "MODTRANS", "kJq2mPyFEHo", "2021", "693"));
    validateRow(response, List.of("Male", "MODABSC", "kJq2mPyFEHo", "2021", "687"));
    validateRow(response, List.of("Male", "MODABSC", "TEQlaapDQoK", "2021", "677"));
    validateRow(response, List.of("Male", "MODDIED", "kJq2mPyFEHo", "2021", "675"));
    validateRow(response, List.of("Male", "MODDISCH", "TEQlaapDQoK", "2021", "666"));
    validateRow(response, List.of("Male", "MODTRANS", "TEQlaapDQoK", "2021", "659"));
    validateRow(response, List.of("Male", "MODDISCH", "fdc6uOvgoji", "2021", "646"));
    validateRow(response, List.of("Male", "MODABSC", "at6UHUQatSo", "2021", "639"));
    validateRow(response, List.of("Male", "MODTRANS", "at6UHUQatSo", "2021", "628"));
    validateRow(response, List.of("Male", "MODDIED", "at6UHUQatSo", "2021", "585"));
    validateRow(response, List.of("Male", "MODTRANS", "eIQbndfxQMb", "2021", "571"));
    validateRow(response, List.of("Male", "MODTRANS", "fdc6uOvgoji", "2021", "570"));
    validateRow(response, List.of("Male", "MODABSC", "fdc6uOvgoji", "2021", "559"));
    validateRow(response, List.of("Male", "MODDIED", "fdc6uOvgoji", "2021", "559"));
    validateRow(response, List.of("Male", "MODABSC", "jmIPBj66vD6", "2021", "552"));
    validateRow(response, List.of("Male", "MODDISCH", "eIQbndfxQMb", "2021", "547"));
    validateRow(response, List.of("Male", "MODDISCH", "jmIPBj66vD6", "2021", "542"));
    validateRow(response, List.of("Male", "MODDISCH", "at6UHUQatSo", "2021", "542"));
    validateRow(response, List.of("Male", "MODDIED", "eIQbndfxQMb", "2021", "526"));
    validateRow(response, List.of("Male", "MODDIED", "jmIPBj66vD6", "2021", "521"));
    validateRow(response, List.of("Male", "MODDIED", "Vth0fbpFcsO", "2021", "509"));
    validateRow(response, List.of("Male", "MODTRANS", "jmIPBj66vD6", "2021", "503"));
    validateRow(response, List.of("Male", "MODABSC", "Vth0fbpFcsO", "2021", "500"));
    validateRow(response, List.of("Male", "MODDISCH", "Vth0fbpFcsO", "2021", "491"));
    validateRow(response, List.of("Male", "MODABSC", "eIQbndfxQMb", "2021", "480"));
    validateRow(response, List.of("Male", "MODTRANS", "Vth0fbpFcsO", "2021", "477"));
    validateRow(response, List.of("Male", "MODDISCH", "jUb8gELQApl", "2021", "462"));
    validateRow(response, List.of("Male", "MODABSC", "jUb8gELQApl", "2021", "459"));
    validateRow(response, List.of("Male", "MODTRANS", "jUb8gELQApl", "2021", "442"));
    validateRow(response, List.of("Male", "MODDIED", "jUb8gELQApl", "2021", "424"));
    validateRow(response, List.of("Male", "MODDIED", "qhqAxPSTUXp", "2021", "423"));
    validateRow(response, List.of("Male", "MODABSC", "qhqAxPSTUXp", "2021", "403"));
    validateRow(response, List.of("Male", "MODTRANS", "qhqAxPSTUXp", "2021", "390"));
    validateRow(response, List.of("Male", "MODDISCH", "qhqAxPSTUXp", "2021", "386"));
    validateRow(response, List.of("Male", "MODABSC", "bL4ooGhyHRQ", "2021", "382"));
    validateRow(response, List.of("Male", "MODABSC", "PMa2VCrupOd", "2021", "378"));
    validateRow(response, List.of("Male", "MODDIED", "bL4ooGhyHRQ", "2021", "378"));
    validateRow(response, List.of("Male", "MODDIED", "PMa2VCrupOd", "2021", "377"));
    validateRow(response, List.of("Male", "MODABSC", "lc3eMKXaEfw", "2021", "366"));
    validateRow(response, List.of("Male", "MODTRANS", "PMa2VCrupOd", "2021", "361"));
    validateRow(response, List.of("Male", "MODTRANS", "bL4ooGhyHRQ", "2021", "358"));
    validateRow(response, List.of("Male", "MODDISCH", "bL4ooGhyHRQ", "2021", "345"));
    validateRow(response, List.of("Male", "MODDIED", "lc3eMKXaEfw", "2021", "331"));
    validateRow(response, List.of("Male", "MODDISCH", "PMa2VCrupOd", "2021", "322"));
    validateRow(response, List.of("Male", "MODTRANS", "lc3eMKXaEfw", "2021", "320"));
    validateRow(response, List.of("Male", "MODDISCH", "lc3eMKXaEfw", "2021", "317"));
    validateRow(response, List.of("Male", "", "O6uvpzGd5pu", "2021", "11"));
    validateRow(response, List.of("Female", "MODABSC", "kJq2mPyFEHo", "2021", "741"));
    validateRow(response, List.of("Female", "MODDIED", "O6uvpzGd5pu", "2021", "733"));
    validateRow(response, List.of("Female", "MODABSC", "O6uvpzGd5pu", "2021", "720"));
    validateRow(response, List.of("Female", "MODTRANS", "O6uvpzGd5pu", "2021", "715"));
    validateRow(response, List.of("Female", "MODDISCH", "O6uvpzGd5pu", "2021", "705"));
    validateRow(response, List.of("Female", "MODDIED", "kJq2mPyFEHo", "2021", "693"));
    validateRow(response, List.of("Female", "MODDIED", "TEQlaapDQoK", "2021", "680"));
    validateRow(response, List.of("Female", "MODTRANS", "TEQlaapDQoK", "2021", "673"));
    validateRow(response, List.of("Female", "MODTRANS", "kJq2mPyFEHo", "2021", "668"));
    validateRow(response, List.of("Female", "MODDISCH", "kJq2mPyFEHo", "2021", "663"));
    validateRow(response, List.of("Female", "MODDISCH", "TEQlaapDQoK", "2021", "652"));
    validateRow(response, List.of("Female", "MODABSC", "TEQlaapDQoK", "2021", "652"));
    validateRow(response, List.of("Female", "MODABSC", "at6UHUQatSo", "2021", "624"));
    validateRow(response, List.of("Female", "MODTRANS", "fdc6uOvgoji", "2021", "601"));
    validateRow(response, List.of("Female", "MODDISCH", "at6UHUQatSo", "2021", "600"));
    validateRow(response, List.of("Female", "MODDIED", "at6UHUQatSo", "2021", "593"));
    validateRow(response, List.of("Female", "MODDIED", "fdc6uOvgoji", "2021", "582"));
    validateRow(response, List.of("Female", "MODABSC", "fdc6uOvgoji", "2021", "580"));
    validateRow(response, List.of("Female", "MODTRANS", "at6UHUQatSo", "2021", "579"));
    validateRow(response, List.of("Female", "MODDISCH", "eIQbndfxQMb", "2021", "567"));
    validateRow(response, List.of("Female", "MODABSC", "jmIPBj66vD6", "2021", "559"));
    validateRow(response, List.of("Female", "MODDISCH", "fdc6uOvgoji", "2021", "553"));
    validateRow(response, List.of("Female", "MODTRANS", "jmIPBj66vD6", "2021", "538"));
    validateRow(response, List.of("Female", "MODDISCH", "jmIPBj66vD6", "2021", "525"));
    validateRow(response, List.of("Female", "MODDIED", "eIQbndfxQMb", "2021", "519"));
    validateRow(response, List.of("Female", "MODDISCH", "Vth0fbpFcsO", "2021", "516"));
    validateRow(response, List.of("Female", "MODTRANS", "eIQbndfxQMb", "2021", "511"));
    validateRow(response, List.of("Female", "MODABSC", "eIQbndfxQMb", "2021", "510"));
    validateRow(response, List.of("Female", "MODDIED", "jmIPBj66vD6", "2021", "498"));
    validateRow(response, List.of("Female", "MODABSC", "Vth0fbpFcsO", "2021", "484"));
    validateRow(response, List.of("Female", "MODTRANS", "Vth0fbpFcsO", "2021", "469"));
    validateRow(response, List.of("Female", "MODDIED", "jUb8gELQApl", "2021", "465"));
    validateRow(response, List.of("Female", "MODDISCH", "jUb8gELQApl", "2021", "464"));
    validateRow(response, List.of("Female", "MODDIED", "Vth0fbpFcsO", "2021", "463"));
    validateRow(response, List.of("Female", "MODABSC", "jUb8gELQApl", "2021", "462"));
    validateRow(response, List.of("Female", "MODTRANS", "jUb8gELQApl", "2021", "429"));
    validateRow(response, List.of("Female", "MODDISCH", "qhqAxPSTUXp", "2021", "404"));
    validateRow(response, List.of("Female", "MODDISCH", "bL4ooGhyHRQ", "2021", "404"));
    validateRow(response, List.of("Female", "MODTRANS", "qhqAxPSTUXp", "2021", "391"));
    validateRow(response, List.of("Female", "MODABSC", "qhqAxPSTUXp", "2021", "388"));
    validateRow(response, List.of("Female", "MODDIED", "qhqAxPSTUXp", "2021", "385"));
    validateRow(response, List.of("Female", "MODDIED", "bL4ooGhyHRQ", "2021", "384"));
    validateRow(response, List.of("Female", "MODTRANS", "bL4ooGhyHRQ", "2021", "379"));
    validateRow(response, List.of("Female", "MODABSC", "bL4ooGhyHRQ", "2021", "375"));
    validateRow(response, List.of("Female", "MODABSC", "PMa2VCrupOd", "2021", "355"));
    validateRow(response, List.of("Female", "MODABSC", "lc3eMKXaEfw", "2021", "354"));
    validateRow(response, List.of("Female", "MODTRANS", "PMa2VCrupOd", "2021", "352"));
    validateRow(response, List.of("Female", "MODDIED", "lc3eMKXaEfw", "2021", "335"));
    validateRow(response, List.of("Female", "MODDISCH", "PMa2VCrupOd", "2021", "332"));
    validateRow(response, List.of("Female", "MODDISCH", "lc3eMKXaEfw", "2021", "331"));
    validateRow(response, List.of("Female", "MODDIED", "PMa2VCrupOd", "2021", "330"));
    validateRow(response, List.of("Female", "MODTRANS", "lc3eMKXaEfw", "2021", "327"));
    validateRow(response, List.of("Female", "", "O6uvpzGd5pu", "2021", "2"));
    validateRow(response, List.of("", "MODTRANS", "O6uvpzGd5pu", "2021", "1"));
    validateRow(response, List.of("", "MODDISCH", "O6uvpzGd5pu", "2021", "1"));
  }
}
