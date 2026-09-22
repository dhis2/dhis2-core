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
package org.hisp.dhis.webapi.controller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.HashUtils;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the structured {@code /api/sessions} response and per-user session invalidation.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class SessionControllerTest extends H2ControllerIntegrationTestBase {

  private static final String SESSION_ID = "session-controller-test-1";

  @Autowired private SessionRegistry sessionRegistry;

  @AfterEach
  void tearDown() {
    sessionRegistry.removeSessionInformation(SESSION_ID);
  }

  @Test
  void listSessionsReturnsStructuredSessionInfo() {
    UserDetails principal = userService.createUserDetails(getAdminUser());
    sessionRegistry.registerNewSession(SESSION_ID, principal);

    JsonArray sessions = GET("/api/sessions").content(HttpStatus.OK).as(JsonArray.class);

    JsonObject match =
        sessions.asList(JsonObject.class).stream()
            .filter(s -> "admin".equals(s.getString("username").string()))
            .filter(
                s -> HashUtils.hashSHA1(SESSION_ID.getBytes()).equals(s.getString("id").string()))
            .findFirst()
            .orElseThrow();

    assertEquals(HashUtils.hashSHA1(SESSION_ID.getBytes()), match.getString("id").string());
    assertEquals("admin", match.getString("username").string());
    assertFalse(match.getBoolean("expired").booleanValue());
    assertNotNull(match.get("lastRequest").node());
    assertFalse(match.get("lastRequest").isNull());
  }

  @Test
  void deleteUsernameExpiresRegisteredSession() {
    UserDetails principal = userService.createUserDetails(getAdminUser());
    sessionRegistry.registerNewSession(SESSION_ID, principal);
    assertFalse(sessionRegistry.getAllSessions(principal, false).isEmpty());

    assertEquals(HttpStatus.OK, DELETE("/api/sessions/admin").status());

    assertTrue(sessionRegistry.getAllSessions(principal, false).isEmpty());
    SessionInformation expired = sessionRegistry.getSessionInformation(SESSION_ID);
    assertNotNull(expired);
    assertTrue(expired.isExpired());
  }
}
