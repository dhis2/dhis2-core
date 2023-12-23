/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUserUtil {
  private CurrentUserUtil() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Get the username of the currently authenticated user
   *
   * @return the current user's username
   */
  public static String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication.getPrincipal() == null) {
      return null;
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
      org.springframework.security.core.userdetails.UserDetails userDetails =
          (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
      return userDetails.getUsername();

    } else {
      throw new RuntimeException(
          "Authentication principal is not supported; principal:" + principal);
    }
  }

  /**
   * Get details about the currently authenticated user
   *
   * @return CurrentUserDetails representing the authenticated user, or null if the user is
   *     unauthenticated
   */
  public static UserDetails getCurrentUserDetails() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication.getPrincipal() == null) {
      return null;
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof UserDetails) {
      return (UserDetails) authentication.getPrincipal();
    } else {
      throw new RuntimeException(
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

    if (currentUserDetails == null) {
      // Anonymous user has no authorities
      return List.of();
    }

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

  /**
   * Return the value of the user setting referred to by 'key'
   *
   * @param key the key of the user setting
   * @return the value of the user setting
   */
  @SuppressWarnings("unchecked")
  public static <T> T getUserSetting(UserSettingKey key) {
    UserDetails currentUser = getCurrentUserDetails();
    if (currentUser == null) {
      return null;
    }

    Map<String, Serializable> userSettings = currentUser.getUserSettings();
    if (userSettings == null) {
      return null;
    }

    return (T) userSettings.get(key.getName());
  }

  /**
   * Set the value of the user setting referred to by 'key'
   *
   * @param key the key of the user setting
   * @param value the value to set
   */
  public static void setUserSetting(UserSettingKey key, Serializable value) {
    setUserSettingInternal(key.getName(), value);
  }

  private static void setUserSettingInternal(String key, Serializable value) {
    UserDetails currentUser = getCurrentUserDetails();
    if (currentUser != null) {
      Map<String, Serializable> userSettings = currentUser.getUserSettings();
      if (userSettings != null) {
        if (value != null) {
          userSettings.put(key, value);
        } else {
          userSettings.remove(key);
        }
      }
    }
  }

  public static boolean hasCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() != null
        && !authentication.getPrincipal().equals("anonymousUser");
  }
}
