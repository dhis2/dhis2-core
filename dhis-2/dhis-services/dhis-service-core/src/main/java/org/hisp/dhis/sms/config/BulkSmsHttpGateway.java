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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.sms.outbound.BulkSmsRequestEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Component("org.hisp.dhis.sms.config.BulkSmsGateway")
public class BulkSmsHttpGateway extends SmsGateway {
  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  public boolean accept(SmsGatewayConfig gatewayConfig) {
    return gatewayConfig instanceof BulkSmsGatewayConfig;
  }

  @Override
  public List<OutboundMessageResponse> sendBatch(
      OutboundMessageBatch smsBatch, SmsGatewayConfig config) {
    return smsBatch.getMessages().stream()
        .map(m -> send(m.getSubject(), m.getText(), m.getRecipients(), config))
        .collect(Collectors.toList());
  }

  @Override
  public OutboundMessageResponse send(
      String subject, String text, Set<String> recipients, SmsGatewayConfig config) {
    BulkSmsGatewayConfig bulkSmsGatewayConfig = (BulkSmsGatewayConfig) config;

    HttpEntity<BulkSmsRequestEntity> request =
        new HttpEntity<>(
            new BulkSmsRequestEntity(text, recipients),
            getAuthenticationHeaderParameters(bulkSmsGatewayConfig));

    HttpStatus httpStatus =
        send(bulkSmsGatewayConfig.getUrlTemplate(), request, HttpMethod.POST, String.class);

    return wrapHttpStatus(httpStatus);
  }
}
