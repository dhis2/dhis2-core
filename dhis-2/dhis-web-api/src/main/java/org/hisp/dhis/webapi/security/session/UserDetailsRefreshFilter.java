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

import static org.hisp.dhis.security.twofa.TwoFactorAuthService.TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.hisp.dhis.user.UserService;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refreshes the {@link UserDetails} principal of a session in place when the user changed since the
 * principal was built, for example when the user got a new user role or joined a user group. The
 * session and its id stay the same.
 *
 * <p>Changes are recorded by the {@link UserDetailsChangeTracker} as a token per user. The session
 * keeps the token of the last change it has applied in the {@link #CHANGE_TOKEN_ATTRIBUTE}
 * attribute, and the principal is rebuilt from the database when the current token differs.
 *
 * <p>Only authentications kept in the session are refreshed: form, HTTP basic and OIDC logins.
 * Personal access tokens and JWT bearer tokens authenticate every request on their own.
 *
 * <p>The session is ended instead of refreshed when:
 *
 * <ul>
 *   <li>the session impersonates another user, since a refresh could give the impersonating user
 *       authorities it was not allowed to impersonate
 *   <li>the user no longer exists, is disabled or expired, or must set up two-factor authentication
 *       before logging in, the same checks the login makes
 * </ul>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@RequiredArgsConstructor
public class UserDetailsRefreshFilter extends OncePerRequestFilter {

  /** Session attribute with the change token the session principal was last refreshed for. */
  public static final String CHANGE_TOKEN_ATTRIBUTE = "DHIS2_USER_DETAILS_CHANGE_TOKEN";

  private final UserDetailsChangeTracker userDetailsChangeTracker;

  private final UserService userService;

  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull FilterChain filterChain)
      throws ServletException, IOException {
    HttpSession session = request.getSession(false);
    if (session != null) {
      refreshIfChanged(request, response, session);
    }
    filterChain.doFilter(request, response);
  }

  private void refreshIfChanged(
      HttpServletRequest request, HttpServletResponse response, HttpSession session) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    UserDetails principal = getSessionPrincipal(authentication);
    if (principal == null) {
      return;
    }
    // The token must be read before the principal is rebuilt. Tokens are written after the
    // change has committed, so the rebuilt principal includes every change up to this token. A
    // change committed while rebuilding writes a new token, which the next request picks up.
    String token = userDetailsChangeTracker.getChangeToken(principal.getUsername());
    if (token == null || token.equals(session.getAttribute(CHANGE_TOKEN_ATTRIBUTE))) {
      return;
    }

    UserDetails user =
        isImpersonation(authentication)
            ? null
            : userService.createUserDetailsByUsername(principal.getUsername());
    if (user == null || !canStayLoggedIn(user)) {
      log.info("Ending session of changed user '{}'", principal.getUsername());
      SecurityContextHolder.clearContext();
      session.invalidate();
      return;
    }

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(createAuthentication(authentication, user));
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);
    session.setAttribute(CHANGE_TOKEN_ATTRIBUTE, token);
    log.debug("Refreshed session principal of changed user '{}'", user.getUsername());
  }

  @CheckForNull
  private static UserDetails getSessionPrincipal(@CheckForNull Authentication authentication) {
    if (authentication instanceof UsernamePasswordAuthenticationToken
        && authentication.getPrincipal() instanceof UserDetails principal) {
      return principal;
    }
    if (authentication instanceof OAuth2AuthenticationToken
        && authentication.getPrincipal() instanceof DhisOidcUser principal) {
      return principal;
    }
    return null;
  }

  private static boolean isImpersonation(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .anyMatch(SwitchUserGrantedAuthority.class::isInstance);
  }

  private static boolean canStayLoggedIn(UserDetails user) {
    boolean mustSetUpTwoFactor =
        !user.isTwoFactorEnabled()
            && user.hasAnyRestrictions(Set.of(TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME));
    return user.isEnabled() && user.isAccountNonExpired() && !mustSetUpTwoFactor;
  }

  /**
   * Creates an authentication of the same type as the current one for the refreshed user. The
   * details and the factor authorities (how the user logged in) are kept.
   */
  private static Authentication createAuthentication(Authentication current, UserDetails user) {
    List<GrantedAuthority> authorities = new ArrayList<>(user.getAuthorities());
    current.getAuthorities().stream()
        .filter(FactorGrantedAuthority.class::isInstance)
        .forEach(authorities::add);

    AbstractAuthenticationToken authentication;
    if (current instanceof OAuth2AuthenticationToken oauth2
        && current.getPrincipal() instanceof DhisOidcUser oidcUser) {
      DhisOidcUser principal =
          new DhisOidcUser(
              user, oidcUser.getAttributes(), IdTokenClaimNames.SUB, oidcUser.getIdToken());
      authentication =
          new OAuth2AuthenticationToken(
              principal, authorities, oauth2.getAuthorizedClientRegistrationId());
    } else {
      authentication =
          UsernamePasswordAuthenticationToken.authenticated(
              user, current.getCredentials(), authorities);
    }
    authentication.setDetails(current.getDetails());
    return authentication;
  }
}
