/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.audit.AuditOperationType.READ;
import static org.hisp.dhis.security.Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.audit.TrackedEntityAuditService;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OperationsParamsValidator {

  private final ProgramService programService;

  private final AclService aclService;

  private final IdentifiableObjectManager manager;

  private final TrackedEntityTypeService trackedEntityTypeService;

  private final OrganisationUnitService organisationUnitService;

  private final TrackedEntityAuditService trackedEntityAuditService;

  /**
   * Validates the user is authorized and/or has the necessary configuration set up in case the org
   * unit mode is ALL, ACCESSIBLE or CAPTURE. If the mode used is none of these three, no validation
   * will be run
   *
   * @param orgUnitMode the {@link OrganisationUnitSelectionMode orgUnitMode} used in the current
   *     case
   * @throws ForbiddenException if a validation error occurs for any of the three aforementioned
   *     modes
   */
  public static void validateOrgUnitMode(
      OrganisationUnitSelectionMode orgUnitMode, Program program, UserDetails user)
      throws ForbiddenException {
    switch (orgUnitMode) {
      case ALL -> validateUserCanSearchOrgUnitModeALL(user);
      case SELECTED, ACCESSIBLE, DESCENDANTS, CHILDREN -> validateUserScope(program, user);
      case CAPTURE -> validateCaptureScope(user);
    }
  }

  private static void validateUserCanSearchOrgUnitModeALL(UserDetails user)
      throws ForbiddenException {
    if (!user.isAuthorized(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS)) {
      throw new ForbiddenException("User is not authorized to query across all organisation units");
    }
  }

  private static void validateUserScope(Program program, UserDetails user)
      throws ForbiddenException {
    if (program != null && (program.isClosed() || program.isProtected())) {
      if (user.getUserOrgUnitIds().isEmpty()) {
        throw new ForbiddenException("User needs to be assigned data capture org units");
      }

    } else if (user.getUserEffectiveSearchOrgUnitIds().isEmpty()) {
      throw new ForbiddenException(
          "User needs to be assigned either search or data capture org units");
    }
  }

  private static void validateCaptureScope(UserDetails user) throws ForbiddenException {
    if (user.getUserOrgUnitIds().isEmpty()) {
      throw new ForbiddenException("User needs to be assigned data capture org units");
    }
  }

  /**
   * Validates the specified tracker program uid exists and is accessible by the supplied user
   *
   * @return the program if found and accessible
   * @throws BadRequestException if the program uid does not exist or is not a tracker program
   * @throws ForbiddenException if the user has no data read access to the program or its tracked
   *     entity type
   */
  public Program validateTrackerProgram(UID uid, UserDetails user)
      throws BadRequestException, ForbiddenException {
    Program program = validateProgramAccess(uid, user);

    if (program == null) {
      return null;
    }

    if (program.isWithoutRegistration()) {
      throw new BadRequestException("Program specified is not a tracker program: " + uid);
    }

    if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
      throw new ForbiddenException(
          "User is not authorized to read data from selected program's tracked entity type: "
              + program.getTrackedEntityType().getUid());
    }

    return program;
  }

  /**
   * Validates the specified program uid exists and is accessible by the supplied user. If no other
   * program related validation is done, to be used by event programs only.
   *
   * @return the program if found and accessible
   * @throws BadRequestException if the program uid does not exist
   * @throws ForbiddenException if the user has no data read access to the program
   */
  public Program validateProgramAccess(UID uid, UserDetails user)
      throws BadRequestException, ForbiddenException {
    if (uid == null) {
      return null;
    }

    Program program = programService.getProgram(uid.getValue());
    if (program == null) {
      throw new BadRequestException("Program is specified but does not exist: " + uid);
    }

    if (!aclService.canDataRead(user, program)) {
      throw new ForbiddenException("User has no access to program: " + program.getUid());
    }

    return program;
  }

  /**
   * Validates the specified tracked entity uid exists and is accessible by the supplied user
   *
   * @return the tracked entity if found and accessible
   * @throws BadRequestException if the tracked entity uid does not exist
   * @throws ForbiddenException if the user has no data read access to type of the tracked entity
   */
  public TrackedEntity validateTrackedEntity(UID uid, UserDetails user, boolean includeDeleted)
      throws BadRequestException, ForbiddenException {
    if (uid == null) {
      return null;
    }

    // TODO(tracker) Are these validations enough? Should we check for ownership too?
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, uid.getValue());
    if (trackedEntity == null || (trackedEntity.isDeleted() && !includeDeleted)) {
      throw new BadRequestException("Tracked entity is specified but does not exist: " + uid);
    }
    trackedEntityAuditService.addTrackedEntityAudit(READ, user.getUsername(), trackedEntity);

    if (trackedEntity.getTrackedEntityType() != null
        && !aclService.canDataRead(user, trackedEntity.getTrackedEntityType())) {
      throw new ForbiddenException(
          "User is not authorized to read data from type of selected tracked entity: "
              + trackedEntity.getTrackedEntityType().getUid());
    }

    return trackedEntity;
  }

  /**
   * Validates the specified tracked entity type uid exists and is accessible by the supplied user
   *
   * @return the tracked entity type uid if found and accessible
   * @throws BadRequestException if the tracked entity type uid does not exist
   * @throws ForbiddenException if the user has no data read access to the tracked entity type
   */
  public TrackedEntityType validateTrackedEntityType(UID uid, UserDetails user)
      throws BadRequestException, ForbiddenException {
    if (uid == null) {
      return null;
    }

    TrackedEntityType trackedEntityType =
        trackedEntityTypeService.getTrackedEntityType(uid.getValue());
    if (trackedEntityType == null) {
      throw new BadRequestException("Tracked entity type is specified but does not exist: " + uid);
    }

    if (!aclService.canDataRead(user, trackedEntityType)) {
      throw new ForbiddenException(
          "User is not authorized to read data from selected tracked entity type: "
              + trackedEntityType.getUid());
    }

    return trackedEntityType;
  }

  /**
   * Validates the specified org unit uid exists and is part of the user scope
   *
   * @return the org unit if found and accessible
   * @throws BadRequestException if the org unit uid does not exist
   * @throws ForbiddenException if the org unit is not part of the user scope
   */
  public Set<OrganisationUnit> validateOrgUnits(Set<UID> orgUnitIds, UserDetails user)
      throws BadRequestException, ForbiddenException {
    Set<OrganisationUnit> orgUnits = new HashSet<>();
    for (UID orgUnitUid : orgUnitIds) {
      OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(orgUnitUid.getValue());
      if (orgUnit == null) {
        throw new BadRequestException("Organisation unit does not exist: " + orgUnitUid);
      }

      if (!user.isSuper()
          && !user.isInUserEffectiveSearchOrgUnitHierarchy(orgUnit.getStoredPath())) {
        throw new ForbiddenException(
            "Organisation unit is not part of the search scope: " + orgUnit.getUid());
      }

      orgUnits.add(orgUnit);
    }

    return orgUnits;
  }

  /**
   * Validates the specified filter contains at least the minimum number of characters to start the
   * search
   *
   * @throws BadRequestException if the filter doesn't contain enough characters
   */
  public static void validateMinCharactersToSearch(
      Map.Entry<UID, List<QueryFilter>> attributeFilter, TrackedEntityAttribute tea)
      throws BadRequestException {
    List<QueryFilter> binaryFilters =
        attributeFilter.getValue().stream().filter(qf -> qf.getOperator().isBinary()).toList();
    for (QueryFilter queryFilter : binaryFilters) {
      if (tea.getMinCharactersToSearch() > 0
          && (queryFilter.getFilter() == null
              || queryFilter.getFilter().length() < tea.getMinCharactersToSearch())) {
        throw new BadRequestException(
            String.format(
                "At least %d character(s) should be present in the filter to start a search, but the filter for the tracked entity attribute %s doesn't contain enough.",
                tea.getMinCharactersToSearch(), tea.getUid()));
      }
    }
  }
}
