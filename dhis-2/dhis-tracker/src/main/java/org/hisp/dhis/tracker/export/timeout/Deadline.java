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

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * An absolute point in time by which a tracker export request must be done. Absolute rather than a
 * duration so that passing it along cannot extend it, and based on {@link System#nanoTime()} so an
 * NTP step cannot move it.
 */
public record Deadline(long deadlineNanos, LongSupplier nanoTime) {

  /** A deadline {@code budget} from now. */
  public static Deadline in(Duration budget) {
    return in(budget, System::nanoTime);
  }

  /** Visible for testing. */
  public static Deadline in(Duration budget, LongSupplier nanoTime) {
    return new Deadline(nanoTime.getAsLong() + budget.toNanos(), nanoTime);
  }

  /** Negative once the deadline has passed. */
  public Duration remaining() {
    return Duration.ofNanos(deadlineNanos - nanoTime.getAsLong());
  }

  public boolean isExpired() {
    return remaining().toNanos() <= 0;
  }

  /**
   * The remaining budget for {@link java.sql.Statement#setQueryTimeout(int)}, which takes whole
   * seconds. Rounded up so a query is never cancelled before the deadline, which is why a response
   * can take up to a second longer than the budget.
   *
   * <p>Floored at 1 because {@code setQueryTimeout(0)} means no timeout at all, so an all but spent
   * budget would leave the query unbounded rather than tightly bounded.
   */
  public int remainingSecondsCeil() {
    long millis = remaining().toMillis();
    return (int) Math.max(1, (millis + 999) / 1000);
  }
}
