package org.hisp.dhis;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.google.common.collect.ImmutableList;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.user.User;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionSynchronizationManager;


import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = { IntegrationTestConfig.class } )
@Category( IntegrationTest.class )
@ActiveProfiles( profiles = {"test-postgres"} )
public abstract class IntegrationTestBase
        extends DhisConvenienceTest
        implements ApplicationContextAware
{
    @Autowired
    protected DbmsManager dbmsManager;

    private static JdbcTemplate jdbcTemplate;

    /**
     * Setter to allow setting JdbcTemplate as static field.
     */
    @Autowired
    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        IntegrationTestBase.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Flag that determines if the IntegrationTestData annotation has
     * been running the database init script. We only want to run
     * the init script once per unit test.
     */
    public static boolean dataInit = false;

    protected ApplicationContext webApplicationContext;

    @Before
    public void before() throws Exception
    {
        bindSession();
        executeStartupRoutines();

        IntegrationTestData annotation = this.getClass().getAnnotation( IntegrationTestData.class );

        if ( annotation != null && !dataInit )
        {
            ScriptUtils.executeSqlScript( jdbcTemplate.getDataSource().getConnection(),
                new EncodedResource( new ClassPathResource( annotation.path() ), StandardCharsets.UTF_8 ) );
            // only executes once per Unit Test
            dataInit = true;
        }

        // method that can be overridden by subclasses
        setUpTest();
    }

    @AfterClass
    public static void afterClass() {

        if ( dataInit ) // only truncate tables if IntegrationTestData is used
        {
            // truncate all tables
            String truncateAll = "DO $$ DECLARE\n" +
                    "  r RECORD;\n" +
                    "BEGIN\n" +
                    "  FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = current_schema()) LOOP\n" +
                    "    EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' CASCADE';\n" +
                    "  END LOOP;\n" +
                    "END $$;";

            jdbcTemplate.execute( truncateAll );
        }
        // reset data init state
        dataInit = false;
    }

    @After
    public void after() throws Exception
    {
        tearDownTest();
        unbindSession();

        if ( emptyDatabaseAfterTest() )
        {
            dbmsManager.emptyDatabase();
        }
    }

    private void executeStartupRoutines() throws Exception
    {
        String id = "org.hisp.dhis.system.startup.StartupRoutineExecutor";

        if ( webApplicationContext.containsBean( id ) )
        {
            Object object = webApplicationContext.getBean( id );
            Method method = object.getClass().getMethod( "executeForTesting", new Class[0] );
            method.invoke( object, new Object[0] );
        }
    }

    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException
    {
        this.webApplicationContext = applicationContext;
    }

    private void bindSession()
    {
        SessionFactory sessionFactory = (SessionFactory) webApplicationContext.getBean( "sessionFactory" );
        Session session = sessionFactory.openSession();
        session.setHibernateFlushMode(FlushMode.ALWAYS);
        TransactionSynchronizationManager.bindResource( sessionFactory, new SessionHolder( session ) );
    }

    protected void preCreateInjectAdminUserWithoutPersistence()
    {
        List<GrantedAuthority> grantedAuthorities = ImmutableList.of( new SimpleGrantedAuthority( "ALL" ) );

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            "admin", "district", grantedAuthorities );

        Authentication authentication = new UsernamePasswordAuthenticationToken( userDetails, "", grantedAuthorities );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication( authentication );
        SecurityContextHolder.setContext( context );
    }

    @SuppressWarnings( "all" )
    protected User preCreateInjectAdminUser()
    {
        List<GrantedAuthority> grantedAuthorities = ImmutableList.of( new SimpleGrantedAuthority( "ALL" ) );

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            "admin", "district", grantedAuthorities );

        Authentication authentication = new UsernamePasswordAuthenticationToken( userDetails, "", grantedAuthorities );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication( authentication );
        SecurityContextHolder.setContext( context );

        return createAndInjectAdminUser( "ALL" );
    }

    @Override
    protected User createAndInjectAdminUser( String... authorities )
    {
        User user = createAdminUser( authorities );

        List<GrantedAuthority> grantedAuthorities = user.getUserCredentials().getAllAuthorities()
            .stream().map( SimpleGrantedAuthority::new ).collect( Collectors.toList() );

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            user.getUserCredentials().getUsername(), user.getUserCredentials().getPassword(), grantedAuthorities );

        Authentication authentication = new UsernamePasswordAuthenticationToken( userDetails, "", grantedAuthorities );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication( authentication );
        SecurityContextHolder.setContext( securityContext );

        return user;
    }

    private void unbindSession()
    {
        SessionFactory sessionFactory = (SessionFactory) webApplicationContext.getBean( "sessionFactory" );

        SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager
            .unbindResource( sessionFactory );

        SessionFactoryUtils.closeSession( sessionHolder.getSession() );
    }

    /**
     * Method to override.
     */
    protected boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    /**
     * Method to override.
     */
    protected void setUpTest()
        throws Exception
    {
    }

    /**
     * Method to override.
     */
    protected void tearDownTest()
        throws Exception
    {
    }
}
