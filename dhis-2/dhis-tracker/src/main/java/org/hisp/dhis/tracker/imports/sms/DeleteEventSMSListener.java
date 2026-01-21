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
package org.hisp.dhis.tracker.imports.sms;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.listener.CompressionSMSListener;
import org.hisp.dhis.sms.listener.SMSProcessingException;
import org.hisp.dhis.smscompression.SmsConsts.SubmissionType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.DeleteSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsSubmission;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.user.UserDetails;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("org.hisp.dhis.tracker.sms.DeleteEventSMSListener")
@Transactional
public class DeleteEventSMSListener extends CompressionSMSListener {
  private final TrackerImportService trackerImportService;

  public DeleteEventSMSListener(
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      IdentifiableObjectManager identifiableObjectManager,
      TrackerImportService trackerImportService) {
    super(incomingSmsService, smsSender, identifiableObjectManager);
    this.trackerImportService = trackerImportService;
  }

  @Override
  protected SmsResponse postProcess(
      IncomingSms sms, SmsSubmission submission, UserDetails smsCreatedBy)
      throws SMSProcessingException {
    DeleteSmsSubmission subm = (DeleteSmsSubmission) submission;

    TrackerImportParams params =
        TrackerImportParams.builder().importStrategy(TrackerImportStrategy.DELETE).build();
    TrackerObjects trackerObjects = map(subm);
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    if (Status.OK == importReport.getStatus()) {
      return SmsResponse.SUCCESS;
    }

    // TODO(DHIS2-18003) we need to map tracker import report errors/warnings to an sms
    log.error("Failed to process SMS of submission type DELETE {}", importReport);
    return SmsResponse.INVALID_EVENT.set(subm.getEvent());
  }

  @Nonnull
  private static TrackerObjects map(@Nonnull DeleteSmsSubmission submission) {
    return TrackerObjects.builder()
        .events(
            List.of(TrackerEvent.builder().event(UID.of(submission.getEvent().getUid())).build()))
        .build();
  }

  @Override
  protected boolean handlesType(SubmissionType type) {
    return (type == SubmissionType.DELETE);
  }
}
