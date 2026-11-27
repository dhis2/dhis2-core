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
package org.hisp.dhis.webapi.security.session;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.hisp.dhis.test.webapi.AuthenticationApiTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;

/**
 * Tests {@link ReauthenticatingExpiredSessionStrategy} through the real security filter chain: a
 * request on an expired session authenticates again when it carries credentials, and is redirected
 * to logout otherwise.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ReauthenticatingExpiredSessionStrategyTest extends AuthenticationApiTestBase {

  @Autowired private SessionRegistry sessionRegistry;

  private MockHttpSession expiredSession;

  @BeforeEach
  void expireAdminSession() {
    expiredSession = new MockHttpSession();
    sessionRegistry.registerNewSession(
        expiredSession.getId(), userService.createUserDetails(getAdminUser()));
    sessionRegistry.getSessionInformation(expiredSession.getId()).expireNow();
  }

  @Test
  void requestWithCredentialsOnExpiredSessionAuthenticatesAgain() throws Exception {
    mvc.perform(
            get("/api/me")
                .session(expiredSession)
                .header(HttpHeaders.AUTHORIZATION, basic("admin", "district")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("admin"));
  }

  @Test
  void requestWithWrongCredentialsOnExpiredSessionIsRejected() throws Exception {
    mvc.perform(
            get("/api/me")
                .session(expiredSession)
                .header(HttpHeaders.AUTHORIZATION, basic("admin", "wrong")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void requestWithoutCredentialsOnExpiredSessionIsRedirectedToLogout() throws Exception {
    mvc.perform(get("/api/me").session(expiredSession))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/dhis-web-commons-security/logout.action"));
  }

  private static String basic(String username, String password) {
    String credentials = username + ":" + password;
    return "Basic "
        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }
}
