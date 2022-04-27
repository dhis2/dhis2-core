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
package org.hisp.dhis.tracker.preheat.supplier.strategy;

import java.util.List;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.mappers.TrackedEntityInstanceMapper;
import org.hisp.dhis.tracker.preheat.supplier.DetachUtils;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@RequiredArgsConstructor
@Component
@StrategyFor( value = TrackedEntity.class, mapper = TrackedEntityInstanceMapper.class )
public class TrackerEntityInstanceStrategy implements ClassBasedSupplierStrategy
{
    @NonNull
    private TrackedEntityInstanceStore trackedEntityInstanceStore;

    @Override
    public void add( TrackerImportParams params, List<List<String>> splitList, TrackerPreheat preheat )
    {
        for ( List<String> ids : splitList )
        {
            // Fetch all Tracked Entity Instance present in the payload
            List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceStore.getIncludingDeleted( ids );

            // Get the uids of all the TEIs which are root (a TEI is not root
            // when is a
            // property of another object, e.g. enrollment)
            final List<String> rootEntities = params.getTrackedEntities().stream()
                .map( TrackedEntity::getTrackedEntity )
                .collect( Collectors.toList() );

            // Add to preheat
            preheat.putTrackedEntities(
                DetachUtils.detach( this.getClass().getAnnotation( StrategyFor.class ).mapper(),
                    trackedEntityInstances ),
                RootEntitiesUtils.filterOutNonRootEntities( ids, rootEntities ) );
        }
    }
}
