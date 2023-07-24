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
package org.hisp.dhis.webapi.controller.event.mapper;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.User;

public class EventParamsMapperUtils {
  private EventParamsMapperUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static List<OrganisationUnit> validateAccessibleOrgUnits(
      User user,
      OrganisationUnit orgUnit,
      OrganisationUnitSelectionMode orgUnitMode,
      Program program,
      Function<String, List<OrganisationUnit>> orgUnitDescendants,
      TrackerAccessManager trackerAccessManager) {
    List<OrganisationUnit> accessibleOrgUnits =
        getUserAccessibleOrgUnits(
            user, orgUnit, orgUnitMode, program, orgUnitDescendants, trackerAccessManager);

    if (orgUnit != null && accessibleOrgUnits.isEmpty()) {
      throw new IllegalQueryException("User does not have access to orgUnit: " + orgUnit.getUid());
    }

    return accessibleOrgUnits;
  }

  /**
   * Returns a list of all the org units the user has access to
   *
   * @param user the user to check the access of
   * @param orgUnit parent org unit to get descendants/children of
   * @param orgUnitDescendants function to retrieve org units, in case ou mode is descendants
   * @param program the program the user wants to access to
   * @return a list containing the user accessible organisation units
   */
  private static List<OrganisationUnit> getUserAccessibleOrgUnits(
      User user,
      OrganisationUnit orgUnit,
      OrganisationUnitSelectionMode orgUnitMode,
      Program program,
      Function<String, List<OrganisationUnit>> orgUnitDescendants,
      TrackerAccessManager trackerAccessManager) {

    switch (orgUnitMode) {
      case DESCENDANTS:
        return orgUnit != null
            ? getAccessibleDescendants(user, program, orgUnitDescendants.apply(orgUnit.getUid()))
            : Collections.emptyList();
      case CHILDREN:
        return orgUnit != null
            ? getAccessibleDescendants(
                user,
                program,
                Stream.concat(Stream.of(orgUnit), orgUnit.getChildren().stream())
                    .collect(Collectors.toList()))
            : Collections.emptyList();
      case CAPTURE:
        return new ArrayList<>(user.getOrganisationUnits());
      case ACCESSIBLE:
        return getAccessibleOrgUnits(user, program);
      case SELECTED:
        return getSelectedOrgUnits(user, program, orgUnit, trackerAccessManager);
      default:
        return Collections.emptyList();
    }
  }

  private static List<OrganisationUnit> getSelectedOrgUnits(
      User user,
      Program program,
      OrganisationUnit orgUnit,
      TrackerAccessManager trackerAccessManager) {
    return trackerAccessManager.canAccess(user, program, orgUnit)
        ? List.of(orgUnit)
        : Collections.emptyList();
  }

  private static List<OrganisationUnit> getAccessibleOrgUnits(User user, Program program) {
    return isProgramAccessRestricted(program)
        ? new ArrayList<>(user.getOrganisationUnits())
        : new ArrayList<>(user.getTeiSearchOrganisationUnitsWithFallback());
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
  private static List<OrganisationUnit> getAccessibleDescendants(
      User user, Program program, List<OrganisationUnit> orgUnits) {
    if (orgUnits.isEmpty()) {
      return Collections.emptyList();
    }

    if (isProgramAccessRestricted(program)) {
      return orgUnits.stream()
          .filter(
              availableOrgUnit ->
                  user.getOrganisationUnits().stream()
                      .anyMatch(
                          captureScopeOrgUnit ->
                              availableOrgUnit.getPath().contains(captureScopeOrgUnit.getPath())))
          .collect(Collectors.toList());
    } else {
      return orgUnits.stream()
          .filter(
              availableOrgUnit ->
                  user.getTeiSearchOrganisationUnits().stream()
                      .anyMatch(
                          searchScopeOrgUnit ->
                              availableOrgUnit.getPath().contains(searchScopeOrgUnit.getPath())))
          .collect(Collectors.toList());
    }
  }

  private static boolean isProgramAccessRestricted(Program program) {
    return program != null && (program.isClosed() || program.isProtected());
  }

  /**
   * Returns the same org unit mode if not null. If null, and an org unit is present, SELECT mode is
   * used by default, mode ACCESSIBLE is used otherwise.
   *
   * @param orgUnit
   * @param orgUnitMode
   * @return an org unit mode given the two input params
   */
  public static OrganisationUnitSelectionMode getOrgUnitMode(
      OrganisationUnit orgUnit, OrganisationUnitSelectionMode orgUnitMode) {
    if (orgUnitMode == null) {
      return orgUnit != null ? SELECTED : ACCESSIBLE;
    }
    return orgUnitMode;
  }
}
