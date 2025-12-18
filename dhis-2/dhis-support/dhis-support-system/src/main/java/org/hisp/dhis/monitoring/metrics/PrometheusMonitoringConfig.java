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
package org.hisp.dhis.monitoring.metrics;

import static org.hisp.dhis.external.conf.ConfigurationKey.DB_POOL_TYPE;
import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_DBPOOL_ENABLED;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import java.time.Duration;
import java.util.Map;
import org.hisp.dhis.datasource.DatabasePoolUtils.DbPoolType;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
 * │    CollectorRegistry      │ ← Prometheus client library's registry
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
   * /api/metrics endpoint reads from this via {@code metricFamilySamples()}.
   */
  @Bean
  public CollectorRegistry collectorRegistry() {
    return new CollectorRegistry(true);
  }

  /**
   * Micrometer's adapter that bridges Micrometer metrics to Prometheus. It translates Micrometer
   * metrics and writes them to the {@link CollectorRegistry}.
   *
   * <p>HikariCP metric filters are applied here, before any datasources are created, because
   * Micrometer requires filters to be registered before metrics are published.
   */
  @Bean
  public PrometheusMeterRegistry prometheusMeterRegistry(
      CollectorRegistry collectorRegistry, DhisConfigurationProvider config) {
    PrometheusMeterRegistry registry =
        new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, collectorRegistry, Clock.SYSTEM);

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
