package org.hisp.dhis.eventchart;

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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hisp.dhis.common.AnalyticalObjectService;
import org.hisp.dhis.common.GenericAnalyticalObjectDeletionHandler;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class EventChartDeletionHandler
    extends GenericAnalyticalObjectDeletionHandler<EventChart>
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private EventChartService eventChartService;

    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    protected AnalyticalObjectService<EventChart> getAnalyticalObjectService()
    {
        return eventChartService;
    }
    
    @Override
    protected String getClassName()
    {
        return EventChart.class.getSimpleName();
    }

    @Override
    public void deleteIndicator( Indicator indicator )
    {
        // Ignore default implementation
    }

    @Override
    public void deleteDataElement( DataElement dataElement )
    {
        List<EventChart> eventCharts = getAnalyticalObjectService().getAnalyticalObjectsByDataDimension( dataElement );
        
        for ( EventChart chart : eventCharts )
        {
            Iterator<TrackedEntityDataElementDimension> dimensions = chart.getDataElementDimensions().iterator();
            
            while ( dimensions.hasNext() )
            {
                if ( dimensions.next().getDataElement().equals( dataElement ) )
                {
                    dimensions.remove();
                }
            }
            
            eventChartService.update( chart );
        }
    }

    @Override
    public void deleteDataSet( DataSet dataSet )
    {
        // Ignore default implementation
    }

    @Override
    public void deleteProgramIndicator( ProgramIndicator programIndicator )
    {
     // Ignore default implementation
    }
    
    @Override
    public void deleteProgramStage( ProgramStage programStage )
    {
        Collection<EventChart> charts = eventChartService.getAllEventCharts();
        
        for ( EventChart chart : charts )
        {
            if( chart.getProgramStage().equals( programStage ))
            {
                eventChartService.deleteEventChart( chart );
            }
        }
    }

    @Override
    public void deleteProgram( Program program )
    {
        Collection<EventChart> charts = eventChartService.getAllEventCharts();
        
        for ( EventChart chart : charts )
        {
            if ( chart.getProgram().equals( program ))
            {
                eventChartService.deleteEventChart( chart );
            }
        }
    }
}
