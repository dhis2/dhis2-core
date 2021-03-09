package org.hisp.dhis.aggregate;



import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.SystemActions;
import org.hisp.dhis.actions.aggregate.DataValueActions;
import org.hisp.dhis.actions.aggregate.DataValueSetActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.ImportSummary;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.JsonFileReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class DataImportTest
    extends ApiTest
{
    private DataValueSetActions dataValueSetActions;

    private MetadataActions metadataActions;

    private DataValueActions dataValueActions;

    private SystemActions systemActions;

    @BeforeAll
    public void before()
    {
        dataValueSetActions = new DataValueSetActions();
        metadataActions = new MetadataActions();
        dataValueActions = new DataValueActions();
        systemActions = new SystemActions();

        new LoginActions().loginAsSuperUser();
        metadataActions.importMetadata( new File( "src/test/resources/aggregate/metadata.json" ), "async=false" ).validate()
            .statusCode( 200 );
    }

    @Test
    public void dataValuesCanBeImportedInBulk()
    {
        ApiResponse response = dataValueSetActions
            .postFile( new File( "src/test/resources/aggregate/dataValues_bulk.json" ),
                new QueryParamsBuilder().add( "importReportMode=FULL" ) );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "SUCCESS" ) )
            .body( "conflicts", empty() )
            .body( "importCount", notNullValue() )
            .rootPath( "importCount" )
            .body( "ignored", not( greaterThan( 0 ) ) )
            .body( "deleted", not( greaterThan( 0 ) ) );

        ImportSummary importSummary = response.getImportSummaries().get( 0 );
        assertThat( response.getAsString(),
            importSummary.getImportCount().getImported() + importSummary.getImportCount().getUpdated(), greaterThan( 0 ) );

    }

    @Test
    public void dataValuesCanBeImportedAsync()
    {
        ApiResponse response = dataValueSetActions
            .postFile( new File( "src/test/resources/aggregate/dataValues_bulk.json" ),
                new QueryParamsBuilder().addAll( "reportMode=DEBUG", "async=true" ) );

        response.validate().statusCode( 200 );

        String taskId = response.extractString( "response.id" );

        // Validate that job was successful

        response = systemActions.waitUntilTaskCompleted( "DATAVALUE_IMPORT", taskId );

        assertThat( response.extractList( "message" ), hasItems(
            containsString( "Process started" ),
            containsString( "Importing data values" ),
            containsString( "Import done" ) ) );

        // validate task summaries were created
        response = systemActions.getTaskSummariesResponse( "DATAVALUE_IMPORT", taskId );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "SUCCESS" ) )
            .rootPath( "importCount" )
            .body( "deleted", equalTo( 0 ) )
            .body( "ignored", equalTo( 0 ) );

        ImportSummary importSummary = response.getImportSummaries().get( 0 );
        assertThat( response.getAsString(),
            importSummary.getImportCount().getImported() + importSummary.getImportCount().getUpdated(), greaterThan( 0 ) );
    }

    @Test
    public void dataValuesCanBeImportedForSingleDataSet()
        throws IOException
    {
        String orgUnit = "O6uvpzGd5pu";
        String period = "201911";
        String dataSet = "VEM58nY22sO";

        JsonObject importedPayload = new JsonFileReader( new File( "src/test/resources/aggregate/dataValues_single_dataset.json" ) )
            .get();
        ApiResponse response = dataValueSetActions.post( importedPayload );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "SUCCESS" ) )
            .body( "conflicts", empty() )
            .body( "importCount", notNullValue() )
            .rootPath( "importCount" )
            .body( "ignored", not( greaterThan( 0 ) ) )
            .body( "deleted", not( greaterThan( 0 ) ) );

        ImportSummary importSummary = response.getImportSummaries().get( 0 );

        assertThat( importSummary, notNullValue() );
        assertThat( response.getAsString(),
            importSummary.getImportCount().getImported() + importSummary.getImportCount().getUpdated(), greaterThanOrEqualTo( 2 ) );

        response = dataValueSetActions.get( String.format( "?orgUnit=%s&period=%s&dataSet=%s", orgUnit, period, dataSet ) );

        response.validate()
            .body( "dataSet", equalTo( dataSet ) )
            .body( "period", equalTo( period ) )
            .body( "orgUnit", equalTo( orgUnit ) )
            .body( "dataValues", hasSize( greaterThanOrEqualTo( 2 ) ) );

        JsonArray dataValues = response.getBody().get( "dataValues" ).getAsJsonArray();

        for ( JsonElement j : dataValues
        )
        {
            JsonObject object = j.getAsJsonObject();

            response = dataValueActions.get( String
                .format( "?ou=%s&pe=%s&de=%s&co=%s", orgUnit, period, object.get( "dataElement" ).getAsString(),
                    object.get( "categoryOptionCombo" ).getAsString() ) );

            response.validate()
                .statusCode( 200 )
                .body( containsString( object.get( "value" ).getAsString() ) );
        }
    }

    @AfterAll
    public void cleanUp()
    {
        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder.addAll( "importReportMode=FULL", "importStrategy=DELETE"  );

        ApiResponse response = dataValueSetActions.postFile( new File( "src/test/resources/aggregate/dataValues_bulk.json" ),
            queryParamsBuilder );
        response.validate().statusCode( 200 );

        response = dataValueSetActions.postFile( new File( "src/test/resources/aggregate/dataValues_single_dataset.json" ),
            queryParamsBuilder );
        response.validate().statusCode( 200 );
    }
}
