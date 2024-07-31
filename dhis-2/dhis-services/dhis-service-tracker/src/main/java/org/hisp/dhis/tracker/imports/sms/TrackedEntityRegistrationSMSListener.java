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

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.notification.event.ProgramEnrollmentNotificationEvent;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.listener.CommandSMSListener;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.tracker.sms.TrackedEntityRegistrationSMSListener")
@Transactional
public class TrackedEntityRegistrationSMSListener extends CommandSMSListener {
  private static final String SUCCESS_MESSAGE = "Tracked Entity Registered Successfully with uid. ";

  private final SMSCommandService smsCommandService;

  private final TrackedEntityTypeService trackedEntityTypeService;

  private final TrackedEntityService trackedEntityService;

  private final ProgramService programService;

  private final IdentifiableObjectManager manager;

  private final TrackerOwnershipManager trackerOwnershipAccessManager;

  private final ApplicationEventPublisher eventPublisher;

  public TrackedEntityRegistrationSMSListener(
      ProgramService programService,
      CategoryService dataElementCategoryService,
      UserService userService,
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      SMSCommandService smsCommandService,
      TrackedEntityTypeService trackedEntityTypeService,
      TrackedEntityService trackedEntityService,
      IdentifiableObjectManager manager,
      TrackerOwnershipManager trackerOwnershipAccessManager,
      ApplicationEventPublisher eventPublisher) {
    super(dataElementCategoryService, userService, incomingSmsService, smsSender);
    this.smsCommandService = smsCommandService;
    this.trackedEntityTypeService = trackedEntityTypeService;
    this.trackedEntityService = trackedEntityService;
    this.programService = programService;
    this.manager = manager;
    this.trackerOwnershipAccessManager = trackerOwnershipAccessManager;
    this.eventPublisher = eventPublisher;
  }

  @Override
  protected void postProcess(
      IncomingSms sms, SMSCommand smsCommand, Map<String, String> parsedMessage) {
    String message = sms.getText();

    Date occurredDate = SmsUtils.lookForDate(message);
    String senderPhoneNumber = StringUtils.replace(sms.getOriginator(), "+", "");
    Collection<OrganisationUnit> orgUnits = getOrganisationUnits(sms);

    Program program = smsCommand.getProgram();

    OrganisationUnit orgUnit = SmsUtils.selectOrganisationUnit(orgUnits, parsedMessage, smsCommand);

    if (!programService.hasOrgUnit(program, orgUnit)) {
      sendFeedback(SMSCommand.NO_OU_FOR_PROGRAM, senderPhoneNumber, WARNING);

      throw new SMSParserException(SMSCommand.NO_OU_FOR_PROGRAM);
    }

    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setOrganisationUnit(orgUnit);
    trackedEntity.setTrackedEntityType(
        trackedEntityTypeService.getTrackedEntityByName(
            smsCommand.getProgram().getTrackedEntityType().getName()));
    Set<TrackedEntityAttributeValue> patientAttributeValues = new HashSet<>();

    smsCommand.getCodes().stream()
        .filter(code -> parsedMessage.containsKey(code.getCode()))
        .forEach(
            code -> {
              TrackedEntityAttributeValue trackedEntityAttributeValue =
                  this.createTrackedEntityAttributeValue(parsedMessage, code, trackedEntity);
              patientAttributeValues.add(trackedEntityAttributeValue);
            });

    if (!patientAttributeValues.isEmpty()) {
      trackedEntityService.createTrackedEntity(trackedEntity, patientAttributeValues);
    } else {
      sendFeedback("No TrackedEntityAttribute found", senderPhoneNumber, WARNING);
    }

    enrollTrackedEntity(
        trackedEntityService.getTrackedEntity(trackedEntity.getUid()),
        program,
        orgUnit,
        occurredDate);

    sendFeedback(
        StringUtils.defaultIfBlank(
            smsCommand.getSuccessMessage(), SUCCESS_MESSAGE + trackedEntity.getUid()),
        senderPhoneNumber,
        INFO);

    update(sms, SmsMessageStatus.PROCESSED, true);
  }

  @Override
  protected SMSCommand getSMSCommand(IncomingSms sms) {
    return smsCommandService.getSMSCommand(
        SmsUtils.getCommandString(sms), ParserType.TRACKED_ENTITY_REGISTRATION_PARSER);
  }

  private TrackedEntityAttributeValue createTrackedEntityAttributeValue(
      Map<String, String> parsedMessage, SMSCode code, TrackedEntity trackedEntity) {
    String value = parsedMessage.get(code.getCode());
    TrackedEntityAttribute trackedEntityAttribute = code.getTrackedEntityAttribute();

    TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
    trackedEntityAttributeValue.setAttribute(trackedEntityAttribute);
    trackedEntityAttributeValue.setTrackedEntity(trackedEntity);
    trackedEntityAttributeValue.setValue(value);
    return trackedEntityAttributeValue;
  }

  // TODO(tracker) we should use the importer here
  private void enrollTrackedEntity(
      TrackedEntity trackedEntity,
      Program program,
      OrganisationUnit organisationUnit,
      Date occurredDate) {
    Enrollment enrollment =
        prepareEnrollment(
            trackedEntity,
            program,
            new Date(),
            occurredDate,
            organisationUnit,
            CodeGenerator.generateUid());
    manager.save(enrollment);
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntity, program, organisationUnit, true, true);
    eventPublisher.publishEvent(new ProgramEnrollmentNotificationEvent(this, enrollment.getId()));
    manager.update(enrollment);
    trackedEntityService.updateTrackedEntity(trackedEntity);
  }

  private Enrollment prepareEnrollment(
      TrackedEntity trackedEntity,
      Program program,
      Date enrollmentDate,
      Date occurredDate,
      OrganisationUnit organisationUnit,
      String uid) {
    if (program.getTrackedEntityType() != null
        && !program.getTrackedEntityType().equals(trackedEntity.getTrackedEntityType())) {
      throw new IllegalQueryException(
          "Tracked entity must have same tracked entity as program: " + program.getUid());
    }

    Enrollment enrollment = new Enrollment();
    enrollment.setUid(CodeGenerator.isValidUid(uid) ? uid : CodeGenerator.generateUid());
    enrollment.setOrganisationUnit(organisationUnit);
    enrollment.enrollTrackedEntity(trackedEntity, program);

    if (enrollmentDate != null) {
      enrollment.setEnrollmentDate(enrollmentDate);
    } else {
      enrollment.setEnrollmentDate(new Date());
    }

    if (occurredDate != null) {
      enrollment.setOccurredDate(occurredDate);
    } else {
      enrollment.setOccurredDate(new Date());
    }

    enrollment.setStatus(EnrollmentStatus.ACTIVE);

    return enrollment;
  }
}
