package org.hisp.dhis.eventreport;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.common.AnalyticalObjectStore;
import org.hisp.dhis.common.GenericAnalyticalObjectService;
import org.hisp.dhis.common.hibernate.HibernateAnalyticalObjectStore;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
* @author Lars Helge Overland
*/
@Transactional
public class DefaultEventReportService
    extends GenericAnalyticalObjectService<EventReport>
    implements EventReportService
{
    private HibernateAnalyticalObjectStore<EventReport> eventReportStore;

    public void setEventReportStore( HibernateAnalyticalObjectStore<EventReport> eventReportStore )
    {
        this.eventReportStore = eventReportStore;
    }

    // -------------------------------------------------------------------------
    // EventReportService implementation
    // -------------------------------------------------------------------------

    @Override
    protected AnalyticalObjectStore<EventReport> getAnalyticalObjectStore()
    {
        return eventReportStore;
    }
    
    @Override
    public int saveEventReport( EventReport report )
    {
        eventReportStore.save( report );
        return report.getId();
    }
    
    @Override
    public void updateEventReport( EventReport report )
    {
        eventReportStore.update( report );
    }
    
    @Override
    public EventReport getEventReport( int id )
    {
        return eventReportStore.get( id );
    }
    
    @Override
    public EventReport getEventReport( String uid )
    {
        return eventReportStore.getByUid( uid );
    }
    
    @Override
    public void deleteEventReport( EventReport report )
    {
        eventReportStore.delete( report );
    }
    
    @Override
    public List<EventReport> getAllEventReports()
    {
        return eventReportStore.getAll();
    }
}
