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

import java.util.Collections;
import java.util.Objects;
import org.hisp.dhis.user.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class ApiTokenAuthenticationToken extends AbstractAuthenticationToken {
  private String tokenKey;

  private ApiToken tokenRef;

  private User user;

  public ApiTokenAuthenticationToken(String tokenKey) {
    super(Collections.emptyList());
    this.tokenKey = tokenKey;
  }

  public ApiTokenAuthenticationToken(ApiToken token, User user) {
    super(user.getAuthorities());
    this.tokenRef = token;
    this.user = user;
  }

  @Override
  public User getCredentials() {
    return this.user;
  }

  @Override
  public UserDetails getPrincipal() {
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    ApiTokenAuthenticationToken that = (ApiTokenAuthenticationToken) o;

    if (!Objects.equals(tokenKey, that.tokenKey)) return false;
    return Objects.equals(tokenRef, that.tokenRef);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (tokenKey != null ? tokenKey.hashCode() : 0);
    result = 31 * result + (tokenRef != null ? tokenRef.hashCode() : 0);
    return result;
  }
}
