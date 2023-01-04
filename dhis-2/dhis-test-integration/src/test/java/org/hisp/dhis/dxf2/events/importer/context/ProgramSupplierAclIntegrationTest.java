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
package org.hisp.dhis.dxf2.events.importer.context;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.dxf2.common.ImportOptions.getDefaultImportOptions;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Luciano Fiandesio
 */
class ProgramSupplierAclIntegrationTest extends TransactionalIntegrationTest
{

    @Autowired
    private ProgramSupplier programSupplier;

    @Autowired
    private UserService _userService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private AclService aclService;

    private Event event = new Event();

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;
        createAndInjectAdminUser();
    }

    //
    // PROGRAM ACL TESTS
    // ----------------------------------------------------------------------------
    //
    @Test
    void verifyUserHasNoWriteAccessToProgram()
    {
        // Given
        final User demo = createUserWithAuth( "demo" );
        final Program program = createProgram( 'A' );
        program.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( program, false );
        dbmsManager.flushSession();
        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );
        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertFalse( aclService.canDataWrite( demo, programs.get( program.getUid() ) ) );
    }

    @Test
    void verifyUserHasWriteAccessToProgramForUserAccess()
    {
        // Given
        final User user = createUserWithAuth( "A" );
        final Program program = createProgram( 'A' );
        UserAccess userAccess = new UserAccess( user, AccessStringHelper.DATA_READ_WRITE );
        Set<UserAccess> userAccesses = new HashSet<>();
        userAccesses.add( userAccess );
        program.getSharing().setUserAccesses( userAccesses );
        manager.save( program, false );
        manager.flush();
        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );
        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertTrue( aclService.canDataWrite( user, programs.get( program.getUid() ) ) );
    }

    @Test
    void verifyUserHasWriteAccessToProgramForUserGroupAccess()
    {
        // Given
        final User user = createUserWithAuth( "A" );
        final Program program = createProgram( 'A' );
        UserGroup userGroup = new UserGroup( "test-group", singleton( user ) );
        manager.save( userGroup, true );
        user.getGroups().add( userGroup );
        UserGroupAccess userGroupAccess = new UserGroupAccess( userGroup, AccessStringHelper.DATA_READ_WRITE );
        program.getSharing().addUserGroupAccess( userGroupAccess );
        manager.save( program, false );
        manager.flush();
        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );
        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertTrue( aclService.canDataWrite( user, programs.get( program.getUid() ) ) );
    }

    @Test
    void verifyUserHasWriteAccessToProgramForSharing()
    {
        // Given
        final User user = createUserWithAuth( "A" );
        final Program program = createProgram( 'A' );
        program.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.save( program, false );
        manager.flush();
        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );
        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertTrue( aclService.canDataWrite( user, programs.get( program.getUid() ) ) );
    }

    @Test
    void verifyUserHasNoWriteAccessToProgramStage()
    {
        // Given
        final User demo = createUserWithAuth( "demo" );
        final ProgramStage programStage = createProgramStage( 'A', 1 );
        programStage.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( programStage );
        final Program program = createProgram( 'A' );
        program.setProgramStages( singleton( programStage ) );
        program.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( program, false );
        manager.flush();
        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );
        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertFalse( aclService.canDataWrite( demo, getProgramStage( programs.get( program.getUid() ) ) ) );
    }

    @Test
    void verifyUserHasWriteAccessToProgramStageForUserAccess()
    {
        // Given
        final User user = createUserWithAuth( "user2" );
        final ProgramStage programStage = createProgramStage( 'B', 1 );
        UserAccess userAccess = new UserAccess( user, AccessStringHelper.DATA_READ_WRITE );
        programStage.getSharing().addUserAccess( userAccess );
        manager.save( programStage, false );
        final Program program = createProgram( 'A' );
        program.setProgramStages( singleton( programStage ) );
        program.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( program, false );
        manager.flush();
        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );
        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertTrue( aclService.canDataWrite( user, getProgramStage( programs.get( program.getUid() ) ) ) );
    }

    @Test
    void verifyUserHasWriteAccessToProgramStageForGroupAccess()
    {
        // Given
        final User user = createUserWithAuth( "user1" );
        final ProgramStage programStage = createProgramStage( 'A', 1 );
        programStage.setPublicAccess( AccessStringHelper.DEFAULT );
        UserGroup userGroup = new UserGroup( "test-group-programstage", singleton( user ) );
        manager.save( userGroup, true );
        user.getGroups().add( userGroup );
        programStage.getSharing().addUserGroupAccess(
            new org.hisp.dhis.user.sharing.UserGroupAccess( userGroup, AccessStringHelper.DATA_READ_WRITE ) );
        manager.save( programStage, false );
        final Program program = createProgram( 'A' );
        program.setProgramStages( singleton( programStage ) );
        program.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( program, false );
        manager.flush();
        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );
        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertTrue( aclService.canDataWrite( user, getProgramStage( programs.get( program.getUid() ) ) ) );
    }

    @Test
    void verifyUserHasWriteAccessToProgramStageForSharing()
    {
        // Given
        final User user = createUserWithAuth( "user1" );
        final ProgramStage programStage = createProgramStage( 'A', 1 );
        programStage.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.save( programStage, false );
        final Program program = createProgram( 'A' );
        program.setProgramStages( singleton( programStage ) );
        program.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( program, false );
        manager.flush();
        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );
        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertTrue( aclService.canDataWrite( user, getProgramStage( programs.get( program.getUid() ) ) ) );
    }

    //
    // TRACKED ENTITY TYPE ACL TESTS
    // ----------------------------------------------------------------------------
    //
    @Test
    void verifyUserHasNoWriteAccessToTrackedEntityType()
    {
        // Given
        final User demo = createUserWithAuth( "demo" );
        final TrackedEntityType tet = createTrackedEntityType( 'A' );
        tet.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( tet );
        final Program program = createProgram( 'A' );
        program.setTrackedEntityType( tet );
        program.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( program, false );
        manager.flush();
        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );
        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertFalse( aclService.canDataWrite( demo, getTrackedEntityType( programs.get( program.getUid() ) ) ) );
    }

    @Test
    void verifyUserHasWriteAccessToTrackedEntityTypeForUserAccess()
    {
        // Given
        final User user = createUserWithAuth( "A" );
        final TrackedEntityType tet = createTrackedEntityType( 'A' );
        manager.save( tet );
        UserAccess userAccess = new UserAccess( user, AccessStringHelper.DATA_READ_WRITE );
        tet.getSharing().addUserAccess( userAccess );
        manager.save( tet, false );
        final Program program = createProgram( 'A' );
        program.setTrackedEntityType( tet );
        program.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( program, false );
        manager.flush();
        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );
        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertTrue( aclService.canDataWrite( user, getTrackedEntityType( programs.get( program.getUid() ) ) ) );
    }

    @Test
    void verifyUserHasWriteAccessToTrackedEntityTypeForGroupAccess()
    {
        // Given
        final User user = createUserWithAuth( "user1" );
        final TrackedEntityType tet = createTrackedEntityType( 'A' );
        manager.save( tet );
        UserGroup userGroup = new UserGroup( "test-group-tet", singleton( user ) );
        manager.save( userGroup, true );
        user.getGroups().add( userGroup );
        UserGroupAccess userGroupAccess = new UserGroupAccess( userGroup, AccessStringHelper.DATA_READ_WRITE );
        tet.getSharing().addUserGroupAccess( userGroupAccess );
        manager.save( tet, false );
        final Program program = createProgram( 'A' );
        program.setTrackedEntityType( tet );
        manager.save( program, false );
        manager.flush();
        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );
        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertTrue( aclService.canDataWrite( user, getTrackedEntityType( programs.get( program.getUid() ) ) ) );
    }

    @Test
    void verifyUserHasWriteAccessToTrackedEntityTypeForSharing()
    {
        // Given
        final User user = createUserWithAuth( "user1" );
        final TrackedEntityType tet = createTrackedEntityType( 'A' );
        tet.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.save( tet, false );
        final Program program = createProgram( 'A' );
        program.setTrackedEntityType( tet );
        program.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.save( program, false );
        manager.flush();
        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );
        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertTrue( aclService.canDataWrite( user, getTrackedEntityType( programs.get( program.getUid() ) ) ) );
    }

    private ProgramStage getProgramStage( Program program )
    {
        assertThat( program.getProgramStages(), hasSize( 1 ) );
        return program.getProgramStages().iterator().next();
    }

    private TrackedEntityType getTrackedEntityType( Program program )
    {
        assertThat( program.getTrackedEntityType(), is( notNullValue() ) );
        return program.getTrackedEntityType();
    }
}
