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
package org.hisp.dhis.security.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;

/**
 * Unit tests for {@link DhisAuthorizationCodeTokenResponseClient}, verifying that the {@code
 * private_key_jwt} client assertion (with a {@code jku} header pointing at the per-provider JWKS
 * endpoint) is added to the token request, and that the standard {@code client_secret_basic}
 * authentication method is unaffected.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class DhisAuthorizationCodeTokenResponseClientTest {

  private static final String REGISTRATION_ID = "test";

  private static final String CLIENT_ID = "test-client";

  private static final String REDIRECT_URI = "https://dhis2.example.org/oauth2/code/test";

  private static final String AUTHORIZATION_URI = "https://idp.example.org/authorize";

  private static final String JWK_SET_URL = "https://client.example.org/jwks.json";

  private static final String CODE = "test-code";

  private static final String STATE = "test-state";

  private static final String TOKEN_RESPONSE_BODY =
      "{\"access_token\":\"test-access-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}";

  @Mock private DhisOidcProviderRepository clientRegistrations;

  private HttpServer httpServer;

  private volatile String capturedRequestBody;

  private volatile Headers capturedRequestHeaders;

  private DhisAuthorizationCodeTokenResponseClient tokenResponseClient;

  @BeforeEach
  void setUp() throws IOException {
    httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    httpServer.createContext("/token", this::handleTokenRequest);
    httpServer.start();

    tokenResponseClient = new DhisAuthorizationCodeTokenResponseClient(clientRegistrations);
    tokenResponseClient.init();
  }

  @AfterEach
  void tearDown() {
    httpServer.stop(0);
  }

  private void handleTokenRequest(HttpExchange exchange) throws IOException {
    capturedRequestBody =
        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    capturedRequestHeaders = exchange.getRequestHeaders();

    byte[] responseBytes = TOKEN_RESPONSE_BODY.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, responseBytes.length);
    try (OutputStream responseBody = exchange.getResponseBody()) {
      responseBody.write(responseBytes);
    }
  }

  private String tokenUri() {
    return "http://localhost:" + httpServer.getAddress().getPort() + "/token";
  }

  private ClientRegistration.Builder baseRegistrationBuilder() {
    return ClientRegistration.withRegistrationId(REGISTRATION_ID)
        .clientId(CLIENT_ID)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri(REDIRECT_URI)
        .authorizationUri(AUTHORIZATION_URI)
        .tokenUri(tokenUri())
        .scope("openid");
  }

  private static OAuth2AuthorizationCodeGrantRequest grantRequest(ClientRegistration registration) {
    OAuth2AuthorizationRequest authorizationRequest =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri(AUTHORIZATION_URI)
            .clientId(registration.getClientId())
            .redirectUri(REDIRECT_URI)
            .state(STATE)
            .build();
    OAuth2AuthorizationResponse authorizationResponse =
        OAuth2AuthorizationResponse.success(CODE).redirectUri(REDIRECT_URI).state(STATE).build();
    OAuth2AuthorizationExchange authorizationExchange =
        new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);
    return new OAuth2AuthorizationCodeGrantRequest(registration, authorizationExchange);
  }

  private static RSAKey generateRsaJwk() throws NoSuchAlgorithmException {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
        .privateKey((RSAPrivateKey) keyPair.getPrivate())
        .keyID("test-key-id")
        .build();
  }

  private static String extractFormParam(String formBody, String name) {
    String prefix = name + "=";
    for (String pair : formBody.split("&")) {
      if (pair.startsWith(prefix)) {
        return URLDecoder.decode(pair.substring(prefix.length()), StandardCharsets.UTF_8);
      }
    }
    return null;
  }

  @Test
  void privateKeyJwtRequestCarriesClientAssertion() throws NoSuchAlgorithmException {
    // given
    RSAKey jwk = generateRsaJwk();
    DhisOidcClientRegistration dhisOidcClientRegistration =
        DhisOidcClientRegistration.builder().jwk(jwk).jwkSetUrl(JWK_SET_URL).build();
    when(clientRegistrations.getDhisOidcClientRegistration(REGISTRATION_ID))
        .thenReturn(dhisOidcClientRegistration);

    ClientRegistration registration =
        baseRegistrationBuilder()
            .clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT)
            .build();

    // when
    OAuth2AccessTokenResponse response =
        tokenResponseClient.getTokenResponse(grantRequest(registration));

    // then
    assertEquals("test-access-token", response.getAccessToken().getTokenValue());
    assertNotNull(capturedRequestBody);
    assertTrue(capturedRequestBody.contains("client_assertion="));
    assertTrue(
        capturedRequestBody.contains(
            "client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer"));
  }

  @Test
  void privateKeyJwtAssertionHeaderCarriesJku() throws Exception {
    // given
    RSAKey jwk = generateRsaJwk();
    DhisOidcClientRegistration dhisOidcClientRegistration =
        DhisOidcClientRegistration.builder().jwk(jwk).jwkSetUrl(JWK_SET_URL).build();
    when(clientRegistrations.getDhisOidcClientRegistration(REGISTRATION_ID))
        .thenReturn(dhisOidcClientRegistration);

    ClientRegistration registration =
        baseRegistrationBuilder()
            .clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT)
            .build();

    // when
    tokenResponseClient.getTokenResponse(grantRequest(registration));

    // then
    String clientAssertion = extractFormParam(capturedRequestBody, "client_assertion");
    assertNotNull(clientAssertion);
    SignedJWT signedJwt = SignedJWT.parse(clientAssertion);
    assertNotNull(signedJwt.getHeader().getAlgorithm());
    assertEquals(JWSAlgorithm.RS256, signedJwt.getHeader().getAlgorithm());
    assertNotNull(signedJwt.getHeader().getJWKURL());
    assertEquals(JWK_SET_URL, signedJwt.getHeader().getJWKURL().toString());
  }

  @Test
  void clientSecretBasicStillWorks() {
    // given
    ClientRegistration registration =
        baseRegistrationBuilder()
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientSecret("test-secret")
            .build();

    // when
    OAuth2AccessTokenResponse response =
        tokenResponseClient.getTokenResponse(grantRequest(registration));

    // then
    assertEquals("test-access-token", response.getAccessToken().getTokenValue());
    List<String> authorizationHeaders = capturedRequestHeaders.get("Authorization");
    assertNotNull(authorizationHeaders);
    assertTrue(authorizationHeaders.get(0).startsWith("Basic "));
    assertFalse(capturedRequestBody.contains("client_assertion"));
    verifyNoInteractions(clientRegistrations);
  }
}
