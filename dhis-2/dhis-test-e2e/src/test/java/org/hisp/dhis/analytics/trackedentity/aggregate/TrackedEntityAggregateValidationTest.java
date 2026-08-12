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
package org.hisp.dhis.analytics.trackedentity.aggregate;

import static org.hamcrest.Matchers.equalTo;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsTrackedEntityActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for the validation rules of the "/trackedEntities/aggregate" endpoint. Error
 * paths are hand written because the test generator only records happy path responses.
 */
public class TrackedEntityAggregateValidationTest extends AnalyticsApiTest {
  private final AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  /**
   * A dimension the query cannot group by used to be dropped from the headers and the GROUP BY
   * while its restriction still applied, so the response was an ungrouped total indistinguishable
   * from a correctly grouped one.
   */
  @Test
  public void aggregateByDataElementDimensionShouldFail() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:USER_ORGUNIT")
            .add("dimension=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    assertGroupByNotSupported(response, "IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU");
  }

  @Test
  public void aggregateByEnrollmentDateDimensionShouldFail() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder().add("dimension=IpHINAT79UW.ENROLLMENT_DATE:2021");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    assertGroupByNotSupported(response, "IpHINAT79UW.ENROLLMENT_DATE");
  }

  /** Only the registration org unit is groupable; a stage scoped one has no column to group on. */
  @Test
  public void aggregateByStageScopedOrgUnitDimensionShouldFail() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder().add("dimension=IpHINAT79UW.A03MvHHogjR.ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    assertGroupByNotSupported(response, "IpHINAT79UW.A03MvHHogjR.ou");
  }

  private void assertGroupByNotSupported(ApiResponse response, String dimension) {
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("httpStatusCode", equalTo(409))
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7258"))
        .body(
            "message",
            equalTo(
                "Dimension is not supported as a group by in a tracked entity aggregate query: `"
                    + dimension
                    + "`. Supported dimensions are the registration organisation unit, tracked"
                    + " entity static fields and tracked entity attributes"));
  }
}
