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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;

class SessionStatisticsTest {

  private SessionRegistryImpl sessionRegistry;

  private SessionStatistics statistics;

  @BeforeEach
  void setUp() {
    sessionRegistry = new SessionRegistryImpl();
    statistics = new SessionStatistics(sessionRegistry);
  }

  @AfterEach
  void tearDown() {
    statistics.unregister();
  }

  @Test
  void httpSessionsCountsCreatedMinusDestroyed() {
    MockHttpSession first = new MockHttpSession();
    MockHttpSession second = new MockHttpSession();

    statistics.onSessionCreated(new HttpSessionCreatedEvent(first));
    statistics.onSessionCreated(new HttpSessionCreatedEvent(second));
    assertEquals(2, statistics.getHttpSessions());

    statistics.onSessionDestroyed(new HttpSessionDestroyedEvent(first));
    assertEquals(1, statistics.getHttpSessions());
    assertEquals(2, statistics.getSessionsCreatedTotal());
    assertEquals(1, statistics.getSessionsDestroyedTotal());
  }

  @Test
  void httpSessionsNeverGoesNegative() {
    statistics.onSessionDestroyed(new HttpSessionDestroyedEvent(new MockHttpSession()));

    assertEquals(0, statistics.getHttpSessions());
    assertEquals(1, statistics.getSessionsDestroyedTotal());
  }

  @Test
  void activeSessionsCountsRegisteredSessionsAndDistinctUsers() {
    sessionRegistry.registerNewSession("session-1", "alice");
    sessionRegistry.registerNewSession("session-2", "alice");
    sessionRegistry.registerNewSession("session-3", "bob");

    assertEquals(3, statistics.getActiveSessions());
    assertEquals(2, statistics.getActiveSessionUsers());
  }

  @Test
  void activeSessionsExcludesExpiredSessionsAndTheirUsers() {
    sessionRegistry.registerNewSession("session-1", "alice");
    sessionRegistry.registerNewSession("session-2", "bob");
    sessionRegistry.getSessionInformation("session-2").expireNow();

    assertEquals(1, statistics.getActiveSessions());
    assertEquals(1, statistics.getActiveSessionUsers());
  }

  @Test
  void activeSessionsAndHttpSessionsAreIndependentPopulations() {
    // An API-token client gets a container session but is never registered as a principal.
    statistics.onSessionCreated(new HttpSessionCreatedEvent(new MockHttpSession()));
    sessionRegistry.registerNewSession("session-1", "alice");

    assertEquals(1, statistics.getHttpSessions());
    assertEquals(1, statistics.getActiveSessions());
  }

  @Test
  void activeSessionsReportsUnavailableWhenTheRegistryCannotEnumerate() {
    SessionRegistry redisBacked =
        new SessionRegistryImpl() {
          @Override
          public java.util.List<Object> getAllPrincipals() {
            throw new UnsupportedOperationException("Spring Session cannot enumerate principals");
          }
        };
    SessionStatistics redisStatistics = new SessionStatistics(redisBacked);

    assertEquals(SessionStatistics.UNAVAILABLE, redisStatistics.getActiveSessions());
    assertEquals(SessionStatistics.UNAVAILABLE, redisStatistics.getActiveSessionUsers());
    assertEquals(0, redisStatistics.getHttpSessions());
  }

  @Test
  void allAttributesAreReadableThroughJmx() throws Exception {
    statistics.register();
    statistics.onSessionCreated(new HttpSessionCreatedEvent(new MockHttpSession()));
    sessionRegistry.registerNewSession("session-1", "alice");

    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    ObjectName name = new ObjectName(SessionStatistics.OBJECT_NAME);

    assertEquals(1L, server.getAttribute(name, "ActiveSessions"));
    assertEquals(1L, server.getAttribute(name, "ActiveSessionUsers"));
    assertEquals(1L, server.getAttribute(name, "HttpSessions"));
    assertEquals(1L, server.getAttribute(name, "SessionsCreatedTotal"));
    assertEquals(0L, server.getAttribute(name, "SessionsDestroyedTotal"));
  }
}
