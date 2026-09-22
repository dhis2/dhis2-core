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
package org.hisp.dhis.webapi.security.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.basic.HttpBasicWebAuthenticationDetails;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AuthenticationListenerTest {

  private static final String LOGIN_COUNTER_NAME = "dhis2_user_logins_total";

  @Mock private UserService userService;

  @Mock private DhisConfigurationProvider config;

  private SimpleMeterRegistry meterRegistry;

  private AuthenticationListener listener;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    listener = new AuthenticationListener();
    ReflectionTestUtils.setField(listener, "userService", userService);
    ReflectionTestUtils.setField(listener, "config", config);
    ReflectionTestUtils.setField(listener, "meterRegistry", meterRegistry);

    lenient()
        .when(userService.getUserByUsername(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(null);
    lenient().when(config.isReadOnlyMode()).thenReturn(false);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void formLoginIsCountedUnderFormMethod() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(1.0, counterValue("form"));
    assertEquals(0.0, counterValue("basic"));
    assertEquals(0.0, counterValue("oidc"));
  }

  @Test
  void basicAuthIsCountedUnderBasicMethod() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");
    when(auth.getDetails()).thenReturn(mock(HttpBasicWebAuthenticationDetails.class));

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(1.0, counterValue("basic"));
    assertEquals(0.0, counterValue("form"));
  }

  @Test
  void apiTokenIsCountedUnderApitokenMethod() {
    ApiTokenAuthenticationToken auth = mock(ApiTokenAuthenticationToken.class);
    when(auth.getName()).thenReturn("pat-user");

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(1.0, counterValue("apitoken"));
    assertEquals(0.0, counterValue("form"));
    assertEquals(0.0, counterValue("basic"));
  }

  @Test
  void oidcLoginIsCountedUnderOidcMethod() {
    OAuth2LoginAuthenticationToken auth = mock(OAuth2LoginAuthenticationToken.class);
    when(auth.getName()).thenReturn("oidcuser");

    DhisOidcUser principal = mock(DhisOidcUser.class);
    UserDetails springUserDetails = mock(UserDetails.class);
    when(principal.getUser()).thenReturn(springUserDetails);
    when(springUserDetails.getUsername()).thenReturn("oidcuser");
    when(auth.getPrincipal()).thenReturn(principal);

    WebAuthenticationDetails details = mock(WebAuthenticationDetails.class);
    when(details.getRemoteAddress()).thenReturn("127.0.0.1");
    when(auth.getDetails()).thenReturn(details);

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(1.0, counterValue("oidc"));
    assertEquals(0.0, counterValue("form"));
  }

  @Test
  void formLoginIsNotDoubleCountedWhenBothEventsFireForTheSameLogin() {
    // AuthenticationController (the /api/auth/login endpoint) publishes both an
    // InteractiveAuthenticationSuccessEvent and an AuthenticationSuccessEvent for the same
    // login - only the latter should be counted.
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));
    listener.handleAuthenticationSuccess(
        new InteractiveAuthenticationSuccessEvent(auth, getClass()));

    assertEquals(1.0, counterValue("form"));
  }

  @Test
  void interactiveEventAloneIsNotCounted() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");

    listener.handleAuthenticationSuccess(
        new InteractiveAuthenticationSuccessEvent(auth, getClass()));

    assertEquals(0.0, counterValue("form"));
  }

  @Test
  void metricsScrapeRequestIsExcludedFromTheCounter() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("prometheus");

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/metrics");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(0.0, counterValue("form"));
  }

  @Test
  void nonMetricsRequestIsStillCountedWhenRequestContextIsPresent() {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/dataElements");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(1.0, counterValue("form"));
  }

  @Test
  void missingRequestContextFailsOpenAndStillCounts() {
    // No RequestContextHolder attributes bound (e.g. embeddedJetty profile does not register
    // RequestContextListener) - the exclusion must never suppress a real login.
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("testuser");

    listener.handleAuthenticationSuccess(new AuthenticationSuccessEvent(auth));

    assertEquals(1.0, counterValue("form"));
  }

  private double counterValue(String method) {
    Counter counter = meterRegistry.find(LOGIN_COUNTER_NAME).tag("method", method).counter();
    return counter == null ? 0.0 : counter.count();
  }
}
