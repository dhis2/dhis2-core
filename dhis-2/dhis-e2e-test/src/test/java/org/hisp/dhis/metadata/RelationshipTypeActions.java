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

package org.hisp.dhis.metadata;

import com.google.gson.JsonObject;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.utils.DataGenerator;

import java.util.Locale;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class RelationshipTypeActions extends RestApiActions
{
    public RelationshipTypeActions( )
    {
        super( "/relationshipTypes" );
    }

    public String create(String fromRelationshipEntity, String fromRelationshipEntityId, String toRelationshipEntity, String toRelationshipEntityId) {
        JsonObject obj = JsonObjectBuilder.jsonObject()
            .addProperty( "name", "TA relationship type" + DataGenerator.randomString() )
            .addProperty( "fromToName", "TA FROM NAME" )
            .addProperty( "toFromName", "TA TO NAME" )
            .addObject( "fromConstraint", JsonObjectBuilder.jsonObject(getRelationshipTypeConstraint( fromRelationshipEntity, fromRelationshipEntityId ) ))
            .addObject( "toConstraint", JsonObjectBuilder.jsonObject(getRelationshipTypeConstraint( toRelationshipEntity, toRelationshipEntityId ) ))
            .build();

        return this.create( obj );
    }

    private JsonObject getRelationshipTypeConstraint( String relationshipEntity, String id ) {
        JsonObject obj = new JsonObject();
        obj.addProperty( "relationshipEntity", relationshipEntity );
        switch ( relationshipEntity ) {
        case "TRACKED_ENTITY_INSTANCE":
            return new JsonObjectBuilder( obj )
                .addObject( "trackedEntityType", new JsonObjectBuilder().addProperty( "id", id ) )
                .build();


        case "PROGRAM_STAGE_INSTANCE": {
            return new JsonObjectBuilder( obj )
                .addObject( "program", new JsonObjectBuilder().addProperty( "id", id ) )
                .build();
        }
    }

    return new JsonObject();
}}
