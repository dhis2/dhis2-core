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
package org.hisp.dhis.datasource;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_DRIVER_CLASS;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_PASSWORD;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_ACQUIRE_INCR;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_ACQUIRE_RETRY_ATTEMPTS;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_ACQUIRE_RETRY_DELAY;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_IDLE_CON_TEST_PERIOD;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_INITIAL_SIZE;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_MAX_IDLE_TIME;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_MAX_IDLE_TIME_EXCESS_CON;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_MAX_SIZE;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_MIN_SIZE;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_NUM_THREADS;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_TEST_ON_CHECKIN;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_TEST_ON_CHECKOUT;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_TEST_QUERY;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_TIMEOUT;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_VALIDATION_TIMEOUT;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_URL;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_USERNAME;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_DRIVER_CLASS;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_PASSWORD;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_ACQUIRE_INCR;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_ACQUIRE_RETRY_ATTEMPTS;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_ACQUIRE_RETRY_DELAY;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_IDLE_CON_TEST_PERIOD;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_INITIAL_SIZE;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_MAX_IDLE_TIME;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_MAX_IDLE_TIME_EXCESS_CON;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_MAX_SIZE;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_MIN_SIZE;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_NUM_THREADS;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_TEST_ON_CHECKIN;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_TEST_ON_CHECKOUT;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_TEST_QUERY;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_TIMEOUT;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_VALIDATION_TIMEOUT;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_POOL_WARN_MAX_AGE;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_URL;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_USERNAME;

import com.google.common.collect.ImmutableMap;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.datasource.model.DbPoolConfig;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public final class DatabasePoolUtils {

  /**
   * This enum maps each database configuration key into a corresponding analytics configuration
   * key. This is used to allow an analytics database to be configured separately from the main
   * database.
   */
  @RequiredArgsConstructor
  public enum ConfigKeyMapper {
    ANALYTICS(
        ImmutableMap.<ConfigurationKey, ConfigurationKey>builder()
            /* common keys for all connection pools */
            .put(CONNECTION_URL, ANALYTICS_CONNECTION_URL)
            .put(CONNECTION_USERNAME, ANALYTICS_CONNECTION_USERNAME)
            .put(CONNECTION_PASSWORD, ANALYTICS_CONNECTION_PASSWORD)
            .put(CONNECTION_DRIVER_CLASS, ANALYTICS_CONNECTION_DRIVER_CLASS)
            .put(CONNECTION_POOL_MAX_SIZE, ANALYTICS_CONNECTION_POOL_MAX_SIZE)
            .put(CONNECTION_POOL_TEST_QUERY, ANALYTICS_CONNECTION_POOL_TEST_QUERY)
            /* hikari-specific */
            .put(CONNECTION_POOL_TIMEOUT, ANALYTICS_CONNECTION_POOL_TIMEOUT)
            .put(CONNECTION_POOL_VALIDATION_TIMEOUT, ANALYTICS_CONNECTION_POOL_VALIDATION_TIMEOUT)
            /* C3P0-specific */
            .put(CONNECTION_POOL_ACQUIRE_INCR, ANALYTICS_CONNECTION_POOL_ACQUIRE_INCR)
            .put(
                CONNECTION_POOL_ACQUIRE_RETRY_ATTEMPTS,
                ANALYTICS_CONNECTION_POOL_ACQUIRE_RETRY_ATTEMPTS)
            .put(CONNECTION_POOL_ACQUIRE_RETRY_DELAY, ANALYTICS_CONNECTION_POOL_ACQUIRE_RETRY_DELAY)
            .put(CONNECTION_POOL_MAX_IDLE_TIME, ANALYTICS_CONNECTION_POOL_MAX_IDLE_TIME)
            .put(CONNECTION_POOL_MIN_SIZE, ANALYTICS_CONNECTION_POOL_MIN_SIZE)
            .put(CONNECTION_POOL_INITIAL_SIZE, ANALYTICS_CONNECTION_POOL_INITIAL_SIZE)
            .put(CONNECTION_POOL_TEST_ON_CHECKIN, ANALYTICS_CONNECTION_POOL_TEST_ON_CHECKIN)
            .put(CONNECTION_POOL_TEST_ON_CHECKOUT, ANALYTICS_CONNECTION_POOL_TEST_ON_CHECKOUT)
            .put(
                CONNECTION_POOL_MAX_IDLE_TIME_EXCESS_CON,
                ANALYTICS_CONNECTION_POOL_MAX_IDLE_TIME_EXCESS_CON)
            .put(
                CONNECTION_POOL_IDLE_CON_TEST_PERIOD,
                ANALYTICS_CONNECTION_POOL_IDLE_CON_TEST_PERIOD)
            .put(CONNECTION_POOL_NUM_THREADS, ANALYTICS_CONNECTION_POOL_NUM_THREADS)
            .build()),
    POSTGRESQL(Map.of());

    private final Map<ConfigurationKey, ConfigurationKey> keyMap;

    public ConfigurationKey getConfigKey(ConfigurationKey key) {
      return keyMap.getOrDefault(key, key);
    }
  }

  public enum DbPoolType {
    C3P0,
    HIKARI,
    UNPOOLED
  }

  public static DataSource createDbPool(DbPoolConfig config)
      throws PropertyVetoException, SQLException {
    Objects.requireNonNull(config);

    ConfigKeyMapper mapper = config.getMapper();
    DbPoolType dbPoolType = DbPoolType.valueOf(config.getDbPoolType().toUpperCase());
    log.info("Database pool type value is '{}'", dbPoolType);

    DhisConfigurationProvider dhisConfig = config.getDhisConfig();
    final String driverClassName =
        firstNonNull(
            config.getDriverClassName(),
            dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_DRIVER_CLASS)));
    final String jdbcUrl =
        firstNonNull(
            config.getJdbcUrl(), dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_URL)));
    final String username =
        firstNonNull(
            config.getUsername(), dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_USERNAME)));
    final String password =
        firstNonNull(
            config.getPassword(), dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_PASSWORD)));

    final DataSource dataSource =
        switch (dbPoolType) {
          case C3P0 -> createC3p0DbPool(username, password, driverClassName, jdbcUrl, config);
          case HIKARI -> createHikariDbPool(username, password, driverClassName, jdbcUrl, config);
          case UNPOOLED -> createNoPoolDataSource(username, password, driverClassName, jdbcUrl);
          default ->
              throw new IllegalArgumentException(
                  TextUtils.format(
                      "Database pool type value is invalid, could not create database pool: '{}'",
                      config.getDbPoolType()));
        };

    testConnection(dataSource);
    return dataSource;
  }

  public static void testConnection(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.executeQuery("select 'connection_test' as connection_test;");
    } catch (SQLException e) {
      log.error(e.getMessage());
    }
  }

  /** Create a data source based on a Hikari connection pool. */
  private static DataSource createHikariDbPool(
      String username,
      String password,
      String driverClassName,
      String jdbcUrl,
      DbPoolConfig config) {
    ConfigKeyMapper mapper = config.getMapper();

    DhisConfigurationProvider dhisConfig = config.getDhisConfig();

    final long connectionTimeout =
        parseLong(dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_TIMEOUT)));
    final long validationTimeout =
        parseLong(dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_VALIDATION_TIMEOUT)));
    final int maxPoolSize =
        Integer.parseInt(
            firstNonNull(
                config.getMaxPoolSize(),
                dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_MAX_SIZE))));
    final String connectionTestQuery =
        dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_TEST_QUERY));

    HikariConfig hc = new HikariConfig();
    hc.setPoolName("HikariDataSource_" + CodeGenerator.generateCode(10));
    hc.setDriverClassName(driverClassName);
    hc.setJdbcUrl(jdbcUrl);
    hc.setUsername(username);
    hc.setPassword(password);
    hc.addDataSourceProperty("cachePrepStmts", "true");
    hc.addDataSourceProperty("prepStmtCacheSize", "250");
    hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    hc.setConnectionTestQuery(connectionTestQuery);
    final String leakThresholdStr =
        dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_WARN_MAX_AGE));

    if (leakThresholdStr != null && !leakThresholdStr.isBlank()) {
      try {
        long leakThreshold = Long.parseLong(leakThresholdStr);

        // Enforce Hikari limits
        final long MIN_LEAK_THRESHOLD = 2000L;
        final long maxLifetime = hc.getMaxLifetime();
        if (leakThreshold >= MIN_LEAK_THRESHOLD && leakThreshold < maxLifetime) {
          log.info("Enabling HikariCP leak detection with threshold: {}ms", leakThreshold);
          hc.setLeakDetectionThreshold(leakThreshold);
        } else {
          log.warn(
              "Leak detection threshold {}ms is out of bounds (must be >= {}ms and < maxLifetime={}ms). Skipping.",
              leakThreshold,
              MIN_LEAK_THRESHOLD,
              maxLifetime);
        }
      } catch (NumberFormatException e) {
        log.warn("Invalid leak detection threshold value '{}', skipping.", leakThresholdStr);
      }
    }

    HikariDataSource ds = new HikariDataSource(hc);
    ds.setConnectionTimeout(connectionTimeout);
    ds.setValidationTimeout(validationTimeout);
    ds.setMaximumPoolSize(maxPoolSize);

    return ds;
  }

  /** Create a data source based on a C3p0 connection pool. */
  private static ComboPooledDataSource createC3p0DbPool(
      String username, String password, String driverClassName, String jdbcUrl, DbPoolConfig config)
      throws PropertyVetoException {
    ConfigKeyMapper mapper = config.getMapper();
    DhisConfigurationProvider dhisConfig = config.getDhisConfig();

    final int maxPoolSize =
        parseInt(
            firstNonNull(
                config.getMaxPoolSize(),
                dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_MAX_SIZE))));
    final int acquireIncrement =
        parseInt(
            firstNonNull(
                config.getAcquireIncrement(),
                dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_ACQUIRE_INCR))));
    final int acquireRetryAttempts =
        parseInt(
            firstNonNull(
                config.getAcquireRetryAttempts(),
                dhisConfig.getProperty(
                    mapper.getConfigKey(CONNECTION_POOL_ACQUIRE_RETRY_ATTEMPTS))));
    final int acquireRetryDelay =
        parseInt(
            firstNonNull(
                config.getAcquireRetryDelay(),
                dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_ACQUIRE_RETRY_DELAY))));
    final int maxIdleTime =
        parseInt(
            firstNonNull(
                config.getMaxIdleTime(),
                dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_MAX_IDLE_TIME))));

    final int minPoolSize =
        parseInt(dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_MIN_SIZE)));
    final int initialSize =
        parseInt(dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_INITIAL_SIZE)));
    boolean testOnCheckIn =
        dhisConfig.isEnabled(mapper.getConfigKey(CONNECTION_POOL_TEST_ON_CHECKIN));
    boolean testOnCheckOut =
        dhisConfig.isEnabled(mapper.getConfigKey(CONNECTION_POOL_TEST_ON_CHECKOUT));
    final int maxIdleTimeExcessConnections =
        parseInt(
            dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_MAX_IDLE_TIME_EXCESS_CON)));
    final int idleConnectionTestPeriod =
        parseInt(dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_IDLE_CON_TEST_PERIOD)));
    final String preferredTestQuery =
        dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_TEST_QUERY));
    final int numHelperThreads =
        parseInt(dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_POOL_NUM_THREADS)));

    final ComboPooledDataSource pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass(driverClassName);
    pooledDataSource.setJdbcUrl(jdbcUrl);
    pooledDataSource.setUser(username);
    pooledDataSource.setPassword(password);
    pooledDataSource.setMaxPoolSize(maxPoolSize);
    pooledDataSource.setMinPoolSize(minPoolSize);
    pooledDataSource.setInitialPoolSize(initialSize);
    pooledDataSource.setAcquireIncrement(acquireIncrement);
    pooledDataSource.setAcquireRetryAttempts(acquireRetryAttempts);
    pooledDataSource.setAcquireRetryDelay(acquireRetryDelay);
    pooledDataSource.setMaxIdleTime(maxIdleTime);
    pooledDataSource.setTestConnectionOnCheckin(testOnCheckIn);
    pooledDataSource.setTestConnectionOnCheckout(testOnCheckOut);
    pooledDataSource.setMaxIdleTimeExcessConnections(maxIdleTimeExcessConnections);
    pooledDataSource.setIdleConnectionTestPeriod(idleConnectionTestPeriod);
    pooledDataSource.setPreferredTestQuery(preferredTestQuery);
    pooledDataSource.setNumHelperThreads(numHelperThreads);

    return pooledDataSource;
  }

  /** Creates a data source with no connection pool. */
  private static DriverManagerDataSource createNoPoolDataSource(
      String username, String password, String driverClassName, String jdbcUrl) {
    final DriverManagerDataSource unPooledDataSource = new DriverManagerDataSource();
    unPooledDataSource.setDriverClassName(driverClassName);
    unPooledDataSource.setUrl(jdbcUrl);
    unPooledDataSource.setUsername(username);
    unPooledDataSource.setPassword(password);

    return unPooledDataSource;
  }
}
