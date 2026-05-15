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
package org.hisp.dhis.test.analytics.hmis.aggregate;

import static io.gatling.javaapi.core.CoreDsl.details;
import static org.hisp.dhis.test.analytics.TestHelper.buildScenario;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import java.util.List;
import org.hisp.dhis.test.analytics.AnalyticsSimulation;

public class AnalyticsAggregate17 extends Simulation implements AnalyticsSimulation {

  private static final String GET_QUERY = "GET AGGREGATE QUERY 17";
  private static final String GET_QUERY_API =
      "/api/analytics?dimension=dx:GW4OUIj8tJZ;BGxfVk7fZu6;mV0Gr5kqgPJ;gtY3kiw1ipN,pe:LAST_12_WEEKS&filter=ou:USER_ORGUNIT&displayProperty=NAME&includeNumDen=false&skipMeta=false&skipData=false&relativePeriodDate=2023-11-14";

  public PopulationBuilder buildPopulation(OpenInjectionStep injectionStep) {
    return buildScenario(GET_QUERY, GET_QUERY_API).injectOpen(injectionStep);
  }

  public List<Assertion> buildAssertions() {
    return List.of(
        details(GET_QUERY).responseTime().percentile(95).lt(1),
        details(GET_QUERY).responseTime().max().lt(1),
        details(GET_QUERY).successfulRequests().percent().is(100D),
        details(GET_QUERY).successfulRequests().percent().is(100D));
  }
}
