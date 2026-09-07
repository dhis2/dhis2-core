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
package org.hisp.dhis.test.platform;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Performance test for data value set export with org unit descendants (DHIS2-21490).
 *
 * <p>Exercises {@code GET /api/dataValueSets/} with {@code children=true}, which activates the
 * {@code ou_with_descendants_ids} CTE in {@code HibernateDataExportStore}. The original query used
 * an O(n²) self-join across the entire org unit table; the fix drives from the selected roots and
 * uses the {@code varchar_pattern_ops} path index to find descendants in O(roots × log n).
 *
 * <p>Assumes a Sierra Leone demo database at {@code localhost:8080}. UIDs are stable across demo
 * database versions:
 *
 * <ul>
 *   <li>Sierra Leone root org unit: {@code ImspTQPwCqd}
 *   <li>ART monthly summary dataset: {@code lyLU2wR22tC}
 * </ul>
 */
public class AggregateDataExportPerformanceTest extends Simulation {

  private static final String BASE_URL = "http://localhost:8080";
  private static final String EXPORT_REQUEST = "GET dataValueSets - root OU with descendants";

  @Override
  public void before() {
    MetadataImporter.importJsonFile("platform/superuser-data-sl-db.json", "admin", "district");
  }

  public AggregateDataExportPerformanceTest() {
    DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
    String endDate = LocalDate.now().format(fmt);
    String startDate = LocalDate.now().minusYears(2).format(fmt);

    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .warmUp(BASE_URL + "/api/ping")
            .disableCaching()
            .basicAuth("admin", "district");

    ScenarioBuilder exportScenario =
        scenario(EXPORT_REQUEST)
            .group(EXPORT_REQUEST)
            .on(
                repeat(1)
                    .on(
                        exec(http(EXPORT_REQUEST)
                                .get("/api/dataValueSets/")
                                .queryParam("orgUnit", "ImspTQPwCqd")
                                .queryParam("children", "true")
                                .queryParam("dataSet", "lyLU2wR22tC")
                                .queryParam("startDate", startDate)
                                .queryParam("endDate", endDate))
                            .pause(1)));

    ClosedInjectionStep closedInjection = rampConcurrentUsers(0).to(5).during(10);

    setUp(exportScenario.injectClosed(closedInjection))
        .protocols(httpProtocol)
        .assertions(
            details(EXPORT_REQUEST).responseTime().percentile(95).lt(300),
            details(EXPORT_REQUEST).responseTime().max().lt(500),
            details(EXPORT_REQUEST).successfulRequests().percent().is(100D));
  }
}
