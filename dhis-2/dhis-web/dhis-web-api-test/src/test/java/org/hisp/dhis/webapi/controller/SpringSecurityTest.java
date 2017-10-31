package org.hisp.dhis.webapi.controller;

/*
 *
 *  Copyright (c) 2004-2017, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import com.google.common.io.ByteStreams;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.security.oauth2.OAuth2ClientService;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.documentation.common.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;
import java.io.InputStream;
import java.lang.reflect.Method;

import static org.junit.Assert.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@RunWith( SpringRunner.class )
@ContextConfiguration( locations = {
    "classpath*:/META-INF/dhis/beans.xml",
    "classpath*:/META-INF/dhis/servlet.xml" }
)
@WebAppConfiguration
@Transactional
public class SpringSecurityTest extends DhisConvenienceTest
{
    @Autowired
    protected UserService _userService;

    @Resource
    protected WebApplicationContext webApplicationContext;

    @Autowired
    private OAuth2ClientService oAuth2ClientService;

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private AuthorizationServerTokenServices tokenServices;

    @Autowired
    private UserDetailsService userDetailsService;

    private OAuthHelper oAuthHelper;

    private MockMvc mvc;

    @Before
    public void setup() throws Exception
    {
        mvc = MockMvcBuilders.webAppContextSetup( webApplicationContext )
            .apply( springSecurity() )
            .build();

        executeStartupRoutines();
        userService = _userService;
        oAuthHelper = new OAuthHelper( tokenServices );
        setUpTest();
    }

    @Test
    public void testBasicRequestOk() throws Exception
    {
        MvcResult result = mvc.perform( get( "/dataElements.json" )
            .with( httpBasic( "admin", "district" ) ) )
            .andExpect( authenticated() )
            .andExpect( status().isOk() ).andReturn();
    }

    @Test
    public void testFailBasicRequest() throws Exception
    {
        MvcResult result = mvc.perform( get( "/dataElements.json" )
            .with( httpBasic( "abcd", "password" ) ) )
            .andExpect( unauthenticated() )
            .andExpect( status().is( 401 ) )
            .andReturn();
    }

    @Test
    public void testFormLoginOk() throws Exception
    {
        mvc.perform( formLogin()
            .loginProcessingUrl( "/dhis-web-commons-security/login.action" )
            .userParameter( "j_username" )
            .passwordParam( "j_password" )
            .user( "admin" )
            .password( "district" ) )
            .andExpect( authenticated() );
    }

    @Test
    public void testFailFormLogin() throws Exception
    {
        mvc.perform( formLogin()
            .loginProcessingUrl( "/dhis-web-commons-security/login.action" )
            .userParameter( "j_username" )
            .passwordParam( "j_password" )
            .user( "admin1" )
            .password( "password" ) )
            .andExpect( unauthenticated() );
    }

    @Test
    public void testOAut2hOk() throws Exception
    {
        InputStream input = new ClassPathResource( "security/OAuth2Client.json" ).getInputStream();

        String clientId = "testOauth2";
        String userName = "admin";
        String password = "district";

        mvc.perform( post( "/oAuth2Clients" )
            .with( httpBasic( userName, password ) )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( ByteStreams.toByteArray( input ) ) )
            .andReturn();

        assertNotNull( oAuth2ClientService.getOAuth2ClientByClientId( clientId ) );

        ClientDetails client = clientDetailsService.loadClientByClientId( clientId );

        assertNotNull( client );

        UserDetails userPrincipal = userDetailsService.loadUserByUsername( userName );

        assertNotNull( userPrincipal );

        RequestPostProcessor bearerToken = oAuthHelper.bearerToken( client, userPrincipal, null );
        mvc.perform( get( "/dataElements.json" )
            .with( bearerToken ) )
            .andExpect( status().isOk() );
    }

    @Test
    public void testFailOAut2h() throws Exception
    {
        InputStream input = new ClassPathResource( "security/OAuth2Client.json" ).getInputStream();

        String clientId = "testOauth2";
        String userName = "admin";
        String password = "district";

        mvc.perform( post( "/oAuth2Clients" )
            .with( httpBasic( userName, password ) )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( ByteStreams.toByteArray( input ) ) )
            .andReturn();

        ClientDetails client = clientDetailsService.loadClientByClientId( clientId );
        assertNotNull( client );

        UserDetails userPrincipal = userDetailsService.loadUserByUsername( userName );
        assertNotNull( userPrincipal );

        String invalidToken = "invalidToken";

        RequestPostProcessor bearerToken = oAuthHelper.bearerToken( client, userPrincipal, invalidToken );
        mvc.perform( get( "/dataElements.json" )
            .with( bearerToken ) )
            .andExpect( status().isUnauthorized() );
    }

    private void setUpTest()
    {
        createAdminUser( "ALL" );
        createTestUser( "user", "password" );
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
}