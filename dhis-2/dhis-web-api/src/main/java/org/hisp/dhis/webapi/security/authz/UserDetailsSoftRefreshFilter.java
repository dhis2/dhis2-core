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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.authz.AuthzConstants;
import org.hisp.dhis.user.authz.AuthzService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Soft-refreshes immutable session {@link UserDetails} when dual-generation stamps change.
 *
 * <p>Does not expire sessions. Replaces Authentication principal and explicitly saves the
 * SecurityContext ({@code requireExplicitSave(true)}). Does not re-register with SessionRegistry.
 *
 * @author Morten Svanæs
 */
@Slf4j
public class UserDetailsSoftRefreshFilter extends OncePerRequestFilter {

  private final AuthzService authzService;
  private final SecurityContextRepository securityContextRepository;

  public UserDetailsSoftRefreshFilter(AuthzService authzService) {
    this(authzService, new HttpSessionSecurityContextRepository());
  }

  public UserDetailsSoftRefreshFilter(
      AuthzService authzService, SecurityContextRepository securityContextRepository) {
    this.authzService = authzService;
    this.securityContextRepository = securityContextRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      UserDetails current = extractUserDetails(authentication);
      if (current != null) {
        long sessionGen = readSessionGen(request);
        long effective = authzService.effectiveGen(current);
        if (effective != sessionGen) {
          UserDetails fresh = authzService.loadFreshUserDetails(current.getUsername());
          if (fresh != null) {
            Authentication replacement = rebuildAuthentication(authentication, fresh);
            if (replacement != null) {
              SecurityContext context = SecurityContextHolder.createEmptyContext();
              context.setAuthentication(replacement);
              SecurityContextHolder.setContext(context);
              securityContextRepository.saveContext(context, request, response);
              writeSessionGen(request, authzService.effectiveGen(fresh));
              log.debug(
                  "Soft-refreshed UserDetails for {} gen {} -> {}",
                  fresh.getUsername(),
                  sessionGen,
                  effective);
            }
          }
        }
      }
    }
    filterChain.doFilter(request, response);
  }

  private static UserDetails extractUserDetails(Authentication authentication) {
    Object principal = authentication.getPrincipal();
    if (principal instanceof DhisOidcUser oidcUser) {
      Object nested = oidcUser.getUser();
      if (nested instanceof UserDetails ud) {
        return ud;
      }
    }
    if (principal instanceof UserDetails ud) {
      return ud;
    }
    return null;
  }

  private static Authentication rebuildAuthentication(
      Authentication existing, UserDetails fresh) {
    Object principal = existing.getPrincipal();
    if (principal instanceof DhisOidcUser oidcUser
        && existing instanceof OAuth2AuthenticationToken oauth2) {
      OidcIdToken idToken = oidcUser.getIdToken();
      Map<String, Object> attributes = oidcUser.getAttributes();
      String nameKey = resolveNameAttributeKey(oidcUser);
      DhisOidcUser newPrincipal = new DhisOidcUser(fresh, attributes, nameKey, idToken);
      return new OAuth2AuthenticationToken(
          newPrincipal, fresh.getAuthorities(), oauth2.getAuthorizedClientRegistrationId());
    }
    if (principal instanceof UserDetails) {
      UsernamePasswordAuthenticationToken token =
          new UsernamePasswordAuthenticationToken(
              fresh, existing.getCredentials(), fresh.getAuthorities());
      token.setDetails(existing.getDetails());
      return token;
    }
    return null;
  }

  private static String resolveNameAttributeKey(DhisOidcUser oidcUser) {
    Map<String, Object> attributes = oidcUser.getAttributes();
    if (attributes.containsKey("sub")) {
      return "sub";
    }
    if (attributes.containsKey("email")) {
      return "email";
    }
    if (!attributes.isEmpty()) {
      return attributes.keySet().iterator().next();
    }
    return "sub";
  }

  private static long readSessionGen(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return 0L;
    }
    Object value = session.getAttribute(AuthzConstants.SESSION_AUTHZ_GEN_ATTR);
    if (value instanceof Long l) {
      return l;
    }
    if (value instanceof Number n) {
      return n.longValue();
    }
    if (value instanceof String s) {
      try {
        return Long.parseLong(s);
      } catch (NumberFormatException ignored) {
        return 0L;
      }
    }
    return 0L;
  }

  private static void writeSessionGen(HttpServletRequest request, long gen) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.setAttribute(AuthzConstants.SESSION_AUTHZ_GEN_ATTR, gen);
    }
  }
}
