/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateOrgUnitMode;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.hisp.dhis.tracker.export.OperationsParamsValidator;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps {@link EventOperationParams} to {@link EventQueryParams} which is used to fetch events from
 * the DB.
 */
@Component
@RequiredArgsConstructor
class EventOperationParamsMapper {

  private final ProgramStageService programStageService;

  private final OrganisationUnitService organisationUnitService;

  private final AclService aclService;

  private final CategoryOptionComboService categoryOptionComboService;

  private final UserService userService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final DataElementService dataElementService;

  private final OperationsParamsValidator paramsValidator;

  @Transactional(readOnly = true)
  public EventQueryParams map(EventOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    Program program = paramsValidator.validateProgram(operationParams.getProgramUid(), currentUser);
    ProgramStage programStage =
        validateProgramStage(operationParams.getProgramStageUid(), currentUser);
    TrackedEntity trackedEntity =
        paramsValidator.validateTrackedEntity(operationParams.getTrackedEntityUid(), currentUser);

    OrganisationUnit orgUnit =
        validateRequestedOrgUnit(operationParams.getOrgUnitUid(), currentUser);
    validateOrgUnitMode(operationParams.getOrgUnitMode(), currentUser, program);

    CategoryOptionCombo attributeOptionCombo =
        categoryOptionComboService.getAttributeOptionCombo(
            operationParams.getAttributeCategoryCombo() != null
                ? operationParams.getAttributeCategoryCombo()
                : null,
            operationParams.getAttributeCategoryOptions(),
            true);

    validateAttributeOptionCombo(attributeOptionCombo, currentUser);

    EventQueryParams queryParams = new EventQueryParams();

    mapDataElementFilters(queryParams, operationParams.getDataElementFilters());
    mapAttributeFilters(queryParams, operationParams.getAttributeFilters());
    mapOrderParam(queryParams, operationParams.getOrder());

    return queryParams
        .setProgram(program)
        .setProgramStage(programStage)
        .setOrgUnit(orgUnit)
        .setTrackedEntity(trackedEntity)
        .setProgramStatus(operationParams.getProgramStatus())
        .setFollowUp(operationParams.getFollowUp())
        .setOrgUnitMode(operationParams.getOrgUnitMode())
        .setAssignedUserQueryParam(
            new AssignedUserQueryParam(
                operationParams.getAssignedUserMode(),
                currentUser,
                operationParams.getAssignedUsers()))
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
        .setIdSchemes(operationParams.getIdSchemes())
        .setIncludeAttributes(false)
        .setIncludeAllDataElements(false)
        .setEvents(operationParams.getEvents())
        .setEnrollments(operationParams.getEnrollments())
        .setIncludeDeleted(operationParams.isIncludeDeleted())
        .setIncludeRelationships(operationParams.getEventParams().isIncludeRelationships());
  }

  private ProgramStage validateProgramStage(String programStageUid, User user)
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

  private OrganisationUnit validateRequestedOrgUnit(String orgUnitUid, User user)
      throws BadRequestException, ForbiddenException {
    if (orgUnitUid == null) {
      return null;
    }
    OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(orgUnitUid);
    if (orgUnit == null) {
      throw new BadRequestException("Org unit is specified but does not exist: " + orgUnitUid);
    }

    if (!organisationUnitService.isInUserHierarchy(
        orgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback())) {
      throw new ForbiddenException(
          "Organisation unit is not part of your search scope: " + orgUnit.getUid());
    }

    return orgUnit;
  }

  private void validateAttributeOptionCombo(CategoryOptionCombo attributeOptionCombo, User user)
      throws ForbiddenException {
    if (attributeOptionCombo != null
        && (user != null && !user.isSuper())
        && !aclService.canDataRead(user, attributeOptionCombo)) {
      throw new ForbiddenException(
          "User has no access to attribute category option combo: "
              + attributeOptionCombo.getUid());
    }
  }

  private void mapDataElementFilters(
      EventQueryParams params, Map<String, List<QueryFilter>> dataElementFilters)
      throws BadRequestException {
    for (Entry<String, List<QueryFilter>> dataElementFilter : dataElementFilters.entrySet()) {
      DataElement de = dataElementService.getDataElement(dataElementFilter.getKey());
      if (de == null) {
        throw new BadRequestException(
            String.format(
                "filter is invalid. Data element '%s' does not exist.",
                dataElementFilter.getKey()));
      }

      for (QueryFilter filter : dataElementFilter.getValue()) {
        params.filterBy(de, filter);
      }
    }
  }

  private void mapAttributeFilters(
      EventQueryParams params, Map<String, List<QueryFilter>> attributeFilters)
      throws BadRequestException {
    for (Map.Entry<String, List<QueryFilter>> attributeFilter : attributeFilters.entrySet()) {
      TrackedEntityAttribute tea =
          trackedEntityAttributeService.getTrackedEntityAttribute(attributeFilter.getKey());
      if (tea == null) {
        throw new BadRequestException(
            String.format(
                "attribute filters are invalid. Tracked entity attribute '%s' does not exist.",
                attributeFilter.getKey()));
      }

      if (attributeFilter.getValue().isEmpty()) {
        params.filterBy(tea);
      }

      for (QueryFilter filter : attributeFilter.getValue()) {
        params.filterBy(tea, filter);
      }
    }
  }

  private void mapOrderParam(EventQueryParams params, List<Order> orders)
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
                  + "' as its neither a data element nor a tracked entity attribute. Events can be ordered by event fields, data elements and tracked entity attributes.");
        }

        params.orderBy(tea, order.getDirection());
      } else {
        throw new IllegalArgumentException(
            "Cannot order by '"
                + order.getField()
                + "'. Events can be ordered by event fields, data elements and tracked entity attributes.");
      }
    }
  }
}
