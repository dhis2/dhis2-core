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
import org.hisp.dhis.test.analytics.sl.aggregate.AnalyticsAggregate1;
import org.hisp.dhis.test.analytics.sl.enrollment.query.AnalyticsEnrollmentQuery1;
import org.hisp.dhis.test.analytics.sl.enrollment.query.AnalyticsEnrollmentQuery2;
import org.hisp.dhis.test.analytics.sl.event.query.AnalyticsEventQuery1;
import org.hisp.dhis.test.analytics.sl.event.query.AnalyticsEventQuery2;
import org.hisp.dhis.test.analytics.sl.event.query.AnalyticsEventQuery3;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery1;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery10;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery11;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery12;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery13;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery14;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery15;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery16;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery17;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery18;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery19;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery2;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery3;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery4;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery5;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery6;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery7;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery8;
import org.hisp.dhis.test.analytics.sl.te.query.AnalyticsTrackedEntityQuery9;

public class SierraLeoneSimulationsRunner extends Simulation {
  public SierraLeoneSimulationsRunner() {
    // How users should enter the scenarios.
    OpenInjectionStep defaultInjectionStep = simpleUsersRumpUp(1, 10);

    // Simulations.
    AnalyticsSimulation analyticsAggregate1 = new AnalyticsAggregate1();

    AnalyticsSimulation analyticsEnrollmentQuery1 = new AnalyticsEnrollmentQuery1();
    AnalyticsSimulation analyticsEnrollmentQuery2 = new AnalyticsEnrollmentQuery2();

    AnalyticsSimulation analyticsEventQuery1 = new AnalyticsEventQuery1();
    AnalyticsSimulation analyticsEventQuery2 = new AnalyticsEventQuery2();
    AnalyticsSimulation analyticsEventQuery3 = new AnalyticsEventQuery3();

    AnalyticsSimulation analyticsTrackedEntityQuery1 = new AnalyticsTrackedEntityQuery1();
    AnalyticsSimulation analyticsTrackedEntityQuery2 = new AnalyticsTrackedEntityQuery2();
    AnalyticsSimulation analyticsTrackedEntityQuery3 = new AnalyticsTrackedEntityQuery3();
    AnalyticsSimulation analyticsTrackedEntityQuery4 = new AnalyticsTrackedEntityQuery4();
    AnalyticsSimulation analyticsTrackedEntityQuery5 = new AnalyticsTrackedEntityQuery5();
    AnalyticsSimulation analyticsTrackedEntityQuery6 = new AnalyticsTrackedEntityQuery6();
    AnalyticsSimulation analyticsTrackedEntityQuery7 = new AnalyticsTrackedEntityQuery7();
    AnalyticsSimulation analyticsTrackedEntityQuery8 = new AnalyticsTrackedEntityQuery8();
    AnalyticsSimulation analyticsTrackedEntityQuery9 = new AnalyticsTrackedEntityQuery9();
    AnalyticsSimulation analyticsTrackedEntityQuery10 = new AnalyticsTrackedEntityQuery10();
    AnalyticsSimulation analyticsTrackedEntityQuery11 = new AnalyticsTrackedEntityQuery11();
    AnalyticsSimulation analyticsTrackedEntityQuery12 = new AnalyticsTrackedEntityQuery12();
    AnalyticsSimulation analyticsTrackedEntityQuery13 = new AnalyticsTrackedEntityQuery13();
    AnalyticsSimulation analyticsTrackedEntityQuery14 = new AnalyticsTrackedEntityQuery14();
    AnalyticsSimulation analyticsTrackedEntityQuery15 = new AnalyticsTrackedEntityQuery15();
    AnalyticsSimulation analyticsTrackedEntityQuery16 = new AnalyticsTrackedEntityQuery16();
    AnalyticsSimulation analyticsTrackedEntityQuery17 = new AnalyticsTrackedEntityQuery17();
    AnalyticsSimulation analyticsTrackedEntityQuery18 = new AnalyticsTrackedEntityQuery18();
    AnalyticsSimulation analyticsTrackedEntityQuery19 = new AnalyticsTrackedEntityQuery19();

    // Scenarios.
    List<PopulationBuilder> scenarios = new ArrayList<>();
    scenarios.add(analyticsAggregate1.buildPopulation(defaultInjectionStep));

    scenarios.add(analyticsEnrollmentQuery1.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEnrollmentQuery2.buildPopulation(defaultInjectionStep));

    scenarios.add(analyticsEventQuery1.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventQuery2.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventQuery3.buildPopulation(defaultInjectionStep));

    scenarios.add(analyticsTrackedEntityQuery1.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery2.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery3.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery4.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery5.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery6.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery7.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery8.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery9.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery10.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery11.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery12.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery13.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery14.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery15.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery16.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery17.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery18.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsTrackedEntityQuery19.buildPopulation(defaultInjectionStep));

    // Assertions.
    List<Assertion> assertions = new ArrayList<>();
    assertions.addAll(analyticsAggregate1.buildAssertions());

    assertions.addAll(analyticsEnrollmentQuery1.buildAssertions());
    assertions.addAll(analyticsEnrollmentQuery2.buildAssertions());

    assertions.addAll(analyticsEventQuery1.buildAssertions());
    assertions.addAll(analyticsEventQuery2.buildAssertions());
    assertions.addAll(analyticsEventQuery3.buildAssertions());

    assertions.addAll(analyticsTrackedEntityQuery1.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery2.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery3.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery4.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery5.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery6.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery7.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery8.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery9.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery10.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery11.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery12.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery13.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery14.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery15.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery16.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery17.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery18.buildAssertions());
    assertions.addAll(analyticsTrackedEntityQuery19.buildAssertions());

    // Execute and assert all scenarios.
    SetUp setUp = setUp(scenarios);
    setUp.protocols(buildHttpProtocol("/api/ping")).assertions(assertions);
  }
}
