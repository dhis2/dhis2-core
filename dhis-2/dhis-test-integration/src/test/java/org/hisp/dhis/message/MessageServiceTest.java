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
package org.hisp.dhis.message;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class MessageServiceTest extends PostgresIntegrationTestBase {

  @Autowired private MessageService messageService;

  private User sender;

  private User userA;

  private User userB;

  private Set<User> users;

  @BeforeAll
  void setUp() {
    sender = makeUser("S");
    userA = makeUser("A");
    userA.setPhoneNumber("40342434");
    userB = makeUser("B");
    userService.addUser(sender);
    userService.addUser(userA);
    userService.addUser(userB);
    users = new HashSet<>();
    users.add(userA);
    users.add(userB);
  }

  @Test
  void testSaveMessageConversationA() {
    MessageConversation conversationA =
        new MessageConversation("SubjectA", sender, MessageType.PRIVATE);
    MessageConversation conversationB =
        new MessageConversation("SubjectB", sender, MessageType.PRIVATE);
    long idA = messageService.saveMessageConversation(conversationA);
    long idB = messageService.saveMessageConversation(conversationB);
    conversationA = messageService.getMessageConversation(idA);
    conversationB = messageService.getMessageConversation(idB);
    assertNotNull(conversationA);
    assertEquals("SubjectA", conversationA.getSubject());
    assertNotNull(conversationB);
    assertEquals("SubjectB", conversationB.getSubject());
  }

  @Test
  void testSaveMessageB() {
    MessageConversation conversation =
        new MessageConversation("Subject", sender, MessageType.PRIVATE);
    UserMessage userMessageA = new UserMessage(userA);
    UserMessage userMessageB = new UserMessage(userB);
    conversation.addUserMessage(userMessageA);
    conversation.addUserMessage(userMessageB);
    Message contentA = new Message("TextA", "MetaA", sender);
    Message contentB = new Message("TextB", "MetaB", sender);
    conversation.addMessage(contentA);
    conversation.addMessage(contentB);
    long id = messageService.saveMessageConversation(conversation);
    conversation = messageService.getMessageConversation(id);
    assertNotNull(conversation);
    assertEquals("Subject", conversation.getSubject());
    assertEquals(2, conversation.getUserMessages().size());
    assertTrue(conversation.getUserMessages().contains(userMessageA));
    assertTrue(conversation.getUserMessages().contains(userMessageB));
    assertEquals(2, conversation.getMessages().size());
    assertTrue(conversation.getMessages().contains(contentA));
    assertTrue(conversation.getMessages().contains(contentB));
  }

  @Test
  void testDeleteMessage() {
    MessageConversation conversation =
        new MessageConversation("Subject", sender, MessageType.PRIVATE);
    UserMessage userMessageA = new UserMessage(userA);
    UserMessage userMessageB = new UserMessage(userB);
    conversation.addUserMessage(userMessageA);
    conversation.addUserMessage(userMessageB);
    Message contentA = new Message("TextA", "MetaA", sender);
    Message contentB = new Message("TextB", "MetaB", sender);
    conversation.addMessage(contentA);
    conversation.addMessage(contentB);
    long id = messageService.saveMessageConversation(conversation);
    conversation = messageService.getMessageConversation(id);
    assertNotNull(conversation);
    messageService.deleteMessages(userA);
    messageService.deleteMessages(userB);
    messageService.deleteMessages(sender);
  }

  @Test
  void testSendMessage() {
    long id = messageService.sendPrivateMessage(users, "Subject", "Text", "Meta", null);
    MessageConversation conversation = messageService.getMessageConversation(id);
    assertNotNull(conversation);
    assertEquals("Subject", conversation.getSubject());
    assertEquals(3, conversation.getUserMessages().size());
    assertEquals(1, conversation.getMessages().size());
    assertTrue(conversation.getMessages().iterator().next().getText().equals("Text"));
  }

  @Test
  void testSendFeedback() {
    User adminUser = getAdminUser();
    injectSecurityContextUser(adminUser);
    User user = createUserWithAuth("feedback", "ALL");
    injectSecurityContextUser(user);
    long id = messageService.sendTicketMessage("Subject", "Text", "Meta");
    MessageConversation conversation = messageService.getMessageConversation(id);
    assertNotNull(conversation);
    assertEquals("Subject", conversation.getSubject());
    assertEquals(1, conversation.getMessages().size());
    assertEquals("Text", conversation.getMessages().iterator().next().getText());
  }

  @Test
  void testSendReply() {
    MessageConversation message = new MessageConversation("Subject", sender, MessageType.PRIVATE);
    message.addMessage(new Message("TextA", "MetaA", sender));
    long id = messageService.saveMessageConversation(message);
    messageService.sendReply(message, "TextB", "MetaB", false, null);
    message = messageService.getMessageConversation(id);
    assertNotNull(message);
    assertEquals("Subject", message.getSubject());
    assertEquals(2, message.getMessages().size());
  }

  @Test
  void testGetMessageConversations() {
    MessageConversation conversationA =
        new MessageConversation("SubjectA", sender, MessageType.PRIVATE);
    MessageConversation conversationB =
        new MessageConversation("SubjectB", sender, MessageType.PRIVATE);
    MessageConversation conversationC =
        new MessageConversation("SubjectC", userA, MessageType.PRIVATE);
    messageService.saveMessageConversation(conversationA);
    messageService.saveMessageConversation(conversationB);
    messageService.saveMessageConversation(conversationC);
    String uidA = conversationA.getUid();
    String uidB = conversationB.getUid();
    messageService.saveMessageConversation(conversationA);
    messageService.saveMessageConversation(conversationB);
    messageService.saveMessageConversation(conversationC);
    Collection<String> uids = Sets.newHashSet(uidA, uidB);
    List<MessageConversation> conversations = messageService.getMessageConversations(sender, uids);
    assertTrue(conversations.contains(conversationA));
    assertTrue(conversations.contains(conversationB));
    assertFalse(conversations.contains(conversationC));
  }

  @Test
  void testSendMessageWithNoCurrentUser() {
    clearSecurityContext();
    MessageConversationParams params =
        new MessageConversationParams.Builder()
            .withRecipients(Set.of(userA))
            .withSubject("subject")
            .withText("text")
            .withMessageType(MessageType.SYSTEM)
            .build();

    assertDoesNotThrow(() -> messageService.sendMessage(params));
  }
}
