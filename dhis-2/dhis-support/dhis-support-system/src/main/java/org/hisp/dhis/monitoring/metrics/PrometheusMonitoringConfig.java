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

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Prometheus metrics integration for DHIS2.
 *
 * <p>Creates the Micrometer-to-Prometheus bridge. Connection pool metrics (HikariCP histogram
 * filters, metric renaming) are configured in {@link DataSourcePoolMetricsConfig} when enabled.
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
  public PrometheusRegistry prometheusRegistry() {
    return new PrometheusRegistry();
  }

  /**
   * Micrometer's adapter that bridges Micrometer metrics to Prometheus. It translates Micrometer
   * metrics and writes them to the {@link PrometheusRegistry}.
   *
   * <p>Note: Connection pool metric filters (histogram buckets, renaming) are applied by {@link
   * DataSourcePoolMetricsConfig} when pool metrics are enabled.
   */
  @Bean
  public PrometheusMeterRegistry prometheusMeterRegistry(PrometheusRegistry prometheusRegistry) {
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusRegistry, Clock.SYSTEM);
  }
}
