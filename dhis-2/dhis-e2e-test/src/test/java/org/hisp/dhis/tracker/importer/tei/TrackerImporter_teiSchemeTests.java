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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.AttributeActions;
import org.hisp.dhis.actions.tracker.importer.TrackerActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerImporter_teiSchemeTests extends ApiTest
{
    private AttributeActions attributeActions;
    private RestApiActions trackedEntityTypeActions;
    private TrackerActions trackerActions;
    private String trackedEntityType;
    private String attributeId;

    @BeforeAll
    public void beforeAll() {
        attributeActions = new AttributeActions();
        trackedEntityTypeActions = new RestApiActions( "/trackedEntityTypes" );
        trackerActions = new TrackerActions();
        
        new LoginActions().loginAsSuperUser();

        setupData();
    }
    private static Stream<Arguments> provideIdSchemeArguments()
    {
        return Stream.of(
            Arguments.arguments( "CODE", "code" ),
            Arguments.arguments( "NAME", "name" ),
            Arguments.arguments( "UID", "id" )
            // ,
            //Arguments.arguments( "ATTRIBUTE:" + ATTRIBUTE_ID, "attributeValues.value[0]" )
        );
    }
    @ParameterizedTest
    @CsvSource( { "CODE,code", "NAME,name", "UID,id" } )
    @Disabled
    public void shouldImportTeisWithTrackedEntityTypeScheme(String scheme, String property) {
        String propertyValue = trackedEntityTypeActions.get(trackedEntityType).extractString( property );
        JsonObject trackedEntities = new JsonObjectBuilder()
           .addProperty( "trackedEntityType", propertyValue)
            .addProperty( "orgUnit", Constants.ORG_UNIT_IDS[0] )
            .wrapIntoArray( "trackedEntities" );


        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities, new QueryParamsBuilder().add( "trackedEntityIdScheme=" + scheme ) );

        response.validateSuccessfulImport();
    }

    private void setupData()
    {
        attributeId = attributeActions.createUniqueAttribute( "TEXT", "trackedEntityType" );

        assertNotNull( attributeId, "Failed to setup attribute" );

        JsonObject trackedEntityTypePayload = JsonObjectBuilder.jsonObject()
            .addProperty( "name", DataGenerator.randomEntityName() )
            .addProperty( "code", DataGenerator.randomString() )
            .addProperty( "shortName", DataGenerator.randomEntityName() )
            .addUserGroupAccess()
            /*.addOrAppendToArray( "trackedEntityTypeAttributes",
                new JsonObjectBuilder()
                    .addProperty( "mandatory", "true" )
                    .addObject( "trackedEntityAttribute", new JsonObjectBuilder()
                        .addProperty( "id", mandatoryTetAttribute ) )
                    .build() )

             */
            .build();

            trackedEntityType = trackedEntityTypeActions.create( trackedEntityTypePayload );
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

    private JsonObject dummyTeiAttribute() {
        return JsonObjectBuilder.jsonObject()
            .addProperty( "name", "TA attribute " + DataGenerator.randomEntityName() )
            .addProperty( "valueType", "TEXT" )
            .addProperty( "aggregationType", "NONE" )
            .addProperty( "shortName", "TA attribute" + DataGenerator.randomString() )
            .addUserGroupAccess()
            .build();
    }
}

