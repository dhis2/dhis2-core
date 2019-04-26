package org.hisp.dhis.helpers.extensions;

import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.helpers.ConfigurationHelper;
import org.hisp.dhis.helpers.TestCleanUp;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataSetupExtension
    implements BeforeAllCallback, ExtensionContext.Store.CloseableResource
{
    private static boolean started = false;

    private static LinkedHashMap<String, String> createdData = new LinkedHashMap();

    @Override
    public void beforeAll( ExtensionContext context )
    {
        if ( !started )
        {
            started = true;
            new LoginActions().loginAsSuperUser();
            new MetadataActions().importMetadata( new File( "src/test/resources/setup/userGroups.json" ), "" );
            new MetadataActions().importMetadata( new File( "src/test/resources/setup/users.json" ), "" );
            new MetadataActions().importMetadata( new File( "src/test/resources/setup/metadata.json" ), "" );
            setupSuperuser();

            createdData = TestRunStorage.getCreatedEntities();

            iterateCreatedData( id -> {
                TestRunStorage.removeEntity( createdData.get( id ), id );
            } );

            // The following line registers a callback hook when the root test context is shut down
            context.getRoot().getStore( GLOBAL ).put( "MetadataSetupExtension", this );
        }
    }

    @Override
    public void close()
    {
        TestCleanUp testCleanUp = new TestCleanUp();

        iterateCreatedData( id -> {
            testCleanUp.deleteEntity( createdData.get( id ), id );
        } );

    }

    private void setupSuperuser( ) {
        UserActions userActions = new UserActions();
        String userRoleId = "yrB6vc5Ip7r";
        String userGroupId= "OPVIvvXzNTw";

        String userId = userActions.get( "?username=" + ConfigurationHelper.SUPER_USER_USERNAME ).extractString("users.id[0]");

        userActions.addUserToUserGroup( userId , userGroupId);
        userActions.addURoleToUser( userId, userRoleId );

        TestRunStorage.removeEntity( "/users", userId);
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
}
