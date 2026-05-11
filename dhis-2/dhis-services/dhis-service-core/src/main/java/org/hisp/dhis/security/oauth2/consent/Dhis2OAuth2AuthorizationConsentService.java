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
package org.hisp.dhis.security.oauth2.consent;

import java.util.List;

/**
 * DHIS2 service for persisting per-(user, client) OAuth2 consent grants. Backs Spring Authorization
 * Server's {@link
 * org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService}.
 *
 * <p>Identity is the composite {@code (registeredClientId, principalName)}; each row holds the set
 * of authorities (scopes) the user granted that client. Clients registered via DCR bypass the
 * consent screen (they are saved with {@code requireAuthorizationConsent=false}) and therefore do
 * not create rows here.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface Dhis2OAuth2AuthorizationConsentService {
  /** Persist the consent or update the existing row with the same composite identity. */
  void save(
      org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
          authorizationConsent);

  /** Delete the consent row matching the given Spring-AS consent's composite identity. */
  void remove(
      org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
          authorizationConsent);

  /**
   * Look up a consent by its composite identity. Returns {@code null} if none.
   *
   * @param registeredClientId the client's DHIS2 UID
   * @param principalName the consenting user's principal name
   */
  org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent findById(
      String registeredClientId, String principalName);

  /** Return all persisted consent rows. */
  List<Dhis2OAuth2AuthorizationConsent> getAll();

  /** Delete the given persisted consent entity. */
  void delete(Dhis2OAuth2AuthorizationConsent consent);
}
