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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.AttributeActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventIdSchemeTests
    extends ApiTest
{

    private static final String OU_NAME = "TA EventsImportIdSchemeTests ou name " + DataGenerator.randomString();

    private static final String OU_CODE = "TA EventsImportIdSchemeTests ou code " + DataGenerator.randomString();

    private static final String ATTRIBUTE_VALUE = "TA EventsImportIdSchemeTests attribute " + DataGenerator.randomString();

    private static final String PROGRAM_ID = Constants.EVENT_PROGRAM_ID;

    private static final String PROGRAM_STAGE_ID = Constants.EVENT_PROGRAM_STAGE_ID;

    private static String ATTRIBUTE_ID;

    private OrgUnitActions orgUnitActions;

    private ProgramActions programActions;

    private AttributeActions attributeActions;

    private TrackerActions trackerActions;

    private String orgUnitId;

    private static Stream<Arguments> provideIdSchemeArguments()
    {
        return Stream.of(
            Arguments.arguments( "CODE", "code" ),
            Arguments.arguments( "NAME", "name" ),
            Arguments.arguments( "UID", "id" ),
            Arguments.arguments( "ATTRIBUTE:" + ATTRIBUTE_ID, "attributeValues.value[0]" )
        );
    }

    @BeforeAll
    public void beforeAll()
    {
        orgUnitActions = new OrgUnitActions();
        programActions = new ProgramActions();
        attributeActions = new AttributeActions();
        trackerActions = new TrackerActions();

        new LoginActions().loginAsSuperUser();

        setupData();
    }

    @ParameterizedTest
    @MethodSource( "provideIdSchemeArguments" )
    public void eventsShouldBeImportedWithOrgUnitScheme( String ouScheme, String ouProperty )
        throws Exception
    {
        String ouPropertyValue = orgUnitActions.get( orgUnitId ).extractString( ouProperty );
        assertNotNull( ouPropertyValue, String.format( "Org unit property %s was not present.", ouProperty ) );

        JsonObject object = new FileReaderUtils().read( new File( "src/test/resources/tracker/importer/events/event.json" ) )
            .replacePropertyValuesWith( "orgUnit", ouPropertyValue )
            .replacePropertyValuesWithIds( "event" )
            .get( JsonObject.class );

        TrackerApiResponse response = trackerActions
            .postAndGetJobReport( object, new QueryParamsBuilder().add( "orgUnitIdScheme=" + ouScheme ) );

        String eventId = response.validateSuccessfulImport()
            .extractImportedEvents().get( 0 );

        trackerActions.get( "/events/" + eventId ).validate()
            .statusCode( 200 )
            .body( "orgUnit", equalTo( orgUnitId ) );
    }

    @ParameterizedTest
    @MethodSource( "provideIdSchemeArguments" )
    public void eventsShouldBeImportedWithProgramScheme( String scheme, String property )
        throws Exception
    {
        // arrange
        String programPropertyValue = programActions.get( PROGRAM_ID ).extractString( property );

        assertNotNull( programPropertyValue, String.format( "Program property %s was not present.", property ) );

        JsonObject object = new FileReaderUtils().read( new File( "src/test/resources/tracker/importer/events/event.json" ) )
            .replacePropertyValuesWithIds( "event" )
            .replacePropertyValuesWith( "orgUnit", orgUnitId )
            .replacePropertyValuesWith( "program", programPropertyValue )
            .replacePropertyValuesWith( "programStage", PROGRAM_STAGE_ID )
            .get( JsonObject.class );

        // act
        TrackerApiResponse response = trackerActions
            .postAndGetJobReport( object, new QueryParamsBuilder().add( "programIdScheme=" + scheme ) );

        // assert
        String eventId = response.validateSuccessfulImport()
            .extractImportedEvents().get( 0 );
        assertNotNull( "Event was not created", eventId );

        trackerActions.get( "/events/" + eventId ).validate()
            .statusCode( 200 )
            .body( "program", equalTo( PROGRAM_ID ) );
    }

    private void setupData()
    {
        ATTRIBUTE_ID = attributeActions.createUniqueAttribute( "TEXT", "organisationUnit", "program" );

        assertNotNull( ATTRIBUTE_ID, "Failed to setup attribute" );

        JsonObject orgUnit = JsonObjectBuilder.jsonObject( orgUnitActions.createOrgUnitBody() )
            .addProperty( "code", OU_CODE )
            .addProperty( "name", OU_NAME )
            .build();

        orgUnitId = orgUnitActions.create( orgUnit );
        assertNotNull( orgUnitId, "Failed to setup org unit" );

        new UserActions().grantCurrentUserAccessToOrgUnit( orgUnitId );
        programActions.addOrganisationUnits( PROGRAM_ID, orgUnitId ).validate().statusCode( 200 );

        orgUnitActions.update( orgUnitId, addAttributeValuePayload( orgUnitActions.get( orgUnitId ).getBody(), ATTRIBUTE_ID,
            ATTRIBUTE_VALUE ) )
            .validate().statusCode( 200 );

        programActions.update( PROGRAM_ID, addAttributeValuePayload( programActions.get( PROGRAM_ID ).getBody(), ATTRIBUTE_ID,
            ATTRIBUTE_VALUE ) )
            .validate().statusCode( 200 );
    }

    public JsonObject addAttributeValuePayload( JsonObject json, String attributeId, String attributeValue )
    {
        JsonObjectBuilder.jsonObject(json)
            .addArray( "attributeValues", JsonObjectBuilder.jsonObject()
                .addProperty( "value", attributeValue )
                .addObject( "attribute", new JsonObjectBuilder().addProperty( "id", attributeId ) )
                .build() );

        return json ;

    }
}
