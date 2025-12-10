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

import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.datasource.DatabasePoolUtils.DbPoolType;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Configures C3P0 connection pool metrics for DHIS2 data sources.
 *
 * <p>This configuration is conditional on {@code monitoring.dbpool.enabled=on} and {@code
 * connection.pool.type=c3p0}.
 *
 * <p>C3P0 doesn't have a metrics integration API like HikariCP's {@code MetricsTrackerFactory}.
 * Instead, we manually register Micrometer gauges that poll the pool's state (active connections,
 * idle connections, etc.) after datasource beans are created.
 *
 * @deprecated C3P0 is deprecated since v43. Migrate to HikariCP ({@code
 *     connection.pool.type=hikari}). HikariCP metrics are configured in {@link
 *     PrometheusMonitoringConfig} and registered via {@code DatabasePoolUtils.createDbPool()}.
 */
@Slf4j
@Configuration
@Conditional(C3P0MetricsConfig.C3P0MetricsEnabledCondition.class)
@Deprecated(since = "v43", forRemoval = true)
public class C3P0MetricsConfig {

  private final PrometheusMeterRegistry registry;

  public C3P0MetricsConfig(PrometheusMeterRegistry registry) {
    this.registry = registry;
    log.warn("C3P0 connection pool metrics are deprecated. Migrate to HikariCP");
  }

  /**
   * Binds C3P0 datasources to the metrics registry after all datasource beans are created.
   *
   * <p>Spring automatically injects all beans of type {@link DataSource} as a {@code Map<String,
   * DataSource>} where the key is the bean name (e.g., "actualDataSource", "analyticsDataSource")
   * and the value is the datasource instance.
   *
   * @param dataSources all DataSource beans in the application context, keyed by bean name
   */
  @Autowired
  public void bindDataSourcesToRegistry(@Lazy Map<String, DataSource> dataSources) {
    dataSources.forEach(this::bindC3p0DataSource);
  }

  private void bindC3p0DataSource(String beanName, DataSource dataSource) {
    if (dataSource instanceof ComboPooledDataSource c3p0) {
      String poolName = stripDataSourceSuffix(beanName);
      registerC3p0Metrics(c3p0, poolName);
    }
  }

  private void registerC3p0Metrics(ComboPooledDataSource dataSource, String poolName) {
    Iterable<Tag> tags = Tags.of("pool", poolName);

    try {
      registry.gauge(
          "jdbc.connections.active",
          tags,
          dataSource,
          ds -> {
            try {
              return ds.getNumBusyConnectionsDefaultUser();
            } catch (SQLException e) {
              return Double.NaN;
            }
          });

      registry.gauge(
          "jdbc.connections.idle",
          tags,
          dataSource,
          ds -> {
            try {
              return ds.getNumIdleConnectionsDefaultUser();
            } catch (SQLException e) {
              return Double.NaN;
            }
          });

      registry.gauge(
          "jdbc.connections.max", tags, dataSource, ComboPooledDataSource::getMaxPoolSize);

      registry.gauge(
          "jdbc.connections.min", tags, dataSource, ComboPooledDataSource::getMinPoolSize);

      log.debug("Registered C3P0 metrics for pool '{}'", poolName);
    } catch (Exception e) {
      log.error("Failed to register C3P0 metrics for pool '{}'", poolName, e);
    }
  }

  private String stripDataSourceSuffix(String beanName) {
    String suffix = "DataSource";
    if (beanName.length() > suffix.length()
        && beanName.toLowerCase().endsWith(suffix.toLowerCase())) {
      return beanName.substring(0, beanName.length() - suffix.length());
    }
    return beanName;
  }

  /**
   * Condition that activates this config only when both:
   *
   * <ul>
   *   <li>{@code monitoring.dbpool.enabled=on}
   *   <li>{@code connection.pool.type=c3p0}
   * </ul>
   */
  static class C3P0MetricsEnabledCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      DhisConfigurationProvider config =
          context.getBeanFactory().getBean(DhisConfigurationProvider.class);

      boolean c3p0 = DbPoolType.C3P0.name().equalsIgnoreCase(config.getProperty(DB_POOL_TYPE));
      boolean metricsEnabled = config.isEnabled(MONITORING_DBPOOL_ENABLED);

      return metricsEnabled && c3p0;
    }
  }
}
