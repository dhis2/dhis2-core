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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.hisp.dhis.security.DhisAuthenticationToken;
import org.hisp.dhis.security.DhisAuthenticationToken.LoginMethod;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.hisp.dhis.user.UserDetailsImpl.UserDetailsImplBuilder;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.util.SerializationUtils;

/**
 * Unit test of {@link UserDetailsRefreshFilter}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class UserDetailsRefreshFilterTest {

  private static final String USERNAME = "alice";
  private static final String USER_UID = "aliceUid001";

  private static final GrantedAuthority PASSWORD_FACTOR =
      FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY);

  private UserDetailsChangeTracker tracker;

  private UserService userService;

  private UserDetailsRefreshFilter filter;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    tracker = mock(UserDetailsChangeTracker.class);
    userService = mock(UserService.class);
    filter = new UserDetailsRefreshFilter(tracker, userService, Duration.ZERO);
    session = new MockHttpSession();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void keepsPrincipalWhenUserDidNotChange() throws Exception {
    Authentication authentication = passwordLogin(user("F_OLD"));

    doFilter(authentication);

    verify(userService, never()).createUserDetailsSafe(anyString());
    assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void keepsPrincipalWhenChangeWasAlreadyApplied() throws Exception {
    when(tracker.getChangeToken(USERNAME)).thenReturn("token1");
    session.setAttribute(CHANGE_TOKEN_ATTRIBUTE, "token1");

    doFilter(passwordLogin(user("F_OLD")));

    verify(userService, never()).createUserDetailsSafe(anyString());
  }

  @Test
  void refreshesPasswordLoginKeepingDetailsAndLoginFactor() throws Exception {
    when(tracker.getChangeToken(USERNAME)).thenReturn("token1");
    UserDetails refreshed = user("F_NEW");
    when(userService.createUserDetailsSafe(USER_UID)).thenReturn(refreshed);
    UsernamePasswordAuthenticationToken login = passwordLogin(user("F_OLD"));
    login.setDetails("loginDetails");

    doFilter(login);

    Authentication current = SecurityContextHolder.getContext().getAuthentication();
    assertInstanceOf(UsernamePasswordAuthenticationToken.class, current);
    assertSame(refreshed, current.getPrincipal());
    assertEquals(
        Set.of("F_NEW", PASSWORD_FACTOR.getAuthority()), authorityNames(current.getAuthorities()));
    assertEquals("loginDetails", current.getDetails());
    assertSame(current, savedAuthentication());
    assertEquals("token1", session.getAttribute(CHANGE_TOKEN_ATTRIBUTE));
    assertFalse(session.isInvalid());
  }

  @Test
  void refreshesOidcLoginKeepingClaimsAndClientRegistration() throws Exception {
    when(tracker.getChangeToken(USERNAME)).thenReturn("token1");
    UserDetails refreshed = user("F_NEW");
    when(userService.createUserDetailsSafe(USER_UID)).thenReturn(refreshed);
    OidcIdToken idToken =
        new OidcIdToken("id", Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", "s1"));
    DhisOidcUser oidcUser = new DhisOidcUser(user("F_OLD"), Map.of("sub", "s1"), "sub", idToken);
    OAuth2AuthenticationToken login =
        new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "keycloak");

    doFilter(login);

    OAuth2AuthenticationToken current =
        assertInstanceOf(
            OAuth2AuthenticationToken.class,
            SecurityContextHolder.getContext().getAuthentication());
    DhisOidcUser principal = assertInstanceOf(DhisOidcUser.class, current.getPrincipal());
    assertSame(refreshed, principal.getUser());
    assertEquals("s1", principal.getName());
    assertSame(idToken, principal.getIdToken());
    assertEquals("keycloak", current.getAuthorizedClientRegistrationId());
    assertEquals(Set.of("F_NEW"), authorityNames(current.getAuthorities()));
    assertSame(current, savedAuthentication());
  }

  @Test
  void storesTheTokenReadBeforeTheRebuild() throws Exception {
    when(tracker.getChangeToken(USERNAME)).thenReturn("token1");
    when(userService.createUserDetailsSafe(USER_UID))
        .thenAnswer(
            call -> {
              // another change commits while the principal is being rebuilt
              when(tracker.getChangeToken(USERNAME)).thenReturn("token2");
              return user("F_NEW");
            });

    doFilter(passwordLogin(user("F_OLD")));
    assertEquals("token1", session.getAttribute(CHANGE_TOKEN_ATTRIBUTE));

    doFilter(SecurityContextHolder.getContext().getAuthentication());
    verify(userService, times(2)).createUserDetailsSafe(USER_UID);
    assertEquals("token2", session.getAttribute(CHANGE_TOKEN_ATTRIBUTE));
  }

  @Test
  void ignoresAuthenticationsNotKeptInTheSession() throws Exception {
    when(tracker.getChangeToken(USERNAME)).thenReturn("token1");

    doFilter(new TestingAuthenticationToken(user("F_OLD"), null, "F_OLD"));

    verify(userService, never()).createUserDetailsSafe(anyString());
  }

  @Test
  void refreshesPrincipalOlderThanTheRefreshIntervalWithoutChange() throws Exception {
    filter = new UserDetailsRefreshFilter(tracker, userService, Duration.ofMinutes(15));
    UserDetails refreshed = user("F_NEW");
    when(userService.createUserDetailsSafe(USER_UID)).thenReturn(refreshed);
    long refreshedAt = System.currentTimeMillis() - Duration.ofMinutes(16).toMillis();
    session.setAttribute(REFRESHED_AT_ATTRIBUTE, refreshedAt);

    doFilter(passwordLogin(user("F_OLD")));

    assertSame(refreshed, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    assertTrue((Long) session.getAttribute(REFRESHED_AT_ATTRIBUTE) > refreshedAt);
    assertNull(session.getAttribute(CHANGE_TOKEN_ATTRIBUTE));
  }

  @Test
  void keepsPrincipalYoungerThanTheRefreshInterval() throws Exception {
    filter = new UserDetailsRefreshFilter(tracker, userService, Duration.ofMinutes(15));
    session.setAttribute(
        REFRESHED_AT_ATTRIBUTE, System.currentTimeMillis() - Duration.ofMinutes(14).toMillis());

    doFilter(passwordLogin(user("F_OLD")));

    verify(userService, never()).createUserDetailsSafe(anyString());
  }

  @Test
  void refreshIntervalDoesNotEndImpersonation() throws Exception {
    filter = new UserDetailsRefreshFilter(tracker, userService, Duration.ofMinutes(15));
    session.setAttribute(
        REFRESHED_AT_ATTRIBUTE, System.currentTimeMillis() - Duration.ofMinutes(16).toMillis());

    doFilter(impersonation(user("F_OLD")));

    assertFalse(session.isInvalid());
    verify(userService, never()).createUserDetailsSafe(anyString());
  }

  @Test
  void refreshIntervalEndsSessionOfDisabledUser() throws Exception {
    filter = new UserDetailsRefreshFilter(tracker, userService, Duration.ofMinutes(15));
    when(userService.createUserDetailsSafe(USER_UID))
        .thenReturn(userBuilder("F_OLD").enabled(false).build());
    session.setAttribute(
        REFRESHED_AT_ATTRIBUTE, System.currentTimeMillis() - Duration.ofMinutes(16).toMillis());

    doFilter(passwordLogin(user("F_OLD")));

    assertTrue(session.isInvalid());
  }

  static Stream<Arguments> sessionsToEnd() {
    return Stream.of(
        Arguments.of("impersonation", true, refreshedAs(builder -> builder)),
        Arguments.of("deleted user", false, null),
        Arguments.of("disabled user", false, refreshedAs(builder -> builder.enabled(false))),
        Arguments.of(
            "expired user", false, refreshedAs(builder -> builder.accountNonExpired(false))),
        Arguments.of(
            "two-factor set up required",
            false,
            refreshedAs(builder -> builder.allRestrictions(Set.of("R_ENABLE_2FA")))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("sessionsToEnd")
  void endsSessionInsteadOfRefreshing(
      String scenario, boolean impersonating, UnaryOperator<UserDetailsImplBuilder> refreshed)
      throws Exception {
    when(tracker.getChangeToken(USERNAME)).thenReturn("token1");
    when(userService.createUserDetailsSafe(USER_UID))
        .thenReturn(refreshed == null ? null : refreshed.apply(userBuilder("F_NEW")).build());
    UserDetails principal = user("F_OLD");

    doFilter(impersonating ? impersonation(principal) : passwordLogin(principal));

    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void deletedAccountCannotBecomeAnotherAccountWithTheSameUsername(boolean timer) throws Exception {
    filter = new UserDetailsRefreshFilter(tracker, userService, Duration.ofMinutes(15));
    session.setAttribute(REFRESHED_AT_ATTRIBUTE, 0L);
    if (!timer) {
      when(tracker.getChangeToken(USERNAME)).thenReturn("replacement");
    }
    when(userService.createUserDetailsByUsername(USERNAME))
        .thenReturn(userBuilder("ALL").uid("otherUid001").build());
    when(userService.createUserDetailsSafe(USER_UID)).thenReturn(null);

    doFilter(passwordLogin(user("F_OLD")));

    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void renamedAccountKeepsItsIdentity() throws Exception {
    when(tracker.getChangeToken(USERNAME)).thenReturn("renamed");
    UserDetails renamed = userBuilder("F_NEW").username("renamed").build();
    when(userService.createUserDetailsSafe(USER_UID)).thenReturn(renamed);

    doFilter(passwordLogin(user("F_OLD")));

    UserDetails principal = (UserDetails) savedAuthentication().getPrincipal();
    assertEquals(USER_UID, principal.getUid());
    assertEquals("renamed", principal.getUsername());
    assertFalse(session.isInvalid());
  }

  @ParameterizedTest
  @CsvSource({"OIDC,false", "OIDC,true", "LDAP,false", "LDAP,true", "LOCAL,false", "LOCAL,true"})
  void refreshKeepsTheLoginProvidersEnrolmentPolicy(String provider, boolean timer)
      throws Exception {
    filter = new UserDetailsRefreshFilter(tracker, userService, Duration.ofMinutes(15));
    session.setAttribute(REFRESHED_AT_ATTRIBUTE, 0L);
    if (!timer) {
      when(tracker.getChangeToken(USERNAME)).thenReturn("token1");
    }
    UserDetails initial = userBuilder("F_OLD").externalAuth(!provider.equals("LOCAL")).build();
    UserDetails refreshed =
        userBuilder("F_NEW")
            .externalAuth(provider.equals("LOCAL"))
            .allRestrictions(Set.of("R_ENABLE_2FA"))
            .build();
    when(userService.createUserDetailsSafe(USER_UID)).thenReturn(refreshed);
    Authentication login;
    if (provider.equals("OIDC")) {
      Map<String, Object> claims = Map.of("sub", "s1");
      OidcIdToken idToken =
          new OidcIdToken("id", Instant.now(), Instant.now().plusSeconds(60), claims);
      DhisOidcUser principal = new DhisOidcUser(initial, claims, "sub", idToken);
      login = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "keycloak");
    } else {
      login =
          new DhisAuthenticationToken(
              initial, null, initial.getAuthorities(), LoginMethod.valueOf(provider));
    }

    doFilter(login);

    if (provider.equals("LOCAL")) {
      assertTrue(session.isInvalid());
      assertNull(SecurityContextHolder.getContext().getAuthentication());
      return;
    }
    assertFalse(session.isInvalid());
    assertEquals(Set.of("F_NEW"), authorityNames(savedAuthentication().getAuthorities()));
    Authentication restored =
        (Authentication)
            SerializationUtils.deserialize(SerializationUtils.serialize(savedAuthentication()));
    when(tracker.getChangeToken(USERNAME)).thenReturn("token2");
    doFilter(restored);
    assertFalse(session.isInvalid());
    assertEquals(Set.of("F_NEW"), authorityNames(savedAuthentication().getAuthorities()));
    if (provider.equals("LDAP")) {
      assertEquals(
          LoginMethod.LDAP,
          assertInstanceOf(DhisAuthenticationToken.class, savedAuthentication()).getLoginMethod());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void concurrentLogoutNeverAuthenticatesAReplacementSession(boolean replacement) throws Exception {
    when(tracker.getChangeToken(USERNAME)).thenReturn("token1");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    SecurityContext replacementContext = new SecurityContextImpl();
    when(userService.createUserDetailsSafe(USER_UID))
        .thenAnswer(
            call -> {
              session.invalidate();
              if (replacement) {
                request
                    .getSession(true)
                    .setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        replacementContext);
              }
              return user("F_NEW");
            });
    SecurityContextHolder.setContext(new SecurityContextImpl(passwordLogin(user("F_OLD"))));

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    if (replacement) {
      assertSame(
          replacementContext,
          request
              .getSession(false)
              .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
      assertNull(replacementContext.getAuthentication());
    } else {
      assertNull(request.getSession(false));
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        CHANGE_TOKEN_ATTRIBUTE,
        REFRESHED_AT_ATTRIBUTE,
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
      })
  void concurrentLogoutDuringSessionWriteClearsAuthentication(String attribute) throws Exception {
    session =
        new MockHttpSession() {
          @Override
          public void setAttribute(String name, Object value) {
            if (name.equals(attribute)) {
              invalidate();
            }
            super.setAttribute(name, value);
          }
        };
    when(tracker.getChangeToken(USERNAME)).thenReturn("token1");
    when(userService.createUserDetailsSafe(USER_UID)).thenReturn(user("F_NEW"));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    SecurityContextHolder.setContext(new SecurityContextImpl(passwordLogin(user("F_OLD"))));

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertTrue(session.isInvalid());
    assertNull(request.getSession(false));
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  private void doFilter(Authentication authentication) throws Exception {
    SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
  }

  private static UsernamePasswordAuthenticationToken impersonation(UserDetails user) {
    List<GrantedAuthority> authorities = new ArrayList<>(user.getAuthorities());
    authorities.add(
        new SwitchUserGrantedAuthority("ROLE_PREVIOUS_ADMINISTRATOR", passwordLogin(user("A"))));
    return UsernamePasswordAuthenticationToken.authenticated(user, null, authorities);
  }

  private Authentication savedAuthentication() {
    SecurityContext saved =
        (SecurityContext)
            session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    return saved.getAuthentication();
  }

  private static UsernamePasswordAuthenticationToken passwordLogin(UserDetails user) {
    List<GrantedAuthority> authorities = new ArrayList<>(user.getAuthorities());
    authorities.add(PASSWORD_FACTOR);
    return new DhisAuthenticationToken(user, null, authorities, LoginMethod.LOCAL);
  }

  private static UserDetails user(String authority) {
    return userBuilder(authority).build();
  }

  private static UserDetailsImplBuilder userBuilder(String authority) {
    return UserDetails.empty()
        .uid(USER_UID)
        .username(USERNAME)
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .authorities(List.of(new SimpleGrantedAuthority(authority)))
        .allAuthorities(Set.of(authority));
  }

  private static UnaryOperator<UserDetailsImplBuilder> refreshedAs(
      UnaryOperator<UserDetailsImplBuilder> operator) {
    return operator;
  }

  private static Set<String> authorityNames(Collection<? extends GrantedAuthority> authorities) {
    return Set.copyOf(authorities.stream().map(GrantedAuthority::getAuthority).toList());
  }
}
