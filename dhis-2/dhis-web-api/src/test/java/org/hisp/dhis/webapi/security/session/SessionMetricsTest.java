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
package org.hisp.dhis.webapi.security.session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link SessionMetrics}. */
class SessionMetricsTest {

  @Test
  @DisplayName("Registration is a no-op without a meter registry")
  void nullRegistry_noOp() {
    SessionStatisticsProvider provider = mock(SessionStatisticsProvider.class);

    assertDoesNotThrow(() -> new SessionMetrics(provider, (MeterRegistry) null));
  }

  @Test
  @DisplayName("Active session gauges read live from the provider")
  void activeSessionGauges_readLive() {
    SessionStatisticsProvider provider = mock(SessionStatisticsProvider.class);
    when(provider.getSessionGauges()).thenReturn(new SessionStatisticsProvider.SessionGauges(3, 2));
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    new SessionMetrics(provider, registry);

    assertEquals(3.0, registry.get(SessionMetrics.ACTIVE_SESSIONS).gauge().value());
    assertEquals(2.0, registry.get(SessionMetrics.ACTIVE_SESSION_USERS).gauge().value());

    when(provider.getSessionGauges()).thenReturn(new SessionStatisticsProvider.SessionGauges(7, 5));

    assertEquals(7.0, registry.get(SessionMetrics.ACTIVE_SESSIONS).gauge().value());
    assertEquals(5.0, registry.get(SessionMetrics.ACTIVE_SESSION_USERS).gauge().value());
  }

  @Test
  @DisplayName("Http session gauge reads live from the provider")
  void httpSessions_readLive() {
    SessionStatisticsProvider provider = mock(SessionStatisticsProvider.class);
    when(provider.getHttpSessions()).thenReturn(4L).thenReturn(9L);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    new SessionMetrics(provider, registry);

    assertEquals(4.0, registry.get(SessionMetrics.HTTP_SESSIONS).gauge().value());
    assertEquals(9.0, registry.get(SessionMetrics.HTTP_SESSIONS).gauge().value());
  }

  @Test
  @DisplayName("Session lifecycle totals are exposed as function counters")
  void lifecycleTotals_readLive() {
    SessionStatisticsProvider provider = mock(SessionStatisticsProvider.class);
    when(provider.getSessionsCreatedTotal()).thenReturn(12L);
    when(provider.getSessionsDestroyedTotal()).thenReturn(5L);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    new SessionMetrics(provider, registry);

    assertEquals(
        12.0, registry.get(SessionMetrics.SESSIONS_CREATED_TOTAL).functionCounter().count());
    assertEquals(
        5.0, registry.get(SessionMetrics.SESSIONS_DESTROYED_TOTAL).functionCounter().count());
  }
}
