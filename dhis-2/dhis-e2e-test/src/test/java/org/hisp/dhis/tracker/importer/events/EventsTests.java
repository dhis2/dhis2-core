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
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.metadata.ProgramStageActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.TestCleanUp;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hisp.dhis.helpers.matchers.MatchesJson.matchesJSON;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventsTests
    extends TrackerNtiApiTest
{
    private static Stream<Arguments> provideEventFilesTestArguments()
    {
        return Stream.of(
            Arguments.arguments( "event.json", ContentType.JSON.toString() ) );
    }

    @BeforeAll
    public void beforeAll()
    {
        loginActions.loginAsSuperUser();
    }

    @Test
    public void shouldImportEvents()
        throws Exception
    {
        JsonObject eventBody = new FileReaderUtils()
            .readJsonAndGenerateData( new File( "src/test/resources/tracker/importer/events/event.json" ) );

        TrackerApiResponse importResponse = trackerActions.postAndGetJobReport( eventBody );

        importResponse.validateSuccessfulImport()
            .validateEvents()
            .body( "stats.created", Matchers.equalTo( 1 ) )
            .body( "objectReports", notNullValue() )
            .body( "objectReports[0].errorReports", empty() );

        // assert that the TEI was imported
        String eventId = importResponse.extractImportedEvents().get( 0 );

        ApiResponse response = trackerActions.get( "/events/" + eventId );

        response.validate().statusCode( 200 );

        assertThat( response.getBody(), matchesJSON( eventBody.getAsJsonArray( "events" ).get( 0 ) ) );
    }

    @Disabled( "disabled until csv is supported" )
    @ParameterizedTest
    @MethodSource( "provideEventFilesTestArguments" )
    public void eventsImportNewEventsFromFile( String fileName, String contentType )
        throws Exception
    {
        Object obj = new FileReaderUtils().read( new File( "src/test/resources/tracker/importer/events/" + fileName ) )
            .replacePropertyValuesWithIds( "event" )
            .get();

        ApiResponse response = trackerActions
            .post( "", contentType, obj, new QueryParamsBuilder()
                .addAll( "dryRun=false", "eventIdScheme=UID", "orgUnitIdScheme=UID", "skipFirst=true" ) );
        response
            .validate()
            .statusCode( 200 );

        String jobId = response.extractString( "response.id" );

        trackerActions.waitUntilJobIsCompleted( jobId );

        response = trackerActions.getJobReport( jobId, "FULL" );

        response.validate()
            .statusCode( 200 )
            .body( "status", equalTo( "OK" ) );
    }

    @ParameterizedTest
    @ValueSource( strings = { "true", "false" } )
    public void shouldImportToRepeatableStage( Boolean repeatableStage )
        throws Exception
    {
        //arrange
        String program = Constants.TRACKER_PROGRAM_ID;
        String programStage = new ProgramStageActions().get( "",
            new QueryParamsBuilder().addAll( "filter=program.id:eq:" + program, "filter=repeatable:eq:" + repeatableStage ) )
            .extractString( "programStages.id[0]" );

        TrackerApiResponse response = importTeiWithEnrollment( program, programStage );
        String teiId = response.extractImportedTeis().get( 0 );
        String enrollmentId = response.extractImportedEnrollments().get( 0 );

        JsonObject event = trackerActions.buildEvent( Constants.ORG_UNIT_IDS[0], program, programStage )
            .getAsJsonArray( "events" ).get( 0 ).getAsJsonObject();

        event.addProperty( "trackedEntity", teiId );
        event.addProperty( "enrollment", enrollmentId );

        JsonObject payload = new JsonObjectBuilder().addArray( "events", event, event ).build();

        // act
        response = trackerActions.postAndGetJobReport( payload );

        // assert
        if ( repeatableStage )
        {
            response
                .validateSuccessfulImport()
                .validate().body( "stats.created", equalTo( 2 ) );
        }

        else
        {
            response.validateErrorReport()
                .body( "errorCode", hasItem( "E1039" ) );
        }
    }

    @Test
    public void shouldAddEventsToExistingTei()
        throws Exception
    {
        String programId = Constants.TRACKER_PROGRAM_ID;
        String programStageId = "nlXNK4b7LVr";

        TrackerApiResponse response = importTeiWithEnrollment( programId, programStageId );

        String enrollmentId = response.extractImportedEnrollments().get( 0 );

        JsonObject event = new JsonObjectBuilder(
            trackerActions.buildEvent( Constants.ORG_UNIT_IDS[1], programId, programStageId ).getAsJsonArray(
                "events"
            ).get( 0 ).getAsJsonObject() )
            .addProperty( "enrollment", enrollmentId )
            .wrapIntoArray( "events" );

        response = trackerActions.postAndGetJobReport( event )
            .validateSuccessfulImport();

        String eventId = response.extractImportedEvents().get( 0 );

        trackerActions
            .get( "/events/" + eventId + "?fields=*" )
            .validate().statusCode( 200 )
            .body( "enrollment", equalTo( enrollmentId ) );
    }

    @AfterEach
    public void afterEach()
    {
        new TestCleanUp().deleteCreatedEntities( "/events" );
    }
}
