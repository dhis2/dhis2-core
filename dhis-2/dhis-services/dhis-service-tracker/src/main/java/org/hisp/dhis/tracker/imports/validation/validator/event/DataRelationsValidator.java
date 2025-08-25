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

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1029;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1079;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1089;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1313;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class DataRelationsValidator implements Validator<Event> {
  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Event event) {
    ProgramStage programStage = bundle.getPreheat().getProgramStage(event.getProgramStage());
    OrganisationUnit organisationUnit = bundle.getPreheat().getOrganisationUnit(event.getOrgUnit());
    Program program = bundle.getPreheat().getProgram(event.getProgram());

    validateProgramStageInProgram(reporter, event, programStage, program);
    validateProgramHasOrgUnit(reporter, bundle.getPreheat(), event, organisationUnit, program);
    if (event instanceof TrackerEvent trackerEvent) {
      validateProgramWithRegistrationHasTrackedEntity(reporter, bundle, trackerEvent);
      validateRegistrationProgram(reporter, bundle, trackerEvent, program);
    }
  }

  private void validateProgramWithRegistrationHasTrackedEntity(
      Reporter reporter, TrackerBundle bundle, TrackerEvent event) {
    if (!enrollmentFromEventHasTrackedEntity(bundle, event)) {
      reporter.addError(event, E1313, event.getEvent());
    }
  }

  private void validateProgramStageInProgram(
      Reporter reporter, Event event, ProgramStage programStage, Program program) {
    if (!program.getUid().equals(programStage.getProgram().getUid())) {
      reporter.addError(event, E1089, event, programStage, program);
    }
  }

  private void validateRegistrationProgram(
      Reporter reporter, TrackerBundle bundle, TrackerEvent event, Program program) {
    Program enrollmentProgram = getEnrollmentProgramFromEvent(bundle, event);

    if (!program.equals(enrollmentProgram)) {
      reporter.addError(event, E1079, event, program, event.getEnrollment());
    }
  }

  private void validateProgramHasOrgUnit(
      Reporter reporter,
      TrackerPreheat preheat,
      Event event,
      OrganisationUnit organisationUnit,
      Program program) {
    if (programDoesNotHaveOrgUnit(program, organisationUnit, preheat.getProgramWithOrgUnitsMap())) {
      reporter.addError(event, E1029, organisationUnit, program);
    }
  }

  private boolean programDoesNotHaveOrgUnit(
      Program program, OrganisationUnit orgUnit, Map<String, List<String>> programAndOrgUnitsMap) {
    return !programAndOrgUnitsMap.containsKey(program.getUid())
        || !programAndOrgUnitsMap.get(program.getUid()).contains(orgUnit.getUid());
  }

  private Program getEnrollmentProgramFromEvent(TrackerBundle bundle, TrackerEvent event) {
    Enrollment preheatEnrollment = bundle.getPreheat().getEnrollment(event.getEnrollment());
    if (preheatEnrollment != null) {
      return preheatEnrollment.getProgram();
    } else {
      final Optional<org.hisp.dhis.tracker.imports.domain.Enrollment> enrollment =
          bundle.findEnrollmentByUid(event.getEnrollment());
      if (enrollment.isPresent()) {
        return bundle.getPreheat().getProgram(enrollment.get().getProgram());
      }
    }
    return null;
  }

  /**
   * Given a program with registration, check whether an event in the preheat or payload refers to
   * enrollment with an existing tracked entity. It should always find a tracked entity, because an
   * enrollment without a tracked entity is not allowed see {@link
   * org.hisp.dhis.tracker.imports.validation.validator.enrollment.MandatoryFieldsValidator}
   *
   * @param bundle to load the enrollment from the preheat or the payload if not in the database
   * @param event the event of an enrollment
   * @return whether the enrollment of the event has an existing tracked entity
   */
  private boolean enrollmentFromEventHasTrackedEntity(TrackerBundle bundle, TrackerEvent event) {
    if (event.getEnrollment() == null) {
      return true;
    }

    Enrollment enrollment = bundle.getPreheat().getEnrollment(event.getEnrollment());

    if (enrollment == null) {
      return !Objects.isNull(
          bundle
              .findEnrollmentByUid(event.getEnrollment())
              .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getTrackedEntity)
              .orElse(null));
    }

    return !Objects.isNull(enrollment.getTrackedEntity());
  }
}
