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
package org.hisp.dhis.usagemetrics;

import static org.hisp.dhis.scheduling.JobType.OBSERVE_USAGE_METRICS;

import io.prometheus.metrics.core.metrics.Histogram;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.datastatistics.DataStatisticsService;
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobEntry;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObserveUsageMetricsJob implements Job {

  @Qualifier("orgUnitHistogram")
  private final Histogram orgUnitHistogram;

  @Qualifier("trackedEntityHistogram")
  private final Histogram trackedEntityHistogram;

  @Qualifier("mapHistogram")
  private final Histogram mapHistogram;

  @Qualifier("visualizationHistogram")
  private final Histogram visualizationHistogram;

  @Qualifier("dashboardHistogram")
  private final Histogram dashboardHistogram;

  @Qualifier("userHistogram")
  private final Histogram userHistogram;

  @Qualifier("trackedEntityTypeHistogram")
  private final Histogram trackedEntityTypeHistogram;

  private final DataStatisticsService dataStatisticsService;
  private final UsageMetricsConsentStore usageMetricsConsentStore;
  private final TrackedEntityTypeStore trackedEntityTypeStore;

  @Override
  public JobType getJobType() {
    return OBSERVE_USAGE_METRICS;
  }

  @Override
  public void execute(JobEntry config, JobProgress progress) {
    List<UsageMetricsConsent> usageMetricsConsents = usageMetricsConsentStore.getAll();
    if (!usageMetricsConsents.isEmpty() && usageMetricsConsents.get(0).isConsent()) {
      DataSummary dataSummary = dataStatisticsService.getSystemStatisticsSummary();
      Map<String, Long> objectCounts = dataSummary.getObjectCounts();

      orgUnitHistogram.observe(objectCounts.get("organisationUnit"));
      trackedEntityHistogram.observe(objectCounts.get("trackedEntity"));

      Map<Integer, Integer> activeUsers = dataSummary.getActiveUsers();
      userHistogram.labelValues("active_users_last_hour").observe(activeUsers.get(0));
      userHistogram.labelValues("active_users_today").observe(activeUsers.get(1));
      userHistogram.labelValues("active_users_last_2_days").observe(activeUsers.get(2));
      userHistogram.labelValues("active_users_last_7_days").observe(activeUsers.get(7));
      userHistogram.labelValues("active_users_last_30_days").observe(activeUsers.get(30));
      userHistogram.labelValues("users").observe(objectCounts.get("user"));

      mapHistogram.observe(objectCounts.get("map"));
      visualizationHistogram.observe(objectCounts.get("visualization"));
      dashboardHistogram.observe(objectCounts.get("dashboard"));
      trackedEntityTypeHistogram.observe(trackedEntityTypeStore.getAll().size());
    } else {
      orgUnitHistogram.clear();
      trackedEntityHistogram.clear();
      userHistogram.clear();
      mapHistogram.clear();
      visualizationHistogram.clear();
      dashboardHistogram.clear();
      trackedEntityTypeHistogram.clear();
    }
  }
}
