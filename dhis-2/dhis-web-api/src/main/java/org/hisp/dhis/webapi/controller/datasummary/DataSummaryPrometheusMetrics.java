/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import io.prometheus.client.Gauge;
import org.hisp.dhis.datasummary.DataSummary;

public class DataSummaryPrometheusMetrics {
  public static final Gauge objectCountsGauge =
      Gauge.build()
          .name("data_summary_object_counts")
          .help("Count of objects by type")
          .labelNames("type")
          .register();
  public static final Gauge activeUsersGauge =
      Gauge.build()
          .name("data_summary_active_users")
          .help("Active users over days")
          .labelNames("days")
          .register();
  public static final Gauge userInvitationsGauge =
      Gauge.build()
          .name("data_summary_user_invitations")
          .help("Count of user invitations")
          .labelNames("type")
          .register();
  public static final Gauge dataValueCountGauge =
      Gauge.build()
          .name("data_summary_data_value_count")
          .help("Data value counts over time")
          .labelNames("time")
          .register();
  public static final Gauge eventCountGauge =
      Gauge.build()
          .name("data_summary_event_count")
          .help("Event counts over time")
          .labelNames("time")
          .register();
  public static final Gauge systemInfoGauge =
      Gauge.build()
          .name("data_summary_system_info")
          .help("DHIS2 System information")
          .labelNames("key", "value")
          .register();

  public static void updateMetrics(DataSummary summary) {
    // Update object counts
    summary
        .getObjectCounts()
        .forEach(
            (type, count) ->
                DataSummaryPrometheusMetrics.objectCountsGauge.labels(type).set(count));

    // Update active users
    summary
        .getActiveUsers()
        .forEach(
            (days, count) ->
                DataSummaryPrometheusMetrics.activeUsersGauge.labels(days.toString()).set(count));

    // Update user invitations
    summary
        .getUserInvitations()
        .forEach(
            (type, count) ->
                DataSummaryPrometheusMetrics.userInvitationsGauge.labels(type).set(count));

    // Update data value count
    summary
        .getDataValueCount()
        .forEach(
            (time, count) ->
                DataSummaryPrometheusMetrics.dataValueCountGauge
                    .labels(time.toString())
                    .set(count));

    // Update event count
    summary
        .getEventCount()
        .forEach(
            (time, count) ->
                DataSummaryPrometheusMetrics.eventCountGauge.labels(time.toString()).set(count));

    // Update system info as static gauges
    if (summary.getSystem() != null) {
      if (summary.getSystem().getVersion() != null) {
        DataSummaryPrometheusMetrics.systemInfoGauge
            .labels("version", summary.getSystem().getVersion())
            .set(1);
      }
      if (summary.getSystem().getRevision() != null) {
        DataSummaryPrometheusMetrics.systemInfoGauge
            .labels("revision", summary.getSystem().getRevision())
            .set(1);
      }
      if (summary.getSystem().getBuildTime() != null) {
        DataSummaryPrometheusMetrics.systemInfoGauge
            .labels("build_time", String.valueOf(summary.getSystem().getBuildTime()))
            .set(1);
      }
      if (summary.getSystem().getSystemId() != null) {
        DataSummaryPrometheusMetrics.systemInfoGauge
            .labels("system_id", summary.getSystem().getSystemId())
            .set(1);
      }
      if (summary.getSystem().getServerDate() != null) {
        DataSummaryPrometheusMetrics.systemInfoGauge
            .labels("server_date", String.valueOf(summary.getSystem().getServerDate()))
            .set(1);
      }
    }
  }
}
