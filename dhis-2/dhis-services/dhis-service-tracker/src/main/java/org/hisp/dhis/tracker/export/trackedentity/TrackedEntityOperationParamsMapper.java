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

  @Nonnull private final ProgramService programService;

  @Nonnull private final TrackedEntityTypeService trackedEntityTypeService;

  @Nonnull private final TrackedEntityAttributeService attributeService;

  @Nonnull private final AclService aclService;

  // TODO Remove this dependency from the mapper when working on
  // https://dhis2.atlassian.net/browse/DHIS2-15915
  @Nonnull private final TrackedEntityStore trackedEntityStore;

  @Nonnull private final TrackedEntityAttributeService trackedEntityAttributeService;

  @Transactional(readOnly = true)
  public TrackedEntityQueryParams map(TrackedEntityOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    User user = operationParams.getUser();

    Program program = validateProgram(operationParams.getProgramUid(), user);
    ProgramStage programStage = validateProgramStage(operationParams, program);

    TrackedEntityType requestedTrackedEntityType =
        validateTrackedEntityType(operationParams.getTrackedEntityTypeUid(), user);

    List<TrackedEntityType> trackedEntityTypes =
        getTrackedEntityTypes(requestedTrackedEntityType, program, user);

    Set<OrganisationUnit> orgUnits = validateOrgUnits(user, operationParams.getOrganisationUnits());
    validateOrgUnitMode(operationParams.getOrgUnitMode(), user, program);

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    mapAttributeFilters(params, operationParams.getFilters());

    mapOrderParam(params, operationParams.getOrder());

    validateTrackedEntityAttributeFilters(
        program, requestedTrackedEntityType, operationParams, orgUnits, params);

    params
        .setProgram(program)
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

    validateGlobalSearchParameters(params);

    return params;
  }

  private List<TrackedEntityType> getTrackedEntityTypes(
      TrackedEntityType trackedEntityType, Program program, User user) throws BadRequestException {

    if (program != null && program.getTrackedEntityType() != null) {
      return List.of(program.getTrackedEntityType());
    } else if (trackedEntityType == null) {
      return filterAndValidateTrackedEntityTypes(user, program);
    }

    return emptyList();
  }

  private List<TrackedEntityType> filterAndValidateTrackedEntityTypes(User user, Program program)
      throws BadRequestException {
    List<TrackedEntityType> trackedEntityTypes =
        trackedEntityTypeService.getAllTrackedEntityType().stream()
            .filter(tet -> aclService.canDataRead(user, tet))
            .toList();

    if (program == null && trackedEntityTypes.isEmpty()) {
      throw new BadRequestException("User has no access to any Tracked Entity Type");
    }

    return trackedEntityTypes;
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

  private Set<OrganisationUnit> validateOrgUnits(User user, Set<String> orgUnitIds)
      throws BadRequestException, ForbiddenException {
    Set<OrganisationUnit> orgUnits = new HashSet<>();
    for (String orgUnitUid : orgUnitIds) {
      OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(orgUnitUid);
      if (orgUnit == null) {
        throw new BadRequestException("Organisation unit does not exist: " + orgUnitUid);
      }

      if (user != null
          && !user.isSuper()
          && !organisationUnitService.isInUserHierarchy(
              orgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback())) {
        throw new ForbiddenException(
            "Organisation unit is not part of the search scope: " + orgUnit.getUid());
      }

      orgUnits.add(orgUnit);
    }

    return orgUnits;
  }

  private Program validateProgram(String uid, User user)
      throws BadRequestException, ForbiddenException {
    if (uid == null) {
      return null;
    }

    Program program = programService.getProgram(uid);
    if (program == null) {
      throw new BadRequestException("Program is specified but does not exist: " + uid);
    }

    if (!aclService.canDataRead(user, program)) {
      throw new ForbiddenException(
          "Current user is not authorized to read data from selected program:  "
              + program.getUid());
    }

    if (program.getTrackedEntityType() != null
        && !aclService.canDataRead(user, program.getTrackedEntityType())) {
      throw new ForbiddenException(
          "Current user is not authorized to read data from selected program's tracked entity type:  "
              + program.getTrackedEntityType().getUid());
    }

    return program;
  }

  private TrackedEntityType validateTrackedEntityType(String uid, User user)
      throws BadRequestException, ForbiddenException {
    if (uid == null) {
      return null;
    }

    TrackedEntityType trackedEntityType = trackedEntityTypeService.getTrackedEntityType(uid);
    if (trackedEntityType == null) {
      throw new BadRequestException("Tracked entity type is specified but does not exist: " + uid);
    }

    if (!aclService.canDataRead(user, trackedEntityType)) {
      throw new ForbiddenException(
          "Current user is not authorized to read data from selected tracked entity type:  "
              + trackedEntityType.getUid());
    }

    return trackedEntityType;
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

  private void validateGlobalSearchParameters(TrackedEntityQueryParams params)
      throws IllegalQueryException {
    if (!isLocalSearch(params, params.getUser())) {

      if (params.hasFilters()) {
        List<String> searchableAttributeIds = getSearchableAttributeIds(params);
        validateSearchableAttributes(params, searchableAttributeIds);
      }

      int maxTeiLimit = getMaxTeiLimit(params);
      checkIfMaxTeiLimitIsReached(params, maxTeiLimit);
      params.setMaxTeLimit(maxTeiLimit);
    }
  }

  private List<String> getSearchableAttributeIds(TrackedEntityQueryParams params) {
    List<String> searchableAttributeIds = new ArrayList<>();

    if (params.hasProgram()) {
      searchableAttributeIds.addAll(params.getProgram().getSearchableAttributeIds());
    }

    if (params.hasTrackedEntityType()) {
      searchableAttributeIds.addAll(params.getTrackedEntityType().getSearchableAttributeIds());
    }

    if (!params.hasProgram() && !params.hasTrackedEntityType()) {
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

    if (params.hasProgram()) {
      maxTeiLimit = params.getProgram().getMaxTeiCountToReturn();

      if (!params.hasTrackedEntities() && isProgramMinAttributesViolated(params)) {
        throw new IllegalQueryException(
            "At least "
                + params.getProgram().getMinAttributesRequiredToSearch()
                + " attributes should be mentioned in the search criteria.");
      }
    }

    return maxTeiLimit;
  }

  private boolean isLocalSearch(TrackedEntityQueryParams params, User user) {
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

    return (!params.hasFilters() && params.getProgram().getMinAttributesRequiredToSearch() > 0)
        || (params.hasFilters()
            && params.getFilters().size() < params.getProgram().getMinAttributesRequiredToSearch());
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
