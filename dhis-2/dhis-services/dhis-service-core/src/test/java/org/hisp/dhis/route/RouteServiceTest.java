/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

class RouteServiceTest {

  @Test
  void testHttpClientConnectionManagerDefaultMaxPerRoute() {
    CountDownLatch countDownLatch = new CountDownLatch(2);
    RouteService routeService =
        new RouteService(null, null, null) {
          @Override
          protected HttpClientConnectionManager newConnectionManager() {
            HttpClientConnectionManager httpClientConnectionManager = super.newConnectionManager();
            assertEquals(
                RouteService.DEFAULT_MAX_HTTP_CONNECTION_PER_ROUTE,
                ((PoolingHttpClientConnectionManager) httpClientConnectionManager)
                    .getDefaultMaxPerRoute());
            countDownLatch.countDown();

            return httpClientConnectionManager;
          }
        };
    routeService.postConstruct();
    assertEquals(1, countDownLatch.getCount());
  }

  @Test
  void testCreateRequestUrlDoesNotEscapeUrl() {
    RouteService routeService = new RouteService(null, null, null);
    String upstreamUrl =
        routeService.createRequestUrl(
            UriComponentsBuilder.fromUriString(
                "https://play.im.dhis2.org/stable-2-42-1/api/organisationUnits"),
            Map.of("filter", List.of("id:in:[Rp268JB6Ne4,cDw53Ej8rju]")));
    assertEquals(
        "https://play.im.dhis2.org/stable-2-42-1/api/organisationUnits?filter=id:in:[Rp268JB6Ne4,cDw53Ej8rju]",
        upstreamUrl);
  }

  @Test
  void testAllowedRequestHeaders() {
    assertTrue(RouteService.ALLOWED_REQUEST_HEADERS.contains("content-type"));
  }
}
