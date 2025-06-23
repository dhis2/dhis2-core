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

import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.SingleEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

@Component("org.hisp.dhis.tracker.imports.validation.validator.event.SecuritySingleEventValidator")
@RequiredArgsConstructor
class SecuritySingleEventValidator
    implements Validator<org.hisp.dhis.tracker.imports.domain.Event> {

  @Nonnull private final AclService aclService;

  @Override
  public void validate(
      Reporter reporter, TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.Event event) {
    if (!(event instanceof SingleEvent)) {
      return;
    }

    TrackerImportStrategy strategy = bundle.getStrategy(event);
    Event preheatEvent = bundle.getPreheat().getEvent(event.getEvent());

    OrganisationUnit organisationUnit =
        strategy.isUpdateOrDelete()
            ? preheatEvent.getOrganisationUnit()
            : bundle.getPreheat().getOrganisationUnit(event.getOrgUnit());
    ProgramStage programStage =
        strategy.isUpdateOrDelete()
            ? preheatEvent.getProgramStage()
            : bundle.getPreheat().getProgramStage(event.getProgramStage());
    CategoryOptionCombo categoryOptionCombo =
        bundle.getPreheat().getCategoryOptionCombo(event.getAttributeOptionCombo());

    checkOrgUnitInCaptureScope(reporter, event, organisationUnit, bundle.getUser());
    checkProgramWriteAccess(reporter, event, programStage.getProgram(), bundle.getUser());
    checkWriteCategoryOptionComboAccess(reporter, event, categoryOptionCombo, bundle.getUser());

    if (strategy.isUpdate()) {
      checkCompletablePermission(reporter, event, preheatEvent, bundle.getUser());
    }
  }

  private void checkCompletablePermission(
      Reporter reporter,
      org.hisp.dhis.tracker.imports.domain.Event event,
      Event preheatEvent,
      UserDetails user) {
    if (EventStatus.COMPLETED == preheatEvent.getStatus()
        && event.getStatus() != preheatEvent.getStatus()
        && (!user.isSuper() && !user.isAuthorized(F_UNCOMPLETE_EVENT))) {
      reporter.addError(event, E1083, user);
    }
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true;
  }

  private void checkOrgUnitInCaptureScope(
      Reporter reporter, TrackerDto dto, OrganisationUnit eventOrgUnit, UserDetails user) {
    if (!user.isInUserHierarchy(eventOrgUnit.getStoredPath())) {
      reporter.addError(dto, ValidationCode.E1000, user, eventOrgUnit);
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
