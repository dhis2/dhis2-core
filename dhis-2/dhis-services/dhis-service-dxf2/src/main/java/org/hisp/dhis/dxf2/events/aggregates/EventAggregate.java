package org.hisp.dhis.dxf2.events.aggregates;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.store.EventStore;
import org.springframework.stereotype.Component;

import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Component
public class EventAggregate
    extends
    AbstractAggregate
{
    private final EventStore eventStore;

    public EventAggregate( EventStore eventStore )
    {
        this.eventStore = eventStore;
    }

    /**
     * Key: enrollment uid -> Value: Event
     *
     * @param ids a List of {@see Enrollment} Primary Keys
     * @return
     */
    public Multimap<String, Event> findByEnrollmentIds( List<Long> ids, boolean includeRelationships )
    {
        Multimap<String, Event> events = this.eventStore.getEventsByEnrollmentIds( ids );

        if ( events.isEmpty() )
        {
            return events;
        }

        List<Long> eventIds = events.values().stream().map( Event::getId ).collect( Collectors.toList() );

        final CompletableFuture<Multimap<String, Relationship>> relationshipAsync = conditionalAsyncFetch(
            includeRelationships, () -> eventStore.getRelationships( eventIds ) );

        final CompletableFuture<Multimap<String, Note>> notesAsync = asyncFetch(
            () -> eventStore.getNotes( eventIds ) );

        final CompletableFuture<Map<String, List<DataValue>>> dataValuesAsync = supplyAsync(
            () -> eventStore.getDataValues( eventIds ) );

        return allOf( dataValuesAsync, notesAsync, relationshipAsync ).thenApplyAsync( dummy -> {

            Map<String, List<DataValue>> dataValues = dataValuesAsync.join();
            Multimap<String, Note> notes = notesAsync.join();
            Multimap<String, Relationship> relationships = relationshipAsync.join();

            for ( Event event : events.values() )
            {
                if ( includeRelationships )
                {
                    event.setRelationships( new HashSet<>( relationships.get( event.getEvent() ) ) );
                }
                if ( !dataValues.isEmpty() )
                {
                    event.setDataValues( new HashSet<>( dataValues.get( event.getEvent() ) ) );
                }
                event.setNotes( new ArrayList<>( notes.get( event.getEvent() ) ) );
            }

            return events;

        } ).join();
    }
}
