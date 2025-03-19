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
package org.hisp.dhis.eventhook.targets.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.hisp.dhis.common.auth.OAuth2ClientCredentialsAuthScheme;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

class OAuth2ClientCredentialsAuthSchemeTest extends AbstractAuthSchemeTest {

  @Test
  void testEncrypt() {
    assertEncrypt(
        new OAuth2ClientCredentialsAuthScheme()
            .setClientId("alice")
            .setClientSecret("passw0rd")
            .setTokenUri("foo"),
        OAuth2ClientCredentialsAuthScheme::getClientSecret);
  }

  @Test
  void testDecrypt() {
    assertDecrypt(
        new OAuth2ClientCredentialsAuthScheme()
            .setClientId("alice")
            .setClientSecret("3PB06m2bcr0blf81OEpcIDUMUYQYHJcdQsBJyOwbmelTYBQ6fuskAGJReGgM30Cv")
            .setTokenUri("foo"),
        OAuth2ClientCredentialsAuthScheme::getClientSecret);
  }

  @Test
  void testApplySavesAuthorizedClientWhenLoadedAuthorizedClientIsNull() throws Exception {
    OAuth2ClientCredentialsAuthScheme oAuth2ClientCredentialsAuthScheme =
        new OAuth2ClientCredentialsAuthScheme();
    oAuth2ClientCredentialsAuthScheme
        .setClientId("clientId")
        .setClientSecret("clientSecret")
        .setTokenUri("http://stub");

    ApplicationContext mockApplicationContext = mock(ApplicationContext.class);
    CountDownLatch saveAuthorizedClientMethodCountDownLatch = new CountDownLatch(2);
    when(mockApplicationContext.getBean(OAuth2AuthorizedClientRepository.class))
        .thenReturn(
            new OAuth2AuthorizedClientRepository() {
              @Override
              public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
                  String clientRegistrationId,
                  Authentication principal,
                  HttpServletRequest request) {
                return null;
              }

              @Override
              public void saveAuthorizedClient(
                  OAuth2AuthorizedClient authorizedClient,
                  Authentication principal,
                  HttpServletRequest request,
                  HttpServletResponse response) {
                saveAuthorizedClientMethodCountDownLatch.countDown();
                assertEquals(
                    oAuth2ClientCredentialsAuthScheme.getRegistrationId(),
                    authorizedClient.getClientRegistration().getRegistrationId());
                assertEquals(
                    oAuth2ClientCredentialsAuthScheme.getClientId(),
                    authorizedClient.getClientRegistration().getClientId());
                assertEquals(
                    oAuth2ClientCredentialsAuthScheme.getClientSecret(),
                    authorizedClient.getClientRegistration().getClientSecret());
                assertEquals(
                    oAuth2ClientCredentialsAuthScheme.getTokenUri(),
                    authorizedClient.getClientRegistration().getProviderDetails().getTokenUri());
              }

              @Override
              public void removeAuthorizedClient(
                  String clientRegistrationId,
                  Authentication principal,
                  HttpServletRequest request,
                  HttpServletResponse response) {}
            });
    when(mockApplicationContext.getBean(OAuth2AuthorizedClientProvider.class))
        .thenReturn(
            context -> {
              OAuth2AuthorizedClient mockOAuth2AuthorizedClient =
                  mock(OAuth2AuthorizedClient.class);
              when(mockOAuth2AuthorizedClient.getAccessToken())
                  .thenReturn(
                      new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "foo", null, null));
              when(mockOAuth2AuthorizedClient.getClientRegistration())
                  .thenReturn(
                      ClientRegistration.withRegistrationId(
                              context.getClientRegistration().getRegistrationId())
                          .clientId(context.getClientRegistration().getClientId())
                          .clientSecret(context.getClientRegistration().getClientSecret())
                          .tokenUri(
                              context.getClientRegistration().getProviderDetails().getTokenUri())
                          .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                          .build());
              return mockOAuth2AuthorizedClient;
            });

    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(null, null));
    Map<String, List<String>> headers = new HashMap<>();
    oAuth2ClientCredentialsAuthScheme.apply(mockApplicationContext, headers, new HashMap<>());
    assertEquals("Bearer foo", headers.get("Authorization").get(0));
    assertEquals(1, saveAuthorizedClientMethodCountDownLatch.getCount());
  }

  @Test
  void testApplyUsesAuthorizedClientWhenLoadedAuthorizedClientIsNotNull() throws Exception {
    OAuth2ClientCredentialsAuthScheme oAuth2ClientCredentialsAuthScheme =
        new OAuth2ClientCredentialsAuthScheme();
    oAuth2ClientCredentialsAuthScheme
        .setClientId("clientId")
        .setClientSecret("clientSecret")
        .setTokenUri("http://stub");

    ApplicationContext mockApplicationContext = mock(ApplicationContext.class);
    when(mockApplicationContext.getBean(OAuth2AuthorizedClientRepository.class))
        .thenReturn(
            new OAuth2AuthorizedClientRepository() {
              @Override
              public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
                  String clientRegistrationId,
                  Authentication principal,
                  HttpServletRequest request) {
                OAuth2AuthorizedClient mockOAuth2AuthorizedClient =
                    mock(OAuth2AuthorizedClient.class);
                when(mockOAuth2AuthorizedClient.getClientRegistration())
                    .thenReturn(
                        ClientRegistration.withRegistrationId(
                                oAuth2ClientCredentialsAuthScheme.getRegistrationId())
                            .clientId(oAuth2ClientCredentialsAuthScheme.getClientId())
                            .clientSecret(oAuth2ClientCredentialsAuthScheme.getClientSecret())
                            .tokenUri(oAuth2ClientCredentialsAuthScheme.getTokenUri())
                            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                            .build());
                return (T) mockOAuth2AuthorizedClient;
              }

              @Override
              public void saveAuthorizedClient(
                  OAuth2AuthorizedClient authorizedClient,
                  Authentication principal,
                  HttpServletRequest request,
                  HttpServletResponse response) {}

              @Override
              public void removeAuthorizedClient(
                  String clientRegistrationId,
                  Authentication principal,
                  HttpServletRequest request,
                  HttpServletResponse response) {}
            });
    when(mockApplicationContext.getBean(OAuth2AuthorizedClientManager.class))
        .thenReturn(
            authorizeRequest -> {
              ClientRegistration clientRegistration =
                  authorizeRequest.getAuthorizedClient().getClientRegistration();
              assertEquals(
                  oAuth2ClientCredentialsAuthScheme.getRegistrationId(),
                  clientRegistration.getRegistrationId());
              assertEquals(
                  oAuth2ClientCredentialsAuthScheme.getClientId(),
                  clientRegistration.getClientId());
              assertEquals(
                  oAuth2ClientCredentialsAuthScheme.getClientSecret(),
                  clientRegistration.getClientSecret());
              assertEquals(
                  oAuth2ClientCredentialsAuthScheme.getTokenUri(),
                  clientRegistration.getProviderDetails().getTokenUri());

              OAuth2AuthorizedClient mockOAuth2AuthorizedClient =
                  mock(OAuth2AuthorizedClient.class);
              when(mockOAuth2AuthorizedClient.getAccessToken())
                  .thenReturn(
                      new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "bar", null, null));

              return mockOAuth2AuthorizedClient;
            });

    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(null, null));
    Map<String, List<String>> headers = new HashMap<>();
    oAuth2ClientCredentialsAuthScheme.apply(mockApplicationContext, headers, new HashMap<>());
    assertEquals("Bearer bar", headers.get("Authorization").get(0));
  }
}
