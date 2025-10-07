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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tests for Dynamic Client Registration (DCR) with JWKS provided inline in the registration
 * request.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class DcrWithJwksTest extends ControllerWithJwtTokenAuthTestBase {

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
                get("/api/deviceClients/enroll")
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
    assertEquals("http://localhost:8080", claims.get("iss"));
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
    return post("/connect/register")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + iat)
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            String.format(
                """
                 {
                   "client_name": "Test DHIS2 Android Client",
                   "redirect_uris": ["https://dhis2.org"],
                   "grant_types": ["client_credentials"],
                   "response_types": ["code"],
                   "token_endpoint_auth_method": "private_key_jwt",
                   "token_endpoint_auth_signing_alg": "RS256",
                   "scope": "openid profile username",
                   "jwks_uri": "https://dhis2.org/jwks.json",
                   "jwks": %s
                 }
                """,
                keyPair
                    .jwkSet())); // Inline JWKS , note jwks_uri is also set but should be ignored,
    // validation will fail if not set, only jwks is used
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

  private String callTokenEndpoint(KeyPair keyPair, String clientId) throws Exception {
    String tokenEndpoint = authorizationServerSettings.getIssuer() + "/oauth2/token";

    JwsHeader assertionHeader =
        JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyPair.rsaKey().getKeyID()).build();

    JwtEncoder clientJwtEncoder =
        new NimbusJwtEncoder((selector, ctx) -> selector.select(keyPair.jwkSet()));

    JwtClaimsSet assertionClaims =
        JwtClaimsSet.builder()
            .issuer(clientId)
            .subject(clientId)
            .audience(List.of(tokenEndpoint))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .build();

    String clientAssertion =
        clientJwtEncoder
            .encode(JwtEncoderParameters.from(assertionHeader, assertionClaims))
            .getTokenValue();

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

  public static DcrWithJwksTest.KeyPair createKeys() throws NoSuchAlgorithmException {
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
