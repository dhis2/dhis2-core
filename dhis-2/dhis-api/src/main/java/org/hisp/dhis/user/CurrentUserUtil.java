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
import java.util.Map;
import javax.annotation.Nonnull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class CurrentUserUtil {

  private CurrentUserUtil() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication.getPrincipal() == null) {
      return null;
    }

    Object principal = authentication.getPrincipal();

    // Principal being a string implies anonymous authentication
    // This is the state before the user is authenticated.
    if (principal instanceof String) {
      if (!"anonymousUser".equals(principal)) {
        return null;
      }

      return (String) principal;
    }

    if (principal instanceof UserDetails) {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      return userDetails.getUsername();
    } else {
      throw new RuntimeException(
          "Authentication principal is not supported; principal:" + principal);
    }
  }

  public static CurrentUserDetails getCurrentUserDetails() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication.getPrincipal() == null) {
      return null;
    }

    Object principal = authentication.getPrincipal();

    // Principal being a string implies anonymous authentication
    // This is the state before the user is authenticated.
    if (principal instanceof String) {
      if (!"anonymousUser".equals(principal)) {
        return null;
      }

      return null;
    }

    if (principal instanceof CurrentUserDetails) {
      return (CurrentUserDetails) authentication.getPrincipal();
    } else {
      throw new RuntimeException(
          "Authentication principal is not supported; principal:" + principal);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T getUserSetting(UserSettingKey key) {
    CurrentUserDetails currentUser = getCurrentUserDetails();
    if (currentUser == null) {
      return null;
    }

    return (T) currentUser.getUserSettings().get(key.getName());
  }

  /**
   * Return the value of the user setting referred to by 'key'.
   *
   * @param defaultValue the default value to return if the setting value is null.
   * @param key the key of the user setting
   * @return the value of the user setting
   */
  @SuppressWarnings("unchecked")
  public static <T> T getUserSetting(UserSettingKey key, @Nonnull T defaultValue) {
    CurrentUserDetails currentUser = getCurrentUserDetails();
    if (currentUser == null) {
      return defaultValue;
    }

    Map<String, Serializable> userSettings = currentUser.getUserSettings();

    Serializable setting = userSettings.get(key.getName());

    return setting != null ? (T) setting : defaultValue;
  }

  public static void setUserSetting(UserSettingKey key, Serializable value) {
    setUserSettingInternal(key.getName(), value);
  }

  private static void setUserSettingInternal(String key, Serializable value) {
    CurrentUserDetails currentUser = getCurrentUserDetails();
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
