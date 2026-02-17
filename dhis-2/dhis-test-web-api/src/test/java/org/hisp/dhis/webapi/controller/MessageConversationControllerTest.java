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

import static com.google.common.collect.Sets.newHashSet;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link MessageConversationController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class MessageConversationControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private ConfigurationService configurationService;
  @Autowired private IdentifiableObjectManager manager;

  @Test
  void testPostJsonObject() {
    assertWebMessage(
        "Created",
        201,
        "OK",
        "Message conversation created",
        POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}")
            .content(HttpStatus.CREATED));
  }

  @Test
  void testPostJsonObject_MissingProperty() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "No recipients selected.",
        POST("/messageConversations/", "{'subject':'Subject','text':'Text','users':[]}")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testDeleteObject() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));
    assertWebMessage(
        "OK", 200, "OK", null, DELETE("/messageConversations/" + uid).content(HttpStatus.OK));
  }

  @Test
  void testPostMessageConversationReply() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));
    assertWebMessage(
        "Created",
        201,
        "OK",
        "Message conversation created",
        POST("/messageConversations/" + uid, "The message").content(HttpStatus.CREATED));
  }

  @Test
  void testPostMessageConversationReply_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "Message conversation does not exist: xyz",
        POST("/messageConversations/xyz", "The message").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testPostMessageConversationReply_NoSuchAttachment() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Attachment 'xyz' not found.",
        POST("/messageConversations/" + uid + "?attachments=xyz", "The message")
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testPostMessageConversationFeedback() {
    assertWebMessage(
        "Created",
        201,
        "OK",
        "Feedback created",
        POST("/messageConversations/feedback?subject=test", "The message")
            .content(HttpStatus.CREATED));
  }

  @Test
  @DisplayName("Change feedback message when in the recipient group")
  void testChangeFeedbackMessage() {
    String uid =
        assertStatus(
            HttpStatus.CREATED, POST("/messageConversations/feedback?subject=test", "The message"));

    User ringo = createUserWithAuth("ringo");
    UserGroup ugA = createUserGroup('A', newHashSet(ringo));
    manager.save(ugA);
    Configuration config = configurationService.getConfiguration();
    config.setFeedbackRecipients(ugA);
    configurationService.setConfiguration(config);

    switchContextToUser(ringo);

    POST("/messageConversations/read?user=%s".formatted(ringo.getUid()), "['%s']".formatted(uid))
        .content(HttpStatus.OK);
  }

  @Test
  @DisplayName("Change feedback message when not in the recipient group")
  void testChangeFeedbackMessageWithoutPermission() {
    String uid =
        assertStatus(
            HttpStatus.CREATED, POST("/messageConversations/feedback?subject=test", "The message"));

    User ringo = switchToNewUser("ringo");

    POST("/messageConversations/read?user=%s".formatted(ringo.getUid()), "['%s']".formatted(uid))
        .content(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Post message to conversation without permission")
  void testPostToConversationWithoutPermission() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));

    switchToNewUser("ringo");

    assertWebMessage(
        "Forbidden",
        403,
        "ERROR",
        "Not authorized to send messages to this conversation.",
        POST("/messageConversations/%s".formatted(uid), "{'text':'text'}")
            .content(HttpStatus.FORBIDDEN));
  }

  @Test
  @DisplayName("Add recipient without permission")
  void testAddRecipientWithoutPermission() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));

    User ringo = switchToNewUser("ringo");

    assertWebMessage(
        "Forbidden",
        403,
        "ERROR",
        "Not authorized to change recipients in this conversation.",
        POST(
                "/messageConversations/%s/recipients".formatted(uid),
                "{'users':[{'id':'%s'}],'userGroups':[],'organisationUnits':[]}"
                    .formatted(ringo.getUid()))
            .content(HttpStatus.FORBIDDEN));
  }

  @Test
  @DisplayName("Change priority without permission")
  void testChangePriorityWithoutPermission() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));

    User ringo = switchToNewUser("ringo");

    assertWebMessage(
        "Forbidden",
        403,
        "ERROR",
        "Not authorized to change priority in this conversation.",
        POST(
                "/messageConversations/%s/priority?messageConversationPriority=HIGH".formatted(uid),
                "{'users':[{'id':'%s'}],'userGroups':[],'organisationUnits':[]}"
                    .formatted(ringo.getUid()))
            .content(HttpStatus.FORBIDDEN));
  }

  @Test
  @DisplayName("Change status without permission")
  void testChangeStatusWithoutPermission() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));

    User ringo = switchToNewUser("ringo");

    assertWebMessage(
        "Forbidden",
        403,
        "ERROR",
        "Not authorized to change status in this conversation.",
        POST(
                "/messageConversations/%s/status?messageConversationStatus=SOLVED".formatted(uid),
                "{'users':[{'id':'%s'}],'userGroups':[],'organisationUnits':[]}"
                    .formatted(ringo.getUid()))
            .content(HttpStatus.FORBIDDEN));
  }

  @Test
  @DisplayName("Assign user without permission")
  void testAssignUsersWithoutPermission() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));

    User ringo = switchToNewUser("ringo");

    assertWebMessage(
        "Forbidden",
        403,
        "ERROR",
        "Not authorized to assign a user to this conversation.",
        POST(
                "/messageConversations/%s/assign?userId=%s".formatted(uid, ringo.getUid()),
                "{'users':[{'id':'%s'}],'userGroups':[],'organisationUnits':[]}"
                    .formatted(ringo.getUid()))
            .content(HttpStatus.FORBIDDEN));
  }

  @Test
  @DisplayName("Delete assigned user without permission")
  void testDeleteAssignUsersWithoutPermission() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));

    User ringo = switchToNewUser("ringo");

    assertWebMessage(
        "Forbidden",
        403,
        "ERROR",
        "Not authorized to remove the assigned user to this conversation.",
        DELETE(
                "/messageConversations/%s/assign".formatted(uid),
                "{'users':[{'id':'%s'}],'userGroups':[],'organisationUnits':[]}"
                    .formatted(ringo.getUid()))
            .content(HttpStatus.FORBIDDEN));
  }

  @Test
  @DisplayName("Change followup without permission")
  void testChangeFollowupWithoutPermission() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));

    User ringo = switchToNewUser("ringo");

    assertWebMessage(
        "Forbidden",
        403,
        "ERROR",
        "Not authorized to change followups in this conversation.",
        POST(
                "/messageConversations/followup?userUid=%s".formatted(ringo.getUid()),
                "['%s']".formatted(uid))
            .content(HttpStatus.FORBIDDEN));
  }

  @Test
  @DisplayName("Change unfollowup without permission")
  void testChangeUnFollowupWithoutPermission() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));

    User ringo = switchToNewUser("ringo");

    assertWebMessage(
        "Forbidden",
        403,
        "ERROR",
        "Not authorized to change unfollowups in this conversation.",
        POST(
                "/messageConversations/unfollowup?userUid=%s".formatted(ringo.getUid()),
                "['%s']".formatted(uid))
            .content(HttpStatus.FORBIDDEN));
  }

  @Test
  @DisplayName("Remove user from conversation without permission")
  void testRemoveUserWithoutPermission() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));

    switchToNewUser("ringo");

    assertWebMessage(
        "Forbidden",
        403,
        "ERROR",
        "Not authorized to remove assigned users to this conversation.",
        DELETE("/messageConversations/%s/%s".formatted(uid, getAdminUid()))
            .content(HttpStatus.FORBIDDEN));
  }

  @Test
  @DisplayName("Remove user from conversation without permission")
  void testRemoveUsersBatchWithoutPermission() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));

    switchToNewUser("ringo");

    assertWebMessage(
        "Forbidden",
        403,
        "ERROR",
        "Not authorized to remove assigned users to this conversation.",
        DELETE("/messageConversations?mc=%s&user=%s".formatted(uid, getAdminUid()))
            .content(HttpStatus.FORBIDDEN));
  }

  @Test
  @DisplayName("Verify Message entity fields are correctly persisted and retrieved")
  void testGetConversationWithMessageFields() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Test Subject','text':'Hello World','users':[{'id':'"
                    + getAdminUid()
                    + "'}]}"));

    JsonObject conversation =
        GET("/messageConversations/" + uid + "?fields=messages[text,sender,internal,lastUpdated]")
            .content(HttpStatus.OK);
    JsonArray messages = conversation.getArray("messages");
    assertFalse(messages.isEmpty(), "Conversation should have messages");

    JsonObject message = messages.getObject(0);
    assertEquals("Hello World", message.getString("text").string());
    assertNotNull(message.get("lastUpdated"), "lastUpdated should be mapped");
    assertFalse(message.getBoolean("internal").booleanValue(), "internal should default to false");
    assertNotNull(message.getObject("sender").getString("id"), "sender should be mapped");
  }

  @Test
  @DisplayName("Verify reply creates a new Message with correct sender mapping")
  void testReplyMessageSenderMapping() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Original','users':[{'id':'"
                    + getAdminUid()
                    + "'}]}"));

    // Reply to conversation
    assertStatus(HttpStatus.CREATED, POST("/messageConversations/" + uid, "Reply message"));

    JsonObject conversation =
        GET("/messageConversations/" + uid + "?fields=messages[text,sender[id]]")
            .content(HttpStatus.OK);
    JsonArray messages = conversation.getArray("messages");
    assertEquals(2, messages.size(), "Should have original + reply");

    // Both messages should have a sender with a valid id
    for (int i = 0; i < messages.size(); i++) {
      JsonObject msg = messages.getObject(i);
      assertNotNull(
          msg.getObject("sender").getString("id").string(),
          "Message " + i + " should have sender mapped");
    }
  }

  @Test
  @DisplayName("Verify internal message flag is persisted")
  void testInternalMessageFlag() {
    // Create a feedback conversation first
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST("/messageConversations/feedback?subject=test", "Feedback message"));

    // Set up feedback recipients group so we can post internal messages
    User ringo = createUserWithAuth("ringo");
    UserGroup ugA = createUserGroup('A', newHashSet(ringo));
    manager.save(ugA);
    Configuration config = configurationService.getConfiguration();
    config.setFeedbackRecipients(ugA);
    configurationService.setConfiguration(config);

    switchContextToUser(ringo);

    // Reply with internal=true
    assertStatus(
        HttpStatus.CREATED,
        POST("/messageConversations/" + uid + "?internal=true", "Internal reply"));

    // Switch back to admin to read the conversation
    switchToAdminUser();

    JsonObject conversation =
        GET("/messageConversations/" + uid + "?fields=messages[text,internal]")
            .content(HttpStatus.OK);
    JsonArray messages = conversation.getArray("messages");

    boolean hasInternal =
        messages.asList(JsonObject.class).stream()
            .anyMatch(m -> m.has("internal") && m.getBoolean("internal").booleanValue());
    assertTrue(hasInternal, "Should have at least one internal message");
  }

  @Test
  @DisplayName("Verify multiple replies maintain correct message ordering and fields")
  void testMultipleRepliesMessageMapping() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'First','users':[{'id':'" + getAdminUid() + "'}]}"));

    assertStatus(HttpStatus.CREATED, POST("/messageConversations/" + uid, "Second"));
    assertStatus(HttpStatus.CREATED, POST("/messageConversations/" + uid, "Third"));

    JsonObject conversation =
        GET("/messageConversations/" + uid + "?fields=messages[text,sender,lastUpdated]")
            .content(HttpStatus.OK);
    JsonArray messages = conversation.getArray("messages");
    assertEquals(3, messages.size(), "Should have 3 messages");

    // All messages should have required fields properly mapped
    for (int i = 0; i < messages.size(); i++) {
      JsonObject msg = messages.getObject(i);
      assertNotNull(msg.getString("text").string(), "Message " + i + " should have text");
      assertNotNull(msg.get("lastUpdated"), "Message " + i + " should have lastUpdated");
      assertNotNull(msg.get("sender"), "Message " + i + " should have sender");
    }
  }

  @Test
  @DisplayName("Change read status without permission")
  void testChangeReadStatusWithoutPermission() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getAdminUid() + "'}]}"));

    User ringo = switchToNewUser("ringo");

    assertWebMessage(
        "Forbidden",
        403,
        "ERROR",
        "Not authorized to change read property of this conversation.",
        POST(
                "/messageConversations/read?user=%s".formatted(ringo.getUid()),
                "['%s']".formatted(uid))
            .content(HttpStatus.FORBIDDEN));
  }
}
