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
package org.hisp.dhis.test.config;

import static org.hisp.dhis.config.HibernateConfig.getAdditionalProperties;
import static org.hisp.dhis.config.HibernateConfig.loadResources;

import jakarta.persistence.EntityManagerFactory;
import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.tool.schema.Action;
import org.hisp.dhis.datasource.DatabasePoolUtils;
import org.hisp.dhis.datasource.model.DbPoolConfig;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.test.h2.H2SqlFunction;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

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

  // NOTE: this must stay in sync with HibernateConfig.entityManagerFactory apart from the
  // HB2DDL_AUTO override, only then do we also test the actual EntityManagerFactory
  @Bean
  @DependsOn({"flyway"})
  public EntityManagerFactory entityManagerFactory(
      DhisConfigurationProvider dhisConfig, @Qualifier("actualDataSource") DataSource dataSource) {
    HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
    adapter.setDatabasePlatform(dhisConfig.getProperty(ConfigurationKey.CONNECTION_DIALECT));
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setJpaVendorAdapter(adapter);
    factory.setPersistenceUnitName("dhis");
    factory.setPersistenceProviderClass(HibernatePersistenceProvider.class);
    factory.setDataSource(dataSource);
    factory.setPackagesToScan("org.hisp.dhis");
    factory.setMappingResources(loadResources());
    Properties jpaProperties = getAdditionalProperties(dhisConfig);
    // let hibernate create the DB schema for H2 tests as no flyway migrations are run
    jpaProperties.put(AvailableSettings.HBM2DDL_AUTO, Action.UPDATE.getExternalHbm2ddlName());
    factory.setJpaProperties(jpaProperties);
    factory.afterPropertiesSet();
    return factory.getObject();
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

    DbPoolConfig.DbPoolConfigBuilder builder = DbPoolConfig.builder();
    builder.dhisConfig(config);
    builder.dbPoolType(dbPoolType);

    final DataSource dbPool = DatabasePoolUtils.createDbPool(builder.build(), "h2test");
    H2SqlFunction.registerH2Functions(dbPool);

    return dbPool;
  }
}
