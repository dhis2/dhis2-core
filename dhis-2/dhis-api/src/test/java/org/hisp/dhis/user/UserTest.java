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
package org.hisp.dhis.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link User}.
 *
 * @author volsch
 */
class UserTest
{

    private UserRole userRole1;

    private UserRole userRole2;

    private UserRole userRole1Super;

    @BeforeEach
    void setUp()
    {
        userRole1 = new UserRole();
        userRole1.setUid( "uid1" );
        userRole1.setAuthorities( new HashSet<>( Arrays.asList( "x1", "x2" ) ) );
        userRole2 = new UserRole();
        userRole2.setUid( "uid2" );
        userRole2.setAuthorities( new HashSet<>( Arrays.asList( "y1", "y2" ) ) );
        userRole1Super = new UserRole();
        userRole1Super.setUid( "uid4" );
        userRole1Super
            .setAuthorities( new HashSet<>( Arrays.asList( "z1", UserRole.AUTHORITY_ALL ) ) );
    }

    @Test
    void isSuper()
    {
        final User user = new User();
        user.setUserRoles(
            new HashSet<>( Arrays.asList( userRole1, userRole1Super, userRole2 ) ) );
        assertTrue( user.isSuper() );
        assertTrue( user.isSuper() );
    }

    @Test
    void isNotSuper()
    {
        final User user = new User();
        user
            .setUserRoles( new HashSet<>( Arrays.asList( userRole1, userRole2 ) ) );
        assertFalse( user.isSuper() );
        assertFalse( user.isSuper() );
    }

    @Test
    void isSuperChanged()
    {
        final User user = new User();
        user.setUserRoles(
            new HashSet<>( Arrays.asList( userRole1, userRole1Super, userRole2 ) ) );
        assertTrue( user.isSuper() );
        user
            .setUserRoles( new HashSet<>( Arrays.asList( userRole1, userRole2 ) ) );
        assertFalse( user.isSuper() );
    }

    @Test
    void getAllAuthorities()
    {
        final User user = new User();
        user.setUserRoles( new HashSet<>( Arrays.asList( userRole1, userRole1Super ) ) );
        Set<String> authorities1 = user.getAllAuthorities();
        assertThat( authorities1, Matchers.containsInAnyOrder( "x1", "x2", "z1", UserRole.AUTHORITY_ALL ) );
        Set<String> authorities2 = user.getAllAuthorities();
        assertEquals( authorities1, authorities2 );
    }

    @Test
    void getAllAuthoritiesChanged()
    {
        final User user = new User();
        user
            .setUserRoles( new HashSet<>( Arrays.asList( userRole1, userRole1Super ) ) );
        Set<String> authorities1 = user.getAllAuthorities();
        assertThat( authorities1, Matchers.containsInAnyOrder( "x1", "x2", "z1", UserRole.AUTHORITY_ALL ) );
        user
            .setUserRoles( new HashSet<>( Arrays.asList( userRole1, userRole2 ) ) );
        Set<String> authorities2 = user.getAllAuthorities();
        assertThat( authorities2, Matchers.containsInAnyOrder( "x1", "x2", "y1", "y2" ) );
    }

    @Test
    void testGetAllAuthoritiesWhenUserRolesNull()
    {
        final User user = new User();
        user.setUserRoles( null );
        Set<String> authorities = assertDoesNotThrow( user::getAllAuthorities );
        assertTrue( authorities.isEmpty() );
    }

    @Test
    void testGetAllAuthoritiesWhenUserRoleAuthoritiesNull()
    {
        final User user = new User();
        UserRole userRole = new UserRole();
        userRole.setAuthorities( null );
        user.setUserRoles( Set.of( userRole ) );
        Set<String> authorities = assertDoesNotThrow( user::getAllAuthorities );
        assertTrue( authorities.isEmpty() );
    }

    @Test
    void testHasAuthoritiesWhenUserRolesNull()
    {
        final User user = new User();
        user.setUserRoles( null );
        assertFalse( user.hasAuthorities() );
    }

    @Test
    void testHasAuthoritiesWhenHasUserRole()
    {
        final User user = new User();
        user.setUserRoles( new HashSet<>( Arrays.asList( userRole1 ) ) );
        assertTrue( user.hasAuthorities() );
    }

    @Test
    void testGetAllRestrictionsWhenUserRolesNull()
    {
        final User user = new User();
        user.setUserRoles( null );
        Set<String> restrictions = assertDoesNotThrow( user::getAllRestrictions );
        assertTrue( restrictions.isEmpty() );
    }

    @Test
    void testGetAllRestrictionsWhenUserRoleRestrictionsNull()
    {
        final User user = new User();
        UserRole userRole = new UserRole();
        userRole.setRestrictions( null );
        user.setUserRoles( Set.of( userRole ) );
        Set<String> restrictions = assertDoesNotThrow( user::getAllRestrictions );
        assertTrue( restrictions.isEmpty() );
    }

    @Test
    void testGetAllRestrictions()
    {
        final User user = new User();
        UserRole userRole = new UserRole();
        userRole.setRestrictions( Set.of( "R1", "R2" ) );
        user.setUserRoles( Set.of( userRole ) );
        Set<String> restrictions = user.getAllRestrictions();
        assertEquals( 2, restrictions.size() );
    }

    @Test
    void testGetManagedGroups()
    {
        final User user = new User();
        UserGroup group = new UserGroup( "group 1" );
        UserGroup managedGroup1 = new UserGroup( "managed group 1" );
        UserGroup managedGroup2 = new UserGroup( "managed group 2" );
        group.setManagedGroups( Set.of( managedGroup1, managedGroup2 ) );
        user.setGroups( Set.of( group ) );

        Set<UserGroup> groups = user.getManagedGroups();
        assertEquals( 2, groups.size() );
    }

    @Test
    void testGetManagedGroupsWhenGroupsNull()
    {
        final User user = new User();
        user.setGroups( null );

        Set<UserGroup> groups = assertDoesNotThrow( user::getManagedGroups );
        assertTrue( groups.isEmpty() );
    }

    @Test
    void testGetManagedGroupsWhenGroupManagedGroupsNull()
    {
        final User user = new User();
        UserGroup group = new UserGroup( "group 1" );
        group.setManagedGroups( null );
        user.setGroups( Set.of( group ) );

        Set<UserGroup> groups = assertDoesNotThrow( user::getManagedGroups );
        assertTrue( groups.isEmpty() );
    }

    @Test
    void testHasManagedGroups()
    {
        final User user = new User();
        UserGroup group = new UserGroup( "group 1" );
        UserGroup managedGroup1 = new UserGroup( "managed group 1" );
        UserGroup managedGroup2 = new UserGroup( "managed group 2" );
        group.setManagedGroups( Set.of( managedGroup1, managedGroup2 ) );
        user.setGroups( Set.of( group ) );

        assertTrue( user.hasManagedGroups() );
    }
}
