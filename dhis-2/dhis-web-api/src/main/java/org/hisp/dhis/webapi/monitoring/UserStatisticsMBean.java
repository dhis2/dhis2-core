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

import static org.hisp.dhis.webapi.security.config.AuthenticationListener.LOGIN_COUNTER_NAME;
import static org.hisp.dhis.webapi.security.config.AuthenticationListener.LOGIN_METHOD_API_TOKEN;
import static org.hisp.dhis.webapi.security.config.AuthenticationListener.LOGIN_METHOD_BASIC;
import static org.hisp.dhis.webapi.security.config.AuthenticationListener.LOGIN_METHOD_FORM;
import static org.hisp.dhis.webapi.security.config.AuthenticationListener.LOGIN_METHOD_OIDC;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.hisp.dhis.datastatistics.DataStatisticsService;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Exposes user statistics (logins by last login, active users by analytics view activity, and
 * successful authentications since startup by method) as a JMX MBean, for monitoring tools that
 * poll MBean attributes such as Glowroot, jconsole or a JMX exporter.
 *
 * <p>The windowed {@code Logins*} and {@code ActiveUsers*} attributes count distinct users and are
 * served from caches (five minutes, see {@code CacheProvider}), so frequent attribute polling never
 * hits the database more than once per cache window. The {@code LoginsSinceStartup*} attributes
 * mirror the Prometheus counter {@code dhis2_user_logins_total} and count successful authentication
 * events per JVM since startup: "form" and "oidc" track discrete logins, while "basic" and
 * "apitoken" re-authenticate on every request without a session and therefore track authenticated
 * request volume.
 *
 * <p>Registered only when {@code monitoring.jmx.enabled} is on in dhis.conf, see {@link
 * JmxMonitoringConfig}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ManagedResource(
    objectName = UserStatisticsMBean.OBJECT_NAME,
    description =
        "DHIS2 user statistics: distinct users by last login, distinct users by analytics view"
            + " activity, and successful authentications since startup by method")
public class UserStatisticsMBean {

  public static final String OBJECT_NAME = "org.hisp.dhis:type=UserStatistics";

  private final DataStatisticsService dataStatisticsService;

  private final MeterRegistry meterRegistry;

  public UserStatisticsMBean(
      DataStatisticsService dataStatisticsService, MeterRegistry meterRegistry) {
    this.dataStatisticsService = dataStatisticsService;
    this.meterRegistry = meterRegistry;
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

  @ManagedAttribute(
      description = "Successful form logins (username/password, LDAP, 2FA) since startup")
  public long getLoginsSinceStartupForm() {
    return loginsSinceStartup(LOGIN_METHOD_FORM);
  }

  @ManagedAttribute(
      description =
          "Successful Basic Auth authentications since startup, one per session-less" + " request")
  public long getLoginsSinceStartupBasic() {
    return loginsSinceStartup(LOGIN_METHOD_BASIC);
  }

  @ManagedAttribute(description = "Successful OIDC logins since startup")
  public long getLoginsSinceStartupOidc() {
    return loginsSinceStartup(LOGIN_METHOD_OIDC);
  }

  @ManagedAttribute(
      description = "Successful API token (PAT) authentications since startup, one per request")
  public long getLoginsSinceStartupApiToken() {
    return loginsSinceStartup(LOGIN_METHOD_API_TOKEN);
  }

  @ManagedAttribute(description = "Successful authentications since startup across all methods")
  public long getLoginsSinceStartupTotal() {
    return meterRegistry.find(LOGIN_COUNTER_NAME).counters().stream()
        .mapToLong(counter -> (long) counter.count())
        .sum();
  }

  private long loginsSinceStartup(String method) {
    Counter counter = meterRegistry.find(LOGIN_COUNTER_NAME).tag("method", method).counter();
    return counter == null ? 0 : (long) counter.count();
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
