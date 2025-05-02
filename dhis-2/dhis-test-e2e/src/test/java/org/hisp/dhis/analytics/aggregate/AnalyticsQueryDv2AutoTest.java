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
public class AnalyticsQueryDv2AutoTest extends AnalyticsApiTest {

  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void query1stAnd3rdVisitCoverageByOrgunitLastYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:THIS_YEAR")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:Uvn6LCg7dVU;sB79w2hiLp8,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(28)))
        .body("height", equalTo(28))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"sB79w2hiLp8\":{\"name\":\"ANC 3 Coverage\"},\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"THIS_YEAR\":{\"name\":\"This year\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"2022\":{\"name\":\"2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"USER_ORGUNIT_CHILDREN\":{\"organisationUnits\":[\"at6UHUQatSo\",\"TEQlaapDQoK\",\"PMa2VCrupOd\",\"qhqAxPSTUXp\",\"kJq2mPyFEHo\",\"jmIPBj66vD6\",\"Vth0fbpFcsO\",\"jUb8gELQApl\",\"fdc6uOvgoji\",\"eIQbndfxQMb\",\"O6uvpzGd5pu\",\"lc3eMKXaEfw\",\"bL4ooGhyHRQ\"]},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\",\"sB79w2hiLp8\"],\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\",\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("Uvn6LCg7dVU", "ImspTQPwCqd", "101.47"));
    validateRow(response, List.of("Uvn6LCg7dVU", "O6uvpzGd5pu", "142.27"));
    validateRow(response, List.of("Uvn6LCg7dVU", "fdc6uOvgoji", "82.19"));
    validateRow(response, List.of("Uvn6LCg7dVU", "lc3eMKXaEfw", "90.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jUb8gELQApl", "81.58"));
    validateRow(response, List.of("Uvn6LCg7dVU", "PMa2VCrupOd", "102.87"));
    validateRow(response, List.of("Uvn6LCg7dVU", "kJq2mPyFEHo", "94.42"));
    validateRow(response, List.of("Uvn6LCg7dVU", "qhqAxPSTUXp", "66.95"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Vth0fbpFcsO", "52.76"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jmIPBj66vD6", "118.41"));
    validateRow(response, List.of("Uvn6LCg7dVU", "TEQlaapDQoK", "99.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "bL4ooGhyHRQ", "88.55"));
    validateRow(response, List.of("Uvn6LCg7dVU", "eIQbndfxQMb", "124.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "at6UHUQatSo", "124.69"));
    validateRow(response, List.of("sB79w2hiLp8", "ImspTQPwCqd", "65.83"));
    validateRow(response, List.of("sB79w2hiLp8", "O6uvpzGd5pu", "92.35"));
    validateRow(response, List.of("sB79w2hiLp8", "fdc6uOvgoji", "50.95"));
    validateRow(response, List.of("sB79w2hiLp8", "lc3eMKXaEfw", "59.73"));
    validateRow(response, List.of("sB79w2hiLp8", "jUb8gELQApl", "70.97"));
    validateRow(response, List.of("sB79w2hiLp8", "PMa2VCrupOd", "65.18"));
    validateRow(response, List.of("sB79w2hiLp8", "kJq2mPyFEHo", "86.79"));
    validateRow(response, List.of("sB79w2hiLp8", "qhqAxPSTUXp", "38.76"));
    validateRow(response, List.of("sB79w2hiLp8", "Vth0fbpFcsO", "36.93"));
    validateRow(response, List.of("sB79w2hiLp8", "jmIPBj66vD6", "92.42"));
    validateRow(response, List.of("sB79w2hiLp8", "TEQlaapDQoK", "47.81"));
    validateRow(response, List.of("sB79w2hiLp8", "bL4ooGhyHRQ", "56.94"));
    validateRow(response, List.of("sB79w2hiLp8", "eIQbndfxQMb", "58.67"));
    validateRow(response, List.of("sB79w2hiLp8", "at6UHUQatSo", "72.85"));
  }

  @Test
  public void query1stTo3rdVisitDropoutRateByOrgunitLastYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:THIS_YEAR")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:ReUHfIn0pTQ,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(14)))
        .body("height", equalTo(14))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"THIS_YEAR\":{\"name\":\"This year\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"2022\":{\"name\":\"2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"USER_ORGUNIT_CHILDREN\":{\"organisationUnits\":[\"at6UHUQatSo\",\"TEQlaapDQoK\",\"PMa2VCrupOd\",\"qhqAxPSTUXp\",\"kJq2mPyFEHo\",\"jmIPBj66vD6\",\"Vth0fbpFcsO\",\"jUb8gELQApl\",\"fdc6uOvgoji\",\"eIQbndfxQMb\",\"O6uvpzGd5pu\",\"lc3eMKXaEfw\",\"bL4ooGhyHRQ\"]},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"ReUHfIn0pTQ\":{\"name\":\"ANC 1-3 Dropout Rate\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"ReUHfIn0pTQ\"],\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\",\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("ReUHfIn0pTQ", "ImspTQPwCqd", "35.03"));
    validateRow(response, List.of("ReUHfIn0pTQ", "O6uvpzGd5pu", "34.38"));
    validateRow(response, List.of("ReUHfIn0pTQ", "fdc6uOvgoji", "38.01"));
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
  }

  @Test
  public void query4VisitsByFacilityThisYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:THIS_YEAR,dx:hfdmMSPBgLG")
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
        "{\"items\":{\"RXL3lPSK8oG\":{\"name\":\"Clinic\"},\"J5jldMd8OHv\":{\"name\":\"Facility Type\"},\"CXw2yu5fodb\":{\"name\":\"CHC\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"2022\":{\"name\":\"2022\"},\"uYxK4wmcPqA\":{\"name\":\"CHP\"},\"tDZVQ1WtwpA\":{\"name\":\"Hospital\"},\"hfdmMSPBgLG\":{\"name\":\"ANC 4th or more visits\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"},\"EYbopBOJWsW\":{\"name\":\"MCHP\"}},\"dimensions\":{\"dx\":[\"hfdmMSPBgLG\"],\"pe\":[\"2022\"],\"J5jldMd8OHv\":[\"uYxK4wmcPqA\",\"tDZVQ1WtwpA\",\"EYbopBOJWsW\",\"RXL3lPSK8oG\",\"CXw2yu5fodb\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "J5jldMd8OHv", "Facility Type", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("RXL3lPSK8oG", "6118"));
    validateRow(response, List.of("tDZVQ1WtwpA", "4072"));
    validateRow(response, List.of("EYbopBOJWsW", "35618"));
    validateRow(response, List.of("uYxK4wmcPqA", "17400"));
    validateRow(response, List.of("CXw2yu5fodb", "31885"));
  }

  @Test
  public void query4VisitsByFacilityTypeLastYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:THIS_YEAR,dx:hfdmMSPBgLG,ou:USER_ORGUNIT")
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
        "{\"items\":{\"RXL3lPSK8oG\":{\"name\":\"Clinic\"},\"J5jldMd8OHv\":{\"name\":\"Facility Type\"},\"ou\":{},\"CXw2yu5fodb\":{\"name\":\"CHC\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"2022\":{\"name\":\"2022\"},\"uYxK4wmcPqA\":{\"name\":\"CHP\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"tDZVQ1WtwpA\":{\"name\":\"Hospital\"},\"hfdmMSPBgLG\":{\"name\":\"ANC 4th or more visits\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"},\"EYbopBOJWsW\":{\"name\":\"MCHP\"}},\"dimensions\":{\"dx\":[\"hfdmMSPBgLG\"],\"pe\":[\"2022\"],\"J5jldMd8OHv\":[\"uYxK4wmcPqA\",\"tDZVQ1WtwpA\",\"EYbopBOJWsW\",\"RXL3lPSK8oG\",\"CXw2yu5fodb\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "J5jldMd8OHv", "Facility Type", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("RXL3lPSK8oG", "6118"));
    validateRow(response, List.of("tDZVQ1WtwpA", "4072"));
    validateRow(response, List.of("EYbopBOJWsW", "35618"));
    validateRow(response, List.of("uYxK4wmcPqA", "17400"));
    validateRow(response, List.of("CXw2yu5fodb", "31885"));
  }

  @Test
  public void query4thVisitCoverageByOrgunitLastYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:THIS_YEAR,dx:hfdmMSPBgLG")
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
        "{\"items\":{\"RXL3lPSK8oG\":{\"name\":\"Clinic\"},\"J5jldMd8OHv\":{\"name\":\"Facility Type\"},\"CXw2yu5fodb\":{\"name\":\"CHC\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"2022\":{\"name\":\"2022\"},\"uYxK4wmcPqA\":{\"name\":\"CHP\"},\"tDZVQ1WtwpA\":{\"name\":\"Hospital\"},\"hfdmMSPBgLG\":{\"name\":\"ANC 4th or more visits\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"},\"EYbopBOJWsW\":{\"name\":\"MCHP\"}},\"dimensions\":{\"dx\":[\"hfdmMSPBgLG\"],\"pe\":[\"2022\"],\"J5jldMd8OHv\":[\"uYxK4wmcPqA\",\"tDZVQ1WtwpA\",\"EYbopBOJWsW\",\"RXL3lPSK8oG\",\"CXw2yu5fodb\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "J5jldMd8OHv", "Facility Type", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("RXL3lPSK8oG", "6118"));
    validateRow(response, List.of("tDZVQ1WtwpA", "4072"));
    validateRow(response, List.of("EYbopBOJWsW", "35618"));
    validateRow(response, List.of("uYxK4wmcPqA", "17400"));
    validateRow(response, List.of("CXw2yu5fodb", "31885"));
  }

  @Test
  public void queryAnc1CoverageAllChiefdomsThisYear() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:THIS_YEAR")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:Uvn6LCg7dVU,ou:ImspTQPwCqd;LEVEL-tTUf91fCytl")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(152)))
        .body("height", equalTo(152))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"KXSqt7jv6DU\":{\"name\":\"Gorama Mende\"},\"yu4N82FFeLm\":{\"name\":\"Mandu\"},\"lY93YpCxJqf\":{\"name\":\"Makari Gbanti\"},\"eROJsBwxQHt\":{\"name\":\"Gaura\"},\"DxAPPqXvwLy\":{\"name\":\"Peje Bongre\"},\"PaqugoqjRIj\":{\"name\":\"Sulima (Koinadugu)\"},\"gy8rmvYT4cj\":{\"name\":\"Ribbi\"},\"RzKeCma9qb1\":{\"name\":\"Barri\"},\"vULnao2hV5v\":{\"name\":\"Fakunya\"},\"EfWCa0Cc8WW\":{\"name\":\"Masimera\"},\"U6Kr7Gtpidn\":{\"name\":\"Kakua\"},\"hjpHnHZIniP\":{\"name\":\"Kissi Tongi\"},\"AovmOHadayb\":{\"name\":\"Timidale\"},\"iUauWFeH8Qp\":{\"name\":\"Bum\"},\"EYt6ThQDagn\":{\"name\":\"Koya (kenema)\"},\"Jiyc4ekaMMh\":{\"name\":\"Kongbora\"},\"FlBemv1NfEC\":{\"name\":\"Masungbala\"},\"XrF5AvaGcuw\":{\"name\":\"Wara Wara Bafodia\"},\"LhaAPLxdSFH\":{\"name\":\"Lei\"},\"BmYyh9bZ0sr\":{\"name\":\"Kafe Simira\"},\"pk7bUK5c1Uf\":{\"name\":\"Ya Kpukumu Krim\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"},\"zSNUViKdkk3\":{\"name\":\"Kpaka\"},\"r06ohri9wA9\":{\"name\":\"Samu\"},\"ERmBhYkhV6Y\":{\"name\":\"Njaluahun\"},\"Z9QaI6sxTwW\":{\"name\":\"Kargboro\"},\"daJPPxtIrQn\":{\"name\":\"Jaiama Bongor\"},\"W5fN3G6y1VI\":{\"name\":\"Lower Banta\"},\"r1RUyfVBkLp\":{\"name\":\"Sambaia Bendugu\"},\"NNE0YMCDZkO\":{\"name\":\"Yoni\"},\"ENHOJz3UH5L\":{\"name\":\"BMC\"},\"QywkxFudXrC\":{\"name\":\"Magbema\"},\"jWSIbtKfURj\":{\"name\":\"Langrama\"},\"J4GiUImJZoE\":{\"name\":\"Nieni\"},\"CF243RPvNY7\":{\"name\":\"Fiama\"},\"cM2BKSrj9F9\":{\"name\":\"Luawa\"},\"I4jWcnFmgEC\":{\"name\":\"Niawa Lenga\"},\"jPidqyo7cpF\":{\"name\":\"Bagruwa\"},\"BGGmAwx33dj\":{\"name\":\"Bumpe Ngao\"},\"kvkDWg42lHR\":{\"name\":\"Kamara\"},\"iGHlidSFdpu\":{\"name\":\"Soa\"},\"g8DdBm7EmUt\":{\"name\":\"Sittia\"},\"ZiOVcrSjSYe\":{\"name\":\"Dibia\"},\"vn9KJsLyP5f\":{\"name\":\"Kaffu Bullom\"},\"QlCIp2S9NHs\":{\"name\":\"Dodo\"},\"j43EZb15rjI\":{\"name\":\"Sella Limba\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"bQiBfA2j5cw\":{\"name\":\"Penguia\"},\"NqWaKXcg01b\":{\"name\":\"Sowa\"},\"VP397wRvePm\":{\"name\":\"Nongoba Bullum\"},\"fwxkctgmffZ\":{\"name\":\"Kholifa Mabang\"},\"QwMiPiME3bA\":{\"name\":\"Kpanga Kabonde\"},\"Qhmi8IZyPyD\":{\"name\":\"Tambaka\"},\"OTFepb1k9Db\":{\"name\":\"Mongo\"},\"DBs6e2Oxaj1\":{\"name\":\"Upper Banta\"},\"eV4cuxniZgP\":{\"name\":\"Magbaimba Ndowahun\"},\"xhyjU2SVewz\":{\"name\":\"Tane\"},\"vWbkYPRmKyS\":{\"name\":\"Baoma\"},\"dGheVylzol6\":{\"name\":\"Bargbe\"},\"TA7NvKjsn4A\":{\"name\":\"Bureh Kasseh Maconteh\"},\"npWGUj37qDe\":{\"name\":\"Valunia\"},\"myQ4q1W6B4y\":{\"name\":\"Dama\"},\"tTUf91fCytl\":{\"uid\":\"tTUf91fCytl\",\"name\":\"Chiefdom\"},\"nV3OkyzF4US\":{\"name\":\"Kori\"},\"X7dWcGerQIm\":{\"name\":\"Wandor\"},\"kbPmt60yi0L\":{\"name\":\"Bramaia\"},\"qIRCo0MfuGb\":{\"name\":\"Gbinleh Dixion\"},\"eNtRuQrrZeo\":{\"name\":\"Galliness Perri\"},\"HV8RTzgcFH3\":{\"name\":\"Kwamabai Krim\"},\"KKkLOTpMXGV\":{\"name\":\"Bombali Sebora\"},\"Pc3JTyqnsmL\":{\"name\":\"Buya Romende\"},\"hdEuw2ugkVF\":{\"name\":\"Lower Bambara\"},\"l7pFejMtUoF\":{\"name\":\"Tunkia\"},\"K1r3uF6eZ8n\":{\"name\":\"Kandu Lepiema\"},\"VGAFxBXz16y\":{\"name\":\"Sengbeh\"},\"GE25DpSrqpB\":{\"name\":\"Malema\"},\"ARZ4y5i4reU\":{\"name\":\"Wonde\"},\"byp7w6Xd9Df\":{\"name\":\"Yawei\"},\"BXJdOLvUrZB\":{\"name\":\"Gbendembu Ngowahun\"},\"uKC54fzxRzO\":{\"name\":\"Niawa\"},\"TQkG0sX9nca\":{\"name\":\"Gbense\"},\"hRZOIgQ0O1m\":{\"name\":\"Libeisaygahun\"},\"PrJQHI6q7w2\":{\"name\":\"Tainkatopa Makama Safrokoh\"},\"USQdmvrHh1Q\":{\"name\":\"Kaiyamba\"},\"nOYt1LtFSyU\":{\"name\":\"Bumpeh\"},\"xGMGhjA3y6J\":{\"name\":\"Mambolo\"},\"e1eIKM1GIF3\":{\"name\":\"Gbanti Kamaranka\"},\"lYIM1MXbSYS\":{\"name\":\"Dea\"},\"sxRd2XOzFbz\":{\"name\":\"Tikonko\"},\"d9iMR1MpuIO\":{\"name\":\"Soro-Gbeima\"},\"qgQ49DH9a0v\":{\"name\":\"Nimiyama\"},\"U09TSwIjG0s\":{\"name\":\"Nomo\"},\"zFDYIgyGmXG\":{\"name\":\"Bargbo\"},\"dx\":{\"name\":\"Data\"},\"M2qEv692lS6\":{\"name\":\"Tankoro\"},\"xIKjidMrico\":{\"name\":\"Kowa\"},\"pRHGAROvuyI\":{\"name\":\"Koya\"},\"qtr8GGlm4gg\":{\"name\":\"Rural Western Area\"},\"GWTIxJO9pRo\":{\"name\":\"Gorama Kono\"},\"HWjrSuoNPte\":{\"name\":\"Sanda Magbolonthor\"},\"UhHipWG7J8b\":{\"name\":\"Sanda Tendaren\"},\"rXLor9Knq6l\":{\"name\":\"Kunike Barina\"},\"kU8vhUkAGaT\":{\"name\":\"Lugbu\"},\"C9uduqDZr9d\":{\"name\":\"Freetown\"},\"YmmeuGbqOwR\":{\"name\":\"Gbo\"},\"iEkBZnMDarP\":{\"name\":\"Folosaba Dembelia\"},\"JsxnA2IywRo\":{\"name\":\"Kissi Kama\"},\"nlt6j60tCHF\":{\"name\":\"Mano Sakrim\"},\"g5ptsn0SFX8\":{\"name\":\"Sandor\"},\"XG8HGAbrbbL\":{\"name\":\"Safroko Limba\"},\"pmxZm7klXBy\":{\"name\":\"Peje West\"},\"YuQRtpLP10I\":{\"name\":\"Badjia\"},\"l0ccv2yzfF3\":{\"name\":\"Kunike\"},\"LfTkc0S4b5k\":{\"name\":\"Upper Bambara\"},\"KSdZwrU7Hh6\":{\"name\":\"Jawi\"},\"ajILkI0cfxn\":{\"name\":\"Gbane\"},\"y5X4mP5XylL\":{\"name\":\"Tonko Limba\"},\"fwH9ipvXde9\":{\"name\":\"Biriwa\"},\"vEvs2ckGNQj\":{\"name\":\"Kasonko\"},\"KIUCimTXf8Q\":{\"name\":\"Nongowa\"},\"XEyIRFd9pct\":{\"name\":\"Imperi\"},\"cgOy0hRMGu9\":{\"name\":\"Sogbini\"},\"EjnIQNVAXGp\":{\"name\":\"Mafindor\"},\"x4HaBHHwBML\":{\"name\":\"Malegohun\"},\"EZPwuUTeIIG\":{\"name\":\"Wara Wara Yagala\"},\"N233eZJZ1bh\":{\"name\":\"Pejeh\"},\"smoyi1iYNK6\":{\"name\":\"Kalansogoia\"},\"DNRAeXT9IwS\":{\"name\":\"Dema\"},\"Zoy23SSHCPs\":{\"name\":\"Gbane Kandor\"},\"fRLX08WHWpL\":{\"name\":\"Lokomasama\"},\"LsYpCyYxSLY\":{\"name\":\"Kamaje\"},\"JdqfYTIFZXN\":{\"name\":\"Maforki\"},\"EB1zRKdYjdY\":{\"name\":\"Bendu Cha\"},\"CG4QD1HC3h4\":{\"name\":\"Yawbeko\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\"},\"RndxKqQGzUl\":{\"name\":\"Dasse\"},\"YpVol7asWvd\":{\"name\":\"Kpanga Krim\"},\"P69SId31eDp\":{\"name\":\"Gbonkonlenken\"},\"aWQTfvgPA5v\":{\"name\":\"Kpanda Kemoh\"},\"KctpIIucige\":{\"name\":\"Selenga\"},\"Mr4au3jR9bt\":{\"name\":\"Dembelia Sinkunia\"},\"j0Mtr3xTMjM\":{\"name\":\"Kissi Teng\"},\"L8iA6eLwKNb\":{\"name\":\"Paki Masabong\"},\"DmaLM8WYmWv\":{\"name\":\"Nimikoro\"},\"DfUfwjM9am5\":{\"name\":\"Malen\"},\"FRxcUEwktoV\":{\"name\":\"Toli\"},\"ou\":{\"name\":\"Organisation unit\"},\"VCtF1DbspR5\":{\"name\":\"Jong\"},\"BD9gU0GKlr2\":{\"name\":\"Makpele\"},\"2022\":{\"name\":\"2022\"},\"GFk45MOxzJJ\":{\"name\":\"Neya\"},\"A3Fh37HWBWE\":{\"name\":\"Simbaru\"},\"Lt8U7GVWvSR\":{\"name\":\"Diang\"},\"vzup1f6ynON\":{\"name\":\"Small Bo\"},\"pe\":{\"name\":\"Period\"},\"JdhagCUEMbj\":{\"name\":\"Komboya\"},\"PQZJPIpTepd\":{\"name\":\"Kholifa Rowalla\"},\"EVkm2xYcf6Z\":{\"name\":\"Malal Mara\"},\"WXnNDWTiE9r\":{\"name\":\"Sanda Loko\"},\"RWvG1aFrr0r\":{\"name\":\"Marampa\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\"],\"pe\":[\"2022\"],\"ou\":[\"YuQRtpLP10I\",\"jPidqyo7cpF\",\"vWbkYPRmKyS\",\"dGheVylzol6\",\"zFDYIgyGmXG\",\"RzKeCma9qb1\",\"EB1zRKdYjdY\",\"fwH9ipvXde9\",\"ENHOJz3UH5L\",\"KKkLOTpMXGV\",\"kbPmt60yi0L\",\"iUauWFeH8Qp\",\"BGGmAwx33dj\",\"nOYt1LtFSyU\",\"TA7NvKjsn4A\",\"Pc3JTyqnsmL\",\"myQ4q1W6B4y\",\"RndxKqQGzUl\",\"lYIM1MXbSYS\",\"DNRAeXT9IwS\",\"Mr4au3jR9bt\",\"Lt8U7GVWvSR\",\"ZiOVcrSjSYe\",\"QlCIp2S9NHs\",\"vULnao2hV5v\",\"CF243RPvNY7\",\"iEkBZnMDarP\",\"C9uduqDZr9d\",\"eNtRuQrrZeo\",\"eROJsBwxQHt\",\"ajILkI0cfxn\",\"Zoy23SSHCPs\",\"e1eIKM1GIF3\",\"BXJdOLvUrZB\",\"TQkG0sX9nca\",\"qIRCo0MfuGb\",\"YmmeuGbqOwR\",\"P69SId31eDp\",\"GWTIxJO9pRo\",\"KXSqt7jv6DU\",\"XEyIRFd9pct\",\"daJPPxtIrQn\",\"KSdZwrU7Hh6\",\"VCtF1DbspR5\",\"BmYyh9bZ0sr\",\"vn9KJsLyP5f\",\"USQdmvrHh1Q\",\"U6Kr7Gtpidn\",\"smoyi1iYNK6\",\"LsYpCyYxSLY\",\"kvkDWg42lHR\",\"K1r3uF6eZ8n\",\"Z9QaI6sxTwW\",\"vEvs2ckGNQj\",\"fwxkctgmffZ\",\"PQZJPIpTepd\",\"JsxnA2IywRo\",\"j0Mtr3xTMjM\",\"hjpHnHZIniP\",\"JdhagCUEMbj\",\"Jiyc4ekaMMh\",\"nV3OkyzF4US\",\"xIKjidMrico\",\"pRHGAROvuyI\",\"EYt6ThQDagn\",\"zSNUViKdkk3\",\"aWQTfvgPA5v\",\"QwMiPiME3bA\",\"YpVol7asWvd\",\"l0ccv2yzfF3\",\"rXLor9Knq6l\",\"HV8RTzgcFH3\",\"jWSIbtKfURj\",\"LhaAPLxdSFH\",\"hRZOIgQ0O1m\",\"fRLX08WHWpL\",\"hdEuw2ugkVF\",\"W5fN3G6y1VI\",\"cM2BKSrj9F9\",\"kU8vhUkAGaT\",\"EjnIQNVAXGp\",\"JdqfYTIFZXN\",\"eV4cuxniZgP\",\"QywkxFudXrC\",\"lY93YpCxJqf\",\"BD9gU0GKlr2\",\"EVkm2xYcf6Z\",\"x4HaBHHwBML\",\"GE25DpSrqpB\",\"DfUfwjM9am5\",\"xGMGhjA3y6J\",\"yu4N82FFeLm\",\"nlt6j60tCHF\",\"RWvG1aFrr0r\",\"EfWCa0Cc8WW\",\"FlBemv1NfEC\",\"OTFepb1k9Db\",\"GFk45MOxzJJ\",\"uKC54fzxRzO\",\"I4jWcnFmgEC\",\"J4GiUImJZoE\",\"DmaLM8WYmWv\",\"qgQ49DH9a0v\",\"ERmBhYkhV6Y\",\"U09TSwIjG0s\",\"VP397wRvePm\",\"KIUCimTXf8Q\",\"L8iA6eLwKNb\",\"DxAPPqXvwLy\",\"pmxZm7klXBy\",\"N233eZJZ1bh\",\"bQiBfA2j5cw\",\"gy8rmvYT4cj\",\"qtr8GGlm4gg\",\"XG8HGAbrbbL\",\"r1RUyfVBkLp\",\"r06ohri9wA9\",\"WXnNDWTiE9r\",\"HWjrSuoNPte\",\"UhHipWG7J8b\",\"g5ptsn0SFX8\",\"KctpIIucige\",\"j43EZb15rjI\",\"VGAFxBXz16y\",\"A3Fh37HWBWE\",\"g8DdBm7EmUt\",\"vzup1f6ynON\",\"iGHlidSFdpu\",\"cgOy0hRMGu9\",\"d9iMR1MpuIO\",\"NqWaKXcg01b\",\"PaqugoqjRIj\",\"PrJQHI6q7w2\",\"Qhmi8IZyPyD\",\"xhyjU2SVewz\",\"M2qEv692lS6\",\"sxRd2XOzFbz\",\"AovmOHadayb\",\"FRxcUEwktoV\",\"y5X4mP5XylL\",\"l7pFejMtUoF\",\"LfTkc0S4b5k\",\"DBs6e2Oxaj1\",\"npWGUj37qDe\",\"X7dWcGerQIm\",\"XrF5AvaGcuw\",\"EZPwuUTeIIG\",\"ARZ4y5i4reU\",\"pk7bUK5c1Uf\",\"CG4QD1HC3h4\",\"byp7w6Xd9Df\",\"NNE0YMCDZkO\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("Uvn6LCg7dVU", "YuQRtpLP10I", "298.65"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jPidqyo7cpF", "103.19"));
    validateRow(response, List.of("Uvn6LCg7dVU", "vWbkYPRmKyS", "209.58"));
    validateRow(response, List.of("Uvn6LCg7dVU", "dGheVylzol6", "124.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "zFDYIgyGmXG", "202.18"));
    validateRow(response, List.of("Uvn6LCg7dVU", "RzKeCma9qb1", "84.29"));
    validateRow(response, List.of("Uvn6LCg7dVU", "EB1zRKdYjdY", "86.86"));
    validateRow(response, List.of("Uvn6LCg7dVU", "fwH9ipvXde9", "76.08"));
    validateRow(response, List.of("Uvn6LCg7dVU", "ENHOJz3UH5L", "52.93"));
    validateRow(response, List.of("Uvn6LCg7dVU", "KKkLOTpMXGV", "75.71"));
    validateRow(response, List.of("Uvn6LCg7dVU", "kbPmt60yi0L", "94.74"));
    validateRow(response, List.of("Uvn6LCg7dVU", "iUauWFeH8Qp", "115.26"));
    validateRow(response, List.of("Uvn6LCg7dVU", "BGGmAwx33dj", "118.49"));
    validateRow(response, List.of("Uvn6LCg7dVU", "nOYt1LtFSyU", "128.84"));
    validateRow(response, List.of("Uvn6LCg7dVU", "TA7NvKjsn4A", "94.47"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Pc3JTyqnsmL", "110.56"));
    validateRow(response, List.of("Uvn6LCg7dVU", "myQ4q1W6B4y", "74.81"));
    validateRow(response, List.of("Uvn6LCg7dVU", "RndxKqQGzUl", "140.55"));
    validateRow(response, List.of("Uvn6LCg7dVU", "lYIM1MXbSYS", "73.42"));
    validateRow(response, List.of("Uvn6LCg7dVU", "DNRAeXT9IwS", "66.25"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Mr4au3jR9bt", "61.94"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Lt8U7GVWvSR", "66.53"));
    validateRow(response, List.of("Uvn6LCg7dVU", "ZiOVcrSjSYe", "86.28"));
    validateRow(response, List.of("Uvn6LCg7dVU", "QlCIp2S9NHs", "79.85"));
    validateRow(response, List.of("Uvn6LCg7dVU", "vULnao2hV5v", "106.54"));
    validateRow(response, List.of("Uvn6LCg7dVU", "CF243RPvNY7", "46.93"));
    validateRow(response, List.of("Uvn6LCg7dVU", "iEkBZnMDarP", "67.48"));
    validateRow(response, List.of("Uvn6LCg7dVU", "C9uduqDZr9d", "108.84"));
    validateRow(response, List.of("Uvn6LCg7dVU", "eNtRuQrrZeo", "67.38"));
    validateRow(response, List.of("Uvn6LCg7dVU", "eROJsBwxQHt", "103.54"));
    validateRow(response, List.of("Uvn6LCg7dVU", "ajILkI0cfxn", "49.94"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Zoy23SSHCPs", "60.78"));
    validateRow(response, List.of("Uvn6LCg7dVU", "e1eIKM1GIF3", "89.65"));
    validateRow(response, List.of("Uvn6LCg7dVU", "BXJdOLvUrZB", "94.53"));
    validateRow(response, List.of("Uvn6LCg7dVU", "TQkG0sX9nca", "53.73"));
    validateRow(response, List.of("Uvn6LCg7dVU", "qIRCo0MfuGb", "120.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "YmmeuGbqOwR", "198.48"));
    validateRow(response, List.of("Uvn6LCg7dVU", "P69SId31eDp", "154.28"));
    validateRow(response, List.of("Uvn6LCg7dVU", "GWTIxJO9pRo", "86.29"));
    validateRow(response, List.of("Uvn6LCg7dVU", "KXSqt7jv6DU", "79.84"));
    validateRow(response, List.of("Uvn6LCg7dVU", "XEyIRFd9pct", "88.63"));
    validateRow(response, List.of("Uvn6LCg7dVU", "daJPPxtIrQn", "87.43"));
    validateRow(response, List.of("Uvn6LCg7dVU", "KSdZwrU7Hh6", "68.05"));
    validateRow(response, List.of("Uvn6LCg7dVU", "VCtF1DbspR5", "90.75"));
    validateRow(response, List.of("Uvn6LCg7dVU", "BmYyh9bZ0sr", "150.08"));
    validateRow(response, List.of("Uvn6LCg7dVU", "vn9KJsLyP5f", "123.22"));
    validateRow(response, List.of("Uvn6LCg7dVU", "USQdmvrHh1Q", "73.73"));
    validateRow(response, List.of("Uvn6LCg7dVU", "U6Kr7Gtpidn", "102.38"));
    validateRow(response, List.of("Uvn6LCg7dVU", "smoyi1iYNK6", "110.65"));
    validateRow(response, List.of("Uvn6LCg7dVU", "LsYpCyYxSLY", "175.32"));
    validateRow(response, List.of("Uvn6LCg7dVU", "kvkDWg42lHR", "45.86"));
    validateRow(response, List.of("Uvn6LCg7dVU", "K1r3uF6eZ8n", "84.13"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Z9QaI6sxTwW", "137.35"));
    validateRow(response, List.of("Uvn6LCg7dVU", "vEvs2ckGNQj", "56.77"));
    validateRow(response, List.of("Uvn6LCg7dVU", "fwxkctgmffZ", "107.23"));
    validateRow(response, List.of("Uvn6LCg7dVU", "PQZJPIpTepd", "91.04"));
    validateRow(response, List.of("Uvn6LCg7dVU", "JsxnA2IywRo", "80.78"));
    validateRow(response, List.of("Uvn6LCg7dVU", "j0Mtr3xTMjM", "71.45"));
    validateRow(response, List.of("Uvn6LCg7dVU", "hjpHnHZIniP", "93.63"));
    validateRow(response, List.of("Uvn6LCg7dVU", "JdhagCUEMbj", "183.29"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Jiyc4ekaMMh", "108.97"));
    validateRow(response, List.of("Uvn6LCg7dVU", "nV3OkyzF4US", "132.44"));
    validateRow(response, List.of("Uvn6LCg7dVU", "xIKjidMrico", "112.34"));
    validateRow(response, List.of("Uvn6LCg7dVU", "pRHGAROvuyI", "118.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "EYt6ThQDagn", "113.94"));
    validateRow(response, List.of("Uvn6LCg7dVU", "zSNUViKdkk3", "75.43"));
    validateRow(response, List.of("Uvn6LCg7dVU", "aWQTfvgPA5v", "71.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "QwMiPiME3bA", "86.56"));
    validateRow(response, List.of("Uvn6LCg7dVU", "YpVol7asWvd", "157.14"));
    validateRow(response, List.of("Uvn6LCg7dVU", "l0ccv2yzfF3", "119.21"));
    validateRow(response, List.of("Uvn6LCg7dVU", "rXLor9Knq6l", "116.78"));
    validateRow(response, List.of("Uvn6LCg7dVU", "HV8RTzgcFH3", "138.98"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jWSIbtKfURj", "65.38"));
    validateRow(response, List.of("Uvn6LCg7dVU", "LhaAPLxdSFH", "38.28"));
    validateRow(response, List.of("Uvn6LCg7dVU", "hRZOIgQ0O1m", "90.54"));
    validateRow(response, List.of("Uvn6LCg7dVU", "fRLX08WHWpL", "90.66"));
    validateRow(response, List.of("Uvn6LCg7dVU", "hdEuw2ugkVF", "87.8"));
    validateRow(response, List.of("Uvn6LCg7dVU", "W5fN3G6y1VI", "109.45"));
    validateRow(response, List.of("Uvn6LCg7dVU", "cM2BKSrj9F9", "89.94"));
    validateRow(response, List.of("Uvn6LCg7dVU", "kU8vhUkAGaT", "94.25"));
    validateRow(response, List.of("Uvn6LCg7dVU", "EjnIQNVAXGp", "74.37"));
    validateRow(response, List.of("Uvn6LCg7dVU", "JdqfYTIFZXN", "78.83"));
    validateRow(response, List.of("Uvn6LCg7dVU", "eV4cuxniZgP", "93.01"));
    validateRow(response, List.of("Uvn6LCg7dVU", "QywkxFudXrC", "112.33"));
    validateRow(response, List.of("Uvn6LCg7dVU", "lY93YpCxJqf", "80.38"));
    validateRow(response, List.of("Uvn6LCg7dVU", "BD9gU0GKlr2", "76.59"));
    validateRow(response, List.of("Uvn6LCg7dVU", "EVkm2xYcf6Z", "114.15"));
    validateRow(response, List.of("Uvn6LCg7dVU", "x4HaBHHwBML", "66.45"));
    validateRow(response, List.of("Uvn6LCg7dVU", "GE25DpSrqpB", "90.26"));
    validateRow(response, List.of("Uvn6LCg7dVU", "DfUfwjM9am5", "76.18"));
    validateRow(response, List.of("Uvn6LCg7dVU", "xGMGhjA3y6J", "87.62"));
    validateRow(response, List.of("Uvn6LCg7dVU", "yu4N82FFeLm", "59.23"));
    validateRow(response, List.of("Uvn6LCg7dVU", "nlt6j60tCHF", "109.26"));
    validateRow(response, List.of("Uvn6LCg7dVU", "RWvG1aFrr0r", "111.89"));
    validateRow(response, List.of("Uvn6LCg7dVU", "EfWCa0Cc8WW", "113.26"));
    validateRow(response, List.of("Uvn6LCg7dVU", "FlBemv1NfEC", "103.02"));
    validateRow(response, List.of("Uvn6LCg7dVU", "OTFepb1k9Db", "53.86"));
    validateRow(response, List.of("Uvn6LCg7dVU", "GFk45MOxzJJ", "121.9"));
    validateRow(response, List.of("Uvn6LCg7dVU", "uKC54fzxRzO", "77.34"));
    validateRow(response, List.of("Uvn6LCg7dVU", "I4jWcnFmgEC", "145.09"));
    validateRow(response, List.of("Uvn6LCg7dVU", "J4GiUImJZoE", "73.84"));
    validateRow(response, List.of("Uvn6LCg7dVU", "DmaLM8WYmWv", "57.14"));
    validateRow(response, List.of("Uvn6LCg7dVU", "qgQ49DH9a0v", "49.75"));
    validateRow(response, List.of("Uvn6LCg7dVU", "ERmBhYkhV6Y", "85.41"));
    validateRow(response, List.of("Uvn6LCg7dVU", "U09TSwIjG0s", "59.15"));
    validateRow(response, List.of("Uvn6LCg7dVU", "VP397wRvePm", "99.38"));
    validateRow(response, List.of("Uvn6LCg7dVU", "KIUCimTXf8Q", "111.25"));
    validateRow(response, List.of("Uvn6LCg7dVU", "L8iA6eLwKNb", "86.25"));
    validateRow(response, List.of("Uvn6LCg7dVU", "DxAPPqXvwLy", "87.1"));
    validateRow(response, List.of("Uvn6LCg7dVU", "pmxZm7klXBy", "55.18"));
    validateRow(response, List.of("Uvn6LCg7dVU", "N233eZJZ1bh", "54.74"));
    validateRow(response, List.of("Uvn6LCg7dVU", "bQiBfA2j5cw", "94.63"));
    validateRow(response, List.of("Uvn6LCg7dVU", "gy8rmvYT4cj", "137.82"));
    validateRow(response, List.of("Uvn6LCg7dVU", "qtr8GGlm4gg", "164.68"));
    validateRow(response, List.of("Uvn6LCg7dVU", "XG8HGAbrbbL", "79.99"));
    validateRow(response, List.of("Uvn6LCg7dVU", "r1RUyfVBkLp", "210.04"));
    validateRow(response, List.of("Uvn6LCg7dVU", "r06ohri9wA9", "102.86"));
    validateRow(response, List.of("Uvn6LCg7dVU", "WXnNDWTiE9r", "80.89"));
    validateRow(response, List.of("Uvn6LCg7dVU", "HWjrSuoNPte", "94.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "UhHipWG7J8b", "85.69"));
    validateRow(response, List.of("Uvn6LCg7dVU", "g5ptsn0SFX8", "63.82"));
    validateRow(response, List.of("Uvn6LCg7dVU", "KctpIIucige", "89.33"));
    validateRow(response, List.of("Uvn6LCg7dVU", "j43EZb15rjI", "91.16"));
    validateRow(response, List.of("Uvn6LCg7dVU", "VGAFxBXz16y", "74.15"));
    validateRow(response, List.of("Uvn6LCg7dVU", "A3Fh37HWBWE", "107.81"));
    validateRow(response, List.of("Uvn6LCg7dVU", "g8DdBm7EmUt", "64.51"));
    validateRow(response, List.of("Uvn6LCg7dVU", "vzup1f6ynON", "110.99"));
    validateRow(response, List.of("Uvn6LCg7dVU", "iGHlidSFdpu", "48.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "cgOy0hRMGu9", "93.53"));
    validateRow(response, List.of("Uvn6LCg7dVU", "d9iMR1MpuIO", "82.51"));
    validateRow(response, List.of("Uvn6LCg7dVU", "NqWaKXcg01b", "150.74"));
    validateRow(response, List.of("Uvn6LCg7dVU", "PaqugoqjRIj", "61.21"));
    validateRow(response, List.of("Uvn6LCg7dVU", "PrJQHI6q7w2", "70.87"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Qhmi8IZyPyD", "77.68"));
    validateRow(response, List.of("Uvn6LCg7dVU", "xhyjU2SVewz", "134.08"));
    validateRow(response, List.of("Uvn6LCg7dVU", "M2qEv692lS6", "39.41"));
    validateRow(response, List.of("Uvn6LCg7dVU", "sxRd2XOzFbz", "199.16"));
    validateRow(response, List.of("Uvn6LCg7dVU", "AovmOHadayb", "125.95"));
    validateRow(response, List.of("Uvn6LCg7dVU", "FRxcUEwktoV", "72.05"));
    validateRow(response, List.of("Uvn6LCg7dVU", "y5X4mP5XylL", "99.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "l7pFejMtUoF", "94.35"));
    validateRow(response, List.of("Uvn6LCg7dVU", "LfTkc0S4b5k", "103.67"));
    validateRow(response, List.of("Uvn6LCg7dVU", "DBs6e2Oxaj1", "133.41"));
    validateRow(response, List.of("Uvn6LCg7dVU", "npWGUj37qDe", "94.03"));
    validateRow(response, List.of("Uvn6LCg7dVU", "X7dWcGerQIm", "88.73"));
    validateRow(response, List.of("Uvn6LCg7dVU", "XrF5AvaGcuw", "67.33"));
    validateRow(response, List.of("Uvn6LCg7dVU", "EZPwuUTeIIG", "62.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "ARZ4y5i4reU", "82.57"));
    validateRow(response, List.of("Uvn6LCg7dVU", "pk7bUK5c1Uf", "58.63"));
    validateRow(response, List.of("Uvn6LCg7dVU", "CG4QD1HC3h4", "91.94"));
    validateRow(response, List.of("Uvn6LCg7dVU", "byp7w6Xd9Df", "82.02"));
    validateRow(response, List.of("Uvn6LCg7dVU", "NNE0YMCDZkO", "131.36"));
  }
}
