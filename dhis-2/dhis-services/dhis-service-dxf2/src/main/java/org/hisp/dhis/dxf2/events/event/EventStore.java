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
package org.hisp.dhis.dxf2.events.event;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.dxf2.events.report.EventRow;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.user.User;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface EventStore
{
    /**
     * Inserts a List of {@see Event}. Notes are not stored at this stage.
     *
     * @param events a List of {@see Event}
     *
     * @return a list of saved program stage instances
     */
    List<Event> saveEvents( List<Event> events );

    /**
     * Updates a List of {@see Event}. Notes are not stored at this stage.
     *
     * @param events a List of {@see Event}
     *
     * @return a list of saved program stage instances
     */
    List<Event> updateEvents( List<Event> events );

    List<org.hisp.dhis.dxf2.events.event.Event> getEvents( EventSearchParams params,
        Map<String, Set<String>> psdesWithSkipSyncTrue );

    List<Map<String, String>> getEventsGrid( EventSearchParams params );

    List<EventRow> getEventRows( EventSearchParams params );

    int getEventCount( EventSearchParams params );

    /**
     * Delete list of given events to be removed. This operation also remove
     * comments connected to each Event.
     *
     * @param events List to be removed
     */
    void delete( List<org.hisp.dhis.dxf2.events.event.Event> events );

    /**
     * Updates the "last updated" and "last updated By" of the Tracked Entity
     * Instances matching the provided list of UIDs
     *
     * @param teiUid a List of Tracked Entity Instance uid
     * @param user the User to use for the last update by value. Can be null.
     */
    void updateTrackedEntityInstances( List<String> teiUid, User user );
}
