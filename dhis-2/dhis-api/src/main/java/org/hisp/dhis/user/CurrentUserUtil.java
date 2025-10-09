/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.user;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUserUtil {
  private CurrentUserUtil() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Get the current authentication object.
   *
   * <p>Throws an IllegalStateException if no valid authentication is found.
   *
   * @return the current Authentication
   */
  @Nonnull
  public static Authentication getAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication.getPrincipal() == null) {
      throw new IllegalStateException("No authentication found");
    }
    return authentication;
  }

  /**
   * Get the username of the currently authenticated user
   *
   * @return the current user's username
   */
  @Nonnull
  public static String getCurrentUsername() {
    Authentication authentication = getAuthentication();

    Object principal = authentication.getPrincipal();

    if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
      org.springframework.security.core.userdetails.UserDetails userDetails =
          (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
      return userDetails.getUsername();

    } else {
      throw new IllegalStateException(
          "Authentication principal is not supported; principal:" + principal);
    }
  }

  /**
   * Get details about the currently authenticated user
   *
   * @return CurrentUserDetails representing the authenticated user
   */
  @Nonnull
  public static UserDetails getCurrentUserDetails() {
    Authentication authentication = getAuthentication();

    Object principal = authentication.getPrincipal();

    if (principal instanceof UserDetails) {
      return (UserDetails) authentication.getPrincipal();
    } else {
      throw new IllegalStateException(
          "Authentication principal is not supported; principal:" + principal);
    }
  }

  /**
   * Get all authorities assigned to the current user Return an empty list if the current session is
   * anonymous
   *
   * @return list of authority names
   */
  public static List<String> getCurrentUserAuthorities() {
    if (!CurrentUserUtil.hasCurrentUser()) {
      return List.of();
    }

    UserDetails currentUserDetails = getCurrentUserDetails();
    return currentUserDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();
  }

  /**
   * Check if the current user has any of the passed candidate authorities
   *
   * @param candidateAuthorities a list of possible authorities to check against
   * @return true if the user has one or more of the candidateAuthorities
   */
  public static Boolean hasAnyAuthority(Collection<String> candidateAuthorities) {
    List<String> currentUserAuthorities = getCurrentUserAuthorities();
    return candidateAuthorities.stream().anyMatch(currentUserAuthorities::contains);
  }

  public static boolean hasCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() != null
        && authentication.getPrincipal()
            instanceof org.springframework.security.core.userdetails.UserDetails;
  }

  public static void injectUserInSecurityContext(UserDetails actingUser) {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(actingUser, "", actingUser.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }

  public static void clearSecurityContext() {
    SecurityContext context = SecurityContextHolder.getContext();
    if (context != null) {
      SecurityContextHolder.getContext().setAuthentication(null);
    }
    SecurityContextHolder.clearContext();
  }
}
