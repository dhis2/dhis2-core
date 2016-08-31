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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.annotation.Resource;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.junit.Test;

public class UserAuthorityGroupTest
    extends DhisSpringTest
{
    @Resource(name="org.hisp.dhis.user.UserAuthorityGroupStore")
    private GenericIdentifiableObjectStore<UserAuthorityGroup> userAuthorityGroupStore;

    @Test
    public void testAddGetUserAuthorityGroup()
    {
        UserAuthorityGroup roleA = createUserAuthorityGroup( 'A' );
        UserAuthorityGroup roleB = createUserAuthorityGroup( 'B' );
        UserAuthorityGroup roleC = createUserAuthorityGroup( 'C' );
        
        int idA = userAuthorityGroupStore.save( roleA );
        int idB = userAuthorityGroupStore.save( roleB );
        int idC = userAuthorityGroupStore.save( roleC );
        
        assertEquals( roleA, userAuthorityGroupStore.get( idA ) );
        assertEquals( roleB, userAuthorityGroupStore.get( idB ) );
        assertEquals( roleC, userAuthorityGroupStore.get( idC ) );
    }

    @Test
    public void testDeleteUserAuthorityGroup()
    {
        UserAuthorityGroup roleA = createUserAuthorityGroup( 'A' );
        UserAuthorityGroup roleB = createUserAuthorityGroup( 'B' );
        UserAuthorityGroup roleC = createUserAuthorityGroup( 'C' );
        
        int idA = userAuthorityGroupStore.save( roleA );
        int idB = userAuthorityGroupStore.save( roleB );
        int idC = userAuthorityGroupStore.save( roleC );
        
        assertEquals( roleA, userAuthorityGroupStore.get( idA ) );
        assertEquals( roleB, userAuthorityGroupStore.get( idB ) );
        assertEquals( roleC, userAuthorityGroupStore.get( idC ) );
        
        userAuthorityGroupStore.delete( roleB );
        
        assertNotNull( userAuthorityGroupStore.get( idA ) );
        assertNull( userAuthorityGroupStore.get( idB ) );
        assertNotNull( userAuthorityGroupStore.get( idA ) );
    }
}
