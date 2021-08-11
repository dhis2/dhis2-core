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
package org.hisp.dhis.webapi.security;

import static org.hisp.dhis.webapi.WebClient.Body;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.descriptors.ApiTokenSchemaDescriptor;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonList;
import org.hisp.dhis.webapi.json.JsonObject;
import org.hisp.dhis.webapi.json.domain.JsonApiToken;
import org.hisp.dhis.webapi.utils.WebClientUtils;
import org.junit.Before;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class ApiTokenControllerTest extends DhisControllerConvenienceTest
{
    @Autowired
    private ApiTokenService apiTokenService;

    @Autowired
    private RenderService _renderService;

    @Before
    public final void setupTest()
    {
        this.renderService = _renderService;
    }

    @Test
    public void testCreate()
        throws IOException
    {
        final JsonObject jsonObject = assertApiTokenCreatedResponse(
            POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/", "{}" ) );

        final String uid = jsonObject.getString( "uid" ).string();
        final String key = jsonObject.getString( "key" ).string();

        assertNotNull( uid );
        assertEquals( 128, key.length() );

        final ApiToken token = fetchAsEntity( uid );

        assertEquals( 64, token.getKey().length() );
    }

    @Test
    public void testCreateApiToken()
        throws IOException
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
    public void testListApiTokens()
    {
        createNewTokenWithAttributes();
        createNewTokenWithAttributes();
        createNewTokenWithAttributes();

        final JsonList<JsonApiToken> apiTokens = GET( ApiTokenSchemaDescriptor.API_ENDPOINT + "/" )
            .content().getList( "apiToken", JsonApiToken.class );

        assertEquals( 3, apiTokens.size() );
    }

    @Test
    public void testListApiTokensNotYours()
    {
        createNewTokenWithAttributes();
        createNewTokenWithAttributes();
        createNewTokenWithAttributes();

        switchToNewUser( "anonymous" );

        createNewTokenWithAttributes();

        final JsonList<JsonApiToken> apiTokens = GET( ApiTokenSchemaDescriptor.API_ENDPOINT + "/" )
            .content().getList( "apiToken", JsonApiToken.class );

        assertEquals( 1, apiTokens.size() );
    }

    @Test
    public void testPatchApiTokenIntegerProperty()
        throws IOException
    {
        final String uid = createNewTokenWithAttributes();

        final ApiToken apiToken1 = fetchAsEntity( uid );
        assertEquals( 1, (int) apiToken1.getVersion() );

        assertStatus( HttpStatus.OK,
            PATCH( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}", uid + "?importReportMode=ERRORS",
                Body( "[{'op': 'replace', 'path': '/version', 'value': 333}]" ) ) );

        final ApiToken apiToken2 = fetchAsEntity( uid );
        assertEquals( 333, (int) apiToken2.getVersion() );
    }

    @Test
    public void testPatchApiTokenAttributesProperty()
        throws IOException
    {
        final String uid = createNewTokenWithAttributes();

        final ApiToken apiToken1 = fetchAsEntity( uid );
        assertEquals( 3, apiToken1.getIpAllowedList().getAllowedIps().size() );
        assertTrue( apiToken1.getIpAllowedList().getAllowedIps().contains( "1.1.1.1" ) );
        assertFalse( apiToken1.getIpAllowedList().getAllowedIps().contains( "8.8.8.8" ) );

        assertStatus( HttpStatus.OK,
            PATCH( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}", uid + "?importReportMode=ERRORS",
                Body(
                    "[{'op':'replace','path':'/attributes','value':[{'type':'IpAllowedList','type': 'IpAllowedList','allowedIps':['8.8.8.8']}]}]" ) ) );

        final ApiToken apiToken2 = fetchAsEntity( uid );
        assertEquals( 1, apiToken2.getIpAllowedList().getAllowedIps().size() );
        assertFalse( apiToken2.getIpAllowedList().getAllowedIps().contains( "1.1.1.1" ) );
        assertTrue( apiToken2.getIpAllowedList().getAllowedIps().contains( "8.8.8.8" ) );
    }

    @Test
    public void testCantModifyKeyPatch()
        throws IOException
    {
        final ApiToken newToken = createNewEmptyToken();

        final HttpResponse patch = PATCH( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
            newToken.getUid() + "?importReportMode=ERRORS",
            Body(
                "[{'op':'replace','path':'/key','value':'MY NEW VALUE'}]" ) );

        assertEquals( "ApiToken key can not be modified.", patch.error().getMessage() );
    }

    @Test
    public void testCantAddInvalidIp()
    {
        final HttpResponse post = POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/",
            "{'attributes':[{'type':'IpAllowedList','type': 'IpAllowedList','allowedIps':['X.1.1.1','2.2.2.2','3.3.3.3']}]}" );

        assertEquals( "Not a valid ip address, value=X.1.1.1", post.error().getMessage() );
    }

    @Test
    public void testCantAddInvalidIpPut()
        throws IOException
    {
        final ApiToken token = createNewEmptyToken();
        token.addIpToAllowedList( "X.1.1.1" );

        final HttpResponse put = PUT( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}", token.getUid(),
            Body( renderService.toJsonAsString( token ) ) );

        assertEquals( "Not a valid ip address, value=X.1.1.1", put.error().getMessage() );
    }

    @Test
    public void testCantAddInvalidIpPatch()
        throws IOException
    {
        final ApiToken token = createNewEmptyToken();

        final HttpResponse patch = PATCH( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
            token.getUid() + "?importReportMode=ERRORS",
            Body(
                "[{'op':'replace','path':'/attributes','value':[{'type':'IpAllowedList','type': 'IpAllowedList','allowedIps':['X.1.1.1']}]}]" ) );

        assertEquals( "Not a valid ip address, value=X.1.1.1", patch.error().getMessage() );
    }

    @Test
    public void testCantAddInvalidMethod()
    {
        final HttpResponse post = POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/",
            "{'attributes':["
                + "{'type':'MethodAllowedList','type':'MethodAllowedList','allowedMethods':['POST','X','PATCH']}"
                + "]}" );

        assertEquals( "Not a valid http method, value=X", post.error().getMessage() );
    }

    @Test
    public void testCantAddInvalidReferrer()
    {
        final HttpResponse post = POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/",
            "{'attributes':["
                + "{'type':'RefererAllowedList','type':'RefererAllowedList','allowedReferrers':['http:XXX//hostname3.com','http://hostname2.com','http://hostname1.com']}]}" );

        assertEquals( "Not a valid referrer url, value=http:XXX//hostname3.com", post.error().getMessage() );
    }

    @Test
    public void testCanModifyWithPut()
        throws IOException
    {
        final ApiToken newToken = createNewEmptyToken();

        final ApiToken apiToken1 = fetchAsEntity( newToken.getUid() );
        apiToken1.addReferrerToAllowedList( "http://hostname1.com" );
        apiToken1.addMethodToAllowedList( "GET" );
        apiToken1.addIpToAllowedList( "2.2.2.2" );

        assertStatus( HttpStatus.OK, PUT( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
            newToken.getUid() + "?importReportMode=ERRORS",
            Body( renderService.toJsonAsString( apiToken1 ) ) ) );

        final ApiToken apiToken2 = fetchAsEntity( newToken.getUid() );
        assertTrue( apiToken2.getIpAllowedList().getAllowedIps().contains( "2.2.2.2" ) );
        assertTrue( apiToken2.getMethodAllowedList().getAllowedMethods().contains( "GET" ) );
        assertTrue( apiToken2.getRefererAllowedList().getAllowedReferrers().contains( "http://hostname1.com" ) );

        apiToken2.getIpAllowedList().getAllowedIps().remove( "2.2.2.2" );
        apiToken2.getMethodAllowedList().getAllowedMethods().remove( "GET" );
        apiToken2.getRefererAllowedList().getAllowedReferrers().remove( "http://hostname1.com" );

        assertStatus( HttpStatus.OK, PUT( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
            newToken.getUid() + "?importReportMode=ERRORS",
            Body( renderService.toJsonAsString( apiToken2 ) ) ) );

        final ApiToken apiToken3 = fetchAsEntity( newToken.getUid() );
        assertFalse( apiToken3.getIpAllowedList().getAllowedIps().contains( "2.2.2.2" ) );
        assertFalse( apiToken3.getMethodAllowedList().getAllowedMethods().contains( "GET" ) );
        assertFalse( apiToken3.getRefererAllowedList().getAllowedReferrers().contains( "http://hostname1.com" ) );
    }

    @Test
    public void testCantModifyKeyPut()
        throws IOException
    {
        final ApiToken newToken = createNewEmptyToken();

        final ApiToken apiToken1 = fetchAsEntity( newToken.getUid() );
        apiToken1.setKey( "x" );

        final HttpResponse put = PUT( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
            newToken.getUid() + "?importReportMode=ERRORS",
            Body( renderService.toJsonAsString( apiToken1 ) ) );

        assertEquals( "ApiToken key can not be modified.", put.error().getMessage() );
    }

    @Test
    public void testCantModifyOthers()
        throws IOException
    {
        final ApiToken newToken = createNewEmptyToken();

        final ApiToken apiToken1 = fetchAsEntity( newToken.getUid() );
        apiToken1.setKey( "x" );

        switchToNewUser( "anonymous" );

        assertStatus( HttpStatus.NOT_FOUND, PUT( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{id}",
            newToken.getUid() + "?importReportMode=ERRORS",
            Body( renderService.toJsonAsString( apiToken1 ) ) ) );
    }

    @Test
    public void testDelete()
        throws IOException
    {
        final ApiToken newToken = createNewEmptyToken();

        assertStatus( HttpStatus.OK,
            DELETE( ApiTokenSchemaDescriptor.API_ENDPOINT + "/" + newToken.getUid() ) );

        assertStatus( HttpStatus.NOT_FOUND,
            GET( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{uid}", newToken.getUid() ) );
    }

    @Test
    public void testCantDeleteOtherTokens()
        throws IOException
    {
        final ApiToken newToken = createNewEmptyToken();

        switchToNewUser( "anonymous" );

        assertStatus( HttpStatus.NOT_FOUND,
            DELETE( ApiTokenSchemaDescriptor.API_ENDPOINT + "/" + newToken.getUid() ) );
    }

    @Test
    public void testCreateApiTokenExpireInFuture()
    {
        final long ONE_HOUR_FROM_NOW = System.currentTimeMillis() + 3600000;

        assertStatus( HttpStatus.CREATED, POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/",
            "{'expire': " + ONE_HOUR_FROM_NOW + "}" ) );
    }

    @Test
    public void testCreateApiTokenFailExpireInPast()
    {
        final HttpResponse post = POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/",
            "{'expire': 0}" );

        assertEquals( "ApiToken expire timestamp must be in the future.", post.error().getMessage() );
    }

    @Test
    public void testCreateAndFetchWithAnotherUser()
        throws IOException
    {
        final ApiToken newToken = createNewEmptyToken();

        switchToNewUser( "anonymous" );

        assertStatus( HttpStatus.NOT_FOUND,
            GET( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{uid}", newToken.getUid() ) );

        switchToSuperuser();

        fetchAsEntity( newToken.getUid() );
    }

    private ApiToken createNewEmptyToken()
        throws IOException
    {
        final HttpResponse post = POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/", "{}" );
        final String uid = assertStatus( HttpStatus.CREATED,
            post );

        return fetchAsEntity( uid );
    }

    private String createNewTokenWithAttributes()
    {
        final HttpResponse post = POST( ApiTokenSchemaDescriptor.API_ENDPOINT + "/",
            "{'attributes':[{'type':'IpAllowedList','type': 'IpAllowedList','allowedIps':['1.1.1.1','2.2.2.2','3.3.3.3']},"
                + "{'type':'MethodAllowedList','type':'MethodAllowedList','allowedMethods':['POST','GET','PATCH']},"
                + "{'type':'RefererAllowedList','type':'RefererAllowedList','allowedReferrers':['http://hostname3.com','http://hostname2.com','http://hostname1.com']}]}" );
        return assertStatus( HttpStatus.CREATED, post );
    }

    private ApiToken fetchAsEntity( String uid )
        throws IOException
    {
        final String json = GET( ApiTokenSchemaDescriptor.API_ENDPOINT + "/{uid}", uid )
            .content().getJsonDocument().toString();

        return renderService.fromJson( json, ApiToken.class );
    }

    public static JsonObject assertApiTokenCreatedResponse( HttpResponse actual )
    {
        HttpStatus actualStatus = actual.status();
        if ( HttpStatus.CREATED != actualStatus )
        {
            assertEquals( "Actual response is not CREATED", HttpStatus.CREATED, actualStatus );
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
