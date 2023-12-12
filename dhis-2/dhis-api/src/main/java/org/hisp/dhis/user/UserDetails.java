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
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.springframework.security.core.GrantedAuthority;

public interface UserDetails extends org.springframework.security.core.userdetails.UserDetails {

  static UserDetails fromUser(User user) {
    if (user == null) {
      return null;
    }

    return createUserDetails(
        user, user.isAccountNonLocked(), user.isCredentialsNonExpired(), new HashMap<>());
  }

  static UserDetails createUserDetails(
      User user,
      boolean accountNonLocked,
      boolean credentialsNonExpired,
      Map<String, Serializable> settings) {
    if (user == null) {
      return null;
    }

    UserDetailsImpl userDetails = new UserDetailsImpl();
    userDetails.setId(user.getId());
    userDetails.setUid(user.getUid());
    userDetails.setUsername(user.getUsername());
    userDetails.setPassword(user.getPassword());

    userDetails.setExternalAuth(user.isExternalAuth());
    userDetails.setTwoFactorEnabled(user.isTwoFactorEnabled());

    userDetails.setCode(user.getCode());
    userDetails.setFirstName(user.getFirstName());
    userDetails.setSurname(user.getSurname());

    userDetails.setEnabled(user.isEnabled());
    userDetails.setAccountNonExpired(user.isAccountNonExpired());
    userDetails.setAccountNonLocked(accountNonLocked);
    userDetails.setCredentialsNonExpired(credentialsNonExpired);

    userDetails.setAuthorities(user.getAuthorities());
    userDetails.setAllAuthorities(
        user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toUnmodifiableSet()));
    userDetails.setSuper(user.isSuper());

    userDetails.setAllRestrictions(user.getAllRestrictions());

    userDetails.setUserRoleIds(
        user.getUserRoles().stream()
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toSet()));

    Set<String> groupIds =
        user.getGroups().stream().map(BaseIdentifiableObject::getUid).collect(Collectors.toSet());
    userDetails.setUserGroupIds(user.getUid() == null ? Set.of() : groupIds);

    userDetails.setUserOrgUnitIds(
        user.getOrganisationUnits().stream()
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toSet()));

    userDetails.setUserSettings(settings);

    return userDetails;
  }

  @Override
  Collection<? extends GrantedAuthority> getAuthorities();

  @Override
  String getPassword();

  @Override
  String getUsername();

  @Override
  boolean isAccountNonExpired();

  @Override
  boolean isAccountNonLocked();

  @Override
  boolean isCredentialsNonExpired();

  @Override
  boolean isEnabled();

  boolean isSuper();

  String getUid();

  Long getId();

  String getCode();

  String getFirstName();

  String getSurname();

  Set<String> getUserGroupIds();

  Set<String> getAllAuthorities();

  Set<String> getUserOrgUnitIds();

  boolean hasAnyAuthority(Collection<String> auths);

  boolean isAuthorized(String auth);

  Map<String, Serializable> getUserSettings();

  Set<String> getUserRoleIds();

  boolean canModifyUser(User userToModify);

  boolean isExternalAuth();

  boolean isTwoFactorEnabled();

  boolean hasAnyRestrictions(Collection<String> restrictions);
}
