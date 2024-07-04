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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserService;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
public abstract class RegisterSMSListener extends CommandSMSListener {

  protected final EventService eventService;

  protected final EnrollmentService enrollmentService;

  protected RegisterSMSListener(
      CategoryService dataElementCategoryService,
      UserService userService,
      IncomingSmsService incomingSmsService,
      MessageSender smsSender,
      EventService eventService,
      EnrollmentService enrollmentService) {
    super(dataElementCategoryService, userService, incomingSmsService, smsSender);
    this.eventService = eventService;
    this.enrollmentService = enrollmentService;
  }

  protected void register(
      List<Enrollment> enrollments,
      Map<String, String> commandValuePairs,
      SMSCommand smsCommand,
      IncomingSms sms,
      Set<OrganisationUnit> ous) {
    if (enrollments.isEmpty()) {
      Enrollment enrollment = new Enrollment();
      enrollment.setEnrollmentDate(new Date());
      enrollment.setOccurredDate(new Date());
      enrollment.setProgram(smsCommand.getProgram());
      enrollment.setStatus(EnrollmentStatus.ACTIVE);

      enrollmentService.addEnrollment(enrollment);

      enrollments.add(enrollment);
    } else if (enrollments.size() > 1) {
      update(sms, SmsMessageStatus.FAILED, false);

      sendFeedback(
          "Multiple active Enrollments exists for program: " + smsCommand.getProgram().getUid(),
          sms.getOriginator(),
          ERROR);

      return;
    }

    Enrollment enrollment = enrollments.get(0);

    UserInfoSnapshot currentUserInfo =
        UserInfoSnapshot.from(CurrentUserUtil.getCurrentUserDetails());

    Event event = new Event();
    event.setOrganisationUnit(ous.iterator().next());
    event.setProgramStage(smsCommand.getProgramStage());
    event.setEnrollment(enrollment);
    event.setOccurredDate(sms.getSentDate());
    event.setScheduledDate(sms.getSentDate());
    event.setAttributeOptionCombo(dataElementCategoryService.getDefaultCategoryOptionCombo());
    event.setCompletedBy("DHIS 2");
    event.setStoredBy(currentUserInfo.getUsername());
    event.setCreatedByUserInfo(currentUserInfo);
    event.setLastUpdatedByUserInfo(currentUserInfo);

    Map<DataElement, EventDataValue> dataElementsAndEventDataValues = new HashMap<>();
    for (SMSCode smsCode : smsCommand.getCodes()) {
      EventDataValue eventDataValue =
          new EventDataValue(
              smsCode.getDataElement().getUid(),
              commandValuePairs.get(smsCode.getCode()),
              currentUserInfo);
      eventDataValue.setAutoFields();

      // Filter empty values out -> this is "adding/saving/creating",
      // therefore, empty values are ignored
      if (!StringUtils.isEmpty(eventDataValue.getValue())) {
        dataElementsAndEventDataValues.put(smsCode.getDataElement(), eventDataValue);
      }
    }

    eventService.saveEventDataValuesAndSaveEvent(event, dataElementsAndEventDataValues);

    update(sms, SmsMessageStatus.PROCESSED, true);

    sendFeedback(
        StringUtils.defaultIfEmpty(smsCommand.getSuccessMessage(), SMSCommand.SUCCESS_MESSAGE),
        sms.getOriginator(),
        INFO);
  }
}
