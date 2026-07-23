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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Shared infrastructure for the external-IdP (Keycloak) e2e tests. Runs only in the auth-idp stack:
 * {@code docker-compose.e2e-auth.yml} provides the {@code keycloak} service and the {@code
 * config/dhis2_home_auth/dhis.conf} server config with the {@code oidc.provider.keycloak.*} block.
 *
 * <p>Keycloak realm and users are seeded from {@code config/keycloak/realm-dhis2.json}. DHIS2-side
 * users are created by the tests; the OIDC mapping key is {@code User.openId} == the IdP {@code
 * email} claim (see {@code DhisOidcUserService} / {@code Dhis2JwtAuthenticationManagerResolver}).
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public abstract class KeycloakBaseTest extends BaseE2ETest {

  public static final String CLIENT_ID = "dhis2-oidc";
  public static final String CLIENT_SECRET = "dhis2-oidc-secret";

  /** Keycloak realm URL as reachable from the test JVM (inside the compose network). */
  public static String keycloakRealmUrl;

  protected static void setupKeycloakBase() throws JsonProcessingException {
    serverApiUrl = TestConfiguration.get().baseUrl();
    serverHostUrl = serverApiUrl.replace("/api", "");
    keycloakRealmUrl =
        System.getProperty("keycloak.realm.url", "http://keycloak:8080/realms/dhis2");
    log.info(
        "[setupKeycloakBase] serverApiUrl={}, keycloakRealmUrl={}", serverApiUrl, keycloakRealmUrl);

    awaitKeycloak();

    orgUnitUID = createOrgUnit();
  }

  /** Realm import on first boot can take a while; poll OIDC discovery until it responds. */
  private static void awaitKeycloak() {
    RestTemplate template = new RestTemplate();
    String discoveryUrl = keycloakRealmUrl + "/.well-known/openid-configuration";
    long deadline = System.currentTimeMillis() + 180_000;
    while (System.currentTimeMillis() < deadline) {
      try {
        ResponseEntity<String> response = template.getForEntity(discoveryUrl, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
          log.info("[awaitKeycloak] Keycloak realm is up at {}", keycloakRealmUrl);
          return;
        }
      } catch (Exception e) {
        log.info("[awaitKeycloak] not up yet ({}), retrying...", e.getMessage());
      }
      try {
        Thread.sleep(2_000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        fail("Interrupted while waiting for Keycloak");
      }
    }
    fail("Keycloak realm did not become available within 180s: " + discoveryUrl);
  }

  /**
   * Obtains an access token from Keycloak via the direct access grant (resource owner password
   * credentials). The realm client has directAccessGrantsEnabled and an audience mapper that puts
   * {@value #CLIENT_ID} into the access token {@code aud} (required by DHIS2 JWT bearer auth).
   */
  protected static String getKeycloakAccessToken(String username, String password) {
    RestTemplate template = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "password");
    form.add("client_id", CLIENT_ID);
    form.add("client_secret", CLIENT_SECRET);
    form.add("username", username);
    form.add("password", password);
    form.add("scope", "openid email");

    ResponseEntity<String> response =
        template.postForEntity(
            keycloakRealmUrl + "/protocol/openid-connect/token",
            new HttpEntity<>(form, headers),
            String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode(), "Keycloak token endpoint failed");
    try {
      JsonNode json = objectMapper.readTree(response.getBody());
      JsonNode accessToken = json.get("access_token");
      assertNotNull(accessToken, "No access_token in Keycloak response: " + response.getBody());
      return accessToken.asText();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a DHIS2 user mapped to a Keycloak user: {@code openId} must equal the IdP email claim
   * and {@code externalAuth} must be true for the OIDC login flow to accept the user.
   */
  protected static String createDhisOidcUser(String username, String email, String userRoleId)
      throws JsonProcessingException {
    Map<String, Object> userMap =
        Map.of(
            "username",
            username,
            "password",
            "Test123###...",
            "email",
            email,
            "openId",
            email,
            "externalAuth",
            true,
            "firstName",
            "Oidc",
            "surname",
            "User",
            "userRoles",
            List.of(Map.of("id", userRoleId)),
            "organisationUnits",
            List.of(Map.of("id", orgUnitUID)));

    ResponseEntity<String> response = postWithAdminBasicAuth("/users", userMap);
    JsonNode json = objectMapper.readTree(response.getBody());
    assertEquals(
        HttpStatus.CREATED,
        response.getStatusCode(),
        "user creation failed: " + response.getBody());
    String uid = json.get("response").get("uid").asText();
    assertNotNull(uid);
    return uid;
  }

  /** Creates a user role with NO authorities, for @RequiresAuthority denial tests. */
  protected static String createEmptyAuthorityRole(String name) throws JsonProcessingException {
    ResponseEntity<String> response =
        postWithAdminBasicAuth("/userRoles", Map.of("name", name, "authorities", List.of()));
    assertEquals(
        HttpStatus.CREATED,
        response.getStatusCode(),
        "role creation failed: " + response.getBody());
    JsonNode json = objectMapper.readTree(response.getBody());
    String uid = json.get("response").get("uid").asText();
    assertNotNull(uid);
    return uid;
  }

  protected static ResponseEntity<String> postWithBearerJwt(String fullUrl, String token) {
    RestTemplate template = addBearerTokenAuthHeaders(new RestTemplate(), token);
    try {
      return template.exchange(
          fullUrl, HttpMethod.POST, new HttpEntity<>("", jsonHeaders()), String.class);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }
}
