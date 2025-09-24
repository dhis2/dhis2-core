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
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
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
@Component("org.hisp.dhis.tracker.imports.validation.validator.event.SecurityOwnershipValidator")
@RequiredArgsConstructor
class SecurityOwnershipValidator implements Validator<org.hisp.dhis.tracker.imports.domain.Event> {

  @Nonnull private final AclService aclService;
  @Nonnull private final TrackerOwnershipManager ownershipAccessManager;

  @Override
  public void validate(
      Reporter reporter, TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.Event event) {
    TrackerImportStrategy strategy = bundle.getStrategy(event);
    TrackerPreheat preheat = bundle.getPreheat();
    Event preheatEvent = bundle.getPreheat().getEvent(event.getEvent());

    ProgramStage programStage = bundle.getPreheat().getProgramStage(event.getProgramStage());
    Program program =
        strategy.isUpdateOrDelete()
            ? preheatEvent.getProgramStage().getProgram()
            : bundle.getPreheat().getProgram(event.getProgram());

    OrganisationUnit organisationUnit;

    if (strategy.isUpdateOrDelete()) {
      organisationUnit = preheatEvent.getOrganisationUnit();
    } else {
      organisationUnit = bundle.getPreheat().getOrganisationUnit(event.getOrgUnit());
    }

    if (program.isWithoutRegistration() || strategy.isCreate() || strategy.isDelete()) {
      checkEventOrgUnitWriteAccess(
          reporter,
          event,
          organisationUnit,
          strategy.isCreate()
              ? event.isCreatableInSearchScope()
              : preheatEvent.isCreatableInSearchScope(),
          bundle.getUser());
    }

    UID teUid = getTeUidFromEvent(bundle, event, program);

    CategoryOptionCombo categoryOptionCombo =
        bundle.getPreheat().getCategoryOptionCombo(event.getAttributeOptionCombo());
    OrganisationUnit ownerOrgUnit = getOwnerOrganisationUnit(preheat, teUid, program);
    // Check acting user is allowed to change existing/write event
    if (strategy.isUpdateOrDelete()) {
      TrackedEntity trackedEntity = preheatEvent.getEnrollment().getTrackedEntity();
      validateUpdateAndDeleteEvent(
          reporter,
          bundle,
          event,
          preheatEvent,
          trackedEntity == null ? null : UID.of(trackedEntity),
          ownerOrgUnit,
          bundle.getUser());
    } else {
      validateCreateEvent(
          reporter,
          bundle,
          event,
          categoryOptionCombo,
          programStage,
          teUid,
          organisationUnit,
          ownerOrgUnit,
          program,
          event.isCreatableInSearchScope());
    }
  }

  private void validateCreateEvent(
      Reporter reporter,
      TrackerBundle bundle,
      org.hisp.dhis.tracker.imports.domain.Event event,
      CategoryOptionCombo categoryOptionCombo,
      ProgramStage programStage,
      UID teUid,
      OrganisationUnit organisationUnit,
      OrganisationUnit ownerOrgUnit,
      Program program,
      boolean isCreatableInSearchScope) {
    boolean noProgramStageAndProgramIsWithoutReg =
        programStage == null && program.isWithoutRegistration();

    programStage =
        noProgramStageAndProgramIsWithoutReg ? program.getProgramStageByStage(1) : programStage;

    checkEventWriteAccess(
        reporter,
        bundle,
        event,
        programStage,
        organisationUnit,
        ownerOrgUnit,
        categoryOptionCombo,
        // TODO: Calculate correct `isCreateableInSearchScope` value
        teUid,
        isCreatableInSearchScope);
  }

  private void validateUpdateAndDeleteEvent(
      Reporter reporter,
      TrackerBundle bundle,
      org.hisp.dhis.tracker.imports.domain.Event event,
      Event preheatEvent,
      UID teUid,
      OrganisationUnit ownerOrgUnit,
      UserDetails user) {
    TrackerImportStrategy strategy = bundle.getStrategy(event);

    checkEventWriteAccess(
        reporter,
        bundle,
        event,
        preheatEvent.getProgramStage(),
        preheatEvent.getOrganisationUnit(),
        ownerOrgUnit,
        preheatEvent.getAttributeOptionCombo(),
        teUid,
        preheatEvent.isCreatableInSearchScope());

    if (strategy.isUpdate()) {
      if (preheatEvent.getProgramStage().getProgram().isWithoutRegistration()) {
        OrganisationUnit payloadOrgUnit =
            bundle.getPreheat().getOrganisationUnit(event.getOrgUnit());
        if (!preheatEvent.getOrganisationUnit().getUid().equals(payloadOrgUnit.getUid())) {
          checkEventOrgUnitWriteAccess(reporter, event, payloadOrgUnit, false, bundle.getUser());
        }
      }

      if (EventStatus.COMPLETED == preheatEvent.getStatus()
          && event.getStatus() != preheatEvent.getStatus()
          && (!user.isSuper() && !user.isAuthorized(F_UNCOMPLETE_EVENT))) {

        reporter.addError(event, E1083, user);
      }
    }
  }

  private UID getTeUidFromEvent(
      TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.Event event, Program program) {
    if (program.isWithoutRegistration()) {
      return null;
    }

    if (bundle.getStrategy(event).isUpdateOrDelete()) {
      return UID.of(
          bundle.getPreheat().getEvent(event.getUid()).getEnrollment().getTrackedEntity());
    }

    Enrollment enrollment = bundle.getPreheat().getEnrollment(event.getEnrollment());

    if (enrollment == null) {
      return bundle
          .findEnrollmentByUid(event.getEnrollment())
          .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getTrackedEntity)
          .orElse(null);
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

  private void checkTeTypeAndTeProgramAccess(
      Reporter reporter,
      TrackerDto dto,
      UID trackedEntity,
      OrganisationUnit ownerOrganisationUnit,
      Program program,
      UserDetails user) {
    if (!aclService.canDataRead(user, program.getTrackedEntityType())) {
      reporter.addError(dto, ValidationCode.E1104, user, program, program.getTrackedEntityType());
    }

    if (ownerOrganisationUnit != null
        && !ownershipAccessManager.hasAccess(
            user,
            trackedEntity == null ? null : trackedEntity.getValue(),
            ownerOrganisationUnit,
            program)) {
      reporter.addError(dto, ValidationCode.E1102, user, trackedEntity, program);
    }
  }

  private void checkEventWriteAccess(
      Reporter reporter,
      TrackerBundle bundle,
      org.hisp.dhis.tracker.imports.domain.Event event,
      ProgramStage programStage,
      OrganisationUnit eventOrgUnit,
      OrganisationUnit ownerOrgUnit,
      CategoryOptionCombo categoryOptionCombo,
      UID trackedEntity,
      boolean isCreatableInSearchScope) {

    if (bundle.getStrategy(event) != TrackerImportStrategy.UPDATE) {
      checkEventOrgUnitWriteAccess(
          reporter, event, eventOrgUnit, isCreatableInSearchScope, bundle.getUser());
    }

    if (programStage.getProgram().isWithoutRegistration()) {
      checkProgramWriteAccess(reporter, event, programStage.getProgram(), bundle.getUser());
    } else {
      checkProgramStageWriteAccess(reporter, event, programStage, bundle.getUser());
      final Program program = programStage.getProgram();

      checkProgramReadAccess(reporter, event, program, bundle.getUser());

      checkTeTypeAndTeProgramAccess(
          reporter,
          event,
          trackedEntity,
          ownerOrgUnit,
          programStage.getProgram(),
          bundle.getUser());
    }

    if (categoryOptionCombo != null) {
      checkWriteCategoryOptionComboAccess(reporter, event, categoryOptionCombo, bundle.getUser());
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

  private void checkProgramWriteAccess(
      Reporter reporter, TrackerDto dto, Program program, UserDetails user) {
    if (!aclService.canDataWrite(user, program)) {
      reporter.addError(dto, ValidationCode.E1091, user, program);
    }
  }

  public void checkWriteCategoryOptionComboAccess(
      Reporter reporter,
      TrackerDto dto,
      CategoryOptionCombo categoryOptionCombo,
      UserDetails user) {
    for (CategoryOption categoryOption : categoryOptionCombo.getCategoryOptions()) {
      if (!aclService.canDataWrite(user, categoryOption)) {
        reporter.addError(dto, ValidationCode.E1099, user, categoryOption);
      }
    }
  }
}
