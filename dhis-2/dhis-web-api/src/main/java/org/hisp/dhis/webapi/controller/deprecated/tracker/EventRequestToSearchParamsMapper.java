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
package org.hisp.dhis.webapi.controller.deprecated.tracker;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.security.Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS;
import static org.hisp.dhis.webapi.controller.deprecated.tracker.EventUtils.getOrgUnitMode;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component("org.hisp.dhis.webapi.controller.deprecated.tracker.EventRequestToSearchParamsMapper")
@RequiredArgsConstructor
class EventRequestToSearchParamsMapper {
  private final CurrentUserService currentUserService;

  private final ProgramService programService;

  private final OrganisationUnitService organisationUnitService;

  private final ProgramStageService programStageService;

  private final AclService aclService;

  private final TrackedEntityService entityInstanceService;

  private final DataElementService dataElementService;

  private final InputUtils inputUtils;

  private final SchemaService schemaService;

  private Schema schema;

  @PostConstruct
  void setSchema() {
    if (schema == null) {
      schema = schemaService.getDynamicSchema(Event.class);
    }
  }

  public EventSearchParams map(
      String program,
      String programStage,
      ProgramStatus programStatus,
      Boolean followUp,
      String orgUnit,
      OrganisationUnitSelectionMode orgUnitSelectionMode,
      String trackedEntityInstance,
      Date startDate,
      Date endDate,
      Date dueDateStart,
      Date dueDateEnd,
      Date lastUpdatedStartDate,
      Date lastUpdatedEndDate,
      String lastUpdatedDuration,
      EventStatus status,
      CategoryOptionCombo attributeOptionCombo,
      IdSchemes idSchemes,
      Integer page,
      Integer pageSize,
      boolean totalPages,
      boolean skipPaging,
      List<OrderParam> orders,
      List<OrderParam> gridOrders,
      boolean includeAttributes,
      Set<String> events,
      Boolean skipEventId,
      AssignedUserSelectionMode assignedUserSelectionMode,
      Set<String> assignedUsers,
      Set<String> filters,
      Set<String> dataElements,
      boolean includeAllDataElements,
      boolean includeDeleted) {
    return map(
        program,
        programStage,
        programStatus,
        followUp,
        orgUnit,
        orgUnitSelectionMode,
        trackedEntityInstance,
        startDate,
        endDate,
        dueDateStart,
        dueDateEnd,
        lastUpdatedStartDate,
        lastUpdatedEndDate,
        lastUpdatedDuration,
        status,
        attributeOptionCombo,
        idSchemes,
        page,
        pageSize,
        totalPages,
        skipPaging,
        orders,
        gridOrders,
        includeAttributes,
        events,
        null,
        skipEventId,
        assignedUserSelectionMode,
        assignedUsers,
        filters,
        dataElements,
        includeAllDataElements,
        includeDeleted);
  }

  public EventSearchParams map(
      String program,
      String programStage,
      ProgramStatus programStatus,
      Boolean followUp,
      String orgUnit,
      OrganisationUnitSelectionMode orgUnitSelectionMode,
      String trackedEntityInstance,
      Date startDate,
      Date endDate,
      Date dueDateStart,
      Date dueDateEnd,
      Date lastUpdatedStartDate,
      Date lastUpdatedEndDate,
      String lastUpdatedDuration,
      EventStatus status,
      CategoryOptionCombo attributeOptionCombo,
      IdSchemes idSchemes,
      Integer page,
      Integer pageSize,
      boolean totalPages,
      boolean skipPaging,
      List<OrderParam> orders,
      List<OrderParam> gridOrders,
      boolean includeAttributes,
      Set<String> events,
      Set<String> enrollments,
      Boolean skipEventId,
      AssignedUserSelectionMode assignedUserSelectionMode,
      Set<String> assignedUsers,
      Set<String> filters,
      Set<String> dataElements,
      boolean includeAllDataElements,
      boolean includeDeleted) {
    User user = currentUserService.getCurrentUser();

    EventSearchParams params = new EventSearchParams();

    Program pr = programService.getProgram(program);

    if (!StringUtils.isEmpty(program) && pr == null) {
      throw new IllegalQueryException("Program is specified but does not exist: " + program);
    }

    ProgramStage ps = programStageService.getProgramStage(programStage);

    if (!StringUtils.isEmpty(programStage) && ps == null) {
      throw new IllegalQueryException(
          "Program stage is specified but does not exist: " + programStage);
    }

    OrganisationUnit requestedOrgUnit = organisationUnitService.getOrganisationUnit(orgUnit);

    if (!StringUtils.isEmpty(orgUnit) && requestedOrgUnit == null) {
      throw new IllegalQueryException("Org unit is specified but does not exist: " + orgUnit);
    }

    validateUser(user, pr, ps, requestedOrgUnit);

    OrganisationUnitSelectionMode orgUnitMode =
        getOrgUnitMode(requestedOrgUnit, orgUnitSelectionMode);

    validateOrgUnitMode(orgUnitMode, user, pr, requestedOrgUnit);

    TrackedEntity tei = entityInstanceService.getTrackedEntity(trackedEntityInstance);

    if (!StringUtils.isEmpty(trackedEntityInstance) && tei == null) {
      throw new IllegalQueryException(
          "Tracked entity instance is specified but does not exist: " + trackedEntityInstance);
    }

    if (attributeOptionCombo != null
        && !user.isSuper()
        && !aclService.canDataRead(user, attributeOptionCombo)) {
      throw new IllegalQueryException(
          "User has no access to attribute category option combo: "
              + attributeOptionCombo.getUid());
    }

    if (!CollectionUtils.isEmpty(events) && !CollectionUtils.isEmpty(filters)) {
      throw new IllegalQueryException(
          "Event UIDs and filters can not be specified at the same time");
    }

    if (events == null) {
      events = new HashSet<>();
    }

    if (filters != null) {
      if (!StringUtils.isEmpty(programStage) && ps == null) {
        throw new IllegalQueryException(
            "ProgramStage needs to be specified for event filtering to work");
      }

      for (String filter : filters) {
        params.addFilter(getQueryItem(filter));
      }
    }

    if (dataElements != null) {
      for (String de : dataElements) {
        QueryItem dataElement = getQueryItem(de);

        params.getDataElements().add(dataElement);
      }
    }

    if (assignedUsers != null) {
      assignedUsers =
          assignedUsers.stream().filter(CodeGenerator::isValidUid).collect(Collectors.toSet());
    }

    if (enrollments != null) {
      enrollments =
          enrollments.stream().filter(CodeGenerator::isValidUid).collect(Collectors.toSet());
    }

    return params
        .setProgram(pr)
        .setProgramStage(ps)
        .setOrgUnit(requestedOrgUnit)
        .setTrackedEntity(tei)
        .setProgramStatus(programStatus)
        .setFollowUp(followUp)
        .setOrgUnitSelectionMode(orgUnitMode)
        .setUserWithAssignedUsers(assignedUserSelectionMode, user, assignedUsers)
        .setStartDate(startDate)
        .setEndDate(endDate)
        .setDueDateStart(dueDateStart)
        .setDueDateEnd(dueDateEnd)
        .setLastUpdatedStartDate(lastUpdatedStartDate)
        .setLastUpdatedEndDate(lastUpdatedEndDate)
        .setLastUpdatedDuration(lastUpdatedDuration)
        .setEventStatus(status)
        .setCategoryOptionCombo(attributeOptionCombo)
        .setIdSchemes(idSchemes)
        .setPage(page)
        .setPageSize(pageSize)
        .setTotalPages(totalPages)
        .setSkipPaging(skipPaging)
        .setSkipEventId(skipEventId)
        .setIncludeAttributes(includeAttributes)
        .setIncludeAllDataElements(includeAllDataElements)
        .addOrders(orders)
        .addGridOrders(gridOrders)
        .setEvents(events)
        .setEnrollments(enrollments)
        .setIncludeDeleted(includeDeleted);
  }

  private QueryItem getQueryItem(String item) {
    String[] split = item.split(DimensionalObject.DIMENSION_NAME_SEP);

    if (split == null || split.length % 2 != 1) {
      throw new IllegalQueryException("Query item or filter is invalid: " + item);
    }

    QueryItem queryItem = getItem(split[0]);

    if (split.length > 1) {
      for (int i = 1; i < split.length; i += 2) {
        QueryOperator operator = QueryOperator.fromString(split[i]);
        queryItem.getFilters().add(new QueryFilter(operator, split[i + 1]));
      }
    }

    return queryItem;
  }

  private QueryItem getItem(String item) {
    DataElement de = dataElementService.getDataElement(item);

    if (de == null) {
      throw new IllegalQueryException("Dataelement does not exist: " + item);
    }

    return new QueryItem(de, null, de.getValueType(), de.getAggregationType(), de.getOptionSet());
  }

  public EventSearchParams map(EventCriteria eventCriteria) {

    CategoryOptionCombo attributeOptionCombo =
        inputUtils.getAttributeOptionCombo(
            eventCriteria.getAttributeCc(), eventCriteria.getAttributeCos(), true);

    Set<String> eventIds = eventCriteria.getEvents();
    Set<String> assignedUserIds = eventCriteria.getAssignedUsers();
    Map<String, SortDirection> dataElementOrders =
        getDataElementsFromOrder(eventCriteria.getOrder());

    return map(
        eventCriteria.getProgram(),
        eventCriteria.getProgramStage(),
        eventCriteria.getProgramStatus(),
        eventCriteria.getFollowUp(),
        eventCriteria.getOrgUnit(),
        eventCriteria.getOuMode(),
        eventCriteria.getTrackedEntityInstance(),
        eventCriteria.getStartDate(),
        eventCriteria.getEndDate(),
        eventCriteria.getDueDateStart(),
        eventCriteria.getDueDateEnd(),
        eventCriteria.getLastUpdatedStartDate() != null
            ? eventCriteria.getLastUpdatedStartDate()
            : eventCriteria.getLastUpdated(),
        eventCriteria.getLastUpdatedEndDate(),
        eventCriteria.getLastUpdatedDuration(),
        eventCriteria.getStatus(),
        attributeOptionCombo,
        eventCriteria.getIdSchemes(),
        eventCriteria.getPage(),
        eventCriteria.getPageSize(),
        eventCriteria.isTotalPages(),
        toBooleanDefaultIfNull(eventCriteria.isSkipPaging(), false),
        getOrderParams(eventCriteria.getOrder()),
        getGridOrderParams(eventCriteria.getOrder(), dataElementOrders),
        false,
        eventIds,
        eventCriteria.getEnrollments(),
        eventCriteria.getSkipEventId(),
        eventCriteria.getAssignedUserMode(),
        assignedUserIds,
        eventCriteria.getFilter(),
        dataElementOrders.keySet(),
        false,
        eventCriteria.isIncludeDeleted());
  }

  private List<OrderParam> getOrderParams(List<OrderCriteria> order) {
    if (order != null && !order.isEmpty()) {
      return QueryUtils.filteredBySchema(order, schema);
    }
    return Collections.emptyList();
  }

  private List<OrderParam> getGridOrderParams(
      List<OrderCriteria> order, Map<String, SortDirection> dataElementOrders) {
    return Optional.ofNullable(order).orElse(Collections.emptyList()).stream()
        .filter(Objects::nonNull)
        .filter(orderCriteria -> dataElementOrders.containsKey(orderCriteria.getField()))
        .map(
            orderCriteria ->
                new OrderParam(
                    orderCriteria.getField(), dataElementOrders.get(orderCriteria.getField())))
        .collect(Collectors.toList());
  }

  private Map<String, SortDirection> getDataElementsFromOrder(List<OrderCriteria> allOrders) {
    Map<String, SortDirection> dataElements = new HashMap<>();

    if (allOrders != null) {
      for (OrderCriteria orderCriteria : allOrders) {
        DataElement de = dataElementService.getDataElement(orderCriteria.getField());
        if (de != null) {
          dataElements.put(orderCriteria.getField(), orderCriteria.getDirection());
        }
      }
    }
    return dataElements;
  }

  private void validateOrgUnitMode(
      OrganisationUnitSelectionMode selectedOuMode,
      User user,
      Program program,
      OrganisationUnit orgUnit) {

    switch (selectedOuMode) {
      case ALL -> validateUserCanSearchOrgUnitModeALL(user);
      case SELECTED, ACCESSIBLE, DESCENDANTS, CHILDREN -> validateUserScope(
          user, program, selectedOuMode, orgUnit);
      case CAPTURE -> validateCaptureScope(user, orgUnit);
    }
  }

  private void validateUser(
      User user, Program program, ProgramStage programStage, OrganisationUnit requestedOrgUnit) {
    if (user == null
        || user.isSuper()
        || user.isAuthorized(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS)) {
      return;
    }

    if (program != null && !user.isSuper() && !aclService.canDataRead(user, program)) {
      throw new IllegalQueryException("User has no access to program: " + program.getUid());
    }

    if (programStage != null && !user.isSuper() && !aclService.canDataRead(user, programStage)) {
      throw new IllegalQueryException(
          "User has no access to program stage: " + programStage.getUid());
    }

    if (requestedOrgUnit != null
        && !organisationUnitService.isInUserHierarchy(
            requestedOrgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback())) {
      throw new IllegalQueryException(
          "Organisation unit is not part of your search scope: " + requestedOrgUnit.getUid());
    }
  }

  private static void validateUserCanSearchOrgUnitModeALL(User user) {
    if (user != null
        && !(user.isSuper()
            || user.isAuthorized(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name()))) {
      throw new IllegalQueryException(
          "Current user is not authorized to query across all organisation units");
    }
  }

  private static void validateUserScope(
      User user,
      Program program,
      OrganisationUnitSelectionMode orgUnitMode,
      OrganisationUnit orgUnit) {

    if (user == null) {
      throw new IllegalQueryException("User is required for orgUnitMode: " + orgUnitMode);
    }

    if (program != null && (program.isClosed() || program.isProtected())) {
      if (user.getOrganisationUnits().isEmpty()) {
        throw new IllegalQueryException("User needs to be assigned data capture org units");
      }

    } else if (user.getTeiSearchOrganisationUnitsWithFallback().isEmpty()) {
      throw new IllegalQueryException(
          "User needs to be assigned either search or data capture org units");
    }

    if (orgUnitMode == ACCESSIBLE) {
      if (orgUnit != null) {
        throw new IllegalQueryException(
            "ouMode ACCESSIBLE cannot be used with orgUnits. Please remove the orgUnit parameter and try again.");
      }
    } else if (orgUnit == null) {
      throw new IllegalQueryException(
          String.format(
              "At least one org unit is required for orgUnitMode: %s. Please add an orgUnit or use a different orgUnitMode.",
              orgUnitMode));
    }
  }

  private static void validateCaptureScope(User user, OrganisationUnit orgUnit) {
    if (user == null) {
      throw new IllegalQueryException("User is required for orgUnitMode: " + CAPTURE);

    } else if (user.getOrganisationUnits().isEmpty()) {
      throw new IllegalQueryException("User needs to be assigned data capture orgunits");
    }

    if (orgUnit != null) {
      throw new IllegalQueryException(
          "ouMode CAPTURE cannot be used with orgUnits. Please remove the orgUnit parameter and try again.");
    }
  }
}
