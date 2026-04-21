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
  void testSchemaValidationAcceptsMissingName() {
    // The settings UI POSTs a payload without `name` to /api/schemas/oAuth2Client
    // for pre-validation before the real create. That endpoint calls the schema
    // validator directly (no controller hook), so the `name` property must not
    // be marked required in the schema. The controller still defaults name to
    // clientId on the actual POST.
    JsonObject response =
        POST(
                "/schemas/oAuth2Client",
                "{"
                    + "'clientId':'schema-probe',"
                    + "'clientSecret':'secret',"
                    + "'clientAuthenticationMethods':'client_secret_basic',"
                    + "'authorizationGrantTypes':'authorization_code',"
                    + "'redirectUris':'https://example.com/callback',"
                    + "'scopes':'openid'"
                    + "}")
            .content(HttpStatus.OK);
    assertEquals("OK", response.getString("status").string());
  }

  @Test
  void testCreatePersistsName() {
    String uid = createClient("client-c", "Charlie Client");

    JsonObject client = GET("/oAuth2Clients/{id}", uid).content(HttpStatus.OK);
    assertEquals("Charlie Client", client.getString("name").string());
    assertEquals("client-c", client.getString("clientId").string());
  }

  @Test
  void testUpdateWithoutNamePreservesExistingName() {
    // The settings UI has no name field, so PUTs omit it. Preserve the
    // existing persisted name rather than clobbering with clientId via
    // REPLACE merge.
    String uid = createClient("client-keep", "Original Name");

    assertStatus(
        HttpStatus.OK,
        PUT(
            "/oAuth2Clients/" + uid,
            "{"
                + "'clientId':'client-keep',"
                + "'clientSecret':'secret',"
                + "'clientAuthenticationMethods':'client_secret_basic',"
                + "'authorizationGrantTypes':'authorization_code,refresh_token',"
                + "'redirectUris':'https://example.com/callback',"
                + "'scopes':'openid'"
                + "}"));

    JsonObject client = GET("/oAuth2Clients/{id}", uid).content(HttpStatus.OK);
    assertEquals("Original Name", client.getString("name").string());
  }

  @Test
  void testRejectsJavascriptSchemeRedirectUri() {
    // Spring Authorization Server emits Location: <storedRedirectUri>?code=...
    // after exact-string match. A stored javascript: URI would execute in the
    // victim's browser. Must be rejected at save time.
    assertStatus(
        HttpStatus.CONFLICT,
        POST(
            "/oAuth2Clients",
            "{"
                + "'clientId':'client-js',"
                + "'clientSecret':'secret',"
                + "'clientAuthenticationMethods':'client_secret_basic',"
                + "'authorizationGrantTypes':'authorization_code',"
                + "'redirectUris':'javascript:alert(1)',"
                + "'scopes':'openid'"
                + "}"));
  }

  @Test
  void testRejectsDataSchemeRedirectUri() {
    assertStatus(
        HttpStatus.CONFLICT,
        POST(
            "/oAuth2Clients",
            "{"
                + "'clientId':'client-data',"
                + "'clientSecret':'secret',"
                + "'clientAuthenticationMethods':'client_secret_basic',"
                + "'authorizationGrantTypes':'authorization_code',"
                + "'redirectUris':'data:text/html,<script>fetch(\"//x\")</script>',"
                + "'scopes':'openid'"
                + "}"));
  }

  @Test
  void testRejectsFileSchemeRedirectUri() {
    assertStatus(
        HttpStatus.CONFLICT,
        POST(
            "/oAuth2Clients",
            "{"
                + "'clientId':'client-file',"
                + "'clientSecret':'secret',"
                + "'clientAuthenticationMethods':'client_secret_basic',"
                + "'authorizationGrantTypes':'authorization_code',"
                + "'redirectUris':'file:///etc/passwd',"
                + "'scopes':'openid'"
                + "}"));
  }

  @Test
  void testDefaultNameTruncatesLongClientId() {
    // clientId is varchar(255); persisted name is varchar(230). When the UI
    // omits name, the controller defaults it to clientId — must truncate so
    // the persist doesn't blow up with value-too-long.
    String longClientId = "c".repeat(250);
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/oAuth2Clients",
                "{"
                    + "'clientId':'"
                    + longClientId
                    + "',"
                    + "'clientSecret':'secret',"
                    + "'clientAuthenticationMethods':'client_secret_basic',"
                    + "'authorizationGrantTypes':'authorization_code',"
                    + "'redirectUris':'https://example.com/callback',"
                    + "'scopes':'openid'"
                    + "}"));

    JsonObject client = GET("/oAuth2Clients/{id}", uid).content(HttpStatus.OK);
    assertEquals(230, client.getString("name").string().length());
  }

  @Test
  void testAcceptsCustomSchemeRedirectUri() {
    // RFC 8252 — native apps legitimately use custom schemes for OAuth2
    // redirect URIs (e.g. dhis2oauth://oauth for the DHIS2 Android app).
    // The validator must accept these, not just http/https.
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/oAuth2Clients",
                "{"
                    + "'clientId':'client-native',"
                    + "'clientSecret':'secret',"
                    + "'clientAuthenticationMethods':'client_secret_basic',"
                    + "'authorizationGrantTypes':'authorization_code,refresh_token',"
                    + "'redirectUris':'dhis2oauth://oauth',"
                    + "'scopes':'openid'"
                    + "}"));

    JsonObject client = GET("/oAuth2Clients/{id}", uid).content(HttpStatus.OK);
    assertEquals("dhis2oauth://oauth", client.getString("redirectUris").string());
  }

  @Test
  void testUpdateAllowsClientCredentialsGrantType() {
    // The DCR system registrar client is created with client_credentials;
    // editing it via the UI must not be rejected by the grant-type validator.
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/oAuth2Clients",
                "{"
                    + "'clientId':'client-cc',"
                    + "'clientSecret':'secret',"
                    + "'clientAuthenticationMethods':'client_secret_basic',"
                    + "'authorizationGrantTypes':'client_credentials',"
                    + "'redirectUris':'https://example.com/callback',"
                    + "'scopes':'openid'"
                    + "}"));

    JsonObject client = GET("/oAuth2Clients/{id}", uid).content(HttpStatus.OK);
    assertEquals("client_credentials", client.getString("authorizationGrantTypes").string());
  }

  @Test
  void testCreateWithoutNameDefaultsToClientId() {
    // Consumers (e.g. the OAuth2 e2e test and existing API clients) may POST
    // a client without a `name` field. Verify the controller defaults it to
    // clientId rather than rejecting the request on the NOT NULL constraint.
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/oAuth2Clients",
                "{"
                    + "'clientId':'client-no-name',"
                    + "'clientSecret':'secret',"
                    + "'clientAuthenticationMethods':'client_secret_basic',"
                    + "'authorizationGrantTypes':'authorization_code,refresh_token',"
                    + "'redirectUris':'https://example.com/callback',"
                    + "'scopes':'openid'"
                    + "}"));

    JsonObject client = GET("/oAuth2Clients/{id}", uid).content(HttpStatus.OK);
    assertEquals("client-no-name", client.getString("name").string());
    assertEquals("client-no-name", client.getString("clientId").string());
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
