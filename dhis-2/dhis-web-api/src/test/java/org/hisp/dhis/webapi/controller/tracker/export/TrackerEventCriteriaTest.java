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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;

import java.util.Set;

import org.junit.jupiter.api.Test;

class TrackerEventCriteriaTest
{

    @Test
    void getAssignedUsersWhenAssignedUserIsNull()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        assertIsEmpty( eventCriteria.getAssignedUsers() );
    }

    @Test
    void getAssignedUsersWhenAssignedUserIsEmpty()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setAssignedUser( " " );

        assertIsEmpty( eventCriteria.getAssignedUsers() );
    }

    @Test
    void getAssignedUsersFiltersOutInvalidUIDs()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setAssignedUser( "d1dmt9P71Sb;NOT_A_UID;uzkOhvvWn76" );

        assertContainsOnly( Set.of( "d1dmt9P71Sb", "uzkOhvvWn76" ), eventCriteria.getAssignedUsers() );
        // ensure cached set is the same
        assertContainsOnly( Set.of( "d1dmt9P71Sb", "uzkOhvvWn76" ), eventCriteria.getAssignedUsers() );
    }

    @Test
    void getEventsWhenEventIsNull()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();

        assertIsEmpty( eventCriteria.getEvents() );
    }

    @Test
    void getEventsWhenEventIsEmpty()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setEvent( "" );

        assertIsEmpty( eventCriteria.getEvents() );
    }

    @Test
    void getEventsFiltersOutInvalidUIDs()
    {
        TrackerEventCriteria eventCriteria = new TrackerEventCriteria();
        eventCriteria.setEvent( "d1dmt9P71Sb;NOT_A_UID;uzkOhvvWn76" );

        assertContainsOnly( Set.of( "d1dmt9P71Sb", "uzkOhvvWn76" ), eventCriteria.getEvents() );
        // ensure cached set is the same
        assertContainsOnly( Set.of( "d1dmt9P71Sb", "uzkOhvvWn76" ), eventCriteria.getEvents() );
    }
}