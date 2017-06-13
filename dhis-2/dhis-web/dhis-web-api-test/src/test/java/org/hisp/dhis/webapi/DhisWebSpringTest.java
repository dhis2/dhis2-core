package org.hisp.dhis.webapi;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RunWith( SpringRunner.class )
@ContextConfiguration( locations = {
    "classpath*:/META-INF/dhis/beans.xml",
    "classpath*:/META-INF/dhis/servlet.xml" }
)
@WebAppConfiguration
@Transactional
public abstract class DhisWebSpringTest
    extends DhisConvenienceTest
{
    @Autowired
    protected FilterChainProxy filterChainProxy;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected RenderService renderService;

    @Autowired
    protected UserService _userService;

    protected MockMvc mvc;

    @Autowired
    protected SchemaService schemaService;

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation( "target/generated-snippets" );

    @Before
    public void setup() throws Exception
    {
        userService = _userService;
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding( "UTF-8" );
        characterEncodingFilter.setForceEncoding( true );
        mvc = MockMvcBuilders.webAppContextSetup( webApplicationContext )
            .addFilters( characterEncodingFilter, new ShallowEtagHeaderFilter(), filterChainProxy )
            .apply( documentationConfiguration( this.restDocumentation ) )
            .build();

        executeStartupRoutines();

        setUpTest();
    }

    protected void setUpTest() throws Exception
    {
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    public MockHttpSession getSession( String... authorities )
    {
        SecurityContextHolder.getContext().setAuthentication( getPrincipal( authorities ) );
        MockHttpSession session = new MockHttpSession();

        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext() );

        return session;
    }

    protected UsernamePasswordAuthenticationToken getPrincipal( String... authorities )
    {
        User user = createAdminUser( authorities );
        List<GrantedAuthority> grantedAuthorities = user.getUserCredentials().getAllAuthorities()
            .stream().map( SimpleGrantedAuthority::new ).collect( Collectors.toList() );

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            user.getUserCredentials().getUsername(), user.getUserCredentials().getPassword(), grantedAuthorities );

        return new UsernamePasswordAuthenticationToken(
            userDetails,
            userDetails.getPassword(),
            userDetails.getAuthorities()
        );
    }

    private void executeStartupRoutines()
        throws Exception
    {
        String id = "org.hisp.dhis.system.startup.StartupRoutineExecutor";

        if ( webApplicationContext.containsBean( id ) )
        {
            Object object = webApplicationContext.getBean( id );

            Method method = object.getClass().getMethod( "executeForTesting", new Class[0] );

            method.invoke( object, new Object[0] );
        }
    }

    public RestDocumentationResultHandler documentPrettyPrint( String useCase, Snippet... snippets )
    {
        return document( useCase, preprocessRequest( prettyPrint() ), preprocessResponse( prettyPrint() ), snippets );
    }

    public SchemaService getSchemaService()
    {
        return schemaService;
    }

    public MockMvc getMvc()
    {
        return mvc;
    }

    public IdentifiableObjectManager getManager()
    {
        return manager;
    }
}
