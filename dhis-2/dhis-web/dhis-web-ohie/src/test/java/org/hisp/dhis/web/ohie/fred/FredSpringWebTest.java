package org.hisp.dhis.web.ohie.fred;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.web.ohie.fred.webapi.v1.utils.ObjectMapperFactoryBean;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath*:/META-INF/dhis/beans.xml",
    "classpath*:/META-INF/dhis/webapi-ohie.xml" }
)
@WebAppConfiguration
@Transactional
public abstract class FredSpringWebTest
{
    @Autowired
    protected FilterChainProxy filterChainProxy;

    @Autowired
    protected WebApplicationContext wac;

    protected MockMvc mvc;

    protected ObjectMapper objectMapper;

    public MockHttpSession getSession( String... authorities )
    {
        SecurityContextHolder.getContext().setAuthentication( getPrincipal( authorities ) );
        MockHttpSession session = new MockHttpSession();

        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext() );

        return session;
    }

    public UsernamePasswordAuthenticationToken getPrincipal( String... authorities )
    {
        List<SimpleGrantedAuthority> grantedAuthorities = new ArrayList<>();

        for ( String authority : authorities )
        {
            grantedAuthorities.add( new SimpleGrantedAuthority( authority ) );
        }

        UserDetails userDetails = new User( "admin", "district", true, true, true, true, grantedAuthorities );

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            userDetails,
            userDetails.getPassword(),
            userDetails.getAuthorities()
        );

        return authenticationToken;
    }

    @Before
    public void setup() throws Exception
    {
        objectMapper = new ObjectMapperFactoryBean().getObject();

        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding( "UTF-8" );
        characterEncodingFilter.setForceEncoding( true );

        mvc = MockMvcBuilders.webAppContextSetup( wac )
            .addFilters( characterEncodingFilter, new ShallowEtagHeaderFilter(), filterChainProxy )
            .build();

        executeStartupRoutines();

        setUpTest();
    }

    protected void setUpTest() throws Exception
    {
    }

    // -------------------------------------------------------------------------
    // Utility methods
    // -------------------------------------------------------------------------

    protected Object getBean( String beanId )
    {
        return wac.getBean( beanId );
    }

    protected OrganisationUnit createOrganisationUnit( char identifier )
    {
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setAutoFields();
        organisationUnit.setName( "OrgUnit" + identifier );
        organisationUnit.setShortName( organisationUnit.getName() );
        organisationUnit.setCreated( new Date() );
        organisationUnit.setLastUpdated( organisationUnit.getCreated() );

        return organisationUnit;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void executeStartupRoutines()
        throws Exception
    {
        String id = "org.hisp.dhis.system.startup.StartupRoutineExecutor";

        if ( wac.containsBean( id ) )
        {
            Object object = wac.getBean( id );

            Method method = object.getClass().getMethod( "executeForTesting", new Class[0] );

            method.invoke( object, new Object[0] );
        }
    }
}
