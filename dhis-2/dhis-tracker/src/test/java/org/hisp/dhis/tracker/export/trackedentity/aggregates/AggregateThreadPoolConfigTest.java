/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AggregateThreadPoolConfigTest {

  @Mock private DhisConfigurationProvider config;

  /**
   * Stubs the raw property and lets the real {@link
   * DhisConfigurationProvider#getIntProperty(ConfigurationKey)} default method parse it, so the
   * tests exercise the same parsing production uses.
   */
  private void withDbPoolSize(String value) {
    when(config.getProperty(ConfigurationKey.CONNECTION_POOL_MAX_SIZE)).thenReturn(value);
    when(config.getIntProperty(ConfigurationKey.CONNECTION_POOL_MAX_SIZE)).thenCallRealMethod();
  }

  @Test
  void shouldSizeToTargetConcurrencyWhenThePoolIsLargeEnough() {
    withDbPoolSize("80");

    // 4 branches * 5 concurrent requests = 20, and 25% of 80 is 20, so the target fits exactly
    assertEquals(20, AggregateThreadPoolConfig.maxThreads(config));
  }

  @Test
  void shouldCapToThePoolShareWhenTheTargetWouldClaimTooMuch() {
    withDbPoolSize("20");

    // 25% of 20 is 5, well below the 20 threads the target concurrency wants
    assertEquals(5, AggregateThreadPoolConfig.maxThreads(config));
  }

  @Test
  void shouldKeepEnoughThreadsForOneRequestOnASmallPool() {
    withDbPoolSize("10");

    // 25% of 10 is 2, which would serialise most of a request. The floor lifts it to one request's
    // worth of parallelism, still under the pool size.
    assertEquals(4, AggregateThreadPoolConfig.maxThreads(config));
  }

  @Test
  void shouldPreferTheDeadlockBoundOverTheOneRequestFloorOnATinyPool() {
    withDbPoolSize("4");

    // The floor wants 4, but maxThreads must stay below the pool size or the Cm = 2 bound
    // pool_size >= Tn + 1 is violated by our own sizing.
    assertEquals(3, AggregateThreadPoolConfig.maxThreads(config));
  }

  @Test
  void shouldStayPositiveOnADegeneratePool() {
    withDbPoolSize("1");

    assertEquals(1, AggregateThreadPoolConfig.maxThreads(config));
  }

  /**
   * The Cm = 2 deadlock bound is {@code pool_size >= maxThreads * (Cm - 1) + 1}, i.e. {@code
   * maxThreads < pool_size}. Guards against a future share or target that violates it.
   */
  @ParameterizedTest
  @ValueSource(strings = {"2", "3", "4", "5", "8", "10", "20", "40", "80", "200"})
  void shouldStayBelowThePoolSizeToSatisfyTheDeadlockBound(String dbPoolSize) {
    withDbPoolSize(dbPoolSize);

    int maxThreads = AggregateThreadPoolConfig.maxThreads(config);

    assertTrue(
        maxThreads < Integer.parseInt(dbPoolSize),
        "maxThreads=%s must stay below connection.pool.max_size=%s"
            .formatted(maxThreads, dbPoolSize));
  }
}
