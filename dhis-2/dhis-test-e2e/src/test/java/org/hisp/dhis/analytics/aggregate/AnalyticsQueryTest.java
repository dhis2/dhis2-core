/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for "/analytics" endpoint.
 *
 * @author dusan bernat
 */
public class AnalyticsQueryTest extends AnalyticsApiTest {
  private RestApiActions analyticsActions;

  @BeforeAll
  public void setup() {
    analyticsActions = new RestApiActions("analytics");
  }

  @Test
  public void query1And3CoverageYearly() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:THIS_YEAR")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:Uvn6LCg7dVU;sB79w2hiLp8,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
            .add("relativePeriodDate=2022-01-01");
    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(28)))
        .body("height", equalTo(28))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    assertEquals(
        response.extract("metaData").toString().replaceAll(" ", ""),
        "{items={sB79w2hiLp8={name=ANC 3 Coverage}, jUb8gELQApl={name=Kailahun}, TEQlaapDQoK={name=Port Loko}, eIQbndfxQMb={name=Tonkolili}, Vth0fbpFcsO={name=Kono}, PMa2VCrupOd={name=Kambia}, ou={name=Organisation unit}, THIS_YEAR={name=This year}, O6uvpzGd5pu={name=Bo}, bL4ooGhyHRQ={name=Pujehun}, 2022={name=2022}, kJq2mPyFEHo={name=Kenema}, fdc6uOvgoji={name=Bombali}, ImspTQPwCqd={name=Sierra Leone}, at6UHUQatSo={name=Western Area}, dx={name=Data}, pe={name=Period}, Uvn6LCg7dVU={name=ANC 1 Coverage}, lc3eMKXaEfw={name=Bonthe}, qhqAxPSTUXp={name=Koinadugu}, jmIPBj66vD6={name=Moyamba}}, dimensions={dx=[Uvn6LCg7dVU,sB79w2hiLp8], pe=[2022], ou=[ImspTQPwCqd,O6uvpzGd5pu,fdc6uOvgoji,lc3eMKXaEfw,jUb8gELQApl,PMa2VCrupOd,kJq2mPyFEHo,qhqAxPSTUXp,Vth0fbpFcsO,jmIPBj66vD6,TEQlaapDQoK,bL4ooGhyHRQ,eIQbndfxQMb,at6UHUQatSo], co=[]}}"
            .replaceAll(" ", ""));
    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("Uvn6LCg7dVU", "ImspTQPwCqd", "101.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "O6uvpzGd5pu", "142.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "fdc6uOvgoji", "82.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "lc3eMKXaEfw", "90.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jUb8gELQApl", "81.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "PMa2VCrupOd", "102.9"));
    validateRow(response, List.of("Uvn6LCg7dVU", "kJq2mPyFEHo", "94.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "qhqAxPSTUXp", "67.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Vth0fbpFcsO", "52.8"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jmIPBj66vD6", "118.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "TEQlaapDQoK", "99.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "bL4ooGhyHRQ", "88.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "eIQbndfxQMb", "124.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "at6UHUQatSo", "124.7"));
    validateRow(response, List.of("sB79w2hiLp8", "ImspTQPwCqd", "65.8"));
    validateRow(response, List.of("sB79w2hiLp8", "O6uvpzGd5pu", "92.3"));
    validateRow(response, List.of("sB79w2hiLp8", "fdc6uOvgoji", "51.0"));
    validateRow(response, List.of("sB79w2hiLp8", "lc3eMKXaEfw", "59.7"));
    validateRow(response, List.of("sB79w2hiLp8", "jUb8gELQApl", "71.0"));
    validateRow(response, List.of("sB79w2hiLp8", "PMa2VCrupOd", "65.2"));
    validateRow(response, List.of("sB79w2hiLp8", "kJq2mPyFEHo", "86.8"));
    validateRow(response, List.of("sB79w2hiLp8", "qhqAxPSTUXp", "38.8"));
    validateRow(response, List.of("sB79w2hiLp8", "Vth0fbpFcsO", "36.9"));
    validateRow(response, List.of("sB79w2hiLp8", "jmIPBj66vD6", "92.4"));
    validateRow(response, List.of("sB79w2hiLp8", "TEQlaapDQoK", "47.8"));
    validateRow(response, List.of("sB79w2hiLp8", "bL4ooGhyHRQ", "56.9"));
    validateRow(response, List.of("sB79w2hiLp8", "eIQbndfxQMb", "58.7"));
    validateRow(response, List.of("sB79w2hiLp8", "at6UHUQatSo", "72.8"));
  }

  @Test
  public void testAnalyticsGetWithTextDataElementAggregationTypeNone() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=dx:M3xtLkYBlKI.fyjPqlHE7Dn,pe:202107")
            .add("filter=ou:USER_ORGUNIT")
            .add("displayProperty=NAME")
            .add("skipMeta=true")
            .add("skipData=false");

    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response.validate().statusCode(200).body("rows", hasSize(equalTo(2)));

    validateRow(response, List.of("M3xtLkYBlKI.fyjPqlHE7Dn", "202107", ""));

    validateRow(
        response, List.of("M3xtLkYBlKI.fyjPqlHE7Dn", "202107", "Some insecticide resistance"));
  }

  @Test
  public void testAnalyticsGetWithLongTextDataElementAggregationTypeSum() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=dx:mxc1T932aWM,pe:202210")
            .add("filter=ou:USER_ORGUNIT")
            .add("displayProperty=NAME")
            .add("desc=lastupdated")
            .add("skipMeta=true")
            .add("skipData=false");

    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response.validate().statusCode(200).body("rows", hasSize(equalTo(1)));

    validateRow(
        response,
        0,
        List.of(
            "mxc1T932aWM",
            "202210",
            "Cholera is an infection of the small intestine caused by the bacterium Vibrio cholerae.\n\nThe main symptoms are watery diarrhea and vomiting. This may result in dehydration and in severe cases grayish-bluish skin.[1] Transmission occurs primarily by drinking water or eating food that has been contaminated by the feces (waste product) of an infected person, including one with no apparent symptoms.\n\nThe severity of the diarrhea and vomiting can lead to rapid dehydration and electrolyte imbalance, and death in some cases. The primary treatment is oral rehydration therapy, typically with oral rehydration solution, to replace water and electrolytes. If this is not tolerated or does not provide improvement fast enough, intravenous fluids can also be used. Antibacterial drugs are beneficial in those with severe disease to shorten its duration and severity."));
  }
}
