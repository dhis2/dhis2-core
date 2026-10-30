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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.login.CodeGenerator;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

/**
 * @author Torbjørn Lindahl
 */
@Slf4j
class ApiTokenAuthTest extends BaseE2ETest {

  @BeforeAll
  static void setup() throws JsonProcessingException {
    serverApiUrl = TestConfiguration.get().baseUrl();
    serverHostUrl = TestConfiguration.get().baseUrl().replace("/api", "");
    orgUnitUID = createOrgUnit();
  }

  /**
   * Regression test for LazyInitializationException on User.userRoles when authenticating with a
   * personal access token (PAT) on an endpoint excluded from open-session-in-view ({@code
   * ConditionalOpenEntityManagerInViewFilter}), such as {@code /api/dataValueSets}. Before the fix,
   * {@code ApiTokenAuthManager} looked up the token owner and created {@code UserDetails} outside a
   * transaction; the first request with a not-yet-cached token on such an endpoint then failed with
   * a 500 error. The fix creates the {@code UserDetails} within a single transaction via {@code
   * UserService.createUserDetailsByUsername}, mirroring the JWT bearer fix (#23611).
   */
  @Test
  void testPatAuthenticationOnNonOsivEndpoint() throws JsonProcessingException {
    String username = CodeGenerator.generateCode(8).toLowerCase();
    String password = "Test123###...";

    createSuperuser(username, password, orgUnitUID);

    // Create a PAT as the new user; the plaintext key is only returned on creation.
    String cookie = performInitialLogin(username, password);
    ResponseEntity<String> tokenResponse = postWithCookie("/apiTokens", Map.of(), cookie);
    assertEquals(HttpStatus.CREATED, tokenResponse.getStatusCode());
    JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());
    String plaintextToken = tokenJson.get("response").get("key").asText();
    assertNotNull(plaintextToken);

    // First request with this token: the token is not in the server's token cache, so
    // authentication resolves the owner and creates UserDetails during this request. The endpoint
    // is excluded from OSIV, so no request-scoped Hibernate session exists. Before the fix this
    // returned 500 (LazyInitializationException on User.userRoles); with the fix, authentication
    // succeeds and the endpoint's parameter validation responds 409 Conflict.
    ResponseEntity<String> response = getWithApiTokenAuth("/dataValueSets", plaintextToken);
    log.info(
        "dataValueSets response: status={}, body={}", response.getStatusCode(), response.getBody());

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }

  private static ResponseEntity<String> getWithApiTokenAuth(String path, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, "ApiToken " + token);
    try {
      return restTemplate.exchange(
          serverApiUrl + path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    } catch (RestClientResponseException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }
}
