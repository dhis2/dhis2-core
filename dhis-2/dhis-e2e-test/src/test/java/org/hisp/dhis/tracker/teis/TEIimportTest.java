package org.hisp.dhis.tracker.teis;



import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.actions.tracker.TEIActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TEIimportTest
    extends ApiTest
{
    JsonObject object;

    private TEIActions teiActions;

    private EventActions eventActions;

    private RestApiActions enrollmentActions;

    @BeforeAll
    public void before()
        throws Exception
    {
        teiActions = new TEIActions();
        eventActions = new EventActions();
        enrollmentActions = new RestApiActions( "/enrollments" );

        new LoginActions().loginAsSuperUser();
        object = new FileReaderUtils().read( new File( "src/test/resources/tracker/teis/teisWithEventsAndEnrollments.json" ) )
            .get( JsonObject.class );
        teiActions.post( object ).validate().statusCode( 200 );
    }

    @Test
    public void teisShouldBeUpdatedAndDeletedInBulk()
    {
        // arrange

        JsonArray teis = object.getAsJsonArray( "trackedEntityInstances" );

        JsonObject tei1event = teis.get( 0 ).getAsJsonObject()
            .getAsJsonArray( "enrollments" ).get( 0 ).getAsJsonObject()
            .getAsJsonArray( "events" )
            .get( 0 )
            .getAsJsonObject();

        JsonObject tei2enrollment = teis.get( 1 ).getAsJsonObject()
            .getAsJsonArray( "enrollments" ).get( 0 ).getAsJsonObject();

        tei1event.addProperty( "deleted", true );
        tei2enrollment.addProperty( "status", "COMPLETED" );

        // act
        ApiResponse response = teiActions.post( object, new QueryParamsBuilder().addAll( "strategy=SYNC" ) );

        // assert
        String eventId = response.validate()
            .statusCode( 200 )
            .body( "response", notNullValue() )
            .rootPath( "response" )
            .body( "updated", Matchers.greaterThanOrEqualTo( 2 ) )
            .appendRootPath( "importSummaries[0]" )
            .body( "importCount.updated", greaterThanOrEqualTo( 1 ) )
            .appendRootPath( "enrollments.importSummaries[0].events.importSummaries[0]" )
            .body(
                "status", Matchers.equalTo( "SUCCESS" ),
                "reference", notNullValue(),
                "importCount.deleted", Matchers.equalTo( 1 ),
                "description", Matchers.stringContainsInOrder( "Deletion of event", "was successful" )
            )
            .extract().path( "response.importSummaries[0].enrollments.importSummaries[0].events.importSummaries[0].reference" );

        String enrollmentId = response.validate()
            .rootPath( "response.importSummaries[1].enrollments.importSummaries[0]" )
            .body(
                "status", Matchers.equalTo( "SUCCESS" ),
                "reference", notNullValue(),
                "importCount.updated", Matchers.equalTo( 1 )
            ).extract().path( "response.importSummaries[1].enrollments.importSummaries[0].reference" );

        // check if updates on event and enrollment were done.

        response = enrollmentActions.get( enrollmentId );

        response.validate().statusCode( 200 )
            .body( "status", Matchers.equalTo( "COMPLETED" ) );

        response = eventActions.get( eventId );

        response.validate().statusCode( 404 );

    }
}
