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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.fileresource.FileResourceContentStore;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Tests the {@link MessageConversationController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class MessageConversationControllerTest extends DhisControllerConvenienceTest {
  @Autowired private FileResourceService fileResourceService;
  @Autowired private FileResourceContentStore fileResourceContentStore;

  @Test
  void testPostJsonObject_AttachmentOfAnotherUser() {
    String fileUid = uploadAttachment();
    switchToNewUser("attachmentSender");
    assertEquals(
        HttpStatus.CONFLICT,
        POST("/messageConversations/", messageWithAttachment(fileUid)).status());
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}/data", fileUid).status());
  }

  @Test
  void testPostMessageConversationReply_AttachmentOfAnotherUser() {
    String fileUid = uploadAttachment();
    switchToNewUser("replySender");
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'"
                    + getSuperuserUid()
                    + "'}]}"));
    assertEquals(
        HttpStatus.CONFLICT,
        POST("/messageConversations/" + uid + "?attachments=" + fileUid, "The message").status());
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}/data", fileUid).status());
  }

  @Test
  void testPostJsonObject_OwnAttachment() {
    switchToNewUser("attachmentOwner");
    String fileUid = uploadAttachment();
    assertStatus(
        HttpStatus.CREATED, POST("/messageConversations/", messageWithAttachment(fileUid)));
    assertEquals("attached", GET("/fileResources/{uid}/data", fileUid).content("text/plain"));
  }

  private String messageWithAttachment(String fileUid) {
    return "{'subject':'Subject','text':'Text','users':[{'id':'"
        + getSuperuserUid()
        + "'}],'attachments':[{'id':'"
        + fileUid
        + "'}]}";
  }

  private String uploadAttachment() {
    byte[] bytes = "attached".getBytes();
    String uid =
        POST_MULTIPART(
                "/fileResources?domain=MESSAGE_ATTACHMENT",
                new MockMultipartFile("file", "attachment.txt", "text/plain", bytes))
            .content(HttpStatus.ACCEPTED)
            .getObject("response")
            .getObject("fileResource")
            .getString("id")
            .string();
    fileResourceContentStore.saveFileResourceContent(
        fileResourceService.getFileResource(uid), bytes);
    return uid;
  }

  @Test
  void testPostJsonObject() {
    assertWebMessage(
        "Created",
        201,
        "OK",
        "Message conversation created",
        POST(
                "/messageConversations/",
                "{'subject':'Subject','text':'Text','users':[{'id':'" + getSuperuserUid() + "'}]}")
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
                "{'subject':'Subject','text':'Text','users':[{'id':'"
                    + getSuperuserUid()
                    + "'}]}"));
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
                "{'subject':'Subject','text':'Text','users':[{'id':'"
                    + getSuperuserUid()
                    + "'}]}"));
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
                "{'subject':'Subject','text':'Text','users':[{'id':'"
                    + getSuperuserUid()
                    + "'}]}"));
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
}
