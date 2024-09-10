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
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InboundSmsProcessingJob implements Job {
  private final IncomingSmsService incomingSmsService;

  private final List<IncomingSmsListener> listeners;

  private final MessageSender smsMessageSender;

  @Override
  public JobType getJobType() {
    return JobType.SMS_INBOUND_PROCESSING;
  }

  @Override
  public void execute(JobConfiguration config, JobProgress progress) {
    SmsInboundProcessingJobParameters params =
        (SmsInboundProcessingJobParameters) config.getJobParameters();
    progress.startingProcess("Process incoming SMS with UID {}", params.getSms());

    IncomingSms sms = incomingSmsService.get(params.getSms());
    if (sms == null) {
      progress.failedProcess("No incoming SMS found for UID {}", params.getSms());
      return;
    }

    try {
      for (IncomingSmsListener listener : listeners) {
        if (listener.accept(sms)) {
          listener.receive(sms);
          progress.completedProcess("Processed SMS with UID {}", params.getSms());
          return;
        }
      }

      sms.setStatus(SmsMessageStatus.UNHANDLED);
      incomingSmsService.update(sms);
      smsSender.sendMessage(null, "No command found", sms.getOriginator());

      progress.failedProcess("No command found for SMS with UID {}", params.getSms());
    } catch (Exception e) {
      sms.setStatus(SmsMessageStatus.FAILED);
      sms.setParsed(false);
      incomingSmsService.update(sms);

      progress.failedProcess(e);
    }
  }
}
