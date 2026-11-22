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
package org.hisp.dhis.webapi.controller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.test.webapi.AuthenticationApiTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.security.CrossOriginGetGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tests the cross-origin GET guard through the Spring Security filter chain. The guard trusts a
 * request with an {@code Origin} header because the CORS origin check earlier in the chain stops
 * origins that are not allowed; these tests check that the two layers together let an allowed
 * origin change state and keep a denied origin away from the handler.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class CrossOriginGetFilterChainTest extends AuthenticationApiTestBase {
  private static final String ALLOWED_ORIGIN = "https://allowed.example.org";

  private static final String DENIED_ORIGIN = "https://denied.example.org";

  /** A handler annotated with {@code @RejectCrossOriginGet} that enrolls the user in TOTP 2FA. */
  private static final String QR_CODE_URL = "/api/2fa/qrCode";

  @Autowired private ConfigurationService configurationService;

  private Set<String> previousCorsWhitelist;

  private User user;

  @BeforeEach
  void setUp() {
    previousCorsWhitelist = new HashSet<>(configurationService.getCorsWhitelist());
    configurationService.setCorsWhitelist(Set.of(ALLOWED_ORIGIN));
    user = switchToNewUser("crossoriginuser");
  }

  @AfterEach
  void restoreCorsWhitelist() {
    // the allowlist is cached outside the test transaction
    configurationService.setCorsWhitelist(previousCorsWhitelist);
  }

  @Test
  void testAllowedOriginReachesHandler() throws Exception {
    MockHttpServletResponse response = getQrCode(ALLOWED_ORIGIN);

    assertEquals(202, response.getStatus(), response.getContentAsString());
    assertEquals(ALLOWED_ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
    assertTrue(response.getContentAsByteArray().length > 0);
    assertEquals(TwoFactorType.ENROLLING_TOTP, twoFactorType());
  }

  @Test
  void testDeniedOriginStopsBeforeHandler() throws Exception {
    MockHttpServletResponse response = getQrCode(DENIED_ORIGIN);

    assertNotEquals(202, response.getStatus());
    assertNull(response.getHeader("Access-Control-Allow-Origin"));
    assertEquals(0, response.getContentAsByteArray().length);
    assertEquals(TwoFactorType.NOT_ENABLED, twoFactorType());
  }

  @Test
  void testCrossSiteWithoutOriginRejectedByGuard() throws Exception {
    MockHttpServletResponse response = getQrCode(null);

    assertEquals(403, response.getStatus(), response.getContentAsString());
    assertTrue(
        response.getContentAsString().contains(CrossOriginGetGuard.MESSAGE),
        response.getContentAsString());
    assertTrue(
        response.getContentAsString().contains("\"errorCode\":\"E1008\""),
        response.getContentAsString());
    assertEquals(TwoFactorType.NOT_ENABLED, twoFactorType());
  }

  /** Sends the qrCode GET as a cross-site fetch, with the given Origin or without one. */
  private MockHttpServletResponse getQrCode(String origin) throws Exception {
    MockHttpServletRequestBuilder request =
        get(QR_CODE_URL)
            .session(session)
            // a browser image Accept header; a bare */* resolves to JSON, which the handler cannot
            // produce
            .header("Accept", "image/avif,image/webp,*/*;q=0.8")
            .header("Sec-Fetch-Site", "cross-site")
            .header("Sec-Fetch-Mode", origin == null ? "navigate" : "cors")
            .header("Sec-Fetch-Dest", origin == null ? "document" : "empty");
    if (origin != null) {
      request.header("Origin", origin);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private TwoFactorType twoFactorType() {
    return userService.getUser(user.getUid()).getTwoFactorType();
  }
}
