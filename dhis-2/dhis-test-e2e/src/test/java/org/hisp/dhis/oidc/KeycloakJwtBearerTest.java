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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * E2e coverage for API authentication with an external-IdP (Keycloak) JWT bearer token ({@code
 * oidc.jwt.token.authentication.enabled}, {@code Dhis2JwtAuthenticationManagerResolver}).
 *
 * <p>Pinned contract: the token {@code iss} must match {@code oidc.provider.keycloak.issuer_uri},
 * the {@code aud} must contain the configured client id, and the {@code email} claim maps to {@code
 * User.openId}. Authorization outcomes (403 message contract, ALL bypass) must be identical to
 * every other authentication mechanism.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Tag("auth-idp")
@Slf4j
class KeycloakJwtBearerTest extends KeycloakBaseTest {

  private static final String MAINTENANCE_PATH = "/maintenance/categoryOptionComboUpdate";

  @BeforeAll
  static void setup() throws JsonProcessingException {
    setupKeycloakBase();

    // kcmapped -> DHIS2 superuser; kclimited -> DHIS2 user with an empty-authority role
    createDhisOidcUser("kcmapped", "kcmapped@dhis2.org", SUPER_USER_ROLE_UID);
    String emptyRole = createEmptyAuthorityRole("authIdpEmptyRoleJwt");
    createDhisOidcUser("kclimited", "kclimited@dhis2.org", emptyRole);
  }

  @AfterAll
  static void tearDown() {
    invalidateAllSession();
  }

  @Test
  void testBearerTokenAuthenticatesMappedUser() throws JsonProcessingException {
    String token = getKeycloakAccessToken("kcmapped", "KcMapped123###");

    ResponseEntity<String> response = getWithBearerJwt(serverApiUrl + "/me", token);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "body: " + response.getBody());
    JsonNode me = objectMapper.readTree(response.getBody());
    assertEquals("kcmapped", me.get("username").asText());
  }

  @Test
  void testBearerTokenUnmappedUserIsRejected() {
    String token = getKeycloakAccessToken("kcunmapped", "KcUnmapped123###");

    ResponseEntity<String> response = getWithBearerJwt(serverApiUrl + "/me", token);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(
        response
            .getBody()
            .contains(
                "Found no matching DHIS2 user for the mapping claim: 'email' with the value:"),
        "unexpected body: " + response.getBody());
  }

  @Test
  void testGarbageBearerTokenIsRejected() {
    ResponseEntity<String> response = getWithBearerJwt(serverApiUrl + "/me", "garbage.token.value");

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void testRequiresAuthorityDeniedOverBearerToken() throws JsonProcessingException {
    String token = getKeycloakAccessToken("kclimited", "KcLimited123###");

    ResponseEntity<String> response = postWithBearerJwt(serverApiUrl + MAINTENANCE_PATH, token);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "body: " + response.getBody());
    JsonNode json = objectMapper.readTree(response.getBody());
    assertEquals(
        "Access is denied, requires one Authority from [F_PERFORM_MAINTENANCE]",
        json.get("message").asText());
  }

  @Test
  void testRequiresAuthoritySuperuserBypassOverBearerToken() {
    String token = getKeycloakAccessToken("kcmapped", "KcMapped123###");

    ResponseEntity<String> response = postWithBearerJwt(serverApiUrl + MAINTENANCE_PATH, token);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "body: " + response.getBody());
  }
}
