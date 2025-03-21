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
package org.hisp.dhis.sms.config;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar.
 */
@Component("org.hisp.dhis.sms.config.SMPPGateway")
public class SMPPGateway extends SmsGateway {
  private final SMPPClient smppClient;

  public SMPPGateway(SMPPClient smppClient) {
    this.smppClient = smppClient;
  }

  @Override
  public boolean accept(SmsGatewayConfig gatewayConfig) {
    return gatewayConfig instanceof SMPPGatewayConfig;
  }

  @Override
  public OutboundMessageResponse send(
      String subject, String text, Set<String> recipients, SmsGatewayConfig gatewayConfig) {
    SMPPGatewayConfig config = (SMPPGatewayConfig) gatewayConfig;

    return smppClient.send(text, recipients, config);
  }

  @Override
  public List<OutboundMessageResponse> sendBatch(
      OutboundMessageBatch batch, SmsGatewayConfig gatewayConfig) {
    SMPPGatewayConfig config = (SMPPGatewayConfig) gatewayConfig;

    return smppClient.sendBatch(batch, config);
  }
}
