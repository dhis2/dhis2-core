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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.springframework.security.core.GrantedAuthority;

@AllArgsConstructor
@Getter
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CurrentUserDetailsImpl implements CurrentUserDetails {
  private final String uid;
  private final Long id;
  private final String code;
  @EqualsAndHashCode.Include private final String username;
  private final String firstName;
  private final String surname;
  private final String password;
  private final boolean enabled;
  private final boolean accountNonExpired;
  private final boolean accountNonLocked;
  private final boolean credentialsNonExpired;
  private final Collection<GrantedAuthority> authorities;
  private final Map<String, Serializable> userSettings;
  private final Set<String> userGroupIds;
  private final Set<String> userOrgUnitIds;
  private final boolean isSuper;
  private final Set<String> userRoleIds;

  public Set<String> getAllAuthorities() {
    return authorities == null
        ? Set.of()
        : authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toUnmodifiableSet());
  }

  public boolean hasAnyAuthority(Collection<String> auths) {
    return getAllAuthorities().stream().anyMatch(auths::contains);
  }

  public boolean isAuthorized(String auth) {
    if (auth == null) {
      return false;
    }
    final Set<String> auths = getAllAuthorities();
    return auths.contains(UserRole.AUTHORITY_ALL) || auths.contains(auth);
  }

  public static CurrentUserDetailsImpl fromUser(User user) {
    return createUserDetails(user, true, true);
  }

  public static CurrentUserDetailsImpl createUserDetails(
      User user, boolean accountNonLocked, boolean credentialsNonExpired) {

    return CurrentUserDetailsImpl.builder()
        .id(user.getId())
        .uid(user.getUid())
        .code(user.getCode())
        .firstName(user.getFirstName())
        .surname(user.getSurname())
        .username(user.getUsername())
        .password(user.getPassword())
        .enabled(user.isEnabled())
        .accountNonExpired(user.isAccountNonExpired())
        .accountNonLocked(accountNonLocked)
        .credentialsNonExpired(credentialsNonExpired)
        .authorities(user.getAuthorities())
        .userSettings(new HashMap<>())
        .userGroupIds(
            user.getUid() == null
                ? Set.of()
                : user.getGroups().stream()
                    .map(BaseIdentifiableObject::getUid)
                    .collect(Collectors.toSet()))
        .isSuper(user.isSuper())
        .userOrgUnitIds(
            user.getOrganisationUnits().stream()
                .map(BaseIdentifiableObject::getUid)
                .collect(Collectors.toSet()))
        .userRoleIds(
            user.getUserRoles().stream()
                .map(BaseIdentifiableObject::getUid)
                .collect(Collectors.toSet()))
        .build();
  }
}
