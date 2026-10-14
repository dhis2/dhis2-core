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
import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpClientAdapter.ContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests how interpretation comments are written and served by the interpretations API. */
@Transactional
class InterpretationCommentControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private VisualizationService visualizationService;

  @Autowired private InterpretationService interpretationService;

  @Autowired private EntityManagerFactory entityManagerFactory;

  private String interpretationUid;

  private String mentionedUsername;

  @BeforeEach
  void setUp() {
    mentionedUsername = createUserWithAuth("commentmentioned").getUsername();
    Visualization visualization = createVisualization('A');
    visualizationService.save(visualization);
    Interpretation interpretation = new Interpretation(visualization, null, "Interpretation");
    interpretationService.saveInterpretation(interpretation);
    interpretationUid = interpretation.getUid();
  }

  /** The property set of the schema, as served before the comment had an annotation mapping. */
  @Test
  void testSchemaProperties() {
    JsonArray properties =
        GET("/schemas/interpretationComment").content(HttpStatus.OK).getArray("properties");

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
            "mentions",
            "name",
            "sharing",
            "text",
            "translation",
            "user"),
        propertyNames(properties, false));
    assertEquals(
        Set.of("created", "createdBy", "id", "lastUpdated", "mentions", "text"),
        propertyNames(properties, true));
    assertEquals(
        Integer.MAX_VALUE,
        properties.asList(JsonObject.class).stream()
            .filter(p -> "text".equals(p.getString("name").string()))
            .findFirst()
            .orElseThrow()
            .getNumber("length")
            .intValue());
  }

  @Test
  void testCommentJsonProperties() {
    String commentUid = postComment("Hello @" + mentionedUsername);

    JsonObject comment = getComment(commentUid);

    assertEquals(
        Set.of(
            "id",
            "created",
            "lastUpdated",
            "text",
            "mentions",
            "createdBy",
            "user",
            "sharing",
            "access",
            "translations",
            "attributeValues"),
        Set.copyOf(comment.names()));
    assertEquals("Hello @" + mentionedUsername, comment.getString("text").string());
    assertEquals(getAdminUid(), comment.getObject("createdBy").getString("id").string());
    assertEquals(getAdminUid(), comment.getObject("user").getString("id").string());
    assertEquals(Set.of("users", "userGroups"), Set.copyOf(comment.getObject("sharing").names()));
    assertEquals(
        List.of(mentionedUsername),
        comment.getArray("mentions").asList(JsonObject.class).stream()
            .map(m -> m.getString("username").string())
            .toList());
    assertEquals(Set.of("username", "created"), mentionNames(comment));
  }

  @Test
  void testUpdateCommentChangesTextAndMentions() {
    String commentUid = postComment("No mention");
    assertEquals(0, getComment(commentUid).getArray("mentions").size());

    assertStatus(
        HttpStatus.NO_CONTENT,
        PUT(
            "/interpretations/" + interpretationUid + "/comments/" + commentUid,
            ContentType("text/plain"),
            Body("Now @" + mentionedUsername)));

    JsonObject comment = getComment(commentUid);
    assertEquals("Now @" + mentionedUsername, comment.getString("text").string());
    assertEquals(
        mentionedUsername,
        comment.getArray("mentions").getObject(0).getString("username").string());
  }

  private String postComment(String text) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/interpretations/" + interpretationUid + "/comments",
            ContentType("text/plain"),
            Body(text)));
  }

  /**
   * The comment as served in a new session, read from the database rather than the session or the
   * second-level cache.
   */
  private JsonObject getComment(String commentUid) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/interpretations/{uid}?fields=comments[*]", interpretationUid)
        .content(HttpStatus.OK)
        .getArray("comments")
        .asList(JsonObject.class)
        .stream()
        .filter(c -> commentUid.equals(c.getString("id").string()))
        .findFirst()
        .orElseThrow();
  }

  private static Set<String> mentionNames(JsonObject comment) {
    return Set.copyOf(comment.getArray("mentions").getObject(0).names());
  }

  private static Set<String> propertyNames(JsonArray properties, boolean persistedOnly) {
    return properties.asList(JsonObject.class).stream()
        .filter(p -> !persistedOnly || p.getBoolean("persisted").booleanValue())
        .map(p -> p.getString("name").string())
        .collect(Collectors.toSet());
  }
}
