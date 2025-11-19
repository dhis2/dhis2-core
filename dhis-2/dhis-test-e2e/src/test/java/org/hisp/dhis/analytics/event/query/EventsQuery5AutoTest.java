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
package org.hisp.dhis.analytics.event.query;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.*;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/events/query" endpoint. */
public class EventsQuery5AutoTest extends AnalyticsApiTest {
  private final AnalyticsEventActions actions = new AnalyticsEventActions();

  @Test
  public void testStagePeriodAndStageFails() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,GxdhnY5wmHq,lastupdated")
            .add("stage=A03MvHHogjR")
            .add("displayProperty=NAME")
            .add("outputType=EVENT")
            .add("eventDate=A03MvHHogjR.LAST_12_MONTHS")
            .add("dimension=ou:USER_ORGUNIT,GxdhnY5wmHq");

    // When
    ApiResponse response = actions.query().get("IpHINAT79UW", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(409)
        .body(
            "message",
            equalTo("stage= parameter cannot be used with eventDate=stage.period syntax"));
  }

  @Test
  public void testStagePeriod() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            // .add("headers=ouname,TEmaOXaSPe7")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("pageSize=10")
            .add("outputType=EVENT")
            .add("page=1")
            .add("dimension=TEmaOXaSPe7,ou:USER_ORGUNIT")
            .add("eventDate=hYyB7FUS5eR.LAST_12_MONTHS")
            .add("relativePeriodDate=2022-07-01")
            .add("desc=lastupdated");

    // When
    ApiResponse response = actions.query().get("qDkgAbB5Jlk", JSON, JSON, params);

    response.prettyPrint();
    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(10)))
        .body("height", equalTo(10))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":false},\"items\":{\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"TEmaOXaSPe7\":{\"uid\":\"TEmaOXaSPe7\",\"name\":\"Malaria case - microscopy Pv. 5-14 years female\",\"dimensionItemType\":\"PROGRAM_INDICATOR\",\"valueType\":\"NUMBER\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk\":{\"uid\":\"qDkgAbB5Jlk\",\"name\":\"Malaria case diagnosis, treatment and investigation\",\"description\":\"All cases in an elimination setting should be registered in this program. Includes relevant case identifiers/details including the ID, Name, Index, Age, Gender, Location,etc..\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"MONTHS_THIS_YEAR\":{\"name\":\"Months this year\"},\"hYyB7FUS5eR\":{\"uid\":\"hYyB7FUS5eR\",\"name\":\"Diagnosis & treatment\",\"description\":\"This stage is used to identify initial diagnosis and treatment. This includes the method of case detection, information about the case include travel history, method of diagnosis, malaria species type and treatment details. \"}},\"dimensions\":{\"TEmaOXaSPe7\":[],\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    // assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "TEmaOXaSPe7",
        "Malaria case - microscopy Pv. 5-14 years female",
        "NUMBER",
        "java.lang.Double",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Ngelehun CHC", "1"));
    validateRow(response, 1, List.of("Ngelehun CHC", "0"));
    validateRow(response, 2, List.of("Ngelehun CHC", "0"));
    validateRow(response, 3, List.of("Ngelehun CHC", "0"));
    validateRow(response, 4, List.of("Ngelehun CHC", "0"));
    validateRow(response, 5, List.of("Ngelehun CHC", "0"));
    validateRow(response, 6, List.of("Ngelehun CHC", "0"));
    validateRow(response, 7, List.of("Ngelehun CHC", "0"));
    validateRow(response, 8, List.of("Ngelehun CHC", "0"));
    validateRow(response, 9, List.of("Ngelehun CHC", "0"));
  }
}
