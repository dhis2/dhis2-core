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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for {@link SignedJwtUserInfoLoader}. The IdP's UserInfo endpoint is replaced by a
 * {@link MockRestServiceServer} and its JWKS by an in-memory key set.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class SignedJwtUserInfoLoaderTest {

  private static final String ISSUER = "https://idp.test";

  private static final String USER_INFO_URI = ISSUER + "/userinfo";

  private static final String JWK_SET_URI = ISSUER + "/jwks";

  private static final String CLIENT_ID = "dhis2-client";

  private static final String SUBJECT = "psut-4711";

  private static final MediaType APPLICATION_JWT = new MediaType("application", "jwt");

  @Mock private JwkSourceCache jwkSourceCache;

  private RSAKey idpSigningKey;

  private DhisOidcClientRegistration registration;

  private MockRestServiceServer idp;

  private SignedJwtUserInfoLoader loader;

  @BeforeEach
  void setUp() throws JOSEException {
    idpSigningKey = new RSAKeyGenerator(2048).keyID("idp-key").generate();
    // keys of the IdP's jwk_uri; never fetched when the UserInfo request itself fails
    lenient()
        .when(jwkSourceCache.get("esignet", JWK_SET_URI))
        .thenReturn(new ImmutableJWKSet<>(new JWKSet(idpSigningKey.toPublicJWK())));

    registration =
        DhisOidcClientRegistration.builder()
            .clientRegistration(
                ClientRegistration.withRegistrationId("esignet")
                    .clientId(CLIENT_ID)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/oauth2/code/{registrationId}")
                    .scope("openid", "profile")
                    .authorizationUri(ISSUER + "/authorize")
                    .tokenUri(ISSUER + "/token")
                    .jwkSetUri(JWK_SET_URI)
                    .userInfoUri(USER_INFO_URI)
                    .userNameAttributeName(IdTokenClaimNames.SUB)
                    .build())
            .mappingClaimKey("email")
            .userInfoResponseType(UserInfoResponseType.JWT)
            .userInfoJwsAlgorithm(JWSAlgorithm.RS256)
            .build();

    RestClient.Builder restClientBuilder = RestClient.builder();
    idp = MockRestServiceServer.bindTo(restClientBuilder).build();
    loader = new SignedJwtUserInfoLoader(jwkSourceCache, restClientBuilder.build());
  }

  @Test
  void loadUserCombinesVerifiedUserInfoWithIdToken() throws JOSEException {
    idp.expect(requestTo(USER_INFO_URI))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
        .andExpect(header(HttpHeaders.ACCEPT, APPLICATION_JWT.toString()))
        .andRespond(withSuccess(sign(userInfo().build(), idpSigningKey), APPLICATION_JWT));
    OidcUserRequest userRequest = userRequest();

    OidcUser oidcUser = loader.loadUser(userRequest, registration);

    idp.verify();
    assertEquals("user@dhis2.org", oidcUser.getUserInfo().getEmail());
    assertEquals("user@dhis2.org", oidcUser.getAttributes().get("email"));
    assertEquals(SUBJECT, oidcUser.getName());
    assertSame(userRequest.getIdToken(), oidcUser.getIdToken());
  }

  @Test
  void loadUserAcceptsIssuerAndAudienceOfThisLogin() throws JOSEException {
    respondWith(
        sign(
            userInfo().issuer(ISSUER).audience(List.of(CLIENT_ID, "other-client")).build(),
            idpSigningKey));

    OidcUser oidcUser = loader.loadUser(userRequest(), registration);

    assertEquals(SUBJECT, oidcUser.getName());
  }

  static Stream<Arguments> userInfoNotAboutThisLogin() {
    return Stream.of(
        arguments("no sub", new JWTClaimsSet.Builder().claim("email", "user@dhis2.org").build()),
        arguments("sub of another user", userInfo().subject("psut-other").build()),
        arguments("iss of another IdP", userInfo().issuer("https://other-idp.test").build()),
        arguments("aud of another client", userInfo().audience("other-client").build()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("userInfoNotAboutThisLogin")
  void loadUserRejectsUserInfoNotAboutThisLogin(String reason, JWTClaimsSet claims)
      throws JOSEException {
    respondWith(sign(claims, idpSigningKey));

    assertLoginFails("invalid_user_info_response");
  }

  @Test
  void loadUserRejectsUserInfoSignedWithKeyOutsideIdpJwks() throws JOSEException {
    RSAKey rogueKey = new RSAKeyGenerator(2048).keyID(idpSigningKey.getKeyID()).generate();
    respondWith(sign(userInfo().build(), rogueKey));

    assertLoginFails("jwt_processing_error");
  }

  @Test
  void loadUserFailsWhenUserInfoEndpointFails() {
    idp.expect(requestTo(USER_INFO_URI)).andRespond(withServerError());

    assertLoginFails("invalid_user_info_response");
  }

  private void assertLoginFails(String expectedErrorCode) {
    OidcUserRequest userRequest = userRequest();
    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.loadUser(userRequest, registration));
    assertEquals(expectedErrorCode, ex.getError().getErrorCode());
  }

  private void respondWith(String jwt) {
    idp.expect(requestTo(USER_INFO_URI)).andRespond(withSuccess(jwt, APPLICATION_JWT));
  }

  private OidcUserRequest userRequest() {
    Instant now = Instant.now();
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token",
            now,
            now.plusSeconds(60),
            Set.of("openid", "profile"));
    OidcIdToken idToken =
        OidcIdToken.withTokenValue("id-token")
            .issuer(ISSUER)
            .subject(SUBJECT)
            .audience(List.of(CLIENT_ID))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .build();
    return new OidcUserRequest(registration.getClientRegistration(), accessToken, idToken);
  }

  /**
   * eSignet-shaped UserInfo: {@code sub} and the consented claims, no {@code iss} or {@code aud}.
   */
  private static JWTClaimsSet.Builder userInfo() {
    return new JWTClaimsSet.Builder().subject(SUBJECT).claim("email", "user@dhis2.org");
  }

  private static String sign(JWTClaimsSet claims, RSAKey signingKey) throws JOSEException {
    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
    jwt.sign(new RSASSASigner(signingKey));
    return jwt.serialize();
  }
}
