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
package org.hisp.dhis.webapi.utils;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.hisp.dhis.webapi.WebClient.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;

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

    public static void assertStatus( HttpStatus expected, HttpResponse response )
    {
        HttpStatus actual = response.status();
        if ( expected != actual )
        {
            String msg = response.error( actual.series() ).getMessage();
            assertEquals( msg, expected, actual );
        }
    }

    public static void assertSeries( HttpStatus.Series expected, HttpResponse response )
    {
        Series actual = response.series();
        if ( expected != actual )
        {
            String msg = response.error( actual ).getMessage();
            assertEquals( msg, expected, actual );
        }
    }

    public static String substitutePlaceholders( String url, String[] args )
    {
        return args.length == 0
            ? url
            : String.format( url.replaceAll( "\\{[a-zA-Z]+}", "%s" ), (Object[]) args );
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

    private WebClientUtils()
    {
        throw new UnsupportedOperationException( "util" );
    }
}
