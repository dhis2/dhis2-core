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
package org.hisp.dhis.analytics.enrollment.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/enrollments/aggregate" endpoint. */
public class EnrollmentsAggregate5AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();

  @Test
  public void queryAggregatedenrollmentsmacase5() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=edqlbukwRfQ")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=ENROLLMENT")
            .add("dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS")
            .add("relativePeriodDate=2023-08-01");

    // When
    ApiResponse response = actions.aggregate().get("WSGAb5XwJ3Y", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(12)))
        .body("height", equalTo(12))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"202208\":{\"name\":\"August 2022\"},\"202307\":{\"name\":\"July 2023\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"202211\":{\"name\":\"November 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202212\":{\"name\":\"December 2022\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"202210\":{\"name\":\"October 2022\"},\"pe\":{},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"}},\"dimensions\":{\"pe\":[\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\",\"202307\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);

    // Assert rows.
    validateRow(response, List.of("144", "ImspTQPwCqd", "202211"));
    validateRow(response, List.of("164", "ImspTQPwCqd", "202210"));
    validateRow(response, List.of("181", "ImspTQPwCqd", "202305"));
    validateRow(response, List.of("188", "ImspTQPwCqd", "202301"));
    validateRow(response, List.of("139", "ImspTQPwCqd", "202302"));
    validateRow(response, List.of("175", "ImspTQPwCqd", "202306"));
    validateRow(response, List.of("147", "ImspTQPwCqd", "202209"));
    validateRow(response, List.of("174", "ImspTQPwCqd", "202307"));
    validateRow(response, List.of("187", "ImspTQPwCqd", "202208"));
    validateRow(response, List.of("169", "ImspTQPwCqd", "202304"));
    validateRow(response, List.of("171", "ImspTQPwCqd", "202303"));
    validateRow(response, List.of("178", "ImspTQPwCqd", "202212"));
  }
}
