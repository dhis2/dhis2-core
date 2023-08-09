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
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;

import java.util.Set;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;

public class RequestParamsValidator {

  /**
   * Validates that no org unit is present if the ou mode is ACCESSIBLE or CAPTURE. If it is, an
   * exception will be thrown. If the org unit mode is not defined, SELECTED will be used by default
   * if an org unit is present. Otherwise, ACCESSIBLE will be the default.
   *
   * @param orgUnits list of org units to be validated
   * @param orgUnitMode
   * @throws BadRequestException
   */
  public static OrganisationUnitSelectionMode validateOrgUnitMode(
      Set<UID> orgUnits, OrganisationUnitSelectionMode orgUnitMode) throws BadRequestException {

    if (orgUnitMode == null) {
      orgUnitMode = orgUnits.isEmpty() ? ACCESSIBLE : SELECTED;
    }

    if (!orgUnits.isEmpty() && (orgUnitMode == ACCESSIBLE || orgUnitMode == CAPTURE)) {
      throw new BadRequestException(
          String.format(
              "orgUnitMode %s cannot be used with orgUnits. Please remove the orgUnit parameter and try again.",
              orgUnitMode));
    }

    if ((orgUnitMode == CHILDREN || orgUnitMode == SELECTED || orgUnitMode == DESCENDANTS)
        && orgUnits.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "At least one org unit is required for orgUnitMode: %s. Please add an orgUnit or use a different orgUnitMode.",
              orgUnitMode));
    }

    return orgUnitMode;
  }

  /**
   * Validates that the org unit is not present if the ou mode is ACCESSIBLE or CAPTURE. If it is,
   * an exception will be thrown. If the org unit mode is not defined, SELECTED will be used by
   * default if an org unit is present. Otherwise, ACCESSIBLE will be the default.
   *
   * @param orgUnit the org unit to validate
   * @param orgUnitMode
   * @return
   * @throws BadRequestException
   */
  public static OrganisationUnitSelectionMode validateOrgUnitMode(
      UID orgUnit, OrganisationUnitSelectionMode orgUnitMode) throws BadRequestException {

    if (orgUnitMode == null) {
      orgUnitMode = orgUnit != null ? SELECTED : ACCESSIBLE;
    }

    if ((orgUnitMode == ACCESSIBLE || orgUnitMode == CAPTURE) && orgUnit != null) {
      throw new BadRequestException(
          String.format(
              "orgUnitMode %s cannot be used with orgUnits. Please remove the orgUnit parameter and try again.",
              orgUnitMode));
    }

    if ((orgUnitMode == CHILDREN || orgUnitMode == SELECTED || orgUnitMode == DESCENDANTS)
        && orgUnit == null) {
      throw new BadRequestException(
          String.format(
              "orgUnit is required for orgUnitMode: %s. Please add an orgUnit or use a different orgUnitMode.",
              orgUnitMode));
    }

    return orgUnitMode;
  }
}
