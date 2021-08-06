/*
 * Copyright (c) 2004-2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.security;

import static org.hisp.dhis.webapi.WebClient.ApiTokenHeader;
import static org.hisp.dhis.webapi.WebClient.Header;
import static org.junit.Assert.assertEquals;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.DhisControllerWithApiTokenAuthTest;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.hisp.dhis.webapi.security.config.DhisWebApiWebSecurityConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class ApiTokenAuthenticationTest extends DhisControllerWithApiTokenAuthTest
{

    public static final String URI = "/me?fields=settings,id";

    @Autowired
    private ApiTokenService apiTokenService;

    private User adminUser;

    @BeforeClass
    public static void setUpClass()
    {
        DhisWebApiWebSecurityConfig.setApiContextPath( "" );
    }

    @Before
    public final void setup()
        throws Exception
    {
        super.setup();
        adminUser = preCreateInjectAdminUser();
    }

    @Test
    public void testApiTokenAuthentication()
    {
        String tokenKey = apiTokenService.createAndSaveToken().getKey();

        JsonUser user = GET( URI, ApiTokenHeader( tokenKey ) ).content().as( JsonUser.class );
        assertEquals( adminUser.getUid(), user.getId() );

        assertEquals( "The API token does not exists.",
            GET( URI, ApiTokenHeader( "FAKE_KEY" ) )
                .error( HttpStatus.UNAUTHORIZED ).getMessage() );
    }

    @Test
    public void testInvalidApiTokenAuthentication()
    {
        String tokenKey = apiTokenService.createAndSaveToken().getKey();

        JsonUser user = GET( URI, ApiTokenHeader( tokenKey ) ).content().as( JsonUser.class );
        assertEquals( adminUser.getUid(), user.getId() );
    }

    @Test
    public void testAllowedIpRule()
    {
        final ApiToken apiToken = apiTokenService.createAndSaveToken();

        apiToken.addIpToAllowedList( "192.168.2.1" );
        apiTokenService.update( apiToken );

        assertEquals( "Failed to authenticate API token, request ip address is not allowed.",
            GET( URI, ApiTokenHeader( apiToken.getKey() ) )
                .error( HttpStatus.UNAUTHORIZED ).getMessage() );

        apiToken.addIpToAllowedList( "127.0.0.1" );
        apiTokenService.update( apiToken );

        JsonUser user = GET( URI, ApiTokenHeader( apiToken.getKey() ) ).content().as( JsonUser.class );
        assertEquals( adminUser.getUid(), user.getId() );
    }

    @Test
    public void testAllowedMethodRule()
    {
        final ApiToken apiToken = apiTokenService.createAndSaveToken();

        apiToken.addMethodToAllowedList( "POST" );
        apiTokenService.update( apiToken );

        assertEquals( "Failed to authenticate API token, request http method is not allowed.",
            GET( URI, ApiTokenHeader( apiToken.getKey() ) )
                .error( HttpStatus.UNAUTHORIZED ).getMessage() );

        apiToken.addMethodToAllowedList( "GET" );
        apiTokenService.update( apiToken );

        JsonUser user = GET( URI, ApiTokenHeader( apiToken.getKey() ) ).content().as( JsonUser.class );
        assertEquals( adminUser.getUid(), user.getId() );
    }

    @Test
    public void testAllowedReferrerRule()
    {
        final ApiToken apiToken = apiTokenService.createAndSaveToken();

        apiToken.addReferrerToAllowedList( "https://one.io" );
        apiTokenService.update( apiToken );

        assertEquals( "Failed to authenticate API token, request http referrer is missing or not allowed.",
            GET( URI, ApiTokenHeader( apiToken.getKey() ) )
                .error( HttpStatus.UNAUTHORIZED ).getMessage() );

        apiToken.addReferrerToAllowedList( "https://two.io" );
        apiTokenService.update( apiToken );

        JsonUser user = GET( URI, ApiTokenHeader( apiToken.getKey() ), Header( "referrer", "https://two.io" ) )
            .content().as( JsonUser.class );
        assertEquals( adminUser.getUid(), user.getId() );
    }

    @Test
    public void testExpiredToken()
    {
        final ApiToken apiToken = apiTokenService.createAndSaveToken();

        apiToken.setExpire( System.currentTimeMillis() - 36000 );

        assertEquals( "Failed to authenticate API token, token has expired.",
            GET( URI, ApiTokenHeader( apiToken.getKey() ) )
                .error( HttpStatus.UNAUTHORIZED ).getMessage() );
    }
}
