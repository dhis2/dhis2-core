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
package org.hisp.dhis.eventvisualization;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.hisp.dhis.common.AnalyticalObjectStore;
import org.hisp.dhis.common.GenericAnalyticalObjectService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Basic service methods for EventVisualization.
 *
 * @author maikel arabori
 */
@Service
public class DefaultEventVisualizationService
    extends GenericAnalyticalObjectService<EventVisualization>
    implements EventVisualizationService
{
    private final AnalyticalObjectStore<EventVisualization> eventVisualizationStore;

    public DefaultEventVisualizationService( @Qualifier( "org.hisp.dhis.eventvisualization.EventVisualizationStore" )
    final AnalyticalObjectStore<EventVisualization> eventVisualizationStore )
    {
        checkNotNull( eventVisualizationStore );

        this.eventVisualizationStore = eventVisualizationStore;
    }

    // -------------------------------------------------------------------------
    // EventReportService implementation
    // -------------------------------------------------------------------------

    @Override
    protected AnalyticalObjectStore<EventVisualization> getAnalyticalObjectStore()
    {
        return eventVisualizationStore;
    }

    @Override
    @Transactional
    public long save( EventVisualization eventVisualization )
    {
        eventVisualizationStore.save( eventVisualization );
        return eventVisualization.getId();
    }

    @Override
    @Transactional
    public void update( EventVisualization report )
    {
        eventVisualizationStore.update( report );
    }

    @Override
    @Transactional( readOnly = true )
    public EventVisualization getEventVisualization( long id )
    {
        return eventVisualizationStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public EventVisualization getEventVisualization( String uid )
    {
        return eventVisualizationStore.getByUid( uid );
    }

    @Override
    @Transactional
    public void delete( EventVisualization eventVisualization )
    {
        eventVisualizationStore.delete( eventVisualization );
    }

    @Override
    public EventVisualization getVisualizationNoAcl( String uid )
    {
        return eventVisualizationStore.getByUidNoAcl( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<EventVisualization> getAllEventVisualizations()
    {
        return eventVisualizationStore.getAll();
    }
}
