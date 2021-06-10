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
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.Program;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Every.everyItem;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class UserAssignmentFilterTests
    extends TrackerApiTest
{
    private EventActions eventActions;

    private UserActions userActions;

    private String userPassword = "Test1212?";

    private String userUsername;

    private String programId;

    private String programStageId;

    private String orgUnit = Constants.ORG_UNIT_IDS[2];

    private String userId;

    private List<String> createdEvents = new ArrayList<>();

    @BeforeAll
    public void beforeAll()
        throws Exception
    {
        eventActions = new EventActions();
        loginActions = new LoginActions();
        userActions = new UserActions();

        userUsername = "EventFiltersUser" + DataGenerator.randomString();

        loginActions.loginAsSuperUser();

        setupData();
    }

    private void setupData()
    {
        Program program = programActions.createEventProgram(orgUnit);
        programId = program.getId();
        programStageId = program.getStages().get( 0 );
        programActions.programStageActions.enableUserAssignment( programStageId, true );

        userId = userActions.addUser( userUsername, userPassword );
        userActions.grantUserAccessToOrgUnit( userId, orgUnit );
        userActions.addUserToUserGroup( userId, Constants.USER_GROUP_ID );
        userActions.addRoleToUser( userId, Constants.USER_ROLE_ID );

        IntStream.rangeClosed( 0, 4 ).forEach( p -> {
            String id = createEvent( userId, "ACTIVE" );
            createdEvents.add( id );
        } );
    }

    @Test
    public void eventsShouldBeFilteredByAssignedUser()
    {
        loginActions.loginAsSuperUser();

        ApiResponse response = eventActions.get( "?program=" + programId + "&assignedUser=" + userId );

        response.validate().statusCode( 200 )
            .body( "events.assignedUser", everyItem( equalTo( userId ) ) );
    }

    @Test
    public void eventsShouldBeFilteredForAssignedUser()
    {
        // arrange
        loginActions.loginAsUser( userUsername, userPassword );

        // act
        ApiResponse response = eventActions.get( "?program=" + programId + "&assignedUserMode=CURRENT" );

        // assert
        response.validate().statusCode( 200 )
            .body( "events.assignedUser", everyItem( equalTo( userId ) ) );
    }

    @Test
    public void eventsShouldBeFilteredByUnassigned()
    {
        // arrange

        String eventId = createEvent( "", "ACTIVE" );

        loginActions.loginAsUser( userUsername, userPassword );

        // act
        ApiResponse currentUserEvents = eventActions.get( "?program=" + programId + "&assignedUserMode=CURRENT" );
        ApiResponse unassignedEvents = eventActions.get( "?program=" + programId + "&assignedUserMode=NONE" );

        // assert
        currentUserEvents.validate()
            .statusCode( 200 )
            .body( "events", notNullValue() )
            .body( "events.event", not( hasItem( eventId ) ) );

        unassignedEvents.validate().statusCode( 200 )
            .body( "events", notNullValue() )
            .body( "events.event", hasItem( eventId ) )
            .body( "events.assignedUser", everyItem( is( emptyOrNullString() ) ) );
    }

    @Test
    public void eventsShouldBeFilteredForAssignedUserByStatus()
    {
        // arrange
        loginActions.loginAsUser( userUsername, userPassword );
        String status = "SCHEDULE";
        String eventId = createEvent( userId, status );

        // act
        ApiResponse filteredEvents = eventActions.get( "?program=" + programId + "&assignedUserMode=CURRENT&status=" + status );
        ApiResponse activeEvents = eventActions.get( "?program=" + programId + "&assignedUserMode=CURRENT&status=ACTIVE" );

        // assert
        filteredEvents.validate().statusCode( 200 )
            .body( "events.assignedUser", everyItem( equalTo( userId ) ) )
            .body( "events.event", contains( eventId ) )
            .body( "events.status", everyItem( equalTo( status ) ) );

        activeEvents.validate().statusCode( 200 )
            .body( "events.assignedUser", everyItem( equalTo( userId ) ) )
            .body( "events.status", everyItem( equalTo( "ACTIVE" ) ) );
    }

    private String createEvent( String assignedUser, String status )
    {
        JsonObject obj = JsonObjectBuilder.jsonObject( eventActions.createEventBody( orgUnit, programId, programStageId ) )
            .addProperty( "assignedUser", assignedUser )
            .addProperty( "status", status )
            .build();

        ApiResponse response = eventActions.post( obj, new QueryParamsBuilder().add( "skipCache=true" ) );

        response.validate().statusCode( 200 );
        return response.extractUid();
    }

    @AfterAll
    public void afterAll()
    {
        createdEvents.forEach( p -> {
            eventActions.delete( p );
        } );
    }

}
