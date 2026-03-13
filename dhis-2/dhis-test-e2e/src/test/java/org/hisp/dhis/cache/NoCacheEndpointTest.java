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
package org.hisp.dhis.cache;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class NoCacheEndpointTest extends CacheApiTest {

  @TestFactory
  Stream<DynamicTest> uncachedEndpointsDoNotEmitAutomaticConditionalCacheHeaders() {
    return routes().stream()
        .map(
            route ->
                dynamicTest(
                    "GET " + route.path(),
                    () -> {
                      route.probeUser().login(loginActions);
                      CacheProbe.CacheResponse response = probe.get(route.path());
                      CacheAssertions.assertNoAutomaticCacheHeaders(response);
                    }));
  }

  private List<NoCacheRoute> routes() {
    return List.of(
        new NoCacheRoute(CacheProbeUser.SUPERUSER, "/system/info"),
        new NoCacheRoute(CacheProbeUser.SUPERUSER, "/system/uid"),
        new NoCacheRoute(CacheProbeUser.SUPERUSER, "/dashboards/search?query=ANC"),
        new NoCacheRoute(CacheProbeUser.SUPERUSER, "/dimensions/recommendations?dimension=dx"),
        new NoCacheRoute(
            CacheProbeUser.SUPERUSER, "/dataStatistics/favorites?eventType=VISUALIZATION_VIEW"),
        new NoCacheRoute(
            CacheProbeUser.SUPERUSER, "/messageConversations?fields=id&page=1&pageSize=1"));
  }

  private record NoCacheRoute(CacheProbeUser probeUser, String path) {}
}
