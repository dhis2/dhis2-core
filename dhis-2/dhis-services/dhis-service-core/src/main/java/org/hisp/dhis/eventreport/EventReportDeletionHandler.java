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
package org.hisp.dhis.eventreport;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;

import org.hisp.dhis.common.AnalyticalObjectService;
import org.hisp.dhis.common.GenericAnalyticalObjectDeletionHandler;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.springframework.stereotype.Component;

/**
 * @author Chau Thu Tran
 */
@Component( "org.hisp.dhis.eventreport.EventReportDeletionHandler" )
public class EventReportDeletionHandler
    extends GenericAnalyticalObjectDeletionHandler<EventReport>
{
    private final EventReportService eventReportService;

    public EventReportDeletionHandler( EventReportService eventReportService )
    {
        super( new DeletionVeto( EventReport.class ) );
        checkNotNull( eventReportService );
        this.eventReportService = eventReportService;
    }

    @Override
    protected void register()
    {
        super.register();
        whenDeleting( DataElement.class, this::deleteDataElement );
        whenDeleting( ProgramStage.class, this::deleteProgramStage );
        whenDeleting( Program.class, this::deleteProgram );
    }

    @Override
    protected AnalyticalObjectService<EventReport> getAnalyticalObjectService()
    {
        return eventReportService;
    }

    @Override
    protected boolean isDeleteIndicator()
    {
        return false;
    }

    @Override
    protected boolean isDeleteDataElement()
    {
        return false; // override below
    }

    private void deleteDataElement( DataElement dataElement )
    {
        List<EventReport> eventReports = getAnalyticalObjectService()
            .getAnalyticalObjectsByDataDimension( dataElement );

        for ( EventReport report : eventReports )
        {
            report.getDataElementDimensions()
                .removeIf( trackedEntityDataElementDimension -> trackedEntityDataElementDimension.getDataElement()
                    .equals( dataElement ) );

            eventReportService.update( report );
        }
    }

    @Override
    protected boolean isDeleteDataSet()
    {
        return false;
    }

    @Override
    protected boolean isDeleteProgramIndicator()
    {
        return false;
    }

    private void deleteProgramStage( ProgramStage programStage )
    {
        Collection<EventReport> charts = eventReportService.getAllEventReports();

        for ( EventReport chart : charts )
        {
            if ( chart.getProgramStage().equals( programStage ) )
            {
                eventReportService.deleteEventReport( chart );
            }
        }
    }

    private void deleteProgram( Program program )
    {
        Collection<EventReport> charts = eventReportService.getAllEventReports();

        for ( EventReport chart : charts )
        {
            if ( chart.getProgram().equals( program ) )
            {
                eventReportService.deleteEventReport( chart );
            }
        }
    }
}
