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
package org.hisp.dhis.analytics.event.query;

import static org.hamcrest.Matchers.equalTo;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for the request shapes ENROLLMENT_OU must reject. The generator only supports
 * happy paths, so these are written by hand.
 */
public class EventQueryEnrollmentOuValidationTest extends AnalyticsApiTest {
  private static final String PROGRAM = "regOuProg01";

  private static final String EVENT_PROGRAM = "eBAyeGv0exc";

  private static final String BO = "O6uvpzGd5pu";

  private static final String TAMBIAMA_CHC = "agEKP19IUKI";

  private final AnalyticsEventActions eventActions = new AnalyticsEventActions();

  /** A district and a facility cannot share one group-by column. */
  @Test
  public void eventAggregateRejectsEnrollmentOuDimensionWithMixedLevels() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2022,ENROLLMENT_OU:" + BO + ";" + TAMBIAMA_CHC)
            .add("displayProperty=NAME")
            .add("totalPages=false");

    ApiResponse response = eventActions.aggregate().get(PROGRAM, JSON, JSON, params);

    response
        .validate()
        .statusCode(409)
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7261"))
        .body(
            "message",
            equalTo(
                "Dimension `ENROLLMENT_OU` does not support organisation units at different"
                    + " hierarchy levels in an aggregate query"));
  }

  @Test
  public void eventAggregateRejectsEnrollmentOuWithUnknownLevel() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2022,ENROLLMENT_OU:LEVEL-9")
            .add("displayProperty=NAME")
            .add("totalPages=false");

    ApiResponse response = eventActions.aggregate().get(PROGRAM, JSON, JSON, params);

    validateInvalidOrgUnit(response);
  }

  @Test
  public void eventQueryRejectsEnrollmentOuWithUnknownLevel() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2022,ENROLLMENT_OU:LEVEL-9")
            .add("displayProperty=NAME")
            .add("totalPages=false");

    ApiResponse response = eventActions.query().get(PROGRAM, JSON, JSON, params);

    validateInvalidOrgUnit(response);
  }

  /** The enrollment org unit is a property of an enrollment, which an event program has none of. */
  @Test
  public void eventAggregateRejectsEnrollmentOuForProgramWithoutRegistration() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2022,ENROLLMENT_OU:" + BO)
            .add("displayProperty=NAME")
            .add("totalPages=false");

    ApiResponse response = eventActions.aggregate().get(EVENT_PROGRAM, JSON, JSON, params);

    response
        .validate()
        .statusCode(409)
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7259"))
        .body(
            "message",
            equalTo(
                "Dimension `ENROLLMENT_OU` is not supported for a program without registration"));
  }

  private void validateInvalidOrgUnit(ApiResponse response) {
    response
        .validate()
        .statusCode(409)
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7143"))
        .body("message", equalTo("Organisation unit or organisation unit level is not valid"));
  }
}
