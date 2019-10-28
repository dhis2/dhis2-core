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
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.store.TrackedEntityInstanceStore;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Component
public class TrackedEntityInstanceAggregate
{

    private final TrackedEntityInstanceStore trackedEntityInstanceStore;

    private final EnrollmentAggregate enrollmentAggregate;

    public TrackedEntityInstanceAggregate( TrackedEntityInstanceStore trackedEntityInstanceStore,
        EnrollmentAggregate enrollmentAggregate )
    {
        this.trackedEntityInstanceStore = trackedEntityInstanceStore;
        this.enrollmentAggregate = enrollmentAggregate;
    }

    public List<TrackedEntityInstance> find( List<Long> ids, TrackedEntityInstanceParams params )
    {
        final CompletableFuture<Multimap<String, Relationship>> relationshipsAsync = (!params.isIncludeRelationships()
            ? supplyAsync( ArrayListMultimap::create )
            : supplyAsync( () -> trackedEntityInstanceStore.getRelationships( ids ) ));

        final CompletableFuture<Multimap<String, Enrollment>> enrollmentsAsync = (!params.isIncludeEnrollments()
            ? supplyAsync( ArrayListMultimap::create )
            : supplyAsync( () -> enrollmentAggregate.findByTrackedEntityInstanceIds( ids, params.isIncludeEvents() ) ));

        CompletableFuture<Map<String, TrackedEntityInstance>> teisAsync = supplyAsync(
            () -> trackedEntityInstanceStore.getTrackedEntityInstances( ids ) );

        CompletableFuture<Multimap<String, Attribute>> attributesAsync = supplyAsync(
            () -> trackedEntityInstanceStore.getAttributes( ids ) );

        return allOf( teisAsync, attributesAsync, relationshipsAsync, enrollmentsAsync ).thenApplyAsync( dummy -> {

            Map<String, TrackedEntityInstance> teis = teisAsync.join();

            Multimap<String, Attribute> attributes = attributesAsync.join();

            Multimap<String, Relationship> relationships = relationshipsAsync.join();
            Multimap<String, Enrollment> enrollments = enrollmentsAsync.join();

            return teis.keySet().parallelStream().map( uid -> {

                TrackedEntityInstance tei = teis.get( uid );
                tei.setAttributes( new ArrayList<>( attributes.get( uid ) ) );
                tei.setRelationships( new ArrayList<>( relationships.get( uid ) ) );
                tei.setEnrollments( new ArrayList<>( enrollments.get( uid ) ) );
                return tei;

            } ).collect( Collectors.toList() );

        } ).join();
    }

}
