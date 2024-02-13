/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariDataSource;
import java.beans.PropertyVetoException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.hisp.dhis.datasource.model.PoolConfig;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class DatabasePoolUtilsTest {

  private static ListAppender appender;

  @BeforeAll
  public static void beforeAll() {
    LoggerContext loggerContext = LoggerContext.getContext(false);
    Logger logger = (Logger) loggerContext.getLogger(DatabasePoolUtils.class);
    appender = new ListAppender("Test");
    appender.start();
    loggerContext.getConfiguration().addLoggerAppender(logger, appender);
  }

  @BeforeEach
  public void beforeEach() throws SQLException {
    appender.clear();
    StubDriver jdbcDriver = new StubDriver();
    DriverManager.registerDriver(jdbcDriver);
  }

  @Test
  void testCreateDbPoolWhenDbPoolTypeIsUnPooled() throws PropertyVetoException, SQLException {

    DhisConfigurationProvider mockDhisConfigurationProvider = mock(DhisConfigurationProvider.class);
    given(mockDhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_DRIVER_CLASS))
        .willReturn("org.hisp.dhis.datasource.StubDriver");

    PoolConfig.PoolConfigBuilder poolConfigBuilder =
        PoolConfig.builder()
            .dbPoolType(DatabasePoolUtils.DbPoolType.UNPOOLED.name())
            .jdbcUrl("jdbc:fake:db")
            .username("")
            .password("")
            .dhisConfig(mockDhisConfigurationProvider);

    DataSource dataSource = DatabasePoolUtils.createDbPool(poolConfigBuilder.build());
    assertInstanceOf(DriverManagerDataSource.class, dataSource);

    assertEquals("Database pool type value is [UNPOOLED]", getLogEntry());
  }

  @Test
  void testCreateDbPoolWhenDbPoolTypeIsC3P0() throws PropertyVetoException, SQLException {
    DhisConfigurationProvider mockDhisConfigurationProvider = mock(DhisConfigurationProvider.class);
    given(mockDhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_DRIVER_CLASS))
        .willReturn("mock");
    given(mockDhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_POOL_MIN_SIZE))
        .willReturn("1");
    given(mockDhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_POOL_INITIAL_SIZE))
        .willReturn("1");
    given(
            mockDhisConfigurationProvider.getProperty(
                ConfigurationKey.CONNECTION_POOL_MAX_IDLE_TIME_EXCESS_CON))
        .willReturn(String.valueOf(ThreadLocalRandom.current().nextInt()));
    given(
            mockDhisConfigurationProvider.getProperty(
                ConfigurationKey.CONNECTION_POOL_IDLE_CON_TEST_PERIOD))
        .willReturn(String.valueOf(ThreadLocalRandom.current().nextInt()));
    given(mockDhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_POOL_NUM_THREADS))
        .willReturn("1");

    PoolConfig.PoolConfigBuilder poolConfigBuilder =
        PoolConfig.builder()
            .dbPoolType(DatabasePoolUtils.DbPoolType.C3P0.name())
            .jdbcUrl("jdbc:fake:db")
            .username("")
            .password("")
            .maxPoolSize("1")
            .acquireIncrement("1")
            .acquireRetryAttempts("1")
            .acquireRetryDelay("1")
            .maxIdleTime(String.valueOf(ThreadLocalRandom.current().nextInt()))
            .dhisConfig(mockDhisConfigurationProvider);

    DataSource dataSource = DatabasePoolUtils.createDbPool(poolConfigBuilder.build());
    assertInstanceOf(ComboPooledDataSource.class, dataSource);

    assertEquals("Database pool type value is [C3P0]", getLogEntry());
  }

  @Test
  void testCreateDbPoolWhenDbPoolTypeIsHikari() throws PropertyVetoException, SQLException {
    DhisConfigurationProvider mockDhisConfigurationProvider = mock(DhisConfigurationProvider.class);
    given(mockDhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_DRIVER_CLASS))
        .willReturn("org.hisp.dhis.datasource.StubDriver");
    given(mockDhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_POOL_TIMEOUT))
        .willReturn("250");
    given(
            mockDhisConfigurationProvider.getProperty(
                ConfigurationKey.CONNECTION_POOL_VALIDATION_TIMEOUT))
        .willReturn("250");

    PoolConfig.PoolConfigBuilder poolConfigBuilder =
        PoolConfig.builder()
            .dbPoolType(DatabasePoolUtils.DbPoolType.HIKARI.name())
            .jdbcUrl("jdbc:fake:db")
            .username("")
            .password("")
            .maxPoolSize("1")
            .acquireIncrement("1")
            .maxIdleTime(String.valueOf(ThreadLocalRandom.current().nextInt()))
            .dhisConfig(mockDhisConfigurationProvider);

    DataSource dataSource = DatabasePoolUtils.createDbPool(poolConfigBuilder.build());
    assertInstanceOf(HikariDataSource.class, dataSource);

    assertEquals("Database pool type value is [HIKARI]", getLogEntry());
  }

  private String getLogEntry() {
    return appender.getEvents().stream()
        .map(event -> event.getMessage().getFormattedMessage())
        .collect(Collectors.toList())
        .get(0);
  }
}
