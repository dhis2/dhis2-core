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
        response, 0, List.of("REF_ACT_STOCKOUT", "VLFVaH1MwnF", "ImspTQPwCqd", "202210", "13"));
    validateRow(
        response, 1, List.of("REF_ACT_STOCKOUT", "yfWXlxYNbhy", "ImspTQPwCqd", "202207", "13"));
    validateRow(
        response, 2, List.of("REF_ACT_STOCKOUT", "g3bcPGD5Q5i", "ImspTQPwCqd", "202211", "12"));
    validateRow(
        response, 3, List.of("REF_ACT_STOCKOUT", "CW81uF03hvV", "ImspTQPwCqd", "202209", "12"));
    validateRow(
        response, 4, List.of("REF_ACT_STOCKOUT", "C6nZpLKjEJr", "ImspTQPwCqd", "202208", "12"));
    validateRow(
        response, 5, List.of("REF_ACT_STOCKOUT", "uilaJSyXt7d", "ImspTQPwCqd", "202210", "12"));
    validateRow(
        response, 6, List.of("REF_ACT_STOCKOUT", "yrwgRxRhBoU", "ImspTQPwCqd", "202208", "11"));
    validateRow(
        response, 7, List.of("REF_ACT_STOCKOUT", "C6nZpLKjEJr", "ImspTQPwCqd", "202207", "11"));
    validateRow(
        response, 8, List.of("REF_ACT_STOCKOUT", "hERJraxV8D9", "ImspTQPwCqd", "202207", "11"));
    validateRow(
        response, 9, List.of("REF_ACT_STOCKOUT", "e5YBV5F5iUd", "ImspTQPwCqd", "202210", "10"));
    validateRow(
        response, 10, List.of("REF_ACT_STOCKOUT", "e5YBV5F5iUd", "ImspTQPwCqd", "202209", "9"));
    validateRow(
        response, 11, List.of("REF_ACT_STOCKOUT", "e5YBV5F5iUd", "ImspTQPwCqd", "202208", "9"));
    validateRow(
        response, 12, List.of("REF_ACT_STOCKOUT", "B3nxOazOO2G", "ImspTQPwCqd", "202212", "9"));
    validateRow(
        response, 13, List.of("REF_ACT_STOCKOUT", "RkbOhHwiOgW", "ImspTQPwCqd", "202211", "9"));
    validateRow(
        response, 14, List.of("REF_ACT_STOCKOUT", "g3bcPGD5Q5i", "ImspTQPwCqd", "202210", "9"));
    validateRow(
        response, 15, List.of("REF_ACT_STOCKOUT", "hERJraxV8D9", "ImspTQPwCqd", "202208", "9"));
    validateRow(
        response, 16, List.of("REF_ACT_STOCKOUT", "RkbOhHwiOgW", "ImspTQPwCqd", "202207", "8"));
    validateRow(
        response, 17, List.of("REF_ACT_STOCKOUT", "VLFVaH1MwnF", "ImspTQPwCqd", "202207", "8"));
    validateRow(
        response, 18, List.of("REF_ACT_STOCKOUT", "TY5rBQzlBRa", "ImspTQPwCqd", "202212", "8"));
    validateRow(
        response, 19, List.of("REF_ACT_STOCKOUT", "e5YBV5F5iUd", "ImspTQPwCqd", "202207", "8"));
    validateRow(
        response, 20, List.of("REF_ACT_STOCKOUT", "hERJraxV8D9", "ImspTQPwCqd", "202211", "8"));
    validateRow(
        response, 21, List.of("REF_ACT_STOCKOUT", "VLFVaH1MwnF", "ImspTQPwCqd", "202212", "7"));
    validateRow(
        response, 22, List.of("REF_ACT_STOCKOUT", "yrwgRxRhBoU", "ImspTQPwCqd", "202209", "7"));
    validateRow(
        response, 23, List.of("REF_ACT_STOCKOUT", "VLFVaH1MwnF", "ImspTQPwCqd", "202208", "7"));
    validateRow(
        response, 24, List.of("REF_ACT_STOCKOUT", "RkbOhHwiOgW", "ImspTQPwCqd", "202210", "7"));
    validateRow(
        response, 25, List.of("REF_ACT_STOCKOUT", "XK6u6cJCR0t", "ImspTQPwCqd", "202208", "7"));
    validateRow(
        response, 26, List.of("REF_ACT_STOCKOUT", "B3nxOazOO2G", "ImspTQPwCqd", "202209", "7"));
    validateRow(
        response, 27, List.of("REF_ACT_STOCKOUT", "TY5rBQzlBRa", "ImspTQPwCqd", "202211", "7"));
    validateRow(
        response, 28, List.of("REF_ACT_STOCKOUT", "uilaJSyXt7d", "ImspTQPwCqd", "202211", "7"));
    validateRow(
        response, 29, List.of("REF_ACT_STOCKOUT", "yfWXlxYNbhy", "ImspTQPwCqd", "202212", "7"));
    validateRow(
        response, 30, List.of("REF_ACT_STOCKOUT", "yfWXlxYNbhy", "ImspTQPwCqd", "202211", "6"));
    validateRow(
        response, 31, List.of("REF_ACT_STOCKOUT", "g3bcPGD5Q5i", "ImspTQPwCqd", "202207", "6"));
    validateRow(
        response, 32, List.of("REF_ACT_STOCKOUT", "hERJraxV8D9", "ImspTQPwCqd", "202210", "6"));
    validateRow(
        response, 33, List.of("REF_ACT_STOCKOUT", "B3nxOazOO2G", "ImspTQPwCqd", "202207", "6"));
    validateRow(
        response, 34, List.of("REF_ACT_STOCKOUT", "VLFVaH1MwnF", "ImspTQPwCqd", "202211", "6"));
    validateRow(
        response, 35, List.of("REF_ACT_STOCKOUT", "e5YBV5F5iUd", "ImspTQPwCqd", "202212", "6"));
    validateRow(
        response, 36, List.of("REF_ACT_STOCKOUT", "B3nxOazOO2G", "ImspTQPwCqd", "202208", "6"));
    validateRow(
        response, 37, List.of("REF_ACT_STOCKOUT", "XK6u6cJCR0t", "ImspTQPwCqd", "202210", "6"));
    validateRow(
        response, 38, List.of("REF_ACT_STOCKOUT", "C6nZpLKjEJr", "ImspTQPwCqd", "202211", "6"));
    validateRow(
        response, 39, List.of("REF_ACT_STOCKOUT", "XK6u6cJCR0t", "ImspTQPwCqd", "202209", "6"));
    validateRow(
        response, 40, List.of("REF_ACT_STOCKOUT", "CW81uF03hvV", "ImspTQPwCqd", "202210", "6"));
    validateRow(
        response, 41, List.of("REF_ACT_STOCKOUT", "XK6u6cJCR0t", "ImspTQPwCqd", "202207", "6"));
    validateRow(
        response, 42, List.of("REF_ACT_STOCKOUT", "TY5rBQzlBRa", "ImspTQPwCqd", "202210", "6"));
    validateRow(
        response, 43, List.of("REF_ACT_STOCKOUT", "TY5rBQzlBRa", "ImspTQPwCqd", "202208", "6"));
    validateRow(
        response, 44, List.of("REF_ACT_STOCKOUT", "yfWXlxYNbhy", "ImspTQPwCqd", "202208", "6"));
    validateRow(
        response, 45, List.of("REF_ACT_STOCKOUT", "VLFVaH1MwnF", "ImspTQPwCqd", "202209", "6"));
    validateRow(
        response, 46, List.of("REF_ACT_STOCKOUT", "uilaJSyXt7d", "ImspTQPwCqd", "202212", "5"));
    validateRow(
        response, 47, List.of("REF_ACT_STOCKOUT", "hERJraxV8D9", "ImspTQPwCqd", "202209", "5"));
    validateRow(
        response, 48, List.of("REF_ACT_STOCKOUT", "XK6u6cJCR0t", "ImspTQPwCqd", "202212", "5"));
    validateRow(
        response, 49, List.of("REF_ACT_STOCKOUT", "yrwgRxRhBoU", "ImspTQPwCqd", "202207", "5"));
    validateRow(
        response, 50, List.of("REF_ACT_STOCKOUT", "XK6u6cJCR0t", "ImspTQPwCqd", "202211", "5"));
    validateRow(
        response, 51, List.of("REF_ACT_STOCKOUT", "C6nZpLKjEJr", "ImspTQPwCqd", "202209", "5"));
    validateRow(
        response, 52, List.of("REF_ACT_STOCKOUT", "e5YBV5F5iUd", "ImspTQPwCqd", "202211", "5"));
    validateRow(
        response, 53, List.of("REF_ACT_STOCKOUT", "yfWXlxYNbhy", "ImspTQPwCqd", "202209", "5"));
    validateRow(
        response, 54, List.of("REF_ACT_STOCKOUT", "hERJraxV8D9", "ImspTQPwCqd", "202212", "5"));
    validateRow(
        response, 55, List.of("REF_ACT_STOCKOUT", "CW81uF03hvV", "ImspTQPwCqd", "202207", "5"));
    validateRow(
        response, 56, List.of("REF_ACT_STOCKOUT", "yfWXlxYNbhy", "ImspTQPwCqd", "202210", "5"));
    validateRow(
        response, 57, List.of("REF_ACT_STOCKOUT", "TY5rBQzlBRa", "ImspTQPwCqd", "202209", "5"));
    validateRow(
        response, 58, List.of("REF_ACT_STOCKOUT", "CW81uF03hvV", "ImspTQPwCqd", "202211", "5"));
    validateRow(
        response, 59, List.of("REF_ACT_STOCKOUT", "RkbOhHwiOgW", "ImspTQPwCqd", "202209", "5"));
    validateRow(
        response, 60, List.of("REF_ACT_STOCKOUT", "uilaJSyXt7d", "ImspTQPwCqd", "202207", "4"));
    validateRow(
        response, 61, List.of("REF_ACT_STOCKOUT", "B3nxOazOO2G", "ImspTQPwCqd", "202210", "4"));
    validateRow(
        response, 62, List.of("REF_ACT_STOCKOUT", "uilaJSyXt7d", "ImspTQPwCqd", "202208", "4"));
    validateRow(
        response, 63, List.of("REF_ACT_STOCKOUT", "C6nZpLKjEJr", "ImspTQPwCqd", "202210", "4"));
    validateRow(
        response, 64, List.of("REF_ACT_STOCKOUT", "C6nZpLKjEJr", "ImspTQPwCqd", "202212", "4"));
    validateRow(
        response, 65, List.of("REF_ACT_STOCKOUT", "RkbOhHwiOgW", "ImspTQPwCqd", "202208", "4"));
    validateRow(
        response, 66, List.of("REF_ACT_STOCKOUT", "g3bcPGD5Q5i", "ImspTQPwCqd", "202208", "4"));
    validateRow(
        response, 67, List.of("REF_ACT_STOCKOUT", "yrwgRxRhBoU", "ImspTQPwCqd", "202210", "4"));
    validateRow(
        response, 68, List.of("REF_ACT_STOCKOUT", "CW81uF03hvV", "ImspTQPwCqd", "202212", "4"));
    validateRow(
        response, 69, List.of("REF_ACT_STOCKOUT", "CW81uF03hvV", "ImspTQPwCqd", "202208", "4"));
    validateRow(
        response, 70, List.of("REF_ACT_STOCKOUT", "TY5rBQzlBRa", "ImspTQPwCqd", "202207", "3"));
    validateRow(
        response, 71, List.of("REF_ACT_STOCKOUT", "g3bcPGD5Q5i", "ImspTQPwCqd", "202209", "3"));
    validateRow(
        response, 72, List.of("REF_ACT_STOCKOUT", "g3bcPGD5Q5i", "ImspTQPwCqd", "202212", "3"));
    validateRow(
        response, 73, List.of("REF_ACT_STOCKOUT", "yrwgRxRhBoU", "ImspTQPwCqd", "202212", "3"));
    validateRow(
        response, 74, List.of("REF_ACT_STOCKOUT", "uilaJSyXt7d", "ImspTQPwCqd", "202209", "3"));
    validateRow(
        response, 75, List.of("REF_ACT_STOCKOUT", "RkbOhHwiOgW", "ImspTQPwCqd", "202212", "2"));
    validateRow(
        response, 76, List.of("REF_ACT_STOCKOUT", "yrwgRxRhBoU", "ImspTQPwCqd", "202211", "2"));
    validateRow(
        response, 77, List.of("REF_ACT_STOCKOUT", "B3nxOazOO2G", "ImspTQPwCqd", "202211", "1"));
    validateRow(
        response, 78, List.of("COMB_BLISTER", "C6nZpLKjEJr", "ImspTQPwCqd", "202212", "13"));
    validateRow(
        response, 79, List.of("COMB_BLISTER", "e5YBV5F5iUd", "ImspTQPwCqd", "202208", "12"));
    validateRow(
        response, 80, List.of("COMB_BLISTER", "VLFVaH1MwnF", "ImspTQPwCqd", "202207", "12"));
    validateRow(
        response, 81, List.of("COMB_BLISTER", "e5YBV5F5iUd", "ImspTQPwCqd", "202211", "11"));
    validateRow(
        response, 82, List.of("COMB_BLISTER", "CW81uF03hvV", "ImspTQPwCqd", "202207", "11"));
    validateRow(
        response, 83, List.of("COMB_BLISTER", "g3bcPGD5Q5i", "ImspTQPwCqd", "202207", "11"));
    validateRow(
        response, 84, List.of("COMB_BLISTER", "B3nxOazOO2G", "ImspTQPwCqd", "202211", "10"));
    validateRow(
        response, 85, List.of("COMB_BLISTER", "CW81uF03hvV", "ImspTQPwCqd", "202208", "10"));
    validateRow(
        response, 86, List.of("COMB_BLISTER", "g3bcPGD5Q5i", "ImspTQPwCqd", "202211", "10"));
    validateRow(
        response, 87, List.of("COMB_BLISTER", "VLFVaH1MwnF", "ImspTQPwCqd", "202210", "10"));
    validateRow(
        response, 88, List.of("COMB_BLISTER", "XK6u6cJCR0t", "ImspTQPwCqd", "202210", "10"));
    validateRow(response, 89, List.of("COMB_BLISTER", "VLFVaH1MwnF", "ImspTQPwCqd", "202209", "9"));
    validateRow(response, 90, List.of("COMB_BLISTER", "yfWXlxYNbhy", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, 91, List.of("COMB_BLISTER", "hERJraxV8D9", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, 92, List.of("COMB_BLISTER", "yrwgRxRhBoU", "ImspTQPwCqd", "202209", "9"));
    validateRow(response, 93, List.of("COMB_BLISTER", "yrwgRxRhBoU", "ImspTQPwCqd", "202210", "9"));
    validateRow(response, 94, List.of("COMB_BLISTER", "TY5rBQzlBRa", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, 95, List.of("COMB_BLISTER", "g3bcPGD5Q5i", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, 96, List.of("COMB_BLISTER", "RkbOhHwiOgW", "ImspTQPwCqd", "202209", "8"));
    validateRow(response, 97, List.of("COMB_BLISTER", "RkbOhHwiOgW", "ImspTQPwCqd", "202212", "8"));
    validateRow(response, 98, List.of("COMB_BLISTER", "RkbOhHwiOgW", "ImspTQPwCqd", "202210", "8"));
    validateRow(response, 99, List.of("COMB_BLISTER", "yrwgRxRhBoU", "ImspTQPwCqd", "202207", "8"));
    validateRow(
        response, 100, List.of("COMB_BLISTER", "RkbOhHwiOgW", "ImspTQPwCqd", "202208", "8"));
    validateRow(
        response, 101, List.of("COMB_BLISTER", "e5YBV5F5iUd", "ImspTQPwCqd", "202207", "8"));
    validateRow(
        response, 102, List.of("COMB_BLISTER", "uilaJSyXt7d", "ImspTQPwCqd", "202211", "8"));
    validateRow(
        response, 103, List.of("COMB_BLISTER", "TY5rBQzlBRa", "ImspTQPwCqd", "202211", "8"));
    validateRow(
        response, 104, List.of("COMB_BLISTER", "uilaJSyXt7d", "ImspTQPwCqd", "202212", "8"));
    validateRow(
        response, 105, List.of("COMB_BLISTER", "VLFVaH1MwnF", "ImspTQPwCqd", "202212", "8"));
    validateRow(
        response, 106, List.of("COMB_BLISTER", "g3bcPGD5Q5i", "ImspTQPwCqd", "202210", "7"));
    validateRow(
        response, 107, List.of("COMB_BLISTER", "VLFVaH1MwnF", "ImspTQPwCqd", "202211", "7"));
    validateRow(
        response, 108, List.of("COMB_BLISTER", "C6nZpLKjEJr", "ImspTQPwCqd", "202210", "7"));
    validateRow(
        response, 109, List.of("COMB_BLISTER", "yfWXlxYNbhy", "ImspTQPwCqd", "202210", "7"));
    validateRow(
        response, 110, List.of("COMB_BLISTER", "XK6u6cJCR0t", "ImspTQPwCqd", "202211", "7"));
    validateRow(
        response, 111, List.of("COMB_BLISTER", "CW81uF03hvV", "ImspTQPwCqd", "202210", "7"));
    validateRow(
        response, 112, List.of("COMB_BLISTER", "TY5rBQzlBRa", "ImspTQPwCqd", "202209", "7"));
    validateRow(
        response, 113, List.of("COMB_BLISTER", "uilaJSyXt7d", "ImspTQPwCqd", "202209", "7"));
    validateRow(
        response, 114, List.of("COMB_BLISTER", "yrwgRxRhBoU", "ImspTQPwCqd", "202208", "7"));
    validateRow(
        response, 115, List.of("COMB_BLISTER", "RkbOhHwiOgW", "ImspTQPwCqd", "202211", "7"));
    validateRow(
        response, 116, List.of("COMB_BLISTER", "XK6u6cJCR0t", "ImspTQPwCqd", "202212", "7"));
    validateRow(
        response, 117, List.of("COMB_BLISTER", "hERJraxV8D9", "ImspTQPwCqd", "202209", "6"));
    validateRow(
        response, 118, List.of("COMB_BLISTER", "e5YBV5F5iUd", "ImspTQPwCqd", "202210", "6"));
    validateRow(
        response, 119, List.of("COMB_BLISTER", "yfWXlxYNbhy", "ImspTQPwCqd", "202211", "6"));
    validateRow(
        response, 120, List.of("COMB_BLISTER", "C6nZpLKjEJr", "ImspTQPwCqd", "202211", "6"));
    validateRow(
        response, 121, List.of("COMB_BLISTER", "C6nZpLKjEJr", "ImspTQPwCqd", "202208", "6"));
    validateRow(
        response, 122, List.of("COMB_BLISTER", "TY5rBQzlBRa", "ImspTQPwCqd", "202212", "6"));
    validateRow(
        response, 123, List.of("COMB_BLISTER", "uilaJSyXt7d", "ImspTQPwCqd", "202208", "6"));
    validateRow(
        response, 124, List.of("COMB_BLISTER", "B3nxOazOO2G", "ImspTQPwCqd", "202207", "6"));
    validateRow(
        response, 125, List.of("COMB_BLISTER", "B3nxOazOO2G", "ImspTQPwCqd", "202210", "6"));
    validateRow(
        response, 126, List.of("COMB_BLISTER", "uilaJSyXt7d", "ImspTQPwCqd", "202207", "6"));
    validateRow(
        response, 127, List.of("COMB_BLISTER", "RkbOhHwiOgW", "ImspTQPwCqd", "202207", "6"));
    validateRow(
        response, 128, List.of("COMB_BLISTER", "C6nZpLKjEJr", "ImspTQPwCqd", "202209", "6"));
    validateRow(
        response, 129, List.of("COMB_BLISTER", "XK6u6cJCR0t", "ImspTQPwCqd", "202209", "5"));
    validateRow(
        response, 130, List.of("COMB_BLISTER", "B3nxOazOO2G", "ImspTQPwCqd", "202208", "5"));
    validateRow(
        response, 131, List.of("COMB_BLISTER", "yfWXlxYNbhy", "ImspTQPwCqd", "202212", "5"));
    validateRow(
        response, 132, List.of("COMB_BLISTER", "g3bcPGD5Q5i", "ImspTQPwCqd", "202212", "5"));
    validateRow(
        response, 133, List.of("COMB_BLISTER", "g3bcPGD5Q5i", "ImspTQPwCqd", "202209", "5"));
    validateRow(
        response, 134, List.of("COMB_BLISTER", "XK6u6cJCR0t", "ImspTQPwCqd", "202207", "5"));
    validateRow(
        response, 135, List.of("COMB_BLISTER", "yfWXlxYNbhy", "ImspTQPwCqd", "202209", "5"));
    validateRow(
        response, 136, List.of("COMB_BLISTER", "XK6u6cJCR0t", "ImspTQPwCqd", "202208", "5"));
    validateRow(
        response, 137, List.of("COMB_BLISTER", "yrwgRxRhBoU", "ImspTQPwCqd", "202212", "5"));
    validateRow(
        response, 138, List.of("COMB_BLISTER", "hERJraxV8D9", "ImspTQPwCqd", "202212", "5"));
    validateRow(
        response, 139, List.of("COMB_BLISTER", "B3nxOazOO2G", "ImspTQPwCqd", "202212", "5"));
    validateRow(
        response, 140, List.of("COMB_BLISTER", "VLFVaH1MwnF", "ImspTQPwCqd", "202208", "5"));
    validateRow(
        response, 141, List.of("COMB_BLISTER", "hERJraxV8D9", "ImspTQPwCqd", "202208", "5"));
    validateRow(
        response, 142, List.of("COMB_BLISTER", "hERJraxV8D9", "ImspTQPwCqd", "202207", "4"));
    validateRow(
        response, 143, List.of("COMB_BLISTER", "yfWXlxYNbhy", "ImspTQPwCqd", "202208", "4"));
    validateRow(
        response, 144, List.of("COMB_BLISTER", "e5YBV5F5iUd", "ImspTQPwCqd", "202209", "4"));
    validateRow(
        response, 145, List.of("COMB_BLISTER", "CW81uF03hvV", "ImspTQPwCqd", "202209", "4"));
    validateRow(
        response, 146, List.of("COMB_BLISTER", "CW81uF03hvV", "ImspTQPwCqd", "202212", "4"));
    validateRow(
        response, 147, List.of("COMB_BLISTER", "yrwgRxRhBoU", "ImspTQPwCqd", "202211", "4"));
    validateRow(
        response, 148, List.of("COMB_BLISTER", "C6nZpLKjEJr", "ImspTQPwCqd", "202207", "4"));
    validateRow(
        response, 149, List.of("COMB_BLISTER", "TY5rBQzlBRa", "ImspTQPwCqd", "202210", "4"));
    validateRow(
        response, 150, List.of("COMB_BLISTER", "e5YBV5F5iUd", "ImspTQPwCqd", "202212", "4"));
    validateRow(
        response, 151, List.of("COMB_BLISTER", "TY5rBQzlBRa", "ImspTQPwCqd", "202208", "4"));
    validateRow(
        response, 152, List.of("COMB_BLISTER", "hERJraxV8D9", "ImspTQPwCqd", "202210", "3"));
    validateRow(
        response, 153, List.of("COMB_BLISTER", "uilaJSyXt7d", "ImspTQPwCqd", "202210", "3"));
    validateRow(
        response, 154, List.of("COMB_BLISTER", "CW81uF03hvV", "ImspTQPwCqd", "202211", "2"));
    validateRow(
        response, 155, List.of("COMB_BLISTER", "B3nxOazOO2G", "ImspTQPwCqd", "202209", "2"));
    validateRow(response, 156, List.of("ACT6x2", "yrwgRxRhBoU", "ImspTQPwCqd", "202209", "16"));
    validateRow(response, 157, List.of("ACT6x2", "B3nxOazOO2G", "ImspTQPwCqd", "202209", "14"));
    validateRow(response, 158, List.of("ACT6x2", "g3bcPGD5Q5i", "ImspTQPwCqd", "202209", "14"));
    validateRow(response, 159, List.of("ACT6x2", "XK6u6cJCR0t", "ImspTQPwCqd", "202207", "12"));
    validateRow(response, 160, List.of("ACT6x2", "g3bcPGD5Q5i", "ImspTQPwCqd", "202210", "12"));
    validateRow(response, 161, List.of("ACT6x2", "yfWXlxYNbhy", "ImspTQPwCqd", "202212", "10"));
    validateRow(response, 162, List.of("ACT6x2", "TY5rBQzlBRa", "ImspTQPwCqd", "202208", "9"));
    validateRow(response, 163, List.of("ACT6x2", "CW81uF03hvV", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, 164, List.of("ACT6x2", "C6nZpLKjEJr", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, 165, List.of("ACT6x2", "C6nZpLKjEJr", "ImspTQPwCqd", "202212", "9"));
    validateRow(response, 166, List.of("ACT6x2", "VLFVaH1MwnF", "ImspTQPwCqd", "202212", "9"));
    validateRow(response, 167, List.of("ACT6x2", "yrwgRxRhBoU", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, 168, List.of("ACT6x2", "uilaJSyXt7d", "ImspTQPwCqd", "202212", "9"));
    validateRow(response, 169, List.of("ACT6x2", "VLFVaH1MwnF", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, 170, List.of("ACT6x2", "hERJraxV8D9", "ImspTQPwCqd", "202209", "9"));
    validateRow(response, 171, List.of("ACT6x2", "hERJraxV8D9", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, 172, List.of("ACT6x2", "uilaJSyXt7d", "ImspTQPwCqd", "202209", "8"));
    validateRow(response, 173, List.of("ACT6x2", "e5YBV5F5iUd", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, 174, List.of("ACT6x2", "CW81uF03hvV", "ImspTQPwCqd", "202212", "8"));
    validateRow(response, 175, List.of("ACT6x2", "RkbOhHwiOgW", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, 176, List.of("ACT6x2", "uilaJSyXt7d", "ImspTQPwCqd", "202207", "8"));
    validateRow(response, 177, List.of("ACT6x2", "RkbOhHwiOgW", "ImspTQPwCqd", "202212", "8"));
    validateRow(response, 178, List.of("ACT6x2", "CW81uF03hvV", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, 179, List.of("ACT6x2", "g3bcPGD5Q5i", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, 180, List.of("ACT6x2", "e5YBV5F5iUd", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, 181, List.of("ACT6x2", "g3bcPGD5Q5i", "ImspTQPwCqd", "202208", "7"));
    validateRow(response, 182, List.of("ACT6x2", "RkbOhHwiOgW", "ImspTQPwCqd", "202207", "7"));
    validateRow(response, 183, List.of("ACT6x2", "XK6u6cJCR0t", "ImspTQPwCqd", "202212", "7"));
    validateRow(response, 184, List.of("ACT6x2", "uilaJSyXt7d", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, 185, List.of("ACT6x2", "B3nxOazOO2G", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, 186, List.of("ACT6x2", "VLFVaH1MwnF", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, 187, List.of("ACT6x2", "XK6u6cJCR0t", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, 188, List.of("ACT6x2", "uilaJSyXt7d", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, 189, List.of("ACT6x2", "g3bcPGD5Q5i", "ImspTQPwCqd", "202212", "6"));
    validateRow(response, 190, List.of("ACT6x2", "yfWXlxYNbhy", "ImspTQPwCqd", "202209", "6"));
    validateRow(response, 191, List.of("ACT6x2", "C6nZpLKjEJr", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, 192, List.of("ACT6x2", "B3nxOazOO2G", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, 193, List.of("ACT6x2", "yrwgRxRhBoU", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, 194, List.of("ACT6x2", "C6nZpLKjEJr", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, 195, List.of("ACT6x2", "hERJraxV8D9", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, 196, List.of("ACT6x2", "yrwgRxRhBoU", "ImspTQPwCqd", "202210", "6"));
    validateRow(response, 197, List.of("ACT6x2", "hERJraxV8D9", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, 198, List.of("ACT6x2", "CW81uF03hvV", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, 199, List.of("ACT6x2", "XK6u6cJCR0t", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, 200, List.of("ACT6x2", "C6nZpLKjEJr", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, 201, List.of("ACT6x2", "C6nZpLKjEJr", "ImspTQPwCqd", "202209", "6"));
    validateRow(response, 202, List.of("ACT6x2", "yfWXlxYNbhy", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, 203, List.of("ACT6x2", "g3bcPGD5Q5i", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, 204, List.of("ACT6x2", "yrwgRxRhBoU", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, 205, List.of("ACT6x2", "TY5rBQzlBRa", "ImspTQPwCqd", "202210", "5"));
    validateRow(response, 206, List.of("ACT6x2", "VLFVaH1MwnF", "ImspTQPwCqd", "202208", "5"));
    validateRow(response, 207, List.of("ACT6x2", "B3nxOazOO2G", "ImspTQPwCqd", "202208", "5"));
    validateRow(response, 208, List.of("ACT6x2", "e5YBV5F5iUd", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, 209, List.of("ACT6x2", "XK6u6cJCR0t", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, 210, List.of("ACT6x2", "VLFVaH1MwnF", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, 211, List.of("ACT6x2", "B3nxOazOO2G", "ImspTQPwCqd", "202207", "5"));
    validateRow(response, 212, List.of("ACT6x2", "TY5rBQzlBRa", "ImspTQPwCqd", "202211", "5"));
    validateRow(response, 213, List.of("ACT6x2", "VLFVaH1MwnF", "ImspTQPwCqd", "202211", "4"));
    validateRow(response, 214, List.of("ACT6x2", "yfWXlxYNbhy", "ImspTQPwCqd", "202211", "4"));
    validateRow(response, 215, List.of("ACT6x2", "RkbOhHwiOgW", "ImspTQPwCqd", "202211", "4"));
    validateRow(response, 216, List.of("ACT6x2", "uilaJSyXt7d", "ImspTQPwCqd", "202211", "4"));
    validateRow(response, 217, List.of("ACT6x2", "yfWXlxYNbhy", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, 218, List.of("ACT6x2", "CW81uF03hvV", "ImspTQPwCqd", "202210", "4"));
    validateRow(response, 219, List.of("ACT6x2", "hERJraxV8D9", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, 220, List.of("ACT6x2", "XK6u6cJCR0t", "ImspTQPwCqd", "202211", "4"));
    validateRow(response, 221, List.of("ACT6x2", "TY5rBQzlBRa", "ImspTQPwCqd", "202207", "4"));
    validateRow(response, 222, List.of("ACT6x2", "yfWXlxYNbhy", "ImspTQPwCqd", "202210", "4"));
    validateRow(response, 223, List.of("ACT6x2", "B3nxOazOO2G", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, 224, List.of("ACT6x2", "TY5rBQzlBRa", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, 225, List.of("ACT6x2", "CW81uF03hvV", "ImspTQPwCqd", "202209", "3"));
    validateRow(response, 226, List.of("ACT6x2", "RkbOhHwiOgW", "ImspTQPwCqd", "202210", "3"));
    validateRow(response, 227, List.of("ACT6x2", "TY5rBQzlBRa", "ImspTQPwCqd", "202209", "3"));
    validateRow(response, 228, List.of("ACT6x2", "e5YBV5F5iUd", "ImspTQPwCqd", "202212", "3"));
    validateRow(response, 229, List.of("ACT6x2", "yrwgRxRhBoU", "ImspTQPwCqd", "202211", "3"));
    validateRow(response, 230, List.of("ACT6x2", "e5YBV5F5iUd", "ImspTQPwCqd", "202211", "3"));
    validateRow(response, 231, List.of("ACT6x2", "e5YBV5F5iUd", "ImspTQPwCqd", "202207", "2"));
    validateRow(response, 232, List.of("ACT6x2", "RkbOhHwiOgW", "ImspTQPwCqd", "202209", "2"));
    validateRow(response, 233, List.of("ACT6x2", "hERJraxV8D9", "ImspTQPwCqd", "202210", "2"));
    validateRow(response, 234, List.of("ACT6x1", "C6nZpLKjEJr", "ImspTQPwCqd", "202210", "14"));
    validateRow(response, 235, List.of("ACT6x1", "C6nZpLKjEJr", "ImspTQPwCqd", "202207", "13"));
    validateRow(response, 236, List.of("ACT6x1", "e5YBV5F5iUd", "ImspTQPwCqd", "202209", "12"));
    validateRow(response, 237, List.of("ACT6x1", "TY5rBQzlBRa", "ImspTQPwCqd", "202209", "12"));
    validateRow(response, 238, List.of("ACT6x1", "yrwgRxRhBoU", "ImspTQPwCqd", "202210", "11"));
    validateRow(response, 239, List.of("ACT6x1", "g3bcPGD5Q5i", "ImspTQPwCqd", "202210", "10"));
    validateRow(response, 240, List.of("ACT6x1", "TY5rBQzlBRa", "ImspTQPwCqd", "202208", "10"));
    validateRow(response, 241, List.of("ACT6x1", "yfWXlxYNbhy", "ImspTQPwCqd", "202207", "10"));
    validateRow(response, 242, List.of("ACT6x1", "RkbOhHwiOgW", "ImspTQPwCqd", "202211", "10"));
    validateRow(response, 243, List.of("ACT6x1", "uilaJSyXt7d", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, 244, List.of("ACT6x1", "yrwgRxRhBoU", "ImspTQPwCqd", "202208", "9"));
    validateRow(response, 245, List.of("ACT6x1", "uilaJSyXt7d", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, 246, List.of("ACT6x1", "VLFVaH1MwnF", "ImspTQPwCqd", "202207", "9"));
    validateRow(response, 247, List.of("ACT6x1", "B3nxOazOO2G", "ImspTQPwCqd", "202212", "9"));
    validateRow(response, 248, List.of("ACT6x1", "TY5rBQzlBRa", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, 249, List.of("ACT6x1", "B3nxOazOO2G", "ImspTQPwCqd", "202211", "9"));
    validateRow(response, 250, List.of("ACT6x1", "e5YBV5F5iUd", "ImspTQPwCqd", "202208", "9"));
    validateRow(response, 251, List.of("ACT6x1", "CW81uF03hvV", "ImspTQPwCqd", "202207", "8"));
    validateRow(response, 252, List.of("ACT6x1", "B3nxOazOO2G", "ImspTQPwCqd", "202210", "8"));
    validateRow(response, 253, List.of("ACT6x1", "g3bcPGD5Q5i", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, 254, List.of("ACT6x1", "C6nZpLKjEJr", "ImspTQPwCqd", "202211", "8"));
    validateRow(response, 255, List.of("ACT6x1", "VLFVaH1MwnF", "ImspTQPwCqd", "202210", "8"));
    validateRow(response, 256, List.of("ACT6x1", "yfWXlxYNbhy", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, 257, List.of("ACT6x1", "TY5rBQzlBRa", "ImspTQPwCqd", "202207", "8"));
    validateRow(response, 258, List.of("ACT6x1", "hERJraxV8D9", "ImspTQPwCqd", "202208", "8"));
    validateRow(response, 259, List.of("ACT6x1", "CW81uF03hvV", "ImspTQPwCqd", "202212", "8"));
    validateRow(response, 260, List.of("ACT6x1", "g3bcPGD5Q5i", "ImspTQPwCqd", "202207", "7"));
    validateRow(response, 261, List.of("ACT6x1", "XK6u6cJCR0t", "ImspTQPwCqd", "202209", "7"));
    validateRow(response, 262, List.of("ACT6x1", "e5YBV5F5iUd", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, 263, List.of("ACT6x1", "XK6u6cJCR0t", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, 264, List.of("ACT6x1", "TY5rBQzlBRa", "ImspTQPwCqd", "202212", "7"));
    validateRow(response, 265, List.of("ACT6x1", "C6nZpLKjEJr", "ImspTQPwCqd", "202208", "7"));
    validateRow(response, 266, List.of("ACT6x1", "yrwgRxRhBoU", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, 267, List.of("ACT6x1", "e5YBV5F5iUd", "ImspTQPwCqd", "202211", "7"));
    validateRow(response, 268, List.of("ACT6x1", "hERJraxV8D9", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, 269, List.of("ACT6x1", "g3bcPGD5Q5i", "ImspTQPwCqd", "202209", "7"));
    validateRow(response, 270, List.of("ACT6x1", "XK6u6cJCR0t", "ImspTQPwCqd", "202207", "7"));
    validateRow(response, 271, List.of("ACT6x1", "yrwgRxRhBoU", "ImspTQPwCqd", "202212", "7"));
    validateRow(response, 272, List.of("ACT6x1", "yfWXlxYNbhy", "ImspTQPwCqd", "202209", "7"));
    validateRow(response, 273, List.of("ACT6x1", "hERJraxV8D9", "ImspTQPwCqd", "202209", "7"));
    validateRow(response, 274, List.of("ACT6x1", "uilaJSyXt7d", "ImspTQPwCqd", "202210", "7"));
    validateRow(response, 275, List.of("ACT6x1", "e5YBV5F5iUd", "ImspTQPwCqd", "202212", "6"));
    validateRow(response, 276, List.of("ACT6x1", "XK6u6cJCR0t", "ImspTQPwCqd", "202212", "6"));
    validateRow(response, 277, List.of("ACT6x1", "uilaJSyXt7d", "ImspTQPwCqd", "202208", "6"));
    validateRow(response, 278, List.of("ACT6x1", "RkbOhHwiOgW", "ImspTQPwCqd", "202207", "6"));
    validateRow(response, 279, List.of("ACT6x1", "B3nxOazOO2G", "ImspTQPwCqd", "202209", "6"));
    validateRow(response, 280, List.of("ACT6x1", "CW81uF03hvV", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, 281, List.of("ACT6x1", "hERJraxV8D9", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, 282, List.of("ACT6x1", "g3bcPGD5Q5i", "ImspTQPwCqd", "202211", "6"));
    validateRow(response, 283, List.of("ACT6x1", "VLFVaH1MwnF", "ImspTQPwCqd", "202208", "5"));
    validateRow(response, 284, List.of("ACT6x1", "TY5rBQzlBRa", "ImspTQPwCqd", "202210", "5"));
    validateRow(response, 285, List.of("ACT6x1", "C6nZpLKjEJr", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, 286, List.of("ACT6x1", "VLFVaH1MwnF", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, 287, List.of("ACT6x1", "RkbOhHwiOgW", "ImspTQPwCqd", "202208", "5"));
    validateRow(response, 288, List.of("ACT6x1", "uilaJSyXt7d", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, 289, List.of("ACT6x1", "CW81uF03hvV", "ImspTQPwCqd", "202210", "5"));
    validateRow(response, 290, List.of("ACT6x1", "VLFVaH1MwnF", "ImspTQPwCqd", "202211", "5"));
    validateRow(response, 291, List.of("ACT6x1", "e5YBV5F5iUd", "ImspTQPwCqd", "202207", "5"));
    validateRow(response, 292, List.of("ACT6x1", "CW81uF03hvV", "ImspTQPwCqd", "202209", "5"));
    validateRow(response, 293, List.of("ACT6x1", "hERJraxV8D9", "ImspTQPwCqd", "202212", "5"));
    validateRow(response, 294, List.of("ACT6x1", "CW81uF03hvV", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, 295, List.of("ACT6x1", "hERJraxV8D9", "ImspTQPwCqd", "202207", "4"));
    validateRow(response, 296, List.of("ACT6x1", "C6nZpLKjEJr", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, 297, List.of("ACT6x1", "XK6u6cJCR0t", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, 298, List.of("ACT6x1", "yrwgRxRhBoU", "ImspTQPwCqd", "202209", "4"));
    validateRow(response, 299, List.of("ACT6x1", "uilaJSyXt7d", "ImspTQPwCqd", "202212", "4"));
    validateRow(response, 300, List.of("ACT6x1", "RkbOhHwiOgW", "ImspTQPwCqd", "202209", "4"));
    validateRow(response, 301, List.of("ACT6x1", "B3nxOazOO2G", "ImspTQPwCqd", "202208", "4"));
    validateRow(response, 302, List.of("ACT6x1", "yfWXlxYNbhy", "ImspTQPwCqd", "202210", "4"));
    validateRow(response, 303, List.of("ACT6x1", "RkbOhHwiOgW", "ImspTQPwCqd", "202210", "3"));
    validateRow(response, 304, List.of("ACT6x1", "XK6u6cJCR0t", "ImspTQPwCqd", "202211", "3"));
    validateRow(response, 305, List.of("ACT6x1", "yfWXlxYNbhy", "ImspTQPwCqd", "202212", "3"));
    validateRow(response, 306, List.of("ACT6x1", "yfWXlxYNbhy", "ImspTQPwCqd", "202211", "3"));
    validateRow(response, 307, List.of("ACT6x1", "VLFVaH1MwnF", "ImspTQPwCqd", "202209", "3"));
    validateRow(response, 308, List.of("ACT6x1", "B3nxOazOO2G", "ImspTQPwCqd", "202207", "2"));
    validateRow(response, 309, List.of("ACT6x1", "yrwgRxRhBoU", "ImspTQPwCqd", "202207", "1"));
    validateRow(response, 310, List.of("ACT6x1", "g3bcPGD5Q5i", "ImspTQPwCqd", "202212", "1"));
    validateRow(response, 311, List.of("ACT6x1", "RkbOhHwiOgW", "ImspTQPwCqd", "202212", "1"));
  }
}
