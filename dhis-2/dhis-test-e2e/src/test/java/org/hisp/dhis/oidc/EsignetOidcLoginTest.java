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
package org.hisp.dhis.oidc;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Real eSignet 2.0.0 authorization-code/OTP/consent exchanges, using the same flow API as its UI.
 * No fabricated tokens or IdP responses. Each login has an isolated browser cookie jar.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Tag("esignet")
class EsignetOidcLoginTest extends BaseE2ETest {
  private static final String IDP = "http://esignet:8080";
  private static final String CLIENT = "dhis2-e2e";
  private static final String EMAIL = "siddhartha.km@gmail.com";
  private static final String CALLBACK = "http://web:8080/oauth2/code/esignet";
  private static final String INDIVIDUAL = "1774231323";
  private static RSAKey publishedKey;

  @BeforeAll
  @org.junit.jupiter.api.Timeout(value = 10, unit = java.util.concurrent.TimeUnit.MINUTES)
  static void setup() throws Exception {
    serverApiUrl = TestConfiguration.get().baseUrl();
    serverHostUrl = serverApiUrl.replace("/api", "");
    awaitReady(serverApiUrl + "/publicKeys/esignet/jwks.json");
    awaitReady(IDP + "/.well-known/openid-configuration");
    awaitReady("http://mock-identity-system:8082/v1/mock-identity-system/actuator/health");
    HttpClient http = browser();
    publishedKey =
        (RSAKey)
            JWKSet.parse(get(http, serverApiUrl + "/publicKeys/esignet/jwks.json").body())
                .getKeys()
                .get(0);
    Map<String, Object> registration = new LinkedHashMap<>();
    registration.put("clientId", CLIENT);
    registration.put("clientName", "DHIS2 E2E");
    registration.put("clientNameLangMap", Map.of("eng", "DHIS2 E2E"));
    registration.put("relyingPartyId", "dhis2");
    registration.put("logoUri", "https://dhis2.org/wp-content/uploads/dhis2-logo-rgb-positive.png");
    registration.put("redirectUris", List.of(CALLBACK));
    registration.put("userClaims", List.of("email", "name"));
    registration.put("authContextRefs", List.of("mosip:idp:acr:generated-code"));
    // eSignet registers a public JWK, not a remotely refreshed JWKS URI. Use DHIS2's key verbatim.
    registration.put("publicKey", publishedKey.toJSONObject());
    registration.put("grantTypes", List.of("authorization_code"));
    registration.put("clientAuthMethods", List.of("private_key_jwt"));
    registration.put(
        "additionalConfig",
        Map.of(
            "require_pkce",
            false,
            "require_pushed_authorization_requests",
            false,
            "dpop_bound_access_tokens",
            false));
    JsonNode result =
        postJson(
            http,
            IDP + "/client-mgmt/client",
            Map.of(
                "requestTime",
                new java.time.format.DateTimeFormatterBuilder()
                    .appendInstant(3)
                    .toFormatter()
                    .format(Instant.now()),
                "request",
                registration));
    assertEquals(CLIENT, result.path("response").path("clientId").asText(), result.toString());
    orgUnitUID = createOrgUnit();
  }

  @Test
  void realOtpLoginRejectsUnmappedThenResolvesMappedDhisUser() throws Exception {
    HttpClient unmapped = browser();
    String denied = login(unmapped);
    assertTrue(
        denied.contains("oidcFailure=true"), "Unmapped user must fail DHIS2 login: " + denied);
    assertTrue(
        location(get(unmapped, serverApiUrl + "/me")).contains("/login/"),
        "Unmapped browser session must not access /api/me");

    Map<String, Object> user = new LinkedHashMap<>();
    user.put("username", "esignetmapped");
    user.put("password", "Test123###...");
    user.put("email", EMAIL);
    user.put("openId", EMAIL);
    user.put("externalAuth", true);
    user.put("firstName", "Esignet");
    user.put("surname", "Mapped");
    user.put("userRoles", List.of(Map.of("id", SUPER_USER_ROLE_UID)));
    user.put("organisationUnits", List.of(Map.of("id", orgUnitUID)));
    var created = postWithAdminBasicAuth("/users", user);
    assertEquals(201, created.getStatusCode().value(), created.getBody());
    String uid = objectMapper.readTree(created.getBody()).path("response").path("uid").asText();
    try {
      HttpClient mapped = browser();
      String redirect = login(mapped);
      assertFalse(redirect.contains("oidcFailure"), "Mapped login rejected: " + redirect);
      var me = get(mapped, serverApiUrl + "/me");
      assertEquals(200, me.statusCode(), me.body());
      JsonNode identity = objectMapper.readTree(me.body());
      assertEquals(uid, identity.path("id").asText());
      assertEquals("esignetmapped", identity.path("username").asText());
      assertTrue(identity.path("authorities").toString().contains("ALL"));
      System.out.println(
          "ESIGNET_LOGIN mapped username=esignetmapped; unmapped rejected; client_auth=private_key_jwt");
    } finally {
      deleteWithAdminBasicAuth("/users/" + uid, null);
    }
  }

  @Test
  void realSignedUserinfoMatchesPublishedJwksAndDocumentsClaims() throws Exception {
    HttpClient http = browser();
    String authorize =
        IDP
            + "/oauth2/authorize?"
            + form(
                Map.of(
                    "client_id",
                    CLIENT,
                    "redirect_uri",
                    CALLBACK,
                    "response_type",
                    "code",
                    "scope",
                    "openid email",
                    "state",
                    UUID.randomUUID().toString(),
                    "nonce",
                    UUID.randomUUID().toString()));
    String callback = authenticate(http, location(get(http, authorize)));
    String code = query(callback).get("code");
    assertNotNull(code, "No authorization code");
    KeyStore store = KeyStore.getInstance("PKCS12");
    try (var in = Files.newInputStream(Path.of("/client.p12"))) {
      store.load(in, "e2e-password".toCharArray());
    }
    RSAKey signingKey = RSAKey.load(store, "esignet", "e2e-password".toCharArray());
    assertEquals(publishedKey.computeThumbprint(), signingKey.toPublicJWK().computeThumbprint());
    Instant now = Instant.now();
    SignedJWT assertion =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(publishedKey.getKeyID()).build(),
            new JWTClaimsSet.Builder()
                .issuer(CLIENT)
                .subject(CLIENT)
                .audience(IDP)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(60)))
                .jwtID(UUID.randomUUID().toString())
                .build());
    assertion.sign(new RSASSASigner(signingKey));
    var tokenResponse =
        send(
            http,
            HttpRequest.newBuilder(URI.create(IDP + "/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        form(
                            Map.of(
                                "grant_type",
                                "authorization_code",
                                "code",
                                code,
                                "redirect_uri",
                                CALLBACK,
                                "client_id",
                                CLIENT,
                                "client_assertion_type",
                                "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                                "client_assertion",
                                assertion.serialize())))));
    assertEquals(
        200,
        tokenResponse.statusCode(),
        "private_key_jwt token exchange failed: " + tokenResponse.statusCode());
    JsonNode tokens = objectMapper.readTree(tokenResponse.body());
    SignedJWT idToken = SignedJWT.parse(tokens.path("id_token").asText());
    var userinfo =
        send(
            http,
            HttpRequest.newBuilder(URI.create(IDP + "/oauth2/userinfo"))
                .header("Authorization", "Bearer " + tokens.path("access_token").asText())
                .header("Accept", "application/jwt")
                .GET());
    assertEquals(200, userinfo.statusCode());
    String contentType = userinfo.headers().firstValue("Content-Type").orElse("");
    assertEquals("application/jwt", contentType.split(";")[0]);
    SignedJWT jwt = SignedJWT.parse(userinfo.body());
    JWKSet keys = JWKSet.parse(get(http, IDP + "/oauth2/jwks").body());
    RSAKey key = (RSAKey) keys.getKeyByKeyId(jwt.getHeader().getKeyID());
    assertNotNull(key, "userinfo kid must be in eSignet JWKS");
    assertTrue(jwt.verify(new RSASSAVerifier(key)), "userinfo signature");
    assertTrue(
        key.getAlgorithm() == null || key.getAlgorithm().equals(jwt.getHeader().getAlgorithm()),
        "JWKS alg must not contradict the real userinfo signature algorithm");
    RSAKey idTokenKey = (RSAKey) keys.getKeyByKeyId(idToken.getHeader().getKeyID());
    assertNotNull(idTokenKey, "ID token kid must be in eSignet JWKS");
    assertTrue(idToken.verify(new RSASSAVerifier(idTokenKey)), "ID token signature");
    JWTClaimsSet claims = jwt.getJWTClaimsSet();
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("esignetVersion", "2.0.0");
    evidence.put("fixturePatch", "external-rsa-jwks.patch");
    evidence.put("contentType", contentType);
    evidence.put("userinfoAlgorithm", jwt.getHeader().getAlgorithm().getName());
    evidence.put("idTokenAlgorithm", idToken.getHeader().getAlgorithm().getName());
    evidence.put("userinfoClaims", objectMapper.readTree(jwt.getPayload().toString()));
    evidence.put("signatureVerifiedAgainstPublishedJwks", true);
    evidence.put("clientAuthentication", "private_key_jwt");
    evidence.put("clientAssertionAlgorithm", "RS256");
    evidence.put("clientAssertionAudience", IDP);
    Files.createDirectories(Path.of("target/surefire-reports"));
    objectMapper
        .writerWithDefaultPrettyPrinter()
        .writeValue(Path.of("target/surefire-reports/esignet-evidence.json").toFile(), evidence);
    System.out.println("ESIGNET_EVIDENCE " + objectMapper.writeValueAsString(evidence));
    assertEquals(JWSAlgorithm.RS256, jwt.getHeader().getAlgorithm());
    assertEquals(idToken.getJWTClaimsSet().getSubject(), claims.getSubject());
    assertEquals(EMAIL, claims.getStringClaim("email"));
    assertEquals(IDP, claims.getIssuer());
    assertEquals(List.of(CLIENT), claims.getAudience());
    assertEquals(Set.of("sub", "email", "iss", "aud"), claims.getClaims().keySet());
  }

  private static String login(HttpClient http) throws Exception {
    String authorize = location(get(http, serverHostUrl + "/oauth2/authorization/esignet"));
    assertTrue(authorize.startsWith(IDP + "/oauth2/authorize?"));
    String callback = authenticate(http, location(get(http, authorize)));
    assertTrue(callback.startsWith(CALLBACK + "?"));
    return location(get(http, callback));
  }

  private static String authenticate(HttpClient http, String loginUrl) throws Exception {
    Map<String, String> query = query(loginUrl);
    assertNotNull(query.get("executionId"), "eSignet did not start a login flow: " + loginUrl);
    assertEquals(200, get(http, loginUrl).statusCode(), "Upstream eSignet UI must be reachable");
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("executionId", query.get("executionId"));
    Map<String, String> inputs = new LinkedHashMap<>();
    boolean suppliedOtp = false;
    for (int step = 0; step < 12; step++) {
      JsonNode flow = postJson(http, IDP + "/flow/execute", payload);
      String status = flow.path("flowStatus").asText();
      if ("COMPLETE".equals(status)) {
        assertTrue(suppliedOtp, "Must authenticate with OTP, not reuse an SSO session");
        JsonNode callback =
            postJson(
                http,
                IDP + "/oauth2/auth/callback",
                Map.of(
                    "authId", query.get("authId"), "assertion", flow.path("assertion").asText()));
        return callback.has("redirect_uri")
            ? callback.path("redirect_uri").asText()
            : callback.path("redirectUri").asText();
      }
      assertEquals("INCOMPLETE", status, "Unexpected eSignet flow status");
      assertTrue(
          flow.path("error").isMissingNode() || flow.path("error").isNull(),
          "Flow error: " + flow.path("error"));
      for (JsonNode input : flow.path("data").path("inputs")) {
        String id = input.path("identifier").asText();
        switch (id) {
          case "username" -> inputs.put(id, INDIVIDUAL);
          case "otp" -> {
            inputs.put(id, "111111");
            suppliedOtp = true;
          }
          case "captcha_token" -> inputs.put(id, "test-captcha-token");
          case "consent_decisions" ->
              inputs.put(
                  id,
                  consent(flow.path("data").path("additionalData").path("consentPrompt").asText()));
          default -> fail("Unexpected eSignet input: " + id);
        }
      }
      JsonNode actions = flow.path("data").path("actions");
      String action = actions.path(0).path("ref").asText();
      for (JsonNode choice : actions) {
        if ("acr_otp".equals(choice.path("ref").asText())) action = "acr_otp";
      }
      assertFalse(action.isBlank(), "Missing flow action");
      payload.put("challengeToken", flow.path("challengeToken").asText());
      payload.put("action", action);
      payload.put("inputs", inputs);
    }
    throw new AssertionError("eSignet login exceeded 12 flow steps");
  }

  private static String consent(String prompt) throws Exception {
    List<Object> purposes = new ArrayList<>();
    for (JsonNode purpose : objectMapper.readTree(prompt)) {
      List<Object> elements = new ArrayList<>();
      for (String kind : List.of("essential", "optional")) {
        for (JsonNode element : purpose.path(kind)) {
          elements.add(Map.of("name", element.path("name").asText(), "approved", true));
        }
      }
      purposes.add(
          Map.of(
              "purposeName",
              purpose.path("purposeName").asText(),
              "purposeId",
              purpose.path("purposeId").asText(),
              "approved",
              true,
              "elements",
              elements));
    }
    return objectMapper.writeValueAsString(Map.of("approved", true, "purposes", purposes));
  }

  private static HttpClient browser() {
    return HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(10))
        .build();
  }

  private static HttpResponse<String> send(HttpClient http, HttpRequest.Builder request)
      throws Exception {
    return http.send(
        request.timeout(Duration.ofSeconds(60)).build(), HttpResponse.BodyHandlers.ofString());
  }

  private static HttpResponse<String> get(HttpClient http, String url) throws Exception {
    return send(http, HttpRequest.newBuilder(URI.create(url)).GET());
  }

  private static JsonNode postJson(HttpClient http, String url, Object body) throws Exception {
    var response =
        send(
            http,
            HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))));
    assertEquals(
        200, response.statusCode(), "POST " + URI.create(url).getPath() + ": " + response.body());
    return objectMapper.readTree(response.body());
  }

  private static String location(HttpResponse<String> response) {
    assertTrue(
        response.statusCode() >= 300 && response.statusCode() < 400,
        "Expected redirect, HTTP " + response.statusCode());
    return response
        .uri()
        .resolve(response.headers().firstValue("Location").orElseThrow())
        .toString();
  }

  private static Map<String, String> query(String url) {
    String raw = URI.create(url).getRawQuery();
    Map<String, String> values = new LinkedHashMap<>();
    if (raw != null)
      for (String part : raw.split("&")) {
        String[] pair = part.split("=", 2);
        values.put(
            URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
            pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
      }
    return values;
  }

  private static String form(Map<String, String> values) {
    return values.entrySet().stream()
        .map(
            e ->
                URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
  }

  private static void awaitReady(String url) throws Exception {
    HttpClient http = browser();
    Instant deadline = Instant.now().plusSeconds(600);
    while (Instant.now().isBefore(deadline)) {
      try {
        if (get(http, url).statusCode() == 200) return;
      } catch (java.io.IOException ignored) {
        // Fresh containers may not yet have bound their HTTP port.
      }
      Thread.sleep(2000);
    }
    fail("Readiness deadline exceeded: " + url);
  }
}
