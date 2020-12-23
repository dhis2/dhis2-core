package org.hisp.dhis.dxf2.events.importer.context;


import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccess;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.dxf2.common.ImportOptions.getDefaultImportOptions;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Luciano Fiandesio
 */
public class ProgramSupplierAclIntegrationTest extends TransactionalIntegrationTest
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
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;
        createAndInjectAdminUser();
    }

    //
    // PROGRAM ACL TESTS ----------------------------------------------------------------------------
    //

    @Test
    public void verifyUserHasNoWriteAccessToProgram()
    {
        // Given
        final User demo = createUser( "demo" );
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
    public void verifyUserHasWriteAccessToProgramForUserAccess()
    {
        // Given
        final User user = createUser( "A" );
        final Program program = createProgram( 'A' );

        UserAccess userAccess = new UserAccess();
        userAccess.setUser( user );
        userAccess.setAccess( AccessStringHelper.DATA_READ_WRITE );

        Set<UserAccess> userAccesses = new HashSet<>();
        userAccesses.add( userAccess );
        program.setUserAccesses( userAccesses );
        manager.save( program, false );

        manager.flush();

        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );

        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertTrue( aclService.canDataWrite( user, programs.get( program.getUid() ) ) );
    }

    @Test
    public void verifyUserHasWriteAccessToProgramForUserGroupAccess()
    {
        // Given
        final User user = createUser( "A" );
        final Program program = createProgram( 'A' );

        UserGroup userGroup = new UserGroup( "test-group", singleton( user ) );
        manager.save( userGroup, true );
        user.getGroups().add( userGroup );

        UserGroupAccess userGroupAccess = new UserGroupAccess();
        userGroupAccess.setUserGroup( userGroup );
        userGroupAccess.setAccess( AccessStringHelper.DATA_READ_WRITE );

        program.setUserGroupAccesses( singleton( userGroupAccess ) );
        manager.save( program, false );

        manager.flush();

        // When
        final Map<String, Program> programs = programSupplier.get( getDefaultImportOptions(), singletonList( event ) );

        // Then
        assertThat( programs.keySet(), hasSize( 1 ) );
        assertTrue( aclService.canDataWrite( user, programs.get( program.getUid() ) ) );
    }

    @Test
    public void verifyUserHasWriteAccessToProgramForSharing()
    {
        // Given
        final User user = createUser( "A" );
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
    public void verifyUserHasNoWriteAccessToProgramStage()
    {
        // Given
        final User demo = createUser( "demo" );
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
    public void verifyUserHasWriteAccessToProgramStageForUserAccess()
    {
        // Given
        final User user = createUser( "user2" );

        final ProgramStage programStage = createProgramStage( 'B', 1 );
        
        UserAccess userAccess = new UserAccess();
        userAccess.setUser( user );
        userAccess.setAccess( AccessStringHelper.DATA_READ_WRITE );
        programStage.setUserAccesses( singleton( userAccess ) );

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
    public void verifyUserHasWriteAccessToProgramStageForGroupAccess()
    {
        // Given
        final User user = createUser( "user1" );

        final ProgramStage programStage = createProgramStage( 'A', 1 );
        programStage.setPublicAccess( AccessStringHelper.DEFAULT );

        UserGroup userGroup = new UserGroup( "test-group-programstage", singleton( user ) );
        manager.save( userGroup, true );

        user.getGroups().add( userGroup );

        programStage.getSharing().addUserGroupAccess( new org.hisp.dhis.user.sharing.UserGroupAccess( userGroup, AccessStringHelper.DATA_READ_WRITE ) );
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
    public void verifyUserHasWriteAccessToProgramStageForSharing()
    {
        // Given
        final User user = createUser( "user1" );

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
    // TRACKED ENTITY TYPE ACL TESTS ----------------------------------------------------------------------------
    //

    @Test
    public void verifyUserHasNoWriteAccessToTrackedEntityType()
    {
        // Given
        final User demo = createUser( "demo" );
        final TrackedEntityType tet = createTrackedEntityType('A');
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
    public void verifyUserHasWriteAccessToTrackedEntityTypeForUserAccess()
    {
        // Given
        final User user = createUser( "A" );
        final TrackedEntityType tet = createTrackedEntityType('A');
        manager.save( tet );

        UserAccess userAccess = new UserAccess();
        userAccess.setUser( user );
        userAccess.setAccess( AccessStringHelper.DATA_READ_WRITE );

        tet.setUserAccesses( Collections.singleton( userAccess ) );
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
    public void verifyUserHasWriteAccessToTrackedEntityTypeForGroupAccess()
    {
        // Given
        final User user = createUser( "user1" );

        final TrackedEntityType tet = createTrackedEntityType('A');
        manager.save( tet );

        UserGroup userGroup = new UserGroup( "test-group-tet", singleton( user ) );
        manager.save( userGroup, true );
        user.getGroups().add( userGroup );

        UserGroupAccess userGroupAccess = new UserGroupAccess();
        userGroupAccess.setUserGroup( userGroup );
        userGroupAccess.setAccess( AccessStringHelper.DATA_READ_WRITE );

        tet.setUserGroupAccesses( singleton( userGroupAccess ) );
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
    public void verifyUserHasWriteAccessToTrackedEntityTypeForSharing()
    {
        // Given
        final User user = createUser( "user1" );

        final TrackedEntityType tet = createTrackedEntityType('A');
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
