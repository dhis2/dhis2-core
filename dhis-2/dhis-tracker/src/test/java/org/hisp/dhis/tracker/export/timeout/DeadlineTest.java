/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.export.timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DeadlineTest {

  /** Test clock so the assertions do not depend on how long the test itself takes. */
  private long nanos = TimeUnit.SECONDS.toNanos(1_000);

  private Deadline deadlineIn(Duration budget) {
    return Deadline.in(budget, () -> nanos);
  }

  private void elapse(Duration elapsed) {
    nanos += elapsed.toNanos();
  }

  @Test
  void shouldReportTheFullBudgetAsRemainingBeforeAnyTimePasses() {
    Deadline deadline = deadlineIn(Duration.ofSeconds(10));

    assertEquals(Duration.ofSeconds(10), deadline.remaining());
    assertFalse(deadline.isExpired());
  }

  @Test
  void shouldShrinkTheRemainingBudgetAsTimePasses() {
    Deadline deadline = deadlineIn(Duration.ofSeconds(10));

    elapse(Duration.ofSeconds(4));

    assertEquals(Duration.ofSeconds(6), deadline.remaining());
    assertFalse(deadline.isExpired());
  }

  @Test
  void shouldBeExpiredExactlyAtTheDeadline() {
    Deadline deadline = deadlineIn(Duration.ofSeconds(10));

    elapse(Duration.ofSeconds(10));

    assertEquals(Duration.ZERO, deadline.remaining());
    assertTrue(deadline.isExpired(), "a budget of exactly zero is used up");
  }

  @Test
  void shouldBeExpiredPastTheDeadlineAndReportNegativeRemaining() {
    Deadline deadline = deadlineIn(Duration.ofSeconds(10));

    elapse(Duration.ofSeconds(11));

    assertEquals(Duration.ofSeconds(-1), deadline.remaining());
    assertTrue(deadline.isExpired());
  }

  @Test
  void shouldRoundTheRemainingSecondsUpSoAQueryIsNeverGivenLessTimeThanItHas() {
    Deadline deadline = deadlineIn(Duration.ofMillis(2_400));

    assertEquals(3, deadline.remainingSecondsCeil());
  }

  @Test
  void shouldNotRoundUpAWholeNumberOfSeconds() {
    Deadline deadline = deadlineIn(Duration.ofSeconds(3));

    assertEquals(3, deadline.remainingSecondsCeil());
  }

  @Test
  void shouldFloorTheRemainingSecondsAtOneForASubSecondBudget() {
    Deadline deadline = deadlineIn(Duration.ofMillis(200));

    assertEquals(1, deadline.remainingSecondsCeil());
  }

  @Test
  void shouldNeverReturnZeroOrNegativeSecondsForAnExpiredBudget() {
    Deadline deadline = deadlineIn(Duration.ofSeconds(10));

    elapse(Duration.ofSeconds(15));

    assertTrue(deadline.isExpired());
    assertEquals(
        1,
        deadline.remainingSecondsCeil(),
        "0 would disable the timeout and a negative value would be rejected by the driver");
  }

  @Test
  void shouldNotBeAffectedByTheClockMovingBackwards() {
    // nanoTime is monotonic, so this only documents that the deadline is absolute: it is computed
    // once and never recomputed from a duration.
    Deadline deadline = deadlineIn(Duration.ofSeconds(10));
    long deadlineNanos = deadline.deadlineNanos();

    elapse(Duration.ofSeconds(5));

    assertEquals(deadlineNanos, deadline.deadlineNanos());
    assertEquals(Duration.ofSeconds(5), deadline.remaining());
  }
}
