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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.sms.config.SMPPGateway;
import org.hisp.dhis.sms.config.SMPPGatewayConfig;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * To run this test, make sure that the SMSC is running on: host: localhost port: 2775 user:
 * smppclient1 password: password
 */
/**
 * @Author Zubair Asghar.
 */
@Disabled("Test to run manually")
class SMPPGatewayTest extends SingleSetupIntegrationTestBase {

  private static final String SYSTEM_ID = "smppclient1";

  private static final String SYSTEM_TYPE = "cp";

  private static final String HOST = "localhost";

  private static final String PASSWORD = "password";

  private static final String RECIPIENT = "47XXXXXX";

  private static final String TEXT = "text through smpp";

  private static final String SUBJECT = "subject";

  private static final int PORT = 2775;

  private SMPPGatewayConfig config;

  @Autowired private SMPPGateway gateway;

  @BeforeEach
  void init() {
    config = new SMPPGatewayConfig();
    config.setUrlTemplate(HOST);
    config.setPassword(PASSWORD);
    config.setPort(PORT);
    config.setSystemType(SYSTEM_TYPE);
    config.setUsername(SYSTEM_ID);
  }

  @Test
  void testSuccessfulMessage() {
    OutboundMessageResponse response;
    response = gateway.send(SUBJECT, TEXT, Sets.newHashSet(RECIPIENT), config);
    assertTrue(response.isOk());
    assertEquals(GatewayResponse.RESULT_CODE_0, response.getResponseObject());
  }

  @Test
  void testBulkMessage() {
    List<OutboundMessage> messages = new ArrayList<>();
    messages.add(new OutboundMessage(SUBJECT, TEXT, Sets.newHashSet(RECIPIENT)));
    messages.add(new OutboundMessage(SUBJECT, TEXT, Sets.newHashSet(RECIPIENT)));
    messages.add(new OutboundMessage(SUBJECT, TEXT, Sets.newHashSet(RECIPIENT)));
    messages.add(new OutboundMessage(SUBJECT, TEXT, Sets.newHashSet(RECIPIENT)));
    messages.add(new OutboundMessage(SUBJECT, TEXT, Sets.newHashSet(RECIPIENT)));
    messages.add(new OutboundMessage(SUBJECT, TEXT, Sets.newHashSet(RECIPIENT)));
    messages.add(new OutboundMessage(SUBJECT, TEXT, Sets.newHashSet(RECIPIENT)));
    messages.add(new OutboundMessage(SUBJECT, TEXT, Sets.newHashSet(RECIPIENT)));
    OutboundMessageBatch batch = new OutboundMessageBatch(messages, DeliveryChannel.SMS);
    List<OutboundMessageResponse> responses = gateway.sendBatch(batch, config);
    assertNotNull(responses);
    assertEquals(8, responses.size());
    responses.forEach(r -> assertTrue(r.isOk()));
  }
}
