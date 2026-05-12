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
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate1;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate10;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate11;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate12;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate13;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate14;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate15;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate16;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate17;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate18;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate2;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate3;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate4;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate5;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate6;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate7;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate8;
import org.hisp.dhis.test.analytics.hmis.aggregate.AnalyticsAggregate9;
import org.hisp.dhis.test.analytics.hmis.enrollment.query.AnalyticsEnrollmentQuery1;
import org.hisp.dhis.test.analytics.hmis.enrollment.query.AnalyticsEnrollmentQuery2;
import org.hisp.dhis.test.analytics.hmis.enrollment.query.AnalyticsEnrollmentQuery3;
import org.hisp.dhis.test.analytics.hmis.enrollment.query.AnalyticsEnrollmentQuery4;
import org.hisp.dhis.test.analytics.hmis.enrollment.query.AnalyticsEnrollmentQuery5;
import org.hisp.dhis.test.analytics.hmis.enrollment.query.AnalyticsEnrollmentQuery6;
import org.hisp.dhis.test.analytics.hmis.enrollment.query.AnalyticsEnrollmentQuery7;
import org.hisp.dhis.test.analytics.hmis.event.aggregate.AnalyticsEventAggregate1;
import org.hisp.dhis.test.analytics.hmis.event.aggregate.AnalyticsEventAggregate2;
import org.hisp.dhis.test.analytics.hmis.event.aggregate.AnalyticsEventAggregate3;
import org.hisp.dhis.test.analytics.hmis.event.aggregate.AnalyticsEventAggregate4;
import org.hisp.dhis.test.analytics.hmis.event.aggregate.AnalyticsEventAggregate5;
import org.hisp.dhis.test.analytics.hmis.event.aggregate.AnalyticsEventAggregate6;
import org.hisp.dhis.test.analytics.hmis.event.aggregate.AnalyticsEventAggregate7;
import org.hisp.dhis.test.analytics.hmis.event.aggregate.AnalyticsEventAggregate8;
import org.hisp.dhis.test.analytics.hmis.event.query.AnalyticsEventQuery1;
import org.hisp.dhis.test.analytics.hmis.event.query.AnalyticsEventQuery2;
import org.hisp.dhis.test.analytics.hmis.event.query.AnalyticsEventQuery3;
import org.hisp.dhis.test.analytics.hmis.event.query.AnalyticsEventQuery4;
import org.hisp.dhis.test.analytics.hmis.event.query.AnalyticsEventQuery5;
import org.hisp.dhis.test.analytics.hmis.outliers.AnalyticsOutliers1;
import org.hisp.dhis.test.analytics.hmis.outliers.AnalyticsOutliers2;
import org.hisp.dhis.test.analytics.hmis.outliers.AnalyticsOutliers3;
import org.hisp.dhis.test.analytics.hmis.outliers.AnalyticsOutliers4;
import org.hisp.dhis.test.analytics.hmis.outliers.AnalyticsOutliers5;
import org.hisp.dhis.test.analytics.hmis.outliers.AnalyticsOutliers6;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery1;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery10;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery11;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery12;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery13;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery14;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery15;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery16;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery17;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery18;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery19;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery2;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery20;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery3;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery4;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery5;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery6;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery7;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery8;
import org.hisp.dhis.test.analytics.hmis.trackedentity.query.AnalyticsTrackedEntityQuery9;

public class HmisSimulationsRunnerOld extends Simulation {
  public HmisSimulationsRunnerOld() {
    // How users should enter the scenarios.
    OpenInjectionStep defaultInjectionStep = simpleUsersRumpUp(1, 10);

    // Simulations.
    AnalyticsSimulation analyticsAggregate1 = new AnalyticsAggregate1();
    AnalyticsSimulation analyticsAggregate2 = new AnalyticsAggregate2();
    AnalyticsSimulation analyticsAggregate3 = new AnalyticsAggregate3();
    AnalyticsSimulation analyticsAggregate4 = new AnalyticsAggregate4();
    AnalyticsSimulation analyticsAggregate5 = new AnalyticsAggregate5();
    AnalyticsSimulation analyticsAggregate6 = new AnalyticsAggregate6();
    AnalyticsSimulation analyticsAggregate7 = new AnalyticsAggregate7();
    AnalyticsSimulation analyticsAggregate8 = new AnalyticsAggregate8();
    AnalyticsSimulation analyticsAggregate9 = new AnalyticsAggregate9();
    AnalyticsSimulation analyticsAggregate10 = new AnalyticsAggregate10();
    AnalyticsSimulation analyticsAggregate11 = new AnalyticsAggregate11();
    AnalyticsSimulation analyticsAggregate12 = new AnalyticsAggregate12();
    AnalyticsSimulation analyticsAggregate13 = new AnalyticsAggregate13();
    AnalyticsSimulation analyticsAggregate14 = new AnalyticsAggregate14();
    AnalyticsSimulation analyticsAggregate15 = new AnalyticsAggregate15();
    AnalyticsSimulation analyticsAggregate16 = new AnalyticsAggregate16();
    AnalyticsSimulation analyticsAggregate17 = new AnalyticsAggregate17();
    AnalyticsSimulation analyticsAggregate18 = new AnalyticsAggregate18();

    AnalyticsSimulation analyticsEnrollmentQuery1 = new AnalyticsEnrollmentQuery1();
    AnalyticsSimulation analyticsEnrollmentQuery2 = new AnalyticsEnrollmentQuery2();
    AnalyticsSimulation analyticsEnrollmentQuery3 = new AnalyticsEnrollmentQuery3();
    AnalyticsSimulation analyticsEnrollmentQuery4 = new AnalyticsEnrollmentQuery4();
    AnalyticsSimulation analyticsEnrollmentQuery5 = new AnalyticsEnrollmentQuery5();
    AnalyticsSimulation analyticsEnrollmentQuery6 = new AnalyticsEnrollmentQuery6();
    AnalyticsSimulation analyticsEnrollmentQuery7 = new AnalyticsEnrollmentQuery7();

    AnalyticsSimulation analyticsEventAggregate1 = new AnalyticsEventAggregate1();
    AnalyticsSimulation analyticsEventAggregate2 = new AnalyticsEventAggregate2();
    AnalyticsSimulation analyticsEventAggregate3 = new AnalyticsEventAggregate3();
    AnalyticsSimulation analyticsEventAggregate4 = new AnalyticsEventAggregate4();
    AnalyticsSimulation analyticsEventAggregate5 = new AnalyticsEventAggregate5();
    AnalyticsSimulation analyticsEventAggregate6 = new AnalyticsEventAggregate6();
    AnalyticsSimulation analyticsEventAggregate7 = new AnalyticsEventAggregate7();
    AnalyticsSimulation analyticsEventAggregate8 = new AnalyticsEventAggregate8();

    AnalyticsSimulation analyticsEventQuery1 = new AnalyticsEventQuery1();
    AnalyticsSimulation analyticsEventQuery2 = new AnalyticsEventQuery2();
    AnalyticsSimulation analyticsEventQuery3 = new AnalyticsEventQuery3();
    AnalyticsSimulation analyticsEventQuery4 = new AnalyticsEventQuery4();
    AnalyticsSimulation analyticsEventQuery5 = new AnalyticsEventQuery5();

    AnalyticsSimulation analyticsOutliers1 = new AnalyticsOutliers1();
    AnalyticsSimulation analyticsOutliers2 = new AnalyticsOutliers2();
    AnalyticsSimulation analyticsOutliers3 = new AnalyticsOutliers3();
    AnalyticsSimulation analyticsOutliers4 = new AnalyticsOutliers4();
    AnalyticsSimulation analyticsOutliers5 = new AnalyticsOutliers5();
    AnalyticsSimulation analyticsOutliers6 = new AnalyticsOutliers6();

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
    AnalyticsSimulation analyticsTrackedEntityQuery20 = new AnalyticsTrackedEntityQuery20();

    // Scenarios.
    List<PopulationBuilder> scenarios = new ArrayList<>();
    scenarios.add(analyticsAggregate1.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate2.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate3.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate4.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate5.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate6.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate7.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate8.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate9.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate10.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate11.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate12.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate13.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate14.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate15.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate16.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate17.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsAggregate18.buildPopulation(defaultInjectionStep));

    scenarios.add(analyticsEnrollmentQuery1.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEnrollmentQuery2.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEnrollmentQuery3.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEnrollmentQuery4.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEnrollmentQuery5.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEnrollmentQuery6.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEnrollmentQuery7.buildPopulation(defaultInjectionStep));

    scenarios.add(analyticsEventAggregate1.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventAggregate2.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventAggregate3.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventAggregate4.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventAggregate5.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventAggregate6.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventAggregate7.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventAggregate8.buildPopulation(defaultInjectionStep));

    scenarios.add(analyticsEventQuery1.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventQuery2.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventQuery3.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventQuery4.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsEventQuery5.buildPopulation(defaultInjectionStep));

    scenarios.add(analyticsOutliers1.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsOutliers2.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsOutliers3.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsOutliers4.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsOutliers5.buildPopulation(defaultInjectionStep));
    scenarios.add(analyticsOutliers6.buildPopulation(defaultInjectionStep));

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
    scenarios.add(analyticsTrackedEntityQuery20.buildPopulation(defaultInjectionStep));

    // Assertions.
    List<Assertion> assertions = new ArrayList<>();
    assertions.addAll(analyticsAggregate1.buildAssertions());
    assertions.addAll(analyticsAggregate2.buildAssertions());
    assertions.addAll(analyticsAggregate3.buildAssertions());
    assertions.addAll(analyticsAggregate4.buildAssertions());
    assertions.addAll(analyticsAggregate5.buildAssertions());
    assertions.addAll(analyticsAggregate6.buildAssertions());
    assertions.addAll(analyticsAggregate7.buildAssertions());
    assertions.addAll(analyticsAggregate8.buildAssertions());
    assertions.addAll(analyticsAggregate9.buildAssertions());
    assertions.addAll(analyticsAggregate10.buildAssertions());
    assertions.addAll(analyticsAggregate11.buildAssertions());
    assertions.addAll(analyticsAggregate12.buildAssertions());
    assertions.addAll(analyticsAggregate13.buildAssertions());
    assertions.addAll(analyticsAggregate14.buildAssertions());
    assertions.addAll(analyticsAggregate15.buildAssertions());
    assertions.addAll(analyticsAggregate16.buildAssertions());
    assertions.addAll(analyticsAggregate17.buildAssertions());
    assertions.addAll(analyticsAggregate18.buildAssertions());

    assertions.addAll(analyticsEnrollmentQuery1.buildAssertions());
    assertions.addAll(analyticsEnrollmentQuery2.buildAssertions());
    assertions.addAll(analyticsEnrollmentQuery3.buildAssertions());
    assertions.addAll(analyticsEnrollmentQuery4.buildAssertions());
    assertions.addAll(analyticsEnrollmentQuery5.buildAssertions());
    assertions.addAll(analyticsEnrollmentQuery6.buildAssertions());
    assertions.addAll(analyticsEnrollmentQuery7.buildAssertions());

    assertions.addAll(analyticsEventAggregate1.buildAssertions());
    assertions.addAll(analyticsEventAggregate2.buildAssertions());
    assertions.addAll(analyticsEventAggregate3.buildAssertions());
    assertions.addAll(analyticsEventAggregate4.buildAssertions());
    assertions.addAll(analyticsEventAggregate5.buildAssertions());
    assertions.addAll(analyticsEventAggregate6.buildAssertions());
    assertions.addAll(analyticsEventAggregate7.buildAssertions());
    assertions.addAll(analyticsEventAggregate8.buildAssertions());

    assertions.addAll(analyticsEventQuery1.buildAssertions());
    assertions.addAll(analyticsEventQuery2.buildAssertions());
    assertions.addAll(analyticsEventQuery3.buildAssertions());
    assertions.addAll(analyticsEventQuery4.buildAssertions());
    assertions.addAll(analyticsEventQuery5.buildAssertions());

    assertions.addAll(analyticsOutliers1.buildAssertions());
    assertions.addAll(analyticsOutliers2.buildAssertions());
    assertions.addAll(analyticsOutliers3.buildAssertions());
    assertions.addAll(analyticsOutliers4.buildAssertions());
    assertions.addAll(analyticsOutliers5.buildAssertions());
    assertions.addAll(analyticsOutliers6.buildAssertions());

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
    assertions.addAll(analyticsTrackedEntityQuery20.buildAssertions());

    // Execute and assert all scenarios.
    SetUp setUp = setUp(scenarios);
    setUp.protocols(buildHttpProtocol("/api/ping")).assertions(assertions);
  }
}
