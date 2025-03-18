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
package org.hisp.dhis.webapi.controller.icon;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertContainsAll;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasMember;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.icon.AddIconRequest;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonIcon;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.webapi.controller.json.JsonPager;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class IconControllerTest extends PostgresControllerIntegrationTestBase {
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String key1 = "key1";
  private static final String key2 = "key2";
  private static final String key3 = "key3";

  private static final String description = "description";

  private static final Set<String> keywordsList1 = Set.of("k1", "k2");

  private static final Set<String> keywordsList2 = Set.of("k1", "m1");

  private static final Set<String> keywordsList3 = Set.of("k1", "m1", "m2");

  @Autowired private ContextService contextService;
  @Autowired private FileResourceService fileResourceService;

  @Test
  void shouldCreateIconWhenFileResourceExist() throws ConflictException {
    JsonWebMessage message = createIcon(keywordsList1, key1);

    assertEquals(String.format("Icon created with key %s", key1), message.getMessage());
  }

  @Test
  void shouldUpdateExistingIcon() throws ConflictException {
    String updatedDescription = "updatedDescription";
    String updatedKeywords = "['new k1', 'new k2']";
    createIcon(keywordsList1, key1);

    JsonObject response =
        PUT(
                String.format("/icons/%s", key1),
                "{'key':'"
                    + key1
                    + "', 'description':'"
                    + updatedDescription
                    + "', 'keywords':"
                    + updatedKeywords
                    + "}")
            .content();

    assertEquals(
        String.format("Icon with key %s updated", key1), response.getString("message").string());
  }

  @Test
  void shouldFailUpdateIconIfIconDoesNotExist() throws ConflictException {
    createIcon(keywordsList1, key1);

    assertEquals(
        "Icon with id key-not-existent could not be found.",
        PUT("/icons/key-not-existent", "{'keywords':[], 'description':''}")
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void shouldDeleteIconWhenKeyExists() throws ConflictException {
    createIcon(keywordsList1, key1);

    JsonObject response = DELETE(String.format("/icons/%s", key1)).content();

    assertEquals(
        String.format("Icon with key %s deleted", key1), response.getString("message").string());
  }

  @Test
  void shouldGetIconWhenIconKeyExists() throws ConflictException {
    String fileResourceId = createFileResource();
    createIcon(fileResourceId, keywordsList1, key1);

    JsonObject response = GET("/icons/%s", key1).content();

    assertEquals(key1, response.getString("key").string());
    assertEquals(description, response.getString("description").string());
    assertEquals(fileResourceId, response.getObject("fileResource").getString("id").string());
    assertEquals(keywordsList1, new HashSet<>(response.getArray("keywords").stringValues()));
    assertEquals(
        getCurrentUser().getUid(), response.getObject("createdBy").getString("id").string());
    assertEquals(
        String.format(contextService.getApiPath() + "/icons/%s/icon", key1),
        response.getString("href").string());
  }

  @Test
  void shouldGetIconData() throws ConflictException {
    FileResource file = createFileResource("image/png", "file content");
    createIcon(file.getUid(), keywordsList1, key1);

    HttpResponse response = GET("/icons/%s/icon", key1);

    assertEquals(HttpStatus.OK, response.status());
    String oneYearInSeconds = "31536000";
    assertEquals("max-age=" + oneYearInSeconds, response.header("Cache-Control"));
    assertEquals(Long.toString(file.getContentLength()), response.header("Content-Length"));
    assertEquals("filename=" + file.getName(), response.header("Content-Disposition"));
    assertEquals("file content", response.content("image/png"));
  }

  @Test
  void shouldGetIconsFilteredByKeywords() throws ConflictException {
    createIcon(keywordsList1, key1);
    createIcon(keywordsList2, key2);
    createIcon(keywordsList3, key3);

    JsonObject response =
        GET("/icons?keywords=m1,k1&type=custom&fields=id,key,description,keywords,href")
            .content(HttpStatus.OK);

    assertNotNull(response);

    JsonList<JsonIcon> icons = response.getList("icons", JsonIcon.class);

    assertContainsAll(List.of(key2, key3), icons, JsonIcon::getKey);
  }

  @Test
  void shouldGetIconsFilteredByKeyWithPagingDisabled() throws ConflictException {
    String fileResourceId = createFileResource();
    createIcon(fileResourceId, keywordsList1, key1);

    JsonObject content = GET("/icons?keys=" + key1 + "&fields=*").content(HttpStatus.OK);

    JsonList<JsonIcon> icons = content.getList("icons", JsonIcon.class);

    assertIcons(icons.get(0), keywordsList1, fileResourceId, key1);
  }

  @Test
  void shouldGetIconsWithPager() throws ConflictException {
    createIcon(keywordsList2, key1);
    createIcon(keywordsList2, key2);
    createIcon(keywordsList2, key3);

    JsonObject iconResponse = GET("/icons?paging=true&page=2&pageSize=2").content(HttpStatus.OK);
    JsonPager pager = iconResponse.get("pager", JsonPager.class);

    JsonList<JsonIcon> icons = iconResponse.getList("icons", JsonIcon.class);

    assertHasMember(iconResponse, "pager");

    assertEquals(2, pager.getPage());
    assertEquals(3, pager.getTotal());
    assertEquals(2, pager.getPageSize());
    assertEquals(2, pager.getPageCount());
    assertNotNull(pager.getString("prevPage").string());

    assertEquals(
        1,
        icons.size(),
        () -> String.format("mismatch in number of expected Icon(s), fetched %s", icons));
  }

  @Test
  void shouldGetIconsWithDefaultPager() throws ConflictException {
    createIcon(keywordsList2, key1);
    createIcon(keywordsList2, key2);
    createIcon(keywordsList2, key3);

    JsonObject iconResponse = GET("/icons").content(HttpStatus.OK);

    JsonList<JsonIcon> icons = iconResponse.getList("icons", JsonIcon.class);

    assertHasMember(iconResponse, "pager");
    assertEquals(
        3,
        icons.size(),
        () -> String.format("mismatch in number of expected Icon(s), fetched %s", icons));
  }

  @Test
  void testRepairPhantomIcons() {
    JsonWebMessage msg = PATCH("/icons").content().as(JsonWebMessage.class);
    assertEquals("0 icons repaired", msg.getMessage());
  }

  private JsonWebMessage createIcon(Set<String> keywords, String key) throws ConflictException {
    return createIcon(createFileResource(), keywords, key);
  }

  private JsonWebMessage createIcon(String fileResourceId, Set<String> keywords, String key) {
    AddIconRequest request =
        AddIconRequest.builder()
            .key(key)
            .fileResourceId(fileResourceId)
            .keywords(keywords)
            .description("description")
            .build();

    JsonWebMessage message = null;
    try {
      message =
          POST("/icons", mapper.writeValueAsString(request))
              .content(HttpStatus.CREATED)
              .as(JsonWebMessage.class);
    } catch (JsonProcessingException e) {
      fail("Could not serialize request to JSON", e);
    }

    return message;
  }

  private String createFileResource() throws ConflictException {
    return createFileResource("image/png", "file content").getUid();
  }

  private FileResource createFileResource(String contentType, String content)
      throws ConflictException {
    byte[] data = content.getBytes();
    FileResource fr = createFileResource('A', data);
    fr.setContentType(contentType);
    fr.setDomain(FileResourceDomain.ICON);
    fileResourceService.syncSaveFileResource(fr, data);
    fr.setStorageStatus(FileResourceStorageStatus.STORED);
    return fr;
  }

  private void assertIcons(JsonIcon icon, Set<String> keywords, String fileResourceId, String key) {
    String actualKey = icon.getString("key").string();
    String actualDescription = icon.getString("description").string();
    String actualFileResourceId = icon.getObject("fileResource").getString("id").string();
    List<String> actualKeywords = icon.getArray("keywords").stringValues();
    assertAll(
        () ->
            assertEquals(
                key,
                actualKey,
                String.format("Expected IconKey was %s but found %s", key, actualKey)),
        () ->
            assertEquals(
                description,
                actualDescription,
                String.format(
                    "Expected Description was %s but found %s", description, actualDescription)),
        () ->
            assertEquals(
                fileResourceId,
                actualFileResourceId,
                String.format(
                    "Expected FileResourceId was %s but found %s",
                    fileResourceId, actualFileResourceId)),
        () -> assertContainsOnly(keywords, actualKeywords));
  }
}
