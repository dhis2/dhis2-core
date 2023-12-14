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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.Program;
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
}
