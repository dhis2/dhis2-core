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
package org.hisp.dhis.webapi.controller.security;

import static org.hisp.dhis.security.oauth2.dcr.OAuth2DcrService.createIaToken;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.security.jwt.Dhis2JwtAuthenticationManagerResolver;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2AuthorizationService;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2AuthorizationServiceImpl;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.security.oauth2.dcr.OAuth2DcrService;
import org.hisp.dhis.security.oauth2.dcr.OAuth2DcrService.IatPair;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.webapi.ControllerWithJwtTokenAuthTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tests for Dynamic Client Registration (DCR) with JWKS provided inline in the registration
 * request.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ActiveProfiles("oauth2-authorization-server-test")
class DcrControllerTest extends ControllerWithJwtTokenAuthTestBase {

  @Autowired private SystemSettingsService systemSettingsService;
  @Autowired private Dhis2OAuth2ClientService oAuth2ClientService;
  @Autowired private Dhis2OAuth2AuthorizationService dhis2OAuth2AuthorizationService;
  @Autowired private AuthorizationServerSettings authorizationServerSettings;
  @Autowired private Dhis2JwtAuthenticationManagerResolver dhis2JwtAuthenticationManagerResolver;
  @Autowired private JWKSource<SecurityContext> jwkSource;
  @Autowired private JwtDecoder jwtDecoder;
  @Autowired private OAuth2DcrService oAuth2DcrService;

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeAll
  static void init() {
    // Configure Jackson mapper with required modules
    ClassLoader classLoader = Dhis2OAuth2AuthorizationServiceImpl.class.getClassLoader();
    List<com.fasterxml.jackson.databind.Module> securityModules =
        SecurityJackson2Modules.getModules(classLoader);
    objectMapper.registerModules(securityModules);
    objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
    objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @BeforeEach
  void beforeEach() {
    dhis2JwtAuthenticationManagerResolver.setJwtDecoder(jwtDecoder);
  }

  @Test
  @DisplayName("Test dynamic client registration with inline JWKS")
  void testRegisterClientWithInlineJwks() throws Exception {
    // Given an initial access token (iat)
    String initialAccessToken = createClientAndIat();

    // Given a key pair to be used for the client's private_key_jwt authentication
    KeyPair keyPair = createKeys();

    // Given a client registration request with the iat and inline JWKS
    String clientId = doClientRegistrationRequest(initialAccessToken, keyPair);
    RegisteredClient client = oAuth2ClientService.findByClientId(clientId);
    assertNotNull(client);
    assertEquals(
        "private_key_jwt",
        client.getClientAuthenticationMethods().stream().findFirst().get().getValue());
    ClientSettings clientSettings = client.getClientSettings();
    assertNotNull(clientSettings.getSetting("client.inline.jwks"));
    assertNull(client.getClientSecret());
    // DCR-registered clients are first-party (Android) and must not require consent
    assertEquals(false, clientSettings.isRequireAuthorizationConsent());

    // When calling token endpoint with private_key_jwt authentication
    String tokenResponse = callTokenEndpoint(keyPair, clientId);
    String accessToken = JsonValue.of(tokenResponse).asObject().getString("access_token").string();
    assertNotNull(accessToken);

    // Then use the access token to make a request to /api/users
    String usersResp =
        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertNotNull(usersResp);
  }

  @Test
  @DisplayName("Test DCR-registered client refresh token settings")
  void testDcrRegisteredClientRefreshTokenSettings() throws Exception {
    // Given an initial access token (iat)
    String initialAccessToken = createClientAndIat();

    // Given a key pair to be used for the client's private_key_jwt authentication
    KeyPair keyPair = createKeys();

    // When registering a client
    String clientId = doClientRegistrationRequest(initialAccessToken, keyPair);
    RegisteredClient client = oAuth2ClientService.findByClientId(clientId);
    assertNotNull(client);

    // Then the refresh token TTL is the oauth2.server.dcr.refresh-token-ttl default (30 days),
    // not the SAS framework default of 60 minutes
    TokenSettings tokenSettings = client.getTokenSettings();
    assertEquals(Duration.ofDays(30), tokenSettings.getRefreshTokenTimeToLive());

    // Then refresh tokens are rotated on every use (OAuth 2.1 requirement for public clients),
    // making the TTL a sliding window instead of a hard wall after the initial login
    assertFalse(tokenSettings.isReuseRefreshTokens());

    // Then the id-token signature algorithm set by the SAS delegate converter is preserved
    assertEquals(SignatureAlgorithm.RS256, tokenSettings.getIdTokenSignatureAlgorithm());
  }

  @Test
  @DisplayName("Test refresh token rotation and sliding window expiry")
  void testRefreshTokenRotationAndSlidingWindow() throws Exception {
    // Given a DCR-registered client with the refresh_token grant
    KeyPair keyPair = createKeys();
    String clientId = registerRefreshCapableClient(keyPair);
    RegisteredClient client = oAuth2ClientService.findByClientId(clientId);

    // Given a persisted authorization holding a valid refresh token, as after a completed
    // authorization_code flow
    String initialRefreshToken = "rotation-initial-refresh-token";
    saveAuthorizationWithRefreshToken(
        client,
        "authzRot001",
        initialRefreshToken,
        Instant.now(),
        Instant.now().plus(Duration.ofDays(30)));

    // When refreshing with the valid refresh token, then new tokens are issued
    String response =
        callRefreshTokenEndpoint(keyPair, clientId, initialRefreshToken)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.refresh_token").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Then the refresh token is rotated: the response contains a NEW refresh token
    String rotatedRefreshToken =
        ((JsonObject) JsonValue.of(response)).getString("refresh_token").string();
    assertNotEquals(initialRefreshToken, rotatedRefreshToken);

    // Then the rotated refresh token has a fresh 30 day expiry (sliding window), enforced from
    // the persisted refresh_token_expires_at
    OAuth2Authorization refreshed =
        dhis2OAuth2AuthorizationService.findByToken(
            rotatedRefreshToken, OAuth2TokenType.REFRESH_TOKEN);
    assertNotNull(refreshed);
    Instant rotatedExpiresAt = refreshed.getRefreshToken().getToken().getExpiresAt();
    assertNotNull(rotatedExpiresAt);
    assertTrue(
        rotatedExpiresAt.isAfter(Instant.now().plus(Duration.ofDays(29)))
            && rotatedExpiresAt.isBefore(Instant.now().plus(Duration.ofDays(31))),
        "rotated refresh token must expire ~30 days from now, was: " + rotatedExpiresAt);

    // Then replaying the superseded refresh token is rejected (rotation invalidates it)
    callRefreshTokenEndpoint(keyPair, clientId, initialRefreshToken)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"));
  }

  @Test
  @DisplayName("Test expired refresh token is rejected")
  void testExpiredRefreshTokenRejected() throws Exception {
    // Given a DCR-registered client with the refresh_token grant
    KeyPair keyPair = createKeys();
    String clientId = registerRefreshCapableClient(keyPair);
    RegisteredClient client = oAuth2ClientService.findByClientId(clientId);

    // Given a persisted authorization whose refresh token expired one hour ago. Expiry is
    // enforced against the persisted refresh_token_expires_at, so backdating it simulates the
    // passage of time without mocking any clock.
    String expiredRefreshToken = "backdated-expired-refresh-token";
    saveAuthorizationWithRefreshToken(
        client,
        "authzExp001",
        expiredRefreshToken,
        Instant.now().minus(Duration.ofDays(31)),
        Instant.now().minus(Duration.ofHours(1)));

    // When refreshing with the expired token, then the grant is rejected
    callRefreshTokenEndpoint(keyPair, clientId, expiredRefreshToken)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"));
  }

  /** Registers a client with the authorization_code and refresh_token grants via DCR. */
  private String registerRefreshCapableClient(KeyPair keyPair) throws Exception {
    String initialAccessToken = createClientAndIat();
    MockHttpServletRequestBuilder registration =
        getGetClientRegPost(
            initialAccessToken, keyPair, "[\"authorization_code\", \"refresh_token\"]");
    String response =
        mvc.perform(registration)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((JsonObject) JsonValue.of(response)).getString("client_id").string();
  }

  /**
   * Persists an {@link OAuth2Authorization} holding a refresh token with the given lifetime, as it
   * would exist after a completed authorization_code flow.
   */
  private void saveAuthorizationWithRefreshToken(
      RegisteredClient client,
      String uid,
      String refreshTokenValue,
      Instant issuedAt,
      Instant expiresAt) {
    // MockMvc requests clear the thread's security context; the authorization store needs a
    // current user for auditing
    injectAdminIntoSecurityContext();
    OAuth2RefreshToken refreshToken =
        new OAuth2RefreshToken(refreshTokenValue, issuedAt, expiresAt);
    UsernamePasswordAuthenticationToken principal =
        UsernamePasswordAuthenticationToken.authenticated("admin", null, List.of());
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(client)
            // the service persists a non-UUID id verbatim as the 11-char uid column value;
            // UUID-shaped ids would be remapped to a generated uid instead
            .id(uid)
            .principalName("admin")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of("username"))
            .attribute(Principal.class.getName(), principal)
            .refreshToken(refreshToken)
            .build();
    dhis2OAuth2AuthorizationService.save(authorization);
  }

  private ResultActions callRefreshTokenEndpoint(
      KeyPair keyPair, String clientId, String refreshToken) throws Exception {
    return mvc.perform(
        post("/oauth2/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("client_id", clientId)
            .param(
                "client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
            .param("grant_type", "refresh_token")
            .param("refresh_token", refreshToken)
            .param("client_assertion", createClientAssertion(keyPair, clientId)));
  }

  @Test
  @DisplayName("Test iat can only be used once ")
  void testIatCanOnlyBeUsedOnce() throws Exception {
    // Given an initial access token (iat)
    String initialAccessToken = createClientAndIat();

    // Given a key pair to be used for the client's private_key_jwt authentication
    KeyPair keyPair = createKeys();

    // When calling client registration endpoint with the iat and inline JWKS
    String clientId = doClientRegistrationRequest(initialAccessToken, keyPair);
    RegisteredClient client = oAuth2ClientService.findByClientId(clientId);
    assertNotNull(client);
    assertEquals(
        "private_key_jwt",
        client.getClientAuthenticationMethods().stream().findFirst().get().getValue());
    ClientSettings clientSettings = client.getClientSettings();
    assertNotNull(clientSettings.getSetting("client.inline.jwks"));
    assertNull(client.getClientSecret());

    // Then expect 401 Unauthorized when called a second time with the same iat
    MockHttpServletRequestBuilder getClientRegPost =
        getGetClientRegPost(initialAccessToken, keyPair);
    mvc.perform(getClientRegPost).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Test enroll endpoint returns iat and redirects")
  void testEnrollEndpointReturnsIatAndRedirects() throws Exception {
    // Given a custom allowlist so only allowed redirect url is accepted.
    mvc.perform(
            post("/api/systemSettings/{key}", "deviceEnrollmentRedirectAllowlist")
                .header("Authorization", "Basic YWRtaW46ZGlzdHJpY3Q=")
                .param("value", "http://testing.com/*")) // http is normally not allowed, only https
        .andExpect(status().isOk());

    // When calling the enroll endpoint
    String location =
        mvc.perform(
                get("/api/auth/enrollDevice")
                    // Using Basic to bypass login form, user is default admin
                    .header("Authorization", "Basic YWRtaW46ZGlzdHJpY3Q=")
                    .param("deviceVersion", "1.0")
                    .param("deviceType", "android")
                    .param("deviceAttestation", "android_version_1")
                    .param("redirectUri", "http://testing.com/android")
                    .param("state", "abc"))
            // Then expect a redirect with iat and state in the query params
            .andExpect(status().is3xxRedirection())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    // Then validate the response contains an iat and the state
    assertNotNull(location);
    String[] parts = location.split("\\?");
    String query = parts[1];
    String[] queryParts = query.split("&");
    String iat = queryParts[0].split("=")[1];
    String state = queryParts[1].split("=")[1];
    assertEquals("abc", state);
    assertNotNull(iat);

    // Then validate the iat JWT claims
    Jwt decodedIat = jwtDecoder.decode(iat);
    Map<String, Object> claims = decodedIat.getClaims();
    assertNotNull(claims);
    assertEquals("admin", claims.get("sub"));
    assertEquals("client.create", claims.get("scope"));
    assertEquals("http://localhost:8080/", claims.get("iss"));
    assertTrue(
        ((Instant) claims.get("exp")).getEpochSecond()
            > Instant.now().plus(30, ChronoUnit.SECONDS).getEpochSecond());
    assertNotNull(claims.get("jti"));
    assertNotNull(claims.get("iat"));
  }

  private String doClientRegistrationRequest(String iat, KeyPair keyPair) throws Exception {
    MockHttpServletRequestBuilder getClientRegPost = getGetClientRegPost(iat, keyPair);
    String response =
        mvc.perform(getClientRegPost)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.client_id").exists())
            .andExpect(jsonPath("$.client_secret").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((JsonObject) JsonValue.of(response)).getString("client_id").string();
  }

  /**
   * Notice we use client_credentials here so we can test without a browser session. This is not
   * allowed in production for DCR, but ok for this test.
   *
   * @param iat
   * @param keyPair
   * @return
   */
  private static MockHttpServletRequestBuilder getGetClientRegPost(String iat, KeyPair keyPair) {
    return getGetClientRegPost(iat, keyPair, "[\"client_credentials\"]");
  }

  private static MockHttpServletRequestBuilder getGetClientRegPost(
      String iat, KeyPair keyPair, String grantTypesJson) {
    return post("/connect/register")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + iat)
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            String.format(
                """
                 {
                   "client_name": "Test DHIS2 Android Client",
                   "redirect_uris": ["https://dhis2.org"],
                   "grant_types": %s,
                   "response_types": ["code"],
                   "token_endpoint_auth_method": "private_key_jwt",
                   "token_endpoint_auth_signing_alg": "RS256",
                   "scope": "openid profile username",
                   "jwks_uri": "https://dhis2.org/jwks.json",
                   "jwks": %s
                 }
                """,
                grantTypesJson,
                keyPair
                    .jwkSet())); // Inline JWKS , note jwks_uri is also set but should be ignored,
    // validation will fail if not set, only jwks is used
    // NOTE: Scope is defined here BUT this is only because we use client_credentials grant
    // when using /authorize first in the real world, you define scope in there.
  }

  private String createClientAndIat() {
    // Create a client with "client.create" scope to be able to register new clients
    RegisteredClient registeredClient =
        RegisteredClient.withId(CodeGenerator.generateUid())
            .clientId("system-registrar")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .scope("client.create")
            .build();
    oAuth2ClientService.save(registeredClient);

    JwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);
    int ttlSeconds = systemSettingsService.getCurrentSettings().getDeviceEnrollmentIATTtlSeconds();
    String issuer = authorizationServerSettings.getIssuer();
    IatPair iaToken =
        createIaToken(
            registeredClient, "https://dhis2.org", issuer, ttlSeconds, objectMapper, jwtEncoder);
    dhis2OAuth2AuthorizationService.save(iaToken.authorization());

    return iaToken.iatJwt();
  }

  private String createClientAssertion(KeyPair keyPair, String clientId) {
    // This is the server base URL with trailing slash!!!
    String serverBaseUrlWithTrailingSlash = authorizationServerSettings.getIssuer();

    JwsHeader assertionHeader =
        JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyPair.rsaKey().getKeyID()).build();

    JwtEncoder clientJwtEncoder =
        new NimbusJwtEncoder((selector, ctx) -> selector.select(keyPair.jwkSet()));

    JwtClaimsSet assertionClaims =
        JwtClaimsSet.builder()
            .issuer(clientId)
            .subject(clientId)
            .audience(List.of(serverBaseUrlWithTrailingSlash))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .build();

    return clientJwtEncoder
        .encode(JwtEncoderParameters.from(assertionHeader, assertionClaims))
        .getTokenValue();
  }

  private String callTokenEndpoint(KeyPair keyPair, String clientId) throws Exception {
    String clientAssertion = createClientAssertion(keyPair, clientId);

    return mvc.perform(
            post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("client_id", clientId) // include client_id
                .param(
                    "client_assertion_type",
                    "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                .param("grant_type", "client_credentials")
                .param("client_assertion", clientAssertion)
                .param("scope", "openid profile username"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").exists())
        .andExpect(jsonPath("$.token_type").value("Bearer"))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  public static DcrControllerTest.KeyPair createKeys() throws NoSuchAlgorithmException {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    java.security.KeyPair kp = kpg.generateKeyPair();
    RSAPublicKey rsaPublicKey = (RSAPublicKey) kp.getPublic();
    RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) kp.getPrivate();
    String kid = UUID.randomUUID().toString();
    RSAKey rsaKey = new RSAKey.Builder(rsaPublicKey).privateKey(rsaPrivateKey).keyID(kid).build();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return new KeyPair(rsaKey, jwkSet);
  }

  public record KeyPair(RSAKey rsaKey, JWKSet jwkSet) {}
}
