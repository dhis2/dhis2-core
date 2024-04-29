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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1103;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @author Ameen <ameen@dhis2.org>
 */
@Component(
    "org.hisp.dhis.tracker.imports.validation.validator.enrollment.SecurityOwnershipValidator")
@RequiredArgsConstructor
@Slf4j
class SecurityOwnershipValidator implements Validator<Enrollment> {

  private final TrackerAccessManager trackerAccessManager;

  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Enrollment enrollment) {
    TrackerImportStrategy strategy = bundle.getStrategy(enrollment);
    TrackerPreheat preheat = bundle.getPreheat();
    User user = bundle.getUser();
    Program program =
        strategy.isUpdateOrDelete()
            ? bundle.getPreheat().getEnrollment(enrollment.getEnrollment()).getProgram()
            : bundle.getPreheat().getProgram(enrollment.getProgram());
    String trackedEntity =
        strategy.isDelete()
            ? bundle
                .getPreheat()
                .getEnrollment(enrollment.getEnrollment())
                .getTrackedEntity()
                .getUid()
            : enrollment.getTrackedEntity();
    OrganisationUnit ownerOrgUnit =
        strategy.isUpdateOrDelete()
            ? preheat
                .getProgramOwner()
                .get(trackedEntity)
                .get(program.getUid())
                .getOrganisationUnit()
            : preheat.getOrganisationUnit(enrollment.getOrgUnit());

    TrackedEntity entity = getTrackedEntity(preheat, trackedEntity);
    if (!trackerAccessManager
        .canWrite(UserDetails.fromUser(user), program, ownerOrgUnit, entity)
        .isEmpty()) {
      reporter.addError(
          enrollment, ValidationCode.E1040, bundle.getUser().getUid(), enrollment.getUid());
    }

    if (strategy.isDelete()) {
      boolean hasNonDeletedEvents =
          preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent().contains(enrollment.getEnrollment());

      if (hasNonDeletedEvents
          && !user.isAuthorized(Authorities.F_ENROLLMENT_CASCADE_DELETE.name())) {
        reporter.addError(enrollment, E1103, user, enrollment.getEnrollment());
      }
    }
  }

  private TrackedEntity getTrackedEntity(TrackerPreheat preheat, String trackedEntity) {
    TrackedEntity entity = preheat.getTrackedEntity(trackedEntity);
    if (entity == null) {
      entity = new TrackedEntity();
      entity.setUid(trackedEntity);
    }
    return entity;
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true;
  }
}
