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
package org.hisp.dhis.webapi.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.security.oidc.DhisOidcUserService;
import org.hisp.dhis.security.oidc.JwkSourceCache;
import org.hisp.dhis.security.oidc.SignedJwtUserInfoLoader;
import org.hisp.dhis.security.oidc.UserInfoResponseType;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

/**
 * Integration test for OIDC login against an IdP whose UserInfo endpoint returns a signed JWT
 * ({@code user_info_response_type=jwt}, as MOSIP eSignet does). A MockServer container plays the
 * IdP's JWKS and UserInfo endpoints and the DHIS2 users live in Postgres, so {@link
 * DhisOidcUserService} runs with the real {@link SignedJwtUserInfoLoader} bean: the HTTP call, the
 * JWKS retrieval through {@link JwkSourceCache}, the signature verification and the DHIS2 user
 * lookup.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class DhisOidcUserServiceJwtUserInfoTest extends PostgresControllerIntegrationTestBase {

  private static final String CLIENT_ID = "dhis2-client";

  private static final String ACCESS_TOKEN = "access-token";

  private static final String SUBJECT = "psut-4711";

  private static final String EMAIL = "esignet.user@dhis2.org";

  /** {@link JwkSourceCache} caches the IdP keys per registration id, so each test uses its own. */
  private static final AtomicInteger REGISTRATION_SEQUENCE = new AtomicInteger();

  private static GenericContainer<?> idpContainer;

  private static MockServerClient idp;

  @Autowired private SignedJwtUserInfoLoader signedJwtUserInfoLoader;

  private RSAKey idpSigningKey;

  private ClientRegistration clientRegistration;

  private DhisOidcUserService dhisOidcUserService;

  @BeforeAll
  static void startIdp() {
    idpContainer =
        new GenericContainer<>("mockserver/mockserver:5.15.0")
            .waitingFor(new HttpWaitStrategy().forStatusCode(404))
            .withExposedPorts(1080);
    idpContainer.start();
    idp = new MockServerClient("localhost", idpContainer.getFirstMappedPort());
  }

  @AfterAll
  static void stopIdp() {
    idpContainer.stop();
  }

  @BeforeEach
  void setUp() throws JOSEException {
    idp.reset();

    idpSigningKey = new RSAKeyGenerator(2048).keyID("idp-key").generate();
    idp.when(request().withMethod("GET").withPath("/jwks.json"))
        .respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(new JWKSet(idpSigningKey.toPublicJWK()).toString()));

    String idpUrl = "http://localhost:" + idpContainer.getFirstMappedPort();
    clientRegistration =
        ClientRegistration.withRegistrationId("esignet" + REGISTRATION_SEQUENCE.incrementAndGet())
            .clientId(CLIENT_ID)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri(idpUrl + "/authorize")
            .tokenUri(idpUrl + "/token")
            .jwkSetUri(idpUrl + "/jwks.json")
            .issuerUri(idpUrl)
            .userInfoUri(idpUrl + "/userinfo")
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .build();

    DhisOidcProviderRepository providers = new DhisOidcProviderRepository();
    providers.addRegistration(
        DhisOidcClientRegistration.builder()
            .clientRegistration(clientRegistration)
            .mappingClaimKey("email")
            .userInfoResponseType(UserInfoResponseType.JWT)
            .userInfoJwsAlgorithm(JWSAlgorithm.RS256)
            .build());

    dhisOidcUserService = new DhisOidcUserService(userService, providers, signedJwtUserInfoLoader);
  }

  @Test
  void loadUserMapsSignedUserInfoToDhisUser() throws JOSEException {
    User user = createOpenIDUser("esignetuser", EMAIL);
    idpRespondsWithUserInfo(signedUserInfo(idpSigningKey));

    OidcUser oidcUser = dhisOidcUserService.loadUser(userRequest());

    DhisOidcUser dhisOidcUser = assertInstanceOf(DhisOidcUser.class, oidcUser);
    assertEquals(user.getUsername(), dhisOidcUser.getUsername());
    assertEquals(SUBJECT, dhisOidcUser.getName());
  }

  @Test
  void loadUserRejectsUserInfoSignedWithKeyOutsideIdpJwks() throws JOSEException {
    // a matching DHIS2 user exists, so only the signature check can stop the login
    createOpenIDUser("esignetuser", EMAIL);
    RSAKey rogueKey = new RSAKeyGenerator(2048).keyID(idpSigningKey.getKeyID()).generate();
    idpRespondsWithUserInfo(signedUserInfo(rogueKey));

    assertLoginFails("jwt_processing_error");
  }

  @Test
  void loadUserFailsWhenUserInfoEndpointFails() {
    idp.when(request().withMethod("GET").withPath("/userinfo"))
        .respond(response().withStatusCode(500));

    assertLoginFails("invalid_user_info_response");
  }

  private void assertLoginFails(String expectedErrorCode) {
    OidcUserRequest userRequest = userRequest();
    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> dhisOidcUserService.loadUser(userRequest));
    assertEquals(expectedErrorCode, ex.getError().getErrorCode());
  }

  /** Answers only requests carrying the access token and asking for a JWT, as eSignet expects. */
  private void idpRespondsWithUserInfo(String jwt) {
    idp.when(
            request()
                .withMethod("GET")
                .withPath("/userinfo")
                .withHeader("Authorization", "Bearer " + ACCESS_TOKEN)
                .withHeader("Accept", "application/jwt"))
        .respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/jwt")
                .withBody(jwt));
  }

  private OidcUserRequest userRequest() {
    Instant now = Instant.now();
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            ACCESS_TOKEN,
            now,
            now.plusSeconds(60),
            Set.of("openid", "profile", "email"));
    OidcIdToken idToken =
        OidcIdToken.withTokenValue("id-token")
            .issuer(clientRegistration.getProviderDetails().getIssuerUri())
            .subject(SUBJECT)
            .audience(List.of(CLIENT_ID))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .build();
    return new OidcUserRequest(clientRegistration, accessToken, idToken);
  }

  /**
   * eSignet-shaped UserInfo: {@code sub} and the consented claims, no {@code iss} or {@code aud}.
   */
  private static String signedUserInfo(RSAKey signingKey) throws JOSEException {
    JWTClaimsSet claims = new JWTClaimsSet.Builder().subject(SUBJECT).claim("email", EMAIL).build();
    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
    jwt.sign(new RSASSASigner(signingKey));
    return jwt.serialize();
  }
}
