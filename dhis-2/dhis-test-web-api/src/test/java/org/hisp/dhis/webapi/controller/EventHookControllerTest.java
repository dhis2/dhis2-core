/*
 * Copyright (c) 2004-2022, University of Oslo
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

import java.nio.file.Path;
import org.hisp.dhis.common.auth.ApiTokenAuthScheme;
import org.hisp.dhis.common.auth.HttpBasicAuthScheme;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link EventHookController} using (mocked) REST requests.
 *
 * @author Morten Olav Hansen
 */
@Transactional
class EventHookControllerTest extends PostgresControllerIntegrationTestBase {
  @Test
  void testGetEventHooks() {
    JsonObject eventHooks = GET("/eventHooks").content(HttpStatus.OK);
    assertTrue(eventHooks.isObject());
    assertTrue(eventHooks.has("eventHooks"));
    assertTrue(eventHooks.get("eventHooks").isArray());
    assertTrue(eventHooks.getArray("eventHooks").isEmpty());
  }

  @Test
  void testCreateEventHookWebhookApiToken() {
    String id =
        assertStatus(
            HttpStatus.CREATED, POST("/eventHooks", Path.of("event-hook/webhook-api-token.json")));
    assertEquals("bRNvL6NMQXb", id);

    JsonObject eventHook = GET("/eventHooks/{id}", id).content(HttpStatus.OK);
    assertTrue(eventHook.has("id", "name", "source", "targets"));
    assertEquals("bRNvL6NMQXb", eventHook.getString("id").string());
    assertEquals("WebhookApiToken", eventHook.getString("name").string());
    assertEquals("metadata", eventHook.get("source").asObject().getString("path").string());
    assertEquals("id,name", eventHook.get("source").asObject().getString("fields").string());

    JsonList<JsonObject> targets = eventHook.get("targets").asList(JsonObject.class);
    assertFalse(targets.isEmpty());

    JsonObject target = targets.get(0);
    assertTrue(target.has("type", "url", "auth"));
    assertEquals(WebhookTarget.TYPE, target.getString("type").string());

    JsonObject auth = target.getObject("auth");
    assertFalse(auth.has("token"));
    assertEquals(ApiTokenAuthScheme.API_TOKEN_TYPE, auth.getString("type").string());
  }

  @Test
  void testCreateEventHookWebhookHttpBasic() {
    String id =
        assertStatus(
            HttpStatus.CREATED, POST("/eventHooks", Path.of("event-hook/webhook-http-basic.json")));
    assertEquals("bRNvL6NMQXb", id);

    JsonObject eventHook = GET("/eventHooks/{id}", id).content(HttpStatus.OK);
    assertTrue(eventHook.has("id", "name", "source", "targets"));
    assertEquals("bRNvL6NMQXb", eventHook.getString("id").string());
    assertEquals("WebhookHttpBasic", eventHook.getString("name").string());
    assertEquals("metadata", eventHook.get("source").asObject().getString("path").string());
    assertEquals("id,name", eventHook.get("source").asObject().getString("fields").string());

    JsonList<JsonObject> targets = eventHook.get("targets").asList(JsonObject.class);
    assertFalse(targets.isEmpty());

    JsonObject target = targets.get(0).asObject();
    assertTrue(target.has("type", "url", "auth"));
    assertEquals(WebhookTarget.TYPE, target.getString("type").string());

    JsonObject auth = target.getObject("auth");
    assertTrue(auth.has("type", "username"));
    assertFalse(auth.has("password"));
    assertEquals(HttpBasicAuthScheme.HTTP_BASIC_TYPE, auth.getString("type").string());
    assertEquals("admin", auth.getString("username").string());
  }

  @Test
  void testCreateEventHookWebhookHttpBasicDefaultEnabled() {
    String id =
        assertStatus(
            HttpStatus.CREATED, POST("/eventHooks", Path.of("event-hook/webhook-http-basic.json")));
    assertEquals("bRNvL6NMQXb", id);

    JsonObject eventHook = GET("/eventHooks/{id}", id).content(HttpStatus.OK);
    assertTrue(eventHook.has("id", "name", "disabled", "source", "targets"));
    assertFalse(eventHook.getBoolean("disabled").bool());
    assertEquals("bRNvL6NMQXb", eventHook.getString("id").string());
    assertEquals("WebhookHttpBasic", eventHook.getString("name").string());
    assertEquals("metadata", eventHook.get("source").asObject().getString("path").string());
    assertEquals("id,name", eventHook.get("source").asObject().getString("fields").string());

    JsonList<JsonObject> targets = eventHook.get("targets").asList(JsonObject.class);
    assertFalse(targets.isEmpty());

    JsonObject target = targets.get(0).asObject();
    assertTrue(target.has("type", "url", "auth"));
    assertEquals(WebhookTarget.TYPE, target.getString("type").string());

    JsonObject auth = target.getObject("auth");
    assertTrue(auth.has("type", "username"));
    assertFalse(auth.has("password"));
    assertEquals(HttpBasicAuthScheme.HTTP_BASIC_TYPE, auth.getString("type").string());
    assertEquals("admin", auth.getString("username").string());
  }

  @Test
  void testDeleteEventHookWebhookHttpBasic() {
    String id =
        assertStatus(
            HttpStatus.CREATED, POST("/eventHooks", Path.of("event-hook/webhook-http-basic.json")));
    assertEquals("bRNvL6NMQXb", id);

    GET("/eventHooks/{id}", id).content(HttpStatus.OK);
    DELETE("/eventHooks/{id}", (Object) id).content(HttpStatus.OK);
    GET("/eventHooks/{id}", id).content(HttpStatus.NOT_FOUND);
  }
}
