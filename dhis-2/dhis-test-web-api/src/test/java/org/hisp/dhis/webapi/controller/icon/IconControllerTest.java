/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertContainsAll;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.controller.json.JsonPager;
import org.hisp.dhis.webapi.json.domain.JsonIcon;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

class IconControllerTest extends DhisControllerIntegrationTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String key1 = "key1";
  private static final String key2 = "key2";
  private static final String key3 = "key3";

  private static final String description = "description";

  private static final Set<String> keywordsList1 = Set.of("k1", "k2");

  private static final Set<String> keywordsList2 = Set.of("k1", "m1");

  private static final Set<String> keywordsList3 = Set.of("k1", "m1", "m2");

  @Autowired private ContextService contextService;

  @Test
  void shouldCreateIconWhenFileResourceExist() throws IOException {

    JsonWebMessage message = createIcon(createFileResource(), keywordsList1, key1);

    assertEquals(String.format("Icon created with key %s", key1), message.getMessage());
  }

  @Test
  void shouldUpdateExistingIcon() throws IOException {
    String updatedDescription = "updatedDescription";
    String updatedKeywords = "['new k1', 'new k2']";
    createIcon(createFileResource(), keywordsList1, key1);

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
  void shouldDeleteIconWhenKeyExists() throws IOException {
    createIcon(createFileResource(), keywordsList1, key1);

    JsonObject response = DELETE(String.format("/icons/%s", key1)).content();

    assertEquals(
        String.format("Icon with key %s deleted", key1), response.getString("message").string());
  }

  @Test
  void shouldGetIconWhenIconKeyExists() throws IOException {
    String fileResourceId = createFileResource();
    createIcon(fileResourceId, keywordsList1, key1);

    JsonObject response = GET(String.format("/icons/%s", key1)).content();

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
  void shouldGetIconsFilteredByKeywords() throws IOException {
    String fileResourceId1 = createFileResource();
    String fileResourceId2 = createFileResource();
    String fileResourceId3 = createFileResource();
    createIcon(fileResourceId1, keywordsList1, key1);
    createIcon(fileResourceId2, keywordsList2, key2);
    createIcon(fileResourceId3, keywordsList3, key3);

    JsonObject response =
        GET("/icons?keywords=m1,k1&type=custom&fields=id,key,description,keywords,href")
            .content(HttpStatus.OK);

    assertNotNull(response);

    JsonList<JsonIcon> icons = response.getList("icons", JsonIcon.class);

    assertEquals(2, icons.size());
    assertContainsAll(List.of(key2, key3), icons, JsonIcon::getKey);
  }

  @Test
  void shouldGetIconsFilteredByKeyWithPagingDisabled() throws IOException {
    String fileResourceId = createFileResource();
    createIcon(fileResourceId, keywordsList1, key1);

    JsonObject content = GET("/icons?keys=" + key1 + "&fields=*").content(HttpStatus.OK);

    JsonList<JsonIcon> icons = content.getList("icons", JsonIcon.class);

    assertIcons(icons.get(0), keywordsList1, fileResourceId, key1);
  }

  @Test
  void shouldGetAllKeywords() throws IOException {

    String fileResourceId1 = createFileResource();
    createIcon(fileResourceId1, keywordsList2, key1);

    String fileResourceId2 = createFileResource();
    createIcon(fileResourceId2, keywordsList1, key2);

    JsonArray response = GET("/icons/keywords").content(HttpStatus.OK);

    assertNotNull(response);

    List<String> keywords = response.stringValues();

    assertEquals(391, keywords.size());

    assertThat(keywords, hasItems("m1", "k1", "k2"));
  }

  @Test
  void shouldGetIconsWithPager() throws IOException {

    String fileResourceId1 = createFileResource();
    createIcon(fileResourceId1, keywordsList2, key1);

    String fileResourceId2 = createFileResource();
    createIcon(fileResourceId2, keywordsList2, key2);

    String fileResourceId3 = createFileResource();
    createIcon(fileResourceId3, keywordsList2, key3);

    JsonObject iconResponse = GET("/icons?paging=true&page=2&pageSize=2").content(HttpStatus.OK);
    JsonPager pager = iconResponse.get("pager", JsonPager.class);

    JsonList<JsonIcon> icons = iconResponse.getList("icons", JsonIcon.class);

    assertHasMember(iconResponse, "pager");

    assertEquals(2, pager.getPage());
    assertEquals(899, pager.getTotal());
    assertEquals(2, pager.getPageSize());
    assertEquals(450, pager.getPageCount());

    assertEquals(
        2,
        icons.size(),
        () -> String.format("mismatch in number of expected Icon(s), fetched %s", icons));
  }

  @Test
  void shouldGetIconsWithDefaultPager() throws IOException {

    String fileResourceId1 = createFileResource();
    createIcon(fileResourceId1, keywordsList2, key1);

    String fileResourceId2 = createFileResource();
    createIcon(fileResourceId2, keywordsList2, key2);

    String fileResourceId3 = createFileResource();
    createIcon(fileResourceId3, keywordsList2, key3);

    JsonObject iconResponse = GET("/icons").content(HttpStatus.OK);

    JsonList<JsonIcon> icons = iconResponse.getList("icons", JsonIcon.class);

    assertHasNoMember(iconResponse, "pager");
    assertEquals(
        900,
        icons.size(),
        () -> String.format("mismatch in number of expected Icon(s), fetched %s", icons));
  }

  private JsonWebMessage createIcon(String fileResourceId, Set<String> keywords, String key) {

    IconRequest request =
        IconRequest.builder()
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
      e.printStackTrace();
    }

    return message;
  }

  private String createFileResource() throws IOException {
    InputStream in = getClass().getResourceAsStream("/icon/test-image.png");
    MockMultipartFile image = new MockMultipartFile("file", "test-image.png", "image/png", in);

    HttpResponse response = POST_MULTIPART("/fileResources?domain=CUSTOM_ICON", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");

    return savedObject.getString("id").string();
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
