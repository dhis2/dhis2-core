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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    String registrationId = getRegistrationId();
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository =
        applicationContext.getBean(OAuth2AuthorizedClientRepository.class);
    OAuth2AuthorizedClient oAuth2AuthorizedClient =
        oAuth2AuthorizedClientRepository.loadAuthorizedClient(registrationId, authentication, null);

    if (oAuth2AuthorizedClient == null) {
      ClientRegistration clientRegistration =
          ClientRegistration.withRegistrationId(registrationId)
              .clientId(clientId)
              .clientSecret(clientSecret)
              .tokenUri(tokenUri)
              .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
              .build();
      OAuth2AuthorizationContext oAuth2AuthorizationContext =
          OAuth2AuthorizationContext.withClientRegistration(clientRegistration)
              .principal(authentication)
              .build();

      OAuth2AuthorizedClientProvider oAuth2AuthorizedClientProvider =
          applicationContext.getBean(OAuth2AuthorizedClientProvider.class);
      oAuth2AuthorizedClient = oAuth2AuthorizedClientProvider.authorize(oAuth2AuthorizationContext);
      oAuth2AuthorizedClientRepository.saveAuthorizedClient(
          oAuth2AuthorizedClient, authentication, null, null);
    } else {
      OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager =
          applicationContext.getBean(OAuth2AuthorizedClientManager.class);
      oAuth2AuthorizedClient =
          oAuth2AuthorizedClientManager.authorize(
              OAuth2AuthorizeRequest.withAuthorizedClient(oAuth2AuthorizedClient)
                  .principal(authentication)
                  .build());
    }

    headers.put(
        "Authorization",
        List.of("Bearer " + oAuth2AuthorizedClient.getAccessToken().getTokenValue()));
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
