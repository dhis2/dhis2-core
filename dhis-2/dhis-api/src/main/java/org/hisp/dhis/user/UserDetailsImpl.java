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
import java.util.Map;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
public class UserDetailsImpl implements UserDetails {

  private String uid;
  private Long id;
  private String code;
  @EqualsAndHashCode.Include private String username;
  private String firstName;
  private String surname;
  private String password;
  private boolean externalAuth;
  private boolean isTwoFactorEnabled;
  private boolean enabled;
  private boolean accountNonExpired;
  private boolean accountNonLocked;
  private boolean credentialsNonExpired;
  private Collection<GrantedAuthority> authorities;
  private Set<String> allAuthorities;
  private Set<String> allRestrictions;
  private Map<String, Serializable> userSettings;
  private Set<String> userGroupIds;
  private Set<String> userOrgUnitIds;
  private boolean isSuper;
  private Set<String> userRoleIds;

  @Override
  public boolean canModifyUser(User other) {
    if (other == null) {
      return false;
    }

    final Set<String> auths = getAllAuthorities();
    if (auths.contains(UserRole.AUTHORITY_ALL)) {
      return true;
    }

    return auths.containsAll(other.getAllAuthorities());
  }

  @Override
  public boolean hasAnyRestrictions(Collection<String> restrictions) {
    return getAllRestrictions().stream().anyMatch(restrictions::contains);
  }

  @Override
  public boolean hasAnyAuthority(Collection<String> auths) {
    return getAllAuthorities().stream().anyMatch(auths::contains);
  }

  @Override
  public boolean isAuthorized(String auth) {
    if (auth == null) {
      return false;
    }
    final Set<String> auths = getAllAuthorities();
    return auths.contains(UserRole.AUTHORITY_ALL) || auths.contains(auth);
  }
}
