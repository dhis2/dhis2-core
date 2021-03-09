/*
 * Copyright (c) 2004-2021, University of Oslo
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
import java.util.List;

import javax.sql.DataSource;

import org.hisp.dhis.cache.DefaultHibernateCacheManager;
import org.hisp.dhis.datasource.DataSourceManager;
import org.hisp.dhis.datasource.DefaultDataSourceManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dbms.HibernateDbmsManager;
import org.hisp.dhis.deletedobject.DeletedObject;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.hibernate.DefaultHibernateConfigurationProvider;
import org.hisp.dhis.hibernate.HibernateConfigurationProvider;
import org.hisp.dhis.hibernate.HibernatePropertiesFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * @author Luciano Fiandesio
 */
@Configuration
@EnableTransactionManagement
public class HibernateConfig
{
    @Autowired
    private DhisConfigurationProvider dhisConfigurationProvider;

    @Bean
    public HibernateConfigurationProvider hibernateConfigurationProvider()
    {
        DefaultHibernateConfigurationProvider hibernateConfigurationProvider = new DefaultHibernateConfigurationProvider();
        hibernateConfigurationProvider.setConfigurationProvider( dhisConfigurationProvider );
        return hibernateConfigurationProvider;
    }

    private HibernatePropertiesFactoryBean hibernateProperties()
    {
        HibernatePropertiesFactoryBean hibernatePropertiesFactoryBean = new HibernatePropertiesFactoryBean();
        hibernatePropertiesFactoryBean.setHibernateConfigurationProvider( hibernateConfigurationProvider() );
        return hibernatePropertiesFactoryBean;
    }

    @Bean
    @DependsOn( "flyway" )
    public LocalSessionFactoryBean sessionFactory()
        throws Exception
    {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource( dataSource() );

        sessionFactory.setHibernateProperties( hibernateProperties().getObject() );

        List<Resource> jarResources = hibernateConfigurationProvider().getJarResources();
        sessionFactory.setMappingJarLocations( jarResources.toArray( new Resource[jarResources.size()] ) );

        List<Resource> directoryResources = hibernateConfigurationProvider().getDirectoryResources();
        sessionFactory
            .setMappingDirectoryLocations( directoryResources.toArray( new Resource[directoryResources.size()] ) );

        sessionFactory.setAnnotatedClasses( DeletedObject.class );

        sessionFactory.setHibernateProperties( hibernateProperties().getObject() );

        return sessionFactory;
    }

    @Bean
    public DataSource dataSource()
        throws PropertyVetoException
    {
        // FIXME LUCIANO destroyMethod ? destroy-method="close"
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass( (String) getConnectionProperty( "hibernate.connection.driver_class" ) );
        dataSource.setJdbcUrl( (String) getConnectionProperty( "hibernate.connection.url" ) );
        dataSource.setUser( (String) getConnectionProperty( "hibernate.connection.username" ) );
        dataSource.setPassword( (String) getConnectionProperty( "hibernate.connection.password" ) );
        dataSource.setMaxPoolSize( Integer.parseInt( (String) getConnectionProperty( "hibernate.c3p0.max_size" ) ) );
        dataSource.setMinPoolSize( Integer
            .parseInt( (String) getConnectionProperty( ConfigurationKey.CONNECTION_POOL_MIN_SIZE.getKey() ) ) );
        dataSource.setInitialPoolSize( Integer
            .parseInt( (String) getConnectionProperty( ConfigurationKey.CONNECTION_POOL_INITIAL_SIZE.getKey() ) ) );
        dataSource.setAcquireIncrement( Integer
            .parseInt( (String) getConnectionProperty( ConfigurationKey.CONNECTION_POOL_ACQUIRE_INCR.getKey() ) ) );
        dataSource.setMaxIdleTime( Integer
            .parseInt( (String) getConnectionProperty( ConfigurationKey.CONNECTION_POOL_MAX_IDLE_TIME.getKey() ) ) );
        dataSource.setTestConnectionOnCheckin( Boolean.parseBoolean(
            (String) getConnectionProperty( ConfigurationKey.CONNECTION_POOL_TEST_ON_CHECKIN.getKey() ) ) );
        dataSource.setTestConnectionOnCheckout( Boolean.parseBoolean(
            (String) getConnectionProperty( ConfigurationKey.CONNECTION_POOL_TEST_ON_CHECKOUT.getKey() ) ) );
        dataSource.setMaxIdleTimeExcessConnections( Integer.parseInt(
            (String) getConnectionProperty( ConfigurationKey.CONNECTION_POOL_MAX_IDLE_TIME_EXCESS_CON.getKey() ) ) );
        dataSource.setIdleConnectionTestPeriod( Integer.parseInt(
            (String) getConnectionProperty( ConfigurationKey.CONNECTION_POOL_IDLE_CON_TEST_PERIOD.getKey() ) ) );

        return dataSource;
    }

    @Bean
    public DataSourceManager dataSourceManager()
        throws PropertyVetoException
    {
        DefaultDataSourceManager defaultDataSourceManager = new DefaultDataSourceManager();
        defaultDataSourceManager.setConfig( dhisConfigurationProvider );
        defaultDataSourceManager.setMainDataSource( dataSource() );

        return defaultDataSourceManager;
    }

    @Bean
    public DataSource readOnlyDataSource()
        throws PropertyVetoException
    {
        // FIXME Luciano why do we need this? Can't we use @Transactional
        // readonly?

        return dataSourceManager().getReadOnlyDataSource();
    }

    @Bean
    public PlatformTransactionManager hibernateTransactionManager()
        throws Exception
    {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory( sessionFactory().getObject() );
        transactionManager.setDataSource( dataSource() );
        return transactionManager;
    }

    @Bean
    public TransactionTemplate transactionTemplate()
        throws Exception
    {
        return new TransactionTemplate( hibernateTransactionManager() );
    }

    @Bean
    public DefaultHibernateCacheManager cacheManager()
        throws Exception
    {
        DefaultHibernateCacheManager cacheManager = new DefaultHibernateCacheManager();
        cacheManager.setSessionFactory( sessionFactory().getObject() );
        return cacheManager;
    }

    @Bean
    public DbmsManager dbmsManager()
        throws Exception
    {
        HibernateDbmsManager hibernateDbmsManager = new HibernateDbmsManager();
        hibernateDbmsManager.setCacheManager( cacheManager() );
        hibernateDbmsManager.setSessionFactory( sessionFactory().getObject() );
        hibernateDbmsManager.setJdbcTemplate( jdbcTemplate() );
        return hibernateDbmsManager;
    }

    @Bean( "jdbcTemplate" )
    @Primary
    public JdbcTemplate jdbcTemplate()
        throws PropertyVetoException
    {
        JdbcTemplate jdbcTemplate = new JdbcTemplate( dataSource() );
        jdbcTemplate.setFetchSize( 1000 );
        return jdbcTemplate;
    }

    @Bean( "readOnlyJdbcTemplate" )
    public JdbcTemplate readOnlyJdbcTemplate()
        throws PropertyVetoException
    {
        JdbcTemplate jdbcTemplate = new JdbcTemplate( readOnlyDataSource() );
        jdbcTemplate.setFetchSize( 1000 );
        return jdbcTemplate;
    }

    private Object getConnectionProperty( String key )
    {
        return hibernateConfigurationProvider().getConfiguration().getProperty( key );
    }
}
