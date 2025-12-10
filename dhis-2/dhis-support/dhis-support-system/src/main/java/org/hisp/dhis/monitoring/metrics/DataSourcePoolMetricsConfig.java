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
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.datasource.DatabasePoolUtils.DbPoolType;
import org.hisp.dhis.datasource.HikariMetricsTrackerProvider;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Configures connection pool metrics for DHIS2 data sources.
 *
 * <p>This configuration is conditional on {@code monitoring.dbpool.enabled=on} and handles metrics
 * for all pool types:
 *
 * <ul>
 *   <li><b>HikariCP</b>: Applies histogram filters for timing metrics and provides {@link
 *       #createMetricsTracker(String)} for datasources to register metrics
 *   <li><b>C3P0</b> (deprecated): Registers basic gauge metrics for active/idle connections
 *   <li><b>UNPOOLED</b>: No metrics (no connection pooling)
 * </ul>
 */
@Slf4j
@Configuration
@Conditional(DataSourcePoolMetricsConfig.DataSourcePoolMetricsEnabledCondition.class)
public class DataSourcePoolMetricsConfig implements HikariMetricsTrackerProvider {

  private final PrometheusMeterRegistry registry;
  private final DbPoolType poolType;
  private final Set<String> registeredDataSourceNames = ConcurrentHashMap.newKeySet();

  public DataSourcePoolMetricsConfig(
      PrometheusMeterRegistry registry, DhisConfigurationProvider config) {
    this.registry = registry;
    this.poolType = DbPoolType.valueOf(config.getProperty(DB_POOL_TYPE).toUpperCase());

    if (poolType == DbPoolType.HIKARI) {
      // Apply HikariCP-specific filters: histogram buckets and metric renaming
      registry.config().meterFilter(hikariHistogramFilter());
      registry.config().meterFilter(hikariRenamingFilter());
      log.debug("HikariCP connection pool metrics enabled");
    } else if (poolType == DbPoolType.C3P0) {
      log.warn("C3P0 connection pool metrics are deprecated. Migrate to HikariCP");
    }
  }

  /**
   * Creates a metrics tracker factory for a HikariCP datasource.
   *
   * <p>This method must be called during datasource creation, before the HikariCP pool is
   * initialized. The returned factory must be set on {@link com.zaxxer.hikari.HikariConfig} via
   * {@code setMetricsTrackerFactory()} before creating the {@link
   * com.zaxxer.hikari.HikariDataSource}.
   *
   * <p><b>Why this approach instead of {@link #bindDataSourcesToRegistry}?</b>
   *
   * <p>HikariCP's {@code MetricsTrackerFactory} must be configured before the pool starts. Once
   * {@code new HikariDataSource(config)} is called, the pool initializes and it's too late to add
   * metrics tracking. This is different from C3P0, where we can register gauges on an existing pool
   * via {@link #bindDataSourcesToRegistry}.
   *
   * <p>Using HikariCP's native metrics integration provides rich timing histograms:
   *
   * <ul>
   *   <li>{@code hikaricp.connections.acquire} - time to obtain a connection from the pool
   *   <li>{@code hikaricp.connections.usage} - time connections are held before being returned
   *   <li>{@code hikaricp.connections.creation} - time to create new connections
   * </ul>
   *
   * These would not be available if we only registered gauges post-creation like C3P0.
   *
   * @param dataSourceName unique name identifying this datasource (e.g., "main", "analytics")
   * @return metrics tracker factory, or null if not using HikariCP
   * @throws IllegalStateException if dataSourceName is already registered
   * @throws NullPointerException if dataSourceName is null
   */
  @Override
  public MicrometerMetricsTrackerFactory createMetricsTracker(String dataSourceName) {
    Objects.requireNonNull(dataSourceName, "dataSourceName is required");

    if (poolType != DbPoolType.HIKARI) {
      return null;
    }

    if (!registeredDataSourceNames.add(dataSourceName)) {
      throw new IllegalStateException(
          "Duplicate datasource name '%s'. Already registered: %s"
              .formatted(dataSourceName, registeredDataSourceNames));
    }

    log.debug("Registered HikariCP metrics for datasource '{}'", dataSourceName);
    return new MicrometerMetricsTrackerFactory(registry);
  }

  /**
   * Binds C3P0 datasources to the metrics registry after all datasource beans are created.
   *
   * <p><b>How this works:</b> Spring automatically injects all beans of type {@link DataSource} as
   * a {@code Map<String, DataSource>} where the key is the bean name (e.g., "actualDataSource",
   * "analyticsDataSource") and the value is the datasource instance. This method is called after
   * all datasource beans are fully initialized.
   *
   * <p><b>Why C3P0 uses this approach:</b> C3P0 doesn't have a metrics integration API like
   * HikariCP's {@code MetricsTrackerFactory}. Instead, we manually register Micrometer gauges that
   * poll the pool's state (active connections, idle connections, etc.). This can be done on an
   * already-running pool, so post-creation binding works fine.
   *
   * <p><b>Why HikariCP doesn't use this approach:</b> See {@link #createMetricsTracker(String)}.
   * HikariCP requires metrics configuration before pool initialization to capture timing
   * histograms.
   *
   * @param dataSources all DataSource beans in the application context, keyed by bean name
   */
  @Autowired
  public void bindDataSourcesToRegistry(@Lazy Map<String, DataSource> dataSources) {
    if (poolType == DbPoolType.C3P0) {
      dataSources.forEach(this::bindC3p0DataSource);
    }
    // HikariCP: metrics registered via createMetricsTracker() called by datasource configs
    // UNPOOLED: no metrics to register
  }

  @Deprecated(since = "v43", forRemoval = true)
  private void bindC3p0DataSource(String beanName, DataSource dataSource) {
    if (dataSource instanceof ComboPooledDataSource c3p0) {
      String poolName = stripDataSourceSuffix(beanName);
      registerC3p0Metrics(c3p0, poolName);
    }
  }

  @Deprecated(since = "v43", forRemoval = true)
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

  static class DataSourcePoolMetricsEnabledCondition extends MetricsEnabler {
    @Override
    protected ConfigurationKey getConfigKey() {
      return MONITORING_DBPOOL_ENABLED;
    }
  }
}
