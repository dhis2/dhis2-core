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

import com.google.common.base.MoreObjects;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import javax.sql.DataSource;
import lombok.Builder;
import lombok.Data;
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

  public static DataSource createHikariDbPool(PoolConfig config) throws SQLException {
    DhisConfigurationProvider dhisConfig = config.getDhisConfig();

    final String driverClassName = dhisConfig.getProperty(ConfigurationKey.CONNECTION_DRIVER_CLASS);
    final String jdbcUrl =
        MoreObjects.firstNonNull(
            config.getJdbcUrl(), dhisConfig.getProperty(ConfigurationKey.CONNECTION_URL));
    final String username =
        MoreObjects.firstNonNull(
            config.getUsername(), dhisConfig.getProperty(ConfigurationKey.CONNECTION_USERNAME));
    final String password =
        MoreObjects.firstNonNull(
            config.getPassword(), dhisConfig.getProperty(ConfigurationKey.CONNECTION_PASSWORD));
    final long connectionTimeout =
        Long.parseLong(dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_TIMEOUT));
    final long validationTimeout =
        Long.parseLong(dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_VALIDATION_TIMEOUT));
    final int maxPoolSize =
        Integer.parseInt(
            MoreObjects.firstNonNull(
                config.getMaxPoolSize(),
                dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_MAX_SIZE)));
    final String connectionTestQuery =
        dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_TEST_QUERY);

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

  public static DataSource createC3p0DbPool(PoolConfig config)
      throws PropertyVetoException, SQLException {
    DhisConfigurationProvider dhisConfig = config.getDhisConfig();

    final String driverClassName = dhisConfig.getProperty(ConfigurationKey.CONNECTION_DRIVER_CLASS);
    final String jdbcUrl =
        MoreObjects.firstNonNull(
            config.getJdbcUrl(), dhisConfig.getProperty(ConfigurationKey.CONNECTION_URL));
    final String username =
        MoreObjects.firstNonNull(
            config.getUsername(), dhisConfig.getProperty(ConfigurationKey.CONNECTION_USERNAME));
    final String password =
        MoreObjects.firstNonNull(
            config.getPassword(), dhisConfig.getProperty(ConfigurationKey.CONNECTION_PASSWORD));
    final int maxPoolSize =
        Integer.parseInt(
            MoreObjects.firstNonNull(
                config.getMaxPoolSize(),
                dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_MAX_SIZE)));
    final int acquireIncrement =
        Integer.parseInt(
            MoreObjects.firstNonNull(
                config.getAcquireIncrement(),
                dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_ACQUIRE_INCR)));
    final int maxIdleTime =
        Integer.parseInt(
            MoreObjects.firstNonNull(
                config.maxIdleTime,
                dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_MAX_IDLE_TIME)));

    final int minPoolSize =
        Integer.parseInt(dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_MIN_SIZE));
    final int initialSize =
        Integer.parseInt(dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_INITIAL_SIZE));
    boolean testOnCheckIn = dhisConfig.isEnabled(ConfigurationKey.CONNECTION_POOL_TEST_ON_CHECKIN);
    boolean testOnCheckOut =
        dhisConfig.isEnabled(ConfigurationKey.CONNECTION_POOL_TEST_ON_CHECKOUT);
    final int maxIdleTimeExcessConnections =
        Integer.parseInt(
            dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_MAX_IDLE_TIME_EXCESS_CON));
    final int idleConnectionTestPeriod =
        Integer.parseInt(
            dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_IDLE_CON_TEST_PERIOD));
    final String preferredTestQuery =
        dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_TEST_QUERY);
    final int numHelperThreads =
        Integer.parseInt(dhisConfig.getProperty(ConfigurationKey.CONNECTION_POOL_NUM_THREADS));

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
