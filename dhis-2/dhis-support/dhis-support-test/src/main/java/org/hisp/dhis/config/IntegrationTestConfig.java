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
package org.hisp.dhis.config;

import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.container.DhisPostgisContainerProvider;
import org.hisp.dhis.container.DhisPostgreSQLContainer;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@Slf4j
@Configuration
@ComponentScan( "org.hisp.dhis" )
public class IntegrationTestConfig
{
    private static final String POSTGRES_DATABASE_NAME = "dhis";

    private static final String POSTGRES_CREDENTIALS = "dhis";

    public static final String CREATE_UPDATE_DELETE = "CREATE;UPDATE;DELETE";

    @Bean
    public LdapAuthenticator ldapAuthenticator()
    {
        return authentication -> null;
    }

    @Bean
    public LdapAuthoritiesPopulator ldapAuthoritiesPopulator()
    {
        return ( dirContextOperations, s ) -> null;
    }

    @Bean
    public PasswordEncoder encoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Bean( name = "dhisConfigurationProvider" )
    public DhisConfigurationProvider dhisConfigurationProvider()
    {
        PostgresDhisConfigurationProvider dhisConfigurationProvider = new PostgresDhisConfigurationProvider();
        JdbcDatabaseContainer<?> postgreSQLContainer = initContainer();

        final String username = postgreSQLContainer.getUsername();
        final String password = postgreSQLContainer.getPassword();

        Properties properties = new Properties();

        String jdbcUrl = postgreSQLContainer.getJdbcUrl();
        properties.setProperty( "connection.url", jdbcUrl );
        properties.setProperty( "connection.dialect", "org.hisp.dhis.hibernate.dialect.DhisPostgresDialect" );
        properties.setProperty( "connection.driver_class", "org.postgresql.Driver" );
        properties.setProperty( "connection.username", username );
        properties.setProperty( "connection.password", password );
        properties.setProperty( ConfigurationKey.AUDIT_USE_IN_MEMORY_QUEUE_ENABLED.getKey(), "off" );
        properties.setProperty( "metadata.audit.persist", "on" );
        properties.setProperty( "tracker.audit.persist", "on" );
        properties.setProperty( "aggregate.audit.persist", "on" );
        properties.setProperty( "audit.metadata", CREATE_UPDATE_DELETE );
        properties.setProperty( "audit.tracker", CREATE_UPDATE_DELETE );
        properties.setProperty( "audit.aggregate", CREATE_UPDATE_DELETE );

        dhisConfigurationProvider.addProperties( properties );

        return dhisConfigurationProvider;
    }

    private JdbcDatabaseContainer<?> initContainer()
    {
        // NOSONAR
        DhisPostgreSQLContainer<?> postgisContainer = ((DhisPostgreSQLContainer<?>) new DhisPostgisContainerProvider()
            .newInstance()) // NOSONAR
                .appendCustomPostgresConfig( "max_locks_per_transaction=100" )
                .withDatabaseName( POSTGRES_DATABASE_NAME )
                .withUsername( POSTGRES_CREDENTIALS )
                .withPassword( POSTGRES_CREDENTIALS )
                .withReuse( true );

        postgisContainer.start();

        log.info( String.format( "PostGIS container initialized: %s", postgisContainer.getJdbcUrl() ) );

        return postgisContainer;
    }
}
