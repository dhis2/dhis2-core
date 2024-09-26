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
package org.hisp.dhis.tracker.imports.validation.validator.trackedentity;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1100;

import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @author Ameen <ameen@dhis2.org>
 */
@Component(
    "org.hisp.dhis.tracker.imports.validation.validator.trackedentity.SecurityOwnershipValidator")
@RequiredArgsConstructor
class SecurityOwnershipValidator
    implements Validator<org.hisp.dhis.tracker.imports.domain.TrackedEntity> {
  @Nonnull private final AclService aclService;

  @Nonnull private final TrackerAccessManager trackerAccessManager;

  @Override
  public void validate(
      Reporter reporter,
      TrackerBundle bundle,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity) {
    TrackerImportStrategy strategy = bundle.getStrategy(trackedEntity);
    UserDetails user = CurrentUserUtil.getCurrentUserDetails();

    TrackedEntityType trackedEntityType =
        strategy.isUpdateOrDelete()
            ? bundle
                .getPreheat()
                .getTrackedEntity(trackedEntity.getTrackedEntity())
                .getTrackedEntityType()
            : bundle.getPreheat().getTrackedEntityType(trackedEntity.getTrackedEntityType());

    OrganisationUnit organisationUnit =
        strategy.isUpdateOrDelete()
            ? bundle
                .getPreheat()
                .getTrackedEntity(trackedEntity.getTrackedEntity())
                .getOrganisationUnit()
            : bundle.getPreheat().getOrganisationUnit(trackedEntity.getOrgUnit());

    if (strategy.isCreate()) {
      checkTeTypeWriteAccess(reporter, trackedEntity, trackedEntityType);
    }

    if (strategy.isCreate() || strategy.isDelete()) {
      checkOrgUnitInCaptureScope(reporter, trackedEntity, organisationUnit);
    }

    if (!strategy.isCreate()) {
      TrackedEntity te = bundle.getPreheat().getTrackedEntity(trackedEntity.getTrackedEntity());
      if (!trackerAccessManager.canWrite(user, te).isEmpty()) {
        reporter.addError(trackedEntity, ValidationCode.E1003, user.getUid(), te.getUid());
      }
    }

    if (strategy.isDelete()) {
      TrackedEntity te = bundle.getPreheat().getTrackedEntity(trackedEntity.getTrackedEntity());

      if (te.getEnrollments().stream().anyMatch(e -> !e.isDeleted())
          && !user.isAuthorized(Authorities.F_TEI_CASCADE_DELETE.name())) {
        reporter.addError(trackedEntity, E1100, user, te);
      }
    }
  }

  private void checkTeTypeWriteAccess(
      Reporter reporter,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity,
      TrackedEntityType trackedEntityType) {
    UserDetails user = CurrentUserUtil.getCurrentUserDetails();
    if (!aclService.canDataWrite(user, trackedEntityType)) {
      reporter.addError(trackedEntity, ValidationCode.E1001, user, trackedEntityType);
    }
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true;
  }

  private void checkOrgUnitInCaptureScope(
      Reporter reporter, TrackerDto dto, OrganisationUnit orgUnit) {
    UserDetails user = CurrentUserUtil.getCurrentUserDetails();
    if (!user.isInUserHierarchy(orgUnit.getPath())) {
      reporter.addError(dto, ValidationCode.E1000, user, orgUnit);
    }
  }
}
