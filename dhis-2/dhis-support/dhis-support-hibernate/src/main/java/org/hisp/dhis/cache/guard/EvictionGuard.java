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
 * <p>Refuses stale {@code putFromLoad} calls the same way READ_WRITE's SoftLock unlock timestamp
 * does, without the per-region ReentrantReadWriteLock: a put is allowed only if its transaction's
 * caching timestamp is strictly newer than the key's last recorded eviction and than the region's
 * last recorded clear. Same discipline as Infinispan's NonStrictAccessDelegate ("putFromLoad not
 * executed since tx started before last region invalidation").
 *
 * <p>Memory is bounded by two rotating generations of eviction records: a record that has survived
 * two rotations is dropped. Retention therefore depends on where in a window a record lands. A
 * record written just after a rotation lives through its own window and the next one, so two
 * windows is the upper bound; a record written just before a rotation is demoted immediately and
 * dropped at the following rotation, so the guaranteed retention is one window, thirty minutes of
 * running time. The window premise is that this floor, not the upper bound, exceeds the longest
 * plausible transaction, and DHIS2 does have long ones. Analytics table generation and metadata
 * import routinely run for many minutes, far past the 60 second API request timeout, so the window
 * is sized in tens of minutes rather than in seconds. If an extreme transaction still outlives its
 * records, the guard forgives a put it would otherwise have refused: that single key degrades to
 * plain unguarded NONSTRICT_READ_WRITE behaviour, which is a bounded staleness risk, never
 * corruption.
 *
 * <p>The bound is on time only, not on size: there is no entry cap, so every distinct evicted key
 * of a NONSTRICT_READ_WRITE region is held for thirty to sixty minutes of running time, which means
 * a mass metadata import can retain on the order of 1e5 to 1e6 keys, tens of MB of transient heap,
 * for up to an hour.
 *
 * <p>A per-key eviction recorded with a timestamp later than a {@link #recordClearAll} timestamp
 * survives independently of that clear. The per-key generations are never wiped by a region clear,
 * and the region bar and the per-key bar both only ever move forward, so out-of-order clear and
 * eviction records cannot lower the per-key bar. A put must clear both bars.
 *
 * <p>Both generations live in one immutable {@code Generations} holder behind a single volatile
 * field. The guard check dereferences that field exactly once, so no rotation can interleave
 * between the two generation lookups of one {@link #isPutAllowed} call and hide a record younger
 * than a full window. The write path deliberately differs: {@link #recordEviction} rereads the
 * field on every iteration, which is how it notices that a rotation demoted the generation it just
 * wrote into and repeats the record, so a record always ends up in a generation whose full window
 * is still ahead of it.
 *
 * <p>The guard reads no wall-clock time anywhere, and neither side of it can be walked backwards by
 * a clock change. Refusal comparisons use Hibernate's cache timestamp clock, which this class does
 * not touch and which cannot regress: {@code SimpleTimestamper.next()} returns the greater of
 * {@code currentTimeMillis() << 12} and {@code previous + 1} under a CAS, so it is wall-seeded but
 * monotonic. A forward clock step raises eviction timestamps and transaction timestamps alike, and
 * a backward step is clamped to {@code previous + 1}, so refusal ordering survives clock steps in
 * either direction. Rotation uses {@link System#nanoTime}, the JVM's monotonic elapsed time source,
 * immune to clock steps, NTP adjustments and RTC changes. The consequence is that a window measures
 * JVM running time, not wall time: a suspended VM does not age generations. That is the coherent
 * measure, because in-flight transactions on this JVM do not progress while it is frozen either.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class EvictionGuard {
  /**
   * One window is the guaranteed retention, so one window must exceed the longest plausible
   * transaction. Thirty minutes covers the long DHIS2 transactions (analytics table generation,
   * metadata import); the class javadoc states the failure mode for a transaction that outlives its
   * records.
   */
  private static final long DEFAULT_WINDOW_NANOS = TimeUnit.MINUTES.toNanos(30);

  private final LongSupplier clock;
  private final long windowNanos;
  private final AtomicLong regionClearTimestamp = new AtomicLong(Long.MIN_VALUE);

  /** The only mutable state reachable by readers, always replaced as a whole. */
  private volatile Generations generations;

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
