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
package org.hisp.dhis.analytics.enrollment.query;

import static org.hamcrest.Matchers.equalTo;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for the category and COGS dimensions the enrollment endpoints must reject before
 * running SQL: a category outside the program's category combo, and any COGS, as the enrollment
 * analytics table has no COGS columns. The generator only supports happy paths, so these are
 * written by hand.
 */
public class EnrollmentCategoryDimensionValidationTest extends AnalyticsApiTest {
  /** Child Programme, default category combo. */
  private static final String PROGRAM = "IpHINAT79UW";

  private static final String BIRTH_STAGE = "A03MvHHogjR";

  /** Implementing Partner, not part of the Child Programme category combo. */
  private static final String IMPLEMENTING_PARTNER = "LFsZ8v5v7rq";

  /** Donor. */
  private static final String DONOR = "C31vHZqu0qU";

  private static final String DONOR_GROUP = "VEGyJKFwKNa";

  private final AnalyticsEnrollmentsActions enrollmentsActions = new AnalyticsEnrollmentsActions();

  @Test
  public void queryRejectsCategoryNotInProgramCategoryCombo() {
    ApiResponse response =
        enrollmentsActions
            .query()
            .get(PROGRAM, JSON, JSON, params().add("dimension=" + IMPLEMENTING_PARTNER));

    assertConflict(
        response,
        "E7268",
        "Category `LFsZ8v5v7rq` is not part of the category combination of program `IpHINAT79UW`");
  }

  @Test
  public void queryRejectsCategoryOptionGroupSetDimension() {
    ApiResponse response =
        enrollmentsActions.query().get(PROGRAM, JSON, JSON, params().add("dimension=" + DONOR));

    assertConflict(
        response,
        "E7269",
        "Category option group set `C31vHZqu0qU` is not supported in enrollment analytics");
  }

  @Test
  public void queryRejectsCategoryOptionGroupSetFilter() {
    ApiResponse response =
        enrollmentsActions
            .query()
            .get(PROGRAM, JSON, JSON, params().add("filter=" + DONOR + ":" + DONOR_GROUP));

    assertConflict(
        response,
        "E7269",
        "Category option group set `C31vHZqu0qU` is not supported in enrollment analytics");
  }

  @Test
  public void queryRejectsStageCategoryOptionGroupSetDimension() {
    ApiResponse response =
        enrollmentsActions
            .query()
            .get(PROGRAM, JSON, JSON, params().add("dimension=" + BIRTH_STAGE + "." + DONOR));

    assertConflict(
        response,
        "E7269",
        "Category option group set `C31vHZqu0qU` is not supported in enrollment analytics");
  }

  @Test
  public void aggregateRejectsCategoryNotInProgramCategoryCombo() {
    ApiResponse response =
        enrollmentsActions
            .aggregate()
            .get(PROGRAM, JSON, JSON, params().add("dimension=" + IMPLEMENTING_PARTNER));

    assertConflict(
        response,
        "E7268",
        "Category `LFsZ8v5v7rq` is not part of the category combination of program `IpHINAT79UW`");
  }

  @Test
  public void aggregateRejectsCategoryOptionGroupSetDimension() {
    ApiResponse response =
        enrollmentsActions.aggregate().get(PROGRAM, JSON, JSON, params().add("dimension=" + DONOR));

    assertConflict(
        response,
        "E7269",
        "Category option group set `C31vHZqu0qU` is not supported in enrollment analytics");
  }

  private QueryParamsBuilder params() {
    return new QueryParamsBuilder()
        .add("dimension=ou:ImspTQPwCqd")
        .add("startDate=2024-01-01")
        .add("endDate=2026-12-31");
  }

  private static void assertConflict(ApiResponse response, String errorCode, String message) {
    response
        .validate()
        .statusCode(409)
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo(errorCode))
        .body("message", equalTo(message));
  }
}
