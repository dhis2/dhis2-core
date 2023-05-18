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

import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClientUtils;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonApiToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ApiTokenControllerTest extends DhisControllerConvenienceTest
{

    public static final String USER_A_USERNAME = "userA";

    public static final String USER_B_USERNAME = "userB";

    @Autowired
    private ApiTokenService apiTokenService;

    @Autowired
    private RenderService _renderService;

    private User userA;

    private User userB;

    @BeforeEach
    final void setupTest()
    {
        this.renderService = _renderService;
        userA = createUserWithAuth( USER_A_USERNAME );
        userB = createUserWithAuth( USER_B_USERNAME );

        // Default user is userA
        injectSecurityContext( userA );
    }

    @Test
    void testCreate()
    {
        final JsonObject jsonObject = assertApiTokenCreatedResponse(
            POST( "/apiTokens/", "{}" ) );
        final String uid = jsonObject.getString( "uid" ).string();
        final String rawKey = jsonObject.getString( "key" ).string();
        assertNotNull( uid );
        assertNotNull( rawKey );
        assertEquals( 48, rawKey.length() );
        final ApiToken token = fetchAsEntity( uid );
        String hashedKey = token.getKey();
        assertEquals( 64, hashedKey.length() );
    }

    @Test
    void testCreateApiToken()
    {
        final String uid = createNewTokenWithAttributes();
        final ApiToken apiToken1 = fetchAsEntity( uid );
        assertEquals( 1, (int) apiToken1.getVersion() );
        assertNotNull( apiToken1.getKey() );
        assertTrue( apiToken1.getIpAllowedList().getAllowedIps().contains( "1.1.1.1" ) );
        assertTrue( apiToken1.getIpAllowedList().getAllowedIps().contains( "2.2.2.2" ) );
        assertTrue( apiToken1.getIpAllowedList().getAllowedIps().contains( "3.3.3.3" ) );
        assertTrue( apiToken1.getMethodAllowedList().getAllowedMethods().contains( "GET" ) );
        assertTrue( apiToken1.getMethodAllowedList().getAllowedMethods().contains( "POST" ) );
        assertTrue( apiToken1.getMethodAllowedList().getAllowedMethods().contains( "PATCH" ) );
        assertTrue( apiToken1.getRefererAllowedList().getAllowedReferrers().contains( "http://hostname1.com" ) );
        assertTrue( apiToken1.getRefererAllowedList().getAllowedReferrers().contains( "http://hostname2.com" ) );
        assertTrue( apiToken1.getRefererAllowedList().getAllowedReferrers().contains( "http://hostname3.com" ) );
    }

    @Test
    void testListApiTokens()
    {
        createNewTokenWithAttributes();
        createNewTokenWithAttributes();
        createNewTokenWithAttributes();
        final JsonList<JsonApiToken> apiTokens = GET( "/apiTokens/" ).content()
            .getList( "apiToken", JsonApiToken.class );
        assertEquals( 3, apiTokens.size() );
    }

    @Test
    void testListApiTokensNotYours()
    {
        createNewTokenWithAttributes();
        createNewTokenWithAttributes();
        createNewTokenWithAttributes();
        switchToNewUser( "anonymous" );
        createNewTokenWithAttributes();
        final JsonList<JsonApiToken> apiTokens = GET( "/apiTokens/" ).content()
            .getList( "apiToken", JsonApiToken.class );
        assertEquals( 1, apiTokens.size() );
    }

    @Test
    void testPatchApiTokenIntegerProperty()
    {
        final String uid = createNewTokenWithAttributes();
        final ApiToken apiToken1 = fetchAsEntity( uid );
        assertEquals( 1, (int) apiToken1.getVersion() );
        assertStatus( HttpStatus.OK, PATCH( "/apiTokens/{id}",
            uid + "?importReportMode=ERRORS", Body( "[{'op': 'replace', 'path': '/version', 'value': 333}]" ) ) );
        final ApiToken apiToken2 = fetchAsEntity( uid );
        assertEquals( 333, (int) apiToken2.getVersion() );
    }

    @Test
    void testPatchApiTokenAttributesProperty()
    {
        final String uid = createNewTokenWithAttributes();
        final ApiToken apiToken1 = fetchAsEntity( uid );
        assertEquals( 3, apiToken1.getIpAllowedList().getAllowedIps().size() );
        assertTrue( apiToken1.getIpAllowedList().getAllowedIps().contains( "1.1.1.1" ) );
        assertFalse( apiToken1.getIpAllowedList().getAllowedIps().contains( "8.8.8.8" ) );
        assertStatus( HttpStatus.OK,
            PATCH( "/apiTokens/{id}", uid + "?importReportMode=ERRORS", Body(
                "[{'op':'replace','path':'/attributes','value':[{'type':'IpAllowedList','allowedIps':['8.8.8.8']}]}]" ) ) );
        final ApiToken apiToken2 = fetchAsEntity( uid );
        assertEquals( 1, apiToken2.getIpAllowedList().getAllowedIps().size() );
        assertFalse( apiToken2.getIpAllowedList().getAllowedIps().contains( "1.1.1.1" ) );
        assertTrue( apiToken2.getIpAllowedList().getAllowedIps().contains( "8.8.8.8" ) );
    }

    @Test
    void testCantModifyKeyPatch()
    {
        final ApiToken newToken = createNewEmptyToken();
        PATCH( "/apiTokens/{id}",
            newToken.getUid() + "?importReportMode=ERRORS",
            Body( "[{'op':'replace','path':'/key','value':'MY NEW VALUE'}]" ) );
        final ApiToken afterPatched = apiTokenService.getByUid( newToken.getUid() );
        assertEquals( newToken.getKey(), afterPatched.getKey() );
    }

    @Test
    void testCantAddInvalidIp()
    {
        final HttpResponse post = POST( "/apiTokens/",
            "{'attributes':[{'type': 'IpAllowedList','allowedIps':['X.1.1.1','2.2.2.2','3.3.3.3']}]}" );
        assertEquals( "Failed to validate the token's attributes, message: Not a valid ip address, value=X.1.1.1",
            post.error().getMessage() );
    }

    @Test
    void testCantAddInvalidMethod()
    {
        final HttpResponse post = POST( "/apiTokens/",
            "{'attributes':[" + "{'type':'MethodAllowedList','allowedMethods':['POST','X','PATCH']}" + "]}" );
        assertEquals( "Failed to validate the token's attributes, message: Not a valid http method, value=X",
            post.error().getMessage() );
    }

    @Test
    void testCantAddInvalidReferrer()
    {
        final HttpResponse post = POST( "/apiTokens/", "{'attributes':["
            + "{'type':'RefererAllowedList','allowedReferrers':['http:XXX//hostname3.com','http://hostname2.com','http://hostname1.com']}]}" );
        assertEquals(
            "Failed to validate the token's attributes, message: Not a valid referrer url, value=http:XXX//hostname3.com",
            post.error().getMessage() );
    }

    @Test
    void testDelete()
    {
        final ApiToken newToken = createNewEmptyToken();
        assertStatus( HttpStatus.OK, DELETE( "/apiTokens/" + newToken.getUid() ) );
        assertStatus( HttpStatus.NOT_FOUND,
            GET( "/apiTokens" + "/{uid}", newToken.getUid() ) );
    }

    @Test
    void testCantDeleteOtherTokens()
    {
        final ApiToken newToken = createNewEmptyToken();
        switchContextToUser( userB );
        assertStatus( HttpStatus.NOT_FOUND,
            DELETE( "/apiTokens" + "/" + newToken.getUid() ) );
    }

    @Test
    void testCreateApiTokenExpireInFuture()
    {
        final long ONE_HOUR_FROM_NOW = System.currentTimeMillis() + 3600000;
        assertStatus( HttpStatus.CREATED,
            POST( "/apiTokens/", "{'expire': " + ONE_HOUR_FROM_NOW + "}" ) );
    }

    @Test
    void testCreateAndFetchWithAnotherUser()
    {
        final ApiToken newToken = createNewEmptyToken();
        switchToNewUser( "anonymous" );
        assertStatus( HttpStatus.NOT_FOUND,
            GET( "/apiTokens/{uid}", newToken.getUid() ) );
        switchToSuperuser();
        fetchAsEntity( newToken.getUid() );
    }

    private ApiToken createNewEmptyToken()
    {
        final HttpResponse post = POST( "/apiTokens/", "{}" );
        final String uid = assertStatus( HttpStatus.CREATED, post );
        return apiTokenService.getByUid( uid );
    }

    private String createNewTokenWithAttributes()
    {
        final HttpResponse post = POST( "/apiTokens/",
            "{'attributes':[{'type': 'IpAllowedList','allowedIps':['1.1.1.1','2.2.2.2','3.3.3.3']},"
                + "{'type':'MethodAllowedList','allowedMethods':['POST','GET','PATCH']},"
                + "{'type':'RefererAllowedList','allowedReferrers':['http://hostname3.com','http://hostname2.com','http://hostname1.com']}]}" );
        return assertStatus( HttpStatus.CREATED, post );
    }

    private ApiToken fetchAsEntity( String uid )
    {
        return apiTokenService.getByUid( uid );
    }

    public static JsonObject assertApiTokenCreatedResponse( HttpResponse actual )
    {
        HttpStatus actualStatus = actual.status();
        if ( HttpStatus.CREATED != actualStatus )
        {
            assertEquals( HttpStatus.CREATED, actualStatus, "Actual response is not CREATED" );
        }
        WebClientUtils.assertValidLocation( actual );
        JsonObject report = actual.contentUnchecked().getObject( "response" );
        if ( report.exists() )
        {
            return report;
        }
        throw new IllegalStateException( "Response is not a proper ApiTokenCreatedResponse" );
    }
}
