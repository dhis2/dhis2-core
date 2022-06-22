package org.hisp.dhis.analytics.event;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.hisp.dhis.dto.ApiResponse;

/**
 * Helper class to assist during the validation/assertion in e2e tests for
 * Events.
 * 
 * @author maikel arabori
 */
public class EventValidationHelper
{

    private EventValidationHelper()
    {
    }

    /**
     * Validate/assert all attributes of the given header (represented by the
     * index), matching each argument with its respective header attribute
     * value.
     * 
     * @param response
     * @param index the header
     * @param name
     * @param column
     * @param valueType
     * @param type
     * @param hidden
     * @param meta
     */
    public static void validateHeader( final ApiResponse response, final int index, final String name,
        final String column, final String valueType, final String type, final boolean hidden, final boolean meta )
    {
        response.validate()
            .body( "headers[" + index + "].name", equalTo( name ) )
            .body( "headers[" + index + "].column", equalTo( column ) )
            .body( "headers[" + index + "].valueType", equalTo( valueType ) )
            .body( "headers[" + index + "].type", equalTo( type ) )
            .body( "headers[" + index + "].hidden", is( hidden ) )
            .body( "headers[" + index + "].meta", is( meta ) );
    }
}
