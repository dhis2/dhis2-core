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
package org.hisp.dhis.message;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 */
public interface MessageService {
  String META_USER_AGENT = "User-agent: ";

  long sendTicketMessage(String subject, String text, String metaData);

  long sendPrivateMessage(
      Set<User> recipients,
      String subject,
      String text,
      String metaData,
      Set<FileResource> attachments);

  long sendSystemMessage(Set<User> recipients, String subject, String text);

  long sendValidationMessage(
      Set<User> recipients, String subject, String text, MessageConversationPriority priority);

  long sendMessage(MessageConversationParams params);

  long sendSystemErrorNotification(String subject, Throwable t);

  void sendReply(
      MessageConversation conversation,
      String text,
      String metaData,
      boolean internal,
      Set<FileResource> attachments);

  long saveMessageConversation(MessageConversation conversation);

  void updateMessageConversation(MessageConversation conversation);

  long sendCompletenessMessage(CompleteDataSetRegistration registration);

  MessageConversation getMessageConversation(long id);

  MessageConversation getMessageConversation(String uid);

  long getUnreadMessageConversationCount();

  long getUnreadMessageConversationCount(User user);

  /**
   * Get all MessageConversations for the current user.
   *
   * @return a list of all message conversations for the current user.
   */
  List<MessageConversation> getMessageConversations();

  List<MessageConversation> getMessageConversations(int first, int max);

  List<MessageConversation> getMatchingExtId(String extId);

  List<MessageConversation> getMessageConversations(User user, Collection<String> uids);

  void deleteMessages(User sender);

  List<UserMessage> getLastRecipients(int first, int max);

  /**
   * Returns true if user is part of the feedback recipients group.
   *
   * @param user user to check
   * @return true if user is part of the feedback recipients group.
   */
  boolean hasAccessToManageFeedbackMessages(User user);

  Set<User> getFeedbackRecipients();

  Set<User> getSystemUpdateNotificationRecipients();
}
