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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.login.CodeGenerator;
import org.hisp.dhis.login.LoginTest;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Authorization-outcome matrix over the non-interactive API authentication mechanisms ({@link
 * AuthMechanism}: session cookie, basic, PAT). Every outcome must be IDENTICAL regardless of how
 * the request authenticated; a divergence means an authentication filter is leaking into
 * authorization decisions.
 *
 * <p>Pinned contracts: the exact {@code @RequiresAuthority} 403 message, the implicit ALL bypass,
 * metadata sharing (private objects are invisible to others), org-unit capture scoping on data
 * value writes, and basic-auth rejection for TOTP-2FA-enabled users.
 *
 * <p>Bearer-token variants of the 403 contract live in {@code OAuth2Test} and the auth-idp
 * Keycloak/LDAP suites, which own the required token infrastructure.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
class AuthorizationMatrixTest extends BaseE2ETest {

  private static final String PW = "Test123###...";
  private static final String MAINTENANCE_PATH = "/maintenance/categoryOptionComboUpdate";
  private static final String DENIED_MESSAGE =
      "Access is denied, requires one Authority from [F_PERFORM_MAINTENANCE]";

  private static String orgUnitB;
  private static String privateDataElement;
  private static String publicDataElement;
  private static String dataSet;

  @BeforeAll
  static void setup() throws JsonProcessingException {
    serverApiUrl = TestConfiguration.get().baseUrl();
    serverHostUrl = serverApiUrl.replace("/api", "");
    orgUnitUID = createOrgUnit(); // capture org unit "A"
    orgUnitB = createOrgUnit(); // outside the capture user's hierarchy

    String emptyRole = createRole("matrixEmptyRole");
    String maintRole = createRole("matrixMaintRole", "F_PERFORM_MAINTENANCE");
    String ownerRole = createRole("matrixOwnerRole", "F_DATAELEMENT_PRIVATE_ADD");
    String captureRole = createRole("matrixCaptureRole", "F_DATAVALUE_ADD");

    createUser("matrixnoauth", emptyRole, orgUnitUID);
    createUser("matrixmaint", maintRole, orgUnitUID);
    createSuperuser("matrixsuper", PW, orgUnitUID);
    createUser("matrixowner", ownerRole, orgUnitUID);
    createUser("matrixother", emptyRole, orgUnitUID);
    createUser("matrixcapture", captureRole, orgUnitUID);

    privateDataElement = createPrivateDataElementAsOwner();
    setupAggregateData();
  }

  // ---------------------------------------------------------------------------
  // @RequiresAuthority outcomes, identical across mechanisms
  // ---------------------------------------------------------------------------

  @ParameterizedTest(name = "{0}")
  @EnumSource(AuthMechanism.class)
  void requiresAuthorityDeniedWithExactMessage(AuthMechanism mechanism)
      throws JsonProcessingException {
    HttpHeaders headers = mechanism.authenticate("matrixnoauth", PW);

    ResponseEntity<String> response = exchange(HttpMethod.POST, MAINTENANCE_PATH, headers);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "body: " + response.getBody());
    JsonNode json = objectMapper.readTree(response.getBody());
    assertEquals(DENIED_MESSAGE, json.get("message").asText());
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(AuthMechanism.class)
  void requiresAuthorityGranted(AuthMechanism mechanism) {
    HttpHeaders headers = mechanism.authenticate("matrixmaint", PW);

    ResponseEntity<String> response = exchange(HttpMethod.POST, MAINTENANCE_PATH, headers);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "body: " + response.getBody());
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(AuthMechanism.class)
  void requiresAuthoritySuperuserAllBypass(AuthMechanism mechanism) {
    HttpHeaders headers = mechanism.authenticate("matrixsuper", PW);

    ResponseEntity<String> response = exchange(HttpMethod.POST, MAINTENANCE_PATH, headers);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "body: " + response.getBody());
  }

  // ---------------------------------------------------------------------------
  // Sharing ACL, identical across mechanisms
  // ---------------------------------------------------------------------------

  @ParameterizedTest(name = "{0}")
  @EnumSource(AuthMechanism.class)
  void sharingHidesPrivateMetadataFromOthers(AuthMechanism mechanism) {
    String path = "/dataElements/" + privateDataElement;

    // PIN: no metadata read access presents as 404, not 403
    ResponseEntity<String> other =
        exchange(HttpMethod.GET, path, mechanism.authenticate("matrixother", PW));
    assertEquals(HttpStatus.NOT_FOUND, other.getStatusCode(), "body: " + other.getBody());

    // write attempt by a non-reader is equally invisible
    ResponseEntity<String> otherDelete =
        exchange(HttpMethod.DELETE, path, mechanism.authenticate("matrixother", PW));
    assertEquals(HttpStatus.NOT_FOUND, otherDelete.getStatusCode());

    ResponseEntity<String> owner =
        exchange(HttpMethod.GET, path, mechanism.authenticate("matrixowner", PW));
    assertEquals(HttpStatus.OK, owner.getStatusCode(), "body: " + owner.getBody());

    ResponseEntity<String> superuser =
        exchange(HttpMethod.GET, path, mechanism.authenticate("matrixsuper", PW));
    assertEquals(HttpStatus.OK, superuser.getStatusCode());
  }

  // ---------------------------------------------------------------------------
  // Org-unit capture scope (mechanism-independent service layer; exercised via basic)
  // ---------------------------------------------------------------------------

  @Test
  void orgUnitCaptureScopeEnforcedOnDataValueWrite() throws JsonProcessingException {
    HttpHeaders headers = AuthMechanism.BASIC.authenticate("matrixcapture", PW);

    // outside the user's capture hierarchy -> rejected
    ResponseEntity<String> outside = postDataValue(headers, orgUnitB);
    assertNotNull(outside.getBody());
    // PIN: capture-scope rejection is conflict E8011
    assertTrue(
        outside.getBody().contains("Current user cannot enter data for org unit"),
        "expected E8011 capture-scope conflict, got: " + outside.getBody());
    assertTrue(outside.getBody().contains("\"errorCode\":\"E8011\""), "body: " + outside.getBody());
    assertFalse(isImported(outside.getBody()), "value outside hierarchy must not be imported");

    // inside the capture hierarchy -> imported
    ResponseEntity<String> inside = postDataValue(headers, orgUnitUID);
    assertTrue(isImported(inside.getBody()), "expected import success, got: " + inside.getBody());
  }

  // ---------------------------------------------------------------------------
  // Basic auth vs TOTP 2FA
  // ---------------------------------------------------------------------------

  @Test
  void basicAuthRejectedForTotpEnabledUser() throws JsonProcessingException {
    String username = CodeGenerator.generateCode(8).toLowerCase();
    createSuperuser(username, PW, orgUnitUID);

    String cookie = performInitialLogin(username, PW);
    String secret = LoginTest.enrollTOTP2FA(cookie);
    // completing a 2FA login activates TOTP for the account
    loginWith2FA(username, PW, new Totp(secret).now());

    ResponseEntity<String> response =
        exchange(HttpMethod.GET, "/me", AuthMechanism.BASIC.authenticate(username, PW));

    // PIN: basic auth carries no 2FA code and must be rejected once TOTP is enabled
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "body: " + response.getBody());
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static ResponseEntity<String> exchange(
      HttpMethod method, String path, HttpHeaders headers) {
    try {
      return restTemplate.exchange(
          serverApiUrl + path, method, new HttpEntity<>("", headers), String.class);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }

  private static ResponseEntity<String> postDataValue(HttpHeaders headers, String orgUnit) {
    Map<String, Object> payload =
        Map.of(
            "dataSet",
            dataSet,
            // previous month: the current period has not ended yet and would need
            // openFuturePeriods > 0 (E8030), while expiryDays=0 keeps past periods open
            "period",
            YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM")),
            "orgUnit",
            orgUnit,
            "dataValues",
            List.of(Map.of("dataElement", publicDataElement, "value", "1")));
    try {
      return restTemplate.exchange(
          serverApiUrl + "/dataValueSets",
          HttpMethod.POST,
          new HttpEntity<>(payload, headers),
          String.class);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }

  /** dataValueSets counts a first-time write as "updated", so check both counters. */
  private static boolean isImported(String importResponseBody) throws JsonProcessingException {
    JsonNode json = objectMapper.readTree(importResponseBody);
    JsonNode summary = json.has("response") ? json.get("response") : json;
    JsonNode importCount = summary.get("importCount");
    return importCount != null
        && importCount.get("imported").asInt() + importCount.get("updated").asInt() > 0;
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

  private static void createUser(String username, String roleId, String orgUnit)
      throws JsonProcessingException {
    Map<String, Object> userMap =
        Map.of(
            "username",
            username,
            "password",
            PW,
            "email",
            username + "@dhis2.org",
            "firstName",
            "Matrix",
            "surname",
            "User",
            "userRoles",
            List.of(Map.of("id", roleId)),
            "organisationUnits",
            List.of(Map.of("id", orgUnit)));
    ResponseEntity<String> response = postWithAdminBasicAuth("/users", userMap);
    assertEquals(
        HttpStatus.CREATED,
        response.getStatusCode(),
        "user creation failed: " + response.getBody());
  }

  /** Created by matrixowner with public access "--------": visible to the owner and supers only. */
  private static String createPrivateDataElementAsOwner() throws JsonProcessingException {
    String cookie = performInitialLogin("matrixowner", PW);
    Map<String, Object> payload =
        Map.of(
            "name", "MatrixPrivateDE",
            "shortName", "MatrixPrivateDE",
            "valueType", "INTEGER",
            "aggregationType", "SUM",
            "domainType", "AGGREGATE",
            "sharing", Map.of("public", "--------"));
    ResponseEntity<String> response = postWithCookie("/dataElements", payload, cookie);
    assertEquals(
        HttpStatus.CREATED, response.getStatusCode(), "DE creation failed: " + response.getBody());
    return objectMapper.readTree(response.getBody()).get("response").get("uid").asText();
  }

  /** Public data element + data set (data-writable by all) assigned to org units A and B. */
  private static void setupAggregateData() throws JsonProcessingException {
    Map<String, Object> dePayload =
        Map.of(
            "name", "MatrixPublicDE",
            "shortName", "MatrixPublicDE",
            "valueType", "INTEGER",
            "aggregationType", "SUM",
            "domainType", "AGGREGATE",
            "sharing", Map.of("public", "rwrw----"));
    ResponseEntity<String> deResponse = postWithAdminBasicAuth("/dataElements", dePayload);
    assertEquals(HttpStatus.CREATED, deResponse.getStatusCode(), "body: " + deResponse.getBody());
    publicDataElement =
        objectMapper.readTree(deResponse.getBody()).get("response").get("uid").asText();

    Map<String, Object> dsPayload =
        Map.of(
            "name",
            "MatrixDataSet",
            "shortName",
            "MatrixDataSet",
            "periodType",
            "Monthly",
            "dataSetElements",
            List.of(Map.of("dataElement", Map.of("id", publicDataElement))),
            "organisationUnits",
            List.of(Map.of("id", orgUnitUID), Map.of("id", orgUnitB)),
            "sharing",
            Map.of("public", "rwrw----"));
    ResponseEntity<String> dsResponse = postWithAdminBasicAuth("/dataSets", dsPayload);
    assertEquals(HttpStatus.CREATED, dsResponse.getStatusCode(), "body: " + dsResponse.getBody());
    dataSet = objectMapper.readTree(dsResponse.getBody()).get("response").get("uid").asText();
  }
}
