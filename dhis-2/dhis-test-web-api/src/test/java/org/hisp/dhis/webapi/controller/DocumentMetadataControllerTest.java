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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

/** Metadata import, export and schema of documents. */
@Transactional
class DocumentMetadataControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String DOCUMENT_UID = "DocUid00001";

  private static final String ATTRIBUTE_UID = "DocAttr0001";

  /** The identifiable object property set, which the dashboard search serializes documents with. */
  private static final Set<String> IDENTIFIABLE_KEYS =
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
          "translations",
          "user");

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void shouldCreateAndUpdateDocumentViaMetadataImport() {
    importMetadata(externalDocument("DOC_A", "Imported document", "https://example.org/a", true));

    JsonObject created = getDocument(DOCUMENT_UID);
    assertEquals("DOC_A", created.getString("code").string());
    assertEquals("Imported document", created.getString("name").string());
    assertEquals("https://example.org/a", created.getString("url").string());
    assertTrue(created.getBoolean("external").booleanValue());
    assertEquals("text/html", created.getString("contentType").string());
    assertTrue(created.getBoolean("attachment").booleanValue());
    assertEquals("rw------", created.getObject("sharing").getString("public").string());
    assertEquals(
        "Document importé",
        created.getArray("translations").getObject(0).getString("value").string());
    assertEquals(
        "value A", created.getArray("attributeValues").getObject(0).getString("value").string());

    importMetadata(
        externalDocument("DOC_B", "Re-imported document", "https://example.org/b", false));

    JsonObject updated = getDocument(DOCUMENT_UID);
    assertEquals("DOC_B", updated.getString("code").string());
    assertEquals("Re-imported document", updated.getString("name").string());
    assertEquals("https://example.org/b", updated.getString("url").string());
    assertFalse(updated.getBoolean("attachment").booleanValue());
  }

  @Test
  void shouldUpdateDocumentWhenExportedDocumentWithChangesIsPut() {
    importMetadata(externalDocument("DOC_A", "Imported document", "https://example.org/a", true));
    String exported = getDocument(DOCUMENT_UID).toJson();
    String changed =
        replace(exported, "\"name\":\"Imported document\"", "\"name\":\"Changed document\"");
    changed =
        replace(changed, "\"url\":\"https://example.org/a\"", "\"url\":\"https://example.org/c\"");
    changed = replace(changed, "\"attachment\":true", "\"attachment\":false");
    changed = replace(changed, "\"value\":\"Document importé\"", "\"value\":\"Document changé\"");

    assertStatus(HttpStatus.OK, PUT("/documents/" + DOCUMENT_UID, changed));

    JsonObject updated = getDocument(DOCUMENT_UID);
    assertEquals("Changed document", updated.getString("name").string());
    assertEquals("https://example.org/c", updated.getString("url").string());
    assertFalse(updated.getBoolean("attachment").booleanValue());
    assertEquals(
        "Document changé",
        updated.getArray("translations").getObject(0).getString("value").string());
  }

  @Test
  void shouldExportDocumentProperties() {
    importMetadata(externalDocument("DOC_A", "Imported document", "https://example.org/a", true));

    JsonObject document = getDocument(DOCUMENT_UID);

    Set<String> expected = new TreeSet<>(IDENTIFIABLE_KEYS);
    expected.addAll(Set.of("attachment", "contentType", "external", "url"));
    assertEquals(expected, new TreeSet<>(document.names()));
    assertTrue(document.getString("href").string().endsWith("/documents/" + DOCUMENT_UID));
    assertEquals("Imported document", document.getString("displayName").string());
  }

  /**
   * The dashboard search serializes documents with the identifiable object properties only. Access
   * and href are not set on a document read in a new request, so they are left out.
   */
  @Test
  void shouldServeDocumentsInDashboardSearchWithIdentifiableProperties() {
    importMetadata(externalDocument("DOC_A", "Imported document", "https://example.org/a", true));
    startNewRequestSession();

    JsonArray resources =
        GET("/dashboards/q/Imported?max=RESOURCES").content(HttpStatus.OK).getArray("resources");

    assertEquals(1, resources.size());
    JsonObject resource = resources.getObject(0);
    Set<String> expected = new TreeSet<>(IDENTIFIABLE_KEYS);
    expected.removeAll(Set.of("access", "href"));
    assertEquals(expected, new TreeSet<>(resource.names()));
    assertEquals(DOCUMENT_UID, resource.getString("id").string());
    assertEquals("Imported document", resource.getString("displayName").string());
  }

  /** Field filtering serves every document property of a dashboard item's resources. */
  @Test
  void shouldServeDocumentPropertiesOfDashboardItemResources() {
    importMetadata(externalDocument("DOC_A", "Imported document", "https://example.org/a", true));
    importMetadata(
        """
        {'dashboards':[{'id':'DashUid0001','name':'Documents',
          'dashboardItems':[{'id':'DashItem001','type':'RESOURCES',
            'resources':[{'id':'%s'}]}]}]}
        """
            .formatted(DOCUMENT_UID));
    startNewRequestSession();

    JsonObject resource =
        GET("/dashboards/DashUid0001?fields=dashboardItems[resources[id,displayName,url,external]]")
            .content(HttpStatus.OK)
            .getArray("dashboardItems")
            .getObject(0)
            .getArray("resources")
            .getObject(0);

    assertEquals(Set.of("id", "displayName", "url", "external"), Set.copyOf(resource.names()));
    assertEquals("Imported document", resource.getString("displayName").string());
    assertEquals("https://example.org/a", resource.getString("url").string());
    assertTrue(resource.getBoolean("external").booleanValue());
  }

  /** The OpenAPI document describes document resources as identifiable objects. */
  @Test
  void shouldDescribeDocumentResourcesAsIdentifiableObjectsInOpenApi() {
    JsonObject schemas =
        GET("/openapi/openapi.json").content(HttpStatus.OK).getObject("components.schemas");

    for (String owner : Set.of("DashboardItem", "DashboardSearchResult")) {
      assertEquals(
          "#/components/schemas/BaseIdentifiableObject",
          schemas
              .getObject(owner)
              .getObject("properties.resources.items")
              .getString("$ref")
              .string(),
          owner);
    }
  }

  @Test
  void shouldServeContentOfImportedFileDocument() {
    String fileResource = upload("file content");
    importMetadata(
        """
        {'documents':[{'id':'%s','name':'File document','url':'%s','external':false}]}
        """
            .formatted(DOCUMENT_UID, fileResource));

    startNewRequestSession();
    assertEquals(fileResource, getDocument(DOCUMENT_UID).getString("url").string());
    assertEquals("file content", GET("/documents/" + DOCUMENT_UID + "/data").content("text/plain"));
  }

  @Test
  void shouldRedirectToUrlOfExternalDocument() {
    importMetadata(externalDocument("DOC_A", "Imported document", "https://example.org/a", true));
    startNewRequestSession();

    HttpResponse response = GET("/documents/" + DOCUMENT_UID + "/data");

    assertEquals(HttpStatus.FOUND, response.status());
    assertEquals("https://example.org/a", response.header("Location"));
  }

  /** The schema properties of a document with their persisted and required flags and lengths. */
  @Test
  void shouldDescribeDocumentSchemaProperties() {
    JsonObject schema = GET("/schemas/document").content(HttpStatus.OK);
    JsonArray properties = schema.getArray("properties");
    Set<String> names = new TreeSet<>();
    Set<String> persisted = new TreeSet<>();
    Set<String> required = new TreeSet<>();
    Map<String, JsonObject> byName = new HashMap<>();
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      String name = property.getString("name").string();
      names.add(name);
      byName.put(name, property);
      if (property.getBoolean("persisted").booleanValue()) {
        persisted.add(name);
      }
      if (property.getBoolean("required").booleanValue()) {
        required.add(name);
      }
    }

    Set<String> expectedNames = new TreeSet<>(IDENTIFIABLE_KEYS);
    expectedNames.remove("translations");
    expectedNames.addAll(Set.of("attachment", "contentType", "external", "translation", "url"));
    assertEquals(expectedNames, names);
    assertEquals(
        Set.of(
            "attachment",
            "attributeValues",
            "code",
            "contentType",
            "created",
            "createdBy",
            "external",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "translation",
            "url"),
        persisted);
    assertEquals(Set.of("external", "name", "url"), required);
    assertEquals(230, byName.get("name").getNumber("length").intValue());
    assertEquals(Integer.MAX_VALUE, byName.get("url").getNumber("length").intValue());
    assertEquals(255, byName.get("contentType").getNumber("length").intValue());
    assertEquals(50, byName.get("code").getNumber("length").intValue());
    assertEquals("URL", byName.get("href").getString("propertyType").string());
    assertEquals("document", schema.getString("name").string());
    assertTrue(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
  }

  private static String externalDocument(String code, String name, String url, boolean attachment) {
    return """
        {'attributes':[{'id':'%s','name':'Document attribute','valueType':'TEXT',
          'documentAttribute':true}],
         'documents':[{'id':'%s','code':'%s','name':'%s','url':'%s','external':true,
          'contentType':'text/html','attachment':%s,
          'translations':[{'locale':'fr','property':'NAME','value':'Document importé'}],
          'attributeValues':[{'attribute':{'id':'%s'},'value':'value A'}],
          'sharing':{'public':'rw------','external':false}}]}
        """
        .formatted(ATTRIBUTE_UID, DOCUMENT_UID, code, name, url, attachment, ATTRIBUTE_UID);
  }

  private String upload(String content) {
    return POST_MULTIPART(
            "/fileResources?domain=DOCUMENT",
            new MockMultipartFile("file", "document.txt", "text/plain", content.getBytes(UTF_8)))
        .content(HttpStatus.ACCEPTED)
        .getObject("response")
        .getObject("fileResource")
        .getString("id")
        .string();
  }

  /** Replaces the single occurrence of {@code from}, failing if it does not occur exactly once. */
  private static String replace(String json, String from, String to) {
    assertEquals(1, json.split(Pattern.quote(from), -1).length - 1, () -> from + " in " + json);
    return json.replace(from, to);
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }

  /** Reads the document from the database, bypassing the session and the L2 cache. */
  private JsonObject getDocument(String uid) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/documents/" + uid).content(HttpStatus.OK);
  }
}
