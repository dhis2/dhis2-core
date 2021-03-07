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
package org.hisp.dhis.webapi;

import static org.hisp.dhis.webapi.documentation.common.TestUtils.APPLICATION_JSON_UTF8;
import static org.hisp.dhis.webapi.utils.WebClientUtils.failOnException;
import static org.hisp.dhis.webapi.utils.WebClientUtils.plainTextOrJson;
import static org.hisp.dhis.webapi.utils.WebClientUtils.startWithMediaType;
import static org.hisp.dhis.webapi.utils.WebClientUtils.substitutePlaceholders;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.shaded.com.google.common.io.ByteStreams;

/**
 * The purpose of this interface is to allow mixin style addition of the
 * convenience web API by implementing this interface's essential method
 * {@link #webRequest(MockHttpServletRequestBuilder)}.
 *
 * @author Jan Bernitt
 */
@FunctionalInterface
public interface AuthenticatedWebClient
{

    WebClient.HttpResponse authWebRequest( String token, MockHttpServletRequestBuilder request );

    default WebClient.HttpResponse GET( String token, String url, Object... args )
    {
        return baseWebRequest( token, get( substitutePlaceholders( url, args ) ), "" );
    }

    default WebClient.HttpResponse POST( String url, Object... args )
    {
        return POST( substitutePlaceholders( url, args ), "" );
    }

    default WebClient.HttpResponse POST( String token, String url, String body )
    {
        return baseWebRequest( token, post( url ), body );
    }

    default WebClient.HttpResponse PATCH(  String token, String url, Object... args )
    {
        return PATCH( substitutePlaceholders( url, args ), "" );
    }

    default WebClient.HttpResponse PATCH(  String token, String url, String body )
    {
        return baseWebRequest( token, patch( url ), body );
    }

    default WebClient.HttpResponse PUT(  String token, String url, Object... args )
    {
        return PUT( substitutePlaceholders( url, args ), "" );
    }

    default WebClient.HttpResponse PUT(  String token, String url, String body )
    {
        return baseWebRequest( token, put( url ), body );
    }

    default WebClient.HttpResponse DELETE(  String token, String url, Object... args )
    {
        return DELETE( substitutePlaceholders( url, args ), "" );
    }

    default WebClient.HttpResponse DELETE(  String token, String url, String body )
    {
        return baseWebRequest( token, delete( url ), body );
    }

    default WebClient.HttpResponse baseWebRequest( String token, MockHttpServletRequestBuilder request, String body )
    {
        if ( body != null && body.endsWith( ".json" ) )
        {
            return failOnException( () -> authWebRequest( token, request
                .contentType( APPLICATION_JSON_UTF8 )
                .content( ByteStreams.toByteArray( new ClassPathResource( body ).getInputStream() ) ) ) );
        }
        if ( startWithMediaType( body ) )
        {
            return authWebRequest(token, request
                .contentType( body.substring( 0, body.indexOf( ':' ) ) )
                .content( body.substring( body.indexOf( ':' ) + 1 ) ) );
        }
        return body == null || body.isEmpty()
            ? authWebRequest( token,request )
            : authWebRequest( token,request
                .contentType( APPLICATION_JSON )
                .content( plainTextOrJson( body ) ) );
    }

}
