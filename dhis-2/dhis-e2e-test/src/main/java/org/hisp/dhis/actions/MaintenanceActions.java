package org.hisp.dhis.actions;



import com.google.gson.JsonObject;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;

import java.util.logging.Logger;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MaintenanceActions
    extends RestApiActions
{
    private Logger logger = Logger.getLogger( MaintenanceActions.class.getName() );

    public MaintenanceActions()
    {
        super( "/maintenance" );
    }

    public void removeSoftDeletedEvents()
    {
        sendRequest( true, "softDeletedEventRemoval=true" );
    }

    public void removeSoftDeletedMetadata()
    {
        sendRequest( true, "softDeletedEventRemoval=true", "softDeletedTrackedEntityInstanceRemoval=true",
            "softDeletedProgramStageInstanceRemoval=true", "softDeletedProgramInstanceRemoval=true",
            "softDeletedDataValueRemoval=true" );
    }

    private void sendRequest( boolean validate, String... queryParams )
    {
        ApiResponse apiResponse = super.post( new JsonObject(), new QueryParamsBuilder().addAll( queryParams ) );

        if ( validate )
        {
            apiResponse.validate().statusCode( 204 );
            return;
        }

        if ( apiResponse.statusCode() != 204 )
        {
            logger.warning( String
                .format( "Maintenance failed with query params %s. Response: %s", queryParams, apiResponse.getBody().toString() ) );
        }
    }
}
