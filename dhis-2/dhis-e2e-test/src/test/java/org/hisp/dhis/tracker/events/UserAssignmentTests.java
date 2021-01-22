/*
 * Copyright (c) 2004-2021, University of Oslo
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

package org.hisp.dhis.tracker.events;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class UserAssignmentTests
    extends ApiTest
{
    private MetadataActions metadataActions;

    private LoginActions loginActions;

    private ProgramActions programActions;

    private EventActions eventActions;

    private String userAssignmentProperty = "enableUserAssignment";

    @BeforeAll
    public void beforeAll()
    {
        metadataActions = new MetadataActions();
        programActions = new ProgramActions();
        eventActions = new EventActions();
        loginActions = new LoginActions();

        loginActions.loginAsSuperUser();
        metadataActions.importAndValidateMetadata( new File( "src/test/resources/tracker/eventProgram.json" ) );
    }

    @ParameterizedTest
    @ValueSource( strings = { "WITHOUT_REGISTRATION", "WITH_REGISTRATION" } )
    public void shouldBeEnabledOnProgramStage( String programType )
    {
        // arrange
        String programId = programActions.get( "?filter=programStages:ge:1&filter=programType:eq:" + programType )
            .extractString( "programs.id[0]" );

        String programStageId = programActions.get( programId ).extractString( "programStages.id[0]" );

        // act - enabling user assignment
        ApiResponse response = programActions.programStageActions.enableUserAssignment( programStageId, true );

        // assert
        ResponseValidationHelper.validateObjectUpdate( response, 200 );

        response = programActions.programStageActions.get( programStageId );

        response.validate()
            .statusCode( 200 )
            .body( userAssignmentProperty, equalTo( true ) );

        // act - disabling user assignment
        response = programActions.programStageActions.enableUserAssignment( programStageId, false );

        // assert
        ResponseValidationHelper.validateObjectUpdate( response, 200 );

        response = programActions.programStageActions.get( programStageId );

        response.validate().statusCode( 200 )
            .body( userAssignmentProperty, equalTo( false ) );
    }

    @ParameterizedTest
    @ValueSource( strings = { "true", "false" } )
    public void eventImportWithUserAssignmentShouldSucceed( String userAssignmentEnabled )
        throws Exception
    {
        String programStageId = "l8oDIfJJhtg";
        String programId = "BJ42SUrAvHo";
        String loggedInUser = loginActions.getLoggedInUserId();

        programActions.programStageActions.enableUserAssignment( programStageId, Boolean.parseBoolean( userAssignmentEnabled ) );

        ApiResponse eventResponse = createEvents( programId, programStageId, loggedInUser );

        assertNotNull( eventResponse.getImportSummaries(), "No import summaries returned when creating event." );
        eventResponse.getImportSummaries().forEach( importSummary -> {
            ApiResponse response = eventActions.get( importSummary.getReference() );

            if ( !Boolean.parseBoolean( userAssignmentEnabled ) )
            {
                assertNull( response.getBody().get( "assignedUser" ) );
                return;
            }

            assertEquals( loggedInUser, response.getBody().get( "assignedUser" ).getAsString() );
        } );
    }

    @Test
    public void eventUserAssignmentShouldBeRemoved()
        throws Exception
    {
        // arrange
        String programStageId = "l8oDIfJJhtg";
        String programId = "BJ42SUrAvHo";
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
    }

    private ApiResponse createEvents( String programId, String programStageId, String assignedUserId )
        throws Exception
    {
        Object file = new FileReaderUtils().read( new File( "src/test/resources/tracker/events/events.json" ) )
            .replacePropertyValuesWithIds( "event" )
            .replacePropertyValuesWith( "program", programId )
            .replacePropertyValuesWith( "programStage", programStageId )
            .replacePropertyValuesWith( "assignedUser", assignedUserId )
            .get();

        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder.add( "skipCache=true" );

        ApiResponse eventResponse = eventActions.post( file, queryParamsBuilder );

        eventResponse.validate().statusCode( 200 );

        return eventResponse;
    }
}
