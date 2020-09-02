package org.hisp.dhis.tracker.converter;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
public class TrackedEntityTrackerConverterService
    implements TrackerConverterService<TrackedEntity, org.hisp.dhis.trackedentity.TrackedEntityInstance>
{

    @Override
    public TrackedEntity to( org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance )
    {
        List<TrackedEntity> trackedEntities = to( Collections.singletonList( trackedEntityInstance ) );

        if ( trackedEntities.isEmpty() )
        {
            return null;
        }

        return trackedEntities.get( 0 );
    }

    @Override
    public List<TrackedEntity> to( List<org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntityInstances )
    {
        return trackedEntityInstances.stream().map( tei -> {
            TrackedEntity trackedEntity = new TrackedEntity();
            trackedEntity.setTrackedEntity( tei.getUid() );

            return trackedEntity;
        } ).collect( Collectors.toList() );
    }

    @Override
    public org.hisp.dhis.trackedentity.TrackedEntityInstance from( TrackerPreheat preheat,
        TrackedEntity trackedEntityInstance )
    {
        List<org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntityInstances = from( preheat,
            Collections.singletonList( trackedEntityInstance ) );

        if ( trackedEntityInstances.isEmpty() )
        {
            return null;
        }

        return trackedEntityInstances.get( 0 );
    }

    @Override
    public List<org.hisp.dhis.trackedentity.TrackedEntityInstance> from( TrackerPreheat preheat,
        List<TrackedEntity> trackedEntityInstances )
    {
        List<org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntities = new ArrayList<>();

        trackedEntityInstances.forEach( te -> {
            org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntity = preheat.getTrackedEntity(
                TrackerIdScheme.UID, te.getTrackedEntity() );
            OrganisationUnit organisationUnit = preheat
                .get( TrackerIdScheme.UID, OrganisationUnit.class, te.getOrgUnit() );
            TrackedEntityType trackedEntityType = preheat
                .get( TrackerIdScheme.UID, TrackedEntityType.class, te.getTrackedEntityType() );

            if ( trackedEntity == null )
            {
                Date now = new Date();

                trackedEntity = new org.hisp.dhis.trackedentity.TrackedEntityInstance();
                trackedEntity.setUid( te.getTrackedEntity() );
                trackedEntity.setCreated( now );
                trackedEntity.setCreatedAtClient( now );
                trackedEntity.setLastUpdated( now );
                trackedEntity.setLastUpdatedAtClient( now );
                trackedEntity.setStoredBy( te.getStoredBy() );
            }

            if ( !CodeGenerator.isValidUid( trackedEntity.getUid() ) )
            {
                trackedEntity.setUid( CodeGenerator.generateUid() );
            }

            trackedEntity.setOrganisationUnit( organisationUnit );
            trackedEntity.setTrackedEntityType( trackedEntityType );
            trackedEntity.setInactive( te.isInactive() );
            trackedEntity.setDeleted( te.isDeleted() );
            trackedEntity.setGeometry( te.getGeometry() );

            trackedEntities.add( trackedEntity );
        } );

        return trackedEntities;
    }
}