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
package org.hisp.dhis.tracker.validation;

import static org.hisp.dhis.tracker.validation.PersistablesFilter.filter;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.Timing;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTrackerValidationService implements TrackerValidationService {

  @Qualifier("validationHooks")
  private final List<TrackerValidationHook> validationHooks;

  @Qualifier("ruleEngineValidationHooks")
  private final List<TrackerValidationHook> ruleEngineValidationHooks;

  @Override
  public TrackerValidationReport validate(TrackerBundle bundle) {
    return validate(bundle, validationHooks);
  }

  @Override
  public TrackerValidationReport validateRuleEngine(TrackerBundle bundle) {
    return validate(bundle, ruleEngineValidationHooks);
  }

  private TrackerValidationReport validate(
      TrackerBundle bundle, List<TrackerValidationHook> hooks) {
    TrackerValidationReport validationReport = new TrackerValidationReport();

    User user = bundle.getUser();

    if ((user == null || user.isSuper()) && ValidationMode.SKIP == bundle.getValidationMode()) {
      log.warn(
          "Skipping validation for metadata import by user '"
              + bundle.getUsername()
              + "'. Not recommended.");
      return validationReport;
    }

    // Note that the bundle gets cloned internally, so the original bundle
    // is always available
    ValidationErrorReporter reporter =
        new ValidationErrorReporter(
            bundle.getPreheat().getIdSchemes(),
            bundle.getValidationMode() == ValidationMode.FAIL_FAST);

    try {
      validateTrackedEntities(bundle, hooks, validationReport, reporter);
      validateEnrollments(bundle, hooks, validationReport, reporter);
      validateEvents(bundle, hooks, validationReport, reporter);
      validateRelationships(bundle, hooks, validationReport, reporter);
      validateBundle(bundle, hooks, validationReport, reporter);
    } catch (ValidationFailFastException e) {
      // exit early when in FAIL_FAST validation mode
    }
    validationReport.addErrors(reporter.getErrors()).addWarnings(reporter.getWarnings());

    PersistablesFilter.Result persistables =
        filter(bundle, reporter.getInvalidDTOs(), bundle.getImportStrategy());

    bundle.setTrackedEntities(persistables.getTrackedEntities());
    bundle.setEnrollments(persistables.getEnrollments());
    bundle.setEvents(persistables.getEvents());
    bundle.setRelationships(persistables.getRelationships());

    validationReport.addErrors(persistables.getErrors());

    return validationReport;
  }

  private void validateTrackedEntities(
      TrackerBundle bundle,
      List<TrackerValidationHook> hooks,
      TrackerValidationReport validationReport,
      ValidationErrorReporter reporter) {
    for (TrackedEntity tei : bundle.getTrackedEntities()) {
      for (TrackerValidationHook hook : hooks) {
        if (hook.needsToRun(bundle.getStrategy(tei))) {
          Timer hookTimer = Timer.startTimer();

          hook.validateTrackedEntity(reporter, bundle, tei);

          validationReport.addTiming(new Timing(hook.getClass().getName(), hookTimer.toString()));

          if (hook.skipOnError() && didNotPassValidation(reporter, tei.getUid())) {
            break; // skip subsequent validation hooks for this
            // invalid entity
          }
        }
      }
    }
  }

  private void validateEnrollments(
      TrackerBundle bundle,
      List<TrackerValidationHook> hooks,
      TrackerValidationReport validationReport,
      ValidationErrorReporter reporter) {
    for (Enrollment enrollment : bundle.getEnrollments()) {
      for (TrackerValidationHook hook : hooks) {
        if (hook.needsToRun(bundle.getStrategy(enrollment))) {
          Timer hookTimer = Timer.startTimer();

          hook.validateEnrollment(reporter, bundle, enrollment);

          validationReport.addTiming(new Timing(hook.getClass().getName(), hookTimer.toString()));

          if (hook.skipOnError() && didNotPassValidation(reporter, enrollment.getUid())) {
            break; // skip subsequent validation hooks for this
            // invalid entity
          }
        }
      }
    }
  }

  private void validateEvents(
      TrackerBundle bundle,
      List<TrackerValidationHook> hooks,
      TrackerValidationReport validationReport,
      ValidationErrorReporter reporter) {
    for (Event event : bundle.getEvents()) {
      for (TrackerValidationHook hook : hooks) {
        if (hook.needsToRun(bundle.getStrategy(event))) {
          Timer hookTimer = Timer.startTimer();

          hook.validateEvent(reporter, bundle, event);

          validationReport.addTiming(new Timing(hook.getClass().getName(), hookTimer.toString()));

          if (hook.skipOnError() && didNotPassValidation(reporter, event.getUid())) {
            break; // skip subsequent validation hooks for this
            // invalid entity
          }
        }
      }
    }
  }

  private void validateRelationships(
      TrackerBundle bundle,
      List<TrackerValidationHook> hooks,
      TrackerValidationReport validationReport,
      ValidationErrorReporter reporter) {
    for (Relationship relationship : bundle.getRelationships()) {
      for (TrackerValidationHook hook : hooks) {
        if (hook.needsToRun(bundle.getStrategy(relationship))) {
          Timer hookTimer = Timer.startTimer();

          hook.validateRelationship(reporter, bundle, relationship);

          validationReport.addTiming(new Timing(hook.getClass().getName(), hookTimer.toString()));

          if (hook.skipOnError() && didNotPassValidation(reporter, relationship.getUid())) {
            break; // skip subsequent validation hooks for this
            // invalid entity
          }
        }
      }
    }
  }

  private static void validateBundle(
      TrackerBundle bundle,
      List<TrackerValidationHook> hooks,
      TrackerValidationReport validationReport,
      ValidationErrorReporter reporter) {
    for (TrackerValidationHook hook : hooks) {
      Timer hookTimer = Timer.startTimer();

      hook.validate(reporter, bundle);

      validationReport.addTiming(new Timing(hook.getClass().getName(), hookTimer.toString()));
    }
  }

  private boolean didNotPassValidation(ValidationErrorReporter reporter, String uid) {
    return reporter.getErrors().stream().anyMatch(r -> r.getUid().equals(uid));
  }
}
