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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.stream.Stream;

import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.metadata.AttributeActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.OrgUnit;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventImportIdSchemeTests
    extends TrackerApiTest
{
    private static String OU_NAME = "TA EventsImportIdSchemeTests ou name " + DataGenerator.randomString();

    private static String OU_CODE = "TA EventsImportIdSchemeTests ou code " + DataGenerator.randomString();

    private static String ATTRIBUTE_VALUE = "TA EventsImportIdSchemeTests attribute " + DataGenerator.randomString();

    private static String PROGRAM_ID = Constants.EVENT_PROGRAM_ID;

    private static String ATTRIBUTE_ID;

    private OrgUnitActions orgUnitActions;

    private AttributeActions attributeActions;

    private String orgUnitId;

    private static Stream<Arguments> provideIdSchemeArguments()
    {
        return Stream.of(
            Arguments.arguments( "CODE", "code" ),
            Arguments.arguments( "NAME", "name" ),
            Arguments.arguments( "UID", "id" ),
            Arguments.arguments( "ATTRIBUTE:" + ATTRIBUTE_ID, "attributeValues.value[0]" ) );
    }

    @BeforeAll
    public void beforeAll()
    {
        orgUnitActions = new OrgUnitActions();
        attributeActions = new AttributeActions();

        loginActions.loginAsSuperUser();

        setupData();
    }

    @ParameterizedTest
    @MethodSource( "provideIdSchemeArguments" )
    public void eventsShouldBeImportedWithOrgUnitScheme( String ouScheme, String ouProperty )
        throws Exception
    {
        String ouPropertyValue = orgUnitActions.get( orgUnitId ).extractString( ouProperty );

        assertNotNull( ouPropertyValue, String.format( "Org unit property %s was not present.", ouProperty ) );

        JsonObject object = new FileReaderUtils().read( new File( "src/test/resources/tracker/events/events.json" ) )
            .replacePropertyValuesWith( "orgUnit", ouPropertyValue )
            .replacePropertyValuesWithIds( "event" )
            .get( JsonObject.class );

        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder()
            .add( "skipCache=true" )
            .add( "orgUnitIdScheme=" + ouScheme );

        ApiResponse response = eventActions.post( object, queryParamsBuilder );

        response.validate().statusCode( 200 )
            .rootPath( "response" )
            .body( "status", equalTo( "SUCCESS" ) )
            .body( "ignored", equalTo( 0 ) )
            .body( "imported", greaterThan( 1 ) )
            .body( "importSummaries.reference", everyItem( notNullValue() ) );

        String eventId = response.extractString( "response.importSummaries.reference[0]" );
        assertNotNull( eventId );

        eventActions.get( eventId ).validate()
            .statusCode( 200 )
            .body( "orgUnit", equalTo( orgUnitId ) );
    }

    @ParameterizedTest
    @MethodSource( "provideIdSchemeArguments" )
    public void eventsShouldBeImportedWithProgramScheme( String scheme, String property )
        throws Exception
    {
        String programPropertyValue = programActions.get( PROGRAM_ID ).extractString( property );

        assertNotNull( programPropertyValue, String.format( "Program property %s was not present.", property ) );

        JsonObject object = new FileReaderUtils().read( new File( "src/test/resources/tracker/events/events.json" ) )
            .replacePropertyValuesWithIds( "event" )
            .replacePropertyValuesWith( "orgUnit", orgUnitId )
            .replacePropertyValuesWith( "program", programPropertyValue )
            .get( JsonObject.class );

        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder()
            .add( "skipCache=true" )
            .add( "programIdScheme=" + scheme );

        ApiResponse response = eventActions.post( object, queryParamsBuilder );

        response.validate().statusCode( 200 )
            .rootPath( "response" )
            .body( "status", equalTo( "SUCCESS" ) )
            .body( "ignored", equalTo( 0 ) )
            .body( "importSummaries.reference", everyItem( notNullValue() ) );

        String eventId = response.extractString( "response.importSummaries.reference[0]" );
        assertNotNull( eventId );

        eventActions.get( eventId ).validate()
            .statusCode( 200 )
            .body( "program", equalTo( PROGRAM_ID ) );
    }

    private void setupData()
    {

        ATTRIBUTE_ID = attributeActions.createUniqueAttribute( "TEXT", "organisationUnit", "program" );
        // programAttributeId = attributeActions.createUniqueAttribute(
        // "program", "TEXT" );

        assertNotNull( ATTRIBUTE_ID, "Failed to setup attribute" );
        OrgUnit orgUnit = orgUnitActions.generateDummy();

        orgUnit.setCode( OU_CODE );
        orgUnit.setName( OU_NAME );

        orgUnitId = orgUnitActions.create( orgUnit );
        assertNotNull( orgUnitId, "Failed to setup org unit" );

        programActions.addOrganisationUnits( PROGRAM_ID, orgUnitId ).validate().statusCode( 200 );

        orgUnitActions
            .update( orgUnitId, addAttributeValuePayload( orgUnitActions.get( orgUnitId ).getBody(), ATTRIBUTE_ID,
                ATTRIBUTE_VALUE ) )
            .validate().statusCode( 200 );

        programActions
            .update( PROGRAM_ID, addAttributeValuePayload( programActions.get( PROGRAM_ID ).getBody(), ATTRIBUTE_ID,
                ATTRIBUTE_VALUE ) )
            .validate().statusCode( 200 );
    }

    public JsonObject addAttributeValuePayload( JsonObject json, String attributeId, String attributeValue )
    {
        JsonObject attributeObj = new JsonObject();
        attributeObj.addProperty( "id", attributeId );

        JsonObject attributeValueObj = new JsonObject();
        attributeValueObj.addProperty( "value", attributeValue );
        attributeValueObj.add( "attribute", attributeObj );

        JsonArray attributeValues = new JsonArray();
        attributeValues.add( attributeValueObj );

        json.add( "attributeValues", attributeValues );

        return json;
    }
}
