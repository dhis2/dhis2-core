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
package org.hisp.dhis.common.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.feedback.BadGatewayException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@Getter
@Setter
@Accessors(chain = true)
@Slf4j
public class OAuth2ClientCredentialsuthAuthScheme implements AuthScheme {
  public static final String OAUTH2_CLIENT_CREDENTIALS_TYPE = "oauth2-client-credentials";

  @JsonProperty(required = true)
  private String clientId;

  @JsonProperty(required = true, access = JsonProperty.Access.WRITE_ONLY)
  private String clientSecret;

  @JsonProperty(required = true)
  private String tokenUri;

  @Override
  public void apply(Map<String, List<String>> headers, Map<String, List<String>> queryParams)
      throws Exception {
    ClientRegistration clientRegistration =
        ClientRegistration.withRegistrationId("dhis2")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .tokenUri(tokenUri)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .build();
    OAuth2AuthorizationContext oAuth2AuthorizationContext =
        OAuth2AuthorizationContext.withClientRegistration(clientRegistration)
            .principal(SecurityContextHolder.getContext().getAuthentication())
            .build();

    OAuth2AuthorizedClient oAuth2AuthorizedClient;
    try {
      oAuth2AuthorizedClient =
          OAuth2AuthorizedClientProviderBuilder.builder()
              .clientCredentials()
              .build()
              .authorize(oAuth2AuthorizationContext);
    } catch (ClientAuthorizationException e) {
      log.error(e.getMessage(), e);
      throw new BadGatewayException(
          "An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response");
    }

    headers.put(
        "Authorization",
        List.of("Bearer " + oAuth2AuthorizedClient.getAccessToken().getTokenValue()));
  }

  @Override
  public AuthScheme encrypt(UnaryOperator<String> encryptFunc) {
    return copy(clientId, encryptFunc.apply(clientSecret), tokenUri);
  }

  @Override
  public AuthScheme decrypt(UnaryOperator<String> decryptFunc) {
    return copy(clientId, decryptFunc.apply(clientSecret), tokenUri);
  }

  @Override
  public String getType() {
    return OAUTH2_CLIENT_CREDENTIALS_TYPE;
  }

  protected OAuth2ClientCredentialsuthAuthScheme copy(
      String clientId, String clientSecret, String tokenUri) {
    OAuth2ClientCredentialsuthAuthScheme oAuth2ClientCredentialsuthAuthScheme =
        new OAuth2ClientCredentialsuthAuthScheme();
    oAuth2ClientCredentialsuthAuthScheme.setClientId(clientId);
    oAuth2ClientCredentialsuthAuthScheme.setClientSecret(clientSecret);
    oAuth2ClientCredentialsuthAuthScheme.setTokenUri(tokenUri);

    return oAuth2ClientCredentialsuthAuthScheme;
  }
}
