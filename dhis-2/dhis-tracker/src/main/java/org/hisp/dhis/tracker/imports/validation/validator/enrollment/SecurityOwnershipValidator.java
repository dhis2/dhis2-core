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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper.map;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1103;

import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @author Ameen <ameen@dhis2.org>
 */
@Component(
    "org.hisp.dhis.tracker.imports.validation.validator.enrollment.SecurityOwnershipValidator")
@RequiredArgsConstructor
class SecurityOwnershipValidator implements Validator<Enrollment> {
  @Nonnull private final TrackerAccessManager trackerAccessManager;

  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Enrollment enrollment) {
    TrackerImportStrategy strategy = bundle.getStrategy(enrollment);
    TrackerPreheat preheat = bundle.getPreheat();
    UserDetails user = bundle.getUser();

    if (strategy.isCreate()) {
      handleCreate(reporter, user, preheat, enrollment);
    } else {
      org.hisp.dhis.tracker.model.Enrollment databaseEnrollment =
          bundle.getPreheat().getEnrollment(enrollment.getEnrollment());
      CategoryOptionCombo aoc =
          preheat.getCategoryOptionCombo(enrollment.getAttributeOptionCombo());
      databaseEnrollment.setAttributeOptionCombo(aoc);

      if (strategy.isUpdate()) {
        handleUpdate(reporter, preheat, user, databaseEnrollment, enrollment);
      } else if (strategy.isDelete()) {
        handleDelete(reporter, preheat, user, databaseEnrollment, enrollment);
      }
    }
  }

  private void handleCreate(
      Reporter reporter, UserDetails user, TrackerPreheat preheat, Enrollment enrollment) {
    org.hisp.dhis.tracker.model.Enrollment mappedEnrollment = map(preheat, enrollment, user);

    trackerAccessManager
        .canCreate(user, mappedEnrollment)
        .forEach(em -> reporter.addError(enrollment, em.validationCode(), em.args().toArray()));
  }

  private void handleUpdate(
      Reporter reporter,
      TrackerPreheat preheat,
      UserDetails user,
      org.hisp.dhis.tracker.model.Enrollment databaseEnrollment,
      Enrollment enrollment) {
    trackerAccessManager
        .canUpdate(user, databaseEnrollment)
        .forEach(em -> reporter.addError(enrollment, em.validationCode(), em.args().toArray()));

    OrganisationUnit enrollmentOrgUnit = preheat.getOrganisationUnit(enrollment.getOrgUnit());
    OrganisationUnit databaseOrgUnit =
        preheat.getEnrollment(enrollment.getUID()).getOrganisationUnit();
    if (!enrollmentOrgUnit.getUid().equals(databaseOrgUnit.getUid())) {
      checkOrgUnitInCaptureScope(reporter, enrollment, enrollmentOrgUnit, user);
    }
  }

  private void handleDelete(
      Reporter reporter,
      TrackerPreheat preheat,
      UserDetails user,
      org.hisp.dhis.tracker.model.Enrollment databaseEnrollment,
      Enrollment enrollment) {
    trackerAccessManager
        .canDelete(user, databaseEnrollment)
        .forEach(eo -> reporter.addError(enrollment, eo.validationCode(), eo.args().toArray()));

    boolean hasNonDeletedEvents = enrollmentHasEvents(preheat, enrollment.getEnrollment());
    boolean hasNotCascadeDeleteAuthority =
        !user.isAuthorized(Authorities.F_ENROLLMENT_CASCADE_DELETE.name());

    if (hasNonDeletedEvents && hasNotCascadeDeleteAuthority) {
      reporter.addError(enrollment, E1103, user, enrollment.getEnrollment());
    }
  }

  private boolean enrollmentHasEvents(TrackerPreheat preheat, UID enrollmentUid) {
    return preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent().contains(enrollmentUid);
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true;
  }

  private void checkOrgUnitInCaptureScope(
      Reporter reporter, TrackerDto dto, OrganisationUnit orgUnit, UserDetails user) {
    if (!user.isInUserHierarchy(orgUnit.getStoredPath())) {
      reporter.addError(dto, ValidationCode.E1000, user, orgUnit);
    }
  }
}
