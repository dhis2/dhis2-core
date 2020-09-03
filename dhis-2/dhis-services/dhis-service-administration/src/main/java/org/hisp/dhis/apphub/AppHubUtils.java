package org.hisp.dhis.apphub;

import java.util.List;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.google.common.collect.Lists;

public class AppHubUtils
{
    /**
     * Validates the query segment. Checks whether the query is null or
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

        final List<String> illegalStrings = Lists.newArrayList( "..", "http", "https", "//" );

        if ( illegalStrings.stream().anyMatch( query::contains ) )
        {
            throw new IllegalQueryException( new ErrorMessage( ErrorCode.E1001 ) );
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
     * Returns an {@link HttpEntity} with {@link HttpHeaders} set to
     * Accept a JSON response.
     *
     * @return a {@link HttpEntity}.
     */
    public static <T> HttpEntity<T> getJsonRequestEntity()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.add( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE );
        return new HttpEntity<T>( headers );
    }
}
