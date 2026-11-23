/*
 * Copyright (c) 2004-2026, University of Oslo
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
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;

import java.util.List;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

/**
 * Custom aggregation on /events/aggregate: the {@code value} and {@code aggregationType} params on
 * WHO RMNCH Tracker, whose Hemoglobin data element lives in four stages with distinct averages.
 */
public class EventsAggregateCustomValueTest extends AnalyticsApiTest {
  private static final String PROGRAM = "WSGAb5XwJ3Y";
  private static final String STAGE = "edqlbukwRfQ";
  private static final String HEMOGLOBIN = "vANAXwtLwcT";
  private static final String HEIGHT_TEA_OUTSIDE_PROGRAM = "lw1SqmMlnfh";
  private static final String TB_PROGRAM = "ur1Edk5Oe2n";
  private static final String HEIGHT_TEA_OF_TYPE_NONE = "lw1SqmMlnfh";

  private final AnalyticsEventActions analyticsEventActions = new AnalyticsEventActions();

  private QueryParamsBuilder baseParams() {
    return new QueryParamsBuilder()
        .add("dimension=pe:2021;2022;2023")
        .add("dimension=ou:ImspTQPwCqd")
        .add("totalPages=false");
  }

  private QueryParamsBuilder tbParams() {
    return new QueryParamsBuilder()
        .add("dimension=pe:2021;2022")
        .add("dimension=ou:ImspTQPwCqd")
        .add("totalPages=false");
  }

  @Test
  void stagePrefixInValueScopesTheAggregationToTheStage() {
    QueryParamsBuilder params =
        baseParams().add("value=" + STAGE + "." + HEMOGLOBIN).add("aggregationType=AVERAGE");

    ApiResponse response = analyticsEventActions.aggregate().get(PROGRAM, JSON, JSON, params);

    response.validate().statusCode(200).body("rows", hasSize(equalTo(3)));

    validateRow(response, List.of("2023", "ImspTQPwCqd", "15.6"));
    validateRow(response, List.of("2022", "ImspTQPwCqd", "15.52"));
    validateRow(response, List.of("2021", "ImspTQPwCqd", "15.58"));
  }

  @Test
  void offsetInValueIsRejected() {
    QueryParamsBuilder params =
        baseParams().add("value=" + STAGE + "[0]." + HEMOGLOBIN).add("aggregationType=AVERAGE");

    ApiResponse response = analyticsEventActions.aggregate().get(PROGRAM, JSON, JSON, params);

    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7264"));
  }

  @Test
  void unknownStagePrefixInValueIsRejected() {
    QueryParamsBuilder params =
        baseParams().add("value=" + STAGE + "X." + HEMOGLOBIN).add("aggregationType=AVERAGE");

    ApiResponse response = analyticsEventActions.aggregate().get(PROGRAM, JSON, JSON, params);

    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7130"));
  }

  @Test
  void programPrefixInValueAggregatesTheAttribute() {
    QueryParamsBuilder params =
        tbParams()
            .add("value=" + TB_PROGRAM + "." + HEIGHT_TEA_OF_TYPE_NONE)
            .add("aggregationType=AVERAGE");

    ApiResponse response = analyticsEventActions.aggregate().get(TB_PROGRAM, JSON, JSON, params);

    response.validate().statusCode(200).body("rows", hasSize(equalTo(2)));

    validateRow(response, List.of("ImspTQPwCqd", "2021", "169.0"));
    validateRow(response, List.of("ImspTQPwCqd", "2022", "171.06"));
  }

  @Test
  void defaultAggregationTypeUsesTheElementsOwnAggregationType() {
    QueryParamsBuilder params =
        baseParams()
            .add("stage=" + STAGE)
            .add("value=" + HEMOGLOBIN)
            .add("aggregationType=DEFAULT");

    ApiResponse response = analyticsEventActions.aggregate().get(PROGRAM, JSON, JSON, params);

    response.validate().statusCode(200).body("rows", hasSize(equalTo(3)));

    // Hemoglobin's own aggregation type is SUM.
    validateRow(response, List.of("2023", "ImspTQPwCqd", "16429.0"));
    validateRow(response, List.of("2022", "ImspTQPwCqd", "76666.0"));
    validateRow(response, List.of("2021", "ImspTQPwCqd", "61539.0"));
  }

  @Test
  void attributeOutsideTheProgramIsRejected() {
    QueryParamsBuilder params =
        baseParams().add("value=" + HEIGHT_TEA_OUTSIDE_PROGRAM).add("aggregationType=AVERAGE");

    ApiResponse response = analyticsEventActions.aggregate().get(PROGRAM, JSON, JSON, params);

    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7223"));
  }

  @Test
  void attributeOfTypeNoneWithoutAggregationTypeIsRejected() {
    QueryParamsBuilder params = tbParams().add("value=" + HEIGHT_TEA_OF_TYPE_NONE);

    ApiResponse response = analyticsEventActions.aggregate().get(TB_PROGRAM, JSON, JSON, params);

    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7265"));
  }

  @Test
  void explicitAggregationTypeNoneIsRejected() {
    QueryParamsBuilder params =
        baseParams().add("value=" + STAGE + "." + HEMOGLOBIN).add("aggregationType=NONE");

    ApiResponse response = analyticsEventActions.aggregate().get(PROGRAM, JSON, JSON, params);

    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7265"));
  }

  @Test
  void explicitOverrideOnAttributeOfTypeNoneAggregates() {
    QueryParamsBuilder params =
        tbParams().add("value=" + HEIGHT_TEA_OF_TYPE_NONE).add("aggregationType=AVERAGE");

    ApiResponse response = analyticsEventActions.aggregate().get(TB_PROGRAM, JSON, JSON, params);

    response.validate().statusCode(200).body("rows", hasSize(equalTo(2)));

    // This program's response lists ou before pe, whichever order the dimensions are requested in.
    validateRow(response, List.of("ImspTQPwCqd", "2021", "169.0"));
    validateRow(response, List.of("ImspTQPwCqd", "2022", "171.06"));
  }
}
