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
package org.hisp.dhis.webapi.security.authz;

import static org.hisp.dhis.user.authz.AuthzConstants.SESSION_AUTHZ_EPOCH_ATTR;
import static org.hisp.dhis.user.authz.AuthzConstants.SESSION_AUTHZ_GEN_ATTR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.authz.AuthzService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Unit tests for {@link UserDetailsSoftRefreshFilter}.
 *
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsSoftRefreshFilterTest {

  @Mock private AuthzService authzService;
  @Mock private SecurityContextRepository securityContextRepository;
  @Mock private FilterChain filterChain;

  private UserDetailsSoftRefreshFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    filter = new UserDetailsSoftRefreshFilter(authzService, securityContextRepository);
    request = new MockHttpServletRequest("GET", "/api/me");
    response = new MockHttpServletResponse();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void noSessionPerformsZeroAuthzServiceCalls() throws Exception {
    // no session created
    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(authzService);
    verifyNoInteractions(securityContextRepository);
  }

  @Test
  void patTokenWithSessionIsSkipped() throws Exception {
    MockHttpSession session = new MockHttpSession();
    request.setSession(session);

    UserDetails principal = userDetails("pat-user", "F_PAT");
    ApiTokenAuthenticationToken pat = new ApiTokenAuthenticationToken(new ApiToken(), principal);
    pat.setAuthenticated(true);
    SecurityContextHolder.getContext().setAuthentication(pat);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(authzService);
    assertSame(pat, SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(securityContextRepository);
  }

  @Test
  void switchUserAuthorityIsSkipped() throws Exception {
    MockHttpSession session = new MockHttpSession();
    request.setSession(session);

    Authentication original =
        UsernamePasswordAuthenticationToken.authenticated(
            userDetails("admin", "ALL"), "n/a", List.of(new SimpleGrantedAuthority("ALL")));
    UserDetails impersonated = userDetails("alice", "F_USER");
    SwitchUserGrantedAuthority switchAuth =
        new SwitchUserGrantedAuthority("F_PREVIOUS_IMPERSONATOR_AUTHORITY", original);
    UsernamePasswordAuthenticationToken auth =
        UsernamePasswordAuthenticationToken.authenticated(
            impersonated, "n/a", List.of(switchAuth, new SimpleGrantedAuthority("F_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(authzService);
    assertSame(auth, SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void matchingEpochNeverCallsEffectiveGen() throws Exception {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SESSION_AUTHZ_EPOCH_ATTR, 7L);
    request.setSession(session);

    setUsernamePasswordAuth(userDetails("alice", "F_USER"));

    when(authzService.currentEpoch()).thenReturn(7L);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(authzService).currentEpoch();
    verify(authzService, never()).effectiveGen(any());
    verify(authzService, never()).getFreshUserDetails(any());
    verifyNoInteractions(securityContextRepository);
  }

  @Test
  void epochDiffersGenMatchesUpdatesAttrsWithoutRebuild() throws Exception {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SESSION_AUTHZ_EPOCH_ATTR, 1L);
    session.setAttribute(SESSION_AUTHZ_GEN_ATTR, 5L);
    request.setSession(session);

    UserDetails current = userDetails("alice", "F_USER");
    Authentication original = setUsernamePasswordAuth(current);

    when(authzService.currentEpoch()).thenReturn(2L);
    when(authzService.effectiveGen(current)).thenReturn(5L);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(authzService, never()).getFreshUserDetails(any());
    verifyNoInteractions(securityContextRepository);
    assertSame(original, SecurityContextHolder.getContext().getAuthentication());
    assertEquals(5L, session.getAttribute(SESSION_AUTHZ_GEN_ATTR));
    assertEquals(2L, session.getAttribute(SESSION_AUTHZ_EPOCH_ATTR));
  }

  @Test
  void epochAndGenDifferRebuildsPrincipalAndSavesContext() throws Exception {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SESSION_AUTHZ_EPOCH_ATTR, 1L);
    session.setAttribute(SESSION_AUTHZ_GEN_ATTR, 3L);
    request.setSession(session);

    UserDetails stale = userDetails("alice", "F_OLD");
    setUsernamePasswordAuth(stale);
    UserDetails fresh = userDetails("alice", "F_NEW");

    when(authzService.currentEpoch()).thenReturn(9L);
    when(authzService.effectiveGen(stale)).thenReturn(8L);
    when(authzService.getFreshUserDetails("alice")).thenReturn(fresh);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    Authentication after = SecurityContextHolder.getContext().getAuthentication();
    assertInstanceOf(UsernamePasswordAuthenticationToken.class, after);
    assertSame(fresh, after.getPrincipal());
    assertTrue(after.getAuthorities().stream().anyMatch(a -> "F_NEW".equals(a.getAuthority())));
    verify(securityContextRepository)
        .saveContext(eq(SecurityContextHolder.getContext()), eq(request), eq(response));
    assertEquals(8L, session.getAttribute(SESSION_AUTHZ_GEN_ATTR));
    assertEquals(9L, session.getAttribute(SESSION_AUTHZ_EPOCH_ATTR));
  }

  @Test
  void oidcTokenRebuildsDhisOidcUserPrincipal() throws Exception {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SESSION_AUTHZ_EPOCH_ATTR, 0L);
    session.setAttribute(SESSION_AUTHZ_GEN_ATTR, 0L);
    request.setSession(session);

    UserDetails stale = userDetails("oidc-user", "F_OLD");
    UserDetails fresh = userDetails("oidc-user", "F_NEW");
    Map<String, Object> attributes = Map.of("sub", "subject-1", "email", "u@example.com");
    OidcIdToken idToken =
        new OidcIdToken(
            "token-value",
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(3600),
            attributes);
    DhisOidcUser oidcUser = new DhisOidcUser(stale, attributes, "sub", idToken);
    OAuth2AuthenticationToken oauth2 =
        new OAuth2AuthenticationToken(oidcUser, stale.getAuthorities(), "google");
    SecurityContextHolder.getContext().setAuthentication(oauth2);

    when(authzService.currentEpoch()).thenReturn(4L);
    when(authzService.effectiveGen(stale)).thenReturn(2L);
    when(authzService.getFreshUserDetails("oidc-user")).thenReturn(fresh);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    Authentication after = SecurityContextHolder.getContext().getAuthentication();
    assertInstanceOf(OAuth2AuthenticationToken.class, after);
    OAuth2AuthenticationToken replaced = (OAuth2AuthenticationToken) after;
    assertEquals("google", replaced.getAuthorizedClientRegistrationId());
    assertInstanceOf(DhisOidcUser.class, replaced.getPrincipal());
    DhisOidcUser newPrincipal = (DhisOidcUser) replaced.getPrincipal();
    assertSame(fresh, newPrincipal.getUser());
    assertNotSame(oidcUser, newPrincipal);
    verify(securityContextRepository).saveContext(any(), eq(request), eq(response));
    assertEquals(2L, session.getAttribute(SESSION_AUTHZ_GEN_ATTR));
    assertEquals(4L, session.getAttribute(SESSION_AUTHZ_EPOCH_ATTR));
  }

  private static Authentication setUsernamePasswordAuth(UserDetails principal) {
    UsernamePasswordAuthenticationToken auth =
        UsernamePasswordAuthenticationToken.authenticated(
            principal, "n/a", principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
    return auth;
  }

  private static UserDetails userDetails(String username, String authority) {
    return UserDetails.empty()
        .uid("uid-" + username)
        .username(username)
        .authorities(List.of(new SimpleGrantedAuthority(authority)))
        .allAuthorities(Set.of(authority))
        .build();
  }
}
