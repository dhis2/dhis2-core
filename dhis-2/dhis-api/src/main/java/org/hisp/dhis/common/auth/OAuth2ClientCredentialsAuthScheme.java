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
package org.hisp.dhis.common.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@Getter
@Setter
@Accessors(chain = true)
@Slf4j
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OAuth2ClientCredentialsAuthScheme implements AuthScheme {
  public static final String OAUTH2_CLIENT_CREDENTIALS_TYPE = "oauth2-client-credentials";

  public static final Authentication ANONYMOUS_AUTHENTICATION =
      new AbstractAuthenticationToken(null) {
        @Override
        public Object getCredentials() {
          return "";
        }

        @Override
        public Object getPrincipal() {
          return "anonymous";
        }

        @Override
        public boolean isAuthenticated() {
          return true;
        }
      };

  @JsonProperty(required = true)
  private String clientId;

  @JsonProperty(required = true, access = JsonProperty.Access.WRITE_ONLY)
  private String clientSecret;

  @JsonProperty(required = true)
  private String tokenUri;

  @Override
  public void apply(
      ApplicationContext applicationContext,
      Map<String, List<String>> headers,
      Map<String, List<String>> queryParams)
      throws Exception {

    final OAuth2AuthorizedClient newOAuth2AuthorizedClient;
    final String registrationId = getRegistrationId();
    final OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository =
        applicationContext.getBean(OAuth2AuthorizedClientRepository.class);
    final OAuth2AuthorizedClient loadedOAuth2AuthorizedClient =
        oAuth2AuthorizedClientRepository.loadAuthorizedClient(
            registrationId, ANONYMOUS_AUTHENTICATION, null);

    if (loadedOAuth2AuthorizedClient == null) {
      final ClientRegistration clientRegistration =
          ClientRegistration.withRegistrationId(registrationId)
              .clientId(clientId)
              .clientSecret(clientSecret)
              .tokenUri(tokenUri)
              .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
              .build();
      final OAuth2AuthorizationContext oAuth2AuthorizationContext =
          OAuth2AuthorizationContext.withClientRegistration(clientRegistration)
              .principal(ANONYMOUS_AUTHENTICATION)
              .build();

      final OAuth2AuthorizedClientProvider oAuth2AuthorizedClientProvider =
          applicationContext.getBean(OAuth2AuthorizedClientProvider.class);
      newOAuth2AuthorizedClient =
          oAuth2AuthorizedClientProvider.authorize(oAuth2AuthorizationContext);
      oAuth2AuthorizedClientRepository.saveAuthorizedClient(
          newOAuth2AuthorizedClient, ANONYMOUS_AUTHENTICATION, null, null);
    } else {
      final OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager =
          applicationContext.getBean(OAuth2AuthorizedClientManager.class);
      newOAuth2AuthorizedClient =
          oAuth2AuthorizedClientManager.authorize(
              OAuth2AuthorizeRequest.withAuthorizedClient(loadedOAuth2AuthorizedClient)
                  .principal(ANONYMOUS_AUTHENTICATION)
                  .build());
    }

    headers.put(
        "Authorization",
        List.of("Bearer " + newOAuth2AuthorizedClient.getAccessToken().getTokenValue()));
  }

  @Override
  public AuthScheme encrypt(UnaryOperator<String> encryptFunc) {
    return this.toBuilder().clientSecret(encryptFunc.apply(clientSecret)).build();
  }

  @Override
  public AuthScheme decrypt(UnaryOperator<String> decryptFunc) {
    return this.toBuilder().clientSecret(decryptFunc.apply(clientSecret)).build();
  }

  public String getRegistrationId() {
    return clientId + ":" + tokenUri;
  }

  @Override
  public String getType() {
    return OAUTH2_CLIENT_CREDENTIALS_TYPE;
  }
}
