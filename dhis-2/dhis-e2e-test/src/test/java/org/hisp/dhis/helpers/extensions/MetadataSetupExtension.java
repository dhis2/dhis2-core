package org.hisp.dhis.helpers.extensions;



import org.hisp.dhis.Constants;
import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.helpers.TestCleanUp;
import org.hisp.dhis.helpers.config.TestConfiguration;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataSetupExtension
        implements BeforeAllCallback, ExtensionContext.Store.CloseableResource
{
    private static boolean started = false;

    private static Map<String, String> createdData = new LinkedHashMap();

    private static Logger logger = Logger.getLogger( MetadataSetupExtension.class.getName() );

    @Override
    public void beforeAll( ExtensionContext context )
    {
        if ( !started )
        {
            started = true;
            logger.info( "Importing metadata for tests" );

            // The following line registers a callback hook when the root test context is shut down
            context.getRoot().getStore( GLOBAL ).put( "MetadataSetupExtension", this );

            MetadataActions metadataActions = new MetadataActions();

            new LoginActions().loginAsDefaultUser();

            String[] files = {
                "src/test/resources/setup/userGroups.json",
                "src/test/resources/setup/metadata.json",
                "src/test/resources/setup/metadata.json",
                "src/test/resources/setup/userRoles.json",
                "src/test/resources/setup/users.json",
                "src/test/resources/setup/users.json"
            };

            String queryParams = "async=false";
            for ( String fileName : files )
            {
                metadataActions.importAndValidateMetadata( new File( fileName ), queryParams );
            }

            setupUsers();

            createdData.putAll( TestRunStorage.getCreatedEntities() );
            TestRunStorage.removeAllEntities();

        }
    }

    private void setupUsers()
    {
        logger.info( "Adding users to the TA user group" );
        UserActions userActions = new UserActions();
        String[] users =  {
            TestConfiguration.get().superUserUsername(),
            TestConfiguration.get().defaultUserUsername(),
            TestConfiguration.get().adminUserUsername()
        };

        String userGroupId = Constants.USER_GROUP_ID;


        for ( String user : users )
        {
            String userId = userActions.get( String.format(
                "?filter=userCredentials.username:eq:%s", user ))
                .extractString( "users.id[0]" );

            userActions.addUserToUserGroup( userId, userGroupId );
            TestRunStorage.removeEntity( "users", userId );
        }

    }

    private void iterateCreatedData( Consumer<String> stringConsumer )
    {
        Iterator iterator = createdData.keySet().iterator();

        while ( iterator.hasNext() )
        {
            String id = (String) iterator.next();
            stringConsumer.accept( id );
        }
    }

    @Override
    public void close()
            throws Throwable
    {
        TestCleanUp testCleanUp = new TestCleanUp();

        iterateCreatedData( id -> {
            testCleanUp.deleteEntity( createdData.get( id ), id );
        } );

    }
}
