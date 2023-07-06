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
package org.hisp.dhis.sms.scheduling;

import static java.lang.String.format;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;

import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.sms.outbound.OutboundSmsStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component("sendScheduledMessageJob")
public class SendScheduledMessageJob implements Job {
  private final OutboundSmsService outboundSmsService;

  @Qualifier("smsMessageSender")
  private final MessageSender smsSender;

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  public JobType getJobType() {
    return JobType.SEND_SCHEDULED_MESSAGE;
  }

  @Override
  public void execute(JobConfiguration config, JobProgress progress) {
    progress.startingProcess("Starting to send messages in outbound");
    sendMessages(progress);
    progress.completedProcess("Sending messages in outbound completed");
  }

  @Override
  public ErrorReport validate() {
    if (!smsSender.isConfigured()) {
      return new ErrorReport(
          SendScheduledMessageJob.class,
          ErrorCode.E7010,
          "SMS gateway configuration does not exist");
    }
    return Job.super.validate();
  }

  private void sendMessages(JobProgress progress) {
    progress.startingStage("Finding outbound SMS messages");
    List<OutboundSms> outboundSmsList =
        progress.runStage(
            List.of(),
            list -> "found " + list.size() + " outbound SMS",
            () -> outboundSmsService.get(OutboundSmsStatus.OUTBOUND));

    if (outboundSmsList != null && !outboundSmsList.isEmpty()) {
      progress.startingStage("Sending SMS messages", outboundSmsList.size(), SKIP_ITEM_OUTLIER);
      progress.runStage(
          outboundSmsList,
          outboundSms ->
              format(
                  "Sending message `%1.20s...` to %d recipients",
                  outboundSms.getMessage(), outboundSms.getRecipients().size()),
          outboundSms -> {
            outboundSms.setDate(new Date());
            outboundSms.setStatus(OutboundSmsStatus.SENT);
            smsSender.sendMessage(null, outboundSms.getMessage(), outboundSms.getRecipients());
          });
    }
  }
}
