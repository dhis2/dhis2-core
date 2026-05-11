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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.client.RestTemplate;

/**
 * End-to-end integration test for {@link SignedJwtUserInfoLoader} that wires the real {@link
 * RestTemplate}, real {@link JwkSourceCache} (which talks real HTTP to fetch the IdP's JWKS), and
 * real Nimbus JWT verification against an in-process HTTP server playing the part of an
 * eSignet-style IdP.
 *
 * <p>The IdP-stub uses the JDK's built-in {@link HttpServer} so the test has no extra third-party
 * dependency (no WireMock). The stub serves:
 *
 * <ul>
 *   <li>{@code GET /jwks.json} — returns the IdP's public {@link JWKSet}
 *   <li>{@code GET /userinfo} — requires a {@code Bearer} access token and returns a freshly signed
 *       userinfo JWT with {@code Content-Type: application/jwt}
 * </ul>
 *
 * Only {@link UserService} is mocked, since exercising it would require a Spring/Hibernate context.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignedJwtUserInfoLoaderHttpIntegrationTest {

  private static final String EXPECTED_BEARER = "at-value";

  @Mock private UserService userService;
  @Mock private OidcUserRequest userRequest;
  @Mock private OAuth2AccessToken accessToken;
  @Mock private OidcIdToken idToken;
  @Mock private ClientRegistration clientRegistration;
  @Mock private ClientRegistration.ProviderDetails providerDetails;
  @Mock private ClientRegistration.ProviderDetails.UserInfoEndpoint userInfoEndpoint;

  private HttpServer httpServer;
  private int port;
  private RSAKey idpSigningKey;
  private SignedJwtUserInfoLoader loader;
  private DhisOidcClientRegistration registration;

  /** Per-test bumped to make each registration id unique so {@link JwkSourceCache} isn't reused. */
  private static final AtomicInteger REG_SEQ = new AtomicInteger();

  @BeforeEach
  void startStubAndWire() throws Exception {
    idpSigningKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
    String publicJwks = new JWKSet(idpSigningKey.toPublicJWK()).toString();

    httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext("/jwks.json", ex -> respond(ex, 200, "application/json", publicJwks));
    httpServer.createContext("/userinfo", this::serveUserInfo);
    httpServer.setExecutor(null);
    httpServer.start();
    port = httpServer.getAddress().getPort();

    String regId = "esignet-" + REG_SEQ.incrementAndGet();
    when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
    when(userRequest.getAccessToken()).thenReturn(accessToken);
    when(userRequest.getIdToken()).thenReturn(idToken);
    when(accessToken.getTokenValue()).thenReturn(EXPECTED_BEARER);
    when(clientRegistration.getRegistrationId()).thenReturn(regId);
    when(clientRegistration.getProviderDetails()).thenReturn(providerDetails);
    when(providerDetails.getUserInfoEndpoint()).thenReturn(userInfoEndpoint);
    when(userInfoEndpoint.getUri()).thenReturn(baseUrl() + "/userinfo");
    when(providerDetails.getJwkSetUri()).thenReturn(baseUrl() + "/jwks.json");

    registration =
        DhisOidcClientRegistration.builder()
            .clientRegistration(clientRegistration)
            .mappingClaimKey("sub")
            .userInfoResponseType(UserInfoResponseType.JWT)
            .userInfoJwsAlgorithm(JWSAlgorithm.RS256)
            .build();

    loader = new SignedJwtUserInfoLoader(userService, new JwkSourceCache(), new RestTemplate());
  }

  @AfterEach
  void stopStub() {
    httpServer.stop(0);
  }

  @Test
  void happyPath_fetchesJwksAndUserInfoOverRealHttp_andResolvesDhisUser() {
    User user = new User();
    user.setExternalAuth(true);
    when(userService.getUserByOpenId("user-42")).thenReturn(user);
    when(userService.createUserDetails(user)).thenReturn(UserDetails.fromUser(user));

    nextResponseClaims = claims("user-42");
    nextResponseSigningKey = idpSigningKey;

    OidcUser result = loader.load(userRequest, registration);

    assertNotNull(result);
    assertEquals("user-42", result.getAttributes().get("sub"));
  }

  @Test
  void jwtSignedWithUnknownKey_isRejectedWithJwtProcessingError() throws Exception {
    nextResponseClaims = claims("user-42");
    nextResponseSigningKey = new RSAKeyGenerator(2048).keyID("rogue").generate();

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("jwt_processing_error", ex.getError().getErrorCode());
  }

  @Test
  void userInfoEndpointReturns500_isMappedToInvalidUserInfoResponse() {
    failNextWith = 500;

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("invalid_user_info_response", ex.getError().getErrorCode());
  }

  @Test
  void missingMappingClaim_isMappedToMissingMappingClaim() {
    nextResponseClaims = new JWTClaimsSet.Builder().issuer("idp").build();
    nextResponseSigningKey = idpSigningKey;

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("missing_mapping_claim", ex.getError().getErrorCode());
  }

  // ---------- HttpServer-stub plumbing ----------

  private JWTClaimsSet nextResponseClaims;
  private RSAKey nextResponseSigningKey;
  private Integer failNextWith;

  private void serveUserInfo(HttpExchange ex) throws IOException {
    String auth = ex.getRequestHeaders().getFirst("Authorization");
    if (auth == null || !auth.equals("Bearer " + EXPECTED_BEARER)) {
      respond(ex, 401, "text/plain", "unauthorized");
      return;
    }
    if (failNextWith != null) {
      respond(ex, failNextWith, "text/plain", "server error");
      return;
    }
    try {
      String jwt = signJwt(nextResponseClaims, nextResponseSigningKey);
      respond(ex, 200, "application/jwt", jwt);
    } catch (JOSEException e) {
      respond(ex, 500, "text/plain", "sign-failed: " + e.getMessage());
    }
  }

  private static void respond(HttpExchange ex, int status, String contentType, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().set("Content-Type", contentType);
    ex.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(bytes);
    }
  }

  private String baseUrl() {
    return "http://127.0.0.1:" + port;
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
