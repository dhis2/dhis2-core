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

public class AnalyticsTrackedEntityQuery17 extends Simulation implements AnalyticsSimulation {

  private static final String GET_QUERY = "GET TRACKED ENTITY QUERY 17";
  private static final String GET_QUERY_API =
      "/api/analytics/trackedEntities/query/nEenWmSyUEp.json?dimension=ou:tEgxbwwrwUd;ObV5AR1NECl;Uwcj0mz78BV;nDwbwJZQUYU;OjTS752GbZE;mt47bcb0Rcj;ZxuSbAmsLCn;ImspTQPwCqd,WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h:IN:NV;0;1,WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI,WSGAb5XwJ3Y.edqlbukwRfQ.DCUDZxqOxUo:IN:0,WSGAb5XwJ3Y.edqlbukwRfQ.w7enwqzx90I,WSGAb5XwJ3Y.PFDfvmGpsR3.hib4oz2sOLw,WSGAb5XwJ3Y.WZbXY0S00lP.Itl05OEupgQ,WSGAb5XwJ3Y.PFDfvmGpsR3.FIHEeJwfhZH,WSGAb5XwJ3Y.PFDfvmGpsR3.BmaBjPQX8ME:IN:1,WSGAb5XwJ3Y.bbKtnxRZKEP.csl3yq5UC46,WSGAb5XwJ3Y.PFDfvmGpsR3.csl3yq5UC46,WSGAb5XwJ3Y.PFDfvmGpsR3.m3XQrgadVK9,WSGAb5XwJ3Y.WZbXY0S00lP.DecmCMPDPdS,WSGAb5XwJ3Y.WZbXY0S00lP.QFX1FLWBwtq,ur1Edk5Oe2n.ZkbAXlQUYJG.HmkXnHJxcD1,ur1Edk5Oe2n.ZkbAXlQUYJG.U5ubm6PPYrM,ur1Edk5Oe2n.ZkbAXlQUYJG.vAzDOljIN1o,ur1Edk5Oe2n.jdRD35YwbRH.yLIPuJHRgey,ur1Edk5Oe2n.EPEcjy3FWmI.lJTx9EZ1dk1,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO,IpHINAT79UW.ZzYYXq4fJie.HLmTEmupdX0,IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,uy2gU8kT1jF.Xgk8Wvl0jHr.Rbv6wcblbxe,uy2gU8kT1jF.eaDHS084uMp.utliJZmDeeC,uy2gU8kT1jF.eaDHS084uMp.pB5sL7Ts4fb,uy2gU8kT1jF.oRySG82BKE6.EzMxXuVww2z,uy2gU8kT1jF.grIfo3oOf4Y.gAbD3uDVHHh,uy2gU8kT1jF.eaDHS084uMp.OuJ6sgPyAbC,uy2gU8kT1jF.oRySG82BKE6.UXz7xuGCEhU,uy2gU8kT1jF.grIfo3oOf4Y.g9eOBujte1U,uy2gU8kT1jF.eaDHS084uMp.NALlPhMmMTQ,fDd25txQckK.lST1OZ5BDJ2.qpQinIDQ6Uy,fDd25txQckK.lST1OZ5BDJ2.fQMBEt42CSl,fDd25txQckK.lST1OZ5BDJ2.Mnkodq2wzlV&headers=ouname,WSGAb5XwJ3Y.bbKtnxRZKEP.QWVRukwa83h,WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI,WSGAb5XwJ3Y.edqlbukwRfQ.DCUDZxqOxUo,WSGAb5XwJ3Y.edqlbukwRfQ.w7enwqzx90I,WSGAb5XwJ3Y.PFDfvmGpsR3.hib4oz2sOLw,WSGAb5XwJ3Y.WZbXY0S00lP.Itl05OEupgQ,WSGAb5XwJ3Y.PFDfvmGpsR3.FIHEeJwfhZH,WSGAb5XwJ3Y.PFDfvmGpsR3.BmaBjPQX8ME,WSGAb5XwJ3Y.bbKtnxRZKEP.csl3yq5UC46,WSGAb5XwJ3Y.PFDfvmGpsR3.csl3yq5UC46,WSGAb5XwJ3Y.PFDfvmGpsR3.m3XQrgadVK9,WSGAb5XwJ3Y.WZbXY0S00lP.DecmCMPDPdS,WSGAb5XwJ3Y.WZbXY0S00lP.QFX1FLWBwtq,WSGAb5XwJ3Y.programstatus,ur1Edk5Oe2n.ZkbAXlQUYJG.HmkXnHJxcD1,ur1Edk5Oe2n.ZkbAXlQUYJG.U5ubm6PPYrM,ur1Edk5Oe2n.ZkbAXlQUYJG.vAzDOljIN1o,ur1Edk5Oe2n.jdRD35YwbRH.yLIPuJHRgey,ur1Edk5Oe2n.EPEcjy3FWmI.lJTx9EZ1dk1,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.ZzYYXq4fJie.GQY2lXrypjO,IpHINAT79UW.ZzYYXq4fJie.HLmTEmupdX0,IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,uy2gU8kT1jF.Xgk8Wvl0jHr.Rbv6wcblbxe,uy2gU8kT1jF.eaDHS084uMp.utliJZmDeeC,uy2gU8kT1jF.eaDHS084uMp.pB5sL7Ts4fb,uy2gU8kT1jF.oRySG82BKE6.EzMxXuVww2z,uy2gU8kT1jF.grIfo3oOf4Y.gAbD3uDVHHh,uy2gU8kT1jF.eaDHS084uMp.OuJ6sgPyAbC,uy2gU8kT1jF.oRySG82BKE6.UXz7xuGCEhU,uy2gU8kT1jF.grIfo3oOf4Y.g9eOBujte1U,uy2gU8kT1jF.eaDHS084uMp.NALlPhMmMTQ,uy2gU8kT1jF.incidentdate,fDd25txQckK.lST1OZ5BDJ2.qpQinIDQ6Uy,fDd25txQckK.lST1OZ5BDJ2.fQMBEt42CSl,fDd25txQckK.lST1OZ5BDJ2.Mnkodq2wzlV,created&totalPages=false&rowContext=true&programStatus=WSGAb5XwJ3Y.COMPLETED,WSGAb5XwJ3Y.ACTIVE&created=LAST_10_YEARS&displayProperty=NAME&pageSize=5&page=1&includeMetadataDetails=true&asc=ouname&relativePeriodDate=2024-06-13";

  public AnalyticsTrackedEntityQuery17() {
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
        details(GET_QUERY).responseTime().percentile(95).lt(875),
        details(GET_QUERY).responseTime().max().lt(930),
        details(GET_QUERY).successfulRequests().percent().is(100D),
        details(GET_QUERY).successfulRequests().percent().is(100D));
  }
}
