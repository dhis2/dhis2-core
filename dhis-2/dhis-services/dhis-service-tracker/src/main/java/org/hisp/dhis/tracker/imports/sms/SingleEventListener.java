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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.listener.SMSProcessingException;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Zubair <rajazubair.asghar@gmail.com> */
@Component("org.hisp.dhis.tracker.sms.SingleEventListener")
@Transactional
public class SingleEventListener extends RegisterSMSListener {
  private final SMSCommandService smsCommandService;

  public SingleEventListener(
      CategoryService dataElementCategoryService,
      UserService userService,
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      EnrollmentService enrollmentService,
      EventChangeLogService eventChangeLogService,
      FileResourceService fileResourceService,
      DhisConfigurationProvider config,
      IdentifiableObjectManager identifiableObjectManager,
      SMSCommandService smsCommandService) {
    super(
        dataElementCategoryService,
        userService,
        incomingSmsService,
        smsSender,
        enrollmentService,
        eventChangeLogService,
        fileResourceService,
        config,
        identifiableObjectManager);
    this.smsCommandService = smsCommandService;
  }

  @Override
  protected SMSCommand getSMSCommand(IncomingSms sms) {
    return smsCommandService.getSMSCommand(
        SmsUtils.getCommandString(sms), ParserType.EVENT_REGISTRATION_PARSER);
  }

  @Override
  protected void postProcess(
      IncomingSms sms, SMSCommand smsCommand, Map<String, String> parsedMessage) {
    Set<OrganisationUnit> ous = getOrganisationUnits(sms);

    registerEvent(parsedMessage, smsCommand, sms, ous);
  }

  private void registerEvent(
      Map<String, String> commandValuePairs,
      SMSCommand smsCommand,
      IncomingSms sms,
      Set<OrganisationUnit> ous) {
    List<Enrollment> enrollments;
    try {
      enrollments =
          new ArrayList<>(
              enrollmentService.getEnrollments(
                  null, smsCommand.getProgram(), EnrollmentStatus.ACTIVE));
    } catch (ForbiddenException | BadRequestException | NotFoundException e) {
      // TODO(tracker) Find a better error message for these exceptions
      throw new SMSProcessingException(SmsResponse.UNKNOWN_ERROR);
    }

    register(enrollments, commandValuePairs, smsCommand, sms, ous);
  }
}
