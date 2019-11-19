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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.ProgramOwner;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.store.TrackedEntityInstanceStore;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Component;

import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Component
public class TrackedEntityInstanceAggregate
    extends
    AbstractAggregate
{
    private final TrackedEntityInstanceStore trackedEntityInstanceStore;

    private final EnrollmentAggregate enrollmentAggregate;

    private final CurrentUserService currentUserService;

    public TrackedEntityInstanceAggregate( TrackedEntityInstanceStore trackedEntityInstanceStore,
        EnrollmentAggregate enrollmentAggregate, CurrentUserService currentUserService )
    {
        this.trackedEntityInstanceStore = trackedEntityInstanceStore;
        this.enrollmentAggregate = enrollmentAggregate;
        this.currentUserService = currentUserService;
    }

    /**
     *
     *
     * @param ids a List of {@see TrackedEntityInstance} Primary Keys
     * @param params an instance of {@see TrackedEntityInstanceParams}
     *
     * @return a List of {@see TrackedEntityInstance} objects
     */
    public List<TrackedEntityInstance> find( List<Long> ids, TrackedEntityInstanceParams params )
    {

        AggregateContext aggregateContext = AggregateContext.builder()
            .userId( currentUserService.getCurrentUser().getId() )
            .superUser( currentUserService.getCurrentUser().isSuper() ).build();

        /*
         * Async fetch Relationships for the given TrackedEntityInstance id (only if
         * isIncludeRelationships = true)
         */
        final CompletableFuture<Multimap<String, Relationship>> relationshipsAsync = conditionalAsyncFetch(
            params.isIncludeRelationships(), () -> trackedEntityInstanceStore.getRelationships( ids ) );

        /*
         * Async fetch Enrollments for the given TrackedEntityInstance id (only if
         * isIncludeEnrollments = true)
         */
        final CompletableFuture<Multimap<String, Enrollment>> enrollmentsAsync = conditionalAsyncFetch(
            params.isIncludeEnrollments(),
            () -> enrollmentAggregate.findByTrackedEntityInstanceIds( ids, aggregateContext, params.isIncludeEvents(),
                params.isIncludeRelationships() ) );

        /*
         * Async fetch all ProgramOwner for the given TrackedEntityInstance id
         */
        final CompletableFuture<Multimap<String, ProgramOwner>> programOwnersAsync = conditionalAsyncFetch(
            params.isIncludeProgramOwners(), () -> trackedEntityInstanceStore.getProgramOwners( ids ) );

        /*
         * Async Fetch TrackedEntityInstances by id
         */
        CompletableFuture<Map<String, TrackedEntityInstance>> teisAsync = supplyAsync(
            () -> trackedEntityInstanceStore.getTrackedEntityInstances( ids, aggregateContext ) );

        /*
         * Async fetch TrackedEntityInstance Attributes by TrackedEntityInstance id
         */
        CompletableFuture<Multimap<String, Attribute>> attributesAsync = supplyAsync(
            () -> trackedEntityInstanceStore.getAttributes( ids ) );

        /*
         * Execute all queries and merge the results
         */
        return allOf( teisAsync, attributesAsync, relationshipsAsync, enrollmentsAsync ).thenApplyAsync( dummy -> {

            Map<String, TrackedEntityInstance> teis = teisAsync.join();

            Multimap<String, Attribute> attributes = attributesAsync.join();

            Multimap<String, Relationship> relationships = relationshipsAsync.join();
            Multimap<String, Enrollment> enrollments = enrollmentsAsync.join();
            Multimap<String, ProgramOwner> programOwners = programOwnersAsync.join();

            return teis.keySet().parallelStream().map( uid -> {

                TrackedEntityInstance tei = teis.get( uid );
                tei.setAttributes( new ArrayList<>( attributes.get( uid ) ) );
                tei.setRelationships( new ArrayList<>( relationships.get( uid ) ) );
                tei.setEnrollments( new ArrayList<>( enrollments.get( uid ) ) );
                tei.setProgramOwners( new ArrayList<>( programOwners.get( uid ) ) );
                return tei;

            } ).collect( Collectors.toList() );

        } ).join();
    }

}
