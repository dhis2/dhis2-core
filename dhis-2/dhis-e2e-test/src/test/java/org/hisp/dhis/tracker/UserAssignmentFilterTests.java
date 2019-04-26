package org.hisp.dhis.tracker;

import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class UserAssignmentFilterTests
    extends ApiTest
{
    private LoginActions loginActions;

    private EventActions eventActions;

    private MetadataActions metadataActions;

    private UserActions userActions;

    private String userPassword = "Test1212?";

    private String userUsername;

    private String programId = "BJ42SUrAvHo";
    private String orgUnit = "r16njpPwUCb";

    private String userId;

    private Object eventsBody;

    @BeforeAll
    public void beforeAll()
        throws Exception
    {
        eventActions = new EventActions();
        loginActions = new LoginActions();
        metadataActions = new MetadataActions();
        userActions = new UserActions();

        userUsername = "EventFiltersUser" + DataGenerator.randomString();

        loginActions.loginAsSuperUser();
        metadataActions.importMetadata( new File( "src/test/resources/tracker/eventProgram.json" ), "" );

        userId = userActions.addUser( userUsername, userPassword );
        userActions.grantUserAccessToOrgUnit( userId, orgUnit );
        userActions.addUserToUserGroup( userId, "OPVIvvXzNTw" );
        userActions.addURoleToUser( userId, "yrB6vc5Ip7r" );
        
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
        ApiResponse response = eventActions.get( "?program=" + programId + "&assignedUser=" + userId );

        response.validate().statusCode( 200 );
        assertEquals( 4, response.extractList( "events" ).size() );
        assertThat( response.extractList( "events.assignedUser" ), everyItem( Matchers.equalTo( userId ) ) );
    }

    @Test
    public void eventsShouldBeFilteredForAssignedUser()
    {
        // arrange
        loginActions.loginAsUser( userUsername, userPassword );

        // act
        ApiResponse response = eventActions.get( "?orgUnit=" +orgUnit + "&assignedUserMode=CURRENT" );

        // assert
        response.validate().statusCode( 200 );

        assertEquals( 4, response.extractList( "events" ).size() );
        assertThat( response.extractList( "events.assignedUser" ), everyItem( Matchers.equalTo( userId ) ) );
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
        currentUserEvents.validate().statusCode( 200 );
        assertThat( currentUserEvents.extractList( "events.event" ), Matchers.not(  Matchers.hasItem( eventId ) ));

        unassignedEvents.validate().statusCode( 200 );

        assertThat( unassignedEvents.extractList( "events.event" ),  Matchers.hasItem( eventId ) ) ;
        assertThat( unassignedEvents.extractList( "events.assignedUser" ), everyItem( Matchers.isEmptyOrNullString(  ) ) );


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
        ApiResponse filteredEvents = eventActions.get( "?orgUnit=" + orgUnit + "&assignedUserMode=CURRENT&status=" + status );
        ApiResponse activeEvents = eventActions.get( "?orgUnit=" + orgUnit + "&assignedUserMode=CURRENT&status=ACTIVE" );

        // assert
        filteredEvents.validate().statusCode( 200 );
        assertEquals( 1, filteredEvents.extractList( "events" ).size() );
        assertThat( filteredEvents.extractList( "events.assignedUser" ), everyItem( Matchers.equalTo( userId ) ) );
        assertThat( filteredEvents.extractList( "events.status" ), everyItem( Matchers.equalTo( status ) ) );

        activeEvents.validate().statusCode( 200 );
        assertEquals( 3, activeEvents.extractList( "events" ).size() );
        assertThat( activeEvents.extractList( "events.assignedUser" ), everyItem( Matchers.equalTo( userId ) ) );
        assertThat( activeEvents.extractList( "events.status" ), everyItem( Matchers.equalTo( "ACTIVE" ) ) );

    }

    private ApiResponse createEvents( Object body )
        throws Exception
    {
        ApiResponse eventResponse = eventActions.post( body );

        eventResponse.validate().statusCode( 200 );

        return eventResponse;
    }

    private Object getEventsBody( String programId, String programStageId, String assignedUserId )
        throws Exception
    {

        Object body = new FileReaderUtils().read( new File( "src/test/resources/events/events.json" ) )
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
