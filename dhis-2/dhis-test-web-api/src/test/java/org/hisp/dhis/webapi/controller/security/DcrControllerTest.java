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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
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
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;

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

  /** Plain mapper for registration request bodies, without the security modules' type info. */
  private static final ObjectMapper registrationMapper = new ObjectMapper();

  /** Redirect URI the test IATs are minted for, i.e. their {@code redirect_url} claim. */
  private static final String IAT_REDIRECT_URI = "https://dhis2.org";

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

    // When registering the client the DHIS2 Android Capture app registers
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
    // DCR auth-code clients require S256 PKCE by default (PR-H)
    assertTrue(clientSettings.isRequireProofKey());
    // Default scopes assigned by the server when registration omits scopes: openid, profile,
    // username (email is intentionally excluded, see OAuth2Constants.DCR_DEFAULT_SCOPES)
    assertEquals(Set.of("openid", "profile", "username"), client.getScopes());
    // The client holds only the device grants and the redirect URI its IAT was minted for
    assertEquals(
        Set.of(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN),
        client.getAuthorizationGrantTypes());
    assertEquals(Set.of(IAT_REDIRECT_URI), client.getRedirectUris());

    // When the client refreshes the tokens of an authorization, authenticating with
    // private_key_jwt. The authorization_code + PKCE exchange is covered by
    // OAuth2PkceEnforcementTest.
    String refreshToken = UUID.randomUUID().toString();
    saveAuthorizationWithRefreshToken(
        client, refreshToken, Instant.now(), Instant.now().plus(Duration.ofDays(30)));
    String tokenResponse =
        callRefreshTokenEndpoint(keyPair, clientId, refreshToken)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String accessToken = JsonValue.of(tokenResponse).asObject().getString("access_token").string();

    // Then the access token acts as the authorizing user on the API
    mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk());

    // And the client cannot mint client_credentials tokens
    clientCredentialsTokenRequest(keyPair, clientId)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("unauthorized_client"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "client_credentials",
        "authorization_code,client_credentials",
        "authorization_code,urn:ietf:params:oauth:grant-type:token-exchange",
        "refresh_token"
      })
  @DisplayName("DCR rejects grant types outside the device profile and persists nothing")
  void testRegistrationRejectsGrantTypesOutsideDeviceProfile(String grantTypes) throws Exception {
    String initialAccessToken = createClientAndIat();
    Map<String, Object> registration = deviceRegistration(createKeys());
    registration.put("grant_types", List.of(grantTypes.split(",")));
    int clientCount = countClients();

    mvc.perform(registrationRequest(initialAccessToken, registration))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_client_metadata"));

    assertEquals(clientCount, countClients());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"https://other.example/cb", IAT_REDIRECT_URI + ",https://other.example/cb"})
  @DisplayName("DCR rejects redirect URIs other than the one the IAT was minted for")
  void testRegistrationRejectsRedirectUrisNotBoundToIat(String redirectUris) throws Exception {
    String initialAccessToken = createClientAndIat();
    Map<String, Object> registration = deviceRegistration(createKeys());
    registration.put("redirect_uris", List.of(redirectUris.split(",")));

    mvc.perform(registrationRequest(initialAccessToken, registration))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_redirect_uri"));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"client_secret_basic", "client_secret_post", "none"})
  @DisplayName("DCR rejects every client authentication method except private_key_jwt")
  void testRegistrationRejectsAuthMethodsOtherThanPrivateKeyJwt(String authMethod)
      throws Exception {
    String initialAccessToken = createClientAndIat();
    Map<String, Object> registration = deviceRegistration(createKeys());
    // A signing algorithm only goes with the JWT-based methods
    registration.remove("token_endpoint_auth_signing_alg");
    if (authMethod == null) {
      // Without a method Spring AS falls back to client_secret_basic and issues a secret
      registration.remove("token_endpoint_auth_method");
    } else {
      registration.put("token_endpoint_auth_method", authMethod);
    }

    mvc.perform(registrationRequest(initialAccessToken, registration))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_client_metadata"))
        .andExpect(jsonPath("$.client_secret").doesNotExist());
  }

  @Test
  @DisplayName("DCR rejects post-logout redirect URIs")
  void testRegistrationRejectsPostLogoutRedirectUris() throws Exception {
    String initialAccessToken = createClientAndIat();
    Map<String, Object> registration = deviceRegistration(createKeys());
    registration.put("post_logout_redirect_uris", List.of(IAT_REDIRECT_URI));

    mvc.perform(registrationRequest(initialAccessToken, registration))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_client_metadata"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"disabled", "expired", "renamed"})
  @DisplayName("DCR rejects an IAT whose subject is no longer an active user")
  void testRegistrationRejectsIatOfInactiveSubject(String change) throws Exception {
    // Given an IAT minted for a user who is disabled, expired or renamed before registering
    User enroller = createUserWithAuth("dcr" + change);
    String initialAccessToken = createIatFor(enroller);
    switch (change) {
      case "disabled" -> enroller.setDisabled(true);
      case "expired" ->
          enroller.setAccountExpiry(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
      default -> enroller.setUsername("dcr" + change + "gone");
    }
    userService.updateUser(enroller);
    int clientCount = countClients();

    // Then the IAT no longer authorizes a registration, and no client is persisted
    mvc.perform(registrationRequest(initialAccessToken, deviceRegistration(createKeys())))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("invalid_token"));

    assertEquals(clientCount, countClients());
  }

  @Test
  @DisplayName("A rejected registration does not consume the IAT")
  void testRejectedRegistrationLeavesIatUsable() throws Exception {
    String initialAccessToken = createClientAndIat();
    KeyPair keyPair = createKeys();
    Map<String, Object> rejected = deviceRegistration(keyPair);
    rejected.put("grant_types", List.of("client_credentials"));
    mvc.perform(registrationRequest(initialAccessToken, rejected))
        .andExpect(status().isBadRequest());

    // Then the device can retry with a conforming registration and the same IAT
    assertNotNull(doClientRegistrationRequest(initialAccessToken, keyPair));
  }

  @Test
  @DisplayName("An enrolling user can register the device client, but no other client")
  void testEnrolledUserCanOnlyRegisterDeviceClient() throws Exception {
    // Given a user without any authority, and the default enrollment redirect allowlist
    createUserWithAuth("dcrenroller");
    mvc.perform(
            post("/api/systemSettings/{key}", "deviceEnrollmentRedirectAllowlist")
                .header(HttpHeaders.AUTHORIZATION, basicAuth("admin", "district"))
                .param("value", "dhis2oauth://oauth"))
        .andExpect(status().isOk());

    // When the user enrolls a device, as the Android Capture app does
    String location =
        mvc.perform(
                get("/api/auth/enrollDevice")
                    .header(HttpHeaders.AUTHORIZATION, basicAuth("dcrenroller", "district"))
                    .param("redirectUri", "dhis2oauth://oauth")
                    .param("state", "abc"))
            .andExpect(status().is3xxRedirection())
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.LOCATION);
    assertNotNull(location);
    String initialAccessToken =
        UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("iat");
    KeyPair keyPair = createKeys();
    Map<String, Object> registration = deviceRegistration(keyPair);
    registration.put("redirect_uris", List.of("dhis2oauth://oauth"));

    // Then the IAT cannot register a client_credentials client
    Map<String, Object> clientCredentials = new LinkedHashMap<>(registration);
    clientCredentials.put("grant_types", List.of("client_credentials"));
    mvc.perform(registrationRequest(initialAccessToken, clientCredentials))
        .andExpect(status().isBadRequest());

    // But it registers the device client, owned by the enrolling user
    String response =
        mvc.perform(registrationRequest(initialAccessToken, registration))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String clientId = ((JsonObject) JsonValue.of(response)).getString("client_id").string();
    assertEquals(
        "dcrenroller",
        oAuth2ClientService
            .getAsDhis2OAuth2ClientByClientId(clientId)
            .getCreatedBy()
            .getUsername());
  }

  @Test
  @DisplayName("A client_credentials token is not mapped to a DHIS2 user")
  void testClientCredentialsTokenIsNotMappedToUser() throws Exception {
    // Given a client_credentials client saved directly through the client service
    KeyPair keyPair = createKeys();
    String clientId = "client-credentials-client";
    RegisteredClient client =
        RegisteredClient.withId(CodeGenerator.generateUid())
            .clientId(clientId)
            .clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .scopes(scopes -> scopes.addAll(Set.of("openid", "profile", "username")))
            .clientSettings(
                ClientSettings.builder()
                    .setting("client.inline.jwks", keyPair.jwkSet().toString())
                    .tokenEndpointAuthenticationSigningAlgorithm(SignatureAlgorithm.RS256)
                    .build())
            .build();
    injectAdminIntoSecurityContext();
    oAuth2ClientService.save(client, CurrentUserUtil.getCurrentUserDetails());

    // When the client obtains a client_credentials token with the username scope
    String tokenResponse =
        clientCredentialsTokenRequest(keyPair, clientId)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String accessToken = JsonValue.of(tokenResponse).asObject().getString("access_token").string();

    // Then the token is not accepted for API requests, as it maps to no user
    mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("An IAT-authenticated save never persists a client_credentials client")
  void testIatAuthenticatedSaveRejectsClientCredentialsClient() {
    // Given the security context of a DCR request, authenticated with an IAT
    Jwt initialAccessToken = jwtDecoder.decode(createClientAndIat());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(initialAccessToken, null, List.of()));
    RegisteredClient client =
        RegisteredClient.withId(CodeGenerator.generateUid())
            .clientId("iat-client-credentials-client")
            .clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .build();

    // Then the client is not persisted, whichever registration endpoint hands it over
    assertThrows(IllegalArgumentException.class, () -> oAuth2ClientService.save(client));
    injectAdminIntoSecurityContext();
    assertNull(oAuth2ClientService.findByClientId("iat-client-credentials-client"));
  }

  @Test
  @DisplayName("Test DCR-registered client default scopes exclude email")
  void testDcrRegisteredClientDefaultScopesExcludeEmail() throws Exception {
    // Given an initial access token (iat)
    String initialAccessToken = createClientAndIat();

    // Given a key pair to be used for the client's private_key_jwt authentication
    KeyPair keyPair = createKeys();

    // When registering a client without specifying scopes
    String clientId = doClientRegistrationRequest(initialAccessToken, keyPair);
    RegisteredClient client = oAuth2ClientService.findByClientId(clientId);
    assertNotNull(client);

    // Then the server-assigned default scopes are exactly openid, profile, username
    assertTrue(client.getScopes().contains("openid"));
    assertTrue(client.getScopes().contains("profile"));
    assertTrue(client.getScopes().contains("username"));
    assertFalse(
        client.getScopes().contains("email"),
        "DCR default scopes must not include email (PR-H, OAuth2Constants.DCR_DEFAULT_SCOPES)");
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
    String initialRefreshToken = UUID.randomUUID().toString();
    saveAuthorizationWithRefreshToken(
        client, initialRefreshToken, Instant.now(), Instant.now().plus(Duration.ofDays(30)));

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
    String expiredRefreshToken = UUID.randomUUID().toString();
    saveAuthorizationWithRefreshToken(
        client,
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
    return doClientRegistrationRequest(createClientAndIat(), keyPair);
  }

  /**
   * Persists an {@link OAuth2Authorization} holding a refresh token with the given lifetime, as it
   * would exist after a completed authorization_code flow.
   */
  private void saveAuthorizationWithRefreshToken(
      RegisteredClient client, String refreshTokenValue, Instant issuedAt, Instant expiresAt) {
    // MockMvc requests clear the thread's security context; the authorization store needs a
    // current user for auditing
    injectAdminIntoSecurityContext();
    OAuth2RefreshToken refreshToken =
        new OAuth2RefreshToken(refreshTokenValue, issuedAt, expiresAt);
    UsernamePasswordAuthenticationToken principal =
        UsernamePasswordAuthenticationToken.authenticated("admin", null, List.of());
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(client)
            .id(UUID.randomUUID().toString())
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
    mvc.perform(registrationRequest(initialAccessToken, deviceRegistration(keyPair)))
        .andExpect(status().isUnauthorized());
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

  @Test
  @DisplayName("Unauthenticated enroll request is saved and resumed after form login")
  void testEnrollEndpointIsResumedAfterLogin() throws Exception {
    // Given an unauthenticated call to the enroll endpoint, like the Android Capture app does
    MvcResult result =
        mvc.perform(
                get("/api/auth/enrollDevice?redirectUri=dhis2oauth://oauth&state=abc")
                    .accept(MediaType.TEXT_HTML))
            // Then the user is sent to the login page
            .andExpect(status().is3xxRedirection())
            .andReturn();
    String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
    assertNotNull(location);
    assertTrue(location.contains("login"), location);

    // And the request is saved in a session so it can be resumed after login. Without this the
    // login app has no saved request and sends the user to the dashboard instead of the device.
    HttpSession session = result.getRequest().getSession(false);
    assertNotNull(session, "enroll request must create a session holding the saved request");

    // When the user logs in on that session
    mvc.perform(
            post("/api/auth/login")
                .session((MockHttpSession) session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"district\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loginStatus").value("SUCCESS"))
        // Then the login response points back to the enroll endpoint, not the dashboard
        .andExpect(
            jsonPath("$.redirectUrl")
                .value("/api/auth/enrollDevice?redirectUri=dhis2oauth://oauth&state=abc"));
  }

  private String doClientRegistrationRequest(String iat, KeyPair keyPair) throws Exception {
    String response =
        mvc.perform(registrationRequest(iat, deviceRegistration(keyPair)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.client_id").exists())
            .andExpect(jsonPath("$.client_secret").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((JsonObject) JsonValue.of(response)).getString("client_id").string();
  }

  /**
   * The registration metadata the DHIS2 Android SDK sends ({@code DCRNetworkHandlerImpl}), bound to
   * the IAT redirect URI. Tests change single fields of it to probe the registration policy.
   */
  private static Map<String, Object> deviceRegistration(KeyPair keyPair) {
    Map<String, Object> registration = new LinkedHashMap<>();
    registration.put("client_name", "Test DHIS2 Android Client");
    registration.put("redirect_uris", List.of(IAT_REDIRECT_URI));
    registration.put("grant_types", List.of("authorization_code", "refresh_token"));
    registration.put("response_types", List.of("code"));
    registration.put("token_endpoint_auth_method", "private_key_jwt");
    registration.put("token_endpoint_auth_signing_alg", "RS256");
    // Spring AS requires a jwks_uri for private_key_jwt; only the inline jwks is used
    registration.put("jwks_uri", "https://dhis2.org/jwks.json");
    registration.put("jwks", keyPair.jwkSet().toJSONObject());
    return registration;
  }

  private static MockHttpServletRequestBuilder registrationRequest(
      String iat, Map<String, Object> registration) throws JsonProcessingException {
    return post("/connect/register")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + iat)
        .contentType(MediaType.APPLICATION_JSON)
        .content(registrationMapper.writeValueAsString(registration));
  }

  private static String basicAuth(String username, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  private int countClients() {
    injectAdminIntoSecurityContext();
    return oAuth2ClientService.getAll().size();
  }

  /** Mints an IAT for the admin user. */
  private String createClientAndIat() {
    return createIatFor(getAdminUser());
  }

  /**
   * Mints an IAT bound to {@link #IAT_REDIRECT_URI} for the given user, as {@code
   * /api/auth/enrollDevice} does.
   */
  private String createIatFor(User subject) {
    injectAdminIntoSecurityContext();
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
    // The IAT subject is the current user
    injectSecurityContextUser(subject);
    IatPair iaToken =
        createIaToken(
            registeredClient, IAT_REDIRECT_URI, issuer, ttlSeconds, objectMapper, jwtEncoder);
    injectAdminIntoSecurityContext();
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

  private ResultActions clientCredentialsTokenRequest(KeyPair keyPair, String clientId)
      throws Exception {
    return mvc.perform(
        post("/oauth2/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("client_id", clientId)
            .param(
                "client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
            .param("grant_type", "client_credentials")
            .param("client_assertion", createClientAssertion(keyPair, clientId))
            .param("scope", "openid profile username"));
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
