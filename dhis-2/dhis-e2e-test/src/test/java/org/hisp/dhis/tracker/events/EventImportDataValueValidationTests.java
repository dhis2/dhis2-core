

package org.hisp.dhis.tracker.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.metadata.SharingActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.utils.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventImportDataValueValidationTests
    extends ApiTest
{
    private ProgramActions programActions;

    private EventActions eventActions;

    private RestApiActions dataElementActions;

    private SharingActions sharingActions;

    private static String OU_ID = Constants.ORG_UNIT_IDS[0];

    private String programId;

    private String programStageId;

    private String mandatoryDataElementId;

    @BeforeAll
    public void beforeAll()
    {
        programActions = new ProgramActions();
        eventActions = new EventActions();
        dataElementActions = new RestApiActions( "/dataElements" );
        sharingActions = new SharingActions();

        new LoginActions().loginAsAdmin();

        setupData();
    }

    @Test
    public void shouldNotValidateDataValuesOnUpdateWithOnCompleteStrategy()
    {
        setValidationStrategy( programStageId, "ON_COMPLETE" );

        JsonObject events = eventActions.createEventBody( OU_ID, programId, programStageId );

        ApiResponse response = eventActions.post( events, new QueryParamsBuilder().add( "skipCache=true" ) );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "OK" ) )
            .body( "response.ignored", equalTo( 0 ) )
            .body( "response.imported", equalTo( 1 ) );
    }

    @Test
    public void shouldValidateDataValuesOnCompleteWhenEventIsCompleted() {
        setValidationStrategy( programStageId, "ON_COMPLETE" );

        JsonObject event = eventActions.createEventBody( OU_ID, programId, programStageId );
        event.addProperty( "status", "COMPLETED" );

        ApiResponse response = eventActions.post( event, new QueryParamsBuilder().add( "skipCache=true" ) );

        response.validate().statusCode( 409 )
            .body( "status", equalTo( "ERROR" ) )
            .rootPath( "response" )
            .body( "ignored", equalTo( 1 ) )
            .body( "imported", equalTo( 0 ) )
            .body( "importSummaries[0].conflicts[0].value", equalTo( "value_required_but_not_provided" ) );
    }

    @Test
    public void shouldValidateDataValuesOnUpdate() {
        setValidationStrategy( programStageId, "ON_UPDATE_AND_INSERT" );

        JsonObject events = eventActions.createEventBody( OU_ID, programId, programStageId );

        ApiResponse response = eventActions.post( events, new QueryParamsBuilder().add( "skipCache=true" ) );

        response.validate().statusCode( 409 )
            .body( "status", equalTo( "ERROR" ) )
            .rootPath( "response" )
            .body( "ignored", equalTo( 1 ) )
            .body( "imported", equalTo( 0 ) )
            .body( "importSummaries[0].conflicts[0].value", equalTo( "value_required_but_not_provided" ) );
    }

    @Test
    public void shouldImportEventsWithCompulsoryDataValues()
    {
        JsonObject events = eventActions.createEventBody( OU_ID, programId, programStageId );

        addDataValue( events, mandatoryDataElementId, "TEXT VALUE" );

        ApiResponse response = eventActions.post( events );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "OK" ) )
            .body( "response.imported", equalTo( 1 ) );

        String eventID = response.extractString( "response.importSummaries.reference[0]" );
        assertNotNull( eventID, "Failed to extract eventId" );

        eventActions.get( eventID )
            .validate()
            .statusCode( 200 )
            .body( "dataValues", not( Matchers.emptyArray() ) );
    }

    private void setupData()
    {
        ApiResponse response = programActions.createEventProgram( OU_ID );
        programId = response.extractUid();
        assertNotNull( programId, "Failed to create a program" );

        sharingActions.setupSharingForConfiguredUserGroup( "program", programId );

        programStageId = programActions.get( programId, new QueryParamsBuilder().add( "fields=*" ) )
            .extractString( "programStages.id[0]" );
        assertNotNull( programStageId, "Failed to create a programStage" );

        String dataElementId = dataElementActions
            .get( "?fields=id&filter=domainType:eq:TRACKER&filter=valueType:eq:TEXT&pageSize=1" )
            .extractString( "dataElements.id[0]" );

        assertNotNull( dataElementId, "Failed to find data elements" );
        mandatoryDataElementId = dataElementId;

        programActions.addDataElement( programStageId, dataElementId, true ).validate().statusCode( 200 );
    }

    private void addDataValue( JsonObject body, String dataElementId, String value )
    {
        JsonArray dataValues = new JsonArray();

        JsonObject dataValue = new JsonObject();

        dataValue.addProperty( "dataElement", dataElementId );
        dataValue.addProperty( "value", value );

        dataValues.add( dataValue );
        body.add( "dataValues", dataValues );
    }

    private void setValidationStrategy(String programStageId, String strategy) {
        JsonObject body = JsonObjectBuilder.jsonObject()
            .addProperty( "validationStrategy", strategy)
            .build();

        programActions.programStageActions.patch( programStageId, body )
            .validate().statusCode( 204 );

        programActions.programStageActions.get( programStageId )
            .validate().body( "validationStrategy", equalTo( strategy ) );

    }

}
