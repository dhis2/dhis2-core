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
package org.hisp.dhis.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.login.LoginResponse;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

/**
 * E2e coverage for LDAP bind authentication ({@code CustomLdapAuthenticationProvider} / {@code
 * DhisBindAuthenticator}) against the OpenLDAP container in the auth-idp stack
 * (docker-compose.e2e-auth.yml, seeded from config/ldap/users.ldif).
 *
 * <p>Pinned contract: a DHIS2 user routes to LDAP only with {@code ldapId} set AND {@code
 * externalAuth=true}; the {@code (cn={0})} search filter is filled with {@code User.ldapId} (NOT
 * the username — the fixtures deliberately differ); authorities always come from DHIS2 roles;
 * password validation for such users happens exclusively against LDAP; non-LDAP users keep
 * DB-password login while LDAP is enabled.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Tag("auth-idp")
@Slf4j
class LdapLoginTest extends BaseE2ETest {

  private static final String MAINTENANCE_PATH = "/maintenance/categoryOptionComboUpdate";

  private static final String DHIS_STORED_PASSWORD = "DbStored123###";

  @BeforeAll
  static void setup() throws JsonProcessingException {
    serverApiUrl = TestConfiguration.get().baseUrl();
    serverHostUrl = serverApiUrl.replace("/api", "");
    orgUnitUID = createOrgUnit();

    // ldapuser1 -> superuser role, LDAP entry cn=ldapcn1
    createLdapUser("ldapuser1", "ldapcn1", SUPER_USER_ROLE_UID);
    // ldapuser2 -> role without authorities, LDAP entry cn=ldapcn2
    String emptyRole = createEmptyRole("authIdpEmptyRoleLdap");
    createLdapUser("ldapuser2", "ldapcn2", emptyRole);
  }

  @AfterAll
  static void tearDown() {
    invalidateAllSession();
  }

  @Test
  void testLdapLoginSuccessAndAuthoritiesFromDhisRoles() throws JsonProcessingException {
    String cookie = performInitialLogin("ldapuser1", "ldapPass1###");

    ResponseEntity<String> me = getWithCookie("/me", cookie);
    assertEquals(HttpStatus.OK, me.getStatusCode(), "body: " + me.getBody());
    JsonNode json = objectMapper.readTree(me.getBody());
    assertEquals("ldapuser1", json.get("username").asText());
    // authorities come from the DHIS2 superuser role, never from the directory
    assertTrue(json.get("authorities").toString().contains("ALL"), "body: " + me.getBody());
  }

  @Test
  void testLdapLoginWrongPasswordIsRejected() {
    ResponseEntity<LoginResponse> response = tryLogin("ldapuser1", "wrongPassword###");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void testLdapUserRejectsDhisStoredPassword() {
    // PIN: for ldapId+externalAuth users the DB password is never consulted;
    // the stored DHIS2 password must NOT authenticate.
    ResponseEntity<LoginResponse> response = tryLogin("ldapuser1", DHIS_STORED_PASSWORD);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void testLdapEntryWithoutDhisUserIsRejected() {
    // cn=ldaporphancn exists in LDAP but has no DHIS2 user -> lookup by DHIS2
    // username fails before any bind is attempted.
    ResponseEntity<LoginResponse> response = tryLogin("ldaporphancn", "ldapOrphan###");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void testRequiresAuthorityContractOverLdapSession() throws JsonProcessingException {
    String limitedCookie = performInitialLogin("ldapuser2", "ldapPass2###");

    ResponseEntity<String> denied = postWithCookie(MAINTENANCE_PATH, null, limitedCookie);
    assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode(), "body: " + denied.getBody());
    JsonNode json = objectMapper.readTree(denied.getBody());
    assertEquals(
        "Access is denied, requires one Authority from [F_PERFORM_MAINTENANCE]",
        json.get("message").asText());

    // superuser (ALL) bypass over an LDAP session
    String superCookie = performInitialLogin("ldapuser1", "ldapPass1###");
    ResponseEntity<String> allowed = postWithCookie(MAINTENANCE_PATH, null, superCookie);
    assertEquals(HttpStatus.OK, allowed.getStatusCode(), "body: " + allowed.getBody());
  }

  @Test
  void testNonLdapUserKeepsDbPasswordLogin() {
    // LDAP being configured must not capture regular users (no ldapId/externalAuth)
    ResponseEntity<String> me = getWithAdminBasicAuth("/me", null);
    assertEquals(HttpStatus.OK, me.getStatusCode());
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static ResponseEntity<LoginResponse> tryLogin(String username, String password) {
    try {
      return loginWithUsernameAndPassword(username, password, null);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).build();
    }
  }

  private static void createLdapUser(String username, String ldapId, String userRoleId)
      throws JsonProcessingException {
    Map<String, Object> userMap =
        Map.of(
            "username",
            username,
            "password",
            DHIS_STORED_PASSWORD,
            "email",
            username + "@dhis2.org",
            "ldapId",
            ldapId,
            "externalAuth",
            true,
            "firstName",
            "Ldap",
            "surname",
            "User",
            "userRoles",
            List.of(Map.of("id", userRoleId)),
            "organisationUnits",
            List.of(Map.of("id", orgUnitUID)));

    ResponseEntity<String> response = postWithAdminBasicAuth("/users", userMap);
    assertEquals(
        HttpStatus.CREATED,
        response.getStatusCode(),
        "user creation failed: " + response.getBody());
    JsonNode json = objectMapper.readTree(response.getBody());
    assertNotNull(json.get("response").get("uid").asText());
  }

  private static String createEmptyRole(String name) throws JsonProcessingException {
    ResponseEntity<String> response =
        postWithAdminBasicAuth("/userRoles", Map.of("name", name, "authorities", List.of()));
    assertEquals(
        HttpStatus.CREATED,
        response.getStatusCode(),
        "role creation failed: " + response.getBody());
    JsonNode json = objectMapper.readTree(response.getBody());
    return json.get("response").get("uid").asText();
  }
}
