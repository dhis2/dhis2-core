package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.List;

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@Transactional
public class DefaultTrackedEntityService
    implements TrackedEntityService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private GenericIdentifiableObjectStore<TrackedEntity> trackedEntityStore;

    public void setTrackedEntityStore( GenericIdentifiableObjectStore<TrackedEntity> trackedEntityStore )
    {
        this.trackedEntityStore = trackedEntityStore;
    }

    // -------------------------------------------------------------------------
    // TrackedEntity
    // -------------------------------------------------------------------------

    @Override
    public int addTrackedEntity( TrackedEntity trackedEntity )
    {
        return trackedEntityStore.save( trackedEntity );
    }

    @Override
    public void deleteTrackedEntity( TrackedEntity trackedEntity )
    {
        trackedEntityStore.delete( trackedEntity );
    }

    @Override
    public void updateTrackedEntity( TrackedEntity trackedEntity )
    {
        trackedEntityStore.update( trackedEntity );
    }

    @Override
    public TrackedEntity getTrackedEntity( int id )
    {
        return trackedEntityStore.get( id );
    }

    @Override
    public TrackedEntity getTrackedEntity( String uid )
    {
        return trackedEntityStore.getByUid( uid );
    }

    @Override
    public TrackedEntity getTrackedEntityByName( String name )
    {
        return trackedEntityStore.getByName( name );
    }

    @Override
    public List<TrackedEntity> getAllTrackedEntity()
    {
        return trackedEntityStore.getAll();
    }

    @Override
    public Integer getTrackedEntityCountByName( String name )
    {
        return trackedEntityStore.getCountLikeName( name );
    }

    @Override
    public List<TrackedEntity> getTrackedEntityBetweenByName( String name, int min, int max )
    {
        return trackedEntityStore.getAllLikeName( name, min, max );
    }

    @Override
    public Integer getTrackedEntityCount()
    {
        return trackedEntityStore.getCount();
    }

    @Override
    public List<TrackedEntity> getTrackedEntitysBetween( int min, int max )
    {
        return trackedEntityStore.getAllOrderedName( min, max );
    }
}
