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
package org.hisp.dhis.monitoring.prometheus.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.time.Duration;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Prometheus metrics integration for DHIS2.
 *
 * <p>Creates the Micrometer-to-Prometheus bridge and configures HikariCP connection pool metrics
 * with histogram buckets for better observability.
 *
 * <h2>Metrics Flow</h2>
 *
 * This shows how metrics are created using the example of the HikariCP connection pool:
 *
 * <pre>
 * ┌───────────────────────────┐
 * │     HikariDataSource      │ ← MicrometerMetricsTrackerFactory registered in DatabasePoolUtils
 * │     (metrics source)      │
 * └─────────────┬─────────────┘
 *               │ publishes hikaricp.connections.* metrics
 *               ▼
 * ┌───────────────────────────┐
 * │ PrometheusMeterRegistry   │ ← Micrometer's bridge to Prometheus
 * │     + MeterFilters        │   (renames hikaricp→jdbc, adds histogram buckets)
 * └─────────────┬─────────────┘
 *               │ writes to
 *               ▼
 * ┌───────────────────────────┐
 * │    PrometheusRegistry     │ ← Prometheus client library's registry (this config creates it)
 * │   (stores all metrics)    │
 * └─────────────┬─────────────┘
 *               │ scraped by
 *               ▼
 * ┌───────────────────────────┐
 * │       /api/metrics        │ ← PrometheusScrapeEndpointController calls registry.scrape()
 * │      (HTTP endpoint)      │
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
  public PrometheusRegistry prometheusRegistry() {
    return new PrometheusRegistry();
  }

  /**
   * Micrometer's adapter that bridges Micrometer metrics to Prometheus. Metrics sources like
   * HikariCP write to this via {@code MicrometerMetricsTrackerFactory}. It translates Micrometer
   * metrics and writes them to the {@link PrometheusRegistry}.
   */
  @Bean
  public PrometheusMeterRegistry prometheusMeterRegistry(PrometheusRegistry prometheusRegistry) {
    PrometheusMeterRegistry registry =
        new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusRegistry, Clock.SYSTEM);

    // Apply filters inline - order matters: histogram config first, then renaming
    registry.config().meterFilter(hikariHistogramFilter());
    registry.config().meterFilter(hikariRenamingFilter());

    return registry;
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
   * <p>HikariCP's default Micrometer integration publishes timer metrics as summaries. This filter
   * enables histogram buckets which are more suitable for aggregation in Prometheus/Grafana.
   *
   * <p>Different ranges are used for each metric type:
   *
   * <ul>
   *   <li>acquire: 1ms-100ms (getting connection from pool should be fast)
   *   <li>creation: 1ms-500ms (TCP connection and auth handshake)
   *   <li>usage: 1ms-10s (actual query execution varies widely)
   * </ul>
   *
   * <p>Micrometer selects buckets from a pre-computed set (~276 buckets) that fall within the
   * min/max range. The bucket boundaries use powers of 4 with linear interpolation between them.
   */
  private static MeterFilter hikariHistogramFilter() {
    // Full metric name -> histogram range configuration
    // Note: Uses jdbc.connections.* because MeterFilter.map() runs before configure(),
    // so metrics are already renamed by the time this configure() method sees them
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
}
