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
package org.hisp.dhis.webapi.monitoring;

import org.hisp.dhis.webapi.security.session.SessionStatisticsProvider;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Exposes HTTP session statistics as a JMX MBean, for monitoring tools that poll MBean attributes
 * such as Glowroot, jconsole or a JMX exporter.
 *
 * <p>Two different populations are exposed deliberately, and the gap between them is itself
 * diagnostic: it is the number of sessions held by clients that never performed an interactive
 * login. {@code ActiveSessions} counts only what Spring Security's session registry knows about
 * (the same population {@code GET /api/sessions} reports, cached one minute), while {@code
 * HttpSessions} counts every session the servlet container holds in this JVM.
 *
 * <p>Registered only when {@code monitoring.jmx.enabled} is on in dhis.conf, see {@link
 * JmxMonitoringConfig}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @author Stian Sandvold
 */
@ManagedResource(
    objectName = SessionStatisticsMBean.OBJECT_NAME,
    description =
        "DHIS2 HTTP session statistics: registry-known active sessions and users, servlet"
            + " container sessions, and per-JVM session lifecycle totals")
public class SessionStatisticsMBean {

  public static final String OBJECT_NAME = "org.hisp.dhis:type=SessionStatistics";

  private final SessionStatisticsProvider sessionStatisticsProvider;

  public SessionStatisticsMBean(SessionStatisticsProvider sessionStatisticsProvider) {
    this.sessionStatisticsProvider = sessionStatisticsProvider;
  }

  @ManagedAttribute(
      description =
          "Active (non-expired) HTTP sessions known to the session registry, same population as"
              + " GET /api/sessions")
  public long getActiveSessions() {
    return sessionStatisticsProvider.getSessionGauges().sessions();
  }

  @ManagedAttribute(description = "Distinct users behind ActiveSessions, repeat logins count once")
  public long getActiveSessionUsers() {
    return sessionStatisticsProvider.getSessionGauges().users();
  }

  @ManagedAttribute(
      description =
          "Every HTTP session the servlet container currently holds in this JVM, a superset of"
              + " ActiveSessions")
  public long getHttpSessions() {
    return sessionStatisticsProvider.getHttpSessions();
  }

  @ManagedAttribute(description = "Sessions created since this instance started (per JVM)")
  public long getSessionsCreatedTotal() {
    return sessionStatisticsProvider.getSessionsCreatedTotal();
  }

  @ManagedAttribute(
      description = "Sessions invalidated, logged out or expired since this instance started")
  public long getSessionsDestroyedTotal() {
    return sessionStatisticsProvider.getSessionsDestroyedTotal();
  }
}
