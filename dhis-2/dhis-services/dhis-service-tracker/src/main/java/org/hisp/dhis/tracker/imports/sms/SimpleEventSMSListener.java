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
package org.hisp.dhis.tracker.imports.sms;

import static org.hisp.dhis.tracker.imports.sms.SmsImportMapper.map;

import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.listener.CompressionSMSListener;
import org.hisp.dhis.sms.listener.SMSProcessingException;
import org.hisp.dhis.smscompression.SmsConsts.SubmissionType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.SimpleEventSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsSubmission;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.tracker.sms.SimpleEventSMSListener")
@Transactional
public class SimpleEventSMSListener extends CompressionSMSListener {
  private final TrackerImportService trackerImportService;

  public SimpleEventSMSListener(
      IncomingSmsService incomingSmsService,
      MessageSender smsMessageSender,
      UserService userService,
      TrackedEntityTypeService trackedEntityTypeService,
      TrackedEntityAttributeService trackedEntityAttributeService,
      ProgramService programService,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      DataElementService dataElementService,
      IdentifiableObjectManager identifiableObjectManager,
      TrackerImportService trackerImportService) {
    super(
        incomingSmsService,
        smsMessageSender,
        userService,
        trackedEntityTypeService,
        trackedEntityAttributeService,
        programService,
        organisationUnitService,
        categoryService,
        dataElementService,
        identifiableObjectManager);
    this.trackerImportService = trackerImportService;
  }

  @Override
  protected SmsResponse postProcess(IncomingSms sms, SmsSubmission submission, User user)
      throws SMSProcessingException {
    SimpleEventSmsSubmission subm = (SimpleEventSmsSubmission) submission;

    TrackerImportParams params =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
            .build();
    TrackerObjects trackerObjects = map(subm, user);
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    if (Status.OK == importReport.getStatus()) {
      return SmsResponse.SUCCESS;
    }
    // TODO(DHIS2-18003) we need to map tracker import report errors/warnings to an sms
    return SmsResponse.INVALID_EVENT.set(subm.getEvent());
  }

  @Override
  protected boolean handlesType(SubmissionType type) {
    return (type == SubmissionType.SIMPLE_EVENT);
  }
}
