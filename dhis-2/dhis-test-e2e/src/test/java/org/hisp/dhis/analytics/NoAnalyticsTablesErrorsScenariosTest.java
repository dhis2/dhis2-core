/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.hisp.dhis.AnalyticsApiTest.JSON;

import org.hisp.dhis.helpers.EnvUtils;
import org.hisp.dhis.helpers.extensions.ConfigurationExtension;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsOutlierDetectionActions;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsTrackedEntityActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
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

  private final AnalyticsEnrollmentsActions analyticsEnrollmentsActions =
      new AnalyticsEnrollmentsActions();
  private final AnalyticsEventActions analyticsEventActions = new AnalyticsEventActions();
  private final AnalyticsTrackedEntityActions analyticsTrackedEntityActions =
      new AnalyticsTrackedEntityActions();
  private final AnalyticsOutlierDetectionActions analyticsOutlierActions =
      new AnalyticsOutlierDetectionActions();
  private final RestApiActions analyticsAggregateActions = new RestApiActions("analytics");

  private static final String ERROR_MSG =
      "Query failed because a referenced table does not exist. Please ensure analytics job was run ";
  private final String analyticsDatabase = EnvUtils.getDataSource();

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
    assertNoAnalyticsTableResponse(
        response,
        ERROR_MSG + getErrorCode(analyticsDatabase),
        getDevMessage(analyticsDatabase, "analytics"));
  }

  private boolean isPgOrDoris() {
    return analyticsDatabase.equals("postgres") || analyticsDatabase.equals("doris");
  }

  @Test
  @EnabledIf(value = "isPgOrDoris", disabledReason = "This test is only for Postgres and Doris")
  void testEventsQueryAnalyticsWhenAnalyticsTablesAreMissing() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=ou:ImspTQPwCqd,eMyVanycQSC")
            .add("eventDate=THIS_QUARTER");

    // When
    ApiResponse response = analyticsEventActions.query().get("eBAyeGv0exc", JSON, JSON, params);

    // Then
    assertNoAnalyticsTableResponse(
        response,
        ERROR_MSG + getErrorCode(analyticsDatabase),
        getDevMessage(analyticsDatabase, "analytics_event_ebayegv0exc"));
  }

  @Test
  void testEnrollmentsQueryAnalyticsWhenAnalyticsTablesAreMissing() {
    // Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("dimension=ou:ImspTQPwCqd");

    // When
    ApiResponse response =
        analyticsEnrollmentsActions.query().get("IpHINAT79UW", JSON, JSON, params);
    // Then
    assertNoAnalyticsTableResponse(
        response,
        ERROR_MSG + getErrorCode(analyticsDatabase),
        getDevMessage(analyticsDatabase, "analytics_enrollment_iphinat79uw"));
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
    assertNoAnalyticsTableResponse(
        response,
        ERROR_MSG + getErrorCode(analyticsDatabase),
        getDevMessage(analyticsDatabase, "analytics_event_iphinat79uw"));
  }

  @Test
  public void queryWithProgramAndEnrollmentDateAndInvalidEnrollmentOffset() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("program=IpHINAT79UW")
            .add("enrollmentDate=IpHINAT79UW.LAST_YEAR");

    // When
    ApiResponse response =
        analyticsTrackedEntityActions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // TEA tables are only in Postgres
    assertNoAnalyticsTableResponse(
        response,
        ERROR_MSG + "(SqlState: 42P01)",
        getDevMessage("postgres", "analytics_te_neenwmsyuep"));
  }

  @Test
  public void testOutliersAnalyticsWhenOutliersDataAreMissing() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dx=Y7Oq71I3ASg")
            .add("endDate=2024-01-02")
            .add("ou=O6uvpzGd5pu,fdc6uOvgoji")
            .add("maxResults=100")
            .add("startDate=2020-10-01")
            .add("algorithm=MODIFIED_Z_SCORE");

    // When
    ApiResponse response = analyticsOutlierActions.query().get("", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(409)
        .body("status", equalTo("ERROR"))
        .body(
            "message",
            equalTo(
                "The analytics outliers data does not exist. Please ensure analytics job was run and did not skip the outliers"))
        .body("errorCode", equalTo("E7180"));
  }

  private void assertNoAnalyticsTableResponse(
      ApiResponse response, String expectedMessage, String expectedDevMessage) {
    response
        .validate()
        .statusCode(409)
        .body("status", equalTo("ERROR"))
        .body("message", equalTo(expectedMessage))
        .body("errorCode", equalTo("E7144"))
        .body("devMessage", startsWith(expectedDevMessage));
  }

  private String getErrorCode(String analyticsDatabase) {
    return switch (analyticsDatabase) {
      case "clickhouse" -> "(SqlState: 22000)";
      case "doris" -> "(SqlState: HY000)";
      case "postgres" -> "(SqlState: 42P01)";
      default -> null;
    };
  }

  private String getDevMessage(String analyticsDatabase, String tableName) {
    return switch (analyticsDatabase) {
      case "clickhouse" ->
          "Code: 60. DB::Exception: Unknown table expression identifier '" + tableName + "'";
      case "doris" ->
          "errCode = 2, detailMessage = Table ["
              + tableName
              + "] does not exist in database [dhis2].";
      case "postgres" -> "ERROR: relation \"" + tableName + "\" does not exist\n  Position:";
      default -> null;
    };
  }
}
