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
public class AnalyticsQueryDv3AutoTest extends AnalyticsApiTest {

  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void queryAnc1CoverageWesternChiefdomsThisYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:THIS_YEAR")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=dx:Uvn6LCg7dVU,ou:O6uvpzGd5pu;fdc6uOvgoji;lc3eMKXaEfw;jUb8gELQApl;LEVEL-tTUf91fCytl")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(54)))
        .body("height", equalTo(54))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"yu4N82FFeLm\":{\"name\":\"Mandu\"},\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"DxAPPqXvwLy\":{\"name\":\"Peje Bongre\"},\"jUb8gELQApl\":{\"uid\":\"jUb8gELQApl\",\"code\":\"OU_204856\",\"name\":\"Kailahun\"},\"e1eIKM1GIF3\":{\"name\":\"Gbanti Kamaranka\"},\"lYIM1MXbSYS\":{\"name\":\"Dea\"},\"sxRd2XOzFbz\":{\"name\":\"Tikonko\"},\"U6Kr7Gtpidn\":{\"name\":\"Kakua\"},\"hjpHnHZIniP\":{\"name\":\"Kissi Tongi\"},\"iUauWFeH8Qp\":{\"name\":\"Bum\"},\"zFDYIgyGmXG\":{\"name\":\"Bargbo\"},\"dx\":{\"name\":\"Data\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"},\"UhHipWG7J8b\":{\"name\":\"Sanda Tendaren\"},\"kU8vhUkAGaT\":{\"name\":\"Lugbu\"},\"ERmBhYkhV6Y\":{\"name\":\"Njaluahun\"},\"YmmeuGbqOwR\":{\"name\":\"Gbo\"},\"daJPPxtIrQn\":{\"name\":\"Jaiama Bongor\"},\"JsxnA2IywRo\":{\"name\":\"Kissi Kama\"},\"XG8HGAbrbbL\":{\"name\":\"Safroko Limba\"},\"pmxZm7klXBy\":{\"name\":\"Peje West\"},\"YuQRtpLP10I\":{\"name\":\"Badjia\"},\"ENHOJz3UH5L\":{\"name\":\"BMC\"},\"LfTkc0S4b5k\":{\"name\":\"Upper Bambara\"},\"fdc6uOvgoji\":{\"uid\":\"fdc6uOvgoji\",\"code\":\"OU_193190\",\"name\":\"Bombali\"},\"KSdZwrU7Hh6\":{\"name\":\"Jawi\"},\"cM2BKSrj9F9\":{\"name\":\"Luawa\"},\"I4jWcnFmgEC\":{\"name\":\"Niawa Lenga\"},\"BGGmAwx33dj\":{\"name\":\"Bumpe Ngao\"},\"fwH9ipvXde9\":{\"name\":\"Biriwa\"},\"g8DdBm7EmUt\":{\"name\":\"Sittia\"},\"XEyIRFd9pct\":{\"name\":\"Imperi\"},\"cgOy0hRMGu9\":{\"name\":\"Sogbini\"},\"j43EZb15rjI\":{\"name\":\"Sella Limba\"},\"DNRAeXT9IwS\":{\"name\":\"Dema\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"O6uvpzGd5pu\":{\"uid\":\"O6uvpzGd5pu\",\"code\":\"OU_264\",\"name\":\"Bo\"},\"EB1zRKdYjdY\":{\"name\":\"Bendu Cha\"},\"bQiBfA2j5cw\":{\"name\":\"Penguia\"},\"CG4QD1HC3h4\":{\"name\":\"Yawbeko\"},\"VP397wRvePm\":{\"name\":\"Nongoba Bullum\"},\"Qhmi8IZyPyD\":{\"name\":\"Tambaka\"},\"eV4cuxniZgP\":{\"name\":\"Magbaimba Ndowahun\"},\"vWbkYPRmKyS\":{\"name\":\"Baoma\"},\"dGheVylzol6\":{\"name\":\"Bargbe\"},\"aWQTfvgPA5v\":{\"name\":\"Kpanda Kemoh\"},\"npWGUj37qDe\":{\"name\":\"Valunia\"},\"KctpIIucige\":{\"name\":\"Selenga\"},\"j0Mtr3xTMjM\":{\"name\":\"Kissi Teng\"},\"L8iA6eLwKNb\":{\"name\":\"Paki Masabong\"},\"tTUf91fCytl\":{\"uid\":\"tTUf91fCytl\",\"name\":\"Chiefdom\"},\"ou\":{\"name\":\"Organisation unit\"},\"VCtF1DbspR5\":{\"name\":\"Jong\"},\"2022\":{\"name\":\"2022\"},\"HV8RTzgcFH3\":{\"name\":\"Kwamabai Krim\"},\"KKkLOTpMXGV\":{\"name\":\"Bombali Sebora\"},\"GE25DpSrqpB\":{\"name\":\"Malema\"},\"pe\":{\"name\":\"Period\"},\"ARZ4y5i4reU\":{\"name\":\"Wonde\"},\"byp7w6Xd9Df\":{\"name\":\"Yawei\"},\"BXJdOLvUrZB\":{\"name\":\"Gbendembu Ngowahun\"},\"JdhagCUEMbj\":{\"name\":\"Komboya\"},\"WXnNDWTiE9r\":{\"name\":\"Sanda Loko\"},\"lc3eMKXaEfw\":{\"uid\":\"lc3eMKXaEfw\",\"code\":\"OU_197385\",\"name\":\"Bonthe\"},\"hRZOIgQ0O1m\":{\"name\":\"Libeisaygahun\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\"],\"pe\":[\"2022\"],\"ou\":[\"YuQRtpLP10I\",\"vWbkYPRmKyS\",\"dGheVylzol6\",\"zFDYIgyGmXG\",\"EB1zRKdYjdY\",\"fwH9ipvXde9\",\"ENHOJz3UH5L\",\"KKkLOTpMXGV\",\"iUauWFeH8Qp\",\"BGGmAwx33dj\",\"lYIM1MXbSYS\",\"DNRAeXT9IwS\",\"e1eIKM1GIF3\",\"BXJdOLvUrZB\",\"YmmeuGbqOwR\",\"XEyIRFd9pct\",\"daJPPxtIrQn\",\"KSdZwrU7Hh6\",\"VCtF1DbspR5\",\"U6Kr7Gtpidn\",\"JsxnA2IywRo\",\"j0Mtr3xTMjM\",\"hjpHnHZIniP\",\"JdhagCUEMbj\",\"aWQTfvgPA5v\",\"HV8RTzgcFH3\",\"hRZOIgQ0O1m\",\"cM2BKSrj9F9\",\"kU8vhUkAGaT\",\"eV4cuxniZgP\",\"lY93YpCxJqf\",\"GE25DpSrqpB\",\"yu4N82FFeLm\",\"I4jWcnFmgEC\",\"ERmBhYkhV6Y\",\"VP397wRvePm\",\"L8iA6eLwKNb\",\"DxAPPqXvwLy\",\"pmxZm7klXBy\",\"bQiBfA2j5cw\",\"XG8HGAbrbbL\",\"WXnNDWTiE9r\",\"UhHipWG7J8b\",\"KctpIIucige\",\"j43EZb15rjI\",\"g8DdBm7EmUt\",\"cgOy0hRMGu9\",\"Qhmi8IZyPyD\",\"sxRd2XOzFbz\",\"LfTkc0S4b5k\",\"npWGUj37qDe\",\"ARZ4y5i4reU\",\"CG4QD1HC3h4\",\"byp7w6Xd9Df\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("Uvn6LCg7dVU", "YuQRtpLP10I", "298.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "vWbkYPRmKyS", "209.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "dGheVylzol6", "124.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "zFDYIgyGmXG", "202.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "EB1zRKdYjdY", "86.9"));
    validateRow(response, List.of("Uvn6LCg7dVU", "fwH9ipvXde9", "76.1"));
    validateRow(response, List.of("Uvn6LCg7dVU", "ENHOJz3UH5L", "52.9"));
    validateRow(response, List.of("Uvn6LCg7dVU", "KKkLOTpMXGV", "75.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "iUauWFeH8Qp", "115.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "BGGmAwx33dj", "118.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "lYIM1MXbSYS", "73.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "DNRAeXT9IwS", "66.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "e1eIKM1GIF3", "89.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "BXJdOLvUrZB", "94.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "YmmeuGbqOwR", "198.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "XEyIRFd9pct", "88.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "daJPPxtIrQn", "87.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "KSdZwrU7Hh6", "68.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "VCtF1DbspR5", "90.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "U6Kr7Gtpidn", "102.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "JsxnA2IywRo", "80.8"));
    validateRow(response, List.of("Uvn6LCg7dVU", "j0Mtr3xTMjM", "71.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "hjpHnHZIniP", "93.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "JdhagCUEMbj", "183.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "aWQTfvgPA5v", "71.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "HV8RTzgcFH3", "139.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "hRZOIgQ0O1m", "90.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "cM2BKSrj9F9", "89.9"));
    validateRow(response, List.of("Uvn6LCg7dVU", "kU8vhUkAGaT", "94.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "eV4cuxniZgP", "93.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "lY93YpCxJqf", "80.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "GE25DpSrqpB", "90.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "yu4N82FFeLm", "59.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "I4jWcnFmgEC", "145.1"));
    validateRow(response, List.of("Uvn6LCg7dVU", "ERmBhYkhV6Y", "85.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "VP397wRvePm", "99.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "L8iA6eLwKNb", "86.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "DxAPPqXvwLy", "87.1"));
    validateRow(response, List.of("Uvn6LCg7dVU", "pmxZm7klXBy", "55.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "bQiBfA2j5cw", "94.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "XG8HGAbrbbL", "80.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "WXnNDWTiE9r", "80.9"));
    validateRow(response, List.of("Uvn6LCg7dVU", "UhHipWG7J8b", "85.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "KctpIIucige", "89.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "j43EZb15rjI", "91.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "g8DdBm7EmUt", "64.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "cgOy0hRMGu9", "93.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Qhmi8IZyPyD", "77.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "sxRd2XOzFbz", "199.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "LfTkc0S4b5k", "103.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "npWGUj37qDe", "94.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "ARZ4y5i4reU", "82.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "CG4QD1HC3h4", "91.9"));
    validateRow(response, List.of("Uvn6LCg7dVU", "byp7w6Xd9Df", "82.0"));
  }

  @Test
  public void queryAnc1CoverageYearOverYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:ImspTQPwCqd,dx:Uvn6LCg7dVU")
            .add("skipData=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=pe:MONTHS_THIS_YEAR")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(12)))
        .body("height", equalTo(12))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ou\":{\"name\":\"Organisation unit\"},\"202208\":{\"name\":\"August 2022\"},\"MONTHS_THIS_YEAR\":{\"name\":\"Months this year\"},\"202209\":{\"name\":\"September 2022\"},\"202206\":{\"name\":\"June 2022\"},\"202207\":{\"name\":\"July 2022\"},\"202204\":{\"name\":\"April 2022\"},\"202205\":{\"name\":\"May 2022\"},\"202202\":{\"name\":\"February 2022\"},\"202203\":{\"name\":\"March 2022\"},\"202211\":{\"name\":\"November 2022\"},\"202201\":{\"name\":\"January 2022\"},\"202212\":{\"name\":\"December 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202210\":{\"name\":\"October 2022\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\"],\"pe\":[\"202201\",\"202202\",\"202203\",\"202204\",\"202205\",\"202206\",\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("202201", "94.7"));
    validateRow(response, List.of("202202", "98.8"));
    validateRow(response, List.of("202203", "87.9"));
    validateRow(response, List.of("202204", "107.0"));
    validateRow(response, List.of("202205", "139.4"));
    validateRow(response, List.of("202206", "116.4"));
    validateRow(response, List.of("202207", "107.1"));
    validateRow(response, List.of("202208", "104.1"));
    validateRow(response, List.of("202209", "109.5"));
    validateRow(response, List.of("202210", "89.8"));
    validateRow(response, List.of("202211", "87.6"));
    validateRow(response, List.of("202212", "75.6"));
  }

  @Test
  public void queryAnc1VisitsCumulativeNumbers() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:fbfJHSPpUQD,pe:LAST_12_MONTHS,ou:ImspTQPwCqd")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(12)))
        .body("height", equalTo(12))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ou\":{\"name\":\"Organisation unit\"},\"202109\":{\"name\":\"September 2021\"},\"202107\":{\"name\":\"July 2021\"},\"fbfJHSPpUQD\":{\"name\":\"ANC 1st visit\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202101\":{\"name\":\"January 2021\"},\"202112\":{\"name\":\"December 2021\"},\"202102\":{\"name\":\"February 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"pe\":{\"name\":\"Period\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"}},\"dimensions\":{\"dx\":[\"fbfJHSPpUQD\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 5, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 7, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 8, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response, List.of("fbfJHSPpUQD", "202107", "ImspTQPwCqd", "22356", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "202106", "ImspTQPwCqd", "23813", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "202109", "ImspTQPwCqd", "22308", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "202104", "ImspTQPwCqd", "18576", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "202108", "ImspTQPwCqd", "22004", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "202105", "ImspTQPwCqd", "29461", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "202110", "ImspTQPwCqd", "17926", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "202112", "ImspTQPwCqd", "16445", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "202111", "ImspTQPwCqd", "19691", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "202101", "ImspTQPwCqd", "20026", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "202103", "ImspTQPwCqd", "21877", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "202102", "ImspTQPwCqd", "18786", "", "", "", "", ""));
  }

  @Test
  public void queryAnc1stVisitsLast12MonthsCumulativeValues() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=dx:fbfJHSPpUQD")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=ou:jUb8gELQApl;PMa2VCrupOd;jmIPBj66vD6;kJq2mPyFEHo;bL4ooGhyHRQ,pe:LAST_12_MONTHS")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(60)))
        .body("height", equalTo(60))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"202109\":{\"name\":\"September 2021\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202112\":{\"name\":\"December 2021\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"fbfJHSPpUQD\":{\"name\":\"ANC 1st visit\"},\"202101\":{\"name\":\"January 2021\"},\"202102\":{\"name\":\"February 2021\"},\"pe\":{\"name\":\"Period\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"fbfJHSPpUQD\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"jUb8gELQApl\",\"PMa2VCrupOd\",\"jmIPBj66vD6\",\"kJq2mPyFEHo\",\"bL4ooGhyHRQ\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("jmIPBj66vD6", "202103", "1448"));
    validateRow(response, List.of("jmIPBj66vD6", "202102", "1419"));
    validateRow(response, List.of("jmIPBj66vD6", "202101", "1440"));
    validateRow(response, List.of("jmIPBj66vD6", "202109", "1437"));
    validateRow(response, List.of("jmIPBj66vD6", "202108", "1228"));
    validateRow(response, List.of("jmIPBj66vD6", "202107", "1489"));
    validateRow(response, List.of("jmIPBj66vD6", "202106", "1390"));
    validateRow(response, List.of("jmIPBj66vD6", "202105", "1508"));
    validateRow(response, List.of("jmIPBj66vD6", "202104", "1167"));
    validateRow(response, List.of("jUb8gELQApl", "202112", "1152"));
    validateRow(response, List.of("jUb8gELQApl", "202110", "1131"));
    validateRow(response, List.of("jUb8gELQApl", "202111", "1266"));
    validateRow(response, List.of("jmIPBj66vD6", "202112", "1129"));
    validateRow(response, List.of("jmIPBj66vD6", "202111", "1245"));
    validateRow(response, List.of("jmIPBj66vD6", "202110", "1269"));
    validateRow(response, List.of("jUb8gELQApl", "202105", "1501"));
    validateRow(response, List.of("jUb8gELQApl", "202106", "1365"));
    validateRow(response, List.of("jUb8gELQApl", "202103", "1264"));
    validateRow(response, List.of("jUb8gELQApl", "202104", "1217"));
    validateRow(response, List.of("jUb8gELQApl", "202101", "1219"));
    validateRow(response, List.of("jUb8gELQApl", "202102", "1146"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202110", "7"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202111", "8"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202112", "6"));
    validateRow(response, List.of("jUb8gELQApl", "202109", "1192"));
    validateRow(response, List.of("jUb8gELQApl", "202107", "1360"));
    validateRow(response, List.of("jUb8gELQApl", "202108", "1332"));
    validateRow(response, List.of("kJq2mPyFEHo", "202111", "2009"));
    validateRow(response, List.of("kJq2mPyFEHo", "202112", "1909"));
    validateRow(response, List.of("kJq2mPyFEHo", "202110", "2036"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202107", "1263"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202108", "1065"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202109", "1036"));
    validateRow(response, List.of("PMa2VCrupOd", "202112", "1295"));
    validateRow(response, List.of("PMa2VCrupOd", "202111", "1127"));
    validateRow(response, List.of("PMa2VCrupOd", "202110", "1342"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202101", "1244"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202102", "1011"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202103", "1306"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202104", "1005"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202105", "1363"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202106", "1348"));
    validateRow(response, List.of("kJq2mPyFEHo", "202101", "1956"));
    validateRow(response, List.of("PMa2VCrupOd", "202104", "1074"));
    validateRow(response, List.of("kJq2mPyFEHo", "202108", "1845"));
    validateRow(response, List.of("PMa2VCrupOd", "202103", "1023"));
    validateRow(response, List.of("kJq2mPyFEHo", "202109", "2034"));
    validateRow(response, List.of("PMa2VCrupOd", "202102", "932"));
    validateRow(response, List.of("kJq2mPyFEHo", "202106", "2066"));
    validateRow(response, List.of("PMa2VCrupOd", "202101", "1359"));
    validateRow(response, List.of("kJq2mPyFEHo", "202107", "1854"));
    validateRow(response, List.of("kJq2mPyFEHo", "202104", "1778"));
    validateRow(response, List.of("kJq2mPyFEHo", "202105", "3013"));
    validateRow(response, List.of("kJq2mPyFEHo", "202102", "1773"));
    validateRow(response, List.of("kJq2mPyFEHo", "202103", "2074"));
    validateRow(response, List.of("PMa2VCrupOd", "202109", "1140"));
    validateRow(response, List.of("PMa2VCrupOd", "202108", "1050"));
    validateRow(response, List.of("PMa2VCrupOd", "202107", "1171"));
    validateRow(response, List.of("PMa2VCrupOd", "202106", "1326"));
    validateRow(response, List.of("PMa2VCrupOd", "202105", "1481"));
  }

  @Test
  public void queryAnc2CoverageThisYeargauge() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:THIS_YEAR")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:OdiHJayrsKo")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"OdiHJayrsKo\":{\"name\":\"ANC 2 Coverage\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"2022\":{\"name\":\"2022\"}},\"dimensions\":{\"dx\":[\"OdiHJayrsKo\"],\"pe\":[\"2022\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("OdiHJayrsKo", "93.8"));
  }

  @Test
  public void queryAnc3CoverageByDistrictsLast12Months() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=dx:sB79w2hiLp8")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=ou:TEQlaapDQoK;Vth0fbpFcsO;bL4ooGhyHRQ;jmIPBj66vD6;qhqAxPSTUXp;LEVEL-wjP19dkFeIk,pe:LAST_12_MONTHS")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(55)))
        .body("height", equalTo(55))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"sB79w2hiLp8\":{\"name\":\"ANC 3 Coverage\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"ou\":{\"name\":\"Organisation unit\"},\"202109\":{\"name\":\"September 2021\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"wjP19dkFeIk\":{\"uid\":\"wjP19dkFeIk\",\"name\":\"District\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202101\":{\"name\":\"January 2021\"},\"202112\":{\"name\":\"December 2021\"},\"202102\":{\"name\":\"February 2021\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"sB79w2hiLp8\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("qhqAxPSTUXp", "202101", "39.6"));
    validateRow(response, List.of("qhqAxPSTUXp", "202102", "42.8"));
    validateRow(response, List.of("qhqAxPSTUXp", "202103", "43.6"));
    validateRow(response, List.of("qhqAxPSTUXp", "202104", "42.9"));
    validateRow(response, List.of("qhqAxPSTUXp", "202105", "45.1"));
    validateRow(response, List.of("qhqAxPSTUXp", "202106", "44.6"));
    validateRow(response, List.of("qhqAxPSTUXp", "202107", "40.2"));
    validateRow(response, List.of("qhqAxPSTUXp", "202108", "49.3"));
    validateRow(response, List.of("qhqAxPSTUXp", "202109", "66.0"));
    validateRow(response, List.of("qhqAxPSTUXp", "202111", "62.8"));
    validateRow(response, List.of("Vth0fbpFcsO", "202101", "19.0"));
    validateRow(response, List.of("Vth0fbpFcsO", "202102", "61.9"));
    validateRow(response, List.of("Vth0fbpFcsO", "202103", "47.9"));
    validateRow(response, List.of("Vth0fbpFcsO", "202104", "58.4"));
    validateRow(response, List.of("Vth0fbpFcsO", "202105", "39.8"));
    validateRow(response, List.of("Vth0fbpFcsO", "202106", "48.4"));
    validateRow(response, List.of("Vth0fbpFcsO", "202107", "46.6"));
    validateRow(response, List.of("Vth0fbpFcsO", "202108", "35.3"));
    validateRow(response, List.of("Vth0fbpFcsO", "202109", "50.0"));
    validateRow(response, List.of("Vth0fbpFcsO", "202112", "47.3"));
    validateRow(response, List.of("jmIPBj66vD6", "202101", "92.8"));
    validateRow(response, List.of("jmIPBj66vD6", "202102", "100.8"));
    validateRow(response, List.of("jmIPBj66vD6", "202103", "101.4"));
    validateRow(response, List.of("jmIPBj66vD6", "202104", "97.3"));
    validateRow(response, List.of("jmIPBj66vD6", "202105", "102.0"));
    validateRow(response, List.of("jmIPBj66vD6", "202106", "97.6"));
    validateRow(response, List.of("jmIPBj66vD6", "202107", "97.6"));
    validateRow(response, List.of("jmIPBj66vD6", "202108", "90.2"));
    validateRow(response, List.of("jmIPBj66vD6", "202109", "100.8"));
    validateRow(response, List.of("jmIPBj66vD6", "202110", "87.3"));
    validateRow(response, List.of("jmIPBj66vD6", "202111", "92.4"));
    validateRow(response, List.of("jmIPBj66vD6", "202112", "72.2"));
    validateRow(response, List.of("TEQlaapDQoK", "202101", "41.3"));
    validateRow(response, List.of("TEQlaapDQoK", "202102", "46.4"));
    validateRow(response, List.of("TEQlaapDQoK", "202103", "49.8"));
    validateRow(response, List.of("TEQlaapDQoK", "202104", "41.2"));
    validateRow(response, List.of("TEQlaapDQoK", "202105", "62.9"));
    validateRow(response, List.of("TEQlaapDQoK", "202106", "62.4"));
    validateRow(response, List.of("TEQlaapDQoK", "202107", "54.3"));
    validateRow(response, List.of("TEQlaapDQoK", "202108", "57.3"));
    validateRow(response, List.of("TEQlaapDQoK", "202109", "59.7"));
    validateRow(response, List.of("TEQlaapDQoK", "202110", "52.6"));
    validateRow(response, List.of("TEQlaapDQoK", "202111", "57.8"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202101", "71.9"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202102", "74.7"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202103", "79.3"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202104", "61.7"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202105", "73.8"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202106", "89.0"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202107", "76.3"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202108", "81.0"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202109", "89.0"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202110", "0.6"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202111", "1.2"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202112", "0.4"));
  }
}
