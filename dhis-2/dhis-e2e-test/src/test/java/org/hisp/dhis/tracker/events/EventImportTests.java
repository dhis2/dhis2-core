package org.hisp.dhis.tracker.events;



import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.SystemActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.ImportSummary;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventImportTests
    extends ApiTest
{
    List<String> createdEvents = new ArrayList<>();

    private EventActions eventActions;

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
        eventActions = new EventActions();
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
