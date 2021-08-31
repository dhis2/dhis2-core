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
package org.hisp.dhis.deduplication;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;

@RunWith( MockitoJUnitRunner.class )
public class DeduplicationHelperTest extends DhisConvenienceTest
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
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private ProgramInstanceService programInstanceService;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private TrackedEntityType trackedEntityTypeA;

    private TrackedEntityType trackedEntityTypeB;

    private RelationshipType relationshipType;

    private TrackedEntityAttribute attribute;

    private ProgramInstance programInstance;

    private MergeObject mergeObject;

    private User user;

    @Before
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
        attribute = createTrackedEntityAttribute( 'A' );
        programInstance = createProgramInstance( createProgram( 'A' ), getTeiA(), organisationUnitA );
        mergeObject = MergeObject.builder()
            .relationships( relationshipUids )
            .trackedEntityAttributes( attributeUids )
            .enrollments( enrollmentUids )
            .build();
        user = createUser( 'A', Lists.newArrayList( "F_TRACKED_ENTITY_MERGE" ) );

        when( currentUserService.getCurrentUser() ).thenReturn( user );
        when( aclService.canDataWrite( user, trackedEntityTypeA ) ).thenReturn( true );
        when( aclService.canDataWrite( user, trackedEntityTypeB ) ).thenReturn( true );
        when( aclService.canDataWrite( user, relationshipType ) ).thenReturn( true );
        when( aclService.canDataWrite( user, attribute ) ).thenReturn( true );
        when( aclService.canDataWrite( user, programInstance ) ).thenReturn( true );
        when( relationshipService.getRelationships( relationshipUids ) ).thenReturn( getRelationships() );
        when( trackedEntityAttributeService.getTrackedEntityAttributes( attributeUids ) ).thenReturn( getAttributes() );
        when( programInstanceService.getProgramInstances( enrollmentUids ) ).thenReturn( getEnrollments() );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnitA ) ).thenReturn( true );
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnitB ) ).thenReturn( true );
    }

    @Test
    public void shouldHasUserAccess()
    {
        boolean hasUserAccess = deduplicationHelper.hasUserAccess(
            getTeiA(), getTeiB(),
            mergeObject );

        assertTrue( hasUserAccess );
    }

    @Test
    public void shouldNotHasUserAccessWhenUserIsNull()
    {
        when( currentUserService.getCurrentUser() ).thenReturn( null );

        boolean hasUserAccess = deduplicationHelper.hasUserAccess(
            getTeiA(), getTeiB(),
            mergeObject );

        assertFalse( hasUserAccess );
    }

    @Test
    public void shouldNotHasUserAccessWhenUserHasNoMergeRoles()
    {
        when( currentUserService.getCurrentUser() ).thenReturn( getNoMergeAuthsUser() );

        boolean hasUserAccess = deduplicationHelper.hasUserAccess(
            getTeiA(), getTeiB(),
            mergeObject );

        assertFalse( hasUserAccess );
    }

    @Test
    public void shouldNotHasUserAccessWhenUserHasNoAccessToOriginalTEIType()
    {
        when( aclService.canDataWrite( user, trackedEntityTypeA ) ).thenReturn( false );

        boolean hasUserAccess = deduplicationHelper.hasUserAccess(
            getTeiA(), getTeiB(),
            mergeObject );

        assertFalse( hasUserAccess );
    }

    @Test
    public void shouldNotHasUserAccessWhenUserHasNoAccessToDuplicateTEIType()
    {
        when( aclService.canDataWrite( user, trackedEntityTypeB ) ).thenReturn( false );

        boolean hasUserAccess = deduplicationHelper.hasUserAccess(
            getTeiA(), getTeiB(),
            mergeObject );

        assertFalse( hasUserAccess );
    }

    @Test
    public void shouldNotHasUserAccessWhenUserHasNoAccessToRelationshipType()
    {
        when( aclService.canDataWrite( user, relationshipType ) ).thenReturn( false );

        boolean hasUserAccess = deduplicationHelper.hasUserAccess(
            getTeiA(), getTeiB(),
            mergeObject );

        assertFalse( hasUserAccess );
    }

    @Test
    public void shouldNotHasUserAccessWhenUserHasNoAccessToAttribute()
    {
        when( aclService.canDataWrite( user, attribute ) ).thenReturn( false );

        boolean hasUserAccess = deduplicationHelper.hasUserAccess(
            getTeiA(), getTeiB(),
            mergeObject );

        assertFalse( hasUserAccess );
    }

    @Test
    public void shouldNotHasUserAccessWhenUserHasNoAccessToProgramInstance()
    {
        when( aclService.canDataWrite( user, programInstance ) ).thenReturn( false );

        boolean hasUserAccess = deduplicationHelper.hasUserAccess(
            getTeiA(), getTeiB(),
            mergeObject );

        assertFalse( hasUserAccess );
    }

    @Test
    public void shouldNotHasUserAccessWhenUserHasNoCaptureScopeAccessToOriginalOrgUnit()
    {
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnitA ) ).thenReturn( false );

        boolean hasUserAccess = deduplicationHelper.hasUserAccess(
            getTeiA(), getTeiB(),
            mergeObject );

        assertFalse( hasUserAccess );
    }

    @Test
    public void shouldNotHasUserAccessWhenUserHasNoCaptureScopeAccessToDuplicateOrgUnit()
    {
        when( organisationUnitService.isInUserHierarchyCached( user, organisationUnitB ) ).thenReturn( false );

        boolean hasUserAccess = deduplicationHelper.hasUserAccess(
            getTeiA(), getTeiB(),
            mergeObject );

        assertFalse( hasUserAccess );
    }

    private List<Relationship> getRelationships()
    {
        Relationship relationshipA = new Relationship();
        relationshipA.setRelationshipType( relationshipType );

        return Lists.newArrayList( relationshipA );
    }

    private List<TrackedEntityAttribute> getAttributes()
    {
        return Lists.newArrayList( attribute );
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

    private User getNoMergeAuthsUser()
    {
        return createUser( 'A', Lists.newArrayList( "USELESS_AUTH" ) );
    }
}
