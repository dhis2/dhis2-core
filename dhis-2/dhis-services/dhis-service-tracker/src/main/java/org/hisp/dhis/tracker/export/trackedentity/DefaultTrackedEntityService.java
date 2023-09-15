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

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.common.Pager.DEFAULT_PAGE_SIZE;
import static org.hisp.dhis.common.SlimPager.FIRST_PAGE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.audit.payloads.TrackedEntityAudit;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityAuditService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.TrackedEntityAggregate;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@Service("org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService")
@RequiredArgsConstructor
class DefaultTrackedEntityService implements TrackedEntityService {

  private final TrackedEntityStore trackedEntityStore;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final TrackedEntityTypeService trackedEntityTypeService;

  private final TrackedEntityAuditService trackedEntityAuditService;

  private final OrganisationUnitService organisationUnitService;

  private final CurrentUserService currentUserService;

  private final TrackerAccessManager trackerAccessManager;

  private final TrackedEntityAggregate trackedEntityAggregate;

  private final AclService aclService;

  private final ProgramService programService;

  private final EnrollmentService enrollmentService;

  private final EventService eventService;

  private final TrackedEntityOperationParamsMapper mapper;

  @Override
  public TrackedEntity getTrackedEntity(
      String uid, TrackedEntityParams params, boolean includeDeleted)
      throws NotFoundException, ForbiddenException {
    TrackedEntity daoTrackedEntity = trackedEntityStore.getByUid(uid);
    addTrackedEntityAudit(daoTrackedEntity, currentUserService.getCurrentUsername());
    if (daoTrackedEntity == null) {
      throw new NotFoundException(TrackedEntity.class, uid);
    }

    return getTrackedEntity(daoTrackedEntity, params, includeDeleted);
  }

  @Override
  public TrackedEntity getTrackedEntity(
      String uid, String programIdentifier, TrackedEntityParams params, boolean includeDeleted)
      throws NotFoundException, ForbiddenException {
    Program program = null;

    if (StringUtils.isNotEmpty(programIdentifier)) {
      program = programService.getProgram(programIdentifier);

      if (program == null) {
        throw new NotFoundException(Program.class, programIdentifier);
      }
    }

    TrackedEntity trackedEntity = getTrackedEntity(uid, params, includeDeleted);

    if (program != null) {
      if (!trackerAccessManager
          .canRead(currentUserService.getCurrentUser(), trackedEntity, program, false)
          .isEmpty()) {
        if (program.getAccessLevel() == AccessLevel.CLOSED) {
          throw new ForbiddenException(TrackerOwnershipManager.PROGRAM_ACCESS_CLOSED);
        }
        throw new ForbiddenException(TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED);
      }

      if (params.isIncludeProgramOwners()) {
        Set<TrackedEntityProgramOwner> filteredProgramOwners =
            trackedEntity.getProgramOwners().stream()
                .filter(te -> te.getProgram().getUid().equals(programIdentifier))
                .collect(Collectors.toSet());
        trackedEntity.setProgramOwners(filteredProgramOwners);
      }
    } else {
      // return only tracked entity type attributes
      TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();
      if (trackedEntityType != null) {
        Set<String> tetAttributes =
            trackedEntityType.getTrackedEntityAttributes().stream()
                .map(TrackedEntityAttribute::getUid)
                .collect(Collectors.toSet());
        Set<TrackedEntityAttributeValue> tetAttributeValues =
            trackedEntity.getTrackedEntityAttributeValues().stream()
                .filter(att -> tetAttributes.contains(att.getAttribute().getUid()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        trackedEntity.setTrackedEntityAttributeValues(tetAttributeValues);
      }
    }

    return trackedEntity;
  }

  @Override
  public TrackedEntity getTrackedEntity(
      @Nonnull TrackedEntity trackedEntity, TrackedEntityParams params, boolean includeDeleted)
      throws ForbiddenException {
    User user = currentUserService.getCurrentUser();
    List<String> errors = trackerAccessManager.canRead(user, trackedEntity);

    if (!errors.isEmpty()) {
      throw new ForbiddenException(errors.toString());
    }

    TrackedEntity result = new TrackedEntity();
    result.setId(trackedEntity.getId());
    result.setUid(trackedEntity.getUid());
    result.setOrganisationUnit(trackedEntity.getOrganisationUnit());
    result.setTrackedEntityType(trackedEntity.getTrackedEntityType());
    result.setCreated(trackedEntity.getCreated());
    result.setCreatedAtClient(trackedEntity.getCreatedAtClient());
    result.setLastUpdated(trackedEntity.getLastUpdated());
    result.setLastUpdatedAtClient(trackedEntity.getLastUpdatedAtClient());
    result.setInactive(trackedEntity.isInactive());
    result.setGeometry(trackedEntity.getGeometry());
    result.setDeleted(trackedEntity.isDeleted());
    result.setPotentialDuplicate(trackedEntity.isPotentialDuplicate());
    result.setStoredBy(trackedEntity.getStoredBy());
    result.setCreatedByUserInfo(trackedEntity.getCreatedByUserInfo());
    result.setLastUpdatedByUserInfo(trackedEntity.getLastUpdatedByUserInfo());
    result.setGeometry(trackedEntity.getGeometry());
    if (params.isIncludeRelationships()) {
      result.setRelationshipItems(getRelationshipItems(trackedEntity, user, includeDeleted));
    }
    if (params.isIncludeEnrollments()) {
      result.setEnrollments(getEnrollments(trackedEntity, user, includeDeleted));
    }
    if (params.isIncludeProgramOwners()) {
      result.setProgramOwners(trackedEntity.getProgramOwners());
    }
    result.setTrackedEntityAttributeValues(getTrackedEntityAttributeValues(trackedEntity, user));

    return result;
  }

  private Set<RelationshipItem> getRelationshipItems(
      TrackedEntity trackedEntity, User user, boolean includeDeleted) {
    Set<RelationshipItem> items = new HashSet<>();

    for (RelationshipItem relationshipItem : trackedEntity.getRelationshipItems()) {
      Relationship daoRelationship = relationshipItem.getRelationship();

      if (trackerAccessManager.canRead(user, daoRelationship).isEmpty()
          && (includeDeleted || !daoRelationship.isDeleted())) {
        items.add(relationshipItem);
      }
    }
    return items;
  }

  private Set<Enrollment> getEnrollments(
      TrackedEntity trackedEntity, User user, boolean includeDeleted) {
    Set<Enrollment> enrollments = new HashSet<>();

    for (Enrollment enrollment : trackedEntity.getEnrollments()) {
      if (trackerAccessManager.canRead(user, enrollment, false).isEmpty()
          && (includeDeleted || !enrollment.isDeleted())) {
        Set<Event> events = new HashSet<>();
        for (Event event : enrollment.getEvents()) {
          if (includeDeleted || !event.isDeleted()) {
            events.add(event);
          }
        }
        enrollment.setEvents(events);
        enrollments.add(enrollment);
      }
    }
    return enrollments;
  }

  private Set<TrackedEntityAttributeValue> getTrackedEntityAttributeValues(
      TrackedEntity trackedEntity, User user) {
    Set<TrackedEntityAttribute> readableAttributes =
        trackedEntityAttributeService.getAllUserReadableTrackedEntityAttributes(user);
    return trackedEntity.getTrackedEntityAttributeValues().stream()
        .filter(av -> readableAttributes.contains(av.getAttribute()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private RelationshipItem withNestedEntity(
      TrackedEntity trackedEntity, RelationshipItem item, boolean includeDeleted)
      throws ForbiddenException, NotFoundException {
    // relationships of relationship items are not mapped to JSON so there is no need to fetch them
    RelationshipItem result = new RelationshipItem();

    if (item.getTrackedEntity() != null) {
      if (trackedEntity.getUid().equals(item.getTrackedEntity().getUid())) {
        // only fetch the TE if we do not already have access to it. meaning the TE owns the item
        // this is just mapping the TE
        result.setTrackedEntity(trackedEntity);
      } else {
        result.setTrackedEntity(
            getTrackedEntity(
                item.getTrackedEntity().getUid(),
                TrackedEntityParams.TRUE.withIncludeRelationships(false),
                includeDeleted));
      }
    } else if (item.getEnrollment() != null) {
      result.setEnrollment(
          enrollmentService.getEnrollment(
              item.getEnrollment().getUid(),
              EnrollmentParams.TRUE.withIncludeRelationships(false),
              false));
    } else if (item.getEvent() != null) {
      result.setEvent(
          eventService.getEvent(
              item.getEvent().getUid(), EventParams.TRUE.withIncludeRelationships(false)));
    }

    return result;
  }

  @Override
  public TrackedEntities getTrackedEntities(TrackedEntityOperationParams operationParams)
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityQueryParams queryParams = mapper.map(operationParams);
    final List<Long> ids = getTrackedEntityIds(queryParams, false, false);

    List<TrackedEntity> trackedEntities =
        this.trackedEntityAggregate.find(
            ids, operationParams.getTrackedEntityParams(), queryParams);

    mapRelationshipItems(
        trackedEntities,
        operationParams.getTrackedEntityParams(),
        operationParams.isIncludeDeleted());

    addSearchAudit(trackedEntities, queryParams.getUser());

    if (operationParams.isSkipPaging()) {
      return TrackedEntities.withoutPagination(trackedEntities);
    }

    Pager pager;

    if (operationParams.isTotalPages()) {
      int count = getTrackedEntityCount(queryParams, true, true);
      pager =
          new Pager(queryParams.getPageWithDefault(), count, queryParams.getPageSizeWithDefault());
    } else {
      pager = handleLastPageFlag(operationParams, trackedEntities);
    }

    return TrackedEntities.of(trackedEntities, pager);
  }

  public List<Long> getTrackedEntityIds(
      TrackedEntityQueryParams params,
      boolean skipAccessValidation,
      boolean skipSearchScopeValidation) {
    if (!params.hasProgram()) {
      Collection<TrackedEntityAttribute> attributes =
          trackedEntityAttributeService.getTrackedEntityAttributesDisplayInListNoProgram();
      attributes.forEach(params::filterBy);
    }

    decideAccess(params);

    // AccessValidation should be skipped only and only if it is internal
    // service that runs the task (for example sync job)
    if (!skipAccessValidation) {
      validate(params);
    }

    if (!skipSearchScopeValidation) {
      validateSearchScope(params);
    }

    return trackedEntityStore.getTrackedEntityIds(params);
  }

  public void decideAccess(TrackedEntityQueryParams params) {
    if (params.isOrganisationUnitMode(ALL)
        && !currentUserService.currentUserIsAuthorized(
            Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name())) {
      throw new IllegalQueryException(
          "Current user is not authorized to query across all organisation units");
    }

    User user = params.getUser();
    if (params.hasProgram()) {
      if (!aclService.canDataRead(user, params.getProgram())) {
        throw new IllegalQueryException(
            "Current user is not authorized to read data from selected program:  "
                + params.getProgram().getUid());
      }

      if (params.getProgram().getTrackedEntityType() != null
          && !aclService.canDataRead(user, params.getProgram().getTrackedEntityType())) {
        throw new IllegalQueryException(
            "Current user is not authorized to read data from selected program's tracked entity type:  "
                + params.getProgram().getTrackedEntityType().getUid());
      }
    }

    if (params.hasTrackedEntityType()
        && !aclService.canDataRead(user, params.getTrackedEntityType())) {
      throw new IllegalQueryException(
          "Current user is not authorized to read data from selected tracked entity type:  "
              + params.getTrackedEntityType().getUid());
    } else {
      params.setTrackedEntityTypes(
          trackedEntityTypeService.getAllTrackedEntityType().stream()
              .filter(tet -> aclService.canDataRead(user, tet))
              .collect(Collectors.toList()));
    }
  }

  public void validate(TrackedEntityQueryParams params) throws IllegalQueryException {
    String violation = null;

    if (params == null) {
      throw new IllegalQueryException("Params cannot be null");
    }

    if (params.hasProgram() && params.hasTrackedEntityType()) {
      violation = "Program and tracked entity cannot be specified simultaneously";
    }

    if (!params.hasTrackedEntities() && !params.hasProgram() && !params.hasTrackedEntityType()) {
      violation = "Either Program or Tracked entity type should be specified";
    }

    if (params.hasProgramStatus() && !params.hasProgram()) {
      violation = "Program must be defined when program status is defined";
    }

    if (params.hasFollowUp() && !params.hasProgram()) {
      violation = "Program must be defined when follow up status is defined";
    }

    if (params.hasProgramEnrollmentStartDate() && !params.hasProgram()) {
      violation = "Program must be defined when program enrollment start date is specified";
    }

    if (params.hasProgramEnrollmentEndDate() && !params.hasProgram()) {
      violation = "Program must be defined when program enrollment end date is specified";
    }

    if (params.hasProgramIncidentStartDate() && !params.hasProgram()) {
      violation = "Program must be defined when program incident start date is specified";
    }

    if (params.hasProgramIncidentEndDate() && !params.hasProgram()) {
      violation = "Program must be defined when program incident end date is specified";
    }

    if (params.hasEventStatus() && (!params.hasEventStartDate() || !params.hasEventEndDate())) {
      violation = "Event start and end date must be specified when event status is specified";
    }

    if (params.hasLastUpdatedDuration()
        && (params.hasLastUpdatedStartDate() || params.hasLastUpdatedEndDate())) {
      violation =
          "Last updated from and/or to and last updated duration cannot be specified simultaneously";
    }

    if (params.hasLastUpdatedDuration()
        && DateUtils.getDuration(params.getLastUpdatedDuration()) == null) {
      violation = "Duration is not valid: " + params.getLastUpdatedDuration();
    }

    if (violation != null) {
      log.warn("Validation failed: " + violation);

      throw new IllegalQueryException(violation);
    }
  }

  public void validateSearchScope(TrackedEntityQueryParams params) throws IllegalQueryException {
    if (params == null) {
      throw new IllegalQueryException("Params cannot be null");
    }

    User user = currentUserService.getCurrentUser();

    if (user == null) {
      throw new IllegalQueryException("User cannot be null");
    }

    if (!user.isSuper() && user.getOrganisationUnits().isEmpty()) {
      throw new IllegalQueryException(
          "User need to be associated with at least one organisation unit.");
    }

    if (!params.hasProgram()
        && !params.hasTrackedEntityType()
        && params.hasFilters()
        && !params.hasAccessibleOrgUnits()) {
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

    if (!isLocalSearch(params, user)) {
      int maxTeiLimit = 0; // no limit

      if (params.hasProgram() && params.hasTrackedEntityType()) {
        throw new IllegalQueryException(
            "Program and tracked entity cannot be specified simultaneously");
      }

      if (params.hasFilters()) {
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

      checkIfMaxTeiLimitIsReached(params, maxTeiLimit);
      params.setMaxTeLimit(maxTeiLimit);
    }
  }

  private boolean isLocalSearch(TrackedEntityQueryParams params, User user) {
    Set<OrganisationUnit> localOrgUnits = user.getOrganisationUnits();

    Set<OrganisationUnit> searchOrgUnits = new HashSet<>();

    if (params.isOrganisationUnitMode(SELECTED)) {
      searchOrgUnits = params.getAccessibleOrgUnits();
    } else if (params.isOrganisationUnitMode(CHILDREN)
        || params.isOrganisationUnitMode(DESCENDANTS)) {
      for (OrganisationUnit orgUnit : params.getAccessibleOrgUnits()) {
        searchOrgUnits.addAll(orgUnit.getChildren());
      }
    } else if (params.isOrganisationUnitMode(ALL)) {
      searchOrgUnits.addAll(organisationUnitService.getRootOrganisationUnits());
    } else {
      searchOrgUnits.addAll(user.getTeiSearchOrganisationUnitsWithFallback());
    }

    for (OrganisationUnit ou : searchOrgUnits) {
      if (!organisationUnitService.isDescendant(ou, localOrgUnits)) {
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

  public int getTrackedEntityCount(
      TrackedEntityQueryParams params,
      boolean skipAccessValidation,
      boolean skipSearchScopeValidation) {
    decideAccess(params);

    if (!skipAccessValidation) {
      validate(params);
    }

    if (!skipSearchScopeValidation) {
      validateSearchScope(params);
    }

    // using countForGrid here to leverage the better performant rewritten
    // sql query
    return trackedEntityStore.getTrackedEntityCount(params);
  }

  /**
   * We need to return the full models for relationship items (i.e. trackedEntity, enrollment and
   * event) in our API. The aggregate stores currently do not support that, so we need to fetch the
   * entities individually.
   */
  private void mapRelationshipItems(
      List<TrackedEntity> trackedEntities, TrackedEntityParams params, boolean includeDeleted)
      throws ForbiddenException, NotFoundException {
    if (params.isIncludeRelationships()) {
      for (TrackedEntity trackedEntity : trackedEntities) {
        mapRelationshipItems(trackedEntity, includeDeleted);
      }
    }
    if (params.getEnrollmentParams().isIncludeRelationships()) {
      for (TrackedEntity trackedEntity : trackedEntities) {
        for (Enrollment enrollment : trackedEntity.getEnrollments()) {
          mapRelationshipItems(enrollment, trackedEntity, includeDeleted);
        }
      }
    }
    if (params.getEventParams().isIncludeRelationships()) {
      for (TrackedEntity trackedEntity : trackedEntities) {
        for (Enrollment enrollment : trackedEntity.getEnrollments()) {
          for (Event event : enrollment.getEvents()) {
            mapRelationshipItems(event, trackedEntity, includeDeleted);
          }
        }
      }
    }
  }

  private void mapRelationshipItems(TrackedEntity trackedEntity, boolean includeDeleted)
      throws ForbiddenException, NotFoundException {
    Set<RelationshipItem> result = new HashSet<>();

    for (RelationshipItem item : trackedEntity.getRelationshipItems()) {
      result.add(mapRelationshipItem(item, trackedEntity, trackedEntity, includeDeleted));
    }

    trackedEntity.setRelationshipItems(result);
  }

  private void mapRelationshipItems(
      Enrollment enrollment, TrackedEntity trackedEntity, boolean includeDeleted)
      throws ForbiddenException, NotFoundException {
    Set<RelationshipItem> result = new HashSet<>();

    for (RelationshipItem item : enrollment.getRelationshipItems()) {
      result.add(mapRelationshipItem(item, enrollment, trackedEntity, includeDeleted));
    }

    enrollment.setRelationshipItems(result);
  }

  private void mapRelationshipItems(
      Event event, TrackedEntity trackedEntity, boolean includeDeleted)
      throws ForbiddenException, NotFoundException {
    Set<RelationshipItem> result = new HashSet<>();

    for (RelationshipItem item : event.getRelationshipItems()) {
      result.add(mapRelationshipItem(item, event, trackedEntity, includeDeleted));
    }

    event.setRelationshipItems(result);
  }

  private RelationshipItem mapRelationshipItem(
      RelationshipItem item,
      BaseIdentifiableObject itemOwner,
      TrackedEntity trackedEntity,
      boolean includeDeleted)
      throws ForbiddenException, NotFoundException {
    Relationship rel = item.getRelationship();
    RelationshipItem from = withNestedEntity(trackedEntity, rel.getFrom(), includeDeleted);
    from.setRelationship(rel);
    rel.setFrom(from);
    RelationshipItem to = withNestedEntity(trackedEntity, rel.getTo(), includeDeleted);
    to.setRelationship(rel);
    rel.setTo(to);

    if (rel.getFrom().getTrackedEntity() != null
        && itemOwner.getUid().equals(rel.getFrom().getTrackedEntity().getUid())) {
      return from;
    }

    return to;
  }

  private void addSearchAudit(List<TrackedEntity> trackedEntities, User user) {
    if (trackedEntities.isEmpty()) {
      return;
    }
    final String accessedBy =
        user != null ? user.getUsername() : currentUserService.getCurrentUsername();
    Map<String, TrackedEntityType> tetMap =
        trackedEntityTypeService.getAllTrackedEntityType().stream()
            .collect(Collectors.toMap(TrackedEntityType::getUid, t -> t));

    List<TrackedEntityAudit> auditable =
        trackedEntities.stream()
            .filter(Objects::nonNull)
            .filter(te -> te.getTrackedEntityType() != null)
            .filter(te -> tetMap.get(te.getTrackedEntityType().getUid()).isAllowAuditLog())
            .map(te -> new TrackedEntityAudit(te.getUid(), accessedBy, AuditType.SEARCH))
            .toList();

    if (!auditable.isEmpty()) {
      trackedEntityAuditService.addTrackedEntityAudit(auditable);
    }
  }

  private void addTrackedEntityAudit(TrackedEntity trackedEntity, String user) {
    if (user != null
        && trackedEntity != null
        && trackedEntity.getTrackedEntityType() != null
        && trackedEntity.getTrackedEntityType().isAllowAuditLog()) {
      TrackedEntityAudit trackedEntityAudit =
          new TrackedEntityAudit(trackedEntity.getUid(), user, AuditType.READ);
      trackedEntityAuditService.addTrackedEntityAudit(trackedEntityAudit);
    }
  }

  /**
   * This method will apply the logic related to the parameter 'totalPages=false'. This works in
   * conjunction with the method: {@link
   * TrackedEntityStore#getTrackedEntityIds(TrackedEntityQueryParams)}
   *
   * <p>This is needed because we need to query (pageSize + 1) at DB level. The resulting query will
   * allow us to evaluate if we are in the last page or not. And this is what his method does,
   * returning the respective Pager object.
   *
   * @param params the request params
   * @param trackedEntityList the reference to the list of Tracked Entities
   * @return the populated SlimPager instance
   */
  private Pager handleLastPageFlag(
      TrackedEntityOperationParams params, List<TrackedEntity> trackedEntityList) {
    Integer originalPage = defaultIfNull(params.getPage(), FIRST_PAGE);
    Integer originalPageSize = defaultIfNull(params.getPageSize(), DEFAULT_PAGE_SIZE);
    boolean isLastPage = false;

    if (isNotEmpty(trackedEntityList)) {
      isLastPage = trackedEntityList.size() <= originalPageSize;
      if (!isLastPage) {
        // Get the same number of elements of the pageSize, forcing
        // the removal of the last additional element added at querying
        // time.
        trackedEntityList.retainAll(trackedEntityList.subList(0, originalPageSize));
      }
    }

    return new SlimPager(originalPage, originalPageSize, isLastPage);
  }

  @Override
  public Set<String> getOrderableFields() {
    return trackedEntityStore.getOrderableFields();
  }
}
