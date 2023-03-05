/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link org.hisp.dhis.webapi.openapi.QueryController} with Mock MVC
 * tests.
 *
 * @author Austin McGee
 */
class QueryControllerTest extends DhisControllerConvenienceTest
{
    private static final String API_QUERY_ALIAS = "/query/alias";

    private static final String API_QUERY_ALIAS_REDIRECT = "/query/redirect";

    private static final String testTargetUrl = "/api/me";

    private static final String testTargetUrlHash = CodecUtils.sha1Hex( testTargetUrl );

    private static final String testTargetAliasUrl = API_QUERY_ALIAS + "/" + testTargetUrlHash;

    @Test
    void testGetUninitializedAlias()
    {
        assertStatus( HttpStatus.NOT_FOUND, GET( API_QUERY_ALIAS + testTargetUrlHash ) );
    }

    @Test
    void testCreateAlias()
    {
        JsonObject doc = POST( API_QUERY_ALIAS, "{ \"target\": \"" + testTargetUrl + "\" }" ).content();

        assertTrue( doc.isObject() );
        assertTrue( doc.getString( "alias" ).string().equals( testTargetAliasUrl ) );

        JsonObject targetResponse = GET( "/me" ).content();
        JsonObject aliasResponse = GET( testTargetAliasUrl ).content();

        assertTrue( targetResponse.toString().equals( aliasResponse.toString() ) );
    }

    @Test
    void testCreateAliasInvalidPayload()
    {
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS, "/api/me" ) );
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS, "\"/api/me\"" ) );
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS, "{ \"destination\": \"/api/me\"" ) );
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS, "" ) );
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS, "\"/api/me\"" ) );
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS, "{ \"target\": \"/me\" }" ) );
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS, "{ \"target\": \"api/me\" }" ) );
        assertStatus( HttpStatus.BAD_REQUEST,
            POST( API_QUERY_ALIAS, "{ \"target\": \"http://localhost:8080/api/me\" }" ) );
    }

    @Test
    void tesAliasRedirect()
    {
        HttpResponse response = POST( API_QUERY_ALIAS_REDIRECT, "{ \"target\": \"" + testTargetUrl + "\" }" );
        assertStatus( HttpStatus.SEE_OTHER, response );
        assertEquals( response.header( "Location" ), "/api/query/alias/" + testTargetUrlHash );
    }

    @Test
    void testAliasRedirectInvalidPayload()
    {
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS_REDIRECT, "/api/me" ) );
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS_REDIRECT, "\"/api/me\"" ) );
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS_REDIRECT, "{ \"destination\": \"/api/me\"" ) );
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS_REDIRECT, "" ) );
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS_REDIRECT, "\"/api/me\"" ) );
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS_REDIRECT, "{ \"target\": \"/me\" }" ) );
        assertStatus( HttpStatus.BAD_REQUEST, POST( API_QUERY_ALIAS_REDIRECT, "{ \"target\": \"api/me\" }" ) );
        assertStatus( HttpStatus.BAD_REQUEST,
            POST( API_QUERY_ALIAS_REDIRECT, "{ \"target\": \"http://localhost:8080/api/me\" }" ) );
    }
}
