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
package org.hisp.dhis.webapi.monitoring;

/**
 * Session statistics published over JMX as {@code org.hisp.dhis:type=SessionStatistics}, so that a
 * JMX consumer such as Glowroot can graph them as gauges.
 *
 * <p>Two different populations are exposed deliberately, and the gap between them is itself
 * diagnostic: it is the number of sessions held by clients that never performed an interactive
 * login. {@link #getActiveSessions()} counts only what Spring Security's session registry knows
 * about, while {@link #getHttpSessions()} counts every session the servlet container holds.
 */
public interface SessionStatisticsMXBean {

  /**
   * Sessions belonging to an authenticated principal, as Spring Security's session registry knows
   * them — the same population {@code GET /api/sessions} reports. Expired-but-not-yet-removed
   * sessions are excluded. {@code -1} if the configured session registry cannot enumerate
   * principals, which is the case for Redis-backed sessions.
   */
  long getActiveSessions();

  /** Distinct principals behind {@link #getActiveSessions()}, so repeat logins count once. */
  long getActiveSessionUsers();

  /**
   * Every HTTP session the servlet container currently holds, which is a strictly larger population
   * than {@link #getActiveSessions()}: it also counts API-token clients and unauthenticated
   * visitors whose request was saved for a post-login redirect.
   */
  long getHttpSessions();

  /** Sessions created since this instance started. Rises steeply if clients do not reuse them. */
  long getSessionsCreatedTotal();

  /** Sessions invalidated, logged out or expired since this instance started. */
  long getSessionsDestroyedTotal();
}
