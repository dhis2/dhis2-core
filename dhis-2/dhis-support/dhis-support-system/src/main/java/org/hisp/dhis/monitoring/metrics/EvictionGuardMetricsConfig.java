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
package org.hisp.dhis.monitoring.metrics;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.guard.EvictionGuardStats;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Exposes the second-level cache eviction guard counters, three per region the guard has seen.
 *
 * <p>Ungated on purpose, unlike the sibling {@code *MetricsConfig} classes: those cost something
 * (Hibernate statistics, reflective cache walks, JVM polling) and default to off. These counters
 * are three {@code LongAdder}s per region that the guard increments anyway, so exposing them costs
 * one {@link FunctionCounter} registration each at boot and nothing after that. They are also the
 * guard's only observability, and a stale-cache incident is diagnosed after the fact, not after a
 * config flip and a restart.
 *
 * <p>Binding is a no-op when the context holds no {@link MeterRegistry}, the same tolerance as
 * {@code StaticCacheMetrics}. The {@code region} tag carries the full Hibernate region name (the
 * entity or collection FQN, also the JCache cache name behind the {@code ehcache_*} series), kept
 * unshortened so the two series stay joinable and unambiguous.
 *
 * <p>{@code @DependsOn("entityManagerFactory")} is ordering only: the guard registers a region's
 * counters while Hibernate builds that region, so this binder must run after the
 * EntityManagerFactory exists or it would find the registry empty. Regions are fixed at boot; none
 * turn up later.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Configuration
@DependsOn("entityManagerFactory")
public class EvictionGuardMetricsConfig {

  @Autowired
  public void bindEvictionGuardToRegistry(ObjectProvider<MeterRegistry> registryProvider) {
    MeterRegistry registry = registryProvider.getIfAvailable();
    if (registry == null) {
      log.debug("No MeterRegistry present, eviction guard counters not exposed.");
      return;
    }
    registerGuardMetrics(registry);
  }

  /**
   * Registers the counters of every region the guard has seen. The counters are function counters
   * over the live per-region adders, so they keep tracking after registration.
   */
  void registerGuardMetrics(MeterRegistry registry) {
    Map<String, EvictionGuardStats> guardStats = EvictionGuardStats.all();
    if (guardStats.isEmpty()) {
      log.debug("No eviction guard regions registered, guard counters not exposed.");
      return;
    }
    log.info("Registering eviction guard counters for {} regions.", guardStats.size());

    for (EvictionGuardStats stats : guardStats.values()) {
      Tags guardTags = Tags.of(Tag.of("region", stats.getRegionName()));

      FunctionCounter.builder(
              "hibernate_l2_guard_refused_puts_total", stats, EvictionGuardStats::getRefused)
          .tags(guardTags)
          .description(
              "The total number of second-level cache puts the eviction guard refused because the transaction's caching timestamp predates the key's last recorded eviction or the region's last recorded clear. A nonzero count is expected in normal operation, not an incident: DHIS2 clears whole regions on metadata mutations, and sessions older than a clear then have their puts refused. Read it as the volume of refused late puts; hibernate_l2_guard_self_evictions_total is the signal that the mid-put race fired")
          .register(registry);

      FunctionCounter.builder(
              "hibernate_l2_guard_self_evictions_total", stats, EvictionGuardStats::getSelfEvicted)
          .tags(guardTags)
          .description(
              "The total number of values a reader stored and then took back, because a write landed between the guard check and the store and the post-store re-check saw the newer eviction. A reader undoing its own put, not writer bookkeeping")
          .register(registry);

      FunctionCounter.builder(
              "hibernate_l2_guard_stored_puts_total", stats, EvictionGuardStats::getStoredPuts)
          .tags(guardTags)
          .description(
              "The total number of second-level cache puts the guard let through and that stayed in the region's storage: the up-front check passed, the value was stored, and the post-store re-check still allowed it. A nonzero and growing count is normal operation, not an incident, it is cache misses reloading rows into the region. It is the denominator the other two guard counters are read against, and per region it should account for the region's own put count together with hibernate_l2_guard_self_evictions_total, since a NONSTRICT_READ_WRITE region is only ever populated through putFromLoad: an ehcache_puts series above that sum means something writes to the region without passing the guard")
          .register(registry);
    }
  }
}
