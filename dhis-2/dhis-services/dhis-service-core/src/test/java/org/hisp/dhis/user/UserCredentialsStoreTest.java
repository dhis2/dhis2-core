/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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

import java.util.UUID;

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

<<<<<<< HEAD
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
/**
 * @author Lars Helge Overland
 */
public class UserCredentialsStoreTest
    extends DhisSpringTest
{
    @Autowired
    private UserCredentialsStore userCredentialsStore;

    @Autowired
    private UserService userService;

    private UserAuthorityGroup roleA;

    private UserAuthorityGroup roleB;

    private UserAuthorityGroup roleC;

    @Override
    public void setUpTest()
        throws Exception
    {
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
    public void testAddGetUserCredentials()
    {
        User userA = createUser( 'A' );
        User userB = createUser( 'B' );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );

        userCredentialsStore.save( credentialsA );
        long idA = credentialsA.getId();
        userCredentialsStore.save( credentialsB );
        long idB = credentialsB.getId();

        assertEquals( credentialsA, userCredentialsStore.get( idA ) );
        assertEquals( credentialsB, userCredentialsStore.get( idB ) );
    }

    @Test
    public void testGetUserCredentialsByUuid()
    {
        User userA = createUser( 'A' );
        User userB = createUser( 'B' );

        UserCredentials credentialsA = createUserCredentials( 'A', userA );
        UserCredentials credentialsB = createUserCredentials( 'B', userB );

        userCredentialsStore.save( credentialsA );
        userCredentialsStore.save( credentialsB );

        UUID uuidA = credentialsA.getUuid();
        UUID uuidB = credentialsB.getUuid();

        UserCredentials ucA = userCredentialsStore.getUserCredentialsByUuid( uuidA );
        UserCredentials ucB = userCredentialsStore.getUserCredentialsByUuid( uuidB );

        assertNotNull( ucA );
        assertNotNull( ucB );

        assertEquals( uuidA, ucA.getUuid() );
        assertEquals( uuidB, ucB.getUuid() );
    }
}
