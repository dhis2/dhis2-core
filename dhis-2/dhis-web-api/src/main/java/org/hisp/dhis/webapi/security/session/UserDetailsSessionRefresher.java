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

import static org.hisp.dhis.external.conf.ConfigurationKey.SYSTEM_SESSION_USER_DETAILS_REFRESH_INTERVAL;
import static org.hisp.dhis.security.Authorities.F_IMPERSONATE_USER;
import static org.hisp.dhis.security.twofa.TwoFactorAuthService.TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

import com.google.common.util.concurrent.Striped;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.DhisAuthenticationToken;
import org.hisp.dhis.security.DhisAuthenticationToken.LoginMethod;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.stereotype.Component;

/**
 * Keeps the {@link UserDetails} principal of a session up to date, and keeps its {@link
 * UserDetailsRefreshState} when the session starts or ends an impersonation.
 *
 * <p>The principal is refreshed in place when the user changed since it was built, for example when
 * the user got a new user role or joined a user group. The session and its id stay the same.
 * Changes are recorded by the {@link UserDetailsChangeTracker} as a token per user. The session
 * keeps the token of the last change it has applied, and the principal is rebuilt from the database
 * when the current token differs.
 *
 * <p>As a backstop for changes that are not recorded, the principal is also rebuilt when it is
 * older than the refresh interval ({@code system.session.user_details_refresh_interval}).
 *
 * <p>Only authentications kept in the session are refreshed: form, HTTP basic and OIDC logins.
 * Personal access tokens and JWT bearer tokens authenticate every request on their own.
 *
 * <p>The session is ended instead of refreshed when:
 *
 * <ul>
 *   <li>the session impersonates another user and that user changed, since a refresh could give the
 *       impersonating user authorities it was not allowed to impersonate
 *   <li>the session impersonates another user and the impersonating user changed after the
 *       impersonation started, or the refresh interval is due and the impersonating user no longer
 *       exists, is disabled or expired, or may no longer impersonate
 *   <li>the user no longer exists, is disabled or expired, or a local-password login must set up
 *       two-factor authentication before logging in
 * </ul>
 *
 * <p>The authentication of an impersonation session is never rebuilt.
 *
 * <p>Refresh resolves the original account by UID and writes only to the given session. A
 * concurrent logout must never create an authenticated replacement session.
 *
 * <p><b>Concurrency.</b> On one node the refresh of a session is serialized by a lock per session
 * id. A request that waited reads the session again: when another request has already applied the
 * change, it continues with the authentication that request stored and does not rebuild. With Redis
 * sessions every request works on its own copy of the session and writes its changes at the end of
 * the request, so a request does not see what a parallel request wrote. Each request then keeps its
 * authentication and refresh state consistent with each other, and the last writer wins.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
public class UserDetailsSessionRefresher {

  /** Number of locks the session ids are spread over. */
  private static final int LOCK_STRIPES = 1024;

  private final UserDetailsChangeTracker userDetailsChangeTracker;

  private final UserService userService;

  /** Interval after which the principal is rebuilt in any case, zero or negative to never. */
  private final Duration refreshInterval;

  private final Striped<Lock> sessionLocks = Striped.lock(LOCK_STRIPES);

  @Autowired
  public UserDetailsSessionRefresher(
      UserDetailsChangeTracker userDetailsChangeTracker,
      UserService userService,
      DhisConfigurationProvider config) {
    this(
        userDetailsChangeTracker,
        userService,
        Duration.ofSeconds(config.getIntProperty(SYSTEM_SESSION_USER_DETAILS_REFRESH_INTERVAL)));
  }

  public UserDetailsSessionRefresher(
      UserDetailsChangeTracker userDetailsChangeTracker,
      UserService userService,
      Duration refreshInterval) {
    this.userDetailsChangeTracker = userDetailsChangeTracker;
    this.userService = userService;
    this.refreshInterval = refreshInterval;
  }

  /**
   * Refreshes or ends the authentication of the current request when the principal of its session
   * is stale. Updates the {@link SecurityContextHolder} accordingly.
   *
   * @param session the session of the current request, from {@code request.getSession(false)}
   */
  public void refresh(@Nonnull HttpSession session) {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      UserDetails principal = getSessionPrincipal(authentication);
      if (principal == null) {
        return;
      }
      // The state is read before the stored context, the reverse of the order a refresh writes
      // them in: a request that sees the state of another refresh then also sees its context.
      if (check(session, authentication, principal).action() == Action.NONE
          && !isReplacedInSession(session, authentication)) {
        return;
      }
      withSessionLock(session, () -> refreshLocked(session));
    } catch (IllegalStateException ex) {
      if (!isInvalidated(session)) {
        throw ex;
      }
      // A concurrent logout invalidated the session. Never save through request.getSession():
      // it could create or return another session.
      SecurityContextHolder.clearContext();
    }
  }

  /**
   * Starts an impersonation in the session of the request. The change token of the target is read
   * before {@code switchToTarget} loads the target, so that a change committed while loading is not
   * acknowledged unseen. The refresh state of the impersonating user is kept for the exit.
   *
   * <p>The refresh state of the impersonating user comes from the session when the request was
   * authenticated by it: the refresh filter has checked that authentication against the state. When
   * the request authenticated itself, with HTTP basic or a personal access token, the session holds
   * no state for it, and its principal was built from the current user details while authenticating
   * this request. Its change token is then read here, before the target is loaded. A change
   * committed between building that principal and reading the token is acknowledged unseen; the
   * refresh interval revalidates the impersonating user in any case.
   *
   * @param request the impersonation request
   * @param targetUsername the username of the user to impersonate
   * @param switchToTarget loads the target and saves its authentication in the session
   */
  public void startImpersonation(
      @Nonnull HttpServletRequest request,
      @Nonnull String targetUsername,
      @Nonnull Runnable switchToTarget) {
    HttpSession session = request.getSession(false);
    withSessionLock(
        session,
        () -> {
          Authentication impersonator = SecurityContextHolder.getContext().getAuthentication();
          // read before switchToTarget loads the target, see UserDetailsChangeTracker
          String targetToken = userDetailsChangeTracker.getChangeToken(targetUsername);
          long now = System.currentTimeMillis();
          UserDetailsRefreshState current =
              isCheckedSessionAuthentication(session, impersonator)
                  ? UserDetailsRefreshState.of(session)
                  : new UserDetailsRefreshState(
                      userDetailsChangeTracker.getChangeToken(getUsername(impersonator)), now);
          switchToTarget.run();
          current.impersonating(targetToken, now).saveTo(request.getSession(false));
        });
  }

  /**
   * Ends the impersonation in the session of the request and restores the refresh state the
   * impersonating user had when the impersonation started.
   *
   * @param request the exit request
   * @param switchToImpersonator saves the authentication of the impersonating user in the session
   */
  public void endImpersonation(
      @Nonnull HttpServletRequest request, @Nonnull Runnable switchToImpersonator) {
    withSessionLock(
        request.getSession(false),
        () -> {
          switchToImpersonator.run();
          HttpSession session = request.getSession(false);
          UserDetailsRefreshState.of(session).afterImpersonation().saveTo(session);
        });
  }

  private void refreshLocked(HttpSession session) {
    Authentication authentication = adoptSessionAuthentication(session);
    UserDetails principal = getSessionPrincipal(authentication);
    if (principal == null) {
      return;
    }
    Check check = check(session, authentication, principal);
    switch (check.action()) {
      case NONE -> {
        // another request already applied the change
      }
      case END_SESSION -> endSession(session, check.user());
      case CHECK_IMPERSONATOR -> checkImpersonator(session, check);
      case REBUILD -> rebuild(session, authentication, check);
    }
  }

  /**
   * Decides what a request has to do with the authentication of its session. Reads only.
   *
   * <p>The token must be read before the principal is rebuilt. Tokens are written after the change
   * has committed, so the rebuilt principal includes every change up to this token. A change
   * committed while rebuilding writes a new token, which the next request picks up.
   */
  private Check check(HttpSession session, Authentication authentication, UserDetails principal) {
    String token = userDetailsChangeTracker.getChangeToken(principal.getUsername());
    UserDetailsRefreshState state = UserDetailsRefreshState.of(session);
    boolean refreshDue = state.isRefreshDue(refreshInterval, System.currentTimeMillis());

    SwitchUserGrantedAuthority impersonation = getImpersonation(authentication);
    if (impersonation == null) {
      Action action = state.isChangedBy(token) || refreshDue ? Action.REBUILD : Action.NONE;
      return new Check(action, principal, token, state);
    }
    if (state.isChangedBy(token)) {
      return new Check(Action.END_SESSION, principal, token, state);
    }
    Authentication source = impersonation.getSource();
    if (source == null || !(source.getPrincipal() instanceof UserDetails impersonator)) {
      return new Check(Action.END_SESSION, principal, token, state);
    }
    String impersonatorToken = userDetailsChangeTracker.getChangeToken(impersonator.getUsername());
    if (state.isImpersonatorChangedBy(impersonatorToken)) {
      return new Check(Action.END_SESSION, impersonator, token, state);
    }
    Action action = refreshDue ? Action.CHECK_IMPERSONATOR : Action.NONE;
    return new Check(action, impersonator, token, state);
  }

  private void rebuild(HttpSession session, Authentication authentication, Check check) {
    UserDetails user = userService.createUserDetailsSafe(check.user().getUid());
    if (user == null || !canStayLoggedIn(authentication, user)) {
      endSession(session, check.user());
      return;
    }
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(createAuthentication(authentication, user));
    // The context first: a request that sees the new state then also finds the new context.
    session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);
    check.state().refreshed(check.token(), System.currentTimeMillis()).saveTo(session);
    SecurityContextHolder.setContext(context);
    log.debug("Refreshed session principal of user '{}'", user.getUsername());
  }

  /**
   * Revalidates the impersonating user when the refresh interval is due. The interval alone never
   * ends an impersonation.
   */
  private void checkImpersonator(HttpSession session, Check check) {
    UserDetails impersonator = userService.createUserDetailsSafe(check.user().getUid());
    if (impersonator == null
        || !impersonator.isEnabled()
        || !impersonator.isAccountNonExpired()
        || !impersonator.isAuthorized(F_IMPERSONATE_USER)) {
      endSession(session, check.user());
      return;
    }
    check.state().checked(System.currentTimeMillis()).saveTo(session);
  }

  /**
   * In-memory sessions are shared by the requests of a session, so another request may have
   * replaced the authentication of the session while this one waited for the lock. The request then
   * continues with that authentication.
   */
  private static Authentication adoptSessionAuthentication(HttpSession session) {
    Authentication current = SecurityContextHolder.getContext().getAuthentication();
    if (session.getAttribute(SPRING_SECURITY_CONTEXT_KEY) instanceof SecurityContext stored
        && stored.getAuthentication() != current) {
      SecurityContextHolder.setContext(stored);
      return stored.getAuthentication();
    }
    return current;
  }

  private static boolean isReplacedInSession(HttpSession session, Authentication authentication) {
    return session.getAttribute(SPRING_SECURITY_CONTEXT_KEY) instanceof SecurityContext stored
        && stored.getAuthentication() != authentication;
  }

  /**
   * Returns true when the authentication of the request is the one its session held when the
   * request started, so that the refresh filter has checked it against the refresh state of the
   * session. A session created by this request was not checked, nor is an authentication that this
   * request replaced, for example with HTTP basic or a personal access token.
   */
  private static boolean isCheckedSessionAuthentication(
      @CheckForNull HttpSession session, @CheckForNull Authentication authentication) {
    return session != null
        && !session.isNew()
        && getSessionPrincipal(authentication) != null
        && session.getAttribute(SPRING_SECURITY_CONTEXT_KEY) instanceof SecurityContext stored
        && stored.getAuthentication() == authentication;
  }

  @Nonnull
  private static String getUsername(@Nonnull Authentication authentication) {
    return authentication.getPrincipal() instanceof UserDetails user
        ? user.getUsername()
        : authentication.getName();
  }

  private void withSessionLock(@CheckForNull HttpSession session, Runnable action) {
    if (session == null) {
      action.run();
      return;
    }
    Lock lock = sessionLocks.get(session.getId());
    lock.lock();
    try {
      action.run();
    } finally {
      lock.unlock();
    }
  }

  private static boolean isInvalidated(HttpSession session) {
    try {
      session.getCreationTime();
      return false;
    } catch (IllegalStateException ex) {
      return true;
    }
  }

  private static void endSession(HttpSession session, UserDetails user) {
    log.info("Ending session of changed user '{}'", user.getUsername());
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

  @CheckForNull
  private static SwitchUserGrantedAuthority getImpersonation(Authentication authentication) {
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if (authority instanceof SwitchUserGrantedAuthority impersonation) {
        return impersonation;
      }
    }
    return null;
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

  private enum Action {
    /** The authentication is current. */
    NONE,
    /** The principal is rebuilt from the database. */
    REBUILD,
    /** The session is ended. */
    END_SESSION,
    /** The impersonating user is revalidated, the impersonation is kept when it may continue. */
    CHECK_IMPERSONATOR
  }

  /**
   * The outcome of a check.
   *
   * @param action what to do
   * @param user the user to rebuild, to end the session for, or to revalidate
   * @param token the change token of the session principal, read before any rebuild
   * @param state the refresh state of the session the decision is based on
   */
  private record Check(
      Action action, UserDetails user, @CheckForNull String token, UserDetailsRefreshState state) {}
}
