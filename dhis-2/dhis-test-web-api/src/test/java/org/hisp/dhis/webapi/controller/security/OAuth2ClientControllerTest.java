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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for {@link org.hisp.dhis.webapi.controller.security.oauth.OAuth2ClientController}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class OAuth2ClientControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testListOrderedByDisplayName() {
    // Reproduces the bug where ORDER BY displayName on an entity without a
    // persisted `name` column (pre-fix Dhis2OAuth2Client) crashed with
    // IllegalArgumentException inside JpaCriteriaQueryEngine. Filter by our
    // known prefix to ignore any system-seeded clients (e.g. DCR registrar).
    createClient("list-test-b", "Bravo Client");
    createClient("list-test-a", "Alpha Client");

    JsonObject response =
        GET("/oAuth2Clients?order=displayName&fields=id,name,clientId").content(HttpStatus.OK);
    List<String> names =
        response.getList("oAuth2Clients", JsonObject.class).stream()
            .filter(c -> c.getString("clientId").string().startsWith("list-test-"))
            .map(c -> c.getString("name").string())
            .toList();

    assertTrue(
        names.indexOf("Alpha Client") >= 0
            && names.indexOf("Alpha Client") < names.indexOf("Bravo Client"),
        "Expected Alpha before Bravo, got: " + names);
  }

  @Test
  void testCreatePersistsName() {
    String uid = createClient("client-c", "Charlie Client");

    JsonObject client = GET("/oAuth2Clients/{id}", uid).content(HttpStatus.OK);
    assertEquals("Charlie Client", client.getString("name").string());
    assertEquals("client-c", client.getString("clientId").string());
  }

  private String createClient(String clientId, String name) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/oAuth2Clients",
            "{"
                + "'name':'"
                + name
                + "',"
                + "'clientId':'"
                + clientId
                + "',"
                + "'clientSecret':'secret',"
                + "'clientAuthenticationMethods':'client_secret_basic',"
                + "'authorizationGrantTypes':'authorization_code,refresh_token',"
                + "'redirectUris':'https://example.com/callback',"
                + "'scopes':'openid'"
                + "}"));
  }
}
