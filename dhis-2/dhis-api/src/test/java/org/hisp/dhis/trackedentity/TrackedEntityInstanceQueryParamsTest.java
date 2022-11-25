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
package org.hisp.dhis.trackedentity;

import static org.hisp.dhis.common.AssignedUserSelectionMode.CURRENT;
import static org.hisp.dhis.common.AssignedUserSelectionMode.PROVIDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Set;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class TrackedEntityInstanceQueryParamsTest
{

    public static final String CURRENT_USER_UID = "Kj6vYde4LHh";

    public static final String NON_CURRENT_USER_UID = "f1AyMswryyX";

    public static final Set<String> NON_CURRENT_USER_UIDS = Set.of( NON_CURRENT_USER_UID );

    private TrackedEntityInstanceQueryParams params;

    private User current;

    @BeforeEach
    void setUp()
    {
        current = new User();
        current.setUid( CURRENT_USER_UID );

        params = new TrackedEntityInstanceQueryParams();
    }

    @Test
    void testUserWithAssignedUsersGivenUsersAndModeProvided()
    {

        params.setUserWithAssignedUsers( null, PROVIDED, NON_CURRENT_USER_UIDS );

        assertNull( params.getUser() );
        assertEquals( PROVIDED, params.getAssignedUserSelectionMode() );
        assertEquals( NON_CURRENT_USER_UIDS, params.getAssignedUsers() );
    }

    @Test
    void testUserWithAssignedUsersGivenUsersAndNoMode()
    {

        params.setUserWithAssignedUsers( current, null, NON_CURRENT_USER_UIDS );

        assertEquals( current, params.getUser() );
        assertEquals( PROVIDED, params.getAssignedUserSelectionMode() );
        assertEquals( NON_CURRENT_USER_UIDS, params.getAssignedUsers() );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testUserWithAssignedUsersGivenNoModeAndNoUsers( Set<String> users )
    {

        params.setUserWithAssignedUsers( current, null, users );

        assertEquals( current, params.getUser() );
        assertNull( params.getAssignedUserSelectionMode() );
        assertIsEmpty( params.getAssignedUsers() );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testUserWithAssignedUsersFailsGivenNoUsersAndProvided( Set<String> users )
    {

        assertThrows( IllegalQueryException.class,
            () -> params.setUserWithAssignedUsers( current, PROVIDED, users ) );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testUserWithAssignedUsersGivenCurrentUserAndModeCurrentAndUsersNull( Set<String> users )
    {

        params.setUserWithAssignedUsers( current, CURRENT, users );

        assertEquals( current, params.getUser() );
        assertEquals( PROVIDED, params.getAssignedUserSelectionMode() );
        assertEquals( Set.of( CURRENT_USER_UID ), params.getAssignedUsers() );
    }

    @Test
    void testUserWithAssignedUsersFailsGivenNoCurrentUserAndModeCurrent()
    {

        assertThrows( IllegalQueryException.class, () -> params.setUserWithAssignedUsers( null, CURRENT, null ) );
    }

    @Test
    void testUserWithAssignedUsersFailsGivenCurrentUserAndModeCurrentAndNonEmptyUsers()
    {

        assertThrows( IllegalQueryException.class,
            () -> params.setUserWithAssignedUsers( current, CURRENT, NON_CURRENT_USER_UIDS ) );
    }

    @ParameterizedTest
    @EnumSource( value = AssignedUserSelectionMode.class, mode = EnumSource.Mode.EXCLUDE, names = "PROVIDED" )
    void testUserWithAssignedUsersFailsGivenUsersAndModeOtherThanProvided( AssignedUserSelectionMode mode )
    {

        assertThrows( IllegalQueryException.class,
            () -> params.setUserWithAssignedUsers( null, mode, NON_CURRENT_USER_UIDS ) );
    }

    @ParameterizedTest
    @EnumSource( value = AssignedUserSelectionMode.class, mode = EnumSource.Mode.EXCLUDE, names = { "PROVIDED",
        "CURRENT" } )
    void testUserWithAssignedUsersGivenNullUsersAndModeOtherThanProvided( AssignedUserSelectionMode mode )
    {
        params.setUserWithAssignedUsers( current, mode, null );

        assertEquals( current, params.getUser() );
        assertEquals( mode, params.getAssignedUserSelectionMode() );
        assertIsEmpty( params.getAssignedUsers() );
    }

    private static void assertIsEmpty( Collection<?> actual )
    {
        assertNotNull( actual );
        assertTrue( actual.isEmpty(), actual.toString() );
    }

}