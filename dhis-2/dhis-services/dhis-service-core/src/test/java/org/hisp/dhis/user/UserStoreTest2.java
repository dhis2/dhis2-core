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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class UserStoreTest2
    extends DhisSpringTest
{

    public static final String AUTH_A = "AuthA";

    public static final String AUTH_B = "AuthB";

    public static final String AUTH_C = "AuthC";

    public static final String AUTH_D = "AuthD";

    @Autowired
    private UserStore userStore;

    @Autowired
    private UserService userService;

    private UserAuthorityGroup roleA;

    private UserAuthorityGroup roleB;

    private UserAuthorityGroup roleC;

    @Override
    public void setUpTest()
        throws Exception
    {
        super.userService = userService;

        roleA = createUserAuthorityGroup( 'A' );
        roleB = createUserAuthorityGroup( 'B' );
        roleC = createUserAuthorityGroup( 'C' );

        roleA.getAuthorities().add( AUTH_A );
        roleA.getAuthorities().add( AUTH_B );
        roleA.getAuthorities().add( AUTH_C );
        roleA.getAuthorities().add( AUTH_D );

        roleB.getAuthorities().add( AUTH_A );
        roleB.getAuthorities().add( AUTH_B );

        roleC.getAuthorities().add( AUTH_C );

        userService.addUserAuthorityGroup( roleA );
        userService.addUserAuthorityGroup( roleB );
        userService.addUserAuthorityGroup( roleC );
    }

    @Test
    public void testAddGetUser()
    {
        User userA = createUser( 'A' );
        User userB = createUser( 'B' );

        userStore.save( userA );
        long idA = userA.getId();
        userStore.save( userB );
        long idB = userB.getId();

        assertEquals( userA, userStore.get( idA ) );
        assertEquals( userB, userStore.get( idB ) );
    }

    @Test
    public void testGetUserByUuid()
    {
        User userA = createUser( 'A' );
        User userB = createUser( 'B' );

        userStore.save( userA );
        userStore.save( userB );

        UUID uuidA = userA.getUuid();
        UUID uuidB = userB.getUuid();

        User ucA = userStore.getUserByUuid( uuidA );
        User ucB = userStore.getUserByUuid( uuidB );

        assertNotNull( ucA );
        assertNotNull( ucB );

        assertEquals( uuidA, ucA.getUuid() );
        assertEquals( uuidB, ucB.getUuid() );
    }

    @Test
    public void testGetUserWithAuthority()
    {
        User userA = addUser( 'A', roleA );
        User userB = addUser( 'B', roleB, roleC );

        List<User> usersWithAuthorityA = userService.getUsersWithAuthority( AUTH_D );
        assertTrue( usersWithAuthorityA.contains( userA ) );

        List<User> usersWithAuthorityB = userService.getUsersWithAuthority( AUTH_D );
        assertFalse( usersWithAuthorityB.contains( userB ) );
    }
}
