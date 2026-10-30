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

import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.outboundmessage.OutboundMessageResponseSummary;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar.
 */
@Slf4j
@Component("org.hisp.dhis.sms.config.SMSSendingCallback")
public class MessageSendingCallback {
  public BiConsumer<OutboundMessageResponse, Throwable> getCallBack() {
    return (result, ex) -> {
      if (ex != null) {
        log.error("Message sending failed", ex);
        return;
      }
      if (result != null && result.isOk()) {
        log.info("Message sending successful: " + result.getDescription());
      } else {
        log.error(
            "Message sending failed: "
                + (result != null ? result.getDescription() : "unknown error"));
      }
    };
  }

  public BiConsumer<OutboundMessageResponseSummary, Throwable> getBatchCallBack() {
    return (result, ex) -> {
      if (ex != null) {
        log.error("Message sending failed", ex);
        return;
      }
      if (result == null) {
        log.error("Message sending failed: unknown error");
        return;
      }
      int successful = result.getSent();
      int failed = result.getFailed();

      log.info(
          String.format(
              "%s Message sending status: Successful: %d Failed: %d",
              result.getChannel().name(), successful, failed));
    };
  }
}
