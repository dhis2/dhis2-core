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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.hisp.dhis.datastatistics.DataStatisticsService;
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.webapi.security.config.AuthenticationListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class UserStatisticsMBeanTest {

  @Mock private DataStatisticsService dataStatisticsService;

  private SimpleMeterRegistry meterRegistry;

  private UserStatisticsMBean mBean;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    mBean = new UserStatisticsMBean(dataStatisticsService, meterRegistry);
  }

  @Test
  void loginsSinceStartupMirrorTheLoginCounter() {
    count(AuthenticationListener.LOGIN_METHOD_FORM, 3);
    count(AuthenticationListener.LOGIN_METHOD_BASIC, 2);
    count(AuthenticationListener.LOGIN_METHOD_OIDC, 1);

    assertEquals(3, mBean.getLoginsSinceStartupForm());
    assertEquals(2, mBean.getLoginsSinceStartupBasic());
    assertEquals(1, mBean.getLoginsSinceStartupOidc());
    assertEquals(0, mBean.getLoginsSinceStartupApiToken());
    assertEquals(6, mBean.getLoginsSinceStartupTotal());
  }

  @Test
  void loginsSinceStartupAreZeroBeforeAnyLogin() {
    assertEquals(0, mBean.getLoginsSinceStartupForm());
    assertEquals(0, mBean.getLoginsSinceStartupTotal());
  }

  @Test
  void loginAndActiveUserWindowsAreMapped() {
    DataSummary overview = new DataSummary();
    overview.setLogins(Map.of(0, 1, 1, 2, 2, 3, 7, 4, 30, 5));
    overview.setActiveUsers(Map.of(0, 6, 1, 7, 2, 8, 7, 9, 30, 10));
    when(dataStatisticsService.getSystemStatisticsOverview()).thenReturn(overview);

    assertEquals(1, mBean.getLoginsPastHour());
    assertEquals(2, mBean.getLoginsToday());
    assertEquals(3, mBean.getLoginsPast2Days());
    assertEquals(4, mBean.getLoginsPast7Days());
    assertEquals(5, mBean.getLoginsPast30Days());
    assertEquals(6, mBean.getActiveUsersPastHour());
    assertEquals(7, mBean.getActiveUsersToday());
    assertEquals(8, mBean.getActiveUsersPast2Days());
    assertEquals(9, mBean.getActiveUsersPast7Days());
    assertEquals(10, mBean.getActiveUsersPast30Days());
  }

  @Test
  void missingWindowsDefaultToZero() {
    when(dataStatisticsService.getSystemStatisticsOverview()).thenReturn(new DataSummary());

    assertEquals(0, mBean.getLoginsPastHour());
    assertEquals(0, mBean.getActiveUsersPast30Days());
  }

  @Test
  void mBeanRegistersAndIsReadableViaJmx() throws Exception {
    count(AuthenticationListener.LOGIN_METHOD_API_TOKEN, 8);
    DataSummary overview = new DataSummary();
    overview.setLogins(Map.of(1, 42));
    when(dataStatisticsService.getSystemStatisticsOverview()).thenReturn(overview);

    MBeanServer server = MBeanServerFactory.newMBeanServer();
    AnnotationMBeanExporter exporter = new AnnotationMBeanExporter();
    exporter.setServer(server);
    ObjectName name = new ObjectName(UserStatisticsMBean.OBJECT_NAME);
    exporter.registerManagedResource(mBean, name);

    assertEquals(42, server.getAttribute(name, "LoginsToday"));
    assertEquals(0, server.getAttribute(name, "LoginsPastHour"));
    assertEquals(8L, server.getAttribute(name, "LoginsSinceStartupApiToken"));
    assertEquals(8L, server.getAttribute(name, "LoginsSinceStartupTotal"));
  }

  private void count(String method, int times) {
    for (int i = 0; i < times; i++) {
      meterRegistry
          .counter(AuthenticationListener.LOGIN_COUNTER_NAME, "method", method)
          .increment();
    }
  }
}
