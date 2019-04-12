package org.hisp.dhis.actions.metadata;

import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;

import java.io.File;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataActions extends RestApiActions
{
    public MetadataActions(  )
    {
        super( "/metadata" );
    }

    public ApiResponse importMetadata( File file) {
        ApiResponse response = postFile( file, "?importReportMode=FULL&atomicMode=NONE" );

        response.validate().statusCode( 200 );

        return response;
    }
}
