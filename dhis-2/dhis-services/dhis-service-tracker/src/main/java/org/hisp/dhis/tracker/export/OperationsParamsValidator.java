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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.security.Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS;

import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OperationsParamsValidator {

  private final ProgramService programService;

  private final AclService aclService;

  private final TrackedEntityService trackedEntityService;

  private final TrackedEntityTypeService trackedEntityTypeService;

  private final OrganisationUnitService organisationUnitService;

  /**
   * Validates the user is authorized and/or has the necessary configuration set up in case the org
   * unit mode is ALL, ACCESSIBLE or CAPTURE. If the mode used is none of these three, no validation
   * will be run
   *
   * @param orgUnitMode the {@link OrganisationUnitSelectionMode orgUnitMode} used in the current
   *     case
   * @throws BadRequestException if a validation error occurs for any of the three aforementioned
   *     modes
   */
  public static void validateOrgUnitMode(
      OrganisationUnitSelectionMode orgUnitMode, User user, Program program)
      throws BadRequestException {
    switch (orgUnitMode) {
      case ALL -> validateUserCanSearchOrgUnitModeALL(user);
      case SELECTED, ACCESSIBLE, DESCENDANTS, CHILDREN -> validateUserScope(
          user, program, orgUnitMode);
      case CAPTURE -> validateCaptureScope(user);
    }
  }

  private static void validateUserCanSearchOrgUnitModeALL(User user) throws BadRequestException {
    if (!user.isAuthorized(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS)) {
      throw new BadRequestException(
          "Current user is not authorized to query across all organisation units");
    }
  }

  private static void validateUserScope(
      User user, Program program, OrganisationUnitSelectionMode orgUnitMode)
      throws BadRequestException {

    if (user == null) {
      throw new BadRequestException("User is required for orgUnitMode: " + orgUnitMode);
    }

    if (program != null && (program.isClosed() || program.isProtected())) {
      if (user.getOrganisationUnits().isEmpty()) {
        throw new BadRequestException("User needs to be assigned data capture org units");
      }

    } else if (user.getTeiSearchOrganisationUnitsWithFallback().isEmpty()) {
      throw new BadRequestException(
          "User needs to be assigned either search or data capture org units");
    }
  }

  private static void validateCaptureScope(User user) throws BadRequestException {
    if (user == null) {
      throw new BadRequestException("User is required for orgUnitMode: " + CAPTURE);
    } else if (user.getOrganisationUnits().isEmpty()) {
      throw new BadRequestException("User needs to be assigned data capture org units");
    }
  }

  /**
   * Validates the specified program uid exists and is accessible by the supplied user
   *
   * @return the program if found and accessible
   * @throws BadRequestException if the program uid does not exist
   * @throws ForbiddenException if the user has no data read access to the program or its tracked
   *     entity type
   */
  public Program validateProgram(String programUid, User user)
      throws BadRequestException, ForbiddenException {
    if (programUid == null) {
      return null;
    }

    Program program = programService.getProgram(programUid);
    if (program == null) {
      throw new BadRequestException("Program is specified but does not exist: " + programUid);
    }

    if (!aclService.canDataRead(user, program)) {
      throw new ForbiddenException("User has no access to program: " + program.getUid());
    }

    if (program.getTrackedEntityType() != null
        && !aclService.canDataRead(user, program.getTrackedEntityType())) {
      throw new ForbiddenException(
          "Current user is not authorized to read data from selected program's tracked entity type: "
              + program.getTrackedEntityType().getUid());
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
  public TrackedEntity validateTrackedEntity(String trackedEntityUid, User user)
      throws BadRequestException, ForbiddenException {
    if (trackedEntityUid == null) {
      return null;
    }

    TrackedEntity trackedEntity = trackedEntityService.getTrackedEntity(trackedEntityUid);
    if (trackedEntity == null) {
      throw new BadRequestException(
          "Tracked entity is specified but does not exist: " + trackedEntityUid);
    }

    if (trackedEntity.getTrackedEntityType() != null
        && !aclService.canDataRead(user, trackedEntity.getTrackedEntityType())) {
      throw new ForbiddenException(
          "Current user is not authorized to read data from type of selected tracked entity: "
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
  public TrackedEntityType validateTrackedEntityType(String uid, User user)
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
          "Current user is not authorized to read data from selected tracked entity type: "
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
  public Set<OrganisationUnit> validateOrgUnits(Set<String> orgUnitIds, User user)
      throws BadRequestException, ForbiddenException {
    Set<OrganisationUnit> orgUnits = new HashSet<>();
    for (String orgUnitUid : orgUnitIds) {
      OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(orgUnitUid);
      if (orgUnit == null) {
        throw new BadRequestException("Organisation unit does not exist: " + orgUnitUid);
      }

      if (!user.isSuper()
          && !organisationUnitService.isInUserHierarchy(
              orgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback())) {
        throw new ForbiddenException(
            "Organisation unit is not part of the search scope: " + orgUnit.getUid());
      }

      orgUnits.add(orgUnit);
    }

    return orgUnits;
  }
}
