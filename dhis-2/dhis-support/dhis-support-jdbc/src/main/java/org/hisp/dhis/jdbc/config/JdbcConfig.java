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
package org.hisp.dhis.jdbc.config;

import com.google.common.collect.Lists;
import javax.sql.DataSource;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.jdbc.dialect.StatementDialectFactoryBean;
import org.hisp.dhis.jdbc.statementbuilder.StatementBuilderFactoryBean;
import org.hisp.quick.StatementDialect;
import org.hisp.quick.StatementInterceptor;
import org.hisp.quick.configuration.JdbcConfigurationFactoryBean;
import org.hisp.quick.factory.DefaultBatchHandlerFactory;
import org.hisp.quick.statement.JdbcStatementManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Luciano Fiandesio
 */
@Configuration
public class JdbcConfig {
  @Autowired private DataSource dataSource;

  @Autowired private DhisConfigurationProvider dhisConfigurationProvider;

  @Bean
  public JdbcStatementManager statementManager() throws Exception {
    JdbcStatementManager jdbcStatementManager = new JdbcStatementManager();
    jdbcStatementManager.setJdbcConfiguration(jdbcConfiguration().getObject());
    return jdbcStatementManager;
  }

  @Bean(initMethod = "init")
  public StatementDialectFactoryBean statementDialect() {
    return new StatementDialectFactoryBean(
        dhisConfigurationProvider.getProperty(ConfigurationKey.CONNECTION_DIALECT));
  }

  @Bean(initMethod = "init")
  public JdbcConfigurationFactoryBean jdbcConfiguration() {
    JdbcConfigurationFactoryBean jdbcConf = new JdbcConfigurationFactoryBean();
    StatementDialect statementDialect = statementDialect().getObject();
    jdbcConf.setDialect(statementDialect);
    jdbcConf.setDataSource(dataSource);

    return jdbcConf;
  }

  @Bean(initMethod = "init")
  public StatementBuilderFactoryBean statementBuilder() {
    return new StatementBuilderFactoryBean(statementDialect().getObject());
  }

  @Bean
  public DefaultBatchHandlerFactory batchHandlerFactory() throws Exception {
    DefaultBatchHandlerFactory defaultBatchHandlerFactory = new DefaultBatchHandlerFactory();
    defaultBatchHandlerFactory.setJdbcConfiguration(jdbcConfiguration().getObject());
    return defaultBatchHandlerFactory;
  }

  @Bean
  public StatementInterceptor statementInterceptor() throws Exception {
    StatementInterceptor statementInterceptor = new StatementInterceptor();
    statementInterceptor.setStatementManagers(Lists.newArrayList(statementManager()));
    return statementInterceptor;
  }
}
