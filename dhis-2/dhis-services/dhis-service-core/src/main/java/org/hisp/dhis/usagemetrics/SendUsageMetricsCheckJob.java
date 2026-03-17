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

import static org.hisp.dhis.scheduling.JobType.SEND_USAGE_METRICS_CHECK;

import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.core.metrics.Info;
import io.prometheus.metrics.exporter.opentelemetry.OpenTelemetryExporter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.datastatistics.DataStatisticsService;
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobEntry;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendUsageMetricsCheckJob implements Job {

  private final DataStatisticsService dataStatisticsService;
  private final AppManager appManager;
  private final SystemService systemService;
  private final JdbcTemplate jdbcTemplate;
  private final SystemSettingsService systemSettingsService;

  private OpenTelemetryExporter openTelemetryExporter;

  @Qualifier("sendUsageMetricsRegistry")
  private final PrometheusRegistry prometheusRegistry;

  private SystemInfo systemInfo;

  @Setter private String otelEndpoint = "http://host.docker.internal:4318/v1/metrics";

  @Setter private int exportInterval = 5;

  @Override
  public JobType getJobType() {
    return SEND_USAGE_METRICS_CHECK;
  }

  @PostConstruct
  public void postConstruct() {
    systemInfo = systemService.getSystemInfo();
  }

  @Override
  public void execute(JobEntry config, JobProgress progress) {
    SystemSettings systemSettings = systemSettingsService.getCurrentSettings();
    if (systemSettings.getOptInSendUsageMetrics()) {
      if (openTelemetryExporter == null) {
        String currentDbSystemIdentifier =
            jdbcTemplate
                .queryForList("SELECT system_identifier FROM pg_control_system()")
                .get(0)
                .get("system_identifier")
                .toString();
        List<Map<String, Object>> sendUsageMetricsSettings =
            jdbcTemplate.queryForList(
                "SELECT dbsystemidentifier FROM sendusagemetricsoptinsettings");
        if (sendUsageMetricsSettings.isEmpty()) {
          jdbcTemplate.update(
              "INSERT INTO sendusagemetricsoptinsettings (dbSystemIdentifier) VALUES (?)",
              currentDbSystemIdentifier);
        } else {
          String optedInDbSystemIdentifier =
              sendUsageMetricsSettings.get(0).get("dbsystemidentifier").toString();
          if (!currentDbSystemIdentifier.equals(optedInDbSystemIdentifier)) {
            systemSettingsService.put("optInSendUsageMetrics", false);
            jdbcTemplate.update("DELETE FROM sendusagemetricsoptinsettings");
            log.warn(
                "Opted-in to sending usage metrics but detected environment mismatch due to different DB system identifiers. DB system identifier was [{}] but now [{}]. Opt-in again to send DHIS2 usage metrics",
                optedInDbSystemIdentifier,
                currentDbSystemIdentifier);
            return;
          }
        }

        Info buildInfo =
            Info.builder()
                .name("build_info")
                .help("Build Info")
                .labelNames("revision", "build_time")
                .register(prometheusRegistry);
        buildInfo.addLabelValues(systemInfo.getRevision(), systemInfo.getBuildTime().toString());

        Info envInfo =
            Info.builder()
                .name("environment_info")
                .help("Environment")
                .labelNames(
                    "os",
                    "jvm_mem_mb_total",
                    "cpu_cores",
                    "postgres_version",
                    "java_version",
                    "java_vendor")
                .register(prometheusRegistry);

        envInfo.addLabelValues(
            systemInfo.getOsArchitecture(),
            String.valueOf((Runtime.getRuntime().totalMemory() / (1024 * 1024))),
            String.valueOf(systemInfo.getCpuCores()),
            systemInfo.getDatabaseInfo().getDatabaseVersion(),
            systemInfo.getJavaVersion(),
            systemInfo.getJavaVendor());

        GaugeWithCallback.builder()
            .name("dhis2_users")
            .help("DHIS2 Users")
            .labelNames("statistics")
            .callback(
                callback -> {
                  DataSummary dataSummary = dataStatisticsService.getSystemStatisticsSummary();
                  Map<Integer, Integer> activeUsers = dataSummary.getActiveUsers();
                  callback.call(activeUsers.get(0), "active_users_last_hour");
                  callback.call(activeUsers.get(1), "active_users_today");
                  callback.call(activeUsers.get(2), "active_users_last_2_days");
                  callback.call(activeUsers.get(7), "active_users_last_7_days");
                  callback.call(activeUsers.get(30), "active_users_last_30_days");
                  callback.call(dataSummary.getObjectCounts().get("user"), "users");
                })
            .register(prometheusRegistry);

        GaugeWithCallback.builder()
            .name("tracker_programs")
            .help("Tracker Programs")
            .callback(
                callback -> {
                  DataSummary dataSummary = dataStatisticsService.getSystemStatisticsSummary();
                  callback.call(dataSummary.getObjectCounts().get("program"));
                })
            .register(prometheusRegistry);

        GaugeWithCallback.builder()
            .name("organisation_units")
            .help("Organisation Units")
            .callback(
                callback -> {
                  DataSummary dataSummary = dataStatisticsService.getSystemStatisticsSummary();
                  callback.call(dataSummary.getObjectCounts().get("organisationUnit"));
                })
            .register(prometheusRegistry);

        GaugeWithCallback.builder()
            .name("dashboards")
            .help("Dashboards")
            .callback(
                callback -> {
                  DataSummary dataSummary = dataStatisticsService.getSystemStatisticsSummary();
                  callback.call(dataSummary.getObjectCounts().get("dashboard"));
                })
            .register(prometheusRegistry);

        GaugeWithCallback.builder()
            .name("maps")
            .help("Maps")
            .callback(
                callback -> {
                  DataSummary dataSummary = dataStatisticsService.getSystemStatisticsSummary();
                  callback.call(dataSummary.getObjectCounts().get("map"));
                })
            .register(prometheusRegistry);

        GaugeWithCallback.builder()
            .name("data_sets")
            .help("Data Sets")
            .callback(
                callback -> {
                  DataSummary dataSummary = dataStatisticsService.getSystemStatisticsSummary();
                  callback.call(dataSummary.getObjectCounts().get("dataSet"));
                })
            .register(prometheusRegistry);

        GaugeWithCallback.builder()
            .name("visualizations")
            .help("Visualizations")
            .callback(
                callback -> {
                  DataSummary dataSummary = dataStatisticsService.getSystemStatisticsSummary();
                  callback.call(dataSummary.getObjectCounts().get("visualization"));
                })
            .register(prometheusRegistry);

        GaugeWithCallback.builder()
            .name("tracked_entities")
            .help("Tracked Entities")
            .callback(
                callback -> {
                  DataSummary dataSummary = dataStatisticsService.getSystemStatisticsSummary();
                  callback.call(dataSummary.getObjectCounts().get("trackedEntity"));
                })
            .register(prometheusRegistry);

        GaugeWithCallback.builder()
            .name("core_apps")
            .help("Core Apps")
            .labelNames("name", "version")
            .callback(
                callback -> {
                  List<App> apps = appManager.getApps(null);
                  for (App app : apps) {
                    if (app.isCoreApp()) {
                      callback.call(1, app.getName(), app.getVersion());
                    }
                  }
                })
            .register(prometheusRegistry);

        openTelemetryExporter =
            OpenTelemetryExporter.builder()
                .endpoint(otelEndpoint)
                .protocol("http/protobuf")
                .serviceInstanceId(
                    DigestUtils.md5Hex(currentDbSystemIdentifier + systemInfo.getSystemId()))
                .serviceName("dhis2-core")
                .serviceNamespace("DHIS2")
                .serviceVersion(systemInfo.getVersion())
                .intervalSeconds(exportInterval)
                .registry(prometheusRegistry)
                .buildAndStart();
      }
    } else {
      if (openTelemetryExporter != null) {
        openTelemetryExporter.close();
        openTelemetryExporter = null;
        jdbcTemplate.update("DELETE FROM sendusagemetricsoptinsettings");
      }
    }
  }
}
