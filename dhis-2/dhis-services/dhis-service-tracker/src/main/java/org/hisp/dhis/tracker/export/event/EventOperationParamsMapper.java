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

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.tracker.export.OperationParamUtils.parseAttributeQueryItems;
import static org.hisp.dhis.tracker.export.OperationParamUtils.parseDataElementQueryItems;
import static org.hisp.dhis.tracker.export.OperationParamUtils.parseQueryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
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
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps {@link EventOperationParams} to {@link EventSearchParams} which is used to fetch enrollments
 * from the DB.
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

  private final TrackedEntityAttributeService attributeService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final DataElementService dataElementService;

  // For now this maps to EventSearchParams. We should create a new EventQueryParams class that
  // should be used in the persistence layer
  @Transactional(readOnly = true)
  public EventSearchParams map(EventOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    User user = currentUserService.getCurrentUser();

    Program program = validateProgram(operationParams.getProgramUid());
    ProgramStage programStage = validateProgramStage(operationParams.getProgramStageUid());
    OrganisationUnit orgUnit = validateOrgUnit(operationParams.getOrgUnitUid());
    validateUser(user, program, programStage, orgUnit, operationParams.getOrgUnitSelectionMode());
    TrackedEntity trackedEntity = validateTrackedEntity(operationParams.getTrackedEntityUid());

    CategoryOptionCombo attributeOptionCombo =
        categoryOptionComboService.getAttributeOptionCombo(
            operationParams.getAttributeCategoryCombo() != null
                ? operationParams.getAttributeCategoryCombo()
                : null,
            operationParams.getAttributeCategoryOptions(),
            true);

    validateAttributeOptionCombo(attributeOptionCombo, user);

    validateOrgUnitSelectionMode(operationParams, user, program);

    Map<String, SortDirection> attributeOrders =
        getAttributesFromOrder(operationParams.getAttributeOrders());
    List<OrderParam> attributeOrderParams = mapToOrderParams(attributeOrders);

    List<QueryItem> filterAttributes =
        parseFilterAttributes(operationParams.getFilterAttributes(), attributeOrderParams);
    validateFilterAttributes(filterAttributes);

    Map<String, SortDirection> dataElementOrders =
        getDataElementsFromOrder(operationParams.getOrders());

    List<QueryItem> dataElements = new ArrayList<>();
    for (String order : dataElementOrders.keySet()) {
      dataElements.add(parseQueryItem(order, this::dataElementToQueryItem));
    }

    List<OrderParam> dataElementOrderParams = mapToOrderParams(dataElementOrders);

    List<QueryItem> filters =
        parseDataElementQueryItems(operationParams.getFilters(), this::dataElementToQueryItem);

    EventSearchParams searchParams = new EventSearchParams();

    return searchParams
        .setProgram(program)
        .setProgramStage(programStage)
        .setOrgUnit(orgUnit)
        .setAccessibleOrgUnits(
            getUserAccessibleOrgUnits(
                program,
                user,
                orgUnit,
                operationParams.getOrgUnitSelectionMode(),
                organisationUnitService::getOrganisationUnitWithChildren))
        .setTrackedEntity(trackedEntity)
        .setProgramStatus(operationParams.getProgramStatus())
        .setFollowUp(operationParams.getFollowUp())
        .setOrgUnitSelectionMode(getOrgUnitMode(orgUnit, operationParams.getOrgUnitSelectionMode()))
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
        .setSkipPaging(toBooleanDefaultIfNull(operationParams.isSkipPaging(), false))
        .setSkipEventId(operationParams.getSkipEventId())
        .setIncludeAttributes(false)
        .setIncludeAllDataElements(false)
        .addDataElements(new LinkedHashSet<>(dataElements))
        .addFilters(filters)
        .addFilterAttributes(filterAttributes)
        .addOrders(operationParams.getOrders())
        .addGridOrders(dataElementOrderParams)
        .addAttributeOrders(attributeOrderParams)
        .setEvents(operationParams.getEvents())
        .setEnrollments(operationParams.getEnrollments())
        .setIncludeDeleted(operationParams.isIncludeDeleted());
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

  private OrganisationUnit validateOrgUnit(String orgUnitUid) throws BadRequestException {
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
      User user,
      Program program,
      ProgramStage programStage,
      OrganisationUnit orgUnit,
      OrganisationUnitSelectionMode ouMode)
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

    if (orgUnit != null
        && getUserAccessibleOrgUnits(
                program,
                user,
                orgUnit,
                ouMode,
                organisationUnitService::getOrganisationUnitWithChildren)
            .isEmpty()) {
      throw new ForbiddenException("User does not have access to orgUnit: " + orgUnit.getUid());
    }
  }

  /**
   * Returns a list of all the org units the user has access to
   *
   * @param program the program the user wants to access to
   * @param user the user to check the access of
   * @param orgUnit parent org unit to get descendants/children of
   * @param orgUnitDescendants function to retrieve org units, in case ou mode is descendants
   * @return a list containing the user accessible organisation units
   */
  private List<OrganisationUnit> getUserAccessibleOrgUnits(
      Program program,
      User user,
      OrganisationUnit orgUnit,
      OrganisationUnitSelectionMode orgUnitMode,
      Function<String, List<OrganisationUnit>> orgUnitDescendants) {

    if (orgUnitMode == null) {
      if (orgUnit != null) {
        return getSelectedOrgUnits(user, program, orgUnit);
      }

      return getAccessibleOrgUnits(user, program);
    }

    return switch (orgUnitMode) {
      case DESCENDANTS -> orgUnit != null
          ? getAccessibleDescendants(orgUnitDescendants.apply(orgUnit.getUid()), program, user)
          : Collections.emptyList();
      case CHILDREN -> orgUnit != null
          ? getAccessibleDescendants(
              Stream.concat(Stream.of(orgUnit), orgUnit.getChildren().stream()).toList(),
              program,
              user)
          : Collections.emptyList();
      case CAPTURE -> user.getOrganisationUnits().stream().toList();
      case ACCESSIBLE -> getAccessibleOrgUnits(user, program);
      case SELECTED -> getSelectedOrgUnits(user, program, orgUnit);
      default -> Collections.emptyList();
    };
  }

  private List<OrganisationUnit> getSelectedOrgUnits(
      User user, Program program, OrganisationUnit orgUnit) {
    return trackerAccessManager.canAccess(user, program, orgUnit)
        ? List.of(orgUnit)
        : Collections.emptyList();
  }

  private List<OrganisationUnit> getAccessibleOrgUnits(User user, Program program) {
    return isProgramAccessRestricted(program)
        ? user.getOrganisationUnits().stream().toList().stream().toList()
        : user.getTeiSearchOrganisationUnitsWithFallback().stream().toList();
  }

  /**
   * Returns the org units whose path is contained in the user search or capture scope org unit. If
   * there's a match, it means the user org unit is at the same level or above the supplied org
   * unit.
   *
   * @param availableOrgUnits the org units to check if the user has access to
   * @param program the program the user wants to access to
   * @param user the user to check the access of
   * @return a list with the org units the user has access to
   */
  private List<OrganisationUnit> getAccessibleDescendants(
      List<OrganisationUnit> availableOrgUnits, Program program, User user) {
    if (availableOrgUnits.isEmpty()) {
      return Collections.emptyList();
    }

    if (isProgramAccessRestricted(program)) {
      return availableOrgUnits.stream()
          .filter(
              availableOrgUnit ->
                  user.getOrganisationUnits().stream()
                      .anyMatch(
                          captureScopeOrgUnit ->
                              availableOrgUnit.getPath().contains(captureScopeOrgUnit.getPath())))
          .toList();
    } else {
      return availableOrgUnits.stream()
          .filter(
              availableOrgUnit ->
                  user.getTeiSearchOrganisationUnits().stream()
                      .anyMatch(
                          searchScopeOrgUnit ->
                              availableOrgUnit.getPath().contains(searchScopeOrgUnit.getPath())))
          .toList();
    }
  }

  private boolean isProgramAccessRestricted(Program program) {
    return program != null && (program.isClosed() || program.isProtected());
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

  private void validateOrgUnitSelectionMode(EventOperationParams params, User user, Program program)
      throws BadRequestException {
    if (params.getOrgUnitSelectionMode() != null) {
      String violation = getOrgUnitModeViolation(params, user, program);
      if (violation != null) {
        throw new BadRequestException(violation);
      }
    }
  }

  private String getOrgUnitModeViolation(EventOperationParams params, User user, Program program) {
    OrganisationUnitSelectionMode selectedOuMode = params.getOrgUnitSelectionMode();

    return switch (selectedOuMode) {
      case ALL -> userCanSearchOuModeALL(user)
          ? null
          : "Current user is not authorized to query across all organisation units";
      case ACCESSIBLE -> getAccessibleScopeValidation(user, program);
      case CAPTURE -> getCaptureScopeValidation(user);
      case CHILDREN, SELECTED, DESCENDANTS -> params.getOrgUnitUid() == null
          ? "Organisation unit is required for ouMode: " + params.getOrgUnitSelectionMode()
          : null;
    };
  }

  private String getCaptureScopeValidation(User user) {
    String violation = null;

    if (user == null) {
      violation = "User is required for ouMode: " + OrganisationUnitSelectionMode.CAPTURE;
    } else if (user.getOrganisationUnits().isEmpty()) {
      violation = "User needs to be assigned data capture orgunits";
    }

    return violation;
  }

  private String getAccessibleScopeValidation(User user, Program program) {
    String violation;

    if (user == null) {
      return "User is required for ouMode: " + OrganisationUnitSelectionMode.ACCESSIBLE;
    }

    if (program == null || program.isClosed() || program.isProtected()) {
      violation =
          user.getOrganisationUnits().isEmpty()
              ? "User needs to be assigned data capture orgunits"
              : null;
    } else {
      violation =
          user.getTeiSearchOrganisationUnitsWithFallback().isEmpty()
              ? "User needs to be assigned either TEI search, data view or data capture org units"
              : null;
    }

    return violation;
  }

  private boolean userCanSearchOuModeALL(User user) {
    if (user == null) {
      return false;
    }

    return user.isSuper()
        || user.isAuthorized(Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name());
  }

  private List<QueryItem> parseFilterAttributes(
      String filterAttributes, List<OrderParam> attributeOrderParams) throws BadRequestException {
    Map<String, TrackedEntityAttribute> attributes =
        attributeService.getAllTrackedEntityAttributes().stream()
            .collect(Collectors.toMap(TrackedEntityAttribute::getUid, att -> att));

    List<QueryItem> filterItems = parseAttributeQueryItems(filterAttributes, attributes);
    List<QueryItem> orderItems =
        attributeQueryItemsFromOrder(filterItems, attributes, attributeOrderParams);

    return Stream.concat(filterItems.stream(), orderItems.stream()).toList();
  }

  private List<QueryItem> attributeQueryItemsFromOrder(
      List<QueryItem> filterAttributes,
      Map<String, TrackedEntityAttribute> attributes,
      List<OrderParam> attributeOrderParams) {
    return attributeOrderParams.stream()
        .map(OrderParam::getField)
        .filter(att -> !containsAttributeFilter(filterAttributes, att))
        .map(attributes::get)
        .map(
            at ->
                new QueryItem(
                    at, null, at.getValueType(), at.getAggregationType(), at.getOptionSet()))
        .toList();
  }

  private boolean containsAttributeFilter(List<QueryItem> attributeFilters, String attributeUid) {
    for (QueryItem item : attributeFilters) {
      if (Objects.equals(item.getItem().getUid(), attributeUid)) {
        return true;
      }
    }
    return false;
  }

  private void validateFilterAttributes(List<QueryItem> queryItems) throws BadRequestException {
    Set<String> attributes = new HashSet<>();
    Set<String> duplicates = new HashSet<>();
    for (QueryItem item : queryItems) {
      if (!attributes.add(item.getItemId())) {
        duplicates.add(item.getItemId());
      }
    }

    if (!duplicates.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "filterAttributes contains duplicate tracked entity attribute (TEA): %s. Multiple filters for the same TEA can be specified like 'uid:gt:2:lt:10'",
              String.join(", ", duplicates)));
    }
  }

  private Map<String, SortDirection> getAttributesFromOrder(List<OrderCriteria> allOrders) {
    if (allOrders == null) {
      return Collections.emptyMap();
    }

    Map<String, SortDirection> attributes = new HashMap<>();
    for (OrderCriteria orderCriteria : allOrders) {
      TrackedEntityAttribute attribute =
          trackedEntityAttributeService.getTrackedEntityAttribute(orderCriteria.getField());
      if (attribute != null) {
        attributes.put(orderCriteria.getField(), orderCriteria.getDirection());
      }
    }
    return attributes;
  }

  private List<OrderParam> mapToOrderParams(Map<String, SortDirection> orders) {
    return orders.entrySet().stream().map(e -> new OrderParam(e.getKey(), e.getValue())).toList();
  }

  private Map<String, SortDirection> getDataElementsFromOrder(List<OrderParam> allOrders) {
    if (allOrders == null) {
      return Collections.emptyMap();
    }

    Map<String, SortDirection> dataElements = new HashMap<>();
    for (OrderParam orderParam : allOrders) {
      DataElement de = dataElementService.getDataElement(orderParam.getField());
      if (de != null) {
        dataElements.put(orderParam.getField(), orderParam.getDirection());
      }
    }
    return dataElements;
  }

  private QueryItem dataElementToQueryItem(String item) throws BadRequestException {
    DataElement de = dataElementService.getDataElement(item);

    if (de == null) {
      throw new BadRequestException("Data element does not exist: " + item);
    }

    return new QueryItem(de, null, de.getValueType(), de.getAggregationType(), de.getOptionSet());
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
}
