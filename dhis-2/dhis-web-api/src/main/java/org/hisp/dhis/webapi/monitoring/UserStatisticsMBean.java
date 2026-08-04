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

import java.util.Map;
import org.hisp.dhis.datastatistics.DataStatisticsService;
import org.hisp.dhis.webapi.security.session.SessionStatisticsProvider;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Exposes user statistics (active HTTP sessions, logins by last login and active users by analytics
 * view activity) as a JMX MBean, for monitoring tools that poll MBean attributes such as Glowroot,
 * jconsole or a JMX exporter.
 *
 * <p>All attributes are served from caches (session gauges one minute, login and active user
 * windows five minutes, see {@code CacheProvider}), so frequent attribute polling never hits the
 * database more than once per cache window.
 *
 * <p>Registered only when {@code monitoring.jmx.enabled} is on in dhis.conf, see {@link
 * JmxMonitoringConfig}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ManagedResource(
    objectName = UserStatisticsMBean.OBJECT_NAME,
    description =
        "DHIS2 user statistics: active HTTP sessions, distinct users by last login and distinct"
            + " users by analytics view activity")
public class UserStatisticsMBean {

  public static final String OBJECT_NAME = "org.hisp.dhis:type=UserStatistics";

  private final DataStatisticsService dataStatisticsService;

  private final SessionStatisticsProvider sessionStatisticsProvider;

  public UserStatisticsMBean(
      DataStatisticsService dataStatisticsService,
      SessionStatisticsProvider sessionStatisticsProvider) {
    this.dataStatisticsService = dataStatisticsService;
    this.sessionStatisticsProvider = sessionStatisticsProvider;
  }

  @ManagedAttribute(description = "Number of active HTTP sessions")
  public long getActiveSessions() {
    return sessionStatisticsProvider.getSessionGauges().sessions();
  }

  @ManagedAttribute(description = "Number of distinct users with an active HTTP session")
  public long getActiveSessionUsers() {
    return sessionStatisticsProvider.getSessionGauges().users();
  }

  @ManagedAttribute(description = "Distinct users who logged in within the past hour")
  public int getLoginsPastHour() {
    return logins(0);
  }

  @ManagedAttribute(description = "Distinct users who logged in since the start of today")
  public int getLoginsToday() {
    return logins(1);
  }

  @ManagedAttribute(description = "Distinct users who logged in within the past 2 days")
  public int getLoginsPast2Days() {
    return logins(2);
  }

  @ManagedAttribute(description = "Distinct users who logged in within the past 7 days")
  public int getLoginsPast7Days() {
    return logins(7);
  }

  @ManagedAttribute(description = "Distinct users who logged in within the past 30 days")
  public int getLoginsPast30Days() {
    return logins(30);
  }

  @ManagedAttribute(description = "Distinct users with analytics view activity in the past hour")
  public int getActiveUsersPastHour() {
    return activeUsers(0);
  }

  @ManagedAttribute(
      description = "Distinct users with analytics view activity since the start of today")
  public int getActiveUsersToday() {
    return activeUsers(1);
  }

  @ManagedAttribute(description = "Distinct users with analytics view activity in the past 2 days")
  public int getActiveUsersPast2Days() {
    return activeUsers(2);
  }

  @ManagedAttribute(description = "Distinct users with analytics view activity in the past 7 days")
  public int getActiveUsersPast7Days() {
    return activeUsers(7);
  }

  @ManagedAttribute(description = "Distinct users with analytics view activity in the past 30 days")
  public int getActiveUsersPast30Days() {
    return activeUsers(30);
  }

  private int logins(int window) {
    return valueOrZero(dataStatisticsService.getSystemStatisticsOverview().getLogins(), window);
  }

  private int activeUsers(int window) {
    return valueOrZero(
        dataStatisticsService.getSystemStatisticsOverview().getActiveUsers(), window);
  }

  private static int valueOrZero(Map<Integer, Integer> byWindow, int window) {
    return byWindow == null ? 0 : byWindow.getOrDefault(window, 0);
  }
}
