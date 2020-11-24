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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.actions.tracker.RelationshipActions;
import org.hisp.dhis.actions.tracker.TEIActions;
import org.hisp.dhis.actions.tracker_v2.TrackerActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.TestCleanUp;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerImporter_relationshipsTests
    extends ApiTest
{
    private static List<String> teis;

    private static List<String> events;

    private static final String relationshipType = "xLmPUYJX8Ks";

    private List<String> createdRelationships = new ArrayList<>();

    private TrackerActions trackerActions;

    private RelationshipActions relationshipActions;

    private RestApiActions relationshipTypeActions;

    private TEIActions teiActions;

    private MetadataActions metadataActions;

    private EventActions eventActions;

    private static Stream<Arguments> provideRelationshipData()
    {
        return Stream.of(
            Arguments.arguments( "HrS7b5Lis6E", "event", events.get( 0 ), "trackedEntity", teis.get( 0 ) ), //event to tei
            Arguments.arguments( "HrS7b5Lis6w", "trackedEntity", teis.get( 0 ), "event", events.get( 0 ) ), // tei to event
            Arguments.arguments( "HrS7b5Lis6P", "event", events.get( 0 ), "event", events.get( 1 ) ), // event to event
            Arguments.arguments( "xLmPUYJX8Ks", "trackedEntity", teis.get( 0 ), "trackedEntity",
                teis.get( 1 ) ) ); // tei to tei
    }

    @BeforeAll
    public void beforeAll()
        throws Exception
    {
        trackerActions = new TrackerActions();
        relationshipActions = new RelationshipActions();
        teiActions = new TEIActions();
        metadataActions = new MetadataActions();
        eventActions = new EventActions();
        relationshipTypeActions = new RestApiActions( "/relationshipTypes" );

        new LoginActions().loginAsSuperUser();

        metadataActions.importAndValidateMetadata( new File( "src/test/resources/tracker/relationshipTypes.json" ) );

        teis = trackerActions.postAndGetJobReport( new File( "src/test/resources/tracker/v2/teis/teis.json" ))
            .validateSuccessfulImport().extractImportedTeis();

        JsonObject eventObject = new FileReaderUtils().read( new File( "src/test/resources/tracker/v2/events/events.json" ) )
            .replacePropertyValuesWithIds( "event" ).get( JsonObject.class );

        events = trackerActions.postAndGetJobReport( eventObject )
            .validateSuccessfulImport().extractImportedEvents();
        //ApiResponse response = eventActions.post( eventObject );
        //response.validate().statusCode( 200 );
        //events = response.extractUids();

    }

    @ParameterizedTest
    @ValueSource( strings = {
        "src/test/resources/tracker/v2/teis/teisAndRelationship.json",
        "src/test/resources/tracker/v2/teis/teisWithRelationship.json"
    } )
    public void shouldImportObjectsWithRelationship( String file )
        throws Exception
    {
        JsonObject jsonObject = new FileReaderUtils().read( new File( file ) ).get( JsonObject.class );

        TrackerApiResponse
            response = trackerActions
            .postAndGetJobReport( jsonObject );

        response.prettyPrint();
        // TODO more validation when the bug is fixed
        createdRelationships = response.extractImportedRelationships();

        response.validateSuccessfulImport()
            .validate()
            .body( "stats.total", equalTo( 3 ) );

        relationshipActions.get( createdRelationships.get( 0 ) )
            .validate().statusCode( 200 )
            .body( "from.trackedEntityInstance", notNullValue() )
            .body( "to.trackedEntityInstance", notNullValue() );

        response.extractImportedTeis().forEach( tei -> {
            teiActions.get( tei, new QueryParamsBuilder().add( "fields=relationships" ) )
                .validate().statusCode( 200 )
                .body( "relationships.relationship", contains( createdRelationships.get( 0 ) ) );
        } );
    }

    @Test
    public void shouldDeleteRelationshipWithDeleteStrategy()
    {
        TrackerApiResponse response = trackerActions
            .postAndGetJobReport( new File( "src/test/resources/tracker/v2/teis/teisAndRelationship.json" ) );

        List<String> teis = response.extractImportedTeis();
        String relationship = response.extractImportedRelationships().get( 0 );

        JsonObject obj = new JsonObjectBuilder()
            .addObject( "from", JsonObjectBuilder.jsonObject().addProperty( "trackedEntity", teis.get( 0 ) ) )
            .addObject( "to", JsonObjectBuilder.jsonObject().addProperty( "trackedEntity", teis.get( 1 ) ) )
            .addProperty( "relationshipType", relationshipType )
            .addProperty( "relationship", relationship )
            .wrapIntoArray( "relationships" );

        response = trackerActions.postAndGetJobReport( obj, new QueryParamsBuilder().add( "importStrategy=DELETE" ) );
        response.validate().body( "status", equalTo( "OK" ) )
            .body( "stats.deleted", equalTo( 1 ) );

        relationshipActions.get( relationship )
            .validate()
            .statusCode( 404 );

        teiActions.get( teis.get( 0 ), new QueryParamsBuilder().add( "fields=relationships" ) )
            .validate()
            .body( "relationships", Matchers.empty() );
    }

    @Test
    public void shouldValidateRelationshipType()
    {
        JsonObject object = JsonObjectBuilder.jsonObject()
            .addProperty( "relationshipType", relationshipType )
            .addObject( "from", JsonObjectBuilder.jsonObject().addProperty( "event", events.get( 0 )) )
            .addObject( "to", JsonObjectBuilder.jsonObject().addProperty( "trackedEntity", teis.get( 0 )) )
            .wrapIntoArray( "relationships" );

        trackerActions.postAndGetJobReport( object )
            .validateErrorReport()
            .validate()
            .body( "validationReport.errorReports.message[0]", containsString( "constraint requires a trackedEntity but a event was found" ) );
    }

    @Test
    public void shouldValidateBothSidesOfRelationship()
    {
        JsonObject object = JsonObjectBuilder.jsonObject()
            .addProperty( "relationshipType", "xLmPUYJX8Ks" )
            .addObject( "from", JsonObjectBuilder.jsonObject().addProperty( "trackedEntity", "invalid-tei" ) )
            .addObject( "to", JsonObjectBuilder.jsonObject().addProperty( "trackedEntity", "more-invalid" ) )
            .wrapIntoArray( "relationships" );

        trackerActions.postAndGetJobReport( object )
            .validateErrorReport()
            .validate()
            .rootPath( "validationReport.errorReports" )
            .body( "message", hasSize( 2 ))
            .body( "message[0]", Matchers.both(containsString(  "Could not find `trackedEntity`" )).and(  containsString( "linked to Relationship" ) ) );

    }

    @MethodSource( "provideRelationshipData" )
    @ParameterizedTest( name = "{index} {1} to {3}" )
    public void shouldImportRelationshipsToExistingEntities( String relType, String fromInstance,
        String fromInstanceId, String toInstance, String toInstanceId )
    {
        // add relationship
        JsonObject relationship = JsonObjectBuilder.jsonObject()
            .addProperty( "relationshipType", relType )
            .addObject( "from", JsonObjectBuilder.jsonObject().addProperty( fromInstance, fromInstanceId ) )
            .addObject( "to", JsonObjectBuilder.jsonObject().addProperty( toInstance, toInstanceId ) )
            .wrapIntoArray( "relationships" );

        System.out.print( relationship );
        TrackerApiResponse response = trackerActions.postAndGetJobReport( relationship );

        //response.prettyPrint();
        response.validateSuccessfulImport();
        createdRelationships = response.extractImportedRelationships();

        ApiResponse response1 = relationshipActions.get( createdRelationships.get(0) );

        validateRelationship( response1, relType, fromInstance, fromInstanceId, toInstance, toInstanceId, createdRelationships.get( 0 ) );

        //response1.prettyPrint();
        ApiResponse entityResponse = getEntityInRelationship( toInstance, toInstanceId );

        //entityResponse.prettyPrint();
        validateRelationship( entityResponse, relType, fromInstance, fromInstanceId, toInstance, toInstanceId,
            createdRelationships.get( 0 ) );

        entityResponse = getEntityInRelationship( fromInstance, fromInstanceId );
        validateRelationship( entityResponse, relType, fromInstance, fromInstanceId, toInstance, toInstanceId,
            createdRelationships.get( 0 ) );
    }

    @Test
    public void shouldValidateRelationshipTypeTei()
        throws Exception
    {
        // create two teis with different relationship types
        JsonObject obj = new FileReaderUtils().read( new File( "src/test/resources/tracker/v2/teis/teis.json" ) ).replacePropertyValuesWithIds( "trackedEntity" ).get(JsonObject.class);

        JsonObject tei1 = obj.getAsJsonArray("trackedEntities").get( 0 ).getAsJsonObject();
        JsonObject tei2 = obj.getAsJsonArray("trackedEntities").get( 1 ).getAsJsonObject();

        //tei2.addProperty( "trackedEntityType", "pZLWzQaAQ9L" );
        JsonObject relationships = new JsonObjectBuilder()
            .addProperty( "relationshipType","FC5HloWtpK3" )
            .addObject( "from", new JsonObjectBuilder().addProperty( "trackedEntity",  tei2.get( "trackedEntity" ).getAsString() ) )
            .addObject( "to", new JsonObjectBuilder().addProperty( "trackedEntity",tei1.get( "trackedEntity" ).getAsString()) )
            .build();

        tei1.add( "relationships", new JsonArray() );
        tei1.getAsJsonArray("relationships").add( relationships );

        TrackerApiResponse apiResponse = trackerActions.postAndGetJobReport( obj );

        apiResponse.validateErrorReport();
    }

    private static Stream<Arguments> provideDuplicateRelationshipData() {
        return Stream.of(
            Arguments.of( teis.get( 0 ), teis.get(1), teis.get( 1 ), teis.get( 0 ), true, 1, "bi: reversed direction should import 1" ),
            Arguments.of( teis.get( 0 ), teis.get( 1 ), teis.get( 0 ), teis.get( 1 ), false, 1, "uni: same direction should import 1" ),
            Arguments.of( teis.get( 0 ), teis.get( 1 ), teis.get( 0 ), teis.get( 1 ), true, 1, "bi: same direction should import 1" ),
            Arguments.of( teis.get( 0 ), teis.get( 1 ), teis.get( 1 ), teis.get( 0 ), false, 2, "uni: reversed direction should import 2" )
        );
    }

    @MethodSource("provideDuplicateRelationshipData")
    @ParameterizedTest(name = "{index} {6}")
    public void shouldNotImportDuplicateRelationships(String fromTei1, String toTei1, String fromTei2, String toTei2, boolean bidirectional, int expectedCount, String representation ) {
        String relationshipTypeId = relationshipTypeActions.get("", new QueryParamsBuilder().addAll( "filter=fromConstraint.relationshipEntity:eq:TRACKED_ENTITY_INSTANCE", "filter=toConstraint.relationshipEntity:eq:TRACKED_ENTITY_INSTANCE", "filter=bidirectional:eq:" + bidirectional ))
            .extractString( "relationshipTypes.id[0]" );

        JsonObject relationship1 = JsonObjectBuilder.jsonObject()
            .addProperty( "relationshipType", relationshipTypeId )
            .addObject( "from", JsonObjectBuilder.jsonObject()
                .addProperty( "trackedEntity", fromTei1 ))
            .addObject( "to", JsonObjectBuilder.jsonObject()
                .addProperty( "trackedEntity", toTei1 ))
            .build();

        JsonObject relationship2 = JsonObjectBuilder.jsonObject()
            .addProperty( "relationshipType", relationshipTypeId )
            .addObject( "from", JsonObjectBuilder.jsonObject()
                .addProperty( "trackedEntity", fromTei2 ))
            .addObject( "to", JsonObjectBuilder.jsonObject()
                .addProperty( "trackedEntity", toTei2 ))
            .build();

        JsonObject array = JsonObjectBuilder.jsonObject()
            .addArray( "relationships", relationship1, relationship2 )
            .build();

        System.out.println(array);
        TrackerApiResponse response = trackerActions.postAndGetJobReport( array );

        createdRelationships = response.extractImportedRelationships();
        response.prettyPrint();
        response
            .validateSuccessfulImport()
            .validate().
            body("stats.created", equalTo( expectedCount ));


    }

    private ApiResponse getEntityInRelationship( String toOrFromInstance, String id )
    {
        switch ( toOrFromInstance )
        {
        case "trackedEntity":
        {
            return teiActions.get( id, new QueryParamsBuilder().add( "fields=relationships" ) );
        }

        case "event":
        {
            return eventActions.get( id, new QueryParamsBuilder().add( "fields=relationships" ) );
        }

        default:
        {
            return null;
        }
        }
    }

    private void validateRelationship( ApiResponse response, String relationshipTypeId, String fromInstance, String fromInstanceId,
        String toInstance, String toInstanceId, String relationshipId )
    {
        String bodyPrefix = "";
        if ( response.getBody().getAsJsonArray( "relationships" ) != null )
        {
            bodyPrefix = "relationships[0]";
        }

        if ( fromInstance.equalsIgnoreCase( "trackedEntity" ) )
        {
            fromInstance = "trackedEntityInstance";
        }
        if ( toInstance.equalsIgnoreCase( "trackedEntity" ) )
        {
            toInstance = "trackedEntityInstance";
        }

        response.validate()
            .statusCode( 200 )
            .body( bodyPrefix, notNullValue() )
            .rootPath( bodyPrefix )
            .body( "relationshipType", equalTo( relationshipTypeId ) )
            .body( "relationship", equalTo( relationshipId ) )
            .body( String.format( "from.%s.%s", fromInstance, fromInstance ),
                equalTo( fromInstanceId ) )
            .body( String.format( "to.%s.%s", toInstance, toInstance ), equalTo( toInstanceId ) );
    }

    @AfterEach
    public void cleanup()
    {
        createdRelationships.forEach( rel -> {
            new TestCleanUp().deleteEntity( "relationships", rel );
        } );
    }

}
