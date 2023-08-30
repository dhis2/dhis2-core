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
package org.hisp.dhis.sms.listener;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.smscompression.SmsResponse;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
public abstract class BaseSMSListener implements IncomingSmsListener {
  private static final String NO_SMS_CONFIG = "No sms configuration found";

  protected static final int INFO = 1;

  protected static final int WARNING = 2;

  protected static final int ERROR = 3;

  private static final Map<Integer, Consumer<String>> LOGGER =
      Map.of(
          1, log::info,
          2, log::warn,
          3, log::error);

  protected final IncomingSmsService incomingSmsService;

  protected final MessageSender smsSender;

  protected BaseSMSListener(IncomingSmsService incomingSmsService, MessageSender smsSender) {
    checkNotNull(incomingSmsService);
    checkNotNull(smsSender);

    this.incomingSmsService = incomingSmsService;
    this.smsSender = smsSender;
  }

  protected void sendFeedback(String message, String sender, int logType) {
    LOGGER.getOrDefault(logType, log::info).accept(message);

    if (smsSender.isConfigured()) {
      smsSender.sendMessage(null, message, sender);
      return;
    }

    LOGGER.getOrDefault(WARNING, log::info).accept(NO_SMS_CONFIG);
  }

  protected void sendSMSResponse(SmsResponse resp, IncomingSms sms, int messageID) {
    // A response code < 100 is either success or just a warning
    SmsMessageStatus status =
        resp.getCode() < 100 ? SmsMessageStatus.PROCESSED : SmsMessageStatus.FAILED;
    update(sms, status, true);

    if (smsSender.isConfigured()) {
      String msg = String.format("%d:%s", messageID, resp.toString());
      smsSender.sendMessage(null, msg, sms.getOriginator());
      return;
    }

    LOGGER.getOrDefault(WARNING, log::info).accept(NO_SMS_CONFIG);
  }

  protected void update(IncomingSms sms, SmsMessageStatus status, boolean parsed) {
    sms.setStatus(status);
    sms.setParsed(parsed);

    incomingSmsService.update(sms);
  }
}
