/*
 * Copyright (c) 2004-2023, University of Oslo
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
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.security.Authorities;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class SystemUser implements UserDetails {

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of((GrantedAuthority) Authorities.ALL::name);
  }

  @Override
  public Set<String> getAllAuthorities() {
    return Set.of(Authorities.ALL.name());
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public String getUsername() {
    // Never rely on this method to get the username of the system user for identification.
    return "system-process_" + CodeGenerator.generateUid();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public boolean isSuper() {
    return true;
  }

  @Override
  public String getUid() {
    return "system-process";
  }

  @Override
  public Long getId() {
    return -1L;
  }

  @Override
  public String getCode() {
    return "code_" + CodeGenerator.generateUid();
  }

  @Override
  public String getFirstName() {
    return "system";
  }

  @Override
  public String getSurname() {
    return "user";
  }

  @Override
  public Set<String> getUserGroupIds() {
    return Set.of();
  }

  @Override
  public Set<String> getUserOrgUnitIds() {
    return Set.of();
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
    return Map.of();
  }

  @Override
  public Set<String> getUserRoleIds() {
    return Set.of();
  }

  @Override
  public boolean canModifyUser(User userToModify) {
    return true;
  }

  @Override
  public boolean isExternalAuth() {
    return false;
  }

  @Override
  public boolean isTwoFactorEnabled() {
    return false;
  }

  @Override
  public boolean hasAnyRestrictions(Collection<String> restrictions) {
    return false;
  }

  @Override
  public void setId(Long id) {
    // do nothing
  }
}
