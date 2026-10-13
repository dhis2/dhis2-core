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

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the per-user state ({@code UserMessage}) of message conversations through the read, unread,
 * followup and unfollowup endpoints, and its JSON shape.
 */
@Transactional
class MessageConversationUserMessagesControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private EntityManagerFactory entityManagerFactory;

  private String recipientUid;

  private String conversationUid;

  @BeforeEach
  void setUp() {
    recipientUid = createUserWithAuth("usermessagerecipient").getUid();
    conversationUid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'%s'}]}"
                    .formatted(recipientUid)));
  }

  @Test
  void testSendingCreatesReadStateOfSenderAndRecipient() {
    assertEquals(Map.of(getAdminUid(), "read", recipientUid, "unread"), userMessageStates());
  }

  @Test
  void testMarkUnreadAndReadChangesOnlyTheCurrentUser() {
    assertStatus(HttpStatus.OK, POST("/messageConversations/" + conversationUid + "/unread"));
    assertEquals(Map.of(getAdminUid(), "unread", recipientUid, "unread"), userMessageStates());

    assertStatus(HttpStatus.OK, POST("/messageConversations/" + conversationUid + "/read"));
    assertEquals(Map.of(getAdminUid(), "read", recipientUid, "unread"), userMessageStates());
  }

  @Test
  void testFollowupAndUnfollowupChangesOnlyTheCurrentUser() {
    assertStatus(
        HttpStatus.OK, POST("/messageConversations/followup", "['%s']".formatted(conversationUid)));
    assertEquals(
        Map.of(getAdminUid(), "read,followUp", recipientUid, "unread"), userMessageStates());

    assertStatus(
        HttpStatus.OK,
        POST("/messageConversations/unfollowup", "['%s']".formatted(conversationUid)));
    assertEquals(Map.of(getAdminUid(), "read", recipientUid, "unread"), userMessageStates());
  }

  /** The JSON properties of a user message and of its user, as on play/dev. */
  @Test
  void testUserMessageJsonProperties() {
    JsonObject userMessage =
        GET("/messageConversations/" + conversationUid + "?fields=userMessages")
            .content(HttpStatus.OK)
            .getArray("userMessages")
            .asList(JsonObject.class)
            .first(um -> recipientUid.equals(um.getObject("user").getString("id").string()));

    assertEquals(Set.of("key", "user", "read", "followUp"), Set.copyOf(userMessage.names()));
    assertEquals(
        Set.of("id", "code", "name", "displayName", "username"),
        Set.copyOf(userMessage.getObject("user").names()));
    UUID.fromString(userMessage.getString("key").string());
  }

  /**
   * The persisted state of each user message as {@code user uid -> read|unread[,followUp]}. The
   * session is flushed and cleared and the second-level cache evicted first, so it is read from the
   * database.
   */
  private Map<String, String> userMessageStates() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET(
            "/messageConversations/"
                + conversationUid
                + "?fields=userMessages[user[id],read,followUp]")
        .content(HttpStatus.OK)
        .getArray("userMessages")
        .asList(JsonObject.class)
        .stream()
        .collect(
            Collectors.toMap(
                um -> um.getObject("user").getString("id").string(),
                um ->
                    (um.getBoolean("read").booleanValue() ? "read" : "unread")
                        + (um.getBoolean("followUp").booleanValue() ? ",followUp" : "")));
  }
}
