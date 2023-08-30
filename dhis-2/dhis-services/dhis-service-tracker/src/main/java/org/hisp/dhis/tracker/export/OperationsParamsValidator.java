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

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OperationsParamsValidator {

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
    // TODO This user check should be done in a separate validation, so when it gets here we already
    // know it's not null
    if (user == null
        || !(user.isSuper()
            || user.isAuthorized(
                Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name()))) {
      throw new BadRequestException(
          "Current user is not authorized to query across all organisation units");

      // TODO Validate user scope if mode ALL needs to use user's search or capture scope
    }
  }

  private static void validateUserScope(
      User user, Program program, OrganisationUnitSelectionMode orgUnitMode)
      throws BadRequestException {

    // TODO This user check should be done in a separate validation, so when it gets here we already
    // know it's not null
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
    // TODO This user check should be done in a separate validation, so when it gets here we already
    // know it's not null
    if (user == null) {
      throw new BadRequestException("User is required for orgUnitMode: " + CAPTURE);
    } else if (user.getOrganisationUnits().isEmpty()) {
      throw new BadRequestException("User needs to be assigned data capture orgunits");
    }
  }

  /**
   * Returns a list of all the org units the user has access to
   *
   * @param user the user to check the access of
   * @param orgUnits parent org units to get descendants/children of
   * @param orgUnitDescendants function to retrieve org units, in case ou mode is descendants
   * @param program the program the user wants to access to
   * @return a list containing the user accessible organisation units
   * @throws ForbiddenException if the user has no access to any of the provided org units
   */
  public static Set<OrganisationUnit> validateAccessibleOrgUnits(
      User user,
      Set<OrganisationUnit> orgUnits,
      OrganisationUnitSelectionMode orgUnitMode,
      Program program,
      Function<String, List<OrganisationUnit>> orgUnitDescendants,
      TrackerAccessManager trackerAccessManager)
      throws ForbiddenException {

    Set<OrganisationUnit> accessibleOrgUnits = new HashSet<>();

    for (OrganisationUnit orgUnit : orgUnits) {
      Set<OrganisationUnit> accessibleOrgUnitsFound =
          switch (orgUnitMode) {
            case DESCENDANTS -> getAccessibleDescendants(
                user, program, orgUnitDescendants.apply(orgUnit.getUid()));
            case CHILDREN -> getAccessibleDescendants(
                user,
                program,
                Stream.concat(Stream.of(orgUnit), orgUnit.getChildren().stream()).toList());
            case SELECTED -> getSelectedOrgUnits(user, program, orgUnit, trackerAccessManager);
            default -> Collections.emptySet();
          };

      if (accessibleOrgUnitsFound.isEmpty()) {
        throw new ForbiddenException("User does not have access to orgUnit: " + orgUnit.getUid());
      }

      accessibleOrgUnits.addAll(accessibleOrgUnitsFound);
    }

    if (orgUnitMode == CAPTURE) {
      return new HashSet<>(user.getOrganisationUnits());
    } else if (orgUnitMode == ACCESSIBLE) {
      return getAccessibleOrgUnits(user, program);
    }

    return accessibleOrgUnits;
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
  private static Set<OrganisationUnit> getAccessibleDescendants(
      User user, Program program, List<OrganisationUnit> orgUnits) {
    if (orgUnits.isEmpty()) {
      return Collections.emptySet();
    }

    Set<OrganisationUnit> userOrgUnits =
        isProgramAccessRestricted(program)
            ? user.getOrganisationUnits()
            : user.getTeiSearchOrganisationUnits();

    return orgUnits.stream()
        .filter(
            availableOrgUnit ->
                userOrgUnits.stream()
                    .anyMatch(
                        userOrgUnit -> availableOrgUnit.getPath().contains(userOrgUnit.getPath())))
        .collect(Collectors.toSet());
  }

  private static boolean isProgramAccessRestricted(Program program) {
    return program != null && (program.isClosed() || program.isProtected());
  }

  private static Set<OrganisationUnit> getAccessibleOrgUnits(User user, Program program) {
    return isProgramAccessRestricted(program)
        ? new HashSet<>(user.getOrganisationUnits())
        : new HashSet<>(user.getTeiSearchOrganisationUnitsWithFallback());
  }

  private static Set<OrganisationUnit> getSelectedOrgUnits(
      User user,
      Program program,
      OrganisationUnit orgUnit,
      TrackerAccessManager trackerAccessManager) {
    return trackerAccessManager.canAccess(user, program, orgUnit)
        ? Set.of(orgUnit)
        : Collections.emptySet();
  }
}
