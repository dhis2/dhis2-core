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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.user.UserDetailsImpl.UserDetailsImplBuilder;
import org.springframework.security.core.GrantedAuthority;

public interface UserDetails extends org.springframework.security.core.userdetails.UserDetails {

  // TODO MAS: This is a workaround and usually indicated a design flaw, and that we should refactor
  // to use UserDetails higher up in the layers.

  /**
   * Create UserDetails from User
   *
   * @param user user to convert
   * @return UserDetails
   */
  @CheckForNull
  static UserDetails fromUser(@CheckForNull User user) {
    // TODO check in session if a UserDetails for the user already exists (if the user is the
    // current user)
    if (user == null) {
      return null;
    }
    return createUserDetails(
        user,
        user.isAccountNonLocked(),
        user.isCredentialsNonExpired(),
        null,
        null,
        null,
        new HashMap<>(),
        true);
  }

  /**
   * Create UserDetails from User without loading org units. NB: ONLY use if you are 100% sure that
   * the user is not going to be used to retrieve org units
   *
   * @param user user to convert
   * @return UserDetails
   */
  static UserDetails fromUserDontLoadOrgUnits(User user) {
    if (user == null) {
      return null;
    }
    return createUserDetails(
        user,
        user.isAccountNonLocked(),
        user.isCredentialsNonExpired(),
        null,
        null,
        null,
        new HashMap<>(),
        false);
  }

  @CheckForNull
  static UserDetails createUserDetails(
      @CheckForNull User user,
      boolean accountNonLocked,
      boolean credentialsNonExpired,
      @CheckForNull Set<String> orgUnitUids,
      @CheckForNull Set<String> searchOrgUnitUids,
      @CheckForNull Set<String> dataViewUnitUids,
      @CheckForNull Map<String, Serializable> settings) {
    return createUserDetails(
        user,
        accountNonLocked,
        credentialsNonExpired,
        orgUnitUids,
        searchOrgUnitUids,
        dataViewUnitUids,
        settings,
        true);
  }

  @CheckForNull
  static UserDetails createUserDetails(
      @CheckForNull User user,
      boolean accountNonLocked,
      boolean credentialsNonExpired,
      @CheckForNull Set<String> orgUnitUids,
      @CheckForNull Set<String> searchOrgUnitUids,
      @CheckForNull Set<String> dataViewUnitUids,
      @CheckForNull Map<String, Serializable> settings,
      boolean loadOrgUnits) {

    if (user == null) {
      return null;
    }

    UserDetailsImplBuilder userDetailsImplBuilder =
        UserDetailsImpl.builder()
            .id(user.getId())
            .uid(user.getUid())
            .username(user.getUsername())
            .password(user.getPassword())
            .externalAuth(user.isExternalAuth())
            .isTwoFactorEnabled(user.isTwoFactorEnabled())
            .code(user.getCode())
            .firstName(user.getFirstName())
            .surname(user.getSurname())
            .enabled(user.isEnabled())
            .accountNonExpired(user.isAccountNonExpired())
            .accountNonLocked(accountNonLocked)
            .credentialsNonExpired(credentialsNonExpired)
            .authorities(user.getAuthorities())
            .allAuthorities(
                Set.copyOf(
                    user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()))
            .isSuper(user.isSuper())
            .userRoleIds(setOfIds(user.getUserRoles()))
            .userGroupIds(user.getUid() == null ? Set.of() : setOfIds(user.getGroups()))
            .userSettings(settings == null ? new HashMap<>() : new HashMap<>(settings));

    if (loadOrgUnits) {

      Set<String> userOrgUnitIds =
          (orgUnitUids == null) ? setOfIds(user.getOrganisationUnits()) : orgUnitUids;

      Set<String> userSearchOrgUnitIds =
          (searchOrgUnitUids == null)
              ? setOfIds(user.getTeiSearchOrganisationUnitsWithFallback())
              : (searchOrgUnitUids.isEmpty() ? orgUnitUids : searchOrgUnitUids);

      Set<String> userDataOrgUnitIds =
          (dataViewUnitUids == null)
              ? setOfIds(user.getDataViewOrganisationUnitsWithFallback())
              : (dataViewUnitUids.isEmpty() ? orgUnitUids : dataViewUnitUids);

      userDetailsImplBuilder
          .userOrgUnitIds(userOrgUnitIds)
          .userSearchOrgUnitIds(userSearchOrgUnitIds)
          .userDataOrgUnitIds(userDataOrgUnitIds)
          .allRestrictions(user.getAllRestrictions());

    } else {
      userDetailsImplBuilder
          .userOrgUnitIds(Set.of())
          .userSearchOrgUnitIds(Set.of())
          .userDataOrgUnitIds(Set.of())
          .allRestrictions(Set.of());
    }

    return userDetailsImplBuilder.build();
  }

  @Nonnull
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

  @Nonnull
  Set<String> getUserGroupIds();

  @Nonnull
  Set<String> getAllAuthorities();

  @Nonnull
  Set<String> getUserOrgUnitIds();

  @Nonnull
  Set<String> getUserSearchOrgUnitIds();

  @Nonnull
  Set<String> getUserDataOrgUnitIds();

  boolean hasAnyAuthority(Collection<String> auths);

  boolean isAuthorized(String auth);

  @Nonnull
  Map<String, Serializable> getUserSettings();

  @Nonnull
  Set<String> getUserRoleIds();

  boolean canModifyUser(User userToModify);

  boolean isExternalAuth();

  boolean isTwoFactorEnabled();

  boolean hasAnyRestrictions(Collection<String> restrictions);

  void setId(Long id);

  default boolean isInUserHierarchy(String orgUnitPath) {
    return isInUserHierarchy(orgUnitPath, getUserOrgUnitIds());
  }

  default boolean isInUserSearchHierarchy(String orgUnitPath) {
    return isInUserHierarchy(orgUnitPath, getUserSearchOrgUnitIds());
  }

  default boolean isInUserDataHierarchy(String orgUnitPath) {
    return isInUserHierarchy(orgUnitPath, getUserDataOrgUnitIds());
  }

  static boolean isInUserHierarchy(
      @CheckForNull String orgUnitPath, @Nonnull Set<String> orgUnitIds) {
    if (orgUnitPath == null) return false;
    for (String uid : orgUnitPath.split("/")) if (orgUnitIds.contains(uid)) return true;
    return false;
  }

  @Nonnull
  private static Set<String> setOfIds(
      @CheckForNull Collection<? extends IdentifiableObject> objects) {
    return objects == null || objects.isEmpty()
        ? Set.of()
        : Set.copyOf(objects.stream().map(IdentifiableObject::getUid).toList());
  }
}
