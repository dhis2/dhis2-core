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
package org.hisp.dhis.analytics;

import static org.hamcrest.Matchers.equalTo;
import static org.hisp.dhis.AnalyticsApiTest.JSON;

import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.actions.analytics.AnalyticsTeiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.extensions.ConfigurationExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This test class has to run before all tests, because the scenarios are testing errors related to
 * missing analytics tables. For this reason they have to run first (before analytics tables are
 * created), hence @Order(1).
 */
@Order(1)
@ExtendWith(ConfigurationExtension.class)
@Tag("analytics")
public class NoAnalyticsTablesErrorsScenariosTest {

  private final AnalyticsEnrollmentsActions analyticsEnrollmentsActions = new AnalyticsEnrollmentsActions();
  private final AnalyticsEventActions analyticsEventActions = new AnalyticsEventActions();
  private final AnalyticsTeiActions analyticsTeiActions = new AnalyticsTeiActions();
  private final RestApiActions analyticsAggregateActions = new RestApiActions("analytics");

  @BeforeAll
  public static void beforeAll() {
    new LoginActions().loginAsAdmin();
  }

  @Test
  void testAggregateAnalyticsWhenAnalyticsTablesAreMissing() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=dx:Uvn6LCg7dVU;sB79w2hiLp8,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
            .add("filter=pe:THIS_YEAR");

    // When
    ApiResponse response = analyticsAggregateActions.get(params);

    // Then
    assertNoAnalyticsTableResponse(response);
  }

  @Test
  void testEventsQueryAnalyticsWhenAnalyticsTablesAreMissing() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd,eMyVanycQSC")
            .add("eventDate=THIS_QUARTER");

    // When
    ApiResponse response = analyticsEventActions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    assertNoAnalyticsTableResponse(response);
  }

  @Test
  void testEnrollmentsQueryAnalyticsWhenAnalyticsTablesAreMissing() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd");

    // When
    ApiResponse response = analyticsEnrollmentsActions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    assertNoAnalyticsTableResponse(response);
  }

  @Test
  void testEventsAggregateAnalyticsWhenAnalyticsTablesAreMissing() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS")
            .add("stage=A03MvHHogjR");

    // When
    ApiResponse response = analyticsEventActions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    assertNoAnalyticsTableResponse(response);
  }

  @Test
  public void queryWithProgramAndEnrollmentDateAndInvalidEnrollmentOffset() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("enrollmentDate=IpHINAT79UW.LAST_YEAR");

    // When
    ApiResponse response = analyticsTeiActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    assertNoAnalyticsTableResponse(response);
  }

  private void assertNoAnalyticsTableResponse(ApiResponse response) {
    response
        .validate()
        .statusCode(409)
        .body("status", equalTo("ERROR"))
        .body("message", equalTo("Query failed because a referenced table does not exist. Please ensure analytics job was run (SqlState: 42P01)"))
        .body("errorCode", equalTo("E7144"))
        .body("devMessage", equalTo("SqlState: 42P01"));
  }
}
