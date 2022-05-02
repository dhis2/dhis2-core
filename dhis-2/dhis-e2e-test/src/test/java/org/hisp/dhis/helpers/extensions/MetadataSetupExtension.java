/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.helpers.extensions;


import org.apache.logging.log4j.LogManager;
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
import org.apache.logging.log4j.Logger;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataSetupExtension
    implements BeforeAllCallback, ExtensionContext.Store.CloseableResource
{
    private static boolean started = false;

    private static Map<String, String> createdData = new LinkedHashMap<>();

    private static Logger logger = LogManager.getLogger( MetadataSetupExtension.class.getName() );

    @Override
    public void beforeAll( ExtensionContext context )
    {
        if ( !started )
        {
            started = true;
            logger.info( "Importing metadata for tests" );

            // The following line registers a callback hook when the root test
            // context is shut down
            context.getRoot().getStore( GLOBAL ).put( "MetadataSetupExtension", this );

            MetadataActions metadataActions = new MetadataActions();

            new LoginActions().loginAsDefaultUser();

            String[] files = {
                "src/test/resources/setup/userGroups.json",
                "src/test/resources/setup/metadata.json",
                // importing for the second time to make sure all sharing is set up correctly - there are bugs in metadata importer
                "src/test/resources/setup/metadata.json",
                "src/test/resources/setup/tracker_metadata.json",
                "src/test/resources/setup/userRoles.json",
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
        String[] users = {
            TestConfiguration.get().superUserUsername(),
            TestConfiguration.get().defaultUserUsername(),
            TestConfiguration.get().adminUserUsername()
        };

        String userGroupId = Constants.USER_GROUP_ID;

        for ( String user : users )
        {
            String userId = userActions.get( String.format(
                "?filter=username:eq:%s", user ) )
                .extractString( "users.id[0]" );

            if ( userId == null )
            {
                return;
            }
            userActions.addUserToUserGroup( userId, userGroupId );
            TestRunStorage.removeEntity( "users", userId );
        }
    }

    private void iterateCreatedData( Consumer<String> stringConsumer )
    {
        Iterator<String> iterator = createdData.keySet().iterator();

        while ( iterator.hasNext() )
        {
            String id = iterator.next();
            stringConsumer.accept( id );
        }
    }

    @Override
    public void close()
    {
        if ( TestConfiguration.get().shouldCleanUp() )
        {
            TestCleanUp testCleanUp = new TestCleanUp();

            iterateCreatedData( id -> {
                testCleanUp.deleteEntity( createdData.get( id ), id );
            } );
        }

    }
}
