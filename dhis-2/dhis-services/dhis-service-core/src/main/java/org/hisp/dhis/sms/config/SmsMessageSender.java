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
package org.hisp.dhis.sms.config;

import static org.hisp.dhis.commons.util.TextUtils.LN;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageBatchStatus;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.outboundmessage.OutboundMessageResponseSummary;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.sms.outbound.OutboundSmsStatus;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * @author Nguyen Kim Lai
 */
@Slf4j
@Component("smsMessageSender")
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SmsMessageSender implements MessageSender {
  private static final String NO_CONFIG = "No default gateway configured";

  private static final String BATCH_ABORTED = "Aborted sending message batch";

  private static final Integer MAX_RECIPIENTS_ALLOWED = 200;

  private static final Pattern SUMMARY_PATTERN =
      Pattern.compile("\\s*High\\s*[0-9]*\\s*,\\s*medium\\s*[0-9]*\\s*,\\s*low\\s*[0-9]*\\s*");

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final GatewayAdministrationService gatewayAdminService;

  private final List<SmsGateway> smsGateways;

  private final UserSettingService userSettingService;

  private final OutboundSmsService outboundSmsService;

  private final SystemSettingManager systemSettingManager;

  public SmsMessageSender(
      GatewayAdministrationService gatewayAdminService,
      List<SmsGateway> smsGateways,
      UserSettingService userSettingService,
      OutboundSmsService outboundSmsService,
      SystemSettingManager systemSettingManager) {

    Preconditions.checkNotNull(gatewayAdminService);
    Preconditions.checkNotNull(smsGateways);
    Preconditions.checkNotNull(outboundSmsService);
    Preconditions.checkNotNull(userSettingService);
    Preconditions.checkState(!smsGateways.isEmpty());
    Preconditions.checkNotNull(systemSettingManager);

    this.gatewayAdminService = gatewayAdminService;
    this.smsGateways = smsGateways;
    this.userSettingService = userSettingService;
    this.outboundSmsService = outboundSmsService;
    this.systemSettingManager = systemSettingManager;
  }

  // -------------------------------------------------------------------------
  // Implementation methods
  // -------------------------------------------------------------------------

  @Override
  public OutboundMessageResponse sendMessage(
      String subject, String text, String footer, User sender, Set<User> users, boolean forceSend) {
    if (!hasRecipients(users)) {
      log.debug(GatewayResponse.NO_RECIPIENT.getResponseMessage());

      return new OutboundMessageResponse(
          GatewayResponse.NO_RECIPIENT.getResponseMessage(), GatewayResponse.NO_RECIPIENT, false);
    }

    Set<User> toSendList;

    toSendList =
        users.stream().filter(u -> forceSend || isQualifiedReceiver(u)).collect(Collectors.toSet());

    if (toSendList.isEmpty()) {
      log.debug(GatewayResponse.SMS_DISABLED.getResponseMessage());

      return new OutboundMessageResponse(
          GatewayResponse.SMS_DISABLED.getResponseMessage(), GatewayResponse.SMS_DISABLED, false);
    }

    // Extract summary from text in case of COLLECTIVE_SUMMARY

    text = SUMMARY_PATTERN.matcher(text).find() ? StringUtils.substringBefore(text, LN) : text;

    return sendMessage(subject, text, SmsUtils.getRecipientsPhoneNumber(toSendList));
  }

  @Async
  @Override
  public Future<OutboundMessageResponse> sendMessageAsync(
      String subject, String text, String footer, User sender, Set<User> users, boolean forceSend) {
    OutboundMessageResponse response = sendMessage(subject, text, footer, sender, users, forceSend);
    return new AsyncResult<>(response);
  }

  @Override
  public OutboundMessageResponse sendMessage(String subject, String text, String recipient) {
    return sendMessage(
        subject,
        text,
        StringUtils.isBlank(recipient) ? new HashSet<>() : Sets.newHashSet(recipient));
  }

  @Override
  public OutboundMessageResponse sendMessage(String subject, String text, Set<String> recipients) {
    if (!hasRecipients(recipients)) {
      log.debug(GatewayResponse.NO_RECIPIENT.getResponseMessage());

      return new OutboundMessageResponse(
          GatewayResponse.NO_RECIPIENT.getResponseMessage(), GatewayResponse.NO_RECIPIENT, false);
    }

    SmsGatewayConfig defaultGateway = gatewayAdminService.getDefaultGateway();

    if (defaultGateway == null) {
      log.debug("Gateway configuration does not exist");

      return new OutboundMessageResponse(
          NO_CONFIG, GatewayResponse.NO_GATEWAY_CONFIGURATION, false);
    }

    return sendMessage(subject, text, normalizePhoneNumbers(recipients), defaultGateway);
  }

  @Override
  public OutboundMessageResponseSummary sendMessageBatch(OutboundMessageBatch batch) {
    if (batch == null) {
      return createMessageResponseSummary(
          BATCH_ABORTED, DeliveryChannel.SMS, OutboundMessageBatchStatus.ABORTED, 0);
    }

    SmsGatewayConfig defaultGateway = gatewayAdminService.getDefaultGateway();

    if (defaultGateway == null) {
      return createMessageResponseSummary(
          NO_CONFIG, DeliveryChannel.SMS, OutboundMessageBatchStatus.FAILED, batch.size());
    }

    batch
        .getMessages()
        .forEach(item -> item.setRecipients(normalizePhoneNumbers(item.getRecipients())));

    sliceBatchMessages(batch);

    for (SmsGateway smsGateway : smsGateways) {
      if (smsGateway.accept(defaultGateway)) {
        List<OutboundMessageResponse> responses = smsGateway.sendBatch(batch, defaultGateway);

        return generateSummary(responses, batch);
      }
    }

    return createMessageResponseSummary(
        NO_CONFIG, DeliveryChannel.SMS, OutboundMessageBatchStatus.ABORTED, batch.size());
  }

  @Override
  public ListenableFuture<OutboundMessageResponseSummary> sendMessageBatchAsync(
      OutboundMessageBatch batch) {
    OutboundMessageResponseSummary summary = sendMessageBatch(batch);
    return new AsyncResult<>(summary);
  }

  @Override
  public boolean isConfigured() {
    return gatewayAdminService.hasGateways();
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private boolean isQualifiedReceiver(User user) {
    Serializable userSetting =
        userSettingService.getUserSetting(UserSettingKey.MESSAGE_SMS_NOTIFICATION, user);

    return userSetting != null ? (Boolean) userSetting : false;
  }

  private OutboundMessageResponse sendMessage(
      String subject, String text, Set<String> recipients, SmsGatewayConfig gatewayConfig) {
    OutboundMessageResponse status = null;

    OutboundSms outboundSms = new OutboundSms(subject, text, recipients);

    for (SmsGateway smsGateway : smsGateways) {
      if (smsGateway.accept(gatewayConfig)) {

        if (text.length()
            > Optional.ofNullable(gatewayConfig.getMaxSmsLength())
                .map(Integer::parseInt)
                .orElseGet(
                    () ->
                        systemSettingManager.getSystemSetting(
                            SettingKey.SMS_MAX_LENGTH, Integer.class))) {
          return new OutboundMessageResponse(
              GatewayResponse.SMS_TEXT_MESSAGE_TOO_LONG.getResponseMessage(),
              GatewayResponse.SMS_TEXT_MESSAGE_TOO_LONG,
              false);
        }

        List<String> temp = new ArrayList<>(recipients);

        List<List<String>> slices = Lists.partition(temp, MAX_RECIPIENTS_ALLOWED);

        for (List<String> to : slices) {
          log.debug("Sending SMS to " + to);

          status = smsGateway.send(subject, text, new HashSet<>(to), gatewayConfig);

          handleResponse(status, outboundSms);
        }

        return status;
      }
    }

    return new OutboundMessageResponse(NO_CONFIG, GatewayResponse.NO_GATEWAY_CONFIGURATION, false);
  }

  private Set<String> normalizePhoneNumbers(Set<String> to) {
    return to.stream().map(SmsUtils::removePhoneNumberPrefix).collect(Collectors.toSet());
  }

  private void sliceBatchMessages(OutboundMessageBatch batch) {
    List<OutboundMessage> messages;

    messages =
        batch.getMessages().stream()
            .flatMap(m -> sliceMessageRecipients(m).stream())
            .collect(Collectors.toList());

    batch.setMessages(messages);
  }

  private List<OutboundMessage> sliceMessageRecipients(OutboundMessage message) {
    List<String> temp = new ArrayList<>(message.getRecipients());

    List<List<String>> slices = Lists.partition(temp, MAX_RECIPIENTS_ALLOWED);

    return slices.stream()
        .map(to -> new OutboundMessage(message.getSubject(), message.getText(), new HashSet<>(to)))
        .collect(Collectors.toList());
  }

  private void handleResponse(OutboundMessageResponse status, OutboundSms sms) {
    Set<GatewayResponse> okCodes =
        Sets.newHashSet(
            GatewayResponse.RESULT_CODE_0,
            GatewayResponse.RESULT_CODE_200,
            GatewayResponse.RESULT_CODE_202);

    GatewayResponse gatewayResponse = (GatewayResponse) status.getResponseObject();

    if (okCodes.contains(gatewayResponse)) {
      log.debug("SMS sent");

      status.setOk(true);
      sms.setStatus(OutboundSmsStatus.SENT);
    } else {
      log.error("SMS failed, failure cause: " + gatewayResponse.getResponseMessage());

      status.setOk(false);
      sms.setStatus(OutboundSmsStatus.FAILED);
    }

    outboundSmsService.save(sms);
    status.setDescription(gatewayResponse.getResponseMessage());
    status.setResponseObject(gatewayResponse);
  }

  private OutboundMessageResponseSummary generateSummary(
      List<OutboundMessageResponse> statuses, OutboundMessageBatch batch) {
    Set<GatewayResponse> okCodes =
        Sets.newHashSet(
            GatewayResponse.RESULT_CODE_0,
            GatewayResponse.RESULT_CODE_200,
            GatewayResponse.RESULT_CODE_202);

    OutboundMessageResponseSummary summary = new OutboundMessageResponseSummary();

    int total, sent = 0;

    boolean ok = true;

    String errorMessage = StringUtils.EMPTY;

    total = batch.getMessages().size();

    for (OutboundMessageResponse status : statuses) {
      if (okCodes.contains(status.getResponseObject())) {
        sent++;
      } else {
        ok = false;

        errorMessage = status.getDescription();
      }
    }

    summary.setTotal(total);
    summary.setChannel(DeliveryChannel.SMS);
    summary.setSent(sent);
    summary.setFailed(total - sent);

    if (!ok) {
      summary.setBatchStatus(OutboundMessageBatchStatus.FAILED);
      summary.setErrorMessage(errorMessage);

      log.error(errorMessage);
    } else {
      summary.setBatchStatus(OutboundMessageBatchStatus.COMPLETED);
      summary.setResponseMessage("SENT");

      log.debug("SMS batch processed successfully");
    }

    return summary;
  }

  private boolean hasRecipients(Set<?> collection) {
    return collection != null && !collection.isEmpty();
  }

  private OutboundMessageResponseSummary createMessageResponseSummary(
      String responseMessage,
      DeliveryChannel channel,
      OutboundMessageBatchStatus batchStatus,
      int total) {
    OutboundMessageResponseSummary summary =
        new OutboundMessageResponseSummary(responseMessage, channel, batchStatus);
    summary.setTotal(total);

    log.warn(responseMessage);

    return summary;
  }
}
