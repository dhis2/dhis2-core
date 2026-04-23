/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.security.oauth2.authorization;

import java.util.List;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

/**
 * DHIS2 service for persisting OAuth2 authorizations (issued grants). Backs Spring Authorization
 * Server's {@link
 * org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService}.
 *
 * <p>One row is stored per grant and holds every token value issued for that grant for its
 * lifetime: authorization code, access token, refresh token, OIDC ID token, user code, and device
 * code. Each is also independently indexed so token-introspection lookups can find the parent
 * authorization by any of those values.
 */
public interface Dhis2OAuth2AuthorizationService {
  /** Persist a new authorization or update the existing row with the same id. */
  void save(
      org.springframework.security.oauth2.server.authorization.OAuth2Authorization authorization);

  /**
   * Remove the persisted authorization identified by the given Spring-AS {@code
   * OAuth2Authorization}.
   */
  void remove(
      org.springframework.security.oauth2.server.authorization.OAuth2Authorization authorization);

  /** Delete the persisted authorization with the given DHIS2 UID. */
  void delete(String uid);

  /** Return all persisted authorizations. */
  List<Dhis2OAuth2Authorization> getAll();

  /** Look up an authorization by its id. Returns {@code null} if no match. */
  org.springframework.security.oauth2.server.authorization.OAuth2Authorization findById(String id);

  /**
   * Look up an authorization by token value. When {@code tokenType} is {@code null}, all token
   * columns are searched; otherwise the column matching {@link OAuth2TokenType} (or equivalent
   * Spring-AS parameter name) is used. Returns {@code null} if no match.
   */
  org.springframework.security.oauth2.server.authorization.OAuth2Authorization findByToken(
      String token, OAuth2TokenType tokenType);
}
