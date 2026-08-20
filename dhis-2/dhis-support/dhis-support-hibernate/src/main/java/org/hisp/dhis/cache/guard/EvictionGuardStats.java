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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Per-region counters for what {@link EvictionGuard} did: puts it let through and that stayed, puts
 * it refused up front, and puts it had already stored and then had to take back.
 *
 * <p>The registry is static on purpose, not out of laziness. The two ends of it cannot see each
 * other through Spring. The counting end is the Hibernate cache access strategy, which Hibernate
 * constructs deep inside its own region factory with no access to any DI container, so it cannot
 * have a registry injected. The reading end is the Micrometer binder, which lives in a different
 * Spring module and context altogether. A process wide static keyed by region name is the only
 * meeting point both ends already share. The counters are diagnostics, so a single JVM wide
 * registry is also exactly the right scope for them.
 *
 * <p>Counting is lock free: {@link LongAdder} trades a slightly more expensive read for contention
 * free increments, which is the correct trade here because increments happen on every guarded put
 * while reads happen only when metrics are scraped.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class EvictionGuardStats {
  private static final ConcurrentHashMap<String, EvictionGuardStats> REGISTRY =
      new ConcurrentHashMap<>();

  private final String regionName;
  private final LongAdder storedPuts = new LongAdder();
  private final LongAdder refused = new LongAdder();
  private final LongAdder selfEvicted = new LongAdder();

  private EvictionGuardStats(String regionName) {
    this.regionName = regionName;
  }

  /** Returns the counters for the given region, creating them once and reusing them after that. */
  public static EvictionGuardStats forRegion(String regionName) {
    return REGISTRY.computeIfAbsent(regionName, EvictionGuardStats::new);
  }

  /**
   * Returns every region's counters keyed by region name, as an unmodifiable view of the live
   * registry. It is a view, not a copy: regions registered later become visible through it, and the
   * counters it exposes keep moving. That is what a metrics binder wants, and no caller needs a
   * frozen snapshot.
   */
  public static Map<String, EvictionGuardStats> all() {
    return Collections.unmodifiableMap(REGISTRY);
  }

  /** Records that the guard refused one stale {@code putFromLoad}. */
  public void countRefused() {
    refused.increment();
  }

  /**
   * Records that a reader took back one value it had already stored, because a write landed between
   * the guard check and the store and the post store re-check saw the newer eviction. This counts
   * the reader undoing its own put, not writer bookkeeping: nothing counts {@link
   * EvictionGuard#recordEviction}.
   */
  public void countSelfEvicted() {
    selfEvicted.increment();
  }

  /**
   * Records that one {@code putFromLoad} passed both guard checks and left its value in the region
   * storage. Counted only when the guarded put reports success, so the three counters partition the
   * guarded puts: every call ends as exactly one of stored, refused or self evicted, never two of
   * them, with one branch left out of the partition on purpose. The guarded put also declines to
   * count anything when the superclass reports that it did not store the value at all, which counts
   * as neither a store nor a refusal by the guard. That branch is unreachable on Hibernate 5.6,
   * whose base {@code putFromLoad} always reports stored, and it is kept precisely because that is
   * an assumption about upstream rather than a guarantee: should an upgrade start reporting a
   * declined store, the three counters would no longer add up to the calls, and this is the branch
   * to count next.
   *
   * <p>This is the denominator the other two are read against, and it is also a cross check on the
   * guard's own reach. A NONSTRICT_READ_WRITE region has no other way into its storage: Hibernate
   * populates such a region through {@code putFromLoad} and nothing else, so per region the
   * storage's own put count has to equal stored puts plus self evictions, the self evictions being
   * the stores that were taken back right after they landed. A region whose {@code ehcache_puts}
   * drifts above that sum is being written to by a path that does not pass this class, and a path
   * the guard does not see is a path it cannot bar.
   */
  public void countStoredPut() {
    storedPuts.increment();
  }

  public long getRefused() {
    return refused.sum();
  }

  public long getSelfEvicted() {
    return selfEvicted.sum();
  }

  public long getStoredPuts() {
    return storedPuts.sum();
  }

  public String getRegionName() {
    return regionName;
  }
}
