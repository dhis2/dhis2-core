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

import static org.hisp.dhis.webapi.utils.TestUtils.APPLICATION_JSON_PATCH_UTF8;
import static org.hisp.dhis.webapi.utils.TestUtils.APPLICATION_JSON_UTF8;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertSeries;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.hisp.dhis.webapi.utils.WebClientUtils.failOnException;
import static org.hisp.dhis.webapi.utils.WebClientUtils.getComponent;
import static org.hisp.dhis.webapi.utils.WebClientUtils.plainTextOrJson;
import static org.hisp.dhis.webapi.utils.WebClientUtils.requestComponentsIn;
import static org.hisp.dhis.webapi.utils.WebClientUtils.startWithMediaType;
import static org.hisp.dhis.webapi.utils.WebClientUtils.substitutePlaceholders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.hisp.dhis.webapi.json.JsonResponse;
import org.hisp.dhis.webapi.json.domain.JsonError;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.WebClientUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
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
public interface WebClient
{

    HttpResponse webRequest( MockHttpServletRequestBuilder request );

    interface RequestComponent
    {

    }

    static Header Header( String name, Object value )
    {
        return new Header( name, value );
    }

    static Header ApiTokenHeader( String token )
    {
        return Header( "Authorization", "ApiToken " + token );
    }

    static Header JwtTokenHeader( String token )
    {
        return Header( "Authorization", "Bearer " + token );
    }

    static Header ContentType( MediaType mimeType )
    {
        return Header( "ContentType", mimeType );
    }

    static Header ContentType( String mimeType )
    {
        return Header( "ContentType", mimeType );
    }

    static Header Accept( MediaType mimeType )
    {
        return Header( "Accept", mimeType );
    }

    static Header Accept( String mimeType )
    {
        return Header( "Accept", mimeType );
    }

    static Body Body( String body )
    {
        return new Body( body );
    }

    final class Header implements RequestComponent
    {
        final String name;

        final Object value;

        Header( String name, Object value )
        {
            this.name = name;
            this.value = value;
        }
    }

    final class Body implements RequestComponent
    {
        final String body;

        Body( String body )
        {
            this.body = body;
        }
    }

    default HttpResponse GET( String url, Object... args )
    {
        return webRequest( get( substitutePlaceholders( url, args ) ), requestComponentsIn( args ) );
    }

    default HttpResponse POST( String url, Object... args )
    {
        return webRequest( post( substitutePlaceholders( url, args ) ), requestComponentsIn( args ) );
    }

    default HttpResponse POST( String url, String body )
    {
        return webRequest( post( url ), new Body( body ) );
    }

    default HttpResponse POST_MULTIPART( String url, MockMultipartFile part )
    {
        return webRequest( multipart( url ).file( part ) );
    }

    default HttpResponse PATCH( String url, Object... args )
    {
        return webRequest( patch( substitutePlaceholders( url, args ) ),
            // default mime type is added as first element so that ContentType
            // in args does override it
            ArrayUtils.insert( 0, requestComponentsIn( args ), ContentType( APPLICATION_JSON_PATCH_UTF8 ) ) );
    }

    default HttpResponse PATCH( String url, String body )
    {
        return webRequest( patch( url ), ContentType( APPLICATION_JSON_PATCH_UTF8 ), Body( body ) );
    }

    default HttpResponse PATCH_OLD( String url, String body )
    {
        return webRequest( patch( url ), ContentType( MediaType.APPLICATION_JSON ), Body( body ) );
    }

    default HttpResponse PUT( String url, Object... args )
    {
        return webRequest( put( substitutePlaceholders( url, args ) ), requestComponentsIn( args ) );
    }

    default HttpResponse PUT( String url, String body )
    {
        return webRequest( put( url ), new Body( body ) );
    }

    default HttpResponse DELETE( String url, Object... args )
    {
        return webRequest( delete( substitutePlaceholders( url, args ) ), requestComponentsIn( args ) );
    }

    default HttpResponse DELETE( String url, String body )
    {
        return webRequest( delete( url ), new Body( body ) );
    }

    default HttpResponse webRequest( MockHttpServletRequestBuilder request, RequestComponent... components )
    {
        // configure headers
        MediaType contentMediaType = null;
        for ( RequestComponent c : components )
        {
            if ( c instanceof Header )
            {
                Header header = (Header) c;
                if ( header.name.equalsIgnoreCase( "ContentType" ) )
                {
                    contentMediaType = WebClientUtils.asMediaType( header.value );
                }
                else
                {
                    request.header( header.name, header.value );
                }
            }
        }
        // configure body
        Body bodyComponent = getComponent( Body.class, components );
        String body = bodyComponent == null ? "" : bodyComponent.body;

        if ( body != null && body.endsWith( ".json" ) )
        {
            MediaType fileContentType = contentMediaType != null ? contentMediaType : APPLICATION_JSON_UTF8;
            return failOnException( () -> webRequest( request
                .contentType( fileContentType )
                .content( ByteStreams.toByteArray( new ClassPathResource( body ).getInputStream() ) ) ) );
        }

        if ( startWithMediaType( body ) )
        {
            return webRequest( request
                .contentType( body.substring( 0, body.indexOf( ':' ) ) )
                .content( body.substring( body.indexOf( ':' ) + 1 ) ) );
        }

        return body == null || body.isEmpty()
            ? webRequest( request )
            : webRequest( request
                .contentType( contentMediaType != null ? contentMediaType : MediaType.APPLICATION_JSON )
                .content( plainTextOrJson( body ) ) );
    }

    default <T> T run( Function<WebClient, ? extends WebSnippet<T>> snippet )
    {
        return snippet.apply( this ).run();
    }

    default <A, B> B run( BiFunction<WebClient, A, ? extends WebSnippet<B>> snippet, A argument )
    {
        return snippet.apply( this, argument ).run();
    }

    final class HttpResponse
    {
        private final MockHttpServletResponse response;

        public HttpResponse( MockHttpServletResponse response )
        {
            this.response = response;
        }

        public HttpStatus status()
        {
            return HttpStatus.resolve( response.getStatus() );
        }

        public HttpStatus.Series series()
        {
            return status().series();
        }

        public boolean success()
        {
            return series() == Series.SUCCESSFUL;
        }

        /**
         * Access raw response body for non JSON responses
         *
         * @param contentType the expected content type
         * @return raw content body in UTF-8 encoding
         */
        public String content( MediaType contentType )
        {
            if ( contentType.isCompatibleWith( MediaType.APPLICATION_JSON ) )
            {
                fail( "Use one of the other content() methods for JSON" );
            }
            String actualContentType = header( "Content-Type" );
            String expected = contentType.toString();
            assertTrue( String.format( "Expected %s but was: %s", expected, actualContentType ),
                actualContentType.startsWith( expected ) );
            return failOnException( () -> response.getContentAsString( StandardCharsets.UTF_8 ) );
        }

        public JsonResponse content()
        {
            return content( Series.SUCCESSFUL );
        }

        public JsonResponse content( Series expected )
        {
            assertSeries( expected, this );
            return contentUnchecked();
        }

        public JsonResponse content( HttpStatus expected )
        {
            assertStatus( expected, this );
            return contentUnchecked();
        }

        public JsonError error()
        {
            assertTrue( "not a client or server error", series().value() >= 4 );
            return errorInternal();
        }

        public JsonError error( HttpStatus expected )
        {
            assertStatus( expected, this );
            return errorInternal();
        }

        public JsonError error( Series expected )
        {
            // OBS! cannot use assertSeries as it uses this method
            assertEquals( expected, series() );
            return errorInternal();
        }

        private JsonError errorInternal()
        {
            if ( !hasBody() )
            {
                String errorMessage = response.getErrorMessage();
                if ( errorMessage != null )
                {
                    errorMessage = '"' + errorMessage + '"';
                }
                String error = String.format(
                    "{\"status\": \"error\",\"httpStatus\":\"%s\",\"httpStatusCode\":%d, \"message\":%s}",
                    status().name(), response.getStatus(), errorMessage );
                return new JsonResponse( error ).as( JsonError.class );
            }
            return contentUnchecked().as( JsonError.class );
        }

        public boolean hasBody()
        {
            try
            {
                return !response.getContentAsString( StandardCharsets.UTF_8 ).isEmpty();
            }
            catch ( UnsupportedEncodingException ex )
            {
                return true;
            }
        }

        public JsonResponse contentUnchecked()
        {
            return failOnException( () -> new JsonResponse( response.getContentAsString( StandardCharsets.UTF_8 ) ) );
        }

        public String location()
        {
            return header( ContextUtils.HEADER_LOCATION );
        }

        public String header( String name )
        {
            return response.getHeader( name );
        }
    }
}
