/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

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
  public void singleValueWithMultiplePeriodTypes() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=pe:LAST_12_MONTHS;TODAY")
            .add("filter=ou:USER_ORGUNIT")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=SHORTNAME")
            .add("skipMeta=true")
            .add("dimension=dx:FTRrcoaog83")
            .add("relativePeriodDate=2022-01-01");
    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert rows.
    validateRow(response, List.of("FTRrcoaog83", "46"));
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
        "{items={sB79w2hiLp8={name=ANC 3 Coverage}, jUb8gELQApl={name=Kailahun}, TEQlaapDQoK={name=Port Loko}, eIQbndfxQMb={name=Tonkolili}, Vth0fbpFcsO={name=Kono}, PMa2VCrupOd={name=Kambia}, ou={name=Organisation unit}, USER_ORGUNIT={organisationUnits=[ImspTQPwCqd]}, THIS_YEAR={name=This year}, O6uvpzGd5pu={name=Bo}, bL4ooGhyHRQ={name=Pujehun}, 2022={name=2022}, kJq2mPyFEHo={name=Kenema}, USER_ORGUNIT_CHILDREN={organisationUnits=[at6UHUQatSo, TEQlaapDQoK, PMa2VCrupOd, qhqAxPSTUXp, kJq2mPyFEHo, jmIPBj66vD6, Vth0fbpFcsO, jUb8gELQApl, fdc6uOvgoji, eIQbndfxQMb, O6uvpzGd5pu, lc3eMKXaEfw, bL4ooGhyHRQ]}, fdc6uOvgoji={name=Bombali}, ImspTQPwCqd={name=Sierra Leone}, at6UHUQatSo={name=Western Area}, dx={name=Data}, pe={name=Period}, Uvn6LCg7dVU={name=ANC 1 Coverage}, lc3eMKXaEfw={name=Bonthe}, qhqAxPSTUXp={name=Koinadugu}, jmIPBj66vD6={name=Moyamba}}, dimensions={dx=[Uvn6LCg7dVU, sB79w2hiLp8], pe=[2022], ou=[ImspTQPwCqd, O6uvpzGd5pu, fdc6uOvgoji, lc3eMKXaEfw, jUb8gELQApl, PMa2VCrupOd, kJq2mPyFEHo, qhqAxPSTUXp, Vth0fbpFcsO, jmIPBj66vD6, TEQlaapDQoK, bL4ooGhyHRQ, eIQbndfxQMb, at6UHUQatSo], co=[]}}"
            .replaceAll(" ", ""),
        response.extract("metaData").toString().replaceAll(" ", ""));
    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("Uvn6LCg7dVU", "ImspTQPwCqd", "101.47"));
    validateRow(response, List.of("Uvn6LCg7dVU", "O6uvpzGd5pu", "142.27"));
    validateRow(response, List.of("Uvn6LCg7dVU", "fdc6uOvgoji", "82.19"));
    validateRow(response, List.of("Uvn6LCg7dVU", "lc3eMKXaEfw", "90.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jUb8gELQApl", "81.58"));
    validateRow(response, List.of("Uvn6LCg7dVU", "PMa2VCrupOd", "102.87"));
    validateRow(response, List.of("Uvn6LCg7dVU", "kJq2mPyFEHo", "94.42"));
    validateRow(response, List.of("Uvn6LCg7dVU", "qhqAxPSTUXp", "66.95"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Vth0fbpFcsO", "52.76"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jmIPBj66vD6", "118.41"));
    validateRow(response, List.of("Uvn6LCg7dVU", "TEQlaapDQoK", "99.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "bL4ooGhyHRQ", "88.55"));
    validateRow(response, List.of("Uvn6LCg7dVU", "eIQbndfxQMb", "124.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "at6UHUQatSo", "124.69"));
    validateRow(response, List.of("sB79w2hiLp8", "ImspTQPwCqd", "65.83"));
    validateRow(response, List.of("sB79w2hiLp8", "O6uvpzGd5pu", "92.35"));
    validateRow(response, List.of("sB79w2hiLp8", "fdc6uOvgoji", "50.95"));
    validateRow(response, List.of("sB79w2hiLp8", "lc3eMKXaEfw", "59.73"));
    validateRow(response, List.of("sB79w2hiLp8", "jUb8gELQApl", "70.97"));
    validateRow(response, List.of("sB79w2hiLp8", "PMa2VCrupOd", "65.18"));
    validateRow(response, List.of("sB79w2hiLp8", "kJq2mPyFEHo", "86.79"));
    validateRow(response, List.of("sB79w2hiLp8", "qhqAxPSTUXp", "38.76"));
    validateRow(response, List.of("sB79w2hiLp8", "Vth0fbpFcsO", "36.93"));
    validateRow(response, List.of("sB79w2hiLp8", "jmIPBj66vD6", "92.42"));
    validateRow(response, List.of("sB79w2hiLp8", "TEQlaapDQoK", "47.81"));
    validateRow(response, List.of("sB79w2hiLp8", "bL4ooGhyHRQ", "56.94"));
    validateRow(response, List.of("sB79w2hiLp8", "eIQbndfxQMb", "58.67"));
    validateRow(response, List.of("sB79w2hiLp8", "at6UHUQatSo", "72.85"));
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

    validateRow(response, List.of("M3xtLkYBlKI.fyjPqlHE7Dn", "202107", ""));
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

  @Test
  public void testQueryFailsGracefullyIfMultipleQueries() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "dimension=cX5k9anHEHd:apsOixVZlf1;jRbMi0aBjYn,dx:luLGbE2WKGP;nq5ohBSWj6E,pe:LAST_12_MONTHS")
            .add("filter=ou:USER_ORGUNIT")
            .add("displayProperty=SHORTNAME");

    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response.validate().statusCode(200);
  }

  @Test
  public void testQueryForReportingRatesWithMeasureCriteriaLT() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=dx:PLq9sJluXvc.REPORTING_RATE,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
            .add("filter=pe:LAST_5_YEARS")
            .add("displayProperty=NAME")
            .add("measureCriteria=LT:25")
            .add("includeNumDen=true")
            .add("relativePeriodDate=2024-04-25")
            .add("skipMeta=true")
            .add("skipData=false");

    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response.validate().statusCode(200).body("rows", hasSize(equalTo(3)));

    validateRow(
        response,
        List.of(
            "PLq9sJluXvc.REPORTING_RATE",
            "bL4ooGhyHRQ",
            "24.82",
            "983.0",
            "3960.0",
            "100",
            "",
            ""));

    validateRow(
        response,
        List.of(
            "PLq9sJluXvc.REPORTING_RATE",
            "at6UHUQatSo",
            "24.71",
            "1542.0",
            "6240.0",
            "100",
            "",
            ""));

    validateRow(
        response,
        List.of(
            "PLq9sJluXvc.REPORTING_RATE",
            "at6UHUQatSo",
            "24.71",
            "1542.0",
            "6240.0",
            "100",
            "",
            ""));
  }

  @Test
  public void testQueryForReportingRatesWithMeasureCriteriaGT_NoNumeratorDenominator() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=dx:PLq9sJluXvc.REPORTING_RATE,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
            .add("filter=pe:LAST_5_YEARS")
            .add("displayProperty=NAME")
            .add("measureCriteria=GT:25")
            .add("includeNumDen=false")
            .add("relativePeriodDate=2024-04-25")
            .add("skipMeta=true")
            .add("skipData=false");

    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response.validate().statusCode(200).body("rows", hasSize(equalTo(11)));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "Vth0fbpFcsO", "25.69"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "PMa2VCrupOd", "25.27"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "O6uvpzGd5pu", "25.56"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "lc3eMKXaEfw", "25.55"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "qhqAxPSTUXp", "25.57"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "ImspTQPwCqd", "25.21"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "fdc6uOvgoji", "25.38"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "jmIPBj66vD6", "25.34"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "jUb8gELQApl", "25.19"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "eIQbndfxQMb", "25.11"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "TEQlaapDQoK", "25.09"));
  }

  @Test
  public void testQueryForReportingRatesWithMeasureCriteriaGE_NoNumeratorDenominator() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=dx:PLq9sJluXvc.REPORTING_RATE,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
            .add("filter=pe:LAST_5_YEARS")
            .add("displayProperty=NAME")
            .add("measureCriteria=GE:25.55")
            .add("includeNumDen=false")
            .add("relativePeriodDate=2024-04-25")
            .add("skipMeta=true")
            .add("skipData=false");

    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response.validate().statusCode(200).body("rows", hasSize(equalTo(4)));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "Vth0fbpFcsO", "25.69"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "O6uvpzGd5pu", "25.56"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "lc3eMKXaEfw", "25.55"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "qhqAxPSTUXp", "25.57"));
  }

  @Test
  public void testQueryForReportingRatesWithMeasureCriteriaLT_Negative() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=dx:PLq9sJluXvc.REPORTING_RATE,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
            .add("filter=pe:LAST_5_YEARS")
            .add("displayProperty=NAME")
            .add("measureCriteria=LT:-5")
            .add("includeNumDen=true")
            .add("relativePeriodDate=2024-04-25")
            .add("skipMeta=true")
            .add("skipData=false");

    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response.validate().statusCode(200).body("rows", hasSize(equalTo(0)));
  }

  @Test
  public void testQueryForReportingRatesWithMeasureCriteriaEQ_WithRounding() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=dx:PLq9sJluXvc.REPORTING_RATE,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
            .add("filter=pe:LAST_5_YEARS")
            .add("displayProperty=NAME")
            .add("measureCriteria=EQ:25.11")
            .add("includeNumDen=true")
            .add("skipRounding=false")
            .add("relativePeriodDate=2024-04-25")
            .add("skipMeta=true")
            .add("skipData=false");

    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response.validate().statusCode(200).body("rows", hasSize(equalTo(1)));

    validateRow(
        response,
        List.of(
            "PLq9sJluXvc.REPORTING_RATE",
            "eIQbndfxQMb",
            "25.11",
            "1401.0",
            "5580.0",
            "100",
            "",
            ""));
  }

  @Test
  public void testQueryForReportingRatesWithMeasureCriteriaEQ_SkipRounding() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("dimension=dx:PLq9sJluXvc.REPORTING_RATE,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
            .add("filter=pe:LAST_5_YEARS")
            .add("displayProperty=NAME")
            .add("measureCriteria=EQ:25.268817204301076")
            .add("skipRounding=true")
            .add("relativePeriodDate=2024-04-25")
            .add("skipMeta=true")
            .add("skipData=false");

    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response.validate().statusCode(200).body("rows", hasSize(equalTo(1)));

    validateRow(
        response, List.of("PLq9sJluXvc.REPORTING_RATE", "PMa2VCrupOd", "25.268817204301076"));
  }

  @Test
  public void testQueryForReportingRatesWithMeasureCriteriaEQ_WithZerosAsResult() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "dimension=pe:LAST_YEAR,dx:PLq9sJluXvc.REPORTING_RATE,ou:ImspTQPwCqd;O6uvpzGd5pu;fdc6uOvgoji;lc3eMKXaEfw;jUb8gELQApl;PMa2VCrupOd;kJq2mPyFEHo;qhqAxPSTUXp")
            .add("displayProperty=NAME")
            .add("measureCriteria=EQ:0")
            .add("relativePeriodDate=2024-04-25")
            .add("skipMeta=true");

    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response.validate().statusCode(200).body("rows", hasSize(equalTo(8)));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "2023", "O6uvpzGd5pu", "0"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "2023", "qhqAxPSTUXp", "0"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "2023", "fdc6uOvgoji", "0"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "2023", "jUb8gELQApl", "0"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "2023", "lc3eMKXaEfw", "0"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "2023", "kJq2mPyFEHo", "0"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "2023", "ImspTQPwCqd", "0"));

    validateRow(response, List.of("PLq9sJluXvc.REPORTING_RATE", "2023", "PMa2VCrupOd", "0"));
  }

  @Test
  public void queryProgramTrackedEntityAttributeDimensionItemValueType() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:USER_ORGUNIT")
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:IpHINAT79UW.w75KJ2mc4zz,pe:LAST_YEAR")
            .add("relativePeriodDate=2024-05-22");

    // When
    ApiResponse response = analyticsActions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(19)))
        .body("height", equalTo(19))
        .body("width", equalTo(8))
        .body("headerWidth", equalTo(8));

    // Assert metaData.
    String expectedMetaData =
        "{\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"ou\":{\"name\":\"Organisation unit\"},\"2023\":{\"name\":\"2023\"},\"LAST_YEAR\":{\"name\":\"Last year\"},\"IpHINAT79UW.w75KJ2mc4zz\":{\"name\":\"Child Programme First name\"}},\"dimensions\":{\"dx\":[\"IpHINAT79UW.w75KJ2mc4zz\"],\"pe\":[\"2023\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    JSONAssert.assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 3, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 5, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 6, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 7, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
    validateRow(response, List.of("IpHINAT79UW.w75KJ2mc4zz", "2023", "", "", "", "", "", ""));
  }
}
