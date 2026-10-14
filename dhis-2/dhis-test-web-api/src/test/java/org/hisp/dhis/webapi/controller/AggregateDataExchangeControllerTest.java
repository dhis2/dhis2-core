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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests creating, updating and exporting aggregate data exchanges through the API. */
@Transactional
class AggregateDataExchangeControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String INTERNAL_UID = "AdeIntern01";

  private static final String BASIC_AUTH_UID = "AdeBasic001";

  private static final String TOKEN_AUTH_UID = "AdeToken001";

  private static final String PASSWORD = "Secret-pwd-123";

  private static final String ACCESS_TOKEN = "d2pat_secretToken0123456789abcdef0123";

  private static final String SOURCE =
      "'source':{'requests':[{'name':'%s','dx':['fbfJHSPpUQD'],'pe':['202201'],"
          + "'ou':['ImspTQPwCqd']}]}";

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void testCreateAndUpdateViaMetadataImport() {
    importMetadata(internalExchange("ADE_A", "Exchange A", "Request A"));

    JsonObject created = getExchange(INTERNAL_UID);
    assertEquals("ADE_A", created.getString("code").string());
    assertEquals("Exchange A", created.getString("name").string());
    assertEquals(
        "Request A",
        created.getObject("source").getArray("requests").getObject(0).getString("name").string());
    assertEquals("INTERNAL", created.getObject("target").getString("type").string());

    importMetadata(internalExchange("ADE_B", "Exchange B", "Request B"));

    JsonObject updated = getExchange(INTERNAL_UID);
    assertEquals("ADE_B", updated.getString("code").string());
    assertEquals("Exchange B", updated.getString("name").string());
    assertEquals(
        "Request B",
        updated.getObject("source").getArray("requests").getObject(0).getString("name").string());
  }

  @Test
  void testCreateViaAggregateDataExchangesEndpoint() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/aggregateDataExchanges",
                ("{'name':'Exchange C'," + SOURCE + ",'target':{'type':'INTERNAL'}}")
                    .formatted("Request C")));

    JsonObject exchange = getExchange(uid);
    assertEquals("Exchange C", exchange.getString("name").string());
    assertEquals("INTERNAL", exchange.getObject("target").getString("type").string());
  }

  @Test
  void testPutOfExportedObjectWithChanges() {
    importMetadata(internalExchange("ADE_A", "Exchange A", "Request A"));
    String exported = getExchange(INTERNAL_UID).toJson();
    assertTrue(exported.contains("\"name\":\"Request A\""), exported);
    assertTrue(exported.contains("\"translations\":[]"), exported);
    String changed =
        exported
            .replace("\"name\":\"Request A\"", "\"name\":\"Request changed\"")
            .replace(
                "\"translations\":[]",
                "\"translations\":[{\"locale\":\"fr\",\"property\":\"NAME\",\"value\":\"Echange\"}]");

    assertStatus(HttpStatus.OK, PUT("/aggregateDataExchanges/" + INTERNAL_UID, changed));

    JsonObject updated = getExchange(INTERNAL_UID);
    assertEquals("Exchange A", updated.getString("name").string());
    assertEquals(
        "Request changed",
        updated.getObject("source").getArray("requests").getObject(0).getString("name").string());
    JsonArray translations = updated.getArray("translations");
    assertEquals(1, translations.size());
    assertEquals("Echange", translations.getObject(0).getString("value").string());
  }

  @Test
  void testTargetApiSecretsAreNotExported() {
    importExternalExchanges();

    JsonObject basicAuth = getExchange(BASIC_AUTH_UID);
    JsonObject tokenAuth = getExchange(TOKEN_AUTH_UID);
    assertEquals(
        Set.of("url", "username"),
        Set.copyOf(basicAuth.getObject("target").getObject("api").names()));
    assertEquals(Set.of("url"), Set.copyOf(tokenAuth.getObject("target").getObject("api").names()));

    String metadataExport =
        GET("/metadata?aggregateDataExchanges=true").content(HttpStatus.OK).toJson();
    for (String json : new String[] {basicAuth.toJson(), tokenAuth.toJson(), metadataExport}) {
      assertFalse(json.contains(PASSWORD), json);
      assertFalse(json.contains(ACCESS_TOKEN), json);
      assertFalse(json.contains("password"), json);
      assertFalse(json.contains("accessToken"), json);
    }
  }

  @Test
  void testTargetApiSecretsAreStoredEncrypted() {
    importExternalExchanges();
    startNewRequestSession();

    Map<String, Object> basicAuth = selectTargetApi(BASIC_AUTH_UID);
    assertEquals("https://a.example.org", basicAuth.get("url"));
    assertEquals("admin", basicAuth.get("username"));
    String storedPassword = (String) basicAuth.get("password");
    assertFalse(storedPassword.isBlank());
    assertFalse(storedPassword.contains(PASSWORD), storedPassword);

    Map<String, Object> tokenAuth = selectTargetApi(TOKEN_AUTH_UID);
    String storedToken = (String) tokenAuth.get("accesstoken");
    assertFalse(storedToken.isBlank());
    assertFalse(storedToken.contains(ACCESS_TOKEN), storedToken);
  }

  /** The keys of an exported exchange, as on a pre-migration instance. */
  @Test
  void testExportedProperties() {
    importMetadata(internalExchange("ADE_A", "Exchange A", "Request A"));

    JsonObject exchange = getExchange(INTERNAL_UID);
    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "displayName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "source",
            "target",
            "translations",
            "user"),
        Set.copyOf(exchange.names()));
    assertTrue(
        exchange.getString("href").string().endsWith("/aggregateDataExchanges/" + INTERNAL_UID),
        exchange.getString("href").string());
    assertTrue(exchange.getObject("access").getBoolean("read").booleanValue());
    assertTrue(exchange.getObject("sharing").has("public"));
  }

  /** Same property set and persisted properties as before the migration. */
  @Test
  void testSchemaProperties() {
    JsonObject schema = GET("/schemas/aggregateDataExchange").content(HttpStatus.OK);
    JsonArray properties = schema.getArray("properties");
    Set<String> names = new TreeSet<>();
    Set<String> persisted = new TreeSet<>();
    Set<String> required = new TreeSet<>();
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      String name = property.getString("name").string();
      names.add(name);
      if (property.getBoolean("persisted").booleanValue()) {
        persisted.add(name);
      }
      if (property.getBoolean("required").booleanValue()) {
        required.add(name);
      }
    }

    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "displayName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "source",
            "target",
            "translation",
            "user"),
        names);
    assertEquals(
        Set.of(
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "source",
            "target",
            "translation"),
        persisted);
    assertEquals(Set.of("name", "source", "target"), required);
    assertTrue(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("dataShareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
  }

  private String internalExchange(String code, String name, String requestName) {
    return ("{'aggregateDataExchanges':[{'id':'%s','code':'%s','name':'%s',"
            + SOURCE
            + ",'target':{'type':'INTERNAL'}}]}")
        .formatted(INTERNAL_UID, code, name, requestName);
  }

  private void importExternalExchanges() {
    importMetadata(
        ("{'aggregateDataExchanges':["
                + "{'id':'%s','name':'Basic auth exchange',"
                + SOURCE
                + ",'target':{'type':'EXTERNAL','api':{'url':'https://a.example.org',"
                + "'username':'admin','password':'%s'}}},"
                + "{'id':'%s','name':'Token auth exchange',"
                + SOURCE
                + ",'target':{'type':'EXTERNAL','api':{'url':'https://b.example.org',"
                + "'accessToken':'%s'}}}]}")
            .formatted(
                BASIC_AUTH_UID, "Request", PASSWORD, TOKEN_AUTH_UID, "Request", ACCESS_TOKEN));
  }

  private Map<String, Object> selectTargetApi(String uid) {
    return jdbcTemplate.queryForMap(
        "select target->'api'->>'url' as url, target->'api'->>'username' as username,"
            + " target->'api'->>'password' as password,"
            + " target->'api'->>'accessToken' as accesstoken"
            + " from aggregatedataexchange where uid = ?",
        uid);
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }

  /** Reads the exchange from the database, bypassing the session and the second-level cache. */
  private JsonObject getExchange(String uid) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/aggregateDataExchanges/" + uid).content(HttpStatus.OK);
  }
}
