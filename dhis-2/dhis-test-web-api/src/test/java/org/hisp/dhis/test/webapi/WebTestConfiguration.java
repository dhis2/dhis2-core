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
package org.hisp.dhis.test.webapi;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.hisp.dhis.datasource.DatabasePoolUtils;
import org.hisp.dhis.datasource.model.PoolConfig;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.SystemAuthoritiesProvider;
import org.hisp.dhis.test.h2.H2SqlFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com
 */
@Configuration
@Order(10)
public class WebTestConfiguration {
  @Bean
  public RequestCache requestCache() {
    return new HttpSessionRequestCache();
  }

  @Autowired private DhisConfigurationProvider dhisConfigurationProvider;

  @Bean(name = {"namedParameterJdbcTemplate", "analyticsNamedParameterJdbcTemplate"})
  @Primary
  public NamedParameterJdbcTemplate namedParameterJdbcTemplate(
      @Qualifier("dataSource") DataSource dataSource) {
    return new NamedParameterJdbcTemplate(dataSource);
  }

  @Bean(name = {"dataSource", "analyticsDataSource"})
  @Primary
  public DataSource actualDataSource() throws PropertyVetoException, SQLException {
    PoolConfig.PoolConfigBuilder builder = PoolConfig.builder();
    builder.dhisConfig(dhisConfigurationProvider);
    builder.dbPoolType(dhisConfigurationProvider.getProperty(ConfigurationKey.DB_POOL_TYPE));

    final DataSource dbPool = DatabasePoolUtils.createDbPool(builder.build());
    H2SqlFunction.registerH2Functions(dbPool);
    return dbPool;
  }

  @Bean
  public DefaultAuthenticationEventPublisher authenticationEventPublisher() {
    DefaultAuthenticationEventPublisher defaultAuthenticationEventPublisher =
        new DefaultAuthenticationEventPublisher();
    defaultAuthenticationEventPublisher.setAdditionalExceptionMappings(
        Map.of(
            OAuth2AuthenticationException.class, AuthenticationFailureBadCredentialsEvent.class));
    return defaultAuthenticationEventPublisher;
  }

  @Bean
  public SystemAuthoritiesProvider systemAuthoritiesProvider() {
    return Authorities::getAllAuthorities;
  }
}
