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

import static org.hisp.dhis.security.Authorities.F_PERFORM_MAINTENANCE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.datastatistics.DataStatisticsService;
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
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
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class DataSummaryController {

  private static final Gauge objectCountsGauge =
      Gauge.build()
          .name("data_summary_object_counts")
          .help("Count of objects by type")
          .labelNames("type")
          .register();

  private static final Gauge activeUsersGauge =
      Gauge.build()
          .name("data_summary_active_users")
          .help("Active users over days")
          .labelNames("days")
          .register();

  private static final Gauge userInvitationsGauge =
      Gauge.build()
          .name("data_summary_user_invitations")
          .help("Count of user invitations")
          .labelNames("type")
          .register();

  private static final Gauge dataValueCountGauge =
      Gauge.build()
          .name("data_summary_data_value_count")
          .help("Data value counts over time")
          .labelNames("time")
          .register();

  private static final Gauge eventCountGauge =
      Gauge.build()
          .name("data_summary_event_count")
          .help("Event counts over time")
          .labelNames("time")
          .register();

  private static final Gauge systemInfoGauge =
      Gauge.build()
          .name("data_summary_system_info")
          .help("DHIS2 System information")
          .labelNames("key", "value")
          .register();

  @Autowired public DataStatisticsService dataStatisticsService;

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  public @ResponseBody DataSummary getStatistics() {
    return dataStatisticsService.getSystemStatisticsSummary();
  }

  @GetMapping(value = "/metrics", produces = TEXT_PLAIN_VALUE)
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  public @ResponseBody String getPrometheusMetrics() throws IOException {
    DataSummary summary = dataStatisticsService.getSystemStatisticsSummary();

    // Update object counts
    summary.getObjectCounts().forEach((type, count) -> objectCountsGauge.labels(type).set(count));

    // Update active users
    summary
        .getActiveUsers()
        .forEach((days, count) -> activeUsersGauge.labels(days.toString()).set(count));

    // Update user invitations
    summary
        .getUserInvitations()
        .forEach((type, count) -> userInvitationsGauge.labels(type).set(count));

    // Update data value count
    summary
        .getDataValueCount()
        .forEach((time, count) -> dataValueCountGauge.labels(time.toString()).set(count));

    // Update event count
    summary
        .getEventCount()
        .forEach((time, count) -> eventCountGauge.labels(time.toString()).set(count));

    // Update system info as static gauges
    if (summary.getSystem() != null) {
      if (summary.getSystem().getVersion() != null) {
        systemInfoGauge.labels("version", summary.getSystem().getVersion()).set(1);
      }
      if (summary.getSystem().getRevision() != null) {
        systemInfoGauge.labels("revision", summary.getSystem().getRevision()).set(1);
      }
      if (summary.getSystem().getBuildTime() != null) {
        systemInfoGauge
            .labels("build_time", String.valueOf(summary.getSystem().getBuildTime()))
            .set(1);
      }
      if (summary.getSystem().getSystemId() != null) {
        systemInfoGauge.labels("system_id", summary.getSystem().getSystemId()).set(1);
      }
      if (summary.getSystem().getServerDate() != null) {
        systemInfoGauge
            .labels("server_date", String.valueOf(summary.getSystem().getServerDate()))
            .set(1);
      }
    }

    // Generate Prometheus metrics as plain text
    Writer writer = new StringWriter();
    TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
    return writer.toString();
  }
}
