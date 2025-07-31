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

import static java.time.Duration.ofDays;
import static java.time.Instant.now;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1031;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1043;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1046;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1047;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1050;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1051;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class DateValidator implements Validator<Event> {
  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Event event) {
    TrackerPreheat preheat = bundle.getPreheat();
    Program program = preheat.getProgram(event.getProgram());

    if (event.getOccurredAt() == null && occurredAtDateIsMandatory(event, program)) {
      reporter.addError(event, E1031, event);
      return;
    }

    if (event instanceof TrackerEvent trackerEvent) {
      if (trackerEvent.getScheduledAt() == null
          && EventStatus.SCHEDULE == trackerEvent.getStatus()) {
        reporter.addError(trackerEvent, E1050, trackerEvent);
        return;
      }
      validateExpiryPeriodType(reporter, trackerEvent, program);
    }
    validateCompletedDateIsSetOnlyForSupportedStatus(reporter, event);
    validateCompletionExpiryDays(reporter, event, program, bundle.getUser());
  }

  private void validateCompletedDateIsSetOnlyForSupportedStatus(Reporter reporter, Event event) {
    if (event.getCompletedAt() != null && EventStatus.COMPLETED != event.getStatus()) {
      reporter.addError(event, E1051, event, event.getStatus());
    }
  }

  private void validateCompletionExpiryDays(
      Reporter reporter, Event event, Program program, UserDetails user) {
    if (event.getCompletedAt() == null || user.isAuthorized(Authorities.F_EDIT_EXPIRED.name())) {
      return;
    }

    if (program.getCompleteEventsExpiryDays() > 0
        && EventStatus.COMPLETED == event.getStatus()
        && now()
            .isAfter(event.getCompletedAt().plus(ofDays(program.getCompleteEventsExpiryDays())))) {
      reporter.addError(event, E1043, event);
    }
  }

  private void validateExpiryPeriodType(Reporter reporter, TrackerEvent event, Program program) {
    PeriodType periodType = program.getExpiryPeriodType();

    if (periodType == null || program.getExpiryDays() == 0) {
      // Nothing more to check here, return out
      return;
    }

    Instant referenceDate =
        Optional.of(event).map(Event::getOccurredAt).orElseGet(event::getScheduledAt);

    if (referenceDate == null) {
      reporter.addError(event, E1046, event);
      return;
    }

    Period eventPeriod = periodType.createPeriod(Date.from(referenceDate));

    if (eventPeriod
        .getEndDate()
        .toInstant() // This will be 00:00 time of the period end date.
        .plus(
            ofDays(
                program.getExpiryDays()
                    + 1L)) // Extra day added to account for final 24 hours of expiring day
        .isBefore(Instant.now())) {
      reporter.addError(event, E1047, event);
    }
  }

  private boolean occurredAtDateIsMandatory(Event event, Program program) {
    if (program.isWithoutRegistration()) {
      return true;
    }

    EventStatus eventStatus = event.getStatus();

    return eventStatus == EventStatus.ACTIVE || eventStatus == EventStatus.COMPLETED;
  }
}
