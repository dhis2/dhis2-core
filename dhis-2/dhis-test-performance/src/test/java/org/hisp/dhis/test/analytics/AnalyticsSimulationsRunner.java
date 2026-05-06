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

import static org.hisp.dhis.test.analytics.TestDefinitions.simpleUsersRumpUp;
import static org.hisp.dhis.test.analytics.TestHelper.buildHttpProtocol;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.test.analytics.aggregate.AnalyticsAggregate1;
import org.hisp.dhis.test.analytics.enrollment.query.AnalyticsEnrollmentQuery1;
import org.hisp.dhis.test.analytics.enrollment.query.AnalyticsEnrollmentQuery2;

public class AnalyticsSimulationsRunner extends Simulation {
  public AnalyticsSimulationsRunner() {
    // How users should enter the scenarios.
    OpenInjectionStep defaultInjectionStep = simpleUsersRumpUp(1, 10);

    // Simulations.
    AnalyticsSimulation analyticsAggregate1 = new AnalyticsAggregate1();
    AnalyticsSimulation analyticsEnrollmentQuery1 = new AnalyticsEnrollmentQuery1();
    AnalyticsSimulation analyticsEnrollmentQuery2 = new AnalyticsEnrollmentQuery2();

    // Scenarios.
    List<PopulationBuilder> scenarios = new ArrayList<>();
    scenarios.add(analyticsAggregate1.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEnrollmentQuery1.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEnrollmentQuery2.buildPopulation(defaultInjectionStep));

    // Assertions.
    List<Assertion> assertions = new ArrayList<>();
    assertions.addAll(analyticsAggregate1.buildAssertions());
    assertions.addAll(analyticsEnrollmentQuery1.buildAssertions());
    assertions.addAll(analyticsEnrollmentQuery2.buildAssertions());

    // Execute and assert all scenarios.
    SetUp setUp = setUp(scenarios);
    setUp.protocols(buildHttpProtocol("/api/ping")).assertions(assertions);
  }
}
