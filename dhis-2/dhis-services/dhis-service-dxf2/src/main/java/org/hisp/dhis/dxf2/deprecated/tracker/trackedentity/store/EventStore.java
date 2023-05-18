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
package org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store;

import java.util.List;
import java.util.Map;

import org.hisp.dhis.dxf2.deprecated.tracker.aggregates.AggregateContext;
import org.hisp.dhis.dxf2.deprecated.tracker.event.DataValue;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Note;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Relationship;

import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 *
 * @deprecated this is a class related to "old" (deprecated) tracker which will
 *             be removed with "old" tracker. Make sure to plan migrating to new
 *             tracker.
 */
@Deprecated( since = "2.41" )
public interface EventStore
{
    /**
     * Key: enrollment uid -> Value: Event
     *
     * @param enrollmentsId a List of Enrollment Primary Keys
     * @param ctx the {@see AggregateContext}
     * @return A Map, where the key is a Enrollment Primary Key, and the value
     *         is a List of {@see Event}
     */
    Multimap<String, Event> getEventsByEnrollmentIds( List<Long> enrollmentsId, AggregateContext ctx );

    /**
     *
     * Key: event uid -> Value: List<DataValue>
     *
     * @param programStageInstanceId a List of Program Stage Instance Primary
     *        Keys
     * @return A Map, where the key is a Program Stage Instance Primary Key, and
     *         the value is a List of {@see DataValue}
     */
    Map<String, List<DataValue>> getDataValues( List<Long> programStageInstanceId );

    /**
     * Fetches all the relationships having the Program Stage Instance id
     * specified in the arg as "left" or "right" relationship
     *
     * @param ids a list of {@see Enrollment} Primary Keys
     * @return a MultiMap where key is a {@see Enrollment} uid and the value a
     *         List of {@see Relationship} objects
     */
    Multimap<String, Relationship> getRelationships( List<Long> ids, AggregateContext ctx );

    /**
     * Fetch all the relationships based on provided relationship ids and return
     * the response as Map of entity as key and List of Relationships as value
     *
     * @param ids Relationship ids
     * @return Multimap containing entity uid as key and its associated list of
     *         Relationships as value.
     */
    Multimap<String, Relationship> getRelationshipsByIds( List<Long> ids, EventSearchParams params );

    Multimap<String, Note> getNotes( List<Long> eventIds );
}
