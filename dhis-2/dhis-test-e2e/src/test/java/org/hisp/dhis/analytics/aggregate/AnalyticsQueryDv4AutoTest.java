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
public class AnalyticsQueryDv4AutoTest extends AnalyticsApiTest {

  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void queryAnc3CoverageByDistrictsLast4Quarters() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=dx:sB79w2hiLp8")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=ou:TEQlaapDQoK;Vth0fbpFcsO;bL4ooGhyHRQ;jmIPBj66vD6;qhqAxPSTUXp;LEVEL-wjP19dkFeIk,pe:LAST_4_QUARTERS")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(20)))
        .body("height", equalTo(20))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"sB79w2hiLp8\":{\"name\":\"ANC 3 Coverage\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"ou\":{\"name\":\"Organisation unit\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"wjP19dkFeIk\":{\"uid\":\"wjP19dkFeIk\",\"name\":\"District\"},\"2021Q4\":{\"name\":\"October - December 2021\"},\"2021Q2\":{\"name\":\"April - June 2021\"},\"2021Q3\":{\"name\":\"July - September 2021\"},\"2021Q1\":{\"name\":\"January - March 2021\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"LAST_4_QUARTERS\":{\"name\":\"Last 4 quarters\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"sB79w2hiLp8\"],\"pe\":[\"2021Q1\",\"2021Q2\",\"2021Q3\",\"2021Q4\"],\"ou\":[\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("qhqAxPSTUXp", "2021Q1", "41.96"));
    validateRow(response, List.of("qhqAxPSTUXp", "2021Q2", "44.18"));
    validateRow(response, List.of("qhqAxPSTUXp", "2021Q3", "51.67"));
    validateRow(response, List.of("qhqAxPSTUXp", "2021Q4", "20.46"));
    validateRow(response, List.of("Vth0fbpFcsO", "2021Q1", "42.31"));
    validateRow(response, List.of("Vth0fbpFcsO", "2021Q2", "48.77"));
    validateRow(response, List.of("Vth0fbpFcsO", "2021Q3", "43.88"));
    validateRow(response, List.of("Vth0fbpFcsO", "2021Q4", "15.94"));
    validateRow(response, List.of("jmIPBj66vD6", "2021Q1", "98.27"));
    validateRow(response, List.of("jmIPBj66vD6", "2021Q2", "99.02"));
    validateRow(response, List.of("jmIPBj66vD6", "2021Q3", "96.13"));
    validateRow(response, List.of("jmIPBj66vD6", "2021Q4", "83.89"));
    validateRow(response, List.of("TEQlaapDQoK", "2021Q1", "45.78"));
    validateRow(response, List.of("TEQlaapDQoK", "2021Q2", "55.62"));
    validateRow(response, List.of("TEQlaapDQoK", "2021Q3", "57.1"));
    validateRow(response, List.of("TEQlaapDQoK", "2021Q4", "36.57"));
    validateRow(response, List.of("bL4ooGhyHRQ", "2021Q1", "75.32"));
    validateRow(response, List.of("bL4ooGhyHRQ", "2021Q2", "74.83"));
    validateRow(response, List.of("bL4ooGhyHRQ", "2021Q3", "82.02"));
    validateRow(response, List.of("bL4ooGhyHRQ", "2021Q4", "0.74"));
  }

  @Test
  public void queryAnc3CoverageThisYeargauge() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:THIS_YEAR")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:sB79w2hiLp8")
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
        "{\"items\":{\"sB79w2hiLp8\":{\"name\":\"ANC 3 Coverage\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"2022\":{\"name\":\"2022\"}},\"dimensions\":{\"dx\":[\"sB79w2hiLp8\"],\"pe\":[\"2022\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("sB79w2hiLp8", "65.83"));
  }

  @Test
  public void queryAnc3rdVisitsByFacilityTypeLast12Months100StackedColumns() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:ImspTQPwCqd,dx:Jtf34kNZhzP")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=J5jldMd8OHv:uYxK4wmcPqA;EYbopBOJWsW;RXL3lPSK8oG;CXw2yu5fodb,pe:LAST_12_MONTHS")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(48)))
        .body("height", equalTo(48))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"J5jldMd8OHv\":{\"name\":\"Facility Type\"},\"202109\":{\"name\":\"September 2021\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"uYxK4wmcPqA\":{\"name\":\"CHP\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202112\":{\"name\":\"December 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"Jtf34kNZhzP\":{\"name\":\"ANC 3rd visit\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"},\"RXL3lPSK8oG\":{\"name\":\"Clinic\"},\"ou\":{\"name\":\"Organisation unit\"},\"CXw2yu5fodb\":{\"name\":\"CHC\"},\"202101\":{\"name\":\"January 2021\"},\"202102\":{\"name\":\"February 2021\"},\"pe\":{\"name\":\"Period\"},\"EYbopBOJWsW\":{\"name\":\"MCHP\"}},\"dimensions\":{\"dx\":[\"Jtf34kNZhzP\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"J5jldMd8OHv\":[\"uYxK4wmcPqA\",\"EYbopBOJWsW\",\"RXL3lPSK8oG\",\"CXw2yu5fodb\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "J5jldMd8OHv", "Facility Type", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("RXL3lPSK8oG", "202102", "472"));
    validateRow(response, List.of("RXL3lPSK8oG", "202101", "549"));
    validateRow(response, List.of("RXL3lPSK8oG", "202104", "440"));
    validateRow(response, List.of("RXL3lPSK8oG", "202103", "502"));
    validateRow(response, List.of("uYxK4wmcPqA", "202105", "2636"));
    validateRow(response, List.of("RXL3lPSK8oG", "202109", "634"));
    validateRow(response, List.of("uYxK4wmcPqA", "202104", "2294"));
    validateRow(response, List.of("uYxK4wmcPqA", "202103", "2520"));
    validateRow(response, List.of("uYxK4wmcPqA", "202102", "2205"));
    validateRow(response, List.of("RXL3lPSK8oG", "202106", "603"));
    validateRow(response, List.of("uYxK4wmcPqA", "202101", "2164"));
    validateRow(response, List.of("RXL3lPSK8oG", "202105", "500"));
    validateRow(response, List.of("RXL3lPSK8oG", "202108", "495"));
    validateRow(response, List.of("RXL3lPSK8oG", "202107", "617"));
    validateRow(response, List.of("uYxK4wmcPqA", "202109", "2685"));
    validateRow(response, List.of("uYxK4wmcPqA", "202108", "2814"));
    validateRow(response, List.of("uYxK4wmcPqA", "202107", "2717"));
    validateRow(response, List.of("uYxK4wmcPqA", "202106", "2809"));
    validateRow(response, List.of("uYxK4wmcPqA", "202112", "1813"));
    validateRow(response, List.of("uYxK4wmcPqA", "202111", "2250"));
    validateRow(response, List.of("uYxK4wmcPqA", "202110", "2135"));
    validateRow(response, List.of("CXw2yu5fodb", "202107", "4097"));
    validateRow(response, List.of("CXw2yu5fodb", "202106", "4643"));
    validateRow(response, List.of("CXw2yu5fodb", "202109", "4505"));
    validateRow(response, List.of("CXw2yu5fodb", "202108", "4137"));
    validateRow(response, List.of("EYbopBOJWsW", "202102", "5134"));
    validateRow(response, List.of("EYbopBOJWsW", "202101", "5016"));
    validateRow(response, List.of("CXw2yu5fodb", "202110", "3219"));
    validateRow(response, List.of("CXw2yu5fodb", "202112", "2983"));
    validateRow(response, List.of("CXw2yu5fodb", "202111", "3807"));
    validateRow(response, List.of("EYbopBOJWsW", "202112", "4168"));
    validateRow(response, List.of("EYbopBOJWsW", "202111", "7058"));
    validateRow(response, List.of("EYbopBOJWsW", "202110", "4930"));
    validateRow(response, List.of("RXL3lPSK8oG", "202112", "518"));
    validateRow(response, List.of("EYbopBOJWsW", "202109", "5977"));
    validateRow(response, List.of("EYbopBOJWsW", "202108", "5894"));
    validateRow(response, List.of("EYbopBOJWsW", "202107", "5898"));
    validateRow(response, List.of("EYbopBOJWsW", "202106", "6237"));
    validateRow(response, List.of("EYbopBOJWsW", "202105", "6729"));
    validateRow(response, List.of("RXL3lPSK8oG", "202111", "565"));
    validateRow(response, List.of("EYbopBOJWsW", "202104", "5444"));
    validateRow(response, List.of("EYbopBOJWsW", "202103", "5645"));
    validateRow(response, List.of("RXL3lPSK8oG", "202110", "483"));
    validateRow(response, List.of("CXw2yu5fodb", "202103", "3974"));
    validateRow(response, List.of("CXw2yu5fodb", "202102", "3392"));
    validateRow(response, List.of("CXw2yu5fodb", "202105", "4179"));
    validateRow(response, List.of("CXw2yu5fodb", "202104", "3542"));
    validateRow(response, List.of("CXw2yu5fodb", "202101", "3318"));
  }

  @Test
  public void queryAncAtFacilitiesInBoThisMonthWithHiddenEmptyColumns() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=ou:zFDYIgyGmXG;BGGmAwx33dj;YmmeuGbqOwR;YuQRtpLP10I;LEVEL-m9lBJogzE95,pe:THIS_MONTH,dx:fbfJHSPpUQD;cYeuwXTCPkU;Jtf34kNZhzP;hfdmMSPBgLG;bqK6eSIwo3h;yTHydhurQQU;V37YqbqpEhV;SA7WeFZnUci")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(114)))
        .body("height", equalTo(114))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"p9ZtyC3LQ9f\":{\"name\":\"Niagorehun CHP\"},\"lvxIJAb2QJo\":{\"name\":\"Sembehun Mamagewor MCHP\"},\"AXZq6q7Dr6E\":{\"name\":\"Buma MCHP\"},\"cJkZLwhL8RP\":{\"name\":\"Kasse MCHP\"},\"m9lBJogzE95\":{\"uid\":\"m9lBJogzE95\",\"name\":\"Facility\"},\"xt08cuqf1ys\":{\"name\":\"Mokoba MCHP\"},\"SA7WeFZnUci\":{\"name\":\"IPT 2nd dose given by TBA\"},\"THIS_MONTH\":{\"name\":\"This month\"},\"jGYT5U5qJP6\":{\"name\":\"Gbaiima CHC\"},\"zFDYIgyGmXG\":{\"uid\":\"zFDYIgyGmXG\",\"code\":\"OU_542\",\"name\":\"Bargbo\"},\"hfdmMSPBgLG\":{\"name\":\"ANC 4th or more visits\"},\"dx\":{\"name\":\"Data\"},\"KvE0PYQzXMM\":{\"name\":\"Mano Yorgbo MCHP\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"vELbGdEphPd\":{\"name\":\"Jimmi CHC\"},\"Tht0fnjagHi\":{\"name\":\"Serabu Hospital Mission\"},\"Jtf34kNZhzP\":{\"name\":\"ANC 3rd visit\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"},\"ctMepV9p92I\":{\"name\":\"Gbangbalia MCHP\"},\"prNiMdHuaaU\":{\"name\":\"Serabu (Bumpe Ngao) UFC\"},\"kEkU53NrFmy\":{\"name\":\"Taninahun (BN) CHP\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"E497Rk80ivZ\":{\"name\":\"Bumpe CHC\"},\"YmmeuGbqOwR\":{\"uid\":\"YmmeuGbqOwR\",\"code\":\"OU_544\",\"name\":\"Gbo\"},\"yTHydhurQQU\":{\"name\":\"IPT 1st dose given by TBA\"},\"fA43H8Ds0Ja\":{\"name\":\"Momajo MCHP\"},\"ou\":{\"name\":\"Organisation unit\"},\"fbfJHSPpUQD\":{\"name\":\"ANC 1st visit\"},\"YuQRtpLP10I\":{\"uid\":\"YuQRtpLP10I\",\"code\":\"OU_539\",\"name\":\"Badjia\"},\"EJoI3HArJ2W\":{\"name\":\"Bum Kaku MCHP\"},\"CTOMXJg41hz\":{\"name\":\"Kaniya MCHP\"},\"wwM3YPvBKu2\":{\"name\":\"Ngolahun CHC\"},\"RTixJpRqS4C\":{\"name\":\"Kpetema CHP\"},\"g8upMTyEZGZ\":{\"name\":\"Njandama MCHP\"},\"202201\":{\"name\":\"January 2022\"},\"BGGmAwx33dj\":{\"uid\":\"BGGmAwx33dj\",\"code\":\"OU_543\",\"name\":\"Bumpe Ngao\"},\"pe\":{\"name\":\"Period\"},\"cYeuwXTCPkU\":{\"name\":\"ANC 2nd visit\"},\"bqK6eSIwo3h\":{\"name\":\"IPT 1st dose given at PHU\"},\"am6EFqHGKeU\":{\"name\":\"Mokpende MCHP\"},\"V37YqbqpEhV\":{\"name\":\"IPT 2nd dose given at PHU\"},\"tZxqVn3xNrA\":{\"name\":\"Wallehun MCHP\"},\"EFTcruJcNmZ\":{\"name\":\"Yengema CHP\"}},\"dimensions\":{\"dx\":[\"fbfJHSPpUQD\",\"cYeuwXTCPkU\",\"Jtf34kNZhzP\",\"hfdmMSPBgLG\",\"bqK6eSIwo3h\",\"yTHydhurQQU\",\"V37YqbqpEhV\",\"SA7WeFZnUci\"],\"pe\":[\"202201\"],\"ou\":[\"EJoI3HArJ2W\",\"AXZq6q7Dr6E\",\"E497Rk80ivZ\",\"jGYT5U5qJP6\",\"ctMepV9p92I\",\"vELbGdEphPd\",\"CTOMXJg41hz\",\"cJkZLwhL8RP\",\"RTixJpRqS4C\",\"KvE0PYQzXMM\",\"xt08cuqf1ys\",\"am6EFqHGKeU\",\"fA43H8Ds0Ja\",\"DiszpKrYNg8\",\"wwM3YPvBKu2\",\"p9ZtyC3LQ9f\",\"g8upMTyEZGZ\",\"lvxIJAb2QJo\",\"prNiMdHuaaU\",\"Tht0fnjagHi\",\"kEkU53NrFmy\",\"tZxqVn3xNrA\",\"EFTcruJcNmZ\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
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

    // Assert rows.
    validateRow(
        response, List.of("Jtf34kNZhzP", "am6EFqHGKeU", "202201", "28", "", "", "", "", ""));
    validateRow(
        response, List.of("bqK6eSIwo3h", "DiszpKrYNg8", "202201", "57", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "prNiMdHuaaU", "202201", "30", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "prNiMdHuaaU", "202201", "32", "", "", "", "", ""));
    validateRow(response, List.of("bqK6eSIwo3h", "xt08cuqf1ys", "202201", "9", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "vELbGdEphPd", "202201", "160", "", "", "", "", ""));
    validateRow(response, List.of("hfdmMSPBgLG", "prNiMdHuaaU", "202201", "1", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "am6EFqHGKeU", "202201", "20", "", "", "", "", ""));
    validateRow(response, List.of("yTHydhurQQU", "RTixJpRqS4C", "202201", "7", "", "", "", "", ""));
    validateRow(
        response, List.of("Jtf34kNZhzP", "prNiMdHuaaU", "202201", "15", "", "", "", "", ""));
    validateRow(
        response, List.of("bqK6eSIwo3h", "kEkU53NrFmy", "202201", "32", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "prNiMdHuaaU", "202201", "20", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD", "ctMepV9p92I", "202201", "5", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "jGYT5U5qJP6", "202201", "12", "", "", "", "", ""));
    validateRow(response, List.of("hfdmMSPBgLG", "vELbGdEphPd", "202201", "3", "", "", "", "", ""));
    validateRow(response, List.of("yTHydhurQQU", "ctMepV9p92I", "202201", "9", "", "", "", "", ""));
    validateRow(response, List.of("Jtf34kNZhzP", "jGYT5U5qJP6", "202201", "9", "", "", "", "", ""));
    validateRow(
        response, List.of("Jtf34kNZhzP", "vELbGdEphPd", "202201", "39", "", "", "", "", ""));
    validateRow(response, List.of("Jtf34kNZhzP", "xt08cuqf1ys", "202201", "3", "", "", "", "", ""));
    validateRow(response, List.of("V37YqbqpEhV", "vELbGdEphPd", "202201", "4", "", "", "", "", ""));
    validateRow(
        response, List.of("bqK6eSIwo3h", "EFTcruJcNmZ", "202201", "10", "", "", "", "", ""));
    validateRow(response, List.of("bqK6eSIwo3h", "EJoI3HArJ2W", "202201", "6", "", "", "", "", ""));
    validateRow(response, List.of("V37YqbqpEhV", "xt08cuqf1ys", "202201", "5", "", "", "", "", ""));
    validateRow(response, List.of("SA7WeFZnUci", "EJoI3HArJ2W", "202201", "3", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU", "g8upMTyEZGZ", "202201", "6", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "jGYT5U5qJP6", "202201", "20", "", "", "", "", ""));
    validateRow(response, List.of("hfdmMSPBgLG", "ctMepV9p92I", "202201", "3", "", "", "", "", ""));
    validateRow(response, List.of("V37YqbqpEhV", "EJoI3HArJ2W", "202201", "1", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "xt08cuqf1ys", "202201", "20", "", "", "", "", ""));
    validateRow(
        response, List.of("bqK6eSIwo3h", "E497Rk80ivZ", "202201", "32", "", "", "", "", ""));
    validateRow(
        response, List.of("bqK6eSIwo3h", "am6EFqHGKeU", "202201", "35", "", "", "", "", ""));
    validateRow(response, List.of("bqK6eSIwo3h", "p9ZtyC3LQ9f", "202201", "7", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "DiszpKrYNg8", "202201", "57", "", "", "", "", ""));
    validateRow(
        response, List.of("Jtf34kNZhzP", "EFTcruJcNmZ", "202201", "16", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD", "fA43H8Ds0Ja", "202201", "6", "", "", "", "", ""));
    validateRow(response, List.of("Jtf34kNZhzP", "ctMepV9p92I", "202201", "3", "", "", "", "", ""));
    validateRow(response, List.of("yTHydhurQQU", "CTOMXJg41hz", "202201", "4", "", "", "", "", ""));
    validateRow(
        response, List.of("Jtf34kNZhzP", "fA43H8Ds0Ja", "202201", "39", "", "", "", "", ""));
    validateRow(
        response, List.of("hfdmMSPBgLG", "fA43H8Ds0Ja", "202201", "13", "", "", "", "", ""));
    validateRow(response, List.of("Jtf34kNZhzP", "g8upMTyEZGZ", "202201", "3", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "KvE0PYQzXMM", "202201", "13", "", "", "", "", ""));
    validateRow(
        response, List.of("bqK6eSIwo3h", "KvE0PYQzXMM", "202201", "13", "", "", "", "", ""));
    validateRow(
        response, List.of("yTHydhurQQU", "DiszpKrYNg8", "202201", "57", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "cJkZLwhL8RP", "202201", "22", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "DiszpKrYNg8", "202201", "13", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "vELbGdEphPd", "202201", "79", "", "", "", "", ""));
    validateRow(response, List.of("hfdmMSPBgLG", "KvE0PYQzXMM", "202201", "4", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "E497Rk80ivZ", "202201", "35", "", "", "", "", ""));
    validateRow(response, List.of("Jtf34kNZhzP", "KvE0PYQzXMM", "202201", "7", "", "", "", "", ""));
    validateRow(
        response, List.of("bqK6eSIwo3h", "jGYT5U5qJP6", "202201", "69", "", "", "", "", ""));
    validateRow(response, List.of("bqK6eSIwo3h", "g8upMTyEZGZ", "202201", "7", "", "", "", "", ""));
    validateRow(response, List.of("hfdmMSPBgLG", "am6EFqHGKeU", "202201", "8", "", "", "", "", ""));
    validateRow(response, List.of("SA7WeFZnUci", "RTixJpRqS4C", "202201", "5", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD", "cJkZLwhL8RP", "202201", "7", "", "", "", "", ""));
    validateRow(
        response, List.of("yTHydhurQQU", "cJkZLwhL8RP", "202201", "15", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "RTixJpRqS4C", "202201", "18", "", "", "", "", ""));
    validateRow(
        response, List.of("hfdmMSPBgLG", "EFTcruJcNmZ", "202201", "23", "", "", "", "", ""));
    validateRow(
        response, List.of("SA7WeFZnUci", "DiszpKrYNg8", "202201", "46", "", "", "", "", ""));
    validateRow(response, List.of("V37YqbqpEhV", "kEkU53NrFmy", "202201", "7", "", "", "", "", ""));
    validateRow(response, List.of("SA7WeFZnUci", "CTOMXJg41hz", "202201", "7", "", "", "", "", ""));
    validateRow(response, List.of("Jtf34kNZhzP", "p9ZtyC3LQ9f", "202201", "7", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "CTOMXJg41hz", "202201", "20", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "CTOMXJg41hz", "202201", "25", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "RTixJpRqS4C", "202201", "12", "", "", "", "", ""));
    validateRow(response, List.of("Jtf34kNZhzP", "E497Rk80ivZ", "202201", "5", "", "", "", "", ""));
    validateRow(response, List.of("hfdmMSPBgLG", "p9ZtyC3LQ9f", "202201", "1", "", "", "", "", ""));
    validateRow(
        response, List.of("hfdmMSPBgLG", "CTOMXJg41hz", "202201", "29", "", "", "", "", ""));
    validateRow(
        response, List.of("Jtf34kNZhzP", "RTixJpRqS4C", "202201", "13", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "p9ZtyC3LQ9f", "202201", "18", "", "", "", "", ""));
    validateRow(response, List.of("V37YqbqpEhV", "E497Rk80ivZ", "202201", "1", "", "", "", "", ""));
    validateRow(
        response, List.of("bqK6eSIwo3h", "ctMepV9p92I", "202201", "24", "", "", "", "", ""));
    validateRow(
        response, List.of("Jtf34kNZhzP", "CTOMXJg41hz", "202201", "27", "", "", "", "", ""));
    validateRow(
        response, List.of("bqK6eSIwo3h", "vELbGdEphPd", "202201", "40", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "kEkU53NrFmy", "202201", "32", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "CTOMXJg41hz", "202201", "18", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU", "kEkU53NrFmy", "202201", "7", "", "", "", "", ""));
    validateRow(
        response, List.of("bqK6eSIwo3h", "prNiMdHuaaU", "202201", "37", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "EFTcruJcNmZ", "202201", "10", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "p9ZtyC3LQ9f", "202201", "24", "", "", "", "", ""));
    validateRow(response, List.of("yTHydhurQQU", "EJoI3HArJ2W", "202201", "4", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "ctMepV9p92I", "202201", "15", "", "", "", "", ""));
    validateRow(
        response, List.of("bqK6eSIwo3h", "fA43H8Ds0Ja", "202201", "72", "", "", "", "", ""));
    validateRow(
        response, List.of("yTHydhurQQU", "xt08cuqf1ys", "202201", "12", "", "", "", "", ""));
    validateRow(
        response, List.of("hfdmMSPBgLG", "RTixJpRqS4C", "202201", "14", "", "", "", "", ""));
    validateRow(response, List.of("hfdmMSPBgLG", "DiszpKrYNg8", "202201", "1", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD", "EJoI3HArJ2W", "202201", "6", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "EFTcruJcNmZ", "202201", "18", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "EJoI3HArJ2W", "202201", "11", "", "", "", "", ""));
    validateRow(response, List.of("hfdmMSPBgLG", "xt08cuqf1ys", "202201", "1", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU", "ctMepV9p92I", "202201", "8", "", "", "", "", ""));
    validateRow(
        response, List.of("Jtf34kNZhzP", "DiszpKrYNg8", "202201", "48", "", "", "", "", ""));
    validateRow(response, List.of("bqK6eSIwo3h", "RTixJpRqS4C", "202201", "8", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "fA43H8Ds0Ja", "202201", "18", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "cJkZLwhL8RP", "202201", "14", "", "", "", "", ""));
    validateRow(response, List.of("Jtf34kNZhzP", "EJoI3HArJ2W", "202201", "1", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "EFTcruJcNmZ", "202201", "18", "", "", "", "", ""));
    validateRow(response, List.of("hfdmMSPBgLG", "cJkZLwhL8RP", "202201", "9", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "am6EFqHGKeU", "202201", "35", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "xt08cuqf1ys", "202201", "15", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "KvE0PYQzXMM", "202201", "10", "", "", "", "", ""));
    validateRow(response, List.of("Jtf34kNZhzP", "cJkZLwhL8RP", "202201", "6", "", "", "", "", ""));
    validateRow(response, List.of("Jtf34kNZhzP", "kEkU53NrFmy", "202201", "7", "", "", "", "", ""));
    validateRow(response, List.of("SA7WeFZnUci", "xt08cuqf1ys", "202201", "3", "", "", "", "", ""));
    validateRow(response, List.of("V37YqbqpEhV", "g8upMTyEZGZ", "202201", "3", "", "", "", "", ""));
    validateRow(response, List.of("bqK6eSIwo3h", "cJkZLwhL8RP", "202201", "7", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "E497Rk80ivZ", "202201", "50", "", "", "", "", ""));
    validateRow(
        response, List.of("fbfJHSPpUQD", "jGYT5U5qJP6", "202201", "23", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "DiszpKrYNg8", "202201", "36", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "KvE0PYQzXMM", "202201", "12", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD", "g8upMTyEZGZ", "202201", "7", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "p9ZtyC3LQ9f", "202201", "12", "", "", "", "", ""));
    validateRow(
        response, List.of("cYeuwXTCPkU", "RTixJpRqS4C", "202201", "30", "", "", "", "", ""));
    validateRow(
        response, List.of("bqK6eSIwo3h", "CTOMXJg41hz", "202201", "13", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "am6EFqHGKeU", "202201", "20", "", "", "", "", ""));
  }

  @Test
  public void queryAncByAreaLastMonth() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:ImspTQPwCqd")
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=dx:Uvn6LCg7dVU;ReUHfIn0pTQ;OdiHJayrsKo;Lzg9LtG1xg3;sB79w2hiLp8;AUqdhY4mpvp;dwEq7wi6nXV;c8fABiNpT0B,pe:LAST_MONTH,uIuxlbV1vRT:J40PpdN4Wkk;b0EsAxm8Nge;jqBqIXoXpfy;nlX2VoouN63")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(32)))
        .body("height", equalTo(32))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"nlX2VoouN63\":{\"name\":\"Eastern Area\"},\"sB79w2hiLp8\":{\"name\":\"ANC 3 Coverage\"},\"ou\":{\"name\":\"Organisation unit\"},\"OdiHJayrsKo\":{\"name\":\"ANC 2 Coverage\"},\"jqBqIXoXpfy\":{\"name\":\"Southern Area\"},\"uIuxlbV1vRT\":{\"name\":\"Area\"},\"AUqdhY4mpvp\":{\"name\":\"ANC => 4 Coverage\"},\"Lzg9LtG1xg3\":{\"name\":\"ANC visits per clinical professional\"},\"LAST_MONTH\":{\"name\":\"Last month\"},\"J40PpdN4Wkk\":{\"name\":\"Northern Area\"},\"202112\":{\"name\":\"December 2021\"},\"b0EsAxm8Nge\":{\"name\":\"Western Area\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"c8fABiNpT0B\":{\"name\":\"ANC IPT 2 Coverage\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"},\"ReUHfIn0pTQ\":{\"name\":\"ANC 1-3 Dropout Rate\"},\"dwEq7wi6nXV\":{\"name\":\"ANC IPT 1 Coverage\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\",\"ReUHfIn0pTQ\",\"OdiHJayrsKo\",\"Lzg9LtG1xg3\",\"sB79w2hiLp8\",\"AUqdhY4mpvp\",\"dwEq7wi6nXV\",\"c8fABiNpT0B\"],\"pe\":[\"202112\"],\"ou\":[\"ImspTQPwCqd\"],\"uIuxlbV1vRT\":[\"J40PpdN4Wkk\",\"b0EsAxm8Nge\",\"jqBqIXoXpfy\",\"nlX2VoouN63\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "uIuxlbV1vRT", "Area", "TEXT", "java.lang.String", false, true);
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
        response,
        List.of(
            "Uvn6LCg7dVU",
            "202112",
            "J40PpdN4Wkk",
            "76.44",
            "4447.0",
            "68500.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "202112",
            "b0EsAxm8Nge",
            "79.64",
            "5173.0",
            "76477.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "202112",
            "jqBqIXoXpfy",
            "88.82",
            "3296.0",
            "43695.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "202112",
            "nlX2VoouN63",
            "45.45",
            "1158.0",
            "30002.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "ReUHfIn0pTQ",
            "202112",
            "J40PpdN4Wkk",
            "43.67",
            "1942.0",
            "4447.0",
            "100.0",
            "100",
            "1"));
    validateRow(
        response,
        List.of(
            "ReUHfIn0pTQ",
            "202112",
            "b0EsAxm8Nge",
            "43.67",
            "2259.0",
            "5173.0",
            "100.0",
            "100",
            "1"));
    validateRow(
        response,
        List.of(
            "ReUHfIn0pTQ",
            "202112",
            "jqBqIXoXpfy",
            "27.88",
            "919.0",
            "3296.0",
            "100.0",
            "100",
            "1"));
    validateRow(
        response,
        List.of(
            "ReUHfIn0pTQ",
            "202112",
            "nlX2VoouN63",
            "13.39",
            "155.0",
            "1158.0",
            "100.0",
            "100",
            "1"));
    validateRow(
        response,
        List.of(
            "OdiHJayrsKo",
            "202112",
            "J40PpdN4Wkk",
            "70.58",
            "4106.0",
            "68500.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "OdiHJayrsKo",
            "202112",
            "b0EsAxm8Nge",
            "62.48",
            "4058.0",
            "76477.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "OdiHJayrsKo",
            "202112",
            "jqBqIXoXpfy",
            "84.88",
            "3150.0",
            "43695.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "OdiHJayrsKo",
            "202112",
            "nlX2VoouN63",
            "46.58",
            "1187.0",
            "30002.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "Lzg9LtG1xg3", "202112", "J40PpdN4Wkk", "27.0", "12122.0", "449.0", "1.0", "1", "1"));
    validateRow(
        response,
        List.of(
            "Lzg9LtG1xg3", "202112", "b0EsAxm8Nge", "2873.4", "14367.0", "5.0", "1.0", "1", "1"));
    validateRow(
        response,
        List.of(
            "Lzg9LtG1xg3", "202112", "jqBqIXoXpfy", "38.41", "10908.0", "284.0", "1.0", "1", "1"));
    validateRow(
        response,
        List.of(
            "Lzg9LtG1xg3", "202112", "nlX2VoouN63", "11.56", "4036.0", "349.0", "1.0", "1", "1"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "202112",
            "J40PpdN4Wkk",
            "43.06",
            "2505.0",
            "68500.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "202112",
            "b0EsAxm8Nge",
            "44.86",
            "2914.0",
            "76477.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "202112",
            "jqBqIXoXpfy",
            "64.05",
            "2377.0",
            "43695.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "202112",
            "nlX2VoouN63",
            "39.36",
            "1003.0",
            "30002.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "AUqdhY4mpvp",
            "202112",
            "J40PpdN4Wkk",
            "18.29",
            "1064.0",
            "68500.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "AUqdhY4mpvp",
            "202112",
            "b0EsAxm8Nge",
            "34.21",
            "2222.0",
            "76477.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "AUqdhY4mpvp",
            "202112",
            "jqBqIXoXpfy",
            "56.18",
            "2085.0",
            "43695.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "AUqdhY4mpvp",
            "202112",
            "nlX2VoouN63",
            "27.0",
            "688.0",
            "30002.0",
            "1177.42",
            "36500",
            "31"));
    validateRow(
        response,
        List.of(
            "dwEq7wi6nXV",
            "202112",
            "J40PpdN4Wkk",
            "108.52",
            "4826.0",
            "4447.0",
            "100.0",
            "100",
            "1"));
    validateRow(
        response,
        List.of(
            "dwEq7wi6nXV",
            "202112",
            "b0EsAxm8Nge",
            "127.49",
            "6595.0",
            "5173.0",
            "100.0",
            "100",
            "1"));
    validateRow(
        response,
        List.of(
            "dwEq7wi6nXV",
            "202112",
            "jqBqIXoXpfy",
            "125.79",
            "4146.0",
            "3296.0",
            "100.0",
            "100",
            "1"));
    validateRow(
        response,
        List.of(
            "dwEq7wi6nXV",
            "202112",
            "nlX2VoouN63",
            "118.91",
            "1377.0",
            "1158.0",
            "100.0",
            "100",
            "1"));
    validateRow(
        response,
        List.of(
            "c8fABiNpT0B",
            "202112",
            "J40PpdN4Wkk",
            "76.81",
            "3154.0",
            "4106.0",
            "100.0",
            "100",
            "1"));
    validateRow(
        response,
        List.of(
            "c8fABiNpT0B",
            "202112",
            "b0EsAxm8Nge",
            "150.76",
            "6118.0",
            "4058.0",
            "100.0",
            "100",
            "1"));
    validateRow(
        response,
        List.of(
            "c8fABiNpT0B",
            "202112",
            "jqBqIXoXpfy",
            "114.0",
            "3591.0",
            "3150.0",
            "100.0",
            "100",
            "1"));
    validateRow(
        response,
        List.of(
            "c8fABiNpT0B",
            "202112",
            "nlX2VoouN63",
            "98.99",
            "1175.0",
            "1187.0",
            "100.0",
            "100",
            "1"));
  }
}
