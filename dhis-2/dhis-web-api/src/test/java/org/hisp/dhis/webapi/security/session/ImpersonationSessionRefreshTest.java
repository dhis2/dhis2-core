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
package org.hisp.dhis.webapi.security.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.DhisAuthenticationToken;
import org.hisp.dhis.security.DhisAuthenticationToken.LoginMethod;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.hisp.dhis.user.UserDetailsImpl.UserDetailsImplBuilder;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.security.ImpersonateUserController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.SerializationUtils;

/**
 * Exercises impersonation transitions and subsequent requests through the refresh filter.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ImpersonationSessionRefreshTest {
  private final UserDetails source = user("source", "F_OLD");
  private final UserDetails target = user("target", "F_VIEW");
  private UserDetailsChangeTracker tracker;
  private UserService userService;
  private UserDetailsService detailsService;
  private ImpersonateUserController controller;
  private UserDetailsRefreshFilter filter;
  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    tracker = mock(UserDetailsChangeTracker.class);
    userService = mock(UserService.class);
    detailsService = mock(UserDetailsService.class);
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    when(config.isEnabled(ConfigurationKey.SWITCH_USER_FEATURE_ENABLED)).thenReturn(true);
    when(config.getProperty(ConfigurationKey.SWITCH_USER_ALLOW_LISTED_IPS)).thenReturn("127.0.0.1");
    when(detailsService.loadUserByUsername("target")).thenReturn(target);
    UserDetailsSessionRefresher sessionRefresher =
        new UserDetailsSessionRefresher(tracker, userService, Duration.ofMinutes(15));
    controller =
        new ImpersonateUserController(
            config, detailsService, mock(ApplicationEventPublisher.class), sessionRefresher);
    filter = new UserDetailsRefreshFilter(sessionRefresher);
    // a session of an earlier login, which the refresh filter checks on every request
    session = new MockHttpSession();
    session.setNew(false);
    SecurityContext context =
        new SecurityContextImpl(
            new DhisAuthenticationToken(source, null, source.getAuthorities(), LoginMethod.LOCAL));
    session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);
    new UserDetailsRefreshState("source-before", System.currentTimeMillis()).saveTo(session);
    SecurityContextHolder.setContext(context);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void targetChangesBeforeEntryDoNotEndTheSession(boolean changed) throws Exception {
    if (changed) {
      when(tracker.getChangeToken("target")).thenReturn("target-before");
    }
    controller.impersonateUser(request(), new MockHttpServletResponse(), "target");

    nextRequest();

    assertFalse(session.isInvalid());
    assertEquals("target", SecurityContextHolder.getContext().getAuthentication().getName());
  }

  @Test
  void targetChangeAfterEntryEndsTheSession() throws Exception {
    when(tracker.getChangeToken("target")).thenReturn("before");
    controller.impersonateUser(request(), new MockHttpServletResponse(), "target");
    when(tracker.getChangeToken("target")).thenReturn("after");

    nextRequest();

    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void changeWhileLoadingTheTargetIsNotAcknowledged() throws Exception {
    when(detailsService.loadUserByUsername("target"))
        .thenAnswer(
            call -> {
              when(tracker.getChangeToken("target")).thenReturn("during-load");
              return target;
            });
    controller.impersonateUser(request(), new MockHttpServletResponse(), "target");

    nextRequest();

    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void exitRefreshesSourceChangesEvenAfterSwitchingTargets(boolean switchAgain) throws Exception {
    when(tracker.getChangeToken("target")).thenReturn("target-before");
    controller.impersonateUser(request(), new MockHttpServletResponse(), "target");
    if (switchAgain) {
      when(detailsService.loadUserByUsername("second")).thenReturn(user("second", "F_VIEW"));
      controller.impersonateUser(request(), new MockHttpServletResponse(), "second");
    }
    Serializable state =
        (Serializable)
            SerializationUtils.deserialize(SerializationUtils.serialize(session.serializeState()));
    session = new MockHttpSession();
    session.deserializeState(state);
    SecurityContextHolder.setContext(
        (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY));
    when(tracker.getChangeToken("source")).thenReturn("source-after");
    when(userService.createUserDetailsSafe(source.getUid())).thenReturn(user("source", "F_NEW"));
    controller.impersonateExit(request(), new MockHttpServletResponse(), target);

    nextRequest();

    assertFalse(session.isInvalid());
    UserDetails refreshed =
        (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertEquals(source.getUid(), refreshed.getUid());
    assertTrue(refreshed.isAuthorized("F_NEW"));
    assertFalse(refreshed.isAuthorized("F_OLD"));
  }

  @Test
  void sourceChangeAfterEntryEndsTheSession() throws Exception {
    controller.impersonateUser(request(), new MockHttpServletResponse(), "target");
    when(tracker.getChangeToken("source")).thenReturn("source-after");

    nextRequest();

    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  /** No live token, or the token the source session had already applied before the entry. */
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = "source-before")
  void sourceChangesAppliedBeforeEntryDoNotEndTheSession(String sourceToken) throws Exception {
    when(tracker.getChangeToken("source")).thenReturn(sourceToken);
    controller.impersonateUser(request(), new MockHttpServletResponse(), "target");

    nextRequest();

    assertFalse(session.isInvalid());
    assertEquals("target", SecurityContextHolder.getContext().getAuthentication().getName());
  }

  @Test
  void impersonationWithoutSourceBaselineEndsOnAnyLiveSourceToken() throws Exception {
    when(tracker.getChangeToken("source")).thenReturn("source-before");
    controller.impersonateUser(request(), new MockHttpServletResponse(), "target");
    UserDetailsRefreshState state = UserDetailsRefreshState.of(session);
    new UserDetailsRefreshState(state.changeToken(), state.refreshedAt()).saveTo(session);

    nextRequest();

    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  static Stream<Arguments> requestsThatAuthenticateThemselves() {
    return Stream.of(
        Arguments.of("HTTP basic, no session", false, false),
        Arguments.of("HTTP basic, session created by the request", true, false),
        Arguments.of("personal access token, no session", false, true),
        Arguments.of("personal access token, session created by the request", true, true));
  }

  /**
   * A request that authenticates itself built the principal of the impersonating user from its
   * current user details, so a change applied before the impersonation does not end it.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("requestsThatAuthenticateThemselves")
  void impersonationByARequestThatAuthenticatesItselfSurvivesTheNextRequest(
      String scenario, boolean sessionCreatedByRequest, boolean personalAccessToken)
      throws Exception {
    when(tracker.getChangeToken("source")).thenReturn("source-before");

    startImpersonationAuthenticatedByTheRequest(sessionCreatedByRequest, personalAccessToken);
    nextRequest();

    assertFalse(session.isInvalid());
    assertEquals("target", SecurityContextHolder.getContext().getAuthentication().getName());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("requestsThatAuthenticateThemselves")
  void sourceChangeAfterAnImpersonationByARequestThatAuthenticatesItselfEndsTheSession(
      String scenario, boolean sessionCreatedByRequest, boolean personalAccessToken)
      throws Exception {
    when(tracker.getChangeToken("source")).thenReturn("source-before");

    startImpersonationAuthenticatedByTheRequest(sessionCreatedByRequest, personalAccessToken);
    when(tracker.getChangeToken("source")).thenReturn("source-after");
    nextRequest();

    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  /**
   * An existing session whose authentication the request replaced, here with HTTP basic for another
   * user, has no refresh state for the new authentication.
   */
  @Test
  void impersonationByARequestThatReplacedTheSessionAuthenticationSurvivesTheNextRequest()
      throws Exception {
    UserDetails other = user("other", "F_IMPERSONATE_USER");
    when(tracker.getChangeToken("other")).thenReturn("other-before");
    SecurityContextHolder.setContext(
        new SecurityContextImpl(
            new DhisAuthenticationToken(other, null, other.getAuthorities(), LoginMethod.LOCAL)));

    controller.impersonateUser(request(), new MockHttpServletResponse(), "target");
    nextRequest();

    assertFalse(session.isInvalid());
    assertEquals("target", SecurityContextHolder.getContext().getAuthentication().getName());
  }

  /**
   * Authenticates the impersonation request itself, as HTTP basic or a personal access token does:
   * the session, if any, was created by the request and holds no refresh state.
   */
  private void startImpersonationAuthenticatedByTheRequest(
      boolean sessionCreatedByRequest, boolean personalAccessToken) throws Exception {
    Authentication authentication;
    if (personalAccessToken) {
      authentication = new ApiTokenAuthenticationToken(new ApiToken(), source);
      authentication.setAuthenticated(true);
    } else {
      authentication =
          new DhisAuthenticationToken(source, null, source.getAuthorities(), LoginMethod.LOCAL);
    }
    SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    if (sessionCreatedByRequest) {
      request.setSession(new MockHttpSession());
    }

    controller.impersonateUser(request, new MockHttpServletResponse(), "target");

    session = (MockHttpSession) request.getSession(false);
    session.setNew(false);
  }

  static Stream<Arguments> sourcesThatMayNoLongerImpersonate() {
    return Stream.of(
        Arguments.of("deleted", null),
        Arguments.of("disabled", builder("source", "F_IMPERSONATE_USER").enabled(false).build()),
        Arguments.of(
            "expired", builder("source", "F_IMPERSONATE_USER").accountNonExpired(false).build()),
        Arguments.of("without F_IMPERSONATE_USER", user("source", "F_VIEW")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("sourcesThatMayNoLongerImpersonate")
  void refreshIntervalEndsTheSessionWhenTheSourceMayNoLongerImpersonate(
      String scenario, UserDetails refreshedSource) throws Exception {
    controller.impersonateUser(request(), new MockHttpServletResponse(), "target");
    makeRefreshDue();
    when(userService.createUserDetailsSafe(source.getUid())).thenReturn(refreshedSource);

    nextRequest();

    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @ParameterizedTest
  @ValueSource(strings = {"F_IMPERSONATE_USER", "ALL"})
  void refreshIntervalKeepsTheSessionWhileTheSourceMayImpersonate(String authority)
      throws Exception {
    controller.impersonateUser(request(), new MockHttpServletResponse(), "target");
    Authentication impersonation =
        ((SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY)).getAuthentication();
    makeRefreshDue();
    when(userService.createUserDetailsSafe(source.getUid())).thenReturn(user("source", authority));
    long beforeCheck = System.currentTimeMillis();

    nextRequest();
    nextRequest();

    assertFalse(session.isInvalid());
    assertSame(impersonation, SecurityContextHolder.getContext().getAuthentication());
    verify(userService, times(1)).createUserDetailsSafe(anyString());
    assertTrue(UserDetailsRefreshState.of(session).refreshedAt() >= beforeCheck);
  }

  private void makeRefreshDue() {
    UserDetailsRefreshState state = UserDetailsRefreshState.of(session);
    new UserDetailsRefreshState(state.changeToken(), 0L, state.impersonatorState()).saveTo(session);
  }

  private void nextRequest() throws Exception {
    SecurityContextHolder.setContext(
        (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY));
    filter.doFilter(request(), new MockHttpServletResponse(), new MockFilterChain());
  }

  private MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    request.setSession(session);
    return request;
  }

  private static UserDetails user(String username, String authority) {
    return builder(username, authority).build();
  }

  private static UserDetailsImplBuilder builder(String username, String authority) {
    return UserDetails.empty()
        .uid(username + "Uid01")
        .username(username)
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .authorities(List.of(new SimpleGrantedAuthority(authority)))
        .allAuthorities(Set.of(authority));
  }
}
