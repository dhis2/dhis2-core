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

import static org.hisp.dhis.tracker.imports.sms.SmsImportMapper.mapCommand;

import java.util.Map;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.listener.CommandSMSListener;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Zubair <rajazubair.asghar@gmail.com> */
@Slf4j
@Component("org.hisp.dhis.tracker.sms.SingleEventListener")
@Transactional
public class SingleEventListener extends CommandSMSListener {
  private final SMSCommandService smsCommandService;

  private final TrackerImportService trackerImportService;

  private final CategoryService dataElementCategoryService;

  public SingleEventListener(
      UserService userService,
      IncomingSmsService incomingSmsService,
      MessageSender smsMessageSender,
      SMSCommandService smsCommandService,
      TrackerImportService trackerImportService,
      CategoryService dataElementCategoryService) {
    super(userService, incomingSmsService, smsMessageSender);
    this.smsCommandService = smsCommandService;
    this.trackerImportService = trackerImportService;
    this.dataElementCategoryService = dataElementCategoryService;
  }

  @Override
  protected SMSCommand getSMSCommand(@Nonnull IncomingSms sms) {
    return smsCommandService.getSMSCommand(
        SmsUtils.getCommandString(sms), ParserType.EVENT_REGISTRATION_PARSER);
  }

  @Override
  protected void postProcess(
      @Nonnull IncomingSms sms,
      @Nonnull UserDetails smsCreatedBy,
      @Nonnull SMSCommand smsCommand,
      @Nonnull Map<String, String> dataValues) {
    TrackerImportParams params =
        TrackerImportParams.builder().importStrategy(TrackerImportStrategy.CREATE).build();
    TrackerObjects trackerObjects =
        mapCommand(
            sms,
            smsCommand,
            dataValues,
            smsCreatedBy.getUserOrgUnitIds().iterator().next(),
            smsCreatedBy.getUsername(),
            dataElementCategoryService);
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    if (Status.OK == importReport.getStatus()) {
      update(sms, SmsMessageStatus.PROCESSED, true);
      sendFeedback(smsCommand.getSuccessMessage(), sms.getOriginator(), INFO);
      return;
    }

    // TODO(DHIS2-18003) we need to map tracker import report errors/warnings to an sms
    log.error(
        "Failed to process SMS command {} of parser type EVENT_REGISTRATION_PARSER {}",
        smsCommand.getName(),
        importReport);
    throw new IllegalStateException(importReport.toString());
  }
}
