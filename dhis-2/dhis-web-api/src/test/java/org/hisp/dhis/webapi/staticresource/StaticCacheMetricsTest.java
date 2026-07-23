/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.staticresource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StaticCacheMetrics}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class StaticCacheMetricsTest {

  @Test
  @DisplayName("All counters are no-ops without a meter registry")
  void nullRegistry_noOp() {
    StaticCacheMetrics metrics = new StaticCacheMetrics((MeterRegistry) null);

    assertDoesNotThrow(
        () -> {
          metrics.countRequest(StaticCacheMetrics.POLICY_DEFAULT);
          metrics.countNotModified();
          metrics.countHtmlRewrite(StaticCacheMetrics.REWRITE_MISS);
        });
  }

  @Test
  @DisplayName("Not modified counter accumulates increments")
  void notModified_increments() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    StaticCacheMetrics metrics = new StaticCacheMetrics(registry);

    metrics.countNotModified();
    metrics.countNotModified();

    assertEquals(2.0, registry.counter(StaticCacheMetrics.NOT_MODIFIED).count());
  }

  @Test
  @DisplayName("Request counter separates policies by tag")
  void requestCounter_separatesPolicies() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    StaticCacheMetrics metrics = new StaticCacheMetrics(registry);

    metrics.countRequest(StaticCacheMetrics.POLICY_IMMUTABLE);
    metrics.countRequest(StaticCacheMetrics.POLICY_IMMUTABLE);
    metrics.countRequest(StaticCacheMetrics.POLICY_NO_STORE);

    assertEquals(
        2.0,
        registry
            .counter(StaticCacheMetrics.REQUESTS, "policy", StaticCacheMetrics.POLICY_IMMUTABLE)
            .count());
    assertEquals(
        1.0,
        registry
            .counter(StaticCacheMetrics.REQUESTS, "policy", StaticCacheMetrics.POLICY_NO_STORE)
            .count());
  }
}
