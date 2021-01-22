/*
 * Copyright (c) 2004-2020, University of Oslo
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

package org.hisp.dhis.tracker.importer.events;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class UserAssignmentTests
    extends ApiTest
{
    private static final String programStageId = "l8oDIfJJhtg";

    private static final String programId = "BJ42SUrAvHo";

    private LoginActions loginActions;

    private ProgramActions programActions;

    private MetadataActions metadataActions;

    private TrackerActions trackerActions;

    @BeforeAll
    public void beforeAll()
    {
        loginActions = new LoginActions();
        programActions = new ProgramActions();
        metadataActions = new MetadataActions();
        trackerActions = new TrackerActions();

        loginActions.loginAsSuperUser();
        metadataActions.importAndValidateMetadata( new File( "src/test/resources/tracker/eventProgram.json" ) );

    }

    @ParameterizedTest
    @ValueSource( strings = { "true", "false" } )
    public void shouldImportEventWithUserAssignment( String userAssignmentEnabled )
        throws Exception
    {
        // arrange
        String loggedInUser = loginActions.getLoggedInUserId();

        programActions.programStageActions.enableUserAssignment( programStageId, Boolean.parseBoolean( userAssignmentEnabled ) );

        // act
        String eventId = createEvents( programId, programStageId, loggedInUser )
            .extractImportedEvents().get( 0 );

        ApiResponse response = trackerActions.get( "/events/" + eventId );
        if ( !Boolean.parseBoolean( userAssignmentEnabled ) )
        {
            response.validate()
                .body( "assignedUser", nullValue() );

            return;
        }

        response.validate()
            .body( "assignedUser", equalTo( loggedInUser ) );
    }

    @Disabled
    @Test
    public void shouldRemoveUserAssignment()
        throws Exception
    {
        // arrange
        String loggedInUser = loginActions.getLoggedInUserId();

        programActions.programStageActions.enableUserAssignment( programStageId, true );
        createEvents( programId, programStageId, loggedInUser );

        JsonObject eventBody = trackerActions.get( "/events?program=" + programId + "&assignedUserMode=CURRENT" )
            .extractJsonObject( "instances[0]" );

        assertNotNull( eventBody, "no events matching the query." );
        String eventId = eventBody.get( "event" ).getAsString();

        // act
        eventBody.add( "assignedUser", null );

        ApiResponse response = trackerActions.postAndGetJobReport( new JsonObjectBuilder( eventBody ).wrapIntoArray( "events" ),
            new QueryParamsBuilder().addAll( "importStrategy=UPDATE" ) );

        // assert
        response.validate().statusCode( 200 );

        trackerActions.get( "/events/" + eventId )
            .validate()
            .body( "assignedUser", nullValue() );

    }

    private TrackerApiResponse createEvents( String programId, String programStageId, String assignedUserId )
        throws Exception
    {
        JsonObject body = new FileReaderUtils().read( new File( "src/test/resources/tracker/importer/events/event.json" ) )
            .replacePropertyValuesWithIds( "event" )
            .replacePropertyValuesWith( "program", programId )
            .replacePropertyValuesWith( "programStage", programStageId )
            .replacePropertyValuesWith( "assignedUser", assignedUserId )
            .get( JsonObject.class );

        TrackerApiResponse eventResponse = trackerActions.postAndGetJobReport( body );

        eventResponse.validateSuccessfulImport();

        return eventResponse;
    }

}
