/*
 *  Copyright (c) 2004-2020, University of Oslo
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
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.hisp.dhis.tracker.importer.relationships;

import org.hisp.dhis.helpers.JsonObjectBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class DuplicatedRelationshipTestSupport
{

    static JsonObject invertRelationship( JsonObject jsonObject )
    {
        JsonObject inverseJsonObject = jsonObject.deepCopy();
        JsonObject relationship = (JsonObject) jsonObject.getAsJsonArray( "relationships" ).get( 0 );
        JsonArray relationships = new JsonArray();
        relationships.add( buildBidirectionalRelationship(
            relationship.getAsJsonObject( "to" ).get( "trackedEntity" ).getAsString(),
            relationship.getAsJsonObject( "from" ).get( "trackedEntity" ).getAsString() ) );
        inverseJsonObject.add( "relationships", relationships );
        return inverseJsonObject;
    }

    static JsonObject buildNonBidirectionalRelationship( String trackedEntity_1, String trackedEntity_2 )
    {
        return buildRelationship( trackedEntity_1, trackedEntity_2, "TV9oB9LT3sh" /* a non bidirectional relationship type*/ );
    }

    static JsonObject buildBidirectionalRelationship( String trackedEntity_1, String trackedEntity_2 )
    {
        return buildRelationship( trackedEntity_1, trackedEntity_2, "xLmPUYJX8Ks"  /* a bidirectional relationship type*/  );
    }

    static JsonObject buildRelationship( String trackedEntity_1, String trackedEntity_2, String relationshipType )
    {
        return new JsonObjectBuilder()
            .addProperty( "relationshipType", relationshipType )
            .addObject( "from", new JsonObjectBuilder()
                .addProperty( "trackedEntity", trackedEntity_1 ) )
            .addObject( "to", new JsonObjectBuilder()
                .addProperty( "trackedEntity", trackedEntity_2 ) )
            .build();
    }

    static JsonObject buildTrackedEntity( String trackedEntity )
    {
        return new JsonObjectBuilder()
            .addProperty( "trackedEntity", trackedEntity )
            .addProperty( "trackedEntityType", "Q9GufDoplCL" )
            .addProperty( "orgUnit", "g8upMTyEZGZ" )
            .build();
    }
}
