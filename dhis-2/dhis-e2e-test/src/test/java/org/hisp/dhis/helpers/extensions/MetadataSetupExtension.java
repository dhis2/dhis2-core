/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.ConfigurationHelper;
import org.hisp.dhis.helpers.TestCleanUp;
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
            MetadataActions metadataActions = new MetadataActions();

            new LoginActions().loginAsDefaultUser();

            metadataActions.importMetadata( new File( "src/test/resources/setup/userGroups.json" ), "" );

            metadataActions.importMetadata( new File( "src/test/resources/setup/metadata.json" ), "" );
            metadataActions.importMetadata( new File( "src/test/resources/setup/metadata.json" ), "" );

            metadataActions.importMetadata( new File( "src/test/resources/setup/users.json" ), "" );


            createdData = TestRunStorage.getCreatedEntities();

            iterateCreatedData( id -> {
                TestRunStorage.removeEntity( createdData.get( id ), id );
            } );

            setupSuperuser();

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
        logger.info( "Setting up super user" );
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
