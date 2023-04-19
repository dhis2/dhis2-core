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
package org.hisp.dhis.tracker.export.trackedentity.aggregates;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.hisp.dhis.tracker.export.trackedentity.aggregates.ThreadPoolManager.getPool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Component( "org.hisp.dhis.tracker.trackedentity.aggregates.EventAggregate" )
@RequiredArgsConstructor
public class EventAggregate
    implements
    Aggregate
{
    @Qualifier( "org.hisp.dhis.tracker.trackedentity.aggregates.EventStore" )
    @Nonnull
    private final EventStore eventStore;

    /**
     * Key: enrollment uid -> Value: Event
     *
     * @param ids a List of {@see Enrollment} Primary Keys
     * @param ctx the {@see Context}
     * @return a Map where the key is a Program Instance Primary Key, and the
     *         value is a List of {@see Event}
     */
    Multimap<String, ProgramStageInstance> findByEnrollmentIds( List<Long> ids, Context ctx )
    {
        // Fetch all the Events that are linked to the given Enrollment IDs

        Multimap<String, ProgramStageInstance> events = this.eventStore.getEventsByEnrollmentIds( ids, ctx );

        if ( events.isEmpty() )
        {
            return events;
        }

        List<Long> eventIds = events.values().stream().map( ProgramStageInstance::getId )
            .collect( Collectors.toList() );

        /*
         * Async fetch Relationships for the given Event ids (only if
         * isIncludeRelationships = true)
         */
        final CompletableFuture<Multimap<String, RelationshipItem>> relationshipAsync = conditionalAsyncFetch(
            ctx.getParams().getEventParams().isIncludeRelationships(),
            () -> eventStore.getRelationships( eventIds, ctx ), getPool() );

        /*
         * Async fetch Notes for the given Event ids
         */
        final CompletableFuture<Multimap<String, TrackedEntityComment>> notesAsync = asyncFetch(
            () -> eventStore.getNotes( eventIds ), getPool() );

        /*
         * Async fetch DataValues for the given Event ids
         */
        final CompletableFuture<Map<String, List<EventDataValue>>> dataValuesAsync = supplyAsync(
            () -> eventStore.getDataValues( eventIds ), getPool() );

        return allOf( dataValuesAsync, notesAsync, relationshipAsync ).thenApplyAsync( fn -> {

            Map<String, List<EventDataValue>> dataValues = dataValuesAsync.join();
            Multimap<String, TrackedEntityComment> notes = notesAsync.join();
            Multimap<String, RelationshipItem> relationships = relationshipAsync.join();

            for ( ProgramStageInstance event : events.values() )
            {
                if ( ctx.getParams().isIncludeRelationships() )
                {
                    event.setRelationshipItems( new HashSet<>( relationships.get( event.getUid() ) ) );
                }

                List<EventDataValue> dataValuesForEvent = dataValues.get( event.getUid() );
                if ( dataValuesForEvent != null && !dataValuesForEvent.isEmpty() )
                {
                    event.setEventDataValues( new HashSet<>( dataValues.get( event.getUid() ) ) );
                }
                event.setComments( new ArrayList<>( notes.get( event.getUid() ) ) );
            }

            return events;

        }, getPool() ).join();
    }
}
