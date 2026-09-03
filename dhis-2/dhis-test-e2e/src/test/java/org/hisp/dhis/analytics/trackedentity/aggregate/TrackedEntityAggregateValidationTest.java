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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsTrackedEntityActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Groups e2e tests for the validation rules of the "/trackedEntities/aggregate" endpoint. Error
 * paths are hand written because the test generator only records happy path responses.
 */
public class TrackedEntityAggregateValidationTest extends AnalyticsApiTest {
  private final AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  /**
   * An end date has no column of that name on the enrollment table, where it is {@code
   * completeddate}, so it cannot be grouped. It used to reach the database and fail there on the
   * missing column.
   */
  @Test
  public void aggregateByEnrollmentEndDateDimensionShouldFail() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder().add("dimension=IpHINAT79UW.ENDDATE:THIS_YEAR");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    assertGroupByNotSupported(response, "IpHINAT79UW.ENDDATE");
  }

  /**
   * An event status belongs to a program stage, so a program scoped one has no enrollment column to
   * group on. It used to reach the database and fail there on the missing column.
   */
  @Test
  public void aggregateByProgramScopedEventStatusDimensionShouldFail() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder().add("dimension=IpHINAT79UW.EVENT_STATUS:ACTIVE");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    assertGroupByNotSupported(response, "IpHINAT79UW.EVENT_STATUS");
  }

  /**
   * {@code ENROLLMENT_OU} is a keyword alias of the registration org unit, so it must group exactly
   * like {@code ou}. It is resolved to {@code ou} while the request is parsed, and the grouped set
   * has to see the resolved dimension.
   */
  @Test
  public void aggregateByEnrollmentOuKeywordShouldGroupByRegistrationOrgUnit() {
    // Given
    QueryParamsBuilder alias = new QueryParamsBuilder().add("dimension=ENROLLMENT_OU:USER_ORGUNIT");
    QueryParamsBuilder canonical = new QueryParamsBuilder().add("dimension=ou:USER_ORGUNIT");

    // When
    ApiResponse aliasResponse = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, alias);
    ApiResponse canonicalResponse = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, canonical);

    // Then
    aliasResponse
        .validate()
        .statusCode(200)
        .body("headers[0].name", equalTo("ou"))
        .body("headers[1].name", equalTo("value"));

    assertEquals(
        canonicalResponse.extractList("rows"),
        aliasResponse.extractList("rows"),
        "the alias and the canonical dimension must return the same grouped rows");
  }

  /** {@code enrollmentouname} is a keyword alias of {@code ouname}. */
  @Test
  public void aggregateByEnrollmentOunameKeywordShouldGroupByOrgUnitName() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder().add("dimension=enrollmentouname").add("pageSize=5");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers[0].name", equalTo("ouname"))
        .body("headers[1].name", equalTo("value"));
  }

  /** A groupable dimension with items is both grouped and restricted. */
  @Test
  public void aggregateByStaticDimensionWithItemsShouldGroupAndRestrict() {
    // Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("dimension=LAST_UPDATED:LAST_5_YEARS");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(2))
        .body("headers[0].name", equalTo("lastupdated"))
        .body("headers[1].name", equalTo("value"));
  }

  /**
   * An enrollment or event level dimension has no column on the tracked entity table even when it
   * is requested without a program or stage prefix, so it cannot be grouped. These used to reach
   * the database and fail there on the missing column.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "enrollmentdate",
        "enddate",
        "incidentdate",
        "occurreddate",
        "eventdate",
        "scheduleddate",
        "enrollmentstatus",
        "programstatus",
        "eventstatus"
      })
  public void aggregateByUnprefixedEnrollmentLevelDimensionShouldFail(String dimension) {
    // Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("dimension=" + dimension);

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    assertGroupByNotSupported(response, dimension);
  }

  /** The tracked entity table has no period column, so a period dimension cannot be grouped. */
  @Test
  public void aggregateByPeriodDimensionShouldFail() {
    // Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("dimension=pe");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    assertGroupByNotSupported(response, "pe");
  }

  /**
   * A program stage dimension is reported under a name that carries no offset, so two offsets of
   * one stage would answer under one name. Such a request is rejected rather than returning rows
   * whose two grouping coordinates cannot be told apart.
   */
  @Test
  public void aggregateByTwoOffsetsOfTheSameStageDimensionShouldFail() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=A03MvHHogjR.ou:USER_ORGUNIT")
            .add("dimension=A03MvHHogjR[1].ou:USER_ORGUNIT");

    // When
    ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(409)
        .body("status", equalTo("ERROR"))
        .body("errorCode", equalTo("E7259"));
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
                    + " entity static fields, tracked entity attributes, and program or program"
                    + " stage scoped organisation units, dates, statuses and data elements"));
  }
}
