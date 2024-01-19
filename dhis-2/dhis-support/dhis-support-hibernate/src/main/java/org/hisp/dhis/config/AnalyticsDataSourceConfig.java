/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.config;

import static org.hisp.dhis.config.DataSourceConfig.createLoggingDataSource;
import static org.hisp.dhis.datasource.DatabasePoolUtils.ConfigKeyMapper.ANALYTICS;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_CONNECTION_URL;

import com.google.common.base.MoreObjects;
import java.beans.PropertyVetoException;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.datasource.DatabasePoolUtils;
import org.hisp.dhis.datasource.ReadOnlyDataSourceManager;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class AnalyticsDataSourceConfig {

  private final DhisConfigurationProvider dhisConfig;

  @Bean("analyticsDataSource")
  @DependsOn("analyticsActualDataSource")
  public DataSource jdbcDataSource(
      @Qualifier("analyticsActualDataSource") DataSource actualDataSource) {
    return createLoggingDataSource(dhisConfig, actualDataSource);
  }

  @Bean("analyticsActualDataSource")
  public DataSource jdbcActualDataSource(
      @Qualifier("actualDataSource") DataSource actualDataSource) {

    String jdbcUrl = dhisConfig.getProperty(ANALYTICS_CONNECTION_URL);

    if (StringUtils.isNotBlank(jdbcUrl)) {
      return createActualDataSourceFromAnalyticsConfiguration();
    }
    // if no analytics connection url is specified, use the same datasource as the main database
    log.info(
        "No analytics connection url is specified ("
            + ANALYTICS_CONNECTION_URL.getKey()
            + "). Analytics won't have a dedicated datasource");
    return actualDataSource;
  }

  private DataSource createActualDataSourceFromAnalyticsConfiguration() {
    String jdbcUrl = dhisConfig.getProperty(ANALYTICS_CONNECTION_URL);

    String dbPoolType = dhisConfig.getProperty(ConfigurationKey.DB_POOL_TYPE);

    DatabasePoolUtils.PoolConfig poolConfig =
        DatabasePoolUtils.PoolConfig.builder()
            .dhisConfig(dhisConfig)
            .mapper(ANALYTICS)
            .dbPoolType(dbPoolType)
            .build();

    try {
      return DatabasePoolUtils.createDbPool(poolConfig);
    } catch (SQLException | PropertyVetoException e) {
      String message =
          String.format(
              "Connection test failed for analytics database pool, " + "jdbcUrl: '%s'", jdbcUrl);

      log.error(message);
      log.error(DebugUtils.getStackTrace(e));

      throw new IllegalStateException(message, e);
    }
  }

  @Bean("analyticsNamedParameterJdbcTemplate")
  @DependsOn("analyticsDataSource")
  public NamedParameterJdbcTemplate namedParameterJdbcTemplate(
      @Qualifier("analyticsDataSource") DataSource dataSource) {
    return new NamedParameterJdbcTemplate(dataSource);
  }

  @Bean("executionPlanJdbcTemplate")
  @DependsOn("analyticsDataSource")
  public JdbcTemplate executionPlanJdbcTemplate(
      @Qualifier("analyticsDataSource") DataSource dataSource) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.setFetchSize(1000);
    jdbcTemplate.setQueryTimeout(10);
    return jdbcTemplate;
  }

  @Bean("analyticsReadOnlyJdbcTemplate")
  @DependsOn("analyticsDataSource")
  public JdbcTemplate readOnlyJdbcTemplate(
      @Qualifier("analyticsDataSource") DataSource dataSource) {
    ReadOnlyDataSourceManager manager = new ReadOnlyDataSourceManager(dhisConfig);

    JdbcTemplate jdbcTemplate =
        new JdbcTemplate(MoreObjects.firstNonNull(manager.getReadOnlyDataSource(), dataSource));
    jdbcTemplate.setFetchSize(1000);

    return jdbcTemplate;
  }

  @Bean("analyticsJdbcTemplate")
  @DependsOn("analyticsDataSource")
  public JdbcTemplate jdbcTemplate(@Qualifier("analyticsDataSource") DataSource dataSource) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.setFetchSize(1000);
    return jdbcTemplate;
  }
}
