/*
 * Copyright (c) 2004-2022, University of Oslo
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
