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
package org.hisp.dhis.test.analytics.sl.trackedentity.query;

import static io.gatling.javaapi.core.CoreDsl.details;
import static org.hisp.dhis.test.analytics.TestDefinitions.simpleUsersRumpUp;
import static org.hisp.dhis.test.analytics.TestHelper.buildHttpProtocol;
import static org.hisp.dhis.test.analytics.TestHelper.buildScenario;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.test.analytics.AnalyticsSimulation;

public class AnalyticsTrackedEntityQuery11 extends Simulation implements AnalyticsSimulation {

  private static final String GET_QUERY = "GET TRACKED ENTITY QUERY 11";
  private static final String GET_QUERY_API =
      "/api/analytics/trackedEntities/query/nEenWmSyUEp?dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ&headers=IpHINAT79UW.programstatus&totalPages=false&rowContext=true&displayProperty=NAME&pageSize=10&page=1&includeMetadataDetails=true&asc=IpHINAT79UW.programstatus&relativePeriodDate=2017-01-27";

  public AnalyticsTrackedEntityQuery11() {
    // How users should enter the scenarios.
    OpenInjectionStep defaultInjectionStep = simpleUsersRumpUp(1, 20);

    // Build scenarios and assertions from the discovered simulations.
    List<PopulationBuilder> scenarios = new ArrayList<>();
    List<Assertion> assertions = new ArrayList<>();

    // Build scenarios, assertions and execution setup.
    scenarios.add(buildPopulation(defaultInjectionStep));
    assertions.addAll(buildAssertions());
    setUp(scenarios).protocols(buildHttpProtocol("/api/ping")).assertions(assertions);
  }

  public PopulationBuilder buildPopulation(OpenInjectionStep injectionStep) {
    return buildScenario(GET_QUERY, GET_QUERY_API).injectOpen(injectionStep);
  }

  public List<Assertion> buildAssertions() {
    return List.of(
        details(GET_QUERY).responseTime().percentile(95).lt(890),
        details(GET_QUERY).responseTime().max().lt(940),
        details(GET_QUERY).successfulRequests().percent().is(100D),
        details(GET_QUERY).successfulRequests().percent().is(100D));
  }
}
