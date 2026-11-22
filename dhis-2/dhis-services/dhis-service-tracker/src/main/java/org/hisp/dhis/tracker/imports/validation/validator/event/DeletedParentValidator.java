/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1132;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.findDeletedEnrollmentOrTrackedEntity;

import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;

/**
 * Rejects creating or updating a tracker event whose enrollment or tracked entity is soft deleted.
 * Deleting such an event is allowed (this validator does not run on delete), so that events left in
 * that state by older versions, which are reported by a data integrity check, can be cleaned up.
 *
 * <p>It runs after the security validators, so that a user without access to the event does not
 * learn about its enrollment or tracked entity from the error.
 */
class DeletedParentValidator implements Validator<Event> {
  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Event event) {
    TrackerPreheat preheat = bundle.getPreheat();
    org.hisp.dhis.program.Event existingEvent = preheat.getEvent(event.getEvent());
    Enrollment enrollment =
        existingEvent != null
            ? existingEvent.getEnrollment()
            : preheat.getEnrollment(event.getEnrollment());
    if (enrollment == null) {
      // the enrollment only exists in the payload or the event is in a program without
      // registration, its own validation rejects it if needed
      return;
    }

    findDeletedEnrollmentOrTrackedEntity(enrollment)
        .ifPresent(
            deleted ->
                reporter.addError(
                    event,
                    E1132,
                    TrackerType.EVENT.getName(),
                    event.getEvent(),
                    deleted.type().getName(),
                    deleted.uid()));
  }
}
