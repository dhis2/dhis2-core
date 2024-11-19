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

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.security.Authorities;
import org.springframework.security.core.GrantedAuthority;

@Getter
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
public class UserDetailsImpl implements UserDetails {

  private final String uid;
  @Setter private Long id;
  private final String code;
  @EqualsAndHashCode.Include private final String username;
  private final String firstName;
  private final String surname;
  private final String password;
  private final boolean externalAuth;
  private final boolean isTwoFactorEnabled;
  private final boolean isEmailVerified;
  private final boolean enabled;
  private final boolean accountNonExpired;
  private final boolean accountNonLocked;
  private final boolean credentialsNonExpired;
  @Nonnull private final Collection<GrantedAuthority> authorities;
  @Nonnull private final Set<String> allAuthorities;
  @Nonnull private final Set<String> allRestrictions;
  @Nonnull private final Set<String> userGroupIds;
  @Nonnull private final Set<String> userOrgUnitIds;
  @Nonnull private final Set<String> userDataOrgUnitIds;
  @Nonnull private final Set<String> userSearchOrgUnitIds;
  @Nonnull private final Set<String> userEffectiveSearchOrgUnitIds;
  private final boolean isSuper;
  @Nonnull private final Set<String> userRoleIds;

  @Override
  public boolean canModifyUser(User other) {
    if (other == null) {
      return false;
    }

    final Set<String> auths = getAllAuthorities();
    if (auths.contains(Authorities.ALL.toString())) {
      return true;
    }

    return auths.containsAll(other.getAllAuthorities());
  }

  @Override
  public boolean isEmailVerified() {
    return this.isEmailVerified;
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
  public boolean hasAnyAuthorities(Collection<Authorities> auths) {
    return hasAnyAuthority(Authorities.toStringList(auths));
  }

  @Override
  public boolean isAuthorized(String auth) {
    if (auth == null) {
      return false;
    }
    final Set<String> auths = getAllAuthorities();
    return auths.contains(Authorities.ALL.toString()) || auths.contains(auth);
  }

  @Override
  public boolean isAuthorized(@Nonnull Authorities auth) {
    return isAuthorized(auth.toString());
  }
}
