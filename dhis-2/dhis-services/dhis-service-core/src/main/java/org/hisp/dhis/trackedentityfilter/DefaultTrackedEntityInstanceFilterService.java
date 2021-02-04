package org.hisp.dhis.trackedentityfilter;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import org.hisp.dhis.program.Program;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
@Service( "org.hisp.dhis.trackedentityfilter.TrackedEntityInstanceFilterService" )
public class DefaultTrackedEntityInstanceFilterService
    implements TrackedEntityInstanceFilterService
{
    
    private final TrackedEntityInstanceFilterStore trackedEntityInstanceFilterStore;

    public DefaultTrackedEntityInstanceFilterService(
        TrackedEntityInstanceFilterStore trackedEntityInstanceFilterStore )
    {
        checkNotNull(trackedEntityInstanceFilterStore);

        this.trackedEntityInstanceFilterStore = trackedEntityInstanceFilterStore;
    }

    // -------------------------------------------------------------------------
    // TrackedEntityInstanceFilterService implementation
    // -------------------------------------------------------------------------
    
    @Override
    @Transactional
    public long add( TrackedEntityInstanceFilter trackedEntityInstanceFilter )
    {        
        trackedEntityInstanceFilterStore.save( trackedEntityInstanceFilter );
        return trackedEntityInstanceFilter.getId();
    }
    
    @Override
    @Transactional
    public void delete( TrackedEntityInstanceFilter trackedEntityInstanceFilter )
    {
        trackedEntityInstanceFilterStore.delete( trackedEntityInstanceFilter );
    }

    @Override
    @Transactional
    public void update( TrackedEntityInstanceFilter trackedEntityInstanceFilter )
    {
        trackedEntityInstanceFilterStore.update( trackedEntityInstanceFilter );
    }
    
    @Override
    @Transactional(readOnly = true)
    public TrackedEntityInstanceFilter get( long id )
    {
        return trackedEntityInstanceFilterStore.get( id );
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TrackedEntityInstanceFilter> getAll()
    {
        return trackedEntityInstanceFilterStore.getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackedEntityInstanceFilter> get( Program program )
    {
        return trackedEntityInstanceFilterStore.get( program );
    }
}
