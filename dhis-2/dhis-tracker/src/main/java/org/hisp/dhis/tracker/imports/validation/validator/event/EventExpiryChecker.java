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

import java.time.Instant;
import java.util.Date;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.Authorities;

/**
 * Checks the expiry rules of a {@link Program}, shared by the validators blocking changes to
 * expired events. Users with the {@link Authorities#F_EDIT_EXPIRED} authority are exempt from both
 * rules, so callers have to check the authority before applying them.
 */
final class EventExpiryChecker {
  private EventExpiryChecker() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Returns true if the event was completed longer ago than the number of days the program allows
   * changes to a completed event.
   */
  static boolean isCompletionExpired(Program program, Instant completedAt) {
    if (program.getCompleteEventsExpiryDays() == 0 || completedAt == null) {
      return false;
    }

    return now().isAfter(completedAt.plus(ofDays(program.getCompleteEventsExpiryDays())));
  }

  /**
   * Returns true if the current instant is after the end date of the period containing {@code
   * referenceDate} plus the program's expiry days.
   */
  static boolean isInExpiredPeriod(Program program, Instant referenceDate) {
    if (!hasExpiryPeriod(program) || referenceDate == null) {
      return false;
    }

    PeriodType periodType = program.getExpiryPeriodType();
    Period eventPeriod = periodType.createPeriod(Date.from(referenceDate));

    return eventPeriod
        .getEndDate()
        .toInstant() // This will be 00:00 time of the period end date.
        .plus(
            ofDays(
                program.getExpiryDays()
                    + 1L)) // Extra day added to account for final 24 hours of expiring day
        .isBefore(now());
  }

  /** Returns true if the program expires its events some days after the period they belong to. */
  static boolean hasExpiryPeriod(Program program) {
    return program.getExpiryPeriodType() != null && program.getExpiryDays() != 0;
  }
}
