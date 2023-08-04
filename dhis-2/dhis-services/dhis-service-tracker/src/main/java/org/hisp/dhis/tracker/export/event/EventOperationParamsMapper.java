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

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
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
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps {@link EventOperationParams} to {@link EventSearchParams} which is used to fetch events from
 * the DB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventOperationParamsMapper {

  private final ProgramService programService;

  private final ProgramStageService programStageService;

  private final OrganisationUnitService organisationUnitService;

  private final TrackedEntityService trackedEntityService;

  private final AclService aclService;

  private final CategoryOptionComboService categoryOptionComboService;

  private final TrackerAccessManager trackerAccessManager;

  private final CurrentUserService currentUserService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final DataElementService dataElementService;

  @Transactional(readOnly = true)
  public EventSearchParams map(EventOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    User user = currentUserService.getCurrentUser();

    Program program = validateProgram(operationParams.getProgramUid());
    ProgramStage programStage = validateProgramStage(operationParams.getProgramStageUid());
    OrganisationUnit requestedOrgUnit = validateRequestedOrgUnit(operationParams.getOrgUnitUid());

    OrganisationUnitSelectionMode orgUnitMode =
        getOrgUnitMode(requestedOrgUnit, operationParams.getOrgUnitMode());
    List<OrganisationUnit> accessibleOrgUnits =
        validateAccessibleOrgUnits(
            user,
            requestedOrgUnit,
            orgUnitMode,
            program,
            organisationUnitService::getOrganisationUnitWithChildren,
            trackerAccessManager);
    validateUser(user, program, programStage);
    TrackedEntity trackedEntity = validateTrackedEntity(operationParams.getTrackedEntityUid());

    CategoryOptionCombo attributeOptionCombo =
        categoryOptionComboService.getAttributeOptionCombo(
            operationParams.getAttributeCategoryCombo() != null
                ? operationParams.getAttributeCategoryCombo()
                : null,
            operationParams.getAttributeCategoryOptions(),
            true);

    validateAttributeOptionCombo(attributeOptionCombo, user);

    validateOrgUnitMode(operationParams, user, program);

    EventSearchParams searchParams = new EventSearchParams();

    mapDataElementFilters(searchParams, operationParams.getDataElementFilters());
    mapAttributeFilters(searchParams, operationParams.getAttributeFilters());
    mapOrderParam(searchParams, operationParams.getOrder());

    return searchParams
        .setProgram(program)
        .setProgramStage(programStage)
        .setAccessibleOrgUnits(accessibleOrgUnits)
        .setTrackedEntity(trackedEntity)
        .setProgramStatus(operationParams.getProgramStatus())
        .setFollowUp(operationParams.getFollowUp())
        .setOrgUnitMode(orgUnitMode)
        .setAssignedUserQueryParam(
            new AssignedUserQueryParam(
                operationParams.getAssignedUserMode(), user, operationParams.getAssignedUsers()))
        .setStartDate(operationParams.getStartDate())
        .setEndDate(operationParams.getEndDate())
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
        .setPage(operationParams.getPage())
        .setPageSize(operationParams.getPageSize())
        .setTotalPages(operationParams.isTotalPages())
        .setSkipPaging(operationParams.isSkipPaging())
        .setSkipEventId(operationParams.getSkipEventId())
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

  private void validateUser(User user, Program program, ProgramStage programStage)
      throws ForbiddenException {
    if (user.isSuper()) {
      return;
    }
    if (program != null && !aclService.canDataRead(user, program)) {
      throw new ForbiddenException("User has no access to program: " + program.getUid());
    }

    if (programStage != null && !aclService.canDataRead(user, programStage)) {
      throw new ForbiddenException("User has no access to program stage: " + programStage.getUid());
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
        && !user.isSuper()
        && !aclService.canDataRead(user, attributeOptionCombo)) {
      throw new ForbiddenException(
          "User has no access to attribute category option combo: "
              + attributeOptionCombo.getUid());
    }
  }

  private void validateOrgUnitMode(EventOperationParams params, User user, Program program)
      throws BadRequestException {
    if (params.getOrgUnitMode() != null) {
      String violation = getOrgUnitModeViolation(params, user, program);
      if (violation != null) {
        throw new BadRequestException(violation);
      }
    }
  }

  private String getOrgUnitModeViolation(EventOperationParams params, User user, Program program) {
    OrganisationUnitSelectionMode orgUnitMode = params.getOrgUnitMode();

    return switch (orgUnitMode) {
      case ALL -> userCanSearchOrgUnitModeALL(user)
          ? null
          : "Current user is not authorized to query across all organisation units";
      case ACCESSIBLE -> getAccessibleScopeValidation(user, program);
      case CAPTURE -> getCaptureScopeValidation(user);
      case CHILDREN, SELECTED, DESCENDANTS -> params.getOrgUnitUid() == null
          ? "Organisation unit is required for orgUnitMode: " + params.getOrgUnitMode()
          : null;
    };
  }

  private String getCaptureScopeValidation(User user) {
    String violation = null;

    if (user == null) {
      violation = "User is required for orgUnitMode: " + OrganisationUnitSelectionMode.CAPTURE;
    } else if (user.getOrganisationUnits().isEmpty()) {
      violation = "User needs to be assigned data capture orgunits";
    }

    return violation;
  }

  private String getAccessibleScopeValidation(User user, Program program) {
    String violation;

    if (user == null) {
      return "User is required for orgUnitMode: " + OrganisationUnitSelectionMode.ACCESSIBLE;
    }

    if (program == null || program.isClosed() || program.isProtected()) {
      violation =
          user.getOrganisationUnits().isEmpty()
              ? "User needs to be assigned data capture orgunits"
              : null;
    } else {
      violation =
          user.getTeiSearchOrganisationUnitsWithFallback().isEmpty()
              ? "User needs to be assigned either TE search, data view or data capture org units"
              : null;
    }

    return violation;
  }

  private boolean userCanSearchOrgUnitModeALL(User user) {
    if (user == null) {
      return false;
    }

    return user.isSuper()
        || user.isAuthorized(Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name());
  }

  /**
   * Returns the same org unit mode if not null. If null, and an org unit is present, SELECT mode is
   * used by default, mode ACCESSIBLE is used otherwise.
   *
   * @param orgUnit
   * @param orgUnitMode
   * @return an org unit mode given the two input params
   */
  private OrganisationUnitSelectionMode getOrgUnitMode(
      OrganisationUnit orgUnit, OrganisationUnitSelectionMode orgUnitMode) {
    if (orgUnitMode == null) {
      return orgUnit != null ? SELECTED : ACCESSIBLE;
    }
    return orgUnitMode;
  }

  private List<OrganisationUnit> validateAccessibleOrgUnits(
      User user,
      OrganisationUnit orgUnit,
      OrganisationUnitSelectionMode orgUnitMode,
      Program program,
      java.util.function.Function<String, List<OrganisationUnit>> orgUnitDescendants,
      TrackerAccessManager trackerAccessManager)
      throws ForbiddenException {
    List<OrganisationUnit> accessibleOrgUnits =
        getUserAccessibleOrgUnits(
            user, orgUnit, orgUnitMode, program, orgUnitDescendants, trackerAccessManager);

    if (orgUnit != null && accessibleOrgUnits.isEmpty()) {
      throw new ForbiddenException("User does not have access to orgUnit: " + orgUnit.getUid());
    }

    return accessibleOrgUnits;
  }

  /**
   * Returns a list of all the org units the user has access to
   *
   * @param user the user to check the access of
   * @param orgUnit parent org unit to get descendants/children of
   * @param orgUnitDescendants function to retrieve org units, in case ou mode is descendants
   * @param program the program the user wants to access to
   * @return a list containing the user accessible organisation units
   */
  private static List<OrganisationUnit> getUserAccessibleOrgUnits(
      User user,
      OrganisationUnit orgUnit,
      OrganisationUnitSelectionMode orgUnitMode,
      Program program,
      Function<String, List<OrganisationUnit>> orgUnitDescendants,
      TrackerAccessManager trackerAccessManager) {

    switch (orgUnitMode) {
      case DESCENDANTS:
        return orgUnit != null
            ? getAccessibleDescendants(user, program, orgUnitDescendants.apply(orgUnit.getUid()))
            : Collections.emptyList();
      case CHILDREN:
        return orgUnit != null
            ? getAccessibleDescendants(
                user,
                program,
                Stream.concat(Stream.of(orgUnit), orgUnit.getChildren().stream()).toList())
            : Collections.emptyList();
      case CAPTURE:
        return new ArrayList<>(user.getOrganisationUnits());
      case ACCESSIBLE:
        return getAccessibleOrgUnits(user, program);
      case SELECTED:
        return getSelectedOrgUnits(user, program, orgUnit, trackerAccessManager);
      default:
        return Collections.emptyList();
    }
  }

  private static List<OrganisationUnit> getSelectedOrgUnits(
      User user,
      Program program,
      OrganisationUnit orgUnit,
      TrackerAccessManager trackerAccessManager) {
    return trackerAccessManager.canAccess(user, program, orgUnit)
        ? List.of(orgUnit)
        : Collections.emptyList();
  }

  private static List<OrganisationUnit> getAccessibleOrgUnits(User user, Program program) {
    return isProgramAccessRestricted(program)
        ? new ArrayList<>(user.getOrganisationUnits())
        : new ArrayList<>(user.getTeiSearchOrganisationUnitsWithFallback());
  }

  /**
   * Returns the org units whose path is contained in the user search or capture scope org unit. If
   * there's a match, it means the user org unit is at the same level or above the supplied org
   * unit.
   *
   * @param user the user to check the access of
   * @param program the program the user wants to access to
   * @param orgUnits the org units to check if the user has access to
   * @return a list with the org units the user has access to
   */
  private static List<OrganisationUnit> getAccessibleDescendants(
      User user, Program program, List<OrganisationUnit> orgUnits) {
    if (orgUnits.isEmpty()) {
      return Collections.emptyList();
    }

    if (isProgramAccessRestricted(program)) {
      return orgUnits.stream()
          .filter(
              availableOrgUnit ->
                  user.getOrganisationUnits().stream()
                      .anyMatch(
                          captureScopeOrgUnit ->
                              availableOrgUnit.getPath().contains(captureScopeOrgUnit.getPath())))
          .toList();
    } else {
      return orgUnits.stream()
          .filter(
              availableOrgUnit ->
                  user.getTeiSearchOrganisationUnits().stream()
                      .anyMatch(
                          searchScopeOrgUnit ->
                              availableOrgUnit.getPath().contains(searchScopeOrgUnit.getPath())))
          .toList();
    }
  }

  private static boolean isProgramAccessRestricted(Program program) {
    return program != null && (program.isClosed() || program.isProtected());
  }

  private void mapDataElementFilters(
      EventSearchParams params, Map<String, List<QueryFilter>> dataElementFilters)
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
      EventSearchParams params, Map<String, List<QueryFilter>> attributeFilters)
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

  private void mapOrderParam(EventSearchParams params, List<Order> orders)
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
