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
package org.hisp.dhis.webapi.controller.security.oauth;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for managing OAuth2 clients for the DHIS2 OAuth2 authorization server.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Controller
@RequestMapping({"/api/oAuth2Clients"})
@RequiredArgsConstructor
public class OAuth2ClientController
    extends AbstractCrudController<Dhis2OAuth2Client, GetObjectListParams> {

  private final Dhis2OAuth2ClientService clientService;

  @Override
  protected void preCreateEntity(Dhis2OAuth2Client entity) throws ConflictException {
    validateAuthorizationGrantTypes(entity);
    validateRedirectUris(entity);

    if (entity.getClientSettings() == null) {
      ClientSettings.Builder builder = ClientSettings.builder();
      builder.requireAuthorizationConsent(true);
      builder.requireProofKey(true);
      ClientSettings clientSettings = builder.build();
      entity.setClientSettings(clientService.writeMap(clientSettings.getSettings()));
    }
    if (entity.getTokenSettings() == null) {
      TokenSettings tokenSettings = TokenSettings.builder().build();
      entity.setTokenSettings(clientService.writeMap(tokenSettings.getSettings()));
    }
  }

  @Override
  protected void preUpdateEntity(Dhis2OAuth2Client entity, Dhis2OAuth2Client newEntity)
      throws ConflictException {
    validateAuthorizationGrantTypes(newEntity);
    validateRedirectUris(newEntity);

    super.preUpdateEntity(entity, newEntity);
  }

  /**
   * Validates that the authorization grant types in the entity contain only allowed values
   * (authorization_code and refresh_token)
   *
   * @param entity the OAuth2 client entity to validate
   * @throws ConflictException if any invalid grant type is found
   */
  private void validateAuthorizationGrantTypes(Dhis2OAuth2Client entity) throws ConflictException {
    if (entity.getAuthorizationGrantTypes() != null) {
      String[] grantTypes = entity.getAuthorizationGrantTypes().split(",");
      for (String grantType : grantTypes) {
        String trimmedGrantType = grantType.trim();
        if (!AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(trimmedGrantType)
            && !AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(trimmedGrantType)) {
          throw new ConflictException(
              "Invalid authorization grant type: "
                  + trimmedGrantType
                  + ". Only authorization_code and refresh_token are allowed.");
        }
      }
    }
  }

  /**
   * Validates that all redirect URIs in the entity are valid URLs. Special handling is done for
   * localhost URLs to allow for development environments.
   *
   * @param entity the OAuth2 client entity to validate
   * @throws ConflictException if any invalid URI is found
   */
  private void validateRedirectUris(Dhis2OAuth2Client entity) throws ConflictException {
    if (entity.getRedirectUris() != null) {
      String[] uris = entity.getRedirectUris().split(",");
      for (String uri : uris) {
        String trimmedUri = uri.trim();
        if (trimmedUri.isEmpty()) {
          continue;
        }
        // Special handling for localhost URLs which are valid for development
        boolean isLocalhost =
            trimmedUri.startsWith("http://localhost") || trimmedUri.startsWith("https://localhost");

        if (!isLocalhost && !org.hisp.dhis.system.util.ValidationUtils.urlIsValid(trimmedUri)) {
          throw new ConflictException("Invalid redirect URI: " + trimmedUri);
        }
      }
    }
  }
}
