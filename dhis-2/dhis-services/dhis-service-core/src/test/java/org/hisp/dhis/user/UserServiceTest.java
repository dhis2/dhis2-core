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
package org.hisp.dhis.user;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.setting.SettingKey.CAN_GRANT_OWN_USER_AUTHORITY_GROUPS;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
        super.userService = userService;

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

    private UserQueryParams getDefaultParams()
    {
        return new UserQueryParams()
            .setCanSeeOwnUserAuthorityGroups( true );
    }

    @Test
    public void testAddGetUser()
    {
        User userA = addUser( 'A', unitA, unitB );
        User userB = addUser( 'B', unitA, unitB );

        Set<OrganisationUnit> expected = new HashSet<>( asList( unitA, unitB ) );
        assertEquals( expected, userService.getUser( userA.getId() ).getOrganisationUnits() );
        assertEquals( expected, userService.getUser( userB.getId() ).getOrganisationUnits() );
    }

    @Test
    public void testGetUserCredentialsByUsernames()
    {
        addUser( 'A' );
        addUser( 'B' );

        assertEquals( 2, userService.getUserCredentialsByUsernames( asList( "UsernameA", "UsernameB" ) ).size() );
        assertEquals( 2,
            userService.getUserCredentialsByUsernames( asList( "UsernameA", "UsernameB", "usernameX" ) ).size() );
        assertEquals( 0, userService.getUserCredentialsByUsernames( asList( "usernameC" ) ).size() );
    }

    @Test
    public void testUpdateUser()
    {
        User userA = addUser( 'A' );
        User userB = addUser( 'B' );

        assertEquals( userA, userService.getUser( userA.getId() ) );
        assertEquals( userB, userService.getUser( userB.getId() ) );

        userA.setSurname( "UpdatedSurnameA" );
        userService.updateUser( userA );

        assertEquals( "UpdatedSurnameA", userService.getUser( userA.getId() ).getSurname() );
    }

    @Test
    public void testDeleteUser()
    {
        User userA = addUser( 'A' );
        User userB = addUser( 'B' );

        assertEquals( userA, userService.getUser( userA.getId() ) );
        assertEquals( userB, userService.getUser( userB.getId() ) );

        userService.deleteUser( userA );

        assertNull( userService.getUser( userA.getId() ) );
        assertNotNull( userService.getUser( userB.getId() ) );
    }

    @Test
    public void testUserByQuery()
    {
        User userA = addUser( 'A', credentials -> credentials.getUser().setFirstName( "Chris" ) );
        User userB = addUser( 'B', credentials -> credentials.getUser().setFirstName( "Chris" ) );

        assertContainsOnly( userService.getUsers( getDefaultParams().setQuery( "Chris" ) ), userA, userB );
        assertContainsOnly( userService.getUsers( getDefaultParams().setQuery( "hris SURNAM" ) ), userA, userB );
        assertContainsOnly( userService.getUsers( getDefaultParams().setQuery( "hris SurnameA" ) ), userA );
        assertContainsOnly( userService.getUsers( getDefaultParams().setQuery( "urnameB" ) ), userB );
        assertContainsOnly( userService.getUsers( getDefaultParams().setQuery( "MAilA" ) ), userA );
        assertContainsOnly( userService.getUsers( getDefaultParams().setQuery( "userNAME" ) ), userA, userB );
        assertContainsOnly( userService.getUsers( getDefaultParams().setQuery( "ernameA" ) ), userA );
    }

    @Test
    public void testUserByOrgUnits()
    {
        User userA = addUser( 'A', unitA );
        addUser( 'B', unitB );
        User userC = addUser( 'C', unitC );
        addUser( 'D', unitD );

        UserQueryParams params = getDefaultParams()
            .addOrganisationUnit( unitA )
            .setUser( userA );
        assertContainsOnly( userService.getUsers( params ), userA );

        params = getDefaultParams()
            .addOrganisationUnit( unitA )
            .setIncludeOrgUnitChildren( true )
            .setUser( userA );
        assertContainsOnly( userService.getUsers( params ), userA, userC );
    }

    @Test
    public void testUserByDataViewOrgUnits()
    {
        User userA = addUser( 'A', unitA );
        userA.getDataViewOrganisationUnits().add( unitA );
        userService.updateUser( userA );

        User userB = addUser( 'B', unitB );
        userB.getDataViewOrganisationUnits().add( unitA );
        userService.updateUser( userB );

        User userC = addUser( 'C', unitC );
        userC.getDataViewOrganisationUnits().add( unitC );
        userService.updateUser( userC );

        User userD = addUser( 'D', unitD );
        userD.getDataViewOrganisationUnits().add( unitD );
        userService.updateUser( userD );

        UserQueryParams params = getDefaultParams()
            .addDataViewOrganisationUnit( unitA )
            .setUser( userA );
        assertContainsOnly( userService.getUsers( params ), userA, userB );

        params = getDefaultParams()
            .addDataViewOrganisationUnit( unitA )
            .setIncludeOrgUnitChildren( true )
            .setUser( userA );
        assertContainsOnly( userService.getUsers( params ), userA, userB, userC );
    }

    @Test
    public void testUserByTeiSearchOrgUnits()
    {
        User userA = addUser( 'A', unitA );
        userA.getTeiSearchOrganisationUnits().add( unitA );
        userService.updateUser( userA );

        User userB = addUser( 'B', unitB );
        userB.getTeiSearchOrganisationUnits().add( unitA );
        userService.updateUser( userB );

        User userC = addUser( 'C', unitC );
        userC.getTeiSearchOrganisationUnits().add( unitC );
        userService.updateUser( userC );

        User userD = addUser( 'D', unitD );
        userD.getTeiSearchOrganisationUnits().add( unitD );
        userService.updateUser( userD );

        UserQueryParams params = getDefaultParams()
            .addTeiSearchOrganisationUnit( unitA )
            .setUser( userA );
        assertContainsOnly( userService.getUsers( params ), userA, userB );

        params = getDefaultParams()
            .addTeiSearchOrganisationUnit( unitA )
            .setIncludeOrgUnitChildren( true )
            .setUser( userA );
        assertContainsOnly( userService.getUsers( params ), userA, userB, userC );
    }

    @Test
    public void testUserByUserGroups()
    {
        User userA = addUser( 'A' );
        User userB = addUser( 'B' );
        User userC = addUser( 'C' );
        User userD = addUser( 'D' );

        UserGroup ugA = createUserGroup( 'A', newHashSet( userA, userB ) );
        UserGroup ugB = createUserGroup( 'B', newHashSet( userB, userC ) );
        UserGroup ugC = createUserGroup( 'C', newHashSet( userD ) );

        userGroupService.addUserGroup( ugA );
        userGroupService.addUserGroup( ugB );
        userGroupService.addUserGroup( ugC );

        assertContainsOnly(
            userService.getUsers( getDefaultParams().setUserGroups( newHashSet( ugA ) ) ),
            userA, userB );

        assertContainsOnly(
            userService.getUsers( getDefaultParams().setUserGroups( newHashSet( ugA, ugB ) ) ),
            userA, userB, userC );

        assertContainsOnly(
            userService.getUsers( getDefaultParams().setUserGroups( newHashSet( ugA, ugC ) ) ),
            userA, userB, userD );
    }

    @Test
    public void testGetUserOrgUnits()
    {
        User currentUser = addUser( 'Z', unitA, unitB );
        User userA = addUser( 'A', unitA );
        User userB = addUser( 'B', unitB );
        User userC = addUser( 'C', unitC );
        User userD = addUser( 'D', unitD );
        addUser( 'E', unitE );

        UserQueryParams params = getDefaultParams()
            .setUser( currentUser )
            .setUserOrgUnits( true );
        assertContainsOnly( userService.getUsers( params ), currentUser, userA, userB );

        params = getDefaultParams()
            .setUser( currentUser )
            .setUserOrgUnits( true )
            .setIncludeOrgUnitChildren( true );
        assertContainsOnly( userService.getUsers( params ), currentUser, userA, userB, userC, userD );
    }

    @Test
    public void testManagedGroups()
    {
        systemSettingManager.saveSystemSetting( CAN_GRANT_OWN_USER_AUTHORITY_GROUPS, true );

        // TODO find way to override in parameters

        User userA = addUser( 'A' );
        User userB = addUser( 'B' );
        User userC = addUser( 'C' );
        User userD = addUser( 'D' );

        UserGroup userGroup1 = createUserGroup( 'A', newHashSet( userA, userB ) );
        UserGroup userGroup2 = createUserGroup( 'B', newHashSet( userC, userD ) );
        userA.getGroups().add( userGroup1 );
        userB.getGroups().add( userGroup1 );
        userC.getGroups().add( userGroup2 );
        userD.getGroups().add( userGroup2 );

        userGroup1.setManagedGroups( newHashSet( userGroup2 ) );
        userGroup2.setManagedByGroups( newHashSet( userGroup1 ) );

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

        addUser( 'A', credentials -> credentials.getUser().setPhoneNumber( "73647271" ) );
        User userB = addUser( 'B', credentials -> credentials.getUser().setPhoneNumber( "23452134" ) );
        addUser( 'C', credentials -> credentials.getUser().setPhoneNumber( "14543232" ) );

        List<User> users = userService.getUsersByPhoneNumber( "23452134" );

        assertEquals( 1, users.size() );
        assertEquals( userB, users.get( 0 ) );
    }

    @Test
    public void testGetByUuid()
    {
        User userA = addUser( 'A' );
        User userB = addUser( 'B' );

        assertEquals( userA, userService.getUserByUuid( userA.getUuid() ) );
        assertEquals( userB, userService.getUserByUuid( userB.getUuid() ) );
    }

    @Test
    public void testGetByIdentifier()
    {
        User userA = addUser( 'A' );
        User userB = addUser( 'B' );
        addUser( 'C' );

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
        User userA = addUser( 'A', credentials -> {
            User user = credentials.getUser();
            user.setSurname( "Yong" );
            user.setFirstName( "Anne" );
            user.setEmail( "lost@space.com" );
            user.getOrganisationUnits().add( unitA );
        } );
        User userB = addUser( 'B', credentials -> {
            User user = credentials.getUser();
            user.setSurname( "Arden" );
            user.setFirstName( "Jenny" );
            user.setEmail( "Inside@other.com" );
            user.getOrganisationUnits().add( unitA );
        } );
        User userC = addUser( 'C', credentials -> {
            User user = credentials.getUser();
            user.setSurname( "Smith" );
            user.setFirstName( "Igor" );
            user.setEmail( "home@other.com" );
            user.getOrganisationUnits().add( unitA );
        } );

        UserQueryParams params = getDefaultParams().addOrganisationUnit( unitA );

        assertEquals(
            userService.getUsers( params, singletonList( "email:idesc" ) ),
            asList( userA, userB, userC ) );

        assertEquals(
            userService.getUsers( params, null ),
            asList( userB, userC, userA ) );

        assertEquals(
            userService.getUsers( params, singletonList( "firstName:asc" ) ),
            asList( userA, userC, userB ) );
    }

    @Test
    public void testGetManagedGroupsLessAuthoritiesDisjointRoles()
    {
        User userA = addUser( 'A', roleA );
        User userB = addUser( 'B', roleB, roleC );
        User userC = addUser( 'C', roleA, roleC );
        User userD = addUser( 'D', roleC );
        User userE = addUser( 'E', roleA );
        User userF = addUser( 'F', roleC );

        UserGroup userGroup1 = createUserGroup( 'A', newHashSet( userA, userB ) );
        UserGroup userGroup2 = createUserGroup( 'B', newHashSet( userC, userD, userE, userF ) );
        userA.getGroups().add( userGroup1 );
        userB.getGroups().add( userGroup1 );
        userC.getGroups().add( userGroup2 );
        userD.getGroups().add( userGroup2 );
        userE.getGroups().add( userGroup2 );
        userF.getGroups().add( userGroup2 );

        userGroup1.setManagedGroups( newHashSet( userGroup2 ) );
        userGroup2.setManagedByGroups( newHashSet( userGroup1 ) );

        userGroupService.addUserGroup( userGroup1 );
        userGroupService.addUserGroup( userGroup2 );

        UserQueryParams params = new UserQueryParams()
            .setCanManage( true )
            .setAuthSubset( true )
            .setUser( userA );
        assertContainsOnly( userService.getUsers( params ), userD, userF );
        assertEquals( 2, userService.getUserCount( params ) );

        params.setUser( userB );
        assertContainsOnly( userService.getUsers( params ) );
        assertEquals( 0, userService.getUserCount( params ) );

        params.setUser( userC );
        assertContainsOnly( userService.getUsers( params ) );
        assertEquals( 0, userService.getUserCount( params ) );
    }

    @Test
    public void testGetManagedGroupsSearch()
    {
        User userA = addUser( 'A' );
        addUser( 'B' );
        addUser( 'C' );
        addUser( 'D' );
        addUser( 'E' );
        addUser( 'F' );

        UserQueryParams params = getDefaultParams()
            .setQuery( "rstnameA" );

        assertContainsOnly( userService.getUsers( params ), userA );
        assertEquals( 1, userService.getUserCount( params ) );
    }

    @Test
    public void testGetManagedGroupsSelfRegistered()
    {
        User userA = addUser( 'A', UserCredentials::setSelfRegistered, true );
        addUser( 'B' );
        User userC = addUser( 'C', UserCredentials::setSelfRegistered, true );
        addUser( 'D' );

        UserQueryParams params = getDefaultParams()
            .setSelfRegistered( true );

        assertContainsOnly( userService.getUsers( params ), userA, userC );
        assertEquals( 2, userService.getUserCount( params ) );
    }

    @Test
    public void testGetManagedGroupsOrganisationUnit()
    {
        User userA = addUser( 'A', unitA, unitB );
        addUser( 'B', unitB );
        User userC = addUser( 'C', unitA );
        addUser( 'D', unitB );

        UserQueryParams params = getDefaultParams()
            .addOrganisationUnit( unitA );

        assertContainsOnly( userService.getUsers( params ), userA, userC );
        assertEquals( 2, userService.getUserCount( params ) );
    }

    @Test
    public void testGetInvitations()
    {
        addUser( 'A' );
        User userB = addUser( 'B', UserCredentials::setInvitation, true );
        addUser( 'C' );
        User userD = addUser( 'D', UserCredentials::setInvitation, true );

        UserQueryParams params = getDefaultParams()
            .setInvitationStatus( UserInvitationStatus.ALL );

        assertContainsOnly( userService.getUsers( params ), userB, userD );
        assertEquals( 2, userService.getUserCount( params ) );

        params.setInvitationStatus( UserInvitationStatus.EXPIRED );

        assertContainsOnly( userService.getUsers( params ) );
        assertEquals( 0, userService.getUserCount( params ) );
    }

    @Test
    public void testGetExpiringUser()
    {
        User userA = addUser( 'A' );
        addUser( 'B', UserCredentials::setDisabled, true );
        User userC = addUser( 'C' );
        addUser( 'D', UserCredentials::setDisabled, true );

        assertContainsOnly( userService.getExpiringUsers(), userA, userC );
    }

    @Test
    public void testDisableUsersInactiveSince()
    {
        ZonedDateTime now = ZonedDateTime.now();
        Date twoMonthsAgo = Date.from( now.minusMonths( 2 ).toInstant() );
        Date threeMonthAgo = Date.from( now.minusMonths( 3 ).toInstant() );
        Date fourMonthAgo = Date.from( now.minusMonths( 4 ).toInstant() );
        Date twentyTwoDaysAgo = Date.from( now.minusDays( 22 ).toInstant() );

        User userA = addUser( 'A', UserCredentials::setLastLogin, threeMonthAgo );
        User userB = addUser( 'B', credentials -> {
            credentials.setDisabled( true );
            credentials.setLastLogin( fourMonthAgo );
        } );

        addUser( 'C', UserCredentials::setLastLogin, twentyTwoDaysAgo );
        addUser( 'D' );

        // User A gets disabled, B would but already was, C is active, D last
        // login is still null
        assertEquals( 1, userService.disableUsersInactiveSince( twoMonthsAgo ) );

        // being a super-user is the simplest way to filter purely on the set
        // parameters
        createAndInjectAdminUser();

        UserQueryParams params = getDefaultParams()
            .setDisabled( true );
        List<User> users = userService.getUsers( params );
        assertEquals( new HashSet<>( asList( userA.getUid(), userB.getUid() ) ),
            users.stream().map( User::getUid ).collect( toSet() ) );
    }

    @Test
    public void testGetDisplayNameNull()
    {
        assertNull( userService.getDisplayName( "notExist" ) );
    }
}
