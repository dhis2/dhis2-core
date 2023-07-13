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
public class EventsAggregate6AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void queryMalariaCaseCountLast7DaysByAgeGroupAndGender() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=pTo4uMt3xur")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:ImspTQPwCqd,pe:LAST_7_DAYS;THIS_MONTH,pTo4uMt3xur.qrur9Dvnyt5-Yf6UHoPkdS6,pTo4uMt3xur.oZg33kd9taw")
            .add("relativePeriodDate=2023-01-01");

    // When
    ApiResponse response = actions.aggregate().get("VBqh0ynB2wv", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(93)))
        .body("height", equalTo(93))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"OyVUzWsX8UF\":{\"name\":\"10 - 20\"},\"qrur9Dvnyt5\":{\"name\":\"Age in years\"},\"pZzk1L4Blf1\":{\"name\":\"0 - 10\"},\"202301\":{\"name\":\"January 2023\"},\"THIS_MONTH\":{\"name\":\"This month\"},\"b7MCpzqJaR2\":{\"name\":\"70 - 80\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"Tq4NYCn9eNH\":{\"name\":\"60 - 70\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"ou\":{\"name\":\"Organisation unit\"},\"CpP5yzbgfHo\":{\"name\":\"40 - 50\"},\"20221229\":{\"name\":\"2022-12-29\"},\"20221228\":{\"name\":\"2022-12-28\"},\"LAST_7_DAYS\":{\"name\":\"Last 7 days\"},\"20221227\":{\"name\":\"2022-12-27\"},\"20221226\":{\"name\":\"2022-12-26\"},\"20221225\":{\"name\":\"2022-12-25\"},\"scvmgP9F9rn\":{\"name\":\"90 - 100\"},\"cbPqyIAFw9u\":{\"name\":\"50 - 60\"},\"TvM2MQgD7Jd\":{\"name\":\"20 - 30\"},\"20221231\":{\"name\":\"2022-12-31\"},\"20221230\":{\"name\":\"2022-12-30\"},\"pe\":{\"name\":\"Period\"},\"VBqh0ynB2wv\":{\"name\":\"Malaria case registration\"},\"puI3YpLJ3fC\":{\"name\":\"80 - 90\"},\"ZUUGJnvX40X\":{\"name\":\"30 - 40\"},\"oZg33kd9taw\":{\"name\":\"Gender\"},\"pTo4uMt3xur\":{\"name\":\"Malaria case registration\"}},\"dimensions\":{\"pe\":[\"20221225\",\"20221226\",\"20221227\",\"20221228\",\"20221229\",\"20221230\",\"20221231\",\"202301\"],\"ou\":[\"ImspTQPwCqd\"],\"oZg33kd9taw\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"pTo4uMt3xur.qrur9Dvnyt5\":[\"pZzk1L4Blf1\",\"OyVUzWsX8UF\",\"TvM2MQgD7Jd\",\"ZUUGJnvX40X\",\"CpP5yzbgfHo\",\"cbPqyIAFw9u\",\"Tq4NYCn9eNH\",\"b7MCpzqJaR2\",\"puI3YpLJ3fC\",\"scvmgP9F9rn\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "oZg33kd9taw", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "qrur9Dvnyt5", "Age in years", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("Male", "OyVUzWsX8UF", "ImspTQPwCqd", "20221225", "30"));
    validateRow(response, 1, List.of("Male", "cbPqyIAFw9u", "ImspTQPwCqd", "20221226", "28"));
    validateRow(response, 2, List.of("Male", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221230", "27"));
    validateRow(response, 3, List.of("Male", "cbPqyIAFw9u", "ImspTQPwCqd", "20221230", "26"));
    validateRow(response, 4, List.of("Male", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221228", "26"));
    validateRow(response, 5, List.of("Male", "OyVUzWsX8UF", "ImspTQPwCqd", "20221230", "24"));
    validateRow(response, 6, List.of("Male", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221229", "24"));
    validateRow(response, 7, List.of("Male", "pZzk1L4Blf1", "ImspTQPwCqd", "20221228", "23"));
    validateRow(response, 8, List.of("Male", "ZUUGJnvX40X", "ImspTQPwCqd", "20221225", "22"));
    validateRow(response, 9, List.of("Male", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221229", "21"));
    validateRow(response, 10, List.of("Male", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221226", "21"));
    validateRow(response, 11, List.of("Male", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221227", "21"));
    validateRow(response, 12, List.of("Male", "cbPqyIAFw9u", "ImspTQPwCqd", "20221228", "20"));
    validateRow(response, 13, List.of("Male", "CpP5yzbgfHo", "ImspTQPwCqd", "20221225", "20"));
    validateRow(response, 14, List.of("Male", "CpP5yzbgfHo", "ImspTQPwCqd", "20221228", "20"));
    validateRow(response, 15, List.of("Male", "cbPqyIAFw9u", "ImspTQPwCqd", "20221225", "19"));
    validateRow(response, 16, List.of("Male", "CpP5yzbgfHo", "ImspTQPwCqd", "20221227", "19"));
    validateRow(response, 17, List.of("Male", "cbPqyIAFw9u", "ImspTQPwCqd", "20221229", "19"));
    validateRow(response, 18, List.of("Male", "pZzk1L4Blf1", "ImspTQPwCqd", "20221230", "18"));
    validateRow(response, 19, List.of("Male", "pZzk1L4Blf1", "ImspTQPwCqd", "20221229", "18"));
    validateRow(response, 20, List.of("Male", "CpP5yzbgfHo", "ImspTQPwCqd", "20221230", "18"));
    validateRow(response, 21, List.of("Male", "cbPqyIAFw9u", "ImspTQPwCqd", "20221227", "17"));
    validateRow(response, 22, List.of("Male", "pZzk1L4Blf1", "ImspTQPwCqd", "20221227", "17"));
    validateRow(response, 23, List.of("Male", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221230", "17"));
    validateRow(response, 24, List.of("Male", "pZzk1L4Blf1", "ImspTQPwCqd", "20221226", "17"));
    validateRow(response, 25, List.of("Male", "ZUUGJnvX40X", "ImspTQPwCqd", "20221228", "16"));
    validateRow(response, 26, List.of("Male", "OyVUzWsX8UF", "ImspTQPwCqd", "20221226", "16"));
    validateRow(response, 27, List.of("Male", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221228", "16"));
    validateRow(response, 28, List.of("Male", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221227", "16"));
    validateRow(response, 29, List.of("Male", "CpP5yzbgfHo", "ImspTQPwCqd", "20221229", "16"));
    validateRow(response, 30, List.of("Male", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221225", "16"));
    validateRow(response, 31, List.of("Male", "OyVUzWsX8UF", "ImspTQPwCqd", "20221227", "15"));
    validateRow(response, 32, List.of("Male", "ZUUGJnvX40X", "ImspTQPwCqd", "20221229", "15"));
    validateRow(response, 33, List.of("Male", "ZUUGJnvX40X", "ImspTQPwCqd", "20221227", "14"));
    validateRow(response, 34, List.of("Male", "ZUUGJnvX40X", "ImspTQPwCqd", "20221230", "14"));
    validateRow(response, 35, List.of("Male", "OyVUzWsX8UF", "ImspTQPwCqd", "20221229", "14"));
    validateRow(response, 36, List.of("Male", "ZUUGJnvX40X", "ImspTQPwCqd", "20221226", "13"));
    validateRow(response, 37, List.of("Male", "CpP5yzbgfHo", "ImspTQPwCqd", "20221226", "13"));
    validateRow(response, 38, List.of("Male", "pZzk1L4Blf1", "ImspTQPwCqd", "20221225", "12"));
    validateRow(response, 39, List.of("Male", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221225", "11"));
    validateRow(response, 40, List.of("Male", "OyVUzWsX8UF", "ImspTQPwCqd", "20221228", "11"));
    validateRow(response, 41, List.of("Male", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221226", "9"));
    validateRow(response, 42, List.of("Male", "b7MCpzqJaR2", "ImspTQPwCqd", "20221226", "4"));
    validateRow(response, 43, List.of("Male", "b7MCpzqJaR2", "ImspTQPwCqd", "20221225", "3"));
    validateRow(response, 44, List.of("Male", "b7MCpzqJaR2", "ImspTQPwCqd", "20221229", "2"));
    validateRow(response, 45, List.of("Male", "b7MCpzqJaR2", "ImspTQPwCqd", "20221228", "1"));
    validateRow(response, 46, List.of("Male", "b7MCpzqJaR2", "ImspTQPwCqd", "20221227", "1"));
    validateRow(response, 47, List.of("Female", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221229", "31"));
    validateRow(response, 48, List.of("Female", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221229", "30"));
    validateRow(response, 49, List.of("Female", "ZUUGJnvX40X", "ImspTQPwCqd", "20221229", "28"));
    validateRow(response, 50, List.of("Female", "OyVUzWsX8UF", "ImspTQPwCqd", "20221226", "26"));
    validateRow(response, 51, List.of("Female", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221228", "25"));
    validateRow(response, 52, List.of("Female", "CpP5yzbgfHo", "ImspTQPwCqd", "20221225", "24"));
    validateRow(response, 53, List.of("Female", "ZUUGJnvX40X", "ImspTQPwCqd", "20221225", "24"));
    validateRow(response, 54, List.of("Female", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221225", "24"));
    validateRow(response, 55, List.of("Female", "OyVUzWsX8UF", "ImspTQPwCqd", "20221229", "23"));
    validateRow(response, 56, List.of("Female", "pZzk1L4Blf1", "ImspTQPwCqd", "20221227", "23"));
    validateRow(response, 57, List.of("Female", "cbPqyIAFw9u", "ImspTQPwCqd", "20221225", "23"));
    validateRow(response, 58, List.of("Female", "cbPqyIAFw9u", "ImspTQPwCqd", "20221229", "22"));
    validateRow(response, 59, List.of("Female", "cbPqyIAFw9u", "ImspTQPwCqd", "20221227", "21"));
    validateRow(response, 60, List.of("Female", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221227", "21"));
    validateRow(response, 61, List.of("Female", "CpP5yzbgfHo", "ImspTQPwCqd", "20221229", "20"));
    validateRow(response, 62, List.of("Female", "pZzk1L4Blf1", "ImspTQPwCqd", "20221230", "20"));
    validateRow(response, 63, List.of("Female", "OyVUzWsX8UF", "ImspTQPwCqd", "20221230", "20"));
    validateRow(response, 64, List.of("Female", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221227", "20"));
    validateRow(response, 65, List.of("Female", "OyVUzWsX8UF", "ImspTQPwCqd", "20221225", "19"));
    validateRow(response, 66, List.of("Female", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221228", "19"));
    validateRow(response, 67, List.of("Female", "OyVUzWsX8UF", "ImspTQPwCqd", "20221227", "19"));
    validateRow(response, 68, List.of("Female", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221225", "19"));
    validateRow(response, 69, List.of("Female", "OyVUzWsX8UF", "ImspTQPwCqd", "20221228", "19"));
    validateRow(response, 70, List.of("Female", "ZUUGJnvX40X", "ImspTQPwCqd", "20221230", "19"));
    validateRow(response, 71, List.of("Female", "pZzk1L4Blf1", "ImspTQPwCqd", "20221225", "19"));
    validateRow(response, 72, List.of("Female", "CpP5yzbgfHo", "ImspTQPwCqd", "20221228", "18"));
    validateRow(response, 73, List.of("Female", "CpP5yzbgfHo", "ImspTQPwCqd", "20221227", "18"));
    validateRow(response, 74, List.of("Female", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221230", "18"));
    validateRow(response, 75, List.of("Female", "CpP5yzbgfHo", "ImspTQPwCqd", "20221226", "18"));
    validateRow(response, 76, List.of("Female", "ZUUGJnvX40X", "ImspTQPwCqd", "20221226", "18"));
    validateRow(response, 77, List.of("Female", "pZzk1L4Blf1", "ImspTQPwCqd", "20221226", "18"));
    validateRow(response, 78, List.of("Female", "ZUUGJnvX40X", "ImspTQPwCqd", "20221227", "17"));
    validateRow(response, 79, List.of("Female", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221230", "17"));
    validateRow(response, 80, List.of("Female", "cbPqyIAFw9u", "ImspTQPwCqd", "20221228", "17"));
    validateRow(response, 81, List.of("Female", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221226", "17"));
    validateRow(response, 82, List.of("Female", "pZzk1L4Blf1", "ImspTQPwCqd", "20221228", "16"));
    validateRow(response, 83, List.of("Female", "CpP5yzbgfHo", "ImspTQPwCqd", "20221230", "16"));
    validateRow(response, 84, List.of("Female", "ZUUGJnvX40X", "ImspTQPwCqd", "20221228", "15"));
    validateRow(response, 85, List.of("Female", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221226", "13"));
    validateRow(response, 86, List.of("Female", "cbPqyIAFw9u", "ImspTQPwCqd", "20221226", "13"));
    validateRow(response, 87, List.of("Female", "pZzk1L4Blf1", "ImspTQPwCqd", "20221229", "10"));
    validateRow(response, 88, List.of("Female", "cbPqyIAFw9u", "ImspTQPwCqd", "20221230", "10"));
    validateRow(response, 89, List.of("Female", "b7MCpzqJaR2", "ImspTQPwCqd", "20221229", "4"));
    validateRow(response, 90, List.of("Female", "b7MCpzqJaR2", "ImspTQPwCqd", "20221230", "4"));
    validateRow(response, 91, List.of("Female", "b7MCpzqJaR2", "ImspTQPwCqd", "20221228", "3"));
    validateRow(response, 92, List.of("Female", "b7MCpzqJaR2", "ImspTQPwCqd", "20221226", "1"));
  }

  @Test
  public void queryMalariaRdtTestResultByFundingAgencyLast12Months() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=kO3z4Dhc038")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS,kO3z4Dhc038.hKZh1et5n7v,SooXFOUnciJ:OK2Nr4wdfrZ;B0bjKC0szQX")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("bMcwwoVnbSR", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(36)))
        .body("height", equalTo(36))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"SooXFOUnciJ\":{\"name\":\"Funding Agency\"},\"202208\":{\"name\":\"August 2022\"},\"uilaJSyXt7d\":{\"name\":\"World Vision\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"202207\":{\"name\":\"July 2022\"},\"202306\":{\"name\":\"June 2023\"},\"VLFVaH1MwnF\":{\"name\":\"Pathfinder International\"},\"202303\":{\"name\":\"March 2023\"},\"CW81uF03hvV\":{\"name\":\"AIDSRelief Consortium\"},\"202304\":{\"name\":\"April 2023\"},\"hERJraxV8D9\":{\"name\":\"Hope Worldwide\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"TY5rBQzlBRa\":{\"name\":\"Family Health International\"},\"202211\":{\"name\":\"November 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202212\":{\"name\":\"December 2022\"},\"202210\":{\"name\":\"October 2022\"},\"XK6u6cJCR0t\":{\"name\":\"Population Services International\"},\"GpyQudG21w1\":{\"code\":\"POS\",\"name\":\"Positive\"},\"B3nxOazOO2G\":{\"name\":\"APHIAplus\"},\"Hw5p0lACaxV\":{\"code\":\"MIX\",\"name\":\"Mixed\"},\"hKZh1et5n7v\":{\"name\":\"RDT test result\"},\"RkbOhHwiOgW\":{\"name\":\"CARE International\"},\"ou\":{\"name\":\"Organisation unit\"},\"ZLBj3xO8sZ4\":{\"code\":\"NEG\",\"name\":\"Negative\"},\"OK2Nr4wdfrZ\":{\"name\":\"CDC\"},\"LFsZ8v5v7rq\":{},\"g3bcPGD5Q5i\":{\"name\":\"International Rescue Committee\"},\"yrwgRxRhBoU\":{\"name\":\"Path\"},\"yfWXlxYNbhy\":{\"name\":\"IntraHealth International\"},\"e5YBV5F5iUd\":{\"name\":\"Plan International\"},\"LEWNFo4Qrrs\":{\"name\":\"World Concern\"},\"pe\":{\"name\":\"Period\"},\"C6nZpLKjEJr\":{\"name\":\"African Medical and Research Foundation\"},\"bMcwwoVnbSR\":{\"name\":\"Malaria testing and surveillance\"},\"kO3z4Dhc038\":{\"name\":\"Malaria testing and surveillance\"},\"xwZ2u3WyQR0\":{\"name\":\"Unicef\"},\"B0bjKC0szQX\":{\"name\":\"DOD\"},\"xEunk8LPzkb\":{\"name\":\"World Relief\"}},\"dimensions\":{\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"ou\":[\"ImspTQPwCqd\"],\"SooXFOUnciJ\":[\"OK2Nr4wdfrZ\",\"B0bjKC0szQX\"],\"hKZh1et5n7v\":[\"GpyQudG21w1\",\"ZLBj3xO8sZ4\",\"Hw5p0lACaxV\"],\"LFsZ8v5v7rq\":[\"C6nZpLKjEJr\",\"CW81uF03hvV\",\"B3nxOazOO2G\",\"RkbOhHwiOgW\",\"TY5rBQzlBRa\",\"hERJraxV8D9\",\"g3bcPGD5Q5i\",\"yfWXlxYNbhy\",\"yrwgRxRhBoU\",\"VLFVaH1MwnF\",\"e5YBV5F5iUd\",\"XK6u6cJCR0t\",\"xwZ2u3WyQR0\",\"LEWNFo4Qrrs\",\"xEunk8LPzkb\",\"uilaJSyXt7d\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "hKZh1et5n7v", "RDT test result", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "SooXFOUnciJ", "Funding Agency", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("POS", "B0bjKC0szQX", "ImspTQPwCqd", "202210", "99"));
    validateRow(response, 1, List.of("POS", "B0bjKC0szQX", "ImspTQPwCqd", "202208", "94"));
    validateRow(response, 2, List.of("POS", "B0bjKC0szQX", "ImspTQPwCqd", "202207", "92"));
    validateRow(response, 3, List.of("POS", "B0bjKC0szQX", "ImspTQPwCqd", "202209", "92"));
    validateRow(response, 4, List.of("POS", "B0bjKC0szQX", "ImspTQPwCqd", "202211", "77"));
    validateRow(response, 5, List.of("POS", "B0bjKC0szQX", "ImspTQPwCqd", "202212", "77"));
    validateRow(response, 6, List.of("POS", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202209", "55"));
    validateRow(response, 7, List.of("POS", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202207", "54"));
    validateRow(response, 8, List.of("POS", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, 9, List.of("POS", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202212", "49"));
    validateRow(response, 10, List.of("POS", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202210", "40"));
    validateRow(response, 11, List.of("POS", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202208", "37"));
    validateRow(response, 12, List.of("NEG", "B0bjKC0szQX", "ImspTQPwCqd", "202207", "101"));
    validateRow(response, 13, List.of("NEG", "B0bjKC0szQX", "ImspTQPwCqd", "202210", "99"));
    validateRow(response, 14, List.of("NEG", "B0bjKC0szQX", "ImspTQPwCqd", "202211", "88"));
    validateRow(response, 15, List.of("NEG", "B0bjKC0szQX", "ImspTQPwCqd", "202209", "76"));
    validateRow(response, 16, List.of("NEG", "B0bjKC0szQX", "ImspTQPwCqd", "202208", "74"));
    validateRow(response, 17, List.of("NEG", "B0bjKC0szQX", "ImspTQPwCqd", "202212", "70"));
    validateRow(response, 18, List.of("NEG", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202208", "61"));
    validateRow(response, 19, List.of("NEG", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, 20, List.of("NEG", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202211", "54"));
    validateRow(response, 21, List.of("NEG", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202210", "46"));
    validateRow(response, 22, List.of("NEG", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, 23, List.of("NEG", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, 24, List.of("MIX", "B0bjKC0szQX", "ImspTQPwCqd", "202208", "99"));
    validateRow(response, 25, List.of("MIX", "B0bjKC0szQX", "ImspTQPwCqd", "202207", "96"));
    validateRow(response, 26, List.of("MIX", "B0bjKC0szQX", "ImspTQPwCqd", "202210", "95"));
    validateRow(response, 27, List.of("MIX", "B0bjKC0szQX", "ImspTQPwCqd", "202209", "94"));
    validateRow(response, 28, List.of("MIX", "B0bjKC0szQX", "ImspTQPwCqd", "202212", "87"));
    validateRow(response, 29, List.of("MIX", "B0bjKC0szQX", "ImspTQPwCqd", "202211", "73"));
    validateRow(response, 30, List.of("MIX", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202211", "62"));
    validateRow(response, 31, List.of("MIX", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202208", "59"));
    validateRow(response, 32, List.of("MIX", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, 33, List.of("MIX", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, 34, List.of("MIX", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202207", "48"));
    validateRow(response, 35, List.of("MIX", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202209", "47"));
  }

  @Test
  public void queryMalariaRdtTestResultByGenderAndChiefdoms() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=kO3z4Dhc038")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=kO3z4Dhc038.hKZh1et5n7v,C31vHZqu0qU:j3C417uW6J7;ddAo6zmIHOk;sHZfCdB8TkN;JLGV7lRQRAg;p916ZCVGNyq,kO3z4Dhc038.oZg33kd9taw,ou:jUb8gELQApl;O6uvpzGd5pu;lc3eMKXaEfw;fdc6uOvgoji,pe:LAST_4_QUARTERS")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("bMcwwoVnbSR", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(6)))
        .body("rows", hasSize(equalTo(222)))
        .body("height", equalTo(222))
        .body("width", equalTo(6))
        .body("headerWidth", equalTo(6));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"JLGV7lRQRAg\":{\"name\":\"NORAD\"},\"uilaJSyXt7d\":{\"name\":\"World Vision\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"C31vHZqu0qU\":{\"name\":\"Donor\"},\"VLFVaH1MwnF\":{\"name\":\"Pathfinder International\"},\"CW81uF03hvV\":{\"name\":\"AIDSRelief Consortium\"},\"hERJraxV8D9\":{\"name\":\"Hope Worldwide\"},\"2022Q3\":{\"name\":\"July - September 2022\"},\"2023Q2\":{\"name\":\"April - June 2023\"},\"2022Q4\":{\"name\":\"October - December 2022\"},\"TY5rBQzlBRa\":{\"name\":\"Family Health International\"},\"sHZfCdB8TkN\":{\"name\":\"GIZ\"},\"XK6u6cJCR0t\":{\"name\":\"Population Services International\"},\"GpyQudG21w1\":{\"code\":\"POS\",\"name\":\"Positive\"},\"LAST_4_QUARTERS\":{\"name\":\"Last 4 quarters\"},\"B3nxOazOO2G\":{\"name\":\"APHIAplus\"},\"Hw5p0lACaxV\":{\"code\":\"MIX\",\"name\":\"Mixed\"},\"hKZh1et5n7v\":{\"name\":\"RDT test result\"},\"ddAo6zmIHOk\":{\"name\":\"DFID\"},\"j3C417uW6J7\":{\"name\":\"DANIDA\"},\"RkbOhHwiOgW\":{\"name\":\"CARE International\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"ou\":{\"name\":\"Organisation unit\"},\"ZLBj3xO8sZ4\":{\"code\":\"NEG\",\"name\":\"Negative\"},\"LFsZ8v5v7rq\":{},\"g3bcPGD5Q5i\":{\"name\":\"International Rescue Committee\"},\"yrwgRxRhBoU\":{\"name\":\"Path\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"2023Q1\":{\"name\":\"January - March 2023\"},\"yfWXlxYNbhy\":{\"name\":\"IntraHealth International\"},\"e5YBV5F5iUd\":{\"name\":\"Plan International\"},\"LEWNFo4Qrrs\":{\"name\":\"World Concern\"},\"pe\":{\"name\":\"Period\"},\"C6nZpLKjEJr\":{\"name\":\"African Medical and Research Foundation\"},\"bMcwwoVnbSR\":{\"name\":\"Malaria testing and surveillance\"},\"oZg33kd9taw\":{\"name\":\"Gender\"},\"kO3z4Dhc038\":{\"name\":\"Malaria testing and surveillance\"},\"p916ZCVGNyq\":{\"name\":\"USAID\"},\"xwZ2u3WyQR0\":{\"name\":\"Unicef\"},\"xEunk8LPzkb\":{\"name\":\"World Relief\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"}},\"dimensions\":{\"pe\":[\"2022Q3\",\"2022Q4\",\"2023Q1\",\"2023Q2\"],\"ou\":[\"jUb8gELQApl\",\"O6uvpzGd5pu\",\"lc3eMKXaEfw\",\"fdc6uOvgoji\"],\"oZg33kd9taw\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"C31vHZqu0qU\":[\"j3C417uW6J7\",\"ddAo6zmIHOk\",\"sHZfCdB8TkN\",\"JLGV7lRQRAg\",\"p916ZCVGNyq\"],\"hKZh1et5n7v\":[\"GpyQudG21w1\",\"ZLBj3xO8sZ4\",\"Hw5p0lACaxV\"],\"LFsZ8v5v7rq\":[\"C6nZpLKjEJr\",\"CW81uF03hvV\",\"B3nxOazOO2G\",\"RkbOhHwiOgW\",\"TY5rBQzlBRa\",\"hERJraxV8D9\",\"g3bcPGD5Q5i\",\"yfWXlxYNbhy\",\"yrwgRxRhBoU\",\"VLFVaH1MwnF\",\"e5YBV5F5iUd\",\"XK6u6cJCR0t\",\"xwZ2u3WyQR0\",\"LEWNFo4Qrrs\",\"xEunk8LPzkb\",\"uilaJSyXt7d\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "hKZh1et5n7v", "RDT test result", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "oZg33kd9taw", "Gender", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "C31vHZqu0qU", "Donor", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 5, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, 0, List.of("POS", "Male", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q3", "11"));
    validateRow(response, 1, List.of("POS", "Female", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q3", "9"));
    validateRow(response, 2, List.of("POS", "Female", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q4", "8"));
    validateRow(response, 3, List.of("POS", "Male", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q4", "7"));
    validateRow(response, 4, List.of("POS", "Male", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q3", "6"));
    validateRow(response, 5, List.of("POS", "Female", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q3", "6"));
    validateRow(response, 6, List.of("POS", "Male", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q4", "5"));
    validateRow(response, 7, List.of("POS", "Male", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q4", "5"));
    validateRow(response, 8, List.of("POS", "Female", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q4", "5"));
    validateRow(response, 9, List.of("POS", "Male", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q3", "5"));
    validateRow(
        response, 10, List.of("POS", "Female", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q3", "5"));
    validateRow(response, 11, List.of("POS", "Male", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q4", "5"));
    validateRow(response, 12, List.of("POS", "Male", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q3", "5"));
    validateRow(response, 13, List.of("POS", "Male", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, 14, List.of("POS", "Male", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, 15, List.of("POS", "Male", "j3C417uW6J7", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(response, 16, List.of("POS", "Male", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(response, 17, List.of("POS", "Male", "j3C417uW6J7", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(
        response, 18, List.of("POS", "Female", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q3", "4"));
    validateRow(
        response, 19, List.of("POS", "Female", "p916ZCVGNyq", "jUb8gELQApl", "2022Q4", "4"));
    validateRow(
        response, 20, List.of("POS", "Female", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q4", "4"));
    validateRow(response, 21, List.of("POS", "Male", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(
        response, 22, List.of("POS", "Female", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(response, 23, List.of("POS", "Male", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q3", "4"));
    validateRow(response, 24, List.of("POS", "Male", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(
        response, 25, List.of("POS", "Female", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q3", "4"));
    validateRow(response, 26, List.of("POS", "Male", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q3", "4"));
    validateRow(response, 27, List.of("POS", "Male", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q3", "3"));
    validateRow(
        response, 28, List.of("POS", "Female", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q4", "3"));
    validateRow(response, 29, List.of("POS", "Male", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(
        response, 30, List.of("POS", "Female", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q4", "3"));
    validateRow(
        response, 31, List.of("POS", "Female", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q3", "3"));
    validateRow(response, 32, List.of("POS", "Male", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, 33, List.of("POS", "Male", "p916ZCVGNyq", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(
        response, 34, List.of("POS", "Female", "j3C417uW6J7", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, 35, List.of("POS", "Male", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q3", "3"));
    validateRow(
        response, 36, List.of("POS", "Female", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, 37, List.of("POS", "Male", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q3", "3"));
    validateRow(
        response, 38, List.of("POS", "Female", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(
        response, 39, List.of("POS", "Female", "j3C417uW6J7", "jUb8gELQApl", "2022Q3", "3"));
    validateRow(
        response, 40, List.of("POS", "Female", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q3", "3"));
    validateRow(
        response, 41, List.of("POS", "Female", "j3C417uW6J7", "fdc6uOvgoji", "2022Q3", "3"));
    validateRow(response, 42, List.of("POS", "Male", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q3", "3"));
    validateRow(
        response, 43, List.of("POS", "Female", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, 44, List.of("POS", "Male", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, 45, List.of("POS", "Male", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(
        response, 46, List.of("POS", "Female", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(response, 47, List.of("POS", "Male", "j3C417uW6J7", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(
        response, 48, List.of("POS", "Female", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(
        response, 49, List.of("POS", "Female", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(
        response, 50, List.of("POS", "Female", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q3", "2"));
    validateRow(response, 51, List.of("POS", "Male", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(
        response, 52, List.of("POS", "Female", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q4", "2"));
    validateRow(
        response, 53, List.of("POS", "Female", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(response, 54, List.of("POS", "Male", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(
        response, 55, List.of("POS", "Female", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(
        response, 56, List.of("POS", "Female", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q4", "2"));
    validateRow(
        response, 57, List.of("POS", "Female", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q3", "2"));
    validateRow(response, 58, List.of("POS", "Male", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, 59, List.of("POS", "Male", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q4", "1"));
    validateRow(response, 60, List.of("POS", "Male", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q4", "1"));
    validateRow(
        response, 61, List.of("POS", "Female", "j3C417uW6J7", "fdc6uOvgoji", "2022Q4", "1"));
    validateRow(
        response, 62, List.of("POS", "Female", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q4", "1"));
    validateRow(
        response, 63, List.of("POS", "Female", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q3", "1"));
    validateRow(
        response, 64, List.of("POS", "Female", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, 65, List.of("POS", "Male", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q3", "1"));
    validateRow(response, 66, List.of("POS", "Male", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q4", "1"));
    validateRow(
        response, 67, List.of("POS", "Female", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(response, 68, List.of("POS", "Male", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q3", "1"));
    validateRow(
        response, 69, List.of("POS", "Female", "p916ZCVGNyq", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(
        response, 70, List.of("POS", "Female", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, 71, List.of("POS", "Male", "j3C417uW6J7", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(response, 72, List.of("POS", "Male", "p916ZCVGNyq", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(response, 73, List.of("POS", "Male", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q4", "1"));
    validateRow(
        response, 74, List.of("POS", "Female", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q4", "1"));
    validateRow(
        response, 75, List.of("POS", "Female", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q4", "1"));
    validateRow(response, 76, List.of("POS", "Male", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q4", "1"));
    validateRow(
        response, 77, List.of("NEG", "Female", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q4", "8"));
    validateRow(response, 78, List.of("NEG", "Male", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q3", "8"));
    validateRow(
        response, 79, List.of("NEG", "Female", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q4", "8"));
    validateRow(
        response, 80, List.of("NEG", "Female", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q4", "7"));
    validateRow(response, 81, List.of("NEG", "Male", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q4", "5"));
    validateRow(response, 82, List.of("NEG", "Male", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q3", "5"));
    validateRow(response, 83, List.of("NEG", "Male", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q3", "5"));
    validateRow(
        response, 84, List.of("NEG", "Female", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q3", "5"));
    validateRow(response, 85, List.of("NEG", "Male", "j3C417uW6J7", "fdc6uOvgoji", "2022Q4", "5"));
    validateRow(
        response, 86, List.of("NEG", "Female", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q4", "5"));
    validateRow(
        response, 87, List.of("NEG", "Female", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q3", "5"));
    validateRow(response, 88, List.of("NEG", "Male", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q3", "5"));
    validateRow(
        response, 89, List.of("NEG", "Female", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q4", "5"));
    validateRow(
        response, 90, List.of("NEG", "Female", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q3", "5"));
    validateRow(
        response, 91, List.of("NEG", "Female", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q3", "5"));
    validateRow(response, 92, List.of("NEG", "Male", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q3", "4"));
    validateRow(
        response, 93, List.of("NEG", "Female", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q3", "4"));
    validateRow(
        response, 94, List.of("NEG", "Female", "j3C417uW6J7", "jUb8gELQApl", "2022Q4", "4"));
    validateRow(
        response, 95, List.of("NEG", "Female", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(response, 96, List.of("NEG", "Male", "j3C417uW6J7", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(
        response, 97, List.of("NEG", "Female", "j3C417uW6J7", "jUb8gELQApl", "2022Q3", "4"));
    validateRow(response, 98, List.of("NEG", "Male", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(
        response, 99, List.of("NEG", "Female", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q3", "4"));
    validateRow(
        response, 100, List.of("NEG", "Female", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, 101, List.of("NEG", "Male", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(
        response, 102, List.of("NEG", "Female", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q3", "4"));
    validateRow(
        response, 103, List.of("NEG", "Female", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(
        response, 104, List.of("NEG", "Female", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(
        response, 105, List.of("NEG", "Female", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q3", "4"));
    validateRow(response, 106, List.of("NEG", "Male", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(response, 107, List.of("NEG", "Male", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(
        response, 108, List.of("NEG", "Female", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q3", "3"));
    validateRow(response, 109, List.of("NEG", "Male", "p916ZCVGNyq", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(
        response, 110, List.of("NEG", "Female", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q4", "3"));
    validateRow(response, 111, List.of("NEG", "Male", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q4", "3"));
    validateRow(response, 112, List.of("NEG", "Male", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(
        response, 113, List.of("NEG", "Female", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(
        response, 114, List.of("NEG", "Female", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q3", "3"));
    validateRow(response, 115, List.of("NEG", "Male", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q3", "3"));
    validateRow(response, 116, List.of("NEG", "Male", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q3", "3"));
    validateRow(
        response, 117, List.of("NEG", "Female", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q3", "3"));
    validateRow(response, 118, List.of("NEG", "Male", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, 119, List.of("NEG", "Male", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q3", "3"));
    validateRow(
        response, 120, List.of("NEG", "Female", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q4", "3"));
    validateRow(response, 121, List.of("NEG", "Male", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q3", "3"));
    validateRow(response, 122, List.of("NEG", "Male", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q4", "3"));
    validateRow(
        response, 123, List.of("NEG", "Female", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, 124, List.of("NEG", "Male", "j3C417uW6J7", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(response, 125, List.of("NEG", "Male", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(
        response, 126, List.of("NEG", "Female", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(response, 127, List.of("NEG", "Male", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, 128, List.of("NEG", "Male", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(
        response, 129, List.of("NEG", "Female", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(response, 130, List.of("NEG", "Male", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(
        response, 131, List.of("NEG", "Female", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(
        response, 132, List.of("NEG", "Female", "p916ZCVGNyq", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(response, 133, List.of("NEG", "Male", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(response, 134, List.of("NEG", "Male", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q4", "2"));
    validateRow(response, 135, List.of("NEG", "Male", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q4", "2"));
    validateRow(response, 136, List.of("NEG", "Male", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, 137, List.of("NEG", "Male", "p916ZCVGNyq", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(
        response, 138, List.of("NEG", "Female", "p916ZCVGNyq", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(response, 139, List.of("NEG", "Male", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q4", "2"));
    validateRow(
        response, 140, List.of("NEG", "Female", "j3C417uW6J7", "fdc6uOvgoji", "2022Q4", "2"));
    validateRow(response, 141, List.of("NEG", "Male", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, 142, List.of("NEG", "Male", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(
        response, 143, List.of("NEG", "Female", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(
        response, 144, List.of("NEG", "Female", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, 145, List.of("NEG", "Male", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q4", "1"));
    validateRow(
        response, 146, List.of("NEG", "Female", "j3C417uW6J7", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, 147, List.of("NEG", "Male", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q3", "1"));
    validateRow(
        response, 148, List.of("NEG", "Female", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q4", "1"));
    validateRow(response, 149, List.of("NEG", "Male", "j3C417uW6J7", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(
        response, 150, List.of("MIX", "Female", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q3", "9"));
    validateRow(
        response, 151, List.of("MIX", "Female", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q4", "9"));
    validateRow(response, 152, List.of("MIX", "Male", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q4", "7"));
    validateRow(response, 153, List.of("MIX", "Male", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q3", "7"));
    validateRow(response, 154, List.of("MIX", "Male", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q3", "6"));
    validateRow(
        response, 155, List.of("MIX", "Female", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q4", "6"));
    validateRow(response, 156, List.of("MIX", "Male", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q3", "6"));
    validateRow(response, 157, List.of("MIX", "Male", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q3", "6"));
    validateRow(response, 158, List.of("MIX", "Male", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q3", "6"));
    validateRow(response, 159, List.of("MIX", "Male", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q4", "5"));
    validateRow(response, 160, List.of("MIX", "Male", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q4", "5"));
    validateRow(
        response, 161, List.of("MIX", "Female", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q3", "5"));
    validateRow(response, 162, List.of("MIX", "Male", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q3", "5"));
    validateRow(response, 163, List.of("MIX", "Male", "j3C417uW6J7", "fdc6uOvgoji", "2022Q3", "5"));
    validateRow(
        response, 164, List.of("MIX", "Female", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q4", "5"));
    validateRow(response, 165, List.of("MIX", "Male", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q3", "5"));
    validateRow(
        response, 166, List.of("MIX", "Female", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(
        response, 167, List.of("MIX", "Female", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q3", "4"));
    validateRow(
        response, 168, List.of("MIX", "Female", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q3", "4"));
    validateRow(
        response, 169, List.of("MIX", "Female", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q3", "4"));
    validateRow(response, 170, List.of("MIX", "Male", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q3", "4"));
    validateRow(
        response, 171, List.of("MIX", "Female", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(
        response, 172, List.of("MIX", "Female", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q4", "4"));
    validateRow(response, 173, List.of("MIX", "Male", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, 174, List.of("MIX", "Male", "j3C417uW6J7", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(
        response, 175, List.of("MIX", "Female", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(response, 176, List.of("MIX", "Male", "j3C417uW6J7", "jUb8gELQApl", "2022Q4", "4"));
    validateRow(
        response, 177, List.of("MIX", "Female", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q3", "4"));
    validateRow(
        response, 178, List.of("MIX", "Female", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(response, 179, List.of("MIX", "Male", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q3", "3"));
    validateRow(
        response, 180, List.of("MIX", "Female", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q3", "3"));
    validateRow(response, 181, List.of("MIX", "Male", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(
        response, 182, List.of("MIX", "Female", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, 183, List.of("MIX", "Male", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q3", "3"));
    validateRow(
        response, 184, List.of("MIX", "Female", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q4", "3"));
    validateRow(response, 185, List.of("MIX", "Male", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, 186, List.of("MIX", "Male", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(
        response, 187, List.of("MIX", "Female", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q4", "3"));
    validateRow(response, 188, List.of("MIX", "Male", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(
        response, 189, List.of("MIX", "Female", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q4", "2"));
    validateRow(response, 190, List.of("MIX", "Male", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, 191, List.of("MIX", "Male", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(
        response, 192, List.of("MIX", "Female", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q3", "2"));
    validateRow(
        response, 193, List.of("MIX", "Female", "j3C417uW6J7", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(
        response, 194, List.of("MIX", "Female", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, 195, List.of("MIX", "Male", "p916ZCVGNyq", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(
        response, 196, List.of("MIX", "Female", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q4", "2"));
    validateRow(response, 197, List.of("MIX", "Male", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(response, 198, List.of("MIX", "Male", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q4", "2"));
    validateRow(response, 199, List.of("MIX", "Male", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q3", "2"));
    validateRow(response, 200, List.of("MIX", "Male", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q3", "2"));
    validateRow(
        response, 201, List.of("MIX", "Female", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q3", "2"));
    validateRow(response, 202, List.of("MIX", "Male", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q4", "2"));
    validateRow(response, 203, List.of("MIX", "Male", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q3", "2"));
    validateRow(
        response, 204, List.of("MIX", "Female", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(response, 205, List.of("MIX", "Male", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q4", "2"));
    validateRow(
        response, 206, List.of("MIX", "Female", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q4", "2"));
    validateRow(
        response, 207, List.of("MIX", "Female", "p916ZCVGNyq", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(
        response, 208, List.of("MIX", "Female", "j3C417uW6J7", "fdc6uOvgoji", "2022Q3", "2"));
    validateRow(
        response, 209, List.of("MIX", "Female", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, 210, List.of("MIX", "Male", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q3", "1"));
    validateRow(response, 211, List.of("MIX", "Male", "j3C417uW6J7", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(
        response, 212, List.of("MIX", "Female", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q3", "1"));
    validateRow(response, 213, List.of("MIX", "Male", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q4", "1"));
    validateRow(
        response, 214, List.of("MIX", "Female", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q4", "1"));
    validateRow(
        response, 215, List.of("MIX", "Female", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q3", "1"));
    validateRow(response, 216, List.of("MIX", "Male", "p916ZCVGNyq", "jUb8gELQApl", "2022Q4", "1"));
    validateRow(
        response, 217, List.of("MIX", "Female", "j3C417uW6J7", "fdc6uOvgoji", "2022Q4", "1"));
    validateRow(response, 218, List.of("MIX", "Male", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q4", "1"));
    validateRow(
        response, 219, List.of("MIX", "Female", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q3", "1"));
    validateRow(response, 220, List.of("MIX", "Male", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q4", "1"));
    validateRow(
        response, 221, List.of("MIX", "Female", "p916ZCVGNyq", "jUb8gELQApl", "2022Q3", "1"));
  }

  @Test
  public void queryMalariaTreatmentsByDonorLast12Months() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=kO3z4Dhc038")
            .add("displayProperty=NAME")
            .add("sortOrder=desc")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add(
                "dimension=ou:jUb8gELQApl;O6uvpzGd5pu;lc3eMKXaEfw;fdc6uOvgoji,C31vHZqu0qU:j3C417uW6J7;ddAo6zmIHOk;sHZfCdB8TkN;JLGV7lRQRAg;p916ZCVGNyq,kO3z4Dhc038.O7OiONht8T3,pe:LAST_12_MONTHS")
            .add("relativePeriodDate=2023-07-01");

    // When
    ApiResponse response = actions.aggregate().get("bMcwwoVnbSR", JSON, JSON, params);
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(5)))
        .body("rows", hasSize(equalTo(361)))
        .body("height", equalTo(361))
        .body("width", equalTo(5))
        .body("headerWidth", equalTo(5));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"202208\":{\"name\":\"August 2022\"},\"uilaJSyXt7d\":{\"name\":\"World Vision\"},\"202209\":{\"name\":\"September 2022\"},\"202207\":{\"name\":\"July 2022\"},\"CW81uF03hvV\":{\"name\":\"AIDSRelief Consortium\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"sHZfCdB8TkN\":{\"name\":\"GIZ\"},\"202210\":{\"name\":\"October 2022\"},\"KS73nQGyKdd\":{\"code\":\"REF_ACT_STOCKOUT\",\"name\":\"Referred ACT stockout\"},\"B3nxOazOO2G\":{\"name\":\"APHIAplus\"},\"j3C417uW6J7\":{\"name\":\"DANIDA\"},\"RkbOhHwiOgW\":{\"name\":\"CARE International\"},\"WmQqeIrSJOV\":{\"code\":\"ACT6x2\",\"name\":\"ACT 6x2\"},\"XEKBlnGyi8w\":{\"code\":\"ACT6x1\",\"name\":\"ACT 6x1\"},\"uCChCCEXKoJ\":{\"code\":\"COMB_BLISTER\",\"name\":\"Combined blister\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"yfWXlxYNbhy\":{\"name\":\"IntraHealth International\"},\"C6nZpLKjEJr\":{\"name\":\"African Medical and Research Foundation\"},\"kO3z4Dhc038\":{\"name\":\"Malaria testing and surveillance\"},\"p916ZCVGNyq\":{\"name\":\"USAID\"},\"xwZ2u3WyQR0\":{\"name\":\"Unicef\"},\"JLGV7lRQRAg\":{\"name\":\"NORAD\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"C31vHZqu0qU\":{\"name\":\"Donor\"},\"202305\":{\"name\":\"May 2023\"},\"202306\":{\"name\":\"June 2023\"},\"VLFVaH1MwnF\":{\"name\":\"Pathfinder International\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"hERJraxV8D9\":{\"name\":\"Hope Worldwide\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"TY5rBQzlBRa\":{\"name\":\"Family Health International\"},\"XK6u6cJCR0t\":{\"name\":\"Population Services International\"},\"ddAo6zmIHOk\":{\"name\":\"DFID\"},\"O7OiONht8T3\":{\"name\":\"Treatment Malaria\"},\"ou\":{\"name\":\"Organisation unit\"},\"LFsZ8v5v7rq\":{},\"g3bcPGD5Q5i\":{\"name\":\"International Rescue Committee\"},\"yrwgRxRhBoU\":{\"name\":\"Path\"},\"e5YBV5F5iUd\":{\"name\":\"Plan International\"},\"LEWNFo4Qrrs\":{\"name\":\"World Concern\"},\"pe\":{\"name\":\"Period\"},\"bMcwwoVnbSR\":{\"name\":\"Malaria testing and surveillance\"},\"xEunk8LPzkb\":{\"name\":\"World Relief\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"}},\"dimensions\":{\"O7OiONht8T3\":[\"XEKBlnGyi8w\",\"WmQqeIrSJOV\",\"KS73nQGyKdd\",\"uCChCCEXKoJ\"],\"pe\":[\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\"],\"ou\":[\"jUb8gELQApl\",\"O6uvpzGd5pu\",\"lc3eMKXaEfw\",\"fdc6uOvgoji\"],\"C31vHZqu0qU\":[\"j3C417uW6J7\",\"ddAo6zmIHOk\",\"sHZfCdB8TkN\",\"JLGV7lRQRAg\",\"p916ZCVGNyq\"],\"LFsZ8v5v7rq\":[\"C6nZpLKjEJr\",\"CW81uF03hvV\",\"B3nxOazOO2G\",\"RkbOhHwiOgW\",\"TY5rBQzlBRa\",\"hERJraxV8D9\",\"g3bcPGD5Q5i\",\"yfWXlxYNbhy\",\"yrwgRxRhBoU\",\"VLFVaH1MwnF\",\"e5YBV5F5iUd\",\"XK6u6cJCR0t\",\"xwZ2u3WyQR0\",\"LEWNFo4Qrrs\",\"xEunk8LPzkb\",\"uilaJSyXt7d\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "O7OiONht8T3", "Treatment Malaria", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "C31vHZqu0qU", "Donor", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response, 0, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "O6uvpzGd5pu", "202210", "5"));
    validateRow(
        response, 1, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "O6uvpzGd5pu", "202209", "5"));
    validateRow(
        response, 2, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "lc3eMKXaEfw", "202208", "5"));
    validateRow(
        response, 3, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "fdc6uOvgoji", "202208", "4"));
    validateRow(
        response, 4, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "jUb8gELQApl", "202208", "4"));
    validateRow(
        response, 5, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "O6uvpzGd5pu", "202209", "4"));
    validateRow(
        response, 6, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "fdc6uOvgoji", "202210", "4"));
    validateRow(
        response, 7, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "O6uvpzGd5pu", "202207", "4"));
    validateRow(
        response, 8, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "jUb8gELQApl", "202207", "4"));
    validateRow(
        response, 9, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "fdc6uOvgoji", "202210", "4"));
    validateRow(
        response, 10, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "lc3eMKXaEfw", "202210", "4"));
    validateRow(
        response, 11, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "O6uvpzGd5pu", "202209", "4"));
    validateRow(
        response, 12, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "O6uvpzGd5pu", "202207", "3"));
    validateRow(
        response, 13, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "jUb8gELQApl", "202210", "3"));
    validateRow(
        response, 14, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "O6uvpzGd5pu", "202211", "3"));
    validateRow(
        response, 15, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "fdc6uOvgoji", "202212", "3"));
    validateRow(
        response, 16, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "lc3eMKXaEfw", "202211", "3"));
    validateRow(
        response, 17, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "fdc6uOvgoji", "202210", "3"));
    validateRow(
        response, 18, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "fdc6uOvgoji", "202211", "3"));
    validateRow(
        response, 19, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "O6uvpzGd5pu", "202208", "3"));
    validateRow(
        response, 20, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "jUb8gELQApl", "202209", "2"));
    validateRow(
        response, 21, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "fdc6uOvgoji", "202210", "2"));
    validateRow(
        response, 22, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "O6uvpzGd5pu", "202209", "2"));
    validateRow(
        response, 23, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "jUb8gELQApl", "202209", "2"));
    validateRow(
        response, 24, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "O6uvpzGd5pu", "202208", "2"));
    validateRow(
        response, 25, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "O6uvpzGd5pu", "202211", "2"));
    validateRow(
        response, 26, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "O6uvpzGd5pu", "202207", "2"));
    validateRow(
        response, 27, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "lc3eMKXaEfw", "202212", "2"));
    validateRow(
        response, 28, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "lc3eMKXaEfw", "202210", "2"));
    validateRow(
        response, 29, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "fdc6uOvgoji", "202207", "2"));
    validateRow(
        response, 30, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "lc3eMKXaEfw", "202212", "2"));
    validateRow(
        response, 31, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "O6uvpzGd5pu", "202210", "2"));
    validateRow(
        response, 32, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "lc3eMKXaEfw", "202208", "2"));
    validateRow(
        response, 33, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "jUb8gELQApl", "202212", "2"));
    validateRow(
        response, 34, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "lc3eMKXaEfw", "202210", "2"));
    validateRow(
        response, 35, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "fdc6uOvgoji", "202211", "2"));
    validateRow(
        response, 36, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "jUb8gELQApl", "202212", "2"));
    validateRow(
        response, 37, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "lc3eMKXaEfw", "202212", "2"));
    validateRow(
        response, 38, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "jUb8gELQApl", "202212", "2"));
    validateRow(
        response, 39, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "fdc6uOvgoji", "202207", "2"));
    validateRow(
        response, 40, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "lc3eMKXaEfw", "202207", "2"));
    validateRow(
        response, 41, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "O6uvpzGd5pu", "202210", "2"));
    validateRow(
        response, 42, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "fdc6uOvgoji", "202212", "2"));
    validateRow(
        response, 43, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "fdc6uOvgoji", "202207", "2"));
    validateRow(
        response, 44, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "jUb8gELQApl", "202210", "2"));
    validateRow(
        response, 45, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "O6uvpzGd5pu", "202212", "2"));
    validateRow(
        response, 46, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "jUb8gELQApl", "202211", "1"));
    validateRow(
        response, 47, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "lc3eMKXaEfw", "202207", "1"));
    validateRow(
        response, 48, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "O6uvpzGd5pu", "202211", "1"));
    validateRow(
        response, 49, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "O6uvpzGd5pu", "202210", "1"));
    validateRow(
        response, 50, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "jUb8gELQApl", "202207", "1"));
    validateRow(
        response, 51, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "O6uvpzGd5pu", "202207", "1"));
    validateRow(
        response, 52, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "jUb8gELQApl", "202211", "1"));
    validateRow(
        response, 53, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "jUb8gELQApl", "202209", "1"));
    validateRow(
        response, 54, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "lc3eMKXaEfw", "202207", "1"));
    validateRow(
        response, 55, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "fdc6uOvgoji", "202211", "1"));
    validateRow(
        response, 56, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "jUb8gELQApl", "202207", "1"));
    validateRow(
        response, 57, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "fdc6uOvgoji", "202208", "1"));
    validateRow(
        response, 58, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "fdc6uOvgoji", "202212", "1"));
    validateRow(
        response, 59, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "O6uvpzGd5pu", "202208", "1"));
    validateRow(
        response, 60, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "lc3eMKXaEfw", "202209", "1"));
    validateRow(
        response, 61, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "lc3eMKXaEfw", "202208", "1"));
    validateRow(
        response, 62, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "O6uvpzGd5pu", "202207", "1"));
    validateRow(
        response, 63, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "O6uvpzGd5pu", "202212", "1"));
    validateRow(
        response, 64, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "jUb8gELQApl", "202208", "1"));
    validateRow(
        response, 65, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "O6uvpzGd5pu", "202208", "1"));
    validateRow(
        response, 66, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "fdc6uOvgoji", "202209", "1"));
    validateRow(
        response, 67, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "lc3eMKXaEfw", "202211", "1"));
    validateRow(
        response, 68, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "jUb8gELQApl", "202208", "1"));
    validateRow(
        response, 69, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "O6uvpzGd5pu", "202208", "1"));
    validateRow(
        response, 70, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "O6uvpzGd5pu", "202211", "1"));
    validateRow(
        response, 71, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "fdc6uOvgoji", "202208", "1"));
    validateRow(
        response, 72, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "jUb8gELQApl", "202207", "1"));
    validateRow(
        response, 73, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "lc3eMKXaEfw", "202211", "1"));
    validateRow(
        response, 74, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "O6uvpzGd5pu", "202210", "1"));
    validateRow(
        response, 75, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "fdc6uOvgoji", "202207", "1"));
    validateRow(
        response, 76, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "fdc6uOvgoji", "202212", "1"));
    validateRow(
        response, 77, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "fdc6uOvgoji", "202207", "1"));
    validateRow(
        response, 78, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "O6uvpzGd5pu", "202212", "1"));
    validateRow(
        response, 79, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "jUb8gELQApl", "202211", "1"));
    validateRow(
        response, 80, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "lc3eMKXaEfw", "202210", "1"));
    validateRow(
        response, 81, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "lc3eMKXaEfw", "202209", "1"));
    validateRow(
        response, 82, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "lc3eMKXaEfw", "202208", "1"));
    validateRow(response, 83, List.of("COMB_BLISTER", "JLGV7lRQRAg", "O6uvpzGd5pu", "202207", "6"));
    validateRow(response, 84, List.of("COMB_BLISTER", "sHZfCdB8TkN", "O6uvpzGd5pu", "202210", "6"));
    validateRow(response, 85, List.of("COMB_BLISTER", "JLGV7lRQRAg", "O6uvpzGd5pu", "202211", "5"));
    validateRow(response, 86, List.of("COMB_BLISTER", "p916ZCVGNyq", "jUb8gELQApl", "202212", "5"));
    validateRow(response, 87, List.of("COMB_BLISTER", "sHZfCdB8TkN", "O6uvpzGd5pu", "202208", "5"));
    validateRow(response, 88, List.of("COMB_BLISTER", "p916ZCVGNyq", "fdc6uOvgoji", "202211", "5"));
    validateRow(response, 89, List.of("COMB_BLISTER", "JLGV7lRQRAg", "lc3eMKXaEfw", "202209", "4"));
    validateRow(response, 90, List.of("COMB_BLISTER", "JLGV7lRQRAg", "O6uvpzGd5pu", "202212", "4"));
    validateRow(response, 91, List.of("COMB_BLISTER", "sHZfCdB8TkN", "fdc6uOvgoji", "202212", "4"));
    validateRow(response, 92, List.of("COMB_BLISTER", "sHZfCdB8TkN", "lc3eMKXaEfw", "202209", "3"));
    validateRow(response, 93, List.of("COMB_BLISTER", "JLGV7lRQRAg", "jUb8gELQApl", "202209", "3"));
    validateRow(response, 94, List.of("COMB_BLISTER", "ddAo6zmIHOk", "jUb8gELQApl", "202209", "3"));
    validateRow(response, 95, List.of("COMB_BLISTER", "sHZfCdB8TkN", "lc3eMKXaEfw", "202212", "3"));
    validateRow(response, 96, List.of("COMB_BLISTER", "sHZfCdB8TkN", "O6uvpzGd5pu", "202207", "3"));
    validateRow(response, 97, List.of("COMB_BLISTER", "p916ZCVGNyq", "O6uvpzGd5pu", "202210", "3"));
    validateRow(response, 98, List.of("COMB_BLISTER", "p916ZCVGNyq", "lc3eMKXaEfw", "202209", "3"));
    validateRow(response, 99, List.of("COMB_BLISTER", "JLGV7lRQRAg", "O6uvpzGd5pu", "202210", "3"));
    validateRow(
        response, 100, List.of("COMB_BLISTER", "sHZfCdB8TkN", "O6uvpzGd5pu", "202211", "3"));
    validateRow(
        response, 101, List.of("COMB_BLISTER", "JLGV7lRQRAg", "fdc6uOvgoji", "202212", "3"));
    validateRow(
        response, 102, List.of("COMB_BLISTER", "sHZfCdB8TkN", "O6uvpzGd5pu", "202209", "3"));
    validateRow(
        response, 103, List.of("COMB_BLISTER", "ddAo6zmIHOk", "O6uvpzGd5pu", "202211", "3"));
    validateRow(
        response, 104, List.of("COMB_BLISTER", "JLGV7lRQRAg", "fdc6uOvgoji", "202210", "3"));
    validateRow(
        response, 105, List.of("COMB_BLISTER", "ddAo6zmIHOk", "O6uvpzGd5pu", "202212", "3"));
    validateRow(
        response, 106, List.of("COMB_BLISTER", "j3C417uW6J7", "fdc6uOvgoji", "202207", "3"));
    validateRow(
        response, 107, List.of("COMB_BLISTER", "j3C417uW6J7", "O6uvpzGd5pu", "202207", "3"));
    validateRow(
        response, 108, List.of("COMB_BLISTER", "p916ZCVGNyq", "O6uvpzGd5pu", "202212", "2"));
    validateRow(
        response, 109, List.of("COMB_BLISTER", "sHZfCdB8TkN", "jUb8gELQApl", "202210", "2"));
    validateRow(
        response, 110, List.of("COMB_BLISTER", "j3C417uW6J7", "O6uvpzGd5pu", "202209", "2"));
    validateRow(
        response, 111, List.of("COMB_BLISTER", "JLGV7lRQRAg", "jUb8gELQApl", "202210", "2"));
    validateRow(
        response, 112, List.of("COMB_BLISTER", "p916ZCVGNyq", "O6uvpzGd5pu", "202209", "2"));
    validateRow(
        response, 113, List.of("COMB_BLISTER", "ddAo6zmIHOk", "fdc6uOvgoji", "202208", "2"));
    validateRow(
        response, 114, List.of("COMB_BLISTER", "JLGV7lRQRAg", "fdc6uOvgoji", "202211", "2"));
    validateRow(
        response, 115, List.of("COMB_BLISTER", "JLGV7lRQRAg", "jUb8gELQApl", "202207", "2"));
    validateRow(
        response, 116, List.of("COMB_BLISTER", "j3C417uW6J7", "fdc6uOvgoji", "202212", "2"));
    validateRow(
        response, 117, List.of("COMB_BLISTER", "p916ZCVGNyq", "jUb8gELQApl", "202210", "2"));
    validateRow(
        response, 118, List.of("COMB_BLISTER", "j3C417uW6J7", "O6uvpzGd5pu", "202208", "2"));
    validateRow(
        response, 119, List.of("COMB_BLISTER", "JLGV7lRQRAg", "fdc6uOvgoji", "202209", "2"));
    validateRow(
        response, 120, List.of("COMB_BLISTER", "JLGV7lRQRAg", "lc3eMKXaEfw", "202208", "2"));
    validateRow(
        response, 121, List.of("COMB_BLISTER", "sHZfCdB8TkN", "O6uvpzGd5pu", "202212", "2"));
    validateRow(
        response, 122, List.of("COMB_BLISTER", "sHZfCdB8TkN", "jUb8gELQApl", "202212", "2"));
    validateRow(
        response, 123, List.of("COMB_BLISTER", "JLGV7lRQRAg", "jUb8gELQApl", "202211", "2"));
    validateRow(
        response, 124, List.of("COMB_BLISTER", "j3C417uW6J7", "fdc6uOvgoji", "202211", "2"));
    validateRow(
        response, 125, List.of("COMB_BLISTER", "j3C417uW6J7", "lc3eMKXaEfw", "202208", "2"));
    validateRow(
        response, 126, List.of("COMB_BLISTER", "JLGV7lRQRAg", "fdc6uOvgoji", "202207", "2"));
    validateRow(
        response, 127, List.of("COMB_BLISTER", "sHZfCdB8TkN", "jUb8gELQApl", "202208", "2"));
    validateRow(
        response, 128, List.of("COMB_BLISTER", "sHZfCdB8TkN", "jUb8gELQApl", "202211", "2"));
    validateRow(
        response, 129, List.of("COMB_BLISTER", "j3C417uW6J7", "fdc6uOvgoji", "202208", "2"));
    validateRow(
        response, 130, List.of("COMB_BLISTER", "p916ZCVGNyq", "O6uvpzGd5pu", "202211", "2"));
    validateRow(
        response, 131, List.of("COMB_BLISTER", "sHZfCdB8TkN", "fdc6uOvgoji", "202210", "2"));
    validateRow(
        response, 132, List.of("COMB_BLISTER", "sHZfCdB8TkN", "lc3eMKXaEfw", "202207", "2"));
    validateRow(
        response, 133, List.of("COMB_BLISTER", "p916ZCVGNyq", "fdc6uOvgoji", "202207", "2"));
    validateRow(
        response, 134, List.of("COMB_BLISTER", "JLGV7lRQRAg", "fdc6uOvgoji", "202208", "2"));
    validateRow(
        response, 135, List.of("COMB_BLISTER", "sHZfCdB8TkN", "lc3eMKXaEfw", "202210", "2"));
    validateRow(
        response, 136, List.of("COMB_BLISTER", "j3C417uW6J7", "jUb8gELQApl", "202211", "2"));
    validateRow(
        response, 137, List.of("COMB_BLISTER", "sHZfCdB8TkN", "jUb8gELQApl", "202207", "2"));
    validateRow(
        response, 138, List.of("COMB_BLISTER", "JLGV7lRQRAg", "jUb8gELQApl", "202208", "2"));
    validateRow(
        response, 139, List.of("COMB_BLISTER", "ddAo6zmIHOk", "fdc6uOvgoji", "202209", "1"));
    validateRow(
        response, 140, List.of("COMB_BLISTER", "p916ZCVGNyq", "lc3eMKXaEfw", "202208", "1"));
    validateRow(
        response, 141, List.of("COMB_BLISTER", "j3C417uW6J7", "lc3eMKXaEfw", "202212", "1"));
    validateRow(
        response, 142, List.of("COMB_BLISTER", "j3C417uW6J7", "jUb8gELQApl", "202208", "1"));
    validateRow(
        response, 143, List.of("COMB_BLISTER", "sHZfCdB8TkN", "jUb8gELQApl", "202209", "1"));
    validateRow(
        response, 144, List.of("COMB_BLISTER", "ddAo6zmIHOk", "lc3eMKXaEfw", "202208", "1"));
    validateRow(
        response, 145, List.of("COMB_BLISTER", "sHZfCdB8TkN", "fdc6uOvgoji", "202209", "1"));
    validateRow(
        response, 146, List.of("COMB_BLISTER", "JLGV7lRQRAg", "O6uvpzGd5pu", "202208", "1"));
    validateRow(
        response, 147, List.of("COMB_BLISTER", "ddAo6zmIHOk", "lc3eMKXaEfw", "202209", "1"));
    validateRow(
        response, 148, List.of("COMB_BLISTER", "j3C417uW6J7", "fdc6uOvgoji", "202209", "1"));
    validateRow(
        response, 149, List.of("COMB_BLISTER", "p916ZCVGNyq", "jUb8gELQApl", "202209", "1"));
    validateRow(
        response, 150, List.of("COMB_BLISTER", "j3C417uW6J7", "jUb8gELQApl", "202210", "1"));
    validateRow(
        response, 151, List.of("COMB_BLISTER", "ddAo6zmIHOk", "lc3eMKXaEfw", "202212", "1"));
    validateRow(
        response, 152, List.of("COMB_BLISTER", "j3C417uW6J7", "jUb8gELQApl", "202212", "1"));
    validateRow(
        response, 153, List.of("COMB_BLISTER", "ddAo6zmIHOk", "jUb8gELQApl", "202210", "1"));
    validateRow(
        response, 154, List.of("COMB_BLISTER", "j3C417uW6J7", "fdc6uOvgoji", "202210", "1"));
    validateRow(
        response, 155, List.of("COMB_BLISTER", "sHZfCdB8TkN", "lc3eMKXaEfw", "202208", "1"));
    validateRow(
        response, 156, List.of("COMB_BLISTER", "sHZfCdB8TkN", "fdc6uOvgoji", "202211", "1"));
    validateRow(
        response, 157, List.of("COMB_BLISTER", "p916ZCVGNyq", "fdc6uOvgoji", "202208", "1"));
    validateRow(
        response, 158, List.of("COMB_BLISTER", "p916ZCVGNyq", "fdc6uOvgoji", "202209", "1"));
    validateRow(
        response, 159, List.of("COMB_BLISTER", "j3C417uW6J7", "lc3eMKXaEfw", "202209", "1"));
    validateRow(
        response, 160, List.of("COMB_BLISTER", "j3C417uW6J7", "lc3eMKXaEfw", "202207", "1"));
    validateRow(
        response, 161, List.of("COMB_BLISTER", "ddAo6zmIHOk", "O6uvpzGd5pu", "202210", "1"));
    validateRow(
        response, 162, List.of("COMB_BLISTER", "sHZfCdB8TkN", "lc3eMKXaEfw", "202211", "1"));
    validateRow(
        response, 163, List.of("COMB_BLISTER", "p916ZCVGNyq", "fdc6uOvgoji", "202210", "1"));
    validateRow(
        response, 164, List.of("COMB_BLISTER", "p916ZCVGNyq", "jUb8gELQApl", "202207", "1"));
    validateRow(
        response, 165, List.of("COMB_BLISTER", "ddAo6zmIHOk", "jUb8gELQApl", "202208", "1"));
    validateRow(
        response, 166, List.of("COMB_BLISTER", "ddAo6zmIHOk", "jUb8gELQApl", "202212", "1"));
    validateRow(
        response, 167, List.of("COMB_BLISTER", "ddAo6zmIHOk", "lc3eMKXaEfw", "202210", "1"));
    validateRow(
        response, 168, List.of("COMB_BLISTER", "j3C417uW6J7", "O6uvpzGd5pu", "202212", "1"));
    validateRow(
        response, 169, List.of("COMB_BLISTER", "ddAo6zmIHOk", "fdc6uOvgoji", "202207", "1"));
    validateRow(
        response, 170, List.of("COMB_BLISTER", "j3C417uW6J7", "jUb8gELQApl", "202207", "1"));
    validateRow(
        response, 171, List.of("COMB_BLISTER", "ddAo6zmIHOk", "fdc6uOvgoji", "202211", "1"));
    validateRow(
        response, 172, List.of("COMB_BLISTER", "j3C417uW6J7", "lc3eMKXaEfw", "202210", "1"));
    validateRow(
        response, 173, List.of("COMB_BLISTER", "p916ZCVGNyq", "O6uvpzGd5pu", "202208", "1"));
    validateRow(
        response, 174, List.of("COMB_BLISTER", "JLGV7lRQRAg", "jUb8gELQApl", "202212", "1"));
    validateRow(
        response, 175, List.of("COMB_BLISTER", "ddAo6zmIHOk", "O6uvpzGd5pu", "202209", "1"));
    validateRow(
        response, 176, List.of("COMB_BLISTER", "JLGV7lRQRAg", "lc3eMKXaEfw", "202210", "1"));
    validateRow(
        response, 177, List.of("COMB_BLISTER", "JLGV7lRQRAg", "O6uvpzGd5pu", "202209", "1"));
    validateRow(
        response, 178, List.of("COMB_BLISTER", "ddAo6zmIHOk", "jUb8gELQApl", "202211", "1"));
    validateRow(response, 179, List.of("ACT6x2", "sHZfCdB8TkN", "O6uvpzGd5pu", "202209", "5"));
    validateRow(response, 180, List.of("ACT6x2", "sHZfCdB8TkN", "O6uvpzGd5pu", "202208", "4"));
    validateRow(response, 181, List.of("ACT6x2", "sHZfCdB8TkN", "fdc6uOvgoji", "202210", "4"));
    validateRow(response, 182, List.of("ACT6x2", "j3C417uW6J7", "fdc6uOvgoji", "202207", "4"));
    validateRow(response, 183, List.of("ACT6x2", "sHZfCdB8TkN", "O6uvpzGd5pu", "202212", "4"));
    validateRow(response, 184, List.of("ACT6x2", "j3C417uW6J7", "O6uvpzGd5pu", "202207", "4"));
    validateRow(response, 185, List.of("ACT6x2", "sHZfCdB8TkN", "fdc6uOvgoji", "202207", "4"));
    validateRow(response, 186, List.of("ACT6x2", "p916ZCVGNyq", "O6uvpzGd5pu", "202209", "3"));
    validateRow(response, 187, List.of("ACT6x2", "sHZfCdB8TkN", "lc3eMKXaEfw", "202210", "3"));
    validateRow(response, 188, List.of("ACT6x2", "j3C417uW6J7", "fdc6uOvgoji", "202209", "3"));
    validateRow(response, 189, List.of("ACT6x2", "j3C417uW6J7", "O6uvpzGd5pu", "202209", "3"));
    validateRow(response, 190, List.of("ACT6x2", "sHZfCdB8TkN", "jUb8gELQApl", "202208", "3"));
    validateRow(response, 191, List.of("ACT6x2", "JLGV7lRQRAg", "lc3eMKXaEfw", "202212", "3"));
    validateRow(response, 192, List.of("ACT6x2", "p916ZCVGNyq", "fdc6uOvgoji", "202208", "3"));
    validateRow(response, 193, List.of("ACT6x2", "j3C417uW6J7", "O6uvpzGd5pu", "202210", "3"));
    validateRow(response, 194, List.of("ACT6x2", "JLGV7lRQRAg", "lc3eMKXaEfw", "202210", "3"));
    validateRow(response, 195, List.of("ACT6x2", "p916ZCVGNyq", "O6uvpzGd5pu", "202210", "3"));
    validateRow(response, 196, List.of("ACT6x2", "JLGV7lRQRAg", "jUb8gELQApl", "202210", "3"));
    validateRow(response, 197, List.of("ACT6x2", "p916ZCVGNyq", "O6uvpzGd5pu", "202207", "3"));
    validateRow(response, 198, List.of("ACT6x2", "j3C417uW6J7", "O6uvpzGd5pu", "202212", "3"));
    validateRow(response, 199, List.of("ACT6x2", "ddAo6zmIHOk", "O6uvpzGd5pu", "202211", "3"));
    validateRow(response, 200, List.of("ACT6x2", "p916ZCVGNyq", "fdc6uOvgoji", "202211", "3"));
    validateRow(response, 201, List.of("ACT6x2", "JLGV7lRQRAg", "O6uvpzGd5pu", "202212", "3"));
    validateRow(response, 202, List.of("ACT6x2", "sHZfCdB8TkN", "O6uvpzGd5pu", "202207", "2"));
    validateRow(response, 203, List.of("ACT6x2", "sHZfCdB8TkN", "jUb8gELQApl", "202210", "2"));
    validateRow(response, 204, List.of("ACT6x2", "sHZfCdB8TkN", "fdc6uOvgoji", "202208", "2"));
    validateRow(response, 205, List.of("ACT6x2", "sHZfCdB8TkN", "lc3eMKXaEfw", "202212", "2"));
    validateRow(response, 206, List.of("ACT6x2", "p916ZCVGNyq", "fdc6uOvgoji", "202212", "2"));
    validateRow(response, 207, List.of("ACT6x2", "sHZfCdB8TkN", "O6uvpzGd5pu", "202211", "2"));
    validateRow(response, 208, List.of("ACT6x2", "JLGV7lRQRAg", "fdc6uOvgoji", "202208", "2"));
    validateRow(response, 209, List.of("ACT6x2", "JLGV7lRQRAg", "jUb8gELQApl", "202211", "2"));
    validateRow(response, 210, List.of("ACT6x2", "p916ZCVGNyq", "lc3eMKXaEfw", "202208", "2"));
    validateRow(response, 211, List.of("ACT6x2", "p916ZCVGNyq", "jUb8gELQApl", "202209", "2"));
    validateRow(response, 212, List.of("ACT6x2", "p916ZCVGNyq", "fdc6uOvgoji", "202209", "2"));
    validateRow(response, 213, List.of("ACT6x2", "JLGV7lRQRAg", "lc3eMKXaEfw", "202207", "2"));
    validateRow(response, 214, List.of("ACT6x2", "j3C417uW6J7", "O6uvpzGd5pu", "202211", "2"));
    validateRow(response, 215, List.of("ACT6x2", "j3C417uW6J7", "jUb8gELQApl", "202209", "2"));
    validateRow(response, 216, List.of("ACT6x2", "sHZfCdB8TkN", "fdc6uOvgoji", "202209", "2"));
    validateRow(response, 217, List.of("ACT6x2", "JLGV7lRQRAg", "O6uvpzGd5pu", "202207", "2"));
    validateRow(response, 218, List.of("ACT6x2", "sHZfCdB8TkN", "jUb8gELQApl", "202207", "2"));
    validateRow(response, 219, List.of("ACT6x2", "ddAo6zmIHOk", "O6uvpzGd5pu", "202212", "2"));
    validateRow(response, 220, List.of("ACT6x2", "j3C417uW6J7", "lc3eMKXaEfw", "202207", "2"));
    validateRow(response, 221, List.of("ACT6x2", "JLGV7lRQRAg", "lc3eMKXaEfw", "202209", "2"));
    validateRow(response, 222, List.of("ACT6x2", "sHZfCdB8TkN", "lc3eMKXaEfw", "202209", "2"));
    validateRow(response, 223, List.of("ACT6x2", "j3C417uW6J7", "lc3eMKXaEfw", "202209", "2"));
    validateRow(response, 224, List.of("ACT6x2", "j3C417uW6J7", "fdc6uOvgoji", "202212", "2"));
    validateRow(response, 225, List.of("ACT6x2", "j3C417uW6J7", "O6uvpzGd5pu", "202208", "2"));
    validateRow(response, 226, List.of("ACT6x2", "ddAo6zmIHOk", "jUb8gELQApl", "202208", "2"));
    validateRow(response, 227, List.of("ACT6x2", "JLGV7lRQRAg", "lc3eMKXaEfw", "202208", "2"));
    validateRow(response, 228, List.of("ACT6x2", "JLGV7lRQRAg", "O6uvpzGd5pu", "202211", "2"));
    validateRow(response, 229, List.of("ACT6x2", "ddAo6zmIHOk", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, 230, List.of("ACT6x2", "j3C417uW6J7", "lc3eMKXaEfw", "202210", "1"));
    validateRow(response, 231, List.of("ACT6x2", "p916ZCVGNyq", "lc3eMKXaEfw", "202209", "1"));
    validateRow(response, 232, List.of("ACT6x2", "ddAo6zmIHOk", "lc3eMKXaEfw", "202211", "1"));
    validateRow(response, 233, List.of("ACT6x2", "ddAo6zmIHOk", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, 234, List.of("ACT6x2", "p916ZCVGNyq", "lc3eMKXaEfw", "202210", "1"));
    validateRow(response, 235, List.of("ACT6x2", "p916ZCVGNyq", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, 236, List.of("ACT6x2", "JLGV7lRQRAg", "jUb8gELQApl", "202212", "1"));
    validateRow(response, 237, List.of("ACT6x2", "JLGV7lRQRAg", "O6uvpzGd5pu", "202209", "1"));
    validateRow(response, 238, List.of("ACT6x2", "ddAo6zmIHOk", "jUb8gELQApl", "202211", "1"));
    validateRow(response, 239, List.of("ACT6x2", "JLGV7lRQRAg", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, 240, List.of("ACT6x2", "JLGV7lRQRAg", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, 241, List.of("ACT6x2", "j3C417uW6J7", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, 242, List.of("ACT6x2", "p916ZCVGNyq", "jUb8gELQApl", "202212", "1"));
    validateRow(response, 243, List.of("ACT6x2", "j3C417uW6J7", "fdc6uOvgoji", "202208", "1"));
    validateRow(response, 244, List.of("ACT6x2", "p916ZCVGNyq", "jUb8gELQApl", "202208", "1"));
    validateRow(response, 245, List.of("ACT6x2", "p916ZCVGNyq", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, 246, List.of("ACT6x2", "sHZfCdB8TkN", "jUb8gELQApl", "202211", "1"));
    validateRow(response, 247, List.of("ACT6x2", "p916ZCVGNyq", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, 248, List.of("ACT6x2", "p916ZCVGNyq", "jUb8gELQApl", "202211", "1"));
    validateRow(response, 249, List.of("ACT6x2", "j3C417uW6J7", "jUb8gELQApl", "202210", "1"));
    validateRow(response, 250, List.of("ACT6x2", "JLGV7lRQRAg", "O6uvpzGd5pu", "202210", "1"));
    validateRow(response, 251, List.of("ACT6x2", "JLGV7lRQRAg", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, 252, List.of("ACT6x2", "ddAo6zmIHOk", "fdc6uOvgoji", "202208", "1"));
    validateRow(response, 253, List.of("ACT6x2", "ddAo6zmIHOk", "O6uvpzGd5pu", "202210", "1"));
    validateRow(response, 254, List.of("ACT6x2", "sHZfCdB8TkN", "lc3eMKXaEfw", "202208", "1"));
    validateRow(response, 255, List.of("ACT6x2", "JLGV7lRQRAg", "jUb8gELQApl", "202208", "1"));
    validateRow(response, 256, List.of("ACT6x2", "ddAo6zmIHOk", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, 257, List.of("ACT6x2", "sHZfCdB8TkN", "fdc6uOvgoji", "202212", "1"));
    validateRow(response, 258, List.of("ACT6x2", "j3C417uW6J7", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, 259, List.of("ACT6x2", "JLGV7lRQRAg", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, 260, List.of("ACT6x2", "sHZfCdB8TkN", "jUb8gELQApl", "202209", "1"));
    validateRow(response, 261, List.of("ACT6x2", "ddAo6zmIHOk", "O6uvpzGd5pu", "202207", "1"));
    validateRow(response, 262, List.of("ACT6x2", "p916ZCVGNyq", "lc3eMKXaEfw", "202207", "1"));
    validateRow(response, 263, List.of("ACT6x2", "sHZfCdB8TkN", "jUb8gELQApl", "202212", "1"));
    validateRow(response, 264, List.of("ACT6x2", "ddAo6zmIHOk", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, 265, List.of("ACT6x2", "ddAo6zmIHOk", "jUb8gELQApl", "202209", "1"));
    validateRow(response, 266, List.of("ACT6x2", "j3C417uW6J7", "jUb8gELQApl", "202208", "1"));
    validateRow(response, 267, List.of("ACT6x2", "sHZfCdB8TkN", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, 268, List.of("ACT6x2", "JLGV7lRQRAg", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, 269, List.of("ACT6x2", "ddAo6zmIHOk", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, 270, List.of("ACT6x2", "p916ZCVGNyq", "jUb8gELQApl", "202210", "1"));
    validateRow(response, 271, List.of("ACT6x2", "sHZfCdB8TkN", "O6uvpzGd5pu", "202210", "1"));
    validateRow(response, 272, List.of("ACT6x1", "j3C417uW6J7", "O6uvpzGd5pu", "202207", "6"));
    validateRow(response, 273, List.of("ACT6x1", "p916ZCVGNyq", "O6uvpzGd5pu", "202207", "5"));
    validateRow(response, 274, List.of("ACT6x1", "j3C417uW6J7", "O6uvpzGd5pu", "202211", "5"));
    validateRow(response, 275, List.of("ACT6x1", "JLGV7lRQRAg", "jUb8gELQApl", "202212", "5"));
    validateRow(response, 276, List.of("ACT6x1", "j3C417uW6J7", "jUb8gELQApl", "202211", "5"));
    validateRow(response, 277, List.of("ACT6x1", "JLGV7lRQRAg", "O6uvpzGd5pu", "202209", "4"));
    validateRow(response, 278, List.of("ACT6x1", "ddAo6zmIHOk", "fdc6uOvgoji", "202208", "4"));
    validateRow(response, 279, List.of("ACT6x1", "JLGV7lRQRAg", "O6uvpzGd5pu", "202211", "4"));
    validateRow(response, 280, List.of("ACT6x1", "sHZfCdB8TkN", "fdc6uOvgoji", "202208", "4"));
    validateRow(response, 281, List.of("ACT6x1", "JLGV7lRQRAg", "O6uvpzGd5pu", "202208", "4"));
    validateRow(response, 282, List.of("ACT6x1", "JLGV7lRQRAg", "fdc6uOvgoji", "202209", "3"));
    validateRow(response, 283, List.of("ACT6x1", "ddAo6zmIHOk", "fdc6uOvgoji", "202210", "3"));
    validateRow(response, 284, List.of("ACT6x1", "sHZfCdB8TkN", "jUb8gELQApl", "202208", "3"));
    validateRow(response, 285, List.of("ACT6x1", "ddAo6zmIHOk", "jUb8gELQApl", "202207", "3"));
    validateRow(response, 286, List.of("ACT6x1", "JLGV7lRQRAg", "fdc6uOvgoji", "202210", "3"));
    validateRow(response, 287, List.of("ACT6x1", "j3C417uW6J7", "O6uvpzGd5pu", "202209", "3"));
    validateRow(response, 288, List.of("ACT6x1", "sHZfCdB8TkN", "O6uvpzGd5pu", "202208", "3"));
    validateRow(response, 289, List.of("ACT6x1", "j3C417uW6J7", "jUb8gELQApl", "202210", "3"));
    validateRow(response, 290, List.of("ACT6x1", "JLGV7lRQRAg", "O6uvpzGd5pu", "202212", "3"));
    validateRow(response, 291, List.of("ACT6x1", "j3C417uW6J7", "lc3eMKXaEfw", "202208", "3"));
    validateRow(response, 292, List.of("ACT6x1", "ddAo6zmIHOk", "fdc6uOvgoji", "202211", "2"));
    validateRow(response, 293, List.of("ACT6x1", "ddAo6zmIHOk", "O6uvpzGd5pu", "202209", "2"));
    validateRow(response, 294, List.of("ACT6x1", "JLGV7lRQRAg", "fdc6uOvgoji", "202207", "2"));
    validateRow(response, 295, List.of("ACT6x1", "p916ZCVGNyq", "O6uvpzGd5pu", "202210", "2"));
    validateRow(response, 296, List.of("ACT6x1", "j3C417uW6J7", "O6uvpzGd5pu", "202210", "2"));
    validateRow(response, 297, List.of("ACT6x1", "JLGV7lRQRAg", "jUb8gELQApl", "202211", "2"));
    validateRow(response, 298, List.of("ACT6x1", "p916ZCVGNyq", "jUb8gELQApl", "202208", "2"));
    validateRow(response, 299, List.of("ACT6x1", "ddAo6zmIHOk", "O6uvpzGd5pu", "202208", "2"));
    validateRow(response, 300, List.of("ACT6x1", "p916ZCVGNyq", "fdc6uOvgoji", "202209", "2"));
    validateRow(response, 301, List.of("ACT6x1", "p916ZCVGNyq", "fdc6uOvgoji", "202211", "2"));
    validateRow(response, 302, List.of("ACT6x1", "j3C417uW6J7", "fdc6uOvgoji", "202212", "2"));
    validateRow(response, 303, List.of("ACT6x1", "sHZfCdB8TkN", "jUb8gELQApl", "202210", "2"));
    validateRow(response, 304, List.of("ACT6x1", "p916ZCVGNyq", "jUb8gELQApl", "202211", "2"));
    validateRow(response, 305, List.of("ACT6x1", "JLGV7lRQRAg", "O6uvpzGd5pu", "202207", "2"));
    validateRow(response, 306, List.of("ACT6x1", "p916ZCVGNyq", "O6uvpzGd5pu", "202208", "2"));
    validateRow(response, 307, List.of("ACT6x1", "sHZfCdB8TkN", "lc3eMKXaEfw", "202207", "2"));
    validateRow(response, 308, List.of("ACT6x1", "sHZfCdB8TkN", "lc3eMKXaEfw", "202209", "2"));
    validateRow(response, 309, List.of("ACT6x1", "JLGV7lRQRAg", "lc3eMKXaEfw", "202207", "2"));
    validateRow(response, 310, List.of("ACT6x1", "ddAo6zmIHOk", "jUb8gELQApl", "202210", "2"));
    validateRow(response, 311, List.of("ACT6x1", "JLGV7lRQRAg", "fdc6uOvgoji", "202211", "2"));
    validateRow(response, 312, List.of("ACT6x1", "sHZfCdB8TkN", "lc3eMKXaEfw", "202210", "2"));
    validateRow(response, 313, List.of("ACT6x1", "JLGV7lRQRAg", "lc3eMKXaEfw", "202208", "2"));
    validateRow(response, 314, List.of("ACT6x1", "JLGV7lRQRAg", "jUb8gELQApl", "202209", "2"));
    validateRow(response, 315, List.of("ACT6x1", "j3C417uW6J7", "O6uvpzGd5pu", "202208", "2"));
    validateRow(response, 316, List.of("ACT6x1", "j3C417uW6J7", "lc3eMKXaEfw", "202211", "2"));
    validateRow(response, 317, List.of("ACT6x1", "sHZfCdB8TkN", "O6uvpzGd5pu", "202210", "2"));
    validateRow(response, 318, List.of("ACT6x1", "JLGV7lRQRAg", "O6uvpzGd5pu", "202210", "2"));
    validateRow(response, 319, List.of("ACT6x1", "sHZfCdB8TkN", "O6uvpzGd5pu", "202211", "2"));
    validateRow(response, 320, List.of("ACT6x1", "sHZfCdB8TkN", "O6uvpzGd5pu", "202207", "2"));
    validateRow(response, 321, List.of("ACT6x1", "sHZfCdB8TkN", "jUb8gELQApl", "202211", "2"));
    validateRow(response, 322, List.of("ACT6x1", "j3C417uW6J7", "lc3eMKXaEfw", "202207", "2"));
    validateRow(response, 323, List.of("ACT6x1", "JLGV7lRQRAg", "jUb8gELQApl", "202208", "2"));
    validateRow(response, 324, List.of("ACT6x1", "sHZfCdB8TkN", "fdc6uOvgoji", "202209", "2"));
    validateRow(response, 325, List.of("ACT6x1", "sHZfCdB8TkN", "jUb8gELQApl", "202209", "1"));
    validateRow(response, 326, List.of("ACT6x1", "sHZfCdB8TkN", "lc3eMKXaEfw", "202212", "1"));
    validateRow(response, 327, List.of("ACT6x1", "ddAo6zmIHOk", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, 328, List.of("ACT6x1", "p916ZCVGNyq", "O6uvpzGd5pu", "202209", "1"));
    validateRow(response, 329, List.of("ACT6x1", "sHZfCdB8TkN", "O6uvpzGd5pu", "202209", "1"));
    validateRow(response, 330, List.of("ACT6x1", "ddAo6zmIHOk", "lc3eMKXaEfw", "202208", "1"));
    validateRow(response, 331, List.of("ACT6x1", "j3C417uW6J7", "O6uvpzGd5pu", "202212", "1"));
    validateRow(response, 332, List.of("ACT6x1", "JLGV7lRQRAg", "fdc6uOvgoji", "202208", "1"));
    validateRow(response, 333, List.of("ACT6x1", "JLGV7lRQRAg", "fdc6uOvgoji", "202212", "1"));
    validateRow(response, 334, List.of("ACT6x1", "p916ZCVGNyq", "fdc6uOvgoji", "202208", "1"));
    validateRow(response, 335, List.of("ACT6x1", "p916ZCVGNyq", "O6uvpzGd5pu", "202212", "1"));
    validateRow(response, 336, List.of("ACT6x1", "j3C417uW6J7", "lc3eMKXaEfw", "202209", "1"));
    validateRow(response, 337, List.of("ACT6x1", "p916ZCVGNyq", "lc3eMKXaEfw", "202211", "1"));
    validateRow(response, 338, List.of("ACT6x1", "JLGV7lRQRAg", "jUb8gELQApl", "202210", "1"));
    validateRow(response, 339, List.of("ACT6x1", "p916ZCVGNyq", "O6uvpzGd5pu", "202211", "1"));
    validateRow(response, 340, List.of("ACT6x1", "j3C417uW6J7", "jUb8gELQApl", "202207", "1"));
    validateRow(response, 341, List.of("ACT6x1", "j3C417uW6J7", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, 342, List.of("ACT6x1", "j3C417uW6J7", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, 343, List.of("ACT6x1", "ddAo6zmIHOk", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, 344, List.of("ACT6x1", "sHZfCdB8TkN", "jUb8gELQApl", "202212", "1"));
    validateRow(response, 345, List.of("ACT6x1", "p916ZCVGNyq", "jUb8gELQApl", "202209", "1"));
    validateRow(response, 346, List.of("ACT6x1", "j3C417uW6J7", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, 347, List.of("ACT6x1", "j3C417uW6J7", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, 348, List.of("ACT6x1", "p916ZCVGNyq", "jUb8gELQApl", "202210", "1"));
    validateRow(response, 349, List.of("ACT6x1", "p916ZCVGNyq", "jUb8gELQApl", "202212", "1"));
    validateRow(response, 350, List.of("ACT6x1", "j3C417uW6J7", "lc3eMKXaEfw", "202210", "1"));
    validateRow(response, 351, List.of("ACT6x1", "j3C417uW6J7", "jUb8gELQApl", "202208", "1"));
    validateRow(response, 352, List.of("ACT6x1", "sHZfCdB8TkN", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, 353, List.of("ACT6x1", "ddAo6zmIHOk", "fdc6uOvgoji", "202212", "1"));
    validateRow(response, 354, List.of("ACT6x1", "sHZfCdB8TkN", "lc3eMKXaEfw", "202211", "1"));
    validateRow(response, 355, List.of("ACT6x1", "sHZfCdB8TkN", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, 356, List.of("ACT6x1", "ddAo6zmIHOk", "O6uvpzGd5pu", "202211", "1"));
    validateRow(response, 357, List.of("ACT6x1", "JLGV7lRQRAg", "lc3eMKXaEfw", "202211", "1"));
    validateRow(response, 358, List.of("ACT6x1", "ddAo6zmIHOk", "jUb8gELQApl", "202211", "1"));
    validateRow(response, 359, List.of("ACT6x1", "j3C417uW6J7", "jUb8gELQApl", "202212", "1"));
    validateRow(response, 360, List.of("ACT6x1", "ddAo6zmIHOk", "O6uvpzGd5pu", "202207", "1"));
  }
}
