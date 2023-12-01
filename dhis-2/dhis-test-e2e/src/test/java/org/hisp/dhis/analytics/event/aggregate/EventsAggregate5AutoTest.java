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
public class EventsAggregate5AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void queryModeOfDischargeByFacilityTypeLast12Monthsagg() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS,Zj7UnCAulEk.fWIAEtYVEGk,J5jldMd8OHv:uYxK4wmcPqA;tDZVQ1WtwpA;EYbopBOJWsW;RXL3lPSK8oG;CXw2yu5fodb")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(120)))
        .body("height", equalTo(120))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"J5jldMd8OHv\":{\"name\":\"Facility Type\"},\"202208\":{\"name\":\"August 2022\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"uYxK4wmcPqA\":{\"name\":\"CHP\"},\"gj2fKKyp8OH\":{\"code\":\"MODDIED\",\"name\":\"Died\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"202211\":{\"name\":\"November 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202212\":{\"name\":\"December 2022\"},\"tDZVQ1WtwpA\":{\"name\":\"Hospital\"},\"202210\":{\"name\":\"October 2022\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"RXL3lPSK8oG\":{\"name\":\"Clinic\"},\"ou\":{\"name\":\"Organisation unit\"},\"CXw2yu5fodb\":{\"name\":\"CHC\"},\"Fhbf4aKpZmZ\":{\"code\":\"MODABSC\",\"name\":\"Absconded\"},\"fWIAEtYVEGk\":{\"name\":\"Mode of Discharge\"},\"pe\":{\"name\":\"Period\"},\"EYbopBOJWsW\":{\"name\":\"MCHP\"},\"fShHdgT7XGb\":{\"code\":\"MODTRANS\",\"name\":\"Transferred\"},\"yeod5tOXpkP\":{\"code\":\"MODDISCH\",\"name\":\"Discharged\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"ou\":[\"ImspTQPwCqd\"],\"J5jldMd8OHv\":[\"uYxK4wmcPqA\",\"tDZVQ1WtwpA\",\"EYbopBOJWsW\",\"RXL3lPSK8oG\",\"CXw2yu5fodb\"],\"fWIAEtYVEGk\":[\"yeod5tOXpkP\",\"gj2fKKyp8OH\",\"fShHdgT7XGb\",\"Fhbf4aKpZmZ\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "fWIAEtYVEGk", "Mode of Discharge", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 3, "J5jldMd8OHv", "Facility Type", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202207", "EYbopBOJWsW", "627"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202208", "EYbopBOJWsW", "618"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202210", "EYbopBOJWsW", "607"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202209", "EYbopBOJWsW", "572"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202211", "EYbopBOJWsW", "558"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202212", "EYbopBOJWsW", "492"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202207", "uYxK4wmcPqA", "233"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202210", "uYxK4wmcPqA", "224"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202209", "uYxK4wmcPqA", "217"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202212", "uYxK4wmcPqA", "214"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202211", "uYxK4wmcPqA", "214"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202208", "CXw2yu5fodb", "213"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202207", "CXw2yu5fodb", "204"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202210", "CXw2yu5fodb", "200"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202209", "CXw2yu5fodb", "193"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202208", "uYxK4wmcPqA", "189"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202212", "CXw2yu5fodb", "160"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202211", "CXw2yu5fodb", "156"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202212", "RXL3lPSK8oG", "54"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202210", "RXL3lPSK8oG", "52"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202207", "RXL3lPSK8oG", "51"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202208", "RXL3lPSK8oG", "50"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202209", "tDZVQ1WtwpA", "40"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202211", "RXL3lPSK8oG", "38"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202209", "RXL3lPSK8oG", "37"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202208", "tDZVQ1WtwpA", "35"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202210", "tDZVQ1WtwpA", "31"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202211", "tDZVQ1WtwpA", "31"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202207", "tDZVQ1WtwpA", "29"));
    validateRow(response, List.of("MODTRANS", "ImspTQPwCqd", "202212", "tDZVQ1WtwpA", "27"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202211", "EYbopBOJWsW", "634"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202207", "EYbopBOJWsW", "611"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202210", "EYbopBOJWsW", "581"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202209", "EYbopBOJWsW", "574"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202212", "EYbopBOJWsW", "548"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202208", "EYbopBOJWsW", "544"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202207", "uYxK4wmcPqA", "233"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202208", "uYxK4wmcPqA", "229"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202210", "uYxK4wmcPqA", "226"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202212", "uYxK4wmcPqA", "226"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202209", "uYxK4wmcPqA", "217"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202210", "CXw2yu5fodb", "202"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202208", "CXw2yu5fodb", "201"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202211", "uYxK4wmcPqA", "195"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202211", "CXw2yu5fodb", "193"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202209", "CXw2yu5fodb", "187"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202207", "CXw2yu5fodb", "178"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202212", "CXw2yu5fodb", "167"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202208", "RXL3lPSK8oG", "64"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202207", "RXL3lPSK8oG", "64"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202211", "RXL3lPSK8oG", "53"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202210", "RXL3lPSK8oG", "48"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202209", "RXL3lPSK8oG", "45"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202210", "tDZVQ1WtwpA", "44"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202212", "RXL3lPSK8oG", "44"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202211", "tDZVQ1WtwpA", "43"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202209", "tDZVQ1WtwpA", "38"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202208", "tDZVQ1WtwpA", "30"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202212", "tDZVQ1WtwpA", "29"));
    validateRow(response, List.of("MODDISCH", "ImspTQPwCqd", "202207", "tDZVQ1WtwpA", "26"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202207", "EYbopBOJWsW", "618"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202209", "EYbopBOJWsW", "602"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202212", "EYbopBOJWsW", "599"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202211", "EYbopBOJWsW", "589"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202210", "EYbopBOJWsW", "579"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202208", "EYbopBOJWsW", "569"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202211", "uYxK4wmcPqA", "238"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202208", "CXw2yu5fodb", "212"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202210", "uYxK4wmcPqA", "212"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202207", "uYxK4wmcPqA", "207"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202209", "uYxK4wmcPqA", "199"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202210", "CXw2yu5fodb", "196"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202208", "uYxK4wmcPqA", "192"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202207", "CXw2yu5fodb", "182"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202209", "CXw2yu5fodb", "176"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202212", "CXw2yu5fodb", "176"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202212", "uYxK4wmcPqA", "170"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202211", "CXw2yu5fodb", "167"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202211", "RXL3lPSK8oG", "54"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202207", "RXL3lPSK8oG", "54"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202207", "tDZVQ1WtwpA", "48"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202212", "RXL3lPSK8oG", "48"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202208", "RXL3lPSK8oG", "45"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202209", "tDZVQ1WtwpA", "44"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202210", "RXL3lPSK8oG", "42"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202209", "RXL3lPSK8oG", "42"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202212", "tDZVQ1WtwpA", "37"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202208", "tDZVQ1WtwpA", "34"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202211", "tDZVQ1WtwpA", "33"));
    validateRow(response, List.of("MODDIED", "ImspTQPwCqd", "202210", "tDZVQ1WtwpA", "30"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202211", "EYbopBOJWsW", "615"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202208", "EYbopBOJWsW", "609"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202210", "EYbopBOJWsW", "606"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202212", "EYbopBOJWsW", "592"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202209", "EYbopBOJWsW", "569"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202207", "EYbopBOJWsW", "559"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202209", "uYxK4wmcPqA", "219"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202212", "uYxK4wmcPqA", "217"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202208", "uYxK4wmcPqA", "214"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202211", "uYxK4wmcPqA", "212"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202210", "uYxK4wmcPqA", "205"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202207", "uYxK4wmcPqA", "204"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202210", "CXw2yu5fodb", "199"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202209", "CXw2yu5fodb", "199"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202212", "CXw2yu5fodb", "194"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202211", "CXw2yu5fodb", "187"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202208", "CXw2yu5fodb", "187"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202207", "CXw2yu5fodb", "182"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202208", "RXL3lPSK8oG", "54"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202209", "RXL3lPSK8oG", "50"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202212", "RXL3lPSK8oG", "50"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202207", "RXL3lPSK8oG", "46"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202211", "RXL3lPSK8oG", "45"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202208", "tDZVQ1WtwpA", "45"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202210", "tDZVQ1WtwpA", "43"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202210", "RXL3lPSK8oG", "38"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202209", "tDZVQ1WtwpA", "37"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202207", "tDZVQ1WtwpA", "30"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202212", "tDZVQ1WtwpA", "26"));
    validateRow(response, List.of("MODABSC", "ImspTQPwCqd", "202211", "tDZVQ1WtwpA", "26"));
  }

  @Test
  public void queryModeOfDischargeLast12Months() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:ImspTQPwCqd")
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=Zj7UnCAulEk.fWIAEtYVEGk,pe:LAST_12_MONTHS")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(24)))
        .body("height", equalTo(24))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ou\":{\"name\":\"Organisation unit\"},\"Fhbf4aKpZmZ\":{\"code\":\"MODABSC\",\"name\":\"Absconded\"},\"fWIAEtYVEGk\":{\"name\":\"Mode of Discharge\"},\"202208\":{\"name\":\"August 2022\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"gj2fKKyp8OH\":{\"code\":\"MODDIED\",\"name\":\"Died\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202210\":{\"name\":\"October 2022\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"pe\":{\"name\":\"Period\"},\"fShHdgT7XGb\":{\"code\":\"MODTRANS\",\"name\":\"Transferred\"},\"yeod5tOXpkP\":{\"code\":\"MODDISCH\",\"name\":\"Discharged\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"ou\":[\"ImspTQPwCqd\"],\"fWIAEtYVEGk\":[\"yeod5tOXpkP\",\"gj2fKKyp8OH\",\"fShHdgT7XGb\",\"Fhbf4aKpZmZ\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "fWIAEtYVEGk", "Mode of Discharge", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("MODTRANS", "202207", "1215"));
    validateRow(response, List.of("MODTRANS", "202210", "1186"));
    validateRow(response, List.of("MODTRANS", "202208", "1166"));
    validateRow(response, List.of("MODTRANS", "202209", "1145"));
    validateRow(response, List.of("MODTRANS", "202211", "1076"));
    validateRow(response, List.of("MODTRANS", "202212", "1003"));
    validateRow(response, List.of("MODDISCH", "202207", "1190"));
    validateRow(response, List.of("MODDISCH", "202210", "1182"));
    validateRow(response, List.of("MODDISCH", "202211", "1181"));
    validateRow(response, List.of("MODDISCH", "202208", "1148"));
    validateRow(response, List.of("MODDISCH", "202209", "1120"));
    validateRow(response, List.of("MODDISCH", "202212", "1092"));
    validateRow(response, List.of("MODDIED", "202207", "1188"));
    validateRow(response, List.of("MODDIED", "202211", "1157"));
    validateRow(response, List.of("MODDIED", "202210", "1128"));
    validateRow(response, List.of("MODDIED", "202209", "1121"));
    validateRow(response, List.of("MODDIED", "202208", "1120"));
    validateRow(response, List.of("MODDIED", "202212", "1093"));
    validateRow(response, List.of("MODABSC", "202208", "1180"));
    validateRow(response, List.of("MODABSC", "202209", "1153"));
    validateRow(response, List.of("MODABSC", "202210", "1153"));
    validateRow(response, List.of("MODABSC", "202211", "1150"));
    validateRow(response, List.of("MODABSC", "202212", "1148"));
    validateRow(response, List.of("MODABSC", "202207", "1107"));
  }

  @Test
  public void queryModeOfDischargeLast12MonthsWide() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=Zj7UnCAulEk.fWIAEtYVEGk")
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=pe:LAST_12_MONTHS,ou:jUb8gELQApl;TEQlaapDQoK;eIQbndfxQMb;Vth0fbpFcsO;PMa2VCrupOd;O6uvpzGd5pu;bL4ooGhyHRQ;kJq2mPyFEHo;fdc6uOvgoji;at6UHUQatSo;lc3eMKXaEfw;qhqAxPSTUXp;jmIPBj66vD6")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(78)))
        .body("height", equalTo(78))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"202208\":{\"name\":\"August 2022\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"202209\":{\"name\":\"September 2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202305\":{\"name\":\"May 2023\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"gj2fKKyp8OH\":{\"code\":\"MODDIED\",\"name\":\"Died\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"202210\":{\"name\":\"October 2022\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"Fhbf4aKpZmZ\":{\"code\":\"MODABSC\",\"name\":\"Absconded\"},\"fWIAEtYVEGk\":{\"name\":\"Mode of Discharge\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"pe\":{\"name\":\"Period\"},\"fShHdgT7XGb\":{\"code\":\"MODTRANS\",\"name\":\"Transferred\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"yeod5tOXpkP\":{\"code\":\"MODDISCH\",\"name\":\"Discharged\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"ou\":[\"jUb8gELQApl\",\"TEQlaapDQoK\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"PMa2VCrupOd\",\"O6uvpzGd5pu\",\"bL4ooGhyHRQ\",\"kJq2mPyFEHo\",\"fdc6uOvgoji\",\"at6UHUQatSo\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"jmIPBj66vD6\"],\"fWIAEtYVEGk\":[\"yeod5tOXpkP\",\"gj2fKKyp8OH\",\"fShHdgT7XGb\",\"Fhbf4aKpZmZ\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("qhqAxPSTUXp", "202209", "289"));
    validateRow(response, List.of("qhqAxPSTUXp", "202207", "280"));
    validateRow(response, List.of("qhqAxPSTUXp", "202212", "271"));
    validateRow(response, List.of("qhqAxPSTUXp", "202208", "269"));
    validateRow(response, List.of("qhqAxPSTUXp", "202211", "268"));
    validateRow(response, List.of("qhqAxPSTUXp", "202210", "261"));
    validateRow(response, List.of("lc3eMKXaEfw", "202207", "215"));
    validateRow(response, List.of("lc3eMKXaEfw", "202211", "211"));
    validateRow(response, List.of("lc3eMKXaEfw", "202209", "210"));
    validateRow(response, List.of("lc3eMKXaEfw", "202208", "209"));
    validateRow(response, List.of("lc3eMKXaEfw", "202210", "206"));
    validateRow(response, List.of("lc3eMKXaEfw", "202212", "191"));
    validateRow(response, List.of("kJq2mPyFEHo", "202210", "521"));
    validateRow(response, List.of("kJq2mPyFEHo", "202208", "496"));
    validateRow(response, List.of("kJq2mPyFEHo", "202212", "485"));
    validateRow(response, List.of("kJq2mPyFEHo", "202211", "482"));
    validateRow(response, List.of("kJq2mPyFEHo", "202207", "468"));
    validateRow(response, List.of("kJq2mPyFEHo", "202209", "460"));
    validateRow(response, List.of("jmIPBj66vD6", "202207", "405"));
    validateRow(response, List.of("jmIPBj66vD6", "202211", "400"));
    validateRow(response, List.of("jmIPBj66vD6", "202209", "374"));
    validateRow(response, List.of("jmIPBj66vD6", "202210", "373"));
    validateRow(response, List.of("jmIPBj66vD6", "202212", "357"));
    validateRow(response, List.of("jmIPBj66vD6", "202208", "348"));
    validateRow(response, List.of("jUb8gELQApl", "202208", "308"));
    validateRow(response, List.of("jUb8gELQApl", "202210", "284"));
    validateRow(response, List.of("jUb8gELQApl", "202212", "282"));
    validateRow(response, List.of("jUb8gELQApl", "202207", "282"));
    validateRow(response, List.of("jUb8gELQApl", "202211", "280"));
    validateRow(response, List.of("jUb8gELQApl", "202209", "276"));
    validateRow(response, List.of("fdc6uOvgoji", "202210", "454"));
    validateRow(response, List.of("fdc6uOvgoji", "202207", "400"));
    validateRow(response, List.of("fdc6uOvgoji", "202211", "382"));
    validateRow(response, List.of("fdc6uOvgoji", "202208", "373"));
    validateRow(response, List.of("fdc6uOvgoji", "202212", "367"));
    validateRow(response, List.of("fdc6uOvgoji", "202209", "356"));
    validateRow(response, List.of("eIQbndfxQMb", "202210", "397"));
    validateRow(response, List.of("eIQbndfxQMb", "202207", "387"));
    validateRow(response, List.of("eIQbndfxQMb", "202211", "379"));
    validateRow(response, List.of("eIQbndfxQMb", "202209", "375"));
    validateRow(response, List.of("eIQbndfxQMb", "202212", "334"));
    validateRow(response, List.of("eIQbndfxQMb", "202208", "331"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202208", "287"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202210", "276"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202211", "272"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202207", "264"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202209", "249"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202212", "247"));
    validateRow(response, List.of("at6UHUQatSo", "202211", "435"));
    validateRow(response, List.of("at6UHUQatSo", "202209", "428"));
    validateRow(response, List.of("at6UHUQatSo", "202208", "423"));
    validateRow(response, List.of("at6UHUQatSo", "202207", "413"));
    validateRow(response, List.of("at6UHUQatSo", "202210", "387"));
    validateRow(response, List.of("at6UHUQatSo", "202212", "364"));
    validateRow(response, List.of("Vth0fbpFcsO", "202208", "364"));
    validateRow(response, List.of("Vth0fbpFcsO", "202210", "349"));
    validateRow(response, List.of("Vth0fbpFcsO", "202207", "342"));
    validateRow(response, List.of("Vth0fbpFcsO", "202212", "329"));
    validateRow(response, List.of("Vth0fbpFcsO", "202211", "318"));
    validateRow(response, List.of("Vth0fbpFcsO", "202209", "313"));
    validateRow(response, List.of("TEQlaapDQoK", "202209", "458"));
    validateRow(response, List.of("TEQlaapDQoK", "202207", "455"));
    validateRow(response, List.of("TEQlaapDQoK", "202208", "442"));
    validateRow(response, List.of("TEQlaapDQoK", "202211", "431"));
    validateRow(response, List.of("TEQlaapDQoK", "202210", "413"));
    validateRow(response, List.of("TEQlaapDQoK", "202212", "399"));
    validateRow(response, List.of("PMa2VCrupOd", "202207", "274"));
    validateRow(response, List.of("PMa2VCrupOd", "202208", "255"));
    validateRow(response, List.of("PMa2VCrupOd", "202210", "248"));
    validateRow(response, List.of("PMa2VCrupOd", "202209", "240"));
    validateRow(response, List.of("PMa2VCrupOd", "202212", "235"));
    validateRow(response, List.of("PMa2VCrupOd", "202211", "234"));
    validateRow(response, List.of("O6uvpzGd5pu", "202207", "515"));
    validateRow(response, List.of("O6uvpzGd5pu", "202209", "511"));
    validateRow(response, List.of("O6uvpzGd5pu", "202208", "509"));
    validateRow(response, List.of("O6uvpzGd5pu", "202210", "480"));
    validateRow(response, List.of("O6uvpzGd5pu", "202212", "475"));
    validateRow(response, List.of("O6uvpzGd5pu", "202211", "472"));
  }

  @Test
  public void queryWeightAndHeightHideNaLast12Months() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=Zj7UnCAulEk")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS,Zj7UnCAulEk.GieVkTxp4HH-TBxGTceyzwy,Zj7UnCAulEk.vV9UWAZohSf-OrkEzxZEH4X")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(254)))
        .body("height", equalTo(254))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"lnccUWrmqL0\":{\"name\":\"80 - 90\"},\"eySqrYxteI7\":{\"name\":\"200+\"},\"BHlWGFLIU20\":{\"name\":\"120 - 140\"},\"202208\":{\"name\":\"August 2022\"},\"GWuQsWJDGvN\":{\"name\":\"140 - 160\"},\"GDFw7T4aFGz\":{\"name\":\"60 - 70\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"NxQrJ3icPkE\":{\"name\":\"0 - 20\"},\"b9UzeWaSs2u\":{\"name\":\"20 - 40\"},\"xVezsaEXU3k\":{\"name\":\"70 - 80\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"Zj7UnCAulEk\":{\"name\":\"Inpatient morbidity and mortality\"},\"202211\":{\"name\":\"November 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202212\":{\"name\":\"December 2022\"},\"CivTksSoCt0\":{\"name\":\"100 - 120\"},\"202210\":{\"name\":\"October 2022\"},\"eBAyeGv0exc\":{\"name\":\"Inpatient morbidity and mortality\"},\"AD5jueZTZSK\":{\"name\":\"40 - 50\"},\"f3prvzpfniC\":{\"name\":\"100+\"},\"sxFVvKLpE0y\":{\"name\":\"0 - 100\"},\"B1X4JyH4Mdw\":{\"name\":\"180 - 200\"},\"ou\":{\"name\":\"Organisation unit\"},\"vV9UWAZohSf\":{\"name\":\"Weight in kg\"},\"Sjp6IB3gthI\":{\"name\":\"50 - 60\"},\"GieVkTxp4HH\":{\"name\":\"Height in cm\"},\"pe\":{\"name\":\"Period\"},\"wgbW2ZQnlIc\":{\"name\":\"160 - 180\"},\"XKEvGfAkh3R\":{\"name\":\"90 - 100\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"Zj7UnCAulEk.vV9UWAZohSf\":[\"NxQrJ3icPkE\",\"b9UzeWaSs2u\",\"AD5jueZTZSK\",\"Sjp6IB3gthI\",\"GDFw7T4aFGz\",\"xVezsaEXU3k\",\"lnccUWrmqL0\",\"XKEvGfAkh3R\",\"f3prvzpfniC\"],\"ou\":[\"ImspTQPwCqd\"],\"Zj7UnCAulEk.GieVkTxp4HH\":[\"sxFVvKLpE0y\",\"CivTksSoCt0\",\"BHlWGFLIU20\",\"GWuQsWJDGvN\",\"wgbW2ZQnlIc\",\"B1X4JyH4Mdw\",\"eySqrYxteI7\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "vV9UWAZohSf", "Weight in kg", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "GieVkTxp4HH", "Height in cm", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("xVezsaEXU3k", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "276"));
    validateRow(response, List.of("xVezsaEXU3k", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "275"));
    validateRow(response, List.of("xVezsaEXU3k", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "266"));
    validateRow(response, List.of("xVezsaEXU3k", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "250"));
    validateRow(response, List.of("xVezsaEXU3k", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "244"));
    validateRow(response, List.of("xVezsaEXU3k", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "237"));
    validateRow(response, List.of("xVezsaEXU3k", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "109"));
    validateRow(response, List.of("xVezsaEXU3k", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "103"));
    validateRow(response, List.of("xVezsaEXU3k", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "100"));
    validateRow(response, List.of("xVezsaEXU3k", "CivTksSoCt0", "ImspTQPwCqd", "202208", "97"));
    validateRow(response, List.of("xVezsaEXU3k", "CivTksSoCt0", "ImspTQPwCqd", "202207", "97"));
    validateRow(response, List.of("xVezsaEXU3k", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "96"));
    validateRow(response, List.of("xVezsaEXU3k", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "94"));
    validateRow(response, List.of("xVezsaEXU3k", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "91"));
    validateRow(response, List.of("xVezsaEXU3k", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "90"));
    validateRow(response, List.of("xVezsaEXU3k", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "90"));
    validateRow(response, List.of("xVezsaEXU3k", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "90"));
    validateRow(response, List.of("xVezsaEXU3k", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "90"));
    validateRow(response, List.of("xVezsaEXU3k", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "86"));
    validateRow(response, List.of("xVezsaEXU3k", "CivTksSoCt0", "ImspTQPwCqd", "202210", "86"));
    validateRow(response, List.of("xVezsaEXU3k", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "83"));
    validateRow(response, List.of("xVezsaEXU3k", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "83"));
    validateRow(response, List.of("xVezsaEXU3k", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "82"));
    validateRow(response, List.of("xVezsaEXU3k", "CivTksSoCt0", "ImspTQPwCqd", "202209", "81"));
    validateRow(response, List.of("xVezsaEXU3k", "CivTksSoCt0", "ImspTQPwCqd", "202212", "79"));
    validateRow(response, List.of("xVezsaEXU3k", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "78"));
    validateRow(response, List.of("xVezsaEXU3k", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "78"));
    validateRow(response, List.of("xVezsaEXU3k", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "74"));
    validateRow(response, List.of("xVezsaEXU3k", "CivTksSoCt0", "ImspTQPwCqd", "202211", "73"));
    validateRow(response, List.of("xVezsaEXU3k", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "69"));
    validateRow(response, List.of("xVezsaEXU3k", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "57"));
    validateRow(response, List.of("xVezsaEXU3k", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "54"));
    validateRow(response, List.of("xVezsaEXU3k", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "52"));
    validateRow(response, List.of("xVezsaEXU3k", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "42"));
    validateRow(response, List.of("xVezsaEXU3k", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "38"));
    validateRow(response, List.of("xVezsaEXU3k", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "32"));
    validateRow(response, List.of("lnccUWrmqL0", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "146"));
    validateRow(response, List.of("lnccUWrmqL0", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "128"));
    validateRow(response, List.of("lnccUWrmqL0", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "127"));
    validateRow(response, List.of("lnccUWrmqL0", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "121"));
    validateRow(response, List.of("lnccUWrmqL0", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "114"));
    validateRow(response, List.of("lnccUWrmqL0", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "107"));
    validateRow(response, List.of("lnccUWrmqL0", "CivTksSoCt0", "ImspTQPwCqd", "202209", "55"));
    validateRow(response, List.of("lnccUWrmqL0", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "54"));
    validateRow(response, List.of("lnccUWrmqL0", "CivTksSoCt0", "ImspTQPwCqd", "202211", "52"));
    validateRow(response, List.of("lnccUWrmqL0", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "51"));
    validateRow(response, List.of("lnccUWrmqL0", "CivTksSoCt0", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, List.of("lnccUWrmqL0", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "49"));
    validateRow(response, List.of("lnccUWrmqL0", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "48"));
    validateRow(response, List.of("lnccUWrmqL0", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "47"));
    validateRow(response, List.of("lnccUWrmqL0", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "47"));
    validateRow(response, List.of("lnccUWrmqL0", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "44"));
    validateRow(response, List.of("lnccUWrmqL0", "CivTksSoCt0", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, List.of("lnccUWrmqL0", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "43"));
    validateRow(response, List.of("lnccUWrmqL0", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "43"));
    validateRow(response, List.of("lnccUWrmqL0", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "41"));
    validateRow(response, List.of("lnccUWrmqL0", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "40"));
    validateRow(response, List.of("lnccUWrmqL0", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "39"));
    validateRow(response, List.of("lnccUWrmqL0", "CivTksSoCt0", "ImspTQPwCqd", "202210", "39"));
    validateRow(response, List.of("lnccUWrmqL0", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "39"));
    validateRow(response, List.of("lnccUWrmqL0", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "38"));
    validateRow(response, List.of("lnccUWrmqL0", "CivTksSoCt0", "ImspTQPwCqd", "202208", "38"));
    validateRow(response, List.of("lnccUWrmqL0", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "37"));
    validateRow(response, List.of("lnccUWrmqL0", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "36"));
    validateRow(response, List.of("lnccUWrmqL0", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "36"));
    validateRow(response, List.of("lnccUWrmqL0", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "36"));
    validateRow(response, List.of("lnccUWrmqL0", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "32"));
    validateRow(response, List.of("lnccUWrmqL0", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "22"));
    validateRow(response, List.of("lnccUWrmqL0", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "20"));
    validateRow(response, List.of("lnccUWrmqL0", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "20"));
    validateRow(response, List.of("lnccUWrmqL0", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "19"));
    validateRow(response, List.of("lnccUWrmqL0", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "18"));
    validateRow(response, List.of("f3prvzpfniC", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "1"));
    validateRow(response, List.of("b9UzeWaSs2u", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "559"));
    validateRow(response, List.of("b9UzeWaSs2u", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "553"));
    validateRow(response, List.of("b9UzeWaSs2u", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "538"));
    validateRow(response, List.of("b9UzeWaSs2u", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "506"));
    validateRow(response, List.of("b9UzeWaSs2u", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "501"));
    validateRow(response, List.of("b9UzeWaSs2u", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "501"));
    validateRow(response, List.of("b9UzeWaSs2u", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "204"));
    validateRow(response, List.of("b9UzeWaSs2u", "CivTksSoCt0", "ImspTQPwCqd", "202209", "189"));
    validateRow(response, List.of("b9UzeWaSs2u", "CivTksSoCt0", "ImspTQPwCqd", "202210", "186"));
    validateRow(response, List.of("b9UzeWaSs2u", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "185"));
    validateRow(response, List.of("b9UzeWaSs2u", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "184"));
    validateRow(response, List.of("b9UzeWaSs2u", "CivTksSoCt0", "ImspTQPwCqd", "202211", "180"));
    validateRow(response, List.of("b9UzeWaSs2u", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "171"));
    validateRow(response, List.of("b9UzeWaSs2u", "CivTksSoCt0", "ImspTQPwCqd", "202207", "171"));
    validateRow(response, List.of("b9UzeWaSs2u", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "170"));
    validateRow(response, List.of("b9UzeWaSs2u", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "170"));
    validateRow(response, List.of("b9UzeWaSs2u", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "168"));
    validateRow(response, List.of("b9UzeWaSs2u", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "168"));
    validateRow(response, List.of("b9UzeWaSs2u", "CivTksSoCt0", "ImspTQPwCqd", "202208", "168"));
    validateRow(response, List.of("b9UzeWaSs2u", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "167"));
    validateRow(response, List.of("b9UzeWaSs2u", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "165"));
    validateRow(response, List.of("b9UzeWaSs2u", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "164"));
    validateRow(response, List.of("b9UzeWaSs2u", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "164"));
    validateRow(response, List.of("b9UzeWaSs2u", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "163"));
    validateRow(response, List.of("b9UzeWaSs2u", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "160"));
    validateRow(response, List.of("b9UzeWaSs2u", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "156"));
    validateRow(response, List.of("b9UzeWaSs2u", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "156"));
    validateRow(response, List.of("b9UzeWaSs2u", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "155"));
    validateRow(response, List.of("b9UzeWaSs2u", "CivTksSoCt0", "ImspTQPwCqd", "202212", "155"));
    validateRow(response, List.of("b9UzeWaSs2u", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "154"));
    validateRow(response, List.of("b9UzeWaSs2u", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "98"));
    validateRow(response, List.of("b9UzeWaSs2u", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "95"));
    validateRow(response, List.of("b9UzeWaSs2u", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "89"));
    validateRow(response, List.of("b9UzeWaSs2u", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "86"));
    validateRow(response, List.of("b9UzeWaSs2u", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "80"));
    validateRow(response, List.of("b9UzeWaSs2u", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "77"));
    validateRow(response, List.of("Sjp6IB3gthI", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "280"));
    validateRow(response, List.of("Sjp6IB3gthI", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "268"));
    validateRow(response, List.of("Sjp6IB3gthI", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "258"));
    validateRow(response, List.of("Sjp6IB3gthI", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "257"));
    validateRow(response, List.of("Sjp6IB3gthI", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "243"));
    validateRow(response, List.of("Sjp6IB3gthI", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "238"));
    validateRow(response, List.of("Sjp6IB3gthI", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "102"));
    validateRow(response, List.of("Sjp6IB3gthI", "CivTksSoCt0", "ImspTQPwCqd", "202207", "100"));
    validateRow(response, List.of("Sjp6IB3gthI", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "99"));
    validateRow(response, List.of("Sjp6IB3gthI", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "98"));
    validateRow(response, List.of("Sjp6IB3gthI", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "97"));
    validateRow(response, List.of("Sjp6IB3gthI", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "94"));
    validateRow(response, List.of("Sjp6IB3gthI", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "93"));
    validateRow(response, List.of("Sjp6IB3gthI", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "91"));
    validateRow(response, List.of("Sjp6IB3gthI", "CivTksSoCt0", "ImspTQPwCqd", "202209", "90"));
    validateRow(response, List.of("Sjp6IB3gthI", "CivTksSoCt0", "ImspTQPwCqd", "202208", "89"));
    validateRow(response, List.of("Sjp6IB3gthI", "CivTksSoCt0", "ImspTQPwCqd", "202210", "89"));
    validateRow(response, List.of("Sjp6IB3gthI", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "88"));
    validateRow(response, List.of("Sjp6IB3gthI", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "87"));
    validateRow(response, List.of("Sjp6IB3gthI", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "85"));
    validateRow(response, List.of("Sjp6IB3gthI", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "85"));
    validateRow(response, List.of("Sjp6IB3gthI", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "82"));
    validateRow(response, List.of("Sjp6IB3gthI", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "82"));
    validateRow(response, List.of("Sjp6IB3gthI", "CivTksSoCt0", "ImspTQPwCqd", "202212", "81"));
    validateRow(response, List.of("Sjp6IB3gthI", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "81"));
    validateRow(response, List.of("Sjp6IB3gthI", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "80"));
    validateRow(response, List.of("Sjp6IB3gthI", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "79"));
    validateRow(response, List.of("Sjp6IB3gthI", "CivTksSoCt0", "ImspTQPwCqd", "202211", "77"));
    validateRow(response, List.of("Sjp6IB3gthI", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "74"));
    validateRow(response, List.of("Sjp6IB3gthI", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "71"));
    validateRow(response, List.of("Sjp6IB3gthI", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, List.of("Sjp6IB3gthI", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "44"));
    validateRow(response, List.of("Sjp6IB3gthI", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "42"));
    validateRow(response, List.of("Sjp6IB3gthI", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "42"));
    validateRow(response, List.of("Sjp6IB3gthI", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "42"));
    validateRow(response, List.of("Sjp6IB3gthI", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "41"));
    validateRow(response, List.of("NxQrJ3icPkE", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "149"));
    validateRow(response, List.of("NxQrJ3icPkE", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "143"));
    validateRow(response, List.of("NxQrJ3icPkE", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "142"));
    validateRow(response, List.of("NxQrJ3icPkE", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "142"));
    validateRow(response, List.of("NxQrJ3icPkE", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "131"));
    validateRow(response, List.of("NxQrJ3icPkE", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "129"));
    validateRow(response, List.of("NxQrJ3icPkE", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "57"));
    validateRow(response, List.of("NxQrJ3icPkE", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "56"));
    validateRow(response, List.of("NxQrJ3icPkE", "CivTksSoCt0", "ImspTQPwCqd", "202210", "52"));
    validateRow(response, List.of("NxQrJ3icPkE", "CivTksSoCt0", "ImspTQPwCqd", "202208", "52"));
    validateRow(response, List.of("NxQrJ3icPkE", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "51"));
    validateRow(response, List.of("NxQrJ3icPkE", "CivTksSoCt0", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, List.of("NxQrJ3icPkE", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "48"));
    validateRow(response, List.of("NxQrJ3icPkE", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "48"));
    validateRow(response, List.of("NxQrJ3icPkE", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "47"));
    validateRow(response, List.of("NxQrJ3icPkE", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "46"));
    validateRow(response, List.of("NxQrJ3icPkE", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "45"));
    validateRow(response, List.of("NxQrJ3icPkE", "CivTksSoCt0", "ImspTQPwCqd", "202207", "45"));
    validateRow(response, List.of("NxQrJ3icPkE", "CivTksSoCt0", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, List.of("NxQrJ3icPkE", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "44"));
    validateRow(response, List.of("NxQrJ3icPkE", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "43"));
    validateRow(response, List.of("NxQrJ3icPkE", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "43"));
    validateRow(response, List.of("NxQrJ3icPkE", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "42"));
    validateRow(response, List.of("NxQrJ3icPkE", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "42"));
    validateRow(response, List.of("NxQrJ3icPkE", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "41"));
    validateRow(response, List.of("NxQrJ3icPkE", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "40"));
    validateRow(response, List.of("NxQrJ3icPkE", "CivTksSoCt0", "ImspTQPwCqd", "202209", "40"));
    validateRow(response, List.of("NxQrJ3icPkE", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "38"));
    validateRow(response, List.of("NxQrJ3icPkE", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "36"));
    validateRow(response, List.of("NxQrJ3icPkE", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "28"));
    validateRow(response, List.of("NxQrJ3icPkE", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "27"));
    validateRow(response, List.of("NxQrJ3icPkE", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "25"));
    validateRow(response, List.of("NxQrJ3icPkE", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "21"));
    validateRow(response, List.of("NxQrJ3icPkE", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "19"));
    validateRow(response, List.of("NxQrJ3icPkE", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "18"));
    validateRow(response, List.of("NxQrJ3icPkE", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "16"));
    validateRow(response, List.of("GDFw7T4aFGz", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "283"));
    validateRow(response, List.of("GDFw7T4aFGz", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "279"));
    validateRow(response, List.of("GDFw7T4aFGz", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "268"));
    validateRow(response, List.of("GDFw7T4aFGz", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "263"));
    validateRow(response, List.of("GDFw7T4aFGz", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "255"));
    validateRow(response, List.of("GDFw7T4aFGz", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "252"));
    validateRow(response, List.of("GDFw7T4aFGz", "CivTksSoCt0", "ImspTQPwCqd", "202210", "106"));
    validateRow(response, List.of("GDFw7T4aFGz", "CivTksSoCt0", "ImspTQPwCqd", "202207", "102"));
    validateRow(response, List.of("GDFw7T4aFGz", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "100"));
    validateRow(response, List.of("GDFw7T4aFGz", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "96"));
    validateRow(response, List.of("GDFw7T4aFGz", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "96"));
    validateRow(response, List.of("GDFw7T4aFGz", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "95"));
    validateRow(response, List.of("GDFw7T4aFGz", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "95"));
    validateRow(response, List.of("GDFw7T4aFGz", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "95"));
    validateRow(response, List.of("GDFw7T4aFGz", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "94"));
    validateRow(response, List.of("GDFw7T4aFGz", "CivTksSoCt0", "ImspTQPwCqd", "202211", "92"));
    validateRow(response, List.of("GDFw7T4aFGz", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "90"));
    validateRow(response, List.of("GDFw7T4aFGz", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "88"));
    validateRow(response, List.of("GDFw7T4aFGz", "CivTksSoCt0", "ImspTQPwCqd", "202208", "87"));
    validateRow(response, List.of("GDFw7T4aFGz", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "87"));
    validateRow(response, List.of("GDFw7T4aFGz", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "85"));
    validateRow(response, List.of("GDFw7T4aFGz", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "84"));
    validateRow(response, List.of("GDFw7T4aFGz", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "83"));
    validateRow(response, List.of("GDFw7T4aFGz", "CivTksSoCt0", "ImspTQPwCqd", "202212", "82"));
    validateRow(response, List.of("GDFw7T4aFGz", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "82"));
    validateRow(response, List.of("GDFw7T4aFGz", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "82"));
    validateRow(response, List.of("GDFw7T4aFGz", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "81"));
    validateRow(response, List.of("GDFw7T4aFGz", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "75"));
    validateRow(response, List.of("GDFw7T4aFGz", "CivTksSoCt0", "ImspTQPwCqd", "202209", "72"));
    validateRow(response, List.of("GDFw7T4aFGz", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "59"));
    validateRow(response, List.of("GDFw7T4aFGz", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "56"));
    validateRow(response, List.of("GDFw7T4aFGz", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "48"));
    validateRow(response, List.of("GDFw7T4aFGz", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "46"));
    validateRow(response, List.of("GDFw7T4aFGz", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "46"));
    validateRow(response, List.of("GDFw7T4aFGz", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "42"));
    validateRow(response, List.of("GDFw7T4aFGz", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "36"));
    validateRow(response, List.of("AD5jueZTZSK", "sxFVvKLpE0y", "ImspTQPwCqd", "202207", "286"));
    validateRow(response, List.of("AD5jueZTZSK", "sxFVvKLpE0y", "ImspTQPwCqd", "202209", "272"));
    validateRow(response, List.of("AD5jueZTZSK", "sxFVvKLpE0y", "ImspTQPwCqd", "202208", "270"));
    validateRow(response, List.of("AD5jueZTZSK", "sxFVvKLpE0y", "ImspTQPwCqd", "202210", "255"));
    validateRow(response, List.of("AD5jueZTZSK", "sxFVvKLpE0y", "ImspTQPwCqd", "202211", "250"));
    validateRow(response, List.of("AD5jueZTZSK", "sxFVvKLpE0y", "ImspTQPwCqd", "202212", "232"));
    validateRow(response, List.of("AD5jueZTZSK", "CivTksSoCt0", "ImspTQPwCqd", "202209", "105"));
    validateRow(response, List.of("AD5jueZTZSK", "BHlWGFLIU20", "ImspTQPwCqd", "202210", "98"));
    validateRow(response, List.of("AD5jueZTZSK", "BHlWGFLIU20", "ImspTQPwCqd", "202209", "97"));
    validateRow(response, List.of("AD5jueZTZSK", "BHlWGFLIU20", "ImspTQPwCqd", "202207", "94"));
    validateRow(response, List.of("AD5jueZTZSK", "GWuQsWJDGvN", "ImspTQPwCqd", "202212", "93"));
    validateRow(response, List.of("AD5jueZTZSK", "CivTksSoCt0", "ImspTQPwCqd", "202207", "92"));
    validateRow(response, List.of("AD5jueZTZSK", "GWuQsWJDGvN", "ImspTQPwCqd", "202209", "91"));
    validateRow(response, List.of("AD5jueZTZSK", "wgbW2ZQnlIc", "ImspTQPwCqd", "202209", "90"));
    validateRow(response, List.of("AD5jueZTZSK", "CivTksSoCt0", "ImspTQPwCqd", "202210", "89"));
    validateRow(response, List.of("AD5jueZTZSK", "BHlWGFLIU20", "ImspTQPwCqd", "202211", "89"));
    validateRow(response, List.of("AD5jueZTZSK", "BHlWGFLIU20", "ImspTQPwCqd", "202212", "88"));
    validateRow(response, List.of("AD5jueZTZSK", "GWuQsWJDGvN", "ImspTQPwCqd", "202210", "88"));
    validateRow(response, List.of("AD5jueZTZSK", "wgbW2ZQnlIc", "ImspTQPwCqd", "202207", "86"));
    validateRow(response, List.of("AD5jueZTZSK", "GWuQsWJDGvN", "ImspTQPwCqd", "202207", "85"));
    validateRow(response, List.of("AD5jueZTZSK", "GWuQsWJDGvN", "ImspTQPwCqd", "202211", "83"));
    validateRow(response, List.of("AD5jueZTZSK", "CivTksSoCt0", "ImspTQPwCqd", "202211", "80"));
    validateRow(response, List.of("AD5jueZTZSK", "wgbW2ZQnlIc", "ImspTQPwCqd", "202208", "79"));
    validateRow(response, List.of("AD5jueZTZSK", "wgbW2ZQnlIc", "ImspTQPwCqd", "202212", "78"));
    validateRow(response, List.of("AD5jueZTZSK", "CivTksSoCt0", "ImspTQPwCqd", "202208", "78"));
    validateRow(response, List.of("AD5jueZTZSK", "wgbW2ZQnlIc", "ImspTQPwCqd", "202210", "77"));
    validateRow(response, List.of("AD5jueZTZSK", "GWuQsWJDGvN", "ImspTQPwCqd", "202208", "76"));
    validateRow(response, List.of("AD5jueZTZSK", "wgbW2ZQnlIc", "ImspTQPwCqd", "202211", "76"));
    validateRow(response, List.of("AD5jueZTZSK", "BHlWGFLIU20", "ImspTQPwCqd", "202208", "75"));
    validateRow(response, List.of("AD5jueZTZSK", "CivTksSoCt0", "ImspTQPwCqd", "202212", "72"));
    validateRow(response, List.of("AD5jueZTZSK", "B1X4JyH4Mdw", "ImspTQPwCqd", "202212", "47"));
    validateRow(response, List.of("AD5jueZTZSK", "B1X4JyH4Mdw", "ImspTQPwCqd", "202208", "47"));
    validateRow(response, List.of("AD5jueZTZSK", "B1X4JyH4Mdw", "ImspTQPwCqd", "202207", "43"));
    validateRow(response, List.of("AD5jueZTZSK", "B1X4JyH4Mdw", "ImspTQPwCqd", "202209", "42"));
    validateRow(response, List.of("AD5jueZTZSK", "B1X4JyH4Mdw", "ImspTQPwCqd", "202210", "41"));
    validateRow(response, List.of("AD5jueZTZSK", "B1X4JyH4Mdw", "ImspTQPwCqd", "202211", "36"));
    validateRow(response, List.of("", "", "ImspTQPwCqd", "202208", "2"));
  }
}
