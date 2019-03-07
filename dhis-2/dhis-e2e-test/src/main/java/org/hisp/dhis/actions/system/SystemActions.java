package org.hisp.dhis.actions.system;

import com.github.fge.jsonschema.core.report.LogLevel;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.ImportSummary;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class SystemActions extends RestApiActions
{
    private Logger logger = Logger.getLogger( SystemActions.class.getName() );

    public SystemActions( )
    {
        super( "/system" );
    }

    public ApiResponse waitUntilTaskCompleted(String taskType, String taskId) {
        logger.info( "Waiting until task " + taskType + " with id " + taskId + "is completed" );
        ApiResponse response = null;
        boolean completed = false;
        while ( !completed )
        {
            response = get( "/tasks/" + taskType + "/" + taskId );
            response.validate().statusCode( 200 );
            completed = response.extractList( "completed" ).contains( true );
        }

        return response;
    }

    public List<ImportSummary> getTaskSummaries(String taskType, String taskId) {
        return get( "/taskSummaries/" + taskType + "/" + taskId ).getImportSummaries();
    }

}
