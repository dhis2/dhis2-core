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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DeduplicationServiceMergeIntegrationTest extends IntegrationTestBase
{
    @Autowired
    private DeduplicationService deduplicationService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramService programService;

    @Override
    public void setUpTest()
    {
        super.userService = this.userService;
    }

    @Test
    void shouldManualMergeWithAuthorityAll()
        throws PotentialDuplicateConflictException,
        PotentialDuplicateForbiddenException
    {
        OrganisationUnit ou = createOrganisationUnit( "OU_A" );
        organisationUnitService.addOrganisationUnit( ou );
        User user = createUser( new HashSet<>( Collections.singletonList( ou ) ), UserRole.AUTHORITY_ALL );
        injectSecurityContext( user );

        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );
        TrackedEntityInstance original = createTrackedEntityInstance( ou );
        TrackedEntityInstance duplicate = createTrackedEntityInstance( ou );
        original.setTrackedEntityType( trackedEntityType );
        duplicate.setTrackedEntityType( trackedEntityType );
        trackedEntityInstanceService.addTrackedEntityInstance( original );
        trackedEntityInstanceService.addTrackedEntityInstance( duplicate );
        Program program = createProgram( 'A' );
        Program program1 = createProgram( 'B' );
        programService.addProgram( program );
        programService.addProgram( program1 );
        Enrollment enrollment1 = createEnrollment( program, original, ou );
        Enrollment enrollment2 = createEnrollment( program1, duplicate, ou );
        enrollmentService.addEnrollment( enrollment1 );
        enrollmentService.addEnrollment( enrollment2 );
        original.getEnrollments().add( enrollment1 );
        duplicate.getEnrollments().add( enrollment2 );
        trackedEntityInstanceService.updateTrackedEntityInstance( original );
        trackedEntityInstanceService.updateTrackedEntityInstance( duplicate );
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( original.getUid(), duplicate.getUid() );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        DeduplicationMergeParams deduplicationMergeParams = DeduplicationMergeParams.builder()
            .potentialDuplicate( potentialDuplicate ).original( original ).duplicate( duplicate ).build();
        Date lastUpdatedOriginal = trackedEntityInstanceService.getTrackedEntityInstance( original.getUid() )
            .getLastUpdated();
        deduplicationService.autoMerge( deduplicationMergeParams );
        assertEquals( deduplicationService.getPotentialDuplicateByUid( potentialDuplicate.getUid() ).getStatus(),
            DeduplicationStatus.MERGED );
        assertTrue( trackedEntityInstanceService.getTrackedEntityInstance( original.getUid() ).getLastUpdated()
            .getTime() > lastUpdatedOriginal.getTime() );
    }

    @Test
    void shouldManualMergeWithUserGroupOfProgram()
        throws PotentialDuplicateConflictException,
        PotentialDuplicateForbiddenException
    {
        OrganisationUnit ou = createOrganisationUnit( "OU_A" );
        organisationUnitService.addOrganisationUnit( ou );
        User user = createAndAddUser( true, "userB", ou, "F_TRACKED_ENTITY_MERGE" );
        injectSecurityContext( user );
        Sharing sharing = getUserSharing( user, AccessStringHelper.FULL );
        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );
        trackedEntityType.setSharing( sharing );
        trackedEntityTypeService.updateTrackedEntityType( trackedEntityType );
        TrackedEntityInstance original = createTrackedEntityInstance( ou );
        TrackedEntityInstance duplicate = createTrackedEntityInstance( ou );
        original.setTrackedEntityType( trackedEntityType );
        duplicate.setTrackedEntityType( trackedEntityType );
        trackedEntityInstanceService.addTrackedEntityInstance( original );
        trackedEntityInstanceService.addTrackedEntityInstance( duplicate );
        Program program = createProgram( 'A' );
        Program program1 = createProgram( 'B' );
        programService.addProgram( program );
        programService.addProgram( program1 );
        program.setSharing( sharing );
        program1.setSharing( sharing );
        Enrollment enrollment1 = createEnrollment( program, original, ou );
        Enrollment enrollment2 = createEnrollment( program1, duplicate, ou );
        enrollmentService.addEnrollment( enrollment1 );
        enrollmentService.addEnrollment( enrollment2 );
        enrollmentService.updateEnrollment( enrollment1 );
        enrollmentService.updateEnrollment( enrollment2 );
        original.getEnrollments().add( enrollment1 );
        duplicate.getEnrollments().add( enrollment2 );
        trackedEntityInstanceService.updateTrackedEntityInstance( original );
        trackedEntityInstanceService.updateTrackedEntityInstance( duplicate );
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( original.getUid(), duplicate.getUid() );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        DeduplicationMergeParams deduplicationMergeParams = DeduplicationMergeParams.builder()
            .potentialDuplicate( potentialDuplicate ).original( original ).duplicate( duplicate ).build();
        Date lastUpdatedOriginal = trackedEntityInstanceService.getTrackedEntityInstance( original.getUid() )
            .getLastUpdated();
        deduplicationService.autoMerge( deduplicationMergeParams );
        assertEquals( deduplicationService.getPotentialDuplicateByUid( potentialDuplicate.getUid() ).getStatus(),
            DeduplicationStatus.MERGED );
        assertTrue( trackedEntityInstanceService.getTrackedEntityInstance( original.getUid() ).getLastUpdated()
            .getTime() > lastUpdatedOriginal.getTime() );
    }

    private Sharing getUserSharing( User user, String accessStringHelper )
    {
        UserGroup userGroup = new UserGroup();
        userGroup.setName( "UserGroupA" );
        user.getGroups().add( userGroup );
        Map<String, org.hisp.dhis.user.sharing.UserAccess> userSharing = new HashMap<>();
        userSharing.put( user.getUid(), new org.hisp.dhis.user.sharing.UserAccess( user, AccessStringHelper.DEFAULT ) );
        Map<String, UserGroupAccess> userGroupSharing = new HashMap<>();
        userGroupSharing.put( userGroup.getUid(), new UserGroupAccess( userGroup, accessStringHelper ) );
        return Sharing.builder().external( false ).publicAccess( AccessStringHelper.DEFAULT ).owner( "testOwner" )
            .userGroups( userGroupSharing ).users( userSharing ).build();
    }

    private User createUser( HashSet<OrganisationUnit> ou, String... authorities )
    {
        User user = createUserWithAuth( "testUser", authorities );
        user.setOrganisationUnits( ou );
        return user;
    }
}
