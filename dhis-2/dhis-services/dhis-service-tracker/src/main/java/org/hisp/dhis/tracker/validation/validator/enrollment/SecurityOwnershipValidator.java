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
package org.hisp.dhis.tracker.validation.validator.enrollment;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1103;
import static org.hisp.dhis.tracker.validation.validator.TrackerImporterAssertErrors.USER_CANT_BE_NULL;

import java.util.Map;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.Reporter;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.hisp.dhis.tracker.validation.Validator;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @author Ameen <ameen@dhis2.org>
 */
@Component("org.hisp.dhis.tracker.validation.validator.enrollment.SecurityOwnershipValidator")
@RequiredArgsConstructor
@Slf4j
class SecurityOwnershipValidator implements Validator<Enrollment> {
  @Nonnull private final AclService aclService;

  @Nonnull private final TrackerOwnershipManager ownershipAccessManager;

  @Nonnull private final OrganisationUnitService organisationUnitService;

  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Enrollment enrollment) {
    TrackerImportStrategy strategy = bundle.getStrategy(enrollment);
    TrackerPreheat preheat = bundle.getPreheat();
    User user = bundle.getUser();
    Program program =
        strategy.isUpdateOrDelete()
            ? bundle.getPreheat().getEnrollment(enrollment.getEnrollment()).getProgram()
            : bundle.getPreheat().getProgram(enrollment.getProgram());
    TrackedEntityInstance trackedEntity = getTrackedEntity(bundle, enrollment);
    OrganisationUnit ownerOrgUnit = getOwnerOrganisationUnit(preheat, trackedEntity, program);

    checkEnrollmentOrgUnit(reporter, bundle, strategy, enrollment);

    if (strategy.isDelete()) {
      boolean hasNonDeletedEvents = programInstanceHasEvents(preheat, enrollment.getEnrollment());
      boolean hasNotCascadeDeleteAuthority =
          !user.isAuthorized(Authorities.F_ENROLLMENT_CASCADE_DELETE.getAuthority());

      if (hasNonDeletedEvents && hasNotCascadeDeleteAuthority) {
        reporter.addError(enrollment, E1103, user, enrollment.getEnrollment());
      }
    }

    checkWriteEnrollmentAccess(
        reporter, bundle, enrollment, program, ownerOrgUnit, trackedEntity.getUid());
  }

  private TrackedEntityInstance getTrackedEntity(TrackerBundle bundle, Enrollment enrollment) {
    return bundle.getStrategy(enrollment).isUpdateOrDelete()
        ? bundle.getPreheat().getEnrollment(enrollment.getEnrollment()).getEntityInstance()
        : getTrackedEntityWhenStrategyCreate(bundle, enrollment);
  }

  private TrackedEntityInstance getTrackedEntityWhenStrategyCreate(
      TrackerBundle bundle, Enrollment enrollment) {
    TrackedEntityInstance trackedEntity =
        bundle.getPreheat().getTrackedEntity(enrollment.getTrackedEntity());

    if (trackedEntity != null) {
      return trackedEntity;
    }

    return bundle
        .findTrackedEntityByUid(enrollment.getTrackedEntity())
        .map(
            entity -> {
              TrackedEntityInstance newEntity = new TrackedEntityInstance();
              newEntity.setUid(entity.getUid());
              newEntity.setOrganisationUnit(
                  bundle.getPreheat().getOrganisationUnit(entity.getOrgUnit()));
              return newEntity;
            })
        .get();
  }

  private OrganisationUnit getOwnerOrganisationUnit(
      TrackerPreheat preheat, TrackedEntityInstance trackedEntity, Program program) {
    Map<String, TrackedEntityProgramOwnerOrgUnit> programOwner =
        preheat.getProgramOwner().get(trackedEntity.getUid());
    if (programOwner == null || programOwner.get(program.getUid()) == null) {
      return trackedEntity.getOrganisationUnit();
    } else {
      return programOwner.get(program.getUid()).getOrganisationUnit();
    }
  }

  private boolean programInstanceHasEvents(TrackerPreheat preheat, String programInstanceUid) {
    return preheat.getProgramInstanceWithOneOrMoreNonDeletedEvent().contains(programInstanceUid);
  }

  private void checkEnrollmentOrgUnit(
      Reporter reporter,
      TrackerBundle bundle,
      TrackerImportStrategy strategy,
      Enrollment enrollment) {
    OrganisationUnit enrollmentOrgUnit;

    if (strategy.isUpdateOrDelete()) {
      enrollmentOrgUnit =
          bundle.getPreheat().getEnrollment(enrollment.getEnrollment()).getOrganisationUnit();
    } else {
      enrollmentOrgUnit = bundle.getPreheat().getOrganisationUnit(enrollment.getOrgUnit());
    }

    // If enrollment is newly created, or going to be deleted, capture scope
    // has to be checked
    if (strategy.isCreate() || strategy.isDelete()) {
      checkOrgUnitInCaptureScope(reporter, bundle, enrollment, enrollmentOrgUnit);
    }
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true;
  }

  private void checkOrgUnitInCaptureScope(
      Reporter reporter, TrackerBundle bundle, TrackerDto dto, OrganisationUnit orgUnit) {
    User user = bundle.getUser();

    checkNotNull(user, USER_CANT_BE_NULL);

    if (!organisationUnitService.isInUserHierarchyCached(user, orgUnit)) {
      reporter.addError(dto, ValidationCode.E1000, user, orgUnit);
    }
  }

  private void checkTeiTypeAndTeiProgramAccess(
      Reporter reporter,
      TrackerDto dto,
      User user,
      String trackedEntityInstance,
      OrganisationUnit ownerOrganisationUnit,
      Program program) {

    if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
      reporter.addError(dto, ValidationCode.E1104, user, program, program.getTrackedEntityType());
    }

    if (ownerOrganisationUnit != null
        && !ownershipAccessManager.hasAccess(
            user, trackedEntityInstance, ownerOrganisationUnit, program)) {
      reporter.addError(dto, ValidationCode.E1102, user, trackedEntityInstance, program);
    }
  }

  private void checkWriteEnrollmentAccess(
      Reporter reporter,
      TrackerBundle bundle,
      Enrollment enrollment,
      Program program,
      OrganisationUnit ownerOrgUnit,
      String trackedEntity) {
    User user = bundle.getUser();

    checkProgramWriteAccess(reporter, enrollment, user, program);

    checkTeiTypeAndTeiProgramAccess(
        reporter, enrollment, user, trackedEntity, ownerOrgUnit, program);
  }

  private void checkProgramWriteAccess(
      Reporter reporter, TrackerDto dto, User user, Program program) {

    if (!aclService.canDataWrite(user, program)) {
      reporter.addError(dto, ValidationCode.E1091, user, program);
    }
  }
}
