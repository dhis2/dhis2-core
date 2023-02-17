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
package org.hisp.dhis.tracker.importer.events;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hisp.dhis.helpers.matchers.MatchesJson.matchesJSON;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import joptsimple.internal.Strings;

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
import org.hisp.dhis.tracker.importer.databuilder.EventDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.JsonObject;
import io.restassured.http.ContentType;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventsTests
    extends TrackerNtiApiTest
{
    private static final String OU_ID_0 = Constants.ORG_UNIT_IDS[0];

    private static final String OU_ID = Constants.ORG_UNIT_IDS[1];

    private static final String OU_ID_2 = Constants.ORG_UNIT_IDS[2];

    private static Stream<Arguments> provideEventFilesTestArguments()
    {
        return Stream.of(
            Arguments.arguments( "event.json", ContentType.JSON.toString() ),
            Arguments.arguments( "event.csv", "text/csv" ) );
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
            .readJsonAndGenerateData( new File( "src/test/resources/tracker/importer/events/events.json" ) );

        TrackerApiResponse importResponse = trackerActions.postAndGetJobReport( eventBody );

        importResponse.validateSuccessfulImport()
            .validateEvents()
            .body( "stats.created", Matchers.equalTo( 4 ) )
            .body( "objectReports", notNullValue() )
            .body( "objectReports[0].errorReports", empty() );

        eventBody.getAsJsonArray( "events" ).forEach( event -> {
            trackerActions.getEvent( event.getAsJsonObject().get( "event" ).getAsString() )
                .validate().statusCode( 200 )
                .body( "", matchesJSON( event ) );
        } );

    }

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
                .addAll( "dryRun=false", "eventIdScheme=UID", "orgUnitIdScheme=UID" ) );
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
        // arrange
        String program = Constants.TRACKER_PROGRAM_ID;
        String programStage = new ProgramStageActions().get( "",
            new QueryParamsBuilder().addAll( "filter=program.id:eq:" + program,
                "filter=repeatable:eq:" + repeatableStage ) )
            .extractString( "programStages.id[0]" );

        TrackerApiResponse response = importTeiWithEnrollment( program );
        String teiId = response.extractImportedTeis().get( 0 );
        String enrollmentId = response.extractImportedEnrollments().get( 0 );

        JsonObject event = new EventDataBuilder()
            .setEnrollment( enrollmentId )
            .setTei( teiId )
            .array( OU_ID, program, programStage ).getAsJsonArray( "events" ).get( 0 ).getAsJsonObject();

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
    public void shouldImportAndGetEventWithOrgUnitDifferentFromEnrollmentOrgUnit()
        throws Exception
    {
        String programId = Constants.TRACKER_PROGRAM_ID;
        String programStageId = "nlXNK4b7LVr";

        TrackerApiResponse response = importTeiWithEnrollment( programId );

        String enrollmentId = response.extractImportedEnrollments().get( 0 );

        JsonObject event = new EventDataBuilder()
            .setEnrollment( enrollmentId )
            .array( OU_ID_0, programId, programStageId );

        response = trackerActions.postAndGetJobReport( event )
            .validateSuccessfulImport();

        String eventId = response.extractImportedEvents().get( 0 );

        trackerActions
            .get( "/enrollments/" + enrollmentId )
            .validate().statusCode( 200 )
            .body( "orgUnit", equalTo( OU_ID ) );

        trackerActions
            .get( "/events/" + eventId + "?fields=*" )
            .validate().statusCode( 200 )
            .body( "orgUnit", equalTo( OU_ID_0 ) );

        QueryParamsBuilder builder = new QueryParamsBuilder()
            .add( "ouMode", "DESCENDANTS" )
            .add( "orgUnit", OU_ID_2 )
            .add( "program", programId );

        eventActions.get( builder.build() )
            .validate().statusCode( 200 )
            .body( "events", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .body( "events[0].orgUnit", equalTo( OU_ID_0 ) );
    }

    @Test
    public void shouldAddEventsToExistingTei()
        throws Exception
    {
        String programId = Constants.TRACKER_PROGRAM_ID;
        String programStageId = "nlXNK4b7LVr";

        TrackerApiResponse response = importTeiWithEnrollment( programId );

        String enrollmentId = response.extractImportedEnrollments().get( 0 );

        JsonObject event = new EventDataBuilder()
            .setEnrollment( enrollmentId )
            .array( OU_ID, programId, programStageId );

        response = trackerActions.postAndGetJobReport( event )
            .validateSuccessfulImport();

        String eventId = response.extractImportedEvents().get( 0 );

        trackerActions
            .get( "/events/" + eventId + "?fields=*" )
            .validate().statusCode( 200 )
            .body( "enrollment", equalTo( enrollmentId ) );
    }

    @Test
    public void shouldImportWithCategoryCombo()
    {
        ApiResponse program = programActions.get( "", new QueryParamsBuilder().add( "programType=WITHOUT_REGISTRATION" )
            .add( "filter=categoryCombo.code:!eq:default" )
            .add( "filter=name:like:TA" )
            .add( "fields=id,categoryCombo[categories[categoryOptions]]" ) );

        String programId = program.extractString( "programs.id[0]" );
        List<String> category = program
            .extractList( "programs[0].categoryCombo.categories.categoryOptions.id.flatten()" );

        Assumptions.assumeFalse( Strings.isNullOrEmpty( programId ) );

        JsonObject object = new EventDataBuilder()
            .setProgram( programId )
            .setAttributeCategoryOptions( category )
            .setOu( OU_ID ).array();

        trackerActions.postAndGetJobReport( object )
            .validateSuccessfulImport();
    }

    @AfterEach
    public void afterEach()
    {
        new TestCleanUp().deleteCreatedEntities( "/events" );
    }
}
