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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.user.User;

public class TrackerEventCriteriaMapperUtils {
  private TrackerEventCriteriaMapperUtils() {
    throw new IllegalStateException("Utility class");
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

  public static void validateOrgUnitMode(
      OrganisationUnitSelectionMode orgUnitMode,
      User user,
      Program program,
      OrganisationUnit requestedOrgUnit) {
    switch (orgUnitMode) {
      case ALL:
        validateUserCanSearchOrgUnitModeALL(user);
        break;
      case SELECTED:
      case ACCESSIBLE:
      case DESCENDANTS:
      case CHILDREN:
        validateUserScope(user, program, orgUnitMode, requestedOrgUnit);
        break;
      case CAPTURE:
        validateCaptureScope(user);
    }
  }

  private static void validateUserCanSearchOrgUnitModeALL(User user) {
    // TODO(tracker) This user check is unnecessary for events, but needs to be here for
    // trackedEntities. In that case, it should be done in a separate validation, so when it gets
    // here we already know it's not null
    if (user == null
        || !(user.isSuper()
            || user.isAuthorized(
                Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name()))) {
      throw new IllegalQueryException(
          "Current user is not authorized to query across all organisation units");

      // TODO(tracker) Validate user scope if mode ALL needs to use user's search or capture scope
    }
  }

  private static void validateUserScope(
      User user,
      Program program,
      OrganisationUnitSelectionMode orgUnitMode,
      OrganisationUnit requestedOrgUnit) {

    // TODO(tracker) This user check is unnecessary for events, but needs to be here for
    // trackedEntities. In that case, it should be done in a separate validation, so when it gets
    // here we already know it's not null
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

    if (orgUnitMode != ACCESSIBLE && requestedOrgUnit == null) {
      throw new IllegalQueryException(
          "Organisation unit is required for org unit mode: " + orgUnitMode);
    }
  }

  private static void validateCaptureScope(User user) {
    // TODO(tracker) This user check is unnecessary for events, but needs to be here for
    // trackedEntities. In that case, it should be done in a separate validation, so when it gets
    // here we already know it's not null
    if (user == null) {
      throw new IllegalQueryException("User is required for orgUnitMode: " + CAPTURE);
    } else if (user.getOrganisationUnits().isEmpty()) {
      throw new IllegalQueryException("User needs to be assigned data capture org units");
    }
  }
}
