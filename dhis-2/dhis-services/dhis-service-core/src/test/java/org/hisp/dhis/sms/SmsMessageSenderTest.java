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
package org.hisp.dhis.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageBatchStatus;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.outboundmessage.OutboundMessageResponseSummary;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.sms.config.BulkSmsGatewayConfig;
import org.hisp.dhis.sms.config.BulkSmsHttpGateway;
import org.hisp.dhis.sms.config.GatewayAdministrationService;
import org.hisp.dhis.sms.config.SmsGateway;
import org.hisp.dhis.sms.config.SmsGatewayConfig;
import org.hisp.dhis.sms.config.SmsMessageSender;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Zubair Asghar.
 */
@ExtendWith(MockitoExtension.class)
class SmsMessageSenderTest {
  private static final Integer MAX_ALLOWED_RECIPIENTS = 200;

  private static final String NO_CONFIG = "No default gateway configured";

  private SmsMessageSender smsMessageSender;

  @Mock private UserSettingService userSettingService;

  @Mock private GatewayAdministrationService gatewayAdministrationService;

  @Mock private OutboundSmsService outboundSmsService;

  @Mock private BulkSmsHttpGateway bulkSmsGateway;

  @Mock private SystemSettingManager systemSettingManager;

  private SmsGatewayConfig smsGatewayConfig;

  private OutboundMessageResponse okStatus;

  private OutboundMessageResponse failedStatus;

  private final List<OutboundMessageResponse> summaryResponses = new ArrayList<>();

  private List<OutboundMessage> outboundMessages = new ArrayList<>();

  private final Set<String> recipientsNonNormalized =
      Sets.newHashSet("+4740222222", "0047407777777");

  private final Set<String> recipientsNormalized = Sets.newHashSet("4740222222", "47407777777");

  private final Set<String> generatedRecipients = Sets.newHashSet();

  private Set<User> users = new HashSet<>();

  private User sender;

  private final String subject = "subject";

  private final String text = "text message";

  private final String footer = "footer";

  private final Integer maxSmsLength = 10000;

  @BeforeEach
  public void initTest() {

    setUp();

    ArrayList<SmsGateway> smsGateways = new ArrayList<>();

    smsGateways.add(bulkSmsGateway);

    smsMessageSender =
        new SmsMessageSender(
            gatewayAdministrationService,
            smsGateways,
            userSettingService,
            outboundSmsService,
            systemSettingManager);
  }

  private void mockGateway() {
    // stub for SmsGateways
    when(bulkSmsGateway.accept(any())).thenReturn(true);
    Mockito.lenient()
        .when(
            bulkSmsGateway.send(
                anyString(), anyString(), anySet(), isA(BulkSmsGatewayConfig.class)))
        .thenReturn(okStatus);
    Mockito.lenient()
        .when(bulkSmsGateway.sendBatch(any(), any(BulkSmsGatewayConfig.class)))
        .thenReturn(summaryResponses);
  }

  @Test
  void testSendMessageWithGatewayConfig() {

    when(systemSettingManager.getSystemSetting(SettingKey.SMS_MAX_LENGTH, Integer.class))
        .thenReturn(maxSmsLength);

    // stub for GateAdministrationService
    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(smsGatewayConfig);
    mockGateway();
    OutboundMessageResponse status =
        smsMessageSender.sendMessage(subject, text, recipientsNormalized);

    assertNotNull(status);
    assertTrue(status.isOk());
    assertEquals("success", status.getDescription());

    verify(gatewayAdministrationService, times(1)).getDefaultGateway();
    verify(bulkSmsGateway, times(1)).accept(any());
    verify(bulkSmsGateway, times(1)).send(anyString(), anyString(), anySet(), any());
  }

  @Test
  void testSendMessageWithOutGatewayConfig() {
    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(null);

    OutboundMessageResponse status =
        smsMessageSender.sendMessage(subject, text, recipientsNormalized);

    assertNotNull(status);
    assertFalse(status.isOk());
    assertEquals(GatewayResponse.NO_GATEWAY_CONFIGURATION, status.getResponseObject());
    assertEquals(NO_CONFIG, status.getDescription());

    verify(gatewayAdministrationService, times(1)).getDefaultGateway();
    verify(bulkSmsGateway, never()).accept(smsGatewayConfig);
  }

  @Test
  void testIsConfiguredWithOutGatewayConfig() {
    when(gatewayAdministrationService.hasGateways()).thenReturn(false);

    boolean isConfigured = smsMessageSender.isConfigured();

    assertFalse(isConfigured);
  }

  @Test
  void testIsConfiguredWithGatewayConfig() {
    when(gatewayAdministrationService.hasGateways()).thenReturn(true);

    boolean isConfigured = smsMessageSender.isConfigured();

    assertTrue(isConfigured);
  }

  @Test
  void testSendMessageWithListOfUsers() {
    when(systemSettingManager.getSystemSetting(SettingKey.SMS_MAX_LENGTH, Integer.class))
        .thenReturn(maxSmsLength);

    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(smsGatewayConfig);
    when(userSettingService.getUserSetting(any(), any())).thenReturn(Boolean.TRUE);
    when(bulkSmsGateway.send(anyString(), anyString(), anySet(), isA(BulkSmsGatewayConfig.class)))
        .thenReturn(okStatus);
    when(bulkSmsGateway.accept(any())).thenReturn(true);

    OutboundMessageResponse status =
        smsMessageSender.sendMessage(subject, text, footer, sender, users, false);

    assertTrue(status.isOk());
    assertEquals(GatewayResponse.RESULT_CODE_0, status.getResponseObject());
    assertEquals("success", status.getDescription());
  }

  @Test
  void testSendMessageWithEmptyUserList() {
    OutboundMessageResponse status =
        smsMessageSender.sendMessage(subject, text, footer, sender, new HashSet<>(), false);

    assertFalse(status.isOk());
    assertEquals(GatewayResponse.NO_RECIPIENT, status.getResponseObject());
    assertEquals("no recipient", status.getDescription());
  }

  @Test
  void testSendMessageWithUserSMSSettingsDisabled() {
    when(userSettingService.getUserSetting(any(), any())).thenReturn(Boolean.FALSE);

    OutboundMessageResponse status =
        smsMessageSender.sendMessage(subject, text, footer, sender, users, false);

    assertFalse(status.isOk());
    assertEquals(GatewayResponse.SMS_DISABLED, status.getResponseObject());
    assertEquals("sms notifications are disabled", status.getDescription());
  }

  @Test
  void testSendMessageWithSingleRecipient() {
    when(systemSettingManager.getSystemSetting(SettingKey.SMS_MAX_LENGTH, Integer.class))
        .thenReturn(maxSmsLength);

    when(bulkSmsGateway.accept(any())).thenReturn(true);
    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(smsGatewayConfig);
    when(bulkSmsGateway.send(anyString(), anyString(), anySet(), isA(BulkSmsGatewayConfig.class)))
        .thenReturn(okStatus);
    OutboundMessageResponse status = smsMessageSender.sendMessage(subject, text, "47401111111");

    assertTrue(status.isOk());
    assertEquals(GatewayResponse.RESULT_CODE_0, status.getResponseObject());
    assertEquals("success", status.getDescription());
  }

  @Test
  void testSendMessageFailed() {

    when(systemSettingManager.getSystemSetting(SettingKey.SMS_MAX_LENGTH, Integer.class))
        .thenReturn(maxSmsLength);

    // stub for GateAdministrationService
    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(smsGatewayConfig);
    mockGateway();

    when(bulkSmsGateway.send(anyString(), anyString(), anySet(), isA(BulkSmsGatewayConfig.class)))
        .thenReturn(failedStatus);

    OutboundMessageResponse status =
        smsMessageSender.sendMessage(subject, text, recipientsNormalized);

    assertNotNull(status);
    assertEquals(GatewayResponse.FAILED, status.getResponseObject());
    assertFalse(status.isOk());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testNumberNormalization() {

    when(systemSettingManager.getSystemSetting(SettingKey.SMS_MAX_LENGTH, Integer.class))
        .thenReturn(maxSmsLength);

    // stub for GateAdministrationService
    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(smsGatewayConfig);
    mockGateway();

    Set<String> tempRecipients = Sets.newHashSet();

    when(bulkSmsGateway.send(anyString(), anyString(), anySet(), any(BulkSmsGatewayConfig.class)))
        .thenAnswer(
            invocation -> {
              tempRecipients.addAll((Set<String>) invocation.getArguments()[2]);
              return okStatus;
            });

    OutboundMessageResponse status =
        smsMessageSender.sendMessage(subject, text, recipientsNonNormalized);

    assertTrue(status.isOk());
    assertEquals(GatewayResponse.RESULT_CODE_0, status.getResponseObject());
    assertEquals("success", status.getDescription());

    Sets.SetView<String> setDifference = Sets.difference(tempRecipients, recipientsNormalized);

    assertEquals(0, setDifference.size());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testSendMessageWithMaxRecipients() {
    when(systemSettingManager.getSystemSetting(SettingKey.SMS_MAX_LENGTH, Integer.class))
        .thenReturn(maxSmsLength);

    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(smsGatewayConfig);
    mockGateway();
    List<Set<String>> recipientList = new ArrayList<>();

    generateRecipients(500);

    when(bulkSmsGateway.send(anyString(), anyString(), anySet(), any(BulkSmsGatewayConfig.class)))
        .then(
            invocation -> {
              recipientList.add((Set<String>) invocation.getArguments()[2]);

              return okStatus;
            });

    OutboundMessageResponse status =
        smsMessageSender.sendMessage(subject, text, generatedRecipients);

    assertNotNull(status);
    assertTrue(status.isOk());

    recipientList.forEach(set -> assertTrue(set.size() <= MAX_ALLOWED_RECIPIENTS));
  }

  @Test
  void testSendMessageWithSmsLengthGreaterThanDefaultMaxSmsLength() {

    when(systemSettingManager.getSystemSetting(SettingKey.SMS_MAX_LENGTH, Integer.class))
        .thenReturn(maxSmsLength);

    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(smsGatewayConfig);
    mockGateway();

    OutboundMessageResponse status =
        smsMessageSender.sendMessage(
            subject, "x".repeat(Math.max(0, maxSmsLength + 1)), recipientsNormalized);

    assertNull(smsGatewayConfig.getMaxSmsLength());
    assertFalse(status.isOk());
    assertEquals(GatewayResponse.SMS_TEXT_MESSAGE_TOO_LONG, status.getResponseObject());
  }

  @Test
  void testSendMessageWithSmsLengthGreaterThanGatewayMaxSmsLength() {
    smsGatewayConfig.setMaxSmsLength(maxSmsLength.toString());
    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(smsGatewayConfig);
    mockGateway();

    OutboundMessageResponse status =
        smsMessageSender.sendMessage(
            subject, "x".repeat(Math.max(0, maxSmsLength + 1)), recipientsNormalized);

    assertFalse(status.isOk());
    assertEquals(GatewayResponse.SMS_TEXT_MESSAGE_TOO_LONG, status.getResponseObject());
    verify(systemSettingManager, times(0))
        .getSystemSetting(SettingKey.SMS_MAX_LENGTH, Integer.class);
  }

  @Test
  void testSendMessageBatchCompleted() {
    mockGateway();
    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(smsGatewayConfig);
    responseForCompletedBatch();

    OutboundMessageBatch batch = new OutboundMessageBatch(outboundMessages, DeliveryChannel.SMS);

    ArgumentCaptor<OutboundMessageBatch> argumentCaptor =
        ArgumentCaptor.forClass(OutboundMessageBatch.class);

    OutboundMessageResponseSummary summary = smsMessageSender.sendMessageBatch(batch);

    assertNotNull(summary);
    assertEquals(OutboundMessageBatchStatus.COMPLETED, summary.getBatchStatus());

    verify(bulkSmsGateway, times(1)).sendBatch(argumentCaptor.capture(), any());
    assertEquals(batch, argumentCaptor.getValue());

    assertEquals(4, argumentCaptor.getValue().size());

    assertEquals(4, summary.getSent());
    assertEquals(4, summary.getTotal());
    assertEquals(0, summary.getFailed());
    assertEquals(0, summary.getPending());
  }

  @Test
  void testSendMessageBatchFailed() {
    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(smsGatewayConfig);
    mockGateway();

    responseForFailedBatch();

    OutboundMessageBatch batch = new OutboundMessageBatch(outboundMessages, DeliveryChannel.SMS);

    ArgumentCaptor<OutboundMessageBatch> argumentCaptor =
        ArgumentCaptor.forClass(OutboundMessageBatch.class);

    OutboundMessageResponseSummary summary = smsMessageSender.sendMessageBatch(batch);

    assertNotNull(summary);
    assertEquals(OutboundMessageBatchStatus.FAILED, summary.getBatchStatus());

    verify(bulkSmsGateway, times(1)).sendBatch(argumentCaptor.capture(), any());
    assertEquals(batch, argumentCaptor.getValue());
    assertEquals(4, argumentCaptor.getValue().size());

    assertEquals(3, summary.getSent());
    assertEquals(4, summary.getTotal());
    assertEquals(1, summary.getFailed());
    assertEquals(0, summary.getPending());
  }

  @Test
  void testSendMessageBatchWithMaxRecipients() {
    // stub for GateAdministrationService
    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(smsGatewayConfig);
    mockGateway();

    summaryResponses.clear();

    when(bulkSmsGateway.sendBatch(any(), isA(BulkSmsGatewayConfig.class)))
        .then(
            invocation -> {
              OutboundMessageBatch batch = (OutboundMessageBatch) invocation.getArguments()[0];

              summaryResponses.addAll(
                  batch.getMessages().stream()
                      .map(
                          message ->
                              new OutboundMessageResponse(
                                  "success", GatewayResponse.RESULT_CODE_0, true))
                      .collect(Collectors.toList()));

              return summaryResponses;
            });

    createOutBoundMessagesWithMaxRecipients();

    ArgumentCaptor<OutboundMessageBatch> argumentCaptor =
        ArgumentCaptor.forClass(OutboundMessageBatch.class);

    OutboundMessageBatch batch = new OutboundMessageBatch(outboundMessages, DeliveryChannel.SMS);

    OutboundMessageResponseSummary summary = smsMessageSender.sendMessageBatch(batch);

    assertNotNull(summary);
    assertEquals(OutboundMessageBatchStatus.COMPLETED, summary.getBatchStatus());

    verify(bulkSmsGateway, times(1)).sendBatch(argumentCaptor.capture(), any());
    assertEquals(batch, argumentCaptor.getValue());

    assertEquals(6, argumentCaptor.getValue().size());
    assertEquals(6, summary.getSent());
    assertEquals(6, summary.getTotal());
    assertEquals(0, summary.getFailed());
    assertEquals(0, summary.getPending());
  }

  @Test
  void testSendMessageBatchWithOutMaxRecipients() {
    // stub for GateAdministrationService
    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(smsGatewayConfig);
    mockGateway();

    responseForCompletedBatch();
    createOutBoundMessagesWithOutMaxRecipients();

    ArgumentCaptor<OutboundMessageBatch> argumentCaptor =
        ArgumentCaptor.forClass(OutboundMessageBatch.class);

    OutboundMessageBatch batch = new OutboundMessageBatch(outboundMessages, DeliveryChannel.SMS);

    OutboundMessageResponseSummary summary = smsMessageSender.sendMessageBatch(batch);

    assertNotNull(summary);
    assertEquals(OutboundMessageBatchStatus.COMPLETED, summary.getBatchStatus());

    verify(bulkSmsGateway, times(1)).sendBatch(argumentCaptor.capture(), any());
    assertEquals(batch, argumentCaptor.getValue());

    assertEquals(4, argumentCaptor.getValue().size());
  }

  @Test
  void testSendMessageBatchWithOutGatewayConfiguration() {
    when(gatewayAdministrationService.getDefaultGateway()).thenReturn(null);

    responseForFailedBatch();

    OutboundMessageBatch batch = new OutboundMessageBatch(outboundMessages, DeliveryChannel.SMS);

    OutboundMessageResponseSummary summary = smsMessageSender.sendMessageBatch(batch);

    assertNotNull(summary);
    assertEquals(OutboundMessageBatchStatus.FAILED, summary.getBatchStatus());
    assertEquals(NO_CONFIG, summary.getErrorMessage());
  }

  @Test
  void testIfNoRecipient() {
    OutboundMessageResponse status = smsMessageSender.sendMessage(subject, text, StringUtils.EMPTY);

    assertNotNull(status);
    assertFalse(status.isOk());
    assertEquals(GatewayResponse.NO_RECIPIENT, status.getResponseObject());
  }

  @Test
  void testIfBatchIsNull() {
    OutboundMessageResponseSummary summary = smsMessageSender.sendMessageBatch(null);

    assertNotNull(summary);
    assertEquals(OutboundMessageBatchStatus.ABORTED, summary.getBatchStatus());
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private void setUp() {
    okStatus = new OutboundMessageResponse();
    okStatus.setResponseObject(GatewayResponse.RESULT_CODE_0);

    failedStatus = new OutboundMessageResponse();
    failedStatus.setResponseObject(GatewayResponse.FAILED);

    smsGatewayConfig = new BulkSmsGatewayConfig();
    smsGatewayConfig.setUrlTemplate("");
    String gateway = "bulksms";
    smsGatewayConfig.setName(gateway);
    smsGatewayConfig.setUsername(" ");
    smsGatewayConfig.setPassword("");
    smsGatewayConfig.setUrlTemplate("");
    smsGatewayConfig.setDefault(true);

    OutboundMessage outboundMessageA = new OutboundMessage(subject, text, recipientsNormalized);
    OutboundMessage outboundMessageB = new OutboundMessage(subject, text, recipientsNonNormalized);
    OutboundMessage outboundMessageC = new OutboundMessage(subject, text, recipientsNormalized);
    OutboundMessage outboundMessageD = new OutboundMessage(subject, text, recipientsNonNormalized);

    outboundMessages =
        Arrays.asList(outboundMessageA, outboundMessageB, outboundMessageC, outboundMessageD);

    User userA = new User();
    userA.setPhoneNumber("47401111111");

    User userB = new User();
    userB.setPhoneNumber("47402222222");

    User userC = new User();
    userC.setPhoneNumber("47403333333");

    User userD = new User();
    userD.setPhoneNumber("47404444444");

    users = Sets.newHashSet(userA, userB, userC, userD);
    sender = new User();
    sender.setPhoneNumber("4740555555");
  }

  private void responseForFailedBatch() {
    summaryResponses.clear();
    summaryResponses.add(
        new OutboundMessageResponse(
            GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0,
            true));
    summaryResponses.add(
        new OutboundMessageResponse(
            GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0,
            true));
    summaryResponses.add(
        new OutboundMessageResponse(
            GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0,
            true));
    summaryResponses.add(
        new OutboundMessageResponse(
            GatewayResponse.FAILED.getResponseMessage(), GatewayResponse.FAILED, false));
  }

  private void responseForCompletedBatch() {
    summaryResponses.clear();
    summaryResponses.add(
        new OutboundMessageResponse(
            GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0,
            true));
    summaryResponses.add(
        new OutboundMessageResponse(
            GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0,
            true));
    summaryResponses.add(
        new OutboundMessageResponse(
            GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0,
            true));
    summaryResponses.add(
        new OutboundMessageResponse(
            GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0,
            true));
  }

  private void createOutBoundMessagesWithMaxRecipients() {
    generateRecipients(500);

    OutboundMessage outboundMessageA = new OutboundMessage(subject, text, generatedRecipients);
    OutboundMessage outboundMessageB = new OutboundMessage(subject, text, recipientsNonNormalized);
    OutboundMessage outboundMessageC = new OutboundMessage(subject, text, recipientsNormalized);
    OutboundMessage outboundMessageD = new OutboundMessage(subject, text, recipientsNonNormalized);

    outboundMessages =
        Arrays.asList(outboundMessageA, outboundMessageB, outboundMessageC, outboundMessageD);
  }

  private void createOutBoundMessagesWithOutMaxRecipients() {
    OutboundMessage outboundMessageA = new OutboundMessage(subject, text, recipientsNormalized);
    OutboundMessage outboundMessageB = new OutboundMessage(subject, text, recipientsNonNormalized);
    OutboundMessage outboundMessageC = new OutboundMessage(subject, text, recipientsNormalized);
    OutboundMessage outboundMessageD = new OutboundMessage(subject, text, recipientsNonNormalized);

    outboundMessages =
        Arrays.asList(outboundMessageA, outboundMessageB, outboundMessageC, outboundMessageD);
  }

  private void generateRecipients(int size) {
    generatedRecipients.clear();

    for (int i = 0; i < size; i++) {
      String temp = RandomStringUtils.random(10, false, true);

      if (generatedRecipients.contains(temp)) {
        i--;
        continue;
      }

      generatedRecipients.add(temp);
    }
  }
}
