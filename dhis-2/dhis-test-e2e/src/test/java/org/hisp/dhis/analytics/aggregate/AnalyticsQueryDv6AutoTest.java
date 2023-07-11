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

/** Groups e2e tests for "/analytics" aggregate endpoint. */
public class AnalyticsQueryDv6AutoTest extends AnalyticsApiTest {
  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void queryAncVisitsByFixedoutreachThisYear() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:ImspTQPwCqd,pe:THIS_YEAR")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=dx:fbfJHSPpUQD;cYeuwXTCPkU;Jtf34kNZhzP,fMZEcRHuamy:qkPbeWaFsnU;wbrDrL2aYEc")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(6)))
        .body("height", equalTo(6))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    assertEquals(
        response.extract("metaData").toString().replaceAll(" ", ""),
        "{items={fMZEcRHuamy={name=Location Fixed/Outreach}, ou={name=Organisation unit}, THIS_YEAR={name=This year}, qkPbeWaFsnU={name=Fixed}, 2022={name=2022}, fbfJHSPpUQD={name=ANC 1st visit}, wbrDrL2aYEc={name=Outreach}, ImspTQPwCqd={name=Sierra Leone}, dx={name=Data}, pq2XI5kz2BY={name=Fixed}, pe={name=Period}, cYeuwXTCPkU={name=ANC 2nd visit}, Jtf34kNZhzP={name=ANC 3rd visit}, PT59n8BQbqM={name=Outreach}}, dimensions={dx=[fbfJHSPpUQD,cYeuwXTCPkU,Jtf34kNZhzP], pe=[2022], fMZEcRHuamy=[qkPbeWaFsnU,wbrDrL2aYEc], ou=[ImspTQPwCqd], co=[pq2XI5kz2BY,PT59n8BQbqM]}}"
            .replaceAll(" ", ""));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "fMZEcRHuamy",
        "Location Fixed/Outreach",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("cYeuwXTCPkU", "wbrDrL2aYEc", "49255"));
    validateRow(response, List.of("Jtf34kNZhzP", "wbrDrL2aYEc", "37279"));
    validateRow(response, List.of("cYeuwXTCPkU", "qkPbeWaFsnU", "184027"));
    validateRow(response, List.of("Jtf34kNZhzP", "qkPbeWaFsnU", "126539"));
    validateRow(response, List.of("fbfJHSPpUQD", "wbrDrL2aYEc", "52483"));
    validateRow(response, List.of("fbfJHSPpUQD", "qkPbeWaFsnU", "199665"));
  }

  @Test
  public void queryAncBirthsAndBcg() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("filter=ou:fdc6uOvgoji")
            .add("skipData=false")
            .add("includeNumDen=false")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add("dimension=dx:Uvn6LCg7dVU;sB79w2hiLp8;ulgL07PF8rq;FnYCr2EAzWS,pe:THIS_YEAR")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(4)))
        .body("height", equalTo(4))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    assertEquals(
        response.extract("metaData").toString().replaceAll(" ", ""),
        "{items={sB79w2hiLp8={name=ANC 3 Coverage}, dx={name=Data}, pe={name=Period}, ou={name=Organisation unit}, Uvn6LCg7dVU={name=ANC 1 Coverage}, THIS_YEAR={name=This year}, 2022={name=2022}, FnYCr2EAzWS={name=BCG Coverage <1y}, ulgL07PF8rq={name=Births attended by skilled health personnel (registered live births)}, fdc6uOvgoji={name=Bombali}}, dimensions={dx=[Uvn6LCg7dVU,sB79w2hiLp8,ulgL07PF8rq,FnYCr2EAzWS], pe=[2022], ou=[fdc6uOvgoji], co=[]}}"
            .replaceAll(" ", ""));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(response, List.of("Uvn6LCg7dVU", "2022", "82.2"));
    validateRow(response, List.of("sB79w2hiLp8", "2022", "51.0"));
    validateRow(response, List.of("ulgL07PF8rq", "2022", "60.0"));
    validateRow(response, List.of("FnYCr2EAzWS", "2022", "22.7"));
  }

  @Test
  public void queryByFacilityType() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=dx:V37YqbqpEhV;SA7WeFZnUci;rbkr8PL0rwM;ybzlGLjWwnK,J5jldMd8OHv:uYxK4wmcPqA;tDZVQ1WtwpA;EYbopBOJWsW;RXL3lPSK8oG;CXw2yu5fodb,pe:THIS_YEAR")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(20)))
        .body("height", equalTo(20))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    assertEquals(
        response.extract("metaData").toString().replaceAll(" ", ""),
        "{items={RXL3lPSK8oG={name=Clinic}, J5jldMd8OHv={name=Facility Type}, CXw2yu5fodb={name=CHC}, THIS_YEAR={name=This year}, rbkr8PL0rwM={name=Iron Folate given at ANC 3rd}, 2022={name=2022}, SA7WeFZnUci={name=IPT 2nd dose given by TBA}, uYxK4wmcPqA={name=CHP}, tDZVQ1WtwpA={name=Hospital}, dx={name=Data}, pq2XI5kz2BY={name=Fixed}, pe={name=Period}, PT59n8BQbqM={name=Outreach}, EYbopBOJWsW={name=MCHP}, ybzlGLjWwnK={name=LLITN given at ANC 1st}, V37YqbqpEhV={name=IPT 2nd dose given at PHU}}, dimensions={dx=[V37YqbqpEhV,SA7WeFZnUci,rbkr8PL0rwM,ybzlGLjWwnK], pe=[2022], J5jldMd8OHv=[uYxK4wmcPqA,tDZVQ1WtwpA,EYbopBOJWsW,RXL3lPSK8oG,CXw2yu5fodb], co=[pq2XI5kz2BY,PT59n8BQbqM]}}"
            .replaceAll(" ", ""));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "J5jldMd8OHv", "Facility Type", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 5, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 7, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 8, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response, List.of("rbkr8PL0rwM", "RXL3lPSK8oG", "2022", "64540", "", "", "", "", ""));
    validateRow(response, List.of("SA7WeFZnUci", "tDZVQ1WtwpA", "2022", "37", "", "", "", "", ""));
    validateRow(
        response, List.of("rbkr8PL0rwM", "uYxK4wmcPqA", "2022", "423046", "", "", "", "", ""));
    validateRow(response, List.of("SA7WeFZnUci", "RXL3lPSK8oG", "2022", "454", "", "", "", "", ""));
    validateRow(
        response, List.of("rbkr8PL0rwM", "tDZVQ1WtwpA", "2022", "46983", "", "", "", "", ""));
    validateRow(
        response, List.of("SA7WeFZnUci", "uYxK4wmcPqA", "2022", "5260", "", "", "", "", ""));
    validateRow(
        response, List.of("ybzlGLjWwnK", "RXL3lPSK8oG", "2022", "3629", "", "", "", "", ""));
    validateRow(
        response, List.of("rbkr8PL0rwM", "EYbopBOJWsW", "2022", "868626", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "CXw2yu5fodb", "2022", "60635", "", "", "", "", ""));
    validateRow(
        response, List.of("SA7WeFZnUci", "EYbopBOJWsW", "2022", "13587", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "uYxK4wmcPqA", "2022", "35724", "", "", "", "", ""));
    validateRow(
        response, List.of("ybzlGLjWwnK", "CXw2yu5fodb", "2022", "36822", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "tDZVQ1WtwpA", "2022", "5654", "", "", "", "", ""));
    validateRow(
        response, List.of("ybzlGLjWwnK", "tDZVQ1WtwpA", "2022", "2095", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "RXL3lPSK8oG", "2022", "6534", "", "", "", "", ""));
    validateRow(
        response, List.of("ybzlGLjWwnK", "EYbopBOJWsW", "2022", "42615", "", "", "", "", ""));
    validateRow(
        response, List.of("ybzlGLjWwnK", "uYxK4wmcPqA", "2022", "17538", "", "", "", "", ""));
    validateRow(
        response, List.of("rbkr8PL0rwM", "CXw2yu5fodb", "2022", "736763", "", "", "", "", ""));
    validateRow(
        response, List.of("V37YqbqpEhV", "EYbopBOJWsW", "2022", "81329", "", "", "", "", ""));
    validateRow(
        response, List.of("SA7WeFZnUci", "CXw2yu5fodb", "2022", "4398", "", "", "", "", ""));
  }

  @Test
  public void queryCoveragesAndReportingByOrgunitLastYear() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=pe:THIS_YEAR,dx:Uvn6LCg7dVU;sB79w2hiLp8,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(28)))
        .body("height", equalTo(28))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    assertEquals(
        response.extract("metaData").toString().replaceAll(" ", ""),
        "{items={sB79w2hiLp8={name=ANC 3 Coverage}, jUb8gELQApl={name=Kailahun}, TEQlaapDQoK={name=Port Loko}, eIQbndfxQMb={name=Tonkolili}, Vth0fbpFcsO={name=Kono}, PMa2VCrupOd={name=Kambia}, ou={name=Organisation unit}, THIS_YEAR={name=This year}, 2022={name=2022}, O6uvpzGd5pu={name=Bo}, bL4ooGhyHRQ={name=Pujehun}, kJq2mPyFEHo={name=Kenema}, fdc6uOvgoji={name=Bombali}, ImspTQPwCqd={name=Sierra Leone}, at6UHUQatSo={name=Western Area}, dx={name=Data}, pe={name=Period}, Uvn6LCg7dVU={name=ANC 1 Coverage}, lc3eMKXaEfw={name=Bonthe}, qhqAxPSTUXp={name=Koinadugu}, jmIPBj66vD6={name=Moyamba}}, dimensions={dx=[Uvn6LCg7dVU,sB79w2hiLp8], pe=[2022], ou=[ImspTQPwCqd,O6uvpzGd5pu,fdc6uOvgoji,lc3eMKXaEfw,jUb8gELQApl,PMa2VCrupOd,kJq2mPyFEHo,qhqAxPSTUXp,Vth0fbpFcsO,jmIPBj66vD6,TEQlaapDQoK,bL4ooGhyHRQ,eIQbndfxQMb,at6UHUQatSo], co=[]}}"
            .replaceAll(" ", ""));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 5, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 7, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 8, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "ImspTQPwCqd",
            "101.5",
            "252510.0",
            "248854.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "O6uvpzGd5pu",
            "142.3",
            "33398.0",
            "23475.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "fdc6uOvgoji",
            "82.2",
            "17031.0",
            "20721.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "lc3eMKXaEfw",
            "90.0",
            "6703.0",
            "7448.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "jUb8gELQApl",
            "81.6",
            "15145.0",
            "18565.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "PMa2VCrupOd",
            "102.9",
            "14320.0",
            "13921.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "kJq2mPyFEHo",
            "94.4",
            "24347.0",
            "25785.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "qhqAxPSTUXp",
            "67.0",
            "9084.0",
            "13568.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "Vth0fbpFcsO",
            "52.8",
            "9254.0",
            "17540.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "jmIPBj66vD6",
            "118.4",
            "16169.0",
            "13655.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "TEQlaapDQoK",
            "99.5",
            "23773.0",
            "23892.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "bL4ooGhyHRQ",
            "88.5",
            "10662.0",
            "12041.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "eIQbndfxQMb",
            "124.7",
            "22505.0",
            "18048.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "at6UHUQatSo",
            "124.7",
            "50119.0",
            "40195.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "ImspTQPwCqd",
            "65.8",
            "163818.0",
            "248854.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "O6uvpzGd5pu",
            "92.3",
            "21679.0",
            "23475.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "fdc6uOvgoji",
            "51.0",
            "10558.0",
            "20721.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "lc3eMKXaEfw",
            "59.7",
            "4449.0",
            "7448.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "jUb8gELQApl",
            "71.0",
            "13175.0",
            "18565.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "PMa2VCrupOd",
            "65.2",
            "9074.0",
            "13921.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "kJq2mPyFEHo",
            "86.8",
            "22378.0",
            "25785.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "qhqAxPSTUXp",
            "38.8",
            "5259.0",
            "13568.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "Vth0fbpFcsO",
            "36.9",
            "6478.0",
            "17540.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "jmIPBj66vD6",
            "92.4",
            "12620.0",
            "13655.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "TEQlaapDQoK",
            "47.8",
            "11422.0",
            "23892.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "bL4ooGhyHRQ",
            "56.9",
            "6856.0",
            "12041.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "eIQbndfxQMb",
            "58.7",
            "10589.0",
            "18048.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "at6UHUQatSo",
            "72.8",
            "29281.0",
            "40195.0",
            "100.0",
            "36500",
            "365"));
  }

  @Test
  public void queryCoveragesAndReportingByOrgunitLastYear2() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=pe:THIS_YEAR,dx:Uvn6LCg7dVU;sB79w2hiLp8,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(28)))
        .body("height", equalTo(28))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    assertEquals(
        response.extract("metaData").toString().replaceAll(" ", ""),
        "{items={sB79w2hiLp8={name=ANC 3 Coverage}, jUb8gELQApl={name=Kailahun}, TEQlaapDQoK={name=Port Loko}, eIQbndfxQMb={name=Tonkolili}, Vth0fbpFcsO={name=Kono}, PMa2VCrupOd={name=Kambia}, ou={name=Organisation unit}, THIS_YEAR={name=This year}, 2022={name=2022}, O6uvpzGd5pu={name=Bo}, bL4ooGhyHRQ={name=Pujehun}, kJq2mPyFEHo={name=Kenema}, fdc6uOvgoji={name=Bombali}, ImspTQPwCqd={name=Sierra Leone}, at6UHUQatSo={name=Western Area}, dx={name=Data}, pe={name=Period}, Uvn6LCg7dVU={name=ANC 1 Coverage}, lc3eMKXaEfw={name=Bonthe}, qhqAxPSTUXp={name=Koinadugu}, jmIPBj66vD6={name=Moyamba}}, dimensions={dx=[Uvn6LCg7dVU,sB79w2hiLp8], pe=[2022], ou=[ImspTQPwCqd,O6uvpzGd5pu,fdc6uOvgoji,lc3eMKXaEfw,jUb8gELQApl,PMa2VCrupOd,kJq2mPyFEHo,qhqAxPSTUXp,Vth0fbpFcsO,jmIPBj66vD6,TEQlaapDQoK,bL4ooGhyHRQ,eIQbndfxQMb,at6UHUQatSo], co=[]}}"
            .replaceAll(" ", ""));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 5, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 7, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 8, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "ImspTQPwCqd",
            "101.5",
            "252510.0",
            "248854.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "O6uvpzGd5pu",
            "142.3",
            "33398.0",
            "23475.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "fdc6uOvgoji",
            "82.2",
            "17031.0",
            "20721.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "lc3eMKXaEfw",
            "90.0",
            "6703.0",
            "7448.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "jUb8gELQApl",
            "81.6",
            "15145.0",
            "18565.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "PMa2VCrupOd",
            "102.9",
            "14320.0",
            "13921.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "kJq2mPyFEHo",
            "94.4",
            "24347.0",
            "25785.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "qhqAxPSTUXp",
            "67.0",
            "9084.0",
            "13568.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "Vth0fbpFcsO",
            "52.8",
            "9254.0",
            "17540.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "jmIPBj66vD6",
            "118.4",
            "16169.0",
            "13655.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "TEQlaapDQoK",
            "99.5",
            "23773.0",
            "23892.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "bL4ooGhyHRQ",
            "88.5",
            "10662.0",
            "12041.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "eIQbndfxQMb",
            "124.7",
            "22505.0",
            "18048.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "2022",
            "at6UHUQatSo",
            "124.7",
            "50119.0",
            "40195.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "ImspTQPwCqd",
            "65.8",
            "163818.0",
            "248854.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "O6uvpzGd5pu",
            "92.3",
            "21679.0",
            "23475.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "fdc6uOvgoji",
            "51.0",
            "10558.0",
            "20721.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "lc3eMKXaEfw",
            "59.7",
            "4449.0",
            "7448.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "jUb8gELQApl",
            "71.0",
            "13175.0",
            "18565.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "PMa2VCrupOd",
            "65.2",
            "9074.0",
            "13921.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "kJq2mPyFEHo",
            "86.8",
            "22378.0",
            "25785.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "qhqAxPSTUXp",
            "38.8",
            "5259.0",
            "13568.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "Vth0fbpFcsO",
            "36.9",
            "6478.0",
            "17540.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "jmIPBj66vD6",
            "92.4",
            "12620.0",
            "13655.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "TEQlaapDQoK",
            "47.8",
            "11422.0",
            "23892.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "bL4ooGhyHRQ",
            "56.9",
            "6856.0",
            "12041.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "eIQbndfxQMb",
            "58.7",
            "10589.0",
            "18048.0",
            "100.0",
            "36500",
            "365"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "2022",
            "at6UHUQatSo",
            "72.8",
            "29281.0",
            "40195.0",
            "100.0",
            "36500",
            "365"));
  }

  @Test
  public void queryCoveragesQuarterly() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("skipData=false")
            .add("includeNumDen=true")
            .add("displayProperty=NAME")
            .add("skipMeta=false")
            .add(
                "dimension=dx:Uvn6LCg7dVU;OdiHJayrsKo;sB79w2hiLp8,ou:ImspTQPwCqd,pe:LAST_4_QUARTERS")
            .add("relativePeriodDate=2022-01-01");

    // When
    ApiResponse response = actions.get(params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(12)))
        .body("height", equalTo(12))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

    // Assert metaData.
    assertEquals(
        response.extract("metaData").toString().replaceAll(" ", ""),
        "{items={2021Q2={name=April - June 2021}, ImspTQPwCqd={name=Sierra Leone}, 2021Q3={name=July - September 2021}, sB79w2hiLp8={name=ANC 3 Coverage}, 2021Q1={name=January - March 2021}, dx={name=Data}, pe={name=Period}, LAST_4_QUARTERS={name=Last 4 quarters}, ou={name=Organisation unit}, Uvn6LCg7dVU={name=ANC 1 Coverage}, OdiHJayrsKo={name=ANC 2 Coverage}, 2021Q4={name=October - December 2021}}, dimensions={dx=[Uvn6LCg7dVU,OdiHJayrsKo,sB79w2hiLp8], pe=[2021Q1,2021Q2,2021Q3,2021Q4], ou=[ImspTQPwCqd], co=[]}}"
            .replaceAll(" ", ""));

    // Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 4, "numerator", "Numerator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 5, "denominator", "Denominator", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(
        response, 7, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 8, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

    // Assert rows.
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "ImspTQPwCqd",
            "2021Q1",
            "100.9",
            "60689.0",
            "243951.0",
            "405.6",
            "36500",
            "90"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "ImspTQPwCqd",
            "2021Q2",
            "118.1",
            "71850.0",
            "243951.0",
            "401.1",
            "36500",
            "91"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "ImspTQPwCqd",
            "2021Q3",
            "108.4",
            "66668.0",
            "243951.0",
            "396.7",
            "36500",
            "92"));
    validateRow(
        response,
        List.of(
            "Uvn6LCg7dVU",
            "ImspTQPwCqd",
            "2021Q4",
            "87.9",
            "54062.0",
            "243951.0",
            "396.7",
            "36500",
            "92"));
    validateRow(
        response,
        List.of(
            "OdiHJayrsKo",
            "ImspTQPwCqd",
            "2021Q1",
            "92.0",
            "55331.0",
            "243951.0",
            "405.6",
            "36500",
            "90"));
    validateRow(
        response,
        List.of(
            "OdiHJayrsKo",
            "ImspTQPwCqd",
            "2021Q2",
            "108.6",
            "66033.0",
            "243951.0",
            "401.1",
            "36500",
            "91"));
    validateRow(
        response,
        List.of(
            "OdiHJayrsKo",
            "ImspTQPwCqd",
            "2021Q3",
            "100.8",
            "61976.0",
            "243951.0",
            "396.7",
            "36500",
            "92"));
    validateRow(
        response,
        List.of(
            "OdiHJayrsKo",
            "ImspTQPwCqd",
            "2021Q4",
            "82.5",
            "50749.0",
            "243951.0",
            "396.7",
            "36500",
            "92"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "ImspTQPwCqd",
            "2021Q1",
            "62.9",
            "37816.0",
            "243951.0",
            "405.6",
            "36500",
            "90"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "ImspTQPwCqd",
            "2021Q2",
            "72.1",
            "43854.0",
            "243951.0",
            "401.1",
            "36500",
            "91"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "ImspTQPwCqd",
            "2021Q3",
            "72.9",
            "44820.0",
            "243951.0",
            "396.7",
            "36500",
            "92"));
    validateRow(
        response,
        List.of(
            "sB79w2hiLp8",
            "ImspTQPwCqd",
            "2021Q4",
            "60.7",
            "37336.0",
            "243951.0",
            "396.7",
            "36500",
            "92"));
  }
}
