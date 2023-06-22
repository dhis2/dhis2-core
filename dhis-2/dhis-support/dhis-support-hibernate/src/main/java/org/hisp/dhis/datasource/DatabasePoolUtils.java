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
package org.hisp.dhis.datasource;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_DRIVER_CLASS;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_PASSWORD;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_POOL_ACQUIRE_INCR;
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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.hibernate.HibernateConfigurationProvider;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class DatabasePoolUtils {

  @RequiredArgsConstructor
  public enum ConfigKeyMapper {
    ANALYTICS(
        ImmutableMap.<ConfigurationKey, ConfigurationKey>builder()
            /* common */
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
    POSTGRESQL(Collections.emptyMap());

    private final Map<ConfigurationKey, ConfigurationKey> keyMap;

    public ConfigurationKey getConfigKey(ConfigurationKey key) {
      return keyMap.getOrDefault(key, key);
    }
  }

  public enum dbPoolTypes {
    C3P0,
    HIKARI
  }

  @Data
  @Builder
  public static class PoolConfig {
    private String dbPoolType;

    private DhisConfigurationProvider dhisConfig;

    private HibernateConfigurationProvider hibernateConfig;

    private String jdbcUrl;

    private String username;

    private String password;

    private String maxPoolSize;

    private String acquireIncrement;

    private String maxIdleTime;

    private ConfigKeyMapper mapper;

    public ConfigKeyMapper getMapper() {
      return Optional.ofNullable(mapper).orElse(ConfigKeyMapper.POSTGRESQL);
    }
  }

  public static DataSource createDbPool(PoolConfig config)
      throws PropertyVetoException, SQLException {
    Objects.requireNonNull(config);

    dbPoolTypes dbType = dbPoolTypes.valueOf(config.dbPoolType.toUpperCase());

    if (dbType == dbPoolTypes.C3P0) {
      return createC3p0DbPool(config);
    } else if (dbType == dbPoolTypes.HIKARI) {
      return createHikariDbPool(config);
    }

    String msg =
        String.format(
            "Database pool type value is invalid, can not create a database pool! Value='%s'",
            config.dbPoolType);
    log.error(msg);

    throw new IllegalArgumentException(msg);
  }

  private static DataSource createHikariDbPool(PoolConfig config) throws SQLException {
    ConfigKeyMapper mapper = config.getMapper();

    DhisConfigurationProvider dhisConfig = config.getDhisConfig();

    final String driverClassName =
        dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_DRIVER_CLASS));

    final String jdbcUrl =
        firstNonNull(
            config.getJdbcUrl(), dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_URL)));

    final String username =
        firstNonNull(
            config.getUsername(), dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_USERNAME)));

    final String password =
        firstNonNull(
            config.getPassword(), dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_PASSWORD)));

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

    HikariDataSource ds = new HikariDataSource(hc);
    ds.setConnectionTimeout(connectionTimeout);
    ds.setValidationTimeout(validationTimeout);
    ds.setMaximumPoolSize(maxPoolSize);

    testConnection(ds);

    return ds;
  }

  private static DataSource createC3p0DbPool(PoolConfig config)
      throws PropertyVetoException, SQLException {
    ConfigKeyMapper mapper = config.getMapper();

    DhisConfigurationProvider dhisConfig = config.getDhisConfig();

    final String driverClassName =
        dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_DRIVER_CLASS));
    final String jdbcUrl =
        firstNonNull(
            config.getJdbcUrl(), dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_URL)));
    final String username =
        firstNonNull(
            config.getUsername(), dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_USERNAME)));
    final String password =
        firstNonNull(
            config.getPassword(), dhisConfig.getProperty(mapper.getConfigKey(CONNECTION_PASSWORD)));
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
    final int maxIdleTime =
        parseInt(
            firstNonNull(
                config.maxIdleTime,
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

    ComboPooledDataSource dataSource = new ComboPooledDataSource();
    dataSource.setDriverClass(driverClassName);
    dataSource.setJdbcUrl(jdbcUrl);
    dataSource.setUser(username);
    dataSource.setPassword(password);
    dataSource.setMaxPoolSize(maxPoolSize);
    dataSource.setMinPoolSize(minPoolSize);
    dataSource.setInitialPoolSize(initialSize);
    dataSource.setAcquireIncrement(acquireIncrement);
    dataSource.setMaxIdleTime(maxIdleTime);
    dataSource.setTestConnectionOnCheckin(testOnCheckIn);
    dataSource.setTestConnectionOnCheckout(testOnCheckOut);
    dataSource.setMaxIdleTimeExcessConnections(maxIdleTimeExcessConnections);
    dataSource.setIdleConnectionTestPeriod(idleConnectionTestPeriod);
    dataSource.setPreferredTestQuery(preferredTestQuery);
    dataSource.setNumHelperThreads(numHelperThreads);

    testConnection(dataSource);

    return dataSource;
  }

  public static void testConnection(DataSource dataSource) throws SQLException {
    Connection conn = dataSource.getConnection();

    try (Statement stmt = conn.createStatement()) {
      stmt.executeQuery("select 'connection_test' as connection_test;");
    }
  }
}
