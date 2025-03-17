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
package org.hisp.dhis.tracker.export.trackedentity;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateOrgUnitMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.export.OperationsParamsValidator;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps {@link TrackedEntityOperationParams} to {@link TrackedEntityQueryParams} which is used to
 * fetch tracked entities from the DB.
 */
@Component
@RequiredArgsConstructor
class TrackedEntityOperationParamsMapper {
  @Nonnull private final OrganisationUnitService organisationUnitService;

  @Nonnull private final TrackedEntityTypeService trackedEntityTypeService;

  @Nonnull private final TrackedEntityAttributeService attributeService;

  @Nonnull private final AclService aclService;

  // TODO Remove this dependency from the mapper when working on
  // https://dhis2.atlassian.net/browse/DHIS2-15915
  @Nonnull private final TrackedEntityStore trackedEntityStore;

  @Nonnull private final TrackedEntityAttributeService trackedEntityAttributeService;

  @Nonnull private final ProgramService programService;

  private final OperationsParamsValidator paramsValidator;

  @Transactional(readOnly = true)
  public TrackedEntityQueryParams map(TrackedEntityOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    User user = operationParams.getUser();

    Program program = paramsValidator.validateTrackerProgram(operationParams.getProgramUid(), user);
    ProgramStage programStage = validateProgramStage(operationParams, program);

    TrackedEntityType requestedTrackedEntityType =
        paramsValidator.validateTrackedEntityType(operationParams.getTrackedEntityTypeUid(), user);

    List<TrackedEntityType> trackedEntityTypes = getTrackedEntityTypes(program, user);

    List<Program> programs = getTrackerPrograms(program, user);

    Set<OrganisationUnit> orgUnits =
        paramsValidator.validateOrgUnits(operationParams.getOrganisationUnits(), user);
    validateOrgUnitMode(operationParams.getOrgUnitMode(), user, program);

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    mapAttributeFilters(params, operationParams.getFilters());

    mapOrderParam(params, operationParams.getOrder());

    validateTrackedEntityAttributeFilters(
        program, requestedTrackedEntityType, operationParams, orgUnits, params);

    params
        .setEnrolledInTrackerProgram(program)
        .setAccessibleTrackerPrograms(programs)
        .setProgramStage(programStage)
        .setProgramStatus(operationParams.getProgramStatus())
        .setFollowUp(operationParams.getFollowUp())
        .setLastUpdatedStartDate(operationParams.getLastUpdatedStartDate())
        .setLastUpdatedEndDate(operationParams.getLastUpdatedEndDate())
        .setLastUpdatedDuration(operationParams.getLastUpdatedDuration())
        .setProgramEnrollmentStartDate(operationParams.getProgramEnrollmentStartDate())
        .setProgramEnrollmentEndDate(operationParams.getProgramEnrollmentEndDate())
        .setProgramIncidentStartDate(operationParams.getProgramIncidentStartDate())
        .setProgramIncidentEndDate(operationParams.getProgramIncidentEndDate())
        .setTrackedEntityType(requestedTrackedEntityType)
        .setTrackedEntityTypes(trackedEntityTypes)
        .addOrgUnits(orgUnits)
        .setOrgUnitMode(operationParams.getOrgUnitMode())
        .setEventStatus(operationParams.getEventStatus())
        .setEventStartDate(operationParams.getEventStartDate())
        .setEventEndDate(operationParams.getEventEndDate())
        .setAssignedUserQueryParam(operationParams.getAssignedUserQueryParam())
        .setUser(user)
        .setTrackedEntityUids(operationParams.getTrackedEntityUids())
        .setIncludeDeleted(operationParams.isIncludeDeleted())
        .setPotentialDuplicate(operationParams.getPotentialDuplicate());

    validateSearchOutsideCaptureScopeParameters(params);

    return params;
  }

  private List<TrackedEntityType> getTrackedEntityTypes(Program program, User user)
      throws BadRequestException {

    if (program != null) {
      return List.of(program.getTrackedEntityType());
    } else {
      return filterAndValidateTrackedEntityTypes(user);
    }
  }

  private List<TrackedEntityType> filterAndValidateTrackedEntityTypes(User user)
      throws BadRequestException {
    List<TrackedEntityType> trackedEntityTypes =
        trackedEntityTypeService.getAllTrackedEntityType().stream()
            .filter(tet -> aclService.canDataRead(user, tet))
            .toList();

    if (trackedEntityTypes.isEmpty()) {
      throw new BadRequestException("User has no access to any Tracked Entity Type");
    }

    return trackedEntityTypes;
  }

  private List<Program> getTrackerPrograms(Program program, User user) {
    if (program == null) {
      return programService.getAllPrograms().stream()
          .filter(Program::isRegistration)
          .filter(p -> aclService.canDataRead(user, p))
          .toList();
    }

    return emptyList();
  }

  private void mapAttributeFilters(
      TrackedEntityQueryParams params, Map<String, List<QueryFilter>> attributeFilters)
      throws BadRequestException {
    for (Map.Entry<String, List<QueryFilter>> attributeFilter : attributeFilters.entrySet()) {
      TrackedEntityAttribute tea =
          attributeService.getTrackedEntityAttribute(attributeFilter.getKey());
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

  private ProgramStage validateProgramStage(
      TrackedEntityOperationParams requestParams, Program program) throws BadRequestException {

    ProgramStage ps =
        requestParams.getProgramStageUid() != null
            ? getProgramStageFromProgram(program, requestParams.getProgramStageUid())
            : null;
    if (requestParams.getProgramStageUid() != null && ps == null) {
      throw new BadRequestException(
          "Program does not contain the specified programStage: "
              + requestParams.getProgramStageUid());
    }
    return ps;
  }

  private ProgramStage getProgramStageFromProgram(Program program, String programStage) {
    if (program == null) {
      return null;
    }

    return program.getProgramStages().stream()
        .filter(ps -> ps.getUid().equals(programStage))
        .findFirst()
        .orElse(null);
  }

  private void mapOrderParam(TrackedEntityQueryParams params, List<Order> orders)
      throws BadRequestException {
    if (orders == null || orders.isEmpty()) {
      return;
    }

    for (Order order : orders) {
      if (order.getField() instanceof String field) {
        params.orderBy(field, order.getDirection());
      } else if (order.getField() instanceof UID uid) {
        TrackedEntityAttribute tea = attributeService.getTrackedEntityAttribute(uid.getValue());
        if (tea == null) {
          throw new BadRequestException(
              "Cannot order by '"
                  + uid.getValue()
                  + "' as its not a tracked entity attribute. Tracked entities can be ordered by fields and tracked entity attributes.");
        }

        params.orderBy(tea, order.getDirection());
      } else {
        throw new IllegalArgumentException(
            "Cannot order by '"
                + order.getField()
                + "'. Tracked entities can be ordered by fields and tracked entity attributes.");
      }
    }
  }

  private void validateTrackedEntityAttributeFilters(
      Program program,
      TrackedEntityType trackedEntityType,
      TrackedEntityOperationParams operationParams,
      Set<OrganisationUnit> orgUnits,
      TrackedEntityQueryParams params) {
    if (program == null
        && trackedEntityType == null
        && operationParams.getFilters() != null
        && orgUnits.isEmpty()) {
      List<String> uniqueAttributeIds =
          trackedEntityAttributeService.getAllSystemWideUniqueTrackedEntityAttributes().stream()
              .map(TrackedEntityAttribute::getUid)
              .toList();

      for (String att : params.getFilterIds()) {
        if (!uniqueAttributeIds.contains(att)) {
          throw new IllegalQueryException(
              "Either a program or tracked entity type must be specified");
        }
      }
    }
  }

  private void validateSearchOutsideCaptureScopeParameters(TrackedEntityQueryParams params)
      throws IllegalQueryException {
    if (isSearchInCaptureScope(params, params.getUser())) {
      return;
    }

    if (params.hasFilters()) {
      List<String> searchableAttributeIds = getSearchableAttributeIds(params);
      validateSearchableAttributes(params, searchableAttributeIds);
    }

    int maxTeiLimit = getMaxTeiLimit(params);
    params.setMaxTeLimit(maxTeiLimit);
    checkIfMaxTeiLimitIsReached(params, maxTeiLimit);
  }

  private List<String> getSearchableAttributeIds(TrackedEntityQueryParams params) {
    List<String> searchableAttributeIds = new ArrayList<>();

    if (params.hasEnrolledInTrackerProgram()) {
      searchableAttributeIds.addAll(
          params.getEnrolledInTrackerProgram().getSearchableAttributeIds());
    }

    if (params.hasTrackedEntityType()) {
      searchableAttributeIds.addAll(params.getTrackedEntityType().getSearchableAttributeIds());
    }

    if (!params.hasEnrolledInTrackerProgram() && !params.hasTrackedEntityType()) {
      searchableAttributeIds.addAll(
          trackedEntityAttributeService.getAllSystemWideUniqueTrackedEntityAttributes().stream()
              .map(TrackedEntityAttribute::getUid)
              .toList());
    }

    return searchableAttributeIds;
  }

  private void validateSearchableAttributes(
      TrackedEntityQueryParams params, List<String> searchableAttributeIds) {
    List<String> violatingAttributes = new ArrayList<>();

    for (String attributeId : params.getFilterIds()) {
      if (!searchableAttributeIds.contains(attributeId)) {
        violatingAttributes.add(attributeId);
      }
    }

    if (!violatingAttributes.isEmpty()) {
      throw new IllegalQueryException(
          "Non-searchable attribute(s) can not be used during global search:  "
              + violatingAttributes);
    }
  }

  private int getMaxTeiLimit(TrackedEntityQueryParams params) {
    int maxTeiLimit = 0;
    if (params.hasTrackedEntityType()) {
      maxTeiLimit = params.getTrackedEntityType().getMaxTeiCountToReturn();

      if (!params.hasTrackedEntities() && isTeTypeMinAttributesViolated(params)) {
        throw new IllegalQueryException(
            "At least "
                + params.getTrackedEntityType().getMinAttributesRequiredToSearch()
                + " attributes should be mentioned in the search criteria.");
      }
    }

    if (params.hasEnrolledInTrackerProgram()) {
      maxTeiLimit = params.getEnrolledInTrackerProgram().getMaxTeiCountToReturn();

      if (!params.hasTrackedEntities() && isProgramMinAttributesViolated(params)) {
        throw new IllegalQueryException(
            "At least "
                + params.getEnrolledInTrackerProgram().getMinAttributesRequiredToSearch()
                + " attributes should be mentioned in the search criteria.");
      }
    }

    return maxTeiLimit;
  }

  private boolean isSearchInCaptureScope(TrackedEntityQueryParams params, User user) {
    if (OrganisationUnitSelectionMode.CAPTURE == params.getOrgUnitMode()) {
      return true;
    }

    Set<OrganisationUnit> localOrgUnits = user.getOrganisationUnits();

    Set<OrganisationUnit> searchOrgUnits = new HashSet<>();

    if (params.isOrganisationUnitMode(SELECTED)) {
      searchOrgUnits = params.getOrgUnits();
    } else if (params.isOrganisationUnitMode(CHILDREN)
        || params.isOrganisationUnitMode(DESCENDANTS)) {
      for (OrganisationUnit orgUnit : params.getOrgUnits()) {
        searchOrgUnits.addAll(orgUnit.getChildren());
      }
    } else if (params.isOrganisationUnitMode(ALL)) {
      searchOrgUnits.addAll(organisationUnitService.getRootOrganisationUnits());
    } else {
      searchOrgUnits.addAll(user.getTeiSearchOrganisationUnitsWithFallback());
    }

    for (OrganisationUnit ou : searchOrgUnits) {
      if (!ou.isDescendant(localOrgUnits)) {
        return false;
      }
    }

    return true;
  }

  private boolean isTeTypeMinAttributesViolated(TrackedEntityQueryParams params) {
    if (params.hasUniqueFilter()) {
      return false;
    }

    return (!params.hasFilters()
            && params.getTrackedEntityType().getMinAttributesRequiredToSearch() > 0)
        || (params.hasFilters()
            && params.getFilters().size()
                < params.getTrackedEntityType().getMinAttributesRequiredToSearch());
  }

  private boolean isProgramMinAttributesViolated(TrackedEntityQueryParams params) {
    if (params.hasUniqueFilter()) {
      return false;
    }

    return (!params.hasFilters()
            && params.getEnrolledInTrackerProgram().getMinAttributesRequiredToSearch() > 0)
        || (params.hasFilters()
            && params.getFilters().size()
                < params.getEnrolledInTrackerProgram().getMinAttributesRequiredToSearch());
  }

  private void checkIfMaxTeiLimitIsReached(TrackedEntityQueryParams params, int maxTeiLimit) {
    if (maxTeiLimit > 0) {
      int teCount = trackedEntityStore.getTrackedEntityCountWithMaxTrackedEntityLimit(params);

      if (teCount > maxTeiLimit) {
        throw new IllegalQueryException("maxteicountreached");
      }
    }
  }
}
