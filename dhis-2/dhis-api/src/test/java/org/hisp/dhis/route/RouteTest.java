/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.auth.ApiTokenAuthScheme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RouteTest {
  Route tldRoute = new Route();

  Route routeWithSubPath = new Route();

  Route routeWithDirectorySubPath = new Route();

  Route routeWithPathWildcard = new Route();

  Route routeWithSubPathAndPathWildcard = new Route();

  @BeforeEach
  void setUp() {
    tldRoute.setUrl("https://thisisatest.com");
    routeWithSubPath.setUrl("https://thisisatest.com/some/path/123");
    routeWithDirectorySubPath.setUrl("https://thisisatest.com/some/path/123/");
    routeWithPathWildcard.setUrl("https://thisisatest.com/**");
    routeWithSubPathAndPathWildcard.setUrl("https://thisisatest.com/sub/path/**");
  }

  @AfterEach
  void tearDown() {}

  @Test
  void testAllowsSubpaths() {
    assertFalse(tldRoute.allowsSubpaths());
    assertFalse(routeWithSubPath.allowsSubpaths());
    assertFalse(routeWithDirectorySubPath.allowsSubpaths());
    assertTrue(routeWithPathWildcard.allowsSubpaths());
    assertTrue(routeWithSubPathAndPathWildcard.allowsSubpaths());
  }

  @Test
  void testGetBaseUrl() {
    assertEquals("https://thisisatest.com", tldRoute.getBaseUrl());
    assertEquals("https://thisisatest.com/some/path/123", routeWithSubPath.getBaseUrl());
    assertEquals("https://thisisatest.com/some/path/123/", routeWithDirectorySubPath.getBaseUrl());
    assertEquals("https://thisisatest.com/", routeWithPathWildcard.getBaseUrl());
    assertEquals("https://thisisatest.com/sub/path/", routeWithSubPathAndPathWildcard.getBaseUrl());
  }

  @Test
  void testEqualsAndHashCodeGivenSameIdentityAndRouteProperties() {
    Route route = createRoute();
    Route other = createRoute();

    assertEquals(route, other);
    assertEquals(route.hashCode(), other.hashCode());
  }

  @Test
  void testEqualsGivenSameIdentityButDifferentRouteProperty() {
    Route route = createRoute();

    assertNotEquals(route, createRoute().setDescription("Other description"));
    assertNotEquals(route, createRoute().setDisabled(false));
    assertNotEquals(route, createRoute().setUrl("https://other.com"));
    assertNotEquals(route, createRoute().setHeaders(Map.of("X-Other", "1")));
    assertNotEquals(route, createRoute().setAuth(new ApiTokenAuthScheme().setToken("other")));
    assertNotEquals(route, createRoute().setAuthorities(List.of("F_OTHER")));
    assertNotEquals(route, createRoute().setResponseTimeoutSeconds(30));
  }

  @Test
  void testEqualsGivenDifferentIdentity() {
    Route route = createRoute();
    Route otherUid = createRoute();
    otherUid.setUid("RouteUid002");
    Route otherCode = createRoute();
    otherCode.setCode("ROUTE_B");
    Route otherName = createRoute();
    otherName.setName("Route B");

    assertNotEquals(route, otherUid);
    assertNotEquals(route, otherCode);
    assertNotEquals(route, otherName);
  }

  private static Route createRoute() {
    Route route =
        new Route()
            .setDescription("Description")
            .setDisabled(true)
            .setUrl("https://thisisatest.com/**")
            .setHeaders(Map.of("X-Header", "value"))
            .setAuthorities(List.of("F_ROUTE"))
            .setResponseTimeoutSeconds(10);
    route.setUid("RouteUid001");
    route.setCode("ROUTE_A");
    route.setName("Route A");
    return route;
  }
}
