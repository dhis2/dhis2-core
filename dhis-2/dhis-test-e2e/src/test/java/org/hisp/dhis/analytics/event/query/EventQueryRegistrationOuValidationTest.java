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
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for the request shapes REGISTRATION_OU must reject. The generator only supports
 * happy paths, so these are written by hand.
 */
public class EventQueryRegistrationOuValidationTest extends AnalyticsApiTest {
  private static final String PROGRAM = "regOuProg01";

  private static final String BO = "O6uvpzGd5pu";

  private static final String BOMBALI = "fdc6uOvgoji";

  private static final String KAILAHUN = "jUb8gELQApl";

  private final AnalyticsEventActions eventActions = new AnalyticsEventActions();

  private final AnalyticsEnrollmentsActions enrollmentActions = new AnalyticsEnrollmentsActions();

  @Test
  public void eventQueryRejectsRegistrationOuNameSortWithoutRegistrationOuDimension() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2022,ou:ImspTQPwCqd")
            .add("headers=ouname")
            .add("desc=registrationouname")
            .add("displayProperty=NAME")
            .add("totalPages=false");

    ApiResponse response = eventActions.query().get(PROGRAM, JSON, JSON, params);

    validateSortRejected(response, "registrationouname");
  }

  @Test
  public void eventQueryRejectsRegistrationOuSortWithoutRegistrationOuDimension() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2022,ou:ImspTQPwCqd")
            .add("headers=ouname")
            .add("asc=registrationou")
            .add("displayProperty=NAME")
            .add("totalPages=false");

    ApiResponse response = eventActions.query().get(PROGRAM, JSON, JSON, params);

    validateSortRejected(response, "registrationou");
  }

  @Test
  public void eventQueryRejectsRegistrationOuNameSortWithFilterOnly() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2022")
            .add("filter=REGISTRATION_OU:" + KAILAHUN)
            .add("headers=ouname")
            .add("asc=registrationouname")
            .add("displayProperty=NAME")
            .add("totalPages=false");

    ApiResponse response = eventActions.query().get(PROGRAM, JSON, JSON, params);

    validateSortRejected(response, "registrationouname");
  }

  @Test
  public void enrollmentQueryRejectsRegistrationOuNameSortWithoutRegistrationOuDimension() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2022,ou:ImspTQPwCqd")
            .add("headers=ouname")
            .add("desc=registrationouname")
            .add("displayProperty=NAME")
            .add("totalPages=false");

    ApiResponse response = enrollmentActions.query().get(PROGRAM, JSON, JSON, params);

    validateSortRejected(response, "registrationouname");
  }

  @Test
  public void enrollmentQueryRejectsRegistrationOuNameSortWithFilterOnly() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2022")
            .add("filter=REGISTRATION_OU:" + KAILAHUN)
            .add("headers=ouname")
            .add("asc=registrationouname")
            .add("displayProperty=NAME")
            .add("totalPages=false");

    ApiResponse response = enrollmentActions.query().get(PROGRAM, JSON, JSON, params);

    validateSortRejected(response, "registrationouname");
  }

  @Test
  public void eventAggregateRejectsRepeatedRegistrationOuDimension() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2022")
            .add("dimension=REGISTRATION_OU:" + BO)
            .add("dimension=REGISTRATION_OU:" + BOMBALI)
            .add("displayProperty=NAME")
            .add("totalPages=false");

    ApiResponse response = eventActions.aggregate().get(PROGRAM, JSON, JSON, params);

    validateDuplicateRejected(response);
  }

  @Test
  public void enrollmentAggregateRejectsRepeatedRegistrationOuDimension() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2022")
            .add("dimension=REGISTRATION_OU:" + BO)
            .add("dimension=REGISTRATION_OU:" + BOMBALI)
            .add("displayProperty=NAME")
            .add("totalPages=false");

    ApiResponse response = enrollmentActions.aggregate().get(PROGRAM, JSON, JSON, params);

    validateDuplicateRejected(response);
  }

  /** Several org units in one dimension is a single occurrence and must still be accepted. */
  @Test
  public void eventAggregateAcceptsSeveralOrgUnitsInOneRegistrationOuDimension() {
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=pe:2022,REGISTRATION_OU:" + BO + ";" + BOMBALI)
            .add("displayProperty=NAME")
            .add("totalPages=false");

    ApiResponse response = eventActions.aggregate().get(PROGRAM, JSON, JSON, params);

    response.validate().statusCode(200);
  }

  private void validateSortRejected(ApiResponse response, String item) {
    response
        .validate()
        .statusCode(409)
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7262"))
        .body(
            "message",
            equalTo("Sorting by `" + item + "` requires the `REGISTRATION_OU` dimension"));
  }

  private void validateDuplicateRejected(ApiResponse response) {
    response
        .validate()
        .statusCode(409)
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7201"))
        .body(
            "message", equalTo("Dimensions cannot be specified more than once: `REGISTRATION_OU`"));
  }
}
