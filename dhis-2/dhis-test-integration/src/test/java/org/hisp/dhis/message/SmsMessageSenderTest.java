/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.hisp.dhis.sms.config.BulkSmsGatewayConfig;
import org.hisp.dhis.sms.config.GatewayAdministrationService;
import org.hisp.dhis.sms.config.SmsGatewayConfig;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Zubair Asghar
 */
class SmsMessageSenderTest extends SingleSetupIntegrationTestBase {
  @Autowired
  @Qualifier("smsMessageSender")
  private MessageSender smsMessageSender;

  @Autowired private UserService _userService;

  @Autowired private OutboundSmsService outboundSmsService;
  @Autowired private GatewayAdministrationService gatewayAdministrationService;
  private User sender;
  private User userA;
  private User userB;
  private Set<User> users;

  @BeforeAll
  void setUp() {
    userService = _userService;
    sender = makeUser("S");
    userA = makeUser("A");
    userA.setPhoneNumber("40342434");
    userB = makeUser("B");
    userService.addUser(sender);
    userService.addUser(userA);
    userService.addUser(userB);
    users = Set.of(userA, userB);
    SmsGatewayConfig smsGatewayConfig = new BulkSmsGatewayConfig();
    smsGatewayConfig.setDefault(true);
    smsGatewayConfig.setUsername("user_uio");
    smsGatewayConfig.setPassword("RefLt4N5<1");
    gatewayAdministrationService.addGateway(smsGatewayConfig);
  }

  @Test
  void shouldSendMessageAsyncAndSaveOutboundSms() {
    smsMessageSender.sendMessageAsync("subject", "text_message", "footer", sender, users, true);
    // Wait until the outbound SMS is persisted
    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> !outboundSmsService.getAll().isEmpty());
    List<OutboundSms> outboundSms = outboundSmsService.getAll();
    assertAll(
        "Verify outbound SMS creation",
        () -> assertFalse(outboundSms.isEmpty(), "Outbound SMS list should not be empty"),
        () ->
            assertEquals(
                "text_message",
                outboundSms.get(0).getMessage(),
                "Message text should match the sent message"));
  }
}
