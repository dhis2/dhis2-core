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
package org.hisp.dhis.tracker.export.trackedentity.aggregates;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.hisp.dhis.tracker.export.timeout.Deadline;
import org.hisp.dhis.tracker.export.timeout.DeadlineHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link TrackedEntityAggregate#withRequestContext}, which carries a request's deadline onto
 * the pooled threads its branches run on.
 *
 * <p>The deadline must not outlive the task that set it. The pool is cached, so its threads are
 * reused across requests, and a deadline left behind is read by whatever lands there next.
 */
class WithRequestContextTest {

  /** One thread, so every task after the first runs on a reused thread, as the cached pool does. */
  private final ExecutorService pool = Executors.newSingleThreadExecutor();

  @AfterEach
  void tearDown() {
    pool.shutdownNow();
    DeadlineHolder.clear();
  }

  /** Runs a task on the pool the way the aggregate does, and waits for it. */
  private <T> T runOnPool(Supplier<T> task) throws Exception {
    return pool.submit(task::get).get(10, TimeUnit.SECONDS);
  }

  /** Runs {@code task} on the pool wrapped in the deadline, as one aggregate branch does. */
  private <T> T runWithRequestContext(Deadline deadline, Supplier<T> task) throws Exception {
    return runOnPool(TrackedEntityAggregate.withRequestContext(Map.of(), deadline, task));
  }

  /** Runs {@code task} on the pool unwrapped, as anything else sharing the pool would. */
  private <T> T runWithoutRequestContext(Supplier<T> task) throws Exception {
    return runOnPool(task);
  }

  /** A budget that is already used up, as an export's is once it has timed out. */
  private static Deadline spent() {
    return Deadline.in(Duration.ZERO);
  }

  /**
   * Puts {@code deadline} on the pooled thread and leaves it there, so the thread is in the state a
   * reused one is in. Without this there is nothing for a later task to inherit and a wrapper that
   * restores instead of clearing looks correct.
   */
  private void threadAlreadyHolds(Deadline deadline) throws Exception {
    runWithoutRequestContext(
        () -> {
          DeadlineHolder.set(deadline);
          return null;
        });
  }

  /** Reads the deadline the way a query does, throwing if the thread holds an expired one. */
  private static Supplier<Void> readDeadline() {
    return () -> {
      DeadlineHolder.checkNotExpired("fetching tracked entities");
      return null;
    };
  }

  @Test
  void shouldNotLetAnExpiredDeadlineReachTheNextTaskOnTheSameThread() throws Exception {
    threadAlreadyHolds(spent());

    runWithRequestContext(Deadline.in(Duration.ofSeconds(10)), () -> null);

    // work that sets no deadline of its own, such as the data sync job or the SMS listener, must
    // stay unbounded rather than fail on a budget that was never its own
    assertDoesNotThrow(
        () -> runWithoutRequestContext(readDeadline()),
        "an unbounded task inherited the expired deadline left by an earlier export");
  }

  @Test
  void shouldClearTheDeadlineEvenWhenTheTaskFails() throws Exception {
    // a branch whose query times out throws, and its budget is spent by then, so the exception
    // path is exactly where an expired deadline would be left behind
    assertThrows(
        Exception.class,
        () ->
            runWithRequestContext(
                spent(),
                () -> {
                  throw new IllegalStateException("query cancelled");
                }));

    assertDoesNotThrow(
        () -> runWithoutRequestContext(readDeadline()),
        "a failed task left its deadline behind, so the next task on this thread inherits it");
  }

  @Test
  void shouldGiveEachTaskItsOwnDeadlineRatherThanTheOneLeftByThePrevious() throws Exception {
    threadAlreadyHolds(spent());
    Deadline mine = Deadline.in(Duration.ofSeconds(30));

    Deadline seen = runWithRequestContext(mine, DeadlineHolder::get);

    assertSame(mine, seen, "the task saw a deadline other than the one it was given");
  }
}
