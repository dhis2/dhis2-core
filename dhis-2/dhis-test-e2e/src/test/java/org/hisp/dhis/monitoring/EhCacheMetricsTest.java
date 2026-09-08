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
package org.hisp.dhis.monitoring;

import static org.hamcrest.Matchers.containsString;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies that per-region Hibernate second level cache metrics are exposed on the Prometheus
 * scrape endpoint when {@code monitoring.ehcache.enabled} is on. This proves end to end that the
 * server booted with the second level cache active, that the ehcache regions exist, and that their
 * statistics are wired into Micrometer (see EhCacheMetricsConfig): if the cache silently stops
 * being configured, or region statistics stop being resolvable, these metrics disappear and this
 * test fails.
 *
 * @author Morten Svanæs
 */
public class EhCacheMetricsTest extends ApiTest {
  private RestApiActions metricsActions;

  private LoginActions loginActions;

  @BeforeAll
  public void setUp() {
    loginActions = new LoginActions();
    metricsActions = new RestApiActions("/metrics");
  }

  @Test
  public void shouldExposePerRegionSecondLevelCacheMetrics() {
    loginActions.loginAsSuperUser();

    // Touch an endpoint that reads cached entities so region counters exist and move
    new RestApiActions("/me").get().validate().statusCode(200);

    ApiResponse response = metricsActions.get("", "text/plain", "text/plain", null);

    response
        .validate()
        .statusCode(200)
        .body(containsString("# TYPE ehcache_gets_total counter"))
        .body(containsString("# TYPE ehcache_hits_total counter"))
        .body(containsString("# TYPE ehcache_misses_total counter"))
        .body(containsString("# TYPE ehcache_puts_total counter"))
        // Per-region counters carry the region short name and the L2 marker tag; the User
        // entity region always exists because authentication loads the current user
        .body(containsString("ehcache_gets_total{cache=\"User\",type=\"L2\"}"));
  }
}
