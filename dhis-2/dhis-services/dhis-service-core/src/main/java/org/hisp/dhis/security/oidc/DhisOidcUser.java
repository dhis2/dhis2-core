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
package org.hisp.dhis.security.oidc;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class DhisOidcUser extends DefaultOAuth2User implements UserDetails, OidcUser {
  private final OidcIdToken oidcIdToken;

  private final UserDetails user;

  public DhisOidcUser(
      UserDetails user,
      Map<String, Object> attributes,
      String nameAttributeKey,
      OidcIdToken idToken) {
    super(user.getAuthorities(), attributes, nameAttributeKey);
    this.oidcIdToken = idToken;
    this.user = user;
  }

  @Override
  public Map<String, Object> getClaims() {
    return this.getAttributes();
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return null;
  }

  @Override
  public OidcIdToken getIdToken() {
    return oidcIdToken;
  }

  public org.springframework.security.core.userdetails.UserDetails getUser() {
    return user;
  }

  @Override
  public String getUsername() {
    return user.getUsername();
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public boolean isAccountNonExpired() {
    return user.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return user.isAccountNonLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return user.isCredentialsNonExpired();
  }

  @Override
  public boolean isEnabled() {
    return user.isEnabled();
  }

  @Override
  public boolean isSuper() {
    return user.isSuper();
  }

  @Override
  public String getUid() {
    return user.getUid();
  }

  @Override
  public Long getId() {
    return user.getId();
  }

  @Override
  public String getCode() {
    return user.getCode();
  }

  @Override
  public String getFirstName() {
    return user.getFirstName();
  }

  @Override
  public String getSurname() {
    return user.getSurname();
  }

  @Override
  public Set<String> getUserGroupIds() {
    return user.getUserGroupIds();
  }

  @Override
  public Set<String> getAllAuthorities() {
    return user.getAllAuthorities();
  }

  @Override
  public Set<String> getUserOrgUnitIds() {
    return user.getUserOrgUnitIds();
  }

  @Override
  public boolean hasAnyAuthority(Collection<String> auths) {
    return false;
  }

  @Override
  public boolean isAuthorized(String auth) {
    return false;
  }

  @Override
  public Map<String, Serializable> getUserSettings() {
    return user.getUserSettings();
  }

  @Override
  public Set<String> getUserRoleIds() {
    return user.getUserRoleIds();
  }

  @Override
  public boolean canModifyUser(User userToModify) {
    return user.canModifyUser(userToModify);
  }

  @Override
  public boolean isExternalAuth() {
    return user.isExternalAuth();
  }

  @Override
  public boolean isTwoFactorEnabled() {
    return user.isTwoFactorEnabled();
  }

  @Override
  public boolean hasAnyRestrictions(Collection<String> restrictions) {
    return user.hasAnyRestrictions(restrictions);
  }

  @Override
  public void setId(Long id) {
    user.setId(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    DhisOidcUser that = (DhisOidcUser) o;

    return Objects.equals(user, that.user);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (user != null ? user.hashCode() : 0);
    return result;
  }
}
