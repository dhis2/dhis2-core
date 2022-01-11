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
package org.hisp.dhis.userkeyjsonvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Stian Sandvold.
 */
class UserKeyJsonValueStoreTest extends DhisSpringTest
{

    @Autowired
    private UserKeyJsonValueStore userKeyJsonValueStore;

    @Autowired
    private UserService injectUserService;

    private User user;

    @Override
    public void setUpTest()
    {
        this.userService = injectUserService;
        user = createUserAndInjectSecurityContext( true );
    }

    @Test
    void testAddUserKeyJsonValue()
    {
        UserKeyJsonValue userKeyJsonValue = new UserKeyJsonValue();
        userKeyJsonValue.setValue( "{}" );
        userKeyJsonValue.setKey( "test" );
        userKeyJsonValue.setNamespace( "a" );
        userKeyJsonValue.setCreatedBy( user );
        userKeyJsonValueStore.save( userKeyJsonValue );
        long id = userKeyJsonValue.getId();
        assertNotNull( userKeyJsonValue );
        assertEquals( userKeyJsonValue, userKeyJsonValueStore.get( id ) );
    }

    @Test
    void testAddUserKeyJsonValuesAndGetNamespacesByUser()
    {
        UserKeyJsonValue userKeyJsonValueA = new UserKeyJsonValue();
        userKeyJsonValueA.setValue( "{}" );
        userKeyJsonValueA.setNamespace( "test_a" );
        userKeyJsonValueA.setKey( "a" );
        userKeyJsonValueA.setCreatedBy( user );
        userKeyJsonValueStore.save( userKeyJsonValueA );
        UserKeyJsonValue userKeyJsonValueB = new UserKeyJsonValue();
        userKeyJsonValueB.setValue( "{}" );
        userKeyJsonValueB.setNamespace( "test_b" );
        userKeyJsonValueB.setKey( "b" );
        userKeyJsonValueB.setCreatedBy( user );
        userKeyJsonValueStore.save( userKeyJsonValueB );
        List<String> list = userKeyJsonValueStore.getNamespacesByUser( user );
        assertTrue( list.contains( "test_a" ) );
        assertTrue( list.contains( "test_b" ) );
    }

    @Test
    void testAddUserKeyJsonValuesAndGetUserKeyJsonValuesByUser()
    {
        UserKeyJsonValue userKeyJsonValueA = new UserKeyJsonValue();
        userKeyJsonValueA.setValue( "{}" );
        userKeyJsonValueA.setNamespace( "a" );
        userKeyJsonValueA.setKey( "test_a" );
        userKeyJsonValueA.setCreatedBy( user );
        userKeyJsonValueStore.save( userKeyJsonValueA );
        UserKeyJsonValue userKeyJsonValueB = new UserKeyJsonValue();
        userKeyJsonValueB.setValue( "{}" );
        userKeyJsonValueB.setNamespace( "a" );
        userKeyJsonValueB.setKey( "test_b" );
        userKeyJsonValueB.setCreatedBy( user );
        userKeyJsonValueStore.save( userKeyJsonValueB );
        List<UserKeyJsonValue> list = userKeyJsonValueStore.getUserKeyJsonValueByUserAndNamespace( user, "a" );
        assertTrue( list.contains( userKeyJsonValueA ) );
        assertTrue( list.contains( userKeyJsonValueB ) );
    }
}
