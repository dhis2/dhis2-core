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
package org.hisp.dhis.dxf2.events.aggregates;

import static java.util.concurrent.CompletableFuture.allOf;
import static org.hisp.dhis.dxf2.events.aggregates.ThreadPoolManager.getPool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.store.EnrollmentStore;
import org.springframework.stereotype.Component;

import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Component
@RequiredArgsConstructor
public class EnrollmentAggregate
    extends
    AbstractAggregate
{
    @NonNull
    private final EnrollmentStore enrollmentStore;

    @NonNull
    private final EventAggregate eventAggregate;

    /**
     * Key: tei uid , value Enrollment
     *
     * @param ids a List of {@see TrackedEntityInstance} Primary Keys
     *
     * @return a MultiMap where key is a {@see TrackedEntityInstance} uid and
     *         the key a List of {@see Enrollment} objects
     */
    Multimap<String, Enrollment> findByTrackedEntityInstanceIds( List<Long> ids, AggregateContext ctx )
    {
        Multimap<String, Enrollment> enrollments = enrollmentStore.getEnrollmentsByTrackedEntityInstanceIds( ids, ctx );

        if ( enrollments.isEmpty() )
        {
            return enrollments;
        }

        List<Long> enrollmentIds = enrollments.values().stream().map( Enrollment::getId )
            .collect( Collectors.toList() );

        final CompletableFuture<Multimap<String, Event>> eventAsync = conditionalAsyncFetch(
            ctx.getParams().getTeiEnrollmentParams().isIncludeEvents(),
            () -> eventAggregate.findByEnrollmentIds( enrollmentIds, ctx ), getPool() );

        final CompletableFuture<Multimap<String, Relationship>> relationshipAsync = conditionalAsyncFetch(
            ctx.getParams().getTeiEnrollmentParams().isIncludeRelationships(),
            () -> enrollmentStore.getRelationships( enrollmentIds, ctx ), getPool() );

        final CompletableFuture<Multimap<String, Note>> notesAsync = asyncFetch(
            () -> enrollmentStore.getNotes( enrollmentIds ), getPool() );

        final CompletableFuture<Multimap<String, Attribute>> attributesAsync = conditionalAsyncFetch(
            ctx.getParams().getTeiEnrollmentParams().isIncludeAttributes(),
            () -> enrollmentStore.getAttributes( enrollmentIds, ctx ), getPool() );

        return allOf( eventAsync, notesAsync, relationshipAsync, attributesAsync ).thenApplyAsync( fn -> {

            Multimap<String, Event> events = eventAsync.join();
            Multimap<String, Note> notes = notesAsync.join();
            Multimap<String, Relationship> relationships = relationshipAsync.join();
            Multimap<String, Attribute> attributes = attributesAsync.join();

            for ( Enrollment enrollment : enrollments.values() )
            {
                if ( ctx.getParams().getTeiEnrollmentParams().isIncludeEvents() )
                {
                    enrollment.setEvents( new ArrayList<>( events.get( enrollment.getEnrollment() ) ) );
                }
                if ( ctx.getParams().getTeiEnrollmentParams().isIncludeRelationships() )
                {
                    enrollment.setRelationships( new HashSet<>( relationships.get( enrollment.getEnrollment() ) ) );
                }
                if ( ctx.getParams().getTeiEnrollmentParams().isIncludeAttributes() )
                {
                    enrollment.setAttributes( new ArrayList<>( attributes.get( enrollment.getEnrollment() ) ) );
                }

                enrollment.setNotes( new ArrayList<>( notes.get( enrollment.getEnrollment() ) ) );
            }

            return enrollments;

        }, getPool() ).join();
    }
}
