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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.login.LoginResponse;
import org.hisp.dhis.login.LoginResponse.STATUS;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * E2e coverage for LDAP bind authentication ({@code CustomLdapAuthenticationProvider} / {@code
 * DhisBindAuthenticator}) against the OpenLDAP container in the auth-idp stack
 * (docker-compose.e2e-auth.yml, seeded from config/ldap/users.ldif).
 *
 * <p>Pinned contract: a DHIS2 user routes to LDAP only with {@code ldapId} set AND {@code
 * externalAuth=true}; the {@code (cn={0})} search filter is filled with {@code User.ldapId} (NOT
 * the username — the fixtures deliberately differ); authorities always come from DHIS2 roles;
 * password validation for such users happens exclusively against LDAP; non-LDAP users keep
 * DB-password login while LDAP is enabled; DHIS2 account status (disabled) still applies to LDAP
 * users; the same rules hold for HTTP Basic, which goes through the same authentication manager.
 *
 * <p>Migrated users retain a usable database password by enabling external authentication without
 * supplying a new password. Their fallback-rejection cases use a separate fixture, since successful
 * LDAP authentication replaces that retained hash.
 *
 * <p>The directory accepts unauthenticated binds (DN with an empty password, see
 * config/ldap/allow-unauthenticated-bind.ldif), so the empty-password cases prove that DHIS2 and
 * Spring reject them on their own rather than relying on the directory.
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
    // ldapuser3 -> disabled DHIS2 account, LDAP entry cn=ldapcn3 with a valid password
    createUser("ldapuser3", "ldapcn3", SUPER_USER_ROLE_UID, true, true);
    // ldapuser4 -> ldapId set but externalAuth=false, LDAP entry cn=ldapcn4
    createUser("ldapuser4", "ldapcn4", SUPER_USER_ROLE_UID, false, false);
    // ldapuser5 -> ldapId with no matching LDAP entry
    createLdapUser("ldapuser5", "ldapmissingcn", SUPER_USER_ROLE_UID);
    // Keep the migrated user's hash intact: successful LDAP authentication replaces it.
    // Only the DB-password rejection cases use ldapuser6, never an LDAP-success case.
    createLdapUser("ldapuser6", "ldapcn6", SUPER_USER_ROLE_UID);
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
    ResponseEntity<LoginResponse> response = tryLogin("ldapuser6", DHIS_STORED_PASSWORD);
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

    // superuser (ALL) bypass over an LDAP session; on 2.42 this endpoint answers 204
    String superCookie = performInitialLogin("ldapuser1", "ldapPass1###");
    ResponseEntity<String> allowed = postWithCookie(MAINTENANCE_PATH, null, superCookie);
    assertEquals(HttpStatus.NO_CONTENT, allowed.getStatusCode(), "body: " + allowed.getBody());
  }

  @Test
  void testNonLdapUserKeepsDbPasswordLogin() {
    // LDAP being configured must not capture regular users (no ldapId/externalAuth)
    ResponseEntity<String> me = getWithAdminBasicAuth("/me", null);
    assertEquals(HttpStatus.OK, me.getStatusCode());
  }

  @Test
  void testLdapLoginWithEmptyPasswordIsRejected() {
    // The directory would accept a bind with this DN and no password (as anonymous),
    // so only DHIS2 / Spring can stop this login.
    ResponseEntity<LoginResponse> response = tryLogin("ldapuser1", "");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void testLdapIdWithoutDirectoryEntryIsRejected() {
    // ldapuser5 exists in DHIS2 with externalAuth, but the (cn={0}) search for its
    // ldapId finds nothing, so there is no DN to bind as; the DHIS2-stored password
    // is no fallback.
    ResponseEntity<LoginResponse> response = tryLogin("ldapuser5", DHIS_STORED_PASSWORD);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void testLdapIdWithoutExternalAuthDoesNotUseLdap() {
    // ldapId alone does not opt a user into LDAP: the directory password is rejected
    // and the DHIS2 password keeps working.
    ResponseEntity<LoginResponse> ldapPassword = tryLogin("ldapuser4", "ldapPass4###");
    assertEquals(HttpStatus.UNAUTHORIZED, ldapPassword.getStatusCode());

    String cookie = performInitialLogin("ldapuser4", DHIS_STORED_PASSWORD);
    assertEquals(HttpStatus.OK, getWithCookie("/me", cookie).getStatusCode());
  }

  @Test
  void testDisabledDhisAccountIsRejectedDespiteValidLdapPassword() {
    ResponseEntity<LoginResponse> response = tryLogin("ldapuser3", "ldapPass3###");
    assertLoginStatus(response, STATUS.ACCOUNT_DISABLED);

    ResponseEntity<String> me = getMeWithBasicAuth("ldapuser3", "ldapPass3###");
    assertEquals(HttpStatus.UNAUTHORIZED, me.getStatusCode());
  }

  @Test
  void testLdapUserAuthenticatesWithHttpBasic() throws JsonProcessingException {
    ResponseEntity<String> me = getMeWithBasicAuth("ldapuser1", "ldapPass1###");
    assertEquals(HttpStatus.OK, me.getStatusCode(), "body: " + me.getBody());
    assertEquals("ldapuser1", objectMapper.readTree(me.getBody()).get("username").asText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"wrongPassword###", DHIS_STORED_PASSWORD, ""})
  void testHttpBasicRejectsInvalidLdapPassword(String password) {
    ResponseEntity<String> me = getMeWithBasicAuth("ldapuser6", password);
    assertEquals(HttpStatus.UNAUTHORIZED, me.getStatusCode());
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

  private static ResponseEntity<String> getMeWithBasicAuth(String username, String password) {
    HttpHeaders headers = jsonHeaders();
    headers.setBasicAuth(username, password);
    return exchangeWithHeaders(restTemplate, "/me", HttpMethod.GET, null, headers);
  }

  private static void createLdapUser(String username, String ldapId, String userRoleId)
      throws JsonProcessingException {
    createUser(username, ldapId, userRoleId, true, false);
  }

  private static void createUser(
      String username, String ldapId, String userRoleId, boolean externalAuth, boolean disabled)
      throws JsonProcessingException {
    // Start with internal authentication so the database contains a real password hash.
    Map<String, Object> userMap =
        new HashMap<>(
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
                false,
                "disabled",
                false,
                "firstName",
                "Ldap",
                "surname",
                "User",
                "userRoles",
                List.of(Map.of("id", userRoleId)),
                "organisationUnits",
                List.of(Map.of("id", orgUnitUID))));

    ResponseEntity<String> response = postWithAdminBasicAuth("/users", userMap);
    assertEquals(
        HttpStatus.CREATED,
        response.getStatusCode(),
        "user creation failed: " + response.getBody());
    JsonNode json = objectMapper.readTree(response.getBody());
    String uid = json.get("response").get("uid").asText();
    assertFalse(uid.isBlank(), "created user must have a UID");

    ResponseEntity<String> me = getMeWithBasicAuth(username, DHIS_STORED_PASSWORD);
    assertEquals(
        HttpStatus.OK, me.getStatusCode(), "internal password must work before conversion");
    assertEquals(username, objectMapper.readTree(me.getBody()).get("username").asText());

    // Omit the password on update: supplying it with externalAuth=true replaces the hash
    // with PW_NO_INTERNAL_LOGIN, which cannot expose an accidental DB-password fallback.
    userMap.remove("password");
    userMap.put("externalAuth", externalAuth);
    userMap.put("disabled", disabled);
    ResponseEntity<String> update =
        exchangeWithHeaders(
            addAdminBasicAuthHeaders(new RestTemplate()),
            "/users/" + uid,
            HttpMethod.PUT,
            userMap,
            jsonHeaders());
    assertEquals(HttpStatus.OK, update.getStatusCode(), "user update failed: " + update.getBody());
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
