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
import static org.hisp.dhis.tracker.export.trackedentity.aggregates.ThreadPoolManager.getPool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Component( "org.hisp.dhis.tracker.trackedentity.aggregates.EnrollmentAggregate" )
@RequiredArgsConstructor
public class EnrollmentAggregate
    implements
    Aggregate
{
    @Qualifier( "org.hisp.dhis.tracker.trackedentity.aggregates.EnrollmentStore" )
    @Nonnull
    private final EnrollmentStore enrollmentStore;

    @Qualifier( "org.hisp.dhis.tracker.trackedentity.aggregates.EventAggregate" )
    @Nonnull
    private final EventAggregate eventAggregate;

    /**
     * Key: tei uid , value Enrollment
     *
     * @param ids a List of {@see TrackedEntity} Primary Keys
     *
     * @return a MultiMap where key is a {@see TrackedEntity} uid and the key a
     *         List of {@see Enrollment} objects
     */
    Multimap<String, Enrollment> findByTrackedEntityInstanceIds( List<Long> ids, Context ctx )
    {
        Multimap<String, Enrollment> enrollments = enrollmentStore.getEnrollmentsByTrackedEntityInstanceIds( ids,
            ctx );

        if ( enrollments.isEmpty() )
        {
            return enrollments;
        }

        List<Long> enrollmentIds = enrollments.values().stream().map( Enrollment::getId )
            .collect( Collectors.toList() );

        final CompletableFuture<Multimap<String, Event>> eventAsync = conditionalAsyncFetch(
            ctx.getParams().getEnrollmentParams().isIncludeEvents(),
            () -> eventAggregate.findByEnrollmentIds( enrollmentIds, ctx ), getPool() );

        final CompletableFuture<Multimap<String, RelationshipItem>> relationshipAsync = conditionalAsyncFetch(
            ctx.getParams().getEnrollmentParams().isIncludeRelationships(),
            () -> enrollmentStore.getRelationships( enrollmentIds, ctx ), getPool() );

        final CompletableFuture<Multimap<String, TrackedEntityComment>> notesAsync = asyncFetch(
            () -> enrollmentStore.getNotes( enrollmentIds ), getPool() );

        final CompletableFuture<Multimap<String, TrackedEntityAttributeValue>> attributesAsync = conditionalAsyncFetch(
            ctx.getParams().getTeiEnrollmentParams().isIncludeAttributes(),
            () -> enrollmentStore.getAttributes( enrollmentIds, ctx ), getPool() );

        return allOf( eventAsync, notesAsync, relationshipAsync, attributesAsync ).thenApplyAsync( fn -> {

            Multimap<String, Event> events = eventAsync.join();
            Multimap<String, TrackedEntityComment> notes = notesAsync.join();
            Multimap<String, RelationshipItem> relationships = relationshipAsync.join();
            Multimap<String, TrackedEntityAttributeValue> attributes = attributesAsync.join();

            for ( Enrollment enrollment : enrollments.values() )
            {
                if ( ctx.getParams().getTeiEnrollmentParams().isIncludeEvents() )
                {
                    enrollment.setEvents( new HashSet<>( events.get( enrollment.getUid() ) ) );
                }
                if ( ctx.getParams().getTeiEnrollmentParams().isIncludeRelationships() )
                {
                    enrollment.setRelationshipItems( new HashSet<>( relationships.get( enrollment.getUid() ) ) );
                }
                if ( ctx.getParams().getTeiEnrollmentParams().isIncludeAttributes() )
                {
                    enrollment.getTrackedEntity()
                        .setTrackedEntityAttributeValues(
                            new LinkedHashSet<>( attributes.get( enrollment.getUid() ) ) );
                }

                enrollment.setComments( new ArrayList<>( notes.get( enrollment.getUid() ) ) );
            }

            return enrollments;

        }, getPool() ).join();
    }
}
