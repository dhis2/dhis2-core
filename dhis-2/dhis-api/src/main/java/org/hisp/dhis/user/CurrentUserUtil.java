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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CurrentUserUtil {
  private static final String ANONYMOUS_USER = "anonymousUser";

  /**
   * Returns the username of the current user.
   *
   * @return the username of the current user.
   */
  public static String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication.getPrincipal() == null) {
      return null;
    }

    Object principal = authentication.getPrincipal();

    // String principal implies anonymous authentication and the state before user is authenticated

    if (principal instanceof String) {
      if (!ANONYMOUS_USER.equals(principal)) {
        return null;
      }

      return (String) principal;
    }

    return getCurrentUserDetails(authentication).getUsername();
  }

  /**
   * Returns the {@link CurrentUserDetails} representing the current user.
   *
   * @return the {@link CurrentUserDetails} representing the current user.
   */
  public static CurrentUserDetails getCurrentUserDetails() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication.getPrincipal() == null) {
      return null;
    }

    Object principal = authentication.getPrincipal();

    // String principal implies anonymous authentication and the state before user is authenticated

    if (principal instanceof String) {
      if (!ANONYMOUS_USER.equals(principal)) {
        return null;
      }

      return null;
    }

    return getCurrentUserDetails(authentication);
  }

  /**
   * Returns the {@link CurrentUserDetails} associated with the authentication.
   *
   * <p>The UID of the user is set on the 'details' property of {@link LdapUserDetailsImpl}.
   *
   * @param authentication the {@link Authentication}.
   * @return the {@link CurrentUserDetails}.
   */
  static CurrentUserDetails getCurrentUserDetails(Authentication authentication) {
    Object principal = authentication.getPrincipal();
    if (principal instanceof CurrentUserDetails) {
      return (CurrentUserDetails) principal;
    } else if (principal instanceof LdapUserDetails) {
      LdapUserDetailsImpl ldapUserDetails = (LdapUserDetailsImpl) principal;
      Objects.requireNonNull(authentication.getDetails());

      return CurrentUserDetailsImpl.builder()
          .uid(String.valueOf(authentication.getDetails()))
          .username(ldapUserDetails.getUsername())
          .password(ldapUserDetails.getPassword())
          .accountNonExpired(ldapUserDetails.isAccountNonExpired())
          .accountNonLocked(ldapUserDetails.isAccountNonLocked())
          .credentialsNonExpired(ldapUserDetails.isCredentialsNonExpired())
          .enabled(ldapUserDetails.isEnabled())
          .authorities(ldapUserDetails.getAuthorities())
          .userSettings(new HashMap<>())
          .build();
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

    Map<String, Serializable> userSettings = currentUser.getUserSettings();
    if (userSettings == null) {
      return null;
    }

    return (T) userSettings.get(key.getName());
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
}
