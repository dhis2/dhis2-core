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

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1043;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1047;

import java.time.Instant;
import java.util.Date;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;

/**
 * Rejects the deletion of an event which has expired, either because its completion or because the
 * period it belongs to is locked by the program. The same rules are applied to creations and
 * updates by {@link DateValidator}, users with the {@link Authorities#F_EDIT_EXPIRED} authority
 * being exempt from them.
 *
 * <p>A delete payload only carries the UID of the event, so the program and the dates the rules are
 * based on are taken from the persisted event.
 */
class ExpiredEventDeletionValidator implements Validator<Event> {
  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Event event) {
    if (bundle.getUser().isAuthorized(Authorities.F_EDIT_EXPIRED)) {
      return;
    }

    PersistedEvent persistedEvent = getPersistedEvent(bundle.getPreheat(), event);

    if (EventExpiryChecker.isCompletionExpired(
        persistedEvent.program(), persistedEvent.completedDate())) {
      reporter.addError(event, E1043, event);
    }

    if (EventExpiryChecker.isInExpiredPeriod(
        persistedEvent.program(), persistedEvent.referenceDate())) {
      reporter.addError(event, E1047, event);
    }
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return strategy == TrackerImportStrategy.DELETE;
  }

  private PersistedEvent getPersistedEvent(TrackerPreheat preheat, Event event) {
    org.hisp.dhis.program.Event persisted = preheat.getEvent(event.getEvent());

    return new PersistedEvent(
        persisted.getProgramStage().getProgram(),
        toInstant(
            persisted.getOccurredDate() != null
                ? persisted.getOccurredDate()
                : persisted.getScheduledDate()),
        getCompletedDate(persisted.getStatus(), persisted.getCompletedDate()));
  }

  private static Instant getCompletedDate(EventStatus status, Date completedDate) {
    return EventStatus.COMPLETED == status ? toInstant(completedDate) : null;
  }

  private static Instant toInstant(Date date) {
    return date == null ? null : date.toInstant();
  }

  /**
   * The state of the event as it is stored in the database. {@code referenceDate} is the date the
   * period expiry is based on and {@code completedDate} is only set if the event is completed.
   */
  private record PersistedEvent(Program program, Instant referenceDate, Instant completedDate) {}
}
