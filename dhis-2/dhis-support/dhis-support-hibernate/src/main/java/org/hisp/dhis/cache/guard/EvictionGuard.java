/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.cache.guard;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Lock-free record of recent L2 cache evictions for NONSTRICT_READ_WRITE regions.
 *
 * <p>A put is allowed only if its transaction's caching timestamp is strictly newer than the key's
 * last recorded eviction and the region's last recorded clear. This is the refusal READ_WRITE gets
 * from its SoftLock unlock timestamp, without the per-region lock, and the same discipline as
 * Infinispan's NonStrictAccessDelegate.
 *
 * <p>Memory is bounded by time, not size: records live in two rotating generations and are dropped
 * after surviving two rotations. Guaranteed retention is one window, thirty minutes of JVM running
 * time, sized to exceed the longest plausible transaction (analytics table generation and metadata
 * import run for many minutes). If a transaction still outlives its records, the guard forgives a
 * put it should have refused: that key degrades to plain unguarded NONSTRICT_READ_WRITE behaviour,
 * a bounded staleness risk, never corruption. There is no entry cap, so a mass metadata import can
 * retain 1e5 to 1e6 keys, tens of MB of transient heap, for up to an hour.
 *
 * <p>Both bars only move forward: a region clear never wipes per-key records, and out-of-order
 * records cannot lower either bar. A put must clear both. The generation pair lives behind one
 * volatile field, which {@link #isPutAllowed} dereferences exactly once, so a rotation cannot hide
 * a record between its two generation lookups. {@link #recordEviction} rereads the field after
 * writing and repeats the record if a rotation demoted its target generation, so a record always
 * lands in a generation with a full window ahead of it.
 *
 * <p>No wall-clock time anywhere. Refusals compare Hibernate cache timestamps ({@code
 * SimpleTimestamper}: wall-seeded, monotone under CAS), so refusal ordering survives clock steps in
 * either direction. Rotation uses {@link System#nanoTime}, so a suspended VM does not age the
 * windows; it does not advance in-flight transactions on this JVM either, which is the coherent
 * pairing.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class EvictionGuard {
  /**
   * One window is the guaranteed retention, so it must exceed the longest plausible transaction;
   * thirty minutes covers the long DHIS2 ones. The class javadoc states the failure mode for a
   * transaction that outlives its records.
   */
  private static final long DEFAULT_WINDOW_NANOS = TimeUnit.MINUTES.toNanos(30);

  private final LongSupplier clock;
  private final long windowNanos;
  private final AtomicLong regionClearTimestamp = new AtomicLong(Long.MIN_VALUE);

  /** The only mutable state reachable by readers, always replaced as a whole. */
  private volatile Generations generations; // NOSONAR java:S3077 - replaced as a whole

  public EvictionGuard() {
    this(System::nanoTime, DEFAULT_WINDOW_NANOS);
  }

  EvictionGuard(LongSupplier clock, long windowNanos) { // visible for tests
    this.clock = clock;
    this.windowNanos = windowNanos;
    long start = clock.getAsLong();
    this.generations = new Generations(new Generation(start), new Generation(start));
  }

  public void recordEviction(Object key, long cacheTimestamp) {
    Generation target;
    do {
      target = rotated().current;
      target.evictions.merge(key, cacheTimestamp, Math::max);
      // a rotation between the two statements above would leave the record in a generation whose
      // remaining lifetime is already partly spent, so record again into the new current one
    } while (generations.current != target);
  }

  public void recordClearAll(long cacheTimestamp) {
    regionClearTimestamp.accumulateAndGet(cacheTimestamp, Math::max);
  }

  public boolean isPutAllowed(Object key, long txTimestamp) {
    if (txTimestamp <= regionClearTimestamp.get()) return false;
    Generations pair = rotated();
    return allows(pair.current, key, txTimestamp) && allows(pair.previous, key, txTimestamp);
  }

  private static boolean allows(Generation generation, Object key, long txTimestamp) {
    Long evictedAt = generation.evictions.get(key);
    return evictedAt == null || txTimestamp > evictedAt;
  }

  /** Returns the live pair, rotating first if the current window has elapsed. */
  private Generations rotated() {
    Generations pair = generations;
    // subtraction, never a comparison of absolute values: only the difference of two nanoTime
    // readings is meaningful, and it stays correct across the counter wrapping
    if (clock.getAsLong() - pair.current.start <= windowNanos) return pair;
    synchronized (this) {
      if (generations != pair) return generations; // another thread rotated, its pair is fresh
      Generations rotated = new Generations(new Generation(clock.getAsLong()), pair.current);
      generations = rotated;
      return rotated;
    }
  }

  private static final class Generations {
    final Generation current;
    final Generation previous;

    Generations(Generation current, Generation previous) {
      this.current = current;
      this.previous = previous;
    }
  }

  private static final class Generation {
    final long start;
    final ConcurrentHashMap<Object, Long> evictions = new ConcurrentHashMap<>();

    Generation(long start) {
      this.start = start;
    }
  }
}
