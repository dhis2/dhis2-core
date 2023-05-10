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
package org.hisp.dhis.tracker.events;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.tracker.DeprecatedTrackerApiTest;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class UserAssignmentFilterTests
    extends DeprecatedTrackerApiTest
{
    private MetadataActions metadataActions;

    private UserActions userActions;

    private String userPassword = Constants.USER_PASSWORD;

    private String userUsername;

    private String programId = "BJ42SUrAvHo";

    private String orgUnit = "O6uvpzGd5pu";

    private String userId;

    private Object eventsBody;

    @BeforeAll
    public void beforeAll()
        throws Exception
    {
        metadataActions = new MetadataActions();
        userActions = new UserActions();

        userUsername = ("EventFiltersUser" + DataGenerator.randomString()).toLowerCase();

        loginActions.loginAsSuperUser();
        metadataActions.importAndValidateMetadata( new File( "src/test/resources/tracker/eventProgram.json" ) );

        userId = userActions.addUser( userUsername, userPassword );
        userActions.grantUserAccessToOrgUnit( userId, orgUnit );
        userActions.addUserToUserGroup( userId, Constants.USER_GROUP_ID );
        userActions.addRoleToUser( userId, Constants.USER_ROLE_ID );

        eventsBody = getEventsBody( programId, "l8oDIfJJhtg", userId );
    }

    @BeforeEach
    public void beforeEach()
        throws Exception
    {
        createEvents( eventsBody );
    }

    @Test
    public void eventsShouldBeFilteredByAssignedUser()
        throws Exception
    {
        loginActions.loginAsSuperUser();
        ApiResponse response = eventActions
            .get( "?program=" + programId + "&assignedUser=" + userId + "&ouMode=ALL" );

        response.validate().statusCode( 200 )
            .body( "events", hasSize( 4 ) )
            .body( "events.assignedUser", everyItem( equalTo( userId ) ) );
    }

    @Test
    public void eventsShouldBeFilteredForAssignedUser()
    {
        // arrange
        loginActions.loginAsUser( userUsername, userPassword );

        // act
        ApiResponse response = eventActions.get( "?orgUnit=" + orgUnit + "&assignedUserMode=CURRENT" );

        // assert
        response.validate().statusCode( 200 )
            .body( "events", hasSize( 4 ) )
            .body( "events.assignedUser", everyItem( equalTo( userId ) ) );
    }

    @Test
    public void eventsShouldBeFilteredByUnassigned()
    {
        // arrange
        loginActions.loginAsUser( userUsername, userPassword );

        String eventId = eventActions.get( "?orgUnit=" + orgUnit + "&assignedUserMode=CURRENT" )
            .extractString( "events.event[0]" );

        assertNotNull( eventId, "Event was not found" );
        unassignEvent( eventId );

        // act
        ApiResponse currentUserEvents = eventActions.get( "?orgUnit=" + orgUnit + "&assignedUserMode=CURRENT" );
        ApiResponse unassignedEvents = eventActions.get( "?orgUnit=" + orgUnit + "&assignedUserMode=NONE" );

        // assert
        currentUserEvents.validate().statusCode( 200 )
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

        String eventId = eventActions.get( "?orgUnit=" + orgUnit + "&assignedUserMode=CURRENT" )
            .extractString( "events.event[0]" );
        assertNotNull( eventId, "Event was not found" );

        String status = "SCHEDULE";
        changeEventStatus( eventId, status );

        // act
        ApiResponse filteredEvents = eventActions
            .get( "?orgUnit=" + orgUnit + "&assignedUserMode=CURRENT&status=" + status );
        ApiResponse activeEvents = eventActions
            .get( "?orgUnit=" + orgUnit + "&assignedUserMode=CURRENT&status=ACTIVE" );

        // assert
        filteredEvents.validate().statusCode( 200 )
            .body( "events", hasSize( 1 ) )
            .body( "events.assignedUser", everyItem( equalTo( userId ) ) )
            .body( "events.status", everyItem( equalTo( status ) ) );

        activeEvents.validate().statusCode( 200 )
            .body( "events", hasSize( 3 ) )
            .body( "events.assignedUser", everyItem( equalTo( userId ) ) )
            .body( "events.status", everyItem( equalTo( "ACTIVE" ) ) );
    }

    private ApiResponse createEvents( Object body )
        throws Exception
    {
        ApiResponse eventResponse = eventActions.post( body, new QueryParamsBuilder().add( "skipCache=true" ) );

        eventResponse.validate().statusCode( 200 );

        return eventResponse;
    }

    private Object getEventsBody( String programId, String programStageId, String assignedUserId )
        throws Exception
    {
        Object body = new FileReaderUtils().read( new File( "src/test/resources/tracker/events/events.json" ) )
            .replacePropertyValuesWithIds( "event" )
            .replacePropertyValuesWith( "program", programId )
            .replacePropertyValuesWith( "programStage", programStageId )
            .replacePropertyValuesWith( "assignedUser", assignedUserId )
            .get();

        return body;
    }

    private ApiResponse unassignEvent( String eventId )
    {
        JsonObject body = eventActions.get( eventId ).getBody();

        body.addProperty( "assignedUser", "" );

        ApiResponse response = eventActions.update( eventId, body );

        response.validate().statusCode( 200 );
        return response;
    }

    private ApiResponse changeEventStatus( String eventId, String eventStatus )
    {
        JsonObject body = eventActions.get( eventId ).getBody();

        body.addProperty( "status", eventStatus );

        ApiResponse response = eventActions.update( eventId, body );

        response.validate().statusCode( 200 );

        return response;

    }
}
