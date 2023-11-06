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

import static org.hisp.dhis.security.Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS;
import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateOrgUnitMode;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps {@link EventOperationParams} to {@link EventQueryParams} which is used to fetch events from
 * the DB.
 */
@Component
@RequiredArgsConstructor
class EventOperationParamsMapper {

  private final ProgramService programService;

  private final ProgramStageService programStageService;

  private final OrganisationUnitService organisationUnitService;

  private final TrackedEntityService trackedEntityService;

  private final AclService aclService;

  private final CategoryOptionComboService categoryOptionComboService;

  private final CurrentUserService currentUserService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final DataElementService dataElementService;

  @Transactional(readOnly = true)
  public EventQueryParams map(EventOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    User user = currentUserService.getCurrentUser();

    Program program = validateProgram(operationParams.getProgramUid());
    ProgramStage programStage = validateProgramStage(operationParams.getProgramStageUid());

    OrganisationUnit orgUnit = validateRequestedOrgUnit(operationParams.getOrgUnitUid());
    validateUser(user, program, programStage, orgUnit);

    validateOrgUnitMode(operationParams.getOrgUnitMode(), user, program);

    TrackedEntity trackedEntity = validateTrackedEntity(operationParams.getTrackedEntityUid());

    CategoryOptionCombo attributeOptionCombo =
        categoryOptionComboService.getAttributeOptionCombo(
            operationParams.getAttributeCategoryCombo() != null
                ? operationParams.getAttributeCategoryCombo()
                : null,
            operationParams.getAttributeCategoryOptions(),
            true);

    validateAttributeOptionCombo(attributeOptionCombo, user);

    EventQueryParams queryParams = new EventQueryParams();

    mapDataElementFilters(queryParams, operationParams.getDataElementFilters());
    mapAttributeFilters(queryParams, operationParams.getAttributeFilters());
    mapOrderParam(queryParams, operationParams.getOrder());
    validateOrderableAttributes(queryParams.getOrder(), user);
    validateOrderableDataElements(queryParams.getOrder(), user);

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
                operationParams.getAssignedUserMode(), user, operationParams.getAssignedUsers()))
        .setOccurredStartDate(operationParams.getOccurredAfter())
        .setOccurredEndDate(operationParams.getOccurredBefore())
        .setScheduleAtStartDate(operationParams.getScheduledAfter())
        .setScheduleAtEndDate(operationParams.getScheduledBefore())
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
        .setIncludeRelationships(operationParams.isIncludeRelationships());
  }

  private Program validateProgram(String programUid) throws BadRequestException {
    if (programUid == null) {
      return null;
    }

    Program program = programService.getProgram(programUid);
    if (program == null) {
      throw new BadRequestException("Program is specified but does not exist: " + programUid);
    }

    return program;
  }

  private ProgramStage validateProgramStage(String programStageUid) throws BadRequestException {
    if (programStageUid == null) {
      return null;
    }

    ProgramStage programStage = programStageService.getProgramStage(programStageUid);
    if (programStage == null) {
      throw new BadRequestException(
          "Program stage is specified but does not exist: " + programStageUid);
    }

    return programStage;
  }

  private OrganisationUnit validateRequestedOrgUnit(String orgUnitUid) throws BadRequestException {
    if (orgUnitUid == null) {
      return null;
    }
    OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(orgUnitUid);
    if (orgUnit == null) {
      throw new BadRequestException("Org unit is specified but does not exist: " + orgUnitUid);
    }
    return orgUnit;
  }

  private void validateUser(
      User user, Program program, ProgramStage programStage, OrganisationUnit requestedOrgUnit)
      throws ForbiddenException {

    if (user == null
        || user.isSuper()
        || user.isAuthorized(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS)) {
      return;
    }
    if (program != null && !aclService.canDataRead(user, program)) {
      throw new ForbiddenException("User has no access to program: " + program.getUid());
    }

    if (programStage != null && !aclService.canDataRead(user, programStage)) {
      throw new ForbiddenException("User has no access to program stage: " + programStage.getUid());
    }

    if (requestedOrgUnit != null
        && !organisationUnitService.isInUserHierarchy(
            requestedOrgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback())) {
      throw new ForbiddenException(
          "Organisation unit is not part of your search scope: " + requestedOrgUnit.getUid());
    }
  }

  private TrackedEntity validateTrackedEntity(String trackedEntityUid) throws BadRequestException {
    if (trackedEntityUid == null) {
      return null;
    }

    TrackedEntity trackedEntity = trackedEntityService.getTrackedEntity(trackedEntityUid);
    if (trackedEntity == null) {
      throw new BadRequestException(
          "Tracked entity is specified but does not exist: " + trackedEntityUid);
    }

    return trackedEntity;
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

  private void validateOrderableAttributes(List<Order> order, User user) throws ForbiddenException {
    Set<TrackedEntityAttribute> orderableTeas =
        order.stream()
            .filter(o -> o.getField() instanceof TrackedEntityAttribute)
            .map(o -> (TrackedEntityAttribute) o.getField())
            .collect(Collectors.toSet());

    for (TrackedEntityAttribute tea : orderableTeas) {
      if (!aclService.canRead(user, tea)) {
        throw new ForbiddenException(
            "User has no access to tracked entity attribute: " + tea.getUid());
      }
    }
  }

  private void validateOrderableDataElements(List<Order> order, User user)
      throws ForbiddenException {
    Set<DataElement> orderableDes =
        order.stream()
            .filter(o -> o.getField() instanceof DataElement)
            .map(o -> (DataElement) o.getField())
            .collect(Collectors.toSet());

    for (DataElement de : orderableDes) {
      if (!aclService.canRead(user, de)) {
        throw new ForbiddenException("User has no access to data element: " + de.getUid());
      }
    }
  }
}
