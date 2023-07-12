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
package org.hisp.dhis.analytics.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
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
    ApiResponse response = actions.get(params);

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
    validateRow(response, List.of("FbKK4ofIv5R", "202101", "23.0"));
    validateRow(response, List.of("FbKK4ofIv5R", "202102", "28.1"));
    validateRow(response, List.of("FbKK4ofIv5R", "202103", "25.5"));
    validateRow(response, List.of("FbKK4ofIv5R", "202104", "27.1"));
    validateRow(response, List.of("FbKK4ofIv5R", "202105", "28.7"));
    validateRow(response, List.of("FbKK4ofIv5R", "202106", "26.2"));
    validateRow(response, List.of("FbKK4ofIv5R", "202107", "24.1"));
    validateRow(response, List.of("FbKK4ofIv5R", "202108", "30.1"));
    validateRow(response, List.of("FbKK4ofIv5R", "202109", "31.3"));
    validateRow(response, List.of("FbKK4ofIv5R", "202110", "26.4"));
    validateRow(response, List.of("FbKK4ofIv5R", "202111", "31.2"));
    validateRow(response, List.of("FbKK4ofIv5R", "202112", "18.6"));
    validateRow(response, List.of("tUIlpyeeX9N", "202101", "23.9"));
    validateRow(response, List.of("tUIlpyeeX9N", "202102", "29.3"));
    validateRow(response, List.of("tUIlpyeeX9N", "202103", "27.0"));
    validateRow(response, List.of("tUIlpyeeX9N", "202104", "27.9"));
    validateRow(response, List.of("tUIlpyeeX9N", "202105", "29.7"));
    validateRow(response, List.of("tUIlpyeeX9N", "202106", "29.0"));
    validateRow(response, List.of("tUIlpyeeX9N", "202107", "27.5"));
    validateRow(response, List.of("tUIlpyeeX9N", "202108", "32.9"));
    validateRow(response, List.of("tUIlpyeeX9N", "202109", "31.8"));
    validateRow(response, List.of("tUIlpyeeX9N", "202110", "26.7"));
    validateRow(response, List.of("tUIlpyeeX9N", "202111", "30.7"));
    validateRow(response, List.of("tUIlpyeeX9N", "202112", "19.7"));
    validateRow(response, List.of("YlTWksXEhEO", "202101", "33.0"));
    validateRow(response, List.of("YlTWksXEhEO", "202102", "38.2"));
    validateRow(response, List.of("YlTWksXEhEO", "202103", "32.0"));
    validateRow(response, List.of("YlTWksXEhEO", "202104", "33.8"));
    validateRow(response, List.of("YlTWksXEhEO", "202105", "34.8"));
    validateRow(response, List.of("YlTWksXEhEO", "202106", "36.6"));
    validateRow(response, List.of("YlTWksXEhEO", "202107", "34.2"));
    validateRow(response, List.of("YlTWksXEhEO", "202108", "37.2"));
    validateRow(response, List.of("YlTWksXEhEO", "202109", "35.8"));
    validateRow(response, List.of("YlTWksXEhEO", "202110", "30.6"));
    validateRow(response, List.of("YlTWksXEhEO", "202111", "33.2"));
    validateRow(response, List.of("YlTWksXEhEO", "202112", "24.0"));
    validateRow(response, List.of("eTDtyyaSA7f", "202101", "22.8"));
    validateRow(response, List.of("eTDtyyaSA7f", "202102", "26.6"));
    validateRow(response, List.of("eTDtyyaSA7f", "202103", "24.3"));
    validateRow(response, List.of("eTDtyyaSA7f", "202104", "24.9"));
    validateRow(response, List.of("eTDtyyaSA7f", "202105", "25.7"));
    validateRow(response, List.of("eTDtyyaSA7f", "202106", "22.9"));
    validateRow(response, List.of("eTDtyyaSA7f", "202107", "22.0"));
    validateRow(response, List.of("eTDtyyaSA7f", "202108", "27.0"));
    validateRow(response, List.of("eTDtyyaSA7f", "202109", "26.6"));
    validateRow(response, List.of("eTDtyyaSA7f", "202110", "24.3"));
    validateRow(response, List.of("eTDtyyaSA7f", "202111", "26.8"));
    validateRow(response, List.of("eTDtyyaSA7f", "202112", "17.5"));
    validateRow(response, List.of("d9thHOJMROr", "202101", "30.3"));
    validateRow(response, List.of("d9thHOJMROr", "202102", "26.2"));
    validateRow(response, List.of("d9thHOJMROr", "202103", "20.7"));
    validateRow(response, List.of("d9thHOJMROr", "202104", "21.6"));
    validateRow(response, List.of("d9thHOJMROr", "202105", "17.9"));
    validateRow(response, List.of("d9thHOJMROr", "202106", "28.4"));
    validateRow(response, List.of("d9thHOJMROr", "202107", "29.7"));
    validateRow(response, List.of("d9thHOJMROr", "202108", "19.7"));
    validateRow(response, List.of("d9thHOJMROr", "202109", "13.0"));
    validateRow(response, List.of("d9thHOJMROr", "202110", "12.8"));
    validateRow(response, List.of("d9thHOJMROr", "202111", "6.9"));
    validateRow(response, List.of("d9thHOJMROr", "202112", "25.1"));
    validateRow(response, List.of("n5nS0SmkUpq", "202101", "31.8"));
    validateRow(response, List.of("n5nS0SmkUpq", "202102", "37.4"));
    validateRow(response, List.of("n5nS0SmkUpq", "202103", "32.5"));
    validateRow(response, List.of("n5nS0SmkUpq", "202104", "33.6"));
    validateRow(response, List.of("n5nS0SmkUpq", "202105", "35.3"));
    validateRow(response, List.of("n5nS0SmkUpq", "202106", "35.5"));
    validateRow(response, List.of("n5nS0SmkUpq", "202107", "33.8"));
    validateRow(response, List.of("n5nS0SmkUpq", "202108", "31.2"));
    validateRow(response, List.of("n5nS0SmkUpq", "202109", "34.2"));
    validateRow(response, List.of("n5nS0SmkUpq", "202110", "28.8"));
    validateRow(response, List.of("n5nS0SmkUpq", "202111", "31.0"));
    validateRow(response, List.of("n5nS0SmkUpq", "202112", "27.5"));
    validateRow(response, List.of("JoEzWYGdX7s", "202101", "23.9"));
    validateRow(response, List.of("JoEzWYGdX7s", "202102", "30.2"));
    validateRow(response, List.of("JoEzWYGdX7s", "202103", "27.0"));
    validateRow(response, List.of("JoEzWYGdX7s", "202104", "27.1"));
    validateRow(response, List.of("JoEzWYGdX7s", "202105", "29.1"));
    validateRow(response, List.of("JoEzWYGdX7s", "202106", "27.6"));
    validateRow(response, List.of("JoEzWYGdX7s", "202107", "27.8"));
    validateRow(response, List.of("JoEzWYGdX7s", "202108", "32.5"));
    validateRow(response, List.of("JoEzWYGdX7s", "202109", "31.8"));
    validateRow(response, List.of("JoEzWYGdX7s", "202110", "26.3"));
    validateRow(response, List.of("JoEzWYGdX7s", "202111", "29.9"));
    validateRow(response, List.of("JoEzWYGdX7s", "202112", "18.2"));
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
    ApiResponse response = actions.get(params);

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
    validateRow(response, List.of("ReUHfIn0pTQ", "O6uvpzGd5pu", "35.7"));
    validateRow(response, List.of("ReUHfIn0pTQ", "fdc6uOvgoji", "38.0"));
    validateRow(response, List.of("ReUHfIn0pTQ", "lc3eMKXaEfw", "33.6"));
    validateRow(response, List.of("ReUHfIn0pTQ", "jUb8gELQApl", "13.0"));
    validateRow(response, List.of("ReUHfIn0pTQ", "PMa2VCrupOd", "36.6"));
    validateRow(response, List.of("ReUHfIn0pTQ", "kJq2mPyFEHo", "8.1"));
    validateRow(response, List.of("ReUHfIn0pTQ", "qhqAxPSTUXp", "42.1"));
    validateRow(response, List.of("ReUHfIn0pTQ", "Vth0fbpFcsO", "30.0"));
    validateRow(response, List.of("ReUHfIn0pTQ", "jmIPBj66vD6", "21.9"));
    validateRow(response, List.of("ReUHfIn0pTQ", "TEQlaapDQoK", "52.0"));
    validateRow(response, List.of("ReUHfIn0pTQ", "bL4ooGhyHRQ", "35.7"));
    validateRow(response, List.of("ReUHfIn0pTQ", "eIQbndfxQMb", "52.9"));
    validateRow(response, List.of("ReUHfIn0pTQ", "at6UHUQatSo", "41.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "O6uvpzGd5pu", "146.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "fdc6uOvgoji", "83.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "lc3eMKXaEfw", "91.8"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jUb8gELQApl", "83.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "PMa2VCrupOd", "104.9"));
    validateRow(response, List.of("Uvn6LCg7dVU", "kJq2mPyFEHo", "96.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "qhqAxPSTUXp", "68.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Vth0fbpFcsO", "53.8"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jmIPBj66vD6", "120.8"));
    validateRow(response, List.of("Uvn6LCg7dVU", "TEQlaapDQoK", "101.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "bL4ooGhyHRQ", "90.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "eIQbndfxQMb", "127.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "at6UHUQatSo", "127.2"));
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
    ApiResponse response = actions.get(params);

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
    validateRow(response, List.of("X3taFC1HtE5", "at6UHUQatSo", "63.7"));
    validateRow(response, List.of("X3taFC1HtE5", "bL4ooGhyHRQ", "83.0"));
    validateRow(response, List.of("X3taFC1HtE5", "TEQlaapDQoK", "54.0"));
    validateRow(response, List.of("X3taFC1HtE5", "O6uvpzGd5pu", "61.0"));
    validateRow(response, List.of("X3taFC1HtE5", "kJq2mPyFEHo", "60.8"));
    validateRow(response, List.of("X3taFC1HtE5", "jUb8gELQApl", "68.1"));
    validateRow(response, List.of("X3taFC1HtE5", "eIQbndfxQMb", "101.9"));
    validateRow(response, List.of("X3taFC1HtE5", "Vth0fbpFcsO", "45.6"));
    validateRow(response, List.of("X3taFC1HtE5", "fdc6uOvgoji", "64.1"));
    validateRow(response, List.of("X3taFC1HtE5", "jmIPBj66vD6", "84.9"));
    validateRow(response, List.of("X3taFC1HtE5", "ImspTQPwCqd", "66.0"));
    validateRow(response, List.of("X3taFC1HtE5", "lc3eMKXaEfw", "65.2"));
    validateRow(response, List.of("X3taFC1HtE5", "qhqAxPSTUXp", "67.7"));
    validateRow(response, List.of("X3taFC1HtE5", "PMa2VCrupOd", "59.2"));
    validateRow(response, List.of("vhzPbO1eEyr", "at6UHUQatSo", "7.1"));
    validateRow(response, List.of("vhzPbO1eEyr", "bL4ooGhyHRQ", "2.4"));
    validateRow(response, List.of("vhzPbO1eEyr", "TEQlaapDQoK", "5.1"));
    validateRow(response, List.of("vhzPbO1eEyr", "O6uvpzGd5pu", "5.5"));
    validateRow(response, List.of("vhzPbO1eEyr", "kJq2mPyFEHo", "6.1"));
    validateRow(response, List.of("vhzPbO1eEyr", "jUb8gELQApl", "3.5"));
    validateRow(response, List.of("vhzPbO1eEyr", "eIQbndfxQMb", "8.2"));
    validateRow(response, List.of("vhzPbO1eEyr", "Vth0fbpFcsO", "9.9"));
    validateRow(response, List.of("vhzPbO1eEyr", "fdc6uOvgoji", "2.7"));
    validateRow(response, List.of("vhzPbO1eEyr", "jmIPBj66vD6", "12.9"));
    validateRow(response, List.of("vhzPbO1eEyr", "ImspTQPwCqd", "6.3"));
    validateRow(response, List.of("vhzPbO1eEyr", "lc3eMKXaEfw", "5.8"));
    validateRow(response, List.of("vhzPbO1eEyr", "qhqAxPSTUXp", "8.2"));
    validateRow(response, List.of("vhzPbO1eEyr", "PMa2VCrupOd", "7.2"));
    validateRow(response, List.of("joIQbN4L1Ok", "at6UHUQatSo", "17.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "bL4ooGhyHRQ", "15.4"));
    validateRow(response, List.of("joIQbN4L1Ok", "TEQlaapDQoK", "20.7"));
    validateRow(response, List.of("joIQbN4L1Ok", "O6uvpzGd5pu", "19.6"));
    validateRow(response, List.of("joIQbN4L1Ok", "kJq2mPyFEHo", "22.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "jUb8gELQApl", "17.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "eIQbndfxQMb", "20.3"));
    validateRow(response, List.of("joIQbN4L1Ok", "Vth0fbpFcsO", "21.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "fdc6uOvgoji", "12.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "jmIPBj66vD6", "31.6"));
    validateRow(response, List.of("joIQbN4L1Ok", "ImspTQPwCqd", "19.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "lc3eMKXaEfw", "23.9"));
    validateRow(response, List.of("joIQbN4L1Ok", "qhqAxPSTUXp", "29.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "PMa2VCrupOd", "21.5"));
    validateRow(response, List.of("aGByu8NFs9m", "at6UHUQatSo", "76.0"));
    validateRow(response, List.of("aGByu8NFs9m", "bL4ooGhyHRQ", "82.2"));
    validateRow(response, List.of("aGByu8NFs9m", "TEQlaapDQoK", "74.3"));
    validateRow(response, List.of("aGByu8NFs9m", "O6uvpzGd5pu", "74.9"));
    validateRow(response, List.of("aGByu8NFs9m", "kJq2mPyFEHo", "71.9"));
    validateRow(response, List.of("aGByu8NFs9m", "jUb8gELQApl", "79.5"));
    validateRow(response, List.of("aGByu8NFs9m", "eIQbndfxQMb", "71.5"));
    validateRow(response, List.of("aGByu8NFs9m", "Vth0fbpFcsO", "69.0"));
    validateRow(response, List.of("aGByu8NFs9m", "fdc6uOvgoji", "85.3"));
    validateRow(response, List.of("aGByu8NFs9m", "jmIPBj66vD6", "55.5"));
    validateRow(response, List.of("aGByu8NFs9m", "ImspTQPwCqd", "74.6"));
    validateRow(response, List.of("aGByu8NFs9m", "lc3eMKXaEfw", "70.3"));
    validateRow(response, List.of("aGByu8NFs9m", "qhqAxPSTUXp", "62.8"));
    validateRow(response, List.of("aGByu8NFs9m", "PMa2VCrupOd", "71.3"));
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
    validateRow(response, List.of("202201", "5.0"));
    validateRow(response, List.of("202202", "6.0"));
    validateRow(response, List.of("202203", "6.5"));
    validateRow(response, List.of("202204", "5.3"));
    validateRow(response, List.of("202205", "5.9"));
    validateRow(response, List.of("202206", "9.5"));
    validateRow(response, List.of("202207", "5.4"));
    validateRow(response, List.of("202208", "6.8"));
    validateRow(response, List.of("202209", "6.2"));
  }
}
