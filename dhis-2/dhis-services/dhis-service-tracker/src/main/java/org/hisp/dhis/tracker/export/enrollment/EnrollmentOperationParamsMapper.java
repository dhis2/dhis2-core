/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.export.enrollment;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateOrgUnitMode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.acl.TrackerProgramService;
import org.hisp.dhis.tracker.export.OperationsParamsValidator;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps {@link EnrollmentOperationParams} to {@link EnrollmentQueryParams} which is used to fetch
 * enrollments from the DB.
 */
@Component
@RequiredArgsConstructor
class EnrollmentOperationParamsMapper {
  private final TrackerProgramService trackerProgramService;

  private final OrganisationUnitService organisationUnitService;

  private final OperationsParamsValidator paramsValidator;

  @Transactional(readOnly = true)
  public EnrollmentQueryParams map(
      @Nonnull EnrollmentOperationParams operationParams, @Nonnull UserDetails user)
      throws BadRequestException, ForbiddenException {
    Program program = paramsValidator.validateTrackerProgram(operationParams.getProgram(), user);
    List<Program> programs = getTrackerPrograms(program);

    TrackedEntity trackedEntity =
        paramsValidator.validateTrackedEntity(
            operationParams.getTrackedEntity(), user, operationParams.isIncludeDeleted());

    Set<OrganisationUnit> orgUnits =
        paramsValidator.validateOrgUnits(operationParams.getOrgUnits(), user);
    validateOrgUnitMode(operationParams.getOrgUnitMode(), program, user);

    EnrollmentQueryParams params = new EnrollmentQueryParams();
    params.setEnrolledInTrackerProgram(program);
    params.setAccessibleTrackerPrograms(programs);
    params.setEnrollmentStatus(operationParams.getEnrollmentStatus());
    params.setFollowUp(operationParams.getFollowUp());
    params.setLastUpdated(operationParams.getLastUpdated());
    params.setLastUpdatedDuration(operationParams.getLastUpdatedDuration());
    params.setProgramStartDate(operationParams.getProgramStartDate());
    params.setProgramEndDate(operationParams.getProgramEndDate());
    params.setTrackedEntity(trackedEntity);
    params.addOrganisationUnits(orgUnits);
    params.setOrganisationUnitMode(operationParams.getOrgUnitMode());
    params.setIncludeDeleted(operationParams.isIncludeDeleted());
    params.setOrder(operationParams.getOrder());
    params.setEnrollments(operationParams.getEnrollments());
    params.setIncludeAttributes(operationParams.getFields().isIncludesAttributes());

    mergeOrgUnitModes(operationParams, params, user);

    return params;
  }

  /**
   * Prepares the org unit modes to simplify the SQL query creation by merging similar behaviored
   * org unit modes.
   */
  private void mergeOrgUnitModes(
      EnrollmentOperationParams operationParams,
      EnrollmentQueryParams queryParams,
      UserDetails user) {
    if (operationParams.getOrgUnitMode() == ACCESSIBLE) {
      queryParams.addOrganisationUnits(
          new HashSet<>(
              organisationUnitService.getOrganisationUnitsByUid(
                  user.getUserEffectiveSearchOrgUnitIds())));
      queryParams.setOrganisationUnitMode(DESCENDANTS);
    } else if (operationParams.getOrgUnitMode() == CAPTURE) {
      queryParams.addOrganisationUnits(
          new HashSet<>(
              organisationUnitService.getOrganisationUnitsByUid(user.getUserOrgUnitIds())));
      queryParams.setOrganisationUnitMode(DESCENDANTS);
    }
  }

  private List<Program> getTrackerPrograms(Program program) {
    if (program == null) {
      return trackerProgramService.getAccessibleTrackerPrograms();
    }

    return emptyList();
  }
}
