/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 */
package org.hisp.dhis.webapi.security.authz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.util.Set;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.authz.AuthzConstants;
import org.hisp.dhis.user.authz.AuthzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailsSoftRefreshFilterTest {

  @Mock private AuthzService authzService;
  @Mock private SecurityContextRepository securityContextRepository;
  @Mock private FilterChain filterChain;

  private UserDetailsSoftRefreshFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    filter = new UserDetailsSoftRefreshFilter(authzService, securityContextRepository);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    session = new MockHttpSession();
    request.setSession(session);
    SecurityContextHolder.clearContext();
  }

  @Test
  void skipsWhenGenMatches() throws Exception {
    UserDetails principal =
        UserDetails.empty().username("alice").userRoleIds(Set.of("r1")).build();
    var auth =
        new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
    session.setAttribute(AuthzConstants.SESSION_AUTHZ_GEN_ATTR, 3L);
    when(authzService.effectiveGen(principal)).thenReturn(3L);

    filter.doFilter(request, response, filterChain);

    verify(authzService, never()).loadFreshUserDetails(any());
    verify(securityContextRepository, never()).saveContext(any(), any(), any());
    verify(filterChain).doFilter(request, response);
    assertSame(principal, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
  }

  @Test
  void rebuildsAndSavesWhenGenDiffers() throws Exception {
    UserDetails stale =
        UserDetails.empty()
            .username("alice")
            .userRoleIds(Set.of("r1"))
            .allAuthorities(Set.of("F_OLD"))
            .build();
    UserDetails fresh =
        UserDetails.empty()
            .username("alice")
            .userRoleIds(Set.of("r1"))
            .allAuthorities(Set.of("F_NEW"))
            .build();
    var auth = new UsernamePasswordAuthenticationToken(stale, "n/a", stale.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
    session.setAttribute(AuthzConstants.SESSION_AUTHZ_GEN_ATTR, 0L);
    when(authzService.effectiveGen(stale)).thenReturn(1L);
    when(authzService.loadFreshUserDetails("alice")).thenReturn(fresh);
    when(authzService.effectiveGen(fresh)).thenReturn(1L);

    filter.doFilter(request, response, filterChain);

    Object newPrincipal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertInstanceOf(UserDetails.class, newPrincipal);
    assertNotSame(stale, newPrincipal);
    assertEquals(Set.of("F_NEW"), ((UserDetails) newPrincipal).getAllAuthorities());
    assertEquals(1L, session.getAttribute(AuthzConstants.SESSION_AUTHZ_GEN_ATTR));
    verify(securityContextRepository).saveContext(any(), eq(request), eq(response));
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void missingSessionGenTreatedAsZero() throws Exception {
    UserDetails stale =
        UserDetails.empty().username("alice").userRoleIds(Set.of("r1")).build();
    UserDetails fresh =
        UserDetails.empty().username("alice").userRoleIds(Set.of("r1")).build();
    var auth = new UsernamePasswordAuthenticationToken(stale, "n/a", stale.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
    when(authzService.effectiveGen(stale)).thenReturn(2L);
    when(authzService.loadFreshUserDetails("alice")).thenReturn(fresh);
    when(authzService.effectiveGen(fresh)).thenReturn(2L);

    filter.doFilter(request, response, filterChain);

    assertEquals(2L, session.getAttribute(AuthzConstants.SESSION_AUTHZ_GEN_ATTR));
    verify(authzService).loadFreshUserDetails("alice");
  }
}
