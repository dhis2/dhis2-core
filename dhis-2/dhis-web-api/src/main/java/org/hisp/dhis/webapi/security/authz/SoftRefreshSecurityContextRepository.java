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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.authz.AuthzService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Decorates the session-backed {@link SecurityContextRepository} to soft-refresh the {@link
 * UserDetails} principal when the authz generation/epoch has advanced since the last check.
 *
 * <p>Decorating the repository (instead of adding a filter) makes the refresh session-only <em>by
 * construction</em>: JWT, PAT, and other stateless authentications never pass through {@link
 * HttpSessionSecurityContextRepository}, so no token-type conversion or accidental session creation
 * is possible. Wrapping the {@link DeferredSecurityContext} means the check only runs when the
 * request actually accesses authentication.
 *
 * <p>Eligibility gates:
 *
 * <ul>
 *   <li>Token type: only {@link UsernamePasswordAuthenticationToken} and {@link
 *       OAuth2AuthenticationToken} carry a session principal we can rebuild.
 *   <li>No impersonation: {@link SwitchUserGrantedAuthority} carries the original Authentication; a
 *       rebuild would drop it.
 * </ul>
 *
 * @author Morten Svanæs
 */
@Slf4j
public class SoftRefreshSecurityContextRepository implements SecurityContextRepository {

  private final SecurityContextRepository delegate;
  private final AuthzService authzService;

  public SoftRefreshSecurityContextRepository(
      SecurityContextRepository delegate, AuthzService authzService) {
    this.delegate = delegate;
    this.authzService = authzService;
  }

  @Override
  public DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
    return new SoftRefreshDeferredContext(delegate.loadDeferredContext(request), request, this);
  }

  @Override
  @SuppressWarnings("deprecation")
  public SecurityContext loadContext(
      org.springframework.security.web.context.HttpRequestResponseHolder requestResponseHolder) {
    return delegate.loadContext(requestResponseHolder);
  }

  @Override
  public void saveContext(
      SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
    delegate.saveContext(context, request, response);
  }

  @Override
  public boolean containsContext(HttpServletRequest request) {
    return delegate.containsContext(request);
  }

  /** Defers the freshness check until the request actually accesses its authentication. */
  private static final class SoftRefreshDeferredContext implements DeferredSecurityContext {

    private final DeferredSecurityContext delegate;
    private final HttpServletRequest request;
    private final SoftRefreshSecurityContextRepository repository;

    private SecurityContext result;

    private SoftRefreshDeferredContext(
        DeferredSecurityContext delegate,
        HttpServletRequest request,
        SoftRefreshSecurityContextRepository repository) {
      this.delegate = delegate;
      this.request = request;
      this.repository = repository;
    }

    @Override
    public SecurityContext get() {
      if (result == null) {
        result = repository.maybeRefresh(delegate.get(), request);
      }
      return result;
    }

    @Override
    public boolean isGenerated() {
      return delegate.isGenerated();
    }
  }

  /**
   * Returns {@code context} unchanged when fresh (or not eligible), otherwise a new context with a
   * rebuilt principal, persisted to the session.
   */
  private SecurityContext maybeRefresh(SecurityContext context, HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null || context == null) {
      return context;
    }
    Authentication auth = context.getAuthentication();
    if (!isEligible(auth)) {
      return context;
    }

    long epoch = authzService.currentEpoch();
    long checkedEpoch = readLongAttr(session, SESSION_AUTHZ_EPOCH_ATTR);
    if (epoch == checkedEpoch) {
      return context;
    }
    UserDetails current = extractUserDetails(auth);
    if (current == null) {
      return context;
    }

    SecurityContext result = context;
    long gen = authzService.effectiveGen(current); // pre-read BEFORE rebuild, never re-read
    long sessionGen = readLongAttr(session, SESSION_AUTHZ_GEN_ATTR);
    if (gen != sessionGen) {
      UserDetails fresh = authzService.getFreshUserDetails(current.getUsername());
      if (fresh != null) {
        Authentication replacement = rebuildAuthentication(auth, fresh);
        if (replacement != null) {
          SecurityContext refreshed = SecurityContextHolder.createEmptyContext();
          refreshed.setAuthentication(replacement);
          // Equivalent of HttpSessionSecurityContextRepository.saveContextInHttpSession: under
          // requireExplicitSave, SecurityContextHolderFilter never saves, so the refresh
          // persists itself for subsequent requests.
          session.setAttribute(
              HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, refreshed);
          session.setAttribute(SESSION_AUTHZ_GEN_ATTR, gen); // pre-read value, NEVER re-read
          result = refreshed;
          log.debug(
              "Soft-refreshed session UserDetails for {} (gen {} -> {})",
              current.getUsername(),
              sessionGen,
              gen);
        }
      }
    } else {
      session.setAttribute(SESSION_AUTHZ_GEN_ATTR, gen);
    }
    session.setAttribute(SESSION_AUTHZ_EPOCH_ATTR, epoch);
    return result;
  }

  /**
   * Impersonation carries the original Authentication inside {@link SwitchUserGrantedAuthority}
   * which a rebuild would drop; only session-native token types are rebuildable.
   */
  private static boolean isEligible(@CheckForNull Authentication auth) {
    return auth != null
        && auth.isAuthenticated()
        && (auth instanceof UsernamePasswordAuthenticationToken
            || auth instanceof OAuth2AuthenticationToken)
        && auth.getAuthorities().stream().noneMatch(SwitchUserGrantedAuthority.class::isInstance);
  }

  @CheckForNull
  private static UserDetails extractUserDetails(@Nonnull Authentication auth) {
    Object principal = auth.getPrincipal();
    if (principal instanceof DhisOidcUser oidcUser) {
      org.springframework.security.core.userdetails.UserDetails nested = oidcUser.getUser();
      if (nested instanceof UserDetails dhisUserDetails) {
        return dhisUserDetails;
      }
      return null;
    }
    if (principal instanceof UserDetails userDetails) {
      return userDetails;
    }
    return null;
  }

  @CheckForNull
  private static Authentication rebuildAuthentication(
      @Nonnull Authentication existing, @Nonnull UserDetails fresh) {
    if (existing instanceof OAuth2AuthenticationToken oauth2
        && oauth2.getPrincipal() instanceof DhisOidcUser oidcUser) {
      Map<String, Object> attributes = oidcUser.getAttributes();
      if (attributes.isEmpty()) {
        return null;
      }
      DhisOidcUser newPrincipal =
          new DhisOidcUser(
              fresh, attributes, resolveNameAttributeKey(oidcUser), oidcUser.getIdToken());
      OAuth2AuthenticationToken token =
          new OAuth2AuthenticationToken(
              newPrincipal, fresh.getAuthorities(), oauth2.getAuthorizedClientRegistrationId());
      token.setDetails(existing.getDetails());
      return token;
    }
    if (existing instanceof UsernamePasswordAuthenticationToken
        && existing.getPrincipal() instanceof UserDetails) {
      UsernamePasswordAuthenticationToken token =
          UsernamePasswordAuthenticationToken.authenticated(
              fresh, existing.getCredentials(), fresh.getAuthorities());
      token.setDetails(existing.getDetails());
      return token;
    }
    return null;
  }

  /**
   * {@link org.springframework.security.oauth2.core.user.DefaultOAuth2User} requires the name
   * attribute key to exist in attributes. {@link DhisOidcUser} does not override {@code getName()},
   * so the value returned by {@code getName()} is the original name attribute's value and can be
   * used to recover the key.
   */
  @Nonnull
  static String resolveNameAttributeKey(@Nonnull DhisOidcUser oidcUser) {
    Map<String, Object> attributes = oidcUser.getAttributes();
    String name = null;
    try {
      name = oidcUser.getName();
    } catch (RuntimeException ignored) {
      // odd principal states; fall through to defaults
    }
    if (name != null) {
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        if (entry.getValue() != null && name.equals(String.valueOf(entry.getValue()))) {
          return entry.getKey();
        }
      }
    }
    if (attributes.containsKey("sub")) {
      return "sub";
    }
    return attributes.keySet().iterator().next();
  }

  private static long readLongAttr(@Nonnull HttpSession session, @Nonnull String name) {
    Object value = session.getAttribute(name);
    if (value instanceof Long longValue) {
      return longValue;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String stringValue) {
      try {
        return Long.parseLong(stringValue);
      } catch (NumberFormatException ignored) {
        return 0L;
      }
    }
    return 0L;
  }
}
