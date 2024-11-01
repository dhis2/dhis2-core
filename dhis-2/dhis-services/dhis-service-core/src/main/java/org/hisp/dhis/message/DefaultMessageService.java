/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.commons.util.TextUtils.LN;
import static org.hisp.dhis.commons.util.TextUtils.removeAnyTrailingSlash;
import static org.hisp.dhis.security.Authorities.ALL;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingsService;
import org.hisp.dhis.util.ObjectUtils;
import org.joda.time.DateTime;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.message.MessageService")
public class DefaultMessageService implements MessageService {
  private static final String COMPLETE_SUBJECT = "Form registered as complete";

  private static final String COMPLETE_TEMPLATE = "completeness_message";

  private static final String MESSAGE_EMAIL_FOOTER_TEMPLATE = "message_email_footer";

  private static final String MESSAGE_PATH = "/dhis-web-messaging/#/";

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final MessageConversationStore messageConversationStore;

  private final UserService userService;

  private final ConfigurationService configurationService;

  private final UserSettingsService userSettingsService;

  private final I18nManager i18nManager;

  private final SystemSettingsProvider settingsProvider;

  private final List<MessageSender> messageSenders;

  private final DhisConfigurationProvider configurationProvider;

  // -------------------------------------------------------------------------
  // MessageService implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long sendTicketMessage(String subject, String text, String metaData) {
    User currentUser = getCurrentUserOrNull();

    MessageConversationParams params =
        new MessageConversationParams.Builder()
            .withRecipients(getFeedbackRecipients())
            .withSender(currentUser)
            .withSubject(subject)
            .withText(text)
            .withMessageType(MessageType.TICKET)
            .withMetaData(metaData)
            .withStatus(MessageConversationStatus.OPEN)
            .build();

    return sendMessage(params);
  }

  @Override
  @Transactional
  public long sendPrivateMessage(
      Set<User> recipients,
      String subject,
      String text,
      String metaData,
      Set<FileResource> attachments) {
    User currentUser = getCurrentUserOrNull();

    MessageConversationParams params =
        new MessageConversationParams.Builder()
            .withRecipients(recipients)
            .withSender(currentUser)
            .withSubject(subject)
            .withText(text)
            .withMessageType(MessageType.PRIVATE)
            .withMetaData(metaData)
            .withAttachments(attachments)
            .build();

    return sendMessage(params);
  }

  @Override
  @Transactional
  public long sendSystemMessage(Set<User> recipients, String subject, String text) {
    MessageConversationParams params =
        new MessageConversationParams.Builder()
            .withRecipients(recipients)
            .withSubject(subject)
            .withText(text)
            .withMessageType(MessageType.SYSTEM)
            .build();

    return sendMessage(params);
  }

  @Override
  @Transactional
  public long sendValidationMessage(
      Set<User> recipients, String subject, String text, MessageConversationPriority priority) {
    MessageConversationParams params =
        new MessageConversationParams.Builder()
            .withRecipients(recipients)
            .withSubject(subject)
            .withText(text)
            .withMessageType(MessageType.VALIDATION_RESULT)
            .withStatus(MessageConversationStatus.OPEN)
            .withPriority(priority)
            .build();

    return sendMessage(params);
  }

  @Override
  @Transactional
  public long sendMessage(MessageConversationParams params, UserDetails actingUser) {
    MessageConversation conversation = params.createMessageConversation();
    long id = saveMessageConversation(conversation, actingUser);

    Message message = new Message(params.getText(), params.getMetadata(), params.getSender());

    message.setAttachments(params.getAttachments());
    conversation.addMessage(message);

    params.getRecipients().stream()
        .filter(r -> !r.equals(params.getSender()))
        .forEach(recipient -> conversation.addUserMessage(new UserMessage(recipient, false)));

    if (params.getSender() != null) {
      conversation.addUserMessage(new UserMessage(params.getSender(), true));
    }

    String footer = getMessageFooter(conversation);

    invokeMessageSenders(
        params.getSubject(),
        params.getText(),
        footer,
        params.getSender(),
        params.getRecipients(),
        params.isForceNotification());

    return id;
  }

  @Override
  @Transactional
  public long sendMessage(MessageConversationParams params) {
    UserDetails currentUserDetails;
    if (CurrentUserUtil.hasCurrentUser()) {
      currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    } else {
      currentUserDetails = new SystemUser();
    }
    return sendMessage(params, currentUserDetails);
  }

  @Override
  @Async
  @Transactional
  public void asyncSendSystemErrorNotification(@Nonnull String subject, @Nonnull Throwable t) {
    String title = settingsProvider.getCurrentSettings().getApplicationTitle();
    String baseUrl = configurationProvider.getServerBaseUrl();

    String text =
        new StringBuilder()
            .append(subject + LN + LN)
            .append("System title: " + title + LN)
            .append("Base URL: " + baseUrl + LN)
            .append("Time: " + new DateTime().toString() + LN)
            .append("Message: " + t.getMessage() + LN + LN)
            .append("Cause: " + DebugUtils.getStackTrace(t.getCause()))
            .toString();

    MessageConversationParams params =
        new MessageConversationParams.Builder()
            .withRecipients(getFeedbackRecipients())
            .withSubject(subject)
            .withText(text)
            .withMessageType(MessageType.SYSTEM)
            .build();

    sendMessage(params, new SystemUser());
  }

  @Override
  @Transactional
  public void sendReply(
      MessageConversation conversation,
      String text,
      String metaData,
      boolean internal,
      Set<FileResource> attachments) {
    User sender = getCurrentUserOrNull();

    Message message = new Message(text, metaData, sender, internal);

    message.setAttachments(attachments);

    conversation.markReplied(sender, message);

    updateMessageConversation(conversation);

    Set<User> users = conversation.getUsers();

    if (conversation.getMessageType().equals(MessageType.TICKET) && internal) {
      users =
          users.stream()
              .filter(user -> hasAccessToManageFeedbackMessages(UserDetails.fromUser(user)))
              .collect(Collectors.toSet());
    }

    invokeMessageSenders(
        conversation.getSubject(), text, null, sender, new HashSet<>(users), false);
  }

  @Override
  @Transactional
  public long sendCompletenessMessage(CompleteDataSetRegistration registration) {
    DataSet dataSet = registration.getDataSet();

    if (dataSet == null) {
      return 0;
    }

    UserGroup userGroup = dataSet.getNotificationRecipients();

    User sender = getCurrentUserOrNull();

    // data set completed through sms
    if (sender == null) {
      return 0;
    }

    Set<User> recipients = new HashSet<>();

    if (userGroup != null) {
      recipients.addAll(new HashSet<>(userGroup.getMembers()));
    }

    if (dataSet.isNotifyCompletingUser()) {
      recipients.add(sender);
    }

    if (recipients.isEmpty()) {
      return 0;
    }

    String text = new VelocityManager().render(registration, COMPLETE_TEMPLATE);

    MessageConversation conversation =
        new MessageConversation(COMPLETE_SUBJECT, sender, MessageType.SYSTEM);

    conversation.addMessage(new Message(text, null, sender));

    for (User user : recipients) {
      conversation.addUserMessage(new UserMessage(user));
    }

    if (!conversation.getUserMessages().isEmpty()) {
      long id = saveMessageConversation(conversation);

      invokeMessageSenders(
          COMPLETE_SUBJECT, text, null, sender, new HashSet<>(conversation.getUsers()), false);

      return id;
    }

    return 0;
  }

  @Override
  @Transactional
  public long saveMessageConversation(MessageConversation conversation) {
    messageConversationStore.save(conversation);
    return conversation.getId();
  }

  @Override
  @Transactional
  public long saveMessageConversation(MessageConversation conversation, UserDetails actingUser) {
    messageConversationStore.save(conversation, actingUser, false);
    return conversation.getId();
  }

  @Override
  @Transactional
  public void updateMessageConversation(MessageConversation conversation) {
    messageConversationStore.update(conversation);
  }

  @Override
  @Transactional(readOnly = true)
  public MessageConversation getMessageConversation(long id) {
    return messageConversationStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public MessageConversation getMessageConversation(String uid) {
    MessageConversation mc = messageConversationStore.getByUid(uid);

    if (mc == null) {
      return null;
    }

    User currentUser = getCurrentUserOrNull();

    mc.setFollowUp(mc.isFollowUp(currentUser));
    mc.setRead(mc.isRead(currentUser));

    return messageConversationStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public long getUnreadMessageConversationCount() {
    User currentUser = getCurrentUserOrNull();
    return messageConversationStore.getUnreadUserMessageConversationCount(currentUser);
  }

  @Override
  @Transactional(readOnly = true)
  public long getUnreadMessageConversationCount(User user) {
    return messageConversationStore.getUnreadUserMessageConversationCount(user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MessageConversation> getMessageConversations() {
    User currentUser = getCurrentUserOrNull();
    return messageConversationStore.getMessageConversations(
        currentUser, null, false, false, null, null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MessageConversation> getMessageConversations(int first, int max) {
    User currentUser = getCurrentUserOrNull();
    return messageConversationStore.getMessageConversations(
        currentUser, null, false, false, first, max);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MessageConversation> getMatchingExtId(String extMessageId) {
    return messageConversationStore.getMessagesConversationFromSenderMatchingExtMessageId(
        extMessageId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MessageConversation> getMessageConversations(User user, Collection<String> uid) {
    List<MessageConversation> conversations = messageConversationStore.getMessageConversations(uid);

    // Set transient properties

    for (MessageConversation mc : conversations) {
      mc.setFollowUp(mc.isFollowUp(user));
      mc.setRead(mc.isRead(user));
    }

    return conversations;
  }

  @Override
  @Transactional
  public void deleteMessages(User user) {
    messageConversationStore.deleteMessages(user);
    messageConversationStore.deleteUserMessages(user);
    messageConversationStore.removeUserFromMessageConversations(user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserMessage> getLastRecipients(int first, int max) {
    User currentUser = getCurrentUserOrNull();
    return messageConversationStore.getLastRecipients(currentUser, first, max);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasAccessToManageFeedbackMessages(UserDetails userDetails) {
    boolean isInGroup = configurationService.isUserInFeedbackRecipientUserGroup(userDetails);
    return isInGroup || userDetails.isAuthorized(ALL);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public Set<User> getFeedbackRecipients() {
    UserGroup feedbackRecipients = configurationService.getConfiguration().getFeedbackRecipients();

    if (feedbackRecipients != null) {
      return feedbackRecipients.getMembers();
    }

    return new HashSet<>();
  }

  @Override
  @Transactional(readOnly = true)
  public Set<User> getSystemUpdateNotificationRecipients() {
    UserGroup feedbackRecipients =
        configurationService.getConfiguration().getSystemUpdateNotificationRecipients();

    if (feedbackRecipients == null) {
      return Collections.emptySet();
    }

    return feedbackRecipients.getMembers();
  }

  private void invokeMessageSenders(
      String subject, String text, String footer, User sender, Set<User> users, boolean forceSend) {
    for (MessageSender messageSender : messageSenders) {
      log.debug("Invoking message sender: " + messageSender.getClass().getSimpleName());

      messageSender.sendMessageAsync(
          subject, text, footer, sender, new HashSet<>(users), forceSend);
    }
  }

  private String getMessageFooter(MessageConversation conversation) {
    HashMap<String, Object> values = new HashMap<>(2);

    String baseUrl = configurationProvider.getServerBaseUrl();

    if (baseUrl == null) {
      return StringUtils.EMPTY;
    }

    UserSettings settings =
        conversation.getCreatedBy() == null
            ? UserSettings.getCurrentSettings()
            : userSettingsService.getUserSettings(conversation.getCreatedBy().getUsername(), true);

    Locale locale = settings.getUserUiLocale();

    locale = ObjectUtils.firstNonNull(locale, LocaleManager.DEFAULT_LOCALE);

    values.put(
        "responseUrl", constructUrl(baseUrl, conversation.getMessageType(), conversation.getUid()));
    values.put("i18n", i18nManager.getI18n(locale));

    return new VelocityManager().render(values, MESSAGE_EMAIL_FOOTER_TEMPLATE);
  }

  private Object constructUrl(
      @Nonnull String baseUrl, @Nonnull MessageType messageType, @Nonnull String uid) {
    String expectedBaseUrlFormat = removeAnyTrailingSlash(baseUrl);
    return expectedBaseUrlFormat + MESSAGE_PATH + messageType + "/" + uid;
  }

  private User getCurrentUserOrNull() {
    if (CurrentUserUtil.hasCurrentUser()) {
      String currentUsername = CurrentUserUtil.getCurrentUsername();
      return userService.getUserByUsername(currentUsername);
    }
    return null;
  }
}
