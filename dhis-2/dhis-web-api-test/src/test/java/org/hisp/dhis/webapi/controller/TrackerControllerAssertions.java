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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

public class TrackerControllerAssertions
{

    public static JsonObject assertFirstRelationship( Relationship expected, JsonArray json )
    {
        return assertNthRelationship( expected, 0, json );
    }

    public static JsonObject assertNthRelationship( Relationship expected, int n, JsonArray json )
    {
        assertFalse( json.isEmpty(), "relationships should not be empty" );
        assertTrue( json.size() >= n,
            String.format( "element %d does not exist in %d relationships elements", n, json.size() ) );
        JsonObject jsonRelationship = json.get( n ).as( JsonObject.class );
        assertRelationship( expected, jsonRelationship );
        return jsonRelationship;
    }

    public static void assertRelationship( Relationship expected, JsonObject json )
    {
        assertFalse( json.isEmpty(), "relationship should not be empty" );
        assertEquals( expected.getUid(), json.getString( "relationship" ).string(), "relationship UID" );
        assertEquals( expected.getRelationshipType().getUid(), json.getString( "relationshipType" ).string(),
            "relationshipType UID" );
    }

    public static void assertNoRelationships( JsonObject json )
    {
        assertFalse( json.isEmpty() );
        JsonArray rels = json.getArray( "instances" );
        assertTrue( rels.isEmpty(), "instances should not contain any relationships" );
    }

    public static void assertEventWithinRelationship( ProgramStageInstance expected, JsonObject json )
    {
        JsonObject jsonEvent = json.getObject( "event" );
        assertFalse( jsonEvent.isEmpty(), "event should not be empty" );
        assertEquals( expected.getUid(), jsonEvent.getString( "event" ).string(), "event UID" );

        assertEquals( expected.getStatus().toString(), jsonEvent.getString( "status" ).string(), "event status" );
        assertEquals( expected.getProgramStage().getUid(), jsonEvent.getString( "programStage" ).string(),
            "event programStage UID" );
        assertEquals( expected.getProgramInstance().getUid(),
            jsonEvent.getString( "enrollment" ).string(), "event programInstance UID" );
        assertFalse( jsonEvent.has( "relationships" ), "relationships is not returned within relationship items" );
    }

    public static void assertTrackedEntityWithinRelationship( TrackedEntityInstance expected, JsonObject json )
    {
        JsonObject jsonTEI = json.getObject( "trackedEntity" );
        assertFalse( jsonTEI.isEmpty(), "trackedEntity should not be empty" );
        assertEquals( expected.getUid(), jsonTEI.getString( "trackedEntity" ).string(), "trackedEntity UID" );
        assertEquals( expected.getTrackedEntityType().getUid(), jsonTEI.getString( "trackedEntityType" ).string(),
            "trackedEntityType UID" );
        assertEquals( expected.getOrganisationUnit().getUid(), jsonTEI.getString( "orgUnit" ).string(), "orgUnit UID" );
        assertTrue( jsonTEI.getArray( "attributes" ).isEmpty(), "attributes should be empty" );
        assertFalse( jsonTEI.has( "relationships" ), "relationships is not returned within relationship items" );
    }

    public static void assertHasOnlyUid( String expectedUid, String member, JsonObject json )
    {
        JsonObject j = json.getObject( member );
        assertFalse( j.isEmpty(), member + " should not be empty" );
        assertHasOnlyMembers( j, member );
        assertEquals( expectedUid, j.getString( member ).string(), member + " UID" );
    }

    public static void assertEnrollmentWithinRelationship( ProgramInstance expected, JsonObject json )
    {
        JsonObject jsonEnrollment = json.getObject( "enrollment" );
        assertFalse( jsonEnrollment.isEmpty(), "enrollment should not be empty" );
        assertEquals( expected.getUid(), jsonEnrollment.getString( "enrollment" ).string(), "enrollment UID" );
        assertEquals( expected.getEntityInstance().getUid(),
            jsonEnrollment.getString( "trackedEntity" ).string(), "trackedEntity UID" );
        assertEquals( expected.getProgram().getUid(), jsonEnrollment.getString( "program" ).string(), "program UID" );
        assertEquals( expected.getOrganisationUnit().getUid(), jsonEnrollment.getString( "orgUnit" ).string(),
            "orgUnit UID" );
        assertTrue( jsonEnrollment.getArray( "events" ).isEmpty(), "events should be empty" );
        assertFalse( jsonEnrollment.has( "relationships" ), "relationships is not returned within relationship items" );
    }

    public static void assertHasOnlyMembers( JsonObject json, String... names )
    {
        Set<String> actual = new HashSet<>( json.names() );
        Set<String> expected = Set.of( names );
        assertEquals( expected.size(), actual.size(), () -> "unexpected total number of members in " + json );
        assertTrue( actual.containsAll( expected ),
            () -> "members mismatch between actual: " + actual + ", expected: " + expected );
    }

    public static void assertHasNoMember( JsonObject json, String name )
    {
        assertFalse( json.has( name ), String.format( "member \"%s\" should NOT be in %s", name, json ) );
    }

    public static void assertHasMembers( JsonObject json, String... names )
    {
        for ( String name : names )
        {
            assertHasMember( json, name );
        }
    }

    public static void assertHasMember( JsonObject json, String name )
    {
        assertTrue( json.has( name ), String.format( "member \"%s\" should be in %s", name, json ) );
    }
}
