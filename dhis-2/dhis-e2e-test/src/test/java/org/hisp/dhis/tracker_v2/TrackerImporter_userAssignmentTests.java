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

package org.hisp.dhis.tracker_v2;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.actions.tracker_v2.TrackerActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerImporter_userAssignmentTests extends ApiTest
{
    private LoginActions loginActions;
    private ProgramActions programActions;
    private MetadataActions metadataActions;
    private TrackerActions trackerActions;
    private EventActions eventActions;

    private String programId = "BJ42SUrAvHo";
    private static String programStageId = "l8oDIfJJhtg";

    @BeforeAll
    public void beforeAll() {
        loginActions = new LoginActions();
        programActions = new ProgramActions();
        metadataActions = new MetadataActions();
        trackerActions = new TrackerActions();
        eventActions = new EventActions();

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
        String eventId =  createEvents( programId, programStageId, loggedInUser )
            .extractImportedEvents().get( 0 );

        // assert
        assertNotNull( eventId );

        ApiResponse response = eventActions.get( eventId );
        if ( !Boolean.parseBoolean( userAssignmentEnabled ) )
        {
            assertNull( response.getBody().get( "assignedUser" ) );
        }

        assertEquals( loggedInUser, response.getBody().get( "assignedUser" ).getAsString() );
    }

    // todo should be finalised when exporter is ready
    /*@Test
    public void shouldRemoveUserAssignment()
        throws Exception
    {
        // arrange
        String loggedInUser = loginActions.getLoggedInUserId();

        programActions.programStageActions.enableUserAssignment( programStageId, true );
        createEvents( programId, programStageId, loggedInUser );

        JsonObject body = eventActions.get( "?program=" + programId + "&assignedUserMode=CURRENT" )
            .extractJsonObject( "events[0]" );

        assertNotNull( body, "no events matching the query." );

        String eventId = body.get( "event" ).getAsString();

        // act
        body.add( "assignedUser", null );

        ApiResponse eventResponse = eventActions.update( eventId, body );

        // assert
        eventResponse.validate().statusCode( 200 );

        eventResponse = eventActions.get( eventId );

        assertEquals( null, eventResponse.getBody().get( "assignedUser" ) );
    }*/

    private TrackerApiResponse createEvents( String programId, String programStageId, String assignedUserId )
        throws Exception
    {
        JsonObject body = new FileReaderUtils().read( new File( "src/test/resources/tracker/v2/events/event.json" ) )
            .replacePropertyValuesWithIds( "event" )
            .replacePropertyValuesWith( "program", programId )
            .replacePropertyValuesWith( "programStage", programStageId )
            .replacePropertyValuesWith( "assignedUser", assignedUserId )
            .get(JsonObject.class);

        TrackerApiResponse eventResponse = trackerActions.postAndGetJobReport( body );

        eventResponse.validateSuccessfulImport();

        return eventResponse;
    }


}
