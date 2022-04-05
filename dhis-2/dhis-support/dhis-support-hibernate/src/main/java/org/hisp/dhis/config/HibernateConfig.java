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

import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.DefaultHibernateCacheManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dbms.HibernateDbmsManager;
import org.hisp.dhis.deletedobject.DeletedObject;
import org.hisp.dhis.hibernate.HibernateConfigurationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Luciano Fiandesio
 * @author Morten Svanæs
 */
@Configuration
@EnableTransactionManagement
public class HibernateConfig
{
    // @Bean( "hibernateConfigurationProvider" )
    // public HibernateConfigurationProvider hibernateConfigurationProvider(
    // @Lazy DhisConfigurationProvider dhisConfig )
    // {
    // DefaultHibernateConfigurationProvider hibernateConfigurationProvider =
    // new DefaultHibernateConfigurationProvider();
    // hibernateConfigurationProvider.setConfigProvider( dhisConfig );
    // return hibernateConfigurationProvider;
    // }

    @Bean
    @DependsOn( "flyway" )
    public LocalSessionFactoryBean sessionFactory( DataSource dataSource,
        @Lazy HibernateConfigurationProvider hibernateConfigurationProvider )
    {
        Objects.requireNonNull( dataSource );
        Objects.requireNonNull( hibernateConfigurationProvider );

        Properties hibernateProperties = hibernateConfigurationProvider.getConfiguration().getProperties();
        Objects.requireNonNull( hibernateProperties );

        List<Resource> jarResources = hibernateConfigurationProvider.getJarResources();
        List<Resource> directoryResources = hibernateConfigurationProvider.getDirectoryResources();

        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource( dataSource );
        sessionFactory.setMappingJarLocations( jarResources.toArray( new Resource[0] ) );
        sessionFactory.setMappingDirectoryLocations( directoryResources.toArray( new Resource[0] ) );
        sessionFactory.setAnnotatedClasses( DeletedObject.class );
        sessionFactory.setHibernateProperties( hibernateProperties );

        return sessionFactory;
    }

    @Bean
    @DependsOn( "dataSource" )
    public HibernateTransactionManager hibernateTransactionManager( DataSource dataSource,
        SessionFactory sessionFactory )
    {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory( sessionFactory );
        transactionManager.setDataSource( dataSource );

        return transactionManager;
    }

    @Bean
    public TransactionTemplate transactionTemplate( HibernateTransactionManager transactionManager )
    {
        return new TransactionTemplate( transactionManager );
    }

    @Bean
    public DefaultHibernateCacheManager cacheManager( SessionFactory sessionFactory )
    {
        DefaultHibernateCacheManager cacheManager = new DefaultHibernateCacheManager();
        cacheManager.setSessionFactory( sessionFactory );
        return cacheManager;
    }

    @Bean
    public DbmsManager dbmsManager( JdbcTemplate jdbcTemplate, SessionFactory sessionFactory,
        DefaultHibernateCacheManager cacheManager )
    {
        HibernateDbmsManager hibernateDbmsManager = new HibernateDbmsManager();
        hibernateDbmsManager.setCacheManager( cacheManager );
        hibernateDbmsManager.setSessionFactory( sessionFactory );
        hibernateDbmsManager.setJdbcTemplate( jdbcTemplate );
        return hibernateDbmsManager;
    }
}
