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

import static org.hisp.dhis.scheduling.JobType.SEND_USAGE_METRICS;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleGauge;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.datastatistics.DataStatisticsService;
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobEntry;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeStore;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendUsageMetricsJob implements Job {

  public static final String COLLECT_USAGE_METRICS_JOB_ID = "CmeXZ8cPBlf";

  private final SystemService systemService;
  private final UsageMetricsService usageMetricsService;
  private final OpenTelemetrySdk otelSdk;
  private final DataStatisticsService dataStatisticsService;
  private final JobConfigurationService jobConfigurationService;
  private final TrackedEntityTypeStore trackedEntityTypeStore;
  private final AppManager appManager;

  private SystemInfo systemInfo;

  private DoubleGauge trackerProgramGauge;
  private DoubleGauge envInfo;
  private DoubleGauge buildInfo;
  private DoubleGauge orgUnitGauge;
  private DoubleGauge dashboardGauge;
  private DoubleGauge mapGauge;
  private DoubleGauge dataSetGauge;
  private DoubleGauge visualizationGauge;
  private DoubleGauge trackedEntityGauge;
  private DoubleGauge coreAppGauge;
  private DoubleGauge trackedEntityTypeGauge;
  private DoubleGauge dhis2UserGauge;

  private double[] bins;

  @Override
  public JobType getJobType() {
    return SEND_USAGE_METRICS;
  }

  @PostConstruct
  public void postConstruct() {
    systemInfo = systemService.getSystemInfo();
    Meter meter = otelSdk.getMeter("usage-metrics");

    trackedEntityTypeGauge =
        meter.gaugeBuilder("tracked_entity_types").setDescription("Tracked Entity Types").build();
    coreAppGauge = meter.gaugeBuilder("core_apps").setDescription("Core Apps").build();
    dataSetGauge = meter.gaugeBuilder("data_sets").setDescription("Data Sets").build();
    trackerProgramGauge =
        meter.gaugeBuilder("tracker_programs").setDescription("Tracker Programs").build();
    orgUnitGauge =
        meter.gaugeBuilder("organisation_units").setDescription("Organisation Units").build();
    dashboardGauge = meter.gaugeBuilder("dashboards").setDescription("Dashboards").build();
    visualizationGauge =
        meter.gaugeBuilder("visualizations").setDescription("Visualizations").build();
    mapGauge = meter.gaugeBuilder("maps").setDescription("Maps").build();
    trackedEntityGauge =
        meter.gaugeBuilder("tracked_entities").setDescription("Tracked Entities").build();
    dhis2UserGauge = meter.gaugeBuilder("dhis2_users").setDescription("DHIS2 Users").build();
    envInfo = meter.gaugeBuilder("environment").setDescription("Environment Info").build();
    buildInfo = meter.gaugeBuilder("build").setDescription("Build Info").build();

    bins = new double[100];
    bins[0] = 0;
    for (int i = 1; i < 100; i++) {
      bins[i] = Math.pow(2, i);
    }
  }

  @Override
  public void execute(JobEntry config, JobProgress progress) {
    UsageMetricsConsent consent = usageMetricsService.getConsent();
    if (consent.isConsent()) {
      String currentImplementationId = usageMetricsService.getImplementationId();

      if (isEnvMatch(currentImplementationId, consent.getImplementationId())) {
        collectMetrics();
        otelSdk.getSdkMeterProvider().forceFlush();
      } else {
        forgetConsent(consent, currentImplementationId);
      }
    }
  }

  protected void forgetConsent(UsageMetricsConsent usageMetricsConsent, String currentImplementationId) {
    usageMetricsService.removeConsent();
    JobConfiguration sendUsageMetricsJobConfig =
        jobConfigurationService.getJobConfigurationByUid(
            SendUsageMetricsJob.COLLECT_USAGE_METRICS_JOB_ID);
    if (sendUsageMetricsJobConfig != null) {
      jobConfigurationService.deleteJobConfiguration(sendUsageMetricsJobConfig);
    }
    log.warn(
        "Opted-in to sending usage metrics but detected environment mismatch due to different implementation IDs. Expected implementation ID was [{}] but actual implementation ID is [{}]. Opt-in again to transmit DHIS2 usage metrics",
        usageMetricsConsent.getImplementationId(),
        currentImplementationId);
  }

  protected void collectMetrics() {
    DataSummary dataSummary = dataStatisticsService.getSystemStatisticsSummary();
    Map<String, Long> objectCounts = dataSummary.getObjectCounts();

    envInfo.set(
        1,
        Attributes.builder()
            .put("os", systemInfo.getOsArchitecture())
            .put(
                "jvm_mem_mb_total",
                String.valueOf((Runtime.getRuntime().totalMemory() / (1024 * 1024))))
            .put("cpu_cores", String.valueOf(systemInfo.getCpuCores()))
            .put("postgres_version", systemInfo.getDatabaseInfo().getDatabaseVersion())
            .put("java_version", systemInfo.getJavaVersion())
            .put("java_vendor", systemInfo.getJavaVendor())
            .build());

    buildInfo.set(
        1,
        Attributes.builder()
            .put("revision", systemInfo.getRevision())
            .put(
                "build_time",
                systemInfo.getBuildTime() == null ? "" : systemInfo.getBuildTime().toString())
            .build());

    dataSetGauge.set(objectCounts.get("dataSet"));
    trackerProgramGauge.set(objectCounts.get("program"));
    trackedEntityTypeGauge.set(trackedEntityTypeStore.getAll().size());

    for (App app : appManager.getApps(null)) {
      if (app.isCoreApp()) {
        coreAppGauge.set(
            1,
            Attributes.builder()
                .put("name", app.getName())
                .put("version", app.getVersion())
                .build());
      }
    }

    bin(bins, objectCounts.get("organisationUnit"), orgUnitGauge);
    bin(bins, objectCounts.get("dashboard"), dashboardGauge);
    bin(bins, objectCounts.get("map"), mapGauge);
    bin(bins, objectCounts.get("visualization"), visualizationGauge);
    bin(bins, objectCounts.get("trackedEntity"), trackedEntityGauge);
    bin(bins, objectCounts.get("trackedEntity"), trackedEntityGauge);

    Map<Integer, Integer> activeUsers = dataSummary.getActiveUsers();
    bin(
        bins,
        activeUsers.get(0),
        Attributes.builder().put("statistics", "active_users_last_hour").build(),
        dhis2UserGauge);
    bin(
        bins,
        activeUsers.get(1),
        Attributes.builder().put("statistics", "active_users_today").build(),
        dhis2UserGauge);
    bin(
        bins,
        activeUsers.get(2),
        Attributes.builder().put("statistics", "active_users_last_2_days").build(),
        dhis2UserGauge);
    bin(
        bins,
        activeUsers.get(7),
        Attributes.builder().put("statistics", "active_users_last_7_days").build(),
        dhis2UserGauge);
    bin(
        bins,
        activeUsers.get(30),
        Attributes.builder().put("statistics", "active_users_last_30_days").build(),
        dhis2UserGauge);
    bin(
        bins,
        objectCounts.get("user"),
        Attributes.builder().put("statistics", "users").build(),
        dhis2UserGauge);
  }

  protected void bin(double[] bins, long value, DoubleGauge doubleGauge) {
    bin(bins, value, Attributes.empty(), doubleGauge);
  }

  protected void bin(
      double[] bins, long gaugeValue, Attributes attributes, DoubleGauge doubleGauge) {
    boolean binned = false;
    Attributes totalAttributes;

    for (int i = 0; i < bins.length; i++) {
      String bucketLabelValue;
      if (i > 0) {
        bucketLabelValue = String.format("%.0f - %.0f", bins[i - 1] + 1, bins[i]);
      } else {
        bucketLabelValue = String.format("%.0f", bins[i]);
      }
      totalAttributes =
          Attributes.builder().putAll(attributes).put("bucket", bucketLabelValue).build();
      if (!binned && bins[i] >= gaugeValue) {
        binned = true;
        doubleGauge.set(1, totalAttributes);
      } else {
        doubleGauge.set(0, totalAttributes);
      }

      if (i == (bins.length - 1)) {
        Attributes infBucketAttributes =
            Attributes.builder().putAll(attributes).put("bucket", "+inf").build();
        if (binned) {
          doubleGauge.set(0, infBucketAttributes);
        } else {
          doubleGauge.set(1, infBucketAttributes);
        }
      }
    }
  }

  protected boolean isEnvMatch(String actualImplementationId, String expectedImplementationId) {
    return actualImplementationId.equals(expectedImplementationId);
  }
}
