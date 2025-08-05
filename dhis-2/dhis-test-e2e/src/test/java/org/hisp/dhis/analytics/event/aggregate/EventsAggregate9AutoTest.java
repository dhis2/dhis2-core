/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.event.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/aggregate" endpoint. */
public class EventsAggregate9AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void financialYear2023Sep() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("outputType=EVENT")
            .add("dimension=ou:ImspTQPwCqd,pe:2023Sep,A03MvHHogjR.a3kGcGDCuk6");

    // When
    ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(0)))
        .body("height", equalTo(0))
        .body("width", equalTo(0))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"A03MvHHogjR.a3kGcGDCuk6\":{\"name\":\"MCH Apgar Score\"},\"pe\":{\"name\":\"Period\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"2023Sep\":{\"name\":\"September 2023 - August 2024\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[\"2023Sep\"],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "A03MvHHogjR.a3kGcGDCuk6",
        "MCH Apgar Score",
        "NUMBER",
        "java.lang.Double",
        false,
        true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
  }
}
