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
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
@Configuration
public class PrometheusMonitoringConfig {
  @Bean
  public Clock micrometerClock() {
    return Clock.SYSTEM;
  }

  @Bean
  public PrometheusProperties defaultProperties() {
    return new PrometheusProperties();
  }

  @Bean
  public PrometheusConfig prometheusConfig(PrometheusProperties prometheusProperties) {
    return new PrometheusPropertiesConfigAdapter(prometheusProperties);
  }

  @Bean
  public PrometheusMeterRegistry prometheusMeterRegistry(
      PrometheusConfig prometheusConfig, PrometheusRegistry prometheusRegistry, Clock clock) {
    PrometheusMeterRegistry registry =
        new io.micrometer.prometheusmetrics.PrometheusMeterRegistry(
            prometheusConfig, prometheusRegistry, clock);

    // Apply filters inline - order matters: histogram config first, then renaming
    registry.config().meterFilter(createHikariCpHistogramMeterFilter());
    registry.config().meterFilter(createHikariCpRenamingMeterFilter());

    return registry;
  }

  @Bean
  public PrometheusRegistry prometheusRegistry() {
    return new PrometheusRegistry();
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
   *   <li>acquire: 1ms-100ms (getting connection from pool, should be fast)
   *   <li>creation: 1ms-500ms (TCP connection + auth handshake)
   *   <li>usage: 1ms-10s (actual query execution varies widely)
   * </ul>
   */
  private static MeterFilter createHikariCpHistogramMeterFilter() {
    // Full metric name -> histogram range configuration
    // Note: Uses jdbc.connections.* because MeterFilter.map() runs before configure(),
    // so metrics are already renamed by the time this configure() method sees them
    var ranges =
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
          io.micrometer.core.instrument.Meter.Id id, DistributionStatisticConfig config) {
        if (id.getType() != io.micrometer.core.instrument.Meter.Type.TIMER) {
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
  private static MeterFilter createHikariCpRenamingMeterFilter() {
    return new MeterFilter() {
      @Override
      public io.micrometer.core.instrument.Meter.Id map(io.micrometer.core.instrument.Meter.Id id) {
        // Match both "hikaricp.connections." and "hikaricp.connections" (without dot)
        if (id.getName().startsWith("hikaricp.connections")) {
          return id.withName(id.getName().replace("hikaricp.connections", "jdbc.connections"));
        }
        return id;
      }
    };
  }
}
