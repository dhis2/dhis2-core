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
public class EventsAggregate7AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void queryMalariaTreatmentsByImplementingPartnerLast12Months() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=kO3z4Dhc038")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS,kO3z4Dhc038.O7OiONht8T3,LFsZ8v5v7rq:C6nZpLKjEJr;CW81uF03hvV;B3nxOazOO2G;RkbOhHwiOgW;TY5rBQzlBRa;hERJraxV8D9;g3bcPGD5Q5i;yfWXlxYNbhy;yrwgRxRhBoU;VLFVaH1MwnF;e5YBV5F5iUd;XK6u6cJCR0t;uilaJSyXt7d")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("bMcwwoVnbSR", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(312)))
        .body("height", equalTo(312))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"uilaJSyXt7d\":{\"name\":\"World Vision\"},\"202208\":{\"name\":\"August 2022\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"VLFVaH1MwnF\":{\"name\":\"Pathfinder International\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"CW81uF03hvV\":{\"name\":\"AIDSRelief Consortium\"},\"202303\":{\"name\":\"March 2023\"},\"hERJraxV8D9\":{\"name\":\"Hope Worldwide\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"TY5rBQzlBRa\":{\"name\":\"Family Health International\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202211\":{\"name\":\"November 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202212\":{\"name\":\"December 2022\"},\"XK6u6cJCR0t\":{\"name\":\"Population Services International\"},\"202210\":{\"name\":\"October 2022\"},\"KS73nQGyKdd\":{\"code\":\"REF_ACT_STOCKOUT\",\"name\":\"Referred ACT stockout\"},\"B3nxOazOO2G\":{\"name\":\"APHIAplus\"},\"RkbOhHwiOgW\":{\"name\":\"CARE International\"},\"O7OiONht8T3\":{\"name\":\"Treatment Malaria\"},\"ou\":{\"name\":\"Organisation unit\"},\"WmQqeIrSJOV\":{\"code\":\"ACT6x2\",\"name\":\"ACT 6x2\"},\"XEKBlnGyi8w\":{\"code\":\"ACT6x1\",\"name\":\"ACT 6x1\"},\"LFsZ8v5v7rq\":{\"name\":\"Implementing Partner\"},\"uCChCCEXKoJ\":{\"code\":\"COMB_BLISTER\",\"name\":\"Combined blister\"},\"g3bcPGD5Q5i\":{\"name\":\"International Rescue Committee\"},\"yrwgRxRhBoU\":{\"name\":\"Path\"},\"yfWXlxYNbhy\":{\"name\":\"IntraHealth International\"},\"e5YBV5F5iUd\":{\"name\":\"Plan International\"},\"C6nZpLKjEJr\":{\"name\":\"African Medical and Research Foundation\"},\"pe\":{\"name\":\"Period\"},\"bMcwwoVnbSR\":{\"name\":\"Malaria testing and surveillance\"},\"kO3z4Dhc038\":{\"name\":\"Malaria testing and surveillance\"}},\"dimensions\":{\"O7OiONht8T3\":[\"XEKBlnGyi8w\",\"WmQqeIrSJOV\",\"KS73nQGyKdd\",\"uCChCCEXKoJ\"],\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"ou\":[\"ImspTQPwCqd\"],\"LFsZ8v5v7rq\":[\"C6nZpLKjEJr\",\"CW81uF03hvV\",\"B3nxOazOO2G\",\"RkbOhHwiOgW\",\"TY5rBQzlBRa\",\"hERJraxV8D9\",\"g3bcPGD5Q5i\",\"yfWXlxYNbhy\",\"yrwgRxRhBoU\",\"VLFVaH1MwnF\",\"e5YBV5F5iUd\",\"XK6u6cJCR0t\",\"uilaJSyXt7d\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "O7OiONht8T3", "Treatment Malaria", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "LFsZ8v5v7rq",
        "Implementing Partner",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response, List.of("REF_ACT_STOCKOUT", "VLFVaH1MwnF", "ImspTQPwCqd", "202210", "13"));
    validateRow(
        response, List.of("REF_ACT_STOCKOUT", "yfWXlxYNbhy", "ImspTQPwCqd", "202207", "13"));
    validateRow(
        response, List.of("REF_ACT_STOCKOUT", "g3bcPGD5Q5i", "ImspTQPwCqd", "202211", "12"));
    validateRow(
        response, List.of("REF_ACT_STOCKOUT", "CW81uF03hvV", "ImspTQPwCqd", "202209", "12"));
    validateRow(
        response, List.of("REF_ACT_STOCKOUT", "C6nZpLKjEJr", "ImspTQPwCqd", "202208", "12"));
    validateRow(
        response, List.of("REF_ACT_STOCKOUT", "uilaJSyXt7d", "ImspTQPwCqd", "202210", "12"));
    validateRow(
        response, List.of("REF_ACT_STOCKOUT", "yrwgRxRhBoU", "ImspTQPwCqd", "202208", "11"));
    validateRow(
        response, List.of("REF_ACT_STOCKOUT", "C6nZpLKjEJr", "ImspTQPwCqd", "202207", "11"));
    validateRow(
        response, List.of("REF_ACT_STOCKOUT", "hERJraxV8D9", "ImspTQPwCqd", "202207", "11"));
    validateRow(
        response, List.of("REF_ACT_STOCKOUT", "e5YBV5F5iUd", "ImspTQPwCqd", "202210", "10"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "e5YBV5F5iUd", "ImspTQPwCqd", "202209", "9"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "e5YBV5F5iUd", "ImspTQPwCqd", "202208", "9"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "B3nxOazOO2G", "ImspTQPwCqd", "202212", "9"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "RkbOhHwiOgW", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "g3bcPGD5Q5i", "ImspTQPwCqd", "202210", "9"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "hERJraxV8D9", "ImspTQPwCqd", "202208", "9"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "RkbOhHwiOgW", "ImspTQPwCqd", "202207", "8"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "VLFVaH1MwnF", "ImspTQPwCqd", "202207", "8"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "TY5rBQzlBRa", "ImspTQPwCqd", "202212", "8"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "e5YBV5F5iUd", "ImspTQPwCqd", "202207", "8"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "hERJraxV8D9", "ImspTQPwCqd", "202211", "8"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "VLFVaH1MwnF", "ImspTQPwCqd", "202212", "7"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "yrwgRxRhBoU", "ImspTQPwCqd", "202209", "7"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "VLFVaH1MwnF", "ImspTQPwCqd", "202208", "7"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "RkbOhHwiOgW", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "XK6u6cJCR0t", "ImspTQPwCqd", "202208", "7"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "B3nxOazOO2G", "ImspTQPwCqd", "202209", "7"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "TY5rBQzlBRa", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "uilaJSyXt7d", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "yfWXlxYNbhy", "ImspTQPwCqd", "202212", "7"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "yfWXlxYNbhy", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "g3bcPGD5Q5i", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "hERJraxV8D9", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "B3nxOazOO2G", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "VLFVaH1MwnF", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "e5YBV5F5iUd", "ImspTQPwCqd", "202212", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "B3nxOazOO2G", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "XK6u6cJCR0t", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "C6nZpLKjEJr", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "XK6u6cJCR0t", "ImspTQPwCqd", "202209", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "CW81uF03hvV", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "XK6u6cJCR0t", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "TY5rBQzlBRa", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "TY5rBQzlBRa", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "yfWXlxYNbhy", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "VLFVaH1MwnF", "ImspTQPwCqd", "202209", "6"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "uilaJSyXt7d", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "hERJraxV8D9", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "XK6u6cJCR0t", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "yrwgRxRhBoU", "ImspTQPwCqd", "202207", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "XK6u6cJCR0t", "ImspTQPwCqd", "202211", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "C6nZpLKjEJr", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "e5YBV5F5iUd", "ImspTQPwCqd", "202211", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "yfWXlxYNbhy", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "hERJraxV8D9", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "CW81uF03hvV", "ImspTQPwCqd", "202207", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "yfWXlxYNbhy", "ImspTQPwCqd", "202210", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "TY5rBQzlBRa", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "CW81uF03hvV", "ImspTQPwCqd", "202211", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "RkbOhHwiOgW", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "uilaJSyXt7d", "ImspTQPwCqd", "202207", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "B3nxOazOO2G", "ImspTQPwCqd", "202210", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "uilaJSyXt7d", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "C6nZpLKjEJr", "ImspTQPwCqd", "202210", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "C6nZpLKjEJr", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "RkbOhHwiOgW", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "g3bcPGD5Q5i", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "yrwgRxRhBoU", "ImspTQPwCqd", "202210", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "CW81uF03hvV", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "CW81uF03hvV", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "TY5rBQzlBRa", "ImspTQPwCqd", "202207", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "g3bcPGD5Q5i", "ImspTQPwCqd", "202209", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "g3bcPGD5Q5i", "ImspTQPwCqd", "202212", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "yrwgRxRhBoU", "ImspTQPwCqd", "202212", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "uilaJSyXt7d", "ImspTQPwCqd", "202209", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "RkbOhHwiOgW", "ImspTQPwCqd", "202212", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "yrwgRxRhBoU", "ImspTQPwCqd", "202211", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "B3nxOazOO2G", "ImspTQPwCqd", "202211", "1"));
    validateRow(response, List.of("COMB_BLISTER", "C6nZpLKjEJr", "ImspTQPwCqd", "202212", "13"));
    validateRow(response, List.of("COMB_BLISTER", "e5YBV5F5iUd", "ImspTQPwCqd", "202208", "12"));
    validateRow(response, List.of("COMB_BLISTER", "VLFVaH1MwnF", "ImspTQPwCqd", "202207", "12"));
    validateRow(response, List.of("COMB_BLISTER", "e5YBV5F5iUd", "ImspTQPwCqd", "202211", "11"));
    validateRow(response, List.of("COMB_BLISTER", "CW81uF03hvV", "ImspTQPwCqd", "202207", "11"));
    validateRow(response, List.of("COMB_BLISTER", "g3bcPGD5Q5i", "ImspTQPwCqd", "202207", "11"));
    validateRow(response, List.of("COMB_BLISTER", "B3nxOazOO2G", "ImspTQPwCqd", "202211", "10"));
    validateRow(response, List.of("COMB_BLISTER", "CW81uF03hvV", "ImspTQPwCqd", "202208", "10"));
    validateRow(response, List.of("COMB_BLISTER", "g3bcPGD5Q5i", "ImspTQPwCqd", "202211", "10"));
    validateRow(response, List.of("COMB_BLISTER", "VLFVaH1MwnF", "ImspTQPwCqd", "202210", "10"));
    validateRow(response, List.of("COMB_BLISTER", "XK6u6cJCR0t", "ImspTQPwCqd", "202210", "10"));
    validateRow(response, List.of("COMB_BLISTER", "VLFVaH1MwnF", "ImspTQPwCqd", "202209", "9"));
    validateRow(response, List.of("COMB_BLISTER", "yfWXlxYNbhy", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, List.of("COMB_BLISTER", "hERJraxV8D9", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, List.of("COMB_BLISTER", "yrwgRxRhBoU", "ImspTQPwCqd", "202209", "9"));
    validateRow(response, List.of("COMB_BLISTER", "yrwgRxRhBoU", "ImspTQPwCqd", "202210", "9"));
    validateRow(response, List.of("COMB_BLISTER", "TY5rBQzlBRa", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, List.of("COMB_BLISTER", "g3bcPGD5Q5i", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, List.of("COMB_BLISTER", "RkbOhHwiOgW", "ImspTQPwCqd", "202209", "8"));
    validateRow(response, List.of("COMB_BLISTER", "RkbOhHwiOgW", "ImspTQPwCqd", "202212", "8"));
    validateRow(response, List.of("COMB_BLISTER", "RkbOhHwiOgW", "ImspTQPwCqd", "202210", "8"));
    validateRow(response, List.of("COMB_BLISTER", "yrwgRxRhBoU", "ImspTQPwCqd", "202207", "8"));
    validateRow(response, List.of("COMB_BLISTER", "RkbOhHwiOgW", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, List.of("COMB_BLISTER", "e5YBV5F5iUd", "ImspTQPwCqd", "202207", "8"));
    validateRow(response, List.of("COMB_BLISTER", "uilaJSyXt7d", "ImspTQPwCqd", "202211", "8"));
    validateRow(response, List.of("COMB_BLISTER", "TY5rBQzlBRa", "ImspTQPwCqd", "202211", "8"));
    validateRow(response, List.of("COMB_BLISTER", "uilaJSyXt7d", "ImspTQPwCqd", "202212", "8"));
    validateRow(response, List.of("COMB_BLISTER", "VLFVaH1MwnF", "ImspTQPwCqd", "202212", "8"));
    validateRow(response, List.of("COMB_BLISTER", "g3bcPGD5Q5i", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, List.of("COMB_BLISTER", "VLFVaH1MwnF", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, List.of("COMB_BLISTER", "C6nZpLKjEJr", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, List.of("COMB_BLISTER", "yfWXlxYNbhy", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, List.of("COMB_BLISTER", "XK6u6cJCR0t", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, List.of("COMB_BLISTER", "CW81uF03hvV", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, List.of("COMB_BLISTER", "TY5rBQzlBRa", "ImspTQPwCqd", "202209", "7"));
    validateRow(response, List.of("COMB_BLISTER", "uilaJSyXt7d", "ImspTQPwCqd", "202209", "7"));
    validateRow(response, List.of("COMB_BLISTER", "yrwgRxRhBoU", "ImspTQPwCqd", "202208", "7"));
    validateRow(response, List.of("COMB_BLISTER", "RkbOhHwiOgW", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, List.of("COMB_BLISTER", "XK6u6cJCR0t", "ImspTQPwCqd", "202212", "7"));
    validateRow(response, List.of("COMB_BLISTER", "hERJraxV8D9", "ImspTQPwCqd", "202209", "6"));
    validateRow(response, List.of("COMB_BLISTER", "e5YBV5F5iUd", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, List.of("COMB_BLISTER", "yfWXlxYNbhy", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, List.of("COMB_BLISTER", "C6nZpLKjEJr", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, List.of("COMB_BLISTER", "C6nZpLKjEJr", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, List.of("COMB_BLISTER", "TY5rBQzlBRa", "ImspTQPwCqd", "202212", "6"));
    validateRow(response, List.of("COMB_BLISTER", "uilaJSyXt7d", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, List.of("COMB_BLISTER", "B3nxOazOO2G", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, List.of("COMB_BLISTER", "B3nxOazOO2G", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, List.of("COMB_BLISTER", "uilaJSyXt7d", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, List.of("COMB_BLISTER", "RkbOhHwiOgW", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, List.of("COMB_BLISTER", "C6nZpLKjEJr", "ImspTQPwCqd", "202209", "6"));
    validateRow(response, List.of("COMB_BLISTER", "XK6u6cJCR0t", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("COMB_BLISTER", "B3nxOazOO2G", "ImspTQPwCqd", "202208", "5"));
    validateRow(response, List.of("COMB_BLISTER", "yfWXlxYNbhy", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, List.of("COMB_BLISTER", "g3bcPGD5Q5i", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, List.of("COMB_BLISTER", "g3bcPGD5Q5i", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("COMB_BLISTER", "XK6u6cJCR0t", "ImspTQPwCqd", "202207", "5"));
    validateRow(response, List.of("COMB_BLISTER", "yfWXlxYNbhy", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("COMB_BLISTER", "XK6u6cJCR0t", "ImspTQPwCqd", "202208", "5"));
    validateRow(response, List.of("COMB_BLISTER", "yrwgRxRhBoU", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, List.of("COMB_BLISTER", "hERJraxV8D9", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, List.of("COMB_BLISTER", "B3nxOazOO2G", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, List.of("COMB_BLISTER", "VLFVaH1MwnF", "ImspTQPwCqd", "202208", "5"));
    validateRow(response, List.of("COMB_BLISTER", "hERJraxV8D9", "ImspTQPwCqd", "202208", "5"));
    validateRow(response, List.of("COMB_BLISTER", "hERJraxV8D9", "ImspTQPwCqd", "202207", "4"));
    validateRow(response, List.of("COMB_BLISTER", "yfWXlxYNbhy", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, List.of("COMB_BLISTER", "e5YBV5F5iUd", "ImspTQPwCqd", "202209", "4"));
    validateRow(response, List.of("COMB_BLISTER", "CW81uF03hvV", "ImspTQPwCqd", "202209", "4"));
    validateRow(response, List.of("COMB_BLISTER", "CW81uF03hvV", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, List.of("COMB_BLISTER", "yrwgRxRhBoU", "ImspTQPwCqd", "202211", "4"));
    validateRow(response, List.of("COMB_BLISTER", "C6nZpLKjEJr", "ImspTQPwCqd", "202207", "4"));
    validateRow(response, List.of("COMB_BLISTER", "TY5rBQzlBRa", "ImspTQPwCqd", "202210", "4"));
    validateRow(response, List.of("COMB_BLISTER", "e5YBV5F5iUd", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, List.of("COMB_BLISTER", "TY5rBQzlBRa", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, List.of("COMB_BLISTER", "hERJraxV8D9", "ImspTQPwCqd", "202210", "3"));
    validateRow(response, List.of("COMB_BLISTER", "uilaJSyXt7d", "ImspTQPwCqd", "202210", "3"));
    validateRow(response, List.of("COMB_BLISTER", "CW81uF03hvV", "ImspTQPwCqd", "202211", "2"));
    validateRow(response, List.of("COMB_BLISTER", "B3nxOazOO2G", "ImspTQPwCqd", "202209", "2"));
    validateRow(response, List.of("ACT6x2", "yrwgRxRhBoU", "ImspTQPwCqd", "202209", "16"));
    validateRow(response, List.of("ACT6x2", "B3nxOazOO2G", "ImspTQPwCqd", "202209", "14"));
    validateRow(response, List.of("ACT6x2", "g3bcPGD5Q5i", "ImspTQPwCqd", "202209", "14"));
    validateRow(response, List.of("ACT6x2", "XK6u6cJCR0t", "ImspTQPwCqd", "202207", "12"));
    validateRow(response, List.of("ACT6x2", "g3bcPGD5Q5i", "ImspTQPwCqd", "202210", "12"));
    validateRow(response, List.of("ACT6x2", "yfWXlxYNbhy", "ImspTQPwCqd", "202212", "10"));
    validateRow(response, List.of("ACT6x2", "TY5rBQzlBRa", "ImspTQPwCqd", "202208", "9"));
    validateRow(response, List.of("ACT6x2", "CW81uF03hvV", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, List.of("ACT6x2", "C6nZpLKjEJr", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, List.of("ACT6x2", "C6nZpLKjEJr", "ImspTQPwCqd", "202212", "9"));
    validateRow(response, List.of("ACT6x2", "VLFVaH1MwnF", "ImspTQPwCqd", "202212", "9"));
    validateRow(response, List.of("ACT6x2", "yrwgRxRhBoU", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, List.of("ACT6x2", "uilaJSyXt7d", "ImspTQPwCqd", "202212", "9"));
    validateRow(response, List.of("ACT6x2", "VLFVaH1MwnF", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, List.of("ACT6x2", "hERJraxV8D9", "ImspTQPwCqd", "202209", "9"));
    validateRow(response, List.of("ACT6x2", "hERJraxV8D9", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, List.of("ACT6x2", "uilaJSyXt7d", "ImspTQPwCqd", "202209", "8"));
    validateRow(response, List.of("ACT6x2", "e5YBV5F5iUd", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, List.of("ACT6x2", "CW81uF03hvV", "ImspTQPwCqd", "202212", "8"));
    validateRow(response, List.of("ACT6x2", "RkbOhHwiOgW", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, List.of("ACT6x2", "uilaJSyXt7d", "ImspTQPwCqd", "202207", "8"));
    validateRow(response, List.of("ACT6x2", "RkbOhHwiOgW", "ImspTQPwCqd", "202212", "8"));
    validateRow(response, List.of("ACT6x2", "CW81uF03hvV", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, List.of("ACT6x2", "g3bcPGD5Q5i", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, List.of("ACT6x2", "e5YBV5F5iUd", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, List.of("ACT6x2", "g3bcPGD5Q5i", "ImspTQPwCqd", "202208", "7"));
    validateRow(response, List.of("ACT6x2", "RkbOhHwiOgW", "ImspTQPwCqd", "202207", "7"));
    validateRow(response, List.of("ACT6x2", "XK6u6cJCR0t", "ImspTQPwCqd", "202212", "7"));
    validateRow(response, List.of("ACT6x2", "uilaJSyXt7d", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, List.of("ACT6x2", "B3nxOazOO2G", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, List.of("ACT6x2", "VLFVaH1MwnF", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, List.of("ACT6x2", "XK6u6cJCR0t", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, List.of("ACT6x2", "uilaJSyXt7d", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, List.of("ACT6x2", "g3bcPGD5Q5i", "ImspTQPwCqd", "202212", "6"));
    validateRow(response, List.of("ACT6x2", "yfWXlxYNbhy", "ImspTQPwCqd", "202209", "6"));
    validateRow(response, List.of("ACT6x2", "C6nZpLKjEJr", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, List.of("ACT6x2", "B3nxOazOO2G", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, List.of("ACT6x2", "yrwgRxRhBoU", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, List.of("ACT6x2", "C6nZpLKjEJr", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, List.of("ACT6x2", "hERJraxV8D9", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, List.of("ACT6x2", "yrwgRxRhBoU", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, List.of("ACT6x2", "hERJraxV8D9", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, List.of("ACT6x2", "CW81uF03hvV", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, List.of("ACT6x2", "XK6u6cJCR0t", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, List.of("ACT6x2", "C6nZpLKjEJr", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, List.of("ACT6x2", "C6nZpLKjEJr", "ImspTQPwCqd", "202209", "6"));
    validateRow(response, List.of("ACT6x2", "yfWXlxYNbhy", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, List.of("ACT6x2", "g3bcPGD5Q5i", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, List.of("ACT6x2", "yrwgRxRhBoU", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, List.of("ACT6x2", "TY5rBQzlBRa", "ImspTQPwCqd", "202210", "5"));
    validateRow(response, List.of("ACT6x2", "VLFVaH1MwnF", "ImspTQPwCqd", "202208", "5"));
    validateRow(response, List.of("ACT6x2", "B3nxOazOO2G", "ImspTQPwCqd", "202208", "5"));
    validateRow(response, List.of("ACT6x2", "e5YBV5F5iUd", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("ACT6x2", "XK6u6cJCR0t", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("ACT6x2", "VLFVaH1MwnF", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("ACT6x2", "B3nxOazOO2G", "ImspTQPwCqd", "202207", "5"));
    validateRow(response, List.of("ACT6x2", "TY5rBQzlBRa", "ImspTQPwCqd", "202211", "5"));
    validateRow(response, List.of("ACT6x2", "VLFVaH1MwnF", "ImspTQPwCqd", "202211", "4"));
    validateRow(response, List.of("ACT6x2", "yfWXlxYNbhy", "ImspTQPwCqd", "202211", "4"));
    validateRow(response, List.of("ACT6x2", "RkbOhHwiOgW", "ImspTQPwCqd", "202211", "4"));
    validateRow(response, List.of("ACT6x2", "uilaJSyXt7d", "ImspTQPwCqd", "202211", "4"));
    validateRow(response, List.of("ACT6x2", "yfWXlxYNbhy", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, List.of("ACT6x2", "CW81uF03hvV", "ImspTQPwCqd", "202210", "4"));
    validateRow(response, List.of("ACT6x2", "hERJraxV8D9", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, List.of("ACT6x2", "XK6u6cJCR0t", "ImspTQPwCqd", "202211", "4"));
    validateRow(response, List.of("ACT6x2", "TY5rBQzlBRa", "ImspTQPwCqd", "202207", "4"));
    validateRow(response, List.of("ACT6x2", "yfWXlxYNbhy", "ImspTQPwCqd", "202210", "4"));
    validateRow(response, List.of("ACT6x2", "B3nxOazOO2G", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, List.of("ACT6x2", "TY5rBQzlBRa", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, List.of("ACT6x2", "CW81uF03hvV", "ImspTQPwCqd", "202209", "3"));
    validateRow(response, List.of("ACT6x2", "RkbOhHwiOgW", "ImspTQPwCqd", "202210", "3"));
    validateRow(response, List.of("ACT6x2", "TY5rBQzlBRa", "ImspTQPwCqd", "202209", "3"));
    validateRow(response, List.of("ACT6x2", "e5YBV5F5iUd", "ImspTQPwCqd", "202212", "3"));
    validateRow(response, List.of("ACT6x2", "yrwgRxRhBoU", "ImspTQPwCqd", "202211", "3"));
    validateRow(response, List.of("ACT6x2", "e5YBV5F5iUd", "ImspTQPwCqd", "202211", "3"));
    validateRow(response, List.of("ACT6x2", "e5YBV5F5iUd", "ImspTQPwCqd", "202207", "2"));
    validateRow(response, List.of("ACT6x2", "RkbOhHwiOgW", "ImspTQPwCqd", "202209", "2"));
    validateRow(response, List.of("ACT6x2", "hERJraxV8D9", "ImspTQPwCqd", "202210", "2"));
    validateRow(response, List.of("ACT6x1", "C6nZpLKjEJr", "ImspTQPwCqd", "202210", "14"));
    validateRow(response, List.of("ACT6x1", "C6nZpLKjEJr", "ImspTQPwCqd", "202207", "13"));
    validateRow(response, List.of("ACT6x1", "e5YBV5F5iUd", "ImspTQPwCqd", "202209", "12"));
    validateRow(response, List.of("ACT6x1", "TY5rBQzlBRa", "ImspTQPwCqd", "202209", "12"));
    validateRow(response, List.of("ACT6x1", "yrwgRxRhBoU", "ImspTQPwCqd", "202210", "11"));
    validateRow(response, List.of("ACT6x1", "g3bcPGD5Q5i", "ImspTQPwCqd", "202210", "10"));
    validateRow(response, List.of("ACT6x1", "TY5rBQzlBRa", "ImspTQPwCqd", "202208", "10"));
    validateRow(response, List.of("ACT6x1", "yfWXlxYNbhy", "ImspTQPwCqd", "202207", "10"));
    validateRow(response, List.of("ACT6x1", "RkbOhHwiOgW", "ImspTQPwCqd", "202211", "10"));
    validateRow(response, List.of("ACT6x1", "uilaJSyXt7d", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, List.of("ACT6x1", "yrwgRxRhBoU", "ImspTQPwCqd", "202208", "9"));
    validateRow(response, List.of("ACT6x1", "uilaJSyXt7d", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, List.of("ACT6x1", "VLFVaH1MwnF", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, List.of("ACT6x1", "B3nxOazOO2G", "ImspTQPwCqd", "202212", "9"));
    validateRow(response, List.of("ACT6x1", "TY5rBQzlBRa", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, List.of("ACT6x1", "B3nxOazOO2G", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, List.of("ACT6x1", "e5YBV5F5iUd", "ImspTQPwCqd", "202208", "9"));
    validateRow(response, List.of("ACT6x1", "CW81uF03hvV", "ImspTQPwCqd", "202207", "8"));
    validateRow(response, List.of("ACT6x1", "B3nxOazOO2G", "ImspTQPwCqd", "202210", "8"));
    validateRow(response, List.of("ACT6x1", "g3bcPGD5Q5i", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, List.of("ACT6x1", "C6nZpLKjEJr", "ImspTQPwCqd", "202211", "8"));
    validateRow(response, List.of("ACT6x1", "VLFVaH1MwnF", "ImspTQPwCqd", "202210", "8"));
    validateRow(response, List.of("ACT6x1", "yfWXlxYNbhy", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, List.of("ACT6x1", "TY5rBQzlBRa", "ImspTQPwCqd", "202207", "8"));
    validateRow(response, List.of("ACT6x1", "hERJraxV8D9", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, List.of("ACT6x1", "CW81uF03hvV", "ImspTQPwCqd", "202212", "8"));
    validateRow(response, List.of("ACT6x1", "g3bcPGD5Q5i", "ImspTQPwCqd", "202207", "7"));
    validateRow(response, List.of("ACT6x1", "XK6u6cJCR0t", "ImspTQPwCqd", "202209", "7"));
    validateRow(response, List.of("ACT6x1", "e5YBV5F5iUd", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, List.of("ACT6x1", "XK6u6cJCR0t", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, List.of("ACT6x1", "TY5rBQzlBRa", "ImspTQPwCqd", "202212", "7"));
    validateRow(response, List.of("ACT6x1", "C6nZpLKjEJr", "ImspTQPwCqd", "202208", "7"));
    validateRow(response, List.of("ACT6x1", "yrwgRxRhBoU", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, List.of("ACT6x1", "e5YBV5F5iUd", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, List.of("ACT6x1", "hERJraxV8D9", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, List.of("ACT6x1", "g3bcPGD5Q5i", "ImspTQPwCqd", "202209", "7"));
    validateRow(response, List.of("ACT6x1", "XK6u6cJCR0t", "ImspTQPwCqd", "202207", "7"));
    validateRow(response, List.of("ACT6x1", "yrwgRxRhBoU", "ImspTQPwCqd", "202212", "7"));
    validateRow(response, List.of("ACT6x1", "yfWXlxYNbhy", "ImspTQPwCqd", "202209", "7"));
    validateRow(response, List.of("ACT6x1", "hERJraxV8D9", "ImspTQPwCqd", "202209", "7"));
    validateRow(response, List.of("ACT6x1", "uilaJSyXt7d", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, List.of("ACT6x1", "e5YBV5F5iUd", "ImspTQPwCqd", "202212", "6"));
    validateRow(response, List.of("ACT6x1", "XK6u6cJCR0t", "ImspTQPwCqd", "202212", "6"));
    validateRow(response, List.of("ACT6x1", "uilaJSyXt7d", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, List.of("ACT6x1", "RkbOhHwiOgW", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, List.of("ACT6x1", "B3nxOazOO2G", "ImspTQPwCqd", "202209", "6"));
    validateRow(response, List.of("ACT6x1", "CW81uF03hvV", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, List.of("ACT6x1", "hERJraxV8D9", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, List.of("ACT6x1", "g3bcPGD5Q5i", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, List.of("ACT6x1", "VLFVaH1MwnF", "ImspTQPwCqd", "202208", "5"));
    validateRow(response, List.of("ACT6x1", "TY5rBQzlBRa", "ImspTQPwCqd", "202210", "5"));
    validateRow(response, List.of("ACT6x1", "C6nZpLKjEJr", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("ACT6x1", "VLFVaH1MwnF", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, List.of("ACT6x1", "RkbOhHwiOgW", "ImspTQPwCqd", "202208", "5"));
    validateRow(response, List.of("ACT6x1", "uilaJSyXt7d", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("ACT6x1", "CW81uF03hvV", "ImspTQPwCqd", "202210", "5"));
    validateRow(response, List.of("ACT6x1", "VLFVaH1MwnF", "ImspTQPwCqd", "202211", "5"));
    validateRow(response, List.of("ACT6x1", "e5YBV5F5iUd", "ImspTQPwCqd", "202207", "5"));
    validateRow(response, List.of("ACT6x1", "CW81uF03hvV", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, List.of("ACT6x1", "hERJraxV8D9", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, List.of("ACT6x1", "CW81uF03hvV", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, List.of("ACT6x1", "hERJraxV8D9", "ImspTQPwCqd", "202207", "4"));
    validateRow(response, List.of("ACT6x1", "C6nZpLKjEJr", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, List.of("ACT6x1", "XK6u6cJCR0t", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, List.of("ACT6x1", "yrwgRxRhBoU", "ImspTQPwCqd", "202209", "4"));
    validateRow(response, List.of("ACT6x1", "uilaJSyXt7d", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, List.of("ACT6x1", "RkbOhHwiOgW", "ImspTQPwCqd", "202209", "4"));
    validateRow(response, List.of("ACT6x1", "B3nxOazOO2G", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, List.of("ACT6x1", "yfWXlxYNbhy", "ImspTQPwCqd", "202210", "4"));
    validateRow(response, List.of("ACT6x1", "RkbOhHwiOgW", "ImspTQPwCqd", "202210", "3"));
    validateRow(response, List.of("ACT6x1", "XK6u6cJCR0t", "ImspTQPwCqd", "202211", "3"));
    validateRow(response, List.of("ACT6x1", "yfWXlxYNbhy", "ImspTQPwCqd", "202212", "3"));
    validateRow(response, List.of("ACT6x1", "yfWXlxYNbhy", "ImspTQPwCqd", "202211", "3"));
    validateRow(response, List.of("ACT6x1", "VLFVaH1MwnF", "ImspTQPwCqd", "202209", "3"));
    validateRow(response, List.of("ACT6x1", "B3nxOazOO2G", "ImspTQPwCqd", "202207", "2"));
    validateRow(response, List.of("ACT6x1", "yrwgRxRhBoU", "ImspTQPwCqd", "202207", "1"));
    validateRow(response, List.of("ACT6x1", "g3bcPGD5Q5i", "ImspTQPwCqd", "202212", "1"));
    validateRow(response, List.of("ACT6x1", "RkbOhHwiOgW", "ImspTQPwCqd", "202212", "1"));
  }

  @Test
  public void queryEnrollmentoutput1() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=ZzYYXq4fJie")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS")
            .add("relativePeriodDate=2023-07-14");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(7)))
        .body("height", equalTo(7))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"202208\":{\"name\":\"August 2022\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202211\":{\"name\":\"November 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202212\":{\"name\":\"December 2022\"},\"202210\":{\"name\":\"October 2022\"},\"pe\":{\"name\":\"Period\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("ImspTQPwCqd", "202207", "619"));
    validateRow(response, List.of("ImspTQPwCqd", "202208", "690"));
    validateRow(response, List.of("ImspTQPwCqd", "202209", "669"));
    validateRow(response, List.of("ImspTQPwCqd", "202210", "686"));
    validateRow(response, List.of("ImspTQPwCqd", "202211", "633"));
    validateRow(response, List.of("ImspTQPwCqd", "202212", "710"));
    validateRow(response, List.of("ImspTQPwCqd", "202301", "19"));
  }

  @Test
  public void queryEnrollmentoutput2() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ZzYYXq4fJie.w75KJ2mc4zz:LIKE:a,ZzYYXq4fJie.lZGmxYbs97q")
            .add("stage=ZzYYXq4fJie")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS")
            .add("relativePeriodDate=2023-07-14");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(7)))
        .body("height", equalTo(7))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"lZGmxYbs97q\":{\"name\":\"Unique ID\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"w75KJ2mc4zz\":{\"name\":\"First name\"},\"202208\":{\"name\":\"August 2022\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202211\":{\"name\":\"November 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202212\":{\"name\":\"December 2022\"},\"202210\":{\"name\":\"October 2022\"},\"pe\":{\"name\":\"Period\"}},\"dimensions\":{\"lZGmxYbs97q\":[],\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"ou\":[\"ImspTQPwCqd\"],\"w75KJ2mc4zz\":[\"like a\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("ImspTQPwCqd", "202207", "358"));
    validateRow(response, List.of("ImspTQPwCqd", "202208", "394"));
    validateRow(response, List.of("ImspTQPwCqd", "202209", "371"));
    validateRow(response, List.of("ImspTQPwCqd", "202210", "404"));
    validateRow(response, List.of("ImspTQPwCqd", "202211", "368"));
    validateRow(response, List.of("ImspTQPwCqd", "202212", "385"));
    validateRow(response, List.of("ImspTQPwCqd", "202301", "14"));
  }

  @Test
  public void queryEnrollmentoutput3() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=uvMKOn1oWvd.zgnTlAH4ZOk")
            .add("stage=uvMKOn1oWvd")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("dimension=ou:ImspTQPwCqd,pe:2019;2020;2021;LAST_5_YEARS,uvMKOn1oWvd.fADIatyOu2g")
            .add("relativePeriodDate=2021-01-01");

    // When
    ApiResponse response = actions.aggregate().get("M3xtLkYBlKI", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"fADIatyOu2g\":{\"name\":\"LLIN coverage (%)\"},\"ou\":{\"name\":\"Organisation unit\"},\"2021\":{\"name\":\"2021\"},\"2020\":{\"name\":\"2020\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"zgnTlAH4ZOk\":{\"name\":\"Follow-up vector control action details\"},\"2019\":{\"name\":\"2019\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"2018\":{\"name\":\"2018\"},\"2017\":{\"name\":\"2017\"},\"2016\":{\"name\":\"2016\"},\"pe\":{\"name\":\"Period\"},\"uvMKOn1oWvd\":{\"name\":\"Foci response\"},\"M3xtLkYBlKI\":{\"name\":\"Malaria focus investigation\"}},\"dimensions\":{\"uvMKOn1oWvd.fADIatyOu2g\":[],\"pe\":[\"2016\",\"2017\",\"2018\",\"2019\",\"2020\",\"2021\"],\"ou\":[\"ImspTQPwCqd\"],\"zgnTlAH4ZOk\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "fADIatyOu2g",
        "LLIN coverage (%)",
        "PERCENTAGE",
        "java.lang.Double",
        false,
        true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("", "2021", "ImspTQPwCqd", "4"));
  }
}
