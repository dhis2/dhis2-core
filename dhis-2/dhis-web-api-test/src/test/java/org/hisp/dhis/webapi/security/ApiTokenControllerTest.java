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

import static org.hisp.dhis.webapi.WebClient.Body;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.descriptors.ApiTokenSchemaDescriptor;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonApiToken;
import org.hisp.dhis.webapi.utils.WebClientUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Transactional
class ApiTokenControllerTest extends DhisControllerConvenienceTest
{

    public static final String USER_A_USERNAME = "userA";

    @Autowired
    private ApiTokenService apiTokenService;

    @Autowired
    private RenderService _renderService;

    private User userA;

    @BeforeEach
    final void setupTest()
    {
        this.renderService = _renderService;
        userA = createUser( USER_A_USERNAME );
    }

    @Test
    void testCreate()
    {
        final JsonObject jsonObject = assertApiTokenCreatedResponse(
            POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/", "{}" ) );
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
        final JsonList<JsonApiToken> apiTokens = GET( ApiTokenSchemaDescriptor.API_ENDPOINT + "/" ).content()
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
        final JsonList<JsonApiToken> apiTokens = GET( ApiTokenSchemaDescriptor.API_ENDPOINT + "/" ).content()
            .getList( "apiToken", JsonApiToken.class );
        assertEquals( 1, apiTokens.size() );
    }

    @Test
    void testPatchApiTokenIntegerProperty()
    {
        final String uid = createNewTokenWithAttributes();
        final ApiToken apiToken1 = fetchAsEntity( uid );
        assertEquals( 1, (int) apiToken1.getVersion() );
        assertStatus( HttpStatus.OK, PATCH( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
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
            PATCH( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}", uid + "?importReportMode=ERRORS", Body(
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
        final HttpResponse patch = PATCH( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
            newToken.getUid() + "?importReportMode=ERRORS",
            Body( "[{'op':'replace','path':'/key','value':'MY NEW VALUE'}]" ) );
        final ApiToken afterPatched = apiTokenService.getWithUid( newToken.getUid() );
        assertEquals( newToken.getKey(), afterPatched.getKey() );
    }

    @Test
    void testCantAddInvalidIp()
    {
        final HttpResponse post = POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/",
            "{'attributes':[{'type': 'IpAllowedList','allowedIps':['X.1.1.1','2.2.2.2','3.3.3.3']}]}" );
        assertEquals( "Not a valid ip address, value=X.1.1.1", post.error().getMessage() );
    }

    @Test
    void testCantAddInvalidIpPut()
    {
        final ApiToken token = createNewEmptyToken();
        token.addIpToAllowedList( "X.1.1.1" );
        final HttpResponse put = PUT( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}", token.getUid(),
            Body( renderService.toJsonAsString( token ) ) );
        assertEquals( "Not a valid ip address, value=X.1.1.1", put.error().getMessage() );
    }

    @Test
    void testCantAddInvalidIpPatch()
    {
        final ApiToken token = createNewEmptyToken();
        final HttpResponse patch = PATCH( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
            token.getUid() + "?importReportMode=ERRORS", Body(
                "[{'op':'replace','path':'/attributes','value':[{'type':'IpAllowedList','allowedIps':['X.1.1.1']}]}]" ) );
        assertEquals( "Not a valid ip address, value=X.1.1.1", patch.error().getMessage() );
    }

    @Test
    void testCantAddInvalidMethod()
    {
        final HttpResponse post = POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/",
            "{'attributes':[" + "{'type':'MethodAllowedList','allowedMethods':['POST','X','PATCH']}" + "]}" );
        assertEquals( "Not a valid http method, value=X", post.error().getMessage() );
    }

    @Test
    void testCantAddInvalidReferrer()
    {
        final HttpResponse post = POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/", "{'attributes':["
            + "{'type':'RefererAllowedList','allowedReferrers':['http:XXX//hostname3.com','http://hostname2.com','http://hostname1.com']}]}" );
        assertEquals( "Not a valid referrer url, value=http:XXX//hostname3.com", post.error().getMessage() );
    }

    @Test
    void testCanModifyWithPut()
    {
        final ApiToken newToken = createNewEmptyToken();
        final ApiToken apiToken1 = fetchAsEntity( newToken.getUid() );
        apiToken1.addReferrerToAllowedList( "http://hostname1.com" );
        apiToken1.addMethodToAllowedList( "GET" );
        apiToken1.addIpToAllowedList( "2.2.2.2" );
        assertStatus( HttpStatus.OK, PUT( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
            newToken.getUid() + "?importReportMode=ERRORS", Body( renderService.toJsonAsString( apiToken1 ) ) ) );
        final ApiToken apiToken2 = fetchAsEntity( newToken.getUid() );
        assertTrue( apiToken2.getIpAllowedList().getAllowedIps().contains( "2.2.2.2" ) );
        assertTrue( apiToken2.getMethodAllowedList().getAllowedMethods().contains( "GET" ) );
        assertTrue( apiToken2.getRefererAllowedList().getAllowedReferrers().contains( "http://hostname1.com" ) );
        apiToken2.getIpAllowedList().getAllowedIps().remove( "2.2.2.2" );
        apiToken2.getMethodAllowedList().getAllowedMethods().remove( "GET" );
        apiToken2.getRefererAllowedList().getAllowedReferrers().remove( "http://hostname1.com" );
        assertStatus( HttpStatus.OK, PUT( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
            newToken.getUid() + "?importReportMode=ERRORS", Body( renderService.toJsonAsString( apiToken2 ) ) ) );
        final ApiToken apiToken3 = fetchAsEntity( newToken.getUid() );
        assertFalse( apiToken3.getIpAllowedList().getAllowedIps().contains( "2.2.2.2" ) );
        assertFalse( apiToken3.getMethodAllowedList().getAllowedMethods().contains( "GET" ) );
        assertFalse( apiToken3.getRefererAllowedList().getAllowedReferrers().contains( "http://hostname1.com" ) );
    }

    @Test
    void testCantModifyKeyPut()
    {
        final ApiToken newToken = createNewEmptyToken();
        final ApiToken apiToken1 = fetchAsEntity( newToken.getUid() );
        apiToken1.setKey( "x" );
        final HttpResponse put = PUT( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
            newToken.getUid() + "?importReportMode=ERRORS", Body( renderService.toJsonAsString( apiToken1 ) ) );
        final ApiToken afterPatched = apiTokenService.getWithUid( newToken.getUid() );
        assertEquals( newToken.getKey(), afterPatched.getKey() );
    }

    @Test
    void testCantModifyOthers()
    {
        final ApiToken newToken = createNewEmptyToken();
        final ApiToken apiToken1 = fetchAsEntity( newToken.getUid() );
        apiToken1.setKey( "x" );
        switchToNewUser( "anonymous" );
        assertStatus( HttpStatus.NOT_FOUND, PUT( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
            newToken.getUid() + "?importReportMode=ERRORS", Body( renderService.toJsonAsString( apiToken1 ) ) ) );
    }

    @Test
    void testDelete()
    {
        final ApiToken newToken = createNewEmptyToken();
        assertStatus( HttpStatus.OK, DELETE( ApiTokenSchemaDescriptor.API_ENDPOINT + "/" + newToken.getUid() ) );
        assertStatus( HttpStatus.NOT_FOUND,
            GET( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{uid}", newToken.getUid() ) );
    }

    @Test
    void testCantDeleteOtherTokens()
    {
        final ApiToken newToken = createNewEmptyToken();
        switchContextToUser( userA );
        assertStatus( HttpStatus.NOT_FOUND, DELETE( ApiTokenSchemaDescriptor.API_ENDPOINT + "/" + newToken.getUid() ) );
    }

    @Test
    void testCreateApiTokenExpireInFuture()
    {
        final long ONE_HOUR_FROM_NOW = System.currentTimeMillis() + 3600000;
        assertStatus( HttpStatus.CREATED,
            POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/", "{'expire': " + ONE_HOUR_FROM_NOW + "}" ) );
    }

    @Test
    void testCreateAndFetchWithAnotherUser()
    {
        final ApiToken newToken = createNewEmptyToken();
        switchToNewUser( "anonymous" );
        assertStatus( HttpStatus.NOT_FOUND,
            GET( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{uid}", newToken.getUid() ) );
        switchToSuperuser();
        fetchAsEntity( newToken.getUid() );
    }

    private ApiToken createNewEmptyToken()
    {
        final HttpResponse post = POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/", "{}" );
        final String uid = assertStatus( HttpStatus.CREATED, post );
        return apiTokenService.getWithUid( uid );
    }

    private String createNewTokenWithAttributes()
    {
        final HttpResponse post = POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/",
            "{'attributes':[{'type': 'IpAllowedList','allowedIps':['1.1.1.1','2.2.2.2','3.3.3.3']},"
                + "{'type':'MethodAllowedList','allowedMethods':['POST','GET','PATCH']},"
                + "{'type':'RefererAllowedList','allowedReferrers':['http://hostname3.com','http://hostname2.com','http://hostname1.com']}]}" );
        return assertStatus( HttpStatus.CREATED, post );
    }

    private ApiToken fetchAsEntity( String uid )
    {
        return apiTokenService.getWithUid( uid );
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
