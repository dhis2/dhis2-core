/*
 * Copyright (c) 2004-2019, University of Oslo
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
package org.hisp.dhis.tracker.teis;

import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class RelationshipsTest
    extends ApiTest
{
    private RestApiActions trackedEntityInstanceActions;
    private RestApiActions relationshipTypesActions;
    private String bidirectionalRelationshipId;

    @BeforeAll
    public void before()
    {
        relationshipTypesActions = new RestApiActions( "/relationshipTypes" );
        trackedEntityInstanceActions = new RestApiActions( "/trackedEntityInstances" );

        new LoginActions().loginAsSuperUser();

        trackedEntityInstanceActions.postFile( new File( "src/test/resources/tracker/teis.json" ), "" ).validate()
            .statusCode( 200 );
        bidirectionalRelationshipId = relationshipTypesActions.get( "?filter=bidirectional:eq:true" ).extractString( "relationshipTypes.id[0]" );
    }

    @Test
    public void bidirectionalRelationshipsCanBeAdded()
    {
        // arrange
        String mother = "PZJz33UMzpS";

        String child = "brqJcRd1L6S";

        JsonObject responseBody = trackedEntityInstanceActions.get( child ).getBody();

        // act

        responseBody = addRelationship( responseBody, child, mother, bidirectionalRelationshipId );

        ApiResponse response = trackedEntityInstanceActions.update( child, responseBody );

        // assert
        response.validate().statusCode( 200 );
        assertEquals( "SUCCESS", response.getImportSummaries().get( 0 ).getStatus() );
        assertEquals( 1, response.getImportSummaries().get( 0 ).getImportCount().getUpdated() );

        ApiResponse motherResponse = trackedEntityInstanceActions.get( mother, "fields=relationships" );
        ApiResponse childResponse = trackedEntityInstanceActions.get( child, "fields=relationships" );

        motherResponse.validate().statusCode( 200 );
        motherResponse.validate()
            .body( "relationships", Matchers.hasSize( 1 ) )
            .body( "relationships[0].relationshipType", Matchers.equalTo( bidirectionalRelationshipId ) )
            .body( "relationships[0].from.trackedEntityInstance.trackedEntityInstance", Matchers.equalTo( child ) )
            .body( "relationships[0].to.trackedEntityInstance.trackedEntityInstance", Matchers.equalTo( mother ) );

        childResponse.validate().statusCode( 200 );
        childResponse.validate()
            .body( "relationships", Matchers.hasSize( 1 ) )
            .body( "relationships[0].relationshipType", Matchers.equalTo( bidirectionalRelationshipId ) )
            .body( "relationships[0].from.trackedEntityInstance.trackedEntityInstance", Matchers.equalTo( child ) )
            .body( "relationships[0].to.trackedEntityInstance.trackedEntityInstance", Matchers.equalTo( mother ) );
    }

    private JsonObject addRelationship( JsonObject object, String fromId, String toId, String relationshipType )
    {

        JsonObject relationship = new JsonObject();
        relationship.addProperty( "relationshipType", relationshipType );

        JsonObject from = new JsonObject();
        JsonObject tei = new JsonObject();
        tei.addProperty( "trackedEntityInstance", fromId );
        from.add( "trackedEntityInstance", tei );

        relationship.add( "from", from );

        JsonObject to = new JsonObject();
        JsonObject tei2 = new JsonObject();
        tei2.addProperty( "trackedEntityInstance", toId );
        to.add( "trackedEntityInstance", tei2 );

        relationship.add( "to", to );

        object.get( "relationships" ).getAsJsonArray().add( relationship );

        return object;
    }
}
