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

import static org.hisp.dhis.common.collection.CollectionUtils.emptyIfNull;
import static org.hisp.dhis.tracker.imports.sms.SmsImportMapper.mapCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.listener.CommandSMSListener;
import org.hisp.dhis.sms.listener.SMSProcessingException;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Created by zubair@dhis2.org on 11.08.17. */
@Slf4j
@Component("org.hisp.dhis.tracker.sms.ProgramStageDataEntrySMSListener")
@Transactional
public class ProgramStageDataEntrySMSListener extends CommandSMSListener {
  private static final String MORE_THAN_ONE_TE =
      "More than one tracked entity found for given phone number";

  private static final String NO_OU_FOUND = "No organisation unit found";

  private static final String NO_TE_EXIST = "No tracked entity exists with given phone number";

  private final TrackedEntityService trackedEntityService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final SMSCommandService smsCommandService;

  private final EnrollmentService enrollmentService;

  private final TrackerImportService trackerImportService;

  private final CategoryService dataElementCategoryService;

  public ProgramStageDataEntrySMSListener(
      UserService userService,
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      TrackedEntityService trackedEntityService,
      TrackedEntityAttributeService trackedEntityAttributeService,
      SMSCommandService smsCommandService,
      EnrollmentService enrollmentService,
      TrackerImportService trackerImportService,
      CategoryService dataElementCategoryService) {
    super(userService, incomingSmsService, smsSender);
    this.trackedEntityService = trackedEntityService;
    this.trackedEntityAttributeService = trackedEntityAttributeService;
    this.smsCommandService = smsCommandService;
    this.enrollmentService = enrollmentService;
    this.trackerImportService = trackerImportService;
    this.dataElementCategoryService = dataElementCategoryService;
  }

  @Override
  protected SMSCommand getSMSCommand(@Nonnull IncomingSms sms) {
    return smsCommandService.getSMSCommand(
        SmsUtils.getCommandString(sms), ParserType.PROGRAM_STAGE_DATAENTRY_PARSER);
  }

  @Override
  public void postProcess(
      @Nonnull IncomingSms sms,
      @Nonnull UserDetails smsCreatedBy,
      @Nonnull SMSCommand smsCommand,
      @Nonnull Map<String, String> dataValues) {
    List<TrackedEntity> trackedEntities = getTrackedEntityByPhoneNumber(sms, smsCommand);
    if (!validate(trackedEntities, smsCreatedBy.getUserOrgUnitIds(), sms)) {
      return;
    }
    TrackedEntity trackedEntity = trackedEntities.get(0);

    List<Enrollment> enrollments;
    try {
      Page<Enrollment> enrollmentPage =
          enrollmentService.findEnrollments(
              EnrollmentOperationParams.builder()
                  .trackedEntity(trackedEntity)
                  .program(smsCommand.getProgram())
                  .enrollmentStatus(EnrollmentStatus.ACTIVE)
                  .orgUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)
                  .build(),
              PageParams.of(1, 2, false));
      enrollments = emptyIfNull(enrollmentPage.getItems());
    } catch (BadRequestException | ForbiddenException e) {
      // TODO(tracker) Find a better error message for these exceptions
      throw new SMSProcessingException(SmsResponse.UNKNOWN_ERROR);
    }
    if (enrollments.size() > 1) {
      update(sms, SmsMessageStatus.FAILED, false);
      sendFeedback(
          "Multiple active Enrollments exists for program: " + smsCommand.getProgram().getUid(),
          sms.getOriginator(),
          ERROR);
      return;
    }

    UID enrollment = null;
    if (!enrollments.isEmpty()) {
      enrollment = UID.of(enrollments.get(0).getUid());
    }

    TrackerImportParams params =
        TrackerImportParams.builder().importStrategy(TrackerImportStrategy.CREATE).build();
    TrackerObjects trackerObjects =
        mapCommand(
            sms,
            smsCommand,
            dataValues,
            smsCreatedBy.getUserOrgUnitIds().iterator().next(),
            smsCreatedBy.getUsername(),
            dataElementCategoryService,
            trackedEntity.getUid(),
            enrollment);
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    if (Status.OK == importReport.getStatus()) {
      update(sms, SmsMessageStatus.PROCESSED, true);
      sendFeedback(smsCommand.getSuccessMessage(), sms.getOriginator(), INFO);
      return;
    }

    // TODO(DHIS2-18003) we need to map tracker import report errors/warnings to an sms
    log.error(
        "Failed to process SMS command {} of parser type PROGRAM_STAGE_DATAENTRY_PARSER {}",
        smsCommand.getName(),
        importReport);
    throw new IllegalStateException(importReport.toString());
  }

  private List<TrackedEntity> getTrackedEntityByPhoneNumber(IncomingSms sms, SMSCommand command) {
    List<TrackedEntityAttribute> attributes =
        trackedEntityAttributeService.getAllTrackedEntityAttributes().stream()
            .filter(attr -> attr.getValueType().equals(ValueType.PHONE_NUMBER))
            .toList();

    List<TrackedEntity> trackedEntities = new ArrayList<>();

    attributes.stream()
        .map(attr -> getParams(attr, sms, command.getProgram()))
        .forEach(
            param -> {
              try {
                Page<TrackedEntity> page =
                    trackedEntityService.findTrackedEntities(param, PageParams.of(1, 2, false));
                trackedEntities.addAll(page.getItems());
              } catch (BadRequestException | ForbiddenException | NotFoundException e) {
                // TODO(tracker) Find a better error message for these exceptions
                throw new SMSProcessingException(SmsResponse.UNKNOWN_ERROR);
              }
            });

    return trackedEntities;
  }

  private boolean hasMoreThanOneEntity(List<TrackedEntity> trackedEntities) {
    return trackedEntities.size() > 1;
  }

  private TrackedEntityOperationParams getParams(
      TrackedEntityAttribute attribute, IncomingSms sms, Program program) {

    QueryFilter queryFilter = new QueryFilter();
    queryFilter.setOperator(QueryOperator.EQ);
    queryFilter.setFilter(sms.getOriginator());

    return TrackedEntityOperationParams.builder()
        .filterBy(UID.of(attribute), List.of(queryFilter))
        .trackedEntityType(program.getTrackedEntityType())
        .orgUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)
        .build();
  }

  private boolean validate(
      List<TrackedEntity> trackedEntities, Set<String> orgUnits, IncomingSms sms) {
    if (trackedEntities == null || trackedEntities.isEmpty()) {
      sendFeedback(NO_TE_EXIST, sms.getOriginator(), ERROR);
      return false;
    }

    if (hasMoreThanOneEntity(trackedEntities)) {
      sendFeedback(MORE_THAN_ONE_TE, sms.getOriginator(), ERROR);
      return false;
    }

    if (validateOrganisationUnits(orgUnits)) {
      sendFeedback(NO_OU_FOUND, sms.getOriginator(), ERROR);
      return false;
    }

    return true;
  }

  private boolean validateOrganisationUnits(Set<String> orgUntis) {
    return orgUntis == null || orgUntis.isEmpty();
  }
}
