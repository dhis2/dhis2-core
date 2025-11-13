/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.trackerevent;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateAttributeOperators;
import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateMinimumCharactersToSearch;
import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateOrgUnitMode;
import static org.hisp.dhis.util.ObjectUtils.applyIfNotNull;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.acl.TrackerProgramService;
import org.hisp.dhis.tracker.export.CategoryOptionComboService;
import org.hisp.dhis.tracker.export.OperationsParamsValidator;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps {@link TrackerEventOperationParams} to {@link TrackerEventQueryParams} which is used to
 * fetch events from the DB.
 */
@Component
@RequiredArgsConstructor
class TrackerEventOperationParamsMapper {

  private final ProgramStageService programStageService;

  private final OrganisationUnitService organisationUnitService;

  private final AclService aclService;

  private final CategoryOptionComboService categoryOptionComboService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final DataElementService dataElementService;

  private final OperationsParamsValidator paramsValidator;

  private final TrackerProgramService trackerProgramService;

  @Transactional(readOnly = true)
  public TrackerEventQueryParams map(
      @Nonnull TrackerEventOperationParams operationParams, @Nonnull UserDetails user)
      throws BadRequestException, ForbiddenException {
    Program program = paramsValidator.validateTrackerProgram(operationParams.getProgram(), user);
    ProgramStage programStage =
        validateProgramStage(
            applyIfNotNull(operationParams.getProgramStage(), UID::getValue), user);
    TrackedEntity trackedEntity =
        paramsValidator.validateTrackedEntity(
            operationParams.getTrackedEntity(), user, operationParams.isIncludeDeleted());
    OrganisationUnit orgUnit =
        validateRequestedOrgUnit(applyIfNotNull(operationParams.getOrgUnit(), UID::getValue), user);
    validateOrgUnitMode(operationParams.getOrgUnitMode(), program, user);

    CategoryOptionCombo attributeOptionCombo =
        categoryOptionComboService.getAttributeOptionCombo(
            operationParams.getAttributeCategoryCombo() != null
                ? operationParams.getAttributeCategoryCombo().getValue()
                : null,
            UID.toValueSet(operationParams.getAttributeCategoryOptions()),
            true);

    validateAttributeOptionCombo(attributeOptionCombo, user);

    TrackerEventQueryParams queryParams = new TrackerEventQueryParams();

    mapDataElementFilters(queryParams, operationParams.getDataElementFilters());
    mapAttributeFilters(queryParams, operationParams.getAttributeFilters());
    mapOrderParam(queryParams, operationParams.getOrder());

    List<Program> accessibleTrackerPrograms = getTrackerPrograms(program);

    return queryParams
        .setEnrolledInTrackerProgram(program)
        .setAccessibleTrackerPrograms(getTrackerPrograms(program))
        .setProgramStage(programStage)
        .setAccessibleTrackerProgramStages(
            getTrackerProgramStages(
                program != null ? List.of(program) : accessibleTrackerPrograms, programStage))
        .setOrgUnit(orgUnit)
        .setTrackedEntity(trackedEntity)
        .setEnrollmentStatus(operationParams.getEnrollmentStatus())
        .setFollowUp(operationParams.getFollowUp())
        .setOrgUnitMode(operationParams.getOrgUnitMode())
        .setAssignedUserQueryParam(
            new AssignedUserQueryParam(
                operationParams.getAssignedUserMode(),
                operationParams.getAssignedUsers(),
                UID.of(user)))
        .setOccurredStartDate(operationParams.getOccurredAfter())
        .setOccurredEndDate(operationParams.getOccurredBefore())
        .setScheduledStartDate(operationParams.getScheduledAfter())
        .setScheduledEndDate(operationParams.getScheduledBefore())
        .setUpdatedAtStartDate(operationParams.getUpdatedAfter())
        .setUpdatedAtEndDate(operationParams.getUpdatedBefore())
        .setUpdatedAtDuration(operationParams.getUpdatedWithin())
        .setEnrollmentEnrolledBefore(operationParams.getEnrollmentEnrolledBefore())
        .setEnrollmentEnrolledAfter(operationParams.getEnrollmentEnrolledAfter())
        .setEnrollmentOccurredBefore(operationParams.getEnrollmentOccurredBefore())
        .setEnrollmentOccurredAfter(operationParams.getEnrollmentOccurredAfter())
        .setEventStatus(operationParams.getEventStatus())
        .setCategoryOptionCombo(attributeOptionCombo)
        .setEvents(operationParams.getEvents())
        .setEnrollments(operationParams.getEnrollments())
        .setIncludeDeleted(operationParams.isIncludeDeleted())
        .setIdSchemeParams(operationParams.getIdSchemeParams());
  }

  private List<Program> getTrackerPrograms(Program program) {
    if (program == null) {
      return trackerProgramService.getAccessibleTrackerPrograms();
    }

    return emptyList();
  }

  private List<ProgramStage> getTrackerProgramStages(
      List<Program> programs, ProgramStage programStage) {
    if (programStage == null) {
      return trackerProgramService.getAccessibleTrackerProgramStages(programs);
    }

    return emptyList();
  }

  private ProgramStage validateProgramStage(String programStageUid, UserDetails user)
      throws BadRequestException, ForbiddenException {
    if (programStageUid == null) {
      return null;
    }

    ProgramStage programStage = programStageService.getProgramStage(programStageUid);
    if (programStage == null) {
      throw new BadRequestException(
          "Program stage is specified but does not exist: " + programStageUid);
    }

    if (!aclService.canDataRead(user, programStage)) {
      throw new ForbiddenException("User has no access to program stage: " + programStage.getUid());
    }

    return programStage;
  }

  private OrganisationUnit validateRequestedOrgUnit(String orgUnitUid, UserDetails user)
      throws BadRequestException, ForbiddenException {
    if (orgUnitUid == null) {
      return null;
    }
    OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(orgUnitUid);
    if (orgUnit == null) {
      throw new BadRequestException(
          "Organisation unit is specified but does not exist: " + orgUnitUid);
    }

    if (!user.isInUserEffectiveSearchOrgUnitHierarchy(orgUnit.getStoredPath())) {
      throw new ForbiddenException(
          "Organisation unit is not part of your search scope: " + orgUnit.getUid());
    }

    return orgUnit;
  }

  private void validateAttributeOptionCombo(
      CategoryOptionCombo attributeOptionCombo, UserDetails user) throws ForbiddenException {
    if (attributeOptionCombo != null
        && !user.isSuper()
        && !aclService.canDataRead(user, attributeOptionCombo)) {
      throw new ForbiddenException(
          "User has no access to attribute category option combo: "
              + attributeOptionCombo.getUid());
    }
  }

  private void mapDataElementFilters(
      TrackerEventQueryParams params, Map<UID, List<QueryFilter>> dataElementFilters)
      throws BadRequestException {
    for (Entry<UID, List<QueryFilter>> dataElementFilter : dataElementFilters.entrySet()) {
      DataElement de = dataElementService.getDataElement(dataElementFilter.getKey().getValue());
      if (de == null) {
        throw new BadRequestException(
            String.format(
                "filter is invalid. Data element '%s' does not exist.",
                dataElementFilter.getKey()));
      }

      if (dataElementFilter.getValue().isEmpty()) {
        params.filterBy(de);
      }

      for (QueryFilter filter : dataElementFilter.getValue()) {
        params.filterBy(de, filter);
      }
    }
  }

  private void mapAttributeFilters(
      TrackerEventQueryParams params, Map<UID, List<QueryFilter>> attributeFilters)
      throws BadRequestException {
    for (Entry<UID, List<QueryFilter>> attributeFilter : attributeFilters.entrySet()) {
      TrackedEntityAttribute tea =
          trackedEntityAttributeService.getTrackedEntityAttribute(
              attributeFilter.getKey().getValue());
      if (tea == null) {
        throw new BadRequestException(
            String.format(
                "attribute filters are invalid. Tracked entity attribute '%s' does not exist.",
                attributeFilter.getKey()));
      }

      validateAttributeOperators(attributeFilter, tea);

      validateMinimumCharactersToSearch(attributeFilter, tea);

      if (attributeFilter.getValue().isEmpty()) {
        params.filterBy(tea);
      }

      for (QueryFilter filter : attributeFilter.getValue()) {
        params.filterBy(tea, filter);
      }
    }
  }

  private void mapOrderParam(TrackerEventQueryParams params, List<Order> orders)
      throws BadRequestException {
    if (orders == null || orders.isEmpty()) {
      return;
    }

    for (Order order : orders) {
      if (order.getField() instanceof String field) {
        params.orderBy(field, order.getDirection());
      } else if (order.getField() instanceof UID uid) {
        DataElement de = dataElementService.getDataElement(uid.getValue());
        if (de != null) {
          params.orderBy(de, order.getDirection());
          continue;
        }

        TrackedEntityAttribute tea =
            trackedEntityAttributeService.getTrackedEntityAttribute(uid.getValue());
        if (tea == null) {
          throw new BadRequestException(
              "Cannot order by '"
                  + uid.getValue()
                  + "' as its neither a data element nor a tracked entity attribute. Events can be"
                  + " ordered by event fields, data elements and tracked entity attributes.");
        }

        params.orderBy(tea, order.getDirection());
      } else {
        throw new IllegalArgumentException(
            "Cannot order by '"
                + order.getField()
                + "'. Events can be ordered by event fields, data elements and tracked entity"
                + " attributes.");
      }
    }
  }
}
