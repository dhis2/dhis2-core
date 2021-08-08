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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Nguyen Hong Duc
 */
public class UserStoreTest
    extends DhisSpringTest
{
    @Autowired
    private UserStore userStore;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private UserGroupService userGroupService;

    private OrganisationUnit unit1;

    private OrganisationUnit unit2;

    @Override
    public void setUpTest()
        throws Exception
    {
        unit1 = createOrganisationUnit( 'A' );
        unit2 = createOrganisationUnit( 'B' );

        organisationUnitService.addOrganisationUnit( unit1 );
        organisationUnitService.addOrganisationUnit( unit2 );
    }

    @Test
    public void testAddGetUser()
    {
        Set<OrganisationUnit> units = new HashSet<>();

        units.add( unit1 );
        units.add( unit2 );

        User userA = createUser( 'A' );
        User userB = createUser( 'B' );

        userA.setOrganisationUnits( units );
        userB.setOrganisationUnits( units );

        userStore.save( userA );
        long idA = userA.getId();
        userStore.save( userB );
        long idB = userB.getId();

        assertEquals( userA, userStore.get( idA ) );
        assertEquals( userB, userStore.get( idB ) );

        assertEquals( units, userStore.get( idA ).getOrganisationUnits() );
        assertEquals( units, userStore.get( idB ).getOrganisationUnits() );
    }

    @Test
    public void testUpdateUser()
    {
        User userA = createUser( 'A' );
        User userB = createUser( 'B' );

        userStore.save( userA );
        long idA = userA.getId();
        userStore.save( userB );
        long idB = userB.getId();

        assertEquals( userA, userStore.get( idA ) );
        assertEquals( userB, userStore.get( idB ) );

        userA.setSurname( "UpdatedSurnameA" );

        userStore.update( userA );

        assertEquals( userStore.get( idA ).getSurname(), "UpdatedSurnameA" );
    }

    @Test
    public void testDeleteUser()
    {
        User userA = createUser( 'A' );
        User userB = createUser( 'B' );

        userStore.save( userA );
        long idA = userA.getId();
        userStore.save( userB );
        long idB = userB.getId();

        assertEquals( userA, userStore.get( idA ) );
        assertEquals( userB, userStore.get( idB ) );

        userStore.delete( userA );

        assertNull( userStore.get( idA ) );
        assertNotNull( userStore.get( idB ) );
    }

    @Test
    public void testGetCurrentUserGroupInfo()
    {
        User userA = createUser( 'A' );
        userStore.save( userA );
        UserGroup userGroupA = createUserGroup( 'A', Sets.newHashSet( userA ) );
        userGroupService.addUserGroup( userGroupA );
        UserGroup userGroupB = createUserGroup( 'B', Sets.newHashSet( userA ) );
        userGroupService.addUserGroup( userGroupB );
        userA.getGroups().add( userGroupA );
        userA.getGroups().add( userGroupB );

        CurrentUserGroupInfo currentUserGroupInfo = userStore.getCurrentUserGroupInfo( userA.getId() );

        assertNotNull( currentUserGroupInfo );
        assertEquals( 2, currentUserGroupInfo.getUserGroupUIDs().size() );
        assertEquals( userA.getUid(), currentUserGroupInfo.getUserUID() );
    }

    @Test
    public void testGetCurrentUserGroupInfoWithoutGroup()
    {
        User userA = createUser( 'A' );
        userStore.save( userA );
        CurrentUserGroupInfo currentUserGroupInfo = userStore.getCurrentUserGroupInfo( userA.getId() );

        assertNotNull( currentUserGroupInfo );
        assertEquals( 0, currentUserGroupInfo.getUserGroupUIDs().size() );
        assertEquals( userA.getUid(), currentUserGroupInfo.getUserUID() );
    }

    @Test
    public void testGetDisplayName()
    {
        User userA = createUser( 'A' );
        userStore.save( userA );

        assertEquals( "FirstNameA SurnameA", userStore.getDisplayName( userA.getUid() ) );
    }
}
