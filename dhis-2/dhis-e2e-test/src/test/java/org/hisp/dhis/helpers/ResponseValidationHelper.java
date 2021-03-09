

package org.hisp.dhis.helpers;

import org.hisp.dhis.dto.ApiResponse;

import static org.hamcrest.CoreMatchers.isA;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ResponseValidationHelper
{
    public static void validateObjectRemoval( ApiResponse response, String message )
    {
        response.validate().statusCode( 200 );
        validateObjectUpdateResponse( response );
    }

    public static void validateObjectCreation( ApiResponse response )
    {
        response.validate().statusCode( 201 );
        validateObjectUpdateResponse( response );
    }

    public static void validateObjectUpdate( ApiResponse response, int statusCode )
    {
        response.validate().statusCode( statusCode );
        validateObjectUpdateResponse( response );
    }

    // TODO integrate with OPEN API 3 when it´s ready
    private static void validateObjectUpdateResponse( ApiResponse response )
    {
        response.validate()
            .body( "response", isA( Object.class ) )
            .body( "status", isA( String.class ) )
            .body( "httpStatusCode", isA( Integer.class ) )
            .body( "httpStatus", isA( String.class ) )
            .body( "response.responseType", isA( String.class ) )
            .body( "response.klass", isA( String.class ) )
            .body( "response.uid", isA( String.class ) );
    }
}
