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
package org.hisp.dhis.test.analytics;

import static org.hisp.dhis.test.analytics.SimulationHelper.discoverSimulations;
import static org.hisp.dhis.test.analytics.TestDefinitions.simpleUsersRumpUp;
import static org.hisp.dhis.test.analytics.TestHelper.buildHttpProtocol;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import java.util.ArrayList;
import java.util.List;

public class HmisSimulationsRunner extends Simulation {

  private static final String SIMULATIONS_PACKAGE = "org.hisp.dhis.test.analytics.hmis";

  public HmisSimulationsRunner() {
    // How users should enter the scenarios.
    OpenInjectionStep defaultInjectionStep = simpleUsersRumpUp(1, 10);

    // Discover and instantiate every AnalyticsSimulation under the hmis package.
    List<AnalyticsSimulation> simulations = discoverSimulations(SIMULATIONS_PACKAGE);

    // Build scenarios and assertions from the discovered simulations.
    List<PopulationBuilder> scenarios = new ArrayList<>();
    List<Assertion> assertions = new ArrayList<>();
    for (AnalyticsSimulation simulation : simulations) {
      scenarios.add(simulation.buildPopulation(defaultInjectionStep));
      assertions.addAll(simulation.buildAssertions());
    }

    // Execute and assert all scenarios.
    setUp(scenarios).protocols(buildHttpProtocol("/api/ping")).assertions(assertions);
  }
}
