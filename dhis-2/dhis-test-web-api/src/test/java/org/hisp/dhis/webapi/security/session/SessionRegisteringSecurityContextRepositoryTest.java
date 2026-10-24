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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.test.webapi.AuthenticationApiTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tests through the real security filter chain that sessions created by an HTTP Basic login are
 * registered, so that invalidating the user's sessions (as a role or user group change does)
 * expires them, also after the client authenticated again on an expired session.
 *
 * <p>The test base is {@code @Transactional}, so a membership change would never commit here; the
 * tests invalidate the sessions directly. That a membership change invalidates the user's sessions
 * is covered by {@code UserGroupSessionInvalidationControllerTest}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class SessionRegisteringSecurityContextRepositoryTest extends AuthenticationApiTestBase {

  private static final String LOGOUT_URL = "/dhis-web-commons-security/logout.action";

  @Autowired private SessionRegistry sessionRegistry;

  @Autowired private DhisConfigurationProvider config;

  private final List<String> sessionIds = new ArrayList<>();

  @AfterEach
  void removeSessions() {
    sessionIds.forEach(sessionRegistry::removeSessionInformation);
  }

  @Test
  void sessionOfBasicLoginIsExpiredOnEveryInvalidation() throws Exception {
    MockHttpSession session = basicLogin(get("/api/me"));

    userService.invalidateUserSessions("admin");
    mvc.perform(get("/api/me").session(session)).andExpect(redirectedUrl(LOGOUT_URL));

    MockHttpSession reauthenticated = basicLogin(get("/api/me").session(session));
    assertNotEquals(session.getId(), reauthenticated.getId());

    userService.invalidateUserSessions("admin");
    mvc.perform(get("/api/me").session(reauthenticated)).andExpect(redirectedUrl(LOGOUT_URL));
  }

  @Test
  void basicLoginsWithoutSessionCookieDoNotLogOutOtherSessions() throws Exception {
    MockHttpSession session = basicLogin(get("/api/me"));

    int maxSessions = config.getIntProperty(ConfigurationKey.MAX_SESSIONS_PER_USER);
    for (int i = 0; i <= maxSessions; i++) {
      basicLogin(get("/api/me"));
    }

    mvc.perform(get("/api/me").session(session)).andExpect(status().isOk());
  }

  private MockHttpSession basicLogin(MockHttpServletRequestBuilder request) throws Exception {
    MockHttpSession session =
        (MockHttpSession)
            mvc.perform(request.header(HttpHeaders.AUTHORIZATION, basic()))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    assertNotNull(session);
    sessionIds.add(session.getId());
    return session;
  }

  private static String basic() {
    return "Basic "
        + Base64.getEncoder().encodeToString("admin:district".getBytes(StandardCharsets.UTF_8));
  }
}
