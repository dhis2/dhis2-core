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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.hisp.dhis.webapi.security.session.SessionStatisticsProvider;
import org.hisp.dhis.webapi.security.session.SessionStatisticsProvider.SessionGauges;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @author Stian Sandvold
 */
@ExtendWith(MockitoExtension.class)
class SessionStatisticsMBeanTest {

  @Mock private SessionStatisticsProvider sessionStatisticsProvider;

  @InjectMocks private SessionStatisticsMBean mBean;

  @Test
  void sessionGaugesAreExposed() {
    when(sessionStatisticsProvider.getSessionGauges()).thenReturn(new SessionGauges(5, 3));

    assertEquals(5, mBean.getActiveSessions());
    assertEquals(3, mBean.getActiveSessionUsers());
  }

  @Test
  void lifecycleCountersAreExposed() {
    when(sessionStatisticsProvider.getHttpSessions()).thenReturn(4L);
    when(sessionStatisticsProvider.getSessionsCreatedTotal()).thenReturn(9L);
    when(sessionStatisticsProvider.getSessionsDestroyedTotal()).thenReturn(5L);

    assertEquals(4, mBean.getHttpSessions());
    assertEquals(9, mBean.getSessionsCreatedTotal());
    assertEquals(5, mBean.getSessionsDestroyedTotal());
  }

  @Test
  void mBeanRegistersAndIsReadableViaJmx() throws Exception {
    when(sessionStatisticsProvider.getSessionGauges()).thenReturn(new SessionGauges(2, 1));
    when(sessionStatisticsProvider.getHttpSessions()).thenReturn(3L);
    when(sessionStatisticsProvider.getSessionsCreatedTotal()).thenReturn(7L);
    when(sessionStatisticsProvider.getSessionsDestroyedTotal()).thenReturn(4L);

    MBeanServer server = MBeanServerFactory.newMBeanServer();
    AnnotationMBeanExporter exporter = new AnnotationMBeanExporter();
    exporter.setServer(server);
    ObjectName name = new ObjectName(SessionStatisticsMBean.OBJECT_NAME);
    exporter.registerManagedResource(mBean, name);

    assertEquals(2L, server.getAttribute(name, "ActiveSessions"));
    assertEquals(1L, server.getAttribute(name, "ActiveSessionUsers"));
    assertEquals(3L, server.getAttribute(name, "HttpSessions"));
    assertEquals(7L, server.getAttribute(name, "SessionsCreatedTotal"));
    assertEquals(4L, server.getAttribute(name, "SessionsDestroyedTotal"));
  }
}
