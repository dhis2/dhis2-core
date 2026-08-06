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

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import javax.annotation.CheckForNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Registers {@link SessionStatisticsProvider} session statistics as Micrometer metrics on the
 * {@code /api/metrics} Prometheus scrape endpoint, mirroring the JMX {@code SessionStatisticsMBean}
 * attributes. Distinct from {@code data_summary_active_sessions} on {@code
 * /api/dataSummary/metrics}, which is a separate, database-backed endpoint gated by {@code
 * F_PERFORM_MAINTENANCE}.
 *
 * <p>Every meter here is function-backed: it reads {@link SessionStatisticsProvider} live on each
 * scrape rather than caching its own copy, so this view can never drift from the JMX MBean, which
 * reads the same provider.
 *
 * <p><b>Naming note:</b> the two lifecycle counters are named {@code dhis2_sessions_started_total}
 * / {@code dhis2_sessions_ended_total}, not {@code _created_total} / {@code _destroyed_total} as
 * the JMX {@code SessionStatisticsMBean} attributes ({@code SessionsCreatedTotal} / {@code
 * SessionsDestroyedTotal}) would suggest. This is deliberate, not an inconsistency to "fix":
 * OpenMetrics reserves the {@code _created} suffix for a companion created-timestamp series, so
 * Prometheus's naming sanitizer strips {@code _created} (along with {@code _total}) from a counter
 * name before re-appending {@code _total} — {@code dhis2_sessions_created_total} would therefore
 * actually be scraped as {@code dhis2_sessions_total}, silently losing the "created" meaning and
 * colliding in appearance with a gauge-like total. Renaming to {@code started}/{@code ended}
 * sidesteps the reserved word entirely so the exposed name matches the constant.
 *
 * <p>No-ops when no {@link MeterRegistry} is available in the application context.
 *
 * @author Jason Pickering
 */
@Component
public class SessionMetrics {

  static final String ACTIVE_SESSIONS = "dhis2_active_sessions";
  static final String ACTIVE_SESSION_USERS = "dhis2_active_session_users";
  static final String HTTP_SESSIONS = "dhis2_http_sessions";
  static final String SESSIONS_STARTED_TOTAL = "dhis2_sessions_started_total";
  static final String SESSIONS_ENDED_TOTAL = "dhis2_sessions_ended_total";

  @Autowired
  public SessionMetrics(
      SessionStatisticsProvider provider, ObjectProvider<MeterRegistry> registryProvider) {
    this(provider, registryProvider.getIfAvailable());
  }

  SessionMetrics(SessionStatisticsProvider provider, @CheckForNull MeterRegistry registry) {
    if (registry == null) {
      return;
    }

    Gauge.builder(ACTIVE_SESSIONS, provider, p -> p.getSessionGauges().sessions())
        .description("Active (non-expired) HTTP sessions known to the session registry")
        .register(registry);

    Gauge.builder(ACTIVE_SESSION_USERS, provider, p -> p.getSessionGauges().users())
        .description("Distinct users behind dhis2_active_sessions")
        .register(registry);

    Gauge.builder(HTTP_SESSIONS, provider, SessionStatisticsProvider::getHttpSessions)
        .description("Every HTTP session the servlet container currently holds in this JVM")
        .register(registry);

    FunctionCounter.builder(
            SESSIONS_STARTED_TOTAL, provider, SessionStatisticsProvider::getSessionsCreatedTotal)
        .description("Sessions created since this instance started")
        .register(registry);

    FunctionCounter.builder(
            SESSIONS_ENDED_TOTAL, provider, SessionStatisticsProvider::getSessionsDestroyedTotal)
        .description("Sessions invalidated, logged out or expired since this instance started")
        .register(registry);
  }
}
