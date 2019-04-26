package org.hisp.dhis.actions.metadata;

import org.apache.commons.lang3.StringUtils;
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

    public ApiResponse importMetadata( File file, String queryParams )
    {
        queryParams = !StringUtils.isEmpty( queryParams ) ? queryParams : "";
        ApiResponse response = postFile( file, "?importReportMode=FULL&atomicMode=NONE" + queryParams );

        response.validate().statusCode( 200 );

        return response;
    }
}
