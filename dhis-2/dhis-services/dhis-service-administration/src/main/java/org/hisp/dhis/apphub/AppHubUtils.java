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
package org.hisp.dhis.apphub;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.UUID;
import java.util.regex.Pattern;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class AppHubUtils
{
    private static final ImmutableSet<String> ILLEGAL_QUERY_STRINGS = ImmutableSet
        .of( "..", "//", "http://", "https://", "file://" );

    private static final Pattern API_VERSION_PATTERN = Pattern.compile( "v\\d+" );

    /**
     * Validates the path and query segment. Checks whether the query is null or
     * contains illegal strings.
     *
     * @param query the query.
     * @throws IllegalQueryException if the query is invalid.
     */
    public static void validateQuery( String query )
        throws IllegalQueryException
    {
        if ( query == null || query.isEmpty() )
        {
            throw new IllegalQueryException( new ErrorMessage( ErrorCode.E1000 ) );
        }

        if ( ILLEGAL_QUERY_STRINGS.stream().anyMatch( query::contains ) )
        {
            throw new IllegalQueryException( new ErrorMessage( ErrorCode.E1001 ) );
        }
    }

    /**
     * Validate the API version. Must start with {@code v} followed by an
     * integer.
     *
     * @param apiVersion the API version string.
     * @throws IllegalQueryException if the API version is invalid.
     */
    public static void validateApiVersion( String apiVersion )
        throws IllegalQueryException
    {
        if ( !API_VERSION_PATTERN.matcher( apiVersion ).matches() )
        {
            throw new IllegalQueryException( new ErrorMessage( ErrorCode.E1002 ) );
        }
    }

    /**
     * Validates the UUID. Apphub uses UUID-v4 for all identifiers.
     *
     * @param uuid
     * @throws IllegalQueryException
     */

    public static void validateUuid( String uuid )
        throws IllegalQueryException
    {
        if ( uuid == null || uuid.isEmpty() )
        {
            throw new IllegalQueryException( new ErrorMessage( ErrorCode.E1003 ) );
        }

        try
        {
            // need to check equality because fromString converts strings like 1-1-1-1-1 to a valid UUID
            checkArgument( UUID.fromString( uuid ).toString() == uuid );
        }
        catch ( IllegalArgumentException e )
        {
            throw new IllegalQueryException( new ErrorMessage( ErrorCode.E7800, uuid ) );
        }
    }

    /**
     * Sanitizes the query. Removes leading forward slashes.
     *
     * @param query the query.
     * @return the sanitized query.
     */
    public static String sanitizeQuery( String query )
    {
        query = query.replaceFirst( "^/*", "" );
        return query;
    }

    /**
     * Returns an {@link HttpEntity} with {@link HttpHeaders} set to accept a
     * {@code application/json} response.
     *
     * @return a {@link HttpEntity}.
     */
    public static <T> HttpEntity<T> getJsonRequestEntity()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept( Lists.newArrayList( MediaType.APPLICATION_JSON ) );
        return new HttpEntity<T>( headers );
    }
}
