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
package org.hisp.dhis.cache.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link EvictionGuard}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class EvictionGuardTest {
  @Test
  void putAllowedWhenNoEvictionRecorded() {
    EvictionGuard guard = new EvictionGuard();
    assertTrue(guard.isPutAllowed("k", 100L));
  }

  @ParameterizedTest
  @CsvSource({
    "100, 100, false", // tx at eviction time: refuse (inclusive bound)
    "100, 99,  false", // tx before eviction: refuse
    "100, 101, true", // tx after eviction: allow
  })
  void perKeyEvictionGatesPut(long evictTs, long txTs, boolean allowed) {
    EvictionGuard guard = new EvictionGuard();
    guard.recordEviction("k", evictTs);
    assertEquals(allowed, guard.isPutAllowed("k", txTs));
  }

  @Test
  void laterEvictionWinsOverEarlier() {
    EvictionGuard guard = new EvictionGuard();
    guard.recordEviction("k", 200L);
    guard.recordEviction("k", 100L); // out-of-order record must not lower the bar
    assertFalse(guard.isPutAllowed("k", 150L));
  }

  @Test
  void otherKeysUnaffected() {
    EvictionGuard guard = new EvictionGuard();
    guard.recordEviction("k", 100L);
    assertTrue(guard.isPutAllowed("other", 50L));
  }

  @ParameterizedTest
  @CsvSource({"100, 100, false", "100, 99, false", "100, 101, true"})
  void regionClearGatesEveryKey(long clearTs, long txTs, boolean allowed) {
    EvictionGuard guard = new EvictionGuard();
    guard.recordClearAll(clearTs);
    assertEquals(allowed, guard.isPutAllowed("anything", txTs));
  }

  @Test
  void rotationKeepsPreviousGenerationVisible() {
    AtomicLong clock = new AtomicLong(0);
    EvictionGuard guard = new EvictionGuard(clock::get, 1000L); // window=1000 clock ticks
    guard.recordEviction("k", 100L);
    clock.set(1500); // one rotation: k moves to previous gen
    guard.recordEviction("other", 200L); // write path triggers rotation
    assertFalse(guard.isPutAllowed("k", 50L), "previous generation must still refuse");
    clock.set(3000); // two windows later: k fully expired
    guard.recordEviction("other2", 300L);
    assertTrue(guard.isPutAllowed("k", 50L), "expired entries no longer refuse");
  }

  @ParameterizedTest
  @CsvSource({
    "200, 100, 150, false", // out-of-order older clear must not lower the region bar
    "100, 200, 150, false", // in-order clears raise it
    "200, 100, 201, true", // a tx newer than the highest clear is still allowed
  })
  void laterClearAllWinsOverEarlier(
      long firstClearTs, long secondClearTs, long txTs, boolean allowed) {
    EvictionGuard guard = new EvictionGuard();
    guard.recordClearAll(firstClearTs);
    guard.recordClearAll(secondClearTs);
    assertEquals(allowed, guard.isPutAllowed("anything", txTs));
  }

  @ParameterizedTest
  @CsvSource({
    "200, 100, 150, 201", // the region clear is the higher bar and refuses on its own
    "200, 300, 250, 350", // the per-key eviction is the higher bar, the older clear never lowers it
    "200, 200, 200, 201", // both bars at the same time: the inclusive bound refuses a tx on it
  })
  void putMustClearBothTheRegionBarAndThePerKeyBar(
      long clearTs, long evictTs, long refusedTxTs, long allowedTxTs) {
    EvictionGuard guard = new EvictionGuard();
    // the eviction is recorded first on purpose: a clear that wiped the per-key generations instead
    // of only raising the region bar would let the second case's tx 250 through
    guard.recordEviction("k", evictTs);
    guard.recordClearAll(clearTs);
    assertFalse(guard.isPutAllowed("k", refusedTxTs), "a tx below either bar must be refused");
    assertTrue(guard.isPutAllowed("k", allowedTxTs), "a tx above both bars must be allowed");
  }

  @Test
  void rotationOnEveryCallNeverHidesYoungEviction() {
    // a clock that jumps a full window on every read forces a rotation inside every guard call, so
    // each call sees the generation pair one rotation later than the previous call did
    AtomicLong clock = new AtomicLong(0);
    EvictionGuard guard = new EvictionGuard(() -> clock.addAndGet(1001L), 1000L);
    guard.recordEviction("k", 100L);
    assertFalse(guard.isPutAllowed("k", 50L), "a rotation on the read path must not hide k");
    assertTrue(guard.isPutAllowed("k", 50L), "after a second rotation k has expired");
  }

  @Test
  void oneConcurrentRotationNeverHidesAYoungEviction() throws Exception {
    long window = 1000L;
    int rounds = 500;
    AtomicLong clock = new AtomicLong(0);
    EvictionGuard guard = new EvictionGuard(clock::get, window);
    CyclicBarrier barrier = new CyclicBarrier(2);
    AtomicReference<Throwable> rotatorFailure = new AtomicReference<>();

    Thread rotator =
        new Thread(
            () -> {
              try {
                for (int round = 0; round < rounds; round++) {
                  barrier.await();
                  clock.addAndGet(window + 1); // exactly one rotation per round
                  guard.recordEviction("rotator-" + round, 1L);
                  barrier.await();
                }
              } catch (InterruptedException e) {
                // the recording thread finished or failed, nothing left to rotate
              } catch (Throwable t) {
                rotatorFailure.set(t);
              }
            });
    rotator.setDaemon(true);
    rotator.start();
    try {
      for (int round = 0; round < rounds; round++) {
        barrier.await();
        String key = "k" + round;
        guard.recordEviction(key, 100L);
        // at most one rotation can interleave per round, so the record is younger than one full
        // window and must still be found in one of the two generations
        assertFalse(guard.isPutAllowed(key, 50L), "a racing rotation hid the record for " + key);
        barrier.await();
      }
    } finally {
      rotator.interrupt();
      rotator.join(5000);
    }
    assertNull(rotatorFailure.get(), "rotator thread failed");
  }
}
