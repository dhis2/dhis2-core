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

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.datasource.DatabasePoolUtils;
import org.hisp.dhis.datasource.DefaultReadOnlyDataSourceManager;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.hibernate.HibernateConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.base.MoreObjects;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Configuration
public class DataSourceConfig
{
    @Autowired
    private DhisConfigurationProvider dhisConfig;

    @Bean
    @DependsOn( "dataSource" )
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate( @Qualifier( "dataSource" ) DataSource dataSource )
    {
        return new NamedParameterJdbcTemplate( dataSource );
    }

    @Bean( "jdbcTemplate" )
    @DependsOn( "dataSource" )
    @Primary
    public JdbcTemplate jdbcTemplate( @Qualifier( "dataSource" ) DataSource dataSource )
    {
        JdbcTemplate jdbcTemplate = new JdbcTemplate( dataSource );
        jdbcTemplate.setFetchSize( 1000 );
        return jdbcTemplate;
    }

    @Bean( "executionPlanJdbcTemplate" )
    @DependsOn( "dataSource" )
    public JdbcTemplate executionPlanJdbcTemplate( @Qualifier( "dataSource" ) DataSource dataSource )
    {
        JdbcTemplate jdbcTemplate = new JdbcTemplate( dataSource );
        jdbcTemplate.setFetchSize( 1000 );
        jdbcTemplate.setQueryTimeout( 10 );
        return jdbcTemplate;
    }

    @Bean( "readOnlyJdbcTemplate" )
    @DependsOn( "dataSource" )
    public JdbcTemplate readOnlyJdbcTemplate( @Qualifier( "dataSource" ) DataSource dataSource )
    {
        DefaultReadOnlyDataSourceManager manager = new DefaultReadOnlyDataSourceManager( dhisConfig );

        JdbcTemplate jdbcTemplate = new JdbcTemplate(
            MoreObjects.firstNonNull( manager.getReadOnlyDataSource(), dataSource ) );
        jdbcTemplate.setFetchSize( 1000 );

        return jdbcTemplate;
    }

    @Bean( "actualDataSource" )
    public DataSource actualDataSource( HibernateConfigurationProvider hibernateConfigurationProvider )
    {
        String jdbcUrl = dhisConfig.getProperty( ConfigurationKey.CONNECTION_URL );
        String username = dhisConfig.getProperty( ConfigurationKey.CONNECTION_USERNAME );
        String dbPoolType = dhisConfig.getProperty( ConfigurationKey.DB_POOL_TYPE );

        DatabasePoolUtils.PoolConfig poolConfig = DatabasePoolUtils.PoolConfig.builder()
            .dhisConfig( dhisConfig )
            .hibernateConfig( hibernateConfigurationProvider )
            .dbPoolType( dbPoolType )
            .build();

        try
        {
            return DatabasePoolUtils.createDbPool( poolConfig );
        }
        catch ( SQLException | PropertyVetoException e )
        {
            String message = String.format( "Connection test failed for main database pool, " +
                "jdbcUrl: '%s', user: '%s'", jdbcUrl, username );

            log.error( message );
            log.error( DebugUtils.getStackTrace( e ) );

            throw new IllegalStateException( message, e );
        }
    }

    @Bean( "dataSource" )
    @DependsOn( "actualDataSource" )
    @Primary
    public DataSource dataSource( @Qualifier( "actualDataSource" ) DataSource actualDataSource )
    {
        boolean enableQueryLogging = dhisConfig.isEnabled(
            ConfigurationKey.ENABLE_QUERY_LOGGING );

        if ( !enableQueryLogging )
        {
            return actualDataSource;
        }

        PrettyQueryEntryCreator creator = new PrettyQueryEntryCreator();
        creator.setMultiline( true );

        SLF4JQueryLoggingListener listener = new SLF4JQueryLoggingListener();
        listener.setLogger( "org.hisp.dhis.datasource.query" );
        listener.setLogLevel( SLF4JLogLevel.INFO );
        listener.setQueryLogEntryCreator( creator );

        ProxyDataSourceBuilder builder = ProxyDataSourceBuilder
            .create( actualDataSource )
            .name( "ProxyDS_DHIS2_" + dhisConfig.getProperty(
                ConfigurationKey.DB_POOL_TYPE ) +
                "_" + CodeGenerator.generateCode( 5 ) )
            .logSlowQueryBySlf4j(
                Integer.parseInt( dhisConfig.getProperty(
                    ConfigurationKey.SLOW_QUERY_LOGGING_THRESHOLD_TIME_MS ) ),
                TimeUnit.MILLISECONDS, SLF4JLogLevel.WARN )
            .listener( listener )
            .proxyResultSet();

        boolean elapsedTimeLogging = dhisConfig.isEnabled( ConfigurationKey.ELAPSED_TIME_QUERY_LOGGING_ENABLED );
        boolean methodLoggingEnabled = dhisConfig.isEnabled( ConfigurationKey.METHOD_QUERY_LOGGING_ENABLED );

        if ( methodLoggingEnabled )
        {
            builder.afterMethod( DataSourceConfig::executeAfterMethod );
        }

        if ( elapsedTimeLogging )
        {
            builder.afterQuery(
                ( execInfo, queryInfoList ) -> log.info( "Query took " + execInfo.getElapsedTime() + "msec" ) );
        }

        return builder.build();
    }

    private static void executeAfterMethod( MethodExecutionContext executionContext )
    {
        Thread thread = Thread.currentThread();
        StackTraceElement[] stackTrace = thread.getStackTrace();

        for ( int i = 0; i < stackTrace.length; i++ )
        {
            StackTraceElement stackTraceElement = stackTrace[i];
            String methodName = stackTraceElement.getMethodName();
            String className = stackTraceElement.getClassName();
            int pos = className.lastIndexOf( '.' );
            String packageName = className.substring( 0, pos );

            if ( className.contains( "org.hisp.dhis.cacheinvalidation.KnownTransactionsService" ) || methodName.equals(
                "getSingleResult" ) || methodName.equals( "doFilterInternal" ) )
            {
                break;
            }

            if ( packageName.startsWith( "org.hisp.dhis" ) && !methodName.equals( "executeAfterMethod" ) )
            {
                StackTraceElement nextElement = stackTrace[i - 1];
                String methodName1 = nextElement.getMethodName();
                String className1 = nextElement.getClassName();

                log.info( "JDBC: " + className + "#" + methodName + " ---- \n ----" + className1 + "#" + methodName1 );
                break;
            }

        }

    }

    private static class PrettyQueryEntryCreator
        extends DefaultQueryLogEntryCreator
    {
        private final Formatter formatter = FormatStyle.HIGHLIGHT.getFormatter();

        @Override
        protected String formatQuery( String query )
        {
            try
            {
                Objects.requireNonNull( query );
                return this.formatter.format( query ) + "\n";
            }
            catch ( Exception e )
            {
                log.error( "Query formatter failed!", e );
            }

            return "Formatter error!";
        }
    }
}