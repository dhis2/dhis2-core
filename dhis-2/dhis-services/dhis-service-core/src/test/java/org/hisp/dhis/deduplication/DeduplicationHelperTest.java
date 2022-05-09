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
package org.hisp.dhis.deduplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Lists;

@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( { MockitoExtension.class } )
class DeduplicationHelperTest extends DhisConvenienceTest
{
    @InjectMocks
    private DeduplicationHelper deduplicationHelper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AclService aclService;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private ProgramInstanceService programInstanceService;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private TrackedEntityType trackedEntityTypeA;

    private TrackedEntityType trackedEntityTypeB;

    private RelationshipType relationshipType;

    private RelationshipType relationshipTypeBidirectional;

    private TrackedEntityAttribute attribute;

    private ProgramInstance programInstance;

    private MergeObject mergeObject;

    private User user;

    @BeforeEach
    public void setUp()
    {
        List<String> relationshipUids = Lists.newArrayList( "REL_A", "REL_B" );
        List<String> attributeUids = Lists.newArrayList( "ATTR_A", "ATTR_B" );
        List<String> enrollmentUids = Lists.newArrayList( "PI_A", "PI_B" );

        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitB = createOrganisationUnit( 'B' );
        trackedEntityTypeA = createTrackedEntityType( 'A' );
        trackedEntityTypeB = createTrackedEntityType( 'B' );
        relationshipType = createRelationshipType( 'A' );
        relationshipTypeBidirectional = createRelationshipType( 'B' );
        attribute = createTrackedEntityAttribute( 'A' );
        programInstance = createProgramInstance( createProgram( 'A' ), getTeiA(), organisationUnitA );
        mergeObject = MergeObject.builder()
            .relationships( relationshipUids )
            .trackedEntityAttributes( attributeUids )
            .enrollments( enrollmentUids )
            .build();
        user = makeUser( "A", Lists.newArrayList( "F_TRACKED_ENTITY_MERGE" ) );
        relationshipType.setBidirectional( false );
        relationshipTypeBidirectional.setBidirectional( true );

        when( currentUserService.getCurrentUser() ).thenReturn( user );
        when( aclService.canDataWrite( user, trackedEntityTypeA ) ).thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityTypeB ) ).thenReturn( true );
        when( aclService.canDataWrite( user, relationshipType ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programInstance.getProgram() ) ).thenReturn( true );
        when( relationshipService.getRelationships( relationshipUids ) ).thenReturn( getRelationships() );
        when( programInstanceService.getProgramInstances( enrollmentUids ) ).thenReturn( getEnrollments() );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnitA ) ).thenReturn( true );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnitB ) ).thenReturn( true );
    }

    @Test
    void shouldHasUserAccess()
    {
        String hasUserAccess = deduplicationHelper.getUserAccessErrors(
            getTeiA(), getTeiB(),
            mergeObject );

        assertNull( hasUserAccess );
    }

    @Test
    void shouldNotHasUserAccessWhenUserIsNull()
    {
        when( currentUserService.getCurrentUser() ).thenReturn( null );

        String hasUserAccess = deduplicationHelper.getUserAccessErrors(
            getTeiA(), getTeiB(),
            mergeObject );

        assertNotNull( hasUserAccess );
        assertEquals( "Missing required authority for merging tracked entities.", hasUserAccess );
    }

    @Test
    void shouldNotHasUserAccessWhenUserHasNoMergeRoles()
    {
        when( currentUserService.getCurrentUser() ).thenReturn( getNoMergeAuthsUser() );

        String hasUserAccess = deduplicationHelper.getUserAccessErrors(
            getTeiA(), getTeiB(),
            mergeObject );

        assertNotNull( hasUserAccess );
        assertEquals( "Missing required authority for merging tracked entities.", hasUserAccess );
    }

    @Test
    void shouldNotHasUserAccessWhenUserHasNoAccessToOriginalTEIType()
    {
        when( aclService.canDataWrite( user, trackedEntityTypeA ) ).thenReturn( false );

        String hasUserAccess = deduplicationHelper.getUserAccessErrors(
            getTeiA(), getTeiB(),
            mergeObject );

        assertNotNull( hasUserAccess );
        assertEquals( "Missing data write access to Tracked Entity Type.", hasUserAccess );
    }

    @Test
    void shouldNotHasUserAccessWhenUserHasNoAccessToDuplicateTEIType()
    {
        when( aclService.canDataWrite( user, trackedEntityTypeB ) ).thenReturn( false );

        String hasUserAccess = deduplicationHelper.getUserAccessErrors(
            getTeiA(), getTeiB(),
            mergeObject );

        assertNotNull( hasUserAccess );
        assertEquals( "Missing data write access to Tracked Entity Type.", hasUserAccess );
    }

    @Test
    void shouldNotHasUserAccessWhenUserHasNoAccessToRelationshipType()
    {
        when( aclService.canDataWrite( user, relationshipType ) ).thenReturn( false );

        String hasUserAccess = deduplicationHelper.getUserAccessErrors(
            getTeiA(), getTeiB(),
            mergeObject );

        assertNotNull( hasUserAccess );
        assertEquals( "Missing data write access to one or more Relationship Types.", hasUserAccess );
    }

    @Test
    void shouldNotHasUserAccessWhenUserHasNoAccessToProgramInstance()
    {
        when( aclService.canDataWrite( user, programInstance.getProgram() ) ).thenReturn( false );

        String hasUserAccess = deduplicationHelper.getUserAccessErrors(
            getTeiA(), getTeiB(),
            mergeObject );

        assertNotNull( hasUserAccess );
        assertEquals( "Missing data write access to one or more Programs.", hasUserAccess );
    }

    @Test
    void shouldNotHasUserAccessWhenUserHasNoCaptureScopeAccessToOriginalOrgUnit()
    {
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnitA ) ).thenReturn( false );

        String hasUserAccess = deduplicationHelper.getUserAccessErrors(
            getTeiA(), getTeiB(),
            mergeObject );

        assertNotNull( hasUserAccess );
        assertEquals( "Missing access to organisation unit of one or both entities.", hasUserAccess );
    }

    @Test
    void shouldNotHasUserAccessWhenUserHasNoCaptureScopeAccessToDuplicateOrgUnit()
    {
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnitB ) ).thenReturn( false );

        String hasUserAccess = deduplicationHelper.getUserAccessErrors(
            getTeiA(), getTeiB(),
            mergeObject );

        assertNotNull( hasUserAccess );
        assertEquals( "Missing access to organisation unit of one or both entities.", hasUserAccess );
    }

    @Test
    void shouldFailGenerateMergeObjectDifferentTrackedEntityType()
    {
        assertThrows( PotentialDuplicateForbiddenException.class,
            () -> deduplicationHelper.generateMergeObject( getTeiA(), getTeiB() ) );
    }

    @Test
    void shouldFailGenerateMergeObjectConflictingValue()
    {
        TrackedEntityInstance original = getTeiA();

        TrackedEntityAttributeValue attributeValueOriginal = new TrackedEntityAttributeValue();
        attributeValueOriginal.setAttribute( attribute );
        attributeValueOriginal.setEntityInstance( original );
        attributeValueOriginal.setValue( "Attribute-Original" );

        original.getTrackedEntityAttributeValues().add( attributeValueOriginal );

        TrackedEntityInstance duplicate = getTeiA();

        TrackedEntityAttributeValue attributeValueDuplicate = new TrackedEntityAttributeValue();
        attributeValueDuplicate.setAttribute( attribute );
        attributeValueDuplicate.setEntityInstance( duplicate );
        attributeValueDuplicate.setValue( "Attribute-Duplicate" );

        duplicate.getTrackedEntityAttributeValues().add( attributeValueDuplicate );

        assertThrows( PotentialDuplicateConflictException.class,
            () -> deduplicationHelper.generateMergeObject( original, duplicate ) );
    }

    @Test
    void shoudGenerateMergeObjectForAttribute()
        throws PotentialDuplicateConflictException,
        PotentialDuplicateForbiddenException
    {
        TrackedEntityInstance original = getTeiA();

        TrackedEntityAttributeValue attributeValueOriginal = new TrackedEntityAttributeValue();
        attributeValueOriginal.setAttribute( attribute );
        attributeValueOriginal.setEntityInstance( original );
        attributeValueOriginal.setValue( "Attribute-Original" );

        original.getTrackedEntityAttributeValues().add( attributeValueOriginal );

        TrackedEntityInstance duplicate = getTeiA();

        TrackedEntityAttributeValue attributeValueDuplicate = new TrackedEntityAttributeValue();
        TrackedEntityAttribute duplicateAttribute = createTrackedEntityAttribute( 'B' );
        attributeValueDuplicate.setAttribute( duplicateAttribute );
        attributeValueDuplicate.setEntityInstance( duplicate );
        attributeValueDuplicate.setValue( "Attribute-Duplicate" );

        duplicate.getTrackedEntityAttributeValues().add( attributeValueDuplicate );

        MergeObject mergeObject = deduplicationHelper.generateMergeObject( original, duplicate );

        assertFalse( mergeObject.getTrackedEntityAttributes().isEmpty() );

        mergeObject.getTrackedEntityAttributes().forEach( a -> assertEquals( duplicateAttribute.getUid(), a ) );
    }

    @Test
    void testMergeObjectRelationship()
        throws PotentialDuplicateConflictException,
        PotentialDuplicateForbiddenException
    {
        TrackedEntityInstance original = getTeiA();

        TrackedEntityInstance another = getTeiA();

        TrackedEntityInstance duplicate = getTeiA();

        Relationship anotherBaseRelationship = getRelationship();

        RelationshipItem relationshipItemAnotherTo = getRelationshipItem( anotherBaseRelationship, another );
        RelationshipItem relationshipItemAnotherFrom = getRelationshipItem( anotherBaseRelationship, duplicate );

        Relationship anotherRelationship = getRelationship( relationshipItemAnotherTo, relationshipItemAnotherFrom );
        RelationshipItem anotherRelationshipItem = getRelationshipItem( anotherRelationship, duplicate );

        duplicate.getRelationshipItems().add( anotherRelationshipItem );

        MergeObject mergeObject = deduplicationHelper.generateMergeObject( original, duplicate );

        assertTrue( mergeObject.getTrackedEntityAttributes().isEmpty() );

        assertFalse( mergeObject.getRelationships().isEmpty() );

        mergeObject.getRelationships().forEach( r -> assertEquals( anotherRelationship.getUid(), r ) );

        Relationship baseRelationship = getRelationship();

        RelationshipItem relationshipItemTo = getRelationshipItem( baseRelationship, original );
        RelationshipItem relationshipItemFrom = getRelationshipItem( baseRelationship, duplicate );

        Relationship relationship = getRelationship( relationshipItemTo, relationshipItemFrom );
        RelationshipItem relationshipItem = getRelationshipItem( relationship, duplicate );

        duplicate.getRelationshipItems().add( relationshipItem );

        mergeObject = deduplicationHelper.generateMergeObject( original, duplicate );

        assertEquals( 1, mergeObject.getRelationships().size() );

    }

    @Test
    void shouldGenerateMergeObjectWIthEnrollments()
        throws PotentialDuplicateConflictException,
        PotentialDuplicateForbiddenException
    {
        TrackedEntityInstance original = getTeiA();
        Program programA = createProgram( 'A' );
        ProgramInstance programInstanceA = createProgramInstance( programA, original, organisationUnitA );
        programInstanceA.setUid( "programInstanceA" );
        original.getProgramInstances().add( programInstanceA );

        TrackedEntityInstance duplicate = getTeiA();
        Program programB = createProgram( 'B' );
        ProgramInstance programInstanceB = createProgramInstance( programB, duplicate, organisationUnitA );
        programInstanceB.setUid( "programInstanceB" );
        duplicate.getProgramInstances().add( programInstanceB );

        MergeObject generatedMergeObject = deduplicationHelper.generateMergeObject( original, duplicate );

        assertEquals( "programInstanceB", generatedMergeObject.getEnrollments().get( 0 ) );
    }

    @Test
    void shouldFailGenerateMergeObjectEnrollmentsSameProgram()
    {
        TrackedEntityInstance original = getTeiA();

        Program program = createProgram( 'A' );
        ProgramInstance programInstanceA = createProgramInstance( program, original, organisationUnitA );
        original.getProgramInstances().add( programInstanceA );

        TrackedEntityInstance duplicate = getTeiA();
        ProgramInstance programInstanceB = createProgramInstance( program, duplicate, organisationUnitA );
        duplicate.getProgramInstances().add( programInstanceB );

        assertThrows( PotentialDuplicateConflictException.class,
            () -> deduplicationHelper.generateMergeObject( original, duplicate ) );
    }

    @Test
    void shouldFailGetDuplicateRelationshipErrorWithDuplicateRelationshipsWithTeis()
    {
        TrackedEntityInstance teiA = getTeiA();
        TrackedEntityInstance teiB = getTeiB();
        TrackedEntityInstance teiC = getTeiC();

        // A->C, B->C
        RelationshipItem fromA = new RelationshipItem();
        RelationshipItem toA = new RelationshipItem();
        RelationshipItem fromB = new RelationshipItem();
        RelationshipItem toB = new RelationshipItem();

        fromA.setTrackedEntityInstance( teiA );
        toA.setTrackedEntityInstance( teiC );
        fromB.setTrackedEntityInstance( teiB );
        toB.setTrackedEntityInstance( teiC );

        Relationship relA = new Relationship();
        Relationship relB = new Relationship();

        relA.setAutoFields();
        relB.setAutoFields();

        relA.setRelationshipType( relationshipType );
        relB.setRelationshipType( relationshipType );

        relA.setFrom( fromA );
        relA.setTo( toA );
        relB.setFrom( fromB );
        relB.setTo( toB );

        fromA.setRelationship( relA );
        toA.setRelationship( relA );

        fromB.setRelationship( relB );
        toB.setRelationship( relB );

        teiA.getRelationshipItems().add( fromA );
        teiB.getRelationshipItems().add( fromB );

        assertNotNull( deduplicationHelper.getDuplicateRelationshipError( teiA,
            teiB.getRelationshipItems().stream().map( RelationshipItem::getRelationship )
                .collect( Collectors.toSet() ) ) );
    }

    @Test
    void shouldFailGetDuplicateRelationshipErrorWithDuplicateRelationshipsWithTeisBidirectional()
    {
        TrackedEntityInstance teiA = getTeiA();
        TrackedEntityInstance teiB = getTeiB();
        TrackedEntityInstance teiC = getTeiC();

        // A->C, B->C
        RelationshipItem fromA = new RelationshipItem();
        RelationshipItem toA = new RelationshipItem();
        RelationshipItem fromB = new RelationshipItem();
        RelationshipItem toB = new RelationshipItem();

        fromA.setTrackedEntityInstance( teiC );
        toA.setTrackedEntityInstance( teiA );
        fromB.setTrackedEntityInstance( teiB );
        toB.setTrackedEntityInstance( teiC );

        Relationship relA = new Relationship();
        Relationship relB = new Relationship();

        relA.setAutoFields();
        relB.setAutoFields();

        relA.setRelationshipType( relationshipTypeBidirectional );
        relB.setRelationshipType( relationshipTypeBidirectional );

        relA.setFrom( fromA );
        relA.setTo( toA );
        relB.setFrom( fromB );
        relB.setTo( toB );

        fromA.setRelationship( relA );
        toA.setRelationship( relA );

        fromB.setRelationship( relB );
        toB.setRelationship( relB );

        teiA.getRelationshipItems().add( fromA );
        teiB.getRelationshipItems().add( fromB );

        assertNotNull( deduplicationHelper.getDuplicateRelationshipError( teiA,
            teiB.getRelationshipItems().stream().map( RelationshipItem::getRelationship )
                .collect( Collectors.toSet() ) ) );
    }

    @Test
    void shouldNotFailGetDuplicateRelationshipError()
    {
        TrackedEntityInstance teiA = getTeiA();
        TrackedEntityInstance teiB = getTeiB();
        TrackedEntityInstance teiC = getTeiC();

        // A->C, C->B
        RelationshipItem fromA = new RelationshipItem();
        RelationshipItem toA = new RelationshipItem();
        RelationshipItem fromB = new RelationshipItem();
        RelationshipItem toB = new RelationshipItem();

        fromA.setTrackedEntityInstance( teiA );
        toA.setTrackedEntityInstance( teiC );
        fromB.setTrackedEntityInstance( teiB );
        toB.setTrackedEntityInstance( teiC );

        Relationship relA = new Relationship();
        Relationship relB = new Relationship();

        relA.setAutoFields();
        relB.setAutoFields();

        relA.setRelationshipType( relationshipType );
        relB.setRelationshipType( relationshipType );

        relA.setFrom( fromA );
        relA.setTo( toA );
        relB.setFrom( fromB );
        relB.setTo( toB );

        fromA.setRelationship( relA );
        toA.setRelationship( relA );

        fromB.setRelationship( relB );
        toB.setRelationship( relB );

        teiA.getRelationshipItems().add( fromA );
        teiB.getRelationshipItems().add( fromB );

        assertNotNull( deduplicationHelper.getDuplicateRelationshipError( teiA,
            teiB.getRelationshipItems().stream().map( RelationshipItem::getRelationship )
                .collect( Collectors.toSet() ) ) );
    }

    private List<Relationship> getRelationships()
    {
        Relationship relationshipA = new Relationship();
        relationshipA.setRelationshipType( relationshipType );

        return Lists.newArrayList( relationshipA );
    }

    private List<ProgramInstance> getEnrollments()
    {
        return Lists.newArrayList( programInstance );
    }

    private TrackedEntityInstance getTeiA()
    {
        TrackedEntityInstance tei = createTrackedEntityInstance( organisationUnitA );
        tei.setTrackedEntityType( trackedEntityTypeA );

        return tei;
    }

    private TrackedEntityInstance getTeiB()
    {
        TrackedEntityInstance tei = createTrackedEntityInstance( organisationUnitB );
        tei.setTrackedEntityType( trackedEntityTypeB );

        return tei;
    }

    private TrackedEntityInstance getTeiC()
    {
        TrackedEntityInstance tei = createTrackedEntityInstance( organisationUnitB );
        tei.setTrackedEntityType( trackedEntityTypeB );

        return tei;
    }

    private User getNoMergeAuthsUser()
    {
        return makeUser( "A", Lists.newArrayList( "USELESS_AUTH" ) );
    }

    private Relationship getRelationship()
    {
        Relationship relationship = new Relationship();
        relationship.setAutoFields();
        relationship.setRelationshipType( relationshipType );

        return relationship;
    }

    private Relationship getRelationship( RelationshipItem to, RelationshipItem from )
    {
        Relationship relationship = getRelationship();
        relationship.setTo( to );
        relationship.setFrom( from );

        return relationship;
    }

    private RelationshipItem getRelationshipItem( Relationship relationship,
        TrackedEntityInstance trackedEntityInstance )
    {
        RelationshipItem relationshipItem = new RelationshipItem();
        relationshipItem.setRelationship( relationship );
        relationshipItem.setTrackedEntityInstance( trackedEntityInstance );

        return relationshipItem;
    }

}
