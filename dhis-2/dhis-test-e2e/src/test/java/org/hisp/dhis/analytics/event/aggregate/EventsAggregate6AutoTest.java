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
    validateRow(response, List.of("Male", "OyVUzWsX8UF", "ImspTQPwCqd", "20221225", "30"));
    validateRow(response, List.of("Male", "cbPqyIAFw9u", "ImspTQPwCqd", "20221226", "28"));
    validateRow(response, List.of("Male", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221230", "27"));
    validateRow(response, List.of("Male", "cbPqyIAFw9u", "ImspTQPwCqd", "20221230", "26"));
    validateRow(response, List.of("Male", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221228", "26"));
    validateRow(response, List.of("Male", "OyVUzWsX8UF", "ImspTQPwCqd", "20221230", "24"));
    validateRow(response, List.of("Male", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221229", "24"));
    validateRow(response, List.of("Male", "pZzk1L4Blf1", "ImspTQPwCqd", "20221228", "23"));
    validateRow(response, List.of("Male", "ZUUGJnvX40X", "ImspTQPwCqd", "20221225", "22"));
    validateRow(response, List.of("Male", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221229", "21"));
    validateRow(response, List.of("Male", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221226", "21"));
    validateRow(response, List.of("Male", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221227", "21"));
    validateRow(response, List.of("Male", "cbPqyIAFw9u", "ImspTQPwCqd", "20221228", "20"));
    validateRow(response, List.of("Male", "CpP5yzbgfHo", "ImspTQPwCqd", "20221225", "20"));
    validateRow(response, List.of("Male", "CpP5yzbgfHo", "ImspTQPwCqd", "20221228", "20"));
    validateRow(response, List.of("Male", "cbPqyIAFw9u", "ImspTQPwCqd", "20221225", "19"));
    validateRow(response, List.of("Male", "CpP5yzbgfHo", "ImspTQPwCqd", "20221227", "19"));
    validateRow(response, List.of("Male", "cbPqyIAFw9u", "ImspTQPwCqd", "20221229", "19"));
    validateRow(response, List.of("Male", "pZzk1L4Blf1", "ImspTQPwCqd", "20221230", "18"));
    validateRow(response, List.of("Male", "pZzk1L4Blf1", "ImspTQPwCqd", "20221229", "18"));
    validateRow(response, List.of("Male", "CpP5yzbgfHo", "ImspTQPwCqd", "20221230", "18"));
    validateRow(response, List.of("Male", "cbPqyIAFw9u", "ImspTQPwCqd", "20221227", "17"));
    validateRow(response, List.of("Male", "pZzk1L4Blf1", "ImspTQPwCqd", "20221227", "17"));
    validateRow(response, List.of("Male", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221230", "17"));
    validateRow(response, List.of("Male", "pZzk1L4Blf1", "ImspTQPwCqd", "20221226", "17"));
    validateRow(response, List.of("Male", "ZUUGJnvX40X", "ImspTQPwCqd", "20221228", "16"));
    validateRow(response, List.of("Male", "OyVUzWsX8UF", "ImspTQPwCqd", "20221226", "16"));
    validateRow(response, List.of("Male", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221228", "16"));
    validateRow(response, List.of("Male", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221227", "16"));
    validateRow(response, List.of("Male", "CpP5yzbgfHo", "ImspTQPwCqd", "20221229", "16"));
    validateRow(response, List.of("Male", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221225", "16"));
    validateRow(response, List.of("Male", "OyVUzWsX8UF", "ImspTQPwCqd", "20221227", "15"));
    validateRow(response, List.of("Male", "ZUUGJnvX40X", "ImspTQPwCqd", "20221229", "15"));
    validateRow(response, List.of("Male", "ZUUGJnvX40X", "ImspTQPwCqd", "20221227", "14"));
    validateRow(response, List.of("Male", "ZUUGJnvX40X", "ImspTQPwCqd", "20221230", "14"));
    validateRow(response, List.of("Male", "OyVUzWsX8UF", "ImspTQPwCqd", "20221229", "14"));
    validateRow(response, List.of("Male", "ZUUGJnvX40X", "ImspTQPwCqd", "20221226", "13"));
    validateRow(response, List.of("Male", "CpP5yzbgfHo", "ImspTQPwCqd", "20221226", "13"));
    validateRow(response, List.of("Male", "pZzk1L4Blf1", "ImspTQPwCqd", "20221225", "12"));
    validateRow(response, List.of("Male", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221225", "11"));
    validateRow(response, List.of("Male", "OyVUzWsX8UF", "ImspTQPwCqd", "20221228", "11"));
    validateRow(response, List.of("Male", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221226", "9"));
    validateRow(response, List.of("Male", "b7MCpzqJaR2", "ImspTQPwCqd", "20221226", "4"));
    validateRow(response, List.of("Male", "b7MCpzqJaR2", "ImspTQPwCqd", "20221225", "3"));
    validateRow(response, List.of("Male", "b7MCpzqJaR2", "ImspTQPwCqd", "20221229", "2"));
    validateRow(response, List.of("Male", "b7MCpzqJaR2", "ImspTQPwCqd", "20221228", "1"));
    validateRow(response, List.of("Male", "b7MCpzqJaR2", "ImspTQPwCqd", "20221227", "1"));
    validateRow(response, List.of("Female", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221229", "31"));
    validateRow(response, List.of("Female", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221229", "30"));
    validateRow(response, List.of("Female", "ZUUGJnvX40X", "ImspTQPwCqd", "20221229", "28"));
    validateRow(response, List.of("Female", "OyVUzWsX8UF", "ImspTQPwCqd", "20221226", "26"));
    validateRow(response, List.of("Female", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221228", "25"));
    validateRow(response, List.of("Female", "CpP5yzbgfHo", "ImspTQPwCqd", "20221225", "24"));
    validateRow(response, List.of("Female", "ZUUGJnvX40X", "ImspTQPwCqd", "20221225", "24"));
    validateRow(response, List.of("Female", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221225", "24"));
    validateRow(response, List.of("Female", "OyVUzWsX8UF", "ImspTQPwCqd", "20221229", "23"));
    validateRow(response, List.of("Female", "pZzk1L4Blf1", "ImspTQPwCqd", "20221227", "23"));
    validateRow(response, List.of("Female", "cbPqyIAFw9u", "ImspTQPwCqd", "20221225", "23"));
    validateRow(response, List.of("Female", "cbPqyIAFw9u", "ImspTQPwCqd", "20221229", "22"));
    validateRow(response, List.of("Female", "cbPqyIAFw9u", "ImspTQPwCqd", "20221227", "21"));
    validateRow(response, List.of("Female", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221227", "21"));
    validateRow(response, List.of("Female", "CpP5yzbgfHo", "ImspTQPwCqd", "20221229", "20"));
    validateRow(response, List.of("Female", "pZzk1L4Blf1", "ImspTQPwCqd", "20221230", "20"));
    validateRow(response, List.of("Female", "OyVUzWsX8UF", "ImspTQPwCqd", "20221230", "20"));
    validateRow(response, List.of("Female", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221227", "20"));
    validateRow(response, List.of("Female", "OyVUzWsX8UF", "ImspTQPwCqd", "20221225", "19"));
    validateRow(response, List.of("Female", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221228", "19"));
    validateRow(response, List.of("Female", "OyVUzWsX8UF", "ImspTQPwCqd", "20221227", "19"));
    validateRow(response, List.of("Female", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221225", "19"));
    validateRow(response, List.of("Female", "OyVUzWsX8UF", "ImspTQPwCqd", "20221228", "19"));
    validateRow(response, List.of("Female", "ZUUGJnvX40X", "ImspTQPwCqd", "20221230", "19"));
    validateRow(response, List.of("Female", "pZzk1L4Blf1", "ImspTQPwCqd", "20221225", "19"));
    validateRow(response, List.of("Female", "CpP5yzbgfHo", "ImspTQPwCqd", "20221228", "18"));
    validateRow(response, List.of("Female", "CpP5yzbgfHo", "ImspTQPwCqd", "20221227", "18"));
    validateRow(response, List.of("Female", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221230", "18"));
    validateRow(response, List.of("Female", "CpP5yzbgfHo", "ImspTQPwCqd", "20221226", "18"));
    validateRow(response, List.of("Female", "ZUUGJnvX40X", "ImspTQPwCqd", "20221226", "18"));
    validateRow(response, List.of("Female", "pZzk1L4Blf1", "ImspTQPwCqd", "20221226", "18"));
    validateRow(response, List.of("Female", "ZUUGJnvX40X", "ImspTQPwCqd", "20221227", "17"));
    validateRow(response, List.of("Female", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221230", "17"));
    validateRow(response, List.of("Female", "cbPqyIAFw9u", "ImspTQPwCqd", "20221228", "17"));
    validateRow(response, List.of("Female", "Tq4NYCn9eNH", "ImspTQPwCqd", "20221226", "17"));
    validateRow(response, List.of("Female", "pZzk1L4Blf1", "ImspTQPwCqd", "20221228", "16"));
    validateRow(response, List.of("Female", "CpP5yzbgfHo", "ImspTQPwCqd", "20221230", "16"));
    validateRow(response, List.of("Female", "ZUUGJnvX40X", "ImspTQPwCqd", "20221228", "15"));
    validateRow(response, List.of("Female", "TvM2MQgD7Jd", "ImspTQPwCqd", "20221226", "13"));
    validateRow(response, List.of("Female", "cbPqyIAFw9u", "ImspTQPwCqd", "20221226", "13"));
    validateRow(response, List.of("Female", "pZzk1L4Blf1", "ImspTQPwCqd", "20221229", "10"));
    validateRow(response, List.of("Female", "cbPqyIAFw9u", "ImspTQPwCqd", "20221230", "10"));
    validateRow(response, List.of("Female", "b7MCpzqJaR2", "ImspTQPwCqd", "20221229", "4"));
    validateRow(response, List.of("Female", "b7MCpzqJaR2", "ImspTQPwCqd", "20221230", "4"));
    validateRow(response, List.of("Female", "b7MCpzqJaR2", "ImspTQPwCqd", "20221228", "3"));
    validateRow(response, List.of("Female", "b7MCpzqJaR2", "ImspTQPwCqd", "20221226", "1"));
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
    validateRow(response, List.of("POS", "B0bjKC0szQX", "ImspTQPwCqd", "202210", "99"));
    validateRow(response, List.of("POS", "B0bjKC0szQX", "ImspTQPwCqd", "202208", "94"));
    validateRow(response, List.of("POS", "B0bjKC0szQX", "ImspTQPwCqd", "202207", "92"));
    validateRow(response, List.of("POS", "B0bjKC0szQX", "ImspTQPwCqd", "202209", "92"));
    validateRow(response, List.of("POS", "B0bjKC0szQX", "ImspTQPwCqd", "202211", "77"));
    validateRow(response, List.of("POS", "B0bjKC0szQX", "ImspTQPwCqd", "202212", "77"));
    validateRow(response, List.of("POS", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202209", "55"));
    validateRow(response, List.of("POS", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202207", "54"));
    validateRow(response, List.of("POS", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202211", "50"));
    validateRow(response, List.of("POS", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202212", "49"));
    validateRow(response, List.of("POS", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202210", "40"));
    validateRow(response, List.of("POS", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202208", "37"));
    validateRow(response, List.of("NEG", "B0bjKC0szQX", "ImspTQPwCqd", "202207", "101"));
    validateRow(response, List.of("NEG", "B0bjKC0szQX", "ImspTQPwCqd", "202210", "99"));
    validateRow(response, List.of("NEG", "B0bjKC0szQX", "ImspTQPwCqd", "202211", "88"));
    validateRow(response, List.of("NEG", "B0bjKC0szQX", "ImspTQPwCqd", "202209", "76"));
    validateRow(response, List.of("NEG", "B0bjKC0szQX", "ImspTQPwCqd", "202208", "74"));
    validateRow(response, List.of("NEG", "B0bjKC0szQX", "ImspTQPwCqd", "202212", "70"));
    validateRow(response, List.of("NEG", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202208", "61"));
    validateRow(response, List.of("NEG", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202207", "60"));
    validateRow(response, List.of("NEG", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202211", "54"));
    validateRow(response, List.of("NEG", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202210", "46"));
    validateRow(response, List.of("NEG", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202209", "46"));
    validateRow(response, List.of("NEG", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202212", "44"));
    validateRow(response, List.of("MIX", "B0bjKC0szQX", "ImspTQPwCqd", "202208", "99"));
    validateRow(response, List.of("MIX", "B0bjKC0szQX", "ImspTQPwCqd", "202207", "96"));
    validateRow(response, List.of("MIX", "B0bjKC0szQX", "ImspTQPwCqd", "202210", "95"));
    validateRow(response, List.of("MIX", "B0bjKC0szQX", "ImspTQPwCqd", "202209", "94"));
    validateRow(response, List.of("MIX", "B0bjKC0szQX", "ImspTQPwCqd", "202212", "87"));
    validateRow(response, List.of("MIX", "B0bjKC0szQX", "ImspTQPwCqd", "202211", "73"));
    validateRow(response, List.of("MIX", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202211", "62"));
    validateRow(response, List.of("MIX", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202208", "59"));
    validateRow(response, List.of("MIX", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202210", "51"));
    validateRow(response, List.of("MIX", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202212", "51"));
    validateRow(response, List.of("MIX", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202207", "48"));
    validateRow(response, List.of("MIX", "OK2Nr4wdfrZ", "ImspTQPwCqd", "202209", "47"));
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
    validateRow(response, List.of("POS", "Male", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q3", "11"));
    validateRow(response, List.of("POS", "Female", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q3", "9"));
    validateRow(response, List.of("POS", "Female", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q4", "8"));
    validateRow(response, List.of("POS", "Male", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q4", "7"));
    validateRow(response, List.of("POS", "Male", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q3", "6"));
    validateRow(response, List.of("POS", "Female", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q3", "6"));
    validateRow(response, List.of("POS", "Male", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q4", "5"));
    validateRow(response, List.of("POS", "Male", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q4", "5"));
    validateRow(response, List.of("POS", "Female", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q4", "5"));
    validateRow(response, List.of("POS", "Male", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q3", "5"));
    validateRow(response, List.of("POS", "Female", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q3", "5"));
    validateRow(response, List.of("POS", "Male", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q4", "5"));
    validateRow(response, List.of("POS", "Male", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q3", "5"));
    validateRow(response, List.of("POS", "Male", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, List.of("POS", "Male", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, List.of("POS", "Male", "j3C417uW6J7", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(response, List.of("POS", "Male", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(response, List.of("POS", "Male", "j3C417uW6J7", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(response, List.of("POS", "Female", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q3", "4"));
    validateRow(response, List.of("POS", "Female", "p916ZCVGNyq", "jUb8gELQApl", "2022Q4", "4"));
    validateRow(response, List.of("POS", "Female", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q4", "4"));
    validateRow(response, List.of("POS", "Male", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(response, List.of("POS", "Female", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(response, List.of("POS", "Male", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q3", "4"));
    validateRow(response, List.of("POS", "Male", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(response, List.of("POS", "Female", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q3", "4"));
    validateRow(response, List.of("POS", "Male", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q3", "4"));
    validateRow(response, List.of("POS", "Male", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q3", "3"));
    validateRow(response, List.of("POS", "Female", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q4", "3"));
    validateRow(response, List.of("POS", "Male", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, List.of("POS", "Female", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q4", "3"));
    validateRow(response, List.of("POS", "Female", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q3", "3"));
    validateRow(response, List.of("POS", "Male", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, List.of("POS", "Male", "p916ZCVGNyq", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, List.of("POS", "Female", "j3C417uW6J7", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, List.of("POS", "Male", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q3", "3"));
    validateRow(response, List.of("POS", "Female", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, List.of("POS", "Male", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q3", "3"));
    validateRow(response, List.of("POS", "Female", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, List.of("POS", "Female", "j3C417uW6J7", "jUb8gELQApl", "2022Q3", "3"));
    validateRow(response, List.of("POS", "Female", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q3", "3"));
    validateRow(response, List.of("POS", "Female", "j3C417uW6J7", "fdc6uOvgoji", "2022Q3", "3"));
    validateRow(response, List.of("POS", "Male", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q3", "3"));
    validateRow(response, List.of("POS", "Female", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, List.of("POS", "Male", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, List.of("POS", "Male", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(response, List.of("POS", "Female", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(response, List.of("POS", "Male", "j3C417uW6J7", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(response, List.of("POS", "Female", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(response, List.of("POS", "Female", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, List.of("POS", "Female", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q3", "2"));
    validateRow(response, List.of("POS", "Male", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(response, List.of("POS", "Female", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q4", "2"));
    validateRow(response, List.of("POS", "Female", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(response, List.of("POS", "Male", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, List.of("POS", "Female", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, List.of("POS", "Female", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q4", "2"));
    validateRow(response, List.of("POS", "Female", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q3", "2"));
    validateRow(response, List.of("POS", "Male", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, List.of("POS", "Male", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q4", "1"));
    validateRow(response, List.of("POS", "Male", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q4", "1"));
    validateRow(response, List.of("POS", "Female", "j3C417uW6J7", "fdc6uOvgoji", "2022Q4", "1"));
    validateRow(response, List.of("POS", "Female", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q4", "1"));
    validateRow(response, List.of("POS", "Female", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q3", "1"));
    validateRow(response, List.of("POS", "Female", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, List.of("POS", "Male", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q3", "1"));
    validateRow(response, List.of("POS", "Male", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q4", "1"));
    validateRow(response, List.of("POS", "Female", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(response, List.of("POS", "Male", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q3", "1"));
    validateRow(response, List.of("POS", "Female", "p916ZCVGNyq", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(response, List.of("POS", "Female", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, List.of("POS", "Male", "j3C417uW6J7", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(response, List.of("POS", "Male", "p916ZCVGNyq", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(response, List.of("POS", "Male", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q4", "1"));
    validateRow(response, List.of("POS", "Female", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q4", "1"));
    validateRow(response, List.of("POS", "Female", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q4", "1"));
    validateRow(response, List.of("POS", "Male", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q4", "1"));
    validateRow(response, List.of("NEG", "Female", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q4", "8"));
    validateRow(response, List.of("NEG", "Male", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q3", "8"));
    validateRow(response, List.of("NEG", "Female", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q4", "8"));
    validateRow(response, List.of("NEG", "Female", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q4", "7"));
    validateRow(response, List.of("NEG", "Male", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q4", "5"));
    validateRow(response, List.of("NEG", "Male", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q3", "5"));
    validateRow(response, List.of("NEG", "Male", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q3", "5"));
    validateRow(response, List.of("NEG", "Female", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q3", "5"));
    validateRow(response, List.of("NEG", "Male", "j3C417uW6J7", "fdc6uOvgoji", "2022Q4", "5"));
    validateRow(response, List.of("NEG", "Female", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q4", "5"));
    validateRow(response, List.of("NEG", "Female", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q3", "5"));
    validateRow(response, List.of("NEG", "Male", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q3", "5"));
    validateRow(response, List.of("NEG", "Female", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q4", "5"));
    validateRow(response, List.of("NEG", "Female", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q3", "5"));
    validateRow(response, List.of("NEG", "Female", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q3", "5"));
    validateRow(response, List.of("NEG", "Male", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q3", "4"));
    validateRow(response, List.of("NEG", "Female", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q3", "4"));
    validateRow(response, List.of("NEG", "Female", "j3C417uW6J7", "jUb8gELQApl", "2022Q4", "4"));
    validateRow(response, List.of("NEG", "Female", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(response, List.of("NEG", "Male", "j3C417uW6J7", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(response, List.of("NEG", "Female", "j3C417uW6J7", "jUb8gELQApl", "2022Q3", "4"));
    validateRow(response, List.of("NEG", "Male", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(response, List.of("NEG", "Female", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q3", "4"));
    validateRow(response, List.of("NEG", "Female", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, List.of("NEG", "Male", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, List.of("NEG", "Female", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q3", "4"));
    validateRow(response, List.of("NEG", "Female", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, List.of("NEG", "Female", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, List.of("NEG", "Female", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q3", "4"));
    validateRow(response, List.of("NEG", "Male", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(response, List.of("NEG", "Male", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, List.of("NEG", "Female", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q3", "3"));
    validateRow(response, List.of("NEG", "Male", "p916ZCVGNyq", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, List.of("NEG", "Female", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q4", "3"));
    validateRow(response, List.of("NEG", "Male", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q4", "3"));
    validateRow(response, List.of("NEG", "Male", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, List.of("NEG", "Female", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, List.of("NEG", "Female", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q3", "3"));
    validateRow(response, List.of("NEG", "Male", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q3", "3"));
    validateRow(response, List.of("NEG", "Male", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q3", "3"));
    validateRow(response, List.of("NEG", "Female", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q3", "3"));
    validateRow(response, List.of("NEG", "Male", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, List.of("NEG", "Male", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q3", "3"));
    validateRow(response, List.of("NEG", "Female", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q4", "3"));
    validateRow(response, List.of("NEG", "Male", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q3", "3"));
    validateRow(response, List.of("NEG", "Male", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q4", "3"));
    validateRow(response, List.of("NEG", "Female", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, List.of("NEG", "Male", "j3C417uW6J7", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(response, List.of("NEG", "Male", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(response, List.of("NEG", "Female", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(response, List.of("NEG", "Male", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, List.of("NEG", "Male", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(response, List.of("NEG", "Female", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(response, List.of("NEG", "Male", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(response, List.of("NEG", "Female", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(response, List.of("NEG", "Female", "p916ZCVGNyq", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(response, List.of("NEG", "Male", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(response, List.of("NEG", "Male", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q4", "2"));
    validateRow(response, List.of("NEG", "Male", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q4", "2"));
    validateRow(response, List.of("NEG", "Male", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, List.of("NEG", "Male", "p916ZCVGNyq", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(response, List.of("NEG", "Female", "p916ZCVGNyq", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(response, List.of("NEG", "Male", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q4", "2"));
    validateRow(response, List.of("NEG", "Female", "j3C417uW6J7", "fdc6uOvgoji", "2022Q4", "2"));
    validateRow(response, List.of("NEG", "Male", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, List.of("NEG", "Male", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(response, List.of("NEG", "Female", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, List.of("NEG", "Female", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, List.of("NEG", "Male", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q4", "1"));
    validateRow(response, List.of("NEG", "Female", "j3C417uW6J7", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, List.of("NEG", "Male", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q3", "1"));
    validateRow(response, List.of("NEG", "Female", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q4", "1"));
    validateRow(response, List.of("NEG", "Male", "j3C417uW6J7", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(response, List.of("MIX", "Female", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q3", "9"));
    validateRow(response, List.of("MIX", "Female", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q4", "9"));
    validateRow(response, List.of("MIX", "Male", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q4", "7"));
    validateRow(response, List.of("MIX", "Male", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q3", "7"));
    validateRow(response, List.of("MIX", "Male", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q3", "6"));
    validateRow(response, List.of("MIX", "Female", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q4", "6"));
    validateRow(response, List.of("MIX", "Male", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q3", "6"));
    validateRow(response, List.of("MIX", "Male", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q3", "6"));
    validateRow(response, List.of("MIX", "Male", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q3", "6"));
    validateRow(response, List.of("MIX", "Male", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q4", "5"));
    validateRow(response, List.of("MIX", "Male", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q4", "5"));
    validateRow(response, List.of("MIX", "Female", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q3", "5"));
    validateRow(response, List.of("MIX", "Male", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q3", "5"));
    validateRow(response, List.of("MIX", "Male", "j3C417uW6J7", "fdc6uOvgoji", "2022Q3", "5"));
    validateRow(response, List.of("MIX", "Female", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q4", "5"));
    validateRow(response, List.of("MIX", "Male", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q3", "5"));
    validateRow(response, List.of("MIX", "Female", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(response, List.of("MIX", "Female", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q3", "4"));
    validateRow(response, List.of("MIX", "Female", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q3", "4"));
    validateRow(response, List.of("MIX", "Female", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q3", "4"));
    validateRow(response, List.of("MIX", "Male", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q3", "4"));
    validateRow(response, List.of("MIX", "Female", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, List.of("MIX", "Female", "JLGV7lRQRAg", "jUb8gELQApl", "2022Q4", "4"));
    validateRow(response, List.of("MIX", "Male", "sHZfCdB8TkN", "O6uvpzGd5pu", "2022Q4", "4"));
    validateRow(response, List.of("MIX", "Male", "j3C417uW6J7", "fdc6uOvgoji", "2022Q4", "4"));
    validateRow(response, List.of("MIX", "Female", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(response, List.of("MIX", "Male", "j3C417uW6J7", "jUb8gELQApl", "2022Q4", "4"));
    validateRow(response, List.of("MIX", "Female", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q3", "4"));
    validateRow(response, List.of("MIX", "Female", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q3", "4"));
    validateRow(response, List.of("MIX", "Male", "JLGV7lRQRAg", "lc3eMKXaEfw", "2022Q3", "3"));
    validateRow(response, List.of("MIX", "Female", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q3", "3"));
    validateRow(response, List.of("MIX", "Male", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, List.of("MIX", "Female", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, List.of("MIX", "Male", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q3", "3"));
    validateRow(response, List.of("MIX", "Female", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q4", "3"));
    validateRow(response, List.of("MIX", "Male", "sHZfCdB8TkN", "jUb8gELQApl", "2022Q4", "3"));
    validateRow(response, List.of("MIX", "Male", "JLGV7lRQRAg", "fdc6uOvgoji", "2022Q4", "3"));
    validateRow(response, List.of("MIX", "Female", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q4", "3"));
    validateRow(response, List.of("MIX", "Male", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(response, List.of("MIX", "Female", "sHZfCdB8TkN", "fdc6uOvgoji", "2022Q4", "2"));
    validateRow(response, List.of("MIX", "Male", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, List.of("MIX", "Male", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(response, List.of("MIX", "Female", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q3", "2"));
    validateRow(response, List.of("MIX", "Female", "j3C417uW6J7", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(response, List.of("MIX", "Female", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q4", "2"));
    validateRow(response, List.of("MIX", "Male", "p916ZCVGNyq", "jUb8gELQApl", "2022Q3", "2"));
    validateRow(response, List.of("MIX", "Female", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q4", "2"));
    validateRow(response, List.of("MIX", "Male", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(response, List.of("MIX", "Male", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q4", "2"));
    validateRow(response, List.of("MIX", "Male", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q3", "2"));
    validateRow(response, List.of("MIX", "Male", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q3", "2"));
    validateRow(response, List.of("MIX", "Female", "ddAo6zmIHOk", "fdc6uOvgoji", "2022Q3", "2"));
    validateRow(response, List.of("MIX", "Male", "p916ZCVGNyq", "O6uvpzGd5pu", "2022Q4", "2"));
    validateRow(response, List.of("MIX", "Male", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q3", "2"));
    validateRow(response, List.of("MIX", "Female", "sHZfCdB8TkN", "lc3eMKXaEfw", "2022Q3", "2"));
    validateRow(response, List.of("MIX", "Male", "j3C417uW6J7", "O6uvpzGd5pu", "2022Q4", "2"));
    validateRow(response, List.of("MIX", "Female", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q4", "2"));
    validateRow(response, List.of("MIX", "Female", "p916ZCVGNyq", "jUb8gELQApl", "2022Q4", "2"));
    validateRow(response, List.of("MIX", "Female", "j3C417uW6J7", "fdc6uOvgoji", "2022Q3", "2"));
    validateRow(response, List.of("MIX", "Female", "p916ZCVGNyq", "fdc6uOvgoji", "2022Q3", "1"));
    validateRow(response, List.of("MIX", "Male", "j3C417uW6J7", "lc3eMKXaEfw", "2022Q3", "1"));
    validateRow(response, List.of("MIX", "Male", "j3C417uW6J7", "jUb8gELQApl", "2022Q3", "1"));
    validateRow(response, List.of("MIX", "Female", "ddAo6zmIHOk", "lc3eMKXaEfw", "2022Q3", "1"));
    validateRow(response, List.of("MIX", "Male", "JLGV7lRQRAg", "O6uvpzGd5pu", "2022Q4", "1"));
    validateRow(response, List.of("MIX", "Female", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q4", "1"));
    validateRow(response, List.of("MIX", "Female", "p916ZCVGNyq", "lc3eMKXaEfw", "2022Q3", "1"));
    validateRow(response, List.of("MIX", "Male", "p916ZCVGNyq", "jUb8gELQApl", "2022Q4", "1"));
    validateRow(response, List.of("MIX", "Female", "j3C417uW6J7", "fdc6uOvgoji", "2022Q4", "1"));
    validateRow(response, List.of("MIX", "Male", "ddAo6zmIHOk", "jUb8gELQApl", "2022Q4", "1"));
    validateRow(response, List.of("MIX", "Female", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q3", "1"));
    validateRow(response, List.of("MIX", "Male", "ddAo6zmIHOk", "O6uvpzGd5pu", "2022Q4", "1"));
    validateRow(response, List.of("MIX", "Female", "p916ZCVGNyq", "jUb8gELQApl", "2022Q3", "1"));
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
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "O6uvpzGd5pu", "202210", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "O6uvpzGd5pu", "202209", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "lc3eMKXaEfw", "202208", "5"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "fdc6uOvgoji", "202208", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "jUb8gELQApl", "202208", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "O6uvpzGd5pu", "202209", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "fdc6uOvgoji", "202210", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "O6uvpzGd5pu", "202207", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "jUb8gELQApl", "202207", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "fdc6uOvgoji", "202210", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "lc3eMKXaEfw", "202210", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "O6uvpzGd5pu", "202209", "4"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "O6uvpzGd5pu", "202207", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "jUb8gELQApl", "202210", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "O6uvpzGd5pu", "202211", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "fdc6uOvgoji", "202212", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "lc3eMKXaEfw", "202211", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "fdc6uOvgoji", "202210", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "fdc6uOvgoji", "202211", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "O6uvpzGd5pu", "202208", "3"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "jUb8gELQApl", "202209", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "fdc6uOvgoji", "202210", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "O6uvpzGd5pu", "202209", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "jUb8gELQApl", "202209", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "O6uvpzGd5pu", "202208", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "O6uvpzGd5pu", "202211", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "O6uvpzGd5pu", "202207", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "lc3eMKXaEfw", "202212", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "lc3eMKXaEfw", "202210", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "fdc6uOvgoji", "202207", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "lc3eMKXaEfw", "202212", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "O6uvpzGd5pu", "202210", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "lc3eMKXaEfw", "202208", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "jUb8gELQApl", "202212", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "lc3eMKXaEfw", "202210", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "fdc6uOvgoji", "202211", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "jUb8gELQApl", "202212", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "lc3eMKXaEfw", "202212", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "jUb8gELQApl", "202212", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "fdc6uOvgoji", "202207", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "lc3eMKXaEfw", "202207", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "O6uvpzGd5pu", "202210", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "fdc6uOvgoji", "202212", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "fdc6uOvgoji", "202207", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "jUb8gELQApl", "202210", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "O6uvpzGd5pu", "202212", "2"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "jUb8gELQApl", "202211", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "lc3eMKXaEfw", "202207", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "O6uvpzGd5pu", "202211", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "O6uvpzGd5pu", "202210", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "jUb8gELQApl", "202207", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "O6uvpzGd5pu", "202207", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "jUb8gELQApl", "202211", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "jUb8gELQApl", "202209", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "lc3eMKXaEfw", "202207", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "jUb8gELQApl", "202207", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "fdc6uOvgoji", "202208", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "fdc6uOvgoji", "202212", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "lc3eMKXaEfw", "202209", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "lc3eMKXaEfw", "202208", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "O6uvpzGd5pu", "202207", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "O6uvpzGd5pu", "202212", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "jUb8gELQApl", "202208", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "lc3eMKXaEfw", "202211", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "jUb8gELQApl", "202208", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "O6uvpzGd5pu", "202211", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "fdc6uOvgoji", "202208", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "jUb8gELQApl", "202207", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "lc3eMKXaEfw", "202211", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "O6uvpzGd5pu", "202210", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "sHZfCdB8TkN", "fdc6uOvgoji", "202212", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "O6uvpzGd5pu", "202212", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "p916ZCVGNyq", "jUb8gELQApl", "202211", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "j3C417uW6J7", "lc3eMKXaEfw", "202210", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "JLGV7lRQRAg", "lc3eMKXaEfw", "202209", "1"));
    validateRow(response, List.of("REF_ACT_STOCKOUT", "ddAo6zmIHOk", "lc3eMKXaEfw", "202208", "1"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "O6uvpzGd5pu", "202207", "6"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "O6uvpzGd5pu", "202210", "6"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "O6uvpzGd5pu", "202211", "5"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "jUb8gELQApl", "202212", "5"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "O6uvpzGd5pu", "202208", "5"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "fdc6uOvgoji", "202211", "5"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "lc3eMKXaEfw", "202209", "4"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "O6uvpzGd5pu", "202212", "4"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "fdc6uOvgoji", "202212", "4"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "lc3eMKXaEfw", "202209", "3"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "jUb8gELQApl", "202209", "3"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "jUb8gELQApl", "202209", "3"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "lc3eMKXaEfw", "202212", "3"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "O6uvpzGd5pu", "202207", "3"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "O6uvpzGd5pu", "202210", "3"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "lc3eMKXaEfw", "202209", "3"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "O6uvpzGd5pu", "202210", "3"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "O6uvpzGd5pu", "202211", "3"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "fdc6uOvgoji", "202212", "3"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "O6uvpzGd5pu", "202209", "3"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "O6uvpzGd5pu", "202211", "3"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "fdc6uOvgoji", "202210", "3"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "O6uvpzGd5pu", "202212", "3"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "fdc6uOvgoji", "202207", "3"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "O6uvpzGd5pu", "202207", "3"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "O6uvpzGd5pu", "202212", "2"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "jUb8gELQApl", "202210", "2"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "O6uvpzGd5pu", "202209", "2"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "jUb8gELQApl", "202210", "2"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "O6uvpzGd5pu", "202209", "2"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "fdc6uOvgoji", "202208", "2"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "fdc6uOvgoji", "202211", "2"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "jUb8gELQApl", "202207", "2"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "fdc6uOvgoji", "202212", "2"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "jUb8gELQApl", "202210", "2"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "O6uvpzGd5pu", "202208", "2"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "fdc6uOvgoji", "202209", "2"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "lc3eMKXaEfw", "202208", "2"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "O6uvpzGd5pu", "202212", "2"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "jUb8gELQApl", "202212", "2"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "jUb8gELQApl", "202211", "2"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "fdc6uOvgoji", "202211", "2"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "lc3eMKXaEfw", "202208", "2"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "fdc6uOvgoji", "202207", "2"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "jUb8gELQApl", "202208", "2"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "jUb8gELQApl", "202211", "2"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "fdc6uOvgoji", "202208", "2"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "O6uvpzGd5pu", "202211", "2"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "fdc6uOvgoji", "202210", "2"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "lc3eMKXaEfw", "202207", "2"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "fdc6uOvgoji", "202207", "2"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "fdc6uOvgoji", "202208", "2"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "lc3eMKXaEfw", "202210", "2"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "jUb8gELQApl", "202211", "2"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "jUb8gELQApl", "202207", "2"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "jUb8gELQApl", "202208", "2"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "lc3eMKXaEfw", "202208", "1"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "lc3eMKXaEfw", "202212", "1"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "jUb8gELQApl", "202208", "1"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "jUb8gELQApl", "202209", "1"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "lc3eMKXaEfw", "202208", "1"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "lc3eMKXaEfw", "202209", "1"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "jUb8gELQApl", "202209", "1"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "jUb8gELQApl", "202210", "1"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "lc3eMKXaEfw", "202212", "1"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "jUb8gELQApl", "202212", "1"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "jUb8gELQApl", "202210", "1"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "lc3eMKXaEfw", "202208", "1"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "fdc6uOvgoji", "202208", "1"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "lc3eMKXaEfw", "202209", "1"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "lc3eMKXaEfw", "202207", "1"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "O6uvpzGd5pu", "202210", "1"));
    validateRow(response, List.of("COMB_BLISTER", "sHZfCdB8TkN", "lc3eMKXaEfw", "202211", "1"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "jUb8gELQApl", "202207", "1"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "jUb8gELQApl", "202208", "1"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "jUb8gELQApl", "202212", "1"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "lc3eMKXaEfw", "202210", "1"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "O6uvpzGd5pu", "202212", "1"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "jUb8gELQApl", "202207", "1"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, List.of("COMB_BLISTER", "j3C417uW6J7", "lc3eMKXaEfw", "202210", "1"));
    validateRow(response, List.of("COMB_BLISTER", "p916ZCVGNyq", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "jUb8gELQApl", "202212", "1"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "O6uvpzGd5pu", "202209", "1"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "lc3eMKXaEfw", "202210", "1"));
    validateRow(response, List.of("COMB_BLISTER", "JLGV7lRQRAg", "O6uvpzGd5pu", "202209", "1"));
    validateRow(response, List.of("COMB_BLISTER", "ddAo6zmIHOk", "jUb8gELQApl", "202211", "1"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "O6uvpzGd5pu", "202209", "5"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "O6uvpzGd5pu", "202208", "4"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "fdc6uOvgoji", "202210", "4"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "fdc6uOvgoji", "202207", "4"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "O6uvpzGd5pu", "202212", "4"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "O6uvpzGd5pu", "202207", "4"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "fdc6uOvgoji", "202207", "4"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "O6uvpzGd5pu", "202209", "3"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "lc3eMKXaEfw", "202210", "3"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "fdc6uOvgoji", "202209", "3"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "O6uvpzGd5pu", "202209", "3"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "jUb8gELQApl", "202208", "3"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "lc3eMKXaEfw", "202212", "3"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "fdc6uOvgoji", "202208", "3"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "O6uvpzGd5pu", "202210", "3"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "lc3eMKXaEfw", "202210", "3"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "O6uvpzGd5pu", "202210", "3"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "jUb8gELQApl", "202210", "3"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "O6uvpzGd5pu", "202207", "3"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "O6uvpzGd5pu", "202212", "3"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "O6uvpzGd5pu", "202211", "3"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "fdc6uOvgoji", "202211", "3"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "O6uvpzGd5pu", "202212", "3"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "O6uvpzGd5pu", "202207", "2"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "jUb8gELQApl", "202210", "2"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "fdc6uOvgoji", "202208", "2"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "lc3eMKXaEfw", "202212", "2"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "fdc6uOvgoji", "202212", "2"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "O6uvpzGd5pu", "202211", "2"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "fdc6uOvgoji", "202208", "2"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "jUb8gELQApl", "202211", "2"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "lc3eMKXaEfw", "202208", "2"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "jUb8gELQApl", "202209", "2"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "fdc6uOvgoji", "202209", "2"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "lc3eMKXaEfw", "202207", "2"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "O6uvpzGd5pu", "202211", "2"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "jUb8gELQApl", "202209", "2"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "fdc6uOvgoji", "202209", "2"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "O6uvpzGd5pu", "202207", "2"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "jUb8gELQApl", "202207", "2"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "O6uvpzGd5pu", "202212", "2"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "lc3eMKXaEfw", "202207", "2"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "lc3eMKXaEfw", "202209", "2"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "lc3eMKXaEfw", "202209", "2"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "lc3eMKXaEfw", "202209", "2"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "fdc6uOvgoji", "202212", "2"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "O6uvpzGd5pu", "202208", "2"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "jUb8gELQApl", "202208", "2"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "lc3eMKXaEfw", "202208", "2"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "O6uvpzGd5pu", "202211", "2"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "lc3eMKXaEfw", "202210", "1"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "lc3eMKXaEfw", "202209", "1"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "lc3eMKXaEfw", "202211", "1"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "lc3eMKXaEfw", "202210", "1"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "jUb8gELQApl", "202212", "1"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "O6uvpzGd5pu", "202209", "1"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "jUb8gELQApl", "202211", "1"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "jUb8gELQApl", "202212", "1"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "fdc6uOvgoji", "202208", "1"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "jUb8gELQApl", "202208", "1"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "jUb8gELQApl", "202211", "1"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "jUb8gELQApl", "202211", "1"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "jUb8gELQApl", "202210", "1"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "O6uvpzGd5pu", "202210", "1"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "fdc6uOvgoji", "202208", "1"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "O6uvpzGd5pu", "202210", "1"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "lc3eMKXaEfw", "202208", "1"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "jUb8gELQApl", "202208", "1"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "fdc6uOvgoji", "202212", "1"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "O6uvpzGd5pu", "202208", "1"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "jUb8gELQApl", "202209", "1"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "O6uvpzGd5pu", "202207", "1"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "lc3eMKXaEfw", "202207", "1"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "jUb8gELQApl", "202212", "1"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "jUb8gELQApl", "202209", "1"));
    validateRow(response, List.of("ACT6x2", "j3C417uW6J7", "jUb8gELQApl", "202208", "1"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, List.of("ACT6x2", "JLGV7lRQRAg", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, List.of("ACT6x2", "ddAo6zmIHOk", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, List.of("ACT6x2", "p916ZCVGNyq", "jUb8gELQApl", "202210", "1"));
    validateRow(response, List.of("ACT6x2", "sHZfCdB8TkN", "O6uvpzGd5pu", "202210", "1"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "O6uvpzGd5pu", "202207", "6"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "O6uvpzGd5pu", "202207", "5"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "O6uvpzGd5pu", "202211", "5"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "jUb8gELQApl", "202212", "5"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "jUb8gELQApl", "202211", "5"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "O6uvpzGd5pu", "202209", "4"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "fdc6uOvgoji", "202208", "4"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "O6uvpzGd5pu", "202211", "4"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "fdc6uOvgoji", "202208", "4"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "O6uvpzGd5pu", "202208", "4"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "fdc6uOvgoji", "202209", "3"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "fdc6uOvgoji", "202210", "3"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "jUb8gELQApl", "202208", "3"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "jUb8gELQApl", "202207", "3"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "fdc6uOvgoji", "202210", "3"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "O6uvpzGd5pu", "202209", "3"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "O6uvpzGd5pu", "202208", "3"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "jUb8gELQApl", "202210", "3"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "O6uvpzGd5pu", "202212", "3"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "lc3eMKXaEfw", "202208", "3"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "fdc6uOvgoji", "202211", "2"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "O6uvpzGd5pu", "202209", "2"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "fdc6uOvgoji", "202207", "2"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "O6uvpzGd5pu", "202210", "2"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "O6uvpzGd5pu", "202210", "2"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "jUb8gELQApl", "202211", "2"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "jUb8gELQApl", "202208", "2"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "O6uvpzGd5pu", "202208", "2"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "fdc6uOvgoji", "202209", "2"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "fdc6uOvgoji", "202211", "2"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "fdc6uOvgoji", "202212", "2"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "jUb8gELQApl", "202210", "2"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "jUb8gELQApl", "202211", "2"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "O6uvpzGd5pu", "202207", "2"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "O6uvpzGd5pu", "202208", "2"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "lc3eMKXaEfw", "202207", "2"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "lc3eMKXaEfw", "202209", "2"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "lc3eMKXaEfw", "202207", "2"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "jUb8gELQApl", "202210", "2"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "fdc6uOvgoji", "202211", "2"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "lc3eMKXaEfw", "202210", "2"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "lc3eMKXaEfw", "202208", "2"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "jUb8gELQApl", "202209", "2"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "O6uvpzGd5pu", "202208", "2"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "lc3eMKXaEfw", "202211", "2"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "O6uvpzGd5pu", "202210", "2"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "O6uvpzGd5pu", "202210", "2"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "O6uvpzGd5pu", "202211", "2"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "O6uvpzGd5pu", "202207", "2"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "jUb8gELQApl", "202211", "2"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "lc3eMKXaEfw", "202207", "2"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "jUb8gELQApl", "202208", "2"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "fdc6uOvgoji", "202209", "2"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "jUb8gELQApl", "202209", "1"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "lc3eMKXaEfw", "202212", "1"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "O6uvpzGd5pu", "202209", "1"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "O6uvpzGd5pu", "202209", "1"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "lc3eMKXaEfw", "202208", "1"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "O6uvpzGd5pu", "202212", "1"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "fdc6uOvgoji", "202208", "1"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "fdc6uOvgoji", "202212", "1"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "fdc6uOvgoji", "202208", "1"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "O6uvpzGd5pu", "202212", "1"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "lc3eMKXaEfw", "202209", "1"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "lc3eMKXaEfw", "202211", "1"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "jUb8gELQApl", "202210", "1"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "O6uvpzGd5pu", "202211", "1"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "jUb8gELQApl", "202207", "1"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "fdc6uOvgoji", "202209", "1"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "jUb8gELQApl", "202212", "1"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "jUb8gELQApl", "202209", "1"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "fdc6uOvgoji", "202211", "1"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "jUb8gELQApl", "202210", "1"));
    validateRow(response, List.of("ACT6x1", "p916ZCVGNyq", "jUb8gELQApl", "202212", "1"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "lc3eMKXaEfw", "202210", "1"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "jUb8gELQApl", "202208", "1"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "fdc6uOvgoji", "202207", "1"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "fdc6uOvgoji", "202212", "1"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "lc3eMKXaEfw", "202211", "1"));
    validateRow(response, List.of("ACT6x1", "sHZfCdB8TkN", "fdc6uOvgoji", "202210", "1"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "O6uvpzGd5pu", "202211", "1"));
    validateRow(response, List.of("ACT6x1", "JLGV7lRQRAg", "lc3eMKXaEfw", "202211", "1"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "jUb8gELQApl", "202211", "1"));
    validateRow(response, List.of("ACT6x1", "j3C417uW6J7", "jUb8gELQApl", "202212", "1"));
    validateRow(response, List.of("ACT6x1", "ddAo6zmIHOk", "O6uvpzGd5pu", "202207", "1"));
  }
}
