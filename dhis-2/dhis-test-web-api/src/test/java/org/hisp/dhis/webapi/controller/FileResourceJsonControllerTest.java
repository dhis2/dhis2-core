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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonSchema;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

/** Pins the JSON shape of file resources in the responses that render them. */
@Transactional
class FileResourceJsonControllerTest extends PostgresControllerIntegrationTestBase {

  /**
   * The key set of an uploaded file resource. Null values are omitted: {@code code} is never set by
   * an upload, and {@code href}/{@code access} are only set when requested with {@code fields} (top
   * level). Nested {@code fileResource[*]} does not get them either, as on play/dev.
   */
  private static final Set<String> FILE_RESOURCE_KEYS =
      Set.of(
          "id",
          "name",
          "displayName",
          "created",
          "lastUpdated",
          "createdBy",
          "lastUpdatedBy",
          "user",
          "translations",
          "sharing",
          "attributeValues",
          "contentType",
          "contentLength",
          "contentMd5",
          "assigned",
          "domain",
          "hasMultipleStorageFiles",
          "storageStatus");

  /**
   * The identifiable object key set that GET /api/icons/{key} writes for its file resource ({@code
   * code}, {@code href} and {@code access} are null and omitted).
   */
  private static final Set<String> ICON_FILE_RESOURCE_KEYS =
      Set.of(
          "id",
          "name",
          "displayName",
          "created",
          "lastUpdated",
          "createdBy",
          "lastUpdatedBy",
          "user",
          "translations",
          "sharing",
          "attributeValues");

  @Test
  void schemaKeepsNameAndPropertySet() {
    JsonSchema schema = GET("/schemas/fileResource").content().as(JsonSchema.class);

    assertEquals("identifiableObject", schema.getName());
    Set<String> names = new HashSet<>();
    schema.getProperties().forEach(p -> names.add(p.getName()));
    assertEquals(
        Set.of(
            "access",
            "assigned",
            "attributeValues",
            "code",
            "contentLength",
            "contentMd5",
            "contentType",
            "created",
            "createdBy",
            "displayName",
            "domain",
            "hasMultipleStorageFiles",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "storageStatus",
            "translation",
            "user"),
        names);
  }

  @Test
  void uploadResponseIncludesSharingSetOnSave() throws IOException {
    JsonObject fileResource = upload("ORG_UNIT", "ou.png");

    assertEquals(FILE_RESOURCE_KEYS, Set.copyOf(fileResource.names()));
    assertEquals(getAdminUid(), fileResource.getString("sharing.owner").string());
    assertEquals("ORG_UNIT", fileResource.getString("domain").string());
    assertEquals("PENDING", fileResource.getString("storageStatus").string());
  }

  @Test
  void getWithoutFieldsWritesAllFileResourceProperties() throws IOException {
    String uid = upload("ORG_UNIT", "ou.png").getString("id").string();
    startNewRequestSession();

    JsonObject fileResource = GET("/fileResources/{uid}", uid).content();

    assertEquals(FILE_RESOURCE_KEYS, Set.copyOf(fileResource.names()));
    assertEquals("ou.png", fileResource.getString("displayName").string());
    assertEquals("image/png", fileResource.getString("contentType").string());
    assertEquals(getAdminUid(), fileResource.getString("createdBy.id").string());
  }

  @Test
  void getWithAllFieldsAddsHrefAndAccess() throws IOException {
    String uid = upload("DOCUMENT", "doc.png").getString("id").string();
    startNewRequestSession();

    JsonObject fileResource = GET("/fileResources/{uid}?fields=*", uid).content();

    Set<String> expected = new HashSet<>(FILE_RESOURCE_KEYS);
    expected.addAll(List.of("href", "access"));
    assertEquals(expected, Set.copyOf(fileResource.names()));
    assertTrue(fileResource.getString("href").string().endsWith("/api/fileResources/" + uid));
  }

  @Test
  void iconByKeyWritesIdentifiablePropertiesOfFileResource() throws IOException {
    String uid = createIcon("icon-a");
    startNewRequestSession();

    JsonObject fileResource = GET("/icons/icon-a").content().getObject("fileResource");

    assertEquals(ICON_FILE_RESOURCE_KEYS, Set.copyOf(fileResource.names()));
    assertEquals(uid, fileResource.getString("id").string());
    assertEquals("icon.png", fileResource.getString("displayName").string());
  }

  @Test
  void iconListWritesAllRequestedFileResourceProperties() throws IOException {
    String uid = createIcon("icon-b");
    startNewRequestSession();

    JsonList<JsonObject> icons =
        GET("/icons?keys=icon-b&fields=fileResource[*]")
            .content()
            .getList("icons", JsonObject.class);

    JsonObject fileResource = icons.get(0).getObject("fileResource");
    assertEquals(FILE_RESOURCE_KEYS, Set.copyOf(fileResource.names()));
    assertEquals(uid, fileResource.getString("id").string());
    assertEquals("ICON", fileResource.getString("domain").string());
  }

  @Test
  void openApiDocumentsIconFileResourceAsIdentifiableObject() {
    JsonObject iconProperties =
        GET("/openapi.json").content().getObject("components.schemas.Icon.properties");

    assertEquals(
        "#/components/schemas/BaseIdentifiableObject",
        iconProperties.getObject("fileResource").getString("$ref").string());
  }

  private String createIcon(String key) throws IOException {
    String uid = upload("ICON", "icon.png").getString("id").string();
    POST(
            "/icons",
            "{'key':'%s','description':'d','keywords':['k'],'fileResourceId':'%s'}"
                .formatted(key, uid))
        .content(HttpStatus.CREATED);
    return uid;
  }

  private JsonObject upload(String domain, String filename) throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile("file", filename, "image/png", Files.readAllBytes(file.toPath()));
    return POST_MULTIPART("/fileResources?domain=" + domain, image)
        .content(HttpStatus.ACCEPTED)
        .getObject("response")
        .getObject("fileResource");
  }
}
