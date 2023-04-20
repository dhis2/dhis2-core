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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.time.StopWatch;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.cache.PreheatCacheService;

/**
 * A {@link PreheatSupplier} subclass can implement this abstract class to
 * execute code before and after the supplier has been executed (e.g. timing)
 *
 * @author Luciano Fiandesio
 */
@Slf4j
public abstract class AbstractPreheatSupplier implements PreheatSupplier
{
    private final int CACHE_TTL = 60;

    private final long CACHE_CAPACITY = 1000;

    @Override
    public void add( TrackerImportParams params, TrackerPreheat preheat )
    {
        StopWatch watch = null;
        if ( log.isDebugEnabled() )
        {
            log.debug( "Executing preheat supplier: {}", this.getClass().getName() );
            watch = new StopWatch();
            watch.start();
        }

        preheatAdd( params, preheat );

        if ( log.isDebugEnabled() )
        {
            if ( watch != null && watch.isStarted() )
            {
                watch.stop();
                log.debug( "Supplier {} executed in : {}", this.getClass().getName(),
                    TimeUnit.SECONDS.convert( watch.getNanoTime(), TimeUnit.NANOSECONDS ) );
            }
        }
    }

    /**
     * Template method: executes preheat logic from the subclass
     */
    public abstract void preheatAdd( TrackerImportParams params, TrackerPreheat preheat );

    protected void addToCache( PreheatCacheService cache, List<? extends IdentifiableObject> objects )
    {
        objects.forEach( rt -> cache.put( HibernateProxyUtils.getRealClass( rt ).getName(), rt.getUid(), rt, CACHE_TTL,
            CACHE_CAPACITY ) );
    }
}
