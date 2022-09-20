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
package org.hisp.dhis.dxf2;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.enrollment.AbstractEnrollmentService;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.*;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith( MockitoExtension.class )
public class EnrollmentServiceTest
{
    @Mock
    CurrentUserService currentUserService;

    @Mock
    EventService eventService;

    @Mock
    RelationshipService relationshipService;

    @Mock
    TrackedEntityAttributeService trackedEntityAttributeService;

    @Mock
    TrackerAccessManager trackerAccessManager;

    @Mock
    ProgramInstance programInstance;

    @Mock
    TrackedEntityInstance trackedEntityInstance;

    @Mock
    User user;

    @Spy
    AbstractEnrollmentService enrollmentService;

    String enrollmentId = CodeGenerator.generateUid();

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField( enrollmentService, "currentUserService", currentUserService );
        ReflectionTestUtils.setField( enrollmentService, "trackerAccessManager", trackerAccessManager );
        ReflectionTestUtils.setField( enrollmentService, "eventService", eventService );
        ReflectionTestUtils.setField( enrollmentService, "relationshipService", relationshipService );
        ReflectionTestUtils.setField( enrollmentService, "trackedEntityAttributeService",
            trackedEntityAttributeService );

        when( programInstance.getProgram() ).thenReturn( new Program() );
        when( programInstance.getStatus() ).thenReturn( ProgramStatus.ACTIVE );
        when( programInstance.getUid() ).thenReturn( enrollmentId );

        when( currentUserService.getCurrentUser() ).thenReturn( user );
    }

    @Test
    void givenNoInputParams_shouldUseDefaultParameters()
    {
        Enrollment enrollment = enrollmentService.getEnrollment( programInstance, TrackedEntityInstanceParams.FALSE );

        assertAll(
            () -> assertEquals( enrollmentId, enrollment.getEnrollment() ),
            () -> assertEquals( 0, enrollment.getRelationships().size() ),
            () -> assertEquals( 0, enrollment.getEvents().size() ),
            () -> assertEquals( 0, enrollment.getAttributes().size() ) );

        verify( relationshipService, times( 0 ) ).getRelationship( any(), any(), any() );
        verify( eventService, times( 0 ) ).getEvent( any(), anyBoolean(), anyBoolean(), anyBoolean() );
    }

    @Test
    void givenRelationshipParam_shouldSearchWithIncludeRelationships()
    {
        when( programInstance.getRelationshipItems() ).thenReturn( getRelationshipItems() );
        when( relationshipService.getRelationship( any(), any(), any() ) )
            .thenReturn( new org.hisp.dhis.dxf2.events.trackedentity.Relationship() );

        Enrollment enrollment = enrollmentService.getEnrollment( programInstance,
            TrackedEntityInstanceParams.FALSE.withIncludeRelationships( true ) );

        assertAll(
            () -> assertEquals( enrollmentId, enrollment.getEnrollment() ),
            () -> assertEquals( 1, enrollment.getRelationships().size() ),
            () -> assertEquals( 0, enrollment.getEvents().size() ) );

        verify( relationshipService, atLeastOnce() ).getRelationship( any(), any(), any() );
    }

    @Test
    void givenEventsParam_shouldSearchWithIncludeEvents()
    {
        when( programInstance.getProgramStageInstances() ).thenReturn( Set.of( new ProgramStageInstance() ) );
        when( eventService.getEvent( any(), anyBoolean(), anyBoolean(), anyBoolean() ) ).thenReturn( new Event() );

        Enrollment enrollment = enrollmentService.getEnrollment( programInstance,
            TrackedEntityInstanceParams.FALSE.withIncludeEvents( true ) );

        assertAll(
            () -> assertEquals( enrollmentId, enrollment.getEnrollment() ),
            () -> assertEquals( 1, enrollment.getEvents().size() ),
            () -> assertEquals( 0, enrollment.getRelationships().size() ) );

        verify( eventService, atLeastOnce() ).getEvent( any(), anyBoolean(), anyBoolean(), anyBoolean() );
    }

    @Test
    void givenAttributesParam_shouldSearchWithIncludeAttributes()
    {
        when( programInstance.getEntityInstance() ).thenReturn( trackedEntityInstance );

        TrackedEntityAttribute attribute = getTrackedEntityAttribute();

        TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
        attributeValue.setAttribute( attribute );

        when( trackedEntityInstance.getTrackedEntityAttributeValues() ).thenReturn( Set.of( attributeValue ) );
        when( trackedEntityInstance.getTrackedEntityType() ).thenReturn( new TrackedEntityType() );
        when( trackedEntityAttributeService.getAllUserReadableTrackedEntityAttributes( any(), any(), any() ) )
            .thenReturn( Set.of( attribute ) );

        Enrollment enrollment = enrollmentService.getEnrollment( programInstance,
            TrackedEntityInstanceParams.FALSE.withIncludeAttributes( true ) );

        assertAll(
            () -> assertEquals( enrollmentId, enrollment.getEnrollment() ),
            () -> assertEquals( 0, enrollment.getEvents().size() ),
            () -> assertEquals( 0, enrollment.getRelationships().size() ),
            () -> assertEquals( 1, enrollment.getAttributes().size() ),
            () -> assertEquals( attributeValue.getAttribute().getUid(),
                enrollment.getAttributes().get( 0 ).getAttribute() ),
            () -> assertEquals( attributeValue.getValue(), enrollment.getAttributes().get( 0 ).getValue() ) );
    }

    @Test
    void givenAttributesParam_shouldNotFindAttributesWhenUserHasNoReadAccess()
    {
        when( programInstance.getEntityInstance() ).thenReturn( trackedEntityInstance );

        TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
        attributeValue.setAttribute( getTrackedEntityAttribute() );

        when( trackedEntityInstance.getTrackedEntityAttributeValues() ).thenReturn( Set.of( attributeValue ) );
        when( trackedEntityInstance.getTrackedEntityType() ).thenReturn( new TrackedEntityType() );
        when( trackedEntityAttributeService.getAllUserReadableTrackedEntityAttributes( any(), any(), any() ) )
            .thenReturn( Set.of( new TrackedEntityAttribute() ) );

        Enrollment enrollment = enrollmentService.getEnrollment( programInstance,
            TrackedEntityInstanceParams.FALSE.withIncludeAttributes( true ) );

        assertAll(
            () -> assertEquals( enrollmentId, enrollment.getEnrollment() ),
            () -> assertEquals( 0, enrollment.getEvents().size() ),
            () -> assertEquals( 0, enrollment.getRelationships().size() ),
            () -> assertEquals( 0, enrollment.getAttributes().size() ) );
    }

    private static TrackedEntityAttribute getTrackedEntityAttribute()
    {
        TrackedEntityAttribute attribute = new TrackedEntityAttribute();
        attribute.setUid( CodeGenerator.generateUid() );
        attribute.setDescription( "description" );
        attribute.setCode( "code" );
        attribute.setShortName( "shortname" );
        attribute.setName( "name" );
        return attribute;
    }

    private Set<RelationshipItem> getRelationshipItems()
    {
        Relationship relationship = new Relationship();
        relationship.setRelationshipType( new RelationshipType() );

        RelationshipItem from = new RelationshipItem();
        RelationshipItem to = new RelationshipItem();
        from.setTrackedEntityInstance( new TrackedEntityInstance() );
        to.setTrackedEntityInstance( new TrackedEntityInstance() );
        from.setRelationship( relationship );
        to.setRelationship( relationship );

        return Set.of( from, to );
    }
}
