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
package org.hisp.dhis.web;

import static org.hisp.dhis.web.WebClientUtils.assertSeries;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.web.WebClientUtils.failOnException;
import static org.hisp.dhis.web.WebClientUtils.fileContent;
import static org.hisp.dhis.web.WebClientUtils.getComponent;
import static org.hisp.dhis.web.WebClientUtils.plainTextOrJson;
import static org.hisp.dhis.web.WebClientUtils.requestComponentsIn;
import static org.hisp.dhis.web.WebClientUtils.startsWithMediaType;
import static org.hisp.dhis.web.WebClientUtils.substitutePlaceholders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.webapi.json.domain.JsonError;

/**
 * The purpose of this interface is to allow mixin style addition of the
 * convenience web API by implementing this interface's essential method
 * {@link #webRequest(HttpMethod, String, List, String, String)}.
 *
 * @author Jan Bernitt
 */
@FunctionalInterface
@SuppressWarnings( "java:S100" )
public interface WebClient
{
    HttpResponse webRequest( HttpMethod method, String url, List<Header> headers, String contentType,
        String content );

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

    static Header ContentType( Object mimeType )
    {
        return ContentType( mimeType.toString() );
    }

    static Header ContentType( String mimeType )
    {
        return Header( "ContentType", mimeType );
    }

    static Header Accept( Object mimeType )
    {
        return Accept( mimeType.toString() );
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

        public String getName()
        {
            return name;
        }

        public Object getValue()
        {
            return value;
        }
    }

    final class Body implements RequestComponent
    {
        final String content;

        Body( String content )
        {
            this.content = content;
        }
    }

    default HttpResponse GET( String url, Object... args )
    {
        return webRequest( HttpMethod.GET, substitutePlaceholders( url, args ), requestComponentsIn( args ) );
    }

    default HttpResponse POST( String url, Object... args )
    {
        return webRequest( HttpMethod.POST, substitutePlaceholders( url, args ), requestComponentsIn( args ) );
    }

    default HttpResponse POST( String url, String body )
    {
        return webRequest( HttpMethod.POST, url, new Body( body ) );
    }

    default HttpResponse PATCH( String url, Object... args )
    {
        // Default mime-type is added as first element so that content type in
        // arguments does not override it
        return webRequest( HttpMethod.PATCH, substitutePlaceholders( url, args ),
            ArrayUtils.insert( 0, requestComponentsIn( args ), ContentType( "application/json-patch+json" ) ) );
    }

    default HttpResponse PATCH( String url, String body )
    {
        return webRequest( HttpMethod.PATCH, url, ContentType( "application/json-patch+json" ), Body( body ) );
    }

    default HttpResponse PATCH_OLD( String url, String body )
    {
        return webRequest( HttpMethod.PATCH, url, ContentType( "application/json" ), Body( body ) );
    }

    default HttpResponse PUT( String url, Object... args )
    {
        return webRequest( HttpMethod.PUT, substitutePlaceholders( url, args ), requestComponentsIn( args ) );
    }

    default HttpResponse PUT( String url, String body )
    {
        return webRequest( HttpMethod.PUT, url, new Body( body ) );
    }

    default HttpResponse DELETE( String url, Object... args )
    {
        return webRequest( HttpMethod.DELETE, substitutePlaceholders( url, args ), requestComponentsIn( args ) );
    }

    default HttpResponse DELETE( String url, String body )
    {
        return webRequest( HttpMethod.DELETE, url, new Body( body ) );
    }

    default HttpResponse webRequest( HttpMethod method, String url, RequestComponent... components )
    {
        // configure headers
        String contentMediaType = null;
        List<Header> headers = new ArrayList<>();
        for ( RequestComponent c : components )
        {
            if ( c instanceof Header )
            {
                Header header = (Header) c;
                if ( header.name.equalsIgnoreCase( "ContentType" ) )
                {
                    contentMediaType = header.value.toString();
                }
                else
                {
                    headers.add( header );
                }
            }
        }
        // configure body
        Body bodyComponent = getComponent( Body.class, components );
        String body = bodyComponent == null ? "" : bodyComponent.content;
        if ( body != null && body.endsWith( ".json" ) )
        {
            String fileContentType = contentMediaType != null ? contentMediaType : "application/json; charset=utf8";
            return failOnException( () -> webRequest( method, url, headers, fileContentType, fileContent( body ) ) );
        }
        if ( body != null && body.endsWith( ".geojson" ) )
        {
            String fileContentType = contentMediaType != null ? contentMediaType : "application/geo+json; charset=utf8";
            return failOnException( () -> webRequest( method, url, headers, fileContentType, fileContent( body ) ) );
        }
        if ( startsWithMediaType( body ) )
        {
            return webRequest( method, url, headers,
                body.substring( 0, body.indexOf( ':' ) ),
                body.substring( body.indexOf( ':' ) + 1 ) );
        }
        String mediaType = contentMediaType != null ? contentMediaType : "application/json";
        return body == null || body.isEmpty()
            ? webRequest( method, url, headers, null, null )
            : webRequest( method, url, headers, mediaType, plainTextOrJson( body ) );
    }

    default <T> T run( Function<WebClient, ? extends WebSnippet<T>> snippet )
    {
        return snippet.apply( this ).run();
    }

    default <A, B> B run( BiFunction<WebClient, A, ? extends WebSnippet<B>> snippet, A argument )
    {
        return snippet.apply( this, argument ).run();
    }

    interface ResponseAdapter
    {

        int getStatus();

        String getContent();

        String getErrorMessage();

        String getHeader( String name );
    }

    final class HttpResponse
    {

        private final ResponseAdapter response;

        public HttpResponse( ResponseAdapter response )
        {
            this.response = response;
        }

        public HttpStatus status()
        {
            return HttpStatus.of( response.getStatus() );
        }

        public HttpStatus.Series series()
        {
            return status().series();
        }

        public boolean success()
        {
            return series() == HttpStatus.Series.SUCCESSFUL;
        }

        /**
         * Access raw response body for non JSON responses
         *
         * @param contentType the expected content type
         * @return raw content body in UTF-8 encoding
         */
        public String content( String contentType )
        {
            if ( contentType.equals( "application/json" ) )
            {
                fail( "Use one of the other content() methods for JSON" );
            }
            String actualContentType = header( "Content-Type" );
            assertTrue( actualContentType.startsWith( contentType ),
                String.format( "Expected %s but was: %s", contentType, actualContentType ) );
            return failOnException( response::getContent );
        }

        public JsonMixed content()
        {
            return content( HttpStatus.Series.SUCCESSFUL );
        }

        public JsonMixed content( HttpStatus.Series expected )
        {
            assertSeries( expected, this );
            return contentUnchecked();
        }

        public JsonMixed content( HttpStatus expected )
        {
            assertStatus( expected, this );
            return contentUnchecked();
        }

        public JsonError error()
        {
            assertTrue( series().value() >= 4, "not a client or server error" );
            return errorInternal();
        }

        public JsonError error( HttpStatus expected )
        {
            assertStatus( expected, this );
            return errorInternal();
        }

        public JsonError error( HttpStatus.Series expected )
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
                return JsonValue.of( error ).as( JsonError.class );
            }
            return contentUnchecked().as( JsonError.class );
        }

        public boolean hasBody()
        {
            return !response.getContent().isEmpty();
        }

        public JsonMixed contentUnchecked()
        {
            return failOnException( () -> JsonMixed.of( response.getContent() ) );
        }

        public String location()
        {
            return header( "Location" );
        }

        public String header( String name )
        {
            return response.getHeader( name );
        }
    }
}
