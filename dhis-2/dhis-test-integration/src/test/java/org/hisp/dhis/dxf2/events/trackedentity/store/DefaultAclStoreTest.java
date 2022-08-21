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
package org.hisp.dhis.dxf2.events.trackedentity.store;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DefaultAclStoreTest extends NonTransactionalIntegrationTest
{

    @Autowired
    private UserService _userService;

    @Autowired
    private DefaultAclStore aclStore;

    @Autowired
    IdentifiableObjectManager manager;

    private User owner;

    private User user;

    @BeforeEach
    void setUp()
    {
        // DhisConvenienceTests needs it to be injected/set for createUser
        userService = _userService;
        user = createUserWithAuth( "userWithoutUserGroup" );
        owner = createUserWithAuth( "owner" );
    }

    @Test
    void getAccessibleProgramsReturnsNoneIfNoneIsPublicAndUserHasNoAccess()
    {
        // a private program
        Program programA = createProgram( 'A' );
        programA.setPublicAccess( "--------" );
        programA.getSharing().setOwner( owner );
        manager.save( programA, false );
        // a private program readable by a user group of which the user is NOT
        // part of
        Program programB = createProgram( 'B' );
        programB.setPublicAccess( "--------" );
        programB.getSharing().setOwner( owner );
        UserGroup g = createUserGroup( 'B', Set.of( owner ) );
        UserGroupAccess a = new UserGroupAccess();
        a.setUserGroup( g );
        a.setAccess( "--r-----" );
        programB.getSharing().addUserGroupAccess( a );
        manager.save( programB, false );
        List<Long> programIds = aclStore.getAccessiblePrograms( user.getUid(), Collections.emptyList() );
        assertThat( programIds, hasSize( 0 ) );
    }

    @Test
    void getAccessibleProgramsReturnsPublicOnes()
    {
        // a publicly readable program
        Program programA = createProgram( 'A' );
        programA.getSharing().setOwner( owner );
        programA.setPublicAccess( "--r-----" );
        manager.save( programA, false );
        // a private program
        Program programB = createProgram( 'B' );
        programB.getSharing().setOwner( owner );
        programB.setPublicAccess( "--------" );
        manager.save( programB, false );
        List<Long> programIds = aclStore.getAccessiblePrograms( user.getUid(), Collections.emptyList() );
        assertContainsOnly( programIds, programA.getId() );
    }

    @Test
    void getAccessibleProgramsReturnsUserAccessibleOnes()
    {
        // a private program
        Program programA = createProgram( 'A' );
        programA.setPublicAccess( "--------" );
        programA.getSharing().setOwner( owner );
        manager.save( programA, false );
        // a private program readable by the user
        Program programB = createProgram( 'B' );
        programB.setPublicAccess( "--------" );
        programB.getSharing().setOwner( owner );
        UserAccess a = new UserAccess();
        a.setUser( user );
        a.setAccess( "--r-----" );
        programB.getSharing().addUserAccess( a );
        manager.save( programB, false );
        List<Long> programIds = aclStore.getAccessiblePrograms( user.getUid(), Collections.emptyList() );
        assertContainsOnly( programIds, programB.getId() );
    }

    @Test
    void getAccessibleProgramsReturnsUserGroupOnes()
    {
        // a private program
        Program programA = createProgram( 'A' );
        programA.setPublicAccess( "--------" );
        programA.getSharing().setOwner( owner );
        manager.save( programA, false );
        // a private program readable by a user group of which the user IS part
        // of
        Program programB = createProgram( 'B' );
        programB.setPublicAccess( "--------" );
        programB.getSharing().setOwner( owner );
        UserGroup g = createUserGroup( 'B', Set.of( owner, user ) );
        UserGroupAccess a = new UserGroupAccess();
        a.setUserGroup( g );
        a.setAccess( "--r-----" );
        programB.getSharing().addUserGroupAccess( a );
        manager.save( programB, false );
        List<Long> programIds = aclStore.getAccessiblePrograms( user.getUid(),
            Collections.singletonList( g.getUid() ) );
        assertContainsOnly( programIds, programB.getId() );
    }
}
