

package org.hisp.dhis.tracker.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.AttributeActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.OrgUnit;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.xml.crypto.Data;
import java.io.File;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventImportIdSchemeTests extends ApiTest
{
    private OrgUnitActions orgUnitActions;

    private ProgramActions programActions;

    private EventActions eventActions;

    private AttributeActions attributeActions;

    private static String OU_NAME = "TA EventsImportIdSchemeTests ou name " + DataGenerator.randomString();
    private static String OU_CODE = "TA EventsImportIdSchemeTests ou code " + DataGenerator.randomString();
    private static String ATTRIBUTE_VALUE = "TA EventsImportIdSchemeTests attribute " + DataGenerator.randomString() ;
    private static String PROGRAM_ID = Constants.EVENT_PROGRAM_ID;

    private String orgUnitId;
    private static String ATTRIBUTE_ID;



    @BeforeAll
    public void beforeAll() {
        orgUnitActions = new OrgUnitActions();
        eventActions = new EventActions();
        programActions = new ProgramActions();
        attributeActions = new AttributeActions();

        new LoginActions().loginAsSuperUser();

        setupData();
    }

    private static Stream<Arguments> provideIdSchemeArguments()
    {
        return Stream.of(
            Arguments.arguments( "CODE", "code" ),
            Arguments.arguments( "NAME", "name" ),
            Arguments.arguments( "UID", "id" ),
            Arguments.arguments( "ATTRIBUTE:" + ATTRIBUTE_ID, "attributeValues.value[0]" )
        );
    }


    @ParameterizedTest
    @MethodSource( "provideIdSchemeArguments" )
    public void eventsShouldBeImportedWithOrgUnitScheme(String ouScheme, String ouProperty)
        throws Exception
    {
        String ouPropertyValue = orgUnitActions.get( orgUnitId ).extractString( ouProperty );

        assertNotNull(ouPropertyValue, String.format(  "Org unit property %s was not present.", ouProperty));

        JsonObject object = new FileReaderUtils().read(  new File( "src/test/resources/tracker/events/events.json" ) )
            .replacePropertyValuesWith( "orgUnit", ouPropertyValue)
            .replacePropertyValuesWithIds( "event" )
            .get( JsonObject.class );

        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder()
            .add( "skipCache=true" )
            .add( "orgUnitIdScheme=" + ouScheme );

        ApiResponse response = eventActions.post( object, queryParamsBuilder );

        response.validate().statusCode( 200 )
            .rootPath( "response" )
            .body( "status",  equalTo("SUCCESS") )
            .body( "ignored", equalTo( 0 ) )
            .body( "imported", greaterThan(1) )
            .body( "importSummaries.reference", everyItem( notNullValue() ) );

        String eventId = response.extractString( "response.importSummaries.reference[0]" );
        assertNotNull( eventId );

        eventActions.get( eventId ).validate()
            .statusCode( 200 )
            .body( "orgUnit", equalTo( orgUnitId ) );
    }

    @ParameterizedTest
    @MethodSource( "provideIdSchemeArguments" )
    public void eventsShouldBeImportedWithProgramScheme(String scheme, String property)
        throws Exception
    {
        String programPropertyValue = programActions.get( PROGRAM_ID ).extractString( property);

        assertNotNull(programPropertyValue, String.format(  "Program property %s was not present.", property));

        JsonObject object = new FileReaderUtils().read(  new File( "src/test/resources/tracker/events/events.json" ) )
            .replacePropertyValuesWithIds( "event" )
            .replacePropertyValuesWith( "orgUnit", orgUnitId )
            .replacePropertyValuesWith( "program", programPropertyValue)
            .get( JsonObject.class );

        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder()
            .add( "skipCache=true" )
            .add( "programIdScheme=" + scheme);

        ApiResponse response = eventActions.post( object, queryParamsBuilder );

        response.validate().statusCode( 200 )
            .rootPath( "response" )
            .body( "status",  equalTo("SUCCESS") )
            .body( "ignored", equalTo( 0 ) )
            .body( "importSummaries.reference", everyItem( notNullValue() ) );

        String eventId = response.extractString( "response.importSummaries.reference[0]" );
        assertNotNull( eventId );

        eventActions.get( eventId ).validate()
            .statusCode( 200 )
            .body( "program", equalTo( PROGRAM_ID ) );
    }



    private void setupData() {

        ATTRIBUTE_ID = attributeActions.createUniqueAttribute(  "TEXT", "organisationUnit", "program" );
        //programAttributeId = attributeActions.createUniqueAttribute( "program", "TEXT" );

        assertNotNull( ATTRIBUTE_ID, "Failed to setup attribute" );
        OrgUnit orgUnit = orgUnitActions.generateDummy();

        orgUnit.setCode( OU_CODE );
        orgUnit.setName( OU_NAME );

        orgUnitId = orgUnitActions.create( orgUnit );
        assertNotNull( orgUnitId, "Failed to setup org unit" );

        programActions.addOrganisationUnits( PROGRAM_ID, orgUnitId ).validate().statusCode( 200 );

        orgUnitActions.update( orgUnitId, addAttributeValuePayload( orgUnitActions.get( orgUnitId ).getBody(), ATTRIBUTE_ID,
            ATTRIBUTE_VALUE ) )
            .validate().statusCode( 200 );

        programActions.update( PROGRAM_ID, addAttributeValuePayload( programActions.get( PROGRAM_ID ).getBody(), ATTRIBUTE_ID,
            ATTRIBUTE_VALUE ) )
            .validate().statusCode( 200 );
    }

    public JsonObject addAttributeValuePayload( JsonObject json, String attributeId, String attributeValue) {
        JsonObject attributeObj = new JsonObject();
        attributeObj.addProperty( "id", attributeId );

        JsonObject attributeValueObj = new JsonObject();
        attributeValueObj.addProperty( "value", attributeValue );
        attributeValueObj.add("attribute", attributeObj );

        JsonArray attributeValues = new JsonArray(  );
        attributeValues.add( attributeValueObj );

        json.add( "attributeValues", attributeValues );

        return json;
    }
}
