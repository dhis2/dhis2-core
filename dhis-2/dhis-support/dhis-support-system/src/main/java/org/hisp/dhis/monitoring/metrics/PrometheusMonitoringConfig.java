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
package org.hisp.dhis.monitoring.metrics;

import static org.hisp.dhis.external.conf.ConfigurationKey.DB_POOL_TYPE;
import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_DBPOOL_ENABLED;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.core.metrics.Info;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.datasource.DatabasePoolUtils.DbPoolType;
import org.hisp.dhis.datastatistics.DataStatisticsService;
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configures Prometheus metrics integration for DHIS2.
 *
 * <p>Creates the Micrometer-to-Prometheus bridge and applies HikariCP metric filters. Filters must
 * be applied here (before any datasources are created) because Micrometer requires filters to be
 * registered before metrics are published.
 *
 * <h2>Metrics Flow</h2>
 *
 * <pre>
 * ┌───────────────────────────┐
 * │     Metrics Sources       │ ← HikariCP, JVM, Hibernate, etc.
 * └─────────────┬─────────────┘
 *               │ publish metrics
 *               ▼
 * ┌───────────────────────────┐
 * │ PrometheusMeterRegistry   │ ← Micrometer's bridge to Prometheus
 * │   (with MeterFilters)     │   Filters rename hikaricp.* → jdbc.*
 * └─────────────┬─────────────┘
 *               │ writes to
 *               ▼
 * ┌───────────────────────────┐
 * │    PrometheusRegistry     │ ← Prometheus client library's registry
 * └─────────────┬─────────────┘
 *               │ scraped by
 *               ▼
 * ┌───────────────────────────┐
 * │       /api/metrics        │ ← PrometheusScrapeEndpointController
 * └───────────────────────────┘
 * </pre>
 */
@Configuration
public class PrometheusMonitoringConfig {

  /**
   * Prometheus client library's native registry that stores metrics in Prometheus format. The
   * /api/metrics endpoint reads from this via {@code scrape()}.
   */
  @Bean
  @Primary
  public PrometheusRegistry prometheusRegistry() {
    return new PrometheusRegistry();
  }

  @Bean(name = "sendUsageMetricsRegistry")
  public PrometheusRegistry sendUsageMetricsRegistry(
      SystemService systemService,
      DataStatisticsService dataStatisticsService,
      AppManager appManager) {
    PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
    SystemInfo systemInfo = systemService.getSystemInfo();

    Info buildInfo =
        Info.builder()
            .name("build_info")
            .help("Build Info")
            .labelNames("revision", "build_time")
            .register(prometheusRegistry);

    String buildTime =
        systemInfo.getBuildTime() == null ? "" : systemInfo.getBuildTime().toString();
    buildInfo.addLabelValues(systemInfo.getRevision(), buildTime);

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

    return prometheusRegistry;
  }

  /**
   * Micrometer's adapter that bridges Micrometer metrics to Prometheus. It translates Micrometer
   * metrics and writes them to the {@link PrometheusRegistry}.
   *
   * <p>HikariCP metric filters are applied here, before any datasources are created, because
   * Micrometer requires filters to be registered before metrics are published.
   */
  @Bean
  public PrometheusMeterRegistry prometheusMeterRegistry(
      PrometheusRegistry prometheusRegistry, DhisConfigurationProvider config) {
    PrometheusMeterRegistry registry =
        new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusRegistry, Clock.SYSTEM);

    // Apply HikariCP filters early, before any data sources are created
    boolean hikari = DbPoolType.HIKARI.name().equalsIgnoreCase(config.getProperty(DB_POOL_TYPE));
    boolean metricsEnabled = config.isEnabled(MONITORING_DBPOOL_ENABLED);

    if (hikari && metricsEnabled) {
      registry.config().meterFilter(hikariRenamingFilter());
      registry.config().meterFilter(hikariHistogramFilter());
    }

    return registry;
  }

  /**
   * Creates a MeterFilter that renames HikariCP metrics from "hikaricp.connections*" to
   * "jdbc.connections*" for backward compatibility.
   */
  private static MeterFilter hikariRenamingFilter() {
    return new MeterFilter() {
      @Override
      public Meter.Id map(Meter.Id id) {
        if (id.getName().startsWith("hikaricp.connections")) {
          return id.withName(id.getName().replace("hikaricp.connections", "jdbc.connections"));
        }
        return id;
      }
    };
  }

  /**
   * Histogram range configuration for a specific metric type.
   *
   * @param minNanos Minimum expected duration in nanoseconds
   * @param maxNanos Maximum expected duration in nanoseconds
   */
  private record HistogramRange(double minNanos, double maxNanos) {
    static HistogramRange of(Duration min, Duration max) {
      return new HistogramRange(min.toNanos(), max.toNanos());
    }
  }

  /**
   * Creates a MeterFilter that configures HikariCP timer metrics with histogram buckets.
   *
   * <p>Different ranges are used for each metric type:
   *
   * <ul>
   *   <li>acquire: 1ms-100ms (getting connection from pool should be fast)
   *   <li>creation: 1ms-500ms (TCP connection and auth handshake)
   *   <li>usage: 1ms-10s (actual query execution varies widely)
   * </ul>
   */
  private static MeterFilter hikariHistogramFilter() {
    // Uses jdbc.connections.* because renaming filter runs first (map() before configure())
    Map<String, HistogramRange> ranges =
        Map.of(
            "jdbc.connections.acquire",
                HistogramRange.of(Duration.ofMillis(1), Duration.ofMillis(100)),
            "jdbc.connections.creation",
                HistogramRange.of(Duration.ofMillis(1), Duration.ofMillis(500)),
            "jdbc.connections.usage",
                HistogramRange.of(Duration.ofMillis(1), Duration.ofSeconds(10)));

    return new MeterFilter() {
      @Override
      public DistributionStatisticConfig configure(
          Meter.Id id, DistributionStatisticConfig config) {
        if (id.getType() != Meter.Type.TIMER) {
          return config;
        }

        HistogramRange range = ranges.get(id.getName());
        if (range == null) {
          return config;
        }

        return DistributionStatisticConfig.builder()
            .percentilesHistogram(true)
            .percentiles((double[]) null) // Disable client-side percentiles, use histograms only
            .minimumExpectedValue(range.minNanos())
            .maximumExpectedValue(range.maxNanos())
            .build()
            .merge(config);
      }
    };
  }
}
