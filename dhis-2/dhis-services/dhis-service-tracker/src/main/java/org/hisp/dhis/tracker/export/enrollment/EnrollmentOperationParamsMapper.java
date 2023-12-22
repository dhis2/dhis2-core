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
package org.hisp.dhis.tracker.export.enrollment;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateOrgUnitMode;

import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.OperationsParamsValidator;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps {@link EnrollmentOperationParams} to {@link EnrollmentQueryParams} which is used to fetch
 * enrollments from the DB.
 */
@Component
@RequiredArgsConstructor
class EnrollmentOperationParamsMapper {
  private final UserService userService;

  private final OperationsParamsValidator paramsValidator;

  @Transactional(readOnly = true)
  public EnrollmentQueryParams map(EnrollmentOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    Program program = paramsValidator.validateProgram(operationParams.getProgramUid(), currentUser);
    TrackedEntityType trackedEntityType =
        paramsValidator.validateTrackedEntityType(operationParams.getTrackedEntityTypeUid(), currentUser);
    TrackedEntity trackedEntity =
        paramsValidator.validateTrackedEntity(operationParams.getTrackedEntityUid(), currentUser);

    Set<OrganisationUnit> orgUnits =
        paramsValidator.validateOrgUnits(operationParams.getOrgUnitUids(), currentUser);
    validateOrgUnitMode(operationParams.getOrgUnitMode(), currentUser, program);

    EnrollmentQueryParams params = new EnrollmentQueryParams();
    params.setProgram(program);
    params.setProgramStatus(operationParams.getProgramStatus());
    params.setFollowUp(operationParams.getFollowUp());
    params.setLastUpdated(operationParams.getLastUpdated());
    params.setLastUpdatedDuration(operationParams.getLastUpdatedDuration());
    params.setProgramStartDate(operationParams.getProgramStartDate());
    params.setProgramEndDate(operationParams.getProgramEndDate());
    params.setTrackedEntityType(trackedEntityType);
    params.setTrackedEntity(trackedEntity);
    params.addOrganisationUnits(orgUnits);
    params.setOrganisationUnitMode(operationParams.getOrgUnitMode());
    params.setIncludeDeleted(operationParams.isIncludeDeleted());
    params.setUser(currentUser);
    params.setOrder(operationParams.getOrder());
    params.setEnrollmentUids(operationParams.getEnrollmentUids());

    mergeOrgUnitModes(operationParams, currentUser, params);

    return params;
  }

  /**
   * Prepares the org unit modes to simplify the SQL query creation by merging similar behaviored
   * org unit modes.
   */
  private void mergeOrgUnitModes(
      EnrollmentOperationParams operationParams, User user, EnrollmentQueryParams queryParams) {
    if (user != null && operationParams.getOrgUnitMode() == ACCESSIBLE) {
      queryParams.addOrganisationUnits(
          new HashSet<>(user.getTeiSearchOrganisationUnitsWithFallback()));
      queryParams.setOrganisationUnitMode(DESCENDANTS);
    } else if (user != null && operationParams.getOrgUnitMode() == CAPTURE) {
      queryParams.addOrganisationUnits(new HashSet<>(user.getOrganisationUnits()));
      queryParams.setOrganisationUnitMode(DESCENDANTS);
    }
  }
}
