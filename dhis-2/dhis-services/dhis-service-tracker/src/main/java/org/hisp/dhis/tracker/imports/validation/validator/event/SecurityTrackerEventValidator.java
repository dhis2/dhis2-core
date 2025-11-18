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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hisp.dhis.security.Authorities.F_UNCOMPLETE_EVENT;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1083;

import java.util.Map;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.tracker.acl.TrackerOwnershipManager;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
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
@Component("org.hisp.dhis.tracker.imports.validation.validator.event.SecurityTrackerEventValidator")
@RequiredArgsConstructor
class SecurityTrackerEventValidator
    implements Validator<org.hisp.dhis.tracker.imports.domain.Event> {

  @Nonnull private final AclService aclService;
  @Nonnull private final TrackerOwnershipManager ownershipAccessManager;

  @Override
  public void validate(
      Reporter reporter, TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.Event event) {
    if (!(event instanceof org.hisp.dhis.tracker.imports.domain.TrackerEvent trackerEvent)) {
      return;
    }
    TrackerImportStrategy strategy = bundle.getStrategy(event);
    TrackerEvent preheatEvent = bundle.getPreheat().getTrackerEvent(trackerEvent.getEvent());

    ProgramStage programStage =
        strategy.isUpdateOrDelete()
            ? preheatEvent.getProgramStage()
            : bundle.getPreheat().getProgramStage(trackerEvent.getProgramStage());
    OrganisationUnit organisationUnit =
        strategy.isUpdateOrDelete()
            ? preheatEvent.getOrganisationUnit()
            : bundle.getPreheat().getOrganisationUnit(trackerEvent.getOrgUnit());
    UID teUid = getTeUidFromEvent(bundle, trackerEvent);
    CategoryOptionCombo categoryOptionCombo =
        bundle.getPreheat().getCategoryOptionCombo(trackerEvent.getAttributeOptionCombo());
    OrganisationUnit ownerOrgUnit =
        getOwnerOrganisationUnit(bundle.getPreheat(), teUid, programStage.getProgram());
    // TODO(tracker) Validate payload org unit in user scope
    boolean isCreatableInSearchScope =
        strategy.isCreate()
            ? trackerEvent.isCreatableInSearchScope()
            : preheatEvent.isCreatableInSearchScope();

    // TODO: Discuss with product how this should be fixed.
    // At the moment we are checking capture scope for event org unit
    // only when we are creating or deleting an event.
    // When updating, ownership is enough.
    // We need to understand what to do when updating the org unit.
    if (strategy.isCreate() || strategy.isDelete()) {
      checkEventOrgUnitWriteAccess(
          reporter, trackerEvent, organisationUnit, isCreatableInSearchScope, bundle.getUser());
    }
    checkProgramStageWriteAccess(reporter, trackerEvent, programStage, bundle.getUser());
    checkProgramReadAccess(reporter, trackerEvent, programStage.getProgram(), bundle.getUser());
    checkTeTypeReadAccess(reporter, trackerEvent, programStage.getProgram(), bundle.getUser());
    checkOwnership(
        reporter, trackerEvent, teUid, ownerOrgUnit, programStage.getProgram(), bundle.getUser());
    checkWriteCategoryOptionComboAccess(
        reporter, trackerEvent, categoryOptionCombo, bundle.getUser());

    if (strategy.isUpdate()) {
      checkCompletablePermission(reporter, trackerEvent, preheatEvent, bundle.getUser());
    }
  }

  private void checkCompletablePermission(
      Reporter reporter,
      org.hisp.dhis.tracker.imports.domain.Event event,
      TrackerEvent preheatEvent,
      UserDetails user) {
    if (EventStatus.COMPLETED == preheatEvent.getStatus()
        && event.getStatus() != preheatEvent.getStatus()
        && (!user.isSuper() && !user.isAuthorized(F_UNCOMPLETE_EVENT))) {
      reporter.addError(event, E1083, user);
    }
  }

  @Nonnull
  private UID getTeUidFromEvent(
      TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.TrackerEvent event) {
    if (bundle.getStrategy(event).isUpdateOrDelete()) {
      return UID.of(
          bundle.getPreheat().getTrackerEvent(event.getUid()).getEnrollment().getTrackedEntity());
    }

    Enrollment enrollment = bundle.getPreheat().getEnrollment(event.getEnrollment());

    if (enrollment == null) {
      return bundle
          .findEnrollmentByUid(event.getEnrollment())
          .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getTrackedEntity)
          .get();
    }

    return UID.of(enrollment.getTrackedEntity());
  }

  private OrganisationUnit getOwnerOrganisationUnit(
      TrackerPreheat preheat, UID teUid, Program program) {
    Map<String, TrackedEntityProgramOwnerOrgUnit> programOwner =
        preheat.getProgramOwner().get(teUid);
    if (programOwner == null || programOwner.get(program.getUid()) == null) {
      return null;
    } else {
      return programOwner.get(program.getUid()).getOrganisationUnit();
    }
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true;
  }

  private void checkTeTypeReadAccess(
      Reporter reporter, TrackerDto dto, Program program, UserDetails user) {
    if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
      reporter.addError(dto, ValidationCode.E1104, user, program, program.getTrackedEntityType());
    }
  }

  private void checkOwnership(
      Reporter reporter,
      TrackerDto dto,
      UID trackedEntity,
      OrganisationUnit ownerOrganisationUnit,
      Program program,
      UserDetails user) {
    if (ownerOrganisationUnit != null
        && !ownershipAccessManager.hasAccess(
            user, trackedEntity.getValue(), ownerOrganisationUnit, program)) {
      reporter.addError(dto, ValidationCode.E1102, user, trackedEntity, program);
    }
  }

  private void checkEventOrgUnitWriteAccess(
      Reporter reporter,
      org.hisp.dhis.tracker.imports.domain.Event event,
      OrganisationUnit eventOrgUnit,
      boolean isCreatableInSearchScope,
      UserDetails user) {
    String path = eventOrgUnit.getStoredPath();
    if (isCreatableInSearchScope
        ? !user.isInUserEffectiveSearchOrgUnitHierarchy(path)
        : !user.isInUserHierarchy(path)) {
      reporter.addError(event, ValidationCode.E1000, user, eventOrgUnit);
    }
  }

  private void checkProgramReadAccess(
      Reporter reporter, TrackerDto dto, Program program, UserDetails user) {
    if (!aclService.canDataRead(user, program)) {
      reporter.addError(dto, ValidationCode.E1096, user, program);
    }
  }

  private void checkProgramStageWriteAccess(
      Reporter reporter, TrackerDto dto, ProgramStage programStage, UserDetails user) {
    if (!aclService.canDataWrite(user, programStage)) {
      reporter.addError(dto, ValidationCode.E1095, user, programStage);
    }
  }

  public void checkWriteCategoryOptionComboAccess(
      Reporter reporter,
      TrackerDto dto,
      CategoryOptionCombo categoryOptionCombo,
      UserDetails user) {
    if (categoryOptionCombo == null) {
      return;
    }

    for (CategoryOption categoryOption : categoryOptionCombo.getCategoryOptions()) {
      if (!aclService.canDataWrite(user, categoryOption)) {
        reporter.addError(dto, ValidationCode.E1099, user, categoryOption);
      }
    }
  }
}
