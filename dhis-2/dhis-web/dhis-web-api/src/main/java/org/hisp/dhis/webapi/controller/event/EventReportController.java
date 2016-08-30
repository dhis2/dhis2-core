package org.hisp.dhis.webapi.controller.event;

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

import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventreport.EventReportService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.schema.descriptors.EventReportSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensions;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = EventReportSchemaDescriptor.API_ENDPOINT )
public class EventReportController
    extends AbstractCrudController<EventReport>
{
    @Autowired
    private EventReportService eventReportService;

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private I18nManager i18nManager;

    //--------------------------------------------------------------------------
    // CRUD
    //--------------------------------------------------------------------------

    @Override
    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    public void postJsonObject( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        EventReport eventReport = deserializeJsonEntity( request, response );
        eventReport.getTranslations().clear();

        mergeEventReport( eventReport );

        eventReportService.saveEventReport( eventReport );

        response.addHeader( "Location", EventReportSchemaDescriptor.API_ENDPOINT + "/" + eventReport.getUid() );
        webMessageService.send( WebMessageUtils.created( "Event report created" ), response, request );
    }

    @Override
    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = "application/json" )
    public void putJsonObject( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        EventReport eventReport = eventReportService.getEventReport( uid );

        if ( eventReport == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Event report does not exist: " + uid ) );
        }

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() );

        EventReport newReport = deserializeJsonEntity( request, response );
        newReport.setTranslations( eventReport.getTranslations() );

        mergeEventReport( newReport );

        eventReport.mergeWith( newReport, params.getMergeMode() );

        eventReportService.updateEventReport( eventReport );
    }

    //--------------------------------------------------------------------------
    // Hooks
    //--------------------------------------------------------------------------

    @Override
    protected void postProcessEntity( EventReport report )
        throws Exception
    {
        report.populateAnalyticalProperties();

        Set<OrganisationUnit> roots = currentUserService.getCurrentUser().getDataViewOrganisationUnitsWithFallback();

        for ( OrganisationUnit organisationUnit : report.getOrganisationUnits() )
        {
            report.getParentGraphMap().put( organisationUnit.getUid(), organisationUnit.getParentGraph( roots ) );
        }

        I18nFormat format = i18nManager.getI18nFormat();

        if ( report.getPeriods() != null && !report.getPeriods().isEmpty() )
        {
            for ( Period period : report.getPeriods() )
            {
                period.setName( format.formatPeriod( period ) );
            }
        }
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private void mergeEventReport( EventReport report )
    {
        dimensionService.mergeAnalyticalObject( report );
        dimensionService.mergeEventAnalyticalObject( report );

        report.getColumnDimensions().clear();
        report.getRowDimensions().clear();
        report.getFilterDimensions().clear();

        report.getColumnDimensions().addAll( getDimensions( report.getColumns() ) );
        report.getRowDimensions().addAll( getDimensions( report.getRows() ) );
        report.getFilterDimensions().addAll( getDimensions( report.getFilters() ) );

        if ( report.getProgram() != null )
        {
            report.setProgram( programService.getProgram( report.getProgram().getUid() ) );
        }

        if ( report.getProgramStage() != null )
        {
            report.setProgramStage( programStageService.getProgramStage( report.getProgramStage().getUid() ) );
        }
    }
}
