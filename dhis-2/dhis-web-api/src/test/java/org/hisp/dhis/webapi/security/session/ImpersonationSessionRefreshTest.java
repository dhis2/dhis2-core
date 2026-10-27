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

import static org.hisp.dhis.webapi.security.session.UserDetailsRefreshFilter.CHANGE_TOKEN_ATTRIBUTE;
import static org.hisp.dhis.webapi.security.session.UserDetailsRefreshFilter.REFRESHED_AT_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.DhisAuthenticationToken;
import org.hisp.dhis.security.DhisAuthenticationToken.LoginMethod;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.security.ImpersonateUserController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
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
    controller =
        new ImpersonateUserController(
            config, detailsService, mock(ApplicationEventPublisher.class), tracker);
    filter = new UserDetailsRefreshFilter(tracker, userService, Duration.ofMinutes(15));
    session = new MockHttpSession();
    SecurityContext context =
        new SecurityContextImpl(
            new DhisAuthenticationToken(source, null, source.getAuthorities(), LoginMethod.LOCAL));
    session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);
    session.setAttribute(CHANGE_TOKEN_ATTRIBUTE, "source-before");
    session.setAttribute(REFRESHED_AT_ATTRIBUTE, System.currentTimeMillis());
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
    return UserDetails.empty()
        .uid(username + "Uid01")
        .username(username)
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .authorities(List.of(new SimpleGrantedAuthority(authority)))
        .allAuthorities(Set.of(authority))
        .build();
  }
}
