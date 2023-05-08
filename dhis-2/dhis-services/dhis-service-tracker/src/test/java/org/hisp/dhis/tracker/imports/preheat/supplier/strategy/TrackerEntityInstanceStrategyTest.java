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
package org.hisp.dhis.tracker.imports.preheat.supplier.strategy;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class TrackerEntityInstanceStrategyTest
{
    @InjectMocks
    private TrackerEntityInstanceStrategy strategy;

    @Mock
    private TrackedEntityInstanceStore trackedEntityInstanceStore;

    @Mock
    private TrackerPreheat preheat;

    private final BeanRandomizer rnd = BeanRandomizer.create();

    @Test
    void verifyStrategyAddRightTeisToPreheat()
    {
        final List<org.hisp.dhis.tracker.imports.domain.TrackedEntity> trackedEntities = trackedEntities();
        final TrackerImportParams params = TrackerImportParams.builder().trackedEntities( trackedEntities ).build();

        final List<String> uids = List.of( "TEIA", "TEIB" );

        List<List<String>> splitUids = new ArrayList<>();
        splitUids.add( uids );

        List<TrackedEntity> trackedEntityInstances = trackedEntityInstances();
        when( trackedEntityInstanceStore.getIncludingDeleted( uids ) ).thenReturn( trackedEntityInstances );
        strategy.add( params, splitUids, preheat );

        Mockito.verify( trackedEntityInstanceStore ).getIncludingDeleted( uids );
        Mockito.verify( preheat ).putTrackedEntities( trackedEntityInstances );
    }

    private List<org.hisp.dhis.tracker.imports.domain.TrackedEntity> trackedEntities()
    {
        return List.of(
            org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder().trackedEntity( "TEIA" ).build(),
            org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder().trackedEntity( "TEIB" ).build() );
    }

    private List<TrackedEntity> trackedEntityInstances()
    {
        TrackedEntity teiA = new TrackedEntity();
        teiA.setUid( "TEIA" );
        TrackedEntity teiB = new TrackedEntity();
        teiB.setUid( "TEIB" );
        return List.of( teiA, teiB );
    }
}