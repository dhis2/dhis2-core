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
package org.hisp.dhis.security.apikey;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.hisp.dhis.user.CurrentUserDetails;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class ApiTokenAuthenticationToken extends AbstractAuthenticationToken
    implements CurrentUserDetails {
  private String tokenKey;

  private ApiToken tokenRef;

  private CurrentUserDetails user;

  public ApiTokenAuthenticationToken(String tokenKey) {
    super(Collections.emptyList());
    this.tokenKey = tokenKey;
  }

  public ApiTokenAuthenticationToken(ApiToken token, CurrentUserDetails user) {
    super(user.getAuthorities());
    this.tokenRef = token;
    this.user = user;
  }

  @Override
  public CurrentUserDetails getCredentials() {
    return this.user;
  }

  @Override
  public CurrentUserDetails getPrincipal() {
    return this.user;
  }

  public String getTokenKey() {
    return tokenKey;
  }

  public void setTokenKey(String tokenKey) {
    this.tokenKey = tokenKey;
  }

  public ApiToken getToken() {
    return this.tokenRef;
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getUsername();
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
  public Set<String> getUserGroupIds() {
    return user.getUserGroupIds();
  }

  @Override
  public Map<String, Serializable> getUserSettings() {
    return user.getUserSettings();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    ApiTokenAuthenticationToken that = (ApiTokenAuthenticationToken) o;
    return Objects.equals(tokenKey, that.tokenKey) && Objects.equals(tokenRef, that.tokenRef);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), tokenKey, tokenRef);
  }
}
