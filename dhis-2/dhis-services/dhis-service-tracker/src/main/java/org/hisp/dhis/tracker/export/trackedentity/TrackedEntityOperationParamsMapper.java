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
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.acl.TrackerProgramService;
import org.hisp.dhis.tracker.export.OperationsParamsValidator;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.UserDetails;
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

  @Nonnull private final TrackedEntityAttributeService trackedEntityAttributeService;

  @Nonnull private final TrackerProgramService trackerProgramService;

  @Nonnull private final SystemSettingsService systemSettingsService;

  private final OperationsParamsValidator paramsValidator;

  @Transactional(readOnly = true)
  public TrackedEntityQueryParams map(
      TrackedEntityOperationParams operationParams, UserDetails user)
      throws BadRequestException, ForbiddenException {
    return map(operationParams, user, null);
  }

  @Transactional(readOnly = true)
  public TrackedEntityQueryParams map(
      TrackedEntityOperationParams operationParams, UserDetails user, PageParams pageParams)
      throws BadRequestException, ForbiddenException {
    validatePagination(pageParams);

    Program program = paramsValidator.validateTrackerProgram(operationParams.getProgram(), user);
    ProgramStage programStage = validateProgramStage(operationParams, program);

    TrackedEntityType requestedTrackedEntityType =
        paramsValidator.validateTrackedEntityType(operationParams.getTrackedEntityType(), user);

    List<TrackedEntityType> trackedEntityTypes = getTrackedEntityTypes(program, user);

    List<Program> programs = getTrackerPrograms(program);

    Set<OrganisationUnit> orgUnits =
        paramsValidator.validateOrgUnits(operationParams.getOrganisationUnits(), user);
    validateOrgUnitMode(operationParams.getOrgUnitMode(), program, user);

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    mapAttributeFilters(params, operationParams.getFilters());

    mapOrderParam(params, operationParams.getOrder());

    validateTrackedEntityAttributeFilters(
        program, requestedTrackedEntityType, operationParams, orgUnits, params);

    params
        .setEnrolledInTrackerProgram(program)
        .setAccessibleTrackerPrograms(programs)
        .setProgramStage(programStage)
        .setEnrollmentStatus(operationParams.getEnrollmentStatus())
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
        .setTrackedEntities(operationParams.getTrackedEntities())
        .setIncludeDeleted(operationParams.isIncludeDeleted())
        .setPotentialDuplicate(operationParams.getPotentialDuplicate());

    validateSearchOutsideCaptureScopeParameters(params, user);

    return params;
  }

  private List<TrackedEntityType> getTrackedEntityTypes(Program program, UserDetails user)
      throws ForbiddenException {

    if (program != null) {
      return List.of(program.getTrackedEntityType());
    } else {
      return filterAndValidateTrackedEntityTypes(user);
    }
  }

  private List<TrackedEntityType> filterAndValidateTrackedEntityTypes(UserDetails user)
      throws ForbiddenException {
    List<TrackedEntityType> trackedEntityTypes =
        trackedEntityTypeService.getAllTrackedEntityType().stream()
            .filter(tet -> aclService.canDataRead(user, tet))
            .toList();

    if (trackedEntityTypes.isEmpty()) {
      throw new ForbiddenException("User has no access to any Tracked Entity Type");
    }

    return trackedEntityTypes;
  }

  private List<Program> getTrackerPrograms(Program program) {
    if (program == null) {
      return trackerProgramService.getAccessibleTrackerPrograms();
    }

    return emptyList();
  }

  private void mapAttributeFilters(
      TrackedEntityQueryParams params, Map<UID, List<QueryFilter>> attributeFilters)
      throws BadRequestException {
    for (Map.Entry<UID, List<QueryFilter>> attributeFilter : attributeFilters.entrySet()) {
      TrackedEntityAttribute tea =
          attributeService.getTrackedEntityAttribute(attributeFilter.getKey().getValue());
      if (tea == null) {
        throw new BadRequestException(
            String.format(
                "attribute filters are invalid. Tracked entity attribute '%s' does not exist.",
                attributeFilter.getKey()));
      }

      Set<QueryOperator> disallowedOperators =
          attributeFilter.getValue().stream()
              .map(QueryFilter::getOperator)
              .map(QueryOperator::mapToTrackerQueryOperator)
              .filter(op -> !tea.getAllowedSearchOperators().contains(op))
              .collect(Collectors.toSet());

      if (!disallowedOperators.isEmpty()) {
        throw new BadRequestException(
            String.format(
                "Operators %s are not allowed for attribute '%s'. Allowed operators are %s",
                disallowedOperators, attributeFilter.getKey(), tea.getAllowedSearchOperators()));
      }

      List<QueryFilter> binaryFilters =
          attributeFilter.getValue().stream().filter(qf -> qf.getOperator().isBinary()).toList();
      for (QueryFilter queryFilter : binaryFilters) {
        if (tea.getMinCharactersToSearch() > 0
            && (queryFilter.getFilter() == null
                || queryFilter.getFilter().length() < tea.getMinCharactersToSearch())) {
          throw new IllegalQueryException(
              String.format(
                  "At least %d character(s) should be present in the filter to start a search, but the filter for the TEA %s doesn't contain enough.",
                  tea.getMinCharactersToSearch(), tea.getUid()));
        }
      }

      params.filterBy(tea, attributeFilter.getValue());
    }
  }

  private ProgramStage validateProgramStage(
      TrackedEntityOperationParams requestParams, Program program) throws BadRequestException {

    ProgramStage ps =
        requestParams.getProgramStage() != null
            ? getProgramStageFromProgram(program, requestParams.getProgramStage())
            : null;
    if (requestParams.getProgramStage() != null && ps == null) {
      throw new BadRequestException(
          "Program does not contain the specified programStage: "
              + requestParams.getProgramStage());
    }
    return ps;
  }

  private ProgramStage getProgramStageFromProgram(Program program, UID programStage) {
    if (program == null) {
      return null;
    }

    return program.getProgramStages().stream()
        .filter(ps -> ps.getUid().equals(programStage.getValue()))
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
      List<UID> uniqueAttributeIds =
          trackedEntityAttributeService.getAllSystemWideUniqueTrackedEntityAttributes().stream()
              .map(UID::of)
              .toList();

      for (UID att : params.getFilterIds()) {
        if (!uniqueAttributeIds.contains(att)) {
          throw new IllegalQueryException(
              "Either a program or tracked entity type must be specified");
        }
      }
    }
  }

  private void validatePagination(PageParams pageParams) throws BadRequestException {
    if (pageParams == null) {
      return;
    }

    int systemMaxLimit = systemSettingsService.getCurrentSettings().getTrackedEntityMaxLimit();
    if (systemMaxLimit > 0 && pageParams.getPageSize() > systemMaxLimit) {
      throw new BadRequestException(
          String.format(
              "Invalid page size: %d. It must not exceed the system limit of KeyTrackedEntityMaxLimit %d.",
              pageParams.getPageSize(), systemMaxLimit));
    }
  }

  private void validateSearchOutsideCaptureScopeParameters(
      TrackedEntityQueryParams params, UserDetails user) throws IllegalQueryException {
    if (isSearchInCaptureScope(params, user)) {
      return;
    }

    params.setSearchOutsideCaptureScope(true);
    validateSearchableAttributes(params);
    validateMinAttributesToSearch(params);
  }

  private List<UID> getSearchableAttributeIds(TrackedEntityQueryParams params) {
    List<UID> searchableAttributeIds = new ArrayList<>();

    if (params.hasEnrolledInTrackerProgram()) {
      searchableAttributeIds.addAll(
          UID.of(params.getEnrolledInTrackerProgram().getSearchableAttributeIds()));
    }

    if (params.hasTrackedEntityType()) {
      searchableAttributeIds.addAll(
          UID.of(params.getTrackedEntityType().getSearchableAttributeIds()));
    }

    if (!params.hasEnrolledInTrackerProgram() && !params.hasTrackedEntityType()) {
      searchableAttributeIds.addAll(
          trackedEntityAttributeService.getAllSystemWideUniqueTrackedEntityAttributes().stream()
              .map(UID::of)
              .toList());
    }

    return searchableAttributeIds;
  }

  private void validateSearchableAttributes(TrackedEntityQueryParams params) {
    if (!params.hasFilters()) {
      return;
    }

    List<UID> violatingAttributes = new ArrayList<>();
    List<UID> searchableAttributeIds = getSearchableAttributeIds(params);

    for (UID attributeId : params.getFilterIds()) {
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

  private void validateMinAttributesToSearch(TrackedEntityQueryParams params) {
    if (params.hasTrackedEntities()) {
      return;
    }

    if (params.hasTrackedEntityType() && isTeTypeMinAttributesViolated(params)) {
      throw new IllegalQueryException(
          "At least "
              + params.getTrackedEntityType().getMinAttributesRequiredToSearch()
              + " attributes should be mentioned in the search criteria.");
    } else if (params.hasEnrolledInTrackerProgram() && isProgramMinAttributesViolated(params)) {
      throw new IllegalQueryException(
          "At least "
              + params.getEnrolledInTrackerProgram().getMinAttributesRequiredToSearch()
              + " attributes should be mentioned in the search criteria.");
    }
  }

  private boolean isSearchInCaptureScope(TrackedEntityQueryParams params, UserDetails user) {
    // If the organization unit selection mode is set to CAPTURE, then it's a local search.
    if (OrganisationUnitSelectionMode.CAPTURE == params.getOrgUnitMode()) {
      return true;
    }

    List<OrganisationUnit> localOrgUnits =
        organisationUnitService.getOrganisationUnitsByUid(user.getUserOrgUnitIds());
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
      searchOrgUnits.addAll(
          organisationUnitService.getOrganisationUnitsByUid(user.getUserSearchOrgUnitIds()));
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
}
