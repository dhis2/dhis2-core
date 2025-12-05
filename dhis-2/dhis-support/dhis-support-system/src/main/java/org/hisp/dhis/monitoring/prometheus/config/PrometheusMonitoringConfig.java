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
   * Creates a MeterFilter that configures HikariCP timer metrics with histogram buckets.
   *
   * <p>HikariCP's default Micrometer integration publishes timer metrics as summaries. This filter
   * enables histogram buckets which are more suitable for aggregation in Prometheus/Grafana.
   *
   * <p>Micrometer's default percentile histogram buckets are used, filtered to the 1ms-500ms range
   * which is appropriate for OLTP connection pool metrics. This yields approximately 39 buckets.
   */
  private static MeterFilter createHikariCpHistogramMeterFilter() {
    return new MeterFilter() {
      @Override
      public DistributionStatisticConfig configure(
          io.micrometer.core.instrument.Meter.Id id, DistributionStatisticConfig config) {
        String name = id.getName();
        // Match on jdbc.connections.* because the renaming filter's map() runs before configure()
        if (id.getType() == io.micrometer.core.instrument.Meter.Type.TIMER
            && (name.startsWith("hikaricp.connections.") || name.startsWith("jdbc.connections."))
            && (name.endsWith(".acquire")
                || name.endsWith(".usage")
                || name.endsWith(".creation"))) {
          return DistributionStatisticConfig.builder()
              .percentilesHistogram(true) // Enable histogram publishing
              .percentiles((double[]) null) // Disable summary percentiles
              .minimumExpectedValue(Duration.ofMillis(1).toNanos()) // Min: 1ms
              .maximumExpectedValue(Duration.ofMillis(500).toNanos()) // Max: 500ms
              .build()
              .merge(config);
        }
        return config;
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
