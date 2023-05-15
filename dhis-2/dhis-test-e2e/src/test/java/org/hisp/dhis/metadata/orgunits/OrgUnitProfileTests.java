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
package org.hisp.dhis.metadata.orgunits;

import static org.hamcrest.Matchers.*;

import java.io.File;
import java.util.List;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.dataitem.DataItemActions;
import org.hisp.dhis.actions.metadata.AttributeActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.dto.MetadataApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OrgUnitProfileTests
    extends ApiTest
{
    private LoginActions loginActions;

    private AttributeActions attributeActions;

    private RestApiActions orgUnitProfileActions;

    private String orgUnitId;

    @BeforeAll
    public void beforeAll()
    {
        loginActions = new LoginActions();
        attributeActions = new AttributeActions();
        orgUnitProfileActions = new RestApiActions( "/organisationUnitProfile" );

        loginActions.loginAsSuperUser();

        orgUnitId = new OrgUnitActions().createOrgUnit();
    }

    @Test
    public void shouldApplyProfileAttributes()
    {
        // arrange
        String attributeId = attributeActions.createAttribute( "TEXT", false, "organisationUnit" );
        String attributeValue = DataGenerator.randomString();

        new OrgUnitActions().addAttributeValue( orgUnitId, attributeId, attributeValue );

        JsonArray array = new JsonArray();
        array.add( attributeId );

        JsonObject profileBody = orgUnitProfileActions.get().getBody();

        // act
        orgUnitProfileActions.post(
            new JsonObjectBuilder( profileBody ).addArray( "attributes", array ).build() ).validate().statusCode( 200 );

        // assert
        orgUnitProfileActions.get().validate().body( "attributes", hasSize( greaterThanOrEqualTo( 1 ) ) );

        orgUnitProfileActions.get( "/" + orgUnitId + "/data" )
            .validate()
            .statusCode( 200 )
            .body( "attributes", hasSize( 1 ) )
            .rootPath( "attributes[0]" )
            .body( "value", equalTo( attributeValue ) )
            .body( "id", equalTo( attributeId ) )
            .body( "label", notNullValue() );
    }

    @Test
    public void shouldApplyGroupSets()
    {
        // arrange

        MetadataApiResponse response = new MetadataActions()
            .importAndValidateMetadata( new File( "src/test/resources/metadata/orgunits/ou_with_group_and_set.json" ) );

        String groupSet = response.extractObjectUid( "OrganisationUnitGroupSet" ).get( 0 );
        String ou = response.extractObjectUid( "OrganisationUnit" ).get( 0 );

        JsonArray array = new JsonArray();
        array.add( groupSet );

        JsonObject profileBody = new JsonObjectBuilder().addArray( "groupSets", array ).build();

        // act
        orgUnitProfileActions.post(
            profileBody ).validate().statusCode( 200 );

        // assert
        orgUnitProfileActions.get().validate().body( "groupSets", hasSize( greaterThanOrEqualTo( 1 ) ) );

        orgUnitProfileActions.get( "/" + ou + "/data" )
            .validate()
            .statusCode( 200 )
            .body( "groupSets", hasSize( 1 ) )
            .rootPath( "groupSets[0]" )
            .body( "id", equalTo( groupSet ) )
            .body( "label", notNullValue() )
            .body( "value", notNullValue() );
    }

    @Test
    public void shouldApplyDataItems()
    {
        List<String> datItems = new DataItemActions()
            .get( "", new QueryParamsBuilder().add( "filter=dimensionItemType:in:[DATA_ELEMENT,PROGRAM_INDICATOR]" ) )
            .extractList( "dataItems.id" );

        JsonArray array = new JsonArray();

        datItems.forEach( p -> array.add( p ) );

        orgUnitProfileActions.post(
            new JsonObjectBuilder().addArray( "dataItems", array ).build() ).validate().statusCode( 200 );

        orgUnitProfileActions.get().validate().body( "dataItems", hasSize( greaterThanOrEqualTo( 1 ) ) );

        // todo add validation for organisationUnitProfile/id/data

    }

}
