/*
 * Copyright (c) 2004-2024, University of Oslo
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.config.H2DhisConfigurationProvider;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Analytics table hooks are disabled by default ({@code analytics.table_hooks.enabled=off}). This
 * suite enables the feature so create/update/duplicate behaviour can be exercised, and includes an
 * explicit default-off denial case in {@link AnalyticsTableHookDisabledControllerTest}.
 *
 * @author Lars Helge Overland
 */
@Transactional
@ContextConfiguration(classes = {AnalyticsTableHookControllerTest.DhisConfig.class})
class AnalyticsTableHookControllerTest extends H2ControllerIntegrationTestBase {

  private static final String HOOK_UID = "TableHook01";

  @Autowired private EntityManagerFactory entityManagerFactory;

  static class DhisConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      H2DhisConfigurationProvider provider = new H2DhisConfigurationProvider();
      Properties properties = new Properties();
      properties.put(ConfigurationKey.ANALYTICS_TABLE_HOOKS_ENABLED.getKey(), "on");
      provider.addProperties(properties);
      return provider;
    }
  }

  private static final String HOOK_SQL =
      "update analytics_rs_orgunitstructure set organisationunitid=1";

  private static String hookJson(String uid, String sql) {
    return "{'id':'%s','name':'Hook %s','phase':'RESOURCE_TABLE_POPULATED','resourceTableType':'ORG_UNIT_STRUCTURE','sql':'%s'}"
        .formatted(uid, uid, sql);
  }

  private static void assertRejected(ErrorCode expected, HttpResponse response) {
    assertNotNull(
        response.content(HttpStatus.CONFLICT).as(JsonWebMessage.class).findErrorReport(expected));
  }

  private AnalyticsTableHook getHook(String uid) {
    manager.clear();
    return manager.get(AnalyticsTableHook.class, uid);
  }

  @Test
  void testImportAnalyticsTableHook() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/analyticsTableHooks/",
            "{'name':'NameA', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=3'}"));
  }

  @Test
  void canUpdateHookNameWithoutDuplicateError() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/analyticsTableHooks/",
                "{'name':'OriginalName', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=1'}"));
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/analyticsTableHooks/" + uid,
            "{'name':'NewName', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=1'}"));
  }

  @Test
  void cannotImportDuplicateTableHook() {

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/analyticsTableHooks/",
            "{'name':'NameA', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=1'}"));
    // Exact duplicate
    assertStatus(
        HttpStatus.CONFLICT,
        POST(
            "/analyticsTableHooks/",
            "{'name':'NameA', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=1'}"));
    // Different name but otherwise equal

    JsonErrorReport error =
        POST(
                "/analyticsTableHooks/",
                "{'name':'NameB', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=1'}")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class)
            .findErrorReport(ErrorCode.E6400);
    assertNotNull(error);
    assertEquals("Analytics table hook `NameB` is a duplicate of `NameA`", error.getMessage());
  }

  @Test
  void testCreateAndUpdateViaMetadataImport() {
    importMetadata(
        ("{'analyticsTableHooks':[{'id':'%s','code':'HOOK_A','name':'Hook A',"
                + "'phase':'RESOURCE_TABLE_POPULATED','resourceTableType':'ORG_UNIT_STRUCTURE',"
                + "'sql':'update analytics_rs_orgunitstructure set level=1'}]}")
            .formatted(HOOK_UID));

    JsonObject created = getHook();
    assertEquals("HOOK_A", created.getString("code").string());
    assertEquals("Hook A", created.getString("name").string());
    assertEquals("RESOURCE_TABLE_POPULATED", created.getString("phase").string());
    assertEquals("ORG_UNIT_STRUCTURE", created.getString("resourceTableType").string());
    assertFalse(created.has("analyticsTableType"));
    assertEquals(
        "update analytics_rs_orgunitstructure set level=1", created.getString("sql").string());

    importMetadata(
        ("{'analyticsTableHooks':[{'id':'%s','code':'HOOK_B','name':'Hook B',"
                + "'phase':'ANALYTICS_TABLE_POPULATED','analyticsTableType':'EVENT',"
                + "'sql':'update analytics_event set value=1'}]}")
            .formatted(HOOK_UID));

    JsonObject updated = getHook();
    assertEquals("HOOK_B", updated.getString("code").string());
    assertEquals("Hook B", updated.getString("name").string());
    assertEquals("ANALYTICS_TABLE_POPULATED", updated.getString("phase").string());
    assertFalse(updated.has("resourceTableType"));
    assertEquals("EVENT", updated.getString("analyticsTableType").string());
    assertEquals("update analytics_event set value=1", updated.getString("sql").string());
  }

  @Test
  void testPutOfExportedHookMovedToAnalyticsTablePhase() throws JsonProcessingException {
    importMetadata(
        ("{'analyticsTableHooks':[{'id':'%s','name':'Hook A','phase':'RESOURCE_TABLE_POPULATED',"
                + "'resourceTableType':'ORG_UNIT_STRUCTURE','sql':'select 1'}]}")
            .formatted(HOOK_UID));
    ObjectNode exported = (ObjectNode) new ObjectMapper().readTree(getHook().toJson());
    assertEquals("RESOURCE_TABLE_POPULATED", exported.get("phase").asText());
    assertEquals("ORG_UNIT_STRUCTURE", exported.get("resourceTableType").asText());
    exported.put("phase", "ANALYTICS_TABLE_POPULATED");
    exported.remove("resourceTableType");
    exported.put("analyticsTableType", "EVENT");
    exported.put("sql", "update analytics_event set value=1");

    assertStatus(HttpStatus.OK, PUT("/analyticsTableHooks/" + HOOK_UID, exported.toString()));

    JsonObject updated = getHook();
    assertEquals("Hook A", updated.getString("name").string());
    assertEquals("ANALYTICS_TABLE_POPULATED", updated.getString("phase").string());
    assertFalse(updated.has("resourceTableType"));
    assertEquals("EVENT", updated.getString("analyticsTableType").string());
    assertEquals("update analytics_event set value=1", updated.getString("sql").string());
  }

  @Test
  void testReplaceTranslationsIsRejectedAsNotTranslatable() {
    importMetadata(
        ("{'analyticsTableHooks':[{'id':'%s','name':'Hook A','phase':'RESOURCE_TABLE_POPULATED',"
                + "'resourceTableType':'ORG_UNIT_STRUCTURE','sql':'select 1'}]}")
            .formatted(HOOK_UID));

    JsonErrorReport error =
        PUT(
                "/analyticsTableHooks/" + HOOK_UID + "/translations",
                "{'translations':[{'locale':'fr','property':'NAME','value':'Nom'}]}")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class)
            .findErrorReport(ErrorCode.E1107);
    assertNotNull(error);
    assertTrue(getHook().getArray("translations").isEmpty());
  }

  @Test
  void testImportWithTranslationsIsRejectedAsNotTranslatable() {
    JsonErrorReport error =
        POST(
                "/metadata",
                ("{'analyticsTableHooks':[{'id':'%s','name':'Hook A',"
                        + "'phase':'RESOURCE_TABLE_POPULATED','resourceTableType':'ORG_UNIT_STRUCTURE',"
                        + "'sql':'select 1','translations':[{'locale':'fr','property':'NAME',"
                        + "'value':'Nom'}]}]}")
                    .formatted(HOOK_UID))
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class)
            .findErrorReport(ErrorCode.E1107);
    assertNotNull(error);
  }

  /** The keys of an exported hook, with href and access set by the web layer. */
  @Test
  void testExportedProperties() {
    importMetadata(
        ("{'analyticsTableHooks':[{'id':'%s','code':'HOOK_A','name':'Hook A',"
                + "'phase':'RESOURCE_TABLE_POPULATED','resourceTableType':'ORG_UNIT_STRUCTURE',"
                + "'sql':'select 1'}]}")
            .formatted(HOOK_UID));

    JsonObject hook = getHook();
    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "displayName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "phase",
            "resourceTableType",
            "sharing",
            "sql",
            "translations"),
        Set.copyOf(hook.names()));
    assertEquals(Set.of("users", "userGroups"), Set.copyOf(hook.getObject("sharing").names()));
    assertTrue(
        hook.getString("href").string().endsWith("/analyticsTableHooks/" + HOOK_UID),
        hook.getString("href").string());
    assertTrue(hook.getObject("access").getBoolean("read").booleanValue());
  }

  /** Same properties, persisted columns and column-derived attributes as before the migration. */
  @Test
  void testSchemaProperties() {
    JsonObject schema = GET("/schemas/analyticsTableHook").content(HttpStatus.OK);
    JsonArray properties = schema.getArray("properties");
    Map<String, String> actual = new HashMap<>();
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      String attributes =
          property.getBoolean("persisted").booleanValue()
              ? "%s required=%s unique=%s length=%s"
                  .formatted(
                      property.getString("propertyType").string(),
                      property.getBoolean("required").booleanValue(),
                      property.getBoolean("unique").booleanValue(),
                      property.getNumber("length").intValue())
              : "transient";
      actual.put(property.getString("name").string(), attributes);
    }

    assertEquals(
        Map.ofEntries(
            Map.entry("access", "transient"),
            Map.entry("analyticsTableType", "CONSTANT required=false unique=false length=50"),
            Map.entry("attributeValues", "transient"),
            Map.entry("code", "IDENTIFIER required=false unique=true length=50"),
            Map.entry("created", "DATE required=false unique=false length=255"),
            Map.entry("createdBy", "transient"),
            Map.entry("displayName", "transient"),
            Map.entry("href", "transient"),
            Map.entry("id", "IDENTIFIER required=false unique=true length=11"),
            Map.entry("lastUpdated", "DATE required=false unique=false length=255"),
            Map.entry("lastUpdatedBy", "REFERENCE required=false unique=false length=255"),
            Map.entry("name", "TEXT required=true unique=true length=255"),
            Map.entry("phase", "CONSTANT required=true unique=false length=50"),
            Map.entry("resourceTableType", "CONSTANT required=false unique=false length=50"),
            Map.entry("sharing", "transient"),
            Map.entry("sql", "TEXT required=true unique=false length=2147483647"),
            Map.entry("translation", "transient"),
            Map.entry("user", "transient")),
        actual);
    assertFalse(schema.getBoolean("shareable").booleanValue());
    assertFalse(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }

  /** Reads the hook from the database, after a flush and with a cleared session and L2 cache. */
  private JsonObject getHook() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/analyticsTableHooks/" + HOOK_UID).content(HttpStatus.OK);
  }

  @Test
  void canDeleteHookWhileFeatureEnabled() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/analyticsTableHooks/",
                "{'name':'ToDelete', 'phase':'RESOURCE_TABLE_POPULATED', 'resourceTableType':'ORG_UNIT_STRUCTURE', 'sql':'update analytics_rs_orgunitstructure set organisationunitid=9'}"));
    assertStatus(HttpStatus.OK, DELETE("/analyticsTableHooks/" + uid));
  }

  @Test
  void nonSuperuserWithHookAuthorityCannotCreateHook() {
    switchToNewUser("hookuser", "F_ANALYTICSTABLEHOOK_ADD", "F_METADATA_IMPORT");

    assertRejected(
        ErrorCode.E3000, POST("/analyticsTableHooks/", hookJson("HookUser001", HOOK_SQL)));
    assertRejected(
        ErrorCode.E3000,
        POST("/analyticsTableHooks?skipValidation=true", hookJson("HookUser002", HOOK_SQL)));
    assertRejected(
        ErrorCode.E3000,
        POST(
            "/metadata?skipValidation=true",
            "{'analyticsTableHooks':[" + hookJson("HookUser003", HOOK_SQL) + "]}"));

    switchToAdminUser();
    assertNull(getHook("HookUser001"));
    assertNull(getHook("HookUser002"));
    assertNull(getHook("HookUser003"));
  }

  @Test
  void nonSuperuserWithHookAuthorityCannotUpdateHook() {
    assertStatus(
        HttpStatus.CREATED, POST("/analyticsTableHooks/", hookJson("HookUser004", HOOK_SQL)));
    switchToNewUser("hookuser", "F_ANALYTICSTABLEHOOK_ADD", "F_METADATA_IMPORT");

    assertRejected(
        ErrorCode.E3001,
        PUT("/analyticsTableHooks/HookUser004", hookJson("HookUser004", "select 2")));
    assertRejected(
        ErrorCode.E3001,
        PATCH(
            "/analyticsTableHooks/HookUser004",
            "[{'op':'replace','path':'/sql','value':'select 3'}]"));
    assertRejected(
        ErrorCode.E3001,
        POST(
            "/metadata?importStrategy=UPDATE",
            "{'analyticsTableHooks':[" + hookJson("HookUser004", "select 4") + "]}"));

    switchToAdminUser();
    assertEquals(HOOK_SQL, getHook("HookUser004").getSql());
  }
}
