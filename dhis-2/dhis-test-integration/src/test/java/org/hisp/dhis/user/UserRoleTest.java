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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

class UserRoleTest extends SingleSetupIntegrationTestBase
{

    @Autowired
    @Qualifier( "org.hisp.dhis.user.UserRoleStore" )
    private IdentifiableObjectStore<UserRole> userRoleStore;

    @Test
    void testAddGetUserRole()
    {
        UserRole roleA = createUserRole( 'A' );
        UserRole roleB = createUserRole( 'B' );
        UserRole roleC = createUserRole( 'C' );
        userRoleStore.save( roleA );
        long idA = roleA.getId();
        userRoleStore.save( roleB );
        long idB = roleB.getId();
        userRoleStore.save( roleC );
        long idC = roleC.getId();
        assertEquals( roleA, userRoleStore.get( idA ) );
        assertEquals( roleB, userRoleStore.get( idB ) );
        assertEquals( roleC, userRoleStore.get( idC ) );
    }

    @Test
    void testDeleteUserRole()
    {
        UserRole roleA = createUserRole( 'A' );
        UserRole roleB = createUserRole( 'B' );
        UserRole roleC = createUserRole( 'C' );
        userRoleStore.save( roleA );
        long idA = roleA.getId();
        userRoleStore.save( roleB );
        long idB = roleB.getId();
        userRoleStore.save( roleC );
        long idC = roleC.getId();
        assertEquals( roleA, userRoleStore.get( idA ) );
        assertEquals( roleB, userRoleStore.get( idB ) );
        assertEquals( roleC, userRoleStore.get( idC ) );
        userRoleStore.delete( roleB );
        assertNotNull( userRoleStore.get( idA ) );
        assertNull( userRoleStore.get( idB ) );
        assertNotNull( userRoleStore.get( idA ) );
    }
}
