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
package org.hisp.dhis.sms.job;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.SmsInboundProcessingJobParameters;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InboundSmsProcessingJob implements Job {
  private final IncomingSmsService incomingSmsService;

  private final List<IncomingSmsListener> listeners;

  @Qualifier("smsMessageSender")
  private final MessageSender smsSender;

  @Override
  public JobType getJobType() {
    return JobType.SMS_INBOUND_PROCESSING;
  }

  @Override
  public void execute(JobConfiguration config, JobProgress progress) {
    progress.startingProcess("Inbound SMS processing");
    SmsInboundProcessingJobParameters params =
        (SmsInboundProcessingJobParameters) config.getJobParameters();

    // TODO(ivo) should we check the sms has not been processed?
    // TODO(ivo) handle null/fail job?
    IncomingSms sms = incomingSmsService.get(params.getSmsId());
    try {
      for (IncomingSmsListener listener : listeners) {
        if (listener.accept(sms)) {
          listener.receive(sms);
          // TODO(ivo) this expects the listener to update the sms status; write down to move this
          // into one place like here
          progress.completedProcess(null);
          return;
        }
      }

      // TODO(ivo) log via job progress?
      //      log.warn("No SMS command found in received data");
      sms.setStatus(SmsMessageStatus.UNHANDLED);
      smsSender.sendMessage(null, "No command found", sms.getOriginator());
    } catch (Exception e) {
      // TODO(ivo) log via job progress?
      //      e.printStackTrace();
      sms.setStatus(SmsMessageStatus.FAILED);
      sms.setParsed(false);
    } finally {
      incomingSmsService.update(sms);
    }

    progress.completedProcess(null);
  }
}
