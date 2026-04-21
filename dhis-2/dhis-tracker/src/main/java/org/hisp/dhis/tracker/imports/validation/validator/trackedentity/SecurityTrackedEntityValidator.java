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
package org.hisp.dhis.tracker.imports.validation.validator.trackedentity;

import static org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper.map;

import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @author Ameen <ameen@dhis2.org>
 */
@Component(
    "org.hisp.dhis.tracker.imports.validation.validator.trackedentity.SecurityTrackedEntityValidator")
@RequiredArgsConstructor
class SecurityTrackedEntityValidator
    implements Validator<org.hisp.dhis.tracker.imports.domain.TrackedEntity> {
  @Nonnull private final TrackerAccessManager trackerAccessManager;

  @Override
  public void validate(
      Reporter reporter,
      TrackerBundle bundle,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity) {
    TrackerImportStrategy strategy = bundle.getStrategy(trackedEntity);
    UserDetails user = bundle.getUser();

    if (strategy.isCreate()) {
      handleCreate(bundle, trackedEntity, user, reporter);
    } else {
      TrackedEntity databaseTrackedEntity =
          bundle.getPreheat().getTrackedEntity(trackedEntity.getTrackedEntity());

      if (strategy.isUpdate()) {
        handleUpdate(trackedEntity, databaseTrackedEntity, bundle, reporter);
      } else if (strategy.isDelete()) {
        handleDelete(trackedEntity, databaseTrackedEntity, user, reporter);
      }
    }
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true;
  }

  private void handleCreate(
      TrackerBundle bundle,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity,
      UserDetails user,
      Reporter reporter) {
    TrackedEntity mappedTrackedEntity = map(bundle.getPreheat(), trackedEntity, user);
    trackerAccessManager
        .canCreate(user, mappedTrackedEntity)
        .forEach(eo -> reporter.addError(trackedEntity, eo.validationCode(), eo.args().toArray()));
  }

  private void handleUpdate(
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity,
      TrackedEntity databaseTrackedEntity,
      TrackerBundle bundle,
      Reporter reporter) {
    OrganisationUnit payloadOrgUnit =
        bundle.getPreheat().getOrganisationUnit(trackedEntity.getOrgUnit());
    OrganisationUnit orgUnit =
        payloadOrgUnit != null ? payloadOrgUnit : databaseTrackedEntity.getOrganisationUnit();

    trackerAccessManager
        .canUpdate(bundle.getUser(), databaseTrackedEntity, orgUnit)
        .forEach(eo -> reporter.addError(trackedEntity, eo.validationCode(), eo.args().toArray()));
  }

  private void handleDelete(
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity,
      TrackedEntity databaseTrackedEntity,
      UserDetails user,
      Reporter reporter) {
    trackerAccessManager
        .canDelete(user, databaseTrackedEntity)
        .forEach(eo -> reporter.addError(trackedEntity, eo.validationCode(), eo.args().toArray()));
  }
}
