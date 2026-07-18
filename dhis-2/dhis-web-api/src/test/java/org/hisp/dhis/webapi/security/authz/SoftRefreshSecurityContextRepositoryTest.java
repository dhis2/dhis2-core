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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.authz.AuthzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * Unit tests for {@link SoftRefreshSecurityContextRepository}, using a real {@link
 * HttpSessionSecurityContextRepository} delegate so contexts load the way they do in production.
 *
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
class SoftRefreshSecurityContextRepositoryTest {

  @Mock private AuthzService authzService;

  private SoftRefreshSecurityContextRepository repository;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    repository =
        new SoftRefreshSecurityContextRepository(
            new HttpSessionSecurityContextRepository(), authzService);
    request = new MockHttpServletRequest("GET", "/api/me");
  }

  @Test
  void noSessionPerformsZeroAuthzServiceCalls() {
    repository.loadDeferredContext(request).get();

    verifyNoInteractions(authzService);
  }

  @Test
  void loadWithoutAccessPerformsZeroAuthzServiceCalls() {
    MockHttpSession session = new MockHttpSession();
    request.setSession(session);
    storeContext(
        session,
        UsernamePasswordAuthenticationToken.authenticated(
            userDetails("alice", "F_USER"), "n/a", List.of()));

    repository.loadDeferredContext(request); // never call get()

    verifyNoInteractions(authzService);
  }

  @Test
  void patTokenContextIsUntouched() {
    MockHttpSession session = new MockHttpSession();
    request.setSession(session);
    UserDetails principal = userDetails("pat-user", "F_PAT");
    ApiTokenAuthenticationToken pat = new ApiTokenAuthenticationToken(new ApiToken(), principal);
    pat.setAuthenticated(true);
    SecurityContext stored = storeContext(session, pat);

    SecurityContext loaded = repository.loadDeferredContext(request).get();

    assertSame(stored, loaded);
    verifyNoInteractions(authzService);
  }

  @Test
  void switchUserAuthorityIsSkipped() {
    MockHttpSession session = new MockHttpSession();
    request.setSession(session);
    Authentication original =
        UsernamePasswordAuthenticationToken.authenticated(
            userDetails("admin", "ALL"), "n/a", List.of(new SimpleGrantedAuthority("ALL")));
    SwitchUserGrantedAuthority switchAuth =
        new SwitchUserGrantedAuthority("F_PREVIOUS_IMPERSONATOR_AUTHORITY", original);
    UsernamePasswordAuthenticationToken auth =
        UsernamePasswordAuthenticationToken.authenticated(
            userDetails("alice", "F_USER"),
            "n/a",
            List.of(switchAuth, new SimpleGrantedAuthority("F_USER")));
    SecurityContext stored = storeContext(session, auth);

    SecurityContext loaded = repository.loadDeferredContext(request).get();

    assertSame(stored, loaded);
    verifyNoInteractions(authzService);
  }

  @Test
  void matchingEpochNeverCallsEffectiveGen() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SESSION_AUTHZ_EPOCH_ATTR, 7L);
    request.setSession(session);
    SecurityContext stored = storeUsernamePasswordContext(session, userDetails("alice", "F_USER"));

    when(authzService.currentEpoch()).thenReturn(7L);

    SecurityContext loaded = repository.loadDeferredContext(request).get();

    assertSame(stored, loaded);
    verify(authzService).currentEpoch();
    verify(authzService, never()).effectiveGen(any());
    verify(authzService, never()).getFreshUserDetails(any());
  }

  @Test
  void epochDiffersGenMatchesUpdatesAttrsWithoutRebuild() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SESSION_AUTHZ_EPOCH_ATTR, 1L);
    session.setAttribute(SESSION_AUTHZ_GEN_ATTR, 5L);
    request.setSession(session);
    UserDetails current = userDetails("alice", "F_USER");
    SecurityContext stored = storeUsernamePasswordContext(session, current);

    when(authzService.currentEpoch()).thenReturn(2L);
    when(authzService.effectiveGen(current)).thenReturn(5L);

    SecurityContext loaded = repository.loadDeferredContext(request).get();

    assertSame(stored, loaded);
    verify(authzService, never()).getFreshUserDetails(any());
    assertEquals(5L, session.getAttribute(SESSION_AUTHZ_GEN_ATTR));
    assertEquals(2L, session.getAttribute(SESSION_AUTHZ_EPOCH_ATTR));
  }

  @Test
  void epochAndGenDifferRebuildAndPersistRefreshedContext() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SESSION_AUTHZ_EPOCH_ATTR, 1L);
    session.setAttribute(SESSION_AUTHZ_GEN_ATTR, 3L);
    request.setSession(session);
    UserDetails stale = userDetails("alice", "F_OLD");
    SecurityContext stored = storeUsernamePasswordContext(session, stale);
    UserDetails fresh = userDetails("alice", "F_NEW");

    when(authzService.currentEpoch()).thenReturn(9L);
    when(authzService.effectiveGen(stale)).thenReturn(8L);
    when(authzService.getFreshUserDetails("alice")).thenReturn(fresh);

    SecurityContext loaded = repository.loadDeferredContext(request).get();

    assertNotSame(stored, loaded);
    Authentication after = loaded.getAuthentication();
    assertInstanceOf(UsernamePasswordAuthenticationToken.class, after);
    assertSame(fresh, after.getPrincipal());
    assertTrue(after.getAuthorities().stream().anyMatch(a -> "F_NEW".equals(a.getAuthority())));
    // The refresh persists itself: subsequent requests must load the refreshed context.
    assertSame(loaded, session.getAttribute(SPRING_SECURITY_CONTEXT_KEY));
    assertEquals(8L, session.getAttribute(SESSION_AUTHZ_GEN_ATTR));
    assertEquals(9L, session.getAttribute(SESSION_AUTHZ_EPOCH_ATTR));
  }

  @Test
  void oidcTokenRebuildsDhisOidcUserPrincipal() {
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
    storeContext(session, oauth2);

    when(authzService.currentEpoch()).thenReturn(4L);
    when(authzService.effectiveGen(stale)).thenReturn(2L);
    when(authzService.getFreshUserDetails("oidc-user")).thenReturn(fresh);

    SecurityContext loaded = repository.loadDeferredContext(request).get();

    Authentication after = loaded.getAuthentication();
    assertInstanceOf(OAuth2AuthenticationToken.class, after);
    OAuth2AuthenticationToken replaced = (OAuth2AuthenticationToken) after;
    assertEquals("google", replaced.getAuthorizedClientRegistrationId());
    assertInstanceOf(DhisOidcUser.class, replaced.getPrincipal());
    DhisOidcUser newPrincipal = (DhisOidcUser) replaced.getPrincipal();
    assertSame(fresh, newPrincipal.getUser());
    assertNotSame(oidcUser, newPrincipal);
    assertSame(loaded, session.getAttribute(SPRING_SECURITY_CONTEXT_KEY));
    assertEquals(2L, session.getAttribute(SESSION_AUTHZ_GEN_ATTR));
    assertEquals(4L, session.getAttribute(SESSION_AUTHZ_EPOCH_ATTR));
  }

  @Test
  void repeatedAccessIsCachedAndChecksOnce() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SESSION_AUTHZ_EPOCH_ATTR, 1L);
    request.setSession(session);
    UserDetails stale = userDetails("alice", "F_OLD");
    storeUsernamePasswordContext(session, stale);
    UserDetails fresh = userDetails("alice", "F_NEW");

    when(authzService.currentEpoch()).thenReturn(2L);
    when(authzService.effectiveGen(stale)).thenReturn(1L);
    when(authzService.getFreshUserDetails("alice")).thenReturn(fresh);

    DeferredSecurityContext deferred = repository.loadDeferredContext(request);
    SecurityContext first = deferred.get();
    SecurityContext second = deferred.get();

    assertSame(first, second);
    verify(authzService, times(1)).currentEpoch();
    verify(authzService, times(1)).getFreshUserDetails("alice");
  }

  private static SecurityContext storeUsernamePasswordContext(
      MockHttpSession session, UserDetails principal) {
    return storeContext(
        session,
        UsernamePasswordAuthenticationToken.authenticated(
            principal, "n/a", principal.getAuthorities()));
  }

  private static SecurityContext storeContext(MockHttpSession session, Authentication auth) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);
    return context;
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
