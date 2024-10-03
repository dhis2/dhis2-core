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

import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_TRACKER;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.listener.CommandSMSListener;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.tracker.export.event.TrackedEntityDataValueChangeLog;
import org.hisp.dhis.user.UserService;

@Slf4j
public abstract class RegisterSMSListener extends CommandSMSListener {

  protected final EnrollmentService enrollmentService;

  protected final EventChangeLogService eventChangeLogService;

  protected final FileResourceService fileResourceService;

  protected final DhisConfigurationProvider config;

  protected final IdentifiableObjectManager identifiableObjectManager;

  protected RegisterSMSListener(
      CategoryService dataElementCategoryService,
      UserService userService,
      IncomingSmsService incomingSmsService,
      MessageSender smsMessageSender,
      EnrollmentService enrollmentService,
      EventChangeLogService eventChangeLogService,
      FileResourceService fileResourceService,
      DhisConfigurationProvider config,
      IdentifiableObjectManager identifiableObjectManager) {
    super(dataElementCategoryService, userService, incomingSmsService, smsMessageSender);
    this.enrollmentService = enrollmentService;
    this.eventChangeLogService = eventChangeLogService;
    this.fileResourceService = fileResourceService;
    this.config = config;
    this.identifiableObjectManager = identifiableObjectManager;
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

      identifiableObjectManager.save(enrollment);

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

    UserInfoSnapshot currentUserInfo = UserInfoSnapshot.from(getCurrentUserDetails());

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

    saveEventDataValuesAndSaveEvent(event, dataElementsAndEventDataValues);

    update(sms, SmsMessageStatus.PROCESSED, true);

    sendFeedback(
        StringUtils.defaultIfEmpty(smsCommand.getSuccessMessage(), SMSCommand.SUCCESS_MESSAGE),
        sms.getOriginator(),
        INFO);
  }

  private void saveEventDataValuesAndSaveEvent(
      Event event, Map<DataElement, EventDataValue> dataElementEventDataValueMap) {
    validateEventDataValues(dataElementEventDataValueMap);
    Set<EventDataValue> eventDataValues = new HashSet<>(dataElementEventDataValueMap.values());
    event.setEventDataValues(eventDataValues);

    event.setAutoFields();
    if (!event.hasAttributeOptionCombo()) {
      CategoryOptionCombo aoc = dataElementCategoryService.getDefaultCategoryOptionCombo();
      event.setAttributeOptionCombo(aoc);
    }
    identifiableObjectManager.save(event);

    for (Map.Entry<DataElement, EventDataValue> entry : dataElementEventDataValueMap.entrySet()) {
      entry.getValue().setAutoFields();
      createAndAddAudit(entry.getValue(), entry.getKey(), event);
      handleFileDataValueSave(entry.getValue(), entry.getKey());
    }
  }

  private String validateEventDataValue(DataElement dataElement, EventDataValue eventDataValue) {

    if (StringUtils.isEmpty(eventDataValue.getStoredBy())) {
      return "Stored by is null or empty";
    }

    if (StringUtils.isEmpty(eventDataValue.getDataElement())) {
      return "Data element is null or empty";
    }

    if (!dataElement.getUid().equals(eventDataValue.getDataElement())) {
      throw new IllegalQueryException(
          "DataElement "
              + dataElement.getUid()
              + " assigned to EventDataValues does not match with one EventDataValue: "
              + eventDataValue.getDataElement());
    }

    String result =
        ValidationUtils.valueIsValid(eventDataValue.getValue(), dataElement.getValueType());

    return result == null ? null : "Value is not valid:  " + result;
  }

  private void validateEventDataValues(
      Map<DataElement, EventDataValue> dataElementEventDataValueMap) {
    String result;
    for (Map.Entry<DataElement, EventDataValue> entry : dataElementEventDataValueMap.entrySet()) {
      result = validateEventDataValue(entry.getKey(), entry.getValue());
      if (result != null) {
        throw new IllegalQueryException(result);
      }
    }
  }

  private void createAndAddAudit(EventDataValue dataValue, DataElement dataElement, Event event) {
    if (!config.isEnabled(CHANGELOG_TRACKER) || dataElement == null) {
      return;
    }

    TrackedEntityDataValueChangeLog dataValueAudit =
        new TrackedEntityDataValueChangeLog(
            dataElement,
            event,
            dataValue.getValue(),
            dataValue.getStoredBy(),
            dataValue.getProvidedElsewhere(),
            ChangeLogType.CREATE);

    eventChangeLogService.addTrackedEntityDataValueChangeLog(dataValueAudit);
  }

  /** Update FileResource with 'assigned' status. */
  private void handleFileDataValueSave(EventDataValue dataValue, DataElement dataElement) {
    if (dataElement == null) {
      return;
    }

    FileResource fileResource = fetchFileResource(dataValue, dataElement);

    if (fileResource == null) {
      return;
    }

    setAssigned(fileResource);
  }

  private FileResource fetchFileResource(EventDataValue dataValue, DataElement dataElement) {
    if (!dataElement.isFileType()) {
      return null;
    }

    return fileResourceService.getFileResource(dataValue.getValue());
  }

  private void setAssigned(FileResource fileResource) {
    fileResource.setAssigned(true);
    fileResourceService.updateFileResource(fileResource);
  }
}
