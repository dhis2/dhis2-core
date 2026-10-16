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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.hisp.dhis.common.auth.ApiTokenAuthScheme;
import org.hisp.dhis.common.auth.HttpBasicAuthScheme;
import org.hisp.dhis.eventhook.EventHook;
import org.hisp.dhis.eventhook.EventHookSecretManager;
import org.hisp.dhis.eventhook.Target;
import org.hisp.dhis.eventhook.targets.JmsTarget;
import org.hisp.dhis.eventhook.targets.KafkaTarget;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests importing, updating and exporting event hooks through the metadata API. */
@Transactional
class EventHookMetadataControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String HOOK_UID = "EvHookMeta1";

  private static final String SECRETS_UID = "EvHookSecr1";

  private static final String BASIC_PASSWORD = "Basic-secret-123";

  private static final String API_TOKEN = "d2pat_hookToken0123456789abcdef01234";

  private static final String JMS_PASSWORD = "Jms-secret-456";

  private static final String KAFKA_PASSWORD = "Kafka-secret-789";

  /**
   * The properties an event hook exposes, except translations, which the schema names {@code
   * translation} and the JSON output {@code translations}.
   */
  private static final Set<String> PROPERTIES =
      Set.of(
          "access",
          "attributeValues",
          "code",
          "created",
          "createdBy",
          "description",
          "disabled",
          "displayName",
          "href",
          "id",
          "lastUpdated",
          "lastUpdatedBy",
          "name",
          "sharing",
          "source",
          "targets",
          "user");

  @Autowired private EventHookSecretManager secretManager;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void testCreateAndUpdateViaMetadataImport() {
    importMetadata(webhookHook("HOOK_A", "Hook A", "First", false, "id,name"));

    JsonObject created = getEventHook(HOOK_UID);
    assertEquals("HOOK_A", created.getString("code").string());
    assertEquals("Hook A", created.getString("name").string());
    assertEquals("First", created.getString("description").string());
    assertFalse(created.getBoolean("disabled").booleanValue());
    assertEquals("metadata", created.getObject("source").getString("path").string());
    assertEquals("id,name", created.getObject("source").getString("fields").string());
    JsonObject target = created.getArray("targets").getObject(0);
    assertEquals("webhook", target.getString("type").string());
    assertEquals("https://hooks.example.org/a", target.getString("url").string());

    importMetadata(webhookHook("HOOK_B", "Hook B", "Second", true, "id,code"));

    JsonObject updated = getEventHook(HOOK_UID);
    assertEquals("HOOK_B", updated.getString("code").string());
    assertEquals("Hook B", updated.getString("name").string());
    assertEquals("Second", updated.getString("description").string());
    assertTrue(updated.getBoolean("disabled").booleanValue());
    assertEquals("id,code", updated.getObject("source").getString("fields").string());
  }

  @Test
  void testPutOfExportedObjectWithChanges() {
    importMetadata(webhookHook("HOOK_A", "Hook A", "First", false, "id,name"));
    String exported = getEventHook(HOOK_UID).toJson();
    assertTrue(exported.contains("\"disabled\":false"), exported);
    assertTrue(exported.contains("\"fields\":\"id,name\""), exported);
    assertTrue(exported.contains("\"translations\":[]"), exported);
    String changed =
        exported
            .replace("\"disabled\":false", "\"disabled\":true")
            .replace("\"fields\":\"id,name\"", "\"fields\":\"id,displayName\"")
            .replace(
                "\"translations\":[]",
                "\"translations\":[{\"locale\":\"fr\",\"property\":\"NAME\",\"value\":\"Crochet\"}]");

    assertStatus(HttpStatus.OK, PUT("/eventHooks/" + HOOK_UID, changed));

    JsonObject updated = getEventHook(HOOK_UID);
    assertEquals("Hook A", updated.getString("name").string());
    assertTrue(updated.getBoolean("disabled").booleanValue());
    assertEquals("id,displayName", updated.getObject("source").getString("fields").string());
    assertEquals(
        "https://hooks.example.org/a",
        updated.getArray("targets").getObject(0).getString("url").string());
    JsonArray translations = updated.getArray("translations");
    assertEquals(1, translations.size());
    assertEquals("Crochet", translations.getObject(0).getString("value").string());
  }

  @Test
  void testTargetSecretsAreNotExported() {
    importSecretsHook();

    JsonObject eventHook = getEventHook(SECRETS_UID);
    JsonArray targets = eventHook.getArray("targets");
    assertEquals(
        Set.of("type", "username"), Set.copyOf(targets.getObject(0).getObject("auth").names()));
    assertEquals(Set.of("type"), Set.copyOf(targets.getObject(1).getObject("auth").names()));
    assertFalse(targets.getObject(2).has("password"));
    assertFalse(targets.getObject(3).has("password"));
    assertEquals("jms-user", targets.getObject(2).getString("username").string());
    assertEquals("kafka-user", targets.getObject(3).getString("username").string());

    String metadataExport = GET("/metadata?eventHooks=true").content(HttpStatus.OK).toJson();
    assertTrue(metadataExport.contains(SECRETS_UID), metadataExport);
    for (String json : new String[] {eventHook.toJson(), metadataExport}) {
      for (String secret : new String[] {BASIC_PASSWORD, API_TOKEN, JMS_PASSWORD, KAFKA_PASSWORD}) {
        assertFalse(json.contains(secret), json);
      }
      assertFalse(json.contains("password"), json);
      assertFalse(json.contains("\"token\""), json);
    }
  }

  @Test
  void testTargetSecretsAreStoredEncrypted() {
    importSecretsHook();
    startNewRequestSession();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select targets->0->'auth'->>'password' as basic,"
                + " targets->1->'auth'->>'token' as token,"
                + " targets->2->>'password' as jms, targets->3->>'password' as kafka,"
                + " targets->0->'auth'->>'username' as username"
                + " from eventhook where uid = ?",
            SECRETS_UID);
    assertEquals("admin", row.get("username"));
    assertNotPlaintext(BASIC_PASSWORD, row.get("basic"));
    assertNotPlaintext(API_TOKEN, row.get("token"));
    assertNotPlaintext(JMS_PASSWORD, row.get("jms"));
    assertNotPlaintext(KAFKA_PASSWORD, row.get("kafka"));

    EventHook eventHook = manager.get(EventHook.class, SECRETS_UID);
    entityManager.detach(eventHook);
    secretManager.decrypt(eventHook);
    List<Target> targets = eventHook.getTargets();
    assertEquals(
        BASIC_PASSWORD,
        ((HttpBasicAuthScheme) ((WebhookTarget) targets.get(0)).getAuth()).getPassword());
    assertEquals(
        API_TOKEN, ((ApiTokenAuthScheme) ((WebhookTarget) targets.get(1)).getAuth()).getToken());
    assertEquals(JMS_PASSWORD, ((JmsTarget) targets.get(2)).getPassword());
    assertEquals(KAFKA_PASSWORD, ((KafkaTarget) targets.get(3)).getPassword());
  }

  /** Pins the exported keys of an event hook. */
  @Test
  void testExportedProperties() {
    importMetadata(webhookHook("HOOK_A", "Hook A", "First", false, "id,name"));

    JsonObject eventHook = getEventHook(HOOK_UID);
    assertEquals(withProperty("translations"), Set.copyOf(eventHook.names()));
    assertTrue(
        eventHook.getString("href").string().endsWith("/eventHooks/" + HOOK_UID),
        eventHook.getString("href").string());
    assertTrue(eventHook.getObject("access").getBoolean("read").booleanValue());
    assertTrue(eventHook.getObject("sharing").has("public"));
  }

  /** Pins the schema's property, persisted and required sets. */
  @Test
  void testSchemaProperties() {
    JsonObject schema = GET("/schemas/eventHook").content(HttpStatus.OK);
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

    assertEquals(withProperty("translation"), names);
    assertEquals(
        Set.of(
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "description",
            "disabled",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "source",
            "targets",
            "translation"),
        persisted);
    assertEquals(Set.of("name", "source", "targets"), required);
    assertEquals("identifiableObject", schema.getString("name").string());
    assertTrue(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
  }

  private static String webhookHook(
      String code, String name, String description, boolean disabled, String fields) {
    return ("{'eventHooks':[{'id':'%s','code':'%s','name':'%s','description':'%s',"
            + "'disabled':%s,'source':{'path':'metadata','fields':'%s'},"
            + "'targets':[{'type':'webhook','url':'https://hooks.example.org/a'}]}]}")
        .formatted(HOOK_UID, code, name, description, disabled, fields);
  }

  /**
   * Imports a hook with a secret on each target type. The hook is disabled because {@code
   * EventHookListener} reloads synchronously on import and skips disabled hooks, so no JMS or Kafka
   * client is created. The secrets are encrypted on import whether or not the hook is disabled.
   */
  private void importSecretsHook() {
    importMetadata(
        ("{'eventHooks':[{'id':'%s','name':'Secrets hook','disabled':true,"
                + "'source':{'path':'metadata','fields':'id'},'targets':["
                + "{'type':'webhook','url':'https://hooks.example.org/basic',"
                + "'auth':{'type':'http-basic','username':'admin','password':'%s'}},"
                + "{'type':'webhook','url':'https://hooks.example.org/token',"
                + "'auth':{'type':'api-token','token':'%s'}},"
                + "{'type':'jms','username':'jms-user','password':'%s'},"
                + "{'type':'kafka','username':'kafka-user','password':'%s'}]}]}")
            .formatted(SECRETS_UID, BASIC_PASSWORD, API_TOKEN, JMS_PASSWORD, KAFKA_PASSWORD));
  }

  private static Set<String> withProperty(String translationsName) {
    Set<String> names = new TreeSet<>(PROPERTIES);
    names.add(translationsName);
    return names;
  }

  private static void assertNotPlaintext(String plaintext, Object stored) {
    String value = (String) stored;
    assertFalse(value == null || value.isBlank(), "secret is not stored");
    assertFalse(value.contains(plaintext), value);
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }

  /** Reads the event hook from the database, bypassing the session and the second-level cache. */
  private JsonObject getEventHook(String uid) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/eventHooks/" + uid).content(HttpStatus.OK);
  }
}
