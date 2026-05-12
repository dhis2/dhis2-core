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
package org.hisp.dhis.test.analytics.hmis.trackedentity.query;

import static io.gatling.javaapi.core.CoreDsl.details;
import static org.hisp.dhis.test.analytics.TestHelper.buildScenario;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import java.util.List;
import org.hisp.dhis.test.analytics.AnalyticsSimulation;

public class AnalyticsTrackedEntityQuery19 extends Simulation implements AnalyticsSimulation {

  private static final String GET_QUERY = "GET TRACKED ENTITY QUERY 19";
  private static final String GET_QUERY_API =
      "/api/analytics/trackedEntities/query/IjGPYyk56jz?dimension=ou:USER_ORGUNIT,B6TnnFMgmCk,oindugucx72,sPDKWSQ2vKQ,qDkgAbB5Jlk.KwrBvn1EJT3.HUzRTYRFcYn,qDkgAbB5Jlk.hYyB7FUS5eR.yO0ZIegEsDk,qDkgAbB5Jlk.wYTF0YCHMWr.y57kkdyw35d:GE:1,NUCtvtcCUIZ,LBwVBieQt45:gHfSdwPrC83;U53tdte60Ku&filter=qDkgAbB5Jlk.ou:IWp9dQGM0bS;W6sNfkJcXGC;YvLOmtTQD6b;XKGgynPS1WZ&filter=qDkgAbB5Jlk.hYyB7FUS5eR.JINgGHgqzSN:GT:1&headers=ouname,B6TnnFMgmCk,oindugucx72,sPDKWSQ2vKQ,qDkgAbB5Jlk.KwrBvn1EJT3.HUzRTYRFcYn,qDkgAbB5Jlk.hYyB7FUS5eR.yO0ZIegEsDk,qDkgAbB5Jlk.wYTF0YCHMWr.y57kkdyw35d,NUCtvtcCUIZ,LBwVBieQt45,createdbydisplayname,lastupdated,qDkgAbB5Jlk.programstatus&totalPages=false&rowContext=true&programStatus=qDkgAbB5Jlk.COMPLETED,qDkgAbB5Jlk.ACTIVE&displayProperty=NAME&pageSize=100&page=1&includeMetadataDetails=true&asc=qDkgAbB5Jlk.KwrBvn1EJT3.HUzRTYRFcYn&relativePeriodDate=2024-06-28";

  public PopulationBuilder buildPopulation(OpenInjectionStep injectionStep) {
    return buildScenario(GET_QUERY, GET_QUERY_API).injectOpen(injectionStep);
  }

  public List<Assertion> buildAssertions() {
    return List.of(
        details(GET_QUERY).responseTime().percentile(95).lt(6900),
        details(GET_QUERY).responseTime().max().lt(7450),
        details(GET_QUERY).successfulRequests().percent().is(100D),
        details(GET_QUERY).successfulRequests().percent().is(100D));
  }
}
