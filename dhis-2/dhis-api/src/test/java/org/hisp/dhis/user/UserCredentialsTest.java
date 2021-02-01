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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link UserCredentials}.
 *
 * @author volsch
 */
public class UserCredentialsTest
{
    private UserAuthorityGroup userAuthorityGroup1;

    private UserAuthorityGroup userAuthorityGroup2;

    private UserAuthorityGroup userAuthorityGroup1Super;

    @Before
    public void setUp()
    {
        userAuthorityGroup1 = new UserAuthorityGroup();
        userAuthorityGroup1.setUid( "uid1" );
        userAuthorityGroup1.setAuthorities( new HashSet<>( Arrays.asList( "x1", "x2" ) ) );

        userAuthorityGroup2 = new UserAuthorityGroup();
        userAuthorityGroup2.setUid( "uid2" );
        userAuthorityGroup2.setAuthorities( new HashSet<>( Arrays.asList( "y1", "y2" ) ) );

        userAuthorityGroup1Super = new UserAuthorityGroup();
        userAuthorityGroup1Super.setUid( "uid4" );
        userAuthorityGroup1Super
            .setAuthorities( new HashSet<>( Arrays.asList( "z1", UserAuthorityGroup.AUTHORITY_ALL ) ) );
    }

    @Test
    public void isSuper()
    {
        final UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUserAuthorityGroups(
            new HashSet<>( Arrays.asList( userAuthorityGroup1, userAuthorityGroup1Super, userAuthorityGroup2 ) ) );

        assertTrue( userCredentials.isSuper() );
        assertTrue( userCredentials.isSuper() );
    }

    @Test
    public void isNotSuper()
    {
        final UserCredentials userCredentials = new UserCredentials();
        userCredentials
            .setUserAuthorityGroups( new HashSet<>( Arrays.asList( userAuthorityGroup1, userAuthorityGroup2 ) ) );

        assertFalse( userCredentials.isSuper() );
        assertFalse( userCredentials.isSuper() );
    }

    @Test
    public void isSuperChanged()
    {
        final UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUserAuthorityGroups(
            new HashSet<>( Arrays.asList( userAuthorityGroup1, userAuthorityGroup1Super, userAuthorityGroup2 ) ) );

        assertTrue( userCredentials.isSuper() );

        userCredentials
            .setUserAuthorityGroups( new HashSet<>( Arrays.asList( userAuthorityGroup1, userAuthorityGroup2 ) ) );
        assertFalse( userCredentials.isSuper() );
    }

    @Test
    public void getAllAuthorities()
    {
        final UserCredentials userCredentials = new UserCredentials();
        userCredentials
            .setUserAuthorityGroups( new HashSet<>( Arrays.asList( userAuthorityGroup1, userAuthorityGroup1Super ) ) );

        Set<String> authorities1 = userCredentials.getAllAuthorities();
        assertThat( authorities1, Matchers.containsInAnyOrder( "x1", "x2", "z1", UserAuthorityGroup.AUTHORITY_ALL ) );

        Set<String> authorities2 = userCredentials.getAllAuthorities();
        assertSame( authorities1, authorities2 );
    }

    @Test
    public void getAllAuthoritiesChanged()
    {
        final UserCredentials userCredentials = new UserCredentials();
        userCredentials
            .setUserAuthorityGroups( new HashSet<>( Arrays.asList( userAuthorityGroup1, userAuthorityGroup1Super ) ) );

        Set<String> authorities1 = userCredentials.getAllAuthorities();
        assertThat( authorities1, Matchers.containsInAnyOrder( "x1", "x2", "z1", UserAuthorityGroup.AUTHORITY_ALL ) );

        userCredentials
            .setUserAuthorityGroups( new HashSet<>( Arrays.asList( userAuthorityGroup1, userAuthorityGroup2 ) ) );
        Set<String> authorities2 = userCredentials.getAllAuthorities();
        assertThat( authorities2, Matchers.containsInAnyOrder( "x1", "x2", "y1", "y2" ) );
    }
}
