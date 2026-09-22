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
package org.hisp.dhis.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.login.LoginRequest;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end impersonation flow over a real session (cookie jar). Requires {@code
 * switch_user_feature.enabled = on} and the test container's IP in {@code
 * switch_user_allow_listed_ips} (see config/dhis2_home/dhis.conf + the fixed compose subnet).
 *
 * <p>Pinned contracts: the {@code /api/me} {@code impersonation} field carries the impersonator
 * USERNAME; authority checks while impersonating apply to the TARGET user; exit restores the
 * impersonator; impersonation state does NOT survive a re-login in the same browser session (see
 * {@link #reLoginDuringImpersonationInvalidatesExit()}).
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
class ImpersonationTest extends BaseE2ETest {

  private static final String PW = "Test123###...";
  private static final String MAINTENANCE_PATH = "/maintenance/categoryOptionComboUpdate";

  @BeforeAll
  static void setup() throws JsonProcessingException {
    serverApiUrl = TestConfiguration.get().baseUrl();
    serverHostUrl = serverApiUrl.replace("/api", "");
    orgUnitUID = createOrgUnit();

    createSuperuser("impadmin", PW, orgUnitUID);
    String emptyRole = createRole("impEmptyRole");
    String impersonatorRole = createRole("impUserRole", "F_IMPERSONATE_USER");
    createUser("imptarget", emptyRole);
    createUser("impguest", emptyRole);
    createUser("impuser", impersonatorRole);
  }

  @Test
  void impersonateActExitFullFlow() throws JsonProcessingException {
    String cookie = performInitialLogin("impadmin", PW);

    // enter impersonation
    ResponseEntity<String> enter =
        postWithCookie("/auth/impersonate?username=imptarget", null, cookie);
    assertEquals(HttpStatus.OK, enter.getStatusCode(), "body: " + enter.getBody());
    JsonNode enterJson = objectMapper.readTree(enter.getBody());
    assertEquals("IMPERSONATION_SUCCESS", enterJson.get("status").asText());
    assertEquals("imptarget", enterJson.get("username").asText());

    // /me shows the target AND the impersonator USERNAME (not display name)
    JsonNode me = getMe(cookie);
    assertEquals("imptarget", me.get("username").asText());
    assertEquals("impadmin", me.get("impersonation").asText());
    assertNull(me.get("canImpersonate"), "target has no impersonation authority");

    // authority checks apply to the TARGET while impersonating
    ResponseEntity<String> denied = postWithCookie(MAINTENANCE_PATH, null, cookie);
    assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode(), "body: " + denied.getBody());

    // exit restores the impersonator
    ResponseEntity<String> exit = postWithCookie("/auth/impersonateExit", null, cookie);
    assertEquals(HttpStatus.OK, exit.getStatusCode(), "body: " + exit.getBody());
    JsonNode exitJson = objectMapper.readTree(exit.getBody());
    assertEquals("IMPERSONATION_EXIT_SUCCESS", exitJson.get("status").asText());

    JsonNode meAfter = getMe(cookie);
    assertEquals("impadmin", meAfter.get("username").asText());
    assertNull(meAfter.get("impersonation"), "impersonation field must be gone after exit");
    assertTrue(meAfter.get("canImpersonate").asBoolean(), "feature on + ALL -> canImpersonate");
  }

  @Test
  void impersonateWithoutAuthorityDeniedWithMessageContract() throws JsonProcessingException {
    String cookie = performInitialLogin("impguest", PW);

    ResponseEntity<String> response =
        postWithCookie("/auth/impersonate?username=imptarget", null, cookie);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "body: " + response.getBody());
    JsonNode json = objectMapper.readTree(response.getBody());
    assertEquals(
        "Access is denied, requires one Authority from [F_IMPERSONATE_USER]",
        json.get("message").asText());
  }

  @Test
  void nonSuperuserCannotImpersonateSuperuser() throws JsonProcessingException {
    String cookie = performInitialLogin("impuser", PW);

    ResponseEntity<String> response =
        postWithCookie("/auth/impersonate?username=impadmin", null, cookie);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertMessage(response, "Forbidden, reason: User is not authorized to impersonate super user");
  }

  @Test
  void impersonateSelfIsRejected() throws JsonProcessingException {
    String cookie = performInitialLogin("impadmin", PW);

    ResponseEntity<String> response =
        postWithCookie("/auth/impersonate?username=impadmin", null, cookie);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertMessage(response, "Forbidden, reason: User can not impersonate itself");
  }

  @Test
  void exitWithoutImpersonatingIsBadRequest() throws JsonProcessingException {
    String cookie = performInitialLogin("impadmin", PW);

    ResponseEntity<String> response = postWithCookie("/auth/impersonateExit", null, cookie);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertMessage(response, "User not impersonating anyone, user: impadmin");
  }

  /**
   * PIN: impersonation state lives in the SecurityContext and is atomically replaced when someone
   * re-authenticates in the same browser session; the new principal cannot "exit" into the old
   * impersonator. A redesign that stores the impersonator elsewhere (e.g. a session attribute) MUST
   * NOT change this outcome.
   */
  @Test
  void reLoginDuringImpersonationInvalidatesExit() throws JsonProcessingException {
    String cookie = performInitialLogin("impadmin", PW);

    ResponseEntity<String> enter =
        postWithCookie("/auth/impersonate?username=imptarget", null, cookie);
    assertEquals(HttpStatus.OK, enter.getStatusCode());

    // re-authenticate as an unprivileged user within the same session/cookie jar
    ResponseEntity<String> reLogin =
        postWithCookie(
            "/auth/login",
            LoginRequest.builder().username("impguest").password(PW).build(),
            cookie);
    assertEquals(HttpStatus.OK, reLogin.getStatusCode(), "body: " + reLogin.getBody());
    // re-login sets several cookies (SESSION_EXPIRE, JSESSIONID); pick the session one
    List<String> setCookies = reLogin.getHeaders().get(HttpHeaders.SET_COOKIE);
    String newCookie =
        setCookies == null
            ? null
            : setCookies.stream().filter(c -> c.startsWith("JSESSIONID")).findFirst().orElse(null);
    String effectiveCookie = newCookie != null ? newCookie : cookie;

    JsonNode me = getMe(effectiveCookie);
    assertEquals("impguest", me.get("username").asText());

    // the fresh principal has no impersonation state or authority -> exit is denied
    ResponseEntity<String> exit = postWithCookie("/auth/impersonateExit", null, effectiveCookie);
    assertEquals(HttpStatus.FORBIDDEN, exit.getStatusCode(), "body: " + exit.getBody());
    JsonNode exitJson = objectMapper.readTree(exit.getBody());
    assertEquals(
        "Access is denied, requires one Authority from [F_IMPERSONATE_USER,"
            + " F_PREVIOUS_IMPERSONATOR_AUTHORITY]",
        exitJson.get("message").asText());
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static JsonNode getMe(String cookie) throws JsonProcessingException {
    ResponseEntity<String> me =
        getWithCookie("/me?fields=username,impersonation,canImpersonate", cookie);
    assertEquals(HttpStatus.OK, me.getStatusCode(), "body: " + me.getBody());
    JsonNode json = objectMapper.readTree(me.getBody());
    assertNotNull(json);
    return json;
  }

  private static String createRole(String name, String... authorities)
      throws JsonProcessingException {
    ResponseEntity<String> response =
        postWithAdminBasicAuth(
            "/userRoles", Map.of("name", name, "authorities", List.of(authorities)));
    assertEquals(
        HttpStatus.CREATED,
        response.getStatusCode(),
        "role creation failed: " + response.getBody());
    return objectMapper.readTree(response.getBody()).get("response").get("uid").asText();
  }

  private static void createUser(String username, String roleId) throws JsonProcessingException {
    Map<String, Object> userMap =
        Map.of(
            "username",
            username,
            "password",
            PW,
            "email",
            username + "@dhis2.org",
            "firstName",
            "Impersonation",
            "surname",
            "User",
            "userRoles",
            List.of(Map.of("id", roleId)),
            "organisationUnits",
            List.of(Map.of("id", orgUnitUID)));
    ResponseEntity<String> response = postWithAdminBasicAuth("/users", userMap);
    assertEquals(
        HttpStatus.CREATED,
        response.getStatusCode(),
        "user creation failed: " + response.getBody());
  }
}
