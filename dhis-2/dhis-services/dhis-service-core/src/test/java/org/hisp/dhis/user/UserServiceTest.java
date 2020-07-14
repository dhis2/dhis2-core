package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hisp.dhis.setting.SettingKey.CAN_GRANT_OWN_USER_AUTHORITY_GROUPS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class UserServiceTest
    extends DhisSpringTest
{
    @Autowired
    private UserService userService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    private OrganisationUnit unitA;
    private OrganisationUnit unitB;
    private OrganisationUnit unitC;
    private OrganisationUnit unitD;
    private OrganisationUnit unitE;

    private UserAuthorityGroup roleA;
    private UserAuthorityGroup roleB;
    private UserAuthorityGroup roleC;

    @Override
    public void setUpTest()
        throws Exception
    {
        unitA = createOrganisationUnit( 'A' );
        unitB = createOrganisationUnit( 'B' );
        unitC = createOrganisationUnit( 'C', unitA );
        unitD = createOrganisationUnit( 'D', unitB );
        unitE = createOrganisationUnit( 'E' );

        organisationUnitService.addOrganisationUnit( unitA );
        organisationUnitService.addOrganisationUnit( unitB );
        organisationUnitService.addOrganisationUnit( unitC );
        organisationUnitService.addOrganisationUnit( unitD );
        organisationUnitService.addOrganisationUnit( unitE );

        roleA = createUserAuthorityGroup( 'A' );
        roleB = createUserAuthorityGroup( 'B' );
        roleC = createUserAuthorityGroup( 'C' );

        roleA.getAuthorities().add( "AuthA" );
        roleA.getAuthorities().add( "AuthB" );
        roleA.getAuthorities().add( "AuthC" );
        roleA.getAuthorities().add( "AuthD" );

        roleB.getAuthorities().add( "AuthA" );
        roleB.getAuthorities().add( "AuthB" );

        roleC.getAuthorities().add( "AuthC" );

        userService.addUserAuthorityGroup( roleA );
        userService.addUserAuthorityGroup( roleB );
        userService.addUserAuthorityGroup( roleC );
    }

    @Test
    public void testAddGetUser()
    {
        Set<OrganisationUnit> units = new HashSet<>();

        units.add( unitA );
        units.add( unitB );

        User userA = createUser( 'A' );
        User userB = createUser( 'B' );

        userA.setOrganisationUnits( units );
        userB.setOrganisationUnits( units );

        long idA = userService.addUser( userA );
        long idB = userService.addUser( userB );

        assertEquals( userA, userService.getUser( idA ) );
        assertEquals( userB, userService.getUser( idB ) );

        assertEquals( units, userService.getUser( idA ).getOrganisationUnits() );
        assertEquals( units, userService.getUser( idB ).getOrganisationUnits() );
    }

    @Test
    public void testUpdateUser()
    {
        User userA = createUser( 'A' );
        User userB = createUser( 'B' );

        long idA = userService.addUser( userA );
        long idB = userService.addUser( userB );

        assertEquals( userA, userService.getUser( idA ) );
        assertEquals( userB, userService.getUser( idB ) );

        userA.setSurname( "UpdatedSurnameA" );

        userService.updateUser( userA );

        assertEquals( userService.getUser( idA ).getSurname(), "UpdatedSurnameA" );
    }

    @Test
    public void testDeleteUser()
    {
        User userA = createUser( 'A' );
        User userB = createUser( 'B' );

        long idA = userService.addUser( userA );
        long idB = userService.addUser( userB );

        assertEquals( userA, userService.getUser( idA ) );
        assertEquals( userB, userService.getUser( idB ) );

        userService.deleteUser( userA );

        assertNull( userService.getUser( idA ) );
        assertNotNull( userService.getUser( idB ) );
    }

    @Test
    public void testUserByOrgUnits()
    {
        systemSettingManager.saveSystemSetting( CAN_GRANT_OWN_USER_AUTHORITY_GROUPS, true );

        User userA = createUser( 'A' );
        User userB = createUser( 'B' );
        User userC = createUser( 'C' );
        User userD = createUser( 'D' );

        userA.getOrganisationUnits().add( unitA );
        userB.getOrganisationUnits().add( unitB );
        userC.getOrganisationUnits().add( unitC );
        userD.getOrganisationUnits().add( unitD );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );
        UserCredentials credentialsC = createUserCredentials( 'C', userC );
        UserCredentials credentialsD = createUserCredentials( 'D', userD );

        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );
        userService.addUser( userD );

        userService.addUserCredentials( credentialsA );
        userService.addUserCredentials( credentialsB );
        userService.addUserCredentials( credentialsC );
        userService.addUserCredentials( credentialsD );

        UserQueryParams params = new UserQueryParams()
            .addOrganisationUnit( unitA )
            .setUser( userA );

        List<User> users = userService.getUsers( params );

        assertEquals( 1, users.size() );
        assertTrue( users.contains( userA ) );

        params = new UserQueryParams()
            .addOrganisationUnit( unitA )
            .setIncludeOrgUnitChildren( true )
            .setUser( userA );

        users = userService.getUsers( params );

        assertEquals( 2, users.size() );
        assertTrue( users.contains( userA ) );
        assertTrue( users.contains( userC ) );
    }

    @Test
    public void testGetUserOrgUnits()
    {
        systemSettingManager.saveSystemSetting( CAN_GRANT_OWN_USER_AUTHORITY_GROUPS, true );

        User currentUser = createUser( 'Z' );
        User userA = createUser( 'A' );
        User userB = createUser( 'B' );
        User userC = createUser( 'C' );
        User userD = createUser( 'D' );
        User userE = createUser( 'E' );

        currentUser.getOrganisationUnits().add( unitA );
        currentUser.getOrganisationUnits().add( unitB );
        userA.addOrganisationUnit( unitA );
        userB.addOrganisationUnit( unitB );
        userC.addOrganisationUnit( unitC );
        userD.addOrganisationUnit( unitD );
        userE.addOrganisationUnit( unitE );

        UserCredentials currentCredentials = createUserCredentials( 'Z', currentUser );
        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );
        UserCredentials credentialsC = createUserCredentials( 'C', userC );
        UserCredentials credentialsD = createUserCredentials( 'D', userD );
        UserCredentials credentialsE = createUserCredentials( 'E', userE );

        userService.addUser( currentUser );
        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );
        userService.addUser( userD );
        userService.addUser( userE );

        userService.addUserCredentials( currentCredentials );
        userService.addUserCredentials( credentialsA );
        userService.addUserCredentials( credentialsB );
        userService.addUserCredentials( credentialsC );
        userService.addUserCredentials( credentialsD );
        userService.addUserCredentials( credentialsE );

        UserQueryParams params = new UserQueryParams()
            .setUser( currentUser )
            .setUserOrgUnits( true );

        List<User> users = userService.getUsers( params );

        assertEquals( 3, users.size() );
        assertTrue( users.contains( currentUser ) );
        assertTrue( users.contains( userA ) );
        assertTrue( users.contains( userB ) );

        params = new UserQueryParams()
            .setUser( currentUser )
            .setUserOrgUnits( true )
            .setIncludeOrgUnitChildren( true );

        users = userService.getUsers( params );

        assertEquals( 5, users.size() );
        assertTrue( users.contains( currentUser ) );
        assertTrue( users.contains( userA ) );
        assertTrue( users.contains( userB ) );
        assertTrue( users.contains( userC ) );
        assertTrue( users.contains( userD ) );
    }

    @Test
    public void testManagedGroups()
    {
        systemSettingManager.saveSystemSetting( CAN_GRANT_OWN_USER_AUTHORITY_GROUPS, true );

        // TODO find way to override in parameters

        User userA = createUser( 'A' );
        User userB = createUser( 'B' );
        User userC = createUser( 'C' );
        User userD = createUser( 'D' );

        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );
        userService.addUser( userD );

        UserGroup userGroup1 = createUserGroup( 'A', Sets.newHashSet( userA, userB ) );
        UserGroup userGroup2 = createUserGroup( 'B', Sets.newHashSet( userC, userD ) );
        userA.getGroups().add( userGroup1 );
        userB.getGroups().add( userGroup1 );
        userC.getGroups().add( userGroup2 );
        userD.getGroups().add( userGroup2 );

        userGroup1.setManagedGroups( Sets.newHashSet( userGroup2 ) );
        userGroup2.setManagedByGroups( Sets.newHashSet( userGroup1 ) );

        long group1 = userGroupService.addUserGroup( userGroup1 );
        long group2 = userGroupService.addUserGroup( userGroup2 );

        assertEquals( 1, userGroupService.getUserGroup( group1 ).getManagedGroups().size() );
        assertTrue( userGroupService.getUserGroup( group1 ).getManagedGroups().contains( userGroup2 ) );
        assertEquals( 1, userGroupService.getUserGroup( group2 ).getManagedByGroups().size() );
        assertTrue( userGroupService.getUserGroup( group2 ).getManagedByGroups().contains( userGroup1 ) );

        assertTrue( userA.canManage( userGroup2 ) );
        assertTrue( userB.canManage( userGroup2 ) );
        assertFalse( userC.canManage( userGroup1 ) );
        assertFalse( userD.canManage( userGroup1 ) );

        assertTrue( userA.canManage( userC ) );
        assertTrue( userA.canManage( userD ) );
        assertTrue( userB.canManage( userC ) );
        assertTrue( userA.canManage( userD ) );
        assertFalse( userC.canManage( userA ) );
        assertFalse( userC.canManage( userB ) );

        assertTrue( userC.isManagedBy( userGroup1 ) );
        assertTrue( userD.isManagedBy( userGroup1 ) );
        assertFalse( userA.isManagedBy( userGroup2 ) );
        assertFalse( userB.isManagedBy( userGroup2 ) );

        assertTrue( userC.isManagedBy( userA ) );
        assertTrue( userC.isManagedBy( userB ) );
        assertTrue( userD.isManagedBy( userA ) );
        assertTrue( userD.isManagedBy( userB ) );
        assertFalse( userA.isManagedBy( userC ) );
        assertFalse( userA.isManagedBy( userD ) );
    }

    @Test
    public void testGetByPhoneNumber()
    {
        systemSettingManager.saveSystemSetting( CAN_GRANT_OWN_USER_AUTHORITY_GROUPS, true );

        User userA = createUser( 'A' );
        User userB = createUser( 'B' );
        User userC = createUser( 'C' );

        userA.setPhoneNumber( "73647271" );
        userB.setPhoneNumber( "23452134" );
        userC.setPhoneNumber( "14543232" );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );
        UserCredentials credentialsC = createUserCredentials( 'C', userC );

        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );

        userService.addUserCredentials( credentialsA );
        userService.addUserCredentials( credentialsB );
        userService.addUserCredentials( credentialsC );

        List<User> users = userService.getUsersByPhoneNumber( "23452134" );

        assertEquals( 1, users.size() );
        assertEquals( userB, users.get( 0 ) );
    }

    @Test
    public void testGetByUuid()
    {
        User userA = createUser( 'A' );
        User userB = createUser( 'B' );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );

        userService.addUser( userA );
        userService.addUser( userB );

        userService.addUserCredentials( credentialsA );
        userService.addUserCredentials( credentialsB );

        assertEquals( userA, userService.getUserByUuid( userA.getUuid() ) );
        assertEquals( userB, userService.getUserByUuid( userB.getUuid() ) );
    }

    @Test
    public void testGetByIdentifier()
    {
        User userA = createUser( 'A' );
        User userB = createUser( 'B' );
        User userC = createUser( 'C' );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );
        UserCredentials credentialsC = createUserCredentials( 'C', userC );

        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );

        userService.addUserCredentials( credentialsA );
        userService.addUserCredentials( credentialsB );
        userService.addUserCredentials( credentialsC );

        // Match

        assertEquals( userA, userService.getUserByIdentifier( userA.getUid() ) );
        assertEquals( userA, userService.getUserByIdentifier( userA.getUuid().toString() ) );
        assertEquals( userA, userService.getUserByIdentifier( userA.getUsername() ) );

        assertEquals( userB, userService.getUserByIdentifier( userB.getUid() ) );
        assertEquals( userB, userService.getUserByIdentifier( userB.getUuid().toString() ) );
        assertEquals( userB, userService.getUserByIdentifier( userB.getUsername() ) );

        // No match

        assertNull( userService.getUserByIdentifier( "hYg6TgAfN71" ) );
        assertNull( userService.getUserByIdentifier( "cac39761-3ef9-4774-8b1e-b96cedbc57a9" ) );
        assertNull( userService.getUserByIdentifier( "johndoe" ) );
    }

    @Test
    public void testGetOrdered()
    {
        systemSettingManager.saveSystemSetting( CAN_GRANT_OWN_USER_AUTHORITY_GROUPS, true );

        User userA = createUser( 'A' );
        User userB = createUser( 'B' );
        User userC = createUser( 'C' );

        userA.setSurname( "Yong" );
        userA.setFirstName( "Anne" );
        userA.setEmail( "lost@space.com" );
        userA.getOrganisationUnits().add( unitA );
        userB.setSurname( "Arden" );
        userB.setFirstName( "Jenny" );
        userB.setEmail( "Inside@other.com" );
        userB.getOrganisationUnits().add( unitA );
        userC.setSurname( "Smith" );
        userC.setFirstName( "Igor" );
        userC.setEmail( "home@other.com" );
        userC.getOrganisationUnits().add( unitA );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );
        UserCredentials credentialsC = createUserCredentials( 'C', userC );

        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );

        userService.addUserCredentials( credentialsA );
        userService.addUserCredentials( credentialsB );
        userService.addUserCredentials( credentialsC );

        List<User> users = userService.getUsers( new UserQueryParams().addOrganisationUnit( unitA ), Collections.singletonList( "email:idesc" ) );
        assertEquals( 3, users.size() );
        assertEquals( userA, users.get( 0 ) );
        assertEquals( userB, users.get( 1 ) );
        assertEquals( userC, users.get( 2 ) );

        users = userService.getUsers( new UserQueryParams().addOrganisationUnit( unitA ), null );
        assertEquals( 3, users.size() );
        assertEquals( userA, users.get( 2 ) );
        assertEquals( userB, users.get( 0 ) );
        assertEquals( userC, users.get( 1 ) );

        users = userService.getUsers( new UserQueryParams().addOrganisationUnit( unitA ), Collections.singletonList( "firstName:asc" ) );
        assertEquals( 3, users.size() );
        assertEquals( userA, users.get( 0 ) );
        assertEquals( userB, users.get( 2 ) );
        assertEquals( userC, users.get( 1 ) );
    }

    @Test
    public void testGetManagedGroupsLessAuthoritiesDisjointRoles()
    {
        systemSettingManager.saveSystemSetting( CAN_GRANT_OWN_USER_AUTHORITY_GROUPS, false );

        User userA = createUser( 'A' );
        User userB = createUser( 'B' );
        User userC = createUser( 'C' );
        User userD = createUser( 'D' );
        User userE = createUser( 'E' );
        User userF = createUser( 'F' );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );
        UserCredentials credentialsC = createUserCredentials( 'C', userC );
        UserCredentials credentialsD = createUserCredentials( 'D', userD );
        UserCredentials credentialsE = createUserCredentials( 'E', userE );
        UserCredentials credentialsF = createUserCredentials( 'F', userF );

        credentialsA.getUserAuthorityGroups().add( roleA );
        credentialsB.getUserAuthorityGroups().add( roleB );
        credentialsB.getUserAuthorityGroups().add( roleC );
        credentialsC.getUserAuthorityGroups().add( roleA );
        credentialsC.getUserAuthorityGroups().add( roleB );
        credentialsD.getUserAuthorityGroups().add( roleC );
        credentialsE.getUserAuthorityGroups().add( roleA );
        credentialsE.getUserAuthorityGroups().add( roleB );
        credentialsF.getUserAuthorityGroups().add( roleC );

        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );
        userService.addUser( userD );
        userService.addUser( userE );
        userService.addUser( userF );

        userService.addUserCredentials( credentialsA );
        userService.addUserCredentials( credentialsB );
        userService.addUserCredentials( credentialsC );
        userService.addUserCredentials( credentialsD );
        userService.addUserCredentials( credentialsE );
        userService.addUserCredentials( credentialsF );

        UserGroup userGroup1 = createUserGroup( 'A', Sets.newHashSet( userA, userB ) );
        UserGroup userGroup2 = createUserGroup( 'B', Sets.newHashSet( userC, userD, userE, userF ) );
        userA.getGroups().add( userGroup1 );
        userB.getGroups().add( userGroup1 );
        userC.getGroups().add( userGroup2 );
        userD.getGroups().add( userGroup2 );
        userE.getGroups().add( userGroup2 );
        userF.getGroups().add( userGroup2 );

        userGroup1.setManagedGroups( Sets.newHashSet( userGroup2 ) );
        userGroup2.setManagedByGroups( Sets.newHashSet( userGroup1 ) );

        userGroupService.addUserGroup( userGroup1 );
        userGroupService.addUserGroup( userGroup2 );

        UserQueryParams params = new UserQueryParams();
        params.setCanManage( true );
        params.setAuthSubset( true );
        params.setUser( userA );

        List<User> users = userService.getUsers( params );

        assertEquals( 2, users.size() );
        assertTrue( users.contains( userD ) );
        assertTrue( users.contains( userF ) );

        assertEquals( 2, userService.getUserCount( params ) );

        params.setUser( userB );

        users = userService.getUsers( params);

        assertEquals( 0, users.size() );

        assertEquals( 0, userService.getUserCount( params ) );

        params.setUser( userC );

        users = userService.getUsers( params);

        assertEquals( 0, users.size() );

        assertEquals( 0, userService.getUserCount( params ) );
    }

    @Test
    public void testGetManagedGroupsSearch()
    {
        systemSettingManager.saveSystemSetting( CAN_GRANT_OWN_USER_AUTHORITY_GROUPS, true );

        User userA = createUser( 'A' );
        User userB = createUser( 'B' );
        User userC = createUser( 'C' );
        User userD = createUser( 'D' );
        User userE = createUser( 'E' );
        User userF = createUser( 'F' );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );
        UserCredentials credentialsC = createUserCredentials( 'C', userC );
        UserCredentials credentialsD = createUserCredentials( 'D', userD );
        UserCredentials credentialsE = createUserCredentials( 'E', userE );
        UserCredentials credentialsF = createUserCredentials( 'F', userF );

        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );
        userService.addUser( userD );
        userService.addUser( userE );
        userService.addUser( userF );

        userService.addUserCredentials( credentialsA );
        userService.addUserCredentials( credentialsB );
        userService.addUserCredentials( credentialsC );
        userService.addUserCredentials( credentialsD );
        userService.addUserCredentials( credentialsE );
        userService.addUserCredentials( credentialsF );

        UserQueryParams params = new UserQueryParams();
        params.setQuery( "rstnameA" );

        List<User> users = userService.getUsers( params );

        assertEquals( 1, users.size() );
        assertTrue( users.contains( userA ) );

        assertEquals( 1, userService.getUserCount( params ) );
    }

    @Test
    public void testGetManagedGroupsSelfRegistered()
    {
        systemSettingManager.saveSystemSetting( CAN_GRANT_OWN_USER_AUTHORITY_GROUPS, true );

        User userA = createUser( 'A' );
        User userB = createUser( 'B' );
        User userC = createUser( 'C' );
        User userD = createUser( 'D' );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );
        UserCredentials credentialsC = createUserCredentials( 'C', userC );
        UserCredentials credentialsD = createUserCredentials( 'D', userD );

        credentialsA.setSelfRegistered( true );
        credentialsC.setSelfRegistered( true );

        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );
        userService.addUser( userD );

        userService.addUserCredentials( credentialsA );
        userService.addUserCredentials( credentialsB );
        userService.addUserCredentials( credentialsC );
        userService.addUserCredentials( credentialsD );

        UserQueryParams params = new UserQueryParams();
        params.setSelfRegistered( true );

        List<User> users = userService.getUsers( params );

        assertEquals( 2, users.size() );
        assertTrue( users.contains( userA ) );
        assertTrue( users.contains( userC ) );

        assertEquals( 2, userService.getUserCount( params ) );
    }

    @Test
    public void testGetManagedGroupsOrganisationUnit()
    {
        systemSettingManager.saveSystemSetting( CAN_GRANT_OWN_USER_AUTHORITY_GROUPS, true );

        User userA = createUser( 'A' );
        User userB = createUser( 'B' );
        User userC = createUser( 'C' );
        User userD = createUser( 'D' );

        userA.getOrganisationUnits().add( unitA );
        userA.getOrganisationUnits().add( unitB );
        userB.getOrganisationUnits().add( unitB );
        userC.getOrganisationUnits().add( unitA );
        userD.getOrganisationUnits().add( unitB );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );
        UserCredentials credentialsC = createUserCredentials( 'C', userC );
        UserCredentials credentialsD = createUserCredentials( 'D', userD );

        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );
        userService.addUser( userD );

        userService.addUserCredentials( credentialsA );
        userService.addUserCredentials( credentialsB );
        userService.addUserCredentials( credentialsC );
        userService.addUserCredentials( credentialsD );

        UserQueryParams params = new UserQueryParams();
        params.getOrganisationUnits().add( unitA );

        List<User> users = userService.getUsers( params );

        assertEquals( 2, users.size() );
        assertTrue( users.contains( userA ) );
        assertTrue( users.contains( userC ) );

        assertEquals( 2, userService.getUserCount( params ) );
    }

    @Test
    public void testGetInvitations()
    {
        systemSettingManager.saveSystemSetting( CAN_GRANT_OWN_USER_AUTHORITY_GROUPS, true );

        User userA = createUser( 'A' );
        User userB = createUser( 'B' );
        User userC = createUser( 'C' );
        User userD = createUser( 'D' );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );
        UserCredentials credentialsC = createUserCredentials( 'C', userC );
        UserCredentials credentialsD = createUserCredentials( 'D', userD );

        credentialsB.setInvitation( true );
        credentialsD.setInvitation( true );

        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );
        userService.addUser( userD );

        userService.addUserCredentials( credentialsA );
        userService.addUserCredentials( credentialsB );
        userService.addUserCredentials( credentialsC );
        userService.addUserCredentials( credentialsD );

        UserQueryParams params = new UserQueryParams();
        params.setInvitationStatus( UserInvitationStatus.ALL );

        List<User> users = userService.getUsers( params );

        assertEquals( 2, users.size() );
        assertTrue( users.contains( userB ) );
        assertTrue( users.contains( userD ) );

        assertEquals( 2, userService.getUserCount( params ) );

        params.setInvitationStatus( UserInvitationStatus.EXPIRED );

        users = userService.getUsers( params );

        assertEquals( 0, users.size() );

        assertEquals( 0, userService.getUserCount( params ) );
    }

    @Test
    public void testGetExpiringUser()
    {
        User userA = createUser( 'A' );
        User userB = createUser( 'B' );
        User userC = createUser( 'C' );
        User userD = createUser( 'D' );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );
        UserCredentials credentialsC = createUserCredentials( 'C', userC );
        UserCredentials credentialsD = createUserCredentials( 'D', userD );

        credentialsB.setDisabled( true );
        credentialsD.setDisabled( true );

        userService.addUser( userA );
        userService.addUser( userB );
        userService.addUser( userC );
        userService.addUser( userD );

        userService.addUserCredentials( credentialsA );
        userService.addUserCredentials( credentialsB );
        userService.addUserCredentials( credentialsC );
        userService.addUserCredentials( credentialsD );

        List<User> users = userService.getExpiringUsers();

        assertEquals( 2, users.size() );
    }
}
