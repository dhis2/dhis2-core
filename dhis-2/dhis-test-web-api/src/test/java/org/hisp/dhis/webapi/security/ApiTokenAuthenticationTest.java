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
package org.hisp.dhis.webapi.security;

import static org.hisp.dhis.security.apikey.ApiKeyTokenGenerator.generatePersonalAccessToken;
import static org.hisp.dhis.web.WebClient.ApiTokenHeader;
import static org.hisp.dhis.web.WebClient.Header;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.hisp.dhis.security.apikey.ApiKeyTokenGenerator;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.security.apikey.ApiTokenStore;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerWithApiTokenAuthTest;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.hisp.dhis.webapi.security.config.DhisWebApiWebSecurityConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ApiTokenAuthenticationTest extends DhisControllerWithApiTokenAuthTest
{
    public static final String URI = "/me?fields=settings,id";

    public static final String CHECKSUM_VALIDATION_FAILED = "Checksum validation failed";

    @Autowired
    private ApiTokenService apiTokenService;

    @Autowired
    private ApiTokenStore apiTokenStore;

    @BeforeAll
    static void setUpClass()
    {
        DhisWebApiWebSecurityConfig.setApiContextPath( "" );
    }

    @BeforeEach
    public void setup()
        throws Exception
    {
        super.setup();
    }

    private ApiKeyTokenGenerator.TokenWrapper createNewToken()
    {
        long thirtyDaysInTheFuture = System.currentTimeMillis() + TimeUnit.DAYS.toMillis( 30 );
        ApiKeyTokenGenerator.TokenWrapper wrapper = generatePersonalAccessToken( null, thirtyDaysInTheFuture );
        apiTokenStore.save( wrapper.getApiToken() );
        return wrapper;
    }

    @Test
    void testInvalidKeyTypeNotResolvable()
    {
        String errorMessage = GET( URI, ApiTokenHeader( "FAKE_KEY" ) ).error( HttpStatus.BAD_REQUEST ).getMessage();
        assertEquals( CHECKSUM_VALIDATION_FAILED, errorMessage );
    }

    @Test
    void testInvalidKeyBadChecksum()
    {
        String errorMessage = GET( URI, ApiTokenHeader( "d2pat_tWhOu7GsXzTZYroHAmdwtBCAmA0qD5Ze383854" ) ).error(
            HttpStatus.BAD_REQUEST ).getMessage();
        assertEquals( CHECKSUM_VALIDATION_FAILED, errorMessage );
    }

    @Test
    void testInvalidKeyBadSize()
    {
        String errorMessage = GET( URI, ApiTokenHeader( "d2pat_tXXXXXWhOu7GsXzTZYroHAmdwtBCAmA0qD5Ze383854" ) ).error(
            HttpStatus.BAD_REQUEST ).getMessage();
        assertEquals( CHECKSUM_VALIDATION_FAILED, errorMessage );
    }

    @Test
    void testValidApiTokenAuthentication()
    {
        JsonUser user = GET( URI, ApiTokenHeader( new String( createNewToken().getPlaintextToken() ) ) )
            .content( HttpStatus.OK )
            .as( JsonUser.class );
        assertEquals( adminUser.getUid(), user.getId() );
    }

    @Test
    void testAllowedIpRule()
    {
        ApiKeyTokenGenerator.TokenWrapper wrapper = createNewToken();
        final String plaintext = new String( wrapper.getPlaintextToken() );
        final ApiToken token = wrapper.getApiToken();

        token.addIpToAllowedList( "192.168.2.1" );
        apiTokenService.update( token );

        String errorMessage = GET( URI, ApiTokenHeader( plaintext ) ).error( HttpStatus.UNAUTHORIZED )
            .getMessage();
        assertEquals( "Failed to authenticate API token, request ip address is not allowed.",
            errorMessage );
        token.addIpToAllowedList( "127.0.0.1" );
        apiTokenService.update( token );

        JsonUser user = GET( URI, ApiTokenHeader( plaintext ) ).content().as( JsonUser.class );
        assertEquals( adminUser.getUid(), user.getId() );
    }

    @Test
    void testAllowedMethodRule()
    {
        ApiKeyTokenGenerator.TokenWrapper tokenWrapper = createNewToken();
        final String plaintext = new String( tokenWrapper.getPlaintextToken() );
        final ApiToken token = tokenWrapper.getApiToken();

        token.addMethodToAllowedList( "POST" );
        apiTokenService.update( token );

        assertEquals( "Failed to authenticate API token, request http method is not allowed.",
            GET( URI, ApiTokenHeader( plaintext ) ).error( HttpStatus.UNAUTHORIZED ).getMessage() );
        token.addMethodToAllowedList( "GET" );
        apiTokenService.update( token );

        JsonUser user = GET( URI, ApiTokenHeader( plaintext ) ).content().as( JsonUser.class );
        assertEquals( adminUser.getUid(), user.getId() );
    }

    @Test
    void testAllowedReferrerRule()
    {
        ApiKeyTokenGenerator.TokenWrapper wrapper = createNewToken();
        final String plaintext = new String( wrapper.getPlaintextToken() );
        final ApiToken token = wrapper.getApiToken();

        token.addReferrerToAllowedList( "https://one.io" );
        apiTokenService.update( token );

        assertEquals( "Failed to authenticate API token, request http referrer is missing or not allowed.",
            GET( URI, ApiTokenHeader( plaintext ) ).error( HttpStatus.UNAUTHORIZED ).getMessage() );
        token.addReferrerToAllowedList( "https://two.io" );
        apiTokenService.update( token );

        JsonUser user = GET( URI, ApiTokenHeader( plaintext ), Header( "referer", "https://two.io" ) ).content()
            .as( JsonUser.class );
        assertEquals( adminUser.getUid(), user.getId() );
    }

    @Test
    void testExpiredToken()
    {
        ApiKeyTokenGenerator.TokenWrapper wrapper = createNewToken();
        final String plaintext = new String( wrapper.getPlaintextToken() );
        final ApiToken token = wrapper.getApiToken();

        token.setExpire( System.currentTimeMillis() - 36000 );

        assertEquals( "Failed to authenticate API token, token has expired.",
            GET( URI, ApiTokenHeader( plaintext ) ).error( HttpStatus.UNAUTHORIZED ).getMessage() );
    }

    @Test
    void testAuthWithDisabledUser()
    {
        ApiKeyTokenGenerator.TokenWrapper wrapper = createNewToken();
        final String token = new String( wrapper.getPlaintextToken() );

        User user = adminUser;
        user.setDisabled( true );
        userService.updateUser( user );

        assertEquals( "The API token is disabled, locked or 2FA is enabled.",
            GET( URI, ApiTokenHeader( token ) ).error( HttpStatus.UNAUTHORIZED ).getMessage() );
    }
}
