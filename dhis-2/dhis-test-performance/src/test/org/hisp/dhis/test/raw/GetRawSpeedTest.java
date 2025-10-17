/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.test.raw;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hisp.dhis.test.raw.TestDefinitions.BASELINE;
import static org.hisp.dhis.test.raw.TestDefinitions.constantSingleUser;
import static org.hisp.dhis.test.raw.TestDefinitions.defaultHttpProtocol;
import static org.hisp.dhis.test.raw.TestHelper.fakePopulationBuilder;
import static org.hisp.dhis.test.raw.TestHelper.isQuerySet;
import static org.hisp.dhis.test.raw.TestHelper.isVersionSupported;
import static org.hisp.dhis.test.raw.TestHelper.loadScenarios;
import static org.hisp.dhis.test.raw.TestHelper.queryMatchesParam;
import static org.slf4j.LoggerFactory.getLogger;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.integration.sdk.Dhis2ClientBuilder;
import org.hisp.dhis.integration.sdk.api.Dhis2Client;
import org.hisp.dhis.integration.sdk.api.RemoteDhis2ClientException;
import org.slf4j.Logger;

/**
 * This is a generic test class focused on GET queries only. Based on a given input file, defined at
 * runtime, it will execute all the queries and assert their respective expectations. The tests are
 * focused on testing the raw speed of each query.
 */
public class GetRawSpeedTest extends Simulation {
  private static final Logger logger = getLogger(GetRawSpeedTest.class);

  public GetRawSpeedTest() throws IOException {
    List<Scenario> scenarios = loadScenarios();
    PopulationBuilder populationBuilder = null;
    List<Assertion> assertions = new ArrayList<>();

    Dhis2Client dhis2Client =
        Dhis2ClientBuilder.newClient(
                TestDefinitions.DHIS2_INSTANCE, TestDefinitions.USERNAME, TestDefinitions.PASSWORD)
            .build();

    for (Scenario scenario : scenarios) {
      // Thread.sleep(5000);
      String query = scenario.getQuery();
      Expectation expectation = scenario.getExpectation(BASELINE);

      if (conditionsAreValid(scenario)) {
        // Define expectations.
        int min = defaultIfNull(expectation.getMin(), 0);
        int max = defaultIfNull(expectation.getMax(), Integer.MAX_VALUE);
        int mean = defaultIfNull(expectation.getMean(), Integer.MAX_VALUE);
        int ninetyPercentile = defaultIfNull(expectation.getNinetyPercentile(), Integer.MAX_VALUE);

        // Build assertions.
        if (populationBuilder == null) {
          populationBuilder = populationBuilder(query);
        } else {
          populationBuilder = populationBuilder.andThen(populationBuilder(query));
        }
        assertions.add(details(query).responseTime().max().lte(max));
        assertions.add(details(query).responseTime().mean().lte(mean));
        assertions.add(details(query).responseTime().percentile(90).lte(ninetyPercentile));
        assertions.add(details(query).successfulRequests().percent().gte(100d));

        if (scenario.getFixtures() != null) {
          addFixtures(scenario, dhis2Client);
        }
      }
    }

    if (populationBuilder != null) {
      // Test and assert.
      setUp(populationBuilder).protocols(defaultHttpProtocol()).assertions(assertions);
    } else {
      // Skip unsupported queries avoiding a crash.
      setUp(fakePopulationBuilder());
    }
  }

  /**
   * Adds the fixtures attached to the scenario.
   *
   * @param scenario the {@link Scenario}.
   * @param dhis2Client the HTTP client for transferring fixtures {@link Dhis2Client}.
   */
  private void addFixtures(Scenario scenario, Dhis2Client dhis2Client) throws IOException {
    for (Fixture fixture : scenario.getFixtures()) {
      try {
        dhis2Client
            .post(fixture.getOnCreatePath())
            .withResource(fixture.getResource())
            .transfer()
            .close();
      } catch (RemoteDhis2ClientException e) {
        if (e.getHttpStatusCode() == 409) {
          dhis2Client
              .put(fixture.getOnConflictPath())
              .withResource(fixture.getResource())
              .transfer()
              .close();
        } else {
          throw e;
        }
      }
    }
  }

  /**
   * Checks if the {@link Scenario} conditions are valid.
   *
   * @param scenario the {@link Scenario}.
   * @return true if the scenarios conditions are valid, false otherwise.
   */
  private boolean conditionsAreValid(Scenario scenario) {
    String query = scenario.getQuery();

    boolean hasExpectation = scenario.getExpectation(BASELINE) != null;
    boolean isVersionSupported = isVersionSupported(scenario.getVersion());
    boolean queryDoesNotMatchParam = isQuerySet() & !queryMatchesParam(query);

    if (!hasExpectation) {
      logger.warn("Skipping query: {}. Expectation is missing, check the query definition.", query);
      return false;
    }

    if (!isVersionSupported) {
      logger.warn(
          "Skipping query: {}. Scenario version is missing or not supported by this query.", query);
      return false;
    }

    if (queryDoesNotMatchParam) {
      logger.warn(
          "Skipping query: {}. A specific 'query' was set to run. It's not this one", query);
      return false;
    }

    return true;
  }

  private PopulationBuilder populationBuilder(String query) {
    return scenarioBuilder(query)
        .injectClosed(constantConcurrentUsers(1).during(1));
  }

  private ScenarioBuilder scenarioBuilder(String query) {
    logger.info(query);

    return scenario("Raw speed test for GET " + query)
            .repeat(100).on(exec(http(query).get(query)));
  }
}
