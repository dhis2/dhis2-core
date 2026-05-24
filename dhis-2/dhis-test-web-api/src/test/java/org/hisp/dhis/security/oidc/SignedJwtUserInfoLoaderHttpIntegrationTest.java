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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
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
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockserver.client.MockServerClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

/**
 * Integration test for {@link SignedJwtUserInfoLoader} that exercises real HTTP, real JWKS fetching
 * via {@link JwkSourceCache}, and real Nimbus JWT verification against a MockServer container
 * playing the part of an eSignet-style IdP.
 *
 * <p>The MockServer stub serves:
 *
 * <ul>
 *   <li>{@code GET /jwks.json} - the IdP's public JWK set
 *   <li>{@code GET /userinfo} - a signed userinfo JWT with {@code Content-Type: application/jwt}
 * </ul>
 *
 * Only {@link UserService} is mocked since exercising it would require a Spring/Hibernate context.
 *
 * @author Morten Svanaes <msvanaes@dhis2.org>
 */
@Tag("integration")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignedJwtUserInfoLoaderHttpIntegrationTest {

  private static final String EXPECTED_BEARER = "at-value";
  private static final AtomicInteger REG_SEQ = new AtomicInteger();

  private static GenericContainer<?> mockServerContainer;
  private static MockServerClient mockServerClient;

  @Mock private UserService userService;
  @Mock private OidcUserRequest userRequest;
  @Mock private OAuth2AccessToken accessToken;
  @Mock private OidcIdToken idToken;
  @Mock private ClientRegistration clientRegistration;
  @Mock private ClientRegistration.ProviderDetails providerDetails;
  @Mock private ClientRegistration.ProviderDetails.UserInfoEndpoint userInfoEndpoint;

  private RSAKey idpSigningKey;
  private SignedJwtUserInfoLoader loader;
  private DhisOidcClientRegistration registration;

  @BeforeAll
  static void startMockServer() {
    mockServerContainer =
        new GenericContainer<>("mockserver/mockserver:5.15.0")
            .waitingFor(new HttpWaitStrategy().forStatusCode(404))
            .withExposedPorts(1080);
    mockServerContainer.start();
    mockServerClient =
        new MockServerClient("localhost", mockServerContainer.getFirstMappedPort());
  }

  @AfterAll
  static void stopMockServer() {
    mockServerContainer.stop();
  }

  @BeforeEach
  void setUp() throws Exception {
    mockServerClient.reset();

    idpSigningKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
    String publicJwks = new JWKSet(idpSigningKey.toPublicJWK()).toString();
    String baseUrl = "http://localhost:" + mockServerContainer.getFirstMappedPort();

    mockServerClient
        .when(request().withPath("/jwks.json"))
        .respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(publicJwks));

    String regId = "esignet-" + REG_SEQ.incrementAndGet();
    when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
    when(userRequest.getAccessToken()).thenReturn(accessToken);
    when(userRequest.getIdToken()).thenReturn(idToken);
    when(accessToken.getTokenValue()).thenReturn(EXPECTED_BEARER);
    when(clientRegistration.getRegistrationId()).thenReturn(regId);
    when(clientRegistration.getProviderDetails()).thenReturn(providerDetails);
    when(providerDetails.getUserInfoEndpoint()).thenReturn(userInfoEndpoint);
    when(userInfoEndpoint.getUri()).thenReturn(baseUrl + "/userinfo");
    when(providerDetails.getJwkSetUri()).thenReturn(baseUrl + "/jwks.json");

    registration =
        DhisOidcClientRegistration.builder()
            .clientRegistration(clientRegistration)
            .mappingClaimKey("sub")
            .userInfoResponseType(UserInfoResponseType.JWT)
            .userInfoJwsAlgorithm(JWSAlgorithm.RS256)
            .build();

    loader = new SignedJwtUserInfoLoader(userService, new JwkSourceCache());
  }

  @Test
  void happyPath_fetchesJwksAndUserInfoOverRealHttp_andResolvesDhisUser() throws Exception {
    User user = new User();
    user.setExternalAuth(true);
    when(userService.getUserByOpenId("user-42")).thenReturn(user);
    when(userService.createUserDetails(user)).thenReturn(UserDetails.fromUser(user));

    mockServerClient
        .when(
            request()
                .withPath("/userinfo")
                .withHeader("Authorization", "Bearer " + EXPECTED_BEARER))
        .respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/jwt")
                .withBody(signJwt(claims("user-42"), idpSigningKey)));

    OidcUser result = loader.load(userRequest, registration);

    assertNotNull(result);
    assertEquals("user-42", result.getAttributes().get("sub"));
  }

  @Test
  void jwtSignedWithUnknownKey_isRejectedWithJwtProcessingError() throws Exception {
    RSAKey rogueKey = new RSAKeyGenerator(2048).keyID("rogue").generate();

    mockServerClient
        .when(
            request()
                .withPath("/userinfo")
                .withHeader("Authorization", "Bearer " + EXPECTED_BEARER))
        .respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/jwt")
                .withBody(signJwt(claims("user-42"), rogueKey)));

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("jwt_processing_error", ex.getError().getErrorCode());
  }

  @Test
  void userInfoEndpointReturns500_isMappedToInvalidUserInfoResponse() {
    mockServerClient
        .when(request().withPath("/userinfo"))
        .respond(response().withStatusCode(500).withBody("server error"));

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("invalid_user_info_response", ex.getError().getErrorCode());
  }

  @Test
  void missingMappingClaim_isMappedToMissingMappingClaim() throws Exception {
    JWTClaimsSet noSub = new JWTClaimsSet.Builder().issuer("idp").build();

    mockServerClient
        .when(
            request()
                .withPath("/userinfo")
                .withHeader("Authorization", "Bearer " + EXPECTED_BEARER))
        .respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/jwt")
                .withBody(signJwt(noSub, idpSigningKey)));

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("missing_mapping_claim", ex.getError().getErrorCode());
  }

  private static JWTClaimsSet claims(String sub) {
    return new JWTClaimsSet.Builder()
        .subject(sub)
        .issuer("idp")
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(Instant.now().plusSeconds(60)))
        .build();
  }

  private static String signJwt(JWTClaimsSet claims, RSAKey signingKey) throws JOSEException {
    SignedJWT signed =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
    signed.sign(new RSASSASigner(signingKey));
    return signed.serialize();
  }
}
