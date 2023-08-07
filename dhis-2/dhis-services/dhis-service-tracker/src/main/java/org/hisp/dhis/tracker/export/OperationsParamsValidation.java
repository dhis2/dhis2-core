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
public class OperationsParamsValidation {

  public static void validateOrgUnitMode(
      OrganisationUnitSelectionMode orgUnitMode, User user, Program program)
      throws BadRequestException {

    String violation =
        switch (orgUnitMode) {
          case ALL -> userCanSearchOrgUnitModeALL(user)
              ? null
              : "Current user is not authorized to query across all organisation units";
          case ACCESSIBLE -> getAccessibleScopeValidation(user, program);
          case CAPTURE -> getCaptureScopeValidation(user);
          default -> null;
        };

    if (violation != null) {
      throw new BadRequestException(violation);
    }
  }

  private static String getCaptureScopeValidation(User user) {
    String violation = null;

    if (user == null) {
      violation = "User is required for orgUnitMode: " + CAPTURE;
    } else if (user.getOrganisationUnits().isEmpty()) {
      violation = "User needs to be assigned data capture orgunits";
    }

    return violation;
  }

  private static String getAccessibleScopeValidation(User user, Program program) {
    String violation;

    if (user == null) {
      return "User is required for orgUnitMode: " + OrganisationUnitSelectionMode.ACCESSIBLE;
    }

    // TODO If program null use search scope?
    if (program == null || program.isClosed() || program.isProtected()) {
      violation =
          user.getOrganisationUnits().isEmpty()
              ? "User needs to be assigned data capture orgunits"
              : null;
    } else {
      violation =
          user.getTeiSearchOrganisationUnitsWithFallback().isEmpty()
              ? "User needs to be assigned either TE search, data view or data capture org units"
              : null;
    }

    return violation;
  }

  private static boolean userCanSearchOrgUnitModeALL(User user) {
    if (user == null) {
      return false;
    }

    return user.isSuper()
        || user.isAuthorized(Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name());
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

  private static Set<OrganisationUnit> getSelectedOrgUnits(
      User user,
      Program program,
      OrganisationUnit orgUnit,
      TrackerAccessManager trackerAccessManager) {
    return trackerAccessManager.canAccess(user, program, orgUnit)
        ? Set.of(orgUnit)
        : Collections.emptySet();
  }

  private static Set<OrganisationUnit> getAccessibleOrgUnits(User user, Program program) {
    return isProgramAccessRestricted(program)
        ? new HashSet<>(user.getOrganisationUnits())
        : new HashSet<>(user.getTeiSearchOrganisationUnitsWithFallback());
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

    if (isProgramAccessRestricted(program)) {
      return orgUnits.stream()
          .filter(
              availableOrgUnit ->
                  user.getOrganisationUnits().stream()
                      .anyMatch(
                          captureScopeOrgUnit ->
                              availableOrgUnit.getPath().contains(captureScopeOrgUnit.getPath())))
          .collect(Collectors.toSet());
    } else {
      return orgUnits.stream()
          .filter(
              availableOrgUnit ->
                  user.getTeiSearchOrganisationUnits().stream()
                      .anyMatch(
                          searchScopeOrgUnit ->
                              availableOrgUnit.getPath().contains(searchScopeOrgUnit.getPath())))
          .collect(Collectors.toSet());
    }
  }

  private static boolean isProgramAccessRestricted(Program program) {
    return program != null && (program.isClosed() || program.isProtected());
  }
}
