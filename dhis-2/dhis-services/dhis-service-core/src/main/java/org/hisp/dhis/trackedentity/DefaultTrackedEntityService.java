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
package org.hisp.dhis.trackedentity;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueChangeLogService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew Gizaw
 */
@Slf4j
@Service("org.hisp.dhis.trackedentity.TrackedEntityService")
public class DefaultTrackedEntityService implements TrackedEntityService {
  private final TrackedEntityStore trackedEntityStore;

  private final TrackedEntityAttributeValueService attributeValueService;

  private final TrackedEntityAttributeService attributeService;

  private final TrackedEntityTypeService trackedEntityTypeService;

  private final OrganisationUnitService organisationUnitService;

  private final AclService aclService;

  private final TrackedEntityChangeLogService trackedEntityChangeLogService;

  private final TrackedEntityAttributeValueChangeLogService attributeValueAuditService;

  private final UserService userService;

  // TODO: FIXME luciano using @Lazy here because we have circular
  // dependencies:
  // TrackedEntityService --> TrackedEntityProgramOwnerService --> TrackedEntityService
  public DefaultTrackedEntityService(
      UserService userService,
      TrackedEntityStore trackedEntityStore,
      TrackedEntityAttributeValueService attributeValueService,
      TrackedEntityAttributeService attributeService,
      TrackedEntityTypeService trackedEntityTypeService,
      OrganisationUnitService organisationUnitService,
      AclService aclService,
      @Lazy TrackedEntityChangeLogService trackedEntityChangeLogService,
      @Lazy TrackedEntityAttributeValueChangeLogService attributeValueAuditService) {
    checkNotNull(trackedEntityStore);
    checkNotNull(attributeValueService);
    checkNotNull(attributeService);
    checkNotNull(trackedEntityTypeService);
    checkNotNull(organisationUnitService);
    checkNotNull(aclService);
    checkNotNull(trackedEntityChangeLogService);
    checkNotNull(attributeValueAuditService);

    this.userService = userService;
    this.trackedEntityStore = trackedEntityStore;
    this.attributeValueService = attributeValueService;
    this.attributeService = attributeService;
    this.trackedEntityTypeService = trackedEntityTypeService;
    this.organisationUnitService = organisationUnitService;
    this.aclService = aclService;
    this.trackedEntityChangeLogService = trackedEntityChangeLogService;
    this.attributeValueAuditService = attributeValueAuditService;
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntity> getTrackedEntities(
      TrackedEntityQueryParams params,
      boolean skipAccessValidation,
      boolean skipSearchScopeValidation) {
    if (params.isOrQuery() && !params.hasAttributes() && !params.hasProgram()) {
      Collection<TrackedEntityAttribute> attributes =
          attributeService.getTrackedEntityAttributesDisplayInListNoProgram();
      params.addAttributes(QueryItem.getQueryItems(attributes));
      params.addFiltersIfNotExist(QueryItem.getQueryItems(attributes));
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

    List<TrackedEntity> trackedEntities = trackedEntityStore.getTrackedEntities(params);

    User user = params.getUser();
    trackedEntities =
        trackedEntities.stream()
            .filter((te) -> aclService.canDataRead(user, te.getTrackedEntityType()))
            .collect(Collectors.toList());

    // Avoiding NullPointerException
    String accessedBy = user != null ? user.getUsername() : CurrentUserUtil.getCurrentUsername();

    for (TrackedEntity te : trackedEntities) {
      addTrackedEntityAudit(te, accessedBy, ChangeLogType.SEARCH);
    }

    return trackedEntities;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Long> getTrackedEntityIds(
      TrackedEntityQueryParams params,
      boolean skipAccessValidation,
      boolean skipSearchScopeValidation) {
    if (params.isOrQuery() && !params.hasAttributes() && !params.hasProgram()) {
      Collection<TrackedEntityAttribute> attributes =
          attributeService.getTrackedEntityAttributesDisplayInListNoProgram();
      params.addAttributes(QueryItem.getQueryItems(attributes));
      params.addFiltersIfNotExist(QueryItem.getQueryItems(attributes));
    }

    handleSortAttributes(params);

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

  /**
   * This method handles any dynamic sort order columns in the params. These have to be added to the
   * attribute list if neither are present in the attribute list nor the filter list.
   *
   * <p>For example, if attributes or filters don't have a specific trackedentityattribute uid, but
   * sorting has been requested for that tea uid, then we need to add them to the attribute list.
   */
  private void handleSortAttributes(TrackedEntityQueryParams params) {
    List<TrackedEntityAttribute> sortAttributes =
        params.getOrders().stream()
            .map(OrderParam::getField)
            .filter(this::isDynamicColumn)
            .map(attributeService::getTrackedEntityAttribute)
            .collect(Collectors.toList());

    params.addAttributesIfNotExist(
        QueryItem.getQueryItems(sortAttributes).stream()
            .filter(sAtt -> !params.getFilters().contains(sAtt))
            .collect(Collectors.toList()));
  }

  public boolean isDynamicColumn(String propName) {
    return Arrays.stream(TrackedEntityQueryParams.OrderColumn.values())
        .noneMatch(orderColumn -> orderColumn.getPropName().equals(propName));
  }

  // TODO lower index on attribute value?
  private void decideAccess(TrackedEntityQueryParams params) {
    User user = params.getUser();
    if (params.isOrganisationUnitMode(ALL)
        && !(user != null
            && user.isAuthorized(Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS))) {
      throw new IllegalQueryException(
          "Current user is not authorized to query across all organisation units");
    }

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

  private void validate(TrackedEntityQueryParams params) throws IllegalQueryException {
    if (params == null) {
      throw new IllegalQueryException("Params cannot be null");
    }

    String violation = null;
    User user = params.getUser();
    if (!params.hasTrackedEntities()
        && !params.hasOrganisationUnits()
        && !(params.isOrganisationUnitMode(ALL)
            || params.isOrganisationUnitMode(ACCESSIBLE)
            || params.isOrganisationUnitMode(CAPTURE))) {
      violation = "At least one organisation unit must be specified";
    }

    if (params.isOrganisationUnitMode(ACCESSIBLE)
        && (user == null || !user.hasDataViewOrganisationUnitWithFallback())) {
      violation =
          "Current user must be associated with at least one organisation unit when selection mode is ACCESSIBLE";
    }

    if (params.isOrganisationUnitMode(CAPTURE) && (user == null || !user.hasOrganisationUnit())) {
      violation =
          "Current user must be associated with at least one organisation unit with write access when selection mode is CAPTURE";
    }

    if (params.hasProgram() && params.hasTrackedEntityType()) {
      violation = "Program and tracked entity cannot be specified simultaneously";
    }

    if (!params.hasTrackedEntities() && !params.hasProgram() && !params.hasTrackedEntityType()) {
      violation = "Either Program or Tracked entity type should be specified";
    }

    if (params.hasEnrollmentStatus() && !params.hasProgram()) {
      violation = "Program must be defined when enrollment status is defined";
    }

    if (params.hasFollowUp() && !params.hasProgram()) {
      violation = "Program must be defined when follow up status is defined";
    }

    if (params.isOrQuery() && params.hasFilters()) {
      violation = "Query cannot be specified together with filters";
    }

    if (!params.getDuplicateAttributes().isEmpty()) {
      violation =
          "Attributes cannot be specified more than once: " + params.getDuplicateAttributes();
    }

    if (!params.getDuplicateFilters().isEmpty()) {
      violation = "Filters cannot be specified more than once: " + params.getDuplicateFilters();
    }

    if (params.hasLastUpdatedDuration()
        && DateUtils.getDuration(params.getLastUpdatedDuration()) == null) {
      violation = "Duration is not valid: " + params.getLastUpdatedDuration();
    }

    if (violation != null) {
      log.warn("Validation failed: {}", violation);

      throw new IllegalQueryException(violation);
    }
  }

  private void validateSearchScope(TrackedEntityQueryParams params) throws IllegalQueryException {
    if (params == null) {
      throw new IllegalQueryException("Params cannot be null");
    }

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();

    if (currentUserDetails == null) {
      throw new IllegalQueryException("User cannot be null");
    }

    if (!currentUserDetails.isSuper() && currentUserDetails.getUserOrgUnitIds().isEmpty()) {
      throw new IllegalQueryException(
          "User need to be associated with at least one organisation unit.");
    }

    if (!params.hasProgram()
        && !params.hasTrackedEntityType()
        && params.hasAttributesOrFilters()
        && !params.hasOrganisationUnits()) {
      List<String> uniqueAttributeIds =
          attributeService.getAllSystemWideUniqueTrackedEntityAttributes().stream()
              .map(TrackedEntityAttribute::getUid)
              .toList();

      for (String att : params.getAttributeAndFilterIds()) {
        if (!uniqueAttributeIds.contains(att)) {
          throw new IllegalQueryException(
              "Either a program or tracked entity type must be specified");
        }
      }
    }

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    if (!isLocalSearch(params, currentUser)) {
      int maxTeiLimit = 0; // no limit

      if (params.hasQuery()) {
        throw new IllegalQueryException("Query cannot be used during global search");
      }

      if (params.hasProgram() && params.hasTrackedEntityType()) {
        throw new IllegalQueryException(
            "Program and tracked entity cannot be specified simultaneously");
      }

      if (params.hasAttributesOrFilters()) {
        List<String> searchableAttributeIds = new ArrayList<>();

        if (params.hasProgram()) {
          searchableAttributeIds.addAll(params.getProgram().getSearchableAttributeIds());
        }

        if (params.hasTrackedEntityType()) {
          searchableAttributeIds.addAll(params.getTrackedEntityType().getSearchableAttributeIds());
        }

        if (!params.hasProgram() && !params.hasTrackedEntityType()) {
          searchableAttributeIds.addAll(
              attributeService.getAllSystemWideUniqueTrackedEntityAttributes().stream()
                  .map(TrackedEntityAttribute::getUid)
                  .toList());
        }

        List<String> violatingAttributes = new ArrayList<>();

        for (String attributeId : params.getAttributeAndFilterIds()) {
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

  private void checkIfMaxTeiLimitIsReached(TrackedEntityQueryParams params, int maxTeiLimit) {
    if (maxTeiLimit > 0) {
      int teCount = trackedEntityStore.getTrackedEntityCountWithMaxTeLimit(params);

      if (teCount > maxTeiLimit) {
        throw new IllegalQueryException("maxteicountreached");
      }
    }
  }

  private boolean isProgramMinAttributesViolated(TrackedEntityQueryParams params) {
    if (params.hasUniqueFilter()) {
      return false;
    }

    return (!params.hasFilters()
            && !params.hasAttributes()
            && params.getProgram().getMinAttributesRequiredToSearch() > 0)
        || (params.hasFilters()
            && params.getFilters().size() < params.getProgram().getMinAttributesRequiredToSearch())
        || (params.hasAttributes()
            && params.getAttributes().size()
                < params.getProgram().getMinAttributesRequiredToSearch());
  }

  private boolean isTeTypeMinAttributesViolated(TrackedEntityQueryParams params) {
    if (params.hasUniqueFilter()) {
      return false;
    }

    return (!params.hasFilters()
            && !params.hasAttributes()
            && params.getTrackedEntityType().getMinAttributesRequiredToSearch() > 0)
        || (params.hasFilters()
            && params.getFilters().size()
                < params.getTrackedEntityType().getMinAttributesRequiredToSearch())
        || (params.hasAttributes()
            && params.getAttributes().size()
                < params.getTrackedEntityType().getMinAttributesRequiredToSearch());
  }

  @Override
  @Transactional
  public long addTrackedEntity(TrackedEntity trackedEntity) {
    trackedEntityStore.save(trackedEntity);

    return trackedEntity.getId();
  }

  @Override
  @Transactional
  public long createTrackedEntity(
      TrackedEntity trackedEntity, Set<TrackedEntityAttributeValue> attributeValues) {
    long id = addTrackedEntity(trackedEntity);

    for (TrackedEntityAttributeValue pav : attributeValues) {
      attributeValueService.addTrackedEntityAttributeValue(pav);
      trackedEntity.getTrackedEntityAttributeValues().add(pav);
    }

    updateTrackedEntity(trackedEntity); // Update associations

    return id;
  }

  @Override
  @Transactional
  public void updateTrackedEntity(TrackedEntity trackedEntity) {
    trackedEntityStore.update(trackedEntity);
  }

  @Override
  @Transactional
  public void updateTrackedEntityLastUpdated(
      Set<String> trackedEntityUIDs, Date lastUpdated, String userInfoSnapshot) {
    trackedEntityStore.updateTrackedEntityLastUpdated(
        trackedEntityUIDs, lastUpdated, userInfoSnapshot);
  }

  @Override
  @Transactional
  public void deleteTrackedEntity(TrackedEntity trackedEntity) {
    attributeValueAuditService.deleteTrackedEntityAttributeValueChangeLogs(trackedEntity);
    trackedEntityStore.delete(trackedEntity);
  }

  @Override
  @Transactional(readOnly = true)
  public TrackedEntity getTrackedEntity(long id) {
    TrackedEntity te = trackedEntityStore.get(id);

    addTrackedEntityAudit(te, CurrentUserUtil.getCurrentUsername(), ChangeLogType.READ);

    return te;
  }

  @Override
  @Transactional
  public TrackedEntity getTrackedEntity(String uid) {
    TrackedEntity te = trackedEntityStore.getByUid(uid);
    addTrackedEntityAudit(te, CurrentUserUtil.getCurrentUsername(), ChangeLogType.READ);

    return te;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean trackedEntityExists(String uid) {
    return trackedEntityStore.exists(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean trackedEntityExistsIncludingDeleted(String uid) {
    return trackedEntityStore.existsIncludingDeleted(uid);
  }

  private boolean isLocalSearch(TrackedEntityQueryParams params, User user) {
    Set<OrganisationUnit> localOrgUnits = user.getOrganisationUnits();

    Set<OrganisationUnit> searchOrgUnits = new HashSet<>();

    if (params.isOrganisationUnitMode(SELECTED)) {
      searchOrgUnits = params.getOrgUnits();
    } else if (params.isOrganisationUnitMode(CHILDREN)
        || params.isOrganisationUnitMode(DESCENDANTS)) {
      for (OrganisationUnit ou : params.getOrgUnits()) {
        searchOrgUnits.addAll(ou.getChildren());
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

  private void addTrackedEntityAudit(
      TrackedEntity trackedEntity, String username, ChangeLogType changeLogType) {
    if (username != null
        && trackedEntity != null
        && trackedEntity.getTrackedEntityType() != null
        && trackedEntity.getTrackedEntityType().isAllowAuditLog()) {
      TrackedEntityChangeLog trackedEntityChangeLog =
          new TrackedEntityChangeLog(trackedEntity.getUid(), username, changeLogType);
      trackedEntityChangeLogService.addTrackedEntityChangeLog(trackedEntityChangeLog);
    }
  }
}
