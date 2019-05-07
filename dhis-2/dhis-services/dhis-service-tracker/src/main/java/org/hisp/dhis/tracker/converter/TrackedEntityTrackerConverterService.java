package org.hisp.dhis.tracker.converter;

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

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@Transactional
public class TrackedEntityTrackerConverterService
    implements TrackerConverterService<TrackedEntityInstance, org.hisp.dhis.trackedentity.TrackedEntityInstance>
{
    private final TrackerPreheatService trackerPreheatService;
    private final IdentifiableObjectManager manager;

    public TrackedEntityTrackerConverterService(
        TrackerPreheatService trackerPreheatService,
        IdentifiableObjectManager manager )
    {
        this.trackerPreheatService = trackerPreheatService;
        this.manager = manager;
    }

    @Override
    public TrackedEntityInstance to( org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance )
    {
        List<TrackedEntityInstance> trackedEntities = to( Collections.singletonList( trackedEntityInstance ) );

        if ( trackedEntities.isEmpty() )
        {
            return null;
        }

        return trackedEntities.get( 0 );
    }

    @Override
    public List<TrackedEntityInstance> to( List<org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntityInstances )
    {
        List<TrackedEntityInstance> trackedEntities = new ArrayList<>();

        trackedEntityInstances.forEach( tei -> {

        } );

        return trackedEntities;
    }

    @Override
    public org.hisp.dhis.trackedentity.TrackedEntityInstance from( TrackedEntityInstance trackedEntityInstance )
    {
        List<org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntityInstances = from( Collections.singletonList( trackedEntityInstance ) );

        if ( trackedEntityInstances.isEmpty() )
        {
            return null;
        }

        return trackedEntityInstances.get( 0 );
    }

    @Override
    public org.hisp.dhis.trackedentity.TrackedEntityInstance from( TrackerPreheat preheat, TrackedEntityInstance trackedEntityInstance )
    {
        List<org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntityInstances = from( preheat, Collections.singletonList( trackedEntityInstance ) );

        if ( trackedEntityInstances.isEmpty() )
        {
            return null;
        }

        return trackedEntityInstances.get( 0 );
    }

    @Override
    public List<org.hisp.dhis.trackedentity.TrackedEntityInstance> from( List<TrackedEntityInstance> trackedEntityInstances )
    {
        return from( preheat( trackedEntityInstances ), trackedEntityInstances );
    }

    @Override
    public List<org.hisp.dhis.trackedentity.TrackedEntityInstance> from( TrackerPreheat preheat,
        List<TrackedEntityInstance> trackedEntityInstances )
    {
        List<org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntities = new ArrayList<>();

        trackedEntityInstances.forEach( te -> {

        } );

        return trackedEntities;
    }

    private TrackerPreheat preheat( List<TrackedEntityInstance> trackedEntities )
    {
        TrackerPreheatParams params = new TrackerPreheatParams()
            .setTrackedEntities( trackedEntities );

        return trackerPreheatService.preheat( params );
    }
}
