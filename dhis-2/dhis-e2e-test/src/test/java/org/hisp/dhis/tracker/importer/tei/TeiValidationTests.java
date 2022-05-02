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
package org.hisp.dhis.tracker.importer.tei;

import com.google.gson.JsonObject;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.metadata.TrackedEntityAttributeActions;
import org.hisp.dhis.actions.metadata.TrackedEntityTypeActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.hisp.dhis.tracker.importer.databuilder.EnrollmentDataBuilder;
import org.hisp.dhis.tracker.importer.databuilder.TeiDataBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hisp.dhis.helpers.matchers.MatchesJson.matchesJSON;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TeiValidationTests
    extends TrackerNtiApiTest
{
    private String trackedEntityType;

    private String program;

    private String mandatoryTetAttribute;

    private String uniqueTetAttribute;

    private String mandatoryProgramAttribute;

    private String attributeWithOptionSet;

    @BeforeAll
    public void beforeAll()
    {
        loginActions.loginAsSuperUser();

        setupData();
    }

    @Test
    public void shouldValidateUniqueness()
    {
        String value = DataGenerator.randomString();

        JsonObject payload = new TeiDataBuilder()
            .addAttribute( uniqueTetAttribute, value )
            .addAttribute( mandatoryTetAttribute, value )
            .array( trackedEntityType, Constants.ORG_UNIT_IDS[0] );

        trackerActions.postAndGetJobReport( payload )
            .validateSuccessfulImport();

        trackerActions.postAndGetJobReport( payload ).validateErrorReport()
            .body( "", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .body( "errorCode", hasItem( "E1064" ) )
            .body( "message", hasItem( containsStringIgnoringCase( "non-unique" ) ) );
    }

    @Test
    public void shouldReturnErrorReportsWhenTeiIncorrect()
    {
        // arrange
        JsonObject trackedEntities = new TeiDataBuilder().array( "", Constants.ORG_UNIT_IDS[0] );

        // act
        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        // assert
        response.validateErrorReport()
            .body( "errorCode", hasItem( "E1121" ) );
    }

    @Test
    public void shouldNotReturnErrorWhenMandatoryTetAttributeIsPresent()
    {
        JsonObject trackedEntities = buildTeiWithMandatoryAttribute().array();

        // assert
        trackerActions.postAndGetJobReport( trackedEntities )
            .validateSuccessfulImport();
    }

    @Test
    public void shouldReturnErrorWhenMandatoryAttributesMissing()
    {
        // arrange
        JsonObject trackedEntities = new TeiDataBuilder().array( trackedEntityType, Constants.ORG_UNIT_IDS[0] );

        // assert
        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        response
            .validateErrorReport()
            .body( "errorCode", hasItem( "E1090" ) );
    }

    @Test
    public void shouldReturnErrorWhenRemovingMandatoryAttributes()
    {
        JsonObject object = buildTeiWithEnrollmentAndMandatoryAttributes().array();

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
        JsonObject payload = buildTeiWithMandatoryAndOptionSetAttribute().array();

        String teiId = trackerActions
            .postAndGetJobReport( payload,
                new QueryParamsBuilder().add( "async=false" ) )
            .validateSuccessfulImport().extractImportedTeis().get( 0 );

        JsonObjectBuilder.jsonObject( payload )
            .addPropertyByJsonPath( "trackedEntities[0]", "trackedEntity", teiId )
            .addPropertyByJsonPath( "trackedEntities[0].attributes[1]", "value", null );

        trackerActions.postAndGetJobReport( payload, new QueryParamsBuilder().add( "async=false" ) )
            .validateSuccessfulImport();

        trackerActions.getTrackedEntity( teiId )
            .validate()
            .body( "attributes", hasSize( 1 ) );

    }

    @Test
    public void shouldReturnErrorWhenMandatoryProgramAttributeMissing()
    {
        // arrange
        JsonObject trackedEntities = new TeiDataBuilder()
            .buildWithEnrollment( trackedEntityType, Constants.ORG_UNIT_IDS[0], program );

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
        JsonObject trackedEntities = buildTeiWithMandatoryAttribute()
            .addAttribute( attributeWithOptionSet, DataGenerator.randomString() )
            .array();

        trackerActions.postAndGetJobReport( trackedEntities, new QueryParamsBuilder().add( "async=false" ) )
            .validateErrorReport()
            .body( "errorCode", hasItem( "E1125" ) )
            .body( "trackerType", hasItem( "TRACKED_ENTITY" ) );
    }

    @Test
    public void shouldReturnErrorWhenUpdatingSoftDeletedTEI()
    {
        JsonObject trackedEntities = new TeiDataBuilder()
                .setTeiType(  Constants.TRACKED_ENTITY_TYPE )
                .setOu( Constants.ORG_UNIT_IDS[0] )
                .array();

        // create TEI
        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        response.validateSuccessfulImport();

        String teiId = response.extractImportedTeis().get( 0 );
        JsonObject trackedEntitiesToDelete = new TeiDataBuilder()
                .setId( teiId )
                .array();

        // delete TEI
        TrackerApiResponse deleteResponse = trackerActions.postAndGetJobReport( trackedEntitiesToDelete,
                new QueryParamsBuilder().add( "importStrategy=DELETE" ) );

        deleteResponse.validateSuccessfulImport();

        JsonObject trackedEntitiesToImportAgain = new TeiDataBuilder()
                .setId( teiId )
                .setTeiType(  Constants.TRACKED_ENTITY_TYPE )
                .setOu( Constants.ORG_UNIT_IDS[0] )
                .array();

        // Update TEI
        TrackerApiResponse responseImportAgain = trackerActions.postAndGetJobReport( trackedEntitiesToImportAgain );

        responseImportAgain
                .validateErrorReport()
                .body( "errorCode", hasItem( "E1114" ) );
    }

    @Test
    public void shouldNotReturnErrorWhenAttributeWithOptionSetIsPresent()
    {
        JsonObject trackedEntities = buildTeiWithMandatoryAndOptionSetAttribute().array();

        trackerActions.postAndGetJobReport( trackedEntities ).validateSuccessfulImport();

    }

    private TeiDataBuilder buildTeiWithMandatoryAndOptionSetAttribute()
    {
        return buildTeiWithMandatoryAttribute()
            .addAttribute( attributeWithOptionSet, "TA_YES" );
    }

    private TeiDataBuilder buildTeiWithMandatoryAttribute()
    {
        return new TeiDataBuilder()
            .setTeiType( trackedEntityType )
            .setOu( Constants.ORG_UNIT_IDS[0] )
            .addAttribute( mandatoryTetAttribute, DataGenerator.randomString() );
    }

    private TeiDataBuilder buildTeiWithEnrollmentAndMandatoryAttributes()
    {
        return buildTeiWithMandatoryAttribute()
            .addEnrollment( new EnrollmentDataBuilder().addAttribute( mandatoryProgramAttribute, DataGenerator.randomString() )
                .setProgram( program ).setOu( Constants.ORG_UNIT_IDS[0] ) );
    }

    private void setupData()
    {
        TrackedEntityAttributeActions trackedEntityAttributeActions = new TrackedEntityAttributeActions();
        ProgramActions programActions = new ProgramActions();
        TrackedEntityTypeActions trackedEntityTypeActions = new TrackedEntityTypeActions();

        trackedEntityType = trackedEntityTypeActions.create();

        // create attributes
        uniqueTetAttribute = trackedEntityAttributeActions.create( "TEXT", true );
        mandatoryTetAttribute = trackedEntityAttributeActions.create( "TEXT" );
        mandatoryProgramAttribute = trackedEntityAttributeActions.create( "TEXT" );
        attributeWithOptionSet = trackedEntityAttributeActions.createOptionSetAttribute( "ZGkmoWb77MW" );

        trackedEntityTypeActions.addAttribute( trackedEntityType, mandatoryTetAttribute, true );
        trackedEntityTypeActions.addAttribute( trackedEntityType, attributeWithOptionSet, false );
        trackedEntityTypeActions.addAttribute( trackedEntityType, uniqueTetAttribute, false );

        // create a program
        program = programActions.createTrackerProgram( trackedEntityType, Constants.ORG_UNIT_IDS ).extractUid();
        programActions.addAttribute( program, mandatoryProgramAttribute, true );
    }
}
