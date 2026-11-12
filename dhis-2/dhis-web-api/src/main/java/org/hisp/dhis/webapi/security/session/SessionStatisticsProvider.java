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

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.user.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

/**
 * Provides gauges for currently active HTTP sessions and the number of distinct users holding them,
 * plus servlet container session lifecycle counters. Gauge values are cached briefly because
 * enumerating sessions is O(number of users) with a Redis-backed session registry, while consumers
 * (Prometheus scrapes, JMX polling) run on fixed intervals.
 *
 * <p>The container session count is kept from HTTP session lifecycle events, so reading it is O(1)
 * and a session is counted from the moment it is created until it is invalidated, logged out or
 * expired. The lifecycle counts are per JVM and reset on restart.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @author Stian Sandvold
 */
@Component
public class SessionStatisticsProvider {

  /** Number of active (non-expired) HTTP sessions and distinct users holding them. */
  public record SessionGauges(long sessions, long users) {}

  private final AtomicLong httpSessions = new AtomicLong();

  private final AtomicLong sessionsCreatedTotal = new AtomicLong();

  private final AtomicLong sessionsDestroyedTotal = new AtomicLong();

  private final UserService userService;

  private final Cache<SessionGauges> sessionGaugeCache;

  public SessionStatisticsProvider(UserService userService, CacheProvider cacheProvider) {
    this.userService = userService;
    this.sessionGaugeCache = cacheProvider.createDataSummarySessionGaugesCache();
  }

  /** Returns the current session gauges, cached for one minute. */
  public SessionGauges getSessionGauges() {
    return sessionGaugeCache.get("gauges", key -> computeSessionGauges());
  }

  @EventListener
  public void onSessionCreated(HttpSessionCreatedEvent event) {
    sessionsCreatedTotal.incrementAndGet();
    httpSessions.incrementAndGet();
  }

  @EventListener
  public void onSessionDestroyed(HttpSessionDestroyedEvent event) {
    sessionsDestroyedTotal.incrementAndGet();
    httpSessions.updateAndGet(held -> held > 0 ? held - 1 : 0);
  }

  /**
   * Every HTTP session the servlet container currently holds, which is a strictly larger population
   * than {@link SessionGauges#sessions()}: it also counts API-token clients and unauthenticated
   * visitors whose request was saved for a post-login redirect.
   */
  public long getHttpSessions() {
    return httpSessions.get();
  }

  /** Sessions created since this instance started. Rises steeply if clients do not reuse them. */
  public long getSessionsCreatedTotal() {
    return sessionsCreatedTotal.get();
  }

  /** Sessions invalidated, logged out or expired since this instance started. */
  public long getSessionsDestroyedTotal() {
    return sessionsDestroyedTotal.get();
  }

  private SessionGauges computeSessionGauges() {
    List<SessionInformation> sessions = userService.listAllSessions();
    long active = sessions.stream().filter(s -> !s.isExpired()).count();
    long users =
        sessions.stream()
            .filter(s -> !s.isExpired())
            .map(SessionStatisticsProvider::sessionUsername)
            .distinct()
            .count();
    return new SessionGauges(active, users);
  }

  /** The registry holds UserDetails principals in-memory but plain usernames under Redis. */
  private static String sessionUsername(SessionInformation session) {
    Object principal = session.getPrincipal();
    return principal instanceof UserDetails userDetails
        ? userDetails.getUsername()
        : String.valueOf(principal);
  }
}
