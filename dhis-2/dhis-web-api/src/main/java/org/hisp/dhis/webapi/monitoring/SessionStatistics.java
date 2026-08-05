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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

/**
 * Maintains {@link SessionStatisticsMXBean} and registers it on the platform MBean server.
 *
 * <p>The container session count is kept from HTTP session lifecycle events, so reading it is O(1)
 * and a session is counted from the moment it is created until it is invalidated, logged out or
 * expired. The authenticated counts have to enumerate the session registry, so they are recomputed
 * at most once per {@link #REGISTRY_CACHE_MILLIS} however often the MBean is polled.
 *
 * <p>All counts are per JVM.
 */
@Slf4j
@Component
public class SessionStatistics implements SessionStatisticsMXBean {

  public static final String OBJECT_NAME = "org.hisp.dhis:type=SessionStatistics";

  /** Shorter than any sane gauge collection interval, so a poller always sees a fresh value. */
  static final long REGISTRY_CACHE_MILLIS = 2_000;

  /** Reported when the session registry cannot enumerate principals, rather than a wrong zero. */
  static final long UNAVAILABLE = -1;

  private final SessionRegistry sessionRegistry;

  private final AtomicLong httpSessions = new AtomicLong();

  private final AtomicLong sessionsCreatedTotal = new AtomicLong();

  private final AtomicLong sessionsDestroyedTotal = new AtomicLong();

  private volatile RegistryCounts registryCounts = new RegistryCounts(0, 0, 0);

  private ObjectName objectName;

  public SessionStatistics(SessionRegistry sessionRegistry) {
    this.sessionRegistry = sessionRegistry;
  }

  private record RegistryCounts(long sessions, long users, long takenAtMillis) {}

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

  @Override
  public long getActiveSessions() {
    return registryCounts().sessions();
  }

  @Override
  public long getActiveSessionUsers() {
    return registryCounts().users();
  }

  @Override
  public long getHttpSessions() {
    return httpSessions.get();
  }

  @Override
  public long getSessionsCreatedTotal() {
    return sessionsCreatedTotal.get();
  }

  @Override
  public long getSessionsDestroyedTotal() {
    return sessionsDestroyedTotal.get();
  }

  /**
   * Two callers arriving together may both recompute. That is cheaper than locking, and they
   * compute the same answer.
   */
  private RegistryCounts registryCounts() {
    long now = System.currentTimeMillis();
    RegistryCounts cached = registryCounts;
    if (now - cached.takenAtMillis() < REGISTRY_CACHE_MILLIS) {
      return cached;
    }
    RegistryCounts fresh = countRegistry(now);
    registryCounts = fresh;
    return fresh;
  }

  private RegistryCounts countRegistry(long now) {
    long sessions = 0;
    long users = 0;
    try {
      for (Object principal : sessionRegistry.getAllPrincipals()) {
        int held = sessionRegistry.getAllSessions(principal, false).size();
        if (held > 0) {
          sessions += held;
          users++;
        }
      }
    } catch (UnsupportedOperationException ex) {
      // Redis-backed registries cannot enumerate principals. Cache the verdict so that a poller
      // does not pay for the exception on every read.
      return new RegistryCounts(UNAVAILABLE, UNAVAILABLE, now);
    }
    return new RegistryCounts(sessions, users, now);
  }

  @PostConstruct
  public void register() {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
      objectName = new ObjectName(OBJECT_NAME);
      server.registerMBean(this, objectName);
      log.info("Registered session statistics MBean: {}", OBJECT_NAME);
    } catch (Exception ex) {
      objectName = null;
      log.warn("Could not register session statistics MBean: {}", OBJECT_NAME, ex);
    }
  }

  @PreDestroy
  public void unregister() {
    if (objectName == null) {
      return;
    }
    try {
      ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName);
    } catch (Exception ex) {
      log.warn("Could not unregister session statistics MBean: {}", OBJECT_NAME, ex);
    } finally {
      objectName = null;
    }
  }
}
