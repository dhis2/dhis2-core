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
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for {@link org.hisp.dhis.webapi.controller.security.oauth.OAuth2ClientController}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class OAuth2ClientControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private Dhis2OAuth2ClientService clientService;

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
  void testCannotCreateWithReservedSystemRegistrarClientId() {
    // If someone could squat the reserved clientId while the authorization
    // server is off, OAuth2DcrService.init() would later find their record,
    // skip creating its own, and DCR would mint initial access tokens through
    // a client whose secret the squatter controls.
    assertStatus(
        HttpStatus.CONFLICT,
        POST(
            "/oAuth2Clients",
            "{"
                + "'clientId':'system-dcr-registrar-client',"
                + "'clientSecret':'secret',"
                + "'clientAuthenticationMethods':'client_secret_basic',"
                + "'authorizationGrantTypes':'authorization_code',"
                + "'redirectUris':'https://example.com/callback',"
                + "'scopes':'openid'"
                + "}"));
  }

  @Test
  void testCannotRenameExistingClientToReservedClientId() {
    String uid = createClient("legit-client", "Legit Client");
    assertStatus(
        HttpStatus.CONFLICT,
        PUT(
            "/oAuth2Clients/" + uid,
            "{"
                + "'clientId':'system-dcr-registrar-client',"
                + "'clientSecret':'secret',"
                + "'clientAuthenticationMethods':'client_secret_basic',"
                + "'authorizationGrantTypes':'authorization_code',"
                + "'redirectUris':'https://example.com/callback',"
                + "'scopes':'openid'"
                + "}"));
  }

  @Test
  void testSystemRegistrarHiddenFromList() {
    // The DCR system registrar is server-managed. Admins must not see it in
    // the settings list. Persist via manager directly to simulate what
    // OAuth2DcrService.init() does at startup, then assert the list filters
    // it out.
    persistSystemRegistrarFixture();
    createClient("visible-client", "Visible");

    List<String> clientIds =
        GET("/oAuth2Clients?fields=clientId&paging=false")
            .content(HttpStatus.OK)
            .getList("oAuth2Clients", JsonObject.class)
            .stream()
            .map(c -> c.getString("clientId").string())
            .toList();

    assertTrue(
        clientIds.contains("visible-client"), "Expected visible-client in list, got: " + clientIds);
    assertTrue(
        !clientIds.contains("system-dcr-registrar-client"),
        "system-dcr-registrar-client must not appear in list, got: " + clientIds);
  }

  @Test
  void testSystemRegistrarCannotBeUpdated() {
    String uid = persistSystemRegistrarFixture();
    assertStatus(
        HttpStatus.CONFLICT,
        PUT(
            "/oAuth2Clients/" + uid,
            "{"
                + "'clientId':'system-dcr-registrar-client',"
                + "'clientSecret':'new-secret',"
                + "'clientAuthenticationMethods':'client_secret_basic',"
                + "'authorizationGrantTypes':'authorization_code',"
                + "'redirectUris':'https://example.com/callback',"
                + "'scopes':'openid'"
                + "}"));
  }

  @Test
  void testSystemRegistrarCannotBeDeleted() {
    String uid = persistSystemRegistrarFixture();
    assertStatus(HttpStatus.CONFLICT, DELETE("/oAuth2Clients/" + uid));
  }

  /**
   * Persist a client with the reserved system-registrar clientId via the store layer, bypassing the
   * controller's rejection of the reserved clientId. Simulates what {@code OAuth2DcrService.init()}
   * does at startup on a fresh database. If a row already exists (the real DCR init ran in the test
   * context, or a prior test left state), reuse it rather than violating the unique constraint on
   * {@code client_id}.
   */
  private String persistSystemRegistrarFixture() {
    Dhis2OAuth2Client existing =
        clientService.getAsDhis2OAuth2ClientByClientId("system-dcr-registrar-client");
    if (existing != null) {
      return existing.getUid();
    }
    Dhis2OAuth2Client client = new Dhis2OAuth2Client();
    client.setAutoFields();
    client.setUid(CodeGenerator.generateUid());
    client.setName("System Registrar");
    client.setClientId("system-dcr-registrar-client");
    client.setClientSecret("secret");
    client.setClientAuthenticationMethods("client_secret_basic");
    client.setAuthorizationGrantTypes("client_credentials");
    client.setRedirectUris("https://example.com/callback");
    client.setScopes("openid");
    manager.save(client);
    return client.getUid();
  }

  @Test
  void testForbiddenWithoutManageAuthority() {
    // OAuth2 clients carry secrets and, via client_credentials, mint long-lived
    // tokens that act as their creator. Restrict management to the dedicated
    // F_OAUTH2_CLIENT_MANAGE authority — a regular authenticated user must get 403.
    switchToNewUser("regular-joe");
    assertStatus(
        HttpStatus.FORBIDDEN,
        POST(
            "/oAuth2Clients",
            "{"
                + "'clientId':'evil-client',"
                + "'clientSecret':'secret',"
                + "'clientAuthenticationMethods':'client_secret_basic',"
                + "'authorizationGrantTypes':'authorization_code',"
                + "'redirectUris':'https://evil.example.com/callback',"
                + "'scopes':'openid'"
                + "}"));
  }

  @Test
  void testAllowedWithManageAuthority() {
    // A non-admin user with F_OAUTH2_CLIENT_MANAGE can create clients.
    switchToNewUser("oauth-admin", "F_OAUTH2_CLIENT_MANAGE");
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/oAuth2Clients",
            "{"
                + "'clientId':'authorized-client',"
                + "'clientSecret':'secret',"
                + "'clientAuthenticationMethods':'client_secret_basic',"
                + "'authorizationGrantTypes':'authorization_code',"
                + "'redirectUris':'https://example.com/callback',"
                + "'scopes':'openid'"
                + "}"));
  }

  @Test
  void testRejectsJavascriptSchemeRedirectUri() {
    // Spring Authorization Server emits Location: <storedRedirectUri>?code=...
    // after exact-string match. A stored javascript: URI would execute in the
    // victim's browser. The URI is not in deviceEnrollmentRedirectAllowlist,
    // so the allow-list validator rejects it.
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
  void testRejectsCustomSchemeNotInAllowList() {
    // intent:// is a legitimate Android scheme but is NOT in
    // deviceEnrollmentRedirectAllowlist by default. Allow-list defaults to
    // deny for every custom scheme — admins must opt-in to each one.
    assertStatus(
        HttpStatus.CONFLICT,
        POST(
            "/oAuth2Clients",
            "{"
                + "'clientId':'client-intent',"
                + "'clientSecret':'secret',"
                + "'clientAuthenticationMethods':'client_secret_basic',"
                + "'authorizationGrantTypes':'authorization_code',"
                + "'redirectUris':'intent://something#Intent;end',"
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
  void testRejectsClientCredentialsGrantTypeOnAdminCreate() {
    // client_credentials is reserved for the server-managed DCR system registrar
    // (which is persisted via the service, bypassing this validator). Admins
    // must not be able to mint client_credentials clients via the UI or the
    // bulk metadata path — it's a long-lived, non-rotating, non-MFA token
    // footgun.
    assertStatus(
        HttpStatus.CONFLICT,
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
