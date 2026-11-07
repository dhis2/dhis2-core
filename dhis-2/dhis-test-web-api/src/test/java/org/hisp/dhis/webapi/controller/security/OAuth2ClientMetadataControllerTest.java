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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests the metadata import, export and schema of OAuth2 clients. */
@Transactional
class OAuth2ClientMetadataControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String CLIENT_UID = "OAuthClnt01";

  /**
   * The properties of a client except translations, which the schema names "translation" and the
   * JSON names "translations".
   */
  private static final Set<String> PROPERTIES_WITHOUT_TRANSLATIONS =
      Set.of(
          "access",
          "attributeValues",
          "authorizationGrantTypes",
          "clientAuthenticationMethods",
          "clientId",
          "clientIdIssuedAt",
          "clientSecret",
          "clientSecretExpiresAt",
          "clientSettings",
          "code",
          "created",
          "createdBy",
          "displayName",
          "href",
          "id",
          "lastUpdated",
          "lastUpdatedBy",
          "name",
          "postLogoutRedirectUris",
          "redirectUris",
          "scopes",
          "sharing",
          "tokenSettings",
          "user");

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void shouldCreateAndUpdateClientViaMetadataImport() {
    importMetadata(client("Client A", "metadata-client-a", "openid,profile"));

    JsonObject created = getClient();
    assertEquals("CLIENT_A", created.getString("code").string());
    assertEquals("Client A", created.getString("name").string());
    assertEquals("metadata-client-a", created.getString("clientId").string());
    assertEquals("{noop}secret", created.getString("clientSecret").string());
    assertTrue(created.getString("clientIdIssuedAt").string().startsWith("2025-01-02T03:04:05"));
    assertTrue(
        created.getString("clientSecretExpiresAt").string().startsWith("2030-01-02T03:04:05"));
    assertEquals("client_secret_basic", created.getString("clientAuthenticationMethods").string());
    assertEquals(
        "authorization_code,refresh_token", created.getString("authorizationGrantTypes").string());
    assertEquals("https://example.com/callback", created.getString("redirectUris").string());
    assertEquals(
        "https://example.com/logout", created.getString("postLogoutRedirectUris").string());
    assertEquals("openid,profile", created.getString("scopes").string());
    assertTrue(
        created
            .getString("clientSettings")
            .string()
            .contains("\"settings.client.require-proof-key\":true"));
    assertTrue(
        created.getString("tokenSettings").string().contains("settings.token.access-token-format"));

    importMetadata(client("Client B", "metadata-client-b", "openid,email"));

    JsonObject updated = getClient();
    assertEquals("Client B", updated.getString("name").string());
    assertEquals("metadata-client-b", updated.getString("clientId").string());
    assertEquals("openid,email", updated.getString("scopes").string());
  }

  @Test
  void shouldDefaultNameToClientIdOnMetadataImport() {
    importMetadata(
        """
        {'oAuth2Clients':[{
          'id':'%s','clientId':'metadata-unnamed-client',
          'clientAuthenticationMethods':'client_secret_basic',
          'authorizationGrantTypes':'authorization_code','scopes':'openid',
          'redirectUris':'https://example.com/callback'
        }]}
        """
            .formatted(CLIENT_UID));

    assertEquals("metadata-unnamed-client", getClient().getString("name").string());
  }

  @Test
  void shouldUpdateClientWhenExportedClientWithChangesIsPut() {
    importMetadata(client("Client A", "metadata-client-a", "openid,profile"));
    JsonObject exported = getClient();
    JsonObject changed = withMember(exported, "name", "\"Client changed\"");
    changed = withMember(changed, "scopes", "\"openid,username\"");
    changed = withMember(changed, "redirectUris", "\"https://example.org/changed\"");

    assertStatus(HttpStatus.OK, PUT("/oAuth2Clients/" + CLIENT_UID, changed.toJson()));

    JsonObject updated = getClient();
    assertEquals("Client changed", updated.getString("name").string());
    assertEquals("openid,username", updated.getString("scopes").string());
    assertEquals("https://example.org/changed", updated.getString("redirectUris").string());
    assertEquals("metadata-client-a", updated.getString("clientId").string());
    assertEquals(
        "https://example.com/logout", updated.getString("postLogoutRedirectUris").string());
    assertEquals(
        exported.getString("clientSettings").string(),
        updated.getString("clientSettings").string());
  }

  @Test
  void shouldDeleteClient() {
    importMetadata(client("Client A", "metadata-client-a", "openid"));

    assertStatus(HttpStatus.OK, DELETE("/oAuth2Clients/" + CLIENT_UID));

    assertEquals(HttpStatus.NOT_FOUND, getClientResponse().status());
  }

  @Test
  void shouldRejectTranslationsOfClient() {
    HttpResponse response =
        POST(
            "/metadata",
            """
            {'oAuth2Clients':[{
              'id':'%s','name':'Client A','clientId':'metadata-client-a',
              'authorizationGrantTypes':'authorization_code','scopes':'openid',
              'translations':[{'locale':'fr','property':'NAME','value':'Client fr'}]
            }]}
            """
                .formatted(CLIENT_UID));

    assertEquals(HttpStatus.CONFLICT, response.status());
    String report = response.contentUnchecked().toJson();
    assertTrue(report.contains("E1107"), report);
  }

  @Test
  void shouldExportClientProperties() {
    importMetadata(client("Client A", "metadata-client-a", "openid"));

    JsonObject client = getClient();

    assertEquals(withTranslationKey("translations"), Set.copyOf(client.names()));
    assertTrue(client.getString("href").string().endsWith("/oAuth2Clients/" + CLIENT_UID));
    assertEquals("Client A", client.getString("displayName").string());
  }

  @Test
  void shouldDescribeClientSchemaPropertyNames() {
    assertEquals(withTranslationKey("translation"), schemaPropertyNames(property -> true));
  }

  @Test
  void shouldDescribeClientSchemaPersistedAndRequiredProperties() {
    assertEquals(
        Set.of(
            "authorizationGrantTypes",
            "clientAuthenticationMethods",
            "clientId",
            "clientIdIssuedAt",
            "clientSecret",
            "clientSecretExpiresAt",
            "clientSettings",
            "code",
            "created",
            "createdBy",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "postLogoutRedirectUris",
            "redirectUris",
            "scopes",
            "tokenSettings"),
        schemaPropertyNames(property -> property.getBoolean("persisted").booleanValue()));
    assertEquals(
        Set.of("clientId", "name"),
        schemaPropertyNames(property -> property.getBoolean("required").booleanValue()));
  }

  @Test
  void shouldDescribeClientSchemaTypesAndLengths() {
    Map<String, JsonObject> properties =
        GET("/schemas/oAuth2Client")
            .content(HttpStatus.OK)
            .getArray("properties")
            .asList(JsonObject.class)
            .stream()
            .collect(Collectors.toMap(p -> p.getString("name").string(), p -> p));

    assertEquals(230, properties.get("name").getNumber("length").intValue());
    assertEquals(255, properties.get("clientId").getNumber("length").intValue());
    assertEquals(1000, properties.get("scopes").getNumber("length").intValue());
    assertEquals(
        Integer.MAX_VALUE, properties.get("clientSettings").getNumber("length").intValue());
    assertEquals(Integer.MAX_VALUE, properties.get("tokenSettings").getNumber("length").intValue());
    assertEquals("DATE", properties.get("clientIdIssuedAt").getString("propertyType").string());
    assertEquals("URL", properties.get("href").getString("propertyType").string());
    assertTrue(properties.get("clientId").getBoolean("unique").booleanValue());
  }

  @Test
  void shouldDescribeClientSchemaNameAndFlags() {
    JsonObject schema = GET("/schemas/oAuth2Client").content(HttpStatus.OK);

    assertEquals("oauth2Client", schema.getString("name").string());
    assertFalse(schema.getBoolean("shareable").booleanValue());
    assertFalse(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
  }

  private Set<String> schemaPropertyNames(Predicate<JsonObject> filter) {
    return GET("/schemas/oAuth2Client")
        .content(HttpStatus.OK)
        .getArray("properties")
        .asList(JsonObject.class)
        .stream()
        .filter(filter)
        .map(property -> property.getString("name").string())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static Set<String> withTranslationKey(String translationKey) {
    Set<String> names = new TreeSet<>(PROPERTIES_WITHOUT_TRANSLATIONS);
    names.add(translationKey);
    return names;
  }

  private static String client(String name, String clientId, String scopes) {
    return """
        {'oAuth2Clients':[{
          'id':'%s','code':'CLIENT_A','name':'%s','clientId':'%s',
          'clientSecret':'{noop}secret',
          'clientIdIssuedAt':'2025-01-02T03:04:05.000',
          'clientSecretExpiresAt':'2030-01-02T03:04:05.000',
          'clientAuthenticationMethods':'client_secret_basic',
          'authorizationGrantTypes':'authorization_code,refresh_token',
          'redirectUris':'https://example.com/callback',
          'postLogoutRedirectUris':'https://example.com/logout',
          'scopes':'%s'
        }]}
        """
        .formatted(CLIENT_UID, name, clientId, scopes);
  }

  /** Returns a copy of the object with the value of the given member replaced. */
  private static JsonObject withMember(JsonObject object, String member, String json) {
    return JsonMixed.of(object.get(member).node().replaceWith(json)).asObject();
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
    assertEquals("OK", response.content().getString("status").string());
  }

  /** Reads the client from the database, bypassing the session and the L2 cache. */
  private JsonObject getClient() {
    return getClientResponse().content(HttpStatus.OK);
  }

  /** Requests the client in a new session with an empty L2 cache. */
  private HttpResponse getClientResponse() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/oAuth2Clients/" + CLIENT_UID);
  }
}
