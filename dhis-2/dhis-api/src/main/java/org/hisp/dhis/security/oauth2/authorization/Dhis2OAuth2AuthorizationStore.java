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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdentifiableObjectStore;

/** Store for OAuth2Authorization entities. */
public interface Dhis2OAuth2AuthorizationStore
    extends IdentifiableObjectStore<Dhis2OAuth2Authorization> {

  /**
   * Retrieves an OAuth2Authorization by its state value.
   *
   * @param state the state value to search for
   * @return the OAuth2Authorization with the given state, or null if not found
   */
  @CheckForNull
  Dhis2OAuth2Authorization getByState(@Nonnull String state);

  /**
   * Retrieves an OAuth2Authorization by its authorization code value.
   *
   * @param authorizationCode the authorization code to search for
   * @return the OAuth2Authorization with the given authorization code, or null if not found
   */
  @CheckForNull
  Dhis2OAuth2Authorization getByAuthorizationCode(@Nonnull String authorizationCode);

  /**
   * Retrieves an OAuth2Authorization by its access token value.
   *
   * @param accessToken the access token to search for
   * @return the OAuth2Authorization with the given access token, or null if not found
   */
  @CheckForNull
  Dhis2OAuth2Authorization getByAccessToken(@Nonnull String accessToken);

  /**
   * Retrieves an OAuth2Authorization by its refresh token value.
   *
   * @param refreshToken the refresh token to search for
   * @return the OAuth2Authorization with the given refresh token, or null if not found
   */
  @CheckForNull
  Dhis2OAuth2Authorization getByRefreshToken(@Nonnull String refreshToken);

  /**
   * Retrieves an OAuth2Authorization by its OIDC ID token value.
   *
   * @param idToken the ID token to search for
   * @return the OAuth2Authorization with the given ID token, or null if not found
   */
  @CheckForNull
  Dhis2OAuth2Authorization getByOidcIdToken(@Nonnull String idToken);

  /**
   * Retrieves an OAuth2Authorization by its user code value.
   *
   * @param userCode the user code to search for
   * @return the OAuth2Authorization with the given user code, or null if not found
   */
  @CheckForNull
  Dhis2OAuth2Authorization getByUserCode(@Nonnull String userCode);

  /**
   * Retrieves an OAuth2Authorization by its device code value.
   *
   * @param deviceCode the device code to search for
   * @return the OAuth2Authorization with the given device code, or null if not found
   */
  @CheckForNull
  Dhis2OAuth2Authorization getByDeviceCode(@Nonnull String deviceCode);

  /**
   * Retrieves an OAuth2Authorization by any token value. This method searches across all token
   * types.
   *
   * @param token the token value to search for across all token types
   * @return the OAuth2Authorization containing the given token, or null if not found
   */
  @CheckForNull
  Dhis2OAuth2Authorization getByToken(@Nonnull String token);

  /**
   * Deletes an OAuth2Authorization by its UID.
   *
   * @param uid the UID of the OAuth2Authorization to delete
   */
  void deleteByUID(@Nonnull String uid);
}
