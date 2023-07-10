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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.sms.config.SMPPClient;
import org.hisp.dhis.sms.config.SMPPGatewayConfig;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * To run this test, make sure that the SMSC is running on: host: localhost port: 2775 user:
 * smppclient1 password: password
 */

/**
 * @Author Zubair Asghar.
 */
@Disabled("Test to run manually")
@ExtendWith(MockitoExtension.class)
class SMPPClientTest {
  private static final String SYSTEM_ID = "smppclient1";

  private static final String SYSTEM_TYPE = "cp";

  private static final String HOST = "localhost";

  private static final String PASSWORD = "password";

  private static final String RECIPIENT = "47XXXXXX";

  private static final String TEXT = "text through smpp";

  private static final int PORT = 2775;

  private SMPPClient subject;

  @BeforeEach
  public void init() {
    subject = new SMPPClient();
  }

  @Test
  void testSuccessMessage() {
    SMPPGatewayConfig config = getSMPPConfigurations();

    OutboundMessageResponse response = subject.send(TEXT, Sets.newHashSet(RECIPIENT), config);

    assertTrue(response.isOk());
    assertNotNull(response.getDescription());
    assertEquals(GatewayResponse.RESULT_CODE_0, response.getResponseObject());
  }

  @Test
  void testFailedMessage() {
    SMPPGatewayConfig config = getSMPPConfigurations();
    config.setPassword("123");

    OutboundMessageResponse response = subject.send(TEXT, Sets.newHashSet(RECIPIENT), config);

    assertFalse(response.isOk());
    assertEquals("SMPP Session cannot be null", response.getDescription());
    assertEquals(GatewayResponse.SMPP_SESSION_FAILURE, response.getResponseObject());
  }

  private SMPPGatewayConfig getSMPPConfigurations() {
    SMPPGatewayConfig config = new SMPPGatewayConfig();
    config.setUrlTemplate(HOST);
    config.setPort(PORT);
    config.setPassword(PASSWORD);
    config.setSystemType(SYSTEM_TYPE);
    config.setUsername(SYSTEM_ID);

    return config;
  }
}
