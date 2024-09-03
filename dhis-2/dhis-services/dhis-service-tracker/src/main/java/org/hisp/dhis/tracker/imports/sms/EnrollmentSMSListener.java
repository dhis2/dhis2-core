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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.listener.SMSProcessingException;
import org.hisp.dhis.smscompression.SmsConsts.SubmissionType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.EnrollmentSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsAttributeValue;
import org.hisp.dhis.smscompression.models.SmsEvent;
import org.hisp.dhis.smscompression.models.SmsSubmission;
import org.hisp.dhis.smscompression.models.Uid;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("org.hisp.dhis.tracker.sms.EnrollmentSMSListener")
@Transactional
public class EnrollmentSMSListener extends EventSavingSMSListener {
  private final TrackedEntityService trackedEntityService;

  private final EnrollmentService enrollmentService;

  private final TrackedEntityAttributeValueService attributeValueService;

  private final ProgramStageService programStageService;

  private final SMSEnrollmentService smsEnrollmentService;

  private final TrackedEntityTypeService trackedEntityTypeService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final ProgramService programService;

  private final OrganisationUnitService organisationUnitService;

  private final CategoryService categoryService;

  public EnrollmentSMSListener(
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      UserService userService,
      TrackedEntityTypeService trackedEntityTypeService,
      TrackedEntityAttributeService trackedEntityAttributeService,
      ProgramService programService,
      OrganisationUnitService organisationUnitService,
      ProgramStageService programStageService,
      EventService eventService,
      EventChangeLogService eventChangeLogService,
      FileResourceService fileResourceService,
      DhisConfigurationProvider config,
      TrackedEntityAttributeValueService attributeValueService,
      TrackedEntityService trackedEntityService,
      EnrollmentService enrollmentService,
      IdentifiableObjectManager manager,
      DataElementService dataElementService,
      SMSEnrollmentService smsEnrollmentService,
      CategoryService categoryService) {
    super(
        incomingSmsService,
        smsSender,
        userService,
        categoryService,
        dataElementService,
        manager,
        eventService,
        eventChangeLogService,
        fileResourceService,
        config);
    this.programService = programService;
    this.trackedEntityService = trackedEntityService;
    this.programStageService = programStageService;
    this.enrollmentService = enrollmentService;
    this.attributeValueService = attributeValueService;
    this.smsEnrollmentService = smsEnrollmentService;
    this.trackedEntityTypeService = trackedEntityTypeService;
    this.trackedEntityAttributeService = trackedEntityAttributeService;
    this.organisationUnitService = organisationUnitService;
    this.categoryService = categoryService;
  }

  @Override
  protected SmsResponse postProcess(IncomingSms sms, SmsSubmission submission, User user)
      throws SMSProcessingException {
    EnrollmentSmsSubmission subm = (EnrollmentSmsSubmission) submission;

    Date enrollmentDate = subm.getEnrollmentDate();
    Date occurredDate = subm.getIncidentDate();
    Uid teUid = subm.getTrackedEntityInstance();
    Uid progid = subm.getTrackerProgram();
    Uid tetid = subm.getTrackedEntityType();
    Uid ouid = subm.getOrgUnit();
    Uid enrollmentid = subm.getEnrollment();
    OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(ouid.getUid());

    Program program = programService.getProgram(progid.getUid());

    if (program == null) {
      throw new SMSProcessingException(SmsResponse.INVALID_PROGRAM.set(progid));
    }

    TrackedEntityType entityType = trackedEntityTypeService.getTrackedEntityType(tetid.getUid());

    if (entityType == null) {
      throw new SMSProcessingException(SmsResponse.INVALID_TETYPE.set(tetid));
    }

    if (!programService.hasOrgUnit(program, orgUnit)) {
      throw new SMSProcessingException(SmsResponse.OU_NOTIN_PROGRAM.set(ouid, progid));
    }

    TrackedEntity trackedEntity;
    boolean teExists = this.manager.exists(TrackedEntity.class, teUid.getUid());

    if (teExists) {
      log.info("Tracked entity exists: '{}'. Updating.", teUid);
      try {
        trackedEntity =
            trackedEntityService.getTrackedEntity(
                teUid.getUid(), null, TrackedEntityParams.FALSE, false);
      } catch (NotFoundException | ForbiddenException | BadRequestException e) {
        // TODO(tracker) Find a better error message for these exceptions
        throw new SMSProcessingException(SmsResponse.UNKNOWN_ERROR);
      }
    } else {
      log.info("Tracked entity does not exist: '{}'. Creating.", teUid);
      trackedEntity = new TrackedEntity();
      trackedEntity.setUid(teUid.getUid());
      trackedEntity.setOrganisationUnit(orgUnit);
      trackedEntity.setTrackedEntityType(entityType);
    }

    Set<TrackedEntityAttributeValue> attributeValues = getSMSAttributeValues(subm, trackedEntity);

    if (teExists) {
      updateAttributeValues(attributeValues, trackedEntity.getTrackedEntityAttributeValues());
      trackedEntity.setTrackedEntityAttributeValues(attributeValues);
      this.manager.update(trackedEntity);
    } else {
      manager.save(trackedEntity);

      for (TrackedEntityAttributeValue pav : attributeValues) {
        attributeValueService.addTrackedEntityAttributeValue(pav);
        trackedEntity.getTrackedEntityAttributeValues().add(pav);
      }

      manager.update(trackedEntity);
    }

    TrackedEntity te;
    try {
      te =
          trackedEntityService.getTrackedEntity(
              teUid.getUid(), null, TrackedEntityParams.FALSE, false);
    } catch (NotFoundException | ForbiddenException | BadRequestException e) {
      // TODO(tracker) Improve this error message
      throw new SMSProcessingException(SmsResponse.INVALID_TEI.set(trackedEntity.getUid()));
    }

    Enrollment enrollment = null;
    try {
      enrollment =
          enrollmentService.getEnrollment(enrollmentid.getUid(), UserDetails.fromUser(user));
      enrollment.setEnrollmentDate(enrollmentDate);
      enrollment.setOccurredDate(occurredDate);
    } catch (ForbiddenException e) {
      throw new SMSProcessingException(SmsResponse.INVALID_ENROLL.set(enrollmentid.getUid()));
    } catch (NotFoundException e) {
      // we'll create a new enrollment if none was found
      // TODO(tracker) a NFE might be thrown if the user has no metadata access, we shouldn't create
      // a new enrollment in that case
    }

    if (enrollment == null) {
      enrollment =
          smsEnrollmentService.enrollTrackedEntity(
              te, program, orgUnit, occurredDate, enrollmentid.getUid());

      if (enrollment == null) {
        throw new SMSProcessingException(SmsResponse.ENROLL_FAILED.set(teUid, progid));
      }
    }

    enrollment.setStatus(getCoreEnrollmentStatus(subm.getEnrollmentStatus()));
    enrollment.setGeometry(convertGeoPointToGeometry(subm.getCoordinates()));
    this.manager.update(enrollment);

    // We now check if the enrollment has events to process
    List<Object> errorUIDs = new ArrayList<>();
    if (subm.getEvents() != null) {
      for (SmsEvent event : subm.getEvents()) {
        errorUIDs.addAll(processEvent(event, user, enrollment));
      }
    }
    enrollment.setStatus(getCoreEnrollmentStatus(subm.getEnrollmentStatus()));
    enrollment.setGeometry(convertGeoPointToGeometry(subm.getCoordinates()));
    this.manager.update(enrollment);

    if (!errorUIDs.isEmpty()) {
      return SmsResponse.WARN_DVERR.setList(errorUIDs);
    }

    if (attributeValues.isEmpty()) {
      // TODO: Is this correct handling?
      return SmsResponse.WARN_AVEMPTY;
    }

    return SmsResponse.SUCCESS;
  }

  private TrackedEntityAttributeValue findAttributeValue(
      TrackedEntityAttributeValue attributeValue,
      Set<TrackedEntityAttributeValue> attributeValues) {
    return attributeValues.stream()
        .filter(v -> v.getAttribute().getUid().equals(attributeValue.getAttribute().getUid()))
        .findAny()
        .orElse(null);
  }

  private void updateAttributeValues(
      Set<TrackedEntityAttributeValue> attributeValues,
      Set<TrackedEntityAttributeValue> oldAttributeValues) {
    // Update existing and add new values
    for (TrackedEntityAttributeValue attributeValue : attributeValues) {
      TrackedEntityAttributeValue oldAttributeValue =
          findAttributeValue(attributeValue, oldAttributeValues);
      if (oldAttributeValue != null) {
        oldAttributeValue.setValue(attributeValue.getValue());
        attributeValueService.updateTrackedEntityAttributeValue(oldAttributeValue);
      } else {
        attributeValueService.addTrackedEntityAttributeValue(attributeValue);
      }
    }

    // Delete any that don't exist anymore
    for (TrackedEntityAttributeValue oldAttributeValue : oldAttributeValues) {
      if (findAttributeValue(oldAttributeValue, attributeValues) == null) {
        attributeValueService.deleteTrackedEntityAttributeValue(oldAttributeValue);
      }
    }
  }

  @Override
  protected boolean handlesType(SubmissionType type) {
    return (type == SubmissionType.ENROLLMENT);
  }

  private Set<TrackedEntityAttributeValue> getSMSAttributeValues(
      EnrollmentSmsSubmission submission, TrackedEntity trackedEntity) {
    if (submission.getValues() == null) {
      return Collections.emptySet();
    }
    return submission.getValues().stream()
        .map(v -> createTrackedEntityValue(v, trackedEntity))
        .collect(Collectors.toSet());
  }

  protected TrackedEntityAttributeValue createTrackedEntityValue(
      SmsAttributeValue SMSAttributeValue, TrackedEntity te) {
    Uid attribUid = SMSAttributeValue.getAttribute();
    String val = SMSAttributeValue.getValue();

    TrackedEntityAttribute attribute =
        trackedEntityAttributeService.getTrackedEntityAttribute(attribUid.getUid());

    if (attribute == null) {
      throw new SMSProcessingException(SmsResponse.INVALID_ATTRIB.set(attribUid));
    } else if (val == null) {
      // TODO: Is this an error we can't recover from?
      throw new SMSProcessingException(SmsResponse.NULL_ATTRIBVAL.set(attribUid));
    }
    TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
    trackedEntityAttributeValue.setAttribute(attribute);
    trackedEntityAttributeValue.setTrackedEntity(te);
    trackedEntityAttributeValue.setValue(val);
    return trackedEntityAttributeValue;
  }

  protected List<Object> processEvent(SmsEvent event, User user, Enrollment enrollment) {
    Uid stageid = event.getProgramStage();
    Uid aocid = event.getAttributeOptionCombo();
    Uid orgunitid = event.getOrgUnit();

    OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(orgunitid.getUid());
    if (orgUnit == null) {
      throw new SMSProcessingException(SmsResponse.INVALID_ORGUNIT.set(orgunitid));
    }

    ProgramStage programStage = programStageService.getProgramStage(stageid.getUid());
    if (programStage == null) {
      throw new SMSProcessingException(SmsResponse.INVALID_STAGE.set(stageid));
    }

    CategoryOptionCombo aoc = categoryService.getCategoryOptionCombo(aocid.getUid());
    if (aoc == null) {
      throw new SMSProcessingException(SmsResponse.INVALID_AOC.set(aocid));
    }

    return saveEvent(
        event.getEvent().getUid(),
        orgUnit,
        programStage,
        enrollment,
        aoc,
        user,
        event.getValues(),
        event.getEventStatus(),
        event.getEventDate(),
        event.getDueDate(),
        event.getCoordinates());
  }
}
