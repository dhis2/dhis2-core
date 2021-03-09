package org.hisp.dhis.tracker.teis;



import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.actions.tracker.RelationshipActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.TestCleanUp;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class RelationshipsTest
    extends ApiTest
{
    private static List<String> teis;

    private static List<String> events;

    private RestApiActions trackedEntityInstanceActions;

    private MetadataActions metadataActions;

    private String createdRelationship;

    private RestApiActions eventActions;

    private RelationshipActions relationshipActions;

    private static Stream<Arguments> provideRelationshipData()
    {
        return Stream.of(
            Arguments.arguments( "HrS7b5Lis6E", "event", events.get( 0 ), "trackedEntityInstance", teis.get( 0 ) ), //event to tei
            Arguments.arguments( "HrS7b5Lis6w", "trackedEntityInstance", teis.get( 0 ), "event", events.get( 0 ) ), // tei to event
            Arguments.arguments( "HrS7b5Lis6P", "event", events.get( 0 ), "event", events.get( 1 ) ), // event to event
            Arguments.arguments( "xLmPUYJX8Ks", "trackedEntityInstance", teis.get( 0 ), "trackedEntityInstance",
                teis.get( 1 ) ) ); // tei to tei
    }

    @BeforeAll
    public void before()
        throws Exception
    {
        relationshipActions = new RelationshipActions();
        trackedEntityInstanceActions = new RestApiActions( "/trackedEntityInstances" );
        metadataActions = new MetadataActions();
        eventActions = new EventActions();

        new LoginActions().loginAsSuperUser();

        metadataActions.importAndValidateMetadata( new File( "src/test/resources/tracker/relationshipTypes.json" ) );

        JsonObject teiObject = new FileReaderUtils().read( new File( "src/test/resources/tracker/teis/teis.json" ) )
            .replacePropertyValuesWithIds( "trackedEntityInstance" ).get( JsonObject.class );

        teis = trackedEntityInstanceActions.post( teiObject ).extractUids();

        JsonObject eventObject = new FileReaderUtils().read( new File( "src/test/resources/tracker/events/events.json" ) )
            .replacePropertyValuesWithIds( "event" ).get( JsonObject.class );

        ApiResponse response = eventActions.post( eventObject );
        response.validate().statusCode( 200 );
        events = response.extractUids();
    }

    @Test
    public void duplicateRelationshipsShouldNotBeAdded()
    {
        // create a relationship
        JsonObject object = relationshipActions
            .createRelationshipBody( "xLmPUYJX8Ks", "trackedEntityInstance", teis.get( 0 ), "trackedEntityInstance",
                teis.get( 1 ) );

        ApiResponse response = relationshipActions.post( object );

        response.validate().statusCode( 200 );
        createdRelationship = response.extractUid();
        assertNotNull( createdRelationship, "First relationship was not created." );

        // create a second relationship
        response = relationshipActions.post( object );

        response.validate().statusCode( 409 )
            .body( "status", equalTo( "ERROR" ) )
            .body( "response.status", equalTo( "ERROR" ) )
            .body( "response.ignored", equalTo( 1 ) )
            .body( "response.total", equalTo( 1 ) )
            .rootPath( "response.importSummaries[0]" )
            .body( "status", equalTo( "ERROR" ) )
            .body( "description", Matchers.stringContainsInOrder( "Relationship", "already exist" ) )
            .body( "importCount.ignored", equalTo( 1 ) );
    }

    @MethodSource( "provideRelationshipData" )
    @ParameterizedTest( name = "{index} {1} to {3}" )
    public void bidirectionalRelationshipFromTrackedEntityInstanceToEventCanBeAdded( String relationshipType, String fromInstance,
        String fromInstanceId, String toInstance, String toInstanceId )
    {
        // add relationship
        JsonObject relationship = relationshipActions
            .createRelationshipBody( relationshipType, fromInstance, fromInstanceId, toInstance, toInstanceId );

        ApiResponse response = relationshipActions.post( relationship );

        response.validate().statusCode( 200 );

        createdRelationship = response.extractUid();

        assertNotNull( createdRelationship, "Relationship id was not returned" );
        assertEquals( 1, response.getSuccessfulImportSummaries().size(), "Relationship import was not successful" );

        // validate created on both sides
        response = getEntityInRelationship( toInstance, toInstanceId );
        validateRelationship( response, relationshipType, fromInstance, fromInstanceId, toInstance, toInstanceId );

        response = getEntityInRelationship( fromInstance, fromInstanceId );
        validateRelationship( response, relationshipType, fromInstance, fromInstanceId, toInstance, toInstanceId );
    }

    private ApiResponse getEntityInRelationship( String toOrFromInstance, String id )
    {
        switch ( toOrFromInstance )
        {
        case "trackedEntityInstance":
        {
            return trackedEntityInstanceActions.get( id, new QueryParamsBuilder().add( "fields=relationships" ) );
        }

        case "event":
        {
            return eventActions.get( id );
        }

        default:
        {
            return null;
        }
        }
    }

    private void validateRelationship( ApiResponse response, String relationshipTypeId, String fromInstance, String fromInstanceId,
        String toInstance, String toInstanceId )
    {
        response.validate()
            .statusCode( 200 )
            .body( "relationships", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .body( "relationships.relationshipType", hasItem( relationshipTypeId ) )
            .body( String.format( "relationships.from.%s.%s", fromInstance, fromInstance ),
                hasItem( Matchers.equalTo( fromInstanceId ) ) )
            .body( String.format( "relationships.to.%s.%s", toInstance, toInstance ), hasItem( equalTo( toInstanceId ) ) );
    }

    @AfterEach
    public void cleanup()
    {
        new TestCleanUp().deleteEntity( "relationships", createdRelationship );
    }
}
