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

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.SystemActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.ImportSummary;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.restassured.http.ContentType;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventImportTests
    extends TrackerApiTest
{
    List<String> createdEvents = new ArrayList<>();

    private SystemActions systemActions;

    private static Stream<Arguments> provideEventFilesTestArguments()
    {
        return Stream.of(
            Arguments.arguments( "events.json", ContentType.JSON.toString() ),
            Arguments.arguments( "events.csv", "text/csv" ),
            Arguments.arguments( "events.xml", ContentType.XML.toString() ) );
    }

    @BeforeAll
    public void before()
    {
        systemActions = new SystemActions();

        new LoginActions().loginAsSuperUser();
    }

    @ParameterizedTest
    @MethodSource( "provideEventFilesTestArguments" )
    public void eventsImportNewEventsFromFile( String fileName, String contentType )
        throws Exception
    {
        Object obj = new FileReaderUtils().read( new File( "src/test/resources/tracker/events/" + fileName ) )
            .replacePropertyValuesWithIds( "event" )
            .get();

        ApiResponse response = eventActions
            .post( "", contentType, obj, new QueryParamsBuilder()
                .addAll( "dryRun=false", "eventIdScheme=UID", "orgUnitIdScheme=UID", "skipFirst=true", "async=true" ) );

        response
            .validate()
            .statusCode( 200 );

        String taskId = response.extractString( "response.id" );
        assertNotNull( taskId, "Task id was not returned" );

        systemActions.waitUntilTaskCompleted( "EVENT_IMPORT", taskId );

        List<ImportSummary> importSummaries = systemActions.getTaskSummaries( "EVENT_IMPORT", taskId );

        assertThat( "Wrong import summaries size", importSummaries.size(), Matchers.greaterThan( 0 ) );

        createdEvents.addAll( importSummaries
            .stream()
            .map( ImportSummary::getReference )
            .collect( toList() ) );

        assertThat( importSummaries, Matchers.everyItem( hasProperty( "status", Matchers.equalTo( "SUCCESS" ) ) ) );
    }

    @Test
    public void eventsImportDeletedEventShouldFail()
    {
        ApiResponse response = post( "events.json", false );

        response.validate().statusCode( 200 );

        createdEvents = response.getImportSummaries()
            .stream()
            .map( p -> {
                return p.getReference();
            } )
            .collect( toList() );

        assertThat( "Expected 4 events created", createdEvents, hasSize( 4 ) );
        eventActions.softDelete( createdEvents );

        response = post( "events.json", true );

        String taskId = response.extractString( "response.id" );
        assertNotNull( taskId, "Task id was not returned" );

        systemActions.waitUntilTaskCompleted( "EVENT_IMPORT", taskId );

        List<ImportSummary> importSummaryList = systemActions.getTaskSummaries( "EVENT_IMPORT", taskId );

        assertThat( importSummaryList, Matchers.everyItem( hasProperty( "status", Matchers.equalTo( "ERROR" ) ) ) );
        assertThat( importSummaryList,
            everyItem( hasProperty( "description", Matchers.containsString( "This event can not be modified." ) ) ) );
    }

    private ApiResponse post( String fileName, boolean async )
    {
        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder
            .addAll( "dryRun=false", "eventIdScheme=UID", "orgUnitIdScheme=UID", "async=" + String.valueOf( async ) );

        ApiResponse response = eventActions
            .postFile( new File( "src/test/resources/tracker/events/" + fileName ), queryParamsBuilder );
        return response;
    }

    @AfterEach
    public void after()
    {
        createdEvents.forEach( event -> {
            TestRunStorage.addCreatedEntity( "events", event );
        } );
    }
}
