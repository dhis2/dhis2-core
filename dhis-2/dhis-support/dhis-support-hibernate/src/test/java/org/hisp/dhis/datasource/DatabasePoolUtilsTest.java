/*
 * Copyright (c) 2004-2024, University of Oslo
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
import javax.sql.DataSource;
import org.hisp.dhis.datasource.model.DbPoolConfig;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class DatabasePoolUtilsTest {
  @BeforeEach
  public void beforeEach() throws SQLException {
    StubDriver jdbcDriver = new StubDriver();
    DriverManager.registerDriver(jdbcDriver);
  }

  @Test
  void testCreateDbPoolWhenDbPoolTypeIsUnPooled() throws PropertyVetoException, SQLException {

    DhisConfigurationProvider mockDhisConfigurationProvider = mock(DhisConfigurationProvider.class);
    given(mockDhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_DRIVER_CLASS))
        .willReturn("org.hisp.dhis.datasource.StubDriver");

    DbPoolConfig.DbPoolConfigBuilder poolConfigBuilder =
        DbPoolConfig.builder()
            .dbPoolType(DatabasePoolUtils.DbPoolType.UNPOOLED.name())
            .jdbcUrl("jdbc:fake:db")
            .username("")
            .password("")
            .dhisConfig(mockDhisConfigurationProvider);

    DataSource dataSource = DatabasePoolUtils.createDbPool(poolConfigBuilder.build());
    assertInstanceOf(DriverManagerDataSource.class, dataSource);
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
    given(mockDhisConfigurationProvider.getIntProperty(ConfigurationKey.CONNECTION_POOL_TIMEOUT))
        .willReturn(250);

    DbPoolConfig.DbPoolConfigBuilder poolConfigBuilder =
        DbPoolConfig.builder()
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
    assertEquals(250, ((ComboPooledDataSource) dataSource).getCheckoutTimeout());
  }

  @Test
  void testCreateDbPoolWhenDbPoolTypeIsHikari() throws PropertyVetoException, SQLException {
    DhisConfigurationProvider mockDhisConfigurationProvider = mock(DhisConfigurationProvider.class);
    given(mockDhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_DRIVER_CLASS))
        .willReturn("org.hisp.dhis.datasource.StubDriver");
    given(mockDhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_POOL_TIMEOUT))
        .willReturn("250");
    given(mockDhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_POOL_MAX_IDLE_TIME))
        .willReturn("120000");
    given(
            mockDhisConfigurationProvider.getProperty(
                ConfigurationKey.CONNECTION_POOL_KEEP_ALIVE_TIME_SECONDS))
        .willReturn("120");
    given(
            mockDhisConfigurationProvider.getProperty(
                ConfigurationKey.CONNECTION_POOL_MAX_LIFETIME_SECONDS))
        .willReturn("120");
    given(mockDhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_POOL_MIN_IDLE))
        .willReturn("10");
    given(
            mockDhisConfigurationProvider.getProperty(
                ConfigurationKey.CONNECTION_POOL_VALIDATION_TIMEOUT))
        .willReturn("250");

    DbPoolConfig.DbPoolConfigBuilder poolConfigBuilder =
        DbPoolConfig.builder()
            .dbPoolType(DatabasePoolUtils.DbPoolType.HIKARI.name())
            .jdbcUrl("jdbc:fake:db")
            .username("")
            .password("")
            .maxPoolSize("1")
            .acquireIncrement("1")
            .maxIdleTime("120000")
            .dhisConfig(mockDhisConfigurationProvider);

    DataSource dataSource = DatabasePoolUtils.createDbPool(poolConfigBuilder.build());
    assertInstanceOf(HikariDataSource.class, dataSource);
  }
}
