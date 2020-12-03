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
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerImporter_teiValidationTests
    extends ApiTest
{
    private TrackerActions trackerActions;

    private final String trackedEntityType = "Q9GufDoplCL";

    private String mandatoryAttribute;

    @BeforeAll
    public void beforeAll()
    {
        trackerActions = new TrackerActions();

        new LoginActions().loginAsSuperUser();

        setupData();
    }

    @Test
    public void shouldReturnErrorReportsWhenTeiIncorrect()
    {
        // arrange
        JsonObject trackedEntities = new JsonObjectBuilder()
            .addProperty( "trackedEntityType", "" )
            .addProperty( "orgUnit", Constants.ORG_UNIT_IDS[0] )
            .wrapIntoArray( "trackedEntities" );

        // act
        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        // assert
        response.validateErrorReport()
            .body( "message[0]",
                containsStringIgnoringCase( "Could not find TrackedEntityType" ) );
    }

    @Test
    public void shouldNotReturnErrorWhenMandatoryTetAttributeIsPresent() {
        JsonObject trackedEntities = new JsonObjectBuilder()
            .addProperty( "trackedEntityType", trackedEntityType )
            .addProperty( "orgUnit", Constants.ORG_UNIT_IDS[0] )
            .addArray( "attributes", new JsonObjectBuilder()
                .addProperty( "attribute", mandatoryAttribute )
                .addProperty( "value", "TETSTS" )
                .build() )
            .wrapIntoArray( "trackedEntities" );

        // assert
        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        response.prettyPrint();
        response
            .validateSuccessfulImport();
    }
    @Test
    public void shouldReturnErrorWhenMandatoryAttributesMissing()
    {
        // arrange
        JsonObject trackedEntities = new JsonObjectBuilder()
            .addProperty( "trackedEntityType", trackedEntityType )
            .addProperty( "orgUnit", Constants.ORG_UNIT_IDS[0] )
            .wrapIntoArray( "trackedEntities" );

        // assert
        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        response.prettyPrint();
        response
            .validateErrorReport()
            .body( "message[0]",
                stringContainsInOrder( "Attribute", "is mandatory in tracked entity type", "but not declared in tracked entity" ) )
            .body( "errorCode[0]", equalTo( "E1090" ) );
    }

    private void setupData()
    {
        RestApiActions trackedEntityTypeActions = new RestApiActions( "trackedEntityTypes" );
        RestApiActions trackedEntityAttributeActions = new RestApiActions( "trackedEntityAttributes" );
        JsonObject attribute = JsonObjectBuilder.jsonObject()
            .addProperty( "name", "TA attribute " + DataGenerator.randomEntityName() )
            .addProperty( "valueType", "TEXT" )
            .addProperty( "aggregationType", "NONE" )
            .addProperty( "shortName", "TA attribute" + DataGenerator.randomString() )
            .addUserGroupAccess()
            .build();

        mandatoryAttribute = trackedEntityAttributeActions.create( attribute );

        JsonObject trackedEntityTypePayload = trackedEntityTypeActions.get( trackedEntityType ).getBody();

        trackedEntityTypePayload = new JsonObjectBuilder(trackedEntityTypePayload)
            .addOrAppendToArray( "trackedEntityTypeAttributes",
                new JsonObjectBuilder()
                .addProperty( "mandatory", "true" )
                .addObject( "trackedEntityAttribute", new JsonObjectBuilder().addProperty( "id", mandatoryAttribute ) )
                .build() )
        .build();

        trackedEntityTypeActions.update( trackedEntityType, trackedEntityTypePayload ).validate().statusCode( 200 );

        mandatoryAttribute = trackedEntityTypeActions.get(trackedEntityType, new QueryParamsBuilder().addAll( "fields=trackedEntityTypeAttributes", "filter=trackedEntityTypeAttributes.mandatory:eq:true" ) )
            .extractString( "trackedEntityTypeAttributes.trackedEntityAttribute.id[0]" );
    }
}
