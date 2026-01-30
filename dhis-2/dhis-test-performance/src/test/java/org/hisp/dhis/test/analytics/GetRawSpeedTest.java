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
package org.hisp.dhis.test.analytics;

import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static org.hisp.dhis.test.raw.TestDefinitions.constantSingleUser;
import static org.hisp.dhis.test.raw.TestDefinitions.defaultHttpProtocol;
import static org.slf4j.LoggerFactory.getLogger;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/** TODO: initial test skeleton. */
public class GetRawSpeedTest extends Simulation {
  private static final Logger logger = getLogger(GetRawSpeedTest.class);

  public GetRawSpeedTest() {
    List<PopulationBuilder> populationBuilders = new ArrayList<>();
    List<Assertion> assertions = new ArrayList<>();
    String query = "/api/analytics/etc";

    // Build assertions.
    populationBuilders.add(populationBuilder(query));
    assertions.add(details(query).responseTime().min().gte(20));
    assertions.add(details(query).responseTime().max().lte(100));
    assertions.add(details(query).responseTime().mean().lte(85));
    assertions.add(details(query).responseTime().percentile(90).lte(65));
    assertions.add(details(query).successfulRequests().percent().gte(100d));

    setUp(populationBuilders).assertions(assertions);
  }

  private PopulationBuilder populationBuilder(String query) {
    return scenarioBuilder(query)
        .injectClosed(constantSingleUser(15))
        .protocols(defaultHttpProtocol());
  }

  private ScenarioBuilder scenarioBuilder(String query) {
    logger.info(query);

    return scenario("Raw speed test for GET " + query)
        .exec(http(query).get(query).check(status().is(200)));
  }
}
