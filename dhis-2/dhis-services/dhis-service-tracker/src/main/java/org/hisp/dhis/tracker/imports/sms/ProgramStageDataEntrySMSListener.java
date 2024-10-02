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
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.listener.SMSProcessingException;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Created by zubair@dhis2.org on 11.08.17. */
@Component("org.hisp.dhis.tracker.sms.ProgramStageDataEntrySMSListener")
@Transactional
public class ProgramStageDataEntrySMSListener extends RegisterSMSListener {
  private static final String MORE_THAN_ONE_TE =
      "More than one tracked entity found for given phone number";

  private static final String NO_OU_FOUND = "No organisation unit found";

  private static final String NO_TE_EXIST = "No tracked entity exists with given phone number";

  private final TrackedEntityService trackedEntityService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final SMSCommandService smsCommandService;

  public ProgramStageDataEntrySMSListener(
      CategoryService dataElementCategoryService,
      UserService userService,
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      EnrollmentService enrollmentService,
      EventChangeLogService eventChangeLogService,
      FileResourceService fileResourceService,
      DhisConfigurationProvider config,
      IdentifiableObjectManager identifiableObjectManager,
      TrackedEntityService trackedEntityService,
      TrackedEntityAttributeService trackedEntityAttributeService,
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
    this.trackedEntityService = trackedEntityService;
    this.trackedEntityAttributeService = trackedEntityAttributeService;
    this.smsCommandService = smsCommandService;
  }

  @Override
  public void postProcess(
      @Nonnull IncomingSms sms,
      @Nonnull String username,
      @Nonnull SMSCommand smsCommand,
      @Nonnull Map<String, String> codeValues) {
    Set<OrganisationUnit> ous = getOrganisationUnits(sms);

    List<TrackedEntity> trackedEntities = getTrackedEntityByPhoneNumber(sms, smsCommand, ous);

    if (!validate(trackedEntities, ous, sms)) {
      return;
    }

    registerProgramStage(trackedEntities.iterator().next(), sms, smsCommand, codeValues, ous);
  }

  @Override
  protected SMSCommand getSMSCommand(@Nonnull IncomingSms sms) {
    return smsCommandService.getSMSCommand(
        SmsUtils.getCommandString(sms), ParserType.PROGRAM_STAGE_DATAENTRY_PARSER);
  }

  private void registerProgramStage(
      TrackedEntity trackedEntity,
      IncomingSms sms,
      SMSCommand smsCommand,
      Map<String, String> keyValue,
      Set<OrganisationUnit> ous) {

    List<Enrollment> enrollments;
    try {
      enrollments =
          new ArrayList<>(
              enrollmentService.getEnrollments(
                  trackedEntity.getUid(), smsCommand.getProgram(), EnrollmentStatus.ACTIVE));
    } catch (BadRequestException | ForbiddenException | NotFoundException e) {
      // TODO(tracker) Find a better error message for these exceptions
      throw new SMSProcessingException(SmsResponse.UNKNOWN_ERROR);
    }

    register(enrollments, keyValue, smsCommand, sms, ous);
  }

  private List<TrackedEntity> getTrackedEntityByPhoneNumber(
      IncomingSms sms, SMSCommand command, Set<OrganisationUnit> ous) {
    List<TrackedEntityAttribute> attributes =
        trackedEntityAttributeService.getAllTrackedEntityAttributes().stream()
            .filter(attr -> attr.getValueType().equals(ValueType.PHONE_NUMBER))
            .toList();

    List<TrackedEntity> trackedEntities = new ArrayList<>();

    attributes.parallelStream()
        .map(attr -> getParams(attr, sms, command.getProgram(), ous))
        .forEach(
            param -> {
              try {
                trackedEntities.addAll(trackedEntityService.getTrackedEntities(param));
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
      TrackedEntityAttribute attribute,
      IncomingSms sms,
      Program program,
      Set<OrganisationUnit> ous) {

    QueryFilter queryFilter = new QueryFilter();
    queryFilter.setOperator(QueryOperator.LIKE);
    queryFilter.setFilter(sms.getOriginator());

    return TrackedEntityOperationParams.builder()
        .filters(Map.of(attribute.getUid(), List.of(queryFilter)))
        .programUid(program.getUid())
        .organisationUnits(
            ous.stream().map(BaseIdentifiableObject::getUid).collect(Collectors.toSet()))
        .build();
  }

  private boolean validate(
      List<TrackedEntity> trackedEntities, Set<OrganisationUnit> ous, IncomingSms sms) {
    if (trackedEntities == null || trackedEntities.isEmpty()) {
      sendFeedback(NO_TE_EXIST, sms.getOriginator(), ERROR);
      return false;
    }

    if (hasMoreThanOneEntity(trackedEntities)) {
      sendFeedback(MORE_THAN_ONE_TE, sms.getOriginator(), ERROR);
      return false;
    }

    if (validateOrganisationUnits(ous)) {
      sendFeedback(NO_OU_FOUND, sms.getOriginator(), ERROR);
      return false;
    }

    return true;
  }

  private boolean validateOrganisationUnits(Set<OrganisationUnit> ous) {
    return ous == null || ous.isEmpty();
  }
}
