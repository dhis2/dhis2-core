package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Dang Duy Hieu
 * @version $Id$
 */
public class UserGroupServiceTest
    extends DhisSpringTest
{
    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private  UserService userService;
    
    private User user1;
    private User user2;
    private User user3;

    @Override
    public void setUpTest()
        throws Exception
    {
        user1 = createUser( 'A' );
        user2 = createUser( 'B' );
        user3 = createUser( 'C' );

        userService.addUser( user1 );
        userService.addUser( user2 );
        userService.addUser( user3 );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void assertEq( char uniqueCharacter, UserGroup userGroup )
    {
        assertEquals( "UserGroup" + uniqueCharacter, userGroup.getName() );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddUserGroup()
    {
        Set<User> members = new HashSet<>();

        members.add( user1 );
        members.add( user2 );
        members.add( user3 );

        UserGroup userGroup = createUserGroup( 'A', members );

        userGroupService.addUserGroup( userGroup );

        assertEq( 'A', userGroup );
        assertNotNull( userGroup.getMembers() );
        assertEquals( members, userGroup.getMembers() );
    }

    @Test
    public void testDeleteUserGroup()
    {
        Set<User> members = new HashSet<>();

        members.add( user1 );
        members.add( user2 );

        UserGroup userGroup = createUserGroup( 'A', members );

        userGroupService.addUserGroup( userGroup );

        userGroup = userGroupService.getUserGroupByName( "UserGroupA" ).get( 0 );

        int id = userGroup.getId();

        assertEq( 'A', userGroup );
        assertTrue( members.size() == userGroup.getMembers().size() );

        userGroupService.deleteUserGroup( userGroup );

        assertNull( userGroupService.getUserGroup( id ) );
    }

    @Test
    public void testUpdateUserGroup()
    {
        Set<User> members = new HashSet<>();

        members.add( user1 );
        members.add( user3 );

        UserGroup userGroup = createUserGroup( 'A', members );

        userGroupService.addUserGroup( userGroup );

        userGroup = userGroupService.getUserGroupByName( "UserGroupA" ).get( 0 );

        int id = userGroup.getId();

        assertEq( 'A', userGroup );
        assertEquals( members, userGroup.getMembers() );

        userGroup.setName( "UserGroupB" );
        userGroup.getMembers().add( user2 );

        userGroupService.updateUserGroup( userGroup );

        userGroup = userGroupService.getUserGroup( id );

        assertEq( 'B', userGroup );

        assertEquals( 3, userGroup.getMembers().size() );

        assertTrue( userGroup.getMembers().contains( user1 ) );
        assertTrue( userGroup.getMembers().contains( user2 ) );
        assertTrue( userGroup.getMembers().contains( user3 ) );
    }

    @Test
    public void testGetAllUserGroups()
    {
        List<UserGroup> userGroups = new ArrayList<>();

        Set<User> members = new HashSet<>();

        members.add( user1 );
        members.add( user3 );

        UserGroup userGroupA = createUserGroup( 'A', members );
        userGroups.add( userGroupA );

        userGroupService.addUserGroup( userGroupA );

        members = new HashSet<>();

        members.add( user1 );
        members.add( user2 );

        UserGroup userGroupB = createUserGroup( 'B', members );
        userGroups.add( userGroupB );

        userGroupService.addUserGroup( userGroupB );

        assertEquals( userGroupService.getAllUserGroups(), userGroups );
    }

    @Test
    public void testGetUserGroupById()
    {
        Set<User> members = new HashSet<>();

        members.add( user1 );
        members.add( user2 );
        members.add( user3 );

        UserGroup userGroup = createUserGroup( 'A', members );

        userGroupService.addUserGroup( userGroup );

        int id = userGroupService.getUserGroupByName( "UserGroupA" ).get( 0 ).getId();

        userGroup = userGroupService.getUserGroup( id );

        assertEq( 'A', userGroup );
        assertNotNull( userGroup.getMembers() );
    }

    @Test
    public void testGetUserGroupByName()
    {
        Set<User> members = new HashSet<>();

        members.add( user1 );

        UserGroup userGroup = createUserGroup( 'B', members );

        userGroupService.addUserGroup( userGroup );

        userGroup = userGroupService.getUserGroupByName( "UserGroupB" ).get( 0 );

        assertEq( 'B', userGroup );
        assertNotNull( userGroup.getMembers() );
    }
}
