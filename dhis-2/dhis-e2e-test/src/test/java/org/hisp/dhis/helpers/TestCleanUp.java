package org.hisp.dhis.helpers;



import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.MaintenanceActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TestCleanUp
{
    private Logger logger = Logger.getLogger( TestCleanUp.class.getName() );

    private int deleteCount = 0;

    /**
     * Deletes entities created during test run. Entities deleted one by one starting from last created one.
     */
    public void deleteCreatedEntities()
    {
        Map<String, String> createdEntities = TestRunStorage.getCreatedEntities();
        List<String> reverseOrderedKeys = new ArrayList<>( createdEntities.keySet() );
        Collections.reverse( reverseOrderedKeys );

        Iterator iterator = reverseOrderedKeys.iterator();

        while ( iterator.hasNext() )
        {
            String key = (String) iterator.next();
            boolean deleted = deleteEntity( createdEntities.get( key ), key );
            if ( deleted )
            {
                TestRunStorage.removeEntity( createdEntities.get( key ), key );
                createdEntities.remove( createdEntities.get( key ), key );
            }

            new MaintenanceActions().removeSoftDeletedMetadata();
        }

        while ( deleteCount < 2 && !createdEntities.isEmpty() )
        {
            deleteCount++;
            deleteCreatedEntities();
        }

        TestRunStorage.removeAllEntities();
    }

    /**
     * Deletes entities created during test run.
     *
     * @param resources I.E /organisationUnits to delete created OU's.
     */
    public void deleteCreatedEntities( String... resources )
    {
        new LoginActions().loginAsSuperUser();

        for ( String resource : resources
        )
        {
            List<String> entityIds = TestRunStorage.getCreatedEntities( resource );

            Iterator iterator = entityIds.iterator();

            while ( iterator.hasNext() )
            {
                boolean deleted = deleteEntity( resource, (String) iterator.next() );
                if ( deleted )
                {
                    iterator.remove();
                }
            }
        }

    }

    public void deleteCreatedEntities( LinkedHashMap<String, String> entitiesToDelete )
    {
        Iterator iterator = entitiesToDelete.keySet().iterator();
        while ( iterator.hasNext() )
        {
            String key = (String) iterator.next();

            deleteEntity( entitiesToDelete.get( key ), key );

        }
    }

    public boolean deleteEntity( String resource, String id )
    {
        ApiResponse response = new RestApiActions( resource ).delete( id + "?force=true" );

        if ( response.statusCode() == 200 || response.statusCode() == 404 )
        {
            logger.info( String.format( "Entity from resource %s with id %s deleted", resource, id ) );

            if ( response.containsImportSummaries() )
            {
                return response.extract( "response.importCount.deleted" ).equals( 1 );
            }

            return true;
        }

        logger.warning( String
            .format( "Entity from resource %s with id %s was not deleted. Status code: %s", resource, id, response.statusCode() ) );
        return false;
    }

}
