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

package org.hisp.dhis.tracker.importer.tei;

import com.google.gson.JsonObject;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TeiValidationTests
    extends TrackerNtiApiTest
{
    private String trackedEntityType;

    private String program;

    private String mandatoryTetAttribute;

    private String mandatoryProgramAttribute;

    private String attributeWithOptionSet;

    @BeforeAll
    public void beforeAll()
    {
        loginActions.loginAsSuperUser();

        setupData();
    }

    @Test
    public void shouldReturnErrorReportsWhenTeiIncorrect()
    {
        // arrange
        JsonObject trackedEntities = trackerActions.buildTei( "", Constants.ORG_UNIT_IDS[0] );

        // act
        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        // assert
        response.validateErrorReport()
            .body( "errorCode", hasItem( "E1121" ) );
    }

    @Test
    public void shouldNotReturnErrorWhenMandatoryTetAttributeIsPresent()
    {
        JsonObject trackedEntities = buildTeiWithMandatoryAttribute();

        // assert
        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );
        response.validateSuccessfulImport();
    }

    @Test
    public void shouldReturnErrorWhenMandatoryAttributesMissing()
    {
        // arrange
        JsonObject trackedEntities = trackerActions.buildTei( trackedEntityType, Constants.ORG_UNIT_IDS[0] );

        // assert
        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        response
            .validateErrorReport()
            .body( "errorCode", hasItem( "E1090" ) );
    }

    @Test
    public void shouldReturnErrorWhenRemovingMandatoryAttributes()
    {
        JsonObject object = buildTeiWithEnrollmentAndMandatoryAttributes();

        TrackerApiResponse response = trackerActions
            .postAndGetJobReport( object, new QueryParamsBuilder().add( "async=false" ) );

        String teiId = response
            .validateSuccessfulImport().extractImportedTeis().get( 0 );

        String enrollmentId = response.extractImportedEnrollments().get( 0 );

        JsonObjectBuilder.jsonObject( object )
            .addPropertyByJsonPath( "trackedEntities[0].trackedEntity", teiId )
            .addPropertyByJsonPath( "trackedEntities[0].attributes[0].value", null )
            .addPropertyByJsonPath( "trackedEntities[0].enrollments[0].enrollment", enrollmentId )
            .addPropertyByJsonPath( "trackedEntities[0].enrollments[0].attributes[0].value", null );

        trackerActions.postAndGetJobReport( object, new QueryParamsBuilder().add( "async=false" ) )
            .validateErrorReport()
            .body( "", hasSize( 2 ) )
            .body( "trackerType", hasItems( "TRACKED_ENTITY", "ENROLLMENT" ) )
            .body( "errorCode", hasItems( "E1076", "E1076" ) )
            .body( "message", hasItem( allOf( containsStringIgnoringCase( "TrackedEntityAttribute" ),
                containsStringIgnoringCase( mandatoryTetAttribute ) ) ) )
            .body( "message", hasItem(
                allOf( containsStringIgnoringCase( "TrackedEntityAttribute" ),
                    containsStringIgnoringCase( mandatoryProgramAttribute ) ) ) );
    }

    @Test
    public void shouldNotReturnErrorWhenRemovingNotMandatoryAttributes()
    {
        JsonObject object = buildTeiWithMandatoryAndOptionSetAttribute();
        String teiId = trackerActions
            .postAndGetJobReport( buildTeiWithMandatoryAndOptionSetAttribute(), new QueryParamsBuilder().add( "async=false" ) )
            .validateSuccessfulImport().extractImportedTeis().get( 0 );

        JsonObjectBuilder.jsonObject( object )
            .addPropertyByJsonPath( "trackedEntities[0]", "trackedEntity", teiId )
            .addPropertyByJsonPath( "trackedEntities[0].attributes[1]", "value", null );

        trackerActions.postAndGetJobReport( object, new QueryParamsBuilder().add( "async=false" ) )
            .validateSuccessfulImport();

        trackerActions.get( "/trackedEntities/" + teiId )
            .validate()
            .body( "attributes", hasSize( 1 ) );

    }

    @Test
    public void shouldReturnErrorWhenMandatoryProgramAttributeMissing()
    {
        // arrange
        JsonObject trackedEntities = JsonObjectBuilder
            .jsonObject( trackerActions.buildTeiAndEnrollment( trackedEntityType, Constants.ORG_UNIT_IDS[0], program ) ).build();

        // assert
        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        response
            .validateErrorReport()
            .body( "trackerType", hasItem( "ENROLLMENT" ) )
            .body( "errorCode", hasItem( "E1018" ) );
    }

    @Test
    public void shouldReturnErrorWhenAttributeWithOptionSetInvalid()
    {
        JsonObject trackedEntities = JsonObjectBuilder
            .jsonObject( buildTeiWithMandatoryAttribute() )
            .addOrAppendToArrayByJsonPath( "trackedEntities[0]", "attributes",
                new JsonObjectBuilder()
                    .addProperty( "attribute", attributeWithOptionSet )
                    .addProperty( "value", DataGenerator.randomString() )
                    .build()
            ).build();

        trackerActions.postAndGetJobReport( trackedEntities, new QueryParamsBuilder().add( "async=false" ) )
            .validateErrorReport()
            .body( "errorCode", hasItem( "E1125" ) )
            .body( "trackerType", hasItem( "TRACKED_ENTITY" ) );
    }

    @Test
    public void shouldNotReturnErrorWhenAttributeWithOptionSetIsPresent()
    {
        JsonObject trackedEntities = buildTeiWithMandatoryAndOptionSetAttribute();

        trackerActions.postAndGetJobReport( trackedEntities ).validateSuccessfulImport();

    }

    private JsonObject buildTeiWithMandatoryAndOptionSetAttribute()
    {
        JsonObject object = buildTeiWithMandatoryAttribute();

        JsonObjectBuilder.jsonObject( object )
            .addOrAppendToArrayByJsonPath( "trackedEntities[0]", "attributes", new JsonObjectBuilder()
                .addProperty( "attribute", attributeWithOptionSet )
                .addProperty( "value", "TA_YES" )
                .build() );

        return object;
    }

    private JsonObject buildTeiWithMandatoryAttribute()
    {
        JsonObject trackedEntities = JsonObjectBuilder
            .jsonObject( trackerActions.buildTei( trackedEntityType, Constants.ORG_UNIT_IDS[0] ) )
            .addPropertyByJsonPath( "trackedEntities[0]", "trackedEntity", new IdGenerator().generateUniqueId() )
            .addArrayByJsonPath( "trackedEntities[0]", "attributes", new JsonObjectBuilder()
                .addProperty( "attribute", mandatoryTetAttribute )
                .addProperty( "value", DataGenerator.randomString() )
                .build() )
            .build();

        return trackedEntities;
    }

    private JsonObject buildTeiWithEnrollmentAndMandatoryAttributes()
    {
        JsonObject trackedEntities = JsonObjectBuilder
            .jsonObject( trackerActions.buildTeiAndEnrollment( trackedEntityType, Constants.ORG_UNIT_IDS[0], program ) )
            .addArrayByJsonPath( "trackedEntities[0]", "attributes", new JsonObjectBuilder()
                .addProperty( "attribute", mandatoryTetAttribute )
                .addProperty( "value", DataGenerator.randomString() )
                .build() )
            .addArrayByJsonPath( "trackedEntities[0].enrollments[0]", "attributes", new JsonObjectBuilder()
                .addProperty( "attribute", mandatoryProgramAttribute )
                .addProperty( "value", DataGenerator.randomString() )
                .build() )
            .build();

        return trackedEntities;
    }

    private void setupData()
    {
        // create attributes
        RestApiActions trackedEntityAttributeActions = new RestApiActions( "trackedEntityAttributes" );
        ProgramActions programActions = new ProgramActions();

        mandatoryTetAttribute = trackedEntityAttributeActions.create( dummyTeiAttribute() );
        mandatoryProgramAttribute = trackedEntityAttributeActions.create( dummyTeiAttribute() );
        attributeWithOptionSet = trackedEntityAttributeActions.create( dummyTeiAttributeWithOptionSet() );

        RestApiActions trackedEntityTypeActions = new RestApiActions( "trackedEntityTypes" );

        // create a TET
        JsonObject trackedEntityTypePayload = JsonObjectBuilder.jsonObject()
            .addProperty( "name", DataGenerator.randomEntityName() )
            .addProperty( "shortName", DataGenerator.randomEntityName() )
            .addUserGroupAccess()
            .addOrAppendToArray( "trackedEntityTypeAttributes",
                new JsonObjectBuilder()
                    .addProperty( "mandatory", "true" )
                    .addObject( "trackedEntityAttribute", new JsonObjectBuilder()
                        .addProperty( "id", mandatoryTetAttribute ) )
                    .build(),
                new JsonObjectBuilder()
                    .addProperty( "mandatory", "false" )
                    .addObject( "trackedEntityAttribute", new JsonObjectBuilder()
                        .addProperty( "id", attributeWithOptionSet ) )
                    .build()
            )
            .build();

        trackedEntityType = trackedEntityTypeActions.create( trackedEntityTypePayload );

        // create a program
        program = programActions.createTrackerProgram( Constants.ORG_UNIT_IDS ).extractUid();
        ApiResponse programResponse = programActions.get( program );

        JsonObject programPayload = programResponse.getBody();

        new JsonObjectBuilder( programPayload )
            .addObject( "trackedEntityType", new JsonObjectBuilder().addProperty( "id", trackedEntityType ) )
            .addOrAppendToArray( "programTrackedEntityAttributes",
                new JsonObjectBuilder()
                    .addProperty( "mandatory", "true" )
                    .addObject( "trackedEntityAttribute", new JsonObjectBuilder().addProperty( "id", mandatoryProgramAttribute ) )
                    .build() )
            .build();

        trackedEntityTypeActions.update( trackedEntityType, trackedEntityTypePayload ).validate().statusCode( 200 );
        programActions.update( program, programPayload ).validate().statusCode( 200 );
    }

    private JsonObject dummyTeiAttribute()
    {
        return JsonObjectBuilder.jsonObject()
            .addProperty( "name", "TA attribute " + DataGenerator.randomEntityName() )
            .addProperty( "valueType", "TEXT" )
            .addProperty( "aggregationType", "NONE" )
            .addProperty( "shortName", "TA attribute" + DataGenerator.randomString() )
            .addUserGroupAccess()
            .build();
    }

    private JsonObject dummyTeiAttributeWithOptionSet()
    {
        return JsonObjectBuilder.jsonObject()
            .addProperty( "name", "TA attribute " + DataGenerator.randomEntityName() )
            .addProperty( "valueType", "TEXT" )
            .addProperty( "aggregationType", "NONE" )
            .addProperty( "shortName", "TA attribute" + DataGenerator.randomString() )
            .addObject( "optionSet", new JsonObjectBuilder().addProperty( "id", "ZGkmoWb77MW" ) )
            .addUserGroupAccess()
            .build();
    }
}
