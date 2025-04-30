/*
 * Copyright (c) 2004-2023, University of Oslo
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
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/analytics" aggregate endpoint. */
public class AnalyticsQueryDv10AutoTest extends AnalyticsApiTest {

  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void queryStillBirthsByFacilityType() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:THIS_YEAR;LAST_5_YEARS,ou:ImspTQPwCqd,dx:HZSdnO5fCUc")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=J5jldMd8OHv:uYxK4wmcPqA;tDZVQ1WtwpA;EYbopBOJWsW;RXL3lPSK8oG;CXw2yu5fodb")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"s3jlIgFCbUb\":{\"name\":\"Untrained TBA, At PHU\"},\"mcwaItVPoeA\":{\"name\":\"MCH Aides, Male\"},\"J5jldMd8OHv\":{\"name\":\"Facility Type\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"HVFCn1yjoGx\":{\"name\":\"Untrained TBA, In Community\"},\"uYxK4wmcPqA\":{\"name\":\"CHP\"},\"CYHGl18lR0W\":{\"name\":\"Trained TBA, In Community\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"YEmiuCcgNQI\":{\"name\":\"SECHN, At PHU\"},\"tDZVQ1WtwpA\":{\"name\":\"Hospital\"},\"w0tNkmER8Lb\":{\"name\":\"Midwives, Male\"},\"dx\":{\"name\":\"Data\"},\"NZAKyj67WW2\":{\"name\":\"SECHN, Male\"},\"PvFFIeF2J9x\":{\"name\":\"CHO, At PHU\"},\"Gqs7snASTtF\":{\"name\":\"MCH Aides, Female\"},\"Ifqide84xSh\":{\"name\":\"SECHN, Female\"},\"quwbAxfBdQU\":{\"name\":\"Trained TBA, Female\"},\"Tk8loZNEo4E\":{\"name\":\"Trained TBA, Male\"},\"w3BcsdjOcfk\":{\"name\":\"CHO, In Community\"},\"RXL3lPSK8oG\":{\"name\":\"Clinic\"},\"Tzg8I9KyEwd\":{\"name\":\"Midwives, At PHU\"},\"ou\":{\"name\":\"Organisation unit\"},\"AKQ15Z2uqit\":{\"name\":\"Midwives, In Community\"},\"CXw2yu5fodb\":{\"name\":\"CHC\"},\"2022\":{\"name\":\"2022\"},\"MaXD86iob3M\":{\"name\":\"SECHN, In Community\"},\"Gmbgme7z9BF\":{\"name\":\"Trained TBA, At PHU\"},\"2021\":{\"name\":\"2021\"},\"SeTGOtrbip1\":{\"name\":\"Untrained TBA, Male\"},\"2020\":{\"name\":\"2020\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"b19Ye0TWs1D\":{\"name\":\"MCH Aides, In Community\"},\"2019\":{\"name\":\"2019\"},\"L4P9VSgHkF6\":{\"name\":\"MCH Aides, At PHU\"},\"2018\":{\"name\":\"2018\"},\"2017\":{\"name\":\"2017\"},\"RgrNGmlMOAJ\":{\"name\":\"CHO, Female\"},\"pe\":{\"name\":\"Period\"},\"vP9xV78M67W\":{\"name\":\"Untrained TBA, Female\"},\"EYbopBOJWsW\":{\"name\":\"MCHP\"},\"HZSdnO5fCUc\":{\"name\":\"Still births\"},\"R3oRRwMRfMT\":{\"name\":\"CHO, Male\"},\"xQbMVHxaUiW\":{\"name\":\"Midwives, Female\"}},\"dimensions\":{\"dx\":[\"HZSdnO5fCUc\"],\"pe\":[\"2017\",\"2018\",\"2019\",\"2020\",\"2021\",\"2022\"],\"J5jldMd8OHv\":[\"uYxK4wmcPqA\",\"tDZVQ1WtwpA\",\"EYbopBOJWsW\",\"RXL3lPSK8oG\",\"CXw2yu5fodb\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"w3BcsdjOcfk\",\"Tzg8I9KyEwd\",\"s3jlIgFCbUb\",\"mcwaItVPoeA\",\"AKQ15Z2uqit\",\"HVFCn1yjoGx\",\"MaXD86iob3M\",\"Gmbgme7z9BF\",\"SeTGOtrbip1\",\"b19Ye0TWs1D\",\"CYHGl18lR0W\",\"YEmiuCcgNQI\",\"L4P9VSgHkF6\",\"w0tNkmER8Lb\",\"RgrNGmlMOAJ\",\"NZAKyj67WW2\",\"vP9xV78M67W\",\"PvFFIeF2J9x\",\"Gqs7snASTtF\",\"Ifqide84xSh\",\"quwbAxfBdQU\",\"Tk8loZNEo4E\",\"R3oRRwMRfMT\",\"xQbMVHxaUiW\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "J5jldMd8OHv", "Facility Type", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("RXL3lPSK8oG", "142"));
    validateRow(response, List.of("tDZVQ1WtwpA", "264"));
    validateRow(response, List.of("EYbopBOJWsW", "2432"));
    validateRow(response, List.of("uYxK4wmcPqA", "696"));
    validateRow(response, List.of("CXw2yu5fodb", "1418"));
  }

  @Test
  public void queryIndicatorsLast12MonthsRadar() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:ImspTQPwCqd")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=dx:FbKK4ofIv5R;tUIlpyeeX9N;YlTWksXEhEO;eTDtyyaSA7f;d9thHOJMROr;n5nS0SmkUpq;JoEzWYGdX7s,pe:LAST_12_MONTHS")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(84)))
        .body("height", equalTo(84))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"FbKK4ofIv5R\":{\"name\":\"Measles Coverage <1y\"},\"202109\":{\"name\":\"September 2021\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"YlTWksXEhEO\":{\"name\":\"OPV 1 Coverage <1y\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202112\":{\"name\":\"December 2021\"},\"d9thHOJMROr\":{\"name\":\"Dropout rate Penta 1 - Measles\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"JoEzWYGdX7s\":{\"name\":\"OPV 3 Coverage <1y\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"tUIlpyeeX9N\":{\"name\":\"Penta 3 Coverage <1y\"},\"ou\":{\"name\":\"Organisation unit\"},\"n5nS0SmkUpq\":{\"name\":\"OPV 0 Coverage <1y\"},\"eTDtyyaSA7f\":{\"name\":\"FIC <1y\"},\"202101\":{\"name\":\"January 2021\"},\"202102\":{\"name\":\"February 2021\"},\"pe\":{\"name\":\"Period\"}},\"dimensions\":{\"dx\":[\"FbKK4ofIv5R\",\"tUIlpyeeX9N\",\"YlTWksXEhEO\",\"eTDtyyaSA7f\",\"d9thHOJMROr\",\"n5nS0SmkUpq\",\"JoEzWYGdX7s\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("FbKK4ofIv5R", "202101", "23.04"));
    validateRow(response, List.of("FbKK4ofIv5R", "202102", "28.06"));
    validateRow(response, List.of("FbKK4ofIv5R", "202103", "25.54"));
    validateRow(response, List.of("FbKK4ofIv5R", "202104", "27.06"));
    validateRow(response, List.of("FbKK4ofIv5R", "202105", "28.69"));
    validateRow(response, List.of("FbKK4ofIv5R", "202106", "26.23"));
    validateRow(response, List.of("FbKK4ofIv5R", "202107", "24.11"));
    validateRow(response, List.of("FbKK4ofIv5R", "202108", "30.15"));
    validateRow(response, List.of("FbKK4ofIv5R", "202109", "31.32"));
    validateRow(response, List.of("FbKK4ofIv5R", "202110", "26.36"));
    validateRow(response, List.of("FbKK4ofIv5R", "202111", "31.2"));
    validateRow(response, List.of("FbKK4ofIv5R", "202112", "18.62"));
    validateRow(response, List.of("tUIlpyeeX9N", "202101", "23.88"));
    validateRow(response, List.of("tUIlpyeeX9N", "202102", "29.34"));
    validateRow(response, List.of("tUIlpyeeX9N", "202103", "27.0"));
    validateRow(response, List.of("tUIlpyeeX9N", "202104", "27.91"));
    validateRow(response, List.of("tUIlpyeeX9N", "202105", "29.7"));
    validateRow(response, List.of("tUIlpyeeX9N", "202106", "28.97"));
    validateRow(response, List.of("tUIlpyeeX9N", "202107", "27.53"));
    validateRow(response, List.of("tUIlpyeeX9N", "202108", "32.88"));
    validateRow(response, List.of("tUIlpyeeX9N", "202109", "31.77"));
    validateRow(response, List.of("tUIlpyeeX9N", "202110", "26.73"));
    validateRow(response, List.of("tUIlpyeeX9N", "202111", "30.72"));
    validateRow(response, List.of("tUIlpyeeX9N", "202112", "19.66"));
    validateRow(response, List.of("YlTWksXEhEO", "202101", "33.0"));
    validateRow(response, List.of("YlTWksXEhEO", "202102", "38.22"));
    validateRow(response, List.of("YlTWksXEhEO", "202103", "32.0"));
    validateRow(response, List.of("YlTWksXEhEO", "202104", "33.84"));
    validateRow(response, List.of("YlTWksXEhEO", "202105", "34.79"));
    validateRow(response, List.of("YlTWksXEhEO", "202106", "36.58"));
    validateRow(response, List.of("YlTWksXEhEO", "202107", "34.25"));
    validateRow(response, List.of("YlTWksXEhEO", "202108", "37.24"));
    validateRow(response, List.of("YlTWksXEhEO", "202109", "35.76"));
    validateRow(response, List.of("YlTWksXEhEO", "202110", "30.6"));
    validateRow(response, List.of("YlTWksXEhEO", "202111", "33.24"));
    validateRow(response, List.of("YlTWksXEhEO", "202112", "23.99"));
    validateRow(response, List.of("eTDtyyaSA7f", "202101", "22.79"));
    validateRow(response, List.of("eTDtyyaSA7f", "202102", "26.64"));
    validateRow(response, List.of("eTDtyyaSA7f", "202103", "24.32"));
    validateRow(response, List.of("eTDtyyaSA7f", "202104", "24.91"));
    validateRow(response, List.of("eTDtyyaSA7f", "202105", "25.7"));
    validateRow(response, List.of("eTDtyyaSA7f", "202106", "22.9"));
    validateRow(response, List.of("eTDtyyaSA7f", "202107", "22.0"));
    validateRow(response, List.of("eTDtyyaSA7f", "202108", "27.03"));
    validateRow(response, List.of("eTDtyyaSA7f", "202109", "26.61"));
    validateRow(response, List.of("eTDtyyaSA7f", "202110", "24.29"));
    validateRow(response, List.of("eTDtyyaSA7f", "202111", "26.77"));
    validateRow(response, List.of("eTDtyyaSA7f", "202112", "17.53"));
    validateRow(response, List.of("d9thHOJMROr", "202101", "30.27"));
    validateRow(response, List.of("d9thHOJMROr", "202102", "26.19"));
    validateRow(response, List.of("d9thHOJMROr", "202103", "20.73"));
    validateRow(response, List.of("d9thHOJMROr", "202104", "21.64"));
    validateRow(response, List.of("d9thHOJMROr", "202105", "17.89"));
    validateRow(response, List.of("d9thHOJMROr", "202106", "28.44"));
    validateRow(response, List.of("d9thHOJMROr", "202107", "29.73"));
    validateRow(response, List.of("d9thHOJMROr", "202108", "19.71"));
    validateRow(response, List.of("d9thHOJMROr", "202109", "13.04"));
    validateRow(response, List.of("d9thHOJMROr", "202110", "12.77"));
    validateRow(response, List.of("d9thHOJMROr", "202111", "6.88"));
    validateRow(response, List.of("d9thHOJMROr", "202112", "25.11"));
    validateRow(response, List.of("n5nS0SmkUpq", "202101", "31.85"));
    validateRow(response, List.of("n5nS0SmkUpq", "202102", "37.36"));
    validateRow(response, List.of("n5nS0SmkUpq", "202103", "32.52"));
    validateRow(response, List.of("n5nS0SmkUpq", "202104", "33.61"));
    validateRow(response, List.of("n5nS0SmkUpq", "202105", "35.26"));
    validateRow(response, List.of("n5nS0SmkUpq", "202106", "35.52"));
    validateRow(response, List.of("n5nS0SmkUpq", "202107", "33.78"));
    validateRow(response, List.of("n5nS0SmkUpq", "202108", "31.22"));
    validateRow(response, List.of("n5nS0SmkUpq", "202109", "34.22"));
    validateRow(response, List.of("n5nS0SmkUpq", "202110", "28.77"));
    validateRow(response, List.of("n5nS0SmkUpq", "202111", "31.01"));
    validateRow(response, List.of("n5nS0SmkUpq", "202112", "27.55"));
    validateRow(response, List.of("JoEzWYGdX7s", "202101", "23.94"));
    validateRow(response, List.of("JoEzWYGdX7s", "202102", "30.23"));
    validateRow(response, List.of("JoEzWYGdX7s", "202103", "26.96"));
    validateRow(response, List.of("JoEzWYGdX7s", "202104", "27.15"));
    validateRow(response, List.of("JoEzWYGdX7s", "202105", "29.1"));
    validateRow(response, List.of("JoEzWYGdX7s", "202106", "27.57"));
    validateRow(response, List.of("JoEzWYGdX7s", "202107", "27.77"));
    validateRow(response, List.of("JoEzWYGdX7s", "202108", "32.53"));
    validateRow(response, List.of("JoEzWYGdX7s", "202109", "31.79"));
    validateRow(response, List.of("JoEzWYGdX7s", "202110", "26.28"));
    validateRow(response, List.of("JoEzWYGdX7s", "202111", "29.89"));
    validateRow(response, List.of("JoEzWYGdX7s", "202112", "18.24"));
  }

  @Test
  public void queryDropoutRateVCoveragePerDistrict() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:LAST_12_MONTHS")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:ReUHfIn0pTQ;Uvn6LCg7dVU,ou:ImspTQPwCqd;LEVEL-wjP19dkFeIk")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(26)))
        .body("height", equalTo(26))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"202109\":{\"name\":\"September 2021\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"wjP19dkFeIk\":{\"uid\":\"wjP19dkFeIk\",\"name\":\"District\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202112\":{\"name\":\"December 2021\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\"},\"202110\":{\"name\":\"October 2021\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"202101\":{\"name\":\"January 2021\"},\"202102\":{\"name\":\"February 2021\"},\"pe\":{\"name\":\"Period\"},\"ReUHfIn0pTQ\":{\"name\":\"ANC 1-3 Dropout Rate\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"ReUHfIn0pTQ\",\"Uvn6LCg7dVU\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("ReUHfIn0pTQ", "O6uvpzGd5pu", "35.66"));
    validateRow(response, List.of("ReUHfIn0pTQ", "fdc6uOvgoji", "37.98"));
    validateRow(response, List.of("ReUHfIn0pTQ", "lc3eMKXaEfw", "33.63"));
    validateRow(response, List.of("ReUHfIn0pTQ", "jUb8gELQApl", "13.01"));
    validateRow(response, List.of("ReUHfIn0pTQ", "PMa2VCrupOd", "36.63"));
    validateRow(response, List.of("ReUHfIn0pTQ", "kJq2mPyFEHo", "8.09"));
    validateRow(response, List.of("ReUHfIn0pTQ", "qhqAxPSTUXp", "42.11"));
    validateRow(response, List.of("ReUHfIn0pTQ", "Vth0fbpFcsO", "30.0"));
    validateRow(response, List.of("ReUHfIn0pTQ", "jmIPBj66vD6", "21.95"));
    validateRow(response, List.of("ReUHfIn0pTQ", "TEQlaapDQoK", "51.95"));
    validateRow(response, List.of("ReUHfIn0pTQ", "bL4ooGhyHRQ", "35.7"));
    validateRow(response, List.of("ReUHfIn0pTQ", "eIQbndfxQMb", "52.95"));
    validateRow(response, List.of("ReUHfIn0pTQ", "at6UHUQatSo", "41.58"));
    validateRow(response, List.of("Uvn6LCg7dVU", "O6uvpzGd5pu", "146.73"));
    validateRow(response, List.of("Uvn6LCg7dVU", "fdc6uOvgoji", "83.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "lc3eMKXaEfw", "91.83"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jUb8gELQApl", "83.22"));
    validateRow(response, List.of("Uvn6LCg7dVU", "PMa2VCrupOd", "104.92"));
    validateRow(response, List.of("Uvn6LCg7dVU", "kJq2mPyFEHo", "96.32"));
    validateRow(response, List.of("Uvn6LCg7dVU", "qhqAxPSTUXp", "68.31"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Vth0fbpFcsO", "53.81"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jmIPBj66vD6", "120.81"));
    validateRow(response, List.of("Uvn6LCg7dVU", "TEQlaapDQoK", "101.49"));
    validateRow(response, List.of("Uvn6LCg7dVU", "bL4ooGhyHRQ", "90.33"));
    validateRow(response, List.of("Uvn6LCg7dVU", "eIQbndfxQMb", "127.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "at6UHUQatSo", "127.19"));
  }

  @Test
  public void queryMalnutritionIndicatorsStackedArea() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:2022")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=dx:X3taFC1HtE5;vhzPbO1eEyr;joIQbN4L1Ok;aGByu8NFs9m,ou:at6UHUQatSo;bL4ooGhyHRQ;TEQlaapDQoK;O6uvpzGd5pu;kJq2mPyFEHo;jUb8gELQApl;eIQbndfxQMb;Vth0fbpFcsO;fdc6uOvgoji;jmIPBj66vD6;ImspTQPwCqd;lc3eMKXaEfw;qhqAxPSTUXp;PMa2VCrupOd")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(56)))
        .body("height", equalTo(56))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"aGByu8NFs9m\":{\"name\":\"Well nourished rate\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"2022\":{\"name\":\"2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"dx\":{\"name\":\"Data\"},\"joIQbN4L1Ok\":{\"name\":\"Moderate malnutrition rate\"},\"pe\":{\"name\":\"Period\"},\"X3taFC1HtE5\":{\"name\":\"Exclusive breast feeding at Penta 3\"},\"vhzPbO1eEyr\":{\"name\":\"Severe malnutrition rate\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"}},\"dimensions\":{\"dx\":[\"X3taFC1HtE5\",\"vhzPbO1eEyr\",\"joIQbN4L1Ok\",\"aGByu8NFs9m\"],\"pe\":[\"2022\"],\"ou\":[\"at6UHUQatSo\",\"bL4ooGhyHRQ\",\"TEQlaapDQoK\",\"O6uvpzGd5pu\",\"kJq2mPyFEHo\",\"jUb8gELQApl\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"fdc6uOvgoji\",\"jmIPBj66vD6\",\"ImspTQPwCqd\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"PMa2VCrupOd\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("X3taFC1HtE5", "at6UHUQatSo", "63.69"));
    validateRow(response, List.of("X3taFC1HtE5", "bL4ooGhyHRQ", "83.04"));
    validateRow(response, List.of("X3taFC1HtE5", "TEQlaapDQoK", "53.97"));
    validateRow(response, List.of("X3taFC1HtE5", "O6uvpzGd5pu", "60.96"));
    validateRow(response, List.of("X3taFC1HtE5", "kJq2mPyFEHo", "60.84"));
    validateRow(response, List.of("X3taFC1HtE5", "jUb8gELQApl", "68.14"));
    validateRow(response, List.of("X3taFC1HtE5", "eIQbndfxQMb", "101.91"));
    validateRow(response, List.of("X3taFC1HtE5", "Vth0fbpFcsO", "45.56"));
    validateRow(response, List.of("X3taFC1HtE5", "fdc6uOvgoji", "64.14"));
    validateRow(response, List.of("X3taFC1HtE5", "jmIPBj66vD6", "84.86"));
    validateRow(response, List.of("X3taFC1HtE5", "ImspTQPwCqd", "65.98"));
    validateRow(response, List.of("X3taFC1HtE5", "lc3eMKXaEfw", "65.21"));
    validateRow(response, List.of("X3taFC1HtE5", "qhqAxPSTUXp", "67.68"));
    validateRow(response, List.of("X3taFC1HtE5", "PMa2VCrupOd", "59.23"));
    validateRow(response, List.of("vhzPbO1eEyr", "at6UHUQatSo", "7.07"));
    validateRow(response, List.of("vhzPbO1eEyr", "bL4ooGhyHRQ", "2.39"));
    validateRow(response, List.of("vhzPbO1eEyr", "TEQlaapDQoK", "5.06"));
    validateRow(response, List.of("vhzPbO1eEyr", "O6uvpzGd5pu", "5.52"));
    validateRow(response, List.of("vhzPbO1eEyr", "kJq2mPyFEHo", "6.08"));
    validateRow(response, List.of("vhzPbO1eEyr", "jUb8gELQApl", "3.52"));
    validateRow(response, List.of("vhzPbO1eEyr", "eIQbndfxQMb", "8.18"));
    validateRow(response, List.of("vhzPbO1eEyr", "Vth0fbpFcsO", "9.91"));
    validateRow(response, List.of("vhzPbO1eEyr", "fdc6uOvgoji", "2.72"));
    validateRow(response, List.of("vhzPbO1eEyr", "jmIPBj66vD6", "12.9"));
    validateRow(response, List.of("vhzPbO1eEyr", "ImspTQPwCqd", "6.35"));
    validateRow(response, List.of("vhzPbO1eEyr", "lc3eMKXaEfw", "5.84"));
    validateRow(response, List.of("vhzPbO1eEyr", "qhqAxPSTUXp", "8.23"));
    validateRow(response, List.of("vhzPbO1eEyr", "PMa2VCrupOd", "7.19"));
    validateRow(response, List.of("joIQbN4L1Ok", "at6UHUQatSo", "16.97"));
    validateRow(response, List.of("joIQbN4L1Ok", "bL4ooGhyHRQ", "15.42"));
    validateRow(response, List.of("joIQbN4L1Ok", "TEQlaapDQoK", "20.68"));
    validateRow(response, List.of("joIQbN4L1Ok", "O6uvpzGd5pu", "19.58"));
    validateRow(response, List.of("joIQbN4L1Ok", "kJq2mPyFEHo", "22.01"));
    validateRow(response, List.of("joIQbN4L1Ok", "jUb8gELQApl", "16.99"));
    validateRow(response, List.of("joIQbN4L1Ok", "eIQbndfxQMb", "20.34"));
    validateRow(response, List.of("joIQbN4L1Ok", "Vth0fbpFcsO", "21.04"));
    validateRow(response, List.of("joIQbN4L1Ok", "fdc6uOvgoji", "12.02"));
    validateRow(response, List.of("joIQbN4L1Ok", "jmIPBj66vD6", "31.6"));
    validateRow(response, List.of("joIQbN4L1Ok", "ImspTQPwCqd", "19.05"));
    validateRow(response, List.of("joIQbN4L1Ok", "lc3eMKXaEfw", "23.9"));
    validateRow(response, List.of("joIQbN4L1Ok", "qhqAxPSTUXp", "28.99"));
    validateRow(response, List.of("joIQbN4L1Ok", "PMa2VCrupOd", "21.52"));
    validateRow(response, List.of("aGByu8NFs9m", "at6UHUQatSo", "75.96"));
    validateRow(response, List.of("aGByu8NFs9m", "bL4ooGhyHRQ", "82.19"));
    validateRow(response, List.of("aGByu8NFs9m", "TEQlaapDQoK", "74.26"));
    validateRow(response, List.of("aGByu8NFs9m", "O6uvpzGd5pu", "74.91"));
    validateRow(response, List.of("aGByu8NFs9m", "kJq2mPyFEHo", "71.91"));
    validateRow(response, List.of("aGByu8NFs9m", "jUb8gELQApl", "79.49"));
    validateRow(response, List.of("aGByu8NFs9m", "eIQbndfxQMb", "71.47"));
    validateRow(response, List.of("aGByu8NFs9m", "Vth0fbpFcsO", "69.05"));
    validateRow(response, List.of("aGByu8NFs9m", "fdc6uOvgoji", "85.27"));
    validateRow(response, List.of("aGByu8NFs9m", "jmIPBj66vD6", "55.49"));
    validateRow(response, List.of("aGByu8NFs9m", "ImspTQPwCqd", "74.6"));
    validateRow(response, List.of("aGByu8NFs9m", "lc3eMKXaEfw", "70.27"));
    validateRow(response, List.of("aGByu8NFs9m", "qhqAxPSTUXp", "62.78"));
    validateRow(response, List.of("aGByu8NFs9m", "PMa2VCrupOd", "71.29"));
  }

  @Test
  public void queryMalnutritionThisYearVsLastYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "filter=dx:vhzPbO1eEyr,ou:at6UHUQatSo;bL4ooGhyHRQ;TEQlaapDQoK;O6uvpzGd5pu;kJq2mPyFEHo;jUb8gELQApl;eIQbndfxQMb;Vth0fbpFcsO;fdc6uOvgoji;jmIPBj66vD6;ImspTQPwCqd;lc3eMKXaEfw;qhqAxPSTUXp;PMa2VCrupOd")
            .add("skipData=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=pe:MONTHS_THIS_YEAR")
            .add("relativePeriodDate=2022-07-06");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(9)))
        .body("height", equalTo(9))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"202208\":{\"name\":\"August 2022\"},\"MONTHS_THIS_YEAR\":{\"name\":\"Months this year\"},\"202209\":{\"name\":\"September 2022\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"202206\":{\"name\":\"June 2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202207\":{\"name\":\"July 2022\"},\"202204\":{\"name\":\"April 2022\"},\"202205\":{\"name\":\"May 2022\"},\"202202\":{\"name\":\"February 2022\"},\"202203\":{\"name\":\"March 2022\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202210\":{\"name\":\"October 2022\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"dx\":{\"name\":\"Data\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"202201\":{\"name\":\"January 2022\"},\"pe\":{\"name\":\"Period\"},\"vhzPbO1eEyr\":{\"name\":\"Severe malnutrition rate\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"}},\"dimensions\":{\"dx\":[\"vhzPbO1eEyr\"],\"pe\":[\"202201\",\"202202\",\"202203\",\"202204\",\"202205\",\"202206\",\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\"],\"ou\":[\"at6UHUQatSo\",\"bL4ooGhyHRQ\",\"TEQlaapDQoK\",\"O6uvpzGd5pu\",\"kJq2mPyFEHo\",\"jUb8gELQApl\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"fdc6uOvgoji\",\"jmIPBj66vD6\",\"ImspTQPwCqd\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"PMa2VCrupOd\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("202201", "4.97"));
    validateRow(response, List.of("202202", "6.03"));
    validateRow(response, List.of("202203", "6.5"));
    validateRow(response, List.of("202204", "5.25"));
    validateRow(response, List.of("202205", "5.89"));
    validateRow(response, List.of("202206", "9.52"));
    validateRow(response, List.of("202207", "5.42"));
    validateRow(response, List.of("202208", "6.76"));
    validateRow(response, List.of("202209", "6.2"));
  }

  @Test
  public void queryWithDifferentValueTypes() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("skipData=false")
            .add("includeMetadataDetails=true")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("relativePeriodDate=2024-01-29")
            .add(
                "dimension=dx:Uvn6LCg7dVU;sB79w2hiLp8;YgsAnqU3I7B;PBXQFnb2AOk;tUdBD1JDxpn;uF1DLnZNlWe;ytHQ7rfvIOe;lxAQ7Zs9VYR.sWoqcoByYmD;lxAQ7Zs9VYR.Ok9OQpitjQr,ou:USER_ORGUNIT;USER_ORGUNIT,pe:LAST_6_MONTHS");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ytHQ7rfvIOe\":{\"uid\":\"ytHQ7rfvIOe\",\"aggregationType\":\"COUNT\",\"valueType\":\"NUMBER\",\"name\":\"Malaria test - microscopy 0-4 years female\",\"description\":\"The number of suspected malaria cases tested by Microscopy\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"totalAggregationType\":\"SUM\"},\"sB79w2hiLp8\":{\"uid\":\"sB79w2hiLp8\",\"indicatorType\":{\"number\":false,\"displayName\":\"Per cent\",\"name\":\"Per cent\",\"factor\":100},\"valueType\":\"NUMBER\",\"name\":\"ANC 3 Coverage\",\"legendSet\":\"fqs276KXCXi\",\"description\":\"Total 3rd ANC visits (Fixed and outreach) by expected number of pregnant women.\",\"dimensionItemType\":\"INDICATOR\",\"totalAggregationType\":\"AVERAGE\"},\"ou\":{\"uid\":\"ou\",\"dimensionType\":\"ORGANISATION_UNIT\",\"name\":\"Organisation unit\"},\"202309\":{\"uid\":\"202309\",\"code\":\"202309\",\"endDate\":\"2023-09-30T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"September 2023\",\"startDate\":\"2023-09-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"PBXQFnb2AOk\":{\"uid\":\"PBXQFnb2AOk\",\"aggregationType\":\"SUM\",\"code\":\"DE_374647\",\"valueType\":\"NUMBER\",\"name\":\"Shift to ABC 300mg + ddI 200mg + LPV\\/r 133.3\\/33.3mg\",\"dimensionItemType\":\"DATA_ELEMENT\",\"totalAggregationType\":\"SUM\"},\"202307\":{\"uid\":\"202307\",\"code\":\"202307\",\"endDate\":\"2023-07-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"July 2023\",\"startDate\":\"2023-07-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"},\"202308\":{\"uid\":\"202308\",\"code\":\"202308\",\"endDate\":\"2023-08-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"August 2023\",\"startDate\":\"2023-08-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"},\"uF1DLnZNlWe\":{\"uid\":\"uF1DLnZNlWe\",\"aggregationType\":\"SUM\",\"code\":\"DE_1152329\",\"valueType\":\"TEXT\",\"name\":\"Additional notes related to facility\",\"dimensionItemType\":\"DATA_ELEMENT\",\"totalAggregationType\":\"SUM\"},\"202312\":{\"uid\":\"202312\",\"code\":\"202312\",\"endDate\":\"2023-12-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"December 2023\",\"startDate\":\"2023-12-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"},\"202310\":{\"uid\":\"202310\",\"code\":\"202310\",\"endDate\":\"2023-10-31T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"October 2023\",\"startDate\":\"2023-10-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"valueType\":\"TEXT\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"totalAggregationType\":\"SUM\"},\"202311\":{\"uid\":\"202311\",\"code\":\"202311\",\"endDate\":\"2023-11-30T00:00:00.000\",\"valueType\":\"TEXT\",\"name\":\"November 2023\",\"startDate\":\"2023-11-01T00:00:00.000\",\"dimensionItemType\":\"PERIOD\",\"totalAggregationType\":\"SUM\"},\"LAST_6_MONTHS\":{\"name\":\"Last 6 months\"},\"dx\":{\"uid\":\"dx\",\"dimensionType\":\"DATA_X\",\"name\":\"Data\"},\"tUdBD1JDxpn\":{\"uid\":\"tUdBD1JDxpn\",\"aggregationType\":\"AVERAGE\",\"valueType\":\"NUMBER\",\"name\":\"Average age of deaths\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"totalAggregationType\":\"SUM\"},\"lxAQ7Zs9VYR.Ok9OQpitjQr\":{\"aggregationType\":\"SUM\",\"valueType\":\"BOOLEAN\",\"name\":\"Antenatal care visit WHOMCH Smoking cessation counselling provided \",\"dimensionItemType\":\"PROGRAM_DATA_ELEMENT\",\"totalAggregationType\":\"SUM\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\",\"name\":\"Period\"},\"Uvn6LCg7dVU\":{\"uid\":\"Uvn6LCg7dVU\",\"code\":\"IN_52486\",\"indicatorType\":{\"number\":false,\"displayName\":\"Per cent\",\"name\":\"Per cent\",\"factor\":100},\"valueType\":\"NUMBER\",\"name\":\"ANC 1 Coverage\",\"legendSet\":\"fqs276KXCXi\",\"description\":\"Total 1st ANC visits (Fixed and outreach) by expected number of pregnant women.\",\"dimensionItemType\":\"INDICATOR\",\"totalAggregationType\":\"AVERAGE\"},\"YgsAnqU3I7B\":{\"uid\":\"YgsAnqU3I7B\",\"aggregationType\":\"SUM\",\"code\":\"DE_374657\",\"valueType\":\"NUMBER\",\"name\":\"Shift to AZT + 3TC + NVP 300mg + 150mg + 200mg\",\"dimensionItemType\":\"DATA_ELEMENT\",\"totalAggregationType\":\"SUM\"},\"HllvX50cXC0\":{\"uid\":\"HllvX50cXC0\",\"code\":\"default\",\"valueType\":\"NUMBER\",\"name\":\"default\",\"totalAggregationType\":\"SUM\"},\"lxAQ7Zs9VYR.sWoqcoByYmD\":{\"aggregationType\":\"SUM\",\"valueType\":\"BOOLEAN\",\"name\":\"Antenatal care visit WHOMCH Smoking\",\"dimensionItemType\":\"PROGRAM_DATA_ELEMENT\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\",\"sB79w2hiLp8\",\"YgsAnqU3I7B\",\"PBXQFnb2AOk\",\"tUdBD1JDxpn\",\"uF1DLnZNlWe\",\"ytHQ7rfvIOe\",\"lxAQ7Zs9VYR.sWoqcoByYmD\",\"lxAQ7Zs9VYR.Ok9OQpitjQr\"],\"pe\":[\"202307\",\"202308\",\"202309\",\"202310\",\"202311\",\"202312\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"HllvX50cXC0\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 5, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 7, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 8, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);
  }
}
