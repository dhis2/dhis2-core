/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.program;

import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;
import static org.hisp.dhis.program.ProgramTest.getNewProgram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.junit.jupiter.api.Test;

/**
 * @author David Mackessy
 */
class EnrollmentTest
{
    @Test
    void testCopyOfWithPropertyValuesSet()
    {
        Enrollment original = getNewEnrollmentWithNoNulls();
        Program copiedProgram = getNewProgram();
        Enrollment copy = Enrollment.copyOf( original, copiedProgram );

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertNotEquals( original.getUid(), copy.getUid() );
        assertNotEquals( original.getProgram(), copy.getProgram() );

        assertEquals( original.getEvents(), copy.getEvents() );
        assertEquals( original.getStatus(), copy.getStatus() );
        assertEquals( original.getComments(), copy.getComments() );
        assertEquals( original.getName(), copy.getName() );
        assertEquals( original.getIncidentDate(), copy.getIncidentDate() );
        assertEquals( original.getEnrollmentDate(), copy.getEnrollmentDate() );
        assertEquals( original.getFollowup(), copy.getFollowup() );
        assertEquals( original.getGeometry(), copy.getGeometry() );
        assertEquals( original.getOrganisationUnit(), copy.getOrganisationUnit() );
        assertEquals( original.getEndDate(), copy.getEndDate() );
        assertEquals( original.getRelationshipItems(), copy.getRelationshipItems() );
        assertEquals( original.getCreatedByUserInfo(), copy.getCreatedByUserInfo() );
        assertEquals( original.getMessageConversations(), copy.getMessageConversations() );
    }

    @Test
    void testCopyOfWithNullPropertyValues()
    {
        Enrollment original = getNewEnrollmentWithNulls();
        Program copiedProgram = getNewProgram();
        Enrollment copy = Enrollment.copyOf( original, copiedProgram );

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertNotEquals( original.getUid(), copy.getUid() );
        assertNotEquals( original.getProgram(), copy.getProgram() );

        assertEquals( original.getCreatedByUserInfo(), copy.getCreatedByUserInfo() );
        assertEquals( original.getEndDate(), copy.getEndDate() );
        assertEquals( original.getEnrollmentDate(), copy.getEnrollmentDate() );
        assertEquals( original.getFollowup(), copy.getFollowup() );
        assertEquals( original.getGeometry(), copy.getGeometry() );
        assertEquals( original.getIncidentDate(), copy.getIncidentDate() );
        assertEquals( original.getName(), copy.getName() );
        assertEquals( original.getOrganisationUnit(), copy.getOrganisationUnit() );
        assertEquals( original.getStatus(), copy.getStatus() );
        assertTrue( copy.getComments().isEmpty() );
        assertTrue( copy.getMessageConversations().isEmpty() );
        assertTrue( copy.getEvents().isEmpty() );
        assertTrue( copy.getRelationshipItems().isEmpty() );
    }

    /**
     * This test checks the expected field count for {@link Enrollment}. This is
     * important due to {@link Enrollment#copyOf} functionality. If a new field
     * is added then {@link Enrollment#copyOf} should be updated with the
     * appropriate copying approach.
     */
    @Test
    void testExpectedFieldCount()
    {
        Field[] allClassFieldsIncludingInherited = getAllFields( Enrollment.class );
        assertEquals( 36, allClassFieldsIncludingInherited.length );
    }

    private Enrollment getNewEnrollmentWithNoNulls()
    {
        Enrollment e = new Enrollment();
        Program program = getNewProgram();
        e.setAutoFields();
        e.setComments( List.of( new TrackedEntityComment( "comment1", "amin" ) ) );
        e.setCompletedBy( "admin" );
        e.setEndDate( new Date() );
        e.setEnrollmentDate( new Date() );
        e.setEvents( Set.of() );
        e.setFollowup( true );
        e.setIncidentDate( new Date() );
        e.setMessageConversations( List.of( new MessageConversation() ) );
        e.setName( "Enrollment 1" );
        e.setOrganisationUnit( new OrganisationUnit( "org1" ) );
        e.setProgram( program );
        e.setRelationshipItems( Set.of() );
        e.setStoredBy( "admin" );
        e.setTrackedEntity( new TrackedEntity() );
        e.setPublicAccess( "rw------" );
        return e;
    }

    private Enrollment getNewEnrollmentWithNulls()
    {
        Enrollment e = new Enrollment();
        e.setName( null );
        e.setComments( null );
        e.setCompletedBy( null );
        e.setEndDate( null );
        e.setEnrollmentDate( null );
        e.setEvents( null );
        e.setIncidentDate( null );
        e.setMessageConversations( null );
        e.setOrganisationUnit( null );
        e.setProgram( null );
        e.setPublicAccess( null );
        e.setRelationshipItems( null );
        e.setStoredBy( null );
        e.setTrackedEntity( null );
        return e;
    }
}
