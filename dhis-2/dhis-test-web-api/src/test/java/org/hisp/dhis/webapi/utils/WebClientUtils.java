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
package org.hisp.dhis.webapi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.webapi.WebClient.HttpResponse;
import org.hisp.dhis.webapi.WebClient.RequestComponent;
import org.hisp.dhis.webapi.json.domain.JsonError;
import org.hisp.dhis.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.webapi.json.domain.JsonObjectReport;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;

/**
 * Helpers needed when testing with {@link org.hisp.dhis.webapi.WebClient} and
 * {@link org.springframework.test.web.servlet.MockMvc}.
 *
 * @author Jan Bernitt
 */
public class WebClientUtils
{

    private static final Pattern IS_MEDIA_TYPE = Pattern.compile( "^[-a-z]+/[-a-z]+:" );

    public static String plainTextOrJson( String body )
    {
        if ( startWithMediaType( body ) )
        {
            return body.substring( body.indexOf( ':' ) + 1 );
        }
        return body.replace( '\'', '"' );
    }

    public static boolean startWithMediaType( String body )
    {
        return body != null && IS_MEDIA_TYPE.matcher( body ).find();
    }

    /**
     * Asserts that the {@link HttpResponse} has the expected
     * {@link HttpStatus}.
     *
     * If status is {@link HttpStatus#CREATED} the method returns the UID of the
     * created object in case it is provided by the response. This is based on a
     * convention used in DHIS2.
     *
     * @param expected status we should get
     * @param actual the response we actually got
     * @return UID of the created object (if available) or {@code null}
     */
    public static String assertStatus( HttpStatus expected, HttpResponse actual )
    {
        HttpStatus actualStatus = actual.status();
        if ( expected != actualStatus )
        {
            if ( expected.series() == actualStatus.series() )
            {
                String msg = actual.error( actualStatus.series() ).getMessage();
                assertEquals( expected, actualStatus, msg );
            }
            else
            {
                // OBS! we use the actual state to not fail the check in error
                String msg = actual.error( actualStatus.series() ).summary();
                assertEquals( expected, actualStatus, msg );
            }
        }
        assertValidLocation( actual );
        return getCreatedId( actual );
    }

    /**
     * Asserts that the {@link HttpResponse} has the expected
     * {@link HttpStatus.Series}. This is useful on cases where it only matters
     * that operation was {@link Series#SUCCESSFUL} or say
     * {@link Series#CLIENT_ERROR} but not which exact code of the series.
     *
     * If status is {@link HttpStatus#CREATED} the method returns the UID of the
     * created object in case it is provided by the response. This is based on a
     * convention used in DHIS2.
     *
     * @param expected status {@link Series} we should get
     * @param actual the response we actually got
     * @return UID of the created object (if available) or {@code null}
     */
    public static String assertSeries( HttpStatus.Series expected, HttpResponse actual )
    {
        Series actualSeries = actual.series();
        if ( expected != actualSeries )
        {
            // OBS! we use the actual state to not fail the check in error
            String msg = actual.error( actualSeries ).summary();
            assertEquals( expected, actualSeries, msg );
        }
        assertValidLocation( actual );
        return getCreatedId( actual );
    }

    public static void assertValidLocation( HttpResponse actual )
    {
        String location = actual.location();
        if ( location == null )
        {
            return;
        }
        assertTrue( location.startsWith( "http://" ) || location.startsWith( "https://" ),
            "Location header does not start with http or https" );
        assertTrue( location.indexOf( "http://", 4 ) < 0 && location.indexOf( "https://", 4 ) < 0,
            "Location header does contain multiple protocol parts" );
    }

    private static String getCreatedId( HttpResponse response )
    {
        HttpStatus actual = response.status();
        if ( actual == HttpStatus.CREATED )
        {
            JsonObject report = response.contentUnchecked().getObject( "response" );
            if ( report.exists() )
            {
                return report.getString( "uid" ).string();
            }
        }
        String location = response.location();
        return location == null ? null : location.substring( location.lastIndexOf( '/' ) + 1 );
    }

    /**
     * Assumes the {@link org.hisp.dhis.webapi.json.domain.JsonError} has a
     * {@link org.hisp.dhis.webapi.json.domain.JsonTypeReport} containing a
     * single {@link org.hisp.dhis.webapi.json.domain.JsonErrorReport} with the
     * expected properties.
     *
     * @param expectedCode The code the single error is expected to have
     * @param expectedMessage The message the single error is expected to have
     * @param actual the actual error from the {@link HttpResponse}
     */
    public static void assertError( ErrorCode expectedCode, String expectedMessage, JsonError actual )
    {
        JsonList<JsonObjectReport> reports = actual.getTypeReport().getObjectReports();
        assertEquals( 1, reports.size() );
        JsonList<JsonErrorReport> errors = reports.get( 0 ).getErrorReports();
        assertEquals( 1, errors.size() );
        JsonErrorReport error = errors.get( 0 );
        assertEquals( expectedCode, error.getErrorCode() );
        assertEquals( expectedMessage, error.getMessage() );
    }

    public static String substitutePlaceholders( String url, Object[] args )
    {
        if ( args.length == 0 )
        {
            return url;
        }
        Object[] urlArgs = Arrays.stream( args ).filter( arg -> !(arg instanceof RequestComponent) ).toArray();
        return String.format( url.replaceAll( "\\{[a-zA-Z]+}", "%s" ), (Object[]) urlArgs );
    }

    public static String objectReferences( String... uids )
    {
        StringBuilder str = new StringBuilder();
        str.append( '[' );
        for ( String uid : uids )
        {
            if ( str.length() > 1 )
            {
                str.append( ',' );
            }
            str.append( objectReference( uid ) );
        }
        str.append( ']' );
        return str.toString();
    }

    public static String objectReference( String uid )
    {
        return String.format( "{\"id\":\"%s\"}", uid );
    }

    public static <T> T failOnException( Callable<T> op )
    {
        try
        {
            return op.call();
        }
        catch ( Exception ex )
        {
            throw new AssertionError( ex );
        }
    }

    public static RequestComponent[] requestComponentsIn( Object... args )
    {
        List<RequestComponent> components = new ArrayList<>();
        for ( Object arg : args )
        {
            if ( arg instanceof RequestComponent )
            {
                components.add( (RequestComponent) arg );
            }
        }
        return components.toArray( new RequestComponent[0] );
    }

    @SuppressWarnings( "unchecked" )
    public static <T extends RequestComponent> T getComponent( Class<T> type, RequestComponent[] components )
    {
        for ( RequestComponent c : components )
        {
            if ( c.getClass() == type )
            {
                return (T) c;
            }
        }
        return null;
    }

    public static MediaType asMediaType( Object value )
    {
        if ( value == null || value instanceof MediaType )
        {
            return (MediaType) value;
        }
        String mediaType = value.toString();
        int typeSubtypeDivider = mediaType.indexOf( '/' );
        int charsetIndex = mediaType.indexOf( "; charset=" );
        if ( charsetIndex > 0 )
        {
            return new MediaType( mediaType.substring( 0, typeSubtypeDivider ),
                mediaType.substring( typeSubtypeDivider + 1, charsetIndex ),
                Charset.forName( mediaType.substring( charsetIndex + 10 ) ) );
        }
        return new MediaType( mediaType.substring( 0, typeSubtypeDivider ),
            mediaType.substring( typeSubtypeDivider + 1 ) );
    }

    private WebClientUtils()
    {
        throw new UnsupportedOperationException( "util" );
    }
}
