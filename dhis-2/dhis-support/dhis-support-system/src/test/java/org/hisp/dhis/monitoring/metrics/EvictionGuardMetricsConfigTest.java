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
package org.hisp.dhis.monitoring.metrics;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import javax.persistence.EntityManagerFactory;
import org.hisp.dhis.cache.guard.EvictionGuardStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Tests that {@link EvictionGuardMetricsConfig} exposes the eviction guard counters per region,
 * that the counters keep tracking after they are bound, that the bean binds through Spring with no
 * monitoring flag set anywhere, and that binding tolerates a context without a {@link
 * MeterRegistry}. The last two are the point of the class: unlike its siblings in this package it
 * carries no condition, so the counters are visible on a default install.
 *
 * <p>Region names are prefixed {@code t6-} because {@link EvictionGuardStats} keeps a process wide
 * registry with no reset, so every test class in the build has to pick names nobody else uses.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class EvictionGuardMetricsConfigTest {

  @Test
  void registersGuardCountersPerRegion() {
    EvictionGuardStats.forRegion("t6-refused").countRefused();
    EvictionGuardStats.forRegion("t6-evicted").countSelfEvicted();
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    new EvictionGuardMetricsConfig().registerGuardMetrics(registry);

    assertEquals(
        1.0,
        registry
            .get("hibernate_l2_guard_refused_puts_total")
            .tag("region", "t6-refused")
            .functionCounter()
            .count());
    assertEquals(
        0.0,
        registry
            .get("hibernate_l2_guard_self_evictions_total")
            .tag("region", "t6-refused")
            .functionCounter()
            .count());
    assertEquals(
        1.0,
        registry
            .get("hibernate_l2_guard_self_evictions_total")
            .tag("region", "t6-evicted")
            .functionCounter()
            .count());
  }

  @Test
  void registersStoredPutsCounterPerRegion() {
    EvictionGuardStats stats = EvictionGuardStats.forRegion("t6-stored-puts");
    stats.countStoredPut();
    stats.countStoredPut();
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    new EvictionGuardMetricsConfig().registerGuardMetrics(registry);

    assertEquals(
        2.0,
        registry
            .get("hibernate_l2_guard_stored_puts_total")
            .tag("region", "t6-stored-puts")
            .functionCounter()
            .count());
    assertEquals(
        0.0,
        registry
            .get("hibernate_l2_guard_refused_puts_total")
            .tag("region", "t6-stored-puts")
            .functionCounter()
            .count());
  }

  @Test
  void guardCountersTrackLaterIncrements() {
    EvictionGuardStats stats = EvictionGuardStats.forRegion("t6-live");
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    new EvictionGuardMetricsConfig().registerGuardMetrics(registry);
    stats.countRefused();
    stats.countRefused();

    assertEquals(
        2.0,
        registry
            .get("hibernate_l2_guard_refused_puts_total")
            .tag("region", "t6-live")
            .functionCounter()
            .count());
  }

  @Test
  @SuppressWarnings("unchecked")
  void bindingWithoutMeterRegistryIsANoOp() {
    ObjectProvider<MeterRegistry> registryProvider = mock(ObjectProvider.class);

    assertDoesNotThrow(
        () -> new EvictionGuardMetricsConfig().bindEvictionGuardToRegistry(registryProvider));
  }

  @Test
  void bindsThroughSpringWithNoMonitoringFlagSet() {
    // the config class carries @DependsOn("entityManagerFactory") for boot ordering, so the context
    // needs a bean under exactly that name; a mock is enough because the binder never touches it
    EvictionGuardStats.forRegion("t6-spring").countRefused();
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.registerBean(
          "entityManagerFactory",
          EntityManagerFactory.class,
          () -> mock(EntityManagerFactory.class));
      context.registerBean(MeterRegistry.class, () -> registry);
      context.register(EvictionGuardMetricsConfig.class);
      context.refresh();

      assertEquals(
          1.0,
          registry
              .get("hibernate_l2_guard_refused_puts_total")
              .tag("region", "t6-spring")
              .functionCounter()
              .count());
    }
  }
}
