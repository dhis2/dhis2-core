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
package org.hisp.dhis.webapi.mvc.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.web.method.HandlerMethod;

/**
 * Characterization tests for {@link AuthorityInterceptor}. These tests pin the CURRENT contract of
 * {@code @RequiresAuthority} enforcement. They are deliberate behavior pins, not endorsements:
 * changing any of the pinned behaviors (especially the authority source, see {@link
 * #authoritiesAreReadFromTokenNotFromPrincipal()}) must be an explicit, reviewed decision.
 *
 * <p>Pinned contract:
 *
 * <ul>
 *   <li>Authorities are read from {@link Authentication#getAuthorities()} (the token), NEVER from
 *       the principal's own authority set. Impersonation relies on this: the {@code
 *       SwitchUserGrantedAuthority} granting {@code F_PREVIOUS_IMPERSONATOR_AUTHORITY} exists only
 *       on the token, so moving the check to the principal silently locks impersonated users out of
 *       {@code /api/auth/impersonateExit}.
 *   <li>{@link Authorities#ALL} is implicitly accepted for any required authority.
 *   <li>A method-level annotation wins over a class-level annotation.
 *   <li>The denial message format is a contract used by clients and e2e tests.
 * </ul>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class AuthorityInterceptorTest {

  private final AuthorityInterceptor interceptor = new AuthorityInterceptor();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  // ---------------------------------------------------------------------
  // Fixture handlers
  // ---------------------------------------------------------------------

  @RequiresAuthority(anyOf = Authorities.F_SYSTEM_SETTING)
  public static class FixtureController {
    @RequiresAuthority(anyOf = Authorities.F_PERFORM_MAINTENANCE)
    public void maintenance() {
      // no-op: only the @RequiresAuthority annotations matter to the interceptor
    }

    public void classLevelOnly() {
      // no-op: covered by the class-level @RequiresAuthority
    }

    @RequiresAuthority(anyOf = Authorities.F_PREVIOUS_IMPERSONATOR_AUTHORITY)
    public void impersonationExit() {
      // no-op: only the @RequiresAuthority annotation matters to the interceptor
    }
  }

  public static class PlainController {
    public void open() {
      // no-op: unannotated handler, must pass through the interceptor
    }
  }

  private static HandlerMethod handler(Class<?> type, String methodName) {
    try {
      return new HandlerMethod(type.getDeclaredConstructor().newInstance(), methodName);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private boolean preHandle(HandlerMethod handlerMethod) {
    return interceptor.preHandle(
        new MockHttpServletRequest(), new MockHttpServletResponse(), handlerMethod);
  }

  private static void setAuthentication(Authentication authentication) {
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private static Authentication tokenWithAuthorities(Object principal, GrantedAuthority... auths) {
    return UsernamePasswordAuthenticationToken.authenticated(principal, "n/a", List.of(auths));
  }

  private static SimpleGrantedAuthority granted(Authorities authority) {
    return new SimpleGrantedAuthority(authority.toString());
  }

  // ---------------------------------------------------------------------
  // Granted
  // ---------------------------------------------------------------------

  @ParameterizedTest(name = "{0}")
  @MethodSource("grantedAuthentications")
  @DisplayName("Access is granted when the token carries a required authority or ALL")
  void accessGrantedFromTokenAuthorities(String description, Authentication authentication) {
    setAuthentication(authentication);
    assertTrue(preHandle(handler(FixtureController.class, "maintenance")));
  }

  private static Stream<Arguments> grantedAuthentications() {
    UserDetails patUser =
        UserDetails.empty()
            .username("patuser")
            .authorities(List.of(granted(Authorities.F_PERFORM_MAINTENANCE)))
            .allAuthorities(Set.of(Authorities.F_PERFORM_MAINTENANCE.toString()))
            .build();
    return Stream.of(
        Arguments.of(
            "username+password token with required authority",
            tokenWithAuthorities("user", granted(Authorities.F_PERFORM_MAINTENANCE))),
        Arguments.of(
            "ALL authority implicitly grants any required authority",
            tokenWithAuthorities("user", granted(Authorities.ALL))),
        Arguments.of(
            "personal access token with required authority",
            new ApiTokenAuthenticationToken(null, patUser)));
  }

  // ---------------------------------------------------------------------
  // Denied + message contract
  // ---------------------------------------------------------------------

  @Test
  @DisplayName("Denial message is a pinned contract and names the required authorities")
  void deniedMessageNamesRequiredAuthorities() {
    setAuthentication(tokenWithAuthorities("user"));
    HandlerMethod maintenance = handler(FixtureController.class, "maintenance");
    AccessDeniedException ex =
        assertThrows(AccessDeniedException.class, () -> preHandle(maintenance));
    assertEquals(
        "Access is denied, requires one Authority from [F_PERFORM_MAINTENANCE]", ex.getMessage());
  }

  @Test
  @DisplayName("Method-level @RequiresAuthority wins over the class-level annotation")
  void methodAnnotationWinsOverClassAnnotation() {
    // user holds the CLASS-level authority but not the METHOD-level one
    setAuthentication(tokenWithAuthorities("user", granted(Authorities.F_SYSTEM_SETTING)));
    HandlerMethod maintenance = handler(FixtureController.class, "maintenance");
    AccessDeniedException ex =
        assertThrows(AccessDeniedException.class, () -> preHandle(maintenance));
    assertEquals(
        "Access is denied, requires one Authority from [F_PERFORM_MAINTENANCE]", ex.getMessage());
  }

  @Test
  @DisplayName("Class-level @RequiresAuthority applies to methods without their own annotation")
  void classAnnotationAppliesWhenMethodHasNone() {
    setAuthentication(tokenWithAuthorities("user", granted(Authorities.F_SYSTEM_SETTING)));
    HandlerMethod classLevelOnly = handler(FixtureController.class, "classLevelOnly");
    assertTrue(preHandle(classLevelOnly));

    setAuthentication(tokenWithAuthorities("user", granted(Authorities.F_PERFORM_MAINTENANCE)));
    AccessDeniedException ex =
        assertThrows(AccessDeniedException.class, () -> preHandle(classLevelOnly));
    assertEquals(
        "Access is denied, requires one Authority from [F_SYSTEM_SETTING]", ex.getMessage());
  }

  // ---------------------------------------------------------------------
  // Authority source: token, not principal
  // ---------------------------------------------------------------------

  @Test
  @DisplayName(
      "PIN: authorities are read from the token; SwitchUserGrantedAuthority grants impersonation"
          + " exit even though the principal never holds F_PREVIOUS_IMPERSONATOR_AUTHORITY")
  void authoritiesAreReadFromTokenNotFromPrincipal() {
    UserDetails impersonated = UserDetails.empty().username("impersonated").build();
    Authentication impersonator =
        tokenWithAuthorities("impersonator", granted(Authorities.F_IMPERSONATE_USER));
    SwitchUserGrantedAuthority switchAuthority =
        new SwitchUserGrantedAuthority(
            Authorities.F_PREVIOUS_IMPERSONATOR_AUTHORITY.name(), impersonator);

    setAuthentication(tokenWithAuthorities(impersonated, switchAuthority));

    assertTrue(preHandle(handler(FixtureController.class, "impersonationExit")));
  }

  @Test
  @DisplayName("PIN: an authority present only on the principal (not the token) does NOT grant")
  void principalAuthoritiesAloneDoNotGrant() {
    UserDetails principal =
        UserDetails.empty()
            .username("user")
            .allAuthorities(Set.of(Authorities.F_PERFORM_MAINTENANCE.toString()))
            .build();
    // token carries no authorities at all
    setAuthentication(tokenWithAuthorities(principal));

    HandlerMethod maintenance = handler(FixtureController.class, "maintenance");
    assertThrows(AccessDeniedException.class, () -> preHandle(maintenance));
  }

  // ---------------------------------------------------------------------
  // Edge authentications
  // ---------------------------------------------------------------------

  @Test
  @DisplayName("Anonymous authentication is denied on protected handlers")
  void anonymousAuthenticationIsDenied() {
    setAuthentication(
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    HandlerMethod maintenance = handler(FixtureController.class, "maintenance");
    assertThrows(AccessDeniedException.class, () -> preHandle(maintenance));
  }

  @Test
  @DisplayName("PIN: missing authentication fails with SessionAuthenticationException")
  void missingAuthenticationThrowsSessionAuthenticationException() {
    SecurityContextHolder.clearContext();
    HandlerMethod maintenance = handler(FixtureController.class, "maintenance");
    SessionAuthenticationException ex =
        assertThrows(SessionAuthenticationException.class, () -> preHandle(maintenance));
    assertEquals("Error trying to get user authentication details", ex.getMessage());
  }

  @Test
  @DisplayName("Handlers without any @RequiresAuthority are not intercepted")
  void unannotatedHandlerIsNotChecked() {
    SecurityContextHolder.clearContext();
    assertTrue(preHandle(handler(PlainController.class, "open")));
  }

  @Test
  @DisplayName("Non-HandlerMethod handlers pass through untouched")
  void nonHandlerMethodPassesThrough() {
    SecurityContextHolder.clearContext();
    assertTrue(
        interceptor.preHandle(
            new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()));
  }
}
