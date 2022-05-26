/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.tracker.importer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;

import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.AttributeActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.metadata.TrackedEntityTypeActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.hisp.dhis.tracker.importer.databuilder.TeiDataBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class IdSchemeTests
    extends TrackerNtiApiTest
{
    private static final String OU_NAME = "TA EventsImportIdSchemeTests ou name " + DataGenerator.randomString();

    private static final String OU_CODE = "TA EventsImportIdSchemeTests ou code " + DataGenerator.randomString();

    private static final String ATTRIBUTE_VALUE = "TA EventsImportIdSchemeTests attribute "
        + DataGenerator.randomString();

    private static String PROGRAM_ID;

    private String ORG_UNIT_ID;

    private static String PROGRAM_STAGE_ID;

    private static String ATTRIBUTE_ID;

    private static String TRACKED_ENTITY_TYPE;

    private OrgUnitActions orgUnitActions;

    private ProgramActions programActions;

    private AttributeActions attributeActions;

    private TrackedEntityTypeActions trackedEntityTypeActions;

    private static Stream<Arguments> idSchemeArguments()
    {
        return Stream.of(
            Arguments.arguments( "CODE", "code" ),
            Arguments.arguments( "NAME", "name" ),
            Arguments.arguments( "UID", "id" ),
            Arguments.arguments( "AUTO", "id" ),
            Arguments.arguments( "ATTRIBUTE:" + ATTRIBUTE_ID, "attributeValues.value[0]" ) );
    }

    @BeforeAll
    public void beforeAll()
    {
        orgUnitActions = new OrgUnitActions();
        programActions = new ProgramActions();
        attributeActions = new AttributeActions();
        trackedEntityTypeActions = new TrackedEntityTypeActions();

        loginActions.loginAsSuperUser();

        setupData();
    }

    // @Gintare: this is what I envision. One NTI test importing a payload with
    // all metadata types we support using
    // idScheme=ATTRIBUTE so without the parametrization. Keeping the
    // parametrized test for now just to ensure that
    // indeed all idSchemes work.
    @ParameterizedTest
    @MethodSource( "idSchemeArguments" )
    public void shouldBeImportedWithIdScheme( String scheme, String property )
    {
        String orgUnit = orgUnitActions.get( ORG_UNIT_ID ).extractString( property );
        assertNotNull( orgUnit, String.format( "OrgUnit property %s was not present.", property ) );
        String program = programActions.get( PROGRAM_ID ).extractString( property );
        assertNotNull( program, String.format( "Program property %s was not present.", property ) );
        String programStage = programActions.programStageActions.get( PROGRAM_STAGE_ID ).extractString( property );
        assertNotNull( programStage, String.format( "ProgramStage property %s was not present.", property ) );
        String trackedEntityType = trackedEntityTypeActions.get( TRACKED_ENTITY_TYPE ).extractString( property );
        assertNotNull( programStage, String.format( "TrackedEntityType property %s was not present.", property ) );
        // TODO add more fields, these are the fields we support idSchemes for
        // * Event.orgUnit
        // * Event.program
        // * Event.programStage
        // * Event.attributeOptionCombo
        // * Event.attributeCategoryOptions
        // * Event.DataValue.dataElement
        // * Relationship.relationshipType
        // * Enrollment.program
        // * Enrollment.orgUnit
        // * TrackedEntity.trackedEntityType
        // * TrackedEntity.orgUnit
        // * TrackedEntity.Attribute.attribute

        JsonObject payload = new TeiDataBuilder()
            .buildWithEnrollmentAndEvent( trackedEntityType, orgUnit, program, programStage );

        TrackerApiResponse response = trackerActions
            .postAndGetJobReport( payload, new QueryParamsBuilder().add( "idScheme=" + scheme ) )
            .validateSuccessfulImport();

        // TODO assert trackedEntityType in the response?
        trackerActions.getEvent( response.extractImportedEvents().get( 0 ) ).validate()
            .statusCode( 200 )
            .body( "orgUnit", equalTo( ORG_UNIT_ID ) )
            .body( "program", equalTo( PROGRAM_ID ) )
            .body( "programStage", equalTo( PROGRAM_STAGE_ID ) );
    }

    private void setupData()
    {
        JsonObject trackedEntityType = JsonObjectBuilder.jsonObject()
            .addProperty( "name", DataGenerator.randomEntityName() )
            .addProperty( "shortName", DataGenerator.randomEntityName() )
            .addProperty( "code", DataGenerator.randomEntityName() )
            .addUserGroupAccess()
            .build();
        TRACKED_ENTITY_TYPE = trackedEntityTypeActions.create( trackedEntityType );

        PROGRAM_ID = programActions.createTrackerProgram( TRACKED_ENTITY_TYPE, Constants.ORG_UNIT_IDS ).extractUid();
        PROGRAM_STAGE_ID = programActions
            .createProgramStage( PROGRAM_ID, "TeiIdSchemeTests program stage " + DataGenerator.randomString() );

        ATTRIBUTE_ID = attributeActions.createUniqueAttribute( "TEXT", "organisationUnit", "program", "programStage",
            "trackedEntityType" );
        assertNotNull( ATTRIBUTE_ID, "Failed to setup attribute" );

        JsonObject orgUnit = JsonObjectBuilder.jsonObject( orgUnitActions.createOrgUnitBody() )
            .addProperty( "code", OU_CODE )
            .addProperty( "name", OU_NAME )
            .build();
        ORG_UNIT_ID = orgUnitActions.create( orgUnit );
        assertNotNull( ORG_UNIT_ID, "Failed to setup org unit" );

        new UserActions().grantCurrentUserAccessToOrgUnit( ORG_UNIT_ID );
        programActions.addOrganisationUnits( PROGRAM_ID, ORG_UNIT_ID ).validate().statusCode( 200 );

        orgUnitActions
            .update( ORG_UNIT_ID, addAttributeValuePayload( orgUnitActions.get( ORG_UNIT_ID ).getBody(), ATTRIBUTE_ID,
                ATTRIBUTE_VALUE ) )
            .validate().statusCode( 200 );

        programActions
            .update( PROGRAM_ID, addAttributeValuePayload( programActions.get( PROGRAM_ID ).getBody(), ATTRIBUTE_ID,
                ATTRIBUTE_VALUE ) )
            .validate().statusCode( 200 );

        programActions.programStageActions.update( PROGRAM_STAGE_ID,
            addAttributeValuePayload( programActions.programStageActions.get( PROGRAM_STAGE_ID ).getBody(),
                ATTRIBUTE_ID,
                ATTRIBUTE_VALUE ) )
            .validate().statusCode( 200 );

        trackedEntityTypeActions.update( TRACKED_ENTITY_TYPE,
            addAttributeValuePayload( trackedEntityTypeActions.get( TRACKED_ENTITY_TYPE ).getBody(), ATTRIBUTE_ID,
                ATTRIBUTE_VALUE ) )
            .validate().statusCode( 200 );
    }

    public JsonObject addAttributeValuePayload( JsonObject json, String attributeId, String attributeValue )
    {
        // TODO should we not return the newly built JsonObject instead of the
        // json var?
        JsonObjectBuilder.jsonObject( json )
            .addArray( "attributeValues", JsonObjectBuilder.jsonObject()
                .addProperty( "value", attributeValue )
                .addObject( "attribute", new JsonObjectBuilder().addProperty( "id", attributeId ) )
                .build() );
        return json;
    }
}