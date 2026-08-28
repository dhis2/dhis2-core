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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Per-region counters for {@link EvictionGuard} outcomes: puts stored, puts refused up front, and
 * puts stored and then taken back.
 *
 * <p>The registry is static because its two ends cannot meet through Spring: the counting end is a
 * Hibernate cache access strategy built deep inside the region factory with no access to any DI
 * container, the reading end is a Micrometer binder in another module. A process-wide static keyed
 * by region name is the only meeting point both ends share, and JVM scope is the right scope for
 * diagnostics. {@link LongAdder} keeps increments contention-free; reads happen only on metrics
 * scrapes.
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
   * registry: regions registered later become visible through it, and the counters keep moving,
   * which is what a metrics binder wants.
   */
  public static Map<String, EvictionGuardStats> all() {
    return Collections.unmodifiableMap(REGISTRY);
  }

  /** Records that the guard refused one stale {@code putFromLoad}. */
  public void countRefused() {
    refused.increment();
  }

  /**
   * Records that a reader took back a value it had already stored: a write landed between the guard
   * check and the store, and the post-store re-check saw the newer eviction. Counts the reader
   * undoing its own put, never writer bookkeeping.
   */
  public void countSelfEvicted() {
    selfEvicted.increment();
  }

  /**
   * Records a {@code putFromLoad} that passed both guard checks and stayed in storage. The three
   * counters partition the guarded puts: every call ends as exactly one of stored, refused or
   * self-evicted. One branch is left out on purpose: when the superclass reports that it did not
   * store, nothing is counted. That branch is unreachable on Hibernate 5.6, whose base {@code
   * putFromLoad} always reports stored; if an upgrade changes that, the three counters stop adding
   * up to the calls, and this is the branch to count next.
   *
   * <p>This counter is also a cross-check on the guard's reach: a NONSTRICT_READ_WRITE region is
   * populated only through {@code putFromLoad}, so per region {@code ehcache_puts} must equal
   * stored puts plus self-evictions. Drift above that sum means a write path bypasses the guard,
   * and a path the guard does not see is a path it cannot bar.
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
