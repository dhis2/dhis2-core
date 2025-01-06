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
package org.hisp.dhis.test.config;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.hisp.dhis.datasource.DatabasePoolUtils;
import org.hisp.dhis.datasource.model.DbPoolConfig;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.test.h2.H2SqlFunction;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** Use this Spring configuration for tests relying on the H2 in-memory DB. */
@Configuration
public class H2TestConfig {
  @Bean
  public DhisConfigurationProvider dhisConfigurationProvider() {
    return new H2DhisConfigurationProvider();
  }

  public static class NoOpFlyway {}

  @Bean
  public NoOpFlyway flyway() {
    return new NoOpFlyway();
  }

  @Bean(name = {"namedParameterJdbcTemplate", "analyticsNamedParameterJdbcTemplate"})
  @Primary
  public NamedParameterJdbcTemplate namedParameterJdbcTemplate(
      @Qualifier("dataSource") DataSource dataSource) {
    return new NamedParameterJdbcTemplate(dataSource);
  }

  @Bean(name = {"dataSource", "analyticsDataSource"})
  public DataSource actualDataSource(DhisConfigurationProvider config)
      throws SQLException, PropertyVetoException {
    String dbPoolType = config.getProperty(ConfigurationKey.DB_POOL_TYPE);

    DbPoolConfig.PoolConfigBuilder builder = DbPoolConfig.builder();
    builder.dhisConfig(config);
    builder.dbPoolType(dbPoolType);

    final DataSource dbPool = DatabasePoolUtils.createDbPool(builder.build());
    H2SqlFunction.registerH2Functions(dbPool);

    return dbPool;
  }
}
