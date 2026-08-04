/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.datasummary;

import static org.hisp.dhis.security.Authorities.F_PERFORM_MAINTENANCE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.common.Dhis2Info;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.Region;
import org.hisp.dhis.datastatistics.DataStatisticsService;
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.utils.PrometheusTextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * dataSummary endpoint to access System Statistics
 *
 * @author Joao Antunes
 */
@OpenApi.Document(
    entity = DataSummary.class,
    classifiers = {"team:analytics", "purpose:analytics"})
@Controller
@RequestMapping("/api/dataSummary")
public class DataSummaryController {

  /**
   * Session gauge values for the Prometheus endpoint, cached briefly because enumerating sessions
   * is O(number of users) with a Redis-backed session registry and Prometheus scrapes on a fixed
   * interval.
   */
  private static final Cache<SessionGauges> SESSION_GAUGE_CACHE =
      new SimpleCacheBuilder<SessionGauges>()
          .forRegion(Region.dataSummarySessionGauges.name())
          .expireAfterWrite(60, TimeUnit.SECONDS)
          .withInitialCapacity(1)
          .withMaximumSize(1)
          .build();

  private record SessionGauges(long sessions, long users) {}

  private final DataStatisticsService dataStatisticsService;

  private final UserService userService;

  @Autowired
  public DataSummaryController(
      DataStatisticsService dataStatisticsService, UserService userService) {
    this.dataStatisticsService = dataStatisticsService;
    this.userService = userService;
  }

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  public @ResponseBody DataSummary getStatistics() {
    return dataStatisticsService.getSystemStatisticsSummary();
  }

  /**
   * Appends system information metrics to the Prometheus metrics.
   *
   * @param systemInfo the system information containing version, commit, revision, and system ID
   */
  public void appendSystemInfoMetrics(PrometheusTextBuilder metrics, Dhis2Info systemInfo) {
    String metricName = "data_summary_build_info";
    if (systemInfo != null) {
      metrics.addHelp(metricName, "Build information");
      metrics.addType(metricName);
      long buildTime = 0L;
      if (systemInfo.getBuildTime() != null) {
        buildTime = systemInfo.getBuildTime().toInstant().getEpochSecond();
      }
      metrics.append(
          String.format(
              "%s{version=\"%s\", commit=\"%s\"} %s%n",
              metricName, systemInfo.getVersion(), systemInfo.getRevision(), buildTime));

      metrics.addHelp("data_summary_system_id", "System ID");
      metrics.addType("data_summary_system_id");
      metrics.append(
          String.format("data_summary_system_id{system_id=\"%s\"} 1%n", systemInfo.getSystemId()));
    }
  }

  @GetMapping(value = "/metrics", produces = TEXT_PLAIN_VALUE)
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  public @ResponseBody String getPrometheusMetrics() {
    DataSummary summary = dataStatisticsService.getSystemStatisticsSummary();

    PrometheusTextBuilder metrics = new PrometheusTextBuilder();

    metrics.addMetrics(
        summary.getObjectCounts(),
        "data_summary_object_counts",
        "type",
        "Count of metadata objects");

    metrics.addMetrics(
        summary.getActiveUsers(),
        "data_summary_active_users",
        "days",
        "Count of active users by day");

    metrics.addMetrics(
        summary.getLogins(), "data_summary_logins", "days", "Count of user logins by day");

    metrics.addMetrics(
        summary.getUserInvitations(),
        "data_summary_user_invitations",
        "type",
        "Count of user invitations");

    metrics.addMetrics(
        summary.getDataValueCount(),
        "data_summary_data_value_count",
        "days",
        "Count of updated data values by day");

    metrics.addMetrics(
        summary.getEventCount(),
        "data_summary_event_count",
        "days",
        "Count of updated events by day");

    metrics.addMetrics(
        summary.getTrackerEventCount(),
        "data_summary_tracker_event_count",
        "days",
        "Count of updated tracker events by day");

    metrics.addMetrics(
        summary.getSingleEventCount(),
        "data_summary_single_event_count",
        "days",
        "Count of updated single events by day");

    metrics.addMetrics(
        summary.getEnrollmentCount(),
        "data_summary_enrollment_count",
        "days",
        "Count of updated enrollments by day");

    appendSessionMetrics(metrics);

    appendSystemInfoMetrics(metrics, summary.getSystem());
    return metrics.getMetrics();
  }

  /**
   * Appends gauges for currently active HTTP sessions and the number of distinct users holding
   * them. Values are cached for one minute, see {@link #SESSION_GAUGE_CACHE}. Sessions only exist
   * for browser clients; PAT/basic/JWT clients are session-less and covered by the user statistics
   * endpoints instead.
   */
  private void appendSessionMetrics(PrometheusTextBuilder metrics) {
    SessionGauges gauges = SESSION_GAUGE_CACHE.get("gauges", key -> computeSessionGauges());

    metrics.addHelp("data_summary_active_sessions", "Number of active HTTP sessions");
    metrics.addType("data_summary_active_sessions");
    metrics.append(String.format("data_summary_active_sessions %d%n", gauges.sessions()));

    metrics.addHelp(
        "data_summary_active_session_users", "Number of distinct users with an active session");
    metrics.addType("data_summary_active_session_users");
    metrics.append(String.format("data_summary_active_session_users %d%n", gauges.users()));
  }

  private SessionGauges computeSessionGauges() {
    List<SessionInformation> sessions = userService.listAllSessions();
    long active = sessions.stream().filter(s -> !s.isExpired()).count();
    long users =
        sessions.stream()
            .filter(s -> !s.isExpired())
            .map(DataSummaryController::sessionUsername)
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
