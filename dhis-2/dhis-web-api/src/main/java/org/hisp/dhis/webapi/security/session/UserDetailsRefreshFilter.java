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
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.security.DhisAuthenticationToken;
import org.hisp.dhis.security.DhisAuthenticationToken.LoginMethod;
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
 * <p>As a backstop for changes that are not recorded, the principal is also rebuilt when it is
 * older than the refresh interval ({@code system.session.user_details_refresh_interval}). The time
 * of the last rebuild is kept in the {@link #REFRESHED_AT_ATTRIBUTE} attribute; before the first
 * rebuild the creation time of the session is used.
 *
 * <p>Only authentications kept in the session are refreshed: form, HTTP basic and OIDC logins.
 * Personal access tokens and JWT bearer tokens authenticate every request on their own.
 *
 * <p>The session is ended instead of refreshed when:
 *
 * <ul>
 *   <li>the session impersonates another user and that user changed, since a refresh could give the
 *       impersonating user authorities it was not allowed to impersonate
 *   <li>the user no longer exists, is disabled or expired, or a local-password login must set up
 *       two-factor authentication before logging in
 * </ul>
 *
 * <p>Refresh resolves the original account by UID and writes only to the original session. A
 * concurrent logout must never create an authenticated replacement session.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@RequiredArgsConstructor
public class UserDetailsRefreshFilter extends OncePerRequestFilter {

  /** Session attribute with the change token the session principal was last refreshed for. */
  public static final String CHANGE_TOKEN_ATTRIBUTE = "DHIS2_USER_DETAILS_CHANGE_TOKEN";

  /** Session attribute with the time in milliseconds the session principal was last rebuilt. */
  public static final String REFRESHED_AT_ATTRIBUTE = "DHIS2_USER_DETAILS_REFRESHED_AT";

  private final UserDetailsChangeTracker userDetailsChangeTracker;

  private final UserService userService;

  /** Interval after which the principal is rebuilt in any case, zero or negative to never. */
  private final Duration refreshInterval;

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull FilterChain filterChain)
      throws ServletException, IOException {
    HttpSession session = request.getSession(false);
    if (session != null) {
      refreshIfStale(session);
    }
    filterChain.doFilter(request, response);
  }

  private void refreshIfStale(HttpSession session) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    UserDetails principal = getSessionPrincipal(authentication);
    if (principal == null) {
      return;
    }
    // The token must be read before the principal is rebuilt. Tokens are written after the
    // change has committed, so the rebuilt principal includes every change up to this token. A
    // change committed while rebuilding writes a new token, which the next request picks up.
    String token = userDetailsChangeTracker.getChangeToken(principal.getUsername());
    boolean changed;
    try {
      changed = token != null && !token.equals(session.getAttribute(CHANGE_TOKEN_ATTRIBUTE));
      if (!changed && !isRefreshDue(session)) {
        return;
      }
    } catch (IllegalStateException ex) {
      // The session was invalidated by a concurrent logout.
      SecurityContextHolder.clearContext();
      return;
    }

    if (isImpersonation(authentication)) {
      // only an actual change of the impersonated user ends the session, not the timer
      if (changed) {
        endSession(session, principal);
      }
      return;
    }

    UserDetails user = userService.createUserDetailsSafe(principal.getUid());
    if (user == null || !canStayLoggedIn(authentication, user)) {
      endSession(session, principal);
      return;
    }

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(createAuthentication(authentication, user));
    try {
      session.setAttribute(CHANGE_TOKEN_ATTRIBUTE, token);
      session.setAttribute(REFRESHED_AT_ATTRIBUTE, System.currentTimeMillis());
      session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);
    } catch (IllegalStateException ex) {
      // Never save through request.getSession(): it could create or return another session.
      SecurityContextHolder.clearContext();
      return;
    }
    SecurityContextHolder.setContext(context);
    log.debug("Refreshed session principal of user '{}'", user.getUsername());
  }

  private boolean isRefreshDue(HttpSession session) {
    if (refreshInterval.isZero() || refreshInterval.isNegative()) {
      return false;
    }
    Long refreshedAt = (Long) session.getAttribute(REFRESHED_AT_ATTRIBUTE);
    long builtAt = refreshedAt != null ? refreshedAt : session.getCreationTime();
    return System.currentTimeMillis() - builtAt >= refreshInterval.toMillis();
  }

  private static void endSession(HttpSession session, UserDetails principal) {
    log.info("Ending session of changed user '{}'", principal.getUsername());
    SecurityContextHolder.clearContext();
    try {
      session.invalidate();
    } catch (IllegalStateException ex) {
      // A concurrent logout already invalidated this session.
    }
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

  private static boolean canStayLoggedIn(Authentication authentication, UserDetails user) {
    boolean mustSetUpTwoFactor =
        authentication instanceof DhisAuthenticationToken login
            && login.getLoginMethod() == LoginMethod.LOCAL
            && !user.isTwoFactorEnabled()
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
    } else if (current instanceof DhisAuthenticationToken login) {
      authentication =
          new DhisAuthenticationToken(
              user, current.getCredentials(), authorities, login.getLoginMethod());
    } else {
      authentication =
          UsernamePasswordAuthenticationToken.authenticated(
              user, current.getCredentials(), authorities);
    }
    authentication.setDetails(current.getDetails());
    return authentication;
  }
}
