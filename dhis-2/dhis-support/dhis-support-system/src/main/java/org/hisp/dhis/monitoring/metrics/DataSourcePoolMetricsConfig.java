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

import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_DBPOOL_ENABLED;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Configures connection pool metrics for DHIS2 data sources.
 *
 * <p>HikariCP metrics are handled by HikariCP's native Micrometer integration via
 * MicrometerMetricsTrackerFactory (configured in DatabasePoolUtils). Metrics are renamed from
 * hikaricp.connections.* to jdbc.connections.* via PrometheusMonitoringConfig for backward
 * compatibility.
 *
 * <p>C3P0 metrics are manually registered as basic gauges with jdbc.connections.* naming
 * (deprecated, will be removed).
 *
 * @author Luciano Fiandesio
 */
@Slf4j
@Configuration
@Conditional(DataSourcePoolMetricsConfig.DataSourcePoolMetricsEnabledCondition.class)
public class DataSourcePoolMetricsConfig {

  private static final String DATASOURCE_SUFFIX = "dataSource";

  private final MeterRegistry registry;

  public DataSourcePoolMetricsConfig(MeterRegistry registry) {
    this.registry = registry;
  }

  @Autowired
  public void bindDataSourcesToRegistry(Map<String, DataSource> dataSources) {
    dataSources.forEach(this::bindDataSourceToRegistry);
  }

  private void bindDataSourceToRegistry(String beanName, DataSource dataSource) {
    String poolName = getDataSourceName(beanName);

    if (dataSource instanceof ComboPooledDataSource c3p0) {
      log.warn("C3P0 connection pool metrics are deprecated. Migrate to HikariCP");
      registerC3p0Metrics(c3p0, poolName);
    }
    // HikariCP metrics are automatically registered via MicrometerMetricsTrackerFactory
    // configured in DatabasePoolUtils.createHikariDbPool()
  }

  @Deprecated(since = "v43", forRemoval = true)
  private void registerC3p0Metrics(ComboPooledDataSource dataSource, String poolName) {
    Iterable<Tag> tags = Tags.of("name", poolName);

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

    } catch (Exception e) {
      log.error("Failed to register C3P0 metrics for pool '{}'", poolName, e);
    }
  }

  private String getDataSourceName(String beanName) {
    if (beanName.length() > DATASOURCE_SUFFIX.length()
        && Strings.CI.endsWith(beanName, DATASOURCE_SUFFIX)) {
      return beanName.substring(0, beanName.length() - DATASOURCE_SUFFIX.length());
    }
    return beanName;
  }

  static class DataSourcePoolMetricsEnabledCondition extends MetricsEnabler {
    @Override
    protected ConfigurationKey getConfigKey() {
      return MONITORING_DBPOOL_ENABLED;
    }
  }
}
