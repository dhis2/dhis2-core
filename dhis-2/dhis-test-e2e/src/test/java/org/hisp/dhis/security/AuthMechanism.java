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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.hisp.dhis.BaseE2ETest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * The API authentication mechanisms exercised by the authorization matrix tests. Each constant
 * turns (username, password) into {@link HttpHeaders} that authenticate subsequent requests via
 * that mechanism, so authorization outcomes can be asserted identically across mechanisms.
 *
 * <p>Not covered here: OAuth2/JWT bearer (needs the authorization-code dance, covered in {@code
 * OAuth2Test} and the auth-idp Keycloak suite) and OIDC/LDAP sessions (auth-idp suite).
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public enum AuthMechanism {
  /** Form login via /api/auth/login, then the session cookie. */
  SESSION_COOKIE,
  /** Preemptive HTTP Basic. */
  BASIC,
  /** Personal access token: created via /api/apiTokens, sent as "Authorization: ApiToken ...". */
  PAT;

  /** Returns headers that authenticate the given user via this mechanism. */
  public HttpHeaders authenticate(String username, String password) {
    HttpHeaders headers = BaseE2ETest.jsonHeaders();
    switch (this) {
      case SESSION_COOKIE ->
          headers.set("Cookie", BaseE2ETest.performInitialLogin(username, password));
      case BASIC -> headers.set(HttpHeaders.AUTHORIZATION, basicAuthHeader(username, password));
      case PAT ->
          headers.set(HttpHeaders.AUTHORIZATION, "ApiToken " + createPat(username, password));
    }
    return headers;
  }

  private static String basicAuthHeader(String username, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  /** The plaintext PAT key is only returned on creation. */
  private static String createPat(String username, String password) {
    String cookie = BaseE2ETest.performInitialLogin(username, password);
    ResponseEntity<String> response = BaseE2ETest.postWithCookie("/apiTokens", Map.of(), cookie);
    if (response.getStatusCode() != HttpStatus.CREATED) {
      throw new IllegalStateException("PAT creation failed: " + response.getBody());
    }
    try {
      JsonNode json = BaseE2ETest.objectMapper.readTree(response.getBody());
      JsonNode key = json.get("response").get("key");
      assertNotNull(key, "no PAT key in response: " + response.getBody());
      return key.asText();
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
