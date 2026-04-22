/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.security.oauth2;

import javax.annotation.Nonnull;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Shared helpers for Spring Authorization Server {@link AuthorizationGrantType} values.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class OAuth2GrantTypes {

  private OAuth2GrantTypes() {}

  /**
   * Map a grant-type string back to Spring's canonical {@link AuthorizationGrantType} singleton
   * (authorization_code, client_credentials, refresh_token, device_code). Falls back to a new
   * instance for any custom value — the equality contract on {@code AuthorizationGrantType} is
   * value-based, but returning the singleton where possible keeps identity comparisons working.
   */
  public static AuthorizationGrantType resolve(@Nonnull String value) {
    if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(value)) {
      return AuthorizationGrantType.AUTHORIZATION_CODE;
    }
    if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(value)) {
      return AuthorizationGrantType.CLIENT_CREDENTIALS;
    }
    if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(value)) {
      return AuthorizationGrantType.REFRESH_TOKEN;
    }
    if (AuthorizationGrantType.DEVICE_CODE.getValue().equals(value)) {
      return AuthorizationGrantType.DEVICE_CODE;
    }
    return new AuthorizationGrantType(value);
  }
}
