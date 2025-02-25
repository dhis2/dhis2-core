/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.security.oauth2.consent;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdentifiableObjectStore;

/** Store for OAuth2AuthorizationConsent entities. */
public interface OAuth2AuthorizationConsentStore
    extends IdentifiableObjectStore<OAuth2AuthorizationConsent> {

  /**
   * Retrieves an OAuth2AuthorizationConsent by registered client ID and principal name.
   *
   * @param registeredClientId the ID of the registered client
   * @param principalName the name of the principal (user)
   * @return the OAuth2AuthorizationConsent, or null if not found
   */
  @CheckForNull
  OAuth2AuthorizationConsent getByRegisteredClientIdAndPrincipalName(
      @Nonnull String registeredClientId, @Nonnull String principalName);

  /**
   * Deletes the OAuth2AuthorizationConsent with the given registered client ID and principal name.
   *
   * @param registeredClientId the ID of the registered client
   * @param principalName the name of the principal (user)
   */
  void deleteByRegisteredClientIdAndPrincipalName(
      @Nonnull String registeredClientId, @Nonnull String principalName);
}
